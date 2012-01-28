package jalview.gui.menus;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * This class is encapsulated in classes such as JalToggleActionGroup
 * and JalCheckBoxGroup to provide the basic ButtonGroup logic which
 * is:
 * <BR>
 *   Only one ToggleI entity in the group can be selected at any one
 *   time.
 * <BR>
 *   Multiple consecutive selections of the same entity do not switch
 *   its state between true and false but just keep it as true.
 * <BR>
 *   It is possible to set all the ToggleIs to false using setNoSelection -
 *   this is different to a ButtonGroup.
 * <BR>
 */
public class JalToggleGroup {
  /** Vector contains all ToggleIs in group */
  Vector items = new Vector();
  Vector listeners = new Vector();
  ToggleI current = null;
  ToggleI defaultItem = null;
  JalToggleNotifier notifier = null;

  public void add(ToggleI item) {
    // Make first entered the current
    if (items.size() == 0) {
      current = item;
      item.setState(true);
    }
    if (!items.contains(item)) {
      items.addElement(item);
      if (item.getState())
        stateChanged(item);
    }
    item.setGroup(this);
  }

  public void setNoSelection() {
    if (current != null)
      setState(current,false);
    current = null;
  }

  protected void setState(ToggleI item, boolean state) {
    item.setStateQuietly(state);
    if (notifier != null) {
      notifier.signalToggle(item);
    }
  }

  protected void setStateActively(ToggleI item, boolean state) {
    item.setStateQuietly(state);
    if (notifier != null) {
      notifier.signalActionToggle(item);
    }
  }

  public void stateChanged(ToggleI item) {
    
    // System.out.println("Got ToggleGroup item change");
    if (items.contains(item)) {
      if ((current == null || current != item) && item.getState()) {
        if (current != null)
           setState(current,false);
        current = item;
        setState(current,true);
        
      } else if (current == item) {
	// shouldnt this then set all the other states to false
	// to make it like a radio button where only one item can
	// be selected?
	deselectAllItems();
        // System.out.println("Setting state to true");
        setState(current,true);
	
      }
    } else {
      System.err.println("Unknown item in JalToggleGroup");
    }
  }

  private void deselectAllItems() {
    for (int i=0; i<items.size(); i++) {
      ToggleI t = (ToggleI)items.get(i);
      t.setStateQuietly(false);
      // setStart fires a jal action event which should notify the checkbox listener
      // this causes an infinite loop
      //t.setState(false);
      if (t instanceof JalAction) { // i think this is always true
	JalAction ja = (JalAction)t;
	ja.fireJalActionEvent(new JalActionEvent(ja,ja,JalActionEvent.SELECT));
      }
    }
  }

  public void addNotifier(JalToggleNotifier jtn) {
    notifier = jtn;
  }

  public ToggleI getCurrent() {
    return current;
  }

  public void setDefault(ToggleI item) {
    if (items.contains(item)) {
      defaultItem = item;
      // shouldnt current be set here?
      //current = defaultItem;
    } else {
      System.err.println("Unknown item in JalToggleGroup");
    }
  }

  public void reset() {
    setNoSelection();
    if (defaultItem != null) {
      current = defaultItem;
      setStateActively(defaultItem,true);
    } else if (items.size() > 0) {
      setState((ToggleI)items.elementAt(0),true);
      current = (ToggleI)items.elementAt(0);
    }
  }

  public ToggleI get(int i) {
    return (ToggleI)items.elementAt(i);
  }

  public int size() {
    return items.size();
  }
}
