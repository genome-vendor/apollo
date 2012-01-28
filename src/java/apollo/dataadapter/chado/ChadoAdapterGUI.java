package apollo.dataadapter.chado;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.apache.log4j.*;

import org.bdgp.io.IOOperation;
import org.bdgp.swing.AbstractDataAdapterUI;

import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.Region;

// need for querying for chroms/seqIds
import apollo.dataadapter.chado.jdbc.JdbcChadoAdapter;
import apollo.dataadapter.chado.jdbc.ChadoInstance; // -> chado?

/**
 * In the Apollo data adapter specification each data adapter has two parts: a 
 * class that implements the graphical user interface, and a class that interacts 
 * with the data source (as directed by the user via the GUI).  This is an 
 * implementation of the graphical user interface for the corresponding adapter 
 * class apollo.dataadapter.chado.ChadoAdapter.
 * 
 * @see ChadoAdapter
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.42 $ $Date: 2008/03/05 18:51:44 $ $Author: gk_fan $
 */
public class ChadoAdapterGUI extends AbstractDataAdapterUI implements ActionListener, ItemListener {

  // -----------------------------------------------------------------------
  // Class/static variables 
  // -----------------------------------------------------------------------
  
    protected final static Logger logger = LogManager.getLogger(ChadoAdapterGUI.class);

  /**
   * NOTE - subclasses of JdbcChadoAdapter must be explicitly added to this list in order to be
   * usable in the Chado adapter.  This is a limitation that could be removed if necessary.
   Yes - this should come from config.
  */
  protected static String DRIVER_CLASSES[] = new String[] { 
    "apollo.dataadapter.chado.jdbc.PostgresChadoAdapter",
    "apollo.dataadapter.chado.jdbc.SybaseChadoAdapter"
  };

  /**
   * Prompt for the pull-down menu that allows the user to select a Chado database from
   * among those read from the Chado adapter's configuration file.
   */
  protected static String CUSTOM_DB_PROMPT = "Enter database info below or select from list";

  /**
   * Prompt for the area in which the user can enter the ID of the sequence he/she wishes
   * to load.
   */
  protected static String CUSTOM_SEQ_PROMPT = "Enter an ID below:";

  // -----------------------------------------------------------------------
  // Instance variables 
  // -----------------------------------------------------------------------

  /**
   * The IOOperation (e.g. reading/writing) that this object was created to support.
   */
  protected IOOperation operation;

  /**
   * List of Chado databases read by the ChadoAdapter from its configuration file.
   *
   * @see ChadoAdapter.readConfigFile
   */
  protected ChadoDatabase[] chado_dbs;

  /** GUI panel for database login and password */
  private JPanel userPanel;
  /** Gui panel for read operation, for querying db. Not needed for write. */
  private JPanel seqQueryPanel;
  // Instance variables to hold the various Swing/AWT UI components
  // GUIs
  // Chado database
  protected JLabel databaseChoicePrompt = new JLabel("Chado database", SwingConstants.RIGHT);
  protected JComboBox databaseComboBox = null;

  // Name of JdbcChadoAdapter subclass used to connect
  protected JLabel driverClassPrompt = new JLabel("Chado driver class: ", SwingConstants.RIGHT);
  protected JComboBox driverClassComboBox = new JComboBox(DRIVER_CLASSES);

  // JDBC URL
  protected JLabel jdbcUrlPrompt = new JLabel("JDBC URL: ", SwingConstants.RIGHT);
  protected JTextField jdbcUrlTextField = new JTextField(30);

  // Database/schema name
  protected JLabel chadoDbPrompt = new JLabel("Database/schema: ", SwingConstants.RIGHT);
  protected JTextField chadoDbTextField = new JTextField(30);

  // Database login
  protected JLabel usernamePrompt = new JLabel("          Login: ", SwingConstants.RIGHT);
  /** Where logins are entered - make this a dropdown history list */
  protected JTextField usernameTextField = new JTextField(30);

  // Database password
  protected JLabel passwordPrompt = new JLabel("       Password: ", SwingConstants.RIGHT);
  protected JPasswordField passwordTextField = new JPasswordField(30);

  // TO DO - the list of sequence types would ideally be read from the database's cvterm table
  // (but this might require hierarchical cvterms in order to identify all sequence-related types)
  // for now we should config these. assembly and supercontig are tigr, 
  // golden_path_region is fly scaffolds
  // Sequence type 
  //  protected String seqTypes[] = new String[] {"gene", "golden_path_region",  "assembly", "supercontig"};
  // For 7/2004 release, just gene and golden path region 
  protected String seqTypes[] = new String[] {"gene", "golden_path_region"};
  protected JLabel seqTypePrompt = new JLabel("Type of region: ", SwingConstants.RIGHT);
  protected JComboBox seqTypeComboBox = new JComboBox(seqTypes);

  // List of sequences and the button that refreshes it
  protected JLabel seqChoicePrompt = new JLabel("Region: ", SwingConstants.RIGHT);
  protected JComboBox seqChoiceComboBox = null;
  protected JButton seqChoiceButton = new JButton("Get " + seqTypes[0] + " list");

  // Sequence id
  protected JLabel seqIdPrompt = new JLabel("Region ID (e.g. gene name): ", SwingConstants.RIGHT);
  /** Where seq ids are entered - this should be changed to a drop down list of
      history */
  protected JTextField seqIdTextField = new JTextField();
  // To specify start position
  private JLabel startLabel = new JLabel("Start:");
  private JTextField startField = new JTextField();
  private JLabel endLabel = new JLabel("End:");
  private JTextField endField = new JTextField();
  // To list chromosomes
  private JLabel chromosomeLabel = new JLabel("Choose Chromosome:");
  private JComboBox chromosomeComboBox = new JComboBox();

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  /**
   * @param operation         The IOOperation that this GUI was invoked to perform.
   * @param chado_dbs  A list of Chado database descriptors from which the user may choose.
   */
  public ChadoAdapterGUI(IOOperation operation, ChadoDatabase chado_dbs[]) {
    this.operation = operation;
    this.chado_dbs = chado_dbs;
    this.makeUI();
  }
  
  /**
   * If this JPanel is not displayed in the parent component, setVisible(boolean) will
   * not have any effect. So seqQueryPanel will be displayed if it is never displayed
   * for writing operation.
   */
  public void doLayout() {
    validateSeqQueryPanel();
    super.doLayout();
  }
  
  private void validateSeqQueryPanel() {
    if (seqQueryPanel == null)
      return;
    if (operation.equals(ApolloDataAdapterI.OP_READ_DATA))
      seqQueryPanel.setVisible(true);
    else if (operation.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      seqQueryPanel.setVisible(false);
    }        
  }
  
  void setOperation(IOOperation operation) {
    if (operation == this.operation)
      return; // already set for this operation
    this.operation = operation;
    validateSeqQueryPanel();
  }

  // -----------------------------------------------------------------------
  // AbstractDataAdapterUI
  // -----------------------------------------------------------------------

  private final static String dbLabelPropName = "dbLabel";
  private final static String loginPropName = "Login";
  private final static String SEQ_ID_PROP_NAME = "SequenceID";
  private final static String seqTypePropName = "SequenceType";
  private final static String startPropName = "Start";
  private final static String endPropName = "End";

  /** Need a height of 200 to accomodate different UI's for each db. May or 
      may not have login. If switch from not having to having a login panel,
      if the height is not explicitly set the newly added login panel will get
      squeezed out(as window size is not changing on db change). 
      Is there a better way to do this? Will need to make height larger for
      databases that provide all the fields (tigr) - for release fixing at 200. */
      //el - 550x220 is not big enough to display the UI correctly and causes
      //certain components to appear disabled, requiring a manual screen
      //resize to get things to work correctly - 600x300 seems to work ok
      //so will change it to it for now, but we really should think of a way
      //to lay this out so that resizing the panel won't be necessary
      //every time we change the UI
  public Dimension getPreferredSize() { 
    return new Dimension(600,300); 
  }

  // Partially implemented.  The adapter uses this information to extract
  // the user's previous choices from the Apollo history file.
  public void setProperties(Properties props) {
    String dbLabel = props.getProperty(dbLabelPropName);
    logger.debug("setProperties called with dbLabel = " + dbLabel);

    if (dbLabel != null) 
      databaseComboBox.setSelectedItem(dbLabel);

    // only set login from props if adapter config allows it to be edited
    String login = props.getProperty(loginPropName);
    if ((login != null) && (usernameTextField.isEditable())) usernameTextField.setText(login);

    // db specific seq type history
    setSequenceType(props.getProperty(getDbSpecificPropName(seqTypePropName)));
    String seqId = props.getProperty(getSeqIdPropName());
    logger.debug("setProperties seqId = " + seqId);
    if (seqId != null) {
      chromosomeComboBox.setSelectedItem(seqId);
      seqIdTextField.setText(seqId);
    }

    setStart(props.getProperty(getDbSpecificPropName(startPropName)));
    setEnd(props.getProperty(getDbSpecificPropName(endPropName)));
  }
  
  /** db label and seq type must be set first */
  private String getSeqIdPropName() {
    return getDbSpecificPropName(SEQ_ID_PROP_NAME) + ":" + getSelectedSeqType();
  }

  private String getDbSpecificPropName(String propName) {
    return propName + ":" + getDbLabel();
  }

  // The properties returned by this method are written to the Apollo history file.
  public Properties getProperties() {
    Properties props = new Properties();
    props.put(dbLabelPropName,getDbLabel());
    props.put(loginPropName,getUsername());
    // seq type history per database
    String typePropName = getDbSpecificPropName(seqTypePropName);
    props.put(typePropName,getSelectedSeqType());
    // prop name for the main field for type (gene,bac,chrom,scaffold...)
    // this prop is specicific both for database and type
    props.put(getSeqIdPropName(),getSequenceId()); // check for null?
    if (getStart() != null)
      props.put(getDbSpecificPropName(startPropName),getStart());
    if (getEnd() != null)
      props.put(getDbSpecificPropName(endPropName),getEnd());
    return props; 
  }

  // The main method supported by the GUI; called when the user clicks on "OK" to read annotation,
  // write sequence, or do whatever operation is appropriate (so long as it is supported by the 
  // adapter.)
  public Object doOperation(Object values) throws ApolloAdapterException {
    ChadoAdapter adapter = getAndInitAdapter();
    IOOperation op = this.getOperation();

    // Need to setDatabase before new style gets set to have DatabaseToStyle work
    // may want to use db label in databaseComboBox instead. chadoDb is the actual db
    // name on the server - one could imagine having multiple server with db names 
    // in common
    adapter.setDatabase(getChadoDbString());

    // This is the only operation supported so far.
    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {
      return adapter.getCurationSet();
    } 
    else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {
      adapter.commitChanges(values);
      return null;
    }
    else if (op.equals(ApolloDataAdapterI.OP_READ_SEQUENCE)) {
      logger.error(this.getClass() + ".doOperation: OP_READ_SEQUENCE unsupported");
      throw new apollo.dataadapter.ApolloAdapterException("Unsupported operation");
     }
    else if (op.equals(ApolloDataAdapterI.OP_READ_RAW_ANALYSIS)) {
      logger.error(this.getClass() + ".doOperation: OP_READ_RAW_ANALYSIS unsupported");
      throw new apollo.dataadapter.ApolloAdapterException("Unsupported operation");
    }
    else if (op.equals(ApolloDataAdapterI.OP_APPEND_DATA)) {
      logger.error(this.getClass() + ".doOperation: OP_APPEND_DATA unsupported");
      throw new apollo.dataadapter.ApolloAdapterException("Unsupported operation");
    }
    else {
      throw new apollo.dataadapter.ApolloAdapterException("Unsupported operation");
    }
  }

  // -----------------------------------------------------------------------
  // ActionListener
  // -----------------------------------------------------------------------

  /**
   * A list of chado feature.uniquenames read from the current database by the ChadoAdapter.
   */
  String sequenceUniquenames[];

  public void actionPerformed(ActionEvent e) {

    // seqChoiceButton
    if (e.getSource() == seqChoiceButton) {
      // If we have valid connection info. tell the adaptor to query the database for a list of sequences
      try {
        ChadoAdapter adapter = getAndInitAdapter();
        Vector uniquenames = new Vector();
        Vector descriptions = new Vector();
        adapter.getSequenceList(getSelectedSeqType(), uniquenames, descriptions);
        int nn = uniquenames.size();

        // Populate the seqChoiceComboBox with the retrieved sequence descriptions
        this.clearSeqChoiceComboBox();

        for (int i = 0;i < nn;++i) {
          String descr = (String)(descriptions.elementAt(i));
          this.seqChoiceComboBox.addItem(descr);
        }
        if (nn > 0) { this.seqChoiceComboBox.setEnabled(true); }

        this.sequenceUniquenames = new String[nn];
        uniquenames.copyInto(sequenceUniquenames);
      } catch (ApolloAdapterException dae) {
        // TO DO - generate a popup with an informative error message
      }
    }
  }

  // -----------------------------------------------------------------------
  // ItemListener
  // -----------------------------------------------------------------------

  public void itemStateChanged(ItemEvent e) {

    // seqChoiceComboBox - allows the user to choose a sequence
    if (e.getItemSelectable() == seqChoiceComboBox) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        Object selections[] = seqChoiceComboBox.getSelectedObjects();
        int selection = seqChoiceComboBox.getSelectedIndex();

        // CUSTOM_SEQ_PROMPT is always index 0
        if (selection == 0) {
          // Make the sequence ID entry box editable
          this.seqIdTextField.setEditable(true);
        } 
        // User has chosen a sequence from the list (read from the db.); 
        // place its ID in the ID entry box and make it non-editable.
        else if (selection > 0) {
          String newval = this.sequenceUniquenames[selection - 1];
          this.seqIdTextField.setEditable(false);
          this.seqIdTextField.setText(newval);
        }
      }
    }
    // seqTypeComboBox - allows the user to choose a Chado sequence type (e.g. 'assembly', 'super-contig'
    else if (e.getItemSelectable() == seqTypeComboBox) {
      this.clearSeqChoiceComboBox();
      
      // When the sequence type changes, so must the button used to retrieve all sequences of that type.
      //      Object selections[] = seqTypeComboBox.getSelectedObjects();
      //      if ((selections != null) && (selections.length == 1)) {
      //	String newval = selections[0].toString();
      //	this.seqChoiceButton.setText("Get " + newval + " list");
      //      }
      String type = (String) seqTypeComboBox.getSelectedItem();
      //if (type != null && type.equalsIgnoreCase("chromosome")) {
      if (isLocationQuery()) {
        setChromosomeGUIVisible(true);
        setSeqIDGUIVisible(false);
      }
      else {
        setChromosomeGUIVisible(false);
        setSeqIDGUIVisible(true);
      }
    }

    // databaseComboBox - allows the user to choose a chado database
    else if (e.getItemSelectable() == databaseComboBox) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
  Object selections[] = databaseComboBox.getSelectedObjects();
    
  if ((selections != null) && (selections.length == 1)) {
    String newval = selections[0].toString();
          newDatabaseSelected(newval,true);
  }
      }
    }
  }

  /** User has selected newDb (dblabel), set up all the database fields associated
      with newDb
      @param changeUI whether to change UI now that db has changed 
  */
  private void newDatabaseSelected(String newDb,boolean changeUI) {
    // "Custom" database selected; allow the user to edit the connection info. fields
    // currently "Custom" is not being used. delete? is it good to have?
    if (newDb.equals(CUSTOM_DB_PROMPT)) {
      this.driverClassComboBox.setEnabled(true);
      this.jdbcUrlTextField.setEditable(true);
      this.chadoDbTextField.setEditable(true);
    } 
    else {

      // should we keep this db around as state for the chado adapter?
      // then we dont have to extract all these things at getCurSet time
      ChadoDatabase db =  getChadoDatabaseByName(newDb);
      
      // A database from the config. file has been chosen; set the connection info. 
      // fields and make them non-editable.
      if (db != null) {
        this.driverClassComboBox.setEnabled(false);
        this.jdbcUrlTextField.setEditable(false);
        this.chadoDbTextField.setEditable(false);
        this.driverClassComboBox.setSelectedItem(db.getAdapterClassName());
        this.jdbcUrlTextField.setText(db.getJdbcUrl());
        this.chadoDbTextField.setText(db.getChadoDb());

        // set default login specified in db, if any
        if (db.hasLogin()) {
          this.usernameTextField.setText(db.getLogin());
        } 
        // otherwise clear the text field
        else {
          this.usernameTextField.setText("");
        }

        // do the same thing for the password field
        String pw = db.getPassword();
        if (pw != null) {
          this.passwordTextField.setText(pw);
        }
        else {
          this.passwordTextField.setText("");
        }

        // set editability of username/password fields based on db config
        this.usernameTextField.setEditable(db.getAllowLoginInput());
        this.passwordTextField.setEditable(db.getAllowPasswordInput());
        
        // Have to reset sequence types
        //List seqTypes = db.getSeqTypes();
        seqTypeComboBox.removeAllItems();
        //if (seqTypes != null) {
        ChadoInstance chadoInstance = db.getChadoInstance();
        for (int i=0; i<chadoInstance.getSeqTypesSize(); i++) {
          //for (Iterator it = seqTypes.iterator(); it.hasNext();)
          seqTypeComboBox.addItem(chadoInstance.getSeqType(i).getName());
        }
        // this is taken care of in setLocationTopLevelSeqIdList
//         // Set the chromosomes for select
//         List chromosomes = db.getChromosomes();
//         //List
//         chromosomeComboBox.removeAllItems();
//         if (chromosomes != null) {
//           for (Iterator it = chromosomes.iterator(); it.hasNext();)
//             chromosomeComboBox.addItem(it.next());
//         }
      }
    }
    // redo UI as user panel disappears if new db has login configged
    if (changeUI) {
      makeReadDataUI(); // presumptious
      //repaint();
      validate();
      validateTree();
      repaint();
    }
  }

  // -----------------------------------------------------------------------
  // ChadoAdapterGUI - protected methods
  // -----------------------------------------------------------------------

  /**
   * Retrieve one of the objects in <code>this.chado_dbs</code> by name.
   *
   * @param name  The human-readable name of one of the databases in <code>chado_dbs</code>
   * @return      The named Chado database descriptor.
   */
  protected ChadoDatabase getChadoDatabaseByName(String name) {
    if (this.chado_dbs != null) {
      int nc = this.chado_dbs.length;
      for (int i = 0;i < nc;++i) {
  if (this.chado_dbs[i].getName().equals(name)) return this.chado_dbs[i];
      }
    }
    return null;
  }

  /**
   * Retrieve the corresponding ChadoAdapter object (stored by the superclass), coerce it
   * to the correct type, and initialize it based on the user's current selections.
   * 
   * @return An initialized instance of ChadoAdapter.
   */
  private ChadoAdapter getAndInitAdapter() throws ApolloAdapterException {
//     ChadoAdapter adapter = null;

//     // Sanity check - we're forced to accept any adapter in setAdapter() but the
//     // Apollo data adapter spec. suggests that each GUI will work only with the
//     // corresponding adapter class.
//     if (this.driver instanceof ChadoAdapter) {
//       adapter = ((ChadoAdapter)this.driver);
//     } else {
//       throw new apollo.dataadapter.ApolloAdapterException("ChadoAdapterGUI not compatible with adapter " + this.driver);
//     }

//     ChadoDatabase selectedDb = getSelectedDatabase();
//     // if (custom) selectedDb = makeCustomDatabase(); // need to add this!
//     if (selectedDb == null) // can this happen?
//       throw new ApolloAdapterException("No database selected(or custom - fix this)");

    ChadoAdapter adapter = setUpChadoAdapter();

    IOOperation op = getOperation();

    // READ
    if (op.equals(ApolloDataAdapterI.OP_READ_DATA)) {

      DataInput dataInput;
      // For location type - types with locations should be configured somehow
      if (isLocationQuery()) {
        String startString = startField.getText();
        String endString = endField.getText();
        try {
          String type = getSelectedSeqType();
          dataInput = new DataInput(getSequenceId(),startString,endString,type);
        }
        catch (RuntimeException e) { throw new ApolloAdapterException(e.getMessage()); }
      }
      else { // not a location
        // for now sequence type has to correlate with a SO type in DataInputType
        dataInput = new DataInput(getSelectedSeqType(),getSequenceId());
      }
      adapter.setDataInput(dataInput);

    }

    // WRITE
    // Only url, dbuser, and dbpwd is needed for writing
    // i guess nothing special needs doing anymore for write, taken care of in chado cd?
    //else if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA)) {}

    //adapter.setStateInformation(params);
//     selectedDb.setLogin(getUsername());
//     selectedDb.setPassword(getPassword());
//     adapter.setActiveDatabase(selectedDb);
    return adapter;
  }

  /** init sans input (thought i needed this for chrom but actually dont but its nice
      to have it separated out anyways */
  private ChadoAdapter setUpChadoAdapter() throws ApolloAdapterException {
    ChadoAdapter adapter = null;

    // Sanity check - we're forced to accept any adapter in setAdapter() but the
    // Apollo data adapter spec. suggests that each GUI will work only with the
    // corresponding adapter class.
    if (this.driver instanceof ChadoAdapter) {
      adapter = ((ChadoAdapter)this.driver);
    } else {
      throw new apollo.dataadapter.ApolloAdapterException("ChadoAdapterGUI not compatible with adapter " + this.driver);
    }

    ChadoDatabase selectedDb = getSelectedDatabase();
    // if (custom) selectedDb = makeCustomDatabase(); // need to add this!
    if (selectedDb == null) // can this happen?
      throw new ApolloAdapterException("No database selected(or custom - fix this)");

    selectedDb.setLogin(getUsername());
    selectedDb.setPassword(getPassword());
    adapter.setActiveDatabase(selectedDb);
    return adapter;

  }


  /** Returns true if DataInputType recognizes SO seqType as a location. 
      location queries should be configured 
      actually now im thinking you may want start & end on anything, so this 
      should just be a configurable - rather than hard wiring what types are 
      "locations" and which arent 
      Although it does bring up the question would you only want a location on 
      a top level feature - but theres lots of top level feats out there isnt there? */
  private boolean isLocationQuery() {
    // DataInputType already recognizes chromosom as location - do it there
    //return getSequenceType().equalsIgnoreCase("chromosome");
    if (getSelectedSeqType() == null)
      return false; // seq type not selected yet - cant say

    if (getSelectedDatabase().getChadoInstance().typeHasStartAndEnd(getSelectedSeqType()))
      return true;
    
    // phase this out i think?
    DataInputType d = DataInputType.getDataTypeForSoType(getSelectedSeqType());
    if (d == null)
      return false;
    return d.isLocation();
  }
  
//   private void setInputType(ApolloDataAdapterI adapter) {
//     DataInputType type = DataInputType.getSoType(getSequenceType());
//     adapter.setInputType(type);
//   }

//   /**
//    * A helper to allow "," for input coordinates in chromosome location.
//    * @param tf
//    * @return
//    */
//   private String getValueInIntegerField(JTextField tf) {
//     String txt = tf.getText().trim();
//     int index = txt.indexOf(',');
//     if (index < 0)
//       return txt;
//     else {
//       return txt.replaceAll(",", "");
//     }
//   }

  /**
   * Create the UI appropriate for <code>this.op</code>.
   */
  protected void makeUI() {

    // Pull-down menu for predefined database choices
    int nc = this.chado_dbs.length /* + 1 */; // no custom db prompt
    if (nc == 0)
      nc = 1;
    String[] databaseChoices = new String[nc];
    // CUSTOM_DB_PROMPT has to be 1st at the moment as CAGUI doesnt set up with other dbs
    // if they initially show up in chooser 1st
    //databaseChoices[0] = CUSTOM_DB_PROMPT;
    // just doing this so my db comes up for my debugging ease - ok im lazy - 
    // we can switch this back at some point
    // config primary choice? - also may not want generic CUSTOM_DB_PROMPT?
    // 6/23/04: commenting out for release--it's just confusing.  --NH
    //    databaseChoices[nc-1] = CUSTOM_DB_PROMPT;
    for (int i = 0;i < chado_dbs.length;++i) {
      //databaseChoices[i+1] = chado_dbs[i].getName();
      databaseChoices[i] = chado_dbs[i].getName();
    }
    if (this.chado_dbs.length == 0)
      databaseChoices[0] = "No dbs available--conf/chado-adapter.xml may be missing";
    this.databaseComboBox = new JComboBox(databaseChoices);

    // Pull-down menu for sequences (if the user retrieves a list of sequences from the db.)
    this.seqChoiceComboBox = new JComboBox();
    this.clearSeqChoiceComboBox();

    // ----------------------------------------------
    // Event handling
    // ----------------------------------------------
    databaseComboBox.addItemListener(this);
    // hoping to get it to fire event to set up other db fields that get set up 
    // on selection
    //databaseComboBox.setSelectedItem(databaseComboBox.getSelectedItem());
    seqTypeComboBox.addItemListener(this);
    seqChoiceComboBox.addItemListener(this);
    seqChoiceButton.addActionListener(this);

    // OP_READ_DATA
    if (getOperation().equals(ApolloDataAdapterI.OP_READ_DATA)) {
      makeReadDataUI();
    }
    // Nothing else supported yet
//     else {
//       logger.error(this.getClass() + ": makeUI() called with unsupported operation " + this.operation);
//     }
    // in case theres no history yet propigate item 0 as selected db
    // Should do after all GUIs are set up to make all displaying correct.
    newDatabaseSelected(databaseChoices[0],false);
  }

  /**
   * Creates the user interface for OP_READ_DATA.
   */
  protected void makeReadDataUI() {

    removeAll();
  
    // The various Swing UI components are added as children of the current object, 
    // which itself subclasses the appropriate top-level Swing component (probably
    // JFrame or JPanel)

    Insets i0 = new Insets(0,0,0,0);
    Insets ir4 = new Insets(0,0,0,4);
    int y = 0;
    Border lineborder = BorderFactory.createLineBorder(Color.gray);

    // ----------------------------------------------
    // Database panel - choose a Chado database
    // ----------------------------------------------
    JPanel dbPanel = new JPanel();
    dbPanel.setLayout(new GridBagLayout());
    Border dbTitle = BorderFactory.createTitledBorder(lineborder, "Chado database");
    dbPanel.setBorder(dbTitle);

    dbPanel.add(databaseChoicePrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
    dbPanel.add(databaseComboBox, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));

    // Just commented all customizable fields. They are not working anywhere.
//    // dont need all this stuff if FlyBase - keeping it simple
//    if (!Config.getChadoInstance().equalsIgnoreCase("FlyBase")) {
//      dbPanel.add(driverClassPrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//      dbPanel.add(driverClassComboBox, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//
//      dbPanel.add(jdbcUrlPrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//      dbPanel.add(jdbcUrlTextField, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//	
//      // Database/schema name (interpretation may vary by platform; in Sybase this is the database name)
//      dbPanel.add(chadoDbPrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//      dbPanel.add(chadoDbTextField, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//    }

    // ----------------------------------------------
    // Login panel - enter login and password
    // ----------------------------------------------
    y = 0;
    userPanel = new JPanel();
    userPanel.setLayout(new GridBagLayout());
    Border userTitle = BorderFactory.createTitledBorder(lineborder, "Username and password");
    userPanel.setBorder(userTitle);

    // Username 
    userPanel.add(usernamePrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
    userPanel.add(usernameTextField, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));

    // Password
    userPanel.add(passwordPrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
    userPanel.add(passwordTextField, new GridBagConstraints(1,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));

    // ----------------------------------------------
    // Sequence panel - enter/choose sequence
    // ----------------------------------------------
  
    // TO DO 
    //  -add support for retrieving/viewing a subsequence of the specified sequence in Apollo
    //  -add support for retrieving data on demand

    y = 0;
    seqQueryPanel = createSeqQueryPanel();
    Border seqTitle = BorderFactory.createTitledBorder(lineborder, "Select a region to display");
    seqQueryPanel.setBorder(seqTitle);
//    seqQueryPanel = new JPanel();
//    seqQueryPanel.setLayout(new GridBagLayout());
//    GridBagConstraints constraints = new GridBagConstraints();
//    constraints.anchor = GridBagConstraints.EAST;
//    // Leave a little space between components
//    constraints.insets = new Insets(1, 2, 1, 2);
//    Border seqTitle = BorderFactory.createTitledBorder(lineborder, "Select a region to display");
//    seqQueryPanel.setBorder(seqTitle);
//
//    // Sequence type (currently "assembly" is the only option)
//    //seqQueryPanel.add(seqTypePrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//    seqQueryPanel.add(seqTypePrompt, constraints);
//    //seqQueryPanel.add(seqTypeComboBox, new GridBagConstraints(1,y,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//    constraints.gridx = 1;
//    constraints.anchor = GridBagConstraints.WEST;
//    seqQueryPanel.add(seqTypeComboBox, constraints);
//    // Commenting out for 7/2004 release
//    //    seqQueryPanel.add(seqChoiceButton, new GridBagConstraints(2,y++,1,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//    //    seqQueryPanel.add(seqChoicePrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//    //    seqQueryPanel.add(seqChoiceComboBox, new GridBagConstraints(1,y++,2,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//
//    // Sequence ID
//    //seqQueryPanel.add(seqIdPrompt, new GridBagConstraints(0,y,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.NONE,ir4,0,0));
//    constraints.gridx = 0;
//    constraints.gridy = 1;
//    constraints.anchor = GridBagConstraints.EAST;
//    seqQueryPanel.add(seqIdPrompt, constraints);
//    //seqQueryPanel.add(seqIdTextField, new GridBagConstraints(1,y++,2,1,0.0,0.0,GridBagConstraints.WEST,GridBagConstraints.NONE,i0,0,0));
//    constraints.gridx = 1;
//    constraints.anchor = GridBagConstraints.WEST;
//    seqQueryPanel.add(seqIdTextField, constraints);
//
    // ----------------------------------------------
    // Main GUI layout
    // ----------------------------------------------
    y = 0;
    this.setLayout(new GridBagLayout());
    Insets betweenInsets = new Insets(4, 4, 4, 4);
    this.add(dbPanel, new GridBagConstraints(0,y++,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,betweenInsets,0,0));
    this.add(userPanel, new GridBagConstraints(0,y++,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,betweenInsets,0,0));
    this.add(seqQueryPanel, new GridBagConstraints(0,y++,1,1,0.0,0.0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,betweenInsets,0,0));
  }
  
  private JPanel createSeqQueryPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.EAST;
    // Leave a little space between components
    Insets inset1 = new Insets(1, 2, 1, 2);
    constraints.insets = inset1;
    panel.add(seqTypePrompt, constraints);
    constraints.gridx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    panel.add(seqTypeComboBox, constraints);
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.anchor = GridBagConstraints.EAST;
    panel.add(seqIdPrompt, constraints);
    constraints.gridx = 1;
    constraints.anchor = GridBagConstraints.WEST;
    seqIdTextField.setColumns(20);
    panel.add(seqIdTextField, constraints);
    // Specify start and end for location type
    Insets inset2 = new Insets(1, 12, 1, 2);
    constraints.insets = inset2;
    constraints.anchor = GridBagConstraints.EAST;
    constraints.gridx = 2;
    constraints.gridy = 0;
    panel.add(startLabel, constraints);
    constraints.gridx = 3;
    constraints.insets = inset1;
    startField.setColumns(12);
    panel.add(startField, constraints);
    constraints.gridx = 2;
    constraints.gridy = 1;
    constraints.insets = inset2;
    panel.add(endLabel, constraints);
    constraints.gridx = 3;
    endField.setColumns(12);
    constraints.insets = inset1;
    panel.add(endField, constraints);
    // For chromosome
    constraints.gridx = 0;
    constraints.gridy = 1;
    panel.add(chromosomeLabel, constraints);
    constraints.gridx = 1;
    panel.add(chromosomeComboBox, constraints);
    return panel;
  }
  
  private void setChromosomeGUIVisible(boolean isVisible) {
    startLabel.setVisible(isVisible);
    startField.setVisible(isVisible);
    endLabel.setVisible(isVisible);
    endField.setVisible(isVisible);
    chromosomeLabel.setVisible(isVisible);
    setLocationTopLevelSeqIdList();
    chromosomeComboBox.setVisible(isVisible);
  }

  // chromosome list renamed
  private void setLocationTopLevelSeqIdList() {
    
    // This method actually gets fired off when clearing out seq types due to action
    // listeners, in which case there are no seq types, null check tests for this
    if (getSelectedSeqType() == null)
      return;
    
    // check if listing has actually changed? most likely only one listing
    //seqChoiceComboBox.removeAllItems();
    chromosomeComboBox.removeAllItems();
    // if this queries for chroms/seqIds, need to connect up jdbc connection
    //connectUpJdbc();
    List locTopLevelSeqIds = null;
    try {
      ChadoDatabase db = getSelectedDatabase();
      String seqType = getSelectedSeqType();
      //JdbcChadoAdapter jdbcAdap = null;

      // actually if we dont have a db connection this will fail
//       if (db.queryForSeqIds(seqType)) {
        
//         //jdbcAdap = getAndInitAdapter().getJdbcAdapter(); // throws ex
//         //setUpChadoAdapter(); // throws aaEx

//       }

      // shouldn't this throw an exception???
      locTopLevelSeqIds = db.getLocationTopLevelSeqIds(seqType);
      // this aint good err msg as only return values for certain seq types
//       if (locTopLevelSeqIds.size() == 0)
//         System.out.println("No features returned for type "+seqType+" either "
//                            +"check db for "+seqType+" in cvterm and feature tables or ammend"
//                            +" your config file (chado-adapter.xml)");
                          
      if (false) throw new ApolloAdapterException(""); // temp til get ex in above
      //adapter.getLocationTopLevelSeqIds(getSelectedDatabase(),getSelectedSequenceType());
    }
    catch (ApolloAdapterException e) {
      // no big deal - may not have data adapter yet (init time)
      locTopLevelSeqIds = new ArrayList(1);
      locTopLevelSeqIds.add("No db connection");
    }
    int size = locTopLevelSeqIds.size();
    for (int i=0; i<size; i++)
      chromosomeComboBox.addItem(locTopLevelSeqIds.get(i));
  }

  private void setSeqIDGUIVisible(boolean isVisible) {
    seqIdPrompt.setVisible(isVisible);
    seqIdTextField.setVisible(isVisible);
  }

  /**
   * @return The IOOperation this UI was instantiated to perform.
   */
  protected IOOperation getOperation() { return this.operation; }

  /**
   * Method that extracts the currently-selected value of a JComboBox.
   *
   * @param cb  The aforementioned JComboBox
   * @return    The currently-selected String, or null if there is not a unique String-valued current selection.
   */
  protected String getComboBoxValue(JComboBox cb) {
    Object sel = cb.getSelectedItem();
    return ((sel != null) && (sel instanceof String)) ? (String)sel : null;
  }

  /**
   * Reset the seqChoiceComboBox to its original state; called when the current database changes.
   */
  protected void clearSeqChoiceComboBox() {
    this.seqChoiceComboBox.removeAllItems();
    this.seqChoiceComboBox.addItem(CUSTOM_SEQ_PROMPT);
    this.seqIdTextField.setEditable(true);
    this.seqChoiceComboBox.setEnabled(false);
  }

  // Retrieve current user input from UI components

  private String getDbLabel() { return databaseComboBox.getSelectedItem().toString(); }
  protected String getDriverClass() { return getComboBoxValue(this.driverClassComboBox); }
  protected String getJdbcUrl() { return this.jdbcUrlTextField.getText(); }
  private String getChadoDbString() { return this.chadoDbTextField.getText(); }
  protected String getUsername() { return this.usernameTextField.getText(); }
  private boolean usernameIsConfigged() {
    if (getSelectedDatabase()==null)
      return false;
    return getSelectedDatabase().hasLogin();
  }
  protected String getPassword() { return new String(this.passwordTextField.getPassword()); }
  
  private void setStart(String start) {
    if (startField == null)
      return;
    startField.setText(start);
  }
  private String getStart() {
    if (startField == null)
      return null;
    return startField.getText();
  }

  private void setEnd(String end) {
    if (endField == null)
      return;
    endField.setText(end);
  }
  private String getEnd() {
    if (endField == null)
      return null;
    return endField.getText();
  }

  protected String getSelectedSeqType() {
    return getComboBoxValue(this.seqTypeComboBox);
  }
  private void setSequenceType(String type) { 
    if (type==null) return;
    seqTypeComboBox.setSelectedItem(type); 
  }

  protected String getSequenceId() { 
    //String type = getSelectedSequenceType();
    if (isLocationQuery()) //type.equalsIgnoreCase("chromosome"))
      return (String) chromosomeComboBox.getSelectedItem();
    else
      return this.seqIdTextField.getText(); 
  }

  /** Returns database select in databaseComboBox. Returns null if nothing
      selected (is that possible) or if the selected database is the "custom"
      database. We need a method that constructs a ChadoDatabase from "custom"
      input. At the moment custom is not used (is it?), so we are ok */
  private ChadoDatabase getSelectedDatabase() {
    Object selections[] = databaseComboBox.getSelectedObjects();
    if ((selections != null) && (selections.length == 1)) {
      String newval = selections[0].toString();
      
      if (!newval.equals(CUSTOM_DB_PROMPT)) {
        return getChadoDatabaseByName(newval);
      } 
    }
    return null;
  }
}
