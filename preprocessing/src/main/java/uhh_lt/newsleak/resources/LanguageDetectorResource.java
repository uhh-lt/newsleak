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

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.annotator.LanguageDetector, among other loading of an openNLP
 * language detection model and tracking a simple statistic of language counts.
 */
public class LanguageDetectorResource extends Resource_ImplBase {

	/** The Constant PARAM_MODEL_FILE. */
	public static final String PARAM_MODEL_FILE = "mModelfile";

	/** The m modelfile. */
	@ConfigurationParameter(name = PARAM_MODEL_FILE)
	private String mModelfile;

	/** The language counter. */
	private HashMap<String, Integer> languageCounter;

	/** The model. */
	private LanguageDetectorModel model;

	/** The statistics logged. */
	private boolean statisticsLogged = false;

	/**
	 * Gets the openNLP language detection model.
	 *
	 * @return the model
	 */
	public LanguageDetectorModel getModel() {
		return model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.Resource_ImplBase#initialize(org.apache.uima.
	 * resource.ResourceSpecifier, java.util.Map)
	 */
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

	/**
	 * Increments the counter for a specific language.
	 *
	 * @param language
	 *            the language
	 */
	public synchronized void addLanguage(String language) {
		languageCounter.put(language, languageCounter.containsKey(language) ? languageCounter.get(language) + 1 : 1);
	}

	/**
	 * Log language statistics.
	 *
	 * @param logger
	 *            the logger
	 */
	public synchronized void logLanguageStatistics(Logger logger) {
		if (!statisticsLogged) {
			StringBuilder sb = new StringBuilder();
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
