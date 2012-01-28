package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import apollo.config.Config;

/**
 * An Abstract base ApolloLayoutManager class
 */

public abstract class ApolloLayoutManager extends BorderLayout {

  /**
   * New layout constraints (indicate which dimensions (horiz,vert) are controlled by layout manager
   */
  public static final String NONE       = "None";
  public static final String HORIZONTAL = "Horizontal";
  public static final String VERTICAL   = "Vertical";
  public static final String BOTH       = "Both";
//   /** Temporary hacks - very embarassing */   // No longer needed
//   static final String BOTH_ANNOTATIONS_PERCENT_VERTICAL =
//     "Both but vertically use PercentSpaceForAnnotations";
//   static final String BOTH_RESULTS_PERCENT_VERTICAL =
//     "Both but use 100% - PercentSpaceForAnnotations";

  Component   free       = null;
  Rectangle   freeBounds = new Rectangle(0,0,100,100);
  Hashtable   viewmap    = null;

  public ApolloLayoutManager() {
    super();
    viewmap = new Hashtable();
    free = null;
  }

  public void addLayoutComponent(Component component,Object constraints) {
    synchronized (component.getTreeLock()) {
      if (constraints != null && ((String)constraints).equals(NONE)) {
        free = component;
        //System.out.println("Added free component " + component);
      }
      else {
        if (component != free) {
          // System.out.println("Added super component " + component);
          super.addLayoutComponent(component,constraints);
        } else {
          System.out.println("Tried to readd free component " + component);
        }
      }
    }
    // System.out.println("returned from addLayoutComponent");
  }

  public void invalidateLayout(Container target) {
    // System.out.println("Called invalidateLayout");
    super.invalidateLayout(target);
  }

  public void removeLayoutComponent(Component comp,boolean force) {
    synchronized (comp.getTreeLock()) {
      if (comp == free) {

        // There is a bug (feature) in the SWING JLayeredPane. When a component's
        // layer is set using setLayer() the component is removed from it's container
        // and then re-added. Unfortunately it is re-added with null constraints. This
        // would cause the free component to become a CENTER component. To get round
        // this DO NOT remove a free component from the layout.

        if (force) {
          System.out.println("Removed free component " + comp);
          free = null;
        } else {
          // on dragging and dropping a result into annotView this message comes
          // with the JScrollBar which seems bizarre to me and bears looking into.
          // For now im just commenting out the message.
          // their should be a if (Debug.DEBUG) or something
          //System.out.println("Attempt to removed free component " + comp);
        }
      }
      else {
        // System.out.println("Removed super component " + comp);
        super.removeLayoutComponent(comp);
      }
    }
  }
  public void removeLayoutComponent(Component comp) {
    removeLayoutComponent(comp,false);
  }

  public void addLayoutView(ViewI view, Object constraints) {

    // By default views are managed
    if (constraints == null) {
      viewmap.put(view,BOTH);
    } else {
      viewmap.put(view,constraints);
    }
    //  System.out.println("returned from addLayoutView");
  }

  public void removeLayoutView(ViewI view) {
    if (viewmap.containsKey(view)) {
      viewmap.remove(view);
      // System.out.println("removed view in removeLayoutView");
    }

  }

  public void layoutContainer(Container target) {
    //      System.out.println("Laying out container " + target);
    super.layoutContainer(target);
    if (free != null) { // what is free??
      Point loc = free.getLocation();
      Dimension sz = free.getSize();

      //System.out.println("Laying out free component " + free);
      free.setBounds(loc.x,loc.y,sz.width,sz.height);
    }
  }

  public abstract void layoutViews(Container target);

//   /** If a PercentSpaceForAnnotations has been set in config, returns true */
//   static boolean doHorribleVerticalLayoutHack() {
//     return Config.doHorribleVerticalLayoutHack();
//   }
//   protected static float getPercentSpaceForAnnotations() {
//     return Config.getPercentSpaceForAnnotations();
//   }
}
