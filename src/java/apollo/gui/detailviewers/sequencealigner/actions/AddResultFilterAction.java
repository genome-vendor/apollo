package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.filters.Filter;

public class AddResultFilterAction extends AbstractAction {
  
  private MultiSequenceAlignerPanel multiSequenceAlignerPanel;
  private Filter<SeqFeatureI> filter;
  
  public AddResultFilterAction(MultiSequenceAlignerPanel multiSequenceAlignerPanel) {
    this.multiSequenceAlignerPanel = multiSequenceAlignerPanel;
    this.filter = null;
  }

  public void actionPerformed(ActionEvent e) {
    if (filter != null) {
      multiSequenceAlignerPanel.getResultFilter().add(filter);
      multiSequenceAlignerPanel.reformatResultPanel();
      multiSequenceAlignerPanel.getResultPanel().repaint();
      multiSequenceAlignerPanel.getOverview().updateState();
      multiSequenceAlignerPanel.getOverview().repaint();
    }
  }
  
  public void setFilter(Filter<SeqFeatureI> filter) {
    this.filter = filter;
  }
  
  public Filter<SeqFeatureI> getFilter() {
    return this.filter;
  }

}
