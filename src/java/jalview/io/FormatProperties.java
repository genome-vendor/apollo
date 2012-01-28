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

import java.util.*;

public class FormatProperties {

  static final int FASTA   = 0;
  static final int MSF     = 1;
  static final int CLUSTAL = 2;
  static final int BLC     = 3;
  static final int PIR     = 4;
  static final int MSP     = 5;
  static final int PFAM    = 6;
  static final int POSTAL  = 7;
  static final int JNET    = 8;

  static FormatPropertyVector formats = new FormatPropertyVector();

  static {
    String prefix = getDefaultClassPrefix();

    formats.add("FASTA",  prefix + "FastaFile");
    formats.add("MSF",    prefix + "MSFfile");
    formats.add("CLUSTAL",prefix + "ClustalFile");
    formats.add("BLC",    prefix + "BLCFile");
    formats.add("PIR",    prefix + "PIRFile");
    formats.add("MSP",    prefix + "MSPFile");
    formats.add("PFAM",   prefix + "PfamFile");
    formats.add("POSTAL", prefix + "PostalFile");
    formats.add("JNET",   prefix + "JnetFile");
  }

  public static String getDefaultClassPrefix() {
    return "jalview.io.";
  }

  static int indexOf(String format) {

    if (format != null) {
       format.toUpperCase();
       if (formats.contains(format)) {
         return formats.indexOf(format);
       }
    }
    return -1;
  }

  public static String getClassName(int index) {
    return formats.getClassName(index);
  }

  public static boolean contains(String format) {

    if (format != null) {
      format.toUpperCase();
  
      if (formats.contains(format)) {
        return true;
      }
    }
    return false;
  } 

  public static Vector getFormatNames() {
    return formats.getFormatNames();
  }

  public static Vector getFormats() {
    return formats.getFormatNames();
  }
}
