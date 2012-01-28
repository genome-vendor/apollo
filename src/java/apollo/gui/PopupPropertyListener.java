package apollo.gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

//
// This class was written to get around a refresh problem with the 
// menus and comboboxes which use a MenuListener to update the 
// state of their items. A symptom of the problem was that tick 
// boxes would have the wrong state until they were moused-over. 
// This definitely occurs in OS X and I believe in other operating 
// systems too. 
// An instance of this class is added as a PropertyChangeListener 
// to the JPopupMenu component of the JMenu or JComboBox to force 
// a repaint of the JPopupMenu when it becomes visible. 
//
public class PopupPropertyListener implements PropertyChangeListener {

  public PopupPropertyListener() {
  }

  public void propertyChange(PropertyChangeEvent e) {
    String name = e.getPropertyName();
 
    // Event should come from the popup
    //
    if (e.getSource() instanceof JPopupMenu) {
      JPopupMenu popup = (JPopupMenu)e.getSource();
      if (name.equals("visible") && popup.isVisible()) {
        popup.invalidate();
        popup.repaint(popup.getBounds());
      }
    }
  }
}
