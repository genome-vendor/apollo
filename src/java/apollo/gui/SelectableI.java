package apollo.gui;

import java.awt.*;

/**
 * An interface defining methods necessary to make an object selectable.
 */
public interface SelectableI {

  /**
   * Set whether this Drawable is currently selected or not.
   */
  public void    setSelected(boolean state);
  /**
   * Determine if this Drawable is currently selected.
   */
  public boolean isSelected();
  
  
  //public SelectableI getSelectableParent();

}
