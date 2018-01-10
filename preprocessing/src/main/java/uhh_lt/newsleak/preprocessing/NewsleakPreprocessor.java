package uhh_lt.newsleak.preprocessing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
import uhh_lt.newsleak.reader.NewsleakElasticsearchReader;
import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.LanguageDetectorResource;
import uhh_lt.newsleak.resources.PostgresResource;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.writer.ElasticsearchDocumentWriter;
import uhh_lt.newsleak.writer.PostgresDbWriter;
import uhh_lt.newsleak.writer.TextLineWriter;

/**
 * Reads document.csv and metadata.csv, processes them in a UIMA pipeline
 * and writes output to an ElasticSearch index.
 *
 */
public abstract class NewsleakPreprocessor 
{

	protected Logger logger;

	private Options cliOptions;
	private String configfile;
	
	protected String readerType;

	protected String defaultLanguage;
	protected String dataDirectory;
	protected String documentFile;
	protected String metadataFile;

	protected String esHost;
	protected String esClustername;
	protected String esIndex;
	protected String esPort;
	
	protected String hooverHost;
	protected String hooverClustername;
	protected String hooverIndex;
	protected String hooverPort;
	protected String hooverTmpMetadata;
	
	protected String dbUrl;
	protected String dbName;
	protected String dbUser;
	protected String dbPass;
	protected String dbSchema;
	protected String dbIndices;

	protected Integer threads;
	protected Integer debugMaxDocuments;

	protected TypeSystemDescription typeSystem;
	protected NewsleakStatusCallbackListener statusListener;
	
	protected static Connection conn;
	protected static Statement st;

	public NewsleakPreprocessor() {
		super();
		logger = UIMAFramework.getLogger();
	}


	public void getConfiguration(String[] cliArgs) {
		this.getCliOptions(cliArgs);
		
		// config file
		Properties prop = new Properties();
		try {
			InputStream input = new FileInputStream(configfile);
			prop.load(input);
			
			readerType = prop.getProperty("datareader");

			defaultLanguage = prop.getProperty("lang");
			dataDirectory = prop.getProperty("datadirectory");
			documentFile = prop.getProperty("documentfile");
			metadataFile = prop.getProperty("metadatafile");
			
			hooverHost = prop.getProperty("hooverurl");
			hooverClustername = prop.getProperty("hooverclustername");
			hooverIndex = prop.getProperty("hooverindex");
			hooverPort = prop.getProperty("hooverport");
			hooverTmpMetadata = prop.getProperty("hoovertmpmetadata");
			
			esHost = prop.getProperty("esurl");
			esClustername = prop.getProperty("esclustername");
			esIndex = prop.getProperty("esindex");
			esPort = prop.getProperty("esport");
			
			dbUrl = prop.getProperty("dburl");
			dbName = prop.getProperty("dbname");
			dbUser = prop.getProperty("dbuser");
			dbPass = prop.getProperty("dbpass");
			dbSchema = prop.getProperty("dbschema");
			dbIndices = prop.getProperty("dbindices");

			threads = Integer.valueOf(prop.getProperty("threads"));
			debugMaxDocuments = Integer.valueOf(prop.getProperty("debugMaxDocuments"));
			if (debugMaxDocuments <= 0) debugMaxDocuments = null;
			input.close();
		}
		catch (IOException e) {
			System.err.println("Could not read configuration file " + configfile);
			System.exit(1);
		}
		
		// uima type system
		String typeSystemFile = new File("desc/NewsleakDocument.xml").getAbsolutePath();	
		this.typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(typeSystemFile);
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

	
	protected void initDb(String dbName, String ip, String user, String pswd)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String url = "jdbc:postgresql://" + ip + "/";
		String userName = user;
		String password = pswd;
		conn = DriverManager.getConnection(url + dbName, userName, password);
		st = conn.createStatement();
	}

}
