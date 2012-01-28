package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.gui.*;

import java.util.*;
import java.awt.*;
import MCview.*;

public class ResidueColourScheme implements ColourSchemeI {
  Color    [] colors;
  int      threshold = 50;

    public ResidueColourScheme(Color[] colors, int threshold) {
	this.colors    = colors;
	this.threshold = threshold;
    }
    
    public ResidueColourScheme() {
    }
    public Color findColour(AlignSequenceI seq,String s, int j, Vector aa) {
	try {
	    return colors[((Integer)(ResidueProperties.aaHash.get(s))).intValue()];
	} catch (Exception e) {
	    return Color.white;
	}
    }

    // aa should maybe be a class
    public Color getColour(AlignSequenceI seq, int j,Vector aa) {

	long start = System.currentTimeMillis();
	long end;

	Color  c       = Color.white;
	String s       = String.valueOf(seq.getBaseAt(j));

	if (threshold > 0 && aa != null) {
	    if (aboveThreshold(aa,seq,j,threshold)) {
		c = findColour(seq,s,j,aa);
	    }
	} else {
	    c = findColour(seq,s,j,aa);
	}

      return c;
    }
    public int getThreshold() {
	return threshold;
    }
    
    public void setThreshold(int ct) {
	threshold = ct;
    }

    public Vector  getColours(AlignSequenceI s, Vector aa) {
	Vector colours = new Vector();

	long start = System.currentTimeMillis();
	long end;
	for (int j = 0; j < s.getLength(); j++) {
	  colours.addElement(getColour(s,j,aa));
	}
	return colours;
    }

    public Vector getColours(SequenceGroup sg, Vector aa) {
	Vector colours = new Vector();

	for (int j = 0; j < sg.getSize(); j++) {
	    AlignSequenceI s = sg.getSequenceAt(j);

	    for (int i = 0; i < s.getLength();i++) {
		colours.addElement(getColour(s,i,aa));
	    }
	}
	return colours;
    }

    public boolean aboveThreshold(Vector aa,AlignSequenceI seq, int j, int threshold) {
	String    s    = String.valueOf (seq.getBaseAt(j));
	Hashtable hash = (Hashtable)aa.elementAt(j);

	if (j < aa.size()) {
	    double sc = 0;

	    if (((Integer)hash.get("maxCount")).intValue() != -1  && hash.contains(s)) {
		int maxCount = ((Integer)hash.get("maxCount")).intValue();
		int resCount = ((Integer)hash.get(s)).intValue();

		sc = resCount * 100.0 / maxCount;
		

		if  ( ! ResidueProperties.isGap(s)) {
		    if (sc >= (double)threshold) {
			return true;
		    }
		}
	    }
	}
	return false;
    }

    public boolean canThreshold() {
	return true;
    }
    public boolean isUserDefinable() {
	return false;
    }
}
