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
import jalview.datamodel.*;
import jalview.gui.*;
import jalview.gui.menus.*;
import jalview.gui.event.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import java.io.File;

import javax.swing.*;

public class CommandPopup extends BrowsePopup {

  public CommandPopup(JFrame parent,AlignViewport av,Controller c,String title) {
    super(parent,av,c,title);

    tfLabel = new JLabel("Filename : ");

    gbc.fill = GridBagConstraints.NONE;

    gbc.insets = new Insets(20,20,20,20);

    add(tfLabel,gb,gbc,0,0,1,1);
    add(tf,gb,gbc,1,0,4,1);
    add(b,gb,gbc,5,0,1,1);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(status,gb,gbc,0,2,1,1);
    gbc.fill = GridBagConstraints.NONE;
    add(apply,gb,gbc,0,3,1,1);
    add(close,gb,gbc,1,3,1,1);

    
    setApplyAction(new RunCommandAction("run commands",av,c));

    pack();
    show();
  }

  public class RunCommandAction extends BrowseAction {
    public RunCommandAction(String name, AlignViewport av,Controller c) {
      super(name,av,c);
    }

    public void applyAction(ActionEvent evt) {
      fireStatusEvent("Reading file...");

      String fileStr = getArg("file");
      File tmp = new File(fileStr);
      CommandParser cp = av.getCommandLog();

      if (tmp.isFile()) {
        CommandLog log = new CommandLog(fileStr);
        log.getCommands();
  
        if (log != null) {
          fireStatusEvent("Running commands...");

          cp.runCommands(log);

          fireStatusEvent("done");
          fireJalActionEvent(new JalActionEvent(this,this,JalActionEvent.DONE));
        } else {
          fireStatusEvent("No commands found. Check format.",StatusEvent.ERROR);
        }
      } else {
        fireStatusEvent("File not found",StatusEvent.ERROR);
      }
    }
  }

  protected String getBrowseString() {
    return "Open command file";
  }
}
