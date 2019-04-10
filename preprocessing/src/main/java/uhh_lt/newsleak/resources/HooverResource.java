package uhh_lt.newsleak.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.reader.HooverElasticsearchReader. This resource connects
 * directly to Hoover's elasticsearch index via a Jest client.
 */
public class HooverResource extends Resource_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant HOOVER_DOCUMENT_TYPE. */
	public static final String HOOVER_DOCUMENT_TYPE = "doc";

	/** The Constant PARAM_HOST. */
	public static final String PARAM_HOST = "mHost";

	/** The m host. */
	@ConfigurationParameter(name = PARAM_HOST)
	private String mHost;

	/** The Constant PARAM_PORT. */
	public static final String PARAM_PORT = "mPort";

	/** The m port. */
	@ConfigurationParameter(name = PARAM_PORT)
	private Integer mPort;

	/** The Constant PARAM_INDEX. */
	public static final String PARAM_INDEX = "mIndex";

	/** The m index. */
	@ConfigurationParameter(name = PARAM_INDEX)
	private String mIndex;

	/** The Constant PARAM_CLUSTERNAME. */
	public static final String PARAM_CLUSTERNAME = "mClustername";

	/** The m clustername. */
	@ConfigurationParameter(name = PARAM_CLUSTERNAME)
	private String mClustername;

	/** The Constant PARAM_SEARCHURL. */
	public static final String PARAM_SEARCHURL = "mSearchUrl";

	/** The m search url. */
	@ConfigurationParameter(name = PARAM_SEARCHURL)
	private String mSearchUrl;

	/** The hoover search relative base path. */
	private String indexPath;

	/** The client. */
	private JestClient client;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.Resource_ImplBase#initialize(org.apache.uima.
	 * resource.ResourceSpecifier, java.util.Map)
	 */
	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();

		// Construct a new Jest client according to configuration via factory
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig.Builder(mHost + ":" + mPort).multiThreaded(false).build());
		client = factory.getObject();

		indexPath = mIndex + "/";

		return true;
	}

	/**
	 * Gets the elasticsearch client.
	 *
	 * @return the client
	 */
	public JestClient getClient() {
		return client;
	}

	/**
	 * Gets the elasticsearch index.
	 *
	 * @return the index
	 */
	public String getIndex() {
		return mIndex;
	}

	/**
	 * Sets the elasticsearch index.
	 *
	 * @param mIndex
	 *            the new index
	 */
	public void setIndex(String mIndex) {
		this.mIndex = mIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.resource.Resource_ImplBase#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
		try {
			client.close();
			logger.log(Level.INFO, "Hoover connection closed.");
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error closing Hoover connection.");
			e.printStackTrace();
		}
	}

	/**
	 * Gets the elasticsearch base URL (can be useful to link from newsleak to the
	 * original source document in Hoover).
	 *
	 * @return the client url
	 */
	public String getHooverBasePath() {
		return indexPath;
	}

	/**
	 * Extracts the document ids from a Jest request on Hoover's elasticsearch
	 * index.
	 *
	 * @param hits
	 *            the hits
	 * @return the ids
	 */
	public ArrayList<String> getIds(JsonArray hits) {
		ArrayList<String> idList = new ArrayList<String>();
		for (JsonElement hit : hits) {
			idList.add(hit.getAsJsonObject().get("_id").getAsString());
		}
		return idList;
	}
}
