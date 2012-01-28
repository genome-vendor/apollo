package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;

/** This class is temporary in theory. FeatureEditorDialog does a limited undo
    via cloning and replacing. Once we have full undo I dont think we will need
    Replace event any longer */

public class AnnotationReplaceEvent extends AnnotationChangeEvent {

  public AnnotationReplaceEvent(Object source, AnnotatedFeatureI replacedGene,
                                AnnotatedFeatureI newGene) {
    super(source,newGene);
    setReplacedFeature(replacedGene);
    // setOperation(FeatureChangeEvent.REPLACE); // pase?
  }
  public boolean isReplace() { return true; }

  boolean isTransactionOperation() { return false; }
}
