
package apollo.gui.genomemap;

import java.util.*;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.TierProperty;
import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.Selection;
import apollo.gui.TierManager;
import apollo.gui.Transformer;
import apollo.gui.FeaturePropertyPanel;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.drawable.DrawableUtil;
import apollo.config.PropSchemeChangeListener;
import apollo.config.PropSchemeChangeEvent;
import apollo.util.QuickSort;

/**
 * Extended from TierManager but still abstract (no layoutTiers methods)
 Implements FeatureTierManagerI which implements ControlledObjectI
 extends DrawableTierManager which extends TierManager which is where the
 ControlledObjectI methods are implemented.
 */
public class FeatureTierManager extends DrawableTierManager implements
  FeatureTierManagerI,
  PropSchemeChangeListener {

  // Vector tiers inherited from TierManager is a vector of FeatureTiers here

  // Feature types is used to get the default order
  protected Vector tier_properties;
  protected boolean            debug = false;

  public FeatureTierManager(Controller c) {
    setController(c);
    tier_properties = Config.getPropertyScheme().getAllTiers();
  }

  /** override the more general method in TierManager so that
      this manager is added as a listener */
  public void setController(Controller c) {
    controller = c;
    if (controller != null)
      controller.addListener((EventListener)this);
  }

  public boolean isExpanded(String type) {
    return (Config.getPropertyScheme().getTierProperty(type).isExpanded());
  }

  public boolean isVisible(String type) {
    return (Config.getPropertyScheme().getTierProperty(type).isVisible());
  }

  public int getIndexForType(String type) {
    for(int i=0; i < getTiers().size(); i++) {
      FeatureTier tier = (FeatureTier) getTiers().elementAt(i);
      if (tier.getTierLabel().equalsIgnoreCase(type))
	return i;
    }
    return -1;
  }

  public void collapseTier(String tier_label) {
    Config.getPropertyScheme().getTierProperty(tier_label).setExpanded(false);
    doLayoutTiers();
  }

  public void expandTier(String type) {
    Config.getPropertyScheme().getTierProperty(type).setExpanded(true);
    doLayoutTiers();
  }
  
  public void showLabelTier(String type) {
    expandTier(type);
    Config.getPropertyScheme().getTierProperty(type).setLabeled(true);
    doLayoutTiers();
  }  

  public void hideLabelTier(String type) {
    Config.getPropertyScheme().getTierProperty(type).setLabeled(false);
    doLayoutTiers();
  }
  public void setVisible(String type,boolean state) {
    Config.getPropertyScheme().getTierProperty(type).setVisible(state);
    doLayoutTiers();
  }

  public Vector getHiddenTiers() {
    Vector hidden = new Vector();
    Vector all_tiers = Config.getPropertyScheme().getAllTiers();

    for (int i = 0; i < all_tiers.size(); i++) {
      TierProperty tp = (TierProperty) all_tiers.elementAt (i);
      if ( ! tp.isVisible() )
	hidden.addElement(tp.getLabel());
    }
    return hidden;
  }

//   public void setAllVisible(boolean state) {
//     PropertyScheme scheme = Config.getPropertyScheme();
//     Vector all_tiers = scheme.getAllTiers();

//     scheme.setGroupedUpdate(true);

//     for (int i = 0; i < all_tiers.size(); i++) {
//       TierProperty tp = (TierProperty) all_tiers.elementAt (i);
//       tp.setVisible(state);
//     }
//     scheme.setGroupedUpdate(false);
//     doLayoutTiers();
//   }

//   public void expandAll(boolean state) {
//     PropertyScheme scheme = Config.getPropertyScheme();
//     Vector all_tiers = scheme.getAllTiers();

//     scheme.setGroupedUpdate(true);

//     for (int i = 0; i < all_tiers.size(); i++) {
//       TierProperty tp = (TierProperty) all_tiers.elementAt (i);
//       tp.setExpanded(state);
//     }
//     scheme.setGroupedUpdate(false);
//     doLayoutTiers();
//   }

  public boolean areAnyTiersLabeled() {
    boolean flag = false;
    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      FeatureTier tier = (FeatureTier) tiers.elementAt(i);
      if (tier.isLabeled()) {
	flag = true;
	break;
      }
    }
    return flag;
  }

  /** This returns a Vector of all features. This has the side effect of assigning
      all DrawableSeqFeatures with their appropriate tier index */
  public Vector getAllFeatures() {
    Vector allFeatures = new Vector();

    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Vector drawables = ((FeatureTier)tiers.elementAt(i)).getDrawables();
      int setSize = drawables.size();
      for (int j=0;j<setSize;j++) {
        Drawable dsf= (Drawable)(drawables.elementAt(j));
        dsf.setTierIndex(i);
        allFeatures.addElement(dsf);
      }
    }
    return allFeatures;
  }

  /**This assigns all DrawableSeqFeatures with their appropriate tier index 
     I added this functionality in FlexibleFeatureTierManager._add
     and _addWithAvoidance but it didnt seem to work - not sure why - 
     but thats where this really belongs. */  
  public void synchDrawablesWithTiers() {
    int tiersSize = tiers.size();
    Vector parents = new Vector();  // keep track of parents of drawables that we've seen
    for (int i=0;i<tiersSize;i++) {
      Vector drawables = ((FeatureTier)tiers.elementAt(i)).getDrawables();
      int setSize = drawables.size();
      for (int j=0;j<setSize;j++) {
        Drawable dsf=(Drawable)(drawables.elementAt(j));
        dsf.setTierIndex(i);
        // Would like to set parent's tier index to tier index of FIRST child
        // Can't check with getTierIndex because it calls synchDrawablesWithTiers
        // so we loop forever!
        Drawable parent = dsf.getRefDrawable();
        if (parents.indexOf(parent) < 0) {
          dsf.getRefDrawable().setTierIndex(i);
          parents.addElement(parent);
        }
      }
    }
  }

  public void setDebug(boolean state) {
    debug = state;
  }

  public boolean getDebug() {
    return debug;
  }

  public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent evt) {
    //tier_properties = Config.getPropertyScheme().getAllTiers();
    // in future may have different prop schemes for different curations
    tier_properties = evt.getPropertyScheme().getAllTiers();
    doLayoutTiers();
    return true;
  }

  /**
   * First calls installDrawables to  add all the DSFs to tiers with sorting.
   * then deal with expanding the tiers
   */
  public void fillTiers() {
    // First, just make sure the drawables are loaded
    super.fillTiers();

    /** tiers is a protected variable in TierManager */
    tiers = sortTiers();

    // Then look for overlaps
    Vector expandedTiers = new Vector();

    int tiersSize = tiers.size();
    PropertyScheme scheme = Config.getPropertyScheme();
    for (int i = 0; i < tiersSize; i++) {
      FeatureTier feature_tier = (FeatureTier) tiers.elementAt(i);
      // sort is used if the FeatureTier does not use a SortedFeatureSet
      // This turns out to be slower than using the SortedFeatureSet,
      // probably because the Collections.sort routine uses compareTo and
      // also makes a copy of the Vector into an array, before sorting.
      // The _add method REQUIRES that the features are sent in sorted order
      //
      // I think this comment is out of date as it appears that
      // SortedFeatureSet does not exist anymore
      // the sorting now seems to happen in

      String feature_type = feature_tier.getTierLabel();

      TierProperty tp = scheme.getTierProperty(feature_type);
      if (isVisible(feature_type)) {
        if (!(tp.isExpanded())) {
          expandedTiers.addElement(feature_tier);
        } else {
          Vector newTiers = new Vector();
          int tierDataSize = feature_tier.size();
          Drawable dsf = (tierDataSize > 0 ? 
                          feature_tier.getDrawableAt(0) : null);
          FeatureTier new_tier = new FeatureTier();
          new_tier.setTierLabel(dsf);
          newTiers.addElement(new_tier);
          if (!tp.isSorted() &&
              tp.isLabeled() &&
              textAvoidance &&
              textTransform.getXCoordsPerPixel() < Config.getTextAvoidLimit()) {
            for (int j = 0; j < tierDataSize; j++) {
              _addWithAvoidance(feature_tier.getDrawableAt(j), 0, newTiers);
            }
          } else {
            for (int j = 0; j < tierDataSize; j++) {
              _add(feature_tier.getDrawableAt(j), 0, newTiers);
            }
          }
          
          if (tp.isSorted()) {
            Vector overlapEnds = new Vector(32);
            newTiers = sortTierByScore(newTiers,tp,overlapEnds);
            int [] overlapEndsArray = new int[overlapEnds.size()];
            int overlapEndsSize = overlapEnds.size();
            for (int j = 0; j <overlapEndsSize; j++) {
              overlapEndsArray[j] =((Integer)overlapEnds.elementAt(j)).intValue();
            }
            if (tp.isLabeled() &&
                textAvoidance &&
                textTransform.getXCoordsPerPixel() < Config.getTextAvoidLimit()) {
              newTiers = removeTextOverlapsFromSorted(newTiers,tp,
                  overlapEndsArray);
            }
          }
          // Add new tiers to the final tiers vector
          int newTiersSize = newTiers.size();
          for (int j = 0; j < newTiersSize; j++) {
            expandedTiers.addElement(newTiers.elementAt(j));
          }
        }
      }
    }
    
    tiers = expandedTiers;
    
    if (debug) {
      checkTiers();
    }
    
  }

  private boolean checkTiers() {
    boolean error = false;
    for (int i=0;i<tiers.size();i++) {
      FeatureTier tier = (FeatureTier)tiers.elementAt(i);
      for (int j=0;j<tier.size()-1;j++) {
	Drawable one = tier.getDrawableAt(j);
        for (int k=j+1;k<tier.size();k++) {
	  Drawable two = tier.getDrawableAt(k);
          if (one.getFeature().overlaps(two.getFeature())) {
	    System.out.println("ERROR: Overlap in tier " + i);
            System.out.println("\tone = " + one.getLow() + "-" + one.getHigh()+
                               " two " + two.getLow() + " " + two.getHigh());
            error = true;
          }
        }
      }
    }
    return error;
  }
    
  public String toString() {
    StringBuffer buff = new StringBuffer();

    for (int i=0;i<tiers.size();i++) {
      FeatureTier tier = (FeatureTier)tiers.elementAt(i);
      buff.append("Tier " + i + "\n");
      buff.append(tier);
    }
    return buff.toString();
  }

  /**
   * Sort tiers into FeatureType order
   */
  public Vector sortTiers() {
    Vector sortedTiers = new Vector();
    int tierPropSize = tier_properties.size();
    int tiersSize = tiers.size();

    for (int i=0; i < tierPropSize; i++) {
      TierProperty tier_prop = (TierProperty) tier_properties.elementAt(i);

      for (int j = 0; j < tiersSize; j++) {
        FeatureTier     ft = (FeatureTier) tiers.elementAt(j);
        FeatureProperty fp = ft.getFeatureProperty();
        
        if (fp.getTier().getLabel().equalsIgnoreCase(tier_prop.getLabel())) {
          sortedTiers.addElement(ft);
        }
      }
    }
    return sortedTiers;
  }

  protected void _add(Drawable dsf, int tierInd, Vector newTiers) {
    //Check feature against new tier
    FeatureTier tier     = (FeatureTier)(newTiers.elementAt(tierInd));
    boolean     overlaps = false;
    //    int        dsfLow    = dsf.getLow();

    // Any feature already added will have a lower Low than dsf (because they
    // are added in Low order). Therefore if and only if the High of the 
    // existing
    // FeatureSet in the Tier is higher than or equal to the Low of the new
    // Feature there must be an overlap.
    overlaps = drawableOverlaps(tier, dsf);
    
    //If overlaps
    if (overlaps) {
      tierInd++;
      //  if next new tier
      if (tierInd < newTiers.size()) {
        //    recurse (next new tier)
	_add(dsf, tierInd, newTiers);
      } else {
	//    add new tier
	FeatureTier t = new FeatureTier();
        t.setTierLabel(dsf);
	newTiers.addElement(t);
	//    add feature
	t.addFeature(dsf, transformer);
	// dsf keeps the tier state - since its a new tier its tierInd+1
	dsf.setTierIndex(tierInd + 1);
      }
    } else {
      //  add to tier
      tier.addFeature(dsf, transformer);
      // dsf keeps the tier state - used for scrolling
      dsf.setTierIndex(tierInd);
    }
    //endif
  }

  protected Vector sortTierByScore(Vector oldTiers, TierProperty tp,
				   Vector overlapEnds) {

    /* Find each set of overlapping features
    // Rearrange these sets into sorted order on some score
    // Ah - problem if one sets overlaps with two or more others - need to
    // split the two into separate tiers so can sort separately */
    
    int oldTiersSize = oldTiers.size();
    int [] startInds = new int[oldTiersSize];

    // These arrays are for speed optimisation.
    // NOTE: An Enumeration won't work - 
    // we need to be able to go back to the previous
    //       element - an Iterator would but we don't get those in 1.1 - sigh
    Vector [] oldTiersFeatures = new Vector[oldTiersSize];
    int [] oldTiersSizes = new int[oldTiersSize];
    
    Vector newTiers = new Vector();
    for (int i=0;i<oldTiersSize;i++) {
      newTiers.addElement(new FeatureTier());
      oldTiersFeatures[i] 
        = ((FeatureTier)oldTiers.elementAt(i)).getDrawables();
      oldTiersSizes[i] = oldTiersFeatures[i].size();
    }

    // For each overlap
    
    Vector overlap;
    while ((overlap = getNextOverlap(oldTiers,oldTiersSize,startInds,
				     oldTiersFeatures,oldTiersSizes,
				     overlapEnds)) != null) {
      sortOverlap(newTiers,overlap);
    }

    int maxRow = tp.getMaxRow();
    if (maxRow > 0 && newTiers.size() > maxRow) {
      newTiers.setSize(maxRow);
    }
    return newTiers;
  }

  // Ascend tiers looking for overlaps. If the high edge changes then we need
  // to return to the bottom and look for any overlaps with the old low and
  // the new high until there are no more overlaps.
  protected Vector getNextOverlap(Vector typeTiers, int typeTiersSize,
                                  int [] startInds, Vector [] typeTiersFeatures,
                                  int [] typeTiersSizes, Vector overlapEnds) {
    Vector overlapFeatures = null;
    boolean hadOverlap = true;
    Vector features;
    boolean first = true;

    int high = -1;

    while (hadOverlap) {
      hadOverlap = false;
      for (int i=0;i<typeTiersSize;i++) {
	features = typeTiersFeatures[i];
        if (startInds[i] < typeTiersSizes[i]) {
          Drawable sf = (Drawable)features.elementAt(startInds[i]);
          if (first) {
	    high = sf.getHigh();
            first = false;
            overlapFeatures = new Vector(32);
          }
          if (sf.getLow() <= high) {
            hadOverlap = true;
            overlapFeatures.addElement(sf);
            startInds[i]++;
            int sfHigh = sf.getHigh();
            if (sfHigh > high) {
	      high = sfHigh;
            }
          }
        }
      }
    }
    if (high > -1) {
      overlapEnds.addElement(new Integer(high));
    }
    return overlapFeatures;
  }

  // Optimisation - use STATIC variables for temporary arrays
  static int                  maxOverlapping = 100;
  static Drawable[] feats  = new DrawableSeqFeature[maxOverlapping];
  static double []            scores = new double[maxOverlapping];

  protected void sortOverlap(Vector newTiers, Vector overlap) {
    int nTier = newTiers.size();

    int nOverlapping = overlap.size();

    if (nOverlapping > maxOverlapping) {
      maxOverlapping = nOverlapping;
      feats  = new DrawableSeqFeature[maxOverlapping];
      scores = new double[maxOverlapping];
    }

    overlap.copyInto(feats);

    for (int i=0;i<nOverlapping; i++) {
      scores[i] = feats[i].getFeature().getScore();
    }

    QuickSort.sort(scores,feats,nOverlapping);

    // Reverse to sort descending
    int nOverlappingDiv2 = nOverlapping/2;
    for (int i=0;i<nOverlappingDiv2; i++) {
      Drawable tmp = feats[i];
      feats[i] = feats[nOverlapping-i-1];
      feats[nOverlapping-i-1] = tmp;
    }

    for (int i=nTier; i<nOverlapping;i++) {
      newTiers.addElement(new FeatureTier());
    }
    for (int i=0; i<nOverlapping;i++) {
      ((FeatureTier)newTiers.elementAt(i)).addFeature(feats[i], transformer);
    }
  }

  protected Transformer textTransform = null;
  protected FontMetrics fm            = null;
  protected boolean     textAvoidance = false;

  public void setTextAvoidance(Transformer trans, Graphics g) {
    this.fm       = g.getFontMetrics();
    textTransform = trans;
    textAvoidance = true;
    doLayoutTiers();
  }

  public void unsetTextAvoidance() {
    textAvoidance = false;
    doLayoutTiers();
  }

  public boolean isAvoidingTextOverlaps() {
    return textAvoidance;
  }

  public int getTextStart(Drawable dsf) {
    int start = textTransform.toPixelX(dsf.getLeft(textTransform));
    return start;
  }

  // Work out string length - gives end position
  public int getTextEnd(Drawable dsf,int start) {
    int end = start;
    String name = dsf.getDisplayLabel();
    if (name != null) {
      end += fm.stringWidth(name);
    }
    return end;
  }

  // They are already sorted SO we must not change the relative
  // order of features in the tiers during the overlap removal.
  //
  // Therefore we create a new set of tiers and add the features from each
  // of the original overlaps using _addWithAvoidance
  //
  protected Vector removeTextOverlapsFromSorted(Vector sortedTiers,
						TierProperty tp,
						int [] overlapEnds) {

    Vector newTiers = new Vector();
    newTiers.addElement(new FeatureTier());

    int sortedTiersSize = sortedTiers.size();
    int [] tierInds  = new int [sortedTiersSize];
    int [] tierSizes = new int [sortedTiersSize];
    for (int i=0;i<sortedTiersSize;i++) {
      tierSizes[i] = ((FeatureTier)sortedTiers.elementAt(i)).size();
    }
    for (int i=0; i<overlapEnds.length; i++) {
      Vector overlapFeatures = new Vector();
      for (int j=0; j<sortedTiersSize; j++) {
        FeatureTier origTier = (FeatureTier)sortedTiers.elementAt(j);
        if (tierInds[j] < tierSizes[j]) {
          Drawable dsf = origTier.getDrawableAt(tierInds[j]);
          if (dsf.getHigh() > overlapEnds[i]) {
            break;
          } else {
            overlapFeatures.addElement(dsf);
            tierInds[j]++;
          }
        }
      }
      int overlapFeaturesSize = overlapFeatures.size();
      int minTier = 0;
      for (int j=0; j<overlapFeaturesSize; j++) {
	minTier = _addWithAvoidance((Drawable)overlapFeatures.elementAt(j),
				    minTier, newTiers);
	if (++minTier == newTiers.size()) {
	  newTiers.addElement(new FeatureTier());
	}
      }
    }
    return newTiers;
  }

  /**
     This still doesn't really work. It can detect overlaps
     in sequence space, but is not detecting overlaps in
     draw space. This means non-overlapping features on the
     sequence, that do overlap when drawn are still colliding */
  protected boolean drawableOverlaps(FeatureTier tier, Drawable dsf) {
    //    int drawable_low = transformer.toPixelX(dsf.getLow() - 1);
    boolean overlaps;
    overlaps = tier.getHigh() >= dsf.getLow();
    /*
    overlaps |= ((drawable_low > 0) &&
                 (tier.getPixelHigh() >= drawable_low));
    if ((drawable_low <= 0 || tier.getPixelHigh() <= 0))
      System.out.println (dsf.getName() + " Low=" + drawable_low +
                          " tier high=" + tier.getPixelHigh() +
                          " overlaps=" + overlaps);
    */
    return overlaps;
  }

  protected int _addWithAvoidance(Drawable dsf,int tierInd,
				  Vector newTiers) {
    FeatureTier tier     = (FeatureTier)(newTiers.elementAt(tierInd));
    //    int        dsfLow   = dsf.getLow();
    int         textStart   = 0;
    int         textEnd     = 0;
    int         tierTextEnd = 0;
    
    /* Any feature already added will have a lower Low than dsf (because they
       are added in Low order). Therefore if and only if the High of the 
       existing FeatureSet in the Tier is higher than or equal to the Low of
       the new Feature there must be an overlap. */
    
    //overlaps = tier.getHigh() >= dsfLow;
    boolean overlaps = drawableOverlaps(tier, dsf);
    
    if (textAvoidance) {
      // System.out.println("Doing text avoidance");
      textStart = getTextStart(dsf);
      textEnd   = getTextEnd(dsf,textStart);
      if (textTransform.getXOrientation() == Transformer.LEFT) {
	if (tier.size() != 0 && tier.getTextEnd() >= textStart) {
	  overlaps = true;
	}
	tierTextEnd = textEnd;
      } else {
	if (tier.size() != 0 && tier.getTextEnd() <= textEnd) {
	  overlaps = true;
	}
	tierTextEnd = textStart;
      }
    }
    //If overlaps
    if (overlaps) {
      tierInd++;
      //  if next new tier
      if (tierInd < newTiers.size()) {
	//    recurse (next new tier)
	tierInd = _addWithAvoidance(dsf,tierInd,newTiers);
      } else {
	//    add new tier
	FeatureTier t = new FeatureTier();
	newTiers.addElement(t);
	//    add feature
	t.addFeature(dsf, transformer);
	if (textAvoidance) {
	  t.setTextEnd(tierTextEnd);
	}
	// dsf keeps the tier state - since its a new tier its tierInd+1
	dsf.setTierIndex(tierInd+1);
      }
    } else {
      //  add to tier
      tier.addFeature(dsf, transformer);
      if (textAvoidance) {
	tier.setTextEnd(tierTextEnd);
      }
      // dsf keeps the tier state - used for scrolling
      dsf.setTierIndex(tierInd);
    }
    //endif
    return tierInd;
  }

  /** Tier movement is done by reordering the types array.
   */
  public void moveTier(int from, int to) {
    // Obtain an array of currently visible types
    Vector tierTypes = new Vector();

    for (int j = 0; j < tiers.size(); j++) {
      FeatureTier     ft = (FeatureTier)tiers.elementAt(j);
      FeatureProperty fp = ft.getFeatureProperty();
      tierTypes.addElement(fp.getTier());
    }

    if (from >= tierTypes.size()) {
      System.out.println("ERROR: Invalid Tier Index: " + from);
    }

    // Find type of tier to move
    TierProperty fromFP = (TierProperty)tierTypes.elementAt(from);
    
    if (to >= tier_properties.size()) {
      System.out.println("ERROR: Invalid Tier Index: " + to);
    }
    TierProperty toFP = (TierProperty)tierTypes.elementAt(to);

    // Change order of types array
    int index;
    if ((index = tier_properties.indexOf(fromFP)) >= 0) {
      tier_properties.removeElementAt(index);
    } else {
      System.out.println("ERROR: Failed looking for from index in order");
    }

    if ((index = tier_properties.indexOf(toFP)) >= 0) {
      tier_properties.insertElementAt(fromFP,index);
    } else {
      System.out.println("ERROR: Failed looking for to index in order");
    }

    // layout
    doLayoutTiers();
  }

  /** For features that are instantiated as drawables the easiest
      way to find out if the view contains these is by rolling
      through the tiers and seeing if the selected item is present.
      It would be much nicer if sometime in the future we could
      register items so that for any given feature we could more
      quickly obtain the list of all views that are currently presenting
      this data. */
  public Selection getViewSelection(Selection selection) {
    Selection view_selection = new Selection();
    // Avoid all the work if there is nothing to look for
    if (selection.size() > 0) {
      int tier_count = tiers.size();
      for (int i = 0; i < tier_count; i++) {
        Vector drawables = ((FeatureTier)tiers.elementAt(i)).getDrawables();
        int featSize = drawables.size();
        for (int j = 0; j < featSize; j++) {
          Drawable dsf = (Drawable) drawables.elementAt(j);
          Selection sfSel
            = selection.getSelectionDescendedFromModel(dsf.getFeature(), true);
          if (selection.size() > 0) {
            view_selection.add(sfSel);
          }
        }
      }
      //      System.out.println ("Selection has " + selection.size() + " items " +
      //                          " found " + view_selection.size());
    }
    return view_selection;
  }

  /** The next few methods are used by TierPopupMenu and AnnotationMenu */
  public void collapseTier(FeatureView fv, ApolloPanelI ap) {
    Vector tier_labels = findTiersForTypes(fv, ap);
    for (int i=0; i < tier_labels.size(); i++)
      collapseTier((String)tier_labels.elementAt(i));
  }

  public void expandTier(FeatureView fv, ApolloPanelI ap) {
    Vector tier_labels = findTiersForTypes(fv, ap);
    for (int i=0; i < tier_labels.size(); i++)
      expandTier((String)tier_labels.elementAt(i));
  }

  public void showLabelTier(FeatureView fv, ApolloPanelI ap) {
    Vector tier_labels = findTiersForTypes(fv, ap);
    for (int i=0; i < tier_labels.size(); i++)
      showLabelTier((String)tier_labels.elementAt(i));
  }  

  public void hideLabelTier(FeatureView fv, ApolloPanelI ap) {
    Vector tier_labels = findTiersForTypes(fv, ap);
    for (int i=0; i < tier_labels.size(); i++)
      hideLabelTier((String)tier_labels.elementAt(i));
  } 
  
  public void hideTier(FeatureView fv, ApolloPanelI ap) {
    Vector tier_labels = findTiersForTypes(fv, ap);
    for (int i=0; i < tier_labels.size(); i++)
      setVisible((String)tier_labels.elementAt(i), false);
  }

  /** Pop up the color chooser for the selected feature type */
  public void changeTypeColor(FeatureView view, Selection f) {
    //    FeatureSetI top = view.getTopModel();

    // Only use the LAST feature
    SeqFeatureI sf = f.getSelectedData(f.size()-1);
    FeatureProperty fp
      = Config.getPropertyScheme().getFeatureProperty(sf.getTopLevelType());

    if (Config.getStyle().internalMode() && Config.doUserTranscriptColouring() &&
        fp.getDisplayType().equalsIgnoreCase("gene")) {
      String message = "Since you have UserTranscriptColoring turned on, genes are colored by owner,\nso changing the (default) gene color won't have any visible effect.";
      JOptionPane.showMessageDialog(null,message,"Warning",JOptionPane.WARNING_MESSAGE);
    }

    Color color = JColorChooser.showDialog(null,
                                           ("Choose color for "+ 
                                            fp.getDisplayType()+" features"),
                                           fp.getColour());
    if (color != null) {
      fp.setColour(color);
      String message = "Color changed for " + fp.getDisplayType() + "--to save this change, use 'Save type preferences' in the File menu.";
      // If they have confirmOverwrite set to true (or, rather, not set explicitly
      // to false), then pop up a message about saving the color change (because
      // they are probably not an "expert" user).  Will this get too annoying?
      if (Config.getConfirmOverwrite())
        JOptionPane.showMessageDialog(null,message,"Reminder",JOptionPane.WARNING_MESSAGE);
      else
        System.out.println(message);
    }
  }

  /** Pop up the settings tab dialog for the selected feature type */
  public void editTypeSettings(FeatureView view, Selection f) {

    // Only use the LAST feature
    SeqFeatureI sf = f.getSelectedData(f.size()-1);
    FeatureProperty fp
      = Config.getPropertyScheme().getFeatureProperty(sf.getTopLevelType());

    FeaturePropertyPanel fpp = new FeaturePropertyPanel(fp,(ApolloPanel)view.getComponent()); 
    fpp.getFrame().setVisible(true);
  }

  /** Returns a Vector of Strings which are the labels of the TierProperties
      of the current selection. Put in Selection? */
  public Vector findTiersForTypes(FeatureView view, ApolloPanelI ap) {
    Vector    tier_labels = new Vector();
    FeatureSetI top = view.getTopModel();
    Selection f = ap.getSelection().getSelectionDescendedFromModel(top,  false);

    PropertyScheme ps = Config.getPropertyScheme();
    for (int i=0; i < f.size(); i++) {
      SeqFeatureI sf = f.getSelectedData(i);
      String tier_label = ps.getTierProperty(sf.getFeatureType()).getLabel();

      if (!(tier_labels.contains(tier_label))) {
        tier_labels.addElement(tier_label);
      }
    }

    return tier_labels;
  }


  /** This is actually for testing - doesnt otherwise need to be exposed to outside
      world */
  public Vector getTierProperties() { return tier_properties; }

}
