package recommendplaylist;
//Basic utilities import..
import java.io.*;
import java.util.*;
import utils.recommendPlaylistLogKeeper;
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

public class PlaylistRecommender implements RequestHandler<Object, String> {
	recommendPlaylistLogKeeper myLog = new recommendPlaylistLogKeeper();
	RecommendPlaylistUtil rputil = new RecommendPlaylistUtil();
	/*
	 * Purpose - This is the main method that is called by the Lambda function on API trigger.
	 * Arguments - 1. Event object obj consisting of asin and uid 2. Context
	 * Returns  - A String value which consists of the playlists name for a given uid in sorted order of liklihood of a given track going into it.
	*/
	public String handleRequest(Object obj, Context context) {
		//Creating a ArrayList<String> of the asin at index 0 and uid at index 1..
		ArrayList<String> credentials = new ArrayList<String>();
		credentials = rputil.getCredentials(obj);
		String asin = credentials.get(0);
		String uid = credentials.get(1);
		myLog.logInfo("Asin = "+asin+" Uid = "+uid);
		//Now we will get the features of the track with the given asin and store it into a HashMap..
		HashMap<String, String> trackDetailsMap = new HashMap<String, String>();
		//Creating the dynamodb instance and client instance..
	  	DynamoDB dynamodb = null;
		try {
			AmazonDynamoDB trackClient = AmazonDynamoDBClientBuilder.standard().build();
			dynamodb = new DynamoDB(trackClient);
		}
		catch(Exception e) {
			myLog.logError("Error --> "+e);
			return "Failed to create a Client!";
		}

		//Getting the track info from the database and storing it into the trackDetailsMap..
		fetchTrackDetails(trackDetailsMap, dynamodb, asin);
		myLog.logInfo("Track Details = "+trackDetailsMap);
		
		//Getting the list of all the playlists with the given uid and storing into a playlistItems..
		ArrayList<Item> playlistItems = new ArrayList<Item>();
		rputil.getPlaylists(playlistItems, dynamodb, uid);
		
		//An ArrayList of names of playlist..
		ArrayList<String> names = new ArrayList<String>();
		rputil.getNames(names, playlistItems);
		
		//Creating a map to store key = name of playlist and value = ArrayList<Integer> with 2 values each, at index 0 = score, 1 = tieBreakerScore..
		HashMap<String, ArrayList<Integer>> playlistScoreMap = new HashMap<String, ArrayList<Integer>>();
		
		//Now we will generate the score for all the Items in the ArrayList ..the scores will correspond to each playlist index wise in playlistItems
		ArrayList<Integer> scores = new ArrayList<Integer>();
		ArrayList<Integer> tieBreakerScores = new ArrayList<Integer>();
		generateScores(playlistScoreMap, trackDetailsMap, playlistItems);
		
		myLog.logInfo("Playlists in the given uid = "+names);
		myLog.logInfo("Scores of corresponding Playlists = "+scores);
		myLog.logInfo("Tie Breaker Scores of corresponding Playlists = "+tieBreakerScores);
		
		//Now we sort the playlistItems ArrayList based on scores ArrayList using a comparator..
		PlaylistSort sorter = new PlaylistSort(playlistScoreMap);
		Collections.sort(names, sorter);
		//Creating a String of playlist names in order for returning..
		String show = "";
		for(int i=0; i<names.size(); i++) {
			show+=String.valueOf(i+1)+" "+names.get(i)+" Score = "+playlistScoreMap.get(names.get(i))+"\\n";
		}
		//Now we will display the playlists in the order of preference..
		//--->Some code here..

		myLog.logInfo("Ranked Playlists = "+show);
		//If there is atleast one playlist in the User's account...else ask him/her to create a playlist..
		if(show.length()>0) {
			return "Success!-->"+show;
		}
		else {
			return "Success--> You do not have any existing playlists, Kindly create one.";
		}
	}

	/*
	 * Purpose - To Get the track details of the track given by the asin of the track and store it into a given HashMap
	 * Arguments - 1. A HashMap which will be populated 2. DynamoDB instance 3. A query Sting with asin
	 * Returns - Nothing, rater populates the HashMap passed.
	*/
	public void fetchTrackDetails(HashMap<String, String> trackDetailsMap, DynamoDB dynamodb, String queryString) {
		Table trackTable = dynamodb.getTable(Constants.TABLE_NAME1);
		ItemCollection currentItem = trackTable.query("asin", queryString);
		Iterator itr = currentItem.iterator();
		
		try{
			//The Integer.parseInt() and Double.parseDouble() are vulnerable to NumberFormatException..
				while(itr.hasNext()) {
					//Logging the details from each item..
					Item temp = (Item)itr.next();
					String album = temp.get("album").toString();
					String artist = temp.get("artist").toString();
					String duration = temp.get("duration").toString();
					String genre = temp.get("genre").toString();
					String name = temp.get("name").toString();
					String popularity = temp.get("popularity").toString();
					String releaseDetails[] = temp.get("release").toString().split(" ");
					String release = releaseDetails[2];
					
					//Updating the map..
					trackDetailsMap.put("album", album);
					trackDetailsMap.put("artist", artist);
					trackDetailsMap.put("duration", duration);
					trackDetailsMap.put("genre", genre);
					trackDetailsMap.put("name", name);
					trackDetailsMap.put("popularity", popularity);
					trackDetailsMap.put("release", release);
					
				}
			}
		  	catch(Exception e) {
				myLog.logError("Exception = "+e);
				return;
			}
	}
	/*
	 * Purpose - To find the scores of a all playlists for a given uid and with respect to a given asin track
	 * Arguments - 1. HashMap<String, ArrayList<Integer>> 2.HashMap<String, String> containing details of the track 3. ArrayList<Item> containing all playlists for a uid
	 * Returns - void, populates the playlistScoreMap HashMap<String, ArrayList<Integer>>  
	*/
	public void generateScores(HashMap<String, ArrayList<Integer>> playlistScoreMap, HashMap<String, String> trackDetailsMap, ArrayList<Item> playlistItems) {
		//Storing the track features into variables..
		String trackGenre = trackDetailsMap.get("genre");
		String trackArtist = trackDetailsMap.get("artist");
		String trackAlbum = trackDetailsMap.get("album");
		int trackPopularity = Integer.parseInt(trackDetailsMap.get("popularity"));
		String trackDuration = trackDetailsMap.get("duration");
		int trackYear = Integer.parseInt(trackDetailsMap.get("release"));
		
		//To break the tie, we would need the least four priority feature's score separately, so we will use a separate ArrayList to store it..
		//Iterating over all the playlistItems
		for(int i=0; i<playlistItems.size(); i++) {
			int score = 0;
			int tieBreakerScore = 0;
			Item temp = playlistItems.get(i);
			//Names of the playlist..
			String playlistName = temp.get("playlist-name").toString();
			//comparing priority 1 item..(GENRE)
			score += rputil.getGenreScore(trackGenre, temp);
			//Comparing priority 2 item..(ERA)
			score += rputil.getEraScore(trackYear, temp);
			//Comparing priority 3 item..(ARTIST)..
			score += rputil.getArtistScore(trackArtist, temp);
			//Comparing priority 4 item..(ALBUM)..
			score += rputil.getAlbumScore(trackAlbum, temp);
			//Comparing priority 5 item..(Last Played)
			score += rputil.getLastPlayedScore(temp);
			tieBreakerScore += rputil.getLastPlayedScore(temp);
			//Comparing priority 6 item..(No-of-tracks)..
			score += Constants.NO_OF_TRACKS_CONSTANT * Integer.parseInt(temp.get("no-of-tracks").toString()); 
			tieBreakerScore += Constants.NO_OF_TRACKS_CONSTANT * Integer.parseInt(temp.get("no-of-tracks").toString());
			//Comparing priority 7 item..(Popularity)
			score += rputil.getPopularityScore(trackPopularity, temp);
			tieBreakerScore += rputil.getPopularityScore(trackPopularity, temp);
			//Comparing priority 8 item..(Duration)..
			score += rputil.getDurationScore(temp);
			tieBreakerScore += rputil.getDurationScore(temp);
			
			myLog.logInfo("Total Score for playlist at index = "+i+" is = "+score);
			myLog.logInfo("Total Tie Breaker Score for playlist at index = "+i+" is = "+tieBreakerScore);
			
			//To temporarily store the score and tie score before putting into the HashMap...
			ArrayList<Integer> tempStore = new ArrayList<Integer>();
			tempStore.add(score);
			tempStore.add(tieBreakerScore);
			//Updating the map with the playlist name as key and list of score and tie scores..
			playlistScoreMap.put(playlistName, tempStore);
		}
	}
	
}
//END OF CODE
