package apollo.dataadapter.chado;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;
import org.bdgp.util.ProgressEvent;

import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.DataInputType.UnknownTypeException;
import apollo.dataadapter.NotImplementedException;
import apollo.dataadapter.StateInformation;
import apollo.dataadapter.chado.jdbc.ChadoInstance;
import apollo.dataadapter.chado.jdbc.JDBCTransactionWriter;
import apollo.dataadapter.chado.jdbc.PureJDBCTransactionWriter;
import apollo.dataadapter.chado.jdbc.JdbcChadoAdapter;
import apollo.dataadapter.chado.jdbc.JdbcChadoWriter;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.SequenceI;
import apollo.editor.TransactionManager;

/**
 * <p>
 * A data adapter that allows Apollo to communicate with a Chado-compliant relational 
 * database.   Note that this class is one of two adapter classes required by the 
 * Apollo data adapter specification; the other one is ChadoAdapterGUI.
 * </p>
 * <p>
 * This data adapter was written primarily as an exercise to help me to learn the 
 * Chado schema (as it was implemented at TIGR in late 2003.)  Therefore there are 
 * numerous caveats, of which a few are listed below:
 * </p>
 *
 * <ul>
 *  <li>
 *    The adapter has been tested only with Sybase, although it was designed with other DBMSs in mind.
 *  </li>
 *  <li>
 *    The adapter supports two versions of the Chado schema, which we refer to as "pre_fmb" and "fmb"; 
 *    the latter should correspond to the "FlyBase Migration Build" freeze of Chado.  The adapter 
 *    looks for a table present in one version but not the other in order to determine the schema version.
 *  </li>
 *  <li>
 *    The adapter only supports read access.  Write access will hopefully be forthcoming.
 *  </li>
 *  <li>
 *    The adapter currently allows the user to select only a single sequence for display.
 *    (By "sequence" we mean any Chado feature.)
 *  </li>
 *  <li>
 *    The adapter loads the entire sequence and all of its annotations before displaying anything.
 *    (i.e., there is no support for on-demand or delayed data loading.)
 *  </li>
 *  <li>
 *     Extensive hard-coding can be observed in the set of annotations that the data adapter 
 *     attempts to load.  Most of these data types (except for the gene models themselves,
 *     which should follow the Chado spec. fairly closely) are also TIGR-specific.
 *  </li>
 * </ul>
 *
 * <p>
 * Finally, here are some thoughts on two additional topics; the first is the question of SO-compliance,
 * and the second is the issue of how to refactor the adapter to make it more configurable.  1. SO 
 * compliance: the adapter is definitely *not* SO-compliant, although it could probably be made to work 
 * with a SO-enabled version of Chado simply by looking up the cvterm_ids for the crucial elements of the 
 * central dogma on which the gene representation depends (e.g. exon, transcript, etc.)  Handling inheritance 
 * properly and dynamically discovering the relationships between SO terms by querying the database, however, 
 * is likely to be a more challenging undertaking.  In other words, getting it to work should be easy, but
 * getting it to work right is likely to be hard.  (Isn't this always the way?)  2. Making the adapter more
 * configurable.  Good progress has already been made on this front in terms of setting up the architecture
 * to allow connections to different types of Chado databases (Sybase, PostgresQL, etc.), although this
 * has not been tested.  What the adapter really needs is a way for the user (or Apollo "administrator"
 * or application programmer) to configure what annotations should be read from different data sources.
 * One thing that significantly impeded efforts to use Apollo here at TIGR is the fact that Apollo supports
 * only one tiers file for each data adapter.  This is fine if you only have a couple of different databases,
 * but we have tens or hundreds, each with different data, and we would really like to be able to 
 * configure the display on a finer-grained basis than is allowed by the current setup.
 * </p>
 *
 * @see ChadoAdapterGUI
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.53 $ $Date: 2009/06/20 03:49:40 $ $Author: jcrabtree $
 */
public class ChadoAdapter extends AbstractApolloAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoAdapter.class);

  // These constants are used by the GUI to communicate connection info using <code>setStateInformation</code>.
  public static String DRIVER_CLASS = "DRIVER_CLASS";
  public static String JDBC_URL = "JDBC_URL";
  public static String CHADO_DB = "CHADO_DB";
  public static String USERNAME = "USERNAME";
  public static String PASSWORD = "PASSWORD";
    
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * The current driver properties set by the GUI in the most recent call to <code>setStateInformation</code>.
   */
  protected Properties stateInfo = new Properties();

  /**
   * List of predefined Chado databases read from an adapter-specific configuration file and presented 
   * to the user in the GUI.  The user may also input connection parameters for a database not in 
   * this list.
   */
  private ChadoDatabase[] databases;

  /** The ChadoDatabase from databases to use if none specified 
      (handy for command line) */
  private ChadoDatabase defaultDatabase;
  /** The ChadoDatabase to use */
  private ChadoDatabase activeDatabase;

  private boolean flatFileWriteMode = false;
  private boolean pureJdbcWriteMode = false;
  
  private String gffSource = null;

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  public ChadoAdapter() {
    // Adapter name to be shown in the drop-down menu that appears when the user chooses to read or write data.
    setName("Chado database");
    logger.debug("in ChadoAdapter constructor, about to call readConfigFile");
    XmlConfigFileParser parser = new XmlConfigFileParser();
    ChadoDatabase[] databases = parser.readConfigFile();
    setDatabases(databases);
  }

  /** Sets databases instance var & sets default db if there is one */
  private void setDatabases(ChadoDatabase[] databases) {
    this.databases = databases;

    // look for & set default db (used for command line)
    for (int i=0; i<databases.length; i++) {
      if (databases[i].isDefaultDatabase()) {
        setDefaultDatabase(databases[i]);
        return; // we're done
      }
    }
  }

  // -----------------------------------------------------------------------
  // AbstractApolloAdapter
  // -----------------------------------------------------------------------
   
  /** Return true - this means that the features potentially have synteny
      data associated with them and then can be used in the synteny viewer
      this follows the game model of embedding links in the features */
  public boolean hasLinkData() {
    return  true; 
  }

  public Properties getStateInformation() {
    return (this.stateInfo == null) ? null : (Properties)(this.stateInfo.clone());
  }
    
  // JC: It's not clear why there isn't a strongly-typed interface for passing configuration
  // info. between the GUI and the data adapter class.  But that's the way it is.
  public void setStateInformation(Properties p) {
    this.stateInfo = p;
    String typeString = p.getProperty(StateInformation.INPUT_TYPE);
    try {
      DataInputType type = DataInputType.stringToType(typeString);
      String inputString = p.getProperty(StateInformation.INPUT_STRING);
      setDataInput(new DataInput(type,inputString));
    }
    catch (UnknownTypeException e) {
      logger.error("Unknown type encountered trying to setDataInput in ChadoAdapter", e);
    }
  }
  
  /**
   * A chado database can set its own style. Override the superclass method
   * to get style from the ChadoDatabase configuration file.
   */
  public Style getStyle() {
    String styleFileName = null; //stateInfo.getProperty(STYLE);
    if (getActiveDatabase() != null)
      styleFileName = getActiveDatabase().getStyleFileName();
    if (styleFileName == null)
      style = getDefaultStyle();
    else { // does config cache these?
      style = Config.createStyle(styleFileName);
    } 
    return style;
  }

    
  // Read annotation and/or analysis results from the database
  public CurationSet getCurationSet() throws ApolloAdapterException {
    
    // Superclass method
    clearOldData();

    DataInput dataInput = getDataInput();

    // if type of data input is configured as a location, makes it a location
    // if type of input is loc/BASEPAIR_RANGE, makes sure has loc so type for db
    logger.debug("ChadoAdapter active db="+getActiveDatabase()+", instance="+getActiveDatabase().getChadoInstance());
    getActiveDatabase().getChadoInstance().checkForLocation(dataInput);
    //DataInputType inputType = getInputType(); // from AbstractApolloAdapter
    String soType = dataInput.getSoType();
    if (soType == null) {
      throw new ApolloAdapterException("failed to get seq type from input with type=" + dataInput.getType());
    }
    
    //String seqId = this.stateInfo.getProperty(INPUT_ID);
    String seqId = dataInput.getSeqId();
    StringBuilder realSeqId = new StringBuilder();

    // Create a JdbcChadoAdapter; all the actual database communication is delegated
    // to this object, allowing us to create subclasses for different Chado 
    // implementations (e.g. Sybase, MySQL, PostgreSQL)
    // passes all the state info to it
    JdbcChadoAdapter jdbcAdap = getActiveDatabase().getJdbcChadoAdapter();
    jdbcAdap.setPropertyScheme(getStyle().getPropertyScheme());
    
    // Superclass method that displays a progress graph
    fireProgressEvent(new ProgressEvent(this, new Double(50.0), "Connecting to chado database"));

    // Check that we can connect to the database and find the requested sequence
    // Throws DataAdapterException if conn or seq invalid
    logger.info("checking that the database contains a sequence of type " + soType + " with name=" + seqId);
    jdbcAdap.validateConnectionAndSequence(soType, seqId, realSeqId);
    fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Connecting to chado database"));

    // Retrieve annotation and accompanying sequence
    CurationSet cset = null;
    if (dataInput.isRegion()) {
      cset = jdbcAdap.getCurationSetInRange(this, soType, dataInput.getRegion());
    }
    else {
      // if realSeqId is populated, that means we did a search by synonym, need to use the actual id
      cset = jdbcAdap.getCurationSet(this, soType, realSeqId.length() > 0 ? realSeqId.toString() : seqId);
    }

    return cset;
  }

  /** Writes back curation set via jdbc, using the curations sets transactions */
  public void commitChanges(CurationSet curation) 
    throws ApolloAdapterException 
  {
    ChadoDatabase activeDatabase = getActiveDatabase();
    ChadoInstance chadoInstance = activeDatabase.getChadoInstance();
        
    // -----------------------------------------------------------------------
    // OPTION 1: write *all* data to ad-hoc flat files (used for testing)
    // -----------------------------------------------------------------------
    if (flatFileWriteMode) {
      logger.info("ChadoAdapter: saving changes with ChadoAdHocFlatFileWriter");
      ChadoAdHocFlatFileWriter ffw = new ChadoAdHocFlatFileWriter();
      ffw.write(getDataInput().getFilename(), curation);
      logger.info("ChadoAdapter: save complete");
    }
    else {
      boolean copyOnWrite = chadoInstance.getPureJDBCCopyOnWrite();
      boolean noCommit = chadoInstance.getPureJDBCNoCommit();
      boolean useCDS = chadoInstance.getPureJDBCUseCDS();
      JdbcChadoWriter writer = new JdbcChadoWriter(this, getActiveDatabase().getJdbcChadoAdapter(),
          copyOnWrite, noCommit, useCDS, chadoInstance);
      if (getGFFSource() != null) {
        writer.setGFFSource(getGFFSource());
      }
      writer.write(curation);
    }
    
    /*
    // -----------------------------------------------------------------------
    // OPTION 2: commit transactions using PureJDBCTransactionWriter
    // -----------------------------------------------------------------------
    else if (chadoInstance.getPureJDBCWriteMode()) {
      logger.info("ChadoAdapter: saving changes with PureJDBCTransactionWriter");
      TransactionManager tm = curation.getTransactionManager();
      boolean copyOnWrite = chadoInstance.getPureJDBCCopyOnWrite();
      boolean noCommit = chadoInstance.getPureJDBCNoCommit();
      boolean useCDS = chadoInstance.getPureJDBCUseCDS();
      PureJDBCTransactionWriter writer = new PureJDBCTransactionWriter(this, getActiveDatabase().getJdbcChadoAdapter(), copyOnWrite, noCommit, useCDS);
      boolean retval = writer.commitTransactions(tm);

      if (retval) {
        // All transactions should be emptied after a successful write to the db.
        logger.info("save succeeded; clearing apollo transaction log.");
        tm.emptyTransactions();
      } else {
        // TODO - handle this better
        logger.error("save failed; not clearing apollo transaction log.");
      }
    }

    // -----------------------------------------------------------------------
    // OPTION 3: commit transactions using JDBCTransactionWriter
    // -----------------------------------------------------------------------
    else {
      logger.info("ChadoAdapter: saving changes with JDBCTransactionWriter");

      // coalesce transactions
      TransactionManager tm = curation.getTransactionManager();
      logger.debug("coalescing transactions with TransactionManager " + tm);
      tm.coalesce();

      // transform Apollo transactions -> JDBC/chado transactions
      JDBCTransactionWriter processor = new JDBCTransactionWriter(activeDatabase);
      ChadoTransactionTransformer transformer = new ChadoTransactionTransformer();

      // transformer needs 1,3 level annot types, should come from style or tiers or SO
      // for now comes from chado config

      if (chadoInstance == null) {
        logger.warn("no chado instance to get 1,3 level annots from");
      }
      else {
        transformer.setChadoInstance(chadoInstance);
      }

      logger.debug("setting JDBCTransactionWriter transformer to " + transformer + " chromosome to " + curation.getChromosome());
      processor.setTransformer(transformer);
      processor.setMapID(curation.getChromosome());

      // top level feat type now comes from config
      if (chadoInstance != null) processor.setMapType(chadoInstance.getTopLevelFeatType());

      try {
        logger.debug("committing transactions");
        processor.commitTransactions(tm);
        // All transactions should be emptied after a successful write to the db.
        logger.debug("calling TransactionManager.emptyTransactions()");
        tm.emptyTransactions();
      }
      catch (Exception e) {
        throw new ApolloAdapterException(e);
      }
      logger.info("ChadoAdapter: save complete");
    }
    */
  }

  // Not yet implemented in this adapter
  public Boolean addToCurationSet()
    throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  // Not yet implemented in this adapter
  public SequenceI getSequence(String id) throws ApolloAdapterException {
    logger.error("not implemented: getSequence id=" + id);
    throw new NotImplementedException();
  }
    
  // Not yet implemented in this adapter
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    logger.error("not implemented: getSequence dbxref=" + dbxref);
    throw new NotImplementedException();
  }
    
  // Not yet implemented in this adapter
  public SequenceI getSequence(DbXref dbxref, int start, int end)
    throws ApolloAdapterException {
    logger.error("not implemented: getSequence dbxref=" + dbxref + " start=" + start + " end=" + end);
    throw new NotImplementedException();
  }
    
  // Not yet implemented in this adapter
  public Vector getSequences(DbXref[] dbxref) throws ApolloAdapterException {
    logger.error("not implemented: getSequence dbxrefs=" + dbxref);
    throw new NotImplementedException();
  }
    
  // Not yet implemented in this adapter
  public Vector getSequences(DbXref[] dbxref, int[] start, int[] end)
    throws ApolloAdapterException {
    logger.error("not implemented: getSequence dbxrefs=" + dbxref + " starts=" + start + " ends=" + end);
    throw new NotImplementedException();
  }
    
  // Not yet implemented in this adapter
  public String getRawAnalysisResults(String id) 
    throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }


  // -----------------------------------------------------------------------
  // org.bdgp.io.AbstractDataAdapter
  // -----------------------------------------------------------------------

  /**
   * Array containing the list of IO operations supported by this adapter; currently the
   * adapter supports read-only access to a Chado database.
   */
  protected IOOperation[] SUPPORTED_OPS = {
    ApolloDataAdapterI.OP_READ_DATA,
    // Add these lines back in when the corresponding support is added:
    //	ApolloDataAdapterI.OP_READ_SEQUENCE,
    ApolloDataAdapterI.OP_WRITE_DATA // add only for fb?
    //	ApolloDataAdapterI.OP_APPEND_DATA,
  };
    
  // JC: not sure where this is used
  public String getType() { return "Chado Adapter"; }

  // Return the list of ops that can be passed to the constructor of this class
  public IOOperation [] getSupportedOperations() { return this.SUPPORTED_OPS; }

  // "Called when adapter is created and added to the registry (see org.bdgp.io.DataAdapterChooser)"
  // Nothing needs to be done here right now.
  public void init() {}

  private ChadoAdapterGUI chadoAdapterGUI=null;
  // Factory method that creates a new UI (i.e., an instance of the corresponding GUI
  // class) for a specified data adapter operation.  Creates a new object on every call.
  // I dont think it should create a new one everytime - probably only needs one
  // per operation (could even parameterize op)
  // getUI gets called 6 times (who knows why?) when data adapter chooser comes up
  // no need to remake ui 6 times (should do this for all adapters - would speed up
  // chooser coming up)
  // change this if more operations get supported (hash on op)
  public DataAdapterUI getUI(IOOperation op) {
    // only read supported at this point
    //if (op != ApolloDataAdapterI.OP_READ_DATA) return null; // shouldnt happen
    if (chadoAdapterGUI == null)
      chadoAdapterGUI = new ChadoAdapterGUI(op, this.databases);
    else 
      chadoAdapterGUI.setOperation(op);
    return chadoAdapterGUI;
  }

//   public void setChadoInstance(ChadoInstance chadoInstance) {
//     this.chadoInstance = chadoInstance;
//   }

  // -----------------------------------------------------------------------
  // ChadoAdapter - protected methods
  // -----------------------------------------------------------------------
    
  /**
   * Read a list of sequence IDs and descriptions into two parallel Vectors.  The sequence IDs must be
   * values from the chado column feature.uniquename, but the descriptions can be any unique human-readable
   * string that adequately describes the corresponding sequence.
   *
   * @param featType      The type of sequence to retrive from the feature table (e.g., 'assembly', 'super-contig')
   * @param uniquenames   Vector that will hold the sequence IDs (values from the chado column feature.uniquename)
   * @param descriptions  Vector that will hold the human-readable descriptions for the sequences named in <code>uniquenames</code>
   */
  protected void getSequenceList(String featType, Vector uniquenames, Vector descriptions) throws ApolloAdapterException {
    JdbcChadoAdapter chadoAdapter = getActiveDatabase().getJdbcChadoAdapter();
    chadoAdapter.getAllChadoSequencesByType(featType, uniquenames, descriptions);
  }

  /** chaod-adapter.xml config file specifies a default database to use for command
      line loading and saving */
  void setDefaultDatabase(ChadoDatabase defaultDatabase) {
    this.defaultDatabase = defaultDatabase;
    // should we just set state info with this??
  }

  /** Should there be a default default db? like the 1st db? */
  private ChadoDatabase getDefaultDatabase() { return defaultDatabase; }

  /** Set the chado database that is currently the active 
      (ie user selected this db from gui) */
  void setActiveDatabase(ChadoDatabase activeDatabase) {
    this.activeDatabase = activeDatabase;
  }

  /** Returns active database. if active database has not been set, return default
      database */
  public ChadoDatabase getActiveDatabase() {
    if (activeDatabase == null)
      return defaultDatabase;
    return activeDatabase;
  }

  /**
   * Created to modify the password from the command line
   * @param pass : Default Db new password
   */
  public void setDbPassForDefaultDb(String pass) {
    getDefaultDatabase().setPassword(pass);
  }

  /**
   * Created to modify the login/username from the command line
   * @param login : Default Db new login/username.
   */
  public void setDbLoginForDefaultDb(String login) {
    getDefaultDatabase().setLogin(login);
  }

  /**
   * Used to set the flat file write mode flag from the command line.
   * @param login : Default Db new login/username.
   */
  public void setFlatFileWriteMode(boolean flatFileWriteMode) {
    this.flatFileWriteMode = flatFileWriteMode;
  }
  
  public String getGFFSource()
  {
    return gffSource;
  }
  
  public void setGFFSource(String gffSource)
  {
    this.gffSource = gffSource;
  }
  
  /**
   * 
   * @param chadoDb
   * @return ChadoDatabase
   * @throws Exception
   */
  private ChadoDatabase getDatabaseByName(String chadoDb) throws Exception {
    ChadoDatabase ch = null;
    for (int i=0;i<databases.length; i++){
      if (databases[i].chadoDb.equals(chadoDb))
        ch=databases[i];
    }
    if (ch == null)
      throw new Exception("Unable to find the cahdo database named : "+ chadoDb); 
    return ch;
  }
  
  /**
   * Set password to pass for the chadoDB identified by chadoDb
   * Created to modify the password from the command line
   * @param chadoDb
   * @param pass
   * @throws Exception
   */
  public void setDbPassForDb(String chadoDb, String pass) throws Exception {
    ChadoDatabase ch = getDatabaseByName(chadoDb); 
    //Let the caller handle the exception
    ch.setPassword(pass);
  }
  
  /**
   * Set password to pass for the chadoDB identified by chadoDb 
   * and set this DB as active
   * Created to modify the password from the command line
   * @param chadoDb
   * @param pass
   * @throws Exception
   */
  public void setDbPassAndActiveForDb(String chadoDb, String pass) throws Exception {
    ChadoDatabase ch = getDatabaseByName(chadoDb); 
    //Let the caller handle the exception
    ch.setPassword(pass);
    this.setActiveDatabase(ch);
  }  

//   List getLocationTopLevelSeqIds(ChadoDatabase selectedDb,String seqType) 
//     throws ApolloAdapterException {
    
//     JdbcChadoAdapter jdbcAdap = null;
//     if (selectedDb.queryForSeqIds(seqType))
//       jdbcAdap = getJdbcAdapter(); // throws ex
//     return selectedDb.getLocationTopLevelSeqIds(seqType);
//   }

  JdbcChadoAdapter getJdbcAdapter() throws ApolloAdapterException {
    return getActiveDatabase().getJdbcChadoAdapter();
  }
}
