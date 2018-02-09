package uhh_lt.newsleak.writer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.uima.Location;
import opennlp.uima.Organization;
import opennlp.uima.Person;
import uhh_lt.newsleak.resources.PostgresResource;
import uhh_lt.newsleak.types.Metadata;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=false)
public class PostgresDbWriter extends JCasAnnotator_ImplBase {

	private Logger logger;
	
	private int counter = 0;
	private int INTERNAL_BATCH_SIZE = 100;

	public static final String RESOURCE_POSTGRES = "postgresResource";
	@ExternalResource(key = RESOURCE_POSTGRES)
	private PostgresResource postgresResource;

	private NewsleakTimeFormatter timeFormatter;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		logger = context.getLogger();
		timeFormatter = new NewsleakTimeFormatter();
	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		postgresResource.commit();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		counter++;

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		Integer docId = Integer.parseInt(metadata.getDocId());

		try {

			// documents
			String docText = jcas.getDocumentText().replaceAll("\r", "");
			String docDate = metadata.getTimestamp();
			postgresResource.insertDocument(docId, docText, docDate);

			// entities and offsets
			Collection<Person> persons = JCasUtil.selectCovered(jcas, Person.class, 0, jcas.getDocumentText().length());
			processEntities(persons, "PER", docId);
			Collection<Organization> orgs = JCasUtil.selectCovered(jcas, Organization.class, 0, jcas.getDocumentText().length());
			processEntities(orgs, "ORG", docId);
			Collection<Location> locs = JCasUtil.selectCovered(jcas, Location.class, 0, jcas.getDocumentText().length());
			processEntities(locs, "LOC", docId);
			
			// eventtime
			ArrayList<String> extractedTimes = timeFormatter.format(jcas);
			if (extractedTimes.size() > 0) {
				for (String line : extractedTimes) {
					String[] items = line.split("\t");
					try {
						String formattedDate = timeFormatter.filterDate(items[4]);
						if (formattedDate != null) {
							postgresResource.insertEventtime(docId, Integer.parseInt(items[0]), Integer.parseInt(items[1]), items[2], items[3], formattedDate);
						}
					}
					catch (Exception e) {
						System.out.println(items);
					}
				}
			}
			
			// terms
			String keytermList = metadata.getKeyterms();
			if (keytermList != null) {
				for (String item : metadata.getKeyterms().split("\t")) {
					String[] termFrq = item.split(":");
					postgresResource.insertKeyterms(docId, termFrq[0], Integer.parseInt(termFrq[1]));
				}
			}			


		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Could not write document " + docId);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (counter % INTERNAL_BATCH_SIZE == 0) {
			postgresResource.commit();
		}
		
	}

	private void processEntities(Collection<? extends Annotation> matches, String type, Integer docId) throws SQLException {
		HashMap<String, Integer> counter = new HashMap<String, Integer>();
		HashMap<String, ArrayList<Annotation>> offsets = new HashMap<String, ArrayList<Annotation>>();
		for (Annotation annotation : matches) {
			String entity = annotation.getCoveredText();
			counter.put(entity, counter.containsKey(entity) ? counter.get(entity) + 1 : 1);
			if (offsets.containsKey(entity)) {
				offsets.get(entity).add(annotation);
			} else {
				ArrayList<Annotation> l = new ArrayList<Annotation>();
				l.add(annotation);
				offsets.put(entity, l);
			}
		}
		for (String entity : counter.keySet()) {
			Integer entityId = postgresResource.insertEntity(entity, type, counter.get(entity));
			for (Annotation annotation : offsets.get(entity)) {
				postgresResource.insertEntityoffset(docId, entityId, annotation.getBegin(), annotation.getEnd());
			}
		}
	}

}
