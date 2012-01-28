package apollo.dataadapter.chado.jdbc;

import apollo.dataadapter.chado.ChadoAdapter;

import apollo.config.Config;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.Score;
import apollo.datamodel.StrandedFeatureSet;

import org.apache.log4j.*;

import org.bdgp.util.ProgressEvent;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Properties;

import java.util.Vector;

/**
 * A subclass of JdbcChadoAdapter specific to Sybase installations of Chado.
 * (And, furthermore, probably specific to the TIGR Chado instances.)
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.34 $ $Date: 2007/02/07 21:14:14 $ $Author: jcrabtree $
 */
public class SybaseChadoAdapter extends JdbcChadoAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SybaseChadoAdapter.class);

  protected final static Integer TRUEVAL = new Integer(1);
  protected final static Integer FALSEVAL = new Integer(0);

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  public SybaseChadoAdapter() {
    // The following code should work with the 5.x version of the Sybase JDBC driver (JConnect):
    try {
      Driver sybDriver = (Driver)Class.forName("com.sybase.jdbc2.jdbc.SybDriver").newInstance();
      DriverManager.registerDriver(sybDriver);
    } catch (Throwable t) {
      logger.error("error initializing Sybase JDBC driver JConnect", t);
    }
  }

  // -----------------------------------------------------------------------
  // SybaseChadoAdapter
  // -----------------------------------------------------------------------

  /**
   * Run 'set forceplan on' or 'set forceplan off'; this is a Sybase option that forces
   * the Sybase query optimizer to access the tables in the order that they are named
   * in the FROM clause of the SQL query.  It is the equivalent of the Oracle optimizer
   * hint "ORDERED".
   *
   * @param stmt  JDBC Statement used to execute the set forceplan command.
   * @param on    Whether to set this Sybase option on (true) or off (false).
   * @return      The JDBC result value from the executeUpdate command.
   */
  protected boolean setForcePlan(Statement stmt, boolean on) {
    String sql = "set forceplan " + (on ? "on" : "off");
    int resultval = -1;

    try {
      resultval = stmt.executeUpdate(sql);
    } catch (SQLException sqle) {
      return false;
    }
	
    return true;
  }

  // -----------------------------------------------------------------------
  // JdbcChadoAdapter
  // -----------------------------------------------------------------------

  // Overrides JdbcChadoAdapter.getNewConnection()
  protected Connection getNewConnection() throws SQLException {
    Connection conn = null;
    Properties props = new Properties();

    // Set some Sybase-specific properties
    props.setProperty("applicationname", "Apollo");
    props.setProperty("user", this.username);
    props.setProperty("password", this.password);

    logger.debug("SybaseChadoAdapter connecting to '" + jdbcUrl + "'");

    try {
      // The database name should appear at the end of jdbcUrl in the config. file:
      conn = DriverManager.getConnection(this.jdbcUrl, props);

      if (logger.isInfoEnabled()) {
          DatabaseMetaData dmd = conn.getMetaData();
          // don't care too much if these fail
          try { logger.debug("DatabaseProductName = " + dmd.getDatabaseProductName()); } catch (Throwable t) {}
          try { logger.debug("DatabaseProductVersion = " + dmd.getDatabaseProductVersion()); } catch (Throwable t) {}
          try { logger.debug("DatabaseMajorVersion = " + dmd.getDatabaseMajorVersion()); } catch (Throwable t) {}
          try { logger.debug("DatabaseMinorVersion = " + dmd.getDatabaseMinorVersion()); } catch (Throwable t) {}
      }
    } catch (SQLException sqle) {
      logger.error("SQLException connecting to Sybase database", sqle);
      
      // Print any chained exceptions too
      SQLException next = sqle;
      while ((next = next.getNextException()) != null) {
        logger.error("chained Exception = " + next, next);
      }
    }

    return conn;
  }

  /**
   * analysisfeature.identity was renamed to pidentity in the TIGR/Sybase port of Chado, since
   * identity is a reserved word in Sybase.
   */
  protected String getAnalysisFeatureIdentityField() {
    return "pidentity";
  }

  protected Object getBooleanValue(boolean val) {
    // boolean columns are represented as BIT types in Sybase; hence 0 and 1 are the allowed values
    return val ? TRUEVAL : FALSEVAL;
  }

  protected String getClobLengthFunction() {
    // JC: Note that we need to use char_length, not datalength, because the latter function
    // may return the wrong value for multibyte character encoded strings.
    return "char_length";
  }

}
