package jalview.gui.menus;

import java.awt.*;
import javax.swing.*;

/**
 * A MenuItem which can take a JalAction for construction.
 */
public class JalMenuItem extends JMenuItem implements JalActionListener,
                                                     ActionEventSourceI {
  JalAction action = null;

  public JalMenuItem(JalAction action) {
    this(action.getName());
    action.addListener(this);
    this.action = action;
    action.listenTo(this);
  }
  public JalMenuItem(String text) {
    super(text);
  }
  public void handleJalAction(JalActionEvent evt) {
    // System.out.println("Calling handleJalAction");
    if (!action.isEnabled()) {
      disable();
    } else {
      enable();
    }
  }

  public void addNotify() {
    // System.out.println("addNotify called for " + action.getName());
    super.addNotify();
  }
}
