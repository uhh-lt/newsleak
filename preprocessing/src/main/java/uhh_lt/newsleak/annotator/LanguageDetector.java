package uhh_lt.newsleak.annotator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;

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
import uhh_lt.newsleak.resources.DocumentLanguagesResource;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.types.Metadata;

public class LanguageDetector extends JCasAnnotator_ImplBase {

	private LanguageDetectorME languageDetector;

	public final static String MODEL_FILE = "languageDetectorResource";
	@ExternalResource(key = MODEL_FILE)
	private LanguageDetectorResource languageDetectorResource;

	//	public final static String DOCLANG_FILE = "DocumentLanguagesFile";
	//	@ExternalResource(key = DOCLANG_FILE)
	//	private DocumentLanguagesResource documentLanguagesResource;
	public final static String DOCLANG_FILE = "documentLanguagesFile";
	@ConfigurationParameter(
			name = DOCLANG_FILE, 
			mandatory = true,
			description = "temporary file to keep document language information")
	private String documentLanguagesFile;

	public HashSet<String> supportedLanguages;
	public HashMap<String, String> documentLanguages;

	Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		supportedLanguages = new HashSet<String>();
		File[] directories = new File("resources").listFiles(File::isDirectory);
		for (File dir : directories) {
			supportedLanguages.add(dir.getName());
		}

		languageDetector = new LanguageDetectorME(languageDetectorResource.getModel());
		documentLanguages = new HashMap<String, String>();

		log = context.getLogger();
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

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		documentLanguages.put(metadata.getDocId(), docLang);

	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		File tmpFile = new File(documentLanguagesFile);
		HashMap<String, String> mergedDocumentLanguages;
		try {
			if (tmpFile.exists()) {
				FileInputStream fis = new FileInputStream(tmpFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				mergedDocumentLanguages = (HashMap<String, String>) ois.readObject();
				ois.close();
			} else {
				mergedDocumentLanguages = new HashMap<String, String>();
			}
			mergedDocumentLanguages.putAll(documentLanguages);
			FileOutputStream fos = new FileOutputStream(tmpFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mergedDocumentLanguages);
			oos.close();
			log.log(Level.INFO, "Written language metadata of " + mergedDocumentLanguages.size() + " to " + documentLanguagesFile);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}



}
