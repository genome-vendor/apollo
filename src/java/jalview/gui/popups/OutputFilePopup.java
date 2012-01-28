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

public class OutputFilePopup extends FilePopup {

  public OutputFilePopup(JFrame parent, AlignViewport av, Controller c,String title) {
    super(parent,av,c,title);
    setApplyAction(new FileOutputAction("align output",av,c));
  }

  public class FileOutputAction extends FileInputAction {
    public FileOutputAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
    }
    public void applyAction(ActionEvent evt) {
      String fileStr = getArg("file");

      System.out.println("Output string " + fileStr);

      if (FormatProperties.contains(getArg("format"))) {
        if (parent instanceof AlignFrame) {
          AlignFrame af = (AlignFrame)parent;
          String outStr = FormatAdapter.get(getArg("format").toUpperCase(),av.getAlignment().getSequences());
          System.out.println(outStr + " " + fileStr);
          try {
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileStr)));
            fireStatusEvent("Saving file");

            try {
              Thread.sleep(500);
            } catch (Exception ex2) {}
            ps.print(outStr);
            ps.close();

            fireStatusEvent("done");

            fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
          } catch (IOException ex) {
            fireStatusEvent("Can't open file",StatusEvent.ERROR);
            System.out.println("Exception : "+ ex);
          }
        } else {
          fireStatusEvent("(Internal Error) Parent isn't Alignment Frame",StatusEvent.ERROR);
        }
      } else {
        fireStatusEvent("Format not yet supported",StatusEvent.ERROR);
      }
    }
  }

  protected String getBrowseString() {
    return ("Save alignment file");
  }

  protected int getBrowseMode() {
    return FileDialog.SAVE;
  }

}
