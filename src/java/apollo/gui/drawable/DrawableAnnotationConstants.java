package apollo.gui.drawable;

/**
 * Constants used in drawing annotations (which stops to display etc).
 */
public interface DrawableAnnotationConstants {

  /**
   * Don't show any stop codons.
   */
  int NOSTOPS  = 1;
  /**
   * Show all stop codons which are in phase.
   */
  int ALLSTOPS = 2;
  /**
   * Show the single stop codon specified as the translation end.
   */
  int ONESTOP  = 3;

  /**
   * Indicates the start of a feature (got with getStart()).
   */
  int START    = 1;
  /**
   * Indicates the end of a feature (got with getEnd()).
   */
  int END      = 2;
  /**
   * Indicates both ends of a feature.
   */
  int BOTHENDS = 3;

}
