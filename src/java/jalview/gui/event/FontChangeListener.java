package jalview.gui.event;

import java.util.EventListener;

import jalview.gui.event.FontChangeEvent;

public interface FontChangeListener extends EventListener {

  public boolean handleFontChangeEvent(FontChangeEvent evt);

}
