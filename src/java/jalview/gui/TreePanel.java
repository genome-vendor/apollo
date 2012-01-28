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

import javax.swing.*;

public class TreePanel extends Panel implements ActionListener,
                                                ItemListener,
                                                SequenceSelectionListener,
                                                OutputGenerator,
                                                TreeSelectionListener {

  NJTree tree;
  Object parent;

  TreeCanvas treeCanvas;

  Button close;
  Button output;

  Checkbox cb;
  Checkbox boot;
  Choice   f;

  FileProperties       fp;
  PostscriptProperties pp;

  PrintWriter bw;
  PrintStream ps;
  boolean makeString = false;
  StringBuffer out;

  Controller controller;
  AlignViewport av;
  Selection selected;

  RubberbandRectangle rubberband;

  public TreePanel(Object parent, NJTree tree) {
    this.tree = tree;
    this.parent = parent;
    this.selected = new Selection();
    treeInit();
  }
    public TreePanel(Object parent, AlignViewport av, Controller c, NJTree tree) {
	this.tree = tree;
	this.controller = c;
	this.av = av;
	this.selected = av.getSelection();
	this.parent = parent;

	treeInit();
	controller.addListener(this);
	controller.handleStatusEvent(new StatusEvent(this,"Finished calculating tree",StatusEvent.INFO));

    }
    public TreePanel(Object parent,AlignViewport av,Controller c,AlignSequenceI[] s,String treetype, String pairdist) {
    this.tree        = new NJTree(s,treetype,pairdist);
    this.controller = c;
    this.av         = av;
    this.selected   = av.getSelection();
    this.parent     = parent;

    controller.addListener(this);
    controller.handleStatusEvent(new StatusEvent(this,"Finished calculating tree",StatusEvent.INFO));

    this.parent = parent;
    treeInit();
  }

  public TreePanel(Object parent,AlignViewport av, Controller c,AlignSequenceI[] s) {
    this(parent,av,c,s,"AV","BL");
  }

  public void treeInit() {
      propertiesInit();
    setLayout(new BorderLayout());

    treeCanvas = new TreeCanvas(this,tree,selected);

    treeCanvas.addTreeSelectionListener(this);

    Panel p2 = new Panel();

    p2.setLayout(new FlowLayout());

    close = new Button("Close");
    close.addActionListener(this);
    output = new Button("Output");
    output.addActionListener(this);

    cb = new Checkbox("Show distances");
    cb.setState(treeCanvas.getShowDistances());
    cb.addItemListener(this);

    boot = new Checkbox("Show bootstrap");
    boot.setState(treeCanvas.getShowBootstrap());
    boot.addItemListener(this);

    Label l = new Label("Font size");

    f = new Choice();

    int count = 1;
    while (count <= 30) {
	f.addItem(Integer.toString(count));
	count++;
    }
    f.select(treeCanvas.getFontSize());
    f.addItemListener(this);

    if (av != null && av.getCurrentTree() == null) {
	av.setCurrentTree(tree);
    }
    tree.reCount(tree.getTopNode());
    tree.findHeight(tree.getTopNode());

    p2.add(l);
    p2.add(f);
    p2.add(cb);
    p2.add(boot);
    p2.add(output);
    p2.add(close);

    add("Center",treeCanvas);
    add("South",p2);

    if (av != null) {
      av.setCurrentTree(tree);
    }
    //    rubberband  = new RubberbandRectangle(p);
    //    rubberband.setActive(true);
    //    rubberband.addListener(this);
  }

  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent evt) {
      if (av != null) {
	  selected = av.getSelection();
      }
      treeCanvas.setSelected(selected);
      treeCanvas.repaint();
      return true;
  }

  public void itemStateChanged(ItemEvent evt) {

    if (evt.getSource() == f) {
      int size = Integer.parseInt(((Choice)f).getSelectedItem());
      treeCanvas.setFontSize(size);
      treeCanvas.repaint();
    } else if (evt.getSource() == cb) {
	    treeCanvas.setShowDistances(cb.getState());
	    treeCanvas.repaint();

      if (parent instanceof JFrame) {
        ((JFrame)parent).validate();
      }
    } else if (evt.getSource() == boot) {
      treeCanvas.setShowBootstrap(boot.getState());
      treeCanvas.repaint();
    }

  }

  public void actionPerformed(ActionEvent evt) {

    if (evt.getSource() == close) {
      this.hide();
      if (getParent() instanceof JFrame) {
        ((Frame)getParent()).dispose();
      }
    } else if (evt.getSource() == output) {
	if (parent instanceof JFrame) {
        PostscriptFilePopup pp = new PostscriptFilePopup((JFrame)parent,av,controller,"Save as postscript",this);
	}
    }
  }

    public void setParent(Object parent) {
	this.parent = parent;
    }
  public void drawPostscript(PostscriptProperties pp) {
    try {
      int width = 0;
      int height = 0;

      printout("%!\n");

      printout("/" + pp.font + " findfont\n");
      printout(pp.fsize + " scalefont setfont\n");

      int offx = pp.xoffset;
      int offy = pp.yoffset;

      if (pp.orientation == PostscriptProperties.PORTRAIT) {
        width = PostscriptProperties.SHORTSIDE;
        height = PostscriptProperties.LONGSIDE;
      } else {
        height = PostscriptProperties.SHORTSIDE;
        width = PostscriptProperties.LONGSIDE;
        printout(height + " 0 translate\n90 rotate\n");
      }
      float wscale = (float)(width*.8-offx*2)/tree.getMaxHeight();
      SequenceNode top = tree.getTopNode();

      if (top.count == 0) {
	  top.count = ((SequenceNode)top.left()).count + ((SequenceNode)top.right()).count ;
      }

      float chunk = (float)(height-offy*2)/(((SequenceNode)top).count+1);

      drawPostscriptNode(top,chunk,wscale,width,offx,offy);

      printout("showpage\n");
    } catch (java.io.IOException e) {
      System.out.println("Exception " + e);
    }
  }

  public void drawPostscriptNode(SequenceNode node, float chunk, float scale, int width, int offx, int offy) throws java.io.IOException {
    if (node == null) {
      return;
    }

    if (node.left() == null && node.right() == null) {
      // Drawing leaf node

      float height = node.height;
      float dist = node.dist;

      int xstart = (int)((height-dist)*scale) + offx;
      int xend =   (int)(height*scale) + offx;

      int ypos = (int)(node.ycount * chunk) + offy;

      // g.setColor(Color.black);
      printout("\n" + new Format("%5.3f").form((double)node.color.getRed()/255) + " " +
               new Format("%5.3f").form((double)node.color.getGreen()/255) + " " +
               new Format("%5.3f").form((double)node.color.getBlue()/255) + " setrgbcolor\n");

      // Draw horizontal line
      //  g.drawLine(xstart,ypos,xend,ypos);
      printout(xstart + " " + ypos + " moveto " + xend + " " + ypos + " lineto stroke \n");

      if (treeCanvas.showDistances && node.dist > 0) {
        //	g.drawString(new Format("%5.2f").form(node.dist),xstart,ypos - 5);
        printout("(" + new Format("%5.2f").form(node.dist) + ") " + xstart + " " + (ypos+5) + " moveto show\n");
      }
      //g.drawString((String)node.element(),xend+20,ypos);
      printout("(" + (((AlignSequenceI)node.element()).getName()) + ") " + (xend+20) + " " + (ypos) + " moveto show\n");
    } else {
      drawPostscriptNode((SequenceNode)node.left(),chunk,scale,width,offx,offy);
      drawPostscriptNode((SequenceNode)node.right(),chunk,scale,width,offx,offy);


      float height = node.height;
      float dist = node.dist;

      int xstart = (int)((height-dist)*scale) + offx;
      int xend =   (int)(height*scale) + offx;
      int ypos = (int)(node.ycount * chunk) + offy;

      printout("\n" + new Format("%5.3f").form((double)node.color.getRed()/255) + " " +
               new Format("%5.3f").form((double)node.color.getGreen()/255) + " " +
               new Format("%5.3f").form((double)node.color.getBlue()/255) + " setrgbcolor\n");
      //      g.setColor(Color.black);
      //      bw.append("\nblack setrgbcolor\n");
      // Draw horizontal line
      //      g.drawLine(xstart,ypos,xend,ypos);
      printout(xstart + " " + ypos + " moveto " + xend + " " + ypos + " lineto stroke\n");
      int ystart = (int)(((SequenceNode)node.left()).ycount * chunk) + offy;
      int yend = (int)(((SequenceNode)node.right()).ycount * chunk) + offy;

      //      g.drawLine((int)(height*scale) + offx, ystart,
      //		 (int)(height*scale) + offx, yend);
      printout
      (((int)(height*scale) + offx) + " " + ystart + " moveto " +  ((int)(height*scale) + offx) + " " +
       yend + " lineto stroke\n");
      if (treeCanvas.showDistances && node.dist > 0) {
        //	g.drawString(new Format("%5.2f").form(node.dist),xstart,ypos - 5);
        printout("(" +new Format("%5.2f").form(node.dist) + ") " + (xstart) + " " + (ypos+5) + " moveto show\n");
      }
    }
  }

  public void printout(String s) throws IOException {
    if (bw != null) {
      bw.write(s);
    }
    if (ps != null) {
      ps.print(s);
    }
    if (makeString == true) {
      out.append(s);
    }
  }

  public MailProperties getMailProperties() {
    return null;
  }
  public void setMailProperties(MailProperties mp) {
  }
  public PostscriptProperties getPostscriptProperties() {
    return pp;
  }

  public FileProperties getFileProperties() {
    return fp;
  }

  public void setPostscriptProperties(PostscriptProperties pp) {
    this.pp = pp;
  }

  public void setFileProperties(FileProperties fp) {
    this.fp = fp;
  }

  public String getText(String format) {
    return null;
  }

  public void getPostscript(PrintWriter bw) {
    this.bw = bw;
    drawPostscript(pp);

  }

  public void getPostscript(PrintStream bw) {
    this.ps = bw;
    drawPostscript(pp);
    bw.flush();
  }

  public StringBuffer getPostscript() {
    makeString = true;
    out = new StringBuffer();
    drawPostscript(pp);
    return out;
  }

  public void propertiesInit() {
    this.pp = new PostscriptProperties();
    this.fp = new FileProperties();
  }
    public boolean handleTreeSelectionEvent(TreeSelectionEvent evt) {
      if (av != null) {
	  selected = av.getSelection();
	  if (evt.getSequence() != null) {
	      if (selected.contains(evt.getSequence())) {
		  selected.removeElement(evt.getSequence());
	      } else {
		  selected.addElement(evt.getSequence());
	      }
	  }
	  controller.handleSequenceSelectionEvent(new SequenceSelectionEvent(this,av.getSelection()));
	  treeCanvas.setSelected(selected);
	  treeCanvas.repaint();
      }

      return true;

    }
}



















