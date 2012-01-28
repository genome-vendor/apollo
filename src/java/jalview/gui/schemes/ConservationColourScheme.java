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

package jalview.gui.schemes;

import jalview.datamodel.*;
import jalview.analysis.Conservation;
import jalview.gui.DrawableSequence; //MG

import java.util.*;
import java.awt.*;

/**
 * This colour scheme can be used to modify the colours generated
 * by another ColourScheme.
 */
public class ConservationColourScheme extends ResidueColourScheme {
  Conservation conserve;
  boolean byResidue = true;
  ColourScheme cs;
  int inc = 30;

  public ConservationColourScheme(SequenceGroup sg, int inc) {
    this(sg);
    this.inc = inc;
  }

  public ConservationColourScheme(SequenceGroup sg) {
    super();
    this.conserve = sg.getConservation();
//MG    colourThreshold = 7;
//MG    this.cs = sg.colourScheme;
  }

  public void setColours(DrawableSequence seq, int j) {
    Color c = Color.white;
    String s = String.valueOf(seq.getBaseAt(j));
    try {

      if (/*colourThreshold > 0 &&*/ conserve.getConsSequence() != null) { //MG
        if (fullConservation(j)) {
          if (byResidue) {
            c = findColour(seq,s,j);
          } else {
            c = Color.red;
          }
        } else {
          if (byResidue) {

            int tmp = 10;
            int t = Integer.parseInt(conserve.getConsSequence().getResidues().substring(j,j+1));
            c = findColour(seq,s,j);

            while (tmp >= t) {
              //      c = c.darker();
              c = lighter(c,inc);
              tmp--;
            }
          } else {
            c = Color.yellow;
          }
        }
      }
      seq.setResidueBoxColour(j,c);
    } catch (Exception e) {
      seq.setResidueBoxColour(j,Color.white);
    }
  }

  public Color findColour(DrawableSequence seq, String s, int j) {
    return cs.findColour(seq, s,j);
  }

  public boolean fullConservation(int j) {
    String tmp = String.valueOf(conserve.getConsSequence().getBaseAt(j));
    if (tmp.equals("*")) {
      return true;
    } else {
      return false;
    }
  }

  public boolean aboveThreshold(DrawableSequence seq, int j, int threshold) {
    String tmp = String.valueOf(conserve.getConsSequence().getBaseAt(j));

    if (Integer.parseInt(tmp) >= threshold || tmp.equals("*")) {
      return true;
    } else {
      return false;
    }
  }

  public Color lighter(Color c, int inc) {
    int red = c.getRed();
    int blue = c.getBlue();
    int green = c.getGreen();

    if (red < 255-inc) {
      red = red +inc;
    } else {
      red = 255;
    }
    if (blue < 255-inc) {
      blue = blue +inc;
    } else {
      blue = 255;
    }
    if (green < 255-inc) {
      green = green +inc;
    } else {
      green = 255;
    }

    return new Color(red,green,blue);
  }

  public int getInc() {
    return inc;
  }

  public void setInc(int inc) {
    this.inc = inc;
  }

  public ColourScheme getColourScheme() {
    return cs;
  }
}
