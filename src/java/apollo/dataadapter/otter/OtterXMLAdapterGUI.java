package apollo.dataadapter.otter;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

//import org.apollo.datamodel.*;

import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;


import apollo.dataadapter.*;
import apollo.datamodel.CurationSet;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.config.Config;
import apollo.gui.ProxyDialog;

import apollo.gui.GenericFileAdapterGUI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;
import apollo.seq.io.FastaFile;
import apollo.dataadapter.*;

public class OtterXMLAdapterGUI extends GenericFileAdapterGUI {
  JButton   seqButton;
  JComboBox seqFileList;
  Vector    seqFilePaths;

  JButton   gffButton;
  JComboBox gffFileList;
  Vector    gffFilePaths;

  public OtterXMLAdapterGUI(IOOperation op) {
    super(op);
  }

  public void buildGUI() {
    // This puts a textfield and browse button for the gff file
    super.buildGUI();

    // Now we want a textfield and browse button fot the sequence file

    seqFileList    = new JComboBox();
    seqFilePaths   = new Vector();
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


    gffFileList    = new JComboBox();
    gffFilePaths   = new Vector();
    gffButton      = new JButton("Browse...");

    gffFileList.setEditable(true);

    gffFileList    .setFont(getFont());
    gffButton.setFont(getFont());

    gffFileList    .setAlignmentY((float) .5);
    gffButton.setAlignmentY((float) .5);

    gffFileList.setPreferredSize(new Dimension(getPathListWidth(),10));

    JPanel panel4 = new JPanel();
    panel4.setLayout(new BoxLayout(panel4, BoxLayout.X_AXIS));

    panel4.add(gffFileList);
    panel4.add(gffButton);

    getPanel().removeAll();
    getPanel().setLayout(new GridLayout(6,1));
    getPanel().add(new JLabel("Otter XML file"));
    getPanel().add(panel2);
    getPanel().add(new JLabel("Sequence file"));
    getPanel().add(panel3);
    getPanel().add(new JLabel("GFF feature file"));
    getPanel().add(panel4);


    seqButton.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          browseFiles(seqFileList);
        }
      });

    gffButton.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          browseFiles(gffFileList);
        }
      });

  }

  public String getSelectedSeqPath() {
    String selectedPath = (String) seqFileList.getSelectedItem();

    if (selectedPath == null ||
        !selectedPath.equals(seqFileList.getEditor().getItem())) {
      selectedPath = (String) seqFileList.getEditor().getItem();
    }

    return selectedPath;
  }
  public String getSelectedGFFPath() {
    String selectedPath = (String) gffFileList.getSelectedItem();

    if (selectedPath == null ||
        !selectedPath.equals(gffFileList.getEditor().getItem())) {
      selectedPath = (String) gffFileList.getEditor().getItem();
    }

    return selectedPath;
  }

  public Object doOperation(Object values) throws ApolloAdapterException {

    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      CurationSet curationSet;

      String fileName = getSelectedPath();

      FileInputStream inputStream;

      try{
        inputStream = new FileInputStream(fileName);
      } catch(FileNotFoundException theException){
        throw new ApolloAdapterException("File not found");
      }

      ((OtterXMLAdapter)driver).setInputStream(inputStream);
      curationSet = ((ApolloDataAdapterI) driver).getCurationSet();


      if (getSelectedSeqPath() != null) {
        String file = getSelectedSeqPath();

        if (file != null) {
          try { // throws IOException
            FastaFile ff = new FastaFile(file, "File", curationSet);
          }catch (IOException e) {
            System.out.println("IOException caught reading sequence file " +
                               file + " " + e);
          }
        }
      }


      String gfffile = getSelectedGFFPath();

      if (gfffile != null) {

        GFFAdapter  gff = new GFFAdapter();

        gff.setFilename(gfffile);
        try {
          StrandedFeatureSetI fset = gff.getAnalysisRegion();

          curationSet.setResults(fset);
        }
        catch (Exception e) {}
      }


      return curationSet;

    } else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      CurationSet set = (CurationSet)values;

      String fileName = getSelectedPath();
      String seqfile  = getSelectedSeqPath();
      String gfffile  = getSelectedGFFPath();

      OtterXMLAdapter oa = (OtterXMLAdapter)driver;

      oa.setSequenceFilename(seqfile);
      oa.setGFFFilename(gfffile);


      try {

        OutputStream outputStream = new FileOutputStream(fileName);

        oa.setOutputStream(outputStream);
        oa.commitChanges((CurationSet)values);
      } catch(IOException theException){
        throw new ApolloAdapterException("problems creating output file", theException);
      }
      return null;
    } else {
      return null;
    }

  }

}
