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

package jalview.gui;

import java.awt.*;
import java.util.*;

import jalview.gui.event.*;
import jalview.datamodel.*;
import javax.swing.*;


public class ScaleCanvas extends ControlledCanvas implements AlignViewportListener,
                                                   ColumnSelectionListener,
                                                   FontChangeListener {

  Image    img;
  Graphics gg;

  int      imgWidth;
  int      imgHeight;
  int      xoffset;
  private Mapper mapper;

  public static final int HEIGHT = 30;

  boolean paintFlag = false;

  protected AlignViewport av;

  public ScaleCanvas(AlignViewport av,Controller c) {
    this.av         = av;
 
    setController(c);
  }

  void setMapper(Mapper mapper) { this.mapper = mapper; }

  public boolean handleAlignViewportEvent(AlignViewportEvent e) {
    repaint();
    return true;
  }
  public boolean handleColumnSelectionEvent(ColumnSelectionEvent e) {
    paintFlag = true;
    repaint();
    return true;
  }
  public boolean handleFontChangeEvent(FontChangeEvent e) {
    paintFlag = true;
    repaint();
    return true;
  }
  public void paint(Graphics g) {

    int charWidth  = av.getCharWidth();
    int charHeight = av.getCharHeight();

    Font f = av.getFont();


    if (img == null ||
        imgWidth  != size().width  ||
        imgHeight != size().height ||
        paintFlag == true) {

      imgWidth  = size().width;
      imgHeight = size().height;
      img       = createImage(imgWidth,imgHeight);

      gg = img.getGraphics();
      gg.setColor(Color.white);
      gg.fillRect(0,0,imgWidth,imgHeight);



      paintFlag = false;
    }
    gg.setFont(f);

    //Fill in the background

    gg.setColor(Color.white);
    gg.fillRect(0,0,imgWidth,imgHeight);


    //Set the text font

    gg.setColor(Color.black);

    int startx      = av.getStartRes();
    int endx        = av.getEndRes();
    int scalestartx = startx - startx%10 + 10;

    // Draw the scale numbers

    gg.setColor(Color.black);

    for (int i=scalestartx;i < endx;i+= 10) {
      if (mapper != null) {
      int coord = mapper.condensed2genomic(i);
      // in intron coord is -1 so skip it
      if (coord == -1) continue;

      // draw tick
      int tickX = (i-startx-1)*charWidth + charWidth/2; // middle of char
      gg.drawLine(tickX,30-charHeight,tickX,30);
      // draw number
      String string = String.valueOf(coord);
      int halfNumberWidth = (string.length()*charWidth)/2;
      int numberX = tickX - halfNumberWidth; // center number on tick 
      gg.drawString(string,numberX,15);
      }

    }

    //Fill the selected columns
    ColumnSelection cs = av.getColumnSelection();
 
    for (int i=0; i<cs.size(); i++) {
      int sel  = cs.columnAt(i);
      //      System.out.println("Selection = " + sel);
      if ( sel >= startx  && sel <= endx) {
        gg.setColor(Color.red);
        gg.fillRect((sel-startx)*charWidth,30-charHeight,charWidth,charHeight);
      }
    }

    g.drawImage(img,0,0,this);

  }

  public void update(Graphics g) {
    paint(g);
  }

  public Dimension minimumSize() {
    return new Dimension(500,HEIGHT);
  }

  public Dimension preferredSize() {
    return minimumSize();
  }

}

