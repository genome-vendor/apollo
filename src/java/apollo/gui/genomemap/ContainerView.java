package apollo.gui.genomemap;

import java.util.Vector;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;

import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.Selection;
import apollo.util.SwingMissingUtil;

public abstract class ContainerView extends LinearView
  implements ContainerViewI, PickViewI {

  protected Controller controller;
  protected Vector views = new Vector();

  public ContainerView (JComponent ap, String name, boolean visible) {
    super (ap, name, visible);
  }

  public void setController (Controller c) {
    this.controller = c;
    for (int i=0; i<views.size(); i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof ControlledObjectI) {
        ((ControlledObjectI)v).setController(c);
      }
    }
  }

  public Controller getController() {
    return controller;
  }

  public Object getControllerWindow() {
    return SwingMissingUtil.getWindowAncestor(getComponent());
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  public void paintView() {
    for (int i=0; i<views.size(); i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v.isVisible()) {
        v.paintView();
      }
    }
  }

  public Vector getViews() {
    return views;
  }

  public Vector getViewsOfClass(Class c) {
    Vector matches = new Vector();
    for (int i=0; i<views.size(); i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof ContainerViewI) {
        matches.addAll(((ContainerViewI)v).getViewsOfClass(c));
      } else if (c.isInstance(v)) {
        matches.addElement(v);
      }
    }
    return matches;
  }

  public ViewI getContainedViewAt(Point p) {
    for (int i=0; i<views.size(); i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v.getBounds().contains(p)) {
        if (v instanceof ContainerViewI) {
          return ((ContainerViewI)v).getContainedViewAt(p);
        } else {
          return v;
        }
      }
    }
    return null;
  }

  public void setInvalidity(boolean state) {
    super.setInvalidity(state);
    /* check for null here because this is called
       from LinearView.setVisible before this class 
       is fully instantiated  */
    int view_count = (views == null ? 0 : views.size());
    for (int i=0; i<view_count; i++) {
      ((ViewI)views.elementAt(i)).setInvalidity(state);
    }
  }
  
  public void setLimits(int [] limits) {
    super.setLimits(limits);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setLimits(limits);
    }
  }
 
  public void setMinimum(int min) {
    super.setMinimum(min);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setMinimum(min);
    }
  }

  public void setMaximum(int max) {
    super.setMaximum(max);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setMaximum(max);
    }
  }

  public void setDrawBounds(Rectangle rect) {
    rect = setScrollSpace(ViewI.RIGHTSIDE);
    super.setDrawBounds(rect);
  }

  public abstract Rectangle setScrollSpace(int where);

  public void setGraphics(Graphics g) {
    super.setGraphics(g);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setGraphics(g);
    }
  }

  public void setComponent(JComponent ap) {
    super.setComponent(ap);
    /* check for null here because this is called
       from LinearView.setComponent before this class 
       is fully instantiated  */
    int view_count = (views == null ? 0 : views.size());
    for (int i=0; i<view_count; i++) {
      ((ViewI)views.elementAt(i)).setComponent(ap);
    }
  }

  public void setCentre(int centre) {
    super.setCentre(centre);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setCentre(centre);
    }
  }

  public void setZoomFactor(double factor) {
    super.setZoomFactor(factor);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setZoomFactor(factor);
    }
  }

  public void setLimitsSet(boolean state) {
    super.setLimitsSet(state);
    for (int i=0; i<views.size(); i++) {
      ((ViewI)views.elementAt(i)).setLimitsSet(state);
    }
  }

  public Selection findFeaturesForSelection(Point p, boolean selectParents) {
    Selection compSel = new Selection();
    for (int i=0; i<views.size(); i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof PickViewI) {
        compSel.add(((PickViewI)v).findFeaturesForSelection(p,selectParents));
      }
    }
    return compSel;
  }

  public Selection findFeaturesForSelection(Rectangle rect) {
    Selection compSel = new Selection();
    for (int i=0; i<views.size(); i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof PickViewI) {
        compSel.add(((PickViewI)v).findFeaturesForSelection(rect));
      }
    }
    return compSel;
  }

}
