package apollo.dataadapter.mysql;

import apollo.datamodel.*;

import java.sql.*;
import java.util.*;

import org.apache.log4j.*;

public class MySQLInstance {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(MySQLInstance.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
    
  Hashtable databases;
    
  String host;
  String user;
  String pass;
  String name = "mysql";
  int    port = 3306;
  String url;
    
  Connection conn;
    
  public MySQLInstance(String host,String user, String pass,int port) {

    databases = new Hashtable();
	
    setHost(host);
    setUser(user);
    setPass(pass);
    setPort(port); 
	
    connect();
	
  }
  public MySQLInstance(String host,String user, String pass) {
    this(host,user,pass,3306);
  }

  public Vector fetchAllDatabases() {
    try {
      
      String    query = "show databases";
      ResultSet rs    =  query(query);
      
      while (rs.next()) {
        String        db       = rs.getString(1);
        Database      dbobj    = new Database(getHost(),db,getUser(),getPass(),getPort());
        MySQLDatabase database = new MySQLDatabase(dbobj);
        
        databases.put(db,database);
        
      }
    } catch (SQLException e) {
      logger.error("SQLException " + e, e);
    }
	
    Vector      dbs = new Vector();
    Enumeration en  = databases.elements();

    while (en.hasMoreElements()) {
      dbs.addElement(en.nextElement());
    }
    return dbs;
  }
    
  public MySQLDatabase fetchDatabaseByName(String name) {
    
    if (databases.get(name) != null) {
      MySQLDatabase db = (MySQLDatabase)databases.get(name);
      return db;
    }

    MySQLDatabase database = new MySQLDatabase(getHost(),name,getUser(),getPass(),getPort());
	
    databases.put(name,database);
	
    return database;
	
  }

  public Vector getDatabaseNames() {
    Vector names = new Vector();
    
    try {
	    
      String    query = "show databases";
      ResultSet rs    =  query(query);
	    
      while (rs.next()) {
        String db = rs.getString(1);
        names.addElement(db);  
      }
    } catch (SQLException e) {
      logger.error("SQLException " + e, e);
    }
    return names;
  }
    
  
  public void setHost(String host) {
    this.host = host;
  }
  public void setUser(String user) {
    this.user = user;
  }
  public void setPass(String pass) {
    this.pass = pass;
  }
  public void setURL(String url) {
    this.url = url;
  }
  public void setPort(int port) {
    this.port = port;
  } 
  public String getHost() {
    return host;
  }
  public String getUser() {
    return user;
  }
  public String getPass() {
    return pass;
  }
  public int getPort() {
    return port;
  }
  public String getURL() {
    return url;
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
      url = "jdbc:mysql://" + host + ":" + getPort() + "/" + name;
      logger.info("Connecting to Database URL = " + url + " " + user  + " " + pass);
      conn = DriverManager.getConnection(url,user,pass);
      logger.info("Connected to database");
    } catch (SQLException e) {
      logger.error("SQLException " + e, e);
    }
    return null;
  }
  
  public static void main(String[] args) {
    MySQLInstance mysql = new MySQLInstance(args[0],args[1],args[2],Integer.parseInt(args[3]));
    
    try {     
      Vector dbs = mysql.fetchAllDatabases();

      for (int i = 0; i < dbs.size(); i++) {
        MySQLDatabase db = (MySQLDatabase)dbs.elementAt(i);
        System.out.println("Got database");
        Vector tables = db.getTables();
	    
        for (int j = 0; j < tables.size(); j++) {
		
          Table table = (Table)tables.elementAt(j);
		
          System.out.println("  - Table " + table.getName());
		
          Vector names = db.getFieldNamesByTable(table);
		
          for (int k = 0;k < names.size(); k++) {
		    
            System.out.println("    - " + names.elementAt(k));
          }
        }
      }
    } catch (Exception e) {
      System.out.println("ERROR: fetching details for database " + args[3] + " " + e);
    }
  }
}
