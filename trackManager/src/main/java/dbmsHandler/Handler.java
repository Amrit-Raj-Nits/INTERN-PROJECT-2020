//Code:- Amrit Raj
package dbmsHandler;
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

public class Handler implements RequestHandler<Object, String> {

//FIXED VSRIABLES DECLARATION....
static int INVALID_INDEX = -1; //IF SEARCH ITEM NOT FOUND
static int INITIAL_ZERO = 0; //INITIALIZATION OF BASIC VARIABLES TO ZERO
static int THRESHOLD = 20; //20% THRESHOLD
static int LISTSIZE = 5; //SIZE OF THE LIST FOR THE 5 COLUMNS

//Creating an instance of our DbmsLogKeeper class for Logging in this file...
static DbmsLogKeeper myLog = new DbmsLogKeeper();

  public String handleRequest(Object obj, Context context) {
	//Logging the begining of the LOG..  
  	myLog.markBegining("*** LOGGING STARTS FOR HANDLER.JAVA ***");
  		//Logging the event details..
  		myLog.makeLog("Event details = "+ obj.toString());
		String eventName = "";
  	
		// Converting the Object into String..
		String event = obj.toString();

		//Setting up the cases when the event is not a dynamoDB event..
		if (event.indexOf("Keys") == INVALID_INDEX || event.indexOf("SequenceNumber") == INVALID_INDEX || event.indexOf("Keys") >= event.indexOf("SequenceNumber")) {
			System.out.println("Invalid Entry!");
		}
		else {
			String eventNameGetter[] = event.substring(event.indexOf("eventName"), event.indexOf(",", event.indexOf("eventName"))).split("=");
			eventName = eventNameGetter[1].trim();
			System.out.println("Event name = "+ eventName);
		}
		
		//Now we have the event type which can either be INSERT or REMOVE based on the problem
		//statement for our project...Now we handle both the cases by separate functions
		int RESULT = 0;
		if(eventName.equalsIgnoreCase("INSERT")) {
			handleInsert(event);
			RESULT = 1; //Denotes success of the operation..
		}
		else if(eventName.equalsIgnoreCase("REMOVE")) {
			handleRemove(event);
			RESULT = 1; //Denotes success of the operation..
		}
		else {
			RESULT = 0; //Denotes Faliure of the operation..
			System.out.println("This is an invalid Event for the action!");
		}
		
		//iF THE EVENT FOUND..
		if(RESULT == 1) {
			//Printing success message if the transaction is successful..
			System.out.println("Transaction Completed Successfully!");
			return "Success!";
		}
		else {
			//If the transaction fails..
			System.out.println("Transaction Failed!");
			return "Faliure!";
		}
	}
  
//************************************************************** BEGINING OF METHOD **************************************************************
 /*
  * This method handles the INSERT event for our code.
  * The method return nothing (void).
  * Parameters of the method :- 1. A DynamoDB event in the form of a String from the playlist-tracks table for reading the streams.
  * Task:- The method reads the stream from the passed event, finds the details of all the tracks belonging to the playlist to which the INSERT 
  * event correseponds and updates the corresponding item (playlist details) in the user-playlist-info.  
  */
  private static void handleInsert(String event) {
  	//The NewImage contents lie between 'NewImage' tag and 'SequenceNumber' tag..So extracting the feature details from the event String
  	String newImageGetter = event.substring(event.indexOf("NewImage"), event.indexOf(", SequenceNumber"));
  	String newImage = newImageGetter.trim().substring(newImageGetter.indexOf("=")+2, newImageGetter.length()-1);
  	if(newImage.length() == 0) {
  		//A case which corresponds to an erranous case..
  		System.out.println("Empty newImage!");
  	}
  	else {
  		//Logging the new Image...
  		myLog.makeLog("newImage = "+newImage);
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
  		myLog.makeLog("Map = "+map.toString());
  		
  		 //Calling the method to update the details based on the details stored in the map..return 1 if success and 0 if failed.
  		 int success = updateTable(map); 	
  		 if(success == 1) {
  			//Notifying the successful completion of the transaction..
  			System.out.println("Entry Successful!");
  		 }
  		 else {
  			 //Notifying the failure of updation..
  			 System.out.println("updateMethod failed to update the table!");
  		 }
  	}
  }
//***************************************************************END OF THE METHOD ***************************************************************
  /*
   * This method handles the REMOVE event for our code.
   * The method return nothing (void).
   * Parameters of the method :- 1. A DynamoDB event in the form of a String from the playlist-tracks table for reading the streams.
   * Task:- The method reads the stream from the passed event, finds the details of all the tracks belonging to the playlist to which the REMOVE 
   * event correseponds and updates the corresponding item (playlist details) in the user-playlist-info.  
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
	    	System.out.println("Empty newImage!");
	    }
	    else {
	    	//Logging the new Image...
	    	myLog.makeLog("newImage = "+newImage);
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
	    	myLog.makeLog("Map = "+map.toString());
	    	
	    	//Calling the method to update the details based on the details stored in the map..return 1 if success and 0 if failed.
	    	int success = updateTable(map); 	
	  		 if(success == 1) {
	  			//Notifying the successful completion of the transaction..
	  			System.out.println("Removal Successful!");
	  		 }
	  		 else {
	  			//Notifying the Failed update..
	  			 System.out.println("updateMethod failed to update the table!");
	  		 }
	    }
  }
//*************************************************** END OF THE METHOD ********************************************************************
  /*
   * This method handles the UPDATE part for Both our methods handleInsert and handleRemove.
   * The method return int (either 1 = success/ 0 = Failure).
   * Parameters of the method :- 1. HashMap<String, String> containing the keys = attributes(feature names) and values = value of the featuress.
   * Task:- The method takes the map and updates the user-playlist-info table according to the attribute names and the values. Also, the  
   * method computes the era (from the release date) and other such variables before updating the table. To update the table, it first reads
   * the previous contents of the concerened item of the table and modifies it then.
   */
//************************************************ BEGINING OF THE METHOD ****************************************************************** 
  public static int updateTable(HashMap<String, String> map) {
	//Getting all the variables from the map...
		String new_asin = map.get("asin");
		String new_pasin = map.get("pasin");
		long new_uid = Long.parseLong(map.get("uid"));
		String new_album = map.get("album");
		String new_artist = map.get("artist");
		String new_duration = map.get("duration");
		String new_name = map.get("name");
		int new_popularity = Integer.parseInt(map.get("popularity"));
		String new_release = map.get("release");
		String new_genre = map.get("genre");
		
		//Logging the variables for acknowlegement..
		myLog.makeLog("new_asin = "+new_asin);
		myLog.makeLog("new_pasin = "+new_pasin);
		myLog.makeLog("new_uid = "+String.valueOf(new_uid));
		myLog.makeLog("new_album = "+new_album);
		myLog.makeLog("new_artist = "+new_artist);
		myLog.makeLog("new_duration = "+new_duration);
		myLog.makeLog("new_name = "+new_name);
		myLog.makeLog("new_popularity = "+String.valueOf(new_popularity));
		myLog.makeLog("new_release = "+new_release);
		myLog.makeLog("new_genre = "+new_genre);
		//Notifying the steps which faced issues while coding into the log..
	
			
		//Now reading the contents of the playlist table to update it as per the new entry..
		AmazonDynamoDB playlistClient = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB playlistDynamodb = new DynamoDB(playlistClient);
		Table playlistTable = playlistDynamodb.getTable("playlist-tracks");
		ItemCollection currentItem = playlistTable.query("pasin", new_pasin);
		//Iterating over all the items received from the query..
		Iterator itr = currentItem.iterator();
		int no_of_tracks = INITIAL_ZERO;
		int track_duration_sum = INITIAL_ZERO;
		int track_popularity_sum = INITIAL_ZERO;
		int avg_popularity = INITIAL_ZERO;
		
		HashMap<String, Integer> artist_dir = new HashMap<String, Integer>();
		HashMap<String, Integer> album_dir = new HashMap<String, Integer>();
		HashMap<String, Integer> genre_dir = new HashMap<String, Integer>();
		HashMap<Integer, Integer> era_dir = new HashMap<Integer, Integer>();
		era_dir.put(0, INITIAL_ZERO);
		era_dir.put(1, INITIAL_ZERO);
		era_dir.put(2, INITIAL_ZERO);
		era_dir.put(3, INITIAL_ZERO);
		era_dir.put(4, INITIAL_ZERO);
		
		while(itr.hasNext()) {
			//Logging the details from each item..
			Item temp = (Item)itr.next();
			myLog.makeLog("Item = "+temp.toString());
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
			artist_dir.put(temp.get("artist").toString(), artist_dir.getOrDefault(temp.get("artist").toString(), INITIAL_ZERO)+1);
			album_dir.put(temp.get("album").toString(), album_dir.getOrDefault(temp.get("album").toString(), INITIAL_ZERO)+1);
			genre_dir.put(temp.get("genre").toString(), genre_dir.getOrDefault(temp.get("genre").toString(), INITIAL_ZERO)+1);
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
			avg_popularity = INITIAL_ZERO;
		}
		
		//Logging all the map's values....
		myLog.makeLog("Artist_dir map = "+artist_dir.toString());
		myLog.makeLog("Album_dir map = "+album_dir.toString());
		myLog.makeLog("Genre_dir map = "+genre_dir.toString());
		myLog.makeLog("Era_dir map = "+era_dir.toString());
		
		//Declaring list variables
		ArrayList<String> artist_list = new ArrayList<String>();
		ArrayList<String> album_list = new ArrayList<String>();
		ArrayList<String> genre_list = new ArrayList<String>();
		for(Map.Entry artistEntry: artist_dir.entrySet()) {
			//Verifying that the given artist's frequency is above the threshold or not..
			double val = (double)(artist_dir.get(artistEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= THRESHOLD) {
				artist_list.add(artistEntry.getKey().toString());
			}
		}
		
		for(Map.Entry albumEntry: album_dir.entrySet()) {
			//Verifying that the given album's frequency is above the threshold or not..
			double val = (double)(album_dir.get(albumEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= THRESHOLD) {
				album_list.add(albumEntry.getKey().toString());
			}
		}
		
		for(Map.Entry genreEntry: genre_dir.entrySet()) {
			//Verifying that the given genre's frequency is above the threshold or not..
			double val = (double)(genre_dir.get(genreEntry.getKey().toString())/((double)no_of_tracks))*100;
			if(val >= THRESHOLD) {
				genre_list.add(genreEntry.getKey().toString());
			}
		}
		
		//Making the incomplete lists if any of size 5 to fill in the table to avoid out of bounds error...
		//As our user-playlist-info table has got exactly 5 columns to fill..
		while(artist_list.size() < LISTSIZE) {
			artist_list.add("");
		}
		
		while(album_list.size() < LISTSIZE) {
			album_list.add("");
		}
		
		while(genre_list.size() < LISTSIZE) {
			genre_list.add("");
		}
		
		//Logging the final values and lists to be updated into user-playlist-info table...
		myLog.makeLog("artist_list = "+artist_list.toString());
		myLog.makeLog("album_list = "+album_list.toString());
		myLog.makeLog("genre_list = "+genre_list.toString());
		
		//Finally, updating the user-playlist-info table...
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamodb = new DynamoDB(client);
		Table table = dynamodb.getTable("user-playlist-info");
		//Map contains the value of the placeholders representing the attributes in the query to come..
		Map<String, String> expressionAttributeName = new HashMap<String, String>();
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
		//Update statement..
		UpdateItemSpec updatedItem = new UpdateItemSpec().withPrimaryKey("uid", new_uid, "pasin", new_pasin)
				.withUpdateExpression("set #p1 = :n, #p2 = :d, #p3 = :al1, #p4 = :al2, #p5 = :al3, #p6 = :al4, #p7 = :al5, #p8 = :ar1, #p9 = :ar2, #p10 = :ar3, #p11 = :ar4, #p12 = :ar5, #p13 = :g1, #p14 = :g2, #p15 = :g3, #p16 = :g4, #p17 = :g5, #p18 = :pop, #p19 = :er1, #p20 = :er2, #p21 = :er3, #p22 = :er4, #p23 = :er5")
				.withValueMap(new ValueMap().withNumber(":n", no_of_tracks).withNumber(":d", track_duration_sum)
						.withString(":al1", album_list.get(0)).withString(":al2", album_list.get(1)).withString(":al3", album_list.get(2)).withString(":al4", album_list.get(3)).withString(":al5", album_list.get(4))
						.withString(":ar1", artist_list.get(0)).withString(":ar2", artist_list.get(1)).withString(":ar3", artist_list.get(2)).withString(":ar4", artist_list.get(3)).withString(":ar5", artist_list.get(4))
						.withString(":g1", genre_list.get(0)).withString(":g2", genre_list.get(1)).withString(":g3", genre_list.get(2)).withString(":g4", genre_list.get(3)).withString(":g5", genre_list.get(4))
						.withNumber(":er1", era_dir.get(0)).withNumber(":er2", era_dir.get(1)).withNumber(":er3", era_dir.get(2)).withNumber(":er4", era_dir.get(3)).withNumber(":er5", era_dir.get(4))
						.withNumber(":pop", avg_popularity)).withNameMap(expressionAttributeName)
				.withReturnValues(ReturnValue.UPDATED_NEW);
		//Logging the query..
		myLog.makeLog("Executing update query = "+updatedItem.toString());
		try {
			//Running the update query and storing the result in the outcom variable..
			UpdateItemOutcome outcome = table.updateItem(updatedItem);
			System.out.println("Update Successful! outcome = "+outcome);
		}
		catch(Exception e) {
			//Handling any error during the execution of query..
			System.out.println("Exception e = "+e);
			System.out.println("Updation failed!");
			return 0;
		}
		return 1;
  }
//************************************************* END OF THE METHOD **********************************************************************
}
//*************************************************** END OF CODE **************************************************************************
