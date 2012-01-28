package apollo.dataadapter.genbank;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionEvent;
import java.util.*;
import java.io.File;

import org.apache.log4j.*;
import org.bdgp.io.*;
import apollo.gui.GenericFileAdapterGUI;
import apollo.config.Config;
import apollo.gui.ProxyDialog;
import apollo.datamodel.*;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.FileTabPanel;
import apollo.dataadapter.GuiTabPanel;
import apollo.dataadapter.DataInputType;
import apollo.util.IOUtil;

public class GenbankAdapterGUI extends GenericFileAdapterGUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GenbankAdapterGUI.class);

  private static int tabbedIndex = 0;
  private final static String indexPropString = "PubTabIndex";

  private static JRadioButton tabular = new JRadioButton("GenBank tabular format (save to directory)");
  private static JRadioButton humanReadable = new JRadioButton("GenBank human readable format (save to file)", true);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private JTabbedPane tabbedPane;

  private GuiTabPanel[] pubPanels;

  private int FileTabIndex = 0;  // File is the first tab

  private Color filePanelColor = new Color(255, 218, 185);
  private Color genbankPanelColor = new Color(244, 240, 245);

  public GenbankAdapterGUI(IOOperation op) {
    this.op = op;
    if (this.op == ApolloDataAdapterI.OP_READ_DATA) {
      initPanels();
      // GUI for reading GenBank offers tabs to let you load by File or Accession
      buildGUIForReading();
    }
    else {
      super.initGUI();
      buildGUIForWriting();
      attachListeners();
    }
  }

  /** The order of the publicDbPanels array is the 
      order that the panels will appear in the JTabbedPane */
  private void initPanels() {
    FileTabPanel file = new FileTabPanel(this, filePanelColor);
    PublicDbPanel gb = new PublicDbPanel("Accession",
                                         "Accession #",
                                         DataInputType.URL,
                                         "AE003603",
                                         genbankPanelColor);
    pubPanels = new GuiTabPanel[] { file, gb };
  }

  /** Loops through TabPanels and inserts into tabbed pane,
   * Also hafta add our own buttons, 
   * since DataAdapterChooser does not add buttons
   * for AbstractIntDataAdapUIs although i think there should be a way for an
   * AbstractIntDAI to say hey i want those buttons like boolean
   * addOKCancelButtons()
   */
  public void buildGUIForReading() {
    tabbedPane = new JTabbedPane();
    for (int i = 0; i < pubPanels.length; i++) {
      pubPanels[i].insertIntoTabbedPane(tabbedPane,i);
    }
    tabbedPane.setSelectedIndex(tabbedIndex);

    tabbedPane.addChangeListener( new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          tabbedIndex = tabbedPane.getSelectedIndex();
        };
      }
                                  );
    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    add(tabbedPane);

    JButton proxyButton = new JButton("Proxy settings...");
    proxyButton.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          ProxyDialog pd = new ProxyDialog(null);
          pd.setVisible(true);
        }
      }
                                   );
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(Box.createHorizontalStrut(300));
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(Box.createHorizontalStrut(150));
    buttonPanel.add(proxyButton);
    buttonPanel.add(Box.createHorizontalGlue());
    add(buttonPanel);
  }

  public void buildGUIForWriting() {
    super.buildGUI();
    ButtonGroup format = new ButtonGroup();
    format.add(tabular);
    format.add(humanReadable);
    JPanel formatPanel = new JPanel();
    formatPanel.setLayout(new BorderLayout());
    panel.add("South", formatPanel);
    formatPanel.add("North", tabular);
    formatPanel.add("South", humanReadable);
  }

  /** DataAdapter has to be a GenbankAdapter, no way to enforce this,
    * will throw cast exception if not GenbankAdapter */
  public void setDataAdapter(DataAdapter driver) {
    if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA))
      super.setDataAdapter(driver);
    else
      this.driver = driver;
    if (driver == null) {
      logger.error("GenbankAdapterGUI.setDataAdapter: driver is null!");
    }
  }

  public Object doOperation(Object values) throws DataAdapterException {
    if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      ((GenbankAdapter) driver).setInputType(DataInputType.DIR);
      String path = getSelectedPath();
      path = IOUtil.findFile(path, true);  // might be a relative path
      boolean wantTabular = tabular.isSelected();
      try {
	  File pathFile = new File(path);
          // Tabular writer requires a directory.  (New human-readable one doesn't.)
          // Should we try to create a new directory if the requested directory
          // doesn't already exist?
          if (wantTabular && (!pathFile.isDirectory() || !pathFile.canRead())) {
            JOptionPane.showMessageDialog(null,
                                          "Need a valid directory.  " +
                                          "Can't write to directory " + path,
                                          "Warning",
                                          JOptionPane.WARNING_MESSAGE);
            return null;
	  }
          // Check that we can create file (e.g. that it's not a filename in
          // a nonexistent dir)
          else if (!wantTabular && 
                   (pathFile == null || pathFile.isDirectory() || !IOUtil.canWriteToDirectory(path))) {
            JOptionPane.showMessageDialog(null,
                                          "Need a valid file.  " +
                                          "Can't write to file " + path + ".\n" + 
                                          ((pathFile == null) ? "pathFile is null" : "") +
                                          ((pathFile.isDirectory()) ? pathFile  + " is a directory" : "") +
                                          ((!IOUtil.canWriteToDirectory(path)) ? "Can't write to directory for " + path : ""),
                                          "Warning",
                                          JOptionPane.WARNING_MESSAGE);
            return null;
	  }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(null,
                                      "Can't write to " + 
                                      (wantTabular ? "directory " : "file ") + 
                                      path,
                                      "Warning",
                                      JOptionPane.WARNING_MESSAGE);
        return null;
      }

      ((GenbankAdapter) driver).setInput(path);
      String validation_config = (Config.getRootDir() + 
				  "/conf/validation.conf");
      if (validation_config != null) {
	((GenbankAdapter) driver).setValidationFile (validation_config);
      }
      if (((GenbankAdapter) driver).getInput().equals (path)) {
        ((GenbankAdapter) driver).commitChanges(((CompositeDataHolder) values).getCurationSet(0), wantTabular);
      } else {
        logger.warn("Unable to write to " + (wantTabular ? "directory " : "file "));
      }
      return null;
    } else if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      ((GenbankAdapter)driver).setDatabase(getCurrentTabPanel().getDatabase());
      ((GenbankAdapter) driver).setInputType(getCurrentTabPanel().getInputType());
      ((GenbankAdapter) driver).setInput(getCurrentInput());
      CurationSet curation = ((GenbankAdapter) driver).getCurationSet();
      if (curation == null)
        JOptionPane.showMessageDialog(null,
                                      "Read failed",
                                      "Warning",
                                      JOptionPane.WARNING_MESSAGE);
      return curation;
    } else {
      return null;
    }
  }

  private String getCurrentInput() {
    return getCurrentTabPanel().getCurrentInput();
  }

  private GuiTabPanel getCurrentTabPanel() {
    return pubPanels[tabbedPane.getSelectedIndex()];
  }

  /** Retrieve history and return in Properties. Also set
   * tabbed index */
  public Properties getProperties() {
    if (this.op == ApolloDataAdapterI.OP_WRITE_DATA) {
      return super.getProperties();
    }
    else {
      getCurrentTabPanel().addSelectedToHistory();
      
      Properties out = new Properties();
      for (int i = 0; i < pubPanels.length; i++) {
        pubPanels[i].putHistoryInProperties(out);
      }
      // tabbed index
      out.put(indexPropString,""+tabbedIndex);
      return out;
    }
  }

  /** Called from browse button, sets browser with currently selected
   * path if there is one, and then calls browser to get a file.
   * Overloads GenericFileAdapterGui.browseFiles, just to force it to call
   * our local fileBrowser method.
   */
  public void browseFiles(JComboBox list) {
    String selectedPath = getSelectedPath();
    File   currentFile  = new File(selectedPath);
    File   browseFile   = fileBrowser(currentFile,this);

    if (browseFile == null) {
      return;
    }

    list.configureEditor(list.getEditor(),browseFile.toString());
  }

  /** Brings up JFileChooser, returns null on cancel or null file. */
  public static File fileBrowser(File currentFile, Component parent) {
    String startPath;
    
    if (currentFile!=null && currentFile.exists()) {
      startPath = currentFile.getPath();
    } else {
      startPath = System.getProperty("user.home");
    }

    JFileChooser chooser = new JFileChooser(startPath);

    // Tabular GenBank adapter saves to a DIRECTORY, not to a single file.
    // (This doesn't seem to be working.)
    if (tabular.isSelected()) {
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setApproveButtonText("Save to this directory");
    }
    else
      chooser.setApproveButtonText("Save to this file");

    int returnVal = chooser.showOpenDialog(parent);

    File file = chooser.getSelectedFile();
    
    if (file==null) {
      JOptionPane.showMessageDialog(parent, 
                                    "No " + 
                                    (tabular.isSelected() ? "directory " : "file ") + 
                                    " selected",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return null;
    }
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return null;
    return file;
  }

  /** Pass in history info for all the fields in Properties.
   * Also retrieve tabbedIndex from properties. */
  public void setProperties(Properties in) {
    if (in == null)
      return;
    if (this.op == ApolloDataAdapterI.OP_WRITE_DATA) {
      super.setProperties(in);
    }
    else {
      for  (int i = 0; i < pubPanels.length; i++) {
        pubPanels[i].retrieveHistoryFromProperties(in);
      }
      // tabbed index
      String indexStr = in.getProperty(indexPropString);
      if (indexStr!=null) {
	tabbedIndex = Integer.parseInt(indexStr);
	tabbedPane.setSelectedIndex(tabbedIndex);
      }
    }
  }

}



