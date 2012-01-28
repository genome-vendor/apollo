package jalview.gui;

import jalview.datamodel.*;

import java.util.*;

public class Selection {
  Vector selected = new Vector();

  public void addElement(DrawableSequence seq) {
    addElement((AlignSequenceI)seq);
  }
  public void addElement(AlignSequenceI seq) {
    selected.addElement(seq);
  }

  public void removeElement(DrawableSequence seq) {
    removeElement((AlignSequenceI)seq);
  }

  public void clear() {
    selected.removeAllElements();
  }

  public void removeElement(AlignSequenceI seq) {
    if (selected.contains(seq)) {
      selected.removeElement(seq);
    } else {
      System.err.println("WARNING: Tried to remove AlignSequenceI NOT in Selection");
    }
  }

  public boolean contains(DrawableSequence seq) {
    return contains((AlignSequenceI)seq);
  }
  public boolean contains(AlignSequenceI seq) {
    return selected.contains(seq);
  }

  public AlignSequenceI sequenceAt(int i) {
    return (AlignSequenceI)selected.elementAt(i);
  }

  public int size() {
    return selected.size();
  }

  public Vector asVector() {
    return selected;
  }

  public void selectAll(AlignmentI align) {
    for (int i=0;i<align.getSequences().size();i++) {
      AlignSequenceI seq = align.getSequenceAt(i);
      if (!contains(seq)) {
        addElement(seq);
      }
    }
  }
}
