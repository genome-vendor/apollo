package apollo.dataadapter.mysql;

import java.sql.*;
import java.util.*;

import apollo.datamodel.*;

import org.apache.log4j.*;

public class MySQLDatabase extends Database {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(MySQLDatabase.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  Connection conn;
  
  public MySQLDatabase(String host,String name,String user,String pass,int port) {
    super(host,name,user,pass,port);
    connect();
  }

  public MySQLDatabase(String host,String name,String user,String pass) {
    super(host,name,user,pass,3306);
    connect();
  }

  public MySQLDatabase(Database d) {
    this(d.getHost(),d.getName(),d.getUser(),d.getPass());
  }
  
  public Connection connect() {
    try {
      Class.forName("org.gjt.mm.mysql.Driver").newInstance();
    } catch (ClassNotFoundException e) {
      logger.error("Exception: " + e.toString(), e);
    } catch (InstantiationException e) {
      logger.error("Exception: " + e.toString(), e);
    } catch (IllegalAccessException e) {
      logger.error("Exception: " + e.toString(), e);
    }
	
    try { 
      String url = "jdbc:mysql://" + host + ":" + port + "/" + name;
      logger.info("connecting to Database URL = " + url + " " + user  + " " + pass);
      conn = DriverManager.getConnection(url,user,pass);
    } catch (SQLException e) {
      logger.error("SQLException " + e, e);
    }
    return null;
  }
  
  public Vector getTables() {
    
    String query = "show tables";
    
    ResultSet rs = query(query);
    
    try {
      while (rs.next()) {
        logger.info(" - " + rs.getString(1));
        String tablestr = rs.getString(1);
        Table table = new Table(tablestr);
        addTable(table);
      }
        
      return tables;
	    
    } catch (SQLException e) {
      logger.error("SQLException e" + e, e);
    }
    return null;
  }

  public Vector getFieldNamesByTable(Table t) {
    String    query = "describe " + t.getName();
    ResultSet rs    = query(query);
    
    try {
      while (rs.next()) {
      
        String name = rs.getString(1);
        t.addFieldName(name);        
      }
        
      return t.getFieldNames();
        
    } catch (SQLException e) {
      logger.error("SQLException e" + e, e);
    }
   
    return null;
  }

  public ResultSet query(String query) {
    try {
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery(query);
      return rs;
    } catch (SQLException e) {
      logger.error("SQL Exception " + e, e);
      return null;
    }
  }
}
