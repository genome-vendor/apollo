package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import apollo.config.Config;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;

import org.apache.log4j.*;

// TODO:
//  -move SQL out of this class (use JdbcChadoAdapter instead of direct JDBC Connection)
//  -handle multiple featurelocs correctly
//  -add static methods to perform all necessary apollo-chado coordinate conversions?
//    note that JdbcChadoAdapter already has the following two methods:
//       static int adjustLowForInterbaseToBaseOrientedConversion(int chadoFmin);
//       static int adjustLowForBaseOrientedToInterbaseConversion(int apolloLow);
//  -resolve/clarify the division of labor between this class and FeatureLocImplementation

/**
 * Class that stores a single Chado feature location (corresponding to a row
 * in the featureloc table) and performs conversions between the Chado and Apollo 
 * coordinate systems.  Can either pass in values for a featureloc from a previous
 * query result, or query for the featureloc given a feature id
 *
 * @version $Revision: 1.10 $ $Date: 2007/01/27 15:47:15 $ $Author: jcrabtree $
 */
class ChadoFeatureLoc {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoFeatureLoc.class);

  // --------------------------------------------
  // Instance variables
  // --------------------------------------------

  // object types are the ones that are nullable
  protected long featurelocId;
  protected long featureId;
  protected Integer fmin, fmax;
  protected boolean fmin_partial;
  protected boolean fmax_partial;
  protected Integer strand;
  protected Integer phase;
  protected String residueInfo;
  protected int locgroup;
  protected int rank;

  // reference sequence (also a feature) on which the main feature is located
  protected ChadoFeature sourceFeature;

  // --------------------------------------------
  // Constructors
  // --------------------------------------------

  /**
   * Construct a ChadoFeatureLoc using previously-determined fmin, fmax, and strand 
   * (which may have been read from a chado database.)
   *
   * @param fmin Chado featureloc.fmin.
   * @param fmax Chado featureloc.fmax.
   * @param fmin_partial Chado featureloc.is_fmin_partial
   * @param fmax_partial Chado featureloc.is_fmax_partial
   * @param strand Chado featureloc.strand
   */
  ChadoFeatureLoc(Integer fmin, Integer fmax, boolean fmin_partial, boolean fmax_partial, Integer strand) {
    if ((fmin != null) && (fmax != null) && (fmin.intValue() > fmax.intValue())) { 
      throw new IllegalArgumentException("by definition fmin (" + fmin + ") cannot be greater than fmax (" + fmax + ")"); 
    }
    this.fmin = fmin;
    this.fmax = fmax;
    this.fmin_partial = fmin_partial;
    this.fmax_partial = fmax_partial;
    this.strand = strand;
  }

  ChadoFeatureLoc(int fmin, int fmax, boolean fmin_partial, boolean fmax_partial, int strand) {
    this(new Integer(fmin), new Integer(fmax), fmin_partial, fmax_partial, new Integer(strand));
  }

  /**
   * Construct a ChadoFeatureLoc by querying the database for a specific feature_id.
   *
   * @param featureId Chado feature.feature_id
   * @param conn JDBC Connection to the chado database to query.
   */
  ChadoFeatureLoc(long featureId, Connection conn) {
    this.featureId = featureId;
    // TODO - use JdbcChadoAdapter instead of direct db connection?  We want all db access to be mediated by a single object
    // JC: What is this boolean query?  Doesn't seem to be documented.
    queryFeatLoc(conn); // optional with boolean query?
  }

  /**
   * Construct a ChadoFeatureLoc using an Apollo SeqFeatureI (which may be newly-created)
   */
  ChadoFeatureLoc(SeqFeatureI feat, ChadoFeature sourceFeature, Connection conn) {
    // TODO - use JdbcChadoAdapter instead of direct db connection?  We want all db access to be mediated by a single object

    // set fmin, fmax, and strand based on <code>apolloFeat</code>
    int start = feat.getStart();
    int end = feat.getEnd();
    int strand = feat.getStrand();

    if (strand == -1) {
      this.fmin = new Integer(end - 1);
      this.fmax = new Integer(start);
    } else {
      this.fmin = new Integer(start - 1);
      this.fmax = new Integer(end);
    }

    this.fmin_partial = false;
    this.fmax_partial = false;

    // Apollo and chado strand is the same (1=forward, -1=reverse, 0=none)
    this.strand = new Integer(strand);
    
    if (sourceFeature != null) {
      this.sourceFeature = sourceFeature;
    } else {
      this.sourceFeature = new ChadoFeature(feat, conn);
    }

    // other defaults (redundant)
    this.phase = null;
    this.residueInfo = null;
    this.rank = 0;
    this.locgroup = 0;
  }
    
  // --------------------------------------------
  // ChadoFeatureLoc - public methods
  // --------------------------------------------

  /**
   * chado feature_id from the featureloc
   */
  public long getFeatureId() { return this.featureId; }

  /**
   * @return chado featureloc_id from the featureloc.
   */
  public long getFeaturelocId() { return this.featurelocId; }

  /**
   * @return chado (interbase, 0-indexed) fmin of the featureloc.
   */
  public Integer getFmin() { return this.fmin; }
  
  /**
   * @return chado (interbase, 0-indexed) fmax of the featureloc.
   */
  public Integer getFmax() { return this.fmax; }

  /**
   * @return chado is_fmin_partial for the featureloc.
   */
  public boolean getFminPartial() { return this.fmin_partial; }
  
  /**
   * @return chado is_fmax_partial for the featureloc.
   */
  public boolean getFmaxPartial() { return this.fmax_partial; }

  /**
   * @return chado phase column for the featureloc
   */
  public Integer getPhase() { return this.phase; }

  /**
   * @return chado residue_info column for the featureloc
   */
  public String getResidueInfo() { return this.residueInfo; }

  /**
   * @return chado locgroup column for the featureloc
   */
  public int getLocgroup() { return this.locgroup; }

  /**
   * @return chado rank column for the featureloc
   */
  public int getRank() { return this.rank; }

  /**
   * @return chado strand of the featureloc
   */
  public Integer getStrand() {
    // JC: this may be a problem, because '0' should be a valid strand in chado (AFAIK)
    int realStrand = ((strand != null) && (strand.intValue() == -1)) ? -1 : 1;
    return new Integer(realStrand);
  }

  /**
   * @return The length of the featureloc in bases.
   */
  public int getLength() { 
    if ((this.fmax == null) || (this.fmin == null)) {
      return 0; // TODO
    }
    // calculating the length is easy with interbase coords:
    return this.fmax.intValue() - this.fmin.intValue(); 
  }

  /**
   * @return The featureloc.srcfeature_id.
   */
  public ChadoFeature getSourceFeature() {
    // TODO - return value won't be valid unless correct constructor was used
    return sourceFeature;
  }

  // --------------------------------------------
  // ChadoFeatureLoc - protected methods
  // --------------------------------------------

  /**
   * Query the database for the featureloc info. for <code>this.featureId</code>
   */
  protected void queryFeatLoc(Connection conn) {
    // TODO - move all SQL into a single location (currently Jdbc adapter class)
    String sql = "SELECT featureloc_id, srcfeature_id, fmin, fmax, strand, " +
      "is_fmin_partial, is_fmax_partial, phase, residue_info, locgroup, rank " +
      "FROM featureloc WHERE feature_id = " + featureId;
    logger.debug(sql);

    try {
      Statement s = conn.createStatement();
      ResultSet rs = s.executeQuery(sql);
      // should be only one row
      // JC: this is not true in the context of redundant/multiple featurelocs
      // TODO - should be using unique key on feature_id, locgroup, rank
      if (rs.next()) {
        featurelocId = rs.getLong(1);
        long sfid = rs.getLong(2);
	sourceFeature = new ChadoFeature(new Long(sfid), conn, false);
	int ifmin = rs.getInt(3);
        fmin = (rs.wasNull()) ? null : new Integer(ifmin);
	int ifmax = rs.getInt(4);
        fmax = (rs.wasNull()) ? null : new Integer(ifmax);
	strand = new Integer(rs.getInt(5));
	fmin_partial = rs.getBoolean(6);
	fmax_partial = rs.getBoolean(7);
        int iphase = rs.getInt(8);
        phase = (rs.wasNull()) ? null : new Integer(iphase);
        residueInfo = rs.getString(9);
        locgroup = rs.getInt(10);
        rank = rs.getInt(11);

        // error if there are multiple featurelocs
        if (rs.next()) {
          logger.error("ChadoFeatureLoc.queryFeatLoc: multiple featurelocs returned for feature " + featureId);
        }
      } else {
	logger.error("no rows returned by queryFeatLoc query");
      }
    } catch (SQLException sqle) {
      logger.error("SQLException retrieving featureloc for feature_id = " + featureId, sqle);
    }
  }

  /** get Fmin converted to base-oriented (add one) */
  int getBaseOrientedMin() { 
    return JdbcChadoAdapter.adjustLowForInterbaseToBaseOrientedConversion(getFmin().intValue());
  }
}
