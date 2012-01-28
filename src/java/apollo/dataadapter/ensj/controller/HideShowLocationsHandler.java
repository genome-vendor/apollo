package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * Declare the types panel (as represented by the model) to be either
 * showing or not, and refresh!
**/
public class HideShowLocationsHandler extends EventHandler{
  
  public HideShowLocationsHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  
  public void doAction(Model model){
    doUpdate();
    
    if(model.isLocationsPanelVisible()){
      model.setLocationsPanelVisible(false);
    }else{
      model.setLocationsPanelVisible(true);
    }
    
    doRead();
  }
}
