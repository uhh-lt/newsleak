package uhh_lt.newsleak.resources;

import java.io.File;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import opennlp.tools.langdetect.LanguageDetectorModel;

public class LanguageDetectorResource implements SharedResourceObject {
	
	private LanguageDetectorModel model;

	public void load(DataResource modelfile) throws ResourceInitializationException {
		model = new LanguageDetectorModelLoader().load(new File(modelfile.getUri()));
	}
	
	public LanguageDetectorModel getModel() {
		return model;
	}

}
