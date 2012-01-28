package apollo.gui;

import javax.swing.*;
import java.awt.*;

import apollo.util.*;

/**
 * An extension of JPanel to make automatic Controller window management possible.
 I think what happens is Controller queries a ControlledObject with 
 getControllerWindow, which ControlledPanel implements. It then adds the 
 ControlledPanel as a child to ControlledWindow, which is a wrapper of 
 getControllerWindow Window. ControlledWindow removes all of its children as listeners
 to the controller on a window close.
 Shouldnt this implement ControlledObjectI - its basically a panel that implements
 ControlledObjectI - would you ever wanna be a ControlledPanel that is not a 
 ControlledObjectI? */

public abstract class ControlledPanel extends JPanel {

  public abstract Controller getController();
  public abstract void setController(Controller c);

  /** override Component.addNotify, called when added to parent. For many 
   *ControlledObjects this is when they add themselves as listeners to the
   controller. At add time can find ancestor of self for window control, I assume
   this is why its done here. Not a big deal to add to same controller twice as 
   Controller checks for that */
  public void addNotify() {
    super.addNotify();
    // System.out.println("Add notify");
    setController(getController()); 
  }

  /** Why wouldnt this return Window? why Object? */
  public Object getControllerWindow() {
    return SwingMissingUtil.getWindowAncestor((Component)this);
  }

  public boolean needsAutoRemoval() {
    return true;
  }
}
