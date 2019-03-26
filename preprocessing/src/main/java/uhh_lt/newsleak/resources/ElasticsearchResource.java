package uhh_lt.newsleak.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

import uhh_lt.newsleak.util.AtomicCounter;

public class ElasticsearchResource extends Resource_ImplBase {

	private Logger logger;
	private static final String DOCUMENT_TYPE = "document";

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
	public static final String PARAM_DOCUMENT_MAPPING_FILE = "documentMappingFile";
	@ConfigurationParameter(name = PARAM_DOCUMENT_MAPPING_FILE)
	private String documentMappingFile;
	public final static String PARAM_CREATE_INDEX = "createIndex";
	@ConfigurationParameter(
			name = PARAM_CREATE_INDEX, 
			mandatory = false,
			defaultValue = "false",
			description = "If true, an new index will be created (existing index will be removed).")
	private boolean createIndex;

	public static final String PARAM_METADATA_FILE = "mMetadata";
	@ConfigurationParameter(name = PARAM_METADATA_FILE)
	private String mMetadata;
	
	private File metadataFile;
	
	private TransportClient client;

	// generator for new document Ids
	private AtomicCounter autoincrementValue;
	// tmp_doc_id : new_doc_ids (necessary for paragraph splitting and correct metadata association)
	private HashMap<Integer, ArrayList<Integer>> documentIdMapping;
	private boolean documentIdMappingWritten = false; 

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();
		Settings settings = Settings.builder().put("cluster.name", mClustername).build();
		try {
			client = TransportClient.builder().settings(settings).build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(mHost), mPort));
			if (createIndex) {
				createIndex();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		autoincrementValue = new AtomicCounter();
		documentIdMapping = new HashMap<Integer, ArrayList<Integer>>();
		metadataFile = new File(mMetadata + ".id-map");
		return true;
	}


	private File File(String string) {
		// TODO Auto-generated method stub
		return null;
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

	/*
	 * Creates the index and adds a mapping for the document type
	 */
	private void createIndex() throws Exception {

		boolean exists = client.admin().indices().prepareExists(mIndex).execute().actionGet().isExists();

		if (exists) {
			logger.log(Level.INFO, "Preexisting index " + mIndex + " will be removed.");
			DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest(mIndex)).actionGet();
			if (deleteResponse.isAcknowledged()) {
				logger.log(Level.INFO, "Preexisting index " + mIndex + " successfully removed.");
				exists = false;
			}
		}

		logger.log(Level.INFO, "Index " + mIndex + " will be created.");
		String docMapping = new String(Files.readAllBytes(Paths.get(documentMappingFile)));

		XContentBuilder builder = XContentFactory.jsonBuilder();
		XContentParser parser = XContentFactory.xContent(XContentType.JSON)
				.createParser(docMapping.getBytes());
		parser.close();
		builder.copyCurrentStructure(parser);

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(mIndex);
		createIndexRequestBuilder.addMapping(DOCUMENT_TYPE, builder);
		createIndexRequestBuilder.execute().actionGet();

	}
	
	public synchronized int getNextDocumentId() {
		autoincrementValue.increment();
		return autoincrementValue.value();
	}
	
	public synchronized void addDocumentIdMapping(Integer tmpId, ArrayList<Integer> newIds) {
		documentIdMapping.put(tmpId, newIds);
	}

	public synchronized void writeDocumentIdMapping() throws IOException {
		logger.log(Level.INFO, "Writing document id mapping file " + metadataFile);
		if (!documentIdMappingWritten) {
			FileOutputStream fos = new FileOutputStream(metadataFile);
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(documentIdMapping);
	        oos.close();
			documentIdMappingWritten = true;
		}
	}
}
