package apollo.gui.genomemap;

import java.util.*;

import apollo.gui.Controller;
import apollo.gui.TierManager;
import apollo.gui.Transformer;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSetI;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.TierProperty;

import org.apache.log4j.*;

public class DrawableTierManager extends TierManager 
  implements DrawableTierManagerI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DrawableTierManager.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected Vector drawables;
  protected Hashtable  tierhash   = new Hashtable();
  protected Transformer transformer = null;

  public DrawableTierManager() {
  }

  /** This is needed so that the features (in DNA coord space)
      can be transformed into pixel space by the tiers to 
      determing how much horizontal real estate they need
  */
  public void setTransformer(Transformer transformer) {
    this.transformer = transformer;
  }

  /* this is called from the View that is using this manager */
  public void setTierData(Object data) {
    setDrawables((Vector) data);
  }

  public void fillTiers() {
    tiers.removeAllElements();
    tierhash.clear();
    if (drawables != null) {
      int drawable_count = drawables.size();
      for (int i = 0; i <  drawable_count; i++) {
        populateTier((Drawable) drawables.elementAt(i));
      }
    }
  }

  protected void setDrawables(Vector drawables) {
    this.drawables = drawables;
    /* This will bounce up to the generic TierManager which
       in turn will call fillTiers, so that the Vector of tiers
       is set appropriately */
    doLayoutTiers();
  }

  /**
   * This private method descends the FeatureSet tree finding FeatureSets and
   * SeqFeatures to be added to tiers, creating new tiers as necessary. It is
   * recursive.
   * NOTE:  once a FeatureSet has been found which contains a FeatureProperty
   * no further descent is done. It is assumed that all features below this
   * will be of the same type. If this is not a correct assumption this
   * routine will need to be modified.
   *
   * After recursing through FeatureSetI's and getting to DrawableSeqFeatures
   * getFeatureProperty == null (i guess that indicates its not a FeatureSetI)
   * it is added to its tier with Tier.addFeature
   * (a new tier is created if there is not a tier already for the label
   * of the DSF). Tier.addFeature is presumably where features may get sorted.
   * This is true of FeatureTier.addFeature(DSF).
   * Each "tier" gets sorted, but what is a "tier". When expanded
   * (via FlexibleFeatureTierManager) a tier is a horizontal row,
   * so each row is sorted. When collapsed (FFTM leaves it as
   * CollapsedFTM has left it) each row consists of many tiers.
   * The tiers themselves are sorted but the row as a whole
   * is not, and thus any optimization (like in FeatureView.findFeatures)
   * that is counting on a row to be sorted can not be used in the
   * collapsed case. This could be rectified by sorting the whole row
   * by thats probably not so efficient.
   */
  /**
   * Add a Feature to a tier with which it has no overlaps. This routine is
   * called recursively with each of the tiers that this Feature can be
   * be added to. If no non-overlapping tier is found a new tier is created
   * and added to the newTiers Vector, and the Feature is added to this
   * newly created tier.
   *
   * NOTE: This routine requires that the Features are delivered in sorted
   *       order on low.
   *
   * Put in call to Drawable.setTierIndex to set its tier and keep drawables
   * in synch with tiers - this did not work - not sure why - so im using
   * FeatureTierManager.synchDrawablesWithTiers to do the same task instead.
   * This should be reexamined. This should be the place where the assignment
   * happens but obviously I'm missing something.
   */
  protected void populateTier(Drawable dsf) {
    FeatureProperty fp;
    TierProperty tp;

    if (dsf.isVisible()) {
      if (dsf.isDrawn()) {
        /* cross my fingers, but if the thing is drawn then
        it should have a feature property to go with it
        */
        fp = dsf.getFeatureProperty();
        tp = fp.getTier();
        // Score thresholding here
        // DragFeatureView sets ignoreScoreThresholds to true as it may be
        // dragging feats under threshold that qualified due to siblings
        // in same feature set being over threshold
        if (ignoreScoreThresholds() ||
            dsf.getFeature().getScore() >= fp.getThreshold()) {
          String label = "";
          if (fp == null) {
            logger.error ("Unable to find FeatureProperty for " +
                          dsf.getFeature().getClass().getName() +
                          " : " + dsf.getName() +
                          " type " + dsf.getFeature().getTopLevelType());
          }
          if (tp == null) {
            logger.error ("Unable to find TierProperty for " +
                          fp.getDisplayType() +
                          " : " + dsf.getName() +
                          " type " + dsf.getFeature().getTopLevelType());
          }
          try {
            label = tp.getLabel();
          } catch (Exception e) {
            logger.error ("It is likely that the type description for " +
                          fp.getDisplayType() + 
                          " is in your tiers file more than once.\n" +
                          "Please fix your tiers file!", e);
            return;
          }
          if (tierhash.containsKey(label)) {
            FeatureTier tier = (FeatureTier)tierhash.get(label);
            tier.addFeature(dsf, transformer);
          }
          else {
            FeatureTier tier = new FeatureTier();
            tiers.addElement(tier);
            tierhash.put(label,tier);
            // sorts the features as they are added
            tier.addFeature(dsf, transformer);
          }
        }
      }
      else {
        if (dsf instanceof DrawableSetI) {
          DrawableSetI fs = (DrawableSetI) dsf;
          int fsSize = fs.size();
          for (int i=0; i<fsSize; i++) {
            // Recurse here
            Drawable d = fs.getDrawableAt(i);
            populateTier(d);
          }
        }
        else {
          logger.error ("DrawableTierManager.populateTier: ignoring unknown drawable of type " + dsf.getType() + ", name " +
                        dsf.getName() + ", class " +
                        dsf.getClass().getName(), new Throwable());
        }
      }
    }
  }

  /** Clears features from tiers, and clear tiers */
  public void clearFeatures() {
    for (int i = 0; i < tiers.size(); i++)
      ((FeatureTier)tiers.elementAt(i)).clearDrawables();
    tiers = new Vector(); // tiers = null;
    drawables = null; // new FeatureSet(); ?
    tierhash.clear();
  }

  // NOTE: This routine requires that the features are in sorted order in
  //       the tiers (which they are in FeatureTiers).
  /**
   * int[] limits is an array of 2 ints corresponding to low and high
   * limits
   * Returns a Vector of Vectors, each Vector within the outer Vector
   * contains DrawableSeqFeatures that are the visible Features for
   * a given tier
   * Each DrawableSeqFeature is assigned a tier index corresponding to vertical
   * placement. All the DrawableSeqFeatures at the same vertical level have the
   * same tier index. The tier indices can go higher than the total number 
   * of tiers. This happens when a given tier is hidden it obviously is not 
   * added to the returned Vector of visible features, but the number it 
   * wouldve been assigned is skipped and not reused for the next tier.
   *
   *
   * The following note:
   * NOTE: This routine requires that the features are in sorted order in
   *  the tiers (which they are in FeatureTiers).
   *   is once again true.
   * There was an optimization that assumed the DSFs were sorted from low to high,
   * so when it hit a DSF that was above the bounds (>limits[1]) it would stop
   * looking at the rest of the DSFs in that "tier".
   * There are 2 problems with the sorting assumption.
   * 1) The reverse strand is sorted high to low (when expanded)
   * This could be solved by reversing the order for reverse strand
   * but more killer is:
   * 2) When collapsed all the "subtiers" get jammed together sequentially
   * and there is no ordering for a whole collapsed row.
   * this could be solved by doing a sort of the collapsed row but it
   * probably isnt worth it.
   * So now it looks at all the features from a "subtier"
   *
   * It turns out there was just a sorting bug for collapsed tiers that is now
   * fixed so the optimization can once again assume sorting and is reinstated.
   */

  public Vector getVisibleDrawables(int [] limits) {
    Vector visFeatures = new Vector();

    int lowestVisTier = getLowestVisible() < 0 ? 0 : getLowestVisible();
    int highestVisTier = getLowestVisible() + getNumVisible();
    if (highestVisTier < 0) 
      highestVisTier = 0;
    if (highestVisTier > tiers.size())
      highestVisTier = tiers.size();

    for (int i = lowestVisTier; i < highestVisTier; i++) {
      Vector drawables = ((FeatureTier)tiers.elementAt(i)).getDrawables();
      Vector curVis = null;
      boolean beyondVisibleLimits = false;
      int featSize = drawables.size();
      for (int j = 0; j < featSize && !beyondVisibleLimits; j++) {
        Drawable dsf = (Drawable) drawables.elementAt(j);
        dsf.setTierIndex(i);
        if (dsf.isVisible() &&
            dsf.getHigh() >= limits[0] &&
            dsf.getLow() <= limits[1]) {
          if (curVis == null) {
            curVis = new Vector();
            visFeatures.addElement(curVis);
          }
          curVis.addElement(dsf);
        }
        beyondVisibleLimits = (dsf.getLow() > limits[1]);
      }
    }
    // WHY
    updateUserCoordBoundaries();
    return visFeatures;
  }

}
