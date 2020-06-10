package utils;
/*
 	This class is exclusively for keeping the logs generated out of
 	playlistRecommender.java file. This stores and shows the logs which 
 	come up during the execution of the said code via a lambda 
 	function when there is a dynamoDB Event or via an api call.
 */
import java.util.logging.Level;
import java.util.logging.Logger;
public class recommendPlaylistLogKeeper{
	//Our Logger instance..
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	//For making an info level log..
	public void logInfo(String s) {
		LOGGER.log(Level.INFO, s);
	}
	
	//For making an fine level log..
	public void logFine(String s) {
		LOGGER.log(Level.FINE, s);
	}
	
	//For making an Error level log..
	public void logError(String s) {
		LOGGER.log(Level.SEVERE, s);
	}
}
//End of the code..