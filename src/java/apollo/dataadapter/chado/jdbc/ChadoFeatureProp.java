package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import org.apache.log4j.*;

/**
 * Class that stores a single Chado feature property (corresponding to a row
 * in the featureprop table) 
 *
 * @version $Revision: 1.5 $ $Date: 2007/03/16 14:25:32 $ $Author: olivierarnaiz $
 */
class ChadoFeatureProp {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoFeatureProp.class);

  // --------------------------------------------
  // Instance variables
  // --------------------------------------------

  // object types are the ones that are nullable
  protected long featurepropId;
  protected long featureId;
  protected long type_id;
  protected String value;
  protected int rank;

  // feature which have the property
  protected ChadoFeature feature;

  // --------------------------------------------
  // Constructors
  // --------------------------------------------


  /**
   * Construct a ChadoFeatureprop by querying the database for a specific feature_id.
   *
   * @param featureId Chado feature.feature_id
   * @param conn JDBC Connection to the chado database to query.
   */
  ChadoFeatureProp(Long featureId, Long type_id, Integer rank, Connection conn) {
    this.featureId = featureId.longValue();
    this.type_id = type_id.longValue();
    this.rank = rank.intValue();
    queryFeatProp(conn, queryFeatPropSql(),queryFeatPropParams()); 
  }

  ChadoFeatureProp(Long featureId, Long type_id, String value, Connection conn) {
    this.featureId = featureId.longValue();
    this.type_id = type_id.longValue();
    this.value = value;
    queryFeatProp(conn, queryFeatPropValueSql(),queryFeatPropValueParams()); 
  }
  // --------------------------------------------
  // ChadoFeatureprop - public methods
  // --------------------------------------------

  /**
   * chado feature_id from the featureprop
   */
  public long getFeatureId() { return this.featureId; } 

  /**
   * @return chado featureprop_id from the featureprop.
   */
  public long getFeaturepropId() { return this.featurepropId; }

  /**
   * @return chado type_id from the featureprop.
   */     
  public long getTypeId() { return this.type_id; }

  /**
   * @return chado value from the featureprop.
   */
  public String getValue() { return this.value; }
  
  /**
   * @return chado rank column for the featureprop
   */  
  public int getRank() { return this.rank; }



  /**
   * @return The featureprop.feature_id.
   */
  public ChadoFeature getFeature() {
    // TODO - return value won't be valid unless correct constructor was used
    return feature;
  }

  // --------------------------------------------
  // ChadoFeatureProp - protected methods
  // --------------------------------------------

  private String queryFeatPropSql() {
  	return "SELECT featureprop_id, feature_id, type_id, value, rank " +
      "FROM featureprop WHERE feature_id = ? AND type_id = ? AND rank = ?";
  }
  
  private ArrayList queryFeatPropParams() {
  	ArrayList list = new ArrayList(3);
	list.add(new Long(featureId));
	list.add(new Long(type_id));
	list.add(new Integer(rank));
	return list;
  }

  private String queryFeatPropValueSql() {	
  	return "SELECT featureprop_id, feature_id, type_id, value, rank " +
      "FROM featureprop WHERE feature_id = ? AND type_id = ? AND value = ?";
  }  
  
  private ArrayList queryFeatPropValueParams() {
  	ArrayList list = new ArrayList(3);
	list.add(new Long(featureId));
	list.add(new Long(type_id));
	list.add(new String(value));
	return list;
  }  
  /**
   * Query the database for the featureprop info. for <code>this.featureId</code>
   */
  protected void queryFeatProp(Connection conn, String sql, ArrayList params) {
  
    logger.debug(sql);

    try {
       PreparedStatement s = conn.prepareStatement(sql);
       for(int i=0; i<params.size(); i++ ) {
	Object o = params.get(i);
	if(o instanceof Long)
	   s.setLong((i+1), ((Long) o).longValue());
	else if(o instanceof Integer)
    	   s.setInt((i+1), ((Integer) o).intValue());
	else if(o instanceof String)
    	   s.setString((i+1), (String) o);
	else
	   logger.info("Problem with this parameter "+o);	
      }
      ResultSet rs = s.executeQuery();
      // should be only one row
      
      if (rs.next()) {
        featurepropId = rs.getLong(1);
        long fid = rs.getLong(2);
	feature = new ChadoFeature(new Long(fid), conn, false);
	type_id = rs.getLong(3);
	value = rs.getString(4);
        rank = rs.getInt(5);

      } else {
	logger.error("no rows returned by queryFeatProp query");
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving featureprop for feature_id = " + featureId, sqle);
    }
  }
  
}
