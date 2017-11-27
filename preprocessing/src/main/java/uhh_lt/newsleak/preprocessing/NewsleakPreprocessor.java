package uhh_lt.newsleak.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.examples.experiment.pos.XmiWriter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;

import annotator.HeidelTimeOpenNLP;
import annotator.LanguageDetector;
import de.tu.darmstadt.lt.ner.annotator.NERAnnotator;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.annotator.heideltime.resources.GenericResourceManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import opennlp.tools.namefind.NameFinderME;
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
import reader.NewsleakCsvStreamReader;
import resources.LanguageDetectorResource;
import writer.TextLineWriter;

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
	private Integer threads;
	

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
		TypeSystemDescription ts = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(typeSystemFile);

		// reader
		CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
				NewsleakCsvStreamReader.class, ts,
				NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, np.documentFile,
				NewsleakCsvStreamReader.PARAM_METADATA_FILE, np.metadataFile,
				NewsleakCsvStreamReader.PARAM_INPUTDIR, np.dataDirectory,
				NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, np.defaultLanguage
		);
		
		// language detection
		ExternalResourceDescription resourceLangDect = ExternalResourceFactory.createExternalResourceDescription(
				LanguageDetectorResource.class, new File("resources/langdetect-183.bin"));
		AnalysisEngineDescription langDetect = AnalysisEngineFactory.createEngineDescription(
				LanguageDetector.class,
				LanguageDetector.MODEL_FILE, resourceLangDect
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
//		Path descriptorFilePath = Paths.get("desc", "Heideltime_annotator.xml");
//		XMLInputSource xmlInputSource = new XMLInputSource(descriptorFilePath.toFile());
//		AnalysisEngineDescription heideltime = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(xmlInputSource);
		
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
		AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
				TextLineWriter.class,
				TextLineWriter.PARAM_OUTPUT_FILE_NAME, np.dataDirectory + File.separator + "output.txt"
		);
		
		AnalysisEngineDescription xmi = AnalysisEngineFactory.createEngineDescription(
				XmiWriter.class,
				XmiWriter.PARAM_OUTPUT_DIRECTORY, np.dataDirectory + File.separator + "xmi"
		);
		
		AnalysisEngineDescription pipeline = AnalysisEngineFactory.createEngineDescription(	
				langDetect,
				sentence,
				token,
				pos,
				heideltime,
				ner, 
				writer,
				xmi
		);


		CpeBuilder cpeBuilder = new CpeBuilder();
		cpeBuilder.setReader(reader);
		cpeBuilder.setMaxProcessingUnitThreadCount(np.threads);
		cpeBuilder.setAnalysisEngine(pipeline);

		NewsleakStatusCallbackListener statusListener = new NewsleakStatusCallbackListener(np.logger);
		CollectionProcessingEngine engine = cpeBuilder.createCpe(statusListener);
		engine.process();


	}

}
