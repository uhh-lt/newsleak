package annotator;

import java.io.File;
import java.util.HashSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import opennlp.tools.langdetect.LanguageDetectorME;
import resources.LanguageDetectorResource;

public class LanguageDetector extends JCasAnnotator_ImplBase {
	
	private LanguageDetectorME languageDetector;
	
	public final static String MODEL_FILE = "LangDetectorModel";
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetectorResource;
	
	public HashSet<String> supportedLanguages;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		supportedLanguages = new HashSet<String>();
		File[] directories = new File("resources").listFiles(File::isDirectory);
		for (File dir : directories) {
			supportedLanguages.add(dir.getName());
		}
		
		languageDetector = new LanguageDetectorME(languageDetectorResource.getModel());
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		String docText = jcas.getDocumentText();
		Integer maxLength = Math.min(docText.length(), 2000);
		String docBeginning = docText.substring(0, maxLength);
		String docLang = languageDetector.predictLanguage(docBeginning).getLang();
		
		// Only set language, if we support it
		if (supportedLanguages.contains(docLang)) {
			jcas.setDocumentLanguage(docLang);
		}
		
	}

}
