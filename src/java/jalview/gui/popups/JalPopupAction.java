package jalview.gui.popups;

import jalview.gui.menus.*;
import jalview.gui.*;
import jalview.gui.event.*;

public abstract class JalPopupAction extends JalAction {
  AlignViewport av;
  Controller    controller;

  public JalPopupAction(String name, AlignViewport av, Controller c) {
    super(name);
    this.av  = av;
    this.controller = c;
  }

  protected void fireStatusEvent(String text) {
    fireStatusEvent(text,StatusEvent.INFO);
  }
  protected void fireStatusEvent(String text, int type) {
    controller.handleStatusEvent(new StatusEvent(this,text,type));
    fireJalActionEvent(new JalTextActionEvent(this,this,JalActionEvent.STATUS,text));
  }
}
