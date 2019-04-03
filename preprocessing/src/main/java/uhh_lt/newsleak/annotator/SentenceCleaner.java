package uhh_lt.newsleak.annotator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import com.ibm.icu.text.DateFormatSymbols;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.types.Metadata;

/**
 * Annotator to remove implausible Sentences. The ICU4J sentence segmentation
 * may result in unlikely sentence structures such as very long base64-encoded
 * blocks from email attachments. This annotator restricts sentence annotations
 * to a maximum number of tokens and tokens to a maximum character length.
 * Overly long tokens are removed from the CAS index. Overly long sentences are
 * split into smaller chunks. In addition, wrong sentence breaks in German dates
 * (e.g. "25. September 2019") are corrected. Also, documents are flagged by
 * noFulltextDocument=true in the document metadata of the CAS if a very low
 * type token ratio is observed.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class SentenceCleaner extends JCasAnnotator_ImplBase {

	/** The Constant TTR_THRESHOLD. */
	private static final double TTR_THRESHOLD = 0.01;

	/** The Constant MAX_TOKENS_PER_SENTENCE. */
	public static final int MAX_TOKENS_PER_SENTENCE = 150;

	/** The Constant RESIZE_TOKENS_PER_SENTENCE. */
	public static final int RESIZE_TOKENS_PER_SENTENCE = 25;

	/** The Constant MAX_TOKEN_LENGTH. */
	private static final int MAX_TOKEN_LENGTH = 70;

	/** Acquire German month names */
	DateFormatSymbols dfs;

	/** The month names. */
	HashSet<String> monthNames;

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
		log.setLevel(Level.ALL);
		dfs = new DateFormatSymbols(Locale.GERMAN);
		monthNames = new HashSet<String>(Arrays.asList(dfs.getMonths()));
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

		// step 1
		cleanTokens(jcas);

		// step 2
		repairSentenceBreaks(jcas);

		// step 3
		restructureSentences(jcas);

		// flag unlikely fulltext documents
		flagUnlikelyDocuments(jcas);

	}

	/**
	 * This function removes token annotations from overly long tokens which can be
	 * large URLs or base64 encoded data. After token annotation removal potentially
	 * empty sentences are removed, too.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void cleanTokens(JCas jcas) {

		// remove too long tokens
		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
		for (Token token : tokens) {
			if (token.getCoveredText().length() > MAX_TOKEN_LENGTH) {
				token.removeFromIndexes();
			}
		}

		// remove empty sentences
		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
		for (Sentence sentence : sentences) {
			tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
			if (tokens.isEmpty()) {
				sentence.removeFromIndexes();
			}
		}

	}

	/**
	 * ICU sentence separator separates German (and other language?) constructions
	 * with dates such as "25. Oktober". This function merges sentences falsely
	 * separated at this date punctuation mark.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void repairSentenceBreaks(JCas jcas) {

		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);

		// merge falsely separated sentences
		List<Token> lastSentenceTokens = new ArrayList<Token>();
		Sentence lastSentence = null;
		for (Sentence sentence : sentences) {
			List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
			// check pattern
			if (monthNames.contains(tokens.get(0).getCoveredText()) && lastSentenceTokens.size() > 1
					&& lastSentenceTokens.get(lastSentenceTokens.size() - 2).getCoveredText().matches("\\d{1,2}")
					&& lastSentenceTokens.get(lastSentenceTokens.size() - 1).getCoveredText().matches("\\.")) {
				lastSentence.setEnd(sentence.getEnd());
				lastSentence.addToIndexes();
				sentence.removeFromIndexes();
			}
			lastSentenceTokens = tokens;
			lastSentence = sentence;
		}

	}

	/**
	 * Sentence tokenization may lead to non-sentences, e.g. very long lists or
	 * automatically generated text files (e.g. logs). This function splits overly
	 * long sentences into smaller pieces.
	 *
	 * @param jcas
	 *            the jcas
	 */
	public void restructureSentences(JCas jcas) {
		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
		for (Sentence sentence : sentences) {

			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(),
					sentence.getEnd());

			if (tokens.size() > MAX_TOKENS_PER_SENTENCE) {

				log.log(Level.FINEST, "Restructuring long sentence: " + sentence.getBegin() + " " + sentence.getEnd());

				int sStart = sentence.getBegin();
				boolean startNew = true;
				int nTok = 0;

				for (Token token : tokens) {
					nTok++;
					if (startNew) {
						sStart = token.getBegin();
						startNew = false;
					}
					if (nTok % RESIZE_TOKENS_PER_SENTENCE == 0) {
						Sentence s = new Sentence(jcas);
						s.setBegin(sStart);
						s.setEnd(token.getEnd());
						s.addToIndexes();
						startNew = true;
						log.log(Level.FINEST, "New sentence: " + sStart + " " + token.getEnd());
					}
				}
				if (!startNew) {
					Sentence s = new Sentence(jcas);
					s.setBegin(sStart);
					s.setEnd(sentence.getEnd());
					s.addToIndexes();
					log.log(Level.FINEST, "New sentence: " + sStart + " " + sentence.getEnd());
				}

				sentence.removeFromIndexes();

			}
		}
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
	private void flagUnlikelyDocuments(JCas jcas) {

		boolean noFulltextDocument = false;

		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);

		if (tokens.size() > 10000) {

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

			double typeTokenRatio = vocabulary.size() / tokenCount;

			// set flag for very low TTR
			if (typeTokenRatio < TTR_THRESHOLD) {
				noFulltextDocument = true;
				log.log(Level.INFO,
						"Unlikely fulltext document flagged: " + jcas.getDocumentText().substring(0, 2500));
			}

		}

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		metadata.setNoFulltextDocument(noFulltextDocument);
		metadata.addToIndexes();

	}

}
