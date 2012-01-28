package jalview.gui.popups;

import jalview.gui.*;
import jalview.gui.event.*;
import jalview.util.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public class ErrorPopup extends Popup implements ControlledObjectI, 
                                                 StatusListener {

  public ErrorPopup(JFrame parent, AlignViewport av, Controller c,String title) {
    super(parent,av,c,title);

    setModal(true);
    setController(c);
    
    gbc.fill = GridBagConstraints.NONE; 

    gbc.insets = new Insets(20,20,20,20);
    remove(apply);
    remove(close);
    add(status,gb,gbc,0,1,2,1);

    apply.setText("Ok");
    add(apply,gb,gbc,1,2,1,1);

    pack();
  }

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

  public boolean handleStatusEvent(StatusEvent evt) {
    if (evt.getType() == StatusEvent.ERROR) {
      status.setText("ERROR: " + evt.getText());
      pack();
      setLocation(parent.getLocation().x+parent.getSize().width/2-getSize().width/2,parent.getLocation().y+parent.getSize().height/2-getSize().height/2);
      show();
    }
    return true;
  }
  protected void closeAction(ActionEvent evt) {
    this.hide();
  }
 
}
