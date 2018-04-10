package uhh_lt.newsleak.resources;

import java.io.File;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.danishStemmer;
import org.tartarus.snowball.ext.dutchStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.finnishStemmer;
import org.tartarus.snowball.ext.frenchStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.hungarianStemmer;
import org.tartarus.snowball.ext.italianStemmer;
import org.tartarus.snowball.ext.norwegianStemmer;
import org.tartarus.snowball.ext.portugueseStemmer;
import org.tartarus.snowball.ext.romanianStemmer;
import org.tartarus.snowball.ext.russianStemmer;
import org.tartarus.snowball.ext.spanishStemmer;
import org.tartarus.snowball.ext.swedishStemmer;
import org.tartarus.snowball.ext.turkishStemmer;

import opennlp.uima.Token;
import uhh_lt.newsleak.annotator.LanguageDetector;

public class DictionaryResource extends Resource_ImplBase {

	private Logger logger;

	public static final String PARAM_DATADIR = "dataDir";
	@ConfigurationParameter(name=PARAM_DATADIR, mandatory=true)
	private String dataDir;

	public static final String PARAM_DICTIONARY_FILES = "dictionaryFilesString";
	@ConfigurationParameter(name = PARAM_DICTIONARY_FILES)
	private String dictionaryFilesString;

	private List<File> dictionaryFiles;

	public static final String PARAM_LANGUAGE_CODE = "languageCode";
	@ConfigurationParameter(name = PARAM_LANGUAGE_CODE)
	private String languageCode;

	private SnowballStemmer stemmer;
	private Locale locale;
	
	private HashMap<String, Dictionary> unigramDictionaries;
	private HashMap<String, Dictionary> mwuDictionaries;


	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}

		this.logger = this.getLogger();
		locale = LanguageDetector.localeToISO().get(languageCode);

		dictionaryFiles = getDictionaryFiles(dictionaryFilesString);

		// stemmer
		switch (languageCode) {
		case "eng":
			stemmer = new englishStemmer();
			break;
		case "dan":
			stemmer = new danishStemmer();
			break;
		case "deu":
			stemmer = new germanStemmer();
			break;
		case "nld":
			stemmer = new dutchStemmer();
			break;
		case "fin":
			stemmer = new finnishStemmer();
			break;
		case "fra":
			stemmer = new frenchStemmer();
			break;
		case "hun":
			stemmer = new hungarianStemmer();
			break;
		case "ita":
			stemmer = new italianStemmer();
			break;
		case "nor":
			stemmer = new norwegianStemmer();
			break;
		case "por":
			stemmer = new portugueseStemmer();
			break;
		case "ron":
			stemmer = new romanianStemmer();
			break;
		case "rus":
			stemmer = new russianStemmer();
			break;
		case "spa":
			stemmer = new spanishStemmer();
			break;
		case "swe":
			stemmer = new swedishStemmer();
			break;
		case "tur":
			stemmer = new turkishStemmer();
			break;
		default:
			stemmer = new noStemmer();
		}

		unigramDictionaries = new HashMap<String, Dictionary>();
		mwuDictionaries = new HashMap<String, Dictionary>();

		for (File f : dictionaryFiles) {

			try {
				String dictType = f.getName().replaceAll("\\..*", "").toUpperCase();
				List<String> dictTermList = FileUtils.readLines(f);
				Dictionary dictUnigrams = new Dictionary();
				Dictionary dictMwu = new Dictionary();
				for (String term : dictTermList) {
					String t = term.trim();
					if (!t.isEmpty()) {
						
						if (isMultiWord(t)) {
							// handle dictionary entry as multiword unit
							dictMwu.put("(?i)" + Pattern.quote(t), t);
						} else {
							// handle dictionary entry as unigram
							String stem;
							synchronized (stemmer) {
								stemmer.setCurrent(t);
								stemmer.stem();
								stem = stemmer.getCurrent().toLowerCase();
							}
							
							String shortestType;
							if (dictUnigrams.containsKey(stem) && dictUnigrams.get(stem).length() < t.length()) {
								shortestType = dictUnigrams.get(stem);
							} else {
								shortestType = t;
							}
							dictUnigrams.put(stem, shortestType);
						}
						
						
					}
				}
				unigramDictionaries.put(dictType, dictUnigrams);
				mwuDictionaries.put(dictType, dictMwu);

			} catch (IOException e) {
				throw new ResourceInitializationException(e.getMessage(), null);
			}

		}

		return true;
	}



	private boolean isMultiWord(String t) {
		BreakIterator tokenBreaker = BreakIterator.getWordInstance(locale);
		tokenBreaker.setText(t);

		// count tokens
		int pos = tokenBreaker.first();
		int nTokens = 0;
		while (pos != BreakIterator.DONE) {
			nTokens++;
			pos = tokenBreaker.next();
		}
		nTokens = nTokens / 2;
		return nTokens > 1;
	}



	private List<File> getDictionaryFiles(String list) {
		List<File> files = new ArrayList<File>();
		for (String f : list.split(", +?")) {
			String[] args = f.split(":");
			if (args.length > 2) {
				logger.log(Level.SEVERE, "Could not parse dictionary files configuration: '" + list + "'\n" 
						+ "Expecting format 'dictionaryfiles = langcode:filename1, langcode:filename2, ...'.\n"
						+ "You can also omit 'langcode:' to apply dictionary to all languages."
						);
				System.exit(1);
			}
			if (args.length == 1 || (args.length == 2 && args[0].equals(languageCode))) {
				String fname = args.length == 1 ? args[0] : args[1];
				files.add(new File(dataDir, fname));
				logger.log(Level.INFO, "Applying dictionary file " + f + " to language " + languageCode);
			}
		}
		return files;
	}


	public HashMap<String, Dictionary> getUnigramDictionaries() {
		return unigramDictionaries;
	}


	private class noStemmer extends org.tartarus.snowball.SnowballStemmer {

		@Override
		public boolean stem() {
			return true;
		}

	}

	public synchronized String stem(String token) {
		stemmer.setCurrent(token);
		stemmer.stem();
		return stemmer.getCurrent();
	}
	
	
	
	public class Dictionary extends HashMap<String, String> {

		/**
		 * Serial ID
		 */
		private static final long serialVersionUID = -4395683941205467020L;
		
		public Dictionary() {
			super();
		}
		
	}

	public HashMap<String, Dictionary> getMwuDictionaries() {
		return mwuDictionaries;
	}
	
}
