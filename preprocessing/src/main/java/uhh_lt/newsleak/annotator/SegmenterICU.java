package uhh_lt.newsleak.annotator;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.fit.util.JCasUtil;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.types.Paragraph;

/**
 * Tokenization, sentence and paragraph annotation with the ICU4J library. ICU4J
 * provides segmentation rules for all unicode locales to iterate over tokens
 * and sentences (BreakIterator).
 * 
 * Paragraph splits are heuristically inferred at one or more empty lines.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class SegmenterICU extends JCasAnnotator_ImplBase {

	/** The Constant TTR_THRESHOLD. */
	private static final double TTR_THRESHOLD = 0.02;

	/** The Constant PARAM_LOCALE. */
	public final static String PARAM_LOCALE = "localeString";

	/** The locale string. */
	@ConfigurationParameter(name = PARAM_LOCALE, mandatory = true, description = "Locale string for ICU4J sentence segmentation")
	private String localeString;

	/** The locale map. */
	private Map<String, Locale> localeMap;

	/** The paragraph pattern. */
	private Pattern paragraphPattern = Pattern.compile("( *\\r?\\n){2,}", Pattern.MULTILINE);
	// private Pattern paragraphPattern = Pattern.compile("(^\\s*$)+",
	// Pattern.MULTILINE);

	/** The log. */
	Logger log;

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
		log = context.getLogger();

		localeMap = LanguageDetector.localeToISO();
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

		// annotate paragraphs in document text
		annotateParagraphs(jcas);

		// get locale from current language
		Locale locale = localeMap.get(localeString);

		Collection<Paragraph> paragraphs = JCasUtil.select(jcas, Paragraph.class);
		for (Paragraph paragraph : paragraphs) {

			int parStart = paragraph.getBegin();

			// load language specific rule set
			BreakIterator sentenceBreaker = BreakIterator.getSentenceInstance(locale);
			sentenceBreaker.setText(paragraph.getCoveredText());

			// find sentence breaks
			int sentStart = sentenceBreaker.first();
			for (int sentEnd = sentenceBreaker
					.next(); sentEnd != BreakIterator.DONE; sentStart = sentEnd, sentEnd = sentenceBreaker.next()) {

				Sentence sentence = new Sentence(jcas);
				sentence.setBegin(parStart + sentStart);
				sentence.setEnd(parStart + sentEnd);

				BreakIterator tokenBreaker = BreakIterator.getWordInstance(locale);
				tokenBreaker.setText(sentence.getCoveredText());

				// find token breaks
				int tokStart = tokenBreaker.first();
				boolean containsTokens = false;
				for (int tokEnd = tokenBreaker
						.next(); tokEnd != BreakIterator.DONE; tokStart = tokEnd, tokEnd = tokenBreaker.next()) {
					Token token = new Token(jcas);
					token.setBegin(parStart + sentStart + tokStart);
					token.setEnd(parStart + sentStart + tokEnd);
					// add non-empty tokens
					if (!token.getCoveredText().trim().isEmpty()) {
						token.addToIndexes();
						containsTokens = true;
					}
				}

				// add non-empty sentences
				if (containsTokens) {
					sentence.addToIndexes();
				}

			}

		}

		// flag unlikely fulltext paragraphs (e.g. log files)
		flagDubiousParagraphs(jcas);

	}

	/**
	 * Annotate paragraphs such that every document contains at least one, starting
	 * at the beginning and ending at the end of a document.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void annotateParagraphs(JCas jcas) {

		Matcher matcher = paragraphPattern.matcher(jcas.getDocumentText());
		Paragraph paragraph = new Paragraph(jcas);
		paragraph.setBegin(0);
		paragraph.setLanguage(localeString);
		while (matcher.find()) {
			if (matcher.start() > 0) {
				paragraph.setEnd(matcher.start());
				paragraph.addToIndexes();
				paragraph = new Paragraph(jcas);
				paragraph.setBegin(matcher.end());
				paragraph.setLanguage(localeString);
			}
		}
		paragraph.setEnd(jcas.getDocumentText().length());
		paragraph.addToIndexes();

	}

	/**
	 * Flag unlikely documents. Documents with a very low type token ratio are
	 * assumed to be log files or other non-fulltext documents. These documents can
	 * be excluded from the information extraction pipeline, if the flag
	 * noFulltextDocument is set to true by this annotator.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void flagDubiousParagraphs(JCas jcas) {

		Collection<Paragraph> paragraphs = JCasUtil.select(jcas, Paragraph.class);

		for (Paragraph paragraph : paragraphs) {

			boolean noFulltextParagraph = false;

			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, paragraph.getBegin(), paragraph.getEnd());

			if (tokens.size() >= 1000) {

				// calculate type-token ratio
				int tokenCount = 0;
				HashSet<String> vocabulary = new HashSet<String>();
				for (Token token : tokens) {
					String word = token.getCoveredText();
					if (StringUtils.isNumeric(word)) {
						continue;
					}
					tokenCount++;
					if (!vocabulary.contains(word)) {
						vocabulary.add(word);
					}
				}

				double typeTokenRatio = vocabulary.size() / (double) tokenCount;

				// set flag for very low TTR
				if (typeTokenRatio < TTR_THRESHOLD) {
					noFulltextParagraph = true;
					String paragraphText = paragraph.getCoveredText();
					log.log(Level.INFO, "Unlikely fulltext paragraph flagged:\n----------------------------\n"
							+ paragraphText.substring(0, Math.min(paragraphText.length(), 1000)));
				}

			}

			paragraph.setIsNotFulltext(noFulltextParagraph);
			paragraph.addToIndexes();

		}

	}

}
