package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

public abstract class EventHandler {
  
  private Controller _controller;
  private String _key;
  public static final String NULL_VERSION = "(No Version)";
  
  
  public EventHandler(Controller controller, String key) {
    _controller = controller;
    _key = key;
  }
  
  protected Controller getController(){
    return _controller;
  }
  
  public abstract void doAction(Model model);

  public String getKey(){
    return _key;
  }
  
  public void doRead(){
    getController().doRead();
  }
  
  public void doUpdate(){
    getController().doUpdate();
  }
  
  public void log(String message){
    getController().log("EVENT:("+getKey()+"):"+message);
  }
  
  public int getRowCountForTable(String tableName, Connection connection){
    return getCountForQuery("select count(*) from " + tableName, connection);
  }

  public int getCountForQuery(String query, Connection connection){
    int rowCount = 0;
    try{
      java.sql.Statement statement = connection.createStatement();
      java.sql.ResultSet count = statement.executeQuery(query);

      while(count.next()){
        rowCount = count.getInt(1);
      }//end while
      
    }catch(java.sql.SQLException exception){
      throw new FatalException(exception.getMessage(), exception);
    }
    
    return rowCount;
  }

  public Connection getConnectionForModel(Model model){
    DatabaseModel databaseModel = model.getDatabaseModel();
    String jdbcDriver = EnsJConnectionUtil.DRIVER;
    
    String host = databaseModel.getHost();
    String port = databaseModel.getPort();
    String database = databaseModel.getSelectedEnsemblDatabase();
    
    //String url = "jdbc:mysql://" + host + ":" + port + "/"+database;
    String user = databaseModel.getUser();
    String password = databaseModel.getPassword();
    
    return EnsJConnectionUtil.getConnection(EnsJConnectionUtil.DRIVER, host, port, database, user, password);
  }
  
  
  public List findCoordinateSystems(Connection connection){
    java.util.List returnList = new ArrayList();
    try{
      
      java.sql.Statement statement = connection.createStatement();

      java.sql.ResultSet names = statement.executeQuery("select version, name from coord_system where attrib like '%default_version%'");
      String version;
      String name;
      String composite;

      while(names.next()){
        version = names.getString(1);
        if(isNull(version)){
          version = NULL_VERSION;
        }
        name = names.getString(2);
        composite = version + "--" + name;
        returnList.add(composite);
      }//end while

      names = statement.executeQuery("select version, name from coord_system where attrib not like '%default_version%'");

      while(names.next()){
        version = names.getString(1);
        if(isNull(version)){
          version = NULL_VERSION;
        }
        name = names.getString(2);
        composite = version + "--" + name;
        returnList.add(composite);
      }//end while
      
    }catch(java.sql.SQLException exception){
      throw new FatalException(exception.getMessage(), exception);
    }
    
    return returnList;
  }
  
  public List findSeqRegions(Model model){
    DatabaseModel databaseModel = model.getDatabaseModel();
    LocationModel locationModel = model.getLocationModel();

    String jdbcDriver = EnsJConnectionUtil.DRIVER;
    String host = databaseModel.getHost();
    String port = databaseModel.getPort();
    String database = databaseModel.getSelectedEnsemblDatabase();
    String user = databaseModel.getUser();
    String password = databaseModel.getPassword();
    
    String versionAndCoordSystem;
    Pattern pattern;
    Matcher matcher ;
    String version;
    String coordSystem;
    
    List returnList = new ArrayList();
    List unsortedReturnList = new ArrayList();
    
    returnList.add("");
    
    java.sql.Connection connection = 
      EnsJConnectionUtil.getConnection(
        jdbcDriver,
        host,
        port,
        database,
        user,
        password
      );
    
    java.sql.Statement statement;
    java.sql.ResultSet names;
    String name;
    int length;
    
    versionAndCoordSystem = locationModel.getSelectedCoordSystem();
    
    if(isNull(versionAndCoordSystem)){
      throw new NonFatalException("Coordinate system must be populated");
    }
    
    log("Found version/coord system: "+versionAndCoordSystem);
    
    //
    //See if we can split the selected coord-system string into a version and an actual coord system.
    pattern = Pattern.compile(FindSeqRegionsHandler.PATTERN);
    matcher = pattern.matcher(versionAndCoordSystem);
    if(!matcher.matches()){
      throw new NonFatalException(
        "The chosen version and coord system: "+versionAndCoordSystem+" don't fit the pattern"+
        " version--coordsystem"
      );
    }
    
    
    version = matcher.group(1);
    coordSystem = matcher.group(2);

    log("Matched to version : "+version+", and coord system: "+coordSystem);
    
    if(isNull(version) || isNull(coordSystem)){
      throw new NonFatalException(
        "The chosen version and coord system "+versionAndCoordSystem+" can't be resolved "+
        " into a separate, non-null version and coord system string"
      );
    }
    
    try{

      statement = connection.createStatement();
      if(version.equals(FindCoordSystemsHandler.NULL_VERSION)){
        
        names = 
          statement.executeQuery(
            "select seq_region.name, seq_region.length from seq_region, coord_system where "+
            " seq_region.coord_system_id = coord_system.coord_system_id and "+
            " coord_system.name = '"+coordSystem+"' and "+
            " coord_system.version is null "
          );
        
      }else{

        names = 
          statement.executeQuery(
            "select seq_region.name, seq_region.length from seq_region, coord_system where "+
            " seq_region.coord_system_id = coord_system.coord_system_id and "+
            " coord_system.name = '"+coordSystem+"' and "+
            " coord_system.version = '"+version+"'"
          );

      }

      while(names.next()){
        name = names.getString(1);
        length = names.getInt(2);
        unsortedReturnList.add(name);
        locationModel.getSeqRegionToLengthsMap().put(name, new Integer(length));
        log("Retreived seq-region: "+name+"("+length+")");
      }


    }catch(java.sql.SQLException exception){
      throw new FatalException(exception.getMessage(), exception);
    }
    
    Collections.sort(unsortedReturnList);
    returnList.addAll(unsortedReturnList);

    return returnList;
  }
  
  public String findCoordSystemForNamedSeqRegion(Model model){
    DatabaseModel databaseModel = model.getDatabaseModel();
    LocationModel locationModel = model.getLocationModel();

    String jdbcDriver = EnsJConnectionUtil.DRIVER;
    String host = databaseModel.getHost();
    String port = databaseModel.getPort();
    String database = databaseModel.getSelectedEnsemblDatabase();
    String user = databaseModel.getUser();
    String password = databaseModel.getPassword();

    String regionName = locationModel.getSelectedSeqRegion();
    
    String returnString = null;
    
    java.sql.Connection connection = 
      EnsJConnectionUtil.getConnection(
        jdbcDriver,
        host,
        port,
        database,
        user,
        password
      );
    
    java.sql.Statement statement;
    java.sql.ResultSet names;
    String name;
    String version;
    String attribs;
    
    try{

      statement = connection.createStatement();
      names = 
          statement.executeQuery(
            "select coord_system.name, coord_system.version, coord_system.attrib from seq_region, coord_system where "+
            " seq_region.coord_system_id = coord_system.coord_system_id and "+
            " seq_region.name = '" + regionName + "'"
          );

      while(names.next()){
        name = names.getString(1);
        version = names.getString(2);
        attribs = names.getString(3);
 
        if(version == null) {
          returnString = new String (FindCoordSystemsHandler.NULL_VERSION + "--" + name);
        } else {
          returnString = new String (version + "--" + name);
        }
        log("Retreived coord system: "+ name + " " + version);
        if (attribs.indexOf("default_version") != -1) {
          break;
        }
      }

    }catch(java.sql.SQLException exception){
      throw new FatalException(exception.getMessage(), exception);
    }
    
    return returnString;
  }

  public boolean isNull(String string){
    if(string == null || string.trim().length() <= 0){
      return true;
    }else{
      return false;
    }
  }
  
  public void closeConnection(Connection connection){
    try{
      connection.close();
    }catch(SQLException exception){
      throw new NonFatalException("Problem closing connection: "+exception.getMessage(), exception);
    }
  }
}
