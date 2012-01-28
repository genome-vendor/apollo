/*
 * Created on Oct 9, 2004
 *
 */
package apollo.dataadapter.chado;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that represents the chado XML counterpart of an Apollo transaction.
 * Each chado transaction can be mapped to an SQL call.
 *
 * @author wgm
 */
public class ChadoTransaction {
  // Allowable operations:
  public static final Operation DELETE = new Operation("delete");
  public static final Operation UPDATE = new Operation("update");
  public static final Operation INSERT = new Operation("insert");
  public static final Operation LOOKUP = new Operation("lookup");
  public static final Operation FORCE = new Operation("force");
  // Genomic Sequence ID for map location
  public static final String MAP_POSITION_ID = "genomicSequence";

  private String tableName;
  private Map properties;
  private String id;
  private Operation operation;
  /** needed as exons use an alternate unique key (name instead of uniquename) */
  private boolean isExon = false;
  
  public ChadoTransaction() {
    operation = FORCE; // Default operation is "force"
  }
  
  public void setTableName(String name) {
    this.tableName = name;
  }
  
  public String getTableName() {
    return this.tableName;
  }
  
  public void setProperties(Map properties) {
    this.properties = properties;
  }
  
  public Map getProperties() {
    return this.properties;
  }
  
  public void addProperty(String propName, String value) {
    if (properties == null)
      properties = new HashMap();
    properties.put(propName, value);
  }

  void removeProperty(String propName) {
    if (properties == null)
      return;
    properties.remove(propName);
  }

  private String getProperty(String propName) {
    return (String)properties.get(propName);
  }
  
  /**
   * Set the id name for this Transaction. This is the id used in Chado transaction xml.
   * @param id
   */
  public void setID(String id) {
    this.id = id;
  }
  
  public String getID() {
    return this.id;
  }
  
  /** needed to distinguish exons different unique key - name instead of uniquename */
  public void setIsExon(boolean isExon) {
    this.isExon = isExon;
  }
  public boolean isExon() { return isExon; }

  /** Return the properties that are part of the unique key, needed for LOOKUP as you
   * only want to lookup on the unique key. returns all props if no unique key defined
   * currently only synonym has unique key defined */
  public Map getUniqueKeyProps() {
    // if we dont know the unique key just return all the props
    if (!hasUniqueKeyInfo())
      return getProperties();
    String[] keys = getUniqueKey();
    Map keyMap = new HashMap();
    for (int i=0; i<keys.length; i++) {
      keyMap.put(keys[i],getProperty(keys[i]));
    }
    return keyMap;
  }

  private boolean hasUniqueKeyInfo() {
    // only have hardwiring of key for sertain tables (syn & pub)
    return getUniqueKey() != null;
  }

  private final static String[] synKey = new String[] {"name","type_id"};
  private final static String[] pubKey = new String[] {"uniquename"};
  private final static String[] featKey
  = new String[] {"uniquename","type_id","organism_id"};
  private final static String[] exonKey  // controversial name instead of uniquename
  = new String[] {"name","type_id","organism_id"};

    /** would be cool to get unique key from database but this is probably platform
     * dependent? - for now just hardwiring - currently only doing syonym & pub 
     * returns null if no key defined for tableName */
  private String[] getUniqueKey() {
    if (tableName.equals("synonym"))
      return synKey;
    if (tableName.equals("pub"))
      return pubKey;
    // this is kinda wacky & controversial but for apollos sake exons use a unique key
    // that has name instead of uniquename, as name is where the range is which is
    // how we can check for shared exons - good ol shared exons
    if (tableName.equals("feature")) {
      if (isExon())
        return exonKey;
      else
        return featKey;
    }
      
    return null;
  }

  public Operation getOperation() {
    return this.operation;
  }
  
  public void setOperation(Operation op) {
    this.operation = op;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[" + operation + ":" + tableName + ":" + id + ":props=|");
    
    for(Iterator i = properties.keySet().iterator(); i.hasNext();) {
      String key = (String) i.next();
      String value = (String) properties.get(key);
      sb.append(key + "=" + value + "|");
    }

    sb.append("]");
    return sb.toString();
  }
  
  /** INNER CLASS Operation */
  public static class Operation {
    private String op;
    private static Map opMap = new HashMap();
    
    private Operation(String op) {
      this.op = op;
      opMap.put(op, this);
    }
    
    public String toString() {
      return op;
    }
    
    public static Operation getOperation(String op) {
      return (Operation) opMap.get(op);
    }

    boolean isLookup() { return this == LOOKUP; }
    boolean isForce() { return this == FORCE; }
    boolean isInsert() { return this == INSERT; }

  } // end of Operation inner class
}
