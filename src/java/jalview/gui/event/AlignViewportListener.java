package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.AlignViewportEvent;

public interface AlignViewportListener extends EventListener {

  public boolean handleAlignViewportEvent(AlignViewportEvent evt);

}
