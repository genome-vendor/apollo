package apollo.dataadapter;

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

public class GFFAdapterGUI extends GenericFileAdapterGUI implements ApolloDataAdapterGUI{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GFFAdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  JButton   seqButton;
  JComboBox seqFileList;
  Vector    seqFilePaths;

  public GFFAdapterGUI(IOOperation op) {
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

    getPanel().removeAll();
    getPanel().setLayout(new GridLayout(4,1));
    getPanel().add(new JLabel("GFF file"));
    getPanel().add(panel2);
    getPanel().add(new JLabel("Sequence file"));
    getPanel().add(panel3);


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
    BaseGFFAdapter bga = (BaseGFFAdapter)driver;


    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      bga.setFilename(getSelectedPath());
      bga.setSequenceFilename(getSelectedSeqPath());
      bga.setRegion(getSelectedPath());

      CurationSet set = bga.getCurationSet();
      //apollo.dataadapter.debug.DisplayTool.showFeatureSet(set.getResults() );
      return set;

    } else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      //      CurationSet set = (CurationSet)values;
      ApolloDataI apolloData = (ApolloDataI)values;
      CurationSet curSet=null;
      if (apolloData.isCurationSet()) {
        curSet = apolloData.getCurationSet();
      }
      // eventually phase this case out
      else if (apolloData.isCompositeDataHolder()) {
        CompositeDataHolder cdh = apolloData.getCompositeDataHolder();
        // assume its the 1st species - presumptious but what else can we do
        curSet = cdh.getCurationSet(0);
      }
      else {
        logger.error("Don't know how to save");
        return null;
      }
      
      bga.setFilename(getSelectedPath());
      bga.setSequenceFilename(getSelectedSeqPath());
      bga.setRegion(getSelectedPath());
      bga.commitChanges(curSet);
      return null;
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

}
