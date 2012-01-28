package jalview.gui.menus;

import java.awt.event.*;
import java.util.*;

import java.net.*;
import java.util.*;

public class CommandParser implements JalActionListener {
  Hashtable actions = new Hashtable();

  public CommandParser() {}

  public void add(JalAction action) {
    if (!actions.containsKey(action.getName())) {
      actions.put(action.getName(),action);
      action.addListener(this);
    } else {
      ((JalAction)actions.get(action.getName())).removeListener(this);
      actions.put(action.getName(),action);
      action.addListener(this);
    }
  }
  public void remove(JalAction action) {
    if (actions.containsKey(action.getName())) {
      if (actions.get(action.getName()) == action) {
        actions.remove(action.getName());
        action.removeListener(this);
      } else {
        System.err.println("Error: Tried to remove action which wasn't in CommandParser hash");
      }
    } else {
      System.err.println("Error: Tried to remove action which wasn't in CommandParser hash");
    }
  }

  public void parseCommand(JalCommand cmd) {
    if (actions.containsKey(cmd.getCommandString())) {
      JalAction action = (JalAction)actions.get(cmd.getCommandString());
      action.actionPerformed(new ActionEvent(cmd,
                                             ActionEvent.ACTION_PERFORMED, "", 0 ));
    } else {
      System.err.println("No such command: " + cmd.getCommandString());
    }
  }

  public void runCommands(CommandLog log) {
    for (int i=0; i<log.size(); i++) {
      parseCommand(log.get(i));
    }
  }

  Vector commands = new Vector();

  public void handleJalAction(JalActionEvent evt) {
    if (evt.getActionType() == JalActionEvent.DO) {
      commands.addElement(evt.getAction().getCommandString());
      if (jalview.gui.Config.DEBUG) 
	System.out.println(evt.getAction().getCommandString());
    }
  }
}
