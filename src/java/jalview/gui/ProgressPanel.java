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
package jalview.gui;

import jalview.analysis.*;

import java.awt.*;
import java.awt.event.*;

public class ProgressPanel extends Panel implements ActionListener,
                                                    Runnable {
  Thread ct = null;
  Object parent;
  long   tstart;
  long   tend;

  GridBagLayout      gb;
  GridBagConstraints gbc;

  Button cancel;
  Label  status;
  Label  time;

  public ProgressPanel(Object parent, Thread ct ) {
    this.ct     = ct;
    this.parent = parent;

    gb  = new GridBagLayout();
    gbc = new GridBagConstraints();

    setLayout(gb);

    status = new Label("Status: Process stopped",Label.LEFT);
    time   = new Label("Time elapsed = ",        Label.LEFT);

    cancel = new Button("Cancel");
    cancel.addActionListener(this);

    gbc.insets = new Insets(10,10,0,0);
    gbc.fill   = GridBagConstraints.EAST;

    add(status,gb,gbc,0,0,1,1);
    add(time,gb,gbc,0,1,1,1);

    gbc.fill = GridBagConstraints.NONE;

    add(cancel,gb,gbc,0,2,1,1);
  }

  public Dimension getPreferredSize() {
    return new Dimension(400,200);
  }
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public void run() {
    tstart = System.currentTimeMillis();

    boolean done = false;
    boolean running = false;

    while (done == false) {

      if  (ct != null && ct.isAlive()) {
        running = true;
        tend    = System.currentTimeMillis();

        status.setText("Process running");
        time.setText("Time elapsed = " + (tend-tstart)/1000 + " seconds");

      }   else  if (running) {
        done = true;
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        System.out.println("Interrupted Exception : " + ex);
      }
    }

    tend = System.currentTimeMillis();
    status.setText("Process finished");

    if (done) {
      if (ct instanceof ClustalwThread) {
        viewOutput();
      }
    }
    time.setText("Total time taken = " + (tend-tstart)/1000 + " seconds");
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == cancel) {
      if (ct != null && ct.isAlive()) {
        if (ct instanceof CommandThread) {
          CommandThread tmp = (CommandThread)ct;
          System.out.println("Destroying process");
          tmp.getProcess().destroy();
        }

        ct.stop();
        ct = null;
        status.setText("Status: Process stopped");
      } else {
        status.setText("No process running");
      }
    }
  }

  public void add(Component c,GridBagLayout gbl, GridBagConstraints gbc,
                  int x, int y, int w, int h) {
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = w;
    gbc.gridheight = h;
    gbl.setConstraints(c,gbc);
    add(c);
  }

  public void viewOutput() {
    if (ct instanceof ClustalwThread) {
      AlignFrame af = new AlignFrame(parent,((ClustalwThread)ct).getOutFile(),"File","CLUSTAL");
      af.resize(700,500);
      af.show();
    }
  }

  public void setThread(Thread t) {
    ct = t;
  }
}
