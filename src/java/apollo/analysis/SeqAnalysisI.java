/*
 * SeqAnalysisI
 *
 */

package apollo.analysis;

/**
 * Some kind of analysis that can be run on a Sequence object(s)
 * <P>
 * Examples of analysis objects - A Blast process running on
 * a local machine, a Gene Prediction to be run via the
 * API to a beowulf cluster / pipeline system, an interpro job
 * That has finished running and is processed...
 * - or composites combining these.
 * <P>
 * An SeqAnalysisI object can be a specification of how to run
 * an analysis (locally or remotely); it may represent the analysis
 * to be run / while running / or one that has been run.
 * <P>
 * This interface makes no assumptions as to whether the analysis
 * is run locally, run externally through some kind of server
 * (examples - NCBI web server; a simple CGI wrapper to a
 *  beowulf cluster; a BSANE compliant server....) 
 * <P>
 * The idea is that the implementation of this class handles
 * any kind of parsing and filtering of the results, 
 * (possibly) turning them into apollo.datamodel objects. 
 * It should of course be possible to plug in different parsers
 * and/or filtering classes. This should be controlled by
 * the implementing class, and configured via the properties.
 * <P>
 * If the analysis is run externally, then the external server
 * may do all the parsing/filtering (for instance, the BDGP
 * server will run BOP over the results and return the corresponding
 * GAME XML to the implementing class).
 * <P>
 * Rather than hardcode a set of attributes which may or may
 * not be relevant to the particular analysis type, extensible
 * Properties are used.
 * This keeps the SeqAnalysis object flexible.
 * One can imagine analysis servers allowing certain
 * options unanticipated by the creators of this interface or the
 * GUI for this interface. It should be easy to plug in
 * new analysis servers / interfaces at run time without
 * writing or compiling any extra code.
 *
 * Of course, there should be a standard set of properties that
 * most implementations should respect:
 *
 * program
 * programVersion
 * programArguments
 * fastaDatabaseName   (for similarity programs; eg nr, drosophila,...)
 * fastaDatabasePath   (for locally run blasts etc)
 * 
 * (This interface can be thought of as a facade over a mini-ontology
 *  of sequence analysis programs)
 *
 * TODO: Exceptions!!! v important!!!
 *
 *
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.datamodel.*;

public interface SeqAnalysisI {

  /**
   * set the value of a property
   **/
  public void        addProperty(String name, String property);

  /**
   * get the value of a property
   **/
  public String      getProperty(String name);

  /**
   * get the description of a property;
   * this gives metadata to the GUI/front end
   **/
  public String      getPropertyDescription(String name);

  /**
   * get the type of a property  (optional).
   * more metadata to help the GUI construct sensible
   * interfaces
   **/
  public String      getPropertyType(String name);

  /**
   * all set properties
   * (should this be public????)
   **/
  public Hashtable   getProperties();

  /**
   * all the known property names (in the order they
   * should be shown in a GUI)
   **/
  public Vector      getAllowedProperties();

  /**
   * allowed/recommended Vector of values
   * for a property
   **/
  public Vector      getAllowedValues(String name);

  /**
   * @return the analysis name
   **/
  public String      getName();
  public void        setName(String name);

  /**
   * @return a human readable String
   **/
  public String      getDesc();
  public void        setDesc(String name);

  public String getProgramName();


  /**
   * Retrieve the SequenceI that this analysis 
   * builds a CurationSet from
   *
   * @return the input SequenceI
   */
  public SequenceI   getInputSequence();
  public void   setInputSequence(SequenceI sequence);

  /**
   * Retrieve the SequenceI list that this builds a curation set
   * from
   *
   * @return a Vector of SequenceI objects
   */
  public Vector      getInputSequences();
  public void   setInputSequence(Vector sequences);


  /**
   * usually returns 1, but some programs
   * (eg clustalw) require >1 input sequence
   **/
  public int         getInputSequenceCount();

  /**
   * Once you have configured the SeqAnalysis properties,
   * call this method to run the analysis.
   * The 
   **/
  public boolean launch();

  /**
   * is the analysis asynchronous
   * (ie will launching the analysis cause the program
   *  to block?)
   * hmmm, this should probably be a property...
   **/
  public boolean     isAsynchronous();

  /**
   * has the analysis been launched?
   **/
  public boolean     isStarted();

  /**
   * does the analysis have any results to read yet?
   * note: an analysis implementation may choose to
   * give results incrementally, so it need not
   * be finished before returning results
   **/
  public boolean     hasResults();

  /**
   * has the analysis finished?
   **/
  public boolean     isFinished();

  /**
   * @return CurationSet representing the analysis results
   **/
  public CurationSet getCurationSet();

  /**
   * @return FeatureSetI representing the analysis results
   **/
  public FeatureSet getFeatureSet();

  /**
   * @return String representing all the results
   * as a single string.
   *
   * If an analysis returns two outputs, say the main output
   * plus an error/warnings file, then the implementation
   * may choose to return just the main output, or the main
   * output with the error file glommed onto the end.
   *
   * this String should *NOT* be parsed; it
   * is up to the SeqAnalysisI implementation object to
   * do any parsing and return the results as a CurationSet
   * or similar object
   **/
  public String getAllRawResults();

  /**
   * all the raw results - usually Vector of
   * main output and error (if any) as Strings
   *
   * these Strings should *NOT* be parsed; it
   * is up to the SeqAnalysisI implementation object to
   * do any parsing and return the results as a CurationSet
   * or similar object
   **/
  public Vector getRawResultVector();

  public Hashtable getRawResultHashtable();

}
