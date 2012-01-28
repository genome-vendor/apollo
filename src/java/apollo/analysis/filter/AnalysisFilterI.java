/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.analysis.filter;

import apollo.datamodel.CurationSet;

public interface AnalysisFilterI {
  /**
   * All filters on computed results must support this interface.
   * <br>
   * It applies the filtering options contained in the hashtable
   * to the analysis. It deletes all spans and hits that do not
   * meet the criteria given. If a hit is left with zero spans 
   * then that hit is deleted. An analysis is of a given type and
   * thus different analyses may have been computed with the
   * same program, but will be filtered independently.
   */
  public void cleanUp(CurationSet curation, 
		      String analysis_type,
		      AnalysisInput filter_input);
}
