package uhh_lt.newsleak.annotator;

import java.util.Collection;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import opennlp.uima.Sentence;
import opennlp.uima.Token;

@OperationalProperties(multipleDeploymentAllowed=true, modifiesCas=true)
public class SentenceCleaner extends JCasAnnotator_ImplBase {


	public static final int MAX_TOKENS_PER_SENTENCE = 150;
	public static final int RESIZE_TOKENS_PER_SENTENCE = 25;
	private static final int MAX_TOKEN_LENGTH = 75;

	Logger log;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		log = context.getLogger();
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {


		Collection<Sentence> sentences = JCasUtil.selectCovered(jcas, Sentence.class, 0, jcas.getDocumentText().length());
		for (Sentence sentence : sentences) {
			
			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
			
			if (tokens.size() > MAX_TOKENS_PER_SENTENCE) {
				log.log(Level.FINEST, "Restructuring long sentence: " + sentence.getCoveredText());
				
				int sStart = sentence.getBegin();
				boolean startNew = true;
				int nTok = 0;

				for (Token token : tokens) {
					nTok++;
					if (startNew) {
						sStart = token.getBegin();
						startNew = false;
					}
					if (nTok % RESIZE_TOKENS_PER_SENTENCE == 0) {
						Sentence s = new Sentence(jcas);
						s.setBegin(sStart);
						s.setEnd(token.getEnd());
						s.addToIndexes();
						startNew = true;	
						log.log(Level.FINEST, "New sentence: " + s.getCoveredText());
					}
				}
				if (!startNew) {
					Sentence s = new Sentence(jcas);
					s.setBegin(sStart);
					s.setEnd(sentence.getEnd());
					s.addToIndexes();
					log.log(Level.FINEST, "New sentence: " + s.getCoveredText());
				}

				sentence.removeFromIndexes();

			}
			
			// remove too long tokens
			
			for (Token token : tokens) {
				if (token.getCoveredText().length() > MAX_TOKEN_LENGTH) {
					token.removeFromIndexes();
				}
			}

		}



	}

	private double letterDigitRatio(String str) {
		int counterLetter = 0;
		int counterNonletter = 0;
		for (int i = 0; i < str.length(); i++) {
			if (Character.isLetter(str.charAt(i))) {
				counterLetter++;
			} else {
				if (str.charAt(i) != ' ') counterNonletter++;
			}
		}
		double ratio = counterNonletter / (double) counterLetter;
		return ratio;
	}

}
