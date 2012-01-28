package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.TierManagerEvent;

public interface TierManagerListener extends EventListener {

  public boolean handleTierManagerEvent(TierManagerEvent evt);

}
