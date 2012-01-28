/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.analysis.filter;

import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Properties;
import apollo.dataadapter.analysis.AnalysisPanel;

public abstract class FilterPanel extends JPanel implements FilterPanelI {
  AnalysisPanel parent;

  // I have a suspicion that this is completely unnecessary
  // because there is already some interface that already
  // does this. What is it?
  public void setProperties (Properties prop) {}
  public Properties getProperties() { return null; }
  public void setInputs (AnalysisInput in) {}

  public void setAnalysisPanel(AnalysisPanel ap) {
    parent = ap;
  }
  public AnalysisPanel getAnalysisPanel() {
    return parent;
  }
  
}
