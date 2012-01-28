package apollo.dataadapter.ensj.controller;

import java.util.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * The user has selected a new coordinate system, so I flush the known seq-regions
 * and update.
**/
public class ClearSeqRegionsHandler extends EventHandler{
  
  public ClearSeqRegionsHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    doUpdate();
    
    if(!model.getLocationModel().isSeqRegionInitialised()){
      log("Returning - seq regions not yet initialised");
      return;
    }
    
    model.getLocationModel().setSeqRegions(new ArrayList());
    model.getLocationModel().setSelectedSeqRegion(null);
    model.getLocationModel().setSeqRegionInitialised(false);
    
    model.getLocationModel().setStart(null);
    model.getLocationModel().setEnd(null);
    
    model.getLocationModel().setSeqRegionInitialised(false);
    
    doRead();
  }
}
