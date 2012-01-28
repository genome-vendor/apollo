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
import jalview.analysis.*;

import java.io.*;
import java.util.*;

public class FastaFile extends AlignFile {

  Vector words = new Vector();  //Stores the words in a line after splitting

  public FastaFile(String inStr) {
    super(inStr);
  }

  public FastaFile(AlignSequenceI [] s) {
    super(s);
  }

  public FastaFile(String inFile, String type) throws IOException {
    super(inFile,type);
  }

  public void parse() {

    String id = "";
    String seq = "";
    int count = 0;
    boolean flag = false;

    int sstart = 0;
    int send = 0;

    for (int i = 0; i < lineArray.size(); i++) {

      //Do we have an id line?
      if (((String)lineArray.elementAt(i)).length() > 0) {
        if (((String)lineArray.elementAt(i)).substring(0,1).equals(">")) {
          flag = true;
          // If this isn't the first sequence add the previous sequence to the array
          if (count != 0) {
            if (sstart != 0) {
              seqs.addElement(new AlignSequence(id,seq,sstart,send));
            } else {
              seqs.addElement(new AlignSequence(id,seq,1,seq.length()));
            }

          }
          count++;
          StringTokenizer str = new StringTokenizer((String)lineArray.elementAt(i)," ");
          // Extract the id and take off the >
          id = str.nextToken();
          id = id.substring(1);
          if (id.indexOf("/") > 0 ) {
            System.out.println(id);
            StringTokenizer st = new StringTokenizer(id,"/");
            if (st.countTokens() == 2) {
              id = st.nextToken();
              String tmp = st.nextToken();

              st = new StringTokenizer(tmp,"-");
              if (st.countTokens() == 2) {
                sstart = Integer.valueOf(st.nextToken()).intValue();
                send = Integer.valueOf(st.nextToken()).intValue();
              }

            }
          }
          seq = "";
        } else {
          // We have sequence
          seq = seq + (String)lineArray.elementAt(i);
        }
      }
    }
    if (flag == true) {
      if (sstart != 0) {
        seqs.addElement(new AlignSequence(id,seq,sstart,send));
      } else {
        seqs.addElement(new AlignSequence(id,seq,1,seq.length()));
      }
    }
  }
  public static String print(AlignSequenceI[] s) {
    return print(s,72);
  }
  public static String print(AlignSequenceI[] s, int len) {
    return print(s,len,true);
  }
  public static String print(AlignSequenceI[] s, int len,boolean gaps) {
    StringBuffer out = new StringBuffer();
    int i = 0;
    while (i < s.length && s[i] != null) {
      String seq = "";
      if (gaps) {
        seq = s[i].getResidues();
      } else {
        seq = AlignSeq.extractGaps(s[i].getResidues(),"-");
        seq = AlignSeq.extractGaps(seq,".");
        seq = AlignSeq.extractGaps(seq," ");
      }


      out.append(">" + s[i].getName() + "/" + s[i].getStart() + "-" + s[i].getEnd() + "\n");

      int nochunks = seq.length() / len + 1;

      for (int j = 0; j < nochunks; j++) {
        int start = j*len;
        int end = start + len;

        if (end < seq.length()) {
          out.append(seq.substring(start,end) + "\n");
        } else if (start < seq.length()) {
          out.append(seq.substring(start) + "\n");
        }
      }
      i++;
    }
    return out.toString();
  }

  public static void main(String[] args) {
    String inStr = ">LCAT_MOUSE_90.35\nMGLPGSPWQRVLLLLGLLLPPATPFWLLNVLFPPHTTPKAELSNHTRPVILVPGCLGNRLEAKLDKPDVVNW\nMCYRKTEDFFTIWLDFNLFLPLGVDCWIDNTRIVYNHSSGRVSNAPGVQIRVPGFGKTESVEYVDDNKLAGY\n\n>LCAT_PAPAN_95.78\nMGPPGSPWQWVPLLLGLLLPPAAPFWLLNVLFPPHTTPKAELSNHTRPVILVPGCLGNQLEAKLDKPDVVNW\nMCYRKTEDFFTIWLDLNMFLPLGVDCWIDNTRVVYNRSSGLVSNAPGVQIRVPGFGKTYSVEYLDSSKLAGY\nLHTLVQNLVNNGYVRDETVRAAPYDWRLEPGQQEEYYHKLAGLVEEMHAAYGKPVFLIGHSLGCLHLLYFLL\n";
    FastaFile fa = new FastaFile(inStr);
  }
  public String print() {
    return print(getSeqsAsArray());
  } 
}



