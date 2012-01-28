package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.SchemeChangeEvent;

public interface SchemeChangeListener extends EventListener {

  public boolean handleSchemeChangeEvent(SchemeChangeEvent evt);

}
