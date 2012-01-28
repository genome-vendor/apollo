package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.ViewEvent;

public interface ViewListener extends EventListener {

  public boolean handleViewEvent(ViewEvent evt);

}
