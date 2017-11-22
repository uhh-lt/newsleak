package annotator;

import java.io.File;
import java.util.HashSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import resources.LanguageDetectorResource;

public class LanguageDetector extends JCasAnnotator_ImplBase {
	
	public final static String MODEL_FILE = "LangDetectorModel";
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetector;
	
	public HashSet<String> supportedLanguages;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		// TODO Auto-generated method stub
		super.initialize(context);
		supportedLanguages = new HashSet<String>();
		File[] directories = new File("resources").listFiles(File::isDirectory);
		for (File dir : directories) {
			supportedLanguages.add(dir.getName());
		}
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		String docText = jcas.getDocumentText();
		Integer maxLength = Math.min(docText.length(), 2000);
		String docLang = languageDetector.detectLanguage(docText.substring(0, maxLength));
		
		// Only set language, if we support it
		if (supportedLanguages.contains(docLang)) {
			jcas.setDocumentLanguage(docLang);
		}
		
	}

}
