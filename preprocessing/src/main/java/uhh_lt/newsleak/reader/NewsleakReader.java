package uhh_lt.newsleak.reader;

import java.util.Scanner;

import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 * This abstract reader provides basic functionality for all primary
 * elasticsearch data readers. This is basically a maximum threshold for reading
 * documents (for debug purposes), and some fulltext cleaning procedures.
 * 
 * Fulltext cleaning encompasses omission of n >
 * MAXIMUM_EMPTY_LINE_SEQUENCE_LENGTH repeated blank lines (this is to deal with
 * fulltext extraction problems from spreadsheet documents such as xlsx files
 * which may result in hundreds of thousands of blank lines), and pruning of
 * documents to a maximum length (given in characters).
 */
public abstract class NewsleakReader extends CasCollectionReader_ImplBase {

	/** The logger. */
	protected Logger logger;

	/** The Constant PARAM_DEBUG_MAX_DOCS. */
	public static final String PARAM_DEBUG_MAX_DOCS = "maxRecords";

	/** The max records. */
	@ConfigurationParameter(name = PARAM_DEBUG_MAX_DOCS, mandatory = false)
	protected Integer maxRecords = Integer.MAX_VALUE;

	/** The Constant PARAM_MAX_DOC_LENGTH. */
	public static final String PARAM_MAX_DOC_LENGTH = "maxDocumentLength";

	/** The max document length. */
	@ConfigurationParameter(name = PARAM_MAX_DOC_LENGTH, mandatory = false)
	protected Integer maxDocumentLength = Integer.MAX_VALUE; // 1500 * 10000 = 15000000 = 10000 norm pages

	/** The Constant MAXIMUM_EMPTY_LINE_SEQUENCE_LENGTH. */
	public static final int MAXIMUM_EMPTY_LINE_SEQUENCE_LENGTH = 50;

	/**
	 * Clean body text (prune to maximum length, delete long sequences of blank
	 * lines).
	 *
	 * @param bodyText
	 *            the body text
	 * @return the string
	 */
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
