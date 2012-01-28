package apollo.dataadapter.ensj.controller;

import java.awt.Component;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.sql.Connection;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

import org.apache.log4j.*;



public class Controller{
  
  protected final static Logger logger = LogManager.getLogger(Controller.class);
  private Model _model;
  private View  _view;
  private HashMap _handlers = new HashMap();
  private boolean _isBusyHandlingEvent;
  
  public static final String UPDATE = "UPDATE";
  public static final String READ = "READ";
  public static final String HIDE_OR_SHOW_TYPES = "HIDE_OR_SHOW_TYPES";
  public static final String HIDE_OR_SHOW_OPTIONS = "HIDE_OR_SHOW_OPTIONS";
  public static final String HIDE_OR_SHOW_DATABASE = "HIDE_OR_SHOW_DATABASE";
  public static final String HIDE_OR_SHOW_LOCATION = "HIDE_OR_SHOW_LOCATION";
  public static final String HIDE_OR_SHOW_ANNOTATIONS = "HIDE_OR_SHOW_ANNOTATIONS";
  public static final String OPEN_FILE_CHOOSER = "OPEN_FILE_CHOOSER";
  
  public static final String USE_STABLE_ID_LOCATION = "USE_STABLE_ID_LOCATION";
  public static final String USE_SEQ_REGION_LOCATION = "USE_SEQ_REGION_LOCATION";

  public static final String FIND_ENSEMBL_DATABASE_NAMES = "FIND_ENSEMBL_DATABASE_NAMES";

  public static final String FIND_COORD_SYSTEMS = "FIND_COORD_SYSTEMS";
  public static final String FIND_SEQ_REGIONS = "FIND_SEQ_REGIONS";
  public static final String FIND_START_ENDS = "FIND_START_ENDS";
  
  public static final String CHANGE_DATABASE = "CHANGE_DATABASE";
  public static final String CLEAR_SEQ_REGIONS = "CLEAR_SEQ_REGIONS";
  
  public static final String SELECT_SEQ_REGION = "SELECT_SEQ_REGION";
  
  public static final String LOAD_SEQ_REGION_BY_HISTORY = "LOAD_SEQ_REGION_BY_HISTORY";

  public static final String SELECT_NEW_ENSEMBL_DATABASE = "SELECT_NEW_ENSEMBL_DATABASE";
  public static final String SHOW_GENE_COUNTS_BY_TYPE = "SHOW_GENE_COUNTS_BY_TYPE";
  public static final String SHOW_DNA_PROTEIN_COUNTS_BY_TYPE = "SHOW_DNA_PROTEIN_COUNTS_BY_TYPE";
  public static final String SHOW_DNA_DNA_COUNTS_BY_TYPE = "SHOW_DNA_DNA_COUNTS_BY_TYPE";
  public static final String SHOW_SIMPLE_FEATURE_COUNTS_BY_TYPE = "SHOW_SIMPLE_FEATURE_COUNTS_BY_TYPE";
  public static final String SHOW_DITAG_FEATURE_COUNTS_BY_TYPE = "SHOW_DITAG_FEATURE_COUNTS_BY_TYPE";
  public static final String SHOW_AB_INITIO_COUNTS_BY_TYPE = "SHOW_AB_INITIO_COUNTS_BY_TYPE";
  
  public Controller(Model model, View view) {
    _model = model;
    _view = view;
    createHandlers();
  }
  
  public Model getModel(){
    return _model;
  }
  
  public View getView(){
    return _view;
  }
  
  public void addKeyRouter(
    JTextField textField,
    String  handlerKey
  ){
    textField.addKeyListener(new KeyEventRouter(this, handlerKey));
  }
  
  public void addKeyRouter(
    Component component,
    String  handlerKey
  ){
    component.addKeyListener(new KeyEventRouter(this, handlerKey));
  }
  
  public void addKeyRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    dropDown.addKeyListener(new KeyEventRouter(this, handlerKey));
  }
  
  public void addActionRouter(
    JButton button,
    String  handlerKey
  ){
    button.addActionListener(new ActionEventRouter(this, handlerKey));
  }
  
  public void addActionRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    dropDown.addActionListener(new ActionEventRouter(this, handlerKey));
  }
  

  public void addPopupRouter(
    JComboBox dropDown,
    String  handlerKey
  ){
    dropDown.addPopupMenuListener(new PopupRouter(this, handlerKey));
  }
  
  private void createHandlers(){
    getHandlers().put(READ, new ReadHandler(this, READ));
    getHandlers().put(UPDATE, new UpdateHandler(this, UPDATE));
    
    getHandlers().put(HIDE_OR_SHOW_TYPES, new HideShowTypesHandler(this, HIDE_OR_SHOW_TYPES));
    getHandlers().put(HIDE_OR_SHOW_OPTIONS, new HideShowOptionsHandler(this, HIDE_OR_SHOW_OPTIONS));
    
    getHandlers().put(HIDE_OR_SHOW_DATABASE , new HideShowDatabasesHandler(this, HIDE_OR_SHOW_DATABASE));
    getHandlers().put(HIDE_OR_SHOW_ANNOTATIONS , new HideShowAnnotationsHandler(this, HIDE_OR_SHOW_ANNOTATIONS));
    
    getHandlers().put(HIDE_OR_SHOW_LOCATION , new HideShowLocationsHandler(this, HIDE_OR_SHOW_LOCATION));

    getHandlers().put(USE_STABLE_ID_LOCATION, new UseStableIDLocationHandler(this, USE_STABLE_ID_LOCATION));
    getHandlers().put(USE_SEQ_REGION_LOCATION, new UseSeqRegionLocationHandler(this, USE_SEQ_REGION_LOCATION));

    getHandlers().put(CHANGE_DATABASE, new ChangeDatabaseHandler(this, CHANGE_DATABASE));
    getHandlers().put(FIND_ENSEMBL_DATABASE_NAMES, new FindEnsemblDatabaseNamesHandler(this, FIND_ENSEMBL_DATABASE_NAMES));
    
    getHandlers().put(FIND_COORD_SYSTEMS, new FindCoordSystemsHandler(this, FIND_COORD_SYSTEMS));
    getHandlers().put(FIND_SEQ_REGIONS, new FindSeqRegionsHandler(this, FIND_SEQ_REGIONS));
    getHandlers().put(CLEAR_SEQ_REGIONS, new ClearSeqRegionsHandler(this, CLEAR_SEQ_REGIONS));

    getHandlers().put(LOAD_SEQ_REGION_BY_HISTORY, new LoadSeqRegionByHistoryHandler(this, LOAD_SEQ_REGION_BY_HISTORY));

    getHandlers().put(SELECT_SEQ_REGION, new SelectSeqRegionHandler(this, SELECT_SEQ_REGION));
    getHandlers().put(SELECT_NEW_ENSEMBL_DATABASE, new SelectNewEnsemblDatabaseHandler(this, SELECT_NEW_ENSEMBL_DATABASE));
    
    getHandlers().put(SHOW_GENE_COUNTS_BY_TYPE, new ShowGeneCountsByTypeHandler(this, SHOW_GENE_COUNTS_BY_TYPE));
    getHandlers().put(SHOW_DNA_PROTEIN_COUNTS_BY_TYPE, new ShowDnaProteinAlignCountsByTypeHandler(this, SHOW_DNA_PROTEIN_COUNTS_BY_TYPE));
    getHandlers().put(SHOW_DNA_DNA_COUNTS_BY_TYPE, new ShowDnaDnaAlignCountsByTypeHandler(this, SHOW_DNA_DNA_COUNTS_BY_TYPE));
    getHandlers().put(SHOW_SIMPLE_FEATURE_COUNTS_BY_TYPE, new ShowSimpleFeatureCountsByTypeHandler(this, SHOW_SIMPLE_FEATURE_COUNTS_BY_TYPE));
    getHandlers().put(SHOW_DITAG_FEATURE_COUNTS_BY_TYPE, new ShowDitagFeatureCountsByTypeHandler(this, SHOW_DITAG_FEATURE_COUNTS_BY_TYPE));
    getHandlers().put(SHOW_AB_INITIO_COUNTS_BY_TYPE, new ShowAbInitioCountsByTypeHandler(this, SHOW_AB_INITIO_COUNTS_BY_TYPE));
  }
  
  private EventHandler getHandler(String handlerKey){
    EventHandler handler = (EventHandler)getHandlers().get(handlerKey);
    if(handler == null){
      throw new FatalException("EventHandler not found for key: "+handlerKey);
    }
    return handler;
  }
  
  HashMap getHandlers(){
    return _handlers;
  }

  public void handleEventForKey(String key){
    log("Received a request for "+key+" event...");
    
    if(isBusyHandlingEvent()){
      log("...ignoring because we're busy");
      return;
    }
    
    log("...processing event for key: "+key);
    try{
      
      setBusyHandlingEvent(true);
      getHandler(key).doAction(getModel());

    }catch(NonFatalException exception){
      
      log("NonFatalException: "+exception.getMessage());
      exception.printStackTrace();
      displayMessage(exception.getMessage());
      
    }catch(FatalException exception){
      
      log("FatalException: "+exception.getMessage());
      exception.printStackTrace();
      displayMessage(exception.getMessage());
      
    }finally{
      
      log("Finished processing event for key: "+key);
      setBusyHandlingEvent(false);

    }
  }
  
  public void displayMessage(String message){
    getView().displayMessage(message);
  }
  
  public void doRead(){
    log("Reading view");
    getView().read(getModel());
    log("Finished Reading view");
  }

  public void doUpdate(){
    log("Updating view");
    getView().update(getModel());
    log("Finished Updating view");
  }
  
  private boolean isBusyHandlingEvent(){
    return _isBusyHandlingEvent;
  }
  
  private void setBusyHandlingEvent(boolean value){
    _isBusyHandlingEvent = value;
  }
  
  public void log(String message){
    //System.out.println(message);
    logger.debug(message);
  }
}
