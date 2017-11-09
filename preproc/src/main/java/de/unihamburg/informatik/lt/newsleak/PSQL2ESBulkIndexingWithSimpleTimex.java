package de.unihamburg.informatik.lt.newsleak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;

import static org.elasticsearch.node.NodeBuilder.*;

import com.google.gson.stream.JsonWriter;

public class PSQL2ESBulkIndexingWithSimpleTimex {
	private static Connection conn;
	private static Statement st;
	static Logger logger = Logger.getLogger(PSQL2ESBulkIndexingWithSimpleTimex.class.getName());

	static String dbName;
	static String dbUser;
	static String dbPass;
	static String dbUrl;
	static String indexName;

	public static void main(String[] args) throws Exception {
		getConfigs("newsleak.properties");
		initDB(dbName, dbUrl, dbUser, dbPass);
		st.setFetchSize(50);
		Path path = new File("elasticsearch.yml").toPath();
		Settings settings = settingsBuilder().loadFromPath(path).build();

		Node node = nodeBuilder().settings(settings).node();
		Client client = node.client();
		// document2Json();
		documenIndexer(client, indexName, "document"/* , args[3] */);
	}

	private static void document2Json() {
		JsonWriter writer;
		try {
			ResultSet docSt = st.executeQuery("select * from document;");
			writer = new JsonWriter(new FileWriter("document.json"));
			writer.beginArray();
			while (docSt.next()) {
				String content = docSt.getString("content");
				Date created = docSt.getDate("created");
				Integer id = docSt.getInt("id");

				writer.beginObject();
				writer.name("id").value(id);
				writer.name("content").value(content);
				writer.name("created").value(created.toString());
				writer.endObject();
			}
			writer.endArray();
			writer.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void documenIndexer(Client client, String indexName,
			String documentType/* , String mappingFile */) throws Exception {
		try {
			boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
			if (!exists) {
				System.out.println("Index " + indexName + " will be created.");

				createDynamicIndex(client, indexName, documentType);
				// createIndex(indexName, client);
				/*
				 * createcableIndex(client, indexName, documentType ,
				 * mappingFile );
				 */

				System.out.println("Index " + indexName + " is created.");
			}
		} catch (Exception e) {
			// starnange error
			System.out.println(e);
			logger.error(e.getMessage());
		}

		System.out.println("Start indexing");
		ResultSet docSt = st.executeQuery("select * from document;");
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		int bblen = 0;

		ResultSet entTypes = conn.createStatement().executeQuery("select distinct type from entity;");
		Set<String> types = new HashSet<>();
		while(entTypes.next()){
			types.add(entTypes.getString("type").toLowerCase());
		}
		while (docSt.next()) {
			List<NamedEntity> namedEntity = new ArrayList<>();
			String content = docSt.getString("content").replace("\r", "");
			// content = content.substring(0,content.length()/10);
			Date dbCreated = docSt.getDate("created");

			SimpleDateFormat simpleCreated = new SimpleDateFormat("yyyy-MM-dd");
			String created = simpleCreated.format(dbCreated);
			Integer docId = docSt.getInt("id");

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
			//// Adding Timex to ES index


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
				// Object type = metadataSt.getObject("type");
				// xb.field(key, value)/* .field("value",
				// value).field("type",type) */;
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
			///// Adding entities
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

			//// Adding terms
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

			//// Adding TimeX
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
			/*
			 * if (rels.size() > 0) { xb.startArray("relations"); for (Long
			 * relId : rels.keySet()) { xb.startObject(); xb.field("id", relId);
			 * xb.field("relation", rels.get(relId)); xb.field("frequency",
			 * relIds.get(relId)); xb.endObject(); } xb.endArray(); }
			 */
			xb.endObject();
			metadataSt.close();
			bulkRequest.add(client.prepareIndex(indexName, documentType, docId.toString()).setSource(xb));
			bblen++;
			
			if (bblen % 50 == 0) {
				logger.info("##### " + bblen + " documents are indexed.");
				BulkResponse bulkResponse = bulkRequest.execute().actionGet();
				if (bulkResponse.hasFailures()) {
					logger.error("##### Bulk Request failure with error: " + bulkResponse.buildFailureMessage());
				}
				bulkRequest = client.prepareBulk();
			}
		}
		docSt.close();
		if (bulkRequest.numberOfActions() > 0) {
			logger.info("##### " + bblen + " data indexed.");
			BulkResponse bulkRes = bulkRequest.execute().actionGet();
			if (bulkRes.hasFailures()) {
				logger.error("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
			}
		}

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
		/*createEntitesPerTypeMappings(mappingBuilder, "Entitiesloc");

		createEntitesPerTypeMappings(mappingBuilder, "Entitiesmisc");

		createEntitesPerTypeMappings(mappingBuilder, "Entitiesorg");

		createEntitesPerTypeMappings(mappingBuilder, "Entitiesper");*/
		
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

	public static void createcableIndex(Client client, String indexName,
			String documentType/* , String mapping */) throws IOException {

		IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
		if (res.isExists()) {
			DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
			delIdx.execute().actionGet();
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("properties").startObject("Content").field("type", "string").field("analyzer", "english")
				.endObject().startObject("Subject").field("type", "string").field("analyzer", "english").endObject()
				.startObject("Header").field("type", "string").field("analyzer", "english").endObject()
				.startObject("Origin").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("Classification").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("ReferenceId").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("References").field("type", "string").field("store", "yes").field("index", "not_analyzed")
				.endObject().startObject("SignedBy").field("type", "string").field("store", "yes")
				.field("index", "not_analyzed").endObject().startObject("Tags").field("type", "string")
				.field("store", "yes").field("index", "not_analyzed").endObject()

				.startArray("Entities").startObject("EntId").field("type", "long").endObject().startObject("Entname")
				.field("type", "string").field("index", "not_analyzed").endObject().startObject("EntType")
				.field("type", "string").field("index", "not_analyzed").endObject().startObject("EntFrequency")
				.field("type", "long").endObject().endArray()

				.endObject().endObject();

		/*
		 * String mappingFile = new
		 * String(Files.readAllBytes(Paths.get(mapping)));
		 * 
		 * createIndexRequestBuilder.addMapping(documentType, mappingFile);
		 */
		createIndexRequestBuilder.addMapping(documentType, mappingBuilder);

		createIndexRequestBuilder.execute().actionGet();
	}

	public static void createEnronIndex(Client client, String indexName, String documentType) throws Exception {

		IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
		if (res.isExists()) {
			DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
			delIdx.execute().actionGet();
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("properties").startObject("Content").field("type", "string").field("analyzer", "english")
				.endObject().startObject("Subject").field("type", "string").field("analyzer", "english").endObject()

				.startObject("Timezone").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("Recipients_name").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("Recipients_email").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("Recipients_order").field("type", "short").field("index", "not_analyzed").endObject()
				.startObject("Recipients_type").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("Recipients_id").field("type", "long").field("index", "not_analyzed").endObject()

				.startObject("sender_id").field("type", "long").field("index", "not_analyzed").endObject()
				.startObject("sender_email").field("type", "string").field("index", "not_analyzed").endObject()
				.startObject("sender_name").field("type", "string").field("index", "not_analyzed")

				.startArray("Entities").startObject("EntId").field("type", "long").endObject().startObject("Entname")
				.field("type", "string").field("index", "not_analyzed").endObject().startObject("EntType")
				.field("type", "string").field("index", "not_analyzed").endObject().startObject("EntFrequency")
				.field("type", "long").endObject().endArray()

				/*
				 * .startArray("relations").startObject("id") .field("type",
				 * "integer").field("index", "not_analyzed").endObject()
				 * .startObject("relation").field("type",
				 * "string").field("index", "not_analyzed")
				 * .endObject().startObject("frequency").field("type",
				 * "integer") .field("index",
				 * "not_analyzed").endObject().endArray()
				 */.endObject().endObject();
		createIndexRequestBuilder.addMapping(documentType, mappingBuilder);

		try {
			CreateIndexResponse response = createIndexRequestBuilder.execute().actionGet();
			if (!response.isAcknowledged()) {
				throw new Exception("Failed to delete index " + indexName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void initDB(String dbName, String ip, String user, String pswd)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {

		String url = "jdbc:postgresql://" + ip + "/";
		String driver = "org.postgresql.Driver";
		String userName = user;
		String password = pswd;
		Class.forName(driver).newInstance();
		conn = DriverManager.getConnection(url + dbName, userName, password);
		conn.setAutoCommit(false);
		st = conn.createStatement();
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

	static void getConfigs(String config) throws IOException {
		Properties prop = new Properties();
		InputStream input = null;

		input = new FileInputStream(config);

		prop.load(input);

		dbName = prop.getProperty("dbname");
		dbUser = prop.getProperty("dbuser");
		dbUrl = prop.getProperty("dbaddress");
		dbPass = prop.getProperty("dbpass");
		indexName = prop.getProperty("indexname");

		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}