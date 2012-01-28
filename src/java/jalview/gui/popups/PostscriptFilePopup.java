package jalview.gui.popups;

import jalview.io.*;
import jalview.gui.*;
import jalview.gui.event.*;
import jalview.gui.menus.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.io.*;

import javax.swing.*;

public class PostscriptFilePopup extends PostscriptPopup {

  public PostscriptFilePopup(JFrame parent, AlignViewport av,Controller c,String title,OutputGenerator og) {
    super(parent,av,c,title,og);
    setApplyAction(new PostscriptFileAction("postscript file",av,c));
  }

  public void createInterface() {

    super.createInterface();

    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    gbc.weightx  = 0.0;

    add(tfLabel,      gb,gbc,0,0,1,1);
    add(fontLabel,    gb,gbc,0,1,1,1);
    add(fontsizeLabel,gb,gbc,0,2,1,1);
    add(orientLabel,  gb,gbc,0,3,1,1);
    add(sizeLabel,    gb,gbc,0,4,1,1);

    gbc.weightx  = 0.5;

    add(tf,           gb,gbc,1,0,2,1);
    add(font,         gb,gbc,1,1,2,1);
    add(fontSize,     gb,gbc,1,2,2,1);
    add(orient,       gb,gbc,1,3,2,1);
    add(size,         gb,gbc,1,4,2,1);

    add(b,gb,gbc,3,0,1,1);
    add(status,gb,gbc,0,4,1,1);

    this.pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    setLocation((screenSize.width - getSize().width) / 2,
		(screenSize.height - getSize().height) / 2);

    this.show();
  }

  public class PostscriptFileAction extends PostscriptAction {
    public PostscriptFileAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
      addRequiredArg("file");
    }

    public void getArgsFromSource(Button but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
      putArg("file",tf.getText());
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
 
      if (containsArg("file")) {
        tf.setText(getArg("file"));
      }
    }

    public void applyAction(ActionEvent evt) {
      String fileStr = getArg("file");
      try {
        PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileStr)));

        fireStatusEvent("Saving file...");
  
        setPostscriptOptions();

        og.getPostscript(ps);
        ps.close();

        try {
          Thread.sleep(500);
        } catch (Exception ex2) {}

        fireStatusEvent("done");

      } catch (IOException ex) {
        fireStatusEvent("Can't open file",StatusEvent.ERROR);

      } finally {
        fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
      }
    }
  }

  protected String getBrowseString() {
    return "Save postscript file";
  }

  protected int getBrowseMode() {
    return FileDialog.SAVE;
  }
}
