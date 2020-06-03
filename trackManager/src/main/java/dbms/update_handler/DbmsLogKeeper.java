package dbms.update_handler;
/*
 	This class is exclusively for keeping the logs generated out of
 	trackManager.java file. This stores and shows the logs which 
 	come up during the execution of the saild code via a lambda 
 	function when there is a dynamoDB Event.
 */
import java.util.logging.Level;
import java.util.logging.Logger;
public class DbmsLogKeeper{
	//Our Logger instance..
	private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	//For making an info level log..
	static public void makeInfoLog(String s) {
		LOGGER.log(Level.INFO, s);
	}
	
	//For making an fine level log..
	static public void makeFineLog(String s) {
		LOGGER.log(Level.FINE, s);
	}
	
	//For making an Severe level log..
	static public void makeSevereLog(String s) {
		LOGGER.log(Level.SEVERE, s);
	}
}
//End of the code..
