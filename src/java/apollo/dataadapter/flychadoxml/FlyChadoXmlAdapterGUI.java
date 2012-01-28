package apollo.dataadapter.flychadoxml;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.*;
import java.io.File;
import java.util.Properties;

import org.apache.log4j.*;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.gui.GenericFileAdapterGUI;
import apollo.config.Config;
import apollo.gui.ProxyDialog;
import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CurationSet;
import apollo.datamodel.CompositeDataHolder;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.GuiTabPanel;
import apollo.dataadapter.FileTabPanel;
//import apollo.dataadapter.flygamexml.*;

/** Holds LoaderPanels in its tabbed pane. */

public class FlyChadoXmlAdapterGUI extends AbstractDataAdapterUI {//extends org.bdgp.swing.AbstractIntDataAdapUI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlyChadoXmlAdapterGUI.class);
  private static int tabbedIndex = 0;
  private final static String indexPropString = "ChadoXmlTabIndex";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private IOOperation ioOperation;
  private FlyChadoXmlAdapter driver;
  private JTabbedPane tabbedPane;

  /** The order of this array is the order that the panels will appear in
   * the JTabbedPane */
  private GuiTabPanel[] panels;

  private int FileTabIndex = 0;  // File is the first tab

  private Color grayBackgroundColor = new Color(220,220,220);

  private JCheckBox saveAnnots = new JCheckBox("Save annotations");
  private JCheckBox saveResults = new JCheckBox("Save evidence (computational results)");

  FlyChadoXmlAdapterGUI() {
  }

  public FlyChadoXmlAdapterGUI(IOOperation op) {
    this.ioOperation = op;
    setBackground(grayBackgroundColor);
    initPanels();
    buildGUI();
  }

  public void setDataAdapter(DataAdapter driver) {
    //    super.setDataAdapter(driver); // not sure if this is needed
    if (driver instanceof FlyChadoXmlAdapter) {
      this.driver = ((FlyChadoXmlAdapter)driver);
      logger.debug("Set data adapter to " + driver);
    } else {
      logger.error("FlyChadoXmlAdapterGUI not compatible with adapter " + driver + " ( class " + driver.getClass().getName() + ")");
    }
  }
  
  /** The order of the panels array is the order that the panels will appear in
   * the JTabbedPane - */
  private void initPanels() {
    LoaderPanel gene = new LoaderPanel("Gene",
                                   "Gene name or synonym",
                                   DataInputType.GENE,
                                   "cact",
                                   grayBackgroundColor);
    LoaderPanel cyt = new LoaderPanel("Cytology",
                                  "Cytological location",
                                  DataInputType.CYTOLOGY,
                                  "34A",
                                  grayBackgroundColor);
    // No query by scaffold anymore
//     GAMEPanel scaf = new GAMEPanel("Scaffold",
//                                    "Scaffold accession",
//                                    DataInputType.SCAFFOLD,
//                                    "AE003490",
//                                    grayBackgroundColor);
    FileTabPanel file = new FileTabPanel(this, grayBackgroundColor);
    // CHANGE
    LocationChadoPanel loc = new LocationChadoPanel(grayBackgroundColor);
    panels = new GuiTabPanel[] { file, gene, loc, cyt  };
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    if (getDataInput()==null || !getDataInput().hasInput())
      throw new ApolloAdapterException("null input");
    driver.setDataInput(getDataInput());
    //driver.setInputType(getCurrentInputType());
    //driver.setInput(getCurrentInput());

    if (ioOperation.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      driver.setDatabase(getCurrentPanel().getDatabase());
      return driver.getCurationSet();
    }
    else if (ioOperation.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
      driver.setDatabase(getCurrentPanel().getDatabase());
      return driver.addToCurationSet();
    }
    else if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      // presently sometimes values is CDH sometimes CS. For now deal with both
      // future refactor should be that it only sees CS. CDH makes no sense for
      // a single species adapter
      driver.commitChanges(values, saveAnnots.isSelected(), saveResults.isSelected());
      return null;
    } 
    else // not read or write
      return null;
  }

  private DataInput getDataInput() throws ApolloAdapterException {
    try {
      return getCurrentPanel().getDataInput();
    }
    catch (RuntimeException e) { throw new ApolloAdapterException(e.getMessage()); }
  }

  /** Returns the type of input that correlates with the tabbed pane currently selected  */
  private DataInputType getCurrentInputType() {
    return getCurrentPanel().getInputType();
  }

  /**
   * Returns the input from the currently selected tabbed pane
   */
  private String getCurrentInput() {
    return getCurrentPanel().getCurrentInput();
  }

  
  /** public for testing synteny - tied in w gui unfortunately */
  public GuiTabPanel getCurrentPanel() {
    return panels[tabbedPane.getSelectedIndex()];
  }

  private void addCheckboxes(FileTabPanel filepanel) {
    JPanel checkboxPanel = new JPanel();
    checkboxPanel.setLayout(new BorderLayout());
    checkboxPanel.add("North", saveAnnots);
    checkboxPanel.add("South", saveResults);
    filepanel.add("South", checkboxPanel);
    saveAnnots.setEnabled(true);
    saveAnnots.setSelected(true);
    saveResults.setEnabled(true);
    saveResults.setSelected(true);
  }

  /** Pass in history info for all the fields in Properties.
   * Also retrieve tabbedIndex from properties. */
  public void setProperties(Properties in) {
    if (in == null)
      return;
    for  (int i=0; i<panels.length; i++) {
      panels[i].retrieveHistoryFromProperties(in);
    }
    if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      chooseTab("file");  // Disable all but the File tab
      FileTabPanel filepanel = (FileTabPanel)(panels[FileTabIndex]);
      //      filepanel.setLabel("Filename ");  // was "Filename or URL"
      addCheckboxes(filepanel);
    }
    else { // READ or APPEND
      // tabbed index
      String indexStr = in.getProperty(indexPropString);
      if (indexStr!=null) {
        tabbedIndex = Integer.parseInt(indexStr);
        tabbedPane.setSelectedIndex(tabbedIndex);
      }
      // Enable all tabs  (NEED?)
      for (int i = 0; i < panels.length; i++)
        tabbedPane.setEnabledAt(i, true);

      //      FileTabPanel filepanel = (FileTabPanel)(panels[FileTabIndex]);
      //      filepanel.setLabel("Filename or URL");  // might have been just "Filename" if we last did a write
    }
  }

  /** Retrieve history and return in Properties. Also set
   * tabbed index */
  public Properties getProperties() {
    getCurrentPanel().addSelectedToHistory();

    Properties out = new Properties();
    for (int i=0; i<panels.length; i++) {
      panels[i].putHistoryInProperties(out);
    }
    // tabbed index
    out.put(indexPropString,""+tabbedIndex);
    return out;
  }

  /** Loops through GamePanels and inserts into tabbed pane,
   * Also hafta add our own buttons, since DataAdapterChooser does not add buttons
   * for AbstractIntDataAdapUIs although i think there should be a way for an
   * AbstractIntDAI to say hey i want those buttons like boolean addOKCancelButtons()
   */
  private void buildGUI() {
    tabbedPane = new JTabbedPane();
    //tabbedPane.setPreferredSize(new Dimension(600,70));// ??
    for (int i=0; i<panels.length; i++) {
      panels[i].insertIntoTabbedPane(tabbedPane,i);
    }
    tabbedPane.setSelectedIndex(tabbedIndex);

    tabbedPane.addChangeListener( new ChangeListener() {
                                    public void stateChanged(ChangeEvent e) {
                                      tabbedIndex = tabbedPane.getSelectedIndex();
                                    };
                                  }
                                );
    //setPreferredSize(new Dimension(450,200)); // ??
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
    buttonPanel.add(Box.createHorizontalStrut(280));
    //buttonPanel.add(okButton);
    buttonPanel.add(Box.createHorizontalStrut(20));
    //buttonPanel.add(cancelButton);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(Box.createHorizontalStrut(150));
    buttonPanel.add(proxyButton);
    buttonPanel.add(Box.createHorizontalGlue());
    add(buttonPanel);
  }

  // Disable all but the requested tab
  public void chooseTab(String which) {
    if (which.equalsIgnoreCase("file")) {
      tabbedIndex = FileTabIndex;
      for (int i = 0; i < panels.length; i++) {
        if (i == FileTabIndex)
          tabbedPane.setEnabledAt(i, true);
        else
          tabbedPane.setEnabledAt(i, false);
      }
      tabbedPane.setSelectedIndex(FileTabIndex);
    }
    // Need others?
  }
}


