package apollo.dataadapter.ensj;

import apollo.dataadapter.ensj.*;
import java.sql.*;
import java.util.*;

import org.ensembl.util.*;

public class EnsJConnectionUtil {
  public static final String DRIVER = "org.gjt.mm.mysql.Driver";

  
  public static Connection getConnection(
    String jdbcDriver,
    String host,
    String port,
    String database,
    String user,
    String password
  ){
    
    Connection connection;
    
    if(isNull(jdbcDriver)){
      throw new NonFatalException("Driver must be supplied");
    }
    
    jdbcDriver = jdbcDriver.trim();
    
    if(isNull(host)){
      throw new NonFatalException("Host must be supplied");
    }
    
    host = host.trim();

    if(isNull(port)){
      throw new NonFatalException("Port must be supplied");
    }

    port = port.trim();
    
    if(isNull(user)){
      throw new NonFatalException("User must be supplied");
    }
    
    user = user.trim();

    if(password != null){
      password = password.trim();
    }
    
    if(isNull(password)){
      password = null;
    }
    //log("PASSWORD: "+password);

    String url;

    if(isNull(database)){
      url = "jdbc:mysql://" + host + ":" + port+"/";
    }else{
      url = "jdbc:mysql://" + host + ":" + port + "/"+database;
    }

    url = url.trim();
    //log("Trying to make connection with URL : "+url);

    try{
      Class.forName(jdbcDriver).newInstance();
    }catch(IllegalAccessException exception){
      throw new FatalException(
        "Cannot access the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }catch(InstantiationException exception){
      throw new FatalException(
        "Cannot create the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }catch(ClassNotFoundException exception){
      throw new FatalException(
        "Cannot find the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }//end try

    try{
      if(password != null){
        connection = java.sql.DriverManager.getConnection(url,user,password); 
      }else{
        connection = java.sql.DriverManager.getConnection(url,user,null); 
      }
    }catch(java.sql.SQLException exception){
      //throw new NonFatalException("Problem creating db connection: "+exception.getMessage(), exception);
      throw new NonFatalException("Problem creating db connection for " + url, exception);
    }
    
    return connection;
  }
  
  private static boolean isNull(String string){
    if(string == null || string.trim().length() <= 0){
      return true;
    }else{
      return false;
    }
  }

  // Hacky method to figure out what schema this db is - for transition from 20+ to 32+
  public static int getEnsemblSchema(Connection connection) {
    String dbsql = "describe gene";
    int schema = 30;

    ResultSet dbresults;
    try{
      dbresults = connection.createStatement().executeQuery(dbsql);
      while(dbresults.next()){
        if (dbresults.getString(1).equals("confidence") ) {
          schema = 32;
        }
        if (dbresults.getString(1).equals("status")) {
          schema = 34;
        }
        dbresults = connection.createStatement().executeQuery("select meta_value from meta where meta_key = 'schema_version' order by meta_value");
        while(dbresults.next()){
          String schemaStr = dbresults.getString(1);
          if (!(schemaStr.indexOf("evision")>=0)) {
            schema = dbresults.getInt(1);
          }
        }
      }
    }catch(SQLException exception){
      throw new NonFatalException(exception.getMessage(), exception);
    }

    return schema;
  }
}
