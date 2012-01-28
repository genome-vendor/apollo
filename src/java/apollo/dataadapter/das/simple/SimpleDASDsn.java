/**
 * I am a lightweight implementation of the <code>DASDsn</code> interface.
 * I am a simple bag of attributes with no further internal functionality.
 * 
 * @see apollo.dataadapter.das.DASSegment
 * @author Vivek Iyer
 * and Nomi Harris
**/
package apollo.dataadapter.das.simple;
import apollo.dataadapter.das.*;
import java.util.Hashtable;

public class
      SimpleDASDsn
      implements
  DASDsn {

  private String sourceId;
  private String sourceVersion;
  private String source;
    // Now used to hold the base URL for the DAS server, e.g. http://das.biopackages.net/das/genome
    private String mapMaster;
  private String description;
    // e.g. <CAPABILITY type="segments" query_id="human/17/segment"/>
    private Hashtable capabilities = null;

  public SimpleDASDsn(
    String theSourceId,
    String theSourceVersion,
    String theSource,
    String theMapMaster,
    String theDescription
  ) {
    sourceId = theSourceId;
    sourceVersion = theSourceVersion;
    source = theSource;
    mapMaster = theMapMaster;
    description = theDescription;
    capabilities = new Hashtable();
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getSourceVersion() {
    return sourceVersion;
  }

  public String getSource() {
    return source;
  }

  public String getMapMaster() {
    return mapMaster;
  }

  public String getDescription() {
    return description;
  }

  public void setSourceId(String newValue) {
    sourceId = newValue;
  }

  public void setSourceVersion(String newValue) {
    sourceVersion = newValue;
  }

  public void setSource(String newValue) {
    source = newValue;
  }

  public void setMapMaster(String newValue) {
    mapMaster = newValue;
  }

  public void setDescription(String newValue) {
    description = newValue;
  }

  /** e.g. <CAPABILITY type="segments" query_id="human/17/segment"/> */
    public void addCapability(String type, String uri) {
	capabilities.put(type, uri);
	//	System.out.println("SimpleDASDsn.addCapability(" + type + ", " + uri + ") for " + getSourceId()); // DEL
    }
    public String getCapabilityURI(String type) {
	return (String) capabilities.get(type);
    }

    public Hashtable getCapabilities() {
	return capabilities;
    }

  public String toString() {
    // This needs to be something that can go in the pulldown list of sources
    return getSourceId(); // for now
    //    return "SimpleDASDsn: sourceID = " + getSourceId() + ", MapMaster = " + mapMaster;
  }
}
