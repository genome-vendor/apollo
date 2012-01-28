package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

import apollo.datamodel.SeqFeatureI;

public class ComparatorFactory {
  
  public static enum TYPE { BASE, TYPE, LENGTH, NAME, SCORE, FRAME };
  
  public static Comparator<SeqFeatureI> makeComparatorSeqFeature(TYPE t) {
    switch (t) {
    case BASE: return new ComparatorBase<SeqFeatureI>();
    case TYPE: return new ComparatorSeqFeatureDisplayType();
    case LENGTH: return new ComparatorSeqFeatureLength();
    case NAME: return new ComparatorSeqFeatureName();
    case SCORE: return new ComparatorSeqFeatureScore();
    case FRAME: return new ComparatorSeqFeatureFrame();
    }
    
    throw new AssertionError("Invalid type: " + t);
  }
  
  public static Comparator<SeqFeatureI> makeReverseComparator(Comparator<SeqFeatureI> c) {
    return new ComparatorReverse<SeqFeatureI>(c);
  }

}
