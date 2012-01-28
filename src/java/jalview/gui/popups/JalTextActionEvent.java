package jalview.gui.popups;

import jalview.gui.menus.*;

public class JalTextActionEvent extends JalActionEvent {

  String text;

  public JalTextActionEvent(Object source, JalAction action, int actionType, String text) {
    super(source,action,actionType);
    this.text = text;
  }

  public String getText() {
    return text;
  }
}
