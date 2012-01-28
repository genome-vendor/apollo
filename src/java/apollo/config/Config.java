package apollo.config;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapterRegistry;
import org.bdgp.io.IOOperation;
import org.bdgp.util.DNAUtils;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Transcript;
import apollo.gui.Controller;
import apollo.util.IOUtil;
import apollo.dataadapter.AbstractApolloAdapter;
import apollo.editor.UserName;
import apollo.main.CommandLine;

/**
 * A class with static data members and static methods for setting and
 * retrieving configuration information for the apollo gui.
 *
 * from apollo.ebi email: createStyle(...) is used to create a new style that
 * is coming in from the apollo.cfg DataAdapterInstall line as you've 
 * probably seen:
 * DataAdapterInstall "apollo.dataadapter.EnsCGIAdapter" "ensembl.style"
 * Config sees this line and installs a new Style whose settings are in 
 * ensembl.style and will get associated with the EnsCGIAdapter. This will
 * NOT then become the present style. apollo.cfg can specify many 
 * DataAdapters with styles, and styles can be shared. At initialization
 * time createStyle() creates these styles and puts them in a hash to
 * be called up later. When the user selects a data adapter
 * (with the DataAdapterChooser gui) then newDataAdapter is called with the new
 * DataAdapter Class (which calls setDataAdapterType with the class string).
 * At this point Config sets its style class variable because we now know which
 * style the user wants.
 * NameAdapters are specific to a an annotation type now;
 * NameAdapterInstall has been moved to TiersIO
*/

public class Config {

  private static final Logger      logger = LogManager.getLogger(Config.class);

  private static Style             style = null;
  private static boolean           outputChadoTransaction = false;
  private static boolean           outputTransactionXML = false;
  private static String            chadoTemplFileName = null;
  private static String            chadoJdbcAdapterConfigFile = "chado-adapter.xml";
  private static String            chadoJdbcAdapterConfigFileFullName = null;
  private static String            dataDir = null;
  private static String            rootDir = null;
  private static String            adapterHistoryFile;
  private static String            karyotypeFile;
  private static String            syntenyDir;
  private static String            exonSyntenyDir;
  private static boolean           showSyntenyEvidencePanels = false;
  private static int               syntenyPanelRelativeSize = 2;
  private static int               translationShowLimit = 5;

  private static long              memoryAllocation;

  //private static TypesObserver     obs;
  // put this in Controller itself - take out of Controller
  //private static Controller        controller = new Controller();
  private static Controller        controller = Controller.getMasterController();
  private static DataAdapterRegistry adapterRegistry = new DataAdapterRegistry();

  //  private static ApolloNameAdapterI defaultNamer = null;
  private static OverlapI defaultOverlapper = null;

  private static HashMap filenameToStyle = new HashMap(8);
  private static HashMap adapterToStyle = new HashMap(8);

  private static int               autosaveInterval = 30;
  private static String            autosaveFile; //     = "apollo.bak";

  private static int               cgiPort  = 80;
  private static String            cgiHost  = "ensrv1.sanger.ac.uk";
  private static String            cgiScript  = "perl/apolloview";

  private static String            dbHost = "";
  private static String            dbName = "";
  private static String            gpName = "";

  private static String            srsServer = "www.sanger.ac.uk";
  private static String            pfetchServer = null;

  private static Hashtable pubDbToURL;

  //private static String dasServer = "http://genome-test.cse.ucsc.edu/cgi-bin/das/";
    // Should be overriden by DASServer line in apollo.cfg
  private static String dasServer = "http://servlet.sanger.ac.uk:8080/das/";
  private static String            frameOrientation = "vertical";
  private static int               mainWindowWidth = 1000;

  // Desired window around genes when zooming to them (e.g. for selection
  // from gene or bookmark menu)
  private static int               geneWindowSize = 5000;

  // Whether to ask the user "Are you sure?" when they try to save data to
  // a file that already exists
  private static boolean           confirmOverwrite = true;

  // Whether to ask the user "Are you sure?" when they try to delete one or
  // more selected annotations.
  private static boolean           confirmDeleteSelection = false;
  
  // Check if the user can delete or modify.
  private static boolean           checkOwnership = false;

  // Whether the various transactions in apollo.editor.* should save a *cloned* copy
  // of each of the SeqFeatures (in addition to a reference to the original SeqFeature.)
  // This parameter must be set to true in order for the PureJDBCTransactionWriter
  // to work correctly.  
  private static boolean           saveClonedFeaturesInTransactions = false;

  // Allows configuration of TigrAnnotNameAdapter.newNameCommand.
  private static String            tigrAnnotNameAdapterNewNameCommand = null;

  // Whether to show the "Save" option in the File menu in Apollo's main window.
  // (even if set to false the option is still available via keyboard shortcut)
  private static boolean           showSaveOptionInFileMenu = false;

  // Whether to show the "Save as..." option in the File menu in Apollo's main window.
  // (even if set to false the option is still available via keyboard shortcut)
  private static boolean           showSaveAsOptionInFileMenu = true;

  // Right now, these next four are not user-configurable from apollo.cfg; it's just a
  // convenient place to keep these constants so they can be accessed by more than one class.
  private static Color             dataLoaderBackgroundColor = new Color(193,205,205);  // sort of a muted turquoise
  private static Color             dataLoaderLabelColor = Color.black;
  private static Color             dataLoaderTitleColor = new Color(0,0,102);  // midnight blue
  // Black was problematic because there are places where it's the background
  // for black text.
  //  static Color                     defaultAnnotationColor = Color.black;
  static Color                     defaultAnnotationColor = Color.blue;

  // I'm leaving these here because I don't think users should need to change them
  private static int               siteShowLimit = 15;
  private static int               textAvoidLimit = 1500;
  private static float             fastDrawLimit = 0.5F;
  private static boolean           fastClipGraphics = true;

  // Whether to retranslate all peptides as they are read in.
  // Default is not to (although peptides are retranslated if the user makes any
  // changes to the transcript).
  // WashingLine sets this to true so it can be used to fix any peptides that are wrong.
  private static boolean           refreshPeptides = false;

  /** Adapter to use for command line xml file, "game" or "chado" */
  private static String commandLineXmlFileFormat = "game";

  public static String             osarch;
  public static String             osname;
  public static String             jvm;
  public static String javaws;

  private static String            fileName;  // The name of the config file

  /** Which browser program to launch for reports--if not set, BrowserLauncher tries to find
   * default browser.  In other words, this needs to be set only if BrowserLauncher is failing
   * to automatically find a browser, e.g. on Linux, which usually has mozilla but not netscape.
   */
  private static String            browserProgram; // ="netscape";

  /** Whether to set up servlet to listen to http requests (from igb) */
  private static boolean httpServerIsEnabled = false;

  private static boolean           initialized = false;

  public static boolean DO_ONE_LEVEL_ANNOTS = false;

  public static boolean            inBatchMode = false;

  private static boolean  supressMacJvmPopup = false;
  
  private static String soObo;
  private static String apolloObo;

  // TODO: remove DEBUG and getDebug() completely and replace with feature-specific
  // flags that can be used to *independently* turn the various experimental features
  // on and off.

  /** 
   * Debug mode for Apollo developers; references to this variable are scattered 
   * throughout the Apollo code.  Setting this flag to true enables several 
   * (unrelated) experimental Apollo features that appear to be under development.
   * This flag used to control the printing of debug statements, but this is now
   * handled by Log4J and should be configured in the log4j config. file (which is
   * set by the apollo start script.)
   */
  public static boolean DEBUG = false;

  static {
    javaws = System.getProperty("javawebstart.version");
  }
  
  // Returns the current value of <code>DEBUG</code>.  Only one or two classes used
  // this method; most seemed to access Config.DEBUG directly.
  public static boolean getDebug() {
    init();
    return DEBUG;
  }

  private static void setDebug(boolean debug) {
    DEBUG = debug;
  }

  /** Data adapter may have changed - fetch the matching style */
  private static void setDataAdapterType(String adapterType) {
    if (!adapterToStyle.containsKey(adapterType)) {
      String message = "Failed to set style "+adapterType+"--couldn't find style file.  This could be a problem!";
      logger.error(message);
      apollo.util.IOUtil.errorDialog(message);
      return; //??
    }
    Style oldStyle = style;
    Style newStyle = (Style)adapterToStyle.get(adapterType);
    if (oldStyle==newStyle)
      return;
    setStyle(newStyle);
  }

  public static void setStyle(Style s) {
    // second parameter is whether to update 
    // the style hashtable with this new style
    setStyle(s, false);
  }

  /** if updateStyleHash is true add key style filename to value style to
      filenameToStyle hash */
  public static void setStyle(Style s, boolean updateStyleHash) {
    if (s == Config.style) {
      return; // not a real style change
    }
    // clean up old style (dangling refs) before set to new
    style = s;
    // set genetic code in style
    DNAUtils.setGeneticCode(s.getGeneticCodeNumber());
    // PropertyScheme holds all the tiers info
    //addTypesObserverToPropertyScheme(style.getPropertyScheme());
    // have to notify world(tierManager) of tiers/propScheme change
    // cant do this here - prop scheme may not have listener yet - do in guiCurState
    //style.getPropertyScheme().firePropSchemeChangeEvent();
    if (updateStyleHash) {
      String styleFile = s.getFileName();
      filenameToStyle.put(styleFile, s);
      // Now we have to go through the hashtable and find all 
      // the keys whose value is this style...
      Iterator keys = adapterToStyle.keySet().iterator();
      while (keys.hasNext()) {
        Object key = keys.next();
        if (((Style)adapterToStyle.get(key)).getFileName().equals(styleFile)) {
          adapterToStyle.put(key, s);
        }
      }
    }
  }

  /** Data adapter has changed - fetch the matching style - i think the purpose
   *  of this was to change the style - but AbstractApolloAdapter seems to do 
   *  this explicitly now through setStyle(s) because if a dataadapter is a child
   *  to the synteny adapter, the synteny style will be used in lieu of the 
   *  data adapters normal style - though im not sure what i think about that 
   *  AnalysisAdapterGUI still uses this but it probably shouldnt because of synteny 
  */
  public static void newDataAdapter(DataAdapter da) {
    // Sets global variable "style"
    setDataAdapterType(da.getClass().getName());
    logger.info("Set style to " + da.getClass().getName() + 
                     "; style file source(s): " + style.getAllStyleFilesString());
  }

  /** Returns Style for adapter class string, null if non existent */
  public static Style getStyle(String adapterType) {
    return (Style)adapterToStyle.get(adapterType);
  }

  public static Style getStyle() {
    return style;
  }

  private static HashMap speciesToStyle;

  public static boolean hasStyleForSpecies(String species) {
    if (species == null)
      return false;
    if (speciesToStyle == null)
      return false;
    return speciesToStyle.get(species) != null;
  }

  public static void setStyleForSpecies(String species) {
    if (!hasStyleForSpecies(species))
      return;
    Style speciesStyle = (Style)speciesToStyle.get(species);
    logger.info("Species style override. Setting style to "+speciesStyle+
                     " for species "+species);
    setStyle(speciesStyle);
  }

  private static void addSpeciesToStyle(String keyValueArrowString) {
    KeyValue keyValue = new KeyValue(keyValueArrowString);
    Style speciesStyle = createStyle(keyValue.value);
    if (speciesToStyle == null)
      speciesToStyle = new HashMap(3);
    speciesToStyle.put(keyValue.key,speciesStyle);
  }

  /** takes "key -> value" string and parses out key & value */
  private static class KeyValue {
    private String key;
    private String value;
    private KeyValue(String arrowString) {
      int arrowIndex = arrowString.indexOf("->");
      key = arrowString.substring(0,arrowIndex);
      value = arrowString.substring(arrowIndex+2);
      key = key.trim();
      value = value.trim();
    }
  }


  public static Color getAnnotationBackground() {
    return style.getAnnotationBackground();
  }

  public static Color getFeatureBackground() {
    return style.getFeatureBackground();
  }

  public static int getEdgematchWidth() {
    return style.getEdgematchWidth();
  }

  public static boolean verticallyMovePopups() {
    return style.verticallyMovePopups();
  }

  public static Color getCoordBackground() {
    return style.getCoordBackground();
  }
  public static Color getCoordForeground() {
    return style.getCoordForeground();
  }
  public static Color getCoordRevcompColor() {
    return style.getCoordRevcompColor();
  }

  public static Color getSequenceColor() {
    return style.getSequenceColor();
  }
  public static Color getEdgematchColor() {
    return style.getEdgematchColor();
  }

  public static Color getHighlightColor() {
    return style.getHighlightColor();
  }

  public static Color getSelectionColor() {
    return style.getSelectionColor();
  }

  public static Color getSeqGapColor() {
    return style.getSeqGapColor();
  }

  public static Color getFeatureLabelColor() {
    return style.getFeatureLabelColor();
  }

  public static Color getAnnotationLabelColor() {
    return style.getAnnotationLabelColor();
  }

  public static Color getDataLoaderBackgroundColor() {
    return dataLoaderBackgroundColor;
  }
  public static Color getDataLoaderLabelColor() {
    return dataLoaderLabelColor;
  }

  public static Color getDataLoaderTitleColor() {
    return dataLoaderTitleColor;
  }

  public static Color getExonDetailEditorBackgroundColor1() {
    return style.getExonDetailEditorBackgroundColor1();
  }
  public static Color getExonDetailEditorBackgroundColor2() {
    return style.getExonDetailEditorBackgroundColor2();
  }
  public static Color getExonDetailEditorFeatureColor1() {
    return style.getExonDetailEditorFeatureColor1();
  }
  public static Color getExonDetailEditorFeatureColor2() {
    return style.getExonDetailEditorFeatureColor2();
  }

  public static Vector getAnnotationComments() {
    return style==null ? null : style.getAnnotationComments();
  }

  public static Vector getTranscriptComments() {
    return style==null ? null : style.getTranscriptComments();
  }

  public static Hashtable getPeptideStates () {
    return style==null ? null : style.getPeptideStates();
  }

  public static PeptideStatus getPeptideStatus(String pep_status) {
    return style==null ? null : style.getPeptideStatus(pep_status);
  }

  public static DataAdapterRegistry getAdapterRegistry() {
    init();
    return adapterRegistry;
  }

  public static boolean dataAdapterIsAvailable (IOOperation io) {
    DataAdapter [] allAdapters = adapterRegistry.getAdapters(io, false);
    return (allAdapters.length > 0);
  }

  public static DisplayPrefsI getDisplayPrefs() {
    return (style == null ? 
            new DefaultDisplayPrefs() : 
            style.getDisplayPrefs());
  }

  public static OverlapI getOverlapper(SeqFeatureI sf) {
    PropertyScheme scheme = getPropertyScheme();
    FeatureProperty fp = scheme.getFeatureProperty(sf.getTopLevelType());
    OverlapI overlapper = null;
    if (fp != null) {
      overlapper = fp.getOverlapper();
    }
    if (overlapper == null) {
      if (defaultOverlapper == null) {
        defaultOverlapper = SimpleOverlap.getSimpleOverlap();
      }
      overlapper = defaultOverlapper;
    }
    return overlapper;
  }

  public static String getTiersFile() {
    init();
    String tiersfile = style.getTiersFile();
    if (tiersfile == null) {
      String err = "can't find Types (tiers) file for style file " + 
        style.getFileName() + 
        ".\nPlease fix the Types parameter in " +
        style.getFileName() +
        ".\nYou may then need to restart Apollo.";
        logger.error(err);
      apollo.util.IOUtil.errorDialog(err);
    }
    return tiersfile;
  }
  public static String getKaryotypeFile() {
    init();
    return karyotypeFile;
  }
  public static String getSyntenyDir() {
    init();
    return syntenyDir;
  }
  public static String getExonSyntenyDir() {
    init();
    return exonSyntenyDir;
  }
  public static boolean getShowAnnotations() {
    init();
    return  style==null ? false : style.getShowAnnotations();
  }

  public static String getLayoutIniFile() {
    init();
    return  style==null ? null : style.getLayoutIniFile();
  }

  public static boolean getShowResults() {
    init();
    if (style==null)
      return true;
    return style.getShowResults();
  }
  public static boolean getShowSyntenyEvidencePanels() {
    return showSyntenyEvidencePanels;
  }

  public static boolean isEditingEnabled() {
    init();
    if (style==null)
      return false;
    return style.isEditingEnabled();
  }

  
  public static boolean isEditingEnabled(String adapterClassName){
    init();
    if (Config.getStyle(adapterClassName)==null){
      return false;
    }//end if
    
    return Config.getStyle(adapterClassName).isEditingEnabled();
  }
  
  public static boolean isNavigationManagerEnabled() {
    init();
    if (style==null)
      return false;
    return style.isNavigationManagerEnabled();
  }

  public static int getAutosaveInterval() {
    init();
    return autosaveInterval;
  }

  public static int getCGIPort() {
    init();
    return cgiPort;
  }
  public static String getCGIScript() {
    init();
    return cgiScript;
  }
  public static String getCGIHost() {
    init();
    return cgiHost;
  }
  public static String getDBHost() {
    init();
    return dbHost;
  }
  public static String getDBName() {
    init();
    return dbName;
  }
  public static String getGPName() {
    init();
    return gpName;
  }
  public static String getSRSServer() {
    init();
    return srsServer;
  }
  public static String getPFetchServer() {
    init();
    return pfetchServer;
  }
  public static String getDASServer() {
    init();
    return dasServer;
  }

  public static String getAutosaveFile() {
    init();
    return autosaveFile;
  }

  /** This should be taken out of here and made a singleton method in controller
   *  Config has nothing to do with the controller */
  public static Controller getController() {
    init();
    return controller;
  }

  /** controller should be taken out of config at some point - controller now
   *  set by default to master controller - take out setController? not used */
  public static void setController(Controller newValue) {
    controller = newValue;
  }

  public static int getSiteShowLimit() {
    init();
    return siteShowLimit;
  }

  public static int getSyntenyPanelRelativeSize() {
    init();
    return syntenyPanelRelativeSize;
  }

  public static int getTranslationShowLimit() {
    init();
    return translationShowLimit;
  }

  public static int getTextAvoidLimit() {
    init();
    return textAvoidLimit;
  }

  /** Should there be an apollo wide font as well as style level font? */
  public static Font getDefaultFont() {
    // should it return some default font is style is null?
    return style==null ? null : style.getDefaultFont();
  }

  /** Default font for gene label if it does not have a peptide status(which has a font) 
      gene label appears as label to feature and in gene menu list */
  public static Font getDefaultFeatureLabelFont() {
    return style==null ? null : style.getDefaultFeatureLabelFont();
  }

  public static float getFastDrawLimit() {
    init(); // ?
    return fastDrawLimit;
  }

  public static boolean getDrawOutline() {
    return style.getDrawOutline();
  }

  public static Color getOutlineColor() {
    return style.getOutlineColor();
  }

  public static String getFrameOrientation() {
    init();  // ?
    return frameOrientation;
  }

  public static int getMainWindowWidth() {
    return mainWindowWidth;
  }

  public static String getGeneUrl() {
    return style==null ? null : style.getGeneUrl();
  }

  public static String getBandUrl() {
    return style==null ? null : style.getBandUrl();
  }

  public static String getScaffoldUrl() {
    return style==null ? null : style.getScaffoldUrl();
  }

  public static String getSeqUrl() {
    return style==null ? null : style.getSeqUrl();
  }

  public static String getRangeUrl() {
    return style==null ? null : style.getRangeUrl();
  }

  public static boolean hasChromosomes() {
    return style==null ? false : style.hasChromosomes();
  }
  /** Returns a vector of chromosome names as strings, null if none given */
  public static Vector getChromosomes() {
    return style==null ? null : style.getChromosomes();
  }

  /** previously fban url - attempting to make more general */
  public static String getExternalRefURL() {
    return style==null ? null : style.getExternalRefUrl();
  }

  // Used for deciding whether to show internal-only comments.
  public static boolean internalMode() {
      return style == null ? false : style.internalMode();
  }

  public static String getProjectName (String username) {
    return style == null ? username : style.getProjectName(username);
  }

  public static String getFileName() {
    return fileName;
  }

  // surely this should be void & throw an exception if readConfig fails?
  private static boolean readConfig(File confFile) {
    fileName = confFile.getPath();

    try {
      StreamTokenizer tokenizer =
        new StreamTokenizer(new BufferedReader(new FileReader(confFile)));
      tokenizer.eolIsSignificant(true);
      tokenizer.slashStarComments(true);

      boolean EOF     =  false;
      int     tokType =  0;
      Vector  words   =  new Vector();
      while (!EOF) {
        if ((tokType = tokenizer.nextToken()) == StreamTokenizer.TT_EOF) {
          EOF = true;
        } else if (tokType != StreamTokenizer.TT_EOL) {
          if (tokenizer.sval != null) {
            words.addElement(tokenizer.sval);
          }
        }
        // DataAdapterInstall has three or four words; most other keys have just two
        // (up to two extra will be ignored)
        else {
          if (words.size() == 2 || words.size() == 3 || words.size() == 4) {
            String key   = (String)words.elementAt(0);
            String value = (String)words.elementAt(1);
            // for DataAdapterInstall style
            String styleName=null;
            String dataAdapterLabel=null;
            if (words.size()>=3)
              styleName = (String) words.elementAt(2);
            if (words.size()==4)
              dataAdapterLabel = (String) words.elementAt(3);
            if (key.equalsIgnoreCase("AutosaveInterval")) {
              if (value.equalsIgnoreCase("none")) {
                setAutosaveInterval(-1);
              } else {
                try {
                  setAutosaveInterval(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                  logger.warn("Can't parse number: " + value + " for AutosaveInterval");
                }
              }
            } else if (key.equalsIgnoreCase("CGIPort")) {
              if (value.equalsIgnoreCase("none")) {
                setCGIPort(-1);
              } else {
                try {
                  setCGIPort(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                  logger.warn("Can't parse number: " + value + " for CGIPort");
                }
              }
            } else if (key.equalsIgnoreCase("SiteShowLimit")) {
              try {
                setSiteShowLimit(Integer.parseInt(value));
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value + " for SiteShowLimit");
              }
            } else if (key.equalsIgnoreCase("TextAvoidLimit")) {
              try {
                setTextAvoidLimit(Integer.parseInt(value));
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value + " for TextAvoidLimit");
              }
	    } else if (key.equalsIgnoreCase("TranslationShowLimit")) {
              try {
                setTranslationShowLimit(Integer.parseInt(value));
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value);
              }
            } else if (key.equalsIgnoreCase("SyntenyPanelRelativeSize")) {
              try {
                setSyntenyPanelRelativeSize(Integer.parseInt(value));
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value);
              }
	    } else if (key.equalsIgnoreCase("ShowSyntenyEvidencePanels")) {
	      showSyntenyEvidencePanels = (new Boolean(value)).booleanValue();

            } else if (key.equalsIgnoreCase("FastDrawLimit")) {
              try {
                setFastDrawLimit(Float.valueOf(value).floatValue());
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value + " for FastDrawLimit");
              }
	    } else if (key.equalsIgnoreCase("SyntenyDir")) {
		syntenyDir = value;
	    } else if (key.equalsIgnoreCase("ExonSyntenyDir")) {
		exonSyntenyDir = value;
            } else if (key.equalsIgnoreCase("CGIHost")) {
              cgiHost = value;
            } else if (key.equalsIgnoreCase("DBHost")) {
              dbHost = value;
            } else if (key.equalsIgnoreCase("DBName")) {
              dbName = value;
            } else if (key.equalsIgnoreCase("GoldenPathType")) {
              gpName = value;
            } else if (key.equalsIgnoreCase("SRSServer")) {
              srsServer = value;
            } else if (key.equalsIgnoreCase("PFetchServer")) {
              pfetchServer = value;
            } else if (key.equalsIgnoreCase("CGIScript")) {
              cgiScript = value;
            } else if (key.equalsIgnoreCase("DASServer")) {
              dasServer = value;
            } else if (key.equalsIgnoreCase("PublicSeqDbURL")) {
              if (pubDbToURL == null)
                pubDbToURL = new Hashtable();
              addStringToHash(value, pubDbToURL);
            } else if (key.equalsIgnoreCase("AutosaveFile")) {
              // true means ok to create new file if it doesn't exist
              autosaveFile = apollo.util.IOUtil.findFile(value, true);
            } else if (key.equalsIgnoreCase("FrameOrientation")) {
              frameOrientation = value;
            } else if (key.equalsIgnoreCase("MainWindowWidth")) {
              mainWindowWidth = Integer.parseInt(value);
            } else if (key.equalsIgnoreCase("AdapterHistoryFile")) {
              String hisfile = apollo.util.IOUtil.findFile(value, true);
              setAdapterHistoryFile(hisfile);
            } else if (key.equalsIgnoreCase("JavaPath")) {
              // do nothing
            }
            else if (key.equalsIgnoreCase("Memory")) {
	      if (value.indexOf("M") >= 0) {
		value = value.substring(0, value.indexOf("M"));
	      }
              try {
		// 1048576 is actual number of bytes in a megabyte
		memoryAllocation = (long)(Integer.parseInt(value))*1048576;
              } catch (NumberFormatException e) {
                logger.warn("Can't parse number: " + value + " for Memory " + value);
	      }
            } else if (key.equalsIgnoreCase("DataAdapterInstall")) {
              // a mandatory data adapter style is in styleName
              installDataAdapter(value,styleName,dataAdapterLabel);
              setDataAdapterType(value);
            } else if (key.equalsIgnoreCase("BrowserProgram")) {
              setBrowserProgram(value);
            } else if (key.equalsIgnoreCase("Karyotypes")) {
              karyotypeFile = new String(value);

              if (osname.startsWith("Windows")) {
                if (karyotypeFile.indexOf(":") < 0 ||
                    karyotypeFile.indexOf("\\") != 0) {
                  karyotypeFile = getRootDir() + "data\\" + karyotypeFile;
                }
              } else if (karyotypeFile.indexOf("/") != 0) {
                karyotypeFile = getRootDir() + "/data/" + karyotypeFile;
              }
            }
            else if (key.equals("OutputChadoTransaction")) {
              outputChadoTransaction = Boolean.valueOf(value).booleanValue();
            }
            else if (key.equals("OutputTransactionXML")) {
              outputTransactionXML = Boolean.valueOf(value).booleanValue();
            }
            else if (key.equals("ChadoTransactionTemplate")) {
              chadoTemplFileName = value;
            }
            else if (key.equals("ChadoJdbcAdapterConfigFile")) {
              setChadoJdbcConfigFile(value);
            }
            else if (key.equals("CommandLineXmlFileFormat")) {
              commandLineXmlFileFormat = value;
            }
            else if (key.equalsIgnoreCase("AskConfirmOverwrite")) {
              if (checkBoolean(value, "AskConfirmOverwrite"))
                confirmOverwrite = (new Boolean(value)).booleanValue();
            }
            else if (key.equalsIgnoreCase("AskConfirmDeleteSelection")) {
              if (checkBoolean(value, "AskConfirmDeleteSelection"))
                confirmDeleteSelection = (new Boolean(value)).booleanValue();
            } 
	    else if (key.equalsIgnoreCase("CheckOwnership")) {
	      if (checkBoolean(value, "CheckOwnership"))
		checkOwnership = (new Boolean(value)).booleanValue();
            }
            else if (key.equalsIgnoreCase("SaveClonedFeaturesInTransactions")) {
              if (checkBoolean(value, "SaveClonedFeaturesInTransactions")) 
                saveClonedFeaturesInTransactions = (new Boolean(value)).booleanValue();
            }
            else if (key.equalsIgnoreCase("TigrAnnotNameAdapterNewNameCommand")) {
              tigrAnnotNameAdapterNewNameCommand = value;
            }
            else if (key.equalsIgnoreCase("ShowSaveOptionInFileMenu")) {
              if (checkBoolean(value, "ShowSaveOptionInFileMenu"))
                showSaveOptionInFileMenu = (new Boolean(value)).booleanValue();
            }
            else if (key.equalsIgnoreCase("ShowSaveAsOptionInFileMenu")) {
              if (checkBoolean(value, "ShowSaveAsOptionInFileMenu"))
                showSaveAsOptionInFileMenu = (new Boolean(value)).booleanValue();
            }
            else if (key.equalsIgnoreCase("SpeciesToStyle")) {
              addSpeciesToStyle(value);
            }
            else if (key.equalsIgnoreCase("EnableHttpServer")) {
              if (checkBoolean(value,key))
                httpServerIsEnabled = getBoolean(value);
            }
            else if (key.equalsIgnoreCase("DEBUG")) {
              if (checkBoolean(value,key))
                setDebug(getBoolean(value));
                DEBUG = getBoolean(value);
            }
            else if (key.equalsIgnoreCase("DO-ONE-LEVEL-ANNOTS")) {
              if (checkBoolean(value,key))
                setDoOneLevelAnnots(getBoolean(value));
            }
            else if (key.equalsIgnoreCase("SupressMacJvmPopupMessage")) {
              if (checkBoolean(value,key))
                supressMacJvmPopup = getBoolean(value);
            }
            else if (key.equalsIgnoreCase("SequenceOntologyOBO")) {
              soObo = IOUtil.findFile(value);
            }
            else if (key.equalsIgnoreCase("ApolloOBO")) {
              apolloObo = IOUtil.findFile(value);
            }
            else {
              logger.warn("Unknown config key " + key + " in config file " + fileName);
            }
          } else {
            if (words.size() == 1) {
              logger.warn("Error parsing " + fileName + ": only 1 word on line: " 
                          + words.elementAt(0));
            }
            else if (words.size() != 0) {
              logger.warn("Error parsing " + fileName +
                          ": too many words on line beginning " +
                          (String)words.elementAt(0) + " " + 
                          (String)words.elementAt(1) + " " +
                          ((words.size() > 1) ? (String)words.elementAt(2) : ""));
            }
          }

          words.removeAllElements();
        }
      }
      return true;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return false;
    }
  }

  private static void setDoOneLevelAnnots(boolean doOne) {
    DO_ONE_LEVEL_ANNOTS = doOne;
    apollo.editor.AnnotationEditor.setDoOneLevelAnnots(doOne);
  }

  /** Create a style from the stylefile for the driverName */
  private static void createStyle(String styleFilename, String driverName) {
    Style newStyle = createStyle(styleFilename);
    if (newStyle!=null)
      adapterToStyle.put(driverName, newStyle);
  }

  /** If already have style for styleFilename, return that. In other words doesnt 
      create 2 Styles for same style file. */
  public static Style createStyle(String styleFilename) {
    if (styleFilename == null)
      styleFilename = "default.style";
    if (filenameToStyle.containsKey(styleFilename)) { // check if already created style
      return (Style)filenameToStyle.get(styleFilename);
    }

    // First read the default style file for this style (should be in apollo/conf)
    String pathname = styleFilename;      
    if (!styleFilename.startsWith("conf/"))  
      pathname = "conf/" + pathname;
    String styleFilePath = apollo.util.IOUtil.findFile(pathname);
    if (styleFilePath == null || isJavaWebStartApplication()) {
      logger.debug("createStyle: couldn't find " + styleFilename + " with findFile--trying digForConfigFile");
      styleFilePath = digForConfigFile(styleFilename);
    }

    Style newStyle = null;
    if (styleFilePath != null) {
      newStyle = new Style(styleFilePath);
      if (newStyle == null) { // how is this possible?
        String err = "Couldn't read style file " + styleFilePath;
        logger.error(err);
        apollo.util.IOUtil.errorDialog(err);
        return null;
      }
      logger.debug("createStyle: read style file " + styleFilePath);
    }

    // 7/2005: Special backwards-compatibility hand-holding:
    // game.style was recently replaced with fly.style (which imports game.style)
    // but some users still have a private game.style in their .apollo directory.
    // If there is one, ask if they want to rename it to fly.style.
    if (styleFilename.indexOf("fly") >= 0)
      Style.askRenameGameConfig("game.style", "fly.style");

    // Now see if the user has a personalized style file in their .apollo directory,
    // and if so, read it and modify the existing style. findFile gets full path
    styleFilePath = apollo.util.IOUtil.findFile("~/.apollo/" + styleFilename);
    if (styleFilePath != null) {
      File sf = new File(styleFilePath);
      // If we didn't yet find a style, set it up now
      if (newStyle == null)
        newStyle = new Style(styleFilePath);
      // We already read a style file; add on this personal one
      else {
        newStyle.setStyleFile(styleFilePath);
        newStyle.readStyle(sf);
      }
      logger.debug("createStyle: read personal style file " + styleFilePath);
    }
    if (newStyle != null)
      filenameToStyle.put(styleFilename, newStyle);
    return newStyle;
  }

  /** Look around for style file, pulling out of the jar if necessary
      (needed for webstart) */
  public static String digForConfigFile(String styleFilename) {
    String pathname = styleFilename;
    // Try looking in .apollo first
    String styleFilePath = apollo.util.IOUtil.findFile(pathname); // gets full path
    // If it's not there, look in conf.
    if (styleFilePath == null && !styleFilename.startsWith("conf/")) {
      pathname = "conf/" + pathname;
      styleFilePath = apollo.util.IOUtil.findFile(pathname);
    }
    if (styleFilePath == null || isJavaWebStartApplication()) {  // try to get from jar
      try {
        logger.info("Couldn't find style file " + styleFilename + "--trying to get from jar"); // DEL
        styleFilePath = getRootDir() + "/conf/" + styleFilename;
        // Need style file name without any path
        if (styleFilename.indexOf("/") > 0)
          styleFilename = styleFilename.substring(styleFilename.indexOf("/")+1);
        File style = new File(styleFilePath);
        ensureExists(style, styleFilename, true);
      }
      catch (Exception e) {
        logger.info("Couldn't get " + styleFilePath + " from jar, either.");
        styleFilePath = null;
      }
    }
    logger.debug("digForConfigFile: found " + styleFilename + " in " + styleFilePath);
    return styleFilePath;
  }

  /** installString is first field of DataAdapterInstall in apollo.cfg, which is
      the full class name(with package) of DataAdapter. Installs data adapter in
      adapterRegistry. DataAdapter must have empty constructor. 
      Optional data adapter label is used to call setName() on the data adapter
      (otherwise, it just keeps its default name label). */
  private static void installDataAdapter(String installString, String style, String label) {
    logger.debug("installDataAdapter(" + installString + ", " + style + ", " + label);
    String driverName = installString;
    try {
      int breakIndex = driverName.indexOf(":");
      if (breakIndex > 0)
        driverName = installString.substring(0,breakIndex);

      // In case this adapter was already installed, remove it (if it's not already there,
      // no harm done)
      adapterRegistry.removeDataAdapter(driverName);
      // EnsJ adapter and Synteny adapter require JDK1.4+ but other data adapters are ok with 1.3.
      // If we're running <1.4, refuse to load EnsJ and Synteny data adapter; that way we
      // can still run Apollo with other data adapters.
      if (canRunWithThisJVM(driverName)) {
        //Style is created before dataadapter so that the adapter
        //can reference style, if necessary.
        createStyle(style,driverName);
        adapterRegistry.installDataAdapter(driverName);
        if (label != null && !(label.equals(""))) {
          // Tell this adapter what its label should be
          DataAdapter newAdapter = adapterRegistry.getAdapter(adapterRegistry.getAdapterCount()-1);
          // AbstractApolloAdapter has a setName method.  Since we don't have
          // direct access to DataAdapter (it's an org.bdgp class), adapters
          // that don't inherit from AbstractApolloAdapter can't set their
          // name labels this way.
          if (newAdapter instanceof AbstractApolloAdapter) {
            // 12/2005 For the GAME adapter, this just changes the label for READING;
            // the label for WRITING is specified by the GAME adapter itself.
            ((AbstractApolloAdapter)newAdapter).setName(label);
          } else { // for debugging
            logger.warn("unable to call setName(\"" + label + "\") on " + newAdapter);
          }
        }
      }
      else {
        logger.warn(driverName + " can't run with Java version " + jvm);
      }
    } catch (org.bdgp.io.DataAdapterException e) {
      logger.error("could not install driver "+driverName+" because of: "+e);
    }
  }

  // EnsJ and Synteny adapters now requirs JDK1.4+ but other data adapters are ok with 1.3.
  private static boolean canRunWithThisJVM(String driverName) {
    if (((driverName.indexOf("EnsJ") > 0) || (driverName.indexOf("Synteny") > 0))
        && (jvm.indexOf("1.4") < 0) && (jvm.indexOf("1.5") < 0) && (jvm.indexOf("1.6") < 0)) {
      logger.error("JVM " + jvm + " doesn't support data adapter " + driverName);
      return false;
    }
    else
      return true;
  }

  public static String getRootDir() {
    init();
    return rootDir;
  }

  public static String getDataDir() {
    init();
    return dataDir;
  }

  public static int getMaxHistoryLength() {
    init();
    return 5;
  }

  public static String getAdapterHistoryFile() {
    init();
    return adapterHistoryFile;
  }

  /** Return true if both configged to true AND NOT in command-line/batch mode 
      this boolean causes a popup that shouldnt happen in cmd line mode and presently
      there is no cmd-line confirm of overwrite - if this is added then the cmd line
      should be tested downstream - fine with present code i think */
  public static boolean getConfirmOverwrite() {
    return !CommandLine.isInCommandLineMode() && confirmOverwrite;
  }
  public static void setConfirmOverwrite(boolean state) {
    confirmOverwrite = state;
  }

  public static boolean getShowSaveOptionInFileMenu() {
    return showSaveOptionInFileMenu;
  }
  public static void setShowSaveOptionInFileMenu(boolean state) {
    showSaveOptionInFileMenu = state;
  }

  public static boolean getShowSaveAsOptionInFileMenu() {
    return showSaveAsOptionInFileMenu;
  }
  public static void setShowSaveAsOptionInFileMenu(boolean state) {
    showSaveAsOptionInFileMenu = state;
  }

  /** Return true if configged to true AND NOT in command-line/batch mode */
  public static boolean getConfirmDeleteSelection() {
    return !CommandLine.isInCommandLineMode() && confirmDeleteSelection;
  }
  public static void setConfirmDeleteSelection(boolean state) {
    confirmDeleteSelection = state;
  }

  public static boolean getCheckOwnership() {
    return checkOwnership;
  } 
  public static boolean getSaveClonedFeaturesInTransactions() {
    return saveClonedFeaturesInTransactions;
  }
  public static void setSaveClonedFeaturesInTransactions(boolean state) {
    saveClonedFeaturesInTransactions = state;
  }

  public static String getTigrAnnotNameAdapterNewNameCommand() {
    return tigrAnnotNameAdapterNewNameCommand;
  }
  public static void setTigrAnnotNameAdapterNewNameCommand(String command) {
    tigrAnnotNameAdapterNewNameCommand = command;
  }
  
  public static boolean httpServerIsEnabled() {
    return httpServerIsEnabled;
  }

  public static boolean getDashSets() {
    return style.getDashSets();
  }
  public static boolean useFastClipGraphics() {
    return fastClipGraphics;
  }
  public static boolean doUserTranscriptColouring() {
    return style.doUserTranscriptColouring();
  }
  public static boolean getDraw3D() {
    return style.getDraw3D();
  }
  public static boolean getNoStripes() {
    return style.getNoStripes();
  }

  /** When multi-curations have multi-propSchemes this method will become pase.
      Currently multi-curations all use same prop scheme, but that may change.*/
  public static PropertyScheme getPropertyScheme() {
    init();
    return getStyle().getPropertyScheme();
  }

  /** New method take an AnnotatedFeatureI (an annot or transcript) */
  public static Color getAnnotationColor(AnnotatedFeatureI annot) {
    // Usernames are associated with transcripts, not annots
    AnnotatedFeatureI transcript=null;
    if (annot instanceof Transcript)
      transcript = (Transcript) annot;
    else if (annot.hasKids() && annot.getFeatureAt(0).isTranscript())
      //transcript = (Transcript) annot.getFeatureAt(0).getTranscript();
      transcript = annot.getFeatureAt(0).getAnnotatedFeature();
    else if (!annot.hasKids()) {
      FeatureProperty property =getPropertyScheme().getFeatureProperty(annot.getTopLevelType());
      return(property.getColour());
    }
    else
      return defaultAnnotationColor;
    String owner = transcript.getOwner();
    FeatureProperty fp = getPropertyScheme().getFeatureProperty(transcript.getTopLevelType());
    return getAnnotationColor(owner, fp);
  }

  public static Color getAnnotationColor(String username,
                                         FeatureProperty fp) {
    // Use default annotation color if we're in public mode or
    // user transcript colouring is turned off.
    if (!doUserTranscriptColouring() || !getStyle().internalMode())
      return fp.getColour();
    if (username == null || username.equals("") || username.equalsIgnoreCase("auto"))
      return fp.getColour();
    if (style==null)  // Why would this happen?
      return defaultAnnotationColor;

    // else
    return style.getAnnotationColor(username);
  }

  public static Color getDefaultAnnotationColor() {
    FeatureProperty fp = getPropertyScheme().getFeatureProperty("gene");
    if (fp == null)
      return defaultAnnotationColor;
    else
      return fp.getColour();
  }

  public static String getBlixemLocation() {
    init();
    return style.getBlixemLocation();
  }

  public static String getBrowserProgram() {
    return browserProgram;
  }
  public static void setBrowserProgram(String browser) {
    browserProgram = browser;
  }

  public static Font getExonDetailEditorSequenceFont() {
    return style.getExonDetailEditorSequenceFont();
  }

  public static long getMemoryAllocation() {
    return memoryAllocation;
  }
  
  public static void main(String [] args) {
    if (args.length == 1) {
      Config.setDataDir(args[0]);
    }
    logger.info("Data dir    = " + Config.getDataDir());
    logger.info("Tiers       = " + Config.getPropertyScheme());
  }

  // if every get method has init() in it this wont be needed
  public static void initializeConfiguration() {
    init();
  }
  
  /** Checks to see if this application is a Java Web Start application
   * 
   * @return true if it is a Java Web Start application
   */
  public static boolean isJavaWebStartApplication()
  {
    return javaws != null;
  }
  
  /** Get the Java Web Start version.  It returns null if not set
   *  (therefore not a Java Web Start application)
   *  
   * @return Java Web Start Version if a JWS application, otherwise null
   */
  public static String getJavaWebStartVersion()
  {
    return javaws;
  }
  
  /** Checks whether the system is running in headless mode.  Checks
   *  for the existence of the "java.awt.headless" property and that
   *  it is set to true.
   *
   * @return true if the program is running in headless mode
   */
  public static boolean isHeadlessApplication()
  {
    String headless = System.getProperty("java.awt.headless");
    return headless != null && headless.equalsIgnoreCase("true");
  }

  private static void init() {
    if(initialized == false) {
      initialized = true;

      osarch = System.getProperty("os.arch");
      osname = System.getProperty("os.name");
      jvm = System.getProperty("java.version");
      rootDir = apollo.util.IOUtil.getRootDir();
      logger.info("OS name: " + osname);
      logger.info("OS arch: " + osarch);
      logger.info("Java version: " + jvm);

      // Set default browser for Linux
      // (may later be overridden by config file)
      if (IOUtil.isUnix() && browserProgram == null) {
        browserProgram = System.getProperty("BROWSER");  // Set by bin/apollo
        // If it hasn't yet been defined, need to figure out real path to browser
        if (browserProgram == null || browserProgram.equals("")) {
          browserProgram = findBrowser("mozilla");
          if (browserProgram == null || browserProgram.equals(""))
            // netscape doesn't seem to require a real physical path
            browserProgram = findBrowser("firefox");
          if (browserProgram == null || browserProgram.equals(""))
            browserProgram = "netscape";
        }
      }

        // Better to let BrowserLauncher handle this.
//       // Set default browser for Mac OS X
//       // (may be overridden by config file)
//       if (osname.startsWith ("Mac")) {
//         browserProgram = "Internet Explorer";
//       }

      // Look for config file (apollo.cfg)
      // First read the apollo.cfg that's in apollo/conf.
      // THEN read the user's personal apollo.cfg, which can include only
      // lines that are a modification of the global apollo.cfg.
      String configFileName = apollo.util.IOUtil.findFile("conf/apollo.cfg");
      if (configFileName == null || isJavaWebStartApplication()) {  // Couldn't find--try to get it from jar
        logger.warn("couldn't find apollo.cfg--trying to get from jar");
        configFileName = getRootDir() + "/conf/apollo.cfg";
        File cfg = new File(configFileName);
        try {
          ensureExists(cfg, "apollo.cfg", true);  // true means recreate
        } catch (Exception e) {
          logger.error("couldn't get apollo.cfg from jar");
          configFileName = null;
        }
      }
      // If we still can't find the config file, we should give up now.
      if (configFileName == null) {
        String err = "Couldn't find config file (apollo.cfg) anywhere!";
        logger.fatal(err);
        apollo.util.IOUtil.errorDialog(err);
        System.exit(-1);
      }

      // Read the global config file
      File configFile = new File(configFileName);
      try {
        logger.info("reading config file " + configFileName);
        boolean success = readConfig(configFile);
        if (!success) {
          String err = "Failed to read config file " + configFileName;
          logger.fatal(err);
          apollo.util.IOUtil.errorDialog(err);
          System.exit(-1);
        }
      } catch (Exception e) {
        // If we can't find the config file, we should give up now.
        String err = "Couldn't open config file " + configFileName + ": " + e.getMessage();
        logger.fatal(err);
        apollo.util.IOUtil.errorDialog(err);
        System.exit(-1);
      }

      // Now read the user's personal config file (if any)
      configFileName = apollo.util.IOUtil.findFile("~/.apollo/apollo.cfg");
      if (configFileName != null) {
        configFile = new File(configFileName);
        try {
          logger.info("reading personal config file " + configFileName);
          boolean success = readConfig(configFile);
          if (!success)
            logger.warn("failed to read personal config file " + configFileName);
        } catch (Exception e) {
          logger.warn("couldn't open personal config file " + configFileName + ": " + e.getMessage());
        }
      }

      // If locations for history file and autosave file were not specified,
      // set them now.
      setDefaultHistoryAndAutosaveLocs();
    }
  }

  private static void setDataDir (String dir) {
    dataDir = dir;
  }

  private static void setAdapterHistoryFile(String fileName) {
    adapterHistoryFile = fileName;
    // If history file is not found, use the default history file, copying it to fileName
    try {
      File hist = new File(fileName);
      if (!hist.exists()) {
	try {
	  String default_hist;
	  if (apollo.util.IOUtil.isWindows())
	    default_hist = getRootDir() + "\\data\\history.default";
	  else
	    default_hist = getRootDir() + "/data/history.default";

	  logger.info("Couldn't find history file " + fileName + "; copying default history file " + default_hist + " to " + fileName);
	  apollo.util.IOUtil.copyFile(default_hist, fileName);
	} catch (Exception e) {
	  logger.error("Exception trying to copy history.default: " + e);
	}
      }
      logger.info("using history file " + fileName);
    } catch (Exception e) {
      logger.error("Exception while checking for history file " + e);
    }
  }

  /** parses string such as
      Genbank==http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?val=
      into a key and value pair and adds them to a hashmap
  **/
  private static void addStringToHash(String string, Hashtable the_hash) {
    int index = string.indexOf("==");
    if (index > 0 && (index + 2) < string.length()) {
      String label = string.substring(0, index);
      index += 2;
      String val = string.substring(index);
      the_hash.put(label, val);
    }
  }

  // If locations for history and autosave file were not defined in apollo.cfg,
  // put them in ~/.apollo.  If .apollo doesn't exist and can't be created, then
  // try to put them in APOLLO_ROOT/data.
  private static void setDefaultHistoryAndAutosaveLocs() {
    File dotApollo = new File(apollo.util.IOUtil.expandSquiggle("~/.apollo"));
    String whereToPut;
    if ((dotApollo.isDirectory() && dotApollo.canWrite()) ||
	dotApollo.mkdir()) {
      whereToPut = apollo.util.IOUtil.expandSquiggle("~/.apollo/");
    }
    else {
      if (apollo.util.IOUtil.isWindows())
	whereToPut = getRootDir() + "\\data\\";
      else
	whereToPut = getRootDir() + "/data/";
    }

    File dir = new File(whereToPut);
    if (!(dir.isDirectory() && dir.canWrite())) {
      String message = "Can't write to " + dotApollo + " or to " + whereToPut + ".\nYour history and backup file will not be saved.\nAsk your sysadmin to change the permissions.";
      logger.warn(message);
      apollo.util.IOUtil.errorDialog(message);
      setAutosaveInterval(-1);  // Turn off autosaving
      return;
    }

    if (getAdapterHistoryFile() == null) {
      setAdapterHistoryFile(whereToPut + "apollo.history");
    }

    if (getAutosaveFile() == null) {
      autosaveFile = whereToPut + "apollo.backup";
    }
    logger.info("using backup file " + autosaveFile);
  }

  public static void setDBName(String name) {
    dbName = name;
  }
  public static void setDBHost(String host) {
    dbHost = host;
  }
  public static void setGPName(String name) {
    gpName = name;
  }

  private static void setAutosaveInterval(int in) {
    autosaveInterval = in;
  }
  private static void setCGIPort(int in) {
    cgiPort = in;
  }
  private static void setSiteShowLimit(int in) {
    siteShowLimit = in;
  }
  private static void setTextAvoidLimit(int in) {
    textAvoidLimit = in;
  }
  private static void setFastDrawLimit(float in) {
    fastDrawLimit = in;
  }
  private static void setTranslationShowLimit(int in) {
    translationShowLimit = in;
  }
  private static void setSyntenyPanelRelativeSize(int in) {
    syntenyPanelRelativeSize = in;
  }

//   /** sets up TypesObserver (renamed from setPropertyScheme) which is the go between
//    the controller and PropertyScheme - probably dont actually need */
//   private static void addTypesObserverToPropertyScheme(PropertyScheme ft) {
//     if (ft != null)
//       obs = new TypesObserver(ft, controller);
//   }

  public static int getGeneWindow() {
    return geneWindowSize;
  }

  public static String getJVM() {
    return jvm;
  }

  private static String findBrowser(String browserName) {
    try {
      String command = getRootDir() + "/bin/get-real-path ";
      Process proc = Runtime.getRuntime().exec(command + browserName);
      proc.waitFor();
      InputStream out = proc.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(out));
      String line;
      while ((line = reader.readLine()) != null) {
        return line;
      }
    }
    catch (Exception e) {
      logger.error(e.getMessage());
    }
    return "";
  }

  public static void setRefreshPeptides(boolean value) {
    refreshPeptides = value;
    logger.debug("set refreshPeptides to " + value);
  }
  public static boolean getRefreshPeptides() {
    return refreshPeptides;
  }

  /* If thing doesn't exist, pull it out of the jar and create it.
     thing should be a File handle pointing to the path where we want the
     file to be (e.g. to "conf/apollo.cfg"), whereas resource is just the
     name of the file with no path ("apollo.cfg").
     If recreate is true, then create the file whether or not it already exists.
     Note: can throw exceptions. */
  public static void ensureExists(File thing, String resource, boolean recreate) {
    if(!thing.exists() || recreate) {
      try {
        // Not sure why we want to add a / in front of a filename that wasn't a path,
        // e.g. "apollo.cfg" -> "/apollo.cfg", but in fact that seems to be the right
        // thing to do!
        if(resource.indexOf("/") != 0) {
          resource = "/" + resource;
        }
        logger.debug("Creating " + thing + " from " + resource);
        InputStream is = Config.class.getResourceAsStream(resource);

        // Throwing an exception seems a bit aggressive, especially since it's
        // caught and then ignored, but whatever.
        if(is == null) {
          throw new NullPointerException("Can't find resource: " + resource);
        }

        getParentFile(thing).mkdirs();
        OutputStream os = new FileOutputStream(thing);

        for(int next = is.read(); next != -1; next = is.read()) {
          os.write(next);
        }

        os.flush();
        os.close();
      } catch (FileNotFoundException fnfe) {
        throw new Error("Can't create resource: " + fnfe.getMessage());
      }
      catch (IOException ioe) {
        throw new Error("Can't create resource: " + ioe.getMessage());
      }
    }
  }

  /** Default is not to recreate. */
  public static void ensureExists(File thing, String resource) {
    ensureExists(thing, resource, false);  // false means don't recreate file if it already exists
  }

  public static File getParentFile(File thing) {
    String p = thing.getParent();
    if (p == null)
      return null;
    return new File(p);
  }

  public static void reset() {
    initialized = false;
  }

  public static String getTimestamp() {
    String time_path = apollo.util.IOUtil.findFile("data/timestamp");
    if (time_path == null || isJavaWebStartApplication()) {
      File timestampFile = new File(getRootDir() + "/data/timestamp");
      try {
        ensureExists(timestampFile, "timestamp");
      }
      catch (Exception e) {
        logger.warn("Couldn't find or create timestamp");
        return null;
      }
    }
    try {
      File timestampFile = new File(time_path);
      BufferedReader in = new BufferedReader(new FileReader(timestampFile));
      String stamp = in.readLine();
      return stamp;
    } catch (Exception ex) {
      return null;
    }
  }

  public static String getUsersFullName() {
    return getFullNameForUser(UserName.getUserName());
  }
  /** Returns full name of user - if dont have full name just returns user */
  public static String getFullNameForUser(String user) {
    return getStyle().getFullNameForUser(user);
  }

  private static final String gbURL 
    = "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=nucleotide&val=*";

  public static Hashtable getPublicDbList() {
    if (pubDbToURL == null) {
      pubDbToURL= new Hashtable(1);
      pubDbToURL.put("Genbank", gbURL);
    }
    return pubDbToURL;
  }

  public static String getSyntenyDatabaseForSpecies(String species) {
    return getStyle().getSyntenyDatabaseForSpecies(species);
  }

  private static String defaultSpecies = "Species1";
  /** doesnt matter what the string is as long as its consistent across data adapters
      and composite data holder - couldnt think where else to put this but Config */
  public static String getDefaultSingleSpeciesName() {
    return defaultSpecies;
  }

// --> chado-adapter.xml
//   /** style param:
//       for chado jdbc adapter. 2 valid values are "TIGR" and "Flybase" to distinguish
//       between the 2 instantiations of the chado schema - eventually will probably
//       want to break this down further. Default to "TIGR" */
//   public static String getChadoInstance() {
//     return getStyle().getChadoInstance();
//   }

  static boolean getBoolean(String value) {
    return Boolean.valueOf(value).booleanValue(); 
  }

  public static String getChadoTemplateName() {
    return chadoTemplFileName;
  }
  
  private static void setChadoJdbcConfigFile(String filename) {
    chadoJdbcAdapterConfigFile = filename;
    chadoJdbcAdapterConfigFileFullName = null; // invalidate old
    getChadoJdbcAdapterConfigFile(); // actually finds file and sets it
    // hafta reinstall chado adapter with new config file
    // is there another way to do this?
    String chadoClassString = "apollo.dataadapter.chado.ChadoAdapter";
    if (adapterRegistry.isInstalled(chadoClassString)) {
      adapterRegistry.removeDataAdapter(chadoClassString);
      try {
        adapterRegistry.installDataAdapter(chadoClassString);
      } catch (org.bdgp.io.DataAdapterException e) {
        logger.error("could not install driver "+chadoClassString+" because of: "+e);
      }
    }
  }

  public static String getChadoJdbcAdapterConfigFile() {
    if (chadoJdbcAdapterConfigFileFullName == null) {
      chadoJdbcAdapterConfigFileFullName = IOUtil.findFile(chadoJdbcAdapterConfigFile, false);
      // Didn't find it--try to get it from jar
      if (chadoJdbcAdapterConfigFileFullName == null || isJavaWebStartApplication()) {
        logger.debug("getChadoJdbcAdapterConfigFile: couldn't find " + chadoJdbcAdapterConfigFile + " with findFile--trying digForConfigFile");
        chadoJdbcAdapterConfigFileFullName = digForConfigFile(chadoJdbcAdapterConfigFile);
      }
      logger.debug("using chado jdbc adapter config file " +chadoJdbcAdapterConfigFileFullName);
    }
    if (chadoJdbcAdapterConfigFileFullName == null) {
      logger.warn("unable to find chado jdbc adapter config file "+chadoJdbcAdapterConfigFile);
    } 
    return chadoJdbcAdapterConfigFileFullName;
  }

  /**
   * To see if chado transaction output is needed.
   * @return true for output transaction in chado xml format.
   */
  public static boolean isChadoTnOutputNeeded() {
    return outputChadoTransaction;
  }

  /** Whether to write a .tnxml file */
  public static boolean outputTransactionXML() {
    return outputTransactionXML;
  }
  
  /** Indicates adapter to use for command line xml file, "game" or "chado".
      null for no default. */
  public static String getCommandLineXmlFileFormat() {
    return commandLineXmlFileFormat;
  }

  /** Returns true if commandLineXmlFileFormat is nonnull and is "game" or "chado" */
  public static boolean commandLineXmlFileFormatIsConfigged() {
    if (commandLineXmlFileFormat == null)
      return false;
    return commandLineXmlFileFormat.matches("game|chado");
  }

  public static boolean supressMacJvmPopupMessage() {
    init();
    return supressMacJvmPopup;
  }

  public static String getSoOboFilename()
  {
    return soObo;
  }

  public static String getApolloOboFilename()
  {
    return apolloObo;
  }
  
  static boolean checkBoolean(String value, String variableName) {
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return true;
    else {
      complain("Value " + value + " for " + variableName + " is not boolean (should"
               +" be true or false)");
      return false;
    }
  }

  private static void complain(String message) {
    //logger.error("Error on line " + lineNum + " of " + styleFileString + ":\n  " + message);
    // TODO - get line numbers working
    logger.warn("Error in apollo.cfg file:\n  " + message);
  }
}
