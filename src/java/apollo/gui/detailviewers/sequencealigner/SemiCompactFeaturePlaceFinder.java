package apollo.gui.detailviewers.sequencealigner;

import java.util.ArrayList;
import java.util.List;

import apollo.datamodel.SeqFeatureI;

/**
 * 
 * This class encapsulates a 'semi compact' place finding algorithm.
 * It will find the tier that has room to place the given feature
 * above all other previously previously placed features that are
 * within its range
 *
 */
public class SemiCompactFeaturePlaceFinder extends FeaturePlaceFinder {
  
  public TierI findTierForFeature(List<TierI> tiers, SeqFeatureI feature) {
    TierI prev = null;
    
    
    // starting from the top 
    // iterate through the tiers until you find one that will overlap
    // then return the tier just before it
    for (int i = tiers.size()-1; i >= 0; i--) {
      TierI tier = tiers.get(i);
      if (tier.willOverlap(feature)) {
        return prev;
      }
      prev = tier;
    }
    
    if (prev != null && !prev.willOverlap(feature)) {
      return prev;
    }
    
    return null;
  }

}
