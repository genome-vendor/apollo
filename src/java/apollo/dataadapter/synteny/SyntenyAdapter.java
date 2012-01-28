package apollo.dataadapter.synteny;

import java.util.*;
import java.util.List;
import java.util.Properties;
import java.io.*;
import javax.swing.JOptionPane; // should go in gui

import apollo.util.FeatureIterator;
import apollo.util.FeatureList;
import apollo.seq.io.*;
import apollo.dataadapter.*;
import apollo.datamodel.*;
import apollo.datamodel.CompositeDataHolder;
import apollo.datamodel.SpeciesComparison;
import apollo.config.Config;
import apollo.config.Style;

import org.apache.log4j.*;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import org.bdgp.swing.widget.*;

/**
 * When the user selects "Synteny" from the datachooser menu, I am the
 * adapter that's loaded. Note that all of my hard work is really done
 * by my GUI - SyntenyAdapterGUI. In particular, the data-fetching is
 * run by the doOperation method on the GUI.
**/
public class SyntenyAdapter extends AbstractApolloAdapter{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SyntenyAdapter.class);

  /** child data adapters keyed by species name. "species name" is the "logical name"
      which is what appears as "Name.LogicalName" in style file (not quoted field 
      after this) */
  private Map adapters;
  private List dataAdapterList = new ArrayList(4);
  //private List speciesAdapters; replaced by dataAdapterList
  
  IOOperation[] supportedOperations = 
    {
      ApolloDataAdapterI.OP_READ_DATA,
      ApolloDataAdapterI.OP_READ_SEQUENCE,
      apollo.dataadapter.ApolloDataAdapterI.OP_WRITE_DATA
    };

  public SyntenyAdapter(){
    adapters = buildAdapters();
  }

  /** maps adapter "logical name"/label to the adapter itself */
  public Map getAdapters(){
    return adapters;
  }
  
  public ApolloDataAdapterI getSpeciesAdapter(String species) {
    Object obj = getAdapters().get(species);
    if (obj instanceof ApolloDataAdapterI) return (ApolloDataAdapterI)obj;
    return null;
  }

  /** dont think this is used -> erase */
//   public void setAdapters(Map adapters){
//     this.adapters = adapters;
//   }//end setAdapters
  
  /** Override styles of child adapters to synteny styles 
   i forget the reason for this - mg */
  void setChildAdapterStyles() {
    for(Iterator i = getAdapters().values().iterator(); i.hasNext();) 
      ((ApolloDataAdapterI)i.next()).setStyle(getStyle());
  }

  public String getName() {
    return "Synteny";
  }

  public String getType() {
    return "Multiple Species and Compara Data";
  }

  /**
   * This is an arcane requirement to get the File->Save menu to light up.
  **/
  public DataInputType getInputType() {
    return DataInputType.FILE;
  }
  
  public String getInput() {
    throw new NotImplementedException("Not yet implemented");
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  /** maps IOOperations to theyre SyntenyDataAdapterUI */
  private Map syntenyOpToUI = new HashMap(3);
  /**
   * gets ui for op from cache. if not there creates new one, adds to cache
  **/
  public DataAdapterUI getUI(IOOperation op) {
    DataAdapterUI ui = (DataAdapterUI)syntenyOpToUI.get(op);
    if (ui == null) {
      ui = new SyntenyAdapterGUI(op, this);
      syntenyOpToUI.put(op,ui);
    }
    return ui;
  }
//   private SyntenyAdapterGUI getReadUI() { 
//     DataAdapterUI daui = getUI(ApolloDataAdapterI.OP_READ_DATA); 
//     if (daui instanceof SyntenyAdapterGUI) return (SyntenyAdapterGUI)daui;
//     return null;
//   }

  public void setRegion(String region) throws ApolloAdapterException {
    throw new NotImplementedException("Not yet implemented");
  }

  public Properties getStateInformation() {
    Properties props = new Properties();
    return props;
  }

  public void setStateInformation(Properties props) {
  }

  /**
   * Visit each child adapter and ask each one for its CurationSet.
   * combine the results into a composite curation set.
  **/
  public CurationSet getCurationSet() throws ApolloAdapterException {
    throw new NotImplementedException("Not yet implemented");
  }//end getCurationSet

  
  private void setSequence(SeqFeatureI sf, CurationSet curationSet) {
    throw new NotImplementedException("Not yet implemented");
  }

  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException{
    throw new NotImplementedException("Not yet implemented");
  }

  public SequenceI getSequence(
    DbXref dbxref, 
    int start, 
    int end
  ) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref) throws ApolloAdapterException{
    throw new NotImplementedException();
  }
  
  public Vector getSequences(
    DbXref[] dbxref, 
    int[] start, 
    int[] end
  ) throws ApolloAdapterException{
    throw new NotImplementedException();
  }
 
  /** Write out composite data holder to multiple data adapters - 
   called by DataLoader/FileMenu not SynAdapGUI */
  public void commitChanges(CompositeDataHolder cdh) throws ApolloAdapterException {
    // data adapter list and cdh should be synched - if not that needs fixin
    // They are not synchronized ...
    // The number if child adapters should be simply the number of species
    // selected, not the number of species max configured in the style file.
    //for (int i=0; i<getNumberOfChildAdapters(); i++)
    for (int i=0; i<cdh.numberOfSpecies(); i++)
      getChildAdapter(i).commitChanges(cdh.getCurationSet(i));
  }

  /** I think this is now pase and should be deleted - 
      always gets a CompositeDataHolder now - this is actually not called by
      SyntenyAdapterGUI.doOp(WRITE) - calls child adapter guis doOp
  this is called by DataLoader though */
  public void commitChanges(CurationSet curationSet) throws ApolloAdapterException {
    Iterator adapterNames = curationSet.getChildCurationSets().keySet().iterator();
    String adapterName = null;
    ApolloDataAdapterI adapter = null;
    CurationSet singleSpeciesCurationSet = null;
    boolean allowSave = false;
    HashMap properties = 
      Config
        .getStyle("apollo.dataadapter.synteny.SyntenyAdapter")
        .getSyntenySpeciesProperties();
    
    String dataAdapterClassName = null;
    String dataAdapterKey = null;
    
    while(adapterNames.hasNext()){
      adapterName = (String)adapterNames.next();
      if(!(adapterName.indexOf("-")>0)){
        dataAdapterKey = "Species."+adapterName+".DataAdapter";
        dataAdapterClassName = (String)properties.get(dataAdapterKey);
        adapter = (ApolloDataAdapterI)getAdapters().get(adapterName);
        singleSpeciesCurationSet = curationSet.getCurationSet(adapterName);

        if(singleSpeciesCurationSet == null){
          throw new apollo.dataadapter.ApolloAdapterException(
            "Expecting curation set for species: "+adapterName+" -- not supplied"
          );
        }//end if

        /*
        allowSave = 
          Config.getAdapterRegistry().adapterSupports(
            dataAdapterClassName,
            ApolloDataAdapterI.OP_WRITE_DATA
          );

        if(allowSave){
          adapter.commitChanges(singleSpeciesCurationSet);
        }//end if
         */

        adapter.commitChanges(singleSpeciesCurationSet);
      }
    }//end while
  }
  
  public String getRawAnalysisResults(String id) throws ApolloAdapterException{
    throw new NotImplementedException();
  }

  public void init() {}
  
  /** 
   * Read the style file for the synteny adapter to get a list of
   * child adapters to insert here. Then create each adapter, and
   * add to the return-hashmap using the species' logical name as a key.
   "logical name" is what appears after Name in the "Name.logical-name" field
   in the style file. It is not the quoted field after that.
  **/
  private Map buildAdapters(){
    // SytenyAdapter string should not be hardwired
    // if Style.UseGenericSyntenySpecies(game), this is just Species1, Species2...
    //Iterator names=getSyntenyStyle().getSyntenySpeciesNames().keySet().iterator();

    if (getSyntenyStyle() == null) {
      logger.error("Can't build Synteny adapter--failed to load Synteny style");
      return null;
    }
    HashMap properties = getSyntenyStyle().getSyntenySpeciesProperties();
    
    String dataAdapterName;
    String logicalName;
    String dataAdapterKey;
    String shortLogicalName;
    AbstractApolloAdapter dataAdapter;
    Class dataAdapterClass;
    String shortDataAdapterName;
    HashMap returnMap = new HashMap();
    HashMap syntenyProperties;
    int index;
    
    for (int i =0; i<getSyntenyStyle().getNumberOfSyntenySpecies(); i++) {
      // First see if species names fully specified in style (ensj,gff)
      //go from the string Name.Human to the string Human
      //logicalName = (String)names.next();
      logicalName = getSyntenyStyle().getSyntenySpeciesString(i);
      index = logicalName.indexOf(".");
      shortLogicalName = logicalName.substring(index+1);
      
      dataAdapterKey = "Species."+shortLogicalName+".DataAdapter";
      dataAdapterName = (String)properties.get(dataAdapterKey);
      
      // If finding with particular species name failed, use default synteny 
      // data adapter if its set (game)
      if (dataAdapterName == null)
        dataAdapterName = getSyntenyStyle().getDefaultSyntenyDataAdapterString();

      if(dataAdapterName == null){
        throw new NonFatalDataAdapterException("No data adapter name found for key: "+dataAdapterKey);
      }//end if
      
      // should check with config to see if instance of data adapter already exists
      // why duplicate instances? - actually need separate instances as data adapter
      // has state - like "database"
      try{
        dataAdapterClass = Class.forName(dataAdapterName);
        dataAdapter = (AbstractApolloAdapter)dataAdapterClass.newInstance();
        dataAdapter.setSpecies(shortLogicalName); // convenience
        dataAdapter.setCurationNumber(i);
        dataAdapterList.add(dataAdapter);
        returnMap.put(shortLogicalName, dataAdapter);
      }catch(ClassNotFoundException exception){
        logger.debug("SyntenyAdapter ClassNotFoundException instantiating species data adapter " + dataAdapterName, exception);
        throw new IllegalStateException("cant create data adapter");
      }catch(InstantiationException exception){
        logger.debug("SyntenyAdapter InstantiationException instantiating species data adapter " + dataAdapterName, exception);
        throw new IllegalStateException("cant create data adapter");
      }catch(IllegalAccessException exception){
        logger.debug("SyntenyAdapter IllegalAccessException instantiating species data adapter " + dataAdapterName, exception);
        throw new IllegalStateException("cant create data adapter");
      }//end try
      
    }

    for (int i =0; i<getSyntenyStyle().getSyntenyLinkOrder().size(); i++) {

      logicalName = (String)getSyntenyStyle().getSyntenyLinkOrder().get(i);
      index = logicalName.indexOf(".");
      shortLogicalName = logicalName.substring(index+1);
      
      dataAdapterKey = "Species."+shortLogicalName+".DataAdapter";
      dataAdapterName = (String)properties.get(dataAdapterKey);
      
      // If finding with particular species name failed, use default synteny 
      // data adapter if its set (game)
      if (dataAdapterName == null)
        dataAdapterName = getSyntenyStyle().getDefaultSyntenyDataAdapterString();

      if(dataAdapterName == null){
        throw new NonFatalDataAdapterException("No data adapter name found for key: "+dataAdapterKey);
      }//end if
      
      // should check with config to see if instance of data adapter already exists
      // why duplicate instances? - actually need separate instances as data adapter
      // has state - like "database"
      try{
        logger.debug("Making adapter of type " + dataAdapterName);
        dataAdapterClass = Class.forName(dataAdapterName);
        dataAdapter = (AbstractApolloAdapter)dataAdapterClass.newInstance();
        dataAdapter.setSpecies(shortLogicalName); // convenience
        //dataAdapter.setCurationNumber(i);
        dataAdapterList.add(dataAdapter);
        returnMap.put(shortLogicalName, dataAdapter);
      }catch(ClassNotFoundException exception){
        throw new IllegalStateException("cant create data adapter");
      }catch(InstantiationException exception){
        throw new IllegalStateException("cant create data adapter");
      }catch(IllegalAccessException exception){
        throw new IllegalStateException("cant create data adapter");
      }//end try
      
    }
    
    return returnMap;
    
  }

  /** true if any curation set contain link data */
  boolean curSetHasSyntenyLinks() {
    for (int i=0; i<getNumberOfChildAdapters(); i++) 
      if (getChildAdapter(i).hasLinkData()) return true;
    return false;
  }

  /** Returns a list of adapters for species, link adapters not included.
      Returned in order specified in synteny style */
//   private List getSpeciesAdapters() {
//     if (speciesAdapters!=null) return speciesAdapters;
//     // dont like how getAdapters has lost the original order
//     // also dont think style should know about what is link and species
//     List adapterNames = getSyntenyStyle().getSyntenySpeciesAndLinkOrder();
//     List speciesAdapters = new ArrayList(2);
//     ListIterator it = adapterNames.listIterator();
//     while (it.hasNext()) {
//       String speciesLogicalName = getSpeciesLogicalName((String)it.next());
//       AbstractApolloAdapter adap = getSpeciesAdapter(speciesLogicalName);
//       if (!logicalNameIsLink(speciesLogicalName) && adap!=null) speciesAdapters.add(adap);
//     }
//     return speciesAdapters;
//   }

  /** This takes in "Name.LogicalName" field and returns the LogicalName 
      (just takes off the "Name." prefix) */
  private String getSpeciesLogicalName(String fullNameField) {
    int index = fullNameField.indexOf(".");
    return fullNameField.substring(index + 1);
  }
  /** Inverse of getSpeciesLogicalName */
  private String logicalToStyleLogicalString(String logicalName) {
    return "Name."+logicalName;
  }

  /** From ApolloDataAdapterI */
  public int getNumberOfChildAdapters() { return dataAdapterList.size(); }

  public ApolloDataAdapterI getChildAdapter(int i) {
    return (ApolloDataAdapterI)dataAdapterList.get(i);
  }

  /** Return species name in ith place in order species listed in style file */
  //private String getSpeciesName(int i) { List adapterNames = getSyntenyStyle().getSyntenyAdapterOrder();}

  //private int getNumberOfChildAdapters() { return getSpeciesAdapters().size(); }

  /** This should return ApolloAdapterI */
//   ApolloDataAdapterI getSpeciesAdapter(int i) {
//     return (ApolloDataAdapterI)getSpeciesAdapters().get(i);
//   }

  private Style getSyntenyStyle() {
    // is there some way to do this more generally?
    //return Config.getStyle("apollo.dataadapter.synteny.SyntenyAdapter");
    return Config.getStyle(getClass().getName());
  }

  /** Returns true for "Compara" or "Links" */
  private boolean logicalNameIsLink(String logicalName) {
    String fullSpeciesName = getSpeciesNameForLogicalName(logicalName);
    // generic species (game) dont have links nor logical names
    if (fullSpeciesName == null) 
      return false;
    if (fullSpeciesName.toLowerCase().equals("compara")) return true;
    if (fullSpeciesName.toLowerCase().equals("links")) return true;
    return false;
  }
  /** Retrieves the full species name from the logical name. Style file format is:
      "Name.LogicalName" "Full Species Name" */
  private String getSpeciesNameForLogicalName(String logicalName) {
    // prepends "Name."
    String styleNameString = logicalToStyleLogicalString(logicalName);
    return (String)getSyntenyStyle().getSyntenySpeciesNames().get(styleNameString);
  }
  
  /** Load data from dataadapters where the link information is embedded in the
      species data (rather than coming from a separate source). Presently only
      the game adapter does this. 
      This is public because TestApollo uses it. */
  public CompositeDataHolder loadSpeciesThatContainLinks(int numberOfSpecies)  
    throws org.bdgp.io.DataAdapterException {
    EmbeddedLinkSynteny embeddedSynteny = new EmbeddedLinkSynteny(this);
    // loop through species? or have GS do that?
    CompositeDataHolder cdh = embeddedSynteny.loadSpeciesAndLinks(numberOfSpecies);
    return cdh;
  }

  /** From ApolloDataAdapterI. Bring up the link as a species in synteny.  */
  public void loadNewSpeciesFromLink(SeqFeatureI link,CompositeDataHolder compData) 
    throws org.bdgp.io.DataAdapterException {
    EmbeddedLinkSynteny gs = new EmbeddedLinkSynteny(this);
    gs.loadNewSpeciesFromLink(link,compData.getSpeciesComparison(0));
  }

  // pase - delete? this goes through curation and brings up a list of its links
//   CompositeDataHolder getCompositeDataFromOneCuration()  
//     throws org.bdgp.io.DataAdapterException {
//     EmbeddedLinkSynteny gameSynteny = new EmbeddedLinkSynteny(this);
//     SpeciesComparison sc = gameSynteny.getSpeciesCompFromOneCuration();
//     return new CompositeDataHolder(sc);
//   }
  
  /**
   * Does nothing yet, because the state info isn't being used on this adapter yet.
  **/
  public void clearStateInformation() {
  }

  public boolean isComposite(){
    return true;
  }
  
}
