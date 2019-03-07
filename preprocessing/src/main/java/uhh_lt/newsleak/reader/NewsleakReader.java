package uhh_lt.newsleak.reader;

import java.util.Scanner;

import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

public abstract class NewsleakReader extends CasCollectionReader_ImplBase {
	
	protected Logger logger;
	
	public static final String PARAM_DEBUG_MAX_DOCS = "maxRecords";
	@ConfigurationParameter(name = PARAM_DEBUG_MAX_DOCS, mandatory = false)
	protected Integer maxRecords = Integer.MAX_VALUE;

	public static final String PARAM_MAX_DOC_LENGTH = "maxDocumentLength";
	@ConfigurationParameter(name = PARAM_MAX_DOC_LENGTH, mandatory = false)
	protected Integer maxDocumentLength = Integer.MAX_VALUE; // 1500 * 10000 = 15000000 = 10000 norm pages
	
	public static final int MAXIMUM_EMPTY_LINE_SEQUENCE_LENGTH = 50;

	public String cleanBodyText(String bodyText) {
		
		int origLength = bodyText.length();
		
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(bodyText);
		int emptyLines = 0;
		while (scanner.hasNextLine()) {
		  String line = scanner.nextLine();
		  if (line.trim().isEmpty()) {
			  if (emptyLines > MAXIMUM_EMPTY_LINE_SEQUENCE_LENGTH) {
				  continue;
			  }
			  emptyLines++;
		  } else {
			  emptyLines = 0;
		  }
		  sb.append(line + "\n");
		}
		scanner.close();
		
		bodyText = sb.toString();
		if (bodyText.length() != origLength) {		
			logger.log(Level.INFO, "Multiple linebreaks have been collapsed.");
		}
		if (bodyText.length() > maxDocumentLength) {
			logger.log(Level.INFO, "Document length exceeds maximum (" + maxDocumentLength + "): " + bodyText.length());
			bodyText = bodyText.substring(0, maxDocumentLength);
		}
		
		return bodyText;
	}
	
}
