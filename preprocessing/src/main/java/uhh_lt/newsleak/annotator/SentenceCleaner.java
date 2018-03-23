package uhh_lt.newsleak.annotator;

import java.util.Collection;
import java.util.List;

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
		log.setLevel(Level.ALL);
	}


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		cleanTokens(jcas);
		restructureSentences(jcas);

	}


	public void restructureSentences(JCas jcas) {
		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
		for (Sentence sentence : sentences) {

			Collection<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());

			if (tokens.size() > MAX_TOKENS_PER_SENTENCE) {
				log.log(Level.FINEST, "Restructuring long sentence: " + sentence.getBegin() + " " + sentence.getEnd());

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
						log.log(Level.FINEST, "New sentence: " + sStart + " " + token.getEnd());
					}
				}
				if (!startNew) {
					Sentence s = new Sentence(jcas);
					s.setBegin(sStart);
					s.setEnd(sentence.getEnd());
					s.addToIndexes();
					log.log(Level.FINEST, "New sentence: " + sStart + " " + sentence.getEnd());
				}

				sentence.removeFromIndexes();

			}
		}
	}

	private void cleanTokens(JCas jcas) {
		
		// remove too long tokens
		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
		for (Token token : tokens) {
			if (token.getCoveredText().length() > MAX_TOKEN_LENGTH) {
				token.removeFromIndexes();
			}
		}
		Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
		
		// remove empty sentences
		for (Sentence sentence : sentences) {
			tokens = JCasUtil.selectCovered(jcas, Token.class, sentence.getBegin(), sentence.getEnd());
			if (tokens.isEmpty()) {
				sentence.removeFromIndexes();
			}
		}
	}
}
