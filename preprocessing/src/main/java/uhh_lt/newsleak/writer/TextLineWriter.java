package uhh_lt.newsleak.writer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.newsleak.resources.TextLineWriterResource;
import uhh_lt.newsleak.types.Metadata;

/**
 * A simple writer for debug and development purposes only. It write fulltexts
 * and/or extracted entities to disk.
 * 
 * This writer is not used in any production setting.
 */
@OperationalProperties(multipleDeploymentAllowed = true, modifiesCas = false)
public class TextLineWriter extends JCasAnnotator_ImplBase {

	/** The sample id hash. */
	private HashSet<String> sampleIdHash = new HashSet<String>();

	/** The logger. */
	Logger logger;

	/** The lang stats. */
	public HashMap<String, String> langStats;

	/** The Constant RESOURCE_LINEWRITER. */
	public static final String RESOURCE_LINEWRITER = "linewriter";

	/** The linewriter. */
	@ExternalResource(key = RESOURCE_LINEWRITER)
	private TextLineWriterResource linewriter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.fit.component.JCasAnnotator_ImplBase#initialize(org.apache.
	 * uima.UimaContext)
	 */
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		langStats = new HashMap<String, String>();
		logger = context.getLogger();
		// restrict to samples
		String[] sampleIds = { "9141", "9099", "10779", "6823", "7455", "8078", "9538", "10051", "9660", "10521" };
		sampleIdHash.addAll(Arrays.asList(sampleIds));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(org.apache.
	 * uima.jcas.JCas)
	 */
	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();
		// Language
		String outputText = jcas.getDocumentLanguage() + "\t";

		// n sentencs
		Collection<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, 0,
				jcas.getDocumentText().length());
		outputText += sentences.size() + "\t";

		// n tokens
		Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, 0, jcas.getDocumentText().length());
		outputText += tokens.size() + "\t";

		// pos
		String firstPOS = tokens.iterator().next().getPos();
		outputText += firstPOS + "\t";

		// text
		outputText += docText.replaceAll("\n", " ");

		// linewriter.append(outputText);

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		langStats.put(metadata.getDocId(), jcas.getDocumentLanguage());

		if (sampleIdHash.contains(metadata.getDocId())) {
			int i = 0;
			for (Sentence s : sentences) {
				i++;
				String sOut = metadata.getDocId() + "\t" + i + "\t";
				String tOut = "";
				for (Token t : JCasUtil.selectCovered(jcas, Token.class, s.getBegin(), s.getEnd())) {
					tOut += t.getCoveredText() + " ";
				}
				sOut += tOut.trim();
				linewriter.append(sOut);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.uima.analysis_component.AnalysisComponent_ImplBase#
	 * collectionProcessComplete()
	 */
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		logger.log(Level.INFO, langStats.toString());
	}

}
