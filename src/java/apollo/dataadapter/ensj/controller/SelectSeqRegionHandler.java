package apollo.dataadapter.ensj.controller;

import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * The user has selected a new seq-region. I insert the default values 
 * into the start/end text fields according to the cached seq-region length.
**/
public class SelectSeqRegionHandler extends EventHandler{
  
  public SelectSeqRegionHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    doUpdate();
    LocationModel locationModel = model.getLocationModel();

    String length;
    String selectedSeqRegion = locationModel.getSelectedSeqRegion();
    
    if(!isNull(selectedSeqRegion)){

      // Set an appropriate coord system if none set in interface
      //
      if (isNull(locationModel.getSelectedCoordSystem())) {
        if(!locationModel.isCoordSystemInitialised()){
          log("Coord systems not initialised - finding and populating");
          locationModel.setCoordSystems(findCoordinateSystems(getConnectionForModel(model)));
          locationModel.setCoordSystemInitialised(true);
          locationModel.setSeqRegions(new ArrayList());
          locationModel.setSeqRegionInitialised(false);
        }
        String csString = findCoordSystemForNamedSeqRegion(model);
        if (csString != null) {
          locationModel.setSelectedCoordSystem(csString);
          findSeqRegions(model);
        }
      }

      // Try to get the length
      //
      length = String.valueOf(locationModel.getSeqRegionToLengthsMap().get(selectedSeqRegion));

      // If length came back null, must mean coord system was already set in interface, but
      // seq region wasn't found in it. Try to find the appropriate coord system for the
      // named seq region and set it.
      //
      if (length == null || length.equals("null")) {
        if(!locationModel.isCoordSystemInitialised()){
          log("Coord systems not initialised - finding and populating");
          locationModel.setCoordSystems(findCoordinateSystems(getConnectionForModel(model)));
          locationModel.setCoordSystemInitialised(true);
          locationModel.setSeqRegions(new ArrayList());
          locationModel.setSeqRegionInitialised(false);
        }

        // Set coord system to something sensible
        String csString = findCoordSystemForNamedSeqRegion(model);
        if (csString != null) {
          locationModel.setSelectedCoordSystem(csString);
  
          // Now coord system is set we can fill the seq region cache, so we can get the
          // length
          findSeqRegions(model);
          length = String.valueOf(locationModel.getSeqRegionToLengthsMap().get(selectedSeqRegion));
        }
      }
      model.getLocationModel().setStart("1");
      model.getLocationModel().setEnd(length);
    }
    
    doRead();
  }
}
