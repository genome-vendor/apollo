/*
 * GenericProgHandler
 *
 */

package apollo.analysis;

/**
 * default Handler for any unrecognised analysis
 * 
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.analysis.*;
import apollo.datamodel.*;

public class GenericProgramHandler implements ProgramHandlerI {

  /**
   * different analysis programs require different ways of
   * running them
   *
   * assumes a command line is made by
   * programPath programParams inputSequence [datasourcePath]
   **/
  public String createUnixShellCommand(SeqAnalysisLocal seqAnalysis) {
    String path = seqAnalysis.getProperty("programName");
    String dbpath = seqAnalysis.getProperty("datasourcePath");
    if (dbpath == null) {
      String dbdir = seqAnalysis.getProperty("datasourceDir");
      String dbname = seqAnalysis.getProperty("datasourceName");
      if (dbdir == null) {
        dbpath = dbname;
      } else {
        dbpath = dbdir + "/" + dbname;
      }
    }
    if (dbpath == null) {
      dbpath = "";
    }
    String cmd = "";
    String seqpath = seqAnalysis.getSeqFile();
    try {
      String args = seqAnalysis.getProperty("programParams");
      if (args == null) {
        args = "";
      }
      cmd = path + " " + args + " " +
            seqpath + " " + dbpath;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return cmd;
  }

  /**
   * different programs must be parsed differently
   * implementations may also choose to also filter the results
   * in some configurable way. it is up to the individual
   * implementations to respect these
   **/
  public CurationSet parseAnalysisResults(SeqAnalysisI seqAnalysis) {
    System.out.println("not implementated!");
    return null;
  }
}
