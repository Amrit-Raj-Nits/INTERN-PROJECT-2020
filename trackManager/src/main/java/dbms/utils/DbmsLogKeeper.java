package dbms.utils;
/*
 	This class is exclusively for keeping the logs generated out of
 	trackManager.java file. This stores and shows the logs which 
 	come up during the execution of the saild code via a lambda 
 	function when there is a dynamoDB Event.
	--
 */
import java.util.logging.Level;
import java.util.logging.Logger;
public class DbmsLogKeeper{
	//Our Logger instance..
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	//For making an info level log..
	static public void logInfo(String s) {
		LOGGER.log(Level.INFO, s);
	}
	
	//For making an fine level log..
	static public void logFine(String s) {
		LOGGER.log(Level.FINE, s);
	}
	
	//For making an Error level log..
	static public void logError(String s) {
		LOGGER.log(Level.SEVERE, s);
	}
}
//End of the code..
