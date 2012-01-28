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

public class Sim4FilterPanel extends FilterPanel {
    //
    private JPanel filterConfigurationPanel;
    private JButton showFilterConfiguration;
    private JTextField min_score = new JTextField ("90");
    private JTextField min_length = new JTextField ("80");
    private JComboBox length_units = new JComboBox();
    private JToggleButton use_length = new JCheckBox ("Remove HITs where length of aligned sequence is below threshold", false);
    private JTextField max_coverage = new JTextField ("10");
    private JCheckBox limit_coverage = new JCheckBox ("Remove redundant HITs to limit depth of coverage", false);
    private JCheckBox use_score = new JCheckBox ("Remove alignments with a % identity below threshold", false);
    private JCheckBox join_reads = new JCheckBox ("Join 5' and 3' alignments (indicated by a common prefix)", false);
    private JCheckBox revcomp3 = new JCheckBox ("Reverse complement 3' reads (indicated by suffix)", false);
    private JTextField suffix = new JTextField ("3prime");
    private JCheckBox remove_polyA = new JCheckBox ("Remove alignments of polyA tails", false);

    private JCheckBox limit_exons = new JCheckBox ("Limit number of alignments (useful for STS and other short contiguous markers)", false);
    private JTextField exon_max = new JTextField ("");
    private JCheckBox limit_gaps = new JCheckBox ("Delete matches with alignment gaps (in the mRNA) > this percentage of length ", false);
    private JTextField gap_max = new JTextField ("50");
    
    public Sim4FilterPanel (Color bg_color) {
      this.setBackground (bg_color);
      buildGUI();
    }

  protected void buildGUI() {
    setLayout(new GridBagLayout());
    int row = 0;
    int textBoxHeight = 20;

    min_score.setPreferredSize(new Dimension(80,textBoxHeight));
    use_score.setBackground (getBackground());
    add(use_score, GuiUtil.makeConstraintAt(0,row,1));
    add(min_score, GuiUtil.makeConstraintAt(1,row,1));
    add(GuiUtil.makeJLabelWithFont(" %"), GuiUtil.makeConstraintAt(2,row++,1));

    min_length.setPreferredSize(new Dimension(80,textBoxHeight));
    length_units.setEditable(false);
    length_units.insertItemAt("percentage", 0);
    length_units.insertItemAt("fixed minimum", 1);
    length_units.setSelectedItem("percentage");
    use_length.setBackground (getBackground());
    add(use_length, GuiUtil.makeConstraintAt(0,row,1));
    add(min_length, GuiUtil.makeConstraintAt(1,row,1));
    add(length_units, GuiUtil.makeConstraintAt(2,row++,1));

    max_coverage.setPreferredSize(new Dimension(80,textBoxHeight));
    limit_coverage.setBackground (getBackground());
    add(limit_coverage, GuiUtil.makeConstraintAt(0,row,1));
    add(max_coverage, GuiUtil.makeConstraintAt(1,row++,1));
 
    join_reads.setBackground (getBackground());
    add(join_reads, GuiUtil.makeConstraintAt(0,row++,1));

    suffix.setPreferredSize(new Dimension(80, textBoxHeight));
    revcomp3.setBackground (getBackground());
    add(revcomp3, GuiUtil.makeConstraintAt(0,row,1));
    add(suffix, GuiUtil.makeConstraintAt(1,row++,1));

    remove_polyA.setBackground (getBackground());
    add(remove_polyA, GuiUtil.makeConstraintAt(0,row++,1));

    exon_max.setPreferredSize(new Dimension(80, textBoxHeight));
    limit_exons.setBackground (getBackground());
    add(limit_exons, GuiUtil.makeConstraintAt(0,row,1));
    add(exon_max, GuiUtil.makeConstraintAt(1,row++,1));

    gap_max.setPreferredSize(new Dimension(80, textBoxHeight));
    limit_gaps.setBackground (getBackground());
    add(limit_gaps, GuiUtil.makeConstraintAt(0,row,1));
    add(gap_max, GuiUtil.makeConstraintAt(1,row++,1));
  }

  /**
   * add on the hit-information to the query-information already in there.
   **/
  public Properties getProperties() {
    //Location, selected feature types copied in.
    Properties stateInfo = new Properties();
    
    stateInfo.setProperty("min_score", min_score.getText());
    stateInfo.setProperty("use_score", use_score.isSelected() ? "true" : "false");
    stateInfo.setProperty("min_length", min_length.getText());
    stateInfo.setProperty("use_length", use_length.isSelected() ? "true" : "false");
    stateInfo.setProperty("length_units", (String) length_units.getSelectedItem());
    stateInfo.setProperty("max_coverage", max_coverage.getText());
    stateInfo.setProperty("limit_coverage", limit_coverage.isSelected() ? "true" : "false");

    stateInfo.setProperty("join_reads", join_reads.isSelected() ? "true" : "false");

    stateInfo.setProperty("suffix", suffix.getText());
    stateInfo.setProperty("revcomp3", revcomp3.isSelected() ? "true" : "false");
    stateInfo.setProperty("remove_polyA", remove_polyA.isSelected() ? "true" : "false");

    stateInfo.setProperty("exon_max", exon_max.getText());
    stateInfo.setProperty("limit_exons", limit_exons.isSelected() ? "true" : "false");
    stateInfo.setProperty("gap_max", gap_max.getText());
    stateInfo.setProperty("limit_gaps", limit_gaps.isSelected() ? "true" : "false");
    return stateInfo;
  }


  /**
   * <p> This contains the properties stored in the adapter history,
   * which are read when the adapter is created. They will populate
   * the history-comboboxes etc. </p>
  **/
  public void setProperties(Properties properties) {
    String propValue;
    propValue = properties.getProperty("min_score");
    if (propValue != null)
      min_score.setText(propValue);
    propValue = properties.getProperty("use_score");
    if (propValue != null && propValue.equals("true"))
      use_score.setSelected(true);
    propValue = properties.getProperty("min_length");
    if (propValue != null)
      min_length.setText(propValue);
    propValue = properties.getProperty("use_length");
    if (propValue != null && propValue.equals("true"))
      use_length.setSelected(true);
    propValue = properties.getProperty("length_units");
    propValue = properties.getProperty("max_coverage");
    if (propValue != null)
      max_coverage.setText(propValue);
    propValue = properties.getProperty("limit_coverage");
    if (propValue != null && propValue.equals("true"))
      limit_coverage.setSelected(true);
    propValue = properties.getProperty("join_reads");
    if (propValue != null && propValue.equals("true"))
      join_reads.setSelected(true);
    propValue = properties.getProperty("suffix");
    if (propValue != null)
      suffix.setText(propValue);
    propValue = properties.getProperty("revcomp3");
    if (propValue != null && propValue.equals("true"))
      revcomp3.setSelected(true);
    propValue = properties.getProperty("remove_polyA");
    if (propValue != null && propValue.equals("true"))
      remove_polyA.setSelected(true);
    propValue = properties.getProperty("exon_max");
    if (propValue != null)
      exon_max.setText(propValue);
    propValue = properties.getProperty("limit_exons");
    if (propValue != null && propValue.equals("true"))
      limit_exons.setSelected(true);
    propValue = properties.getProperty("gap_max");
    if (propValue != null)
      gap_max.setText(propValue);
    propValue = properties.getProperty("limit_gaps");
    if (propValue != null && propValue.equals("true"))
      limit_gaps.setSelected(true);

  }//end setProperties

    /**
     * add on the hit-information to the query-information already in there.
     **/
  public void setInputs(AnalysisInput inputs) {
    if (use_score.isSelected())
      inputs.setMinScore(min_score.getText());
    else
      inputs.setMinScore(AnalysisInput.NO_LIMIT);

    if (use_length.isSelected()) {
      inputs.setMinLength(min_length.getText());
      String units = (String) length_units.getSelectedItem();
      inputs.setLengthUnits(units.equals("percentage"));
    }
    else
      inputs.setMinLength(AnalysisInput.NO_LIMIT);

    if (limit_coverage.isSelected())
      inputs.setMaxCover(max_coverage.getText());
    else
      inputs.setMaxCover(AnalysisInput.NO_LIMIT);

    inputs.trimPolyA (remove_polyA.isSelected());
    
    inputs.joinESTends (join_reads.isSelected());

    if (revcomp3.isSelected())
      inputs.revComp3Prime(suffix.getText());

    if (limit_exons.isSelected())
      inputs.setMaxExons(exon_max.getText());
    else
      inputs.setMaxExons(AnalysisInput.NO_LIMIT);

    if (limit_gaps.isSelected())
      inputs.setMaxAlignGap(gap_max.getText());
    else
      inputs.setMaxAlignGap(AnalysisInput.NO_LIMIT);
  }

}

