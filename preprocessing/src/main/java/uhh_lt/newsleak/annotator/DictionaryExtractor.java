package uhh_lt.newsleak.annotator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.resources.DictionaryResource;
import uhh_lt.newsleak.resources.DictionaryResource.Dictionary;
import uhh_lt.newsleak.types.DictTerm;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class DictionaryExtractor extends JCasAnnotator_ImplBase {

	public static final Pattern REGEX_EMAIL = Pattern.compile("[\\p{L}0-9._%+-]+@[\\p{L}0-9.-]+\\.[\\p{L}]{2,6}", Pattern.UNICODE_CHARACTER_CLASS);
	public static final Pattern REGEX_PHONE = Pattern.compile("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$");
	public static final Pattern REGEX_URL = Pattern.compile("(https?|ftp|file)://[-\\p{L}0-9+&@#/%?=~_|!:,.;]*[-\\p{L}0-9+&@#/%=~_|]", Pattern.UNICODE_CHARACTER_CLASS);
	public static final Pattern REGEX_IP = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

	public final static String RESOURCE_DICTIONARIES = "dictTermExtractor";
	@ExternalResource(key = RESOURCE_DICTIONARIES)
	private DictionaryResource dictTermExtractor;

	private Logger log;
	private HashMap<String, Dictionary> unigramDictionaries;
	private HashMap<String, Dictionary> mwuDictionaries;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		unigramDictionaries = dictTermExtractor.getUnigramDictionaries();
		mwuDictionaries = dictTermExtractor.getMwuDictionaries();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		ArrayList<DictTerm> termsToTokenList = new ArrayList<DictTerm>();
		
		// IP
		termsToTokenList.addAll(annotateRegex(jcas, REGEX_IP, "IP"));
		
		// EMAIL
		termsToTokenList.addAll(annotateRegex(jcas, REGEX_EMAIL, "EMAIL"));
		
		// URL
		termsToTokenList.addAll(annotateRegex(jcas, REGEX_URL, "URL"));
		
		// PHONE
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

		if (dictTermFound) {
			DictTerm dictTerm = new DictTerm(jcas);
			dictTerm.setBegin(token.getBegin());
			dictTerm.setEnd(token.getEnd());
			dictTerm.setDictType(typeList);
			dictTerm.setDictTerm(baseFormList);
			dictTerm.addToIndexes();
		}

	}
	
	

	private void annotateMultiWordUnits(JCas jcas) {
		for (String dictType : mwuDictionaries.keySet()) {
			HashMap<String, String> dict = mwuDictionaries.get(dictType);
			for (String regexPattern : dict.keySet()) {
				annotateRegex(jcas, Pattern.compile(regexPattern), dictType);
			}
		}
	}


	public ArrayList<DictTerm> annotateRegex(JCas jcas, Pattern pattern, String type) {
		String docText = jcas.getDocumentText();
		ArrayList<DictTerm> regexMatches =  new ArrayList<DictTerm>();
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

	private class noStemmer extends org.tartarus.snowball.SnowballStemmer {

		@Override
		public boolean stem() {
			return true;
		}

	}
}
