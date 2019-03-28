package uhh_lt.newsleak.resources;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.writer.PostgresDbWriter. A shared client is used to
 * insert/update entries for each document as queried by the writer. For this,
 * the class uses prepared insert and upsert statements.
 */
public class PostgresResource extends Resource_ImplBase {

	/** The logger. */
	private Logger logger;

	/** The Constant TABLE_DOCUMENT. */
	public static final String TABLE_DOCUMENT = "document";

	/** The Constant TABLE_METADATA. */
	public static final String TABLE_METADATA = "metadata";

	/** The Constant TABLE_ENTITY. */
	public static final String TABLE_ENTITY = "entity";

	/** The Constant TABLE_ENTITYOFFSET. */
	public static final String TABLE_ENTITYOFFSET = "entityoffset";

	/** The Constant TABLE_EVENTTIME. */
	public static final String TABLE_EVENTTIME = "eventtime";

	/** The Constant TABLE_KEYTERMS. */
	public static final String TABLE_KEYTERMS = "terms";

	/** The Constant PARAM_DBNAME. */
	public static final String PARAM_DBNAME = "dbName";

	/** The db name. */
	@ConfigurationParameter(name = PARAM_DBNAME)
	private String dbName;

	/** The Constant PARAM_DBUSER. */
	public static final String PARAM_DBUSER = "dbUser";

	/** The db user. */
	@ConfigurationParameter(name = PARAM_DBUSER)
	private String dbUser;

	/** The Constant PARAM_DBURL. */
	public static final String PARAM_DBURL = "dbUrl";

	/** The db url. */
	@ConfigurationParameter(name = PARAM_DBURL)
	private String dbUrl;

	/** The Constant PARAM_DBPASS. */
	public static final String PARAM_DBPASS = "dbPass";

	/** The db pass. */
	@ConfigurationParameter(name = PARAM_DBPASS)
	private String dbPass;

	/** The Constant PARAM_INDEX_SCHEMA. */
	public static final String PARAM_INDEX_SCHEMA = "indexSqlFile";

	/** The index sql file. */
	@ConfigurationParameter(name = PARAM_INDEX_SCHEMA)
	private String indexSqlFile;

	/** The Constant PARAM_TABLE_SCHEMA. */
	public static final String PARAM_TABLE_SCHEMA = "tableSchemaFile";

	/** The table schema file. */
	@ConfigurationParameter(name = PARAM_TABLE_SCHEMA)
	private String tableSchemaFile;

	/** The Constant PARAM_CREATE_DB. */
	public final static String PARAM_CREATE_DB = "createDb";

	/** The create db. */
	@ConfigurationParameter(name = PARAM_CREATE_DB, mandatory = false, defaultValue = "false", description = "If true, an new db will be created (existing db will be removed).")
	private boolean createDb;

	/** The db connection. */
	private Connection dbConnection;

	/** The db statement. */
	private Statement dbStatement;

	/** The prepared statement document. */
	private PreparedStatement preparedStatementDocument;

	/** The prepared statement entity upsert. */
	private PreparedStatement preparedStatementEntityUpsert;

	/** The prepared statement entityoffset. */
	private PreparedStatement preparedStatementEntityoffset;

	/** The prepared statement eventtime. */
	private PreparedStatement preparedStatementEventtime;

	/** The prepared statement keyterms. */
	private PreparedStatement preparedStatementKeyterms;

	/** The document counter. */
	private int documentCounter = 0;

	/** The internal batch size. */
	private int INTERNAL_BATCH_SIZE = 100;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.Resource_ImplBase#initialize(org.apache.uima.
	 * resource.ResourceSpecifier, java.util.Map)
	 */
	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();

		// init db
		try {
			if (createDb) {
				createDb(dbUrl, dbName, dbUser, dbPass);
				logger.log(Level.INFO, "DB " + dbName + " created");
			} else {
				initDb(dbUrl, dbName, dbUser, dbPass);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		prepareStatements();

		return true;
	}

	/**
	 * Gets the db statement.
	 *
	 * @return the db statement
	 */
	public Statement getDbStatement() {
		return dbStatement;
	}

	/**
	 * Execute insert.
	 *
	 * @param sql
	 *            the sql
	 * @return true, if successful
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized boolean executeInsert(String sql) throws SQLException {
		return dbStatement.execute(sql);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.resource.Resource_ImplBase#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
		try {
			dbConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the db.
	 *
	 * @param dbUrl
	 *            the db url
	 * @param dbName
	 *            the db name
	 * @param dbUser
	 *            the db user
	 * @param dbPass
	 *            the db pass
	 * @throws Exception
	 *             the exception
	 */
	private void createDb(String dbUrl, String dbName, String dbUser, String dbPass) throws Exception {

		Connection rootConnection = DriverManager.getConnection("jdbc:postgresql://" + dbUrl + "/postgres", dbUser,
				dbPass);
		Statement dbStatement = rootConnection.createStatement();
		dbStatement.executeUpdate("DROP DATABASE IF EXISTS " + dbName + ";");

		dbStatement.executeUpdate("CREATE DATABASE " + dbName
				+ " WITH ENCODING='UTF8' LC_CTYPE='en_US.UTF-8' LC_COLLATE='en_US.UTF-8'"
				+ " TEMPLATE=template0 CONNECTION LIMIT=-1; GRANT ALL ON DATABASE " + dbName + " TO " + dbUser + ";");

		initDb(dbUrl, dbName, dbUser, dbPass);

		/**
		 * create SQL schema (SQL indexes will be created later by @see
		 * uhh_lt.newsleak.preprocessing.InformationExtraction2Postgres)
		 */
		createSchema(tableSchemaFile);
	}

	/**
	 * Inits the db.
	 *
	 * @param dbUrl
	 *            the db url
	 * @param dbName
	 *            the db name
	 * @param dbUser
	 *            the db user
	 * @param dbPass
	 *            the db pass
	 * @throws InstantiationException
	 *             the instantiation exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void initDb(String dbUrl, String dbName, String dbUser, String dbPass)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String url = "jdbc:postgresql://" + dbUrl + "/";
		dbConnection = DriverManager.getConnection(url + dbName, dbUser, dbPass);
		dbStatement = dbConnection.createStatement();
		dbConnection.setAutoCommit(false);
	}

	/**
	 * Commit.
	 */
	public void commit() {
		try {
			dbConnection.commit();
			logger.log(Level.INFO,
					"Another " + INTERNAL_BATCH_SIZE + " documents committed (total: " + documentCounter + ")");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the schema.
	 *
	 * @param tableSchemaFile
	 *            the table schema file
	 */
	private void createSchema(String tableSchemaFile) {
		try {
			String schemaSql = FileUtils.readFileToString(new File(tableSchemaFile)).replace("\n", " ");
			dbStatement.executeUpdate(schemaSql);
			logger.log(Level.INFO, "Schema created");
		} catch (IOException e1) {
			logger.log(Level.SEVERE, "Could not read DB schema file " + tableSchemaFile);
			System.exit(1);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Could create DB schema.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Prepare statements.
	 */
	private void prepareStatements() {
		try {
			preparedStatementDocument = dbConnection
					.prepareStatement("INSERT INTO " + TABLE_DOCUMENT + " (id, content, created) VALUES (?, ?, ?)");
			preparedStatementEntityUpsert = dbConnection
					.prepareStatement("INSERT INTO " + TABLE_ENTITY + " as e (name, type, frequency) VALUES (?, ?, ?) "
							+ "ON CONFLICT ON CONSTRAINT unique_name_type DO "
							+ "UPDATE SET frequency = e.frequency + ? " + "RETURNING id");
			preparedStatementEntityoffset = dbConnection.prepareStatement("INSERT INTO " + TABLE_ENTITYOFFSET
					+ " (docid, entid, entitystart, entityend) VALUES (?, ?, ?, ?)");
			preparedStatementEventtime = dbConnection.prepareStatement("INSERT INTO " + TABLE_EVENTTIME
					+ " (docid, beginoffset, endoffset, timex, type, timexvalue) VALUES (?, ?, ?, ?, ?, ?)");
			preparedStatementKeyterms = dbConnection
					.prepareStatement("INSERT INTO " + TABLE_KEYTERMS + " (docid, term, frequency) VALUES (?, ?, ?)");
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Insert document.
	 *
	 * @param id
	 *            the id
	 * @param content
	 *            the content
	 * @param created
	 *            the created
	 * @return true, if successful
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized boolean insertDocument(Integer id, String content, String created) throws SQLException {

		documentCounter++;

		preparedStatementDocument.setInt(1, id);
		preparedStatementDocument.setString(2, content.replaceAll("\u0000", ""));
		preparedStatementDocument.setDate(3, Date.valueOf(created));
		return preparedStatementDocument.execute();
	}

	/**
	 * Insert entity.
	 *
	 * @param name
	 *            the name
	 * @param type
	 *            the type
	 * @param frequency
	 *            the frequency
	 * @return the integer
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized Integer insertEntity(String name, String type, Integer frequency) throws SQLException {
		Integer entityId;
		preparedStatementEntityUpsert.setString(1, name.replaceAll("\u0000", ""));
		preparedStatementEntityUpsert.setString(2, type);
		preparedStatementEntityUpsert.setInt(3, frequency);
		preparedStatementEntityUpsert.setInt(4, frequency);
		ResultSet rs = preparedStatementEntityUpsert.executeQuery();
		rs.next();
		entityId = rs.getInt(1);
		return entityId;
	}

	/**
	 * Insert entityoffset.
	 *
	 * @param docid
	 *            the docid
	 * @param entid
	 *            the entid
	 * @param entitystart
	 *            the entitystart
	 * @param entityend
	 *            the entityend
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized void insertEntityoffset(Integer docid, Integer entid, Integer entitystart, Integer entityend)
			throws SQLException {
		preparedStatementEntityoffset.setInt(1, docid);
		preparedStatementEntityoffset.setInt(2, entid);
		preparedStatementEntityoffset.setInt(3, entitystart);
		preparedStatementEntityoffset.setInt(4, entityend);
		preparedStatementEntityoffset.addBatch();
	}

	/**
	 * Insert eventtime.
	 *
	 * @param docid
	 *            the docid
	 * @param beginoffset
	 *            the beginoffset
	 * @param endoffset
	 *            the endoffset
	 * @param timex
	 *            the timex
	 * @param type
	 *            the type
	 * @param timexvalue
	 *            the timexvalue
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized void insertEventtime(Integer docid, Integer beginoffset, Integer endoffset, String timex,
			String type, String timexvalue) throws SQLException {
		preparedStatementEventtime.setInt(1, docid);
		preparedStatementEventtime.setInt(2, beginoffset);
		preparedStatementEventtime.setInt(3, endoffset);
		preparedStatementEventtime.setString(4, timex.replaceAll("\u0000", ""));
		preparedStatementEventtime.setString(5, type);
		preparedStatementEventtime.setString(6, timexvalue.replaceAll("\u0000", ""));
		preparedStatementEventtime.addBatch();
	}

	/**
	 * Insert keyterms.
	 *
	 * @param docid
	 *            the docid
	 * @param term
	 *            the term
	 * @param frequency
	 *            the frequency
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized void insertKeyterms(Integer docid, String term, Integer frequency) throws SQLException {
		preparedStatementKeyterms.setInt(1, docid);
		preparedStatementKeyterms.setString(2, term.replaceAll("\u0000", ""));
		preparedStatementKeyterms.setInt(3, frequency);
		preparedStatementKeyterms.addBatch();
	}

	/**
	 * Execute batches.
	 *
	 * @throws SQLException
	 *             the SQL exception
	 */
	public synchronized void executeBatches() throws SQLException {
		preparedStatementEntityoffset.executeBatch();
		preparedStatementEntityoffset.clearBatch();

		preparedStatementEventtime.executeBatch();
		preparedStatementEventtime.clearBatch();

		preparedStatementKeyterms.executeBatch();
		preparedStatementKeyterms.clearBatch();

		if (documentCounter % INTERNAL_BATCH_SIZE == 0) {
			this.commit();
		}
	}

}
