package apollo.gui.menus;


import apollo.gui.*;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.CurationManager;

import javax.swing.*;


public class MenuManager {
  ApolloFrame frame;

  private JMenuBar        menuBar;
  FileMenu        fileMenu;
  EditMenu        editMenu;
  public ViewMenu        viewMenu;
  TiersMenu       tiersMenu;
  TranscriptMenu  transcriptMenu;
  WindowMenu      windowMenu;
  BookmarkMenu    bookmarkMenu;
  LinksMenu       linksMenu;
  HelpMenu        helpMenu;
  AnalysisMenu    analysisMenu;
  SyntenyChoiceMenu     syntenyMenu;

  /** Where synteny menu is placed */
  private final static int syntenyMenuPosition = 8;

  private static MenuManager menuManagerSingleton;

  public static MenuManager createMenuManager(ApolloFrame af) {
    menuManagerSingleton = new MenuManager(af);
    return menuManagerSingleton;
  }
  
//   public static MenuManager getMenuManagerInstance() {
//     return menuManagerSingleton;
//   }

  private MenuManager(ApolloFrame frame) {
    this.frame = frame;
    menuInit();
    new MenuManagerDataLoadListener();
  }

  private void menuInit() {
    menuBar = new JMenuBar();

    frame.setJMenuBar(menuBar);

    fileMenu = new FileMenu(frame);
    editMenu = new EditMenu(frame);
    viewMenu = new ViewMenu(frame);
    tiersMenu = new TiersMenu();//frame);
    analysisMenu = new AnalysisMenu();
    transcriptMenu = new TranscriptMenu(frame);
    windowMenu = new WindowMenu(frame);
    bookmarkMenu = new BookmarkMenu();
    helpMenu = new HelpMenu(null);
    linksMenu = LinksMenu.getLinksMenu();

    menuBar.add(fileMenu);
    menuBar.add(editMenu);
    menuBar.add(viewMenu);
    menuBar.add(tiersMenu);
    menuBar.add(analysisMenu);

    menuBar.add(bookmarkMenu);
    bookmarkMenu.setMnemonic('B');
    menuBar.add(transcriptMenu);
    transcriptMenu.setMnemonic('N');

    menuBar.add(windowMenu);
    if (CurationManager.getCurationManager().isMultiCuration()) {
      insertSyntenyMenu();
    }
    menuBar.add(linksMenu);
    menuBar.add(helpMenu);

    fileMenu.setMnemonic('F');
    editMenu.setMnemonic('E');
    viewMenu.setMnemonic('V');
    tiersMenu.setMnemonic('T');
    //    transcriptMenu.setMnemonic('T');
    windowMenu.setMnemonic('W');
    linksMenu.setMnemonic('L');
    helpMenu.setMnemonic('H');
  }

  public void setHaveSequence(boolean haveSequence) {
    viewMenu.setHaveSequence(haveSequence);
  }
  
  public SyntenyChoiceMenu getSyntenyChoiceMenu(){
    return syntenyMenu;
  }

  private void insertSyntenyMenu() {
    if (syntenyMenuVisible()) {
      return;
    }
    syntenyMenu = new SyntenyChoiceMenu((CompositeApolloFrame)frame);
    //menuBar.add(syntenyMenu);
    menuBar.add(syntenyMenu,syntenyMenuPosition);
    menuBar.validate(); // menuBar needs to do a layout to bring in new menu
  }
  private void removeSyntenyMenu() {
    if (!syntenyMenuVisible()) {
      return;
    }
    menuBar.remove(syntenyMenuPosition);
    menuBar.validate(); // force layout to rid menu
  }
  private boolean syntenyMenuVisible() {
    if (syntenyMenu == null) return false;
    return menuBar.getMenu(syntenyMenuPosition) == syntenyMenu;
  }


  /** Change synteny menu visibility on region change events whether frame
      is multi species */
  private class MenuManagerDataLoadListener implements DataLoadListener {

    private MenuManagerDataLoadListener() {
      Controller.getMasterController().addListener(this);
    }

    public boolean handleDataLoadEvent(DataLoadEvent rce) {
      // if data retrieval is not done we arent interested (no apollo data yet)
      if (!rce.dataRetrievalDone())
        return false;
      // apollo data can still be null on a species downsizing
      //if (rce.getApolloData() != null && rce.getApolloData().isMultiSpecies()) {
      // CurationManager now updated before RCE fired
      if (CurationManager.getCurationManager().isMultiCuration()) {
        insertSyntenyMenu(); // multi species
      }
      else { // single species
        removeSyntenyMenu();
      }
      return true;
    }

  } // end of MenuManagerDataLoadListener inner class

}
