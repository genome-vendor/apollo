package apollo.dataadapter.analysis;

import java.util.Vector;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import apollo.gui.GenericFileAdapterGUI;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.TierProperty;
import apollo.datamodel.CurationSet;
import apollo.dataadapter.DataInputType;
import apollo.analysis.filter.AnalysisFilterI;
import apollo.analysis.filter.FilterPanel;
import apollo.analysis.filter.AnalysisFilter;
import apollo.analysis.filter.AnalysisInput;
import apollo.util.GuiUtil;

/**
 * This is only used by AnalysisAdapterGUI. It was an inner class of it, but
 * it seems to have outgrown innerclassness, so I broke it out into its own
 * class
 * AnalysisPanel class represents a panel within the JTabbedPane
 * For now this is hardwired with one JComboBox to get input from
 * change later if have different kinds of panels 
 */
public class AnalysisPanel extends JPanel {
  private String label;
  protected JComboBox fileBox;
  protected JTextField seqField;
  protected JComboBox displayTierBox;
  protected JComboBox displayTypeBox;
  public JTextField offsetField;
  private Box box;
  private String example;
  protected AnalysisAdapterGUI parent;
  protected CurationSet curation;
  protected static String seq_label = "Query sequence file (FASTA format)";
  protected static String tier_label = "Tier ";
  protected static String type_label = "Type ";
  protected static String file_prop = "file";
  protected static String seq_prop = "seq";
  protected static String offset_label = "Offset";
  // Default type to use for showing new analysis results (needs to be
  // defined in tiers file for this to work)
  protected static String DEFAULT_TYPE = "New analysis results";
  protected static String DEFAULT_TIER = "New analysis results";

  // all this for tracking history
  // one set of properties that are associated with each tier
  private HashMap tier_history = new HashMap (20);
  // one set of properties that are associated with each file, for each tier
  private HashMap file_history = new HashMap (100);
  private String most_recent_tier;
  private String most_recent_file;
  private String historic_file;
  private boolean history_is_initialized = false;

  // this should be in AbstractDataAdapterUI?
  static final short DEFAULT_MAX_HISTORY_LENGTH = 10;

  // the appropriate parser for this type of analysis
  protected AnalysisParserI parser;

  // likewise the appropriate filter for this type of analysis
  protected AnalysisFilterI filter;
  protected FilterPanel filterPanel;

  AnalysisPanel(AnalysisAdapterGUI parent, 
		String name,
		AnalysisParserI parser,
		FilterPanel filter_panel) {
    this.parent = parent;
    this.curation = parent.getCurationSet();
    setName(name);
    this.label = name + " output file ";
    this.parser = parser;
    this.filter = new AnalysisFilter();
    this.filterPanel = filter_panel;
    filter_panel.setAnalysisPanel(this);
    this.setBackground (filter_panel.getBackground());
    buildGUI();
  }

  void addActionListener(ActionListener l) {
    if (fileBox != null)
      fileBox.addActionListener(l);
  }

  public AnalysisParserI getParser() {
    return parser;
  }

  public AnalysisFilterI getFilter() {
    return filter;
  }

  protected short getMaxHistoryLength() {
    return DEFAULT_MAX_HISTORY_LENGTH;
  }

  protected void setInputs (AnalysisInput in) {
    in.setTier ((String) displayTierBox.getEditor().getItem());
    in.setType ((String) displayTypeBox.getEditor().getItem());
    //    System.out.println ("AnalysisPanel.setInputs: tier is " + in.getTier() + "; Type is " + in.getType()); // DEL
    filterPanel.setInputs (in);
    if (isNewCuration()) {
      in.setOffset ("", 1);
    }
    else {
      // The offset defaults to 0 (not the start of curation) if the query 
      // was not a piece of the current region.
      if (!(in.queryIsGenomic()))
        in.setOffset (offsetField.getText(), 0);
      else
        in.setOffset (offsetField.getText(), curation.getStart());
    }
  }

  protected boolean isNewCuration() {
    return curation == null;
  }

  protected String getCurrentFile () {
      return most_recent_file;
  }

  protected String getCurrentSeq () {
    return (isNewCuration() ?
	    seqField.getText() :
	    null);
  }

  protected String getCurrentTier () {
    if (most_recent_tier == null)
      most_recent_tier = (String) displayTierBox.getEditor().getItem();
    return most_recent_tier;
  }

  /** Fills in the data structure with all of the values
   * that the user has selected
   */
  protected AnalysisInput getCurrentInput() {
    String input_src = getCurrentFile();
    AnalysisInput in = null;
    if (input_src != null) {
      in = new AnalysisInput ();
      setInputs (in);
    }
    return in;
  }

  public DataInputType getInputType() {
    String filename = getCurrentFile();
    if (filename != null && filename.startsWith("http"))
      return DataInputType.URL;
    else
      return DataInputType.FILE;
  }

  /** Tool tip is the same as label - is that lame? */
  private String getToolTip() {
    return label;
  }

  protected void buildGUI() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    JLabel jLabel = GuiUtil.makeJLabelWithFont(label);
      
    box = new Box(BoxLayout.Y_AXIS);

    Box browsebox = new Box(BoxLayout.X_AXIS);
    browsebox.add(jLabel);
    fileBox = new JComboBox();
    fileBox.setPreferredSize(new Dimension(500,15));
    fileBox.setEditable(true);
    browsebox.add(fileBox);
    browsebox.add(Box.createHorizontalGlue());
    fileBox.addItemListener (new AnalysisPanel.fileItemListener());
    JButton browseOutputButton = new JButton("Browse...");
    browseOutputButton.addActionListener( new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  browseFiles();
	}
      }
					  );
    browsebox.add(browseOutputButton);
    box.add (browsebox);
    box.add (Box.createVerticalGlue());

    if (isNewCuration()) {
      browsebox = new Box(BoxLayout.X_AXIS);
      seqField = new JTextField("");
      seqField.setEditable(true);
      seqField.setPreferredSize(new Dimension(500,30));
      seqField.setMaximumSize(new Dimension(500,30));
      browsebox.add(GuiUtil.makeJLabelWithFont(seq_label));
      browsebox.add(seqField);
      browsebox.add(Box.createHorizontalGlue());

      JButton browseSeqButton = new JButton("Browse...");
      browseSeqButton.addActionListener( new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    browseSeqs();
	  }
	}
					 );
      browsebox.add(browseSeqButton);
      box.add (browsebox);
      box.add (Box.createVerticalGlue());
    }
    addDisplayComponents();
    add(box);

    filterPanel.setVisible(true);
    addToPanel(filterPanel);

  }

  /** This is called from the browse button if it exists, brings up
   * a file browser and puts the selected file in the combo box
   * calls GenericFileAdapterGUI for file browser */
  private void browseFiles() {
    File selectedFile = null;
    String current_file = getCurrentFile();
    if (current_file == null)
      current_file = (String) fileBox.getEditor().getItem();
    if (current_file!= null)
      selectedFile = new File(current_file);
    File browsedFile =
      GenericFileAdapterGUI.fileBrowser(selectedFile,parent);
    if (browsedFile==null)
      return;
    // Stick file name in combo box
    fileBox.configureEditor(fileBox.getEditor(), browsedFile.toString());
    most_recent_file = (String) fileBox.getEditor().getItem();
  }

  /** This is called from the browse button if it exists, brings up
   * a file browser and puts the selected file in the combo box
   * calls GenericFileAdapterGUI for file browser */
  private void browseSeqs() {
    File selectedFile = null;
    String current_file = getCurrentSeq();
    if (current_file == null)
      current_file = seqField.getText();
    if (current_file!= null)
      selectedFile = new File(current_file);
    File browsedFile =
      GenericFileAdapterGUI.fileBrowser(selectedFile, parent);
    if (browsedFile==null)
      return;
    // Stick file name in combo box
    seqField.setText(browsedFile.toString());
  }

  /** Default display type entry component is JComboBox */
  protected void addDisplayComponents() {
    //    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    displayTierBox = new JComboBox();
    displayTierBox.setEditable(true);
    JLabel tierLabel = GuiUtil.makeJLabelWithFont(tier_label);

    displayTypeBox = new JComboBox();
    displayTypeBox.setEditable(true);
    JLabel typeLabel = GuiUtil.makeJLabelWithFont(type_label);

    displayTierBox.addItemListener (new AnalysisPanel.displayTierItemListener());

    panel.add(tierLabel,GuiUtil.makeConstraintAt(0,0,1));
    panel.add(displayTierBox, GuiUtil.makeConstraintAt(1,0,1));
    panel.add(typeLabel, GuiUtil.makeConstraintAt(2,0,1));
    panel.add(displayTypeBox, GuiUtil.makeConstraintAt(3,0,1));

    if (!isNewCuration()) {
      offsetField = new JTextField("0", 12);
      offsetField.setEditable(true);
      JLabel offsetLabel 
	= GuiUtil.makeJLabelWithFont("Genomic offset from " +
                                     + curation.getStart());
      panel.add(offsetLabel, GuiUtil.makeConstraintAt(0,1,1));
      panel.add(offsetField, GuiUtil.makeConstraintAt(1,1,1));
    }
    addToPanel (panel);
  }

  protected void addToPanel(JComponent comp) {
    box.add (Box.createVerticalGlue());
    box.add(comp);
  }

  /**
   * Which file contains the analysis results
   **/
  public class fileItemListener implements ItemListener {
    public void itemStateChanged (ItemEvent event) {
      if (event.getStateChange() == ItemEvent.SELECTED) {
	if (history_is_initialized) {
	  recordCurrentFileProperties();
	  setFileProperties ((String) event.getItem());
	}	
      }
    }
  }

  /**
   * Where will the blast analysis results appear, name that tier.
   **/
  public class displayTierItemListener implements ItemListener {
    public void itemStateChanged (ItemEvent event) {
      if (event.getStateChange() == ItemEvent.SELECTED) {
	if (history_is_initialized) {
	  recordCurrentTierProperties();
	}	
        initTypesList ((String) event.getItem());
        setTierProperties ((String) event.getItem());
      }
    }
  }

  /** Puts the size of history and all the history items into
   * Properties prop
   */
  public Properties getProperties() {

    Properties current_properties = new Properties();
    current_properties.setProperty (tier_label, getCurrentTier());

    recordCurrentTierProperties();

    Iterator e = tier_history.keySet().iterator();
    while (e.hasNext()) {
      String tier_name = (String) e.next();
      Properties tier_props = (Properties) tier_history.get(tier_name);
      parent.addPropertiesWithPrefix (current_properties,
				      tier_name,
				      tier_props);
    }
    return current_properties;
  }

  /** Puts currently selected item at top of history list */
  protected String recordCurrentTierProperties() {
    Properties tier_properties = new Properties();
    String file_name = getCurrentFile();
    if (file_name != null) {
      tier_properties.setProperty (file_prop, file_name);
    }
    String tier = getCurrentTier();
    if (tier != null)
      tier_history.put (getCurrentTier(), tier_properties);

    recordCurrentFileProperties();
    
    Iterator e = file_history.keySet().iterator();
    while (e.hasNext()) {
      file_name = (String) e.next();
      Properties file_properties = (Properties) file_history.get(file_name);
      parent.addPropertiesWithPrefix (tier_properties,
				      file_name,
				      file_properties);
    }
    return file_name;
  }
  
  protected void recordCurrentFileProperties () {
    Properties file_properties = filterPanel.getProperties();
    String type = (String) displayTypeBox.getEditor().getItem();
    file_properties.setProperty (type_label, type);
    if (isNewCuration()) {
      file_properties.setProperty (seq_prop, getCurrentSeq());
    }
    else {
      file_properties.setProperty (offset_label, offsetField.getText());
    }
    file_history.put (getCurrentFile(), file_properties);
  }

  public void initTiersList (String tier) {
    //    System.out.println("AnalysisPanel.initTiersList: tier = " + tier); // DEL
    PropertyScheme scheme = Config.getPropertyScheme();
    Vector tier_scheme = scheme.getAllTiers();
    displayTierBox.removeAllItems();
    for (int i = 0; i < tier_scheme.size(); i++) {
      TierProperty tp = (TierProperty) tier_scheme.elementAt(i);
      displayTierBox.addItem (tp.getLabel());
    }
    if (tier == null) {
      // default to DEFAULT_TIER if none was previously selected
      //      tier = (String) displayTierBox.getItemAt(0);
      //      System.out.println("AnalysisPanel.initTiersList: tier was null, setting to " + DEFAULT_TIER); // DEL
      tier = DEFAULT_TIER;
    }
    displayTierBox.setSelectedItem(tier);
    displayTierBox.configureEditor(displayTierBox.getEditor(), tier);
    most_recent_tier = tier;
    initTypesList(tier);
  }

  public void initTypesList (String tier) {
    displayTypeBox.removeAllItems();
    if (tier != null) {
      PropertyScheme scheme = Config.getPropertyScheme();
      TierProperty tp = (TierProperty) scheme.getTierProperty(tier, false);
      if (tp != null) {
	Vector fp_vect = tp.getFeatureProperties();
	int fp_size = fp_vect.size();
	for (int i = 0; i < fp_size; i++) {
	  FeatureProperty fp = (FeatureProperty) fp_vect.elementAt (i);
	  displayTypeBox.addItem (fp.getDisplayType());
	}
      }
    }
    String type = (String) displayTypeBox.getItemAt(0);
    if (type != null) {
      displayTypeBox.setSelectedItem(type);
      displayTypeBox.configureEditor(displayTypeBox.getEditor(), type);
    }
  }

  /**
   * <p> Walk each property we've been handed. 
   * Strip off the analysis name at the
   * front of the property, and gather the properties into analysis-specific
   * groups. </p>
   *
   * <p> Add to these properties the configuration information in the analysis
   *
   * <p> Call setProperties() on each analysis' adapter panel, 
   * passing in the specific groups we've gathered.</p>
  **/
  public void setProperties(Properties combinedProperties) {
    tier_history.clear();
    parent.setChildProperties (combinedProperties, tier_history);
    history_is_initialized = false;
    initTiersList((String) combinedProperties.get(tier_label));
    setTierProperties ((String) displayTierBox.getEditor().getItem());
    history_is_initialized = true;
  }

  protected void setTierProperties (String tier) {
    // selected tier, type and file
    if (tier != null) {
      Properties combinedTierProperties = (Properties) tier_history.get(tier);
      file_history.clear();
      parent.setChildProperties (combinedTierProperties, file_history);
      Vector file_list = new Vector(file_history.keySet());
      fileBox.setModel(new DefaultComboBoxModel(file_list));
      if (combinedTierProperties != null)
	setFileProperties (combinedTierProperties.getProperty(file_prop));
      else if (most_recent_file != null) {
	fileBox.setSelectedItem(most_recent_file);
	fileBox.configureEditor(fileBox.getEditor(), most_recent_file);
      }
    }
  }

  protected void setFileProperties (String file) {
    if (file != null) {
      // if the user has entered a new file name then
      // don't overwrite it with the one from the history
      if (most_recent_file == null ||
	  (most_recent_file != null &&
	   historic_file != null &&
	   !historic_file.equals("") &&
	   most_recent_file.equals (historic_file))) {
	fileBox.setSelectedItem(file);
	fileBox.configureEditor(fileBox.getEditor(),file);
	// The most recent file is either what was set here,
	// from the history, or what is set by the user by
	// entering it
	most_recent_file = file;
      }
      // The historic file is what is pulled from the history
      historic_file = file;
      Properties fileProperties = (Properties) file_history.get(file);
      if (fileProperties != null) {
        String type = fileProperties.getProperty(type_label);
        //        System.out.println("AnalysisPanel.setFileProperties: using type " + type + " from history"); // DEL
        setCurrentTypeAndTier(type);

	String offset = fileProperties.getProperty(offset_label);
	String seq_file = fileProperties.getProperty(seq_prop);
	if (offset != null && !isNewCuration()) {
	  offsetField.setText(offset);
	}
	else if (isNewCuration() && seq_file != null) {
	  seqField.setText(seq_file);
	}
	if (filterPanel != null)
	  filterPanel.setProperties (fileProperties);
      }
    }
  }

  private void setCurrentTypeAndTier(String type) {
    if (type == null || type.equals("")) {
      //      System.out.println("AnalysisPanel.setCurrentTierAndType: type was null; setting to " + DEFAULT_TYPE); // DEL
      type = DEFAULT_TYPE;
    }
    // Set tier based on type
    String tier_label = Config.getPropertyScheme().getTierProperty(type).getLabel();
    if (tier_label != null) {
      displayTierBox.setSelectedItem(tier_label);
      //      System.out.println("AnalysisPanel.setCurrentTierAndType: set tier=" + tier_label + " from type " + type); // DEL
    }
    displayTypeBox.setSelectedItem(type);
    displayTypeBox.configureEditor(displayTypeBox.getEditor(), type); // Need?
  }

  /** Inserts panel into tabbed pane with name and tooltip at index */
  void insertIntoTabbedPane(JTabbedPane pane,int index) {
    pane.insertTab(getName(),null,this,getToolTip(),index);
    pane.setBackgroundAt (index, getBackground());
  }

}
