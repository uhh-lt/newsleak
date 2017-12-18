package uhh_lt.newsleak.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import org.apache.uima.util.Logger;
import org.xml.sax.SAXException;

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
import uhh_lt.newsleak.annotator.LanguageDetector;
import uhh_lt.newsleak.reader.NewsleakCsvStreamReader;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.writer.ElasticsearchAnnotationWriter;
import uhh_lt.newsleak.writer.ElasticsearchDocumentWriter;
import uhh_lt.newsleak.writer.TextLineWriter;

/**
 * Reads document.csv and metadata.csv, processes them in a UIMA pipeline
 * and writes output to an ElasticSearch index.
 *
 */
public class NewsleakPreprocessor 
{

	private Logger logger;

	private Options cliOptions;
	private String configfile;

	private String defaultLanguage;
	private String dataDirectory;
	private String documentFile;
	private String metadataFile;

	private String esClustername;
	private String esIndex;
	private String esPort;

	private Integer threads;

	private TypeSystemDescription typeSystem;

	public NewsleakPreprocessor() {
		super();
		logger = UIMAFramework.getLogger();
	}

	private void getConfiguration() {
		getConfiguration(this.configfile);
	}

	private void getConfiguration(String config) {
		Properties prop = new Properties();

		try {
			InputStream input = new FileInputStream(config);
			prop.load(input);

			defaultLanguage = prop.getProperty("lang");
			dataDirectory = prop.getProperty("datadirectory");
			documentFile = prop.getProperty("documentfile");
			metadataFile = prop.getProperty("metadatafile");

			esClustername = prop.getProperty("esclustername");
			esIndex = prop.getProperty("esindex");
			esPort = prop.getProperty("esport");

			threads = Integer.valueOf(prop.getProperty("threads"));
			input.close();
		}
		catch (IOException e) {
			System.err.println("Could not read configuration file " + config);
			System.exit(1);
		}
	}

	private void getCliOptions(String[] args) {
		cliOptions = new Options();
		Option configfileOpt = new Option("c", "configfile", true, "config file path");
		configfileOpt.setRequired(true);
		cliOptions.addOption(configfileOpt);
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		try {
			cmd = parser.parse(cliOptions, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", cliOptions);
			System.exit(1);
			return;
		}
		this.configfile = cmd.getOptionValue("configfile");
	}

	public static void main( String[] args ) throws Exception
	{

		NewsleakPreprocessor np = new NewsleakPreprocessor();
		np.getCliOptions(args);
		np.getConfiguration();

		String typeSystemFile = new File("desc/NewsleakDocument.xml").getAbsolutePath();	
		np.typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(typeSystemFile);

		np.pipelineLanguageDetection();
		// np.pipelineAnnotation();

	}

	public void pipelineLanguageDetection() throws Exception {
		// reader
		CollectionReaderDescription csvReader = CollectionReaderFactory.createReaderDescription(
				NewsleakCsvStreamReader.class, this.typeSystem,
				NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, this.documentFile,
				NewsleakCsvStreamReader.PARAM_METADATA_FILE, this.metadataFile,
				NewsleakCsvStreamReader.PARAM_INPUTDIR, this.dataDirectory,
				NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, this.defaultLanguage
				);

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
				ElasticsearchResource.PARAM_CLUSTERNAME, this.esClustername,
				ElasticsearchResource.PARAM_INDEX, this.esIndex,
				ElasticsearchResource.PARAM_PORT, this.esPort,
				ElasticsearchResource.PARAM_DOCUMENT_MAPPING_FILE, "desc/elasticsearch_mapping_document.json");
		AnalysisEngineDescription esWriter = AnalysisEngineFactory.createEngineDescription(
				ElasticsearchDocumentWriter.class,
				ElasticsearchDocumentWriter.RESOURCE_ESCLIENT, esResource
				);

		AnalysisEngineDescription ldPipeline = AnalysisEngineFactory.createEngineDescription(	
				langDetect,
				esWriter
				);
		
		CpeBuilder ldCpeBuilder = new CpeBuilder();
		ldCpeBuilder.setReader(csvReader);
		ldCpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
		ldCpeBuilder.setAnalysisEngine(ldPipeline);
		NewsleakStatusCallbackListener statusListener = new NewsleakStatusCallbackListener(this.logger);
		ldCpeBuilder.createCpe(statusListener).process();
	}

	public void pipelineAnnotation() throws Exception {
		// reader
		CollectionReaderDescription csvReader = CollectionReaderFactory.createReaderDescription(
				NewsleakCsvStreamReader.class, this.typeSystem,
				NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, this.documentFile,
				NewsleakCsvStreamReader.PARAM_METADATA_FILE, this.metadataFile,
				NewsleakCsvStreamReader.PARAM_INPUTDIR, this.dataDirectory,
				NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, this.defaultLanguage
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
				HeidelTimeOpenNLP.class
				);


		// ner
		ExternalResourceDescription resourceNer = ExternalResourceFactory.createExternalResourceDescription(
				TokenNameFinderModelResourceImpl.class, new File("./resources/eng/en-ner-person.bin"));
		AnalysisEngineDescription ner = AnalysisEngineFactory.createEngineDescription(
				NameFinder.class,
				UimaUtil.MODEL_PARAMETER, resourceNer,
				UimaUtil.SENTENCE_TYPE_PARAMETER, Sentence.class,
				UimaUtil.TOKEN_TYPE_PARAMETER, Token.class,
				NameFinder.NAME_TYPE_PARAMETER, "opennlp.uima.Person"
				);

		// writer
		ExternalResourceDescription resourceLinewriter = ExternalResourceFactory.createExternalResourceDescription(
				TextLineWriterResource.class, 
				TextLineWriterResource.PARAM_OUTPUT_FILE, this.dataDirectory + File.separator + "output.txt");
		AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
				TextLineWriter.class,
				TextLineWriter.RESOURCE_LINEWRITER, resourceLinewriter
				);

		AnalysisEngineDescription xmi = AnalysisEngineFactory.createEngineDescription(
				XmiWriter.class,
				XmiWriter.PARAM_OUTPUT_DIRECTORY, this.dataDirectory + File.separator + "xmi"
				);

		AnalysisEngineDescription pipeline = AnalysisEngineFactory.createEngineDescription(
				sentence,
				token,
				pos,
				heideltime,
				ner, 
				writer
				// xmi,
				// esWriter
				);

		CpeBuilder cpeBuilder = new CpeBuilder();
		cpeBuilder.setReader(csvReader);
		cpeBuilder.setMaxProcessingUnitThreadCount(this.threads);
		cpeBuilder.setAnalysisEngine(pipeline);

		// run processing
		StatusCallbackListener statusListener = new NewsleakStatusCallbackListener(this.logger);
		CollectionProcessingEngine engine = cpeBuilder.createCpe(statusListener);
		engine.process();

	}

}
