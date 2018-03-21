package uhh_lt.newsleak.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.Logger;

import opennlp.uima.Token;
import uhh_lt.keyterms.Extractor;

public class DictionaryResource extends Resource_ImplBase {

	private Logger logger;

	public static final String PARAM_DICTIONARY_FILES = "dictionaryFiles";
	@ConfigurationParameter(name = PARAM_DICTIONARY_FILES)
	private DictFiles dictionaryFiles;

	
    private HashMap<String, HashSet<String>> dictionaries;
    
    
	@Override
	public boolean initialize(ResourceSpecifier aSpecifier, Map<String, Object> aAdditionalParams)
			throws ResourceInitializationException {
		if (!super.initialize(aSpecifier, aAdditionalParams)) {
			return false;
		}
		this.logger = this.getLogger();

		dictionaries = new HashMap<String, HashSet<String>>();
		
		for (File f : dictionaryFiles) {
			
			try {
				String dictType = f.getName().replaceAll("\\..*", "").toUpperCase();
				List<String> dictTermList = FileUtils.readLines(f);
				HashSet<String> dictTerms = new HashSet<String>();
				for (String term : dictTermList) {
					String t = term.trim();
					if (!t.isEmpty()) {
						dictTerms.add(t.toLowerCase());
					}
				}
				dictionaries.put(dictType, dictTerms);
				
			} catch (IOException e) {
				throw new ResourceInitializationException(e.getMessage(), null);
			}
		
		}
		
		return true;
	}



	private class DictFiles implements Iterable<File> {

		List<File> files;

		DictFiles(String list) {
			files = new ArrayList<File>();
			for (String f : list.split(", +?")) {
				files.add(new File(f));
			}
		}

		@Override
		public Iterator<File> iterator() {
			return files.iterator();
		}

	}
}
