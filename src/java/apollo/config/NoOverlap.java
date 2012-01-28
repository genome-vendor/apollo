package apollo.config;

import apollo.datamodel.SeqFeatureI;


/** as the name implies this overlapper will return false for all areOverlapping
    queries. This is for 1 level annots that never overlap each other. */

public class NoOverlap implements OverlapI {

  /** Just returns false */
  public boolean areOverlapping(SeqFeatureI a, SeqFeatureI b) {
    return false;
  }
}
