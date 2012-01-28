package apollo.dataadapter.ensj.controller;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

/**
 * Ask the Controller to make the view do a read.
**/
public class UpdateHandler extends EventHandler{
  
  public UpdateHandler(Controller controller, String key) {
    super(controller, key);
  }
  
  
  public void doAction(Model model){
    log("Start update");
    getController().doUpdate();
    log("Finished update");
  }
}
