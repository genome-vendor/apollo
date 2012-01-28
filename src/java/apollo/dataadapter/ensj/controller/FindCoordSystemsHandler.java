package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.util.*;

/**
 * Find the coord systems (with their versions concatenated on) for the user
 * to choose from. Once we've found the coord systems, we clear out the old seq-regions
 * and declare them to be not initialised.
**/
public class FindCoordSystemsHandler extends EventHandler{
  
  public FindCoordSystemsHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    //
    //Get the db parameters into the model.
    doUpdate();
    
    DatabaseModel databaseModel = model.getDatabaseModel();
    LocationModel locationModel = model.getLocationModel();

    if(locationModel.isCoordSystemInitialised()){
      log("Coord systems already initialised - returning");
      return;
    }
    
    log("Finding available coord systems");

    List returnList = new ArrayList();
    returnList.add("");
    
    String jdbcDriver = EnsJConnectionUtil.DRIVER;
    
    String host = databaseModel.getHost();
    String port = databaseModel.getPort();
    String database = databaseModel.getSelectedEnsemblDatabase();
    
    String url = "jdbc:mysql://" + host + ":" + port + "/"+database;
    String user = databaseModel.getUser();
    String password = databaseModel.getPassword();
    
    java.sql.Connection connection = 
      EnsJConnectionUtil.getConnection(
        jdbcDriver,
        host,
        port,
        database,
        user,
        password
      );

    if(password == null){
      password = "";
    }

    returnList.addAll(findCoordinateSystems(connection));
      
    locationModel.setCoordSystems(returnList);
    
    locationModel.setCoordSystemInitialised(true);
    
    locationModel.setSeqRegions(new ArrayList());
    locationModel.setSeqRegionInitialised(false);
    
    locationModel.setStableIDLocation(false);
    
    doRead();
  }
}
