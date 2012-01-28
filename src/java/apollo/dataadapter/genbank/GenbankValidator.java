/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.genbank;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.config.OverlapI;
import apollo.config.Config;
import apollo.seq.io.FastaFile;

import org.apache.log4j.*;

import org.bdgp.util.DNAUtils;

public class GenbankValidator {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GenbankValidator.class);

  private static String [] specials = {"Hand edit",
				       "Known mutation",
				       "trans spliced",
				       "CDS edit"};

  // Not currently used
  private static String [] GB_exceptions = {"RNA editing",
					    "reasons given in citation",
					    "ribosomal slippage",
					    "trans splicing",
					    "artificial frameshift",
					    "nonconsensus splice site"};

  private static final String read_through_note1 =
    "A UGA codon at amino acid position ";
  private static final String read_through_note2 =
    "is replaced by a selenocysteine allowing read through";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private int gene_cnt;
  private CurationSet curation;
  private Hashtable validator_hash;
  private Hashtable patches_hash;
  private String database = "";
  private String tech = "";
  private String organism = "";
  private String genotype = "";
  private Vector db_xrefs = new Vector(1);

  public GenbankValidator() {
  }

  public boolean setValidationFile (String valid_filename) {
    // read this in and set up the validation classes
    validator_hash = new Hashtable();
    patches_hash = new Hashtable();
    File validation_file = null;
    try {
      validation_file = new File (valid_filename);
    }
    catch (Exception e) {
      logger.error("could not open validation file " + valid_filename, e);
      return false;
    }
    try {
      StreamTokenizer tokenizer =
        new StreamTokenizer(new BufferedReader(new FileReader(validation_file)));

      tokenizer.eolIsSignificant(true);
      tokenizer.slashStarComments(true);
      tokenizer.slashSlashComments(true);

      boolean EOF     = false;
      int     tokType = 0;
      Stack   words   = new Stack();
      while (!EOF) {
	if ((tokType = tokenizer.nextToken()) == StreamTokenizer.TT_EOF) {
	  EOF = true;
	}
	else if (tokType != StreamTokenizer.TT_EOL) {
	  if (tokenizer.sval != null) {
	    words.push(tokenizer.sval);
	  }
	}
	else { // we've reached the end of the line
	  if (words.size() == 2) {
	    String class_name = (String)words.pop();
	    String feature_type = (String)words.pop();
	    if (feature_type.equalsIgnoreCase ("database")) {
	      database = class_name;
	    }
	    else if (feature_type.equalsIgnoreCase ("tech")) {
	      tech = class_name;
	    }
	    else if (feature_type.equalsIgnoreCase ("organism")) {
	      organism = class_name;
	    }
	    else if (feature_type.equalsIgnoreCase ("genotype")) {
	      genotype = class_name;
	    }
	    else if (feature_type.equalsIgnoreCase ("db_xref")) {
	      db_xrefs.addElement(new XrefUtil(class_name));
	    }
	    else {
	      Vector validators = (Vector) validator_hash.get (feature_type);
	      if (validators == null) {
		validators = new Vector();
		validator_hash.put (feature_type, validators);
	      }
	      try {
		Class check_class = Class.forName (class_name);
		if (check_class == null)
		  logger.error("GenbankValidator.setValidationFile: unable to create class " + class_name);
		else {
		  FeatureValidatorI check = (FeatureValidatorI)check_class.newInstance();
		  validators.add (check);
		}
	      }
	      catch (Exception e) {
		logger.error("GenbankValidator.setValidationFile: couldn't create a " + class_name, e);
	      }
	    }
	  }
	  String pep = null;
	  if (words.size() == 6) {
	    pep = (String)words.pop();
	  }
	  if (words.size() == 5) {
	    String note = (String)words.pop();
	    String acc = (String)words.pop();
	    String pep_id = (String)words.pop();
	    String gene_name = (String)words.pop();
            //	    String keyword = (String)words.pop();
	    if (!gene_name.equals ("-")) {
	      String key = gene_name + (pep_id.equals ("-") ?
					"" : "::" + pep_id);
	      PeptideNote patch = (PeptideNote) patches_hash.get (key);
	      if (patch == null) {
		patch = new PeptideNote();
	      }
	      else {
                logger.debug("Hey second patch for " + key);
	      }
	      patch.setGeneName (gene_name);
	      patch.setPeptideId (pep_id);
	      patch.setAccession (acc);
	      patch.setNote (note);
	      patch.setPeptide (pep);
	      patches_hash.put (key, patch);
              logger.debug("Added patch for " + key);
	    }
	  }
	  else if (words.size() > 0) {
	    logger.error("Couldn't parse line in validation file " + valid_filename + ":");
            StringBuffer msg = new StringBuffer();
	    while (words.size() > 0)
	      msg.append(words.pop() + "\t");
            msg.append("\n");
            logger.debug(msg.toString());
	  }
	  words.removeAllElements();
	}
      }
    }
    catch (Exception e) {
      logger.error("Could not parse validation file " + validation_file, e);
    }
    return true;
  }

  public String getDatabase () {
    return database;
  }

  public Vector getXrefs () {
    return db_xrefs;
  }

  public String getTech () {
    return tech;
  }

  public String getOrganism() {
    return organism;
  }

  public String getGenotype() {
    return genotype;
  }

  /**
   * Writing G
   */
  public void checkCuration(CurationSet curation,
			    DataOutputStream report) {
    this.curation = curation;

    StrandedFeatureSetI annots = curation.getAnnots();
    int annot_count = annots.size();
    try {
      if (annot_count > 0) {
	gene_cnt = 0;
	for (int i = 0; i < annot_count; i++) {
	  AnnotatedFeatureI gene = (AnnotatedFeatureI) annots.getFeatureAt(i);
	  String type = gene.getFeatureType().toLowerCase();
          if (gene.isProteinCodingGene()) {
	    gene_cnt++;
	  }
	  String prefix = getPrefix(gene, type);
	  report.writeBytes (checkFeature (gene, type, prefix));
	  report.writeBytes (checkFeature (gene, "all", prefix));
	  report.writeBytes (checkOverlap (gene, i+1, annots, prefix));
	  report.writeBytes (checkDuplicates (gene, i+1, annots, prefix));
	}	
	report.writeBytes ("Found " + gene_cnt + " genes\n");
      }
      else {
	StrandedFeatureSetI results = curation.getResults();
	int result_count = results.size();
	if (result_count > 0) {
	  for (int i = 0; i < result_count; i++) {
	    FeatureSetI fs = (FeatureSetI) results.getFeatureAt(i);
	    String type = fs.getFeatureType().toLowerCase();
	    String prefix = getPrefix(fs, type);
	    report.writeBytes (checkFeature (fs, type, prefix));
	  }
	}
      }
    }
    catch ( Exception ex ) {
      logger.error("exception writing validation report", ex);
    }
    try {
      report.close();
    } catch ( Exception ex ) {
      logger.error("exception closing validation report", ex);
    }
  }

  protected String checkFeature(FeatureSetI fs, String type, String prefix) {
    StringBuffer buf = new StringBuffer();

    if (validator_hash != null) {
      Vector validators = (Vector) validator_hash.get(type);
      if (validators != null) {
	int check_total = validators.size();
	for (int i = 0; i < check_total; i++) {
	  FeatureValidatorI v =
	    (FeatureValidatorI) validators.elementAt (i);
	  buf.append (v.validateFeature (fs, curation, prefix, "\n"));
	}
      }
    }
    return buf.toString();
  }

  protected boolean sameName (String name, SeqFeatureI test) {
    return (name.equals(test.getName()) || name.equals(test.getId()));
  }

  protected String sameName (SeqFeatureI sf, SeqFeatureI test, String prefix) {
    if (sameName (sf.getName(), test)) {
      return (prefix + " has same symbol as " + 
	      test.getId() + "," + test.getName() + "\n");
    }
    else if (sameName (sf.getId(), test)) {
      return (prefix + " has same ID as " + 
	      test.getId() + "," + test.getName() + "\n");
    }
    else
      return "";
  }

  private String checkDuplicates (AnnotatedFeatureI gene, 
				  int begin_index,
				  StrandedFeatureSetI annots,
				  String prefix) {
    StringBuffer buf = new StringBuffer();
    int annot_count = annots.size();
    int trans_count = gene.size();
    boolean duplicate = false;

    for (int i = begin_index; i < annot_count && !duplicate; i++) {
      AnnotatedFeatureI test = (AnnotatedFeatureI) annots.getFeatureAt(i);
      int test_count = test.size();
      buf.append (sameName(gene, test, prefix));
      for (int k = 0; k < test_count && buf.length() == 0; k++) {
	Transcript test_trans = (Transcript) test.getFeatureAt(k);	
	buf.append (sameName (gene, test_trans, prefix));
      }
      for (int j = 0; j < trans_count && buf.length() == 0; j++) {
	Transcript trans = (Transcript) gene.getFeatureAt(j);	
	buf.append (sameName (trans, test, prefix));
	for (int k = 0; k < test_count && buf.length() == 0; k++) {
	  Transcript test_trans = (Transcript) test.getFeatureAt(k);	
	  buf.append (sameName (trans, test_trans, prefix));
	}
      }
    }
    return buf.toString();
  }

  private boolean ignoreTransposons (AnnotatedFeatureI gene) {
    boolean ignore = (!getComment(gene, "transposon overlap OK").equals(""));
    int transcript_count = gene.size();
    for (int i = 0; i < transcript_count && !ignore; i++) {
      Transcript transcript = (Transcript) gene.getFeatureAt(i);
      ignore |= (!getComment(transcript, "transposon overlap OK").equals(""));
    }
    return ignore;
  }

  private String checkOverlap (AnnotatedFeatureI gene, 
			       int begin_index,
			       StrandedFeatureSetI annots,
			       String prefix) {
    StringBuffer buf = new StringBuffer();
    OverlapI checker = Config.getOverlapper(gene);
    int annot_count = annots.size();
    boolean ignore_transposon = ignoreTransposons (gene);
    boolean is_transposon = gene.getFeatureType().equals ("transposable_element");
    int trans_count = gene.size();

    for (int i = begin_index; i < annot_count; i++) {
      AnnotatedFeatureI test = (AnnotatedFeatureI) annots.getFeatureAt(i);
      boolean test_is_transposon 
	  = test.getFeatureType().equals ("transposable_element");
      if (!((ignore_transposon && 
	     test_is_transposon) ||
	    (is_transposon &&
	     ignoreTransposons (test)))) {
	if (checker.areOverlapping (gene, test)) {
	  boolean overlap_okay;
	  overlap_okay = (gene.isProblematic() ||
			  ! getComment (gene, "Shares CDS").equals(""));
	  for (int j = 0; j < trans_count && !overlap_okay; j++) {
	    Transcript trans = (Transcript) gene.getFeatureAt(j);
	    overlap_okay |= (trans.isProblematic() ||
			     !getComment (trans, "Shares CDS").equals(""));
	  }
	  if (!overlap_okay)
	    buf.append (prefix + " overlaps " + test.getName() + "\n");
	}
      }
    }
    return buf.toString();
  }

  private String getPrefix (FeatureSetI fs, String type) {
    String prefix = (type + 
		     " ERROR:  " + fs.getId() + 
		     "," + fs.getName() +
		     "," + (String) fs.getProperty ("gbunit"));
    //if there are no child features as in the case on one-level-annots, don't add
    //to the prefix
    if (fs instanceof AnnotatedFeatureI && fs.getFeatures().size() > 0)
      prefix +=  "," +
	((Transcript) fs.getFeatures().elementAt(0)).getOwner();
    prefix += "\t";
    return prefix;
  }

  private String getComment (AnnotatedFeatureI sf, String clue) {
    String text = "";
    Vector comments = sf.getComments ();
    for (int i = 0; i < comments.size() && text.equals(""); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      if ((comment.getText().indexOf (clue) >= 0)) {
	text = comment.getText();
      }
    }
    return text;
  }

  public String patchPeptideCheck (AnnotatedFeatureI gene,
				   Transcript transcript,
				   String header,
				   DataOutputStream report) {
    SequenceI pep = transcript.getPeptideSequence();
    String key = gene.getName() + "::" + pep.getName();
    PeptideNote patch = (PeptideNote) patches_hash.get (key);
    String text = "";
    String gene_text = "";

    for (int i = 0; i < specials.length && text.equals(""); i++) {
      text = getComment (gene, specials[i]);
      gene_text = text;
      if (text.equals (""))
	text = getComment (transcript, specials[i]);
    }
    // there is a comment denoting special handling for
    // this peptide

    if (!text.equals ("")) {
      if (patch == null) {
	String prefix = getPrefix(gene, gene.getFeatureType().toLowerCase());
	try {
	  if (!gene_text.equals("") && gene.size() > 1) {
	    // don't want to do anything with this
	    report.writeBytes (prefix + transcript.getName() +
			       " will not be submitted " + "\n");
	    return null;
	  }
	  else {
	    report.writeBytes (prefix + key +
			       " no patch " + text + "\n");
	  }
	}
	catch ( Exception ex ) {
	  logger.error("exception writing validation report", ex);
	}
      }
      else {
	pep.setResidues (patch.getPeptide(pep.getResidues()));
      }
    }
    // there is not a comment denoting special handling for
    // this peptide
    else {
      if (patch != null) {
	String prefix = getPrefix(gene, gene.getFeatureType().toLowerCase());
	try {
	  report.writeBytes (prefix + " no comment " + "\n");
	}
	catch ( Exception ex ) {
	  logger.error("exception writing validation report", ex);
	}
	pep.setResidues (patch.getPeptide(pep.getResidues()));
      }
    }
    String residues = pep.getResidues();
    String clue = "Unconventional translation start";
    String comment = getComment (gene, clue);
    if (comment.equals ("") )
      comment = getComment (transcript, clue);
    if ( ! comment.equals ("") ) {
      residues = "M" + residues.substring (1);
    }

    return (FastaFile.format (header, residues, 50));
  }

  public String addNote (String except, PeptideNote patch) {
    StringBuffer buf = new StringBuffer();
    String note = patch.getNote();
    if (!except.equals (note)) {
      buf.append ("\t\t\tnote\t" + note);
      String acc = patch.getAccession();
      if (!acc.equals ("") && !acc.equals ("-")) {
	buf.append (". Used peptide of " + acc);
      }
      buf.append ("\n");
    }
    return buf.toString();
  }

  public int getExceptPos (PeptideNote patch) {
    int position = 0;
    
    if (patch != null) {
      String note = patch.getNote();
      int index = note.indexOf ("position ");
      if (index >= 0) {
	// first parse out the nucleotide or 
	// amino acid position from the string
	try {
	  index += "position ".length();
	  int end_index = note.indexOf (" ", index);
	  String number = (end_index >= 0 ?
			   note.substring (index, end_index) :
			   note.substring (index));
	  position = Integer.parseInt(number);
	}
	catch (Exception e) {
	  logger.error(patch.getPeptideId() + " couldn't parse position from " + note, e);
	  position = 0;
	}
      }
    }
    return position;
  }


  protected String getTranslExcept (Transcript transcript, int offset) {
    StringBuffer buf = new StringBuffer();

    int aa_position = transcript.readThroughStopPosition();

    if (aa_position > 0) {
      // this is an amino acid position
      String aa = "Sec";
      buf.append ("\t\t\ttransl_except\t(pos:" + 
                  aa_position + ".." + 
                  (aa_position + (2 * transcript.getStrand())) +
                  ",aa:" + aa + ")\n");
      int trans_pos = transcript.getFeaturePosition(aa_position);
      buf.append ("\t\t\tnote\t" + 
                  read_through_note1 + ((trans_pos / 3) + 1) + 
                  read_through_note2 + "\n");
    }
    if (transcript.unConventionalStart()) {
      int tss = transcript.getTranslationStart() - offset;
      buf.append ("\t\t\ttransl_except\t(pos:" +
                  tss + ".." + (tss + (2 * transcript.getStrand())) +
                  "," + 
                  /* DNAUtils.translate(transcript.getStartCodon(),
                                     DNAUtils.FRAME_ONE,
                                     DNAUtils.THREE_LETTER_CODE) +
                  */
                  "aa:Met)\n");
    }
    return buf.toString();
  }

  // Not currently used
  private static String [] splices = {"junctions",
				      "aberrant splice",
				      "nonconsensus splice",
				      "non-consensus splice",
				      "no conventional splice",
				      "non conventional splice",
				      "TG splice",
				      "GC splice",
				      "GG splice",
				      "CT splice",
				      "AT/AC splice",
				      "AT-AC splice",
				      "splice donor site postulated",
				      "Unconventional splice supported by ",
				      "GC splice donor site postulated",
				      "Unconventional splice site postulated",
				      "Unconventional splice (CC/CT)",
				      "Unconventional splice donor and acceptor sites",
				      "Unconventional splice site acceptor postulated",
				      "Unconventional GA splice site postulated",
				      "without consensus splice",
				      "doesn't fall at a conserved splice"};

  public String getException (Transcript transcript) {
    StringBuffer buf = new StringBuffer();

    if (transcript.isTransSpliced()) {
      buf.append ("\t\t\texception\ttrans splicing\n");
    }
    if (transcript.plus1FrameShiftPosition() > 0) {
      // transform from aa position to transcript position to genome
      buf.append ("\t\t\texception\tribosomal slippage plus 1 at " + 
                  transcript.plus1FrameShiftPosition() + "\n");
    }
    if (transcript.minus1FrameShiftPosition() > 0) {
      // transform from aa position to transcript position to genome
      buf.append ("\t\t\texception\tribosomal slippage minus 1 at " + 
                  transcript.minus1FrameShiftPosition() + "\n");
    }
    if ((transcript.getNonConsensusAcceptorNum() >= 0 || 
         transcript.getNonConsensusDonorNum() >= 0) &&
        transcript.nonConsensusSplicingOkay()) {
      buf.append ("\t\t\texception\tnonconsensus splice site\n");
    }
    return buf.toString();
  }

}
