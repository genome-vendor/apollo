package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;

public class AnnotationCompoundEvent extends AnnotationChangeEvent {

  private CompoundTransaction compoundTransaction;

  /**Constructor
   * It defaults the transaction to not undo
   * 
   * @param ct - CompoundTransaction to generate the events from
   */
  public AnnotationCompoundEvent(CompoundTransaction ct) {
    super(ct.getSource());
    compoundTransaction = ct;
    setTransaction(ct);
  }

  public boolean isCompound() { return true; }

  public int getNumberOfChildren() {
    if (!hasTransaction())
      return 0;
    return getTransaction().size();
  }

  public void addTransaction(Transaction trans) {
    compoundTransaction.addTransaction(trans);
  }

  public void addTransaction(CompoundTransaction trans) {
    compoundTransaction.addTransaction(trans);
  }
  
  public AnnotationChangeEvent getChildChangeEvent(int i) {
    if (!hasTransaction())
      return null;
    // cache?? get from trans??
    
    //System.out.println(getTransaction().getTransaction(i));
    
    if (isUndo()) {
      return getTransaction().getTransaction(getNumberOfChildren() - i - 1).generateUndoChangeEvent();
    }
    else {
      return getTransaction().getTransaction(i).generateAnnotationChangeEvent();
    }
  }

  /** Returns true. This method should be phased out once compound events and
      transactions are fully in place - not there yet */
  public boolean isEndOfEditSession() { return true; }


  /** returns the annot top of the first child. this bascially assumes all children
      have same annot top, but what else are ya gonna do here? i think this is
      probably sufficient */
  public AnnotatedFeatureI getAnnotTop() {
    if (getNumberOfChildren() == 0)
      return null; // shouldnt happen
    return getChildChangeEvent(0).getAnnotTop();
  }

  public boolean isMove() {
    return getTransaction().isUpdateParent();
  }
  
}
