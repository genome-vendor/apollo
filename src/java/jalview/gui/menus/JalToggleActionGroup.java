package jalview.gui.menus;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

/**
 * This class is like a SWING ButtonGroup in that it controls
 * the state of a set of toggleable entities. However the entities
 * controlled by this class are JalToggleActions, giving the
 * benefits of JalActions (multiple sources, command file control
 * etc.) to the group.
 */
public class JalToggleActionGroup implements JalToggleNotifier {
  JalToggleGroup group = new JalToggleGroup();
  boolean enabled;

  public void add(JalToggleAction item) {
    group.add(item);
    group.addNotifier(this);
  }

  public void setNoSelection() {
    group.setNoSelection();
  } 
  
  public void setDefault(JalToggleAction item) {
    group.setDefault(item);
  }
  public JalToggleAction getCurrent() {
    return (JalToggleAction)group.getCurrent();
  }
    
  /** Fires a JalActionEvent */
  public void signalToggle(ToggleI changed) {
    //System.out.println("Signaling toggle for " + this + " changed = " + changed);
    JalToggleAction jta = (JalToggleAction)changed;
    jta.fireJalActionEvent(new JalActionEvent(changed,jta,JalActionEvent.SELECT));
  }

  public void signalActionToggle(ToggleI changed) {
    //System.out.println("Signaling action toggle for " + this + " changed = " + changed);
    JalToggleAction jta = (JalToggleAction)changed;
    jta.actionPerformed(new ActionEvent(jta,ActionEvent.ACTION_PERFORMED,"JalPress"));
  }

  public void setEnabled(boolean state) {
    if (!state) {
      setNoSelection();
    } else if (!enabled) {
      group.reset();
    }
    for (int i=0; i<group.size(); i++) {
      ((JalToggleAction)group.get(i)).setEnabled(state);
    }
    enabled = state;
  }
}
