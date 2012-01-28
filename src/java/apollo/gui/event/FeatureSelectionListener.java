package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.FeatureSelectionEvent;

public interface FeatureSelectionListener extends EventListener {

  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt);

}


