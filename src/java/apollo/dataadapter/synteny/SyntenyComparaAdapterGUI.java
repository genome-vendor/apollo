package apollo.dataadapter.synteny;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.io.*;

import apollo.config.Config;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj19.CompositeDataSourceConfigurationPanel;
import apollo.dataadapter.ensj.*;
import apollo.dataadapter.ensj.model.*;

import org.apache.log4j.*;
import org.bdgp.io.*;
import java.util.Properties;
import org.bdgp.swing.AbstractDataAdapterUI;

import edu.stanford.ejalbert.*;

import apollo.datamodel.CurationSet;
import apollo.datamodel.GenomicRange;
import apollo.gui.*;
import apollo.util.GuiUtil;

import org.ensembl.util.*;
//import org.ensembl19.gui.*;
import org.ensembl.driver.*;
import org.ensembl.compara.driver.*;
import org.ensembl.compara.datamodel.*;

import java.util.regex.*;


/**
 * I am loaded into the 'compara' tab of the synteny adapter, and drive the 
 * loading of compara data.
**/
public class SyntenyComparaAdapterGUI extends AbstractDataAdapterUI {



  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SyntenyComparaAdapterGUI.class);

  public static final String PATTERN = "([^,]*),$";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String querySpecies;
  private String hitSpecies;
  
  private String logicalQuerySpecies;
  private String logicalHitSpecies;

  private JDialog frame;
  private JButton fullPanelButton  ;
  private SyntenyAdapterGUI parent;
  private DataAdapter adapter;
  
  private FullEnsJDBCSyntenyPanel thePanel; //syntenypanel
  
  //
  //Keeps configurations for the databases providing compara info,
  //as well as karyotype information for each species.
  private CompositeDataSourceConfigurationPanel dataConfigChooser;
  private JPanel dataSourceConfigurationPanel;
  private JButton showDataSourceConfiguration;
  private JCheckBox addHighConservationDnaAligns = new JCheckBox("Highly Conserved Dna-Dna Aligns");
  private JCheckBox addDnaAligns = new JCheckBox("Dna-Dna Aligns");
  private JButton dnaAlignTypesButton = new JButton("Method Link Types");
  private JList dnaAlignTypesList = new JList();
  private JCheckBox addProteinAligns = new JCheckBox("Protein-Protein Aligns");
  private JCheckBox addDnaAlignsToSingleSpeciesPanes = new JCheckBox("Add Dna Aligns To Each Species' Results");

  private boolean methodLinksVisible = false;
  private boolean methodLinksInitialised = false;

  public class DnaAlignTypesButtonActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){

      if (!methodLinksVisible) {
      
        Vector typeStrings = getDnaAlignTypeStrings();
  
        if (!methodLinksInitialised) {
          dnaAlignTypesList.setListData(typeStrings);
          methodLinksInitialised = true;
        }
        
        add(dnaAlignTypesList,GuiUtil.makeConstraintAt(2,1,1));
  
        JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, SyntenyComparaAdapterGUI.this);
        dialog.pack();
        methodLinksVisible = true;
      } else {
        remove(dnaAlignTypesList);

        JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, SyntenyComparaAdapterGUI.this);
        dialog.pack();
        methodLinksVisible = false;
      }
    }
  }
  
  private Vector getDnaAlignTypeStrings() {
    java.util.List types = getDnaAlignTypes();
    Vector typeStrings = new Vector();
  
    Iterator typesIter = types.iterator();
  
    while (typesIter.hasNext()) {
      MethodLink ml = (MethodLink)typesIter.next();
  
      typeStrings.add(ml.getType());
    }
    return typeStrings;
  }

  /**
   * Space saver - 
   * Toggles the data source chooser to be visible/not. Repacks the parent dialog every time.
  **/
  public class ShowDataSourceConfigurationActionListener implements ActionListener{
    public void actionPerformed(ActionEvent event){
      JDialog dialog = (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, SyntenyComparaAdapterGUI.this);
      if(
        getShowDataSourceConfigurationButton()
          .getLabel().equals("Show Data Source Configuration")
      ){
        getDataConfigChooser().setVisible(true);
        getShowDataSourceConfigurationButton().setLabel("Hide Data Source Configuration");
        dialog.pack();
      }else{
        getDataConfigChooser().setVisible(false);
        getShowDataSourceConfigurationButton().setLabel("Show Data Source Configuration");
        dialog.pack();
      }
    }
  }
  
  /** 
   * Launches the FullEnsjSyntenyPanel when the "view panel" button is hit. 
   * The purpose of the panel is to set the ranges back onto the individual 
   * adapters.
  **/
  public class FullPanelListener implements ActionListener{
    public void actionPerformed(ActionEvent theEvent){
      try{
        KaryotypeAdapterI karyotypeAdapter = createKaryotypeAdapter();
        SyntenyAdapterI syntenyAdapter = createSyntenyAdapter(karyotypeAdapter);

        SyntenyComparaAdapterGUI.this.thePanel = 
          new FullEnsJDBCSyntenyPanel(
            getQuerySpecies(), //logical
            getHitSpecies(), //logical
            karyotypeAdapter,
            syntenyAdapter,
            getParentAdapter() //the callback is my parent composite adapter
          );

        thePanel.init();
        SyntenyComparaAdapterGUI.this.frame = new JDialog((JFrame)null, "range chooser", true);
        SyntenyComparaAdapterGUI.this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        SyntenyComparaAdapterGUI.this.frame.getContentPane().setLayout(new BorderLayout());
        SyntenyComparaAdapterGUI.this.frame.getContentPane().add("Center",thePanel);

        SyntenyComparaAdapterGUI.this.frame.setSize(700,700);
        SyntenyComparaAdapterGUI.this.frame.show();
      }catch(NonFatalDataAdapterException exception){
        JOptionPane.showMessageDialog(null, exception.getMessage());
      }//end try
      
    }//end actionPerformed
  }//end FullPanelListener 
  
  public SyntenyComparaAdapterGUI(IOOperation op) {
    buildGUI();
  }

  /**
   * Creates an implementation of Apollo's KaryotypeAdapterI using the 
   * species-level database config information provided in the 'shared data'
   * map of the parent adapter. (Note - that shared data comprises of handles
   * to the individual species' adapters, and has to have been placed there
   * when those adapters were created by the parent composite adapter).
  **/
  private KaryotypeAdapterI createKaryotypeAdapter(){
    //
    //Create a species-specific ensj driver for each input species.
    //And pull chromosome+band information from it. Then create Apollo
    //chromosome and band-objects. Then load these into an Apollo KaryotypeI
    //implementation. The inner class just serves these apollo karyotype object when asked.

    KaryotypeAdapter adapter = new KaryotypeAdapter();

    EnsJAdapterGUI querySpeciesAdapter;
    Properties querySpeciesProperties;
    EnsJAdapterGUI hitSpeciesAdapter;
    Properties hitSpeciesProperties;
    CoreDriver speciesDriver;

    try{
      
      adapter.setName("karyotype adapter");
      adapter.setType("karyotype adapter");
    
      querySpeciesAdapter = 
        (EnsJAdapterGUI)
        //getParentAdapter().getSharedData().get(getQuerySpecies());
        getParentAdapter().getAdapterGUIs().get(getQuerySpecies());
    
      querySpeciesProperties = 
        querySpeciesAdapter
          //.getDataConfigChooser()
          //.getChooser("default")
          .getProperties();
    
      validateDatabaseInfoSpecified(getQuerySpecies(), querySpeciesProperties);
    
      hitSpeciesAdapter = 
        (EnsJAdapterGUI)
        //getParentAdapter().getSharedData().get(getHitSpecies());
        getParentAdapter().getAdapterGUIs().get(getHitSpecies());
    
      hitSpeciesProperties =
        hitSpeciesAdapter
          //.getDataConfigChooser()
          //.getChooser("default")
          .getProperties();

      validateDatabaseInfoSpecified(getHitSpecies(), hitSpeciesProperties);
    
      speciesDriver = DriverManager.load(querySpeciesProperties);
    
      adapter.addKaryotype(
        new Karyotype(
          querySpecies,
          fetchAllApolloChromosomesWithDriver(speciesDriver)
        )
      );

      speciesDriver = DriverManager.load(hitSpeciesProperties);
      
      adapter.addKaryotype(
        new Karyotype(
          hitSpecies,
          fetchAllApolloChromosomesWithDriver(speciesDriver)
        )
      );

    }catch(AdaptorException exception){
      throw new NonFatalDataAdapterException(exception.getMessage());
    }
    
    return adapter;
  }//end createKaryotypeAdapter

  /**
   * Create a set of Apollo chromosomes (with bands) for all chromosomes in the
   * ensembl database pointed at by the input adapter
  **/
  private Vector fetchAllApolloChromosomesWithDriver(CoreDriver speciesDriver)
  throws AdaptorException
  {
    apollo.datamodel.Chromosome apolloChromosome;
    org.ensembl.datamodel.Location ensjChromoLoc;
    org.ensembl.driver.KaryotypeBandAdaptor bandAdaptor;
    org.ensembl.driver.CoordinateSystemAdaptor coordSystemAdaptor;
    
    bandAdaptor = 
      (KaryotypeBandAdaptor)speciesDriver.getKaryotypeBandAdaptor();
    coordSystemAdaptor = 
        (CoordinateSystemAdaptor)speciesDriver.getCoordinateSystemAdaptor();

    apollo.datamodel.ChromosomeBand apolloChromosomeBand;
    org.ensembl.datamodel.KaryotypeBand ensjChromosomeBand;
    Vector apolloChromosomes = new Vector();
    Vector apolloChromosomeBands = new Vector();
    
    Iterator chromosomes = coordSystemAdaptor.fetchTopLevelLocations().iterator();

    while(chromosomes.hasNext()){

      ensjChromoLoc = (org.ensembl.datamodel.Location)chromosomes.next();

      // Only do for chromosomes which aren't extra NT contigs
      if (ensjChromoLoc.getCoordinateSystem().getName().indexOf("chromosome") != -1 &&
          ensjChromoLoc.getSeqRegionName().indexOf("_NT") == -1) {

        try {
          Iterator bands = bandAdaptor.fetch(ensjChromoLoc.getCoordinateSystem(),ensjChromoLoc.getSeqRegionName()).iterator();
    
          while(bands.hasNext()){
    
            ensjChromosomeBand = (org.ensembl.datamodel.KaryotypeBand)bands.next();
            apolloChromosomeBand = 
              new apollo.datamodel.ChromosomeBand(
                ensjChromosomeBand.getBand(),
                ensjChromosomeBand.getLocation().getSeqRegionName(),
                ensjChromosomeBand.getLocation().getStart(),
                ensjChromosomeBand.getLocation().getEnd(),
                ensjChromosomeBand.getStain()
              );
    
            apolloChromosomeBands.add(apolloChromosomeBand);
    
          }//end while
        } catch (Exception e) {
          logger.error("Caught exception fetching bands - ignoring as it probably means theres no karyotype data", e);
          bandAdaptor = null;
        }
  
        apolloChromosome = 
          new apollo.datamodel.Chromosome(
            querySpecies, //need to convert from logical -> species name?
            ensjChromoLoc.getSeqRegionName(),
            apolloChromosomeBands
          );
  
        apolloChromosome.setLength(new Long(ensjChromoLoc.getEnd()).intValue());
        apolloChromosome.setCoordSystem(ensjChromoLoc.getCoordinateSystem().getVersion() + "--" + ensjChromoLoc.getCoordinateSystem().getName());
  
        /* Parsing of bands for a human-karyotype correctly gets p-andq-lengths.
         * BUT it fails miserably for other species. For now (to get sensible
         * pictures drawn) we will just set the q-length to be that for the 
         * whole chromosome. This way we actually get good images in the
         * full-synteny-panel.
         */
        apolloChromosome.setPLength(0);
        apolloChromosome.setQLength(apolloChromosome.getLength());
        apolloChromosome.setCentroLength(0);
        
        apolloChromosomes.add(apolloChromosome);
      }
    }//end while
    
    return apolloChromosomes;
  }//end fetchAllApolloChromosomesWithDriver
  
  /**
   * Creates an implementation of Apollo's SyntenyAdapterI using the 
   * compara database config information provided.
  **/
  private SyntenyAdapterI createSyntenyAdapter(KaryotypeAdapterI karyotypeAdapter){
    
    //
    //Read high-level homology information from the compara-tables. Load into
    //an inner class implementing SyntenyAdapterI implementation.
    
    Properties comparaProps = getDataConfigChooser().getChooser("compara").getProperties();

    Iterator chromosomesForQuerySpecies;

    apollo.dataadapter.SyntenyAdapter syntenyAdapter = 
      (apollo.dataadapter.SyntenyAdapter)new apollo.dataadapter.SyntenyAdapter();

    Chromosome queryChromosome;
    
    Karyotype queryKaryotype = karyotypeAdapter.getKaryotypeBySpeciesName(getQuerySpecies());
    Karyotype hitKaryotype;
    
    syntenyAdapter.setName("synteny adapter");
    syntenyAdapter.setType("synteny adapter");
    syntenyAdapter.setKaryotypeAdapter(karyotypeAdapter);
      
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(comparaProps);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor("member");
      org.ensembl.datamodel.FeaturePair ensjFeaturePair;
      SyntenyRegion apolloSyntenyRegion;
      org.ensembl.datamodel.Location queryLocation;
      org.ensembl.datamodel.Location hitLocation;
      Iterator ensjFeaturePairs;
      
      chromosomesForQuerySpecies = 
          karyotypeAdapter
            .getKaryotypeBySpeciesName(getQuerySpecies())
            .getChromosomes()
            .iterator();


      while(chromosomesForQuerySpecies.hasNext()){
      
        queryChromosome = (Chromosome)chromosomesForQuerySpecies.next();
        
        java.util.List fps = 
          memberAdaptor.fetch(
            convertLogicalNameToSpeciesName(querySpecies),
            convertLogicalNameToSpeciesName(hitSpecies),
            queryChromosome.getDisplayId()
          );
        
        ensjFeaturePairs = fps.iterator();

        while(ensjFeaturePairs.hasNext()){

          ensjFeaturePair = (org.ensembl.datamodel.FeaturePair)ensjFeaturePairs.next();

          queryLocation = (org.ensembl.datamodel.Location)ensjFeaturePair.getLocation();

          hitLocation = (org.ensembl.datamodel.Location)ensjFeaturePair.getHitLocation();
          hitKaryotype = karyotypeAdapter.getKaryotypeBySpeciesName(getHitSpecies());

          apolloSyntenyRegion = 
            new SyntenyRegion(
              queryChromosome,
              queryLocation.getStart(),
              queryLocation.getEnd(),
              hitKaryotype.getChromosomeByName(hitLocation.getSeqRegionName()),
              hitLocation.getStart(),
              hitLocation.getEnd(),
              hitLocation.getStrand()
            );

          syntenyAdapter.addSyntenyRegionForChromosome(
            queryChromosome.getDisplayId(),
            apolloSyntenyRegion
          );

        }//end while
      }//end try

    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.NonFatalDataAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.NonFatalDataAdapterException(exception.getMessage());
    }//end try
    
    return syntenyAdapter;
  }

  /**
   * Find out the types of dna aligns in the compara database
  **/
  private java.util.List getDnaAlignTypes(){
    java.util.List methodLinks;
    java.util.List dnaMethodLinks;
    
    //
    //Read high-level homology information from the compara-tables. Load into
    //an inner class implementing SyntenyAdapterI implementation.
    
    Properties comparaProps = getDataConfigChooser().getChooser("compara").getProperties();

    try {
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(comparaProps);

      MethodLinkAdaptor methodLinkAdaptor = (MethodLinkAdaptor)comparaDriver.getAdaptor("method_link");

      methodLinks = methodLinkAdaptor.fetch();


      Iterator methodLinkIter = methodLinks.iterator();

      dnaMethodLinks = new ArrayList();
      while(methodLinkIter.hasNext()){
        MethodLink ml = (MethodLink)methodLinkIter.next();
        if (ml.getClassStr() != null && ml.getClassStr().startsWith("GenomicAlign")) {
          dnaMethodLinks.add(ml);
        }
      }

    } catch(ConfigurationException exception) {
      throw new apollo.dataadapter.NonFatalDataAdapterException(exception.getMessage());
    } catch(AdaptorException exception) {
      throw new apollo.dataadapter.NonFatalDataAdapterException(exception.getMessage());
    }//end try
    
    return dnaMethodLinks;
  }

  
  protected void buildGUI() {
    setLayout(new GridBagLayout());
    fullPanelButton = new JButton("View Full Synteny Panel");
    JLabel explainLabel = new JLabel("...shows synteny blocks between species, when available");
    FullPanelListener fullPanelListener = new FullPanelListener();
    fullPanelButton.addActionListener(fullPanelListener);
    DnaAlignTypesButtonActionListener dnaAlignTypesButtonActionListener = new DnaAlignTypesButtonActionListener();
    dnaAlignTypesButton.addActionListener(dnaAlignTypesButtonActionListener);
    
    add(getAddHighConservationDnaAligns(),GuiUtil.makeConstraintAt(0,0,1));
    add(getAddDnaAligns(),GuiUtil.makeConstraintAt(0,1,1));
    add(dnaAlignTypesButton,GuiUtil.makeConstraintAt(1,1,1));
    add(getAddProteinAligns(),GuiUtil.makeConstraintAt(0,2,1));
    add(getDnaAlignsIntoSingleSpecies(),GuiUtil.makeConstraintAt(0,3,1));
    add(fullPanelButton,GuiUtil.makeConstraintAt(0,4,1));
    add(explainLabel,GuiUtil.makeConstraintAt(1,4,1));
    buildDataSourceConfigurationPanel();
    add(getDataSourceConfigurationPanel(), GuiUtil.makeConstraintAt(0,5,2));
  }//end buildGUI

  /**
   * add on the hit-information to the query-information already in there.
  **/
  public Properties createStateInformation() throws apollo.dataadapter.ApolloAdapterException {
    Properties querySpeciesProperties;
    Properties hitSpeciesProperties;
    EnsJAdapterGUI querySpeciesAdapter;
    EnsJAdapterGUI hitSpeciesAdapter;

    //
    //Location, selected feature types copied in.
    Properties stateInfo = new Properties();
    
    String apolloRoot = System.getProperty("APOLLO_ROOT");
    File logFile = new File(apolloRoot+"/conf/logging_info_level.conf");
    if(!logFile.exists()){
      throw new 
        apollo.dataadapter.ApolloAdapterException(
          "The following file must be provided to support ensj - conf/logging_info_level.conf"
        );
    }
    
    stateInfo.put("loggingFile", apolloRoot+"/conf/logging_info_level.conf");

    stateInfo.setProperty("querySpecies",getQuerySpecies());
    stateInfo.setProperty("hitSpecies",getHitSpecies());
    
    querySpeciesAdapter = 
      (EnsJAdapterGUI)
      //getParentAdapter().getSharedData().get(getQuerySpecies());
      getParentAdapter().getAdapterGUIs().get(getQuerySpecies());

    hitSpeciesAdapter = 
      (EnsJAdapterGUI)
      //getParentAdapter().getSharedData().get(getHitSpecies());
      getParentAdapter().getAdapterGUIs().get(getHitSpecies());


    LocationModel querySpeciesLocation = querySpeciesAdapter.getModel().getLocationModel();
    LocationModel hitSpeciesLocation = hitSpeciesAdapter.getModel().getLocationModel();
    if (hasRangeInformation(querySpeciesLocation)) {
      stateInfo.put("chromosome", querySpeciesLocation.getSelectedSeqRegion());
      stateInfo.put("coordsystem", querySpeciesLocation.getSelectedCoordSystem());
      logger.debug("Query coordsystem = " + querySpeciesLocation.getSelectedCoordSystem());
      stateInfo.put("start", querySpeciesLocation.getStart());
      stateInfo.put("end", querySpeciesLocation.getEnd());
    }
    
    if (hasRangeInformation(hitSpeciesLocation)) {
      stateInfo.setProperty("hitChr",hitSpeciesLocation.getSelectedSeqRegion());
      stateInfo.put("hitCoordSys", hitSpeciesLocation.getSelectedCoordSystem());
      logger.debug("Hit coordsystem = " + hitSpeciesLocation.getSelectedCoordSystem());
      stateInfo.setProperty("hitStart",hitSpeciesLocation.getStart());
      stateInfo.setProperty("hitEnd",hitSpeciesLocation.getEnd());
    }

    if (querySpeciesLocation.isStableIDLocation()) {
      stateInfo.setProperty("stableId", querySpeciesLocation.getStableID());
    }
    
    if (hitSpeciesLocation.isStableIDLocation()) {
      stateInfo.setProperty("hitStableId", hitSpeciesLocation.getStableID());
    }
    
    stateInfo.putAll(getDataConfigChooser().getChooser("compara").getProperties());
    
    stateInfo.put("highConservationDnaAligns", String.valueOf(getAddHighConservationDnaAligns().isSelected()));
    stateInfo.put("dnaAligns", String.valueOf(getAddDnaAligns().isSelected()));
    stateInfo.put("proteinAligns", String.valueOf(getAddProteinAligns().isSelected()));
    stateInfo.put("dnaAlignsIntoSingleSpecies", String.valueOf(getDnaAlignsIntoSingleSpecies().isSelected()));

    Object[] methodLinkTypes = dnaAlignTypesList.getSelectedValues();

    String typesString = null;
    for (int i=0;i<methodLinkTypes.length;i++) {
      if (typesString != null) {
        typesString = typesString + ",";
        typesString = typesString + (String)methodLinkTypes[i];
      } else {
        typesString = (String)methodLinkTypes[i];
      }
    }

    if (typesString == null) {
      typesString = "";
    }
    stateInfo.put("dnaAlignTypes", typesString);
    
    validateDatabaseInfoSpecified("compara", stateInfo);
    
    return stateInfo;
  }

  private void validateDatabaseInfoSpecified(String speciesName, Properties properties){
    String host = properties.getProperty("host");
    String port = properties.getProperty("port");
    String database = properties.getProperty("database");
    
    if(host == null || host.trim().length() <= 0){
      throw new apollo.dataadapter.NonFatalDataAdapterException(speciesName+" database host name must be provided");
    }else if(
      port == null ||
      port.trim().length() <= 0
    ){
      throw new apollo.dataadapter.NonFatalDataAdapterException(speciesName+" database port  must be provided");
    }else if(
      database == null ||
      database.trim().length() <= 0
    ){
      throw new apollo.dataadapter.NonFatalDataAdapterException(speciesName+" database name must be provided");
    }//end if
  }//end validateSpeciesDatabasesSpecified
  
  private void addPropertiesWithPrefix(
    Properties properties,
    String prefix,
    Properties additionalProperties
  ){
    String key;
    Iterator keys = properties.keySet().iterator();
    String value;
    while(keys.hasNext()){
      key = (String)keys.next();
      value = additionalProperties.getProperty(key);
      key = prefix+"."+key;
      properties.put(key, value);
    }//end while
  }//end addPropertiesWithPrefix

  /**
   * <p>This SNEAKY method is invoked by the CompositeAdapter after it's created this adapter 
   * as a child. </p>
   *
   * <p>This is invoked by the SyntenyMenu after the user has chosen a synteny region.</p>
   * 
   * <p>If the input is a HashMap, we use it to set values into the chromosome and 
   * high/low text fields. </p>
   *
   * <p>If the input is an AbstractDataAdapterUI instance, then we
   * are being told that this input is our 'parent'. We will set 
   * this parent on ourselves </p>
   *
   * <p> WHY IS THIS METHOD OVERUSED? </p>
   * <p> (1) It is part of the standard AbstractDataAdapterUI 
   * interface,so I can use it without having to create an interface like "composable" 
   * with a get/setParent() method.</p>
   *
   * <p> (2) I should move the other functionality in here to the setProperties() method. </p>
   *
  **/
  public void setInput(Object input) {
    HashMap theInput;
    String text;
    
    if(input instanceof HashMap){
      
      theInput = (HashMap)input;

      //
      //If we've received input, then the frame can disappear
      frame.dispose();
      
    }else if(input instanceof AbstractDataAdapterUI){
      
      setParentAdapter((SyntenyAdapterGUI)input);
      getParentAdapter().getSharedData().put(getQuerySpecies()+"-"+getHitSpecies(), this);
      
    }//end if
  }//end setInput

  /**
   * <p> This contains the properties stored in the adapter history,
   * which are read when the adapter is created. They will populate
   * the history-comboboxes etc. </p>
   *
   * <p> It also contains the NAMES of the query and hit species - 
   * note that these properties live in the style file for the 
   * synteny adapter. They are added onto the history-file properties
   * passed into this adapter by the SyntenyAdapterGUI class.
  **/
  public void setProperties(Properties properties){
    String booleanString;
    
    //
    //As passed by the parent adapter, these are LOGICAL names for
    //the species, like "Human" or "Mouse", or "Human1" and "Human2"
    querySpecies = properties.getProperty("querySpecies");
    hitSpecies = properties.getProperty("hitSpecies");

    //
    //Set a default database configuration if one hasn't been given to us.
    if(properties.getProperty("jdbc_driver") == null || properties.getProperty("jdbc_driver").trim().length() <=0){
      properties.setProperty("jdbc_driver","org.gjt.mm.mysql.Driver");
    }//end if
    
    if(properties.getProperty("host") == null || properties.getProperty("host").trim().length() <=0){
      properties.setProperty("host","ensembldb.ensembl.org");
    }//end if
    
    if(properties.getProperty("port") == null || properties.getProperty("port").trim().length() <=0){
      properties.setProperty("port","3306");
    }//end if
    
    if(properties.getProperty("user") == null || properties.getProperty("user").trim().length() <=0){
      properties.setProperty("user","anonymous");
    }//end if
    
    if(properties.getProperty("ensembl_driver") == null || properties.getProperty("ensembl_driver").trim().length() <=0){
      properties.setProperty("ensembl_driver","org.ensembl19.driver.plugin.compara.ComparaMySQLDriver");
    }//end if
    
    if(properties.getProperty("database") == null || properties.getProperty("database").trim().length() <=0){
      properties.setProperty("database","ensembl_compara_45");
    }//end if
    
    getDataConfigChooser().setProperties(properties);
    
    getDataConfigChooser().populateEnsemblDatabases();

    String db = properties.getProperty("database");
    
    getDataConfigChooser()
      .getChooser("compara")
      .setSelectedEnsemblDatabase(properties.getProperty("database"));

    getDataSourceConfigurationPanel().add(
      dataConfigChooser, GuiUtil.makeConstraintAt(0,1,1)
    );
    
    booleanString = properties.getProperty("proteinAligns");
    if(booleanString == null || booleanString.trim().length() <=0){
      getAddProteinAligns().setSelected(true);
    }else{
      getAddProteinAligns().setSelected(Boolean.valueOf(booleanString).booleanValue());
    }
    
    booleanString = properties.getProperty("dnaAligns");
    if(booleanString == null || booleanString.trim().length() <=0){
      getAddDnaAligns().setSelected(true);
    }else{
      getAddDnaAligns().setSelected(Boolean.valueOf(booleanString).booleanValue());
    }
    
    booleanString = properties.getProperty("highConservationDnaAligns");
    if(booleanString == null || booleanString.trim().length() <=0){
      getAddHighConservationDnaAligns().setSelected(true);
    }else{
      getAddHighConservationDnaAligns().setSelected(Boolean.valueOf(booleanString).booleanValue());
    }
    
    booleanString = properties.getProperty("dnaAlignsIntoSingleSpecies");
    if(booleanString == null || booleanString.trim().length() <=0){
      getDnaAlignsIntoSingleSpecies().setSelected(true);
    }else{
      getDnaAlignsIntoSingleSpecies().setSelected(Boolean.valueOf(booleanString).booleanValue());
    }

    String wantedTypesString = properties.getProperty("dnaAlignTypes");
    if (wantedTypesString!=null) {
      String [] wantedTypes = wantedTypesString.split(",");

      Vector alignTypes = getDnaAlignTypeStrings();

      int [] wantedIndices = new int [wantedTypes.length];

      for (int i=0;i<wantedTypes.length;i++) {
        boolean found = false;
        for (int j=0;j<alignTypes.size();j++) {
          String typeString = (String) alignTypes.get(j);
          if (typeString.equals(wantedTypes[i])) {
            wantedIndices[i] = j;
            found = true;
            break;
          }
        }
        if (!found) {
          logger.warn("Didn't find dna align type " + wantedTypes[i]);
        }
      }
      if (!methodLinksInitialised) {
        dnaAlignTypesList.setListData(alignTypes);
        methodLinksInitialised = true;
      }
      dnaAlignTypesList.setSelectedIndices(wantedIndices);
    }
  }//end setProperties
  
  private String getQuerySpecies(){
    return querySpecies;
  }//end getQuerySpecies
  
  private String getHitSpecies(){
    return hitSpecies;
  }//end getHitSpecies

  private SyntenyAdapterGUI getParentAdapter(){
    return parent;
  }//end getParent
  
  /**
   * This MUST be invoked if this adapter is created as part of a composite-
   * I do it in the setInput method
  **/
  private void setParentAdapter(SyntenyAdapterGUI newValue){
    parent = newValue;
  }//end setParent

  /**
   * Returns the state of the database config panel, pretty much.
  **/
  public Properties getProperties() {
    Properties properties = new Properties();
    Properties tempProperties = getDataConfigChooser().getPrefixedProperties();
    properties.putAll(tempProperties);
    
    properties.put(
      "proteinAligns",
      String.valueOf(
        new Boolean(
          getAddProteinAligns().isSelected()
        ).booleanValue()
      )
    );
    
    properties.put(
      "dnaAligns",
      String.valueOf(
        new Boolean(
          getAddDnaAligns().isSelected()
        ).booleanValue()
      )
    );
    
    properties.put(
      "highConservationDnaAligns",
      String.valueOf(
        new Boolean(
          getAddHighConservationDnaAligns().isSelected()
        ).booleanValue()
      )
    );
    
    properties.put(
      "dnaAlignsIntoSingleSpecies",
      String.valueOf(
        new Boolean(
          getDnaAlignsIntoSingleSpecies().isSelected()
        ).booleanValue()
      )
    );


    Object[] methodLinkTypes = dnaAlignTypesList.getSelectedValues();

    String typesString = null;
    for (int i=0;i<methodLinkTypes.length;i++) {
      if (typesString != null) {
        typesString = typesString + ",";
        typesString = typesString + (String)methodLinkTypes[i];
      } else {
        typesString = (String)methodLinkTypes[i];
      }
    }

    if (typesString == null) {
      typesString = "";
    }
    properties.put( "dnaAlignTypes", typesString);
        
    return properties;
  }//end getProperties

  /**
   * Use the synteny style to convert from logical to actual species names.
  **/
  private String convertLogicalNameToSpeciesName(String logicalName){
    HashMap speciesNames = 
      Config
        .getStyle("apollo.dataadapter.synteny.SyntenyAdapter")
        .getSyntenySpeciesNames();
    
    Iterator logicalNames = speciesNames.keySet().iterator();
    int index;
    String longName;
    String shortName = null;
    
    while(logicalNames.hasNext()){
      longName = (String)logicalNames.next();
      
      //
      //Convert Name.Human to Human
      index = longName.indexOf(".");
      shortName = longName.substring(index+1);
      
      if(shortName.equals(logicalName)){
        return (String)speciesNames.get(longName);
      }//end if
      
    }//end while
    
    if(true){
      throw new NonFatalDataAdapterException("No logical species name matches the name input:"+shortName);
    }//end if
      
    return null;
  }//end convertLogicalNameToSpeciesName
  
 
  private JPanel buildDataSourceConfigurationPanel(){
    dataSourceConfigurationPanel = new JPanel();
    dataSourceConfigurationPanel.setLayout(new GridBagLayout());
    dataSourceConfigurationPanel.setBorder(
      BorderFactory.createTitledBorder("Data Sources")
    );
    
    showDataSourceConfiguration = new JButton("Show Data Source Configuration");
    showDataSourceConfiguration.addActionListener(
      new SyntenyComparaAdapterGUI.ShowDataSourceConfigurationActionListener()
    );

    dataSourceConfigurationPanel.add(
      getShowDataSourceConfigurationButton(), GuiUtil.makeConstraintAt(0,0,1)
    );

    //
    //This component is not immediately visible - we'll display when 
    //the user selects "show" checkbox.
    dataConfigChooser = 
      new CompositeDataSourceConfigurationPanel(
        new String[]{"compara"}
      );
    
    dataConfigChooser.setVisible(false);
    dataSourceConfigurationPanel.add(
      dataConfigChooser, GuiUtil.makeConstraintAt(0,1,1)
    );
    
    return dataSourceConfigurationPanel;
  }//end buildDataSourceConfigurationPanel 
  
  private CompositeDataSourceConfigurationPanel getDataConfigChooser(){
    return dataConfigChooser;
  }//end getDataConfigChooser
  
  private JPanel getDataSourceConfigurationPanel(){
    return dataSourceConfigurationPanel;
  }//end getDataSourceConfigurationPanel
  
  private JButton getShowDataSourceConfigurationButton(){
    return showDataSourceConfiguration;
  }//end getShowDataSourceConfigurationButton
  
  public Object doOperation(Object values) throws apollo.dataadapter.ApolloAdapterException {
    try{
      Properties stateInformation = createStateInformation();
      
      ((ApolloDataAdapterI)getDataAdapter()).setStateInformation(stateInformation);
      
      return ((ApolloDataAdapterI) getDataAdapter()).getCurationSet();

    }catch(apollo.dataadapter.NonFatalDataAdapterException exception){
      throw new apollo.dataadapter.ApolloAdapterException("Problem loading data:"+exception.getMessage(), exception);
    }
  }
  
  public void setDataAdapter(DataAdapter adapter) {
    this.adapter = adapter;
  }

  public DataAdapter getDataAdapter(){
    return adapter;
  }
  
  private JCheckBox getAddDnaAligns(){
    return addDnaAligns;
  }
    
  private JCheckBox getAddHighConservationDnaAligns(){
    return addHighConservationDnaAligns;
  }
  
  private JCheckBox getAddProteinAligns(){
    return addProteinAligns;
  }

  private JCheckBox getDnaAlignsIntoSingleSpecies(){
    return addDnaAlignsToSingleSpeciesPanes;
  }

  private boolean hasRangeInformation(LocationModel locationModel){
    boolean hasLocation = false;

    if(!locationModel.isStableIDLocation()){
      hasLocation = locationModel.getSelectedCoordSystem() != null &&
                    locationModel.getSelectedSeqRegion() != null &&
                    locationModel.getStart() != null &&
                    locationModel.getEnd() != null;
    }
    return hasLocation;
  }
}

