package apollo.dataadapter.gff3;

import apollo.dataadapter.NonFatalDataAdapterException;

import apollo.datamodel.*;

import apollo.util.OBOParser;
import apollo.util.Pair;

import java.util.*;

import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.LinkedObject;
import org.obo.datamodel.Link;

/**This class represents a GFF3 entry
 * 
 * @author elee
 *
 */

public class GFF3Entry
{

  private static Set<String> oneLevelAnnots;
  private static Set<String> threeLevelAnnots;
  private static int tmpIdCounter;
  static {
    oneLevelAnnots = new TreeSet<String>(Arrays.asList(new String[] {
        "promoter", "insertion_site", "transposable_element",
        "transposable_element_insertion_site", "remark", "repeat_region",
        "substitution"
    }));
    threeLevelAnnots = new TreeSet<String>(Arrays.asList(new String[] {
        "gene", "pseudogene", "tRNA", "snRNA", "snoRNA", "ncRNA", "rRNA",
        "miRNA"
    }));
    tmpIdCounter = 0;
  }

  /**expected number of fields
   */
  public final static int NUM_FIELDS = 9;

  //instance variables
  private String refId;
  private String src;
  private String type;
  private int start;
  private int end;
  private double score;
  private int strand;
  private int phase;
  private String id;
  private String name;
  private Map<String, List<String> > attrs;
  private boolean scoreSet;
  private boolean phaseSet;
  private SeqFeatureI feat;

  /**Constructs a GFF3Entry
   *
   *  @param data - tab delimited data
   */
  public GFF3Entry(String data)
  {
    init();
    parseData(data);
  }

  /**Constructs a GFF3Entry
   *
   *  @param feat - SeqFeatureI object
   */
  public GFF3Entry(SeqFeatureI feat)
  {
    init();
    parseData(feat);
  }

  /**Get the reference sequence ID
   *
   *  @return refId - reference sequence ID
   */
  public String getReferenceId()
  {
    return refId;
  }

  /**Get the source for the feature
   *
   *  @return source for the feature
   */
  public String getSource()
  {
    return src;
  }

  /**Set the source for the feature
   *
   *  @param src - source for the feature
   */
  public void setSource(String src)
  {
    this.src = src;
  }

  /**Get the type of feature
   *
   *  @return type of feature
   */
  public String getType()
  {
    return type;
  }

  /**Set the type of feature
   *
   *  @param type - type of feature
   */
  public void setType(String type)
  {
    this.type = type;
  }

  /**Get the start of the feature on the reference sequence
   *
   *  @return start of the feature
   */
  public int getStart()
  {
    return start;
  }

  /**Set the start of the feature on the reference sequence
   *
   *  @param start - start of the feature
   */
  public void setStart(int start)
  {
    this.start = start;
  }

  /**Get the end of the feature on the reference sequence
   *
   *  @return end of feature
   */
  public int getEnd()
  {
    return end;
  }

  /**Set the end of the feature on the reference sequence
   *
   *  @param end - end of feature
   */
  public void setEnd(int end)
  {
    this.end = end;
  }

  /**Get the strand of the feature on the reference sequence
   *
   *  @return strand of feature (-1: minus, 1: plus, 0: unknown)
   */
  public int getStrand()
  {
    return strand;
  }

  /**Get the feature score
   *
   *  @return feature score
   */
  public double getScore()
  {
    /*
        if (!isScoreSet()) {
            throw new NonFatalDataAdapterException("This feature has no score");
        }
     */
    return score;
  }

  /**Set the feature score
   *
   *  @param score - feature score
   */
  public void setScore(double score)
  {
    this.score = score;
    scoreSet = true;
  }

  /**Get the feature phase
   *
   *  @return feature phase
   */
  public int getPhase()
  {
    /*
        if (!isPhaseSet()) {
            throw new NonFatalDataAdapterException("This feature has no phase");
        }
     */
    return phase;
  }

  /**Set the feature phase
   *
   *  @param phase - feature phase
   */
  public void setPhase(int phase)
  {
    this.phase = phase;
    phaseSet = true;
  }

  /**Checks whether the score has been set for this feature
   *
   *  @return whether the score has been set for the feature
   */
  public boolean isScoreSet()
  {
    return scoreSet;
  }

  /**Checks whether the phase has been set for this feature
   *
   *  @return whether the phase has been set for the feature
   */
  public boolean isPhaseSet()
  {
    return phaseSet;
  }

  /**Get a list of attribute values for a given attribute name
   *
   *  @param attrName - name of attribute
   *  @return list of attribute values for given attribute (null if does not
   *          exist)
   */
  public List<String> getAttributeValues(String attrName)
  {
    return attrs.get(attrName);
  }

  /**Add an attribute
   *
   *  @param name - attribute name
   *  @param value - attribute value
   */
  public void addAttributeValue(String name, String value)
  {
    List<String> attr = attrs.get(name);
    if (attr == null) {
      attr = new LinkedList<String>();
      attrs.put(name, attr);
    }
    attr.add(value);
  }

  /**Get all attributes
   *
   *  @return all attributes for this entry
   */
  public Map<String, List<String> > getAttributes()
  {
    return attrs;
  }

  /**Set the feature ID
   *
   *  @param id - feature id
   */
  public void setId(String id)
  {
    this.id = id;
  }

  /**Get the feature ID
   *
   *  @return feature id (null if not existent)
   */
  public String getId()
  {
    if (id == null) {
      List<String> ids = getAttributeValues("ID");
      if (ids == null) {
        return null;
      }
      id = ids.get(0);
    }
    return id;
  }

  /**Get the feature name
   *
   *  @return feature name (null if not existent)
   */
  public String getName()
  {
    if (name == null) {
      List<String> names = getAttributeValues("Name");
      if (names == null) {
        return null;
      }
      name = names.get(0).replaceAll("\\s+", "_");
    }
    return name;
  }

  /**Check if the feature name is set
   * 
   * @return whether the name is set
   */
  public boolean isNameSet()
  {
    return getName() != null;
  }

  /**Create the implementing class of AnnotatedFeatureI given the type
   *
   *  @param oboParser - OBO parser to be used for mapping SO types to Apollo datamodel types
   *  @return a newly created AnnotatedFeatureI instance
   */
  public SeqFeatureI createFeature(OBOParser oboParser) throws ClassNotFoundException,
  InstantiationException, IllegalAccessException
  {
    if (feat != null) {
      return feat;
    }
    String javaClassName = getJavaClassName(oboParser);
    feat = javaClassName != null ? (SeqFeatureI)Class.forName(javaClassName).newInstance() :
      new AnnotatedFeature();
      //new SeqFeature();

    //Can't really handle match_part objects in a general way for dynamic class loading since
    //the ontologies have no way of knowing whether it should be a FeaturePairI or a SeqFeatureI.
    //Also, can't dynamically instantiate an object that does not have a default constructor
    //(FeaturePair)

    if (getType().equals("match_part")) {
      List<String> target = getAttributeValues("Target");
      if (target != null) {
        SeqFeature queryFeat =
          new SeqFeature(getStart(), getEnd(), getType(),
              getStrand());
        String []tokens = target.get(0).split("\\s+");
        SeqFeature hitFeat =
          new SeqFeature(Integer.parseInt(tokens[1]),
              Integer.parseInt(tokens[2]),
              getType(),
              tokens.length >= 4 ? stringToStrand(tokens[3]) : 1);
        hitFeat.addProperty("refId", tokens[0]);
        feat = new FeaturePair(queryFeat, hitFeat);
        List<String> cigar = getAttributeValues("Gap");
        if (cigar != null) {
          ((FeaturePair)feat).setCigar(cigar.get(0));
        }
      }
    }        

    //populate annotation's general fields
    if (feat != null) {
      feat.setLow(getStart());
      feat.setHigh(getEnd());
      feat.setStrand(getStrand());
      feat.setProgramName(getSource());
      if (isScoreSet()) {
        feat.setScore(getScore());
      }
      if (isPhaseSet()) {
        feat.setPhase(getPhase());
      }

      //setting feature type - for evidence, use the source as the type
      if (isAnnotation()) {
        feat.setFeatureType(getType());
      }
      else {
        feat.setFeatureType(getSource());
      }

      feat.setId(getId());
      if (isNameSet()) {
        feat.setName(getName());
      }
      if (isAnnotation()) {
        //add aliases
        List<String> aliases = getAttributeValues("Alias");
        if (aliases != null) {
          for (String syn : aliases) {
            ((AnnotatedFeatureI)feat).addSynonym(syn);
          }
        }
        //add comments
        List<String> comments = getAttributeValues("Note");
        if (comments != null) {
          for (String comment : comments) {
            Comment c = new Comment();
            c.setPerson("N/A");
            c.setText(comment);
            ((AnnotatedFeatureI)feat).addComment(c);
          }
        }
      }
      //add dbxrefs
      List<String> dbxrefs = getAttributeValues("Dbxref");
      if (dbxrefs != null) {
        addDbXrefs(dbxrefs, feat, false);
      }
      //add ontology_terms
      List<String> ontologyTerms = getAttributeValues("Ontology_term");
      if (ontologyTerms != null) {
        addDbXrefs(ontologyTerms, feat, true);
      }
    }
    return feat;
  }

  /**Convert this entry object to a GFF3 tab delimited entry
   *
   *  @return GFF3 tab delimited format
   */
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getReferenceId() + "\t");
    sb.append(getSource() + "\t");
    sb.append(getType() + "\t");
    sb.append(getStart() + "\t");
    sb.append(getEnd() + "\t");
    sb.append((isScoreSet() ? getScore() : ".") + "\t");
    sb.append(strandToString(getStrand()) + "\t");
    sb.append((isPhaseSet() ? getPhase() : ".") + "\t");
    sb.append("ID=" + getId());
    sb.append(";Name=" + getName());
    for (Map.Entry<String, List<String> > entry : attrs.entrySet()) {
      if (entry.getKey().equals("ID") || entry.getKey().equals("Name")) {
        continue;
      }
      sb.append(";");
      sb.append(entry.getKey() + "=");
      List<String> values = entry.getValue();
      for (int i = 0; i < values.size(); ++i) {
        if (i > 0) {
          sb.append(",");
        }
        sb.append(values.get(i));
      }
    }
    return sb.toString();
  }

  /** Check to see if entry is for a one-level annotation
   *
   *   @return is this entry for an one-level annotation
   */
  public boolean isOneLevelAnnotation()
  {
    return oneLevelAnnots.contains(getType());
  }

  /** Check to see if entry is for a three-level annotation
   *
   *   @return is this entry for an three-level annotation
   */
  public boolean isThreeLevelAnnotation()
  {
    return threeLevelAnnots.contains(getType());
  }

  /** Check whether this feature is annotation (evidence otherwise)
   *
   *   @return whether this feature is annotation
   */
  public boolean isAnnotation()
  {
    if (feat != null) {
      return feat instanceof AnnotatedFeature;
    }
    return false;
  }

  /**Init data
   */
  private void init()
  {
    attrs = new TreeMap<String, List<String> >();
    score = 0;
    phase = 0;
    scoreSet = false;
    phaseSet = false;
  }

  /**Parse tab delimited data
   */
  private void parseData(String data)
  {
    String []tokens = data.split("\t");
    if (tokens.length != NUM_FIELDS) {
      throw new
      NonFatalDataAdapterException("Invalid number of columns");
    }
    refId = tokens[0];
    src = tokens[1];
    type = tokens[2];
    start = Integer.parseInt(tokens[3]);
    end = Integer.parseInt(tokens[4]);

    if (!tokens[5].equals(".")) {
      setScore(Double.parseDouble(tokens[5]));
    }

    //strand parsing
    strand = stringToStrand(tokens[6]);

    if (!tokens[7].equals(".")) {
      setPhase(Integer.parseInt(tokens[7]));
    }

    //parse attributes
    for (String attr : tokens[8].split(";")) {
      String []keyValue = attr.split("=");
      if (keyValue.length != 2) {
        continue;
      }
      for (String val : keyValue[1].split(",")) {
        String key = keyValue[0].trim();
        addAttributeValue(keyValue[0].trim(), val.trim());
      }
    }
    if (getId() == null) {
      setTmpId();
    }
  }

  /**Parse SeqFeatureI data
   */
  private void parseData(SeqFeatureI feat)
  {
    refId = feat.getRefSequence().getName();
    src = feat.getProgramName() != null && feat.getProgramName().length() > 0 ?
        feat.getProgramName() : ".";
    type = feat.getFeatureType();
    start = feat.getLow();
    end = feat.getHigh();
    score = feat.getScore();
    strand = feat.getStrand();
    phase = feat.getPhase();
    id = feat.getId();
    name = feat.getName();
    //populate dbxrefs
    for (Object o : feat.getDbXrefs()) {
      DbXref dbxref = (DbXref)o;
      String key;
      String value;
      //if dbxref is an ontology ID or it contains ":", in which case 
      //we assume it to be an ontology ID, don't prepend the database
      if (dbxref.isOntology() || dbxref.getIdValue().contains(":")) {
        key = "Ontology_term";
        value = dbxref.getIdValue();
      }
      else {
        key = "Dbxref";
        value = dbxref.getDbName() + ":" + dbxref.getIdValue();
      }
      addAttributeValue(key, value);
    }
    if (feat instanceof AnnotatedFeatureI) {
      AnnotatedFeatureI annot = (AnnotatedFeatureI)feat;
      //populate aliases
      for (Object o : annot.getSynonyms()) {
        Synonym syn = (Synonym)o;
        addAttributeValue("Alias", syn.getName());
      }
      //store comments
      for (Object o : annot.getComments()) {
        Comment c = (Comment)o;
        addAttributeValue("Note", c.getText());
      }
    }
    else if (feat instanceof FeaturePairI) {
      FeaturePairI fp = (FeaturePairI)feat;
      setScore(fp.getScore());
      SeqFeatureI hit = fp.getHitFeature();
      SequenceI hitSeq = hit.getHitFeature().getRefSequence();
      String hitRefId = hitSeq != null ? hitSeq.getName() :
        hit.getProperty("refId");
      if (hitRefId == null || hitRefId.length() == 0) {
        GFF3Adapter.logger.warn("No target id for hit on " +
            fp.getQueryFeature().getName() + " [" +
            hit.getLow() + ", " + hit.getHigh() + "]");
        return;
      }
      addAttributeValue("Target", hitRefId + " " + hit.getLow() + " " +
          hit.getHigh() + " " + strandToString(hit.getStrand()));
      String cigar = fp.getCigar();
      if (cigar != null && cigar.length() > 0) {
        addAttributeValue("Gap", cigar);
      }
    }
  }

  /** Convert the strand from string to int
   */
  private int stringToStrand(String strand)
  {
    if (strand.equals("+")) {
      return 1;
    }
    else if (strand.equals("-")) {
      return -1;
    }
    return 0;
  }

  /** Convert the strand from int to string
   */
  private String strandToString(int strand)
  {
    if (strand == 1) {
      return "+";
    }
    else if (strand == -1) {
      return "-";
    }
    return ".";
  }

  /** Add to the feature's DbXrefs
   */
  private void addDbXrefs(List<String> dbxrefs, SeqFeatureI feat,
      boolean isOntology)
  {
    for (String dbxref : dbxrefs) {
      int idx = dbxref.indexOf(':');
      if (idx == -1) {
        throw new NonFatalDataAdapterException
        ("Malformatted dbxref: " + dbxref);
      }
      /*
      feat.addDbXref(new DbXref("id", dbxref.substring(idx + 1),
          dbxref.substring(0, idx), isOntology));
       */
      feat.addDbXref(new DbXref("id",
          isOntology ? dbxref : dbxref.substring(idx + 1),
          dbxref.substring(0, idx), isOntology));
    }
  }

  private String getJavaClassName(OBOParser oboParser)
  {
    Pair<IdentifiedObject, String> io = oboParser.getTermByName(getType());
    String javaClassName = null;
    LinkedObject node = (LinkedObject)io.getFirst();
    while (javaClassName == null && node != null) {
      LinkedObject parent = null;
      for (Link l : node.getParents()) {
        String pname = l.getType().getName();
        if (pname.equals("uses_java_class")) {
          javaClassName = l.getParent().getName();
        }
        else if (pname.equals("is_a")) {
          if (l.getParent() instanceof LinkedObject) {
            parent = (LinkedObject)l.getParent();
          }
        }
      }
      node = parent;
    }
    return javaClassName;
  }
  
  private void setTmpId()
  {
    setId(String.format("ID%06d", ++tmpIdCounter));
  }
}
