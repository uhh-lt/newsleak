package uhh_lt.newsleak.annotator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Logger;

import opennlp.uima.Token;
import uhh_lt.newsleak.resources.DictionaryResource;
import uhh_lt.newsleak.types.DictTerm;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class DictionaryExtractor extends JCasAnnotator_ImplBase {

	public final static String RESOURCE_DICTIONARIES = "dictTermExtractor";
	@ExternalResource(key = RESOURCE_DICTIONARIES)
	private DictionaryResource dictTermExtractor;
	
	private Logger log;
	private HashMap<String, HashSet<String>> dictionaries;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		dictionaries = dictTermExtractor.getDictionaries();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
		for (Token t : tokens) {
			annotateDictTypes(jcas, t);
		}
		
	}
	
	
	public void annotateDictTypes(JCas jcas, Token token) {
		
		String tokenValue = dictTermExtractor.stem(token.getCoveredText()).toLowerCase();
		
		boolean dictTermFound = false; 
		StringList typeList = new StringList(jcas);
		
		for (String dictType : dictionaries.keySet()) {
			HashSet<String> dict = dictionaries.get(dictType);
			if (dict.contains(tokenValue)) {
				typeList = typeList.push(dictType);
				dictTermFound = true;
			}
		}
		
		if (dictTermFound) {
			DictTerm dictTerm = new DictTerm(jcas);
			dictTerm.setBegin(token.getBegin());
			dictTerm.setEnd(token.getEnd());
			dictTerm.setDictType(typeList);
			dictTerm.addToIndexes();
		}
		
	}

}
