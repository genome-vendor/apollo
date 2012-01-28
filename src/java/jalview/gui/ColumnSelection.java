package jalview.gui;

import jalview.datamodel.*;

import java.util.*;

/**
 * NOTE: Columns are zero based.
 */
public class ColumnSelection {
  Vector selected = new Vector();

  public void addElement(int col) {
    selected.addElement(new Integer(col));
  }

  public void clear() {
    selected.removeAllElements();
  }

  public void removeElement(int col) {
    Integer colInt = new Integer(col);
    if (selected.contains(colInt)) {
      selected.removeElement(colInt);
    } else {
      System.err.println("WARNING: Tried to remove Integer NOT in ColumnSelection");
    }
  }

  public boolean contains(int col) {
    return selected.contains(new Integer(col));
  }

  public int columnAt(int i) {
    return ((Integer)selected.elementAt(i)).intValue();
  }

  public int size() {
    return selected.size();
  }

  public int getMax() {
    int max = -1;

    for (int i=0;i<selected.size();i++) {
      if (columnAt(i) > max) {
        max = columnAt(i);
      }
    }
    return max;
  }

  public int getMin() {
    int min = 1000000000;

    for (int i=0;i<selected.size();i++) {
      if (columnAt(i) < min) {
        min = columnAt(i);
      }
    }
    return min;
  }

  public Vector asVector() {
    return selected;
  }

  public void compensateForEdit(int start, int change) {
    for (int i=0; i < size();i++) {
      int temp = columnAt(i);
  
      if (temp >= start) {
        selected.setElementAt(new Integer(temp-change),i);
      }
    }
  }
}
