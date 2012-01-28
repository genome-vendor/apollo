// Note to self: to test roundtripping, use XORTDiff.pl:
//    ~/apollo/bin/apollo -roundtrip -cx ~/apollo/data/chado/small.chado -cx ~/apollo/data/chado/small.saved.chado
//    pushd ~nomi/chado; setenv CodeBase `pwd`; popd
//    ~nomi/chado/XORT/bin/XORTDiff.pl -f ~/apollo/data/chado/small.chado -s ~/apollo/data/chado/small.saved.chado

package apollo.dataadapter.chadoxml;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JOptionPane;
import apollo.config.Config;
import apollo.config.ApolloNameAdapterI;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.gamexml.TransactionXMLAdapter;
import apollo.datamodel.*;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.Transaction;
import apollo.editor.UserName;
import apollo.util.DateUtil;
import org.apache.log4j.*;

import org.bdgp.xml.XML_util;

/** ChadoXmlWrite: writes Chado XML to a file.
 *  Main method is writeXML. 
 *  Please note that this is still somewhat FlyBase-specific, though some
 *  of the constants are now configurable in the style file. */

public class ChadoXmlWrite {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoXmlWrite.class);

  // The following constants can be changed in the style file by putting in lines
  // of the form
  // TypeIDOntology               "SO"
  static String typeIDOntology = "SO";  // Used when writing out type_id's
  static String IDForDbxref = "SO:0000105";  // SO id for dbxrefs
  // In case we can't figure out the database when writing xrefs
  static String defaultChadoDatabase = "FlyBase";
  static String database = defaultChadoDatabase;  // May get changed later
  static String defaultSynonymAuthor = "gadfly3";
  static String defaultSynonymPubType = "computer file";
  // String used to identify cv name in feature props (was "property type").  
  // Should make configurable.
  static String featurepropCV = "annotation property type";

  static String TAB = "  ";  // !! Change to a real tab when we're ready to release
  static String CVTermForResults = "match";
  static String chromosome = "";
  static ApolloNameAdapterI nameAdapter = null;
  static String CHADO_XML_VERSION = "FlyBase v1.0, no macros";

  /** Mention whether an annotation may have been edited, but do so only once. */
  static String mayHaveBeenEditedMessage = "";

  /** Main method for writing out Chado XML */
  public static boolean writeXML(CurationSet curation, 
                                 String file_str,
                                 String preamble, // <!> lines roundtripped from input
                                 boolean saveAnnots,
                                 boolean saveResults,
                                 ApolloNameAdapterI localNameAdapter,
                                 // Apollo version
                                 String version) {
    nameAdapter = localNameAdapter;
    String filename = apollo.util.IOUtil.findFile(file_str, true);
    if (filename == null) {
      String message = "Failed to open file for writing: " + file_str + "\nThe directory may not exist, or may be unwriteable.";
      logger.warn(message);
      JOptionPane.showMessageDialog(null,
                                    message,
                                    "Warning",
                                    JOptionPane.WARNING_MESSAGE);
      return false;
    }

    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(file_str);
    } 
    catch ( Exception ex ) {
      logger.error("writeXML: caught exception opening " + file_str + 
                   " (" + filename + ")", ex);
      return false;
    }
    if (fileWriter == null) 
      return false;

    String what = "annotations and evidence";
    if (!saveAnnots)
      what = "evidence (results) only";
    if (!saveResults)
      what = "annotations only (no results)";
    String msg = "Saving " + what + " to file " + filename;
    logger.info(msg);
    boolean success = writeXML(curation, fileWriter, preamble, saveAnnots, saveResults, nameAdapter, version);

    try {
      fileWriter.close();
    }
    catch ( Exception ex ) {
      logger.error("writeXML: caught exception closing " + filename, ex);
      return false;
    }
    return success;
  }

  /** This currently assumes CurationSet is the root object,  and
   *  that all its children do not contain references to other
   *  AnnotatedSeqs that would need to be included in the XML doc.
   *  Returns false if something went wrong. 
   *  Preamble is the lines before <chado> that were retrieved from
   *  the original input file by ChadoXmlAdapter.getPreamble. */
  public static boolean writeXML(CurationSet curation,
                                 FileWriter fileWriter,
                                 String preamble, // header lines roundtripped from input
                                 boolean saveAnnots,
                                 boolean saveResults,
                                 ApolloNameAdapterI nameAdapter,
				 String version) {
    initConstants();
    String startingIndent = TAB;
    // Do the actual writing
    BufferedWriter bw = new BufferedWriter(fileWriter);
    PrintWriter pw = new PrintWriter(bw);
    try {
      pw.print(writeBegin(version));
      pw.print("\n" + preamble);

      // Write the actual <chado> line
      pw.print("<chado date=\"" + DateUtil.toGMT(new Date()) + "\">\n");
      writeGenomePosition(curation, startingIndent, pw);  // includes residues
      if (saveAnnots)
        writeAnnotations(curation, startingIndent, pw);
      if (saveResults)
        writeResults(curation, startingIndent, pw);
      pw.print(writeEnd());
      pw.close();
    } 
    catch ( Exception ex ) {
      logger.error("Caught exception committing XML", ex);
      return false;
    }
    return true;
  }

  /** Initialize some of the constants used when writing out ChadoXML for FlyBase.
   *  These are defined in fly.style (or whichever style is used for the ChadoXML
   *  adapter) and read in as generic parameters by Style. */
  private static void initConstants() {
    if (!(Config.getStyle().getParameter("typeIDOntology")).equals(""))
      typeIDOntology = Config.getStyle().getParameter("typeIDOntology");
    if (!(Config.getStyle().getParameter("IDForDbxref")).equals(""))
      IDForDbxref = Config.getStyle().getParameter("IDForDbxref");
    if (!Config.getStyle().getParameter("defaultChadoDatabase").equals("")) {
      defaultChadoDatabase = Config.getStyle().getParameter("defaultChadoDatabase");
      logger.debug("Got new setting for defaultDatabase: " + defaultChadoDatabase);
      database = defaultChadoDatabase;
    }
    if (!Config.getStyle().getParameter("defaultSynonymAuthor").equals(""))
      defaultSynonymAuthor = Config.getStyle().getParameter("defaultSynonymAuthor");
    if (!Config.getStyle().getParameter("defaultSynonymPubType").equals(""))
      defaultSynonymPubType = Config.getStyle().getParameter("defaultSynonymPubType");
    if (!Config.getStyle().getParameter("featurepropCV").equals(""))
      featurepropCV = Config.getStyle().getParameter("featurepropCV");
  }

  /** Write the ChadoXML header */
  private static String writeBegin(String ApolloVersion) {
    StringBuffer buf = new StringBuffer();
    /** Version of XML itself, not of ChadoXML or this writer or Apollo */
    String XML_VERSION = "1.0";
    buf.append("<?xml version=\"" + XML_VERSION + "\" encoding=\"ISO-8859-1\"?>\n");
    buf.append("<!-- ChadoXML file (" + CHADO_XML_VERSION + ") created by " + UserName.getUserName() + " on " + DateUtil.toGMT(new Date()) + " -->\n");
    buf.append("<!-- " + ApolloVersion + " -->\n");
    return buf.toString();
  }

  private static void writeGenomePosition (CurationSet curation, String indent, PrintWriter pw) {
    SequenceI genomic_seq = curation.getRefSequence();
    pw.print(indent + "<_appdata  name=\"title\">" + curation.getName() + "</_appdata>\n");
    chromosome = curation.getChromosome();
    if (chromosome == null || chromosome.equals("")) {
      logger.warn("couldn't find chromosome for curation " + curation.getName());
      chromosome = "";
    }
    pw.print(indent + "<_appdata  name=\"arm\">" + chromosome + "</_appdata>\n");
    // Convert start back to interbase by subtracting 1
    pw.print(indent + "<_appdata  name=\"fmin\">" + (curation.getStart()-1) + "</_appdata>\n");
    pw.print(indent + "<_appdata  name=\"fmax\">" + curation.getEnd() + "</_appdata>\n");

    writeGenomicSequence((AbstractSequence) genomic_seq,
                         indent + "",
                         true,  // true means include residues
                         pw);

    // If there were genomic sequencing errors attached to this sequence 
    // (seq.getGenomicErrors() != null) should print warning that we're not
    // saving them (don't know how to).
    if (genomic_seq != null && genomic_seq.getGenomicErrors() != null) {
      String message = "there are genomic sequencing errors annotated, but I don't know how to save them in ChadoXML!";
      logger.warn(message);
      JOptionPane.showMessageDialog(null,
                                    message,
                                    "Warning",
                                    JOptionPane.WARNING_MESSAGE);
    }
  }

  private static void writeGenomicSequence(SequenceI seq,
                                           String indent,
                                           boolean include_residues,
                                           PrintWriter pw) {
    if (seq == null)
      return;

    // This commented-out code is how the GAME writer writes genomic and sequencing
    // errors.  The Chado XML adapter does not yet capture these.
//     /* This is the genomic and sequencing errors that have been noted */
//     HashMap seq_errors = seq.getGenomicErrors();
//     if (seq_errors != null) {
//       Iterator positions = seq_errors.keySet().iterator();
//       while (positions.hasNext()) {
//         String position = (String) positions.next();
//         SequenceEdit seq_edit = (SequenceEdit) seq_errors.get(position);
//         pw.print(indent + "  <potential_sequencing_error>\n");
//         pw.print(indent + "    <type>" + 
//                    seq_edit.getEditType() + "</type>\n");
//         pw.print(indent + "    <position>" + 
//                    seq_edit.getPosition() + "</position>\n");
//         if (seq_edit.getResidue() != null)
//           pw.print(indent + "      <base>" + 
//                      seq_edit.getResidue() + "</base>\n");
//         pw.print(indent + "  </potential_sequencing_error>\n");
//       }
//     }

    // Write out the genomic residues
    if (include_residues &&
        seq.getResidues() != null &&
        ! seq.getResidues().equals("")) {
      pw.print(indent + "<_appdata  name=\"residues\">");
      pw.print(seq.getResidues());
      pw.print("</_appdata>\n");
    }
  }

  private static void writeAnnotations(CurationSet curation, String indent, PrintWriter pw) {
    StrandedFeatureSetI annots = curation.getAnnots();
    if (annots != null) {
      AnnotatedFeatureI prevAnnot = null;
      AnnotatedFeatureI annot = null;
      for (int i = 0; i < annots.size(); i++) {
        prevAnnot = annot;
	annot = (AnnotatedFeatureI) annots.getFeatureAt(i);

        // 5/05: P elements have no strand defined in the input.  For
        // now, I have guessed the strand by checking whether
        // fmax>fmin.  In some cases, fmin==fmax, so strand stayed 0,
        // in which case it gets added to both + and - strands, and
        // then when you save, the P elements would get saved twice.
        // Here I prevent the double saving by checking that this
        // annot isn't the same as the previous annot, but this seems
        // like a data problem
        if (prevAnnot != null && (annot.getId() == null || (annot.getId().equals(prevAnnot.getId()))))
          logger.info("Annotation " + annot.getId() + "has strand=0 so it got added to both strands.  Saving single copy.");
        else
          writeAnnotation (curation, annot, indent, pw);
      }
    }
  }

  private static void writeAnnotation (CurationSet curation, 
                                       AnnotatedFeatureI annot,
                                       String startingIndent,
                                       PrintWriter pw) {
    pw.print(startingIndent + "<feature>\n");
    String indent = startingIndent + TAB;

    // Write primary dbxref (if any) for annot
    DbXref xref = ((SeqFeature)annot).getPrimaryDbXref();
    if (xref != null)
      writeXref(indent, xref.getDbName(), xref.getIdValue(),xref.getVersion(), false, -1, pw);
    else
      logger.warn("couldn't find primary xref for annotation with uniquename " + annot.getId()); // DEL

    pw.print(indent + "<is_analysis>0</is_analysis>\n");
    if (!(annot.getProperty("is_obsolete")).equals(""))
      pw.print(writeField(indent, "is_obsolete", annot.getProperty("is_obsolete")));
    pw.print(indent + "<name>" + XML_util.transformToPCData (annot.getName()) + "</name>\n");
    // Write out any other generic fields now
    writeFields(indent, annot, pw);

    // Write organism ID
    writeOrganismFromFeature(indent, annot, curation, pw);

    // Write seqlen (though I don't think this makes a lot of sense--*transcripts*
    // have a sequence length, but how can an annotation?)
    // Seqlen--calculate from start/end.
    // 12/2005: Don't write seqlen if we don't have transcripts
    if (annot.getFeatures().size() > 0) {
      SequenceI seq = annot.getRefSequence();
      if (seq != null && seq.getResidues() != null &&
          ! seq.getResidues().equals("")) {
        int seqlen;
        if (annot.getStart() > annot.getEnd())
          seqlen = annot.getStart() - annot.getEnd() + 1;
        else
          seqlen = annot.getEnd() - annot.getStart() + 1;

        pw.print(writeField(indent, "seqlen", seqlen+"")); // +"" is to turn the int into a string
      }
    }

    // Write timeaccessioned, timelastmodified
    writeTimes(indent, annot, pw);

    // Write type
    // Note that Apollo's datamodels (which mostly match GAME XML) encode pseudogenes (etc.)
    // as annot type = pseudogene, whereas in Chado, pseudogenes are encoded as annot type = gene,
    // transcript type = pseudogene.  So we need to adjust for this when writing.
    writeCVtype(indent, typeIDOntology, annotTypeForChado(annot), pw);

    // Write uniquename (ID)
    pw.print(writeField(indent, "uniquename", XML_util.transformToPCData(annot.getId())));

    boolean isa_gene = annot.getTopLevelType().equalsIgnoreCase ("gene");
    Vector transcripts = annot.getFeatures();
    for (int i = 0; i < transcripts.size(); i++) {
      Transcript fs = (Transcript) transcripts.elementAt (i);
      writeTranscript(indent, fs, isa_gene, curation, pw);
    }

    // Write owner (in chadoXML, mixed in with other featureprops)
    writeProperty(indent, "owner", annot.getOwner(), 0, pw);

    // Write annot comments (in chadoXML, they are mixed in with the other featureprops)
    writeComments(indent, annot, pw);

    // Write ALL feature_dbxrefs
    Vector xrefs = annot.getDbXrefs();
    for (int i = 0; i < xrefs.size(); i++) {
      xref = (DbXref) xrefs.elementAt (i);
      // true means write as feature_dbxref
      if (xref.isSecondary())
        writeXref(indent, xref.getDbName(), xref.getIdValue(), xref.getVersion(), true, xref.getCurrent(), pw);
      else
        logger.debug("Not writing primary dbxref " + xref.getIdValue() + " as feature_dbxref for " + annot.getId());
    }

    // Write synonyms
    writeSynonyms(indent, annot, pw);

    // Write featureloc for annotation
    writeFeatureloc(indent, annot, pw);

    // Write featureprops
    writeProperties(indent, annot, pw);

    // Special property: 
    // This is for boolean "problem" property, which can be set in annot info editor.
    // It is stored as isProblematic on the AnnotatedFeature, NOT as a featureprop,
    // so it won't get written out with the other properties.
    // Annot/transcript could also come in with a string for "problem"; this is stored
    // inside apollo as a "tag" and will be written out separately.
    if (annot.isProblematic())
      writeProperty(indent, "problem", "true", 0, pw);

    // Close tag
    pw.print(startingIndent + "</feature>\n");
  } // end of writeAnnotation

  /** Not currently used */
//   private static String getID (SeqFeatureI annot) {
//     String annot_id = annot.getId();
//     if (annot_id == null)
//       annot_id = annot.getName();
//     return annot_id;
//   }

  /** Write either a plain dbxref_id (if feature_dbxref is false) or a feature_dbxref,
   *  which has a <feature_dbxref> around the dbxref_id. 
   *  is_current is only used for feature_dbxref. */
  private static void writeXref (String indent, String db, String id, String ver, boolean feature_dbxref, int isCurrent, PrintWriter pw) {
    // Don't write xref for empty accession
    if (id == null || id.equals(""))
      return;

    String original_indent = indent;
    if (feature_dbxref) {
      pw.print(indent + "<feature_dbxref>\n");
      indent += TAB;
    }
    pw.print(indent + "<dbxref_id>\n");
    pw.print(indent + TAB + "<dbxref>\n");
    pw.print(writeField(indent + TAB + TAB, "accession", id));
    pw.print(indent + TAB + TAB + "<db_id>\n");
    pw.print(indent + TAB + TAB + TAB + "<db>\n");
    // Note: some db_id blocks include a contact_id block.  Leaving that out (we don't
    // have a good way to record it).
    pw.print(writeField(indent + TAB + TAB + TAB + TAB, "name", db));
    pw.print(indent + TAB + TAB + TAB + "</db>\n");
    pw.print(indent + TAB + TAB + "</db_id>\n");
    if(ver!=null)
	pw.print(writeField(indent + TAB + TAB,"version",ver));
    pw.print(indent + TAB + "</dbxref>\n");
    pw.print(indent + "</dbxref_id>\n");
    if (feature_dbxref) {
      pw.print(indent + "<is_current>" + isCurrent + "</is_current>\n");
      pw.print(original_indent + "</feature_dbxref>\n");
    }
  }

  private static void writeTranscript(String indent, Transcript transcript, 
                                      boolean isa_gene, CurationSet curation,
                                      PrintWriter pw) {
    pw.print(indent + "<feature_relationship>\n");
    pw.print(indent + TAB + "<subject_id>\n");
    pw.print(indent + TAB + TAB + "<feature>\n");

    // Write primary dbxref for transcript
    DbXref xref = transcript.getPrimaryDbXref();
    if (xref != null)
      writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(),xref.getVersion(), false, -1, pw);
    else
      logger.warn("couldn't find primary xref for transcript with uniquename " + transcript.getId()); // DEL

    pw.print(indent + TAB + TAB + TAB);
    pw.print("<is_analysis>0</is_analysis>\n");
    if (!(transcript.getProperty("is_obsolete")).equals(""))
      pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", transcript.getProperty("is_obsolete")));

    // Write md5checksum
    SequenceI seq = transcript.get_cDNASequence();
    String checksum = seq.getChecksum();
    if (checksum != null && !checksum.equals("")) {
      pw.print(writeField(indent + TAB + TAB + TAB, "md5checksum", checksum));
    }

    // Write name
    pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData (transcript.getName())));

    // Write out any other generic fields now
    writeFields(indent + TAB + TAB + TAB, transcript, pw);

    // Write organism ID
    writeOrganismFromFeature(indent + TAB + TAB + TAB, transcript, curation, pw);

    // Write residues
    if (seq != null && seq.getResidues() != null &&
        ! seq.getResidues().equals("")) {
      pw.print(writeField(indent + TAB + TAB + TAB, "residues", seq.getResidues()));
      pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seq.getResidues().length()+"")); // +"" is to turn the int into a string
    }

    // Write timeaccessioned, timelastmodified
    writeTimes(indent + TAB + TAB + TAB, transcript, pw);

    // Write type of transcript.
    writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, transcriptTypeForChado(transcript), pw);
    // Write uniquename
    pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(transcript.getId())));

    // Write exons
    boolean edited = mayHaveBeenEdited(transcript, curation);
    if (edited) {
      String message = transcript.getRefFeature().getName() + " may have been edited--updaing exon names and ids";
      if (!mayHaveBeenEditedMessage.equals(message)) {
        logger.info(message);
        mayHaveBeenEditedMessage = message;
      }
    }
    Vector exons = transcript.getFeatures();
    logger.debug("Writing transcript " + transcript.getName() + " (" + exons.size() + " exons)");
    for (int i = 0; i < exons.size (); i++) {
      Exon exon = (Exon) exons.elementAt(i);
      // If data was read from GAME, then exons didn't get proper names or IDs
      // (names will be no_name).  Reassign exon names/ids if necessary.
      if (edited || exon.getName().equals("no_name"))
        renameExon(exon, i, curation);  // Also assigns new id and rank
      writeExon(indent + TAB + TAB + TAB, exon, i, pw);
    }

    // Write protein
    if (isa_gene) {
      writePeptide(indent + TAB + TAB + TAB, transcript, curation, pw);
    }

    // Write featureloc
    writeFeatureloc(indent + TAB + TAB + TAB, transcript, pw);

    // Write feature_dbxrefs
    Vector xrefs = transcript.getDbXrefs();
    for (int i = 0; i < xrefs.size(); i++) {
      xref = (DbXref) xrefs.elementAt (i);
      if (xref.isSecondary())
        // true means write as feature_dbxref
        writeXref(indent + TAB + TAB + TAB,
                  xref.getDbName(),
                  xref.getIdValue(),xref.getVersion(), true, xref.getCurrent(), pw);
    }

    // Write feature_synonyms
    writeSynonyms(indent + TAB + TAB + TAB, transcript, pw);

    // Write owner (in chadoXML, it's mixed in with other featureprops)
    writeProperty(indent + TAB + TAB + TAB, "owner", transcript.getOwner(), 0, pw);

    // Write comments (in chadoXML, they are mixed in with the other featureprops)
    writeComments(indent + TAB + TAB + TAB, transcript, pw);

    // Write (other) featureprops
    writeProperties(indent + TAB + TAB + TAB, transcript, pw);

    // Write weird things (e.g. readthrough stop codon) as properties
    // (Do we want to do this?)
    if (isa_gene)
      writeWeirdTranscriptProperties(indent + TAB + TAB + TAB, transcript, pw);

    // Close tags
    pw.print(indent + TAB + TAB + "</feature>\n");
    pw.print(indent + TAB + "</subject_id>\n");

    // Write partof
    writeCVtype(indent + TAB, "relationship type", "partof", pw);
    pw.print(indent + "</feature_relationship>\n");
  }  // End of writeTranscript

  /** Write the peptide associated with transcript */
  private static void writePeptide(String indent, Transcript transcript, CurationSet curation, PrintWriter pw) {
    SequenceI seq = transcript.getPeptideSequence();
    Protein protFeat = transcript.getProteinFeat();

    if (seq == null) {
      logger.warn("peptide seq is null for transcript " + transcript.getName() + "--parent annot type is " + transcript.getRefFeature().getTopLevelType());
      return;
    }

    pw.print(indent + "<feature_relationship>\n");
    pw.print(indent + TAB + "<subject_id>\n");
    pw.print(indent + TAB + TAB + "<feature>\n");

    // Write peptide accesssion (if any) as a dbxref.
    // (All the dbxrefs will be written out later.)
    // (!! Are we sure the peptide accession was saved on the protein feature and
    // not on the sequence?)
    DbXref xref = protFeat.getPrimaryDbXref();
    if (xref != null)
      writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(),xref.getVersion(), false, -1, pw);

    pw.print(indent + TAB + TAB + TAB);
    pw.print("<is_analysis>0</is_analysis>\n");
    if (!(protFeat.getProperty("is_obsolete")).equals(""))
      pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", protFeat.getProperty("is_obsolete")));

    // Write md5checksum
    String checksum = seq.getChecksum();
    if (checksum != null && !checksum.equals("")) {
      pw.print(writeField(indent + TAB + TAB + TAB, "md5checksum", checksum));
    }

    // Write name
    pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData(seq.getName())));

    // Write out any other generic fields now
    writeFields(indent + TAB + TAB + TAB, protFeat, pw);

    // Write organism ID
    //writeOrganismFromSeq(indent + TAB + TAB + TAB, seq, transcript, pw);
    writeOrganismFromFeature(indent + TAB + TAB + TAB, protFeat, curation, pw);

    // Write residues and seqlen
    // If data originally came from GAME, there might be peptides that extend outside of
    // this genomic region, in which case seq.getResidues() will throw an exception.
    try {
      logger.debug("About to save peptide " + seq.getName());
      if (seq.getResidues() != null &&
          !(seq.getResidues().equals(""))) {
        pw.print(writeField(indent + TAB + TAB + TAB, "residues", seq.getResidues()));
        pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seq.getResidues().length()+"")); // +"" is to turn the int into a string
      }
    } catch (Exception e) {
      logger.error("Can't save residues for peptide " + seq.getName() + ": ", e);
    }

    // Write timeaccessioned, timelastmodified
    writeTimes(indent + TAB + TAB + TAB, protFeat, pw); // seq

    // Write type_id--CV type is protein
    writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "protein", pw);

    // Write uniquename
    String acc = seq.getAccessionNo();
    if (acc == null || acc.equals(""))
      acc = seq.getName();
    pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(acc)));
    // Write featureloc for seq
    writePepFeatloc(indent + TAB + TAB + TAB, protFeat, transcript, pw); // seq

    // Write feature_dbxrefs for protein
    for (int i = 0; i < protFeat.getDbXrefs().size(); i++) {
      xref = protFeat.getDbXref(i);
      if (xref.isSecondary())
        writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(), xref.getVersion(),true, xref.getCurrent(), pw);
    }

    // Write (other) featureprops that have not already been taken care of explicitly.
    writeProperties(indent + TAB + TAB + TAB, protFeat, pw); // seq

    // Write peptide synonyms (stored in protFeat)
    writeSynonyms(indent + TAB + TAB + TAB, protFeat, pw);

    // It looks like now peptides have a whole <feature_relationship> block here
    // explaining that they are producedby their parent mRNA (I don't think they used
    // to have that).
    // 8/2005: The very latest examples DON'T have this feature_relationship block.
    // Is it needed or not?  (Pinglei says no.)
    //     writePeptideFeatureRelationship(indent + TAB + TAB + TAB, transcript, curation, pw);

    // Close tags
    pw.print(indent + TAB + TAB + "</feature>\n");
    pw.print(indent + TAB + "</subject_id>\n");

    // Write producedby
    writeCVtype(indent + TAB, "relationship type", "producedby", pw);

    pw.print(indent + "</feature_relationship>\n");
  }

  private static void writeExon(String indent, Exon exon, int exonRank, PrintWriter pw) {
    pw.print(indent + "<feature_relationship>\n");
    writeExonRank(indent + TAB, exon, exonRank, pw);

    pw.print(indent + TAB + "<subject_id>\n");
    pw.print(indent + TAB + TAB + "<feature>\n");
    pw.print(indent + TAB + TAB + TAB);
    pw.print("<is_analysis>0</is_analysis>\n");
    if (!(exon.getProperty("is_obsolete")).equals(""))
      pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", exon.getProperty("is_obsolete")));

    // Write exon name
    // (If annot has been modified, exon name and rank will already have been changed)
    // has "temp" in it)
    pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData(exon.getName())));

    // Write out any other generic fields now
    writeFields(indent + TAB + TAB + TAB, exon, pw);

    // Write organism ID
    writeOrganismFromFeature(indent + TAB + TAB + TAB, exon, pw);

    // Seqlen--calculate from start/end?
    int seqlen;
    if (exon.getStart() > exon.getEnd())
      seqlen = exon.getStart() - exon.getEnd() + 1;
    else
      seqlen = exon.getEnd() - exon.getStart() + 1;

    pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seqlen+"")); // +"" is to turn the int into a string

    // Write timeaccessioned, timelastmodified
    writeTimes(indent + TAB + TAB + TAB, exon, pw);

    // Write type
    writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "exon", pw);

    logger.debug("Exon " + exon.getName() + ": " + exon.getLow() + "-" + exon.getHigh());

    // Write uniquename (ID)
    pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", exon.getId()));

    // Write featureloc
    writeFeatureloc(indent + TAB + TAB + TAB, exon, pw);

    pw.print(indent + TAB + TAB + "</feature>\n");
    pw.print(indent + TAB + "</subject_id>\n");

    // Write partof
    writeCVtype(indent + TAB, "relationship type", "partof", pw);
    pw.print(indent + "</feature_relationship>\n");
  }

  /**  3/2005: it's hard to make sure exon names and ids are correct, so for now,
       if ANY edits were done on ANY transcript belonging to the same annot as this
       one, then we'll invalidate ALL exon names and ids.  This method returns true
       if that's the case. */
  private static boolean mayHaveBeenEdited(Transcript transcript, CurationSet curation) {
    // Check transactions to see if any include this annotation--if so, this annotation
    // may have been edited.
    AnnotatedFeatureI annot = (AnnotatedFeatureI)transcript.getRefFeature();
    return someTransactionIncludesThisAnnot(annot, curation);
  }

  /** Returns true is there is a transaction that includes annotInQuestion */
  private static boolean someTransactionIncludesThisAnnot(AnnotatedFeatureI annotInQuestion, CurationSet curation) {
    if (curation.getTransactionManager() == null)
      return false;

    boolean edited = false;
    try {
      // should this be done on flattened trans??
      edited = curation.getTransactionManager().featureHasBeenEdited(annotInQuestion);
    } catch (Exception e) {
      logger.error("Error checking transactions: ", e);
    }
    return edited;
  }

  /** Exon (or transcript or annot it belongs to) may have been changed--assign it
   *  a new name, ID, and rank. */
  private static void renameExon(Exon exon, int exonNum, CurationSet curation) {
    //    SeqFeatureI annot = exon.getRefFeature().getRefFeature();
    exon.setName(nameAdapter.generateName(curation.getAnnots(), curation.getName(), exon));
    exon.setId(nameAdapter.generateId(curation.getAnnots(), curation.getName(), exon));
    // Should this go in the name adapter?
    assignExonRank(exon, exonNum);
  }

  // Not used here (maybe it was moved to name adapter)
//   /** This should really be done in the name adapter! 
//       Exon may have changed--assign temporary name based on gene name and exon range. */
//   private static void assignExonName(Exon exon, SeqFeatureI annot) {
//     if (exon.getName().indexOf("temp") >= 0)  // already includes "temp"
//       return;
//     // temp:genename:exonstart-exonend
//     exon.setName("temp:" + annot.getName() + ":" + exon.getLow() + "-" + exon.getHigh());
//   }

//   /** This should really be done in the name adapter! */
//   private static void assignExonId(Exon exon, SeqFeatureI annot) {
//     if (exon.getId().indexOf("temp") >= 0)  // already includes "temp"
//       return;
//     // temp:geneid:exonstart-exonend
//     exon.setId("temp:" + annot.getId() + ":" + exon.getLow() + "-" + exon.getHigh());
//   }

  /** Frank wrote (on 17 Mar 2005):
   *   The <rank> should be assigned sequentially and contiguously on a per
   *   transcript basis.  If CG12345-RA has 5 exons, they should have rank
   *   numbers 1,2,3,4,5.  (regardless of the rank numbers of those same
   *   exons in other transcripts of the same gene.)

   *   (Should this go in the name adapter?)
  */
  private static void assignExonRank(Exon exon, int rank) {
    // +"" is a trick to change an int to a String
    exon.replaceProperty("rank", Integer.toString(rank)); //rank+"");
  }

  /** If feature is a pair (e.g. a blast hit), it will have TWO featurelocs,
   *  for subject and query.
   *  If it is not a pair (e.g. an exon), it will only have a query featureloc. */
  private static void writeFeatureloc(String indent, SeqFeatureI sf, PrintWriter pw) {
    int strand = sf.getStrand();
    // Unstranded annotations may have been forced onto a strand--restore their strand=0
    if (sf.getProperty("unstranded").equals("true"))
      strand = 0;

    int start, end;
    if (sf instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair)sf;
      SeqFeatureI query = fp.getQueryFeature();
      SeqFeatureI subject = fp.getHitFeature();

      // Write featureloc for subject (hit) first
      // Convert start back to interbase by subtracting 1
      start = subject.getLow()-1;
      end = subject.getHigh();

      writeFeatureloc(indent, start, end, strand, 
		      // false, false is for is_fmin/max partial (can only be true for peptide)
		      false, false,
		      // true is for is subject
		      subject, true, pw);

      // Write featureloc for query
      start = query.getLow()-1;
      end = query.getHigh();
      // false means this is not the subject
      writeFeatureloc(indent, start, end, strand,
		      // false, false is for is_fmin/max partial (can only be true for peptide)
//		      isPartial(query, "fmin"), isPartial(query, "fmax"), 
		      false, false,
		      query, false, pw);
    }
    else {  // singleton
      // Convert start back to interbase by subtracting 1
      start = sf.getLow()-1;
      end = sf.getHigh();

      // Some unstranded features came in with fmin>fmax, which is illegal, so we had
      // to switch them in ChadoXmlAdapter.forceStrandIfNeeded.  Switch them back
      // for writing out.
      if (sf.getProperty("unstranded").equals("true") && sf.getStrand() == -1) {
        logger.warn("Unstranded feature " + sf.getName() + " had its fmax/fmin swapped when it was forced\nonto the minus strand--swapping back for writing.");
        int temp = start;
        start = end-1;
        end = temp+1;
      }

      boolean isFminPartial = false;
      boolean isFmaxPartial = false;
      if (sf instanceof Transcript) {
        isFminPartial = isPartial((Transcript)sf, "fmin");
        isFmaxPartial = isPartial((Transcript)sf, "fmax");
      }
      writeFeatureloc(indent, start, end, strand, 
                      //		      // false, false is for is_fmin/max partial (can only be true for peptide)
                      // I don't think that's right.
                      //		      false, false,
		      isFminPartial, isFmaxPartial,
                      // false is for is subject
		      sf, false, pw);
    }
  }

  /** Method for writing peptide feat locs */
  private static void writePepFeatloc(String indent, Protein prot, Transcript trans,
                                      PrintWriter pw) {
    // A sequence doesn't really have a strand--we want the strand of the parent transcript.
    RangeI baseOrientedRange = new Range(-1,-1);
    //int start = -1;  int end = -1;
    // this will always be true...
//     if (trans instanceof FeatureSet) {
// //	    logger.debug("Parent " + trans.getName() + " of seq " + seq.getDisplayId() + " has translation start = " + ((FeatureSet)trans).getTranslationStart()); // DEL
//       // Need to subtract 1 from start and end because it's added when
//       // translation start and end are set (for some reason)
// 	if (((FeatureSet)trans).getTranslationStart() > 0)
//           start = ((FeatureSet)trans).getTranslationStart() - 1;
// 	else {
//           logger.warn("translation start < 0 for " + prot.getName() + "--using start of " + trans.getName());
//           start = trans.getStart()-1;
//         }

// 	if (((FeatureSet)trans).getTranslationEnd() > 0)
//           end = ((FeatureSet)trans).getTranslationEnd() - 1;
// 	else {
//           logger.warn("translation end < 0 for " + prot.getName() + "--using end of " + trans.getName());
//           end = trans.getEnd();
//         }
//     }
//     else {
      // If we read in the data from GAME, the sequence won't have a range.
      //if (seq.getRange() == null) {
        // isnt this wrong? the trans is the transcript but shouldnt the start & end
        // translation start & end? not transcript start & end? - tried above for
    // trans start & stop & failed - thats why
        //start = trans.getStart();
        //end = trans.getEnd();
      //}
      //else {
        // Convert start back to interbase by subtracting 1
    baseOrientedRange.setStrand(trans.getStrand()); // hafta set strand before start
    if (prot.hasTranslationStart()) {
      baseOrientedRange.setStart(prot.getStart());//start = prot.getStart()-1;
    }
    else {
      logger.warn("no translation start for " + prot.getName() + 
                  "--using start of " + trans.getName());
      baseOrientedRange.setStart(trans.getStart());//start = trans.getStart()-1;
    }
    if (prot.hasTranslationEnd()) {
      baseOrientedRange.setEnd(prot.getEnd());//end = prot.getEnd();
    }
    else {
      logger.warn("no translation end for " + prot.getName() +
                  "--using end of " + trans.getName());
      baseOrientedRange.setEnd(trans.getEnd());//end = trans.getEnd();
    }
    RangeI interbaseRange = convertToInterbase(baseOrientedRange);


    //if (strand == 1) {
    writeFeatureloc(indent, interbaseRange, 
                    // isPartial for this peptide is determined by checking whether the TRANSCRIPT
                    // has no 3'/5' end
//                     isPartial(prot, "fmin") || isPartial(trans, "fmin"), 
//                     isPartial(prot, "fmax") || isPartial(trans, "fmax"),
                    isPartial(trans, "fmin"), 
                    isPartial(trans, "fmax"),
                    // False is for "this is not the subject of a feature pair"
                    prot, false, pw);
    //}
//     else {  // minus strand
//       // Ugh, more magic arithmetic!
//       start = start+1;
    // im this is wrong - this should be end-1
//       end = end+1;  // Why do I have to do this?

//       writeFeatureloc(indent, interbaseRange, 
// 		      isPartial(prot, "fmin") || isPartial(trans, "fmin"), 
// 		      isPartial(prot, "fmax") || isPartial(trans, "fmax"),
// 		      prot, false, pw);
//     }
  }

  private static RangeI convertToInterbase(RangeI baseOriented) {
    RangeI interbase = baseOriented.getRangeClone();
    interbase.convertFromBaseOrientedToInterbase();
    return interbase;
  }

  private static void writeFeatureloc(String in,RangeI range,boolean minPart,
                                      boolean maxPart,SeqFeatureI feat,boolean isSub,
                                      PrintWriter pw) {
    writeFeatureloc(in,range.getLow(),range.getHigh(),range.getStrand(),minPart,
                    maxPart,feat,isSub,pw);
  }

  /** thing can be a SeqFeatureI or a SequenceI.
    * If feature is a pair (e.g. a blast hit), it will have TWO featurelocs,
    * for subject and query.  
    * I think start & end are actually low/fmin & high/fmax here */
  private static void writeFeatureloc(String indent, int start, int end, int strand, 
				      boolean is_fmin_partial, boolean is_fmax_partial,
				      SeqFeatureI feat, boolean isSubject, PrintWriter pw) {
    // The data from the FlyBase server includes many transposable elements
    // with no featureloc (i.e. no start/end position).  These will have
    // start=-1 and end=0.  Don't write out these bogus featurelocs.
    if (start==-1 && end==0) {
      logger.debug("Not saving bogus featureloc for " + feat.getName());
      return;
    }

    pw.print(indent + "<featureloc>\n");
    // It would make more sense to print fmin before fmax, but this is not how it is done
    // in the Chado XML examples.
    pw.print(writeField(indent + TAB, "fmax", end+""));
    pw.print(writeField(indent + TAB, "fmin", start+""));

    pw.print(writeField(indent + TAB, "is_fmax_partial", is_fmax_partial ? "1" : "0"));
    pw.print(writeField(indent + TAB, "is_fmin_partial", is_fmin_partial ? "1" : "0"));

    // In the input, strand appears much later, but it would make more sense to put it here

    // locgroup is always 0
    pw.print(writeField(indent + TAB, "locgroup", "0"));

    // The rank is always 0 if this is not a feature pair.
    // For feature pairs, need to use 1 for subject (hit) and 0 for query.
    if (isSubject)
      pw.print(writeField(indent + TAB, "rank", "1"));
    else
      pw.print(writeField(indent + TAB, "rank", "0"));

    // Print alignment residue info (if applicable) for results
    //if (feat instanceof SeqFeatureI)
    if (feat.haveExplicitAlignment())
      pw.print(writeField(indent + TAB, "residue_info", feat.getExplicitAlignment()));

    // srcfeature_id, including organism, type_id (of chromosome_arm), chromosome arm name
    if (feat != null)
      writeSrcfeatureId(indent + TAB, feat, isSubject, pw);

    // Strand (why do they put it way down here?)
    if (isSubject) {// !! Is subject strand always 1??
      // The right thing seems to happen if we write 1 here.  It looks like sometimes
      // the query strand is ending up here!  What's up with that?
      //      if (strand != 1)  // DEL
      //        logger.warn("Weird--strand is " + strand + " for subject " + ((SeqFeatureI)thing).getId() + " from " + start + "-" + end); // DEL
      pw.print(writeField(indent + TAB, "strand", "1"));
    }
    else
      pw.print(writeField(indent + TAB, "strand", strand+""));
    pw.print(indent + "</featureloc>\n");
  }

  /** isSubject -> hit seq(FeatPair?) */
  private static void writeSrcfeatureId(String indent, SeqFeatureI sf,
                                        boolean isSubject, PrintWriter pw) {
    pw.print(indent + "<srcfeature_id>\n");
    pw.print(indent + TAB + "<feature>\n");

    // RESULT - There are extra fields to print if this is a result
    if (isResult(sf)) {
      // Query doesn't seem to have this is_analysis field
      if (isSubject) {
        pw.print(indent + TAB + TAB + "<is_analysis>1</is_analysis>\n");
        String name = sf.getProperty("ref_name");
        if (name.equals(""))
          name = sf.getName();
        pw.print(indent + TAB + TAB + "<name>" + name + "</name>\n");
        logger.debug("Printing result with name=" + name + ": " + sf);
      }
      else {  // print dbxref for the chromosome
        // false means this is not a feature_dbxref
        // -1 is for is_current (won't be used)
        // database should have been set by writeResults.
        writeXref(indent + TAB + TAB, database, chromosome,null, false, -1, pw);
      }
    }

    // Write organism ID yet again
    if (isSubject) {
      writeOrganism(indent + TAB + TAB, "Computational", "Result", pw);
    }
    else
      writeOrganismFromFeature(indent + TAB + TAB, sf, pw);

    // Write residues if feature has a sequence and is a result (not an annot)
    // and is a subject sequence.
    if (isSubject) {
      SequenceI seq = sf.getRefSequence();
      if (seq != null)
      {
        logger.debug("Ref seq for subject " + sf.getName() + " is called " + seq.getName());
        pw.print(writeField(indent + TAB + TAB, "residues", seq.getResidues()));
      }

      // Write timeaccessioned, timelastmodified
      writeTimes(indent + TAB + TAB, sf, pw);
    }

//     else if (sf instanceof SequenceI) {
//       writeOrganismFromSeq(indent + TAB + TAB, (SequenceI)sf, null, pw);
//       writeChromArmTypeId(indent + TAB + TAB, sf, pw);
//     }

    // There are extra fields to print if this is a result
    if (isResult(sf)) {
      if (isSubject)
        // Is it ok to leave out this cv type if it's missing?
        // Mark thought no, but there are some result spans in CG9397.chado.xml
        // with no type_id record.
        writeCVtype(indent + TAB + TAB, typeIDOntology, typeForResult(sf), pw);
      else
        writeCVtype(indent + TAB + TAB, typeIDOntology, "chromosome_arm", pw);

      // true arg to idForResult means use ref_id if available
      pw.print(indent + TAB + TAB + "<uniquename>" + idForResult(sf, true) + "</uniquename>\n");
      if (isSubject) {
        writeProperties(indent + TAB + TAB, sf, pw);
        logger.debug("property desc = " + sf.getProperty("description") + " for sf " + sf.getName());
        // If data was read from GAME, description won't have been added
        // as a property, so look for it with the ref sequence
        if (sf.getProperty("description").equals("")) {
          SequenceI seq = sf.getRefSequence();
          if (seq != null) {
            logger.debug("seq desc = " + seq.getDescription() + " for seq " + seq.getName());
            writeProperty(indent + TAB + TAB, "description", seq.getDescription(), 0, pw);
          }
        }
      }
    }
    else {  // NOT a result
      // Write type_id for chrom arm
      writeChromArmTypeId(indent + TAB + TAB, sf, pw);
    }

    pw.print(indent + TAB + "</feature>\n");
    pw.print(indent + "</srcfeature_id>\n");
  }

  private static boolean isResult(SeqFeatureI sf) {
    if (sf instanceof AnnotatedFeature)
      return false;
    else
      return true;
  }

  /** 12/2005 Not currently used. */
//   private static boolean isSubject(SeqFeatureI sf) {
//     String rank = sf.getProperty("rank");
//     if (rank.equals("1"))
//       return true;
//     else
//       return false;
//   }

  /** If we read the data from chadoxml, type should be stored as ref_type property.
   *  If that's missing, try to get it from parent. */
  private static String typeForResult(SeqFeatureI result) {
    if (!result.getProperty("ref_type").equals(""))
      return result.getProperty("ref_type");
    else {
      String parent_type = "";
      if (result.getRefFeature() != null)
        parent_type = result.getRefFeature().getFeatureType();
      //      if (parent_type == null || parent_type.equals(""))
      //        parent_type = "UNKNOWN_TYPE";
      return parent_type;
    }
  }

  /** result arg could be a result span or a parent result (singleton or pair).
   *  If this is a span of a feature pair and we read the data from chadoxml, 
   *  the result ID should be stored as ref_id property.
   *  If that's missing (or boring), try the id or name. */ 
  private static String idForResult(SeqFeatureI result, boolean useRefId) {
    logger.debug("idForResult: result = " + result + "; ref_id = " + result.getProperty("ref_id"));
    if (useRefId && !result.getProperty("ref_id").equals(""))
      return result.getProperty("ref_id");
    // no ref_id --try id or name
    else if (result.getId() != null && !result.getId().equals("")) {
      // Special case--data was read from GAME and assigned boring result span IDs.
      // Use the name instead.
      if ((result.getId().startsWith("result_span") || result.getId().equals(""))
          && !(result.getName().equals("")))
        return result.getName();
      else
        return result.getId();
    }
    else 
      //      return "UNKNOWN_UNIQUENAME";
      return "";  // ??
  }

  /** is_fmax_partial is true if the property is set to 1 *or* if missing_stop_codon is true.
   *  is_fmin_partial is true if the property is set to 1 *or* if missing_start_codon is true.
   *  1/2006: Changed to look at transcript for missing start/stop codon, rather than
   *  using is_fmin/fmax_partial property, because user might have changed transcript
   *  so that it now is ok, but Apollo didn't change the ChadoXML-specific property
   *  is_fmin/fmax_partial.
   *  (The reason this method returns a boolean instead of a String is so we can combine
   *  calls with ||.) */
  private static boolean isPartial(Transcript trans, String which) {
    if (which.equals("fmin")) {
      //      logger.debug("isPartial(" + trans.getName() + "): is_fmin_partial = " + trans.getProperty("is_fmin_partial") + ", trans.isMissing5prime() = " + trans.isMissing5prime()); // DEL
      //      if (trans.getProperty("is_fmin_partial").equals("1"))
      //        return true;
      //      else 
      if (trans.getStrand() == 1 && trans.isMissing5prime())  // missing start codon
        return true;
      else if (trans.getStrand() == -1 && trans.isMissing3prime())  // missing stop (minus strand)
        return true;
    }
    else {
      //      if (trans.getProperty("is_fmax_partial").equals("1"))
      //        return true;
      //      else 
      if (trans.getStrand() == -1 && trans.isMissing5prime())  // missing start codon (minus strand)
        return true;
      else if (trans.getStrand() == 1 && trans.isMissing3prime())  // missing stop
        return true;
    }
    return false;
  }

  private static boolean isPartial(Protein prot, String which) {
    if (which.equals("fmin")) {
      if (prot.getProperty("is_fmin_partial").equals("1"))
        return true;
      else if (prot.getProperty("is_fmax_partial").equals("1"))
          return true;
    }
    return false;
  }
  
  /** Exon rank was stored as a property on the exon */
  private static void writeExonRank(String indent, Exon exon, int exonRank, PrintWriter pw) {
    // Rank should be saved as a property
    if (!(exon.getProperty("rank").equals("")))
      pw.print(indent + "<rank>" + exon.getProperty("rank") + "</rank>\n");
    else {
      //      logger.error("Couldn't find rank for exon " + exon.getName() + "--using exon order " + exonNum); // DEL
      // Print 0 as the default
      //el - why are we defaulting to 0?  if the rank is not explicitly set, shouldn't we just use the exon number?
      //pw.print(indent + "<rank>0</rank>\n");
      pw.print(indent + "<rank>" + exonRank + "</rank>\n");
    }
  }

  /** Write type ID (including dbxref for NON-top-level annots) for chromosome arm */
  private static void writeChromArmTypeId(String indent, SeqFeatureI sf, PrintWriter pw) {
    pw.print(indent + "<type_id>\n");
    pw.print(indent + TAB + "<cvterm>\n");
    pw.print(indent + TAB + TAB + "<cv_id>\n");
    pw.print(indent + TAB + TAB + TAB + "<cv>\n");
    pw.print(indent + TAB + TAB + TAB + TAB + "<name>" + typeIDOntology + "</name>\n");
    pw.print(indent + TAB + TAB + TAB + "</cv>\n");
    pw.print(indent + TAB + TAB + "</cv_id>\n");

    // For some reason, annots DON'T have this dbxref; transcripts and exons do.
    if (sf.isAnnot()) { //object instanceof AnnotatedFeatureI) {
      //SeqFeatureI sf = (AnnotatedFeatureI) object;
      // couldnt this be sf.isAnnotTop()?
      if (!(sf.isExon()) &&
          !(sf.isTranscript()) &&
          !(sf.isProtein()) &&
          !(sf.getFeatureType().equalsIgnoreCase("mRNA"))) // mrna is trans isnt it?
        //        logger.warn("NOT writing dbxref for " + sf.getName() + " (type " + sf.getType() + ")"); // DEL
        ; // don't write dbxref
      else
        writeXref(indent + TAB + TAB, typeIDOntology, IDForDbxref, null,false, -1, pw);
    }
    // A result, not an annot or a part thereof
    else { 
      writeXref(indent + TAB + TAB, typeIDOntology, IDForDbxref, null,false, -1, pw);
    }

    pw.print(writeField(indent + TAB + TAB, "name", "chromosome_arm"));
    pw.print(indent + TAB + "</cvterm>\n");
    pw.print(indent + "</type_id>\n");
    // chromosome is a global variable that was set earlier (in writeGenomePosition)
    pw.print(writeField(indent, "uniquename", chromosome));
  }

  private static void writeSynonyms(String indent, AnnotatedFeatureI feat, PrintWriter pw) {
    Vector syns = feat.getSynonyms();

    for (int i = 0; i < syns.size(); i++) {
      Synonym syn = (Synonym) syns.elementAt (i);
//       // synonym might include illegal XML characters like <, so convert
//       // to entities before writing out.
//       // (Now this is done in writeSynonym)
//       syn = XML_util.transformToPCData(syn, true); // true means do transform quotes into entities
      writeSynonym(indent, feat, feat.getName(), syn, pw);
    }
  }

  private static void writeSynonym(String indent, AnnotatedFeatureI feat, String feature_name, Synonym syn, PrintWriter pw) {
    if (syn == null) // || syn.equals(""))
      return;

    pw.print(indent + "<feature_synonym>\n");

    // synonym might include illegal XML characters like <, so convert
    // to entities before writing out.
    syn.setName(XML_util.transformToPCData(syn.getName(), true)); // true means do transform quotes into entities

    pw.print(writeField(indent + TAB, "is_current", getIsCurrent(syn, feature_name)));
    pw.print(writeField(indent + TAB, "is_internal", getIsInternal(syn)));
    
    //    // 06/2005: If synonym came in with a pub id, the id was saved as property "author".
    //    // If this is missing, use feature owner as synonym author.
    //    String id = syn.getProperty("author");
    // Synonyms now have explicit setOwner/getOwner methods.
    String pub_id = syn.getOwner();
    if (syn == null || syn.equals(""))
      pub_id = syn.getProperty("author");
    if (syn.equals("")) {
      // Try to get owner from feature
      if (feat.getOwner() != null && !feat.getOwner().equals("") && !feat.getOwner().equals("null"))
        pub_id = feat.getOwner();
      else
        pub_id = defaultSynonymAuthor;
    }

    String pubType = syn.getProperty("pub_type");
    if (pubType.equals(""))
      pubType = defaultSynonymPubType;

    writePubId(indent + TAB, pub_id, pubType, pw);

    pw.print(indent + TAB + "<synonym_id>\n");
    pw.print(indent + TAB + TAB + "<synonym>\n");
    pw.print(writeField(indent + TAB + TAB + TAB, "name", syn.getName()));
    if (syn.getProperty("synonym_sgml").equals(""))  // need something for this field
      pw.print(writeField(indent + TAB + TAB + TAB, "synonym_sgml", syn.getName()));
    else
      pw.print(writeField(indent + TAB + TAB + TAB, "synonym_sgml", syn.getProperty("synonym_sgml")));
    // !! What about writing other properties that might have been attached to synonym?
    // Where should they go?
    writeCVtype(indent + TAB + TAB + TAB, "synonym type", "synonym", pw);
    pw.print(indent + TAB + TAB + "</synonym>\n");
    pw.print(indent + TAB + "</synonym_id>\n");
    pw.print(indent + "</feature_synonym>\n");
  }

  /** Write a pub_id, filling in pubType and id */
  private static void writePubId(String indent, String id, String pubType, PrintWriter pw) {
    pw.print(indent + "<pub_id>\n");
    pw.print(indent + TAB + "<pub>\n");
    writeCVtype(indent + TAB + TAB, "pub type", pubType, pw);
    pw.print(indent + TAB + TAB + "<uniquename>" + id + "</uniquename>\n");
    pw.print(indent + TAB + "</pub>\n");
    pw.print(indent + "</pub_id>\n");
  }
    
  private static String getIsCurrent(Synonym syn, String feature_name) {
    String value;
    value = syn.getProperty("is_current");
    if (value != null && !(value.equals("")))
      return value;
    // No property defined--is_current is 1 if this is an autosynonym (same as feature name)
    return (feature_name.equals(syn.getName())) ? "1" : "0";
  }

  private static String getIsInternal(Synonym syn) {
    String value;
    value = syn.getProperty("is_internal");
    if (value != null && !(value.equals("")))
      return value;
    // No property defined--just assume it's not internal (0)
    return "0";
  }

  /** At the end of the peptide record, there's now (as of July 2005) a feature_relationship
      block that says that the peptide is producedby its parent mRNA.
                <feature_relationship>
                  <object_id>
                    <feature>
                      <organism_id>
                      <type_id>
                          <name>mRNA</name>
                      <uniquename>CG16983-RE</uniquename>
                    </feature>
                  </object_id>
                  <type_id>
                          <name>relationship type</name>
                      <name>producedby</name>
                  </type_id>
                </feature_relationship>
     12/2005: This method is currently not used */
//   private static void writePeptideFeatureRelationship(String indent, Transcript transcript, CurationSet curation, PrintWriter pw) {
//     pw.print(indent + "<feature_relationship>\n");
//     pw.print(indent + TAB + "<object_id>\n");
//     pw.print(indent + TAB + TAB + "<feature>\n");
//     writeOrganismFromFeature(indent + TAB + TAB + TAB, transcript, curation, pw);
//     writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "mRNA", pw);
//     pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(transcript.getId())));
//     pw.print(indent + TAB + TAB + "</feature>\n");
//     pw.print(indent + TAB + "</object_id>\n");
//     writeCVtype(indent + TAB, "relationship type", "producedby", pw);
//     pw.print(indent + "</feature_relationship>\n");
//   }

  /** Writes results directly to buffered print writer, which is much faster than
   *  writing to a temporary string buffer and passing back a string */
  private static void writeResults(CurationSet curation,
                                   String indent, PrintWriter pw) {
    StrandedFeatureSetI analyses = curation.getResults();
    if (analyses == null) {
      logger.debug("No results to save."); 
      return;
    }
    logger.debug("Saving " + analyses.size() + " types of results");

    if (analyses.size() > 0) {
      // Get database for genomic sequences, to be used when writing out results
      Vector genomic_xrefs = curation.getRefSequence().getDbXrefs();
      if (genomic_xrefs != null && genomic_xrefs.size() > 0) {
        DbXref xref = (DbXref) genomic_xrefs.firstElement();
        database = xref.getDbName();
        logger.debug("Set genomic database to " + database + " based on xref for curation's ref seq");
      }
      else
        logger.info("No dbxref for genomic sequence--using default database definition " + database);

      for (int i = 0; i < analyses.size(); i++) {
        FeatureSetI analysis = (FeatureSetI) analyses.getFeatureAt(i);
        if (analysis.getFeatureType() != null &&
            !analysis.getFeatureType().equals ("codons") &&   // ?
            !analysis.getFeatureType().equals ("Gene")) {     // ?
          writeAnalysisResults(analysis, indent, pw);
        }
      }
    }
  }

  private static String getProgramName(SeqFeatureI sf)
  {
    String program = sf.getProgramName(); 
    /* If we started with GFF data, 
       we won't have a program name in the SeqFeature,
       so use the type instead */
    if (program == null || 
        program.length() == 0 || 
        program.equals(RangeI.NO_TYPE))
      program = sf.getProperty ("type");
    if (program.length() == 0 ||
        program.equals(RangeI.NO_TYPE))
      program = sf.getTopLevelType();
    return program;
  }
  
  /** Write out all the results of this particular analysis type */
  private static void writeAnalysisResults(FeatureSetI analysis, String indent, PrintWriter pw) {
    String program = getProgramName(analysis); 

    // In the GAME writer, the results can be FeatureSets, FeaturePairs, or SeqFeatures.
    // Here, they always seem to be FeatureSets, though that is presumably just a result
    // of how they're saved when they're read in.
    Vector results = analysis.getFeatures();

    if (logger.isDebugEnabled()) {
      logger.debug("Writing " + (results.size()+1) + " results (program = " + program + ", db = " + database + ")");
    }

    for (int i = 0; i < results.size(); i++) {
      if (results.elementAt(i) instanceof FeatureSetI) {
        FeatureSetI result = (FeatureSetI) results.elementAt (i);
        writeResult(result, indent, pw);
      } 
      else {
        logger.error ("writeAnalysisResults: don't know what to do to save non-FeatureSet class " +
                      results.elementAt(i).getClass().getName());
      }
    }
  }
  
  /** Note that result spans can be FeaturePairs or just SeqFeatures */
  private static void writeResult(FeatureSetI result, String indent, PrintWriter pw) {
    pw.print(indent + "<feature>\n");
    pw.print(writeField(indent + TAB, "is_analysis", "1"));
    if (!(result.getProperty("is_obsolete")).equals(""))
      pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", result.getProperty("is_obsolete")));

    pw.print(writeField(indent + TAB, "name", XML_util.transformToPCData(result.getName())));
    // Write out any other generic fields now
    writeFields(indent + TAB + TAB + TAB, result, pw);

    writeOrganism(indent + TAB, "Computational", "Result", pw);
    pw.print(writeField(indent + TAB, "seqlen", "0"));  // seems pointless, but might as well match input

    // Write timeaccessioned, timelastmodified
    writeTimes(indent + TAB, result, pw);

    // SO term: match (is it ever anything else at this level?)
    writeCVtype(indent + TAB, typeIDOntology, CVTermForResults, pw);

    // This result id is ok
    pw.print(writeField(indent + TAB, "uniquename", XML_util.transformToPCData(result.getId())));

    // Write analysisfeature (including score)
    //    pw.print(indent + TAB + "<analysisfeature>for result set</analysisfeature>\n");
    writeAnalysisFeature(indent + TAB, result, pw);

    // Write result spans
    Vector spans = result.getFeatures();
    for (int i = 0; i < spans.size (); i++) {
      SeqFeatureI span = (SeqFeatureI) spans.elementAt (i);
      writeSpan (indent + TAB, span, pw);
    }

    // Close feature tag
    pw.print(indent + "</feature>\n");
  }

  /** Write a result span */
  private static void writeSpan(String indent, SeqFeatureI span, PrintWriter pw) {
    pw.print(indent + "<feature_relationship>\n");
    pw.print(indent + TAB + "<subject_id>\n");
    pw.print(indent + TAB + TAB + "<feature>\n");

    writeOrganism(indent + TAB + TAB + TAB, "Computational", "Result", pw);
      //    }
    // Do we ever want to do this?
    //      writeOrganismFromFeature(indent + TAB + TAB, span, pw);

    // SO term: could it be something other than match?  Hardcode to "match" for now.
    writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "match", pw);

    // Write uniquename (id)
    pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", 
                        //                        XML_util.transformToPCData(idForResult(span))));
                        // false == use ref_id if available, rather than span.getId()
                        XML_util.transformToPCData(idForResult(span, false))));

    // Write analysisfeature
    writeAnalysisFeature(indent + TAB + TAB + TAB, span, pw);

    // Write featureloc
    writeFeatureloc(indent + TAB + TAB + TAB, span, pw);

    // Close tags
    pw.print(indent + TAB + TAB + "</feature>\n");
    pw.print(indent + TAB + "</subject_id>\n");

    writeCVtype(indent + TAB, "relationship type", "partof", pw);

    pw.print(indent + "</feature_relationship>\n");
  }

  private static void writeOrganismFromFeature(String indent, SeqFeatureI feat,
                                               PrintWriter pw) {
    writeOrganismFromFeature(indent, feat, null, pw); // don't have CurationSet
  }

  /** In SeqFeatures, organism is stored as a property.
   *  If feature doesn't have organism, try to figure it out. */
  private static void writeOrganismFromFeature(String indent, SeqFeatureI feat, CurationSet curation, PrintWriter pw) {
    String organism = getOrganismFromFeature(feat, curation);
    if (organism != null) {
      String genus = organism;
      String species = "";
      if (organism.indexOf(" ") > 0) {
        genus = organism.substring(0, organism.indexOf(" "));
        species = organism.substring(organism.indexOf(" ")+1);
      }
      writeOrganism(indent, genus, species, pw);
    }
    else {
      // If organism is not filled in, use the default one for this style
      logger.warn("no organism for " + feat.getName() + " (" + feat.getId() + ")");
      organism = Config.getStyle().getParameter("organism");
      if (organism != null && !(organism.equals(""))) {
        logger.info("Using default organism for this style: " + organism);
        String genus = organism;
        String species = "";
        if (organism.indexOf(" ") > 0) {
          genus = organism.substring(0, organism.indexOf(" "));
          species = organism.substring(organism.indexOf(" ")+1);
        }
        writeOrganism(indent, genus, species, pw);
      }
    }
  }

  /** Try to get organism from feat; failing that, look at its parent, refseq, etc. */
  private static String getOrganismFromFeature(SeqFeatureI feat, CurationSet curation) {
    String organism = feat.getProperty("organism");
    if (organism != null && !(organism.equals("")))
      return organism;

    // didn't find organism--try parent
    SeqFeatureI parent = feat.getRefFeature();
    if (parent != null) {
      organism = parent.getProperty("organism");
      if (organism != null && !(organism.equals(""))) {
        if (organism.indexOf(" ") > 0) {
          logger.debug("organism for " + feat.getName() + " is " + organism);
          String genus = organism.substring(0, organism.indexOf(" "));
          String species = organism.substring(organism.indexOf(" ")+1);
          feat.addProperty("organism", genus + " " + species);  // remember it for future reference
        }
        return organism;
      }
    }

    // still didn't find organism--try refseq
    if (feat.isAnnot()) {
      SequenceI seq = feat.getRefSequence();
      organism = seq.getOrganism();
      if (organism != null && !(organism.equals(""))) {
        if (organism.indexOf(" ") > 0) {
          logger.debug("organism for " + feat.getName() + " is " + organism);
          String genus = organism.substring(0, organism.indexOf(" "));
          String species = organism.substring(organism.indexOf(" ")+1);
          feat.addProperty("organism", genus + " " + species);  // remember it for future reference
        }
        return organism;
      }
    }

    // still didn't find organism--try parent's refseq
    if (parent != null) { // protein doesnt have parent - should it be transcript?
      SequenceI seq = parent.getRefSequence();
      organism = seq.getOrganism();
      if (organism != null && !(organism.equals(""))) {
        if (organism.indexOf(" ") > 0) {
          String genus = organism.substring(0, organism.indexOf(" "));
          String species = organism.substring(organism.indexOf(" ")+1);
          feat.addProperty("organism", genus + " " + species);  // remember it for future reference
          parent.addProperty("organism", genus + " " + species);  // remember it for future reference
        }
        return organism;
      }
    }

    // STILL didn't find organism--if this is an annot, use curation set's
    if (curation != null && feat.isAnnot()) {
      organism = curation.getOrganism();
      if (organism != null && !(organism.equals(""))) {
        if (organism.indexOf(" ") > 0) {
          String genus = organism.substring(0, organism.indexOf(" "));
          String species = organism.substring(organism.indexOf(" ")+1);
          feat.addProperty("organism", genus + " " + species);  // remember it for future reference
          if (parent != null)
            parent.addProperty("organism", genus + " " + species);  // remember it for future reference
        }
        return organism;
      }
    }

    // Oh well, we tried.
    return null;
  }

  // write peptide organism (called by writeSrcFeatId & writePeptide)
//   private static void writeOrganismFromSeq(String indent, SequenceI seq,
//                                            SeqFeatureI parent, PrintWriter pw) {
//     String organism = seq.getOrganism();
//     // If we can't get it from the seq, see if we can get it from the parent
//     if ((organism == null || organism.equals("")) && parent != null)
//       organism = getOrganismFromFeature(parent, null);
//     if (organism != null) {
//       String genus = organism;
//       String species = "";
//       if (organism.indexOf(" ") > 0) {
//         genus = organism.substring(0, organism.indexOf(" "));
//         species = organism.substring(organism.indexOf(" ")+1);
//       }
//       seq.setOrganism(organism);  // Save for future reference
//       writeOrganism(indent, genus, species, pw);
//     }
//   }

  private static void writeOrganism(String indent, String genus, String species, PrintWriter pw) {
    pw.print(indent + "<organism_id>\n");
    pw.print(indent + TAB + "<organism>\n");
    pw.print(indent + TAB + TAB + "<genus>" + genus + "</genus>\n");
    pw.print(indent + TAB + TAB + "<species>" + species + "</species>\n");
    pw.print(indent + TAB + "</organism>\n");
    pw.print(indent + "</organism_id>\n");
  }

  private static void writeCVtype(String indent, String type, String term, PrintWriter pw) {
    if (term == null || term.equals(""))
      return;  // Don't write a type with no name
    pw.print(indent + "<type_id>\n");
    pw.print(indent + TAB + "<cvterm>\n");
    pw.print(indent + TAB + TAB + "<cv_id>\n");
    pw.print(indent + TAB + TAB + TAB + "<cv>\n");
    pw.print(indent + TAB + TAB + TAB + TAB + "<name>" + convertToChado(type) + "</name>\n");
    pw.print(indent + TAB + TAB + TAB + "</cv>\n");
    pw.print(indent + TAB + TAB + "</cv_id>\n");
    pw.print(indent + TAB + TAB + "<name>" + term + "</name>\n");
    pw.print(indent + TAB + "</cvterm>\n");
    pw.print(indent + "</type_id>\n");
  }

  /** There are certain type names in GAME that are different in Chado, so in
   *  case the data came in from GAME, we may need to convert the type.
   *   from:                                   to:
   *    cdna                                    cDNA
   *    transcript                              mRNA
   *    pseudotranscript                        mRNA
   *    transposon                              transposable_element
   *    aa                                      protein
   *    misc. non-coding RNA                    ncRNA
   *    microRNA                                nuclear_micro_RNA_coding_gene
   *    miscellaneous curator's observation     remark
  */
  private static String convertToChado(String type) {
    if (type.equalsIgnoreCase("cdna"))
      return "cDNA";
    else if (type.equalsIgnoreCase("transcript"))
      return "mRNA";
    else if (type.equalsIgnoreCase("pseudotranscript"))
      return "mRNA";
    else if (type.equalsIgnoreCase("transposon"))
      return "transposable_element";
    else if (type.equalsIgnoreCase("aa"))
      return "protein";
    else if (type.equalsIgnoreCase("misc. non-coding RNA"))
      return "ncRNA";
    else if (type.equalsIgnoreCase("microRNA"))
      return "nuclear_micro_RNA_coding_gene";
    else if (type.equalsIgnoreCase("miscellaneous curator's observation"))
      return "remark";
    else
      return type;
  }

  /** Note that Apollo's datamodels (which mostly match GAME XML) encode pseudogenes (etc.)
      as annot type = pseudogene, whereas in Chado, pseudogenes are encoded as annot type = gene,
      transcript type = pseudogene.  So we need to adjust for this when writing. */
  private static String annotTypeForChado(AnnotatedFeatureI annot) {
    String type = annot.getTopLevelType();
    if (type.equalsIgnoreCase("pseudogene") ||
        type.equalsIgnoreCase("tRNA") ||
        type.equalsIgnoreCase("snoRNA") ||
        type.equalsIgnoreCase("ncRNA") ||
        type.equalsIgnoreCase("rRNA") ||
        type.equalsIgnoreCase("miRNA") ||
        type.equalsIgnoreCase("snRNA")) {
      return "gene";
    }
    else
      return type;
  }

  /** If transcript's bioType is gene, we print "mRNA"--otherwise, we use the bioType. */
  private static String transcriptTypeForChado(Transcript transcript) {
    String type = transcript.getTopLevelType();
    if (type.equalsIgnoreCase("gene"))
      type = "mRNA";
    return type;
  }

  private static void writeAnalysisFeature(String indent, SeqFeatureI sf, PrintWriter pw) {
    // Be sure to get score from this feature, not its parent
    double score = sf.getScore();
    pw.print(indent + "<analysisfeature>\n");
    pw.print(indent + TAB + "<analysis_id>\n");
    pw.print(indent + TAB + TAB + "<analysis>\n");
    // Spans don't have this stuff set, so if we don't see the program name,
    // try looking at span's parent feature.
    if (sf.getProgramName() == null || sf.getProgramName().equals(""))
      sf = sf.getRefFeature();

    //if there is no program associated with this feature, use the top-level type
    String program = getProgramName(sf);
    
    pw.print(writeField(indent + TAB + TAB + TAB, "program", program));
    pw.print(writeField(indent + TAB + TAB + TAB, "programversion", sf.getProperty("programversion")));
    pw.print(writeField(indent + TAB + TAB + TAB, "sourcename", sf.getDatabase()));
    pw.print(writeField(indent + TAB + TAB + TAB, "sourceversion", sf.getProperty("sourceversion")));
    pw.print(writeField(indent + TAB + TAB + TAB, "timeexecuted", sf.getProperty("timeexecuted")));
    pw.print(indent + TAB + TAB + "</analysis>\n");
    pw.print(indent + TAB + "</analysis_id>\n");
    pw.print(writeField(indent + TAB, "rawscore", score +""));
    pw.print(indent + "</analysisfeature>\n");
  }

  /** Write timeaccessioned and timelastmodified (which were stored as properties on a SeqFeature). */
  private static void writeTimes(String indent, SeqFeatureI sf, PrintWriter pw) {
    pw.print(writeField(indent, "timeaccessioned", sf.getProperty("timeaccessioned")));
    pw.print(writeField(indent, "timelastmodified", sf.getProperty("timelastmodified")));
  }

  /** Writes all properties for this feature or sequence (except for those that are
   *  explicitly written out elsewhere). */
  private static void writeProperties(String indent, SeqFeatureI feat, PrintWriter pw) {
    Hashtable props = feat.getPropertiesMulti();
    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      // Don't write out the property if it's explicitly written out elsewhere
      if (!isSpecialProperty(type) &&
          // I didn't add "unstranded" to the list of special properties because
          // I want it to appear in the annot info window (the other special properties
          // are suppressed there), but don't write it out.
          !type.equals("unstranded")) {
        Vector values = feat.getPropertyMulti(type);
        if (values == null)
          continue;
          
        for (int i = 0; i < values.size(); i++) {
          String value = (String) values.elementAt(i);
          // Don't write out dicistronic property unless it's true
          if (type.equals("dicistronic") && value.equals("false"))
            continue;

          // Unfamiliar fields in the input were stored as properties, but with
          // the FIELD_LABEL prepended to the value (e.g. color=field:blue).
          // If this property has FIELD_LABEL prepended, then DON'T write it
          // here--it will be written by writeFields.
          if (!value.startsWith(ChadoXmlAdapter.FIELD_LABEL))
            // i is the rank
            writeProperty(indent, type, value, i, pw);
        }
      }
    }
  }

  /** Writes out flat fields for this feature or sequence (except for those that are
   *  explicitly written out elsewhere).
   *  Unfamiliar fields in the input were stored as properties, but with
   *  the FIELD_LABEL prepended to the value (e.g. color=field:blue).
  */
  private static void writeFields(String indent, SeqFeatureI feat, PrintWriter pw) {
    Hashtable props = feat.getPropertiesMulti();
    Enumeration e = props.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      // Don't write out the property if it's explicitly written out elsewhere
      if (!isSpecialProperty(type)) {
        Vector values = feat.getPropertyMulti(type);
        if (values == null)
          continue;
          
        for (int i = 0; i < values.size(); i++) {
          String value = (String) values.elementAt(i);

          // Unfamiliar fields in the input were stored as properties, but with
          // the FIELD_LABEL prepended to the value (e.g. color=field:blue).
          // Look for FIELD_LABEL to see if we need to write this "property" out as a
          // field instead of a property.
          if (value.startsWith(ChadoXmlAdapter.FIELD_LABEL))
            // writeField will strip out the FIELD_LABEL prefix
            pw.print(writeField(indent, type, value));

          // Otherwise, don't write it out--it will be written by writeProperties.
        }
      }
    }
  }

  private static void writeWeirdTranscriptProperties(String indent, Transcript trans, PrintWriter pw) {
    // Note: the 0 argument in all the calls to writeProperty is for featureprop rank

    // If this came in as stop_codon_readthrough or readthrough_stop_codon,
    // don't write out stop_codon_redefinition_as_selenocysteine property.
    if (trans.readThroughStopResidue() != null) {
      if (trans.readThroughStopResidue().equals("U"))
        writeProperty(indent, "stop_codon_redefinition_as_selenocysteine", "true", 0, pw);
      // If it's the generic one, just say "true"
      else if (trans.readThroughStopResidue().equals("X"))
        writeProperty(indent, "stop_codon_readthrough", "true", 0, pw);
      // Otherwise, if it's something other than U or X, write the residue
      else
        writeProperty(indent, "stop_codon_readthrough", trans.readThroughStopResidue(), 0, pw);
    }
    if (trans.plus1FrameShiftPosition() > 0)
      // was plus_1_translational_frame_shift
      writeProperty(indent, "plus_1_translational_frameshift", trans.plus1FrameShiftPosition()+"", 0, pw);
    if (trans.minus1FrameShiftPosition() > 0)
      writeProperty(indent, "minus_1_translational_frameshift", trans.minus1FrameShiftPosition()+"", 0, pw);
    //      buf.append (writeTSS (indent + "  ", trans, "feature_span", curation)); ??

    if (trans.unConventionalStart())
      writeProperty(indent, "non_canonical_start_codon", trans.getStartCodon(), 0, pw);

    if (trans.isMissing5prime())
      writeProperty(indent, "missing_start_codon", "true", 0, pw);
    if (trans.isMissing3prime())
      writeProperty(indent, "missing_stop_codon", "true", 0, pw);
    if ((trans.getNonConsensusAcceptorNum() >= 0 || 
         trans.getNonConsensusDonorNum() >= 0)) {
      // Check if there was already an explicit non_canonical_splice_site property saved
      // (it might have some value other than "approved" or "unapproved")
      if (trans.getProperty("non_canonical_splice_site").equals(""))
        writeProperty(indent, "non_canonical_splice_site",
                      (trans.nonConsensusSplicingOkay() ?
                       "approved" : "unapproved"), 0, pw);
      // If the property is *not* empty, it will get written when we write out
      // the generic properties.
    }
    // This is for boolean "problem" property of annot/transcript, which can be set 
    // in annot info editor.
    // (Results could also come in with a string for "problem"; this is stored
    // inside apollo as a "tag" and will be written out separately.)
    if (trans.isProblematic())
      writeProperty(indent, "problem", "true", 0, pw);
  }

  /** Identify "special" properties that are explicitly written out elsewhere
   *  so we don't also write them out as generic properties. */
  public static boolean isSpecialProperty(String prop) {
    // Should make a vector of special props or something instead of all these ifs
    if (prop.equalsIgnoreCase("timeaccessioned"))
      return true;
    if (prop.equalsIgnoreCase("timelastmodified"))
      return true;
    if (prop.equalsIgnoreCase("seqlen"))
      return true;
    if (prop.equalsIgnoreCase("is_analysis"))
      return true;
    if (prop.equalsIgnoreCase("is_obsolete"))
      return true;
    if (prop.equalsIgnoreCase("uniquename"))
      return true;
    if (prop.equalsIgnoreCase("name"))
      return true;
    if (prop.equalsIgnoreCase("organism"))
      return true;
    if (prop.equalsIgnoreCase("rank"))
      return true;
    if (prop.equalsIgnoreCase("is_fmin_partial"))
      return true;
    if (prop.equalsIgnoreCase("is_fmax_partial"))
      return true;
    //    if (prop.equalsIgnoreCase("added_fake_transcript"))
    //      return true;
    if (prop.equalsIgnoreCase("ref_name"))
      return true;
    if (prop.equalsIgnoreCase("ref_id"))
      return true;
    if (prop.equalsIgnoreCase("ref_type"))
      return true;
    if (prop.equalsIgnoreCase("type_id"))
      return true;
    // !! What about "program", "sourcename", and "database"--why weren't they in this list?
    // Do they ever appear as annotation properties (rather than as result properties
    // used to hold the analysis info)?
    if (prop.equalsIgnoreCase("programversion"))
      return true;
    if (prop.equalsIgnoreCase("sourceversion"))
      return true;
    if (prop.equalsIgnoreCase("timeexecuted"))
      return true;
    if (prop.equalsIgnoreCase("internal_synonym"))
      return true;
    if (prop.equalsIgnoreCase("readthrough_stop_codon"))
      return true;
    return false;
  }

  private static void writeProperty(String indent, String prop, String value, int rank, PrintWriter pw) {
    if (value == null || value.equals(""))
      return;

    // Special case: result tags are stored as "tag" internally but written out
    // as "problem".
    if (prop.equals("tag"))
      prop = "problem";

    pw.print(indent + "<featureprop>\n");
    pw.print(indent + TAB + "<rank>" + rank + "</rank>\n");
    writeCVtype(indent + TAB, featurepropCV, prop, pw);
    value = XML_util.transformToPCData(value, true); // true means do transform quotes into entities
    pw.print(indent + TAB + "<value>" + value + "</value>\n");
    pw.print(indent + "</featureprop>\n");
  }

  /** Comment rank, as for other featureprops, is generated as comments are written out,
   *  not roundtripped. */
  private static void writeComments(String indent, AnnotatedFeatureI feat, PrintWriter pw) {
    Vector comments = feat.getComments ();
    for (int i = 0; i < comments.size(); i++) {
      Comment comment = (Comment) comments.elementAt (i);
      pw.print(indent + "<featureprop>\n");
      pw.print(indent + TAB + "<rank>" + i + "</rank>\n");
      writeCVtype(indent + TAB, featurepropCV, "comment", pw);
      String commentText = XML_util.transformToPCData(comment.getText(), true); // true means do transform quotes into entities
      pw.print(indent + TAB + "<value>" + commentText + "</value>\n");
      writeFeaturepropPub(indent + TAB, comment.getPerson(), pw);
      pw.print(indent + "</featureprop>\n");
    }
  }

  private static void writeFeaturepropPub(String indent, String id, PrintWriter pw) {
    // Some featureprops don't have featureprop_pubs (e.g. the comment attached
    // to skpA).  If id == null, don't write out the record.
    if (id == null)
      return;

    pw.print(indent + "<featureprop_pub>\n");
    pw.print(indent + TAB + "<pub_id>\n");
    pw.print(indent + TAB + TAB + "<pub>\n");
    // 5/5/05: This used to have "curator"; the new examples have "computer file" instead
    writeCVtype(indent + TAB + TAB + TAB, "pub type", defaultSynonymPubType, pw);
    pw.print(indent + TAB + TAB + TAB + "<uniquename>" + id + "</uniquename>\n");
    pw.print(indent + TAB + TAB + "</pub>\n");
    pw.print(indent + TAB + "</pub_id>\n");
    pw.print(indent + "</featureprop_pub>\n");
  }

  /** Writes a field on one or more lines:
   *  <name>value</name> or
   *  <name>
   *    value
   *  </name> */
  private static String writeField(String startingIndent, String name, String value, boolean separateLines) {
    if (name == null || name.equals(""))
      return "";
    if (value == null || value.equals("") || value.equals(ChadoXmlAdapter.FIELD_LABEL))
      return "";
    String field = startingIndent + "<" + name + ">";
    if (separateLines)
      field += "\n" + startingIndent + TAB;
    // Value may have been prepended with FIELD_LABEL, e.g. "field:blue"
    if (value.startsWith(ChadoXmlAdapter.FIELD_LABEL))
      value = value.substring((ChadoXmlAdapter.FIELD_LABEL).length());
    field += value;
    if (separateLines)
      field += "\n" + startingIndent;
    field += "</" + name + ">" + "\n";
    return field;
  }

  private static String writeField(String startingIndent, String name, String value) {
    return writeField(startingIndent, name, value, false);  // default is all on one line
  }

  private static String writeEnd() {
    return "</chado>\n";
  }

  /** The transactions are saved in a separate file.  The transaction
   *  filename is actually generated by the ChadoTransactionXMLWriter
   *  from the mainFileName. */
  protected static void saveTransactions(CurationSet curation, String mainFileName) {
    // If there's no transaction manager, that probably means there were no edits done.
    if (curation.getTransactionManager() == null) {
      return; // No transaction support (or no transactions to save)
    }
    try {
      // Make sure Transaction instances coalesced
      curation.getTransactionManager().coalesce();

      if (curation.getTransactionManager().getTransactions().size() == 0) {
        logger.info("No transactions to save");
        return;
      }

      // Save transactions in Apollo transaction format, if desired
      if (Config.outputTransactionXML()) {
        //        String filename = transactionFileName(mainFileName);
        saveTransactionsInGAME(curation, mainFileName);
      }
      // Save transactions in Chado transaction format, if desired
      // Config.isChadoTnOutputNeeded() returns true if apollo.cfg has
      // OutputChadoTransaction            "true"
      if (Config.isChadoTnOutputNeeded()) {
        // Save transactions to chado transaction format
        TransactionOutputAdapter output = new ChadoTransactionXMLWriter();
        if (curation.isChromosomeArmUsed()) {
          output.setMapID(curation.getChromosome());
          output.setMapType("chromosome_arm");
        }
        else {
          output.setMapID(curation.getChromosome());
          output.setMapType("chromosome");
        }
        output.setTransformer(new ChadoTransactionTransformer());
        output.setTarget(mainFileName);
        output.commitTransactions(curation.getTransactionManager());
        logger.debug("Saved transactions to " + output.getTarget());
      }
    }
    catch(Exception e) {
      logger.error("ChadoXmlWrite.saveTransactions(): " + e, e);
      JOptionPane.showMessageDialog(null, 
                                    "Transactions cannot be saved.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

  /** Borrowed from GAMEAdapter */
  private static void saveTransactionsInGAME(CurationSet curationSet, String fileName) {
    try {
      TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
      tnAdapter.setFileName(fileName);
      tnAdapter.save(curationSet.getTransactionManager().getTransactions());
    }
    catch(IOException e) {
      logger.error("saveTransactionsInGAME(): " + e, e);
    }
  }

  /** Comes up with name for transaction file based on annotation filename.
   *  12/2005: Not used */
//   private static String transactionFileName(String annotFileName) {
//     String tranFileName;
//     int index1 = annotFileName.indexOf(File.separator);
//     int index2 = -1;
//     if (index1 > -1)
//       index2 = annotFileName.indexOf(".", index1);
//     else
//       index2 = annotFileName.indexOf(".");
//     if (index2 == -1) {
//       // No extension name for annotFileName
//       tranFileName = annotFileName + TransactionXMLAdapter.EXT_NAME;
//     }
//     else {
//       tranFileName = annotFileName.substring(0, index2) + TransactionXMLAdapter.EXT_NAME;
//     }
//     return tranFileName;
//   }

}
