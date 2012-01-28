package jalview.gui.popups;

import jalview.gui.event.*;
import jalview.gui.menus.*;
import jalview.util.*;

import java.awt.*;

import javax.swing.*;

public class PopupStatusLabel extends JLabel implements JalActionListener {
                                                       
  public PopupStatusLabel(String text, int pos) {
    super(text,pos);
  }

  public void handleJalAction(JalActionEvent evt) {
    
    if (evt instanceof JalTextActionEvent) {

      JalTextActionEvent te = (JalTextActionEvent)evt;

      if (te.getActionType() == JalActionEvent.STATUS) {
        setText("Status: " + te.getText());
      } else if (te.getActionType() == StatusEvent.WARNING) {
        setText("Warning: " + te.getText());
      } else if (te.getActionType() == StatusEvent.ERROR) {
        setText("ERROR: " + te.getText());
      } else {
        System.out.println("ERROR: Unknown JalActionEvent type = " + te.getActionType());
      }
    }
  }
}
