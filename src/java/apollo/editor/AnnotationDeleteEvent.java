package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.util.SeqFeatureUtil;

public class AnnotationDeleteEvent extends AnnotationChangeEvent {

  //private Comment deletedComment;

  /** Delete whole feature. Have to have parent as deletedFeat no longer connected
      with parent */
  public AnnotationDeleteEvent(Object source, AnnotatedFeatureI deletedFeat, 
                               SeqFeatureI parent) {
    super(source,deletedFeat);
    setDeletedFeature(deletedFeat);
    setParentFeature(parent);
    // backwards compatiability - or weed out?
    // setOperation(FeatureChangeEvent.DELETE); // ??
  }

  /** Delete subpart (comment, synonym) */
  public AnnotationDeleteEvent(Object source, AnnotatedFeatureI feat,
                               TransactionSubpart subpart) {
    super(source,feat,subpart);
  }
  public AnnotationDeleteEvent(Object source, AnnotatedFeatureI feat,
                               TransactionSubpart subpart, boolean singular) {
    this(source,feat,subpart);
    setSingularEventState(singular);
  }

  AnnotationDeleteEvent(DeleteTransaction trans) {
    super(trans);
//     super(trans.getSource(),trans.getAnnotatedFeature());
//     if (trans.hasSubpart())
//       setSubpart(trans.getSubpart());
  }

  //Delete undo
  AnnotationDeleteEvent(AddTransaction trans) {
    super(trans);
    setUndo(true);
  }
  
  public boolean isDelete() { return true; }

  public AnnotatedFeatureI getAnnotTop() {
    if (getChangedAnnot().isAnnotTop())
      return getChangedFeature().getAnnotatedFeature();
    return SeqFeatureUtil.getAnnotRoot(getParentFeature().getAnnotatedFeature());
  }
  
  protected String getOperationAsString() { return "DELETE"; }

}
