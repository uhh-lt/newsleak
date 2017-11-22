package resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import opennlp.tools.langdetect.LanguageDetector;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;

public class LanguageDetectorResource implements SharedResourceObject {
	
	private LanguageDetectorModel model;
	private LanguageDetector ld;

	public void load(DataResource modelfile) throws ResourceInitializationException {
		model = new LanguageDetectorModelLoader().load(new File(modelfile.getUri()));
		ld = new LanguageDetectorME(model);
	}
	
	public String detectLanguage(String text) {
		return ld.predictLanguage(text).getLang();
	}

}
