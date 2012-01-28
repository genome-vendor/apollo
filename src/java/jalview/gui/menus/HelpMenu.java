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

package jalview.gui.menus;

import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.gui.event.*;
import jalview.analysis.*;
import jalview.gui.*;
import jalview.util.*;

import java.awt.event.*;
import java.applet.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class HelpMenu extends FrameMenu {

  InformationAction info;
  ContentsAction    contents;

  public HelpMenu(AlignFrame frame,AlignViewport av,Controller c) {
    super("Help",frame,av,c);
  }

  protected void init() {
    info     = new InformationAction("Information");
    contents = new ContentsAction("Contents");

    add(info);
    add(contents);
  }

  class InformationAction extends JalAction {
    public InformationAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      if (frame != null && frame.getAFParent() instanceof Applet) {
        controller.handleStatusEvent(new StatusEvent(this,"Fetching URL help file",StatusEvent.INFO));

        try {
          String urlStr = "http://jura.ebi.ac.uk:6543/~michele/jalview/help.html";
          URL u = new URL(urlStr);
          ((Applet)frame.getParent()).getAppletContext().showDocument(u,"right");
        } catch (MalformedURLException ex) {
          controller.handleStatusEvent(new StatusEvent(this,"Couldn't fetch help URL",StatusEvent.ERROR));
        }
        controller.handleStatusEvent(new StatusEvent(this,"Help file displayed in browser",StatusEvent.INFO));
      } else {
        controller.handleStatusEvent(new StatusEvent(this,"Fetching URL information file (NOT!!!)",StatusEvent.WARNING));

        //IceBrowser ib = new IceBrowser("Jalview information file","http://jura.ebi.ac.uk:6543/jalview/index.html");
        //ib.resize(600,800);
        // ib.show();
      }
    }
  }
  class ContentsAction extends JalAction {
    public ContentsAction(String name) {
      super(name);
    }
    public void applyAction(ActionEvent evt) {
      if (frame != null && frame.getAFParent() instanceof Applet) {

        controller.handleStatusEvent(new StatusEvent(this,"Fetching URL help file",StatusEvent.INFO));

        try {
          String urlStr = "http://jura.ebi.ac.uk:6543/~michele/jalview/contents.html";
          URL u = new URL(urlStr);
          ((Applet)frame.getParent()).getAppletContext().showDocument(u,"right");
        } catch (MalformedURLException ex) {
          controller.handleStatusEvent(new StatusEvent(this,"Couldn't fetch help URL",StatusEvent.ERROR));
        }
        controller.handleStatusEvent(new StatusEvent(this,"Help file displayed in browser",StatusEvent.INFO));

      } else {
        controller.handleStatusEvent(new StatusEvent(this,"Fetching URL help file (NOT!!!)",StatusEvent.WARNING));

        //IceBrowser ib = new IceBrowser("Jalview help file","http://jura.ebi.ac.uk:6543/jalview/index.html");
        //ib.resize(600,800);
        //ib.show();
      }
    }
  }
}
