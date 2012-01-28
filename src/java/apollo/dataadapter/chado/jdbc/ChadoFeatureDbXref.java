package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.*;

/**
 * Class that stores a single Chado feature_dbxref row.
 *
 * @version $Revision: 1.1 $ $Date: 2007/04/20 11:00:45 $ $Author: jcrabtree $
 */
class ChadoFeatureDbXref {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoFeatureDbXref.class);

  // --------------------------------------------
  // Instance variables
  // --------------------------------------------

  // object types are the ones that are nullable
  protected long featureDbXrefId;
  protected long featureId;
  protected long dbxrefId;
  protected boolean isCurrent;

  // --------------------------------------------
  // Constructors
  // --------------------------------------------

  /**
   * Construct a ChadoFeatureDbXref by id.
   *
   * @param featureDbXrefId Chado feature_dbxref.feature_dbxref_id
   * @param conn JDBC Connection to the chado database to query.
   */
  ChadoFeatureDbXref(long featureDbXrefId, Connection conn) {
    this.featureDbXrefId = featureDbXrefId;
    queryFeatureDbXref(conn, sql(), featureDbXrefId);
  }

  // --------------------------------------------
  // ChadoFeatureDbXref - public methods
  // --------------------------------------------

  /**
   * chado feature_dbxref_id
   */
  public long getFeatureDbXrefId() { return this.featureDbXrefId; } 

  /**
   * @return chado feature_id
   */
  public long getFeatureId() { return this.featureId; }

  /**
   * @return chado dbxref_id
   */     
  public long getDbXrefId() { return this.dbxrefId; }

  /**
   * @return chado is_current 
   */
  public boolean getIsCurrent() { return this.isCurrent; }
  
  // --------------------------------------------
  // ChadoFeatureDbXref - protected methods
  // --------------------------------------------

  private String sql() {
    return "SELECT feature_id, dbxref_id, is_current " +
      "FROM feature_dbxref WHERE feature_dbxref_id = ? ";
  }
  
  /**
   * Query the database for the featureprop info. for <code>this.featureId</code>
   */
  protected void queryFeatureDbXref(Connection conn, String sql, long featureDbXrefId) {
    logger.debug(sql);

    try {
       PreparedStatement s = conn.prepareStatement(sql);
       s.setLong(1, featureDbXrefId);
      ResultSet rs = s.executeQuery();
      // should be only one row
      
      if (rs.next()) {
        featureId = rs.getLong(1);
        dbxrefId = rs.getLong(2);
	isCurrent = rs.getBoolean(3);
	logger.trace("retrieved feature_id=" + featureId + " dbxref_id=" + dbxrefId + " is_current=" + isCurrent);
      } else {
	logger.error("no rows returned by queryFeatureDbXref query");
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving feature_dbxref with feature_dbxref_id = " + featureDbXrefId, sqle);
    }
  }
}
