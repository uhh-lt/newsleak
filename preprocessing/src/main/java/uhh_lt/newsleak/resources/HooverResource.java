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

public class HooverResource extends Resource_ImplBase {

	private Logger logger;

	public static final String HOOVER_DOCUMENT_TYPE = "doc";

	public static final String PARAM_HOST = "mHost";
	@ConfigurationParameter(name = PARAM_HOST)
	private String mHost;
	public static final String PARAM_PORT = "mPort";
	@ConfigurationParameter(name = PARAM_PORT)
	private Integer mPort;
	public static final String PARAM_INDEX = "mIndex";
	@ConfigurationParameter(name = PARAM_INDEX)
	private String mIndex;
	public static final String PARAM_CLUSTERNAME = "mClustername";
	@ConfigurationParameter(name = PARAM_CLUSTERNAME)
	private String mClustername;

	public static final String PARAM_SEARCHURL = "mSearchUrl";
	@ConfigurationParameter(name = PARAM_SEARCHURL)
	private String mSearchUrl;
	private String hooverSearchBaseUrl;

	private JestClient client;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();

		// Construct a new Jest client according to configuration via factory
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig
				.Builder(mHost + ":" + mPort)
				.multiThreaded(false)
				.build());
		client = factory.getObject();

		hooverSearchBaseUrl = PARAM_SEARCHURL + "/" + HOOVER_DOCUMENT_TYPE + "/" + mIndex + "/";

		return true;
	}


	public JestClient getClient() {
		return client;
	}


	public String getIndex() {
		return mIndex;
	}


	public void setIndex(String mIndex) {
		this.mIndex = mIndex;
	}


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


	public String getClientUrl() {
		return hooverSearchBaseUrl;
	}


	public ArrayList<String> getIds(JsonArray hits) {
		ArrayList<String> idList = new ArrayList<String>();
		for (JsonElement hit : hits) {
			idList.add(hit.getAsJsonObject().get("_id").getAsString());
		}
		return idList;
	}
}
