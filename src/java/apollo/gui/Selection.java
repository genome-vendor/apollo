package apollo.gui;

import java.util.*;
import apollo.util.FeatureList;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.FeatureSetI;

/**
  Manages a selection of objects(a vector of SelectionItems),
  and for SelectableI's controls their selection state.
  A Selection may have both parent and child.
  getSelectionDescendedFromModel(SeqFeat,true) will filter out 
  parent child redundnancies. This is used in AnnotationEditor
 */
public class Selection extends FeatureList {
  public static final int FEATURE = 1;
  public static final int SET     = 2;

  /** Vector of SelectionItems */
  private Vector selectionItems = new Vector();
  /** Vector of SeqFeatureI data of SelectionItems, for convenience */
  //private Vector selectedData;
  //private FeatureList selectedData;

  public Selection() {
    //selectionItems     = new Vector(); 
    //selectedData = new Vector(); // -> FeatureList
    //selectedData = new FeatureList();
  }

  public Selection(FeatureList feats,Object source) {
    for (int i=0; i<feats.size(); i++) {
      add(new SelectionItem(source,feats.getFeature(i)));
    }
  }

  /**
   * I took out the boolean force, add is always an add
   and if its already it does nothing, in other words have it always be
   force == true, i dont think force==false was used at all
   Took out the return value of SelectionItem - dont think it was used.
   No longer checks if the new item descends from or is the ancestor of 
   anything in the selection - too slow on big selections. This is done 
   on demand with getSelectionDescendedFromModel(model,true)
   */
  public void add(SelectionItem item) {
    // Need to test if there is parents and kids in the same set!?
    // This is too costly on big selections (eg big rubberbands)
    // Have to go backwards as we are removing as we iterate
    if (!contains(item.getData())) {
      selectionItems.addElement(item);
      //selectedData.addFeature(item.getData());
      addFeature(item.getData());
    }
  }

  public void add(SelectionItem item, boolean deselectIfAlreadySelected) {
      if (deselectIfAlreadySelected && contains(item.getData())) {
	item.deselect();
        remove(item);
      }
      else {
        add(item);
      }
  }

  public void add(apollo.util.FeatureList feats, 
                  Object source,
                  boolean deselectIfAlreadySelected) {
    apollo.util.FeatureIterator iter = feats.featureIterator();
    while (iter.hasNextFeature()) {
      add(new SelectionItem(source, iter.nextFeature()), 
          deselectIfAlreadySelected);
    }
  }

  public void add(apollo.util.FeatureList feats, Object source) {
    add(feats, source, false);
  }

  public void add(Selection selection, boolean deselectIfAlreadySelected) {
    Vector selVec = selection.getSelected();

    for (int i=0;i<selVec.size();i++) {
      SelectionItem si = (SelectionItem)selVec.elementAt(i);
      // Why does a new SelectionItem need to be created/cloned?
      // Is it problematic to use the old one?
      add(new SelectionItem(si), deselectIfAlreadySelected);
    }
  }

  public void add(Selection selection) {
    add(selection, false);
  }

  public void removeFeature(SeqFeatureI feat) {
    int index = indexOf(feat); // -1 if not found
    if (index >= 0) {
      selectionItems.removeElementAt(index);
      this.remove(index);
    }
  }

  void remove(SelectionItem si) {
    int index = selectionItems.indexOf(si); // -1 if not found
    if (index >= 0) {
      selectionItems.removeElementAt(index);
      remove(index);
    }
    // Sometimes it doesn't seem to find the item in the selectionItems vector,
    // but looking for the data in SelectedData (which is what removeFeature
    // does) seems to work.
    else
      removeFeature(si.getData());
  }

  void replaceFeature(SeqFeatureI old_feat, SeqFeatureI new_feat) {
    int index = this.indexOf(old_feat); // -1 if not found
    if (index >= 0) {
      SelectionItem si = (SelectionItem) selectionItems.elementAt(index);
      si.setData(new_feat);
      this.remove(index);
      this.add(index,new_feat);
    } else {
      if (old_feat.canHaveChildren()) {
        FeatureSetI old_fs = (FeatureSetI) old_feat;
        FeatureSetI new_fs = (FeatureSetI) new_feat;
        for (int i = 0; i < old_fs.size(); i++) {
          replaceFeature(old_fs.getFeatureAt(i), new_fs.getFeatureAt(i));
        }
      }
    }
  }

  /** Vector of SelectionItems */
  public Vector getSelected() {
    return selectionItems;
  }

  public FeatureList getListDescendedFromModel(SeqFeatureI model,
                                               boolean check) {
    Selection sel = getSelectionDescendedFromModel(model, check);
    return (sel.getSelectedData());
  }

  /** Return a sub-Selection of the current Selection that are descendants of
      SeqFeatureI model.
      This should replace getSelectionForSource as it is independent of
      which view the selection originally came from.
  */
  public Selection getSelectionDescendedFromModel(SeqFeatureI model) {
    return getSelectionDescendedFromModel(model,false);
  }

  /** Return a sub-Selection of the current Selection that are descendants of
      SeqFeatureI model.
      This should replace getSelectionForSource as it is independent of
      which view the selection originally came from.
      If checkForRedundantDescendants is true then selection items whose
      parents/ancestors are already selected are filtered out.
      (Should the descend check be a separate method?)
      return a feat list?
  */
  public Selection getSelectionDescendedFromModel(SeqFeatureI model,
                                                  boolean checkForRedundantDescendants) {
    Selection descendants = new Selection();
    for (int i = 0; i < selectionItems.size(); i++) {
      SelectionItem si = (SelectionItem)selectionItems.elementAt(i);
      SeqFeatureI newFeat = si.getData();
      if (newFeat.descendsFrom(model)) {
        boolean notRedundantDescendant = true;
        if (checkForRedundantDescendants) {
          for (int j = descendants.size() - 1 ;
               j >= 0 && notRedundantDescendant;
               j--) {
            SeqFeatureI sf = descendants.getSelectedData(j);
            if (newFeat.descendsFrom(sf)) 
              notRedundantDescendant = false;
            if (sf.descendsFrom(newFeat))
              descendants.removeFeature(sf);
          }
        }
        if (notRedundantDescendant)
          descendants.add(si);
      }
    }
    return descendants;
  }
  
  /** Returns true if there is a SelectionItem in Selection with sf */
  public boolean containsFeature(SeqFeatureI sf) {
    return this.contains(sf);
  }

  /** Returns vector of selection items data of selection (SeqFeatureI's) 
      change this to return FeatureList */
  public FeatureList getSelectedData() {
    return this; 
      //return getSelectedData(selected);
  }
  // for backward compatibility - phase out eventually 
  public Vector getSelectedVector() {
    return this.toVector(); // FeatureList method
  }
  
//   public FeatureList getSelectedList() {
//     // for now wrap vector - eventually make vector a feat list
//     return new FeatureList(selectedData);
//   }

  /** The data of the selection items is a SeqFeatureI, model not drawable */
  public SeqFeatureI getSelectedData(int i) {
    return this.getFeature(i);
  }

  public SelectionItem getSelectionItem(int i) {
    return (SelectionItem)selectionItems.elementAt(i);
  }

  public SelectionItem getSelectionItem(SeqFeatureI sf) {
    int i = this.indexOf(sf);
    if (i >= 0)
      return (SelectionItem)selectionItems.elementAt(i);
    else
      return null;
  }

  public int size() {
    return selectionItems.size();
  }

  /** This does deselection. deselect() is called on all the SelectionItems
      which handle deselection */
  public void clear() {
    for (int i=0;i<size();i++) {
      SelectionItem si = getSelectionItem(i);
      si.deselect(); // this replaces getData().setSelected(false)
    }
    selectionItems.clear();
    super.clear();
  }

  /** Select all selection items in Selection */
  void select() {
    for (int i=0; i<size(); i++)
      getSelectionItem(i).select();
  }

  /** Consolidates features. eg if all exons in a transcript are in the
      selection, the transcript is added and the exons are removed.
      Presently this is not recursive, only does one level of processing
      as that is all that is needed, change if need be.
      This stops at "holders". In other words if we have selected all the
      genes on a strand it will not consolidate to the FeatureSet that is
      the holder of all those genes.
      @see #FeatureSetI.isHolder
      change to return FeatList?
  */
//   public Vector getConsolidatedFeatures() {
//     return getConsolidatedFeatures(this);
//   }
//   /** Consolidates SeqFeatureIs that are in Vector of feats - should probably go in
//       a util class(?) - move this to FeatureList */
//   public static Vector getConsolidatedFeatures(FeatureList feats) {
//     Vector consolidatedFeatures = new Vector(feats.size());
//     HashMap parentToNumberOfKids = new HashMap();
//     for (int i=0; i<feats.size(); i++) {
//       SeqFeatureI child = feats.getFeature(i);
//       SeqFeatureI parent = child.getParent();
//       // codons have null parents
//       if (parent == null) {
// 	consolidatedFeatures.add(child);
// 	continue;
//       }
//       Integer numKids = new Integer(1);
//       if (parentToNumberOfKids.containsKey(parent)) {
// 	numKids = (Integer)parentToNumberOfKids.get(parent);
// 	// need to be removed?
// 	numKids = new Integer(numKids.intValue() + 1);
//       }
//       parentToNumberOfKids.put(parent,numKids);

//       // if we have all of the parent's kids, remove kids and add parent
//       if (numKids.intValue() == parent.getNumberOfChildren()) {
// 	Vector kids = parent.getFeatures();
// 	for (int j=0; j<kids.size(); j++)
// 	  consolidatedFeatures.remove(kids.get(j));
// 	consolidatedFeatures.add(parent);
//       } else {
// 	// add for now - will get removed if parent added later
// 	// if already have parent(messy selection) then dont add child
// 	if (!consolidatedFeatures.contains(parent))
// 	  consolidatedFeatures.add(child);
//       }
//     }
//     return consolidatedFeatures;
//   }

  /** Returns a HashSet of Strings that is the unique set of visual types for
      the selection. This is not the logical types. SeqFeature.getType returns
      the logical type. DetailInfo.getPropertyType maps the logical type to 
      the visual type. The visual type is what is displayed in the evidence 
      panel and tiers window. Should this take in strand?
      put this in FeatureList?
  */
  public HashSet getSelectedVisualTypes() {
    HashSet visualTypes = new HashSet();
    for (int i=0; i<size(); i++) {
      SeqFeatureI feat = getSelectedData(i);

      String visualType = DetailInfo.getPropertyType(feat);
      // HashSet only adds if not already in there
      visualTypes.add(visualType);
    }
    return visualTypes;
  }

}
