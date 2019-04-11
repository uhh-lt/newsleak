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
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Logger;

import uhh_lt.newsleak.resources.ElasticsearchResource;
import uhh_lt.newsleak.resources.MetadataResource;

/**
 * Abstract class to provide common functionality for each newsleak data reader
 * (e.g. processing the preprocessing configuration file, initialization of the
 * postgres database connection, and the metadata UIMA resource initialization).
 *
 */
public abstract class NewsleakPreprocessor {

	/** The logger. */
	protected Logger logger;

	/** command line options */
	private Options cliOptions;
	private String configfile;
	protected String configDir;

	/** config file options */
	protected String readerType;

	// processing parameters
	protected String defaultLanguage;
	protected String[] processLanguages;
	protected String dataDirectory;
	protected boolean paragraphsAsDocuments;
	protected Integer paragraphMinimumLength;
	protected Integer maxDocumentLength;
	protected Integer threads;
	protected Integer debugMaxDocuments;
	
	// csv externally preprocessed data
	protected String documentFile;
	protected String metadataFile;
	
	// newsleak elasticsearch configuration 
	protected String esHost;
	protected String esClustername;
	protected String esIndex;
	protected String esPort;
	
	// hoover elasticsearch configuration
	protected String hooverHost;
	protected String hooverClustername;
	protected String hooverIndex;
	protected String hooverPort;
	protected String hooverTmpMetadata;
	protected String hooverSearchUrl;

	// newsleak postgres configuration
	protected String dbUrl;
	protected String dbName;
	protected String dbUser;
	protected String dbPass;
	protected String dbSchema;
	protected String dbIndices;

	// newsleak-ner microservice configuration
	protected String nerServiceUrl;
	
	// dictionary and pattern extraction
	protected String dictionaryFiles;
	protected boolean patternEmail;
	protected boolean patternUrl;
	protected boolean patternPhone;
	protected boolean patternIP;

	// UIMA configuration variables
	protected TypeSystemDescription typeSystem;
	protected NewsleakStatusCallbackListener statusListener;
	protected ExternalResourceDescription metadataResourceDesc = null;

	// postgres connection
	protected static Connection conn;
	protected static Statement st;

	/**
	 * Instantiates a new newsleak preprocessor.
	 */
	public NewsleakPreprocessor() {
		super();
		logger = UIMAFramework.getLogger();
	}

	/**
	 * Reads the configuration from a config file.
	 *
	 * @param cliArgs the cli args
	 * @return the configuration
	 */
	public void getConfiguration(String[] cliArgs) {
		this.getCliOptions(cliArgs);

		// config file
		Properties prop = new Properties();
		try {
			this.configDir = new File(configfile).getParentFile().getAbsolutePath();
			InputStream input = new FileInputStream(configfile);
			prop.load(input);

			readerType = prop.getProperty("datareader");

			defaultLanguage = prop.getProperty("defaultlanguage");
			processLanguages = prop.getProperty("processlanguages").split("[, ]+");
			dataDirectory = prop.getProperty("datadirectory");
			documentFile = prop.getProperty("documentfile");
			metadataFile = prop.getProperty("metadatafile");

			hooverHost = prop.getProperty("hooverurl");
			hooverClustername = prop.getProperty("hooverclustername");
			hooverIndex = prop.getProperty("hooverindex");
			hooverPort = prop.getProperty("hooverport");
			hooverTmpMetadata = prop.getProperty("hoovertmpmetadata");
			hooverSearchUrl = prop.getProperty("hooversearchurl");

			esHost = prop.getProperty("esurl");
			esClustername = prop.getProperty("esclustername");
			esIndex = prop.getProperty("esindex");
			esPort = prop.getProperty("esport");
			paragraphsAsDocuments = Boolean.parseBoolean(prop.getProperty("paragraphsasdocuments"));
			paragraphMinimumLength = Integer.valueOf(prop.getProperty("paragraphminimumlength"));
			maxDocumentLength = Integer.valueOf(prop.getProperty("maxdocumentlength"));
			if (maxDocumentLength <= 0)
				maxDocumentLength = Integer.MAX_VALUE;

			dbUrl = prop.getProperty("dburl");
			dbName = prop.getProperty("dbname");
			dbUser = prop.getProperty("dbuser");
			dbPass = prop.getProperty("dbpass");
			dbSchema = prop.getProperty("dbschema");
			dbIndices = prop.getProperty("dbindices");

			nerServiceUrl = prop.getProperty("nerserviceurl");
			
			dictionaryFiles = prop.getProperty("dictionaryfiles");
			patternEmail = Boolean.parseBoolean(prop.getProperty("patternemail", "true"));
			patternUrl = Boolean.parseBoolean(prop.getProperty("patternurl", "false"));
			patternPhone = Boolean.parseBoolean(prop.getProperty("patternphone", "false"));
			patternIP = Boolean.parseBoolean(prop.getProperty("patternip", "false"));

			threads = Integer.valueOf(prop.getProperty("threads"));
			debugMaxDocuments = Integer.valueOf(prop.getProperty("debugMaxDocuments"));
			if (debugMaxDocuments <= 0)
				debugMaxDocuments = null;

			input.close();
		} catch (IOException e) {
			System.err.println("Could not read configuration file " + configfile);
			System.exit(1);
		}

		// uima type system
		String typeSystemFile = new File("desc/NewsleakDocument.xml").getAbsolutePath();
		this.typeSystem = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(typeSystemFile);
	}

	/**
	 * Gets the cli options.
	 *
	 * @param args the args
	 * @return the cli options
	 */
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

	/**
	 * Initalizes the postgres db.
	 *
	 * @param dbName the db name
	 * @param ip the ip
	 * @param user the user
	 * @param pswd the pswd
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws ClassNotFoundException the class not found exception
	 * @throws SQLException the SQL exception
	 */
	protected void initDb(String dbName, String ip, String user, String pswd)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String url = "jdbc:postgresql://" + ip + "/";
		String userName = user;
		String password = pswd;
		conn = DriverManager.getConnection(url + dbName, userName, password);
		st = conn.createStatement();
	}

	/**
	 * Gets the metadata resource description.
	 *
	 * @return the metadata resource description
	 */
	protected ExternalResourceDescription getMetadataResourceDescription() {
		if (metadataResourceDesc == null) {
			metadataResourceDesc = ExternalResourceFactory.createExternalResourceDescription(MetadataResource.class,
					MetadataResource.PARAM_METADATA_FILE, this.dataDirectory + File.separator + this.metadataFile,
					MetadataResource.PARAM_RESET_METADATA_FILE, "true");
		}
		return metadataResourceDesc;
	}
	
	
	/**
	 * Gets the elasticsearch resource description.
	 *
	 * @param createNewIndex Should be "true" or "false". If "true", the index will be newly created (a pre-existing index with the same name will be overwritten)
	 * @return the metadata resource description
	 */
	protected ExternalResourceDescription getElasticsearchResourceDescription(String createNewIndex) {
		ExternalResourceDescription esResource = ExternalResourceFactory.createExternalResourceDescription(
				ElasticsearchResource.class, ElasticsearchResource.PARAM_CREATE_INDEX, createNewIndex,
				ElasticsearchResource.PARAM_CLUSTERNAME, this.esClustername, ElasticsearchResource.PARAM_INDEX,
				this.esIndex, ElasticsearchResource.PARAM_HOST, this.esHost, ElasticsearchResource.PARAM_PORT,
				this.esPort, ElasticsearchResource.PARAM_DOCUMENT_MAPPING_FILE,
				"desc/elasticsearch_mapping_document_2.4.json",
				ElasticsearchResource.PARAM_METADATA_FILE, this.dataDirectory + File.separator + this.metadataFile);
		return esResource;
	}

}
