package dbms.update_handler;
//Basic utilities import..
import java.io.*;
import java.util.*;
import java.io.IOException;
import java.util.regex.Pattern;

//AWS Specific imports...
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import software.amazon.awssdk.regions.Region;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;

public class PlaylistUpdateHandler implements RequestHandler<Object, String> {

//Creating the enum to store the Event types..
enum EventType{ INSERT, REMOVE, UPDATE}
//Creating an instance of the fixants class to access the fixants..
static Constants fix = new Constants();

//Creating an instance of our DbmsLogKeeper class for Logging in this file...
static DbmsLogKeeper myLog = new DbmsLogKeeper();

  public String handleRequest(Object obj, Context context) {
	//Logging the begining of the LOG..  
  	myLog.makeInfoLog("*** LOGGING STARTS FOR HANDLER.JAVA ***");
  		//Logging the event details..
  		myLog.makeInfoLog("Event details = "+ obj.toString());
		String eventName = "";
  	
		// Converting the Object into String..
		String event = obj.toString();

		//Setting up the cases when the event is not a dynamoDB event..
		if (event.indexOf("Keys") == fix.INVALID_INDEX || event.indexOf("SequenceNumber") == fix.INVALID_INDEX || event.indexOf("Keys") >= event.indexOf("SequenceNumber")) {
			myLog.makeInfoLog("Invalid Entry!");
		}
		else {
			String eventNameGetter[] = event.substring(event.indexOf("eventName"), event.indexOf(",", event.indexOf("eventName"))).split("=");
			eventName = eventNameGetter[1].trim();
			myLog.makeInfoLog("Event name = "+ eventName);
		}
		
		//Now we have the event type which can either be INSERT or REMOVE based on the problem
		//statement for our project...Now we handle both the cases by separate functions
		if(eventName.equalsIgnoreCase(EventType.INSERT.toString())) {
			handleInsert(event);
		}
		else if(eventName.equalsIgnoreCase(EventType.REMOVE.toString())) {
			handleRemove(event);
		}
		else {
			myLog.makeInfoLog("This is an invalid Event for the action!");
			return "Faliure!";
		}
		
		myLog.makeInfoLog("Transaction Completed Successfully!");
		return "Success!";
	}
  
//************************************************************** BEGINING OF METHOD **************************************************************
 /*
  * Task:- The method reads the stream from the passed event, finds the details of all the tracks belonging to the playlist to which the INSERT 
  * event correseponds and updates the corresponding item (playlist details) in the user-playlist-info.
  * The method return nothing (void).
  * Parameters of the method :- 1. A DynamoDB event in the form of a String from the playlist-tracks table for reading the streams.  
  */
  private static void handleInsert(String event) {
  	//The NewImage contents lie between 'NewImage' tag and 'SequenceNumber' tag..So extracting the feature details from the event String
  	String newImageGetter = event.substring(event.indexOf("NewImage"), event.indexOf(", SequenceNumber"));
  	String newImage = newImageGetter.trim().substring(newImageGetter.indexOf("=")+2, newImageGetter.length()-1);
  	if(newImage.length() == 0) {
  		//A case which corresponds to an erranous case..
  		myLog.makeInfoLog("Empty newImage!");
  	}
  	else {
  		//Getting all items of the newImage which are space separated...
  		String newImageItems[] = newImage.split(", ");
  		//Creating a hashmap consisting of Keys = attribute names and values = Attribute value..
  		HashMap<String, String> map = new HashMap<String, String>();
  		//Separating the attribute id and value for each feature which are originally in the form of 'key=value'..
  		for(String item : newImageItems) {
  			String tempId = item.substring(0, item.indexOf("="));
  			String tempValue = item.substring(item.indexOf("=", item.indexOf("=")+1)+1, item.length()-1);
  			map.put(tempId, tempValue);
  		}
  		//Logging the key and value map of newly inserted item as a Map...
  		myLog.makeInfoLog("Map of new Image = "+map.toString());
  		
  		 //Calling the method to update the details based on the details stored in the map..return 1 if success and 0 if failed.
  		 int success = updateTable(map); 	
  		 if(success == 1) {
  			//Notifying the successful completion of the transaction..
  			myLog.makeInfoLog("Entry Successful!");
  		 }
  		 else {
  			 //Notifying the failure of updation..
  			myLog.makeInfoLog("updateMethod failed to update the table!");
  		 }
  	}
  }
//***************************************************************END OF THE METHOD ***************************************************************
  /*
   * Task:- The method reads the stream from the passed event, finds the details of all the tracks belonging to the playlist to which the REMOVE 
   * event correseponds and updates the corresponding item (playlist details) in the user-playlist-info.
   * The method return nothing (void).
   * Parameters of the method :- 1. A DynamoDB event in the form of a String from the playlist-tracks table for reading the streams.  
   */
 //************************************************************** BEGINING OF METHOD **************************************************************
  /*
   * This method handles the REMOVE event for our code.
   * The method return nothing (void).
   * Parameters of the method :- 1. A DynamoDB event in the form of a String from the playlist-tracks table for reading the streams.
   * Task:- The method reads the stream from the passed event, finds the details of all the tracks belonging to the playlist to which the REMOVE 
   * event correseponds and updates the corresponding item (playlist details) in the user-playlist-info.  
   */
  //This method handles the remove case...
  private static void handleRemove(String event) {
	  	//The OldImage contents lie between 'OldImage' tag and 'SequenceNumber' tag..So extracting the feature details from the event String
	    String newImageGetter = event.substring(event.indexOf("OldImage"), event.indexOf(", SequenceNumber"));
	    String newImage = newImageGetter.trim().substring(newImageGetter.indexOf("=")+2, newImageGetter.length()-1);
	    if(newImage.length() == 0) {
	    	//A case which corresponds to an erranous case..
	    	myLog.makeInfoLog("Empty newImage!");
	    }
	    else {
	    	//Getting all items of the newImage which are space separated...
	    	String newImageItems[] = newImage.split(", ");
	    	//Creating a hashmap consisting of Keys = attribute names and values = Attribute value..
	    	HashMap<String, String> map = new HashMap<String, String>();
	    	//Separating the attribute id and value for each feature which are originally in the form of 'key=value'..
	    	for(String item : newImageItems) {
	    		String tempId = item.substring(0, item.indexOf("="));
	    		String tempValue = item.substring(item.indexOf("=", item.indexOf("=")+1)+1, item.length()-1);
	    		map.put(tempId, tempValue);
	    	}
	    	//Logging the key and value map of newly inserted item as  a Map...
	    	myLog.makeInfoLog("Map of Old Image = "+map.toString());
	    	
	    	//Calling the method to update the details based on the details stored in the map..return 1 if success and 0 if failed.
	    	int success = updateTable(map); 	
	  		 if(success == 1) {
	  			//Notifying the successful completion of the transaction..
	  			myLog.makeInfoLog("Removal Successful!");
	  		 }
	  		 else {
	  			//Notifying the Failed update..
	  			myLog.makeInfoLog("updateMethod failed to update the table!");
	  		 }
	    }
  }
//*************************************************** END OF THE METHOD ********************************************************************
  /*
   * Task:- The method takes the map and updates the user-playlist-info table according to the attribute names and the values. Also, the  
   * method computes the era (from the release date) and other such variables before updating the table. To update the table, it first reads
   * the previous contents of the concerened item of the table and modifies it then.
   * The method return int (either 1 = success/ 0 = Failure).
   * Parameters of the method :- 1. HashMap<String, String> containing the keys = attributes(feature names) and values = value of the featuress.
   */
//************************************************ BEGINING OF THE METHOD ****************************************************************** 
  public static int updateTable(HashMap<String, String> map) {
	  	//Creating an instance of the POJO class TableUpdateVariables...it takes a Map and initializes the variables via the Map...
	  	TableUpdateVariables tuv = new TableUpdateVariables(map);
	  	
		//Now reading the contents of the playlist table to update it as per the new entry..
	  	DynamoDB dynamodb = null;
		try {
			AmazonDynamoDB playlistClient = AmazonDynamoDBClientBuilder.standard().build();
			dynamodb = new DynamoDB(playlistClient);
		}
		catch(Exception e) {
			myLog.makeInfoLog("Error in making connection to DynamoDB. Precise error = "+e);
			return 0;
		}
		
		Table playlistTable = dynamodb.getTable("playlist-tracks");
		ItemCollection currentItem = playlistTable.query("pasin", tuv.getPasin());
		//Iterating over all the items received from the query..
		Iterator itr = currentItem.iterator();
		int no_of_tracks = 0;
		int track_duration_sum = 0;
		int track_popularity_sum = 0;
		int avg_popularity = 0;
		
		HashMap<String, Integer> artist_dir = new HashMap<String, Integer>();
		HashMap<String, Integer> album_dir = new HashMap<String, Integer>();
		HashMap<String, Integer> genre_dir = new HashMap<String, Integer>();
		HashMap<Integer, Integer> era_dir = new HashMap<Integer, Integer>();
		era_dir.put(0, 0);
		era_dir.put(1, 0);
		era_dir.put(2, 0);
		era_dir.put(3, 0);
		era_dir.put(4, 0);
		
		while(itr.hasNext()) {
			//Logging the details from each item..
			Item temp = (Item)itr.next();
			myLog.makeInfoLog("Item = "+temp.toString());
			String eraTemp[] = temp.get("release").toString().split(" "); //In the format date month year...so 0 = date, 1 = month, 2 = year..
			//Extracting the year for classifying it into an era..
			int eraYear = Integer.parseInt(eraTemp[2]);
			//Deciding the era of each track based on the release year..
			if(eraYear >= 1920 && eraYear <=1940) {
				era_dir.put(0, era_dir.get(0)+1);
			}
			else if(eraYear > 1940 && eraYear <=1960) {
				era_dir.put(1, era_dir.get(1)+1);
			}
			else if(eraYear > 1960 && eraYear <=1980) {
				era_dir.put(2, era_dir.get(2)+1);
			}
			else if(eraYear > 1980 && eraYear <=2000) {
				era_dir.put(3, era_dir.get(3)+1);
			}
			else{
				era_dir.put(4, era_dir.get(4)+1);
			}
			artist_dir.put(temp.get("artist").toString(), artist_dir.getOrDefault(temp.get("artist").toString(), 0)+1);
			album_dir.put(temp.get("album").toString(), album_dir.getOrDefault(temp.get("album").toString(), 0)+1);
			genre_dir.put(temp.get("genre").toString(), genre_dir.getOrDefault(temp.get("genre").toString(), 0)+1);
			no_of_tracks++; //Counting the number of tracks..
			track_duration_sum += (int)Double.parseDouble(temp.get("duration").toString()); //Summing the duration of all the tracks..
			track_popularity_sum += (int)Double.parseDouble(temp.get("popularity").toString()); //Summing the rating of all the tracks for avg later..
		}
		
		if(no_of_tracks > 0) {
			//To avoid divide by zero error in case of empty playlist..
			//No of tracks will never be -ve.
			avg_popularity = (track_popularity_sum)/(no_of_tracks);
		}
		else {
			//Handles the corner case when the playlist is empty..
			avg_popularity = 0;
		}
		
		//Logging all the map's values....
		myLog.makeInfoLog("Artist_dir map = "+artist_dir.toString());
		myLog.makeInfoLog("Album_dir map = "+album_dir.toString());
		myLog.makeInfoLog("Genre_dir map = "+genre_dir.toString());
		myLog.makeInfoLog("Era_dir map = "+era_dir.toString());
		
		//Declaring list variables
		ArrayList<String> artist_list = new ArrayList<String>();
		ArrayList<String> album_list = new ArrayList<String>();
		ArrayList<String> genre_list = new ArrayList<String>();
		for(Map.Entry artistEntry: artist_dir.entrySet()) {
			//Verifying that the given artist's frequency is above the fix.THRESHOLD or not..
			double val = (double)(artist_dir.get(artistEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= fix.ARTIST_THRESHOLD) {
				artist_list.add(artistEntry.getKey().toString());
			}
		}
		
		for(Map.Entry albumEntry: album_dir.entrySet()) {
			//Verifying that the given album's frequency is above the fix.THRESHOLD or not..
			double val = (double)(album_dir.get(albumEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= fix.ALBUM_THRESHOLD) {
				album_list.add(albumEntry.getKey().toString());
			}
		}
		
		for(Map.Entry genreEntry: genre_dir.entrySet()) {
			//Verifying that the given genre's frequency is above the fix.THRESHOLD or not..
			double val = (double)(genre_dir.get(genreEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= fix.GENRE_THRESHOLD) {
				genre_list.add(genreEntry.getKey().toString());
			}
		}
		
		//Making the incomplete lists if any of size 5 to fill in the table to avoid out of bounds error...
		//As our user-playlist-info table has got exactly 5 columns to fill..
		while(artist_list.size() < fix.LISTSIZE) {
			artist_list.add("");
		}
		
		while(album_list.size() < fix.LISTSIZE) {
			album_list.add("");
		}
		
		while(genre_list.size() < fix.LISTSIZE) {
			genre_list.add("");
		}
		
		
		//Finally, updating the user-playlist-info table...
		Table table = dynamodb.getTable("user-playlist-info");
		//Map contains the value of the placeholders representing the attributes in the query to come..
		Map<String, String> expressionAttributeName = new HashMap<String, String>();
		//Updating the map with placeholders..
		updatePlaceHolders(expressionAttributeName);
		
		//Update statement..
		UpdateItemSpec updatedItem = new UpdateItemSpec().withPrimaryKey("uid", tuv.getUid(), "pasin", tuv.getPasin())
				.withUpdateExpression("set #p1 = :n, #p2 = :d, #p3 = :al1, #p4 = :al2, #p5 = :al3, #p6 = :al4, #p7 = :al5, #p8 = :ar1, #p9 = :ar2, #p10 = :ar3, #p11 = :ar4, #p12 = :ar5, #p13 = :g1, #p14 = :g2, #p15 = :g3, #p16 = :g4, #p17 = :g5, #p18 = :pop, #p19 = :er1, #p20 = :er2, #p21 = :er3, #p22 = :er4, #p23 = :er5")
				.withValueMap(new ValueMap().withNumber(":n", no_of_tracks).withNumber(":d", track_duration_sum)
						.withString(":al1", album_list.get(0)).withString(":al2", album_list.get(1)).withString(":al3", album_list.get(2)).withString(":al4", album_list.get(3)).withString(":al5", album_list.get(4))
						.withString(":ar1", artist_list.get(0)).withString(":ar2", artist_list.get(1)).withString(":ar3", artist_list.get(2)).withString(":ar4", artist_list.get(3)).withString(":ar5", artist_list.get(4))
						.withString(":g1", genre_list.get(0)).withString(":g2", genre_list.get(1)).withString(":g3", genre_list.get(2)).withString(":g4", genre_list.get(3)).withString(":g5", genre_list.get(4))
						.withNumber(":er1", era_dir.get(0)).withNumber(":er2", era_dir.get(1)).withNumber(":er3", era_dir.get(2)).withNumber(":er4", era_dir.get(3)).withNumber(":er5", era_dir.get(4))
						.withNumber(":pop", avg_popularity)).withNameMap(expressionAttributeName)
				.withReturnValues(ReturnValue.UPDATED_NEW);
		try {
			//Running the update query and storing the result in the outcom variable..
			UpdateItemOutcome outcome = table.updateItem(updatedItem);
			myLog.makeInfoLog("Update Successful! outcome = "+outcome);
		}
		catch(Exception e) {
			//Handling any error during the execution of query..
			myLog.makeInfoLog("Exception e = "+e);
			myLog.makeInfoLog("Updation failed!");
			return 0;
		}
		return 1;
  }
//************************************************* END OF THE METHOD **********************************************************************
  /*
   * Task:- The method takes the map and updates it by adding the placeholders for the update query
   * The method return nothing (void). It just updates the map given to it as input parameter.
   * Parameters of the method :- 1. An empty Map<String, String>
   */
//************************************************ BEGINING OF THE METHOD ******************************************************************
	public static void updatePlaceHolders(Map<String, String> expressionAttributeName) {
		expressionAttributeName.put("#p1", "no-of-tracks");
		expressionAttributeName.put("#p2", "playlist-duration");
		expressionAttributeName.put("#p3", "album-rank1");
		expressionAttributeName.put("#p4", "album-rank2");
		expressionAttributeName.put("#p5", "album-rank3");
		expressionAttributeName.put("#p6", "album-rank4");
		expressionAttributeName.put("#p7", "album-rank5");
		expressionAttributeName.put("#p8", "artist-rank1");
		expressionAttributeName.put("#p9", "artist-rank2");
		expressionAttributeName.put("#p10", "artist-rank3");
		expressionAttributeName.put("#p11", "artist-rank4");
		expressionAttributeName.put("#p12", "artist-rank5");
		expressionAttributeName.put("#p13", "genre-rank1");
		expressionAttributeName.put("#p14", "genre-rank2");
		expressionAttributeName.put("#p15", "genre-rank3");
		expressionAttributeName.put("#p16", "genre-rank4");
		expressionAttributeName.put("#p17", "genre-rank5");
		expressionAttributeName.put("#p18", "popularity");
		expressionAttributeName.put("#p19", "era1");
		expressionAttributeName.put("#p20", "era2");
		expressionAttributeName.put("#p21", "era3");
		expressionAttributeName.put("#p22", "era4");
		expressionAttributeName.put("#p23", "era5");
	}
}
//*************************************************** END OF CODE **************************************************************************
