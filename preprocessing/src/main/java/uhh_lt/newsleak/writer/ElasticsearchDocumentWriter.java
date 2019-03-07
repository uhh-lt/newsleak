package uhh_lt.newsleak.writer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.uimafit.util.JCasUtil;

import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.types.Paragraph;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=false)
public class ElasticsearchDocumentWriter extends JCasAnnotator_ImplBase {

	private Logger logger;

	public static final String ES_TYPE_DOCUMENT = "document";

	public static final String RESOURCE_ESCLIENT = "esResource";
	@ExternalResource(key = RESOURCE_ESCLIENT)
	private ElasticsearchResource esResource;

	private TransportClient client;

	public static final String PARAM_PARAGRAPHS_AS_DOCUMENTS = "splitIntoParagraphs";
	@ConfigurationParameter(name = PARAM_PARAGRAPHS_AS_DOCUMENTS, mandatory = false, defaultValue = "false")
	private boolean splitIntoParagraphs;

	private Pattern paragraphPattern = Pattern.compile("[?!\\.]( *\\r?\\n){2,}", Pattern.MULTILINE);
	public static int MINIMUM_PARAGRAPH_LENGTH = 1500;

	public static final String PARAM_MAX_DOC_LENGTH = "maxDocumentLength";
	@ConfigurationParameter(name = PARAM_MAX_DOC_LENGTH, mandatory = false)
	protected Integer maxDocumentLength = Integer.MAX_VALUE;


	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		client = esResource.getClient();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();

		// skip indexing empty documents
		if (docText.trim().length() > 0) {

			// always convert windows line breaks to unix line break
			docText = docText.replaceAll("\\r\\n", "\n");
			docText = docText.replaceAll("\\r", "\n");

			docText = dehyphenate(docText);
			docText = replaceHtmlLineBreaks(docText);

			Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
			String docId = metadata.getDocId();

			if (!splitIntoParagraphs) {
				writeToIndex(jcas, docText, docId);
			} else {
				annotateParagraphs(jcas);
				int i = 0;
				for (Paragraph paragraph : JCasUtil.select(jcas, Paragraph.class)) {
					i++;
					String pId = "" + (Integer.parseInt(docId) + i);
					writeToIndex(jcas, paragraph.getCoveredText(), pId);
				}

			}

		}

	}

	public void writeToIndex(JCas jcas, String docText, String docId) {

		if (docText.length() > maxDocumentLength) {
			logger.log(Level.SEVERE, "Skipping document " + docId + ". Exceeds maximum length (" + maxDocumentLength + ")");
		} else {
			Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			XContentBuilder builder;
			try {
				Date created = dateFormat.parse(metadata.getTimestamp());
				builder = XContentFactory.jsonBuilder()
						.startObject()
						.field("id", docId)
						.field("Content", docText)
						.field("Created", dateFormat.format(created))
						.field("DocumentLanguage", jcas.getDocumentLanguage())
						.endObject();
				IndexResponse response = client.prepareIndex(esResource.getIndex(), ES_TYPE_DOCUMENT, docId)
						.setSource(builder).get();
				logger.log(Level.INFO, response.toString());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				logger.log(Level.SEVERE, "Could not parse document date from document " + docId);
				e.printStackTrace();
			} catch (NullPointerException e) {
				logger.log(Level.SEVERE, "No date for document " + docId);
				e.printStackTrace();
			}
		}	

	}


	public static String replaceHtmlLineBreaks(String html) {
		if (html == null)
			return html;
		Document document = Jsoup.parse(html);
		//makes html() preserve linebreaks and spacing
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));
		document.select("br").append("\\n");
		document.select("p").prepend("\\n\\n");
		String s = document.html().replaceAll("\\\\n", "\n");
		return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
	}


	/**
	 * An advanced dehyphanator based on regex.
	 *  - " -" is accepted as hyphen
	 *  - "und"/"and" and "or"/"oder" in second line prevent dehyphenation
	 *  - leaves the hyphen if there are a number or an upper case letter
	 *    as first character in second line
	 *  - deletes the first spaces in second line
	 * @param sequence A string to dehyphenate
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
		while(m.find()){
			String sep = "";
			Character firstLetterOfNewline = m.group(4).toCharArray()[0];
			// If the first character of the word in the second line is uppercase or a number leave the hyphen
			if (Character.isUpperCase(firstLetterOfNewline) || Character.isDigit(firstLetterOfNewline)) {
				sep = "-";
			}
			String replaceString =  "\n" + m.group(2) + sep + m.group(4);
			dehyphenatedString = dehyphenatedString.replace(m.group(0), replaceString);
		}
		return dehyphenatedString;
	}



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

}
