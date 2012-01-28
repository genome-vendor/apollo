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

/**
 * A Base class for alignment file parser classes.
 */
public abstract class AlignFile extends FileParse {
  int noSeqs    = 0;
  int maxLength = 0;

  Hashtable myHash;
  Vector    seqs;
  Vector    headers;

  long start;
  long end;

/**
 * Constructor which parses the data from a String, rather than a file.
 * @param inStr String containing data formatted in the alignment file format.
 */
  public AlignFile(String inStr) {
    initData();

    readLines(inStr);

    System.out.println(noLines);
    System.out.println(lineArray.size());

    parse();
  }

/**
 * This Constructor is used when an AlignFile is used for formatted printing.
 * It is slightly odd to set the sequences on an AlignFile. The printing
 * functionality should maybe be split away from this class.
 */
  public AlignFile(AlignSequenceI [] s) {
    initData();
    setSeqs(s);
  }

/**
 * Constructor which parses the data from a file of some specified type.
 * @param inFile Filename to read from.
 * @param type   What type of file to read from (File, URL)
 */
  public AlignFile(String inFile, String type) throws IOException {
    //Read in the file first
    super(inFile,type);
 
    initData();
 
    //Read lines from file
    System.out.print("Reading file....");
    start = System.currentTimeMillis();

    readLines();

    end = System.currentTimeMillis();
    System.out.println("done");
    System.out.println("Total time taken = " + (end-start) + "ms");
 
    System.out.println("Parsing file....");
    start = System.currentTimeMillis();

    parse();                                   

    end = System.currentTimeMillis();
    System.out.println("Total time taken = " + (end-start) + "ms");
  }

/**
 * Return the seqs Vector
 */
  public Vector getSeqs() {
    return seqs;
  }

/**
 * Return the Sequences in the seqs Vector as an array of Sequences
 */
  public AlignSequenceI [] getSeqsAsArray() {
    AlignSequenceI [] s = new AlignSequenceI[seqs.size()];
    for (int i=0;i < seqs.size();i++) {
      s[i] = (AlignSequenceI)seqs.elementAt(i);
    }
    return s;
  }


/**
 * Initialise objects to store sequence data in.
 */
  protected void initData() {
    seqs    = new Vector();
    headers = new Vector();
    myHash  = new Hashtable();
  }

  protected void setSeqs(AlignSequenceI [] s) {
    for (int i=0; i<s.length; i++) {
      seqs.addElement(s[i]);
    }
  }

/**
 * This method must be implemented to parse the contents of the file.
 */
  public abstract void parse();


/**
 * Print out in alignment file format the Sequences in the seqs Vector.
 */
  public abstract String print();
}
