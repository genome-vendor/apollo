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
import apollo.config.Config;
import apollo.gui.ProxyDialog;
import apollo.util.GuiUtil;

/**
 * I am a very simple test interface for the otter adapter.
 * I am NOT a production-strength interface to fetch/write
 * otter-format annotations.
**/
public class OtterAdapterGUI extends AbstractDataAdapterUI {

  protected DataAdapter driver;
  protected IOOperation op;
  private JLabel inputFileLabel;
  private JTextField inputFileField;
  private JLabel outputFileLabel;
  private JTextField outputFileField;

  private Properties props;

  public OtterAdapterGUI(IOOperation op) {
    this.op = op;
    inputFileLabel = new JLabel("Otter Input File:");
    inputFileField = new JTextField(20);
    outputFileLabel = new JLabel("Otter Output File:");
    outputFileField = new JTextField(20);
    buildGUI();
  }

  public void setProperties(Properties in) {
  }

  public Properties getProperties() {
    return new Properties();
  }

  public void buildGUI() {
    JPanel internalPanel = new JPanel();
    internalPanel.setLayout(new GridBagLayout());
    
    internalPanel.add(inputFileLabel, GuiUtil.makeConstraintAt(0,0,1));
    internalPanel.add(inputFileField, GuiUtil.makeConstraintAt(0,1,1));
    internalPanel.add(outputFileLabel, GuiUtil.makeConstraintAt(1,0,1));
    internalPanel.add(outputFileField, GuiUtil.makeConstraintAt(1,1,1));
    add(internalPanel);
  }

  public void setDataAdapter(DataAdapter driver) {
    this.driver = driver;
  }
  
  public DataAdapter getDataAdapter(){
    return driver;
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    CurationSet curationSet;
    String fileName = inputFileField.getText();
    FileInputStream theInputStream;
    File outputFile;
    FileOutputStream outputStream;
    
    if(fileName == null || fileName.trim().length() <=0){
      fileName = "/auto/acari/vvi/src/apollo-current/apollo/src/java/apollo/dataadapter/otter/parser/test4.xml";
    }//end if
    
    try{
      theInputStream = new FileInputStream(fileName);
    }catch(FileNotFoundException theException){
      throw new ApolloAdapterException("File not found");
    }//end try
    
    fileName = outputFileField.getText();

    if(fileName == null || fileName.trim().length() <=0){
      fileName = "/auto/acari/vvi/src/apollo-current/apollo/src/java/apollo/dataadapter/otter/parser/annotation-output.xml";
    }//end if
    
    outputFile = new File(fileName);
    
    if(outputFile.exists()){
      outputFile.delete();
    }//end if

    try{
      outputFile.createNewFile();

      outputStream = new FileOutputStream(outputFile);
    }catch(IOException theException){
      throw new ApolloAdapterException("problems creating output file", theException);
    }//end try
    
    ((OtterAdapter)getDataAdapter()).setInputStream(theInputStream);
    ((OtterAdapter)getDataAdapter()).setOutputStream(outputStream);
    
    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      curationSet = ((ApolloDataAdapterI) driver).getCurationSet();
      return curationSet;
    } else {
      return null;
    }//end if

  }//end doOperation
  
}
