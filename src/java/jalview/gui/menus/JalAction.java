package jalview.gui.menus;

import jalview.gui.popups.Popup;
import jalview.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import jalview.gui.*;

/**
 * An action handling class a bit like the SWING Action
 * class, but for use with the Jalview AWT menu/popup
 * system.
 */
public abstract class JalAction extends ListenList implements ActionListener,
                                                              JalActionListener {
  String  name;
  String  shortDescription;
  String  longDescription;
  boolean enabled = true;
  Hashtable args;
  Vector requiredArgs;

  Vector enablers;
  Vector disablers;

  public JalAction(String name) {
    this.name    = name;

    listeners    = new Vector();
    requiredArgs = new Vector();
    args         = new Hashtable();
  }

  public void addRequiredArg(String argName) {
    requiredArgs.addElement(argName);
  }

  public void listenTo(ActionEventSourceI source) {
    source.addActionListener(this);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getName() {
    return name;
  }

  public void setEnabled(boolean state) {
    enabled = state;
    // System.out.println("Got a setEnabled = " + state + " for " + getName());
    fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.ENABLE));
  }

  public void fireJalActionEvent(JalActionEvent evt) {
    for (int i=listeners.size()-1; i>= 0; i--) {
      if (evt.getSource() != listeners.elementAt(i)) {
        ((JalActionListener)listeners.elementAt(i)).handleJalAction(evt);
      }
    }
  }

  public void putArg(String name, String value) {
    if (!args.containsKey(name)) {
      args.put(name,value);
    } else {
      args.put(name,value);
    }
  }

  public boolean containsArg(String name) {
    return args.containsKey(name);
  }

  public String getArg(String name) {
    if (args.containsKey(name)) {
      return (String)args.get(name);
    } else {
      System.err.println("Tried to getArg for argument " + name + " which does not exist");
      return null;
    }
  }

  public String getArgString() {
    StringBuffer buff = new StringBuffer();

    checkArgs();
    for (Enumeration e = args.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      buff.append(" \"" + key + "\"" + " " + "\"" + getArg(key) + "\"");
    }
    return buff.toString();
  }

  public void clearArgs() {
    args = new Hashtable();
  }

  public String getCommandString() {
    return getName() + " ARGS" + getArgString();
  }

  public void actionPerformed(ActionEvent evt) {
    getArgsFromSourceDispatcher(evt);
    updateActionState(evt);
    fireJalActionEvent(new JalActionEvent(this,JalAction.this,JalActionEvent.DO));
    applyAction(evt);
  }

  public boolean checkArgs() {
    for (Enumeration e = requiredArgs.elements(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      if (!containsArg(key)) {
        System.err.println("Error: Missing required argument " + key + " in " + this);
        return false;
      }
    }
    return true;
  }

  public void getArgsFromSource(JalCommand jc, ActionEvent evt) {
    
    Hashtable newArgs = jc.getArgs();

    clearArgs();

    if (newArgs.size() != 0) {
      for (Enumeration e = newArgs.keys(); e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        putArg(key,(String)newArgs.get(key));
      }
    }
    checkArgs();
  }

  public void getArgsFromSourceDispatcher(ActionEvent evt) {
    if (evt.getSource() instanceof JalCommand) {
      getArgsFromSource((JalCommand)evt.getSource(),evt);
    } else if (evt.getSource() instanceof Button) {
      getArgsFromSource((Button)evt.getSource(),evt);
    } else if (evt.getSource() instanceof JalCheckBoxMenuItem) {
      getArgsFromSource((JalCheckBoxMenuItem)evt.getSource(),evt);
    } else if (evt.getSource() instanceof JalTwoStringMenuItem) {
      getArgsFromSource((JalTwoStringMenuItem)evt.getSource(),evt);
    } else if (evt.getSource() instanceof JalToggleAction) {
      getArgsFromSource((JalToggleAction)evt.getSource(),evt);
    } else {
      getArgsFromSource(evt.getSource(),evt);
    }
  }
  public void getArgsFromSource(Button button, ActionEvent evt) {}
  public void getArgsFromSource(JalCheckBoxMenuItem mi, ActionEvent evt) {}
  public void getArgsFromSource(JalTwoStringMenuItem mi, ActionEvent evt) {}
  public void getArgsFromSource(JalToggleAction ja, ActionEvent evt) {}
  public void getArgsFromSource(Object obj, ActionEvent evt) {
    if (Config.DEBUG) System.out.println("Default getArgsFromSource() called from " + obj);
  }
  public void updateActionState(ActionEvent evt) {}

  public void handleJalAction(JalActionEvent evt) {
    if (evt.getActionType() == JalActionEvent.SELECT) {
      if (enablers!=null && enablers.contains(evt.getSource())) {
        JalToggleAction toggle = (JalToggleAction)evt.getSource();
        if (toggle.getState() || toggle.getGroup() == null)
          setEnabled(toggle.getState());
      } else if (disablers!=null && disablers.contains(evt.getSource())) {
        JalToggleAction toggle = (JalToggleAction)evt.getSource();
        if (toggle.getState() || toggle.getGroup() == null)
          setEnabled(!toggle.getState());
      }
    }
  }

  public void addEnabler(JalToggleAction enabler) {
    if (enablers == null) {
      enablers = new Vector();
    }
    enablers.addElement(enabler);
    enabler.addListener(this);
  }

  public void addDisabler(JalToggleAction disabler) {
    if (disablers == null) {
      disablers = new Vector();
    }
    disablers.addElement(disabler);
    disabler.addListener(this);
  }

  public abstract void applyAction(ActionEvent evt);
}





