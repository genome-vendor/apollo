package apollo.dataadapter.mysql;

import java.util.*;

public class Database {
  
  protected String host;
  protected String name;
  protected String user;
  protected String pass;
  protected String path;
  protected int    port;

  protected Vector tables;
  
  public Database(String host,String name,String user,String pass) {
      this(host,name,user,pass,3306);
  }
  public Database(String host,String name,String user,String pass,int port) {
    
    tables = new Vector();
    
    setHost(host);
    setName(name);
    setUser(user);
    setPass(pass);
    setPort(port);
  }
  
  public void addTable(Table t) {
    if (!tables.contains(t)) {
        tables.addElement(t);
    }
  }
  
  public Vector getTables() {
      return tables;
  }
        
  public void setHost(String host) {
    this.host = host;
  }
  public void setName(String name) {
    this.name = name;
  }
  public void setUser(String user) {
    this.user = user;
  }
  public void setPass(String pass) {
    this.pass = pass;
  }
  public void setPort(int port) {
    this.port = port;
  }
  public void setPath(String path) {
    this.path = path;
  }
  
  public String getHost() {
    return host;
  }
  public String getName() {
    return name;
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
  public String getPath() {
    return path;
  }
}

  
    
