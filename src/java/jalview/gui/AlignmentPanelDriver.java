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

import jalview.gui.schemes.*;
import jalview.datamodel.*;

import jalview.io.*;

import java.awt.*;
import java.util.*;

public class AlignmentPanelDriver extends Driver {

  public static void main(String[] args) {
    Frame f = new Frame("SeqPanel");

//MG    Sequence[] seq = FormatAdapter.read(args[0],"File","POSTAL");
//MG    ScoreSequence[] s = new ScoreSequence[seq.length];
//MG    for (int i=0;i < seq.length;i++) {
//MG      s[i] = new ScoreSequence(seq[i]);
//MG    }
//MG    DrawableSequence[] s1 = new DrawableSequence[seq.length];
//MG    for (int i=0;i < seq.length;i++) {
//MG      s1[i] = new DrawableSequence(seq[i]);
//MG    }

//MG    AlignmentPanel ap = new AlignmentPanel(null,s1);
//MG    ScorePanel sp = new ScorePanel(null,s);
    //   ap.setScorePanel(sp);
    f.setLayout(new BorderLayout());
//MG    f.add("Center",ap);
    f.resize(700,500);
    f.show();
  }
}
