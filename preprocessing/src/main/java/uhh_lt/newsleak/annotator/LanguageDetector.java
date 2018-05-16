package uhh_lt.newsleak.annotator;

import java.io.File;
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
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.tools.langdetect.LanguageDetectorME;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.MetadataResource;
import uhh_lt.newsleak.types.Metadata;

public class LanguageDetector extends JCasAnnotator_ImplBase {

	private LanguageDetectorME languageDetector;

	public final static String MODEL_FILE = "languageDetectorResource";
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetectorResource;

	public final static String METADATA_FILE = "metadataResource";
	@ExternalResource(key = METADATA_FILE)
	private MetadataResource metadataResource;

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

	Logger logger;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		supportedLanguages = getSupportedLanguages();
		languageDetector = new LanguageDetectorME(languageDetectorResource.getModel());
		logger = context.getLogger();
	}


	public static HashSet<String> getSupportedLanguages() {
		HashSet<String> supportedLanguages = new HashSet<String>();
		//		File[] directories = new File("resources").listFiles(File::isDirectory);
		//		for (File dir : directories) {
		//			supportedLanguages.add(dir.getName());
		//		}
		try (Stream<String> stream = Files.lines(Paths.get("resources/supportedLanguages.txt"))) {
			stream.forEach(supportedLanguages::add);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return supportedLanguages;
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();
		Integer maxLength = Math.min(docText.length(), 2000);
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



	public static Map<String, Locale> localeToISO() {
		String[] languages = Locale.getISOLanguages();
		Map<String, Locale> localeMap = new HashMap<String, Locale>(languages.length);
		for (String language : languages) {
			Locale locale = new Locale(language);
			localeMap.put(locale.getISO3Language(), locale);
		}
		return localeMap;
	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		languageDetectorResource.logLanguageStatistics(logger);
		super.collectionProcessComplete();
	}
	
	
	


}
