package uhh_lt.newsleak.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.uima.Token;
import uhh_lt.keyterms.Extractor;
import uhh_lt.newsleak.types.Metadata;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class KeytermExtractor extends JCasAnnotator_ImplBase {

	public static final String PARAM_LANGUAGE_CODE = "languageCode";
	@ConfigurationParameter(name = PARAM_LANGUAGE_CODE)
	private String languageCode;
	
	public static final String PARAM_N_KEYTERMS = "nKeyterms";
	@ConfigurationParameter(name = PARAM_N_KEYTERMS)
	private Integer nKeyterms;

	private Extractor extractor;
	
	private Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		try {
			extractor = new Extractor(languageCode, nKeyterms);
		} catch (IOException e) {
			throw new ResourceInitializationException(e.getMessage(), null);
		}
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, 0, jcas.getDocumentText().length());
		
		Set<String> keytermSet = getKeyWords(tokens);
		
		// generate decreasing count value for each keyterm
		int pseudoCount = keytermSet.size();
		StringBuilder keyterms = new StringBuilder();
		String text;
		for (String term : keytermSet) {
			text = keyterms.length() > 0 ? "\t" : "";
			text += term.replaceAll(":", "");
			keyterms.append(text).append(":").append(pseudoCount);
			pseudoCount--;
		}

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		metadata.setKeyterms(keyterms.toString());
		metadata.addToIndexes();
	}
	
	public Set<String> getKeyWords(List<Token> document) {
		List<String> tokens = new ArrayList<String>();
		for (Token token : document) {
			tokens.add(token.getCoveredText());
		}
		return extractor.extractKeyTerms(tokens);
	}

}
