package apollo.dataadapter.mysql;

import java.sql.*;

public class BaseAdaptor {

  protected MySQLDatabase db;
  
  public BaseAdaptor() {}
  
  public BaseAdaptor(MySQLDatabase db) {
      setDatabase(db);
  }
 
 
 public void setDatabase(MySQLDatabase db) {
    this.db = db;
 }
 public MySQLDatabase getDatabase() {
    return db;
 }
}
