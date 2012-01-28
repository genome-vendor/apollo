package apollo.gui.genomemap;

import apollo.gui.TierManager;
import apollo.gui.Selection;

/**
 * An interface representing a View object which has defined
 * extents and a number of tiers 
 */

import java.awt.event.*;

public interface TierViewI extends ManagedViewI {

  /** Returns Vector of visible features */
  public java.util.Vector getVisibleDrawables();

  /**
   * Set the lowest visible tier.
   */
  public void setLowestVisibleTier(long tier);

  /**
   * Get the lowest visible tier.
   */
  public int getLowestVisibleTier();

  /**
   * Whether this view allows tier dragging
   */
  public boolean allowsTierDrags();

  /**
   * Start a tier drag
   */
  public boolean beginTierDrag(MouseEvent evt);

  /**
   * continue a tier drag
   */
  public void updateTierDrag(MouseEvent evt);

  /**
   * end a tier drag
   */
  public void endTierDrag(MouseEvent evt);

  public void incrementTierHeight();
  public void decrementTierHeight();

  /**
   * Returns only those selection items that are presented
   * in this view
   */
  public Selection getViewSelection(Selection selection);

  /**
   * Interface of method required for starting a drag from this TierView
   */
  public DragViewI createDragView(MouseEvent evt, Selection selection);

}
