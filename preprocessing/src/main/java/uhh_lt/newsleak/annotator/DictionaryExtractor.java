package uhh_lt.newsleak.annotator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.uima.Token;
import uhh_lt.newsleak.resources.DictionaryResource;
import uhh_lt.newsleak.resources.DictionaryResource.Dictionary;
import uhh_lt.newsleak.types.DictTerm;

/**
 * A UIMA annotator to annotate regular expression patterns and dictionary
 * lists. REs are emails, phone numbers (does not work too well), URLs, and IP
 * addresses. Dictionaries are text files containing category terms. Dictionary
 * files should be stored in <i>conf/dictionaries</i> and follow the naming
 * convention <i>dictionarytype.langcode</i> (e.g. spam.deu for German spam
 * terms). The files should contain dictionary terms one per line. Terms can be
 * multi word units (MWU). MWUs are searched by regular expression patterns
 * (case-insensitive). Single work units are stemmed and lowercased before
 * comparison with tokens. Matching tokens are annotated as DictTerm type.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class DictionaryExtractor extends JCasAnnotator_ImplBase {

	/** REGEX_EMAIL. */
	public static final Pattern REGEX_EMAIL = Pattern.compile("[\\p{L}0-9._%+-]+@[\\p{L}0-9.-]+\\.[\\p{L}]{2,6}",
			Pattern.UNICODE_CHARACTER_CLASS);

	/** REGEX_PHONE (needs probably a better pattern...). */
	public static final Pattern REGEX_PHONE = Pattern.compile(
			"\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$");

	/** REGEX_URL. */
	public static final Pattern REGEX_URL = Pattern.compile(
			"(https?|ftp|file)://[-\\p{L}0-9+&@#/%?=~_|!:,.;]*[-\\p{L}0-9+&@#/%=~_|]", Pattern.UNICODE_CHARACTER_CLASS);

	/** REGEX_IP. */
	public static final Pattern REGEX_IP = Pattern
			.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

	/** Dictionary Resource. */
	public final static String RESOURCE_DICTIONARIES = "dictTermExtractor";
	@ExternalResource(key = RESOURCE_DICTIONARIES)
	private DictionaryResource dictTermExtractor;

	/** PARAM_EXTRACT_EMAIL. */
	public static final String PARAM_EXTRACT_EMAIL = "extractEmail";
	@ConfigurationParameter(name = PARAM_EXTRACT_EMAIL, mandatory = false, defaultValue = "true")
	private boolean extractEmail;

	/** PARAM_EXTRACT_URL. */
	public static final String PARAM_EXTRACT_URL = "extractUrl";
	@ConfigurationParameter(name = PARAM_EXTRACT_URL, mandatory = false, defaultValue = "false")
	private boolean extractUrl;

	/** PARAM_EXTRACT_IP. */
	public static final String PARAM_EXTRACT_IP = "extractIp";
	@ConfigurationParameter(name = PARAM_EXTRACT_IP, mandatory = false, defaultValue = "false")
	private boolean extractIp;

	/** PARAM_EXTRACT_PHONE. */
	public static final String PARAM_EXTRACT_PHONE = "extractPhone";
	@ConfigurationParameter(name = PARAM_EXTRACT_PHONE, mandatory = false, defaultValue = "false")
	private boolean extractPhone;

	private Logger log;

	/** The unigram dictionaries. */
	private HashMap<String, Dictionary> unigramDictionaries;

	/** The mwu dictionaries. */
	private HashMap<String, Dictionary> mwuDictionaries;

	/*
	 * Uima initializer fetching dictionary entries.
	 * 
	 * @see org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.
	 * apache.uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		unigramDictionaries = dictTermExtractor.getUnigramDictionaries();
		mwuDictionaries = dictTermExtractor.getMwuDictionaries();
	}

	/*
	 * Uima process method extracting REGEX patterns for URLs, IPs, Email and
	 * Phone numbers as well as dictionary patterns.
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.
	 * apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		ArrayList<DictTerm> termsToTokenList = new ArrayList<DictTerm>();

		// EMAIL
		if (extractEmail)
			termsToTokenList.addAll(annotateRegex(jcas, REGEX_EMAIL, "EMAIL"));

		// URL
		if (extractUrl)
			termsToTokenList.addAll(annotateRegex(jcas, REGEX_URL, "URL"));

		// IP
		if (extractIp)
			termsToTokenList.addAll(annotateRegex(jcas, REGEX_IP, "IP"));

		// PHONE
		if (extractPhone)
			termsToTokenList.addAll(annotateRegex(jcas, REGEX_PHONE, "PHONE"));

		// Set new token and sentence boundaries for pattern matches
		correctTokenBoundaries(jcas, termsToTokenList);

		// Dictionary multi word units
		annotateMultiWordUnits(jcas);

		// Dictionary unigrams
		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
		for (Token t : tokens) {
			annotateDictTypes(jcas, t);
		}

	}

	/**
	 * Resets token boundaries for retrieved REGEX matches which span over
	 * multiple tokens.
	 *
	 * @param jcas
	 *            the jcas
	 * @param termsToTokenList
	 *            the terms to token list
	 */
	private void correctTokenBoundaries(JCas jcas, ArrayList<DictTerm> termsToTokenList) {
		for (DictTerm dictTerm : termsToTokenList) {
			// tokens
			Collection<Token> coveredTokens = JCasUtil.selectCovered(jcas, Token.class, dictTerm);
			if (coveredTokens.size() > 1) {
				Token newToken = new Token(jcas);
				boolean firstTok = true;
				for (Token t : coveredTokens) {
					if (firstTok) {
						newToken.setBegin(t.getBegin());
						newToken.setPos(t.getPos());
						firstTok = false;
					}
					newToken.setEnd(t.getEnd());
					t.removeFromIndexes();
				}
				newToken.addToIndexes();
			}
		}
	}

	/**
	 * Annotate dict types.
	 *
	 * @param jcas
	 *            the jcas
	 * @param token
	 *            the token to annotate
	 */
	public void annotateDictTypes(JCas jcas, Token token) {

		String tokenStem = dictTermExtractor.stem(token.getCoveredText()).toLowerCase();
		String tokenValue = token.getCoveredText().toLowerCase();

		boolean dictTermFound = false;
		StringList typeList = new StringList(jcas);
		StringList baseFormList = new StringList(jcas);

		for (String dictType : unigramDictionaries.keySet()) {
			HashMap<String, String> dict = unigramDictionaries.get(dictType);
			if (dict.containsKey(tokenStem)) {
				String baseForm = dict.get(tokenStem);
				if (tokenValue.startsWith(baseForm)) {
					typeList = typeList.push(dictType);
					baseFormList = baseFormList.push(baseForm);
					dictTermFound = true;
				}
			}
		}

		// add to cas index
		if (dictTermFound) {
			DictTerm dictTerm = new DictTerm(jcas);
			dictTerm.setBegin(token.getBegin());
			dictTerm.setEnd(token.getEnd());
			dictTerm.setDictType(typeList);
			dictTerm.setDictTerm(baseFormList);
			dictTerm.addToIndexes();
		}

	}

	/**
	 * Annotate multi word units (with regex pattern).
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void annotateMultiWordUnits(JCas jcas) {
		for (String dictType : mwuDictionaries.keySet()) {
			HashMap<String, String> dict = mwuDictionaries.get(dictType);
			for (String regexPattern : dict.keySet()) {
				annotateRegex(jcas, Pattern.compile(regexPattern), dictType);
			}
		}
	}

	/**
	 * Annotate regex patterns (URLs, IPs, email addresses and Phone numbers)
	 *
	 * @param jcas
	 *            the jcas
	 * @param pattern
	 *            the pattern
	 * @param type
	 *            the type
	 * @return the array list
	 */
	public ArrayList<DictTerm> annotateRegex(JCas jcas, Pattern pattern, String type) {
		String docText = jcas.getDocumentText();
		ArrayList<DictTerm> regexMatches = new ArrayList<DictTerm>();
		Matcher matcher = pattern.matcher(docText);
		// Check all occurrences
		while (matcher.find()) {
			DictTerm dictTerm = new DictTerm(jcas);
			dictTerm.setBegin(matcher.start());
			dictTerm.setEnd(matcher.end());
			StringList typeList = new StringList(jcas);
			StringList baseFormList = new StringList(jcas);
			typeList = typeList.push(type);
			baseFormList = baseFormList.push(matcher.group());
			dictTerm.setDictType(typeList);
			dictTerm.setDictTerm(baseFormList);
			dictTerm.addToIndexes();
			regexMatches.add(dictTerm);
		}
		return regexMatches;
	}

	/**
	 * Placeholder for no stemming (if no stemmer is available for the current
	 * document language)
	 */
	private class noStemmer extends org.tartarus.snowball.SnowballStemmer {

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.tartarus.snowball.SnowballStemmer#stem()
		 */
		@Override
		public boolean stem() {
			return true;
		}

	}
}
