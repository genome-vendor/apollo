package jalview.gui.menus;

import java.util.EventObject;


public class JalActionEvent extends EventObject {
  int actionType;
  JalAction action;

  public static final int SELECT = 1;
  public static final int ENABLE = 2;
  public static final int NAME   = 3;
  public static final int DO     = 4;
  public static final int DONE   = 5;
  public static final int STATUS = 6;
  public static final int WARNING = 7;
  public static final int ERROR  = 8;

  public JalActionEvent(Object source, JalAction action, int actionType) {
    super(source);
    this.action = action;
    this.actionType = actionType;
  }

  public int getActionType() {
    return actionType;
  }

  public JalAction getAction() {
    return action;
  }

  public Object getSource() {
    return source;
  }
}
