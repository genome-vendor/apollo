package apollo.gui.detailviewers.sequencealigner;

import apollo.datamodel.SeqFeatureI;

/** Inner class FeatureWrapper implements org.bdgp.util.Range, can be used in
 * RangeHash. Range is just getLow(),getHigh()
 * In a feature wrapper every one is
 * oriented 5' to 3' just as it is
 * in the display. In other words the
 * start position of the feature is
 * always the low end.
 */
class FeatureWrapper implements org.bdgp.util.Range {
  SeqFeatureI feature;
  TierI parent;

  public FeatureWrapper(SeqFeatureI feature, TierI parent) {
    this.feature = feature;
    this.parent = parent;
  }

  /** Range interface, returns the beginning of the feature which is
      for reverse strand its length - high */
  public int getLow() {
    //return parent.basePairToPos(feature.getStart());
    return parent.getPosition(feature.getStart());
  }
  /** Range interface */
  public int getHigh() {
    //return parent.basePairToPos(feature.getEnd());
    return parent.getPosition(feature.getEnd());

  }

  public SeqFeatureI getFeature() {
    return feature;
  }

  public TierI getParent() {
    return parent;
  }

  public String toString() {
    return "("+getLow()+","+getHigh()+")";
  }
}
