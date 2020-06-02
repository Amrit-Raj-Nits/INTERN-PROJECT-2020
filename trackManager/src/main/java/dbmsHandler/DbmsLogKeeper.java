package dbmsHandler;
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
	//Constructor
	public DbmsLogKeeper() {
		//Nothing to do..
	}
	//Marking the beginning of the Logging segment..
	public void markBegining(String s) {
		LOGGER.log(Level.INFO, s);
	}
	//For making logs other than the first Log..
	public void makeLog(String s) {
		LOGGER.log(Level.INFO, s);
	}
}
//End of the code..
