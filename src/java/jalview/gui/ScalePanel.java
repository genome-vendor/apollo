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
import java.awt.event.*;

import jalview.gui.event.*;
import jalview.datamodel.*;
import javax.swing.*;

public class ScalePanel extends JPanel implements MouseListener {

  protected ScaleCanvas scaleCanvas;

  protected int offy;
  public    int width;

  protected AlignViewport av;
  protected Controller    controller;

  public ScalePanel(AlignViewport av,Controller c) {
    this.av         = av;
    this.controller = c;

    componentInit();

    //System.out.println("Loaded ScalePanel");
  }

  private void componentInit() {
    scaleCanvas = new ScaleCanvas(av,controller);
    setLayout(new BorderLayout());
    add("Center",scaleCanvas);
    addMouseListener(this);
    scaleCanvas.addMouseListener(this);
  }

  void setMapper(Mapper mapper) { scaleCanvas.setMapper(mapper); }

  public Dimension minimumSize() {
    return scaleCanvas.minimumSize();
  }

  public Dimension preferredSize() {
    return scaleCanvas.preferredSize();
  }

  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }
  public void mouseClicked(MouseEvent evt) { }
  public void mousePressed(MouseEvent evt) { 
    int x = evt.getX();
    int y = evt.getY();

    int res = x/av.getCharWidth() + av.getStartRes();
 
    boolean found = false;
 
    if (Config.DEBUG) System.out.println("Selected column = " + res);
 
    if (! av.getColumnSelection().contains(res)) {
      av.getColumnSelection().addElement(res);
    } else {
      av.getColumnSelection().removeElement(res);
    }

    controller.handleColumnSelectionEvent(new ColumnSelectionEvent(this,av.getColumnSelection()));
 
 
    return;
  }
  public void mouseReleased(MouseEvent evt) { }
}
