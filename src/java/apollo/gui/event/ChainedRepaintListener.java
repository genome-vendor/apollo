package apollo.gui.event;

import java.util.EventListener;

public interface ChainedRepaintListener extends EventListener {
  public boolean handleChainedRepaint(ChainedRepaintEvent event);
}


