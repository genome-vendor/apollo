package apollo.gui.genomemap;

import java.util.*;
import apollo.gui.*;
import apollo.datamodel.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import apollo.util.*;

import gov.sandia.postscript.PSGr2;


public class SplitterView extends ContainerView 
  implements MouseListener, MouseMotionListener {

  protected ViewI v1;
  protected ViewI v2;
  
  protected Polygon   splitPoly = new Polygon();
  protected Polygon   splitSelectPoly = new Polygon();
  protected Polygon   splitBoundary = new Polygon();

  protected double splitFract = 0.5;

  protected Color triangleColor = Color.red;
  protected int triangleSize = 8;  // Original was 6

  public SplitterView(JComponent ap, String name, boolean visible,
                      ViewI v1, ViewI v2) {
    super(ap,name,visible);
    
    this.v1 = v1;
    this.v2 = v2;

    setViews();

    ap.addMouseListener(this);
    ap.addMouseMotionListener(this);
  }

  public void setLeftView(ViewI v1) {
    this.v1 = v1;
    setViews();
  }
  
  public ViewI getLeftView() {
    return v1;
  }
  
  public void setRightView(ViewI v2) {
    this.v2 = v2;
    setViews();
  }

  public ViewI getRightView() {
    return v2;
  }

  protected void setViews() {
    views.clear();
    views.add(v1);
    views.add(v2);
  }

  public Rectangle setScrollSpace(int where) {
    JScrollBar jb          = new JScrollBar(JScrollBar.VERTICAL);
    int        scrollWidth = jb.getMinimumSize().width;

    Rectangle  rect = new Rectangle(getBounds());

    rect.width -= scrollWidth;
    if (where == ViewI.LEFTSIDE) {
      rect.x += scrollWidth;
    }
    return rect;
  }

  public void setBounds(Rectangle rect) {
    super.setBounds(rect);

    updateSplitFract();
    int splitPos = (int)((double)rect.height*splitFract);

    v1.setBounds(new Rectangle(rect.x,rect.y,rect.width,splitPos));
    v2.setBounds(new Rectangle(rect.x,rect.y+splitPos,
                               rect.width,rect.height-splitPos));
  }

  boolean currentV1Vis;
  boolean currentV2Vis;
  protected void updateSplitFract() {
    if (!v1.isVisible() && v2.isVisible()) {
      splitFract = 0.0;
    } else if (v1.isVisible() && !v2.isVisible()) {
      splitFract = 1.0;
    }

    if (splitFract < 0.0) { splitFract = 0.0; }
    if (splitFract > 1.0) { splitFract = 1.0; }
    if (v1.isVisible() != currentV1Vis ||
        v2.isVisible() != currentV2Vis) {
      if (v1.isVisible() && v2.isVisible()) {
        splitFract = 0.5;
      }
      currentV1Vis = v1.isVisible();
      currentV2Vis = v2.isVisible();
      setBounds(getBounds());
    }
  }

  public void resetSplitFract() {
    splitFract = 0.5;
  }

  public double getSplitFract() {
    return splitFract;
  }

  public void setSplitFract(double newFract) {
    splitFract = newFract;
    getComponent().invalidate();
    getComponent().repaint();
  }

/**
 * Draws the v1 and v2 views (if they are visible) and then if both are visible
 * draws a small triangle at the border between them.
 */

  public void paintView() {
    updateSplitFract();

    super.paintView();

    // SMJS Don't bother drawing triangle if on the printing Graphics
    //      It's not part of the data and you usually end up having
    //      to edit it out the postscript 
    if (graphics instanceof PSGr2) {
      return;
    }

    graphics.setColor(triangleColor);
    
    splitPoly = new Polygon();
    splitSelectPoly = new Polygon();
    splitBoundary = new Polygon();

    if (v1.isVisible() && v2.isVisible()) {
      Rectangle vb = transformer.getPixelBounds();
      int splitPos = (int)((double)vb.height*splitFract) + vb.y;
  
      // I like the triangles on the left instead of the right.  --NH
      //      int trianglePos = vb.width;
      int trianglePos = 0;
      splitPoly.addPoint(vb.x+trianglePos,splitPos-triangleSize/2);
      splitPoly.addPoint(vb.x+trianglePos+triangleSize,splitPos);
      splitPoly.addPoint(vb.x+trianglePos,splitPos+triangleSize/2);
  
      trianglePos = 0;
      int selTriangleSize = triangleSize+2;
      splitSelectPoly.addPoint(vb.x+trianglePos,splitPos-selTriangleSize/2);
      splitSelectPoly.addPoint(vb.x+trianglePos+selTriangleSize,splitPos);
      splitSelectPoly.addPoint(vb.x+trianglePos,splitPos+selTriangleSize/2);
  
      splitBoundary.addPoint(vb.x,splitPos-2);
      splitBoundary.addPoint(vb.x+vb.width,splitPos-2);
      splitBoundary.addPoint(vb.x+vb.width,splitPos+2);
      splitBoundary.addPoint(vb.x,splitPos+2);
  
      graphics.fillPolygon(splitPoly);
    }
  }

  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }
  public void mouseClicked(MouseEvent evt) { }

  int start;
  double startFract;

  public void mousePressed(MouseEvent evt) {
    if (splitSelectPoly.contains(evt.getPoint())) {
    
      start = evt.getPoint().y;
      startFract = splitFract;
    } else {
      start = -1;
    }
  }

  public void mouseReleased(MouseEvent evt) { }

  public void mouseDragged(MouseEvent evt) { 
   if (start != -1) {
     splitFract = ((double)evt.getPoint().y-(double)start) / 
                   (double)viewBounds.height  + startFract;


     updateSplitFract();
   
     setBounds(getBounds());
     getComponent().invalidate();
     getComponent().repaint();
   }
  }

  public void mouseMoved(MouseEvent evt) { 
  }

  public void invertViews() {
    ViewI tmp = v1;
    v1 = v2;
    v2 = tmp;
    splitFract = 1.0 - splitFract;
  }
}
