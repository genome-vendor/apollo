package apollo.dataadapter.ensj19;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;
import java.util.Properties;

import org.bdgp.io.*;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.config.Config;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.synteny.*;
import apollo.datamodel.CurationSet;
import apollo.datamodel.GenomicRange;
import apollo.gui.*;
import apollo.util.GuiUtil;

import org.ensembl19.util.*;
import org.ensembl19.gui.*;
import org.ensembl19.driver.*;

/**
 *
**/
public class AnnotationEnsJAdapterGUI extends EnsJAdapterGUI implements ApolloDataAdapterGUI{

  //  private JLabel chrLabel = new JLabel("Chr");
  //  private JLabel startLabel = new JLabel("Start");
  //  private JLabel endLabel = new JLabel("End");
  
  //  private JLabel inputFileLabel;
  //  private JTextField  inputFileField;
  //  private JLabel outputFileLabel;
  //  private JTextField outputFileField;
  private JButton testButton;
  //
  //Checkboxes which determine whether we show the expanded form of the 
  //panels...
  private JButton showTypes;
  private JButton showDataSourceConfiguration;
  private JButton showAnnotation;
  
  //
  //Overall panels, which can be expanded or just showing a checkbox...
  private JPanel typesPanel;
  private JPanel dataSourceConfigurationPanel;
  private JPanel annotationPanel;
  
  //
  //The configuration panels which are displayed when the user hits the
  //show... checkbox.
  private AnnotationSourceChooser annotationSourceChooser;
  private JPanel includePanel;
  private CompositeDataSourceConfigurationPanel dataConfigChooser;
  private boolean dbInitialised = false;
  private String logicalSpeciesName = null;
  
  private class TestActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      String inputServerName = getAnnotationSourceChooser().getSelectedInputServerName();
      String inputDataSet = getAnnotationSourceChooser().getSelectedInputDataSetName();
      
      String serverQueryString = 
        "http://"+inputServerName+
        ":19332/perl/get_region?chr="+AnnotationEnsJAdapterGUI.this.getSelectedChr()+"&"+
        "chrstart="+AnnotationEnsJAdapterGUI.this.getSelectedStart()+"&"+
        "chrend="+AnnotationEnsJAdapterGUI.this.getSelectedEnd()+"&"+
        "dataset="+inputDataSet;
      
      if(getAnnotationSourceChooser().getAnnotationUser() != null){
        serverQueryString += 
          "&author="+getAnnotationSourceChooser().getAnnotationUser()+"&email="+
          getAnnotationSourceChooser().getAnnotationUserEmail();
      }

      if(getAnnotationSourceChooser().isEditingEnabled()){
        serverQueryString += "&lock="+"true";
      }
      
      URLDiagnosticTool tool = new URLDiagnosticTool(serverQueryString);
    }
  }

  /**
   * Sets the Include Panel contained in the types panel to visible
   * and re-packs the dialog - this makes it look like the dialog
   * has expanded with those options
  **/
  public class ShowTypesActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, AnnotationEnsJAdapterGUI.this);
      if(getShowTypesButton().getLabel().equals("Show Tracks")){
        getIncludePanel().setVisible(true);
        getShowTypesButton().setLabel("Hide Tracks");
        dialog.pack();
      }else{
        getIncludePanel().setVisible(false);
        getShowTypesButton().setLabel("Show Tracks");
        dialog.pack();
      }
    }
  }

  /**
   * Space saver - 
   * Toggles the data source chooser to be visible/not. Repacks the parent dialog every time.
  **/
  public class ShowDataSourceConfigurationActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, AnnotationEnsJAdapterGUI.this);
      if(
        getShowDataSourceConfigurationButton()
          .getLabel().equals("Show Data Source Configuration")
      ){
        getDataConfigChooser().setVisible(true);
        getShowDataSourceConfigurationButton().setLabel("Hide Data Source Configuration");
        dialog.pack();
      }else{
        getDataConfigChooser().setVisible(false);
        getShowDataSourceConfigurationButton().setLabel("Show Data Source Configuration");
        dialog.pack();
      }
    }
  }
  
  /**
   * Space saver - 
   * Toggles the annotation source chooser to be visible/not. Repacks the parent dialog every time.
  **/
  public class ShowAnnotationConfigurationActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, AnnotationEnsJAdapterGUI.this);
      if(getShowAnnotationButton().getLabel().equals("Show Annotations")){
        getAnnotationSourceChooser().setVisible(true);
        getShowAnnotationButton().setLabel("Hide Annotations");
        dialog.pack();
      }else{
        getAnnotationSourceChooser().setVisible(false);
        getShowAnnotationButton().setLabel("Show Annotations");
        dialog.pack();
      }
    }
  }
  
  /**
   * If the user chooses a particular ensembl database from the default filechooser,
   * we will update the other databases with our best guess (if we can find one)
   * for the maps and variation databases.
  **/
  public class DataSourceConfigInteractionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      String ensemblDatabaseName;
      String preCoreSegment;
      String postCoreSegment;
      String variationDatabaseName;
      int coreIndex;
      DataSourceConfigurationPanel variationPanel;
      DataSourceConfigurationPanel sequencePanel;
      Properties properties;
      
      if(getDataConfigChooser().getSelectedChooser().equals("default")){
        ensemblDatabaseName = event.getActionCommand();
        //
        //split the dbname -e.g. "homo_sapiens_core_9_30" into three pieces: 
        //the bit before "core", the word "core" and the part after "core". Guess
        //the variation database name by replacing "core"->"snp"
        if(ensemblDatabaseName != null){
          coreIndex = ensemblDatabaseName.indexOf("core");
          
          //
          //Grab ALL the properties the user selected - this includes
          //the host name and port etc.
          properties = getDataConfigChooser().getChooser("default").getProperties();
          
          if(coreIndex >0){
            preCoreSegment = ensemblDatabaseName.substring(0,coreIndex);
            postCoreSegment = ensemblDatabaseName.substring(coreIndex+4);
            variationDatabaseName = preCoreSegment+"snp"+postCoreSegment;

            sequencePanel = getDataConfigChooser().getChooser("sequence");
            sequencePanel.setProperties(properties);
            
            variationPanel = getDataConfigChooser().getChooser("variation");
            properties.setProperty("database",variationDatabaseName);
            variationPanel.setProperties(properties);
            
          }else{
            //
            //The selected default db didn't have the word 'core' in it. No problem,
            //we can, at least, propagate the user's choice to the other three panels.
            variationPanel = getDataConfigChooser().getChooser("variation");
            variationPanel.setProperties(properties);

            sequencePanel = getDataConfigChooser().getChooser("sequence");
            sequencePanel.setProperties(properties);
          }//end if
        }//end if
      }//end if
    }//end actionPerformed
  }//end DataSourceConfigInteractionListener 
  
  public AnnotationEnsJAdapterGUI(IOOperation op) {
    super(op);
  }

  protected void buildGUI() {
    int row = 0;
    setLayout(new GridBagLayout());
    add(buildLocationPanel(), GuiUtil.makeConstraintAt(0,row,1));
    row++;
    
    buildTypesPanel();
    add(getTypesPanel(), GuiUtil.makeConstraintAt(0,row,1));
    
    row++;

    buildDataSourceConfigurationPanel();
    add(getDataSourceConfigurationPanel(), GuiUtil.makeConstraintAt(0,row,1));
    
    if(Config.isEditingEnabled("apollo.dataadapter.ensj.AnnotationEnsJAdapter")){
      row++;
      buildAnnotationConfigurationPanel();
      //
      //Only add annotations if editing is enabled in the configuration
      add( getAnnotationConfigurationPanel(), GuiUtil.makeConstraintAt(0,row,1));
    }
  }//end buildGUI

  /**
   * We make sure (in addition to the superclass's setStateInfo) that
   * we have added the names of the chosen annotation input and output files.
  **/
  public Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException{
    //
    //Location, selected feature types copied in.
    StateInformation stateInfo = new StateInformation();
    String region;

    //
    //Act of desperation: you could reload the ensj_defaults.conf file in here - so you had a bare
    //minimum of stuff to set into the state info?
    String filepath = findFile( DEFAULT_CONFIG_FILE );
    boolean selected;
    Properties input = new Properties();

    if(filepath!=null){
      Properties initialSettings = PropertiesUtil.createProperties( filepath );
      stateInfo.putAll(initialSettings);
    }

    
    if ( getChrButton().isSelected() ) {
      region = getSelectedChrStartEnd();
      if(region != null){
        stateInfo.put(StateInformation.REGION, region);
      }
    } else if (getCloneFragmentButton().isSelected() ) {
      stateInfo.put(StateInformation.REGION, getSelectedCloneFragment());
    } else if ( getStableIdButton().isSelected() ) {
      stateInfo.put(StateInformation.REGION, getSelectedStableID());
    }

    String apolloRoot = System.getProperty("APOLLO_ROOT");
    File logFile = new File(apolloRoot+"/conf/logging_info_level.conf");
    if(!logFile.exists()){
      throw new 
        apollo.dataadapter.ApolloAdapterException(
          "The following file must be provided to support ensj - conf/logging_info_level.conf"
        );
    }//end if
    
    stateInfo.put(StateInformation.LOGGING_FILE, apolloRoot+"/conf/logging_info_level.conf");

    //
    //Selected features
    for(int i=0; i<getDataTypeButtons().length; ++i){
      stateInfo.put(
        getDataTypeButtons()[i].propertyName,
        booleanToString( getDataTypeButtons()[i].isSelected() ) 
      );
    }//end for

    //
    //DB driver configuration - for the main driver and the sequence driver (if its different).
    Properties databaseProperties = getDataConfigChooser().getPrefixedProperties();
    stateInfo.putAll(databaseProperties);
    
    return stateInfo;
  }//end createStateInformation

  private String breakMessageIntoLines(String message){
    boolean useLineBrokenMessage = false;
    String lineSubString; //temp holder for chopped line
    String remainingMessage = message;//holds the un-line-broken piece
    String lineBrokenMessage = ""; //end result

    while(
      remainingMessage.length() > 80 ||
      remainingMessage.indexOf("\n") > 81
    ){
      useLineBrokenMessage = true;
      lineSubString = remainingMessage.substring(0,81);
      remainingMessage = remainingMessage.substring(81);
      lineBrokenMessage = 
        (new StringBuffer(lineBrokenMessage))
          .append(lineSubString)
          .append("\n")
          .toString();
    }//end if
    

    if(useLineBrokenMessage){
      
      lineBrokenMessage = 
          (new StringBuffer(lineBrokenMessage))
            .append(remainingMessage)
            .toString();
      
      return lineBrokenMessage;
    }else{
      return message;
    }
  }//end breakMessageIntoLines
  
  private void populateAnnotationStateInformation(Properties stateInfo){

    if(getAnnotationSourceChooser() != null){
      //
      //Annotation configuration - server / dataset name etc
      if(
        getAnnotationSourceChooser().getSelectedInputFileName()!=null
        &&
        getAnnotationSourceChooser().getSelectedInputFileName().trim().length() > 0
      ){
        stateInfo.setProperty(
          "AnnotationInputFile", 
          getAnnotationSourceChooser().getSelectedInputFileName().trim()
        );
      }//end if

      if(
        getAnnotationSourceChooser().getSelectedOutputFileName()!=null
        &&
        getAnnotationSourceChooser().getSelectedOutputFileName().trim().length() > 0
      ){
        stateInfo.setProperty(
          "AnnotationOutputFile", 
          getAnnotationSourceChooser().getSelectedOutputFileName().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getSelectedInputServerName()!=null){
        stateInfo.setProperty(
          "AnnotationInputServer",
          getAnnotationSourceChooser().getSelectedInputServerName().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getSelectedServerPort() != null){
        stateInfo.setProperty(
          "AnnotationServerPort",
          getAnnotationSourceChooser().getSelectedServerPort().trim()
        );
      }

      if(getAnnotationSourceChooser().getSelectedInputServerName()!=null){
        stateInfo.setProperty(
          "AnnotationOutputServer", 
          getAnnotationSourceChooser().getSelectedInputServerName().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getSelectedInputDataSetName()!=null){
        stateInfo.setProperty(
          "AnnotationInputDataSet",
          (String)getAnnotationSourceChooser().getSelectedInputDataSetName().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getSelectedOutputDataSetName()!=null){
        stateInfo.setProperty(
          "AnnotationOutputDataSet",
          (String)getAnnotationSourceChooser().getSelectedInputDataSetName().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getAnnotationUser() !=null){
        stateInfo.setProperty(
          "AnnotationAuthor",
          getAnnotationSourceChooser().getAnnotationUser().trim()
        );
      }//end if

      if(getAnnotationSourceChooser().getAnnotationUserEmail() !=null){
        stateInfo.setProperty(
          "AnnotationAuthorEmail",
          getAnnotationSourceChooser().getAnnotationUserEmail().trim()
        );
      }//end if

      stateInfo.setProperty(
        "AnnotationLock",
        new Boolean(getAnnotationSourceChooser().isEditingEnabled()).toString()
      );
    }//end if
  }//end populateAnnotationStateInformation
  
  public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException {
    Properties stateInformation = null;

    try{
  
      if(
        getOperation().equals(ApolloDataAdapterI.OP_READ_DATA) ||
        getOperation().equals(ApolloDataAdapterI.OP_APPEND_DATA)
      ){
        
        stateInformation = createStateInformation();
        populateAnnotationStateInformation(stateInformation);

        ((AbstractApolloAdapter)getDataAdapter()).clearStateInformation();
        ((ApolloDataAdapterI)getDataAdapter()).setStateInformation(stateInformation);
        
        if(getOperation().equals(ApolloDataAdapterI.OP_READ_DATA)){
          return ((ApolloDataAdapterI) getDataAdapter()).getCurationSet();
        }else{
          return ((ApolloDataAdapterI) getDataAdapter()).addToCurationSet();
        }//end if
        
      }else if(getOperation().equals(ApolloDataAdapterI.OP_WRITE_DATA)){
        stateInformation = new Properties();
        populateAnnotationStateInformation(stateInformation);
        ((AnnotationEnsJAdapter)getDataAdapter()).setWritingStateInformation(stateInformation);
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
        //((AnnotationEnsJAdapter)getDataAdapter()).commitChanges((CurationSet)values);
        ((AnnotationEnsJAdapter)getDataAdapter()).commitChanges(curSet);
        return null;
      }else{
        stateInformation = new Properties();
        populateAnnotationStateInformation(stateInformation);
        throw new apollo.dataadapter.ApolloAdapterException(
          "I cannot run the exception: "+getOperation().getName()
        );
      }
    }catch(apollo.dataadapter.NonFatalDataAdapterException exception){
      String lineBrokenMessage = breakMessageIntoLines(exception.getMessage());
      throw new apollo.dataadapter.ApolloAdapterException("Problem loading data:"+lineBrokenMessage, exception);
    }
  }

  
  /**
   * If the input is a HashMap, and use it to set values into
   * the chromosome and high/low text fields.
   *
   * If the input is a string, use it to set the logical species name.
   *
   * If the input is a composite adapter, set a handle to ourselves on
   * that adapter (our parent), keyed by our logical species name.
  **/
  public void setInput(Object input) {
    HashMap theInput;
    String text;
    //    JComboBox chromosomeDropdown;
    SyntenyAdapterGUI parentAdapterUI = null;
    //    String speciesLogicalName = null;
    
    if(input instanceof HashMap){

      getChrButton().setSelected(true);
      
      theInput = (HashMap)input;
      text = (String)theInput.get("chr");
      if(!isChromosomeListInitialised()){
        initialiseChromosomeDropdown();
      }//end if
      
      getChrDropdown().setSelectedItem(text);

      text = (String)theInput.get("start");
      getStartTextBox().setText(text);

      text = (String)theInput.get("end");
      getEndTextBox().setText(text);
      
    }else if(input instanceof String){
      
      setLogicalSpeciesName((String) input);
      
    }else if(input instanceof SyntenyAdapterGUI){
      
      if(getLogicalSpeciesName() != null){
        parentAdapterUI = (SyntenyAdapterGUI)input;
        parentAdapterUI.getSharedData().put(getLogicalSpeciesName(), this);
      }//end if

      getLocationPanel().remove(getCloneFragmentPanel());
    }//end if
  }//end setInput

  /**
   * <p>This method is run to capture the state of the GUI. It returns all GUI-state -
   * the contents of the fields, as well as the history vectors in the fields. </p>
   * 
   * <p> It is run when the adapter gui has successfully read data once (to
   * write out the history file, as well as keep state information to help with
   * navigation)</p>
   * 
   * <p> In addition to the superclass method's work, this method writes out the annotation
   * input/output file information and history, and the annotation's input/output server
   * information and history</p>
  **/
  public Properties getProperties() {
    Properties properties = super.getProperties();
    boolean lock;
    String author;
    String authorEmail;
    
    //
    //Database configuration properties
    properties.putAll(getDataConfigChooser().getPrefixedProperties());
    
    if(Config.isEditingEnabled("apollo.dataadapter.ensj.AnnotationEnsJAdapter")){
      //
      //FILE-BASED OUTPUT NAME AND HISTORIES
      String name = getAnnotationSourceChooser().getSelectedInputFileName();
      Vector history = getAnnotationSourceChooser().getInputFileHistory();

      if(name != null){
        history.add(0, name);
        properties.put("AnnotationInputFileName", name);
      }//end if

      putPrefixedProperties(properties, history, "AnnotationInputFileHistory");

      name = getAnnotationSourceChooser().getSelectedOutputFileName();
      history = getAnnotationSourceChooser().getOutputFileHistory();

      if(name != null){
        history.add(0, name);
        properties.put("AnnotationInputFileName", name);
      }//end if

      putPrefixedProperties(properties, history, "AnnotationOutputFileHistory");

      //
      //SERVER-BASED OUTPUT NAME AND HISTORIES
      name = getAnnotationSourceChooser().getSelectedInputServerName();

      history = getAnnotationSourceChooser().getInputServerHistory();

      if(
        history.size()>0 &&
        (
          history.elementAt(0)==null || 
          ((String)history.elementAt(0)).trim().length() <=0
        )
      ){
        history.remove(0);
      }//end if

      if(name != null){
        history.add(0, name);
        properties.put("AnnotationInputServerName", name);
      }//end if

      putPrefixedProperties(properties, history, "AnnotationInputServerHistory");

      name = getAnnotationSourceChooser().getSelectedServerPort();

      if(name != null){
        properties.put("AnnotationServerPort", name);
      }//end if

      name = getAnnotationSourceChooser().getSelectedInputDataSetName();

      if(name != null){
        properties.put("AnnotationInputDataSetName", name);
      }//end if

      //
      //AUTHORS, EMAILS, LOCKS
      lock = getAnnotationSourceChooser().isEditingEnabled();

      if(lock){
        properties.put("AnnotationLock", "true");
      }//end if

      author = getAnnotationSourceChooser().getAnnotationUser();

      if(author != null){
        properties.put("AnnotationAuthor", author);
      }//end if

      authorEmail = getAnnotationSourceChooser().getAnnotationUserEmail();

      if(authorEmail != null){
        properties.put("AnnotationAuthorEmail", authorEmail);
      }//end if
    }//end if
  
    return properties;
  }//end getProperties
    
  /**
   * <p>This is run by apollo to set the GUI state: it is run when the adapter is first
   * started up, as well as when the user moves around or reloads data.</p>
   *
   * <p> I set the widgets to reflect the history fields passed in - the annotation file
   * chooser histories, in addition to the superclass.</p>
  **/
  public void setProperties(Properties inputPassedIn){
    String filepath = findFile( DEFAULT_CONFIG_FILE );
    boolean selected;
    Properties input = new Properties();

    if(filepath!=null){
      Properties initialSettings = PropertiesUtil.createProperties( filepath );
      input.putAll(initialSettings);
    }

    input.putAll(inputPassedIn);

    super.setProperties(input);
    boolean setDefaultEnsemblDatabaseName = false;

    //
    //Set a default database configuration if one hasn't been given to us.
    if(input.getProperty("jdbc_driver") == null || input.getProperty("jdbc_driver").trim().length() <=0){
      input.setProperty("jdbc_driver",getInitialSettings().getProperty("jdbc_driver"));
    }//end if
    
    if(input.getProperty("host") == null || input.getProperty("host").trim().length() <=0){
      input.setProperty("host",getInitialSettings().getProperty("host"));
    }//end if
    
    if(input.getProperty("port") == null || input.getProperty("port").trim().length() <=0){
      input.setProperty("port",getInitialSettings().getProperty("port"));
    }//end if
    
    if(input.getProperty("user") == null || input.getProperty("user").trim().length() <=0){
      input.setProperty("user","anonymous");
    }//end if
    
    if(input.getProperty("ensembl_driver") == null || input.getProperty("ensembl_driver").trim().length() <=0){
      input.setProperty("ensembl_driver",getInitialSettings().getProperty("ensembl_driver"));
    }//end if
    
    if(input.getProperty("database") == null || input.getProperty("database").trim().length() <=0){
      setDefaultEnsemblDatabaseName = true;
      input.setProperty("database",getInitialSettings().getProperty("database"));
    }//end if

    if(input.getProperty("signature") == null || input.getProperty("signature").trim().length() <=0){
      input.setProperty("signature", getInitialSettings().getProperty("signature"));
    }//end if
    
    if(input.getProperty("variation.signature") == null || input.getProperty("variation.signature").trim().length() <=0){
      input.setProperty("variation.signature", getInitialSettings().getProperty("variation.signature"));
    }//end if
    
    if(input.getProperty("sequence.signature") == null || input.getProperty("sequence.signature").trim().length() <=0){
      input.setProperty("sequence.signature", getInitialSettings().getProperty("sequence.signature"));
    }//end if
    
    //
    //The setting of all the fiels in the database config section should only
    //happen once, when the gui is first created.
    if(!isDbInitialised()){
      //
      //Pass these properties onto the data-config chooser - this will make each
      //panel in the chooser fill in the text fields pointing to the database instance.
      getDataConfigChooser().setProperties(input);

      //
      //Now load up _all_ the ensembl db dropdowns with
      //the appropriate ensembl databases - core, variation and sequence resp.
      getDataConfigChooser().populateEnsemblDatabases();

      //
      //Now turn around and re-select the right ensembl database (if one was passed in).
      //--but don't bother unless we successfully loaded some databases...
      if(
        getDataConfigChooser()
          .getChooser("default")
          .getEnsemblDatabaseDropdown()
          .getModel().getSize() >1
      ){
        getDataConfigChooser()
          .getChooser("default")
          .setSelectedEnsemblDatabase(input.getProperty("database"));
      }//end if
      setDbInitialised(true);
    }//end if
    
    if(Config.isEditingEnabled("apollo.dataadapter.ensj.AnnotationEnsJAdapter")){
      //
      //RECOVER FILE NAMES AND HISTORIES
      String name = input.getProperty("AnnotationInputFileName");
      Vector fileVector =  
        fileVector =  
          filterHistory(
            getPrefixedProperties(input, "AnnotationInputFileHistory", true)
          );
      if(fileVector.size()>0 && fileVector.elementAt(0) != null && ((String)fileVector.elementAt(0)).trim().length() > 0){
        fileVector.add(0,"");
      }//end if
      getAnnotationSourceChooser().setInputFileHistory(fileVector);
      getAnnotationSourceChooser().setSelectedInputFileName(name);

      fileVector =  
        filterHistory(
          getPrefixedProperties(input, "AnnotationOutputFileHistory", true)
        );
      if(fileVector.size()>0 && fileVector.elementAt(0) != null && ((String)fileVector.elementAt(0)).trim().length() > 0){
        fileVector.add(0,"");
      }//end if
      getAnnotationSourceChooser().setOutputFileHistory(fileVector);
      name = input.getProperty("AnnotationOutputFileName");
      getAnnotationSourceChooser().setSelectedOutputFileName(name);

      fileVector =  
        filterHistory(
          getPrefixedProperties(input, "AnnotationInputServerHistory", false)
        );
      if(fileVector.size()>0 && fileVector.elementAt(0) != null && ((String)fileVector.elementAt(0)).trim().length() > 0){
        fileVector.add(0,"");
      }//end if
      getAnnotationSourceChooser().setInputServerHistory(fileVector);

      name = input.getProperty("AnnotationInputServerName");
      getAnnotationSourceChooser().setSelectedInputServerName(name);

      name = input.getProperty("AnnotationServerPort");
      getAnnotationSourceChooser().setSelectedServerPort(name);
      
      //
      //At this point we have set what we need to load possible datasets.
      //-so we run a fetch on those datasets to load the dropdown with them.
      if(
        getAnnotationSourceChooser().getSelectedInputServerName() != null &&
        getAnnotationSourceChooser().getSelectedServerPort() != null
      ){
        getAnnotationSourceChooser().getFindButton().doClick();
      }//end if
      
      name = input.getProperty("AnnotationInputDataSetName");
      getAnnotationSourceChooser().setSelectedInputDataSetName(name);

      //
      //Note that the output server is taken to be the input server by default for this adapter.

      //
      //RECOVER AUTHORS, EMAILS, LOCKS 
      String lock = input.getProperty("AnnotationLock");
      if(lock != null){
        getAnnotationSourceChooser().isEditingEnabled();
      }

      String author = input.getProperty("AnnotationAuthor");
      getAnnotationSourceChooser().setAnnotationUser(author);

      String authorEmail = input.getProperty("AnnotationAuthorEmail");
      getAnnotationSourceChooser().setAnnotationUserEmail(authorEmail);
    }//end if
  }//end setProperties

  /**
   * Go through the history, removing any blank elements and 
   * duplicates.
  **/
  private Vector filterHistory(Vector history){
    Vector uniqueHistory = new Vector();
    Object element = null;
    for(int i=0; i<history.size(); i++){
      element = history.get(i);
      if(
        element != null &&
        element.toString().trim().length() > 0 &&
        !uniqueHistory.contains(history.get(i))
      ){
        uniqueHistory.add(history.get(i));
      }
    }
    return uniqueHistory;
  }
  
  private AnnotationSourceChooser getAnnotationSourceChooser(){
    return annotationSourceChooser;
  }//end getAnnotationSourceChooser
  
  private void setAnnotationSourceChooser(AnnotationSourceChooser newValue){
    annotationSourceChooser = newValue;
  }//end setAnnotationSourceChooser
  
  private JButton getTestButton(){
    return testButton;
  }//end getTestButton

  private void setTestButton(JButton button){
    testButton = button;
  }//end setTestButton
  
  private JButton getShowTypesButton(){
    return showTypes;
  }//end getShowTypesButton
  
  private JButton getShowDataSourceConfigurationButton(){
    return showDataSourceConfiguration;
  }//end getShowDataSourceConfigurationButton
  
  private JButton getShowAnnotationButton(){
    return showAnnotation;
  }//end getShowAnnotationButton

  private JPanel buildTypesPanel(){
    typesPanel = new JPanel();
    typesPanel.setLayout(new GridBagLayout());
    typesPanel.setBorder(new TitledBorder("Tracks"));

    //
    //Not immediately visible - will display when the user selects 
    //"show" checkbox.
    includePanel = buildIncludePanel();
    
    showTypes = new JButton("Show Tracks");
    
    showTypes.addActionListener(new AnnotationEnsJAdapterGUI.ShowTypesActionListener());
    
    typesPanel.add(getShowTypesButton(), GuiUtil.makeConstraintAt(0,0,1));
    includePanel.setVisible(false);
    typesPanel.add(includePanel, GuiUtil.makeConstraintAt(0,1,1));
    return typesPanel;
  }//end buildTypesPanel
  
  private JPanel getTypesPanel(){
    return typesPanel;
  }
  
  private JPanel getIncludePanel(){
    return includePanel;
  }
  
  private JPanel buildDataSourceConfigurationPanel(){
    dataSourceConfigurationPanel = new JPanel();
    dataSourceConfigurationPanel.setLayout(new GridBagLayout());
    dataSourceConfigurationPanel.setBorder(
      BorderFactory.createTitledBorder("Data Source")
    );
    showDataSourceConfiguration = new JButton("Show Data Source Configuration");
    showDataSourceConfiguration.addActionListener(
      new AnnotationEnsJAdapterGUI.ShowDataSourceConfigurationActionListener()
    );

    dataSourceConfigurationPanel.add(
      getShowDataSourceConfigurationButton(), GuiUtil.makeConstraintAt(0,0,1)
    );

    //
    //This component is not immediately visible - we'll display when 
    //the user selects "show" checkbox.
    dataConfigChooser = new CompositeDataSourceConfigurationPanel();
    //
    //This is the listener which controls which databases are chosen by
    //default when the user selects databases.
    dataConfigChooser.setInteractionListener(
      new DataSourceConfigInteractionListener()
    );
    
    dataConfigChooser.setVisible(false);
    dataSourceConfigurationPanel.add(
      dataConfigChooser, GuiUtil.makeConstraintAt(0,1,1)
    );
    
    addDataChangeListenerToDataSourceConfigPanels();
    
    return dataSourceConfigurationPanel;
  }//end buildDataSourceConfigurationPanel
  
  public CompositeDataSourceConfigurationPanel getDataConfigChooser(){
    return dataConfigChooser;
  }//end getDataConfigChooser
  
  private JPanel getDataSourceConfigurationPanel(){
    return dataSourceConfigurationPanel;
  }//end getDataSourceConfigurationPanel
  
  private JPanel getAnnotationConfigurationPanel(){
    return annotationPanel;
  }//end getAnnotationConfigurationPanel
  
  private JPanel buildAnnotationConfigurationPanel(){
    annotationPanel = new JPanel();
    annotationPanel.setLayout(new GridBagLayout());
    annotationPanel.setBorder(
      BorderFactory.createTitledBorder("Annotation Source")
    );
    showAnnotation = new JButton("Show Annotations");
    showAnnotation.addActionListener(
      new AnnotationEnsJAdapterGUI.ShowAnnotationConfigurationActionListener()
    );
    
    //
    //This component is not immediately visible - we'll display when 
    //the user selects "show" checkbox.
    annotationSourceChooser = new OtterAnnotationSourceChooser();
    annotationSourceChooser.setVisible(false);
    annotationPanel.add(getShowAnnotationButton(), GuiUtil.makeConstraintAt(0,0,1));
    annotationPanel.add(annotationSourceChooser, GuiUtil.makeConstraintAt(0,1,1));
    return annotationPanel;
  }
  
  private boolean isDbInitialised(){
    return dbInitialised;
  }//end isDbInitialised
  
  private void setDbInitialised(boolean initialised){
    dbInitialised = initialised;
  }//end setDbInitialised
  
  private String getLogicalSpeciesName(){
    return logicalSpeciesName;
  }//end getLogicalSpeciesName
  
  private void setLogicalSpeciesName(String speciesName){
    logicalSpeciesName = speciesName;
    System.out.println ("Species is " + speciesName);
  }//end setLogicalSpeciesName
  
  protected void addDataChangeListenerToDataSourceConfigPanels(){
    ActionListener listener = new EnsJAdapterGUI.DataSourceChangeListener();
    getDataConfigChooser().addExternalDataSourceChangeListener(listener);
  }//end addDataChangeListenerToDataSourceConfigPanels
  
}


