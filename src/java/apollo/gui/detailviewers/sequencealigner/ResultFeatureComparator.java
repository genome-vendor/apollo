package apollo.gui.detailviewers.sequencealigner;

import java.util.Comparator;

import apollo.datamodel.SeqFeatureI;


public class ResultFeatureComparator implements Comparator {

  public int compare(Object o1, Object o2) {
    SeqFeatureI f1 = (SeqFeatureI) o1;
    SeqFeatureI f2 = (SeqFeatureI) o2;
    
    // the feature lengths
    int l1 = f1.getHigh() - f1.getLow();
    int l2 = f2.getHigh() - f2.getLow();
    
    int result = 0;
    
    if (f1.getScore() < f2.getScore()) {
      result = 1;
    } else if (f1.getScore() > f2.getScore()) {
      result = -1;
    } else if (l1 < l2) {
      result = 1;
    } else if (l1 > l2) {
      result = -1;
    } else {
      result = f1.getName().compareToIgnoreCase(f2.getName());
    }
    
    return result;
  }

}
