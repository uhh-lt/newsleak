package uhh_lt.newsleak.preprocessing;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * The listener interface for receiving newsleakStatusCallback events.
 * The class that is interested in processing a newsleakStatusCallback
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addNewsleakStatusCallbackListener<code> method. When
 * the newsleakStatusCallback event occurs, that object's appropriate
 * method is invoked.
 *
 * @see NewsleakStatusCallbackEvent
 */
public class NewsleakStatusCallbackListener implements StatusCallbackListener {

	/** The Constant ENTITY_CNT_FOR_LOG. */
	private static final int ENTITY_CNT_FOR_LOG = 1;
	
	/** The logger. */
	private Logger logger;
	
	/** The is processing. */
	private boolean isProcessing = true;
	
	/** The entity process count. */
	private int entityProcessCount;

	/**
	 * Instantiates a new newsleak status callback listener.
	 *
	 * @param logger the logger
	 */
	public NewsleakStatusCallbackListener(Logger logger) {
		super();
		this.logger = logger;
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#resumed()
	 */
	public void resumed() {
		logger.log(Level.INFO, "CPM resumed");
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#paused()
	 */
	public void paused() {
		logger.log(Level.INFO, "CPM paused");
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#initializationComplete()
	 */
	public void initializationComplete() {
		logger.log(Level.INFO, "CPM initialization completed");
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#collectionProcessComplete()
	 */
	public void collectionProcessComplete() {
		logger.log(Level.INFO, "CPM processing completed");
		isProcessing = false;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#batchProcessComplete()
	 */
	public void batchProcessComplete() {
		logger.log(Level.INFO, "CPM batch process completed");
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
	 */
	public void aborted() {
		logger.log(Level.SEVERE, "CPM aborted");
		isProcessing = false;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.collection.StatusCallbackListener#entityProcessComplete(org.apache.uima.cas.CAS, org.apache.uima.collection.EntityProcessStatus)
	 */
	public void entityProcessComplete(CAS arg0, EntityProcessStatus arg1) {
		entityProcessCount++;
		if (entityProcessCount % ENTITY_CNT_FOR_LOG == 0) {
			logger.log(Level.INFO, "CPM entity process completed - " + entityProcessCount + " entities");
		}
	}

	/**
	 * Checks if is processing.
	 *
	 * @return true, if is processing
	 */
	public boolean isProcessing() {
		return isProcessing;
	}
	
	

}
