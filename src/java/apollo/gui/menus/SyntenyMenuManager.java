package apollo.gui.menus;


import apollo.gui.*;
import apollo.gui.synteny.SyntenyPanel;

import javax.swing.*;

/** This is no longer used - delete? - used to be used by SyntenyLinkMenu */
public class SyntenyMenuManager {
  JFrame frame;

  JMenuBar        menuBar;
  SyntenyViewMenu        viewMenu;
  SyntenyPanel sp;

  public SyntenyMenuManager(JFrame frame, SyntenyPanel sp) {
    this.frame = frame;
    this.sp = sp;
    menuInit();
  }


  public void menuInit() {
    menuBar = new JMenuBar();

    frame.setJMenuBar(menuBar);

    viewMenu = new SyntenyViewMenu(frame,sp);

    menuBar.add(viewMenu);

    viewMenu.setMnemonic('V');

  }
}


