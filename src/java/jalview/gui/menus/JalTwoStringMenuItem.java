package jalview.gui.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


/**
 * A MenuItem which can take a JalTwoStringToggleAction for construction.
 * The menu item is actually two MenuItems which are switched in a toggle
 * like way. This class is a hack to get round the bugs in some JVMs which
 * do not allow MenuItem labels to be changed.
 */
public class JalTwoStringMenuItem extends JMenuItem implements ActionEventSourceI,
                                                              ActionListener,
                                                              ItemListener,
                                                              ItemSelectable,
                                                              JalActionListener,
                                                              ToggleI {
  JalAction   action = null;
  JalMenu     menu;
  JMenuItem [] items = new JMenuItem[2];
  int         position;
  boolean     state;
  transient ItemListener itemListener;

  public JalTwoStringMenuItem(JalTwoStringToggleAction action) {
    items[0] = new JMenuItem(action.getName());
    items[1] = new JMenuItem(action.getAltName());

    items[0].addActionListener(this);
    items[1].addActionListener(this);


    setAction(action);
  }

  public Object [] getSelectedObjects() {
    
    Object [] sel = new Object[1];
    sel[0] = this;
    return sel;
  }

  /**
   * Intercept the addition and redirect so that one of the JMenuItems in
   * the items array is added.
   */
  public void addNotify() {
    super.addNotify();
    if (!(getParent() instanceof JalMenu)) {
      if (jalview.gui.Config.DEBUG)
	System.err.println("ERROR: JalTwoStringMenuItems should only be added to JalMenus"); 
    } else {
      menu = (JalMenu)getParent();
      position = menu.getItemPosition(this);
      menu.remove(this);
      //System.out.println("Inserting item0");
      menu.insert(items[0],position);
    }
  }

  protected void setAction(JalAction action) {
    this.action = action;
    action.listenTo(this);
    action.addListener(this);
    if (action instanceof JalToggleAction) {
      setState(((ToggleI)action).getState());
      addItemListener((JalToggleAction)action);
// NOTE Order is important
      addItemListener(this);
    }
  }

  public synchronized void removeItemListener(ItemListener l) {
    if (l == null) {
      return;
    }
    itemListener = AWTEventMulticaster.remove(itemListener, l);
    //System.out.println("removed item listener " + l);
  }

  public synchronized void addItemListener(ItemListener l) {
    if (l == null) {
      return;
    }
    itemListener = AWTEventMulticaster.add(itemListener, l);
    //System.out.println("added item listener " + l);
    //System.out.println("itemListener  = " +  itemListener);
  }

  public void handleJalAction(JalActionEvent evt) {
    // System.out.println("Calling handleJalAction");
    if (!action.isEnabled()) {
      disable();
    } else {
      enable();
    }
    if (action instanceof ToggleI) {
      if (((ToggleI)action).getState() != getState()) {
        setState(((ToggleI)action).getState());
      }
    }
  }
  
  public void actionPerformed(ActionEvent evt) {
    //System.out.println("Called actionPerformed for " + this + " from " + evt.getSource());
    int stateChange = (!state) ? ItemEvent.SELECTED : ItemEvent.DESELECTED;
    ItemEvent event = new ItemEvent(this,ItemEvent.ITEM_STATE_CHANGED,items[0].getLabel(),stateChange);
    itemListener.itemStateChanged(event);
  }

  public void itemStateChanged(ItemEvent evt) {
    //System.out.println("Called itemStateChanged for " + this + " from " + evt.getSource());
// Should be posted but can't be in 1.1 applet
    //processActionEvent(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"JalPress"));
    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"JalPress"));

  }
  
  public void setStateQuietly(boolean state) {
    //System.out.println("Called setStateQuietly for " + this);
    setState(state);
  }

  public void setState(boolean state) {
    //System.out.println("Called setState for " + this);
    if (state != this.state) {
      this.state = state;
      if (state) {
        menu.remove(items[0]);
        menu.insert(items[1],position);
      } else {
        menu.remove(items[1]);
        menu.insert(items[0],position);
      }
    }
  }

  public boolean getState() {
    return state;
  }

  public void setGroup(JalToggleGroup group) {
    System.err.println("ERROR: JalTwoStringMenuItems cannot be added to a group");
  }

  public JalToggleGroup getGroup() {
    return null;
  }

}
