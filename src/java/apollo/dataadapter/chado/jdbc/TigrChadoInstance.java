package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.*;

import org.bdgp.util.ProgressEvent;

import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.Region;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.SequenceI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.Transcript;

/**
 * Implementation of AbstractChadoInstance for TIGR's chado databases.
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.18 $ $Date: 2007/12/06 01:40:42 $ $Author: gk_fan $
 */
public class TigrChadoInstance extends AbstractChadoInstance {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TigrChadoInstance.class);

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------

  public TigrChadoInstance() {}
  
  TigrChadoInstance(JdbcChadoAdapter jdbcChadoAdapter) {
    super(jdbcChadoAdapter);
  }

  // -----------------------------------------------------------------------
  // ChadoInstance
  // -----------------------------------------------------------------------

  public String getGeneNameField() {
    return "uniquename";
  }

  public String getTranscriptNameField() {
    return "uniquename";
  }

  public String getCdsSql(FeatureLocImplementation featLocImp)
    throws RelationshipCVException 
  {
    return getCdsSql(featLocImp, false);
  }

  public String getPredictedCdsSql(FeatureLocImplementation featLocImp, ChadoProgram[] chadoPrgs)
    throws RelationshipCVException 
  {
    // TODO - Limit by chadoPrgs?  It won't necessarily be faster, and it's OK if this method
    // returns too many CDS features.
    return getCdsSql(featLocImp, true);
  }

  public String getAnalysisType(long analysisId, String program, String programversion, String sourcename) {
    // TODO - refine this
    return program;
  }

  /** returns target name as species + targetChadoName, unless align type is SNP,
      where query name is used for target name */
  public String getTargetName(String targetChadoName,String species,String alignType) {
    if (alignType.equals("SNP")) return null;
    if (species.equals("not known")) return targetChadoName;
    return species + " " + targetChadoName;
  }

  /** tigrs feature type */
  public String getFeatureType(String alignType,String program,
			       String programversion,String targetSp,
			       String sourcename,String featProp) {

    if ((alignType != null) && (alignType.equals("SNP"))) {
      return "SNP" + featProp!=null ? " - "+featProp : "";
    }

    // special case for NAP, AAT_AA, AAT_NA, PASA
    // TODO - make this configurable
    if (program.startsWith("aat_") || program.endsWith("/pasagf") || program.equals("nap")) {
      return program + " " + sourcename;
    }

    String type = program;
    if (programversion != null) type = type + " " + programversion;
    if (targetSp != null) type = type + " " + targetSp;
    return type;
  }

  /**
   * This should come from config 
   */
  public String getFeatureCVName() {
    if (super.getFeatureCVName() != null)
      return super.getFeatureCVName();

    // just in case not set in chado-adapter.xml (though it should be)
    return "TIGR Ontology";
  }

  public CurationSet getCurationSet(ChadoAdapter adapter, String seqType, String seqId) {
    super.clear();
    Connection conn = getConnection();

    if (conn == null)
      return null;
    
    CurationSet cset = null;
    StrandedFeatureSet results = getResultStrandedFeatSet();
    StrandedFeatureSet annotations = getAnnotStrandedFeatSet();

    // TO DO - allow the data retrieved by this method to be customized somehow, either in a 
    // configuration file or directly in the ChadoAdapterGUI (the latter may require some
    // additional communication between this class and ChadoAdapterGUI)

    cset = new CurationSet();

    // Query for the Chado feature.feature_id of the requested sequence
    long seqFeatId = getChadoAdapter().getFeatureId(conn, seqType, seqId);

    FeatureLocImplementation featLocImp = 
      new FeatureLocImplementation(seqFeatId,haveRedundantFeatureLocs(),conn);
    setTopFeatLoc(featLocImp);

    logger.info("retrieving sequence and annotations for " + seqType + " sequence " + seqId);
    adapter.fireProgressEvent(new ProgressEvent(this, new Double(50.0), "Retrieving " + seqType + " sequence " + seqId));
    // Retrieve the corresponding apollo.datamodel.Sequence object
    // should this be getting GAMESequence? (need to rename)
    //    SequenceI seq = getChadoAdapter().getSequence(conn, seqFeatId,true);
    SequenceI seq = featLocImp.retrieveSequence(getChadoAdapter());

    // Change the residue offset; Chado uses 0-based coordinates, and 1-based coordinates work better in Apollo
    // handled by AbstractSeq.needShiftFromOneToZero i think
    // not part of SequenceI interface - should it? i think suzis opposed
    // seq.setResidueOffset(1); 
    cset.setRefSequence(seq);
    // TO DO - include sequence coordinates in CurationSet name when support for subsequence display is added:
    cset.setName(seq.getName());
    // JC: HACK - JDBCTransactionWriter won't work without a chromosome 
    cset.setChromosome(seq.getName());
    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving " + seqType + " sequence " + seqId));

    cset.setStart(1);
    cset.setEnd(seq.getLength());
    logger.info("retrieved " + seq.getLength() + " bp for " + seqId);
    
    // TO DO - set other CurationSet attributes using info. retrieved from Chado e.g., 
    // cset.setOrganism("?");
    // cset.setChromosome("1");

    Double zero = new Double(0.0);
    Double hero = new Double(100.0);

    // ------------------------------------------------
    // 1-level results
    // ------------------------------------------------
    ChadoProgram[] oneLevels = getOneLevelResultPrograms();
    String m1 = "Retrieving 1-level results";
    logger.debug("retrieving 1-level results");
    adapter.fireProgressEvent(new ProgressEvent(this, zero, m1));
    getChadoAdapter().addOneLevelResults();
    adapter.fireProgressEvent(new ProgressEvent(this, hero, m1));
    logger.debug("done retrieving 1-level results");

    // ------------------------------------------------
    // Gene models and 1-level annotations
    // ------------------------------------------------
    logger.debug("retrieving 1-level annotations");

    boolean getFeatProps = true;
    boolean getSynonyms = true;
    boolean getDbXRefs = true;

    // list of 1-level annotations is configured in chado-config.xml
    getAnnotations(conn,seq,annotations,featLocImp,getFeatProps,getSynonyms,getDbXRefs,adapter);
    logger.debug("done retrieving 1-level annotations");

    // ------------------------------------------------
    // Gene predictions
    // ------------------------------------------------    
    logger.debug("retrieving gene models");
    adapter.fireProgressEvent(new ProgressEvent(this, zero, "Retrieving gene predictions"));
    // list of gene prediction programs is configured in chado-config.xml
    ChadoProgram[] predictionPrograms = getGenePredictionPrograms();
    getChadoAdapter().addGenePredictionResults(conn, predictionPrograms, results, featLocImp, seq);
    adapter.fireProgressEvent(new ProgressEvent(this, hero, "Retrieving gene predictions"));
    logger.debug("done retrieving gene models");

    // ------------------------------------------------
    // Copy annotated genes into result tier (optional)
    // ------------------------------------------------    
    if (getCopyGeneModelsIntoResultTier()) {
      String ctname = "Copy_of_Annotation";
      logger.debug("copying gene models into '" + ctname + "' tier");
      getChadoAdapter().copyAnnotatedGenesIntoResultTier(annotations, results, ctname, seq);
      logger.debug("done copying gene models into '" + ctname + "' tier");
    }

    // ------------------------------------------------
    // Search hits
    // ------------------------------------------------    
    String primaryScoreColumn = getChadoAdapter().getAnalysisFeatureIdentityField();

    // flag to addSearchHits to get tgt seqs in separate query
    boolean getTgtSeqSep = false;
    boolean getAlignSeqs = false;
    boolean setTargetSeqs = true;
    boolean getTargetSeqDescriptions = true;
    boolean joinWithFeatureProp = false;

    ChadoProgram[] hitProgs = getSearchHitPrograms();
    String progNameList = concatProgramNames(hitProgs);
    String m = "Retrieving " + progNameList + " results";
    logger.debug("retrieving search hits for: " + progNameList);
    ProgressEvent p = new ProgressEvent(this, zero, m);
    adapter.fireProgressEvent(p);
    boolean makeSeqsLazy = true;
    getChadoAdapter().addSearchHits(conn, hitProgs, setTargetSeqs, getTgtSeqSep,
				    makeSeqsLazy, getTargetSeqDescriptions,joinWithFeatureProp,seq, 
				    results,primaryScoreColumn,getAlignSeqs,featLocImp);
    adapter.fireProgressEvent(new ProgressEvent(this,hero,"Retrieved " + progNameList + " results"));
    logger.debug("done retrieving search hits for: " + progNameList);
    
    // all the addSearchHits could be and probably should be combined into one if it in fact speeds 
    // things up as i suspect it will (speeds up flybase) 
    // PROmer/NUCmer results (stored with strand = 0)
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving NUCmer annotations"));
    //    getChadoAdapter().addSearchHits(conn, new String[]{"NUCmer%"},/*1,0,*/ false,getTgtSeqSep,
    //                                   getTargetDescriptions, false, seq,results,scoreColumn,
    //                                   getAlignSeqs,featLocImp);
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving NUCmer annotations"));
    //
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving PROmer annotations"));
    //    getChadoAdapter().addSearchHits(conn, new String[]{"PROmer%"},/*1,0,*/ false, getTgtSeqSep,
    //                  getTargetDescriptions, false, seq,results,scoreColumn,getAlignSeqs,featLocImp);
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving PROmer annotations"));

    // SNPs
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving SNPs"));
    // snp score column null - just gets set to 100
    //    getChadoAdapter().addSearchHits(conn, new String[]{"TIGR SNP%"},/*0,1,*/ true, getTgtSeqSep,
    //                  getTargetDescriptions, true, seq,results,null,getAlignSeqs,featLocImp);
  //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving SNPs"));

    // "region" analysis
//    adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving region analyses"));
    //    getChadoAdapter().addSearchHits(conn, new String[]{"region%"},/*1,0,*/ false, getTgtSeqSep,getTargetDescriptions,false, seq,
    //                  results,scoreColumn,getAlignSeqs,featLocImp);
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving region analyses"));

    // ------------------------------------------------
    // Features featureloc'd to the sequence's proteins
    // ------------------------------------------------

    // PEffect
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving peffect annotation"));
    //    getChadoAdapter().addProteinAlignments(conn, "peffect", results, featLocImp);
    //    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving peffect annotation"));

    // BLASTP
    //      adapter.fireProgressEvent(new ProgressEvent(this, new Double(0.0), "Retrieving BLASTP hits"));
    //      addProteinAlignments(conn, seqFeatId, "washu blastp", results);
    //      adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving BLASTP hits"));

    // ------------------------------------------------
    // Add annotations and results
    // ------------------------------------------------
    logger.info(seqId + " loaded");
    cset.setAnnots(annotations);
    cset.setResults(results);

    // ------------------------------------------------
    // Close Sybase/JDBC connection
    // ------------------------------------------------
    try {
      conn.close();
    } catch (SQLException sqle) {
      logger.error("failed to close JDBC Connection", sqle);
    }

    return cset;
    
  }
  
  /** Explicitly set translation start and stop from cds */
  public void setTranslationStartAndStop(Transcript trans, ChadoCds cds) {
    boolean calculateEnd = false;
    boolean retval = trans.setTranslationStart(cds.getStart(),calculateEnd);
    if (!retval) {
      logger.error("setTranslationStart(" + cds.getStart() + ", false) failed for " + trans);
    }
    trans.setTranslationEnd(cds.getTranslationEnd());
  }

  /** Explicitly set translation start and stop from cds */
  public void setTranslationStartAndStop(FeatureSet trans, ChadoCds cds) {
    boolean calculateEnd = false;
    boolean retval = trans.setTranslationStart(cds.getStart(),calculateEnd);
    if (!retval) {
      logger.error("setTranslationStart(" + cds.getStart() + ", false) failed for " + trans);
    }
    trans.setTranslationEnd(cds.getTranslationEnd());
  }
  
  /* (non-Javadoc)
   * @see apollo.dataadapter.chado.jdbc.ChadoInstance#getCurationSetInRange(apollo.dataadapter.chado.ChadoAdapter, java.lang.String, java.lang.String, int, int, java.sql.Connection)
   */
  public CurationSet getCurationSetInRange(ChadoAdapter adapter,String seqType,
                                           Region region) {
    logger.error("getCurationSetInRange() called with seqType=" + seqType);
    throw new UnsupportedOperationException("TigrChadoInstance.getCurationSetInRange()");
  }
  
  // -----------------------------------------------------------------------
  // TigrChadoInstance - private methods
  // -----------------------------------------------------------------------
  
  private SchemaVersion getChadoVersion() {
    return getChadoAdapter().getChadoVersion();
  }

  private boolean haveRedundantFeatureLocs() {
    return true;
  }

  // This was copied from FlybaseChadoInstance; should be moved to a utility class because
  // it's not at all specific to program names.
  private String concatProgramNames(ChadoProgram[] names) {
    String msg = "";
    if (names == null)
      return msg;
    int size = names.length;
    for (int i = 0; i < size; i++) {
      msg += names[i].getProgram();
      if (i < size - 2)
        msg += ", ";
      else if (i < size - 1)
        msg += " and ";
    }
    return msg;
  }

  private String getCdsSql(FeatureLocImplementation featLocImp, boolean isAnalysis)
    throws RelationshipCVException 
  {
    String fminCol = getChadoVersion().getFMinCol();
    String fmaxCol = getChadoVersion().getFMaxCol();
    String subjFeatCol = getChadoVersion().getSubjFeatCol();
    String objFeatCol = getChadoVersion().getObjFeatCol();
    Long producedByCvId = getProducedByCVTermId(); // throws RelCVException
    
    // SQL that retrieves all CDS features featureloc'ed to a given sequence.
    // Note that we join all the way through to the protein/polypeptide 
    // feature in order to get the protein name and sequence.
    
    return 
      "SELECT cds.uniquename AS cds_name, "+
      "       trans.uniquename AS transcript_uniquename, "+
      "       trans.residues AS transcript_seq,"+
      "       cdsloc." + fminCol + " AS fmin, "+
      "       cdsloc." + fmaxCol + " AS fmax, "+
      "       cdsloc.is_fmin_partial, "+
      "       cdsloc.is_fmax_partial, "+
      "       cdsloc.strand, "+
      "       prot.uniquename AS protein_name, " +
      "       prot.residues AS protein_seq " +
      "FROM featureloc cdsloc, feature cds, feature_relationship cds2trans, "+
      "     feature trans, feature_relationship cds2prot, feature prot " +
      "WHERE cdsloc.srcfeature_id = " + featLocImp.getContainingFeatureId() + " " + 
      "AND cdsloc.feature_id = cds.feature_id " +
      "AND cds.type_id = " + getFeatureCVTermId("CDS") + " " +
      "AND cds.is_analysis = " + (isAnalysis ? "1" : "0") + " " +
      "AND cds.feature_id = cds2trans." + subjFeatCol + " " +
      "AND cds2trans." + objFeatCol + " = trans.feature_id " +
      "AND cds2trans.type_id = " + producedByCvId + " " + 
      "AND trans.type_id = " + getFeatureCVTermId("transcript") + " " +
      "AND cds.feature_id = cds2prot." + objFeatCol + " " +
      "AND cds2prot.type_id = " + producedByCvId + " " + 
      "AND cds2prot." + subjFeatCol + " = prot.feature_id " +
      "AND prot.type_id = " + getPolypeptideCVTermId();
  }

}
