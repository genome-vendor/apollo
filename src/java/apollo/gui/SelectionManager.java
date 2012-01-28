package apollo.gui;

import java.util.Vector;
import apollo.util.FeatureList;
import apollo.datamodel.SeqFeatureI;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.FeatureChangeEvent;

/**
 * Controller for selection. This is only partially implemented at this point.
 * The idea would be that this would be the controller for selection,
 * replacing Controller for just selection (not everything else it controls.
 * SelectionManager deals with selection and deselection where Controller
 * only passes along selects and does not deal with deselecting when new
 * selects come in. When SelectionManager gets a new select it does the
 * appropriate deselecting to accompnay this.
 *
 * note from cvs notes that i thought would be clariflying to put here:
 Fixed slowness of selection in the views. When i changed selection to
be model based the views were just sending out the model selected
and then receiving that model and having to refind the associated
view which was obviously inefficient. I managed to solve this issue and
keep selection model based (rather than having drawables as the data
in the SelectionItems) by attaching the drawables as selectionListeners
to the SelectionItems which hold the model(SeqFeatureI) of the drawable.
So the SelectionItems are told to select and deselect (by the
SelectionManager via Selection) and the SelectionItems tell their
drawable selection listeners to select and deselect, thus avoiding
having to refind them at handleFeatureSelectionEvent. At the moment
only the ApolloPanel(and its views) participate in putting
selectionListeners on the SelectionItems, but clearly any other selection
source could do the same, but they probably dont need to since they dont
have the intense selection needs that ApolloPanel has.
ApolloPanel.handleFeatureSelectionEvent doesnt search for drawables if
the event source is itself.
Im thinking this should be in a apollo.controller package.
 */

public class SelectionManager implements ControlledObjectI {

  // What is currently selected - apollo panel should be accessing this rather
  // than vice versa
  Selection currentlySelected = new Selection();

  //private ApolloPanel apolloPanel;
  private Controller controller; // for now

  public SelectionManager() {}

//   /** Temp method til full implementation */
//   public void setApolloPanel(ApolloPanel ap) {
//     apolloPanel = ap;
//   }

  public void select(FeatureList feats,
                     boolean exclusiveSelection,
                     boolean selectParents,
                     Object source) {
    if (exclusiveSelection)
      clearSelections();
    if (selectParents)
      feats = feats.getParents();

    // adding to selection sets dsf.setSelected
    // !exclusiveSelection becomes deselectIfAlreadySelected in Selection.add
    currentlySelected.add(feats, source, !exclusiveSelection);
    // eventually wont be necasary
    // replace with selectionListener.select(selectionEvent)
    fireFeatureSelectionEvent(currentlySelected, source, false);
  }

  /** Process selection of Selection
      @param newSelection Selection to add to or replace old selection
      @param exclusiveSelection whether to add or replace old selection
      @param source source(view) of the selection, gets put in sel event
  */
  public void select(Selection newSelection,
                     boolean exclusiveSelection,
                     Object source) {
    // true -> fire feature selection event
    if (exclusiveSelection)
      clearSelections();
    /* Select the new selection
       this causes SelectionItems to notify select listeners
       for now thats just the drawables from FeatureViews to avoid
       having to refind them apon receiving the selection event */
    newSelection.select();
    // !exclusiveSelection becomes deselectIfAlreadySelected in Selection.add
    currentlySelected.add(newSelection, !exclusiveSelection);

    fireFeatureSelectionEvent(currentlySelected, source, false);
  }

  public void select(SeqFeatureI feat,Object source) {
    FeatureList fl = new FeatureList();
    fl.addFeature(feat);
    select(fl,true,false,source);
  }

  public void select(Vector features,Object source) {
    FeatureList list = new FeatureList();
    list.addVector(features);
    select(list,true,false,source);
  }

  /** Just adds newSelection to the current selection. Similar to doing a non
      exclusive select except there is no firing of a selection event.
      This is handy for SiteView slipping in a codon from transcript selection,
      no need to trigger another selection event(which can get sluggish with large
      selections. (An alternate approach we may want to consider is only firing
      new stuff on non-exclusive selection, possible now with exclusivity in
      selection event)
  */
  public void addToCurrentSelection(Selection newSelection) {
    newSelection.select();
    currentlySelected.add(newSelection);
  }

  public Selection getSelection() {
    return currentlySelected;
  }

  /** clear() is called on the currently selected Selection. Selection calls
      deselect on all of its SelectionItems. SelectionItem calls
      setSelected(false) on all of its selection listeners, and the
      drawable is a selection listener. This is how drawables get deselected
  */
  public void clearSelections() {
    currentlySelected.clear(); // site view selections included in this
    // apolloPanel.clearEdges(); this should not have to be called here
  }

//   public ApolloPanel getApolloPanel(){
//     return apolloPanel;
//   }//end getApolloPanel

  /**
   * Part of ControlledObjectI interface
   **/
  public void setController(Controller c) {
    controller = c;
    // to listen for annotation deletions
    controller.addListener(new SelectionAnnotationChangeListener());
  }

  /**
   * Part of ControlledObjectI interface
   **/
  public Controller getController() {
    return controller;
  }

  /**
   * Part of ControlledObjectI interface. If we've been set up with an ApolloPanel,
   * use it to find the ControllerWindow. Otherwise return null.
   **/
  public Object getControllerWindow(){
//     if(getApolloPanel()!=null){
//       return getApolloPanel().getControllerWindow();
//     }else{
      return null;
//    }
  }

  /**
   * Part of ControlledObjectI interface
   **/
  public boolean needsAutoRemoval(){
    return true;
  }

  private void fireFeatureSelectionEvent(Selection sel,
                                         Object source,
                                         boolean force_selection) {
    FeatureSelectionEvent e = new FeatureSelectionEvent(source,
                                                        sel,
                                                        force_selection);
    controller.handleFeatureSelectionEvent(e);
  }


  /** Listens for annotation deletions. If something in current selection is
      deleted, takes deleted feat out of selection and sends out a selection
      event with the modified selection. what will happen with empty
      selections? Should empty selection be synonomous with deselect?
      or should there be an explicit deselectAll? No- empty selections
      should be synonomous with deselectAll
  */
  private class SelectionAnnotationChangeListener
    implements AnnotationChangeListener {
    public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
      // The feature deleted is the "second feature" of the evt
      // Only care about deletions
      if (evt.isDelete()) {
        // Only care if deletion affects selection
        SeqFeatureI changedAnnot = evt.getChangedAnnot();
        if (!currentlySelected.containsFeature(changedAnnot))
          return false;
        /* It is inappropriate to make fire off changes to the
           selection here because other listeners for the annotation
           change event have not yet had a chance to reflect
           this change. The source that originated the delete
           must also deliver a selection event for DELETE and REPLACE
           fireFeatureSelectionEvent(currentlySelected, evt.getSource(), true);
        */
        currentlySelected.removeFeature(changedAnnot);
      }
      // translation should not be replace - fix
//       else if (evt.isReplace()) {
//         SeqFeatureI oldAnnot = evt.getReplacedFeature();
//         SeqFeatureI newAnnot = evt.getChangedFeature();
//         currentlySelected.replaceFeature(oldAnnot, newAnnot);
//       }
      else if (evt.isAdd()) {
        // does add evidence nned to be filtered out? not sure? is it still used?
        // this might not work as other listeners may not have gotten the ADD yet???
        // should probably change selection but not fire it off until get
        // REDRAW/MODEL_CHANGE
        select(evt.getAddedFeature(),evt.getSource());
      }

/* if (evt.getType() == AnnotationChangeEvent.ADD &&
            evt.getSubType() != AnnotationChangeEvent.EVIDENCE) {
        SeqFeatureI sf = evt.getSecondFeature();
        if (!currentlySelected.containsFeature(sf))
          currentlySelected.add(new SelectionItem(null, sf));
          } */
      return true;
    }
  }

}
