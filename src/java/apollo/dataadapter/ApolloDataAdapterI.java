package apollo.dataadapter;

import apollo.datamodel.*;
import apollo.config.Style;
//import apollo.gui.event.DataLoadListener;// should be in data adapter pkg

import org.bdgp.io.*;

import java.util.*;

/**
 * DataAdapterI interface
 *
 * Used to load annotated and non-annotated data from
 * whatever datasource

 Could this implement org.bdgp.io.VisualDataAdapter or are there apollo 
 adapters with no UI? 
 */
//public interface ApolloDataAdapterI extends DataAdapter {
public interface ApolloDataAdapterI extends VisualDataAdapter {

  public static final IOOperation OP_READ_DATA =
    new IOOperation("Read data");
  public static final IOOperation OP_WRITE_DATA =
    new IOOperation("Write data");
  public static final IOOperation OP_READ_SEQUENCE =
    new IOOperation("Read sequence");
  public static final IOOperation OP_READ_RAW_ANALYSIS =
    new IOOperation("Read raw analysis results");
  public static final IOOperation OP_APPEND_DATA =
    new IOOperation("Add additional data");

  /**
   * returns a saved CurationSet.
   */
  public CurationSet getCurationSet() throws ApolloAdapterException;

  public void setCuration(CurationSet curation) throws ApolloAdapterException;
  public Boolean addToCurationSet() throws ApolloAdapterException;




  /**
   * writes the changes from a featureChangeLog to a writeable datasource.
   Not just a featureChangeLog - this writes out the whole curationSet
    */
  public void commitChanges(CurationSet curationSet) throws ApolloAdapterException;
  /** Write out composite data holder to multiple data adapters 
      (only SyntenyAdapter actually implements this) */
  public void commitChanges(CompositeDataHolder cdh) throws ApolloAdapterException;

  /** Straight from DataAdapterGUI.doOp. Could be cur set or comp data holder */
  public void commitChanges(Object values) throws ApolloAdapterException;

  /** GAMEAdapter and ChadoXMLAdapter can specify whether to save annots and
      whether to save results */
  public void commitChanges(Object values, boolean saveAnnots, boolean saveResults) throws ApolloAdapterException;

  /** This should replace setInput. setInput is limited to just a String which
      is awkward for locations where you have to jam several fields into one
      String. DataInput deals with locations much better. 
      DataInput can complement stateInfo (hopefully?) */
  public void setDataInput(DataInput dataInput);

  /** Returns the type of input data (gene,file,band...)
   * I believe this is now contained in the DataInput object, so this should probably
   * be phased out eventually
   * @see apollo.dataadapter.DataInputType
   * Should this go into org.bdgp.io.DataAdapter?
   */
  public DataInputType getInputType();

  /** Returns the input String passed to the DataAdapter, the input is
   * of course associated with the input type
   * Should this go into org.bdgp.io.DataAdapter? */
  public String getInput();

  /** DataInputType describes the type of input (gene,cytology,scaffold...)
      DataInput contains a DataInputType, so setDataInput covers this. 
      Phase this method out? */
  public void setInputType(DataInputType type);

  /** Input string that corresponds with the input type
   * (eg gene name for gene input type)
  setDataInput is the better way to go. This should be phased out.
   */
  public void setInput(String input);

  /** State info Properties carries all the info needed for the adapter to do its query
      This is an alternative to setDataInput. For most cases setDataInput should be
      sufficient. This should return StateInformation */
  public Properties getStateInformation();
  /** This should set StateInformation object! see StateInformation for Strings
      to use for keys. */
  public void setStateInformation(Properties props);

  /** Both EnsJAdapters use this and EnsCGIAdapter to pass in location string. This
      is a precursor to setInput & setInputType, and setStateInformation. These should
      all probably be collapsed, which is to say setRegion and setInput should probably
      be replaced by setStateInfo(?). Actually now im thinking setDataInput is the
      way to go. StateInfo is more generic than need be. setRegion has nothing to do with
      new Region object - maybe should rename Region ApolloLoc
      @deprecated use setDataInput(DataInput) */
  public void setRegion(String extId) throws ApolloAdapterException;

  /** Strings for input types */
  //public final static String LOCATION = "LOCATION_INPUT_TYPE";

  public SequenceI getSequence(String id) throws ApolloAdapterException;
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException;
  public SequenceI getSequence(DbXref dbxref, int start, int end) throws ApolloAdapterException;

  public Vector    getSequences(DbXref[] dbxref) throws ApolloAdapterException;
  public Vector    getSequences(DbXref[] dbxref, int[] start, int[] end) throws ApolloAdapterException;

  public String getRawAnalysisResults(String id) throws ApolloAdapterException;


  /** A data listener gets notified when new data is loading 
      - merged with DataLoadEvent*/
  //public void addDataListener(DataListener dataListener);
  
  /**
   * Send any necessary signals to the server to release annotation locks or undo edits
   * --after the user has been prompted that these will be lost.
  **/
  //public boolean rollbackAnnotations(CurationSet curationSet);
  public boolean rollbackAnnotations(CompositeDataHolder cdh);

  /** Bring up the link as a species in synteny. Presently this can only be done in
      synteny mode - eventually would be nice to do from regular apollo */
  public void loadNewSpeciesFromLink(SeqFeatureI link,CompositeDataHolder cdh) 
    throws org.bdgp.io.DataAdapterException;

  /** Synteny gives a species to each adapter to help keep track of them */
  public String getSpecies();

  /** Presently only game adapter uses this */
  public void setDatabase(String database);


  /** Request to "pad" the input padLeft basepairs to the left(5' forward strand) */
  public void setPadLeft(int padLeft);
  /** Request to "pad" the input padRight basepairs to the right(3' forward strand) */
  public void setPadRight(int padRight);

  /** By default a data adapter uses the style listed with it in the config file.
      In synteny mode the childrens default style needs to be overridden by the 
      synteny adapters style. This method os for the synteny override of its
      child adapters style. */
  public void setStyle(apollo.config.Style style);
  
  /** Return style associated with data adapter - at the moment synteny overrides all
      its child adapters with its own style - this may change in the future */
  public Style getStyle();

  /** returns true if the data adapter holds species adapters.
   Rename this hasSpeciesAdapters()? */
  public boolean isComposite();
  
  /** if isComposite() then returns child adapter for string adapterName.
      if not composite returns null. */
  public ApolloDataAdapterI getChildAdapter(String adapterName);
  /** Returns ith child adapter if isComposite(), null otherwise */
  public ApolloDataAdapterI getChildAdapter(int i);

  /** Returns number of child adapters. If isComposite() is false returns 0. */
  public int getNumberOfChildAdapters();

  public Map getAdapters();  

  /** Whether write operation is supported by data adapter */
  public boolean canWriteData();

  /** Whether data adapter contains link data used for synteny (e.g. GAME data 
      contains link data) */
  public boolean hasLinkData();
  /** Controller should be in apollo.controller package not gui */
  //public void setController(Controller c);
  public void setDataLoadListener(DataLoadListener l);

  /** Get CurationState associated with data adapter. */
  public CurationState getCurationState();
}
