package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;
import java.util.*;
import java.util.regex.*;

/**
 * Find the coord systems (with their versions concatenated on) for the user
 * to choose from
**/
public class FindSeqRegionsHandler extends EventHandler{
  public static final String PATTERN = "([^--]*)--(.*)";
  
  public FindSeqRegionsHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    
    log("Finding available seq regions");

    //
    //Get the db parameters into from the view  the model.
    doUpdate();
    
    DatabaseModel databaseModel = model.getDatabaseModel();
    LocationModel locationModel = model.getLocationModel();

    if(locationModel.isSeqRegionInitialised()){
      log("Seq regions already initialised - returning");
      return;
    }
    
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
    pattern = Pattern.compile(PATTERN);
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

    locationModel.setSeqRegions(returnList);
    locationModel.setSeqRegionInitialised(true);
    
    closeConnection(connection);
    
    doRead();
  }
}
