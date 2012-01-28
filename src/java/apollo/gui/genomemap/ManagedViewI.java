package apollo.gui.genomemap;

import apollo.gui.TierManagerI;

/**
 * An interface representing a View object which has defined
 * extents eg a start and end base and has a tier manager to 
 * go with it.
 */

public interface ManagedViewI extends ViewI {
  /**
   * Set the tier manager for the view
   */
  public void setTierManager(TierManagerI tm);
  public TierManagerI getTierManager();

}

