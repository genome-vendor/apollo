package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Each instance of this class represents a specific Chado schema version.  
 * The class also contains a method that attempts to guess the schema version
 * of a particular Chado database, given a connection to that database.
 * Currently the class knows about only two Chado schema versions, called
 * 'pre_fmb' and 'fmb'; these are successive versions of the schema used at
 * TIGR.  'fmb' stands for FlyBase Migration Build, so the fmb schema should
 * correspond to the Chado/GMOD cvs schema freeze.  Some work may need to be
 * done to bring this class up to speed with respect to the regular Chado
 * release cycle.
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.5 $ $Date: 2004/04/27 16:59:20 $ $Author: jcrabtree $
 */
public class SchemaVersion {

  // -----------------------------------------------------------------------
  // Static/class variables for the known Chado schema versions
  // -----------------------------------------------------------------------

  /**
   * Pre-flybase migration build 
   */
  public static SchemaVersion PRE_FMB = new SchemaVersion("pre_fmb");

  /**
   * Flybase migration build 
   */
  public static SchemaVersion FMB = new SchemaVersion("fmb");

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
    
  /**
   * A String that uniquely identifies a Chado schema version.
   */
  protected String tag;

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  /**
   * Note that the constructor is private so that no other class can create instances.
   */
  private SchemaVersion(String tag) {
    this.tag = tag;
  }

  // -----------------------------------------------------------------------
  // SchemaVersion - static methods
  // -----------------------------------------------------------------------

  /**
   * Attempt to determine the version of a chado schema by interrogating one
   * or two key fields or tables.  Hopefully it will be possible to do this in a more 
   * structured manner in future e.g., by retrieving an explicitly-stored schema
   * version number from the database (hint, hint).
   *
   * @param c A connection to a Chado database whose schema version must be be determined.
   * @return  The predicted version of the Chado database represented by <code>c</code>.
   */
  public static SchemaVersion guessSchemaVersion(Connection c) {
    if (c != null) {
      try {
	Statement s = c.createStatement();
	// the 'db' table does not exist in the pre_fmb schema
	ResultSet rs = s.executeQuery("select count(*) from db");
	long count; 
	while (rs.next()) { count = rs.getLong(1); }
      } catch (SQLException sqle) {
	return PRE_FMB;
      }
    }

    return FMB;
  }

  // -----------------------------------------------------------------------
  // SchemaVersion
  // -----------------------------------------------------------------------
    
  // Methods that return column names that differ between the various schema versions; 
  // this will have to be generalized in future.

  /**
   * @return The name of the chado column formerly known as featureprop.pkey_id
   */ 
  public String getPKeyCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "pkey_id" : "type_id";
  }

  /**
   * @return The name of the chado column formerly known as featureprop.pkey_id
   */
  public String getPValCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "pval" : "value";
  }

  /**
   * @return The name of the chado column formerly known as featureloc.fmin
   */
  public String getFMinCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "nbeg" : "fmin";
  }

  /**
   * @return The name of the chado column formerly known as featureloc.nend
   */
  public String getFMaxCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "nend" : "fmax";
  }

  /**
   * @return The name of the chado column formerly known as feature_relationship.subjfeature_id
   */
  public String getSubjFeatCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "subjfeature_id" : "subject_id";
  }

  /**
   * @return The name of the chado column formerly known as feature_relationship.objfeature_id
   */
  public String getObjFeatCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "objfeature_id" : "object_id";
  }

  /**
   * @return The name of the chado column formerly known as organism.abbrev
   */
  public String getAbbrevCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "abbrev" : "abbreviation";
  }

  /**
   * @return The name of the chado column formerly known as cv.cvname
   */
  public String getCvNameCol() {
    return (this.equals(SchemaVersion.PRE_FMB)) ? "cvname" : "name";
  }

  // -----------------------------------------------------------------------
  // Object
  // -----------------------------------------------------------------------

  public String toString() { return tag; }

}
