package de.unihamburg.informatik.lt.newsleak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;

public class ExtractKeywords {

    static String lang;
    static String documentName;
    static int threads;
    public static void main(String[] args)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
        Configuration.setTaggerType("default");
        Configuration.setSingleStrength(1);
        Configuration.setNoLimitStrength(1);
        Configuration.setModelFileLocation("model/default/english-lexicon.txt");

        getConfigs("newsleak.properties");
        File keywordOut = new File("keywords");
        FileUtils.forceMkdir(keywordOut);
        ThreadLocal<FileOutputStream> os = ThreadLocal.withInitial(new Supplier<FileOutputStream>() {
            @Override
            public FileOutputStream get() {
                try {
                    return new FileOutputStream(keywordOut+"/keywords-" + Thread.currentThread().getId() + ".tsv");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        });

        ThreadLocal<TermsExtractor> termExtractor = ThreadLocal.withInitial(() -> new TermsExtractor());

        ExecutorService t = Executors.newFixedThreadPool(50);

        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(new FileReader(new File(documentName)));
        
        for (CSVRecord record : records) {

            int id = Integer.valueOf(record.get(0));
            String content = record.get(1);
            t.execute(new Runnable() {
                @Override
                public void run() {

                    TermDocument termDocument = termExtractor.get().extractTerms(content);
                    Map<String, ArrayList<Integer>> extracted = termDocument.getFinalFilteredTerms();

                    for (String term : extracted.keySet()) {
                        try {
                            IOUtils.write(id + "\t" + term + "\t" + extracted.get(term).get(0) + "\n", os.get(),
                                    "UTF-8");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                }
            });

        }

        t.shutdown();
        while (!t.isTerminated())
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

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