package apollo.dataadapter.synteny.builder;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj.EnsJAdapterUtil;
import apollo.dataadapter.debug.*;
import apollo.dataadapter.ensj.DataModelConversionUtil;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.*;
import org.ensembl.util.PropertiesUtil;

import org.apache.log4j.*;

public class BuilderAdapter extends AbstractApolloAdapter {

  protected final static Logger logger = LogManager.getLogger(BuilderAdapter.class);

  private MultiProperties stateInformation = new MultiProperties();
  private BuilderAdapterGUI gui;
  
  public BuilderAdapter(){
  }//end BuilderAdapter
  
  public Model getModel(){
    if(gui != null){
      return gui.getController().getModel();
    }else{
      return Model.fromXML();
    }
  }

  public String getName() {
    return "Builder";
  }

  public String getType() {
    return "Builder Adapter";
  }

  public IOOperation [] getSupportedOperations() {
    return new IOOperation[] {
       ApolloDataAdapterI.OP_READ_DATA
    };
  }

  public DataAdapterUI getUI(IOOperation op) {
    if(gui == null){
      gui = new BuilderAdapterGUI(op, getModel());
    }
    
    return gui;
  }

  /**
   * This stores the input properties on the adapter. It also
   * transmits any input properties - via a direct setStateInformation call -
   * to all child adapters.
  **/
  public void setStateInformation(Properties properties){
    stateInformation.putAll(properties);
    String key;
    Model model = getModel();
    Iterator adapterKeys = model.getAdapterMap().keySet().iterator();
    HashMap adapters = model.getAdapterMap();
    Properties childProperties;
    String region = properties.getProperty(StateInformation.REGION);

    logger.debug("Builder adapter running set state info with input props: "+properties);
    logger.debug(" -- found region: "+region);

    //
    //If the main (parent) state has a 'region' then copy that into each child adapter state.
    logger.debug("region: "+region);
        
    if(properties instanceof MultiProperties){
      logger.debug("set using multiprops being propagated to kids");
      while(adapterKeys.hasNext()){
        key = (String)adapterKeys.next();
        childProperties = ((MultiProperties)properties).getProperties(key);
        if(region != null && region.trim().length() > 0){
          childProperties.setProperty(StateInformation.REGION, region);
        }
        logger.debug("propagating: "+key+" values: "+childProperties);
        ((AbstractApolloAdapter)adapters.get(key)).setStateInformation(childProperties);
      }
    }
    
  }//end setStateInformation

  public void init() {
  }

  private boolean getStateAsBoolean(Properties stateInfo, String type){
    return PropertiesUtil.booleanValue(stateInfo, type, false);
  }


  public CurationSet getCurationSet() throws ApolloAdapterException{
    String key;
    Iterator keys = getModel().getAdapterMap().keySet().iterator();
    AbstractApolloAdapter adapter;
    Properties childState = new StateInformation();
    CurationSet returnSet;
    String selectedAdapterSet = getModel().getSelectedAdapterSet();
    String mainAdapterKey = getModel().getMainAdapterModel(selectedAdapterSet).getName();
    String chromosome;
    int low;
    int high;
    String region;
    CurationSet childSet;
    boolean setRegionOnChildren = false;
    Model model = getModel();
    Properties childProperties;

    clearOldData();
    
    adapter = (AbstractApolloAdapter)model.getAdapterMap().get(mainAdapterKey);
    returnSet = adapter.getCurationSet();
    region = getStateInformation().getProperty(StateInformation.REGION);

    if (getStateAsBoolean(adapter.getStateInformation(), StateInformation.RESET_GENE_START_AND_STOP)) {
      logger.info("Resetting translation starts and stops to match apollo conventions");
      DataModelConversionUtil.resetTranslationStartsAndStops(returnSet.getResults());
      DataModelConversionUtil.resetTranslationStartsAndStops(returnSet.getAnnots());
    }

    // Save region (possibly after stable id to location conversion) onto 
    // main adapter location history if it has one
    // NOTE: Currently the builder adapter gui will destroy the changes
    //       made here if it is the caller, but the nav bar will not, and its the nav bar ones I want to save.
    Properties propertiesFromMainAdapter = adapter.getStateInformation();
    if (propertiesFromMainAdapter.getProperty("locationHistory0") != null) {
      String currentLocation = 
        returnSet.getChromosome()+":"+
        String.valueOf(returnSet.getStart())+"-"+
        String.valueOf(returnSet.getEnd());
      
      logger.debug("Current location = " + currentLocation);
      List locationHistory = EnsJAdapterUtil.getPrefixedProperties(propertiesFromMainAdapter,"locationHistory");
      if(!currentLocation.equals("null:null:-") && !locationHistory.contains(currentLocation)){
        locationHistory.add(0, currentLocation);
      }
      EnsJAdapterUtil.putPrefixedProperties(propertiesFromMainAdapter,locationHistory,"locationHistory");

      model.getModelWithName(selectedAdapterSet, mainAdapterKey).setProperties(propertiesFromMainAdapter);
      model.toXML();
    }

    logger.debug("region = " + region);

    if((region == null || region.startsWith("ID")) && 
      returnSet.getChromosome() != null &&
      returnSet.getStart() > 0 &&
      returnSet.getEnd() > 0
    ){
      setRegionOnChildren = true;
      region = 
        "Chr "+
        returnSet.getChromosome()+" "+
        String.valueOf(returnSet.getStart())+" "+
        String.valueOf(returnSet.getEnd());
      
      childState.setProperty(StateInformation.REGION, region);
      
      logger.warn("Builder adapter getCurationSet finds region null - created region: "+region);
    }
    


    while(keys.hasNext()){
      key = (String)keys.next();
      adapter = (AbstractApolloAdapter)model.getAdapterMap().get(key);
      if(!key.equals(mainAdapterKey)){
        if(setRegionOnChildren){
          logger.debug("Setting region on child: "+key+" -- "+childState);

          childProperties = adapter.getStateInformation();
	  childProperties.setProperty(StateInformation.REGION, region);
          adapter.setStateInformation(childProperties);
        }
        childSet = adapter.getCurationSet();
        copyFeaturesFromChildToParent(childSet, returnSet);

        // Reason for doing this here is so sequence is changed to parent (other dbs might not have real sequence)
        if (getStateAsBoolean(adapter.getStateInformation(), StateInformation.RESET_GENE_START_AND_STOP)) {
          logger.debug("Resetting translation starts and stops to match apollo conventions");
          DataModelConversionUtil.resetTranslationStartsAndStops(childSet.getResults());
          DataModelConversionUtil.resetTranslationStartsAndStops(childSet.getAnnots());
        }

      }
    }


    return returnSet;


  }//end getCurationSet


  public DataInputType getInputType(){
    return DataInputType.FILE;
  }
  
  public Properties getStateInformation() {
    return stateInformation;
  }
  
  /**
   * This method has to pass the setRegion call onto each child adapter.
  **/
  public void setRegion(String region) throws ApolloAdapterException {
    this.region = region;
    Iterator adapters = getModel().getAdapterMap().values().iterator();
    while(adapters.hasNext()){
      ((AbstractApolloAdapter)adapters.next()).setRegion(region);
    }
  }

  private void copyFeaturesFromChildToParent(CurationSet child, CurationSet parent){
    logger.info("copying features from child to parent");
    Vector from = child.getResults().getFeatures();
    for(int i=0; i<from.size(); i++){
      SeqFeatureI sf = (SeqFeatureI)from.get(i);

      sf.setRefSequence(parent.getResults().getRefSequence());


      //System.out.println("Adding feature: "+sf.getName()+" -- "+sf.getNumberOfChildren());
      parent.getResults().addFeature(sf, false);
    }

    
    from = child.getAnnots().getFeatures();
    for(int i=0; i<from.size(); i++){
      SeqFeatureI sf = (SeqFeatureI)from.get(i);

      sf.setRefSequence(parent.getAnnots().getRefSequence());

      parent.getAnnots().addFeature(sf,false);
    }
  }

  
  public void clearStateInformation() {
    stateInformation = new MultiProperties();
  }  

  
}
