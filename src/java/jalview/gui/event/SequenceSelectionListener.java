package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.SequenceSelectionEvent;

public interface SequenceSelectionListener extends EventListener {

  public boolean handleSequenceSelectionEvent(SequenceSelectionEvent evt);

}
