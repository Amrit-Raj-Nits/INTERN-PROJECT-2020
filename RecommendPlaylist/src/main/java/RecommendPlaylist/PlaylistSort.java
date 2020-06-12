package recommendplaylist;
/*
This class implements comparator class so as to sort the ArrayList of Names of playlists with respect to its score
The Map has the playlist names as keys and scores->tieScores as values (arraylist). The sorting is done in descending 
order of the scores.
*/
import java.io.*;
import java.util.HashMap;
import utils.recommendPlaylistLogKeeper;
import java.util.Comparator;
import java.util.ArrayList;
public class PlaylistSort implements Comparator<String>{
	
	recommendPlaylistLogKeeper myLog = new recommendPlaylistLogKeeper();
	
	private HashMap<String, ArrayList<Integer>> frequency = new HashMap<String, ArrayList<Integer>>();
	public PlaylistSort(HashMap<String, ArrayList<Integer>> arr) {
		myLog.logInfo("Creating a Comparator Map and copying original map here for Sorting");;
		frequency = arr;
	}
	public int compare(String s1, String s2) {
		if(frequency.get(s1).get(Constants.SCORES_INDEX) < frequency.get(s2).get(Constants.SCORES_INDEX)) {
			return 1;
		}
		else if(frequency.get(s1).get(Constants.SCORES_INDEX) > frequency.get(s2).get(Constants.SCORES_INDEX)) {
			return -1;
		}
		else {
			//If two playlists have the same score, we need to break the tie..so we compare the tie scores..
			if((frequency.get(s1).get(Constants.TIE_INDEX)) < (frequency.get(s2).get(Constants.TIE_INDEX))) {
				return 1;
			}
			else if((frequency.get(s1).get(Constants.TIE_INDEX)) > (frequency.get(s2).get(Constants.TIE_INDEX))) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
}
