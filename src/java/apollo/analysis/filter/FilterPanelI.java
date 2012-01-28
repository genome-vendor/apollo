/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.analysis.filter;

import java.util.Properties;

// I have a suspicion that this is completely unnecessary
// because there is already some interface that already
// does this. What is it?
public interface FilterPanelI {
  public void setProperties (Properties prop);
  public Properties getProperties();
  public void setInputs (AnalysisInput in);
}
