/*
 * ProgramHandler
 *
 */

package apollo.analysis;

/**
 * interface specifies the behaviour of a program handler
 *
 * the program handler acts as a kind of adapter between
 * the analysis program (usually run in the unix shell)
 * and the SeqAnalysisI object
 * 
 * it provides a way of executing an analysis program,
 * and a way of interpreting the results
 * 
 * 
 * In general, a program handler is only required by
 * SeqAnalysisLocal
 *
 * (if you are running an analysis program non-locally then
 * presumably the remote server will deal with these things)
 * 
 * @see ProgramHandlerFactory
 * @see GenericProgramHandler
 * 
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.analysis.*;
import apollo.datamodel.*;

public interface ProgramHandlerI {

  /**
   * different analysis programs require different ways of
   * running them
   *
   **/
  public String createUnixShellCommand(SeqAnalysisLocal seqAnalysis);

  /**
   * different programs must be parsed differently
   * implementations may also choose to also filter the results
   * in some configurable way. it is up to the individual
   * implementations to respect these
   **/
  public CurationSet parseAnalysisResults(SeqAnalysisI seqAnalysis);
}
