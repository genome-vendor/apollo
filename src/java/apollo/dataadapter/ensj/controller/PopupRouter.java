package apollo.dataadapter.ensj.controller;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;
import apollo.dataadapter.ensj.view.*;

public class PopupRouter extends EventRouter implements PopupMenuListener
{
  public PopupRouter(Controller handler, String key){
    super(handler, key);
  }

  public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
    getHandler().handleEventForKey(getKey());
  }
  
  public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
  }
  
  public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
  }
  
}
