package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * The user has typed into the stable id field, so we clear out
 * the seq-region information they might have entered.
**/
public class UseSeqRegionLocationHandler extends EventHandler{
  
  public UseSeqRegionLocationHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    if(!model.getLocationModel().isStableIDLocation()){
      //      System.out.println("Returning - already set to be seq region location");
      return;
    }
    
    doUpdate();
    
    model.getLocationModel().setStableIDLocation(false);
    
    model.getLocationModel().setStableID(null);
    
    doRead();
  }
}
