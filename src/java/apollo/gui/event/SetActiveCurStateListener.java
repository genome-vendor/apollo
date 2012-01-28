package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.SetActiveCurStateEvent;

public interface SetActiveCurStateListener extends EventListener {

  public boolean handleSetActiveCurStateEvent(SetActiveCurStateEvent evt);
}
