package uhh_lt.newsleak.annotator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.tools.langdetect.LanguageDetectorME;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.types.Metadata;

public class LanguageDetector extends JCasAnnotator_ImplBase {

	private LanguageDetectorME languageDetector;

	public final static String MODEL_FILE = "languageDetectorResource";
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetectorResource;
	
	public static final String PARAM_DEFAULT_LANG = "defaultLanguage";
	@ConfigurationParameter(name = PARAM_DEFAULT_LANG, mandatory = false, defaultValue = "eng")
	private String defaultLanguage;

	public final static String DOCLANG_FILE = "documentLanguagesFile";
	@ConfigurationParameter(
			name = DOCLANG_FILE, 
			mandatory = true,
			description = "temporary file to keep document language information")
	private String documentLanguagesFile;

	public HashSet<String> supportedLanguages;

	Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		supportedLanguages = getSupportedLanguages();

		languageDetector = new LanguageDetectorME(languageDetectorResource.getModel());

		log = context.getLogger();
	}


	public static HashSet<String> getSupportedLanguages() {
		HashSet<String> supportedLanguages = new HashSet<String>();
		File[] directories = new File("resources").listFiles(File::isDirectory);
		for (File dir : directories) {
			supportedLanguages.add(dir.getName());
		}
		return supportedLanguages;
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();
		Integer maxLength = Math.min(docText.length(), 2000);
		String docBeginning = docText.substring(0, maxLength);
		String docLang = languageDetector.predictLanguage(docBeginning).getLang();
		
		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();

		// Only set language, if we support it
		if (supportedLanguages.contains(docLang)) {
			jcas.setDocumentLanguage(docLang);
			
			// append language information to metadata file
			ArrayList<List<String>> langmetadata = new ArrayList<List<String>>();
			langmetadata.add(createTextMetadata(metadata.getDocId(), "language", docLang));
			languageDetectorResource.appendMetadata(langmetadata);
		} 

	}



	public static Map<String, Locale> localeToISO() {
		String[] languages = Locale.getISOLanguages();
		Map<String, Locale> localeMap = new HashMap<String, Locale>(languages.length);
		for (String language : languages) {
		    Locale locale = new Locale(language);
		    localeMap.put(locale.getISO3Language(), locale);
		}
		return localeMap;
	}
	
	private ArrayList<String> createTextMetadata(String docId, String key, String value) {
		ArrayList<String> meta = new ArrayList<String>();
		meta.add(docId);
		meta.add(StringUtils.capitalize(key));
		meta.add(value.replaceAll("\\r|\\n", " "));
		meta.add("Text");
		return meta;
	}
}
