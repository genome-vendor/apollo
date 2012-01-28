package apollo.datamodel;

import java.util.*;

import org.apache.log4j.*;

public class Identifier implements java.io.Serializable, java.lang.Cloneable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Identifier.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String description;
  Vector synonyms;
  TreeMap<String, DbXref> dbXrefs;

  public Identifier(String description) {
    setDescription(description);
  }

  public void setDescription(String desc) {
    this.description = desc;
  }

  public String getDescription() {
    return this.description;
  }

  /** this returns all synonyms, both internal and external. internal synonyms have
   *the property "is_internal" set to 1 */
  public Vector getSynonyms() {
    return getSynonyms(false);
  }

  /** This used to return a clone, for no good reason that anyone can recall, so
   *  now it doesn't. */
  public Vector getSynonyms(boolean excludeInternalSynonyms) {
    if (synonyms == null)
      return new Vector();
    else {
      if (!excludeInternalSynonyms)  // return all of them
        //        return new Vector(synonyms); // clone
        return synonyms;

      // If excludeInternalSynonyms==true, return only those that are NOT internal
      Vector syns = new Vector();
      for (int i = 0; i < synSize(); i++) {
        Synonym syn = (Synonym) synonyms.elementAt(i);
        if (!syn.getProperty("is_internal").equals("1"))
          syns.add(syn);
      }
      return syns;
    }
  }

  private int synSize() { 
    if (synonyms==null)
      return 0;
    return synonyms.size();
  }

  public void addSynonym(String syn) {
    addSynonym(synSize(), new Synonym(syn));
  }

  public void addSynonym(int index, String syn) {
    addSynonym(index, new Synonym(syn));
  }

  public void addSynonym(Synonym syn) {
    addSynonym(synSize(), syn);
  }

  public void addSynonym(int index, Synonym syn) {
    if (syn != null && syn.getName()!=null && !syn.getName().equals ("")) {
      if (synonyms == null) // Lazy initialization
        synonyms = new Vector();
      // don't duplicate synonym
      // 6/14/2005: For now, allow duplicate synonyms--just print a warning.
      //      if (!alreadyInList(syn, synonyms))
      alreadyInList(syn, synonyms);  // will print a warning if this synonym is already in list
      synonyms.add(index,syn);
    }
    else if (logger.isDebugEnabled()) {
      String m = "Syn not added as its null/empty "+syn;
      if (syn!=null) m += " name "+syn.getName();
      logger.debug(m);
    }
  }

  boolean hasSynonym(String name) {
    if (synonyms == null)
      return false;
    return alreadyInList(new Synonym(name),synonyms);
  }

  private boolean alreadyInList(Synonym syn, Vector synonyms) {
    String name = syn.getName();
    for (int i = 0; i < synonyms.size(); i++) {
      Synonym s = (Synonym) synonyms.elementAt(i);
      if (name.equals(s.getName())) {
        logger.debug("duplicate copies of synonym " + syn);
        return true;
      }
    }
    return false;
  }

  public void deleteSynonym(Synonym nym) {
    if (synonyms != null)
      synonyms.remove(nym);
  }

  public void deleteSynonym(String nym) {
    if (synonyms != null) {
      for (int i = 0; i < synonyms.size(); i++) {
        Synonym syn = (Synonym) synonyms.elementAt(i);
        if (syn.getName().equals(nym))
          synonyms.remove(syn);
      }
    }
  }

  public void clearSynonyms() {
    if (synonyms != null)
      synonyms.clear();
  }

  public void nullSynonyms() {
    synonyms = null;
  }

  public Vector getDbXrefs() {
    Vector xref_vect = new Vector();
    if (dbXrefs != null) {
      for (DbXref dbxref : dbXrefs.values()) {
        xref_vect.add(dbxref);
      }
    }
    return xref_vect;
  }

  public void addDbXref(DbXref dbxref) {
    if (dbxref != null) {
      String key = dbxref.getDbName() + dbxref.getIdValue();
      if (key != null && !key.equals("")) {
        if (dbXrefs == null)
          dbXrefs = new TreeMap<String, DbXref>();

        dbXrefs.put(key, dbxref);
      }
    }
  }

  public void deleteDbXref(DbXref dbxref) {
    if (dbXrefs != null && dbxref != null) {
      String key = dbxref.getDbName() + dbxref.getIdValue();
      if (key != null && !key.equals(""))
        dbXrefs.remove (key);
    }
  }

  public void nullDbXrefs() {
    dbXrefs = null;
  }

  public String toString() {
    return "[" + description + "][" +
           getSynonyms().toString() + "][" +
           getDbXrefs().toString();
  }

  public Object clone() {
    try {
      Identifier clone = (Identifier) super.clone();
      clone.nullSynonyms();
      for (int i = 0; i < getSynonyms().size(); i++) {
        Synonym syn = (Synonym) getSynonyms().elementAt(i);
        clone.addSynonym(syn.cloneSynonym());
      }

      // note, changes to the dbxrefs will be reflected in the original object!
      // dbxrefs are not truly cloned. For now this doesn't matter, because
      // they are not editable
      clone.nullDbXrefs();
      for (int i=0; i < getDbXrefs().size(); i++) {
        DbXref xref = (DbXref) getDbXrefs().elementAt(i);
        clone.addDbXref(new DbXref(xref.getIdType(), 
                                   xref.getIdValue(),
                                   xref.getDbName()));
      }
      return clone;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

}
