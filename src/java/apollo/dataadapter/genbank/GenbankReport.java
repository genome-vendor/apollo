package apollo.dataadapter.genbank;

import apollo.datamodel.*;
import apollo.datamodel.seq.GenbankSequence;
import apollo.util.DateUtil;
import java.util.Iterator;
import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.log4j.*;

/**
 * GenbankReport.java
 *
 *
 * Created: Mon Sep  1 16:04:06 2003
 *
 * @author <a href="mailto:shiranp@bcm.tmc.edu">Shiran Pasternak</a>
 * @author <a href="mailto:rlozado@bcm.tmc.edu">Ryan Lozado</a>
 * @version 1.0
 * $Header: /cvsroot/gmod/apollo/src/java/apollo/dataadapter/genbank/GenbankReport.java,v 1.16 2007/01/03 13:50:15 jcrabtree Exp $
 *
 * Code from BCM for writing human-readable GenBank files.
 * Fixed/extended by Nomi.
 */
public class GenbankReport {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GenbankReport.class);

  private static final String LOCUS      = "LOCUS";
  private static final String DEFINITION = "DEFINITION";
  private static final String ACCESSION  = "ACCESSION";
  private static final String VERSION    = "VERSION";
  private static final String KEYWORDS   = "KEYWORDS";
  private static final String SOURCE     = "SOURCE";
  private static final String ORGANISM   = "ORGANISM";
  private static final String REFERENCE  = "REFERENCE";
  private static final String AUTHORS    = "AUTHORS";
  private static final String TITLE      = "TITLE";
  private static final String JOURNAL    = "JOURNAL";
  private static final String MEDLINE    = "MEDLINE";
  private static final String COMMENT    = "COMMENT";
  private static final String FEATURES   = "FEATURES";
  private static final String ORIGIN     = "ORIGIN";
    
  private static final String DEFAULT_ORGANISM = "Homo sapiens";
    
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private PrintStream out = null;
  private CurationSet cs = null;
  private String organism = DEFAULT_ORGANISM;
  SequenceI refseq;

  public GenbankReport(CurationSet cs) {
    setCurationSet(cs);
    organism = cs.getOrganism();
    if (organism == null)
      organism = "";

    refseq = cs.getRefSequence();
    if (refseq == null)
      logger.debug("GenbankReport: warning: refseq is null");
  } // GenbankReport constructor


    /**
     * Gets the value of cs
     *
     * @return the value of cs
     */
  public CurationSet getCurationSet()  {
    return this.cs;
  }

  /**
   * Sets the value of cs
   *
   * @param cs Value to assign to this.cs
   */
  public void setCurationSet(CurationSet cs) {
    this.cs = cs;
  }

  public PrintStream getPrintStream() {
    return this.out;
  }

  public void setPrintStream(PrintStream out) {
    this.out = out;
  }

  /** Properties that were saved from the GenBank-format input were
   *  saved verbatim, including newlines, and we don't need to add the
   *  keyword to the first line because it's there already. */
  public String getLocus() {
    if (getProp(LOCUS) != null)
      return(getProp(LOCUS, ""));

    // If we don't have sequence, refseq could be null.  What then?
    String acc = refseq.getAccessionNo();
    if (acc == null)
      acc = refseq.getName();
    String res_type = (refseq.getResidueType() == SequenceI.RNA) ? "mRNA" : "DNA ";
    return GenbankFormatter.getHeading(LOCUS) +
      GenbankFormatter.padBetweenWithSpaces(acc, refseq.getLength()+"", 28) + 
      " bp    " + res_type +"    linear   " + 
      division(organism) + " " +
      DateUtil.dateNoTime(new Date()) + "\n";
  }

  public String getDefinition() {
    if (getProp(DEFINITION) != null)
      return getProp(DEFINITION, "");

    String def = refseq.getDescription();
    if (def == null || def.equals(""))
      def = refseq.getAccessionNo();
    if (def == null || def.equals(""))
      def = cs.getName();
    return GenbankFormatter.breakSingle(GenbankFormatter.getHeading(DEFINITION) + def + "\n");
  }

  public String getAccession() {
    if (getProp(ACCESSION) != null)
      return getProp(ACCESSION, "");
    else {
      String acc = refseq.getAccessionNo();
      if (acc == null)
        acc = ".";
      return GenbankFormatter.getHeading(ACCESSION) + acc + "\n";
    }
  }

  public String getVersion() {
    if (getProp(VERSION) != null)
      return getProp(VERSION, "");
    else
      return GenbankFormatter.getHeading(VERSION) + "." + "\n";
  }

  public String getKeywords() {
    if (getProp(KEYWORDS) != null)
      return getProp(KEYWORDS, "");
    else
      return GenbankFormatter.getHeading(KEYWORDS) + "." + "\n";
  }

  public String getSource() {
    if (getProp(SOURCE) != null)
      return getProp(SOURCE, "");

    String org = organism.equals("") ? "." : organism;
    return GenbankFormatter.getHeading(SOURCE) + org + commonName(organism) + "\n";
  }

  public String getOrganism() {
    // Sometimes organism is already included in SOURCE
    if (getProp(SOURCE) != null && 
        getProp(SOURCE).indexOf(ORGANISM) > 0)
      return "";

    if (getProp(ORGANISM) != null)
      return getProp(ORGANISM, "");

    String org = organism.equals("") ? "." : organism;
    return GenbankFormatter.getSubHeading(ORGANISM) + org + taxonomy(organism) + "\n";
  }

  public String getReference() {
    if (getProp(REFERENCE) != null)
      return getProp(REFERENCE, ".");

    // If there is no REFERENCE record, make one
    // REFERENCE   1  (bases 1 to 384)
    int start = cs.getStart();
    int end = cs.getEnd();
    // Should this use chromosome coords or local coords??
    return GenbankFormatter.getHeading(REFERENCE) + "1  (bases " + start + " to " + end + ")\n";
  }

  public String getComment() {
    if (getProp(COMMENT) != null)
      return getProp(COMMENT, "");
    else
      return "";
  }

  public void printHeader() {
    out.print(getLocus());
    out.print(getDefinition());
    out.print(getAccession());
    out.print(getVersion());
    out.print(getKeywords());
    out.print(getSource());
    out.print(getOrganism());
    out.print(getReference());
    out.print(getComment());
  }

  /** Ranges should be relative to source sequence (i.e. start from one, not
   *  start from position on chrom arm) */
  private String getRange(RangeI r) {
    int start = r.getStart();
    int end = r.getEnd();
    return localCoords(start, end);
  }

  /** Convert to local coordinates */
  private String localCoords(int start, int end) {
    start = start - cs.getStart() + 1;
    end = end - cs.getStart() + 1;
    return printRange(start, end);
  }

  /** Ranges where start > end need to be written as complements */
  private static String printRange(int s, int e) {
    if (s > e)
      return "complement(" + e + ".." + s + ")";
    else
      return s + ".." + e;
  }

  private static String printRange(String s, String e, String before_s, String before_e) {
    try {
      // We need to figure out whether start > end, so need to extract the
      // integer portion of these strings (stripping off '>' or '<')
      String s_int = s;
      if (!(before_s == null || before_s.equals("")))
        s_int = s_int.substring(1);
      String e_int = e;
      if (!(before_e == null || before_e.equals(""))) {
        e_int = e_int.substring(1);
        // Also check at end of string
        if (e_int.indexOf(">") > 0)
          e_int = e_int.substring(0, e_int.indexOf(">"));
      }

      int start = (int)(Integer.parseInt(s_int));
      int end = (int)(Integer.parseInt(e_int));
      if (before_s == null)
        before_s = "";
      if (before_e == null)
        before_e = "";
      if (start > end)
        // Need to change around before_e and before_s??
        return "complement(" + before_e + e + ".." + before_s + s + ")";
      else
        return before_s + s + ".." + before_e + e;
    }
    catch (Exception err) {
      logger.error("printRange: couldn't parse range " + s + "-" + e, err);
      return "";
    }
  }

  public void printTranscript(Transcript transcript) {
    if (transcript.getTranslationStart() == 0) {
      try {
        logger.debug("translation start was 0 for " + transcript.getName() + "--setting to first codon");
        transcript.calcTranslationStartForLongestPeptide();
      } catch (NullPointerException e) {
      }
    }
    
    AnnotatedFeatureI gene = transcript.getGene();
    String id = gene.getId();
    if (id.equals("") || id.equals("no_name"))
      id = gene.getName();

    printmRNA(transcript);
    out.println(GenbankFormatter.getFeatureItem("locus_tag",id));
    //    out.println(GenbankFormatter.getFeatureItem("product",transcript.getName()));
    printDbXref(transcript);
    printOtherFields(transcript, "mRNA");

    // Now print the CDS
    printCDS(transcript);
    out.println(GenbankFormatter.getFeatureItem("locus_tag",id));
    out.println(GenbankFormatter.getFeatureItem("product",GenbankAdapter.getSequenceName(transcript.getGene(), transcript.getPeptideSequence(), transcript)));
    out.println(GenbankFormatter.getFeatureItem("prot_desc",GenbankAdapter.getProteinDesc(transcript.getGene(), transcript)));
    out.println(GenbankFormatter.getFeatureItem("protein_id", transcript.getProperty("protein_id")));
    printFunction(transcript);
    printNotes(transcript);
//     out.println(GenbankFormatter.getFeatureItem("codon_start",
//                                                 String.valueOf(transcript.getTranslationStart()-
//                                                                transcript.getStart()+1)));
    String codon_start = transcript.getProperty("codon_start"); // ??
    if (codon_start == null || codon_start.equals(""))
      codon_start = "1";
    out.println(GenbankFormatter.getFeatureItem("codon_start", codon_start));
                                                 
    // Official GenBank records only seem to have codon_start, not codon_stop
    // 	out.println(GenbankFormatter.getFeatureItem("codon_stop",
    // 						    String.valueOf(transcript.getTranslationEnd()-
    // 								   transcript.getStart()+1)));
    printDbXref(transcript);
    try {
      // Uh oh!  This is making the transcript retranslate its peptide.
      out.println(GenbankFormatter.getFeatureItem("translation",
                                                  transcript.getPeptideSequence().getResidues()));
    } catch (NullPointerException e) {
    }
    printOtherFields(transcript, "CDS");
  }

  public void printGene(AnnotatedFeature gene) {
    // Type might not be gene--might be, e.g., transposable_element,
    // which in GenBank records is recorded as a repeat_region with a /transposon
    // record for the transposon name.
    // Need comprehensive list of GenBank annotation types!
    String type = gene.getFeatureType();
    String type_for_name = type;
    if (type.equals("transposable_element")) {
      type = "repeat_region";
      type_for_name = "transposon";
    }
    else if (type.length() > 15) { // any longer won't fit!
      type = type.substring(0,15);
      type_for_name = type;
    }
    out.println(GenbankFormatter.getFeatureHeading(type, getRange(gene)));
    out.println(GenbankFormatter.getFeatureItem(type_for_name, gene.getName()));
    String id = gene.getId();
    if (id.equals("") || id.equals("no_name"))
      id = gene.getName();
    out.println(GenbankFormatter.getFeatureItem("locus_tag",id));
    //    if (gene.getProperty("cyto_range") != null)
    //      out.println(GenbankFormatter.getFeatureItem("map",gene.getProperty("cyto_range")));
    printFunction(gene);
    printNotes(gene);
    printDbXref(gene);
    printOtherFields(gene, "gene");

    // Only print CDS and mRNA for protein-coding genes!
    if (gene.isProteinCodingGene())
      for (Iterator iter = gene.getFeatures().iterator(); iter.hasNext();) {
        // printTranscript prints the CDS and mRNA records.
        printTranscript((Transcript)iter.next());
      }
  }

  /** mRNA is just all the exons */
  private void printmRNA(Transcript transcript) {
    // Make a join for the exons, e.g.
    // mRNA             join(123652..124122,124182..125096,125194..125301)
    String mrna = "";
    int offset = cs.getStart() - 1;
    int numExons = transcript.getExons().size();
    if (numExons > 1)
      mrna += "join(";
    Vector exons = transcript.getExons();
    // This doesn't seem to get the right answers--some transcripts should
    // answer yes to isMissing5prime yet none do.
    String trans_start = transcript.isMissing5prime() ? "<" : "";
    String trans_stop = transcript.isMissing3prime() ? ">" : "";
    logger.debug("GenbankReport.printmRNA: for " + transcript.getName() + ", trans_start = " + trans_start);
    for (int i = 0; i < numExons; i++) {
      Exon exon = (Exon)exons.elementAt(i);
      int start = exon.getStart() - offset;
      int end = exon.getEnd() - offset;
      String exon_start = (trans_start.equals("<") && i == 0) ? trans_start : "";
      String exon_stop = (trans_stop.equals(">") && i == numExons-1) ? trans_stop : "";
      mrna += printRange(start+"", end+"", exon_start, exon_stop);
      if (i < numExons-1)
        // Include a space after the comma for convenience when splitting the line
        mrna += ", ";
    }
    if (numExons > 1)
      mrna += ")";
    String multiLinemRNA = GenbankFormatter.breakSingle(GenbankFormatter.getFeatureHeading("mRNA", mrna));
    // Get rid of any spaces after commas
    out.println(multiLinemRNA.replaceAll(", ", ","));
  }

  /** CDS is the translation start to the donor splice of the exon, followed by
      middle exons, ending with last acceptor splice to the final base of the
      stop codon. */
  private void printCDS(Transcript transcript) {
    AnnotatedFeatureI gene = transcript.getGene();
    String gene_has_start = GenbankAdapter.get5primeStart (gene);
    String gene_has_stop = GenbankAdapter.get3primeStop (gene);
    String gb_type = GenbankAdapter.getGenbankType (gene.getFeatureType());
    //    boolean not_a_gene = (!(gene.isProteinCodingGene()));  // not used (?)
    String CDS_start = (gene_has_start.equals("") ?
                        GenbankAdapter.get5primeStart (transcript) :
                        gene_has_start);
	  
    String CDS_stop = (gene_has_stop.equals("") ?
                       GenbankAdapter.get3primeStop (transcript) :
                       gene_has_stop);

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
    int offset = cs.getStart() - 1;
    int end_base = transcript.getLastBaseOfStopCodon();
    Vector exons = transcript.getFeatures();
    int numExons = exons.size();
    boolean found_end = false;
    StringBuffer buf = new StringBuffer();
    if (numExons > 1)
      buf.append("join(");

    for (int i = start_index; i < numExons && !found_end; i++) {
      Exon exon = (Exon) exons.elementAt (i);
      boolean contains_tss = (i == start_index);
      found_end = exon.contains(end_base);
      // start and end need to be strings rather than ints because they might include
      // > or <
      String start;
      if (contains_tss && phase == 0)
	start = CDS_start + (tss - offset);
      else if (contains_tss && phase > 0)
	start = CDS_start + (exon.getStart() - offset);
      else
	start = "" + (exon.getStart() - offset);

      int na_except_pos = 0;  // How does this get set??  GenbankAdapter doesn't seem to set it to anything other than 0.
      String end;
      if (na_except_pos > 0 && exon.contains (na_except_pos)) {
	end = "" + (na_except_pos - transcript.getStrand());
// 	if (contains_tss)
// 	  buf.append ("\tCDS\n");
// 	else
// 	  buf.append ("\n");
	end = "" + (na_except_pos + transcript.getStrand());
	contains_tss = false;
        //	except = validator.getException (transcript);  // ?
      }
      if (found_end)
	end = "" + (end_base - offset) + CDS_stop;
      else
	end = "" + (exon.getEnd() - offset);

      buf.append(printRange(start, end, CDS_start, CDS_stop));
      
      //      if (contains_tss)
      //	buf.append ("\tCDS\n");
      //      else
      //	buf.append ("\n");

      if (i < numExons-1)
        // Include a space after the comma for convenience when splitting the line
        buf.append(", ");
    }

    if (numExons > 1)
      buf.append(")");
    String multiLineCDS = GenbankFormatter.breakSingle(GenbankFormatter.getFeatureHeading("CDS", buf.toString()));
    // Get rid of any spaces after commas
    out.println(multiLineCDS.replaceAll(", ", ","));
  }

  private static String getDbXrefStr(DbXref dx) {
    String xref = dx.getIdValue();
    // Sometimes id already starts with db name, e.g. GO:0005079
    if (xref.startsWith(dx.getDbName()))
      return xref;
    else
      return dx.getDbName() + ":" + xref;
  }

  public void printNotes(AnnotatedFeatureI annotation) {
    if (annotation.getProperty("notes") != null &&
        !(annotation.getProperty("notes").equals(""))) {
      out.println(GenbankFormatter.getFeatureItem("notes",annotation.getProperty("notes")));
      return;
    }
    StringBuffer b = new StringBuffer();
    for (Iterator iter = annotation.getComments().iterator(); iter.hasNext();) {
      Comment comment = (Comment)iter.next();
      b.append(comment.getText());
      if (iter.hasNext()) {
        b.append(";");
      }
    }
    if (b.length() > 0) {
      out.println(GenbankFormatter.getFeatureItem("note", b.toString()));
    }
  }

  public void printFunction(AnnotatedFeature annotation) {
    if (annotation.getProperty("function") != null &&
        !(annotation.getProperty("function").equals(""))) {
      out.println(GenbankFormatter.getFeatureItem("function",annotation.getProperty("function")));
      return;
    }

    if (annotation.getDescription() == null ||
        annotation.getDescription().equals(""))
      return;
    out.println(GenbankFormatter.getFeatureItem("function", annotation.getDescription()));
  }

  public void printDbXref(AnnotatedFeatureI annotation) {
    for (Iterator iter = annotation.getDbXrefs().iterator(); iter.hasNext();) {
      DbXref dx = (DbXref)iter.next();
      out.println(GenbankFormatter.getFeatureItem("db_xref",getDbXrefStr(dx)));
    }
  }

  public void printFeatures() {
    if (cs == null) {
      return;
    }
    out.println(GenbankFormatter.getFeature(FEATURES)+"Location/Qualifiers");
    FeatureSetI annotations = cs.getAnnots();
    printSeqSource(annotations);
    for (int i = 0; i < annotations.size(); i++) {
      AnnotatedFeature gene = (AnnotatedFeature)annotations.getFeatureAt(i);
      printGene(gene);
    }
  }

  /** Print source record for reference seq */
  private void printSeqSource(RangeI annotations) {
    // Use local coords for FEATURES:source
    out.println(GenbankFormatter.getFeatureHeading("source", getRange(refseq.getRange())));
    out.println(GenbankFormatter.getFeatureItem("organism", organism));
//     if (getProp("sub_species", "") != null)
//       out.println(GenbankFormatter.getFeatureItem("sub_species", getProp("sub_species", "")));
//     if (getProp("organelle", "") != null)
//       out.println(GenbankFormatter.getFeatureItem("organelle", getProp("organelle", "")));
    if (getProp("mol_type") == null)
      out.println(GenbankFormatter.getFeatureItem("mol_type", "genomic DNA"));
    else
      out.println(GenbankFormatter.getFeatureItem("mol_type", getProp("mol_type", "")));
    if (cs.getChromosome() != null)
      out.println(GenbankFormatter.getFeatureItem("chromosome", cs.getChromosome()));
    //    out.println(GenbankFormatter.getFeatureItem("map", );  // cytology
    printOtherFields(null, "source");
  }

  // Print the actual DNA sequence, which starts with "ORIGIN"
  private void printSequence() {
    if (refseq.getResidues() == null)
      return;
    out.println(GenbankFormatter.getHeading(ORIGIN));
    out.println(GenbankFormatter.formatSequence(refseq.getResidues()));
  }

  public void printFooter() {
    out.println("//");
  }

  public void writeReport(PrintStream out) {
    if (out == null) {
      logger.error("GenbankReport.writeReport: PrintStream is null");
      return;
    }
    setPrintStream(out);
    printHeader();
    printFeatures();
    printSequence();
    printFooter();
  }

  public static void save(CurationSet cs, String filename) {
    try {
      // Do smart path completion
      filename = apollo.util.IOUtil.findFile(filename, true);
      File file = new File(filename);  // is filename required to exist??
      logger.info("Saving GenBank report (in human-readable format) to file " + filename);
      GenbankReport.save(cs, file);
    } catch (Exception e) {
      logger.error("Error in GenbankReport.save(cs, " + filename +"): " + e, e);
      return;
    }
    logger.info("Done saving GenBank report");
  }

  public static void save(CurationSet cs, File file) {
    try {
      if (file == null) {
        logger.error("GenbankReport.save: file pointer is null");
      }
      logger.info("Saving human-readable GenBank report in file " + file + "...");
      // Why are we generating a new one of ourself?
      GenbankReport report = new GenbankReport(cs);
      PrintStream out = new PrintStream(new FileOutputStream(file));
      report.writeReport(out);
      out.close();
    } catch (Exception e) {
      logger.error("Error saving GenBank report in " + file + ": " + e, e);
    }
  }

  private static String commonName(String org) {
    if (org.equals("Homo sapiens"))
      return " (human)";
    else if (org.equals("Drosophila melanogaster"))
      return " (fruit fly)";
    else
      return "";
  }

  private static String taxonomy(String org) {
    if (org.equals("Homo sapiens"))
      return "\n            Eukaryota; Metazoa; Chordata; Craniata; Vertebrata; Euteleostomi;\n            Mammalia; Eutheria; Primates; Catarrhini; Hominidae; Homo.";
    else if (org.equals("Drosophila melanogaster"))
      return "\n            Eukaryota; Metazoa; Arthropoda; Hexapoda; Insecta; Pterygota;\n            Neoptera; Endopterygota; Diptera; Brachycera; Muscomorpha;\n            Ephydroidea; Drosophilidae; Drosophila.";
    else
      return "";
  }

  private static String division(String org) {
    if (org.equals("Homo sapiens"))
      return "PRI";
    else if (org.equals("Drosophila melanogaster"))
      return "INV";
    else
      return "   ";
  }

  private String getProp(String field) {
    return getProp(field, null);
  }

  private String getProp(String field, String defaultValue) {
    if (!(refseq instanceof GenbankSequence))
      return defaultValue;
    else {
      GenbankSequence gs = (GenbankSequence)refseq;
      if (gs.getProperty(field) == null)
        return defaultValue;
      else
        return gs.getProperty(field);
    }
  }

  /** Print any fields we haven't already explicitly printed (they
   *  should be stored as properties) */
  private void printOtherFields(AnnotatedFeature feature, String whatKind) {
    if (whatKind.equals("mRNA") || whatKind.equals("CDS")) {
      Transcript transcript = (Transcript)feature;
      Hashtable props = transcript.getProperties();
      printProps(props, whatKind);
    }
    else if (whatKind.equals("gene")) {
      AnnotatedFeature gene = (AnnotatedFeature)feature;
      Hashtable props = gene.getProperties();
      printProps(props, whatKind);
    }
    else if (whatKind.equals("source")) {
      if (!(refseq instanceof GenbankSequence))
        return;
      GenbankSequence gs = (GenbankSequence)refseq;
      Hashtable props = gs.getProperties();
      printProps(props, whatKind);
    }
  }

  private void printProps(Hashtable props, String whatKind) {
    if (props != null) {
      Enumeration en  = props.keys();
      while (en.hasMoreElements()) {
        String key = (String)en.nextElement();
        if (wantedField(key, whatKind))
          out.println(GenbankFormatter.getFeatureItem(key, (String)props.get(key)));
      }
    }
  }

  private boolean wantedField(String field, String whatKind) {
    if (whatKind.equals("mRNA")) {
      // Already printed: locus_tag, product
      if (field.equals("locus_tag") || field.equals("product") ||
          // These belong in CDS record, not mRNA record, but are associated
          // with transcript, so we need to explicitly exclude them.
          field.equals("codon_start") || field.equals("translation") ||
          field.equals("protein_id") || field.equals("db_xref"))
        return false;
      else
        return true;
    }
    else if (whatKind.equals("CDS")) {
      if (field.equals("locus_tag") || field.equals("product") ||
          field.equals("prot_desc") || field.equals("protein_id") ||
          field.equals("codon_start") || field.equals("translation"))
        return false;
      else
        return true;
    }
    else if (whatKind.equals("gene")) {
      // Already printed: locus_tag, function, notes, dbxrefs
      if (field.equals("locus_tag") || field.equals("function") ||
          field.equals("notes") || field.equals("db_xref"))
        return false;
      else
        return true;
    }
    else if (whatKind.equals("source")) {
      if (field.equals("organism") || field.equals("mol_type") ||
          field.equals("chromosome") || field.equalsIgnoreCase("DEFINITION") ||
          field.equalsIgnoreCase("REFERENCE") || field.equalsIgnoreCase("FEATURES") ||
          field.equalsIgnoreCase("ORIGIN") || field.equalsIgnoreCase("SOURCE") ||
          field.equalsIgnoreCase("VERSION") || field.equalsIgnoreCase("ACCESSION") ||
          field.equalsIgnoreCase("KEYWORDS") || field.equalsIgnoreCase("LOCUS") ||
          field.equalsIgnoreCase("COMMENT"))
        return false;
      else
        return true;
    }

    return true;
  }

} // GenbankReport
