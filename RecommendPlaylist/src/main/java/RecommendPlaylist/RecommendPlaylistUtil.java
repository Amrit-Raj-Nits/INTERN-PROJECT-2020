package recommendplaylist;

import java.util.ArrayList;
import java.util.Iterator;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Table;

import utils.recommendPlaylistLogKeeper;

/*
 *	This class contains the functions which are essential for manipulating the details extracted by PlaylistRecommender.java 
 *	This stores major functions of the mentioned class.
 */
public class RecommendPlaylistUtil{
	
	recommendPlaylistLogKeeper myLog = new recommendPlaylistLogKeeper();
	
	/*
	 * Purpose - To extract asin and uid from an Event Object.
	 * Arguments - Event Object containing asin and uid
	 * Returns - An ArrayList of size 2 with pasin at index 0 and uid at index 1.
	 */
	public ArrayList<String> getCredentials(Object obj){
		ArrayList<String> cred = new ArrayList<String>(); 
		String eventObject = obj.toString();
		String tempAsin = eventObject.substring(eventObject.indexOf("=")+1, eventObject.indexOf(","));
		String tempUid = eventObject.substring(eventObject.lastIndexOf("=")+1, eventObject.indexOf("}"));
		cred.add(tempAsin);
		cred.add(tempUid);
		return cred;
	}
	
	/*
	 * Purpose - To get all the playlists for a given uid and store it in a provided arraylist
	 * Arguments - 1. An ArrayList to fill with playlists 2. DynamoDB instance 3. String containing the uid
	 * Returns - void, populates the provided ArrayList
	 */
	public void getPlaylists(ArrayList<Item> playlistItems, DynamoDB dynamodb, String queryString) {
		Table playlistTable = dynamodb.getTable(Constants.TABLE_NAME2);
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
	/*  Purpose - To get the playlist names and store into a given arraylist
	 *  Arguments - 1. Empty ArrayList<String> 2. ArrayList<Item> containing all playlists
	 *  Returns - void, populates the empty arraylist 
	*/
	public void getNames(ArrayList<String> names, ArrayList<Item> playlistItems) {
		for(int i=0; i<playlistItems.size(); i++) {
			Item temp = playlistItems.get(i);
			String playlistName = temp.get("playlist-name").toString();
			names.add(playlistName);
		}
	}
	/*
	 *  Purpose - To get the scores for genre matches
	 *  Arguments - 1. String 2. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getGenreScore(String trackGenre, Item temp) {
		int score = 0;
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
		
		myLog.logInfo("Score of Genre Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Era matches
	 *  Arguments - 1. int 2. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getEraScore(int trackYear, Item temp) {
		int score = 0;
		double percentage = 0.0;
		int era1 = Integer.parseInt(temp.get("era1").toString());
		int era2 = Integer.parseInt(temp.get("era2").toString());
		int era3 = Integer.parseInt(temp.get("era3").toString());
		int era4 = Integer.parseInt(temp.get("era4").toString());
		int era5 = Integer.parseInt(temp.get("era5").toString());
		
		int totalEra = era1+era2+era3+era4+era5;
		
		//To handle divide by zero error..
		if(totalEra > 0) {
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
		}
		else {
			percentage = 0;
		}
		
		score += Constants.PRIORITY_2_CONSTANT * percentage;

		myLog.logInfo("Score of Era Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Artist matches
	 *  Arguments - 1. String 2. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getArtistScore(String trackArtist, Item temp) {
		int score = 0;
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
		
		myLog.logInfo("Score of Artist Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Album matches
	 *  Arguments - 1. String 2. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getAlbumScore(String trackAlbum, Item temp) {
		int score = 0;
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
		
		myLog.logInfo("Score of Album Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Last played
	 *  Arguments - 1. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getLastPlayedScore(Item temp) {
		int score = 0;
		int lastPlayedValue = 0;
		//Handling the number format exception during parsing to int..
		try {
			if(Integer.parseInt(temp.get("last-played").toString()) != Constants.NEGATIVE_LAST_PLAYED) {
				//The case in which the playlist is non - empty. When empty, value == -1 and should be alloted zero..
				if(Integer.parseInt(temp.get("last-played").toString()) <= Constants.LAST_PLAYED_LOWER_THRESHOLD) {
					lastPlayedValue = Constants.LAST_PLAYED_LOWER_POSITIVE;
				}
				else if(Integer.parseInt(temp.get("last-played").toString()) > Constants.LAST_PLAYED_LOWER_THRESHOLD && 
						Integer.parseInt(temp.get("last-played").toString()) <= Constants.LAST_PLAYED_MIDDLE_THRESHOLD){
					
					lastPlayedValue = Constants.LAST_PLAYED_MIDDLE_POSITIVE;
				}
				else if(Integer.parseInt(temp.get("last-played").toString()) > Constants.LAST_PLAYED_MIDDLE_THRESHOLD && 
						Integer.parseInt(temp.get("last-played").toString()) <= Constants.LAST_PLAYED_UPPER_THRESHOLD) {
					
					lastPlayedValue = Constants.LAST_PLAYED_UPPER_POSITIVE;
				}
				else {
					lastPlayedValue = Constants.LAST_PLAYED_NEGATIVE;
				}
			}
			else {
				lastPlayedValue = 0;
			}
		}
		catch(NumberFormatException e) {
			myLog.logError("Number Format Exception occured = "+e);
			return 0;
		}
		score += Constants.PRIORITY_5_CONSTANT * lastPlayedValue;
		myLog.logInfo("Score of Last-Played Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Popularity proximity
	 *  Arguments - 1. int 2. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getPopularityScore(int trackPopularity, Item temp) {
		int score = 0;
		int popularity = Integer.parseInt(temp.get("popularity").toString());
		//To avoid divide by zero and to ensure denominator is not less than 1..
		if(popularity != 0) {
			// If the playlist is not empty, it will have a non - zero popularity..
			if(Math.abs(trackPopularity - popularity) > 1) {
				score += (double)(1/(double)(Math.abs(trackPopularity - popularity))) * Constants.POPULARITY_CONSTANT;
			}
			else {
				score += Constants.POPULARITY_CONSTANT;
			}
		}
		else {
			// For empty playlist, the popularity is equal to zero..
			score = 0;
		}
		myLog.logInfo("Score of Popularity Match is = "+score);
		return score;
	}
	/*
	 *  Purpose - To get the scores for Duration proximity
	 *  Arguments - 1. Instance of an Item
	 *  Returns - int - Score 
	*/
	public int getDurationScore(Item temp) {
		int score = 0;
		int duration = temp.getInt("playlist-duration");
		score += duration * Constants.DURATION_CONSTANT;
		myLog.logInfo("Score of Duration Match is = "+score);
		return score;
	}
}
