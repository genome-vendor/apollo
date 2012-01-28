package apollo.util;

import java.util.*;
import apollo.datamodel.SeqFeatureI;

public class FeatureAlphabeticalComparator implements Comparator {
    public int compare(Object oa, Object ob) {
      if (oa instanceof SeqFeatureI && ob instanceof SeqFeatureI) {
	SeqFeatureI a = (SeqFeatureI) oa;
	SeqFeatureI b = (SeqFeatureI) ob;
	String name_a = a.getName();
	String name_b = b.getName();
	if (name_a == null)
	  return 1;
	else if (name_b == null)
	  return -1;
	else {
	  int comp = name_a.compareToIgnoreCase(name_b);
	  if (comp < 0)
	    return -1;
	  else if (comp > 0)
	    return 1;
	  else
	    return 0;
	}
      }
      else
	return -1;
    }
}
