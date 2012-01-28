package apollo.gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * A TableCellEditor which disables edits in the cells.
 */
public class DisabledEditor implements TableCellEditor {
  public DisabledEditor() {}
  public void cancelCellEditing() {}
  public boolean isCellEditable(EventObject eo) {
    return false;
  }
  public Object getCellEditorValue() {
    return null;
  }

  public boolean shouldSelectCell(EventObject anEvent) {
    return false;
  }

  public boolean stopCellEditing() {
    return true;
  }

  public void addCellEditorListener(CellEditorListener l) { }

  public void removeCellEditorListener(CellEditorListener l) { }

  public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected,
      int row, int column) {
    return null;
  }
}
