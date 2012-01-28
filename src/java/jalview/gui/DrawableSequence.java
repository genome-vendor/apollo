package jalview.gui;

import jalview.datamodel.*;
import jalview.util.*;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.RangeI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.DbXref;

import MCview.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Vector;
import java.util.Date;
import java.util.Hashtable;


/** This should be a HasA with AlignSequenceI(& SequenceI), not a ISA. as ISA it muddles
 the model and the view. If you have a SequenceI you dont know if its a model or 
 view object. */
public class DrawableSequence implements AlignSequenceI {
  protected int     charHeight;

  protected boolean fastDraw = true;
  protected boolean displayBoxes;
  protected boolean displayText;
  protected boolean colourText;

  protected Vector  textColour;
  protected Vector  boxColour;
  protected Color[] boxColourArray;

  protected Color   color = Color.black;

  protected Font    font;
  protected String  fontName;
  protected int     fontSize;
  protected int     fontStyle;

  protected int     padx = 0;
  protected int     pady = 2;

  //protected PDBfile pdb;
  protected int     maxchain = -1;

  protected int     pdbstart;
  protected int     pdbend;
  protected int     seqstart;
  protected int     seqend;

  protected String  type;

  protected AlignSequenceI sequence;

  public DrawableSequence(AlignSequenceI s) {
    this.sequence = s;

    _init();
  }

  public DrawableSequence(String name,String seq, int start, int end) {
    sequence   = new AlignSequence(name,seq,start,end);

    _init();
  }

  private void _init() {
    textColour = new Vector(sequence.getLength());
    boxColour  = new Vector(sequence.getLength());
    boxColourArray = new Color[sequence.getLength()];

    displayBoxes = true;
    displayText  = true;
    colourText   = false;

    setFont("Dialog",Font.PLAIN,14);
  }

  public void setColourText(boolean state) {
    colourText = state;
  }
  public void setDisplayBoxes(boolean state) {
    displayBoxes = state;
  }
  public void setDisplayText(boolean state) {
    displayText = state;
  }

  public void setFastDraw(boolean state) {
    fastDraw = state;
  }

  public Font getFont() {
    return this.font;
  }

  public void setFont(String name,int style, int size) {
    this.font = new Font(name,style,size);
    fontName = name;
    fontStyle = style;
    fontSize = size;
  }

  public void setFontSize(int fontSize) {
    this.fontSize = fontSize;
  }

  public Color getResidueTextColour(int i) {
    return (Color)textColour.elementAt(i);
  }
  public void setResidueTextColour(int i,Color c) {
    if (textColour.size() <= i) {
      for (int j = textColour.size(); j <= i; j++) {
        textColour.addElement(null);
      }
    }
    textColour.setElementAt(c,i);
  }

  public Color getResidueBoxColour(int i) {
    return (Color)boxColour.elementAt(i);
  }

  public void setResidueBoxColour(int i, Color c) {
    int pos = i;

    if (i > boxColour.size()-1) {
      int j = boxColour.size();
      while (j <=  i) {
        boxColour.addElement(Color.white);
	//	boxColourArray[j] = Color.white;
        j++;
      }
    }
    boxColour.setElementAt(c,i);
    //    boxColourArray[i] = c;

  }

  public void drawSequence(Graphics g,int start, int end, int x1, int y1, int width, int height,boolean showScores) {
    if (!font.getName().equals("Courier")) {
      padx = 1;
    } else {
      padx = 1;
    }

    if (displayBoxes == true) {
      drawBoxes(g,start,end,x1,y1,width, height);
    }
    if (displayText == true) {
      if (colourText == false) {
        drawText(g,start,end,x1,y1,width,height);
      } else {
        drawColourText(g,start,end,x1,y1,width,height);
      }
	
    }
  }

  public void drawBoxes(Graphics g,int start, int end, int x1, int y1, int width, int height) {
    int i      = start;
    int length = sequence.getLength();

    Color currentColor = Color.white;

    int curStart = x1;
    int curWidth = width;
 
    while (i <= end && i < length) {
	      Color c = getResidueBoxColour(i);
	//Color c = boxColourArray[i];

      if (c != currentColor || c != null) {
	  /**
	     System.out.println("Filling " + i + 
			     " x1 "     + x1 + 
			     " curStart " + curStart + 
			     " start " + start + 
			     " y1 "     + y1 + 
			     " curWidth " + curWidth + 
			     " height " + height);
	  */
	  //g.fillRect(x1+width*(curStart-start),y1,curWidth-3,height);
	  //currentColor = c;
        g.setColor(c);

        curStart = i;
        curWidth = width;
      } else {
        curWidth += width;
      }

      g.fillRect(x1+width*(i-start),y1,width,height);
      
      i++;
    }

    g.fillRect(x1+width*(curStart-start),y1,curWidth,height);
  }

  public AlignSequenceI getSequenceObj() {
    return sequence;
  }
  public void drawText(Graphics g, int start, int end, int x1, int y1, int width, int height) {

    g.setColor(Color.black);

    // Need to find the sequence position here.

    // Should only do fastDraw if it is a fixed width font
    //if (fastDraw) {
    if (isFixedWidthFont(g.getFontMetrics())) {
      String s = getResidues (start, end);
      g.drawString(s,x1,y1+height-pady);
    } 
    // Not a fixed width font - slower drawing
    else {
      if (Debug.getDebugLevel() > 0) {
        System.out.println("Drawing sequence: ");
      }
      for (int i=start; i <= end; i++) {
        String s = getResidues(i,i+1);
        if (Debug.getDebugLevel() > 0) {
          System.out.print("|" + s + "|");
        }
        g.drawString(s,x1+width*(i-start),y1+height-pady);
      }
      if (Debug.getDebugLevel() > 0) {
        System.out.println("");
      }
    }
  }

  public static boolean isFixedWidthFont(FontMetrics fm) {
    // oddly for courier which i believe is fixed width '.' and 'A' are 7 and
    // max advance is 17. max advance doesnt seem the way to go.
    //return fm.charWidth('.') == fm.getMaxAdvance();
    // I think this should work for our purposes
    return fm.charWidth('.') == fm.charWidth('A');
  }
  

  public int getPosition(int res) {

    return res;
  }

  public int getResidue(int pos) {

    return pos;
  }


  public void drawColourText(Graphics g, 
			     int start, int end, 
			     int x1, int y1, int width, int height) {
    int length = sequence.getLength();
    if (start < length) {
      int i = start;
      while (i < length && i <= (end) && i < boxColour.size()) {
        if (fastDraw) {
          char c = getBaseAt(i);
          g.setColor(((Color)boxColour.elementAt(i)).darker());
          g.drawString(String.valueOf(c),x1+width*(i-start),y1+height-pady);
        } else {
          char c = getBaseAt(i);
          g.setColor(((Color)boxColour.elementAt(i)).darker());
          g.drawString(String.valueOf(c),x1+width*(i-start)+padx,y1+height-pady);
        }
        i++;
      }
    }
  }

  // SequenceI methods

  public String      getName() {
    return sequence.getName();
  }
  public String      getDisplayId() {
    return sequence.getName();
  }

  public void        setStart(int start) {
    sequence.setStart(start);
  }
  public int         getStart() {
    return sequence.getStart();
  }
  public void        setEnd(int end) {
    sequence.setEnd(end);
  }
  public int         getEnd() {
    return sequence.getEnd();
  }

  public int         getLength() {
    return sequence.getLength();
  }
  public void        setResidues(String seq) {
    sequence.setResidues(seq);
  }
  public String      getResidues() {
    return sequence.getResidues();
  }
  public String      getResidues(int start,int end) {
    return sequence.getResidues(start,end);
  }

  /** This is due to DrawableSequence implementing AlignSequenceI, which it shouldnt
      it should be a HASA! */
  public boolean hasResidues() { return sequence.hasResidues(); }
  public void clearResidues() { sequence.clearResidues(); }

  public char        getBaseAt(int i) {
    return sequence.getBaseAt(i);
  }

  public void        setDescription(String desc) {
    sequence.setDescription(desc);
  }
  public String      getDescription() {
    return sequence.getDescription();
  }

  public void        addPDBCode(String code) {
    sequence.addPDBCode(code);
  }
  public Vector      getPDBCodes() {
    return sequence.getPDBCodes();
  }

  public Vector[]    getScores() {
    return sequence.getScores();
  }
  public Vector      getScoresAt(int i) {
    return sequence.getScoresAt(i);
  }
  public void        setScore(int i, float s,int num) {
    sequence.setScore(i,s,num);
  }
  public void        setScore(int i,float s) {
    sequence.setScore(i,s);
  }

  public void        addFeature(SeqFeatureI sf ) {
    sequence.addFeature(sf);
  }
  public Vector      getFeatures() {
    return sequence.getFeatures();
  }
  public void        getFeatures(String server, String database) {
    sequence.getFeatures(server,database);
  }

  public int         findIndex(int pos) {
    return sequence.findIndex(pos);
  }
  public int         findPosition(int i) {
    return sequence.findPosition(i);
  }


  public void       deleteCharAt(int i,int n) {
    sequence.deleteCharAt(i,n);

    if (boxColour.size() > i) {
      boxColour.removeElementAt(i);
    }

  }

  public void deleteCharAt(int i) {
    sequence.deleteCharAt(i);

    if (boxColour.size() > i) {
      boxColour.removeElementAt(i);
    }
  }

  public void insertCharAt(int i,char c) {
    sequence.insertCharAt(i,c);

    for (int j=boxColour.size(); j < i; j++) {
      boxColour.addElement(Color.white);
    }

    boxColour.insertElementAt(Color.white,i);
  }

  public void       insertCharAt(int i,char c, int n) {
    sequence.insertCharAt(i,c,n);

    for (int j=boxColour.size(); j < i; j++) {
      boxColour.addElement(Color.white);
    }

    boxColour.insertElementAt(Color.white,i);

  }
  public void       insertCharAt(int i,char c,boolean chop) {
    sequence.insertCharAt(i,c,chop);

    for (int j=boxColour.size(); j < i; j++) {
      boxColour.addElement(Color.white);
    }

    boxColour.insertElementAt(Color.white,i);

  }

  public int[]      setNums(String s) {
    return sequence.setNums(s);
  }

  public int getNum(int j) {
    return sequence.getNum(j);
  }

  public int [] getNums() {
    return sequence.getNums();
  }

  public Color getColor() {
    return this.color;
  }

  public void setColor(Color c) {
    this.color = c;
  }

  public int getCharHeight() {
    return sequence.getCharHeight();
  }
  public void setCharHeight(int height) {
    sequence.setCharHeight(height);
  }

//  public void cleanup() {
//    sequence.cleanup();
//  }

  public RangeI getRange() {
    return sequence.getRange();
  }

  public void setRange(RangeI range) {}

  public boolean usesGenomicCoords() {
    return sequence.usesGenomicCoords();
  }

  public int getFrame(long pos, boolean forward) {
    return sequence.getFrame(pos, forward);
  }

  public boolean isSequenceAvailable(long position) {
    return sequence.isSequenceAvailable(position);
  }

  public String getReverseComplement() {
    return sequence.getReverseComplement();
  }

  public void setChecksum (String checksum) {
    sequence.setChecksum (checksum);
  }

  public String getChecksum () {
    return sequence.getChecksum ();
  }

  public boolean residueIsSpacer (int pos) {
    return sequence.residueIsSpacer (pos);
  }

  public Vector getDbXrefs () {
    return sequence.getDbXrefs ();
  }

  public void addDbXref (String db, String acc) {
    sequence.addDbXref (db, acc);
  }

  public void addDbXref (DbXref xref) {
    sequence.addDbXref(xref);
  }

  public void      setAccessionNo(String acc) {
    sequence.setAccessionNo(acc);
  }

  public String    getAccessionNo() {
    return sequence.getAccessionNo();
  }

  /** This is not actually used. from SequenceI interface */
  public void setName(String id) {
    sequence.setName (id);
  }

  public SequenceI getSubSequence (int start, int end) {
    return sequence.getSubSequence(start, end);
  }

  public boolean hasResidueType() {
    return sequence.hasResidueType();
  }

  public void setResidueType (String res_type) {
    sequence.setResidueType(res_type);
  }

  public String getResidueType () {
    return sequence.getResidueType();
  }

  public boolean isAA() { return sequence.isAA(); }

  public void setLength (int len) {
    sequence.setLength(len);
  }

  public boolean isLazy() {
    return sequence.isLazy();
  }

  public HashMap getGenomicErrors() {
    return sequence.getGenomicErrors();
  }

  public boolean isSequencingErrorPosition(int base_position) {
    return sequence.isSequencingErrorPosition(base_position);
  }

  public SequenceEdit getSequencingErrorAtPosition(int base_position) {
    return sequence.getSequencingErrorAtPosition(base_position);
  }

  public boolean addSequencingErrorPosition(String operation, 
                                            int pos,
                                            String residue) {
    return sequence.addSequencingErrorPosition(operation, pos, residue);
  }

  public boolean addSequenceEdit(SequenceEdit seq_edit) {
    return sequence.addSequenceEdit(seq_edit);
  }
  public void setOrganism(String organism) {
    sequence.setOrganism(organism);
  }

  public String getOrganism() {
    return sequence.getOrganism();
  }

  public void setDate(Date update_date) {
    sequence.setDate(update_date);
  }

  public void addDbXref (String db, String acc, int isCurrent) {
    sequence.addDbXref(db, acc, isCurrent);
  }

  /** These are needed because they are declared in SequenceI */
  public void addProperty(String key, String value) {
    throw new apollo.dataadapter.NotImplementedException("addProperty not implemented for jalview.gui.DrawableSequence");
  }
  public void removeProperty(String key) {
    throw new apollo.dataadapter.NotImplementedException("removeProperty not implemented for jalview.gui.DrawableSequence");
  }
  public void replaceProperty(String key, String value) {
    throw new apollo.dataadapter.NotImplementedException("replaceProperty not implemented for jalview.gui.DrawableSequence");
  }
  public String getProperty(String key) {
    throw new apollo.dataadapter.NotImplementedException("getProperty not implemented for jalview.gui.DrawableSequence");
  }
  public Vector getPropertyMulti(String key) {
    throw new apollo.dataadapter.NotImplementedException("getPropertyMulti not implemented for jalview.gui.DrawableSequence");
  }
  public Hashtable getProperties() {
    throw new apollo.dataadapter.NotImplementedException("getProperties not implemented for jalview.gui.DrawableSequence");
  }
  
  public boolean hasName() { return sequence.hasName(); }

  public boolean removeSequenceEdit(SequenceEdit s) { return false; }
}
