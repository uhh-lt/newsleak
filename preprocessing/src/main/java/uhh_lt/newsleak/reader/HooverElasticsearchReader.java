package uhh_lt.newsleak.reader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.unihd.dbs.uima.types.heideltime.Dct;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Get;
import io.searchbox.core.Search;
import io.searchbox.core.SearchScroll;
import io.searchbox.params.Parameters;
import uhh_lt.newsleak.resources.HooverResource;
import uhh_lt.newsleak.resources.MetadataResource;
import uhh_lt.newsleak.types.Metadata;

/**
 * The HooverElasticsearchReader connects to a running instance of the Hoover
 * text data extraction system created by the EIC.network (see
 * <a href="https://hoover.github.io">https://hoover.github.io</a>). It utilizes
 * the Hoover API to query for all extracted documents in a collection.
 * 
 * Hoover is expected to extract raw fulltext (regardless of any further NLP
 * application or human analysts requirement). Newsleak takes Hoover's output
 * and extracted file metadata (e.g. creation date).
 * 
 * Duplicated storage of fulltexts (with newly generated document IDs) is
 * necessary since we clean and preprocess raw data for further annotation
 * processes. Among others, this includes deletion of multiple blank lines
 * (often extracted from Excel sheets), dehyphenation at line endings (a result
 * from OCR-ed or badly encoded PDFs) , or splitting of long documents into
 * chunks of roughly page length.
 * 
 * This reader sets document IDs to 0. The final document IDs will be generated
 * as an automatically incremented by @see
 * uhh_lt.newsleak.writer.ElasticsearchDocumentWriter
 * 
 * Metadata is written into a temporary file on the disk to be inserted into the
 * newsleak postgres database lateron.
 */
public class HooverElasticsearchReader extends NewsleakReader {

	/** The Constant RESOURCE_HOOVER. */
	public static final String RESOURCE_HOOVER = "hooverResource";

	/** The hoover resource. */
	@ExternalResource(key = RESOURCE_HOOVER)
	private HooverResource hooverResource;

	/** The Constant RESOURCE_METADATA. */
	public static final String RESOURCE_METADATA = "metadataResource";

	/** The metadata resource. */
	@ExternalResource(key = RESOURCE_METADATA)
	private MetadataResource metadataResource;

	/** The Constant PARAM_SCROLL_SIZE. */
	private static final String PARAM_SCROLL_SIZE = "10000";

	/** The Constant PARAM_SCROLL_TIME. */
	private static final String PARAM_SCROLL_TIME = "1m";

	/** JEST client to run JSON API requests. */
	private JestClient client;

	/** The Hoover elasticsearch index. */
	private String esIndex;

	/** The total records. */
	private int totalRecords = 0;

	/** The current record. */
	private int currentRecord = 0;

	/** The list of all Ids. */
	private ArrayList<String> totalIdList;

	/** The date format. */
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	/** The date created. */
	SimpleDateFormat dateCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/** The date json. */
	SimpleDateFormat dateJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

	/** The email regex pattern. */
	Pattern emailPattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");

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
		// init hoover connection
		client = hooverResource.getClient();
		esIndex = hooverResource.getIndex();

		// query hoover's elasticsearch index
		Search search = new Search.Builder(
				"{\"query\": {\"match_all\" : {}}, \"_source\" : false, \"size\" : " + PARAM_SCROLL_SIZE + "}")
						.addIndex(hooverResource.getIndex()).addType(HooverResource.HOOVER_DOCUMENT_TYPE)
						.setParameter(Parameters.SCROLL, PARAM_SCROLL_TIME).build();

		try {

			// run JEST request
			JestResult result = client.execute(search);

			totalIdList = new ArrayList<String>();

			JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
			Integer total = result.getJsonObject().getAsJsonObject("hits").get("total").getAsInt();

			int nHits = hits.size();

			logger.log(Level.INFO, "Hits first result: " + nHits);
			logger.log(Level.INFO, "Hits total: " + total);

			totalIdList.addAll(hooverResource.getIds(hits));

			String scrollId = result.getJsonObject().get("_scroll_id").getAsString();

			// run scroll request to collect all Ids
			int i = 0;
			while (nHits > 0) {
				SearchScroll scroll = new SearchScroll.Builder(scrollId, PARAM_SCROLL_TIME).build();

				result = client.execute(scroll);

				hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
				nHits = hits.size();
				logger.log(Level.INFO, "Hits " + ++i + " result: " + nHits);
				totalIdList.addAll(hooverResource.getIds(hits));
				scrollId = result.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();
			}

			if (maxRecords > 0 && maxRecords < totalIdList.size()) {
				totalIdList = new ArrayList<String>(totalIdList.subList(0, maxRecords));
			}

			totalRecords = totalIdList.size();
			logger.log(Level.INFO, "Found " + totalRecords + " ids in index " + esIndex);

		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}

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

		// temporary document Id (a new id will be generated by the
		// ElasticsearchDocumentWriter)
		String docIdNewsleak = Integer.toString(currentRecord);
		String docIdHoover = totalIdList.get(currentRecord - 1);

		logger.log(Level.INFO, "Proceessing document: " + docIdHoover);

		Get get = new Get.Builder(hooverResource.getIndex(), docIdHoover).type(HooverResource.HOOVER_DOCUMENT_TYPE)
				.build();
		JestResult getResult = client.execute(get);
		JsonObject o = getResult.getJsonObject();
		JsonObject source = o.get("_source").getAsJsonObject();

		String docText = "";
		String field;

		// put email header information in main text
		field = getField(source, "from");
		if (field != null) {
			String fromText = field.trim();
			docText += "From: " + fromText.replaceAll("<", "[").replaceAll(">", "]") + "\n";
		}
		JsonArray arrayField = getFieldArray(source, "to");
		if (arrayField != null) {
			String toList = "";
			for (JsonElement item : arrayField) {
				String toListItem = item.getAsString().trim();
				toListItem = toListItem.replaceAll("<", "[").replaceAll(">", "]");
				toListItem = toListItem.replaceAll("\\s+", " ") + "\n";
				toList += toList.isEmpty() ? toListItem : "; " + toListItem;
			}
			docText += "To: " + toList;
		}
		field = getField(source, "subject");
		if (field != null) {
			docText += "Subject: " + field.trim() + "\n";
		}
		if (!docText.isEmpty()) {
			docText += "\n-- \n\n";
		}

		// add main text
		field = getField(source, "text");
		if (field != null) {
			String completeText = field.trim();
			docText += cleanBodyText(completeText);
		}
		jcas.setDocumentText(docText);

		// set document metadata
		Metadata metaCas = new Metadata(jcas);
		metaCas.setDocId(docIdNewsleak);

		// date
		String docDate = "1900-01-01";
		Date dateField = null;
		Date dateCreatedField = null;
		try {

			String date = getField(source, "date");
			if (date != null) {
				// docDate = dateFormat.format();
				dateField = dateCreated.parse(date);
			}

			date = getField(source, "date-created");
			if (date != null) {
				// docDate = dateFormat.format() ;
				dateCreatedField = dateJson.parse(date);
			}

			if (dateField != null && dateCreatedField != null) {
				docDate = dateField.before(dateCreatedField) ? dateFormat.format(dateCreatedField)
						: dateFormat.format(dateField);
			} else {
				if (dateField != null) {
					docDate = dateFormat.format(dateField);
				}
				if (dateCreatedField != null) {
					docDate = dateFormat.format(dateCreatedField);
				}
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

		metaCas.setTimestamp(docDate);

		// heideltime
		Dct dct = new Dct(jcas);
		dct.setValue(docDate);
		dct.addToIndexes();

		metaCas.addToIndexes();

		// write external metadata
		ArrayList<List<String>> metadata = new ArrayList<List<String>>();

		// filename, subject, path
		String fileName = "";
		field = getField(source, "filename");
		if (field != null) {
			fileName = field;
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "filename", fileName));
		}
		field = getField(source, "subject");
		if (field != null) {
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "subject", field));
		} else {
			if (!fileName.isEmpty()) {
				metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "subject", fileName));
			}
		}
		field = getField(source, "path");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "path", field));

		// Source Id
		metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "Link", hooverResource.getHooverBasePath() + docIdHoover));

		// attachments
		Boolean booleanField = getFieldBoolean(source, "attachments");
		if (booleanField != null)
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "attachments", booleanField.toString()));
		// content-type
		field = getField(source, "content-type");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "content-type", field));
		// file-type
		field = getField(source, "filetype");
		if (field != null)
			metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "filetype", field));
		// from
		field = getField(source, "from");
		if (field != null) {
			for (String email : extractEmail(field)) {
				metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "from", email));
			}
		}
		// to
		arrayField = getFieldArray(source, "to");
		if (arrayField != null) {
			for (JsonElement toList : arrayField) {
				for (String email : extractEmail(toList.getAsString())) {
					metadata.add(metadataResource.createTextMetadata(docIdNewsleak, "to", email));
				}
			}
		}
		metadataResource.appendMetadata(metadata);

	}

	/**
	 * Returns a string field value from a JSON object.
	 *
	 * @param o
	 *            the Json object
	 * @param fieldname
	 *            the fieldname
	 * @return the field
	 */
	private String getField(JsonObject o, String fieldname) {
		JsonElement fieldValue = o.get(fieldname);
		if (fieldValue == null) {
			return null;
		} else {
			return fieldValue.isJsonNull() ? null : fieldValue.getAsString();
		}
	}

	/**
	 * Returns a boolean field value from a Json Object.
	 *
	 * @param o
	 *            the Json object
	 * @param fieldname
	 *            the fieldname
	 * @return the field boolean
	 */
	private Boolean getFieldBoolean(JsonObject o, String fieldname) {
		JsonElement fieldValue = o.get(fieldname);
		if (fieldValue == null) {
			return null;
		} else {
			return fieldValue.isJsonNull() ? null : fieldValue.getAsBoolean();
		}
	}

	/**
	 * Returns an array field value from a Json Object.
	 *
	 * @param o
	 *            the Json object
	 * @param fieldname
	 *            the fieldname
	 * @return the field array
	 */
	private JsonArray getFieldArray(JsonObject o, String fieldname) {
		JsonElement fieldValue = o.get(fieldname);
		if (fieldValue == null) {
			return null;
		} else {
			return fieldValue.isJsonNull() ? null : fieldValue.getAsJsonArray();
		}
	}

	/**
	 * Extracts email addresses from a given string.
	 *
	 * @param s
	 *            the string to match email patterns in
	 * @return an array of email addresses
	 */
	private ArrayList<String> extractEmail(String s) {
		ArrayList<String> emails = new ArrayList<String>();
		Matcher m = emailPattern.matcher(s);
		while (m.find()) {
			emails.add(m.group());
		}
		return emails;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
	 */
	public Progress[] getProgress() {
		return new Progress[] { new ProgressImpl(Long.valueOf(currentRecord).intValue() - 1,
				Long.valueOf(totalRecords).intValue(), Progress.ENTITIES) };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
	 */
	public boolean hasNext() throws IOException, CollectionException {
		if (currentRecord < totalRecords) {
			currentRecord++;
			return true;
		} else {
			return false;
		}
	}

}
