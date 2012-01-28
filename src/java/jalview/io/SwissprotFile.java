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
import jalview.gui.SimpleBrowser;

import java.io.*;
import java.util.*;

import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;

public class SwissprotFile extends FileParse implements SequenceFeatureSourceI {
  String title;
  String id = "";
  String acc;
  AlignSequenceI sequence;
  Vector features;
  int length;
  Vector pdbcode = new Vector();

  public String getId() {
    return id;
  }

  public AlignSequenceI getSequence() {
    return sequence;
  }

  public String getSequenceString() {
    return sequence.getResidues();
  }

  public Vector getFeatures() {
    return features;
  }

  public Vector getPDBCode() {
    return pdbcode;
  }

  public SwissprotFile(String inStr) {
    features = new Vector();
    readLines(inStr);
    parse();
  }
  public SwissprotFile(String inFile, String type) throws IOException {
    super(inFile,type);
    features = new Vector();
    readLines();
    parse();
  }

  private String removeHtml(String line) {
    StringBuffer output = new StringBuffer();
    int i=0;
    String curChar;
    curChar = line.substring(i,i+1);
    // System.out.println("removeHtml called: i = " + i + " curChar = " + curChar);
    while (i<line.length()) {
      if (curChar.equals("<") && isHtmlToken(line,i)) {
        i = skipAngled(line,i);
      } else {
        // System.out.println("  Added to output: " + curChar);
        output.append(curChar);
        i++;
      }
      // System.out.println("getting next char");
      if (i < line.length()) {
        curChar = line.substring(i,i+1);
      }
    }
    // System.out.println(" returning from removeHtml");
    return output.toString();
  }

  private boolean isHtmlToken(String line, int pos) {
    if (line.indexOf("<A ",pos) == pos) {
      return true;
    }
    if (line.indexOf("</A>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<pre>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<PRE>",pos) == pos) {
      return true;
    }
    if (line.indexOf("<br>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</br>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</pre>",pos) == pos) {
      return true;
    }
    if (line.indexOf("</PRE>",pos) == pos) {
      return true;
    }
    return false;
  }
  private int skipAngled(String line, int start) {
    // System.out.println("skipAngled called at " + start);
    int pos = start+1;
    String curChar = line.substring(pos,pos+1);
    while (pos<line.length() && !curChar.equals(">")) {
      // System.out.println("  skipping " + curChar);
      if (curChar.equals("<") && isHtmlToken(line,pos)) {
        pos = skipAngled(line,pos);
      } else {
        pos++;
      }
      curChar = line.substring(pos,pos+1);
    }
    // System.out.println("return from skipAngled");
    return ++pos;
  }

  public void parse() {
    int start = -1;
    int end = -1;

    for (int i=0; i < noLines;i++) {
      if (lineArray.elementAt(i).toString().indexOf(" ") != 0 &&
          lineArray.elementAt(i).toString().length() != 0) {

        String tmpStr = removeHtml(lineArray.elementAt(i).toString());

        // System.out.println("Line = " + lineArray.elementAt(i).toString());
        // System.out.println("tmpStr = " + tmpStr);
        StringTokenizer str = new StringTokenizer(tmpStr," ");

        String type = "";
        if (tmpStr.length() > 0) {
          type = str.nextToken().toUpperCase();
        }

        if (type.equals("ID")) {
          id = str.nextToken();
        } else if (type.equals("AC")) {
          acc = str.nextToken();
          if (acc.indexOf(";") != -1) {
            acc = acc.substring(0,acc.indexOf(";"));
          }
        } else if (type.equals("DE")) {
          title = str.nextToken();
          i++;
          while (lineArray.elementAt(i).toString().indexOf(" ") == 0) {
            title = title + lineArray.elementAt(i).toString();
            i++;
          }
        } else if (type.equals("SQ")) {
          String tmp = str.nextToken();
          tmp = str.nextToken();

          length = Integer.valueOf(tmp).intValue();
          i++;
          String seq = "";
          while (lineArray.elementAt(i).toString().indexOf(" ") == 0) {
            seq = seq + lineArray.elementAt(i).toString();
            i++;
          }
          String seq2 = "";
          StringTokenizer str2 = new  StringTokenizer(seq," ");
          while (str2.hasMoreTokens()) {
            seq2 = seq2 + str2.nextToken();
          }
          if (start != -1) {
            sequence = new AlignSequence(id,seq2,start,end);
          } else {
            sequence = new AlignSequence(id,seq2,1,length);
          }
          for (int j=0;j < features.size();j++) {
            SeqFeatureI sf = (SeqFeatureI)features.elementAt(j);
            sequence.addFeature(sf);
            sf.setRefSequence(sequence);
          }
        } 
	else if (type.equals("FT") &&
		 !(lineArray.elementAt(i).toString().substring(6,7).equals(" "))) {
          String ftype = str.nextToken();
          try {
            int fstart = Integer.valueOf(str.nextToken()).intValue();
            int fend = Integer.valueOf(str.nextToken()).intValue();

            String def = "";
            while (str.hasMoreTokens()) {
              def = def + str.nextToken() + " ";
            }
            System.out.println(ftype + " " + fstart + " " + fend + " " + def);
            if (!(ftype.equals("REPEAT"))) {
	      AlignSequenceI s = new AlignSequence (ftype, "", fstart, fend);
	      s.setDescription (def);
	      SeqFeatureI sf = new SeqFeature(fstart,fend,ftype);
	      sf.setRefSequence(s);
              features.addElement(sf);
            }
          } catch (NumberFormatException e) {
            System.out.println("Exception : " + e);
          }

        } else if (type.equals("DR")) {
          //	  System.out.println("Found DR line");

          // Parse the database refs;
          String tmp = SimpleBrowser.parse(lineArray.elementAt(i).toString());
          StringTokenizer str2 = new StringTokenizer(tmp);

          String dtype = str2.nextToken();
          dtype = str2.nextToken();

          if (dtype.indexOf(";") == (dtype.length()-1)) {
            dtype = dtype.substring(0,dtype.indexOf(";"));
          }
          if (dtype.equals("PDB")) {
            String code = str2.nextToken();
            if (code.indexOf(";") == (code.length()-1)) {
              code = code.substring(0,code.indexOf(";"));
            }
            System.out.println("Found pdb code " + code);
            pdbcode.addElement(code);
          }
        }
      }
    }
    print();
  }
  public void print() {
    System.out.println("ID = " + id);
    System.out.println("ACC = " + acc);
    System.out.println("length = " + length);
    System.out.println("SEQ = " + sequence.getResidues());
    for (int j=0;j < features.size();j++) {
      SeqFeatureI sf = (SeqFeatureI)features.elementAt(j);
      System.out.println(sf.getFeatureType() + " " + sf.getStart() + " " + sf.getEnd() + " " + sf.getRefSequence().getDescription());
    }
  }

  public static void main(String[] args) {
    try {
      SwissprotFile sp = new SwissprotFile("http://srs.ebi.ac.uk:5000/srs5bin/cgi-bin/wgetz?-e+[swall-id:" + args[0] + "]","URL");
      sp.print();
    } catch (IOException e) {
      System.out.println("Exception " + e);
    }
  }


}
