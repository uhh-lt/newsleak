package uhh_lt.newsleak.annotator;

import java.text.BreakIterator;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;
import org.apache.uima.fit.util.JCasUtil;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
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

}
