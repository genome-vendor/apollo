package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for deleting an exon
 */
public class DeleteExonAction extends AbstractAction {

  private SeqFeatureI feature;
  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   */
  public DeleteExonAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.setEnabled(false);
  }

  /**
   * Performs the deletion
   */
  public void actionPerformed(ActionEvent e) {

    if (!canExecute()) {
      throw new IllegalStateException("feature:" + feature);
    }

    Vector<SeqFeatureI> exons = new Vector<SeqFeatureI>();
    exons.addElement(feature);

    annotationEditor.setSelections(exons, exons, null, null);
    annotationEditor.deleteSelectedFeatures();

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
  }

  /**
   * Sets the exon which is to be deleted
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  private boolean canExecute() {
    return feature != null && !feature.hasKids() && feature.isExon();
  }

}
