package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for performing a frame shift on a feature
 * 
 * NOTE: I think that there might be some kind of limit to the number of 
 * frame shifts allowed on a feature.
 */
public class FrameShiftAction extends AbstractAction {

  public enum FrameShiftType {
    PLUS1, MINUS1
  };

  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private SeqFeatureI feature;
  private int basepair;
  private FrameShiftType shiftType;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   * @param plus1 true if this will perform a +1 frame shift
   */
  public FrameShiftAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor, FrameShiftType shiftType) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.basepair = -1;
    this.shiftType = shiftType;
    this.setEnabled(false);
  }

  /**
   * Performs the insertion
   */
  public void actionPerformed(ActionEvent e) {

    // TODO check that base pair is within exon region
    if (basepair < 0 || feature == null || shiftType == null) {
      throw new IllegalStateException("basepair:" + basepair + " feature:"
          + feature + " shiftType:" + shiftType);
    }

    annotationEditor.setSelections(null, null, null, null);
    if (shiftType == FrameShiftType.PLUS1) {
      annotationEditor.setFrameShiftPosition((ExonI) feature, basepair, true);
    } else {
      annotationEditor.setFrameShiftPosition((ExonI) feature, basepair, false);
    }
    curationState.getSZAP().repaint();
    // TODO repaint all msa's

  }

  /**
   * Sets what feature the frame shift will be added to
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * Sets the location of the frame shift
   * @param basepair genomic coordinates 
   * 
   * NOTE: Does it only make sense to have a frame shift in an exon?
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }

  /**
   * Sets the shift type for this action
   * @param shiftType
   */
  public void setShiftType(FrameShiftType shiftType) {
    this.shiftType = shiftType;
  }

}
