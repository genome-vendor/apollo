package apollo.editor;

import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Exon;
import apollo.datamodel.Transcript;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.SequenceI;

/**
 * Transaction for adding a SeqFeatureI.
 *
 * @author wgm
 */
public class AddTransaction extends Transaction {

  private boolean isPeptide = false;
  private String residues = null; // for insertion & substitution residue

  public AddTransaction() {
  }
  
  public AddTransaction(SeqFeatureI feature) {
    setSeqFeature(feature);
    FeatureSetI parent = feature.getParent();
    if (parent != null) {
      setParentFeature(parent);
    }
  }

  AddTransaction(Object source, SeqFeatureI feat) {
    this(feat);
    setSource(source);
  }
  
  /** yuck! peptide needs to be a feature! peptide stuff is done through 
      transcript for now. set isPeptide to true for this to add peptide
      instead of transcript */
  AddTransaction(Object source, SeqFeatureI transcript, boolean isPeptide) {
    this(source,transcript);
    this.isPeptide = isPeptide;
  }
  
  public AddTransaction(SeqFeatureI feature, TransactionSubpart subpart) {
    setSeqFeature(feature);
    setSubpart(subpart);
  }
  
  public AddTransaction(SeqFeatureI feature, TransactionSubpart subpart,
                        Object addedValue) {
    this(feature, subpart);
    setNewSubpartValue(addedValue);
  }
  public AddTransaction(Object src, SeqFeatureI feat, TransactionSubpart subpart,
                        Object addedValue) {
    this(feat,subpart,addedValue);
    setSource(src);
  }
  
  public AddTransaction(SeqFeatureI feature, TransactionSubpart subpart,
                        Object addedValue, int subpartRank) {
    this(feature, subpart, addedValue);
    setSubpartRank(subpartRank);
  }
  public AddTransaction(Object source,SeqFeatureI feature,
                        TransactionSubpart subpart,
                        Object addedValue, int subpartRank) {
    this(feature, subpart, addedValue,subpartRank);
    setSource(source);
  }
  
  public SeqFeatureI getAddedFeature() {
    return getSeqFeature();
  }

  public SeqFeatureI getAddedFeatureClone() {
    return getSeqFeatureClone();
  }

  public boolean isAddPeptide() {
    return isPeptide;
  }

  /** Seq errors insertion & substition include residues that need to be inserted
      with feature */
  public String getResidues() {
    return residues;
  }

  public void setResidues(String res) {
    residues = res;
  }

  // --> its own feature
  //public boolean isAddSeqError() {
  //return getSubpart() != null && getSubpart().isSeqError();}
  
  public boolean isAdd() { return true; }

  public AnnotationChangeEvent generateAnnotationChangeEvent() {
    return new AnnotationAddEvent(this);
  }

  public AnnotationChangeEvent generateUndoChangeEvent()
  {
    return new AnnotationDeleteEvent(this);
  }
  
  public void editModel() {
    if (!hasSubpart())
      throw new IllegalStateException("no editModel on add whole feats yet");
    boolean isAdd = true;
    getSubpart().setValue(getSeqFeature(),getNewSubpartValue(),isAdd);
  }

  public void undo() {
    boolean isAdd = false; // for undo we delete!
    if (hasSubpart()) {
      if (getSubpart().isComment()) {
        TransactionUtil.undoAddComment(this);
      }
      else if (getSubpart().isDbXref()) {
        TransactionUtil.undoAddDbxref(this);
      }
      else {
        getSubpart().setValue(getSeqFeature(),getNewSubpartValue(),isAdd);
      }
    }
    else {
      if (getParentFeature() != null) {
        ((apollo.datamodel.FeatureSetI)getParentFeature()).deleteFeature(getAnnotatedFeature());
        /*
        System.out.println("AddTransaction.undo():\tparent:\t" + getParentFeature() + "\t" + getParentFeature().hashCode());
        System.out.println("AddTransaction.undo():\tchild:\t" + getAnnotatedFeature() + "\t" + getAnnotatedFeature().hashCode());
        */
      }
      else if (getSeqFeature() instanceof SequenceEdit) {
        SequenceEdit edit = (SequenceEdit)getSeqFeature();
        SequenceI seq = edit.getRefSequence();
        seq.removeSequenceEdit(edit);
      }
    }
    

  }

  protected String getOperationString() { return "ADD"; }

  /** 
   * Short (one-line) summary of the object; concise alternative to toString() 
   * that displays every instance variable set in one of the constructors,
   * plus date and author.
   */
  public String oneLineSummary() {
    // not displaying: source
    StringBuffer sb = new StringBuffer();
    sb.append("Add[");
    sb.append("feat=" + getSeqFeature().getId());
    sb.append(",date=" + date);
    sb.append(",author=" + author);
    sb.append(",ispep=" + isPeptide);
    sb.append(",rank=" + getRank());
    sb.append(",subpartRank=" + getSubpartRank());
    sb.append(",subpart=" + getSubpart());
    sb.append(",newval=" + getNewSubpartValue());
    sb.append("]");
    return sb.toString();
  }
}
