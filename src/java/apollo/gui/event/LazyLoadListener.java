package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.LazyLoadEvent;

public interface LazyLoadListener extends EventListener {

  public boolean handleLazyLoadEvent(LazyLoadEvent evt);
}
