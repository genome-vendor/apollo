package apollo.dataadapter.das.simple;

import apollo.dataadapter.das.*;

/**
 * I am a lightweight implementation of the <code>DASSequence</code> interface.
 * I am a simple bag of attributes with no further internal functionality.
 * 
 * @see apollo.dataadapter.das.DASSequence
**/
public class
      SimpleDASSequence
      implements
  DASSequence {
  private String id;
  private String start;
  private String stop;
  private String version;
  private String dNALength;
  private String dNA;

  public SimpleDASSequence(
    String theId,
    String theStart,
    String theStop,
    String theVersion,
    String theDNALength,
    String theDNA
  ) {
    id = theId;
    start = theStart;
    stop = theStop;
    version = theVersion;
    dNALength = theDNALength;
    dNA = theDNA;
  }//end SimpleDASSequence

  public String getId() {
    return id;
  }//end getId

  public String getStart() {
    return start;
  }//end getStart

  public String getStop() {
    return stop;
  }//end getStop

  public String getVersion() {
    return version;
  }//end getVersion

  public String getDNALength() {
    return dNALength;
  }//end getDNALength

  public String getDNA() {
    return dNA;
  }//end getDNA

  public void setId(String newValue) {
    id = newValue;
  }//end setId

  public void setStart(String newValue) {
    start = newValue;
  }//end setStart

  public void setStop(String newValue) {
    stop = newValue;
  }//end setStop

  public void setVersion(String newValue) {
    version = newValue;
  }//end setVersion

  public void setDNALength(String newValue) {
    dNALength = newValue;
  }//end setDNALength

  public void setDNA(String newValue) {
    dNA = newValue;
  }//end setDNA

  public String toString() {
    return getId()+":"+getStart()+","+getStop()+":"+getDNA()+"("+getDNALength()+")";
  }
}//end DASSequende


