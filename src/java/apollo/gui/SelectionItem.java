package apollo.gui;

import java.util.*;
import apollo.datamodel.SeqFeatureI;


/**
 * A holder for a selected object, which also records the objects source.
 *
 * At the moment all selection is of SeqFeatureI's, so making the data 
 * variable an Object seems unnecasarily generic. Ive changed it to 
 * SeqFeatureI. In the future if we need to select more than just 
 * SeqFeatureI's we can change this to Object, or some interface that 
 * encompasses selection objects. But for now its nice to have it as a 
 * SeqFeatureI
 */
public class SelectionItem {
  Object source;
  SeqFeatureI data;
  private HashSet selectionListenerSet;

  public SelectionItem(Object source, SeqFeatureI data) {
    this.source = source;
    this.data   = data;
  }

  // This constructor makes a clone
  public SelectionItem(SelectionItem from) {
    source = from.getSource();
    data   = from.getData();
    this.selectionListenerSet = from.selectionListenerSet;
  }

  public Object getSource() {
    return source;
  }
  public SeqFeatureI getData() {
    return data;
  }

  public void setData(SeqFeatureI data) {
    this.data = data;
  }

  /** Deselects all selection listener SelectableIs */
  void deselect() {
    if (selectionListenerSet==null)
      return;
    Iterator it = selectionListenerSet.iterator();
    // Should this be sending out some sort of deselect event?
    while (it.hasNext())
      ((SelectableI)it.next()).setSelected(false);
  }

  /** Tell selection listeners to select */
  void select() {
    if (selectionListenerSet==null)
      return;
    Iterator it = selectionListenerSet.iterator();
    // Should this be sending out some sort of select event?
    while (it.hasNext())
      ((SelectableI)it.next()).setSelected(true);
  }

  public void addSelectionListener(SelectableI selectable) {
    if (selectionListenerSet==null)
      selectionListenerSet = new HashSet(2);
    selectionListenerSet.add(selectable);
  }
}
