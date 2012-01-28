package apollo.editor;

import java.util.EventObject;
import java.util.Vector;

import apollo.datamodel.*;
import apollo.util.SeqFeatureUtil;

/**
 * A controller managed event class which signals when a change is made to
 * a set of annotations and what type of change occurred. Objects interested
 * in listening for these event should implement the FeatureChangeListener
 * interface and register with the controller.
 Eventually I would like this to just be a carrier/wrapper for a transaction,
 so it could/should just query transaction for everything. All of its fields are
 really redundant of transactions.
 */

public class AnnotationChangeEvent extends FeatureChangeEvent {


  private AnnotatedFeatureI annotFeature; // ????
  private AnnotatedFeatureI deletedAnnot;

  /** For subparts that are lists, like comments and synoyms we need to know which
      item in the list is being operated on. FeatCngEv? */
  private int subpartRank;

  private String oldString;

  private Comment deletedComment;

  private String oldId;

  private Transaction transaction;

  private boolean isUndo;
  
  /** Solely for edit session done event */
  protected AnnotationChangeEvent(Object source) {
    super(source);
  }

  void setUndo(boolean isUndo)
  {
    this.isUndo = isUndo;
  }
  
  public boolean isUndo()
  {
    return isUndo;
  }
  
  AnnotationChangeEvent(Transaction trans) {
    super(trans.getSource(),trans.getAnnotatedFeature());
    setTransaction(trans);
    if (trans.hasSubpart())
      setSubpart(trans.getSubpart());
  }
  
  /** No subpart */
  protected AnnotationChangeEvent(Object source, AnnotatedFeatureI feature) {
    super(source,feature);
  }
  
  /** With subpart */
  protected AnnotationChangeEvent(Object source, AnnotatedFeatureI changedFeat,
                                  TransactionSubpart subpart) {
    super(source,changedFeat,subpart);
  }

  void setSource(Object source) {
    super.source = source; // source is protected var in EventObject
  }

  void setTransaction(Transaction trans) {
    this.transaction = trans;
  }

  boolean hasTransaction() {
    return transaction != null;
  }

  Transaction getTransaction() {
    return transaction;
  }

  
  public SeqFeatureI getParentFeature() {
    if (super.parentFeature != null)
      return parentFeature;
    // get from delete transaction
    if (hasTransaction() && getTransaction().getParentFeature() != null) {
      return getTransaction().getParentFeature();
    } else {
      SeqFeatureI cf = getChangedFeature();
      return (cf == null) ? null : cf.getRefFeature();
    }
  }

  /** If compound event return # of child events, default 0 */
  public int getNumberOfChildren() { return 0; }
  /** Default is null. If compound event return ith chil event */
  public AnnotationChangeEvent getChildChangeEvent(int i) { return null; }
  /** If compound change event, add trans to compound transaction 
   AnnotationCompoundEvent overrides no-op */
  public void addTransaction(Transaction trans) {}

  public AnnotatedFeatureI getChangedAnnot() {
    if (getChangedFeature() == null)// end of edit - is that right?
      return null;
    return getChangedFeature().getAnnotatedFeature();
  }

  public AnnotatedFeatureI getDeletedFeature() {
    return deletedAnnot;
    //return getTransaction().getDeletedFeature();
  }

  public void setDeletedFeature(AnnotatedFeatureI delFeat) {
    this.deletedAnnot = delFeat;
  }

  /** rename get change top? annot top? should this return an AnnotatedFeatureI?
   rename getAnnotRoot? */
  public AnnotatedFeatureI getAnnotTop() {
    // if change top explicitly set return it
    if (super.getChangeTop() != null) // do we need this - delete?
      return super.getChangeTop().getAnnotatedFeature(); 
    if (getChangedAnnot() == null) //this is true of end of edit, or should there be
      return null;
    AnnotatedFeatureI annotTop = SeqFeatureUtil.getAnnotRoot(getChangedAnnot());
    setChangeTop(annotTop);
    return annotTop;
  }

  public SeqFeatureI getChangeTop() {
    return getAnnotTop();
  }

  /** For subparts that are lists, like comments and synoyms we need to know which
      item in the list is being operated on */
  public void setSubpartRank(int rank) { this.subpartRank = rank; }
  public int getSubpartRank() { return subpartRank; }

  /** for updates(name...) and deletes(syn) to strings */
  public void setOldString(String oldString) {
    this.oldString = oldString;
  }

  public boolean isStringChange() {
    return getOldString() != null;
  }
  
  public String getOldString() {
    if (hasTransaction() && transaction.getSubpart().isString())
      return (String)transaction.getOldSubpartValue();
    return oldString;
  }

  /** For comment deletes */
  public void setOldComment(Comment comment) {
    deletedComment = comment;
  }
  public Comment getOldComment() {
    return deletedComment;
  }
  public boolean isCommentChange() { 
    return getSubpart() == TransactionSubpart.COMMENT;
  }

  /** Set original id before update of parent changed it. */
  public void setOldId(String oldId) {
    this.oldId = oldId;
  }

  /** A parent update for transcripts will change the transcript id and make it
      impossible to update the db. getOldId returns the original ID of the transcript
      before changes. */
  public String getOldId() {
    if (hasTransaction())
      return transaction.getOldId();
    return this.oldId;
  }

  public String toString() {
    if (hasTransaction())
      return transaction.toString();
    else
      return super.toString(); // pase
  }
  
}
  // AnnotationUpdateEvent getUpdateEvent() ???

//   static String asString(int changeSubType) {
//     // NOTE: No breaks
//     switch (changeSubType) {
//     case COMMENT:
//       return new String("COMMENT");
//       //case EVIDENCE:  return new String("EVIDENCE");
//     case EXON:
//       return new String("EXON");
//     case TRANSLATION:
//       return new String("TRANSLATION");
//     case TRANSCRIPT:
//       return new String("TRANSCRIPT");
//     case ANNOTATION:
//       return new String("ANNOTATION");

//     case TYPE:
//       return TransactionClass.TYPE.toString();

//     default:
//       return new String("UNKNOWN OBJECT CLASS (" + changeSubType + ")");
//     }
//   }

//   public static int toNum(String changeSubType) {
//     if (changeSubType.equals("COMMENT"))
//       return COMMENT;
//     //if (changeSubType.equals("EVIDENCE"))  return EVIDENCE;
//     if (changeSubType.equals("EXON"))
//       return EXON;
//     if (changeSubType.equals("TRANSLATION"))
//       return TRANSLATION;
//     if (changeSubType.equals("TRANSCRIPT"))
//       return TRANSCRIPT;
//     if (changeSubType.equals("ANNOTATION"))
//       return ANNOTATION;
//     else
//       return -1;  // unknown
//   }


//   /**
//    * I think feature1 and feature2 are used differently for each type.
//    * For deletions feature2 is the deleted feature and feature1 is the parent
//    * of the deleted feature(feature2)
//    For ADD feat1 is the feature being added to, feat2 is the  feature being added.
//    ADD EVIDENCE gene is top, transcript feat1, evidence feat2
//    ADD TRANSCRIPT: top gene, feat1 gene, feat2 transcript
//    ADD EXON: top gene, feat1 transcript, feat2 exon
//    ADD GENE: top gene, feat1 geneHolder, feat2 gene
//    phase this out
//    */
//   public AnnotationChangeEvent(Object      source,
//                                SeqFeatureI changeTop,
//                                int         operation,
//                                int         objectClass,
//                                SeqFeatureI feature1,
//                                SeqFeatureI feature2) {
//     super(source, changeTop, operation, /*objectClass,*/ feature1, feature2);
//     //if (isTransactionOperation(operation))
//     //transaction = new Transaction(this);
//   }
  // Replace these constants with TransactionClass
//   public static final int            COMMENT      = 10; // subpart
//   // i commented out evidence events - evidence is irrelevant
//   //public static final int            EVIDENCE     = 11;
//   // I dont think translation is a class
//   public static final int            TRANSLATION  = 12; // subpartish
//   public static final int            EXON         = 13;
//   public static final int            TRANSCRIPT   = 14;
  // ANNOTATION still used by AE - fix this
//   public static final int            ANNOTATION   = 15;

  /** This is controversial ground. Type is a part of one of the above objects.
      Should there be another SubObjectClass? ObjectPart? i think so - for now
      just shove here */
//  public static final int   TYPE = 16; // subpart
//   public static AnnotationChangeEvent getSessionDoneEvent(Object source) {
//     return new AnnotationChangeEvent(source);
//   }
//   private AnnotationChangeEvent(Object source, TransactionOperation op, 
//                                AnnotatedFeatureI feat) {
//     super(source,op,feat);
//   }


//   /** Constructor with no feature2(null) - phase out*/
//   public AnnotationChangeEvent(Object source,SeqFeatureI changeTop,int operation,
//                                int objectClass,SeqFeatureI feature1) {
//     this(source,changeTop,operation,objectClass,feature1,null);
//   }

  /** should be abstract? */
  //void fireEvent(AnnotationChangeListener acl) {}
