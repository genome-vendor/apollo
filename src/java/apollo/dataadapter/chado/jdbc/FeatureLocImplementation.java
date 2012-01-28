package apollo.dataadapter.chado.jdbc;

import org.apache.log4j.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import apollo.datamodel.SequenceI;

/** 
 * Different implementations of chado deal with featurelocs differently - 
 * all hanging off top level feats (FB), 
 * or redundant featlocs at each level for optimization (TIGR) 
 * This also takes in padding that will pad out the where clause - this is starting
 * to go beyond FeatureLoc - rename ChadoRange? ChadoQueryRange?
 */
class FeatureLocImplementation {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureLocImplementation.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected long featId;
  private ChadoFeatureLoc featureLoc;
  protected Connection conn;
  protected boolean haveRedundantFeatLocs;
  private int padding = 0;
  private String topLevelFeatName;
  protected int max;
  protected int flankLength = 20000;
  
  /**
   * For subclassing.
   */
  protected FeatureLocImplementation() {
  }

  FeatureLocImplementation(long featId, boolean redundantLocs, Connection conn) {
    this (featId,redundantLocs,conn,0,0);
  }

  FeatureLocImplementation(long featId,boolean haveRedundantFeatLocs, Connection conn,
      int padding) {
    this(featId, haveRedundantFeatLocs, conn, padding, 0);
}
  
  
  FeatureLocImplementation(long featId,boolean haveRedundantFeatLocs, Connection conn,
                           int padding, int max) {
    this.featId = featId;
    this.conn = conn;
    this.haveRedundantFeatLocs = haveRedundantFeatLocs;
    this.padding = padding;
    this.max = max;
  }


  /** used for non-redundant feat locs */
  private ChadoFeatureLoc getFeatureLoc() {
    if (featureLoc == null) {
      featureLoc = new ChadoFeatureLoc(featId,conn);
    }
    return featureLoc;
  }

  /** Get src feature id to use for querying feature. For redundant feat locs
      this is the feature itself, otherwise its the top level feature that the
      feature hangs off of - rename this? */
  long getContainingFeatureId() {
    return haveRedundantFeatLocs ? featId : getFeatureLoc().getSourceFeature().getFeatureId();
  }
  
  long getFeatureId() {
    return featId;
  }

  /** How much to pad out where fmin and fmax in where clause. This is used
      to retrieve region surrouding the feature of interest. This is starting
      to creep out of pure FeatureLoc so perhaps the class should be renamed to
      something like ChadoQueryRange? */
  void setPadding(int padding) {
    this.padding = padding;
  }

  /** Returns true if there is no padding set (padding == 0) */
  private boolean noPadding() { 
    return padding == 0;
  }

  /** Returns true if feat loc same range as the feature itself. this is false if
      there is padding OR for chrom feat locs (subclass) which have sub-range */
  protected boolean featLocSameAsFeatRange() {
    return noPadding();
  }

  /** get min converted to base-oriented (add one) with padding subtracted.
   *  If subtracting the padding causes it to go beyond the end (<1) return 1
   *  instead
   */
  int getBaseOrientedMinWithPadding() {
    return getFeatureLoc().getBaseOrientedMin() - getBaseOrientedMinPadding();
  }

  /** base oriented and interbase max are the same
   * If adding the padding causes it to go beyond the end (>length) return
   * the length instead
   * @return max with padding
   */
  int getMaxWithPadding() {
    return getFeatureLoc().getFmax().intValue() + getMaxPadding(); 
  }

  /** get length from min to max plus padding on both sides */
  int getLengthWithPadding() {
    return getFeatureLoc().getLength() + getBaseOrientedMinPadding() + getMaxPadding();
  }

  private int getBaseOrientedMinPadding()
  {
    if (getFeatureLoc().getBaseOrientedMin() - padding < 1) {
      return getFeatureLoc().getBaseOrientedMin() - 1;
    }
    return padding;
  }
  
  private int getMaxPadding()
  {
    if (max == 0) {
      return padding;
    }
    if (getFeatureLoc().getFmax().intValue() + padding > max) {
      return max - getFeatureLoc().getFmax().intValue();
    }
    return padding;
  }

  /** Add the flanking region to a coordinate upstream (subtract)
   * 
   * @param coord - coordinate to have flank added upstream
   * @return coordinate with flank
   */
  protected int getCoordinateWithFlankUpstream(int coord)
  {
    int c = coord - flankLength;
    if (c < 0) {
      c = 0;
    }
    return c;
  }

  /** Add the flanking region to a coordinate downstream (add)
   * 
   * @param coord - coordinate to have flank added downstream
   * @return coordinate with flank
   */
  protected int getCoordinateWithFlankDownstream(int coord)
  {
    int c = coord + flankLength;
    if (max > 0 && c > max) {
      c = max;
    }
    return c;
  }
  
  /** Returns string to add for where clause for range relevant to feature.
      Returns empty string if redundant feat locs - dont need range clause */
  String getContainingFeatureWhereClause(String featLocTableName) {
    if (haveRedundantFeatLocs && noPadding()) 
      return "";
    else {
      int min = getCoordinateWithFlankUpstream(getBaseOrientedMinWithPadding());
      int max = getCoordinateWithFlankDownstream(getMaxWithPadding());
      /*
      int min = getFeatureLoc().getFmin().intValue() - padding; // method for this?
      int max = getFeatureLoc().getFmax().intValue() + padding;
      //this can be really slow when there are many featurelocs to filter, it's more efficient
      //to just use between instead
      return " AND "+featLocTableName+".fmax > " + min
        +" AND "+featLocTableName+".fmin < "+ max +" ";
      */

      return " AND " + featLocTableName + ".fmax BETWEEN " + min + " AND " + max + " AND " +
      featLocTableName + ".fmin BETWEEN " + min + " AND " + max + " AND " +
      featLocTableName + ".fmin < " + getMaxWithPadding() + " AND " +
      featLocTableName + ".fmax > " + getBaseOrientedMinWithPadding() + " ";
      
      /*
      return " AND " + featLocTableName + ".fmax BETWEEN " + min + " AND " + max +
      " AND " + featLocTableName + ".fmin BETWEEN " + min + " AND " + max + " ";
      */
    }
  }
  
  String getSrcFeatureType() {
    if (featureLoc == null)
      featureLoc = new ChadoFeatureLoc(featId, conn);
    if (featureLoc.getSourceFeature() == null) {
      return null;
    }
    return featureLoc.getSourceFeature().getType();
  }

  boolean hasTopLevelName() { return topLevelFeatName != null; } 

  void setTopLevelFeatName(String name) {
    topLevelFeatName = name;
  }
  String getTopLevelFeatName() { return topLevelFeatName; }

  SequenceI retrieveSequence(JdbcChadoAdapter adapter) {
    if(featLocSameAsFeatRange()) {
      //boolean hasRange = true;
      return adapter.getSequence(getFeatureId());
    }
    else
      return retrieveSequenceUsingFeatLoc(adapter);
  }

  /** feature doesnt have residues of its own (eg fb gene). 
      have to get residues from substring of featureloc srcfeature 
      this is also used to get sub-ranges of large feats (ie chromosomes)
      @param featLoc      has feat id and padding
      @param seqHasRange  if true return GAMESequence, else Sequence
  */
  private SequenceI retrieveSequenceUsingFeatLoc(JdbcChadoAdapter adap) {
    String sql = "SELECT f.name, f.organism_id, "+
      "substring(residues from " + getBaseOrientedMinWithPadding() +
      " for "+ getLengthWithPadding() + ") as seq, length(residues) as max "+
      "FROM feature f "+
      // is containing feature id guaranteed to be the one with seq?
      "WHERE f.feature_id = "+getContainingFeatureId();

    try {
      ResultSet rs = adap.executeLoggedSelectQuery("FeatureLocImplementation.retrieveSequenceUsingFeatLoc", sql);
      if (rs.next()) {
        max = rs.getInt("max");
        // this name ends up being the "chromosome" (rename this!) of CurationSet
        setTopLevelFeatName(rs.getString("name"));
        String name = getTopLevelFeatName()+":"+getBaseOrientedMinWithPadding()
          +"-"+getMaxWithPadding();
        String residues = rs.getString("seq");
        String organism = adap.getOrganismFullName(rs.getInt("organism_id"));
        //boolean seqHasRange = true;
        return adap.makeSequence(name,residues,organism);
      }
    } catch (SQLException sqle) {
      logger.error("retrieveSequenceUsingFeatLoc SQLException", sqle);
    }
    return null;
    
  }
}
