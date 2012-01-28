package jalview.gui.event;

import jalview.gui.ColumnSelection;

import java.util.*;

public class ColumnSelectionEvent extends EventObject {
  ColumnSelection colSel;

  public ColumnSelectionEvent(Object source, ColumnSelection colSel) {
    super(source);
    this.colSel = colSel;
  }

  public ColumnSelection getColumnSelection() {
    return colSel;
  }

  public Object getSource() {
    return source;
  }
}
