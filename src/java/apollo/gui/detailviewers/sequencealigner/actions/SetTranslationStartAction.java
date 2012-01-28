package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanel;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for setting a translation start
 * 
 * NOTE: I'm not really sure what this actually does.
 * What is a translation start? and how does it relate to the feature?
 *
 */
public class SetTranslationStartAction extends AbstractAction {

  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private SeqFeatureI feature;
  private int basepair;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   */
  public SetTranslationStartAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.basepair = -1;
    this.setEnabled(false);
  }

  /**
   * Performs the update
   */
  public void actionPerformed(ActionEvent e) {

    // TODO check that the basepair is within the bounds of an exon
    if (basepair < 0 || feature == null) {
      throw new IllegalArgumentException("basepair:" + basepair + " feature:"
          + feature);
    }

    annotationEditor.setSelections(null, null, null, null);
    annotationEditor.setTranslationStart((ExonI) feature, basepair);

    curationState.getSZAP().repaint();
    // TODO repaint all msa's

  }

  /**
   * Sets the Feature which will be affected
   * 
   * @param feature should be of type apollo.datamodel.ExonI
   * 
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * The base which will become the new translation start
   * 
   * @param basepair
   * 
   * NOTE should be within the exon region of the feature.
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }

}
