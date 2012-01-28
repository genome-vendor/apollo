package jalview.util;

import jalview.datamodel.*;

public class Comparison {


  public static float compare(AlignSequenceI ii, AlignSequenceI jj) {
    String si   = ii.getResidues();
    String sj   = jj.getResidues();
    int    ilen = ii.getLength();
    int    jlen = jj.getLength();


    if ( si.substring(ilen).equals("-") ||
         si.substring(ilen).equals(".") ||
         si.substring(ilen).equals(" ")) {

      ilen--;

      while (si.substring(ilen,ilen+1).equals("-")  ||
             si.substring(ilen,ilen+1).equals(".")  ||
             si.substring(ilen,ilen+1).equals(" ")) {
        ilen--;
      }
    }

    if ( sj.substring(jlen).equals("-")  ||
         sj.substring(jlen).equals(".")  ||
         sj.substring(jlen).equals(" ")) {
      jlen--;

      while (sj.substring(jlen,jlen+1).equals("-")  ||
             sj.substring(jlen,jlen+1).equals(".")  ||
             sj.substring(jlen,jlen+1).equals(" ")) {
        jlen--;
      }
    }

    int   count = 0;
    int   match = 0;
    float pid   = -1;

    if (ilen > jlen) {

      for (int j = 0; j < jlen; j++) {
        if (si.substring(j,j+1).equals(sj.substring(j,j+1))) {
          match++;
        }
        count++;
      }
      pid = (float)match/(float)ilen * 100;
    } else {
      for (int j = 0; j < jlen; j++) {
        if (si.substring(j,j+1).equals(sj.substring(j,j+1))) {
          match++;
        }
        count++;
      }
      pid = (float)match/(float)jlen * 100;
    }

    return pid;
  }

  /**    */
  public static float PID(AlignSequenceI s1 , AlignSequenceI s2) {
    int res = 0;
    int len = (s1.getLength() > s2.getLength() ?
	       s1.getLength() : s2.getLength());
    int bad = 0;
    for (int i = 0; i < len; i++) {
      if (!s1.residueIsSpacer(i) && !s2.residueIsSpacer(i)) {
        if (s1.getBaseAt(i) != s2.getBaseAt(i)) {
          bad++;
        }
      }
    }

    return (float)100*(len-bad)/len;
  }
}
