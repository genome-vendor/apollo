package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.gui.DrawableSequence;

import java.util.*;
import java.awt.*;


public class Blosum62ColourScheme extends ResidueColourScheme {

  public void setColours(DrawableSequence seq , int j, Vector aa) {
    Color c = Color.white;
    String s = String.valueOf(seq.getBaseAt(j));
    Hashtable hash = null;

    if (hash != null) {
      c = findColour(seq,s,j,aa);
    } else {
      c = Color.white;
    }
    seq.setResidueBoxColour(j,c);
  }

  public Color findColour(DrawableSequence seq, String s, int j, Vector aa) {
    if  ( !s.equals("-")  && !s.equals(".") && !s.equals(" ")) {

      Hashtable hash = null;
      
      if (aa != null) {
	  hash = (Hashtable)aa.elementAt(j);
      }

      String max = (String)hash.get("maxResidue");

      if (s.equals(max)) {
        return new Color(154,154,255);
      } else if (ResidueProperties.getBLOSUM62(max,s) > 0) {
        return new Color(204,204,255);
      } else {
        return Color.white;
      }
    } else {
      return Color.white;
    }
  }
  public boolean canThreshold() {
    return false;
  } 
}
