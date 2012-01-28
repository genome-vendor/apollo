package apollo.gui.detailviewers.sequencealigner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.text.*;

import apollo.datamodel.*;
import apollo.main.Apollo;
import apollo.seq.*;
import apollo.config.Config;
import apollo.util.*;

import org.apache.log4j.*;

import org.bdgp.util.RangeHash;
import org.bdgp.swing.FastTranslatedGraphics;

import apollo.gui.BaseScrollable; // move to detailviewers?
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRenderer;

/** 
 * Generates a scrollable base level display for sequences. A <class>SeqAlignPanel</class>
 * can be thought of logically as a set of <class>SeqFeature</class> objects, each of
 * which is displayed on some tier. A tier is a row on which a sequence can be placed,
 * with lower number tiers being above higher number tiers. The tier on which a newly added
 * sequence will be placed is determined by the <class>TierManager</class>.
 * 
 * A <class>SeqAlignPanel</class> can only display either forward stranded features or
 * reverse stranded features but not both.
 * XXX: allow for forward and reverse stranded features to be displayed together?
 * 
 * When discussing the location of a particular base in this display there are
 * two types of coordinates you can use:
 * 
 * pixel: refers to the x/y coordinate of a base
 * position: refers to row/column position. 
 * basePair: refers to the actual base pair, on forward strand basePair = pos + lowestBase
 */

public class SeqAlignPanel extends JPanel
  implements Scrollable, BaseScrollable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SeqAlignPanel.class);

  public static final int NO_BOUNDARY = 0;
  public static final int START_BOUNDARY = 1;
  public static final int END_BOUNDARY = 2;

  public static final int NO_TYPE = -1;
  public static final int STOP_CODON = 1;
  public static final int START_CODON = 2;

  public static final int INTRON = 4;
  public static final int EXON = 5;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** Holds a sequence (SeqWrapper) for every tier, element # is tier number */
  protected Vector<SeqWrapper> sequences; //TODO:change to ArrayList<SeqWrapper>
  
  /** The number of base pairs displayed in a single row */
  public int cols; //XXX this should always be highestbase - lowestbase

  /** The default size of the background color stripes, in number of bases */
  protected int stripeWidth;
  
  /** Maybe this controls the space between rows when in vertical mode? */
  protected int rowMargin; //TODO: test vertical mode to find out?

  /** Related to the scroll policy. Controls how display is generated */
  protected boolean horizontalMode; // I think this is implied by the scroll policy of the panel

  /** What type of sequence is being displayed */
  public boolean reverseStrand; //TODO: create a direction type and use that 
  // or at least add some public static final variables

  /** Related to scroll handling */
  private AutoscrollThread horizontalScrollThread;
  private AutoscrollThread verticalScrollThread;

  /** More Scrolling... not really sure. */
  private boolean autoscroll; // XXX: what does this do?

  /** The base pair the panel starts at in outside world(chromosomal) */
  protected int lowestBase;
  
  /** The base pair the panel ends at in outside world(chromosomal) */
  protected int highestBase;

  /**
   * Constructor.
   * TODO: create a factory? make this a 
   * 
   * @param numCols
   * @param lowestBase
   * @param highestBase
   */
  public SeqAlignPanel(int numCols, int lowestBase, int highestBase) {
    super();
    this.sequences = new Vector();
    this.cols = numCols;
    this.stripeWidth = 10;
    this.rowMargin = 10;
    this.horizontalMode = true;
    this.reverseStrand = false; //TODO: add strand type to constructor?
    attachListeners(); //XXX what does this do?
    //TODO find out how to initialize horizontalScrollThread and verticalScrollThread
    this.autoscroll = false;
    this.lowestBase = lowestBase;
    this.highestBase = highestBase;
  }


  /**
   * TODO: Find out what this does
   */
  public void setAutoscroll(boolean autoscroll) {
    this.autoscroll = autoscroll;
    if (autoscroll) {
      ScrollMouseListener listener = new ScrollMouseListener();
      addMouseListener(listener);
      addMouseMotionListener(listener);
    }
  }

  /**
   * Sets the reverse strand property value
   * 
   * @param aFlag a flag indicating whether or not the 
   * view is for the forward or reverse strand
   * 
   * TODO: change to setDirection? remove from api?
   */
  public void setReverseStrand(boolean aFlag) {
    reverseStrand = aFlag;
  }

  public boolean getReverseStrand() {
    return reverseStrand;
  }

  /**
   * TODO: put someting here
   * 
   * @param mode
   */
  public void setHorizontalMode (boolean mode) {
    this.horizontalMode = mode;
  }

  /**
   * Sets the size of the background color stripes, in number of bases.
   * 
   * @param width the number of bases that will be included in a stripe
   */
  public void setStripeWidth(int width) {
    stripeWidth = width;
  }

  /**
   * Returns the number of bases that will be included in a stripe
   * 
   * @return the <code>stripeWidth</code> property
   */
  public int getStripeWidth() {
	  return this.stripeWidth;
  }

  /**
   * FIXME: Returns the row margin property value, whatever that is...
   * 
   * @return the <code>rowMargin</code> property
   */
  public int getRowMargin() {
    return rowMargin;
  }

  /**
   * Returns the number of sequence tiers in the panel
   * 
   * @return the number of sequence tiers in the panel
   */
   public int getTierCount() {
    return sequences.size();
  }

   /**
    * Adds a new tier to the panel.
    * 
    * @param seq the sequence to be used as a reference for this tier
    * @param phase the phase of the sequence being read
    * @param seq_type the the type of sequence being added
    * @param tier the index at which this tier is to be placed
    */
  public void addTier(SequenceI seq, int phase, String seq_type,
                          int index) {

    RangeI r = seq.getRange();
    if (seq.getRange() != null && highestBase < seq.getRange().getHigh()) {
      highestBase = seq.getRange().getHigh();
    }
    
    if (seq.getRange() != null && lowestBase > seq.getRange().getLow()) {
      lowestBase = seq.getRange().getLow();
    }

    if (index >= sequences.size())
      sequences.addElement(new SeqWrapper(seq, phase, seq_type,this));
    else
      sequences.insertElementAt(new SeqWrapper(seq, phase, seq_type,this),
                                index);
  }

  /**
   * Attaches a new <class>BaseRenderer</class> to the given tier
   * 
   * @param tier the tier to attach the base renderer too
   * @param renderer the specified renderer
   */
  public void attachRendererToTier(int tier, BaseRenderer renderer) {
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(tier);
    sw.setRenderer(renderer);
  }
  
  
  /**
   * Returns the boundary type of the <class>SeqFeature</class> at a particular
   * position on a tier.
   * 
   * Can be one of:
   *     <code>SeqAlignPanel.NO_BOUNDARY</code>
   *     <code>SeqAlignPanel.LEFT_BOUNDARY</code>
   *     <code>SeqAlignPanel.RIGHT_BOUNDARY</code>
   * 
   * FIXME:what if the feature at that position is only a single base?
   * 
   * @param position the position in the tier to look
   * @param tier the tier to look at
   * @return the boundary type of at a position on a tier
   */
  public int getBoundaryType(int position, int tier) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    if (sw == null)
      return NO_BOUNDARY;
    else
      return sw.getBoundaryType(position);
  }

  /**
   * Returns the <class>SeqFeatureI</class> at a position 
   * on a tier.
   * 
   * @param position the position in the tier to look
   * @param tier the to look at
   * @return the feature at a position on a tier
   */
  public SeqFeatureI getFeatureAtPosition(int position, int tier) {
    if (tier >= sequences.size())
      return null;
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(tier);
    return sw.getFeatureAtPosition(position);
  }

  /**
   * XXX:Dont think this is in use
   * Returns all of the sequence features within a given range
   * on a tier.
   * 
   * @param startpos the position that the search begins with (inclusive?)
   * @param endpos the position that the search ends at (inclusive?)
   * @param tier the tier that the search is executed on
   * @return a list of sequence features within the given range
   */
  public Vector<SeqFeatureI> getFeaturesInRange(int startpos,
                                   int endpos,
                                   int tier) {
    if (tier >= sequences.size())
      return null;
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(tier);
    return sw.getFeaturesInRange(startpos, endpos);
  }

  /**
   * Returns the sequence feature that is shown at the
   * given display coordinates.
   * 
   * @param x the x coordinate of this display
   * @param y the y coordinate of this display
   * @return the sequence feature at the given coordinates
   */
  public SeqFeatureI getFeatureForPixelPosition(int x, int y) {
    return getFeatureForPixelPosition(x,y,false);
  }

  /**
   * Returns the sequence feature that is shown at the
   * given display coordinates.
   * 
   * @param x the x coordinate of this display
   * @param y the y coordinate of this display
   * @param noSet flag indicating whether or not the
   *        sequence feature will also be a sequence feature set
   * @return returns the sequence feature at the given coordinates
   *         or null if no feature exists
   */
  public SeqFeatureI getFeatureForPixelPosition(int x, int y,
      boolean noSet) {
    int tier = getTierForPixelPosition(y);
    int row = getRowForPixelPosition(y);
    int col = getColForPixelPosition(x);
    int pos = cols*row+col+1;
    if (noSet)
      return getFeatureAtPosition(pos, tier);
    else
      return getFeatureSetAtPosition(pos, tier);
  }

  public FeatureSetI getFeatureSetAtPosition(int position, int tier) {
    if (tier >= sequences.size())
      return null;
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(tier);
    return sw.getFeatureSetAtPosition(position);
  }

  protected SeqWrapper getSeqWrapperForTier(int tier) {
    if (tier >= 0 && tier < sequences.size())
      return (SeqWrapper) sequences.elementAt(tier);
    else
      return null;
  }

  public int getTypeAtPosition(int tier, int base) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    return sw.getTypeAtPosition(base);
  }

  public double [] getRangeAtPosition(int tier, int pos) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    if (sw == null)
      return null;
    else
      return sw.getRangeAtPosition(pos);
  }

  public int getRangeIndex(int tier, int low, int high) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    return sw.getRangeIndex(low, high);
  }

  public int getExonRangeIndex(int tier, int low, int high) {
    return getExonRangeIndex(tier, low, high, false);
  }

  public int getExonRangeIndex(int tier, int low, int high, boolean exact) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    return sw.getExonRangeIndex(low, high, exact);
  }

  public SeqFeatureI getFeatureAtIndex(int tier, int index) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    return sw.getFeatureAtIndex(index);
  }

  public int getFeatureCount(int tier) {
    SeqWrapper sw = getSeqWrapperForTier(tier);
    return sw.getFeatureCount();
  }

  private void attachListeners() {
    if (autoscroll) {
      ScrollMouseListener listener = new ScrollMouseListener();
      addMouseListener(listener);
      addMouseMotionListener(listener);
    }
  }

  /** Checks if there are any overlaps between features in sw and feature,
   * if so return false, else true
   */ //was protected
  private boolean canBeAddedToSequence(SeqWrapper sw,
                                         SeqFeatureI feature) {
    Vector features = sw.getFeatures();
    for(int i=0; i < features.size(); i++) {
      FeatureWrapper fw = (FeatureWrapper) features.elementAt(i);
      SeqFeatureI currentFeature = fw.getFeature();
      if (feature.contains(currentFeature.getLow()) ||
          feature.contains(currentFeature.getHigh()) ||
          currentFeature.contains(feature.getHigh()) ||
          currentFeature.contains(feature.getLow()))
        return false;
    }
    return true;
  }

  public void clear() {
    sequences.removeAllElements();
    //longest_seq = 0;
    this.highestBase = 0;
    this.lowestBase = 0;
  }

  /** 
   * Adds feature to an existing SeqWrapper if it doesn't overlap with it,
   * otherwise creates a new SeqWrapper for it,
   * @return The element number in sequences,
   * which is synonomous with tier number
   */
  public int addAlignFeature(SequenceI seq, SeqFeatureI feature) {
    SeqWrapper wrapper = null;

    int seqNum = -1;

    // Loop through SeqWrappers in vector sequences,
    // if none of the features in a SeqWrapper
    // overlap with feature then set wrapper to that SeqWrapper(sw)
    for(int seqIndex = 0;
        seqIndex < sequences.size();
        seqIndex++) {
      SeqWrapper sw = (SeqWrapper) sequences.elementAt(seqIndex);
      if (sw.getSequence() == seq //&& sw.canHold(feature) //RAY will probably have to change
          && canBeAddedToSequence(sw, feature)) {
        wrapper = sw;
        seqNum = seqIndex;
        break;
      }
    }
    // If no non overlapping wrapper was found above create a new one
    if (wrapper == null) {
      seqNum = sequences.size();
      addTier (seq,
                   -1,
                   SequenceI.DNA,
                   seqNum);
      attachRendererToTier(seqNum,
                               new DefaultBaseRenderer(getCharWidth(),
                                                       getCharHeight()));
      wrapper = (SeqWrapper) sequences.elementAt(seqNum);
    }
    wrapper.addFeature(feature);
    return seqNum;
  }

  /** Removes feature from an existing SeqWrapper
   */
  protected void removeFeature(FeatureSetI feature, int tier) {
    if (tier >= sequences.size()) {
      logger.warn("SeqAlignPanel.removeFeature: requested tier " + tier + " is bigger than max tier " + sequences.size());
      return;
    }
    // tier not found - bug!
    if (tier == -1) {
      logger.error("EDE trying to remove feature that it can't find (possible cloning issue)!", new Throwable());
      return;
    }
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(tier);
    sw.removeFeature (feature);
    if (sw.getFeatureCount() == 0) {
      sequences.removeElement (sw);
    }
  }

  public void moveSequence(int oldpos, int newpos) {
    SeqWrapper sequence = (SeqWrapper) sequences.elementAt(oldpos);
    sequences.removeElementAt(oldpos);
    sequences.insertElementAt(sequence, newpos);
    reformat();
  }

  public int getSequenceCount() {
    return sequences.size();
  }

  public char getCharAt(int tier, int pos) {
    return ((SeqWrapper) sequences.elementAt(tier)).getCharAt(pos);
  }

  public SequenceI getSequenceForTier(int tier) {
    return ((SeqWrapper) sequences.elementAt(tier)).getSequence();
  }

  public void setVisibilityForSequence(int seqIndex, boolean visible) {
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(seqIndex);
    sw.setVisible(visible);
  }

  public BaseRenderer getRendererAt(int seqIndex) {
    if (seqIndex < 0 || seqIndex >= sequences.size())
      seqIndex = 0;
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(seqIndex);
    return sw.getRenderer();
  }

  public void attachFeatureToSequence(int seqIndex, SeqFeatureI sf) {
    SeqWrapper sw = (SeqWrapper) sequences.elementAt(seqIndex);
    sw.addFeature(sf);
  }

  /** Returns tier number that SeqFeatureI is in. SeqFeatureI can be an exon
      or a transcript as it searches on both the feature itself and its 
      refFeature. */
  protected int getTierForFeature(SeqFeatureI sf) {
    if (sf == null) {
      logger.error("getTierForFeature called on null SeqFeatureI", new Throwable());
      return -1;
    }
    for(int i=0; i < sequences.size(); i++) {
      SeqWrapper sw = (SeqWrapper) sequences.elementAt(i);
      /* these are the transcripts rather than the exons */
      Vector tier_features = sw.getFeatures();
      for (int j = 0; j < tier_features.size(); j++) {
        SeqFeatureI tier_feature
          = ((FeatureWrapper) tier_features.elementAt(j)).getFeature();
	// Added in trying ref feature as well, this allows for exons to
	// work as the tier_features are transcripts
        if (tier_feature == sf || tier_feature == sf.getRefFeature()) {
          return i;
        }
        
        // return tier in case of a cloned feature
        /* 
        if (sf.getName().equals(tier_feature.getName())) {
          System.out.println(sf.getName() + "\t" + sf + "(" + sf.hashCode() + ")" + "\t" + tier_feature + "(" + tier_feature.hashCode() + ")");
          System.out.println(sf.getName() + "\t" + sf.getCloneSource() + "\t" + tier_feature.getCloneSource());
          System.out.println(sf.getName() + "\t" + sf.getId() + "\t" + tier_feature.getId());
        }
        */
        
        if (tier_feature.isClone() && tier_feature.getCloneSource() == sf ||
            sf.isClone() && sf.getCloneSource() == tier_feature) {
          return i;
        }
        
        // isIdentical will pick up clones. deleted transcripts are cloned (because
        // for splits & merges they are added to new genes)
        if (tier_feature.isIdentical(sf)) {
          logger.debug("EDE found clone of feature, not actual feature."+
                       " Double check this is right. "+sf.getName(), new Throwable());

          return i;
        }
      }
    }
    logger.error("EDE failed to find tier for feature "+sf.getName()+", returning -1", new Throwable());
    return -1;
  }

  protected static String padPerChar(String target, String padString) {
    StringBuffer out = new StringBuffer();
    for(int i=0; i < target.length(); i++) {
      out.append(target.charAt(i)+padString);
    }
    return out.toString();
  }

  public int getCharHeight() {
    return getFont().getSize();
  }

  /** 
   * This fixes a resize bug on solaris and linux. When the ede was resizing 
   sometimes the SeqAlignPanel was not resizing. I dont understand this but
   in the JViewPort/Container.validateTree it would call either validate() or
   validateTree() on SeqAlignPanel depending on if it was valid. It is was valid
   validate was called, and validate below caused a reformat and everything was ok.
   If it was not valid, validateTree was called which previously just went to the
   superclass and the needed reformat did not get called. So I added this 
   validateTree that just emulates validate. Why it sometimes is valid and sometimes
   not on resize I dont know. But this did fix the bug. Very strange. 
      
   This doesnt seem to make a difference in terms of the 1.4 jdk bug, but it seems
   like if we are commenting out validate we should also comment out validateTree
   
*/
  public void validateTree() {
    int visible = getVisibleBase();
    reformat();
    super.validateTree();
    scrollToBase(visible);
  }
  
  // This validate method was the mechenism that SeqAlignPanel would redraw itself
  // on resize. Unfortunately in jdk1.4 it would cause an endless paint loop.
  // So it needs to be commented out to work with 1.4. So now the question is
  // how to get resize repainting in 1.4.
  public void validate() {
    int visible = getVisibleBase();
    reformat();
    super.validate();
    scrollToBase(visible);
  }

  protected void reformat() {
    Component c = getParent();
    int width = -1;
    int height = -1;
    if (horizontalMode)
      cols = highestBase - lowestBase;

    if (c != null && c instanceof JViewport) {
      Component sc = c.getParent();
      height = sc.getSize().height;
      if (sc instanceof JScrollPane) {
        if (((JScrollPane) sc).getHorizontalScrollBarPolicy() ==
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
          horizontalMode = false;

          width = ((JViewport) c).getViewRect().width;
          // width = c.getSize().width - getDefaultHeader().getSize().width;
          cols = width / getCharWidth();
          if (cols < 1)
            cols = 100;
        }
      }
    }

    int desiredHeight = getRowCount()*(getRowHeight()+getRowMargin());
    int desiredWidth = cols*getCharWidth();
    height = desiredHeight;

    if (logger.isDebugEnabled()) {
      logger.debug("width,height=" + width + "," + height);
      logger.debug("Desired=" + desiredWidth + "," + desiredHeight);
      logger.debug("CurrentPreferred=" + getPreferredSize().width + "," + getPreferredSize().height);
      logger.debug("CurrentSize=" + getSize().width + "," + getSize().height);
    }

    // The if statement is part of the patch for EDE problem with java 1.4 where 
    // repaint continuously called - contributed by Len Trigg 
    if ((desiredWidth != getPreferredSize().width) || (desiredHeight != getPreferredSize().height)) {
      logger.debug("Setting size and preferredsize.");
      setPreferredSize(new Dimension(desiredWidth,desiredHeight));
      if (width == -1)
        setSize(getPreferredSize());
      else
        setSize(desiredWidth, getPreferredSize().height);
    }
  }

  public void addNotify() {
    Component c = getParent();
    if (c != null && c instanceof JViewport) {
      Component sc = c.getParent();
      if (sc instanceof JScrollPane) {
        if (((JScrollPane) sc).getVerticalScrollBarPolicy() ==
            JScrollPane.VERTICAL_SCROLLBAR_NEVER) { // RAY: change to AS_NEEDED?
          horizontalMode = true;
        }
      }
    }
    reformat();
    super.addNotify();
  }

  public int getRowHeight() {
    return getCharHeight()*sequences.size();
  }

  public int getRowWidth() {
    return getCharWidth()*getColumnCount();
  }

  public int getCharWidth() {
    return getFont().getSize();
  }

  public Rectangle getRectangleForCharPosition(int pos, int tier) {
    Point p = getPixelLocForPosition(pos, tier);
    return new Rectangle(p.x,
                         p.y,
                         getCharWidth(),
                         getCharHeight());
  }

  /**
   * The pixel coordinates for the position
   * at a particular tier
   * @param pos row/col coord, not bp 
   */
  public Point getPixelLocForPosition(int pos, int tier) {
    if (tier == -1)
      tier = sequences.size()-1;
    int colNum = getColForPosition(pos);
    int rowNum = getRowForPosition(pos);

    int tierHeight = getRowHeight();

    int x = colNum * getCharWidth();
    int y = rowNum*(tierHeight+getRowMargin())+
            tier*getCharHeight();
    return new Point(x,y);
  }

  public Rectangle getRectangleForTierPosition(int pos) {
    Point p = getPixelLocForPosition(pos, 0);
    return new Rectangle(p.x,
                         p.y,
                         getCharWidth(),
                         getRowHeight());
  }

  public int getRowForPosition(int pos) {
    return pos / cols;
  }

  public int getColumnCount() {
    return cols;
  }

  public int getRowCount() { // RAY: is it used?
    int length = highestBase - lowestBase;//basePairToPos(getLongestSeq()) + 1; is there a getLength?
    return (int)
           Math.ceil((double) length / (double) cols);
  }

  public int getColForPosition(int pos) {
    return pos % cols;
  }

  public int getRowForPixelPosition(int y) {
    int rowNum = y /
                 (getRowHeight()+getRowMargin());
    return rowNum;
  }


  public int getTierForPixelPosition(int y) {
    int loc = (y %
               (getRowHeight()+getRowMargin())) / getCharHeight();
    return loc;
  }

  protected Point getPixelLocForColAndRow(int col,int row) {
    return new Point(col*getCharWidth(),
                     row*(getRowHeight()+getRowMargin()));
  }

  public void scrollToRow(int row) {
    JViewport view = SwingMissingUtil.getViewportAncestor(this);
    if (view != null) {
      Point point = getPixelLocForColAndRow(0,row);
      if (point.x + view.getViewRect().width >
          getSize().width)
        point.x = getSize().width - view.getViewRect().width;
      if (point.x + view.getViewRect().height >
          getSize().height)
        point.y = getSize().height - view.getViewRect().height;
      view.setViewPosition(point);
    }
  }

  /** From BaseScrollable interface. Returns basepair that SeqAlignPanel is
      currently at. SZAP queries this to know which base to highlight */
  public int getVisibleBase() {
    JViewport view = SwingMissingUtil.getViewportAncestor(this);
    if (view == null)
      return -1;
    Rectangle r = view.getViewRect();
    // row and column are both counted starting at 0
    int row = getRowForPixelPosition(r.y);
    int col = getColForPixelPosition(r.x);
    int pos = col+row*getColumnCount();

    return posToBasePair(pos);
  }

  /** Takes a row/col 1 based pos and converts to global base pairs 
   * lowest base does not seem to be accurate, highest probably isn't either...
   */
  public int posToBasePair(int pos) {
    // reverse strand numbering is backwards
    if (reverseStrand)
      return highestBase - pos;
    else // forward
      return pos + lowestBase;
  }

  /* positions in the window start at 0 so there
     is no need to add one */
  public int basePairToPos(int basePair) {
    if (reverseStrand)
      return highestBase - basePair;
    else // forward strand
      return basePair - lowestBase;
  }

  /** Takes a row/col zero based pos and converts it to offset
      into the dna/aa string !!! SHOULD THESE REALLY BE basePairToPos? I dont think its right*/
  public int posToResidue(int pos) {
    // reverse strand numbering is backwards
    // but the DNA sequence residues are already
    // reversed
    if (reverseStrand)
      return pos;
    else // forward
      return pos + lowestBase;
  }

  /** Takes an offset into the dna/aa string and 
   *  converts it to a row/col zero based pos
   */
  public int residueToPos(int residue) {
    // reverse strand numbering is backwards
    // but the DNA sequence residues are already
    // reversed
    if (reverseStrand)
      return residue;
    else // forward
      return residue - lowestBase;
  }

  /** takes a zero based string offset for the residues
      and converts it to an offset in zero based pos space
  */
  public int residueToBasePair(int residue) {
    // reverse strand numbering is backwards
    // but the DNA sequence residues are already
    // reversed
    if (reverseStrand)
      return residue;
    else // forward counting from 1
      return residue - lowestBase;
  }

  public int getLowestBase() {
    return (int)lowestBase;
  }

  public int getHighestBase() {
    return (int)highestBase;
  }

  public int getVisibleBaseCount() {
    JViewport view = SwingMissingUtil.getViewportAncestor(this);
    if (view == null)
      return -1;
    Rectangle r = view.getViewRect();
    int height =  (horizontalMode ? 0 : getRowForPixelPosition(r.height));
    int width = getColForPixelPosition(r.width);
    return getColumnCount()*height+width;
  }

  /** from BaseScrollable interface, there is both a scrollToBase and a
      scrollToPos.
      scrollToBase calls scrollToPosition after transforming into
      window sequence coordinates
  */
  public void scrollToBase(int basepair) {
    scrollToPosition (basePairToPos(basepair));
  }

  public void scrollToPosition (int pos) {
    JViewport view = SwingMissingUtil.getViewportAncestor(this);
    if (view != null) {
      Point point = getPixelLocForPosition(pos, 0);
      if (point.x + view.getViewRect().width >
          getSize().width)
        point.x = getSize().width - view.getViewRect().width;
      if (!horizontalMode && point.y + view.getViewRect().height >
          getSize().height) {
        point.y = getSize().height - view.getViewRect().height;
      }
      view.setViewPosition(point);
    }
  }

  public void scrollToFeature(SeqFeatureI feature) {
    JViewport view = SwingMissingUtil.getViewportAncestor(this);
    if (feature != null && view != null) {
      Rectangle r = view.getViewRect();
      // row and column are both counted starting at 0
      int first_row = getRowForPixelPosition(r.y);
      int first_col = getColForPixelPosition(r.x);
      int first_pos = first_col + (first_row * getColumnCount());

      int first_base = posToBasePair(first_pos);

      int last_row = getRowForPixelPosition(r.y + r.height);
      int last_col = getColForPixelPosition(r.x + r.width);
      int last_pos = last_col + (last_row * getColumnCount());
      int last_base = posToBasePair(last_pos);
      if ((!reverseStrand && (feature.getStart() > first_base ||
                              feature.getEnd() < last_base)) ||
          (reverseStrand && (feature.getStart() < first_base ||
                             feature.getEnd() > last_base)))
        scrollToBase((int)feature.getStart());
    }
  }

  public int getColForPixelPosition(int x) {
    int colNum = x / getCharWidth();
    return colNum;
  }

  /** Converts a pixel rectangle to a pos(row/col) rectangle */
  public Rectangle getPosRectangleForPixels(Rectangle r) {
    int rowStart = getRowForPixelPosition(r.y);
    int rowEnd = getRowForPixelPosition(r.y+r.height)-rowStart+1;
    int colStart = getColForPixelPosition(r.x);
    int colEnd = getColForPixelPosition(r.x+r.width)-colStart+1;
    return new Rectangle(colStart, rowStart, colEnd, rowEnd);
  }


  public void repaint(int startPos, int endPos) {
    if (endPos < startPos) {
      int temp = endPos;
      endPos = startPos;
      startPos = temp;
    }
    endPos = endPos+1;

    int startCol = getColForPosition(startPos);
    int startRow = getRowForPosition(startPos);
    int endCol = getColForPosition(endPos);
    int endRow = getRowForPosition(endPos);
    if (endRow < startRow) {
      int t = endRow;
      endRow = startRow;
      startRow = t;
    }
    if (endCol < startCol) {
      int t = endCol;
      endCol = startCol;
      startCol = t;
    }
    if (startRow == endRow) {
      repaint(startCol,startRow,endCol - startCol,1);
    } else {
      repaint(0,startRow,cols,endRow - startRow + 1);
    }
  }

  public void repaint(int x, int y, int width, int height) {
    Point start = getPixelLocForColAndRow(x,y);
    int drawWidth = width*getCharWidth();
    int drawHeight = height*(getRowHeight()+getRowMargin());

    RepaintManager.currentManager(this).addDirtyRegion(this,
        start.x,
        start.y,
        drawWidth,
        drawHeight);
  }

  /** called by paint(Graphics), x,y,width,height are in
   * row/column numbers, not pixels!  paint(Graphics) converts
   * pixels to coords via getPosRectangleForPixels
   * @param x visible start column,
   * @param y visible start row
   * @param width number of visible columns
   * @param height number of visible rows
   * column and row does not necasarily correspond with base pair if the
   * sequence is offset
   */
  private void paint(Graphics g, int x, int y, int width, int height) {
    int charWidth = getCharWidth();

    FastTranslatedGraphics transG = new FastTranslatedGraphics(g);

    // so i,j represents a row/col which is one base pair. So for
    // every base pair draw in background and then loop through
    // every SeqWrapper(for every bp) and get a component, and
    // draw eachof those components
    // loop through rows
    for(int j=y; j < y+height; j++) {
      // loop through columns
      for(int i=x; i < x+width; i++) {
        g.setFont(getFont());
        // pos is row column position
        int pos = i+j*cols;

        // draw in the background stripes
        g.setColor(getBackgroundColor(i));
        Point p = getPixelLocForPosition(pos,0);
        g.fillRect(p.x,
                   p.y,
                   charWidth,
                   getRowHeight()+getRowMargin());

        // loop through all of the seq wrappers
        for(int n=0; n < sequences.size(); n++) {
          SeqWrapper sw = (SeqWrapper) sequences.elementAt(n);
          if (sw.isVisible()) {
            // checks if pos in sw offset to offset+length
            // and then gets base or aminoacid if it is
            char ch = sw.getCharAt(pos);
            if (ch != '\0') {
              // Draw base_pair of SeqWrapper, if there is an
              // exon at that base pair draw in exon
              Component c = null; //sw.getRenderer().getBaseRendererComponent(ch, pos, n, sw.getSequence());
              Point pl = getPixelLocForPosition(pos,n);
              transG.translateAbsolute(pl.x,pl.y);
              c.paint(transG);
            }
          }
        }
      }
    }
  }

  static int i=0;
  public void paint(Graphics g) {
    
    // Debugging statements for jdk1.4 bug - remove when fixed
//     logger.debug("SAP.paint1 called"+ i);
//     RepaintManager.currentManager(this).removeInvalidComponent(this);

//     RepaintManager.currentManager(this).removeInvalidComponent((JComponent)getParent());
//     RepaintManager.currentManager(this).removeInvalidComponent((JComponent)getParent().getParent());
//     RepaintManager.currentManager(this).removeInvalidComponent((JComponent)getParent().getParent().getParent());
//     RepaintManager.currentManager(this).removeInvalidComponent((JComponent)getParent().getParent().getParent().getParent());


//     RepaintManager.currentManager(this).markCompletelyClean(this);
//     RepaintManager.currentManager(this).markCompletelyClean((JComponent)getParent());
//     RepaintManager.currentManager(this).markCompletelyClean((JComponent)getParent().getParent());
//     RepaintManager.currentManager(this).markCompletelyClean((JComponent)getParent().getParent().getParent());
//     RepaintManager.currentManager(this).markCompletelyClean((JComponent)getParent().getParent().getParent().getParent());
//     logger.debug("SAP.paint parent cleaned"+ i++);
//     if (true) return;

    super.paint(g);

    Rectangle clipBounds = g.getClipBounds();

    Rectangle clipPos = getPosRectangleForPixels(clipBounds);
    int x = clipPos.x;
    int y = clipPos.y;
    int width = clipPos.width;
    int height = clipPos.height;

    paint(g,x,y,width,height);
  }
  
  /** Return background color for the column */
  private Color getBackgroundColor(int column) {
    if ((column % (stripeWidth*2)) >= stripeWidth) // 2nd stripe
      return getBackgroundColor2();
    else
      return getBackgroundColor1(); // first stripe
  }

  /** For now these just get configs ede bg color - this means alignment
      viewer gets same colors as ede, i alignment wants to be
      different needs to override these methods */
  protected Color getBackgroundColor1() {
    return Config.getExonDetailEditorBackgroundColor1();    
  }
  protected Color getBackgroundColor2() {
    return Config.getExonDetailEditorBackgroundColor2();  
  }

  protected Graphics createGraphics(Graphics g, int x, int y, int w, int h) {
    Graphics out = g.create();
    out.translate(x,y);
    //out.setClip(0,0,w,h);
    return out;
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect,
                                         int orientation,
                                         int direction) {
    if (orientation == Adjustable.HORIZONTAL)
      return (visibleRect.width * 3) / 4;
    else
      return visibleRect.height;
  }

  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect,
                                        int orientation,
                                        int direction) {
    if (orientation == Adjustable.HORIZONTAL)
      return getCharWidth();
    else
      if (horizontalMode)
        return getCharHeight();
      else
        return getRowHeight()+getRowMargin();
  }

  /**
   * Inner class.
   * 
   *
   */
  protected class ScrollMouseListener extends
    javax.swing.event.MouseInputAdapter {
    public void mouseReleased(MouseEvent e) {
      haltThread(verticalScrollThread);
      haltThread(horizontalScrollThread);
    }

    public void mouseDragged(MouseEvent e) {
      handleAutoscroll(e.getX(), e.getY());
    }
  }

  /**
   * Inner class.
   * 
   * 
   */
  protected class AutoscrollThread extends Thread implements Directions {

    private int timeInterval;
    private int distance;
    private JViewport viewport;
    private Component comp;
    private int direction;
    protected boolean halt;

    public AutoscrollThread(int timeInterval,
                            int distance,
                            JViewport viewport,
                            int direction) {
      this.timeInterval = timeInterval;
      this.distance = distance;
      this.viewport = viewport;
      this.direction = direction;
      this.comp = viewport.getView();
      halt = false;
    }

    public void run() {
      Apollo.setLog4JDiagnosticContext();
      do {
        Point p = viewport.getViewPosition();
        int x = (int) p.getX();
        int y = (int) p.getY();
        int verticaloffset = 0;
        int horizontaloffset = 0;

        if (direction == NORTH) {
          verticaloffset = -distance;
        } else if (direction == SOUTH) {
          verticaloffset = distance;
        } else if (direction == EAST) {
          horizontaloffset = -distance;
        } else if (direction == WEST) {
          horizontaloffset = distance;
        }

        x = x + horizontaloffset;
        y = y + verticaloffset;

        Dimension scrollerSize = viewport.getSize();
        Dimension viewSize = viewport.getView().getSize();
        if (x >= 0 &&
            y >= 0 &&
            x + scrollerSize.getWidth() <= viewSize.getWidth() &&
            y + scrollerSize.getHeight() <= viewSize.getHeight()) {
          Point newPos = new Point(x,y);
          viewport.setViewPosition(newPos);
          comp.repaint();
        } else {
          halt = true;
          return;
        }
        try {
          sleep(timeInterval);
        } catch (InterruptedException e) {
          halt = true;
          return;
        }
      } while (!halt);
      Apollo.clearLog4JDiagnosticContext();
    }

    public boolean getHalt() {
      return halt;
    }

    public void halt() {
      halt = true;
    }
  }


  protected void haltThread(AutoscrollThread thread) {
    if (thread == null) {
      return;
    }
    if (thread.getHalt()) {
      return;
    }
    thread.halt();
    thread.interrupt();
  }

  protected void handleAutoscroll(int x, int y) {
    int threshold = 20;
    int interval = 100;
    int distance = getCharHeight();
    Component parent = getParent();
    if (parent != null && parent instanceof JViewport) {
      JViewport view = (JViewport) parent;
      Rectangle rect = view.getViewRect();
      boolean upperHotspot = (y < rect.getY()+threshold);
      boolean leftHotspot = (x < rect.getX()+threshold);
      boolean lowerHotspot = (y > rect.getY()+rect.getHeight()-threshold);
      boolean rightHotspot = (x > rect.getX()+rect.getWidth()-threshold);
      if (!leftHotspot && !rightHotspot) {
        haltThread(horizontalScrollThread);
      }

      if (!upperHotspot && !lowerHotspot) {
        haltThread(verticalScrollThread);
      }

      if (leftHotspot) {
        haltThread(horizontalScrollThread);
        horizontalScrollThread =
          new AutoscrollThread(interval,
                               distance,
                               view,
                               AutoscrollThread.EAST);
        horizontalScrollThread.start();
      }
      if (rightHotspot) {
        haltThread(horizontalScrollThread);
        horizontalScrollThread =
          new AutoscrollThread(interval,
                               distance,
                               view,
                               AutoscrollThread.WEST);
        horizontalScrollThread.start();
      }
      if (upperHotspot) {
        haltThread(verticalScrollThread);
        verticalScrollThread =
          new AutoscrollThread(interval,
                               distance,
                               view,
                               AutoscrollThread.NORTH);
        verticalScrollThread.start();
      }
      if (lowerHotspot) {
        haltThread(verticalScrollThread);
        verticalScrollThread =
          new AutoscrollThread(interval,
                               distance,
                               view,
                               AutoscrollThread.SOUTH);
        verticalScrollThread.start();
      }
    }
  }
}
