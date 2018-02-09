package uhh_lt.newsleak.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.examples.experiment.pos.XmiWriter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.xml.sax.SAXException;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import opennlp.uima.Sentence;
import opennlp.uima.Token;
import opennlp.uima.namefind.NameFinder;
import opennlp.uima.namefind.TokenNameFinderModelResourceImpl;
import opennlp.uima.postag.POSModelResourceImpl;
import opennlp.uima.postag.POSTagger;
import opennlp.uima.sentdetect.SentenceDetector;
import opennlp.uima.sentdetect.SentenceModelResourceImpl;
import opennlp.uima.tokenize.Tokenizer;
import opennlp.uima.tokenize.TokenizerModelResourceImpl;
import opennlp.uima.util.UimaUtil;
import uhh_lt.newsleak.annotator.HeidelTimeOpenNLP;
import uhh_lt.newsleak.annotator.KeytermExtractor;
import uhh_lt.newsleak.annotator.LanguageDetector;
import uhh_lt.newsleak.reader.HooverElasticsearchReader;
import uhh_lt.newsleak.reader.NewsleakCsvStreamReader;
import uhh_lt.newsleak.reader.NewsleakElasticsearchReader;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.HooverResource;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.PostgresResource;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.writer.ElasticsearchAnnotationWriter;
import uhh_lt.newsleak.writer.ElasticsearchDocumentWriter;
import uhh_lt.newsleak.writer.PostgresDbWriter;
import uhh_lt.newsleak.writer.TextLineWriter;

/**
 * Reads document.csv and metadata.csv, processes them in a UIMA pipeline
 * and writes output to an ElasticSearch index.
 *
 */
public class InformationExtraction2Postgres extends NewsleakPreprocessor
{

	public static void main(String[] args) throws Exception
	{

		InformationExtraction2Postgres np = new InformationExtraction2Postgres();
		np.getConfiguration(args);

		// run language detection
		np.pipelineLanguageDetection();
		// extract information (per language)
		np.pipelineAnnotation();

		// import metadata.csv
		np.initDb(np.dbName, np.dbUrl, np.dbUser, np.dbPass);
		np.metadataToPostgres();

		// create postgres indices
		String indexSql = FileUtils.readFileToString(new File(np.dbIndices)).replace("\n", "");
		try {
			st.executeUpdate(indexSql);
			np.logger.log(Level.INFO, "Index created");
		} catch (Exception e) {
			e.printStackTrace();
		}

		conn.close();
	}

	private void metadataToPostgres() {
		try {
			CopyManager cpManager = new CopyManager((BaseConnection) conn);
			st.executeUpdate("TRUNCATE TABLE metadata;");
			String metaFile = this.dataDirectory + File.separator + this.metadataFile;
			this.logger.log(Level.INFO, "Importing metadata from " + metaFile);
			Long n = cpManager.copyIn("COPY metadata FROM STDIN WITH CSV", new FileReader(metaFile));
			this.logger.log(Level.INFO, n + " metadata imported");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public CollectionReaderDescription getReader(String type) throws ResourceInitializationException {
		CollectionReaderDescription reader = null;
		if (type.equals("csv")) {
			reader = CollectionReaderFactory.createReaderDescription(
					NewsleakCsvStreamReader.class, this.typeSystem,
					NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, this.documentFile,
					NewsleakCsvStreamReader.PARAM_METADATA_FILE, this.metadataFile,
					NewsleakCsvStreamReader.PARAM_INPUTDIR, this.dataDirectory,
					NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, this.defaultLanguage,
					NewsleakCsvStreamReader.PARAM_DEBUG_MAX_DOCS, this.debugMaxDocuments
					);
		} else if (type.equals("hoover")) {
			this.metadataFile = this.hooverTmpMetadata;
			ExternalResourceDescription hooverResource = ExternalResourceFactory.createExternalResourceDescription(
					HooverResource.class, 
					HooverResource.PARAM_METADATA_FILE, this.dataDirectory + File.separator + this.metadataFile,
					HooverResource.PARAM_HOST, this.hooverHost,
					HooverResource.PARAM_CLUSTERNAME, this.hooverClustername,
					HooverResource.PARAM_INDEX, this.hooverIndex,
					HooverResource.PARAM_PORT, this.hooverPort
					);
			reader = CollectionReaderFactory.createReaderDescription(
					HooverElasticsearchReader.class, this.typeSystem,
					HooverElasticsearchReader.RESOURCE_HOOVER, hooverResource,
					HooverElasticsearchReader.PARAM_DEFAULT_LANG, this.defaultLanguage,
					HooverElasticsearchReader.PARAM_DEBUG_MAX_DOCS, this.debugMaxDocuments
					);
		} else {
			this.logger.log(Level.SEVERE, "Unknown reader type: " + type);
			System.exit(1);
		}
		return reader;
	}
	public void pipelineLanguageDetection() throws Exception {
		statusListener = new NewsleakStatusCallbackListener(this.logger);

		// reader
		CollectionReaderDescription reader = getReader(this.readerType);

		// language detection
		ExternalResourceDescription resourceLangDect = ExternalResourceFactory.createExternalResourceDescription(
				LanguageDetectorResource.class, new File("resources/langdetect-183.bin"));
		AnalysisEngineDescription langDetect = AnalysisEngineFactory.createEngineDescription(
				LanguageDetector.class,
				LanguageDetector.MODEL_FILE, resourceLangDect,
				LanguageDetector.DOCLANG_FILE, "data/documentLanguages.ser"
				);

		// elasticsearch writer
		ExternalResourceDescription esResource = ExternalResourceFactory.createExternalResourceDescription(
				ElasticsearchResource.class, 
				ElasticsearchResource.PARAM_CREATE_INDEX, "true",
				ElasticsearchResource.PARAM_CLUSTERNAME, this.esClustername,
				ElasticsearchResource.PARAM_INDEX, this.esIndex,
				ElasticsearchResource.PARAM_HOST, this.esHost,
				ElasticsearchResource.PARAM_PORT, this.esPort,
				ElasticsearchResource.PARAM_DOCUMENT_MAPPING_FILE, "desc/elasticsearch_mapping_document_2.4.json");
		AnalysisEngineDescription esWriter = AnalysisEngineFactory.createEngineDescription(
				ElasticsearchDocumentWriter.class,
				ElasticsearchDocumentWriter.RESOURCE_ESCLIENT, esResource
				);

		AnalysisEngineDescription ldPipeline = AnalysisEngineFactory.createEngineDescription(	
				langDetect,
				esWriter
				);

		CpeBuilder ldCpeBuilder = new CpeBuilder();
		ldCpeBuilder.setReader(reader);
		ldCpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
		ldCpeBuilder.setAnalysisEngine(ldPipeline);
		CollectionProcessingEngine engine = ldCpeBuilder.createCpe(statusListener); 
		engine.process();

		while (statusListener.isProcessing()) {
			Thread.sleep(500);
		}
	}

	public void pipelineAnnotation() throws Exception {
		statusListener = new NewsleakStatusCallbackListener(this.logger);

		// reader
		ExternalResourceDescription esResource = ExternalResourceFactory.createExternalResourceDescription(
				ElasticsearchResource.class, 
				ElasticsearchResource.PARAM_CREATE_INDEX, "false",
				ElasticsearchResource.PARAM_HOST, this.esHost,
				ElasticsearchResource.PARAM_CLUSTERNAME, this.esClustername,
				ElasticsearchResource.PARAM_INDEX, this.esIndex,
				ElasticsearchResource.PARAM_PORT, this.esPort,
				ElasticsearchResource.PARAM_DOCUMENT_MAPPING_FILE, "desc/elasticsearch_mapping_document_2.4.json");
		CollectionReaderDescription esReader = CollectionReaderFactory.createReaderDescription(
				NewsleakElasticsearchReader.class, this.typeSystem,
				NewsleakElasticsearchReader.RESOURCE_ESCLIENT, esResource,
				NewsleakElasticsearchReader.PARAM_LANGUAGE, "eng"
				);


		/* openNLP base annotations: Sentence, Token, POS */

		/* Strategy for Multi-Language-Support:
		 * - 1. run language detection and write out language per document
		 * - 2. create list of languages in corpus (intersect with available)
		 * - 3. run annotation pipeline with language dependent config files
		 */

		// sentences
		ExternalResourceDescription resourceSentence = ExternalResourceFactory.createExternalResourceDescription(
				SentenceModelResourceImpl.class, new File("./resources/eng/en-sent.bin"));
		AnalysisEngineDescription sentence = AnalysisEngineFactory.createEngineDescription(
				SentenceDetector.class,
				UimaUtil.MODEL_PARAMETER, resourceSentence,
				UimaUtil.SENTENCE_TYPE_PARAMETER, Sentence.class,
				UimaUtil.IS_REMOVE_EXISTINGS_ANNOTAIONS, false
				);

		// tokens
		ExternalResourceDescription resourceToken = ExternalResourceFactory.createExternalResourceDescription(
				TokenizerModelResourceImpl.class, new File("./resources/eng/en-token.bin"));
		AnalysisEngineDescription token = AnalysisEngineFactory.createEngineDescription(
				Tokenizer.class,
				UimaUtil.MODEL_PARAMETER, resourceToken,
				UimaUtil.SENTENCE_TYPE_PARAMETER, Sentence.class,
				UimaUtil.TOKEN_TYPE_PARAMETER, Token.class
				);		

		// pos
		ExternalResourceDescription resourcePos = ExternalResourceFactory.createExternalResourceDescription(
				POSModelResourceImpl.class, new File("./resources/eng/en-pos-maxent.bin"));
		AnalysisEngineDescription pos = AnalysisEngineFactory.createEngineDescription(
				POSTagger.class,
				UimaUtil.MODEL_PARAMETER, resourcePos,
				UimaUtil.SENTENCE_TYPE_PARAMETER, Sentence.class,
				UimaUtil.TOKEN_TYPE_PARAMETER, Token.class,
				UimaUtil.POS_FEATURE_PARAMETER, "pos"
				);		


		// heideltime
		AnalysisEngineDescription heideltime = AnalysisEngineFactory.createEngineDescription(
				HeidelTimeOpenNLP.class,
				HeidelTimeOpenNLP.PARAM_LANGUAGE, "english",
				HeidelTimeOpenNLP.PARAM_LOCALE, "en_US"
				);


		// ner
		AnalysisEngineDescription nerPer = getOpennlpNerAed("PER", "opennlp.uima.Person", "./resources/eng/en-ner-person.bin");
		AnalysisEngineDescription nerOrg = getOpennlpNerAed("ORG", "opennlp.uima.Organization", "./resources/eng/en-ner-organization.bin");
		AnalysisEngineDescription nerLoc = getOpennlpNerAed("LOC", "opennlp.uima.Location", "./resources/eng/en-ner-location.bin");


		// keyterms
		AnalysisEngineDescription keyterms = AnalysisEngineFactory.createEngineDescription(
				KeytermExtractor.class
				);


		// writer
		ExternalResourceDescription resourceLinewriter = ExternalResourceFactory.createExternalResourceDescription(
				TextLineWriterResource.class, 
				TextLineWriterResource.PARAM_OUTPUT_FILE, this.dataDirectory + File.separator + "output.txt");
		AnalysisEngineDescription linewriter = AnalysisEngineFactory.createEngineDescription(
				TextLineWriter.class,
				TextLineWriter.RESOURCE_LINEWRITER, resourceLinewriter
				);

		AnalysisEngineDescription xmi = AnalysisEngineFactory.createEngineDescription(
				XmiWriter.class,
				XmiWriter.PARAM_OUTPUT_DIRECTORY, this.dataDirectory + File.separator + "xmi"
				);

		ExternalResourceDescription resourcePostgres = ExternalResourceFactory.createExternalResourceDescription(
				PostgresResource.class, 
				PostgresResource.PARAM_DBURL, this.dbUrl,
				PostgresResource.PARAM_DBNAME, this.dbName,
				PostgresResource.PARAM_DBUSER, this.dbUser,
				PostgresResource.PARAM_DBPASS, this.dbPass,
				PostgresResource.PARAM_TABLE_SCHEMA, this.dbSchema,
				PostgresResource.PARAM_CREATE_DB, "true"
				);
		AnalysisEngineDescription postgresWriter = AnalysisEngineFactory.createEngineDescription(
				PostgresDbWriter.class,
				PostgresDbWriter.RESOURCE_POSTGRES, resourcePostgres
				);


		// define pipeline

		AnalysisEngineDescription pipeline = AnalysisEngineFactory.createEngineDescription(
				sentence,
				token,
				pos,
				heideltime,
				nerPer, 
				nerOrg,
				nerLoc,
				keyterms,
				linewriter,
				// xmi,
				// esWriter
				postgresWriter
				);

		CpeBuilder cpeBuilder = new CpeBuilder();
		cpeBuilder.setReader(esReader);
		cpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
		cpeBuilder.setAnalysisEngine(pipeline);

		// run processing
		CollectionProcessingEngine engine = cpeBuilder.createCpe(statusListener);
		engine.process();

		while (statusListener.isProcessing()) {
			// wait...
			Thread.sleep(500);
		}
	}

	private AnalysisEngineDescription getOpennlpNerAed(String shortType, String type, String modelFile) throws ResourceInitializationException {
		ExternalResourceDescription resourceNer = ExternalResourceFactory.createExternalResourceDescription(
				TokenNameFinderModelResourceImpl.class, new File(modelFile));
		AnalysisEngineDescription ner = AnalysisEngineFactory.createEngineDescription(
				NameFinder.class,
				UimaUtil.MODEL_PARAMETER, resourceNer,
				UimaUtil.SENTENCE_TYPE_PARAMETER, Sentence.class,
				UimaUtil.TOKEN_TYPE_PARAMETER, Token.class,
				NameFinder.NAME_TYPE_PARAMETER, type
				);
		return ner;
	}

}
