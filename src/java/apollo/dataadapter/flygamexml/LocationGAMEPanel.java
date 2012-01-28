package apollo.dataadapter.flygamexml;

import java.util.Vector;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.gui.ChromosomeField;
import apollo.config.Style;
import apollo.util.GuiUtil;

public class LocationGAMEPanel extends GAMEPanel {

  private final static String label = "Location";

  private ChromosomeField chrField;
  private JTextField startField;
  private JTextField endField;

  public LocationGAMEPanel(Color bkgnd) {
    // calls buildGUI
    super("Location",label,DataInputType.BASEPAIR_RANGE,bkgnd);
  }

  protected void buildGUI() {
    JLabel chromLabel = GuiUtil.makeJLabelWithFont(" Chromosome ");
    Vector chroms = null;
    if (getAdapterStyle()!=null) chroms = getAdapterStyle().getChromosomes();
    chrField = new ChromosomeField(chroms);

    JLabel startLabel = GuiUtil.makeJLabelWithFont(" Start ");
    startField = GuiUtil.makeNumericTextField();

    JLabel endLabel = GuiUtil.makeJLabelWithFont(" End ");
    endField =  GuiUtil.makeNumericTextField();

    JPanel chromStartEndPanel = new JPanel(new GridBagLayout());
    chromStartEndPanel.setBackground (getBackground());
    ApolloGridBagConstraints cons = newConstraints();
    cons.ipadx = 10;
    chromStartEndPanel.add(chromLabel,cons);
    chromStartEndPanel.add(chrField.getComponent(),cons.nextColumn());
    chromStartEndPanel.add(startLabel,cons.nextColumn());
    chromStartEndPanel.add(startField,cons.nextColumn());
    chromStartEndPanel.add(endLabel,cons.nextColumn());    
    chromStartEndPanel.add(endField,cons.nextColumn());

    // history down below??

    JPanel locPanel = new JPanel(new GridBagLayout());
    locPanel.setBackground(getBackground());
    ApolloGridBagConstraints gbc = newConstraints();
    gbc.ipady = 10;
    if (showDatabaseList())
      locPanel.add(super.getDatabasePanel(),gbc);
    locPanel.add(chromStartEndPanel,nextRow(gbc));

    getPanel().add(locPanel);
  }

  public String getCurrentInput() {
    // this is the loc format used by navigation panel i think
    return getChrom()+":"+getStart()+"-"+getEnd();
  }

  private String getChrom() { return chrField.getChromosome(); }
  private String getStart() { return startField.getText(); }
  private String getEnd() { return endField.getText(); }

  public DataInput getDataInput() {
    return new DataInput(getChrom(),getStart(),getEnd());
  }

  protected void setEditorsHistory(Vector history) {
    // history stuff here
  }

}
