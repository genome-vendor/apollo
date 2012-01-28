package jalview.gui.popups;

import jalview.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.io.File;

import javax.swing.*;

public class BrowsePopup extends Popup {
  JButton    b;
  JTextField tf;
  JLabel     tfLabel;

  public BrowsePopup(JFrame parent,AlignViewport av,Controller c,String title) {
    super(parent,av,c,title);

    b = new JButton("Browse..");
    b.addActionListener(pal);


    tf = new JTextField(10);
    tfLabel = new JLabel("Filename : ");

  }

  public abstract class BrowseAction extends JalPopupAction {
    public BrowseAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("file");
    }
    public void getArgsFromSource(JButton but, ActionEvent evt) {
      clearArgs();
      putArg("file",tf.getText());
    }
 
    public void updateActionState(ActionEvent evt) {
      if (containsArg("file")) {
        tf.setText(getArg("file"));
      }
    }                    
  }

  protected void otherAction(ActionEvent evt) {
    if (evt.getSource() == b) {
      FileDialog fd = new FileDialog(parent,getBrowseString(),getBrowseMode());
      fd.show();
      String dir = "";
      String file = "";
      if (fd.getDirectory() != null) {
        dir = fd.getDirectory();
      }
      if (fd.getFile() != null) {
        file = fd.getFile();
      }
      tf.setText(dir + file);
    } else {
      System.out.println("Unknown action event source " + evt.getSource() + " in " + this);
    }
  }

  protected String getBrowseString() {
    return "SET THIS IN YOUR CLASS";
  }

  protected int getBrowseMode() {
    return FileDialog.LOAD;
  }
}
