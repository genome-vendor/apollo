package apollo.datamodel;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/** In Apollo, synonyms used to be strings, but in the ChadoXML data, synonyms
    have other fields (pub_id, is_current), so this new datamodel allows us to
    capture those fields (and any others that may come up) as properties. 
    Note:  addProperty etc. are duplicated in other classes--should break out
    as a separate class. */

public class Synonym 
  implements java.io.Serializable {
  protected String name;
  protected Hashtable properties = null;
  private String owner;
  private int ownerId;

  public Synonym() {}

  public Synonym(String syn) {
    this.name = syn;
  }

  public Synonym(String name, String owner) {
    this.name = name;
    this.owner = owner;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean hasOwner() {
    return owner != null;
  }
  public void setOwner(String owner) {
    this.owner = owner;
  }
  public String getOwner() {
    return owner;
  }

  /** not sure if we actually need this? */
  public void setOwnerId(int ownerId) {
    this.ownerId = ownerId;
  }
  public int getOwnerId() {
    return ownerId;
  }
  
  public Synonym cloneSynonym() {
    Synonym clone = new Synonym(name);
    // this is a shallow clone as the properties are not cloned
    clone.setProperties(getProperties());
    return clone;
  }

  public void clearProperties () {
    // Clearing out the hash isn't good enough.  If we've cloned this object,
    // we need to set its properties to null so the clone will be forced to
    // make a new properties rather than dangerously sharing its parent's.
    properties = null;
  }

  public void addProperty(String key, String value) {
    if (value != null && !value.equals("")) {
      if (properties == null) {
        properties = new Hashtable(1, 1.0F);
      }
      Vector values;
      if (properties.get(key) == null) {
        values = new Vector();
        properties.put(key, values);
      } else {
        values = (Vector) properties.get(key);
      }
      values.addElement(value);
    }
  }

  public void removeProperty(String key) {
    if (key != null && !key.equals("")) {
      if (properties == null)
        return;
      if (properties.containsKey(key)) {
        properties.remove(key);
      }
    }
  }

  public void replaceProperty(String key, String value) {
    removeProperty(key);
    addProperty(key, value);
  }

  // 2/2003: If there's more than one value, return the LAST one, not the first.  --NH
  public String getProperty(String key) {
//     if (properties == null) // DEL
//       System.out.println("Synonym.getProperty: can't get prop " + key + "--properties is null for " + getName()); // DEL
//     else  // DEL
//       System.out.println("For synonym " + getName() + ", getProperty(" + key + ")=" + properties.get(key)); // DEL

    if ((properties == null) || (properties.get(key) == null)) {
      return "";
    } else {
      Vector values = (Vector) properties.get(key);
      //      return (String) values.elementAt(0);
      return (String) values.lastElement();
    }
  }

  // New 12/2002 CTW - supports multiple properties with same name.
  public Vector getPropertyMulti(String key) {
    if (properties == null)
      return null;
    else 
      return (Vector) properties.get(key);
  }

  // Modified 12/2002 CTW - returns hash with only one string for each key.
  public Hashtable getProperties() {
    if (properties == null)
      properties = new Hashtable(1, 1.0F);
    Hashtable hash = new Hashtable(1, 1.0F);
    Enumeration e = properties.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      String value = (String) ((Vector) properties.get(key)).elementAt(0); 
      hash.put(key, value);
    }
    return hash;
  }

  // New 12/2002 CTW - returns hash of property vectors.
  public Hashtable getPropertiesMulti() {
    if (properties == null)
      properties = new Hashtable(1, 1.0F);
    return properties;
  }

  public void setProperties(Hashtable props) {
    this.properties = props;
  }


  public String toString() {
    return getName();
  }
}
