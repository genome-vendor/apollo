package jalview.gui.popups;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;

import javax.swing.*;

public class URLPopup extends Popup {
  JTextField tf;
  JLabel tfLabel;
  JLabel format;
  JComboBox f;

  public URLPopup(JFrame parent,AlignViewport av,Controller c,String title) {
    super(parent,av,c,title);

    tf = new JTextField(40);
    tfLabel = new JLabel("URL address : ");

    format = new JLabel("Alignment format");
    f = new JComboBox();
    for (int i = 0; i < FormatProperties.getFormats().size(); i++) {
      f.addItem((String)FormatProperties.getFormats().elementAt(i));
    }

    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(20,20,20,20);

    add(tfLabel,gb,gbc,0,0,1,1);
    add(tf,gb,gbc,1,0,4,1);
    add(format,gb,gbc,0,1,1,1);
    add(f,gb,gbc,1,1,1,1);

    add(status,gb,gbc,0,2,1,1);

    add(apply,gb,gbc,0,3,1,1);
    add(close,gb,gbc,1,3,1,1);
    pack();
    show();
  }

  public class URLAction extends JalPopupAction {
    public URLAction(String name,AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("format");
    }

    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("format",(String)f.getSelectedItem());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("format")) {
        f.setSelectedItem(getArg("format"));
      }
    } 

    public void applyAction(ActionEvent evt) {
      String urlStr = tf.getText();
      AlignSequenceI[] s = null;
  
      if (FormatProperties.contains(getArg("format"))) {
        s = FormatAdapter.read(getArg("format"),urlStr);
      } else {
        fireStatusEvent("Format not supported",StatusEvent.ERROR);
      }
  
      if (s != null) {
        AlignFrame af = new AlignFrame(parent.getParent(),new Alignment(s));
        af.resize(700,500);
        af.show();
  
  //      if (parent instanceof AlignFrame) {
  //        AlignFrame af2 = (AlignFrame)parent;
  //        Font f = af2.getAlignFont();
  //        af.setAlignFont(f.getName(),f.getStyle(),f.getSize());
  //      }
  //      af.resize(700,500);
  //      af.show();
  //      ConsThread ct = new ConsThread(af);
  //      ct.start();
        fireStatusEvent("done");
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
  
      } else {
        fireStatusEvent("Can't open URL or wrong format",StatusEvent.ERROR);
      }
    }
  }
}
