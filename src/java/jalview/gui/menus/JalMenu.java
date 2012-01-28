package jalview.gui.menus;

import jalview.gui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public abstract class JalMenu extends JMenu {

  public JalMenu(String title, boolean tearable) {
    super(title,tearable);
  }

/**
 * Factory method for adding menu items created from actions.
 */
  public JMenuItem add(JalAction action) {
    
    if (action instanceof JalTwoStringToggleAction) {
      return super.add(new JalTwoStringMenuItem((JalTwoStringToggleAction)action));
    } else if (action instanceof ToggleI) {
      return super.add(new JalCheckBoxMenuItem(action));
    } else {
      return super.add(new JalMenuItem(action));
    }
  }

  public int getItemPosition(JMenuItem item) {
    for (int i = 0 ; i < getItemCount(); i++) {
      JMenuItem mi = getItem(i);
      if (mi == item) {
        return i;
      }
    }
    return -1;
  }
}
