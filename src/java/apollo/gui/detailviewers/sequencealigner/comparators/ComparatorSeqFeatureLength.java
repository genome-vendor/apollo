package apollo.gui.detailviewers.sequencealigner.comparators;

import java.util.Comparator;

import apollo.datamodel.SeqFeatureI;

public class ComparatorSeqFeatureLength  implements Comparator<SeqFeatureI> {
  
  public int compare(SeqFeatureI o1, SeqFeatureI o2) {
    int result = 0;
    
    // the feature lengths
    int l1 = o1.getHigh() - o1.getLow() + 1;
    int l2 = o2.getHigh() - o2.getLow() + 1;
    
    if (l1 < l2) {
      result = 1;
    } else if (l1 > l2) {
      result = -1;
    }
    
    return result;
  }

}
