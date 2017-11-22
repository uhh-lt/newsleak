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
import org.apache.uima.fit.cpe.CpeBuilder;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Logger;

import annotator.LanguageDetector;
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

		
		CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
				NewsleakCsvStreamReader.class, ts,
				NewsleakCsvStreamReader.PARAM_DOCUMENT_FILE, np.documentFile,
				NewsleakCsvStreamReader.PARAM_METADATA_FILE, np.metadataFile,
				NewsleakCsvStreamReader.PARAM_INPUTDIR, np.dataDirectory,
				NewsleakCsvStreamReader.PARAM_DEFAULT_LANG, np.defaultLanguage
		);
		
		ExternalResourceDescription extDesc = ExternalResourceFactory.createExternalResourceDescription(
				LanguageDetectorResource.class, new File("resources/langdetect-183.bin"));
		
		AnalysisEngineDescription langDetect = AnalysisEngineFactory.createEngineDescription(
				LanguageDetector.class,
				LanguageDetector.MODEL_FILE, extDesc
		);
		
		AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
				TextLineWriter.class,
				TextLineWriter.PARAM_OUTPUT_FILE_NAME, np.dataDirectory + File.separator + "output.txt"
		);
		
		AnalysisEngineDescription pipeline = AnalysisEngineFactory.createEngineDescription(	
				langDetect,
				writer
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
