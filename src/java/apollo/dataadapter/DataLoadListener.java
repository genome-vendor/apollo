package apollo.dataadapter;

import java.util.EventListener;

public interface DataLoadListener extends EventListener {

  public boolean handleDataLoadEvent(DataLoadEvent evt);
}
