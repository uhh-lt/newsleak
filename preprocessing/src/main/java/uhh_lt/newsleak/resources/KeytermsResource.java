package uhh_lt.newsleak.resources;

import java.io.IOException;
import java.util.Map;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;

import uhh_lt.keyterms.Extractor;

public class KeytermsResource extends Resource_ImplBase {

	private Logger logger;
	
	public static final String PARAM_LANGUAGE_CODE = "languageCode";
	@ConfigurationParameter(name = PARAM_LANGUAGE_CODE)
	private String languageCode;
	
	public static final String PARAM_N_KEYTERMS = "nKeyterms";
	@ConfigurationParameter(name = PARAM_N_KEYTERMS)
	private Integer nKeyterms;

	private Extractor extractor;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();
		
		try {
			extractor = new Extractor(languageCode, nKeyterms);
		} catch (IOException e) {
			throw new ResourceInitializationException(e.getMessage(), null);
		}
		
		return true;
	}

	public Extractor getExtractor() {
		return extractor;
	}

}
