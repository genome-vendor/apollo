package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.ZoomEvent;

/**
 * Introduced to propagate join-scroll events to both panels: the original scroll event
 * can be stored inside, and unwrapped at the point of reaction
**/
public interface ScrollListener extends EventListener {

  public boolean handleScrollEvent(ScrollEvent event);

}
