package jalview.analysis;

import jalview.datamodel.*;

import java.util.*;

public class AlignmentUtil {

  private AlignmentUtil() {
  }

  public static int[][] percentIdentity2(AlignmentI align) {
    return percentIdentity2(align,0,align.getWidth()-1);
  }
 
  public static int[][] percentIdentity2(AlignmentI align, int start, int end) {
    int [][] cons2 = new int[align.getWidth()][24];
    // Initialize the array
    for (int j=0;j<24;j++) {
      for (int i=0; i < align.getWidth();i++) {
        cons2[i][j] = 0;
      }
    }
 
    for (int j=0;j<align.getHeight();j++) {
      AlignSequenceI seq = align.getSequenceAt(j);
      for (int i = start; i <= end; i++) {
        cons2[i][seq.getNum(i)]++;
      }
    }
    return cons2;
  }
}
