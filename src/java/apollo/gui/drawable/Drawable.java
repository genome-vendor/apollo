package apollo.gui.drawable;

import java.awt.*;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.TierManagerI;
import apollo.gui.SelectableI;
import apollo.gui.Transformer;
import apollo.config.FeatureProperty;
import apollo.gui.genomemap.PixelMaskI;

/**
 * An interface defining methods necessary for a drawable object
 */
public interface Drawable extends SelectableI {

  /**
     returns the number of levels in the hierarchy that
     this will draw
  */
  public int getDrawLevel();
  public void setDrawLevel(int level);

  /**
   * Draws the Drawable onto the Graphics
   */
  public boolean draw(Graphics g, 
		      Transformer transformer, 
		      TierManagerI manager,
		      PixelMaskI mask);
  
  public boolean draw(Graphics g,
                      Transformer transformer,
                      TierManagerI manager);

  public int getLeft(Transformer transformer);
  public int getRight(Transformer transformer);

  public String getDisplayLabel();

  /**
   * Set whether this Drawable is to be drawn or not.
   */
  public void    setVisible(boolean visible);
  /**
   * Determine if this Drawable is to be drawn.
   */
  public boolean isVisible();

  /**
   * Set the tier on which this Drawable resides
   */
  public void    setTierIndex(int index);

  /**
   * Get the tier on which this Drawable resides
   */
  public int     getTierIndex(TierManagerI manager);

  /** each drawable contains the actual datamodel for that 
      feature and must support both setting and getting
      the actual datamodel
  */
  public SeqFeatureI getFeature();
  public void setFeature(SeqFeatureI sf);

  public Drawable findDrawable (SeqFeatureI sf);

  /** returns true if FeatureSetI is really just a holder.
      Its not a "real feature". It cant be drawn. 
      e.g. Gene is not a holder, the FeatureSet that holds all of
      the Genes is a holder, its sole purpose is to hold all the genes. */
  public boolean     isDrawn();
  public void        setDrawn(boolean drawn);

  /** 
      sets the FeatureProperty for the drawable, if it is 
      is to be shown
  */
  public FeatureProperty getFeatureProperty();
  // this is drawable internal convenience fn - doesnt need to be part of interface
  //public FeatureProperty getFeatureType(String type);

  public boolean isDecorated();
  public void setHighlighted(boolean state);
  public void setEdgeHighlights(int [] edges,
				boolean state,
				Transformer transformer);
  public Rectangle getBoxBounds();
  public void setBoxBounds(Rectangle r);
  public int getSize(Transformer transformer, TierManagerI manager);
  public boolean intersects (Rectangle pixrect,
                             Transformer transformer,
                             TierManagerI manager);

  /** these are for ease of use and pass through to the
     actual datamodel for the feature */
  public int        getStart();
  public int        getEnd();
  public int        getLow();
  public int        getHigh();
  public int        getStrand();
  public String     getType();
  public String     getName();

  public void setRefDrawable (Drawable dfs);
  public Drawable getRefDrawable ( );

  /** If drawable contains child, deletes it */
  public void deleteDrawable(Drawable child);

  /** For efficiency of drawing, feat prop is cached. This method explicitly resynchs
      drawable with its features feature property */
  public void synchFeatureProperty();
}
