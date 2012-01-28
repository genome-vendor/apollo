package apollo.dataadapter.mysql;

import java.util.*;

public class Table {
  Vector     fieldNames;
  String     name;
  
  public Table() {
      fieldNames = new Vector();
  }
  public Table(String name) {
    this();
    this.name = name;
  }  
  public void setName(String name) {
      this.name = name;
  }
  public String getName() {
      return name;
  }
  
  public void addFieldName(String name) {
    if (!fieldNames.contains(name)) {
        fieldNames.addElement(name);
    }
  }
  public Vector getFieldNames() {
      return fieldNames;
  }
}
