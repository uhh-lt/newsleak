package uhh_lt.newsleak.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.reader.HooverElasticsearchReader and @see
 * uhh_lt.newsleak.reader.HooverElasticsearchApiReader. Metadata is written
 * temporarily to disk in CSV format for later import into the newsleak postgres
 * database.
 */
public class MetadataResource extends Resource_ImplBase {

	/** The Constant PARAM_METADATA_FILE. */
	public static final String PARAM_METADATA_FILE = "mMetadata";

	/** The m metadata. */
	@ConfigurationParameter(name = PARAM_METADATA_FILE)
	private String mMetadata;

	/** The Constant PARAM_RESET_METADATA_FILE. */
	public static final String PARAM_RESET_METADATA_FILE = "resetMetadata";

	/** The reset metadata. */
	@ConfigurationParameter(name = PARAM_RESET_METADATA_FILE)
	private boolean resetMetadata;

	/** The metadata file. */
	private File metadataFile;

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

	/**
	 * Append a list of metadata entries for one document to the temporary metadata
	 * file.
	 *
	 * @param metadata
	 *            the metadata
	 */
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

	/**
	 * Create a metadata entry with type text.
	 *
	 * @param docId
	 *            the doc id
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @return the array list
	 */
	public ArrayList<String> createTextMetadata(String docId, String key, String value) {
		ArrayList<String> meta = new ArrayList<String>();
		meta.add(docId);
		meta.add(StringUtils.capitalize(key));
		meta.add(value.replaceAll("\\r|\\n", " "));
		meta.add("Text");
		return meta;
	}

}
