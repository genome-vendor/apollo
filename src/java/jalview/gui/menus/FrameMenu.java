package jalview.gui.menus;

import jalview.gui.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * Base class for Menu's in an AlignFrame.
 */
public abstract class FrameMenu extends JalMenu {
  protected AlignFrame frame;
  protected AlignViewport av;
  protected Controller    controller;

  public FrameMenu(String title,AlignFrame frame,AlignViewport av,Controller controller) {
    super(title,true);
    this.frame = frame;
    this.av    = av;
    this.controller = controller;

    init();
  }

  public Object getFrameParent() {
    //return frame.getAFParent();
    return frame;
  }

  protected AlignFrame getAlignFrame() { return frame; }

  public JMenuItem add(JMenuItem item) {
    return super.add(item);
  }


  protected abstract void init();

  public void setMnemonic(char c) {
    //System.out.println("setMnemonic not implemented");
  }

/**
 * Factory method for adding menu items created from actions.
 */
  public JMenuItem add(JalAction action) {
 
    if (av != null && av.getCommandLog() != null) {
      //action.addListener(av.getCommandLog());
      av.getCommandLog().add(action);
    }
    return super.add(action);
  }
}
