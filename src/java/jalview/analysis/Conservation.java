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
package jalview.analysis;

import jalview.gui.*;
import jalview.datamodel.AlignSequenceI;
import jalview.io.*;

import java.awt.*;
import java.applet.Applet;
import java.util.*;
import java.net.*;
import java.io.*;

public class Conservation {
  Vector sequences;
  int    start;
  int    end;

  Vector total = new Vector();

  String consString = "";

  DrawableSequence consSequence;
  Hashtable        propHash;
  int              threshold;
  Hashtable[]      freqs;

  String name = "";

  public Conservation(String name,Hashtable[] freqs,Hashtable propHash, int threshold, Vector sequences, int start, int end) {
    this.name      = name;
    this.freqs     = freqs;
    this.propHash  = propHash;
    this.threshold = threshold;
    this.sequences = sequences;
    this.start     = start;
    this.end       = end;
  }


  public void  calculate() {

    for (int i = start;i <= end; i++) {
      Hashtable resultHash  = null;
      Hashtable residueHash = null;

      resultHash  = new Hashtable();
      residueHash = new Hashtable();

      for (int j=0; j < sequences.size(); j++) {

        if (sequences.elementAt(j) instanceof AlignSequenceI) {
          AlignSequenceI s = (AlignSequenceI)sequences.elementAt(j);

          if (s.getLength() > i) {
            String res = String.valueOf(s.getBaseAt(i));

            if (residueHash.containsKey(res)) {
              int count = ((Integer)residueHash.get(res)).intValue() ;
              count++;
              residueHash.put(res,new Integer(count));
            } else {
              residueHash.put(res,new Integer(1));
            }
          } else {
            if (residueHash.containsKey("-")) {
              int count = ((Integer)residueHash.get("-")).intValue() ;
              count++;
              residueHash.put("-",new Integer(count));
            } else {
              residueHash.put("-",new Integer(1));
            }
          }
        }
      }

      //What is the count threshold to count the residues in residueHash()
      int thresh = threshold*(sequences.size())/100;

      //loop over all the found residues
      Enumeration e = residueHash.keys();

      while (e.hasMoreElements()) {

        String res = (String)e.nextElement();
        if (((Integer)residueHash.get(res)).intValue() > thresh) {

          //Now loop over the properties
          Enumeration e2 = propHash.keys();

          while (e2.hasMoreElements()) {
            String    type = (String)e2.nextElement();
            Hashtable ht   = (Hashtable)propHash.get(type);

            //Have we ticked this before?
            if (! resultHash.containsKey(type)) {
              if (ht.containsKey(res)) {
                resultHash.put(type,ht.get(res));
              } else {
                resultHash.put(type,ht.get("-"));
              }
            } else if ( ((Integer)resultHash.get(type)).equals((Integer)ht.get(res)) == false) {
              resultHash.put(type,new Integer(-1));
            }
          }
        }
      }
      total.addElement(resultHash);
    }
  }

  public int countGaps(int j) {
    int count = 0;

    for (int i = 0; i < sequences.size();i++) {
      if (((AlignSequenceI)sequences.elementAt(i)).residueIsSpacer(j))
        count++;
    }
    return count;
  }

  public  void  verdict(boolean consflag, float percentageGaps) {
    String consString = "";

    for (int i=start; i <= end; i++) {
      int totGaps = countGaps(i);
      float pgaps = (float)totGaps*100/(float)sequences.size();

      if (percentageGaps > pgaps) {
        Hashtable resultHash = (Hashtable)total.elementAt(i);

        //Now find the verdict
        int         count = 0;
        Enumeration e3    = resultHash.keys();

        while (e3.hasMoreElements()) {
          String type    = (String)e3.nextElement();
          Integer result = (Integer)resultHash.get(type);

          //Do we want to count +ve conservation or +ve and -ve cons.?

          if (consflag) {
            if (result.intValue() == 1) {
              count++;
            }
          } else {
            if (result.intValue() != -1) {
              count++;
            }
          }
        }

        if (count < 10) {
          consString = consString + String.valueOf(count);
        } else {
          consString = consString + "*";
        }
      } else {
        consString = consString + "-";
      }
    }

    consSequence = new DrawableSequence(name,consString,start,end);

  }

  public jalview.gui.DrawableSequence getConsSequence() {
    return consSequence;
  }

}
