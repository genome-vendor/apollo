package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.TreeSelectionEvent;

public interface TreeSelectionListener extends EventListener {

  public boolean handleTreeSelectionEvent(TreeSelectionEvent evt);

}
