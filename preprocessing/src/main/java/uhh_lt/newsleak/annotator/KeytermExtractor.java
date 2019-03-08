package uhh_lt.newsleak.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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

import opennlp.uima.Location;
import opennlp.uima.Organization;
import opennlp.uima.Person;
import opennlp.uima.Token;
import uhh_lt.keyterms.Extractor;
import uhh_lt.newsleak.types.Metadata;

// TODO: Auto-generated Javadoc
/**
 * UIMA annotator for key term extraction. Uses the uhh-lt/lt-keyterms maven
 * package providing statistical keyness measurement based on log likelihood
 * comparison with a reference corpus.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class KeytermExtractor extends JCasAnnotator_ImplBase {

	/** PARAM_LANGUAGE_CODE to initialize the keyterm extractor */
	public static final String PARAM_LANGUAGE_CODE = "languageCode";
	@ConfigurationParameter(name = PARAM_LANGUAGE_CODE)
	private String languageCode;

	/** PARAM_N_KEYTERMS Number of keyterms to extract per document. */
	public static final String PARAM_N_KEYTERMS = "nKeyterms";
	@ConfigurationParameter(name = PARAM_N_KEYTERMS)
	private Integer nKeyterms;

	private Extractor extractor;

	private Logger log;

	/*
	 * UIMA initializer
	 * 
	 * @see org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.
	 * apache.uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		try {
			extractor = new Extractor(languageCode, 100);
		} catch (IOException e) {
			throw new ResourceInitializationException(e.getMessage(), null);
		}
	}

	/*
	 * Extracts keyterms and named entities. Removes named entities from keyterm
	 * list to not produce overlap
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.
	 * apache.uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);

		Set<String> keytermSet = getKeyWords(tokens);
		HashSet<String> namedEntities = getNamedEntities(jcas);

		// generate decreasing count value for each keyterm
		int pseudoCount = keytermSet.size();
		int n = 0;
		StringBuilder keyterms = new StringBuilder();
		String text;
		for (String term : keytermSet) {
			pseudoCount--;
			// do not extract NEs as keyterms too
			if (namedEntities.contains(term))
				continue;
			text = keyterms.length() > 0 ? "\t" : "";
			text += term.replaceAll(":", "");
			keyterms.append(text).append(":").append(pseudoCount);
			n++;
			// extract only top n keyterms
			if (n >= nKeyterms)
				break;
		}

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		metadata.setKeyterms(keyterms.toString());
		metadata.addToIndexes();
	}

	/**
	 * Gets the key words.
	 *
	 * @param document
	 *            the document
	 * @return the key words
	 */
	public Set<String> getKeyWords(Collection<Token> document) {
		List<String> tokens = new ArrayList<String>();
		for (Token token : document) {
			tokens.add(token.getCoveredText());
		}
		return extractor.extractKeyTerms(tokens);
	}

	/**
	 * Gets the named entities.
	 *
	 * @param jcas
	 *            the jcas
	 * @return the named entities
	 */
	public HashSet<String> getNamedEntities(JCas jcas) {
		Collection<Person> persons = JCasUtil.select(jcas, Person.class);
		Collection<Organization> organizations = JCasUtil.select(jcas, Organization.class);
		Collection<Location> locations = JCasUtil.select(jcas, Location.class);
		HashSet<String> nes = new HashSet<String>();
		for (Person ne : persons) {
			nes.add(ne.getCoveredText());
		}
		for (Location ne : locations) {
			nes.add(ne.getCoveredText());
		}
		for (Organization ne : organizations) {
			nes.add(ne.getCoveredText());
		}
		return nes;
	}

}
