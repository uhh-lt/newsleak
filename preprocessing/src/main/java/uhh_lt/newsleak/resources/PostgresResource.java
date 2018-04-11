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

public class PostgresResource extends Resource_ImplBase {

	private Logger logger;

	public static final String TABLE_DOCUMENT = "document";
	public static final String TABLE_METADATA = "metadata";
	public static final String TABLE_ENTITY = "entity";
	public static final String TABLE_ENTITYOFFSET = "entityoffset";
	public static final String TABLE_EVENTTIME = "eventtime";
	public static final String TABLE_KEYTERMS = "terms";

	public static final String PARAM_DBNAME = "dbName";
	@ConfigurationParameter(name = PARAM_DBNAME)
	private String dbName;
	public static final String PARAM_DBUSER = "dbUser";
	@ConfigurationParameter(name = PARAM_DBUSER)
	private String dbUser;
	public static final String PARAM_DBURL = "dbUrl";
	@ConfigurationParameter(name = PARAM_DBURL)
	private String dbUrl;
	public static final String PARAM_DBPASS = "dbPass";
	@ConfigurationParameter(name = PARAM_DBPASS)
	private String dbPass;
	public static final String PARAM_INDEX_SCHEMA = "indexSqlFile";
	@ConfigurationParameter(name = PARAM_INDEX_SCHEMA)
	private String indexSqlFile;
	public static final String PARAM_TABLE_SCHEMA = "tableSchemaFile";
	@ConfigurationParameter(name = PARAM_TABLE_SCHEMA)
	private String tableSchemaFile;
	public final static String PARAM_CREATE_DB = "createDb";
	@ConfigurationParameter(
			name = PARAM_CREATE_DB, 
			mandatory = false,
			defaultValue = "false",
			description = "If true, an new db will be created (existing db will be removed).")
	private boolean createDb;

	private Connection dbConnection;
	private Statement dbStatement;
	
	private PreparedStatement preparedStatementDocument;
	// private PreparedStatement preparedStatementMetadata;
	private PreparedStatement preparedStatementEntityUpsert;
	// private PreparedStatement preparedStatementEntitySelect;
	// private PreparedStatement preparedStatementEntityUpdate;
	private PreparedStatement preparedStatementEntityoffset;
	private PreparedStatement preparedStatementEventtime;
	private PreparedStatement preparedStatementKeyterms;
	
	
	private int documentCounter = 0;
	private int INTERNAL_BATCH_SIZE = 100;

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



	public Statement getDbStatement() {
		return dbStatement;
	}

	public synchronized boolean executeInsert(String sql) throws SQLException {
		return dbStatement.execute(sql);
	}

	@Override
	public void destroy() {
		super.destroy();
		try {
			dbConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	private void createDb(String dbUrl, String dbName, String dbUser, String dbPass) throws Exception {

		Connection rootConnection = DriverManager.getConnection("jdbc:postgresql://" + dbUrl + "/postgres", dbUser, dbPass);
		Statement dbStatement = rootConnection.createStatement();
		dbStatement.executeUpdate("DROP DATABASE IF EXISTS " + dbName + ";");

		dbStatement.executeUpdate("CREATE DATABASE " + dbName
				+ " WITH ENCODING='UTF8' LC_CTYPE='en_US.UTF-8' LC_COLLATE='en_US.UTF-8'"
				+ " TEMPLATE=template0 CONNECTION LIMIT=-1; GRANT ALL ON DATABASE "
				+ dbName + " TO " + dbUser + ";");

		initDb(dbUrl, dbName, dbUser, dbPass);
		createSchema(tableSchemaFile);
		// createIndices(indexSqlFile);
	}

	public void initDb(String dbUrl, String dbName, String dbUser, String dbPass)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		String url = "jdbc:postgresql://" + dbUrl + "/";
		dbConnection = DriverManager.getConnection(url + dbName, dbUser, dbPass);
		dbStatement = dbConnection.createStatement();
		dbConnection.setAutoCommit(true);
		// dbConnection.setAutoCommit(false);
	}
	
//	public void commit() {
//		try {
//			dbConnection.commit();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//	}

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
	
	
	
//	private void createIndices(String indexSqlFile) {
//		// create postgres indices
//		try {
//			String indexSql = FileUtils.readFileToString(new File(indexSqlFile)).replace("\n", "");
//			dbStatement.executeUpdate(indexSql);
//			logger.log(Level.INFO, "Index created");
//		} catch (Exception e) {
//			logger.log(Level.SEVERE, "Could not create DB indices.");
//			e.printStackTrace();
//			System.exit(1);
//		}
//	}


	private void prepareStatements() {
		try {
			preparedStatementDocument = dbConnection.prepareStatement("INSERT INTO " + TABLE_DOCUMENT + " (id, content, created) VALUES (?, ?, ?)");
			// preparedStatementMetadata = dbConnection.prepareStatement("INSERT INTO " + TABLE_METADATA + " VALUES (?, ?, ?, ?)");
			preparedStatementEntityUpsert = dbConnection.prepareStatement(
					"INSERT INTO " + TABLE_ENTITY + " as e (name, type, frequency) VALUES (?, ?, ?) "
							+ "ON CONFLICT ON CONSTRAINT unique_name_type DO "
							+ "UPDATE SET frequency = e.frequency + ? "
							+ "RETURNING id"
			);
			// preparedStatementEntitySelect = dbConnection.prepareStatement("SELECT id, frequency FROM " + TABLE_ENTITY + " WHERE name=? AND type=?");
			// preparedStatementEntityUpdate = dbConnection.prepareStatement("UPDATE " + TABLE_ENTITY + " SET frequency=? WHERE id=?");
			preparedStatementEntityoffset = dbConnection.prepareStatement("INSERT INTO " + TABLE_ENTITYOFFSET + " (docid, entid, entitystart, entityend) VALUES (?, ?, ?, ?)");
			preparedStatementEventtime = dbConnection.prepareStatement("INSERT INTO " + TABLE_EVENTTIME + " (docid, beginoffset, endoffset, timex, type, timexvalue) VALUES (?, ?, ?, ?, ?, ?)");
			preparedStatementKeyterms = dbConnection.prepareStatement("INSERT INTO " + TABLE_KEYTERMS + " (docid, term, frequency) VALUES (?, ?, ?)");
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public synchronized boolean insertDocument(Integer id, String content, String created) throws SQLException {
		
		documentCounter++;
		
		preparedStatementDocument.setInt(1, id);
		preparedStatementDocument.setString(2, content);
		preparedStatementDocument.setDate(3, Date.valueOf(created));
		return preparedStatementDocument.execute();
	}
	
	public synchronized Integer insertEntity(String name, String type, Integer frequency) throws SQLException {
		Integer entityId;
//		preparedStatementEntitySelect.setString(1, name);
//		preparedStatementEntitySelect.setString(2, type); 
//		ResultSet rs = preparedStatementEntitySelect.executeQuery();
//		if (rs.next()) {
//			entityId = rs.getInt(1);
//			preparedStatementEntityUpdate.setInt(1, rs.getInt(2) + frequency);
//			preparedStatementEntityUpdate.setInt(2, entityId);
//			preparedStatementEntityUpdate.execute();
//		} else {
//			preparedStatementEntity.setString(1, name);
//			preparedStatementEntity.setString(2, type);
//			preparedStatementEntity.setInt(3, frequency);
//			rs = preparedStatementEntity.executeQuery();
//			rs.next();
//			entityId = rs.getInt(1);
//		}
		preparedStatementEntityUpsert.setString(1, name);
		preparedStatementEntityUpsert.setString(2, type);
		preparedStatementEntityUpsert.setInt(3, frequency);
		preparedStatementEntityUpsert.setInt(4, frequency);
		ResultSet rs = preparedStatementEntityUpsert.executeQuery();
		rs.next();
		entityId = rs.getInt(1);
		return entityId;
	}
	
	public synchronized void insertEntityoffset(Integer docid, Integer entid, Integer entitystart, Integer entityend) throws SQLException {
		preparedStatementEntityoffset.setInt(1, docid);
		preparedStatementEntityoffset.setInt(2, entid);
		preparedStatementEntityoffset.setInt(3, entitystart);
		preparedStatementEntityoffset.setInt(4, entityend);		
		preparedStatementEntityoffset.addBatch();
	}
	
	
	public synchronized void insertEventtime(Integer docid, Integer beginoffset, Integer endoffset, String timex, String type, String timexvalue) throws SQLException {
		preparedStatementEventtime.setInt(1, docid);
		preparedStatementEventtime.setInt(2, beginoffset);
		preparedStatementEventtime.setInt(3, endoffset);
		preparedStatementEventtime.setString(4, timex);
		preparedStatementEventtime.setString(5, type);
		preparedStatementEventtime.setString(6, timexvalue);
		preparedStatementEventtime.addBatch();
	}
	
	public synchronized void insertKeyterms(Integer docid, String term, Integer frequency) throws SQLException {
		preparedStatementKeyterms.setInt(1, docid);
		preparedStatementKeyterms.setString(2, term);
		preparedStatementKeyterms.setInt(3, frequency);
		preparedStatementKeyterms.addBatch();
	}
	
	public synchronized void executeBatches() throws SQLException {
		preparedStatementEntityoffset.executeBatch();
		preparedStatementEntityoffset.clearBatch();
		
		preparedStatementEventtime.executeBatch();
		preparedStatementEventtime.clearBatch();
		
		preparedStatementKeyterms.executeBatch();
		preparedStatementKeyterms.clearBatch();
		
//		if (documentCounter % INTERNAL_BATCH_SIZE == 0) {
//			this.commit();
//		}
	}

}
