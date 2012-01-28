package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.synteny.GuiCurationState;

/**
 * An Action class for creating a single base exon within an intron region.
 * 
 * NOTE: Will this work in a non intron region? In that case the feature
 * would be null. For now I'll assume it won't, but maybe it should.
 */
public class CreateExonAction extends AbstractAction {

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
  public CreateExonAction(String name, GuiCurationState curationState, 
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

    // TODO check that basepair falls within the bounds of an intron region

    SeqFeatureI temp = new SeqFeature();
    temp.setStrand(feature.getStrand());
    temp.setStart(basepair); 
    temp.setEnd(basepair); 

    // Should feature be required to have a type of Transcript?
    Vector<SeqFeatureI> transcripts = new Vector<SeqFeatureI>();
    transcripts.addElement(feature);
    
    Vector<SeqFeatureI> exons = new Vector<SeqFeatureI>();
    exons.addElement(temp);
    
    annotationEditor.setSelections(transcripts,
        transcripts,
        exons,
        null);
    annotationEditor.addExons();

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
    // TODO add exon based on donor/acceptor
  }

  /**
   * Sets what feature the exon will be added to
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * Sets the location of the one base exon
   * @param basepair genomic coordinates 
   * 
   * The base pair should be within an intron region for the feature
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }
}
