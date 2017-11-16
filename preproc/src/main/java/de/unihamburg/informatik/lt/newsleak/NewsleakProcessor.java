package de.unihamburg.informatik.lt.newsleak;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Properties;

import de.tu.darmstadt.lt.ner.preprocessing.GermaNERMain;

/**
 * 
 * This class is used to pre-process and store newsleak data into a database and
 * elastic search index. All required files should be available from the home
 * directory . Configuration will be read from the newsleak.properties. the
 * elasticsearch.yml file contains configuration about the elastic search index
 * (cluster name, ports, folder...). It is required a proper CSV files named
 * document.csv and metadata.csv. See the example files home directory of this
 * project. 
 *
 */
public class NewsleakProcessor {
    static String lang;
    static String documentName;
    static int threads;

    public static void main(String[] args) throws Exception {
        getConfigs("newsleak.properties");
        // Extracting HidelTime event expressions
        if (args[0].equals("-h")) {
            extractHeidelTime();
        }
        // Recognizing NER using GermaNER
        else if (args[0].equals("-g")) {
            extractNER();
        }
        // Extracting keyterms
        else if (args[0].equals("-k")) {
            extractKeyterms();
        }
        // Importing data to PSQL database
        else if (args[0].equals("-d")) {
            importUnitsToPSQLDB();
        }
        // Building ElasticsearchIndex
        else if (args[0].equals("-e")) {
            buildESIndex();
        }
        // run all in sequence
        else if (args[0].equals("-a")) {
            extractHeidelTime();
            extractNER();
            importUnitsToPSQLDB();
            buildESIndex();
        } else {
            System.out.println("USAGE: please type -h to extract heideltime expressions, -g to extract named entities, "
                    + "-d to import the extracted units to PSQL database -e to build the Elasticsearch index or -a to run all of the components");
        }
    }

    private static void extractKeyterms() throws Exception {
      	System.out.println("=======================");
        System.out.println("= Extracting Keyterms =");
        System.out.println("=======================");
        KeyTermExtractor.main(
                ("-t " + documentName + " -o out -l resources/wordlists/" + lang + ".frequency.txt").split(" "));
        System.out.println("============================");
        System.out.println("= Extracting Keyterms done =");
        System.out.println("============================");
	}

	private static void buildESIndex() throws Exception {
        System.out.println("======================================");
        System.out.println("=    Building elasticsearch index    =");
        System.out.println("======================================");
        PSQL2ESBulkIndexingWithSimpleTimex.main(new String[] {});
        System.out.println("======================================");
        System.out.println("=  Building elasticsearch index  done=");
        System.out.println("======================================");
    }

    private static void importUnitsToPSQLDB()
            throws IOException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        System.out.println("====================================");
        System.out.println("= Importing date to PSQL databse  =");
        System.out.println("====================================");
        PSQLDBImport.main(new String[] {});
        System.out.println("======================================");
        System.out.println("= Importing date to PSQL databse  done=");
        System.out.println("======================================");
    }

    private static void extractNER() throws Exception {
        System.out.println("===================================");
        System.out.println("= Extracting Named Entities =");
        System.out.println("===================================");
        GermaNERMain.main(
                ("-t " + documentName + " -o out -c resources/ner_models/" + lang + "/config.properties").split(" "));
        System.out.println("====================================");
        System.out.println("= Extracting Named Entities  done  =");
        System.out.println("====================================");
    }

    private static void extractHeidelTime() throws ParseException, InstantiationException, IllegalAccessException,
            ClassNotFoundException, SQLException, IOException {
        System.out.println("================================");
        System.out.println("= Extracting Time Expressions   =");
        System.out.println("================================");
        HeidelTimingFromDocument.main(new String[] {});
        System.out.println("===================================");
        System.out.println("= Extracting Time Expressions done =");
        System.out.println("===================================");
    }

    static void getConfigs(String config) throws IOException {
        Properties prop = new Properties();
        InputStream input = null;

        input = new FileInputStream(config);

        prop.load(input);

        lang = prop.getProperty("lang");
        documentName = prop.getProperty("documentname");
        threads = Integer.valueOf(prop.getProperty("threads"));
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
