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

import jalview.datamodel.*;
import jalview.gui.schemes.*;
import jalview.io.*;

import java.awt.*;
import javax.swing.JFrame; //MG
import MCview.*;


//MG public class SingleSequencePanelDriver extends Driver {
public class SeqPanelDriver extends Driver { // MG changed

  public static void main(String[] args) {
    JFrame f = new JFrame("SingleSequencePanel");
    MSFfile msf;
    DrawableSequence[] s = null;

    ScrollPane sp = new ScrollPane();

    f.getContentPane().add(sp);

    try {
      msf = new MSFfile("pog.msf","File");
      for (int i=0;i < msf.getSeqs().size();i++) {
        s[i] = new DrawableSequence((AlignSequenceI)msf.getSeqs().elementAt(i));

//MG        SingleSequencePanel ssp = new SingleSequencePanel(s[i]);

//MG        sp.add(ssp);

      }
    } catch (java.io.IOException e) {
      System.out.println("Exception : " + e);
    }



      f.add(sp);
      f.resize(700,500);
      f.show();
  }
}


