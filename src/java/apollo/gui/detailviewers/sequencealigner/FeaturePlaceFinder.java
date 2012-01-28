package apollo.gui.detailviewers.sequencealigner;

import java.util.List;

import apollo.datamodel.SeqFeatureI;

public abstract class FeaturePlaceFinder {
  
  public enum Type { COMPACT, SEMI_COMPACT };
  
  public abstract TierI findTierForFeature(List<TierI> tiers, SeqFeatureI feature);
  
  public static FeaturePlaceFinder createFinder(Type type) {
    
    switch(type) {
    case COMPACT: return new CompactFeaturePlaceFinder();
    case SEMI_COMPACT: return new SemiCompactFeaturePlaceFinder();
    }
    
    throw new IllegalArgumentException("not a valid type: " + type);
    
  }

}
