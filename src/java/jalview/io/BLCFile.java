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

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.Font;

public class BLCFile extends AlignFile {

  Vector titles;

  public BLCFile(AlignSequenceI [] s) {
    super(s);
  }
  public BLCFile(String inStr) {
    super(inStr);
  }

  public void initData() {
    super.initData();
    titles = new Vector();
  }

  public BLCFile(String inFile, String type) throws IOException {
    super(inFile,type);
  }

  public void parse() {
    boolean foundids = false;
    Vector seqstrings = new Vector();
    Vector starts = new Vector();
    Vector ends = new Vector();

    for (int i=0; i < lineArray.size(); i++) {
      String line = (String)lineArray.elementAt(i);
      if (line.indexOf(">") >= 0) {
        // Extract an id
        String tmp = line.substring(line.indexOf(">")+1);
        StringTokenizer st = new StringTokenizer(tmp);
        String id = st.nextToken();

        if (id.indexOf("/") > 0 ) {
          StringTokenizer st2 = new StringTokenizer(id,"/");
          if (st2.countTokens() == 2) {
            id = st2.nextToken();
            String tmp2 = st2.nextToken();
            st2 = new StringTokenizer(tmp2,"-");
            if (st2.countTokens() == 2) {
              starts.addElement(new Integer(st2.nextToken()));
              starts.addElement(new Integer(st2.nextToken()));
            } else {
              starts.addElement(new Integer(-1));
              ends.addElement(new Integer(-1));
            }
          }
        } else {
          starts.addElement(new Integer(-1));
          ends.addElement(new Integer(-1));
        }
        // Extract the title
        String title = "";
        while (st.hasMoreTokens()) {
          title = title + " " + st.nextToken();
        }
        System.out.println(id);
        headers.addElement(id);
        titles.addElement(title);
        seqstrings.addElement("");
        foundids = true;
      }

      if (line.indexOf("*") >= 0) {
        int startcol = line.indexOf("*");
        i++;
        line = (String)lineArray.elementAt(i);
        while (i < lineArray.size() && line.indexOf("*") < 0) {
          System.out.println(":"+line+":");
          for (int j = startcol; j < headers.size(); j++) {
            String s = (String)seqstrings.elementAt(j);
            //  System.out.println(j + " " + line.length());
            if (line.length() > j) {
              seqstrings.setElementAt(s + line.substring(j,j+1),j);
            } else {
              seqstrings.setElementAt(s + "-",j);
            }
          }
          i++;
          line = (String)lineArray.elementAt(i);
        }
        for (int j = startcol; j < headers.size(); j++) {
          if (((Integer)starts.elementAt(j)).intValue() >= 0 && ((Integer)ends.elementAt(j)).intValue() >= 0) {
            seqs.addElement(new AlignSequence((String)headers.elementAt(j-startcol),
                                         (String)seqstrings.elementAt(j),
                                         ((Integer)starts.elementAt(j)).intValue(),
                                         ((Integer)ends.elementAt(j)).intValue()));

          } else {
            seqs.addElement(new AlignSequence((String)headers.elementAt(j-startcol),
                                         (String)seqstrings.elementAt(j),
                                         1,
                                         ((String)seqstrings.elementAt(j)).length()));
          }
        }

      }
    }
  }

  public String print() {
    return print(getSeqsAsArray());
  }
  public static String print(AlignSequenceI[] s) {
    StringBuffer out = new StringBuffer();

    int i=0;
    int max = -1;
    while (i < s.length && s[i] != null) {
      out.append(">" + s[i].getName() + "/" + s[i].getStart()+ "-" + s[i].getEnd() + "\n");
      if (s[i].getLength() > max) {
        max = s[i].getLength();
      }
      i++;

      out.append("* iteration\n");
      for (int j = 0; j < max; j++) {
        i=0;
        while (i < s.length && s[i] != null) {
          out.append(s[i].getBaseAt(j));
          i++;
        }
        out.append("\n");
      }
    }
    out.append("*\n");
    return out.toString();
  }
}
