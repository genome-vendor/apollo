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
import java.io.File;

import javax.swing.*;

public class FilePopup extends BrowsePopup {
  JLabel     formatLabel;
  Choice    formatChoice;

  public FilePopup(JFrame parent,AlignViewport av,Controller c,String title) {
    super(parent,av,c,title);

    tfLabel      = new JLabel("Filename");
    formatLabel  = new JLabel("Alignment format");

    formatChoice = new Choice();

    for (int i = 0; i < FormatProperties.getFormats().size(); i++) {
      formatChoice.addItem((String)FormatProperties.getFormats().elementAt(i));
    }

    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.weightx = 0.0;
    add(tfLabel,gb,gbc,0,0,1,1);
    add(formatLabel,gb,gbc,0,1,1,1);

    gbc.weightx = 0.5;
    add(tf,gb,gbc,1,0,2,1);
    add(formatChoice,gb,gbc,1,1,2,1);

    gbc.weightx = 0.0;
    gbc.insets = new Insets(5,5,5,5);
    add(b,gb,gbc,3,0,1,1);

    add(status,gb,gbc,0,2,1,1);

    setApplyAction(new FileInputAction("align input",av,c));

    pack();
    setSize(400,200);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    setLocation((screenSize.width - getSize().width) / 2,
		(screenSize.height - getSize().height) / 2);

    show();
  }

  public class FileInputAction extends BrowseAction {
    public FileInputAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("format");
    }
    public void getArgsFromSource(JButton but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("format",formatChoice.getSelectedItem());
    }

    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);

      if (containsArg("format")) {
        formatChoice.select(getArg("format"));
      }
    }

    public void applyAction(ActionEvent evt) {
      fireStatusEvent("Reading file...");

      String fileStr = getArg("file");
      System.out.println("File " + fileStr);

      File tmp = new File(fileStr);
      if (tmp.isFile()) {

        AlignSequenceI[] s = null;
        s = FormatAdapter.read(fileStr,"File",getArg("format"));
  
        if (s != null) {
          fireStatusEvent("Creating new alignment window...");

          AlignFrame af = new AlignFrame(parent.getParent(),new Alignment(s));
          af.resize(700,500);
          af.show();
          fireStatusEvent("done");
          fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
        } else {
          fireStatusEvent("No sequences found. Check format.",StatusEvent.ERROR);
        }
      } else {
        fireStatusEvent("File not found or wrong format",StatusEvent.ERROR);
      }
    }
  }

  protected String getBrowseString() {
    return "Open alignment file";
  }
}
