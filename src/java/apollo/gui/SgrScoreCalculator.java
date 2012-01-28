package apollo.gui;

import apollo.datamodel.CurationSet;
import apollo.gui.genomemap.LinearView;

import java.util.*;

import java.io.*;

import org.apache.log4j.*;

/** Gets the score for a given position from a sgr file
 * 
 * @author elee
 *
 */

public class SgrScoreCalculator extends StoredScoreCalculator {

	//class variables
	protected final static Logger logger = LogManager.getLogger(SgrScoreCalculator.class);
	
	/** Builds a SgrScoreCalculator object
	 * 
	 * @param data - name of file containing sgr data
	 * @param winSize - size of display window
	 * @throws IOException - if the file cannot be accessed
	 */
	public SgrScoreCalculator(String data, int winSize, CurationSet curation)
		throws IOException
	{
		super(winSize);
		BufferedReader br = new BufferedReader(new FileReader(data));
		String line;
    double minAbsScore = Double.POSITIVE_INFINITY;
    Set<String> unmatchedIds = new HashSet<String>();
		while ((line = br.readLine()) != null) {
			String []tokens = line.split("\t");
			String refId = curation.getRefSequence().getName();
			if (!tokens[0].equals(refId)) {
			  if (!unmatchedIds.contains(tokens[0])) {
	        logger.warn("Score id " + tokens[0] + " does not match main sequence id " +
	            refId + ". Skipping score.");
			    unmatchedIds.add(tokens[0]);
			  }
				continue;
			}
			Double score = new Double(Double.parseDouble(tokens[2]));
      // chuck out scores outside of the given range
			int pos = Integer.parseInt(tokens[1]);
      if (pos >= curation.getLow() && pos <= curation.getHigh()) {
        scoreMap.put(pos, score);
      }

      if (Math.abs(score) < minAbsScore) {
        minAbsScore = Math.abs(score);
      }
			if (score.doubleValue() < minScore) {
			  minScore = score;
			}
			if (score.doubleValue() > maxScore) {
			  maxScore = score;
			}
		}
    while (minAbsScore * factor < 1) {
      factor *= 10;
    }
	}
	
}
