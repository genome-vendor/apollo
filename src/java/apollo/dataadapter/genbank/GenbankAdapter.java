/* Written by Berkeley Drosophila Genome Project.
   Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.genbank;

import java.net.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.seq.io.FastaFile;
import apollo.dataadapter.*;
import apollo.dataadapter.DataInputType.UnknownTypeException;

import org.apache.log4j.*;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.ProgressEvent;

public class GenbankAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GenbankAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String LABEL = "GenBank/EMBL format";

  private boolean commit_acc = false;
  GenbankValidator validator = null;

  /* This indicates no GUI so certain non-apollo, non-gui apps can use this
     adapter. */
  boolean NO_GUI = false;

  private File genbank_file;
  private DataOutputStream report_os;

  // private String input;

  String region;
  String database = "";
  String tech = "";
  String organism = "";
  String genotype = "";
  Vector db_xrefs = null;

  private int offset;

  SequenceI curated_seq = null;
  
  IOOperation [] supportedOperations = {
    ApolloDataAdapterI.OP_READ_DATA,
    // ApolloDataAdapterI.OP_READ_SEQUENCE
    ApolloDataAdapterI.OP_WRITE_DATA
  };

  public GenbankAdapter() {
    setName(LABEL);
  }

  public GenbankAdapter(DataInputType inputType, String input) {
    setInputType(inputType);
    setInput(input);
    setName(LABEL);
  }

  public GenbankAdapter(DataInputType inputType,
                        String input,
                        boolean noGUI) {
    this(inputType, input);
    NO_GUI = noGUI;
    setName(LABEL);
  }

  public void init() {}

  /** org.bdgp.io.DataAdapter method */
  /** Doesn't seem to be used */
  public String getType() {
    return getName();
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    if (op.equals(ApolloDataAdapterI.OP_WRITE_DATA) ||
        op.equals(ApolloDataAdapterI.OP_READ_DATA))
      return new GenbankAdapterGUI(op);
    else
      return null;
  }

  /** Input string that corresponds with the input type
   * (e.g. gene name for gene input type)
   */
  public void setInput(String input) {
    if ((getInputType() == null) || 
        (getInputType() != null && getInputType().equals(DataInputType.DIR))) {
      File input_dir = new File (input);
      if (input_dir.isDirectory() &&
          input_dir.canWrite()) {
        super.setInputType(DataInputType.DIR);
        super.setInput(input);
      } else if (getInputType() != null) {
          super.setInputType(DataInputType.FILE);
          super.setInput(input);
      }
    }
    else
      super.setInput(input);
  }

  public void commitAccessions (boolean commit_acc) {
    this.commit_acc = commit_acc;
  }

  public void setValidationFile (String valid_filename) {
    validator = new GenbankValidator();
    /* if the file could not be read then cancel the validation */
    if (!validator.setValidationFile (valid_filename)) {
      validator = null;
      logger.error("Couldn't parse validation file " + valid_filename);
    }
    else {
      database = validator.getDatabase();
      tech = validator.getTech();
      organism = validator.getOrganism();
      genotype = validator.getGenotype();
      db_xrefs = validator.getXrefs();
    }
  }

  public Properties getStateInformation() {
    Properties props = new Properties();
    props.put(StateInformation.INPUT_STRING,getInput());
    props.put(StateInformation.INPUT_TYPE,getInputType().toString());
    props.put(StateInformation.DATABASE,getDatabase());
    return props;
  }

  /** DataLoader calls this */
  public void setStateInformation(Properties props) {
    setInput(props.getProperty(StateInformation.INPUT_STRING));
    String typeString = props.getProperty(StateInformation.INPUT_TYPE);
    try {
      setInputType(DataInputType.stringToType(typeString));
    }
    catch (UnknownTypeException e) {
      logger.error(e.getMessage()+" Can not set genbank adapter state info", e);
   }
    
    setDatabase(props.getProperty(StateInformation.DATABASE));
  }

  public SequenceI getSequence(String id) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end)
    throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref, int[] start, int[] end)
    throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public void setRegion(SequenceI seq) throws apollo.dataadapter.ApolloAdapterException {
    if (seq != null) {
      this.curated_seq = seq;
      setRegion (seq.getName());
    }
  }

  public void setRegion(String region)
    throws apollo.dataadapter.ApolloAdapterException {
    this.region = region;
  }

  /**
   * Write GenBank report in tabular or human-readable format, as requested
   */
  public void commitChanges(CurationSet curation, boolean wantTabular) {
    logger.debug("Committing data now to " + getInput());
    File out = new File (getInput());
    if (wantTabular)
      saveTabular(curation, out);
    else
      GenbankReport.save(curation, out);
  }

  private void saveTabular(CurationSet curation, File dir) {
    // Write tabular GenBank format to directory
    boolean okay_commit = (dir.isDirectory() &&
			   dir.canWrite());
    if (!okay_commit) {
      logger.warn("GenbankAdapter.saveTabular: directory " +
                  dir + " is not writeable--can't save.");
      return;
    }
    logger.info("Saving GenBank tabular format to directory " + dir);

    String report_file = (commit_acc ?
			  outputFileStem(curation) + ".acc.rpt" :
			  outputFileStem(curation) + ".annot.rpt");
    report_os = openGenbankFile (dir, report_file);
			  
    if (commit_acc) {
      commitAccessions (curation, dir);
    }
    else {
      // this must be done in this order because
      // the first flags any peptides that are 
      // NOT to be submitted to Genbank
      Vector omit = new Vector();
      commitGenomic (curation, dir);
      commitPeptides (curation, dir, omit);
      commitTranscripts (curation, dir, omit);
      commitTable (curation, dir, omit);
    }
    if (validator != null) {
      validator.checkCuration (curation, report_os);
    }
  }

  private void commitGenomic (CurationSet curation, File dir) {
    DataOutputStream dos = openGenbankFile (dir,
					    outputFileStem(curation) + ".fsa");
    try {
      dos.writeBytes(writeFasta(curation));
      try {
	dos.close();
      } catch ( Exception ex ) {
	logger.error("caught exception closing " + genbank_file.getName(), ex);
      }
    }
    catch ( Exception ex ) {
      logger.error("caught exception writing " + genbank_file.getName(), ex);
    }
  }

  private void commitTranscripts (CurationSet curation, 
                                  File dir,
                                  Vector omit) {
    DataOutputStream dos = openGenbankFile (dir,
					    outputFileStem(curation) + ".rna");
    try {
      StrandedFeatureSetI annots = curation.getAnnots();
      for (int i = 0; i < annots.size(); i++) {
	AnnotatedFeatureI gene = (AnnotatedFeatureI) annots.getFeatureAt(i);
	dos.writeBytes (writeTranscript (gene, dir, omit));
      }
      try {
	dos.close();
      } catch ( Exception ex ) {
	logger.error("caught exception closing " + genbank_file.getName(), ex);
      }
    }
    catch ( Exception ex ) {
      logger.error("caught exception writing " + genbank_file.getName(), ex);
    }
  }

  private void commitPeptides (CurationSet curation, 
			       File dir,
			       Vector omit) {
    DataOutputStream dos = openGenbankFile (dir,
					    outputFileStem(curation) + ".pep");
    try {
      StrandedFeatureSetI annots = curation.getAnnots();
      for (int i = 0; i < annots.size(); i++) {
	AnnotatedFeatureI gene = (AnnotatedFeatureI) annots.getFeatureAt(i);
	dos.writeBytes (writePeptide (gene, dir, omit));
      }
      try {
	dos.close();
      } catch ( Exception ex ) {
	logger.error("caught exception closing " + genbank_file.getName(), ex);
      }
    }
    catch ( Exception ex ) {
      logger.error("caught exception writing " + genbank_file.getName(), ex);
    }
  }

  private void commitTable (CurationSet curation, 
			    File dir,
			    Vector omit) {
    DataOutputStream dos = openGenbankFile (dir, outputFileStem(curation) + ".tbl");
    try {
      dos.writeBytes (">Feature " + getGenomeId (curation) + "\n");

      StrandedFeatureSetI annots = curation.getAnnots();
      offset = curation.getStart() - 1;
      int i;
      for (i = 0; i < annots.size(); i++) {
	AnnotatedFeatureI gene = (AnnotatedFeatureI) annots.getFeatureAt(i);
	dos.writeBytes (writeGene (gene, omit));
      }
      try {
	dos.close();
      } catch ( Exception ex ) {
	logger.error("caught exception closing " + genbank_file.getName(), ex);
      }
    }
    catch ( Exception ex ) {
      logger.error("caught exception writing " + genbank_file.getName(), ex);
    }
  }

  private void commitAccessions (CurationSet curation, File dir) {
    DataOutputStream dos = openGenbankFile (dir,
					    outputFileStem(curation) + ".acc");
    try {
      StrandedFeatureSetI results = curation.getResults();
      for (int i = 0; i < results.size(); i++) {
	FeatureSetI fs = (FeatureSetI) results.getFeatureAt(i);
	dos.writeBytes (writeAccession (fs));
      }
      try {
	dos.close();
      } catch ( Exception ex ) {
	logger.error("caught exception closing " + genbank_file.getName(), ex);
      }
    }
    catch ( Exception ex ) {
      logger.error("caught exception writing " + genbank_file.getName(), ex);
    }
  }

  private DataOutputStream openGenbankFile (File dir,
					    String file_name) {
    genbank_file = new File (dir, file_name);
    DataOutputStream dos = null;

    try {
      if ( ! genbank_file.exists ())
	genbank_file.createNewFile ();
      dos = new DataOutputStream(new FileOutputStream(genbank_file));
    } catch ( Exception ex ) {
      logger.error("caught exception opening " + genbank_file.getName() + " for writing", ex);
    }
    return dos;
  }

  // This currently assumes CurationSet  is the root object,  and
  //   that all its children do not contain references to other
  //   AnnotatedSeqs that would need to be included in the XML doc
  protected String writeFasta(CurationSet curation) {
    /* do like this
       >gnl|FlyBase|AE003519 [organism=Drosophila melanogaster] [chromosome=3L] [map=73
       D5] [tech=htgs 3]
    */
    StringBuffer buf = new StringBuffer();
    String chrom;
    if (database.equalsIgnoreCase("flybase")) {
      String arm = curation.getRefSequence().getName();
      int index = arm.indexOf (".");
      if (index > 0)
	arm = arm.substring (0, index);
      chrom = "[chromosome=" + arm + "] ";
    }
    else {
      chrom = "";
    }
    String org = (organism.equals ("") ?
		  "" : "[organism=" + organism + "] ");
    String geno = (genotype.equals ("") ?
		   "" : "[genotype=\"" + genotype + "\"]");
    String techno = (tech.equals ("") ?
		     "" : "[tech=" + tech + "] ");
    String header = (">" + getGenomeId (curation) + " " +
		     org +
		     chrom +
		     techno +
		     geno + "\n");
    String dna = curation.getResidues();
    if (dna != null && !dna.equals (""))
      buf.append (FastaFile.format (header, dna, 50));

    return buf.toString();
  }

  protected static String getGenbankType (String type) {
    String gb_type = "";
    if (type.equalsIgnoreCase ("gene") ||
	type.equalsIgnoreCase ("pseudogene")) {
      gb_type = "gene";
    }
    else if (type.equals("snRNA") ||
	     type.equals("snoRNA") ||
	     type.equals("tRNA") ||
	     type.equals("rRNA")) {
      gb_type = type;
    }
    else if (type.equalsIgnoreCase ("transposable_element")) {
      gb_type = "repeat_region";
    }
    else if (type.equalsIgnoreCase ("misc. non-coding RNA")) {
      gb_type = "misc_RNA";
    }
    else if ((type.equalsIgnoreCase ("miscellaneous curator's observation")) ||
	     (type.equalsIgnoreCase ("misc. curator's observation"))) {
      gb_type = "misc_feature";
    }
    else {
      gb_type = type + "=say_what?";
    }
    return gb_type;
  }

  protected String getIds (AnnotatedFeatureI gene, Transcript transcript) {
    return ("\t\t\ttranscript_id\t" +
	    getSequenceId(gene, 
			  transcript.get_cDNASequence(),
			  transcript) +
	    "\n" +
	    "\t\t\tprotein_id\t" +
	    getProteinId(gene, 
                         transcript.getPeptideSequence(),
                         transcript) +
	    "\n");
  }
  
  protected String getGeneProduct (AnnotatedFeatureI gene, 
				   Transcript transcript,
				   String CDS_start,
				   String CDS_stop,
				   String except,
				   int na_except_pos) {
    StringBuffer buf = new StringBuffer();
    
    int tss = transcript.getTranslationStart();
    int phase = 0;
    if (((FeatureSetI) gene).isMissing5prime() ||
	((FeatureSetI) transcript).isMissing5prime()) {
      phase = transcript.getFeaturePosition (tss) - 1;
    }
    SeqFeatureI start_span = transcript.getFeatureContaining(tss);
    int start_index = (start_span != null ?
		       transcript.getFeatureIndex (start_span) :
		       0);
    SequenceI pep = transcript.getPeptideSequence();
    int end_base = transcript.getLastBaseOfStopCodon ();
    Vector exons = transcript.getFeatures();
    int end_index = exons.size();
    boolean found_end = false;
    for (int i = start_index; i < end_index && !found_end; i++) {
      Exon exon = (Exon) exons.elementAt (i);
      boolean contains_tss = (i == start_index);
      found_end = exon.contains(end_base);
      if (contains_tss && phase == 0)
	buf.append (CDS_start + (tss - offset) + "\t");
      else if (contains_tss && phase > 0)
	buf.append (CDS_start + (exon.getStart() - offset) + "\t");
      else
	buf.append ((exon.getStart() - offset) + "\t");

      if (na_except_pos > 0 && exon.contains (na_except_pos)) {
	buf.append ("" + (na_except_pos - transcript.getStrand()));
	if (contains_tss)
	  buf.append ("\tCDS\n");
	else
	  buf.append ("\n");
	buf.append ((na_except_pos + transcript.getStrand()) + "\t");
	contains_tss = false;
	except = validator.getException (transcript);
      }
      if (found_end)
	buf.append (CDS_stop + (end_base - offset));
      else
	buf.append (exon.getEnd() - offset);
      
      if (contains_tss)
	buf.append ("\tCDS\n");
      else
	buf.append ("\n");
    }
    buf.append ("\t\t\tproduct\t" +
		getSequenceName(gene, pep, transcript) + "\n");
    buf.append (getIds(gene, transcript));
    buf.append ("\t\t\tprot_desc\t" +
		getProteinDesc (gene, transcript) + "\n");

    if (phase > 0) {
      buf.append ("\t\t\tcodon_start\t" + (phase + 1) + "\n");
    }

    if (!except.equals(""))
      buf.append (except);

    return buf.toString();
  }
 
  /** Change the flag that is passed if Genbank decides
      that some feature does/doesn't get the locus_tag field
  */
  protected String getLocus (AnnotatedFeatureI gene, 
                             String tag) {
    if (gene.getName().equals (gene.getId())) {
      if (tag.equals("transposon")) {
        return ("\t\t\tnote\tsymbol=" + gene.getId() + "\n");
      } else {      
        return ("\t\t\tlocus_tag\t" + gene.getId() + "\n");
      }
    } else {
      if (tag.equals("transposon")) {
        return("\t\t\tnote\tsymbol=" + gene.getName() + "\n");
      } else {
        return("\t\t\t" + tag + "\t" + gene.getName() + "\n" +
               "\t\t\tlocus_tag\t" + gene.getId() + "\n");
      }
    }
  }

  private String getGeneInterval (AnnotatedFeatureI gene,
                                  String gene_has_start,
                                  String gene_has_stop,
                                  String gb_type) {
    return (gene_has_start +
            (gene.getStart() - offset) + "\t" +
            gene_has_stop + (gene.getEnd() - offset) + "\t" +
            gb_type + "\n");
  }

  protected String writeGene(AnnotatedFeatureI gene, Vector omit) {
    StringBuffer buf = new StringBuffer();

    String gene_has_start = get5primeStart (gene);
    String gene_has_stop = get3primeStop (gene);
    String gb_type = getGenbankType (gene.getFeatureType());
    boolean not_a_gene = (!(gene.isProteinCodingGene()));

    if (gb_type.equals ("repeat_region")) {
      buf.append (getGeneInterval(gene,
                                  gene_has_start,
                                  gene_has_stop,
                                  gb_type));
      buf.append (getLocus (gene, "transposon"));      
    } else {
      buf.append (getGeneInterval(gene,
                                  gene_has_start,
                                  gene_has_stop,
                                  "gene"));
      buf.append (getLocus (gene, "gene"));
    }
    if (gb_type.equals ("gene")) {
      if (gene.getFeatureType().equalsIgnoreCase("pseudogene"))
	buf.append ("\t\t\tpseudo\n");
    } 
    buf.append (getSynonyms (gene, gb_type));
    buf.append (getCyto (gene));
    if (!(gb_type.equals ("repeat_region")) &&
        !(gb_type.equals ("gene"))) {
      buf.append (getGeneInterval(gene,
                                  gene_has_start,
                                  gene_has_stop,
                                  gb_type));
      // this is redundant with the locus tag above
      // buf.append("\t\t\tnote\tsymbol=" + gene.getName() + "\n");
      if (gb_type.equals ("tRNA")) {
        String aa = gene.getProperty ("aminoacid");
        if (!aa.equals (""))
          buf.append ("\t\t\tproduct\ttRNA-" + aa + "\n");
      } else {
        // ! There are other types we need to deal with too.
        buf.append (getNotes (gene));
      }
    }
    buf.append (getDbXrefs (gene));
    if (gene.size() > 0) {
      String date_note = gene.getFeatureAt(0).getProperty("date");
      if (date_note != null && !date_note.equals ("")) 
	buf.append ("\t\t\tnote\tlast curated on " + date_note + "\n");
    }

    if (gb_type.equals ("gene")) {
      Vector transcripts = gene.getFeatures ();
      int count = transcripts.size();
      for (int i = 0; i < count; i++) {
	Transcript transcript = (Transcript) transcripts.elementAt (i);
	if (! omit.contains (transcript) ) {
	  String CDS_start = (gene_has_start.equals("") ?
			      get5primeStart (transcript) :
			      gene_has_start);
	  
	  String CDS_stop = (gene_has_stop.equals("") ?
			     get3primeStop (transcript) :
			     gene_has_stop);
	  
	  Vector exons = transcript.getFeatures();
	  for (int j = 0; j < exons.size(); j++) {
	    Exon exon = (Exon) exons.elementAt (j);
	    if (j == 0)
	      buf.append (CDS_start);
	    buf.append ((exon.getStart() - offset) + "\t");
	    if (j == exons.size() - 1)
	      buf.append (CDS_stop);
            buf.append(exon.getEnd() - offset);
	    if (j == 0)
	      buf.append ("\tmRNA\n");
	    else
	      buf.append ("\n");
	  }
	  buf.append ("\t\t\tproduct\t" +
		      getSequenceName(gene, 
				      transcript.get_cDNASequence(),
				      transcript) + "\n");
	  if (!not_a_gene && (transcript != null)) {
	    buf.append (getIds(gene, transcript));
	    // transl_except is only for CDS
	    String except = "";
	    int na_except_pos = 0;  // How does this ever get set??
	    if (validator != null) {
	      except = validator.getTranslExcept (transcript, offset);
	      if (except.equals ("")) {
                except = validator.getException(transcript);
		// exception goes in both mRNA and CDS
		buf.append (except);
	      }
	    }
	    buf.append (getGeneProduct (gene, 
					transcript,
					CDS_start,
					CDS_stop,
					except,
					na_except_pos));
	  }
	  else if (gene.getFeatureType().equalsIgnoreCase("pseudogene"))
	    buf.append("\t\t\tpseudo\n");

	  else
	    buf.append("\t\t\t" + gene.getFeatureType() + "\n");
	}
      }
    }

    return buf.toString();

  }
  
  protected String writeTranscript(AnnotatedFeatureI gene, 
                                   File dir,
                                   Vector omit) {
    /* do like this
       >gnl|FlyBase|CG1234-RA (AE003519) [gene=CH1-2] [gene_syn=CG3889][prot_desc=CH1-2 gene product]
    */
    StringBuffer buf = new StringBuffer();
    //    String type = gene.getType();
    
    if (gene.isProteinCodingGene()) {
      String symbol = "[gene=" + gene.getName() + "] ";
      String gene_id = (gene.getName().equals (gene.getId()) ?
			"" : "[gene_syn=" + gene.getId() + "] ");
      String acc = gene.getProperty ("gbunit");
      
      Vector transcripts = gene.getFeatures ();
      int count = transcripts.size();
      for (int i = 0; i < count; i++) {
	Transcript transcript = (Transcript) transcripts.elementAt (i);
	if (! omit.contains (transcript) ) {
	  SequenceI rna = transcript.get_cDNASequence();
	  if (rna != null) {
	    String header = (">" + getSequenceId(gene, rna, transcript) +
			     " " +
			     (acc != null && !acc.equals ("") ?
			      "("+acc+") " : "") +
			     symbol +
			     gene_id + "\n");
	    buf.append (FastaFile.format (header,
					  rna.getResidues(),
					  50));
	  }
	  else {
	    logger.error("ERROR: " + transcript.getName() +
                         " IS NOT TRANSCRIBED??!!??" +
                         " (" + transcript.getOwner() + ")");
	  }
	}
      }
    }
    return buf.toString();
  }

  protected String writePeptide(AnnotatedFeatureI gene,
                                File dir,
                                Vector omit) {
    /* do like this
       >gnl|FlyBase|CT12959 (AE003519) [gene=CH1-2] [gene_syn=CG3889][prot_desc=CH1-2 gene product]
    */
    StringBuffer buf = new StringBuffer();
    if (gene.isProteinCodingGene()) {
      String symbol = "[gene=" + gene.getName() + "] ";
      String gene_id = (gene.getName().equals (gene.getId()) ?
			"" : "[gene_syn=" + gene.getId() + "] ");
      String acc = gene.getProperty ("gbunit");

      Vector transcripts = gene.getFeatures ();
      int count = transcripts.size();
      for (int i = 0; i < count; i++) {
	Transcript transcript = (Transcript) transcripts.elementAt (i);
	SequenceI pep = transcript.getPeptideSequence();
	if (pep != null) {
	  String header = (">" + getSequenceId(gene, pep, transcript) +
			   " " +
			   (acc != null && !acc.equals ("") ?
			    "("+acc+") " : "") +
			   symbol +
			   gene_id +
			   "[prot_desc=" +
			   getProteinDesc (gene, transcript) +
			   "]\n");
	  if (validator != null) {
	    String fasta = validator.patchPeptideCheck(gene, 
						       transcript,
						       header,
						       report_os);
	    if (fasta != null)
	      buf.append (fasta);
	    else
	      // this peptide should be skipped
	      omit.addElement (transcript);
	  }
	  else
	    buf.append (FastaFile.format (header,
					  pep.getResidues(),
					  50));
	}
	else {
	  logger.error("ERROR: " + transcript.getName() +
                       " DOES NOT TRANSLATE??!!??" +
                       " (" + transcript.getOwner() + ")");
	}
      }
    }
    return buf.toString();
  }

  protected String writeAccession(FeatureSetI fs) {
    /* do like this
       start stop accession
    */
    StringBuffer buf = new StringBuffer();
    if (fs.getProgramName().equalsIgnoreCase ("gbunits") ||
	fs.getProgramName().equalsIgnoreCase ("assembly")) {
      Vector spans = fs.getFeatures ();
      int count = spans.size();
      for (int i = 0; i < count; i++) {
	FeatureSetI sf = (FeatureSetI) spans.elementAt (i);
	String name = sf.getName();
	int index = name.indexOf (".");
	if (index > 0)
	  name = name.substring (0, index);
	buf.append (sf.getStart() + "\t" +
		    sf.getEnd() + "\t" +
		    name + "\n");
      }
    }
    return buf.toString();
  }

  public static String get5primeStart (AnnotatedFeatureI sf) {
    String starter = sf.isMissing5prime() ? "<" : "";
    if (starter.equals ("") &&
        sf.size() > 0 &&
        (sf.getFeatureAt(0) instanceof Transcript)) {
      int trans_count = sf.size();
      int missing_count = 0;
      for (int i = 0; i < trans_count && i == missing_count; i++) {
        Transcript transcript = (Transcript) sf.getFeatureAt (i);
        if (transcript.isMissing5prime()) 
          missing_count++;
      }
      starter = missing_count == trans_count ? "<" : "";
    }
    return starter;
  }

  public static String get3primeStop (AnnotatedFeatureI sf) {
    String stop = sf.isMissing3prime() ? ">" : "";
    if (stop.equals ("") &&
        sf.size() > 0 &&
        (sf.getFeatureAt(0) instanceof Transcript)) {
      int trans_count = sf.size();
      int missing_count = 0;
      for (int i = 0; i < trans_count && i == missing_count; i++) {
        Transcript transcript = (Transcript) sf.getFeatureAt (i);
        if (transcript.isMissing3prime()) 
          missing_count++;
      }
      stop = missing_count == trans_count ? ">" : "";
    }
    return stop;
  }

  // Not currently used
  private String getComment (AnnotatedFeatureI sf, String clue) {
    String text = "";
    String lc_clue = clue.toLowerCase();
    Vector comments = sf.getComments ();
    for (int i = 0; i < comments.size() && text.equals(""); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      if ((comment.getText().toLowerCase().indexOf (lc_clue) >= 0)) {
	text = comment.getText().trim();
      }
    }
    return text;
  }

  private String getNotes (AnnotatedFeatureI gene) {
    StringBuffer buf = new StringBuffer();
    Vector comments = gene.getComments ();
    for (int i = 0; i < comments.size(); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      if (!comment.isInternal()) {
	String note = comment.getText().trim();
	note = note.replace ('\n', ' ');
	buf.append ("\t\t\tnote\t" + note + "\n");
      }
    }
    return buf.toString();
  }

  private String getSynonyms (AnnotatedFeatureI gene, String gb_type) {
    StringBuffer buf = new StringBuffer();

    Vector syns = gene.getSynonyms();
    for (int i = 0; i < syns.size(); i++) {
      Synonym synObject = (Synonym) syns.elementAt (i);
      String syn = synObject.getName();
      if (!syn.equals("") &&
	  syn.indexOf ("temp") < 0 &&
	  !syn.equals (gene.getName()) &&
	  !syn.equals (gene.getId()))
        if (gb_type.equals ("repeat_region")) {
          buf.append ("\t\t\tnote\tsynonym=" + syn + "\n");
        } else {
          buf.append ("\t\t\tgene_syn\t" + syn + "\n");
        }
    }
    return buf.toString();
  }

  private String getDbXrefs (AnnotatedFeatureI gene) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < db_xrefs.size(); i++) {
      XrefUtil xref = (XrefUtil) db_xrefs.elementAt (i);
      buf.append(xref.getGenBankXref(gene));
    }
    return buf.toString();
  }

  public static String getDbXref (SequenceI seq) {
    String acc = "";
    Vector xrefs = seq.getDbXrefs();
    for (int i = 0; i < xrefs.size() && acc.equals(""); i++) {
      DbXref xref = (DbXref) xrefs.elementAt (i);
      if (xref.getIdValue() != null && !xref.getIdValue().equals ("")) {
	acc = xref.getIdValue();
      }
    }
    return acc;
  }

  private String getCyto (AnnotatedFeatureI gene) {
    StringBuffer buf = new StringBuffer();
    String cyto = (String) gene.getProperty("cyto_range");
    if (cyto != null && !cyto.equals ("")) {
      buf.append ("\t\t\tmap\t" + cyto + "\n");
    }
    return buf.toString();
  }

  private String getGenomeId (CurationSet curation) {
    String seq_id = (getDbXref(curation.getRefSequence()).equals("") ?
		     curation.getRefSequence().getName() :
		     getDbXref(curation.getRefSequence()));
    String region = curation.getName();
    int index = region.indexOf (".");
    if (index > 0)
      region = region.substring (0, index);
    return ("gnl|" + database + "|" + seq_id + 
	    "|gb|" + region);
  }

  public static String getProteinDesc (AnnotatedFeatureI gene,
                                       Transcript transcript) {
    if (gene.size() == 1)
      return (gene.getName() + " gene product");
    else
      return (gene.getName() + " gene product from transcript " +
	      getSequenceName(gene,
			      transcript.get_cDNASequence(),
			      transcript));
  }

  public static String getSequenceName (AnnotatedFeatureI gene,
                                        SequenceI seq, 
                                        Transcript transcript) {
    if (seq != null) {
      return seq.getName();
    }
    else {
      logger.warn("No seq for " + transcript.getName());
      return "";
    }
  }

  private String getSequenceId (AnnotatedFeatureI gene,
				SequenceI seq, 
				Transcript transcript) {
    String name = getSequenceName (gene, seq, transcript);
    String seq_id = name;
    if (!name.equals ("") && knownToGenbank(transcript))
      seq_id = ("gnl|" + database + "|" + name);
    return seq_id;
  }
  
  private String getProteinId (AnnotatedFeatureI gene,
                               SequenceI seq, 
                               Transcript transcript) {
    String name = getSequenceName (gene, seq, transcript);
    String seq_id = "";
    if (!name.equals ("")) {
      if (knownToGenbank(transcript))
        seq_id = ("gnl|" + database + "|" + name +
                  "|gb|" + transcript.getProperty("protein_id"));
      else
        seq_id = ("lcl|" + name);
    }
    return seq_id;
  }

  protected boolean knownToGenbank(Transcript transcript) {
    return !transcript.getProperty("protein_id").equals ("");
  }

  /** Curation name may contain : (e.g. 2L:12345-67890), which is problematic
   *  on Windows.  (Any other special characters we should look out for?)
   *  Replace : with _. */
  private String outputFileStem(CurationSet curation) {
    String name = curation.getName();
    name = name.replace(':', '_');
    return name;
  }
  
  public String getRawAnalysisResults(String id) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  /** from ApolloDataAdapterI interface. input type and input should be set
   * previous to this
   */
  public CurationSet getCurationSet() 
    throws apollo.dataadapter.ApolloAdapterException {
    CurationSet curation = new CurationSet();
    try {
      fireProgressEvent(new ProgressEvent(this, new Double(10.0),
                                          "Finding data..."));
      // Throws DataAdapter exception if faulty input (like bad filename)
      InputStream stream = getInputStream(getInputType(), getInput());
	
      BufferedInputStream bis = new BufferedInputStream(stream);
      InputStreamReader reader = new InputStreamReader(bis);
      /* Now that we know input is not faulty (exception not thrown if we got
         here) we can clear out old data. If we dont do clearing we get 
         exceptions on style changes as apollo tiers get in an inconsistent 
         state between  old and new data */
      if (!NO_GUI) {
	super.clearOldData();
      }

      // All "progress" percentages are made up--we can't actually tell
      // how far along we are, but we want to give the user an idea that
      // progress is being made.
      fireProgressEvent(new ProgressEvent(this, new Double(15.0),
                                          "Reading " + getInput() + "..."));
      
      GenbankRead gbio = new GenbankRead();
      logger.info("Loading " + getInput() + "...");
      if (gbio.loadCurationSet(curation, new BufferedReader(reader)))
        return curation;
      else
        return null;  // read failed
    } catch ( Exception ex2 ) {
      logger.error(ex2.getMessage(), ex2);
      throw new apollo.dataadapter.ApolloAdapterException(ex2.getMessage());
    }
  }

  private InputStream getInputStream(DataInputType type, String input) 
    throws ApolloAdapterException {
    InputStream stream = null;

    if (input == null) {
      String message = "The input was null. Neither filename nor URL given";
      logger.warn(message);
      throw new ApolloAdapterException(message);
    }
    if (type == DataInputType.FILE) {
      try {
        String path = apollo.util.IOUtil.findFile(input, false);
        logger.info("Trying to open file " + input);
        stream = new FileInputStream (path);
      } catch (Exception ex) {
        // FileNotFoundException
        throw new ApolloAdapterException("Error: could not open " +
                                       input + " for reading.");
      }
    } else if (type == DataInputType.URL) {
      String query_str = null;
      if (!input.startsWith("http://")) {
        Hashtable pubDbToURL = Config.getPublicDbList();
        String url_str = (String) pubDbToURL.get(getDatabase());
        logger.debug("database " + database + " URL is " + url_str);
        if (url_str != null) {
          if (!url_str.startsWith("http://"))
            url_str = "http://" + url_str;
          int index = url_str.indexOf('*');
          if (index > 0) {
            query_str = url_str.substring(0, index) + input;
            index++;
            if (index < url_str.length())
              query_str = query_str + url_str.substring(index);
          }
          else {
            query_str = url_str + input;
          }
        }
      } else {
        query_str = input;
      }
      try {
        logger.info("Trying to open URL " + query_str);
        URL url = new URL(query_str);
        stream = url.openStream();
      } catch ( Exception ex1 ) {
        stream = null;
        throw new ApolloAdapterException("Error: could not open " +
                                       input + " for reading using \"" +
                                       query_str + "\"");
      }
    }
    return stream;
  }

}
