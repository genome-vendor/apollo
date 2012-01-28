package apollo.dataadapter.flygamexml;

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

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import org.bdgp.swing.AbstractDataAdapterUI;

// is it a scandal for apollo.dataadapter to import apollo.gui
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
import apollo.dataadapter.gamexml.*;

/** Most of the smarts here are actually in GAMEPanel and its subclasses
  * This just holds the game panels in its tabbed pane */

public class FlyGAMEAdapterGUI extends AbstractDataAdapterUI {//extends org.bdgp.swing.AbstractIntDataAdapUI {

  private static int tabbedIndex = 0;
  private IOOperation ioOperation;
  /** The only driver that works with this is the FlyGAMEAdapter */
  private FlyGAMEAdapter gameDriver;
  private JTabbedPane tabbedPane;
  private final static String indexPropString = "GAMETabIndex";

  /** The order of this array is the order that the panels will appear in
   * the JTabbedPane */
  private GuiTabPanel[] gamePanels;

  private int FileTabIndex = 0;  // File is the first tab

  private Color grayBackgroundColor = new Color(220,220,220);

  private JCheckBox saveAnnots = new JCheckBox("Save annotations");
  private JCheckBox saveResults = new JCheckBox("Save evidence (computational results)");

  public FlyGAMEAdapterGUI(IOOperation op) {
    //super(op);
    this.ioOperation = op;
    setBackground(grayBackgroundColor);
    initPanels();
    buildGUI();
  }

  /** The order of the gamePanels array is the order that the panels will appear in
   * the JTabbedPane - */
  private void initPanels() {
    GAMEPanel gene = new GAMEPanel("Gene",
                                   "Gene name or synonym",
                                   DataInputType.GENE,
                                   "cact",
                                   grayBackgroundColor);
    GAMEPanel cyt = new GAMEPanel("Cytology",
                                  "Cytological location",
                                  DataInputType.CYTOLOGY,
                                  "34A",
                                  grayBackgroundColor);
    GAMEPanel scaf = new GAMEPanel("Scaffold",
                                   "Scaffold accession",
                                   DataInputType.SCAFFOLD,
                                   "AE003490",
                                   grayBackgroundColor);
    FileTabPanel file = new FileTabPanel(this, grayBackgroundColor);
    LocationGAMEPanel loc = new LocationGAMEPanel(grayBackgroundColor);
    // 2/2005: We are no longer supporting query by sequence (doesn't work for >=r3.2)
    //    SequenceGAMEPanel seq = new SequenceGAMEPanel(seqPanelColor);
    gamePanels = new GuiTabPanel[] { file, gene, cyt, scaf, loc };
  }

  /** DataAdapter has to be a GAMEAdapter, no way to enforce this,
    * will throw cast exception if not GAME */
  public void setDataAdapter(DataAdapter driver) {
    super.setDataAdapter(driver); // not sure if this is needed
    gameDriver = (FlyGAMEAdapter)driver;
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    //Config.newDataAdapter(driver); // now done in AbstractApolloAdapter
    if (getDataInput()==null || !getDataInput().hasInput())
      throw new ApolloAdapterException("null input");
    gameDriver.setDataInput(getDataInput());
    //gameDriver.setInputType(getCurrentInputType());
    //gameDriver.setInput(getCurrentInput());

    if (ioOperation.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      gameDriver.setDatabase(getCurrentGamePanel().getDatabase());
      return gameDriver.getCurationSet();
    }
    else if (ioOperation.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
      gameDriver.setDatabase(getCurrentGamePanel().getDatabase());
      return gameDriver.addToCurationSet();
    }
    else if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      // presently sometimes values is CDH sometimes CS. For now deal with both
      // future refactor should be that it only sees CS. CDH makes no sense for
      // a single species adapter
//       ApolloDataI apolloData = (ApolloDataI)values;
//       CurationSet curSet=null;
//       if (apolloData.isCurationSet()) {
//         curSet = apolloData.getCurationSet();
//       } 
//       // eventually phase this case out
//       else if (apolloData.isCompositeDataHolder()) {
//         CompositeDataHolder cdh = apolloData.getCompositeDataHolder();
//         // assume its the 1st species - presumptious but what else can we do
//         curSet = cdh.getSpeciesCurationSet(0);
//       }
//       //gameDriver.commitChanges(((CompositeDataHolder)values).getSpeciesCurationSet(0));
//       gameDriver.commitChanges(curSet);

      gameDriver.commitChanges(values, saveAnnots.isSelected(), saveResults.isSelected());
      return null;
    } 
    else // not read or write
      return null;
  }

  private DataInput getDataInput() throws ApolloAdapterException {
    try {
      return getCurrentGamePanel().getDataInput();
    }
    catch (RuntimeException e) { throw new ApolloAdapterException(e.getMessage()); }
  }

  /** Returns the type of GAME input that correlates with the tabbed pane
   * currently selected  */
  private DataInputType getCurrentInputType() {
    return getCurrentGamePanel().getInputType();
  }

  /**
   * Returns the input from the currently selected tabbed pane
   */
  private String getCurrentInput() {
    return getCurrentGamePanel().getCurrentInput();
  }

  
  /** public for testing synteny - tied in w gui unfortunately 
   this isnt just for tetsting synteny - used to get input - crucial! */
  public GuiTabPanel getCurrentGamePanel() {
    return gamePanels[tabbedPane.getSelectedIndex()];
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
    for  (int i=0; i<gamePanels.length; i++) {
      gamePanels[i].retrieveHistoryFromProperties(in);
    }
    if (ioOperation.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      chooseTab("file");  // Disable all but the File tab
      FileTabPanel filepanel = (FileTabPanel)(gamePanels[FileTabIndex]);
      addCheckboxes(filepanel);
    }
    else {
      // tabbed index
      String indexStr = in.getProperty(indexPropString);
      if (indexStr!=null) {
        tabbedIndex = Integer.parseInt(indexStr);
        tabbedPane.setSelectedIndex(tabbedIndex);
      }
    }
  }

  /** Retrieve history and return in Properties. Also set
   * tabbed index */
  public Properties getProperties() {
    getCurrentGamePanel().addSelectedToHistory();

    Properties out = new Properties();
    for (int i=0; i<gamePanels.length; i++) {
      gamePanels[i].putHistoryInProperties(out);
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
    for (int i=0; i<gamePanels.length; i++) {
      gamePanels[i].insertIntoTabbedPane(tabbedPane,i);
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
  public
 void chooseTab(String which) {
    if (which.equalsIgnoreCase("file")) {
      tabbedIndex = FileTabIndex;
      for (int i = 0; i < gamePanels.length; i++) {
        if (i != FileTabIndex)
          tabbedPane.setEnabledAt(i, false);
      }
      tabbedPane.setSelectedIndex(FileTabIndex);
    }
    // Need others?
  }
}
