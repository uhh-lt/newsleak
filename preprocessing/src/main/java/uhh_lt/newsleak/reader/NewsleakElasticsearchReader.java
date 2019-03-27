package uhh_lt.newsleak.reader;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;

import de.unihd.dbs.uima.types.heideltime.Dct;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.writer.ElasticsearchDocumentWriter;

/**
 * This reader reads from a temporary elasticsearch index which has been
 * populated by an earlier preprocessing process. The earlier process is
 * expected to provide a document id, the fulltext, a document imte stamp, and
 * document language information.
 * 
 * The reader reads all documents for exactly one language into CASes for
 * further information extraction processing steps.
 */
public class NewsleakElasticsearchReader extends CasCollectionReader_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant RESOURCE_ESCLIENT. */
	public static final String RESOURCE_ESCLIENT = "esResource";

	/** The es resource. */
	@ExternalResource(key = RESOURCE_ESCLIENT)
	private ElasticsearchResource esResource;

	/** The Constant PARAM_LANGUAGE. */
	public static final String PARAM_LANGUAGE = "language";

	/** The language. */
	@ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = true)
	private String language;

	/** The client. */
	private TransportClient client;

	/** The es index. */
	private String esIndex;

	/** The total records. */
	private long totalRecords = 0;

	/** The current record. */
	private int currentRecord = 0;

	/** The total id list. */
	private ArrayList<String> totalIdList;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.CasCollectionReader_ImplBase#initialize(org.
	 * apache.uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		client = esResource.getClient();
		esIndex = esResource.getIndex();

		try {
			XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("match").startObject()
					.field("DocumentLanguage", language).endObject().endObject();

			// retrieve all ids
			totalIdList = new ArrayList<String>();
			SearchResponse scrollResp = client.prepareSearch(esIndex)
					.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC).setScroll(new TimeValue(60000))
					.setQuery(builder).setSize(10000).execute().actionGet();
			while (true) {
				for (SearchHit hit : scrollResp.getHits().getHits()) {
					totalIdList.add(hit.getId());
				}
				scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000))
						.execute().actionGet();
				// Break condition: No hits are returned
				if (scrollResp.getHits().getHits().length == 0) {
					break;
				}
			}

			totalRecords = totalIdList.size();
			logger.log(Level.INFO, "Found " + totalRecords + " for language " + language + " in index");

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// System.exit(1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
	 */
	public void getNext(CAS cas) throws IOException, CollectionException {
		JCas jcas;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}

		String docId = totalIdList.get(currentRecord);
		GetResponse response = client.prepareGet(esIndex, ElasticsearchDocumentWriter.ES_TYPE_DOCUMENT, docId)
				.setFields("Content", "Created").get();

		jcas.setDocumentText((String) response.getField("Content").getValue());
		jcas.setDocumentLanguage(language);

		// Set metadata
		Metadata metaCas = new Metadata(jcas);
		metaCas.setDocId(docId);
		String docDate = (String) response.getField("Created").getValue();
		metaCas.setTimestamp(docDate);
		metaCas.addToIndexes();

		// heideltime
		Dct dct = new Dct(jcas);
		dct.setValue(docDate);
		dct.addToIndexes();

		currentRecord++;

		logger.log(Level.FINEST, "Document ID: " + docId);
		logger.log(Level.FINEST, "Document Length: " + jcas.getDocumentText().length());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	 */
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(Long.valueOf(currentRecord).intValue(),
				Long.valueOf(totalRecords).intValue(), Progress.ENTITIES) };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {
		return currentRecord < totalRecords ? true : false;
	}

}
