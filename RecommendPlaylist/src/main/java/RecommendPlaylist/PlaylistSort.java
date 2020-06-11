package RecommendPlaylist;
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
			if((frequency.get(s1).get(0) + frequency.get(s1).get(1)) < (frequency.get(s2).get(0) + frequency.get(s2).get(1))) {
				return 1;
			}
			else if((frequency.get(s1).get(0) + frequency.get(s1).get(1)) > (frequency.get(s2).get(0) + frequency.get(s2).get(1))) {
				return -1;
			}
			else {
				return 0;
			}
		}
	}
}
