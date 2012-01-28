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
package apollo.seq.io;

import java.io.*;
import java.util.*;
import apollo.io.*;
import apollo.datamodel.*;
import apollo.gui.*;
import java.awt.*;

public class GFFFile extends FileParse {
  int noSeqs;
  int maxLength = 0;

  public Vector seqs;       //Vector of SequenceElements

  long start;
  long end;

  public GFFFile(DataInputStream in) {
    seqs	= new Vector();
    String data;
    lineArray = new Vector();
    try {
      while ((data = in.readLine()) != null) {
        lineArray.addElement(data);
      }
    } catch (IOException ioex) {
      System.out.println("Exception " + ioex);
    }
    parse();
  }

  public GFFFile(String inStr) {
    seqs    = new Vector();
    readLines(inStr);
    parse();
  }
  public FeatureSetI getFeatures() {
    FeatureSet fset = new FeatureSet();

    for (int i = 0; i < seqs.size(); i++) {
      fset.addFeature((SeqFeatureI)seqs.elementAt(i));
    }
    return fset;
  }
  public GFFFile(String inFile, String type) throws IOException {
    //Read in the file first
    super(inFile,type);

    seqs   = new Vector();

    //Read lines from file
    System.out.println("Reading Ensembl-style GFF file " + inFile + "....");
    start = System.currentTimeMillis();
    readLines();
    end = System.currentTimeMillis();
    //    System.out.println("done");
    //System.out.println("Total time taken = " + (end-start) + "ms");

    //    System.out.println("Parsing file....");
    start = System.currentTimeMillis();
    parse();
  }

  public void parse() {

    for (int i = 0; i < lineArray.size(); i++) {
      String line = (String)lineArray.elementAt(i);

      if (line.indexOf("#") == -1 ) {

        // SMJS Added delimiter argument to call. GFF has 8 tab delimited fields.
        //      The last field is a fairly free text format field, which needs
        //      special handling.

        StringTokenizer st = new StringTokenizer(line,"\t");

        if (st.countTokens() >= 8) {
          try {
            String s      = st.nextToken();
            String type   = st.nextToken();
            String prim   = st.nextToken();

            int    qstart = Integer.parseInt(st.nextToken());
            int    qend   = Integer.parseInt(st.nextToken());
            double score  = 0;
            try {
              score  = (Double.valueOf(st.nextToken())).doubleValue();
            } catch (Exception e) {
              System.out.println("Error parsing score : " + e);
            }
            String strand = st.nextToken();
            String frame  = st.nextToken();
            String id     = prim;

            SeqFeatureI se = new SeqFeature(qstart,qend,id);

            if (strand.equals("-")) {
              se.setStrand(-1);
            } else {
              se.setStrand(1);
            }

            se.setFeatureType(type);
            se.setScore(score);
            se.setName(s);
            se.setId(s);
            if (!(frame.equals("."))) {
              se.setPhase(Integer.parseInt(frame));
            } else {
              // se.setPhase(-1);
            }

            if (st.hasMoreTokens() && prim.equals("similarity")) {
              try {
                // SMJS Get remainder of string by setting
                // delimiter to nothing ("")
                String htok   = st.nextToken("");
                // System.out.println("htok = " + htok);
                // SMJS Setup a new tokenizer which doesn't require tabs
                StringTokenizer sth = new StringTokenizer(htok);
  
                String hid    = sth.nextToken();
                int    hstart = Integer.parseInt(sth.nextToken());
                int    hend   = Integer.parseInt(sth.nextToken());
                int hitStrand = 0;
                SeqFeatureI f2 = null;
                
                if (hstart < hend){
                  hitStrand = 1;
                  f2 = new SeqFeature(hstart, hend, hid, hitStrand);
                }else{
                  hitStrand = -1;
                  f2 = new SeqFeature(hend, hstart, hid, hitStrand);
                }//end if
  
                f2.setName(hid);
                f2.setId(hid);
  
                se.setId(hid);
                FeaturePair fp = new FeaturePair(se,f2);
  
                seqs.addElement(fp);
              } catch (Exception e) {
                System.out.println("Can't add line - " + line + " " + e);
              }
            } else if (st.hasMoreTokens() && prim.equals("exon")) {

              // SMJS Modified for BDGP GFF file (get rest of string
              String hid   = st.nextToken("\t");

              se.setName(hid);
              se.setId(hid);
              // Try Id instead of name
              seqs.addElement(se);
            } else {
              if (!(prim.equals("intron") ||
                    prim.equals("sequence") ||
                    prim.equals("coding_exon"))) {
                seqs.addElement(se);
              }
            }

          } catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException " + nfe);
            System.out.println("ERROR: parsing line " + line);
          }
        }
      }
    }

    noSeqs = seqs.size();

  }
  // virtual_contig_Z97653.00001.12521.1.138272.41262        blastp  similarity      558     763     171.0000        +       .       SW:HT2A_HUMAN   575     643
  public static String print(FeatureSetI set) {
    StringBuffer out = new StringBuffer();

    for (int i=0; i< set.size(); i++) {
      SeqFeatureI sf = set.getFeatureAt(i);
      if (sf.canHaveChildren()) {
        FeatureSetI fs = (FeatureSetI)sf;
        out.append(print(fs));
      } else if (sf instanceof FeaturePair) {

        FeaturePair fp = (FeaturePair)sf;
        String strand;

        strand = ".";
        if (fp.getStrand() == 1) {
          strand = "+";
        } else if (fp.getStrand() == -1) {
          strand = "-";
        }
        // NOTE: sets primitive type to similarity and phase to .
        out.append(fp.getId() + "\t" + fp.getFeatureType() + "\tsimilarity\t" + fp.getLow() + "\t" +
                   fp.getHigh() + "\t" + fp.getScore() + "\t" + strand + "\t.\t" +
                   fp.getHname() + "\t" + fp.getHstart() + "\t" + fp.getHend() + "\n");
      } else {
        String strand;

        strand = ".";
        if (sf.getStrand() == 1) {
          strand = "+";
        } else if (sf.getStrand() == -1) {
          strand = "-";
        }
// NOTE: sets primitive type to similarity and phase to .
	String type = sf.getFeatureType();
	if (type.equals(""))
	  type = "unknown_type";

        out.append(sf.getId() + "\t" + type + "\texon\t" + sf.getLow() + "\t" +
                   sf.getHigh() + "\t" + sf.getScore() + "\t" + strand + "\t.");
        if (sf.getRefId() != null) {
          out.append("\t" + sf.getRefId() + "\n");
        }
	else
	  out.append("\n");
      }
    }
    return out.toString();
  }
}
