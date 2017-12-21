package uhh_lt.newsleak.writer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.types.Metadata;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=false)
public class ElasticsearchDocumentWriter extends JCasAnnotator_ImplBase {

	private Logger logger;
	
	public static final String ES_TYPE_DOCUMENT = "document";

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
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		XContentBuilder builder;
		try {
			Date created = dateFormat.parse(metadata.getTimestamp());
			builder = XContentFactory.jsonBuilder()
					.startObject()
					.field("id", metadata.getDocId())
					.field("Content", docText)
					.field("Created", dateFormat.format(created))
					.field("DocumentLanguage", jcas.getDocumentLanguage())
					.endObject();
			IndexResponse response = client.prepareIndex(esResource.getIndex(), ES_TYPE_DOCUMENT, metadata.getDocId())
					.setSource(builder).get();
			logger.log(Level.INFO, response.toString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			logger.log(Level.SEVERE, "Could not parse document date from document " + metadata.getDocId());
			e.printStackTrace();
		}
	}

}
