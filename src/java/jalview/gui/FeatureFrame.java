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

import java.awt.*;
import java.awt.event.*;

import jalview.gui.event.*;


public class FeatureFrame extends TextFrame {

  Object parent;
  AlignViewport av;
  Controller controller;
  SequenceFeatureThread sft;

  public FeatureFrame(AlignViewport av,Controller c,Object parent,SequenceFeatureThread sft,String title,int nrows, int ncols, String text) {
    super(title,nrows,ncols,text);
    this.parent = parent;
    this.av = av;
    this.controller = c;
    this.sft = sft;
    FeatureFrameWindowListener ffwl = new FeatureFrameWindowListener(this);
    b.addActionListener(this);
  }

  class FeatureFrameWindowListener extends WindowAdapter {
    FeatureFrame ff;
    public FeatureFrameWindowListener(FeatureFrame f) {
      ff = f;
      ff.addWindowListener(this);
    }
    public void windowClosing(WindowEvent evt) {
      ff.hide();
      ff.dispose();
//      if (parent instanceof AlignFrame) {
//        AlignFrame af = (AlignFrame)parent;
//        af.ff = null;
//        if (af.sft.isAlive()) {
//          System.out.println("icky");
//          af.sft.stop();
//          af.sft = null;
//
//        }
//        av.getStatusBar().setText("Closed sequence feature console");
//        av.getStatusBar().validate();
//      }
      tidyUp();
    }
  }

  private void tidyUp() {
    if (sft.isAlive()) {
      System.out.println("icky");
      sft.stop();
    }
    controller.handleStatusEvent(new StatusEvent(this,"Closed sequence feature console",StatusEvent.INFO));
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == b) {
      System.out.println("Disposing of feature frame");
      hide();
      dispose();
//      if (parent instanceof AlignFrame) {
//        AlignFrame af = (AlignFrame)parent;
//        af.ff = null;
//        if (af.sft.isAlive()) {
//          af.sft.stop();
//          af.sft = null;
//        }
//        av.getStatusBar().setText("Closed sequence feature console");
//        av.getStatusBar().validate();
//      }
      tidyUp();
    }
  }
}

