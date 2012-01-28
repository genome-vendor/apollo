package apollo.config;

import java.awt.Color;
import java.util.*;
import java.io.PrintWriter;
import java.io.IOException;

import org.apache.log4j.*;

import apollo.gui.DetailInfo;

/** FeatureProperty comes from "Types" in the tiers file. All the configurations
 *  of Types in the tiers file are reflected here (color, score threshold, glyph...).
 *  FeatureProperties are grouped into TierProperties, which are for expanding and 
 *  hiding and such 
 *  In the types panel Types/FeatureProperties of the displayed tiers can be seen
 *  by right clicking on the tier.
 *  When something changes tell TierProperty to fire PropSchemeChangeEvent (TP then
 *  tells PropScheme to fire PSCE). This replaces Observable which ended up 
 *  firing a PSCE after doing a bunch of notifyObservers.
*/
public class FeatureProperty /*extends Observable*/
  implements Cloneable, java.io.Serializable {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureProperty.class);
  private final static String package_path = "apollo.config.";  // Config this?

  public static Color DEFAULT_COLOR = new Color(250,250,210);  // light yellow
  private static final Color DEFAULT_UTR_COLOR = Color.black; // ??
  public static String DEFAULT_STYLE
    = "DrawableResultFeatureSet"; // this should be unqualified, since there's now a drawable path in effect
  public static Color midBlue   = new Color(150,150,255);
  public static Color midRed    = new Color(255,100,100);
  public static Color blueGreen = new Color(0,255,150);
  public static Color brown     = new Color(227,144,0);

  public static final Integer GRP_GENE     = new Integer(1);
  public static final Integer GRP_HOMOLOGY = new Integer(2);
  public static final Integer GRP_SINGLE   = new Integer(3);

  public static Hashtable groupTypes = new Hashtable();
  static
  {
    groupTypes.put("GENE"    ,GRP_GENE);
    groupTypes.put("HOMOLOGY",GRP_HOMOLOGY);
    groupTypes.put("SINGLE"  ,GRP_SINGLE);
  }

  protected Vector columns;
  public static Vector default_columns = new Vector();
  static
  {
    default_columns.addElement(new ColumnProperty("GENOMIC_LENGTH"));
    default_columns.addElement(new ColumnProperty("MATCH_LENGTH"));
    default_columns.addElement(new ColumnProperty("GENOMIC_RANGE"));
    default_columns.addElement(new ColumnProperty("MATCH_RANGE"));
    default_columns.addElement(new ColumnProperty("SCORE"));
  }

  protected String sortProperty = null;
  protected boolean reverseSort = false;

  public static Hashtable colourHash = new Hashtable();

  static {
    colourHash.put("red",Color.red);
    colourHash.put("green",Color.green);
    colourHash.put("blue",Color.blue);
    colourHash.put("pink",Color.pink);
    colourHash.put("orange",Color.orange);
    colourHash.put("darkGray",Color.darkGray);
    colourHash.put("gray",Color.gray);
    colourHash.put("magenta",Color.magenta);
    colourHash.put("cyan",Color.cyan);
    colourHash.put("yellow",Color.yellow);
    colourHash.put("black",Color.black);
    colourHash.put("white",Color.white);
    colourHash.put("midBlue",midBlue);
    colourHash.put("midRed",midRed);
    colourHash.put("blueGreen",blueGreen);
    colourHash.put("brown",brown);
  }

  protected String  type;
  protected String tiername;
  protected TierProperty tier;
  protected Vector analysis_types = new Vector();
  protected String  style = DEFAULT_STYLE;
  protected Color   colour = DEFAULT_COLOR;
  private   Color   utrColor = DEFAULT_UTR_COLOR;
  private   Color   problematicColor = null;
  private   Color   finishedColor = null;
  protected boolean usescore = false;
  protected float   minScore = 0.0f;
  protected float   maxScore = 100.0f;
  // This is the minimal # of pixels required for the display
  protected int     minWidth = 3;
  protected float   thresholdScore = -1.0f;
  protected String  URLString = null;
  protected Integer groupFlag = GRP_GENE;
  protected Date    recentDate = null;
  protected Vector comments = new Vector();
  private   String idFormat;  
  private String idPrefix;
  private String chromosomeFormat;
  /** Making default annotType "gene". It actually already sort of is in that feats
      with null annotTypes end up as genes (due to a default in AnnotatedFeature).
      And null makes no sense as an annotType unless tiers io requires an annot type
      or annot editor disallows making an annot with no annot type, neither of which
      are the case MG 9.19.05 - this could be configurable?*/
  private static final String defaultAnnotType = "gene";
  private   String annotType = defaultAnnotType; //null;
  // These are only valid if this is an annotation tier
  //private   ApolloNameAdapterI name_adapter = null;
  private   String nameAdapterString;
  private   OverlapI overlapper = null;

  // Set to true if name adapter is explicitly set (rather than just defaulting
  // to DefaultNameAdapter)
  private boolean nameAdapterWasSet = false;
  private boolean overlapperWasSet = false;

  // Shouldn't be needed eventually but added now to allow writing out properly
  protected boolean groupFlagSet = false;

  /** Link params if feature is linked via FeatureProperty. */
  /** linkType indicates type of link it is. Default is NO_LINK */
  private LinkType linkType = new LinkType("NO_LINK");
  /** Species that are linked */
  private String linkSpecies1;
  private String linkSpecies2;

  /** warnOnEdit can be set at the Tier level and also at the Feature level (here) */
  private boolean warnOnEdit = false;
  // If warnOnEdit was set at the FeatureProperty level, leave it.
  // Otherwise, at some point we'll inherit it from the parent TierProperty.
  private boolean warnOnEditWasSet = false;

  /** Number of levels to the annotation - default is now 1 except for
      gene/pseudogene, which have 3 levels (gene-transcript-exon).
      Normally the number of levels for a result doesn't matter; it
      only matters if you're flipping the result to the other strand,
      so I made DrawableUtil.createDrawable do the special "make it a
      one-level glyph" thing only for one-level annotations; results
      are treated as DrawableFeatureSets, as before.
      # of levels can be explicitly set in tiers file with "number_of_levels : 1" */
  private int numberOfLevels = 1;

  //  /** Name of the ontology to which this type belongs--e.g. "SO:telomere"
  //   *  corresponds to ontology="SO", type="telomere" */
  //  private String ontology;

  /*
    The first three arguments are mandatory 
    the rest can be left empty
  */
  public FeatureProperty(TierProperty tier, String type, Vector anal_types,
                         Color colour, String style,
                         boolean usescore, float min, float max) {
    this(tier, type, anal_types);
    setColour (colour);
    setStyle (style);
    setSizeByScore (usescore);
    setMinScore (min);
    setMaxScore (max);
  }

  public FeatureProperty(TierProperty tier, String type, Vector anal_types) {
    init(tier,type,anal_types);
  }

  private void init(TierProperty tier, String type, Vector anal_types) {
    setDisplayType (type);
    setAnalysisTypes (anal_types);
    setTier (tier);
    columns = cloneDefaultColumns();
    //if (isAnnotationType())setNumberOfLevels(1);
    //otherwise for results, let it default to 3
  }

  private Vector cloneDefaultColumns() {
    Vector cloneOfDefaults = new Vector();
    
    for (int i=0; i<default_columns.size(); i++) {
      ColumnProperty cp = (ColumnProperty)default_columns.elementAt(i);
      ColumnProperty clonedCp;
      clonedCp = new ColumnProperty(cp);
      cloneOfDefaults.addElement(clonedCp);
    }

    return cloneOfDefaults;
  }

  public FeatureProperty(FeatureProperty from) {
    this(from.getTier(),
        from.getDisplayType(),
        from.getAnalysisTypes(),
        from.getColour(),
        from.getStyle(),
        from.getSizeByScore(),
        from.getMinScore(),
        from.getMaxScore());
    this.setMinWidth(from.getMinWidth());
    this.setUtrColor(from.getUtrColor());
    this.setURLString(from.getURLString());
  }

  public FeatureProperty(FeatureProperty from, boolean cloneTier) throws CloneNotSupportedException
  {
    this.setDisplayType(from.getDisplayType());
    if (from.getAnalysisTypes() != null && from.getAnalysisTypes().size() > 0) {
      this.setAnalysisTypes(from.getAnalysisTypes());
    }
    this.setColour(from.getColour());
    this.setStyle(from.getStyle());
    this.setSizeByScore(from.getSizeByScore());
    this.setMinScore(from.getMinScore());
    this.setMaxScore(from.getMaxScore());
    this.setMinWidth(from.getMinWidth());
    this.setUtrColor(from.getUtrColor());
    this.setURLString(from.getURLString());
    this.setTier(cloneTier ? (TierProperty)from.getTier().clone() : from.getTier());
  }
  
  public FeatureProperty(TierProperty tier, String type, Vector anal_types,
                         Color colour, String style) {
    init(tier,type,anal_types,colour,style);
  }

  void init(TierProperty tier, String type, Vector anal_types, Color colour,
            String style) {
    init(tier,type,anal_types);
    setColour(colour);
    setStyle(style);
  }

  public FeatureProperty(TierProperty tier, String type, Vector anal_types,
                         Color colour) {
    this(tier, type, anal_types);
    setColour (colour);
  }

  public FeatureProperty() {
  }

  protected void setTierName (String tier) {
    this.tiername = tier;
  }

  protected String getTierName () {
    return this.tiername;
  }

  protected void setTier (TierProperty tier) {
    if (tier != null) {
      this.tier = tier;
      setTierName(tier.getLabel());
      if (getDisplayType() != null)
	tier.addFeatureProperty(this);
    } 
    else {
      throw new NullPointerException("FeatureProperty.setTier: can't accept null tier!");
    }
  }

  public TierProperty getTier() {
    return tier;
  }

  public boolean isAnnotationType() {
    return Config.getPropertyScheme().isAnnotationTier(tiername);
  }

  /** Tells tier to fire PropSchemeChangeEvent which tell PropScheme to do so 
      This replaces notifyListeners from Observable (redundant) */
  private void firePropSchemeChangeEvent() {
    if (getTier() != null)
      getTier().firePropSchemeChangeEvent();
  }

  public boolean getSizeByScore() {
    return usescore;
  }

  public void setSizeByScore(boolean usescore, boolean isTemporary) {
    this.usescore = usescore;
    if (usescore == false) {
      minScore = 0.0f;
      maxScore = 0.0f;
    }
    else if (maxScore == 0.0f) {
      maxScore = 100.0f;
    }
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }

  public void setSizeByScore(boolean usescore) {
    setSizeByScore(usescore, false);
  }

  public float getThreshold() {
    return thresholdScore;
  }

  public void setThreshold(float thresh) {
    this.thresholdScore = thresh;
    firePropSchemeChangeEvent();
  }

  public void setDisplayType(String type) {
    if(type == null) {
      throw new NullPointerException("FeatureProperty.setDisplayType: can't accept null type");
    }
    this.type = type;
    firePropSchemeChangeEvent();
  }

  public void setDisplayType(String type, boolean temporary) {
    if (temporary) {
      this.type = type;
    }
    else {
      setDisplayType(type);
    }
  }
  
  public String getDisplayType() {
    return this.type;
  }

  public Date getRecentDate() {
    return recentDate;
  }

  public void setRecentDate(String dateString) {
    recentDate = apollo.util.DateUtil.makeADate(dateString);
  }

  /** Initially, the value of warnonedit is inherited from the parent tier--may later be
   *  overridden. */
  public void inheritWarnOnEdit(boolean warn) {
    if (!warnOnEditWasSet)
      setWarnOnEdit(warn);
  }

  public boolean warnOnEdit() {
    return warnOnEdit;
  }

  public void setWarnOnEdit(boolean warn) {
    warnOnEdit = warn;
    warnOnEditWasSet = true;
  }

  /** Set regular expression String that describes id format for type */
  public void setIdFormat(String idFormat) {
    this.idFormat = idFormat;
  }
  /** Regular expression that describes format for ID, null if not set */
  public String getIdFormat() {
    return idFormat;
  }
  
  /** 
  id prefix is not a regular expression
  Set String which describe id prefix for the type 
  used by ChadoJdbcNameAdapter
  */
  public void setIdPrefix(String idPrefix) {
    this.idPrefix = idPrefix;
  }
  /** Get the id prefix */
  public String getIdPrefix() {
    return idPrefix;
  } 
  
   /** Set chromosome Format
       This is a regular expression to get the chromosome number.
       used by ChadoJdbcNameAdapter
    */
  public void setChromosomeFormat(String chromosomeFormat) {
    this.chromosomeFormat = chromosomeFormat;
  }
  /** Get chromosome Prefix */
  public String getChromosomeFormat() {
    return chromosomeFormat;
  }

  /** Type of annotation this result creates, by default */
  public void setAnnotType(String annot_type) {
    this.annotType = annot_type;
  }
  /** Type of annotation this result creates, by default */
  public String getAnnotType() {
    return annotType;
  }
  /** Making default annotType "gene". It actually already sort of is in that feats
      with null annotTypes end up as genes (due to a default in AnnotatedFeature).
      And null makes no sense as an annotType unless tiers io requires an annot type
      or annot editor disallows making an annot with no annot type, neither of which
      are the case MG 9.19.05 - this could be configurable?*/
  public static String getDefaultAnnotType() { return defaultAnnotType; }

  protected void setAnalysisTypes (Vector anal_types) {
    if (anal_types != null && anal_types.size() > 0) {
      this.analysis_types = (Vector) anal_types.clone();
      // SUZ no need to do this because setTiers 
      // calls TierProperty.addFeatureProperty and in
      // that method there is already a call to addAnalysisType
      // tier.addAnalysisTypes (this, anal_types);
    } else {
      throw new NullPointerException("Can't create feature without analyses");
    }
  }

  public void addAnalysisType (String analysis) {
    if (!analysis_types.contains (analysis)) {
      analysis_types.addElement (analysis);
      if (tier != null)
	tier.addAnalysisType (this, analysis);
    }
  }

  /** An analysis type is what comes from "datatype" in the tiers file.
      There can be more than one data/analysis type per FeatProp. These map
      to SeqFeatures feature types. perhaps this should be renamed 
      getFeatureTypes? */
  public Vector getAnalysisTypes () {
    return analysis_types;
  }

  public int getAnalysisTypesSize() { return analysis_types.size(); }

  public String getAnalysisType(int i) {
    return (String)analysis_types.get(i);
  }

  public void setStyle(String style, boolean isTemporary)
  {
    this.style = style;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }
  
  public void setStyle(String style) {
    setStyle(style, false);
  }

  public String getStyle() {
    return this.style;
  }

  public void setColour(Color colour, boolean temporary) {
    if (temporary)
      this.colour = colour;
    else
      setColour(colour);
  }

  public void setColour(Color colour) {
    this.colour = colour;
    firePropSchemeChangeEvent();
  }

  public void setUtrColor(Color color) {
    this.utrColor = color;
    // i dont think we need to notify observers because this is only set 
    // at init time - am i right?
  }
  
  void setProblematicColor(Color color) {
    this.problematicColor = color;
  }
  void setFinishedColor(Color color) {
    this.finishedColor = color;
  } 
   
  public static Color toColour(String colourStr) {
    return toColour(colourStr, Color.darkGray);
  }

  public static Color toColour(String colourStr, Color defaultColour) {
    Color colour;
    try {
      colour = parseColour(colourStr);
    } 
    catch (NumberFormatException e) {
      logger.warn("failed to parse RGB colour \"" + colourStr + "\"");
      colour = defaultColour;
    }
    return colour;
  }

  public static Color parseColour(String colourStr) 
    throws NumberFormatException {

    if (colourHash.containsKey(colourStr)) {
      return (Color)colourHash.get(colourStr);
    } else {
      Color colour;
      int count = 0;
      int [] components = new int[3];
      StringTokenizer tokenizer = new StringTokenizer(colourStr,",");

      while (tokenizer.hasMoreTokens() && count < 3) {
	String token = tokenizer.nextToken();
	components[count++] = Integer.parseInt(token);
      }
      if (count == 3 && !tokenizer.hasMoreTokens()) {
	colour = new Color(components[0],components[1],components[2]);
      } else {
	throw(new NumberFormatException("Warning: Unknown colour " + colourStr));
      }
      return colour;
    }
  }

  public String  getGroupFlagAsString() {
    String groupStr = getKeyFromValue(groupTypes,groupFlag);
    if (groupStr.equals("UNKNOWN")) {
      logger.error("unknown grouping flag " + groupFlag);
      groupStr = "GENE";
    }
    return groupStr;
  }

  public String  getColourAsString() {
    return getColourAsString(colour);
  }

  public String getColourAsString(Color colour) {
    String colourStr = getKeyFromValue(colourHash,colour);
    if (colourStr.equals("UNKNOWN")) {
      colourStr = new String(colour.getRed() + "," +
                             colour.getGreen() + "," +
                             colour.getBlue());
    }
    return colourStr;
  }

  public String getKeyFromValue(Hashtable table, Object value) {
    Enumeration keys = table.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      if (table.get(key).equals(value)) {
        return (String)key;
      }
    }
    return "UNKNOWN";
  }

  public Color getColour() {
    return this.colour;
  }

  /** For DrawableGeneSeqFeature that colors utrs */
  public Color getUtrColor() {
    return utrColor;
  }
  
  public Color getProblematicColor() {
    return problematicColor;
  }
  public Color getFinishedColor() {
    return finishedColor;
  }  
  
  public Vector getColumns() {
    return getColumns (true);
  }

  public Vector getColumns(boolean use_default) {
    if (columns == null)
      return cloneDefaultColumns();
    else
      return columns;
  }

  public void setColumns(Vector columns) {
    this.columns = columns;
    for (int i=0;i<columns.size();i++) {
      ColumnProperty cp = (ColumnProperty)columns.elementAt(i);
    }
    firePropSchemeChangeEvent();
  }

  public void addColumn (ColumnProperty column) {
    if (columns == null)
      columns = new Vector(6);
    if (!columns.contains(column)) {
      columns.addElement(column);
    }
  }

  private String getColHeadingsString () {
    StringBuffer buf = new StringBuffer();
    buf.append ("{");
    for (int i = 0; i < getColumns().size(); i++) {
      buf.append (((ColumnProperty)getColumns().elementAt(i)).getHeading());
      if (i < getColumns().size() - 1)
        buf.append (" ");
    }
    buf.append ("}");
    return buf.toString();
  }

  public String getSortProperty() {
    return sortProperty;
  }

  public void setSortProperty(String sort_by) {
    this.sortProperty = sort_by;
  }

  public boolean getReverseSort() {
    return reverseSort;
  }

  public void setReverseSort(boolean reverse) {
    this.reverseSort = reverse;
  }

  public void setMaxScore(float max, boolean isTemporary) {
    this.maxScore = max;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }
  
  public void setMaxScore(float max) {
    setMaxScore(max, false);
  }

  public void setMinScore(float min, boolean isTemporary) {
    this.minScore = min;
    if (!isTemporary) {
      firePropSchemeChangeEvent();
    }
  }

  public void setMinScore(float min) {
    setMinScore(min, false);
  }

  public void setMinWidth(int min) {
    this.minWidth = min;
    firePropSchemeChangeEvent();
  }
  
  public void setURLString(String str) {
    URLString = str;
  }
  public void setGroupFlag(String grpStr) {
    if (groupTypes.get(grpStr) != null) {
      groupFlag = (Integer)groupTypes.get(grpStr);
      groupFlagSet = true;
    } else {
      logger.error("unknown grouping flag " + grpStr);
    }
  }
  public Integer getGroupFlag() {
    return groupFlag;
  }
  public String getURLString() {
    return URLString;
  }

  public float getMaxScore() {
    return maxScore;
  }

  public float getMinScore() {
    return minScore;
  }

  public int getMinWidth() {
    return minWidth;
  }

  /*
    type "Prediction", "tRNA", {"trnascan-se:dummy" "tRNAscan-se"}, "139,72,61", tru
    e, 0, 100 "apollo.gui.DrawableResultFeatureSet", {score GENOMIC_RANGE query_fram
    e}, score, false
  */
  public void write(PrintWriter out) throws IOException {
    StringBuffer buf = new StringBuffer("type ");
    buf.append(quoteIfSpace(getTier().getLabel()) + ", " +
               quoteIfSpace(getDisplayType()) + ", {");

    for (int i = 0; i < analysis_types.size(); i++) {
      buf.append ("\"" + (String) analysis_types.elementAt(i) + "\"");
      if (i < analysis_types.size() - 1)
        buf.append (" ");
    }
    buf.append ("}, ");

    buf.append (getColourAsString() + ", " +
                getSizeByScore() + ", " +
                getMinScore() + ", " +
                getMaxScore() + ", " +
                getStyle() + ", " +
                getColHeadingsString() + ", " +
                getSortProperty() + ", " +
                getReverseSort());

    if (minWidth != 3) {
      buf.append(", " + getMinWidth());
    }

    if (groupFlagSet) {
      buf.append(", " + getGroupFlagAsString());
    }
    if (URLString != null) {
      if (!groupFlagSet) {
        logger.error("trying to write URL string without group flag");
      }
      buf.append(", \"" + URLString + "\"");
      if ((thresholdScore != -1.0) || (getRecentDate() != null)) {
        buf.append(", " + getThreshold());
      }
      if (getRecentDate() != null) {
	// Be sure to write date in the appropriate format!
        buf.append(", " + apollo.util.DateUtil.formatDate(getRecentDate()));
      }
    }

    out.println (buf.toString());

    return;
  }

  protected String quoteIfSpace(String str) {
    if (str.indexOf(" ") > -1  || str.indexOf("/") > -1) {
      return new String("\"" + str + "\"");
    } else {
      return str;
    }
  }

  /** this needs to be a deep clone not a shallow one */
  public Object clone() throws CloneNotSupportedException {
    FeatureProperty clone = (FeatureProperty)super.clone();
    if (columns != null) // go through vector?
      clone.columns = (Vector)columns.clone();
    if (linkType != null)
      clone.linkType = linkType;
    clone.analysis_types = (Vector)analysis_types.clone();
    clone.colour = new Color(colour.getRGB());
    clone.utrColor = new Color(utrColor.getRGB());
    clone.comments = (Vector)comments.clone();
    //if (overlapper != null)clone.overlapper = (OverlapI)overlapper.clone();
    
    return clone;
  }

  FeatureProperty cloneFeatureProperty() {
    try { return (FeatureProperty)clone(); }
    catch (CloneNotSupportedException e) { return null; }
  }

  public boolean typeEquals(String typeName) {
    return (type.equalsIgnoreCase(typeName));
  }

  protected void addComment (String comment) {
    comments.addElement (comment);
  }

  protected Vector getComments () {
    return comments;
  }

  void setNameAdapterString(String str) {
    if (str == null) 
      return;
    nameAdapterString = str;
    nameAdapterWasSet = true;
//     try {
//       // Should FeatureProperties from same PropScheme with same name adapter class
//       // share the same name adapter instance. presently each feat prop has its own
//       // instance - probably no need for that.
//       // curationState.getNameAdapter(str);
//       name_adapter = (ApolloNameAdapterI) createClassFromName(str);
//     }
//     catch (ClassCastException e) {
//       name_adapter = null;
//       logger.error("FeatureProperty.setNameAdapter: " + str + " does not implement ApolloNameAdapterI");
//     }
  }

  public String getNameAdapterString() {
    return nameAdapterString;
  }

  public ApolloNameAdapterI createNameAdapter() {
    if (nameAdapterString == null) {
      logger.warn("No name adapter for " + this + "; please check the tiers file.");
      return new DefaultNameAdapter();
      //return null; //  this causes null pointer exception in CurState
    }
    try {
      return (ApolloNameAdapterI)createClassFromName(nameAdapterString);
    }
    catch (ClassCastException e) {
      logger.error("FeatureProperty.createNameAdapter: " + nameAdapterString +
                   " does not implement ApolloNameAdapterI", e);
      return null;
    }
  }
  
  void setNameAdapterStringIfNotSet(String str) {
    if (!nameAdapterWasSet)
      setNameAdapterString(str);
  }

  /** Has nameadapter if this is an annotation tier */
  public boolean hasNonDefaultNameAdapter() {
    //    return tiername != null && tiername.equalsIgnoreCase("Annotation") 
    return tiername != null && Config.getPropertyScheme().isAnnotationTier(tiername)
      && nameAdapterString != null && !nameAdapterString.equals(DefaultNameAdapter.class.getName());
  }

  /** Return null if no tiername or tiername is not "annotation" */
  // Why?  Is that really necessary?  It hurts us if we need to create a new tier
  // for an annotation type missing from the tiers file (e.g. ncRNA).
  // i think it should return default name adapter if name adapter is null
  // moved to CurationState to associate AnnotChangeLog
//   public ApolloNameAdapterI getNameAdapter() {
//     if (tiername == null) // may not have tiername 
//       return null; // -> return DefaultNameAdapter?
//     //    if (tiername.equalsIgnoreCase("annotation")) {
//       if (name_adapter == null)
//         name_adapter = DefaultNameAdapter.getDefaultNameAdapter();
//       return name_adapter;
// //     } else {
// //       logger.warn("Why is a name adapter requested in non-annotation tier " +
// //                           tiername);
// //       return null; // DefaultNameAdapter?
// //     }
//   }

  public void setOverlapper(String str) {
    try {
      overlapper = (OverlapI) createClassFromName(str);
    }
    catch (ClassCastException e) {
      overlapper = null;
      logger.error("FeatureProperty.setOverlapper: " + str + " does not implement OverlapI", e);
    }
    overlapperWasSet = true;
  }

  public void setOverlapperIfNotSet(String str) {
    if (!overlapperWasSet)
      setOverlapper(str);
  }

  /** Has overlapper if this is an annotation tier */
  public boolean hasOverlapper() {
    return tiername != null && Config.getPropertyScheme().isAnnotationTier(tiername);
  }
      
  public OverlapI getOverlapper() {
    if (tiername == null) 
      return null;
    //    if (tiername.equalsIgnoreCase("annotation")) {
    if (Config.getPropertyScheme().isAnnotationTier(tiername)) {
      if (overlapper == null)
        overlapper = SimpleOverlap.getSimpleOverlap();
      return overlapper;
    } else {
      logger.warn("an overlapper adapter was requested in non-annotation tier " + tiername);
      return null;
    }
  }

  protected Object createClassFromName(String str) {
    boolean no_class = true;
    String prefix = package_path;
    String class_name = str;
    int tries = 0;
    Object new_instance = null;
    while (new_instance == null && tries < 2) {
      try {
        if (prefix != "" && !(class_name.startsWith(prefix)))
          class_name = prefix + class_name;
        Class new_class = Class.forName (class_name);
        no_class = (new_class == null);
        new_instance = new_class.newInstance();
      }
      catch (Exception e) {
        logger.error("createClassFromName: couldn't create " + class_name + "; no class = " + no_class, e);
        tries++;
        if (tries < 2)
          prefix = "";
      }
    }
    return new_instance;
  }

  /** Given a class, e.g. apollo.config.FlyNameAdapter@1b5a5cf,
      return the simple string that names that class (FlyNameAdapter) */
  public String createNameFromClass(Object cl) {
    String name = cl.getClass().getName();
    if (name.indexOf(".") > 0)
      name = name.substring(name.lastIndexOf(".") + 1);
    return name;
  }

  /** linkTypeString proscribes the link type. Valid Strings are PEPTIDE, TRANSCRIPT, 
      and SELF. These are translated to a LinkType 
  */
  void setLinkType(String linkTypeString) {
    linkType = new LinkType(linkTypeString);
  }

  /** This describes the level at which the synteny links are created. valid values
      are "PARENT" & "CHILD" */
  void setSyntenyLinkLevel(String syntenyLinkLevel) {
    if (linkType == null) {
      logger.error("synteny_link_type must be specified before synteny_link_level");
      return;
    }
    //syntenyLinkLevel = LinkType.getLinkLevelForString(syntenyLinkLevel);
    linkType.setSyntenyLinkLevel(syntenyLinkLevel);
  }

  /** What should the synteny link use to match on. valid values are 
      "ID" & "RANGE" */
  void setSyntenyLinkMatchOn(String matchOn) {
    if (linkType == null) {
      logger.error("synteny_link_type must be specified before synteny_link_match_on");
      return;
    }
    linkType.setSyntenyLinkMatchOn(matchOn);
  }
  
  /** Return true if feat prop is a synteny link to another species */
  public boolean isSyntenyLinked() { 
    return !linkType.isNoLink();
  }

  /** Return LinkType - default LinkType.NO_LINK (not linked) 
      LinkType describes how FeatureProperty links to other species */
  public LinkType getLinkType() {
    return linkType;
  }

  void setLinkSpecies1(String species1) {
    linkSpecies1 = species1;
  }

  public String getLinkSpecies1() {
    return linkSpecies1;
  }

  void setLinkSpecies2(String species2) {
    linkSpecies2 = species2;
  }
  public String getLinkSpecies2() { 
    return linkSpecies2;
  }

  void setNumberOfLevels(int levels) {
    numberOfLevels = levels;
  }
  
  public int getNumberOfLevels() { 
    return numberOfLevels; 
  }
  public boolean isOneLevel() { return getNumberOfLevels() == 1; }

//   public void setOntology(String ontology) {
//     this.ontology = ontology;
//   }
//   public String getOntology() {
//     return ontology;
//   }

  public String toString() { return "FeatureProperty "+type+" (in tier "+tiername + ")"; }
}
