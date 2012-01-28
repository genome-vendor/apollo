package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;

public class AnnotationUpdateEvent extends AnnotationChangeEvent 
  implements UpdateDetailsI {

  private SeqFeatureI oldParent; // PARENT - AnnotatedFeatureI?
  private RangeI oldRange; // LIMITS
  //private int oldExonEnd;

  // or String oldValueString? 
  // or private int?Class?Enum?String? subpartType;
  // maybe TransactionSubpart could get involved? TransactionSubpart could know
  // what type its value is.
  private boolean isIntUpdate = false;
  private int oldInt;
  
  private Comment oldComment;

  // OR int newInt??? but its not a new value - seq err position is needed info
  // to locate the actual subpart - TranSubpart not enough info - for 
  // sequencing errors each basepair is a separate "subpart"
  // this is not old position - this is new
  private int newSequencingErrorPosition; // ??
  

  //private int oldTranslationStop;
  //private int oldPlus1FrameShiftPosition;
  //private int oldMinus1FrameShiftPosition;


  /** New way of doing annotation change events - they are just carriers of
      transactions. This deals with all the redundancy between annotation change
      events and transactions. */
  public AnnotationUpdateEvent(UpdateTransaction trans) {
    // for now do extraction here, default singular event
    this(trans.getSource(),trans.getSeqFeature().getAnnotatedFeature(),
          trans.getSubpart(),true);
    setTransaction(trans);
  }
//   public AnnotationUpdateEvent(Transaction trans) { // --> CompoundTrans???
//     // for now do extraction here, default singular event
//     this(trans.getSource(),trans.getSeqFeature().getAnnotatedFeature(),
//           trans.getSubpart(),true);
//     setTransaction(trans);
//   }

  public AnnotationUpdateEvent(Object source,AnnotatedFeatureI updateFeat,
                               TransactionSubpart subpart) {
    super(source,updateFeat,subpart);
  }
  public AnnotationUpdateEvent(Object source,AnnotatedFeatureI updateFeat,
                               TransactionSubpart subpart, boolean singularEvent) {
    this(source,updateFeat,subpart);
    setSingularEventState(singularEvent);
  }

  public boolean isUpdate() { return true; }

  /** A "move" is when a parent is updated */
  public boolean isMove() { 
    return getSubpart() == TransactionSubpart.PARENT;
  }

  protected String getOperationAsString() { return "UPDATE"; }

  /** generic way to get details of update */
  public UpdateDetailsI getUpdateDetails() {
    return this;
  }

  public boolean isStringUpdate() {
    return super.isStringChange(); // is this silly?
  }

  /** For update parent(isMove) */
  void setOldParent(SeqFeatureI oldParent) {
    this.oldParent = oldParent;
  }

  public SeqFeatureI getOldParent() {
    if (oldParent == null && isMove() && hasTransaction())
      if (getTransaction().getOldSubpartValue() instanceof SeqFeatureI)
        oldParent = (SeqFeatureI)getTransaction().getOldSubpartValue();
    return oldParent;
  }


  /** If the exons limis are being updated, need the old exons start and end
      to find it in the database - its identity */
  void setOldRange(RangeI oldRange) {
    this.oldRange = oldRange;
  }

  public boolean isRangeUpdate() {
    return getSubpart().isLimits();
    //return getSubpart() == TransactionSubpart.LIMITS;
  }

  public RangeI getOldRange() {
    return oldRange;
  }

//   void setOldExonEnd(int oldExonEnd) {
//     this.oldExonEnd = oldExonEnd;
//   }

//   public int getOldExonEnd() {
//     return oldExonEnd;
//   }

  void setNewSequencingErrorPosition(int pos) { // ??
    this.newSequencingErrorPosition = pos;
  }

  public int getNewSequencingErrorPosition() { // ???
    return newSequencingErrorPosition;
  }

  void setOldInt(int oldInt) {
    this.oldInt = oldInt;
    isIntUpdate = true; // subpartType = Class.INTEGER?
  }
  public int getOldInt() {
    return oldInt;
  }
  

  public boolean isIntUpdate() { return isIntUpdate; }

  public boolean isBooleanUpdate() {
    return getSubpart().isBoolean();
  }

  public void setOldComment(Comment oldComment) { this.oldComment = oldComment; }
  public Comment getOldComment() { return oldComment; }
  public boolean isCommentUpdate() { return oldComment != null; }
  // public void setOldSeqEdit(SeqEdit seqEdit) for undo on seq errors
}
