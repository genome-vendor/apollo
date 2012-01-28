package apollo.dataadapter;

import java.util.*;

import apollo.datamodel.*;

public class KaryotypeUtil {
  public static void sortByChrName(Vector chrs) {
    Collections.sort(chrs, new ChrComparator());
  }
}

class ChrComparator implements Comparator{
  public int compare(Object o1, Object o2) {
    String chr1Name = ((Chromosome)o1).getDisplayId();
    String chr2Name = ((Chromosome)o2).getDisplayId();

    int chr1Num = -1000;
    int chr2Num = -1000;

    try {
      chr1Num = Integer.parseInt(chr1Name);
    } catch (Exception e) {
    }
    try {
      chr2Num = Integer.parseInt(chr2Name);
    } catch (Exception e) {
    }

    if (chr1Num != -1000 &&
        chr2Num != -1000) {
      return (chr1Num - chr2Num);
    } else if (chr1Num != -1000) {
      return -1;
    } else if (chr2Num != -1000) {
      return 1;
    } else {
      return chr1Name.compareTo(chr2Name);
    }
  }
}
