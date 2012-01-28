package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * Declare the types panel (as represented by the model) to be either
 * showing or not, and refresh!
**/
public class HideShowDatabasesHandler extends EventHandler{
  
  public HideShowDatabasesHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  
  public void doAction(Model model){
    doUpdate();
    
    if(model.isDatabasePanelVisible()){
      model.setDatabasePanelVisible(false);
    }else{
      model.setDatabasePanelVisible(true);
    }
    
    doRead();
  }
}
