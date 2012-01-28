package jalview.gui;

import java.awt.*;
import jalview.gui.menus.CommandParser;
import jalview.io.*;
import jalview.analysis.NJTree;

public class AlignViewport {
  int startRes;
  int endRes;

  int startSeq;
  int endSeq;
  boolean showScores;
  boolean showText;
  boolean showBoxes;
  boolean wrapAlignment;

  boolean groupEdit = false;
  String gapCharacter = new String("-");

  protected int charHeight;
  protected int charWidth;
  protected int chunkWidth;
  protected int chunkHeight;

  Color backgroundColour;

  Font font;
  DrawableAlignment alignment;

  CommandParser log;

  Selection sel = new Selection();
  ColumnSelection colSel = new ColumnSelection();

  OutputGenerator og;

  Rectangle pixelBounds = new Rectangle();
  int threshold;
  int increment;

  NJTree currentTree = null;
 
  public AlignViewport(DrawableAlignment da,
                       boolean showScores,
                       boolean showText,
                       boolean showBoxes,
                       boolean wrapAlignment) {
    this(0,da.getWidth()-1,0,da.getHeight()-1,showScores,
         showText,
         showBoxes,
         wrapAlignment);

    setAlignment(da);

  }
  public AlignViewport(int startRes, int endRes,
                       int startSeq, int endSeq,
                       boolean showScores,
                       boolean showText,
                       boolean showBoxes,
                       boolean wrapAlignment) {
    this.startRes = startRes;
    this.endRes   = endRes;
    this.startSeq = startSeq;
    this.endSeq   = endSeq;

    this.showScores    = showScores;
    this.showText      = showText;
    this.showBoxes     = showBoxes;
    this.wrapAlignment = wrapAlignment;

    log = new CommandParser();
    og = new AlignmentOutputGenerator(this);

    setFont(new Font("Courier",Font.PLAIN,10));
    setCharHeight(15);
    setCharWidth(10);

  }

  public AlignViewport(int startRes, int endRes,
                       int startSeq, int endSeq,
                       boolean showScores,
                       boolean showText,
                       boolean showBoxes,
                       boolean wrapAlignment,
                       Color backgroundColour) {
    this(startRes,endRes,startSeq,endSeq,showScores,showText,showBoxes,wrapAlignment);

    this.backgroundColour = backgroundColour;
  }

  public int getStartRes() {
    return startRes;
  }

  public int getEndRes() {
    return endRes;
  }

  public int getStartSeq() {
    return startSeq;
  }

  public void setPixelBounds(Rectangle rect) {
    pixelBounds = rect;
  }

  public Rectangle getPixelBounds() {
    return pixelBounds;
  }

  public void setStartRes(int res) {
    //System.out.println(" set startres to " + res + " from " + startRes);
    this.startRes = res;
    //try {
    //  throw new NullPointerException();
    //} catch (Exception e) {
    //  e.printStackTrace();
    //}
  }
  public void setStartSeq(int seq) {
    this.startSeq = seq;
  }
  public void setEndRes(int res) {
    if (res > alignment.getWidth()-1) {
      System.out.println(" Corrected res from " + res + " to maximum " + (alignment.getWidth()-1));
      res = alignment.getWidth() -1;
    }
    if (res < 0) {
      res = 0;
    }
    //System.out.println(" SET END RES to " + res);
    this.endRes = res;
  }
  public void setEndSeq(int seq) {
    if (seq > alignment.getHeight()) {
      seq = alignment.getHeight();
    }
    if (seq < 0) {
      seq = 0;
    }
    this.endSeq = seq;
  }
  public int getEndSeq() {
    return endSeq;
  }
  public void setFont(Font f) {
    this.font = f;
  }

  public Font getFont() {
    return font;
  }

  public void setCharWidth(int w) {
    this.charWidth = w;
  }
  public int getCharWidth() {
    return charWidth;
  }

  public void setCharHeight(int h) {
    this.charHeight = h;
  }
  public int getCharHeight() {
    return charHeight;
  }
  public void setChunkWidth(int w) {
    this.chunkWidth = w;
  }
  public int getChunkWidth() {
    return chunkWidth;
  }

  public void setChunkHeight(int h) {
    this.chunkHeight = h;
  }
  public int getChunkHeight() {
    return chunkHeight;
  }

  public DrawableAlignment getAlignment() {
    return alignment;
  }
  public void setAlignment(DrawableAlignment align) {
    this.alignment = align;
  }
  public void setShowScores(boolean state) {
    showScores = state;
  }
  public void setWrapAlignment(boolean state) {
    wrapAlignment = state;
  }
  public void setShowText(boolean state) {
    showText = state;
  }
  public void setShowBoxes(boolean state) {
    showBoxes = state;
  }
  public boolean getShowScores() {
    return showScores;
  }
  public boolean getWrapAlignment() {
      return wrapAlignment;
  }
  public boolean getShowText() {
    return showText;
  }
  public boolean getShowBoxes() {
    return showBoxes;
  }
  public CommandParser getCommandLog() {
    return log;
  }
  public boolean getGroupEdit() {
    return groupEdit;
  }
  public void setGroupEdit(boolean state) {
    groupEdit = state;
  }
  public String getGapCharacter() {
    return gapCharacter;
  }
  public void setGapCharacter(String gap) {
    gapCharacter = new String(gap);
    if (getAlignment() != null) {
      getAlignment().setGapCharacter(gapCharacter);
    }
  }
  public void setThreshold(int thresh) {
    threshold = thresh;
  }
  public int getThreshold() {
    return threshold;
  }
  public void setIncrement(int inc) {
    increment = inc;
  }
  public int getIncrement() {
    return increment;
  }

  public int getIndex(int y) {

    int y1 = 0;
    int starty = getStartSeq();
    int endy   = getEndSeq();

//    System.out.println("getIndex with y      = " + y);
//    System.out.println("              starty = " + starty);
//    System.out.println("              endy   = " + endy);

    for (int i = starty; i <= endy; i++) {
      if (i < alignment.getHeight() && alignment.getSequenceAt(i) != null) {
        int y2 = y1 + getCharHeight();
//        System.out.println("  Checking against sequence " + i + " y1 = " + y1 + " y2 = " + y2);

        if (y>=y1 && y <=y2) {
          return i;
        }
        y1  = y2;
      } else {
        return -1;
      }
    }
    return -1;
  }

  public Selection getSelection() {
    return sel;
  }
  public ColumnSelection getColumnSelection() {
    return colSel;
  }

  public OutputGenerator getOutputGenerator() {
    return og;
  }

  public void resetSeqLimits() {
    setStartSeq(0);
    setEndSeq(getPixelBounds().height/getCharHeight()); 
  }

  public void setCurrentTree(NJTree tree) {
      currentTree = tree;
  }
  public NJTree getCurrentTree() {
    return currentTree;
  }
}
