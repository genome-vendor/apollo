package apollo.datamodel;


public class DbXref implements java.io.Serializable {
  String idtype;
  String idvalue;
  String dbname;
  String version;
  short current;  // Used by Chado
  String description;
  // isPrimary and isSecondary are used by the ChadoXML adapter.
  // isPrimary or isSecondary could both be true (because often the primary
  // dbxref appears again in the list of feature_dbxrefs), or just one could be true.
  // isPrimary==true if this is the primary dbxref for its feature.
  boolean isPrimary = false;
  // isSecondary==true if this is a feature_dbxref.
  boolean isSecondary = true;
  // isOntology flags this DbXref as an ontology term (GFF3 supports this)
  boolean isOntology = false;

  public DbXref(String type,String value,String db) {
    this(type, value, db, false);
  }

  public DbXref(String type, String value, String db, boolean isOntology)
  {
    setIdType (type);
    setIdValue(value);
    setDbName (db);
    setIsOntology(isOntology);
  }

  public void setIdType(String type) {
    this.idtype = type;
  }
  public void setVersion(String ver){
    this.version=ver;
  }
  public String getVersion(){
    return this.version;
  }
  public String getIdType() {
    return this.idtype;
  }
  public void setIdValue(String value) {
    this.idvalue = value;
  }
  public String getIdValue() {
    return this.idvalue;
  }
  public void setDbName(String db) {
    this.dbname = db;
  }
  public String getDbName() {
    return this.dbname;
  }
  public void setDescription(String descr) {
    this.description = descr;
  }
  public String getDescription() {
    return this.description;
  }
  public void setCurrent(int val) {
    this.current = (short)val;
  }
  public short getCurrent() {
    return this.current;
  }
  public void setIsPrimary(boolean value) {
    isPrimary = value;
  }
  public boolean isPrimary() {
    return isPrimary;
  }
  public void setIsSecondary(boolean value) {
    isSecondary = value;
  }
  public boolean isSecondary() {
    return isSecondary;
  }
  public void setIsOntology(boolean value)
  {
    isOntology = value;
  }
  public boolean isOntology()
  {
    return isOntology;
  }
}
