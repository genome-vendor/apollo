package apollo.util;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.*;

public class SwingMissingUtil {

  // setResizeWeight is only available in java 1.3, so we need to jump
  // through several hoops to use it.
  public static void setResizeWeight(JSplitPane sp, double weight) {
    try {
      Class[] args = new Class[1];
      args[0] = Double.TYPE;

      Method meth = (sp.getClass().getMethod("setResizeWeight",args));
      //      System.out.println("Setting resize weight");
      Object[] objs = new Object[1];
      objs[0] = new Double(weight);
      meth.invoke(sp,objs);

    } catch (NoSuchMethodException e) {
      System.out.println("No setResizeWeight");
    }
    catch (IllegalAccessException e) {
      System.out.println("No access");
    }
    catch (InvocationTargetException e) {
      System.out.println("No invoke");
    }
  }

  public  static Window getWindowAncestor(Component c) {
    for(Container p = c.getParent(); p != null; p = p.getParent()) {
      if (p instanceof Window) {
        return (Window)p;
      }
    }
    return null;
  }

  public static JViewport getViewportAncestor(Component c) {
    for(Container p = c.getParent(); p != null; p = p.getParent()) {
      if (p instanceof JViewport) {
        return (JViewport)p;
      }
    }
    return null;
  }
}
