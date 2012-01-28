package apollo.analysis.filter;

import java.util.*;

/**
 * This contains  all of the filtering parameters to use 
 * on a set of raw output from a computational analysis of
 * the sequence.
**/
public class AnalysisInput {

  public static int NO_LIMIT = -1;

  protected String tier;
  protected String display_type;
  protected String analysis_type;
  protected boolean filter = true;
  protected int min_score = NO_LIMIT;
  protected int min_identity = NO_LIMIT;
  protected int min_length = NO_LIMIT;
  protected int wordsize = NO_LIMIT;
  protected int max_ratio = 15;
  protected double max_expect = NO_LIMIT;
  protected int max_cover = -1;
  protected int max_exons = -1;
  protected int max_gap = -1;
  protected int coincidence = NO_LIMIT;
  protected int offset = 0;
  protected boolean remove_twilight = false;
  protected boolean remove_shadows = false;
  protected boolean distinctHSPs = false;
  protected boolean split_frames = false;
  protected boolean split_dups = false;
  protected boolean use_percentage = true;
  protected boolean join_EST_ends = false;
  protected boolean trim_polyA = false;
  protected String revcomp_3prime = null;
  protected String debug_str = null;
  protected boolean debug = false;
  protected Date run_date;
  protected String database = "";
  protected boolean collapse = false;
  protected String autopromote = null;
  protected boolean keep_polyApredict = false;
  protected boolean keep_promoter = false;
  /** The filter panel isn't necessarily the right place to put this checkbox,
    * but it was the easiest way to implement it. */
  protected boolean queryIsGenomic = false;

  public AnalysisInput () {
  }

  public void setTier (String tier) {
    this.tier = tier;
  }

  public String getTier () {
    return tier;
  }

  public void setType (String type) {
    this.display_type = type;
  }

  public String getType () {
    return display_type;
  }

  public void setAnalysisType (String type) {
    this.analysis_type = type;
  }

  public String getAnalysisType () {
    return analysis_type;
  }

  public boolean runFilter () {
    return filter;
  }

  public void runFilter (boolean filter) {
    this.filter = filter;
  }

  public void setMaxRatio (String maxratioStr) {
    try {
      setMaxRatio (Integer.parseInt (maxratioStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse max_ratio from " + maxratioStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMaxRatio (int max_ratio) {
    this.max_ratio = max_ratio;
  }

  public int getMaxRatio () {
    return max_ratio;
  }

  public void setWordSize (String wordsizeStr) {
    try {
      setWordSize (Integer.parseInt (wordsizeStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse wordsize from " + wordsizeStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setWordSize (int wordsize) {
    if (wordsize > 0)
      this.wordsize = wordsize;
    else
      this.wordsize = NO_LIMIT;
  }

  public int getWordSize () {
    return wordsize;
  }

  public boolean removeLowContent () {
    return wordsize != NO_LIMIT;
  }

  public void setMaxExpect (String max_expectStr) {
    try {
      setMaxExpect (Double.valueOf(max_expectStr).doubleValue());
    }
    catch (Exception e) {
      System.out.println ("Unable to parse max_expect from " + max_expectStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMaxExpect (double max_expect) {
    if (max_expect >= 0 && max_expect <= 1)
      this.max_expect = max_expect;
    else
      this.max_expect = NO_LIMIT;
  }

  public double getMaxExpect () {
    return max_expect;
  }

  public boolean useExpect() {
    return max_expect != NO_LIMIT;
  }

  public void setMaxCover (String max_coverStr) {
    try {
      setMaxCover (Integer.parseInt (max_coverStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse max_cover from " + max_coverStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMaxCover (int max_cover) {
    if (max_cover > 0)
      this.max_cover = max_cover;
    else
      this.max_cover = NO_LIMIT;
  }

  public int getMaxCover () {
    return max_cover;
  }

  public boolean limitCoverage () {
    return max_cover != NO_LIMIT;
  }

  public void setMaxExons (String max_exonsStr) {
    try {
      setMaxExons (Integer.parseInt (max_exonsStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse max_exons from " + max_exonsStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMaxExons (int max_exons) {
    if (max_exons > 0)
      this.max_exons = max_exons;
    else
      this.max_exons = NO_LIMIT;
  }

  public int getMaxExons () {
    return max_exons;
  }

  public boolean limitMaxExons () {
    return max_exons != NO_LIMIT;
  }

  public void setMaxAlignGap (String max_gapStr) {
    try {
      setMaxAlignGap (Integer.parseInt (max_gapStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse max_gap from " + max_gapStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMaxAlignGap (int max_gap) {
    if (max_gap >= 0 && max_gap <= 100)
      this.max_gap = max_gap;
    else
      this.max_gap = NO_LIMIT;
  }

  public int getMaxAlignGap () {
    return max_gap;
  }

  public boolean limitAlignGap() {
    return max_gap != NO_LIMIT;
  }

  public void setCoincidence (String coincidenceStr) {
    try {
      setCoincidence (Integer.parseInt (coincidenceStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse coincidence from " + coincidenceStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setCoincidence (int coincidence) {
    if (coincidence >= 0 && coincidence <= 100)
      this.coincidence = coincidence;
    else
      this.coincidence = NO_LIMIT;
  }

  public int getCoincidence () {
    return coincidence;
  }

  public boolean useCoincidence() {
    return coincidence != NO_LIMIT;
  }

  public boolean removeShadows() {
    return remove_shadows;
  }

  public void filterShadows(boolean remove) {
    remove_shadows = remove;
  }

  /* at some point make it possible for these to be set
     in the meantime a value that is less than 0 will force
     the program to use the defaults in the twilightfilter
  */
  public int getTwilightUpper () {
    return -1;
  }

  public int getTwilightLower() {
    return -1;
  }

  public boolean removeTwilights () {
    return remove_twilight;
  }

  public void setRemoveTwilights(boolean dothis) {
    this.remove_twilight = dothis;
  }

  public void setMinScore (String scoreStr) {
    try {
      setMinScore (Integer.parseInt (scoreStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse score from " + scoreStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMinScore (int score) {
    if (score > 0)
      this.min_score = score;
    else
      this.min_score = NO_LIMIT;
  }

  public int getMinScore () {
    return min_score;
  }

  public boolean useScore() {
    return min_score != NO_LIMIT;
  }

  public void setMinIdentity (String identityStr) {
    try {
      setMinIdentity (Integer.parseInt (identityStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse identity from " + identityStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMinIdentity (int identity) {
    if (identity > 0)
      this.min_identity = identity;
    else
      this.min_identity = NO_LIMIT;
  }

  public int getMinIdentity () {
    return min_identity;
  }

  public boolean useIdentity() {
    return min_identity != NO_LIMIT;
  }

  public void setMinLength (String lengthStr) {
    try {
      setMinLength (Integer.parseInt (lengthStr));
    }
    catch (Exception e) {
      System.out.println ("Unable to parse length from " + lengthStr +
			  " " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setMinLength (int length) {
    if (length > 0)
      this.min_length = length;
    else
      this.min_length = NO_LIMIT;
  }

  public int getMinLength () {
    return min_length;
  }

  public boolean useLength() {
    return min_length != NO_LIMIT;
  }

  public void setLengthUnits (boolean is_percent) {
    this.use_percentage = is_percent;
  }

  public boolean usePercentage() {
    return use_percentage;
  }

  
  public void autonomousHSPs(boolean dothis) {
    this.distinctHSPs = dothis;
  }

  public boolean useAutonomousHSPs() {
    return distinctHSPs;
  }

  public void setSplitTandems(boolean dothis) {
    this.split_dups = dothis;
  }

  public boolean splitTandems() {
    return split_dups;
  }

  public void revComp3Prime(String suffix) {
    this.revcomp_3prime = suffix;
  }

  public boolean revComp3Prime() {
    return (revcomp_3prime != null && !revcomp_3prime.equals(""));
  }

  public String get3PrimeSuffix() {
    return revcomp_3prime;
  }

  public void joinESTends(boolean dothis) {
    this.join_EST_ends = dothis;
  }

  public boolean joinESTends() {
    return join_EST_ends;
  }

  public void trimPolyA(boolean dothis) {
    this.trim_polyA = dothis;
  }

  public boolean trimPolyA() {
    return trim_polyA;
  }

  public void keepPolyA(boolean dothis) {
    this.keep_polyApredict = dothis;
  }

  public boolean keepPolyA() {
    return keep_polyApredict;
  }

  public void keepPromoter(boolean dothis) {
    this.keep_promoter = dothis;
  }

  public boolean keepPromoter() {
    return keep_promoter;
  }

  public void setDebug(String debug_str) {
    this.debug_str = debug_str;
    setDebug (debug_str != null &&
	      (debug_str.equalsIgnoreCase("t") ||
	       debug_str.equalsIgnoreCase("true")));
  }

  public void setDebug(boolean dothis) {
    this.debug = dothis;
  }

  public boolean debugFilter(String this_str) {
    return (debug || (debug_str != null && this_str.startsWith(debug_str)));
  }

  public void setSplitFrames(boolean dothis) {
    this.split_frames = dothis;
  }

  public boolean splitFrames() {
    return split_frames;
  }

  public void setQueryIsGenomic(boolean dothis) {
//    System.out.println("AnalysisInput.setQueryIsGenomic " + dothis);  // DEL
    this.queryIsGenomic = dothis;
  }

  public boolean queryIsGenomic() {
    return queryIsGenomic;
  }

  public void setOffset (String offsetStr, int genomic_start) {
    offsetStr = offsetStr.trim();
    if (offsetStr != null && !offsetStr.equals("")) {
      try {
        setOffset (Integer.parseInt (offsetStr) + genomic_start - 1);
      }
      catch (Exception e) {
        System.out.println ("Unable to parse offset from " + offsetStr +
                            " " + e.getMessage());
        e.printStackTrace();
        setOffset (genomic_start); // - 1);
      }
    }
    else {
      setOffset (genomic_start); // - 1);
    }
  }

  public void setOffset (int offset) {
    if (offset > 0)
      this.offset = offset;
    else
      this.offset = 0;
    System.out.println ("Set offset to " + this.offset);
  }

  public int getOffset () {
    return offset;
  }

  public Date getRunDate () {
    return run_date;
  }
  
  public void setRunDate (long processed_time) {
    if (processed_time > 0)
      run_date = new java.util.Date(processed_time * 1000);
    else
      run_date = new java.util.Date(); //default to now
  }

  public void seRtunDate (Date run_date) {
    this.run_date = run_date;
  }

  public void collapseResults (boolean collapse) {
    this.collapse = collapse;
  }

  public boolean collapseResults () {
    return collapse;
  }

  public void promoteResults (String baptizer) {
    this.autopromote = baptizer;
  }

  public boolean promoteResults () {
    return autopromote != null;
  }

  public String getBaptizer () {
    return autopromote;
  }

  public void setDatabase (String db) {
    this.database = db;
  }
  
  public String getDatabase() {
    return this.database;
  }
}
