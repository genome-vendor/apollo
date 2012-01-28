package jalview.gui;

import jalview.analysis.*;
import jalview.io.*;
import jalview.datamodel.*;
import jalview.gui.popups.*;
import jalview.gui.event.*;
import jalview.gui.schemes.*;
import jalview.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class TreeCanvas extends Panel implements MouseListener,
						 RubberbandListener,
						 SequenceSelectionListener {


  NJTree tree;
  Object parent;

  Font font;
  int  fontSize = 12;

  boolean showDistances = false;
  boolean showBootstrap = false;

  int offx = 20;
  int offy = 20;

  int threshold;

  RubberbandRectangle rubberband;

  Selection selected;
  Vector    listeners;

  Hashtable nameHash = new Hashtable();
  Hashtable nodeHash = new Hashtable();

  public TreeCanvas(Object parent, NJTree tree, Selection selected) {
    this.tree     = tree;
    this.parent   = parent;
    this.selected = selected;

    treeInit();
  }
    public void setSelected(Selection selected) {
	this.selected = selected;
    }
  public void treeInit() {
      addMouseListener(this);
    tree.findHeight(tree.getTopNode());

    setBackground(Color.white);

  }

    public void drawNode(Graphics g,SequenceNode node, float chunk, float scale, int width,int offx, int offy) {
    if (node == null) {
      return;
    }

    if (node.left() == null && node.right() == null) {
      // Drawing leaf node

      float height = node.height;
      float dist   = node.dist;

      int xstart = (int)((height-dist)*scale) + offx;
      int xend =   (int)(height*scale)        + offx;

      int ypos = (int)(node.ycount * chunk) + offy;

      if (node.element() instanceof DrawableSequence) {
	  g.setColor(((DrawableSequence)((SequenceNode)node).element()).getColor().darker());
      } else {
	  g.setColor(Color.black);
      }

      // Draw horizontal line
      g.drawLine(xstart,ypos,xend,ypos);

      String nodeLabel = "";
      if (showDistances && node.dist > 0) {
        nodeLabel = new Format("%5.2f").form(node.dist);
      }
      if (showBootstrap) {
        if (showDistances) {
          nodeLabel = nodeLabel + " : ";
        }
        nodeLabel = nodeLabel + String.valueOf(node.getBootstrap());
      }
      if (! nodeLabel.equals("")) {
      g.drawString(nodeLabel,xstart,ypos - 10);
      }

      // Colour selected leaves differently
      String name    = node.getName();
      FontMetrics fm = g.getFontMetrics(font);
      int charWidth  = fm.stringWidth(node.getName()) + 3;
      int charHeight = fm.getHeight();

      Rectangle rect = new Rectangle(xend+20,ypos-charHeight,
				     charWidth,charHeight);

      nameHash.put((AlignSequenceI)node.element(),rect);

      if (selected.contains((AlignSequenceI)node.element())) {
        g.setColor(Color.gray);

        g.fillRect(xend + 20, ypos - charHeight + 3,charWidth,charHeight);
        g.setColor(Color.white);
      }
      g.drawString(node.getName(),xend+20,ypos);
      g.setColor(Color.black);
    } else {
      drawNode(g,(SequenceNode)node.left(), chunk,scale,width,offx,offy);
      drawNode(g,(SequenceNode)node.right(),chunk,scale,width,offx,offy);

      float height = node.height;
      float dist   = node.dist;

      int xstart = (int)((height-dist)*scale) + offx;
      int xend   = (int)(height       *scale) + offx;
      int ypos   = (int)(node.ycount  *chunk) + offy;

      g.setColor(((SequenceNode)node).color.darker());

      // Draw horizontal line
      g.drawLine(xstart,ypos,xend,ypos);

      int ystart = (int)(((SequenceNode)node.left()) .ycount * chunk) + offy;
      int yend   = (int)(((SequenceNode)node.right()).ycount * chunk) + offy;

      Rectangle pos = new Rectangle(xend-2,ypos-2,5,5);
      nodeHash.put(node,pos);
      g.drawLine((int)(height*scale) + offx, ystart,
                 (int)(height*scale) + offx, yend);

      if (showDistances && node.dist > 0) {
        g.drawString(new Format("%5.2f").form(node.dist),xstart,ypos - 5);
      }

    }
  }
  public Object findElement(int x, int y) {
       Enumeration keys = nameHash.keys();

    while (keys.hasMoreElements()) {
	    Object ob = keys.nextElement();
	    Rectangle rect = (Rectangle)nameHash.get(ob);

	    if (x >= rect.x && x <= (rect.x + rect.width) &&
	      y >= rect.y && y <= (rect.y + rect.height)) {
	      return ob;
	    }
  }
  keys = nodeHash.keys();

    while (keys.hasMoreElements()) {
	    Object ob = keys.nextElement();
	    Rectangle rect = (Rectangle)nodeHash.get(ob);

	    if (x >= rect.x && x <= (rect.x + rect.width) &&
	      y >= rect.y && y <= (rect.y + rect.height)) {
	      return ob;
	    }
    }
      return null;
  }

  public void pickNodes(Rectangle pickBox, Selection sel) {
    int width  = size().width;
    int height = size().height;

    SequenceNode top = tree.getTopNode();

    float wscale = (float)(width*.8-offx*2)/tree.getMaxHeight()
;
    if (top.count == 0) {
      top.count = ((SequenceNode)top.left()).count + ((SequenceNode)top.right()).count ;
    }
    float chunk = (float)(height-offy*2)/top.count;

    pickNode(pickBox,sel,top,chunk,wscale,width,offx,offy);
  }

  public void pickNode(Rectangle pickBox, Selection sel, SequenceNode node, float chunk, float scale, int width,int offx, int offy) {
    if (node == null) {
      return;
    }

    if (node.left() == null && node.right() == null) {
      float height = node.height;
      float dist   = node.dist;

      int xstart = (int)((height-dist)*scale) + offx;
      int xend   = (int)(height*scale) + offx;

      int ypos = (int)(node.ycount * chunk) + offy;

      if (pickBox.contains(new Point(xend,ypos))) {
        if (node.element() instanceof AlignSequenceI) {
          AlignSequenceI seq = (AlignSequenceI)node.element();
          if (sel.contains(seq)) {
            sel.removeElement(seq);
          } else {
            sel.addElement(seq);
          }
        }
      }
    } else {
      pickNode(pickBox,sel,(SequenceNode)node.left(), chunk,scale,width,offx,offy);
      pickNode(pickBox,sel,(SequenceNode)node.right(),chunk,scale,width,offx,offy);
    }
  }

  public void setColor(SequenceNode node, Color c) {
    if (node == null) {
      return;
    }

    if (node.left() == null && node.right() == null) {
      node.color = c;

      if (node.element() instanceof DrawableSequence) {
	  ((DrawableSequence)node.element()).setColor(c);
      }
    } else {
      node.color = c;
      setColor((SequenceNode)node.left(),c);
      setColor((SequenceNode)node.right(),c);
    }
  }


  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent evt) {
      invalidate();
     validate();

      return true;
  }

  public void paint(Graphics g) {
      if (size() != null) {
	draw(g,size().width,size().height);

	if (threshold != 0) {
	    g.setColor(Color.black);
	    g.drawLine(threshold,0,threshold,size().height);
	}
    } else {
	draw(g,500,500);

	if (threshold != 0) {
	    g.setColor(Color.black);
	    g.drawLine(threshold,0,threshold,size().height);
	}
    }
  }
    public int getFontSize() {
	return fontSize;
    }
    public void setFontSize(int fontSize) {
	this.fontSize = fontSize;
    }
  public void draw(Graphics g, int width, int height) {
      font = new Font("Helvetica",Font.PLAIN,fontSize);

      g.setFont(font);

      float wscale = (float)(width*.8-offx*2)/tree.getMaxHeight();

      SequenceNode top = tree.getTopNode();

      if (top.count == 0) {
	  top.count = ((SequenceNode)top.left()).count + ((SequenceNode)top.right()).count ;
      }
      float chunk = (float)(height-offy*2)/top.count;

      drawNode(g,tree.getTopNode(),chunk,wscale,width,offx,offy);
  }

  public void mouseReleased(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseClicked(MouseEvent e) {
  }

  public boolean handleRubberbandEvent(RubberbandEvent evt) {
    System.out.println("Rubberband handler called in TreePanel with " +
                       evt.getBounds());

    Rubberband rb = (Rubberband)evt.getSource();

    pickNodes(evt.getBounds(),selected);

    return true;
  }

  public void mousePressed(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      
      System.out.println("Mouse pressed " + x + " " + y);
      //if (rubberband.isRbButton(e)) {
      // Leave to rubberband
      //      } else {
      Object ob = findElement(x,y);
      
      System.out.println("Element " + ob);
      
      if (ob instanceof AlignSequenceI) {
	  AlignSequenceI s = (AlignSequenceI)ob;
	  
	  fireTreeSelectionEvent(new TreeSelectionEvent(this,s));
	  repaint();
	  return;
      } else if (ob instanceof SequenceNode) {
	  System.out.println("Found node " + ob);
	  SequenceNode tmpnode = (SequenceNode)ob;
	  tree.swapNodes(tmpnode);
	  tree.reCount(tree.getTopNode());
	  tree.findHeight(tree.getTopNode());
	  
	  repaint();
      } else {
          // Find threshold
	  
          if (tree.getMaxHeight() != 0) {
	      float threshold = (float)(x - offx)/(float)(size().width*0.8 - 2*offx);
	      //            threshold = x;
	      repaint();
	      System.out.println("Trehsold " + threshold);
	      tree.getGroups().removeAllElements();
	      tree.groupNodes(tree.getTopNode(),threshold);
	      setColor(tree.getTopNode(),Color.black);
	      
	      for (int i=0; i < tree.getGroups().size(); i++) {
		  
		  int tmp = i%(7);
		  Color col = new Color((int)(Math.random()*255),
					(int)(Math.random()*255),
					(int)(Math.random()*255));
		  
		  setColor((SequenceNode)tree.getGroups().elementAt(i),col.brighter());
		  
		  // l is vector of Objects
		  Vector l = tree.findLeaves((SequenceNode)tree.getGroups().elementAt(i),new Vector());
		  
	      }
	      fireTreeSelectionEvent(new TreeSelectionEvent(this,null));
	      repaint();
	      
	  }
      }
      //      }
  }
    public void addTreeSelectionListener(TreeSelectionListener l) {
	if (listeners == null) {
	    listeners = new Vector();
	}
	listeners.addElement(l);
    }
    public void fireTreeSelectionEvent(TreeSelectionEvent e) {
	for (int i =0; i < listeners.size(); i++) {
	    if (listeners.elementAt(i) instanceof TreeSelectionListener) {
		System.out.println("Firign tree event");
		((TreeSelectionListener)listeners.elementAt(i)).handleTreeSelectionEvent(e);
	    }
	}
    }
    public void setShowDistances(boolean state) {
	this.showDistances = state;
    }
    public boolean getShowDistances() {
	return showDistances;
    }
    public void setShowBootstrap(boolean state) {
      this.showBootstrap = state;
    }
    public boolean getShowBootstrap() {
      return showBootstrap;
    }
  public Dimension getPreferredSize() {
	return getMinimumSize();
  }
  public Dimension getMinimumSize() {
    return new Dimension(500,500);
  }
}



















