package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.gui.*;

import java.util.*;
import java.awt.*;

public class PIDColourScheme extends ResidueColourScheme {

  public Color[] pidColours;
  public float[] thresholds;
    
  SequenceGroup group;

  public PIDColourScheme() {

    this.pidColours = ResidueProperties.pidColours;
    this.thresholds = ResidueProperties.pidThresholds;
  }


  public Color getColour(AlignSequenceI seq, int j,Vector aa) {

    Color  c = Color.white;
    String s = String.valueOf(seq.getBaseAt(j));

    //System.out.println("Getting colour for " + seq.getName() + " " + s + " " + j);

    Hashtable   hash = (Hashtable)aa.elementAt(j);
    Enumeration en   = hash.keys();

    while (en.hasMoreElements()) {
      String key = (String)en.nextElement();

      //System.out.println("Key " + key + " " + hash.get(key));
    }

    if (aa != null && j < aa.size()) {
      c = findColour(seq,s,j,aa);
    }
    
    return c;
  }


  /**
   * MC.  Ok so passing in a vector here is not nice.  The vector
   * contains one element per alignment column.  Each element is 
   * a hashtable with one key per residue letter.  The value for
   * each key is the number of occurrences of that residue in 
   * each column.  
   *
   * Two extra keys are available :
   *     maxResidue  - the residue with the maximum count
   *     maxCount    - ummm - not sure.
   *
   * This vector is calculated from the AAFrequency class.
   */

  public Color findColour(AlignSequenceI seq,String s, int j,Vector aa) {
    Color     c    = Color.white;
    Hashtable hash = null;

    if (aa != null) {
      hash = (Hashtable)aa.elementAt(j);
    }

    String    max  = (String)hash.get("maxResidue");
    double sc = 0;


    int maxCount = ((Integer)hash.get("maxCount")).intValue();
    int maxRes   = ((Integer)hash.get(max)).intValue();

    if (maxCount != -1  && hash.contains(s)) {
      sc = maxCount * 100.0/maxRes;

      if (! ResidueProperties.isGap(s)) {

	for (int i=0; i < thresholds.length; i++) {
	  if (sc > thresholds[i]) {
	    c = pidColours[i];
	    break;
	  }
	}
      } else {
	c = Color.white;
      }
    }
    return c;
  }
}
