package apollo.datamodel;

import java.util.Vector;

public interface StrandedFeatureSetI extends FeatureSetI {
  public FeatureSetI getForwardSet();
  public FeatureSetI getReverseSet();
  public FeatureSetI getFeatSetForStrand(int strand);
}


