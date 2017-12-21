package uhh_lt.newsleak.writer;

import de.unihd.dbs.heideltime.standalone.components.ResultFormatter;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Timex3Interval;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
public class NewsleakTimeFormatter implements ResultFormatter {

	public String format(JCas jcas) throws Exception {
		final String documentText = jcas.getDocumentText();
		String outText = new String();

		// get the timex3 intervals, do some pre-selection on them
		FSIterator iterIntervals = jcas.getAnnotationIndex(Timex3Interval.type).iterator();
		TreeMap<Integer, Timex3Interval> intervals = new TreeMap<Integer, Timex3Interval>();
		while(iterIntervals.hasNext()) {
			Timex3Interval t = (Timex3Interval) iterIntervals.next();

			// disregard intervals that likely aren't a real interval, but just a timex-translation
			if(t.getTimexValueLE().equals(t.getTimexValueLB()) && t.getTimexValueEE().equals(t.getTimexValueEB()))
				continue;

			if(intervals.containsKey(t.getBegin())) {
				Timex3Interval tInt = intervals.get(t.getBegin());

				// always get the "larger" intervals
				if(t.getEnd() - t.getBegin() > tInt.getEnd() - tInt.getBegin()) {
					intervals.put(t.getBegin(), t);
				}
			} else {
				intervals.put(t.getBegin(), t);
			}
		}

		FSIterator iterTimex = jcas.getAnnotationIndex(Timex3.type).iterator();
		TreeMap<Integer, Timex3> forwardTimexes = new TreeMap<Integer, Timex3>(),
				backwardTimexes = new TreeMap<Integer, Timex3>();
		while(iterTimex.hasNext()) {
			Timex3 t = (Timex3) iterTimex.next();
			forwardTimexes.put(t.getBegin(), t);
			backwardTimexes.put(t.getEnd(), t);
		}

		HashSet<Timex3> timexesToSkip = new HashSet<Timex3>();
		Timex3 prevT = null;
		Timex3 thisT = null;
		// iterate over timexes to find overlaps
		for(Integer begin : forwardTimexes.navigableKeySet()) {
			thisT = (Timex3) forwardTimexes.get(begin);

			// check for whether this and the previous timex overlap. ex: [early (friday] morning)
			if(prevT != null && prevT.getEnd() > thisT.getBegin()) {

				Timex3 removedT = null; // only for debug message
				// assuming longer value string means better granularity
				if(prevT.getTimexValue().length() > thisT.getTimexValue().length()) {
					timexesToSkip.add(thisT);
					removedT = thisT;
					/* prevT stays the same. */
				} else {
					timexesToSkip.add(prevT);
					removedT = prevT;
					prevT = thisT; // this iteration's prevT was removed; setting for new iteration 
				}

				// ask user to let us know about possibly incomplete rules
				Logger l = Logger.getLogger("TimeMLResultFormatter");
				l.log(Level.WARNING, "Two overlapping Timexes have been discovered:" + System.getProperty("line.separator")
				+ "Timex A: " + prevT.getCoveredText() + " [\"" + prevT.getTimexValue() + "\" / " + prevT.getBegin() + ":" + prevT.getEnd() + "]" 
				+ System.getProperty("line.separator")
				+ "Timex B: " + removedT.getCoveredText() + " [\"" + removedT.getTimexValue() + "\" / " + removedT.getBegin() + ":" + removedT.getEnd() + "]" 
				+ " [removed]" + System.getProperty("line.separator")
				+ "The writer chose, for granularity: " + prevT.getCoveredText() + System.getProperty("line.separator")
				+ "This usually happens with an incomplete ruleset. Please consider adding "
				+ "a new rule that covers the entire expression.");
			} else { // no overlap found? set current timex as next iteration's previous timex
				prevT = thisT;
			}
		}

		// alternative xml creation method
		Timex3Interval interval = null;
		Timex3 timex = null;
		for (Integer docOffset = 0; docOffset <= documentText.length(); docOffset++) {
			/**
			 *  see if we have to finish off old timexes/intervals
			 */
			if (timex != null && timex.getEnd() == docOffset) {
				outText += "\n";
				timex = null;
			}
			if (interval != null && interval.getEnd() == docOffset) {
				outText += "\n";
				interval = null;
			}

			/**
			 *  grab a new interval/timex if this offset marks the beginning of one
			 */
			if (interval == null && intervals.containsKey(docOffset))
				interval = intervals.get(docOffset);
			if (timex == null && forwardTimexes.containsKey(docOffset) && !timexesToSkip.contains(forwardTimexes.get(docOffset)))
				timex = forwardTimexes.get(docOffset);

			// handle timex openings after that
			if (timex != null && timex.getBegin() == docOffset) {
				String timexTag = "";

				if (!timex.getTimexType().equals(""))
					timexTag +=  timex.getTimexType();
				if (!timex.getTimexValue().equals(""))
					timexTag += "\t" + timex.getTimexValue() ;

				outText += timex.getBegin() +"\t"+ timex.getEnd() +"\t"+ timex.getCoveredText()+"\t"+ timexTag;
			}

		}

		return outText;
	}

	public String filterDate(String timexvalue) {
		String timexDateValFormatted = null;
		String timexDateVal;
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date timexDateValPars = null;
			try {
				timexDateValPars = formatter.parse(timexvalue);
				timexDateVal = formatter.format(timexDateValPars);
				timexDateValFormatted = formatter.format(timexDateValPars);
			} catch (Exception e) {
				try {
					formatter = new SimpleDateFormat("yyyy-MM");
					timexDateValPars = formatter.parse(timexvalue);
					timexDateVal = timexvalue + "-00";
					timexDateValFormatted = formatter.format(timexDateValPars);
				} catch (Exception e2) {
					try {
						formatter = new SimpleDateFormat("yyyy");
						timexDateValPars = formatter.parse(timexvalue);
						timexDateVal = timexvalue + "-00-00";
						timexDateValFormatted = formatter.format(timexDateValPars);
					} catch (Exception e3) {
						// do nothing
					}
				}
			}

		} catch (Exception e) {
			// do nothing
		}
		return timexDateValFormatted;
	}
}


