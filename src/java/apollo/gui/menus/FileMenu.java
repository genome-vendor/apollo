package apollo.gui.menus;

import apollo.config.Config;
import apollo.gui.*;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.event.*;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.SyntenyPanel;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;
import apollo.seq.io.FastaFile;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Properties;
import java.util.*;

import misc.JIniFile;

import org.apache.log4j.*;

import org.bdgp.io.*;
import org.bdgp.swing.widget.*;

import gov.sandia.postscript.*;
import apollo.main.*;

public class FileMenu extends JMenu implements ActionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FileMenu.class);

  public final static Object[] quit_options = { "Quit anyway",
                                           "Save first, then quit",
                                           "Cancel" };

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  JMenuItem   open;
  JMenuItem   addon;

  JMenuItem   save;
  JMenuItem   saveAll;
  JMenuItem   saveAs;
  JMenuItem   saveLayout;
  JMenuItem   loadLayout;
  JMenuItem   saveFasta;
  JMenuItem   writeTypes;
  JMenuItem   print;
  JMenuItem   quit;
  
  ApolloFrame frame;

  private class FileMenuListener implements MenuListener {
    public void menuCanceled(MenuEvent e) {}

    public void menuDeselected(MenuEvent e) {}

    public void menuSelected(MenuEvent e) {
      menuInit();
    }
  }

  public FileMenu(ApolloFrame frame) {
    super("File");
    this.frame = frame;
    addMenuListener(new FileMenuListener());
    getPopupMenu().addPropertyChangeListener(new PopupPropertyListener());
    buildMenu();
    menuInit();
  }

  public void buildMenu() {
    open     = new JMenuItem("Open new...");
    addon    = new JMenuItem("Layer more results or annotations...");
    
    // 2/2005: "Save" is no longer available as a menu option--"Save as" is safer and clearer.
    // Save is still available as a keyboard shortcut, though (ctrl-s), so Save is on the
    // menu (that's the only way to enable the shortcut) but invisible.
    save     = new JMenuItem("Save");
    
    //This one only gets added if the apollo frame is composite, and 
    //the style has editing allowed.
    // 2/2005: I'm commenting this one out, too.  It overwrites the original files, which
    // might not be what the user wants, and it's also currently throwing an exception.
    saveAll     = new JMenuItem("Save All");
    
    saveAs = new JMenuItem("Save as...");
    // "Save active species sequence..." if multi species?
    saveFasta = new JMenuItem("Save sequence...");

    saveLayout = new JMenuItem("Save window layout...");
    loadLayout = new JMenuItem("Load window layout...");

    writeTypes = new JMenuItem("Save type preferences...");
    quit     = new JMenuItem("Quit");
    print    = new JMenuItem("Print to file...");
    open    .addActionListener(this);
    addon   .addActionListener(this);
    save    .addActionListener(this);
    saveAll.addActionListener(this);
    saveAs.addActionListener(this);
    saveFasta.addActionListener(this);
    saveLayout.addActionListener(this);
    loadLayout.addActionListener(this);
    writeTypes.addActionListener(this);
    quit    .addActionListener(this);
    print   .addActionListener(this);

    open.setMnemonic('O');

    save.setMnemonic('S');
    saveAs.setMnemonic('A');
    quit.setMnemonic('Q');

    open    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                            ActionEvent.CTRL_MASK));

    // ctrl-s is the shortcut for Save (i.e. save in the same file we read from),
    // which is no longer listed on the File menu but stll might be useful for
    // experienced users.
    // The shortcut for Save As is ctrl-w.
    if (Config.isEditingEnabled()) {
      // save or saveAll is active 
      save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                 ActionEvent.CTRL_MASK));
      saveAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                 ActionEvent.CTRL_MASK));					 
    }
    saveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                   ActionEvent.CTRL_MASK));
    quit    .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                   ActionEvent.CTRL_MASK));
  }

  public void menuInit() {
    removeAll();

    add (open);

    if (Config.dataAdapterIsAvailable(ApolloDataAdapterI.OP_APPEND_DATA))
      add(addon);

    //Vector history = /*frame.*/getHistory();

//     if (history.size() > 0)
//     {
//       for(int i=0; i < history.size(); i++) {
//         Properties props = (Properties) history.elementAt(i);
//         add(getHistoryMenuItem(props));
//       }
//     }

    addSeparator();
    if (Config.isEditingEnabled()) {
      // Save and Save All no longer appear on the menu.
      // Save is still available with keyboard shortcut ctrl-s.
      if (getCurationManager().isMultiCuration()) {
        add(saveAll);
      } else {
        add(save);
      	if (!Config.getShowSaveOptionInFileMenu()) {
        	save.setVisible(false);
      	}
      }
    }
    // Add this even if editing isn't enabled--can use to save data fetched from a db
    // as a flat file.
    add(saveAs);  
    if (!Config.getShowSaveAsOptionInFileMenu()) {
      saveAs.setVisible(false);
    }

    add(saveFasta);
    add(writeTypes);
    add(saveLayout);
    add(loadLayout);

    addSeparator();
    add(print);

    addSeparator();
    
    add(quit);

    // don't allow saves to a read-only datasource
    //if (frame instanceof CompositeApolloFrame&&((CompositeApolloFrame)frame).getCompositeCurationSet() != null) {
      
//       allowSave = Config.getAdapterRegistry().adapterSupports(
//             ((CompositeApolloFrame)frame).getCompositeAdapter().getClass().getName(),
//             ApolloDataAdapterI.OP_WRITE_DATA);
    boolean allowSave = getCurationManager().getDataAdapter().canWriteData();
      
//     } else {
//       if (frame.getAdapter() != null) {
//         allowSave = Config.getAdapterRegistry().
//                     adapterSupports(frame.getAdapter().getClass().getName(),
//                                     ApolloDataAdapterI.OP_WRITE_DATA);

//         if (!(frame.getAdapter().getInputType().equals(DataInputType.FILE)))
//           allowSave = false;
//       }
//     }
//    save.setEnabled(allowSave);
//    saveAll.setEnabled(allowSave);

    CurationSet curation = getActiveCurState().getCurationSet();
    if (curation == null) // is this possible?
      saveFasta.setEnabled(false);
    else
      saveFasta.setEnabled(curation.isSequenceAvailable(curation.getEnd()));
  }

//   /** Stub - not implemented - returns empty vector  */
//   private Vector getHistory() { return new Vector(0); }

// this looks a good thing - why is it taken out? was it not working anymore?
//   public JMenuItem getHistoryMenuItem(Properties props) {
//     String region = props.getProperty("region");
//     String adapter = Config.getAdapterRegistry
//                      ().getNameForAdapter(
//                        props.getProperty("adapter"));
//     JMenuItem item = new JMenuItem(region+" ("+adapter+")");
//     item.addActionListener(new HistoryItemListener(props));
//     return item;
//   }
//   /** This is currently not used at all, delete? or rehook it in?? 
// was used by the commented out history menu item */
//   private class HistoryItemListener implements ActionListener {
//     Properties props;
//     public HistoryItemListener(Properties props) {
//       this.props = props;
//     }
//     public void actionPerformed(ActionEvent e) {
//       LoadUtil.loadWithProgress(frame, props, true);
//     }
//   }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == quit) {
      if (LoadUtil.confirmSaved (quit_options, new DataLoader())) {
        System.exit(0);
      }
    } else if (e.getSource() == save) { // save active species (multi)
      DataLoader loader = new DataLoader();
      JDialog saveDialog = new JDialog(frame,"Saving data...");
      saveDialog.setLocationRelativeTo(frame);
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(200,10));
      saveDialog.getContentPane().add(p);
      saveDialog.pack();
      saveDialog.show();

      // I think its funny to get curation set from frame - what does this mean
      // in multi species context?
      //loader.putCurationSet (frame.getAdapter(),frame.getCurationSet());
      loader.putCurationSet(getActiveCurState().getDataAdapter(),
                            getActiveCurState().getCurationSet());
      
      saveDialog.hide();
      saveDialog.dispose();
    } else if (e.getSource() == saveAll) {
      //&&frame instanceof CompositeApolloFrame&&((CompositeApolloFrame)frame).getCompositeCurationSet() != null){

      DataLoader loader = new DataLoader();

      //loader.putCurationSet(((CompositeApolloFrame)frame).getCompositeAdapter(),((CompositeApolloFrame)frame).getCompositeCurationSet());
      loader.saveCompositeDataHolder(getCurationManager().getDataAdapter(),
                                    getCurationManager().getCompositeDataHolder());
      
    } else if (e.getSource() == saveAs) {
      
      DataLoader loader = new DataLoader(); // singleton?
      //loader.saveFileDialog(frame.getApolloData());
      // should this be for all curations or just active curation? for now active
      // this ultimates ends up in DataAdapterGUI.doOperation(Object)
      //loader.saveFileDialog(getCurationManager().getActiveCurState().getCurationSet());
      // should we just send a curation set if single curation?
      //if (getCurationManager().isSingleCuration()) 
      //loader.saveFileDialog(getCurationManager().getSingleCurationSet());
      //else
      loader.saveFileDialog(getCurationManager().getCompositeDataHolder());
    } 
    // PRINT 
    else if (e.getSource() == print) {
      print();

    } else if (e.getSource() == open) {
      LoadUtil.loadWithProgress(frame, new String[0], true);
    } else if (e.getSource() == addon) {
      LoadUtil.loadWithProgress(frame, new String[0], false);
    } else if (e.getSource() == saveFasta) {
      saveFastaFile();
    } else if (e.getSource() == saveLayout) {
      saveLayout();
    } else if (e.getSource() == loadLayout) {
      loadLayout();
    } else if (e.getSource() == writeTypes) {
      try {
        String suggestedName = suggestTiersFileName();
        JFileChooser chooser = new JFileChooser(suggestedName);
        chooser.setSelectedFile(new File(suggestedName));
        chooser.setDialogTitle("Save tiers file");
        int result = chooser.showSaveDialog(frame);
        if (result == chooser.APPROVE_OPTION) {
          Config.getPropertyScheme().write(chooser.getSelectedFile());
          String m = "Saved types to " + chooser.getSelectedFile() + "\n";
          if (!((chooser.getSelectedFile().toString()).equals(suggestedName))) {
            m = m + "\nIf you want to use your new types next time you run Apollo, be sure to\nchange the Types parameter in " + Config.getStyle().getFileName() + "\nto '" + chooser.getSelectedFile() + "'";
          }
          JOptionPane.showMessageDialog(null,m);
        }
      } catch (Exception ex) {
        logger.error("Failed writing types", ex);
      }//end try
    }//end if
  }

  private void print() {
    try {
      //final ApolloPanel ap = frame.getOverviewPanel().getApolloPanel();
      //final ApolloPanel apolloPanel = getActiveCurState().getSZAP().getApolloPanel();

      final SyntenyPanel syntenyPanel = CompositeApolloFrame.getApolloFrame().getSyntenyPanel();

      if (syntenyPanel == null) {
        logger.error("Couldn't find SyntenyPanel to print");
        return;
      }

      JFileChooser chooser = new JFileChooser();
      JPanel accPan = new JPanel();
      final JRadioButton portrait = new JRadioButton("Portrait");
      final JRadioButton landscape = new JRadioButton("Landscape",true);
      ButtonGroup orientation = new ButtonGroup();
      orientation.add(portrait);
      orientation.add(landscape);
      JPanel orientPan = new JPanel();
      orientPan.setLayout(new GridLayout(2,1));
      orientPan.add(portrait);
      orientPan.add(landscape);
      orientPan.setBorder(new TitledBorder("Orientation"));


      final JTextField scaleField = new JTextField();
      scaleField.setPreferredSize(new Dimension(50,30));

      double scale = PSGrBase.getPSScale(syntenyPanel.getSize(),"landscape");
      scaleField.setText("" + scale);
      JLabel scaleLabel = new JLabel("Scale");
      JPanel scalePan = new JPanel();
      portrait.addItemListener(
        new ItemListener() { public void itemStateChanged(ItemEvent evt) {
          double locscale = PSGrBase.getPSScale(syntenyPanel.getSize(), (portrait.isSelected() ? "portrait" : "landscape"));
          scaleField.setText("" + locscale);} });
      landscape.addItemListener(
        new ItemListener() { public void itemStateChanged(ItemEvent evt) {
          double locscale = PSGrBase.getPSScale(syntenyPanel.getSize(), (portrait.isSelected() ? "portrait" : "landscape"));
          scaleField.setText("" + locscale);} });
      ComponentAdapter apca = new ComponentAdapter() {
          public void componentResized(ComponentEvent e) {
            logger.debug("resize of syntenyPanel");
            double locscale = PSGrBase.getPSScale(syntenyPanel.getSize(), (portrait.isSelected() ? "portrait" : "landscape"));
            scaleField.setText("" + locscale);
          }
        };

      syntenyPanel.addComponentListener(apca);

      scalePan.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      scalePan.add(scaleLabel,gbc);
      scalePan.add(scaleField,gbc);

      accPan.setLayout(new GridLayout(2,1));
      accPan.add(orientPan);
      accPan.add(scalePan);

      chooser.setAccessory(accPan);
      ExampleFileFilter filter = new ExampleFileFilter();
      filter.addExtension("ps");
      filter.addExtension("eps");
      filter.setDescription("Postscript files");
      chooser.setFileFilter(filter);
      int result = chooser.showDialog(frame,"Print to file");

      if (logger.isDebugEnabled()) {
        logger.debug("scaleField  = " + scaleField.getText());
        logger.debug("orientation = " +
                     (portrait.isSelected() ? "portrait" : "landscape"));
      }
      syntenyPanel.removeComponentListener(apca);
      if (result == chooser.APPROVE_OPTION) {
        String s = portrait.isSelected() ? "portrait" : "landscape";
        syntenyPanel.print(chooser.getSelectedFile(),s,scaleField.getText());
        //frame.getOverviewPanel().getApolloPanel().print(chooser.getSelectedFile());
      }
    } catch (Exception ex) {
      logger.error("Failed creating postscript", ex);
    }
  } // end of print()

  private void saveLayout() {
    try {

      if (CompositeApolloFrame.getApolloFrame() == null) {
        logger.error("Couldn't find ApolloFrame to save layout for");
        return;
      }

      JFileChooser chooser = new JFileChooser();

      ExampleFileFilter filter = new ExampleFileFilter();
      filter.addExtension("layout");
      filter.setDescription("Layout files");
      chooser.setFileFilter(filter);
      int result = chooser.showDialog(frame,"Save layout to file");

      if (result == chooser.APPROVE_OPTION) {
        if (chooser.getSelectedFile().exists()) {
          // Delete the existing layout file - config will get very confusing if don't do this because JIniFile will 
          // read the existing one and make modifications to it rather than starting from scratch
          int choice = JOptionPane.showConfirmDialog(null, "File exists. Do you want to overwrite it?", "File exists. Overwrite?", JOptionPane.YES_NO_OPTION); 
          if (choice == JOptionPane.NO_OPTION) {
            logger.info("Layout save cancelled");
            return;
          }
          chooser.getSelectedFile().delete();
        }

        JIniFile iniFile = new JIniFile(chooser.getSelectedFile());
        CompositeApolloFrame.getApolloFrame().saveLayout(iniFile);
        iniFile.addBlankLinesBetweenSections();
        if (!iniFile.updateFile()) {
          logger.error("Failed saving layout");
        }
      }
    } catch (Exception ex) {
      logger.error("Failed saving layout", ex);
    }
  } // end of saveLayout()

  private void loadLayout() {
    try {

      if (CompositeApolloFrame.getApolloFrame() == null) {
        logger.error("Couldn't find ApolloFrame to save layout for");
        return;
      }

      JFileChooser chooser = new JFileChooser();
      JPanel baseUpPan = new JPanel();
      baseUpPan.setLayout(new GridLayout(1,1));
      JCheckBox baseUpdate = new JCheckBox("Update base location");
      baseUpPan.add(baseUpdate);
      chooser.setAccessory(baseUpPan);

      ExampleFileFilter filter = new ExampleFileFilter();
      filter.addExtension("layout");
      filter.setDescription("Layout files");
      chooser.setFileFilter(filter);
      int result = chooser.showOpenDialog(frame);

      if (result == chooser.APPROVE_OPTION) {
        JIniFile iniFile = new JIniFile(chooser.getSelectedFile());
        CompositeApolloFrame.getApolloFrame().applyLayout(iniFile,baseUpdate.isSelected());
      }
    } catch (Exception ex) {
      logger.error("Failed reading layout", ex);
    }
  } // end of loadLayout()

  private String suggestTiersFileName() {
    String tiersFile = Config.getStyle().getTiersFile();
    if (tiersFile.indexOf(".apollo") > 0)
      return tiersFile;

    if (tiersFile.lastIndexOf("/") >= 0)
      tiersFile = tiersFile.substring(tiersFile.lastIndexOf("/")+1);
    else if (tiersFile.lastIndexOf("\\") >= 0)
      tiersFile = tiersFile.substring(tiersFile.lastIndexOf("\\")+1);
    String homeDotApolloDir = System.getProperty("user.home") + "/.apollo/";
    tiersFile = homeDotApolloDir + tiersFile;
    return tiersFile;
  }

  private void saveFastaFile() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Save whole sequence in FASTA format");
    int returnVal = chooser.showSaveDialog(null);
    if(returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (file==null) { // file might be null
        // is this message overkill - should it just exit?
        JOptionPane.showMessageDialog(null,"No file selected");
        return;
      }
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(getFasta());
        writer.close();
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(null,"Couldn't write sequence to "+file);
      }
      logger.info("Saved sequence to " + file);
    }
  }

  /** Saves sequence of active species */
  private String getFasta() {
    CurationSet curation = getActiveCurState().getCurationSet();
    SequenceI seq = curation.getRefSequence();
    String header = (">"+ Config.getDisplayPrefs().getHeader(curation) +
                     " " + seq.getLength() + " residues\n");
    return FastaFile.format(header, seq.getResidues(), 50);
  }

  /** convenience methods for getting species holder and active species */
  private CurationManager getCurationManager() { 
    return CurationManager.getCurationManager(); 
  }
  private GuiCurationState getActiveCurState() { 
    return getCurationManager().getActiveCurState(); 
  }
}


