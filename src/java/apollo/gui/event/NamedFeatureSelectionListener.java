package apollo.gui.event;

import java.util.EventListener;
import apollo.gui.event.NamedFeatureSelectionEvent;

public interface NamedFeatureSelectionListener extends EventListener {

    public boolean handleNamedFeatureSelectionEvent(NamedFeatureSelectionEvent evt);

}//end NamedFeatureSelectionListener


