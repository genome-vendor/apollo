package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.gui.DrawableSequence;

import java.util.*;
import java.awt.*;


public class SecondaryColourScheme extends ResidueColourScheme {

  public Color getColor(DrawableSequence seq, int j, Vector aa) {
    Color c = Color.white;

    String s = String.valueOf(seq.getBaseAt(j));

    if (ResidueProperties.ssHash.containsKey(s)) {
	c = (Color)ResidueProperties.ssHash.get(s);
    } else {
        c = Color.white;
    }
    return c;
  }
}





