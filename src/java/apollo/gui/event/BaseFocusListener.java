package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.BaseFocusEvent;

public interface BaseFocusListener extends EventListener {

  public boolean handleBaseFocusEvent(BaseFocusEvent evt);

}
