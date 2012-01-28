package apollo.analysis.filter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.File;

import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.datamodel.CurationSet;
import apollo.datamodel.GenomicRange;
import apollo.config.Config;

public class NoFilterPanel extends FilterPanel {
    //
    private JPanel filterConfigurationPanel;
    private JButton showFilterConfiguration;

    public NoFilterPanel (Color bg_color) {
      this.setBackground (bg_color);
      buildGUI();
    }

  protected void buildGUI() {
    setLayout(new GridBagLayout());
    int row = 0;

  }

    /**
     * add on the hit-information to the query-information already in there.
     **/
  public Properties getProperties() {
    //Location, selected feature types copied in.
    Properties stateInfo = new Properties();
    return stateInfo;
  }


  /**
   * <p> This contains the properties stored in the adapter history,
   * which are read when the adapter is created. They will populate
   * the history-comboboxes etc. </p>
  **/
  public void setProperties(Properties properties) {
    String propValue;
  }//end setProperties

    /**
     * add on the hit-information to the query-information already in there.
     **/
  public void setInputs(AnalysisInput inputs) {
    inputs.setMinScore(AnalysisInput.NO_LIMIT);
    inputs.setMinLength(AnalysisInput.NO_LIMIT);
    inputs.setMaxCover(AnalysisInput.NO_LIMIT);
    inputs.trimPolyA (false);
    inputs.joinESTends (false);
    inputs.setMaxExons(AnalysisInput.NO_LIMIT);
    inputs.setMaxAlignGap(AnalysisInput.NO_LIMIT);
  }

}


