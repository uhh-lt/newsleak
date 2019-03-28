package uhh_lt.newsleak.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

/**
 * Provides shared functionality and data for the @see
 * uhh_lt.newsleak.writer.TextLineWriter. It allows for synchronzied writing of
 * text by any annotator class in a given output file.
 * 
 * This writer is used for debug purposes only.
 */
public class TextLineWriterResource extends Resource_ImplBase {

	/** The Constant PARAM_OUTPUT_FILE. */
	public static final String PARAM_OUTPUT_FILE = "outputFile";

	/** The outfile. */
	@ConfigurationParameter(name = PARAM_OUTPUT_FILE, mandatory = true, description = "Output dir for writing")
	private File outfile;

	/**
	 * Append text to the output file.
	 *
	 * @param text
	 *            the text
	 */
	public synchronized void append(String text) {
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(outfile, true);
			fileWriter.write(text + "\n");
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
