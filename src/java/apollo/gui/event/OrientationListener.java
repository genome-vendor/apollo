package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.OrientationEvent;

public interface OrientationListener extends EventListener {

  public boolean handleOrientationEvent(OrientationEvent evt);

}
