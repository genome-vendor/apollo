package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * The user has typed into the stable id field, so we clear out
 * the seq-region information they might have entered.
**/
public class UseStableIDLocationHandler extends EventHandler{
  
  public UseStableIDLocationHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  public void doAction(Model model){
    if(model.getLocationModel().isStableIDLocation()){
      return;
    }

    doUpdate();
    
    model.getLocationModel().setStableIDLocation(true);
    
    model.getLocationModel().setSelectedCoordSystem(null);
    model.getLocationModel().setSelectedSeqRegion(null);
    model.getLocationModel().setStart(null);
    model.getLocationModel().setEnd(null);
    
    doRead();
  }
}
