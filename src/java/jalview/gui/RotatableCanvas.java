package jalview.gui;

import jalview.gui.event.*;
import jalview.math.*;
import jalview.datamodel.*;
import jalview.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class RotatableCanvas extends Canvas implements MouseListener, 
                                                       MouseMotionListener,
                                                       KeyListener,
                                                       RubberbandListener, 
                                                       SequenceSelectionListener {
  RotatableMatrix idmat  = new RotatableMatrix(3,3);
  RotatableMatrix objmat = new RotatableMatrix(3,3);
  RotatableMatrix rotmat = new RotatableMatrix(3,3);

  RubberbandRectangle rubberband; 

  boolean redrawneeded =true;
  boolean drawAxes = true;

  int omx = 0;
  int mx = 0;
  int omy = 0;
  int my = 0;

  Image img;
  Graphics ig;

  Dimension prefsize;

  float centre[] = new float[3];
  float width[] = new float[3];

  float max[] = new float[3];
  float min[] = new float[3];

  float maxwidth;
  float scale;

  int npoint;

  Vector points;
  float[][] orig;
  float[][] axes;

  Object parent;

  int startx;
  int starty;

  int lastx;
  int lasty;

  int rectx1;
  int recty1;
  int rectx2;
  int recty2;

  float scalefactor = 1;

  AlignViewport av;
  Controller    controller;


  public RotatableCanvas(Object parent, AlignViewport av,
                         Controller c, Vector points, int npoint) {
    this.parent = parent;
    this.points = points;
    this.npoint = npoint;
    this.controller = c;
    this.av = av;

    controller.addListener(this);

    prefsize = getPreferredSize();
    orig = new float[npoint][3];

    for (int i=0; i < npoint; i++) {
      SequencePoint sp = (SequencePoint)points.elementAt(i);
      for (int j=0; j < 3; j++) {
        orig[i][j] = sp.coord[j];
      }
    }
    //Initialize the matrices to identity

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3 ; j++) {
        if (i != j) {
          idmat.addElement(i,j,0);
          objmat.addElement(i,j,0);
          rotmat.addElement(i,j,0);
        } else {
          idmat.addElement(i,j,0);
          objmat.addElement(i,j,0);
          rotmat.addElement(i,j,0);
        }
      }
    }

    axes = new float[3][3];
    initAxes();

    findCentre();
    findWidth();

    scale = findScale();

    //    System.out.println("Scale factor = " + scale);

    addMouseListener(this);
    addKeyListener(this);
    if (getParent() != null) {
      getParent().addKeyListener(this);
    }
    addMouseMotionListener(this);

    // Add rubberband
    rubberband  = new RubberbandRectangle(this);
    rubberband.setActive(true);
    rubberband.addListener(this);
  }

  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent evt) {
    redrawneeded = true;
    repaint();
    return true;
  }

  public void removeNotify() {
    controller.removeListener(this);
    super.removeNotify();
  }

  public void initAxes() {
    for (int i = 0; i < 3; i++) {
      for (int j=0; j < 3; j++) {
        if (i != j) {
          axes[i][j] = 0;
        } else {
          axes[i][j] = 1;
        }
      }
    }
  }

  public void findWidth() {
    max = new float[3];
    min = new float[3];

    max[0] = (float)-1e30;
    max[1] = (float)-1e30;
    max[2] = (float)-1e30;

    min[0] = (float)1e30;
    min[1] = (float)1e30;
    min[2] = (float)1e30;

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < npoint; j++) {
        SequencePoint sp = (SequencePoint)points.elementAt(j);
        if (sp.coord[i] >= max[i]) {
          max[i] = sp.coord[i];
        }
        if (sp.coord[i] <= min[i]) {
          min[i] = sp.coord[i];
        }
      }
    }

    //    System.out.println("xmax " + max[0] + " min " + min[0]);
    //System.out.println("ymax " + max[1] + " min " + min[1]);
    //System.out.println("zmax " + max[2] + " min " + min[2]);

    width[0] = Math.abs(max[0] - min[0]);
    width[1] = Math.abs(max[1] - min[1]);
    width[2] = Math.abs(max[2] - min[2]);

    maxwidth = width[0];

    if (width[1] > width[0])
      maxwidth = width[1];
    if (width[2] > width[1])
      maxwidth = width[2];

    //System.out.println("Maxwidth = " + maxwidth);
  }

  public float findScale() {
    int dim, width, height;
    if (size().width != 0) {
      width = size().width;
      height = size().height;
    } else {
      width = prefsize.width;
      height = prefsize.height;
    }

    if (width < height) {
      dim = width;
    } else {
      dim = height;
    }

    return (float)(dim*scalefactor/(2*maxwidth));
  }

  public void findCentre() {
    //Find centre coordinate
    findWidth();

    centre[0] = (max[0] + min[0])/2;
    centre[1] = (max[1] + min[1])/2;
    centre[2] = (max[2] + min[2])/2;

    //    System.out.println("Centre x " + centre[0]);
    //System.out.println("Centre y " + centre[1]);
    //System.out.println("Centre z " + centre[2]);
  }

  public Dimension getPreferredSize() {
    if (prefsize != null) {
      return prefsize;
    } else {
      return new Dimension(400,400);
    }
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public void paint(Graphics g) {
    //Only create the image at the beginning -
    if ((img == null) || (prefsize.width != size().width) || (prefsize.height != size().height)) {
      prefsize.width = size().width;
      prefsize.height = size().height;

      scale = findScale();

      //      System.out.println("New scale = " + scale);
      img = createImage(size().width,size().height);
      ig = img.getGraphics();

      redrawneeded = true;
    }

    if (redrawneeded == true) {
      drawBackground(ig,Color.black);
      drawScene(ig);
      if (drawAxes == true) {
        drawAxes(ig);
      }
      redrawneeded = false;
    } else {
      ig = img.getGraphics();
    }

    g.drawImage(img,0,0,this);
  }

  public void drawAxes(Graphics g) {

    g.setColor(Color.yellow);
    for (int i=0; i < 3 ; i++) {
      g.drawLine(size().width/2,size().height/2,
                 (int)(axes[i][0]*scale*max[0] + size().width/2),
                 (int)(axes[i][1]*scale*max[1] + size().height/2));
    }
  }

  public void drawBackground(Graphics g, Color col) {
    g.setColor(col);
    g.fillRect(0,0,prefsize.width,prefsize.height);
  }


  public void drawScene(Graphics g) {
    boolean darker = false;

    int halfwidth = size().width/2;
    int halfheight = size().height/2;

    for (int i = 0; i < npoint; i++) {
      SequencePoint sp = (SequencePoint)points.elementAt(i);
      int x = (int)((float)(sp.coord[0] - centre[0])*scale) + halfwidth;
      int y = (int)((float)(sp.coord[1] - centre[1])*scale) + halfheight;
      float z = sp.coord[1] - centre[2];
      if (sp.sequence instanceof DrawableSequence) {
        if (((DrawableSequence)sp.sequence).color == Color.black) {
          g.setColor(Color.white);
        } else {
          g.setColor(((DrawableSequence)sp.sequence).color);
        }
      } else {
        g.setColor(Color.red);
      }
      if (av != null) {
        if (av.getSelection().contains(((SequencePoint)points.elementAt(i)).sequence)) {
          g.setColor(Color.gray);
        }
      }
      if (z < 0) {
        g.setColor(g.getColor().darker());
      }

      g.fillRect(x-3,y-3,6,6);
      g.setColor(Color.red);
    }
//    //Now the rectangle
//    if (rectx2 != -1 && recty2 != -1) {
//      g.setColor(Color.white);
//
//      g.drawRect(rectx1,recty1,rectx2-rectx1,recty2-recty1);
//    }
  }

  public Dimension minimumsize() {
    return prefsize;
  }

  public Dimension preferredsize() {
    return prefsize;
  }

  public void keyTyped(KeyEvent evt) { }
  public void keyReleased(KeyEvent evt) { }

  public void keyPressed(KeyEvent evt) {
    requestFocus();
    if (evt.getKeyCode() == KeyEvent.VK_UP) {
      scalefactor = (float)(scalefactor * 1.1);
      scale = findScale();
      redrawneeded = true;
      repaint();
    } else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
      scalefactor = (float)(scalefactor * 0.9);
      scale = findScale();
      redrawneeded = true;
      repaint();
    } else if (evt.getKeyChar() == 's') {
      System.out.println("Rectangle selection");
      if (rectx2 != -1 && recty2 != -1) {
        rectSelect(rectx1,recty1,rectx2,recty2);
        redrawneeded = true;
        repaint();
      }
    }
    return;
  }

  public void printPoints() {
    for (int i=0; i < npoint; i++) {
      SequencePoint sp = (SequencePoint)points.elementAt(i);
      Format.print(System.out,"%5d ", i);
      for (int j=0; j < 3;j++) {
        Format.print(System.out,"%13.3f  ",sp.coord[j]);
      }
      System.out.println();
    }
  }

  public void mouseClicked(MouseEvent evt) { }
  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }
  public void mouseReleased(MouseEvent evt) { }

  public void mousePressed(MouseEvent evt) {
    int x = evt.getX();
    int y = evt.getY();

    mx = x;
    my = y;

    omx = mx;
    omy = my;

    startx = x;
    starty = y;

    rectx1 = x;
    recty1 = y;

    rectx2 = -1;
    recty2 = -1;

    AlignSequenceI found = findPoint(x,y);

    if (found != null) {
      if (av != null) {

        if (av.getSelection().contains(found)) {
          av.getSelection().removeElement(found);
        } else {
          av.getSelection().addElement(found);
        }

        fireSequenceSelectionEvent(av.getSelection());

        System.out.println("Selection code not implemented in RotatableCanvas");
        //if (af.tt != null) {
        //  af.tt.tf.p.mc.repaint();
        //}
      }

      System.out.println("Sequence found = " + found.getName());

    }
    redrawneeded = true;
    repaint();
    return;
  }

  private void fireSequenceSelectionEvent(Selection sel) {
    controller.handleSequenceSelectionEvent(new SequenceSelectionEvent(this,sel));
  }

  public void mouseMoved(MouseEvent evt) { }

  public void mouseDragged(MouseEvent evt) {
    mx = evt.getX();
    my = evt.getY();
    //Check if this is a rectangle drawing drag
    if ((evt.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
//      rectx2 = evt.getX();
//      recty2 = evt.getY();
    } else {
      rotmat.setIdentity();

      rotmat.rotate((float)(my-omy),'x');
      rotmat.rotate((float)(mx-omx),'y');

      for (int i = 0; i < npoint; i++) {
        SequencePoint sp = (SequencePoint)points.elementAt(i);
        sp.coord[0] -= centre[0];
        sp.coord[1] -= centre[1];
        sp.coord[2] -= centre[2];

        //Now apply the rotation matrix
        sp.coord= rotmat.vectorMultiply(sp.coord);

        //Now translate back again
        sp.coord[0] += centre[0];
        sp.coord[1] += centre[1];
        sp.coord[2] += centre[2];
      }

      for (int i=0; i < 3; i++) {
        axes[i] = rotmat.vectorMultiply(axes[i]);
      }
      omx = mx;
      omy = my;

      redrawneeded = true;
      paint(this.getGraphics());
    }

    return;
  }

  public void rectSelect(int x1, int y1, int x2, int y2) {
    boolean changedSel = false;
    for (int i=0; i < npoint; i++) {
      SequencePoint sp = (SequencePoint)points.elementAt(i);
      int tmp1 = (int)((sp.coord[0] - centre[0])*scale + (float)size().width/2.0);
      int tmp2 = (int)((sp.coord[1] - centre[1])*scale + (float)size().height/2.0);

      if (tmp1 > x1 && tmp1 < x2 && tmp2 > y1 && tmp2 < y2) {
        if (av != null) {
          if (!av.getSelection().contains(sp.sequence)) {
            changedSel = true;
            av.getSelection().addElement(sp.sequence);
          }
        }
      }
    }
    if (changedSel) {
       fireSequenceSelectionEvent(av.getSelection());
    }
  }

  public AlignSequenceI findPoint(int x, int y) {

    int halfwidth = size().width/2;
    int halfheight = size().height/2;

    int found = -1;

    for (int i=0; i < npoint; i++) {

      SequencePoint sp = (SequencePoint)points.elementAt(i);
      int px = (int)((float)(sp.coord[0] - centre[0])*scale) + halfwidth;
      int py = (int)((float)(sp.coord[1] - centre[1])*scale) + halfheight;


      if (Math.abs(px-x)<3 && Math.abs(py - y) < 3 ) {
        found = i;
      }
    }
    if (found != -1) {
      return ((SequencePoint)points.elementAt(found)).sequence;
    } else {
      return null;
    }
  }
  public boolean handleRubberbandEvent(RubberbandEvent evt) {
    System.out.println("Rubberband handler called in RotatableCanvas with " +
                       evt.getBounds());

    Rubberband rb = (Rubberband)evt.getSource();
 
    // Clear the current selection (instance variable)
    //if ((rb.getModifiers() & Event.SHIFT_MASK) == 0) {
    //   clearSelection();
    //}
 
    if (rb.getComponent() == this) {
      Rectangle bounds = evt.getBounds();
      rectSelect(bounds.x,bounds.y,bounds.x+bounds.width,bounds.y+bounds.height);
    }

    redrawneeded = true;
    paint(this.getGraphics());

    return true;
  }
  public void update(Graphics g) {
    paint(g);
  }
}
