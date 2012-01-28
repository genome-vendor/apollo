package jalview.util;

import java.util.*;

public class ListenList {
  protected Vector listeners;
  public ListenList() {
    listeners = new Vector();
  }

  public void addListener(EventListener l) {
    if (l == null) {
      return;
    }
    if (!listeners.contains(l)) {
      listeners.addElement(l);
    }
  }

  public void removeListener(EventListener l) {
    if (l == null) {
      return;
    }
    if (listeners.contains(l)) {
      listeners.removeElement(l);
    }
  }
}
