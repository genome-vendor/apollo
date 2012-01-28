package jalview.datamodel;

import jalview.gui.*;
import jalview.gui.schemes.*;
import jalview.analysis.*;
import jalview.datamodel.*;

import java.util.Vector;
import java.awt.*;

public class SequenceGroup {
    boolean isSelected;
    boolean displayBoxes;
    boolean displayText;
    boolean colourText;
    boolean display;
    Conservation conserve;
    Vector   aaFrequency;
    boolean     aaFrequencyValid = false;
    Vector sequences = new Vector();
    int         width = -1;


  public SequenceGroup() {
    this.isSelected = false;
    this.displayBoxes = true;
    this.displayText = true;
    this.colourText = false;
    this.display = true;
  }

  public SequenceGroup( ColourSchemeI scheme, boolean isSelected,
                        boolean displayBoxes, boolean displayText,
                        boolean colourText,
                        boolean display) {

    this.isSelected = isSelected;
    this.displayBoxes = displayBoxes;
    this.displayText = displayText;
    this.colourText = colourText;
    this.display = display;
  }

  public Conservation getConservation() {
     return conserve;
  }
  public void addSequence(AlignSequenceI s) {
    sequences.addElement(s);
  }

  public void deleteSequence(AlignSequenceI s) {
    sequences.removeElement(s);
  }
 
  public void setColourText(boolean state) {
    colourText = state;
  }
  public boolean getColourText() {
    return colourText;
  }
 
  public void setDisplayText(boolean state) {
    displayText = state;
  }

  public boolean getDisplayText() {
    return displayText;
  }
 
  public void setDisplayBoxes(boolean state) {
    displayBoxes = state;
  }
 
  public boolean getDisplayBoxes() {
    return displayBoxes;
  }

    public int getSize() {
	return sequences.size();
    }
    public AlignSequenceI  getSequenceAt(int i) {
	return (AlignSequenceI)sequences.elementAt(i);
    }

    public Vector getAAFrequency() {
	if (aaFrequency == null || aaFrequencyValid == false) {
	    aaFrequency = AAFrequency.calculate(sequences,1,getWidth());
	    aaFrequencyValid = true;
	}
	return aaFrequency;
    }
    public int getWidth() {
	// MC This needs to get reset when characters are inserted and deleted
	if (width == -1) {
	    for (int i = 0; i < sequences.size(); i++) {
		AlignSequenceI seq = (AlignSequenceI)sequences.elementAt(i);
		if (seq.getLength() > width) {
		    width = seq.getLength();
		}
	    }
	}

	return width;
    }
}


