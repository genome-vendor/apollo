package apollo.dataadapter.ensj19;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

import apollo.datamodel.*;
import apollo.dataadapter.*;

import org.apache.log4j.*;
import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;

import edu.stanford.ejalbert.*;

import apollo.datamodel.CurationSet;
import apollo.datamodel.GenomicRange;
import apollo.gui.*;
//import apollo.util.SequenceUtil;

import org.ensembl19.util.*;
import org.ensembl19.gui.*;
import org.ensembl19.driver.*;



/**
 * Configuration GUI for the EnsJAdapter class. It enables the user to
 * specify:
 * <ul>
 *
 * <li>a region of a chromosome to retrieve data, or a clone fragment
 * (contig accesion/stable ID).
 *
 * <li>Ensj-driver settings. Where to get data from.
 *
 * <li>Logging configuration for ensj-core. 
 *
 * </ul>
 *
 * <p>This class interacts with the Apollo history framework by getting and
 * setting various history parameters.
 *
 *  <p>Usage: the user should add the line <code>DataAdapterInstall
 * "apollo.dataadapter.ensj.EnsJAdapter"</code> to her apollo.cfg file. The
 * Adaptor and this GUI will then be made available from the initial apollo
 * screen.
 *
 *
 * <p>Originally based on EnsCGIAdaptorGUI.  */
public abstract class EnsJAdapterGUI extends AbstractDataAdapterUI{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(EnsJAdapterGUI.class);

  // default GUI settings
  protected static final String DEFAULT_CONFIG_FILE = "conf/ensj_defaults.conf";
  private static final int MAX_HISTORY_LENGTH = 20;

  private static String HELP_URL = "http://www.ebi.ac.uk/~craig/ensembl-java/apollo-plugin.html";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //  private JButton seqDriverButton;
  //  private LoggingConfPanel loggingConfPanel;

  private JRadioButton chrButton;
  private JComboBox chrStartEndList;
  //private JTextField chrTextBox;
  private JComboBox chrDropdown;
  private JTextField startTextBox;
  private JTextField endTextBox;
  private ActionListener chrAction;
  private Vector chrStartEndVector;

  private JRadioButton cloneFragmentButton ;
  private JComboBox cloneFragmentList;
  private JTextField cloneFragmentTextBox;
  private ActionListener cloneFragmentAction;
  private Vector cloneFragmentHistory;

  private JRadioButton stableIDButton;
  private JComboBox stableIDList;
  //  private ActionListener stableIDAction;
  private Vector stableIDHistory;

  private DataTypeButton[] dataTypeButtons;

  //  private JPanel assemblyLocationPanel;
  private Box cloneFragmentPanel;

  private DataAdapter adapter;
  private IOOperation op;

  private Properties initialSettings;

  //  private Properties seqDriverProperties = new Properties();
  
  
  /** 
   * Holds the logging file parameter passed in from the history file. An
   * exception will be thrown in createStateInformation() if this is null.
  **/
  private String loggingFile;
  
  public JPanel locationPanel;
  
  private boolean chromosomeListInitialised = false;

  public class DataTypeButton extends JCheckBox {

    public String propertyName;
    public String adaptorName;

    public DataTypeButton(
      String label,
      String adaptorName,
      String propertyName
    ){
      super(label);
      this.propertyName = propertyName;
      this.adaptorName = adaptorName;
    }
  }

  /** 
   * This class clears out chromosome dd,
   * leaving the initialisation taks to the dd's popup menu listener
  **/
  public class DataSourceChangeListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      
      getChrDropdown().setModel(new DefaultComboBoxModel());
      setChromosomeListInitialised(false);

    }//end actionPerformed
  }//end DataSourceChangeListener
  
  
  /**
   * When the user pops up the chromosome dropdown, it is populated - 
   * this listener makes the call to the driver, in turn to retrieve
   * the chromsome list.
  **/
  public class ChromosomeDropdownPopupListener implements PopupMenuListener{
    public void popupMenuCanceled(PopupMenuEvent event){}
    public void popupMenuWillBecomeInvisible(PopupMenuEvent event){}
    public void popupMenuWillBecomeVisible(PopupMenuEvent event){
      EnsJAdapterGUI.this.initialiseChromosomeDropdown();
    }//end popupMenuWillBecomeVisible
  }//end ChromosomeDropdownPopupListener

  
  /**
   * A focus listener which sets button.selected(true) when focus gained.
   */
  private final class FocusSelectsButton extends FocusAdapter {
    
    private AbstractButton button;

    FocusSelectsButton(AbstractButton button) {
      this.button = button;
    }

    public void focusGained(FocusEvent ke) {
      button.setSelected( true );
    }
  }


  public EnsJAdapterGUI(IOOperation op) {
    this.op = op;


    chrButton = new JRadioButton("Chromosome", false);
    cloneFragmentList = new JComboBox();
    stableIDList = new JComboBox();
    chrDropdown = new JComboBox();

    chrDropdown.addPopupMenuListener(new ChromosomeDropdownPopupListener());

    startTextBox = new JTextField();
    endTextBox = new JTextField();
    chrStartEndList = new JComboBox();

    stableIDButton = new JRadioButton("Stable ID", false);

    chrAction = 
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          initialiseChromosomeDropdown();
          //GenomicRange loc = SequenceUtil.parseChrStartEndString((String)chrStartEndList.getSelectedItem());
          Region loc = new Region((String)chrStartEndList.getSelectedItem());
          if (loc != null) {
            chrDropdown.setSelectedItem(loc.getChromosome());
            startTextBox.setText(loc.getStart() + "");
            endTextBox.setText(loc.getEnd() + "");
            chrButton.setSelected(true);
          }
        }
      };

    cloneFragmentButton = new JRadioButton("Clone Fragment", false);
    cloneFragmentTextBox = new JTextField();
    cloneFragmentAction = 
      new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          cloneFragmentTextBox.setText( (String)cloneFragmentList.getSelectedItem() );
          cloneFragmentButton.setSelected( true );
        }
      };

    setDataTypeButtons(
      new DataTypeButton[] {
        new DataTypeButton(
          "Genes", 
          "gene",
          StateInformation.INCLUDE_GENE
        ),
        new DataTypeButton(
          "Dna Protein Alignments", 
          "dna_protein_alignment",
          StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT
        ),
        new DataTypeButton(
          "Dna Dna Alignments", 
          "dna_dna_alignment",
          StateInformation.INCLUDE_DNA_DNA_ALIGNMENT
        ),
        new DataTypeButton(
          "Features (simple)", 
          "feature",
          StateInformation.INCLUDE_FEATURE
        ),
        new DataTypeButton(
          "Simple Peptides", 
          "simple_peptide_feature",
          StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE
        ),
        new DataTypeButton(
          "Repeats", 
          "RepeatMask",
          StateInformation.INCLUDE_REPEAT_FEATURE
        ),
        new DataTypeButton(
          "PredictionTranscripts", 
          "prediction_transcript",
          StateInformation.INCLUDE_PREDICTION_TRANSCRIPT
        ),
        new DataTypeButton(
          "Variations (e.g. SNPs)", 
          "variation",
          StateInformation.INCLUDE_VARIATION
        )
      }
    );

    buildGUI();
  }



  /**
   * Construct a vector containing all the values of properties with a key
   * that begins with _prefix_. The indexes should begin at 0 and be
   * consecutive. Example key/value format expected:
   * <pre>
   * cloneFragmentItem0=3333
   * cloneFragmentItem1=55
   * </pre>
  **/
  protected Vector getPrefixedProperties(
    Properties settings, 
    String prefix,
    boolean convertValueToFilePath
  ){
    Vector returnVector = new Vector();
    int i=0;
    String value;
    String filepath;
    
    while(true){
      value = settings.getProperty( prefix + i);
      i++;
      if(value==null){
        break;
      }
      
      if(convertValueToFilePath){
        filepath = findFile(value);
        if ( filepath!=null && new java.io.File(filepath).exists() ){
          value = filepath.toString();
        }
      }

      returnVector.addElement(value);

    }
    return returnVector;
  }

  /**
   * Puts each element from _values_ into _settings_. Each element is given a
   * unique name beginning with _prefix_. An additional element is added to the
   * vector containing the number of elements. This is the inverse of
   * "getPrefixedProperties()" 
  **/
  protected void putPrefixedProperties(
    Properties settings,
    Vector values,
    String prefix
  ){
    int size = values.size();
    if ( size>MAX_HISTORY_LENGTH ){
      size = MAX_HISTORY_LENGTH;
    }
    for(int i=0; i<size; ++i) {
      settings.put(prefix+i, (String)values.elementAt(i));
    }
  }


  /**
   * Attempt to locate and add fully qualified path
   * correspnding to filename to vector.
  **/
  private void addDefault(Vector v, String filename) {
    String filepath = findFile(filename);
    if ( filepath!=null ){
      v.add(filepath);
    }
  }



  /**
   * Loads history values into 'drop down' lists and other GUI
   * components. Uses defaults where necessary.
  **/
  public void setProperties(Properties input) {
    String filepath = findFile( DEFAULT_CONFIG_FILE );
    boolean selected;
    
    //if(filepath!=null){
    //  initialSettings = PropertiesUtil.createProperties( filepath );
    //  logger.debug(
    //    "Initial settings: value of missing ? property: "+initialSettings.getProperty("dna_protein_alignment.supports")
    //  );
    //  initialSettings.putAll(input);
    //}else{

    initialSettings = input;

    //}
    
    // load clone fragment history
    setCloneFragmentHistory(
      getPrefixedProperties(
        initialSettings,
        "cloneFragmentHistory",
        false 
      )
    );
    
    cloneFragmentList.removeActionListener(cloneFragmentAction);
    cloneFragmentList.setModel(new DefaultComboBoxModel(getCloneFragmentHistory()));
    cloneFragmentList.addActionListener(cloneFragmentAction);

    // load stableID history 
    setStableIDHistory(
      getPrefixedProperties(
        initialSettings,
        "stableIDHistory",
        false
      )
    );
    
    stableIDList.setModel(new DefaultComboBoxModel(getStableIDHistory()));
    
    chrStartEndList.removeActionListener(chrAction);
    chrStartEndVector = 
      getPrefixedProperties(
        initialSettings,
        "chrStartEndHistory",
        false
      );

    chrStartEndList.setModel(new DefaultComboBoxModel(chrStartEndVector));
    chrStartEndList.addActionListener(chrAction);

    setLoggingFile(initialSettings.getProperty(StateInformation.LOGGING_FILE));

    for(int i=0; i<getDataTypeButtons().length; ++i) {
      selected = 
        stringToBoolean( 
          initialSettings.getProperty(getDataTypeButtons()[i].propertyName) 
        );
        getDataTypeButtons()[i].setSelected( selected );
    }
  }



  /**
   * @return all of the history information for the drop-down lists.
   */
  public Properties getProperties() {

    // Move currently selected items to top of history lists.

    chrStartEndList.removeActionListener(chrAction);
    Properties out = new Properties();

    if ( chrButton.isSelected() ) {
      
      String selectedLocation = 
        "Chr "
        + ((String)chrDropdown.getSelectedItem()) + " "
        + startTextBox.getText() + " "
        + endTextBox.getText();
      makeFirstElement(selectedLocation, chrStartEndVector);
      putPrefixedProperties(out, chrStartEndVector, "chrStartEndHistory");
      
    } 
    
    else if ( cloneFragmentButton.isSelected() ) {
      
      String selectedCloneFragmentName = cloneFragmentTextBox.getText();
      makeFirstElement(selectedCloneFragmentName, getCloneFragmentHistory());
      putPrefixedProperties(out, getCloneFragmentHistory(), "cloneFragmentHistory");

    } 
    
    else if ( stableIDButton.isSelected() ) {

      String selectedStableID = (String)stableIDList.getSelectedItem();
      makeFirstElement(selectedStableID, getStableIDHistory());
      putPrefixedProperties(out, getStableIDHistory(), "stableIDHistory");
      
    }

    // TODO store parameters from config panel
    
//     Vector driverHistory = driverConfigPanel.getDriverHistory();
//     makeFirstElement(driverConfigPanel.getDriver(), driverHistory);
//     putPrefixedProperties(out, driverHistory, "driverHistory");

//     Vector serverHistory = driverConfigPanel.getServerHistory();
//     makeFirstElement(driverConfigPanel.getServer(), serverHistory);
//     putPrefixedProperties(out, serverHistory, "serverHistory");

//     Vector loggingHistory = loggingConfPanel.getCustomHistory();
//     makeFirstElement(loggingConfPanel.getSelected(), loggingHistory);
//     putPrefixedProperties(out, loggingHistory, "loggingHistory");

    chrStartEndList.addActionListener(chrAction);

    for(int i=0; i<getDataTypeButtons().length; ++i)
      out.put( getDataTypeButtons()[i].propertyName 
               ,booleanToString(getDataTypeButtons()[i].isSelected()) );


    return out;
  }


  static String booleanToString(boolean v) {
    if (v)
      return "true";
    return "false";
  }

  static boolean stringToBoolean(String v) {
    if ( v!=null && v.equals("true") )
      return true;
    return false;
  }



  /**
   * The element is placed at the front of the vector. If it is already
   * present in the vector is moved from that position to the front.
   */
  private void makeFirstElement(Object element, Vector vector) {
    if ( vector.contains(element) ) {
      vector.removeElement(element);
      vector.insertElementAt(element, 0);
    }
    else {
      vector.insertElementAt(element, 0);
    }
  }


  protected TitledBorder createBorder(String label) {
    TitledBorder border = new TitledBorder(new LineBorder(Color.gray), label);
    border.setTitleColor(Color.black);
    return border;
  }


  protected JPanel buildLocationPanel() {

    final int height = 20;
    final int space = 10;
    Dimension labelSize = new Dimension(150, height);
    Dimension textBoxSize = new Dimension(100, height);
    Dimension dropDownListSize = new Dimension(150, height);


    locationPanel = new JPanel();
    Box stableIDPanel = Box.createHorizontalBox();
    Box chromosomePanel = Box.createHorizontalBox();
    Box chrInputRow = Box.createHorizontalBox();
    Box chromosomeInputOptionsPanel = Box.createVerticalBox();
    Box cfOptionsPanel = Box.createVerticalBox();
    cloneFragmentPanel = Box.createHorizontalBox();

    locationPanel.setLayout(new BoxLayout(locationPanel, BoxLayout.Y_AXIS));
    locationPanel.setBorder( createBorder("Region") );

    locationPanel.add(stableIDPanel);
    locationPanel.add(Box.createVerticalStrut(space));
    locationPanel.add(chromosomePanel);
    locationPanel.add(Box.createVerticalStrut(space));
    locationPanel.add(cloneFragmentPanel);


    // Stable ID
    stableIDButton.setPreferredSize( labelSize );
    stableIDList.setMinimumSize( dropDownListSize );
    stableIDPanel.add( stableIDButton );
    stableIDPanel.add( Box.createHorizontalStrut(space) );
    stableIDPanel.add( stableIDList );
    stableIDList.setEditable( true );
    stableIDList.getEditor().getEditorComponent()
      .addFocusListener( new FocusSelectsButton(stableIDButton) );


  
    // Chromosome
    chrButton.setPreferredSize(labelSize);
    chromosomePanel.add(chrButton);
    chromosomePanel.add(Box.createHorizontalStrut(space));
    chromosomePanel.add(chromosomeInputOptionsPanel);
    chromosomeInputOptionsPanel.add(chrInputRow);
    chromosomeInputOptionsPanel.add(Box.createVerticalStrut(space/2));
    chromosomeInputOptionsPanel.add(chrStartEndList);

    // Clone fragment
    Box cfInputRow = Box.createHorizontalBox();
    cloneFragmentButton.setPreferredSize( labelSize );
    cloneFragmentPanel.add( cloneFragmentButton );
    cloneFragmentPanel.add( Box.createHorizontalStrut(space) );
    cloneFragmentTextBox.setPreferredSize( dropDownListSize );
    cloneFragmentTextBox.addFocusListener( new FocusSelectsButton(cloneFragmentButton) );
    //cloneFragmentTextBox.setMaximumSize( dropDownListSize );
    cfInputRow.add( new JLabel("Accession", SwingConstants.RIGHT) );
    cfInputRow.add( cloneFragmentTextBox );
    cfInputRow.add( Box.createHorizontalGlue() ); // don't work?
    cfOptionsPanel.add( cfInputRow );
    cfOptionsPanel.add( Box.createVerticalStrut(space/2) );
    cfOptionsPanel.add( cloneFragmentList );
    cloneFragmentPanel.add( cfOptionsPanel );
    // Make changes in clone fragment text box automatically select clone
    // fragment mode.
    KeyListener selectCloneFragmentListener = new KeyAdapter() {
          public void keyPressed(KeyEvent ke) {
            cloneFragmentButton.setSelected( true );
          }
        };
    cloneFragmentTextBox.addKeyListener( selectCloneFragmentListener );
    // If history item selected then set mode and copy text to input text
    // box.
    cloneFragmentList.addActionListener(cloneFragmentAction);



    // Configure input boxes

    chrDropdown.setPreferredSize(dropDownListSize);
    chrDropdown.setMaximumSize(dropDownListSize);
    chrDropdown.setEditable(false);
    chrInputRow.add(new JLabel("Chr", SwingConstants.RIGHT));
    chrInputRow.add(chrDropdown);

    chrInputRow.add( Box.createHorizontalStrut(space) );
    startTextBox.setPreferredSize( textBoxSize );
    startTextBox.setMaximumSize( textBoxSize );
    startTextBox.setEditable(true);
    chrInputRow.add(new JLabel("Start", SwingConstants.RIGHT));
    chrInputRow.add(startTextBox);

    chrInputRow.add( Box.createHorizontalStrut(space) );
    endTextBox.setPreferredSize( textBoxSize );
    endTextBox.setMaximumSize( textBoxSize );
    endTextBox.setEditable(true);
    chrInputRow.add(new JLabel("End", SwingConstants.RIGHT));
    chrInputRow.add(endTextBox);

    chrStartEndList.setPreferredSize( dropDownListSize );
    chrStartEndList.setEditable( false );

    FocusListener chrListener = new FocusSelectsButton( chrButton );
    chrDropdown.addFocusListener( chrListener );
    startTextBox.addFocusListener( chrListener );
    endTextBox.addFocusListener( chrListener );

    cloneFragmentList.setPreferredSize( dropDownListSize );
    cloneFragmentList.setEditable( false );

    // Link radio buttons
    ButtonGroup group = new ButtonGroup();
    group.add( stableIDButton );
    group.add(chrButton);
    group.add(cloneFragmentButton);

    // Make the chr text boxes automatically select chromosome selection mode.
    KeyListener selectChromosomeListener = new KeyAdapter() {
                                             public void keyPressed(KeyEvent ke) {
                                               chrButton.setSelected( true );
                                             }
                                           };
    chrDropdown.addKeyListener( selectChromosomeListener );
    startTextBox.addKeyListener( selectChromosomeListener );
    endTextBox.addKeyListener( selectChromosomeListener );
    // If history item selected then copy the value into the text box.
    chrStartEndList.addActionListener(chrAction);


    return locationPanel;
  }


  protected JPanel buildIncludePanel() {
    JPanel all = new JPanel();
    all.setBorder( createBorder("Tracks"));
    all.setLayout(new BoxLayout(all, BoxLayout.X_AXIS));

    Box includePanel = Box.createVerticalBox();

    // TODO do this dynamically
    for(int i=0; i<getDataTypeButtons().length; ++i)
      includePanel.add( getDataTypeButtons()[i] );

    all.add( includePanel );
    all.add( Box.createHorizontalGlue() );
    return all;
  }


  
  private JPanel buildHelpPanel() {
    final Component parent = this;
    JPanel p = new JPanel();
    p.setLayout( new FlowLayout( FlowLayout.RIGHT, 0, 0) );

    ActionListener l = new ActionListener() {

                         public void actionPerformed(ActionEvent evt) {

                           String[] browsers
                           = new String[] {
                               apollo.config.Config.getBrowserProgram()
                               ,"netscape"
                               ,"iexplore"
                             };

                           boolean browserOpen = false;
                           for ( int i=0;
                                 !browserOpen &&i<browsers.length;
                                 i++ ) {

                             try {
                               String browser = browsers[i];
                               if (browser!=null)
                                 BrowserLauncher.setBrowser(browser);
                               BrowserLauncher.openURL( HELP_URL );
                               JOptionPane.showMessageDialog(parent,"Opened help page.");

                               browserOpen = true;
                             } catch (IOException err) {}
                           }
                           if ( !browserOpen )
                             JOptionPane.showMessageDialog(parent,
                                                           "Failed to open help page in a browser. Is a browser installed?",
                                                           "Warning",
                                                           JOptionPane.WARNING_MESSAGE);
                         }
                       };

    JButton b = new JButton("Help");
    b.addActionListener( l );
    p.add( b );
    return p;
  }


  protected void buildGUI() {

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    add( buildHelpPanel() );
    add( buildLocationPanel() );
    add( buildIncludePanel() );
    //add( driverConfigPanel );
    //add( buildSeqButtonPanel() );
    //add( loggingConfPanel );
  }


  public void setDataAdapter(DataAdapter adapter) {
    this.adapter = adapter;
  }

  public DataAdapter getDataAdapter(){
    return adapter;
  }

  public String getSelectedStableID() {
    return EnsJAdapter.STABLE_ID_PREFIX 
      + (String)stableIDList.getModel().getSelectedItem();
  }


  protected String getSelectedCloneFragment() {
    return cloneFragmentTextBox.getText();
  }



  public String getSelectedChrStartEnd() {
    String chromosome = getSelectedChr();
    String start = getSelectedStart();
    String end = getSelectedEnd();
    
    if(
      chromosome == null ||
      chromosome.trim().length() <= 0 ||
      chromosome.equals("null")
    ){
      return null;
    }
    
    if(
      start == null ||
      start.trim().length() <= 0 ||
      start.equals("null")
    ){
      return null;
    }

    if(
      end == null ||
      end.trim().length() <= 0 ||
      end.equals("null")
    ){
      return null;
    }
    
    String output = new String("Chr " + chromosome + " " + start + " " + end);
    
    if(output.trim().equals("Chr")){
      return null;
    }else{
      return output;
    }
  }

  public String getSelectedChr() {
    String selectedChr = (String) chrDropdown.getSelectedItem();
    return selectedChr;
  }



  public String getSelectedStart() {
    String selectedStart = (String) startTextBox.getText();

    return selectedStart;
  }



  public String getSelectedEnd() {
    String selectedEnd = (String) endTextBox.getText();
    return selectedEnd;
  }



  public JComboBox getChrDropdown(){
    return chrDropdown;
  }//end getChrDropdown
  
  public JTextField getStartTextBox(){
    return startTextBox;
  }//end getStartTextBox
  
  public JTextField getEndTextBox(){
    return endTextBox;
  }//end getEndTextBox
  
//   protected DriverConfigPanel getDriverConfigPanel(){
//     return driverConfigPanel;
//   }

  /**
   * Trys to locate file using apollo's Config class first and by searching
   * the classpath second.
   * @return filpath to file IF found, otherwise null.
   */
  protected String findFile(String file) {
    String filepath = null;
    if(file != null && file.trim().length() > 0){
      filepath = apollo.util.IOUtil.findFile(file);
      if ( filepath==null ) {
        try {
          java.net.URL url = org.apache.log4j.helpers.Loader.getResource( file );
          if (url!=null ) {
            filepath = url.getFile();
            logger.info("using filepath = " + filepath);
          }
        }catch (Exception e) {} 
      }
    }//end if
    return filepath;
  }
  
  protected ActionListener getChrAction(){
    return chrAction;
  }//end  getChrAction

  public JComboBox getChrStartEndList(){
    return chrStartEndList;
  }//end getChrStartEndList
  

  public JRadioButton getChrButton(){
    return chrButton;
  }//end getChrButton
  
  protected JRadioButton getCloneFragmentButton(){
    return cloneFragmentButton;
  }//end getCloneFragmentButton
  
  public JRadioButton getStableIdButton(){
    return stableIDButton;
  }//end getStableIdButton
  
  protected DataTypeButton[] getDataTypeButtons(){
    return dataTypeButtons;
  }//end getDataTypeButtons
  
  protected void setDataTypeButtons(DataTypeButton[] buttons){
    dataTypeButtons = buttons;
  }//end getDataTypeButtons
  
  protected Properties getInitialSettings(){
    return initialSettings;
  }//end getInitialSettings

  public Box getCloneFragmentPanel(){
    return cloneFragmentPanel;
  }//end getCloneFragmentPanel
  
  public JPanel getLocationPanel(){
    return locationPanel;
  }//end getLocationPanel

  public void setSelectedStableId(String stableId){
    getStableIDHistory().insertElementAt(stableId,0);
    stableIDList.setModel(new DefaultComboBoxModel(getStableIDHistory()));
    stableIDList.setSelectedIndex(0);
  }
  
  public IOOperation getOperation(){
    return op;
  }
  
  public void setLoggingFile(String newValue){
    loggingFile = newValue;
  }
  
  public String getLoggingFile(){
    return loggingFile;
  }
  
  private Vector getCloneFragmentHistory(){
    return cloneFragmentHistory;
  }
  
  private void setCloneFragmentHistory(Vector history){
    cloneFragmentHistory = history;
  }
  
  private Vector getStableIDHistory(){
    return stableIDHistory;
  }
  
  private void setStableIDHistory(Vector history){
    stableIDHistory = history;
  }

  public void initialiseChromosomeDropdown(){
    EnsJAdapter adapter = (EnsJAdapter)getDataAdapter();
    Vector listOfChromosomes = new Vector();
    Properties stateInformation = null;

    // Temporarily removed - reinsert when popupmenulistener is added in - when jdk1.4 is standard
    if(isChromosomeListInitialised()){
      return;
    }//end if

    
    try{
      
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      chrButton.setSelected(true);
      stateInformation = createStateInformation();

      //
      //dummy location passed in to allow us to run a setStateInformation on the 
      //adapter without getting a problem. The dummy is never used...
      stateInformation.setProperty(StateInformation.REGION, "Chr dummy 1 2");
      adapter.setStateInformation(stateInformation);
      
      listOfChromosomes = new Vector(adapter.getChromosomes());
      listOfChromosomes.add(0,"");
      getChrDropdown().setModel(new DefaultComboBoxModel(listOfChromosomes));
      getStartTextBox().setText("");
      getEndTextBox().setText("");
      setChromosomeListInitialised(true);

    }catch(org.bdgp.io.DataAdapterException exception){
      JOptionPane.showMessageDialog(
        null, 
        "I can't load a list of chromosomes for your current DB configuration: "+
        exception.getMessage()
      );
    }catch(org.ensembl19.driver.ConfigurationException exception){
      JOptionPane.showMessageDialog(
        null, 
        "I can't load a list of chromosomes for your current DB configuration"+
        exception.getMessage()
      );
    }finally{
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }//end try
    
  }
  
  private void setChromosomeListInitialised(boolean value){
    chromosomeListInitialised = value;
  }//end setChromosomeListInitialised
  
  protected boolean isChromosomeListInitialised(){
    return chromosomeListInitialised;
  }//end isChromosomeListInitialised
  
  public abstract Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException;
}


