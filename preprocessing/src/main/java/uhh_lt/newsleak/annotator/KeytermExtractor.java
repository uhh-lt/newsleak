package uhh_lt.newsleak.annotator;

import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.uima.Token;
import uhh_lt.newsleak.resources.KeytermsResource;
import uhh_lt.newsleak.types.Metadata;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class KeytermExtractor extends JCasAnnotator_ImplBase {

	public final static String RESOURCE_KEYTERMS = "keyTermExtractor";
	@ExternalResource(key = RESOURCE_KEYTERMS)
	private KeytermsResource keyTermExtractor;
	
	public static final String PARAM_NOUN_TAG = "nounPosTag";
	@ConfigurationParameter(name = PARAM_NOUN_TAG)
	private String nounPosTag;

	Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, 0, jcas.getDocumentText().length());
		
		Set<String> keytermSet = keyTermExtractor.getKeyWords(tokens);
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

}
