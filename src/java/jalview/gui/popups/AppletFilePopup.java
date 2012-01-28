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

public class AppletFilePopup extends FilePopup {

  public AppletFilePopup(JFrame parent, AlignViewport av, Controller c,String title) {
    super(parent,av,c,title);
    tf.hide();
    b.hide();
    b = null;
    tf = null;
    tfLabel.hide();
    tfLabel = null;
    validate();
    setApplyAction(new AppletFileOutputAction("applet output",av,c));
  }

  public class AppletFileOutputAction extends JalPopupAction {
    public AppletFileOutputAction(String name,AlignViewport av,Controller c) {
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
      if (FormatProperties.contains(getArg("format"))) {
        if (parent instanceof AlignFrame) {
          AlignFrame af = (AlignFrame)parent;
          String outStr = FormatAdapter.get(getArg("format").toUpperCase(),av.getAlignment().getSequences());

          fireStatusEvent("Saving file to server");
  
          try {
            Thread.sleep(500);
          } catch (Exception ex2) {}
  
          fireStatusEvent("Sending file to browser...");
  
          int rnums = (int)Math.round((10000-1) * Math.random()) + 1;
          //String outDir  = "/net/nfs0/vol1/production/webadmin/tmp/";
          String outDir  = "/homes/michele/public_html/temp/";
          String outFile = outDir + "jalview" + rnums + ".txt";
          String outHttpFile = "jalview" + rnums + ".txt";
  
          SendFileCGI sf= new SendFileCGI("www2.ebi.ac.uk",80,"cgi-bin/michele.cgi",outFile,System.out,outStr);
  
          sf.run();

          try {
            URL fileski = new URL("http://www2.ebi.ac.uk/~michele/temp/" + outHttpFile);
            if (af.getAFParent() instanceof Applet) {
              ((Applet)af.getAFParent()).getAppletContext().showDocument(fileski);
            }
          } catch (MalformedURLException ex1) {
            fireStatusEvent("ERROR: Can't open file",StatusEvent.ERROR);
            System.out.println("Exception : "+ ex1);
          }

          fireStatusEvent("done");

          fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
        } else {
          fireStatusEvent("(Internal error) Parent isn't an alignment frame",StatusEvent.ERROR);
        }
      } else {
        fireStatusEvent("Format not yet supported",StatusEvent.ERROR);
      }
    }
  }
}
