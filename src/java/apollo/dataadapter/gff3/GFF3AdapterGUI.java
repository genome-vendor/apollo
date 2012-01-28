package apollo.dataadapter.gff3;

import apollo.dataadapter.*;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

import org.apache.log4j.*;

import org.bdgp.io.*;

import apollo.seq.io.*;
import apollo.config.Config;
import apollo.gui.GenericFileAdapterGUI;
import apollo.datamodel.*;
import java.awt.event.*;

import apollo.util.GuiUtil;

public class GFF3AdapterGUI extends GenericFileAdapterGUI implements ApolloDataAdapterGUI{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GFF3AdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  JButton   seqButton;
  JComboBox seqFileList;
  Vector    seqFilePaths;
  private JCheckBox fastaFromGff;

  public GFF3AdapterGUI(IOOperation op) {
    super(op);
  }

  public void buildGUI() {
    // This puts a textfield and browse button for the gff file
    super.buildGUI();

    // Now we want a textfield and browse button fot the sequence file

    seqFileList    = new JComboBox();
    seqButton      = new JButton("Browse...");

    seqFileList.setEditable(true);

    seqFileList    .setFont(getFont());
    seqButton.setFont(getFont());

    seqFileList    .setAlignmentY((float) .5);
    seqButton.setAlignmentY((float) .5);

    seqFileList.setPreferredSize(new Dimension(getPathListWidth(),10));

    JPanel panel3 = new JPanel();
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));

    panel3.add(seqFileList);
    panel3.add(seqButton);

    fastaFromGff = new JCheckBox("Embedded FASTA in GFF", false);
    fastaFromGff.setFont(getFont());
    fastaFromGff.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
            checkFastaFromGff();
        }
    });
    JPanel panel4 = new JPanel();
    panel4.add(fastaFromGff);
    panel4.setBorder(BorderFactory.createEtchedBorder());

    getPanel().removeAll();
    getPanel().setLayout(new GridBagLayout());

    getPanel().add(new JLabel("GFF file"), GuiUtil.makeConstraintAt(0, 0, 1));
    getPanel().add(panel2, GuiUtil.makeConstraintAt(0, 1, 1));
    getPanel().add(new JLabel("Sequence file"),
        GuiUtil.makeConstraintAt(0, 2, 1));
    getPanel().add(panel3, GuiUtil.makeConstraintAt(0, 3, 1));
    GridBagConstraints c = GuiUtil.makeConstraintAt(0, 4, 2);
    c.anchor = GridBagConstraints.CENTER;
    getPanel().add(fastaFromGff, c);

    seqButton.addActionListener( new ActionListener() {
                                   public void actionPerformed(ActionEvent e) {
                                     browseFiles(seqFileList);
                                   }
                                 });

  }

  public void setProperties(Properties in) {
    props = in;
    if (props == null)
      return;

    filePaths = new Vector();
    seqFilePaths = new Vector();

    try {
      String historyItems = props.getProperty("historyItems");
      if (historyItems == null || Integer.parseInt(historyItems) == 0) {
        logger.warn("No history for GFF files");
        return;
      }
      int items = Integer.parseInt(historyItems);
      for(int i=0; i < items; i++) {
        filePaths.addElement(props.getProperty("historyItem"+i));
      }

      String seqFileItems = props.getProperty("seqFileItems");
      if (seqFileItems != null && Integer.parseInt(seqFileItems) > 0) {
        items = Integer.parseInt(seqFileItems);
        for(int i=0; i < items; i++) {
          seqFilePaths.addElement(props.getProperty("seqFileItem"+i));
        }
      }
    } catch (NumberFormatException e) {
      logger.error("Exception parsing history file: " + e, e);
    }
    pathList.setModel(new DefaultComboBoxModel(filePaths));
    seqFileList.setModel(new DefaultComboBoxModel(seqFilePaths));

    if (props.containsKey("fastaFromGff")) {
        if (props.get("fastaFromGff").equals("true")) {
            fastaFromGff.setSelected(true);
        }
        else {
            fastaFromGff.setSelected(false);
        }
    }
    checkFastaFromGff();
  }

  /** Returns Properties with history items of all the files in
   * filePaths Vector, puts currently selected path at front of list
   */
  public Properties getProperties() {
    String selectedPath = getSelectedPath();
    filePaths.removeElement(selectedPath);
    filePaths.insertElementAt(selectedPath, 0);

    Properties out = new Properties();
    if (filePaths.size() > MAX_HISTORY_LENGTH)
      out.put("historyItems", MAX_HISTORY_LENGTH+"");
    else
      out.put("historyItems", filePaths.size()+"");
    for(int i=0; i < filePaths.size() && i < MAX_HISTORY_LENGTH; i++) {
      out.put("historyItem"+i, (String) filePaths.elementAt(i));
    }
  
    selectedPath = getSelectedSeqPath();
    if(selectedPath.trim().length()<=0){
      selectedPath = null;
    }
    if(seqFilePaths != null && selectedPath != null){
      seqFilePaths.removeElement(selectedPath);
      seqFilePaths.insertElementAt(selectedPath, 0);

      if (seqFilePaths.size() > MAX_HISTORY_LENGTH)
        out.put("seqFileItems", MAX_HISTORY_LENGTH+"");
      else
        out.put("seqFileItems", seqFilePaths.size()+"");
      for(int i=0; i < seqFilePaths.size() && i < MAX_HISTORY_LENGTH; i++) {
        out.put("seqFileItem"+i, (String) seqFilePaths.elementAt(i));
      }
    }

    out.put("fastaFromGff", Boolean.toString(fastaFromGff.isSelected()));
    return out;
  }

  public String getSelectedSeqPath() {
    String selectedPath = (String) seqFileList.getSelectedItem();

    if (selectedPath == null ||
        !selectedPath.equals(seqFileList.getEditor().getItem())) {
      selectedPath = (String) seqFileList.getEditor().getItem();
    }
    return selectedPath;
  }

  public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException {
    GFF3Adapter adapter = (GFF3Adapter)driver;

    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      adapter.setGffFilename(getSelectedPath());
      adapter.setSeqFilename(getSelectedSeqPath());
      adapter.setFastaFromGff(fastaFromGff.isSelected());

      CurationSet set = adapter.getCurationSet();
      return set;

    }
    else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      adapter.setGffFilename(getSelectedPath());
      adapter.setSeqFilename(getSelectedSeqPath());
      adapter.setFastaFromGff(fastaFromGff.isSelected());
      adapter.write(values);
      return null;
    }
    else if (op.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
      adapter.setGffFilename(getSelectedPath());
      adapter.setSeqFilename(getSelectedSeqPath());
      adapter.setFastaFromGff(fastaFromGff.isSelected());
      adapter.addToCurationSet();
      return values;
    }
    else {
      throw new org.bdgp.io.DataAdapterException("This adapter only works for ApolloDataAdapterI.OP_READ_DATA or ApolloDataAdapterI.OP_WRITE_DATA");
    }
  }

  public Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException {
    StateInformation stateInformation = new StateInformation();
    stateInformation.setProperty(StateInformation.DATA_FILE_NAME, getSelectedPath());
    stateInformation.setProperty(StateInformation.SEQUENCE_FILE_NAME, getSelectedSeqPath());
    return stateInformation;
  }

  private void checkFastaFromGff()
  {
      if (fastaFromGff.isSelected()) {
          seqButton.setEnabled(false);
          seqFileList.setEnabled(false);
      }
      else {
          seqButton.setEnabled(true);
          seqFileList.setEnabled(true);
      }
  }

}
