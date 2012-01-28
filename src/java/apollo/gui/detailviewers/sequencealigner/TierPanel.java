package apollo.gui.detailviewers.sequencealigner;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Enumeration;

import org.bdgp.swing.FastTranslatedGraphics;
import org.bdgp.util.Range;
import org.bdgp.util.RangeHolder;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.editor.AnnotationChangeEvent;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidBaseRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidRendererSpaced;
import apollo.gui.detailviewers.sequencealigner.renderers.TestBaseRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRendererI;

/**
 * A Tier Panel is a JPanel which is used to render a TierI. The Orientation of
 * the tier panel determines what direction the tier should be rendered while
 * the BaseRendererI determines how each individual base is rendered.
 */
public class TierPanel extends AbstractTierPanel {

  /** the tier that will be rendered by this panel */
  TierI tier;

  /**
   * the orientation of this panel 
   */
  Orientation orientation;

  /** the renderer for this panel */
  BaseRendererI renderer;

  /**
   * Constructor. The default orientation is FIVE_TO_THREE.
   * 
   * @param tier
   *          the tier to be rendered
   * 
   */
  public TierPanel(TierI tier) {
    this.tier = tier;
    this.orientation = Orientation.FIVE_TO_THREE;
    this.renderer = new TestBaseRenderer();
  }

  /**
   * Constructor.
   * 
   * @param tier
   * @param orientation
   * @param renderer
   */
  public TierPanel(TierI tier, Orientation orientation, BaseRendererI renderer) {
    this.tier = tier;
    this.orientation = orientation;
    this.renderer = renderer;
  }

  /**
   * Paints this tier within the clipping bounds of the graphic.
   * 
   * @param g
   *          the graphic in which the tier will be painted
   */
  public void paint(Graphics g) {

    Rectangle clipBounds = g.getClipBounds();
    Point lowPixel = new Point(clipBounds.x, 0);
    Point highPixel = new Point(clipBounds.x + clipBounds.width, 0);

    int low = getPositionForPixel(lowPixel);
    int high = getPositionForPixel(highPixel);

    paint(g, new RangeHolder(low, high));
  }

  /**
   * Paints the portion of the tier that is inside of the range
   * 
   * @param g
   *          the graphic in which the tier will be painted
   * @param r
   *          the range (in pixel position coordinates) of the tier to be
   *          painted
   */
  private void paint(Graphics g, Range r) {

    FastTranslatedGraphics transG = new FastTranslatedGraphics(g);
    for (int pos = r.getLow(), high = r.getHigh(); pos <= high; pos++) {
      transG.translateAbsolute(pos * getBaseWidth(), 0);

      if (renderer instanceof AminoAcidRendererSpaced) {
        int i = 1;
      }
      
      int p = pixelPositionToTierPosition(pos);
      
      Component c = renderer.getBaseComponent(p, tier, orientation);
      c.paint(transG);

    }
  }

  /**
   * Set the preferred size for this panel
   */
  public void reformat(boolean isRecursive) {
    this.setBackground(Color.red);
    this.setBorder(null);
    this.setOpaque(true);
    int width = tier.getReference().getLength() * getBaseWidth();
    int height = getBaseHeight();
    setPreferredSize(new Dimension(width, height));
  }

  /** pixel size */
  public int getBaseWidth() {
    return renderer.getBaseWidth();
  }

  /** pixel size */
  public int getBaseHeight() {
    return renderer.getBaseHeight();
  }

  /**
   * Gets the (pixel) position in the tierPanel for a given pixel
   */
  public int getPositionForPixel(Point p) {
    int position = p.x / getBaseWidth();
    return position;
  }

  /**
   * Gets a pixel in the tier panel for a given (pixel) position in the tier
   * panel
   * 
   */
  public Point getPixelForPosition(int p) {
    Point pixel = new Point(p * getBaseWidth(), 0);
    return pixel;
  }

  public int pixelPositionToTierPosition(int p) {
    return renderer.pixelPositionToTierPosition(p, tier, orientation);
  }
  
  public int tierPositionToPixelPosition(int p) {
    return renderer.tierPositionToPixelPosition(p, tier, orientation);
  }
  
  /** the highest pixel position on the tier */
  public int getHigh() {
    return Math.max(tierPositionToPixelPosition(tier.getLow()),
                    tierPositionToPixelPosition(tier.getHigh()));
  }

  /** the lowest pixel position on the tier */
  public int getLow() {
    return Math.min(tierPositionToPixelPosition(tier.getLow()),
                    tierPositionToPixelPosition(tier.getHigh()));
  }
  
  /** the highest pixel position on a feature */
  public int getHigh(SeqFeatureI f) {
    return Math.max(
        tierPositionToPixelPosition(tier.getPosition(f.getLow())),
        tierPositionToPixelPosition(tier.getPosition(f.getHigh())));
  }
  
  /** the lowest pixel position on a feature */
  public int getLow(SeqFeatureI f) {
    return Math.min(
        tierPositionToPixelPosition(tier.getPosition(f.getLow())),
        tierPositionToPixelPosition(tier.getPosition(f.getHigh())));
  }
  
  /** Given a pixel position return the next feature on the tier */
  public SeqFeatureI getNextFeature(int p, Level level) {
    
    SeqFeatureI feature = null;
    
    int position = pixelPositionToTierPosition(p);
    if (orientation == Orientation.FIVE_TO_THREE) {
      feature = this.tier.getNextFeature(position, level);
    } else if (orientation == Orientation.THREE_TO_FIVE) {
      feature = this.tier.getPrevFeature(position, level);
    }
    return feature;
  }
  
  /** Given a pixel position return the feature at that position */
  public SeqFeatureI featureAt(int p, Level level) {
    int position = pixelPositionToTierPosition(p);
    return this.tier.featureAt(position, level);
  }
  
  /** Given a pixel position return the previous feature on the tier */
  public SeqFeatureI getPrevFeature(int p, Level level) {
    
    SeqFeatureI feature = null;
    
    int position = pixelPositionToTierPosition(p);
    if (orientation == Orientation.FIVE_TO_THREE) {
      feature = this.tier.getPrevFeature(position, level);
    } else if (orientation == Orientation.THREE_TO_FIVE) {
      feature = this.tier.getNextFeature(position, level);
    }
    return feature;
  }

  public BaseRendererI getRenderer() {
    return this.renderer;
  }

  public void setRenderer(BaseRendererI r) {
    this.renderer = r;
  }

  public TierI getTier() {
    return this.tier;
  }

  public Orientation getOrientation() {
    return this.orientation;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  /*************************************************************************
   * These Are the methods needed to implement the Scrollable interface.
   * 
   * @see javax.swing.Scrollable 
   * ***********************************************/

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect,
      int orientation, int direction) {
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

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
      int direction) {
    if (orientation == Adjustable.HORIZONTAL)
      return getBaseWidth();
    else
      return getBaseHeight();
  }

  public int getVisibleBase() {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getVisibleBaseCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  public void scrollToBase(int pos) {
    // TODO Auto-generated method stub
    
  }

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    getTier().handleAnnotationChangeEvent(evt);
    return true;
  }


}
