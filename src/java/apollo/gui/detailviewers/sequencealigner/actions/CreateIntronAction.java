package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for creating a single base intron within an exon region.
 */
public class CreateIntronAction extends AbstractAction {

  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private int basepair;
  private SeqFeatureI feature;


  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   */
  public CreateIntronAction(String name, GuiCurationState curationState, 
      AnnotationEditor annotationEditor) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.basepair = -1;
    this.setEnabled(false);
  }

  /**
   * Performs the insertion
   */
  public void actionPerformed(ActionEvent e) {

    if (basepair < 0 || feature == null) {
      throw new IllegalStateException(
          "basepair:" + basepair + " feature:" + feature);
    }

    // TODO check that basepair falls within the bounds of an exon region

    Vector<SeqFeatureI> exons = new Vector<SeqFeatureI>();
    exons.addElement(feature);

    annotationEditor.setSelections(basepair, -1, exons, exons, null, null);
    annotationEditor.splitExon();

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
    // TODO Try to create intron based on donor/selector.

    /*
    // see Utils.findSplices
    if (Utils.notTooSmall(feature.getLow(), donor_bp) &&
        Utils.notTooSmall(acceptor_bp, feature.getHigh())) {

      int low = Math.min(donor_bp, acceptor_bp);
      int high = Math.min(donor_bp, acceptor_bp);

      annotationEditor.setSelections(low, high, exons, exons, null, null);

    } else { // Otherwise create intron at current base
      annotationEditor.setSelections(basepair, -1, exons, exons, null, null);
    }
     */
  }

  /**
   * Sets what feature the exon will be added to
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * Sets the location of the one base intron
   * @param basepair genomic coordinates 
   * 
   * The base pair should be within an exon region for the feature
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }

}
