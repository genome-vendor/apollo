package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import apollo.config.Config;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;

import org.apache.log4j.*;

/**
 * Class that stores info. on a single Chado feature (corresponding to a row 
 * in the feature table.)  Note that the information stored is only partial.
 *
 * @version $Revision: 1.5 $ $Date: 2008/04/03 00:02:46 $ $Author: gk_fan $
 */
class ChadoFeature {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoFeature.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected long feature_id;
  protected Long dbxref_id;
  protected long organism_id;
  protected String name;
  protected String uniquename;
  // TODO - use a larger datatype for this?
  protected String residues;
  protected Long seqlen;
  protected String md5checksum;
  protected long type_id;
  // TODO make sure that these boolean values are handled correctly in MySQL and PostgresQL
  protected boolean is_analysis;
  protected boolean is_obsolete;
  protected java.sql.Date timeaccessioned;
  protected java.sql.Date timelastmodified;

  // whether the value of <code>residues</code> has been retrieved from the database
  protected boolean residuesRetrieved;
  // cvterm.name that corresponds to type_id
  protected String type;

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------
  
  /**
   * Construct a ChadoFeature for the *reference sequence* of an Apollo SeqFeatureI 
   * (which may be newly-created)
   *
   * @param feat Apollo feature to lookup in the database.
   * @param conn connection to use for the lookup
   */
  // TODO - get rid of this constructor
  ChadoFeature(SeqFeatureI feat, Connection conn) {
    SequenceI refSeq = feat.getRefSequence();
    queryForFeatureInfo(refSeq, conn, false);
  }

  /**
   * Construct a ChadoFeature using a feature_id
   *
   * @param featureId feature_id of the chado feature
   * @param conn connection to use for the lookup
   * @param retrieveResidues whether to pull the 'residues' column out of the database
   */
  ChadoFeature(Long featureId, Connection conn, boolean retrieveResidues) {
    // TODO - use JdbcChadoAdapter instead of direct db connection?  We want all db access to be mediated by a single object
    queryForFeatureInfo(featureId, conn, retrieveResidues);
  }

  /**
   * Construct a ChadoFeature using explicitly-provided feature info.
   */  
  //  ChadoFeature(Long featureId, Long organismId, String type) {
  //    this.featureId = featureId;
  //    this.organismId = organismId;
  //    this.type = type;
  //  }

  // -----------------------------------------------------------------------
  // ChadoFeature
  // -----------------------------------------------------------------------

  public long getFeatureId() {
    return this.feature_id;
  }

  public Long getDbXrefId() {
    return this.dbxref_id;
  }

  public long getOrganismId() {
    return this.organism_id;
  }

  public String getName() {
    return this.name;
  }

  public String getUniquename() {
    return this.uniquename;
  }

  public String getResidues() {
    // TODO - throw an exception if !residuesRetrieved?
    return this.residues;
  }

  public Long getSeqlen() {
    return this.seqlen;
  }

  public String getMD5Checksum() {
    return this.md5checksum;
  }

  public long getTypeId() {
    return this.type_id;
  }

  public boolean getIsAnalysis() {
    return this.is_analysis;
  }

  // TODO - will need to change this for TIGR usage (is_obsolete used as a counter, not a boolean)
  public boolean getIsObsolete() {
    return this.is_obsolete;
  }

  public java.sql.Date getTimeAccessioned() {
    return this.timeaccessioned;
  }

  public java.sql.Date getTimeLastModified() {
    return this.timelastmodified;
  }

  public boolean getResiduesRetrieved() {
    return this.residuesRetrieved;
  }

  public String getType() {
    return this.type;
  }

  protected String getQuery(boolean retrieveResidues) {
    return "SELECT f.feature_id, f.dbxref_id, f.organism_id, f.name, f.uniquename, " +
      "f.seqlen, f.md5checksum, f.type_id, f.is_analysis, f.is_obsolete, " +
      "f.timeaccessioned, f.timelastmodified, cv.name " +
      (retrieveResidues ? ", f.residues " : "") +
      "FROM feature f, cvterm cv " +
      "WHERE f.type_id = cv.cvterm_id ";
  }

  protected void queryForFeatureInfo(SequenceI refSeq, Connection conn, boolean retrieveResidues) {
    String sql = getQuery(retrieveResidues) + " AND f.uniquename = ? ";
    logger.debug(sql);

    // HACK - this assumes that refSeq.getName() is the chado uniquename
    // TODO - this method should be removed (and the name -> id mapping handled elsewhere)
    String refseqUniquename = refSeq.getName();
    PreparedStatement pstmt = null;

    try {
      pstmt = conn.prepareStatement(sql);
      pstmt.setString(1, refseqUniquename);
      if (!runFeatureQuery(pstmt, "uniquename = " + refseqUniquename, retrieveResidues)) {
        // HACK - if refSeq.getName() is not found, then try with the substring before ':'
        //        ':' usually means sublocation within the sequence
        int idx = refseqUniquename.indexOf(':');
        logger.warn("No feature found for refSeq id " + refseqUniquename);
        if (idx != -1) {
          logger.warn("Trying refSeq id " + refseqUniquename.substring(0, idx));
          pstmt.setString(1, refseqUniquename.substring(0, idx));
          runFeatureQuery(pstmt, "uniquename = " + refseqUniquename, retrieveResidues);
        }
        
      }
    }
    catch(SQLException e) {
      logger.error(e.getMessage(), e);
    }
  }

  protected void queryForFeatureInfo(Long refSeqId, Connection conn, boolean retrieveResidues) {
    String sql = getQuery(retrieveResidues) + " AND f.feature_id = ? ";
    logger.debug(sql);
    PreparedStatement pstmt = null;

    try {
      pstmt = conn.prepareStatement(sql);
      pstmt.setLong(1, refSeqId.longValue());
      runFeatureQuery(pstmt, "feature_id = " + refSeqId, retrieveResidues);
    }
    catch(SQLException e) {
      logger.error(e.getMessage(), e);
    }
  }

  protected boolean runFeatureQuery(PreparedStatement pstmt, String idString, boolean retrieveResidues) {
    try {
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        feature_id = rs.getLong(1);
        long ldbxref_id = rs.getLong(2);
        dbxref_id = rs.wasNull() ? null : new Long(ldbxref_id);
        organism_id = rs.getLong(3);
        name = rs.getString(4);
        uniquename = rs.getString(5);
        long lseqlen = rs.getLong(6);
        seqlen = rs.wasNull() ? null : new Long(lseqlen);
        md5checksum = rs.getString(7);
        type_id = rs.getLong(8);
        is_analysis = rs.getBoolean(9);
        is_obsolete = rs.getBoolean(10);
        timeaccessioned = rs.getDate(11);
        timelastmodified = rs.getDate(12);
        type = rs.getString(13);
        if (retrieveResidues) {
          residues = rs.getString(14);
        }
      }
      else {
        return false;
      }

      // should be only one record
      if (rs.next()) {
        logger.error("ChadoFeatureLoc.runFeatureQuery: multiple rows retrieved for refseq with " + idString);
      }
    }
    catch(SQLException e) {
      logger.error(e.getMessage(), e);
    }
    return true;
  }
}
