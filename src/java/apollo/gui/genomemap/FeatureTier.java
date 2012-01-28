package apollo.gui.genomemap;

import java.util.*;
import java.awt.Color;
import java.awt.Rectangle;

import apollo.datamodel.*;
import apollo.gui.Tier;
import apollo.gui.Transformer;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.drawable.DrawableFeatureSet;
import apollo.config.FeatureProperty;

import org.apache.log4j.*;

/**
 * A Tier to hold features.
 */
public class FeatureTier extends Tier {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureTier.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // as long as the strand of this is set to zero
  // (which is the default) then it will sort features
  // from low to high
  private Vector drawables;
  int min_pos;
  int max_pos;
  int max_pix;
  int textEnd = -1;
  int charHeight;
  int totalHeight;
  String tier_label;

  public FeatureTier() {
    drawables = new Vector();
  }

  /**
   * Calls addFeature on its FeatureSet with the DrawableSeqFeature and the sort flag
   * set to true, this will cause the DSFs to be sorted
   * Since it only allows DrawableSeqFeatures to be added shouldnt the fset be a
   * DrawableFeatureSet instead of just a FeatureSet?
   * Then getFeatureSet could return a DrawableFeatureSet and we wouldnt have to cast it
   * There doesnt seem to be a reason at the moment to let it other features than
   * dsfs - am i right about this?
   */
  public void addFeature(Drawable dsf, Transformer transformer) {
    /* Every feature will be added from low base to high
       because the fset has the strand set to zero */
    int insLoc = getLocation(dsf);

    drawables.insertElementAt(dsf,insLoc);

    if (dsf.getLow() < min_pos) {
      min_pos = dsf.getLow();
    }

    if (dsf.getHigh() > max_pos) {
      max_pos = dsf.getHigh();
      SeqFeatureI sf = dsf.getFeature();
      FeatureProperty fp = dsf.getFeatureProperty();
      Transformer.PixelRange pixRng 
        = transformer.basepairRangeToPixelRange(sf);
      // make sure width is at least minimum width
      pixRng.ensureMinimumWidth(fp.getMinWidth());
      max_pix = pixRng.hi;
    }
  }

  public int getLocation(Drawable dsf) {
    int bot = 0;
    int top = drawables.size() - 1;

    int low  = dsf.getLow();
    int high = dsf.getHigh();

    while (bot <= top) {
      int mid = (bot + top)/2;
      Drawable midFeature = (Drawable) drawables.elementAt(mid);

      int midLow = midFeature.getLow();
      int cmp = 0;
      if (midLow > low) {
        cmp = 1;
      } 
      else if (midLow < low) {
        cmp = -1;
      }

      if (cmp == 0) {
        int midHigh = midFeature.getHigh();
        if (midHigh > high) {
          cmp = 1;
        } 
	else if (midHigh < high) {
          cmp = -1;
        }
      }

      if (cmp < 0)
        bot = mid + 1;
      else if (cmp > 0)
        top = mid - 1;
      else
        return mid;
    }
    return bot;
  }

  public int size() {
    return drawables.size();
  }

  public void removeDrawable(Drawable dsf) {
    drawables.removeElement(dsf);
  }

  void clearDrawables() {
    drawables = null;
  } // new FeatureSet()? go through kids?

  public Vector getDrawables() {
    return drawables;
  }

  public int getHigh() {
    return max_pos;
  }

  public int getPixelHigh() {
    return max_pix;
  }

  public Drawable getDrawableAt(int i) {
    return (Drawable)(drawables.elementAt(i));
  }

  public FeatureProperty getFeatureProperty () {
    FeatureProperty fp = null;
    if (size() > 0) {
      Drawable dsf = (Drawable) drawables.elementAt(0);
      fp = dsf.getFeatureProperty();
    }
    return fp;
  }

  public void setTierLabel(Drawable dsf) {
    if (dsf != null && dsf.getFeatureProperty() != null) {
      tier_label = dsf.getFeatureProperty().getDisplayType();
    } 
    else {
      if (dsf != null)
        logger.error("Feature Property for tier " + 
                     dsf.getType() + " is null");
    } 
  }

  public String getTierLabel() {
    if (tier_label == null && size() > 0) {
      setTierLabel((Drawable) drawables.elementAt(0));
    } 
    return tier_label;
  }

  public Color getColour() {
    Color color = null;
    if (size() > 0) {
      Drawable dsf = (Drawable) drawables.elementAt(0);
      if (dsf.getFeatureProperty() != null) {
        color = dsf.getFeatureProperty().getColour();
      } 
      else {
        logger.error("Feature Property for tier " + 
                     dsf.getType() + " is null");
      }
    }
    return color;
  }

  public boolean isLabeled() {
    boolean labeled = false;
    if (size() > 0) {
      Drawable dsf = (Drawable) drawables.elementAt(0);
      if (dsf.getFeatureProperty() != null &&
          dsf.getFeatureProperty().getTier() != null ) {
        labeled = dsf.getFeatureProperty().getTier().isLabeled();
      } 
      else {
        logger.error("Feature Property for tier " + 
                     dsf.getType() + " is null");
      }
    }
    return labeled;
  }

  public String toString() {
    StringBuffer tmpstr = new StringBuffer();
    for (int i=0; i < drawables.size();i++) {
      Drawable sf = (Drawable) drawables.elementAt(i);
      tmpstr.append("Feature " + i + ". Limits " + sf.getLow() +
                    " " + sf.getHigh() + "\n");
    }
    return tmpstr.toString();
  }

  public int getTextEnd() {
    return textEnd;
  }

  public void setTextEnd(int newEnd) {
    textEnd = newEnd;
  }
}
