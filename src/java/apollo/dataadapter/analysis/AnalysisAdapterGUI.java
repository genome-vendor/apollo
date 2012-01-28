package apollo.dataadapter.analysis;

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

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapter;
import org.bdgp.swing.AbstractDataAdapterUI;
import org.bdgp.swing.AbstractIntDataAdapUI;
import java.util.Properties;

// is it a scandal for apollo.dataadapter to import apollo.gui
import apollo.config.Config;
import apollo.datamodel.CurationSet;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInputType;
import apollo.analysis.filter.AnalysisFilterI;
import apollo.analysis.filter.BlastFilterPanel;
import apollo.analysis.filter.GenscanFilterPanel;
import apollo.analysis.filter.NoFilterPanel;
import apollo.analysis.filter.Sim4FilterPanel;
import apollo.analysis.filter.AnalysisInput;


/** Most of the smarts here are actually in AnalysisPanel and its subclasses
 * This just holds the game panels in its tabbed pane */

public class AnalysisAdapterGUI extends org.bdgp.swing.AbstractIntDataAdapUI {

  private static int tabbedIndex = 0;
  private IOOperation op;
  /** The only driver that works with this is the BlastAdapter */
  private AnalysisAdapter analysisDriver;
  private JTabbedPane tabbedPane;
  private final static String indexPropString = "AnalysisTabIndex";
  // used for basal offset when appending new data
  private CurationSet curation_set;

  /** The order of this array is the order that the panels will appear in
   * the JTabbedPane */
  private AnalysisPanel[] analysisPanels;

  public AnalysisAdapterGUI(IOOperation op, CurationSet curation) {
    this.op = op;
    this.curation_set = curation;
    initPanels();
    buildGUI();
    // setBackground(Config.getDataLoaderBackgroundColor());
    setBackground(Color.white);
    setForeground(Color.black);
  }

  /** The order of the analysisPanels array is the order that the panels will appear in
   * the JTabbedPane - */
  private void initPanels() {
    // alice blue
    Color pane_color = new Color(240, 248, 255);
    AnalysisPanel blast_run 
      = new AnalysisPanel(this, 
			  "BLAST",
			  new BlastParser(),
			  new BlastFilterPanel(pane_color));
    // lavender blush
    pane_color = new Color(244, 240, 245);
    AnalysisPanel sim4_run
      = new AnalysisPanel(this,
			  "Sim4",
			  new Sim4Parser(),
			  new Sim4FilterPanel(pane_color));
    // pale green
    pane_color = new Color(152, 251, 152);
    AnalysisPanel blat_run
      = new AnalysisPanel(this,
			  "blat",
			  new BlatParser(),
			  new NoFilterPanel(pane_color));
    // lemon chiffon
    pane_color = new Color(255, 250, 205);
    AnalysisPanel gs_run
      = new AnalysisPanel(this,
			  "Genscan",
			  new GenscanParser(),
			  new GenscanFilterPanel(pane_color));
    // peach puff
    pane_color = new Color(255, 218, 185);
    AnalysisPanel fg_run
      = new AnalysisPanel(this,
			  "Fgenesh",
			  new FgeneshParser(),
			  new NoFilterPanel(pane_color));
    // misty rose
    pane_color = new Color(255, 228, 225);
    AnalysisPanel tRNA_run
      = new AnalysisPanel(this,
			  "tRNAscanSE",
			  new tRNA_Parser(),
			  new NoFilterPanel(pane_color));
    // light cyan
    pane_color = new Color(224, 255, 255);
    AnalysisPanel rp_run
      = new AnalysisPanel(this,
			  "RepeatMasker",
			  new RpMaskerParser(),
			  new NoFilterPanel(pane_color));

    analysisPanels = new AnalysisPanel[] { blast_run, 
					   sim4_run, 
					   blat_run,
					   fg_run, 
					   gs_run,
					   tRNA_run,
					   rp_run };
    for (int i=0; i<analysisPanels.length; i++) {
      ReturnCommit returnCommit = new ReturnCommit(analysisPanels[i]);
      analysisPanels[i].addActionListener(returnCommit);
    }
  }

  /** ActionListener that does a commit("OK") for line return
   * does commit on controllingObject which comes from
   * AbstractItDataAdapUI, only commits if analysis panel approves 
   * This is disabled for now as it commits for list selection as well
   * as return which is annoying, should reeneable if figure out how to distinguish
   * between return and list selection, might need to muck with look and feel?
   */
  class ReturnCommit implements ActionListener {
    AnalysisPanel analysisPanel;
    private ReturnCommit(AnalysisPanel ap) {
      analysisPanel = ap;
    }
    public void actionPerformed(ActionEvent e) {
    }
  }

  /** DataAdapter has to be an AnalysisAdapter, no way to enforce this,
   * will throw cast exception if not Analysis */
  public void setDataAdapter(DataAdapter driver) {
    super.setDataAdapter(driver); // not sure if this is needed
    analysisDriver = (AnalysisAdapter)driver;
  }

  public Object doOperation(Object values) throws ApolloAdapterException {
    analysisDriver.setInputType(getCurrentInputType());
    analysisDriver.setParser (getCurrentAnalysisParser());
    analysisDriver.setFilter (getCurrentAnalysisFilter());
    if (getCurrentSource()==null)
      throw new ApolloAdapterException("null input");
    analysisDriver.setInput(getCurrentSource());
    analysisDriver.setRegion(getCurrentSeq());
    analysisDriver.setAnalysisInput(getCurrentInput());

    if (op.equals(ApolloDataAdapterI.OP_READ_DATA))
      return analysisDriver.getCurationSet();
    else if (op.equals(ApolloDataAdapterI.OP_APPEND_DATA))
      return analysisDriver.addToCurationSet();
    else
      return null;
  }

  public IOOperation getCurrentOperation() {
    return op;
  }

  /** Returns the type of analysis that correlates with the tabbed pane
   * currently selected  */
  private DataInputType getCurrentInputType() {
    return getCurrentAnalysisPanel().getInputType();
  }

  private String getCurrentSource() {
    return getCurrentAnalysisPanel().getCurrentFile();
  }

  private String getCurrentSeq() {
    return getCurrentAnalysisPanel().getCurrentSeq();
  }

  /** Returns the parser appropriate for the current analysis */
  private AnalysisParserI getCurrentAnalysisParser() {
    return getCurrentAnalysisPanel().getParser();
  }

  /** Returns the parser appropriate for the current analysis */
  private AnalysisFilterI getCurrentAnalysisFilter() {
    return getCurrentAnalysisPanel().getFilter();
  }

  /**
   * Returns the input from the currently selected tabbed pane
   */
  private AnalysisInput getCurrentInput() {
    return getCurrentAnalysisPanel().getCurrentInput();
  }

  private AnalysisPanel getCurrentAnalysisPanel() {
    return analysisPanels[tabbedPane.getSelectedIndex()];
  }

  /** Retrieve history and return in Properties. Also set
   * tabbed index */
  public Properties getProperties() {
    Properties combinedProperties = new Properties();
    String analysisName;
    Iterator childPropertyNames;
    Properties childProperties;
    String childPropertyName;
    String clildProperty;

    // remember what analysis is currently selected
    combinedProperties.put(indexPropString,""+tabbedIndex);

    for (int i=0; i < analysisPanels.length; i++) {
      AnalysisPanel ap = analysisPanels[i];
      analysisName = ap.getName();
      childProperties = ap.getProperties();
      addPropertiesWithPrefix (combinedProperties,
			       analysisName,
			       childProperties);
    }
    return combinedProperties;
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
  public void setProperties(Properties combinedProperties){
    HashMap childPropertiesMap = new HashMap();
    AnalysisPanel ap;
    String analysisName;
    Properties childProperty;
    
    if (!Config.getController().isCurationSetLoaded()) {
      // So that the tiers selection is as specified in apollo.cfg
      // I think this isnt right for synteny
      Config.newDataAdapter(analysisDriver);
      // Config.setStyle(getStyle()); ???
    }

    if (combinedProperties == null) {
      System.out.println("No properties available");
      for (int i=0; i < analysisPanels.length; i++) {
	ap = analysisPanels[i];
	ap.initTiersList (null);
      }//end while
    }
    else {
      // otherwise use the existing tiers selection
      // tabbed index
      String indexStr = combinedProperties.getProperty(indexPropString);
      if (indexStr!=null) {
	tabbedIndex = Integer.parseInt(indexStr);
	tabbedPane.setSelectedIndex(tabbedIndex);
      }
      
      setChildProperties (combinedProperties, childPropertiesMap);
      
      for (int i=0; i < analysisPanels.length; i++) {
	ap = analysisPanels[i];
	analysisName = ap.getName();
	childProperty = (Properties)childPropertiesMap.get(analysisName);
	if (childProperty != null) {
	  ap.setProperties(childProperty);
	}
	else
	  ap.initTiersList (null);
      }//end while
    }
  }
  
  public void setChildProperties (Properties combinedProperties,
				  HashMap childPropertiesMap) {
    String combinedPropertyName;
    String propertyValue;
    String child;
    String childPropertyName;
    Properties childProperty;
    int index;

    if (combinedProperties != null) {
      Iterator combinedPropertyNames = combinedProperties.keySet().iterator();
      while(combinedPropertyNames.hasNext()){
	combinedPropertyName = (String)combinedPropertyNames.next();
	propertyValue = (String)combinedProperties.get(combinedPropertyName);
	index = combinedPropertyName.indexOf(":");
	if(index > 0) {
	  //split out the child name from the property key.
	  child = combinedPropertyName.substring(0,index);
	  childPropertyName = combinedPropertyName.substring(index+1);
	  childProperty = (Properties) childPropertiesMap.get(child);
	  
	  //dig out our child Properties from the temporary map. 
	  // Create if we have to.
	  if(childProperty == null){
	    childProperty = new Properties();
	    childPropertiesMap.put(child, childProperty);
	  }//end if
	  childProperty.put(childPropertyName, propertyValue);
	}//end if
      }//end while
    }
  }

  public void addPropertiesWithPrefix(Properties properties,
				       String prefix,
				       Properties additionalProperties) {
      String key;
      Iterator keys = additionalProperties.keySet().iterator();
      String value;
      while(keys.hasNext()){
	  key = (String)keys.next();
	  value = additionalProperties.getProperty(key);
	  key = prefix+":"+key;
	  properties.setProperty(key, value);
      }//end while
  }//end addPropertiesWithPrefix

  public String getPropertyValueWithPrefix(Properties properties,
					   String prefix,
					   String propertyName) {
    String value = properties.getProperty(prefix+":"+propertyName);
    if (value == null) {
      // this is for debugging
      Iterator keys = properties.keySet().iterator();
      while(keys.hasNext()){
	String key = (String)keys.next();
	System.out.println ("getPropertyValueWithPerfix: " + propertyName + " is not " + key); // DEL
      }
    }
    return value;
  }//end getPropertyWithPrefix

  /** Loops through AnalysisPanels and inserts into tabbed pane,
   * Also hafta add our own buttons, since DataAdapterChooser does not add buttons
   * for AbstractIntDataAdapUIs although i think there should be a way for an
   * AbstractIntDAI to say hey i want those buttons like boolean addOKCancelButtons()
   */
  private void buildGUI() {
    tabbedPane = new JTabbedPane();
    tabbedPane.setBackground(Config.getDataLoaderBackgroundColor());
    for (int i=0; i<analysisPanels.length; i++) {
      analysisPanels[i].insertIntoTabbedPane(tabbedPane,i);
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

    // Buttons
    JButton okButton = new JButton("Ok");
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  // On Solaris, this is not considered kosher:
	  // Variable controllingObject in class org.bdgp.swing.AbstractIntDataAdapUI 
	  // not accessible from local class apollo.dataadapter.AnalysisAdapterGUI.
	  //	  controllingObject.cancel();
	  cancelControllingObject();
	}
      }
				   );
    okButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  commitControllingObject();
	}
      }
			       );
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.add(Box.createHorizontalGlue());
    //    okButton.setBackground(Color.green);
    buttonPanel.add(okButton);
    buttonPanel.add(Box.createHorizontalStrut(10));
    //    cancelButton.setBackground(Color.yellow);
    buttonPanel.add(cancelButton);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.setBackground(Color.white);
    add(buttonPanel);
  }

  public CurationSet getCurationSet() {
    if (op.equals(ApolloDataAdapterI.OP_APPEND_DATA))
      return this.curation_set;
    else
      return null;
  }

  private void cancelControllingObject() {
    controllingObject.cancel();
  }

  private void commitControllingObject() {
    controllingObject.commit();
  }

}
