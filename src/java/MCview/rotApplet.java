package MCview;

import java.awt.*;
import java.applet.*;

import java.io.*;
import java.util.*;

public class rotApplet extends Applet {
  String pdbid;
  String type;
  Panel p;
  rotCanvas rc;
  TextField tf;

  public void init() {

    pdbid = this.getParameter("PDBID");
    type = this.getParameter("TYPE");
    drawStruct();
  }

  public boolean action(Event evt, Object arg) {
    if (evt.target == tf) {
      pdbid = tf.getText();
      rc.deleteBonds();
//MG      rc.addPDBfile(pdbid,type);
      rc.redrawneeded = true;
      add("Center",rc);
      rc.repaint();
    } else
      super.action(evt,arg);
    return true;

  }

  public void drawStruct() {
//MG    try {
      System.out.println("poggy");
//MG      rc = new rotCanvas(new Dimension(500,500),pdbid,type);
      setLayout(new BorderLayout());
      add("Center",rc);

      p = new Panel();
      p.setLayout(new BorderLayout());
      tf = new TextField(pdbid);
      p.add("North",tf);
      add("East",p);


      //       rotFrame rf = new rotFrame(pdbid,type);
      //       rf.resize(500,500);
      //       rf.show();
//MG    }
//MG    catch (IOException e) {
//MG      System.out.println(e);
//MG    }
  }
}
