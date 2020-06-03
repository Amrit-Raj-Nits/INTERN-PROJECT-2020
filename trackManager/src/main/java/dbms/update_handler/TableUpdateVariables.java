/*
 * This is a POJO class to store the variables which will be used to update the tables
 */
package dbms.update_handler;
import java.util.HashMap;
public class TableUpdateVariables{
	private String new_asin;
	private String new_pasin;
	private long new_uid;
	private String new_album;
	private String new_artist;
	private String new_duration;
	private String new_name;
	private int new_popularity;
	private String new_release;
	private String new_genre;
	
	//Constructor..
	public TableUpdateVariables(HashMap<String, String> map) {
		new_asin = map.get("asin");
		new_pasin = map.get("pasin");
		new_uid = Long.parseLong(map.get("uid"));
		new_album = map.get("album");
		new_artist = map.get("artist");
		new_duration = map.get("duration");
		new_name = map.get("name");
		new_popularity = Integer.parseInt(map.get("popularity"));
		new_release = map.get("release");
		new_genre = map.get("genre");
		
	}
	
	//Defining the getter methods..
	public String getAsin() {
		return new_asin;
	}
	public String getPasin() {
		return new_pasin;
	}
	public long getUid() {
		return new_uid;
	}
	public String getAlbum() {
		return new_album;
	}
	public String getArtist() {
		return new_artist;
	}
	public String getGenre() {
		return new_genre;
	}
	public String getDuration() {
		return new_duration;
	}
	public String getRelease() {
		return new_release;
	}
	public int getPopularity() {
		return new_popularity;
	}
	public String getName() {
		return new_name;
	}
} 
