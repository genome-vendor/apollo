package apollo.gui.event;

import java.util.EventObject;
import apollo.datamodel.SeqFeatureI;
import apollo.util.FeatureList;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import java.util.*;

public class FeatureSelectionEvent extends EventObject {
  private Selection selected;
  /** whether this event requires attention. some of the i/fs
      have a checkbox to ignore changes to selection, but if
      the change to the selection is a delete or a replace then
      the change must not be ignored */
  private boolean force_selection = false;

  //private Controller sourceSpeciesController;//controller selection originated from

  // Maybe sequence selection should be a separate event SequenceSelectionEvent
  public static final int FEATURES = 1;
  public static final int SEQUENCE = 2;
  public static final int SYNLINK  = 3;

  // This is a hack to stop sequence selections taking for ever
  int type = FEATURES;

  public FeatureSelectionEvent(Object source,Vector features, int type) {
    this(source,features);
    this.type = type;
  }

  public FeatureSelectionEvent(Object source, FeatureList feats) {
    super(source);
    init(feats);
  }

  public FeatureSelectionEvent(Object source,SeqFeatureI feature) {
    super(source);
    //Vector v = new Vector();
    FeatureList f = new FeatureList(feature,false);
    init(f);
  }

  public FeatureSelectionEvent(Object source,Vector features) {
    super(source);
    init(features);
  }

  public FeatureSelectionEvent(Object source,Selection selected) {
    super(source);
    this.selected = selected;
  }

  public FeatureSelectionEvent(Object source, Selection selected, 
			       boolean force_selection) {
    this(source,selected);
    this.force_selection = force_selection;
  }

  private void init(FeatureList feats) {
    selected = new Selection(feats,source);
  }

  private void init(Vector features) {
    Selection s = new Selection();

    int fSize = features.size();
    for (int i=0; i<fSize; i++) {
      s.add(new SelectionItem(source, (SeqFeatureI)features.elementAt(i)));
    }
    this.selected = s;
  }

  public int getType() {
    return type;
  }
  //public Vector getFeatures() {
  public FeatureList getFeatures() {
    return selected; // a Selection is a FeatureList 
  }
//     Vector selVec   = selected.getSelected();
//     Vector features = new Vector();

//     for (int i=0; i<selVec.size(); i++) {
//       SelectionItem si = (SelectionItem)selVec.elementAt(i);
//       if (si.getData() instanceof SeqFeatureI) {
//         features.addElement(si.getData());
//       }
//     }
//     return new FeatureList(features);
//  }

  /** Returns fist feature - rename getFirstFeature */
  public SeqFeatureI getFeature() {
    //Vector features = getFeatures();
    FeatureList features = getFeatures();

    if (features.size() > 0) {
      return features.getFeature(0);
    } else {
      return null;
    }
  }

  public Selection getSelection() {
    return selected;
  }

  public Object getSource() {
    return source;
  }

  /** whether this event requires attention. some of the i/fs
      have a checkbox to ignore changes to selection, but if
      the change to the selection is a delete or a replace then
      the change must not be ignored */
  public boolean forceSelection() {
    return force_selection;
  }

}
