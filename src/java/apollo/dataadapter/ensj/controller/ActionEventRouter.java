package apollo.dataadapter.ensj.controller;
import java.awt.event.*;

import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;


/**
 * Generic class to forward action events onto central handler.
**/
public class ActionEventRouter 
extends EventRouter
implements ActionListener
{
  public ActionEventRouter(Controller handler, String key){
    super(handler, key);
  }
  
  public void actionPerformed(ActionEvent event){
    getHandler().handleEventForKey(getKey());
  }
}
