package uhh_lt.newsleak.resources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import opennlp.tools.langdetect.LanguageDetectorModel;

public class LanguageDetectorResource extends Resource_ImplBase {
	
	public static final String PARAM_MODEL_FILE = "mModelfile";
	@ConfigurationParameter(name = PARAM_MODEL_FILE)
	private String mModelfile;

	private HashMap<String, Integer> languageCounter;
	private LanguageDetectorModel model;
	private boolean statisticsLogged = false;
	

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
		languageCounter = new HashMap<String, Integer>();
		return true;
	}
	
	public synchronized void addLanguage(String language) {
		languageCounter.put(language, languageCounter.containsKey(language) ? languageCounter.get(language) + 1 : 1);
	}
	

	public synchronized void logLanguageStatistics(Logger logger) {
		if (!statisticsLogged ) {
			StringBuilder sb =  new StringBuilder();
			sb.append("Languages detected in current collection\n");
			sb.append("-------------------------------------\n");
			for (String language : languageCounter.keySet()) {
				sb.append(language + ": " + languageCounter.get(language) + "\n");
			}
			logger.log(Level.INFO, sb.toString());
			statisticsLogged = true;
		}
	}

}
