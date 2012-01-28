package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.synteny.GuiCurationState;

/**
 * An action class for brining up the display sequence window
 */
public class DisplaySequenceAction extends AbstractAction {

  private GuiCurationState curationState;
  private SeqFeatureI feature;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   */
  public DisplaySequenceAction(String name, GuiCurationState curationState) {
    super(name);
    this.curationState = curationState;
  }

  /**
   * Brings up the display sequence window
   * @see apollo.gui.detailviewers.seqexport.SeqExport
   */
  public void actionPerformed(ActionEvent arg0) {
    if (feature == null) {
      throw new IllegalStateException("feature:" + feature);
    }
    new SeqExport(feature, curationState.getController());
  }

  /**
   * Sets what feature will have its sequence displayed
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

}
