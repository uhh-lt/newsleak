package uhh_lt.newsleak.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.examples.experiment.pos.XmiWriter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import uhh_lt.newsleak.annotator.DictionaryExtractor;
import uhh_lt.newsleak.annotator.HeidelTimeOpenNLP;
import uhh_lt.newsleak.annotator.KeytermExtractor;
import uhh_lt.newsleak.annotator.LanguageDetector;
import uhh_lt.newsleak.annotator.NerMicroservice;
import uhh_lt.newsleak.annotator.SentenceCleaner;
import uhh_lt.newsleak.annotator.SegmenterICU;
import uhh_lt.newsleak.reader.HooverElasticsearchReader;
import uhh_lt.newsleak.reader.NewsleakCsvStreamReader;
import uhh_lt.newsleak.reader.NewsleakElasticsearchReader;
import uhh_lt.newsleak.reader.NewsleakReader;
import uhh_lt.newsleak.resources.DictionaryResource;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.HooverResource;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.PostgresResource;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.writer.ElasticsearchDocumentWriter;
import uhh_lt.newsleak.writer.PostgresDbWriter;
import uhh_lt.newsleak.writer.TextLineWriter;

/**
 * Information extraction pipeline. The process iterates over the entire dataset
 * twice.
 * 
 * The first process reads fulltexts and metadata from a @see
 * uhh_lt.newsleak.reader.NewsleakReader, then determines the language for each
 * document and writes everything temporarily to an elasticsearch index
 * (metadata is written to a temporary file on the disk for later insertion into
 * the database).
 * 
 * The second process iterates over the elasticsearch index and extracts
 * entities from fulltexts for each of the configured languages separately. If
 * languages mainly contained in a collection are unknown, the first process
 * outputs a statistic of how many documents of a language it has seen.
 * 
 * Extracted information is written into a relation database (postgres) to allow
 * the newsleak explorer app relational queries lateron.
 *
 */
public class InformationExtraction2Postgres extends NewsleakPreprocessor {

	/**
	 * The main method running language detection and information extraction.
	 *
	 * @param args
	 *            CLI option pointing to the configuration file
	 * @throws Exception
	 *             anything that can go wrong...
	 */
	public static void main(String[] args) throws Exception {

		InformationExtraction2Postgres np = new InformationExtraction2Postgres();

		// read configuration file
		np.getConfiguration(args);

		// run language detection
		np.pipelineLanguageDetection();
		// extract information (per language)
		np.pipelineAnnotation();

		// init postgres db
		np.initDb(np.dbName, np.dbUrl, np.dbUser, np.dbPass);
		
		// create postgres indices
		String indexSql = FileUtils.readFileToString(new File(np.dbIndices)).replace("\n", "");
		try {
			st.executeUpdate(indexSql);
			np.logger.log(Level.INFO, "Index created");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// import temporary metadata.csv
		np.metadataToPostgres();

		conn.close();
	}

	/**
	 * Metadata is supposed to be presented in a four-tuple CSV format (docid, key,
	 * value, type). @see uhh_lt.newsleak.reader.NewsleakReader should write a
	 * temporary metadata file in that format (or assume it was produced by an
	 * external process)
	 * 
	 * The CSV file is imported via postgres directly.
	 * 
	 * See <i>data/metadata_example.csv</i> for an example.
	 */
	private void metadataToPostgres() {

		try {
			// we need a mapping of document ids since ElasticsearchDocumentWriter generates
			// new Ids from an autoincrement-value
			String mappedMetadataFilepath = this.dataDirectory + File.separator + this.metadataFile + ".mapped";
			mappingIdsInMetadata(mappedMetadataFilepath);

			// import csv into postgres db
			CopyManager cpManager = new CopyManager((BaseConnection) conn);
			st.executeUpdate("TRUNCATE TABLE metadata;");
			this.logger.log(Level.INFO, "Importing metadata from " + mappedMetadataFilepath);
			Long n = cpManager.copyIn("COPY metadata FROM STDIN WITH CSV", new FileReader(mappedMetadataFilepath));
			this.logger.log(Level.INFO, n + " metadata imported");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void mappingIdsInMetadata(String mappedMetadataFile) throws Exception {
		// read mappings file
		FileInputStream fis = new FileInputStream(this.dataDirectory + File.separator + this.metadataFile + ".id-map");
		ObjectInputStream ois = new ObjectInputStream(fis);
		HashMap<Integer, ArrayList<Integer>> documentIdMapping = (HashMap<Integer, ArrayList<Integer>>) ois
				.readObject();
		ois.close();

		// open metadata file, replace ids, write to temporary metadata file
		BufferedWriter writer = new BufferedWriter(new FileWriter(mappedMetadataFile));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180);

		BufferedReader reader = new BufferedReader(new FileReader(this.dataDirectory + File.separator + this.metadataFile));
		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(reader);
		for (CSVRecord record : records) {
			Integer tmpDocId = Integer.parseInt(record.get(0));
			if (documentIdMapping.containsKey(tmpDocId)) { 
				ArrayList<Integer> mappedIds = documentIdMapping.get(tmpDocId);
				int nParts = mappedIds.size();
				int partCounter = 0;
				for (Integer newsleakDocId : mappedIds) {
					String key = StringUtils.capitalize(record.get(1));
					String value = record.get(2);
					if (nParts > 0 && key.equals("Subject")) {
						partCounter++;
						value += " (" + partCounter + "/" + nParts + ")";
					}
					ArrayList<String> meta = new ArrayList<String>();
					meta.add(newsleakDocId.toString());
					meta.add(key);
					meta.add(value);
					meta.add(record.get(3));
					csvPrinter.printRecord(meta);
				}
			}
		}
		csvPrinter.close();
		reader.close();
	}

	/**
	 * Gets the UIMA reader according to the current configuration.
	 *
	 * @param type
	 *            The reader type (e.g. "csv" for externally preprocessed fulltexts
	 *            and metadata, or "hoover" for the Hoover text extraction system)
	 * @return the reader
	 * @throws ResourceInitializationException
	 *             the resource initialization exception
	 */
	public CollectionReaderDescription getReader(String type) throws ResourceInitializationException {
		CollectionReaderDescription reader = null;
		if (type.equals("csv")) {
			reader = CollectionReaderFactory.createReaderDescription(NewsleakCsvStreamReader.class, this.typeSystem,
					NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, this.documentFile,
					NewsleakCsvStreamReader.PARAM_METADATA_FILE, this.metadataFile,
					NewsleakCsvStreamReader.PARAM_INPUTDIR, this.dataDirectory,
					NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, this.defaultLanguage,
					NewsleakReader.PARAM_DEBUG_MAX_DOCS, this.debugMaxDocuments, NewsleakReader.PARAM_MAX_DOC_LENGTH,
					this.maxDocumentLength);
		} else if (type.equals("hoover")) {
			this.metadataFile = this.hooverTmpMetadata;
			ExternalResourceDescription hooverResource = ExternalResourceFactory.createExternalResourceDescription(
					HooverResource.class, HooverResource.PARAM_HOST, this.hooverHost, HooverResource.PARAM_CLUSTERNAME,
					this.hooverClustername, HooverResource.PARAM_INDEX, this.hooverIndex, HooverResource.PARAM_PORT,
					this.hooverPort, HooverResource.PARAM_SEARCHURL, this.hooverSearchUrl);
			reader = CollectionReaderFactory.createReaderDescription(HooverElasticsearchReader.class, this.typeSystem,
					HooverElasticsearchReader.RESOURCE_HOOVER, hooverResource,
					HooverElasticsearchReader.RESOURCE_METADATA, this.getMetadataResourceDescription(),
					NewsleakReader.PARAM_DEBUG_MAX_DOCS, this.debugMaxDocuments, NewsleakReader.PARAM_MAX_DOC_LENGTH,
					this.maxDocumentLength);
		} else {
			this.logger.log(Level.SEVERE, "Unknown reader type: " + type);
			System.exit(1);
		}
		return reader;
	}

	/**
	 * The language detection pipeline detects the language of each document and
	 * writes this information and the metadata acquired by the the reader
	 * temporarily to disk. The extracted fulltext is temporarily stored in the
	 * elasticsearch index.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void pipelineLanguageDetection() throws Exception {
		statusListener = new NewsleakStatusCallbackListener(this.logger);

		// check for language support
		HashSet<String> supportedLanguages = LanguageDetector.getSupportedLanguages();
		for (String lang : this.processLanguages) {
			if (!supportedLanguages.contains(lang)) {
				logger.log(Level.SEVERE, "Language " + lang + " not supported (use ISO 639-3 codes)");
				System.exit(1);
			}
		}

		// reader
		CollectionReaderDescription reader = getReader(this.readerType);

		// language detection annotator
		ExternalResourceDescription resourceLangDect = ExternalResourceFactory.createExternalResourceDescription(
				LanguageDetectorResource.class, LanguageDetectorResource.PARAM_MODEL_FILE,
				"resources/langdetect-183.bin");
		AnalysisEngineDescription langDetect = AnalysisEngineFactory.createEngineDescription(LanguageDetector.class,
				LanguageDetector.MODEL_FILE, resourceLangDect, LanguageDetector.METADATA_FILE,
				this.getMetadataResourceDescription(), LanguageDetector.PARAM_DEFAULT_LANG, this.defaultLanguage,
				LanguageDetector.DOCLANG_FILE, "data/documentLanguages.ser");

		// elasticsearch writer to store fulltexts
		AnalysisEngineDescription esWriter = AnalysisEngineFactory.createEngineDescription(
				ElasticsearchDocumentWriter.class, ElasticsearchDocumentWriter.RESOURCE_ESCLIENT,
				this.getElasticsearchResourceDescription("true"),
				ElasticsearchDocumentWriter.PARAM_PARAGRAPHS_AS_DOCUMENTS, this.paragraphsAsDocuments,
				ElasticsearchDocumentWriter.PARAM_MAX_DOC_LENGTH, this.maxDocumentLength);

		// create pipeline
		AnalysisEngineDescription ldPipeline = AnalysisEngineFactory.createEngineDescription(langDetect, esWriter);

		// run pipeline in parallel manner with UIMA CPE
		CpeBuilder ldCpeBuilder = new CpeBuilder();
		ldCpeBuilder.setReader(reader);
		ldCpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
		ldCpeBuilder.setAnalysisEngine(ldPipeline);
		CollectionProcessingEngine engine = ldCpeBuilder.createCpe(statusListener);
		engine.process();

		// wait until language detection has finished before running the next
		// information extraction processing step
		while (statusListener.isProcessing()) {
			Thread.sleep(500);
		}

	}

	/**
	 * The annotation pipeline performs several annotation tasks, for each language
	 * separately (sentence detection, sentence cleaning, temporal expression
	 * detection, named entity recognition, keyterm extraction, and dictionary
	 * annotation). Extracted information is stored in a postgres database.
	 * 
	 * Languages to process have to be configured as a comma separated list of
	 * ISO-639-3 language codes in the configuration file ("processlanguages").
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void pipelineAnnotation() throws Exception {

		/*
		 * Proceeding for multi-language collections: - 1. run language detection and
		 * write language per document to ES index - 2. set document language for
		 * unsupported languages to default language - 3. run annotation pipeline per
		 * language with lang dependent resources
		 */

		// iterate over configured ISO-639-3 language codes
		boolean firstLanguage = true;
		for (String currentLanguage : processLanguages) {

			NewsleakStatusCallbackListener annotationListener = new NewsleakStatusCallbackListener(this.logger);

			Map<String, Locale> localeMap = LanguageDetector.localeToISO();
			Locale currentLocale = localeMap.get(currentLanguage);

			logger.log(Level.INFO, "Processing " + currentLocale.getDisplayName() + " (" + currentLanguage + ")");
			Thread.sleep(2000);

			// reader
			CollectionReaderDescription esReader = CollectionReaderFactory.createReaderDescription(
					NewsleakElasticsearchReader.class, this.typeSystem, NewsleakElasticsearchReader.RESOURCE_ESCLIENT,
					this.getElasticsearchResourceDescription("false"), NewsleakElasticsearchReader.PARAM_LANGUAGE,
					currentLanguage);

			// sentences
			AnalysisEngineDescription sentenceICU = AnalysisEngineFactory.createEngineDescription(SegmenterICU.class,
					SegmenterICU.PARAM_LOCALE, currentLanguage);

			// sentence cleaner
			AnalysisEngineDescription sentenceCleaner = AnalysisEngineFactory
					.createEngineDescription(SentenceCleaner.class);

			// heideltime
			AnalysisEngineDescription heideltime = AnalysisEngineFactory.createEngineDescription(
					HeidelTimeOpenNLP.class, HeidelTimeOpenNLP.PARAM_LANGUAGE,
					"auto-" + currentLocale.getDisplayName().toLowerCase(), HeidelTimeOpenNLP.PARAM_LOCALE, "en_US");

			// named entity recognition
			AnalysisEngineDescription nerMicroservice = AnalysisEngineFactory.createEngineDescription(
					NerMicroservice.class, NerMicroservice.NER_SERVICE_URL, this.nerServiceUrl);

			// keyterms
			AnalysisEngineDescription keyterms = AnalysisEngineFactory.createEngineDescription(KeytermExtractor.class,
					KeytermExtractor.PARAM_N_KEYTERMS, 15, KeytermExtractor.PARAM_LANGUAGE_CODE, currentLanguage);

			// dictionaries
			ExternalResourceDescription dictResource = ExternalResourceFactory.createExternalResourceDescription(
					DictionaryResource.class, DictionaryResource.PARAM_DATADIR,
					this.configDir + File.separator + "dictionaries", DictionaryResource.PARAM_DICTIONARY_FILES,
					this.dictionaryFiles, DictionaryResource.PARAM_LANGUAGE_CODE, currentLanguage);
			AnalysisEngineDescription dictionaries = AnalysisEngineFactory.createEngineDescription(
					DictionaryExtractor.class, 
					DictionaryExtractor.RESOURCE_DICTIONARIES, dictResource,
					DictionaryExtractor.PARAM_EXTRACT_EMAIL, this.patternEmail,
					DictionaryExtractor.PARAM_EXTRACT_URL, this.patternUrl,
					DictionaryExtractor.PARAM_EXTRACT_PHONE, this.patternPhone,
					DictionaryExtractor.PARAM_EXTRACT_IP, this.patternIP);

			// alternative writers for testing purposes (rawtext, xmi) ...
			// ... raw text writer
			// ExternalResourceDescription resourceLinewriter =
			// ExternalResourceFactory.createExternalResourceDescription(
			// TextLineWriterResource.class,
			// TextLineWriterResource.PARAM_OUTPUT_FILE, this.dataDirectory + File.separator
			// + "output.txt");
			// AnalysisEngineDescription linewriter =
			// AnalysisEngineFactory.createEngineDescription(
			// TextLineWriter.class,
			// TextLineWriter.RESOURCE_LINEWRITER, resourceLinewriter
			// );
			//
			// ... xmi writer
			// AnalysisEngineDescription xmi =
			// AnalysisEngineFactory.createEngineDescription(
			// XmiWriter.class,
			// XmiWriter.PARAM_OUTPUT_DIRECTORY, this.dataDirectory + File.separator + "xmi"
			// );

			// postgres writer
			ExternalResourceDescription resourcePostgres = ExternalResourceFactory.createExternalResourceDescription(
					PostgresResource.class, PostgresResource.PARAM_DBURL, this.dbUrl, PostgresResource.PARAM_DBNAME,
					this.dbName, PostgresResource.PARAM_DBUSER, this.dbUser, PostgresResource.PARAM_DBPASS, this.dbPass,
					PostgresResource.PARAM_TABLE_SCHEMA, this.dbSchema, PostgresResource.PARAM_INDEX_SCHEMA,
					this.dbIndices, PostgresResource.PARAM_CREATE_DB, firstLanguage ? "true" : "false");
			AnalysisEngineDescription postgresWriter = AnalysisEngineFactory.createEngineDescription(
					PostgresDbWriter.class, PostgresDbWriter.RESOURCE_POSTGRES, resourcePostgres);

			// define pipeline
			AnalysisEngineDescription pipeline = AnalysisEngineFactory.createEngineDescription(sentenceICU,
					sentenceCleaner, dictionaries, heideltime, nerMicroservice, keyterms,
					// linewriter,
					// xmi,
					postgresWriter);

			// run as UIMA CPE
			CpeBuilder cpeBuilder = new CpeBuilder();
			cpeBuilder.setReader(esReader);
			cpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
			cpeBuilder.setAnalysisEngine(pipeline);

			// run processing
			CollectionProcessingEngine engine = cpeBuilder.createCpe(annotationListener);
			engine.process();

			while (annotationListener.isProcessing()) {
				// wait...
				Thread.sleep(1);
			}

			firstLanguage = false;

		}

	}

}
