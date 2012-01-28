package apollo.gui.menus;

import apollo.gui.*;
import apollo.gui.event.*;
import apollo.gui.detailviewers.exonviewer.BaseFineEditor;
import apollo.gui.synteny.CurationManager;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;

public class WindowMenu extends JMenu {
  ApolloFrame frame;

  public WindowMenu(ApolloFrame frame) {
    super("Window");
    this.frame = frame;
    addMenuListener(new WindowMenuListener());
  }

  private class WindowMenuListener implements MenuListener {

    public void menuCanceled(MenuEvent e) {
      removeAll(); // what causes a menuCancel?
    }
    
    /** Remove all menu items on deselection clears dangling ref to windows
	(mem leak) and the menu items get remade on selection */
    public void menuDeselected(MenuEvent e) {
      removeAll();
    }

    public void menuSelected(MenuEvent e) {
      menuInit();
    }
  }

  private class MenuActionListener implements ActionListener {
    Window w;

    public MenuActionListener(Window w) {
      this.w = w;
    }

    public void actionPerformed(ActionEvent e) {
      w.toFront();
      // if iconified frame, deiconify
      if (w instanceof Frame) {
        Frame f = (Frame)w;
        if (f.getState() == Frame.ICONIFIED)
          f.setState(Frame.NORMAL);
      }
    }
  }

  /** Gets windows from all curation controllers. */
  public void menuInit() {
    removeAll();
    //Vector windows = frame.getController().getWindowList(frame);
    Vector windows = Controller.getMasterController().getWindowList(frame);
    // add all curation controller windows
    CurationManager cm = CurationManager.getCurationManager();
    for (int i=0; i<cm.numberOfCurations(); i++) 
      windows.addAll(cm.getCurationController(i).getWindowList(frame));

    int nAdded = 0;
    int winCount = 1;
    for(int i=0; i < windows.size(); i++) {
      Window win = (Window) windows.elementAt(i);
      String title;
      if (win.isVisible()) {
        if (win instanceof Dialog)
          title = ((Dialog) win).getTitle();
        else if (win instanceof Frame)
          title = ((Frame) win).getTitle();
        else
          title = "Window "+(winCount++);

        JMenuItem item = new JMenuItem(title);

        if (win instanceof BaseFineEditor)
          item.setForeground(((BaseFineEditor) win).getIndicatorColor());

        item.addActionListener(new MenuActionListener(win));
        add(item);
        nAdded++;
      }
    }
    if (nAdded == 0) {
      JMenuItem item = new JMenuItem("<no open windows>");
      item.setEnabled(false);
      add(item);
      return;
    }
  }
}
