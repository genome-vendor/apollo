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
import jalview.gui.AlignFrame;

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.Font;

public class MSPDriver {
  public static void main(String[] args) {
    try {
      MSPFile msp = new MSPFile(args[0],"File");
      MSPSequence[] s = new MSPSequence[msp.seqs.size()];

//MG      msp.extractSeqs(5,20,true);
      for (int i=0; i < msp.seqs.size(); i++) {
        s[i] = (MSPSequence)msp.seqs.elementAt(i);
      }
      String out = msp.print(s);
      AlignFrame af = new AlignFrame(s);
      af.resize(700,500);
      af.show();
//MG      ConsThread ct = new ConsThread(af);
//MG      ct.start();
      System.out.println(out);
      System.out.println("done");
    } catch (IOException e) {
      System.out.println(e);
    }
  }
}
