package de.unihamburg.informatik.lt.newsleak;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * 
 * The main method is used to true case documents based on an existing corpus.
 * First argument is the corpus used to true case the file. The second argument
 * is the document to be true cased. The third argument is the name of the output file which is true cased.
 *
 */
public class TrueCaser {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("USAGE: java -jar truecaser.jar trucasingFile fileToTruecase trueCasedFile.\n"
                    + "Please provide the truecasing corpus file, the file to be truecased, and the output file name for the truecase file.");
            System.exit(1);
        }
        if (args.length < 2) {
            System.out.println("USAGE: java -jar truecaser.jar trucasingFile fileToTruecase trueCasedFile.\n"
                    + "Please provide the document to be truecased.");
            System.exit(1);
        }
        if (args.length < 3) {
            System.out.println("USAGE: java -jar truecaser.jar trucasingFile fileToTruecase trueCasedFile.\n"
                    + "Please provide the output file name for the truecased file.");
            System.exit(1);
        }
        try {
            File fileDir = new File(args[0]);
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileDir), "UTF8"));
            String str;
            Map<String, Integer> freq = new HashMap<>();
            int i = 0;
            while ((str = in.readLine()) != null) {
                i++;
                if (i % 10000 == 0) {
                    System.out.println(i);
                }
                for (String term : str.split(" ")) {
                    freq.put(term, freq.getOrDefault(term, 0) + 1);
                }
            }
            freq = MapUtil.sortByValue(freq);

            in.close();
            FileOutputStream os = new FileOutputStream(args[2]);
            Map<String, String> truecase = new HashMap<>();

            for (String term : freq.keySet()) {
                if (truecase.get(term.toLowerCase()) == null) {
                    truecase.put(term.toLowerCase(), term);
                } else {
                    String trueCase = truecase.get(term.toLowerCase());
                    if (freq.get(term) > freq.get(trueCase)) {
                        truecase.put(term.toLowerCase(), term);
                    }
                }
            }

            fileDir = new File(args[1]);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(new FileInputStream(fileDir), "UTF8"));
            while ((str = in2.readLine()) != null) {
                for (String term : str.split(" ")) {
                    IOUtils.write(truecase.getOrDefault(term.toLowerCase(), term) + " ", os, "UTF-8");
                }
                IOUtils.write("\n", os, "UTF-8");
            }
            in2.close();
            os.close();
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}