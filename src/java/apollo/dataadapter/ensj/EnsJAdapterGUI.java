package apollo.dataadapter.ensj;

import org.bdgp.swing.AbstractDataAdapterUI;
import java.util.Properties;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import apollo.datamodel.CurationSet;
import java.awt.*;
import apollo.dataadapter.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import apollo.util.GuiUtil;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import apollo.dataadapter.ensj.controller.*;

public class EnsJAdapterGUI extends AbstractDataAdapterUI  implements ApolloDataAdapterGUI{

  private IOOperation _operation;
  private View _view;
  private Model _model;
  private Controller _controller;
//  private static final int MAX_HISTORY_LENGTH = 20;  

  public EnsJAdapterGUI(IOOperation operation) {

    _operation = operation;
    _view = new View(this);
    _model = new Model();
    _controller = new Controller(getModel(), getView());

    setLayout(new BorderLayout());
    add(getView(),BorderLayout.CENTER);
    getView().initialiseView();
    getController().handleEventForKey(Controller.READ);
  }
  
  public Object doOperation(Object values) throws ApolloAdapterException {
    Properties stateInformation = null;

    try{
  
      if(getOperation().equals(ApolloDataAdapterI.OP_READ_DATA)){
        
        stateInformation = createStateInformation();

        ((ApolloDataAdapterI)getDataAdapter()).setStateInformation(stateInformation);
        
        return ((ApolloDataAdapterI) getDataAdapter()).getCurationSet();
        
      } else if(getOperation().equals(ApolloDataAdapterI.OP_APPEND_DATA)){
        stateInformation = createStateInformation();
        ((ApolloDataAdapterI)getDataAdapter()).setStateInformation(stateInformation);
        
        return ((ApolloDataAdapterI) getDataAdapter()).addToCurationSet();

      }/*else if(getOperation().equals(ApolloDataAdapterI.OP_WRITE_DATA)){
        
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
          curSet = cdh.getSpeciesCurationSet(0);
        }
        //((AnnotationEnsJAdapter)getDataAdapter()).commitChanges((CurationSet)values);
        ((AnnotationEnsJAdapter)getDataAdapter()).commitChanges(curSet);
        return null;
      }*/
      else{

        throw new apollo.dataadapter.ApolloAdapterException(
          "I cannot run the exception: "+getOperation().getName()
        );
        
      }
    }catch(apollo.dataadapter.NonFatalDataAdapterException exception){
      //String lineBrokenMessage = breakMessageIntoLines(exception.getMessage());
      //throw new apollo.dataadapter.DataAdapterException("Problem loading data:"+lineBrokenMessage, exception);
      throw new apollo.dataadapter.ApolloAdapterException("Problem loading data:"+exception.getMessage(), exception);
    }
  }//end doOperation

  /**
   * Convert the Model (which has all the UI's state information) into a properties object
   * to be passed to the dataadapter.
  **/
  public Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException{
    //
    //Location, selected feature types copied in.
    StateInformation stateInfo = new StateInformation();
    String region;
    getController().handleEventForKey(Controller.UPDATE);
    LocationModel locationModel = getModel().getLocationModel();
    TypesModel typesModel = getModel().getTypesModel();
    OptionsModel optionsModel = getModel().getOptionsModel();
    DatabaseModel databaseModel = getModel().getDatabaseModel();
    File logFile = null;
    String apolloRoot;
    
    if (!locationModel.isStableIDLocation()){
      region = 
        "REGION: "+locationModel.getSelectedCoordSystem()+":"+locationModel.getSelectedSeqRegion()+":"+
        locationModel.getStart()+"-"+locationModel.getEnd();
      
      if(region != null){
        stateInfo.put(StateInformation.REGION, region);
      }
    } else {
      stateInfo.put(StateInformation.REGION, "ID: "+locationModel.getStableID());
    }

    apolloRoot = System.getProperty("APOLLO_ROOT");

    logFile = new File(apolloRoot+"/conf/logging_info_level.conf");
    
    if(! logFile.exists()){
      throw new 
        apollo.dataadapter.ApolloAdapterException(
          "The following file must be provided to support ensj - conf/logging_info_level.conf"
        );
    }//end if

    stateInfo.put(StateInformation.LOGGING_FILE, apolloRoot+"/conf/logging_info_level.conf");

    //
    //Selected features
    stateInfo.put(StateInformation.INCLUDE_GENE, typesModel.includeGenes());
    stateInfo.put(StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT, typesModel.includeDnaProteinAlignments());
    stateInfo.put(StateInformation.INCLUDE_DNA_DNA_ALIGNMENT, typesModel.includeDnaDnaAlignments());
    stateInfo.put(StateInformation.INCLUDE_FEATURE, typesModel.includeSimpleFeatures());
    stateInfo.put(StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE, typesModel.includeProteinAnnotations());
    stateInfo.put(StateInformation.INCLUDE_PREDICTION_TRANSCRIPT, typesModel.includeAbInitioPredictions());
    stateInfo.put(StateInformation.INCLUDE_REPEAT_FEATURE, typesModel.includeRepeats());
    stateInfo.put(StateInformation.INCLUDE_DITAG_FEATURE, typesModel.includeDitagFeatures());
    stateInfo.put(StateInformation.INCLUDE_CONTIG_FEATURE, typesModel.includeContigs());

    //Options for feature load
    stateInfo.put(StateInformation.RESET_GENE_START_AND_STOP, optionsModel.resetGeneStartAndStop());
    stateInfo.put(StateInformation.AGGRESSIVE_GENE_NAMING, optionsModel.aggressiveGeneNaming());
    stateInfo.put(StateInformation.ADD_RESULT_GENES_AS_ANNOTATIONS, optionsModel.addResultGenesAsAnnotations());
    stateInfo.put(StateInformation.ADD_TRANSCRIPT_SUPPORT, optionsModel.addSupportToTranscripts());
    stateInfo.put(StateInformation.TYPE_PREFIX_STRING, optionsModel.typePrefix());
    
    //
    //If you have selected to ONLY bring in certain of the types of features, we will write out
    //the types with subscripts - so INCLUDE_GENE_TYPE0='ensembl', INCLUDE_GENE_TYPE1='estgene' etc.
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedGeneTypes(), StateInformation.INCLUDE_GENE_TYPES);
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedDnaDnaAlignTypes(), StateInformation.INCLUDE_DNA_DNA_ALIGNMENT_TYPES);
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedDnaProteinAlignTypes(), StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES);
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedPredictionTypes(), StateInformation.INCLUDE_PREDICTION_TRANSCRIPT_TYPES);
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedSimpleFeatureTypes(), StateInformation.INCLUDE_FEATURE_TYPES);
    EnsJAdapterUtil.putPrefixedProperties(stateInfo, typesModel.getSelectedDitagFeatureTypes(), StateInformation.INCLUDE_DITAG_TYPES);

    //
    //DB driver configuration - for the main driver and the sequence driver (if its different).
    //Properties databaseProperties = getDataConfigChooser().getPrefixedProperties();
    stateInfo.put(StateInformation.HOST, databaseModel.getHost());
    stateInfo.put(StateInformation.PORT, databaseModel.getPort());
    stateInfo.put(StateInformation.USER, databaseModel.getUser());
    stateInfo.put(StateInformation.PASSWORD, databaseModel.getPassword());
    if (databaseModel.getSelectedEnsemblDatabase() != null) {
      stateInfo.put(StateInformation.DATABASE, databaseModel.getSelectedEnsemblDatabase());
      stateInfo.put(StateInformation.NAME, databaseModel.getSelectedEnsemblDatabase());
    }
    
    addLocationHistory(stateInfo);
    return stateInfo;
  }//end createStateInformation

 /**
   * If the input is a HashMap, and use it to set values into
   * the chromosome and high/low text fields.
   *
   * I don't like having to put this back in but SytenyAdapterGUI
   * needs it
   *
  **/
  public void setInput(Object input) {
    HashMap theInput;

    if(input instanceof HashMap){
      LocationModel locationModel = getModel().getLocationModel();
      String text;

      theInput = (HashMap)input;

      getController().handleEventForKey(Controller.FIND_COORD_SYSTEMS);

      text = (String)theInput.get("coordsys");
      locationModel.setSelectedCoordSystem(text);

      text = (String)theInput.get("chr");

      locationModel.setSelectedSeqRegion(text);

      text = (String)theInput.get("start");
      locationModel.setStart(text);

      text = (String)theInput.get("end");
      locationModel.setEnd(text);

      getController().handleEventForKey(Controller.READ);
    } else {
      //System.out.println("Don't know how to handle data of type " + input);
    }
  }

  /**
   * Take the history provided by the input properties and 
   * create the model.
  **/
  public void setProperties(Properties input) {
    LocationModel locationModel = getModel().getLocationModel();
    TypesModel typesModel = getModel().getTypesModel();
    OptionsModel optionsModel = getModel().getOptionsModel();
    DatabaseModel databaseModel = getModel().getDatabaseModel();
    java.util.List locationHistory;
    java.util.List stableIDHistory; 
    java.util.List includedTypes; 

    locationHistory = EnsJAdapterUtil.getPrefixedProperties(input, "locationHistory");
    locationModel.setLocationHistory(locationHistory);
    
    stableIDHistory = EnsJAdapterUtil.getPrefixedProperties(input, "stableIDHistory");
    locationModel.setStableIDHistory(stableIDHistory);
    
    typesModel.setIncludeGenes(input.getProperty(StateInformation.INCLUDE_GENE));
// This and other setSelectedXTypes calls put further down method after db has been selected because
// db selection now clears selected types.
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_GENE_TYPES);
//    typesModel.setSelectedGeneTypes(includedTypes);
    
    typesModel.setIncludeDnaProteinAlignments(input.getProperty(StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT));
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES);
//    typesModel.setSelectedDnaProteinAlignTypes(includedTypes);
    
    typesModel.setIncludeDnaDnaAlignments(input.getProperty(StateInformation.INCLUDE_DNA_DNA_ALIGNMENT));
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DNA_DNA_ALIGNMENT_TYPES);
//    typesModel.setSelectedDnaDnaAlignTypes(includedTypes);
    
    typesModel.setIncludeAbInitioPredictions(input.getProperty(StateInformation.INCLUDE_PREDICTION_TRANSCRIPT)); 
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_PREDICTION_TRANSCRIPT_TYPES);
//    typesModel.setSelectedPredictionTypes(includedTypes);
    
    typesModel.setIncludeSimpleFeatures(input.getProperty(StateInformation.INCLUDE_FEATURE));
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_FEATURE_TYPES);
//    typesModel.setSelectedSimpleFeatureTypes(includedTypes);
    
    typesModel.setIncludeProteinAnnotations(input.getProperty(StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE));
    
    typesModel.setIncludeRepeats((input.getProperty(StateInformation.INCLUDE_REPEAT_FEATURE)));
    
    typesModel.setIncludeDitagFeatures(input.getProperty(StateInformation.INCLUDE_DITAG_FEATURE));
//    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DITAG_TYPES);
//    typesModel.setSelectedDitagFeatureTypes(includedTypes);

    typesModel.setIncludeContigs((input.getProperty(StateInformation.INCLUDE_CONTIG_FEATURE)));

    // SMJS added this so that initial state isn't inconsistent with view in builder adapter
    //      on second load. It may be irritating to the user, but lets see. It's definitely
    //      less irritating than thinking you've selected types when you haven't
    typesModel.setTypePanelToShow(TypesModel.NONE);

    optionsModel.setResetGeneStartAndStop((input.getProperty(StateInformation.RESET_GENE_START_AND_STOP)));
    optionsModel.setAggressiveGeneNaming((input.getProperty(StateInformation.AGGRESSIVE_GENE_NAMING)));
    optionsModel.setAddResultGenesAsAnnotations((input.getProperty(StateInformation.ADD_RESULT_GENES_AS_ANNOTATIONS)));
    optionsModel.setAddSupportToTranscripts((input.getProperty(StateInformation.ADD_TRANSCRIPT_SUPPORT)));
    optionsModel.setTypePrefix((input.getProperty(StateInformation.TYPE_PREFIX_STRING)));

    databaseModel.setHost(input.getProperty(StateInformation.HOST));    
    databaseModel.setPort(input.getProperty(StateInformation.PORT));
    databaseModel.setUser(input.getProperty(StateInformation.USER));
    databaseModel.setPassword(input.getProperty(StateInformation.PASSWORD));
    
    getController().handleEventForKey(Controller.READ);
    
    //
    //At this point the model has enough information to load up the list of databases
    //at the mysql instance. So do that:
    getController().handleEventForKey(Controller.FIND_ENSEMBL_DATABASE_NAMES);
    
    //
    //Now select the database the user last chose
    databaseModel.setSelectedEnsemblDatabase(input.getProperty(StateInformation.DATABASE));

    getController().handleEventForKey(Controller.READ);
    
    // SMJS Note this will clear lots of the types model because it has to ensure that types are actually available in this database, so
    // here we must set included types AFTER this call
    if(databaseModel.getSelectedEnsemblDatabase() != null && databaseModel.getSelectedEnsemblDatabase().trim().length()>0){
      getController().handleEventForKey(Controller.SELECT_NEW_ENSEMBL_DATABASE);
    }

    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_GENE_TYPES);
    typesModel.setSelectedGeneTypes(includedTypes);
    
    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES);
    typesModel.setSelectedDnaProteinAlignTypes(includedTypes);
    
    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DNA_DNA_ALIGNMENT_TYPES);
    typesModel.setSelectedDnaDnaAlignTypes(includedTypes);
    
    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_PREDICTION_TRANSCRIPT_TYPES);
    typesModel.setSelectedPredictionTypes(includedTypes);
    
    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_FEATURE_TYPES);
    typesModel.setSelectedSimpleFeatureTypes(includedTypes);
    
    includedTypes = EnsJAdapterUtil.getPrefixedProperties(input, StateInformation.INCLUDE_DITAG_TYPES);
    typesModel.setSelectedDitagFeatureTypes(includedTypes);

    getController().handleEventForKey(Controller.READ);
    
    if (getOperation().equals(ApolloDataAdapterI.OP_APPEND_DATA)){
      getController().handleEventForKey(Controller.HIDE_OR_SHOW_DATABASE);
      getController().handleEventForKey(Controller.HIDE_OR_SHOW_TYPES);
      getController().handleEventForKey(Controller.HIDE_OR_SHOW_LOCATION);
    }
    //
    //Make the panel display the initial values we've set into the model.
    getController().handleEventForKey(Controller.READ);
    //if (
  }

  /**
   * Create the property list which is written to the history file
  **/
  public Properties getProperties() {
    //this loads in include-info, and the database properties. 
    Properties properties = new Properties(); 

    try{
      properties = createStateInformation();
    }catch(ApolloAdapterException exception){
       System.out.println("Not providing properties because of this problem: "+exception.getMessage()); 
       exception.printStackTrace();
       return properties;
    }
    //loads in the histories of stable id and seq-region choices.
    //addLocationHistory(properties);
    return properties;
  }

  private void addLocationHistory(Properties properties){
    LocationModel locationModel = getModel().getLocationModel();
    DatabaseModel databaseModel = getModel().getDatabaseModel();
    
    String currentStableID = null;
    java.util.List locationHistory = locationModel.getLocationHistory();
    java.util.List stableIDHistory = locationModel.getStableIDHistory();
    
    //write out a string corresponding summarising the current location.
    String currentLocation = null;

    if(!locationModel.isStableIDLocation()){
      
      currentLocation = 
        locationModel.getSelectedCoordSystem()+":"+locationModel.getSelectedSeqRegion()+":"+
        locationModel.getStart()+"-"+locationModel.getEnd();

      if(currentLocation != null && !currentLocation.equals("null:null:-") && !locationHistory.contains(currentLocation)){
        locationHistory.add(0, currentLocation);
      }
      
    }else if(locationModel.isStableIDLocation()){
      
      currentStableID = locationModel.getStableID();
      if(
        currentStableID != null && 
        currentStableID.trim().length() > 0 &&
        !stableIDHistory.contains(currentStableID)
      ){
        stableIDHistory = locationModel.getStableIDHistory();
        stableIDHistory.add(0, currentStableID); 
      }
      
    }
    EnsJAdapterUtil.putPrefixedProperties(properties, locationHistory, "locationHistory");
    EnsJAdapterUtil.putPrefixedProperties(properties, stableIDHistory, "stableIDHistory");
  }


  public DataAdapter getDataAdapter(){
    return driver;
  }

  private IOOperation getOperation(){
    return _operation;
  }//end getOperation

  public View getView(){
    return _view;
  }

  public Model getModel(){
    return _model;
  }

  public Controller getController(){
    return _controller;
  }
  
  public void addActionRouter(
    JButton button,
    String  handlerKey
  ){
    getController().addActionRouter(button, handlerKey);
  }
  
  public void addKeyRouter(
    JTextField textField,
    String  handlerKey
  ){
    getController().addKeyRouter(textField, handlerKey);
  }
  
  public void addActionRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    getController().addActionRouter(dropDown, handlerKey);
  }
  
  public void addKeyRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    getController().addKeyRouter(dropDown, handlerKey);
  }
  
  public void addKeyRouter(
    Component component,
    String  handlerKey
  ){
    getController().addKeyRouter(component, handlerKey);
  }

  public void addPopupRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    getController().addPopupRouter(dropDown, handlerKey);
  }
  
  /*
  public static void main(String[] args) {
    EnsJAdapterGUI app =  new EnsJAdapterGUI(org.bdgp.io.IOOperation.READ);
    
    View view = app.getView();
    view.initialiseView();

    Model model = app.getModel();

    LocationModel locationModel = model.getLocationModel();
    DatabaseModel databaseModel = model.getDatabaseModel();
    databaseModel.setHost("ensembldb.sanger.ac.uk");
    databaseModel.setUser("anonymous");
    databaseModel.setPort("3306");
//    ArrayList dbs = new ArrayList();
    String myDB = "homo_sapiens_core_21_34d";
//    dbs.add(myDB);
//    dbs.add("mus_musculus_core_21_32b");
    
//    databaseModel.setEnsemblDatabases(dbs);
    databaseModel.setSelectedEnsemblDatabase(myDB);

    app.getController().handleEventForKey(Controller.READ);
    view.setPreferredSize(new Dimension(800, 800));

    JFrame frame = new JFrame("Test");
    frame.getContentPane().add(view, BorderLayout.CENTER);
    frame.pack();
    frame.show();
  }
   */
}
