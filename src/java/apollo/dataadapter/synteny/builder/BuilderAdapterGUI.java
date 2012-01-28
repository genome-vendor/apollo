package apollo.dataadapter.synteny.builder;

import org.bdgp.swing.AbstractDataAdapterUI;
import java.util.Properties;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import apollo.datamodel.CurationSet;
import java.awt.*;
import java.awt.event.*;
import apollo.dataadapter.*;
import apollo.datamodel.*;
import javax.swing.*;
import java.util.*;
import org.bdgp.util.MultiProperties;
import apollo.util.GuiUtil;

import org.apache.log4j.*;

public class BuilderAdapterGUI extends AbstractDataAdapterUI implements ApolloDataAdapterGUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BuilderAdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Properties properties;
  private IOOperation operation;
  private Controller controller;
  private View view;
  
  public class ModelChangeListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      logger.warn("Model changed - adapter gui isn't doing anything!");
      //BuilderAdapterGUI.this.resetChildProperties();
    }    
  }
  
  public BuilderAdapterGUI(IOOperation operation, Model model) {
    this.operation = operation;
    view = new View();
    controller = new Controller(model, view);
    view.setController(controller);
    view.initialiseGUI();

    getController().addActionListener(new ModelChangeListener());

    buildGUI();
    //by the time we're creating this, the model is KNOWN - passed in - 
    //so we can interrogate it to see if there's a selected adapterSet.
    String set = model.getSelectedAdapterSet();
    if(set != null){
    	model.createAdaptersAndGUIs(set);
    }

/*    Iterator sets = model.getAdapterSets().keySet().iterator();
    String selectedSet = null; 
    if(sets.hasNext()){
      selectedSet = (String)sets.next();
    }
    model.setSelectedAdapterSet(selectedSet);
    getController().handleEvent(Controller.DROPDOWN_SELECT_SET);*/
    view.read(model);
  }

  public void resetChildProperties(){
    setChildProperties();
  }
  
  public void setChildProperties(){
    String key;
    Model model = getController().getModel();
    Iterator guiKeys = model.getAdapterGUIMap().keySet().iterator();
    HashMap guis = model.getAdapterGUIMap();
    String selectedAdapterSet = model.getSelectedAdapterSet();
    Properties propertiesFromModel;
    
    while(guiKeys.hasNext()){
      key = (String)guiKeys.next();
      propertiesFromModel = model.getModelWithName(selectedAdapterSet, key).getProperties();
      ((AbstractDataAdapterUI)guis.get(key)).setProperties(propertiesFromModel);
    }
  }
  
  public void getChildProperties(){
    String key;
    Model model = getController().getModel();
    Iterator guiKeys = model.getAdapterGUIMap().keySet().iterator();
    Properties propertiesFromAdapterGUI;
    String selectedAdapterSet = model.getSelectedAdapterSet();

    while(guiKeys.hasNext()){
      key = (String)guiKeys.next();
      propertiesFromAdapterGUI = ((AbstractDataAdapterUI)model.getAdapterGUIMap().get(key)).getProperties();
      model.getModelWithName(selectedAdapterSet, key).setProperties(propertiesFromAdapterGUI);
      model.toXML();
    }
  }
  
  public Properties createStateInformation() throws ApolloAdapterException{
    String adapterKey;
    String selectedAdapterSet = getController().getModel().getSelectedAdapterSet();
    Iterator adapterKeys = getController().getModel().getAdapterModels(selectedAdapterSet).iterator();
    ApolloDataAdapterGUI adapterGUI;
    MultiProperties compositeProperties = new MultiProperties();
    Properties stateInformation;
    String mainAdapterKey = getController().getModel().getMainAdapterModel(selectedAdapterSet).getName();
    String region;
    
    while(adapterKeys.hasNext()){
      adapterKey = ((AdapterModel)adapterKeys.next()).getName();

      logger.debug("Child adapter key: "+adapterKey);

      adapterGUI = 
        (ApolloDataAdapterGUI)getController()
          .getModel()
          .getAdapterGUIMap()
          .get(adapterKey);

      logger.debug("Child adapter gui class: "+adapterGUI.getClass().getName());

      stateInformation = adapterGUI.createStateInformation();
      
      logger.debug("Child adapter state info: "+stateInformation);

      compositeProperties.setProperties(
        adapterKey, 
        stateInformation
      );
      
      if(adapterKey.equals(mainAdapterKey)){
        region = stateInformation.getProperty(StateInformation.REGION);
        if(region != null){
          compositeProperties.setProperty(StateInformation.REGION, region);
        }
      }
    }

    return compositeProperties;
  }

  public Controller getController(){
    return controller;
  }

  public View getView(){
    return view;
  }

  private void buildGUI() {
    setLayout(new BorderLayout());
    add(getView(), BorderLayout.CENTER);
  }//end buildGUI
  
  public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException {
    CurationSet curationSet = null;
    
    BuilderAdapter adapter = ((BuilderAdapter)getDataAdapter());
    String adapterKey;
    
    CurationSet returnSet = new CurationSet();
    
    Properties stateInformation;
    String region;
    
    AbstractApolloAdapter childAdapter;
    ApolloDataAdapterGUI childAdapterGUI;
    HashMap adapterMap = getController().getModel().getAdapterMap();
    MultiProperties compositeProperties;
    
    try{
      
      if (getOperation().equals(ApolloDataAdapterI.OP_READ_DATA)){
        
        compositeProperties = (MultiProperties)createStateInformation();
        
        ((AbstractApolloAdapter)getDataAdapter()).clearStateInformation();
        ((AbstractApolloAdapter)getDataAdapter()).setStateInformation(compositeProperties);

        returnSet = ((AbstractApolloAdapter)getDataAdapter()).getCurationSet();
        
        return returnSet;
        
      }else{
        
        throw new apollo.dataadapter.ApolloAdapterException(
          "This adapter cannot be used to write data"
        );
        
      }//end if

    }catch(NonFatalDataAdapterException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }//end try
    
  }//end doOperation

  public void setProperties(Properties input) {
    properties = input;
    setChildProperties();
  }

  public Properties getProperties() {
    Properties properties = new Properties();
    getChildProperties();
    return properties;
  }

  public DataAdapter getDataAdapter(){
    return driver;
  }

  private IOOperation getOperation(){
    return operation;
  }//end getOperation

}
