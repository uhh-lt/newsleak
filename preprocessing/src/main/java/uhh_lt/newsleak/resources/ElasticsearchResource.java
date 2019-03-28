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

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.writer.ElasticsearchDocumentWriter which writes fulltexts
 * extracted by a reader process temporarily into the newsleak elasticsearch
 * index.
 * 
 * The class takes care for assigning unique document id (as auto-increment
 * integers) to documents indexed by newsleak. In case of splitting of long
 * documents (if the parameter paragraphsasdocuments = true in the
 * pre-processing configuration) a new document id needs to be created for each
 * document split. To assign metadata assigned to the original document
 * correctly to the document splits, the class keeps a record of the mapping of
 * original document ids to document split ids. @See
 * uhh_lt.newsleak.preprocessing.InformationExtraction2Postgres duplicates the
 * metadata according to this record later on.
 */
public class ElasticsearchResource extends Resource_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant DOCUMENT_TYPE. */
	private static final String DOCUMENT_TYPE = "document";

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

	/** The Constant PARAM_DOCUMENT_MAPPING_FILE. */
	public static final String PARAM_DOCUMENT_MAPPING_FILE = "documentMappingFile";

	/** The document mapping file. */
	@ConfigurationParameter(name = PARAM_DOCUMENT_MAPPING_FILE)
	private String documentMappingFile;

	/** The Constant PARAM_CREATE_INDEX. */
	public final static String PARAM_CREATE_INDEX = "createIndex";

	/** The create index. */
	@ConfigurationParameter(name = PARAM_CREATE_INDEX, mandatory = false, defaultValue = "false", description = "If true, an new index will be created (existing index will be removed).")
	private boolean createIndex;

	/** The Constant PARAM_METADATA_FILE. */
	public static final String PARAM_METADATA_FILE = "mMetadata";

	/** The m metadata. */
	@ConfigurationParameter(name = PARAM_METADATA_FILE)
	private String mMetadata;

	/** The metadata file. */
	private File metadataFile;

	/** The elasticsearch client. */
	private TransportClient client;

	/** The autoincrement value for generating unique document ids. */
	private AtomicCounter autoincrementValue;

	/**
	 * Mapping of temporary document ids to new document ids (necessary for
	 * paragraph splitting and correct metadata association)
	 */
	private HashMap<Integer, ArrayList<Integer>> documentIdMapping;

	/**
	 * Memorize, if document id mapping has been written already (for parallel
	 * execution of CPEs).
	 */
	private boolean documentIdMappingWritten = false;

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
		// setup elasticsearch connection
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
		// initialize fields
		autoincrementValue = new AtomicCounter();
		documentIdMapping = new HashMap<Integer, ArrayList<Integer>>();
		metadataFile = new File(mMetadata + ".id-map");
		return true;
	}

	/**
	 * Gets the elasticsearch client.
	 *
	 * @return the client
	 */
	public TransportClient getClient() {
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
		client.close();
	}

	/**
	 * Creates a new elasticsearch index and adds a mapping for the document type.
	 * Previously existing indexes will be removed.
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void createIndex() throws Exception {

		boolean exists = client.admin().indices().prepareExists(mIndex).execute().actionGet().isExists();

		// remove preexisting index
		if (exists) {
			logger.log(Level.INFO, "Preexisting index " + mIndex + " will be removed.");
			DeleteIndexResponse deleteResponse = client.admin().indices().delete(new DeleteIndexRequest(mIndex))
					.actionGet();
			if (deleteResponse.isAcknowledged()) {
				logger.log(Level.INFO, "Preexisting index " + mIndex + " successfully removed.");
				exists = false;
			}
		}

		// create schema mapping from file
		logger.log(Level.INFO, "Index " + mIndex + " will be created.");
		String docMapping = new String(Files.readAllBytes(Paths.get(documentMappingFile)));

		XContentBuilder builder = XContentFactory.jsonBuilder();
		XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(docMapping.getBytes());
		parser.close();
		builder.copyCurrentStructure(parser);

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(mIndex);
		createIndexRequestBuilder.addMapping(DOCUMENT_TYPE, builder);
		createIndexRequestBuilder.execute().actionGet();

	}

	/**
	 * Generates a new document id.
	 *
	 * @return the next document id
	 */
	public synchronized int getNextDocumentId() {
		autoincrementValue.increment();
		return autoincrementValue.value();
	}

	/**
	 * Adds a new mapping between temporary and final document ids
	 *
	 * @param tmpId
	 *            the tmp id
	 * @param newIds
	 *            the new ids (can be more than one due to splitting of long
	 *            documents)
	 */
	public synchronized void addDocumentIdMapping(Integer tmpId, ArrayList<Integer> newIds) {
		documentIdMapping.put(tmpId, newIds);
	}

	/**
	 * Write document id mapping to disk.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public synchronized void writeDocumentIdMapping() throws IOException {
		logger.log(Level.INFO, "Writing document id mapping file " + metadataFile);
		if (!documentIdMappingWritten) {
			FileOutputStream fos = new FileOutputStream(metadataFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(documentIdMapping);
			oos.close();
			/* run only once even in parallel execution of a CPE */
			documentIdMappingWritten = true;
		}
	}
}
