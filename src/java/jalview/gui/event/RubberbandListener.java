package jalview.gui.event;

import java.util.EventListener;
import jalview.gui.event.RubberbandEvent;

public interface RubberbandListener extends EventListener {

    public boolean handleRubberbandEvent(RubberbandEvent evt);

}

	
