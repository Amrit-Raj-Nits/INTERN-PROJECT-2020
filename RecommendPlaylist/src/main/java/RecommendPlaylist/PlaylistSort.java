package RecommendPlaylist;
import java.io.*;
import java.util.HashMap;
import java.util.Comparator;
public class PlaylistSort implements Comparator<String>{
	private HashMap<String, Integer> frequency = new HashMap<String, Integer>();
	public PlaylistSort(HashMap<String, Integer> arr) {
		frequency = arr;
	}
	public int compare(String s1, String s2) {
		if(frequency.get(s1) < frequency.get(s2)) {
			return 1;
		}
		else if(frequency.get(s1) > frequency.get(s2)) {
			return -1;
		}
		else {
			return 0;
		}
	}
}