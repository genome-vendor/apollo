package apollo.editor;

import java.util.*;

import apollo.util.FeatureList;
import apollo.datamodel.*;
import apollo.gui.Controller;
import apollo.gui.ControlledObjectI;
//import apollo.gui.event.*;

/**

TransactionManager does the real coalescing for saving transactions.
This is more like the place where events are stored before firing now.
Renamed ChangeList (used to be AnnotationChangeCoalescer)

 * The AnnotationChangeCoalescer holds all the AnnotationChangeEvents for
 * the current edit along with a representation of the
 * feature tree before the edit began.
 * <BR> <BR>
 * <b>NOTE: THIS CLASS IS NOT FULLY IMPLEMENTED.</b>
 * <BR> <BR>
 * As edits are added to the AnnotationChangeCoalescer it checks to see
 * if they can be coalesced - for example if you delete all the
 * exons in a transcript and then delete the transcript the edit
 * can be coalesced into a delete transcript - all the delete
 * exon AnnotationChangeEvents can be removed from the changes
 * vector.
 * <BR> <BR>
 * A new AnnotationChangeCoalescer should be created at the start of each
 * edit in the AnnotationEditor because it is important that
 * the tree representing the underlying features (the origTree)
 * is current. After that it is simply a matter of adding changes
 * using addChanges. At the end of the edit executeChanges is
 * called to send the AnnotationChangeEvents to all the listeners.
 * <BR> <BR>
 * Remember that the Drawable tree will be in tht origTree state
 * until executeChanges is called. This is VERY important for
 * deciding which edits are necessary.
 * <BR> <BR>
 * delete exon <BR>
 *   Check if its in the add list <BR>
 *   Look to see if any of the current parents of the exon have <BR>
 *   been deleted. <BR>
 * delete transcript <BR>
 *   Check if its in the add list <BR>
 *   Look to see if any of the current parents of the transcript <BR>
 *   have been deleted. <BR>
 *   Look to see if any of the previous children of the  <BR>
 *   transcript have been deleted - remove those. <BR>
 * delete gene <BR>
 *   Check if its in the add list <BR>
 *   Look to see if any of the previous children of the  <BR>
 *   gene have been deleted - remove those. <BR>
 * I dont think any of the add checks are currently happening, just the
 * delete checks. <BR>
 * add exon <BR>
 *   Add to add list <BR>
 *   Look to see if any of the current parents of the exon have <BR>
 *   already been added.  <BR>
 *   Could do error check if add to parents which have already  <BR>
 *   been deleted. <BR>
 * add transcript <BR>
 *   Add to add list <BR>
 * add gene <BR>
 *   Add to add list <BR>
 * other exon <BR>
 * other transcript <BR>
 * other gene <BR>
 The only reason i can see that this implements ControlledObjectI is so
 SyntenyLinkPanel can avoid firing at its controller - but ACC is not even
 the event source - so actually I dont think being a ControlledObjectI serves any
 purpose as getControllerWindow is null.
Not sure about this but this could eventually just be a list of transactions(?)
and convert to change events at firing time.
 */
public class ChangeList implements ControlledObjectI {
  /** List of AnnotationChangeEvents */
  private Vector changes = new Vector();
  private Controller          controller;
  private AnnotationEditor annotEditor;

  /** for gene edit panel */
  public ChangeList(Controller c) {
    setController(c);
  }

  ChangeList(AnnotationEditor editor,Controller c) {
    annotEditor = editor;
    setController(c);
  }
  
  public Controller getController() {
    return controller;
  }

  public void setController(Controller c) {
    controller = c;
  }

  public Object getControllerWindow() {
    return null;
  }

  /** Since getControllerWindow is null, needsAutoRemoval is meaningless */
  public boolean needsAutoRemoval() {
    return true;
  }

  /** Returns true if coalescer contains changes, changes vector is not empty */
  boolean hasChanges() {
    return changes.size() > 0;
  }

  /** Add change to end of changes list */
  public void addChange(AnnotationChangeEvent evt) {
    if (evt == null)
      return;
    changes.addElement(evt);
  }

  void addTransaction(Transaction trans) {
    trans.setSource(annotEditor);
    // eventually should just store transactions -> TransactionList?
    // or is that redundant of CompoundTransaction?
    addChange(trans.generateAnnotationChangeEvent());
  }

  public void addChangeList(ChangeList changeList) {
    if (changeList == null)
      return;
    for (int i=0; i<changeList.size(); i++)
      addChange(changeList.getChange(i));
  }

  private int size() {
    return changes.size();
  }

  private AnnotationChangeEvent getChange(int i) {
    return (AnnotationChangeEvent)changes.get(i); // check w size...
  }

  /** Insert change into change list at postion */
  void insertChange(int position,AnnotationChangeEvent evt) {
    changes.add(position,evt);
  }


  private void addTranscriptAndGeneRangeChanges() {
    if (annotEditor == null)
      return;

    CompoundTransaction ranges = new CompoundTransaction(annotEditor);

    // transcripts
    CompoundTransaction transRanges = 
      addAnnotRangeChanges(annotEditor.getOldTranscripts(),annotEditor.getNewTranscripts());
    if (transRanges!=null && transRanges.hasKids())
      ranges.addTransaction(transRanges);

    // genes/annots
    CompoundTransaction annotRanges =
      addAnnotRangeChanges(annotEditor.getOldTopAnnots(),annotEditor.getNewTopAnnots());

    if (annotRanges != null && annotRanges.hasKids())
      ranges.addTransaction(annotRanges);

    /*
    if (ranges.hasKids()) {
      // This is a little funny but if there is only one compound transaction that
      // add this transaction to the compound trans. otherwise give it its own event.
      // I think ultimately it would be nice if change list just dealt with one 
      // single compound trans, rather than a list of changes.
      // this is handy for splits & merges where the range change belongs with 
      // the compound trans.
      if (haveOnlyOneCompoundChange()) {
        getChange(0).addTransaction(ranges);
      }
      // more than one change, or its not compound, so just add own event.
      else {
        addChange(ranges.generateAnnotationChangeEvent());
      }
    }
    */

    annotEditor.synchOldAnnots();
  }


  private boolean haveOnlyOneCompoundChange() {
    return size() == 1 && getChange(0).isCompound();
  }

  /** annot lists are either gene or transcript lists
   Chaecks for range changes from old to new. Also if a transcript checks if translation
   limits have changed. */
  private CompoundTransaction addAnnotRangeChanges(FeatureList oldAnnots,
                                                   FeatureList newAnnots) {
    if (oldAnnots == null || newAnnots == null) return null; // happens w test case
    CompoundTransaction compTrans = new CompoundTransaction(annotEditor);
    TransactionSubpart ts = TransactionSubpart.LIMITS;
    for (int i=0; i<oldAnnots.size(); i++) {
      SeqFeatureI oldAnnot = oldAnnots.getFeature(i);
      SeqFeatureI newAnnot = newAnnots.getFeatWithName(oldAnnot.getName());
      if (newAnnot == null)
        continue; // oldAnnot not present in new - must be a new annot - skip
      if (hasBeenDeleted(newAnnot)) // if annot has been deleted skip it
        continue;
      AnnotatedFeatureI af = newAnnot.getAnnotatedFeature();
      if (!oldAnnot.sameRange(newAnnot)) {
        //AnnotationUpdateEvent a = new AnnotationUpdateEvent(annotEditor,af,ts);
        //a.setOldRange(oldAnnot);
        //addChange(a);
        UpdateTransaction ut = new UpdateTransaction(af,ts);
        ut.setOldSubpartValue(oldAnnot); // RangeI
        compTrans.addTransaction(ut);
      }
      if (newAnnot.isTranscript() 
          && !translationRangeEqual(oldAnnot,newAnnot)) {
        RangeI oldRange = oldAnnot.getTranslation().getTranslationRange();
        Transaction t = annotEditor.updatePeptideRange(newAnnot,oldRange);
//         TransactionSubpart t = TransactionSubpart.PEPTIDE_LIMITS;
//         //AnnotationUpdateEvent a = new AnnotationUpdateEvent(annotEditor,af,t);
//         //a.setOldRange(oldAnnot.getTranslation().getTranslationRange());
//         //addChange(a);
//         UpdateTransaction ut = new UpdateTransaction(af,t);
//         ut.setOldSubpartValue(oldAnnot.getTranslation().getTranslationRange());
        compTrans.addTransaction(t);
      }
    }
    if (!compTrans.hasKids())
      return null; // no range changes
    return compTrans;
  }

  /** Return true if feat has been deleted by seeing if feats refFeat still holds it*/
  private boolean hasBeenDeleted(SeqFeatureI sf) {
    return sf.getRefFeature().getFeatureIndex(sf) == -1;
  }
  
  // SeqFeatureI method?
  private boolean translationRangeEqual(SeqFeatureI oldAnnot,SeqFeatureI newAnnot) {
    RangeI r = oldAnnot.getTranslation().getTranslationRange();
    return r.sameRange(newAnnot.getTranslation().getTranslationRange());
  }
  
  /** If both parent and child are set for a delete, removes child delete.
      root is currently not used - take out? whats it for? 
      this doesnt seem to work anymore and transaction manager dupllicates this
      functionality so i think it should be ousted */
  private void coalesceChanges(SeqFeatureI root) {
    for (int i=0; i < size(); i++) {
      AnnotationChangeEvent parent_evt = getChange(i);
      if (parent_evt.isDelete()) {
        SeqFeatureI parent = parent_evt.getAnnotTop().getRefFeature();
        for (int j = i + 1; j < size(); j++) {
          AnnotationChangeEvent child_evt = getChange(j);
          if (child_evt.getAnnotTop() == parent &&
              child_evt.isDelete()) {
            System.out.println("FCC: Redundant delete of " +
                               parent.getName());
            changes.removeElementAt(j);
          }
        }
      }
    }
  }

    

  /** calls coalesceChanges which seems to be broken. puts redraw event at end
      fires all events - only one redraw event fired. */
  public void executeChanges() {

    addTranscriptAndGeneRangeChanges();

    coalesceChanges(null);

    AnnotationChangeEvent ace = null;
    for (int i = 0; i <size(); i++) {
      AnnotationChangeEvent evt = getChange(i);
      if (evt.getOperation() == FeatureChangeEvent.REDRAW) {
        ace = evt; // save redraw for end
      } else {
        fireAnnotationChangeEvent(evt);
        //changes.removeElementAt (i);
      }
    }
    changes.clear(); // is clearing problematic?
    if (ace != null) {
      //	    System.out.println ("Carried out " + ace.toString());
      fireAnnotationChangeEvent(ace);
    }
  }

  private void fireAnnotationChangeEvent(AnnotationChangeEvent evt) {
    getController().handleAnnotationChangeEvent(evt);
  }

  /**
   * Workaround to correct exon UpdateParentTransactions.  Only affects
   * PureJDBCTransactionWriter.  Calls the method of the same name on
   * each Transaction in the ChangeList.
   *
   * @param updateAllIds If false then only cloned feature ids/names that are 
   * either null or "no_name" will be updated.  If true then all cloned seq feature ids
   * and names will be updated (but not parent feature ids/names.)
   * @param updateAllParentIds Same as <code>updateAllIds</code>, but applies only to
   * cloned parent features.
   */ 
  void updateClonedTranscriptIdsAndNames(boolean updateAllIds, boolean updateAllParentIds) {
    int nec = size();
    for (int i = 0;i < nec; ++i) {
      AnnotationChangeEvent ace = getChange(i);
      Transaction ect = ace.getTransaction();
      ect.updateClonedTranscriptIdsAndNames(ect, updateAllIds, updateAllParentIds);
    }
  }

}

//   void removeUpdateParentEvent(SeqFeatureI updatedFeat) {
//     for (int i=0; i < size(); i++) {
//       AnnotationChangeEvent ace = getChange(i);
//       if (ace.isMove() && ace.getOldId().equals(updatedFeat.getId()));
//         changes.remove(i);
//     }
//   }

//   void removeDeleteEvent(RangeI deletedFeat) {
//     for (int i=0; i < size(); i++) {
//       AnnotationChangeEvent ace = getChange(i);
//       if (ace.isDelete() && ace.getChangedFeature().isIdentical(deletedFeat))
//         changes.remove(i);
//     }
//   }
//   /** This goes through the change events in the OPPOSITE order of how they were
//       entered. Why is that? It seems counter intuitive so im gonna change it to
//       do it in the order entered. Can the changer have events added to it in the
//       middle of executeChanges? Redraw happens at end */
//   public void executeChangesOLD() {
//     coalesceChanges(null);
//     AnnotationChangeEvent ace = null;
//     for (int i = changes.size() - 1; i >= 0; i--) {
//       AnnotationChangeEvent evt
//       = (AnnotationChangeEvent) changes.elementAt(i);
//       if (evt.getOperation() == FeatureChangeEvent.REDRAW) {
//         ace = evt;
// 	// why is this change not removed? should it be?
//       } else {
//         fireAnnotationChangeEvent(evt);
//         changes.removeElementAt (i);
//       }
//     }
//     if (ace != null) {
//       //	    System.out.println ("Carried out " + ace.toString());
//       fireAnnotationChangeEvent(ace);
//     }
//   }
