package apollo.dataadapter.sample;

import org.bdgp.swing.AbstractDataAdapterUI;
import java.util.Properties;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import apollo.datamodel.CurationSet;
import java.awt.*;
import apollo.dataadapter.*;
import javax.swing.*;
import java.util.*;
import apollo.util.GuiUtil;

/**
 * I am a simple adapter gui that serves as a minimal sample that people can
 * use to understand how a data adapter works, and 
 * cut-and-paste from, if neccessary.
**/
public class SampleAdapterGUI extends AbstractDataAdapterUI {

  private IOOperation operation;
  
  private JLabel numberLabel = new JLabel("Number of features : ");
  private JLabel lastChosenLabel = new JLabel("");
  private JTextField numberTextField = new JTextField(10);
  
  /**
   * <p>Builds the graphical elements and lays them out. </p>
   * 
   * <p>Note that a GUI is created for a _particular_ operation. The notion
   * was (I guess) that you'd create a gui-instance to do reading, and another
   * to do writing etc. Both instances are attached to the _same_ dataadapter. </p>
  **/
  public SampleAdapterGUI(IOOperation operation) {
    this.operation = operation;
    buildGUI();
  }


  /**
   * This is effectively a part of the constructor, split out for neatness.
  **/
  private void buildGUI() {
    setLayout(new GridBagLayout());
    add(getNumberLabel(), GuiUtil.makeConstraintAt(0,0,1));
    add(getNumberTextField(), GuiUtil.makeConstraintAt(1,0,1));
    add(getLastChosenLabel(), GuiUtil.makeConstraintAt(2,0,1));
  }//end buildGUI
  
  /**
   * The central method - called by the DataAdapterChooser - when the user hits the "OK"
   * button.
  **/
  public Object doOperation(Object values) throws ApolloAdapterException {
    //    CurationSet curationSet = null;
    SampleAdapter adapter = ((SampleAdapter)getDataAdapter());
    Properties stateInformation = new Properties();
    
    try{
      
      if (getOperation().equals(ApolloDataAdapterI.OP_READ_DATA)) {
        stateInformation.setProperty(adapter.NUMBER_OF_FEATURES, String.valueOf(getNumberOfFeatures()));
        adapter.setStateInformation(stateInformation);
        CurationSet set = adapter.getCurationSet();
        return set;
      }else{
        throw new apollo.dataadapter.ApolloAdapterException(
          "This adapter cannot be used to write data"
        );
      }//end if
      
    }catch(NonFatalDataAdapterException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }//end try
    
  }//end doOperation

  /**
   * Allows the adapter to set its history lists from the input properties
   * passed in: these properties are derived from the history file. We choose
   * to remind the user of their last choice in a label (a clearly silly 
   * way of using history).
  **/
  public void setProperties(Properties input) {

    String lastChosen = null;
    lastChosen = input.getProperty(SampleAdapter.NUMBER_OF_FEATURES);
    if(lastChosen != null && lastChosen.trim().length() >0){
      getLastChosenLabel().setText(" ( "+lastChosen+" ) ");
    }//end if
  }

  /*
   * This method is called in the org.bdgp.io.DataAdapterChooser, and the
   * output is written to the adapter's history. Note 
   * we use the static constants stored on the SampleAdapter to key our properties (safe!).
  **/
  public Properties getProperties() {
    Properties properties = new Properties();
    properties.setProperty(SampleAdapter.NUMBER_OF_FEATURES, String.valueOf(getNumberOfFeatures()));
    return properties;
  }


  /**
   * The "setDataAdapter" is implemented in the superclass, which is where the
   * instance variable is defined. For some reason we still have to implement the
   * accessor, though (!).
  **/
  public DataAdapter getDataAdapter(){
    return driver;
  }

  /**
   * The paricular operation that this GUI has been created for (reading, writing
   * or 'rithmetic?).
  **/
  private IOOperation getOperation(){
    return operation;
  }//end getOperation

  /**
   * Label on screen
  **/
  private JLabel getNumberLabel(){
    return numberLabel;
  }//end getNumberLabel
  
  /**
   * A label that displays the last choice of number-of-features
   * the user made.
  **/
  private JLabel getLastChosenLabel(){
    return lastChosenLabel;
  }//end getLastChosenLabel
  
  /**
   * User types in choice of number-of-features to display in here.
  **/
  private JTextField getNumberTextField(){
    return numberTextField;
  }//end getNumberTextField

  /**
   * Returns the number of features the user asked to be displayed
  **/
  private int getNumberOfFeatures(){
    String numberString = getNumberTextField().getText();
    int number = 0;
    try{
      number = Integer.valueOf(numberString).intValue();
    }catch(NumberFormatException exception){
      throw new NonFatalDataAdapterException("Please enter a valid integer number of features");
    }//end try

    return number;
  }//end getNumberOfFeatures

}
