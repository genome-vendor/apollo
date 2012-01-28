package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.ZoomEvent;

public interface ZoomListener extends EventListener {

  public boolean handleZoomEvent(ZoomEvent evt);

}
