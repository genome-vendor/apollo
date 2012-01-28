package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

public class ComparatorSeqFeatureScore implements Comparator<SeqFeatureI> {

  public int compare(SeqFeatureI o1, SeqFeatureI o2) {
    int result = 0;
    
    
    FeatureProperty fp1 = Config.getPropertyScheme()
    .getFeatureProperty(o1.getTopLevelType());
    
    FeatureProperty fp2 = Config.getPropertyScheme()
    .getFeatureProperty(o2.getTopLevelType());
    
    if (!fp1.getDisplayType().equals("Oligo") 
        && !fp2.getDisplayType().equals("Oligo")) {
      double s1 = o1.getScore();
      double s2 = o2.getScore();
      String n1 = o1.getName();
      String n2 = o2.getName();
      int j1 = 1;
    }
    
    if (o1.getScore() < o2.getScore()) {
      result = 1;
    } else if (o1.getScore() > o2.getScore()) {
      result = -1;
    }
    
    return result;
  }

}
