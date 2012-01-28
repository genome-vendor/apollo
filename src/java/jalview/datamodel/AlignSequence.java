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

package jalview.datamodel;

import java.util.Vector;
import jalview.gui.*;
import jalview.io.*;
import jalview.analysis.*;
import jalview.gui.schemes.*;
import apollo.datamodel.SeqFeatureI;

public class AlignSequence extends apollo.datamodel.Sequence 
  implements AlignSequenceI {
  protected int      start;
  protected int      end;
  protected int[]    num;
  protected Vector   features;
  protected Vector[] score;
  protected Vector   pdbcode;
  protected SequenceFeatureSourceI sfs;
  protected int      charHeight;
  
  public AlignSequence(String name, 
		       String sequence,
		       int start, int end) {
    super (name, sequence);
    this.start    = start;
    this.end      = end;
    this.score    = new Vector[10];
    this.num      = new int[sequence.length()];
    pdbcode       = new Vector();
    num           = setNums(sequence);
    // AbstractSequence assumes sequence is one based and shifts it to zero based
    // for string ops. jalview is zero based, so this tells AbstractSequence not to
    // do the shift
    super.setNeedShiftFromOneToZeroBasedCoords(false);
  }

  /** Overrides AbstractSequence which does one based check. This does zero based 
      check. */
  protected int pegLimits(int value) {
    if (value < 0) return 0;
    if (value >= getLength()) return getLength() - 1; 
    return value; // if in range just return value
  }

  public String getDisplayId() {
    return (super.getName() + ":" + start + "-" + end);
  }

  //public String getName () {return getDisplayId();}

  public void setStart(int start) {
    this.start = start;
  }

  public int getStart() {
    return this.start;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public int getEnd() {
    return this.end;
  }

  public void addPDBCode(String code) {
    if (!pdbcode.contains(code)) {
      this.pdbcode.addElement(code);
    }
  }

  public Vector getPDBCodes() {
    return this.pdbcode;
  }

  public Vector[] getScores() {
    return this.score;
  }

  public Vector getScoresAt(int i) {
    return score[i];
  }

  public void addFeature(SeqFeatureI sf ) {
    if (features == null) features = new Vector();
    if (!features.contains(sf)) {
      features.addElement(sf);
    }
  }

  public Vector getFeatures() {
    return this.features;
  }

  public int findIndex(int pos) {
    // returns the alignment position for a residue
    int j = start;
    int i = 0;

    while (i< getLength() && j <= end && j <= pos) {
      if (!residueIsSpacer (i))
	j++;
      i++;
    }
    if (j == end && j < pos) {
      return end+1;
    } else {

      return i;
    }
  }

  public boolean residueIsSpacer(int position) {
    char c = getBaseAt(position);
    return (c == '.' || c == '-' || c == ' ');
  }

  public int findPosition(int i) {
    // Returns the sequence position for an alignment position
    int j   = 0;
    int pos = start;

    while (j < i) {
      if (!residueIsSpacer(j))
        pos++;
      j++;
    }
    return pos;
  }

  public void setScore(int i, float s,int num) {
    if (num < 10 && score[num] == null) {
      score[num] = new Vector();
    } else {
      System.out.println("ERROR: maximum number of scores = 10 " + num);
    }
    if (score[num].size() <= i) {
      for(int j= score[num].size(); j <= i; j++) {
        score[num].addElement(null);
      }
    }
    score[num].setElementAt(new Double(s),i);
  }


  public void getFeatures(String server, String database) {
    System.out.println("getFeatures with server = " + server + " database " + database);
    if (features == null) {
      try {
        String id = getDisplayId();
	if (id.indexOf(":") > 0)
	  id = id.substring(0, id.indexOf(":"));

        sfs = new SwissprotFile("http://" + server + "wgetz?-e+[" + database + "-id:" + id + "]","URL");

        if (sfs == null || sfs.getSequence() == null) {
          sfs = new SwissprotFile("http://" + server + "wgetz?-e+[" + database + "-acc:" + id + "]","URL");
        }
	String seq_str = getResidues();
        if (sfs != null && sfs.getSequence() != null) {
          String ungap = AlignSeq.extractChars(". -", seq_str);
          System.out.println(ungap);
          System.out.println(sfs.getSequenceString());
          System.out.println(start + " " + end);
          System.out.println(sfs.getSequenceString().indexOf(ungap));
          if (sfs.getSequenceString().indexOf(ungap) != (start-1)) {
            start = sfs.getSequenceString().indexOf(ungap)+1;
            end = start + getLength() - 1;
          }
          if (!(sfs.getId().equals(""))) {
            System.out.println("Fetched features for " + getDisplayId());
            this.features = sfs.getFeatures();
            this.pdbcode = sfs.getPDBCode();
          }
        }
      } catch (java.io.IOException e) {
        System.out.println("Exception in fetching features " + e);
      }
    }
  }


  public void deleteCharAt(int i,int n) {
    if (score[n].size() > i) {
      score[n].removeElementAt(i);
    }
  }


  public void insertCharAt(int i,char c, int n) {
    if (score[n] != null) {
      for (int j=score[n].size(); j < i; j++) {
        score[n].addElement(new Double(0));
      }
      score[n].insertElementAt(new Double(0),i);
    }

  }

  public void deleteCharAt(int i) {
    String seq_str = getResidues();
    if ((i+1) < getLength()) {
      setResidues(seq_str.substring(0,i) + seq_str.substring(i+1));
    } else {
      setResidues(seq_str.substring(0,i));
    }


    int[] tmp = new int[num.length];

    for (int j = 0; j < i; j++) {
      tmp[j] = num[j];
    }

    for (int j=i+1;j < num.length; j++) {
      tmp[j-1] = num[j];
    }

    num[num.length-1] = 23;
    num               = tmp;

    int j = 0;

    while (j < score.length && score[j] != null) {
      deleteCharAt(i,j);
      j++;
    }
  }


  public void insertCharAt(int i, char c) {
    insertCharAt(i,c,true);
  }


  public void insertCharAt(int i,char c,boolean chop) {

    // Insert the char into the sequence string
    String tmp = new String(getResidues());

    if (i < getLength()) {
      setResidues(tmp.substring(0,i) + String.valueOf(c) + tmp.substring(i));
    } else {
      setResidues(tmp + String.valueOf(c));
    }

    // If this sequence had a gap at end don't increase the length
    int len = num.length+1;
    if (chop == true) {
      String seq_str = getResidues();
      if (residueIsSpacer (seq_str.length() - 1)) {
        if (i < seq_str.length()-1) {
          len = num.length;
          setResidues(seq_str.substring(0,seq_str.length()-1));
        }
      }
    }
    int[]  newnum = new  int[len];
    int j         = 0;

    for (j=0;j<i;j++) {
      newnum[j] = num[j];
    }

    try {
      newnum[j] =  ((Integer)ResidueProperties.aaHash.get(String.valueOf(c))).intValue();
    } catch (Exception e) {
      System.out.println("Exception in insertChar " + c);
      newnum[j] = 23;
    }

    for (j = i+1; j < len;j++) {
      newnum[j] = num[j-1];
    }
    num = newnum;
    j = 0;
    while (j < score.length && score[j] != null) {
      insertCharAt(i,c,j);
      j++;
    }
  }

  public void setScore(int i,float s) {
    int j = 0;
    while (j < score.length && score[j] != null) {
      setScore(i,s,j);
      j++;
    }
  }


  public int[] setNums(String s) {
    int[] out = new int[s.length()];
    int i=0;
    while (i < s.length()) {
      try {
        out[i] = ((Integer)ResidueProperties.aaHash.get(s.substring(i,i+1))).intValue();
      } catch (Exception e) {
        // System.out.println("Exception in Sequence:setNums " + i + " " + s.substring(i,i+1));
        out[i] = 23;
      }
      i++;
    }
    this.num = out;
    return out;
  }

  public int getNum(int i) {
    return num[i];
  }

  public int [] getNums() {
    return num;
  }

  public void setCharHeight(int height) {
    this.charHeight = height;
  }
  public int getCharHeight() {
    return this.charHeight;
  }
}
