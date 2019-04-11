package uhh_lt.newsleak.writer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.apache.uima.fit.util.JCasUtil;

import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.types.Paragraph;

/**
 * A writer to populate a temporary elasticsearch index with fulltexts from a
 * prior annotation chain. This writer may modify original document contents in
 * the following way:
 * 
 * - splitting of long documents into paragraphs of a certain minimum length
 * (1500 characters, i.e. one norm page).
 * 
 * - standardization of line breaks and conversion of of HTML line breaks /
 * paragraph markup to text line breaks
 * 
 * - pruning of documents to a maximum length (can be configured in the
 * preprocessing configuration)
 * 
 * Paragraph splitting is heuristically assumed at occurrence of one or more
 * empty lines.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class ElasticsearchDocumentWriter extends JCasAnnotator_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant ES_TYPE_DOCUMENT. */
	public static final String ES_TYPE_DOCUMENT = "document";

	/** The Constant RESOURCE_ESCLIENT. */
	public static final String RESOURCE_ESCLIENT = "esResource";

	/** The es resource. */
	@ExternalResource(key = RESOURCE_ESCLIENT)
	private ElasticsearchResource esResource;

	/** The elasticsearch client. */
	private TransportClient client;

	/** The Constant PARAM_PARAGRAPHS_AS_DOCUMENTS. */
	public static final String PARAM_PARAGRAPHS_AS_DOCUMENTS = "splitIntoParagraphs";

	/** The split into paragraphs. */
	@ConfigurationParameter(name = PARAM_PARAGRAPHS_AS_DOCUMENTS, mandatory = false, defaultValue = "false")
	private boolean splitIntoParagraphs;
	
	/** The Constant PARAM_MINIMUM_PARAGRAPH_LENGTH. */
	public static final String PARAM_MINIMUM_PARAGRAPH_LENGTH = "MINIMUM_PARAGRAPH_LENGTH";
	
	/** The minimum paragraph length. */
	@ConfigurationParameter(name = PARAM_MINIMUM_PARAGRAPH_LENGTH, mandatory = false, defaultValue = "1500")
	private int MINIMUM_PARAGRAPH_LENGTH;

	/** The paragraph pattern. */
	private Pattern paragraphPattern = Pattern.compile("[?!\\.]( *\\r?\\n){2,}", Pattern.MULTILINE);

	/** The Constant PARAM_MAX_DOC_LENGTH. */
	public static final String PARAM_MAX_DOC_LENGTH = "maxDocumentLength";

	/** The max document length. */
	@ConfigurationParameter(name = PARAM_MAX_DOC_LENGTH, mandatory = false)
	protected Integer maxDocumentLength = Integer.MAX_VALUE;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.apache.
	 * uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		client = esResource.getClient();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.
	 * uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();

		// skip indexing empty documents
		if (docText.trim().length() > 0) {

			// always convert windows line breaks to unix line break
			docText = docText.replaceAll("\\r\\n", "\n");
			docText = docText.replaceAll("\\r", "\n");

			// process text normalization
			docText = dehyphenate(docText);
			docText = replaceHtmlLineBreaks(docText);

			// get temporary document id (as assigned by the reader) and prepare mapping to
			// new ids
			Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
			String tmpDocId = metadata.getDocId();
			ArrayList<Integer> newsleakDocIds = new ArrayList<Integer>();

			if (!splitIntoParagraphs) {
				// write entire document into the index
				newsleakDocIds.add(writeToIndex(jcas, docText, tmpDocId));
			} else {
				// look for paragraoph boundaries
				annotateParagraphs(jcas);
				// write each paragraph as new document into the index
				for (Paragraph paragraph : JCasUtil.select(jcas, Paragraph.class)) {
					newsleakDocIds.add(writeToIndex(jcas, paragraph.getCoveredText(), tmpDocId));
				}
			}

			// keep track of mapping from tmp ids to new ids (for metadata assignment)
			esResource.addDocumentIdMapping(Integer.parseInt(tmpDocId), newsleakDocIds);

		}

	}

	/**
	 * Write document to temporary newsleak elasticsearch index.
	 *
	 * @param jcas
	 *            the jcas
	 * @param docText
	 *            the doc text
	 * @param tmpDocId
	 *            the tmp doc id
	 * @return the integer
	 */
	public Integer writeToIndex(JCas jcas, String docText, String tmpDocId) {

		// init with tmp id
		Integer newsleakDocId = Integer.parseInt(tmpDocId);

		if (docText.length() > maxDocumentLength) {
			// skip overly long documents
			logger.log(Level.SEVERE,
					"Skipping document " + tmpDocId + ". Exceeds maximum length (" + maxDocumentLength + ")");
		} else {
			Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			// generate new id from auto-increment
			newsleakDocId = esResource.getNextDocumentId();

			// index document, and date + language metadata alogn with new document id
			XContentBuilder builder;
			try {
				Date created = dateFormat.parse(metadata.getTimestamp());
				builder = XContentFactory.jsonBuilder().startObject().field("id", newsleakDocId.toString())
						.field("Content", docText).field("Created", dateFormat.format(created))
						.field("DocumentLanguage", jcas.getDocumentLanguage()).endObject();
				IndexResponse response = client
						.prepareIndex(esResource.getIndex(), ES_TYPE_DOCUMENT, newsleakDocId.toString())
						.setSource(builder).get();
				logger.log(Level.INFO, response.toString());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				logger.log(Level.SEVERE, "Could not parse document date from document " + tmpDocId);
				e.printStackTrace();
			} catch (NullPointerException e) {
				logger.log(Level.SEVERE, "No date for document " + tmpDocId);
				e.printStackTrace();
			}
		}

		return newsleakDocId;
	}

	/**
	 * Replace html line breaks and &gt; &lt; entities.
	 *
	 * @param html
	 *            the html
	 * @return the string
	 */
	public static String replaceHtmlLineBreaks(String html) {
		if (html == null)
			return html;
		Document document = Jsoup.parse(html);
		// makes html() preserve linebreaks and spacing
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));
		document.select("br").append("\\n");
		document.select("p").prepend("\\n\\n");
		String s = document.html().replaceAll("\\\\n", "\n");
		String cleanedString = Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
		cleanedString = cleanedString.replaceAll("&gt;", ">");
		cleanedString = cleanedString.replaceAll("&lt;", "<");
		return cleanedString;
	}

	/**
	 * An advanced dehyphanator based on regex.
	 * 
	 * - " -" is accepted as hyphen
	 * 
	 * - "und"/"and" and "or"/"oder" in second line prevent dehyphenation
	 * 
	 * - leaves the hyphen if there are a number or an upper case letter as first
	 * character in second line
	 * 
	 * - deletes the first spaces in second line
	 * 
	 * @param sequence
	 *            A string to dehyphenate
	 * @return A dehyphenated string
	 */
	public static String dehyphenate(String sequence) {
		if (!sequence.contains("\n")) {
			// do nothing
			return sequence;
		}
		String dehyphenatedString = sequence.replaceAll(" ", " ");
		StringBuilder regexForDehyphenation = new StringBuilder();
		// Before hyphen a string with letters, numbers and signs
		regexForDehyphenation.append("(\\s)*(\\S*\\w{2,})");
		// a hyphen, some spaces, a newline and some spaces
		regexForDehyphenation.append("([‐‑‒–]\\s*\\n{1,2}\\s*)");
		// the first word starts
		regexForDehyphenation.append("(");
		// no 'and' or 'or' in new line
		regexForDehyphenation.append("(?!und )(?!oder )(?!and )(?!or )");
		// the first two characters are not allowed to be numbers or punctuation
		regexForDehyphenation.append("(?![\\p{P}\\p{N}])");
		// the first word end ending of this group
		regexForDehyphenation.append("\\w+)");
		Pattern p = Pattern.compile(regexForDehyphenation.toString(), Pattern.UNICODE_CHARACTER_CLASS);
		Matcher m = p.matcher(sequence);
		while (m.find()) {
			String sep = "";
			Character firstLetterOfNewline = m.group(4).toCharArray()[0];
			// If the first character of the word in the second line is uppercase or a
			// number leave the hyphen
			if (Character.isUpperCase(firstLetterOfNewline) || Character.isDigit(firstLetterOfNewline)) {
				sep = "-";
			}
			String replaceString = "\n" + m.group(2) + sep + m.group(4);
			dehyphenatedString = dehyphenatedString.replace(m.group(0), replaceString);
		}
		return dehyphenatedString;
	}

	/**
	 * Annotate paragraphs.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void annotateParagraphs(JCas jcas) {

		Matcher matcher = paragraphPattern.matcher(jcas.getDocumentText());
		Paragraph paragraph = new Paragraph(jcas);
		paragraph.setBegin(0);
		paragraph.setLanguage(jcas.getDocumentLanguage());
		while (matcher.find()) {
			if (matcher.start() > 0 && (matcher.start() - paragraph.getBegin()) > MINIMUM_PARAGRAPH_LENGTH) {
				paragraph.setEnd(matcher.start() + 1);
				paragraph.addToIndexes();
				paragraph = new Paragraph(jcas);
				paragraph.setBegin(matcher.end());
				paragraph.setLanguage(jcas.getDocumentLanguage());
			}
		}
		paragraph.setEnd(jcas.getDocumentText().length());
		paragraph.addToIndexes();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
	 * collectionProcessComplete()
	 */
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		try {
			esResource.writeDocumentIdMapping();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		super.collectionProcessComplete();
	}

}
