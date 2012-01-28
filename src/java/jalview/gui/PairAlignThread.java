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
import jalview.analysis.*;
import jalview.util.*;
import jalview.gui.event.*;

import java.awt.*;
import java.util.*;

public final class PairAlignThread extends Thread {
  AlignViewport av;
  Controller controller;

  public PairAlignThread(AlignViewport av, Controller c) {
    this.av = av;
    this.controller = c;
  }

  public void run() {
    float scores[][] = new float[av.getAlignment().getHeight()][av.getAlignment().getHeight()];
    double totscore = 0;
    int count = av.getSelection().size();

    if (count == 1) {
      controller.handleStatusEvent(new StatusEvent(this,"only 1 sequence selected",StatusEvent.ERROR));
    } else if (count == 0) {
      controller.handleStatusEvent(new StatusEvent(this,"No sequences selected",StatusEvent.ERROR));
    } else {
      PairAlignFrame tf = new PairAlignFrame("Pairwise alignments",25,73,"");
      tf.setTextFont(new Font("Courier",Font.PLAIN,12));
      tf.resize(550,550);
      tf.show();
      int acount = 0;
      for (int i = 1; i < count; i++) {
        for (int j = 0; j < i; j++) {
          acount++;
          AlignSeq as = new AlignSeq(av.getSelection().sequenceAt(i),av.getSelection().sequenceAt(j),"pep");
          tf.status.setText("Aligning " + as.getS1().getName() + " and " + as.getS2().getName() + " (" + acount + "/" + (count*(count-1)/2) + ")");

          as.calcScoreMatrix();
          as.traceAlignment();
          as.printAlignment();
          scores[i][j] = (float)as.getMaxScore()/(float)as.getASeq1().length;
          totscore = totscore + scores[i][j];

          tf.setText(tf.getText() + as.getOutput());
          tf.ta.setCaretPosition(tf.ta.getText().length());
          tf.addSequence(new DrawableSequence(as.getS1().getName(),as.getAStr1(),0,0));
          tf.addSequence(new DrawableSequence(as.getS2().getName(),as.getAStr2(),0,0));

        }
      }
      controller.handleStatusEvent(new StatusEvent(this,"done",StatusEvent.INFO));
      System.out.println();

      if (count > 2) {
        System.out.print("      ");
        //	for (int i = 0; i < count ; i++) {
        //	 Format.print(System.out,"%6d ",i);
        // Format.print(System.out,"%6d ",sel[i]+1);
        //}
        //System.out.println();

        for (int i = 0; i < count;i++) {
          //	  Format.print(System.out,"%6d",sel[i]+1);
          for (int j = 0; j < i; j++) {
            Format.print(System.out,"%7.3f",scores[i][j]/totscore);
          }
          System.out.println();
        }
      }
    }
  }
}
