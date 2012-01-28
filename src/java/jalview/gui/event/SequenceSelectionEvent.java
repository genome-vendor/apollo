package jalview.gui.event;

import java.util.*;
import jalview.gui.Selection;

public class SequenceSelectionEvent extends EventObject {
  Selection sel;

  public SequenceSelectionEvent(Object source, Selection sel) {
    super(source);
    this.sel = sel;
  }

  public Selection getSelection() {
    return sel;
  }

  public Object getSource() {
    return source;
  }
}
