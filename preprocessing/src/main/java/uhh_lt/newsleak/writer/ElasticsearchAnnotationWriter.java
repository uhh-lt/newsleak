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
	
	
	private void createMappingEntities() {
		
	}
	
	private void createMappingKeyterms() {
		
	}
	
	private void createMappingTimeX() {
		
	}

	private void documenIndexer(Client client, String indexName,
			String documentType/* , String mappingFile */) throws Exception {


	}


	private void createDynamicIndex(Client client, String indexName,
			String documentType) throws IOException, SQLException {


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
