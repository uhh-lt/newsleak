package uhh_lt.newsleak.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class MetadataResource extends Resource_ImplBase {

	private Logger logger;

	public static final String PARAM_METADATA_FILE = "mMetadata";
	@ConfigurationParameter(name = PARAM_METADATA_FILE)
	private String mMetadata;
	
	public static final String PARAM_RESET_METADATA_FILE = "resetMetadata";
	@ConfigurationParameter(name = PARAM_RESET_METADATA_FILE)
	private boolean resetMetadata;

	private File metadataFile;

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();
		metadataFile = new File(mMetadata);
		
		if (resetMetadata) {
			try {
				// reset metadata file
				new FileOutputStream(metadataFile).close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return true;
	}



	public synchronized void appendMetadata(List<List<String>> metadata) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile, true));
			CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.RFC4180);
			csvPrinter.printRecords(metadata);
			csvPrinter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	
	public ArrayList<String> createTextMetadata(String docId, String key, String value) {
		ArrayList<String> meta = new ArrayList<String>();
		meta.add(docId);
		meta.add(StringUtils.capitalize(key));
		meta.add(value.replaceAll("\\r|\\n", " "));
		meta.add("Text");
		return meta;
	}

}
