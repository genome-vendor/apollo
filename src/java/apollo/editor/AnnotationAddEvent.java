package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;

/** AnnotationChangeEvent for add */

public class AnnotationAddEvent extends AnnotationChangeEvent {

  /** A feature has been added, e.g. gene, transcript, exon */
  public AnnotationAddEvent(Object source, AnnotatedFeatureI addedAnnot) {
    super(source,addedAnnot);
    // setOperation(FeatureChangeEvent.ADD);
  }

  /** A subpart has been added, e.g. comment, synonym. This will cause hasSubpart()
      to be true. */
  public AnnotationAddEvent(Object source, AnnotatedFeatureI annot,
                            TransactionSubpart subpart) {
    super(source,annot,subpart);
  }

  public AnnotationAddEvent(Object source, AnnotatedFeatureI feat,
                            TransactionSubpart subpart, boolean singular) {
    this(source,feat,subpart);
    setSingularEventState(singular);
  }

  /** Adding event
   * 
   * @param trans
   */
  AnnotationAddEvent(AddTransaction trans) {
    this(trans.getSource(),trans.getAnnotatedFeature(),trans.getSubpart());
    setTransaction(trans);
  }

  /** Deletion undo event
   * 
   * @param trans
   */
  AnnotationAddEvent(DeleteTransaction trans) {
    this(trans.getSource(),trans.getAnnotatedFeature(),trans.getSubpart());
    setUndo(true);
    setTransaction(trans);
  }

  public boolean isAdd() { return true; }

  protected String getOperationAsString() { return "ADD"; }
}
