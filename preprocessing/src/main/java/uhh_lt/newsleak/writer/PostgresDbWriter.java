package uhh_lt.newsleak.writer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.FSCollectionFactory;
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
import uhh_lt.newsleak.types.DictTerm;
import uhh_lt.newsleak.types.Metadata;

/**
 * A writer to populate the newsleak postgres database with final fulltexts and
 * extracted entities from a prior annotation chain. This writer does not modify
 * the documents.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = false)
public class PostgresDbWriter extends JCasAnnotator_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant RESOURCE_POSTGRES. */
	public static final String RESOURCE_POSTGRES = "postgresResource";

	/** The postgres resource. */
	@ExternalResource(key = RESOURCE_POSTGRES)
	private PostgresResource postgresResource;

	/** The time formatter. */
	private NewsleakTimeFormatter timeFormatter;

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
		logger = context.getLogger();
		timeFormatter = new NewsleakTimeFormatter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
	 * collectionProcessComplete()
	 */
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		// commit final inserts/updates
		postgresResource.commit();
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

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		Integer docId = Integer.parseInt(metadata.getDocId());

		try {

			// documents
			String docText = jcas.getDocumentText().replaceAll("\r", "");
			String docDate = metadata.getTimestamp();
			postgresResource.insertDocument(docId, docText, docDate);

			// entities and offsets
			Collection<Person> persons = JCasUtil.select(jcas, Person.class);
			processEntities(persons, "PER", docId);
			Collection<Organization> orgs = JCasUtil.select(jcas, Organization.class);
			processEntities(orgs, "ORG", docId);
			Collection<Location> locs = JCasUtil.select(jcas, Location.class);
			processEntities(locs, "LOC", docId);

			// dictionary entities
			HashMap<String, HashSet<DictTerm>> dictAnnotations = new HashMap<String, HashSet<DictTerm>>();
			HashMap<String, HashMap<String, String>> baseFormMap = new HashMap<String, HashMap<String, String>>();
			Collection<DictTerm> dictTerms = JCasUtil.select(jcas, DictTerm.class);
			for (DictTerm dictTerm : dictTerms) {
				Collection<String> typeList = FSCollectionFactory.create(dictTerm.getDictType());
				int i = 0;
				for (String type : typeList) {
					HashSet<DictTerm> typeTerms = dictAnnotations.containsKey(type) ? dictAnnotations.get(type)
							: new HashSet<DictTerm>();
					HashMap<String, String> baseForms = baseFormMap.containsKey(type) ? baseFormMap.get(type)
							: new HashMap<String, String>();
					typeTerms.add(dictTerm);
					baseForms.put(dictTerm.getCoveredText(), dictTerm.getDictTerm().getNthElement(i));
					i++;
					dictAnnotations.put(type, typeTerms);
					baseFormMap.put(type, baseForms);
				}
			}
			for (String type : dictAnnotations.keySet()) {
				processEntities(dictAnnotations.get(type), type, docId, baseFormMap.get(type));
			}

			// eventtime
			ArrayList<String> extractedTimes = timeFormatter.format(jcas);
			if (extractedTimes.size() > 0) {
				for (String line : extractedTimes) {
					String[] items = line.split("\t");
					try {
						String formattedDate = timeFormatter.filterDate(items[4]);
						if (formattedDate != null) {
							postgresResource.insertEventtime(docId, Integer.parseInt(items[0]),
									Integer.parseInt(items[1]), items[2], items[3], formattedDate);
						}
					} catch (Exception e) {
						System.out.println(items);
					}
				}
			}

			// terms
			String keytermList = metadata.getKeyterms();
			if (keytermList != null) {
				for (String item : metadata.getKeyterms().split("\t")) {
					String[] termFrq = item.split(":");
					if (termFrq.length == 2) {
						postgresResource.insertKeyterms(docId, termFrq[0], Integer.parseInt(termFrq[1]));
					}
				}
			}

			// execute batches
			postgresResource.executeBatches();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Could not write document " + docId);
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Process entities.
	 *
	 * @param matches
	 *            the matches
	 * @param type
	 *            the type
	 * @param docId
	 *            the doc id
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void processEntities(Collection<? extends Annotation> matches, String type, Integer docId)
			throws SQLException {
		processEntities(matches, type, docId, null);
	}

	/**
	 * Process entities.
	 *
	 * @param matches
	 *            the matches
	 * @param type
	 *            the type
	 * @param docId
	 *            the doc id
	 * @param baseForms
	 *            the base forms
	 * @throws SQLException
	 *             the SQL exception
	 */
	private void processEntities(Collection<? extends Annotation> matches, String type, Integer docId,
			HashMap<String, String> baseForms) throws SQLException {
		HashMap<String, Integer> counter = new HashMap<String, Integer>();
		HashMap<String, ArrayList<Annotation>> offsets = new HashMap<String, ArrayList<Annotation>>();
		for (Annotation annotation : matches) {
			String entity;
			if (baseForms == null) {
				entity = annotation.getCoveredText();
			} else {
				String coveredText = annotation.getCoveredText();
				entity = baseForms.containsKey(coveredText) ? baseForms.get(coveredText) : coveredText;
			}
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
