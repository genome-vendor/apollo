package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.ReverseComplementEvent;

public interface ReverseComplementListener extends EventListener {

  public boolean handleReverseComplementEvent(ReverseComplementEvent evt);

}
