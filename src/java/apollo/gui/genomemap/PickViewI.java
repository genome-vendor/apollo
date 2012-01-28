package apollo.gui.genomemap;

import java.util.Vector;
import java.awt.Point;
import java.awt.Rectangle;

import apollo.gui.Selection;

/**
 * An interface representing a View object which contains
 * selectable Drawables.
 */

public interface PickViewI extends ViewI {

  /**
   * Find features for selection. Returns selection with SelectionItems that have 
   * model as data and drawables as listeners. This replaces findFeatures that only 
   * returned model, the problem there being we then would hafta refind the drawables
   * later on which was awkward and inefficient.
   * If selectParents is true then return the parents of the features under the point.
   */
  public Selection findFeaturesForSelection(Point p, boolean selectParents);
  /** Same as above for rectangle, no selectParents needed */
  public Selection findFeaturesForSelection(Rectangle rect);

}

