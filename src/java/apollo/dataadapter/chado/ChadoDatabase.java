package apollo.dataadapter.chado;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.sql.SQLException;

import apollo.config.Config;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.chado.jdbc.ChadoInstance;
import apollo.dataadapter.chado.jdbc.JdbcChadoAdapter;

import org.apache.log4j.*;

/**
 * Connection and configuration information for a Chado database.
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.24 $ $Date: 2008/08/26 19:42:31 $ $Author: gk_fan $
 */
public class ChadoDatabase {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoDatabase.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * Short (and preferably unique) user-readable identifier for the database.
   */
  protected String name;

  /**
   * Fully-qualified name of a Java class that extends apollo.dataadapter.chado.jdbc.JdbcChadoAdapter;
   * used to implement DBMS-specific features or optimizations.
   */
  protected String adapterClassName;

  /**
   * JDBC connection URL
   */
  protected String jdbcUrl;

  /**
   * Name of the specific Chado database instance/schema on the database server.  The
   * interpretation of this value may vary slightly depending on the DBMS.  In Oracle,
   * for example, this will most likely be a "schema" (i.e., username), whereas in Sybase 
   * or MySQL it will more likely be a database name.
   */
  protected String chadoDb;

  /**
   * String used to limit the sequences listed in the pull-down menu presented by the 
   * data adapter.  Implemented via a 'like' query on organism.common_name using
   * this string.
   */
  protected String organismLike;
  // TODO - generalize/improve this mechanism

  /** 
   * Optional login to use for accessing db. If none is provided in config, 
   * user must provide login. 
   */
  private String login;
  
  /**
   * Database password that corresponds to <code>login</code>
   */
  private String password = null;

  /**
   * Name of the style file to be used by this ChadoDatabase.
   */
  private String styleName;

  /** 
   * SO/Chado type of top level feature (hopefully there's only one top in db) 
   */
  private String topLevelFeatType;

  // ChadoInstance for DAO
  private ChadoInstance chadoInstance;
  
  /** 
   * True if this is the default database. Loading and saving from the command line
   * uses the default db if not specified. Not sure if this belongs here. 
   * There should only be one default database, which isn't enforced here. 
   */
  private boolean isDefaultDatabase = false;

  /**
   * Whether the user should be allowed to enter a login/db username that is
   * different from the default value.
   */
  private boolean allowLoginInput = true;

  /**
   * Whether the user should be allowed to enter a password that is
   * different from the default value.
   */
  private boolean allowPasswordInput = true;
  
  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  /**
   * Default constructor.
   */
  public ChadoDatabase() {}

  /**
   * @param name               Short (and preferably unique) user-readable identifier for the database.
   * @param adapterClassName   Name of a Java class that extends apollo.dataadapter.chado.jdbc.JdbcChadoAdapter.
   * @param jdbcUrl            JDBC connection URL.
   * @param chadoDb            Name of the specific Chado database instance/schema on the database server.
   * @param login              Database login
   * @param organismLike       String used to limit the sequences listed in the pull-down menu, via a 'like' query on organism.common_name.
   */
  public ChadoDatabase(String name, String adapterClassName, String jdbcUrl,
                       String chadoDb, String login, String organismLike) {
    this.name = name;
    this.adapterClassName = adapterClassName;
    this.jdbcUrl = jdbcUrl;
    this.chadoDb = chadoDb;
    this.login = login;
    this.organismLike = organismLike;
  }

  // -----------------------------------------------------------------------
  // ChadoDatabase - setters/getters
  // -----------------------------------------------------------------------

  public String getName() { return this.name; }
  
  /**
   * Set the name that will be displayed in the chado connection pane.
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * @return The name of a Java class that extends apollo.dataadapter.chado.jdbc.JdbcChadoAdapter.
   */
  public String getAdapterClassName() { return this.adapterClassName; }
  
  public void setAdapterClassName(String clsName) {
    adapterClassName = clsName;
  }
  
  /**
   * @return The JDBC URL used to connect to the database
   */
  public String getJdbcUrl() { return this.jdbcUrl; }
  
  public void setJdbcUrl(String url) {
    jdbcUrl = url;
  }
  
  /**
   * @return The database/schema name.
   */
  public String getChadoDb() { return this.chadoDb; }
  
  public void setChadoDb(String dbName) {
    this.chadoDb = dbName;
  }
  
  /**
   * @return Database username/login.
   */
  public String getLogin() { return login; }
  
  public void setLogin(String dbUser) { 
    this.login = dbUser;
  }
  
  boolean hasLogin() { return login != null; }

  public String getPassword() { return password; }

  void setPassword(String password) {
    this.password = password;
  }

  public String getOrganismLike() { return this.organismLike; }

  public void setOrganismLike(String like) { 
    organismLike = like; 
  }
  
  public ChadoInstance getChadoInstance() {
    return this.chadoInstance;
  }
  public void setChadoInstance(ChadoInstance chadoInstance) {
    this.chadoInstance = chadoInstance;
  }
  
  public String getStyleFileName() {
    return this.styleName;
  }

  public void setStyleFileName(String fileName) {
    this.styleName = fileName;
  }

  boolean isDefaultDatabase() { return isDefaultDatabase; }

  void setIsDefaultDatabase(boolean isDefault) {
    isDefaultDatabase = isDefault;
  }

  public boolean getAllowLoginInput() {
    return this.allowLoginInput;
  }

  public void setAllowLoginInput(boolean b) {
    this.allowLoginInput = b;
  }

  public boolean getAllowPasswordInput() {
    return this.allowPasswordInput;
  }

  public void setAllowPasswordInput(boolean b) {
    this.allowPasswordInput = b;
  }
  
  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  /** 
   * Get top level seq ids (e.g. chromosomes), either from the database
   * or the config. file.
   */
  List getLocationTopLevelSeqIds(String typeName) {
    SeqType st = getChadoInstance().getSeqType(typeName);
    if (st == null)
      return new ArrayList(0);
    return st.getLocationTopLevelSeqIds(this);
  }

  /**
   * Factory method that returns an instance of JdbcChadoAdapter, or one of its subclasses; 
   * which subclass is determined by the GUI.  The data adapter configuration file read 
   * by <code>readConfigFile</code> allows the GUI to present the user with a list of available
   * Chado databases, each of which will have an associated JdbcChadoAdapter subclass.
   *
   * @return A JdbcChadoAdapter instance capable of communicating with the Chado database 
   * instance currently selected by the user in the GUI.
   */
  JdbcChadoAdapter getJdbcChadoAdapter() throws ApolloAdapterException {
    Class dc = null;
    try {
      dc = Class.forName(adapterClassName);
    } catch (ClassNotFoundException cnfe) {
      throw new ApolloAdapterException("Unable to find Chado driver class " + adapterClassName);
    }

    // Attempt to instantiate an object of the appropriate class; by convention any subclass
    // of JdbcChadoAdapter will have a JavaBean-style 0-argument constructor.
    try {
      JdbcChadoAdapter chadoAdapter = (JdbcChadoAdapter) (dc.newInstance());
      // Pass some essential connection information along to the adapter object.
      chadoAdapter.initWithException(jdbcUrl, chadoDb, login, password, organismLike);
      chadoAdapter.setChadoInstance(chadoInstance);
      return chadoAdapter;
    }
    catch (IllegalAccessException iae) {
      logger.error("IllegalAccessException initializing JdbcChadoAdapter", iae);
    }
    catch (InstantiationException ie) {
      logger.error("InstantiationException initializing JdbcChadoAdapter", ie);
    }
    catch (SQLException se) {
      logger.error("SQLException initializing JdbcChadoAdapter", se);
      throw new ApolloAdapterException("Unable to access database: " + se.getMessage());
    }
    return null;
  }
}
