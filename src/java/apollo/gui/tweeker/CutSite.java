package apollo.gui.tweeker;

import java.awt.Color;

import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.SequenceI;

public class CutSite extends Range {
  private RestrictionEnzyme enzyme;
  boolean palindrome = false;
  boolean export = false;

  public CutSite(int start,
                 int end,
                 RestrictionEnzyme enzyme) {
    super(enzyme == null ? "stub" : enzyme.getName(), start, end);
    this.enzyme = enzyme;
    this.setFeatureType("cutsite");
  }
  
  public RestrictionEnzyme getRestrictionEnzyme() {
    return this.enzyme;
  }

  public boolean isPalindrome() {
    return palindrome;
  }

  public void setPalindromic(boolean palindrome) {
    this.palindrome = palindrome;
  }

  public void exportFragment(boolean export) {
    this.export = export;
  }

  public boolean exportFragment() {
    return export;
  }
}
