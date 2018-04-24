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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import uhh_lt.newsleak.util.ResultSetIterable;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.util.Level;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class Postgres2ElasticsearchIndexer extends NewsleakPreprocessor {

	private static final int BATCH_SIZE = 100;

	public static void main(String[] args) throws Exception {

		Postgres2ElasticsearchIndexer indexer = new Postgres2ElasticsearchIndexer();
		indexer.getConfiguration(args);

		indexer.initDb(indexer.dbName, indexer.dbUrl, indexer.dbUser, indexer.dbPass);

		TransportClient client;
		Settings settings = Settings.builder().put("cluster.name", indexer.esClustername).build();
		st.setFetchSize(BATCH_SIZE);
		try {
			client = TransportClient.builder().settings(settings).build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(indexer.esHost), Integer.parseInt(indexer.esPort)));
			// remove existing index
			client.admin().indices().delete(new DeleteIndexRequest(indexer.esIndex)).actionGet();
			// create index with all extracted data
			indexer.documenIndexer(client, indexer.esIndex, "document");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		conn.close();
	}


	private void documenIndexer(Client client, String indexName,
			String documentType) throws Exception {
		try {
			boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
			if (!exists) {
				System.out.println("Index " + indexName + " will be created.");

				createDynamicIndex(client, indexName, documentType);

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
		while(entTypes.next()){
			types.add(entTypes.getString("type").toLowerCase());
		}


		Function<ResultSet, String> indexDoc = new Function<ResultSet, String>() {

			@Override
			public String apply(ResultSet docSt) {
				List<NamedEntity> namedEntity = new ArrayList<>();
				String content;
				Integer docId = 0;
				try {
					content = docSt.getString("content");
					Date dbCreated = docSt.getDate("created");

					SimpleDateFormat simpleCreated = new SimpleDateFormat("yyyy-MM-dd");
					String created = simpleCreated.format(dbCreated);
					docId = docSt.getInt("id");

					ResultSet docEntSt = conn.createStatement()
							.executeQuery("select entid from entityoffset where  docid = " + docId + ";");
					Set<Long> ids = new HashSet<>();
					while (docEntSt.next()) {
						long entId = docEntSt.getLong("entid");
						if(ids.contains(entId)){
							continue;
						}
						ids.add(entId);
						ResultSet entSt = conn.createStatement()
								.executeQuery("select * from entity where  id = " + entId + ";");
						if (entSt.next()) {
							NamedEntity ne = new NamedEntity(entSt.getLong("id"), entSt.getString("name"),
									entSt.getString("type"), 1 /*docEntSt.getInt("frequency")*/);
							namedEntity.add(ne);
						}

					}

					///// Adding important terms to the index - ONLY top 10

					ResultSet docTermSt = conn.createStatement()
							.executeQuery("select * from terms where  docid = " + docId + " limit 10;");

					Map<String, Integer> termMap = new HashMap<>();
					while (docTermSt.next()) {
						String term = docTermSt.getString("term");
						int freq = docTermSt.getInt("frequency");
						termMap.put(term, freq);
					}

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

					ResultSet metadataSt = conn.createStatement()
							.executeQuery("select * from metadata where docid =" + docId + ";");

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



						for (String type:types){			
							xb.startArray("Entities"+type);
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
					
					synchronized (bulkRequestConcurrent) {
						bulkRequestConcurrent.add(client.prepareIndex(indexName, documentType, docId.toString()).setSource(xb));
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
		bulkRequestConcurrent.execute();

		docSt.close();

	}

	public static void createIndex(String indexName, Client client) throws Exception {
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		IndicesAdminClient iac = client.admin().indices();

		CreateIndexResponse response = iac.create(request).actionGet();
		if (!response.isAcknowledged()) {
			throw new Exception("Failed to delete index " + indexName);
		}
	}

	public static void createDynamicIndex(Client client, String indexName,
			String documentType/* , String mapping */) throws IOException, SQLException {

		IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
		if (res.isExists()) {
			DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
			delIdx.execute().actionGet();
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("properties");
		mappingBuilder.startObject("Content").field("type", "string").field("analyzer", "english").endObject();

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
			createEntitesPerTypeMappings(mappingBuilder, "Entities"+type);
		}
		System.out.println("creating nested entities mapping ... done");

		createKeywordsMappings(mappingBuilder);
		createEventTimeMappings(mappingBuilder);
		createSimpleTimexMappings(mappingBuilder);

		Map<String, String> metaFields = new HashMap<>();
		System.out.println("creating metadata mapping ...");

		ResultSet metadataSt = conn.createStatement().executeQuery(
				"select key, value, type from metadata  group by key, value, type;");
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
	// Based on this so:
	// http://stackoverflow.com/questions/22071198/adding-mapping-to-a-type-from-java-how-do-i-do-it

	private static void createEntitesPerTypeMappings(XContentBuilder mappingBuilder, String neType) throws IOException {

		//mappingBuilder.startObject(neType).field("type", "nested");
		mappingBuilder.startObject(neType);
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("EntId").field("type", "long").endObject();
		mappingBuilder.startObject("Entname").field("type", "string").field("analyzer", "english").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("EntType").field("type", "string").field("analyzer", "english").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("EntFrequency").field("type", "long").endObject().endObject().endObject();
	}

	private static void createEventTimeMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("EventTimes");	
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Beginoffset").field("type", "long").endObject()
		.startObject("Endoffset").field("type", "long").endObject();
		mappingBuilder.startObject("TimeXType").field("type", "string").field("analyzer", "english")
		.startObject("fields").startObject("raw").field("type", "string").field("index", "not_analyzed")
		.endObject().endObject().endObject();
		mappingBuilder.startObject("Timex").field("type", "string").field("analyzer", "english").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("Timexvalue").field("type", "string").field("analyzer", "english")
		.startObject("fields").startObject("raw").field("type", "string").field("index", "not_analyzed")
		.endObject().endObject().endObject().endObject().endObject();
	}



	private static void createKeywordsMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("Keywords");	
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Keyword").field("type", "String").field("analyzer", "english")
		.startObject("fields").startObject("raw").field("type", "string").field("index", "not_analyzed")
		.endObject().endObject().endObject();
		mappingBuilder.startObject("TermFrequency").field("type", "long").endObject().endObject().endObject();
	}

	
	private static void createSimpleTimexMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("SimpleTimeExpresion").field("type", "date")
		.field("format", "yyyy-MM-dd || yyyy || yyyy-MM").startObject("fields").startObject("raw")
		.field("type", "date").field("format", "yyyy-MM-dd || yyyy || yyyy-MM").endObject().endObject()
		.endObject();

	}
	

	private static void createMetadataMappings(XContentBuilder mappingBuilder, String meta, String type)
			throws IOException {
		mappingBuilder.startObject(meta).field("type", type).startObject("fields").startObject("raw")
		.field("type", type).field("index", "not_analyzed").endObject().endObject().endObject();

	}


	static class NamedEntity {
		long id;
		String name;
		String type;
		int frequency;

		public NamedEntity(long aId, String aName, String aType, int aFreq) {
			this.id = aId;
			this.name = aName;
			this.type = aType;
			this.frequency = aFreq;

		}
	}

	static class TimeX {
		int beginOffset;
		int endOffset;
		String timeX;
		String timeXType;
		String timexValue;

		public TimeX(int aBeginOffset, int aEndOffset, String aTimeX, String aTimexType, String aTimexValue) {
			this.beginOffset = aBeginOffset;
			this.endOffset = aEndOffset;
			this.timeX = aTimeX;
			this.timeXType = aTimexType;
			this.timexValue = aTimexValue;

		}
	}
	
	
	class AtomicCounter {
	    private AtomicInteger c = new AtomicInteger(0);

	    public void increment() {
	        c.incrementAndGet();
	    }

	    public void decrement() {
	        c.decrementAndGet();
	    }

	    public int value() {
	        return c.get();
	    }
	}
	
	class BulkRequestConcurrent {
		private BulkRequestBuilder bulkRequest;
		private Client client;
		public BulkRequestConcurrent(Client client) {
			super();
			this.client = client;
			this.bulkRequest = this.client.prepareBulk();
		}
		public synchronized void add(IndexRequestBuilder request) {
			this.bulkRequest.add(request);
		}
		public synchronized void execute() {
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				logger.log(Level.SEVERE, "##### Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
			}
			this.bulkRequest = client.prepareBulk();
		}
	}

}