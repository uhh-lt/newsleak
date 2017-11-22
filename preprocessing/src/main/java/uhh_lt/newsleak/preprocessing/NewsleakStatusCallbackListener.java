package uhh_lt.newsleak.preprocessing;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

public class NewsleakStatusCallbackListener implements StatusCallbackListener {

	private static final int ENTITY_CNT_FOR_LOG = 1;
	private Logger logger;
	public boolean isProcessing = true;
	private int entityProcessCount;

	public NewsleakStatusCallbackListener(Logger logger) {
		super();
		this.logger = logger;
	}

	public void resumed() {
		logger.log(Level.INFO, "CPM resumed");
	}
	
	public void paused() {
		logger.log(Level.INFO, "CPM paused");
	}
	
	public void initializationComplete() {
		logger.log(Level.INFO, "CPM initialization completed");
	}
	
	public void collectionProcessComplete() {
		logger.log(Level.INFO, "CPM processing completed");
		isProcessing = false;
	}
	
	public void batchProcessComplete() {
		logger.log(Level.INFO, "CPM batch process completed");
	}
	
	public void aborted() {
		logger.log(Level.SEVERE, "CPM aborted");
		isProcessing = false;
	}
	
	public void entityProcessComplete(CAS arg0, EntityProcessStatus arg1) {
		entityProcessCount++;
		if (entityProcessCount % ENTITY_CNT_FOR_LOG == 0) {
			logger.log(Level.INFO, "CPM entity process completed - " + entityProcessCount + " entities");
		}
	}

}
