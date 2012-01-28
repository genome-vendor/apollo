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
import jalview.gui.DrawableSequence;

import java.util.*;
import java.io.*;

public class FormatAdapter {
 
  public static String get(String format,Vector seqs) {
    AlignSequenceI [] s = new AlignSequenceI[seqs.size()];
    for (int i=0;i<seqs.size(); i++) {
      s[i] = (AlignSequenceI)seqs.elementAt(i);
    }
    if (FormatProperties.contains(format)) {
      AlignFile afile = FormatFactory.get(format,s);
      return afile.print();
    } else {
      // Should throw exception here
      return null;
    }
  }

  public static String get(String format,AlignSequenceI[] s) {
    if (FormatProperties.contains(format)) {
      AlignFile afile = FormatFactory.get(format,s);
      return afile.print();
    } else {
      // Should throw exception here
      return null;
    }
  }

  public static AlignSequenceI[] read(String format,String inStr) {
    if (FormatProperties.contains(format)) {
      AlignFile afile = FormatFactory.get(format,inStr);
      return afile.getSeqsAsArray();
    } else {
    // Should throw exception
      return null;
    }
  }

  public static AlignSequenceI[] read(String inFile, String type, String format) {
    System.out.println("In FormatAdapter: " + inFile + " " + type + " " + format);
    if (FormatProperties.contains(format)) {
      try {
        AlignFile afile = FormatFactory.get(format,inFile,type);
        return afile.getSeqsAsArray();
      } catch (Exception e) {
	System.out.println("Exception " + e + " in FormatAdapter");
      }
    } else {
      // Should throw exception
      return null;
    }
    return null;
  }

  public static DrawableSequence[] toDrawableSequence(AlignSequenceI[] s) {
    System.out.println("In FormatAdapter " + s.length);
    DrawableSequence[] ds = new DrawableSequence[s.length];
    int i=0;
    while (i < ds.length && s[i] != null) {
      ds[i] = new DrawableSequence(s[i]);
      i++;
    }
    return ds;
  }

  public static void main(String[] args) {
    AlignSequenceI[] s  = FormatAdapter.read(args[0],"File",args[1]);

    System.out.println(FormatAdapter.get(args[2],s));
  }
}
