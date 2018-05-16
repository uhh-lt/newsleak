package uhh_lt.newsleak.annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
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

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class NerMicroservice extends JCasAnnotator_ImplBase {

	
	public final static String NER_SERVICE_URL = "nerMicroserviceUrl";
	@ConfigurationParameter(
			name = NER_SERVICE_URL, 
			mandatory = true,
			description = "Url to microservice for named entity recognition (JSON API)")
	private String nerMicroserviceUrl;
	
	private Map<String, Locale> localeMap;
	
	Logger log;
	HttpClient httpClient;
	HttpPost request;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
		httpClient = HttpClientBuilder.create().build();
		request = new HttpPost("http://" + nerMicroserviceUrl);
		localeMap = LanguageDetector.localeToISO(); 
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		// document language in ISO-639-1 format
		String docLang = localeMap.get(jcas.getDocumentLanguage()).getLanguage();

		try {
			
			// create json request for microservice
			
			XContentBuilder sb = XContentFactory.jsonBuilder().startObject();
			sb.field("lang", docLang);
			sb.field("sentences").startArray();
			Collection<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, 0, jcas.getDocumentText().length());
			for (Sentence sentence : sentences) {
				Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
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
			}
			catch (Exception e) {
				log.log(Level.SEVERE, "Invalid NER result. Check if there is a model for language '" + docLang + "' in the NER microservice?");
		
				System.exit(1);
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

				Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
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
						}
						else if (tag.equals("I-PER")) {
							annotation = new Person(jcas);
						}
						else if (tag.equals("I-LOC")) {
							annotation = new Location(jcas);
						}
						else if (tag.equals("I-ORG")) {
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
		catch (IOException e) {
			e.printStackTrace();
		}
		
		cleanNerAnnotations(jcas);
		
	}


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
	
	private void cleanAnnotation(Collection<?extends Annotation> annotations) {
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
