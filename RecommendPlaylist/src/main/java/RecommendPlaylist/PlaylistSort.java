package RecommendPlaylist;
/*
	This class implements comparator class so as to sort the ArrayList of Names of playlists with respect to its score
	The Map has the playlist names as keys and scores->tieScores as values (arraylist). The sorting is done in descending 
	order of the scores.
*/
import java.io.*;
import java.util.HashMap;
import java.util.Comparator;
import java.util.ArrayList;
public class PlaylistSort implements Comparator<String>{
	private HashMap<String, ArrayList<Integer>> frequency = new HashMap<String, ArrayList<Integer>>();
	public PlaylistSort(HashMap<String, ArrayList<Integer>> arr) {
		frequency = arr;
	}
	public int compare(String s1, String s2) {
		if(frequency.get(s1).get(0) < frequency.get(s2).get(0)) {
			return 1;
		}
		else if(frequency.get(s1).get(0) > frequency.get(s2).get(0)) {
			return -1;
		}
		else {
			//If two playlists have the same score, we need to break the tie..so we add the tieBreaker scores
			//to the calculated scores and then re-compare.
			if((frequency.get(s1).get(0) + frequency.get(s1).get(1)) < (frequency.get(s2).get(0) + frequency.get(s2).get(1))) {
				return 1;
			}
			else if((frequency.get(s1).get(0) + frequency.get(s1).get(1)) > (frequency.get(s2).get(0) + frequency.get(s2).get(1))) {
				return -1;
			}
			else {
				//If they are still the same, we declare them as equally likely..
				return 0;
			}
		}
	}
}
