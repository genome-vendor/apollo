package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.apache.log4j.*;

import org.bdgp.util.ProgressEvent;

import apollo.config.Config;
import apollo.dataadapter.chado.ChadoAdapter;
import apollo.datamodel.CurationSet;

/**
 * A subclass of JdbcChadoAdapter specific to Postgres installations of Chado.
 * This should just be a has-a not an is-a with JdbcChadoAdapter 
 */
public class PostgresChadoAdapter extends JdbcChadoAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PostgresChadoAdapter.class);

  public PostgresChadoAdapter() {
    try {
      Driver pgDriver = (Driver)Class.forName("org.postgresql.Driver").newInstance();
      DriverManager.registerDriver(pgDriver);
    } catch (Exception e) {
      logger.error("Exception initializing Postgres JDBC driver", e);
    }
  }
  
  /** Postgres hack to force indexing - 7.3 can be lame - similar to sybase adapters
      setForcePlan*/
  private void setSeqscan(Connection conn, boolean on) {
    String sql = "SET ENABLE_SEQSCAN TO "+ (on ? "ON" : "OFF");
    logger.debug("turning off seqscan in postgres sql = "+sql+"\n");
    try {
      conn.createStatement().executeUpdate(sql);
    } catch  (SQLException sqle) {
      logger.error("SQLException trying to set Postgres ENABLE_SEQSCAN", sqle);
    }
  }

  // dont think we need this with indiana server
//   public CurationSet getCurationSet(ChadoAdapter adapter,String seqType,String seqId) {
//     // this is sort of cheesy - should just have setDdbOptimization() call or something
//     // like that
//     if (seqType.equals("gene")) 
//       setSeqscan(getConnection(),false);
//     return super.getCurationSet(adapter,seqType,seqId);
//   }
  
  // -----------------------------------------------------------------------
  // JdbcChadoAdapter
  // -----------------------------------------------------------------------

  // Note that calling toString() on the Boolean values returned by this method will produce
  // String literals that can be inserted directly into a PostgreSQL query, since PostgreSQL
  // (at least recent versions) seems happy with any of the following variants:
  //
  // select count(*) from table where booleancol = 't';
  // select count(*) from table where booleancol = 'true';
  // select count(*) from table where booleancol = true;
  //
  protected Object getBooleanValue(boolean val) {
    return val ? Boolean.TRUE : Boolean.FALSE;
  }

  protected String getClobLengthFunction() {
    return "length";
  }
  
  public long getNextPrimaryKeyId(String tableName) {
  
    String sql = "SELECT nextval('"+tableName+"_"+tableName+"_id_seq')";
 
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
}
