package apollo.config;

import apollo.datamodel.*;

import java.util.*;
  /**
   * areOverlapping determines if two SeqFeatureIs overlap using the
   * currently defined gene definition (from Config.getGeneDefinition().
   * Obviously this method can be called with FeatureSets such as
   * Genes or Transcripts - it should handle such cases.
   */
public interface OverlapI {

  public boolean areOverlapping(SeqFeatureI sa,SeqFeatureI sb);
  
}
