package uhh_lt.newsleak.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.SharedResourceObject;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import opennlp.tools.cmdline.langdetect.LanguageDetectorModelLoader;
import opennlp.tools.langdetect.LanguageDetectorModel;

public class LanguageDetectorResource extends Resource_ImplBase {
	
	public static final String PARAM_METADATA_FILE = "mMetadata";
	@ConfigurationParameter(name = PARAM_METADATA_FILE)
	private String mMetadata;
	
	public static final String PARAM_MODEL_FILE = "mModelfile";
	@ConfigurationParameter(name = PARAM_MODEL_FILE)
	private String mModelfile;

	private File metadataFile;
	
	private LanguageDetectorModel model;
	
//	public LanguageDetectorResource() {
//		super();
//		
//	}
//
//	public void load(DataResource modelfile) throws ResourceInitializationException {
//		
//	}

	public LanguageDetectorModel getModel() {
		return model;
	}
	

	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		model = new LanguageDetectorModelLoader().load(new File(mModelfile));
		metadataFile = new File(mMetadata);
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



}
