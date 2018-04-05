package uhh_lt.newsleak.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

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

	private TransportClient client;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();
		Settings settings = Settings.builder().put("cluster.name", mClustername).build();
		try {
			//			client = new PreBuiltTransportClient(settings)
			//			        .addTransportAddress(new TransportAddress(InetAddress.getLocalHost(), mPort));
			client = TransportClient.builder().settings(settings).build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(mHost), mPort));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}


	public TransportClient getClient() {
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
		client.close();
	}
	
	
	public String getClientUrl() {
		String url = client.transportAddresses().iterator().next().getAddress();
		return url + "/" + HOOVER_DOCUMENT_TYPE + "/" + mIndex + "/";
	}


}
