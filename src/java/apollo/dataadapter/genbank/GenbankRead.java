/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2004 Berkeley Drosophila Genome Center, UC Berkeley.
 *  @author Suzanna Lewis
 */

package apollo.dataadapter.genbank;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import apollo.datamodel.*;
import apollo.datamodel.seq.GenbankSequence;
import apollo.config.Config;
import apollo.seq.io.FastaFile;
import apollo.dataadapter.*;

import org.apache.log4j.*;

import org.bdgp.io.*;
import org.bdgp.util.*;

public class GenbankRead {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GenbankRead.class);

  /**
   *  This hash table contains the GENBANK start of line keywords (LOCUS,
   *  DEFINITION, FEATURES and the EMBL start of lines that they correspond to)
   **/
  private static Hashtable genbank_hash = null;

  static {
    genbank_hash = new Hashtable ();
    genbank_hash.put ("LOCUS","ID");
    genbank_hash.put ("DEFINITION","DE");
    genbank_hash.put ("ACCESSION","AC");
    genbank_hash.put ("NID","NID");
    genbank_hash.put ("VERSION","DT");
    genbank_hash.put ("KEYWORDS","KW");
    genbank_hash.put ("SOURCE","OS");
    genbank_hash.put ("ORGANISM","OC");
    genbank_hash.put ("REFERENCE","RP");
    genbank_hash.put ("AUTHORS","RA");
    genbank_hash.put ("TITLE","RT");
    genbank_hash.put ("JOURNAL","RL");
    genbank_hash.put ("PUBMED","RK");
    genbank_hash.put ("MEDLINE","RK");
    genbank_hash.put ("COMMENT","CC");
    genbank_hash.put ("FEATURES","FH");
    genbank_hash.put ("source","FT");
    genbank_hash.put ("BASE","SQ");
    genbank_hash.put ("ORIGIN","SQ");
  }

  /** There are absolutely no mandatory qualifiers for an
     mRNA feature so that makes finding an appropriate value
     to use as an ID/Name a bit problematic. Do the best
     we can and if that fails then make something up. This
     method however, returns null if there are no ID-able
     qualifiers found. 
     Optional qualifiers
     /allele="text" (and in gene)
     /citation=[number] (and in gene)
     /db_xref="<database>:<identifier>"  (and in gene)
     /evidence=<evidence_value>              (and in gene)
     /function="text" (and in gene)
     /gene="text" (and in gene)
     /label=feature_label (and in gene)
     /locus_tag="text" (single token) (and in gene)
     /map="text" (and in gene)
     /note="text" (and in gene)
     /operon="text" (and in gene)
     /product="text" (and in gene)
     /pseudo (and in gene)
     /standard_name="text"
     /usedin=accnum:feature_label
     /codon_start=int
 */
  private static Vector transcript_id_tags = null;
  private static Vector annot_id_tags = null;
  private static Vector annot_name_tags = null;

  static {
    transcript_id_tags = new Vector ();
    transcript_id_tags.addElement ("product");
    transcript_id_tags.addElement ("transcript_id");
    transcript_id_tags.addElement ("protein_id");
    transcript_id_tags.addElement ("codon_start"); // ?
  }

  static {
    annot_id_tags = new Vector ();
    annot_id_tags.addElement ("locus_tag");
    annot_id_tags.addElement ("transposon");
  }

  static {
    annot_name_tags = new Vector ();
    annot_name_tags.addElement ("gene");
    annot_name_tags.addElement ("standard_name");
    annot_name_tags.addElement ("allele");
    annot_name_tags.addElement ("label");
    annot_name_tags.addElement ("note");
  }

  private static Hashtable featureTag_hash = null;

  static {
    featureTag_hash = new Hashtable();
    featureTag_hash.put ("map", "cyto_range");
    featureTag_hash.put ("RBS", "ribosomal_binding_site");
    featureTag_hash.put ("transposon", "transposable_element");
    featureTag_hash.put ("misc_RNA", "ncRNA");
  }

  /**
   *  The tag for the end of entry line: "//"
   **/
  final static int BEGINNING_OF_ENTRY = 0;
  final static String BEGINNING_OF_ENTRY_STRING = "ID";

  /**
   *  The tag for the end of entry line: "//"
   **/
  final static int END_OF_ENTRY = 1;
  final static String END_OF_ENTRY_STRING = "//";

  /**
   *  The tag for the start of sequence line
   **/
  final static int SEQUENCE = 2;
  final static String SEQUENCE_STRING = "SQ";

  /**
   *  The tag for EMBL feature table lines
   **/
  final static int FEATURE = 3;
  final static String FEATURE_STRING = "FT";

  /**
   *  The tag for EMBL feature header lines (FH ...)
   **/
  final static int FEATURE_HEADER = 4;
  final static String FEATURE_HEADER_STRING = "FH";

  /**
   *  The tag for EMBL definition lines
   **/
  final static int DEFINITION = 5;
  final static String DEFINITION_STRING = "DE";

  /**
   *  The tag for EMBL accession lines
   **/
  final static int ACCESSION = 6;
  final static String ACCESSION_STRING = "AC";

  /**
   *  The tag for EMBL organism of source lines
   **/
  final static int ORGANISM = 7;
  final static String ORGANISM_STRING = "OS";

  /**
   *  The tag for EMBL cross-references
   **/
  final static int REF_KEY = 8;
  final static String REF_KEY_STRING = "RK";

  /**
   *  The tag used for unidentified input. Which is kept, but not
   *  distinguished
   **/
  final static private int MISC = 9;

  private final static int max_attempts = 500;

  /**
   *  The column of the output where we should start writting the location.
   *  e.g. for EMBL the 's' in source is the 5th character in the line
   FT   source 
   *  e.g. for Genbank the 'O' in On is the 12th character in the line
   COMMENT     On Sep 18, 2002 this sequence version replaced gi:10727164.
   **/
  private final static int EMBL_CONTENT_OFFSET = 5;
  private final static int GENBANK_CONTENT_OFFSET = 12;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * The column to use for the content of a row file
   */
  private int content_offset;

  private int line_number = 0;
  private String current_line;
  private String current_content;
  private String current_first_word = "";
  private String current_line_key;
  private int current_line_type;

  private HashMap annots_hash;
  private HashMap trans_hash;

  private boolean curation_range_set;

  public boolean loadCurationSet(CurationSet curation,
                                 BufferedReader input) {
    logger.debug("Trying to load curation set now");
    int attempts = 0;
    line_number = 0;
    current_line_type = -1;
    
    String first_line = null;
    getCurrentInput(input);
    while (attempts++ < max_attempts && first_line == null) {
      if (current_line == null) {
        attempts = max_attempts;
      }
      else {
        int index = current_line.indexOf("LOCUS       ");
        if (index < 0) 
          index = current_line.indexOf("ID   ");
        if (index >= 0) {
          first_line = current_line.substring(index);
          current_line = first_line;
          current_line_type = getLineType(current_line);
          current_content = getRestOfLine(current_line, content_offset);
        } else {
          getCurrentInput(input);
        }
      }
    }
    
    boolean success = (first_line != null);
    if (success) {
      logger.debug("Initial read was successful, parsing now");
      success &= (beginEntry (curation) != null);
      if (success) {
        // Save LOCUS line
        GenbankSequence seq = (GenbankSequence) curation.getRefSequence();
        seq.addProperty("LOCUS", current_line + "\n");
        success &= readAbout (curation, input);
      }
    }
    if (!success)
      logger.error("GenBank read failed");
    return success;
  }

  // loop until there are no more lines in the file or we hit the start of
  // the next feature or the start of the next line group
  private boolean readAbout(CurationSet curation, BufferedReader input) {
    boolean done = false;
    GenbankSequence seq = (GenbankSequence) curation.getRefSequence();
    getCurrentInput(input);
    int index;

    while (current_line != null && !done) {
      // If we're reading GenBank (not EMBL) format, save lines verbatim for
      // later printing
      if (!isEMBL() && !(current_first_word.equals(""))) {
        logger.debug("added property " + current_first_word + " = " + current_line);
        seq.addProperty(current_first_word, current_line + "\n");
      }
      switch (current_line_type) {
      case END_OF_ENTRY:
	addAnnotations(curation.getAnnots());
        done = true;
        break;
      case SEQUENCE:
        readSequence(curation, input);
        break;
      case FEATURE_HEADER:
      case FEATURE:
        readFeature(curation, input);
        break;
      case DEFINITION:
        if (seq.getDescription() != null)
          seq.setDescription(seq.getDescription() + " " + current_content);
        else
          seq.setDescription(current_content);
        getCurrentInput(input);
        break;
      case ACCESSION:
        index = current_content.indexOf(';');
        if (index < 0)
	    index = current_content.indexOf(' ');
	if (index < 0)
          seq.setAccessionNo(current_content);
        else
          seq.setAccessionNo(current_content.substring(0, index));
        getCurrentInput(input);
        break;
      case ORGANISM:
        seq.setOrganism(current_content);
        getCurrentInput(input);
        break;
      case REF_KEY:
        String db;
        String id;
        if (isEMBL()) {
          erasePeriod();
          index = current_content.indexOf("; ");
          db = current_content.substring(0, index);
          id = current_content.substring(index + 2);
        } else {
          index = current_line.indexOf(" ");
          db = current_line.substring(0, index);
          id = current_content;
        }
        seq.addDbXref(db, id);
        getCurrentInput(input);
        break;
      case MISC:
        if (!current_content.equals(".")) {
          // Note: current_line_key is a GLOBAL value set by getLineType.
          // It uses the EMBL codes, so we should only use it if we're reading EMBL.
          if (isEMBL())
            seq.addProperty(current_line_key, current_content);
        }
        getCurrentInput(input);
        break;
      default:
        logger.warn ("forgot to deal with line type \"" + current_line_type +
                     "\" (current content = " + current_content + ")");
        return false;
      }
    }
    if (current_line_type != END_OF_ENTRY) {
      logger.warn("didn't find end of record (//) but saving annotations anyway.");
      addAnnotations(curation.getAnnots());
    }

    return true;
  }

  /**
     Sets up all the current values, the line itself, the
     line type, the offset, and the content */
  private void getCurrentInput(BufferedReader input) {
    current_line = readLineFromInput(input);
    if (current_line != null) {
       boolean within_html = true;
       while (within_html) {
         int html1 = current_line.indexOf("<");
         int html2 = current_line.indexOf(">") + 1;
         within_html = (html1 >= 0 && html2 > 0 && html2 > html1);
         // Is this REALLY html, or is it < or > indicating incomplete 5' or 3' end?
         // (don't want to get rid of those!!)
         within_html = within_html && 
           (current_line.indexOf("   gene   ") < 0) &&
           (current_line.indexOf("   mRNA   ") < 0);
         if (within_html) {
           String prefix = current_line.substring(0, html1);
           String suffix = (html2 < current_line.length() ?
                            current_line.substring(html2) : "");
           current_line = prefix + suffix;
         }
       }
      current_line = replaceInString(current_line, "&gt;", ">");
      current_line = replaceInString(current_line, "&lt;", "<");
      current_line_type = getLineType(current_line);
      current_content = getRestOfLine(current_line, content_offset);
    }
  }
 
  private String readLineFromInput(BufferedReader input) {
    line_number++;
    try {
      return (input.readLine ());
    } catch (Exception e) {
      logger.error("Unable to read line " + line_number, e);
      return null;
    }
  }

  private String replaceInString(String current_line,
                                 String get_out, 
                                 String put_in) {
    boolean seeking = true;
    while (seeking) {
      int sgml = current_line.indexOf(get_out);
      seeking = sgml >= 0;
      if (seeking) {
        String prefix = current_line.substring(0, sgml);
        int index = sgml + get_out.length();
        String suffix = (index < current_line.length() ?
                         current_line.substring(index) : "");
        current_line = prefix + put_in + suffix;
      }
    }
    return current_line;
  }

  /**
   *  Return the embl line type of the line contained in the argument String.
   */
  public int getLineType (String line) {
    String line_key = null;
    if (line.length () >= 2 &&
        (line.charAt (0) == '/' || Character.isLetter (line.charAt (0))) &&
        (line.charAt (1) == '/' || Character.isLetter (line.charAt (1))) &&
        (line.length () == 2 ||
         line.length () == 3 && line.endsWith (" ") ||
         line.length () == 4 && line.endsWith ("  ") ||
         line.length () >= 5 && line.substring (2,5).equals ("   "))) {
      line_key = line.substring(0, 2);
      content_offset = EMBL_CONTENT_OFFSET;
    }
    else if (line.length () > 0) {
      if (Character.isLetter (line.charAt (0))) {
        int first_space = line.indexOf (' ');
        if (first_space == -1) {
          line_key = (String) genbank_hash.get (line);
        } else {
          String first_word = line.substring (0, first_space);
          if (!(first_word.equals("")))
            current_first_word = first_word;
          line_key = (String) genbank_hash.get (first_word);
        }
        if (line_key != null)
          content_offset = GENBANK_CONTENT_OFFSET;
      } else if (GENBANK_CONTENT_OFFSET < line.length()){
        String sub_key = line.substring(0, GENBANK_CONTENT_OFFSET).trim();
        // Don't use the subkeys, stick with the main key
        //        if (!(sub_key.equals("")))
        //          current_first_word = sub_key;
        line_key = (String) genbank_hash.get (sub_key);
      }
    }

    if (line_key != null) {
      current_line_key = line_key;
      if (line_key.startsWith(BEGINNING_OF_ENTRY_STRING)){
        return BEGINNING_OF_ENTRY;
      }
      if (line_key.startsWith(ORGANISM_STRING)){
        return ORGANISM;
      }
      if (line_key.startsWith(END_OF_ENTRY_STRING)) {
        return END_OF_ENTRY;
      }
      if (line_key.startsWith (SEQUENCE_STRING)) {
        return SEQUENCE;
      }
      if (line_key.startsWith (FEATURE_HEADER_STRING)) {
        return FEATURE_HEADER;
      }
      if (line_key.startsWith (FEATURE_STRING)) {
        return FEATURE;
      }
      if (line_key.startsWith (DEFINITION_STRING)) {
        return DEFINITION;
      }
      if (line_key.startsWith (ACCESSION_STRING)) {
        return ACCESSION;
      }
      else {
        return MISC;
      }
    }
    // default is whatever was last parsed in
    return current_line_type;
  }

  /**
   *  Returns a String containing the contents of the line with the initial
   *  type string (number of letters dependent upon whether or not it is
   *  genbank or embl format) and trailing white space removed.
   */
  private String getRestOfLine (String line, int content_offset) {
    if (line.length () > content_offset) {
      return line.substring(content_offset).trim();
    } else {
      return "";
    }
  }

  private void addSynonyms(AnnotatedFeatureI annot, Vector synonyms) {
    if (synonyms != null) {
      for (int i = 0; i < synonyms.size(); i++) {
        String syn = (String) synonyms.elementAt(i);
        annot.addSynonym(syn);
      }
    }
  }

  private void addDbXref(AnnotatedFeatureI annot, HashMap db_xrefs) {
    if (db_xrefs != null) {
      Iterator keys = db_xrefs.keySet().iterator();
      while (keys.hasNext()) {
        String db = (String) keys.next();
        String id = (String) db_xrefs.get(db);
        annot.addDbXref(new DbXref("id", id, db));
      }
    }
  }

  private void addDbXref(SequenceI seq, HashMap db_xrefs) {
    if (db_xrefs != null) {
      Iterator keys = db_xrefs.keySet().iterator();
      while (keys.hasNext()) {
        String db = (String) keys.next();
        String id = (String) db_xrefs.get(db);
        seq.addDbXref(db, id);
      }
    }
  }

  private void setDescription(CurationSet curation,
                              GenbankSequence seq,
                              PublicDbFeature pub_feat) {
    HashMap tagValues = pub_feat.getTagValues();
    Iterator keys = tagValues.keySet().iterator();
    while (keys.hasNext()) {
      String tag = (String) keys.next();
      String value = pub_feat.getValue(tag);
      if (value != null && !value.equals("")) {
        if (tag.equals("chromosome"))
          curation.setChromosome(value);
        else if (tag.equals("organism"))
          seq.setOrganism (value);
        else {
          seq.setDescription (seq.getDescription() + " " + value);
          seq.addProperty(tag, value);
          logger.debug("Added property to sequence: " + tag + "=" + value);
        }
      }
    }
    addDbXref (seq, pub_feat.getDbXrefs());
  }

  private void setDescription(AnnotatedFeatureI af, 
                              PublicDbFeature pub_feat) {
    HashMap tagValues = pub_feat.getTagValues();
    Iterator keys = tagValues.keySet().iterator();
    while (keys.hasNext()) {
      String tag = (String) keys.next();
      if (//!tag.equals("translation") &&
          //!tag.equals("codon_start") &&
          !tag.equals("gene") &&
          !tag.equals("db_xref")) {
        String value = pub_feat.getValue(tag);
        if (value != null &&
            !value.equals("") &&
            !(value.equals(af.getName()) || value.equals(af.getId()))) {
          if (tag.equals("map"))
            tag = "cyto_range";
          if (tag.equals("note")) {
            Comment comment = new Comment();
            comment.setText(value);
            af.addComment(comment);
          }
          else {
            af.addProperty(tag, value);
            // If tag was codon_start, we need to save that--it indicates the phase.
            if (tag.equals("codon_start") && af instanceof Transcript) {
              Transcript transcript = (Transcript) af;
              try {
                transcript.setTranslationStart(transcript.getTranslationStart() + (Integer.parseInt(value) - 1));
                transcript.setPeptideValidity(true);
              } catch (Exception e) { }
            }
            // Also need to save translation.  When we translate ourselves, it doesn't
            // always match what was in the GenBank record.
            if (tag.equals("translation") && af instanceof Transcript) {
              Transcript transcript = (Transcript) af;
              String seq_id = af.getProperty(af.getProperty(transcript.getProperty("prot_desc")));
              if (seq_id == null)
                seq_id = af.getProperty("gene") + " sequence";
              // Trim trailing quote
              if (value.indexOf("\"") > 0)
                value = value.substring(0, value.indexOf("\""));
              SequenceI seq = new Sequence(seq_id, value);
              seq.setResidueType (SequenceI.AA);
              transcript.setPeptideSequence (seq);
              transcript.setPeptideValidity(true);
              if (logger.isDebugEnabled()) {
                logger.debug("Saved translation for transcript " + af.getName() + ": " + transcript.getPeptideSequence().getResidues());
              }
            }
          }
        }
      }
    }
    addDbXref(af, pub_feat.getDbXrefs());
    addSynonyms(af, pub_feat.getSynonyms());
  }

  private void setLocation(RangeI sf, Vector locs) {
    int loc_count = locs.size();
    if (sf instanceof Transcript) {
      Transcript transcript = (Transcript) sf;
      for (int i = 0; i < loc_count; i++) {
        int [] loc = (int []) locs.elementAt(i);
        ExonI exon = new Exon();
        int strand = (loc[0] > loc[1] ? -1 : 1);
        exon.setStrand(strand);
        exon.setStart(loc[0]);
        exon.setEnd(loc[1]);
        if (transcript.getStrand() != strand) 
          transcript.setStrand(strand);
        transcript.addExon(exon);
      }
    } else {
      if (loc_count != 1)
        logger.warn("Odd, " + loc_count + " locs parsed");
      if (loc_count == 0)
        return;
      int [] loc = (int []) locs.elementAt(0);
      int strand = (loc[0] > loc[1] ? -1 : 1);
      sf.setStrand(strand);
      sf.setStart(loc[0]);
      sf.setEnd(loc[1]);
    }
  }

  private void addTranslationStart(FeatureSetI fs, PublicDbFeature pub_feat) {
    Vector locs = pub_feat.getLocation();
    if (locs.size() > 0 && !pub_feat.missing5prime()) {
      int [] loc = (int []) locs.elementAt(0);
      int strand = (loc[0] > loc[1] ? -1 : 1);
      int tss = 0;
      if (strand == -1) {
        int loc_count = locs.size();
        for (int i = 0; i < loc_count; i++) {
          loc = (int []) locs.elementAt(i);
          if (loc[0] > tss)
            tss = loc[0];
        }
      } else {
        tss = loc[0];
      }
      /* don't bother setting the end because the sequence
         hasn't been read in yet */
      tss += (pub_feat.getCodonStart() * fs.getStrand());
      fs.setTranslationStart(tss, false);
    }
  }

  private void readFeature(CurationSet curation, BufferedReader input) {
    GenbankSequence seq = (GenbankSequence) curation.getRefSequence();
    StrandedFeatureSetI annotations
      = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    annotations.setName ("Annotations");
    annotations.setFeatureType ("Annotation");
    annotations.setRefSequence(curation.getRefSequence());
    curation.setAnnots(annotations);
    // just plug in a place holder here
    StrandedFeatureSetI results
      = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    // the reference sequence should propagate down as
    // new features are added
    results.setRefSequence(curation.getRefSequence());
    curation.setResults(results);
    String value;

    /* first get past the header */
    while (current_line != null && current_line_type != FEATURE) {
      getCurrentInput(input);
    }

    /* now actually read in the features */
    while (current_line != null && current_line_type == FEATURE) {
      PublicDbFeature current_feature = new PublicDbFeature(this);
      String key = current_feature.getFeatureType(current_line);
      if (logger.isDebugEnabled()) {
        logger.debug("GenbankRead.readFeature: key = " + key + " for getFeatureType(" + current_line + ")");
      }
      getCurrentInput(input);
      while (current_line != null && 
             current_line_type == FEATURE &&
             current_feature.addToFeature(current_line)) {
        getCurrentInput(input);
      }
      if (key == null) {
        logger.error("GenBank read error: no key in line " + current_line);
        continue;
      }
      if (key.equals("source")) {
        if (!curation_range_set) {
          setLocation(curation, current_feature.getLocation());
          setDescription(curation, seq, current_feature);
          curation_range_set = true;
        } else {
          logger.warn("Got 'source' key, but curation range is not set--this should be treated as a clone. (?)");
        }
      }
      else if (key.equals("gene") ||
               // Some GenBank records seem to use locus_tag instead of gene
               // for the gene name/id
               key.equals("locus_tag")) {
        AnnotatedFeatureI annotation = buildAnnotation(current_feature);
        setLocation(annotation, current_feature.getLocation());
        setDescription(annotation, current_feature);
      }
      else if (key.equals("mRNA") ||
               key.equals("rRNA") ||
               key.equals("tRNA") ||
               key.equals("scRNA") ||
               key.equals("snRNA") ||
               key.equals("snoRNA")) {
        AnnotatedFeatureI fs = buildTranscript(current_feature);
        AnnotatedFeatureI annot = (AnnotatedFeatureI) fs.getRefFeature();
        value = current_feature.getValue("gene");
        if (value != null && !value.equals("")) {
          annot.setName(value);
          }
        if (!key.equals("mRNA")) {
          annot.setFeatureType(key);
        }
        /*
        value = current_feature.getValue("locus_tag");
        if (value != null && !value.equals("")) {
          annot.setId(value);
          if (annot.getName().equals(RangeI.NO_NAME))
            annot.setName(annot.getId());
            } */
        setDescription(fs, current_feature);
      }
      else if (key.equals("CDS")) {
        setTranscriptCDS(current_feature);
      }
      else if (key.equals("repeat_region") ||
               key.equals("repeat_unit") ||
               key.equals("LTR") ||
               key.equals("satellite")) {
        setTransposon(current_feature, key);
      }
      else if (key.equals("misc_feature")) {
        setGeneric(current_feature, "miscellaneous curator's observation");
      }
      else if (!key.equals("variation") &&
               !key.equals("intron") &&
               !key.equals("exon") &&
               !key.equals("polyA_site")) {
        setGeneric(current_feature, key);
      }
      else {
        logger.error("GenbankRead: still not dealing with " + key +
                     " features; next line is " + line_number);
      }
    }
  }

  /**
   *  This method will read raw sequence from the stream in this object
   *  removing whitespace as it goes.  No checks are made on the format of the
   *  sequence, apart from checking that the stream contains only letters.
   **/
  private void readSequence(CurationSet curation, BufferedReader input) {
    SequenceI seq = curation.getRefSequence();
    getCurrentInput(input);
    // we buffer up the sequence bases then assign them to sequence once all
    // bases are read
    StringBuffer sequence_buffer = new StringBuffer (seq.getLength());
    while (current_line != null && current_line.length() > 0 && current_line_type == SEQUENCE) {
      if (current_line.charAt(0) == ' ') {
        String seq_str;
        if (isEMBL()) {
          // the line numbers are at the end of the line
          int index = current_line.lastIndexOf(' ');
          seq_str = current_line.substring(0, index).trim();
        } else {
          // the line numbers are at the beginning of the line and
          // the sequence ALWAYS(??) begins in column 10
          seq_str = current_line.substring(10);
        }
        for (int i = 0 ; i < seq_str.length () ; ++i) {
          char this_char = seq_str.charAt (i);
          if (Character.isLetter (this_char) ||
              this_char == '.' ||
              this_char == '-' ||
              this_char == '*') {
            sequence_buffer.append (this_char);
          } else {
            if (Character.isSpaceChar (this_char)) {
              // just ignore it
            } else {
              logger.warn("Sequence file contains " +
                          "a character that is not a " +
                          "letter: " + this_char + " in line " +
                           line_number);
            }
          }
        }
      }
      getCurrentInput(input);
    }
    seq.setResidues(sequence_buffer.toString());
  }

  private boolean isEMBL() {
    return content_offset == EMBL_CONTENT_OFFSET;
  }

  private void erasePeriod() {
    // eliminate the trailing '.' in EMBL headers
    current_content = current_content.replace('.', ' ').trim();
  }

  private SequenceI beginEntry(CurationSet curation) {
    annots_hash = new HashMap();
    trans_hash = new HashMap();
    curation_range_set = false;

    GenbankSequence seq = null;
    if (!current_content.equals("")) {
      // eliminate the trailing '.' in EMBL headers
      current_content = current_content.replace('.', ' ').trim();
      // AE003603   standard; genomic DNA; INV; 295289 BP.
      // NT_033777             738221 bp    DNA     linear   INV 12-SEP-2003
      // AE003603              295289 bp    DNA     linear   INV 14-FEB-2003
      int index;
      index = current_content.indexOf(" ");
      String seq_id = (index > 0 ? 
                       current_content.substring(0, index) : current_content);
      seq = new GenbankSequence (seq_id, "");
      String key = (isEMBL() ? " BP" : " bp");
      index = current_content.indexOf(key);
      if (index > 0) {
        String str = current_content.substring(0, index);
        String prefix = "";
        String suffix = (index < current_content.length() ? 
                         current_content.substring(index) : "");
        index = str.lastIndexOf(' ');
        if (index > 0) {
          prefix = str.substring(0, index);
          str = str.substring(index);
        }
        try {
          int length = Integer.parseInt(str.trim());
          seq.setLength(length);
          seq.setHeader(prefix + suffix);
        } catch (Exception e) {
          logger.error("Unable to parse length from " +
                       current_content + " from \"" + str + "\"", e);
        }
      }
      if (current_content.indexOf("DNA") > 0)
        seq.setResidueType (SequenceI.DNA);
      else if (current_content.indexOf("RNA") > 0)
        seq.setResidueType (SequenceI.RNA);
      else
        logger.error("couldn't parse residue type from " + current_content);
      curation.addSequence(seq);
      curation.setRefSequence(seq);
      curation.setName (seq.getName());
    }
    return seq;
  }

  private AnnotatedFeatureI buildAnnotation(PublicDbFeature pub_feat) {
    AnnotatedFeatureI annotation = new AnnotatedFeature();
    String id = getAnnotationId(pub_feat);
    String name = getAnnotationName(pub_feat);
    if (id == null || id.equals(""))
      id = pub_feat.getValue("protein_id");
    if (id == null || id.equals("")) {
      logger.debug("buildAnnotation: no id for " + pub_feat.toString());
      /* just make something up then */
      //      id = RangeI.NO_NAME;
      // The line below was commented out, but why is it not ok to make up an ID
      // like this?  Otherwise, we can only remember one unnamed feature, because
      // the name "no_name" gets reused.
      id = pub_feat.getFeatureType() + (annots_hash.size() + 1);
    }
    if (name == null || name.equals(""))
      name = id;
    annotation.setId(id);
    annotation.setName(name);
    if (!pub_feat.getValue("pseudo").equals("")) {
      annotation.setFeatureType("pseudogene");
    }
    // can't add it until the strand is determined
    annots_hash.put(id, annotation);
    return annotation;
  }

  private Transcript buildTranscript(PublicDbFeature pub_feat) {
    Transcript trans = new Transcript();
    setLocation(trans, pub_feat.getLocation());
    return buildTranscript(pub_feat, trans);
  }

  private Transcript buildTranscript(PublicDbFeature pub_feat, 
                                     Transcript transcript) {
    AnnotatedFeatureI annotation = getAnnotation(pub_feat, true);
    String id = getTranscriptId(pub_feat);
    if (id == null || id.equals("")) {
      /* just make something up then */
      id = annotation.getId();
    }
    transcript.setId(id);
    transcript.setName(id);

    // This seems to set it in this instance of the transcript, but
    // for some reason if we look at the transcript later,the value of
    // missing5prime is not preserved.  ???
    transcript.setMissing5prime(pub_feat.missing5prime());
    //    if (pub_feat.missing5prime()) // DEL
    //      transcript.setName(id + " (MISSING 5')"); // DEL--for debugging

    trans_hash.put(id, transcript);
    if (annotation.getStrand() != transcript.getStrand())
      annotation.setStrand(transcript.getStrand());
    if (!pub_feat.getValue("pseudo").equals(""))
      annotation.setFeatureType("pseudogene");

    annotation.addFeature(transcript, true);
    return transcript;
  }

  private AnnotatedFeatureI getFeatureById(PublicDbFeature pub_feat,
                                           Vector id_tags,
                                           HashMap feature_hash) {
    AnnotatedFeatureI af = null;
    for (int i = 0; i < id_tags.size() && af == null; i++) {
      String tag = (String) id_tags.elementAt(i);
      String id = pub_feat.getValue(tag);
      af = (id != null && !id.equals("") ? 
            (AnnotatedFeatureI) feature_hash.get(id) :
            null);
    }
    return af;
  }

  private AnnotatedFeatureI getAnnotation(PublicDbFeature pub_feat, 
                                          boolean build) {
    AnnotatedFeatureI annotation = getFeatureById(pub_feat, 
                                                  annot_id_tags,
                                                  annots_hash);
    if (annotation == null)
      annotation = getFeatureById(pub_feat, 
                                  annot_name_tags,
                                  annots_hash);
    if (annotation == null) {
      if (build) {
        annotation = buildAnnotation(pub_feat);
      } else {
        String name = pub_feat.getValue("gene");
        if (name != null && !name.equals("")) {
          Iterator keys = annots_hash.keySet().iterator();
          while (keys.hasNext() && annotation == null) {
            String id = (String) keys.next();
            AnnotatedFeatureI af = (AnnotatedFeatureI) annots_hash.get(id);
            annotation = (af.getName().equals(name) ? af : null);
          }
        }
      }
    }
    return annotation;
  }

  private Vector getContainingTranscripts(AnnotatedFeatureI fs) {
    Vector trans = new Vector();
    Iterator keys = trans_hash.keySet().iterator();
    while (keys.hasNext()) {
      String id = (String) keys.next();
      AnnotatedFeatureI af = (AnnotatedFeatureI) trans_hash.get(id);
      if (af.isProteinCodingGene() && 
          af.getTranslationStart() == 0 &&
          af.contains(fs)) {
        int af_start_index = af.getIndexContaining(fs.getStart());
        if (af_start_index >= 0) {
          int af_end_index = af.getIndexContaining(fs.getEnd());
          int fs_end_index = fs.size() - 1;
          if (af_end_index == (fs_end_index + af_start_index)) {
            boolean matches = true;
            for (int j = 0; j <= fs_end_index && matches; j++) {
              SeqFeatureI af_sf = af.getFeatureAt(af_start_index + j);
              SeqFeatureI fs_sf = fs.getFeatureAt(j);
              if (af_sf == null || fs_sf == null) {
                logger.debug("Checking annotation at index " + 
                             (af_start_index + j) + " for match to " +
                             j + " sizes are " + af.size() + " and " +
                             fs.size());
              }
              if (j == 0 && j == fs_end_index)
                matches &= af_sf.contains(fs_sf);
              else if (j == 0) 
                matches &= (af_sf.getEnd() == fs_sf.getEnd());
              else if (j == fs_end_index)
                matches &= (af_sf.getStart() == fs_sf.getStart());
              else
                matches &= af_sf.isExactOverlap(fs_sf);
            }
            if (matches)
              trans.addElement(af);
          }
        }
      }
    }
    return trans;
  }
  
  private AnnotatedFeatureI findSame(AnnotatedFeatureI fs,
                                     HashMap feat_hash) {
    Iterator keys = feat_hash.keySet().iterator();
    AnnotatedFeatureI same = null;
    while (keys.hasNext() && same == null) {
      String id = (String) keys.next();
      AnnotatedFeatureI current_fs = (AnnotatedFeatureI) feat_hash.get(id);
      if (areSameLocation(fs, current_fs)) {
        same = current_fs;
      }
    }
    return same;
  }
    
  private String getFeatureId(PublicDbFeature pub_feat, Vector tags) {
    String id = "";
    for (int i = 0; i < tags.size() && (id == null || id.equals("")); i++) {
      String tag = (String) tags.elementAt(i);
      id = pub_feat.getValue(tag);
      logger.debug(tag + " is \"" + id + "\"");
    }
    return id;
  }

  private String getTranscriptId(PublicDbFeature pub_feat) {
    return getFeatureId(pub_feat, transcript_id_tags);
  }

  private String getAnnotationId(PublicDbFeature pub_feat) {
    String id = getFeatureId(pub_feat, annot_id_tags);
    if (id == null || id.equals("")) {
      id = getFeatureId(pub_feat, annot_name_tags);
    }
    return id;
  }

  private String getAnnotationName(PublicDbFeature pub_feat) {
    return getFeatureId(pub_feat, annot_name_tags);
  }

  private AnnotatedFeatureI getTranscript(PublicDbFeature pub_feat) {
    Transcript trial_fs = new Transcript();
    setLocation(trial_fs, pub_feat.getLocation());
    return getTranscript(pub_feat, trial_fs);
  }

  private AnnotatedFeatureI getTranscript(PublicDbFeature pub_feat,
                                          Transcript trial_fs) {
    AnnotatedFeatureI transcript = getFeatureById(pub_feat,
                                                  transcript_id_tags,
                                                  trans_hash);
    if (transcript == null) {
      AnnotatedFeatureI af = getAnnotation(pub_feat, false);
      if (af != null) {
        for (int i = 0; i < af.size() && transcript == null; i++) {
          AnnotatedFeatureI fs = (AnnotatedFeatureI) af.getFeatureAt(i);
          transcript = findSame(fs, trans_hash);
        }
      }
    }
    if (transcript == null) 
      transcript = buildTranscript(pub_feat, trial_fs);
    return transcript;
  }

  private void setTranscriptCDS(PublicDbFeature pub_feat) {
    // For now, refuse to deal with CDSs that use a different translation table
    if (pub_feat.getValue("transl_table") != null &&
        !(pub_feat.getValue("transl_table").equals(""))) {
      logger.warn("CDS " + pub_feat.getValue("product") + " uses alternative translation table (" + pub_feat.getValue("transl_table") + ")\n Can't yet deal with alternative translation tables--not loading this CDS.");
      logger.debug(" pub_feat = " + pub_feat);
      return;
    }
    Transcript trial_cds = new Transcript();
    setLocation(trial_cds, pub_feat.getLocation());
    Vector trans = getContainingTranscripts(trial_cds);
    // do this because sometimes the same CDS produces more than one 
    // product (due to different transcripts with alternate UTR structure)
    Vector vals = pub_feat.getValues("product");
    if (vals != null && vals.size() > 0) {
      for (int i = vals.size() - 1; i >= 0; i--) {
        AnnotatedFeatureI fs = null;
        String hope = (String) vals.elementAt(i);
        // This looks like we're trying to figure out the transcript ID from the
        // peptide ID--can we then save it as the transcript name, since it seems
        // to get lost?  --NH
        int index = hope.indexOf("-P");
        if (index >= 0) {
          String id = (hope.substring(0, index) + "-R" +
                       hope.substring(index + "-P".length()));
          fs = (AnnotatedFeatureI) trans_hash.get(id);
          //in some cases fs appears to be null, so calling fs.getName() causes
          //a NullPointerException
          if (fs != null) {
              logger.debug("Found annotated feature " + fs + " with peptide ID " + hope + "; transcript id would be " + id + ", whereas feature name is " + fs.getName());
          }
        }
        if (fs == null) {
          if (trans.size() > 0) {
            fs = (AnnotatedFeatureI) trans.elementAt(0);
            trans.removeElementAt(0);
        }
          else {
            fs = buildTranscript(pub_feat);
          }
        }
        else {
          trans.removeElement(fs);
        }
        buildCDS(fs, pub_feat);
        vals.removeElement(hope);

        if (logger.isDebugEnabled()) {
          Transcript transcript = (Transcript)fs;
          logger.debug("end of setTranscriptCDS.if: for " + transcript.getName() + ", missing5prime = " + transcript.isMissing5prime());
        }
      }
    } else {
      AnnotatedFeatureI fs = (trans.size() > 0 ?
                              (AnnotatedFeatureI) trans.elementAt(0) :
                              buildTranscript(pub_feat, trial_cds));
      buildCDS(fs, pub_feat);

      if (logger.isDebugEnabled()) {
        Transcript transcript = (Transcript)fs;
        logger.debug("end of setTranscriptCDS.else: for " + transcript.getName() + ", missing5prime = " + transcript.isMissing5prime());
      }
    }
  }

  private void buildCDS(AnnotatedFeatureI fs, PublicDbFeature pub_feat) {
    addTranslationStart(fs, pub_feat);
    setDescription(fs, pub_feat);
  }

  private void setTransposon(PublicDbFeature pub_feat, String key) {
    Transcript trial_fs = new Transcript();
    String type = null;
    setLocation(trial_fs, pub_feat.getLocation());
    AnnotatedFeatureI af;
    AnnotatedFeatureI transposon = findSame(trial_fs, trans_hash);
    if (transposon == null) {
      transposon = trial_fs;
      af = findSame(transposon, annots_hash);
      if (af == null) {
        af = buildAnnotation(pub_feat);
      }
      String id = pub_feat.getValue("transposon");
      if (id == null || id.equals("")) {
        id = af.getId();
        type = pub_feat.getValue("rpt_type");
        if (type == null || type.equals(""))
          type = key;
      }
      else {
        type = "transposable_element";
      }
      transposon.setId(id);
      transposon.setName(id);
      setDescription(af, pub_feat);  // Otherwise map is lost
      trans_hash.put(id, transposon);
      if (af.getStrand() != transposon.getStrand())
        af.setStrand(transposon.getStrand());
      af.addFeature(transposon, true);
    } else {
      af = (AnnotatedFeatureI) transposon.getRefFeature();
    }
    af.setFeatureType(type);
    if (af.getName().equals(af.getId()))
      af.setName(transposon.getName());
    setDescription(transposon, pub_feat);
  }

  private void setGeneric(PublicDbFeature pub_feat, String type) {
    Transcript trial_fs = new Transcript();
    setLocation(trial_fs, pub_feat.getLocation());
    AnnotatedFeatureI fs = findSame(trial_fs, trans_hash);
    if (fs == null) {
      fs = getTranscript(pub_feat, trial_fs);
    }
    AnnotatedFeatureI af = (AnnotatedFeatureI) fs.getRefFeature();
    af.setFeatureType(type);
    fs.setName(af.getName());
    setDescription(fs, pub_feat);
  }

  private void setTranslationEnds(AnnotatedFeatureI gene) {
    Vector transcripts = gene.getFeatures ();
    int trans_cnt = transcripts.size();
    for (int i = 0; i < trans_cnt; i++) {
      Transcript fs = (Transcript) transcripts.elementAt (i);
      int tss = fs.getTranslationStart();
      if (tss > 0) {
        fs.setTranslationStart(tss, true);
        int start = fs.getStart();
        if (fs.unConventionalStart() &&
            (tss == start || 
             tss == (start + fs.getStrand()) ||
             tss == (start + (2 * fs.getStrand()))))
          fs.calcTranslationStartForLongestPeptide();
      }
      else {
        fs.calcTranslationStartForLongestPeptide();
      }
    }
  }

  private void addAnnotations(StrandedFeatureSetI annotations) {
    if (annots_hash != null) {
      Iterator keys = annots_hash.keySet().iterator();
      while (keys.hasNext()) {
        String id = (String) keys.next();
	AnnotatedFeatureI annot = (AnnotatedFeatureI) annots_hash.get(id);
        if (annot.size() > 0) {
          annotations.addFeature(annot);
          if (annot.isProteinCodingGene()) {
            setTranslationEnds(annot);
          }
        } else {
          AnnotatedFeatureI af 
            = (AnnotatedFeatureI) annots_hash.get(RangeI.NO_NAME);
          if (af != null && af.overlaps(annot)) {
            logger.debug("GenbankRead.addAnnotations: using substitution for annotation " + annot.getName());
            af.setName(annot.getName());
            af.setId(annot.getId());
          }
          else
            logger.debug("GenbankRead.addAnnotations: annotation " + annot.getName() + " has no children");
        }
      }

      if (logger.isDebugEnabled()) {
        for (int i = 0; i < annotations.size(); i++) {
          AnnotatedFeature gene = (AnnotatedFeature)annotations.getFeatureAt(i);
          for (Iterator iter = gene.getFeatures().iterator(); iter.hasNext();) {
            // printTranscript prints the CDS and mRNA records.
            Transcript transcript = (Transcript)iter.next();
            // Nope, it doesn't remember
            logger.debug("addAnnotations: for " + transcript.getName() + ", missing5prime = " + transcript.isMissing5prime());
            }
        }
      }
    }
  }

  public boolean areSameLocation(SeqFeatureI sa,SeqFeatureI sb) {
    boolean overlap = false;
    if (sb.canHaveChildren() && sb.getNumberOfChildren() > 0) {
      FeatureSetI fb = (FeatureSetI)sb;
      for (int i=0; i<fb.size() && !overlap; i++) {
        overlap = areSameLocation(sa,fb.getFeatureAt(i));
      }
    }
    else if (sa.canHaveChildren() && sa.getNumberOfChildren() > 0) {
      FeatureSetI fa = (FeatureSetI)sa;
      for (int i=0; i<fa.size() && !overlap; i++) {
        overlap = areSameLocation(fa.getFeatureAt(i),sb);
      }
    }
    else if (sa.overlaps(sb)) {
      overlap = sa.isExactOverlap (sb);
    }
    return overlap;
  }
 
}
