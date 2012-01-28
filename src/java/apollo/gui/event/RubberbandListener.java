package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.RubberbandEvent;

public interface RubberbandListener extends EventListener {

  public boolean handleRubberbandEvent(RubberbandEvent evt);

}


