package apollo.gui.genomemap;

import java.util.*;
import java.awt.*;

import apollo.gui.ControlledObjectI;

public interface ContainerViewI extends ControlledObjectI {
  public Vector getViews();

  public ViewI getContainedViewAt(Point p);
 
  public Vector getViewsOfClass(Class c);
}
