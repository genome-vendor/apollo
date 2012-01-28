package apollo.gui;

import apollo.datamodel.SeqFeatureI;

/** Interface for JFrames that will respond when a interior
    component wants them to do something with a feature
    that has been selected. Only has one method so far
    Now used in FeatureEditor and FeatureTreeFrame
*/
public interface FeatureNavigationI {
  public void featureSelected(SeqFeatureI sf);
}
