package apollo.dataadapter.synteny;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.lang.reflect.*;

import org.apache.log4j.*;

import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.*;

import apollo.dataadapter.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.controller.*;
import apollo.datamodel.*;
import apollo.gui.*;
import apollo.config.Config;
import apollo.config.Style;
import apollo.gui.ProxyDialog;

public class SyntenyAdapterGUI extends AbstractDataAdapterUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SyntenyAdapterGUI.class);
  private final static String speciesNumberPropName = "SyntenySpeciesNumber";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private Map adapterGUIs = new HashMap();

  private java.util.List adapterLabelList;
  
  private HashMap sharedData = new HashMap();
  protected SyntenyAdapter driver;
  
  protected IOOperation op;

  private Properties props;

  private JButton fullPanelButton;
  
  private FullEnsJDBCSyntenyPanel thePanel;
  
  private JTabbedPane tabbedPane;

  /** Dropdown list of how many species to query for - max is configged */
  private JComboBox speciesNumberDropdownList;
  

  public SyntenyAdapterGUI(IOOperation op, SyntenyAdapter adapter) {
    this.op = op;
    driver = adapter;
    setAdapterLabelList(buildAdapterLabelList());
    buildGUI();
  }

  /**
   * This sets the SyntenyAdapter as the dataadapter for this GUI. We must
   * propagate this relation down to the children, otherwise none of the
   * GUI's in the child adapters will be connected to the child dataadapers.
  **/
  public void setDataAdapter(DataAdapter driver) {
    Iterator names = getAdapter().getAdapters().keySet().iterator();
    String speciesName;
    AbstractApolloAdapter theAdapter;
    org.bdgp.io.DataAdapterUI theAdapterUI;
    
    this.driver = (SyntenyAdapter)driver;
    
    while(names.hasNext()){
      speciesName = (String)names.next();
      theAdapter = (AbstractApolloAdapter)getAdapter().getAdapters().get(speciesName);
      theAdapterUI = (org.bdgp.io.DataAdapterUI)getAdapterGUIs().get(speciesName);
      theAdapterUI.setDataAdapter(theAdapter);
    }//end while
    
  }//end setDataAdapter
  
  /**
   * <p>This is called specifically by helpers (like the FullEnsJDBCSyntenyPanel)
   * which need to set values directly into the widgets in its children.
   * -- The point is that we get fed something (currently a SyntenyRegion) from
   * the our invoker (currently the SyntenyMenu). We find the adapter gui's 
   * relevant to the chromosomes on the region and setInput on them, passing
   * in a HashMap of text-field values relevant to each.</p>
   *
   * <p> For instance, when given ranges on Human and Mouse, we will set
   * the Human ranges on the Human adapter, the Mouse ranges on the Mouse
   * adapter, and both ranges on the Human/Mouse adapter. </p>
   *
   * If we get called with anything other than a HashMap, we do nothing.
  **/
  public void setInput(Object input){
    Iterator iterator = getAdapter().getAdapters().values().iterator();
    AbstractDataAdapterUI theUI;
    SyntenyRegion region;
    HashMap nextInput = new HashMap();
    String textValue;
    String logicalQuerySpecies;
    String logicalHitSpecies;
    Iterator adapterNames;
    String adapterName;
    String shortAdapterName;
    int index;
    String comparaName;
    String reverseComparaName;
    
    if(!(input instanceof HashMap)){
      return;
    }
    
    logicalQuerySpecies = (String)((HashMap)input).get("logicalQuerySpecies");
    logicalHitSpecies = (String)((HashMap)input).get("logicalHitSpecies");
    comparaName = logicalQuerySpecies+"-"+logicalHitSpecies;
    reverseComparaName = logicalHitSpecies+"-"+logicalQuerySpecies;
    
    region = (SyntenyRegion)((HashMap)input).get("region");
    
    if(region == null){
      return; //we're not interested if we're passed a null region.
    }

    adapterNames = getAdapter().getAdapters().keySet().iterator();
    
    while(adapterNames.hasNext()){
      adapterName = (String)adapterNames.next();
      //
      //If we match an adapter for the query or hit-species,
      //then set in the range information into the adapter.
      //If we match the compara name (species1-species2) then
      //set up query and hit input simultaneously
      if (adapterName.equals(logicalQuerySpecies)) {

        logger.debug("Setting input on query species (" + logicalQuerySpecies + ")");
        nextInput.clear();
        nextInput.put("coordsys", (String)region.getChromosome1().getCoordSystem());
        nextInput.put("chr", (String)region.getChromosome1().getDisplayId());
        nextInput.put("start", String.valueOf(region.getStart1()));
        nextInput.put("end", String.valueOf(region.getEnd1()));
        
        theUI = (AbstractDataAdapterUI) getAdapterGUIs().get(adapterName);
        theUI.setInput(nextInput);

      } else if(adapterName.equals(logicalHitSpecies)) {

        logger.debug("Setting input on hit species (" + logicalHitSpecies + ")");
        nextInput.clear();
        nextInput.put("coordsys", (String)region.getChromosome2().getCoordSystem());
        nextInput.put("chr", (String)region.getChromosome2().getDisplayId());
        nextInput.put("start", String.valueOf(region.getStart2()));
        nextInput.put("end", String.valueOf(region.getEnd2()));
        
        theUI = (AbstractDataAdapterUI) getAdapterGUIs().get(adapterName);
        theUI.setInput(nextInput);

      } else if (adapterName.equals(comparaName) ||
                 adapterName.equals(reverseComparaName)){

        nextInput.clear();
        nextInput.put("chr", (String)region.getChromosome1().getDisplayId());
        nextInput.put("start", String.valueOf(region.getStart1()));
        nextInput.put("end", String.valueOf(region.getEnd1()));
        nextInput.put("hitChr", (String)region.getChromosome2().getDisplayId());
        nextInput.put("hitStart", String.valueOf(region.getStart2()));
        nextInput.put("hitEnd", String.valueOf(region.getEnd2()));
        
        theUI = (AbstractDataAdapterUI) getAdapterGUIs().get(adapterName);
        logger.debug("Setting input on compara (" + comparaName + ") adaptor UI " + theUI);
        theUI.setInput(nextInput);
      }
    }
  }

 

  /**
   * Gather properties for each child adapter - prefix each gathered property
   * with the name of the species (so, "myProp" -> "Homo_sapiens:myProp")
   * and then glue all child Properties into one big Properties. Hand out!
  **/
  public Properties getProperties(){
    Properties returnedProperties = new Properties();
    Properties childProperties;
    Properties combinedChildProperties = new Properties();
    Iterator names = getAdapter().getAdapters().keySet().iterator();
    String speciesName;
    Iterator childPropertyNames;
    String childPropertyName;
    
    // if we have radio query buttons add state(1 or 2) to props
    if (addNumSpeciesDropdown()) {
      String state = speciesNumberDropdownList.getSelectedItem().toString();
      combinedChildProperties.put(speciesNumberPropName,state);
    }

    while(names.hasNext()){
      speciesName = (String)names.next();
      
      childProperties = 
        ((AbstractDataAdapterUI)
          getAdapterGUIs().get(speciesName)
        ).getProperties();
      
      childPropertyNames = childProperties.keySet().iterator();
      
      while(childPropertyNames.hasNext()){
        childPropertyName = (String)childPropertyNames.next();
        
        combinedChildProperties.setProperty(
          speciesName+":"+childPropertyName, 
          childProperties.getProperty(childPropertyName)
        );
      }//end while
    }//end while
    
    return combinedChildProperties;
  }//end getProperties
    
  /**
   * <p> Walk each property we've been handed. Strip off the species name at the
   * front of the property, and gather the properties into species-specific
   * groups. </p>
   *
   * <p> Add to these properties the configuration information in the synteny
   * style file for this species (in particular, we'll throw in the query- and
   * hit-species for the compara-adapters we load into the properties list)</p>
   *
   * <p> Call setProperties() on each species' adapter, 
   * passing in the specific groups we've gathered.</p>
  **/
  public void setProperties(Properties combinedProperties){
    String combinedPropertyName;
    Iterator combinedPropertyNames = combinedProperties.keySet().iterator();
    String propertyValue;
    String speciesName;
    String childPropertyName;
    Properties childProperty;
    HashMap childPropertiesMap = new HashMap();
    HashMap syntenyStyleSpeciesProperties;
    int index;
    Iterator adaptorNames;
    Iterator styleKeyIterator;
    String styleKey;
    String styleValue;

    // if we have link query buttons(game) check if state in props and set radio
    if (addNumSpeciesDropdown()) {
      String numSpeciesStr = combinedProperties.getProperty(speciesNumberPropName);
      if (numSpeciesStr != null) {
        int numSpecies = new Integer(numSpeciesStr).intValue();
        // make sure history doesn exceed configged max (config couldve changed)
        if (numSpecies > getConfiggedNumberOfSpecies())
          numSpecies = getConfiggedNumberOfSpecies();
        if (numSpecies > 0)
          speciesNumberDropdownList.setSelectedIndex(numSpecies-1);
      }
    }
    
    // combined properties have a ":" in the middle of their key string(name)
    while(combinedPropertyNames.hasNext()){
      combinedPropertyName = (String)combinedPropertyNames.next();
      propertyValue = (String)combinedProperties.get(combinedPropertyName);
      index = combinedPropertyName.indexOf(":");
      
      if(index > 0){
        //
        //split out the species name from the property key.
        speciesName = combinedPropertyName.substring(0,index);
        childPropertyName = combinedPropertyName.substring(index+1);
        childProperty = (Properties)childPropertiesMap.get(speciesName);

        //
        //dig out our child Properties from the temporary map. Create if we 
        //have to.
        if(childProperty == null){
          childProperty = new Properties();
          childPropertiesMap.put(speciesName, childProperty);
        }//end if

        childProperty.put(childPropertyName, propertyValue);
      }//end if
    }//end while

    syntenyStyleSpeciesProperties = 
      Config
        .getStyle("apollo.dataadapter.synteny.SyntenyAdapter")
        .getSyntenySpeciesProperties();

    //now set each child property we've set up into its respective adapter.
    adaptorNames = getAdapter().getAdapters().keySet().iterator();
    
    
    while(adaptorNames.hasNext()){
      speciesName = (String)adaptorNames.next();

      childProperty = (Properties)childPropertiesMap.get(speciesName);
      
      //
      //If you've got historical input data for this species, then use it,
      //otherwise proceed with a new Properties.
      if(childProperty == null){
        childProperty = new Properties();
      }

      styleKeyIterator = syntenyStyleSpeciesProperties.keySet().iterator();
      while(styleKeyIterator.hasNext()){
        styleKey = (String)styleKeyIterator.next();

        //e.g., if the entry species.Human-Mouse.querySpecies => Homo_sapiens
        //contains the string Human-Mouse, place the entry 
        //querySpecies => Homo_sapiens into the properties file.
        if(styleKey.indexOf(speciesName)>0){
          index = styleKey.lastIndexOf(".");
          childProperty.put(
            styleKey.substring(index+1),
            syntenyStyleSpeciesProperties.get(styleKey)
          );
        }//end if
      }//end if

      ((AbstractDataAdapterUI)
        getAdapterGUIs().get(speciesName)
      ).setProperties(childProperty);
    }//end while
  }
  
  /** 
   * The order of the adapters is given by the synteny style's
   * syntenySpeciesOrder vector.
   This returns a list of labels for the adapter (Species1,Species2...) NOT a list
   of adapters themselves - rename
   **/ // Why did we lose the order in the first place?
  private List buildAdapterLabelList() {
    int index;
    List syntenySpeciesOrder = getSyntenyStyle().getSyntenySpeciesAndLinkOrder();
    
    List newList = new ArrayList();

    for(int i=0; i<syntenySpeciesOrder.size(); i++){
      index = ((String)syntenySpeciesOrder.get(i)).lastIndexOf(".");
      newList.add(((String)syntenySpeciesOrder.get(i)).substring(index+1));
    }
    return newList;
  }

  private Style getSyntenyStyle() {
    return Config.getStyle("apollo.dataadapter.synteny.SyntenyAdapter");
  }
  
  /**
   * We need to post-process this composite curation set. 
   * Right now, protein homology information
   * is being provided with no "extra" redundant information - namely, the positions
   * of the homologous genes are no longer being stored with the gene pairs, but we 
   * have already retrieved these genes in the individual species' curation sets, so we
   * will fill-in the feature pairs' position information with the gene info we
   * already know.
   Changed this so it always returns a CompositeDataHolder now. Presently for gff and
   ensj it makes a composite cur set and then makes a comp data holder from that. 
   Eventually they should just make a comp data holder from scratch.
  **/
  public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException {
    boolean actNaively = getAdapter().getStyle().getNaiveCrossSpeciesDataLoading();
    CurationSet masterCurationSet = new CurationSet();
    CurationSet childCurationSet = null;
    Iterator adapterNames = null;
    String speciesName = null;
    DataAdapterUI adapterUI = null;
    CompositeDataHolder compositeDataHolder;
    

    // Override styles of child adapters to synteny styles 
    getAdapter().setChildAdapterStyles();

    // WRITE
    if(getOperation().equals(ApolloDataAdapterI.OP_WRITE_DATA)){
      // couldnt/shouldnt this just call adapter.commitChanges?
      // i dont think we can presume its a cdh. fileMenu saveAs saves active cur set
      CompositeDataHolder compData = (CompositeDataHolder)values;
      for (int i=0; i<compData.numberOfSpecies(); i++) {
        childCurationSet = compData.getCurationSet(i);
        adapterUI = (DataAdapterUI)getAdapterGUIs().get(compData.getSpecies(i));
        adapterUI.doOperation(childCurationSet);
      }
      return null;
      
    } 

    // READ
    else {

      // READ NAIVELY - just blindly load the species data and link data provided by
      // user. Presently this is for gff synteny (not ensj or game)
      if(actNaively){ // gff
        adapterNames = getAdapter().getAdapters().keySet().iterator();
        compositeDataHolder = new CompositeDataHolder();
        while(adapterNames.hasNext()){
          speciesName = (String)adapterNames.next();
          adapterUI = (AbstractDataAdapterUI)getAdapterGUIs().get(speciesName);

          childCurationSet = (CurationSet)adapterUI.doOperation(values);
          childCurationSet.setOrganism(speciesName);
          //apollo.dataadapter.debug.DisplayTool.showFeatureSet(childCurationSet.getResults());
          masterCurationSet.addCurationSet(speciesName,childCurationSet);
        }//end while

      } else if (getAdapter().curSetHasSyntenyLinks()) { // game
        
        compositeDataHolder = 
          getAdapter().loadSpeciesThatContainLinks(getNumberOfSpeciesToLoad());

        return compositeDataHolder;
        
      } 

      else { // ensj // if (dataAdapter instanceof EnsJAdapter)
        
        logger.info("adapter.curSetHasSyntenyLinks() returns false. "+
                    "thus im expecting an ensj data adapter...");

        // Read non-naively doing clever retrievals (ensj)
        
        //
        //which is the first adapter, reading left->right,
        //which has enough data in its GUI for us to get going?
        //--meaning a filled in range or an input gene.
        // expects EnsjAdapter
        int startAdapterIndex = findStartAdapterIndex();

        //
        //Do the triplets of adapter-compara-adapters to the
        //"right" of the start adapter.
        findUpstreamCurationSets(startAdapterIndex, masterCurationSet, values);

        //
        //Do the triplets of adapter-compara-adapters to the
        //"left" of the start adapter.
        findDownstreamCurationSets(startAdapterIndex, masterCurationSet, values);

        Vector setNames = new Vector();
        for(int i=0; i<getAdapterLabelList().size(); i+=2){
          setNames.add(getAdapterLabelList().get(i));
        }

        masterCurationSet.setChildSetOrderedNames(setNames);

      }//end if

      //
      //At this point we've gathered all the interesting ranges for all displayed
      //species: requery the compara database, and find the dna-dna and protein-protein
      //hits for each region (now that we've determined the regions). Add these features
      //back into the individual curation sets for each species.
      addComparaFeaturesIntoSingleSpeciesSets(masterCurationSet);
      
      compositeDataHolder = new CompositeDataHolder(masterCurationSet);
      //return masterCurationSet;
      return compositeDataHolder;
    }
  }//end doOperation

  private void addComparaFeaturesIntoSingleSpeciesSets(CurationSet masterCurationSet)
  throws apollo.dataadapter.ApolloAdapterException
  {
    String speciesName = null;
    CurationSet curationSet = null;
    String chromosome;
    int start;
    int end;
    String comparaKey = (String)getAdapterLabelList().get(1);
    AbstractApolloAdapter abstractAdapter = (AbstractApolloAdapter)getAdapter().getAdapters().get(comparaKey);
    SyntenyComparaAdapter adapter = null;
    java.util.List featurePairList = null;
    String doFetch =  null;
    String typesString = null;
    String [] types = null;
    HashMap sequenceHash = new HashMap();//store all the reference sequences by species.

    if(abstractAdapter instanceof SyntenyComparaAdapter){ 
      adapter = (SyntenyComparaAdapter)abstractAdapter; 
      doFetch = adapter.getStateInformation().getProperty("dnaAlignsIntoSingleSpecies");
      typesString = adapter.getStateInformation().getProperty("dnaAlignTypes");
      types = typesString.split(",");
    }
    
    if(
      doFetch != null && 
      doFetch.trim().length() >0 &&
      doFetch.equals("true")//Boolean.toString(true)) jdk 1.3
    ){
      //
      //Prepare a hashmap of all sequences (keyed by species name)
      for(int i=0; i<getAdapterLabelList().size(); i+=2){
        speciesName = (String)getAdapterLabelList().get(i);
        curationSet = masterCurationSet.getCurationSet(speciesName);
        sequenceHash.put(
          adapter.convertLogicalNameToSpeciesName(speciesName), 
          curationSet.getRefSequence()
        );
      }

      //
      //Take every second adapter - it has the single species in it.
      for(int i=0; i<getAdapterLabelList().size(); i+=2){
        speciesName = (String)getAdapterLabelList().get(i);
        curationSet = masterCurationSet.getCurationSet(speciesName);
        chromosome = curationSet.getChromosome();
        start = curationSet.getStart();
        end = curationSet.getEnd();

        featurePairList = new ArrayList();

        for (int j=0;j<types.length;j++) {
          featurePairList.addAll( 
            adapter.fetchAllAlignFeatures(
              speciesName,
              chromosome,
              start,
              end,
              sequenceHash,
              types[j]
            ));
        }

        for(int j=0; j<featurePairList.size(); j++){
          curationSet.getResults().addFeature((SeqFeatureI)featurePairList.get(j)); 
        }//end for
      }//end for
    }
  }//end addComparaFeaturesIntoSingleSpeciesSets
  
  /** Presently this is hard wired for EnsJAdapterGUI */
  private int findStartAdapterIndex() throws apollo.dataadapter.ApolloAdapterException{
    String speciesName;
    EnsJAdapterGUI adapterGUI=null;
    
    for(int i=0; i<getAdapterLabelList().size(); i+=2){
      speciesName = (String)getAdapterLabelList().get(i);
      
      Object adapGuiObj = getAdapterGUIs().get(speciesName);

      if (! (adapGuiObj instanceof EnsJAdapterGUI)) {
        String m = "Error: expecting EnsJAdapterGUI for synteny, got "+adapGuiObj
          +" abandoning ship!";
        ApolloAdapterException aae = new ApolloAdapterException(m);
        logger.error(m, aae);
        throw aae;
      }
      adapterGUI = (EnsJAdapterGUI)adapGuiObj;
      logger.debug("Species " + speciesName);

      // This causes the EnsJAdapterGUI to update its model to match the GUI
      adapterGUI.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.UPDATE);
      
      if(hasRangeInformation(adapterGUI) || hasStableIdInformation(adapterGUI)){
        return i;
      }//end if
    }//end for
    
    throw new 
      apollo.dataadapter.ApolloAdapterException(
        "No species had sufficient starting information - range or gene stable id"
      );
  }//end findStartAdapterIndex

  /**
   * Fill in the curation sets to the "right" of the input index
   * Only works with EnsJAdapterGUIs
  **/
  private void findUpstreamCurationSets(
    int startIndex, 
    CurationSet masterCurationSet,
    Object values    
  ) throws org.bdgp.io.DataAdapterException{
    SyntenyComparaAdapterGUI comparaAdapterGUI;
    EnsJAdapterGUI querySpeciesAdapterGUI;
    EnsJAdapterGUI hitSpeciesAdapterGUI;

    CurationSet comparaCurationSet;
    CurationSet querySpeciesCurationSet;
    CurationSet hitSpeciesCurationSet;

    String querySpeciesName;
    String hitSpeciesName;
    String comparaName;

    for(int i=startIndex; i<getAdapterLabelList().size() - 1; i+=2){
      querySpeciesName = (String)getAdapterLabelList().get(i);
      hitSpeciesName = (String)getAdapterLabelList().get(i+2);
      
      querySpeciesAdapterGUI = (EnsJAdapterGUI)getAdapterGUIs().get(querySpeciesName);
      
      hitSpeciesAdapterGUI = (EnsJAdapterGUI)getAdapterGUIs().get(hitSpeciesName);

      
      comparaName = querySpeciesName+"-"+hitSpeciesName;
      comparaAdapterGUI = (SyntenyComparaAdapterGUI)getAdapterGUIs().get(comparaName);
      
      if(comparaAdapterGUI == null){
        comparaName = hitSpeciesName+"-"+querySpeciesName;
        comparaAdapterGUI = (SyntenyComparaAdapterGUI)getAdapterGUIs().get(comparaName);
      }//end if
      
      if(comparaAdapterGUI == null){
        throw new IllegalStateException(
          "No compara adapter set for species: "+querySpeciesName+"-"+hitSpeciesName
        );
      }//end if
      
      doAppropriateFetchesForInputQueryHitAndComparaAdapters(
        values,
        querySpeciesName,
        querySpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        masterCurationSet
      );
    }//end for
  }//end findUpstreamCurationSets
  
  /**
   * Fill in the curation sets to the "left" of the input index
  **/
  private void findDownstreamCurationSets(
    int startIndex, 
    CurationSet masterCurationSet, 
    Object values
  )throws org.bdgp.io.DataAdapterException{
    SyntenyComparaAdapterGUI comparaAdapterGUI;
    EnsJAdapterGUI querySpeciesAdapterGUI;
    AbstractApolloAdapter comparaAdapter;
    EnsJAdapterGUI hitSpeciesAdapterGUI;

    CurationSet querySpeciesCurationSet;
    CurationSet hitSpeciesCurationSet;

    String querySpeciesName;
    String hitSpeciesName;
    String comparaName;

    for(int i=startIndex; i>=2; i-=2){
      querySpeciesName = (String)getAdapterLabelList().get(i-2);
      hitSpeciesName = (String)getAdapterLabelList().get(i);
      
      querySpeciesAdapterGUI = (EnsJAdapterGUI)getAdapterGUIs().get(querySpeciesName);
      
      hitSpeciesAdapterGUI = (EnsJAdapterGUI)getAdapterGUIs().get(hitSpeciesName);
      
      comparaName = querySpeciesName+"-"+hitSpeciesName;
      comparaAdapterGUI = (SyntenyComparaAdapterGUI)getAdapterGUIs().get(comparaName);
      
      if(comparaAdapterGUI == null){
        comparaName = hitSpeciesName+"-"+querySpeciesName;
        comparaAdapterGUI = (SyntenyComparaAdapterGUI)getAdapterGUIs().get(comparaName);
      }
      
      if(comparaAdapterGUI == null){
        throw new IllegalStateException(
          "No compara adapter set for species: "+querySpeciesName+"-"+hitSpeciesName
        );
      }
      
      doAppropriateFetchesForInputQueryHitAndComparaAdapters(
        values,
        querySpeciesName,
        querySpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        masterCurationSet
      );
    }//end for
  }//end findDownstreamCurationSets
  
  private void doAppropriateFetchesForInputQueryHitAndComparaAdapters(
    Object values,
    String querySpeciesName,
    EnsJAdapterGUI querySpeciesAdapterGUI,
    String comparaName,
    SyntenyComparaAdapterGUI comparaAdapterGUI,
    String hitSpeciesName,
    EnsJAdapterGUI hitSpeciesAdapterGUI,
    CurationSet masterCurationSet
  ) throws org.bdgp.io.DataAdapterException{
    if(hasRangeInformation(querySpeciesAdapterGUI) && hasRangeInformation(hitSpeciesAdapterGUI)){
      doTwoSidedFetch(
        values,
        querySpeciesName,
        querySpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        masterCurationSet
      );
    }else if(hasRangeInformation(querySpeciesAdapterGUI)){
      doOneSidedFetch(
        values,
        querySpeciesName,
        querySpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        masterCurationSet
      );
    }else if(hasRangeInformation(hitSpeciesAdapterGUI)){
      doOneSidedFetch(
        values,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        querySpeciesName,
        querySpeciesAdapterGUI,
        masterCurationSet
      );
    }else if(hasStableIdInformation(querySpeciesAdapterGUI)){
      doOneSidedFetch(
        values,
        querySpeciesName,
        querySpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        masterCurationSet
      );
    }else if(hasStableIdInformation(hitSpeciesAdapterGUI)){
      doOneSidedFetch(
        values,
        hitSpeciesName,
        hitSpeciesAdapterGUI,
        comparaName,
        comparaAdapterGUI,
        querySpeciesName,
        querySpeciesAdapterGUI,
        masterCurationSet
      );
    }//end if
  }//end doAppropriateFetchesForInputQueryHitAndComparaAdapters
  
  /**
   * -Run individual species fetches, compile two lists of allowed stable gene ids.
   *
   * -Run the compara adapter and get dna-dna aligns for two input ranges
   *
   * -Set the gene stable id info on the compara and get protein aligns.
   *
   * -Fill in protein-align start/end information from the individual species'
   * gene info.
  **/
  private void doTwoSidedFetch(
    Object values,
    String querySpeciesName,
    EnsJAdapterGUI querySpeciesAdapterGUI,
    String comparaName,
    SyntenyComparaAdapterGUI comparaAdapterGUI,
    String hitSpeciesName,
    EnsJAdapterGUI hitSpeciesAdapterGUI,
    CurationSet masterCurationSet
  ) throws org.bdgp.io.DataAdapterException{
    CurationSet comparaCurationSet = null;
    CurationSet querySpeciesCurationSet = null;
    CurationSet hitSpeciesCurationSet = null;
    
    logger.debug("Doing two sided fetch with query species " + querySpeciesName + " and hit species " + hitSpeciesName);

    if(masterCurationSet.getCurationSet(querySpeciesName) == null){
      querySpeciesCurationSet = 
        (CurationSet)querySpeciesAdapterGUI.doOperation(values);
      querySpeciesCurationSet.setOrganism(querySpeciesName);
      masterCurationSet.addCurationSet(querySpeciesName, querySpeciesCurationSet);
    }//end if

    if(masterCurationSet.getCurationSet(hitSpeciesName) == null){
      hitSpeciesCurationSet = 
        (CurationSet)hitSpeciesAdapterGUI.doOperation(values);
      hitSpeciesCurationSet.setOrganism(hitSpeciesName);
      masterCurationSet.addCurationSet(hitSpeciesName, hitSpeciesCurationSet);
    }//end if

    if(masterCurationSet.getCurationSet(comparaName) == null){
      comparaCurationSet = 
        (CurationSet)comparaAdapterGUI.doOperation(values);
      masterCurationSet.addCurationSet(comparaName, comparaCurationSet);
    }//end if
    
    /** Addition of strand information left out
    postProcessComparaCurationSet(
      querySpeciesCurationSet,
      comparaCurationSet,
      hitSpeciesCurationSet
    );
     */
  }//end doTwoSidedFetch

  /**
   * -Run individual fetch on QUERY species, compile a list of gene stable ids
   * -Run the compara with a "one sided fetch" for dna-aligns
   * -sift the output to see what the "other side" should be
   * -run the individual species fetch for the "other side", as
   * -indicated by the compara analysis. Compile another list of
   * gene stable ids
   * -run the compara for protein-homologies, passing in both
   * lists of gene stable ids.
  **/
  private void doOneSidedFetch(
    Object values,
    String querySpeciesName,
    EnsJAdapterGUI querySpeciesAdapterGUI,
    String comparaName,
    SyntenyComparaAdapterGUI comparaAdapterGUI,
    String hitSpeciesName,
    EnsJAdapterGUI hitSpeciesAdapterGUI,
    CurationSet masterCurationSet
  ) throws org.bdgp.io.DataAdapterException{
    
    CurationSet comparaCurationSet;
    CurationSet querySpeciesCurationSet;
    CurationSet hitSpeciesCurationSet;
    String region;
    String chromosome;
    int start;
    int end;
    String stableId;
    
    querySpeciesCurationSet = (CurationSet)masterCurationSet.getCurationSet(querySpeciesName);

    logger.debug("Doing one sided fetch with query species " + querySpeciesName);
    
    if(querySpeciesCurationSet == null){
      querySpeciesCurationSet = 
        (CurationSet)querySpeciesAdapterGUI.doOperation(values);
      querySpeciesCurationSet.setOrganism(querySpeciesName);
      masterCurationSet.addCurationSet(querySpeciesName, querySpeciesCurationSet);
    }//end if

    comparaCurationSet = (CurationSet)masterCurationSet.getCurationSet(comparaName);
    

    if(comparaCurationSet == null){
      logger.debug("Doing one sided compara call");
      comparaCurationSet = (CurationSet)comparaAdapterGUI.doOperation(values);
      masterCurationSet.addCurationSet(comparaName, comparaCurationSet);
    }//end if

    chromosome = comparaCurationSet.getChromosome();
    start = comparaCurationSet.getStart();
    end = comparaCurationSet.getEnd();
    
    stableId = ((SyntenyComparaAdapter)comparaAdapterGUI.getDataAdapter()).getStableId();

    LocationModel hitSpeciesLocationModel = hitSpeciesAdapterGUI.getModel().getLocationModel();

    hitSpeciesAdapterGUI.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.FIND_COORD_SYSTEMS);

    if(stableId != null && stableId.trim().length()>0){
      hitSpeciesLocationModel.setStableID(stableId);
      hitSpeciesLocationModel.setStableIDLocation(true);
      logger.debug("Setting hit location model to stable id " + stableId);

    } else if (start > 0) {
      // Make a guess at the coord system - we don't know this, so look for a 'chromosome' coord system
      // If none found then look for a 'scaffold' coord system
      String coordSystem = null;
      for (int i=0;i<hitSpeciesLocationModel.getCoordSystems().size() && coordSystem==null;i++) {
        if (((String)hitSpeciesLocationModel.getCoordSystems().get(i)).endsWith("chromosome")) {
          coordSystem = (String)hitSpeciesLocationModel.getCoordSystems().get(i);
        }
      } 
      for (int i=0;i<hitSpeciesLocationModel.getCoordSystems().size() && coordSystem==null;i++) {
        if (((String)hitSpeciesLocationModel.getCoordSystems().get(i)).endsWith("scaffold")) {
          coordSystem = (String)hitSpeciesLocationModel.getCoordSystems().get(i);
        }
      } 
      if (coordSystem==null) {
        coordSystem = (String)hitSpeciesLocationModel.getCoordSystems().get(0);
      }

      logger.debug("Setting hit location model to " + coordSystem + " " + chromosome + " " + start + " " + end);
      hitSpeciesLocationModel.setStableIDLocation(false);
      hitSpeciesLocationModel.setSelectedCoordSystem(coordSystem);
      hitSpeciesLocationModel.setSelectedSeqRegion(chromosome);
      hitSpeciesLocationModel.setStart(String.valueOf(start));
      hitSpeciesLocationModel.setEnd(String.valueOf(end));
    }//end if

    hitSpeciesAdapterGUI.getController().handleEventForKey(apollo.dataadapter.ensj.controller.Controller.READ);


    hitSpeciesCurationSet = (CurationSet)masterCurationSet.getCurationSet(hitSpeciesName);
    
    if(hitSpeciesCurationSet == null){
      hitSpeciesCurationSet = 
        (CurationSet)hitSpeciesAdapterGUI.doOperation(values);
      hitSpeciesCurationSet.setOrganism(hitSpeciesName);
      masterCurationSet.addCurationSet(hitSpeciesName, hitSpeciesCurationSet);
    }//end if
    
    /* Addition of strand information left out
    postProcessComparaCurationSet(
      querySpeciesCurationSet,
      comparaCurationSet,
      hitSpeciesCurationSet
    );
     */
  }//end doOneSidedFetch
  
  private boolean hasRangeInformation(EnsJAdapterGUI adapterGUI){
    LocationModel locationModel = adapterGUI.getModel().getLocationModel();

    boolean hasLocation = false;

    if(!locationModel.isStableIDLocation()){
      hasLocation = locationModel.getSelectedCoordSystem() != null && 
                    locationModel.getSelectedSeqRegion() != null &&
                    locationModel.getStart() != null &&
                    locationModel.getEnd() != null;
    }


    return hasLocation;
  }//end hasRangeInformation
  
  private boolean hasStableIdInformation(EnsJAdapterGUI adapterGUI){
    LocationModel locationModel = adapterGUI.getModel().getLocationModel();
    
    String stableId;
    String actualStableId = null;

    if (locationModel.isStableIDLocation()) {
    
      stableId = locationModel.getStableID();

      logger.debug("Got stable id " + stableId);
      if(stableId != null &&
         stableId.trim().length() > 0) {

    //  stableId.length() > EnsJAdapter.STABLE_ID_PREFIX.length()
    //){
    //  actualStableId = stableId.substring(EnsJAdapter.STABLE_ID_PREFIX.length());

        actualStableId = stableId;
      }
    }//end if
    
    return 
      actualStableId != null && 
      actualStableId.trim().length()>0 && 
      !actualStableId.equals("null");
  }//end hasStableIdInformation
  
  /**
   * Add information to the compara-information gleaned from the two 
   * inidividual species' genes: eg the strandedness of the link-feature-pairs
   * depends on the strands of the individual genes, which is only known after you
   * read the strands of the genes from the individual species.
  **/
  /* Taken out for now: let's leave strand-information out of the links: it will
  simplify things a lot, and may not be missed.
  private void postProcessComparaCurationSet(
    CurationSet querySpeciesCurationSet,
    CurationSet comparaCurationSet,
    CurationSet hitSpeciesCurationSet
  ){
    FeatureSetI queryForwardFeatures = querySpeciesCurationSet.getResults().getForwardSet();
    FeatureSetI queryReverseFeatures = querySpeciesCurationSet.getResults().getReverseSet();
    FeatureSetI hitForwardFeatures = hitSpeciesCurationSet.getResults().getForwardSet();
    FeatureSetI hitReverseFeatures = hitSpeciesCurationSet.getResults().getReverseSet();
    
    Vector linkFeatures = comparaCurationSet.getResults().getForwardSet().getFeatures();
    
    apollo.util.FeatureList list = null;
    FeaturePair pair = null;
    SeqFeatureI queryGene = null;
    SeqFeatureI hitGene = null;
    int start;
    int end;
    String name = null;

    for(int i=0; i<linkFeatures.size(); i++){
      pair = (FeaturePair)linkFeatures.get(i);

      if(pair.getType().equals("protein-protein-align")){
        //If the pair is a pair of orthologous genes then 
        //set the start/end of the pair strands based on the start/end of
        //the genes we found
        name = pair.getName();
        list = queryForwardFeatures.findFeaturesByName(name);
        if(list.size() >0){
          queryGene = (SeqFeatureI)list.get(0);
        }else{
          list = queryReverseFeatures.findFeaturesByName(name);
          if(list.size() >0){
            queryGene = (SeqFeatureI)list.get(0);
          } else{
            list = hitForwardFeatures.findFeaturesByName(name);
            if(list.size() >0){
              queryGene = (SeqFeatureI)list.get(0);
            }else{
              list = hitReverseFeatures.findFeaturesByName(name);
              if(list.size() >0){
                queryGene = (SeqFeatureI)list.get(0);
              }//end if
            }//end if
          }//end if
        }

        name = pair.getHitFeature().getName();
        list = hitForwardFeatures.findFeaturesByName(name);
        if(list.size() >0){
          hitGene = (SeqFeatureI)list.get(0);
        }else{
          list = hitReverseFeatures.findFeaturesByName(name);
          if(list.size() >0){
            hitGene = (SeqFeatureI)list.get(0);
          }else{
            list = queryForwardFeatures.findFeaturesByName(name);
            if(list.size() >0){
              hitGene = (SeqFeatureI)list.get(0);
            }else{
              list = queryReverseFeatures.findFeaturesByName(name);
              if(list.size() >0){
                hitGene = (SeqFeatureI)list.get(0);
              }//end if
            }//end if
          }//end if
        }//end if

        if((queryGene != null) && (hitGene != null)){
          pair.setStrand(queryGene.getStrand());
          pair.setStart(pair.getStart());
          pair.setEnd(pair.getEnd());
          pair.getHitFeature().setStrand(hitGene.getStrand());
          pair.getHitFeature().setStart(pair.getHitFeature().getStart());
          pair.getHitFeature().setEnd(pair.getHitFeature().getEnd());
        }//end if
      }else{
        //
        //this a dna-dna alignment.
      }
    }//end for
  }
   */
  
  private Style getStyle() { return getAdapter().getStyle(); }
  private boolean isRead() { 
    return getOperation() == ApolloDataAdapterI.OP_READ_DATA; 
  }
  private boolean addNumSpeciesDropdown() { 
    return isRead()  && getStyle().addSyntenySpeciesNumDropdown(); 
  }

  private void buildGUI()
  {
    setTabbedPane(new JTabbedPane());
    Component adapterComponent;
    Iterator adapters = getAdapterLabelList().iterator();
    AbstractApolloAdapter adapter;
    DataAdapterUI adapterUI;
    String adapterName;

    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    if (addNumSpeciesDropdown()) 
      buildNumberOfSpeciesDropdownList(gbc);
      //buildLinkQueryButtons(gbc);

    while(adapters.hasNext()){
      adapterName = (String)adapters.next();
      adapter = (AbstractApolloAdapter)getAdapter().getAdapters().get(adapterName);
      if (adapter == null) {
	logger.error("Could not get adapter " + adapterName);
	//System.exit (1); // throw an exception?
      }
      //
      //Once and only time this thing is created.
      adapterUI = adapter.getUI(getOperation());
      getAdapterGUIs().put(adapterName, adapterUI);
      
      //
      //This cryptic step allows the child dataadapterUI to set this
      //class as its parent (if it wants). See SyntenyComparaAdapter
      //-it also allows the child adapter to store a handle to itself
      //in the 'shared data' hashmap (if it wants).
      adapterUI.setInput(adapterName);
      adapterUI.setInput(this);
        
      getTabbedPane().addTab(adapterName, (Component)adapterUI);
    }//end for
    enableTabbedPanes(addNumSpeciesDropdown());
    add(getTabbedPane(),gbc);
  }
  
  /** A drop down list of numbers from 1 to numberOfSpecies, for user to choose how
      many species to load */
  private void buildNumberOfSpeciesDropdownList(GridBagConstraints gbc) {
    speciesNumberDropdownList = new JComboBox();

    speciesNumberDropdownList.addItemListener( new ItemListener() {
        public void itemStateChanged(ItemEvent e) { 
          Integer i = (Integer)speciesNumberDropdownList.getSelectedItem();
          setNumberOfSpeciesToLoad(i.intValue()); } } );

    for (int i=1; i<=getConfiggedNumberOfSpecies(); i++) 
      speciesNumberDropdownList.addItem(new Integer(i));
    JLabel label = new JLabel("Select Number of Species to Load :  ");
    Box b = new Box(BoxLayout.X_AXIS);
    b.add(label);
    b.add(speciesNumberDropdownList);
    gbc.gridx = 0; // gridy is RELATIVE by default
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0,160,0,0);
    add(b,gbc);
    gbc.insets = new Insets(0,0,0,0); // reset insets for tabbed pane
  }

  private int getConfiggedNumberOfSpecies() {
    return getSyntenyStyle().getNumberOfSpecies();
  }

  private int numberOfSpeciesToLoad = 1;
  private void setNumberOfSpeciesToLoad(int i) {
    numberOfSpeciesToLoad = i;
    enableTabbedPanes(i-1); // 0 based
  }
  
  private int getNumberOfSpeciesToLoad() {
    return numberOfSpeciesToLoad;
  }

  /** Enable tabbed panes 0 to index */
  private void enableTabbedPanes(int index) {
    for (int i=0; i < getTabbedPane().getTabCount(); i++) {
      getTabbedPane().setEnabledAt(i,i <= index);
    }
    if (getTabbedPane().getSelectedIndex() > index) 
      getTabbedPane().setSelectedIndex(0);
  }

  /** Disable beyond first tab if justFirstSpecies true */
  private void enableTabbedPanes(boolean justFirstSpecies) {
    boolean enable = !justFirstSpecies;
    for (int i = 1; i < getTabbedPane().getTabCount(); i++)
      getTabbedPane().setEnabledAt(i,enable);
    getTabbedPane().setSelectedIndex(0);
  }

  private JTabbedPane getTabbedPane(){
    return tabbedPane;
  }
  
  private void setTabbedPane(JTabbedPane newValue){
    tabbedPane = newValue;
  }

  private SyntenyAdapter getAdapter(){
    return driver;
  }
  
  public Map getAdapterGUIs(){
    return adapterGUIs;
  }
  
  protected IOOperation getOperation(){
    return op;
  }//end getOperation
  
  /** List of labels associated with adapters (not adapters themselves) */
  private void setAdapterLabelList(java.util.List list){
    adapterLabelList = list;
  }
  
  public java.util.List getAdapterLabelList(){
    return adapterLabelList;
  }
  
  /**
   * Data common to all adapters should have a place to live - this is it.
  **/
  public HashMap getSharedData(){
    return sharedData;
  }//end getSharedData
  
  
  String getHitFromUser(String[] names) {//Set hitNames) {
    //Object[] namesArr = hitNames.toArray();
    String initial = names[0];
    // now we're getting into gui land - do in SyntenyAdapterGUI?
    String m = "Select a hit to display";
    return (String)JOptionPane.showInputDialog(null,m,m,JOptionPane.PLAIN_MESSAGE,
                                               null,names,initial);
  }

}
