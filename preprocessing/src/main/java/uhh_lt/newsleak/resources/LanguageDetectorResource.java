package uhh_lt.newsleak.resources;

import java.io.File;
import java.util.Map;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import opennlp.tools.langdetect.LanguageDetectorModel;

public class LanguageDetectorResource extends Resource_ImplBase {
	
	public static final String PARAM_MODEL_FILE = "mModelfile";
	@ConfigurationParameter(name = PARAM_MODEL_FILE)
	private String mModelfile;

	private LanguageDetectorModel model;
	

	public LanguageDetectorModel getModel() {
		return model;
	}
	

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		model = new LanguageDetectorModelLoader().load(new File(mModelfile));
		return true;
	}

}
