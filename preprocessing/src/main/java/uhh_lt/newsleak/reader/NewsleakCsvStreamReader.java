package uhh_lt.newsleak.reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import uhh_lt.newsleak.types.Metadata;

public class NewsleakCsvStreamReader extends CasCollectionReader_ImplBase {

	/**
	 * Directory containing input files
	 */
	public static final String PARAM_INPUTDIR = "inputDir";
	@ConfigurationParameter(name=PARAM_INPUTDIR, mandatory=false, defaultValue=".")
	private String inputDir;

	public static final String PARAM_DOCUMENT_FILE = "documentFile";
	@ConfigurationParameter(name=PARAM_DOCUMENT_FILE, mandatory=true)
	private String documentFile;

	public static final String PARAM_METADATA_FILE = "metadataFile";
	@ConfigurationParameter(name=PARAM_METADATA_FILE, mandatory=true)
	private String metadataFile;

	public static final String PARAM_DEFAULT_LANG = "defaultLanguage";
	@ConfigurationParameter(name=PARAM_DEFAULT_LANG, mandatory=false, defaultValue="en")
	private String defaultLanguage;

	private Reader csvReader;
	private Iterable<CSVRecord> records;
	private Iterator<CSVRecord> recordsIterator;
	
//	private Reader metadataReader;
//	private Iterable<CSVRecord> metadata; 
//	private Iterator<CSVRecord> metadataIterator; 

	private int totalRecords = 0;
	private int currentRecord = 0;
	private int maxRecords = 100;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			File csvFile = new File(inputDir, documentFile);
			csvReader = new FileReader(csvFile);
			records = CSVFormat.RFC4180.parse(csvReader);
			recordsIterator = records.iterator();
			
			// Process metadata file separately
//		    metadataReader = new FileReader(new File(inputDir, metadataFile));
//		    metadata = CSVFormat.RFC4180.parse(metadataReader);
//		    metadataIterator = metadata.iterator();
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}


	public void getNext(CAS cas) throws IOException, CollectionException {
		currentRecord++;
		JCas jcas;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			throw new CollectionException(e);
		}
		
		// Set document data
		CSVRecord record = recordsIterator.next();
		String docId = record.get(0);
		
		// split into paragraphs
		// add paragraph with annotation
		// LangDect: per paragraph, doc-lang = majority of paragraph languages
		
		jcas.setDocumentText(record.get(1));
		jcas.setDocumentLanguage(record.size() > 3 ? record.get(3) : defaultLanguage);
		
		// Set metadata
		Metadata metaCas = new Metadata(jcas);
		metaCas.setDocId(docId);
		metaCas.setTimestamp(record.get(2));
		metaCas.addToIndexes();
		
//		CSVRecord metaRecord;
//		ArrayList<String> tripletNames = new ArrayList<String>();
//		ArrayList<String> tripletValues = new ArrayList<String>();
//		ArrayList<String> tripletTypes = new ArrayList<String>();
//		while (metadataIterator.hasNext()) {
//			metaRecord = metadataIterator.next();
//			Integer refId = Integer.parseInt(metaRecord.get(2));
//			if (docId != refId) {
//				break;
//			}
//		}
		
		
	}


	public void close() throws IOException {
		csvReader.close();
		// metadataReader.close();
	}

	public Progress[] getProgress() {
		return new Progress[] {
				new ProgressImpl(
						Long.valueOf(currentRecord).intValue(),
						Long.valueOf(totalRecords).intValue(),
						Progress.ENTITIES
						)
		};
	}

	public boolean hasNext() throws IOException, CollectionException {
		if (currentRecord > maxRecords) return false;
		return recordsIterator.hasNext();
	}

}
