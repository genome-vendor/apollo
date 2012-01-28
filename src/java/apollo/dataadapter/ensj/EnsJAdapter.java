package apollo.dataadapter.ensj;
import apollo.gui.*;
import apollo.config.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.debug.*;
import apollo.datamodel.seq.*;
import apollo.dataadapter.ensj.controller.FindCoordSystemsHandler;
import apollo.dataadapter.ensj.EnsJAdapterUtil;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.*;
import java.util.regex.*;

import org.ensembl.*;
import org.ensembl.driver.*;
import org.ensembl.datamodel.*;
import org.ensembl.util.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.*;

/** EnsJ - Direct Access for Ensembl Databases (Schema 20 and above) */
public class EnsJAdapter extends AbstractApolloAdapter {
  //People can use this constant to use for a key in the properties
  //that are passed in to set state (see setStateInformation).
  public static final String NUMBER_OF_FEATURES = "NUMBER_OF_FEATURES";

  public static final String REGION_PATTERN = "REGION: ([^--]*)--(.*):(\\S*):(\\S*)-(\\S*)";
  public static final String REGION_PATTERN_2 = "Chr ([^--]*)--(.*):(\\S*) (\\S*) (\\S*)";
  public static final String REGION_PATTERN_3 = "Chr (\\S*) (\\S*) (\\S*)";
  public static final String ID_PATTERN = "ID: (\\S*)";

  //State as passed in by GUI.
  private Properties _stateInformation;

  private Driver _driver;

  protected final static Logger logger = LogManager.getLogger(EnsJAdapter.class);

  public static final String ID = "ID";
  public static final String VERSION = "VERSION";
  public static final String COORD_SYSTEM = "COORD_SYSTEM";
  public static final String SEQ_REGION = "SEQ_REGION";
  public static final String START = "START";
  public static final String END = "END";
  
  public final static String STABLE_ID_PREFIX = "ID: ";


  public EnsJAdapter() {
    // This is the name that will be shown in the chooser drop-down
    setName("EnsJ - Direct Access for Ensembl Databases (Schema 32 and above)");
  }
  
  /**
   * From bdgp DataAdapter. No idea where this is used.
  **/
  public String getType() {
    return "Ensembl Adapter";
  }

  /**
   * From bdgp DataAdapter. 
   * If you want the adapter to WRITE stuff, it needs to add OP_WRITE_DATA to the list.
  **/
  public IOOperation [] getSupportedOperations() {
    return new IOOperation[] {
       ApolloDataAdapterI.OP_READ_DATA,
       ApolloDataAdapterI.OP_APPEND_DATA
    };
  }

  /**
   * This method traditionally creates a new UI for the adapter,
   * based on the operation - read or write (each creates a different UI instance).
  **/
  public DataAdapterUI getUI(IOOperation op) {
    return new EnsJAdapterGUI(op);
  }

  /**
   * This is a convenient way of passing in the adapter's state: each part of the
   * state is a key-value pair in the input Properties. Use the static constant(s)
   * on this dataadapter for the keys - it's just safer!
  **/
  public void setStateInformation(Properties properties) {
    _stateInformation = properties;
  }//end setStateInformation
  
  public void setRegion(String region){
    getStateInformation().setProperty(StateInformation.REGION, region);
  }

  /**
   * From org.bdgp.DataAdapter interface. This is called when the adapter is
   * created and added to the registry (see org.bdgp.io.DataAdapterChooser).
  **/
  public void init() {
  }

  /**
   * This is the main data-retrieval method.
  **/
  public CurationSet getCurationSet() throws ApolloAdapterException{
    CurationSet curationSet = new CurationSet();
    StrandedFeatureSet results = null;
    StrandedFeatureSet annotations = null;
    Location location;
    String region = getStateInformation().getProperty(StateInformation.REGION);
    
    //
    //Superclass magic:
    clearOldData();
    
    //make sure we have basic db information
    validateDatabaseProperties();

    //Now you can try to initialise the ensj-driver
    try {
      
      initialiseDriver();
      
    } catch(ConfigurationException exception) {
      
      logger.error(exception.getMessage());
      throw new apollo.dataadapter.ApolloAdapterException(
        "Didn't specify enough information to configure database access: "
          + exception.getMessage()
      );
      
    }

    //parse the REGION string in the stateinformation to create a Location
    location = getLocationForInputRegion();    
    logger.debug("Fetching region " + location);
    
    //if we've been passed a stable-id, then we have to first
    //do a fetch on it to find the genomic location of the thing with
    //that id. Otherwise, just create the Location with the coordinates we've been given


    try{
      results = getResults(curationSet, location);
  // This is what GAMEAdaptor sets name for the top set to
      results.setName("Analyses");

      if(getStateAsBoolean(StateInformation.ADD_RESULT_GENES_AS_ANNOTATIONS)){
        DataFetcher fetcher = new DataFetcher(this);
        annotations = (StrandedFeatureSet)fetcher.getAnnotations(curationSet,returnedGenes,getStateAsBoolean(StateInformation.ADD_TRANSCRIPT_SUPPORT));
      } else {
        annotations = getAnnotations();
      }

      curationSet.setRefSequence(getSequence(new DbXref(region,region,region)));
      
    }catch(AdaptorException exception){
      throw new ApolloAdapterException("Problem fetching data: "+exception.getMessage(), exception);
    }

    curationSet.setStart(location.getStart());
    curationSet.setEnd(location.getEnd());
    //([^--]*)--(.*):(\\S*)
    curationSet.setChromosome(
      location.getCoordinateSystem().getVersion()+"--"+
      location.getCoordinateSystem().getName()+":"+
      location.getSeqRegionName()
    );
    //curationSet.setOrganism (getStateInformation().getProperty(StateInformation.DATABASE));
    curationSet.setOrganism (getOrganism());
    curationSet.setName("ensj");
    
    curationSet.setResults(results);
    curationSet.setAnnots(annotations);

    if(getStateAsBoolean(StateInformation.RESET_GENE_START_AND_STOP)){
      // Currently done in BuilderAdapter so sequence can be reset. This is not clean
      // DataModelConversionUtil.resetTranslationStartsAndStops(curationSet.getResults());
      // DataModelConversionUtil.resetTranslationStartsAndStops(curationSet.getAnnots());
    }
    
    //apollo.dataadapter.debug.DisplayTool.showFeatureSet(curationSet.getResults());
    //more Superclass magic.
    return curationSet;
  }//end getCurationSet


  private String getOrganism() {
 
    String dbsql = "select meta_value from meta where meta_key='species.classification' order by meta_id limit 2";

    // This is a dumb default, but without any real meta data it's difficult to
    // guess the species. This is what it used to be set to so lets default to that.
    String organismName = getStateInformation().getProperty(StateInformation.DATABASE);

    ResultSet dbresults;
    try{
 
      int nRow = 0;
      Connection connection = getDriver().getConnection();
      String[] classification;

      classification = new String[2];

      dbresults = connection.createStatement().executeQuery(dbsql);
      while(dbresults.next()){
        classification[nRow++] = dbresults.getString(1);
      }
      if (nRow != 2) {
      } else {
        organismName = classification[1] + "_" + classification[0];
      }
      JDBCUtil.close(connection);
      
    }catch(Exception exception){
      throw new NonFatalException(exception.getMessage(), exception);
    }
    return organismName;
  }

  public Boolean addToCurationSet() throws ApolloAdapterException {
    // Existing curation_set is stored in superclass
    boolean okay = false;
    logger.debug("Existing curation set = " + curation_set + " chr " + curation_set.getChromosome() + 
                       " name " + curation_set.getName());
    getStateInformation().setProperty(StateInformation.REGION, 
                                      "Chr " + curation_set.getChromosome() + " " + curation_set.getStart() + " " + curation_set.getEnd());
    //make sure we have basic db information
    validateDatabaseProperties();

    //Now you can try to initialise the ensj-driver
    try {
      
      initialiseDriver();
      
    } catch(ConfigurationException exception) {
      
      logger.error(exception.getMessage());
      throw new apollo.dataadapter.ApolloAdapterException(
        "Didn't specify enough information to configure database access: "
          + exception.getMessage()
      );
      
    }

    //parse the REGION string in the stateinformation to create a Location
    Location location = getLocationForInputRegion();
  
    logger.debug("Generated location = " + location);

    StrandedFeatureSet results = null;
    StrandedFeatureSet annotations = null;

    try{
      results = getResults(curation_set, location);

    }catch(AdaptorException exception){
      throw new ApolloAdapterException("Problem fetching data: "+exception.getMessage(), exception);
    }

    for (int i=0; i<results.getForwardSet().size();i++) {
      SeqFeatureI sf = (SeqFeatureI)results.getForwardSet().getFeatureAt(i);
      curation_set.getResults().getForwardSet().addFeature(sf);
    }
    for (int i=0; i<results.getReverseSet().size();i++) {
      SeqFeatureI sf = (SeqFeatureI)results.getReverseSet().getFeatureAt(i);
      curation_set.getResults().getReverseSet().addFeature(sf);
    }

    okay = true;
    return new Boolean(okay);
  }

  
  private Location getLocationForInputRegion() throws ApolloAdapterException{
    HashMap coords;
    Location location;
    String version;
    String coordSystemName;
    CoordinateSystem coordSystem;
    CoordinateSystem[] coordSystems;

    try{
      coords = parseRegionString();

      if(coords.get(ID) != null){

        location = getLocationForStableID((String)coords.get(ID));

      }else{

        version = (String)coords.get(VERSION); //could be null
        coordSystemName = (String)coords.get(COORD_SYSTEM);

        if(isNull(coordSystemName)){

          coordSystems = getDriver().getCoordinateSystemAdaptor().fetchAllByFeatureTable("gene"); 
          if(coordSystems.length != 1){
            throw new ApolloAdapterException("Can't find a unique coord system that genes are stored in!");
          }else{
            coordSystem = coordSystems[0];
          }
          
        }else{
	  if(version.equals(FindCoordSystemsHandler.NULL_VERSION)){
            coordSystem = getDriver().getCoordinateSystemAdaptor().fetch(coordSystemName,null);
	  } else {
            coordSystem = getDriver().getCoordinateSystemAdaptor().fetch(coordSystemName, version);
          }
        }

        if(coordSystem == null){
          throw new ApolloAdapterException("Can't find coord system with name/version: "+coordSystemName+"/"+version);
        }

        location = 
          new Location(
            coordSystem, 
            (String)coords.get(SEQ_REGION), 
            ((Integer)coords.get(START)).intValue(), 
            ((Integer)coords.get(END)).intValue(), 
            0
          );
      }
    }catch(AdaptorException exception){
      exception.printStackTrace();
      throw new ApolloAdapterException("Problem fetching data: "+exception.getMessage(), exception);
    }
    
    return location;
  }
    

  /**
   * Do a fetch of the gene pointed at by the stable id, then deduce its location,
  **/
  private Location getLocationForStableID(String stableID) 
  throws ApolloAdapterException{

    Location location;
    
    String version;
    String coordSystem;
    String seqRegion;
    int start = 0;
    int end = 0;
    
    org.ensembl.datamodel.Gene gene;
    org.ensembl.datamodel.Transcript transcript;
    org.ensembl.datamodel.Exon exon;
    org.ensembl.datamodel.Translation translation;
    
    try{
      gene = getDriver().getGeneAdaptor().fetch(stableID);

      if(gene != null){
        return deStrandLocation(gene.getLocation());
      }

      transcript = getDriver().getTranscriptAdaptor().fetch(stableID);

      if(transcript != null){
        return deStrandLocation(transcript.getLocation());
      }

      translation = getDriver().getTranslationAdaptor().fetch(stableID);

      if(translation != null){
        return deStrandLocation(translation.getTranscript().getLocation());
      }

      exon = getDriver().getExonAdaptor().fetch(stableID);

      if(exon != null){
        return deStrandLocation(exon.getLocation());
      }
    }catch(AdaptorException exception){
      throw new ApolloAdapterException(
        "Problems finding gene, transcript, translation or exon with stableID: "+stableID+
        ": "+exception.getMessage(), 
        exception
      );
    }
   
    throw new ApolloAdapterException("Cannot find a gene, transcript, translation or exon with input stableID: "+stableID);
  }

  private Location deStrandLocation(Location strandedLoc) {
    Location unstrandedLoc = strandedLoc.copy();
    unstrandedLoc.setStrand(0);
    return unstrandedLoc;
  }
  
  // SMJS Temporary hack
    List returnedGenes = null;

  private StrandedFeatureSet getResults(CurationSet curationSet, Location location) throws AdaptorException{
    StrandedFeatureSet results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    List flatFeatures = new ArrayList();
    int numberFetched = 0;
    List includedTypes = null;
    
    //A utility to help fetch data - just a way of splitting out lots of code.
    DataFetcher fetcher = new DataFetcher(this);
    
    //If you've nominated to include the ProteinAnnotations -- i.e. SimplePeptides,
    //then you MUST load the genes, so you can load their translations...
    if(
      getStateAsBoolean(StateInformation.INCLUDE_GENE) || 
      getStateAsBoolean(StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE)
    ){
      logger.info("fetching genes...");
      fireProgressEvent(new ProgressEvent(this,new Double(10.0),"Getting genes..."));
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_GENE_TYPES);
      returnedGenes = fetcher.addGenes(results, location, includedTypes, getStateAsBoolean(StateInformation.AGGRESSIVE_GENE_NAMING) || getStateAsBoolean(StateInformation.ADD_TRANSCRIPT_SUPPORT), getStateInformation().getProperty(StateInformation.TYPE_PREFIX_STRING) );
      numberFetched = returnedGenes.size();
      logger.info("fetched "+numberFetched+" genes");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_REPEAT_FEATURE)){
      fireProgressEvent(new ProgressEvent(this,new Double(20.0),"Getting repeats..."));
      numberFetched = fetcher.addRepeatFeatures(curationSet, flatFeatures, location);
      logger.info("fetched "+numberFetched+" repeats ");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT)){
      fireProgressEvent(new ProgressEvent(this,new Double(30.0),"Getting dna-protein aligns..."));
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES);
      logger.info("fetching dna protein alignments for types: "+listToString(includedTypes));
      numberFetched = fetcher.addProteinAlignFeatures(curationSet, flatFeatures, location, includedTypes);
      logger.info("fetched "+numberFetched+" dna protein alignments");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_DNA_DNA_ALIGNMENT)){
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_DNA_DNA_ALIGNMENT_TYPES);
      fireProgressEvent(new ProgressEvent(this,new Double(40.0),"Getting dna-dna aligns ..."));
      logger.info("fetching dna dna alignments for types: "+listToString(includedTypes));
      numberFetched = fetcher.addDnaAlignFeatures(curationSet, flatFeatures, location, includedTypes);
      logger.info("fetched "+numberFetched+" dna dna alignments");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_PREDICTION_TRANSCRIPT)){
      fireProgressEvent(new ProgressEvent(this,new Double(50.0),"Getting prediction transcripts..."));
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_PREDICTION_TRANSCRIPT_TYPES);
      numberFetched = fetcher.addPredictionTranscripts(results, location, includedTypes);
      logger.info("fetched "+numberFetched+" prediction transcripts");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_FEATURE)){
      fireProgressEvent(new ProgressEvent(this,new Double(60.0),"Getting simple features..."));
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_FEATURE_TYPES);
      numberFetched = fetcher.addSimpleFeatures(curationSet, flatFeatures, location, includedTypes);
      logger.info("fetched "+numberFetched+" simple features");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_CONTIG_FEATURE)){
      fireProgressEvent(new ProgressEvent(this,new Double(70.0),"Getting contigs..."));
      numberFetched = fetcher.addContigFeatures(curationSet, flatFeatures, location);
      logger.info("fetched "+numberFetched+" contigs");
    }

    if(getStateAsBoolean(StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE)){
      fireProgressEvent(new ProgressEvent(this,new Double(80.0),"Getting protein annotatinos..."));
      numberFetched = fetcher.addProteinAnnotations(curationSet, flatFeatures, returnedGenes);
      logger.info("fetched "+numberFetched+" protein annotations");
    }
    
    if(getStateAsBoolean(StateInformation.INCLUDE_DITAG_FEATURE)){
      fireProgressEvent(new ProgressEvent(this,new Double(90.0),"Getting ditag features..."));
      includedTypes = EnsJAdapterUtil.getPrefixedProperties(getStateInformation(), StateInformation.INCLUDE_DITAG_TYPES);
      numberFetched = fetcher.addDitagFeatures(curationSet, flatFeatures, location, includedTypes);
      logger.info("fetched "+numberFetched+" ditag features");
    }

    (new EnsJFeatureSetBuilder()).makeSetFeatures(
      results, 
      new Vector(flatFeatures), apollo.config.Config.getPropertyScheme()
    );
    
    return results;
  }
  
  private String listToString(List list){
    String returnString = "";
    int listSize = list.size();
    for(int i=0; i<listSize; i++){
      returnString+=list.get(i).toString();
      if (i<listSize-1) returnString+=", ";
    }
    return returnString;
  }
  
  public StrandedFeatureSet getAnnotations(){
    StrandedFeatureSet annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    return annotations;
  }
  
  /** 
   * This is queried by Apollo's FileMenu
  **/
  public DataInputType getInputType(){
    return DataInputType.FILE;
  }
  
  public Properties getStateInformation() {
    return _stateInformation;
  }
  
  /**
   * Validates that the db information is present
  **/
  private void validateDatabaseProperties() throws apollo.dataadapter.ApolloAdapterException{
    String host = getStateInformation().getProperty(StateInformation.HOST);
    String port = getStateInformation().getProperty(StateInformation.PORT);
    String database = getStateInformation().getProperty(StateInformation.DATABASE);
    
    if(isNull(host)){
      throw new apollo.dataadapter.ApolloAdapterException("database host name must be provided");
    }else if(isNull(port)){
      throw new apollo.dataadapter.ApolloAdapterException("database port  must be provided");
    }else if(isNull(database)){
      throw new apollo.dataadapter.ApolloAdapterException("ensembl database name must be provided");
    }
  }

  private HashMap parseRegionString() throws apollo.dataadapter.ApolloAdapterException{
    Pattern pattern;
    Matcher matcher;
    String region = getStateInformation().getProperty(StateInformation.REGION);
    HashMap returnMap = new HashMap();
    
    String id;
    String version;
    String coordSystem;
    String seqRegion;
    int start = 0;
    int end = 0;

    pattern = Pattern.compile(REGION_PATTERN);
    matcher = pattern.matcher(region);
    
    if(matcher.matches()){
      version = matcher.group(1);
      coordSystem = matcher.group(2);
      seqRegion = matcher.group(3);
      try{
        start = Integer.valueOf(matcher.group(4)).intValue();
      }catch(NumberFormatException exception){
        throw new apollo.dataadapter.ApolloAdapterException("start: "+start+" cannot be parsed into an integer");
      }
      
      try{
        end = Integer.valueOf(matcher.group(5)).intValue();
      }catch(NumberFormatException exception){
        throw new apollo.dataadapter.ApolloAdapterException("end: "+end+" cannot be parsed into an integer");
      }

      returnMap.put(VERSION, version);
      returnMap.put(COORD_SYSTEM, coordSystem);
      returnMap.put(SEQ_REGION, seqRegion);
      returnMap.put(START, new Integer(start));
      returnMap.put(END, new Integer(end));
      
    }else{
      
      pattern = Pattern.compile(REGION_PATTERN_2);
      matcher = pattern.matcher(region);
      if(matcher.matches()){

        version = matcher.group(1);
        coordSystem = matcher.group(2);
        seqRegion = matcher.group(3);
        try{
          start = Integer.valueOf(matcher.group(4)).intValue();
        }catch(NumberFormatException exception){
          throw new apollo.dataadapter.ApolloAdapterException("start: "+start+" cannot be parsed into an integer");
        }

        try{
          end = Integer.valueOf(matcher.group(5)).intValue();
        }catch(NumberFormatException exception){
          throw new apollo.dataadapter.ApolloAdapterException("end: "+end+" cannot be parsed into an integer");
        }

        returnMap.put(VERSION, version);
        returnMap.put(COORD_SYSTEM, coordSystem);
        returnMap.put(SEQ_REGION, seqRegion);
        returnMap.put(START, new Integer(start));
        returnMap.put(END, new Integer(end));

      }else{
      
        pattern = Pattern.compile(REGION_PATTERN_3);
        matcher = pattern.matcher(region);
        if(matcher.matches()){

          version = null;
          coordSystem = null;
          seqRegion = matcher.group(1);

          try{
            start = Integer.valueOf(matcher.group(2)).intValue();
          }catch(NumberFormatException exception){
            throw new apollo.dataadapter.ApolloAdapterException("start: "+start+" cannot be parsed into an integer");
          }

          try{
            end = Integer.valueOf(matcher.group(3)).intValue();
          }catch(NumberFormatException exception){
            throw new apollo.dataadapter.ApolloAdapterException("end: "+end+" cannot be parsed into an integer");
          }

          returnMap.remove(VERSION);
          returnMap.remove(COORD_SYSTEM);
          returnMap.put(SEQ_REGION, seqRegion);
          returnMap.put(START, new Integer(start));
          returnMap.put(END, new Integer(end));
          
        }else{

          logger.debug("Doing id pattern match with: " + region);
          pattern = Pattern.compile(ID_PATTERN);
          matcher = pattern.matcher(region);

          if(matcher.matches()){
            id = matcher.group(1);
            logger.debug("id from matcher = " + id + " pattern = " + 
                         ID_PATTERN + " region string = " + region);
            returnMap.put(ID, id);
          }else{
            throw new apollo.dataadapter.ApolloAdapterException(
              "the input region: "+region+" doesnt match either REGION: (version)--(coordsystem):seqregion:start-end or ID: id"
            );
          }
        }
      }
    }

    return returnMap;
  }

  private boolean isNull(String test){
    if(test == null || test.trim().length() <= 0){
      return true;
    }else{
      return false;
    }
  }

  private void initialiseDriver() throws ConfigurationException{
    logger.info("Initialising driver");

    // Hacky stuff to set schema for 32 and 34
    String host = getStateInformation().getProperty(StateInformation.HOST);
    String port = getStateInformation().getProperty(StateInformation.PORT);
    String database = getStateInformation().getProperty(StateInformation.DATABASE);
    String user = getStateInformation().getProperty(StateInformation.USER);
    String password = getStateInformation().getProperty(StateInformation.PASSWORD);

    Connection conn = EnsJConnectionUtil.getConnection(EnsJConnectionUtil.DRIVER,host,port,database,user,password);
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);

    // What ensj expects
    if (schema == 32) schema = 33;

    Properties props = getStateInformation();
    
    props.setProperty(StateInformation.SCHEMA_VERSION, "" + schema);

    logger.info("Setting schema to " + schema + " for db " + database + " on " + host + " port " + port );


    LoggingManager.configure(getStateInformation().getProperty(StateInformation.LOGGING_FILE));
    setDriver((Driver)DriverManager.load(props));
    logger.info("Done initialising driver");
  }

  public Driver getDriver(){
    return _driver;
  }
  
  private void setDriver(Driver newValue){
    _driver = newValue;
  }

  private boolean getStateAsBoolean(String type){
    return PropertiesUtil.booleanValue(getStateInformation(), type, false);
  }
  
  /**
   * There is no longer a notion of a 'sequence' database - it is now assumed to
   * come from the standard db (use the BuilderAdapter to stack up results from different db's).
  **/
  public SequenceI getSequence(DbXref dbxref) throws apollo.dataadapter.ApolloAdapterException {
    Properties sequenceProperties = getStateInformation();

    try {

      Location location = getLocationForInputRegion();

      AlternateEnsJSequence sequence =
        new AlternateEnsJSequence(
          getStateInformation().getProperty(StateInformation.REGION),
          Config.getController(),
          location,
          sequenceProperties
        );

      sequence.getCacher().setMaxSize(1000000);

      return sequence;
      
    } catch (Exception exception) {
      exception.printStackTrace();
      throw new apollo.dataadapter.ApolloAdapterException("Problem fetching main sequence: " + exception.getMessage(), exception);
    }
  }//end getSequence

  public void setSequence(apollo.datamodel.FeaturePair pair, CurationSet curationSet) {
    String      name = pair.getHname();
    SequenceI   sequence  = curationSet.getSequence(name);
    if (sequence == null) {
      //Controller controller = apollo.config.Config.getController();
      //sequence = new PFetchSequence(name,controller);
      //curationSet.addSequence(sequence);
    }
    pair.getHitFeature().setRefSequence(sequence);
  }
}
