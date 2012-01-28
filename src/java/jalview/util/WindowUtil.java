package jalview.util;

import java.awt.*;
import java.lang.reflect.*;

public class WindowUtil {

  public  static Window getWindowAncestor(Component c) {
    for(Container p = c.getParent(); p != null; p = p.getParent()) {
      if (p instanceof Window) {
        return (Window)p;
      }
    }
    return null;
  }

  public static void removeComponents(Container cont) {
    Component[] components = cont.getComponents();
    Component comp;
 
    for (int i = 0; i < components.length; i++) {
      comp = components[i];
      if (comp != null) {
        cont.remove(comp);
        if (comp instanceof Container)
          removeComponents((Container) comp);
      }
    }
  }
  public static void invalidateComponents(Container cont) {
    Component[] components = cont.getComponents();
    Component comp;
 
    cont.invalidate();
    for (int i = 0; i < components.length; i++) {
      comp = components[i];
      if (comp != null) {
        if (comp instanceof Container)
          invalidateComponents((Container) comp);
        else 
          comp.invalidate();
      }
    }
  }
}
