package apollo.gui.event;

import apollo.datamodel.*;
import java.util.EventObject;

public class ReverseComplementEvent extends EventObject {

  public ReverseComplementEvent(Object source) {
    super(source);
  }

  public Object getSource() {
    return source;
  }
}


