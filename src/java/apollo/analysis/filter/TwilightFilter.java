/*
	Copyright (c) 1997
	University of California Berkeley
*/

package apollo.analysis.filter;

import java.util.*;

import apollo.datamodel.*;

import org.apache.log4j.*;

public class TwilightFilter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TwilightFilter.class);

  protected static int certainly_good = 50;
  protected static int certainly_bad = 20;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected boolean seq_nucleic = true;
  protected boolean ref_nucleic = true;

  public void setSeqNucleic (boolean nucleic) {
    this.seq_nucleic = nucleic;
  }
    
  public void setRefNucleic (boolean nucleic) {
    this.ref_nucleic = nucleic;
  }

  protected void setCertainlyBad(int lower_bound) {
    certainly_bad = (lower_bound >= 0) ? lower_bound : certainly_bad;
  }
    
  protected void setCertainlyGood(int upper_bound) {
    certainly_good = (upper_bound >= 0) ? upper_bound : certainly_good;
  }

  protected int getCertainlyBad() {
    return certainly_bad;
  }

  // TODO - currently ignoring debug flag in favor of Log4J configuration.  Need to
  // refactor/rework AnalysisFilter debugging to play nicely with Log4J.  See also
  // AnalysisInput.debugFilter.
  public FeaturePairI cleanUpTwilightZone(FeaturePairI terminal_span,
                                          FeaturePairI internal_span,
                                          boolean both_terminal,
                                          boolean debug) 
    throws Exception {
    
    int min_score;
    FeaturePairI min_span;
	
    if (terminal_span.getScore("bits") < internal_span.getScore("bits") ||
        !both_terminal) {
      min_span = terminal_span;
      min_score = (int) terminal_span.getScore("bits");
    }
    else {
      min_span = internal_span;
      min_score = (int) internal_span.getScore("bits");
    }

    if (certainly_good > 0 && min_score >= certainly_good) {
      return null;
    }
    if (min_score <= certainly_bad) {
      logger.debug(" min_score " + min_score + " < " + certainly_bad);
      return min_span;
    }

    int seq_gap = calcGap (terminal_span, internal_span, seq_nucleic);
    int ref_gap = calcGap (terminal_span.getHitFeature(), 
                           internal_span.getHitFeature(), 
                           ref_nucleic);

    int min_length = ((terminal_span.length() < internal_span.length()) ? 
                      terminal_span.length() : internal_span.length());

    int score_difference;
    if (certainly_good > certainly_bad)
      score_difference = certainly_good - certainly_bad;
    else
      // an upper < 0 implies that the scores are percent identities
      // ala sim4
      score_difference = 100; // - certainly_bad;

    int below_good_score = certainly_good - min_score;
    int above_bad_score = min_score - certainly_bad;
    logger.debug("genomic gap is " + seq_gap +
                 " subject gap is " + ref_gap +
                 " shortest span is " + min_length +
                 " score difference is " + score_difference + 
                 " nucleic is " + ref_nucleic);

    int MINdistmax = 15;
    double scale = (Math.pow((double)(score_difference), 2) /
                    (900 - MINdistmax));
    int distmax = (int)(MINdistmax + 
                    Math.pow((double)(below_good_score), 2) / scale);
      
    int MINdistmin   = -5;
    scale = (Math.pow((double)(score_difference), 2) / (MINdistmin + 20));
    int distmin = (int)(MINdistmin - 
                        (Math.pow((double)(below_good_score), 2) / scale));
    /* Don't allow small MSPs to sneak in under the distmin criterion */
    if (min_length < -distmin)
      distmin = -min_length;
      
    int MINshiftmax = 15;
    scale = (Math.pow((double)(score_difference), 1) / 
             (150 - MINshiftmax));
    int shiftmax = (int)(MINshiftmax + 
                         Math.pow((double)(below_good_score), 1) / scale);
      
    int MINintronmax = 5;
    scale = (Math.pow((double)(score_difference), 2) / 
             (3000 - MINintronmax));
    int intronmax = (int)(MINintronmax + 
                      Math.pow((double)(below_good_score), 2) / scale);
      
    if (seq_gap > ref_gap && seq_gap < intronmax) {
      logger.debug("intronmax is " + intronmax +
                   ", seq_gap changing from " + seq_gap + 
                   " to " + ref_gap);
      seq_gap = ref_gap;
    }

    int distance;
    int shift;
      
    if (seq_gap < ref_gap) {
      distance = seq_gap;
      shift = ref_gap - seq_gap;
    }
    else {
      distance = ref_gap;
      shift = seq_gap - ref_gap;
    }
      
    logger.debug("distance=" + distance +
                 " distmin=" + distmin + " > " +
                 (distance > distmin) +
                 " distmax=" + distmax + " < " +
                 (distance < distmax) +
                 " shift=" + shift +
                 " shiftmax=" + shiftmax + " < " +
                 (shift < shiftmax));

    if ( distance > distmin
         && distance < distmax
         && shift < shiftmax ) {
      /* this is OK */
      return null;
    }
    else {
      logger.debug("distance=" + distance +
                   " distmin=" + distmin + " > " +
                   (distance > distmin) +
                   " distmax=" + distmax + " < " +
                   (distance < distmax) +
                   " shift=" + shift +
                   " shiftmax=" + shiftmax + " < " +
                   (shift < shiftmax));

      return min_span;
    }
  }

  private int calcGap (RangeI span1, RangeI span2, boolean nucleic)
    throws Exception {
    int high;
    int low;
      
    if (span1.getLow() < span2.getLow()) {
      low = span1.getHigh();
      high = span2.getLow();
    }
    else {
      low = span2.getHigh();
      high = span1.getLow();
    }
    int gap = (high - low) * (nucleic ? 1 : 3);
    return gap;
  }

}
  
