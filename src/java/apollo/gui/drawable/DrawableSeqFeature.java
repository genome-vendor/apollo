package apollo.gui.drawable;

import java.awt.*;
import java.util.*;
import java.util.Hashtable;

import org.apache.log4j.*;

import apollo.datamodel.*;
import apollo.dataadapter.NotImplementedException; // move to util?
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.gui.DetailInfo;
import apollo.gui.TierManagerI;
import apollo.gui.genomemap.LinearView;
import apollo.gui.genomemap.PixelMaskI;
import apollo.gui.genomemap.FeatureTierManagerI;
import apollo.gui.SelectableI;
import apollo.gui.Transformer;

/**
 * A drawable for drawing basic sequence features.
 */
public class DrawableSeqFeature implements Drawable, SelectableI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DrawableSeqFeature.class);

  protected static byte VISIBLE     = 1<<0;
  protected static byte SELECTED    = 1<<1;
  protected static byte HIGHLIGHTED = 1<<2;
  protected static byte LEFTEDGE    = 1<<3;
  protected static byte RIGHTEDGE   = 1<<4;
  protected static byte LABELED     = 1<<5;
  protected static byte DECORATED   = (byte)(LEFTEDGE | 
                                             RIGHTEDGE | 
                                             HIGHLIGHTED | 
                                             SELECTED | 
                                             LABELED);

  // These are not used!
  //protected static String [] stopCodons = {"TAG","TGA","TAA"};
  //private static String[] stopCodons = DNAUtils.get3NucleotideStopCodons();
  //protected static String [] startCodons = {"ATG"};
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
 
  protected SeqFeatureI        feature;
  protected Drawable        drawable_parent;
  protected byte               flags;

  /** changed from yindex tier number
      tier_index -1 says i havent been assigned a tier yet */
  private int tier_index = -1;

  /** Allocate a rectangle to put bounding box in because instantiation
   // is so slow. But I dont think this boxBounds is used anywhwere, all
   // the boxBounds are passed in - am i missing something - MG */
    protected Rectangle          boxBounds = new Rectangle();

  PropertyScheme pscheme;

  protected int draw_level = 1;
  protected boolean is_drawn = false;

  /** features FeatureProperty. For efficiency reasons we store this rather
      than query the feature for it. querying the feature for it every time 
      causes gui things (like zooming) to slow down */
  private FeatureProperty featureProperty;

  public DrawableSeqFeature(boolean drawn) {
    setDrawn(drawn);
  }
  
  public DrawableSeqFeature(SeqFeatureI feature, boolean drawn) {
    setFeature(feature);
    setDrawn(drawn);
  }

  /* This is needed when new drawables are created for drag 
     boxes. It should not be needed in any other case because
     the parent sets it appropriately */
  public void setDrawn (boolean drawn) {
    this.is_drawn = drawn;
  }
  
  public int getDrawLevel () {
    return draw_level;
  }

  public void setDrawLevel (int level) {
    this.draw_level = level;
  }

  public SeqFeatureI getFeature() {
    return feature;
  }

  /** Part of Drawable interface. Currently only called from constructor and 
      DrawableFeatureSet. Is there a scenario where we would publicly change 
      the model of a drawable? 
      It seems like a drawable reason for being comes
      out of a model object, and ya probably arent going to slip in a new model
      to a drawable. so im wondering if this needs to be part of drawable interface
      and perhaps this method should be protected?
      Subclasses overriding setFeature should call super.setFeature to get
      featureProperty set
  */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
    // Need to reset featureProperty with new feature 
    // no feature prop for no_type
    if (!feature.getTopLevelType().equals(RangeI.NO_TYPE)) {
      synchFeatureProperty();
    }
  }
  
  /** Need to change cached feature property - resynch with feature */
  public void synchFeatureProperty() {
    featureProperty = getFeatureType(feature.getTopLevelType());
  }

  public int getLeft(Transformer transformer) {
    if (transformer.getXOrientation() == Transformer.LEFT) {
      return getLow();
    } else {
      return getHigh();
    }
  }

  public int getRight(Transformer transformer) {
    if (transformer.getXOrientation() == Transformer.LEFT) {
      return getHigh();
    } else {
      return getLow();
    }
  }

  /** This returns the variable called tier_index
      so i renamed to getTierIndex - hope thats ok.
      if tier_index == -1 then hasnt got a tier yet.
      synchDrawablesWithTiers assigns all tiers this happens when doing a find
      on a feature that has never been in view
  */
  public int getTierIndex(TierManagerI manager) {
    if (this.tier_index == -1) {
      // synchDrawablesWithTiers is supposed to set tier_index, 
      // but for some reason, it's staying -1
      // tier_index gets set via setTierIndex()
      if (manager instanceof FeatureTierManagerI)
        ((FeatureTierManagerI) manager).synchDrawablesWithTiers();
      // We don't really need to hear this warning if it happens to be a codon, do we?
      //      else
      //        logger.warn ("getTierIndex: manager is a " + 
      //                     manager.getClass().getName() +
      //                     " so no synch for " + getName());
      // only do this if synchDrawablesWithTiers fails to set tier_index
      // and its still -1
      // (which of course shouldnt happen - need to explore further)
      if (tier_index==-1) {
	this.tier_index = 0;
      }
    }
    return this.tier_index;
  }

  public Rectangle getBoxBounds() {
    return boxBounds;
  }

  public void setBoxBounds(Rectangle r) {
    boxBounds = r;
  }

  /**
   * This method draws the feature on the Graphics using a Transformer for
   * coordinate transformations. 
   * There are minimum size limits set the displayed
   * feature so that it is not too small to be seen (3 pixels X and Y).
   */
  public boolean draw(Graphics g, Transformer transformer,
                      TierManagerI manager, PixelMaskI mask) {
    setLabeled();
    DrawableUtil.setBoxBounds(this, transformer, manager);

    boolean obscured = (mask == null ? false :
			mask.isCompletelyObscured(boxBounds.x,
						  boxBounds.x + 
						  boxBounds.width,
						  getTierIndex(manager)));
			
    if (!obscured || isSelected()) {
      if (wantToDraw(g,manager,transformer,boxBounds)) {
        mask.setPixelRangeState(boxBounds.x,
                                boxBounds.x + boxBounds.width,
                                true,
                                getTierIndex(manager));
        feature_draw(g,transformer,manager,boxBounds);
        return true;
      }
    }
    return false;
  }

  /**
   * This method draws the feature on the Graphics using a Transformer for
   * coordinate transformations. There are minimum size limits set the 
   * displayed
   * feature so that it is not too small to be seen (3 pixels X and Y).
   */
  public boolean draw(Graphics g, Transformer transformer,
                      TierManagerI manager) {
    setLabeled();

    DrawableUtil.setBoxBounds(this, transformer, manager);

    if (wantToDraw(g,manager,transformer,boxBounds)) {
      feature_draw(g,transformer,manager,boxBounds);
      return true;
    }
    else
      return false;
  }

  protected boolean wantToDraw(Graphics g,
                               TierManagerI manager,
                               Transformer transformer,
                               Rectangle boxBounds) {
    if (!isVisible()) {
      return false;
    }

    /* This is slow - g.getClipBounds() allocates a new Rectangle */
    /* However it is useful when only part of the window needs redrawing */
    /* Use the FastClippingGraphics which doesn't do the alloc */

    Rectangle clipBounds = g.getClipBounds();
    if (boxBounds.width != 0 && !clipBounds.intersects(boxBounds)) {
      if (isLabeled()) {
        if (getLabeledBounds(transformer,manager,g,boxBounds).intersects(clipBounds)) {
          return true;
        } else {
          return false;
        }
      }
      return false;
    }
    return true;
  }

  // NOTE USE STATIC Rectangle for speed (saves allocation).
  private static Rectangle labStaticBounds = new Rectangle();

  // DO NOT USE THIS METHOD UNLESS YOU'RE GOING TO BE VERY CAREFUL
  private Rectangle getLabeledBounds(Transformer transformer,
                                     TierManagerI manager,
                                     Graphics g,
                                     Rectangle boxBounds) {

    int low  = (int) manager.getTier(getTierIndex(manager)).getDrawLow();
    int high = (int) manager.getTier(getTierIndex(manager)).getCharHigh();

    Point lowpos = transformer.toPixel(getLeft(transformer),low);
    Point highpos = transformer.toPixel(getRight(transformer),high);

    // The 100s are a bodge to save having to calculate the string width here.
    if (transformer.getYOrientation() == Transformer.DOWN) {
      labStaticBounds.x = lowpos.x;
      labStaticBounds.y = lowpos.y;
      labStaticBounds.width = 100;
      labStaticBounds.height = highpos.y-lowpos.y+2;
    }
    else {
      labStaticBounds.x = lowpos.x;
      labStaticBounds.y = highpos.y;
      labStaticBounds.width = 120;
      labStaticBounds.height = lowpos.y-highpos.y+2;
    }
    return labStaticBounds;
  }

  protected void feature_draw(Graphics g, 
			      Transformer transformer,
			      TierManagerI manager,
			      Rectangle boxBounds) {
    if (isSelected()) {
      drawSelected(g, boxBounds, transformer, manager);
    } 
    else {
      drawUnselected(g, boxBounds, transformer, manager);
    }

    addHighlights(g,boxBounds,transformer,manager);

    addDecorations(g,boxBounds,transformer,manager);

    return;
  }

  /** highlights edges and box around if isHighlighted */
  public void addHighlights(Graphics g,
                            Rectangle boxBounds,
                            Transformer transformer,
                            TierManagerI manager) {
    if (isHighlighted()) {
      
      g.setColor(Config.getHighlightColor());
      g.drawRect(boxBounds.x-2,boxBounds.y-2,
                 boxBounds.width+4,boxBounds.height+4);
    }

    // Edge match highlights were just one pixel wide.  Make wider.
    int width = Config.getEdgematchWidth();
    if (isLeftEdge()) {
      g.setColor(Config.getEdgematchColor());
      if (width == 1) {
        g.drawLine(boxBounds.x, boxBounds.y,
                   boxBounds.x, boxBounds.y+boxBounds.height);
      } else {
        g.fillRect(boxBounds.x-1,boxBounds.y,
                   width, boxBounds.height+1);
      }
    }

    if (isRightEdge()) {
      g.setColor(Config.getEdgematchColor());
      if (width == 1) {
        g.drawLine(boxBounds.x+boxBounds.width, boxBounds.y,
                   boxBounds.x+boxBounds.width, boxBounds.y+boxBounds.height);
      } else {
        g.fillRect(boxBounds.x+boxBounds.width-1, boxBounds.y,
                   width, boxBounds.height+1);
      }
    }
  }

  /**  draw in label and codons */
  protected void addDecorations(Graphics g,
                             Rectangle boxBounds,
                             Transformer transformer,
                             TierManagerI manager) {
    if (isLabeled()) {
      String name = getDisplayLabel();
      if (name != null) {
        // This is the lower Y coordinate (not X)
        int y = (int) manager.getTier(getTierIndex(manager)).getCharLow();
        int x = getLeftmostVisible(transformer);
        int text_x = transformer.toPixelX(x);
        int text_y = transformer.toPixelY(y);
        // NOTE Changed
        if (transformer.getYOrientation() == Transformer.DOWN) {
          text_y += g.getFontMetrics().getAscent();
        } else {
          text_y -= g.getFontMetrics().getDescent();
        }
        // this makes no sense - why would you be asking if ref feature is annot
        // dont need this for exons and wont work for 1 levels - changing
        if (feature != null && feature.hasAnnotatedFeature()) {
            //feature.getRefFeature() instanceof AnnotatedFeatureI) {
          g.setColor(Config.getStyle().getAnnotationLabelColor());
        }
        else {
          g.setColor(Config.getFeatureLabelColor());
        }
        g.setFont (getFont ());
        g.drawString(name, text_x, text_y);
      }
    }
    // Draw stop codons if they have been provided

    if (feature.getRefSequence() != null &&
        feature.getRefFeature() != null) {
      drawStartAndStopCodons(g, boxBounds, transformer, manager);
    }
  }

  private Font getFont () {
    SeqFeatureI sf = (feature != null ? feature.getRefFeature() : null);
    if (sf != null && sf.getFeatureType().equalsIgnoreCase("gene")) {
      String pep_status = sf.getProperty ("sp_status");
      return (Config.getPeptideStatus(pep_status).getFont());
    } else {
      return Config.getDefaultFeatureLabelFont();
    }
  }


  public void drawStartAndStopCodons(Graphics g, 
                                     Rectangle boxBounds,
                                     Transformer transformer,
                                     TierManagerI manager) {


    FeatureSetI fs = (FeatureSetI) feature.getRefFeature();
    if (fs.isMissing3prime()) {
      if (fs.getFeatureIndex(feature) == (fs.size() - 1)) {
        boolean pointRight = ((transformer.getXOrientation() == 
                               Transformer.LEFT &&
                               getStrand() == 1) ||
                              (transformer.getXOrientation() ==
                               Transformer.RIGHT &&
                               getStrand() == -1));

        Color backgroundColour = manager.getView() instanceof LinearView ? ((LinearView)(manager.getView())).getBackgroundColour() : Config.getAnnotationBackground();

        drawMissing(g, 
                    boxBounds,
                    pointRight, 
                    //transformer.toPixelX(feature.getEnd()), 
                    pointRight ? boxBounds.x+boxBounds.width : boxBounds.x,
                    Color.red,
                    backgroundColour);
      }
    }
    else {
      int position = fs.getTranslationEnd();
      // make sure the sequence is available since we do show
      // features that begin within, but extend beyond the
      // available sequence
      if (feature.contains(position) &&
          feature.isSequenceAvailable(position)) {
        drawBases(g,
                  boxBounds,
                  transformer,
                  position,
                  3,
                  Color.red);
      }
    }

    if (fs.isMissing5prime()) {
      if (fs.getFeatureIndex(feature) == 0) {
        boolean pointRight = !((transformer.getXOrientation() == 
                                Transformer.LEFT &&
                                getStrand() == 1) ||
                               (transformer.getXOrientation() ==
                                Transformer.RIGHT &&
                                getStrand() == -1));
        Color backgroundColour = manager.getView() instanceof LinearView ? ((LinearView)(manager.getView())).getBackgroundColour() : Config.getAnnotationBackground();
        drawMissing(g, 
                    boxBounds,
                    pointRight, 
                    //transformer.toPixelX(feature.getStart()), 
                    pointRight ? boxBounds.x+boxBounds.width : boxBounds.x,
                    Color.green,
                    backgroundColour);
      }
    }
    else {
      int position = fs.getTranslationStart();
      // make sure the sequence is available since we do show
      // features that begin within, but extend beyond the
      // available sequence
      if (feature.contains(position) &&
          feature.isSequenceAvailable(position)) {
        drawBases(g,
                  boxBounds,
                  transformer,
                  position,
                  3,
                  DrawableUtil.getStartCodonColor(fs));
      }
    }
    int position = fs.plus1FrameShiftPosition();
    if (feature.contains(position) &&
        feature.isSequenceAvailable(position)) {
      drawBases(g,
                boxBounds,
                transformer,
                position,
                1,
                Color.yellow);
    }
    position = fs.minus1FrameShiftPosition();
    if (feature.contains(position) &&
        feature.isSequenceAvailable(position)) {
      drawBases(g,
                boxBounds,
                transformer,
                position,
                1,
                Color.yellow);
    }
    position = fs.readThroughStopPosition();
    // make sure the sequence is available since we do show
    // features that begin within, but extend beyond the
    // available sequence
    if (feature.contains(position) &&
        feature.isSequenceAvailable(position)) {
      drawBases(g,
                boxBounds,
                transformer,
                position,
                3,
                Color.pink);
    }
  }

  protected int getCodonX (Transformer transformer, int x_position) {
    int codon_x;
    if (transformer.getXOrientation() == Transformer.LEFT &&
        getStrand() == 1)
      codon_x = transformer.toPixelX(x_position - 1);
    else if (transformer.getXOrientation() == Transformer.LEFT &&
             getStrand() == -1)
      codon_x = transformer.toPixelX(x_position - 3);
    // if right then the DNA has been reverse-complemented
    // by the user
    else if (transformer.getXOrientation() == Transformer.RIGHT &&
             getStrand() == 1)
      codon_x = transformer.toPixelX(x_position + 2);
    else
      codon_x = transformer.toPixelX(x_position);
    return codon_x;
  }

  private void drawBases(Graphics g,
                         Rectangle boxBounds,
                         Transformer transformer,
                         int position,
                         int base_count,
                         Color colour) {
    int codon_x = getCodonX (transformer, position);

    g.setColor(colour);
    // 3 bases per codon
    int width = (int) (base_count * transformer.getXPixelsPerCoord());
    
    if (width < 1) {
      width = 1;
    }
    /* make it one pixel higher so that it matches what is done
       in Drawables FillRect */
    g.fillRect (codon_x, boxBounds.y, width, boxBounds.height + 1);
  }

  protected void drawMissing (Graphics g,
                              Rectangle boxBounds, 
                              boolean pointRight,
                              int edge,
                              Color color,
                              Color backgroundColour) {
    int point_height = Math.min(boxBounds.height, boxBounds.width);

    int x[] = new int [3];
    int y[] = new int [3];
    
    /* upper flange of arrow
    x[0] = edge + (pointRight ? -point_height : point_height);
    y[0] = boxBounds.y;

    // the tip of the arrow
    x[1] = edge;
    y[1] = getYCentre(boxBounds);

    // lower flange of arrow
    x[2] = x[0];
    y[2] = boxBounds.y + boxBounds.height + 1;

    g.setColor(color);
    g.fillPolygon (x, y, 3);
    */
    x[0] = edge;
    y[0] = boxBounds.y;

    x[1] = edge + (pointRight ? -point_height : point_height);
    y[1] = boxBounds.y;

    x[2] = edge;
    y[2] = getYCentre(boxBounds);

    g.setColor(backgroundColour);
    g.fillPolygon (x, y, 3);
    g.setColor(color);
    g.drawLine(x[1], y[1], x[2], y[2]);

    // SMJS Removed +1 from two lines below which was making the triangle lower edge 1 pixel too low
    y[0] = boxBounds.y + boxBounds.height;
    y[1] = boxBounds.y + boxBounds.height;

    g.setColor(backgroundColour);
    g.fillPolygon (x, y, 3);
    g.setColor(color);
    g.drawLine(x[1], y[1], x[2], y[2]);
  }

  public void drawSelected(Graphics g,
                           Rectangle boxBounds,
                           Transformer transformer,
                           TierManagerI manager) {
    // Selection box
    g.setColor(Config.getSelectionColor());
    if (Config.getDraw3D()) {
      g.fill3DRect(boxBounds.x-2,boxBounds.y-2,
                   boxBounds.width+5,boxBounds.height+5,true);
    } else {
      g.fillRect(boxBounds.x-2,boxBounds.y-2,
                 boxBounds.width+5,boxBounds.height+5);
    }

    drawUnselected(g,boxBounds,transformer,manager);
  }

  protected Color getDrawableColor () {
    return DrawableUtil.getFeatureColor(feature, getFeatureProperty());
  }

  public void drawUnselected(Graphics g,
			     Rectangle boxBounds,
			     Transformer transformer,
                             TierManagerI manager) {
    // Internal box
    g.setColor(getDrawableColor());
		   
    /* color by owner if its type gene
       (not Gene object which could be other annot types)
    */
    if (feature != null &&
	feature.getRefFeature() instanceof Transcript) {
      Transcript t = (Transcript) feature.getRefFeature();
      // Aha!  Although the ref feature is always type transcript,
      // getGene reveals its true flavor (e.g. transposable_element).  
      // Need to use this to decide on color.
      // of feature.getType().equals("exon") ??
      if (t.getGene() == null) {
	String m = "ERROR: You are trying to draw a Transcript that has been deleted "+
	  "from its Gene. The view and the model are out of synch. When deleting a "+
	  "transcript from the model an AnnotationChangeEvent must be sent out to "+
	  "notify the view. If you use AnnotationEditor this is done for you.";
	logger.error(m);
	throw new RuntimeException(m);
      }
    }

    if (Config.getDraw3D()) {
      g.fill3DRect(boxBounds.x,boxBounds.y,boxBounds.width,boxBounds.height,true);
    } else {
      g.fillRect(boxBounds.x,boxBounds.y,boxBounds.width,boxBounds.height);
    }
    if (Config.getDrawOutline()) {
      g.setColor(Config.getOutlineColor());
      g.drawRect(boxBounds.x,boxBounds.y,boxBounds.width,boxBounds.height);
    }
  }

  public int getYSpace(Transformer transformer, TierManagerI manager) {
    int tierNum = getTierIndex(manager);
    return manager.getTier(tierNum).getDrawSpace();
  }

  public int getYCentre(Rectangle boxBounds) {
    return (boxBounds.y + (int) ((double)boxBounds.height * 0.5));
  }

  public int getSize(Transformer transformer,TierManagerI manager) {

    int ysize;

    // Get the number of pixels per tier.
    // Pre subtract 2
    int yspace = getYSpace(transformer,manager) -1;

    if (getFeatureProperty().getSizeByScore()) {
      double score = feature.getScore();

      float max = featureProperty.getMaxScore();
      float min = featureProperty.getMinScore();

      if (max > min) {
        ysize  = (int)((score - min)/(max-min) * yspace);
      } else {
        ysize  = (int)((min - score)/(min-max) * yspace);
      }

      if (ysize < 3) {
        ysize = 3;
      } 
      else if (ysize > yspace) {
        ysize = yspace;
      }
      // ysize is always even
    }
    else {
      ysize = yspace;
      if (ysize < 3) {
        ysize = 3;
      }
    }
    return ysize;
  }

  public void setTierIndex(int index) {
    this.tier_index = (short)index;
  }

  public void  setSelected(boolean state) {
    setFlag(state,SELECTED);
  }
  public boolean isSelected() {
    return flagValue(SELECTED);
  }
  public void  setVisible(boolean state) {
    setFlag(state,VISIBLE);
  }

  public boolean isVisible() {
    return flagValue(VISIBLE);
  }

  private void setFlag(boolean state, byte mask) {
    if (state) {
      flags |= mask;
    } else if ((flags & mask) == mask) {
      flags ^= mask;
    }
  }

  private boolean flagValue(int mask) {
    return ((flags & mask) != 0);
  }

  /** features FeautureProperty. For efficiency reasons we store this rather than
      query the feature for it. querying the feature for it every time causes 
      gui things (like zooming) to slow down. It is set with setFeature */
  public FeatureProperty getFeatureProperty() {
    if (featureProperty == null) {
      synchFeatureProperty();
    }
    return featureProperty;
  }

  /** Convenience fn. Queries PropertyScheme for FeatureProperty of type.
      If type not found prints error msg. */
  protected FeatureProperty getFeatureType(String type) {
    if (pscheme == null)
      pscheme = Config.getPropertyScheme();
    FeatureProperty fp = pscheme.getFeatureProperty(type);
    if (fp == null) {
      logger.warn("No type " + type);
    }
    return fp;
  }

  /** I believe the RefFeature that is often stored is actually a DrawableFeatureSet
   * as DrawableFeatureSet is a SeqFeatureI
   * so to get your Drawable parent one would cast (DrawableFeatureSet)getRefFeature()
   * My qualm here is drawables and seqFeatureI/models being muddled.
   * It is not obvious that getRefFeature() actually returns a drawable, I figured it
   * out by looking at other snips of code where the casting happens.
   * Its more confused by the fact that getFeature actually returns the model/SeqFeatureI.
   * I propose a refactor where the model and drawables(usually called view) are separated.
   * - MG
   */

  /* I'm doing what MG suggests now Nov 2003 SUZ   Yahoo! - MG
  public SeqFeatureI getRefFeature() {
    return refFeature;
  }

  public void setRefFeature(SeqFeatureI f) {
    refFeature = f;
  }
  */

  public void setHighlighted(boolean state) {
    setFlag(state,HIGHLIGHTED);
  }

  protected void setLabeled () {
    setLabeled(featureProperty != null &&
               featureProperty.getTier().isLabeled() &&
               (getRefDrawable() == null ||
                (getRefDrawable() != null && ! getRefDrawable().isDrawn())));
  }

  public void setLabeled(boolean state) {
    setFlag(state,LABELED);
  }

  public boolean isHighlighted() {
    return flagValue(HIGHLIGHTED);
  }

  public boolean isDecorated() {
    return flagValue(DECORATED);
  }

  public boolean isLabeled() {
    return flagValue(LABELED);
  }

  public boolean isLeftEdge() {
    return flagValue(LEFTEDGE);
  }

  public boolean isRightEdge() {
    return flagValue(RIGHTEDGE);
  }

  public void setEdgeHighlights(int [] edges,boolean state,Transformer transformer) {

    setFlag(false,LEFTEDGE);
    setFlag(false,RIGHTEDGE);


    int left  = getLeft(transformer);
    int right = getRight(transformer);

    if (Arrays.binarySearch(edges,left) > -1) {
      setFlag(state,LEFTEDGE);
    }
    if (Arrays.binarySearch(edges,right) > -1) {
      setFlag(state,RIGHTEDGE);
    }
  }

  public boolean contains (Point point,
                           Transformer transformer,
                           TierManagerI manager) {
    DrawableUtil.setBoxBounds(this,
			      transformer,
			      manager);
    return boxBounds.contains(point);
  }

  public boolean intersects (Rectangle pixrect,
                             Transformer transformer,
                             TierManagerI manager) {
    DrawableUtil.setBoxBounds(this, transformer, manager);
    return boxBounds.intersects(pixrect);
  }

  public String getDisplayLabel() {
    if (feature == null)
      return "";
    String label = DetailInfo.getName(feature);
    if (label.equals("") || label.equals("no_name")) {
      if (getFeature().getParent() != null)
        label = getFeature().getParent().getName();
    }
    return label;
  }

  // Actually returns a drawable, not the feature itself
  public Drawable findDrawable(SeqFeatureI sf) {
    if (sf == null) {
      return null;
    }
    Drawable found = null;
    if (sameFeature (sf, this.getFeature())) {
      found = this;
    }
    return found;
  }

  protected boolean sameFeature (SeqFeatureI one, SeqFeatureI two) {
    boolean same = (one == two);
    if (one == null || two == null || same) {
      return same;
    }
    
    //return true if one sequence was cloned from another
    if (one.isClone() && one.getCloneSource() == two ||
        two.isClone() && two.getCloneSource() == one) {
      same = true;
    }
    
    if (!same &&
        one.getName().equals(two.getName()) &&
        one.getStart() == two.getStart() &&
        one.getEnd() == two.getEnd() &&
        one.getClass().getName().equals (two.getClass().getName())) {
      // I had to set this to false to fix selection bug with ensembl
      // data. The bug was that the wrong thing was getting selected.
      // The reason was that selection does a findDrawable with the model
      // elements it has on the view. Some features in ensembl have the
      // same name, start, end, and class, so it was returning a feature
      // with these qualities which is not the feature clicked necasarily.
      // This is all due to the selection holding model instead of drawable
      // (as i believe it should). Selection could be improved further
      // where the SelectionItems hold the model and have the drawables as
      // listeners, and set it up this way from the initial find, not
      // after the view gets back the model selection it sent out and has
      // to refind its features (admitedly awkward).
      //same = true;
      same = false;
      //logger.warn ("Why isn't " + one.getName() +
      //	     " the same as " + two.getName());
    }
    return same;
  }

  /** Return y value for writing characters in the box passed in,
   * @param fm FontMetrics for writing the text
   * @param box Rectangle to draw/write into
   */
  protected int getCharY (FontMetrics fm, Rectangle box) {
    int char_height = fm.getAscent() + fm.getDescent();
    if (char_height >= box.height)
      return (box.y + box.height - fm.getLeading());
    else
      return (box.y +
              char_height +
              ((box.height - char_height) / 2));
  }

  protected int getLeftmostVisible(Transformer transformer) {
    int [] visRange = transformer.getXVisibleRange();

    if (transformer.getXOrientation() == Transformer.LEFT) {
      return Math.max(visRange[0], getLeft(transformer));
    } else {
      return Math.min(visRange[1], getLeft(transformer));
    }
  }
  
  public boolean isDrawn() {
    return is_drawn;
  }

  // Choose a font color (dark or light) based on how dark background color is.
  // Need to improve algorithm.
  public Color fontColorForBackground(Color bgColor) {
    // Use brightness plus half of saturation
    float[] HSB = Color.RGBtoHSB(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), null);
    float lightness = HSB[2] + HSB[1]/2;
    Color labelColor = Color.black;
    // Empirically derived.
    // 12/2003 Bev wants lime green results to use black font, not white.
    if (lightness < 1.11)
      labelColor = Color.white;

    logger.debug("Font color for bg color " + bgColor + " (lightness = " + lightness +", B = " + HSB[2] + ") = " + labelColor);
    return labelColor;
  }
  
  /**
   * General implementation of Visitor pattern. (see apollo.util.Visitor).
   * -- has to be implemented here because I implement SeqFeatureI, but I do
   * not extend SeqFeature. I throw a NotImplementedException right now. Change
   * as necessary.
   **/
  public void accept(apollo.util.Visitor visitor){
    if(true){
      throw new apollo.dataadapter.NotImplementedException("Not implemented for DrawableSeqFeature");
    }
    
  }//end accept

  public void setRefDrawable (Drawable dfs) {
    this.drawable_parent = dfs;
  }

  public Drawable getRefDrawable () {
    return this.drawable_parent;
  }

  public int getStart() {
    return feature.getStart();
  }
  
  public int getEnd() {
    return feature.getEnd();
  }

  public int getLow() {
    return feature.getLow();
  }

  public int getHigh() {
    return feature.getHigh();
  }

  public int getStrand() {
    return feature.getStrand();
  }

  public boolean isForwardStrand() {
    return feature.isForwardStrand();
  }

  public String getType() {
    return feature.getFeatureType();
  }

  public String getName() {
    return feature.getName();
  }
  /** Default no-op implementation, DrawableSeqFeature doesnt have children,
      do nothing. Overridded by DrawableFeatureSet */
  public void deleteDrawable(Drawable child) {}
}
