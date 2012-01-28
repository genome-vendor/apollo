package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.ColumnSelectionEvent;

public interface ColumnSelectionListener extends EventListener {

  public boolean handleColumnSelectionEvent(ColumnSelectionEvent evt);

}
