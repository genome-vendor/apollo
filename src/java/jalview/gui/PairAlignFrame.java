/* Jalview  - a java multiple alignment editor
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

import jalview.datamodel.*;

import java.awt.*;
import java.awt.event.*;

public class PairAlignFrame extends TextFrame implements ActionListener {

  DrawableSequence[] s = new DrawableSequence[2000];
  Button align;
  int noseqs = 0;

  public PairAlignFrame(String title,int nrows, int ncols, String text) {
    super(title,nrows,ncols,text);
    align = new Button("View in alignment editor");
    p.add(align);
    PairAlignWindowListener pawl = new PairAlignWindowListener(this);
    align.addActionListener(this);
    b.addActionListener(this);
  }

  public void addSequence(DrawableSequence s) {
    this.s[noseqs] = s;
    noseqs++;
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == align) {
      DrawableSequence [] tmp = new DrawableSequence[noseqs];

      for (int i=0;i<noseqs;i++) {
        tmp[i] = s[i];
      }
      AlignFrame af  = new AlignFrame(this,new Alignment(tmp));
      af.resize(700,200);
      af.show();
    } else if (evt.getSource() == b) {
      System.out.println("Closing frame");
      this.hide();
      this.dispose();
    } 
  }

  class PairAlignWindowListener extends WindowAdapter {
    PairAlignFrame paf;
    public PairAlignWindowListener(PairAlignFrame f) {
      paf = f;
      paf.addWindowListener(this);
    }
    public void windowClosing(WindowEvent evt) {
      paf.hide();
      paf.dispose();
    }
  }
}
