package apollo.dataadapter.chado.jdbc;

import java.io.*;
import java.net.*;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Enumeration;
import java.security.MessageDigest;

import org.apache.log4j.*;
import org.apache.log4j.xml.*;
import org.bdgp.util.DNAUtils;

import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.Region;
import apollo.dataadapter.chado.ChadoAdapter;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.RangeI;
import apollo.datamodel.Score;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.Synonym;
import apollo.datamodel.Transcript;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.UserName;
import apollo.util.FeatureList;

/**
 * Abstract superclass that encapsulates vendor-independent routines 
 * for interacting with Chado-compliant relational databases.  Used by
 * apollo.dataadapter.chado.ChadoAdapter to implement the database access
 * layer of an Apollo/Chado adapter.
 *
 * @see apollo.dataadapter.chado.ChadoAdapter
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.205 $ $Date: 2009/02/24 16:58:44 $ $Author: gk_fan $
 */
public abstract class JdbcChadoAdapter {

  // TODO - Extract all the embedded SQL in this class and its subclasses into a set of more easily-edited files?
  // TODO - Refactor this class to make it smaller, particularly now that we have both read and write methods
  // TODO - Many of the methods appear to accept a JDBCConnection parameter, others use getConnection(). See
  //        whether it's possible to standardize this

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(JdbcChadoAdapter.class);

  /**
   * Path to a file where all SQL statements are logged.
   */
  protected static String sqlLogPath = null;

  /**
   * Path to a file where all committed Apollo transactions are logged
   */
  protected static String txnLogPath = null;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * JDBC URL of a Chado-compliant database (or of a server/DBMS that hosts such a database.)
   */
  protected String jdbcUrl;

  /**
   * Schema/database name of the Chado database instance.
   */
  protected String chadoDb;

  /**
   * Database username.
   */
  protected String username;

  /**
   * Database password
   */
  protected String password;

  /**
   * Used by <code>getAllChadoSequencesByType</code> if not null.
   */
  protected String organismLike;

  /**
   * Chado schema version - should this be merged with chado instance?
   */
  protected SchemaVersion chadoVersion;

  /**
   * Cached copy of the 'feature' cvterms in the Chado cvterm table .
   */
  private Map featureCvTerms;

  /**
   * Cached copy of the 'relationship' cvterms in the Chado cvterm table .
   */
  private Map relationshipCvTerms;

  /**
   * Cached copy of the 'property type' cvterms in the Chado cvterm table .
   */
  private Map propertyTypeCvTerms;

  private Map nullCvTerms;

  private ChadoInstance chadoInstance;

  /** needed to see if type is synteny linked */
  private PropertyScheme propertyScheme;

  // -----------------------------------------------------------------------
  // JdbcChadoAdapter
  // -----------------------------------------------------------------------

  /**
   * This method must be called before calling any of the other methods of this class (or those of its subclasses.)  
   * This will typically be done by an instance of <code>apollo.dataadapter.chado.ChadoAdaptor</code>.
   *
   * @param jdbcUrl       JDBC URL of a Chado-compliant database (or of a server/DBMS that hosts such a database.)
   * @param chadoDb       Schema/database name of the Chado database instance.
   * @param username      Database username.
   * @param password      Database password.
   * @param organismLike  Only sequences that match this string are retrieved by <code>getAllChadoSequencesByType</code>.
   */
  public void init(String jdbcUrl, String chadoDb, String username, String password, String organismLike)
  {
    this.jdbcUrl = jdbcUrl;
    this.chadoDb = chadoDb;
    this.username = username;
    this.password = password;
    this.organismLike = organismLike;
    logger.info("connecting to database " + chadoDb + " as user " + username + " at " + jdbcUrl);

    // try to determine the Chado schema version
    // TODO - this needs to be improved or, preferably, replaced entirely by configurable options
    Connection conn = this.getConnection();
    this.chadoVersion = SchemaVersion.guessSchemaVersion(conn);
    logger.debug("guessed schema version = " + this.chadoVersion);
  }

  /**
   * This method must be called before calling any of the other methods of this class (or those of its subclasses.)  
   * This will typically be done by an instance of <code>apollo.dataadapter.chado.ChadoAdaptor</code>.
   *
   * @param jdbcUrl       JDBC URL of a Chado-compliant database (or of a server/DBMS that hosts such a database.)
   * @param chadoDb       Schema/database name of the Chado database instance.
   * @param username      Database username.
   * @param password      Database password.
   * @param organismLike  Only sequences that match this string are retrieved by <code>getAllChadoSequencesByType</code>.
   */
  public void initWithException(String jdbcUrl, String chadoDb, String username,
      String password, String organismLike) 
  throws SQLException {
    this.jdbcUrl = jdbcUrl;
    this.chadoDb = chadoDb;
    this.username = username;
    this.password = password;
    this.organismLike = organismLike;
    logger.info("connecting to database " + chadoDb + " as user " + username + " at " + jdbcUrl);

    // Try to determine the Chado schema version
    Connection conn = this.getConnectionWithException(); // throws SQLException
    this.chadoVersion = SchemaVersion.guessSchemaVersion(conn);
    logger.debug("guessed schema version = " + this.chadoVersion);
  }

  /**
   * Execute a single SQL query and return the corresponding ResultSet.
   * Used for all select queries to ensure that the SQL is logged.
   *
   * @param method - name of the calling method
   * @param c - JDBC Connection
   * @param sql - query to run
   */ 
  public ResultSet executeLoggedSelectQuery(String method, Connection c, String sql) {
    ResultSet rs = null;
    // Since all queries are executed via this method the Log4J LocationInfo won't
    // be of any use.  Therefore we'll manually add the calling method name to the
    // nested diagnostic context:
    NDC.push(method);
    if (c == null) { c = getConnection(); }
    logger.debug(sql);

    try {
      Statement s = c.createStatement();
      rs = s.executeQuery(sql);
    } 
    catch (SQLException sqle) {
      logger.error("SQLException running " + sql, sqle);
    }
    NDC.pop();
    return rs;
  }

  /**
   * Execute an SQL update statement (update, insert, or delete) and return the number
   * of rows affected.  Uses a PreparedStatement to allow bound parameters to be used.
   *
   * @param method - name of the calling method
   * @param c - JDBC Connection
   * @param sql - query to run
   * @param colNames - names of the affected columns (if applicable)
   * @param colVals - values corresponding to <code>colNames</code>
   * @param unquotedColValues - Map whose keys list the column names that should NOT be
   *        passed as bound parameters.
   *
   * @return The number of rows inserted or updated.
   */
  public int executeLoggedUpdate(String method, Connection c, String sql, String colNames[], Object colVals[], Map unquotedColValues) {
    NDC.push(method);
    int nc = (colVals == null) ? 0 : colVals.length;
    int rowsAffected = 0;

    logger.debug(sql);
    StringBuffer paramStr = new StringBuffer();

    try {
      PreparedStatement pstmt = c.prepareStatement(sql);

      // TODO - should be querying the database for the column types instead of having to figure it out from the data
      // set values for placeholders - avoids having to worry about quoting
      int pnum = 1;
      for (int cnum = 0;cnum < nc;++cnum) {
        if ((unquotedColValues == null) || (!unquotedColValues.containsKey(colNames[cnum]))) {
          if (cnum > 0) { paramStr.append(","); }
          paramStr.append(colNames[cnum] + "=" + colVals[cnum]);

          if (colVals[cnum] instanceof String) {
            pstmt.setString(pnum, (String)(colVals[cnum]));
          } 
          else if (colVals[cnum] instanceof Short) {
            pstmt.setShort(pnum, ((Short)(colVals[cnum])).shortValue());
          } 
          else if (colVals[cnum] instanceof Integer) {
            pstmt.setInt(pnum, ((Integer)(colVals[cnum])).intValue());
          } 
          else if (colVals[cnum] instanceof Long) {
            pstmt.setLong(pnum, ((Long)(colVals[cnum])).longValue());
          }
          else if (colVals[cnum] instanceof Float) {
            pstmt.setFloat(pnum, ((Long)(colVals[cnum])).floatValue());
          }
          else if (colVals[cnum] instanceof java.sql.Date) {
            pstmt.setDate(pnum, (java.sql.Date)(colVals[cnum]));
          }
          else if (colVals[cnum] instanceof Time) {
            pstmt.setTime(pnum, (Time)(colVals[cnum]));
          }
          else if (colVals[cnum] instanceof Timestamp) {
            pstmt.setTimestamp(pnum, (Timestamp)(colVals[cnum]));
          }
          else if (colVals[cnum] instanceof Clob) {
            pstmt.setClob(pnum, (Clob)(colVals[cnum]));
          }
          else if (colVals[cnum] instanceof Boolean) {
            pstmt.setBoolean(pnum, ((Boolean) colVals[cnum]).booleanValue());
          }
          else if (colVals[cnum] == null) {
            NDC.pop();
            throw new IllegalArgumentException("can't set NULL value for column " + colNames[cnum] + " without knowing the type");
          }
          else {
            NDC.pop();
            throw new IllegalArgumentException("don't know how to handle value with class = " + colVals[cnum].getClass());
          }
          ++pnum;
        }
      }
      logger.debug("params: " + paramStr.toString());
      rowsAffected = pstmt.executeUpdate();
    } 
    catch (SQLException sqle) {
      logger.error("executeLoggedUpdate failed", sqle);
    }

    NDC.pop();
    return rowsAffected;
  }

  /** 
   * Alternate form of executeLoggedSelectQuery that uses <code>getConnection()</code>
   * instead of an expicitly-passed Connection object.
   */
  protected ResultSet executeLoggedSelectQuery(String method, String sql) {
    return this.executeLoggedSelectQuery(method, getConnection(), sql);
  }

  /** need property scheme to check if type is a synteny link */
  public void setPropertyScheme(PropertyScheme ps) {
    propertyScheme = ps;
  }

  protected SchemaVersion getChadoVersion() {
    return chadoVersion;
  }

  // zero-pad integer <code>n</code> to width <code>width</code>
  protected String zeroPad(int n, int width) {
    String result = Integer.toString(n);
    int rl = result.length();
    int diff = width - rl;

    for (int i = 0;i < diff;++i) {
      result = "0" + result;
    }
    return result;
  }

  public void setChadoInstance(ChadoInstance instance) {
    this.chadoInstance = instance;
    // Circular reference: Make reference consistent.
    if (chadoInstance != null)
      chadoInstance.setChadoAdapter(this);

    // Enable SQL logging if the instance specifies an SQL log destination
    String logDir = chadoInstance.getLogDirectory();
    if ((logDir != null) && (sqlLogPath == null)) {
      logger.info("chado <logDirectory> set to " + logDir);

      // construct a filename that's specific to this particular host, user, and session
      String hostname = "unknown";
      String username = UserName.getUserName();
      TimeZone ctz = TimeZone.getDefault();
      Calendar now = Calendar.getInstance();
      now.setTimeZone(ctz);
      int year = now.get(Calendar.YEAR);
      int month = now.get(Calendar.MONTH) + 1;
      int dayofmonth = now.get(Calendar.DAY_OF_MONTH);
      int hourofday = now.get(Calendar.HOUR_OF_DAY);
      int minute = now.get(Calendar.MINUTE);
      int second = now.get(Calendar.SECOND);
      int millisecond = now.get(Calendar.MILLISECOND);

      try { 
        InetAddress localhost = InetAddress.getLocalHost(); 
        hostname = localhost.getHostName();
      } catch (java.net.UnknownHostException uhe) {}

      String logFilePrefix = year + "-" + zeroPad(month,2) + "-" + zeroPad(dayofmonth,2) + "," + 
      zeroPad(hourofday,2) + ":" + zeroPad(minute,2) + ":" + zeroPad(second,2) 
      + ":" + zeroPad(millisecond,3) + "," + hostname + "," + username;

      File sqlLogFile = new File(logDir, logFilePrefix + "-sql.xml");
      File txnLogFile = new File(logDir, logFilePrefix + "-txn.xml");

      // send JdbcChadoAdapter messages -> sqlLogFile
      // send PureJDBCTransactionWriter messages -> transactionLogFile

      try {
        this.sqlLogPath = sqlLogFile.getCanonicalPath();
      } catch (IOException ioe) {
        logger.error("IOException finding canonical path for SQL log file at " + logFilePrefix, ioe);
      }

      try {
        this.txnLogPath = txnLogFile.getCanonicalPath();
      } catch (IOException ioe) {
        logger.error("IOException finding canonical path for SQL log file at " + logFilePrefix, ioe);
      }

      // modify log4j configuration to log all JdbcChadoAdapter info to these files
      if (sqlLogPath != null) {
        Logger jdbcL = LogManager.getLogger(JdbcChadoAdapter.class);
        Appender jdbcA = null;
        try {
          jdbcA = new FileAppender(new XMLLayout(), sqlLogPath);
        } catch (IOException ioe) {
          logger.error("IOException writing to " + sqlLogPath, ioe);
        }
        jdbcL.setAdditivity(false);
        jdbcL.addAppender(jdbcA);
        // set level to debug, unless it's already set to a more detailed level
        Level cl = jdbcL.getLevel();
        if ((cl == null) || (cl.isGreaterOrEqual(Level.DEBUG))) {
          jdbcL.setLevel(Level.DEBUG);
        }
        logger.info("logging SQL to " + sqlLogPath);
      }

      if (txnLogPath != null) {
        Logger txnL = LogManager.getLogger(PureJDBCTransactionWriter.class);
        Appender txnA = null;
        try {
          txnA = new FileAppender(new XMLLayout(), txnLogPath);
        } catch (IOException ioe) {
          logger.error("IOException writing to " + txnLogPath, ioe);
        }
        txnL.setAdditivity(false);
        txnL.addAppender(txnA);
        // set level to debug, unless it's already set to a more detailed level
        Level cl = txnL.getLevel();
        if ((cl == null) || (cl.isGreaterOrEqual(Level.DEBUG))) {
          txnL.setLevel(Level.DEBUG);
        }
        logger.info("logging transactions to " + txnLogPath);
      }
    }
  }

  public ChadoInstance getChadoInstance() {
    return chadoInstance;
  }

  /**
   * Check that the current connection information and the supplied sequence id are both valid.
   * Used by the GUI to determine whether it should display an error dialog or proceed to 
   * loading the sequence and sequence annotations.
   *
   * @param seqType   Name of a sequence type found in the current chado database's cvterm.name column.
   * @param seqId     A sequence identifier found in the current chado database's feature.uniquename column.
   */
  public void validateConnectionAndSequence(String seqType, String seqId) throws ApolloAdapterException {
    validateConnectionAndSequence(seqType, seqId, null);
  }
  
  /**
   * Check that the current connection information and the supplied sequence id are both valid.
   * Used by the GUI to determine whether it should display an error dialog or proceed to 
   * loading the sequence and sequence annotations.
   *
   * @param seqType   Name of a sequence type found in the current chado database's cvterm.name column.
   * @param seqId     A sequence identifier found in the current chado database's feature.uniquename column.
   * @param realSeqId StringBuilder that gets updated with actual feature.uniquename (used for synonym lookups).
   */
  public void validateConnectionAndSequence(String seqType, String seqId, StringBuilder realSeqId) throws ApolloAdapterException {
    Connection conn = getConnection();

    if (conn == null) {
      String err = "unable to connect to chado database - please check database/login information and try again";
      logger.error(err);
      throw new ApolloAdapterException(err);
    }
    if (seqId.trim().equals("")) {
      logger.error("seqId.equals(\"\")");
      throw new ApolloAdapterException("No sequence specified - please enter a valid sequence name");
    }
    if (!chadoFeatureExists(conn, seqType, seqId, realSeqId)) {
      logger.error("cannot find seq", new Throwable());
      throw new ApolloAdapterException("Cannot find sequence of type '" + seqType + "'" + " with uniquename '" + seqId + "'");
    }
  }

  /**
   * This method can be called by the subclass before calling <code>getConnection</code>, 
   * in order to load any needed JDBC driver classes with a Class.forName().
   *
   * @param jdbcDriverClass  The name of the Java class for which to do a Class.forName().
   */
  protected void loadClass(String jdbcDriverClass) {
    Class c = null;

    try {
      c = Class.forName(jdbcDriverClass);
    } catch (ClassNotFoundException cnfe) {
      logger.warn("unable to find JDBC driver class '" + jdbcDriverClass + "' - please check CLASSPATH");
    }
    if (c != null) {
      logger.debug("loaded JDBC driver class '" + c);
    }
  }

  private Connection connection;

  /**
   * Returns the current database connection, if one exists and is valid.  If not,
   * opens and returns a new database connection, using the connection parameters
   * supplied in the arguments to <code>init</code>.
   *
   * @return  A JDBC Connection object or null if a connection could not be opened.
   */
  protected Connection getConnection() {

    try {
      return getConnectionWithException();
    } catch (SQLException sqle) {
      logger.error("SQLException connecting to chado database at JDBC URL " + jdbcUrl, sqle);
    }

    return connection; 
  }

  /**
   * A variant of <code>getConnection</code> that throws an SQLException in the 
   * case of an SQL error.
   */
  protected Connection getConnectionWithException() throws SQLException {
    if (connection == null || connection.isClosed())
      connection = getNewConnection();
    return connection;
  }

  /**
   * Opens and return a new Connection to the database specified in the arguments
   * to <code>init</code>.
   */
  protected Connection getNewConnection() throws SQLException {
    logger.debug("JdbcChadoAdapter: connecting with '" + jdbcUrl + "' username='" + username + "' password='" + password + "'");
    return DriverManager.getConnection(this.jdbcUrl, this.username, this.password);
  }

  /** Get feature.feature_id by searching against synonyms.  Updates the synonym StringBuilder object to feature.uniquename
   *  which is expected elsewhere.
   *  
   * @param c - Connection object to the database
   * @param featType - Chado cvterm.name
   * @param synonym - Synonym to be searched
   * @param realSeqId - StringBuilder that gets updated with the actual id for the feature
   * @return The feature_id of the specified chado feature or -1 if it could not be determined.
   */
  protected long getFeatureIdBySynonym(Connection c, String featType, String synonym, StringBuilder realSeqId)
  {
    long featureId = -1;
    try {
      //String sql = "SELECT feature_id, uniquename FROM gene_synonym INNER JOIN feature ON (gene_synonym.name = feature.name) WHERE key='" + synonym + "'";
      String sql = "SELECT f.feature_id, f.uniquename FROM feature f INNER JOIN feature_synonym fs ON (f.feature_id = fs.feature_id) INNER JOIN synonym s ON (fs.synonym_id = s.synonym_id) WHERE s.name='" + synonym + "'";
      ResultSet rs = executeLoggedSelectQuery("getFeatureIdBySynonym", c, sql);
      if (rs.next()) {
        featureId = rs.getLong("feature_id");
        realSeqId.delete(0, realSeqId.length());
        realSeqId.append(rs.getString("uniquename"));
      }
    }
    catch (SQLException sqle) {
      logger.error("getFeatureIdBySynonym: SQLException retrieving feature.feature_id for " + synonym, sqle);
    }
    return featureId;
  }
  
  /**
   * Retrieve the Chado feature.feature_id for a specified feature.
   *
   * @param featType   chado cvterm.name
   * @param featName   chado feature.uniquename
   * @return           The feature_id of the specified chado feature or -1 if it could not be determined.
   */
  protected long getFeatureId(Connection c, String featType, String featName) {
    long featureId = - 1;
    String quotedFeatName = "'" + featName + "'";
    boolean useUniquename = chadoInstance.getQueryFeatureIdWithUniquename();
    boolean useName = chadoInstance.getQueryFeatureIdWithName();

    if (!useUniquename && !useName) {
      logger.error("can't have both queryFeatureIdWithUniquename and queryFeatureIdWithName set to false");
      useUniquename = true;
    }
    try {
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT f.feature_id FROM feature f WHERE ");

      if (useUniquename && useName) {
        sql.append("(f.uniquename = " + quotedFeatName + " OR f.name = " + quotedFeatName + ") ");
      } else if (useUniquename) {
        sql.append("f.uniquename = " + quotedFeatName + " ");
      } else {
        sql.append("f.name = " + quotedFeatName + " ");
      }

      if (featType != null) {
        sql.append("AND f.type_id = " + getFeatureCVTermId(featType));
      }

      ResultSet rs = executeLoggedSelectQuery("getFeatureId", c, sql.toString());
      if (rs.next()) { featureId = rs.getLong("feature_id"); }
      if (rs.next()) { logger.error("getFeatureId: multiple rows retrieved for feature with name = " + featName); }
    } 
    catch (SQLException sqle) {
      logger.error("getFeatureId: SQLException retrieving feature.feature_id for " + featName, sqle);
    }

    return featureId;
  }
  
  /**
   * Retrieve the Chado feature.seqlen for a specified feature.
   *
   * @param c           JDBC connection to use
   * @param seqFeatId   chado feature.feature_id
   * @return            The seqlen of the specified chado feature or -1 if it could not be determined.
   */  
  protected int getSeqLengthForFeatureId(Connection c, long seqFeatId)
  {
    int seqLen = -1;
    String sql = "SELECT seqlen FROM feature WHERE feature_id = " + seqFeatId;
    try {
      ResultSet rs = executeLoggedSelectQuery("getSeqLengthForFeatureId", c, sql);
      if (rs.next()) {
        seqLen = rs.getInt("seqlen");
      }
    }
    catch (SQLException e) {
      logger.error("getSeqLengthForFeatureId: SQLException retrieving feature.seqlen for " + seqFeatId, e);
    }
    return seqLen;
  }

  /**
   * Retrieve the Chado feature.feature_id for a specified feature.
   *
   * @param featName   chado feature.uniquename
   * @return           The feature_id of the specified chado feature or -1 if it could not be determined.
   */
  public long getFeatureId(String featName) {
    Connection conn = getConnection();
    return getFeatureId(conn, null, featName);
  }

  /**
   * Check whether a particular feature exists in a given Chado database.  Note
   * that this method may return false if the feature exists but is unreadable by
   * the current user (i.e., the specified username may not have SELECT permissions
   * on every feature in the database.)
   *
   * @param conn      JDBC Connection to a Chado-compliant relational database.
   * @param seqType   Name of a sequence type found in the current chado database's cvterm.name column.
   * @param seqId     A sequence identifier found in the current chado database's feature.uniquename column.
   * @return          Whether the specified Chado feature can be found
   */
  public boolean chadoFeatureExists(Connection conn, String seqType, String seqId) {
    return chadoFeatureExists(conn, seqType, seqId, null);
  }
  
  /**
   * Check whether a particular feature exists in a given Chado database.  Note
   * that this method may return false if the feature exists but is unreadable by
   * the current user (i.e., the specified username may not have SELECT permissions
   * on every feature in the database.)
   *
   * @param conn      JDBC Connection to a Chado-compliant relational database.
   * @param seqType   Name of a sequence type found in the current chado database's cvterm.name column.
   * @param seqId     A sequence identifier found in the current chado database's feature.uniquename column.
   * @param realSeqId StringBuilder object that will get populated with feature.uniquename.
   * @return          Whether the specified Chado feature can be found
   */
  public boolean chadoFeatureExists(Connection conn, String seqType, String seqId, StringBuilder realSeqId) {
    long featId = getFeatureId(conn, seqType, seqId.toString());
    if (featId == -1 && realSeqId != null && chadoInstance.getUseSynonyms()) {
      featId = getFeatureIdBySynonym(conn, seqType, seqId, realSeqId);
    }
    return (featId != -1);
  }

  /**
   * Retrieve the id (Chado uniquename) and description of every Chado sequence of a particular type.
   *
   * @param featType        Name of a sequence type found in the current chado database's cvterm.name column.
   * @param seqUniqueNames  Vector to be populated with the Chado uniquenames for the sequences.
   * @param seqDescrs       Vector to be populated with human-readable sequence descriptions.
   */
  public void getAllChadoSequencesByType(String featType, Vector seqUniqueNames,Vector seqDescrs)
  throws ApolloAdapterException {

    try {
      String pvalCol = chadoVersion.getPValCol();
      String pkeyCol = chadoVersion.getPKeyCol();

      // The following SQL uses ANSI outer join syntax in order to maintain compatibility with both 
      // PostgresQL (version 7.x?) and Sybase (which supports ANSI syntax as of version 12.0)
      String sql = 
        "SELECT o.common_name, f.uniquename, fp." + pvalCol + " AS chrom " +
        "FROM feature f LEFT OUTER JOIN featureprop fp on " +
        "(f.feature_id = fp.feature_id AND fp." + pkeyCol + " = " + getFeatureCVTermId("chromosome") + " ), " + 
        "organism o " +
        "WHERE f.type_id = " + getFeatureCVTermId(featType) + " " + 
        "AND f.organism_id = o.organism_id " +
        ((organismLike != null) ? "and o.common_name LIKE '" + organismLike + "'" : "") +
        "ORDER BY o.common_name, o.species, f.uniquename";
      // convert is sybase specific i think - doesnt seem to be in postgres
      // taking out for now - do we really need it?
      //"ORDER BY o.common_name, o.species, convert(numeric, fp." + pvalCol + "), f.uniquename";

      ResultSet rs = executeLoggedSelectQuery("getAllChadoSequencesByType", sql);

      while (rs.next()) {
        String name = rs.getString("uniquename");
        String organism = rs.getString("common_name");
        String chromosome = rs.getString("chrom");

        // Construct a user-friendly description of each sequence that includes the
        // chromosome number, if known. Works well for databases of nearly-finished
        // genomes in which there is a 1-1 mapping between "assembly" sequences and
        // chromosomes.  Works less well in other cases.
        String descr = "";
        if ((organism != null) && (chromosome != null)) {
          descr = organism + " chr. " + chromosome + " (" + name + ")";
        } else {
          descr = name;
        }

        seqUniqueNames.addElement(name);
        seqDescrs.addElement(descr);
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving all features of type " + featType, sqle);
    }
  }

  /** This is a stripped down version of getAllChadoSequencesByType. It just returns 
      a list of feat names, no descriptions. The problem with the other method is it's slow
      on postgres (outer join) and queries for descriptions that are not present in 
      fly & rice - so this is the zippier/simpler alternative */
  public List getFeatNamesByType(String featType) {
    String select = makeSelectClause("name");
    String from = makeFromClause("feature");
    Long featTypeId = getFeatureCVTermId(featType);
    if (featTypeId == null) {
      logger.error("Failed to get cv_term id for type "+featType+"! This feature type "
          +"does not seem to be presnt in your chado database. Either the database"
          +" or the configuration needs to be amended", new Throwable());
      return new ArrayList(0); // null?
    }
    WhereClause where = new WhereClause("type_id",featTypeId);
    String sql = select + from + where+" ORDER BY name";
    try {
      List nameList = new ArrayList();
      ResultSet rs = executeLoggedSelectQuery("getFeatNamesByType", sql);
      while (rs.next()) {
        String name = rs.getString("name");
        nameList.add(name);
      }
      return nameList;

    } catch (SQLException sqle) {
      logger.error("SQLException retrieving all features of type '" + featType + "'", sqle);
      return null;
    }
  }

  /**
   * A cache of apollo.datamodel.sequence objects read from the current database, indexed 
   * by Chado feature uniquename with the strand appended (a "(-)" or "(+)" symbol).
   */
  Hashtable seqHash = new Hashtable();

  /**
   * Retrieve a sequence from Chado and convert it into an instance of apollo.dataadapter.Sequence.
   * Subsequent calls to this method with the same values of <code>featureId</code> and 
   * <code>revComp</code> are guaranteed to return the same cached object returned by the first
   * call.
   *
   * @param featureId  Chado feature.feature_id.
   * @return           The Apollo equivalent of the specified Chado sequence.
   */
  protected SequenceI getSequence(long featureId) {
    return getSequence(featureId,false,null,0);
  }

  protected SequenceI getSequence(long featureId, boolean isLazy, String uniquename,
      int seqLength) {
    // Check the cache first
    SequenceI seq = getSeqFromCache(featureId);
    if (seq != null)
      return seq;

    if (isLazy)
      return new ChadoLazySequence(uniquename,seqLength,this);

    // i dont think we need revcomp - at least its not presently used - whats a use
    // case for it?     //boolean revComp = false;
    // Reverse complement requested; first retrieve the original sequence with a nested call
    //       if (revComp) { //SequenceI fwdSeq = getSequence(c, featureId);
    //         String revRes = DNAUtils.reverseComplement(fwdSeq.getResidues());
    //         String name = fwdSeq.getName() + " (-)";
    //         SequenceI revSeq = makeSequence(name, revRes, seqHasRange,null);
    //         putSeqInCache(featureId, revComp, revSeq);
    //         seq = revSeq;//  }//else {

    // not found in the cache; access the database for the sequence residues
    try {
      String sql = "SELECT uniquename,residues FROM feature WHERE feature_id = " + featureId;
      ResultSet rs = executeLoggedSelectQuery("getSequence", sql);

      if (rs.next()) {
        String name = rs.getString("uniquename");
        String residues = rs.getString("residues");
        seq = makeSequence(name, residues, null);// null org
        putSeqInCache(featureId, seq);
      }
    }
    catch (SQLException sqle) {
      logger.error("SQLException retrieving sequence with feature_id = " + featureId, sqle);
    }
    return seq;
  }

  /** low & high are base oriented(??) - used by ChadoLazySeq & FeatLocImp */
  SequenceI getResiduesSubstring(String uniquename, int low, int high) {
    low = adjustLowForBaseOrientedToInterbaseConversion(low); // ??
    int length = high - low + 1; // +1 due to base oriented ??
    String res = "substring(residues from "+low+" for "+length+") as seq";
    String[] select = new String[] {res,"name","organism_id"};
    String[] from = new String[] {"feature"};
    WhereClause where = new WhereClause("uniquename",uniquename,true);
    //where.addQuotes();
    String sql = makeSql(select,from,where);
    try {
      ResultSet rs = executeLoggedSelectQuery("getResiduesSubstring", sql);
      if (rs.next()) { // this should be true
        String name = rs.getString("name");
        String residues = rs.getString("seq");
        String organism = getOrganismFullName(rs.getInt("organism_id"));
        return makeSequence(name,residues,organism);
      }
    }
    catch (SQLException e) {
      logger.error("unable to retrieve subsequence " + low + "-" + high + " for " + uniquename, e);
    }
    return null;
  }

  SequenceI makeSequence(String name, String residues, String organism) {
    SequenceI seq;
    seq = new Sequence(name,residues);
    if (organism != null)
      seq.setOrganism(organism);
    return seq;
  }

  private SequenceI getSeqFromCache(long featureId) {
    return getSeqFromCache(featureId,false);
  }

  private SequenceI getSeqFromCache(long featureId, boolean revComp) {
    return (SequenceI)seqHash.get(seqHashKey(featureId,revComp));
  }
  private void putSeqInCache(long featureId, SequenceI seq) {
    putSeqInCache(featureId,false,seq);
  }
  private void putSeqInCache(long featureId, boolean revComp,SequenceI seq) {
    seqHash.put(seqHashKey(featureId,revComp),seq);
  }
  // take revcomp out?
  private String seqHashKey(long featureId, boolean revComp) {
    return Long.toString(featureId) + "(" + (revComp ? "-" : "+") + ")";
  }

  // -----------------------------------------------------------------------
  // featureCvTerms
  // -----------------------------------------------------------------------
  // ontology inner class? outer class?

  /**
   * Retrieve all cvterms that are part of the specified controlled vocabulary for
   * features, * or *all* cvterms if no cv is specified.
   *
   * @param c
   * @param cvName
   * @return Hashtable mapping cvterm.name to cvterm.cvterm_id (objects of type Long)
   */
  private Map getCVTerms(String cvName) {
    String cvNameCol = chadoVersion.getCvNameCol();

    String sql = "SELECT cv.cvterm_id, cv.name " + 
    "FROM cvterm cv, cv c " + 
    "WHERE c.cv_id = cv.cv_id ";

    // if no cvName given, retrieve *all* cvterms
    if (cvName != null) {
      sql = sql + "AND c." + cvNameCol + "='" + cvName + "' ";
    }

    Hashtable h = new Hashtable();

    try {
      ResultSet rs = executeLoggedSelectQuery("getCVTerms", sql);

      while (rs.next()) {
        long id = rs.getLong("cvterm_id");
        String name = rs.getString("name");
        if (h.get(name) != null) {
          logger.warn("read multiple cvterms with name = '" + name  + "'");
        }
        h.put(name, new Long(id));
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving cvterms with cvname = " + cvName, sqle);
    }

    return h;
  }
  // cvterm from the table cv 'null'

  public Long getNullCVTermId(String name) {
    if (nullCvTerms == null) 
      nullCvTerms = getCVTerms("null");

    Long result = (Long)(nullCvTerms.get(name));

    if (result == null) 
      logger.error("lookup failed for cvterm_id for cvterm with name ='" + name + "' in 'null' cv; " );
    return result;  
  } 

  /** Returns null if name not present in cvterm table */
  Long getFeatureCVTermId(String name) {
    if (featureCvTerms == null) {
      // cache controlled vocabulary terms specified by 
      // <code>getDefaultFeatureCVName()</code>
      featureCvTerms = getCVTerms(getChadoInstance().getFeatureCVName());
    } 

    Long result = (Long)(featureCvTerms.get(name));

    if (result == null) {
      logger.error("lookup failed for cvterm_id for cvterm with name ='" + name + "'; " +
          "you may need to insert the missing row into the chado cvterm table, or " +
      "modify the <featureCV> setting in the chado adapter configuration file");
    }

    return result;
  }

  private String getFeatCvName(int id) {
    return getFeatCvName(new Long(id));
  }

  private String getFeatCvName(Long id) {
    if (featureCvTerms == null)
      featureCvTerms = getCVTerms(getChadoInstance().getFeatureCVName());
    // Get the name for id
    for (Iterator it = featureCvTerms.keySet().iterator(); it.hasNext();) {
      String name = (String) it.next();
      Long tmpId = (Long) featureCvTerms.get(name);
      if (tmpId.equals(id))
        return name;
    }
    throw new IllegalStateException("JdbcChadoAdapter.getFeatCvName(): cvterm_id = " + id + " not found");
  }

  // --------------------------------------------
  // relationshipCVTerms
  // --------------------------------------------

  private class PartOfException extends Exception {
    private PartOfException(String m) { super(m); }
  }

  private Long getPartOfCVTermId() throws PartOfException {
    String partOfString = getChadoInstance().getPartOfCvTerm();
    Long id = getRelationshipCVTermId(partOfString);
    if (id == null) {
      String m = "part of cv term string '"+partOfString+"' not found in relationship"
      +" ontology. Configure this in chado-adapter.xml";
      throw new PartOfException(m);
    }
    return id;
  }

  protected Long getRelationshipCVTermId(String name) {
    if (this.relationshipCvTerms == null) {
      // cache controlled vocabulary terms specified by 
      // <code>getDefaultRelationshipCVName()</code>
      this.relationshipCvTerms = 
        getCVTerms(getChadoInstance().getRelationshipCVName());
    } 
    return (Long)(this.relationshipCvTerms.get(name));
  }

  // -------------------------------------
  // propertyTypeCVTerms
  // -------------------------------------

  Long getPropertyTypeCVTermId(String name) {
    if (this.propertyTypeCvTerms == null) {
      // cache controlled vocabulary terms specified by 
      // <code>getDefaultPropertyTypeCVName()</code>
      this.propertyTypeCvTerms = 
        getCVTerms(getChadoInstance().getPropertyTypeCVName());
    } 
    return (Long)(this.propertyTypeCvTerms.get(name));
  }

  // -------------------------------------
  // Retrieve an arbitrary cvterm
  // -------------------------------------

  // TODO - cache these results also
  Long getCVTermId(String cvName, String cvTermName) {
    Long result = null;
    String cvNameCol = chadoVersion.getCvNameCol();

    String sql = "SELECT cvt.cvterm_id " + 
    "FROM cvterm cvt, cv c " + 
    "WHERE c.cv_id = cvt.cv_id " +
    "AND c." + cvNameCol + "='" + cvName + "' " +
    "AND cvt.name = '" + cvTermName + "' ";

    try {
      ResultSet rs = executeLoggedSelectQuery("getCVTermId", sql);
      if (rs.next()) {
        result = new Long(rs.getLong("cvterm_id"));
      } else {
        logger.warn("no cvterm_id found for cvterm '" + cvTermName + "' in CV '" + cvName + "'");
      }
      if (rs.next()) {
        logger.error("multiple cvterm_ids found for cvterm '" + cvTermName + "' in CV '" + cvName + "'");
      }
    }
    catch (SQLException sqle) {
      logger.error("SQLException retrieving cvterms with cvname = " + cvTermName, sqle);
    }

    return result;
  }

  Long getLikeCVTermId(String cvName, String cvTermName) {
    Long result = null;
    String cvNameCol = chadoVersion.getCvNameCol();

    String sql = "SELECT cvt.cvterm_id " + 
    "FROM cvterm cvt, cv c " + 
    "WHERE c.cv_id = cvt.cv_id " +
    "AND c." + cvNameCol + " LIKE '" + cvName + "' " +
    "AND cvt.name LIKE '" + cvTermName + "' ";

    try {
      ResultSet rs = executeLoggedSelectQuery("getCVTermId", sql);
      if (rs.next()) {
        result = new Long(rs.getLong("cvterm_id"));
      } else {
        logger.warn("no cvterm_id found for cvterm '" + cvTermName + "' in CV '" + cvName + "'");
      }
      if (rs.next()) {
        logger.error("multiple cvterm_ids found for cvterm '" + cvTermName + "' in CV '" + cvName + "'");
      }
    }
    catch (SQLException sqle) {
      logger.error("SQLException retrieving cvterms with cvname = " + cvTermName, sqle);
    }

    return result;
  }
  
  // ------------------
  // Organism map/cache
  // ------------------

  /** Maps Integers to Organism (inner class) */
  Map idToOrganism;

  private Map getIdToOrganism() {

    if (idToOrganism == null) {

      idToOrganism = new HashMap();

      String abbrevCol = chadoVersion.getAbbrevCol();
      String sql = "SELECT organism_id,"+ abbrevCol+" AS abbreviation, genus, species "
      +" FROM organism ";

      try {
        ResultSet rs = executeLoggedSelectQuery("getIdToOrganism", sql);

        while (rs.next()) {
          Organism org = new Organism(rs);
          //int id = rs.getInt("organism_id");
          //String abbrev = rs.getString("abbreviation");
          idToOrganism.put(org.getIdInteger(),org);
        }
      }
      catch (SQLException e) {
        logger.error("getIdToOrganism: id-organism map retrieval failed", e);
      }

    }
    return idToOrganism;
  }

  private class Organism {
    private int id;
    private String abbreviation;
    private String genus;
    private String species;
    private Organism(ResultSet rs) throws SQLException {
      id = rs.getInt("organism_id");
      abbreviation = rs.getString("abbreviation");
      genus = rs.getString("genus");
      species = rs.getString("species");
    }
    private Integer getIdInteger() {
      return new Integer(id);
    }
    private String getFullName() {
      return genus + " " + species;
    }
  }

  private Organism getOrganism(int orgId) {
    return (Organism)getIdToOrganism().get(new Integer(orgId));
  }

  private String getOrganismAbbrev(int orgId) {
    return getOrganism(orgId).abbreviation;
  }

  String getOrganismFullName(int orgId) {
    return getOrganism(orgId).getFullName();
  }

  /**
   * Front end to the main getCDSFeatures
   * 
   * @param c
   * @param srcSeqId
   * @param refSeq
   * @param sfs
   * @param featLocImp
   * @param chadoPrgs
   * @return
   * @throws RelationshipCVException
   * @see #getCDSFeatures(Connection, long, SequenceI, StrandedFeatureSet, String)
   */
  protected Hashtable getCDSFeatures(Connection c, long srcSeqId,
      SequenceI refSeq, StrandedFeatureSet sfs,
      FeatureLocImplementation featLocImp, ChadoProgram[] chadoPrgs)
  throws RelationshipCVException {

    String sql = getChadoInstance().getPredictedCdsSql(featLocImp, chadoPrgs); // throws RelCVException
    if (sql == null || sql.length()<1)
      return new Hashtable();

    return getCDSFeatures(c, srcSeqId, refSeq, sfs, sql);
  }

  /**
   * Front end to the main getCDSFeatures
   * 
   * @param c
   * @param srcSeqId
   * @param refSeq
   * @param sfs
   * @param featLocImp
   * @return
   * @throws RelationshipCVException
   * @see #getCDSFeatures(Connection, long, SequenceI, StrandedFeatureSet, String)
   */
  protected Hashtable getCDSFeatures(Connection c, long srcSeqId,
      SequenceI refSeq, StrandedFeatureSet sfs,
      FeatureLocImplementation featLocImp) throws RelationshipCVException {
    String sql = getChadoInstance().getCdsSql(featLocImp); // throws
    // RelCVException
    return getCDSFeatures(c, srcSeqId, refSeq, sfs, sql);
  }

  /**
   * Retrieve all CDS features feature-loc'ed to a Chado sequence.
   * 
   * @param c JDBC connection to the database.
   * @param srcSeqId Chado feature_id of the sequence whose CDS features are to be returned.
   * @param refSeq Apollo Sequence that corresponds to Chado sequence <code>srcSeqId</code>
   * @param sfs StrandedFeatureSet to which CDS features should be added when running in debug mode.
   * @param sql Sql string wich retrieves the cds, according to featlocimp and chadoprogram[]
   * @return A Hashtable mapping transcript.uniquename to a Vector of ChadoCds objects
   */
  protected Hashtable getCDSFeatures(Connection c, long srcSeqId,
      SequenceI refSeq, StrandedFeatureSet sfs, String sql)
  throws RelationshipCVException {
    Hashtable thash = new Hashtable();
    int nAltSplicedTrans = 0;

    // String sql = getChadoInstance().getCdsSql(featLocImp); // throws
    // RelCVException

    try {
      ResultSet rs = executeLoggedSelectQuery("getCDSFeatures", c, sql);

      while (rs.next()) {
        int ifmin = rs.getInt("fmin");
        Integer fmin = rs.wasNull() ? null : new Integer(ifmin);
        int ifmax = rs.getInt("fmax");
        Integer fmax = rs.wasNull() ? null : new Integer(ifmax);
        boolean fmin_partial = rs.getBoolean("is_fmin_partial");
        boolean fmax_partial = rs.getBoolean("is_fmax_partial");
        int icdsStrand = rs.getInt("strand");
        Integer cdsStrand = rs.wasNull() ? null : new Integer(icdsStrand);
        String cdsName = rs.getString("cds_name");
        String transUniqueName = rs.getString("transcript_uniquename");
        String transSeq = rs.getString("transcript_seq");
        String proteinName = rs.getString("protein_name");
        String proteinSeq = rs.getString("protein_seq");

        ChadoFeatureLoc cdsLoc = new ChadoFeatureLoc(fmin, fmax, fmin_partial, fmax_partial, cdsStrand);

        Object o = thash.get(transUniqueName);
        Vector v = null;

        if (o == null) {
          v = new Vector();
          thash.put(transUniqueName, v);
        }
        else {
          v = (Vector) o;
          // Track the number of alternatively-spliced transcripts
          ++nAltSplicedTrans;
        }
        v.addElement(new ChadoCds(cdsName, transSeq, proteinName, proteinSeq,
            cdsLoc.getFmin().intValue(), cdsLoc.getFmax().intValue(), 
            cdsLoc.getFminPartial(), cdsLoc.getFmaxPartial(), cdsLoc.getStrand().intValue()));
      }
    }
    catch (SQLException sqle) {
      logger.error("SQLException retrieving gene features for sequence with feature_id = " + srcSeqId, sqle);
    }
    return thash;
  }


  /** add annotations with only one level, like transposons (for now) and
      annotated tiling paths, promoters & what not. this also includes
      seq errors (if configured for in chado-adpater.xml) 
      - indels & substitions - which get special treatment ->
      they end up as SequenceEdit features hanging off of genomic */
  protected void addOneLevelAnnotations(Connection c,
      SequenceI refSeq,
      StrandedFeatureSet sfs,
      FeatureLocImplementation featLoc,
      boolean getFeatProps,
      boolean getSynonyms,
      boolean getDbXRefs
  ) 
  {
    ChadoInstance chadoInstance = getChadoInstance();
    List oneLevelTypes = chadoInstance.getOneLevelAnnotTypes();
    if (oneLevelTypes.isEmpty())
      return;
    StringBuffer typeBuffer = new StringBuffer();
    for (Iterator it = oneLevelTypes.iterator(); it.hasNext();) {
      String type = (String) it.next();
      typeBuffer.append(getFeatureCVTermId(type));
      if (it.hasNext()) 
        typeBuffer.append(",");
    }
    String query = "SELECT f.feature_id as feature_id, name, uniquename, " +
    "type_id, dbxref_id, fmin, fmax, strand, residues " +
    "\nFROM feature f, featureloc fl " +
    "\nWHERE f.type_id in (" + typeBuffer.toString() + ") AND " +
    "f.feature_id = fl.feature_id \nAND " +
    "is_analysis = " +  getBooleanValue(false) + " \nAND " + 
    "fl.srcfeature_id = " + featLoc.getContainingFeatureId() +
    featLoc.getContainingFeatureWhereClause("fl");
    try {
      ResultSet resultSet = executeLoggedSelectQuery("addOneLevelAnnotations", c, query);
      String name, uniquename, residues;
      int type_id, fmin, fmax, feature_id;
      int strand;

      while (resultSet.next()) {
        feature_id = resultSet.getInt("feature_id");
        name = resultSet.getString("name");
        uniquename = resultSet.getString("uniquename");
        type_id = resultSet.getInt("type_id");
        fmin = resultSet.getInt("fmin");
        fmax = resultSet.getInt("fmax");
        strand = resultSet.getInt("strand");
        residues = resultSet.getString("residues");
        // Create AnnotatedFeature based these infomation
        // if substitution, nuceotide_insertion or nuc_deletion then do as
        // SeqEdit and put on genomic (these should be configged in xml)
        String featType = getFeatCvName(new Long(type_id));
        AnnotatedFeatureI annot = new AnnotatedFeature();

        boolean isSeqError = isSequencingError(featType);
        if (isSeqError) {
          // residues are null for deletion, or should be
          annot = new SequenceEdit(residues);
        }
        annot.setFeatureType(getFeatCvName(new Long(type_id)));

        // name can be null in chado (probably bad data but nonetheless it happens)
        // which causes a null pointer in setName - check for this
        setName(annot,name,uniquename);

        annot.setId(uniquename);
        annot.setStrand(strand);
        if (Config.DO_ONE_LEVEL_ANNOTS) // take out flag - do no matter what?
          setFeatCoordsFromInterbase(annot,fmin,fmax,strand);

        if (isSeqError) { // side effect -> seqEdit adds itself to seq
          // el - had to change the automatic adding of the seqEdit to seq to allow
          // correct undo behavior
          annot.setRefSequence(refSeq);
          refSeq.addSequenceEdit((SequenceEdit)annot);
        }
        else
          sfs.addFeature(annot); // annot gets added to seq by FeatSet

        // Need to add Sequence
        // Add other infomation

        if (getFeatProps)
          addAnnotationFeatProps(annot, feature_id);
        if (getSynonyms)
          addAnnotSynonyms(annot, feature_id);
        if (getDbXRefs)
          addAnnotDbXRefs(annot, 
              feature_id, 
              resultSet.getInt("dbxref_id"));

        if (!Config.DO_ONE_LEVEL_ANNOTS) { // take out flag? never do 3 levels?
          // To mimic Apollo data stuctures
          Transcript tran = new Transcript();
          tran.setRefSequence(refSeq);
          tran.setId(uniquename);
          setName(tran,name,uniquename+"(transcript)"); // null name check
          tran.setStrand(strand);
          annot.addFeature(tran);
          Exon exon = _makeExon(fmin, fmax, strand);
          exon.setRefSequence(refSeq);
          exon.setId(uniquename + ":" + fmin + "-" + fmax);
          tran.addExon(exon);
        }
      }
    }
    catch(SQLException e) {
      logger.error("addOneLevelAnnotations()", e);
    }
  }

  /** eventually config this - return true if featType is a seq error
      insertion, deletion, substitution */
  private boolean isSequencingError(String type) {
    return SequenceEdit.typeIsSeqError(type);
  }

  /** name can be null in chado (probably bad data but nonetheless it happens)
      which causes a null pointer in setName - check for this */
  private void setName(SeqFeatureI feat, String name, String uniquename) {
    if (name != null)
      feat.setName(name);
    else
      logger.debug("One level annot feature with uniquename "+uniquename+" has a null name");
  }

  /**
   * Retrieve a set of gene models from Chado and convert them into instances of apollo.datamodel.Gene.
   *
   * @param c         JDBC connection to the database.
   * @param refSeq    Apollo Sequence that corresponds to Chado sequence <code>srcSeqId</code>
   * @param sfs       StrandedFeatureSet to which the Apollo Genes should be added.
   * @param featLoc   FeatureLocImplementation has featloc src feat id, range 
   * (if non-redundant feat locs), and any padding of the range
   */  
  void addAnnotationModels(Connection c, SequenceI refSeq, StrandedFeatureSet sfs,
      FeatureLocImplementation featLoc, boolean getFeatProps,
      boolean getSynonyms, boolean getDbXRefs, List featureTypes) {
    // NOTE: the following assumes that negative numbers are not being used as Chado feature_ids
    int currentGeneId = -1; 
    int currentTranscriptId = -1;
    AnnotatedFeature currentGene = null;
    Transcript currentTranscript = null;
    Vector genes = new Vector();
    Vector transcripts = new Vector();
    Vector exons = new Vector();

    String fminCol = chadoVersion.getFMinCol();
    String fmaxCol = chadoVersion.getFMaxCol();
    String subjFeatCol = chadoVersion.getSubjFeatCol();
    String objFeatCol = chadoVersion.getObjFeatCol();

    // deal with variance in feature loc implementations
    //FeatureLocImplementation featLoc = new FeatureLocImplementation(srcSeqId,getConnection());
    long srcSeqId = featLoc.getContainingFeatureId();
    String featLocWhereClause = featLoc.getContainingFeatureWhereClause("exonloc");

    /** could be partof or part_of */
    Long partOfCvTermId;
    try {
      partOfCvTermId = getPartOfCVTermId();
    }
    catch (PartOfException pe) {
      logger.error("cannot retrieve genes: " + pe.getMessage());
      return;
    }

    ChadoInstance ci = getChadoInstance();

    // feature table column used for gene.setName()
    String geneNameField = ci.getGeneNameField();
    String geneNameAlias, geneNameSelect;

    // special case for "uniquename" since the SQL already includes that field
    if (geneNameField.equals("uniquename")) {
      geneNameAlias = "gene_uniquename";
      geneNameSelect = "";
    } else {
      geneNameAlias = "gene_name";
      geneNameSelect = "gene." + geneNameField + " as " + geneNameAlias + ", ";
    }

    // feature table column used for transcript.setName()
    String transcriptNameField = ci.getTranscriptNameField();
    String transcriptNameAlias, transcriptNameSelect;

    // special case for "uniquename" since the SQL already includes that field
    if (transcriptNameField.equals("uniquename")) {
      transcriptNameAlias = "transcript_uniquename";
      transcriptNameSelect = "";
    } else {
      transcriptNameAlias = "transcript_name";
      transcriptNameSelect = "trans." + transcriptNameField + " as " + transcriptNameAlias + ", ";
    }

    Iterator featureType = featureTypes.iterator();
    StringBuffer bufferTypes = new StringBuffer("");
    while (featureType.hasNext()) {
      bufferTypes.append(getFeatureCVTermId((String)featureType.next()));
      if(featureType.hasNext())
        bufferTypes.append(",");
    }
    if (bufferTypes.length() == 0) {
      return;
    }

    String sql = 
      "SELECT gene.feature_id as gene_id, "+
      "       trans.feature_id as transcript_id, "+
      "       exon.feature_id as exon_id, "+
      "       gene.uniquename as gene_uniquename, "+
      geneNameSelect +
      "       genetype.name as genetype, "+
      "       trans.uniquename as transcript_uniquename, "+
      transcriptNameSelect +
      "       trans.type_id as type_id, " + 
      "       exon.uniquename as exon_name,  " +
      "       exonloc.rank, "+
      "       exonloc." + fminCol + " as fmin, "+
      "       exonloc." + fmaxCol + " as fmax, "+
      "       exonloc.strand, "+
      "       exonloc.phase, "+
      "       exonloc.locgroup, "+
      "       trans.timelastmodified as transDate, " +
      "       gene.dbxref_id as geneDbXRefId " +
      "FROM featureloc exonloc, feature exon, feature_relationship exon2trans, feature trans,  " +
      "     feature_relationship trans2gene, feature gene, cvterm genetype " +
      "WHERE exonloc.srcfeature_id = " + srcSeqId + " " +
      "AND exonloc.feature_id = exon.feature_id " +
      "AND exon.type_id = " + getFeatureCVTermId("exon") + " " + 
      "AND exon.feature_id = exon2trans." + subjFeatCol + "  " +
      "AND exon2trans." + objFeatCol + " = trans.feature_id  " +
      "AND exon2trans.type_id = " + partOfCvTermId + " " +
      "AND trans.feature_id = trans2gene." + subjFeatCol + " " +
      "AND trans2gene." + objFeatCol + " = gene.feature_id  " +
      "AND gene.type_id in (" + bufferTypes.toString() + ") " +
      "AND gene.type_id = genetype.cvterm_id "+
      "AND gene.is_analysis = " + getBooleanValue(false) + " " +
      featLocWhereClause +
      "ORDER BY gene.feature_id, trans.feature_id, exonloc." + fminCol + "  ";

    try {
      ResultSet rs = executeLoggedSelectQuery("addAnnotationModels", c, sql);

      // Exons are numbered on a per-gene basis (not per-transcript)
      int exonNum = 1;

      while (rs.next()) {
        int exonBeg = rs.getInt("fmin");
        int exonEnd = rs.getInt("fmax");
        String exonName = rs.getString("exon_name");
        int newTranscriptId = rs.getInt("transcript_id");
        int newGeneId = rs.getInt("gene_id");
        int strand = rs.getInt("strand");

        int exonX1, exonX2;
        if (exonBeg <= exonEnd) {
          exonX1 = exonBeg;
          exonX2 = exonEnd;
        } 
        // NOTE - the following should never happen in Chado (since fmin <= fmax); the code
        // is here to support a couple of older TIGR databases which had incorrect location
        // encodings.
        else {
          exonX2 = exonBeg;
          exonX1 = exonEnd;
          strand = -1;
        }

        // New gene?
        if (newGeneId != currentGeneId) {
          if (currentTranscript != null) { _addExonsToTranscript(currentTranscript, exons); }
          if (currentGene != null) { _addTranscriptsToGene(currentGene, transcripts); }

          currentGene = new AnnotatedFeature();
          currentGeneId = newGeneId;
          String geneName = rs.getString(geneNameAlias);
          String geneIdString = rs.getString("gene_uniquename");
          exonNum = 1;
          currentGene.setId(geneIdString);
          currentGene.setName(geneName);
          currentGene.setStrand(strand);
          // we have only queried for genes. apollo default annot type is actually 
          // "gene", but might as well be sure,found bug once where gene type wasnt set
          currentGene.setFeatureType(rs.getString("genetype"));
          //currentGene.setBioType("gene");
          if (getFeatProps) 
            addAnnotationFeatProps(currentGene,currentGeneId);
          if (getSynonyms)
            addAnnotSynonyms(currentGene,currentGeneId);
          if (getDbXRefs) 
            addAnnotDbXRefs(currentGene,currentGeneId,rs.getInt("geneDbXRefId"));
          genes.addElement(currentGene);
        }

        // New transcript?
        if (newTranscriptId != currentTranscriptId) {
          // add exons to previous transcript
          if (currentTranscript != null) { 
            _addExonsToTranscript(currentTranscript, exons); 
          }
          // now make new transcript 
          currentTranscript = new Transcript();
          currentTranscriptId = newTranscriptId;
          String transcriptName = rs.getString(transcriptNameAlias);
          String transcriptIdString = rs.getString("transcript_uniquename");
          currentTranscript.setRefSequence(refSeq);
          currentTranscript.setId(transcriptIdString);
          currentTranscript.setName(transcriptName);
          currentTranscript.setFeatureType("transcript");

          // Set the AnnotatedFeature type. The type info in on transcript level.
          // A rather weird thing
          // actually this is where apollo diverges from SO - in SO "tRNA" is
          // the transcript type and "gene" is still the annot type - but
          // apollo is hardwired to think of top annots with type "gene" as 
          // protein coding - so we diverge from SO and make the top annot 
          // type the same as the transcript type (also is tRNA actually 
          // a transcript?) so this needs to be rectified - basically apollo
          // needs a better way of figuring if something is protein coding - i
          // think the ultimate solution is gleening this from SO - seeing which
          // types have derives_from polypeptides - a more expedient interim solution
          // would be to put this in the good ol tiers file. this is all saying that
          // apollos featureTypes should be in line with SO, which i think it should
          long type_id = rs.getLong("type_id");
          String typeName = getFeatCvName(new Long(type_id));
          if (currentGene != null && !typeName.equals("mRNA") && !typeName.equals("transcript")) {
            currentGene.setFeatureType(typeName);
          }
          Date transcriptDate = new Date(rs.getTimestamp("transDate").getTime());
          currentTranscript.addProperty("date",transcriptDate.toString());
          if (getFeatProps) // this aint right!
            addAnnotationFeatProps(currentTranscript,currentTranscriptId);
          if (getSynonyms)
            addAnnotSynonyms(currentTranscript,currentTranscriptId);
          currentTranscript.setStrand(strand);
          transcripts.addElement(currentTranscript);
        }

        // Always a new exon
        Exon exon = _makeExon(exonX1, exonX2, strand);
        exon.setRefSequence(refSeq);
        exon.setId(exonName);
        exon.setName(currentTranscript.getId() + " exon " + exonNum++);
        exons.addElement(exon);
      }
      if (currentTranscript != null) { _addExonsToTranscript(currentTranscript, exons); }
      if (currentGene != null) { _addTranscriptsToGene(currentGene, transcripts); }
    } 
    catch (SQLException sqle) {
      logger.error("SQLException retrieving gene features for sequence with feature_id = " + srcSeqId, sqle);
    }

    // Set translation start and stop sites
    Hashtable thash;
    try { thash = this.getCDSFeatures(c,srcSeqId,refSeq,sfs,featLoc); }
    catch (RelationshipCVException e) {
      logger.error("can't retrieve CDS/protein: " + e.getMessage());
      return;
    }
    int ng = genes.size();
    for (int g = 0;g < ng;++g) {
      AnnotatedFeature gene = (AnnotatedFeature)(genes.elementAt(g));

      //Vector tv = gene.getTranscripts();
      int numTranscripts = gene.getNumberOfChildren();//tv.size();
      for (int t = 0; t < numTranscripts; ++t) {
        Transcript originalTrans = (Transcript) (gene.getFeatureAt(t));
        //String tname = originalTrans.getName();
        String transcriptId = originalTrans.getId();
        // hash keys on transcript uniquename which for both tigr and fb is the
        // ID, value is a Vector of ChadoCds's
        Vector cdsFeats = (Vector) (thash.get(transcriptId));

        if (cdsFeats == null) {
          logger.error("addAnnotationModels could not find CDS feature for transcript '" + transcriptId + "'");
        }
        else {
          // If there is more than one CDS feature for the same transcript it
          // means that the gene has alternative translation start and/or stop positions.
          // In this situation we must create multiple copies of the Transcript.
          // This is a rare case isnt it, not sure this even happens for rice & fly
          // JC: yes; perhaps a warning should be printed whenever this code is invoked?
          int numCdsFeats = cdsFeats.size();

          // Copy the original Transcript as many times as necessary
          Transcript newTranscripts[] = new Transcript[numCdsFeats];
          newTranscripts[0] = originalTrans;
          for (int cf = 1; cf < numCdsFeats; ++cf) {
            ChadoCds cds = (ChadoCds) (cdsFeats.elementAt(cf));
            newTranscripts[cf] = _makeNewTranscript(originalTrans, cds);
            gene.addFeature(newTranscripts[cf]);
          }

          for (int cf = 0; cf < numCdsFeats; ++cf) {
            ChadoCds cds = (ChadoCds) (cdsFeats.elementAt(cf));
            Transcript trans = newTranscripts[cf];
            trans.set_cDNASequence(cds.getcDNASequence());
            trans.setPeptideSequence(cds.getProteinSequence());
            cds.checkCdsBounds(trans);
            ci.setTranslationStartAndStop(trans, cds);
          }
        }
      }
      sfs.addFeature(gene);
    }
  } // end of addAnnotationModels()


  /** Retrieve all the feature props for the AnnotatedFeatureI(Transcript or gene), 
      and stuff them in the appropriate places (as props or actual vars).
      2 reasons to do as separate query: may or may not have props so it would
      have to add a troublesome outer join to main query, also may want to get these
      separately on demand (when annot info brought up) */
  private void addAnnotationFeatProps(AnnotatedFeatureI annot,int feature_id) {
    // should we just select fp.type_id and map 
    String sql = "SELECT fp.value, cvterm.name, fp.featureprop_id "+
    "FROM featureprop fp, cvterm "+
    "WHERE fp.feature_id = "+feature_id+" AND fp.type_id = cvterm.cvterm_id";

    String ownerTerm = chadoInstance.getFeatureOwnerPropertyTerm();    
    String altDescrTerm = chadoInstance.getSeqDescriptionTerm();

    try {
      ResultSet rs = executeLoggedSelectQuery("addAnnotationFeatProps", sql);
      while (rs.next()) {
        String type = rs.getString("name");
        String value = rs.getString("value");
        if (type.equals(ownerTerm)) // only makes sense for transcripts
          annot.setOwner(value);
        // "non-canonical_splice_site"?? Transcript ->AnnotatedFeat
        // not sure where to get comment date and internal??
        else if (type.equals("comment")) {
          int featurePropId = rs.getInt("featureprop_id");
          addAnnotComment(annot,value,featurePropId);
        }
        else if (type.equals("problem") && value.equals("true"))
          annot.setIsProblematic(true);
        else if (type.equals("description"))
          annot.setDescription(value);
        else if (type.equals(altDescrTerm)) 
          annot.setDescription(value);
        else
          annot.addProperty(type,value);
      }
    } catch  (SQLException sqle) {
      logger.error("addAnnotationFeatProps: exception retrieving annotation featureprops", sqle);
    }
  }

  /** Queries pub for the comment author. Parses internal flag and date out of
      fullComment string. Adds Comment object to annot. Comment strings look like
      this: 
      Flag Cambridge: gene merge (internal view only)::DATE:2002-05-22 17:32:14::TS:1022103134000 
   */
  private void addAnnotComment(AnnotatedFeatureI annot,String fullComment,
      int featurePropId) {
    // query for author
    String sql = "SELECT pub.uniquename FROM pub, featureprop_pub fpp "+
    "WHERE fpp.featureprop_id = "+featurePropId+" AND fpp.pub_id = pub.pub_id";
    try {
      ResultSet rs = executeLoggedSelectQuery("addAnnotComment", sql);
      boolean hasAuthor = rs.next(); // should be either 1 or 0
      String author = hasAuthor ?  rs.getString("uniquename") : null;
      String[] commStrings = fullComment.split("::");
      String comment = null;
      long timestampLong;
      if (commStrings.length != 3) {
        //used to just bail out if the comments were not in the Apollo "comment::date::timestamp"
        //format - the format was only really used for fetching the timestamp (in this case)
        //seems silly to bail, so will just set the timestamp to the current time
        comment = fullComment;
        timestampLong = Calendar.getInstance().getTimeInMillis();
      }
      else {
        comment = commStrings[0];
        boolean internal = false;
        if (comment.indexOf("(internal view only)") != -1) 
          internal = true;
        String timeStampStr = commStrings[2];
        timeStampStr = timeStampStr.replaceFirst("TS:","");
        timestampLong = new Long(timeStampStr).longValue();
      }
      // comment id???
      String id = null; // ??? are these present somewhere? i think theyre pase
      Comment c = new Comment(id,comment,author,timestampLong);
      annot.addComment(c);
    } catch  (SQLException sqle) {
      logger.error("addAnnotComment", sqle);
    }
  }

  /** addAnnotationModels helper function. queries feature_synonym and synonym with feature_id
   * and adds synonyms to annot (if any found). Filters out self synonyms, synonyms
   * that are in fact just the name or id (in chado for querying ease) */
  private void addAnnotSynonyms(AnnotatedFeatureI annot, int feature_id) {
    String[] sel = new String[]{"s.name","p.uniquename as owner"};
    String[] from = new String[] {"feature_synonym fs","synonym s","pub p"};
    WhereClause w = new WhereClause("fs.feature_id",feature_id);
    w.add("fs.synonym_id","s.synonym_id").add("fs.pub_id","p.pub_id");
    //String sql = "SELECT s.name, fs.pub_id FROM feature_synonym fs, synonym s "+
    //  "WHERE fs.feature_id = "+feature_id+" AND fs.synonym_id = s.synonym_id";
    String sql = makeSql(sel,from,w);
    try {
      ResultSet rs = executeLoggedSelectQuery("addAnnotSynonyms", sql);
      while (rs.next()) {
        String name = rs.getString("name");
        // filter out self synonyms - chado puts name & id in as syns for ease of
        // querying, no need for apollo to display these syns
        // actually filtering out self syns as logical as it seems creates problems
        // when making new name old name gets made a syn if not present - with
        // filtering old name is not present and gets added both in apollo & chado
        // leading to duplicate syns (with different pub ids) - so im commenting
        // out this filter
        //if (name.equals(annot.getName()) || name.equals(annot.getId()))
        //  continue;
        String owner = rs.getString("owner");
        Synonym syn = new Synonym(name,owner);
        //int pubId = rs.getInt("pub_id"); //syn.setOwnerId(pubId);
        annot.addSynonym(syn);
      }
    } catch  (SQLException sqle) {
      logger.error("addAnnotSynonyms", sqle);
    }
  }

  /** addAnnotationModels helper function. queries feature_dbxref, dbxref, and db for db
      cross references. One dbxref is given in the feature table itself, so theres actually
      2 retrievals that are unioned into one query */
  private void addAnnotDbXRefs(AnnotatedFeatureI annot,int feature_id,int dbXRefId) {

    // use a map to eliminate duplicates in the case where a dbxref 
    // is both a primary and a secondary dbxref
    HashMap map = new HashMap();

    // key class for the map
    class dbxrefKey {
      public String db, accession, version;

      dbxrefKey(String db, String accession, String version) {
        this.db = db;
        this.accession = accession;
        this.version = version;
      }

      public int hashCode() {
        return accession.hashCode(); // ok for small maps
      }

      public boolean equals(Object o) {
        if ((o != null) && (o instanceof dbxrefKey)) {
          dbxrefKey k2 = (dbxrefKey)o;
          return (db.equals(k2.db) && accession.equals(k2.accession) && version.equals(k2.version));
        }
        return false;
      }
    }

    String sql = "SELECT dx.accession, dx.version, db.name as dbName, dx.description, 'secondary' as type "+
    "FROM feature_dbxref fd, dbxref dx, db "+
    "WHERE fd.feature_id = "+feature_id+" AND dx.dbxref_id = fd.dbxref_id "+
    "AND dx.db_id = db.db_id "+ 
    "UNION " +
    "SELECT dx.accession, dx.version, db.name, dx.description, 'primary' as type " +
    "FROM dbxref dx, db "+
    "WHERE dbxref_id = "+dbXRefId+" AND dx.db_id = db.db_id";

    try {
      ResultSet rs = executeLoggedSelectQuery("AddAnnotDbXrefs", sql);

      while (rs.next()) {
        String id = rs.getString("accession");
        String db = rs.getString("dbName");
        String version = rs.getString("version");
        String type = rs.getString("type");
        String descr = rs.getString("description");

        // db,id,version is a unique key
        dbxrefKey key = new dbxrefKey(db, id, version);

        Object o = map.get(key);
        DbXref dbx = null;

        if (o != null) {
          logger.trace("found existing dbxref for feature_id=" + feature_id + " dbxref_id=" + dbXRefId + 
              "with db=" + db + " id=" + id + " version=" + version + " type=" + type);

          dbx = (DbXref)o;
        } else {
          logger.trace("creating new dbxref for feature_id=" + feature_id + " dbxref_id=" + dbXRefId + 
              "with db=" + db + " id=" + id + " version=" + version + " type=" + type);

          dbx = new DbXref("id",id,db);
          dbx.setVersion(version);
          dbx.setDescription(descr);
          // set these values to a known state - a workaround for the questionable practice
          // of having the DbXref class default to "secondary"
          dbx.setIsPrimary(false);
          dbx.setIsSecondary(false);
          map.put(key, dbx);
        }

        if (type.equals("primary")) {
          dbx.setIsPrimary(true);
        } else {
          dbx.setIsSecondary(true);
        }
      }
    } catch  (SQLException sqle) {
      logger.error("exception retrieving annot dbxref", sqle);
    }

    // it isn't necessary to add the primary dbxref first
    int np = 0;
    int ndbx = 0;

    Iterator i = map.values().iterator();
    while (i.hasNext()) {
      DbXref dbx = (DbXref)(i.next());
      ++ndbx;
      if (dbx.isPrimary()) {
        ++np;
        // shouldn't have more than 1 primary dbxref
        if (np > 1) {
          logger.error("trying to add multiple primary dbxrefs to feature with feature_id=" + feature_id);
          continue;
        }
      }
      annot.addDbXref(dbx);
    }
    logger.trace("added " + ndbx + " dbxref(s) (" + np + " primary) to feature with feature_id=" + feature_id);
  }

  /**
   * Retrieve a set of search hits (e.g. pairwise alignments) from Chado and convert them into instances
   * of apollo.datamodel.FeaturePair.
   *
   * @param c                         JDBC connection to the database.
   * @param programStr                Retrieve search hits (Chado analysisfeatures) whose Chado 
   *                                  analysis.program matches this string.
   * @param srcRank                   Chado featureloc.rank of the source sequence.
   * @param tgtRank                   Chado featureloc.rank of the target sequence.
   * @param setTargetSeqs             Whether to call <code>setRefSequence</code> on the target Apollo features
   * @param getTargetSeqsSeparately   Whether to get the the target seqs in the main query
   *                                  or retrieve them with a separate query. If most or all of the
   *                                  hits go to 1 or just a handful of sequences, then it makes
   *                                  sense to retrieve separately (tigr is of this ilk). If most
   *                                  of the hits are to separate seqs its better to retrieve in
   *                                  query (flybase is of this ilk).
   * @param getTargetSeqDescriptions  if true retrieve description for target 
   *                                  sequence. descriptions are retrieved by joining with featureprop. only 
   *                                  useful if retrieving target seqs to begin with, if setTargetSeqs = true
   * @param joinWithFeatureProp       Whether to join the analysis features with featureprop.
   * @param refSeq                    Apollo Sequence that corresponds to Chado sequence <code>srcSeqId</code>
   * @param sfs                       StrandedFeatureSet to which the search hits should be added.
   * @param scoreColumn               analysisfeature column to use for primary feature score(setScore)
   * @param setAlignSeqs              If true set explicit query and hit seqs (once we have cigars
   *                                  i suspect we will need a flag for cigar vs explicit aligns
   * @param featLoc                   Has srcfeature_id to use for featureloc as well as where clause
   *                                  for range (needed for non-redundant featlocs) and range padding
   *                                  maybe all the params here could be lumped into a SearchHitsParams object?
   programs should be changed to how one level results does it - ChadoProgram[]
   with programversion and sourcename and getChadoProgramWhereClause
   */
  protected void addSearchHits(Connection c, ChadoProgram[] programs, 
      boolean setTargetSeqs, 
      boolean getTargetSeqsSeparately,
      boolean lazyTgtSeqs, boolean getTargetSeqDescriptions,
      boolean joinWithFeatureProp, SequenceI refSeq, 
      StrandedFeatureSet sfs, String scoreColumn, 
      boolean setAlignSeqs, FeatureLocImplementation featLoc)
  {

    // if there were no programs specified, theres no hits to get
    if (programs == null || programs.length == 0)
      return;

    // Used to register result sequence. It seems there is a bug in GameSave. This is a workaround.
    CurationSet cset = (CurationSet) refSeq.getRange();
    // should make new ones of these for each type if theres multiple types in the query
    Map forwardFeatMap = new HashMap(); // To sort out FeatureSets based on feature type
    Map reverseFeatMap = new HashMap(); 
    //FeatureSet analysisForwardFeatSet = new FeatureSet();
    //analysisForwardFeatSet.setStrand(1);
    //sfs.addFeature(analysisForwardFeatSet);
    //FeatureSet analysisReverseFeatSet = new FeatureSet();
    //analysisReverseFeatSet.setStrand(-1);
    //sfs.addFeature(analysisReverseFeatSet);
    // Count gapped alignments; can't currently display sequences for these
    // JC: The above comment refers to the fact that we don't have the actual 
    // alignment sequence stored in Chado.
    //int numGappedAligns = 0;
    //int numAligns = 0;

    String fminCol = chadoVersion.getFMinCol();
    String fmaxCol = chadoVersion.getFMaxCol();
    //      String abbrevCol = chadoVersion.getAbbrevCol();
    String pvalCol = chadoVersion.getPValCol();
    String identityField = getAnalysisFeatureIdentityField();

    // from array of prog string(sim4,%blastx%) make where clause
    // e.g. AND (a.program like 'sim4' OR a.program like '%blastx')
    StringBuffer programWhere = new StringBuffer();
    for (int i=0; programs!=null && i<programs.length; i++) {
      programWhere.append("a.program like '" + programs[i].getProgram() + "'");
      if (programs[i].getSourcename() != null) {
        programWhere.append(" AND a.sourcename like '" + programs[i].getSourcename() + "'");
      }
      if (i != programs.length-1) 
        programWhere.append(" OR ");
    }
    if (programWhere.length() != 0) {
      programWhere.insert(0," AND (");
      programWhere.append(")");
    }
    
    /*
    String programWhere = null;
    try {
      programWhere = getChadoProgramWhereClause(programs, "a", null);
    }
    catch (AnalysisException e) {
      logger.error("unable to retrieve search program results " + e.getMessage());
      return;
    }
    */
        
    //FeatureLocImplementation featLoc = new FeatureLocImplementation(srcSeqId,getConnection());
    long srcSeqId = featLoc.getContainingFeatureId();

    try {

      String targetSeqSelect = "";
      if (setTargetSeqs && !getTargetSeqsSeparately) 
        targetSeqSelect = "tgtFeat.residues as target_residues,";

      //String tgtDescOuterJoin=""; 
      String tgtDescSubSelect="", tgtDescField="";
      // no point in retrieving tgt seq descrips if we are not retrieving tgt seqs
      if (!setTargetSeqs) 
        getTargetSeqDescriptions = false;
      if (getTargetSeqDescriptions) {
        logger.debug("addSearchHits retrieving target seq descriptions");
        tgtDescField = "tgtDescription";
        //tgtDescSelect = "tgt_fp.value as "+tgtDescField+", ";

        String seqDescrCv = chadoInstance.getSeqDescriptionCVName();
        String seqDescrTerm = chadoInstance.getSeqDescriptionTerm();
        Long descriptionTypeId = getCVTermId(seqDescrCv, seqDescrTerm);

        //tgtDescOuterJoin = "LEFT OUTER JOIN featureprop tgt_fp ON "+
        //"(tgtFeat.feature_id = tgt_fp.feature_id "
        //  +"AND tgt_fp.type_id = "+descriptionTypeId+")";
        // this is much faster than outer join
        tgtDescSubSelect = "(SELECT value FROM featureprop WHERE type_id="
          + descriptionTypeId +" and "+
          "feature_id=tgtFeat.feature_id and rank=0) as "+tgtDescField+",";
        //tgtDescWhere = "AND tgt_fp.feature_id = tgtFeat.feature_id "+
        //  "AND tgt_fp.type_id = "+descriptionTypeId;
      }

      // returns range of feature - empty string if have redundant feat locs,
      String featLocWhereClause = featLoc.getContainingFeatureWhereClause("leafFeatLoc");

      String featLocParentFromTables = "";
      String joinToFeatLocParent = "";
      if (getChadoInstance().searchHitsHaveFeatLocs()) {
        // feat_rel & featloc tables
        featLocParentFromTables = 
          ", feature_relationship leafToParent, featureloc parentFeatLoc";
        // use parent feat loc for query
        featLocWhereClause = featLoc.getContainingFeatureWhereClause("parentFeatLoc");
        // where clause joining feat_rel & featloc
        Long partOfCvId;
        try { partOfCvId = getPartOfCVTermId(); }
        catch (PartOfException pe) {
          logger.error("cannot retrieve search hits: " + pe.getMessage(), pe);
          return;
        }
        joinToFeatLocParent = "AND leafToParent.subject_id = leafFeat.feature_id "+
        "AND leafToParent.object_id = parentFeatLoc.feature_id "+
        "AND leafToParent.type_id = "+partOfCvId +
        " AND parentFeatLoc.srcfeature_id = " + srcSeqId;
      }

      // if doing lazy tgt seqs need to get the length of the seq
      String lazySelect = "";
      if (lazyTgtSeqs) {
        String lengthFn = getClobLengthFunction();
        lazySelect = lengthFn + "(tgtFeat.residues) as tgtSeqLength,";
      }

      // HACK - featureloc has to appear first in the FROM clause, otherwise PostgresQL 7.3 chokes
      String sql = "\nSELECT leafFeat.uniquename, leafFeatLoc." + fminCol + 
      " AS query_fmin, "+
      "leafFeatLoc." + fmaxCol + " AS query_fmax, " +
      " leafFeatLoc.strand AS query_strand, " +
      "tgtFeat.feature_id AS target_id, tgtFeat.uniquename AS targetName, "+
      //          "o2." + abbrevCol + " AS target_species, " +
      "tgtFeat.organism_id AS tgtOrganismId, "+
      "tgtFeatLoc." + fminCol + " AS target_fmin, " +
      "tgtFeatLoc." + fmaxCol + " AS target_fmax, "+
      "tgtFeatLoc.strand AS target_strand, " +
      "a.analysis_id, a.program, a.sourcename, a.programversion, af.rawscore, "+
      "af.normscore, af.significance, af." + identityField + ", " +
      "leafFeatLoc.residue_info AS query_residue_info, "+
      targetSeqSelect +
      tgtDescSubSelect +
      "leafFeat.type_id as featureTypeId, "+
      lazySelect +
      "tgtFeatLoc.residue_info AS target_residue_info "+
      (joinWithFeatureProp ? ", fp." + pvalCol + " AS feature_prop " : "") + ", " +
      "leafToParent.object_id AS parent_id" +

      "\nFROM featureloc leafFeatLoc, feature leafFeat, " +
      (joinWithFeatureProp ? "featureprop fp, " : "") +
      "analysisfeature af, analysis a, featureloc tgtFeatLoc, "+
      "feature tgtFeat " + //tgtDescOuterJoin + //", "+
      featLocParentFromTables + // "" if no parent feat locs
      //          "organism o2 " +

      "\nWHERE leafFeatLoc.srcfeature_id = " + srcSeqId + " " +
      //  "AND leafFeatLoc.rank = " + srcRank+" "+"AND tgtFeatLoc.rank ="+tgtRank+" "+
      "AND leafFeatLoc.feature_id = leafFeat.feature_id  " +
      "AND leafFeatLoc.feature_id = af.feature_id " +
      "AND af.analysis_id = a.analysis_id " +
      "AND leafFeat.feature_id = tgtFeatLoc.feature_id " +
      "AND tgtFeatLoc.srcfeature_id != "+ srcSeqId +
      " AND tgtFeatLoc.srcfeature_id = tgtFeat.feature_id " +
      //          "AND tgtFeat.organism_id = o2.organism_id " + 
      (joinWithFeatureProp ? "AND fp.feature_id = leafFeat.feature_id " : "") +
      //((programStr != null) ? " AND a.program like '" + programStr + "' " : "") +
      programWhere +
      featLocWhereClause + // either parent or leaf
      joinToFeatLocParent + // empty string if no parent feat loc
      
      //" AND tgtFeat.uniquename = 'gi|70992245|ref|XP_750971.1|'" +
      
      "\nORDER BY a.program, a.programversion, a.sourcename, tgtFeat.organism_id "
      //  o2." + abbrevCol 
      + ", tgtFeat.uniquename "
      + ", leafFeatLoc.strand "
      + ", leafToParent.object_id";
      
      ResultSet rs = executeLoggedSelectQuery("addSearchHits", c, sql);

      String lastTargetSp = null;
      String lastTargetName = null;
      String lastSourceName = null;
      String lastFeatType = null;
      int lastStrand = 0;
      long lastParentId = 0;
      Vector featurePairs = new Vector();

      while (rs.next()) {
        int fmin = rs.getInt("query_fmin");
        int fmax = rs.getInt("query_fmax");
        int strand = rs.getInt("query_strand");
        String name = rs.getString("uniquename"); // name of the HSP/alignment object 
        //          String alignType = rs.getString("feature_type"); // type of HSP/alignment object
        String alignType = getFeatCvName(rs.getInt("featureTypeId")); // prefetched
        String queryResidueInfo = rs.getString("query_residue_info");

        String targetChadoName = rs.getString("targetName"); // uniquename
        long targetId = rs.getLong("target_id");
        //String targetSp = rs.getString("target_species");
        int organismId = rs.getInt("tgtOrganismId");
        String targetSp = getOrganismAbbrev(organismId);
        if (targetSp == null) // this happens in fly w comp results (dummy organism)
          targetSp = getOrganismFullName(organismId);
        int tgt_fmin = rs.getInt("target_fmin");
        int tgt_fmax = rs.getInt("target_fmax");
        int tgt_strand = rs.getInt("target_strand");
        String tgtResidueInfo = rs.getString("target_residue_info");

        String program = rs.getString("program");
        // workaround the fact that analysis.sourcename can be null
        String sourcename = rs.getString("sourcename") != null ?
            rs.getString("sourcename") : "";
        String programversion = rs.getString("programversion");
        long analysisId = rs.getLong("analysis_id");
        String targetResidues =  null;
        if (setTargetSeqs && !getTargetSeqsSeparately) 
          targetResidues = rs.getString("target_residues");
        long parentId = rs.getLong("parent_id");
        
        String featName = null;
        String tgtFeatName = null;

        ChadoFeatureLoc queryLoc, targetLoc;


        tgtFeatName = getChadoInstance().getTargetName(targetChadoName,targetSp,alignType);

        // JC: this is a special case for SNPs
        if (alignType.equals("SNP")) {
          queryLoc = new ChadoFeatureLoc(fmin, fmin, false, false, strand);
          targetLoc = new ChadoFeatureLoc(tgt_fmin, tgt_fmin, false, false, tgt_strand);
          featName = "SNP (" + queryResidueInfo + ".." + tgtResidueInfo + ")";
          //tgtFeatName = featName;
        } 
        else {
          queryLoc = new ChadoFeatureLoc(fmin, fmax, false, false, strand);
          targetLoc = new ChadoFeatureLoc(tgt_fmin, tgt_fmax, false, false, tgt_strand);
          featName = name;
          //tgtFeatName = targetSp + " " + targetChadoName;
        }

        String featProp = null;
        if (joinWithFeatureProp) {
          featProp = rs.getString("feature_prop");
        }

        String featType = getFeatureType(alignType,program,programversion,
            targetSp,sourcename,featProp);

        // should be making new feat sets if featType changes
        // doesnt seem to matter that the type is set here does it?
        // nor does it seem to matter to make a whole new feat set
        // for each analysis type - should i anyways as game does
        //analysisForwardFeatSet.setType(featType);
        //analysisReverseFeatSet.setType(featType);

        //++numAligns;

        // Query SeqFeature
        SeqFeature queryFeat = _makeSeqFeature(queryLoc.getFmin(), queryLoc.getFmax(), featType, queryLoc.getStrand());
        queryFeat.setName(featName);
        queryFeat.setId(featName);

        if (scoreColumn!=null) {
          queryFeat.setScore(rs.getDouble(scoreColumn));
        }

        else if (alignType.equals("SNP")) { // tigr
          queryFeat.setScore(100.0);
        } //else {
        float pIdentity = rs.getFloat(identityField);
        //queryFeat.setScore(pIdentity);
        queryFeat.addScore(new Score("identity", pIdentity));
        //}

        queryFeat.addScore(new Score("rawscore", rs.getFloat("rawscore")));
        queryFeat.addScore(new Score("normscore", rs.getFloat("normscore")));
        queryFeat.addScore(new Score("significance", rs.getFloat("significance")));

        // Target SeqFeature
        SeqFeature tgtFeat = _makeSeqFeature(targetLoc.getFmin(), targetLoc.getFmax(), featType, targetLoc.getStrand());


        // Set target/hit reference sequence if flagged to do so
        if (setTargetSeqs) {
          // Only set reference sequence if this looks like an ungapped alignment (may fail if there are compensatory indels)
          // TO DO - accomodate indels somehow (e.g., via storage of "cigar" lines in chado)
          // flybase displays target seqs even if different length, tigr just for debug
          //if (queryLoc.getLength() == targetLoc.getLength()) {
          // sepearate query for seq
          SequenceI tgtRefSeq=null;
          if (getTargetSeqsSeparately) {
            int tgtSeqLength = rs.getInt("tgtSeqLength");
            tgtRefSeq = getSequence(targetId,lazyTgtSeqs,tgtFeatName,tgtSeqLength);
          }
          // check if Sequence already made? does it matter? is it bad to have dups
          // trade-off inefficieny in hashing and checking hash vs. memory excess
          // of duplicating seqs
          else 
            tgtRefSeq = new Sequence(tgtFeatName,targetResidues);

          if (tgtRefSeq != null) {
            tgtFeat.setRefSequence(tgtRefSeq);
            cset.addSequence(tgtRefSeq); // register this sequence. Otherwise, it will not be saved.
            if (getTargetSeqDescriptions) {
              tgtRefSeq.setDescription(rs.getString(tgtDescField));
              // query for each description separately. description may not be present
              // and outer join is really slow. separate queries is faster, but still
              // slow - need better solution. on-demand? background load? collect all 
              // tgt ids and do an in (id list)? rather pesky problem
              //String desc = getDescriptionFeatProp(c,targetId);
              //if (desc != null)
              //  tgtRefSeq.setDescription(desc);
            }

            // Additional sanity check for SNPs; the base indicated in the featureprop 
            // should match the appropriate base in the underlying sequence.
            if (alignType.equals("SNP")) {
              String queryBase = refSeq.getResidues(queryFeat.getStart(), queryFeat.getEnd());
              String tgtBase = tgtRefSeq.getResidues(tgtFeat.getStart(), tgtFeat.getEnd());
              boolean isRev = tgt_fmax < tgt_fmin;

              if (!queryBase.toUpperCase().equals(queryResidueInfo.toUpperCase())) {
                logger.warn("SNP query base '" + queryResidueInfo + "' does not match sequence at position " +
                    queryFeat.getStart() + " - '"+ queryBase +"'");
                logger.warn("query locn=" + fmin + "-" + fmax + " target locn=" + tgt_fmin + "-" + tgt_fmax +
                    " isrev=" + isRev);
              }

              if (!tgtBase.toUpperCase().equals(tgtResidueInfo.toUpperCase())) {
                logger.warn("SNP target base '" + tgtResidueInfo + "' does not match sequence at position " +
                    tgtFeat.getStart() + " - '"+ tgtBase +"'");
                logger.warn("query locn=" + fmin + "-" + fmax + " target locn=" + tgt_fmin + "-" + tgt_fmax +
                    " isrev=" + isRev);
              }
            }
          }
          //} else { ++numGappedAligns; }
        }
        tgtFeat.setName(tgtFeatName);
        tgtFeat.setId(tgtFeatName);

        // new target is in - make feature set of feature pairs from last target and
        // clear out featurePairs for new target feature set
        //if (targetSp !=lastTargetSp) { // cant use != on String
        // BUGFIX can have same tgt name from 2 diff sourcenames - need to chech src
        
        if (!targetChadoName.equals(lastTargetName) ||
            (lastStrand != strand) ||
            (!sourcename.equals(lastSourceName))
            || lastParentId != parentId) {
          // if not first seqfeat - make feat set
          if (lastTargetName != null) { 
            FeatureSet targetFeatSet = _makeFeatureSet(featurePairs, lastTargetSp, 
                lastFeatType, lastStrand);
            FeatureSet featureSet = getFeatureSetForType(lastFeatType,
                targetFeatSet.isForwardStrand(),
                forwardFeatMap,
                reverseFeatMap);
            featureSet.addFeature(targetFeatSet);

            //sfs.addFeature(targetFeatSet);
            
            featurePairs.clear();
          }
          lastTargetSp = targetSp;
          lastTargetName = targetChadoName;
          lastSourceName = sourcename;
          lastFeatType = featType;
          lastStrand = queryLoc.getStrand().intValue();
          lastParentId = parentId;
        }
        FeaturePair fp = new FeaturePair(queryFeat, tgtFeat);
        // add tgt seq desc as query description property (for evidence panel desc)
        SequenceI tgtSeq = tgtFeat.getRefSequence();
        if (tgtSeq != null) 
          queryFeat.addProperty("description",tgtSeq.getDescription());

        // add alignments, for now doing explicit aligns. rewrite when cigars come in 
        if (setAlignSeqs) {
          fp.setExplicitAlignment(queryResidueInfo);
          tgtFeat.setExplicitAlignment(tgtResidueInfo);
        }

        featurePairs.addElement(fp);

      } // end of rs.next() result set iterating

      // make FeatureSet of last set of FeaturePairs
      if (lastTargetName != null) {
        FeatureSet targetFeatSet = _makeFeatureSet(featurePairs, lastTargetSp, 
            lastFeatType, lastStrand);
        FeatureSet featureSet = getFeatureSetForType(lastFeatType,
            targetFeatSet.isForwardStrand(),
            forwardFeatMap,
            reverseFeatMap);
        featureSet.addFeature(targetFeatSet);

        //sfs.addFeature(targetFeatSet);
        
        featurePairs.clear();
      }
      addFeatureToParents(sfs, forwardFeatMap);
      addFeatureToParents(sfs, reverseFeatMap);
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving analysisfeatures for sequence with feature_id = " + srcSeqId, sqle);
    }
    // TO DO - need cigar support in both Chado and Apollo before anything can be done about this
    //     if (numGappedAligns > 0) {
    //       logger.warn(numGappedAligns + "/" + numAligns + " " + programStr + 
    // 		     " alignment(s) are gapped and their sequences therefore cannot be displayed");
    //     }
  } // end of addSearchHits()

  private FeatureSet getFeatureSetForType(String featureType, 
      boolean isForward, 
      Map forwardMap,
      Map reverseMap) {
    FeatureSet feature = null;
    if (isForward) {
      feature = (FeatureSet) forwardMap.get(featureType);
      if (feature == null) {
        feature = new FeatureSet();
        feature.setStrand(1);
        feature.setFeatureType(featureType);
        forwardMap.put(featureType, feature);
      }
    }
    else {
      feature = (FeatureSet) reverseMap.get(featureType);
      if (feature == null) {
        feature = new FeatureSet();
        feature.setStrand(-1);
        feature.setFeatureType(featureType);
        reverseMap.put(featureType, feature);
      }
    }
    return feature;
  }

  /** better alternative to getFeatureSetForType - 
      should replace it in gene predics...*/
  private SeqFeatureI getTierFeatForType(String type, int strand, 
      StrandedFeatureSet sfs) {
    SeqFeatureI tierFeat = sfs.getSeqFeat(strand,type);
    if (tierFeat == null) { // doesnt exist yet
      tierFeat = new FeatureSet(type,strand);
      tierFeat.setName(type); // ???
      sfs.addFeature(tierFeat);
    }
    return tierFeat;
  }

  /**
   * Retrieve a set of protein alignments from Chado and convert them into instances of apollo.datmodel.FeaturePair.
   * 
   * @param c             JDBC connection to the database.
   * @param srcSeqId      Chado feature_id of the sequence whose protein alignments are to be returned.
   * @param programStr    Retrieve search hits (Chado analysisfeatures) whose Chado analysis.program matches this string.
   * @param sfs           StrandedFeatureSet to which the protein alignment features should be added.
   */
  protected void addProteinAlignments(Connection c, String programStr, 
      StrandedFeatureSet sfs, 
      FeatureLocImplementation featLoc)
  {
    String fminCol = chadoVersion.getFMinCol();
    String fmaxCol = chadoVersion.getFMaxCol();
    String abbrevCol = chadoVersion.getAbbrevCol();
    String identityField = getAnalysisFeatureIdentityField();
    long srcSeqId = featLoc.getContainingFeatureId();

    try {
      String sql = "SELECT p1.uniquename as query_name, " +
      " fl1." + fminCol + " AS query_fmin, " +
      " fl1." + fmaxCol + " AS query_fmax, " + 
      " fl1.strand AS query_strand, " +
      " g2.uniquename AS target_seq, " +
      " o2." + abbrevCol + " AS target_species, " +
      " p2.uniquename AS target_name, " +
      " fl4." + fminCol + " AS target_fmin, " +
      " fl4." + fmaxCol + " AS target_fmax, " +
      " fl4.strand AS target_strand, " +
      " a.program, " +
      " a.programversion, " +
      " af." + identityField + ", " +
      " af.rawscore, " +
      " af.normscore, " +
      " af.significance " +
      "FROM featureloc fl1, featureloc fl2, analysisfeature af, analysis a, " +
      "featureloc fl3, feature p1, feature p2, featureloc fl4, feature g2, organism o2 " +
      "WHERE fl1.srcfeature_id = " + srcSeqId + " " +
      "AND fl1.feature_id = fl2.srcfeature_id " +
      "AND fl1.feature_id = p1.feature_id " +
      "AND fl2.feature_id = af.feature_id " +
      "AND fl1.rank != fl2.rank " +
      "AND af.analysis_id = a.analysis_id " +
      ((programStr != null) ? "AND a.program = '" + programStr + "' " : "") +
      "AND fl2.feature_id = fl3.feature_id " +
      "AND fl3.srcfeature_id = p2.feature_id " +
      "AND p1.feature_id != p2.feature_id " +
      "AND p2.feature_id = fl4.feature_id " +
      "AND fl4.srcfeature_id = g2.feature_id " +
      "AND g2.organism_id = o2.organism_id " +
      "ORDER BY g2.uniquename ";

      ResultSet rs = executeLoggedSelectQuery("addProteinAlignments", sql);

      String lastTargetSp = null;
      String lastFeatType = null;
      int lastStrand = 0;
      Vector featurePairs = new Vector();

      while (rs.next()) {
        int fmin = rs.getInt("query_fmin");
        int fmax = rs.getInt("query_fmax");
        String name = rs.getString("query_name");   // name of the HSP/alignment object 
        int strand = rs.getInt("query_strand");

        String target = rs.getString("target_name");
        String targetSp = rs.getString("target_species");
        int tgt_fmin = rs.getInt("target_fmin");
        int tgt_fmax = rs.getInt("target_fmax");
        int tgt_strand = rs.getInt("target_strand");

        String program = rs.getString("program");
        String programversion = rs.getString("programversion");
        String featType = program + " " + programversion + " " + targetSp;

        ChadoFeatureLoc queryLoc = new ChadoFeatureLoc(fmin, fmax, false, false, strand);
        ChadoFeatureLoc targetLoc = new ChadoFeatureLoc(tgt_fmin, tgt_fmax, false, false, tgt_strand);

        // Query SeqFeature
        SeqFeature queryFeat = _makeSeqFeature(queryLoc.getFmin(),queryLoc.getFmax(),featType,queryLoc.getStrand());
        queryFeat.setName(name);

        float pIdentity = rs.getFloat(identityField);
        queryFeat.addScore(new Score("identity", pIdentity));
        queryFeat.addScore(new Score("rawscore", rs.getFloat("rawscore")));
        queryFeat.addScore(new Score("normscore", rs.getFloat("normscore")));
        queryFeat.addScore(new Score("significance", rs.getFloat("significance")));
        queryFeat.setScore(pIdentity);

        // Target SeqFeature
        SeqFeature tgtFeat = _makeSeqFeature(targetLoc.getFmin(),targetLoc.getFmax(),featType,targetLoc.getStrand());
        tgtFeat.setName(targetSp + " " + target);

        if (targetSp != lastTargetSp) {
          if (lastTargetSp != null) {
            sfs.addFeature(_makeFeatureSet(featurePairs, lastTargetSp, lastFeatType, lastStrand));
            featurePairs.clear();
          }
          lastTargetSp = targetSp;
          lastFeatType = featType;
          lastStrand = queryLoc.getStrand().intValue();
        }
        featurePairs.addElement(new FeaturePair(queryFeat, tgtFeat));
      }
      if (lastTargetSp != null) {
        sfs.addFeature(_makeFeatureSet(featurePairs, lastTargetSp, lastFeatType, lastStrand));
        featurePairs.clear();
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving analysisfeatures for sequence with feature_id = " + srcSeqId, sqle);
    }
  } // end of addProteinAlignments()

  /**
   * Translates String[] programStr to ChadoProgram[] programStr and calls addGenePredictionResults (..., ChadoProgram[]...)
   * 
   * @param c JDBC connection to the database.
   * @param programStr Retrieve predictions (Chado analysisfeatures) whose Chado analysis.program matches this string. (eg genscan, piecegenie) Can do
   *          multiple programs "in ('piecegenie','genscan')" or "= 'genscan'"
   * @param parentFeatSet StrandedFeatureSet to which the gene prediction features should be added.
   * @param featLoc Has srcfeature_id to use for featureloc as well as where clause for range (needed for non-redundant featlocs) and range padding
   *          This was made for flybase - will probably need some tweeking if tigr or some other db needs to use this.
   * @deprecated use chadoProgram array instead of string array
   * @see #addGenePredictionResults(Connection, ChadoProgram[], StrandedFeatureSet, FeatureLocImplementation)
   */
  protected void addGenePredictionResults(Connection c, 
      String[] programStr,
      StrandedFeatureSet parentFeatSet, 
      FeatureLocImplementation featLoc,
      SequenceI refSeq
  ) 
  {
    ChadoProgram[] progArray = new ChadoProgram[programStr.length];
    for (int i = 0; i < programStr.length; i++) {
      progArray[i] = new ChadoProgram(programStr[i], false);
    }
    addGenePredictionResults(c, progArray, parentFeatSet, featLoc, refSeq);
  }

  /**
   * Retrieve gene predictions and add them to parentFeatSet as SeqFeatures. 
   * By default the score column is 'significance'.
   * 
   * @param c JDBC connection to the database.
   * @param programStr Retrieve predictions (Chado analysisfeatures) whose Chado analysis.program matches this program (chadoProgram.getName). (eg
   *          genscan, piecegenie) Can do multiple programs "in ('piecegenie','genscan')" or "= 'genscan'"
   * @param parentFeatSet StrandedFeatureSet to which the gene prediction features should be added.
   * @param featLoc Has srcfeature_id to use for featureloc as well as where clause for range (needed for non-redundant featlocs) and range padding
   *          This was made for flybase - will probably need some tweeking if tigr or some other db needs to use this.
   * @see ChadoProgram
   */  

  protected void addGenePredictionResults(Connection c,
      ChadoProgram[] programs, 
      StrandedFeatureSet parentFeatSet,
      FeatureLocImplementation featLoc, 
      SequenceI refSeq ) 
  {
    addGenePredictionResults(c, programs, parentFeatSet, featLoc, refSeq,"significance");
  }
  /**
   * Retrieve gene predictions and add them to parentFeatSet as SeqFeatures.
   * 
   * @param c JDBC connection to the database.
   * @param programStr Retrieve predictions (Chado analysisfeatures) whose Chado analysis.program matches this program (chadoProgram.getName). (eg
   *          genscan, piecegenie) Can do multiple programs "in ('piecegenie','genscan')" or "= 'genscan'"
   * @param parentFeatSet StrandedFeatureSet to which the gene prediction features should be added.
   * @param featLoc Has srcfeature_id to use for featureloc as well as where clause for range (needed for non-redundant featlocs) and range padding
   *          This was made for flybase - will probably need some tweeking if tigr or some other db needs to use this.
   * @param scoreColumn  analysisfeature column to use for primary feature 
   * @see ChadoProgram
   */
  protected void addGenePredictionResults(Connection c,
      ChadoProgram[] programs, 
      StrandedFeatureSet parentFeatSet,
      FeatureLocImplementation featLoc, 
      SequenceI refSeq,String scoreColumn) 
  {
    if (programs == null || programs.length == 0)
      return; // no prediction programs to query on

    ChadoInstance chadoInstance = getChadoInstance();

    Long exonCvId = getFeatureCVTermId("exon");
    Long partOfCvId;
    try {
      partOfCvId = getPartOfCVTermId();
    }
    catch (PartOfException pe) {
      logger.error("PartOfException retrieving gene predictions: " + pe.getMessage(), pe);
      return;
    }

    //FeatureLocImplementation featLoc = new FeatureLocImplementation(srcFeatId,getConnection());
    long srcFeatId = featLoc.getContainingFeatureId();
    // returns range of feature - empty string if have redundant feat locs
    String featLocWhereClause = featLoc.getContainingFeatureWhereClause("transLoc");
    //String featLocWhereClause = featLoc.getContainingFeatureWhereClause("exonLoc");
    
    // "in(...)" clause of anlaysis ids - postgres7.3 speeds up by a factor of 2 if 
    // i take analysis out of the join - 7.3 is lame lame lame

    if (logger.isDebugEnabled()) {
      StringBuffer msg = new StringBuffer();
      msg.append("addGenePredictionResults querying for the following programs: ");
      for (int i = 0; i < programs.length; i++) {
        if (i > 0) { msg.append(", "); }
        msg.append(programs[i]);
      }
      logger.debug(msg.toString());
    }

    String analysisAlias = chadoInstance.cacheAnalysisTable() ? null : "a";
    String analysisWhereString = null;

    try {    
      analysisWhereString = getChadoProgramWhereClause(programs, "af", analysisAlias);
    }
    catch (AnalysisException e) {
      logger.error("unable to retrieve gene prediction results " + e.getMessage());
      return;
    }

    String sql = "SELECT exon.uniquename as exon_name, exonLoc.fmin, exonLoc.fmax, exonLoc.strand, "
      + "af."+scoreColumn+", af.analysis_id, trans.feature_id as transId, trans.uniquename as transUniquename, trans.name as transName "
      + (chadoInstance.cacheAnalysisTable() ? "" : ", a.program as analysis_program, a.sourcename as analysis_source ") 
      + "FROM featureloc exonLoc, feature exon, analysisfeature af, "
      + "feature_relationship exon2trans, feature trans, featureloc transLoc "
      + (chadoInstance.cacheAnalysisTable() ? "" : ", analysis a ") 
      + "WHERE transLoc.srcfeature_id = " + srcFeatId  + " "
      + "AND exonLoc.feature_id = exon.feature_id "
      + "AND exonLoc.srcfeature_id = " + srcFeatId + " "
      + "AND transLoc.feature_id = af.feature_id " 
      + analysisWhereString + " "
      + "AND exon.type_id = " + exonCvId + " "
      + "AND exon2trans.subject_id = exon.feature_id "
      + "AND trans.feature_id = exon2trans.object_id "
      + "AND exon2trans.type_id = " + partOfCvId + " " 
      + "AND transLoc.feature_id = trans.feature_id " + featLocWhereClause + " "
      + (chadoInstance.cacheAnalysisTable() ? "" : " AND a.analysis_id = af.analysis_id ")
      + "ORDER BY af.analysis_id, trans.feature_id, exonLoc.fmin ";

    // Get Predicted CDS
    // Prepare the hashmap of predicted CDS's
    // JC: why are we initializing a Hashtable() here when getCDSFeatures will overwrite it?
    Hashtable predictedCDS = new Hashtable();
    try {
      predictedCDS = getCDSFeatures(c, (long) featLoc.getFeatureId(),
          parentFeatSet.getFeatureSequence(), parentFeatSet, featLoc, programs);
    } catch (RelationshipCVException e) {
      logger.error("RelationshipCVException calling getCDSFeatures()", e);
    }

    // END Get Predicted CDS

    try {
      ResultSet rs = executeLoggedSelectQuery("addGenePredictionResults", c, sql);
      int lastTranscriptId = -1;
      String lastTranscriptUniquename = null;
      String featType = null;
      String lastProgram = null;
      String lastAnalysisSource = null;
      FeatureSet transcriptFeatSet = null;
      Map forwardMap = new HashMap();
      Map reverseMap = new HashMap();
      Vector exons = new Vector();

      boolean resultSetEmpty = true;
      while (rs.next()) {
        resultSetEmpty = false;
        int transcriptId = rs.getInt("transId");
        int fmin = rs.getInt("fmin");
        int fmax = rs.getInt("fmax");
        int strand = rs.getInt("strand");
        double score = rs.getDouble(scoreColumn);
        String transUniquename = rs.getString("transUniquename");
        String transName = rs.getString("transName");

        long analysisId = rs.getLong("analysis_id");
        String analysisSource, analysisProgram;
        // TODO: add analysis.programversion somewhere?

        // analysis table cached - retrieve analysis info from the cache
        //
        if (chadoInstance.cacheAnalysisTable()) {
          AnalysisTable at = getAnalysisTable();
          AnalysisRow ar = at.getAnalysisRow(analysisId);
          analysisSource = ar.sourcename;
          analysisProgram = ar.program;
        }

        // analysis table not cached - the required columns should be in the query result
        // 
        else {
          analysisSource = rs.getString("analysis_source");
          analysisProgram = rs.getString("analysis_program");
        }
        
        // workaround if analysis.sourcename is null (usually not recommended in chado
        // databases, but the schema allows it so we're bound to find cases where it is
        // null...)
        if (analysisSource == null) {
          // instantiate an empty string rather than a null string
          analysisSource = "";
        }

        if (!analysisProgram.equals(lastProgram) 
            || !analysisSource.equals(lastAnalysisSource)) {
          featType = getFeatureType(null,analysisProgram,null,null,analysisSource,null);
          // make analysis feature set? do we actually need it?
        }

        SeqFeature exon = _makeSeqFeature(fmin,fmax,featType,strand);
        exon.setId(rs.getString("exon_name"));

        // TODO: this looks like something that should be configurable
        // sima suggestion name like "Genscan:4000-4500"
        String rng = exon.getStart()+"-"+exon.getEnd();
        String exonName = analysisProgram+":"+rng;//rs.getString("exonName");
        exon.setName(exonName);
        exon.setScore(score);
        exon.setProgramName(analysisProgram);
        if (refSeq != null) exon.setRefSequence(refSeq);

        // finish instantiating last transcript (if any) and create a new one
        if (transcriptId != lastTranscriptId) {
          // set name and translation start/stop of last transcript
          if (transcriptFeatSet != null) {
            _addFeaturesToFeatureSet(transcriptFeatSet, exons);
            setTranscriptNameAndCdsBounds(transcriptFeatSet, lastTranscriptUniquename, lastProgram, predictedCDS);
          }
          // make new transcript
          transcriptFeatSet = _makeFeatureSet(featType,strand);
          transcriptFeatSet.setId(transUniquename);
          transcriptFeatSet.setName(transName);
          setSyntenyInfo(transcriptFeatSet,transcriptId);
          transcriptFeatSet.setProgramName(analysisProgram);
          if (refSeq != null) transcriptFeatSet.setRefSequence(refSeq);
          // getTierFeatFortype new hip way
          SeqFeatureI programFeature = getTierFeatForType(analysisProgram,
              transcriptFeatSet.getStrand(),parentFeatSet);
          programFeature.addFeature(transcriptFeatSet);
          lastTranscriptId = transcriptId;
        }
        exons.addElement(exon);

        lastProgram = analysisProgram;
        lastAnalysisSource = analysisSource;
        lastTranscriptUniquename = transUniquename;
      } // end of while rs.next ResultSet iteration

      if (resultSetEmpty)
        return;

      // set name and translation start/stop of last transcript
      _addFeaturesToFeatureSet(transcriptFeatSet, exons);
      setTranscriptNameAndCdsBounds(transcriptFeatSet, lastTranscriptUniquename, lastProgram, predictedCDS);

      // dont see why maps are needed - both of these do the same thing
      addFeatureToParents(parentFeatSet, forwardMap);
      addFeatureToParents(parentFeatSet, reverseMap);
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving gene predictions "
          +"for sequence with feature_id = " + srcFeatId, sqle);
    }
  }

  /**
   * A method that copies all the annotated protein-coding genes into a result tier.  This can be useful
   * if the curators wish to refer back to the version of the annotation that was most recently loaded
   * into Apollo.
   */
  protected void copyAnnotatedGenesIntoResultTier(StrandedFeatureSet annotations, StrandedFeatureSet results, 
      String tierType, SequenceI refSeq) 
  {
    Vector allAnnotFeats = annotations.getFeatures();
    int nf = allAnnotFeats.size();

    // look at all annotated features
    for (int f = 0;f < nf;++f) {
      SeqFeatureI sf = (SeqFeatureI)(allAnnotFeats.elementAt(f));
      String type = sf.getTopLevelType();
      if (sf instanceof FeatureSetI) {
        FeatureSetI gene = (FeatureSetI)sf;

        // ignore everything except protein-coding genes
        if (gene.isProteinCodingGene()) {

          // get children of gene (transcripts)
          int nt = gene.getNumberOfChildren();
          for (int t = 0;t < nt;++t) {
            SeqFeatureI transFeat = gene.getFeatureAt(t);
            if (transFeat instanceof Transcript) {
              Transcript trans = (Transcript)transFeat;

              // create new (result feature) transcript
              FeatureSet tfs = _makeFeatureSet(tierType, trans.getStrand());
              tfs.setId(trans.getId() + "_copy");
              tfs.setName(trans.getName() + "_copy");
              tfs.setProgramName(tierType);
              if (refSeq != null) tfs.setRefSequence(refSeq);
              SeqFeatureI programFeature = getTierFeatForType(tierType, tfs.getStrand(), results);
              programFeature.addFeature(tfs);

              // get children of transcript (exons)
              int ne = trans.getNumberOfChildren();
              for (int e = 0;e < ne;++e) {
                SeqFeatureI exon = trans.getFeatureAt(e);
                if (exon.isExon()) {
                  SeqFeature ef = new SeqFeature(exon.getLow(), exon.getHigh(), tierType, exon.getStrand());
                  ef.setId(exon.getId() + "_copy");
                  ef.setName(exon.getName() + "_copy");
                  if (refSeq != null) ef.setRefSequence(refSeq);
                  tfs.addFeature(ef);
                } else {
                  logger.warn("copyAnnotatedGenesIntoResultTier: found non-exon descendant of transcript " + transFeat + ": " + exon);
                }
              }

              // set translation start/end
              tfs.setTranslationStart(trans.getTranslationStart());
              tfs.setTranslationEnd(trans.getTranslationEnd());
            } else {
              logger.warn("copyAnnotatedGenesIntoResultTier: found non-transcript descendant of gene " + gene + ": " + transFeat);
            }
          }
        }
      }
    }
  }

  /**
   * Helper method for addGenePredictionResults
   */
  private void setTranscriptNameAndCdsBounds(FeatureSet tfs, String transName, String program, Hashtable cdsHash) {

    // set transcript name
    String name = getChadoInstance().getTranscriptName(tfs,program);
    tfs.setName(name);
    // set transcript translation start and stop
    Vector predCds = (Vector) cdsHash.get(transName);

    if (predCds != null) {
      int nps = predCds.size();

      // only one CDS by predicted transcript (i.e., one polypeptide)
      if (nps == 1) {
        ChadoCds pcds = (ChadoCds) predCds.firstElement();
        getChadoInstance().setTranslationStartAndStop(tfs, pcds);
      } 
      // multiple or 0 CDS features for a single predicted transcript
      else {
        logger.error("addGenePredictions retrieved " + nps + " CDS features for transcript " + transName);
      }
    } 
    else {
      logger.error("addGenePredictions could not find CDS feature for transcript '" + transName + "'");
    }

    if (logger.isDebugEnabled()) {
      StringBuffer msg = new StringBuffer();
      msg.append("setTranscriptNameAndCdsBounds found predicted transcript ");
      msg.append(tfs.getName() + ":" + tfs.getStart() + ".." + tfs.getEnd() + 
          "CDS: " + tfs.getTranslationStart() + ".." + tfs.getTranslationEnd());
      logger.debug(msg);
      Vector ve = tfs.getFeatures();
      for (Enumeration e = ve.elements(); e.hasMoreElements();) {
        SeqFeature sf = (SeqFeature) e.nextElement();
        logger.debug("exon: " + sf.getStart() + ".." + sf.getEnd());
      }
    }
  }

  private boolean isTypeSyntenyLink(String type) {
    return propertyScheme.isTypeSyntenyLink(type);
  }

  private void setSyntenyInfo(SeqFeatureI seqFeat,int featureId) {
    if (!isTypeSyntenyLink(seqFeat.getFeatureType()))
      return;

    // query feature relationship with featureId & join with match/paralog/synt
    // feature -> join with feature_rel -> join with syntenic feat and 
    // grab its uniquename

    String tgtId = "tgtId";
    String[] sel = new String[]{"t.uniquename as "+tgtId};
    String select = makeSelectClause(sel);
    String[] tables = new String[]{"feature_relationship fr1",
        "feature_relationship fr2","feature t"};
    String from = makeFromClause(tables);

    // get
    Long relTypeId = getRelationshipCVTermId(getSyntenyRelationshipType());
    WhereClause where = new WhereClause("fr1.object_id",featureId);
    where.add("fr1.type_id",relTypeId).add("fr1.subject_id","fr2.subject_id");
    where.add("fr2.object_id","t.feature_id");
    where.add("t.feature_id","!=",featureId);

    String query = select + from + where;

    try {
      ResultSet rs = executeLoggedSelectQuery("setSyntenyInfo", query);
      while (rs.next()) {
        String uniquename = rs.getString(tgtId);
        // this aint right! this can be a list - for now...
        seqFeat.setSyntenyLinkInfo(uniquename);
        logger.debug("got synteny link "+uniquename+" for "+featureId);
      }
    }

    catch (SQLException sqle) {
      logger.error("SQLException retrieving synteny links with feature_id = " + featureId, sqle);
    }

  }

  private String getSyntenyRelationshipType() {
    return getChadoInstance().getSyntenyRelationshipType();
  }

  /** This was prompted by tiling-path-fragment results in bdgp-4.2 db. They are
      one level results - which are different creatures than predictions (2 levels),
      and search hits (2 levels w/alignments) */
  void addOneLevelResults() {
    Connection c = getConnection();
    ChadoInstance ci = getChadoInstance();

    FeatureLocImplementation featLoc = ci.getTopFeatLoc();
    StrandedFeatureSet parentFeatSet = ci.getResultStrandedFeatSet();
    ChadoProgram[] programs = ci.getOneLevelResultPrograms();

    if (programs == null || programs.length == 0)
      return; // no one level programs to query on

    long srcFeatId = featLoc.getContainingFeatureId();
    // returns range of feature - empty string if have redundant feat locs
    String featLocWhereClause = featLoc.getContainingFeatureWhereClause("fl");

    LinkedList selCols = new LinkedList();
    selCols.add("f.feature_id");
    selCols.add("f.name");
    selCols.add("f.uniquename");
    selCols.add("fl.fmin");
    selCols.add("fl.fmax");
    selCols.add("fl.strand");
    selCols.add("af.analysis_id");

    LinkedList tables = new LinkedList();
    tables.add("feature f");
    tables.add("featureloc fl");
    tables.add("analysisfeature af");

    WhereClause where = new WhereClause("f.feature_id","fl.feature_id");
    where.add("fl.srcfeature_id",new Long(srcFeatId));
    where.add(featLocWhereClause);
    where.add("af.feature_id","f.feature_id");

    String analysisAlias = null;
    AnalysisTable at = null;

    // add analysis table and columns if we don't have the whole thing cached
    if (ci.cacheAnalysisTable()) {
      at = getAnalysisTable();
    } else {
      analysisAlias = "a";
      tables.add("analysis " + analysisAlias);
      selCols.add("a.program");
      selCols.add("a.programversion");
      selCols.add("a.sourcename");
      where.add("af.analysis_id", "a.analysis_id");
    }

    try {    
      where.add(getChadoProgramWhereClause(programs, "af", analysisAlias));
    }
    catch (AnalysisException e) {
      logger.error("unable to retrieve one-level results: " + e.getMessage());
      return;
    }

    String select = makeSelectClause(selCols);
    String from = makeFromClause(tables);
    // order by af.analysis_id?

    String query = select + from + where;

    try {

      ResultSet rs = executeLoggedSelectQuery("addOneLevelResults", c, query);
      while (rs.next()) {
        SeqFeature feat = makeResultSeqFeature(at, rs);

        // these are 1 level features - there are no "transcripts" grouping feats
        // dont need these feat sets technically but not sure if apollo will barf without
        // them - or if we need them for game and other adapters, so just shoving them in
        FeatureSet fs = _makeFeatureSet(rs,feat);
        SeqFeatureI tiersFS = getTierFeatForType(feat.getFeatureType(),//feat.getProgramName(),
            feat.getStrand(),parentFeatSet);

        tiersFS.addFeature(fs);
      }

    }
    catch (SQLException sqle) {
      logger.error("SQLException retrieving 1 level results "
          +"for sequence with feature_id = " + srcFeatId, sqle);
      sqle.printStackTrace(); 
    }

  }


  /** INNER CLASS WHERE CLAUSE for constructing sql where clauses */
  private class WhereClause {

    StringBuffer clause = new StringBuffer();

    private WhereClause(String s1, String s2) {
      add(s1,s2,false);
    }
    // would be nice to figure out quoting automatically
    private WhereClause(String s1, String s2, boolean quote) {
      add(s1,s2,false,quote);
    }
    private WhereClause(String col,InClause in) {
      add(col,in,false);
    }
    private WhereClause(String col, Long value) {
      if (value == null) {
        logger.error("yikes trying to add null long value to where clause for "+ col +"; ignoring column", new Throwable());
        return;
      }
      add(col,value,false);
    }
    private WhereClause(String col, int i) {
      add(col,i,false);
    }

    private WhereClause add(String s1,String s2) { return add(s1,s2,true); }

    private WhereClause add(String s1, String rel, String s2) {
      and(true);
      clause.append(s1).append(rel).append(s2);
      return this;
    }
    private WhereClause add(String s1, String rel, int i) {
      return add(s1,rel,i+"");
    }

    private WhereClause add(String s1, String s2, boolean prependAnd) {
      and(prependAnd);
      clause.append(s1).append(" = ").append(s2);
      return this;
    }

    private WhereClause add(String s1, String s2, boolean prependAnd, boolean quoteS2) {
      and(prependAnd);
      clause.append(s1).append(" = ").append(quote(s2));
      return this;
    }

    private WhereClause add(String s1, Long l) {
      return add(s1,l+"");
    }

    private String quote(String s) {
      return "'"+s+"'";
    }

    private WhereClause add(String col,InClause in, boolean prependAnd) {
      and(prependAnd);
      clause.append(col).append(in.getBuffer());
      return this;
    }

    private void and(boolean doIt) {
      if (!doIt) return;
      clause.append("\nAND ");
    }

    private WhereClause add(String s, int i) {
      return add(s,i+"");
    }
    private WhereClause add(String s, int i, boolean prependAnd) {
      return add(s,i+"",false);
    }
    private WhereClause add(String s, Long l, boolean prependAnd) {
      if (l == null) {
        logger.error("yikes! trying to add null to where clause: ignoring", new Throwable());
        return this;
      }
      return add(s,l.toString(),prependAnd);
    }
    private WhereClause add(String s) {
      newline();
      clause.append(s);
      return this;
    }

    private WhereClause add(WhereClause wc) {
      newline();
      clause.append(wc.getClauseWithAnd());
      return this;
    }

    private void newline() { clause.append("\n"); }

    private StringBuffer getBuffer() { return clause; }

    /** has WHERE at start */
    private String getClauseWithWhere() {
      StringBuffer clauseWithWhere = new StringBuffer("\n\nWHERE ");
      clauseWithWhere.append(clause);
      return clauseWithWhere.toString();
    }

    /** has AND at start */
    private String getClauseWithAnd() {
      StringBuffer withAnd = new StringBuffer(" AND ");
      withAnd.append(clause);
      return withAnd.toString();
    }

    /** Returns clause with "WHERE" at beginning */
    public String toString() {
      return getClauseWithWhere();
    }

  } // end of WhereClause inner class



  /** InClause inner class for sql in clauses */
  private class InClause {
    private boolean hasContent = false;
    private StringBuffer in = new StringBuffer(" IN (");

    private InClause add(String s) {
      if (s == null || s.equals(""))
        return this;
      if (hasContent)
        in.append(", ");
      in.append(s);
      hasContent = true;
      return this;
    }

    private InClause add(int i) {
      return add(i+"");
    }
    private InClause add(long l) {
      return add(l+"");
    }

    private boolean hasContent() {
      return hasContent;
    }

    private void finish() {
      in.append(")");
    }

    private StringBuffer getBuffer() { return in; }
    public String toString() { return in.toString(); }
  }

  private String makeSelectClause(String[] columns) {
    StringBuffer s = new StringBuffer("\nSELECT ");
    int i;
    for (i=0; i < columns.length - 1; i++)
      s.append("\n").append(columns[i]).append(", ");
    s.append("\n").append(columns[i]); // no comma on last
    return s.toString();
  }

  private String makeSelectClause(String column) {
    return makeSelectClause(new String[] {column});
  }

  private String makeSelectClause(Collection cols) {
    return makeSelectClause((String []) cols.toArray(new String[] {}));
  }

  private String makeFromClause(String[] tables) {
    StringBuffer s = new StringBuffer("\n\nFROM ");
    int i;
    for (i=0; i<tables.length - 1; i++)
      s.append("\n").append(tables[i]).append(", ");
    s.append("\n").append(tables[i]); // last table no comma
    return s.toString();
  }

  private String makeFromClause(String table) {
    return makeFromClause(new String[] {table});
  }

  private String makeFromClause(Collection cols) {
    return makeFromClause((String []) cols.toArray(new String[] {}));
  }

  private String makeSql(String[] select,String[] from,WhereClause where) {
    return makeSelectClause(select) + makeFromClause(from) + where;
  }

  private void addFeatureToParents(StrandedFeatureSet parentFeatSet, Map forwardMap) {
    for (Iterator it = forwardMap.keySet().iterator(); it.hasNext();) {
      String program = (String) it.next();
      FeatureSet fs = (FeatureSet) forwardMap.get(program);

      /*
      List<FeatureSet> toBeAdded = new LinkedList<FeatureSet>();
      getTopLevelFeatureSets(fs, toBeAdded);
      
      for (FeatureSet f : toBeAdded) {
        setFeatureSetIdIfNull(f);
        parentFeatSet.addFeature(f);
      }
      */

      setFeatureSetIdIfNull(fs);
      
      parentFeatSet.addFeature(fs);
    }
  }
  
  private void getTopLevelFeatureSets(FeatureSet fs, List<FeatureSet> toBeAdded)
  {
    for (Object o : fs.getFeatures()) {
      if (o instanceof FeatureSet) {
        getTopLevelFeatureSets((FeatureSet)o, toBeAdded);
      }
      else {
        toBeAdded.add(fs);
        break;
      }
    }
  }

  private void setFeatureSetIdIfNull(FeatureSet fs)
  {
    if (fs.getId() != null) {
      return;
    }
    for (Object o : fs.getFeatures()) {
      if (o instanceof FeatureSet) {
        setFeatureSetIdIfNull((FeatureSet)o);
      }
    }
    fs.setId(fs.getFeatureAt(0).getId() + "-fs");
  }
  
  /**
   * Construct a where clause that will constrain a query on analysisfeature
   * and/or analysis to exactly those rows in the analysis table that are 
   * specified by a list of ChadoProgram objects.
   * 
   * @param programs Array of ChadoProgram objects that specifies the analysis rows to include.
   * @param analysisfeatureAlias Alias of the analysisfeature table, if the query includes it.
   * @param analysisAlias Alias of the analysis table, if the query includes it.
   * @return
   * @throws AnalysisException
   */
  public String getChadoProgramWhereClause(ChadoProgram[] programs, 
      String analysisfeatureAlias, 
      String analysisAlias)
  throws AnalysisException 
  {
    ChadoInstance ci = getChadoInstance();

    // If the analysis table has been cached then construct the query with
    // an explicit list of analysis ids and an "IN" statement.  In this
    // mode EITHER analysisfeatureAlias or analysisAlias must be non-null.
    //
    // TODO: make the maximum IN clause size configurable.  The current
    // implementation imposes no limit, which means that it may generate
    // SQL statements that the DBMS cannot execute.
    //
    if (ci.cacheAnalysisTable()) {

      // Get list of analysis ids
      int np = programs.length;
      AnalysisTable at = getAnalysisTable();
      InClause inClause = new InClause();

      for(int i = 0;i < np;++i) {
        List l = at.getAnalysisRows(programs[i]);

        // TODO - make this a warning, not an exception

        if ((l == null) || (l.size() == 0)) {
          logger.warn("No rows found in the chado analysis table for " + programs[i]);
          continue;
          //throw new AnalysisException("No rows found in the chado analysis table for " + programs[i]);
        }

        for (Iterator it = l.iterator(); it.hasNext();) {
          AnalysisRow row = (AnalysisRow) it.next();
          inClause.add(row.analysis_id);
        }
      }

      inClause.finish();
      String colAlias = (analysisfeatureAlias != null) ? analysisfeatureAlias : analysisAlias;
      if (inClause.hasContent()) {
        return "AND " + colAlias + ".analysis_id" + inClause.toString();
      }
      else {
        return "";
      }
    } 

    // If the analysis table has not been cached then construct the query
    // using a disjunct (series of OR statements) with one clause for 
    // each ChadoProgram.  In this mode analysisAlias must be non-null.
    //
    // TODO: make the maximum disjunct size configurable.  In order to implement
    // both this and the maximum IN clause size restriction we'd need to do
    // some refactoring in order to be able to run the same query several 
    // times, but with different sets of analysis ids or OR conditions each time.
    //
    // TODO: it should be possible to support querying by analysis_id even
    // if the analysis table is not cached, and, conversely, to query by 
    // program/programversion name even if the analysis table is cached.
    //
    else {
      if (analysisAlias == null) {
        throw new IllegalArgumentException("getChadoProgramWhereClause requires an alias for " +
        "the analysis table when the table is not pre-cached");
      }

      int np = programs.length;
      List conjuncts = new ArrayList(np);

      for(int i = 0;i < np;++i) {
        String program = programs[i].getProgram();
        String programversion = programs[i].getProgramversion();
        String sourcename = programs[i].getSourcename();
        String conjunct = analysisAlias + ".program = '" + program + "'";
        int nc = 1;

        if (programversion != null) {
          conjunct += " AND " + analysisAlias + ".programversion = '" + programversion + "'";
          ++nc;
        }
        if (sourcename != null) {
          conjunct += " AND " + analysisAlias + ".sourcename = '" + sourcename + "'";
          ++nc;
        }
        if (nc > 1) { conjuncts.add("(" + conjunct + ")"); } else { conjuncts.add(conjunct); } 
      }      

      // TODO: modify this to use WhereClause (and clean that class up)

      // TODO: possible problem here in that you won't get an exception if there are no qualifying
      // analyses in the database, but you will if you use the pre-caching option

      int nc = conjuncts.size();
      StringBuffer disjunct = new StringBuffer();
      for(int i = 0;i < nc;++i) {
        String c = (String) conjuncts.get(i);
        if (i > 0) { disjunct.append(" OR "); }
        disjunct.append(c);
      }

      return "AND (" + disjunct.toString()  + ")";
    }
  }

  private AnalysisTable analysisTable;
  private AnalysisTable getAnalysisTable() {
    if (analysisTable==null)
      analysisTable = new AnalysisTable();
    return analysisTable;
  }

  /** 
   * Class that caches the whole analysis table.  It's _usually_ not that
   * big, although some of the TIGR chado databases have tens of thousands
   * of rows in this table.
   */
  private class AnalysisTable {

    //    private List dbPrograms;

    // Map from analysis.program to a List of AnalysisRow objects
    private Map programToAnalysisRows;

    // Map from analysis.analysis_id to a single AnalysisRow object
    private Map idToAnalysisRow;

    private AnalysisTable() {
      cacheAnalysisTable();
    }

    // Cache the contents of the entire analysis table 
    private void cacheAnalysisTable() {
      //      dbPrograms = new ArrayList();
      programToAnalysisRows = new HashMap();
      idToAnalysisRow = new HashMap();

      // Querying for program, programversion, and sourcename because the schema defines
      // a UNIQUE constraint on these three columns:
      String sql = "SELECT analysis_id, program, programversion, sourcename FROM analysis ";
      try {
        ResultSet rs = executeLoggedSelectQuery("cacheAnalysisTable", sql);
        while (rs.next()) {
          AnalysisRow ar = new AnalysisRow(rs);
          //          dbPrograms.add(cp);

          // update <code>programToAnalysisRows</code>
          List rowList = (List) programToAnalysisRows.get(ar.program);
          if (rowList == null) {
            rowList = new ArrayList();
            programToAnalysisRows.put(ar.program, rowList);
          }
          rowList.add(ar);
          // update <code>idToAnalysisRow</code>
          idToAnalysisRow.put(new Long(ar.analysis_id),ar);
        }
      } catch (SQLException e) {
        logger.error("SQLException caching analysis table. SQL: " + sql);
      }       
    }

    private AnalysisRow getAnalysisRow(long analysisId) {
      return (AnalysisRow)idToAnalysisRow.get(new Long(analysisId));
    }

    /**
     * Retrieve a list of all the analysis rows with the given program,
     * programversion, and sourcename.  In some chado instances there
     * may be a 1-1 relationship between analysis_id and program, but 
     * since the schema does not define a unique constraint on program 
     * alone, this is not guaranteed.
     *
     * @param program A non-null value from analysis.program.
     * @param programversion A value to match against analysis.programversion.  May be null.
     * @param sourcename A value to match against analysis.sourcename.  May be null.
     * @return A List of the AnalysisRows that match the specified column values.
     */
    private List getAnalysisRows(String program, String programversion, String sourcename) {
      if (program == null) throw new IllegalArgumentException("AnalysisTable.getAnalysisRows() requires non-null program name");

      List rowList = (List) programToAnalysisRows.get(program);
      int numRows = (rowList != null) ? rowList.size() : 0;
      List result = new ArrayList(numRows);

      if (numRows > 0) {
        for (Iterator it = rowList.iterator(); it.hasNext();) {
          AnalysisRow ar = (AnalysisRow) it.next();
          if ((programversion != null) && (!programversion.equals(ar.programversion))) {
            continue;
          }
          if ((sourcename != null) && (!sourcename.equals(ar.sourcename))) {
            continue;
          }
          result.add(ar);
        }
      }
      return result;
    }

    private List getAnalysisRows(ChadoProgram cp) {
      return getAnalysisRows(cp.getProgram(), cp.getProgramversion(), cp.getSourcename());
    }

  } // end AnalysisTable

  // Analysis-related exceptions
  private class AnalysisException extends Exception {
    private AnalysisException(String m) { super(m); }
  }

  /** A single row in the analysis table */
  private class AnalysisRow {
    private long analysis_id;
    private String program;
    private String programversion;
    private String sourcename;

    private AnalysisRow(ResultSet rs) {
      try {
        program = rs.getString("program");
        programversion = rs.getString("programversion");
        analysis_id = rs.getLong("analysis_id");
        sourcename = rs.getString("sourcename");
        // add more as needed
      } catch (SQLException e) {
        logger.error("SQLException retrieving analysis.\n"+e);
      }
    }
  } // end AnalysisRow

  // -----------------------------------------------------------------------
  // Update and update-related methods
  // -----------------------------------------------------------------------

  // Used to ensure that the same JDBC Connection is used for all operations that
  // need to be grouped into a single transaction.
  //

  /** 
   * Used to ensure that the same JDBC Connection is used for all write operations 
   * that are part of a single transaction.
   */
  protected Connection transactionConnection = null;

  /**
   * Retrieve the JDBC Connection used to initiate the most recent transaction.
   */
  public Connection getConnectionUsedForLastTransaction() {

    // error - no open transaction
    if (transactionConnection == null) {
      throw new RuntimeException("Unmatched call to getConnectionUsedForLastTransaction()");
    }

    // error - open transaction doesn't match return value of getConnection()
    else {
      Connection c = getConnection();
      if (c != transactionConnection) {
        // print a warning only, since transactionConnection may still be valid
        logger.warn("getConnectionUsedForLastTransaction: stored value does not match getConection()");
      }
    }

    return transactionConnection;
  }

  /**
   * Begin a transaction using the current JDBC Connection.
   * 
   * @return Whether the operation succeeded.
   */
  public boolean beginTransaction() {
    // current model supports only 1 open ("meta") transaction per adapter
    if (this.transactionConnection != null) {
      throw new RuntimeException("beginTransaction() called again before previous transaction committed or rolled back");
    }

    Connection c = getConnection();
    this.transactionConnection = c;
    try {
      c.setAutoCommit(false);
    } catch (SQLException sqle) {
      logger.error("SQLException calling Connection.setAutoCommit(false)", sqle);
      return false;
    }

    // TODO - restore autocommit to original setting when done?
    // TODO - check autocommit setting before each operation
    // TODO - use a different logger or NDC for updates versus reads?

    logger.debug("BEGIN TRAN");
    return true;
  }

  public boolean hasOpenTransaction() {
    return (transactionConnection != null);
  }

  /**
   * Commit a transaction using the current JDBC Connection.
   *
   * @return Whether the operation succeeded.
   */
  public boolean commitTransaction() {
    Connection c = getConnectionUsedForLastTransaction();
    try {
      c.commit();
      this.transactionConnection = null;
      logger.debug("COMMIT");
    } catch (SQLException sqle) {
      logger.debug("COMMIT FAILED");
      return false;
    }
    return true;
  }

  /**
   * Roll back a transaction using the current JDBC Connection.
   *
   * @return Whether the operation succeeded.
   */
  public boolean rollbackTransaction() {
    Connection c = getConnectionUsedForLastTransaction();
    try {
      c.rollback();
      this.transactionConnection = null;
      logger.debug("ROLLBACK");
    } catch (SQLException sqle) {
      logger.debug("ROLLBACK FAILED");
      return false;
    }
    return true;
  }

  /**
   * Recursive delete of a row and all the rows that reference it via a foreign 
   * key constraint.
   *
   * @param tableName chado table from which the row is to be deleted
   * @param rowId chado feature_id of the row to be deleted
   * @return Whether the delete succeeded
   */
  public boolean deleteRow(String tableName, long rowId) {
    int rowsDeleted = 0;
    logger.debug("DELETING row with id=" + rowId + " from " + tableName);

    // TODO - track/summarize deleted rows
    // TODO - distinguish between nullable and non-nullable foreign key constraints 
    //        and add option to NULL any NULLable refs instead of always deleting
    //        (currently assuming that all are NON-nullable)

    // 1. delete all rows in tables that reference tableName (no option to NULL references)
    rowsDeleted += deleteReferencingRows(tableName, rowId);

    // 2. delete row in tableName
    boolean result = deleteUnreferencedRow(tableName, rowId);

    logger.debug("DELETED row with id=" + rowId + " from " + tableName + " (" + rowsDeleted + " deleted)");
    return result;
  }

  /**
   * @return Total number of rows deleted.
   */
  protected int deleteReferencingRows(String tableName, long rowId) {
    int rowsDeleted = 0;

    // Determine which tables have a foreign key constraint to tableName
    TableColumn refTables[] = getForeignKeyConstraints(tableName);
    int rtl = (refTables == null) ? 0 : refTables.length;

    // foreach table in tables
    for (int t = 0;t < rtl;++t) {
      String refTable = refTables[t].tableName;
      String refCol = refTables[t].colName;

      // get rows that reference tableName/rowId      
      // the advantage of doing this indirectly (i.e., gathering row ids and then deleting them)
      // is that then we have a record of exactly what was deleted.  the disadvantage is that it
      // is slightly slower and less direct
      List rowIds = getReferencingRowIds(refTable, refCol, rowId);
      if (rowIds == null) {
        continue;
      }
      Iterator i = rowIds.iterator();

      while (i.hasNext()) {
        Long refRowId = (Long)(i.next());
        if (deleteRow(refTable, refRowId.longValue())) {
          ++rowsDeleted;
        }
      }
    }
    return rowsDeleted;
  }

  /**
   * TODO - this is actually a generic method; all it's doing is evaluating a single equijoin 
   *        (modulo quoting) and returning the row ids
   *
   * @param refTable
   * @param refCol
   * @param rowId
   *
   * @return List of Long row ids (primary key values)
   */
  protected List getReferencingRowIds(String refTable, String refCol, long rowId) {
    String refKeyCol = getPrimaryKeyColumn(refTable);
    if (refKeyCol == null) return null;
    Connection c = getConnectionUsedForLastTransaction();
    ArrayList ids = new ArrayList();

    try {
      String sql = "SELECT " + refKeyCol + " FROM " + refTable + " WHERE " + refCol + " = " + rowId;
      ResultSet rs = executeLoggedSelectQuery("getReferencingRowIds", c, sql);
      while (rs.next()) {
        ids.add(new Long(rs.getLong(refKeyCol)));
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getReferencingRowIds() ResultSet", sqle);
      return null;
    }

    logger.debug("FOUND " + ids.size() + " ROWS WHERE " + refTable + "." + refCol + " = " + rowId);
    return ids;
  }

  /**
   * Delete a row known not to be referenced by any other (e.g., because deleteReferencingRows has
   * already been called on the row)
   *
   * @param tableName chado table from which the row is to be deleted
   * @param rowId chado feature_id of the row to be deleted
   *
   * @return Whether the delete succeeded.
   */
  protected boolean deleteUnreferencedRow(String tableName, long rowId) {
    int rowcount = 0;
    String primaryKeyCol = getPrimaryKeyColumn(tableName);
    if (primaryKeyCol == null) return false;
    Connection c = getConnectionUsedForLastTransaction();
    logger.debug("DELETE ROW " + tableName + " " + rowId);

    // NOTE - assumes that primary key column values never need quoting (this should be true in chado)
    String sql = "DELETE FROM " + tableName + " WHERE " + primaryKeyCol + " = " + rowId;

    try {
      Statement stmt = c.createStatement();
      rowcount = stmt.executeUpdate(sql);
    } catch (SQLException sqle) {
      logger.error("DELETE ROW " + tableName + " " + rowId + " FAILED", sqle);
    }

    return (rowcount == 1);
  }

  protected boolean integersTheSame(Integer i1, Integer i2) {
    if ((i1 != null) && (i2 != null)) {
      return i1.intValue() == i2.intValue();
    } else {
      return ((i1 == null) && (i2 == null));
    }
  }

  protected boolean longsTheSame(Long l1, Long l2) {
    if ((l1 != null) && (l2 != null)) {
      return l1.longValue() == l2.longValue();
    } else {
      return ((l1 == null) && (l2 == null));
    }
  }

  protected boolean stringsTheSame(String s1, String s2) {
    if ((s1 != null) && (s2 != null)) {
      return s1.equals(s2);
    } else {
      return ((s1 == null) && (s2 == null));
    }
  }

  public boolean updateFeatureRow(long featureId, ChadoFeature dbFeat, Long dbxrefId, long organismId, String name, String uniquename, 
      String residues, long typeId, boolean isAnalysis, boolean isObsolete) 
  {
    String tableName = "feature";
    HashMap colValues = new HashMap();    // quoted column values
    HashMap unquotedCols = new HashMap(); // unquoted/hard-coded column values

    // check specified values against dbLoc to see what has changed
    int numColsChanged = 0;

    // automatically update timelastmodified if anything else is changed
    colValues.put("timelastmodified", new Timestamp(new Date().getTime()));

    // dbxrefId
    Long dbDbXrefId = dbFeat.getDbXrefId();
    if (!longsTheSame(dbDbXrefId, dbxrefId)) {
      if (dbxrefId == null) { unquotedCols.put("dbxref_id", "NULL"); } else { colValues.put("dbxref_id", dbxrefId); }
      ++numColsChanged;
    }

    // organismId
    long dbOrganismId = dbFeat.getOrganismId();
    if (dbOrganismId != organismId) {
      colValues.put("organism_id", new Long(organismId));
      ++numColsChanged;
    }

    // name
    String dbName = dbFeat.getName();
    if (!stringsTheSame(dbName, name)) {
      if (name == null) { unquotedCols.put("name", "NULL"); } else { colValues.put("name", name); }
      ++numColsChanged;
    }

    // uniquename
    String dbUniquename = dbFeat.getUniquename();
    if (!stringsTheSame(dbUniquename, uniquename)) {
      if (uniquename == null) { unquotedCols.put("uniquename", "NULL"); } else { colValues.put("uniquename", uniquename); }
      ++numColsChanged;
    }

    // residues
    String dbResidues = dbFeat.getResidues();
    // TODO - check dbFeat.getResiduesRetrieved()
    if (!stringsTheSame(dbResidues, residues)) {
      if (residues == null) { 
        unquotedCols.put("residues", "NULL"); 
        unquotedCols.put("md5checksum", "NULL"); 
        colValues.put("seqlen", new Long(0));
      } else { 
        colValues.put("residues", residues);
        colValues.put("md5checksum", computeMD5Checksum(residues)); 
        colValues.put("seqlen", new Long(residues.length()));
      }
      ++numColsChanged;
    }

    // typeId
    long dbTypeId = dbFeat.getTypeId();
    if (dbTypeId != typeId) {
      colValues.put("type_id", new Long(typeId));
      ++numColsChanged;
    }

    // isAnalysis
    boolean dbIsAnalysis = dbFeat.getIsAnalysis();
    if (dbIsAnalysis != isAnalysis) {
      colValues.put("is_analysis", getBooleanValue(isAnalysis));
      ++numColsChanged;
    }

    // isObsolete
    boolean dbIsObsolete = dbFeat.getIsObsolete();
    if (dbIsObsolete != isObsolete) {
      colValues.put("is_obsolete", getBooleanValue(isObsolete));
      ++numColsChanged;
    }

    // TODO - currently not allowing updates to timeaccessioned column

    // don't do the update if nothing has changed
    if (numColsChanged == 0) {
      logger.debug("not updating feature with feature_id=" + featureId + "; no changes found");
      return true;
    }

    return updateRow(tableName, featureId, colValues, unquotedCols); 
  } 

  /**
   * @param dbFeatprop ChadoFeatureProp giving the *current* database contents
   */
  public boolean updateFeaturepropRow(long featurepropId, ChadoFeatureProp dbFeatprop, long featureId,
      long typeId, String value, int rank) 
  {
    String tableName = "featureprop";
    HashMap colValues = new HashMap();    // quoted column values
    HashMap unquotedCols = new HashMap(); // unquoted/hard-coded column values

    // DEBUG
    logger.debug("updating featureprop_id " + featurepropId + " to " + value + " from " + dbFeatprop.getValue());

    // check specified values against dbLoc to see what has changed
    int numColsChanged = 0;

    // feature_id
    long dbFeatId = dbFeatprop.getFeatureId();
    if (!longsTheSame(new Long(featureId) , new Long(dbFeatId))) {
      colValues.put("feature_id", new Long(featureId));
      ++numColsChanged;
    }					 

    // type_id
    long dbTypeId = dbFeatprop.getTypeId();
    if (!longsTheSame(new Long(typeId) , new Long(dbTypeId))) {
      colValues.put("type_id", new Long(typeId));
      ++numColsChanged;
    }

    // value
    String dbValue = dbFeatprop.getValue();
    if (!value.equals(dbValue)) {
      colValues.put("value", value);
      ++numColsChanged;
    }    

    // rank
    int dbRank = dbFeatprop.getRank();
    if (dbRank != rank) {
      colValues.put("rank", new Integer(rank));
    }    

    // don't do the update if nothing has changed
    if (numColsChanged == 0) {
      logger.debug("not updating featureprop with featureprop_id=" + featurepropId + "; no changes found");
      return true;
    }

    return updateRow(tableName, featurepropId, colValues, unquotedCols);

  }					 

  /**
   * @param dbLoc ChadoFeatureLoc giving the *current* database contents
   */
  public boolean updateFeaturelocRow(long featurelocId, ChadoFeatureLoc dbLoc, long featureId, Long srcFeatureId, 
      Integer fmin, Integer fmax, boolean fmin_partial, boolean fmax_partial, Integer strand, 
      Integer phase, String residueInfo, int locgroup, int rank) 
  {
    String tableName = "featureloc";
    HashMap colValues = new HashMap();    // quoted column values
    HashMap unquotedCols = new HashMap(); // unquoted/hard-coded column values

    // DEBUG
    logger.debug("updating featureloc_id " + featurelocId + " to " + fmin + " - " + fmax + " from " + dbLoc.getFmin() + " - " + dbLoc.getFmax());

    // check specified values against dbLoc to see what has changed
    int numColsChanged = 0;

    // feature_id
    long dbFeatId = dbLoc.getFeatureId();
    if (featureId != dbFeatId) {
      colValues.put("feature_id", new Long(featureId));
      ++numColsChanged;
    }
    // srcfeature_id
    ChadoFeature sf = dbLoc.getSourceFeature();
    Long dbSrcFeatId = (sf == null) ? null : new Long(sf.getFeatureId());
    if (!longsTheSame(dbSrcFeatId,srcFeatureId)) {
      if (srcFeatureId == null) { unquotedCols.put("srcfeature_id", "NULL"); } else { colValues.put("srcfeature_id", srcFeatureId); }
      ++numColsChanged;
    }
    // fmin
    Integer dbFmin = dbLoc.getFmin();
    if (!integersTheSame(dbFmin, fmin)) {
      if (fmin == null) { unquotedCols.put("fmin", fmin); } else { colValues.put("fmin", fmin); }
      ++numColsChanged;
    }
    // fmax
    Integer dbFmax = dbLoc.getFmax();
    if (!integersTheSame(dbFmax, fmax)) {
      if (fmax == null) { unquotedCols.put("fmax", fmax); } else { colValues.put("fmax", fmax); }
      ++numColsChanged;
    }
    // is_fmin_partial
    boolean dbFminPartial = dbLoc.getFminPartial();
    if (dbFminPartial != fmin_partial) {
      unquotedCols.put("is_fmin_partial", getBooleanValue(fmin_partial));
      ++numColsChanged;
    }
    // is_fmax_partial
    boolean dbFmaxPartial = dbLoc.getFmaxPartial();
    if (dbFmaxPartial != fmax_partial) {
      unquotedCols.put("is_fmax_partial", getBooleanValue(fmax_partial));
      ++numColsChanged;
    }
    // strand
    Integer dbStrand = dbLoc.getStrand();
    if (!integersTheSame(dbStrand, strand)) {
      if (strand == null) { unquotedCols.put("strand", strand); } else { colValues.put("strand", strand); }
      ++numColsChanged;
    }
    // phase
    Integer dbPhase = dbLoc.getPhase();
    if (!integersTheSame(dbPhase, phase)) {
      if (phase == null) { unquotedCols.put("phase", phase); } else { colValues.put("phase", phase); }
      ++numColsChanged;
    } 
    // residue_info
    String dbResInfo = dbLoc.getResidueInfo();
    if (!stringsTheSame(dbResInfo, residueInfo)) {
      if (residueInfo == null) { unquotedCols.put("residue_info", residueInfo); } else { colValues.put("residue_info", residueInfo); }
      ++numColsChanged;
    }
    // locgroup
    int dbLocgroup = dbLoc.getLocgroup();
    if (dbLocgroup != locgroup) {
      colValues.put("locgroup", new Integer(locgroup));
    }
    // rank
    int dbRank = dbLoc.getRank();
    if (dbRank != rank) {
      colValues.put("rank", new Integer(rank));
    }

    // don't do the update if nothing has changed
    if (numColsChanged == 0) {
      logger.debug("not updating featureloc with featureloc_id=" + featurelocId + "; no changes found");
      return true;
    }

    return updateRow(tableName, featurelocId, colValues, unquotedCols);
  }

  /**
   * Returns the name of the (single) primary key column for a chado table.
   */
  protected String getPrimaryKeyColumn(String tableName) {
    HashMap map = getPrimaryKeyColumnMapping();
    Object obj = map.get(tableName);
    return (obj == null) ? null : (String)obj;
  }

  /**
   * Returns an array of the chado columns that reference a given table.
   */
  protected TableColumn[] getForeignKeyConstraints(String tableName) {
    HashMap map = getForeignKeyReferenceMapping();
    Object obj = map.get(tableName);
    return (obj == null) ? null : (TableColumn[])obj;
  }

  // TODO - the following schema information may depend on the schema version and should be 
  // factored out or possibly queried directly from the database

  // primary key column mapping
  protected static HashMap pkcMap; 

  protected static HashMap getPrimaryKeyColumnMapping() {
    if (pkcMap == null) {
      pkcMap = new HashMap();
      pkcMap.put("analysisfeature", "analysisfeature_id");
      pkcMap.put("phylonode", "phylonode_id");
      pkcMap.put("feature", "feature_id");
      pkcMap.put("featureloc", "featureloc_id");
      pkcMap.put("feature_relationship", "feature_relationship_id");
      pkcMap.put("featureprop", "featureprop_id");
      pkcMap.put("feature_pub", "feature_pub_id");
      pkcMap.put("feature_cvterm", "feature_cvterm_id");
      pkcMap.put("feature_dbxref", "feature_dbxref_id");
      pkcMap.put("feature_synonym", "feature_synonym_id");
      pkcMap.put("synonym", "synonym_id");
      pkcMap.put("feature_relationship", "feature_relationship_id");
      pkcMap.put("featureprop_pub", "featureprop_pub_id");      
      pkcMap.put("pub", "pub_id");
      pkcMap.put("dbxref", "dbxref_id");
      pkcMap.put("db", "db_id");
      
      // TODO - add remaining tables
    }
    return pkcMap;
  }

  // foreign key constraints
  protected static HashMap fkcMap;

  protected static HashMap getForeignKeyReferenceMapping() {
    if (fkcMap == null) {
      fkcMap = new HashMap();
      fkcMap.put("feature", new TableColumn[] { 
          new TableColumn("analysisfeature", "feature_id"),
          // EL - not part of Sequence module for Chado and probably no one
          // that uses Apollo uses that module, since there was a bug in the
          // mapping and no one complained about it (it mapped to phylonode.type_id)
          // new TableColumn("phylonode", "feature_id"),
          new TableColumn("featureloc", "feature_id"),
          new TableColumn("featureloc", "srcfeature_id"),
          new TableColumn("feature_pub", "feature_id"),
          new TableColumn("featureprop", "feature_id"),
          new TableColumn("feature_cvterm", "feature_id"),
          new TableColumn("feature_dbxref", "feature_id"),
          new TableColumn("feature_synonym", "feature_id"),
          new TableColumn("feature_relationship", "subject_id"),
          new TableColumn("feature_relationship", "object_id"),
      } );

      // TODO - add remaining foreign key constraints (preferably automatically!)
    } 
    return fkcMap;
  }

  // Inserts into various chado tables.  Note that these methods aren't designed
  // with efficiency in mind because Apollo generally won't have to update or 
  // insert many rows in the database.

  /**
   * Run a SELECT(MAX()) + 1 query to get the next available primary key value
   * for a chado table.  Note that this is the preferred method to obtain new
   * ids in Sybase (since the primary key columns are not defined as Sybase
   * IDENTITY types, at least in the current TIGR implementation).
   */
  public long getNextPrimaryKeyId(String tableName) {
    String colName = getPrimaryKeyColumn(tableName);
    String sql = "SELECT MAX(" + colName + ") + 1 FROM " + tableName;
    Connection c = getConnectionUsedForLastTransaction();
    long newId = -1;

    try {
      ResultSet rs = executeLoggedSelectQuery("getNextPrimaryKeyId", c, sql);

      if (rs.next()) {
        newId = rs.getLong(1);

        // handle case where table is empty initially (!)
        if (newId == 0) { newId = 1; }
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving row from getNextPrimaryKeyId()", sqle);
    }

    logger.debug(sql + " returned " + newId);
    return newId;
  }

  protected long addPrimaryKeyColumnValue(String tableName, HashMap columnValues) {
    long newId = getNextPrimaryKeyId(tableName);
    columnValues.put(getPrimaryKeyColumn(tableName), new Long(newId));
    return newId;
  }

  /**
   * Generic method to insert a single row into a named table.  The insert will
   * fail if the caller does not provide a value for each non-nullable column
   * in the table.
   *
   * @param tableName         Name of the chado table into which to insert.
   * @param colValues         Map from column name to column value.  May be null.
   * @param unquotedColValues Map from column name to column value.  May be null.
   * @return Whether the insert succeeded.
   */ 
  public boolean insertRow(String tableName, Map colValues, Map unquotedColValues) {

    // Copy the column names and values into an array, since the iteration 
    // order of Maps is not guaranteed to be predictable.  Basically we're
    // doing some extra work in the method itself to make it more convenient
    // to pass in the parameters.
    int ncv = (colValues == null) ? 0 : colValues.size();
    int nucv = (unquotedColValues == null) ? 0 : unquotedColValues.size();
    int nc = ncv + nucv;
    String colNames[] = new String[nc];
    Object colVals[] = new Object[nc];
    int cnum = 0;

    if (colValues != null) {
      Iterator cvi = colValues.keySet().iterator();
      while (cvi.hasNext()) {
        String key = (String)cvi.next();
        colNames[cnum] = key;
        Object val = colValues.get(key);
        colVals[cnum] = val;
        ++cnum;
      }
    }

    if (unquotedColValues != null) {
      Iterator ucvi = unquotedColValues.keySet().iterator();
      while (ucvi.hasNext()) {
        String key = (String)ucvi.next();
        colNames[cnum] = key;
        Object val = unquotedColValues.get(key);
        colVals[cnum] = val;
        ++cnum;
      }
    }

    StringBuffer sql = new StringBuffer();
    sql.append("INSERT INTO " + tableName + " (");

    // list of column names (must include all non-nullable columns)
    for (cnum = 0;cnum < nc;++cnum) {
      if (cnum > 0) { sql.append(", "); }
      // TODO - quote column name for safety?
      sql.append(colNames[cnum]);
    }

    sql.append(") VALUES (");

    // list of column values 
    // (using placeholders for anything not in <code>unquotedColValues</code>)
    for (cnum = 0;cnum < nc;++cnum) {
      if (cnum > 0) { sql.append(", "); }

      // unquoted value - insert value directly into SQL string
      if ((unquotedColValues != null) && (unquotedColValues.containsKey(colNames[cnum]))) {
        sql.append(colVals[cnum]);
      } 
      // quoted value - use placeholder
      else {
        sql.append("?");
      }
    }
    sql.append(")");

    // prepare the statement
    Connection c = getConnectionUsedForLastTransaction();
    if (logger.isDebugEnabled()) {
      StringBuffer msg = new StringBuffer();
      msg.append("INSERT ROW into " + tableName + " (");
      for (cnum = 0;cnum < nc;++cnum) {
        if (cnum > 0) msg.append(",");
        msg.append(colNames[cnum] + "=" + colVals[cnum]);
      }
      msg.append(")");
      logger.debug(msg);
    }

    // number of rows inserted
    int rowsInserted = executeLoggedUpdate("insertRow", c, sql.toString(), colNames, colVals, unquotedColValues);
    return (rowsInserted == 1);
  }

  /**
   * Generic method to update a single row.  Only updates the supplied column values.
   *
   * @param tableName         Name of the chado table to update.
   * @param colValues         Map from column name to column value.  May be null.
   * @param unquotedColValues Map from column name to column value.  May be null.
   * @return Whether the update succeeded.
   */ 
  public boolean updateRow(String tableName, long id, Map colValues, Map unquotedColValues) {
    String pkeyCol = getPrimaryKeyColumn(tableName);

    // Copy the column names and values into an array, since the iteration 
    // order of Maps is not guaranteed to be predictable.  Basically we're
    // doing some extra work in the method itself to make it more convenient
    // to pass in the parameters.
    int ncv = (colValues == null) ? 0 : colValues.size();
    int nucv = (unquotedColValues == null) ? 0 : unquotedColValues.size();
    int nc = ncv + nucv;
    String colNames[] = new String[nc];
    Object colVals[] = new Object[nc];
    int cnum = 0;

    if (colValues != null) {
      Iterator cvi = colValues.keySet().iterator();
      while (cvi.hasNext()) {
        String key = (String)cvi.next();
        colNames[cnum] = key;
        Object val = colValues.get(key);
        colVals[cnum] = val;
        ++cnum;
      }
    }

    if (unquotedColValues != null) {
      Iterator ucvi = unquotedColValues.keySet().iterator();
      while (ucvi.hasNext()) {
        String key = (String)ucvi.next();
        colNames[cnum] = key;
        Object val = unquotedColValues.get(key);
        colVals[cnum] = val;
        ++cnum;
      }
    }

    StringBuffer sql = new StringBuffer();
    sql.append("UPDATE " + tableName + " SET ");

    // list of column names (must include all non-nullable columns)
    for (cnum = 0;cnum < nc;++cnum) {
      if (cnum > 0) { sql.append(", "); }
      // TODO - quote column name for safety?
      sql.append(colNames[cnum] + " = ");

      // unquoted value - insert value directly into SQL string
      if ((unquotedColValues != null) && (unquotedColValues.containsKey(colNames[cnum]))) {
        sql.append(colVals[cnum]);
      } 
      // quoted value - use placeholder
      else {
        sql.append("?");
      }
    }

    sql.append(" WHERE " + pkeyCol + " = " + id);

    // prepare the statement
    Connection c = getConnectionUsedForLastTransaction();
    // number of rows updated
    int rowsUpdated = executeLoggedUpdate("updateRow", c, sql.toString(), colNames, colVals, unquotedColValues);
    return (rowsUpdated == 1);
  }

  public String computeMD5Checksum(String seq) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(seq.getBytes());
      byte[] md5digest = md5.digest();
      // convert to 32 character string

      StringBuffer checksum = new StringBuffer();
      for (int i=0; i < md5digest.length; i++) {
        // converts to hexadecimal
        checksum.append(Integer.toString((md5digest[i] & 0xff) + 0x100, 16).substring(1));
      }
      if (checksum.length() != 32) {
        logger.error("computed MD5 checksum string of length " + checksum.length() + " for " + seq);
        return null;
      }
      return checksum.toString();
    }
    // MD5 not supported
    catch (java.security.NoSuchAlgorithmException nsa) {
      logger.error("unable to computed MD5 checksum; not supported by Java security");
    }
    return null;
  }

  // TODO - check maximum length of String value for residues column; should be using a bigger datatype

  public Long insertFeatureRow(Long dbxrefId, long organismId, String name, String uniquename, 
      String residues, Long seqlen, Long typeId, Integer isAnalysis, Date createDate) {
    String tableName = "feature";
    HashMap colValues = new HashMap();

    // add feature_id
    long newFeatureId = addPrimaryKeyColumnValue(tableName, colValues);
    
    logger.trace("inserting feature with feature_id=" + newFeatureId + ", uniquename=" + uniquename);

    // check that seqlen and residues agree if both are specified
    // calculate md5 checksum
    if (dbxrefId != null) { colValues.put("dbxref_id", dbxrefId); }
    colValues.put("organism_id", new Long(organismId));
    if (name != null) { colValues.put("name", name); }
    colValues.put("uniquename", uniquename);

    if (residues != null) {
      colValues.put("residues", residues);

      // check that seqlen and residues agree
      long residuesLen = residues.length();
      if (seqlen == null) {
        seqlen = new Long(residuesLen);
      } 
      else if (seqlen.longValue() != residuesLen) {
        throw new IllegalArgumentException("seqlen (" + seqlen + ") != residues.length (" + residuesLen + ")");
      }

      // calculate MD5 checksum
      colValues.put("md5checksum", computeMD5Checksum(residues));
    }

    if (seqlen != null) { colValues.put("seqlen", seqlen); }
    colValues.put("type_id", typeId);
    colValues.put("is_analysis", getBooleanValue(isAnalysis.intValue() != 0));
    colValues.put("timeaccessioned", new Timestamp(createDate.getTime()));
    colValues.put("timelastmodified", new Timestamp(createDate.getTime()));
    // new rows cannnot be obsolete
    colValues.put("is_obsolete", getBooleanValue(false));

    // no unquoted values here
    HashMap unquotedCols = null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeatureId);
    } else {
      return null;
    }
  }

  public Long insertFeatureRelationshipRow(Long subjectId, Long objectId, Long typeId, String value, Long rank)
  {
    String tableName = "feature_relationship";
    HashMap colValues = new HashMap();

    // add feature_relationship_id
    long newFeatureRelId = addPrimaryKeyColumnValue(tableName, colValues);

    colValues.put("subject_id", subjectId);
    colValues.put("object_id", objectId);
    colValues.put("type_id", typeId);
    if (value != null) { colValues.put("value", value); }
    if (rank != null) { colValues.put("rank", rank); }

    // no unquoted values here
    HashMap unquotedCols = null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeatureRelId);
    } else {
      return null;
    }
  }

  public boolean deleteFeatureRelationshipRow(Long subjectId, Long objectId, Long typeId)
  {
    String tableName = "feature_relationship";
    Long featRelId = getFeatureRelationshipId(subjectId, objectId, typeId);
    if (featRelId == null) {
      logger.error("unable to retrieve feature_relationship_id for relationship with subject_id=" + subjectId + " object_id=" + objectId + " type_id=" + typeId);
      return false;
    }
    return deleteRow(tableName, featRelId.longValue());
  }

  public Long insertFeaturelocRow(Long featureId, Long srcFeatureId, Integer fmin, Integer fmax, 
      boolean fmin_partial, boolean fmax_partial, Integer strand, 
      Integer phase, String residueInfo, int locgroup, long rank)
  {
    String tableName = "featureloc";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add featureloc_id
    long newFeaturelocId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("feature_id", featureId);
    colValues.put("locgroup", new Integer(locgroup));
    colValues.put("rank", new Long(rank));

    // NULLable columns
    if (srcFeatureId != null) { colValues.put("srcfeature_id", srcFeatureId); }
    if (fmin != null) { colValues.put("fmin", fmin); }
    if (fmax != null) { colValues.put("fmax", fmax); }
    if (strand != null) { colValues.put("strand", strand); }
    if (phase != null) { colValues.put("phase", phase); }
    if (residueInfo != null) { colValues.put("residue_info", residueInfo); }

    // Unquoted/hard-coded columns
    HashMap unquotedCols = new HashMap();
    unquotedCols.put("is_fmin_partial", getBooleanValue(fmin_partial));
    unquotedCols.put("is_fmax_partial", getBooleanValue(fmax_partial));

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeaturelocId);
    } else {
      return null;
    }
  }

  public Long insertFeaturepropRow(Long featureId, Long typeId, String value, long rank, boolean checkNextRank) 
  {
    if(checkNextRank) {
      Connection c = getConnectionUsedForLastTransaction();

      try {
        String sql = "SELECT MAX(rank)+1 AS next_rank FROM featureprop WHERE feature_id ="+featureId+" AND type_id="+typeId;
        ResultSet rs = executeLoggedSelectQuery("getFeaturepropRankId", c, sql);
        if(rs.next()) 
          rank = rs.getLong("next_rank");
      } catch (SQLException sqle) {
        logger.error("SQLException getting row id from get Rank ResultSet", sqle);
        return null;
      }
    }
    return insertFeaturepropRow(featureId, typeId, value, rank);
  }   

  public Long insertFeaturepropRow(Long featureId, Long typeId, String value, long rank)
  {
    String tableName = "featureprop";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add featureprop_id
    long newFeaturepropId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("feature_id", featureId);
    colValues.put("type_id", typeId);
    colValues.put("rank", new Long(rank));

    // NULLable columns
    if (value != null) {  colValues.put("value", value); }

    // Unquoted/hard-coded columns
    HashMap unquotedCols =null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeaturepropId);
    } else {
      return null;
    }
  }

  public boolean deleteFeaturepropRow(Long featurePropId)
  { 
    String tableName = "featureprop";
    if (featurePropId == null) {
      logger.error("unable to delete featureprop row with a featurePropId = "+featurePropId);
      return false;
    }
    return deleteRow(tableName, featurePropId.longValue());
  } 

  public Long insertPubRowIfNeeded(String pubUniquename,long pubTypeId) {

    Long pubId = getPubId(pubUniquename);
    if(pubId != null)
      return pubId;

    String tableName = "pub";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add synonym_id
    long newPubId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("uniquename", pubUniquename);
    colValues.put("type_id", new Long(pubTypeId));

    // Unquoted/hard-coded columns
    HashMap unquotedCols =null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newPubId);
    } else {
      return null;
    }    
  }

  private Long getPubId(String uniquename) {
    Connection c = getConnectionUsedForLastTransaction();

    try {
      String sql = "SELECT pub_id FROM pub WHERE uniquename ='"+uniquename+"'";
      ResultSet rs = executeLoggedSelectQuery("getSynonymId", c, sql);
      if(rs.next()) 
        return new Long(rs.getLong("pub_id"));
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getPubId() ResultSet", sqle);
    }
    return null;
  }    

  public Long insertFeaturepropPubRow(Long featPropId, Long pubId) {

    String tableName = "featureprop_pub";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add feature_synonym_id
    long newFeaturepropPubId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("featureprop_id", featPropId);
    colValues.put("pub_id", pubId);

    // Unquoted/hard-coded columns
    HashMap unquotedCols =null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeaturepropPubId);
    } else {
      return null;
    } 
  }   

  public Long insertSynonymRowIfNeeded(String name,Long typeId,String synonym_sgml) {
    Long synonymId = getSynonymId(name,typeId);
    if(synonymId != null)
      return synonymId;

    String tableName = "synonym";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add synonym_id
    long newSynonymId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("name", name);
    colValues.put("type_id", typeId);
    colValues.put("synonym_sgml", synonym_sgml);

    // Unquoted/hard-coded columns
    HashMap unquotedCols =null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newSynonymId);
    } else {
      return null;
    }  
  }

  public boolean deleteSynonymRowIfNoLongerNeeded(Long synonymId)
  {
    String refTableName = "feature_synonym";
    String refTableCol = "synonym_id";
    String tableName = "synonym";
    
    String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s=%d",
        refTableName, refTableCol, synonymId);
    ResultSet rs = executeLoggedSelectQuery("deleteSynonymRowIfNoLongerNeeded",
        getConnectionUsedForLastTransaction(), sql);
    try {
      if (rs.next() && rs.getInt(1) == 0) {
        return deleteRow(tableName, synonymId);
      }
    }
    catch (SQLException e) {
      logger.error("Error fetching query results: " + e.getMessage());
      return false;
    }
    return true;
  }
  
  // allow to get synonym_id with the name and the type_id

  public Long getSynonymId(String name,Long typeId) {
    Connection c = getConnectionUsedForLastTransaction();

    try {
      String sql = "SELECT synonym_id FROM synonym WHERE name ='"+name+ "' AND type_id="+typeId;
      ResultSet rs = executeLoggedSelectQuery("getSynonymId", c, sql);
      if(rs.next()) 
        return new Long(rs.getLong("synonym_id"));
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getSynonymId() ResultSet", sqle);
    }
    return null;
  }

  public Long insertFeatureSynonymRow(Long synId, Long featId,Integer pubId,boolean is_current ,boolean is_internal) {
    String tableName = "feature_synonym";
    HashMap colValues = new HashMap();

    // TODO - add some error checking

    // add feature_synonym_id
    long newFeatureSynonymId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("synonym_id", synId);
    colValues.put("feature_id", featId);
    colValues.put("pub_id", pubId);
    colValues.put("is_current", new Boolean(is_current));
    colValues.put("is_internal", new Boolean(is_internal));

    // Unquoted/hard-coded columns
    HashMap unquotedCols =null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newFeatureSynonymId);
    } else {
      return null;
    } 
  }   

  public boolean deleteFeatureSynonymRow(Long featureId, Long synonymId, Integer pubId)
  {
    String tableName = "feature_synonym";
    Long featSynId = getFeatureSynonymId(featureId, synonymId, pubId);
    if (featSynId == null) {
      logger.error("unable to retrieve feature_synonym_id for feature_id="+featureId+" and the synonym_id="+synonymId);
      return false;
    }
    return deleteRow(tableName, featSynId.longValue());
  }

  private Long getFeatureSynonymId(Long featureId, Long synonymId, Integer pubId) {
    Connection c = getConnectionUsedForLastTransaction();
    Long id = null;

    try {
      String sql = "SELECT feature_synonym_id " +
      "FROM feature_synonym " + 
      "WHERE feature_id = " + featureId + " " + 
      "AND synonym_id = " + synonymId + " " + 
      "AND pub_id = " + pubId;


      ResultSet rs = executeLoggedSelectQuery("getFeatureSynonymId", c, sql);

      if (rs.next()) {
        id = new Long(rs.getLong("feature_synonym_id"));
        if (rs.next()) {
          logger.error("multiple rows returned by getFeatureRelationshipId query: " + sql);
          return null;
        }
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getFeatureSynonymId() ResultSet", sqle);
      return null;
    }
    return id;
  }

  public Long getDbId(String name) {
    Connection c = getConnectionUsedForLastTransaction();

    try {
      String sql = "SELECT db_id FROM db WHERE name ='" + name + "'";
      ResultSet rs = executeLoggedSelectQuery("getDbId", c, sql);
      if(rs.next()) 
        return new Long(rs.getLong("db_id"));
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getDbId() ResultSet", sqle);
    }
    return null;
  }

  public Long insertDbRowIfNeeded(String name) {

    // see whether the row already exists
    Long dbId = getDbId(name);

    // it doesn't, so insert it
    if (dbId == null) {
      String tableName = "db";
      HashMap colValues = new HashMap();
      long newDbId = addPrimaryKeyColumnValue(tableName, colValues);

      // non-NULLable columns
      colValues.put("name", name);

      // Unquoted/hard-coded columns
      HashMap unquotedCols = null;

      if (!insertRow(tableName, colValues, unquotedCols)) return null;
      dbId = new Long(newDbId);
    }
    return dbId;
  }

  public Long getDbXrefId(long dbId, String accession, String version) {
    Connection c = getConnectionUsedForLastTransaction();

    try {
      String sql = "SELECT dbx.dbxref_id " +
      "FROM dbxref dbx " + 
      "WHERE dbx.db_id = " + dbId + " " +
      "AND dbx.accession = '" + accession + "' " +
      "AND dbx.version = '" + version + "'";
      ResultSet rs = executeLoggedSelectQuery("getDbXrefId", c, sql);
      if(rs.next()) 
        return new Long(rs.getLong("dbxref_id"));
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getDbXrefId() ResultSet", sqle);
    }
    return null;
  }

  public Long insertDbXrefRowIfNeeded(long dbId, String accession, String version, String description) {
    // see whether the row already exists
    Long dbxrefId = getDbXrefId(dbId, accession, version);

    // it doesn't, so insert it
    if (dbxrefId == null) {
      String tableName = "dbxref";
      HashMap colValues = new HashMap();
      long newDbXrefId = addPrimaryKeyColumnValue(tableName, colValues);

      // non-NULLable columns
      colValues.put("db_id", new Long(dbId));
      colValues.put("accession", accession);
      colValues.put("version", version);
      // nullable columns
      if (description != null) colValues.put("description", description);

      // Unquoted/hard-coded columns
      HashMap unquotedCols = null;

      if (!insertRow(tableName, colValues, unquotedCols)) return null;
      dbxrefId = new Long(newDbXrefId);
    }

    return dbxrefId;
  }

  public Long getFeatureDbXrefId(long featureId, long dbXrefId) {
    Connection c = getConnectionUsedForLastTransaction();

    try {
      String sql = "SELECT feature_dbxref_id " +
      "FROM feature_dbxref " + 
      "WHERE feature_id = " + featureId + " " +
      "AND dbxref_id = " + dbXrefId;
      ResultSet rs = executeLoggedSelectQuery("getFeatureDbXrefId", c, sql);
      if(rs.next()) 
        return new Long(rs.getLong("feature_dbxref_id"));
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getFeatureDbXrefId() ResultSet", sqle);
    }
    return null;
  }

  /**
   * @param dbFeatDbXref ChadoFeatureDbXref giving the *current* database contents
   * @param featureId    New feature_id
   * @param dbXrefId     New dbxref_id
   * @param isCurrent    New value for is_current
   * @return Returns true if the update succeeded or was not necessary.
   */
  public boolean updateFeatureDbXrefRow(ChadoFeatureDbXref dbFeatDbXref, long featureId, 
      long dbXrefId, boolean isCurrent)
  {
    String tableName = "feature_dbxref";
    HashMap colValues = new HashMap();    // quoted column values
    HashMap unquotedCols = new HashMap(); // unquoted/hard-coded column values

    // check specified values against dbFeatDbXref to see what has changed
    int numColsChanged = 0;

    // feature_id
    long dbFeatId = dbFeatDbXref.getFeatureId();
    if (!longsTheSame(new Long(featureId) , new Long(dbFeatId))) {
      colValues.put("feature_id", new Long(featureId));
      ++numColsChanged;
    }					 

    // dbxref_id
    long dbDbXrefId = dbFeatDbXref.getDbXrefId();
    if (!longsTheSame(new Long(dbDbXrefId), new Long(dbXrefId))) {
      colValues.put("dbxref_id", new Long(dbXrefId));
      ++numColsChanged;
    }

    // is_current
    boolean dbIsCurrent = dbFeatDbXref.getIsCurrent();
    if (dbIsCurrent != isCurrent) {
      colValues.put("is_current", new Boolean(isCurrent));
      ++numColsChanged;
    }    

    long featureDbXrefId = dbFeatDbXref.getFeatureDbXrefId();

    // don't do the update if nothing has changed
    if (numColsChanged == 0) {
      logger.debug("not updating feature_dbxref with featuredbxref_id=" + featureDbXrefId + "; no changes found");
      return true;
    }

    return updateRow(tableName, featureDbXrefId, colValues, unquotedCols);
  }					 

  public Long insertFeatureDbXrefRow(long featureId, long dbXrefId, boolean isCurrent) {
    String tableName = "feature_dbxref";
    HashMap colValues = new HashMap();
    long newDbXrefId = addPrimaryKeyColumnValue(tableName, colValues);

    // non-NULLable columns
    colValues.put("feature_id", new Long(featureId));
    colValues.put("dbxref_id", new Long(dbXrefId));
    colValues.put("is_current", new Boolean(isCurrent));

    // Unquoted/hard-coded columns
    HashMap unquotedCols = null;

    if (insertRow(tableName, colValues, unquotedCols)) {
      return new Long(newDbXrefId);
    } 
    return null;
  }

  public boolean deleteFeatureDbXrefRow(long featureId, long dbXrefId)
  { 
    String tableName = "feature_dbxref";
    Long featureDbXrefId = getFeatureDbXrefId(featureId, dbXrefId);
    if (featureDbXrefId == null) {
      logger.error("unable to retrieve feature_dbxref_id for feature_id=" + featureId + " and the dbXrefId_id=" + dbXrefId);
      return false;
    }
    return deleteRow(tableName, featureDbXrefId.longValue());
  } 
    
  /**
   * @param featureId chado feature_id of the original feature
   * @param isSubject whether <code>featureId</code> is the subject in the feature_relationship
   * @param relTypeName relationship cvterm.name
   * @return A List of (Long) feature_ids of features related to <code>featureId</code>
   */
  public List getRelatedFeatureIds(Long featureId, boolean isSubject, String relTypeName, String relFeatType) {
    String refCol = isSubject ? "subject_id" : "object_id";
    String tgtCol = isSubject ? "object_id" : "subject_id";
    Long relTypeId = getRelationshipCVTermId(relTypeName);
    Long relFeatTypeId = getFeatureCVTermId(relFeatType);
    Connection c = getConnectionUsedForLastTransaction();
    ArrayList ids = new ArrayList();

    try {
      String sql = "SELECT fr." + tgtCol + " " +
      "FROM feature_relationship fr, feature f " +
      "WHERE fr." + refCol + " = " + featureId + " " +
      "AND fr.type_id = " + relTypeId + " " +
      "AND fr." + tgtCol + " = f.feature_id " +
      "AND f.type_id = " + relFeatTypeId;

      ResultSet rs = executeLoggedSelectQuery("getRelatedFeatureIds", c, sql);

      while (rs.next()) {
        ids.add(new Long(rs.getLong(tgtCol)));
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getRelatedFeatureIds() ResultSet", sqle);
      return null;
    }

    return ids;
  }

  public Long getRelatedFeatureRank(Long subjectId, Long objectId, String relTypeName) {
    Long relTypeId = getRelationshipCVTermId(relTypeName);
    Connection c = getConnectionUsedForLastTransaction();
    Long rank = null;

    try {
      // should return at most 1 row due to unique constraint o these columns:
      String sql = "SELECT rank " +
      "FROM feature_relationship " +
      "WHERE subject_id = " + subjectId + " " +
      "AND object_id = " + objectId + " " + 
      "AND type_id = " + relTypeId;

      ResultSet rs = executeLoggedSelectQuery("getRelatedFeatureRank", c, sql);

      if (rs.next()) {
        rank = new Long(rs.getLong("rank"));

        if (rs.next()) {
          logger.error("multiple rows returned by getRelatedFeatureRank query");
          logger.error(rank);
          do {
            logger.error(rs.getLong("rank"));
          } while (rs.next());          

          return null;
        }
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getRelatedFeatureRank() ResultSet", sqle);
      return null;
    }

    return rank;
  }

  // TODO - factor out code that's in common to getRelatedFeatureIds
  public boolean updateFeatureRelationshipRanks(Long featureId, boolean isSubject, String relTypeName, String relFeatType, long newRank, boolean increment) {
    String refCol = isSubject ? "subject_id" : "object_id";
    String tgtCol = isSubject ? "object_id" : "subject_id";
    Long relTypeId = getRelationshipCVTermId(relTypeName);
    Long relFeatTypeId = getFeatureCVTermId(relFeatType);
    String pkeyCol = getPrimaryKeyColumn("feature_relationship");
    Connection c = getConnectionUsedForLastTransaction();

    ArrayList pkeys = new ArrayList();
    ArrayList newvals = new ArrayList();

    try {
      String sql = "SELECT fr." + pkeyCol + ", " + 
      (increment ? "fr.rank+1" : "fr.rank-1" ) + " as newval " +
      "FROM feature_relationship fr, feature f " +
      "WHERE fr." + refCol + " = " + featureId + " " +
      "AND fr.type_id = " + relTypeId + " " +
      "AND fr." + tgtCol + " = f.feature_id " +
      "AND f.type_id = " + relFeatTypeId +
      "AND fr.rank " + (increment ? " >= " : " > ") + newRank ;

      ResultSet rs = executeLoggedSelectQuery("updateFeatureRelationshipRanks", c, sql);

      while (rs.next()) {
        pkeys.add(new Long(rs.getLong(pkeyCol)));
        newvals.add(new Long(rs.getLong("newval")));
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from updateFeatureRelationshipRanks() ResultSet", sqle);
      return false;
    }

    // do updates
    int ni = pkeys.size();
    logger.debug("updateFeatureRelationshipRanks updating rank for " + ni + " row(s)");

    for (int i = 0;i < ni;++i) {
      Long pkey = (Long)(pkeys.get(i));
      Long newval = (Long)(newvals.get(i));
      logger.debug("updateFeatureRelationshipRanks updating rank for feature_relationship_id=" + pkey + ", new value=" + newval);
      String upd = "update feature_relationship set rank = " + newval + " where feature_relationship_id = " + pkey;
      int nrows = executeLoggedUpdate("updateFeatureRelationshipRanks", c, upd, null, null, null);
      if (nrows != 1) {
        logger.error("updated failed: " + upd);
        return false;
      }
    }
    return true;
  }

  public Long getFeatureRelationshipId(Long subjectId, Long objectId, Long typeId) {
    Connection c = getConnectionUsedForLastTransaction();
    Long id = null;

    try {
      String sql = "SELECT feature_relationship_id " +
      "FROM feature_relationship " + 
      "WHERE subject_id = " + subjectId + " " + 
      "AND object_id = " + objectId + " " + 
      "AND type_id = " + typeId;


      ResultSet rs = executeLoggedSelectQuery("getFeatureRelationshipId", c, sql);

      if (rs.next()) {
        id = new Long(rs.getLong("feature_relationship_id"));
        if (rs.next()) {
          logger.error("multiple rows returned by getFeatureRelationshipId query: " + sql);
          return null;
        }
      }
    } catch (SQLException sqle) {
      logger.error("SQLException getting row id from getFeatureRelationshipId() ResultSet", sqle);
      return null;
    }

    return id;
  }

  // Increment the feature_relationship.rank of any exon whose rank is >= (greater than or equal to) newExonRank
  public boolean incrementTranscriptExonRanks(Long transFeatureId, long newExonRank) {
    boolean transIsSubject = false;
    String relTypeName = getChadoInstance().getExonTransRelationTerm();
    return updateFeatureRelationshipRanks(transFeatureId, transIsSubject, relTypeName, "exon", newExonRank, true);
  }

  // Decrement the feature_relationship.rank of any exon whose rank is > (strictly greater than) oldExonRank
  public boolean decrementTranscriptExonRanks(Long transFeatureId, long newExonRank) {
    boolean transIsSubject = false;
    String relTypeName = getChadoInstance().getExonTransRelationTerm();
    return updateFeatureRelationshipRanks(transFeatureId, transIsSubject, relTypeName, "exon", newExonRank, false);
  }

  // Increment the feature_relationship.rank of any transcript whose rank is >= (greater than or equal to) newTranscriptRank
  public boolean incrementGeneTranscriptRanks(Long geneFeatureId, long newTranscriptRank) {
    boolean geneIsSubject = false;
    String relTypeName = getChadoInstance().getTransGeneRelationTerm();
    String transcriptTerm = getChadoInstance().getTranscriptTerm();
    return updateFeatureRelationshipRanks(geneFeatureId, geneIsSubject, relTypeName, transcriptTerm, newTranscriptRank, true);
  }

  // Decrement the feature_relationship.rank of any transcript whose rank is > (strictly greater than) oldTranscriptRank
  public boolean decrementGeneTranscriptRanks(Long geneFeatureId, long newTranscriptRank) {
    boolean geneIsSubject = false;
    String relTypeName = getChadoInstance().getTransGeneRelationTerm();
    String transcriptTerm = getChadoInstance().getTranscriptTerm();
    return updateFeatureRelationshipRanks(geneFeatureId, geneIsSubject, relTypeName, transcriptTerm, newTranscriptRank, false);
  } 

  public List getGeneTranscriptIds(Long geneFeatureId) {
    boolean geneIsSubject = false;
    String relTypeName = getChadoInstance().getTransGeneRelationTerm();
    String transcriptTerm = getChadoInstance().getTranscriptTerm();
    return getRelatedFeatureIds(geneFeatureId, geneIsSubject, relTypeName, transcriptTerm);
  }

  public List getTranscriptCdsIds(Long transFeatureId) {
    boolean transIsSubject = false;
    String relTypeName = getChadoInstance().getCdsTransRelationTerm();
    return getRelatedFeatureIds(transFeatureId, transIsSubject, relTypeName, "CDS");
  }

  public List getTranscriptExonIds(Long transFeatureId) {
    boolean transIsSubject = false;
    String relTypeName = getChadoInstance().getExonTransRelationTerm();
    return getRelatedFeatureIds(transFeatureId, transIsSubject, relTypeName, "exon");
  }

  public List getCdsPolypeptideIds(Long cdsFeatureId) {
    boolean cdsIsSubject = false;
    String relTypeName = getChadoInstance().getPolypeptideCdsRelationTerm();
    return getRelatedFeatureIds(cdsFeatureId, cdsIsSubject, relTypeName, "polypeptide");
  }

  public List getTranscriptPolypeptideIds(Long transFeatureId) {
    boolean transIsSubject = false;
    String relTypeName = getChadoInstance().getTransProtRelationTerm();
    return getRelatedFeatureIds(transFeatureId, transIsSubject, relTypeName, "polypeptide"); 
  }

  public Long getExonRank(Long exonFeatureId, Long transFeatureId) {
    String relTypeName = getChadoInstance().getExonTransRelationTerm();
    return getRelatedFeatureRank(exonFeatureId, transFeatureId, relTypeName);
  }

  public Long getTranscriptRank(Long transFeatureId, Long geneFeatureId) {
    String relTypeName = getChadoInstance().getTransGeneRelationTerm();
    return getRelatedFeatureRank(transFeatureId, geneFeatureId, relTypeName);
  }

  /** Check to see if this is a shared exon (more than one transcript has it
   * 
   * @param exonId - feature.feature_id of the exon
   * @return true if this is a shared exon
   */
  public boolean isSharedExon(long exonId) {
    String relTypeName = getChadoInstance().getExonTransRelationTerm();
    Long exonTransRelId = getRelationshipCVTermId(relTypeName);
    String sql = "SELECT count(*) " +
    "FROM feature_relationship " +
    "WHERE subject_id = " + exonId + " AND type_id = " + exonTransRelId;
    boolean ok = false;
    try {
      ResultSet rs = executeLoggedSelectQuery("isSharedExon", getConnectionUsedForLastTransaction(), sql);
      if (rs.next()) {
        return rs.getInt(1) > 1 ? true : false;
      }
    }
    catch (SQLException e) {
      logger.error("Error retrieving exon feature_relationship information for " +
          "exon with feature_id = " + exonId);
    }
    return false;
  }

  // -----------------------------------------------------------------------
  // Wrappers for ChadoInstance methods
  // -----------------------------------------------------------------------

  /**
   * @param adapter  ChadoAdapter on whose behalf the database accesses are to be performed.
   * @param seqType  cvterm type_id of the sequence identified by <code>seqId</code>
   * @param seqId    Chado feature_id of the sequence to be displayed/annotated in Apollo.
   */
  // TO DO - put the implementation of getCurationSet here, but make it configurable on a per-database basis
  public CurationSet getCurationSet(ChadoAdapter adapter,String seqType,String seqId) {
    return getChadoInstance().getCurationSet(adapter,seqType,seqId);
  }

  public CurationSet getCurationSetInRange(ChadoAdapter adapter, String seqType,
      Region region) {
    return getChadoInstance().getCurationSetInRange(adapter, seqType, region);
  }

  // this just deletes things from transaction manager - who calls this?
  public void commitChanges(CurationSet curationSet) {
    //getChadoInstance().commitChanges(curationSet);
    // getChadoWriteInstance().commitChanges(curationSet);
    // for now just calling flybase object direct - fill in interface later
    //ChadoWrite chadoWrite = getChadoWriteInstance(); // db handle?
    FlybaseWrite chadoWrite = new FlybaseWrite(getConnection());
    chadoWrite.writeCurationSet(curationSet);
  }


  /** Tigr and fb assign feature type differently - config! */
  protected String getFeatureType(String alignType,String program,
      String programversion,String targetSp,
      String sourcename,String featProp) {
    return getChadoInstance().getFeatureType(alignType,program,programversion,targetSp,
        sourcename,featProp);
  }

  // Wrapper for ChadoInstance.getAnalysisType that retrieves the requisite 
  // column values from either the cached AnalysisTable or the ResultSet
  // row, depending on ChadoInstance.cacheAnalysisTable()
  //
  private String getAnalysisType(ResultSet rs) throws SQLException {
    ChadoInstance ci = getChadoInstance();
    long analysisId = rs.getLong("analysis_id");

    String program;
    String programversion;
    String sourcename;

    if (ci.cacheAnalysisTable()) {
      AnalysisTable at = getAnalysisTable();
      AnalysisRow ar = at.getAnalysisRow(analysisId);
      program = ar.program;
      programversion = ar.programversion;
      sourcename = ar.sourcename;
    } else {
      program = rs.getString("program");
      programversion = rs.getString("programversion");
      sourcename = rs.getString("sourcename");
    }
    return ci.getAnalysisType(analysisId, program, programversion, sourcename);
  }

  // -----------------------------------------------------------------------
  // Miscellaneous DBMS-specific properties
  // -----------------------------------------------------------------------

  /** 
   * The analysisfeature table has a column named 'identity' in the standard Chado release.
   * In the TIGR Sybase port of Chado this column had to be renamed, since 'identity' is
   * a reserved word in Sybase.  The Sybase adapter overrides this method to return the 
   * appropriate column name.
   */
  protected String getAnalysisFeatureIdentityField() {
    return "identity";
  }

  // Not sure if this is the best place for the following methods.  The issue
  // here is that chado might define a column as having type "Boolean" (e.g., 
  // feature.is_analysis).  That type is best represented by a BIT type in Sybase
  // whereas in Postgres/FlyBase it seems to have been translated to a character
  // type, with 't' and 'f' used to represent true and false, respectively.
  // This information could be stored in the SchemaVersion or the config. file
  // but since it's so closely tied to the choice of DBMS this seems like an
  // appropriate place for it.

  /**
   * @return The Java object that represents the "true" or "false" value for chado columns of type boolean.
   */
  protected abstract Object getBooleanValue(boolean val);

  /**
   * @return The name of a function that can be used to compute the length (in characters)
   * of a CLOB (Character Large OBject) or TEXT value.
   */
  protected abstract String getClobLengthFunction();

  // -----------------------------------------------------------------------
  // Methods that manipulate the Apollo data model
  // -----------------------------------------------------------------------
  // separate util class?

  /**
   * Add a set of apollo.datamodel.Transcripts to an instance of apollo.datamodel.Gene.
   *
   * @param gene        Gene to which the transcripts should be added.
   * @param transcripts A Vector of apollo.datamodel.Transcript objects.  Cleared after the transcripts are added.
   * @return            The number of transcripts added.
   */
  protected int _addTranscriptsToGene(AnnotatedFeature gene, Vector transcripts) {
    int nt = transcripts.size();
    logger.debug("adding " + nt + " transcript(s) to gene " + gene);

    for (int i = 0;i < nt;++i) {
      Transcript t = (Transcript)(transcripts.elementAt(i));
      gene.addFeature(t);
    }
    transcripts.clear();
    return nt;
  }

  /**
   * Add a set of apollo.datamodel.Exons to an instance of apollo.datamodel.Transcript.
   *
   * @param t             Transcript to which the exons should be added.
   * @param exons         A Vector of apollo.datamodel.Exon objects.  Cleared after the exons are added.
   * @return              The number of exons added.
   */
  protected void _addExonsToTranscript(Transcript t, Vector exons) {
    int ne = exons.size();

    // Exons *must* be sorted in order for Apollo to display the gene models correctly.
    if (t.getStrand() == -1) {
      for (int i = ne-1;i >= 0;--i) {
        Exon e = (Exon)(exons.elementAt(i));
        t.addExon(e);
      }
    } else {
      for (int i = 0;i < ne;++i) {
        Exon e = (Exon)(exons.elementAt(i));
        t.addExon(e);
      }
    }
    exons.clear();
  }

  /**
   * Add a set of apollo.datamodel.SeqFeatureIs to an instance of apollo.datamodel.FeatureSet
   *
   * @param t             FeatureSet to which the subfeatures should be added.
   * @param exons         A Vector of apollo.datamodel.SeqFeatureI objects.  Cleared after the features are added.
   */
  protected void _addFeaturesToFeatureSet(FeatureSet fs, Vector feats) {
    int nf = feats.size();

    // Subfeatures *must* be sorted in order for Apollo to calculate the cDNA and protein
    // translation correctly (if applicable)
    if (fs.getStrand() == -1) {
      for (int i = nf-1;i >= 0;--i) {
        SeqFeatureI sf = (SeqFeatureI)(feats.elementAt(i));
        fs.addFeature(sf);
      }
    } else {
      for (int i = 0;i < nf;++i) {
        SeqFeatureI sf = (SeqFeatureI)(feats.elementAt(i));
        fs.addFeature(sf);
      }
    }
    feats.clear();
  }

  /**
   * Create a new instance of apollo.datamodel.Transcript based on an existing template.
   *
   * @param t    A template Transcript on which to base the new object.
   * @param cds  ChadoCds that specifies the coding region of the new transcript.
   * @return     A new Transcript whose coding region is described by <code>cds</code>.
   */
  protected Transcript _makeNewTranscript(Transcript t, ChadoCds cds) {
    Transcript nt = new Transcript();
    nt.setRefSequence(t.getRefSequence());

    // NOTE - currently we have multiple cds features but not multiple transcript features so we're
    // using the cds name instead of the transcript name for the alternatively-spliced transcripts
    nt.setId(cds.getName());
    nt.setName(cds.getName());
    nt.setFeatureType("transcript");
    nt.setStrand(cds.getStrand());

    // HACK - we currently have no information on alternatively-spliced exons, so each transcript
    // gets the same set of exons.
    Vector exons = t.getExons();
    int ne = (exons == null) ? 0 : exons.size();
    Vector newExons = new Vector(ne);

    for (int i =0;i < ne;++i) {
      Exon e = (Exon)(exons.elementAt(i));
      Exon newExon = new Exon(e);
      newExon.setRefSequence(e.getRefSequence());
      newExon.setId(e.getId());
      newExon.setName(e.getName());
      newExons.addElement(newExon);
    }

    _addExonsToTranscript(nt, newExons);
    return nt;
  }

  /**
   * Helper method for addSearchHits; 
   *
   * @param features
   * @param name
   * @param type
   * @param strand
   * @return
   */
  protected FeatureSet _makeFeatureSet(Vector features, String name, String type, int strand) {
    FeatureSet fs = _makeFeatureSet(type,strand);
    if (name == null) {
      logger.error("JDBC Attempting to make FeatSet w null name, using NO_NAME");
      name = RangeI.NO_NAME;
    }
    fs.setName(name);
    int nf = features.size();
    for (int i = 0;i < nf;++i) {
      SeqFeature sf = (SeqFeature)(features.elementAt(i));
      fs.addFeature(sf);
    }
    return fs;
  }
  private FeatureSet _makeFeatureSet(String type, int strand) {
    FeatureSet fs = new FeatureSet();
    //fs.setName(name);
    fs.setFeatureType(type);
    fs.setStrand(strand);
    return fs;
  }

  /** _makeFeatureSet with FeatureList (strongly typed feature list) - delete? or
      use this instead of _makeFeatureSet with vector */
  private FeatureSet _makeFeatureSet(FeatureList kids, String name, String type,
      int strand) {
    //return new FeatureSet(kids,name,type,strand);
    FeatureSet fs = new FeatureSet(kids,name,type,strand);
    
    setFeatureSetIdIfNull(fs);
    
    return fs;
  }

  private FeatureSet _makeFeatureSet(ResultSet rs, SeqFeature kid) throws SQLException {
    String type = getAnalysisType(rs);
    FeatureSet fs = new FeatureSet(type,rs.getInt("strand"));
    fs.addFeature(kid);
    
    fs.setId(kid.getId() + "-fs");
    
    return fs;
  }

  /**
   * An alternative to _makeFeatureSet.
   *
   * @param fs
   * @param features
   * @return 
   */
  protected void _addFeatures(FeatureSet fs, Vector features) {
    int nf = features.size();

    for (int i = 0;i < nf;++i) {
      SeqFeature sf = (SeqFeature)(features.elementAt(i));
      fs.addFeature(sf);
    }
  }

  /**
   * Factory method for SeqFeatures.
   *
   * @param low
   * @param high
   * @param type
   * @param strand
   * @return
   */
  protected SeqFeature _makeSeqFeature(int low, int high, String type, int strand) {
    low = adjustLowForInterbaseToBaseOrientedConversion(low);
    return new SeqFeature(low, high, type, strand);
  }

  protected SeqFeature _makeSeqFeature(Integer low, Integer high, String type, Integer strand) {
    return _makeSeqFeature(low.intValue(), high.intValue(), type, strand.intValue());
  }

  private SeqFeature _makeSeqFeature(ResultSet rs) throws SQLException {
    int fmin = rs.getInt("fmin");
    int fmax = rs.getInt("fmax");
    //String type = rs.getString("type_id");
    //int type_id = rs.getInt("type_id");  String type = getFeatCvName(type_id);
    String type = getAnalysisType(rs);
    int strand = rs.getInt("strand");
    return _makeSeqFeature(fmin,fmax,type,strand);
  }

  private SeqFeature makeResultSeqFeature(AnalysisTable at, ResultSet rs) throws SQLException {
    SeqFeature sf = _makeSeqFeature(rs);

    long analysisId = rs.getLong("analysis_id"); // should test if there?
    String name = rs.getString("name");
    String uniquename = rs.getString("uniquename");

    sf.setName(name != null ? name : uniquename);
    sf.setId(uniquename != null ? uniquename : name);

    String program = null;

    // Use analysis column(s) in ResultSet if not caching analysis table
    if (at == null) {
      program = rs.getString("program");
    }
    else {
      AnalysisRow ar = at.getAnalysisRow(analysisId);
      program = ar.program;
    }

    sf.setProgramName(program);

    // TODO: retrieve analysis scores?

    return sf;
  }

  /**
   * Factory method for Exons.
   *
   * @param low
   * @param high
   * @param strand
   * @return
   */
  protected Exon _makeExon(int low, int high, int strand) {
    Exon e = new Exon();
    setFeatCoordsFromInterbase(e,low,high,strand);
    return e;
  }

  private void setFeatCoordsFromInterbase(RangeI rge, int fmin, int fmax, int strand){
    int low = adjustLowForInterbaseToBaseOrientedConversion(fmin);
    rge.setStrand(strand);
    rge.setLow(low);
    rge.setHigh(fmax);
  }

  /** a rather verbose method for the adding of one. i wanted to make it clear what this
      method was really for. chado coordinates are interbase, apollo is base-oriented.
      To convert from interbase to base-oriented you need to add one to the low
      coord. This in theory should be true for all chado to apollo conversions, but
      we can always parameterize/config it if theres chados that are not interbase. */
  static int adjustLowForInterbaseToBaseOrientedConversion(int chadoFmin) {
    return chadoFmin + 1;
  }
  static int adjustLowForBaseOrientedToInterbaseConversion(int apolloLow) {
    return apolloLow - 1;
  }

  ChadoFeature getChadoFeatureByLocation(SeqFeatureI feat, ChadoFeature srcFeature)
  {
    ChadoFeature cfeat = null;
    Connection c = getConnection();
    int fmin = adjustLowForBaseOrientedToInterbaseConversion(feat.getLow());
    int fmax = feat.getHigh();
    int strand = feat.getStrand();
    String type = "'" + feat.getFeatureType() + "'";
    String sql = "SELECT f.feature_id " + 
      "FROM featureloc fl INNER JOIN feature f on (fl.feature_id = f.feature_id) " +
      "INNER JOIN cvterm c ON (f.type_id = c.cvterm_id) " +
      "WHERE fmin = " + fmin + " AND " +
      "fmax = " + fmax + " AND strand = " + strand + " AND " +
      "srcfeature_id = " + srcFeature.getFeatureId() + " AND " + 
      "c.name = " + type + " AND is_analysis = " + getBooleanValue(false);
    try {
      Statement stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        cfeat = new ChadoFeature(rs.getLong(1), c, false);
      }
    }
    catch (SQLException e) {
      logger.error("Error fetching feature by location");
    }
    return cfeat;
  }

}
