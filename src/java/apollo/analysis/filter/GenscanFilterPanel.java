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
import apollo.util.GuiUtil;

public class GenscanFilterPanel extends FilterPanel {
    //
    private JCheckBox keep_polyA = new JCheckBox ("Keep polyadenylation site predictions", false);
    private JCheckBox keep_promoter = new JCheckBox ("Keep promoter site predictions", false);
    
    public GenscanFilterPanel (Color bg_color) {
      this.setBackground (bg_color);
      buildGUI();
    }

  protected void buildGUI() {
    setLayout(new GridBagLayout());
    int row = 0;

    keep_promoter.setBackground (getBackground());
    add(keep_promoter, GuiUtil.makeConstraintAt(0,row++,1));

    keep_polyA.setBackground (getBackground());
    add(keep_polyA, GuiUtil.makeConstraintAt(0,row,1));
  }

    /**
     * add on the hit-information to the query-information already in there.
     **/
  public Properties getProperties() {
    //
    //Location, selected feature types copied in.
    Properties stateInfo = new Properties();
    stateInfo.setProperty("keep_promoter", keep_promoter.isSelected() ? "true" : "false");
    stateInfo.setProperty("keep_polyA", keep_polyA.isSelected() ? "true" : "false");
    return stateInfo;
  }


  /**
   * <p> This contains the properties stored in the adapter history,
   * which are read when the adapter is created. They will populate
   * the history-comboboxes etc. </p>
  **/
  public void setProperties(Properties properties) {
    String propValue;
    propValue = properties.getProperty("keep_promoter");
    if (propValue != null && propValue.equals("true"))
      keep_promoter.setSelected(true);
    propValue = properties.getProperty("keep_polyA");
    if (propValue != null && propValue.equals("true"))
      keep_polyA.setSelected(true);
  }//end setProperties

    /**
     * add on the hit-information to the query-information already in there.
     **/
  public void setInputs(AnalysisInput inputs) {
    inputs.keepPolyA (keep_polyA.isSelected());
    inputs.keepPromoter (keep_promoter.isSelected());
  }

}


