/*
 * Created on Sep 15, 2004
 * This class is used to manage all transactions generated. 
 */
package apollo.editor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Transcript;
import apollo.dataadapter.CurationState;

import org.apache.log4j.*;

/**
 * @author wgm
 *  rename TransactionList?
 */
public class TransactionManager implements AnnotationChangeListener, Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TransactionManager.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** Hold all unflattened Transaction instances, includes compound trans */
  private List unflattenedTransactions;

  /** Holds all transactions in flattened form, compound trans have been flattened
      out to children */
  private List flattenedTransactions;

  // Default author for all transactions
  private String author;

  // To control if an newly added Transaction should be merged to ones
  // already added
  private boolean needCoalescence;
  // A flag to display if added Transaction instances are coalesced
  // Default should be true. It is assumed saved Transactions have already
  // been coalesced.
  private boolean isCoalescedDone = true;

  /** To fire undo events to */
  private transient AnnotationChangeListener undoListener;

  /**
   * Need this to get the name adapter for a feature.
   */
  private transient CurationState curationState;

  /**
   * Default constructor.
   */
  public TransactionManager() {
    author = UserName.getUserName();
    unflattenedTransactions = new ArrayList();
  }

//   /** not used
//    * An overloaded constructor with needCoalescence specified.
//    * 
//    * @param needCoalescence
//    *          true for coalescencing newly added Transaction
//    */
//   public TransactionManager(boolean needCoalescence) {
//     this();
//     this.needCoalescence = needCoalescence;
//   }

//   /** not used
//    * An overloaded constructor.
//    * 
//    * @param author
//    *          the author for all transactions
//    */
//   public TransactionManager(String author) {
//     setAuthor(author);
//     unflattenedTransactions = new ArrayList();
//   }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getAuthor() {
    return this.author;
  }

  public void addTransaction(Transaction trans) {
    unflattenedTransactions.add(trans);
    logger.debug("added transaction " + trans.oneLineSummary());
  }

  /**
   * A transaction is created based on the passed FeatureChangeEvent instance.
   * 
   * @param event
   */
  private void addTransaction(AnnotationChangeEvent event) {
// pase'
//     if (event instanceof AnnotationReplaceEvent) {
//       FeatureSet original = (FeatureSet) event.getReplacedFeature();
//       FeatureSet newFeature = (FeatureSet) event.getChangedFeature();
//       replaceSeqFeature(original, newFeature);
//       return;
//     }
    // A special case: change Update parent for exon to two Transactions, 
    // delete and add
    List tns = generateTransaction(event);
    int nt = tns.size();
    logger.debug("addTransaction generated " + nt + " transaction(s) from event " + event);

    if (nt > 0) { // can it be 0?
      // this is for coalescing as we go which isnt happening at the moment
      // i dont think it can happen with undo (unless 2 diff trans lists)
      if (needCoalescence) { 
        for (Iterator it = tns.iterator(); it.hasNext();) {
          Transaction transaction = (Transaction) it.next();
          coalesceTransaction(transaction, unflattenedTransactions);
        }
      }
      else {
        unflattenedTransactions.addAll(tns);
        isCoalescedDone = false;
        invalidateFlattenedTransactions(); // out of synch with add to unflat
      }
    }
    if (nt == 0) {
      logger.debug("no transaction is generated for this type of event: " + event.getClass());
      return;
    }
    if (logger.isDebugEnabled()) {
      for (Iterator it = tns.iterator(); it.hasNext();) {
        Transaction transaction = (Transaction) it.next();
        logger.debug("added transaction " + transaction.oneLineSummary());
      }
    }
  }
  
  /**
   * Both features are genes that are fired from AnnotationReplaceEvent.
   * @param replacedFeature
   * @param newFeature 
   i think we can delete this - replace has been phased out
   */
  private void replaceSeqFeature(FeatureSet replacedFeature,
                                  FeatureSet newFeature) {
    Transaction tran = null;
    for (Iterator it = unflattenedTransactions.iterator(); it.hasNext();) {
      tran = (Transaction) it.next();
      if (tran.getSeqFeature() == replacedFeature)
        tran.setSeqFeature(newFeature);
      //TODO: Need to consider exons. It might be correct for transcript.
      // However, it is definitely wrong for exons.
      else if (replacedFeature.contains(tran.getSeqFeature())) { // e.g. A transcript in a gene
        // a very dangerous way: Is index stable?
        int index = replacedFeature.getFeatureIndex(tran.getSeqFeature());
        tran.setSeqFeature(newFeature.getFeatureAt(index));
      }
    }
  }

  /**
   * A method to coalesce a Transaction others in the list of Transaction
   * objects. The following is how coalescing is implemented: 1. If a new
   * DeleteTransaction is added, all types of Transactions (delete, add and
   * update) belong to features that are under the deleted feature (include the
   * deleted feature) should be removed from the list because they are not
   * needed any more. For example, if a transcript is deleted, all transactions
   * belong to this transcript, exons owned by the transcript, or subparts to
   * this transcript should be removed. To implement this, a kind of tree
   * structure should be kept for deleted features. Maybe keeping the parent_id
   * for the deleted feature will solve this problem easily. 2. A new
   * UpdateTransaction should be merged by a previous UpdateTransaction that was
   * applied to the same feature and its subpart. 3. A new UpdateTransaction 
   * should be coalesced to an AddTransaction if both of them reference to the 
   * same SeqFeatureI object. 4. A new UpdateTransaction should be coalesced to an 
   * AddTransaction if the target of UpdateTransaction is a descendent of one in
   * the AddTransaction object. 5. Adding a child feature or a subpart should be 
   * coalesced to transaction for adding feature to which this subpart belong to.
   * 6. A DeleteTransaction should be absorbed by a previous AddTransaction that
   * adds an ancestor of the deleted SeqFeatureI or its subpart.
   * 
   * @param transaction the new Transaction object should be coalesced.
   * @param list the list coalescing is targeted. A list of transactions that have
   survied coalescing thus far, and that the transaction will be added to if it
   is not coalesced.

   For now just flattening out compound transactions. This certainly makes coalescing
   easier. It does mean that transactions cant be undone after save/coalesce (or at
   least its a bad idea). I think this is an ok restriction for now and perhaps forever.
   */
  private void coalesceTransaction(Transaction transaction, List list) {

    // COMPOUND - gets flattened! (for now at least)
    // actually coalesceTrans is only given flattened trans so this is no longer needed
    if (transaction.isCompound()) {
      for (int i=0; i<transaction.size(); i++)
        coalesceTransaction(transaction.getTransaction(i),list);
    }

    // ADD
    else if (transaction.isAdd()) {

      // so with splits its possible that an adding of a gene happens after some
      // transactions that occurred to this now "new" gene - have to coalesce them
      // retroactively
      boolean checkForBackwardsCoalesence = !transaction.hasSubpart();
      boolean haveParent = haveParentAddTransaction(transaction,list,
                                                    checkForBackwardsCoalesence);
      if (haveParent)
        return; // The added info should already be in SeqFeaureI in the found
                 // transaction.

      // So an add can end up having a temp as a parent. Its a wierd case but if
      // you add a transcript to a gene and then split the gene into 2 genes, the 
      // add transcript will now be for a new split gene, but the add transcript will
      // not get coalesced since it happens before the add gene from the split. An easy
      // solution is to just take out adds to parents that are temps, as the adding of 
      // the parent temp should be happening later and at that time this add transaction
      // will get taken care of effectively - thus this check is needed. I think this
      // is the best way to do this but im open to other ideas. - MG
      // i think checkForBackwardsCoalescence above takes care of this wierd case
      // would be best if AE spit things out in proper order
      //if (parentIsNew(transaction))
      //return; // coalesce trans out - return without adding to list

      list.add(transaction);
    }

    // UPDATE
    else if (transaction instanceof UpdateTransaction) {

      // If there is an add transaction in list that makes the update irrelevant
      // the index of the add trans is returned, otherwise returns -1
      int index = searchAddTransaction(transaction, list);
      if (index >= 0) {
        // Don't need update. The new values are sticked with SeqFeatureI
        // which will be handled by AddTransaction. No prevValue in the
        // database
        return;
      }
      // If there is an update transaction that can absorb this update return index
      // of that trans in list, otherwise returns -1
      index = searchUpdateTransaction(transaction, list);
      if (index == -1) // Cannot find a coalescable UpdateTransaction
        list.add(transaction);
      else {
        // Merge two UpdateTransaction: Use the preValue from the old
        // UpdateTransaction
        // and use the timestamp from new UpdateTransaction.
        // Delete the oldTransaction
        UpdateTransaction prevTn = (UpdateTransaction) list.get(index);
        UpdateTransaction newTn = (UpdateTransaction) transaction;
        newTn.setOldSubpartValue(prevTn.getOldSubpartValue());
        list.add(newTn);
        list.remove(index);
      }
    } 

    // DELETE
    else if (transaction.isDelete()) {
      SeqFeatureI deletedFeat = transaction.getSeqFeature();

      // If whole feature delete exclude self from search, thats taken care of below
      // where add is taken out as well (cancel each other)
      //boolean includeSelf = transaction.hasSubpart();

      boolean haveParent = haveParentAddTransaction(transaction,list);
      if (haveParent)
        return; // The deleted info should already be in SeqFeaureI in the found
                 // transaction.      

      // WHOLE FEATURE DELETE (not subpart delete)
      if (!transaction.hasSubpart()) {
        // A flag to check id deletedFeat is a newly added feature.
        // If true, this transaction should also be coalesced away
        boolean isNew = false;
        // Remove all Transactions that are covered by deletedFeature
        Transaction tx = null;
        SeqFeatureI txFeat;
        for (Iterator it = list.iterator(); it.hasNext();) {
          tx = (Transaction) it.next();
          txFeat = tx.getSeqFeature();

          // Check isNew
          if (!isNew && 
              (tx.isAdd()) && 
              (!tx.hasSubpart()) && 
              (deletedFeat.isIdentical(txFeat))) {
            isNew = true;
            // checkSplitInvalidation();
          }

          // remove transactions on deleted feat - isIdentical catches clones (e.g. exons
          // on merging)
          //To be identical,  feature should have different names and ID
          //isIdentical chops because on split, deleted feats range values are the defaults.
          //So use isSameFeat instead
         //if (deletedFeat.isAncestorOf(txFeat) || deletedFeat.isIdentical(txFeat)) 
  //                 it.remove(); 
           if (deletedFeat.isAncestorOf(txFeat) || deletedFeat.isSameFeat(txFeat) )
            it.remove(); 
          // if tx is a delete from a update parent/move cmpd trans than the deleted
          // feat actually has a new parent and the isAncestorOf above wont coalesce it
          // need to check oldParent it came from
          else if (tx.isDelete()) {
            SeqFeatureI originalParent = tx.getParentFeature();
            //TODO : throw an exception if originalParent is null
            if (originalParent != null && (deletedFeat.isAncestorOf(originalParent)
                || deletedFeat.isIdentical(originalParent))) {
              it.remove();
            }
          }
        }

        // Only add delete transaction if it hasnt been cancelled out by a previous 
        // add/new transaction. In other words drop delete if it was deleting new feat.
        if (!isNew)
          list.add(transaction);
      }
      
      // SUBPART DELETE
      else {
        TransactionSubpart subpart = transaction.getSubpart();
        // Only can remove a add or modify the same feature and same subpart
        // instance
        Object deletedValue = ((DeleteTransaction)transaction).getNewSubpartValue();
        boolean isNew = false;
        Transaction tx = null;
        SeqFeatureI tmp;
        for (Iterator it = list.iterator(); it.hasNext();) {
          tx = (Transaction) it.next();
          tmp = tx.getSeqFeature();
          // Check isNew
          if (!isNew && 
              (tx instanceof AddTransaction) && 
              (tx.getSubpart() == subpart) && 
              (tmp == deletedFeat)) {
            if (((AddTransaction)tx).getNewSubpartValue() == deletedValue)
              isNew = true;
          }
          if (shouldRemoveTransaction(tx,
                                      subpart,
                                      deletedFeat,
                                      deletedValue))
            it.remove();
        }
        if (!isNew)
          list.add(transaction);
      }
    } // end of delete trans coalescing
  }
  

  private void checkSplitInvalidation(SeqFeatureI delFeat,Transaction addTrans) {
    // if top level annot (eg gene) and the add is part of a split then
    // the split is no longer (delete has wiped out new split off gene)
    if (!delFeat.hasAnnotatedFeature())
      return; // only for annots

    AnnotatedFeatureI delAnnot = delFeat.getAnnotatedFeature();
    if (!delAnnot.isAnnotTop())
      return; // must be top level (gene)

    
  }
  
  /** Return true is trans part of (or is) a split transaction.
      (make Transaction method? */
  private boolean isPartOfSplit(Transaction tx) {
    if (tx.isSplit())
      return true;
    if (!tx.hasParentTransaction())
      return false;
    return isPartOfSplit(tx.getParentTransaction());
  }

  
  /**
   * A helper to determine another Transaction should be coalesced because of
   * a newly added DeleteTransaction.
   * @param tx
   * @param subpart
   * @param deletedFeature
   * @param deletedValue
   * @return
   */
  private boolean shouldRemoveTransaction(Transaction tx,
                                          TransactionSubpart subpart,
                                          SeqFeatureI deletedFeature,
                                          Object deletedValue) {
    if (tx.getSeqFeature() != deletedFeature)
      return false;
    if (tx.getSubpart() != subpart)
      return false;
    if (tx instanceof AddTransaction) {
      Object addedValue = ((AddTransaction)tx).getNewSubpartValue();
      if (addedValue == deletedValue)
        return true;
      else 
        return false;
    }
    if (tx instanceof UpdateTransaction) {
      //TODO: Have to check the implementation for updating comments and synonyms
    }
    return false;
  }
  
  /**
   * Coalesce all Transaction instances in this TransactionManager. This method should
   * be called for a TransactionManager instance whose needCoalesce is false before
   * Transaction instances are saved.
   This presently has the side effect of flattening unflattened transactions.
   */
  public void coalesce() {

    // TODO - add a config. parameter to optionally skip the coalesce() phase

    if (isCoalescedDone) // No need
      return;
    List list = new ArrayList();
    Transaction tn = null;
    //for (Iterator it = unflattenedTransactions.iterator(); it.hasNext();) {
    for (int i=0; i<getFlattenedSize(); i++) {
      //tn = (Transaction) it.next();
      tn = getFlattenedTransaction(i);
      coalesceTransaction(tn, list);
    }
    // Keep the original transactions list in case it is needed. ???
    flattenedTransactions.clear();
    flattenedTransactions.addAll(list);
    //unflattenedTransactions.clear();
    //unflattenedTransactions.addAll(list);
    invalidateUnflattenedTransactions(); // for now does unflat = flat
    isCoalescedDone = true;
  }
  
  /**
   * A helper method to find an coalescable UpdateTransaction from the list.
    If there is an update transaction that can absorb this update return index
    of that trans in list, otherwise returns -1
   * 
   * @param newUpdate transaction to potentially coalesce
   * @return index of transaction that trumps newUpdate, -1 if none
   */
  private int searchUpdateTransaction(Transaction newUpdate, List list) {
    Transaction tn = null;
    int index = 0;
    for (Iterator it = list.iterator(); it.hasNext();) {
      tn = (Transaction) it.next();
      if (tn instanceof UpdateTransaction) {
        if (tn.getSeqFeature() == newUpdate.getSeqFeature()
            && tn.getSubpart() == newUpdate.getSubpart())
          return index;
      }
      index++;
    }
    return -1;
  }
  
  /** If there is an add transaction in list that makes the update irrelevant
      the index of the add trans is returned, otherwise returns -1 */
  private int searchAddTransaction(Transaction srcTn, List list) {
    Transaction tn = null;
    int index = 0;
    SeqFeatureI feature = srcTn.getSeqFeature();
    for (Iterator it = list.iterator(); it.hasNext();) {
      tn = (Transaction) it.next();
      if (tn instanceof AddTransaction) {
        if (canAbsorbUpdateTn(tn, srcTn))
          return index;
      }
      index ++;
    }
    return -1;
  }
  
  /** Checks both updates feature has been added or if the subpart it is updating
      has been added */
  private boolean canAbsorbUpdateTn(Transaction addTn, 
                                     Transaction updateTn) {
    // Updating a newly added subpart value
    if (addTn.getSeqFeature() == updateTn.getSeqFeature() &&
        addTn.getSubpart() == updateTn.getSubpart() &&
        addTn.getSubpartRank() == updateTn.getSubpartRank())
      return true;
    // Updated feature's ancestor is newly added
    if (addTn.getSeqFeature().contains(updateTn.getSeqFeature()) &&
        addTn.getSubpart() == null)
      return true;
    return false;
  }
  
  private boolean haveParentAddTransaction(Transaction transaction, List list) {
    return haveParentAddTransaction(transaction,list,false);
  }

  /**
   * Search an AddTransaction whose SeqFeatureI contains the specified feature 
   * (including itself if includeSelf is true).
   * List is the current working list of coalesced transactions in progress (past
   * transactions not future) If a past transaction has an add that is for an ancestor
   * of feature (or if includeSelf is true then the feature itself as well) then
   * return the index of that transaction. This is used in a boolean manner so
   * maybe it should just return a boolean.
   * This does not cover the funny case where a feature/transaction ancestor later
   * on splits off and becomes a new gene, because future transactions are not searched.
   * maybe it should future as well for these funny cases?
   * checkForBackwardsCoalescence if true deals with this funny case - separate method?
   * yes the check for backwards doesnt really belong her but in separate method
   * @param feature
   * @param transactions - list of transactions
   * @return -1 if a parent is not added. return int of index in list of parent/ancestor
   * being added
   */
  private boolean haveParentAddTransaction(Transaction transaction,//SeqFeatureI feature, 
                                           List transactions,
                                           //boolean includeSelf,
                                           boolean checkForBackwardsCoalesence) {

    SeqFeatureI feature = transaction.getSeqFeature();
    // dont include self for deletes of whole feat (non-subpart) as then the delete
    // will wipe out the add as well
    //boolean includeSelf = !transaction.isDelete() || transaction.hasSubpart();
    // actually deletes are even wackier - for moves a deleted feat will have a new
    // parent and will pass the isAncestor test even though really meaning to test for
    // old lineage not new - to ammend this use old parent thats in transaction - and
    // since for deletes we want to return false if self has been added its ok to just
    // check parent - admitttedly funny logic though - these coalescings get kinda wacky
    if (transaction.isDelete() && !transaction.hasSubpart())
      feature = transaction.getParentFeature();

    Transaction tn = null;
    int index = 0;
    //if (includeSelf) {
    for (Iterator it = transactions.iterator(); it.hasNext();) {
      tn = (Transaction) it.next();
      if (tn.isAdd()) {
        // Should exclude for subpart transaction
        SeqFeatureI addedFeature = tn.getSeqFeature();
        if (addedFeature.isAncestorOf(feature) && !tn.hasSubpart())
            //&& (includeSelf || addedFeature != feature)) --> getParentFeat
          return true;
        
        // if previous tran feature is descendant of this feature remove it - 
        // this can happen with splits - this should happen in separate method not here
        if (checkForBackwardsCoalesence) {
          if (feature.isAncestorOf(addedFeature)) {
            logger.debug("backward remove "+addedFeature);
            it.remove();
          }
        }
      }
      index++;
    }
    //}
//     else {
//       for (Iterator it = transactions.iterator(); it.hasNext();) {
//         tn = (Transaction) it.next();
//         if (tn instanceof AddTransaction) {
//           // Should exclude for subpart transaction
//           SeqFeatureI addedFeature = tn.getSeqFeature();
//           if (addedFeature != feature && // the difference between the else and if
//               addedFeature.isAncestorOf(feature) && 
//               tn.getSubpart() == null)
//             return index;
//         }
//         index++;
//       }
//     }
    return false;
  }

  // this has been repplaced by backwards coalescence trick in 
  // searchParentAddTransaction
//   private boolean parentIsNew(Transaction transaction) {
//     AnnotatedFeatureI annot = transaction.getAnnotatedFeature();
//     // dont need to check parents of annotation root. also annot roots parents have 
//     // NO_NAME as name, FeatureSet actually returns childs name in this case(yikes)
//     if (annot.isAnnotationRoot())
//       return false;
//     return idIsTemp(transaction.getSeqFeature().getParent().getId());
//   }
  
//   /** This ultimately belongs in the name adapter, just doing it here for now */
//   private boolean idIsTemp(String id) {
//     return id.matches(".*temp.*");
//   }

  /**
   * Generate a Transaction based on a specified AnnotationChangeEvent. The
   * generated Transaction should be one of subclasses to Transaction.
   old way - annotation change event does not have transaction - have to create
   a transaction from the annotation change event
   new way - annotation change event just holds the transaction - just get it
   and we're done 
   * 
   * @param e
   */
  protected List generateTransaction(AnnotationChangeEvent e) {
    List rtn = new ArrayList(2);
    boolean needAddToList = true;
    Transaction transaction = null;
    

    // new style - change event carries a transaction
    if (e.hasTransaction()) {
      if (e.getTransaction().isCompound() && !e.getTransaction().hasKids())
        return rtn; // ditch empty compound trans, return empty list
      rtn.add(e.getTransaction());
      return rtn;
    }

    // this is the old way - we have to creat a transaction from the ACE
    // eventually this will go away!
    // Based on if there is a subpart available from e to choose a Constrcutor
    // for Transaction
    // ADD
    if (e.isAdd()) {
      if (e.getSubpart() == null)
        transaction = new AddTransaction(e.getAddedFeature());
      else {
        int index = e.getSubpartRank();
        Object newValue = null;
        if (e.getSubpart() == TransactionSubpart.SYNONYM) {
          // Synonym Object
          newValue = ((AnnotatedFeatureI)e.getAddedFeature()).getSynonyms().get(index);
        }
        else if (e.getSubpart() == TransactionSubpart.COMMENT) {
          // Comment is an Object of Comment class
          newValue = ((AnnotatedFeatureI)e.getAddedFeature()).getComments().get(index);
        }
        else if (e.getSubpart() == TransactionSubpart.DBXREF) {
          // DBXref is an object
          newValue = ((AnnotatedFeatureI)e.getAddedFeature()).getDbXrefs().get(index);
        }
        transaction = new AddTransaction(e.getAddedFeature(), 
                                         e.getSubpart(),
                                         newValue);
        transaction.setSubpartRank(index);
      }
    }

    // DELETE
    else if (e.isDelete()) {
      if (e.getSubpart() == null)
        // should we get rid of deleted feat and just used changed feat.
        // does deleted feat really provide any needed functionality or clarity?
        transaction = new DeleteTransaction(e.getDeletedFeature(), 
                                            e.getParentFeature());
      else {
        // On a subpart
        AnnotationDeleteEvent deleteEvent = (AnnotationDeleteEvent) e;
        if (deleteEvent.getSubpart() == TransactionSubpart.COMMENT) {
          // for subpart its changed feat not deleted - should it be deleted as well?
          //transaction = new DeleteTransaction(deleteEvent.getDeletedFeature(), 
          transaction = new DeleteTransaction(deleteEvent.getChangedFeature(), 
                                              deleteEvent.getSubpart(),
                                              deleteEvent.getOldComment());
        }
        else if (deleteEvent.getSubpart() == TransactionSubpart.SYNONYM) {
          transaction = new DeleteTransaction(deleteEvent.getChangedFeature(),
                                              deleteEvent.getSubpart(),
                                              deleteEvent.getOldString());
        }
        transaction.setSubpartRank(e.getSubpartRank());
      }
    }

    // UPDATE 
    else if (e.isUpdate()) {
      if (e.getSubpart() == null)
        throw new IllegalStateException("TransactionManager.generateTransaction(): "
                                       + "No subpart for AnnotationChangeEvent");
      // Update can only work on a subpart
      transaction = new UpdateTransaction(e.getChangedFeature(), e.getSubpart());
      SeqFeatureI changedFeature = e.getChangedFeature();
      TransactionSubpart subpart = e.getSubpart();
      AnnotationUpdateEvent updateEvent = (AnnotationUpdateEvent) e;

      // EXON UPDATE
      if (changedFeature instanceof ExonI) {
        if (subpart == TransactionSubpart.LIMITS) {
          ExonI exon = (ExonI)changedFeature;
          RangeI range = updateEvent.getOldRange();
          ((UpdateTransaction) transaction).setOldSubpartValue(range);
          // give the name adapter a chance to update the exon's id/name:
          curationState.getNameAdapter(exon).updateExonId(exon);
        }
        // this should be done by request by the data adapter as rice needs to do this
        // but tair wants the parent update - or actually the parent trans should be
        // a compound trans with del & add - or is that a special case?
        // its now done with compound UpdateParentTrans
//         else if (subpart == TransactionSubpart.PARENT) {
//           SeqFeatureI oldParent = updateEvent.getOldParent();
//           ExonI exonCopy = (ExonI) changedFeature.clone();
//           exonCopy.setRefFeature(oldParent); // is this needed?
//           // this is wrong - should be name - may not change if same gene
//           updateExonID(changedFeature); // name adapter/AE!!
//           transaction = new DeleteTransaction(exonCopy,
//                                               oldParent);
//           rtn.add(transaction);
//           transaction = new AddTransaction(changedFeature);
//           rtn.add(transaction);
//           //((UpdateTransaction) transaction).setPreValue(oldParent);
//           needAddToList = false;
//         }
      }

      // ID UPDATE
      // An ID change will trigger a delete/add actions to the db
      // I dont think this should be here - like type change this should happen
      // at commit time - and should be data adapter specific - another data
      // adapter may be able to deal with id changes and not have to resort to
      // wiping out and readding the annot. also it would be easier for undo to 
      // have the original id transaction.
      // actually the only thing using transactions right now is rice - and you cant 
      // change ids in rice - so the only purpose this is for is undoing an id change
      // in fly...
      // actually thats not right - merging genes causes id change and this is possible
      // in rice...
      // tair needs to see merge & split events which this hack clears out - moving
      // this logic to chado adapter where it belongs (eventually should go)
      // THIS IS NOW MOVED - its in method replaceIdUpdateWithDelAndAdd which is called
      // by ChadoTransactionTransformer, and could potentially be called by other
      // transformers (there arent any others at this point)
//       else if (subpart == TransactionSubpart.ID) { 
//         // if feature previously added dont bother doing delete & add again.
//         // (a previous add can be just an id change (which is delete & add of course)
//         // this is bad for undo as its bascially coalescing away this transaction!
//         // Also del & add cant be recognized as id change for undo!
//         if (!isNewFeature(changedFeature)) {
//           // For old AnnotatedFeature
//           AnnotatedFeatureI oldFeature = new AnnotatedFeature();
//           oldFeature.setId(updateEvent.getOldString());
//           // presumptious - name could be different than id!
//           oldFeature.setName(updateEvent.getOldString());
//           // For coalescing purpose (???)
//           oldFeature.setStart(changedFeature.getStart());
//           oldFeature.setEnd(changedFeature.getEnd());
//           // Name is not needed for delete
//           transaction = new DeleteTransaction(oldFeature);
//           rtn.add(transaction);
//           transaction = new AddTransaction(changedFeature);
//           rtn.add(transaction);
//         }
//         needAddToList = false; // dont add transaction to rtn
//       }
      
      // TOP ANNOT OR TRANSCRIPT UPDATE (all others...)
      else if (changedFeature.getFeatureType().equals(Transcript.TRANSCRIPT_TYPE) ||
                changedFeature.getAnnotatedFeature().isAnnotTop()) {
        extractPreValue(updateEvent, subpart, (UpdateTransaction)transaction);
      }
      
    }

    // If transaction is not null AND needAddToList is still true then add
    // transaction to rtn return list
    if (transaction != null) {// It is null for unsupported types
      transaction.setSource(e.getSource()); // temporary - for FED undo
      if (needAddToList)
        rtn.add(transaction);
    }

    return rtn;
  }

  /** Flattens out transactions. Compound transactions are reduced to their
      children. children still have */
  public List getFlattenedTransactions() {
    if (flattenedTransactions == null) {
      flattenedTransactions = new ArrayList(size());
      if (unflattenedTransactions == null) // shouldnt happen
        return flattenedTransactions;
      for (int i=0; i<size(); i++) {
        Transaction unflatTrans = getUnflattenedTransaction(i);
        flattenedTransactions.addAll(unflatTrans.getLeafTransactions());
      }
    }
    return flattenedTransactions;
  }

  private void invalidateFlattenedTransactions() {
    flattenedTransactions = null;
  }

  private Transaction getFlattenedTransaction(int i) {
    return (Transaction)getFlattenedTransactions().get(i);
  }

  private int getFlattenedSize() {
    return getFlattenedTransactions().size();
  }

  
  /** Change all id update transactions to delete and add. Current rice/fly chado
   *  trigger for creating new ids only, doesnt give new ids for id updates
   * scott may have ammended this - check this out - no scott hasnt ammended this yet
   *but its not urgent. this will modify flattened list of trans(if there is an id
   *update), which will invalidate the unflattened list, for now that means just 
   *setting the unflattened = flattened, which i think will be ok. in other words once
   *we've flattened i dont think theres any need to go back to unflattened */
  public void replaceIdUpdatesWithDelAndAdd() {
    List newFlatTransactions = new ArrayList(getFlattenedSize());
    boolean transChanged = false;
    for (int i=0; i < getFlattenedSize(); i++) {
      Transaction trans = getFlattenedTransaction(i);
      if (trans.isUpdate() && trans.getSubpart().isId()) {
        // replace
        AnnotatedFeatureI deletedFeat = new AnnotatedFeature();
        // name? start? end? (for exons?)
        deletedFeat.setId(trans.getOldString()); // old string is old id
        SeqFeatureI parent = trans.getSeqFeature().getRefFeature();
        Transaction tr = new DeleteTransaction(deletedFeat,parent,this);
        newFlatTransactions.add(tr);
        tr = new AddTransaction(trans.getSeqFeature());
        newFlatTransactions.add(tr);
        transChanged = true;
        logger.debug("id update->del,add");
      }
      else {
        newFlatTransactions.add(trans);
      }
    }
    flattenedTransactions = newFlatTransactions;
    if (transChanged) {
      // flattened trans have been changed - invalidate unflattened
      invalidateUnflattenedTransactions(); // for now does unflat = flat
      isCoalescedDone = false; // del-add can cause further coalescing
    }
  }
  
  /** Returns true if feature has been added in a previous transaction. 
   (wierdo case - previous id change causes delete & add of feature) */
  private boolean isNewFeature(SeqFeatureI feature) {
    Transaction tn = null;
    for (Iterator it = unflattenedTransactions.iterator(); it.hasNext();) {
      tn = (Transaction) it.next();
      if ((tn.isAdd()) &&
          (tn.getSubpart() == null) &&
          (tn.getSeqFeature().isIdentical(feature)))
          return true;
    }
    return false;
  }
  
  /** This actually is incomplete, it doesnt catch every kind of subpart. for instance
      Parent subpart is not here. In that case old subpart value gets set to null.
      in the case of parent that currently doesnt matter because parent updates
      always get coalesced out (due to splits causing id changes that cause an add
      delete). when this is no longer true this will need ammending (course eventually
      the mechanism of making transactions from change events will be pase' */
  private void extractPreValue(AnnotationUpdateEvent updateEvent, 
                               TransactionSubpart subpart,
                               UpdateTransaction transaction) {
    Object oldValue = null;
    if (updateEvent.isIntUpdate()) {
      oldValue = new Integer(updateEvent.getOldInt());
    }
    else if (updateEvent.isStringUpdate()) {
      oldValue = updateEvent.getOldString();
    }
    else if (updateEvent.isBooleanUpdate()) {
      //oldValue = new Boolean(updateEvent.getOldBoolean());
      oldValue = new Boolean(!TransactionUtil.getBoolean(updateEvent.getChangedAnnot(),
                                                         subpart));
    }
    else if (updateEvent.isCommentUpdate()) {
      // getOldComment gives a comment object - will this jibe with committing?
      oldValue = updateEvent.getOldComment();
      //if (oldValue != null)
      //  oldValue = ((Comment)oldValue).getText();
      transaction.setSubpartRank(updateEvent.getSubpartRank());
    }
    else if (updateEvent.isRangeUpdate()) {
      oldValue = updateEvent.getOldRange();
    }

    // TYPE CHANGE - can cause id change, need to record old id in transaction
    // as type changes are now done as delete, add, need old id for delete
    // now GeneEditPanel actually creates type change transaction itself
//     if (updateEvent.getSubpart().isType()) {
//       transaction.setOldId(updateEvent.getOldId());
//     }

    transaction.setOldSubpartValue(oldValue);
  }
  
  /**
   * Implements the method from AnnotationChangeListener. Basically, this method
   * just delegates to addTransaction(AnnotationChangeEvent).
   * 
   * @return always return true for the time being
   */
  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {

    if (evt.getSource() == this)
      return false; // event is result of undo from TranMan - dont readd it
    addTransaction(evt);
    return true;
  }

  /**
   * Set a list of Transactions for this TransactionManager. This method should
   * be called during opening a new file to load saved Transaction instances.
   * 
   * @param list
   */
  public void setTransactions(java.util.List list) {
    // Use the List in this Class instead of from other place
    unflattenedTransactions.clear();
    //flattenedTransactions.clear();
    flattenedTransactions = null;
    // actually compound trans cant be saved & loaded yet(no need), so in fact
    // these unflattened are flat (should disable undo for them!)
    if (list != null)
      unflattenedTransactions.addAll(list);
  }

  /**
   * Get the list of Transaction instances managed by this object. A new List
   * object (cloned) is returned. why are we cloning the list? taking out clone
   for now - dont think we need it.
   * @return
   */
  public java.util.List getTransactions() {
    if (unflattenedTransactions == null && flattenedTransactions != null)
      unflattenedTransactions = makeUnflattenedFromFlattened();
    //return new ArrayList(unflattenedTransactions);
    return unflattenedTransactions;
  }

  /** Flattened trans have been changed, unflattened trans no longer valid.
      For now just setting to unflattened trans. this assumes we dont need
      unflattened trans once theyve been flattened. i think this is actually true.
      if not then makeFlattenedFromFlattened OR set to null (make on demand) 
      lets see how this pans out. */
  private void invalidateUnflattenedTransactions() {
    // what should happen - unclear if necasary though
    // unflattenedTransactions = null; OR makeUnflattenedFromFlattened()
    // for now...
    unflattenedTransactions = flattenedTransactions;
  }

  /** Flattened has been edited, invalidating unflattened. Reconstruct flattened
      from unflattened. it unclear if this is actually needed. once list has
      been flattened dont think we need it flattened again. for now just
      returning flattened list - ammend this if needed. */
  private List makeUnflattenedFromFlattened() {
    logger.debug("Just using flattened trans - make sure ok", new Throwable());
    return flattenedTransactions;
  }


  /** direct undo - to replace undo via ACE */
  public void undo(Object source) {
    // have to get from end of list to get most recent
    //System.out.println("number of transactions:\t" + numberOfTransactions());
    if (numberOfTransactions() > 0) {
      undo(popTransaction());
    }
    /*
    for (int i=numberOfTransactions()-1; i>=0; i--) {
      Transaction trans = getTransaction(i);
      // for now only undoing from AIE - thus the checking of source
      // only AIE/FED is calling undo at the moment (with itself as source)
      //if (trans.getSource() == source) {
        undo(trans);
        return;
      //}
    }
    */
  }
  
  public Transaction popTransaction()
  {
    List transactions = getTransactions();
    if (transactions.size() == 0) {
      return null;
    }
    return (Transaction)transactions.remove(transactions.size() - 1);
  }

  /** Partial - For now only undoing FED stuff - 
      should this go in a separate undo class?*/
  private void undo(Transaction t) {

    // COMPOUND
    if (t.isCompound()) {
      undoCompound(t);
      unflattenedTransactions.remove(t);
      logger.debug("removed transaction " + t.oneLineSummary());
      invalidateFlattenedTransactions();
      return;
    }


    // NOT COMPOUND
    if (!t.getSeqFeature().hasAnnotatedFeature())
      return; // shouldnt happen for non-compound

    /*
    if (!t.hasSubpart())
      return; // for now - eventually handl non-subpart transactions
    */

    if (t.isUpdate()) 
      undoUpdate((UpdateTransaction)t); // would be nice to do this without casting

    else if (t.isAdd())
      undoAdd(t);

    else if (t.isDelete())
      undoDelete(t);

    unflattenedTransactions.remove(t);
    logger.debug("removed transaction " + t.oneLineSummary());
    invalidateFlattenedTransactions();
  }

  private void undoCompound(Transaction t) {
    t.undo();
    //AnnotationChangeEvent a = t.generateAnnotationChangeEvent(this);
    AnnotationChangeEvent a = t.generateUndoChangeEvent(this);
    // generateACE sets source to trans source - which is not the source for 
    // the event! wierd but on undo the transaction source is different than
    // the event source. do we even need a source in the transaction? what is it
    // used for?
    //a.setSource(this);
    fireAnnotEvent(a);
    fireAnnotEvent(new AnnotSessionDoneEvent(this));
  }

  private void undoUpdate(UpdateTransaction trans) {

    AnnotatedFeatureI af = trans.getSeqFeature().getAnnotatedFeature();
    TransactionSubpart ts = trans.getSubpart();

    if (ts.isBoolean())
      TransactionUtil.flipBoolean(af,ts);

    // i think this is no longer used since name changes are part of 
    // compound trans which undos through child trans, not here - delete?
    // check this
    else if (ts.isName())
      nameUndoTempHack(trans); // compound hack

    else if (ts.isType()) 
      typeUndoTempHack(trans); // compound hack

    else if (ts.isId())
      trans.undo();

//     else if (ts.isString())
//       TransactionUtil.undoStringUpdate(trans,af);

    else if (ts.isComment())
      TransactionUtil.undoUpdateComment(trans,af);

    else
      return;
    
    boolean isSingularEvent = true;
    AnnotationUpdateEvent au = new AnnotationUpdateEvent(this,af,ts,isSingularEvent);
    fireAnnotEvent(au);
  }

  private void undoAdd(Transaction trans) {

    /*
    TransactionSubpart ts = trans.getSubpart();
    
    if (ts.isComment())
      TransactionUtil.undoAddComment(trans);
    else if (ts.isSynonym())
      //TransactionUtil.undoAddSynonym(trans);
      trans.undo(); // woohoo!
    else return; // dont fire event if not handled

    AnnotatedFeatureI af = trans.getAnnotatedFeature();
    boolean isSingularEvent = true;
    AnnotationDeleteEvent ad = new AnnotationDeleteEvent(this,af,ts,isSingularEvent);
    fireAnnotEvent(ad);
     */

    trans.undo();
    AnnotationChangeEvent a = trans.generateUndoChangeEvent();
    a.setSingularEventState(true);
    a.setSource(this);
    fireAnnotEvent(a);
  }
  
  private void undoDelete(Transaction trans) {
    /*

    if (trans.hasSubpart()) {
      TransactionSubpart ts = trans.getSubpart();
      
      if (ts.isComment()) {
        TransactionUtil.undoDeleteComment(trans);
      }
      else if (ts.isSynonym()) {
        //TransactionUtil.undoDeleteSynonym(trans);
        trans.undo();
      }
      else { //can only deal with comments and synonyms
        return;
      }
      // fire event - for redo will need to add more stuff i think
      AnnotatedFeatureI af = trans.getAnnotatedFeature();
      boolean isSingularEvent = true;
      //Old way for doing transactions
      //AnnotationAddEvent ad = new AnnotationAddEvent(this,af,ts,isSingularEvent);
    }
    else {
      trans.undo();
    }*/
    trans.undo();
    AnnotationChangeEvent a = trans.generateUndoChangeEvent();
    a.setSingularEventState(true);
    a.setSource(this);
    fireAnnotEvent(a);
  }

  /** For now undoing transcript and gene name changes all at once - just rips out
      all name changes until it hits something else. 
      2 possible solutions for future:
      1) have just gene name transaction (transcript name change is implicit), 
      2) compound transaction that bundles them
      No exon names, exons use id, no proteins - rice has no prot names, just ids */
  private void nameUndoTempHack(UpdateTransaction nameTrans) {
    nameTrans.undo(); // fiddling around
    // fireAnnotEvent
    int index = unflattenedTransactions.indexOf(nameTrans);
    Transaction nextTrans = getTransaction(index - 1);
    // undo all subsequent name transactions (get transcripts and gene names)
    if (nextTrans != null && nextTrans.hasSubpart() 
        && nextTrans.getSubpart().isName()) {
      nameUndoTempHack((UpdateTransaction)nextTrans);
      unflattenedTransactions.remove(nextTrans);
    }
  }

  private void typeUndoTempHack(UpdateTransaction typeTrans) {
    typeTrans.undo();
    // undo id(add/del?) trans - possible name changes...
    // or check with name adapter if caused an id change
  }

  /** For firing annot change events from undos */
  public void setAnnotationChangeListener(AnnotationChangeListener acl) {
    undoListener = acl;
  }

  private void fireAnnotEvent(AnnotationChangeEvent a) {
    if (undoListener == null) { // shouldnt happen
      logger.error("no undo listener in TranManager for undo");
      return;
    }
    undoListener.handleAnnotationChangeEvent(a);

  }

  public boolean hasTransactions() { 
    return (unflattenedTransactions != null || flattenedTransactions != null)
      && numberOfTransactions() > 0;
  }

  public int numberOfTransactions() { 
    return unflattenedTransactions.size(); 
  }

  public int size() { return numberOfTransactions(); }
  
  public Transaction getTransaction(int i) {
    if (i < 0 || i >= numberOfTransactions()) 
      return null;
    return (Transaction)unflattenedTransactions.get(i);
  }

  /** for clarity - getTransaction actually returns the unflattened trans */
  private Transaction getUnflattenedTransaction(int i) {
    return getTransaction(i);
  }
  
  /**
   * Check if coalescing is done.
   * @return
   */
  public boolean isCoalescedDone() {
    return isCoalescedDone;
  }
  
  /**
   * Remove all transactions in the memory. This method should be called after Transactions
   * are saved to the database to keep the data synchronization.
   */
  public void emptyTransactions() {
    clear();
  }

  public void clear() {
    if (unflattenedTransactions != null)
      unflattenedTransactions.clear(); // == null?
    flattenedTransactions = null; //.clear();
    logger.debug("clearing transaction list");
  }

  /** convenience function. returns true if feat has been edited or a descendant of 
      feat has been edited. (looks through compound trans as well) */
  public boolean featureHasBeenEdited(SeqFeatureI feat) {
    for (int i=0; i<numberOfTransactions(); i++) {
      Transaction t = getTransaction(i);
      if (t.isCompound()) {
        if (featInCompoundTrans(feat,t))
          return true;
      }
      else if (t.getSeqFeature() != null && t.getSeqFeature().descendsFrom(feat))
        return true;
    }
    return false;
  }

  private boolean featInCompoundTrans(SeqFeatureI feat, Transaction compTrans) {
    for (int i=0; i<compTrans.size(); i++) {
      Transaction kidTrans = compTrans.getTransaction(i);
      if (kidTrans.isCompound())
        return featInCompoundTrans(feat,kidTrans);
      if (kidTrans.getSeqFeature() == null) {
        // shouldnt happen - programmer error
        logger.error("transaction with no feat", new Throwable());
        return false; 
      }
      if (compTrans.getTransaction(i).getSeqFeature().descendsFrom(feat))
        return true;
    }
    return false;
  }

  // Need this to get access to the name adapter:
  public void setCurationState(CurationState cs) {
    this.curationState = cs;
  }
}
//   /**
//    * Check if two SeqFeatureI objects reference to the same feature.
//    * @param feature1
//    * @param feature2
//    * @return
//    */
//   private boolean isIdentical(SeqFeatureI feature1, SeqFeatureI feature2) {
//     if (feature1 == feature2)
//       return true;
//     // Features have to have same type,range, AND name
//     if ((feature1.getType().equals(feature2.getType())) &&
//         (feature1.sameRange(feature2)) &&
//         feature1.getName().equals(feature2.getName()))
//       return true;
//     return false;
//   }
