package uhh_lt.newsleak.writer;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.Node;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.types.TimeX;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=false)
public class ElasticsearchAnnotationWriter extends JCasAnnotator_ImplBase {

	private Logger logger;

	public static final String RESOURCE_ESCLIENT = "esResource";
	@ExternalResource(key = RESOURCE_ESCLIENT)
	private ElasticsearchResource esResource; 

	private TransportClient client;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		client = esResource.getClient();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();

		XContentBuilder builder;
		try {
			builder = XContentFactory.jsonBuilder()
					.startObject()
					.field("text", docText)
					.field("date", new Date())
					.field("id", metadata.getDocId())
					.endObject();
			IndexResponse response = client.prepareIndex(esResource.getIndex(), "document", metadata.getDocId())
					.setSource(builder).get();
			// client.listedNodes().get(0).
			logger.log(Level.INFO, response.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void documenIndexer(Client client, String indexName,
			String documentType/* , String mappingFile */) throws Exception {
		try {
			boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
			if (!exists) {
				System.out.println("Index " + indexName + " will be created.");

				createDynamicIndex(client, indexName, documentType);

				System.out.println("Index " + indexName + " is created.");
			}
		} catch (Exception e) {
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


	private void createDynamicIndex(Client client, String indexName,
			String documentType) throws IOException, SQLException {

		// delete index, if it already exists
		IndicesExistsResponse res = client.admin().indices().prepareExists(indexName).execute().actionGet();
		if (res.isExists()) {
			DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(indexName);
			delIdx.execute().actionGet();
		}

		CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(indexName);

		XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(documentType)
				.startObject("properties");
		mappingBuilder.startObject("Content").field("type", "string").field("analyzer", "standard").endObject();

		mappingBuilder.startObject("Created").field("type", "date").field("format", "yyyy-MM-dd").

		startObject("fields").startObject("raw").field("type", "date").field("format", "yyyy-MM-dd").endObject()
		.endObject().endObject();

		System.out.println("Creating entities mapping ...");
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

	private void createEntitesPerTypeMappings(XContentBuilder mappingBuilder, String neType) throws IOException {

		//mappingBuilder.startObject(neType).field("type", "nested");
		mappingBuilder.startObject(neType);
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("EntId").field("type", "long").endObject();
		mappingBuilder.startObject("Entname").field("type", "string").field("analyzer", "standard").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("EntType").field("type", "string").field("analyzer", "standard").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("EntFrequency").field("type", "long").endObject().endObject().endObject();
	}

	private static void createEventTimeMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("EventTimes");	
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Beginoffset").field("type", "long").endObject()
		.startObject("Endoffset").field("type", "long").endObject();
		mappingBuilder.startObject("TimeXType").field("type", "string").field("analyzer", "standard")
		.startObject("fields").startObject("raw").field("type", "string").field("index", "not_analyzed")
		.endObject().endObject().endObject();
		mappingBuilder.startObject("Timex").field("type", "string").field("analyzer", "standard").startObject("fields")
		.startObject("raw").field("type", "string").field("index", "not_analyzed").endObject().endObject()
		.endObject();
		mappingBuilder.startObject("Timexvalue").field("type", "string").field("analyzer", "standard")
		.startObject("fields").startObject("raw").field("type", "string").field("index", "not_analyzed")
		.endObject().endObject().endObject().endObject().endObject();
	}



	private static void createKeywordsMappings(XContentBuilder mappingBuilder) throws IOException {
		mappingBuilder.startObject("Keywords");	
		mappingBuilder.startObject("properties");
		mappingBuilder.startObject("Keyword").field("type", "String").field("analyzer", "standard")
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

}
