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
package jalview.io;

import jalview.datamodel.*;
import jalview.util.*;

import java.io.*;
import java.util.*;

public class MSPFile extends AlignFile {

  Vector words = new Vector();  //Stores the words in a line after splitting

  public MSPFile(AlignSequenceI [] s) {
    super(s);
  }

  public MSPFile(String inStr) {
    super(inStr);
  }

  public MSPFile(String inFile, String type) throws IOException {
    super(inFile,type);
  }

  public void parse() {

    // The first two lines are descriptive

    for (int i = 0; i < lineArray.size(); i++) {
      String line = (String)lineArray.elementAt(i);


      // Check for comment lines
      if (line.indexOf("#") == -1 ) {
        //Split into fields

        StringTokenizer st = new StringTokenizer(line);
        if (st.countTokens() == 8) {
          try {
            String s = st.nextToken();
            int score = Integer.parseInt(s);
            String frame = st.nextToken();
            int  qstart = Integer.parseInt(st.nextToken());
            int  qend   = Integer.parseInt(st.nextToken());
            int hstart = Integer.parseInt(st.nextToken());
            int hend = Integer.parseInt(st.nextToken());
            String id = st.nextToken();
            String seq = st.nextToken();
            String database = "";

            if (id.indexOf("|") != -1) {
              StringTokenizer st2 = new StringTokenizer(id,"|");
              while (st2.hasMoreTokens()) {
                String tmp = st2.nextToken();
                if (!tmp.equals("")) {
                  id = tmp;
                }
              }
            } else if (id.indexOf(":") != -1) {
              database = id.substring(0,id.indexOf(":"));
              id = id.substring(id.indexOf(":")+1);
            }
            seqs.addElement(new MSPSequence(id,seq,hstart,hend,qstart,qend,score,frame,database));
          } catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException " + nfe);
          }
        }
      }
    }
    noSeqs = seqs.size();
  }

  public static String print(AlignSequenceI[] s) {
    return print(s,true);
  }

  public static String print(AlignSequenceI[] s,boolean gaps) {
    StringBuffer out = new StringBuffer();
    int i = 0;

    while (i < s.length && s[i] != null) {

      if (s[i] instanceof MSPSequence) {
        MSPSequence tmp = (MSPSequence)s[i];
        out.append(tmp.getScore() + " (" + tmp.getFrame() + ")  " + tmp.getQStart() + " " + tmp.getQEnd() + " " +
                   tmp.getStart() + " " + tmp.getEnd() + " " + tmp.getName() + " " +
                   tmp.getResidues(tmp.getQStart()-1, tmp.getLength()) + "\n");
      }
      i++;
    }
    return out.toString();
  }



  public String unPad(String in, boolean end) {
    if (end == true) {
      while (in.length() > 0&&
             (in.substring(0,1).equals(" ") || in.substring(0,1).equals("-") || in.substring(0,1).equals("."))) {
        in = in.substring(1);
      }
      return in;
    } else if (end == false) {
      while (in.length() > 0 &&
             (in.substring(in.length()-1).equals(" ") || in.substring(in.length()-1).equals("-") || in.substring(in.length()-1).equals("."))) {
        in = in.substring(0,in.length()-1);
      }
      return in;
    }
    return null;
  }
  public String print() {
    return print(getSeqsAsArray());
  } 
}
