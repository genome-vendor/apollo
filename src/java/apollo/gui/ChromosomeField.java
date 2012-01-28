package apollo.gui;

import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import apollo.config.Config;

/** Chromsome field is either a combo box or text field depending on if config
      has a chromosome vector */
public class ChromosomeField {
  private JComponent jComponent;
  private boolean isJCombo; // ?
  private JComboBox jComboBox;
  private JTextField jTextField;
  private boolean haveChromList=false;

  public ChromosomeField() {
    initGui(null);
  }
  public ChromosomeField(Vector chroms) {
    initGui(chroms);
  }

  private void initGui(Vector chroms) {
    if (chroms!=null || Config.hasChromosomes()) {
      isJCombo = true;
      if (chroms==null)
        chroms = Config.getChromosomes();
      jComboBox = new JComboBox(chroms);
      jComponent = jComboBox;
    } else {
      isJCombo = false;
      jTextField = new JTextField();
      jComponent = jTextField;
    }
    jComponent.setPreferredSize(new Dimension(70,25));
    haveChromList = Config.hasChromosomes();
  }

  public void addActionListener(ActionListener al) {
    if (!isJCombo)
      jTextField.addActionListener(al);
    // jCombo doesnt get action listener - makes no sense
  }

  public JComponent getComponent() {
    return jComponent;
  }

  public void setChromosome(String chrom) {
    // check if style change has changed chrom state - is this funny?
    if (styleChanged())
      initGui(null); // null?
    if (isJCombo)
      jComboBox.setSelectedItem(chrom);
    else
      jTextField.setText(chrom);
  }

  public String getChromosome() {
    if (isJCombo)
      return (String)jComboBox.getSelectedItem();
    return jTextField.getText();
  }
  //
  public boolean styleChanged() {
    return haveChromList != Config.hasChromosomes();
  }
}
