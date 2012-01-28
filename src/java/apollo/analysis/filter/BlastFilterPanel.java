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

public class BlastFilterPanel extends FilterPanel {
  protected JPanel filterConfigurationPanel;
  protected JButton showFilterConfiguration;
  protected JTextField min_expect = new JTextField ("1.0e-4");
  protected JCheckBox use_expect = new JCheckBox ("Remove hits with an expect above threshold", false);
  protected JTextField min_score = new JTextField ("50");
  protected JCheckBox use_score = new JCheckBox ("Remove hits with a score below threshold", false);
  protected JTextField min_identity = new JTextField ("50");
  protected JCheckBox use_identity = new JCheckBox ("Remove HSPs with a percent identity below threshold", false);
  protected JTextField min_length = new JTextField ("50");
  protected JComboBox length_units = new JComboBox();
  protected JToggleButton use_length = new JCheckBox ("Remove hits where length of aligned sequence is below threshold", false);
  protected JTextField min_coincidence = new JTextField ("80");
  protected JCheckBox use_coincidence = new JCheckBox ("Remove overlapping secondary HSPs within hits", false);
  protected JTextField max_coverage = new JTextField ("10");
  protected JCheckBox limit_coverage = new JCheckBox ("Remove redundant hits to limit depth of coverage", false);
  protected JTextField word_size = new JTextField ("2");
  protected JTextField compress_threshold = new JTextField ("15");
  protected JCheckBox remove_lowinfo = new JCheckBox ("Remove HSPs that are simple repeats", false);

  protected JCheckBox remove_twilight = new JCheckBox ("Remove HSPs that are distant and weakly scored", false);
  protected JCheckBox remove_shadows = new JCheckBox("Remove hits that are shadowing hits on opposite strand", false);
  protected JCheckBox split_frames = new JCheckBox("Split hits according to frame of HSPs", false);
  protected JCheckBox split_dups = new JCheckBox("Split hits that are tandemly duplicated on subject into separate hits", false);
  protected JCheckBox split_HSPs = new JCheckBox("Split hits into separate hits for each HSPs (for genomic to genomic)", false);
  /** Before, query==genomic was the default.  Now query != genomic is the default. */
  //  protected JCheckBox queryNotGenomic = new JCheckBox("BLAST results are for an unknown query sequence against the chromosomes at NCBI");
  protected JCheckBox queryIsGenomic = new JCheckBox("I used the current genomic sequence (or a portion thereof) as my BLAST query");

    
  public BlastFilterPanel (Color bg_color) {
    this.setBackground(bg_color);
    buildGUI();
  }

  protected void buildGUI() {
    setLayout(new GridBagLayout());
    int row = 0;
    int textBoxHeight = 20;

    queryIsGenomic.setBackground (getBackground());
    add(queryIsGenomic, GuiUtil.makeConstraintAt(0,row++,1));
    queryIsGenomic.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // Allow user to enter an offset only if their query is genomic
          // (and data is being layered)
          if (getAnalysisPanel().offsetField != null)
            getAnalysisPanel().offsetField.setEditable(queryIsGenomic.isSelected());
        }
      }
                                     );

    split_frames.setBackground (getBackground());
    add(split_frames, GuiUtil.makeConstraintAt(0,row++,1));

    min_coincidence.setPreferredSize(new Dimension(80,textBoxHeight));
    add(use_coincidence, GuiUtil.makeConstraintAt(0,row,1));
    use_coincidence.setBackground (getBackground());
    add(min_coincidence, GuiUtil.makeConstraintAt(1,row++,1));

    split_HSPs.setBackground (getBackground());
    add(split_HSPs, GuiUtil.makeConstraintAt(0,row++,1));

    split_dups.setBackground (getBackground());
    add(split_dups, GuiUtil.makeConstraintAt(0,row++,1));

    min_expect.setPreferredSize(new Dimension(80,textBoxHeight));
    add(use_expect, GuiUtil.makeConstraintAt(0,row,1));
    use_expect.setBackground (getBackground());
    add(min_expect, GuiUtil.makeConstraintAt(1,row,1));
    add(GuiUtil.makeJLabelWithFont(" maximum expect value"), 
	GuiUtil.makeConstraintAt(2,row++,1));

    min_score.setPreferredSize(new Dimension(80,textBoxHeight));
    add(use_score, GuiUtil.makeConstraintAt(0,row,1));
    use_score.setBackground (getBackground());
    add(min_score, GuiUtil.makeConstraintAt(1,row,1));
    add(GuiUtil.makeJLabelWithFont(" "),
        GuiUtil.makeConstraintAt(2,row++,1));

    min_identity.setPreferredSize(new Dimension(80,textBoxHeight));
    add(use_identity, GuiUtil.makeConstraintAt(0,row,1));
    use_identity.setBackground (getBackground());
    add(min_identity, GuiUtil.makeConstraintAt(1,row,1));
    add(GuiUtil.makeJLabelWithFont(" %"), 
	GuiUtil.makeConstraintAt(2,row++,1));

    word_size.setPreferredSize(new Dimension(40,textBoxHeight));
    compress_threshold.setPreferredSize(new Dimension(40,textBoxHeight));
    add(remove_lowinfo, GuiUtil.makeConstraintAt(0,row,1));
    remove_lowinfo.setBackground (getBackground());
    add(word_size, GuiUtil.makeConstraintAt(1,row,1));
    add(GuiUtil.makeJLabelWithFont(" word size"), 
	GuiUtil.makeConstraintAt(2,row,1));
    add(compress_threshold, GuiUtil.makeConstraintAt(3,row,1));
    add(GuiUtil.makeJLabelWithFont(" % compression"),
	GuiUtil.makeConstraintAt(4,row++,1));

    add(remove_twilight, GuiUtil.makeConstraintAt(0,row++,1));
    remove_twilight.setBackground (getBackground());

    min_length.setPreferredSize(new Dimension(80,textBoxHeight));
    length_units.setEditable(false);
    length_units.insertItemAt("percentage", 0);
    length_units.insertItemAt("fixed minimum", 1);
    length_units.setSelectedItem("percentage");
    add(use_length, GuiUtil.makeConstraintAt(0,row,1));
    use_length.setBackground (getBackground());
    add(min_length, GuiUtil.makeConstraintAt(1,row,1));
    add(length_units, GuiUtil.makeConstraintAt(2,row++,1));

    add(remove_shadows, GuiUtil.makeConstraintAt(0,row++,1));
    remove_shadows.setBackground (getBackground());
    
    max_coverage.setPreferredSize(new Dimension(80,textBoxHeight));
    add(limit_coverage, GuiUtil.makeConstraintAt(0,row,1));
    limit_coverage.setBackground (getBackground());
    add(max_coverage, GuiUtil.makeConstraintAt(1,row++,1));
 
  }

  /**
   * add on the hit-information to the query-information already in there.
   **/
  public Properties getProperties() {
    //
    //Location, selected feature types copied in.
    Properties stateInfo = new Properties();
    
    stateInfo.setProperty("min_expect", min_expect.getText());
    stateInfo.setProperty("use_expect", use_expect.isSelected() ? "true" : "false");
    stateInfo.setProperty("min_score", min_score.getText());
    stateInfo.setProperty("use_score", use_score.isSelected() ? "true" : "false");
    stateInfo.setProperty("min_identity", min_identity.getText());
    stateInfo.setProperty("use_identity", use_identity.isSelected() ? "true" : "false");
    stateInfo.setProperty("min_length", min_length.getText());
    stateInfo.setProperty("use_length", use_length.isSelected() ? "true" : "false");
    stateInfo.setProperty("length_units", (String) length_units.getSelectedItem());
    stateInfo.setProperty("min_coincidence", min_coincidence.getText());
    stateInfo.setProperty("use_coincidence", use_coincidence.isSelected() ? "true" : "false");
    stateInfo.setProperty("max_coverage", max_coverage.getText());
    stateInfo.setProperty("limit_coverage", limit_coverage.isSelected() ? "true" : "false");
    stateInfo.setProperty("word_size", word_size.getText());
    stateInfo.setProperty("compress_threshold", compress_threshold.getText());
    stateInfo.setProperty("remove_lowinfo", remove_lowinfo.isSelected() ? "true" : "false");
    stateInfo.setProperty("remove_twilight", remove_twilight.isSelected() ? "true" : "false");
    stateInfo.setProperty("remove_shadows", remove_shadows.isSelected() ? "true" : "false");
    stateInfo.setProperty("split_frames", split_frames.isSelected() ? "true" : "false");
    stateInfo.setProperty("split_dups", split_dups.isSelected() ? "true" : "false");
    stateInfo.setProperty("split_HSPs", split_HSPs.isSelected() ? "true" : "false");
    stateInfo.setProperty("queryIsGenomic", queryIsGenomic.isSelected() ? "true" : "false");

    return stateInfo;
  }

  /**
   * <p> This contains the properties stored in the adapter history,
   * which are read when the adapter is created. They will populate
   * the history-comboboxes etc. </p>
   *
   **/
  public void setProperties(Properties properties) {
    String propValue;
    propValue = properties.getProperty("queryIsGenomic");
    if (propValue != null && propValue.equals("true") &&
        getAnalysisPanel().offsetField != null) {
      queryIsGenomic.setSelected(true);
      getAnalysisPanel().offsetField.setEditable(true);
    }
    else if (getAnalysisPanel().offsetField != null) {
      getAnalysisPanel().offsetField.setEditable(false);
    }
    propValue = properties.getProperty("min_expect");
    if (propValue != null)
      min_expect.setText(propValue);
    propValue = properties.getProperty("use_expect");
    if (propValue != null && propValue.equals("true"))
      use_expect.setSelected(true);
    propValue = properties.getProperty("min_score");
    if (propValue != null)
      min_score.setText(propValue);
    propValue = properties.getProperty("use_score");
    if (propValue != null && propValue.equals("true"))
      use_score.setSelected(true);
    propValue = properties.getProperty("min_identity");
    if (propValue != null)
      min_identity.setText(propValue);
    propValue = properties.getProperty("use_identity");
    if (propValue != null && propValue.equals("true"))
      use_identity.setSelected(true);
    propValue = properties.getProperty("min_length");
    if (propValue != null)
      min_length.setText(propValue);
    propValue = properties.getProperty("use_length");
    if (propValue != null && propValue.equals("true"))
      use_length.setSelected(true);
    propValue = properties.getProperty("length_units");
    if (propValue != null)
      length_units.setSelectedItem(propValue);
    propValue = properties.getProperty("min_coincidence");
    if (propValue != null)
      min_coincidence.setText(propValue);
    propValue = properties.getProperty("use_coincidence");
    if (propValue != null && propValue.equals("true"))
      use_coincidence.setSelected(true);
    propValue = properties.getProperty("max_coverage");
    if (propValue != null)
      max_coverage.setText(propValue);
    propValue = properties.getProperty("limit_coverage");
    if (propValue != null && propValue.equals("true"))
      limit_coverage.setSelected(true);
    propValue = properties.getProperty("word_size");
    if (propValue != null)
      word_size.setText(propValue);
    propValue = properties.getProperty("compress_threshold");
    if (propValue != null)
      compress_threshold.setText(propValue);
    propValue = properties.getProperty("remove_lowinfo");
    if (propValue != null && propValue.equals("true"))
      remove_lowinfo.setSelected(true);
    propValue = properties.getProperty("remove_twilight");
    if (propValue != null && propValue.equals("true"))
      remove_twilight.setSelected(true);
    propValue = properties.getProperty("remove_shadows");
    if (propValue != null && propValue.equals("true"))
      remove_shadows.setSelected(true);
    propValue = properties.getProperty("split_frames");
    if (propValue != null && propValue.equals("true"))
      split_frames.setSelected(true);
    propValue = properties.getProperty("split_dups");
    if (propValue != null && propValue.equals("true"))
      split_dups.setSelected(true);
    propValue = properties.getProperty("split_HSPs");
    if (propValue != null && propValue.equals("true"))
      split_HSPs.setSelected(true);
  }//end setProperties

  /**
   * add on the hit-information to the query-information already in there.
   **/
  public void setInputs(AnalysisInput inputs) {
    if (use_expect.isSelected()) 
      inputs.setMaxExpect(min_expect.getText());
    else
      inputs.setMaxExpect(AnalysisInput.NO_LIMIT);

    if (use_score.isSelected())
      inputs.setMinScore(min_score.getText());
    else
      inputs.setMinScore(AnalysisInput.NO_LIMIT);

    if (use_identity.isSelected())
      inputs.setMinIdentity(min_identity.getText());
    else
      inputs.setMinIdentity(AnalysisInput.NO_LIMIT);

    if (use_length.isSelected()) {
      inputs.setMinLength(min_length.getText());
      String units = (String) length_units.getSelectedItem();
      inputs.setLengthUnits(units.equals("percentage"));
    }
    else
      inputs.setMinLength(AnalysisInput.NO_LIMIT);

    if (use_coincidence.isSelected())
      inputs.setCoincidence(min_coincidence.getText());
    else
      inputs.setCoincidence(AnalysisInput.NO_LIMIT);

    if (limit_coverage.isSelected())
      inputs.setMaxCover(max_coverage.getText());
    else
      inputs.setMaxCover(AnalysisInput.NO_LIMIT);

    if (remove_lowinfo.isSelected()) {
      inputs.setWordSize(word_size.getText());
      inputs.setMaxRatio(compress_threshold.getText());
    }
    else {
      inputs.setWordSize(AnalysisInput.NO_LIMIT);
    }
      
    inputs.setRemoveTwilights(remove_twilight.isSelected());

    inputs.filterShadows(remove_shadows.isSelected());

    inputs.setSplitFrames (split_frames.isSelected());

    inputs.setSplitTandems (split_dups.isSelected());

    inputs.autonomousHSPs(split_HSPs.isSelected());

    inputs.setQueryIsGenomic(queryIsGenomic.isSelected());
  }
}
