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
import java.awt.*;
import java.util.Vector;
import jalview.gui.*;

public class MSPSequence extends DrawableSequence {

  int qstart;
  int qend;
  int score;
  String frame;
  String database;

  public MSPSequence(String id,String seq, int hstart, int hend, int qstart, int qend, int score,
                     String frame,String database) {
    super(id,seq,hstart,hend);
    if (qstart != 1) {
      setResidues(getPads(qstart-1) + getResidues());
      setNums(getResidues());
    }
    this.qstart = qstart;
    this.qend = qend;
    this.score = score;
    this.frame = frame;
  }

  // This should be modified
  public MSPSequence(AlignSequenceI s) {
    super(s);
    this.qstart = 1;
    this.qend = getLength();
    this.score = 0;
    this.frame = "";
  }
  public static String getPads(int n, String s) {
    String tmp = "";
    for (int i=0; i < n ;i++) {
      tmp = tmp + s;
    }
    return tmp;
  }

  private String getPads(int n) {
    String tmp = "";

    for (int i=0; i < n ;i++) {
      tmp = tmp + " ";
    }
    return tmp;
  }

  public int getQStart() {
    return qstart;
  }
  public int getQEnd() {
    return qend;
  }

  public String getFrame() {
    return frame;
  }

  public int getScore() {
    return score;
  }
}
