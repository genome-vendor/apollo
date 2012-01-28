package apollo.dataadapter.chado.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

/**
 * A set of chado.analysis rows that have the same value in their 
 * 'program' column.  The rows may also have the same 'programversion' 
 * and/or 'sourcename'.  If all three values (program, programversion,
 * and sourcename) are specified then the class is guaranteed (by
 * a constraint in the chado schema) to represent at most one row 
 * in the analysis table.
 *
 * @author cpommier
 * @version $Revision: 1.4 $ $Date: 2006/07/13 02:16:06 $ $Author: jcrabtree $
 */
public class ChadoProgram {

  // TODO: generalize the class to allow regular expressions and/or SQL LIKE strings in 
  //       the program, programversion, and sourcename specifications

  // a value to match against the chado analysis.program column; may not be null
  private String program;

  // a value to match against the chado analysis.programversion column; may be null
  private String programversion;

  // a value to match against the chado analysis.sourcename column; may be null
  private String sourcename;

  // whether CDS sequences should be retrieved for this type of program/analysis
  private boolean retrieveCDS;

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  /**
   * @param program A value to match against analysis.program.  May not be null.
   * @param programversion A value to match against analysis.programversion.  May be null.
   * @param sourcename A value to match against analysis.sourcename.  May be null.
   * @param retrievecds Whether to retrieve CDS sequences for this type of program.
   */
  public ChadoProgram(String program, String programversion, String sourcename, boolean retrievecds) {
    setProgram(program);
    setProgramversion(programversion);
    setSourcename(sourcename);
    setRetrieveCDS(retrievecds);
  }

  /**
   * @param program A value to match against analysis.program.  May not be null.
   * @param retrievecds Whether to retrieve CDS sequences for this type of program.
   */
  public ChadoProgram(String program, boolean retrievecds) {
    this(program, null, null, retrievecds);
  }

  /**
   * @param program A value to match against analysis.program.  May not be null.
   */
  public ChadoProgram(String program) {
    this(program, null, null, false);
  }
  
  // -----------------------------------------------------------------------
  // ChadoProgram
  // -----------------------------------------------------------------------

  // Get/set program name
  //
  public String getProgram() {
    return program;
  }
  public void setProgram(String newProgram) {
    if (newProgram == null) throw new IllegalArgumentException("setProgram cannot be passed a null value");
    program = newProgram;
  }

  // Get/set programversion
  //
  public String getProgramversion() { 
    return programversion; 
  }
  public void setProgramversion(String newProgramversion) {
    programversion = newProgramversion;
  }

  // Get/set sourcename
  //
  public String getSourcename() {
    return sourcename;
  }
  public void setSourcename(String newSourcename) {
    sourcename = newSourcename;
  }

  // Get/set retrieveCDS
  //
  public boolean retrieveCDS() {
    return retrieveCDS;
  }
  public void setRetrieveCDS(boolean newRetrieveCDS) {
    retrieveCDS = newRetrieveCDS;
  }
  
  // -----------------------------------------------------------------------
  // Object
  // -----------------------------------------------------------------------

  public String toString() {
    String program = getProgram();
    String programver = getProgramversion();
    String source = getSourcename();

    if (programver == null) { programver = ""; }
    if (source == null) { source = ""; }

    return "ChadoProgram:[program=" + program + ",programversion=" + programver + ",sourcename=" + source + "]";
  }
}
