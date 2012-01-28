package apollo.editor;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import apollo.datamodel.AnnotatedFeatureI;

import org.apache.log4j.*;

/**
 * A CompoundTransaction contains a list of Transactions (children).
 * In some cases the child Transactions may also be CompoundTransactions
 * (i.e., CompoundTransactions may be nested)
 */
public class CompoundTransaction extends Transaction {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(CompoundTransaction.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // A list of Transaction objects
  private List childTransactions;

  private AnnotatedFeatureI newSplitFeature;

  /** Compound Types - these may turn into subclasses */
  static final int SPLIT = 1;
  static final int MERGE = 2;
  //static final int UPDATE_PARENT = 3; --> subclass UpdateParentTrans
  static final int NO_TYPE = -1;

  private int compoundType = NO_TYPE;

  /** phase out -> require source! */
  //public CompoundTransaction() { super(); }

  public CompoundTransaction(Object source) {
    setSource(source);
  }

  CompoundTransaction(int type,Object source) {
    this(source);
    setCompoundType(type);
  }


  void setCompoundType(int type) {
    compoundType = type;
  }

  public boolean hasCompoundType() { return compoundType != NO_TYPE; }

  public boolean isSplit() {
    return compoundType == SPLIT;
  }

  public boolean isMerge() {
    return compoundType == MERGE;
  }

  public boolean isCompound() { return true; }
  
  void undo() {
    //for (int i=0; i<size(); i++)
    for (int i = size() - 1; i >=0; --i) {
      getTransaction(i).undo();
    }
  }

  /**
   * Add a child Transaction to this CompoundTransaction object.
   * @param transaction
   */
  public void addTransaction(Transaction childTrans) {
    if (childTrans == null)
      return;
    addTrans(childTrans);
  }

  private void addTrans(Transaction childTrans) {
    if (childTransactions == null)
      childTransactions = new ArrayList(); // Do a lazy initialization
    childTransactions.add(childTrans);
    childTrans.setParentTransaction(this);
    if (getSource() != null)
      childTrans.setSource(getSource());
  }

  /** Adds the children of the compound trans NOT the CompoundTransaction itself.
      A CompoundTransaction is flat (at least for now) */
  public void addTransaction(CompoundTransaction addedTrans) {
    if (addedTrans == null)
      return;

    if (addedTrans == this) {
      logger.error("Can't add self as kid in CompoundTransaction.addTrans()");
      return;
    }
    
    // if empty compound trans dont bother adding
    if (!addedTrans.hasKids())
      return;

    // dont flatten
    if (!addedTrans.flattenOnAddingToCompTrans()) {
      addTrans(addedTrans);
    }
    // flatten
    else {
      for (int i=0; i<addedTrans.size(); i++)
        addTrans(addedTrans.getTransaction(i));
    }
  }

  /** Returns true. this means flatten out this transactions when adding it
      as a child to another compound transactions, which is the default. in other
      words dont preserve this comp trans in the comp trans heirarchy. 
      subclasses (UpdateParent) override this to preserve themselves. 
      actually if we have a type (split,merge) we probably need to be preserved,
      otherwise probably no need to. The reason no need to is that compound
      trans are often just used as trans collection and have no meaning.
      typed compound trans are the exception to that.
  */
  protected boolean flattenOnAddingToCompTrans() {
    return !hasCompoundType();
  }
  
  public Transaction getTransaction(int i) {
    return (Transaction)childTransactions.get(i); // check size?
  }

  public int size() {
    if (childTransactions == null)
      return 0;
    return childTransactions.size();
  }

  public String getCompoundTypeString() {
    String s = null;
    if (hasCompoundType()) {
      if (isSplit()) s = "SPLIT";
      if (isMerge()) s = "MERGE";
      if (isUpdateParent()) s = "UPDATE PARENT";
    }
    else { s = "NO TYPE"; }
    return s;
  }

  public String toString() {
    String s = "CompoundTrans num kids "+size()+" "+getCompoundTypeString();
    return s;
  }


  /** Return list of transactions */
  public List getTransactions() { return childTransactions; }

  /**
   * Remove a child Transaction object from this CompoundTransaction object.
   * @param transaction
   */
  public void removeTransaction(Transaction transaction) {
    if (childTransactions != null)
      childTransactions.remove(transaction);
  }

  public void setSource(Object source) {
    super.setSource(source);
    for (int i=0; i<size(); i++)
      getTransaction(i).setSource(source);
  }

  public AnnotationChangeEvent generateAnnotationChangeEvent() {
    if (getSource() == null) {
      if (logger.isDebugEnabled()) {
        String m = "DEBUG: Compound trans without src is generating annot cng evt. "+
          " Setting src to self for now.";
        logger.debug(m, new Throwable());
      }
      // workaround - source shouldve been set already!
      setSource(this);
    }
    return new AnnotationCompoundEvent(this);
  }
  
  AnnotationChangeEvent generateUndoChangeEvent()
  {
    if (getSource() == null) {
      if (logger.isDebugEnabled()) {
        String m = "DEBUG: Compound trans without src is generating annot cng evt. "+
          " Setting src to self for now.";
        logger.debug(m, new Throwable());
      }
      // workaround - source shouldve been set already!
      setSource(this);
    }
    AnnotationCompoundEvent ace = new AnnotationCompoundEvent(this);
    ace.setUndo(true);
    return ace;
  }

  /** returns list of all non-compound leaf descendants. should there be a 
      TransactionList class? */
  public List getLeafTransactions() {
    ArrayList leaves = new ArrayList(size());
    for (int i=0; i<size(); i++) {
      Transaction child = getTransaction(i);
      if (child.isCompound())
        leaves.addAll(child.getLeafTransactions());
      else
        leaves.add(child);
    }
    return leaves;
  }

  // for splitTransaction subclass?
  void setNewSplitFeature(AnnotatedFeatureI newSplitFeat) {
    this.newSplitFeature = newSplitFeat;
  }

  /** If split compound transaction (isSplit == true), this returns the new split off
      feature. getSeqFeature() returns the feature split off from. */
  public AnnotatedFeatureI getNewSplitFeature() { return newSplitFeature; }

  /** 
   * Short (one-line) summary of the object; concise alternative to toString() 
   * that displays every instance variable set in one of the constructors,
   * plus date and author.
   */
  public String oneLineSummary() {
    // not displaying: source
    // TODO - not really a 1-line summary!  also need indent level for this
    StringBuffer sb = new StringBuffer();
    sb.append("Compound[");
    sb.append("type=" + getCompoundTypeString());
    sb.append(",date=" + date);
    sb.append(",author=" + author);
    if (newSplitFeature != null) { sb.append(",newsplitfeat=" + newSplitFeature.getId()); }
    
    // TODO - doesn't handle nesting correctly
    Iterator ci = childTransactions.iterator();
    while (ci.hasNext()) {
      Transaction t = (Transaction)(ci.next());
      sb.append("  " + t.oneLineSummary());
    }
    sb.append("]\n");
    return sb.toString();
  }
}

// dont see need for this....???  am i missing something?
//   /**
//    * Another overloaded constructor with a list of SeqFeatureI
//    * @param features
//    */
//   public CompoundTransaction(java.util.List features) {
//     this.features = features;
//   }
  // A list of touched SeqFeatures objects
//  private java.util.List features;
//   /**
//    * An overloaded constructor with a specified SeqFeatureI.
//    * @param feature
//    */
//   public CompoundTransaction(SeqFeatureI feature) {
//     setSeqFeature(feature);
//   }

//   /**
//    * Override the method in the superclass.
//    */
//   public void setSeqFeature(SeqFeatureI feature) {
//     // Use a List object to keep track the changed SeqFeatureI object.
//     if (features == null)
//       features = new ArrayList();
//     else
//       features.clear();
//     features.add(feature);
//   }
  
//   /**
//    * Override the method in the superclass.
//    */
//   public SeqFeatureI getSeqFeature() {
//     if (features == null || features.size() < 1)
//       return null;
//     return (SeqFeatureI) features.get(0);
//   }
  
//   /**
//    * Get a list of SeqFeatureI objects that are changed during this Transaction.
//    * @return
//    */
//   public java.util.List getSeqFeatures() {
//     return features;
//   }
  
//   /**
//    * Set a list of SeqFeatureI objects that are changed during this Transaction.
//    * @param features
//    */
//   public void setSeqFeatures(java.util.List features) {
//     this.features = features;
//   }
  
//}
