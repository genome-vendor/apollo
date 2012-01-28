package apollo.config;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.apache.log4j.*;
import org.bdgp.io.DataAdapterException;
import org.bdgp.io.DataAdapterRegistry;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

import apollo.config.Config;
import apollo.config.TierProperty;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.util.IOUtil;

/** A Style is basically config options for a particular data adapter */

public class Style {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Style.class);

  /** Default for default db if no default db specified */
  private final static String defaultDefaultDatabase = "gadfly";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //added by TAIR
  private boolean queryForNamesOnSplit = false;
  private PropertyScheme properties;  // This is NOT static!  One per style.
  private String tiersFile;
  private DisplayPrefsI displayPrefs;
  private String blixemLocation=null;
  private boolean showAnnotations=false;
  private boolean showResults=true;
  private boolean editingEnabled=false;
  private boolean navigationManagerEnabled=false;
  private Boolean localNavigationManagerEnabled;
  private Vector chromosomes;
  private Vector annotationComments; // comment strings for annotations
  private Vector transcriptComments; // comment strings for transcripts
  private Hashtable userToColor; // = new Hashtable();
  private Hashtable userToProject; // = new Hashtable();
  private Hashtable userToFullName;
  private Hashtable peptideStates = new Hashtable();
  private boolean verticallyMovePopups = true;
  /** URL string that takes a gene name and spits back the region with that gene*/
  private String geneUrl;
  private String scaffoldUrl;
  /** URL string: band -> data for band */
  private String bandUrl;
  private String seqUrl;
  private String rangeUrl;
  private String nameAdapter;
  private String layoutIniFile;
  private String styleFileString;
  // Style files can import other style files, so there can be more than one style file.
  // This array lists all of them.  (Is 2 big enough?)
  private ArrayList styleFiles = new ArrayList(2);

  private Color             coordBackground = Color.white;
  private Color             coordForeground = Color.black;
  private Color             coordRevcompColor = Color.red;
  private Color             featureBackground = Color.black;
  private Color             annotationBackground = Color.cyan;
  private Color             edgematchColor = Color.black;
  private Color             highlightColor = Color.green;
  private Color             selectionColor = Color.red;
  private Color             seqGapColor = Color.yellow;
  private Color             featureLabelColor = Color.white;
  private Color             annotationLabelColor = Color.black;
  private Color             edeBackgroundColor1 = Color.gray;
  private Color             edeBackgroundColor2 = Color.black;
  private Color             edeFeatureColor1 = Color.blue;
  private Color             edeFeatureColor2 = new Color(0,0,180);
  private boolean           dashSets = false;
  private int               edgematchWidth = 2;
  private Color             sequenceColor = coordForeground;
  private Color             taggedColor = new Color(255,0,204); // color for boxing (or hatching) tagged results (not yet user-configurable)
  private Color             seqErrorColor = new Color(255, 153, 0); // bright orange  (not yet user-configurable)
  private int               evidencePanelHeight = 200;

  private boolean           noStripes = true;
  private boolean           draw3D = false;
  private boolean           drawOutline = false;
  // Whether to color GENE transcripts by owner color
  private boolean           userTranscriptColouring = false;
  // Whether to color ALL ANNOTS by owner color (in which case userTranscriptColouring
  // should also be true)
  private boolean           colorAllAnnotsByOwner = false;
  private boolean           fastClipGraphics = true;
  // 7/2005: No longer used--this has been replaced by the parameter
  // InternalMode "true"
  // and the gene report URL is now controlled by the "weburl" parameter in
  // the tiers file, as it is for results.
  //  // Used for deciding whether to show internal-only comments.
  //  // this is used in preference to the external ref URL
  //  private String projectRefUrl = null;
  private boolean           internalMode = false;
  private String externalRefUrl;  // URL for annotation reports
  private Font defaultFont = new Font("Dialog",0,9); // Arial 9 no good on red hat
  private Font defaultFeatureLabelFont = new Font("Dialog",0,9);
  private Font exonDetailEditorSequenceFont = new Font("Courier", Font.BOLD, 12);
  private Color defaultUserColor = Color.orange;
  private int featureLoadSize = 1000000;
  private Hashtable resultTag = new Hashtable();

  private String errors = "";
  private int lineNum = 0;
  private Vector databaseList;
  private String defaultDatabase;
  /** Field in url to replace with database selection */
  private String databaseURLField = "%DATABASE%";
  /** Field in url to replace with padding int */
  private String padLeftURLField = "%PADLEFT%";
  private String padRightURLField = "%PADRIGHT%";
  private int defaultPadding = 25000;
  /** Initial state for sites visibility (view menu item) */
  private boolean initialSitesVisibility = true;

  /** Synteny stuff - put in SyntenyConfig object? */

  /** Number of synteny species - default 1 */
  private int numberOfSpecies = 1;
  /** if true just create species name Species1 to SpeciesN for N species.
      default is false. game uses generic species - gff and ensj specify species */
  private boolean useGenericSyntenySpecies=false;
  /** String of class name for default synteny data adapter to use if none are 
      specified in style file */
  private String defaultSyntenyDataAdapter;
  /** List of "Name.LogicalName" of just species (no links) */
  private java.util.List syntenySpeciesOrder = new java.util.ArrayList();
  private java.util.List syntenyLinkOrder = new java.util.ArrayList();

  /** List of "Name.LogicalName" of all adapters - should parse out Name. */
  private java.util.List syntenySpeciesAndLinkOrder = new java.util.ArrayList();
  /** Maps "Name.LogicalName" to "Full Species Name" Should parse out Name. */
  private HashMap syntenySpeciesNames = new HashMap();
  private HashMap syntenySpeciesProperties = new HashMap();
  private boolean naiveCrossSpeciesDataLoading = false;
  private boolean useOpaqueLinks = true;
  /** Whether to add link query radio buttons to synteny adap gui */
  private boolean addSyntenySpeciesNumDropdown=false;
  /** Whether to add a menu item to the result popup menu that will bring up
      the selected feature as the other species in synteny - delete? */
  private boolean addSyntenyResultMenuItem=false;
  /** Whether synteny should lock zooming with a shift initially */
  private boolean initialShiftForLockedZooming = true;
  /** Whether synteny's initial state is to lock scrolling */
  private boolean initialLockedScrolling = false;
  /** Whether synteny links are gene to gene or not */
  private boolean syntenyLinksAreGeneToGene = true;
  private int singleSpeciesPanelSize = 0;
  private int linkPanelSize = 0;
  /** Maps species strings to database strings */
  private Map syntenySpeciesToDb;
  /** Maps of db string to style file strings - so a file can actually point to other
      styles via a database - perhaps theres a better way to do this? in Config? */
  private Map dbToStyle;

///** for chado jdbc adapter. 2 valid values are "TIGR" and "Flybase" to distinguish
//between the 2 instantiations of the chado schema - eventually will probably
//want to break this down further. Default to "TIGR" */
//private String chadoInstance = "TIGR";

  /** annot info stuff... */
  private boolean showIdField = true;
  private boolean transcriptSymbolEditable = true;
  private boolean showIsDicistronicCheckbox = true;
  private boolean showEvalOfPeptide = true;
  private boolean showIsProbCheckbox = true;
  private boolean showFinishedCheckbox = true;
  private boolean showDbXRefTable = true;
  private boolean showReplaceStopCheckbox = true;
  private boolean translationalFrameShiftEditingEnabled = true;
  private boolean seqErrorEditingIsEnabled = true;
  /** annot menu item */
  private boolean showOwnershipAnnotMenuItem = true;
  private boolean showTranscriptFinishedAnnotMenuItem = true;

  private boolean transactionsAreInGameFile = false;

  /** true to enable http connection to igb */
  private boolean enableIgbHttpConnection = false;

  /** number code for genetic code to use for translation, default is 1 */
  private int geneticCodeNumber = 1;

  /** General way to store config variables.  We could start replacing the 
   *  special-purpose variables and methods with add/getProperty. */
  protected Hashtable parameters = null;
  
  private String overlapDefinition;

  public Style(String styleFileString) {
    logger.debug("Creating new style "+styleFileString);
    setStyleFile(styleFileString);
    File styleFile = new File(styleFileString);
    readStyle(styleFile);
  }
  
  public void setStyleFile(String name) {
    styleFileString = name;
  }

  /** Returns a string of all the style files used to create this file */
  public String getAllStyleFilesString() {
    if (styleFiles.size()==0) return null;
    String files = (String)styleFiles.get(0);
    for (int i=1; i<styleFiles.size(); i++) 
      files += ", "+styleFiles.get(i);
    return files;
  }

  public void readStyle(String styleFilename) {
    if (styleFilename == null) return;
    // Check if we already read this style (maybe it was already imported
    // by another style file we read)
    if (styleFiles.contains(styleFilename)) {
      logger.debug("Already read style file " + styleFilename + " for style " + getFileName());
      return;
    }

    readStyle(new File(styleFilename));
  }

  /** Saves error messages for later checking */
  public void readStyle(File styleFile) {
    logger.debug("Reading style "+styleFile);
    styleFiles.add(styleFile.toString());
    try {
      StreamTokenizer tokenizer =
        new StreamTokenizer(new BufferedReader(new FileReader(styleFile)));
      tokenizer.eolIsSignificant(true);
      tokenizer.slashStarComments(true);
      boolean endOfFile     =  false;
      int     tokType =  0;
      ArrayList  lineWords   =  new ArrayList();

      while (!endOfFile) {
        tokType =  tokenizer.nextToken(); // token iteration

        // End of file
        if (tokType == StreamTokenizer.TT_EOF) {
          endOfFile = true; // end of file - break loop
        }

        // Not the end of line - gather words
        else if (tokType != StreamTokenizer.TT_EOL) {
          if (tokenizer.sval != null) { // grab words til end of line
            lineWords.add(tokenizer.sval);
          }
          ++lineNum;
        }   // hit end of line - do something with parsed words only if 2 of them
        // Hit end of line - process if 2 words
        else if (lineWords.size() == 2) {
          String key   = (String)lineWords.get(0);
          String value = (String)lineWords.get(1);
          if (key.equals("Types")) {
            // 7/2005: Special backwards-compatibility hand-holding:
            // game.tiers was recently renamed fly.tiers, but some users may
            // still have a private game.tiers in their .apollo directory.
            // Ask if they want to rename it to fly.tiers.
            if (getFileName().indexOf("fly") >= 0)
              askRenameGameConfig("game.tiers", "fly.tiers");
            String newTiersFile = apollo.util.IOUtil.findFile(value);
            //when running in webstart mode, always fetch the tiers file from the jar, unless
            //a customized one exists in ~/.apollo
            if (newTiersFile == null ||
                (Config.isJavaWebStartApplication() && !newTiersFile.startsWith(System.getProperty("user.home") + "/.apollo"))) {
              logger.info("Can't find tiers file " + value + "--looking in jar");
              newTiersFile = Config.getRootDir() + "/conf/" + value;
              File tiers = new File(newTiersFile);
              try {
                Config.ensureExists(tiers, value, true);  // true means make new one
              } catch (Exception e) {
                logger.error("Couldn't get " + value + " from jar", e);
                newTiersFile = null;
              }
            }
            if (newTiersFile == null) {
              complain("Types file " + value + " not found!");
              return;
            }
            else {
              // If we have imported a style with a different tiers file, override it
              if (tiersFile == null || !newTiersFile.equals(tiersFile)) {
                setPropertyScheme(newTiersFile);
              }
              tiersFile = newTiersFile;
              logger.debug("Tiers file = " + tiersFile);
            }
          } else if (key.equals("BlixemLocation")) {
            blixemLocation = new String(value);

          } else if (key.equalsIgnoreCase("ShowAnnotations")) {
            if (checkBoolean(value, "ShowAnnotations"))
              showAnnotations = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("ShowResults")) {
            if (checkBoolean(value, "ShowResults"))
              showResults = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("EnableEditing")) {
            if (checkBoolean(value, "EnableEditing"))
              editingEnabled = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("EnableNavigationManager")) {
            if (checkBoolean(value, "EnableNavigationManager"))
              navigationManagerEnabled = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("Chromosomes")) {
            chromosomes = parseStringIntoVector(value);
          } else if (key.equalsIgnoreCase("AnnotationTypes")) {
            logger.warn ("As of November 30, 2003 annotation types " +
                " are set using the tiers file. Ignoring " +
                value + " in style file " + styleFile);
          } else if (key.equalsIgnoreCase("CommentOption")) {
            logger.warn("Note: CommentOption (found in " + styleFile + ") is obsolete--use GeneComment or TranscriptComment.\n  Adding comment '" + value + "' to both lists.");
            addCannedComment(value, "annotation");
            addCannedComment(value, "transcript");
          } else if (key.equalsIgnoreCase("AnnotationComment")) {
            addCannedComment(value, "annotation");
          } else if (key.equalsIgnoreCase("TranscriptComment")) {
            addCannedComment(value, "transcript");
          } else if (key.equalsIgnoreCase("UserInfo")) {
            addUserInfo(value);
          } else if (key.equalsIgnoreCase("PeptideStatus")) {
            addPeptideStatus(value);
          } else if (key.equalsIgnoreCase("FeatureLoadSize")) {
            addFeatureLoadSize(Integer.valueOf(value).intValue());
          } else if (key.equalsIgnoreCase("GeneURL")) {
            geneUrl = value;
          } else if (key.equalsIgnoreCase("BandURL")) {
            bandUrl = value;
          } else if (key.equalsIgnoreCase("ScaffoldURL")) {
            scaffoldUrl = value;
          } else if (key.equalsIgnoreCase("SequenceURL")) {
            seqUrl = value;
          } else if (key.equalsIgnoreCase("LayoutIniFile")) {
            layoutIniFile = value;
          } else if (key.equalsIgnoreCase("RangeURL")) {
            rangeUrl = value;
          } else if (key.equalsIgnoreCase("ExternalRefURL")) {
            externalRefUrl = value;
          } else if (key.equalsIgnoreCase("ProjectRefURL")) {
            logger.warn("ProjectRefURL parameter (found in " + getFileName() + ") is no longer supported.\nUse the InternalMode parameter instead.");
          } else if (key.equalsIgnoreCase("InternalMode")) {
            if (checkBoolean(value, "internalMode"))
              internalMode = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("NameAdapterInstall")) {
            // NameAdapterInstall can be defaulted in the style file.
            // It also can be specified for annotation types in the tiers file.
            // Same for OverlapDefinition.
            installNameAdapter(value);
          } else if (key.equalsIgnoreCase("OverlapDefinition")) {
            setOverlapper(value);
          } else if (key.equalsIgnoreCase("DisplayPreferences")) {
            installDisplayPrefs(value);
          } else if (key.equalsIgnoreCase("GeneDefinition")) {
            //setGeneDefinition(value);
            logger.warn ("GeneDefinition is no longer used");
          } else if (key.equalsIgnoreCase("DashSets")) {
            if (checkBoolean(value, "DashSets"))
              dashSets = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("FeatureBackgroundColor") ||
              key.equalsIgnoreCase("ResultBackgroundColor")) {
            if (checkValidColor(value, "FeatureBackgroundColor"))
              setFeatureBackground(FeatureProperty.toColour(value,
                  featureBackground));
          } else if (key.equalsIgnoreCase("AnnotationBackgroundColor")) {
            if (checkValidColor(value, "AnnotationBackgroundColor"))
              setAnnotationBackground(FeatureProperty.toColour(value,
                  annotationBackground));
          } else if (key.equalsIgnoreCase("EdgematchWidth")) {
            try {
              edgematchWidth = (Integer.valueOf(value).intValue());
            } catch (NumberFormatException e) {
              complain("Can't parse number for EdgematchWidth: "+value);
            }
          } else if (key.equalsIgnoreCase("EdgematchColor")) {
            if (checkValidColor(value, "EdgematchColor"))
              edgematchColor = FeatureProperty.toColour(value,
                  edgematchColor);
          } else if (key.equalsIgnoreCase("AnnotationLabelColor")) {
            if (checkValidColor(value, "AnnotationLabelColor"))
              setAnnotationLabelColor(FeatureProperty.toColour(value,
                  annotationLabelColor));
          } else if (key.equalsIgnoreCase("LabelColor") || key.equalsIgnoreCase("featureLabelColor")) {
            if (checkValidColor(value, "FeatureLabelColor"))
              setFeatureLabelColor(FeatureProperty.toColour(value,
                  featureLabelColor));
          } else if (key.equalsIgnoreCase("DefaultAnnotationColor")) {
            if (checkValidColor(value, "DefaultAnnotationColor"))
              // Is this a good idea?
              Config.defaultAnnotationColor = FeatureProperty.toColour(value,
                  Config.defaultAnnotationColor);
          } else if (key.equalsIgnoreCase("HighlightColor")) {
            if (checkValidColor(value, "HighlightColor"))
              highlightColor = FeatureProperty.toColour(value,
                  highlightColor);
          } else if (key.equalsIgnoreCase("SelectionColor")) {
            if (checkValidColor(value, "SelectionColor"))
              selectionColor = FeatureProperty.toColour(value,
                  selectionColor);
          } else if (key.equalsIgnoreCase("CoordBackgroundColor")) {
            if (checkValidColor(value, "CoordBackgroundColor"))
              coordBackground = FeatureProperty.toColour(value,
                  coordBackground);
          } else if (key.equalsIgnoreCase("SequenceColor")) {
            if (checkValidColor(value, "SequenceColor"))
              sequenceColor = FeatureProperty.toColour(value,
                  sequenceColor);
          } else if (key.equalsIgnoreCase("ExonDetailEditorBackgroundColor1")) {
            if (checkValidColor(value, "ExonDetailEditorBackgroundColor1"))
              edeBackgroundColor1 = FeatureProperty.toColour(value,edeBackgroundColor1);
          } else if (key.equalsIgnoreCase("ExonDetailEditorBackgroundColor2")) {
            if (checkValidColor(value, "ExonDetailEditorBackgroundColor2"))
              edeBackgroundColor2 = FeatureProperty.toColour(value,edeBackgroundColor2);
          } else if (key.equalsIgnoreCase("ExonDetailEditorFeatureColor1")) {
            if (checkValidColor(value, "ExonDetailEditorFeatureColor1"))
              edeFeatureColor1 = FeatureProperty.toColour(value,edeFeatureColor1);
          } else if (key.equalsIgnoreCase("ExonDetailEditorFeatureColor2")) {
            if (checkValidColor(value, "ExonDetailEditorFeatureColor2"))
              edeFeatureColor2 = FeatureProperty.toColour(value,edeFeatureColor2);
          } else if (key.equalsIgnoreCase("DashSets")) {
            if (checkBoolean(value, "DashSets"))
              dashSets = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("FastClipGraphics")) {
            if (checkBoolean(value, "FastClipGraphics"))
              fastClipGraphics = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("UserTranscriptColouring") ||
              key.equalsIgnoreCase("UserTranscriptColoring")) {
            if (checkBoolean(value, "UserTranscriptColoring"))
              userTranscriptColouring = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("colorAllAnnotsByOwner")) {
            if (checkBoolean(value, "colorAllAnnotsByOwner")) {
              colorAllAnnotsByOwner = (new Boolean(value)).booleanValue();
              userTranscriptColouring = true;  // in case it wasn't already
            }
          } else if (key.equalsIgnoreCase("Draw3D")) {
            if (checkBoolean(value, "Draw3D"))
              draw3D = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("DrawOutline")) {
            if (checkBoolean(value, "drawOutline"))
              drawOutline = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("NoStripes")) {
            if (checkBoolean(value, "NoStripes"))
              noStripes = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("CoordForegroundColor")) {
            if (checkValidColor(value, "CoordForegroundColor"))
              coordForeground = FeatureProperty.toColour(value,
                  coordForeground);
          } else if (key.equalsIgnoreCase("SequenceGapColor")) {
            if (checkValidColor(value, "SequenceGapColor"))
              seqGapColor = FeatureProperty.toColour(value,
                  seqGapColor);

          } else if (key.equalsIgnoreCase("VerticallyMovePopups")) {
            if (checkBoolean(value, "VerticallyMovePopups"))
              verticallyMovePopups = (new Boolean(value)).booleanValue();
          } else if (key.equalsIgnoreCase("EvidencePanelHeight")) {
            try {
              evidencePanelHeight = (Integer.valueOf(value).intValue());
            } catch (NumberFormatException e) {
              complain("Can't parse number for EvidencePanelHeight: "+value);
            }
          } else if (key.startsWith("Name")) {
            //Synteny species logical-and-species-names.
            // Maps "Name.LogicalName" to "Full Species Name"
            // Shouldnt we just parse the Name. out here? not needed anymore right?
            getSyntenySpeciesNames().put(key, value);
            // List of "Name.LogicalName" - again should parse out Name.
            getSyntenySpeciesAndLinkOrder().add(key);
            // Change to or add a  more general term - compara is ensj specific
            //if(!value.equals("Compara")){
            if (!stringIsTagForSyntenyLink(value)) {
              // List of "Name.LogicalName" - again should parse out Name.
              getSyntenySpeciesOrder().add(key);
            } else {
              getSyntenyLinkOrder().add(key);
            }
          } else if (key.equalsIgnoreCase("ResultTag")) { // subroutinize!
            // e.g. ResultTag "Fly EST, DGC cDNA:Problematic: not full ORF"
            int breakIndex = value.indexOf(":");
            if (breakIndex < 0) {
              logger.warn("couldn't find : separator in ResultTag line " + value + ".  Ignoring line.");
              continue;
            }
            // Handle multiple result types before :
            String result_types = value.substring(0,breakIndex).trim();
            Vector result_type_vector = parseStringIntoVector(result_types);
            String tag = value.substring(breakIndex+1);
            for (int i = 0; i < result_type_vector.size(); i++) {
              String result_type = (String) result_type_vector.elementAt(i);
              Vector tag_list = (Vector) resultTag.get (result_type);
              if (tag_list == null) {
                tag_list = new Vector();
                resultTag.put(result_type, tag_list);
              }
              // !! Really should check first whether we already have this tag
              tag_list.addElement (tag);
            }
          } else if (key.equals("SpeciesToDatabase")) {
            addSyntenySpeciesToDatabase(value);
          } else if (key.equals("DatabaseToStyle")) {
            addDatabaseToStyle(value);
          } else if (key.startsWith("Species")) {
            //Synteny species property.
            getSyntenySpeciesProperties().put(key, value);
          } else if (key.equalsIgnoreCase("UseGenericSyntenySpecies")) {
            if (checkBoolean(value,key)) 
              useGenericSyntenySpecies = getBoolean(value);
          } else if (key.equalsIgnoreCase("DefaultSyntenyDataAdapter")) {
            defaultSyntenyDataAdapter = value;
          } else if (key.equalsIgnoreCase("NumberOfSpecies")) {
            if (checkInteger(value,key)) {
              int num = getInteger(value);
              if (num < 1) // < 1 is meaningless
                num = 1;
              numberOfSpecies = num;
            }
          } else if (key.equalsIgnoreCase("NaiveCrossSpeciesDataLoading")) {
            naiveCrossSpeciesDataLoading = Boolean.valueOf(value).booleanValue();
          } else if (key.equalsIgnoreCase("useOpaqueLinks")) {
            useOpaqueLinks = Boolean.valueOf(value).booleanValue();
          } else if (key.equalsIgnoreCase("SingleSpeciesPanelSize")) {
            singleSpeciesPanelSize = Integer.valueOf(value).intValue(); 
          } else if (key.equalsIgnoreCase("LinkPanelSize")) {
            linkPanelSize = Integer.valueOf(value).intValue(); 
          } else if (key.equalsIgnoreCase("DefaultFont")) {
            defaultFont = parseFont(value);
          } else if (key.equalsIgnoreCase("DefaultFeatureLabelFont")) {
            defaultFeatureLabelFont = parseFont(value);
          } else if (key.equalsIgnoreCase("ExonDetailEditorSequenceFont")) {
            exonDetailEditorSequenceFont = parseFont(value);
          } else if (key.equals("DatabaseList")) {
            databaseList = parseStringIntoVector(value);
          } else if (key.equals("DefaultDatabase")) {
            defaultDatabase = value;
          } else if (key.equals("DatabaseURLField")) {
            databaseURLField = value;
          } else if (key.equals("PadLeftURLField")) {
            padLeftURLField = value;
          } else if (key.equals("PadRightURLField")) {
            padRightURLField = value;
          } else if (key.equals("DefaultPadding")) {
            if (checkInteger(value,key)) defaultPadding = getInteger(value);
          } else if (key.equals("AddSyntenySpeciesNumberDropdown")) {
            if (checkBoolean(value,key)) 
              addSyntenySpeciesNumDropdown = getBoolean(value);
          } else if (key.equals("AddSyntenyResultMenuItem")) {
            if (checkBoolean(value,key)) addSyntenyResultMenuItem = getBoolean(value);
          } else if (key.equals("SyntenyLinksAreGeneToGene")) {
            if (checkBoolean(value,key)) {
              syntenyLinksAreGeneToGene = getBoolean(value);
            }
          } else if (key.equals("InitialStateForStartStopCodonVisibility")) {
            if (checkBoolean(value,key)) initialSitesVisibility = getBoolean(value);
          } else if (key.equals("InitialShiftForLockedZooming")) {
            if (checkBoolean(value,key)) 
              initialShiftForLockedZooming = getBoolean(value);
          } else if (key.equals("InitialLockedScrolling")) {
            if (checkBoolean(value,key)) initialLockedScrolling = getBoolean(value);
            //          } else if (key.equalsIgnoreCase("ChadoInstance")) {
            //            chadoInstance = value;
          } else if (key.equalsIgnoreCase("ShowIdField")) {
            if (checkBoolean(value,key))
              showIdField = getBoolean(value);
          } else if (key.equalsIgnoreCase("TranscriptSymbolEditable")) {
            if (checkBoolean(value,key))
              transcriptSymbolEditable = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowIsDicistronicAnnotInfoCheckbox")) {
            if (checkBoolean(value,key))
              showIsDicistronicCheckbox = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowEvaluationOfPeptide")) {
            if (checkBoolean(value,key))
              showEvalOfPeptide = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowIsProblematicAnnotInfoCheckbox")) {
            if (checkBoolean(value,key))
              showIsProbCheckbox = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowFinishedAnnotInfoCheckbox")) {
            if (checkBoolean(value,key))
              showFinishedCheckbox = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowDBCrossRefAnnotInfoTable")) {
            if (checkBoolean(value,key))
              showDbXRefTable = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowReplaceStopAnnotInfoCheckbox")) {
            if (checkBoolean(value,key))
              showReplaceStopCheckbox = getBoolean(value);
          } else if (key.equalsIgnoreCase("EnableTranslationalFrameShiftEditing")) {
            if (checkBoolean(value,key))
              translationalFrameShiftEditingEnabled = getBoolean(value);
          } else if (key.equalsIgnoreCase("EnableSequenceErrorEditing")) {
            if (checkBoolean(value,key))
              seqErrorEditingIsEnabled = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowOwnershipAnnotMenuItem")) {
            if (checkBoolean(value,key))
              showOwnershipAnnotMenuItem = getBoolean(value);
          } else if (key.equalsIgnoreCase("ShowTranscriptFinishedAnnotMenuItem")) {
            if (checkBoolean(value,key))
              showTranscriptFinishedAnnotMenuItem = getBoolean(value);
          } else if (key.equalsIgnoreCase("EnableIgbHttpConnection")) {
            if (checkBoolean(value,key))
              enableIgbHttpConnection = getBoolean(value);
          } else if (key.equalsIgnoreCase("TransactionXmlInGameFile")) {
            if (checkBoolean(value,key))
              transactionsAreInGameFile = getBoolean(value);
          } else if (key.equals("ImportStyle")) {
            importStyle(value);
          } else if (key.equalsIgnoreCase("QueryForNamesOnSplit")) {
            this.queryForNamesOnSplit=(value.equalsIgnoreCase("true")?true:false);
          } else if (key.equalsIgnoreCase("GeneticCode")) {
            if (checkInteger(value,key)) {
              int num = getInteger(value);
              if (num != 1 && num != 6) {
                logger.warn("GeneticCode style config only takes 1 & 6, setting to 1");
                num = 1;
              }
              geneticCodeNumber = num;
            }
          } else {
            // Stick it in as a generic parameter
            logger.debug("saved unknown parameter from " + styleFile + ": " + key + "=" + value);
            addParameter(key, value);
          }
          lineWords.clear(); // empty for next line
        } // end of 2 words - else if (lineWords.size() == 2) 

        // <link> is special case - its xml
        else if (lineWords.size() != 0
            && ((String)lineWords.get(0)).equals("link")) {
          logger.debug("got link "+lineWords.get(0));
          lineWords.clear();
        }

        // If there are some words but not two words (handled above), print error
        else if (lineWords.size() != 0) {
          String error = "Line " + lineNum + " in " + styleFile + " has only 1 word, not two words separated by spaces: " + lineWords.get(0);
          if (lineWords.size() >= 3) 
            error = "Line " + lineNum + " in " + styleFile + " has " + lineWords.size() + "words, not two words separated by spaces: " + lineWords.get(0) + "," + lineWords.get(1) + "," + lineWords.get(2) + " (etc.)";
          complain(error);
          lineWords.clear(); // empty for next line
        }
      }
    } catch (FileNotFoundException e) {
      apollo.util.IOUtil.errorDialog("Style file "+styleFile+" not found");
    }
    catch (Exception e) {
      String error = "Error parsing style file "+styleFile+": " + e;
      apollo.util.IOUtil.errorDialog(error);
    }
  }
  
  /** Writes the style data to a file.
   * 
   * @param styleFilename - String for the name of the style file to write to
   */
  public void writeStyle(String styleFilename)
  {
    writeStyle(new File(styleFilename));
  }
  
  /** Writes the style data to a file.
   * 
   * @param styleFile - File for the style file to write to
   * @return true if successful
   */
  public boolean writeStyle(File styleFile)
  {
    PrintWriter out = null;
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(styleFile)));
    }
    catch (IOException e) {
      logger.error("Unable to write style to " + styleFile.getAbsolutePath());
      return false;
    }
    writePair(out, "Types", new File(getTiersFile()).getName());
    writePair(out, "BlixemLocation", getBlixemLocation());
    writePair(out, "ShowAnnotations", getShowAnnotations());
    writePair(out, "ShowResults", getShowResults());
    writePair(out, "EnableEditing", isEditingEnabled());
    writePair(out, "EnableNavigationManager", isNavigationManagerEnabled());
    writeListAsSingleEntry(out, "Chromosomes", getChromosomes());
    if (getAnnotationComments() != null) {
      writeListAsMultipleEntries(out, "AnnotationComment", null, getAnnotationComments().subList(1, getAnnotationComments().size()));
    }
    if (getTranscriptComments() != null) {
      writeListAsMultipleEntries(out, "TranscriptComment", null, getTranscriptComments().subList(1, getTranscriptComments().size()));
    }
    writeUserInfo(out);
    writePeptideStatus(out);
    writePair(out, "FeatureLoadSize", getFeatureLoadSize());
    writePair(out, "GeneURL", getGeneUrl());
    writePair(out, "BandURL", getBandUrl());
    writePair(out, "ScaffoldURL", getScaffoldUrl());
    writePair(out, "SequenceURL", getSeqUrl());
    writePair(out, "LayoutIniFile", getLayoutIniFile());
    writePair(out, "RangeURL", getRangeUrl());
    writePair(out, "ExternalRefURL", getExternalRefUrl());
    writePair(out, "InternalMode", internalMode());
    writePair(out, "NameAdapterInstall", getNameAdapter());
    writePair(out, "DisplayPreferences", getDisplayPrefs().getClass().getName());
    writePair(out, "DashSets", getDashSets());
    writePair(out, "FeatureBackgroundColor", getFeatureBackground());
    writePair(out, "AnnotationBackgroundColor", getAnnotationBackground());
    writePair(out, "EdgematchWidth", getEdgematchWidth());
    writePair(out, "EdgematchColor", getEdgematchColor());
    writePair(out, "AnnotationLabelColor", getAnnotationLabelColor());
    writePair(out, "FeatureLabelColor", getFeatureLabelColor());
    writePair(out, "HighlightColor", getHighlightColor());
    writePair(out, "SelectionColor", getSelectionColor());
    writePair(out, "CoordBackgroundColor", getCoordBackground());
    writePair(out, "SequenceColor", getSequenceColor());
    writePair(out, "ExonDetailEditorBackgroundColor1", getExonDetailEditorBackgroundColor1());
    writePair(out, "ExonDetailEditorBackgroundColor2", getExonDetailEditorBackgroundColor2());
    writePair(out, "ExonDetailEditorFeatureColor1", getExonDetailEditorFeatureColor1());
    writePair(out, "ExonDetailEditorFeatureColor2", getExonDetailEditorFeatureColor2());
    writePair(out, "FastClipGraphics", fastClipGraphics);
    writePair(out, "UserTranscriptColoring", doUserTranscriptColouring());
    writePair(out, "ColorAllAnnotsByOwner", colorAllAnnotsByOwner());
    writePair(out, "Draw3D", getDraw3D());
    writePair(out, "DrawOutline", getDrawOutline());
    writePair(out, "NoStripes", getNoStripes());
    writePair(out, "CoordForegroundColor", getCoordForeground());
    writePair(out, "SequenceGapColor", getSeqGapColor());
    writePair(out, "VerticallyMovePopups", verticallyMovePopups());
    writePair(out, "EvidencePanelHeight", getEvidencePanelHeight());
    writeResultTags(out);
    writePair(out, "UseGenericSyntenySpecies", useGenericSyntenySpecies());
    writePair(out, "DefaultSyntenyDataAdapter", getDefaultSyntenyDataAdapterString());
    writePair(out, "NumberOfSpecies", getNumberOfSpecies());
    writePair(out, "NaiveCrossSpeciesDataLoading", getNaiveCrossSpeciesDataLoading());
    writePair(out, "UseOpaqueLinks", useOpaqueLinks());
    writePair(out, "SingleSpeciesPanelSize", getSingleSpeciesPanelSize());
    writePair(out, "LinkPanelSize", getLinkPanelSize());
    writePair(out, "DefaultFont", getDefaultFont());
    writePair(out, "DefaultFeatureLabelFont", getDefaultFeatureLabelFont());
    writePair(out, "ExonDetailEditorSequenceFont", getExonDetailEditorSequenceFont());
    writePair(out, "DefaultDatabase", getDefaultDatabase());
    writePair(out, "DatabaseURLField", getDatabaseURLField());
    writePair(out, "PadLeftURLField", getPadLeftURLField());
    writePair(out, "PadRightURLField", getPadRightURLField());
    writePair(out, "DefaultPadding", getDefaultPadding());
    writePair(out, "AddSyntenySpeciesNumberDropdown", addSyntenySpeciesNumDropdown());
    writePair(out, "AddSyntenyResultMenuItem", addSyntenyResultMenuItem());
    writePair(out, "SyntenyLinksAreGeneToGene", syntenyLinksAreGeneToGene());
    writePair(out, "InitialStateForStartStopCodonVisibility", getInitialSitesVisibility());
    writePair(out, "InitialShiftForLockedZooming", initialShiftForLockedZooming());
    writePair(out, "InitialLockedScrolling", initialLockedScrolling());
    writePair(out, "ShowIdField", showIdField());
    writePair(out, "TranscriptSymbolEditable", getTranscriptSymbolEditable());
    writePair(out, "ShowIsDicistronicAnnotInfoCheckBox", showIsDicistronicCheckbox());
    writePair(out, "ShowEvaluationOfPeptide", showEvalOfPeptide());
    writePair(out, "ShowIsProblematicAnnotInfoCheckbox", showIsProbCheckbox());
    writePair(out, "ShowFinishedAnnotInfoCheckbox", showFinishedCheckbox());
    writePair(out, "ShowDBCrossRefAnnotInfoTable", showDbXRefTable());
    writePair(out, "ShowReplaceStopAnnotInfoCheckbox", showReplaceStopCheckbox());
    writePair(out, "EnableTranslationalFrameShiftEditing", translationalFrameShiftEditingIsEnabled());
    writePair(out, "EnableSequenceErrorEditing", seqErrorEditingIsEnabled());
    writePair(out, "ShowOwnershipAnnotMenuItem", showOwnershipAnnotMenuItem());
    writePair(out, "ShowTranscriptFinishedAnnotMenuItem", showTranscriptFinishedAnnotMenuItem());
    writePair(out, "EnableIgbHttpConnection", enableIgbHttpConnection);
    writePair(out, "TransactionXmlInGameFile", transactionsAreInGameFile());
    writePair(out, "QueryForNamesOnSplit", queryForNamesOnSplit());
    writePair(out, "GeneticCode", getGeneticCodeNumber());
    writePair(out, "OverlapDefinition", getOverlapDefinition());
    writePair(out, "DefaultAnnotationColor", Config.defaultAnnotationColor);
    writeMapping(out, getSyntenySpeciesNames());
    writeMapping(out, getSyntenySpeciesProperties());
    writeArrowMapping(out, "SpeciesToDatabase", getSpeciesToDbMap());
    writeArrowMapping(out, "DatabaseToStyle", getDbToStyleMap());
    writeListAsSingleEntry(out, "DatabaseList", getDatabaseList());
    writePair(out, "SequenceOntologyOBO", getParameter("SequenceOntologyOBO"));
    writePair(out, "ApolloOBO", getParameter("ApolloOBO"));
    
    // TODO: handle ImportStyle
    
    if (out != null) {
      out.close();
    }
    return true;
  }

  /** Writes a collection of objects as a single entry.  Each
   *  object has its toString() method called and is separated by a ','.
   *  
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param values - Collection of objects to have the values stored
   */
  private void writeListAsSingleEntry(PrintWriter out, String key, Collection values)
  {
    if (values == null) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    for (Object o : values) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(o.toString());
    }
    if (sb.length() > 0) {
      writePair(out, key, sb.toString());
    }
  }

  /** Writes a collection of objects as multiple entries.  Each
   *  object has its toString() method called and is outputted one per line.
   *  
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param preValue - String for prepending each value.  Can be null
   * @param values - Collection of object to have the values stored
   */
  private void writeListAsMultipleEntries(PrintWriter out, String key, String preValue, Collection values)
  {
    if (values == null) {
      return;
    }
    for (Object o : values) {
      String value;
      if (preValue != null) {
        value = preValue + ":" + o.toString();
      }
      else {
        value = o.toString();
      }
      writePair(out, key, value);
    }
  }
  
  /** Writes a key/value pair in the Style format.
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param value - boolean to write the value of
   */
  private void writePair(PrintWriter out, String key, boolean value)
  {
    writePair(out, key, Boolean.toString(value));
  }

  /** Writes a key/value pair in the Style format.
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param value - int to write the value of
   */
  private void writePair(PrintWriter out, String key, int value)
  {
    writePair(out, key, Integer.toString(value));
  }

  /** Writes a key/value pair in the Style format.
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param color - Color to write the value of
   */
  private void writePair(PrintWriter out, String key, Color color)
  {
    writePair(out, key, convertColorToRgbString(color));
  }

  /** Writes a key/value pair in the Style format.
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param font - Font to write the value of
   */
  private void writePair(PrintWriter out, String key, Font font)
  {
    writePair(out, key, convertFontToString(font));
  }
  
  /** Writes a key/value pair in the Style format.
   *  If either the key or value are null or empty, nothing is written.
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param value - String to write the value of
   */
  private void writePair(PrintWriter out, String key, String value)
  {
    if (key == null || key.length() == 0 ||
        value == null || value.length() == 0) {
      return;
    }
    out.printf("%s\t\"%s\"\n", key, value);
  }

  /** Converts a Color object into its RGB
   *  representation, where each component is separated by a ','.
   *  
   * @param c - Color to get the RGB information from
   * @return String representing the RGB elements
   */
  private String convertColorToRgbString(Color c)
  {
    if (c == null) {
      return null;
    }
    return String.format("%d,%d,%d", c.getRed(), c.getGreen(), c.getBlue());
  }

  /** Converts a Font to the a string given the Style
   *  format of 'name,style,size'.
   *  
   * @param f - Font to get the string representation
   * @return String representing the Font
   */
  private String convertFontToString(Font f)
  {
    if (f == null) {
      return null;
    }
    return String.format("%s,%d,%d", f.getName(), f.getStyle(), f.getSize());
  }
  
  /** Iterates through the user info maps (project, color, fullname) and writes
   *  it.
   *  
   * @param out - PrintWriter to write the data to
   */
  private void writeUserInfo(PrintWriter out)
  {
    if (userToProject == null) {
      return;
    }
    for (Object o : userToProject.entrySet()) {
      String user = (String)((Map.Entry)o).getKey();
      String project = (String)((Map.Entry)o).getValue();
      Color color = getAnnotationColor(user);
      String fullName = (String)getFullNameForUser(user);
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("user=%s:project=%s:color=%s",
          user, project, convertColorToRgbString(color)));
      if (fullName != null) {
        sb.append(String.format(":full-name=%s", fullName));
      }
      writePair(out, "UserInfo", sb.toString());
    }
  }
  
  /** Iterates through the each PeptideStatus and writes it.
   *  
   * @param out - PrintWriter to write the data to
   */
  private void writePeptideStatus(PrintWriter out)
  {
    for (Object o : getPeptideStates().values()) {
      PeptideStatus ps = (PeptideStatus)o;
      Font f = ps.getFont();
      writePair(out, "PeptideStatus",
          String.format("%d:%s:%s:%s", ps.getPrecedence(),
              convertFontToString(f),
              Boolean.toString(ps.getCurated()), ps.getText()));
    }
  }

  /** Iterates through the each result tag and writes it.
   *  
   * @param out - PrintWriter to write the data to
   */
  private void writeResultTags(PrintWriter out)
  {
    for (Object o : getResultTags().entrySet()) {
      String resultType = (String)((Map.Entry)o).getKey();
      writeListAsMultipleEntries(out, "ResultTag", resultType, (Vector)((Map.Entry)o).getValue());
    }
  }
  
  /** Iterates through the each mapping and writes it as follows:
   *  "Map.Entry.key" "Map.Entry.value"
   *  
   * @param out - PrintWriter to write the data to
   */
  private void writeMapping(PrintWriter out, Map map)
  {
    for (Object o : map.entrySet()) {
      Map.Entry entry = (Map.Entry)o;
      writePair(out, "\"" + entry.getKey().toString() + "\"", entry.getValue().toString());
    }
  }

  /** Iterates through the each mapping and writes it delimited with a '->' as
   *  follows: key "Map.Entry.key -> Map.Entry.value"
   * 
   * @param out - PrintWriter to write the data to
   * @param key - String for the key in the Style key/value pair format
   * @param map - Map containing the mappings for the values
   */
  private void writeArrowMapping(PrintWriter out, String key, Map map)
  {
    for (Object o : map.entrySet()) {
      Map.Entry entry = (Map.Entry)o;
      String value = entry.getKey().toString() + " -> " + entry.getValue().toString();
      writePair(out, key, value);
    }
  }
  
  /** Print message to stderr */
  public void complain(String message) {
    logger.error("Error on line " + lineNum + " of " + styleFileString + ":\n  " + message);
    errors = errors + "\n" + "Error on line " + lineNum + " of " + styleFileString + ":\n   " + message;
  }

  public String getErrors() {
    return errors;
  }

  /** added BY TAIR
   * someday we may need to change this to something more fitting than a
   * boolean, but for now it is binary */
  public boolean queryForNamesOnSplit() {
    return this.queryForNamesOnSplit;
  }

  /** 7/2005: Special backwards-compatibility hand-holding:
   * game.style and game.tiers were recently renamed fly.style (which
   * imports game.style) and fly.tiers, but some users may still have
   * a private game.style or game.tiers in their .apollo directory.
   * Ask if they want to rename it to the new name. */
  public static void askRenameGameConfig(String oldname, String newname) {
    String homeDotApolloDir = System.getProperty("user.home") + "/.apollo/";
    String oldfile = apollo.util.IOUtil.findFile(homeDotApolloDir + oldname);
    if (oldfile != null) {
      String newfile = apollo.util.IOUtil.findFile(homeDotApolloDir + newname);
      if (newfile != null) {
        String msg = "You have both a " + oldname + " and a " + newname + "\nin your .apollo directory.  You really should only\nhave a " + newname + ".  You might want to fix this.";
        apollo.util.IOUtil.errorDialog(msg);
        return;
      }
      String msg = "You have a " + oldname + " in your .apollo directory.\nApollo now looks for " + newname + " instead.\nDo you want to rename your .apollo/" + oldname + " to .apollo/" + newname + "?";
      boolean answer = apollo.main.LoadUtil.areYouSure(msg);
      if (answer == true) {
        File oldF = new File(oldfile);
        newfile = apollo.util.IOUtil.findFile(homeDotApolloDir + newname, true);
        if (newfile == null) {
          msg = "Failed to rename " + oldfile + " to " + newname;
          apollo.util.IOUtil.errorDialog(msg);
          return;
        }
        File newF = new File(newfile);
        if (oldF.renameTo(newF))
          logger.info("Renamed " + oldfile + " to " + newfile);
        else {
          msg = "Failed to rename " + oldfile + " to " + newfile;
          apollo.util.IOUtil.errorDialog(msg);
        }
      }
    }
  }

  public boolean checkValidColor(String colorStr, String variableName) {
    Color color;
    try {
      color = FeatureProperty.parseColour(colorStr);
    } catch (NumberFormatException e) {
      complain("Bad color " + colorStr + " for " + variableName);
      return false;
    }
    return true;
  }

  public String getBlixemLocation() {
    if (blixemLocation == null)
      return null;

    // FOR NOW just decide whether to use the linux or solaris version
    String osname = System.getProperty("os.name");
    // Are there other Linux osnames not caught by this pattern?
    if (osname.startsWith("Linux") || osname.startsWith("linux"))
      blixemLocation = "bin/blixem.LINUX";
    else if (osname.startsWith("Solaris") ||
        osname.startsWith("Sun"))
      blixemLocation = "bin/blixem.SOLARIS";

    String absolute_path = apollo.util.IOUtil.findFile(blixemLocation);
    return absolute_path;
  }


  private boolean checkInteger(String value, String varName) {
    try { Integer.valueOf(value); } 
    catch (NumberFormatException e) {
      complain("Can't parse number for "+varName+": "+value);
      return false;
    }
    return true; // no exception
  }
  private int getInteger(String value) { 
    return Integer.valueOf(value).intValue();
  }

  private boolean getBoolean(String value) {
    //return Boolean.valueOf(value).booleanValue(); 
    return Config.getBoolean(value);
  }

  public boolean checkBoolean(String value, String variableName) {
    return Config.checkBoolean(value,variableName);
  }

  public String getTiersFile() {
    return tiersFile;
  }

  public void setTiersFile(String tiersFile) {
    this.tiersFile = tiersFile;
  }

  public String getLayoutIniFile() {
    return layoutIniFile;
  }

  public boolean getShowAnnotations() {
    return showAnnotations;
  }

  public void setShowAnnotations(boolean showAnnotations) {
    this.showAnnotations = showAnnotations;
  }

  public boolean getShowResults() {
    return showResults;
  }

  public void setShowResults(boolean showResults) {
    this.showResults = showResults;
  }

  public int getEdgematchWidth() {
    return edgematchWidth;
  }

  public boolean doUserTranscriptColouring() {
    return userTranscriptColouring;
  }

  public boolean colorAllAnnotsByOwner() {
    return colorAllAnnotsByOwner;
  }

  public boolean getDraw3D() {
    return draw3D;
  }
  public void setDraw3D(boolean state) {
    draw3D = state;
  }
  public boolean getNoStripes() {
    return noStripes;
  }

  public int getEvidencePanelHeight() {
    return evidencePanelHeight;
  }

  boolean verticallyMovePopups() {
    return verticallyMovePopups;
  }

  public boolean isEditingEnabled() {
    return editingEnabled;
  }

  public void setEditingEnabled(boolean editingEnabled) {
    this.editingEnabled = editingEnabled;
  }

  public void setLocalNavigationManagerEnabled(Boolean enable) {
    localNavigationManagerEnabled = enable;
  }

  boolean isNavigationManagerEnabled() {
    if (localNavigationManagerEnabled != null) {
      return localNavigationManagerEnabled.booleanValue();
    }
    return navigationManagerEnabled;
  }

  boolean hasChromosomes() {
    return chromosomes!=null;
  }
  /** Returns a vector of chromosome names as strings, null if none given */
  public Vector getChromosomes() {
    return chromosomes;
  }

  public boolean getDrawOutline() {
    return drawOutline;
  }

  public void setDrawOutline(boolean state) {
    drawOutline = state;
  }

  /** parses string (e.g. "2L,2R,3L,3R,4,X") into vector */
  private Vector parseStringIntoVector(String string) {
    Vector v = new Vector();
    StringTokenizer tokenizer = new StringTokenizer(string,",");
    while (tokenizer.hasMoreTokens())
      v.addElement(tokenizer.nextToken().trim());
    return v;
  }

  /** If no DatabaseList set in style, defaults to a list of just the 
      DatabaseDefault (which defaults to gadfly if not set) */
  public Vector getDatabaseList() { 
    if (databaseList == null) {
      databaseList = new Vector(1);
      databaseList.add(getDefaultDatabase());
    }
    return databaseList;
  }


  /** Get list of databases with changes possible based on queryType.
   *  FOR NOW, database list for sequence queries doesn't include database r3.2 */
  public Vector getDatabaseList(String queryType) { 
    if (queryType.equalsIgnoreCase("sequence")) {
      // Gotta clone the list--don't want to change it globally for this style
      Vector seqDbList = (Vector)getDatabaseList().clone();
      for (int i = 0; i < seqDbList.size(); i++) {
        String db = (String)seqDbList.elementAt(i);
        if (db.indexOf("r3.2") >= 0) {
          seqDbList.setElementAt("(Not available for release 3.2)", i);
        }
      }
      return seqDbList;
    }
    return databaseList;
  }

  /** Used for navigation bar db (for game), also if no database list is
   *  provided, getDatabaseList defaults to a list of default db.
   *  defaultDatabase defaults to "gadfly" if not provided. */
  public String getDefaultDatabase() { 
    if (defaultDatabase == null) {
      if (databaseList!=null && databaseList.size() > 0) {
        defaultDatabase = (String)databaseList.get(0);
      }
      else {
        defaultDatabase = defaultDefaultDatabase;
      }
    }
    return defaultDatabase;
  }

  public Vector getAnnotationComments() {
    return annotationComments;
  }

  public Vector getTranscriptComments() {
    return transcriptComments;
  }

  /** Add a canned annotation comment.
   * 
   * @param comment - String to be added
   */
  public void addAnnotationComment(String comment)
  {
    addCannedComment(comment, "annotation");
  }

  /** Add a canned transcript comment.
   * 
   * @param comment - String to be added
   */
  public void addTranscriptComment(String comment)
  {
    addCannedComment(comment, "transcript");
  }

  private void addCannedComment(String comment, String which) {
    Vector comments;
    if (which.equals("annotation")) {
      if (annotationComments == null)
        annotationComments = new Vector();
      comments = annotationComments;
    }
    else {
      if (transcriptComments == null)
        transcriptComments = new Vector();
      comments = transcriptComments;
    }

    if (comments.size() == 0)
      comments.addElement("Select comment or enter your own");

    // Don't add the same comment twice
    if (comments.indexOf(comment) == -1)
      comments.addElement(comment);
  }

  // Used for deciding whether to show internal-only comments.
  public boolean internalMode() {
    return internalMode;
  }

  public String getProjectName (String username) {
    String projectname = null;
    if (userToProject != null && !internalMode() && username != null)
      projectname = (String) userToProject.get(username);
    return (projectname == null ? username : projectname);
  }

  /** Retrieve color for user name; if no color use defaultAnnotationColor.
   *  Note that this is called only if doUserTranscriptColouring is true
   *  and we're in internal (not public) mode. */
  public Color getAnnotationColor(String username) {
    Color color = null;
    if (username == null || username.trim().length() <=0 ||
        username.equalsIgnoreCase("auto") || userToColor == null) {
      color = Config.defaultAnnotationColor;
    } else {
      color = (Color) userToColor.get(username);
      if (color == null)
        // color = fp.getColour();
        color = Config.defaultAnnotationColor;
    }
    return color;
  }

  /** Return color for boxing tagged results */
  public Color getTaggedColor() {
    return taggedColor;
  }

  /** Return color for showing genomic sequencing errors or frameshifts */
  public Color getSeqErrorColor() {
    return seqErrorColor;
  }

  private void addUserInfo(String userinfo) {
    StringTokenizer tokens = new StringTokenizer (userinfo, ":");
    String user = null;
    String project = null;
    Color color = null;
    String full_name = null;
    while (tokens.hasMoreTokens()) {
      String tagval = tokens.nextToken();
      int breakIndex = tagval.indexOf("=");
      if (breakIndex > 0) {
        String tag = tagval.substring(0, breakIndex);
        String val = tagval.substring(breakIndex+1);
        if (tag.equals("user"))
          user = val;
        else if (tag.equals ("project"))
          project = val;
        else if (tag.equals ("color"))
          color = FeatureProperty.toColour(val,defaultUserColor);
        else if (tag.equals("full-name"))
          full_name = val;
      }
    }
    if (user != null) {
      if (project != null) {
        if (userToProject==null)
          userToProject = new Hashtable();
        userToProject.put(user, project);
      }
      if (color != null) {
        if (userToColor==null)
          userToColor = new Hashtable();
        userToColor.put(user, color);
      }
      if (full_name != null) {
        if (userToFullName==null)
          userToFullName = new Hashtable();
        userToFullName.put(user,full_name);
      }
    }
    if (user == null || color == null)
      complain("Bad user info string: " + userinfo);
  }

  Hashtable getPeptideStates () {
    return peptideStates;
  }

  PeptideStatus getPeptideStatus(String pep_status) {
    if (pep_status == null || pep_status.equals (""))
      // this is certain to be found
      pep_status = "not analyzed";
    else
      pep_status = pep_status.toLowerCase();
    PeptideStatus status = (PeptideStatus) peptideStates.get (pep_status);
    if (status == null) {
      status = new PeptideStatus();
      peptideStates.put (pep_status, status);
      logger.debug ("Had to create a default for peptide status " + pep_status);
    }
    return status;
  }

  private void addFeatureLoadSize(int size) {
    this.featureLoadSize = size;
  }
  public int getFeatureLoadSize() {
    return featureLoadSize;
  }
  private void addPeptideStatus(String peptideString) {
    PeptideStatus status = new PeptideStatus ();
    String peptide_key = status.initStatus (peptideString);
    if (peptideStates==null)
      peptideStates = new Hashtable();
    peptideStates.put(peptide_key.toLowerCase(), status);
  }

  public String getGeneUrl() {
    return geneUrl;
  }

  public String getBandUrl() {
    return bandUrl;
  }

  public String getScaffoldUrl() {
    return scaffoldUrl;
  }

  public String getSeqUrl() {
    return seqUrl;
  }

  public String getRangeUrl() {
    return rangeUrl;
  }

  /** For now, all annotations share a common base URL, defined in the style
   * file as ExternalRefURL.
   * We really should use weburl (in tiers file) to specify annot  URLs just as we
   * already do for result URLs. */
  String getExternalRefUrl() {
    return externalRefUrl;
  }

  public boolean getDashSets() {
    return dashSets;
  }

  public Color getAnnotationBackground() {
    return annotationBackground;
  }

  public void setAnnotationBackground(Color color) {
    annotationBackground = color;
  }

  public Color getFeatureBackground() {
    return featureBackground;
  }
  public void setFeatureBackground(Color col) {
    featureBackground = col;
  }

  public Color getEdgematchColor() {
    return edgematchColor;
  }
  public void setEdgematchColor(Color col) {
    edgematchColor = col;
  }

  /** Edgematch color is also used for generic outlines around features */
  public Color getOutlineColor() {
    return edgematchColor;
  }

  public Color getFeatureLabelColor() {
    return featureLabelColor;
  }

  public void setFeatureLabelColor(Color col) {
    featureLabelColor = col;
  }

  public Color getAnnotationLabelColor() {
    return annotationLabelColor;
  }

  public void setAnnotationLabelColor(Color col) {
    annotationLabelColor = col;
  }

  public Color getCoordBackground() {
    return coordBackground;
  }
  public void setCoordBackground(Color col) {
    coordBackground = col;
  }

  public Color getCoordForeground() {
    return coordForeground;
  }
  public void setCoordForeground(Color col) {
    coordForeground = col;
  }

  public Color getCoordRevcompColor() {
    return coordRevcompColor;
  }
  public void setCoordRevcompColor(Color col) {
    coordRevcompColor = col;
  }

  public Color getSequenceColor() {
    return sequenceColor;
  }
  public void setSequenceColor(Color col) {
    sequenceColor = col;
  }

  public Color getHighlightColor() {
    return highlightColor;
  }

  public Color getSelectionColor() {
    return selectionColor;
  }

  public Color getSeqGapColor() {
    return seqGapColor;
  }

  Color getExonDetailEditorBackgroundColor1() {
    return edeBackgroundColor1;
  }
  Color getExonDetailEditorBackgroundColor2() {
    return edeBackgroundColor2;
  }
  Color getExonDetailEditorFeatureColor1() {
    return edeFeatureColor1;
  }
  Color getExonDetailEditorFeatureColor2() {
    return edeFeatureColor2;
  }

  private void installDisplayPrefs(String nameString) {
    try {
      // For backwards compatibility with v1.4.2
      if (nameString.startsWith("apollo.gui.schemes")) {
        String newNameString = "apollo.config" + nameString.substring(nameString.lastIndexOf("."));
        logger.warn("Found obsolete DisplayPreferences " + nameString + "--using " +
            newNameString + ".  Please update " + styleFileString + ".");
        nameString = newNameString;
      }
      Class namerClass = Class.forName(nameString);
      displayPrefs = (DisplayPrefsI) namerClass.newInstance();
    } catch (Exception e) {
      logger.warn ("could not find DisplayPreferences " + nameString +
          " (err " + e.getMessage() + ")", e);
    }
  }

  protected DisplayPrefsI getDisplayPrefs() {
    if (displayPrefs == null)
      displayPrefs = new DefaultDisplayPrefs();
    return displayPrefs;
  }

  /** NameAdapters can be set in style as a default.  Can also be set per-type
      in tiers file. */
  private void installNameAdapter(String nameString) {
    this.nameAdapter = nameString;
    PropertyScheme scheme = getPropertyScheme();
    // There might be more than one annotation tier!
    TierProperty tp = scheme.getTierProperty("annotation", false);
    if (tp != null) {
      Vector annot_types = tp.getFeatureProperties();
      for (int i = 0; i < annot_types.size(); i++) {
        FeatureProperty fp = (FeatureProperty) annot_types.elementAt(i);
        fp.setNameAdapterStringIfNotSet(nameString);
      }
//    try {
//    scheme.write(new File(tiersFile));
//    System.out.println("Rewrote tiers file " + tiersFile + " because " +
//    "NameAdapters are no longer set in style " +
//    "(please delete), they are set in tiers file");
//    } catch (Exception e) {
//    System.out.println("Unable to update tiers file " + tiersFile + " please " +
//    "make changes to your *.tiers and *.style " +
//    "files");
//    System.out.println(e);
//    }
    }
  }

  public String getNameAdapter() {
    return nameAdapter;
  }

  public String getOverlapDefinition()
  {
    return overlapDefinition;
  }
  
  private void setOverlapper (String classString) {
    overlapDefinition = classString;
    PropertyScheme scheme = getPropertyScheme();
    TierProperty tp = scheme.getTierProperty("annotation", false);
    if (tp != null) {
      Vector annot_types = tp.getFeatureProperties();
      for (int i = 0; i < annot_types.size(); i++) {
        FeatureProperty fp = (FeatureProperty) annot_types.elementAt(i);
        fp.setOverlapperIfNotSet(classString);
      }
//    try {
//    scheme.write(new File(tiersFile));
//    System.out.println("Rewrote " + tiersFile + " because " +
//    "OverlapDefinitions are no longer set in style " +
//    "(please delete), they are set in tiers file");
//    } catch (Exception e) {
//    System.out.println("Unable to update " + tiersFile + " please " +
//    "make changes to your *.tiers and *.style " +
//    "files");
//    }
    }
  }

  public String getFileName() {
    return styleFileString;
  }

  /** Default font for all of apollo in theory, used for axis labels */
  Font getDefaultFont() { 
    return defaultFont; 
  }

  /** Default font for gene label if it does not have a peptide status(which has a font) 
      gene label appears as label to feature and in gene menu list */
  Font getDefaultFeatureLabelFont() {
    return defaultFeatureLabelFont;
  }

  /** Font for amino and dna sequence in EDE */
  public Font getExonDetailEditorSequenceFont() {
    return exonDetailEditorSequenceFont;
  }

  public Vector getResultTag(String result_type) {
    return (Vector) resultTag.get(result_type);
  }

  public Hashtable getResultTags() {
    return resultTag;
  }

  /** Field in url to replace with database selection */
  public String getDatabaseURLField() { return databaseURLField; }

  /** Field in url to replace with padding int */
  public String getPadLeftURLField() { return padLeftURLField; }
  public String getPadRightURLField() { return padRightURLField; }

  public int getDefaultPadding() { return defaultPadding; }

  /** font_str is composed of 3 comma delimited fields:
      fontname,fontstyle,fontsize (9 is the default)
              fontstyle can be 0,1,2,3 (plain, bold, italic, bolditalic)  
   */
  static Font parseFont (String font_str) {
    StringTokenizer tokenizer = new StringTokenizer(font_str, ",");
    // this is the default font
    String font_name = "Dialog"; // "Arial";
    int font_style = Font.PLAIN;
    int font_size = 9;
    Font font = null;

    try {
      if (tokenizer.hasMoreTokens()) {
        font_name = tokenizer.nextToken();
      }
      if (tokenizer.hasMoreTokens()) {
        font_style = Integer.parseInt(tokenizer.nextToken());
      }
      if (tokenizer.hasMoreTokens()) {
        font_size = Integer.parseInt(tokenizer.nextToken());
      }
      font = new Font(font_name, font_style, font_size);
    } catch (Exception e) {
      logger.error ("Failed to parse Font from " + font_str, e);
    }
    return font;
  }

  public String toString() {
    return(getFileName());
  }

  /** Returns full name of user - if dont have full name just returns user */
  String getFullNameForUser(String user) {
    if (user == null) return null;
    if (userToFullName == null) return user;
    String fullName = (String)userToFullName.get(user);
    if (fullName == null)
      fullName = user;
    // If we're in "public" mode just return user's project
    if (!internalMode()) {
      fullName = (String)userToProject.get(user);
      if (fullName == null)
        fullName = "unknown";
    }
    return fullName;
  }    

  // Need to refine this a bit.
  public boolean isSuperUser(String name) {
    return true;
  }

  public PropertyScheme getPropertyScheme() {
    if (properties == null && getTiersFile() != null) {
      setPropertyScheme(getTiersFile());
    }
    return properties;
  }

  /** Makes new PropertyScheme from tiers file, sets properties */
  private void setPropertyScheme(String tiersFile) {
    try {
      properties = new PropertyScheme(tiersFile);
    }
    catch (Exception e) {
      String msg = ("Failed to parse tiers file " + tiersFile);
      apollo.util.IOUtil.errorDialog(msg);
    }
  }

  /** This overlaps with Config.createStyle, but i dont see a good way to 
   *  generalize/combine the 2 
   *  It's possible to have an endless loop if 2 files point to each other 
   *  (which is a rather silly thing to do)--check for this. */
  private void importStyle(String styleFilename) {
    // First read the default style file for this style (should be in apollo/conf)
    // (This will be needed for webstart, but it's not working right yet.)
    //    String styleFilePath = Config.findStyleFile(styleFilename);
    String pathname = styleFilename;
    if (!styleFilename.startsWith("conf/"))  
      pathname = "conf/" + pathname;  
    String styleFilePath = apollo.util.IOUtil.findFile(pathname);
    if (styleFilePath == null || Config.isJavaWebStartApplication()) {
      logger.debug("importStyle: couldn't find " + styleFilename + " with findFile--trying findStyleFile");
      styleFilePath = Config.digForConfigFile(styleFilename);//findStyleFile(styleFilename);
    }

    if (styleFilePath != null) {
      // Make sure this new style we want to import isn't one of the ones already
      // on our list, which would cause an infinite loop.
      // styleFiles is the list of style files we've already read.
      for (int i=0; i<styleFiles.size(); i++) {
        String sf = (String)styleFiles.get(i);
        if (sf.equals(styleFilePath)) {
          logger.debug("already read style file " + sf + "--ignoring ImportStyle line for it in " + styleFilePath);
        }
      }

      // This will read the style settings right into this Style
      logger.debug("importStyle: reading " + styleFilePath);
      readStyle(styleFilePath);
    }

    // Now see if the user has a personalized style file in their .apollo directory
    String personalStyleFilePath = apollo.util.IOUtil.findFile("~/.apollo/" + styleFilename); 
    if (personalStyleFilePath != null && !(personalStyleFilePath.equals(styleFilePath))) {
      logger.debug("importStyle: reading personal style file " + personalStyleFilePath);
      readStyle(personalStyleFilePath);
    }
  }

  public int getSingleSpeciesPanelSize(){
    return singleSpeciesPanelSize;
  }

  public int getLinkPanelSize(){
    return linkPanelSize;
  }

  /** Initial state for sites visibility (view menu item) */
  public boolean getInitialSitesVisibility() { return initialSitesVisibility; }

  /** Whether to add link query radio buttons to synteny adap gui */
  public boolean addSyntenySpeciesNumDropdown() {
    return addSyntenySpeciesNumDropdown;
  }
  /** Whether to add a menu item to the result popup menu that will bring up
      the selected feature as the other species in synteny */
  public boolean addSyntenyResultMenuItem() { return addSyntenyResultMenuItem; }

  public boolean syntenyLinksAreGeneToGene() {
    return syntenyLinksAreGeneToGene;
  }

  /** Whether synteny should lock zooming with a shift or vice versa, initially. 
      Default: true */
  public boolean initialShiftForLockedZooming() { 
    return initialShiftForLockedZooming; 
  }

  /** Whether synteny's initial state is to lock scrolling. Default false */
  public boolean initialLockedScrolling() { return initialLockedScrolling; } 

  /**
   * This indicates whether a composite adapter UI is to load cross-species data
   * simply - that is, just read the adapters for each species & the compara sets 
   * in turn, and not propagate information from one read to the other.
   **/
  public boolean getNaiveCrossSpeciesDataLoading(){
    return naiveCrossSpeciesDataLoading;
  }

  /**
   * If the links between individual species' features are drawn opaquely
   * then rendering of the panels as a whole will be faster.
   **/
  public boolean useOpaqueLinks(){
    return useOpaqueLinks;
  }

  /** if true just create species name Species1 to SpeciesN for N species.
      default is false. game uses generic species - gff and ensj specify species */
  private boolean useGenericSyntenySpecies() {
    return useGenericSyntenySpecies;
  }

  /** String of class name for default synteny data adapter to use if none are 
      specified in style file - default is null */
  public String getDefaultSyntenyDataAdapterString() {
    return defaultSyntenyDataAdapter;
  }

  /** How many species to display in synteny - default is 1 */
  public int getNumberOfSpecies() {
    return numberOfSpecies;
  }

  /** Maps names (Name.Mouse) to adapter label/logical name (Mus Musculus).
      if useGenericSyntenySpecies is true just maps "Species1" -> "Species1",
      "Species2" -> "Species2", ... to work with SyntenyAdapter
   */
  public HashMap getSyntenySpeciesNames() {
    if (syntenySpeciesNames.isEmpty() && useGenericSyntenySpecies()) {
      for (int i=1; i<=getNumberOfSpecies(); i++) {
        syntenySpeciesNames.put("Species"+i,"Species"+i);
      }
    }
    return syntenySpeciesNames;
  }



  /** This maps these kinds of strings from the style file:
      "Species.Mouse.DataAdapter" -> "apollo.dataadapter.ensj.AnnotationEnsJAdapter"
      So basically its where the dataadapter is
   */
  public HashMap getSyntenySpeciesProperties() {
    return syntenySpeciesProperties;
  }

  /** Same as syntenySpeciesAndLinkOrder(Strings) minus links(denoted by "Compara")
      eg "Name.Mouse", "Name.Human"
   If synteny species names not explicitly set(ensj,gff), and useGenericSyntenySpecies
   is true, make a generic list up
   to getNumberOfSpecies (Species1,Species2,...SpeciesN) (game) */
  public List getSyntenySpeciesOrder() {
    if (syntenySpeciesOrder.isEmpty() && useGenericSyntenySpecies()) {
      for (int i=1; i<=getNumberOfSpecies(); i++) {
        syntenySpeciesOrder.add("Species"+i);
      }
    }
    return syntenySpeciesOrder;
  }

  public List getSyntenyLinkOrder() {
    return syntenyLinkOrder;
  }

  public String getSyntenySpeciesString(int i) {
    if (i >= getNumberOfSyntenySpecies()) return null; // err msg?
    return (String)getSyntenySpeciesOrder().get(i);
  }
  public int getNumberOfSyntenySpecies() { return getSyntenySpeciesOrder().size(); }

  /** Strings "Compara" and "Links" indicate that adapter is for loading links */
  boolean stringIsTagForSyntenyLink(String string) {
    if (string.toLowerCase().equals("compara")) return true;
    if (string.toLowerCase().equals("links")) return true;
    return false;
  }

  /** List of Strings which are the species/names/tags of sub-adapters. This is not
   a list of the adapters themselves. (eg "Name.Species1","Name.Species2",...)
   If UseGenericSyntenySpecies is true, just return the generic getSyntenySpeciesOrder
   Species1 thru SpeciesN with no link names (assume no link adapters - game)
   */
  public List getSyntenySpeciesAndLinkOrder() {
    if (useGenericSyntenySpecies())
      return getSyntenySpeciesOrder();
    return syntenySpeciesAndLinkOrder;
  }

  private Map getSpeciesToDbMap() {
    if (syntenySpeciesToDb  == null) {
      syntenySpeciesToDb = new HashMap(5);
    }
    return syntenySpeciesToDb;
  }

  /** speciesToDbString expected format "Droshophila melanogaster -> gadfly3" */
  private void addSyntenySpeciesToDatabase(String speciesToDbString) {
    int arrowIndex = speciesToDbString.indexOf("->");
    String species = speciesToDbString.substring(0,arrowIndex);
    String db = speciesToDbString.substring(arrowIndex+2);
    getSpeciesToDbMap().put(species.trim(),db.trim()); // trim whitespace
  }
  /** Database species maps to as proscribed by SpeciesToDatabase entries. 
      This is for synteny links to other species (used in game synteny). 
      Returns null (and prints err msg) if no such mapping.
   */
  public String getSyntenyDatabaseForSpecies(String species) {
    String db = (String)getSpeciesToDbMap().get(species);
    if (db == null) {
      String m="No mapping to database for species "+species+" in style "
      +" file. Need to have:\nSpeciesToDatabase \""+species+" -> database\"";
      logger.error(m);
    }
    return db;
  }

  /** This is perhaps a little funny, but a Style can specify different styles to be 
      loaded for certain databases. This allows one to specify styles and tiers for 
      each database(species). Whats a little funny is that the style now listed with
      the data adapter in apollo.cfg is not necasarily the style you get, depnding on
      the database you choose. Is there a better way to config this?
      I dont think we can check if this style exists yet as it may not be loaded - 
      or should we load it at this point? or only load it as needed? */
  private void addDatabaseToStyle(String databaseToStyleString) {
    // subroutinze this parsing?
    int arrowIndex = databaseToStyleString.indexOf("->");
    String db = databaseToStyleString.substring(0,arrowIndex);
    String style = databaseToStyleString.substring(arrowIndex+2);
    getDbToStyleMap().put(db.trim(),style.trim()); // trim whitespace
  }
  private Map getDbToStyleMap() {
    if (dbToStyle==null) 
      dbToStyle = new HashMap(5);
    return dbToStyle;
  }
  /** Returns true if have style for db */
  public boolean databaseHasStyle(String db) {
    if (db == null)
      return false;
    return getStyleForDb(db) != null;
  }

  /** Returns null if cant make style for db */
  public Style getStyleForDb(String db) {
    String styleString = (String)getDbToStyleMap().get(db);
    if (styleString==null)
      return null;
    return Config.createStyle(styleString); // returns null if style file doesnt exist
  }

  // this is now in chado-adapter.xml
///** for chado jdbc adapter. 2 valid values are "TIGR" and "Flybase" to distinguish
//between the 2 instantiations of the chado schema - eventually will probably
//want to break this down further. Default to "TIGR" */
//String getChadoInstance() {
//return chadoInstance;
//}

  /** Whether gene id field is displayed in annot info */
  public boolean showIdField() { return showIdField; }
  /** Whether transcript symbol/name is editable in annot info, default true */
  public boolean getTranscriptSymbolEditable() { return transcriptSymbolEditable; }
  /** Whether to show, and therefore use, the "Is Dicistronic?" checkbox in annot 
      info editor */
  public boolean showIsDicistronicCheckbox() { return showIsDicistronicCheckbox; }
  public boolean showEvalOfPeptide() { return showEvalOfPeptide; }
  public boolean showIsProbCheckbox() { return showIsProbCheckbox; }
  /** Whether to show/use finished checkbox in annot info transcript panel */
  public boolean showFinishedCheckbox() { return showFinishedCheckbox; }
  /** Whether to show db cross reference table in annot info */
  public boolean showDbXRefTable() { return showDbXRefTable; }
  public boolean showReplaceStopCheckbox() { return showReplaceStopCheckbox; }
  public boolean translationalFrameShiftEditingIsEnabled() {
    return translationalFrameShiftEditingEnabled;
  }
  public boolean seqErrorEditingIsEnabled() {
    return seqErrorEditingIsEnabled;
  }
  public boolean showOwnershipAnnotMenuItem() {
    return showOwnershipAnnotMenuItem;
  }
  public boolean showTranscriptFinishedAnnotMenuItem() {
    return showTranscriptFinishedAnnotMenuItem;
  }

  public boolean igbHttpConnectionEnabled() {
    return enableIgbHttpConnection;
  }

  public boolean transactionsAreInGameFile() {
    return transactionsAreInGameFile;
  }

  public int getGeneticCodeNumber() {
    return geneticCodeNumber;
  }

  /** Whenever we add a new parameter to the style file, we've been
   *  adding methods in Style.java to set/get some variable--it's
   *  getting annoying.  Parameters give us a general-purpose,
   *  extensible way to set key=value(s) pairs in the style.  The
   *  default can be set here (which, of course, means we can't set it
   *  for brand-new parameters that are not yet known) or by the
   *  caller. For case-insensitivity, keys are stored as lowercase and
   *  getParameter converts the request to lowercase before looking
   *  for it. */
  public void addParameter(String name, String value) {
    if (value != null && !value.equals("")) {
      if (parameters == null) {
        parameters = new Hashtable(1, 1.0F);
      }
      Vector values;
      name = name.toLowerCase();
      if (parameters.get(name) == null) {
        values = new Vector();
        parameters.put(name, values);
      } else {
        values = (Vector) parameters.get(name);
      }
      values.addElement(value);
    }
  }

  public void removeParameter(String key) {
    if (key != null && !key.equals("")) {
      if (parameters == null)
        return;
      key = key.toLowerCase();
      if (parameters.containsKey(key)) {
        parameters.remove(key);
      }
    }
  }

  public void replaceParameter(String key, String value) {
    key = key.toLowerCase();
    removeParameter(key);
    addParameter(key, value);
  }

  /** Returns the last value (even if there are many) */
  public String getParameter(String name) {
    name = name.toLowerCase();
    if ((parameters == null) || (parameters.get(name) == null)) {
      return "";
    } else {
      Vector values = (Vector) parameters.get(name);
      return (String) values.lastElement();
    }
  }

  /** Returns the whole vector of values for this parameter */
  public Vector getParameterMulti(String name) {
    if (parameters == null)
      return null;
    else {
      name = name.toLowerCase();
      return (Vector) parameters.get(name);
    }
  }

  /** Returns hash with only one string for each key. */
  public Hashtable getParameters() {
    if (parameters == null)
      parameters = new Hashtable(1, 1.0F);
    Hashtable hash = new Hashtable(1, 1.0F);
    Enumeration e = parameters.keys();
    while (e.hasMoreElements()) {
      Object key = e.nextElement();
      String value = (String) ((Vector) parameters.get(key)).elementAt(0); 
      hash.put(key, value);
    }
    return hash;
  }

  /** Returns hash of Parameter vectors. */
  public Hashtable getParametersMulti() {
    if (parameters == null)
      parameters = new Hashtable(1, 1.0F);
    return parameters;
  }

}
