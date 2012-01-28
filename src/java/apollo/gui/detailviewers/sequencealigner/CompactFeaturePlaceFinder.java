package apollo.gui.detailviewers.sequencealigner;

import java.util.ArrayList;
import java.util.List;

import apollo.datamodel.SeqFeatureI;

/**
 * This class encapsulates a 'compact' place finding algorithm.
 * It will find the first tier that has room to place the given
 * feature without overlapping any other features.
 *
 */
public class CompactFeaturePlaceFinder extends FeaturePlaceFinder {
  
  
  public TierI findTierForFeature(List<TierI> tiers, SeqFeatureI feature) {
    for(TierI tier : tiers) {
      if (!tier.willOverlap(feature)) {
        return tier;
      }
    }
    return null;
  }

}
