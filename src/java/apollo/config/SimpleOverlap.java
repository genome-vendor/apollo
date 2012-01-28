package apollo.config;

import apollo.datamodel.*;

import java.util.*;
  /**
   * areOverlapping determines if two SeqFeatureIs overlap using the
   * currently defined gene definition (from Config.getGeneDefinition().
   * Obviously this method can be called with FeatureSets such as
   * Genes or Transcripts - it should handle such cases.
   SimpleOverlap is a Singleton - only need one instance. Can then figure if 
   have default overlap (which SimpleOverlap is) with == to singleton.
   Rename to DefaultOverlap? or SimpleDefaultOverlap?
   */
public class SimpleOverlap implements OverlapI {

  private final static SimpleOverlap simpleOverlapSingleton = new SimpleOverlap();

  /** Private constractor. Singleton access through getSimpleOverlap 
      this cant be a singleton - needs to be able to be used via TiersIO - 
      nned public constructor for introspection */
  public SimpleOverlap() {}

  public static SimpleOverlap getSimpleOverlap() { 
    return simpleOverlapSingleton; 
  }

  /**
   * The simple overlap checking function. This descends both SeqFeature
   * trees until have features and compare them.
   */
  public boolean areOverlapping(SeqFeatureI sa,SeqFeatureI sb) {
    boolean overlap = false;
    if (sb.canHaveChildren()) {
      FeatureSetI fb = (FeatureSetI)sb;
      for (int i=0; i<fb.size() && !overlap; i++) {
        // Shouldn't this be |= rather than =?  It seems like we'll
        // throw away all but the last areOverlapping result.
        overlap = areOverlapping(sa,fb.getFeatureAt(i));
      }
    } 
    else if (sa.canHaveChildren()) {
      FeatureSetI fa = (FeatureSetI)sa;
      for (int i=0; i<fa.size() && !overlap; i++) {
        // Shouldn't this be |= rather than =?  It seems like we'll
        // throw away all but the last areOverlapping result.
        overlap = areOverlapping(fa.getFeatureAt(i),sb);
      }
    }
    else if (sa.overlaps(sb)) {
      overlap = sa.overlaps (sb);
    }
    return overlap;
  }

}
