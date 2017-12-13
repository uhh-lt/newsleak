package uhh_lt.newsleak.resources;

import java.io.File;
import java.util.HashMap;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

public class DocumentLanguagesResource implements SharedResourceObject {
	
	private HashMap<String, String> documentLanguage;

	@Override
	public void load(DataResource f) throws ResourceInitializationException {
		File documentLanguageFile = new File(f.getUri());
		if (documentLanguageFile.exists()) {
			// load file
		} else {
			documentLanguage = new HashMap<String, String>();
		}
	}
	
	public HashMap<String, String> getDocumentLanguages() {
		return documentLanguage;
	}
	
	public void save() {
		// save file
	}

}
