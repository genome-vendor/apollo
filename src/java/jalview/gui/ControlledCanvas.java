package jalview.gui;

import jalview.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class ControlledCanvas extends JPanel implements ControlledObjectI,
                                                        EventListener {
  Controller controller;

  public void setController(Controller c) {
    controller = c;
    controller.addListener(this);
  }
 
  public Controller getController() {
    return controller;
  }
 
  public void addNotify() {
    super.addNotify();
    controller.changeWindow(this);
  }
 
  public Object getControllerWindow() {
    return WindowUtil.getWindowAncestor(this);
  }
}
