package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.synteny.CurationManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.apache.log4j.*;

public class EditMenu extends JMenu implements ActionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(EditMenu.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  ApolloFrame frame;

  JMenuItem undo;
  JMenuItem redo;
  JMenuItem find;
  //JMenuItem prefs;
  JMenuItem newPrefs;
  //JMenuItem types;
  //  JMenuItem proxy;

  JDialog findDialog;
  //PreferencesDialog prefsDialog;
  //TypesWizard typesWizard;
  //  ProxyDialog proxyDialog;


  public EditMenu(ApolloFrame frame) {
    super("Edit");
    this.frame = frame;
    menuInit();
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
  }

  public void menuInit() {
    undo    = new JMenuItem("Undo");
    redo    = new JMenuItem("Redo");
    find = new JMenuItem("Find...");
    //prefs = new JMenuItem("Preferences (old)");
    newPrefs = new JMenuItem("Preferences");
    //    proxy = new JMenuItem("Proxy settings");

    undo.setEnabled(true);
    //undo.setEnabled(true);
    redo.setEnabled(false);

    // Undo and redo are not going to be implemented any time soon, so it's just a 
    // tease to put them on the menu.
    if (Config.isEditingEnabled()) {
	    add(undo);
	    undo.addActionListener(this);
	    undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
	                       		ActionEvent.CTRL_MASK));
//       add(redo);
//       addSeparator();
    }

    // Here are the only things that are really on the menu now:  Find and Preferences
    add(find);
    addSeparator();
    //add(prefs);
    add(newPrefs);
    //add(types);
    addSeparator();
    JMenuItem lafTitle = new JMenuItem("Look & Feel:");
    lafTitle.setForeground(Color.blue);
    //lafTitle.setEnabled(false);
    add(lafTitle);
    addLAFMenu();

    // Don't need Proxy Settings here--can get to it from Ensembl data adapter, where you need it
    //    addSeparator();
    //    add(proxy);
    //    proxy   .addActionListener(this);

    find    .addActionListener(this);
    //prefs    .addActionListener(this);
    newPrefs.addActionListener(this);
    //types.addActionListener(this);
    //    find    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
    //						   ActionEvent.CTRL_MASK));
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == undo) {
      //apollo.editor.AnnotationChangeLog.getAnnotationChangeLog().undo();
      CurationManager.getActiveCurationState().getTransactionManager().undo(this);

      /*
      FeatureSelectionEvent fse = new FeatureSelectionEvent(this,
                                  CurationManager.getActiveCurationState().getSelectionManager().getSelection(),
                                  true);
      System.out.println(CurationManager.getActiveCurationState().getSelectionManager().getSelection().getSelected());
      CurationManager.getActiveCurationState().getController().handleFeatureSelectionEvent(fse);
      */
      
      /*
      panel.setInvalidity(true);
      apolloPanel.doLayout();
      apolloPanel.setInvalidity(false);
      */
      //gcs.getSZAP().resetViews();
      //CurationManager.getActiveCurationState().getSZAP().repaint();
      /*
      annotView.setInvalidity(true);
      annotView.getComponent().repaint(annotView.getBounds().x,
                                       annotView.getBounds().y,
                                       annotView.getBounds().width,
                                       annotView.getBounds().height);
                                       */
      
    }
    else if (e.getSource() == redo) {
      logger.error("Redo selected; not implemented");
    }
    else if (e.getSource() == find) {
      if (findDialog != null) {
        findDialog.dispose();
        findDialog = null;
      }
      findDialog = new JDialog(ApolloFrame.getFrame(),"Find");
      findDialog.setLocationRelativeTo(ApolloFrame.getFrame());
      CurationManager cm = CurationManager.getCurationManager();
      FindPanel fp = new FindPanel(cm.getActiveCurState());
      
      findDialog.addWindowListener(new FindWindowListener());
      findDialog.getContentPane().add(fp);
      findDialog.pack();
      findDialog.show();
    }
    //     } else if (e.getSource() == proxy) {
    //       if (proxyDialog == null) {
    //         proxyDialog = new ProxyDialog((JFrame)SwingUtilities.getRootPane(szap).getParent());
    //       }
    //       proxyDialog.setLocationRelativeTo(
    //         SwingUtilities.getRootPane(szap).getParent());
    //       proxyDialog.show();
    //     }
    /*
    else if (e.getSource() == prefs) {
      if (prefsDialog != null) {
        prefsDialog.dispose();
        prefsDialog = null;
      }
      prefsDialog = new PreferencesDialog();
      prefsDialog.addWindowListener(new PrefsWindowListener());
      prefsDialog.show();
    }
    */
    else if (e.getSource() == newPrefs) {
      PreferenceWindow.getInstance().setVisible(true);
    }
    /*
    else if (e.getSource() == types) {
      if (typesWizard != null) {
        typesWizard.dispose();
      }
      typesWizard = new TypesWizard();
    }
    */
  }

  /** Radiobutton group to change look & feel */
  private void addLAFMenu() {
    ButtonGroup group = new ButtonGroup();
    UIManager.LookAndFeelInfo[] LFs = UIManager.getInstalledLookAndFeels();
    JRadioButtonMenuItem lfsMenuItem[] = new JRadioButtonMenuItem[LFs.length];
    for (int i=0; i<LFs.length; i++) {
      // Don't add Windows LAF option if we're not on Windows (it doesn't work)
      if (LFs[i].getName().equals("Windows") && !(apollo.util.IOUtil.isWindows()))
        continue;
      lfsMenuItem[i] = (JRadioButtonMenuItem) add(new JRadioButtonMenuItem(LFs[i].getName()));
      group.add(lfsMenuItem[i]);
      // Use class name comparison rather than comparing getName()s because those were returning
      // inconsistent results for "Mac OS X Aqua" look and feel between the LookAndFeel object and the 
      // LookAndFeelInfo object so no match was found when this was the default laf resulting in 
      // no button being selected.
      lfsMenuItem[i].setSelected(UIManager.getLookAndFeel().getClass().getName().equals(LFs[i].getClassName()));
      lfsMenuItem[i].addItemListener(new LAFListener());
    }
  }

  private class LAFListener implements ItemListener {
    public void itemStateChanged (ItemEvent e) {
      JRadioButtonMenuItem rb = (JRadioButtonMenuItem) e.getSource();
      try {
        if (rb.isSelected()) {
          UIManager.LookAndFeelInfo[] tmpLFs = UIManager.getInstalledLookAndFeels();
          for (int j=0; j<tmpLFs.length; j++) {
            if (rb.getText().equals(tmpLFs[j].getName())) {
              UIManager.setLookAndFeel(tmpLFs[j].getClassName());
              SwingUtilities.updateComponentTreeUI(frame);
            }
          }
        }
      } catch (Exception ex) {
        logger.error("could not change UIFactory: " + ex.getMessage(), ex);
      }
    }
  }

  private class FindWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      findDialog.dispose();
      findDialog = null;
    }
  }

  /*
  private class PrefsWindowListener extends WindowAdapter {
    public void windowClosing(WindowEvent e) {
      prefsDialog.dispose();
      prefsDialog = null;
    }
  }
  */
  
  //private ApolloFrame getFrame(){ return frame; }
}
