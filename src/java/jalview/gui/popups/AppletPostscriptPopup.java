
/* Jalview - a java multiple alignment editor
 * Copyright (C) 1998  Michele Clamp
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

public class AppletPostscriptPopup extends PostscriptPopup {

  public AppletPostscriptPopup(JFrame parent, AlignViewport av, Controller c,String title,OutputGenerator og) {
    super(parent,av,c,title,og);
    setApplyAction(new AppletPostscriptAction("applet postscript",av,c));
  }

  public void createInterface() {

    super.createInterface();

    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(10,10,10,10);

    add(status,gb,gbc,0,5,1,1);
    add(apply,gb,gbc,0,6,1,1);
    add(close,gb,gbc,1,6,1,1);

    add(fontLabel,gb,gbc,0,1,1,1);
    add(font,gb,gbc,1,1,1,1);

    add(fontsizeLabel,gb,gbc,0,2,1,1);
    add(fontSize,gb,gbc,1,2,1,1);
    add(orientLabel,gb,gbc,0,3,1,1);
    add(orient,gb,gbc,1,3,1,1);

    add(sizeLabel,gb,gbc,0,4,1,1);
    add(size,gb,gbc,1,4,1,1);

    this.pack();
    this.show();
  }

  public class AppletPostscriptAction extends PostscriptAction {
    public AppletPostscriptAction(String name, AlignViewport av, Controller c) {
      super(name,av,c);
    }

    public void getArgsFromSource(JButton but, ActionEvent evt) {
      super.getArgsFromSource(but,evt);
    }
 
    public void updateActionState(ActionEvent evt) {
      super.updateActionState(evt);
    }

    public void applyAction(ActionEvent evt) {
      fireStatusEvent("Saving file to server...");
  
      setPostscriptOptions();
  
      StringBuffer sb = og.getPostscript();
      int rnums = (int)Math.round((10000-1) * Math.random()) + 1;
      String outDir  = "/homes/michele/public_html/temp/";
      String outFile = outDir + "jalview" + rnums + ".ps";
      String outHttpFile = "jalview" + rnums + ".ps";
  
      SendFileCGI sf= new SendFileCGI("www2.ebi.ac.uk",80,"cgi-bin/michele.cgi",outFile,System.out,sb.toString());
  
      sf.run();
      try {
        URL fileski = new URL("http://www2.ebi.ac.uk/~michele/temp/" + outHttpFile);
        if (parent instanceof AlignFrame) {
          if (((AlignFrame)parent).getAFParent() instanceof Applet) {
            AlignFrame af = (AlignFrame)parent;
            ((Applet)af.getAFParent()).getAppletContext().showDocument(fileski);
          }
        }
      } catch (MalformedURLException ex1) {
        fireStatusEvent("ERROR: Can't open file",StatusEvent.ERROR);
        System.out.println("Exception : "+ ex1);
      }

      try {
        Thread.sleep(500);
      } catch (Exception ex2) {}
  
      fireStatusEvent("done");

      fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
    }
  }
}
