package apollo.config;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.awt.Color;
import javax.swing.JOptionPane;

import org.apache.log4j.*;

import apollo.config.Config;
import apollo.config.SimpleOverlap;
import apollo.editor.UserName;
import apollo.gui.drawable.DrawableUtil; // scandalous
import apollo.gui.drawable.Drawable;

/** 
 * Read/write tiers files 
 *
 * @version $Revision: 1.26 $ $Date: 2007/11/27 14:08:22 $ $Author: olivierarnaiz $
 */
public class TiersIO {

  protected static final Logger logger = LogManager.getLogger(TiersIO.class);
  protected static final Set stanzaSet = new HashSet();
  protected static final HashMap propertyMap = new HashMap();
  protected String filename;
  /** default feature property/type can be defined in tiers file with 
      typename DEFAULT */
  private FeatureProperty defaultFeatureProperty = null;
  
  /** Any new key needs to be added to this hash */
  static {
    HashSet temp = new HashSet();
    temp.add("tiername");
    temp.add("visible");
    temp.add("expanded");
    temp.add("sorted");
    temp.add("maxrows");
    temp.add("labeled");
    temp.add("curated");
    temp.add("warnonedit");
      
    stanzaSet.add("Tier");
    propertyMap.put("Tier", temp);

    temp = new HashSet();
    temp.add("label");
    temp.add("tiername");
    temp.add("datatype");
    temp.add("color");
    temp.add("usescore");
    temp.add("minscore");
    temp.add("maxscore");
    temp.add("minwidth");
    temp.add("scorethreshold");
    temp.add("glyph");
    temp.add("column");
    temp.add("sortbycolumn");
    temp.add("reversesort");
    temp.add("groupby");
    temp.add("weburl");
    temp.add("freshdate");
    temp.add("idformat");    
    temp.add("idprefix");
    temp.add("chromosomeformat");
    /* This should only apply to the Annotation tier
       It indicates which result types generate this 
       particular type of annotation, so that the annotation
       type can be set automatically, instead of requiring
       the curators to manually change the types each time
    */
    temp.add("annot_type");
    temp.add("utr_color");
    temp.add("problematic_color");
    temp.add("finished_color");
    /* These really apply to individual annotation types, not
       the entire session. Moved from style to here */
    temp.add("overlap_method");
    temp.add("name_method");
    temp.add("synteny_link_type");
    temp.add("link_query_species");
    temp.add("link_hit_species");
    temp.add("synteny_link_level");
    temp.add("synteny_link_match_on");

    temp.add("number_of_levels");
    temp.add("warnonedit");

    stanzaSet.add("Type");
    propertyMap.put("Type", temp);
  }

  protected PropertyScheme scheme;
  // If you use a HashSet, the types that belong to a
  // tier end up getting added to that tier in a random order.
  // Vector preserves the order.
  //  protected HashSet allTypes = new HashSet();
  protected Vector allTypes = new Vector();
  protected Vector comments = new Vector();

  public TiersIO(PropertyScheme scheme) {
    this.scheme = scheme;
  }

  private BufferedReader openFile (String filename) {
    this.filename = filename;
    File tiersfile = new File(filename);
    return openFile (tiersfile);
  }
  
  private BufferedReader openFile (File tiersfile) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(tiersfile));
    }
    catch (Exception e) {
      logger.error(e.getMessage());
    }
    return reader;
  }

  public Vector doParse (String filename) {
    int lineNumber = 0;
    String currentLine = null;
    String currentStanza = null;
    TierProperty currentTier = null;
    FeatureProperty currentType = null;

    try {
      BufferedReader reader = openFile(filename);
      if (reader != null) {
	boolean new_format = true;
	while((currentLine = reader.readLine()) != null && new_format) {
	  lineNumber++;
	  currentLine = currentLine.trim();
	  if (blankLine(currentLine)) {
	    currentStanza = null;
	  }
	  else if (isComment(currentLine)) {
	    Vector currentComments = (currentTier != null ?
				      currentTier.getComments() :
				      (currentType != null ? 
				       currentType.getComments() : comments));
	    currentComments.add(currentLine);
	  } 
	  else if (isStanza(currentLine)) {
	    currentStanza = getStanza(currentLine, lineNumber);
	    if (currentStanza != null) {
              currentTier = (isStanza(currentStanza, "Tier") ? 
                             getTier(null) : null);
              currentType = (isStanza(currentStanza, "Type") ? 
                             makeDefaultFeatureProperty() : null);
            }
            else {
              currentTier = null;
              currentType = null;
            }
	  }
	  else {
	    int index = findUnescaped (currentLine, ':');
	    if (index > 0) {
	      String tag = currentLine.substring(0, index).trim();
	      String value = currentLine.substring(++index).trim();
	      if (currentTier != null) {
		updateTier (currentTier, tag.toLowerCase(), value);
	      }
	      else if (currentType != null) {
		updateType (currentType, tag.toLowerCase(), value);
	      }
	      else {
		logger.info(filename + 
                            " is in old format based on line " +
                            lineNumber + ": " + currentLine);
		new_format = false;
	      }
	    }
	  }
	}
	if (new_format) {
	  Iterator i = allTypes.iterator(); // doesn't include DEFAULT type
	  while (i.hasNext()) {
	    currentType = (FeatureProperty) i.next();
	    currentTier = getTier (currentType.getTierName());
	    if (currentTier != null) {
	      currentTier.addFeatureProperty (currentType);
	    }
	    else
	      logger.warn("No tier " + currentType.getTier() +
                          " for type " + currentType.getDisplayType() + " in tiers file " + filename);
	  }
	  repairDrawableNames();
          validateAnnotTypes();
	}
	else {
	  reader.close();
	  readOld(filename);
	  repairDrawableNames();
          // Copy old tiers file to tiers.orig before we overwrrite it
          String orig = filename + ".orig";
          apollo.util.IOUtil.copyFile(filename, orig);
          logger.info("Moved old tiers file " + filename + " to " + orig);
	  /* save in the new format */
	  logger.info("Saving tiers file " + filename + " in new format ");
	  doSave (new File(filename), comments);
	}
      }
    }
    catch (Exception e) {
      logger.warn("Unable to parse line " + lineNumber + ": " + 
                  currentLine + "\n in tiers file " + filename, e);
    }
    return comments;
  }

  /** Just clones default feature property (hafta clone dont wanna reuse same ref)
      default feat prop can be specified with typename: DEFAULT */
  private FeatureProperty makeDefaultFeatureProperty() {
    if (defaultFeatureProperty == null)
      return new FeatureProperty();
    // cloning can be dangerous if the cloning doesnt go deep enough
    // then parts of the clone will be present in other clones
    return defaultFeatureProperty.cloneFeatureProperty();
    //return new FeatureProperty();
  }

  private void setDefaultFeatureProperty(FeatureProperty fp) {
    defaultFeatureProperty = fp;
    scheme.setDefaultFeatureProperty(fp);
  }
  
  protected boolean blankLine (String line) {
    return (line.length() == 0);
  }

  protected boolean isComment(String line) {
    return ((line.charAt(0) == '#') ||
	    (line.charAt(0) == '/' && line.charAt(1) == '/'));
  }

  protected boolean isStanza(String line) {
    return (line.charAt(0) == '[');
  }

  protected String getStanza(String line, 
			     int lineNumber) {
    String stanzaname = null;
    if (isStanza(line)) {
      if (line.charAt(line.length() - 1) != ']') {
	logger.warn("Unclosed stanza \"" + line + "\" at line" +
                    lineNumber + " in tiers file " + filename);
	stanzaname = line.substring (1);
      }
      else {
	stanzaname = line.substring(1, line.length() - 1);
      }
      if (stanzaname.length() < 1) {
	logger.warn("Empty stanza \"" + line + "\" at line" +
                    lineNumber  + " in tiers file " + filename);
	stanzaname = null;
      }
      else if (!isAllowed(stanzaname)) {
	logger.warn("stanza \"" + stanzaname + "\" at line" +
                    lineNumber + " is not allowed" + " in tiers file " + filename);
	stanzaname = null;
      }
    }
    return stanzaname;
  }

  protected boolean isStanza(String currentStanza, String stanzaname) {
    return (currentStanza != null && currentStanza.equals(stanzaname));
  }

  protected void updateTier (TierProperty tp, String tag, String value) {
    if (isAllowed ("Tier", tag)) {
      try {
	if ("tiername".equals(tag)) {
	  /* may need to replace the one that is currently there 
	     the other (from type) was just a reference to it,
	     this is the real thing */
          boolean isFirstTier = (scheme.getAllTiers().size() == 0);
	  initTier (tp, value, isFirstTier);
	}
	else if ("visible".equals(tag)) {
	  tp.setVisible((new Boolean(value)).booleanValue());
	}
	else if ("expanded".equals(tag)) {
	  tp.setExpanded((new Boolean(value)).booleanValue());
	}
	else if ("sorted".equals(tag)) {
	  tp.setSorted((new Boolean(value)).booleanValue());
	}
	else if ("labeled".equals(tag)) {
	  tp.setLabeled((new Boolean(value)).booleanValue());
	}
	else if ("maxrows".equals(tag)) {
	  tp.setMaxRow(Integer.parseInt(value));
	}
	else if ("curated".equals(tag)) {
	  tp.setCurated((new Boolean(value)).booleanValue());
	}
	else if ("warnonedit".equalsIgnoreCase(tag)) {
	  tp.setWarnOnEdit((new Boolean(value)).booleanValue());
	}
      }
      catch (Exception e) {
	logger.warn("Unable to parse tier " + tag + " : " + value + " in tiers file " + filename, e);
      }
    }
    else {
      logger.warn("Invalid tag \"" + tag + "\" for Tier. Ignoring " +
                  tag + " = " + value + " in tiers file " + filename);
    }
  }

  /** parses type in tiers file and fleshes out FeatureProperty fp, adds fp
      allTypes Vector of feat props (except default fp) */
  protected void updateType (FeatureProperty fp,
			     String tag,
			     String value) {
    if (isAllowed ("Type", tag)) {
      try {
        // old was typename, new is label
	if (tag.equalsIgnoreCase("typename") || tag.equalsIgnoreCase("label")) {
          // Check whether there's already a type with this name (case-insensitive),
          // and warn user if so
	  Iterator i = allTypes.iterator();
	  while (i.hasNext()) {
	    FeatureProperty type = (FeatureProperty) i.next();
            if (type.getDisplayType().equalsIgnoreCase(value))
              logger.warn("adding type " + value + " to tier " + fp.getTierName() + ", but another tier, " + 
                          type.getTierName() + ", already has a type called " + value + "(in tiers file " + filename + ")");
          }
	  fp.setDisplayType(value);

          // Special default setting of number of levels:  it defaults to 1 for
          // all annots, but we want it to be 3 for genes/pseudogenes (it can be
          // overridden later by a "number_of_levels parameter")
          if (value.equalsIgnoreCase("gene") ||
              value.equalsIgnoreCase("pseudogene"))
            fp.setNumberOfLevels(3);

          // dont add DEFAULT to allTypes, just use as template defFP
          if (value.equals("DEFAULT")) {
            setDefaultFeatureProperty(fp);
            //defaultFeatureProperty = fp;
          }
          else {
            allTypes.add(fp);
          }
	}
	else if ("tiername".equalsIgnoreCase(tag)) {
	  fp.setTierName(value);
	}
        // old was resulttype, new is datatype
	else if ("resulttype".equalsIgnoreCase(tag) || "datatype".equalsIgnoreCase(tag)) {
	  fp.addAnalysisType(value);
	}
	else if ("color".equalsIgnoreCase(tag)) {
	  Color colour = FeatureProperty.toColour(value);
	  fp.setColour(colour, false);
	}
	else if ("usescore".equalsIgnoreCase(tag)) {
	  fp.setSizeByScore((new Boolean(value)).booleanValue());
	}
	else if ("minscore".equalsIgnoreCase(tag)) {
	  fp.setMinScore((new Float(value)).floatValue());
	}
	else if ("maxscore".equalsIgnoreCase(tag)) {
	  fp.setMaxScore((new Float(value)).floatValue());
	}
	else if ("minwidth".equalsIgnoreCase(tag)) {
	  fp.setMinWidth(Integer.parseInt(value));
	}
	else if ("scorethreshold".equalsIgnoreCase(tag)) {
	  fp.setThreshold((new Float(value)).floatValue());
	}
	else if ("glyph".equalsIgnoreCase(tag)) {
	  fp.setStyle(value);
	}
	else if ("column".equalsIgnoreCase(tag)) {
          String [] columnWords = value.split("\\s+");
          if (columnWords.length == 1) {
	    fp.addColumn(new ColumnProperty(value));
          } else if (columnWords.length == 2) {
	    fp.addColumn(new ColumnProperty(columnWords[0], Integer.parseInt(columnWords[1])));
          } else {
            logger.error("column line format wrong for line with tag " + tag + " and value " + value);
          }
	}
	else if ("sortbycolumn".equalsIgnoreCase(tag)) {
	  fp.setSortProperty(value);
	}
	else if ("reversesort".equalsIgnoreCase(tag)) {
	  fp.setReverseSort((new Boolean(value)).booleanValue());
	}
	else if ("groupby".equalsIgnoreCase(tag)) {
	  fp.setGroupFlag(value);
	}
	else if ("weburl".equalsIgnoreCase(tag)) {
	  fp.setURLString(value);
	}
	else if ("freshdate".equalsIgnoreCase(tag)) {
	  fp.setRecentDate(value);
	}
        else if ("idformat".equalsIgnoreCase(tag)) {
          fp.setIdFormat(value);
        }	
	else if ("idprefix".equalsIgnoreCase(tag)) {
          fp.setIdPrefix(value);
        }
	else if ("chromosomeformat".equalsIgnoreCase(tag)) {
          fp.setChromosomeFormat(value);
        }
        else if ("annot_type".equalsIgnoreCase(tag)) { 
          fp.setAnnotType(value);
        }
        else if ("utr_color".equalsIgnoreCase(tag)) {
	  Color colour = FeatureProperty.toColour(value);
          fp.setUtrColor(colour);
        }
	else if ("problematic_color".equalsIgnoreCase(tag)) {
	  Color colour = FeatureProperty.toColour(value);
          fp.setProblematicColor(colour);
        }
	else if ("finished_color".equalsIgnoreCase(tag)) {
	  Color colour = FeatureProperty.toColour(value);
          fp.setFinishedColor(colour);
        }
        else if ("name_method".equalsIgnoreCase(tag)) {
          fp.setNameAdapterString(value);
        }
        else if ("overlap_method".equalsIgnoreCase(tag)) {
          fp.setOverlapper(value);
        }
        else if ("synteny_link_type".equalsIgnoreCase(tag)) {
          fp.setLinkType(value);
        }
        else if ("link_query_species".equalsIgnoreCase(tag)) {
          fp.setLinkSpecies1(value);
        }
        else if ("link_hit_species".equalsIgnoreCase(tag)) {
          fp.setLinkSpecies2(value);
        }
        // valid values are PARENT & CHILD
        else if ("synteny_link_level".equalsIgnoreCase(tag)) {
          fp.setSyntenyLinkLevel(value);
        }
        else if ("synteny_link_match_on".equalsIgnoreCase(tag)) {
          fp.setSyntenyLinkMatchOn(value);
        }
	else if ("warnonedit".equalsIgnoreCase(tag)) {
	  fp.setWarnOnEdit((new Boolean(value)).booleanValue());
	}
        else if ("number_of_levels".equalsIgnoreCase(tag)) {
          try {
            int levels = Integer.valueOf(value).intValue();
            fp.setNumberOfLevels(levels);
          }
          catch (NumberFormatException e) { 
            logger.warn("NumberFormatException in " + tag);
          }
        }
      }
      catch (Exception e) {
	logger.debug(e.getMessage());
	logger.fatal("unable to parse type " + tag + " : " + value + " in tiers file " + filename, e);
	System.exit(1);
      }
    }
    else {
      logger.warn("Invalid tag \"" + tag + "\" for Type. Ignoring " +
                  value + " in tiers file " + filename+" Check that tag"+
                  " is allowed (has to be set in HashSet)");
    }
  }

  protected TierProperty getTier(String tiername) {
    TierProperty tp;
    if (tiername == null) {
      tp = new TierProperty();
    }
    else {
      tp = scheme.getTierProperty(tiername, false);
      if (tp == null) {
	tp = new TierProperty ();
        boolean isFirstTier = (scheme.getAllTiers().size() == 0);
	initTier (tp, tiername, isFirstTier);
      }
    }
    return tp;
  }

  protected void initTier (TierProperty tp, String tiername, boolean isFirstTier) {
    tp.setLabel(tiername);
    scheme.addTierType(tp);
    tp.setIsFirstTier(isFirstTier);
  }

  /** Used for updating existing tier--no need to set isFirstTier */
  protected void initTier (TierProperty tp, String tiername) {
    tp.setLabel(tiername);
    scheme.addTierType(tp);
  }

  public boolean isAllowed(String stanza) {
    return stanzaSet.contains(stanza);
  }
  
  public boolean isAllowed(String stanza, String tag) {
    Object key = stanza;
    Set propSet = (Set) propertyMap.get(key);
    if (propSet == null)
      return false;
    else if (propSet.contains(tag))
      return true;
    else
      // for backwards compatibility
      return ("resulttype".equalsIgnoreCase(tag) || "typename".equalsIgnoreCase(tag));
  }

  public static int findUnescaped(String str, char toChar) {
    return findUnescaped(str, toChar, 0, str.length());
  }
  
  public static int findUnescaped(String str, char toChar, int startindex,
				  int endindex) {
    for(int i=startindex; i < endindex; i++) {
      char c = str.charAt(i);
      if (c == '\\') {
	i++;
	continue;
      }
      else if (c == toChar) {
	return i;
      }
    }
    return -1;
  }

  public void doSave (File file, Vector comments) {
    try {
      PrintWriter out
	= new PrintWriter(new BufferedWriter(new FileWriter(file)));
      String date = (new Date()).toString();
      out.println("# Tiers for " + Config.getStyle().getFileName() + 
		  " saved by " + UserName.getUserName() + " on " + 
		  date + "\n");

      saveComments (out, comments);
      out.println ("");
      Vector tiersVect = scheme.getAllTiers();
      int tier_count = tiersVect.size();
      TierProperty default_tp = new TierProperty();
      FeatureProperty default_fp = makeDefaultFeatureProperty();
      //new FeatureProperty();
      for (int i=0; i < tier_count; i++) {
	TierProperty tp = (TierProperty) tiersVect.elementAt(i);
	saveTier (out, tp, default_tp);
	Vector typesVect = tp.getFeatureProperties();
	for (int j = 0; j < typesVect.size(); j++) {
	  FeatureProperty fp = (FeatureProperty)typesVect.elementAt(j);
	  saveType (out, fp, default_fp);
	}
      }
      out.close();
    }
    catch (Exception e) {
      logger.warn("unable to write tiers file due to exception", e);
    }
  }

  private void saveTier (PrintWriter out,
			 TierProperty tp, 
			 TierProperty default_tp) {
    saveComments (out, tp.getComments());
    out.println ("[Tier]");
    out.println ("tiername : " + tp.getLabel());
    if (tp.isVisible() != default_tp.isVisible())
      out.println ("visible : " + tp.isVisible());
    if (tp.isExpanded() != default_tp.isExpanded())
      out.println ("expanded : " + tp.isExpanded());
    if (tp.isSorted() != default_tp.isSorted())
      out.println ("sorted : " + tp.isSorted());
    if (tp.isLabeled() != default_tp.isLabeled())
      out.println ("labeled : " + tp.isLabeled());
    if (tp.isCurated() != default_tp.isCurated())
      out.println ("curated : " + tp.isCurated());
    // warnOnEdit can apply to a tier or to individual types.
    // Rather than trying to figure out whether all types in this tier have
    // the same setting for this parameter, just write out this parameter for
    // each type rather than for the whole tier.
    //    if (tp.warnOnEdit() != default_tp.warnOnEdit())
    //      out.println ("warnonedit : " + tp.warnOnEdit());
    if (tp.getMaxRow() != default_tp.getMaxRow())
      out.println ("maxrows : " + tp.getMaxRow());
    out.println ("");
  }

  private void saveType (PrintWriter out,
			 FeatureProperty fp, 
			 FeatureProperty default_fp) {
    saveComments (out, fp.getComments());
    out.println ("[Type]");
    out.println ("label : " + fp.getDisplayType());
    out.println ("tiername : " + fp.getTierName());
    
    Vector analysis_types = fp.getAnalysisTypes();
    for (int i = 0; i < analysis_types.size(); i++) {
      out.println ("datatype : " + (String) analysis_types.elementAt(i));
    }
    out.println ("glyph : " + fp.getStyle());
    out.println ("color : " + fp.getColourAsString());
    if (fp.warnOnEdit() != default_fp.warnOnEdit())
      out.println ("warnonedit : " + fp.warnOnEdit());
    if (fp.getIdFormat() != null && fp.getIdFormat() != default_fp.getIdFormat())
      out.println ("idformat : " + fp.getIdFormat());
    if (fp.getIdPrefix() != null && fp.getIdPrefix() != default_fp.getIdPrefix())
      out.println ("idPrefix : " + fp.getIdPrefix());  
    if (fp.getChromosomeFormat() != null && fp.getChromosomeFormat() != default_fp.getChromosomeFormat())
      out.println ("chromosomeFormat : " + fp.getChromosomeFormat());     
    if (fp.getAnnotType() != null && fp.getAnnotType() != default_fp.getAnnotType())
      out.println ("annot_type : " + fp.getAnnotType());
    if (fp.getUtrColor() != null && fp.getUtrColor() != default_fp.getUtrColor())
      out.println ("utr_color : " + fp.getColourAsString(fp.getUtrColor()));
    // 3/22/04: Added by NH.  Make sure these work.
    // Since DefaultNameAdapter is now a singleton != works against singleton. MG 4/5
    if (fp.hasNonDefaultNameAdapter())
        //&& fp.getNameAdapterString() != DefaultNameAdapter.getDefaultNameAdapter())
      out.println ("name_method : " + fp.getNameAdapterString());
    // similar to name adapter check against singleton default overlapper(SimpleOverlap)
    if (fp.hasOverlapper() 
        && fp.getOverlapper() != SimpleOverlap.getSimpleOverlap())
      out.println ("overlap_method : " + fp.createNameFromClass(fp.getOverlapper()));
    if (fp.getSizeByScore() != default_fp.getSizeByScore())
      out.println ("usescore : " + fp.getSizeByScore());
    if (fp.getSizeByScore()) {
      if (fp.getMinScore() != default_fp.getMinScore())
	out.println ("minscore : " + fp.getMinScore());
      if (fp.getMaxScore() != default_fp.getMaxScore())
	out.println ("maxscore : " + fp.getMaxScore());
      if (fp.getMinWidth() != default_fp.getMinWidth())
	out.println ("minwidth : " + fp.getMinWidth());
    }
    if (fp.getThreshold() != default_fp.getThreshold())
      out.println ("scorethreshold : " + fp.getThreshold());
    Vector columns = fp.getColumns(false);
    if (columns != null) {
      for (int i = 0; i < columns.size(); i++) {
        ColumnProperty column_property = (ColumnProperty)columns.elementAt(i);
	out.println ("column : " + column_property.getHeading() + " " + column_property.getPreferredWidth());
      }
    }
    if (fp.getSortProperty() != null && fp.getSortProperty() != default_fp.getSortProperty())
      out.println ("sortbycolumn : " + fp.getSortProperty());
    if (fp.getReverseSort() != default_fp.getReverseSort())
      out.println ("reversesort : " + fp.getReverseSort());
    if (fp.getGroupFlag() != null && fp.getGroupFlag() != default_fp.getGroupFlag())
      out.println ("groupby : " + fp.getGroupFlagAsString());
    if (fp.getURLString() != null)
      out.println ("weburl : " + fp.getURLString());
    if ((fp.getThreshold() != -1.0) || (fp.getRecentDate() != null))
      out.println ("freshdate : " + fp.getRecentDate());
    out.println ("");

    if (fp.getNumberOfLevels() != 1)
      out.println ("number_of_levels : " + fp.getNumberOfLevels());
  }

  private void saveComments (PrintWriter out, Vector comments) {      
    for (int i = 0; i < comments.size(); i++) {
      out.println ((String) comments.elementAt(i));
    }
  }

  private void readOld (String fileName)
    throws IOException, FileNotFoundException {
    File    typeFile = new File(fileName);
    String  tier_label;
    String  type;
    Boolean visible;
    Boolean expand;
    Boolean sort;
    Boolean labeled;
    Integer maxRow;
    Boolean size;
    String  style;
    Float   minScore;
    Float   maxScore;
    Float   threshScore;
    Color   colour;
    Vector  bracedStrs = null;
    Stack   bracedSets = new Stack();
    boolean inBraces = false;
    Boolean reverseSort;
    String sortProperty = null;
    Hashtable datatypeLabels = new Hashtable();

    try {
      StreamTokenizer tokenizer =
        new StreamTokenizer(new BufferedReader(new FileReader(typeFile)));
      tokenizer.resetSyntax();
      tokenizer.wordChars('_','_');
      tokenizer.wordChars('a','z');
      tokenizer.wordChars('A','Z');
      tokenizer.wordChars('.','.');
      tokenizer.wordChars('0','9');
      tokenizer.wordChars('-','-');
      tokenizer.wordChars(':',':');
      tokenizer.wordChars('{','{');
      tokenizer.wordChars('}','}');
      tokenizer.wordChars('*','*');
      tokenizer.commentChar('/');
      tokenizer.quoteChar('"');
      tokenizer.whitespaceChars(' ',' ');
      tokenizer.whitespaceChars(',',',');
      tokenizer.eolIsSignificant(true);
      tokenizer.slashStarComments(true);

      boolean EOF     =  false;
      int     tokType =  0;
      Vector  words   =  new Vector();
      int line_number = 0;
      boolean alreadyPoppedUpAParseError = false;
      
      while (!EOF) {
        try {
          tokType = tokenizer.nextToken();
	  // end of file
          if (tokType == StreamTokenizer.TT_EOF) {
            EOF = true;
	    
          } 
	  // parse out worrds
	  else if (tokenizer.sval != null) {
            if (!inBraces) {
              if (!tokenizer.sval.startsWith("{")) {
                // this is a simple bare word
                words.addElement(tokenizer.sval);
              } 
	      else {
                /* this is starting a new set of braces,
                   so it must be either the analysis types
                   or the column headings
                */
                bracedStrs = grabTypeStr(tokenizer.sval,
                                         bracedStrs, bracedSets);
                inBraces = ! tokenizer.sval.endsWith("}");

              }
            } 
	    else {
              bracedStrs = grabTypeStr(tokenizer.sval,
                                       bracedStrs, bracedSets);
              inBraces = ! tokenizer.sval.endsWith("}");
            }
          } 
	  // if number -> error
	  else if (tokType == StreamTokenizer.TT_NUMBER) {
            words.addElement(Double.toString(tokenizer.nval));
            logger.warn("number found in group types file: " + tokenizer.nval);
          } 
	  // End of line reached - do something with words
	  else { 
	    TierProperty tp=null;
            ++line_number;
            /* Counting the line numbers as tokenizer.lineno()
	       always returns 1. not sure why.
	       Apparently the number of words has to be from
	       4 to 7 or 10 to 13(see below) */
            if ((words.size() == 4 || 
		 words.size() == 5 || 
		 words.size() == 6 ||
		 words.size() == 7) &&
                bracedSets.size() == 0 &&
                ((String) words.elementAt(0)).equalsIgnoreCase("tier")) {
              tier_label = (String) words.elementAt(1);
              visible  = Boolean.valueOf((String)words.elementAt(2));
              expand   = Boolean.valueOf((String)words.elementAt(3));
              if (words.size() >= 5) {
                sort     = Boolean.valueOf((String)words.elementAt(4));
              } 
	      else {
                sort = new Boolean(false);
              }
              if (words.size() >= 6) {
                maxRow     = Integer.valueOf((String)words.elementAt(5));
              }
	      else {
                maxRow = new Integer(0);
              }
              if (words.size() == 7) {
                labeled  = Boolean.valueOf((String)words.elementAt(6));
              }
	      else {
                labeled = new Boolean(false);
              }
	      tp = scheme.getTierProperty(tier_label, false);
              if (tp == null) {
                tp = new TierProperty(tier_label,
                                      visible.booleanValue(),
                                      expand.booleanValue(),
                                      sort.booleanValue(),
                                      maxRow.intValue(),
                                      labeled.booleanValue());
                scheme.addTierType(tp);
              }
	      else {
                tp.setVisible (visible.booleanValue());
                tp.setExpanded (expand.booleanValue());
                tp.setSorted (sort.booleanValue());
              }
            }
	    else if ((words.size() >= 10 &&
		      words.size() <= 14) &&
		     bracedSets.size() == 2 &&
		     ((String)words.elementAt(0)).equalsIgnoreCase("type")) {
              tier_label = (String) words.elementAt(1);
              type = (String) words.elementAt(2);
	      tp = scheme.getTierProperty(tier_label, false);
              if (tp == null) {
                tp = new TierProperty(tier_label, true, false);
                scheme.addTierType(tp);
              }

              colour =
                FeatureProperty.toColour((String)words.elementAt(3));
              size     = Boolean.valueOf((String)words.elementAt(4));
              minScore = Float.valueOf((String)words.elementAt(5));
              maxScore = Float.valueOf((String)words.elementAt(6));
              style    = (String) words.elementAt(7);
              sortProperty = (String)words.elementAt(8);
              reverseSort =
                Boolean.valueOf((String)words.elementAt(9));

              Vector column_headings = (Vector) bracedSets.pop();
              Vector prog_db = (Vector) bracedSets.pop();

              // Note that this doesn't test for whether the same type name is
              // used twice but with different case (e.g. promoter and Promoter)
              if (datatypeLabels.containsKey(type)) {
		logger.warn("type name " + type + " appears twice in tiers file " + Config.getTiersFile());
              } 
	      else {
                datatypeLabels.put(type,new Boolean(true));
              }

              FeatureProperty fp 
		= new FeatureProperty(tp,
				      type,
				      prog_db,
				      colour,
				      style,
				      size.booleanValue(),
				      minScore.floatValue(),
				      maxScore.floatValue());
              Vector columns = new Vector();
              for (int i=0;i<column_headings.size();i++) {
                String column_heading = (String)column_headings.elementAt(i);
                columns.addElement(new ColumnProperty(column_heading));
              }
              fp.setColumns(columns);
	      fp.setSortProperty(sortProperty);
	      fp.setReverseSort(reverseSort.booleanValue());
              if (words.size() >= 11) {
                String grouper = (String)words.elementAt(10);
                fp.setGroupFlag(grouper);
              }
              if (words.size() >= 12) {
                String URLString = (String)words.elementAt(11);
                fp.setURLString(URLString);
              }
              if (words.size() >= 13) {
                threshScore = Float.valueOf((String)words.elementAt(12));
                fp.setThreshold(threshScore.floatValue());
              }
              if (words.size() >= 14) {
		/** Date of last pipeline run--
		    sequences with dates newer than this are "new" sequences */
		String dateString = (String)words.elementAt(13);
		fp.setRecentDate(dateString);
              }
            } 
	    // improperly formatted line - throw error
	    else {
              if (words.size() > 0) {
                // Fixes exception thrown if no second word
                String first_word = (String)words.elementAt(0);
                String second_word = "";
                if (words.size() > 1)
                  second_word =  (String)words.elementAt(1);
                String msg = ("ignoring type line "+line_number+
			      " in file "+fileName+" due to parse errors.\n" +
			      "The line begins with \"" +
			      first_word+" "+second_word+
			      "\""+" and has " + words.size() + " words.\n");
		if (!first_word.equalsIgnoreCase("type"))
		  msg +=  " Should start with \"type\".";
                /* Should there be a popup message box for user?
		   only show for 1st error */
                if(!alreadyPoppedUpAParseError) {
                  msg = msg+
		    "\nOnly showing this error dialog for first error.";
                  JOptionPane.showMessageDialog(null,
						"WARNING: " + msg,
						"Parse Error in "+fileName,
                                                JOptionPane.ERROR_MESSAGE);
                }
                alreadyPoppedUpAParseError = true;
                logger.warn(msg);
              }
            } // end of else

	    parseKeyValuePairs(words,tp);
	    // done going through all words
            words.removeAllElements();
            bracedStrs = null;
            bracedSets.removeAllElements();
          } // end of else statement for parsing words
        } // end of try after while EOF
	catch (IOException e) {
          // fixme: If this is exiting, shouldn't it just throw the
          // exception?
          logger.warn(e);
          throw e;
        }
      } // end of while(!EOF) loop 
    } catch (FileNotFoundException e) {
      // fixme: see above
      logger.warn(e);
      throw e;
    }
    //    System.out.println("Reading types group file: " + typeFile);
  }

  private static String removeBraces(String str) {
    if (str.startsWith("{")) {
      str = str.substring(1);
    }
    if (str.endsWith("}")) {
      str = str.substring(0,str.length()-1);
    }
    return str;
  }

  private static Vector grabTypeStr (String str,
                                     Vector bracedStrs,
                                     Stack bracedSets) {
    boolean braces_end = str.endsWith("}");

    String clean_str = removeBraces(str);

    if (bracedStrs == null)
      bracedStrs = new Vector();

    if (clean_str.length() != 0) {
      bracedStrs.addElement(clean_str);
    }

    if (braces_end) {
      bracedSets.push (bracedStrs);
      bracedStrs = null;
    }
    return bracedStrs;
  }

  /** Parse all words that begin with "key=value:". Presently this is just 
      isProtein. Evenutally all words could go this route.
      Then the type string wouldnt have so strict about order,
      and it would be clearer.
  */
  private void parseKeyValuePairs(Vector words,TierProperty tierProperty) {
    if (tierProperty==null) return;
    for (int i=0; i<words.size(); i++) {
      String word = (String)words.get(i);
      if (!isKeyValue(word)) continue;
      // strip out property:
      String keyValue = word.substring(9);
      int equalsIndex = keyValue.indexOf('=');
      if (equalsIndex==-1) continue;
      String key = keyValue.substring(0,equalsIndex);
      String value = keyValue.substring(equalsIndex+1);
      
      if (key.equals("isProtein")) {
	// "true" -> true
	boolean isProtein = Boolean.valueOf(value).booleanValue(); 
	tierProperty.setIsProtein(isProtein);
      }
    }
  }
  /** KeyValues start with "key=value:" */
  private boolean isKeyValue(String word) {
    return word.startsWith("property:");
  }

  private void repairDrawableNames() {
    Vector tiers = scheme.getAllTiers();
    int tier_count = tiers.size();
    for (int i = 0; i < tier_count; i++) {
      TierProperty tp = (TierProperty) tiers.elementAt(i);
      Vector types = tp.getFeatureProperties();
      int type_count = types.size();
      for (int j = 0; j < type_count; j++) {
	FeatureProperty fp = (FeatureProperty) types.elementAt(j);
	String style = fp.getStyle();
        // No longer need complete glyph path
	if (style.startsWith("apollo.gui.drawable."))
	  style = style.substring ("apollo.gui.drawable.".length());
	if (style.startsWith("shape:"))
	  style = style.substring ("shape:".length());
	int index;
	index = style.indexOf("SeqFeature");
	if (index > 0)
	  style = style.substring(0, index) + "FeatureSet";
	/* in either of the cases below, switch to triangle */
	if (style.equalsIgnoreCase("DrawableInsertFeatureSet") ||
	    style.equalsIgnoreCase("DrawableInsertion"))
          style = "Triangle";
        // Update some archaic glyph names
        if (style.equalsIgnoreCase("DrawableAnnotatedFeatureSet"))
          style = "DrawableGeneFeatureSet";
        if (style.equalsIgnoreCase("DrawableTerminalCodon"))
          style = "SiteCodon";
	/* just to be on the safe side, try it first and see
	   if this is a valid drawable */
	Drawable glyph = DrawableUtil.createGlyph(style);
	if (glyph == null) {
          /* this is the default */
          logger.warn("unable to find drawable Java class for " +
                      style + " in tiers file " + filename +
                      ". Will use default DrawableResultFeatureSet");
          style = "DrawableResultFeatureSet";
        }
	fp.setStyle(style);
      }
    }
  }

  /** In the tiers file, you can specify the annotation type to use if a result type
      is made into an annotation, e.g.
      tiername : Transposon result
      datatype : JOSHTRANSPOSON:Sept
      datatype : BDGP_TE:092002
      color : 255,0,203
      annot_type : transposable_element

      Check these and make sure the annot_types are actually on the list of
      known annotation types. */
  private void validateAnnotTypes () {
    Vector tiers = scheme.getAllTiers();
    int tier_count = tiers.size();
    //    TierProperty annotations = scheme.getTierProperty("annotation", false);
    //    Vector annot_types = annotations.getFeatureProperties();
    Vector annot_types = scheme.getAnnotationFeatureProps();
    for (int i = 0; i < tier_count; i++) {
      TierProperty tp = (TierProperty) tiers.elementAt(i);
      Vector types = tp.getFeatureProperties();
      int type_count = types.size();
      for (int j = 0; j < type_count; j++) {
	FeatureProperty fp = (FeatureProperty) types.elementAt(j);
        String annot_type = fp.getAnnotType();
        if (annot_type != null) {
          // make sure this string is one of the supported
          // annotation types
          boolean matched = false;
          // first see if its the default type("gene")
          matched = annot_type.equals(FeatureProperty.getDefaultAnnotType());
          for (int k = 0; k < annot_types.size() && !matched; k++) {
            FeatureProperty annot_prop
              = (FeatureProperty) annot_types.elementAt(k);
            matched = annot_prop.getDisplayType().equalsIgnoreCase(annot_type);
          }
          if (!matched) {
            String defaultType = FeatureProperty.getDefaultAnnotType();
            String m = "feature type " + fp.getDisplayType()+" has non-existent "+
              "annot_type " + annot_type +" in tiers file " + filename+" Setting to "
              + "default annot type \""+defaultType+"\"";
            logger.warn(m);
            fp.setAnnotType(defaultType); 
          }
        }
      }
    }
  }

}
