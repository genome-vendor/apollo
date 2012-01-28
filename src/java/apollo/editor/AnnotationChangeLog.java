package apollo.editor;

import java.util.*;
import java.io.*;

//import apollo.gui.event.*;
//import apollo.gui.synteny.CurationManager;
//import apollo.gui.synteny.CurationState;
import apollo.config.Config;
import apollo.datamodel.*;
//import apollo.gui.ControlledObjectI; // -> apollo.controller
//import apollo.gui.Controller;
// for debugging
//import apollo.dataadapter.debug.*;

/**

DELETE THIS - replace by TransactionManager - i think i got all references -
test that it works out and delete!

 * Logs AnnotationChangeEvents which are fired when edits occur.
 repackage all editing stuff to apollo.edit?
 AnnotationChangeLog serializes to preserve log in backup
 ControlledObjectI actually extends Serializable (not sure why)
 which makes this possible
 Should ACL clear out on DatLoadEvents? yes - definitely
 I think loading is going to be a lot easier if there is one ACL per curation.
 If (later on) we want cross curation undo there could be a master ACL in addition 
 to curation ACLs, that tracked transactions across all species (analogous to 
 MasterController), though even that gets confusing with loading curations separately.
 So CurationState would holds an ACL (and CurationManager might hold the master ACL)
 took out implementing ControlledObjectI - dont think we need it
 */
// Serializable?

// DELETE? REPLACED BY TRANSACTION MANAGER
public class AnnotationChangeLog implements AnnotationChangeListener, Serializable {

  //private transient Controller controller;
  private List transactions = new ArrayList();

  //public AnnotationChangeLog() {}

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    // Save the *transaction* for this event
    // i dont know if evt should have a getTrans? maybe just make trans here?
// DISABLING - THIS CLASS NOT USED ANYMORE!
//     if (evt.isTransactionOperation())
//       addTransaction(evt.getTransaction());
    // addTransaction(new Transaction(evt)); // ??
    return true;
  }
  
  /** if new data is loaded transactions need to be cleared out. CurationState
      handles this. */
  public void clearTransactions() {
    transactions.clear();
  }

  public void addTransaction(Transaction trans) {
    if (trans != null) {
      transactions.add(trans);
    }
  }

  /** Returns list of Transactions */
  public List getTransactions() {
    return transactions;
  }

  public Transaction getTransaction(int i) {
    if (i > transactions.size())
      return null; // exception?
    return (Transaction)transactions.get(i);
  }

  public int getTransactionSize() { return transactions.size(); }

  /** Returns null if there are no transactions */
  private Transaction getLastTransaction() {
    if (transactions == null)
      return null;
    return (Transaction)transactions.get(transactions.size()-1);
  }

  public boolean hasTransactions() {
    return transactions != null && transactions.size() > 0;
  }

  public void undo() {
    if (!hasTransactions())
      return;
    Transaction t = getLastTransaction();

  }
}
