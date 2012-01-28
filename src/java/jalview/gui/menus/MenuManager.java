package jalview.gui.menus;


import jalview.gui.*;

import java.awt.*;
import javax.swing.*;


public class MenuManager {
  AlignFrame frame;

  JMenuBar         menuBar;

  FileMenu        fileMenu;
  EditMenu        editMenu;
  FontMenu        fontMenu;
  ViewMenu        viewMenu;
  ColourMenu      colourMenu;
  CalcMenu        calcMenu;
  AlignMenu       alignMenu;
  HelpMenu        helpMenu;

  AlignViewport av;
  Controller    controller;

  public MenuManager(AlignFrame frame,AlignViewport av,Controller controller) {
    this.frame      = frame;
    this.av         = av;
    this.controller = controller;

    menuInit();
  }

  public void menuInit() {
    menuBar = new JMenuBar();


    fileMenu   = new FileMenu(frame,av,controller);
    viewMenu   = new ViewMenu(frame,av,controller);
    fontMenu   = new FontMenu(frame,av,controller);
    colourMenu = new ColourMenu(frame,av,controller);
    calcMenu   = new CalcMenu(frame,av,controller);
    alignMenu  = new AlignMenu(frame,av,controller);
    helpMenu   = new HelpMenu(frame,av,controller);

    menuBar.add(fileMenu);
    //if (!frame.getReadOnlyMode()) {
    // keeping edit menu in read only mode - some handy read only things
    editMenu   = new EditMenu(frame,av,controller);
    menuBar.add(editMenu);
    editMenu.setMnemonic('E');
    //}
    menuBar.add(fontMenu);
    menuBar.add(viewMenu);
    menuBar.add(colourMenu);
    menuBar.add(calcMenu);
    if (frame.showEverything()) {
      // I think these options work at sanger(?)
      menuBar.add(alignMenu);
      // Nothing comes up
      menuBar.add(helpMenu);
    }
    //menuBar.setHelpMenu(helpMenu);

    fileMenu.setMnemonic('F');
    fontMenu.setMnemonic('O');
    viewMenu.setMnemonic('V');
    colourMenu.setMnemonic('C');
    calcMenu.setMnemonic('L');
    alignMenu.setMnemonic('A');
    helpMenu.setMnemonic('H');

    frame.setJMenuBar(menuBar);
  }
}
