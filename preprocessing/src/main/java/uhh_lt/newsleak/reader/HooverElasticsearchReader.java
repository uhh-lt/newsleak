package uhh_lt.newsleak.reader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;

import de.unihd.dbs.uima.types.heideltime.Dct;
import uhh_lt.newsleak.resources.HooverResource;
import uhh_lt.newsleak.resources.MetadataResource;
import uhh_lt.newsleak.types.Metadata;

public class HooverElasticsearchReader extends CasCollectionReader_ImplBase {

	private Logger logger;

	public static final String RESOURCE_HOOVER = "hooverResource";
	@ExternalResource(key = RESOURCE_HOOVER)
	private HooverResource hooverResource;
	
	public static final String RESOURCE_METADATA = "metadataResource";
	@ExternalResource(key = RESOURCE_METADATA)
	private MetadataResource metadataResource;

	public static final String PARAM_DEBUG_MAX_DOCS = "maxRecords";

	private static final int MAXIMUM_DOCUMENT_LENGTH = 1500 * 50; // 50 norm pages
	@ConfigurationParameter(name = PARAM_DEBUG_MAX_DOCS, mandatory = false)
	private Integer maxRecords = Integer.MAX_VALUE;

	private TransportClient client;
	private String esIndex;

	private int totalRecords = 0;
	private int currentRecord = 0;

	private ArrayList<String> totalIdList;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat dateCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	SimpleDateFormat dateJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		logger = context.getLogger();
		client = hooverResource.getClient();
		esIndex = hooverResource.getIndex();

		try {

			XContentBuilder builder = XContentFactory.jsonBuilder()
					.startObject()
					.field("match_all")
					.startObject()
					.endObject().endObject();
			totalIdList = new ArrayList<String>();
			logger.log(Level.INFO, "Start scroll request on " + esIndex);
			SearchRequestBuilder sb = client.prepareSearch(esIndex)
					.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
					.setScroll(TimeValue.timeValueSeconds(15L))
					.setQuery(builder)
					.setFetchSource(false)
					.setSize(10000); 
			System.out.println(sb.toString());
			SearchResponse scrollResp = sb.execute().actionGet();
			while (true) {
				logger.log(Level.INFO, "Continuing scroll request on " + esIndex);
				for (SearchHit hit : scrollResp.getHits().getHits()) {
					totalIdList.add(hit.getId());
				}
				scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(TimeValue.timeValueSeconds(15L)).execute().actionGet();
				//Break condition: No hits are returned
				int nHits = scrollResp.getHits().getHits().length;
				if (nHits == 0) {
					break;
				}
				logger.log(Level.INFO, "Added hits " + nHits);
			}
			
			if (maxRecords > 0 && maxRecords < totalIdList.size()) {
				totalIdList = new ArrayList<String>(totalIdList.subList(0, maxRecords));
			}

			totalRecords = totalIdList.size();
			logger.log(Level.INFO, "Found " + totalRecords + " in index " + esIndex);

		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}


	public void getNext(CAS cas) throws IOException, CollectionException {
		JCas jcas;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}

		String docId = totalIdList.get(currentRecord - 1);
		GetField field;
		GetResponse response = client
				.prepareGet(esIndex, "doc", docId)
				.setFields("attachments", "date", "date-created", "content-type", 
						"filetype", "from", "to", "in-reply-to", "subject", "text",
						"filename", "path")
				.get();

		String docText = "";
		// put email header information in main text
		field = response.getField("from");
		if (field != null) {
			docText += ((String) field.getValue()).trim() + "\n";
		}
		field = response.getField("to");
		if (field != null) {
			docText += ((String) field.getValue()).trim() + "\n";
		}
		field = response.getField("subject");
		if (field != null) {
			docText += ((String) field.getValue()).trim() + "\n\n";
		}
		field = response.getField("text");
		if (field != null) {
			String completeText = ((String) field.getValue()).trim();
			docText += completeText.substring(0, Math.min(completeText.length(), MAXIMUM_DOCUMENT_LENGTH));
		}
		jcas.setDocumentText(docText);

		// set document metadata
		Metadata metaCas = new Metadata(jcas);
		metaCas.setDocId(docId);

		// date
		String docDate = "1900-01-01";
		try {
			GetField date = response.getField("date");
			if (date != null) {
				docDate = dateFormat.format(dateCreated.parse((String) date.getValue()));
			} else {
				date = response.getField("date-created");
				if (date != null) {
					docDate = dateFormat.format(dateJson.parse((String) date.getValue())) ;
				}
			}
			
			metaCas.setTimestamp(docDate);

			// heideltime
			Dct dct = new Dct(jcas);
			dct.setValue(docDate);
			dct.addToIndexes();

		} catch (ParseException e) {
			e.printStackTrace();
		}

		metaCas.addToIndexes();

		// write external metadata
		ArrayList<List<String>> metadata = new ArrayList<List<String>>();

		// subject, filename, path
		field = response.getField("subject");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "subject", ((String) field.getValue()).toString()));
		field = response.getField("filename");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "filename", ((String) field.getValue()).toString()));
		field = response.getField("path");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "path", ((String) field.getValue()).toString()));

		// attachments
		field = response.getField("attachments");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "attachments", ((Boolean) field.getValue()).toString()));
		// content-type
		field = response.getField("content-type");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "content-type", (String) field.getValue()));
		// file-type
		field = response.getField("filetype");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docId, "filetype", (String) field.getValue()));
		// from
		field = response.getField("from");
		if (field != null) {
			for (String email : extractEmail((String) field.getValue())) {
				metadata.add(metadataResource.createTextMetadata(docId, "from", email));
			}
		}
		// to
		field = response.getField("to");
		if (field != null) {
			for (Object toList : field.getValues()) {
				for (String email : extractEmail((String) toList)) {
					metadata.add(metadataResource.createTextMetadata(docId, "to", email));
				}
			}
		}
		metadataResource.appendMetadata(metadata);

	}

	private ArrayList<String> extractEmail(String s) {
		ArrayList<String> emails = new ArrayList<String>();
		Matcher m = emailPattern.matcher(s);
		while (m.find()) {
			emails.add(m.group());
		}
		return emails;
	}



	public Progress[] getProgress() {
		return new Progress[] {
				new ProgressImpl(
						Long.valueOf(currentRecord).intValue() - 1,
						Long.valueOf(totalRecords).intValue(),
						Progress.ENTITIES
						)
		};
	}

	public boolean hasNext() throws IOException, CollectionException {
		if (currentRecord < totalRecords) {
			currentRecord++;
			return true;
		} else {
			return false;
		}
	}

}
