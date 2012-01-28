package jalview.gui.event;

import jalview.gui.*;
import apollo.datamodel.SequenceEdit;

import java.util.EventObject;

public class EditEvent extends EventObject {
  SequenceEdit edit;

  public EditEvent(Object source, SequenceEdit edit) {
    super(source);
    this.edit = edit;
  }

  public SequenceEdit getEdit() {
    return edit;
  }

  public Object getSource() {
    return source;
  }
}
