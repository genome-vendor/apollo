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

import java.applet.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;

import jalview.datamodel.*;
import jalview.gui.event.*;
import javax.swing.*;

public class IdPanel extends JPanel implements MouseListener, MouseMotionListener {

  protected IdCanvas       idCanvas;

  protected int            offy;
  public    int            width;
  public    int            lastid;

  protected AlignViewport  av;
  protected Controller     controller;

  public IdPanel(AlignViewport av,Controller c) {
    this.av         = av;
    this.controller = c;

    componentInit();
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  private void componentInit() {
    idCanvas = new IdCanvas(av,controller);

    setLayout(new BorderLayout());
    add("Center",idCanvas);
    idCanvas.addMouseListener(this);
    idCanvas.addMouseMotionListener(this);
  }

  public Dimension minimumSize() {
    return idCanvas.minimumSize();
  }
  public Dimension preferredSize() {
    return idCanvas.preferredSize();
  }


  public void mouseMoved(MouseEvent e) {}

  public void mouseDragged(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();

    try{

// SMJS Was this but usually is getIndex      int seq = (y)/av.getCharHeight() + av.getStartSeq();
      int seq = av.getIndex(y);
      if (seq != lastid) {
        DrawableSequence pickedSeq = av.getAlignment().getDrawableSequenceAt(seq);
        controller.handleStatusEvent(new StatusEvent(this,"Sequence ID : " + pickedSeq.getName() +  " (" + seq + ")", StatusEvent.INFO));
	if (av.getSelection().contains(pickedSeq)) {
	  System.out.println("Selected " + seq);
	  av.getSelection().removeElement(pickedSeq);	    
          fireSequenceSelectionEvent(av.getSelection());
	} else {
	  av.getSelection().addElement(pickedSeq);
          fireSequenceSelectionEvent(av.getSelection());
	}
	
	idCanvas.paintFlag = true;
	idCanvas.repaint();
      }
      lastid = seq;
    } catch (Exception ex) {
    }
    return;
  }
      
  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }

  private void fireSequenceSelectionEvent(Selection sel) {
    controller.handleSequenceSelectionEvent(new SequenceSelectionEvent(this,sel));
  }

  public void mousePressed(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();

    int seq = av.getIndex(y);
    if (Config.DEBUG)
      System.out.println("Y = " + y + " " + seq + " " + av.getAlignment().getDrawableSequenceAt(seq).getName());

    try {
      if ((e.getModifiers() & Event.META_MASK) != 0) {
        try {	
          if (seq != -1) {
            String id = av.getAlignment().getDrawableSequenceAt(seq).getName();
            if ( id.indexOf("/") != -1) {
              id = id.substring(0,id.indexOf("/"));
            }
          }
          System.out.println("NOTE: META_MASK mouse press commented out");
	  
//	  URL u = new URL("http://" + af.srsServer + "wgetz?-e+[" + af.database + "-id:" + 
//			  id + "]");
	  
//      	  if (af != null && af.parent instanceof Applet) {
//      	    Applet app = (Applet)af.parent;
//      	    app.getAppletContext().showDocument(u,"entry");
//      	  } else {
//	    if (af != null && af.browser != null) {
//	      if (af.browser.bf != null) {
//		System.out.println("browser exists");
//		af.browser.bf.tf.setText(u.toString());
//		af.browser.pages.addElement(u.toString());
//		af.browser.position = af.browser.pages.size()-1;
//		if (af.browser.pages.size() > 1) {
//		  af.browser.bf.back.enable();
//		}
//		af.browser.connect(af.browser.split(u.toString()));
//	      } else {
//		System.out.println("browser null");
//		af.browser = new SimpleBrowser(u.toString());
//	      }
//	    } else if (af != null) {
//	      af.browser = new SimpleBrowser(u.toString());
//	    }
//	  }
          System.out.println("NOTE: Browser code commented out");
      	} catch (Exception ex) {
      	  System.out.println("Exception : " + ex);
      	}
      } else {
        lastid = seq;
        if (seq != -1) {
          DrawableSequence pickedSeq = av.getAlignment().getDrawableSequenceAt(seq);
          controller.handleStatusEvent(new StatusEvent(this,"Sequence ID : " + pickedSeq.getName() +  " (" + seq + ")", StatusEvent.INFO));

          if (av.getSelection().contains(pickedSeq)) {
            av.getSelection().removeElement(pickedSeq);
             fireSequenceSelectionEvent(av.getSelection());
            
          } else {
            av.getSelection().addElement(pickedSeq);
             fireSequenceSelectionEvent(av.getSelection());
          }
        }
        //idCanvas.paintFlag = true;
        //idCanvas.repaint();
      }
    } catch (Exception ex) {
    }
    return;
  }
  public void mouseReleased(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
//    if (alignPanel.seqPanel.seqCanvas.colourSelected) {
//	alignPanel.seqPanel.seqCanvas.paintFlag = true;
//	alignPanel.seqPanel.seqCanvas.repaint();
//    }
    if (Config.DEBUG) 
      System.out.println("NOTE: IdPanel mouseReleased code commented out");
    return;
  }
}
