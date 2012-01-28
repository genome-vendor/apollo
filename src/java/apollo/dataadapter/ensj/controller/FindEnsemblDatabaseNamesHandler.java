package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.util.*;

/**
 * Find the coord systems (with their versions concatenated on) for the user
 * to choose from
**/
public class FindEnsemblDatabaseNamesHandler extends EventHandler{
  
  public FindEnsemblDatabaseNamesHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    //
    //Get the db parameters into the model.
    doUpdate();
    
    DatabaseModel databaseModel = model.getDatabaseModel();

    log("Finding available available databases");

    List returnList = new ArrayList();
    returnList.add("");

    String jdbcDriver = EnsJConnectionUtil.DRIVER;

    String host = databaseModel.getHost();
    String port = databaseModel.getPort();

    String user = databaseModel.getUser();
    String password = databaseModel.getPassword();
    
    java.sql.Connection connection = 
      EnsJConnectionUtil.getConnection(
        jdbcDriver,
        host,
        port,
        null,
        user,
        password
      );

    try{
      java.sql.Statement statement = connection.createStatement();
      java.sql.ResultSet names = statement.executeQuery("show databases");
      String name;

      while(names.next()){
        name = names.getString(1);
        log("Adding database: "+name);
        returnList.add(name);
      }//end while
      
    }catch(java.sql.SQLException exception){
      throw new FatalException(exception.getMessage(), exception);
    }

    databaseModel.setEnsemblDatabases(returnList);
    databaseModel.setDatabaseListFound(true);
    
    doRead();
  }
}
