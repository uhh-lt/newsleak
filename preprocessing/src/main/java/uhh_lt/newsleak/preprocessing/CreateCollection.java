package uhh_lt.newsleak.preprocessing;

public class CreateCollection {
	
	public static void main(String[] args) throws Exception
	{
		long startTime = System.currentTimeMillis();
		
		InformationExtraction2Postgres.main(args);
		Postgres2ElasticsearchIndexer.main(args);
		
		long estimatedTime = System.currentTimeMillis() - startTime;
		
		System.out.println("Processing time passed (seconds): " + estimatedTime / 1000);
	}

}
