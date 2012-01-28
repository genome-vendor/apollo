package apollo.gui.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import apollo.util.FeatureList;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.DetailInfo;
import apollo.gui.Selection;

import apollo.gui.ApolloJalviewEventBridge;
import jalview.gui.AlignFrame;
//import jalview.gui.Controller;

/** menu items for  TiersPopupMenu and AnnotationMenu for showing alignments. 
    Provides 2 menu items.
    One for aligning selection and one for aligning all features that 
    are of the same type and in the
    same region as the selection.
*/

class AlignMenuItems {
  
  private FeatureSetI annotTop;
  private FeatureSetI resultTop;
  private Selection selection;
  private apollo.gui.Controller apolloController;

  AlignMenuItems(FeatureSetI annots,FeatureSetI results,Selection selection,
               apollo.gui.Controller c) {
    this.annotTop = annots;
    this.resultTop = results;
    this.selection = selection;
    this.apolloController = c;
  }

  JMenuItem getAlignSelectionMenuItem() {
    JMenuItem alignSelection = new JMenuItem("Align selected features...");
    alignSelection.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) { alignSelection(); } 
      } );
    if (getSelectedFeatures().size() == 0) 
      alignSelection.setEnabled(false);
    return alignSelection;
  }

  JMenuItem getAlignRegionMenuItem() {
    JMenuItem alignRegion = new JMenuItem("Align same type features in region...");
    alignRegion.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) { alignRegion(); } 
      } );
    if (getSelectedFeatures().size() == 0) 
      alignRegion.setEnabled(false);
    return alignRegion;
  }


  /** Send selection to jalview.  */
  private void alignSelection() {
    FeatureList feats = filterFeatures(getSelectedFeatures());
    if (feats.size() == 0) return;
    putUpJalview(feats);
  }

  private FeatureList getSelectedFeatures() {
    FeatureList feats = selection.getListDescendedFromModel(resultTop,
                                                            true);
    feats.addAllFeatures(selection.getListDescendedFromModel(annotTop,
                                                             true));
    return feats;
  }

  /** filter for hasAlignmentSequence and in refSeqRegion */
  private FeatureList filterFeatures(FeatureList selectedFeats) {
    // check for presence of main genomic sequence (?)
    // do we still want alignments if the curation set has no genomic
    // if so jalview has to deal with this case??
    
    // filter out those without alignments
    FeatureList feats = selectedFeats.getAllLeaves().getFeatsWithAlignments();
    if (feats.size() == 0) {
      String m = "None of the features selected contain alignments";
      JOptionPane.showMessageDialog(null,m,"Warning",JOptionPane.WARNING_MESSAGE);
      return feats;
    }

    // filter out those who are not fully in the region
    int featsPreviousSize = feats.size();
    feats = feats.getFeatsWithinRefSeqRegion();
    // if any filtered out for out of region bring up warning per sima request
    String msg = null;
    if (feats.size() == 0) 
      msg = "The features selected were outside of the entry region";
    else if (feats.size() < featsPreviousSize)
      msg = "Some of the features selected were outside of the entry region. " +
	" These will not be displayed in the alignment";
    if (msg != null)
      JOptionPane.showMessageDialog(null,msg,"Warning",JOptionPane.WARNING_MESSAGE);

    return feats;
  }

  private void putUpJalview(FeatureList feats) {
    if (feats.size() == 0) {
      // put in a popup?
      System.out.println("No feats to align"); 
      return; 
    }
    // consolidates feature pairs into feature sets if all fps are present
    // jalview will be consolidating anything thats part of the same transcript
    //feats = Selection.getConsolidatedFeatures(feats);
    // false - dont exit on close, true - read only!
    AlignFrame alignFrame = new AlignFrame(feats,false,true);
    alignFrame.setSize(750,300);

    alignFrame.setTitle("AlignFrame");
    // What is this doing? is this a work in progress?
    // the bridge is meant to send events between apollo and jalview - its a work in
    // progress - does nothing at the moment
    new ApolloJalviewEventBridge(apolloController,
                                 alignFrame.getController(),
                                 alignFrame);
    alignFrame.show();

  }

  /** Send all feats with same type as selection and in selected region to
      jalview */
  private void alignRegion() {
    FeatureList feats = getFeatsInRegion(resultTop);
    feats.addAllFeatures(getFeatsInRegion(annotTop));
    feats = filterFeatures(feats);
    putUpJalview(feats);
  }

  private FeatureList getFeatsInRegion(FeatureSetI topModel) {
    FeatureList feats = new FeatureList();
    // strand? result/annot?
    HashSet types = selection.getSelectedVisualTypes();
    // return either the basic FeaturePairs or Exon models
    FeatureList selFeats = selection.getListDescendedFromModel(topModel,
                                                               true);
    if (!selFeats.isEmpty()) {
      RangeI featsRange = selFeats.getRangeOfWholeList();
      feats = getFeatsInRegion(topModel,featsRange,types);
    }
    return feats;
  }

  /** Gets all features on the strand of TierPopupMenu's view's strand that are 
      between the low and high of the selection.
      Has to be completely contained (no overhangers). Should this be a method
      of FeatureList? 
      Recurses and gets feats in range on range's strand, that have type in types.
      Needs to also check if has alignment sequence 
  */
  private FeatureList getFeatsInRegion(SeqFeatureI feat,
				       RangeI range,
				       HashSet types) {
    FeatureList feats = new FeatureList();
    if (feat.canHaveChildren()) { // FeatureSetI
      for (int i=0; i<feat.getNumberOfChildren(); i++) {
	FeatureList overlappingKids = 
	  getFeatsInRegion(feat.getFeatureAt(i),range,types);
	feats.addAllFeatures(overlappingKids);
      }
    }
    // there are featureSetIs with no children it seems
    else if (feat.hasAlignable()) { // is a leaf (not a FeatureSetI)
      // if meets all criteria add to list - otherwise return empty list
      if (feat.getLow() < range.getLow()) return feats; // empty list
      if (feat.getHigh() > range.getHigh()) return feats;
      if (feat.getStrand() != range.getStrand()) return feats;
      // check types
      if (!featureHasSelectedType(feat,types)) return feats;
      // sequence check - does this cause an ensembl lazy seq load?
      // this is needed for game as not all game feats have align seg
      // eg the virtual est contigs
      // success this is something to hold onto
      feats.addFeature(feat);
    }
    return feats;
  }
  
  /** Return true if feature has one of the types in HashSet */
  private boolean featureHasSelectedType(SeqFeatureI feat,HashSet types) {
    // DetailInfo just calls the property scheme's feature property
    return types.contains(DetailInfo.getPropertyType(feat));
  }

  void clear() { 
    annotTop = null; 
  }

}

