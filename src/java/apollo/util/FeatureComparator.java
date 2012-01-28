package apollo.util;

import java.util.*;
import apollo.datamodel.SeqFeatureI;

public class FeatureComparator implements org.bdgp.util.Comparator {
  public int compare(Object oa, Object ob) {
    if (oa instanceof SeqFeatureI && ob instanceof SeqFeatureI) {
      SeqFeatureI a = (SeqFeatureI) oa;
      SeqFeatureI b = (SeqFeatureI) ob;
      if (a.getStrand() < b.getStrand())
        return -1;
      else if (a.getStrand() > b.getStrand())
        return 1;
      else {
        if (a.getLow() < b.getLow())
          return -1;
        else if (a.getLow() > b.getLow())
          return 1;
        else {
          if (a.getHigh() < b.getHigh())
            return -1;
          else if (a.getHigh() > b.getHigh())
            return 1;
          else
            return 0;
        }
      }
    } else
      return -1;
  }
}

