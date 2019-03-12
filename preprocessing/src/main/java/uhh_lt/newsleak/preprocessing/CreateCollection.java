package uhh_lt.newsleak.preprocessing;

/**
 * Main class to create a new newsleak collection. It runs the information
 * extraction pipeline which writes to a relational database. The second step
 * then indexes retrieved entities and metadata from the database for fast
 * restrieval and aggregation to the system's fulltext index (elasticsearch).
 * 
 * For configuration it expects a config file as parsed in @see
 * uhh_lt.newsleak.preprocessing.NewsleakPreprocessor
 */
public class CreateCollection {

	/**
	 * The main method to start the creation process of a new collection.
	 *
	 * @param args
	 *             expects a paramter -c to point to the config file
	 * @throws Exception
	 *             Any exception which may occur...
	 */
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		// extract fulltext, entities and metdadata and write to DB
		InformationExtraction2Postgres.main(args);
		// read from DB and write to fullext index
		Postgres2ElasticsearchIndexer.main(args);

		long estimatedTime = System.currentTimeMillis() - startTime;

		System.out.println("Processing time passed (seconds): " + estimatedTime / 1000);
	}

}
