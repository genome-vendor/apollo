package apollo.editor;

import apollo.datamodel.SeqFeatureI;

public class UpdateParentTransaction extends CompoundTransaction {

  private SeqFeatureI child;
  private SeqFeatureI oldParent;
  private SeqFeatureI newParent;

  public UpdateParentTransaction(Object src,SeqFeatureI child,SeqFeatureI oldParent,SeqFeatureI newParent) {
    super(src);
    this.child = child;
    this.oldParent = oldParent;
    this.newParent = newParent;
    addTransaction(createDeleteTransaction());
    addTransaction(createAddTransaction());
  }

  public boolean hasCompoundType() { return true; }

  public boolean isUpdateParent() { return true; }

  public String toString() {
    return "CompoundTrans num kids "+size()+" UPDATE PARENT";
  }

  /** Returns false. this means dont flatten out this transactions when adding it
      as a child to another compound transactions, which is the default. in other
      words preserve this comp trans in the comp trans heirarchy. */
  protected boolean flattenOnAddingToCompTrans() { return false; }

  private DeleteTransaction createDeleteTransaction() {
    // clone wont be found by DFS.findDrawable to delete from view
    //SeqFeatureI childCopy = child.cloneFeature();
    return new DeleteTransaction(child,oldParent);
  }

  private AddTransaction createAddTransaction() {
    return new AddTransaction(child);
  }

  /** 
   * Short (one-line) summary of the object; concise alternative to toString() 
   * that displays every instance variable set in one of the constructors,
   * plus date and author.
   */
  public String oneLineSummary() {
    // not displaying: source
    // TODO - this is actually a compound transaction
    StringBuffer sb = new StringBuffer();
    SeqFeatureI sf = getSeqFeature();

    sb.append("UpdateParent[");
    sb.append("feat=" + ((child == null) ? "null" : child.toString()));
    sb.append(",date=" + date);
    sb.append(",author=" + author);
    sb.append(",oldparent=" + oldParent.getId());
    sb.append(",newparent=" + newParent.getId());
    sb.append("]");
    return sb.toString();
  }
}
