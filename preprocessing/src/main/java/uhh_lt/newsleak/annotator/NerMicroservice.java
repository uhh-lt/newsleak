package uhh_lt.newsleak.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import opennlp.uima.Location;
import opennlp.uima.Organization;
import opennlp.uima.Person;
import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.types.Metadata;

/**
 * Annotator for Named Entity Recognition. The annotator queries a micro-service
 * via JSON API. The default micro-service (available at
 * https://github.com/uhh-lt/newsleak-ner) wraps the polyglot NLP library
 * (https://github.com/aboSamoor/polyglot) in a Flask APP
 * (http://flask.pocoo.org/) and is deployed as a docker container
 * (https://www.docker.com/).
 * 
 * Currently, polyglot NER only supports O, I-PER, I-LOC and I-ORG labels. No
 * information about beginning of a new entity is present. Our heuristic assumes
 * that sequences of tokens with identical I-tags are one entity
 * 
 * Moreover, polyglot NER tends to produce many false positives for noisy text
 * data. Thus, we utilize a simple heuristic to remove detected entities which
 * contain less than 2 alphabetic characters.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = true)
public class NerMicroservice extends JCasAnnotator_ImplBase {

	/** The Constant NER_SERVICE_URL. */
	public final static String NER_SERVICE_URL = "nerMicroserviceUrl";

	/** The ner microservice url. */
	@ConfigurationParameter(name = NER_SERVICE_URL, mandatory = true, description = "Url to microservice for named entity recognition (JSON API)")
	private String nerMicroserviceUrl;

	/** The locale map. */
	private Map<String, Locale> localeMap;

	/** The log. */
	Logger log;

	/** The http client. */
	HttpClient httpClient;

	/** The request. */
	HttpPost request;

	private static final int MAXIMUM_SENTENCES_PER_REQUEST = 1000;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.apache.
	 * uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		httpClient = HttpClientBuilder.create().build();
		request = new HttpPost(nerMicroserviceUrl);
		localeMap = LanguageDetector.localeToISO();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.
	 * uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		// document language in ISO-639-1 format
		String docLang = localeMap.get(jcas.getDocumentLanguage()).getLanguage();
		
		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();

		if (metadata.getNoFulltextDocument()) {
			log.log(Level.INFO, "Skipping document for NER annotation, because it not assumed to be a fulltext document.");
		} else {
			try {

				// create json request for microservice
				
				Collection<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, 0,
						jcas.getDocumentText().length());
				
				// annotate in batches
				if (sentences.size() < MAXIMUM_SENTENCES_PER_REQUEST ) {
					annotateNer(jcas, docLang, sentences);
				} else {
					ArrayList<Sentence> sentenceList = new ArrayList<Sentence>(sentences);
					int batches = (int) Math.ceil(sentenceList.size() / MAXIMUM_SENTENCES_PER_REQUEST);
					for (int i = 0; i < batches; i++) {
						int start = i * MAXIMUM_SENTENCES_PER_REQUEST;
						int end = Math.min(sentenceList.size(), (i + 1) * MAXIMUM_SENTENCES_PER_REQUEST);
						annotateNer(jcas, docLang, sentenceList.subList(start, end));
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		

		// remove unlikely entities
		cleanNerAnnotations(jcas);

	}

	private void annotateNer(JCas jcas, String docLang, Collection<Sentence> sentences)
			throws IOException, ClientProtocolException, AnalysisEngineProcessException {
		XContentBuilder sb = XContentFactory.jsonBuilder().startObject();
		sb.field("lang", docLang);
		sb.field("sentences").startArray();
		for (Sentence sentence : sentences) {
			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(),
					sentence.getEnd());
			sb.startArray();
			for (Token token : tokens) {
				sb.value(token.getCoveredText());
			}
			sb.endArray();
		}
		sb.endArray();

		StringEntity entity = new StringEntity(sb.string(), ContentType.APPLICATION_JSON);
		request.setEntity(entity);
		HttpResponse response = httpClient.execute(request);

		// evaluate request response

		String responseText = EntityUtils.toString(response.getEntity());

		JsonObject obj = new JsonParser().parse(responseText).getAsJsonObject();

		JsonArray sentenceArray = null;
		try {
			sentenceArray = obj.getAsJsonArray("result");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Invalid NER result. Check if there is a model for language '" + docLang
					+ "' in the NER microservice?");
			log.log(Level.SEVERE, "Json request string: " + sb.string());

			// System.exit(1);
			throw new AnalysisEngineProcessException();
		}

		ArrayList<String> tagList = new ArrayList<String>();
		for (int i = 0; i < sentenceArray.size(); i++) {
			JsonArray tokenArray = sentenceArray.get(i).getAsJsonArray();
			for (int j = 0; j < tokenArray.size(); j++) {
				JsonArray annotationArray = tokenArray.get(j).getAsJsonArray();
				tagList.add(annotationArray.get(1).getAsString());
			}
		}

		int position = -1;

		// annotate NE types

		for (Sentence sentence : sentences) {

			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(),
					sentence.getEnd());
			Annotation annotation = null;
			String prevTag = "";

			for (Token token : tokens) {

				position++;
				String tag = tagList.get(position);

				if (tag.equals(prevTag)) {
					if (annotation != null) {
						annotation.setEnd(token.getEnd());
					}
				} else {
					if (annotation != null) {
						annotation.addToIndexes();
					}
					if (tag.equals("O")) {
						annotation = null;
					} else if (tag.equals("I-PER")) {
						annotation = new Person(jcas);
					} else if (tag.equals("I-LOC")) {
						annotation = new Location(jcas);
					} else if (tag.equals("I-ORG")) {
						annotation = new Organization(jcas);
					}
					if (annotation != null) {
						annotation.setBegin(token.getBegin());
						annotation.setEnd(token.getEnd());
					}
				}
				prevTag = tag;
			}
			if (annotation != null) {
				annotation.addToIndexes();
			}

		}
	}

	/**
	 * Remove NE annotation for unlikely PER, ORG and LOC entities.
	 *
	 * @param jcas
	 *            the jcas
	 */
	private void cleanNerAnnotations(JCas jcas) {

		// do not apply filter to chinese or japanese texts
		if (jcas.getDocumentLanguage().equals("zho") || jcas.getDocumentLanguage().equals("jpn"))
			return;

		Collection<Person> persons = JCasUtil.select(jcas, Person.class);
		cleanAnnotation(persons);
		Collection<Organization> organizations = JCasUtil.select(jcas, Organization.class);
		cleanAnnotation(organizations);
		Collection<Location> locations = JCasUtil.select(jcas, Location.class);
		cleanAnnotation(locations);
	}

	/**
	 * Remove unlikely annotation.
	 *
	 * @param annotations
	 *            removal candidates
	 */
	private void cleanAnnotation(Collection<? extends Annotation> annotations) {
		for (Annotation a : annotations) {
			// less than two letters
			String ne = a.getCoveredText();
			if (ne.replaceAll("[^\\p{L}]", "").length() < 2) {
				log.log(Level.FINEST, "Removing Named Entity: " + ne);
				a.removeFromIndexes();
			}
		}
	}
}
