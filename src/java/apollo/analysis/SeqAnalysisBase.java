/*
 * SeqAnalysisBase
 *
 */

package apollo.analysis;

/**
 * @author Chris Mungall
 **/

import java.util.*;

import apollo.analysis.*;
import apollo.datamodel.*;
import apollo.gui.ApolloFrame;

import org.bdgp.io.AbstractDataAdapter;
import org.bdgp.io.IOOperation;

public abstract class SeqAnalysisBase extends AbstractDataAdapter implements SeqAnalysisI, AnalysisDataAdapterI {

  protected SequenceI sequence;
  protected CurationSet curationSet;

  private Hashtable properties;
  private Hashtable propertyDescriptions;
  private Hashtable propertyTypes;
  private Hashtable defaultProperties;
  private Hashtable allowedValues;
  private Vector    allowedProperties;

  protected Hashtable rawResults;

  // DataAdapter compliance
  IOOperation [] supportedOperations = {
                                         AnalysisDataAdapterI.OP_ANALYZE_DATA
                                       };

  // CONSTRUCTORS
  public SeqAnalysisBase() {
    initSeqAnalysis();
  }

  /**
   * initialize with a properties hash
   **/
  public SeqAnalysisBase(Hashtable ht) {
    initSeqAnalysis();
    properties = ht;
  }

  // make it conform to org.bdgp.io.DataAdapter
  public void init() {}

  protected void initSeqAnalysis() {
    properties = new Hashtable();
    propertyDescriptions = new Hashtable();
    propertyTypes = new Hashtable();
    defaultProperties = new Hashtable();
    allowedValues = new Hashtable();
    allowedProperties = new Vector();

    rawResults = new Hashtable();
  }

  // make it conform to org.bdgp.io.DataAdapter
  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public void addProperty(String name, String value) {
    if (value != null && !value.equals (""))
      properties.put(name, value);
  }

  public String getProperty(String name) {
    if (properties.get (name) != null)
      return (String) properties.get(name);
    else
      return null;
  }

  public Hashtable getProperties() {
    return properties;
  }

  public Vector getAllowedProperties() {
    return allowedProperties;
  }

  protected void setAllowedProperties(Vector in) {
    allowedProperties = in;
  }

  public String getPropertyDescription(String name) {
    if (propertyDescriptions.get (name) != null)
      return (String) propertyDescriptions.get(name);
    else
      return null;
  }
  protected void setPropertyDescription(String name, String desc) {
    propertyDescriptions.put(name, desc);
  }

  public String getPropertyType(String name) {
    if (propertyTypes.get (name) != null)
      return (String) propertyTypes.get(name);
    else
      return null;
  }
  protected void setPropertyType(String name, String type) {
    propertyTypes.put(name, type);
  }

  public Vector getAllowedValues(String name) {
    if (allowedValues.get (name) != null)
      return (Vector) allowedValues.get(name);
    else
      return null;
  }
  protected void setAllowedValues(Hashtable in) {
    allowedValues = in;
  }
  protected void setAllowedValues(String name, Vector in) {
    allowedValues.put(name, in);
  }


  public String      getName() {
    return getProperty("name");
  }
  public String      getType() {
    return getProperty("name");
  }

  public void        setName(String name) {
    addProperty("name", name);
  }

  public String      getDesc() {
    return getProperty("desc");
  }
  public void        setDesc(String desc) {
    addProperty("desc", desc);
  }

  public String getProgramName() {
    return getProperty("programName");
  }

  protected String getProgramParams() {
    return getProperty("programParams");
  }

  public SequenceI   getInputSequence() {
    return null; //ApolloFrame.getSeqFromClipboard();
  }

  public void   setInputSequence(SequenceI inseq) {
    sequence = inseq;
    ;
  }

  public Vector      getInputSequences() {
    return null;
  }
  public void   setInputSequence(Vector sequences) {}

  public int getInputSequenceCount() {
    return 1;
  }

  /**
   * is the analysis asynchronous?
   * (ie will launching the analysis cause the program
   *  to block?)
   **/
  public boolean     isAsynchronous() {
    String v = getProperty("isAsynchronous");
    if (v == null || v.equals("")) {
      return false;
    } else {
      return true;
    }
  }

  public boolean launch() {
    return false;
  }

  public boolean     isStarted() {
    String v = getProperty("isStarted");
    if (v == null) {
      return false;
    } else {
      return true;
    }
  }

  public boolean     isFinished() {
    String v = getProperty("isFinished");
    if (v == null) {
      return false;
    } else {
      return true;
    }
  }

  public boolean hasResults() {
    // default behaviour unless overridden is
    // to never give results until analysis is finished
    String v = getProperty("isFinished");
    return (v.equals ("true"));
  }

  public CurationSet getCurationSet() {
    return curationSet;
  }

  public FeatureSet getFeatureSet() {
    return null;
  }

  public String getAllRawResults() {
    return (String)rawResults.get("out");
  }

  public Vector getRawResultVector() {
    Vector v = new Vector();
    v.addElement((String)rawResults.get("out"));
    return v;
  }

  public Hashtable getRawResultHashtable() {
    return rawResults;
  }

  protected void setRawResultHashtable(Hashtable in) {
    rawResults = in;
  }

}
