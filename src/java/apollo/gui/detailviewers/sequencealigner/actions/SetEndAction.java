package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.synteny.GuiCurationState;

/**
 * An action class for setting the start or end of a feature
 */
public class SetEndAction extends AbstractAction {

  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private SeqFeatureI feature;
  private int basepair, terminus;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   * @param terminus  Either DrawableAnnotationConstants.START or
   *                         DrawableAnnotationConstants.END or
   *                         DrawableAnnotationConstants.BOTHENDS
   */
  public SetEndAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor, int terminus) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.basepair = -1;
    setTerminus(terminus);
    this.setEnabled(false);
  }

  /**
   * Performs the update
   */
  public void actionPerformed(ActionEvent arg0) {

    // TODO check that the basepair is within the bounds of an exon
    if (basepair < 0 || feature == null) {
      throw new IllegalArgumentException("basepair:" + basepair + " feature:"
          + feature);
    }

    SeqFeatureI temp = new SeqFeature();
    temp.setStrand(feature.getStrand());
    temp.setStart(basepair);
    temp.setEnd(basepair);

    Vector<SeqFeatureI> temps = new Vector<SeqFeatureI>();
    temps.addElement(temp);

    Vector<SeqFeatureI> exons = new Vector<SeqFeatureI>();
    exons.addElement(feature);

    annotationEditor.setSelections(exons, exons, temps, null);
    annotationEditor.setExonTerminus(terminus);

    //annotationEditor.setExonTerminus(annotationEditor.END); // set3prime
    //annotationEditor.setExonTerminus(annotationEditor.START); // set5prime

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
  }

  /**
   * Sets the feature which will have its start or end updated
   * 
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * The base which will become the new start or end
   * 
   * @param basepair
   * 
   * NOTE should be within the exon region of the feature.
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }

  /**
   * Sets which end will be updated
   * 
   * @param terminus Either DrawableAnnotationConstants.START or
   *                        DrawableAnnotationConstants.END or
   *                        DrawableAnnotationConstants.BOTHENDS
   */
  public void setTerminus(int terminus) {
    if (terminus == DrawableAnnotationConstants.START
        || terminus == DrawableAnnotationConstants.END
        || terminus == DrawableAnnotationConstants.BOTHENDS) {
      this.terminus = terminus;
    } else {
      throw new IllegalArgumentException("Unknown terminus:" + terminus);
    }
  }

}
