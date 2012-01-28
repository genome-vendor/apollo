/* Jalview - a java multiple alignment editor
 * Copyright (C) 1998  Michele Clamp
 *
 *  $Log: ScoreSequence.java,v $
 *  Revision 1.2  2003/05/25 21:14:16  suzi
 *
 *  first use of cigars for multiple alignments in jalview.
 *
 *  Revision 1.1  2003/04/09 10:25:46  mclamp
 *  Added datamodel source
 *
 *  Revision 1.1.1.1  2003/01/06 17:35:53  michele
 *  Imported sources - may not be up to date
 *
 *  Revision 1.2  2001/03/27 08:58:03  searle
 *  Reindented everything (with astyle)
 *
 *  Removed jalview/gui/popups/save
 *
 *  Added reindent targets to Makefile
 *
 *  Added in FormatFactory and ColourSchemeFactory code.
 *
 *  Revision 1.1.1.1  2001/03/26 14:09:21  michele
 *  Imported sources
 *
 *  Revision 1.1.1.1  2001/03/25 19:33:19  michele
 *  Imported sources
 *
 *  Revision 1.1.2.1  1998/11/25 11:05:32  michele
 *  Added files created for the ScorePanel/BigPanel/clustalx rewrite
 *
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

import MCview.*;
import java.awt.*;
import java.util.Vector;
import jalview.gui.DrawableSequence;

public class ScoreSequence extends DrawableSequence {
  double      max;
  double      min;
  int         scoreno = 0;
  int         red     = 255;
  int         green   = 255;
  int         blue    = 255;
  Color       c;

  public ScoreSequence(AlignSequenceI s, int num, int red, int green, int blue) {
    super(s);

    this.red   = red;
    this.green = green;
    this.blue  = blue;

    c = new Color(red,green,blue);

    scoreno = num;

    findMaxMin();

  }


  public ScoreSequence(AlignSequenceI s , int num) {
    this(s,num,1,1,1);
  }
  public ScoreSequence(AlignSequenceI s) {
    this(s,0,1,1,1);
  }

  public ScoreSequence(String name,String sequence, int start, int end) {
    super(name,sequence,start,end);
    scoreno = 0;
    findMaxMin();
  }

  public void findMaxMin() {
    int i = 0;

    Vector[] score = getScores();

    max = -10000.0;
    min = 10000.0;

    while ( score[scoreno] != null && i < score[scoreno].size()) {
      double s = ((Double)score[scoreno].elementAt(i)).doubleValue();
      if (s > max)
        max = s;
      if (s < min)
        min = s;
      i++;
    }
    System.out.println("Score max/min " + max + " " + min);
  }

  public void drawSequence(Graphics g,int start, int end, int x1, int y1, int width, int height,boolean showScores) {
    drawScores(g,start,end,x1,y1,width,height);
  }

  public void drawScores(Graphics g, int start, int end, int x1, int y1, int width, int height) {
    int i = start;

    Vector[] score = getScores();

    while (i < end && i < score[scoreno].size()) {

      double s1 = ((Double)score[scoreno].elementAt(i)).doubleValue();
      double yd1= (s1-min)/(max - min);
      int yy1 = (int)(yd1*height);
      //   Color c = (Color)ResidueProperties.scaleColours.elementAt((int)((yd1)*9));
      //  System.out.println(yd1);
      //       Color c = new Color((int)(yd1*155+100)*red,(int)(yd1*155+100)*green,(int)(yd1*155+100)*blue);

      if (c != Color.white) {
        g.setColor(c);
        g.fillRect(x1+(i-start)*width,y1+height-yy1,width,yy1);
      }
      i++;
    }
  }
}
