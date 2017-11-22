package writer;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

@OperationalProperties(multipleDeploymentAllowed=false, modifiesCas=false)
public class TextLineWriter extends JCasAnnotator_ImplBase {
	
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
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		
		String docText = jcas.getDocumentText();
		
		String outputText = jcas.getDocumentLanguage() + "\t";
		outputText += docText.replaceAll("\n", " ");
		
		try {
			fileWriter.append(outputText + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}


	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		try {
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
