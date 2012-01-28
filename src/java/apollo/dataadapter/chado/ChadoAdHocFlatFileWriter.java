package apollo.dataadapter.chado;

import apollo.config.Config;
import apollo.dataadapter.NotImplementedException;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.TranslationI;
import apollo.editor.TransactionManager;
import apollo.editor.Transaction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;

import org.apache.log4j.*;

/**
 * Writes a curation set to an ad-hoc chado-based flat file format.
 * Used primarily for chado roundtrip test suite; note that this class
 * is not guaranteed to write *all* of Apollo's state to the file, but
 * it should write at least as much as was read from the chado database
 * by the JdbcChadoAdapter.
 *
 * @author jcrabtree@sourceforge.net
 */
public class ChadoAdHocFlatFileWriter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoAdHocFlatFileWriter.class);

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  public ChadoAdHocFlatFileWriter() {}

  // -----------------------------------------------------------------------
  // ChadoAdHocFlatFileWriter
  // -----------------------------------------------------------------------

  /**
   * @param filename The name of the file to write to.
   * @param cs       The CurationSet whose data is to be written.
   * @return whether the write succeeded
   */
  public boolean write(String filename, CurationSet cs) {

    logger.debug("ChadoAdHocFlatFileWriter: writing CurationSet " + cs + " to file " + filename);

    // open file for writing
    File outFile = new File(filename);

    // don't overwrite
    if (outFile.exists()) {
      logger.error("ChadoAdHocFlatFileWriter: file " + filename + " already exists - write failed");
      return false;
    }

    // set up output streams
    try {
      FileWriter writer = new FileWriter(outFile);
      PrintWriter pwriter = new PrintWriter(writer);

      // do the write
      writeCurationSet(pwriter, cs);

      // close outermost output stream/writer
      pwriter.close();
    } 
    catch (IOException ioe) {
      logger.error("IOException writing CurationSet to " + filename, ioe);
      return false;
    }

    // indicate success
    return true;
  }

  // -----------------------------------------------------------------------
  // ChadoAdHocFlatFileWriter - protected methods
  // -----------------------------------------------------------------------

  /**
   * @param pw Where to write the flat file output.
   * @param cs The CurationSet to write.
   */
  protected void writeCurationSet(PrintWriter pw, CurationSet cs) {
    pw.println("begin CurationSet");

    String inFile = cs.getInputFilename();
    String assemType = cs.getAssemblyType();
    StrandedFeatureSetI annots = cs.getAnnots();
    StrandedFeatureSetI results = cs.getResults();
    HashMap childSets = cs.getChildCurationSets();
    Vector seqs = cs.getSequences();
    TransactionManager tm = cs.getTransactionManager();

    // String values
    pw.println("inputFilename=" + inFile);
    pw.println("assemblyType=" + assemType);

    // StrandedFeatureSets
    pw.println("annots=begin annots()");
    writeStrandedFeatureSet(pw, annots);
    pw.println("end annots\n");

    pw.println("results=begin results()");
    writeStrandedFeatureSet(pw, results);
    pw.println("end results\n");    

    // Sequences
    int ns = (seqs == null) ? 0 : seqs.size();
    pw.println("sequences=begin sequences(" + ns + ")");
    writeSequences(pw, seqs);
    pw.println("end sequences\n");

    // Transactions
    int nt = 0;
    if (tm != null) {
      List trans = tm.getTransactions();
      nt = (trans == null) ? 0 : trans.size();
    }
    pw.println("transactions=begin transactions(" + nt + ")");
    writeTransactions(pw, tm);
    pw.println("end transactions\n");

    // child CurationSets
    int ncs = (childSets == null) ? 0 : childSets.size();
    pw.println("curationsets=begin curationsets(" + ncs + ")");
    writeCurationSets(pw, childSets);
    pw.println("end curationsets\n");

    pw.println("end CurationSet\n");
  }

  /**
   * @param pw Where to write the flat file output.
   * @param sfs The StrandedFeatureSetI to write.
   */
  protected void writeStrandedFeatureSet(PrintWriter pw, StrandedFeatureSetI sfs) {
    pw.println("begin StrandedFeatureSet");

    // forward-strand features
    FeatureSetI fs = sfs.getForwardSet();
    //    int nf = fs.getNumberOfChildren();
    pw.println("forward=begin forward()");
    writeFeatureSet(pw, fs);
    pw.println("end forward\n");

    // reverse-strand features
    FeatureSetI rs = sfs.getReverseSet();
    //    int nr = rs.getNumberOfChildren;
    pw.println("reverse=begin reverse()");
    writeFeatureSet(pw, rs);
    pw.println("end reverse\n");

    pw.println("end StrandedFeatureSet");
  }

  /**
   * @param pw Where to write the flat file output.
   * @param fs The FeatureSetI to write.
   */
  protected void writeFeatureSet(PrintWriter pw, FeatureSetI fs) {
    // treat feature sets the same as seqfeatures for now:
    writeSeqFeature(pw, fs);
  }

  /**
   * @param pw Where to write the flat file output.
   * @param sf The SeqFeatureI to write.
   */
  protected void writeSeqFeature(PrintWriter pw, SeqFeatureI sf) {
    Vector kids = sf.getFeatures();
    int nk = (kids == null) ? 0 : kids.size();
    String type = sf.getTopLevelType();

    pw.println("begin feature");
    pw.println("start=" + sf.getStart());
    pw.println("end=" + sf.getEnd());
    pw.println("id=" + sf.getId());
    pw.println("refId=" + sf.getRefId());
    pw.println("topLevelType=" + type);
    pw.println("programName=" + sf.getProgramName());
    //    pw.println("residues=" + sf.getResidues()); // from RangeI

    // coding info.
    if (sf.hasTranslation()) {
      TranslationI trans = sf.getTranslation();
      int ts = trans.getTranslationStart();
      int te = trans.getTranslationEnd();

      if (ts != te) {
        pw.println("codingDNA=" + sf.getCodingDNA());
        pw.println("translation=" + sf.translate());
      }
    }

    pw.println("kids=begin kids(" + nk + ")");
    Iterator ki = kids.iterator();
    while (ki.hasNext()) {
      SeqFeatureI kid = (SeqFeatureI)ki.next();
      writeSeqFeature(pw, kid);
    }
    pw.println("end kids");

    pw.println("end feature\n");
  }

  /**
   * @param pw Where to write the flat file output.
   * @param sfs Vector of SequenceI
   */
  protected void writeSequences(PrintWriter pw, Vector seqs) {
    int ns = (seqs == null) ? 0 : seqs.size();
    if (ns > 0) {
      Iterator si = seqs.iterator();
      while (si.hasNext()) {
        SequenceI seq = (SequenceI)si.next();
        writeSequence(pw, seq);
      }
    }
  }

  protected void writeSequence(PrintWriter pw, SequenceI seq) {
      pw.println("begin sequence");
      pw.println("accession=" + seq.getAccessionNo());
      pw.println("name=" + seq.getName());
      pw.println("organism=" + seq.getOrganism());
      pw.println("length=" + seq.getLength());
      pw.println("residues=" + seq.getResidues());
      pw.println("end sequence\n");
  }

  /**
   * @param pw Where to write the flat file output.
   * @param tm TransactionManager.
   */
  protected void writeTransactions(PrintWriter pw, TransactionManager tm) {
    int ts = 0;
    if (tm != null) {
      List trans = tm.getTransactions();
      ts = (trans == null) ? 0 : trans.size();
    }
    if (ts > 0) {
      throw new NotImplementedException("writeTransactions() not implemented");
    }
  }

  /**
   * @param pw Where to write the flat file output.
   * @param cs HashMap of child curation sets.
   */
  protected void writeCurationSets(PrintWriter pw, HashMap cs) {
    int ncs = (cs == null) ? 0 : cs.size();
    if (ncs > 0) {
      throw new NotImplementedException("writeCurationSets() not implemented");
    }
  }
}
