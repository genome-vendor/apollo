package jalview.gui.menus;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * A CheckboxMenuItem which can take a JalAction for construction.
 * It can also be made part of a JalCheckBoxGroup.
 */
public class JalCheckBoxMenuItem extends JCheckBoxMenuItem implements ActionEventSourceI,
                                                                     JalActionListener,
                                                                     ItemListener,
                                                                     ToggleI {
  JalAction action = null;
  JalToggleGroup group = null;

  public JalCheckBoxMenuItem(JalAction action) {
    this(action.getName());
    setAction(action);
  }

  protected void setAction(JalAction action) {
    this.action = action;
    action.listenTo(this);
    action.addListener(this);
    if (action instanceof JalToggleAction) {
      setState(((ToggleI)action).getState());
      addItemListener((JalToggleAction)action);
// Order is important (see note about applet below)
      addItemListener(this);
    }
  }

  public JalCheckBoxMenuItem(String text) {
    super(text);
  }

  public void handleJalAction(JalActionEvent evt) {
    //System.out.println("JalCheckBoxMenuItem got a handleJalAction");
    if (!action.isEnabled()) {
      disable();
    } else {
      enable();
    }
    if (action instanceof ToggleI) {
      if (((ToggleI)action).getState() != getState()) {
	removeItemListener((JalToggleAction)action); // otherwise infinite loop
        setState(((ToggleI)action).getState());
	addItemListener((JalToggleAction)action);
      }
    }
  }
  
  public void itemStateChanged(ItemEvent evt) {
    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"JalPress"));

// Should be a post but can't be because of 1.1 applet security problems
//    processActionEvent(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"JalPress"));
    if (group != null) {
      setStateQuietly(true);
    }
  }
  
  public void setStateQuietly(boolean state) {
    // System.out.println("Called setStateQuietly for " + this);
    super.setState(state);
  }

  public void setState(boolean state) {
    // System.out.println("Called setState for " + this);
    super.setState(state);
    if (group!=null) {
      group.stateChanged(this);
    }
  }

  public void setGroup(JalToggleGroup group) {
    this.group = group;
  }

  public JalToggleGroup getGroup() {
    return group;
  }
}
