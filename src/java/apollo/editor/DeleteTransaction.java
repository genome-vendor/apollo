/*
 * Created on Sep 15, 2004
 */
package apollo.editor;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.SequenceI;

/**
 * The transaction class for deleting operation in Apollo. This class can handle
 * one SeqFeatureI only.
 * @author wgm
 */
public class DeleteTransaction extends Transaction {

  /**
   * Default Constructor.
   */
  public DeleteTransaction() {
  }

  public DeleteTransaction(SeqFeatureI feature, SeqFeatureI parent) {
    setSeqFeature(feature);
    setParentFeature(parent);
  }
  
  public DeleteTransaction(SeqFeatureI feature, Object source) {
    setSeqFeature(feature);
    setSource(source);
  }

  /** source should come first to be consistent with add trans */
  public DeleteTransaction(SeqFeatureI feature, SeqFeatureI parent, Object source) {
    this(feature,parent);
    setSource(source);
  }
  
  public DeleteTransaction(SeqFeatureI feature, TransactionSubpart subpart) {
    setSeqFeature(feature);
    setSubpart(subpart);
  }
  
  public DeleteTransaction(SeqFeatureI feature,
                           TransactionSubpart subpart,
                           Object subpartValue) {
    setSeqFeature(feature);
    setSubpart(subpart);
    // isnt subpart value the post value which is null for delete?
    // will this mess db commit up?
    //setSubpartValue(subpartValue);
    setOldSubpartValue(subpartValue);
  }
  public DeleteTransaction(Object source, SeqFeatureI feature,
                           TransactionSubpart subpart, Object subpartValue,
                           int subpartRank) {
    this (feature,subpart,subpartValue);
    setSubpartRank(subpartRank);
    setSource(source);
  }
  
  public boolean isDelete() { return true; }

  /**
   * Get the deleted SeqFeatureI object.
   */
  public SeqFeatureI getDeletedFeature() {
    return getSeqFeature();
  }

  public SeqFeatureI getDeletedFeatureClone() {
    return getSeqFeatureClone();
  }
  
  public AnnotationChangeEvent generateAnnotationChangeEvent() {
    return new AnnotationDeleteEvent(this);
  }
  
  public AnnotationChangeEvent generateUndoChangeEvent()
  {
    return new AnnotationAddEvent(this);
  }

  public void editModel() {
    if (!hasSubpart())
      throw new IllegalStateException("no editModel on add whole feats yet");
    boolean isAdd = false;
    getSubpart().setValue(getSeqFeature(),getOldSubpartValue(),isAdd);
  }
  
  public void undo() {
    boolean isAdd = true; // undo -> add!
    if (hasSubpart()) {
      if (getSubpart().isComment()) {
        TransactionUtil.undoDeleteComment(this);
      }
      else if (getSubpart().isDbXref()) {
        TransactionUtil.undoDeleteDbxref(this);
      }
      else {
        getSubpart().setValue(getSeqFeature(),getOldSubpartValue(),isAdd);
      }
    }
    else {
      /*
      if (getParentFeature() == null) {
        System.out.println("No parent feature!!!!");
        return;
      }
      //all this craziness is necessary since when deleting a transcript, it clones it
      //and attaching an exon to this cloned transcript will not attach it to the
      //existing transcript
      SeqFeatureI parent = null;
      if (getAnnotatedFeature().isAnnotTop()) {
        parent = getParentFeature();
      }
      else {
        SeqFeatureI grandParent = getParentFeature().getParent();
        //search for existing parent
        for (Object o : grandParent.getFeatures()) {
          SeqFeatureI feat = (SeqFeatureI)o;
          if (feat.getId().equals(getParentFeature().getId())) {
            parent = feat;
            break;
          }
        }
      }
      //no existing parent found, so use the transaction's parent
      if (parent == null) {
        parent = getParentFeature();
      }
      parent.addFeature(getAnnotatedFeature());
      setParentFeature(parent);
      System.out.println("isTopLevelAnnot:\t" + getAnnotatedFeature().isAnnotTop());
      System.out.println("parent_old:\t" + getParentFeature() + "\t" + getParentFeature().hashCode());
      System.out.println("parent_new:\t" + parent + "\t" + parent.hashCode());
      System.out.println("child:\t" + getAnnotatedFeature() + "\t" + getAnnotatedFeature().hashCode());
      */
      if (getParentFeature() != null) {
        getParentFeature().addFeature(getAnnotatedFeature());
        /*
        System.out.println("DeleteTransaction.undo():\tparent:\t" + getParentFeature() + "\t" + getParentFeature().hashCode());
        System.out.println("DeleteTransaction.undo():\tchild:\t" + getAnnotatedFeature() + "\t" + getAnnotatedFeature().hashCode());
        */
      }
      else if (getSeqFeature() instanceof SequenceEdit) {
        SequenceEdit edit = (SequenceEdit)getSeqFeature();
        SequenceI seq = edit.getRefSequence();
        seq.addSequenceEdit(edit);
      }
    }
  }

  public String toString() {
    return "DeleteTransaction[" +
      "source=" + getSource() + "\n                 " +
      "feature=" + getDeletedFeature() + "\n                 " +
      "parent=" + getParentFeature() + "\n                 " +
      "subpartRank=" + getSubpartRank() + "\n                 " +
      "subpart=" + getSubpart() + "\n                 " +
      "oldVal=" + getOldSubpartValue() + "]";
  }

  /** 
   * Short (one-line) summary of the object; concise alternative to toString() 
   * that displays every instance variable set in one of the constructors,
   * plus date and author.
   */
  public String oneLineSummary() {
    // not displaying: source
    StringBuffer sb = new StringBuffer();
    sb.append("Del[");
    sb.append("feat=" + getSeqFeature().getId());
    SeqFeatureI parent = getParentFeature();
    if(parent != null)
    	sb.append(",parent=" + getParentFeature().getId());
    sb.append(",date=" + date);
    sb.append(",author=" + author);
    sb.append(",subpart=" + getSubpart());
    sb.append(",rank=" + getSubpartRank());
    sb.append(",oldval=" + getOldSubpartValue());
    sb.append("]");
    return sb.toString();
  }
}


