package apollo.gui;

import apollo.config.Config;
import apollo.datamodel.*;

import java.util.*;

import org.apache.log4j.*;

/** This class is mainly for EvidencePanel and its SetDetailPanel, but there is handy methods
    that could be widely used. Perhaps it belongs in util? I think it does belong in util */

public class DetailInfo {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DetailInfo.class);

  public static final int GENOMIC_LENGTH = 0;
  public static final int MATCH_LENGTH = 1;
  public static final int GENOMIC_RANGE = 2;
  public static final int MATCH_RANGE = 3;
  public static final int SCORE = 4;
  public static final int ID = 5;
  public static final int TYPE = 6;
  public static final int TIER = 7;
  public static final int NAME = 8;
  public static final int EVIDENCE = 9;
  public static final int GENOMIC_SEQUENCE = 10;
  public static final int MATCH_SEQUENCE = 11;
  public static final int START = 12;
  public static final int END = 13;
  public static final int LOW = 14;
  public static final int HIGH = 15;
  public static final int STRAND = 16;
  public static final int Type = 17;
  public static final int Range = 18;
  public static final int PHASE = 19;
  public static final int BIOTYPE = 20;
  public static final int END_PHASE = 21;
  public static final int CODING_PROPERTIES = 22;

  private static final Hashtable stringMapping = new Hashtable();
  private static final Hashtable prettyNameMapping = new Hashtable();
  static {
    stringMapping.put("GENOMIC_LENGTH", new Integer(GENOMIC_LENGTH));
    stringMapping.put("MATCH_LENGTH", new Integer(MATCH_LENGTH));
    stringMapping.put("GENOMIC_RANGE", new Integer(GENOMIC_RANGE));
    stringMapping.put("MATCH_RANGE", new Integer(MATCH_RANGE));
    stringMapping.put("SCORE", new Integer(SCORE));
    stringMapping.put("TYPE", new Integer(TYPE));
    stringMapping.put("BIOTYPE", new Integer(BIOTYPE));
    stringMapping.put("TIER", new Integer(TIER));
    stringMapping.put("NAME", new Integer(NAME));
    stringMapping.put("ID", new Integer(ID));
    stringMapping.put("EVIDENCE", new Integer(EVIDENCE));
    stringMapping.put("GENOMIC_SEQUENCE", new Integer(GENOMIC_SEQUENCE));
    stringMapping.put("START", new Integer(START));
    stringMapping.put("END", new Integer(END));
    stringMapping.put("LOW", new Integer(LOW));
    stringMapping.put("HIGH", new Integer(HIGH));
    stringMapping.put("STRAND", new Integer(STRAND));
    stringMapping.put("Type", new Integer(Type));
    stringMapping.put("Range", new Integer(Range));
    stringMapping.put("PHASE", new Integer(PHASE));
    stringMapping.put("END_PHASE", new Integer(END_PHASE));
    stringMapping.put("CODING_PROPERTIES", new Integer(CODING_PROPERTIES));

    prettyNameMapping.put(new Integer(GENOMIC_LENGTH), "Genomic Length");
    prettyNameMapping.put(new Integer(MATCH_LENGTH), "Match Length");
    prettyNameMapping.put(new Integer(GENOMIC_RANGE), "Genomic Range");
    prettyNameMapping.put(new Integer(MATCH_RANGE), "Match Range");
    prettyNameMapping.put(new Integer(SCORE), "Score");
    prettyNameMapping.put(new Integer(TYPE), "Type");
    prettyNameMapping.put(new Integer(BIOTYPE),"Type");
    prettyNameMapping.put(new Integer(TIER), "Tier");
    prettyNameMapping.put(new Integer(NAME), "Name");
    prettyNameMapping.put(new Integer(ID), "Id");
    prettyNameMapping.put(new Integer(EVIDENCE), "Evidence");
    prettyNameMapping.put(new Integer(GENOMIC_SEQUENCE),
                          "Genomic Sequence");
    prettyNameMapping.put(new Integer(MATCH_SEQUENCE), "Match Sequence");
    prettyNameMapping.put(new Integer(START), "Start");
    prettyNameMapping.put(new Integer(END), "End");
    prettyNameMapping.put(new Integer(LOW), "Low");
    prettyNameMapping.put(new Integer(HIGH), "High");
    prettyNameMapping.put(new Integer(STRAND), "Strand");
    prettyNameMapping.put(new Integer(Type), "Type");
    prettyNameMapping.put(new Integer(Range), "Range");
    prettyNameMapping.put(new Integer(PHASE), "Phase");
    prettyNameMapping.put(new Integer(END_PHASE), "End Phase");
    prettyNameMapping.put(new Integer(CODING_PROPERTIES), "Coding Props.");
  }

  public static Integer getPropertyForString(String name) {
    return (Integer) stringMapping.get(name);
  }

  public static Enumeration getAllProperties() {
    return stringMapping.elements();
  }

  public static Enumeration getAllPropertyStrings() {
    return stringMapping.keys();
  }

  public static String getPrettyNameForString(String key) {
    Integer value = (Integer) stringMapping.get(key);
    if (value == null)
      return key;
    else
      return (String) prettyNameMapping.get(value);
  }

  public static String getPrettyNameForProperty(Integer key) {
    return (String) prettyNameMapping.get(key);
  }

  public static Vector getPrettyNamesFromStrings(Vector values) {
    Vector out = new Vector();
    for(int i=0; i < values.size(); i++) {
      out.addElement(getPrettyNameForString((String) values.elementAt(i)));
    }
    return out;
  }

  public static Vector getRow(Vector def, SeqFeatureI feature) {
    Vector out = new Vector();
    for(int i=0; i < def.size(); i++) {
      String prop = (String) def.elementAt(i);
      out.addElement(getPropertyForFeature(prop, feature));
    }
    return out;
  }

  public static Object getPropertyForFeature(String prop,
                                             SeqFeatureI feature) {
    Integer val = (Integer) stringMapping.get(prop);
    if (val == null) {
      String property;
      if (prop.equals ("query_frame")) {
	// Why are we adding 1 to the frame?  In GAME XML, the
	// frame is already 1,2,3, not 0,1,2.  I don't see any
	// frames in Ensembl data.  --NH, 04/30/2003
	//        property = "" + (feature.getFrame() + 1);
        property = "" + feature.getFrame();
      } else {
        property = feature.getProperty(prop);
        if ((property == null || property.equals(""))
            && feature.getRefFeature() != null) {
          property = feature.getRefFeature().getProperty(prop);
        }
        if (property == null || property.equals("")) {
          Double score = (Double) getScore(feature, prop);
          property = ((score.doubleValue() != -1) ?
                      score.toString() : "");
        }
      }
      return property;
    }
    int command = val.intValue();
    switch (command) {
    case GENOMIC_LENGTH:
      return getGenomicLength(feature);
    case MATCH_LENGTH :
      return getMatchLength(feature);
    case GENOMIC_RANGE :
    case Range :
      return getGenomicRange(feature);
    case MATCH_RANGE :
      return getMatchRange(feature);
    case SCORE :
      return getScore(feature);
    case TYPE :
      return getType(feature);
    case BIOTYPE :
      return getBioType(feature);
    case TIER :
      return getTier(feature);
    case Type :
      return getPropertyType(feature);
    case NAME:
      return getName(feature);
    case ID:
      return getID(feature);
    case EVIDENCE:
      return getEvidence(feature);
    case GENOMIC_SEQUENCE:
      return getGenomicSequence(feature);
    case MATCH_SEQUENCE:
      return getMatchSequence(feature);
    case START:
      return getStart(feature);
    case END:
      return getEnd(feature);
    case LOW:
      return getLow(feature);
    case HIGH:
      return getHigh(feature);
    case STRAND:
      return getStrand(feature);
    case PHASE:
      return getPhase(feature);
    case END_PHASE:
      return getEndPhase(feature);
    case CODING_PROPERTIES:
      return getCodingProperties(feature);
    }
    return "";
  }

  public static Object getStrand(SeqFeatureI feature) {
    if (feature.getStrand() < 0)
      return "-";
    else
      return "+";
  }

  public static Object getLow(SeqFeatureI feature) {
    return new Integer((int) feature.getLow());
  }

  public static Object getHigh(SeqFeatureI feature) {
    return new Integer((int) feature.getHigh());
  }

  public static Object getStart(SeqFeatureI feature) {
    return new Integer((int) feature.getStart());
  }

  public static Object getEnd(SeqFeatureI feature) {
    return new Integer((int) feature.getEnd());
  }

  public static Object getPhase(SeqFeatureI feature) {
    return new Integer((int) feature.getPhase());
  }

  public static Object getEndPhase(SeqFeatureI feature) {
    return new Integer((int) feature.getEndPhase());
  }

  public static Object getCodingProperties(SeqFeatureI feature) {
    String codingPropString;

    switch (feature.getCodingProperties()) {
      case CodingPropertiesI.MIXED_5PRIME:
        codingPropString = "MIXED_5PRIME";
        break;
      case CodingPropertiesI.MIXED_3PRIME:
        codingPropString = "MIXED_3PRIME";
        break;
      case CodingPropertiesI.MIXED_BOTH:
        codingPropString = "MIXED_BOTH";
        break;
      case CodingPropertiesI.CODING:
        codingPropString = "CODING";
        break;
      case CodingPropertiesI.UTR_3PRIME:
        codingPropString = "UTR_3PRIME";
        break;
      case CodingPropertiesI.UTR_5PRIME:
        codingPropString = "UTR_5PRIME";
        break;
      case CodingPropertiesI.UNKNOWN:
      default:
        codingPropString = "UNKNOWN";
        break;
    }
    return codingPropString;
  }

  public static Object getEvidence(SeqFeatureI feature) {
    // Vector evidenceFeatures = new Vector();
    StringBuffer evidenceFeatures = new StringBuffer ();
    if (feature instanceof AnnotatedFeatureI) {
      AnnotatedFeatureI gi = (AnnotatedFeatureI) feature;
      EvidenceFinder finder = gi.getEvidenceFinder();
      if (finder != null) {
        Vector evidence = gi.getEvidence();
        for (int i=0; i < evidence.size(); i++) {
          Evidence e = (Evidence) evidence.elementAt(i);
          String evidenceId = e.getFeatureId();
          SeqFeatureI evidenceSF;
          if ((evidenceSF = finder.findEvidence(evidenceId)) != null) {
            // evidenceFeatures.addElement (evidenceSF);
            if (i > 0)
              evidenceFeatures.append (", ");
            evidenceFeatures.append (evidenceSF.toString());
          } else {
            logger.error ("Could not find evidence " + evidenceId);
            // evidenceFeatures.addElement (e);
            evidenceFeatures.append (e.toString());
          }
        }
      }
    }
    return evidenceFeatures;
  }

  public static Object getTranslationLength(SeqFeatureI feature) {
    if (feature instanceof Transcript)
      return new Integer(((Transcript) feature).translate().length());
    else
      return "";
  }

  public static String getTranslation(SeqFeatureI feature) {
    if (feature instanceof Transcript)
      return ((Transcript) feature).translate();
    else
      return "";
  }

  public static String getMatchSequence(SeqFeatureI feature) {
    SeqFeatureI hit = getHitFeature(feature);
    if (hit == null)
      return "";
    else
      return getGenomicSequence(hit);
  }

  public static String getGenomicSequence(SeqFeatureI feature) {
    return feature.getResidues();
  }

  public static String getID(SeqFeatureI feature) {
    return feature.getId();
  }

  /** The whole logic of this section has been put into the individual
   * getDisplayName() methods of the various classes 
   */
  public static String getName(SeqFeatureI feature) {
    return Config.getDisplayPrefs().getDisplayName(feature);
  }

  public static Object getScore(SeqFeatureI feature) {
    return new Double(feature.getScore());
  }

  private static Object getScore(SeqFeatureI feature, String scorename) {
    if (scorename.equals("score") && feature.getScore(scorename) == -1)
      return new Double (feature.getScore ("total_score"));
    else
      return new Double(feature.getScore(scorename));
  }

  public static Object getGenomicLength(SeqFeatureI feature) {
    return new Integer((int) feature.length());
  }

  public static Object getMatchLength(SeqFeatureI feature) {
    SeqFeatureI hit = getHitFeature(feature);
    if (hit == null)
      return "";
    else
      return getGenomicLength(hit);
  }

  public static Object getGenomicRange(SeqFeatureI feature) {
    return new DetailRange((int) feature.getStart(),
                           (int) feature.getEnd());
  }

  public static Object getMatchRange(SeqFeatureI feature) {
    SeqFeatureI hit = getHitFeature(feature);
    if (hit == null)
      return "";
    else
      return getGenomicRange(hit);
  }

  public static Object getType(SeqFeatureI feature) {
    return feature.getFeatureType();
  }
  public static String getBioType(SeqFeatureI feature) {
    return feature.getTopLevelType();
  }

  public static Object getTier(SeqFeatureI feature) {
    apollo.config.TierProperty property = null;
    SeqFeatureI sf = feature;
    while (property == null && sf != null) {
      property = Config.getPropertyScheme().getTierProperty(sf.getFeatureType(), 
                                                            false);
      sf = sf.getRefFeature();
    }
    return (property != null ? property.getLabel() : "??");
  }

  /** Changed return from Object to String since FeatureProperty.getDisplayType 
      is a String - rename this getVisualType? */
  public static String getPropertyType(SeqFeatureI feature) {
    String biotype = feature.getTopLevelType();
    if (biotype==null) 
      return null; 
    apollo.config.FeatureProperty property =
      Config.getPropertyScheme().getFeatureProperty(biotype);
    String type = property.getDisplayType();
    // But transcripts are in the gene visual type. 
    return type;
  }

  protected static SeqFeatureI getHitFeature(SeqFeatureI feature) {
    if (feature instanceof FeaturePairI)
      return ((FeaturePairI) feature).getHitFeature();
    else
      return null;
  }

}

class DetailRange implements org.bdgp.util.Range {
  int low;
  int high;
  public DetailRange(int low, int high) {
    this.low = low;
    this.high = high;
  }

  public int getLow() {
    return low;
  }

  public int getHigh() {
    return high;
  }

  public String toString() {
    return low+"-"+high;
  }
}
