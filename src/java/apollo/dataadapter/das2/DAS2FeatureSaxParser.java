package apollo.dataadapter.das2;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.log4j.*;

import org.xml.sax.*;

import apollo.datamodel.*;

/** Parser that reads features from das2xml and stores them in Apollo datamodels
 *  (the top level of which is a CurationSet).
 *  Here's an example of das2xml as of 2/9/2006:
 * <FEATURELIST
 *    xmlns="http://www.biodas.org/ns/das/2.00"
 *    xmlns:xlink="http://www.w3.org/1999/xlink"
 *    xml:base="http://127.0.0.1:9021/das2/genome/D_melanogaster_Apr_2004/feature" >
 *   <FEATURE id="feature/hit12" type="est-alignment-hsp"
 *            created="2001-12-15T22:43:36"
 *            modified="2004-09-26T21:10:15" >
 *     <LOC pos="region/residues/Chr3/1201:1400:1" />
 *     <PART id="feature/hit12.hsp1" />
 *     <PART id="feature/hit12.hsp2" />
 *     <ALIGN target_id="feature/yk12391" range="200:299" />
 *     <PROP key="icon" href="images/est2.png" />
 *     <PROP key="est2genomescore" value="180" />
 *   </FEATURE>
 *   <FEATURE id="feature/hit12.hsp1" type="est-alignment-hsp" >
 *     <LOC pos="region/residues/Chr3/1201:1250:-1" />
 *     <PARENT id="feature/hit12" />
 *     <PROP key="est2genomescore" value="180" />
 *   </FEATURE>
 *  <FEATURE id="feature/hit12.hsp2" type="est-alignment-hsp" >
 *    <LOC pos="region/residues/Chr3/1351:1400:1" />
 *    <PARENT id="feature/hit12" />
 *    <PROP key="est2genomescore" value="120" />
 *  </FEATURE>
 * </FEATURELIST>
 *
 * Note that this class has a "main" so you can test DAS2XML parsing from the
 * command line by specifying the name of a DAS2XML file, e.g.
 * java -ms50M -Xmx500M -classpath /users/nomi/apollo/src/java/classfiles:/users/nomi/apollo/jars/jakarta-oro-2.0.6.jar:/users/nomi/apollo/jars/xerces.jar:/users/nomi/apollo/data -DAPOLLO_ROOT=/users/nomi/apollo apollo.dataadapter.das2.DAS2Adapter /users/nomi/apollo/data/features.das2xml
 *
 * 3/21/2006: Updated to handle (in a superficial way) simple v300 DAS2XML:
 * <FEATURES xmlns="http://biodas.org/documents/das2">
 *   <FEATURE
 *     uri="feature/Affymetrix_Yeast_2:1773363_x_at"
 *     type="type/SO:PCR_product">    
 *       <LOC segment="segment/chrI" range="15:61:1"/>    
 *       <LOC segment="segment/chrI" range="230152:230206:-1"/>    
 *   </FEATURE>
 **/

public class DAS2FeatureSaxParser extends org.xml.sax.helpers.DefaultHandler {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DAS2FeatureSaxParser.class);

  /**
   *  elements possible in DAS2 feature response
   */
  static final String FEATURELIST = "FEATURELIST";
  static final String FEATURES = "FEATURES";  // seems to have replaced FEATURELIST
  static final String FEATURE = "FEATURE";
  static final String LOC = "LOC";
  static final String XID = "XID";  // I haven't seen this in any of the examples
  static final String PART = "PART";
  static final String PROP = "PROP";
  static final String ALIGN = "ALIGN";
  static final String PARENT = "PARENT";
  static final String STYLE = "STYLE";
  static final String BOX = "BOX";  // element under STYLE
  static final String LABEL = "LABEL";  // element under STYLE
  static final String LINE = "LINE";  // element under STYLE
  static final String XHTML = "XHTML";


  // Some of the elements and attributes are obsolete and should be removed

  /**
   *  attributes possible in DAS2 feature response (v200)
   */
  static final String ID = "id";   // in <FEATURE>, <PART>
  static final String TYPE = "type";  // in <FEATURE>
  static final String TYPE_ID = "type_id";  // in <FEATURE> (has this replaced "type"?)
  static final String NAME = "name";  // in <FEATURE>
  // parent has moved from attribute of FEATURE to subelement of FEATURE
  //  static final String PARENT = "parent";  // in <FEATURE>
  static final String CREATED = "created";  // in <FEATURE>
  static final String MODIFIED = "modified";  // in <FEATURE>
  static final String DOC_HREF = "doc_href";  // in <FEATURE>
  static final String POS = "pos";  // in <LOC>
  static final String RANGE = "range";  // in <LOC>--I think this may have replaced "pos"
  static final String HREF = "href";  // in <XID> or <PROP>
  static final String PTYPE = "ptype";  // in <PROP> (obsolete?)
  static final String MIME_TYPE = "mime_type";  // in <PROP>
  static final String CONTENT_ENCODING = "content_encoding";  // in <PROP>
  static final String TGT = "tgt";  // in <ALIGN>
  static final String TARGET_ID = "target_id";  // in <ALIGN>--I think this has replaced "tgt"
  static final String GAP = "gap";  // in <ALIGN>
  static final String KEY = "key";  // in <PROP>
  static final String VALUE = "value";  // in <PROP>

  /**
   *  built-in ptype attribute values possible for <PROP> element in DAS2 feature response
   *  1/2006: are these obsolete?
   */
  static final String NOTE_PROP = "das:note";
  static final String ALIAS_PROP = "das:alias";
  static final String PHASE_PROP = "das:phase";
  static final String SCORE_PROP = "das:score";

  static final Pattern range_splitter = Pattern.compile("/");
  static final Pattern interval_splitter = Pattern.compile(":");

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String current_elem = null;  // current element
  StringBuffer current_chars = null;
  Stack elemstack = new Stack();

  /** For current feature */
  String feat_id = null;
  String feat_type = null;
  String feat_name = null;
  String feat_parent_id = null;
  //  String feat_created = null;
  //  String feat_modified = null;
  //  String feat_doc_href = null;
  String feat_prop_key = null;
  String feat_prop_value = "";
  int min = -1;
  int max = -999;
  int strand = 0;

  /**  list of SeqSpans specifying feature locations */
  List feat_locs = new ArrayList();
  List feat_xids = new ArrayList();
  /**
   *  map of child feature id to either:
   *      itself  (if child feature not parsed yet), or
   *      child feature object (if child feature already parsed)
   */
  Map feat_parts = new LinkedHashMap();
  List feat_aligns = new ArrayList();
  Map feat_props = null;

  /**
   *  lists for builtin feature properties
   *  not using yet (but clearing in clearFeature() just in case)
   */
  List feat_notes = new ArrayList();
  List feat_aliass = new ArrayList();
  List feat_phases = new ArrayList();
  List feat_scores = new ArrayList();

  /**
   *  List of feature jsyms resulting from parse
   */
  List result_syms = null;

  /**
   *  Need mapping so can connect parents and children after sym has already been created
   */
  Map id2sym = new HashMap();

  /**
   *  Need mapping of parent sym to map of child ids to connect parents and children
   */
  Map parent2parts = new HashMap();

  /** These need to be global to allow easy layering of new data */
  StrandedFeatureSetI analyses = null;
  Hashtable all_analyses = null;
  String chromosome = null;


  public DAS2FeatureSaxParser() {}

  /** Populates curation set */
  public void parse(BufferedInputStream isrc, CurationSet curation) 
    //    throws IOException {
    throws Exception {
    //    try {
      XMLReader reader = new org.apache.xerces.parsers.SAXParser();
      //      reader.setFeature("http://xml.org/sax/features/string-interning", true);
      reader.setFeature("http://xml.org/sax/features/validation", false);
      reader.setFeature("http://apache.org/xml/features/validation/dynamic", false);
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setContentHandler(this);
      initCurationSet(curation);
      // Parser sends events that call some of the other methods
      reader.parse(new InputSource(isrc));
      logger.debug("done parsing DAS2XML input");
      curation.setResults(analyses);  // analyses was filled in as results were parsed
      // Set up empty annotations for curation--we run into trouble otherwise
      StrandedFeatureSetI annotations = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      annotations.setName("Annotations");
      annotations.setFeatureType("Annotation");
      curation.setAnnots(annotations);

      // Commenting out catch while debugging so we can see where error happened
      //    }
    //    catch (Exception ex) {
    //      System.out.println("DAS2FeatureSaxParser: error while parsing: " + ex.getMessage());
    //      ex.printStackTrace();
    //    }
  }

  /**
   *  implementing sax content handler interface
   */
  public void startDocument() {
    logger.debug("startDocument() called");
  }

  /**
   *  implementing sax content handler interface
   */
  public void endDocument() {
    logger.debug("endDocument() called");
  }

  /**
   *  implementing sax content handler interface
   */
  public void startElement(String uri, String name, String qname, Attributes atts) {
    elemstack.push(current_elem);
    current_elem = name.intern();

    logger.trace("DAS2FeatureSaxParser.startElement: element = " + name);

    // Top level element
    if (current_elem == FEATURELIST || current_elem == FEATURES) {
      // !! Extract xmlns and xml:base?  Use to set organism field in curation set?
    }
    // v200: <FEATURE id="feature/hit12" type="est-alignment-hsp"
    // v300: <FEATURE uri="feature/Affymetrix_Yeast_2:1773363_x_at" type="type/SO:PCR_product">    
    else if (current_elem == FEATURE) {
      feat_id = atts.getValue(ID);  // v200
      if (feat_id == null || feat_id.equals(""))
        feat_id = atts.getValue("uri");  // v300
      // Strip out the initial "feature/", if any
      feat_id = stripPrefix(feat_id);
      feat_type = atts.getValue(TYPE);
      feat_type = stripPrefix(feat_type);
      // !! type can include ontology name, e.g. "SO:telomere"
      // For now, leave as-is.  Eventually, these should be separated out and
      // saved separately.
      feat_name = atts.getValue("name");  // names don't seem to have a prefix
      logger.debug("got feature with id " + feat_id + ", type " + feat_type + ", name = " + name);
      //      feat_doc_href = atts.getValue("doc_href");

      SeqFeature feat = new SeqFeature();
      feat.setId(feat_id);
      if (feat_name != null)
        feat.setName(feat_name);
      else
        feat.setName(feat_id);
      feat.setFeatureType(feat_type);
      feat.setTopLevelType(feat_type); // ?

      // add created/modified/href as props
      feat.addProperty("created", atts.getValue("created"));
      feat.addProperty("modified", atts.getValue("modified"));

      // checking to make sure feature with same id doesn't already exist
      //   (ids _should_ be unique, but want to make sure)
      if (id2sym.get(feat_id) != null)
        logger.warn("duplicate feature id: " + feat_id);
      else
        id2sym.put(feat_id, feat);

      //      feat_parts.put(feat_id, feat); // ?
    }
    else if (current_elem == LOC)  {
      // v100: <LOC pos="region/chr4/26481:51939:-1" />
      // v200: <LOC id="segment/Chr3" range="1201:1400:1" />
      // v300: <LOC segment="segment/chrI" range="15:61:1"/>    
      SeqFeature span = (SeqFeature)id2sym.get(feat_id);
      if (span == null) {
        logger.debug("no pre-existing seqfeature for " + feat_id);
        span = new SeqFeature();
        span.setId(feat_id);
      }
      String span_id = atts.getValue("id");  // v200
      if (span_id == null || span_id.equals(""))
        span_id = atts.getValue("segment");  // v300
      String range = atts.getValue("range");
      logger.debug("got LOC id=" + span_id + ", range = " + range);
      setSpanRange(span, span_id, range);

      if (id2sym.get(feat_id) == null)  // we just created this span
        id2sym.put(feat_id, span);
    }
    else if (current_elem == XID) {
    }
    // Has parent changed in v300?
    else if (current_elem == PARENT) {
      if (feat_parent_id == null) { 
        feat_parent_id = atts.getValue(ID);
        feat_parent_id = stripPrefix(feat_parent_id);
        logger.trace("got parent id " + feat_parent_id); 
      }
      else {
        logger.warn("second parent (" + atts.getValue("id") + ") for feature, just using first one (" + feat_parent_id +")");
      }
    }
    // Has part changed in v300?
    else if (current_elem == PART) {
      String part_id = atts.getValue(ID);
      part_id = stripPrefix(part_id);
      logger.debug("got part id " + part_id);
      /*
       *  Use part_id to look for child sym already constructed and placed in id2sym hash
       *  If child sym found then map part_id to child sym in feat_parts
       *  If child sym not found then map part_id to itself, and swap in child sym later when it's created
       */
      SeqFeature child = (SeqFeature)id2sym.get(part_id);
      if (child == null) {
        logger.trace("no child was associated with part_id " + part_id);
	feat_parts.put(part_id, part_id);
      }
      else {
        logger.trace("child " + child.getId() + " is associated with part_id " + part_id);
	feat_parts.put(part_id, child);
      }
    }
    else if (current_elem == PROP) {
      // OLD: <PROP ptype="property/biological_process unknown"/>
      // NEW: <PROP key="est2genomescore" value="180" />
      String feat_prop_ptype = atts.getValue(PTYPE);
      if (feat_prop_ptype != null && !feat_prop_ptype.equals("")) {
        logger.warn("not handling old-fashioned PROP with ptype=" + feat_prop_ptype);
        return; // ?
      }
      feat_prop_key = atts.getValue(KEY);
      feat_prop_value = atts.getValue(VALUE);
      if (feat_prop_value == null || feat_prop_value.equals("")) {
        // property had an href rather than a value
        String url = atts.getValue(HREF);
        feat_prop_value = "href:" + url;
      }
      SeqFeature child = (SeqFeature)id2sym.get(feat_id);
      // what if it doesn't exist yet?  could that happen?
      if (child == null) {
        logger.error("got prop " + feat_prop_key + " = " + feat_prop_value + " for child " + feat_id + " that can't be found");
      }
      else {
        // Automatically makes a vector of values if there was already a value for this key
        child.addProperty(feat_prop_key, feat_prop_value);
        logger.trace("added property " + feat_prop_key + " = " + feat_prop_value + " to " + feat_id);
        // TEMPORARY
        if (feat_prop_key.indexOf("score") >= 0 && child.getScore() == 0) {
          try {
            double score = Double.parseDouble(feat_prop_value);
            child.setScore(score);
            logger.debug("set score = " + feat_prop_value + " for " + child.getName());
          } catch (Exception e) { }
        }
      }
    }
    else if (current_elem == ALIGN) {
      // !! Need to deal with this--extract subject ID, range, gap (?); construct feature pair
      logger.debug("got ALIGN record");
      String subject_id = atts.getValue("target_id");
      String subject_range = atts.getValue("range");
      addSubject(feat_id, subject_id, subject_range);
    }
    else if (current_elem == STYLE) {
      logger.debug("got STYLE record");
      // !! Style record can have elements under it such as BOX and LABEL
    }
    else if (current_elem == BOX) {
      logger.debug("got BOX record");
    }
    else if (current_elem == LABEL) {
      logger.debug("got LABEL record");
    }
    else if (current_elem == LINE) {
      logger.debug("got LINE record");
    }
    else if (current_elem == XHTML) {
      //    <XHTML xmlns="http://www.biodas.org/extension">
      //        <p>This is a tiny example of an <acronym title="Extensible HyperText 
      //        Markup Language">XHTML</acronym> document.</p>
      //   </XHTML>
      logger.debug("Got XHTML record");
      // !! What we want to do is slurp up the whole XHTML element, rather than
      // trying to interpret the elements nested in it.  I'm not sure how to do that,
      // because SAX sends events for the beginning of the "<P>" element, etc.
    }
    else {
      logger.warn("element not recognized: " + current_elem);
    }
  }

  /** Extract the min/max/strand from a position string like "region/chr20/87414:87804:1"
   *  and set the fields in span.
   *  For now, also use the chromosome to set it in the curation, if not already set.
   *  2/9/2006: Format of LOC record has changed to
   *   <LOC pos="segment/chrVII" range="364967:365435:-1"/>
   *  so this method is now passe and you can just call setSpanRange on the range part. */
  private void getPositionSpan(String position, SeqFeature span) {
    if (position == null) { return; }
    String[] fields = range_splitter.split(position);
    //    String seqid = fields[fields.length-2];
    String remainder = fields[fields.length-1];  // should be the "87414:87804:1" part
    setSpanRange(span, fields[1], remainder);
  }

  /** Works for older LOC style:
   * <LOC pos="region/chr4/26481:51939:-1" /> (with chrom and range already parsed out by getPositionSpan)
   * or newer LOC style:
   * <LOC id="residues/Chr3" range="1201:1400:1" /> */
  private void setSpanRange(SeqFeature span, String chrom, String range) {
    String[] subfields = interval_splitter.split(range);
    min = Integer.parseInt(subfields[0]);
    max = Integer.parseInt(subfields[1]);
    if (subfields.length >= 3) {
      try {
        strand = Integer.parseInt(subfields[2]);
      }
      catch (Exception e) {
        logger.error("couldn't parse integer strand from " + subfields[2] + "--loc string was " + range, e);
      }
    }

    span.setLow(min);
    span.setHigh(max);
    span.setStrand(strand);
    logger.debug("getPositionSpan: " + min + "-" + max + ":" + strand);

    this.chromosome = chrom;
    logger.debug("chrom = " + chromosome);
  }

  public void clearAll() {
    result_syms = null;
    id2sym.clear();
    clearFeature();
  }

  public void clearFeature() {
    logger.debug("clearing feature " + feat_id);
    feat_id = null;
    feat_type = null;
    feat_name = null;
    feat_parent_id = null;

    feat_locs.clear();
    feat_xids.clear();
    // making new feat_parts map because ref to old feat_parts map may be held for parent/child resolution
    feat_parts = new LinkedHashMap();
    feat_aligns.clear();

    feat_notes.clear();
    feat_aliass.clear();
    feat_phases.clear();
    feat_scores.clear();
    feat_props = null;
    feat_prop_key = null;
    feat_prop_value = "";
  }

  /**
   *  implementing sax content handler interface
   */
  public void endElement(String uri, String name, String qname)  {
    logger.trace("end element: " + name);

    // only two elements that need post-processing are  <FEATURE> and <PROP> ?
    //   other elements are either top <FEATURELIST> or have only attributes
    if (name.equals(FEATURE)) {
      logger.trace("endElement: got FEATURE--calling addFeature");
      addFeature();
      clearFeature();
    }
    else if (name.equals(PROP)) {
      if (feat_props == null) { feat_props = new HashMap(); }
      // need to process <PROP> elements after element is ended, because value may be in CDATA?
      // need to account for possibility that there are multiple property values of same ptype
      //    for such cases, make object that feat_prop_key maps to a List of the prop vals
      Object prev = feat_props.get(feat_prop_key);
      if (prev == null) {
	feat_props.put(feat_prop_key, feat_prop_value);
      }
      else if (prev instanceof List) {
	((List)prev).add(feat_prop_value);
      }
      else {
	List multivals = new ArrayList();
	multivals.add(prev);
	multivals.add(feat_prop_value);
	feat_props.put(feat_prop_key, feat_prop_value);
      }
      feat_prop_key = null;
      feat_prop_value = "";
    }
    // otherwise, don't worry about it

    //    prev_chars = false;
    //    current_chars = null;
    current_elem = (String)elemstack.pop();
  }

  /**
   *  implementing sax handler interface
   */
  public void characters(char[] ch, int start, int length) {
    if (current_elem == PROP) {
      // need to collect characters for property CDATA
      feat_prop_value += new String(ch, start, length);
    }
  }

  /** Some of the fields have a prefix saying what kind of thing they are, e.g.
   *  <FEATURE id = "feature/hit12"
   *           type_id = "type/est-alignment"
   * These prefixes are not interesting to Apollo, so we remove them. */
  private String stripPrefix(String s) {
    if (s != null && !s.equals("")) {
      // Strip out the initial "foo/", if any (index will be -1 if there isn't one
      // in which case s will be unchanged)
      s = s.substring(s.indexOf("/") + 1);
    }
    return s;
  }

  public void addFeature() {
    logger.debug("adding feature: " + feat_id);
    SeqFeatureI feat = (SeqFeatureI)id2sym.get(feat_id);
    if (feat == null) {
      logger.warn("addFeature: couldn't find " + feat_id + " in id2sym");
      return;
    }

    parent2parts.put(feat, feat_parts);

    SeqFeatureI parent = null;

    // If feat_parent_id is null, this is a top-level feature with no parents.
    // Go ahead and add it.
    if (feat_parent_id == null) {
      logger.debug("addFeature: feat " + feat + " is a top-level feature");
      parent = feat;
    }

    // This is a child--look for parent of this feature
    else { // if (id2sym.get(feat_parent_id) != null) {
      parent = (SeqFeatureI)id2sym.get(feat_parent_id);
      if (parent == null)  {
        logger.debug("no parent feature yet for id " + feat_parent_id);
        return;
      }
      feat.setRefFeature(parent);
      logger.debug("found parent " + feat_parent_id + " for child feature " + feat);
      // Child was in parent2parts hash as a string--replace with the actual seqfeature
      LinkedHashMap parent_parts = (LinkedHashMap)parent2parts.get(parent);
      if (parent_parts == null)  {
        logger.warn("no parent_parts found for parent " + feat_parent_id);
      }
      else {
        parent_parts.put(feat_id, feat);
      }
    }

    if (childrenReady(parent)) {
      logger.debug("all children are ready for parent " + parent);
      FeatureSet parentset = addChildren(parent);
      logger.debug("added " + parentset.getFeatures().size() + " children to parent " + parentset.getId());
      if (parentset.getFeatures().size() > 0) {
        // Set type of parent based on type of first child (?)
        parentset.setFeatureType(((SeqFeatureI)parentset.getFeatures().firstElement()).getFeatureType());
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Done making parent feature: " + parentset);
        logger.debug("Children: ");
        Vector spans = parentset.getFeatures();
        for (int i = 0; i < spans.size (); i++) {
          SeqFeature span = (SeqFeature) spans.elementAt (i);
          logger.debug("  Child " + i + ": " + span);
          //              writeProperties(span);
        }
      }
      // Add parentset to result set for curation
      addResultToAnalysis(parentset);
    }
    else 
      logger.debug("children not all ready for parent " + parent);

    //    // add feat to id2sym hash  // it should already be there
    //    id2sym.put(feat_id, feat);

     /**
      *  Add children _only_ if all children already have symmetries in feat_parts
      *  Otherwise need to wait till have all child syms, because need to be
      *     added to parent sym in order.
      *   add children if already parsed (should then be in id2sym hash);
      */
    // Does this happen? Remove?
     if (feat_parts.size() > 0) {
       logger.debug("feat_parts.size = " + feat_parts.size() + " for " + feat);
     }
//       if (childrenReady(feat)) {
//         // parent is the FeatureSet version of feat
//  	FeatureSet parent = addChildren(feat);
//         if (DEBUG) {
//           System.out.println("Done making parent feature: " + parent);
//           System.out.println("Children: ");
//           Vector spans = parent.getFeatures();
//           for (int i = 0; i < spans.size (); i++) {
//             SeqFeature span = (SeqFeature) spans.elementAt (i);
//             System.out.println("  Child " + i + ": " + span);
//           }
//         }
//         parent2parts.remove(feat);
//       }
//      }

//     // if no parent, then attach directly to AnnotatedBioSeq(s)  (get seqid(s) from location)
//     if (feat_parent_id == null) {
//       for (int i=0; i<loc_count; i++) {
// 	SeqSpan span = (SeqSpan)feat_locs.get(i);
// 	BioSeq seq = span.getBioSeq();
// 	MutableAnnotatedBioSeq aseq = seqgroup.getSeq(seq.getID());  // should be a SmartAnnotBioSeq
// 	if ((seq != null) && (aseq != null) && (seq == aseq)) {
// 	  // really want an extra level of annotation here (add as child to a Das2FeatureRequestSym),
// 	  //    but Das2FeatureRequestSym is not yet implemented
// 	  //
// 	  result_syms.add(feat);
// 	  if (add_annots_to_seq)  {
// 	    aseq.addAnnotation(feat);
// 	  }
// 	}
//       }
//     }

//     else {
//       MutableSeqSymmetry parent = (MutableSeqSymmetry)id2sym.get(feat_parent_id);
//       if (parent != null) {
// 	// add child to parent parts map
// 	LinkedHashMap parent_parts = (LinkedHashMap)parent2parts.get(parent);
//         if (parent_parts == null)  {
//           System.out.println("WARNING: no parent_parts found for parent, id=" + feat_parent_id);
//         }
// 	else  {
// 	  parent_parts.put(feat_id, feat);
// 	  if (childrenReady(parent)) {
// 	    addChildren(parent);
// 	    //	  parent2parts.remove(parent_sym);
// 	  }
// 	}
//       }
//     }
  }

  /** Returns true if all children of this feature have already been read in */
  protected boolean childrenReady(SeqFeatureI parent)  {
    if (parent == null) {
      logger.warn("childrenReady: parent is null!");
      return false;
    }
    LinkedHashMap parts = (LinkedHashMap)parent2parts.get(parent);
    Iterator citer = parts.values().iterator();
    boolean all_child_syms = true;
    while (citer.hasNext()) {
      Object val = citer.next();
      if (! (val instanceof SeqFeatureI)) {
        logger.debug("child " + val + " of " + parent.getId() + " not read yet");
	all_child_syms = false;
	break;
      }
    }
    if (all_child_syms == true) 
      logger.trace("all children ready for " + parent.getId());
    return all_child_syms;
  }

  /** Find all children of this parent and add them to the parent seqfeature */
  protected FeatureSet addChildren(SeqFeatureI parent_feat)  {
    // Promote the seqfeature to a featureset
    FeatureSet parent_set = new FeatureSet(parent_feat);
    copyProperties(parent_feat, parent_set);
    // get parts
    LinkedHashMap parts = (LinkedHashMap)parent2parts.get(parent_feat);
    Iterator citer = parts.entrySet().iterator();
    while (citer.hasNext()) {
      Map.Entry keyval = (Map.Entry)citer.next();
      String child_id = (String)keyval.getKey();
      SeqFeatureI child_sym = (SeqFeatureI)keyval.getValue();
      logger.trace("adding child " + child_sym.getId() + " to parent " + parent_set.getId());
      parent_set.addFeature(child_sym);
    }
    //    id2sym.remove(parent_feat);
    // this already happens in caller (?)
    //    parent2parts.remove(parent_feat);

    return parent_set;
  }

  private static void copyProperties(SeqFeatureI from, SeqFeatureI to) {
    Hashtable props = from.getProperties();
    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      Vector values = ((SeqFeature)from).getPropertyMulti(type);
      if (values == null)
        continue;
      for (int i = 0; i < values.size(); i++) {
        String value = (String) values.elementAt(i);
        to.addProperty(type, value);
      }
    }
  }

  private void initCurationSet(CurationSet curation) {
    if (curation == null)
      curation = new CurationSet();

    // Set up empty results (or retrieve them, if we're layering)
    StrandedFeatureSetI results = curation.getResults();
    if (results == null) {
      logger.debug("initializing results");
      results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      analyses = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
      all_analyses = new Hashtable();
    }
    else logger.debug("adding to existing results");
  }

  private void addResultToAnalysis(FeatureSetI result) {
    String prog = result.getProperty("program");
    String db = result.getProperty("sourcename");
    String date = result.getProperty("timeexecuted");
    String programversion = result.getProperty("programversion");
    String sourceversion = result.getProperty("sourceversion");

    if (db.equals("")) {
      // !! Should be DAS2 source--where to get that?
      db = "das2";  // for now
    }
    if (prog.equals("")) {
      prog = result.getFeatureType();
    }

    // Find or create FeatureSet for this analysis type (existing one will be
    // returned if found)
    FeatureSetI forward_analysis = 
      // 1 is forward strand
      initAnalysis (analyses, 1, prog, db, date, programversion, sourceversion, all_analyses);

    FeatureSetI reverse_analysis = 
      initAnalysis (analyses, -1, prog, db, date, programversion, sourceversion, all_analyses);

    if (result.getStrand() == 1) {
      forward_analysis.addFeature(result);
      logger.debug ("added result " + result + " to forward analysis " + forward_analysis.getName() + ", which has " + forward_analysis.size() + " features");
      if (!forward_analysis.hasFeatureType())
        forward_analysis.setFeatureType(result.getFeatureType());
    } else {
      logger.debug ("added result " + result + " to reverse analysis " + reverse_analysis.getName() + ", which has " + reverse_analysis.size() + " features");
      reverse_analysis.addFeature(result);
      if (!reverse_analysis.hasFeatureType()) {
        //        System.out.println ("For result " + result + ", setting analysis type to " + result.getType());  // DEL
        reverse_analysis.setFeatureType(result.getFeatureType());
      }
    }

    // Add in stranded analyses only if features were found on that strand;
    // otherwise it's scrapped (not added to all_analyses and analyses)
    boolean fwd = addAnalysisIfHasFeatures(forward_analysis,all_analyses,analyses);
    boolean rev = addAnalysisIfHasFeatures(reverse_analysis,all_analyses,analyses);
    // If we have analysis for both strands, set pointers to analysis "twin" on 
    // opposite strand (this is needed later for results that need to be moved to the
    // opposite strand).
    if (fwd && rev) {
      forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
      reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
    }
  }

  /** Creates a new FeatureSet for the analysis on strand. If an analysis with prog,db, 
   *  and strand has already been created, the existing one is returned. If new a
   *  FeatureSet is created, added to all_analyses hash, added to analyses, and returned. 
   *  (Adapted from GAMEAdapter.) 
   *  Note that many of the arguments are not actually used. */
  private FeatureSetI initAnalysis (StrandedFeatureSetI analyses,
                                    int strand,
                                    String prog,
                                    String db,
                                    String date,
                                    String programversion,
                                    String sourceversion,
                                    Hashtable all_analyses) {
    String analysis_type = constructAnalysisType(prog, db);
    String analysis_name;
    if (!analysis_type.equals(RangeI.NO_TYPE))
      analysis_name = (analysis_type +
                       (strand == 1 ? "-plus" :
                        (strand == -1 ? "-minus" : "")));
    else
      analysis_name = (RangeI.NO_NAME +
                       (strand == 1 ? "-plus" :
                        (strand == -1 ? "-minus" : "")));

    // check all_analyses cache to see if we've already created this analysis
    if (all_analyses == null)
      all_analyses = new Hashtable();
    FeatureSetI analysis = (FeatureSetI) all_analyses.get(analysis_name);
    // analysis has not been already created - go ahead and make a new one
    if (analysis == null) {
      analysis = new FeatureSet();
      //      analysis.setId (analysis_id);
      analysis.setProgramName(prog);
      analysis.setDatabase(db);
      analysis.setFeatureType (analysis_type);
      if (!analysis_type.equals(RangeI.NO_TYPE))
        analysis.setName (analysis_name);
      analysis.setStrand (strand);
      //      System.out.println("Initialized analysis type " + analysis_name); // DEL
      // analysis.setHolder (true);
      //       if (type != null)
      //         analysis.setBioType (type);
      //       if (date != null)
      //         analysis.addProperty ("date", date);
      //       if (version != null)
      //         analysis.addProperty ("version", version);
      // Analysis is added later on, but only if it has features
    }
    return analysis;
  }

  /** Build Apollo-style analysis type name from program and sourcename.
   *  (From GAMEAdapter (called getAnalysisType there).) */
  private String constructAnalysisType(String prog, String db) {
    String analysis_type;
    if (!(prog == null) && !prog.equals("") && !(db == null) && !db.equals(""))
      analysis_type = prog + ":" + db;
    else if (!(prog == null) && !prog.equals(""))
      analysis_type = prog;
    else if (!(db == null) && !db.equals(""))
      analysis_type = db;
    else
      analysis_type = RangeI.NO_TYPE;
    return analysis_type;
  }

  /** Add in stranded analyses only if features were found on that strand;
   *  otherwise it's scrapped (not added to all_analyses and analyses) 
   *  and false is returned */
  private boolean addAnalysisIfHasFeatures(FeatureSetI analysis,
                                           Hashtable all_analyses,
                                           StrandedFeatureSetI analyses) {
    if (all_analyses == null) {
      logger.debug("addAnalysisIfHasFeatures: analysis hash was null!");
      return false;
    }
    if (all_analyses.containsKey(analysis.getName())) {
      logger.trace("analysis type " + analysis.getName() + " already in all_analyses");
      return true;
    }
    if (analysis.size() > 0) {
      all_analyses.put(analysis.getName(),analysis);
      logger.trace("added analysis type " + analysis.getName() + " to all_analyses");
      analyses.addFeature(analysis);
      return true;
    }
    logger.trace("no analyses of type " + analysis.getName());
    return false; // no feats for analysis
  }

  /** Add the subject id and range to the feature as properties.
   *  Note that this probably isn't the right thing to do once we start making
   *  actual feature pairs for alignments. */
  public void addSubject(String feat_id, String subject_id, String subject_range) {
    SeqFeature feature = (SeqFeature)id2sym.get(feat_id);
    feature.addProperty("subject_id", subject_id);
    feature.addProperty("subject_range", subject_range);
  }

}
