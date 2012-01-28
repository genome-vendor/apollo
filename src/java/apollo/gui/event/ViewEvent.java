package apollo.gui.event;

import apollo.gui.genomemap.ViewI;
import java.util.EventObject;

public class ViewEvent extends EventObject {
  public static final int LIMITS_CHANGED = 1;
  public static final int NEEDS_PAINT    = 2;

  int    type;
  ViewI  viewSource;

  public ViewEvent(Object source,ViewI viewSource, int type) {
    super(source);

    this.viewSource = viewSource;
    this.type       = type;
  }

  public int getType() {
    return type;
  }

  public ViewI getViewSource() {
    return viewSource;
  }

  public Object getSource() {
    return source;
  }
}
