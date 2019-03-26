package uhh_lt.newsleak.annotator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.tools.langdetect.LanguageDetectorME;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.MetadataResource;
import uhh_lt.newsleak.types.Metadata;

/**
 * Detects the language of a document based on the first 3000 characters of its
 * content. The annotator wraps the Apache OpenNLP maximum entropy language
 * identifier model.
 * 
 * Inferred language is written as metadata to a temporary metadata collection.
 * ISO codes for supported languages (one per line) should reside in
 * <i>resources/supportedLanguages.txt</i>
 */
@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class LanguageDetector extends JCasAnnotator_ImplBase {

	/** The OpenNLP model. */
	private LanguageDetectorME languageDetector;

	/** The OpenNLP model file */
	public final static String MODEL_FILE = "languageDetectorResource";

	/** The language detector resource. */
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetectorResource;

	/** Location of the temporary METADATA_FILE collection. */
	public final static String METADATA_FILE = "metadataResource";

	/** The metadata resource. */
	@ExternalResource(key = METADATA_FILE)
	private MetadataResource metadataResource;

	/** The Constant PARAM_DEFAULT_LANG. */
	public static final String PARAM_DEFAULT_LANG = "defaultLanguage";

	/** The default language (English). */
	@ConfigurationParameter(name = PARAM_DEFAULT_LANG, mandatory = false, defaultValue = "eng")
	private String defaultLanguage;

	/** The Constant DOCLANG_FILE. */
	public final static String DOCLANG_FILE = "documentLanguagesFile";

	/** Temporary file to keep document language information. */
	@ConfigurationParameter(name = DOCLANG_FILE, mandatory = true, description = "temporary file to keep document language information")
	private String documentLanguagesFile;

	/** The supported languages. */
	public HashSet<String> supportedLanguages;

	/** The logger. */
	Logger logger;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.
	 * apache.uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		supportedLanguages = getSupportedLanguages();
		languageDetector = new LanguageDetectorME(languageDetectorResource.getModel());
		logger = context.getLogger();
	}

	/**
	 * Gets the supported languages.
	 *
	 * @return the supported languages
	 */
	public static HashSet<String> getSupportedLanguages() {
		HashSet<String> supportedLanguages = new HashSet<String>();
		try (Stream<String> stream = Files.lines(Paths.get("resources/supportedLanguages.txt"))) {
			stream.forEach(supportedLanguages::add);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return supportedLanguages;
	}

	/*
	 * Infers doc language from the first 3000 characters, counts a statistic
	 * and sets CAS metadata.
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.
	 * apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();
		Integer maxLength = Math.min(docText.length(), 3000);
		String docBeginning = docText.substring(0, maxLength);
		String docLang = languageDetector.predictLanguage(docBeginning).getLang();

		// count languages for statistics
		languageDetectorResource.addLanguage(docLang);

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();

		// Only set language metadata, if it is supported
		if (supportedLanguages.contains(docLang)) {
			jcas.setDocumentLanguage(docLang);

			// append language information to metadata file
			ArrayList<List<String>> langmetadata = new ArrayList<List<String>>();
			langmetadata.add(metadataResource.createTextMetadata(metadata.getDocId(), "language", docLang));
			metadataResource.appendMetadata(langmetadata);
		}

	}

	/**
	 * Locale to ISO.
	 *
	 * @return map containing ISO : locale pairs
	 */
	public static Map<String, Locale> localeToISO() {
		String[] languages = Locale.getISOLanguages();
		Map<String, Locale> localeMap = new HashMap<String, Locale>(languages.length);
		for (String language : languages) {
			Locale locale = new Locale(language);
			localeMap.put(locale.getISO3Language(), locale);
		}
		return localeMap;
	}

	/*
	 * Logs language counts after process has completed.
	 * 
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
	 * collectionProcessComplete()
	 */
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		languageDetectorResource.logLanguageStatistics(logger);
		super.collectionProcessComplete();
	}

}
