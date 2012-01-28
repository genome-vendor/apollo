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
import jalview.gui.DrawableSequence;
import apollo.datamodel.SeqFeatureI;

import java.util.*;
import java.awt.*;

public class FeatureColourScheme extends ResidueColourScheme {

  public void setColours(DrawableSequence seq) {
    for (int i=0; i < seq.getLength(); i++) {
      seq.setResidueBoxColour(i,Color.white);
    }

    if (seq.getFeatures() != null && seq.getFeatures().size() != 0) {

      for (int i=0; i < seq.getFeatures().size(); i++) {
        //System.out.println("Feature numver = " + i + " " + seq.features.size());
        SeqFeatureI sf = (SeqFeatureI)seq.getFeatures().elementAt(i);
        //System.out.println("Feature = " + sf);
        if (sf.getStart() <= seq.getEnd() && sf.getEnd() >= seq.getStart()) {
          int startx = seq.findIndex(sf.getStart())-1;
          // System.out.println("picky");
          int endx = seq.findIndex(sf.getEnd())-1;
          //System.out.println("icky");
          if (!sf.getFeatureType().equals("DISULFID")) {
            if (endx >= 0 && startx < seq.getLength()-1) {
              //  System.out.println("wicky");
              if (startx < 0) {
                startx = 0;
              }
              //System.out.println("sicky");
              if (endx > seq.getLength()) {
                endx = seq.getLength()-1;
              }

              // System.out.println("Feature " + sf.getStart() + " " + sf.getEnd() + " " + startx + " " + endx);
              for  (int ii=startx; ii <= endx; ii++) {
                // System.out.println("Setting " + seq.getName() + " " + ii + " " + getColor(sf) + " " + sf.getType());
                seq.setResidueBoxColour(ii,getColor(sf));
              }
            }
          } else {
            try {
              if (startx >= 0 && startx < seq.getLength() -1) {
                seq.setResidueBoxColour(startx,getColor(sf));
                // System.out.println("Setting " + seq.name + " " + startx + " " + sf.color + " " + sf.type);
              }
              if (endx >= 0 && endx < seq.getLength() -1) {
                seq.setResidueBoxColour(endx,getColor(sf));
                // System.out.println("Setting " + seq.name + " " + endx + " " + sf.color + " " + sf.type);
              }

            } catch (Exception e) {
              System.out.println(e);
            }

          }
          // System.out.println("Done setting feature");
        }

      }
    }
  }

  public void setColours(SequenceGroup sg) {
//MG    for (int j = 0; j < sg.sequences.size(); j++) {
//MG      DrawableSequence s = (DrawableSequence)sg.sequences.elementAt(j);
//MG      setColours(s);
//MG    }
  }
  public Color findColour(DrawableSequence seq, String s, int j) {
    if (seq.getFeatures() != null && seq.getFeatures().size() != 0) {

      for (int i=0; i < seq.getFeatures().size(); i++) {
        SeqFeatureI sf = (SeqFeatureI)seq.getFeatures().elementAt(i);

        int startx = seq.findIndex(sf.getStart())-1;
        int endx = seq.findIndex(sf.getEnd())-1;


        if (startx < 0) {
          startx = 0;
        }
        if (endx > seq.getEnd()) {
          endx = seq.getEnd();
        }


        if (startx <= j && endx >= j) {
          //System.out.println("Setting colour for " + sf.type + " to " + sf.color);
          //System.out.println("Setting " + seq.name + " " + j + " " + sf.color);
          return getColor(sf);
        }
      }
    }
    return Color.white;
  }

  protected Color getColor (SeqFeatureI sf) {
    String type = sf.getFeatureType();
    Color color;
    if (type.equals("CHAIN")) {
      color = Color.white;
    } else if (type.equals("DOMAIN")) {
      color = Color.white;
    } else if (type.equals("TRANSMEM")) {
      color = Color.red.darker();
    } else if (type.equals("SIGNAL")) {
      color = Color.cyan;
    } else if (type.equals("HELIX")) {
      color = Color.magenta;
    } else if (type.equals("TURN")) {
      color = Color.cyan;
    } else if (type.equals("SHEET")) {
      color = Color.yellow;
    } else if (type.equals("STRAND")) {
      color = Color.yellow;
    } else if (type.equals("CARBOHYD")) {
      color = Color.pink;
    } else if (type.equals("ACT_SITE")) {
      color = Color.red;
    } else if (type.equals("TRANSIT")) {
      color = Color.orange;
    } else if (type.equals("VARIANT")) {
      color = Color.orange.darker();
    } else if (type.equals("BINDING")) {
      color = Color.blue;
    } else if (type.equals("DISULFID")) {
      color = Color.yellow.darker();
    } else if (type.equals("NP_BIND")) {
      color = Color.red;
    } else if (type.indexOf("BIND") > 0) {
      color = Color.red;
    } else {
      color = Color.lightGray;
    }
    return color;
  }
}
