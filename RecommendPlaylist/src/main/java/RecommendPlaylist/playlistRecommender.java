package RecommendPlaylist;
//Basic utilities import..
import java.io.*;
import java.util.*;

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

public class playlistRecommender implements RequestHandler<Object, String> {
	public String handleRequest(Object obj, Context context) {
		//Creating a ArrayList<String> of the asin at index 0 and uid at index 1..
		ArrayList<String> credentials = new ArrayList<String>();
		credentials = getCredentials(obj);
		String asin = credentials.get(0);
		String uid = credentials.get(1);
		
		//Now we will get the features of the track with the given asin and store it into a HashMap..
		HashMap<String, String> trackDetailsMap = new HashMap<String, String>();
		//Creating the dynamodb instance and client instance..
	  	DynamoDB dynamodb = null;
		try {
			AmazonDynamoDB trackClient = AmazonDynamoDBClientBuilder.standard().build();
			dynamodb = new DynamoDB(trackClient);
		}
		catch(Exception e) {
			return "Failed to create a Client!";
		}
		
		//Getting the track info from the database and storing it into the trackDetailsMap..
		getTrackDetails(trackDetailsMap, dynamodb, asin);
		
		//Getting the list of all the playlists with the given uid and storing into a playlistItems..
		ArrayList<Item> playlistItems = new ArrayList<Item>();
		getPlaylists(playlistItems, dynamodb, uid);
		
		//Now we will generate the score for all the Items in the ArrayList ..the scores will correspond to each playlist index wise in playlistItems
		ArrayList<Integer> scores = new ArrayList<Integer>();
		generateScores(scores, trackDetailsMap, playlistItems);
		
		//An ArrayList of names of playlist..
		ArrayList<String> names = new ArrayList<String>();
		getNames(names, playlistItems);
		//Now we sort the playlistItems ArrayList based on scores ArrayList using a comparator..
		//Creating a map to store key = name of playlist and value = score..
		HashMap<String, Integer> playlistScoreMap = new HashMap<String, Integer>();
		for(int i=0; i<names.size(); i++) {
			playlistScoreMap.put(names.get(i), scores.get(i));
		}
		PlaylistSort sorter = new PlaylistSort(playlistScoreMap);
		Collections.sort(names, sorter);
		//Creating a String of playlist names in order for returning..
		String show = "";
		for(int i=0; i<names.size(); i++) {
			show+=String.valueOf(i+1)+" "+names.get(i)+" Score = "+playlistScoreMap.get(names.get(i))+"\n";
		}
		//Now we will display the playlists in the order of preference..
		//--->Some code here..
		return "Success!-->"+show;
	}
	
	public ArrayList<String> getCredentials(Object obj){
		ArrayList<String> cred = new ArrayList<String>(); 
		String eventObject = obj.toString();
		String tempAsin = eventObject.substring(eventObject.indexOf("=")+1, eventObject.indexOf(","));
		String tempUid = eventObject.substring(eventObject.lastIndexOf("=")+1, eventObject.indexOf("}"));
		cred.add(tempAsin);
		cred.add(tempUid);
		return cred;
	}
	
	public void getTrackDetails(HashMap<String, String> trackDetailsMap, DynamoDB dynamodb, String queryString) {
		Table trackTable = dynamodb.getTable("track-repository");
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
				return;
			}
	}
	
	public void getPlaylists(ArrayList<Item> playlistItems, DynamoDB dynamodb, String queryString) {
		Table playlistTable = dynamodb.getTable("user-playlist-info");
		ItemCollection currentItem = playlistTable.query("uid", Integer.parseInt(queryString));

		Iterator itr = currentItem.iterator();
		try{
			//The Integer.parseInt() and Double.parseDouble() are vulnerable to NumberFormatException..
			while(itr.hasNext()) {
				//Logging the details from each item..
				Item temp = (Item)itr.next();
				playlistItems.add(temp);
			}
		}
	  	catch(Exception e) {
	  		System.out.println("Error is->"+e);
			return;
		}
	}
	
	public void generateScores(ArrayList<Integer> scores, HashMap<String, String> trackDetailsMap, ArrayList<Item> playlistItems) {
		String trackGenre = trackDetailsMap.get("genre");
		String trackArtist = trackDetailsMap.get("artist");
		String trackAlbum = trackDetailsMap.get("album");
		int trackPopularity = Integer.parseInt(trackDetailsMap.get("popularity"));
		String trackDuration = trackDetailsMap.get("duration");
		//Iterating over all the playlistItems
		for(int i=0; i<playlistItems.size(); i++) {
			int score = 0;
			Item temp = playlistItems.get(i);
			//comparing priority 1 item..(GENRE)
			String genre1 = temp.get("genre-rank1").toString();
			String genre2 = temp.get("genre-rank2").toString();
			String genre3 = temp.get("genre-rank3").toString();
			String genre4 = temp.get("genre-rank4").toString();
			String genre5 = temp.get("genre-rank5").toString();
			
			if(trackGenre.equals(genre1)) {
				score += Constants.PRIORITY_1_CONSTANT * Constants.VALUE1;
			}
			else if(trackGenre.equals(genre2)) {
				score += Constants.PRIORITY_1_CONSTANT * Constants.VALUE2;
			}
			else if(trackGenre.equals(genre3)) {
				score += Constants.PRIORITY_1_CONSTANT * Constants.VALUE3;
			}
			else if(trackGenre.equals(genre4)) {
				score += Constants.PRIORITY_1_CONSTANT * Constants.VALUE4;
			}
			else if(trackGenre.equals(genre5)) {
				score += Constants.PRIORITY_1_CONSTANT * Constants.VALUE5;
			}
			
			//Comparing priority 2 item..(ERA)
			int trackYear = Integer.parseInt(trackDetailsMap.get("release"));
			double percentage = 0.0;
			int era1 = Integer.parseInt(temp.get("era1").toString());
			int era2 = Integer.parseInt(temp.get("era2").toString());
			int era3 = Integer.parseInt(temp.get("era3").toString());
			int era4 = Integer.parseInt(temp.get("era4").toString());
			int era5 = Integer.parseInt(temp.get("era5").toString());
			
			int totalEra = era1+era2+era3+era4+era5;
			
			if(trackYear >= 1920 && trackYear <=1940) {
				percentage = (double)era1/(double)totalEra; 
			}
			else if(trackYear > 1940 && trackYear <=1960) {
				percentage = (double)era2/(double)totalEra;
			}
			else if(trackYear > 1960 && trackYear <=1980) {
				percentage = (double)era3/(double)totalEra;
			}
			else if(trackYear > 1980 && trackYear <=2000) {
				percentage = (double)era4/(double)totalEra;
			}
			else{
				percentage = (double)era5/(double)totalEra;
			}
			
			score += Constants.PRIORITY_2_CONSTANT * percentage;
			//Comparing priority 3 item..(ARTIST)..
			
			String artist1 = temp.get("artist-rank1").toString();
			String artist2 = temp.get("artist-rank2").toString();
			String artist3 = temp.get("artist-rank3").toString();
			String artist4 = temp.get("artist-rank4").toString();
			String artist5 = temp.get("artist-rank5").toString();
			
			if(trackArtist.equals(artist1)) {
				score += Constants.PRIORITY_3_CONSTANT * Constants.VALUE1;
			}
			else if(trackArtist.equals(artist2)) {
				score += Constants.PRIORITY_3_CONSTANT * Constants.VALUE2;
			}
			else if(trackArtist.equals(artist3)) {
				score += Constants.PRIORITY_3_CONSTANT * Constants.VALUE3;
			}
			else if(trackArtist.equals(artist4)) {
				score += Constants.PRIORITY_3_CONSTANT * Constants.VALUE4;
			}
			else if(trackArtist.equals(artist5)) {
				score += Constants.PRIORITY_3_CONSTANT * Constants.VALUE5;
			}
			
			//Comparing priority 4 item..(ALBUM)..
			
			String album1 = temp.get("album-rank1").toString();
			String album2 = temp.get("album-rank2").toString();
			String album3 = temp.get("album-rank3").toString();
			String album4 = temp.get("album-rank4").toString();
			String album5 = temp.get("album-rank5").toString();
			
			if(trackAlbum.equals(album1)) {
				score += Constants.PRIORITY_4_CONSTANT * Constants.VALUE1;
			}
			else if(trackAlbum.equals(album2)) {
				score += Constants.PRIORITY_4_CONSTANT * Constants.VALUE2;
			}
			else if(trackAlbum.equals(album3)) {
				score += Constants.PRIORITY_4_CONSTANT * Constants.VALUE3;
			}
			else if(trackAlbum.equals(album4)) {
				score += Constants.PRIORITY_4_CONSTANT * Constants.VALUE4;
			}
			else if(trackAlbum.equals(album5)) {
				score += Constants.PRIORITY_4_CONSTANT * Constants.VALUE5;
			}
			
			//Comparing priority 5 item..(Last Played)
			int lastPlayedValue = 0;
			if(Integer.parseInt(temp.get("last-played").toString()) <= 30) {
				lastPlayedValue = 50;
			}
			else {
				lastPlayedValue = -50;
			}
			score += Constants.PRIORITY_5_CONSTANT * lastPlayedValue;
			//Comparing priority 6 item..(No-of-tracks)..
			//..Some code missing here...
			//..To be taken up later..
			//Comparing priority 7 item..(Popularity)
			int popularity = Integer.parseInt(temp.get("popularity").toString());
			//Code for popularity missing..
			//..To be added later..
			//Comparing priority 8 item..(Duration)..
			//String duration = temp.get("duration").toString();
			//Code for duration missing..
			//..To be written later..
			
			System.out.println("Score for playlist No "+i+" = "+score);
			scores.add(score);
		}
	}
	
	public void getNames(ArrayList<String> names, ArrayList<Item> playlistItems) {
		for(int i=0; i<playlistItems.size(); i++) {
			Item temp = playlistItems.get(i);
			String playlistName = temp.get("playlist-name").toString();
			names.add(playlistName);
			System.out.println("Name no "+i+" = "+playlistName);
		}
	}
}

//END OF CODE