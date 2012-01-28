package jalview.gui;

import jalview.datamodel.*;
import jalview.gui.schemes.*;
import jalview.analysis.*;

import java.awt.*;
import java.util.*;

public class DrawableAlignment implements AlignmentI {
  Alignment          align;
  Hashtable          colourSchemes;

  public DrawableAlignment(Alignment align) {
    colourSchemes = new Hashtable();

    DrawableSequence[] ds = new DrawableSequence[align.getHeight()];
	
    for (int i=0; i < align.getHeight(); i++) {
      if (!(align.getSequenceAt(i) instanceof DrawableSequence)) {
	DrawableSequence tmp = 
	  new DrawableSequence((AlignSequenceI)align.getSequenceAt(i));
	ds[i] = tmp;
      } else {
	ds[i] = (DrawableSequence)align.getSequenceAt(i);
      }
    }

    this.align = new Alignment(ds);

    // Need a config here to get the default ColourScheme
    for (int i = 0; i < align.getGroups().size(); i++) {
      SequenceGroup group = (SequenceGroup)align.getGroups().elementAt(i);
      ZappoColourScheme tmp = new ZappoColourScheme();
      colourSchemes.put(group,tmp);
    }

  }

  public DrawableAlignment(DrawableSequence[] ds) {
    align  = new Alignment(ds);
    colourSchemes = new Hashtable();
  }
    
  public int getPixelHeight(int charHeight) {
    int i = 0;
    int h = 0;
    
    while (i < getHeight()) {
      h += charHeight;
      i++;
    }
    return h;
  }

  public int getPixelHeight(int i, int j,int charHeight) {
    int h=0;
    while (i < j) {
      h += charHeight;
      i++;
    }
    return h;
  }
    
  public int getPixelHeight(int i,int charHeight) {
    int j = 0;
    int h = 0;
    while (j < i && j < getHeight()) {
      if (j > 0) {
	h += charHeight;
      }
      j++;
    }
    return h;
  }
    
  public Vector getGroups() {
    return align.getGroups();
  }
  public void setGroups(Vector groups) {
    align.setGroups(groups);
  }

  public int getPixelHeight(DrawableSequence s) {
    int i = 0;
    int h = 0;
	
    while (i < getHeight()) {
      if (getDrawableSequenceAt(i) != s) {
	h+= getDrawableSequenceAt(i).charHeight;
      }
      i++;
    }
    return h;
  }
    

  public void setThreshold(int thresh) {
    Vector sg = align.getGroups();

    for (int i = 0 ; i < sg.size(); i++) {
      SequenceGroup g = (SequenceGroup)sg.elementAt(i);
      ColourSchemeI cs = (ColourSchemeI)colourSchemes.get(g);

      if (cs instanceof ResidueColourScheme) {
	if (Config.DEBUG) System.out.println("Setting threshold " + thresh);
	((ResidueColourScheme)cs).setThreshold(thresh);
	setColourScheme(g,cs);
      }
    }
  }

  public void setColourScheme(SequenceGroup sg, ColourSchemeI cs) {
    colourSchemes.put(sg,cs);
    setColourScheme(sg);
  }

  public void setColourScheme(SequenceGroup sg) {

    ColourSchemeI cs = (ColourSchemeI)colourSchemes.get(sg);

    long start = System.currentTimeMillis();
    long end;

    for (int i = 0 ; i < sg.getSize(); i++) {


      AlignSequenceI seq  = sg.getSequenceAt(i);
      Vector colours = cs.getColours(seq,getAAFrequency());

      for (int j = 0; j < colours.size(); j++) {
	DrawableSequence dseq = (DrawableSequence)seq;
	Color col = (Color)colours.elementAt(j);
	dseq.setResidueBoxColour(j,col);
      }
    }
  }
    
  public void setColourScheme(ColourSchemeI colourScheme) {
    for (int i=0 ; i < align.getGroups().size(); i++) {
      SequenceGroup sg = (SequenceGroup)align.getGroups().elementAt(i);
	    
      // Also need to set colours here
      colourSchemes.put(sg,colourScheme);
      setColourScheme(sg);
    }
  }
    
  public int maxIdLength(FontMetrics fm) {
    int i   = 0;
    int max = 0;
	
    while(i < getHeight()) {
      AlignSequenceI s   = getSequenceAt(i);
      String    tmp = s.getName() + "/" + s.getStart() + "-" + s.getEnd();
	    
      if (fm.stringWidth(tmp ) > max) {
	max = fm.stringWidth(tmp);
      }
      i++;
    }
    return max;
  }
  public DrawableSequence getDrawableSequenceAt(int j) {
    return (DrawableSequence)getSequenceAt(j);
  }
    
    
  public void removeGappedColumns() {
    Vector v = new Vector(getWidth());
	
    for (int i=0; i < getWidth(); i++) {
      boolean gap = true;
	    
      for (int j=0; j < getHeight(); j++) {
	char tmp = getSequenceAt(j).getBaseAt(i);
	if (!(tmp == '-' || tmp == '.' || tmp == ' ')) {
	  gap = false;
	  break;
	}
      }
	    
      if (gap) {
	v.addElement("0");
      } else {
	v.addElement("1");
      }
    }
	
    for (int j = 0; j < v.size(); j++) {
      if ((String)v.elementAt(j) == "0") {
		
	deleteColumns(j,j);
	v.removeElementAt(j);
	j--;
      }
    }
  }
    
  public void deleteColumns(int start, int end) {
    align.deleteColumns(start,end);
    int i=0;
	
    while (i < getHeight()) {
      for (int j = start; j <= end; j++) {
		
	int l=0;
	while (getSequenceAt(i).getScores() != null &&
	       l < getSequenceAt(i).getScores().length &&
	       getSequenceAt(i).getScores()[l] != null) {
	  if (getSequenceAt(i).getScores()[l].size() > start) {
	    getSequenceAt(i).getScores()[l].removeElementAt(start);
	  }
	  l++;
	}
      }
	    
      i++;
    }
  }
    
  // AlignmentI methods
  public int         getHeight() {
    return align.getHeight();
  }
  public int         getWidth() {
    return align.getWidth();
  }
  public int         getMaxIdLength() {
    return align.getMaxIdLength();
  }

  public Vector  getSequences() {
    return align.getSequences();
  }
  public AlignSequenceI   getSequenceAt(int i) {
    return align.getSequenceAt(i);
  }

  public void        addSequence(AlignSequenceI seq) {
    align.addSequence(seq);
  }

  public void        setSequenceAt(int i,AlignSequenceI seq) {
    align.setSequenceAt(i,seq);
  }

  public void addSequence(AlignSequenceI[] seq) {
    for (int i=0; i < seq.length; i++) {
      addSequence(seq[i]);
    }
  }

  public void deleteSequence(AlignSequenceI s) {
    for (int i = 0; i < getHeight(); i++) {
      if (getSequenceAt(i) == s) {
        deleteSequence(i);
      }
    }
  }

  public void  deleteSequence(int i) {
    align.deleteSequence(i);
  }

  public AlignSequenceI[] getColumns(int start, int end) {
    return align.getColumns(start,end);
  }
  public AlignSequenceI[] getColumns(int seq1, int seq2, int start, int end) {
    return align.getColumns(seq1,seq2,start,end);
  }

  public void        deleteColumns(int seq1, int seq2, int start, int end) {
    align.deleteColumns(seq1,seq2,start,end);
  }

  public void        insertColumns(AlignSequenceI[] seqs, int pos) {
    align.insertColumns(seqs,pos);
  }

  public AlignSequenceI   findName(String name) {
    return align.findName(name);
  }
  public int         findIndex(AlignSequenceI s) {
    return align.findIndex(s);
  }

  // Modifying
  public void        removeGaps() {
    align.removeGaps();
  }
  public Vector      removeRedundancy(float threshold, Vector sel) {
    return align.removeRedundancy(threshold,sel);
  }


  // Grouping methods
  public SequenceGroup findGroup(int i) {
    return align.findGroup(i);
  }
  public SequenceGroup findGroup(AlignSequenceI s) {
    return align.findGroup(s);
  }
  public void          addToGroup(SequenceGroup g, AlignSequenceI s) {
    align.addToGroup(g,s);
  }
  public void          removeFromGroup(SequenceGroup g,AlignSequenceI s) {
    align.removeFromGroup(g,s);
  }
  public void          addGroup(SequenceGroup sg) {
    align.addGroup(sg);
  }
  public SequenceGroup addGroup() {
    return align.addGroup();
  }
  public void          deleteGroup(SequenceGroup g) {
    align.deleteGroup(g);
  }

  public Hashtable[]   removeArrayElement(Hashtable[] cons, int i) {
    return align.removeArrayElement(cons,i);
  }
  public void          removeIntArrayColumn(int[][] cons2, int start) {
    align.removeIntArrayColumn(cons2,start);
  }

  public void trimLeft(int i) {

    align.trimLeft(i);

    for (int j = 0; j < getHeight(); j++) {

      for (int k = 0; k < i; k++) {
        if (getDrawableSequenceAt(j).textColour.size() > k) {
          getDrawableSequenceAt(j).textColour.removeElementAt(0);
        }

        if (getDrawableSequenceAt(j).boxColour.size() > k) {
          getDrawableSequenceAt(j).boxColour.removeElementAt(0);
        }

        int l=0;

        while (l < getSequenceAt(j).getScores().length &&
               getSequenceAt(j).getScores()[l] != null) {
          if (getSequenceAt(j).getScores()[l].size() > k) {
            getSequenceAt(j).getScores()[l].removeElementAt(0);
          }
          l++;
        }
      }
    }
  }

  public void trimRight(int i) {
    align.trimRight(i);

    for (int j = 0; j < getHeight(); j++) {
      int length = getSequenceAt(j).getLength();
      for (int k = i+1; k < length; k++) {
        if (getDrawableSequenceAt(j).textColour.size() > (i+1)) {
          getDrawableSequenceAt(j).textColour.removeElementAt(i+1);
        }
        if (getDrawableSequenceAt(j).boxColour.size() > (i+1)) {
          getDrawableSequenceAt(j).boxColour.removeElementAt(i+1);
        }
        int l=0;
        while (l < getDrawableSequenceAt(j).getScores().length &&
               getDrawableSequenceAt(j).getScores()[l] != null) {
          if (getSequenceAt(j).getScores()[l].size() > (i+1)) {
            getSequenceAt(j).getScores()[l].removeElementAt(i+1);
          }
          l++;
        }
      }
    }
  }

  // Sorting
  public void          sortGroups() {
    align.sortGroups();
  }
  public void          sortByPID(AlignSequenceI s) {
    align.sortByPID(s);
  }
  public void          sortByID() {
    align.sortByID();
  }


  // Conservation
  public void          percentIdentity(Vector sel) {
    align.percentIdentity(sel);
  }
  public void          percentIdentity2(int start, int end, Vector sel) {
    align.percentIdentity2(start,end,sel);
  }
  public void          percentIdentity2() {
    align.percentIdentity2();
  }
  public void          percentIdentity2(int start, int end) {
    align.percentIdentity2(start,end);
  }
  public void          percentIdentity(int start, int end, Vector sel) {
    align.percentIdentity(start,end,sel);
  }
  public void          percentIdentity() {
    align.percentIdentity();
  }

  public void setColourText(boolean state) {
    for (int i=0 ; i < align.getGroups().size(); i++) {
      SequenceGroup sg = (SequenceGroup)align.getGroups().elementAt(i);
      sg.setColourText(state);
    }
    for (int i=0 ; i < align.getSequences().size(); i++) {
      DrawableSequence seq = getDrawableSequenceAt(i);
      seq.setColourText(state);
    }
  }

  public void setDisplayBoxes(boolean state) {
    for (int i=0 ; i < align.getGroups().size(); i++) {
      SequenceGroup sg = (SequenceGroup)align.getGroups().elementAt(i);
      sg.setDisplayBoxes(state);
    }
    for (int i=0 ; i < align.getSequences().size(); i++) {
      DrawableSequence seq = getDrawableSequenceAt(i);
      seq.setDisplayBoxes(state);
    }
  }

  public void setDisplayText(boolean state) {
    for (int i=0 ; i < align.getGroups().size(); i++) {
      SequenceGroup sg = (SequenceGroup)align.getGroups().elementAt(i);
      sg.setDisplayText(state);
    }
    for (int i=0 ; i < align.getSequences().size(); i++) {
      DrawableSequence seq = getDrawableSequenceAt(i);
      seq.setDisplayText(state);
    }
  }

  public void setFastDraw(boolean state) {
    for (int i=0 ; i < align.getSequences().size(); i++) {
      DrawableSequence seq = getDrawableSequenceAt(i);
      seq.setFastDraw(state);
    }
  }

  public void colourText(SequenceGroup sg) {
    System.err.println("DrawableAlignment.colourText(SequenceGroup sg) is dummy method");
  }
 
  public void displayText(SequenceGroup sg) {
    System.err.println("DrawableAlignment.displayText(SequenceGroup sg) is dummy method");
  }
 
  public void displayBoxes(SequenceGroup sg) {
    System.err.println("DrawableAlignment.displayBoxes(SequenceGroup sg) is dummy method");
  }

  public void setGapCharacter(String gc) {
    align.setGapCharacter(gc);
  }
  public String getGapCharacter() {
    return align.getGapCharacter();
  }
  public Vector getAAFrequency() {
    return align.getAAFrequency();
  }
  public AlignmentI getAlignment() {
    return align;
  }
}










