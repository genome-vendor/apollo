package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for merging two exons.
 */
public class MergeExonAction extends AbstractAction {

  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private SeqFeatureI feature, mergeFeature;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   */
  public MergeExonAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.mergeFeature = null;
    this.setEnabled(false);
  }

  /**
   * Performs the merge
   */
  public void actionPerformed(ActionEvent e) {

    // NOTE: Are there any other restraints?
    if (feature == null || mergeFeature == null) {
      throw new IllegalStateException("feature:" + feature + " mergeFeature:"
          + mergeFeature);
    }

    Vector<SeqFeatureI> exons = new Vector<SeqFeatureI>();
    exons.addElement(feature);
    exons.addElement(mergeFeature);
    annotationEditor.setSelections(exons, exons, null, null);
    annotationEditor.mergeExons();

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
  }

  /**
   * Sets first feature of the merge
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * Sets second feature of the merge
   * @param feature
   */
  public void setMergeFeature(SeqFeatureI feature) {
    this.mergeFeature = feature;
  }

}
