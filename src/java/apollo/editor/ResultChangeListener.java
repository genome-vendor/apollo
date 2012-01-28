package apollo.editor;

import java.util.EventListener;

public interface ResultChangeListener extends EventListener {

  public boolean handleResultChangeEvent(ResultChangeEvent evt);

}


