package uhh_lt.newsleak.preprocessing;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import uhh_lt.newsleak.util.AtomicCounter;
import uhh_lt.newsleak.util.ResultSetIterable;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.util.Level;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * The Class Postgres2ElasticsearchIndexer reads fulltext and extracted
 * information from the newsleak postgres database and feeds it to the newsleak
 * elasticsearch index. For this, several mappings of elasticsearch data objects
 * are created. Indexing itself is carried out in parallel bulk requests.
 * 
 * As analyzer for fulltext search, one elasticsearch language analyzer is
 * used. The analyzer used is to be configured as defaultlanguage configuration
 * variable in the preprocessing configuration (ISO 639-3 code). If in
 * elasticsearch no language analyzer for a given language code is available,
 * the the English analyzer is used.
 */
public class Postgres2ElasticsearchIndexer extends NewsleakPreprocessor {

	/** The Constant BATCH_SIZE. */
	private static final int BATCH_SIZE = 100;

	private String elasticsearchDefaultAnalyzer;

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 * @throws Exception
	 *             the exception
	 */
	public static void main(String[] args) throws Exception {

		Postgres2ElasticsearchIndexer indexer = new Postgres2ElasticsearchIndexer();
		indexer.getConfiguration(args);

		indexer.initDb(indexer.dbName, indexer.dbUrl, indexer.dbUser, indexer.dbPass);

		indexer.setElasticsearchDefaultAnalyzer(indexer.defaultLanguage);

		TransportClient client;
		Settings settings = Settings.builder().put("cluster.name", indexer.esClustername).build();
		st.setFetchSize(BATCH_SIZE);
		try {
			client = TransportClient.builder().settings(settings).build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(indexer.esHost),
							Integer.parseInt(indexer.esPort)));
			// remove existing index
			client.admin().indices().delete(new DeleteIndexRequest(indexer.esIndex)).actionGet();
			// create index with all extracted data
			indexer.documentIndexer(client, indexer.esIndex, "document");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		conn.close();
	}

	public String getElasticsearchDefaultAnalyzer() {
		return elasticsearchDefaultAnalyzer;
	}

	public void setElasticsearchDefaultAnalyzer(String isoCode) {

		// language analyzers supported by elasticsearch 2.4
		HashMap<String, String> analyzers = new HashMap<String, String>();
		analyzers.put("ara", "arabic");
		analyzers.put("bul", "bulgarian");
		analyzers.put("cat", "catalan");
		analyzers.put("ces", "czech");
		analyzers.put("dan", "danish");
		analyzers.put("eng", "english");
		analyzers.put("nld", "dutch");
		analyzers.put("fin", "finnish");
		analyzers.put("fra", "french");
		analyzers.put("deu", "german");
		analyzers.put("ell", "greek");
		analyzers.put("hin", "hindi");
		analyzers.put("hun", "hungarian");
		analyzers.put("ind", "indonesian");
		analyzers.put("ita", "italian");
		analyzers.put("lav", "latvian");
		analyzers.put("lit", "lithuanian");
		analyzers.put("nno", "norwegian");
		analyzers.put("fas", "persian");
		analyzers.put("por", "portuguese");
		analyzers.put("ron", "romanian");
		analyzers.put("rus", "russian");
		analyzers.put("spa", "spanish");
		analyzers.put("swe", "swedish");
		analyzers.put("tur", "turkish");
		analyzers.put("tha", "thai");

		// set elasticsearch analyse (english as default)
		this.elasticsearchDefaultAnalyzer = analyzers.containsKey(isoCode) ? analyzers.get(isoCode) : "english";

		if (!analyzers.containsKey(isoCode)) {
			this.logger.log(Level.WARNING, "Configuration parameter defaultlanguage=" + isoCode
					+ " is not supported by elasticsearch language analyzers. Switching to 'english' as default elasticsearch analyzer.");
		}

	}

	/**
	 * Document indexer.
	 *
	 * @param client
	 *            the client
	 * @param indexName
	 *            the index name
	 * @param documentType
	 *            the document type
	 * @throws Exception
	 *             the exception
	 */
	private void documentIndexer(Client client, String indexName, String documentType) throws Exception {
		try {
			boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
			if (!exists) {
				System.out.println("Index " + indexName + " will be created.");

				createElasticsearchIndex(client, indexName, documentType);

				System.out.println("Index " + indexName + " is created.");
			}
		} catch (Exception e) {
			System.out.println(e);
			logger.log(Level.SEVERE, e.getMessage());
		}

		System.out.println("Start indexing");
		ResultSet docSt = st.executeQuery("select * from document;");

		BulkRequestConcurrent bulkRequestConcurrent = new BulkRequestConcurrent(client);
		AtomicCounter bblen = new AtomicCounter();

		ResultSet entTypes = conn.createStatement().executeQuery("select distinct type from entity;");
		Set<String> types = new HashSet<>();
		while (entTypes.next()) {
			types.add(entTypes.getString("type").toLowerCase());
		}

		Function<ResultSet, String> indexDoc = new Function<ResultSet, String>() {

			@Override
			public String apply(ResultSet docSt) {
				List<NamedEntity> namedEntity = new ArrayList<>();
				String content;
				Integer docId = 0;
				try {
					// fulltext
					content = docSt.getString("content");
					// creation date
					Date dbCreated = docSt.getDate("created");
					SimpleDateFormat simpleCreated = new SimpleDateFormat("yyyy-MM-dd");
					String created = simpleCreated.format(dbCreated);
					// document id
					docId = docSt.getInt("id");
					// entities
					ResultSet docEntSt = conn.createStatement()
							.executeQuery("select entid from entityoffset where  docid = " + docId + ";");
					Set<Long> ids = new HashSet<>();
					while (docEntSt.next()) {
						long entId = docEntSt.getLong("entid");
						if (ids.contains(entId)) {
							continue;
						}
						ids.add(entId);
						ResultSet entSt = conn.createStatement()
								.executeQuery("select * from entity where  id = " + entId + ";");
						if (entSt.next()) {
							NamedEntity ne = new NamedEntity(entSt.getLong("id"), entSt.getString("name"),
									entSt.getString("type"), 1 /* docEntSt.getInt("frequency") */);
							namedEntity.add(ne);
						}

					}
					// key terms (top 10 only)
					ResultSet docTermSt = conn.createStatement()
							.executeQuery("select * from terms where  docid = " + docId + " limit 10;");
					Map<String, Integer> termMap = new HashMap<>();
					while (docTermSt.next()) {
						String term = docTermSt.getString("term");
						int freq = docTermSt.getInt("frequency");
						termMap.put(term, freq);
					}
					// temporal expressions
					ResultSet docTimexSt = conn.createStatement()
							.executeQuery("select * from eventtime where  docid = " + docId + ";");
					List<TimeX> timexs = new ArrayList<>();
					Set<String> simpeTimex = new HashSet<>();
					while (docTimexSt.next()) {
						String timeXValue = docTimexSt.getString("timexvalue");
						TimeX t = new TimeX(docTimexSt.getInt("beginoffset"), docTimexSt.getInt("endoffset"),
								docTimexSt.getString("timex"), docTimexSt.getString("type"), timeXValue);
						timexs.add(t);
						simpeTimex.add(timeXValue);
					}
					// metadata
					ResultSet metadataSt = conn.createStatement()
							.executeQuery("select * from metadata where docid =" + docId + ";");

					// Create a JSON request object for adding the data to the index
					// -------------------------------------------------------------
					XContentBuilder xb = XContentFactory.jsonBuilder().startObject();
					xb.field("Content", content).field("Created", created);
					Map<String, List<String>> metas = new HashMap<>();
					while (metadataSt.next()) {
						// we capitalize the first character on purpose
						String key = StringUtils.capitalize(metadataSt.getString("key").replace(".", "_"));
						String value = metadataSt.getString("value");
						metas.putIfAbsent(key, new ArrayList<>());
						metas.get(key).add(value);
					}
					for (String key : metas.keySet()) {
						if (metas.get(key).size() > 1) { // array field
							xb.field(key, metas.get(key));
						} else {
							xb.field(key, metas.get(key).get(0));
						}
					}
					// Adding entities
					if (namedEntity.size() > 0) {
						xb.startArray("Entities");
						for (NamedEntity ne : namedEntity) {
							xb.startObject();
							xb.field("EntId", ne.id);
							xb.field("Entname", ne.name);
							xb.field("EntType", ne.type);
							xb.field("EntFrequency", ne.frequency);
							xb.endObject();
						}
						xb.endArray();

						for (String type : types) {
							xb.startArray("Entities" + type);
							for (NamedEntity ne : namedEntity) {
								if (ne.type.toLowerCase().equals(type)) {
									xb.startObject();
									xb.field("EntId", ne.id);
									xb.field("Entname", ne.name);
									xb.field("EntFrequency", ne.frequency);
									xb.endObject();
								}
							}
							xb.endArray();
						}

					}

					// Adding terms
					if (termMap.size() > 0) {
						xb.startArray("Keywords");
						for (String term : termMap.keySet()) {
							xb.startObject();
							xb.field("Keyword", term);
							xb.field("TermFrequency", termMap.get(term));
							xb.endObject();
						}
						xb.endArray();
					}

					// Adding TimeX
					if (timexs.size() > 0) {
						xb.startArray("EventTimes");
						for (TimeX t : timexs) {
							xb.startObject();
							xb.field("Beginoffset", t.beginOffset);
							xb.field("Endoffset", t.endOffset);
							xb.field("Timex", t.timeX);
							xb.field("TimeXType", t.timeXType);
							xb.field("Timexvalue", t.timexValue);
							xb.endObject();
						}
						xb.endArray();
						xb.field("SimpleTimeExpresion", new ArrayList<>(simpeTimex));
					}

					xb.endObject();
					metadataSt.close();

					// perform concurrent bulk requests
					synchronized (bulkRequestConcurrent) {
						bulkRequestConcurrent
								.add(client.prepareIndex(indexName, documentType, docId.toString()).setSource(xb));
						bblen.increment();

						if (bblen.value() % BATCH_SIZE == 0) {
							logger.log(Level.INFO, "##### " + bblen.value() + " documents are indexed.");
							bulkRequestConcurrent.execute();
						}
					}

				} catch (SQLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return docId.toString();
			}

		};

		// parallel execution
		List<String> userIdList = new ResultSetIterable<String>(docSt, indexDoc).stream().collect(Collectors.toList());
		// index last requests
		try {
			bulkRequestConcurrent.execute();
		} catch (ActionRequestValidationException e) {
			logger.log(Level.INFO, "All data has been indexed.");
		}

		docSt.close();

	}

	/**
	 * Creates the elasticsearch index mappings.
	 *
	 * @param client
	 *            the client
	 * @param indexName
	 *            the index name
	 * @param documentType
	 *            the document type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void createElasticsearchIndex(Client client, String indexName, String documentType/* , String mapping */)
			throws IOException, SQLException {

		IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
		if (res.isExists()) {
			DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
			delIdx.execute().actionGet();
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("properties");
		mappingBuilder.startObject("Content").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).endObject();

		mappingBuilder.startObject("Created").field("type", "date").field("format", "yyyy-MM-dd").

				startObject("fields").startObject("raw").field("type", "date").field("format", "yyyy-MM-dd").endObject()
				.endObject().endObject();

		System.out.println("creating entities mapping ...");
		createEntitesPerTypeMappings(mappingBuilder, "Entities");
		System.out.println("creating entities mapping ... done");

		ResultSet entTypes = conn.createStatement().executeQuery("select distinct type from entity;");
		System.out.println("creating nested entities mapping ...");
		while (entTypes.next()) {
			String type = entTypes.getString("type").toLowerCase();
			createEntitesPerTypeMappings(mappingBuilder, "Entities" + type);
		}
		System.out.println("creating nested entities mapping ... done");

		createKeywordsMappings(mappingBuilder);
		createEventTimeMappings(mappingBuilder);
		createSimpleTimexMappings(mappingBuilder);

		Map<String, String> metaFields = new HashMap<>();
		System.out.println("creating metadata mapping ...");

		ResultSet metadataSt = conn.createStatement()
				.executeQuery("select key, value, type from metadata  group by key, value, type;");
		while (metadataSt.next()) {
			String key = StringUtils.capitalize(metadataSt.getString("key").replace(".", "_"));
			String type = metadataSt.getString("type");
			if (type.toLowerCase().equals("date")) {
				type = "date";
			} else if (type.toLowerCase().equals("number") || type.toLowerCase().startsWith("int")) {
				type = "long";
			} else {
				type = "string";
			}
			metaFields.put(key, type);
		}

		System.out.println("creating metadata mapping ... done");

		for (String meta : metaFields.keySet()) {
			createMetadataMappings(mappingBuilder, meta, metaFields.get(meta));
		}
		mappingBuilder.endObject().endObject().endObject();

		System.out.println(mappingBuilder.string());
		createIndexRequestBuilder.addMapping(documentType, mappingBuilder);

		createIndexRequestBuilder.execute().actionGet();
	}

	/**
	 * Creates the entites per type mappings.
	 *
	 * @param mappingBuilder
	 *            the mapping builder
	 * @param neType
	 *            the ne type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void createEntitesPerTypeMappings(XContentBuilder mappingBuilder, String neType) throws IOException {

		mappingBuilder.startObject(neType);
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("EntId").field("type", "long").endObject();
		mappingBuilder.startObject("Entname").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
		mappingBuilder.startObject("EntType").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
		mappingBuilder.startObject("EntFrequency").field("type", "long").endObject().endObject().endObject();
	}

	/**
	 * Creates the event time mappings.
	 *
	 * @param mappingBuilder
	 *            the mapping builder
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void createEventTimeMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("EventTimes");
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Beginoffset").field("type", "long").endObject().startObject("Endoffset")
				.field("type", "long").endObject();
		mappingBuilder.startObject("TimeXType").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
		mappingBuilder.startObject("Timex").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
		mappingBuilder.startObject("Timexvalue").field("type", "string")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject().endObject()
				.endObject();
	}

	/**
	 * Creates the keywords mappings.
	 *
	 * @param mappingBuilder
	 *            the mapping builder
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void createKeywordsMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("Keywords");
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Keyword").field("type", "String")
				.field("analyzer", this.getElasticsearchDefaultAnalyzer()).startObject("fields").startObject("raw")
				.field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
		mappingBuilder.startObject("TermFrequency").field("type", "long").endObject().endObject().endObject();
	}

	/**
	 * Creates the simple timex mappings.
	 *
	 * @param mappingBuilder
	 *            the mapping builder
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static void createSimpleTimexMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("SimpleTimeExpresion").field("type", "date")
				.field("format", "yyyy-MM-dd || yyyy || yyyy-MM").startObject("fields").startObject("raw")
				.field("type", "date").field("format", "yyyy-MM-dd || yyyy || yyyy-MM").endObject().endObject()
				.endObject();

	}

	/**
	 * Creates the metadata mappings for the elasticsearch index.
	 *
	 * @param mappingBuilder
	 *            the mapping builder
	 * @param meta
	 *            the meta
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static void createMetadataMappings(XContentBuilder mappingBuilder, String meta, String type)
			throws IOException {
		mappingBuilder.startObject(meta).field("type", type).startObject("fields").startObject("raw")
				.field("type", type).field("index", "not_analyzed").endObject().endObject().endObject();

	}

	/**
	 * The Class NamedEntity.
	 */
	static class NamedEntity {

		/** The id. */
		long id;

		/** The name. */
		String name;

		/** The type. */
		String type;

		/** The frequency. */
		int frequency;

		/**
		 * Instantiates a new named entity.
		 *
		 * @param aId
		 *            the a id
		 * @param aName
		 *            the a name
		 * @param aType
		 *            the a type
		 * @param aFreq
		 *            the a freq
		 */
		public NamedEntity(long aId, String aName, String aType, int aFreq) {
			this.id = aId;
			this.name = aName;
			this.type = aType;
			this.frequency = aFreq;

		}
	}

	/**
	 * The Class TimeX.
	 */
	static class TimeX {

		/** The begin offset. */
		int beginOffset;

		/** The end offset. */
		int endOffset;

		/** The time X. */
		String timeX;

		/** The time X type. */
		String timeXType;

		/** The timex value. */
		String timexValue;

		/**
		 * Instantiates a new time X.
		 *
		 * @param aBeginOffset
		 *            the a begin offset
		 * @param aEndOffset
		 *            the a end offset
		 * @param aTimeX
		 *            the a time X
		 * @param aTimexType
		 *            the a timex type
		 * @param aTimexValue
		 *            the a timex value
		 */
		public TimeX(int aBeginOffset, int aEndOffset, String aTimeX, String aTimexType, String aTimexValue) {
			this.beginOffset = aBeginOffset;
			this.endOffset = aEndOffset;
			this.timeX = aTimeX;
			this.timeXType = aTimexType;
			this.timexValue = aTimexValue;

		}
	}

	/**
	 * The Class BulkRequestConcurrent.
	 */
	class BulkRequestConcurrent {

		/** The bulk request. */
		private BulkRequestBuilder bulkRequest;

		/** The elasticsearch client. */
		private Client client;

		/**
		 * Instantiates a new concurrent bulk request.
		 *
		 * @param client
		 *            the client
		 */
		public BulkRequestConcurrent(Client client) {
			super();
			this.client = client;
			this.bulkRequest = this.client.prepareBulk();
		}

		/**
		 * Adds a bulk of data to the index.
		 *
		 * @param request
		 *            the request
		 */
		public synchronized void add(IndexRequestBuilder request) {
			this.bulkRequest.add(request);
		}

		/**
		 * Executes a bulk request.
		 */
		public synchronized void execute() {
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				logger.log(Level.SEVERE,
						"##### Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
			}
			this.bulkRequest = client.prepareBulk();
		}
	}

}