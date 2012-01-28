package apollo.editor;

import java.util.EventListener;

public interface FeatureChangeListener extends EventListener {

  public boolean handleFeatureChangeEvent(FeatureChangeEvent evt);

}


