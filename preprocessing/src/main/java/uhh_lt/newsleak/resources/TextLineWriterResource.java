package uhh_lt.newsleak.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

public class TextLineWriterResource extends Resource_ImplBase {
	
	public static final String PARAM_OUTPUT_FILE = "outputFile";
	@ConfigurationParameter(
			name = PARAM_OUTPUT_FILE, 
			mandatory = true,
			description = "Output dir for writing")
	private 	File outfile;

	
	public synchronized void append(String text) {
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(outfile, true);
			fileWriter.write(text + "\n");
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
