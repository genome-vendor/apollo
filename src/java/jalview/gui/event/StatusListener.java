package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.StatusEvent;

public interface StatusListener extends EventListener {

  public boolean handleStatusEvent(StatusEvent evt);
}
