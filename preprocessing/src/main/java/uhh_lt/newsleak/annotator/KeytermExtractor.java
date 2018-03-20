package uhh_lt.newsleak.annotator;

import java.util.Collection;
import java.util.HashMap;
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
import uhh_lt.newsleak.types.Metadata;
import uhh_lt.newsleak.util.MapUtil;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class KeytermExtractor extends JCasAnnotator_ImplBase {

//	public final static String FRQ_FILE = "keyTermResource";
//	@ExternalResource(key = FRQ_FILE)
//	private KeyTermResource keyTermResource;
	
	public static final String PARAM_NOUN_TAG = "nounPosTag";
	@ConfigurationParameter(name = PARAM_NOUN_TAG)
	private String nounPosTag;

	public HashMap<String, Integer> tokenCounts;

	Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		tokenCounts = new HashMap<String, Integer>();
		log = context.getLogger();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, 0, jcas.getDocumentText().length());
		
		for (Token token : tokens) {
			if (token.getPos().matches(nounPosTag)) {
				String text = token.getCoveredText().toLowerCase();
				tokenCounts.put(text, tokenCounts.containsKey(text) ? tokenCounts.get(text) + 1 : 1);
			}
		}
		
		tokenCounts = (HashMap<String, Integer>) MapUtil.sortByValueDecreasing(tokenCounts);
		
		StringBuilder keyterms = new StringBuilder();
		int nTerms = 0;
		String text;
		for (String term : tokenCounts.keySet()) {
			int count = tokenCounts.get(term);
			if (term.replaceAll("\\W", "").length() < 2) continue; 
			text = nTerms > 0 ? "\t" : "";
			text += term.replaceAll(":", "");
			keyterms.append(text).append(":").append(count);
			nTerms++;
			// maximum of 10 terms
			if (nTerms >= 10) break;
		}

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		metadata.setKeyterms(keyterms.toString());
		metadata.addToIndexes();
	}

}
