package writer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.uima.Sentence;
import opennlp.uima.Token;
import uhh_lt.types.Metadata;

@OperationalProperties(multipleDeploymentAllowed=false, modifiesCas=false)
public class TextLineWriter extends JCasAnnotator_ImplBase {

	Logger logger;

	public HashMap<String, String> langStats;

	public static final String PARAM_OUTPUT_FILE_NAME = "outputFile";
	@ConfigurationParameter(
			name = PARAM_OUTPUT_FILE_NAME, 
			mandatory = true,
			description = "Output dir for writing")
	private String outputFile;
	private FileWriter fileWriter;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		try {
			fileWriter = new FileWriter(outputFile);
		} catch (IOException e) {
			System.err.println("Could not create outputfile " + outputFile);
			e.printStackTrace();
			System.exit(1);
		}
		langStats = new HashMap<String, String>();
		logger = context.getLogger();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		String docText = jcas.getDocumentText();
		// Language
		String outputText = jcas.getDocumentLanguage() + "\t";

		// n sentencs
		Collection<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, 0, jcas.getDocumentText().length());
		outputText += sentences.size() + "\t";

		// n tokens
		Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, 0, jcas.getDocumentText().length());
		outputText += tokens.size() + "\t";
		
		// pos
		String firstPOS = tokens.iterator().next().getPos();
		outputText += firstPOS + "\t";

		// text
		outputText += docText.replaceAll("\n", " ");

		try {
			fileWriter.append(outputText + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Metadata metadata = (Metadata) jcas.getAnnotationIndex(Metadata.type).iterator().next();
		langStats.put(metadata.getDocId(), jcas.getDocumentLanguage());

	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		try {
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.log(Level.INFO, langStats.toString());
	}

}
