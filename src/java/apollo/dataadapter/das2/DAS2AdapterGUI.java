package apollo.dataadapter.das2;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import org.apache.log4j.*;

import org.bdgp.io.*;
import org.bdgp.util.MethodRunnerActionListener;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.datamodel.ApolloDataI;
import apollo.datamodel.CurationSet;
import apollo.config.Config;
import apollo.dataadapter.*;
import apollo.dataadapter.das.*;
import apollo.dataadapter.das.simple.*;
import apollo.gui.ProxyDialog;
import apollo.gui.GenericFileAdapterGUI;

import apollo.util.GuiUtil;

/** This actually constructs a completely different GUI for reading vs. writing. 
 *  The version for reading from a DAS/2 server offers a pulldown list of known
 *  servers (currently hardcoded in apollo.cfg as DASServer; in the future, to be
 *  retrieved from registry), a list of sequence sources (organism + version),
 *  a list of chromosomes or segments, boxes for indicating the desired start/end,
 *  and (maybe this should go BEFORE the sequence sources list?) a list of previous
 *  selections (i.e. a history).  There's also a box for Proxy settings and a "Change"
 *  button to change the proxy settings.
 *  
 *  In write mode, the GUI is much simpler, with a box for you to enter the filename.
 *
 *  If you want a simpler way to test reading of DAS/2 features, you can call
 *  DAS2FeatureSaxParser from the command line with a filename argument for a file
 *  containing DAS2XML. */

public class DAS2AdapterGUI 
//  extends AbstractDataAdapterUI 
  extends GenericFileAdapterGUI 
  implements ApolloDataAdapterGUI
{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DAS2AdapterGUI.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // what the user must provide or select from (history)
  JComboBox serverList;
  JComboBox versionedSourcesList;
  JComboBox segList;
  JTextField lowTextBox;
  JTextField hiTextBox;
  JComboBox historyList;

  private static final String CHOOSE_STRING = "choose...";

  JLabel serverLabel;
  JLabel dsnLabel;
  JLabel segLabel;
  JLabel lowLabel;
  JLabel hiLabel;
  JLabel historyLabel;
  JLabel proxyLabel;

  JPanel      pane;

  private JTextField proxyTextField;
  private JButton proxyButton;

  Vector versionedSourcesVector = new Vector();
  Vector serverVector = new Vector();
  Vector historyVector = new Vector();
  Vector segmentVector = null;
  Vector emptyVector = new Vector();

  public static final int MAX_HISTORY_LENGTH = 5;

  //  protected DataAdapter driver;
  protected DAS2Adapter driver;
  protected IOOperation op;

  private Properties props;
  protected ServerListener server_listener;
  protected DSNListener dsn_listener;
  protected HistoryListener history_listener;
  LongValidator posValidator = new LongValidator();
    //  private DASServerI dasServer;
  private DAS2Request dasServer;

    // Apollo can't handle huge regions
    private static int MAX_REGION_SIZE = 500000;

  public DAS2AdapterGUI() {
    logger.debug("DAS2AdapterGUI called with no args");
    //    buildGUIForReading();
  }

  public DAS2AdapterGUI(IOOperation op) {
    setIOOperation(op);
  }

  /** Breaking this out because it gets called separately if you want to save
   *  or load data during an already-running Apollo session */
  public void setIOOperation(IOOperation op) {
    if (!(op.equals(this.op))) {
      if (op == ApolloDataAdapterI.OP_READ_DATA ||
          op == ApolloDataAdapterI.OP_APPEND_DATA) {
        this.op = op;
        removeAll();
        logger.debug("Set op to " + op + "; building gui for reading");
        buildGUIForReading();
      }
      else {
        // If we're writing, most of the GUI building is handled by GenericFileAdapterGUI
        this.op = op;
        removeAll();
        logger.debug("Set op to " + op + "; building gui for writing");
        super.initGUI();
        super.buildGUI();
        super.attachListeners();
      }
    }
  }

  public void init() {}

  public void buildGUIForReading() {
    serverList = new JComboBox();
    versionedSourcesList = new JComboBox();
    segList = new JComboBox();
    hiTextBox = new JTextField();
    lowTextBox = new JTextField();
    historyList = new JComboBox();

    serverLabel = new JLabel("DAS/2 server");
    dsnLabel = new JLabel("Sources");
    segLabel = new JLabel("Chromosomes or segments");
    lowLabel = new JLabel("Sequence from");
    hiLabel = new JLabel("   to");
    historyLabel = new JLabel("Previous selections");

    //create the listener, but don't add yet.
    //-- wait till 'setProperties'
    server_listener = new ServerListener();

    //create the listener, but don't add yet.
    //-- wait till 'setProperties'
    dsn_listener = new DSNListener();

    //create the listener, but don't add yet.
    //-- wait till 'setProperties'
    history_listener = new HistoryListener();

    proxyLabel = new JLabel("Proxy settings");
    proxyTextField = new JTextField(20);
    proxyTextField.setEnabled(false);
    proxyButton = new JButton("Change...");
    proxyButton.addActionListener(new ProxyListener());

    finishGUIForReading();
  }
  
  public void setDataAdapter(DataAdapter driver) {
    //    super.setDataAdapter(driver); // not sure if this is needed
    if (driver instanceof DAS2Adapter) {
      this.driver = ((DAS2Adapter)driver);
      logger.debug("DAS2AdapterGUI.setDataAdapter: set data adapter to " + driver);
      if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
        this.driver.setName("DAS2XML file");
      }
      else
        this.driver.setName("DAS/2 Server");
    } else {
      logger.error("DAS2AdapterGUI not compatible with adapter " + driver + " ( class " + driver.getClass().getName() + ")");
    }
  }

  public Properties createStateInformation(){
    StateInformation stateInfo = new StateInformation();
    String proxySet = System.getProperty("proxySet");
    String proxyHost = System.getProperty("proxyHost");
    String proxyPort = System.getProperty("proxyPort");
    
    DASSegment theSegment = getSelectedSegment();
    
    if(getSelectedDSN() != null){
      setPropertyIfNotNull(stateInfo,StateInformation.DSN_DESCRIPTION, getSelectedDSN().getDescription());
      setPropertyIfNotNull(stateInfo,StateInformation.DSN_MAP_MASTER, getSelectedDSN().getMapMaster());
      setPropertyIfNotNull(stateInfo,StateInformation.DSN_SOURCE, getSelectedDSN().getSource());
      setPropertyIfNotNull(stateInfo,StateInformation.DSN_SOURCE_ID, getSelectedDSN().getSourceId());
      setPropertyIfNotNull(stateInfo,StateInformation.DSN_SOURCE_VERSION, getSelectedDSN().getSourceVersion());
    }

    if(getDASServer() != null){
      setPropertyIfNotNull(stateInfo,StateInformation.SERVER_URL, getDASServer().getURL());
    }

    if(theSegment != null){
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_SEGMENT, theSegment.getSegment());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_ID, theSegment.getId());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_START, lowTextBox.getText());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_STOP, hiTextBox.getText());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_ORIENTATION, theSegment.getOrientation());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_SUBPARTS, theSegment.getSubparts());
      setPropertyIfNotNull(stateInfo,StateInformation.SEGMENT_LENGTH, theSegment.getLength());
    }
    
    return stateInfo;
  }

  /**
   * This gets called when Apollo starts up (and probably elsewhere as well)
   * and is responsible for the initialisation of the listboxes.
  **/
  public void setProperties(Properties in) {
    // Don't do anything else if we're going to write data (?)
    if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      super.setProperties(in);
      return;
    }

    props = in;
    String server;
    serverVector.removeAllElements();
    String proxyHost;
    String proxyPort;
    String useProxy;
    Properties systemProperties = System.getProperties();
    String propertyString;
    int items;

    //Get rid of these listeners if they are set: we are manipulating
    //the lists.
    serverList.removeItemListener(server_listener);
    historyList.removeActionListener(history_listener);

    try {
      //The first item in the list of servers should be a blank.
      serverVector.addElement(CHOOSE_STRING);
      propertyString = props.getProperty("srvItems");

      if(propertyString != null) {
        items = Integer.parseInt(propertyString);
        for(int i=0; i < items; i++) {
          server = props.getProperty("srvItem"+i);
          if(server != null && server.trim().length() > 0) {
            serverVector.addElement(props.getProperty("srvItem"+i));
          }//end if
        }//end for
      }
      else {
        //        serverVector.addElement(Config.getDASServer());
        findServers();
      }//end if

    }
    catch (Exception e) {
      // This isn't very nice
      if (serverVector.size() == 0) {
        // When does this happen?
        logger.error("Exception while filling in server vector from history!");
        serverVector.addElement(Config.getDASServer());
      }
    }

    serverList.setModel(new DefaultComboBoxModel(serverVector));

    historyVector.removeAllElements();

    try {
      logger.debug("DASAdapterGUI.setProperties: adding choose_string to history vector");
      historyVector.addElement(CHOOSE_STRING);
      propertyString = props.getProperty("dasItems");

      if(propertyString != null) {
        items = Integer.parseInt(propertyString);
      } else {
        items = 0;
      }//end if

      for(int i=0; i < items; i++) {
        historyVector.addElement(props.getProperty("dasItem"+i));
      }//end for

    }
    catch (Exception e) {
      logger.error(e.getMessage(), e);
    }

    historyList.setModel(new DefaultComboBoxModel(historyVector));

    useProxy = props.getProperty("dasUseProxy");
    proxyPort = props.getProperty("dasProxyPort");
    proxyHost = props.getProperty("dasProxyHost");

    if(proxyHost != null && !proxyHost.equals("none")) {
      System.setProperty("http.proxyHost", proxyHost);
    }//end if

    if(proxyPort != null && !proxyPort.equals("none")) {
      systemProperties.setProperty("http.proxyPort", proxyPort);
    } else {
      systemProperties.remove("http.proxyPort");
    }//end if

    if(useProxy != null && useProxy.equals("true")) {
      systemProperties.put("http.proxySet", "true");
    } else {
      systemProperties.remove("http.proxySet");
    }//end if

    updateProxyTextField(
      useProxy,
      proxyHost,
      proxyPort
    );

    serverList.addItemListener(server_listener);
    historyList.addActionListener(history_listener);
    versionedSourcesList.addItemListener(dsn_listener);
  }//end setProperties

  /**
   * <p>
   * This must run after the server successfully finishes the doOperation()
   * method - i.e. after a successful read. Copy the data from the server,dsn,entry
   * point and range into a string, insert into the history dropdown. Then
   * copy the contents of both dropdowns into the properies file, which you return.
   * </p>
   * <p>
   * -- of course, you omit any "choose..." prompts in the dropdowns when
   * copying into the properties files.</p>
   * <p>
   * One nice side-effect of the timing of this method is that unsuccessful url's
   * don't seem to get copied into the history.
   * </p>
  **/
  public Properties getProperties() {
    if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      return super.getProperties();
    }

    String history=null;
    Properties out = new Properties();
    int maximumHistoryCount;

    serverList.removeItemListener(server_listener);
    historyList.removeActionListener(history_listener);

    String selectedServer = getSelectedServer();

    if (selectedServer != null &&
        selectedServer.trim().length() > 0 &&
        !selectedServer.equals(CHOOSE_STRING)) {
//       history =
//         selectedServer + " " +
//         getSelectedDSN().getSourceId() + " " +
//         getSelectedSegment().getId() + " " +
//         lowTextBox.getText() + " " +
//         hiTextBox.getText();

	String segmentID = getSelectedSegment().getId();
	if (segmentID.indexOf("/") > 0)
	    segmentID = segmentID.substring(segmentID.indexOf("/")+1);
	// Put the interesting stuff earlier so it will be where user can see it
	// This ends up looking like:
	// http://das.biopackages.net/das/genome/yeast/S228C segment/chrIV 6666 9999 [other stuff cut off]
	history = getSelectedDSN().getSourceId() + " " +
	    segmentID + " " +
	    lowTextBox.getText() + " " +
	    hiTextBox.getText() + " " + 
	    selectedServer;
      if (!historyVector.contains(history)) {
        historyVector.insertElementAt(history, 0);
        logger.debug("Put selected item first in history list");
      }
    }//end if

    if(serverVector.size()>MAX_HISTORY_LENGTH) {
      maximumHistoryCount = MAX_HISTORY_LENGTH;
    } else {
      maximumHistoryCount = serverVector.size();
    }//end if

    out.put("srvItems", maximumHistoryCount+"");

    for (int i=0; i < maximumHistoryCount; i++) {
      if(!serverVector.elementAt(i).equals(CHOOSE_STRING)) {
        if (!out.contains(serverVector.elementAt(i)))
          out.put("srvItem"+i, (String) serverVector.elementAt(i));
      }//end if
    }//end for


    if(historyVector.size()>MAX_HISTORY_LENGTH) {
      maximumHistoryCount = MAX_HISTORY_LENGTH;
    } else {
      maximumHistoryCount = historyVector.size();
    }//end if

    out.put("dasItems", maximumHistoryCount+"");

    for (int i=0; i < maximumHistoryCount; i++) {
      if (historyVector.elementAt(i) != null &&
          !historyVector.elementAt(i).equals(CHOOSE_STRING)) {
        if (!out.contains(historyVector.elementAt(i)))
          out.put("dasItem"+i, (String) historyVector.elementAt(i));
      }//end if
    }//end for

    //Now that we've filled in the contents of the vectors,
    //switch on the listeners.

    serverList.addItemListener(server_listener);
    historyList.addActionListener(history_listener);

    //
    //finally, copy in the current proxy-usage settings into the history
    String proxySet = System.getProperty("http.proxySet");
    String proxyHost = System.getProperty("http.proxyHost");
    String proxyPort = System.getProperty("http.proxyPort");

    if(proxySet != null) {
      out.put("dasUseProxy", proxySet);
    } else {
      out.put("dasUseProxy", "none");
    }//end if

    if(proxyHost != null) {
      out.put("dasProxyHost", proxyHost);
    } else {
      out.put("dasProxyHost", "none");
    }//end if

    if(proxyPort != null) {
      out.put("dasProxyPort", proxyPort);
    } else {
      out.put("dasProxyPort", "none");
    }//end if

    return out;
  }

  /** Detect valid DAS/2 servers and populate serverVector.
   *  For now, this just fills in the DAS server returned by Config.getDASServer()
   *  (which can be set with a DASServer line in apollo.cfg) */
  private void findServers() {
    logger.debug("DAS2AdapterGUI.findServers: adding " + Config.getDASServer());
    serverVector.addElement(Config.getDASServer());
  }

    /** Not currently used */
//   public void setFont(Font font) {
//     /*
//     super.setFont(font);
//     if (serverLabel != null)
//         serverLabel.setFont(font);
//     if (dsnLabel != null)
//         dsnLabel.setFont(font);
//     if (segLabel != null)
//         segLabel.setFont(font);
//     if (serverList != null)
//         serverList.setFont(font);
//     if (versionedSourcesList != null)
//         versionedSourcesList.setFont(font);
//     if (segList != null)
//         segList.setFont(font);
//     if (lowTextBox != null)
//         lowTextBox.setFont(font);
//     if (lowLabel != null)
//         lowLabel.setFont(font);
//     if (hiTextBox != null)
//         hiTextBox.setFont(font);
//     if (hiLabel != null)
//         hiLabel.setFont(font);
//     if (historyList != null)
//         historyList.setFont(font);
//     if (historyLabel != null)
//         historyLabel.setFont(font);
//     */
//   }

  public void finishGUIForReading() {

    Dimension listBoxSize = new Dimension(500,20);
    Dimension textBoxSize = new Dimension(140,20);
    Dimension labelSize = new Dimension(150,20);

    setPathListWidth(600);

    setForeground (SystemColor.text);
    setBackground (SystemColor.window);

    serverList.setPreferredSize(listBoxSize);
    serverList.setMinimumSize(listBoxSize);
    serverList.setEditable(true);
    versionedSourcesList.setPreferredSize(listBoxSize);
    versionedSourcesList.setMinimumSize(listBoxSize);
    segList.setPreferredSize(listBoxSize);
    segList.setMinimumSize(listBoxSize);
    historyList.setPreferredSize(listBoxSize);

    lowTextBox.setPreferredSize(textBoxSize);
    lowTextBox.setMinimumSize(textBoxSize);
    hiTextBox.setPreferredSize(textBoxSize);
    hiTextBox.setMinimumSize(textBoxSize);

    hiTextBox.addKeyListener (posValidator);
    lowTextBox.addKeyListener (posValidator);

    // On some platforms, this was resulting in white text on a white background.
    // Let label color default to black.
//     serverLabel.setForeground (SystemColor.activeCaptionText);
//     dsnLabel.setForeground (SystemColor.activeCaptionText);
//     segLabel.setForeground (SystemColor.activeCaptionText);
//     historyLabel.setForeground (SystemColor.activeCaptionText);
//     lowLabel.setForeground (SystemColor.activeCaptionText);
//     hiLabel.setForeground (SystemColor.activeCaptionText);

    pane = new JPanel();
    pane.setLayout(new GridBagLayout());
    pane.setBackground (SystemColor.window);
    pane.setForeground (SystemColor.text);

    // Put history box ABOVE the other fields.  Otherwise, you tend not to
    // notice it until you've already filled in the other fields.
    pane.add(historyLabel, GuiUtil.makeConstraintAt (0, 0, 1));
    pane.add(historyList, GuiUtil.makeConstraintAt (1, 0, 3));
    pane.add(serverLabel, GuiUtil.makeConstraintAt (0, 1, 1));
    pane.add(serverList, GuiUtil.makeConstraintAt (1, 1, 3));
    pane.add(dsnLabel, GuiUtil.makeConstraintAt (0, 2, 1));
    pane.add(versionedSourcesList, GuiUtil.makeConstraintAt (1, 2, 3));
    pane.add(segLabel, GuiUtil.makeConstraintAt (0, 3, 1));
    pane.add(segList, GuiUtil.makeConstraintAt (1, 3, 3));
    pane.add(lowLabel, GuiUtil.makeConstraintAt (0, 4, 1));
    pane.add(lowTextBox, GuiUtil.makeConstraintAt (1, 4, 1));
    pane.add(hiLabel, GuiUtil.makeConstraintAt (2, 4, 1));
    pane.add(hiTextBox, GuiUtil.makeConstraintAt (3, 4, 1));

    pane.add(getProxyLabel(), GuiUtil.makeConstraintAt (0, 5, 1));
    pane.add(getProxyTextField(), GuiUtil.makeConstraintAt (1, 5, 2));
    pane.add(getProxyButton(), GuiUtil.makeConstraintAt (3, 5, 2));

    add(pane);
  }

  public String getSelectedServer() {
    return (String) serverList.getSelectedItem();
  }

  /**
   * This returns an actual DASDsn - either the source id or the 
   * MapMaster attribute will give the URL of the annotated source.
  **/
  public DASDsn getSelectedDSN() {
    logger.debug("getSelectedDSN: " + versionedSourcesList.getSelectedItem());
    return (DASDsn) versionedSourcesList.getSelectedItem();
  }

  public DASSegment getSelectedSegment() {
    DASSegment selectedSegment = (DASSegment) segList.getSelectedItem();
    logger.debug("DAS2AdapterGUI.getSelectedSegment: segment = " + selectedSegment);
    return selectedSegment;
  }

  public String getSelectedHistory() {
    String selectedHistory = (String) historyList.getSelectedItem();
    if (selectedHistory == null) {
      selectedHistory = (String) historyList.getItemAt(0);
    }
    return selectedHistory;
  }

  protected void updateVersionedSources () {
    DASDsn dsn = null;
    java.util.List theDSNList;
    Iterator dsnIterator;

    //
    //This should remove all the dsns and entry points drop-downs - even if
    //the new url makes no sense, these have to be empty
    versionedSourcesVector.removeAllElements();

    try {
      //If the url is non-null and meaningful,
      //create a new DSN list, starting with an empty DSN
      serverList.removeItemListener(server_listener);
      historyList.removeActionListener(history_listener);

      //If "Choose..." is still part of the list of available URL's at this point,
      //get rid of it - the user has been prompted once already.
      ((MutableComboBoxModel)serverList.getModel()).removeElement(CHOOSE_STRING);
      ((MutableComboBoxModel)historyList.getModel()).removeElement(CHOOSE_STRING);

      if(
        getSelectedServer() != null &&
        getSelectedServer().trim().length() > 0 &&
        !getSelectedServer().equals(CHOOSE_STRING)
      ) {
        setDASServer(new DAS2Request(getSelectedServer()));
	logger.debug("DAS2AdapterGUI.updateVersionedSources: set das server to " + getSelectedServer());

        // Get list of versioned sources (as SimpleDSNs)
        theDSNList = getDASServer().getDSNs();
        dsnIterator = theDSNList.iterator();

        // !! Why add an empty one first?
        // (I gather this is because otherwise it thinks you've already picked
        // the first one, and doesn't register your selection)
        versionedSourcesVector.addElement(new SimpleDASDsn("","","","",""));
        while (dsnIterator.hasNext()) {
          logger.debug("Adding versioned source to list: " + dsn);
          versionedSourcesVector.addElement(dsnIterator.next());
        }//end while
      }//end if

      versionedSourcesList.setModel(new DefaultComboBoxModel(versionedSourcesVector));

      serverList.addItemListener(server_listener);
      historyList.addActionListener(history_listener);

    } catch(NonFatalDataAdapterException theException) {
      logger.error(theException.getMessage(), theException);
      //
      //make sure we have the listeners back in, in case
      //anything went wrong - don't want them in twice, though
      serverList.removeItemListener(server_listener);
      serverList.addItemListener(server_listener);
      historyList.removeActionListener(history_listener);
      historyList.addActionListener(history_listener);

      displayDialog(
        "The URL you provided could not provide DSN's - source message:"+
        theException.getMessage()
      );
    }//end try

  }//updateVersionedSources


  protected void updateSegments () {
    DASDsn dsn = getSelectedDSN();
    Iterator segmentIterator = null;
    segmentVector = new Vector();
    java.util.List segmentList = null;

    logger.debug("DAS2AdapterGUI.updateSegments: selected dsn = " + dsn);
    if (dsn != null && dsn.getSourceId() != null) {
      segmentList = getDASServer().getEntryPoints(dsn);
    }//end if

    if (segmentList != null) {
      segList.setEditable (false);
      segmentIterator = segmentList.iterator();
      while(segmentIterator.hasNext()) {
        DASSegment segment = (DASSegment) segmentIterator.next();
        segmentVector.addElement(segment);
      }//end while
    }
    else {
      segList.setEditable (true);
    }//end if

    segList.setModel(new DefaultComboBoxModel(segmentVector));
  }//end updateSegments

    /** Returns true if inputs look ok */
  private boolean validateInputs() {
    String lowText = lowTextBox.getText().trim();
    String highText = hiTextBox.getText().trim();
    int lowTextInt;
    int highTextInt;

    if(
      driver == null
      ||
      getSelectedDSN() == null
      ||
      getSelectedSegment() == null
    ) {
      throw new apollo.dataadapter.NonFatalDataAdapterException(
        "Attempt to fetch features from DAS server without "+
        "specifying adapter, dsn or segment"
      );
    }//end if

    if(lowText == null ||
       lowText.length() <= 0 ||
       highText == null ||
       highText.length() <= 0
       ) {
      // FOR NOW--just for debugging
      logger.warn("low/high not set--setting to 1-999");
      lowText = "1";
      highText = "999";
      // UNCOMMENT after testing
      //      throw new NonFatalDataAdapterException("Low/High range must be specified");
    }//end if

    try {
      lowTextInt = Integer.parseInt(lowText);
      highTextInt = Integer.parseInt(highText);
    } catch(NumberFormatException theException) {
      throw new NonFatalDataAdapterException("Low/High ranges are not in integer format");
    }

    // Also check that requested range is legal (not longer than chromosome)
    // and not too big for apollo
    if (!regionIsReasonable(lowTextInt, highTextInt)) {
	String warning = "Error: the requested range, " + lowTextInt + "-" + highTextInt + ", is not reasonable.";
	logger.warn(warning);
	JOptionPane.showMessageDialog(null,warning,"Warning",JOptionPane.WARNING_MESSAGE);
	return false;  // ?
    }
    else
      logger.debug("Region is reasonable: " + lowTextInt + "-" + highTextInt);

    return true;  // looks ok
  }//end validateInputs

    private boolean regionIsReasonable(int low, int high) {
      // ! Should also check that high is not beyond range of requested region
      if (low <= 0 || high <= 1)
	  return false;
      if (high-low > MAX_REGION_SIZE)
	  return false;
      return true;
  }

    /** This is where the real work starts--we read the data or write it. */
  public Object doOperation(Object values) throws org.bdgp.io.DataAdapterException {
    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      try {
        //        DASSegment theSegment = getSelectedSegment();
        //        setSegment(theSegment);
        //        logger.debug("DAS2AdapterGUI.doOperation");
          
	// Check if the requested range is reasonable.  If not, return null.
	// Unfortunately, this then results in Apollo exiting because nothing was loaded.
	// It would be nice to give the user another chance.
        if (!validateInputs())
	    return null;
        driver.setStateInformation(createStateInformation());
        CurationSet out = ((ApolloDataAdapterI) driver).getCurationSet();
        //        apollo.dataadapter.debug.DisplayTool.showFeatureSet(out.getResults());
        return out;
      } catch(NonFatalDataAdapterException exception){
        throw new apollo.dataadapter.ApolloAdapterException("Problem loading data: "+exception.getMessage(),exception);
      }
    }
    else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      driver.setInput(getSelectedPath());
      ApolloDataI apolloData = (ApolloDataI)values;
      CurationSet curSet=null;
      if (apolloData.isCurationSet()) {
        curSet = apolloData.getCurationSet();
      } else if (apolloData.isCompositeDataHolder()) {
        curSet = apolloData.getCompositeDataHolder().getCurationSet(0);
      }
      //      driver.commitChanges(curSet, saveAnnots.isSelected(), saveResults.isSelected());
      driver.commitChanges(curSet);
      return null;
    } else {
      return null;  // not supported
    }
  }//end doOperation
  
  private void setPropertyIfNotNull(StateInformation props, String key, String value){
    if(value != null){
      props.setProperty(key, value);
    }
  }

  class LongValidator extends KeyAdapter {
    public void keyTyped(KeyEvent evt) {
      JTextField field = (JTextField)evt.getSource();

      if (!Character.isDigit(evt.getKeyChar()) &&
          !Character.isISOControl(evt.getKeyChar())) {
        evt.consume();
        _beep();
      }
    }
  }

  private void _beep() {
    byte []  beep = new byte[1];
    beep[0] = 007;
    System.out.print(new String(beep));
  }

  public class ServerListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateVersionedSources();
    }
  }

  public class DSNListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      updateSegments();
    }
  }

  /**
   * Sets the input model to be that of the list.
   * Additionally, if the input list contains the item, then select it.
   * Otherwise, add it into the list.
  **/
  protected void setSelection (Object item, Vector model, JComboBox list) {
    int index = -1;

    for (int i = 0; i < model.size() && (index < 0); i++) {
      if ((model.elementAt (i)).equals (item)) {
        index = i;
      }
    }//end for

    if (index < 0) {
      model.insertElementAt (item, 0);
      index = 0;
    }//end if

    list.setModel(new DefaultComboBoxModel(model));
    list.setSelectedIndex (index);
  }

  protected void setSelectedSourceWithId(String id, Vector model, JComboBox list) {
    int index = -1;

    for (int i = 0; i < model.size() && (index < 0); i++) {
      if (((DASDsn)(model.elementAt (i))).getSourceId().equals (id)) {
        index = i;
      }
    }//end for

    list.setModel(new DefaultComboBoxModel(model));

    if (index >= 0) {
      list.setSelectedIndex (index);
    }//end if
  }

  protected void setSelectedSegmentWithId(String id, Vector model, JComboBox list) {
    int index = -1;

    for (int i = 0; i < model.size() && (index < 0); i++) {
      if (((DASSegment)(model.elementAt (i))).getId().equals (id)) {
        index = i;
      }
    }//end for

    logger.debug("DAS2AdapterGUI.setSelectedSegmentWithId(" + id + ")");
    list.setModel(new DefaultComboBoxModel(model));

    if (index >= 0) {
      list.setSelectedIndex (index);
    }//end if
  }

  public class HistoryListener implements ActionListener {
    public void actionPerformed(ActionEvent evt) {
      String item = (String) historyList.getSelectedItem();
      StringTokenizer tokenizer;
      String server;
      String dsn;
      String segment;
      int low = 0;
      int hi = 0;

      // New order of things in history:
      // segment dsn low high server
      if(item != null &&
        item.trim().length() > 0 &&
        !item.trim().equals(CHOOSE_STRING)) {
        tokenizer = new StringTokenizer(item);
        try {
          dsn = tokenizer.nextToken();
          segment = tokenizer.nextToken();

	  low = Integer.parseInt(tokenizer.nextToken());
	  logger.debug("DAS2AdapterGUI.historyListener.actionPerformed: low = " + low);
	  hi = Integer.parseInt(tokenizer.nextToken());

          server = tokenizer.nextToken();

          setSelection (server, serverVector, serverList);

          //This will recreate the versionedSourcesVector which is the model
          //for the versionedSourcesList combobox.
          updateVersionedSources();

          setSelectedSourceWithId(
            dsn,
            versionedSourcesVector,
            versionedSourcesList
          );

          //
          //This will recreate the segmentVector, which is the model
          //for the segList comboBox
          updateSegments();

          setSelectedSegmentWithId(segment, segmentVector, segList);

          if(low > 0) {
            lowTextBox.setText(low + "");
            hiTextBox.setText(hi + "");
          }//end if
        }
        catch (Exception e) {
          logger.error("Failed parsing location string " + item, e);
        }//end try
      }//end if
    }//end actionPerformed
  }//end HistoryListener


    //  private DASServerI getDASServer() {
  private DAS2Request getDASServer() {
    return dasServer;
  }//end getDASServer

    //  private void setDASServer(DASServerI theServer) {
  private void setDASServer(DAS2Request theServer) {
    dasServer = theServer;
  }//end setDASServer

  private JButton getProxyButton() {
    return proxyButton;
  }//end getProxyButton

  private JLabel getProxyLabel() {
    return proxyLabel;
  }//end getProxyLabel

  private JTextField getProxyTextField() {
    return proxyTextField;
  }//end getProxyTextField

  /**
   * This listener gets fired when the user wants to set a Proxy Server for the session
  **/
  public class
        ProxyListener
        implements
    ActionListener {
    public void actionPerformed(ActionEvent theEvent) {
      //
      //The default dialog that's opened is MODAL - so the execution thread
      //blocks till the dialog is dismissed.
      ProxyDialog pd = new ProxyDialog(null);
      pd.setVisible(true);

      //
      //Now the user has made a choice of proxy settings (which have been
      //set into the System properties), so we need to update the text
      //field with the chosen settings.
      Properties systemProperties = System.getProperties();
      String useProxy = systemProperties.getProperty("http.proxySet");
      String proxyPort = systemProperties.getProperty("http.proxyPort");
      String proxyHost = systemProperties.getProperty("http.proxyHost");

      updateProxyTextField(
        useProxy,
        proxyHost,
        proxyPort
      );
    }//end actionPerformed
  }//end ProxyListener


  private void updateProxyTextField(
    String useProxy,
    String proxyHost,
    String proxyPort
  ) {
    if(proxyHost != null && proxyHost.trim().length() > 0 && !proxyHost.equals("none")) {
      getProxyTextField().setText(proxyHost+":"+proxyPort+". Use? "+useProxy);
    } else {
      getProxyTextField().setText(null);
    }//end if
  }//end updateProxyTextFieldWithSystemProperties

  /**
   * Pop up a modal information dialog
  **/
  private void displayDialog(String message) {
    JOptionPane.showMessageDialog(this, message, "Error Loading DSNs", JOptionPane.ERROR_MESSAGE);
  }


  /**
   * For writing DAS2XML
  **/

}
