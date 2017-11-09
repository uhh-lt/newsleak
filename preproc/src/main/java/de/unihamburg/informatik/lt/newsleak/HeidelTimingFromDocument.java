package de.unihamburg.informatik.lt.newsleak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import de.unihd.dbs.heideltime.standalone.CLISwitch;
import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

@SuppressWarnings("unused")
public class HeidelTimingFromDocument {

	static String lang;
	static String documentName;
	static int threads;
	public static void main(String[] arg) throws ParseException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException, IOException {
		Language language;
		getConfigs("newsleak.properties");
		if (lang.equals("en")) { // default English
			language = Language.ENGLISH;
		} else if (lang.equals("de")) {
			language = Language.GERMAN;
		} else { // default English
			language = Language.ENGLISH;
		}
		String[] args = new String[] { "-c", "newsleak.properties", "-t", "news", "-o", "newsleak" };
		// FileOutputStream os = new FileOutputStream("heideltime.tsv");
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				CLISwitch sw = CLISwitch.getEnumFromSwitch(args[i]);
				if (sw == null) { // unsupported CLI switch
					System.exit(-1);
				}

				if (sw.getHasFollowingValue()) { // handle values for
													// switches
					if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
						sw.setValue(args[++i]);
					} else { // value is missing or malformed
						System.exit(-1);
					}
				} else { // activate the value-less switches
					sw.setValue(null);
				}
			}
		}

		File heideltimex = new File("heideltimex");
		FileUtils.forceDeleteOnExit(heideltimex);
		FileUtils.forceMkdir(heideltimex);

		// display help dialog if HELP-switch is given
		if (CLISwitch.HELP.getIsActive()) {
			printHelp();
			System.exit(1);
		}
		long startTime = System.currentTimeMillis();
		if (!new File(documentName).exists()) {
			System.out.println("USAGE: run it as java -jar document where document i"
					+ "s a CSV file of format ID, Content, CreateionDAte ");
			System.exit(1);
		}

		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(new FileReader(new File(documentName)));

		ThreadLocal<FileOutputStream> os = ThreadLocal.withInitial(new Supplier<FileOutputStream>() {
			@Override
			public FileOutputStream get() {
				try {

					return new FileOutputStream(heideltimex + "/" + Thread.currentThread().getId() + ".tsv");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

		});
		// Check output format
		OutputType outputType = OutputType.valueOf(CLISwitch.OUTPUTTYPE.getValue().toString().trim().toUpperCase());

		// Check language
		// language = Language.getLanguageFromString((String)
		// CLISwitch.LANGUAGE.getValue());

		// Check type
		DocumentType type = DocumentType.valueOf(CLISwitch.DOCTYPE.getValue().toString().toUpperCase());

		// Set the preprocessing POS tagger
		POSTagger posTagger = (POSTagger) CLISwitch.POSTAGGER.getValue();
		// Read configuration from file
		String configPath = CLISwitch.CONFIGFILE.getValue().toString();
		try {
			readConfigFile(configPath);
		} catch (Exception e) {
			printHelp();
			System.exit(-1);
		}

		// Set whether or not to use the Interval Tagger
		Boolean doIntervalTagging = false;

		ThreadLocal<HeidelTimeStandalone> standalone = ThreadLocal.withInitial(
				() -> new HeidelTimeStandalone(language, type, outputType, null, posTagger, doIntervalTagging));

		ExecutorService t = Executors.newFixedThreadPool(Integer.valueOf(threads));

		for (CSVRecord record : records) {

			int id = Integer.valueOf(record.get(0));
			String content = record.get(1);
			String created = record.get(2);

			t.execute(new Runnable() {
				@Override
				public void run() {

					// Check document creation time
					Date dct = null;
					try {
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
						dct = formatter.parse(created.toString());
					} catch (Exception e) {
						printHelp();
						System.exit(-1);
					}

					// Handle locale switch
					String locale = (String) CLISwitch.LOCALE.getValue();
					Locale myLocale = null;
					if (CLISwitch.LOCALE.getIsActive()) {
						// check if the requested locale is available
						for (Locale l : Locale.getAvailableLocales()) {
							if (l.toString().toLowerCase().equals(locale.toLowerCase()))
								myLocale = l;
						}

						try {
							Locale.setDefault(myLocale); // try to set the
															// locale
						} catch (Exception e) {
							printHelp();
							System.exit(-1);
						}
					}

					try {

						String out = standalone.get().process(content, dct);
						for (String line : out.split("\n")) {
							IOUtils.write(id + "\t" + line + "\n", os.get(), "UTF-8");
						}

					} catch (Exception e) {
						e.printStackTrace();
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
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total time in second to extract timex= " + (double) totalTime / 1000);

		System.out.println("Start merging ...");
		cleanAndMerge(heideltimex);
		System.out.println("Start merging Done");

		System.out.println("Start delteteing old files ...");
		delteFiles(heideltimex);
		System.out.println("Start delteteing old files done");

	}

	private static void printHelp() {
		String path = HeidelTimeStandalone.class.getProtectionDomain().getCodeSource().getLocation().getFile();
		String filename = path.substring(path.lastIndexOf(System.getProperty("file.separator")) + 1);

		System.out.println("HeidelTime Standalone");
		System.out.println("Copyright © 2011-2015 Jannik Strötgen");
		System.out.println("This software is free. See the COPYING file for copying conditions.");
		System.out.println();

		System.out.println("Usage:");
		System.out.println("  java -jar " + filename + " <input-document> [-param1 <value1> ...]");
		System.out.println();
		System.out.println("Parameters and expected values:");
		for (CLISwitch c : CLISwitch.values()) {
			System.out.println(
					"  " + c.getSwitchString() + "\t" + ((c.getSwitchString().length() > 4) ? "" : "\t") + c.getName());

			if (c == CLISwitch.LANGUAGE) {
				System.out.print("\t\t" + "Available languages: [ ");
				for (Language l : Language.values())
					if (l != Language.WILDCARD)
						System.out.print(l.getName().toLowerCase() + " ");
				System.out.println("]");
			}

			if (c == CLISwitch.POSTAGGER) {
				System.out.print("\t\t" + "Available taggers: [ ");
				for (POSTagger p : POSTagger.values())
					System.out.print(p.toString().toLowerCase() + " ");
				System.out.println("]");
			}

			if (c == CLISwitch.DOCTYPE) {
				System.out.print("\t\t" + "Available types: [ ");
				for (DocumentType t : DocumentType.values())
					System.out.print(t.toString().toLowerCase() + " ");
				System.out.println("]");
			}
		}

		System.out.println();
	}

	public static void readConfigFile(String configPath) {
		InputStream configStream = null;
		try {
			configStream = new FileInputStream(configPath);

			Properties props = new Properties();
			props.load(configStream);

			Config.setProps(props);

			configStream.close();
		} catch (FileNotFoundException e) {

			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void cleanAndMerge(File aFile) throws IOException {

		FileUtils.forceMkdir(new File(aFile.getParent(), "result"));
		File result = new File(aFile.getParent(), "result");
		File mergedDate = new File(result, "heideltimex_date.tsv");
		File merged = new File(result, "heideltimex.tsv");
		OutputStream out = new FileOutputStream(merged);
		OutputStream outDate = new FileOutputStream(mergedDate);
		for (File file : aFile.listFiles()) {
			LineIterator iterator = FileUtils.lineIterator(file);
			while (iterator.hasNext()) {
				String line = iterator.nextLine();
				if (line.split("\t").length == 6) {
					String timexvalue = line.split("\t")[5];
					String timeXType = line.split("\t")[4];
					if (timeXType.equals("DATE")) {

						// TODO This is such an ugly code
						String timexDateVal = timexvalue; // String timexDateVal
															// =
															// formatter.format(timexDateValPars);
						String timexDateValFormated = timexvalue;
						try {
							DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
							Date timexDateValPars = null;
							try {
								timexDateValPars = formatter.parse(timexvalue);
								timexDateVal = formatter.format(timexDateValPars);
								timexDateValFormated = formatter.format(timexDateValPars);
							} catch (Exception e) {
								try {
									formatter = new SimpleDateFormat("yyyy-MM");
									timexDateValPars = formatter.parse(timexvalue);
									timexDateVal = timexvalue + "-00";// formatter.parse(timexvalue);
									timexDateValFormated = formatter.format(timexDateValPars);
								} catch (Exception e2) {
									try {
										formatter = new SimpleDateFormat("yyyy");
										timexDateValPars = formatter.parse(timexvalue);
										timexDateVal = timexvalue + "-00-00";
										timexDateValFormated = formatter.format(timexDateValPars);
									} catch (Exception e3) {
										// do nothing
									}
								}
							}

							formatter = new SimpleDateFormat("yyyy-MM-dd");
							Date after = formatter.parse("1900-01-01");
							Date futureDate = new Date();
							if (timexDateValPars != null && timexDateValPars.after(after)
									&& timexDateValPars.before(futureDate)) {

								String[] timexVals = line.split("\t");
								if(!timexVals[1].isEmpty() && !timexVals[2].isEmpty())
								IOUtils.write(
										timexVals[0] + "\t" + timexVals[1] + "\t" + timexVals[2] + "\t" + timexVals[3]
												+ "\t" + timeXType + "\t" + timexDateValFormated + "\n",
										outDate, "UTF8");
							}

						} catch (Exception e) {
							// do nothing
						}
					}
					IOUtils.write(line + "\n", out, "UTF8");

				}
			}
		}
		out.close();
		outDate.close();

	}

	static void delteFiles(File aFile) throws IOException {
		for (File file : aFile.listFiles()) {
			FileUtils.forceDelete(file);
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
