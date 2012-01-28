package jalview.analysis;

import jalview.gui.*;
import jalview.datamodel.*;
import jalview.io.*;

import java.awt.*;
import java.applet.Applet;
import java.util.*;
import java.net.*;
import java.io.*;

public class AAFrequency {

    // Takes in a vector of sequences and column start and column end
    // and returns a vector of size (end-start+1). Each element of the
    // vector contains a hashtable with the keys being residues and
    // the values being the count of each residue in that column.
    // This class is used extensively in calculating alignment colourschemes
    // that depend on the amount of conservation in each alignment column.

  public static Vector calculate(Vector sequences,int start,int end) {

    Vector result = new Vector();

    for (int i = start-1;i < end; i++) {


      Hashtable residueHash = new Hashtable();
      int       maxCount    = -1;
      String    maxResidue  = "-";
      
      for (int j=0; j < sequences.size(); j++) {

        if (sequences.elementAt(j) instanceof AlignSequenceI) {
          AlignSequenceI s = (AlignSequenceI)sequences.elementAt(j);


          if (s.getLength() > i) {

            String res = String.valueOf(s.getBaseAt(i));

            if (residueHash.containsKey(res)) {

              int count = ((Integer)residueHash.get(res)).intValue() ;
              count++;

	      if (count >= maxCount) {
		  maxCount = count;
		  maxResidue = res;
	      }

              residueHash.put(res,new Integer(count));
            } else {
              residueHash.put(res,new Integer(1));

	      maxCount   = 1;
	      maxResidue = res;

            }

          } else {
	    int count;
            if (residueHash.containsKey("-")) {
              count = ((Integer)residueHash.get("-")).intValue() ;
              count++;
              residueHash.put("-",new Integer(count));
            } else {
              residueHash.put("-",new Integer(1));
	      count = 1;
            }

	    if (count >= maxCount) {
	      maxCount = count;
	      maxResidue = "-";
	    }
          }
        }
      }
      residueHash.put("maxCount",new Integer(maxCount));
      residueHash.put("maxResidue", maxResidue);

      result.addElement(residueHash);
    }

    return result;
  }

  public static void print(Vector aa) {

    for (int i = 0; i < aa.size(); i++) {
      System.out.println("Column " + i);

      Hashtable hash = (Hashtable)aa.get(i);

      Enumeration en = hash.keys();

      while (en.hasMoreElements()) {

	String key = (String)en.nextElement();
	System.out.println("Key " + key);

	if (hash.get(key) instanceof Integer) {
	  int    val = ((Integer)hash.get(key)).intValue();
	  System.out.println("Residue " + key + " count " + val);

	} else {
	  System.out.println("Residue " + key + " " + hash.get(key));
	}
      }
    }
  }
	


    
}
