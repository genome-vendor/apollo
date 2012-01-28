package apollo.gui;

import java.util.*;
import apollo.gui.genomemap.ViewI;
import apollo.gui.genomemap.FeatureTier;  // DEL
import apollo.gui.event.*;
import apollo.util.*;

import org.apache.log4j.*;

/**
 * The base abstract TierManager class to organise a set of data into several 
 * tiers.                
 * Uses longs for tier numbers, this could probably be changed to ints(?)
 */
public abstract class TierManager implements 
  TierManagerI {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TierManager.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** Vector of Tiers (FeatureTiers for FeatureTierManager)
   * This is not set in this class, a subclass has to set this and there
   * is no set method(perhaps there should be) only way to set is to use
   * the variable itself. */
  protected Vector     tiers             = new Vector();
  protected int []     visibleLimits     = new int [2];

  protected int        viewHeight        = -1;
  protected int        offsetPixelHeight =  0;
  protected int        aggregateSizeChange = 0;

  protected int        charHeight = 17;

  protected ViewI      view;
  protected Controller controller;
  protected Vector     tierManagerListeners = new Vector();

  private boolean ignoreScoreThresholds = false;

  public TierManager() {}

  public void doLayoutTiers() {
    // puts features into tiers
    fillTiers();

    /** Go through the tiers array and calls tier.setUp 
     * Add in size change (default 0), set char height if labeled
     * Size change, space for labels
     */
    int tiersSize = tiers.size();
    for (int i=0; i < tiersSize; i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      /* Make annotations taller
         (This doesn't work--
         tiers get reinitialized elsewhere if they end up being
         redrawn, and then they forget to add extra height.) */
      tier.setup(aggregateSizeChange, charHeight);
    }

    // I added this in because i was doing a verticalScrollToSelection after a 
    // zoom and without this here the placement of tiers doesnt happen til
    // draw time and the vertical scroll fails. But it leads to the question
    // is there any reason the tiers should not get placement during layout?
    // In other words is there a reason this isnt here?
    updateUserCoordBoundaries();
    fireTierManagerEvent(TierManagerEvent.LAYOUT_CHANGED);
  }

  public void setCharHeight(int height) {
    charHeight = height;
    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      tier.setCharHeight(charHeight);
    }
  }

  public void updateUserCoordBoundaries() {

    int tiersSize = tiers.size();

    int lowBound = 0;
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      tier.updateUserCoordBoundaries(lowBound);
      lowBound += (int)(tier.getTotalSpace() * Y_PIXELS_PER_FEATURE);
    }
  }

  public abstract void fillTiers();

  public abstract void setTierData(Object data);

  public Vector getTiers() {
    return tiers;
  }

  public Tier getTier(int i) {
    // !! This prevents exception from being thrown when we ask 
    // for a too-high tier, but is really not the right thing to do. 
    // Need to fix vertical scrolling!
    if (i >= tiers.size()) {
      logger.debug ("TierManager.getTier: was asked for tier at index " + i + 
                    ", but tier count is only " + tiers.size() +
                    ". Will use max visible index of " +
                    (getMaxVisibleTierNumber() - 1));
      /* Added -1, because what getMaxVisibleTierNumber returns
         is not an index (0 based), but is a number count (1-based)
      */
      return (Tier)tiers.elementAt(getMaxVisibleTierNumber() - 1);
    }
    else
      return (Tier)tiers.elementAt(i);
  }

  public int getNumTiers() {
    return tiers.size();
  }

  public int getLowestVisible() {
    return (int)visibleLimits[0];
  }

  public void setLowestVisible(int lowest) {
    visibleLimits[0] = lowest;
    visibleLimits[1] = lowest + getNumVisible();
  }

  public int getNumVisible() {
    int availablePixels =  viewHeight - offsetPixelHeight;
    int numVisible = 0;
    int nTier = tiers.size();
    logger.debug ("TierManager: viewHeight=" + viewHeight +
                  " offsetPixelHeight=" + offsetPixelHeight +
                  " nTiers=" + nTier +
                  " visibleLimits[0]=" + visibleLimits[0]);
    for (int i = visibleLimits[0]; i < nTier && availablePixels > 0; i++) {
      availablePixels -= ((Tier)tiers.elementAt(i)).getTotalSpace();
      logger.debug ("TierManager: totalSpace=" + 
                    ((Tier)tiers.elementAt(i)).getTotalSpace() +
                    " down to " + availablePixels + " pixels");
      numVisible++;
    }

    return numVisible;
  }
  /**
   * Returns the maximum visible tier number - 
   * The highest tier number can be greater than the number of tiers. 
   * This is handy for tracking visible tiers (eg masking optimization)
   */
  public int getMaxVisibleTierNumber() {
    // Sometimes this sums to more than the total # of tiers, resulting in
    // an exception in the caller, so don't return 
    // a # bigger than total # of tiers
    int n = getLowestVisible() + getNumVisible();
    if (n > getTiers().size()) {
      logger.error("getMaxVisibleTierNumber: lowest visible tier: " + 
                   getLowestVisible() + " Number Visible: " + getNumVisible() +
                   ", is more than total number of tiers " + 
                   getTiers().size());
      return getTiers().size();
    }
    else
      return n;
  }

  public int getMaximumVisibleTransformCoord(int min) {
    int availablePixels =  viewHeight - offsetPixelHeight;
    int coord = min;
    coord += availablePixels * Y_PIXELS_PER_FEATURE;

    return coord;
  }

  public int getMinimumVisibleTransformCoord() {
    int coord = 0;
    for (int i=0; i<visibleLimits[0] && i < tiers.size(); i++) {
      coord 
        += ((Tier)tiers.elementAt(i)).getTotalSpace() * Y_PIXELS_PER_FEATURE;
    }

    return coord;
  }

  public void incrementTierHeight() {
    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      tier.setDrawSpace(tier.getDrawSpace()+1);
    }
    aggregateSizeChange++;
  }

  public void decrementTierHeight() {
    // Use the static that was defined for this, DON'T hardcode it
    if (getMinimumDrawSpace() == Tier.MINHEIGHT) {
      logger.info("Minimum tier size reached");
      // TODO - factor this out, along with other "beep" methods
      System.out.println("\007");
    } else {
      int tiersSize = tiers.size();
      for (int i=0;i<tiersSize;i++) {
        Tier tier = (Tier)tiers.elementAt(i);
        tier.setDrawSpace(tier.getDrawSpace()-1);
      }
      aggregateSizeChange--;
    }
  }

  public void setAggregateSizeChange(int change) {
    aggregateSizeChange = change;
  }

  public int getAggregateSizeChange() {
    return aggregateSizeChange;
  }

  private int getMinimumDrawSpace() {
    int minSize = 1000000;
    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      if (tier.getDrawSpace() < minSize) {
        minSize = tier.getDrawSpace();
      }
    }
    return minSize;
  }

  /**
   * Offset in pixels before first tier
   */
  public void setOffsetHeight(int height) {
    offsetPixelHeight = height;
  }

  /**
   * Height in pixels of area to display tiers in
   */
  public void setViewHeight(int height) {
    viewHeight = height;
  }

  public int getViewHeight() {
    logger.info ("TierManager: viewHeight=" + viewHeight +
                 " offsetPixelHeight=" + offsetPixelHeight);
    return viewHeight - offsetPixelHeight;
  }

  /**
   * Convert from user coords to tier number
   */
  public long toTier(long userCoord) {
    long lastEnd = 0;
    long thisEnd;
    long tierNum;

    if (userCoord >= 0) {
      int tiersSize = tiers.size();
      tierNum = tiersSize;
      for (int i=0;i<tiersSize;i++) {
        Tier tier = (Tier)tiers.elementAt(i);
        thisEnd = tier.getTotalSpace() * Y_PIXELS_PER_FEATURE + lastEnd;

        if (userCoord >= lastEnd && userCoord < thisEnd) {
          tierNum = i;
          break;
        }
        lastEnd = thisEnd;
      }
    } else {
      tierNum = -1;
    }
    return tierNum;
  }

  public int getTotalHeight() {
    int height = 0;

    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      height += tier.getTotalSpace();
    }
    return height;
  }

  public long getMaxTierUserHeight() {

    int maxHeight = 0;

    int tiersSize = tiers.size();
    for (int i=0;i<tiersSize;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      if (tier.getTotalSpace() > maxHeight) {
        maxHeight = tier.getTotalSpace();
      }
    }
    return (long)maxHeight*Y_PIXELS_PER_FEATURE;
  }

  public long getMaxUserCoord() {
    return (long)getTotalHeight()*Y_PIXELS_PER_FEATURE;
  }

  public long getVisibleUserCoord() {
    return ((long)(viewHeight-offsetPixelHeight))*Y_PIXELS_PER_FEATURE;
  }

  public int toUser(int tierNum) {

    int tiersSize = tiers.size();

    if (tierNum > tiersSize) {
      logger.error("Asked for toUser on tier greater than tiersSize");
      return 0;
    }

    int userCoord = 0;
    for (int i=0;i<tierNum;i++) {
      Tier tier = (Tier)tiers.elementAt(i);
      userCoord += tier.getTotalSpace() * Y_PIXELS_PER_FEATURE;
    }
    return userCoord;
  }

  // Event routines
  public void addTierManagerListener(TierManagerListener l) {
    tierManagerListeners.addElement(l);
  }

  public void fireTierManagerEvent(TierManagerEvent evt) {
    for (int i=0;i<tierManagerListeners.size();i++) {
      TierManagerListener l 
        = (TierManagerListener)tierManagerListeners.elementAt(i);
      l.handleTierManagerEvent(evt);
    }
  }

  public void fireTierManagerEvent(int type) {
    TierManagerEvent evt = new TierManagerEvent(this, this, type);
    fireTierManagerEvent(evt);
  }

  public void setController(Controller c) {
    controller = c;
  }

  public Controller getController() {
    return controller;
  }

  /** Even though TierManagers are not gui they are gui-associated with its view.
      Thus when the ancestor window of the view (ApolloFrame) closes, TierManager
      gets removed as listener to its controller. Presently this is irrelevant because
      if ApolloFrame is closing apollo exits, so removing listener doesnt matter */
  public Object getControllerWindow() {
    if (getView() != null) {
      return (SwingMissingUtil.getWindowAncestor(getView().getComponent()));
    } else {
      return null;
    }
  }

  /** Remove as listener from controller when window closes. */
  public boolean needsAutoRemoval() {
    return true;
  }

  public void setView(ViewI v) {
    view = v;
  }

  public ViewI getView() {
    return view;
  }

  public int [] getYRange() {
    int min = getMinimumVisibleTransformCoord();
    int max = getMaximumVisibleTransformCoord(min);
    return (new int [] {min,max});
  }

  /** If ignore is true, will not consider score thresholds in populating the tier.
      This is needed for drag view where it is possible to drag a feature that has
      a score under threshold, but since it has siblings(in feat set) above threshold 
      it gets displayed. Since only items that have somehow gotten through the 
      threshold (either by self score or siblings) can be displayed and thus dragged,
      theres no need to do the score threshold again anyways. */
  public void setIgnoreScoreThresholds(boolean ignore) {
    ignoreScoreThresholds = ignore;
  }

  public boolean ignoreScoreThresholds() { 
    return ignoreScoreThresholds;
  }

  public String getTierLabel(int tierNum) {
    if (tierNum >= 0  && tierNum < tiers.size()) {
      Tier tier = (Tier)tiers.elementAt(tierNum);
      return tier.getTierLabel();
    } else {
      if (tierNum != tiers.size())
        logger.error("ERROR: Invalid tier in getTierLabel()");
      return null;
    }
  }

}
