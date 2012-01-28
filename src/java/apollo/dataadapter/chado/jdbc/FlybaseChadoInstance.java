package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.*;

import org.bdgp.util.ProgressEvent;

import apollo.dataadapter.chado.ChadoAdapter;

import apollo.dataadapter.Region;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.Transcript;

/** we need an AbstractChadoInstance super class for generic stuff, like
    gene prediction programs. */

public class FlybaseChadoInstance extends AbstractChadoInstance {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlybaseChadoInstance.class);

  public FlybaseChadoInstance() {
  }
  
  FlybaseChadoInstance(JdbcChadoAdapter jdbcChadoAdapter) {
    super(jdbcChadoAdapter);
  }

  public String getGeneNameField() {
    return "name";
  }
  public String getTranscriptNameField() {
    return "name";
  }
  /** flybase query for translation start stop (cds) is different from tigrs - 
      just shoving in a query that works with how things are set up for now - probably need
      a revisit - protein fields being labeled as cds - take out the sequence? 
      (get it from the genomic on demand?) */
  public String getCdsSql(FeatureLocImplementation featLocImp)
    throws RelationshipCVException {

    Long protCvId = getPolypeptideCVTermId();
    Long cdsCvId = getFeatureCVTermId("CDS");
    Long producedByCvId = getProducedByCVTermId(); // throws relationsCvException
    Long mrnaCvId = getFeatureCVTermId("mRNA");
    Long transcriptCvId = getFeatureCVTermId("transcript");
    
    //making modification to look for both 'polypeptide' and 'CDS' features that are
    //mapped to 'mRNA' and 'transcript' features accordingly (different uses of the
    //schema...)
    return 
      "SELECT cds.uniquename AS cds_name, " +
      "trans.uniquename AS transcript_uniquename, " +
      "trans.residues AS transcript_seq, cdsloc.fmin AS fmin, cdsloc.fmax AS fmax," +
      "cdsloc.is_fmin_partial as is_fmin_partial, cdsloc.is_fmax_partial as is_fmax_partial, " +
      "cdsloc.strand, cds.uniquename AS protein_name, cds.residues AS protein_seq , trans.feature_id as transcript_id " +
      "FROM featureloc cdsloc, feature cds, feature_relationship cds2trans, " +
      "feature trans " +
      " WHERE cdsloc.srcfeature_id = "+featLocImp.getContainingFeatureId()+" " +
      " AND cdsloc.feature_id = cds.feature_id " +
      " AND cds.type_id IN (" + protCvId + "," + cdsCvId + ")" +
      " AND cds.feature_id = cds2trans.subject_id "+
      " AND cds2trans.object_id = trans.feature_id "+
      " AND cds2trans.type_id = "+producedByCvId+
      " AND trans.type_id IN (" + mrnaCvId + ", " + transcriptCvId + ") " +
      featLocImp.getContainingFeatureWhereClause("cdsloc");
  }
  
  /**
   * Returns only the cds linked to a transcript predicted by a program of chadoPrgs which isRetrieveCDS is true
   * @param featLocImp
   * @param chadoPrgs array of chadoProgram 
   * @return an sql query as a string, null if no predicted CDSs wanted (ChadoProgram.retrieveCDS)
   * @throws RelationshipCVException
   * @see #getCdsSql(FeatureLocImplementation)
   */
  public String getPredictedCdsSql(FeatureLocImplementation featLocImp, ChadoProgram[] chadoPrgs)
    throws RelationshipCVException 
  {
    Long protCvId = getPolypeptideCVTermId();
    Long producedByCvId = getProducedByCVTermId(); // throws relationsCvException

    // Get list of ChadoPrograms for which retrieveCDS() is true
    List cdsChadoPrgs = new ArrayList();
    for (int i = 0; i<chadoPrgs.length; i++){
      if (chadoPrgs[i].retrieveCDS()){ cdsChadoPrgs.add(chadoPrgs[i]); }
    }   
    int numCdsChadoPrgs = cdsChadoPrgs.size();
    String analysisSelect = "";
    String analysisFrom = "";
    StringBuffer analysisWhereStr = new StringBuffer();
    
    // No predicted CDS to fetch
    if (numCdsChadoPrgs == 0) {
      return null;
    } 
    else {
      ChadoProgram [] cdsChadoPrograms = (ChadoProgram [])cdsChadoPrgs.toArray();
      analysisWhereStr.append("AND cds.feature_id = af.feature_id ");

      // analysis table is cached; no need to add it to the query
      if (cacheAnalysisTable()) {
        analysisFrom = ", analysisfeature af ";
        try {
          analysisWhereStr.append(getChadoAdapter().getChadoProgramWhereClause(cdsChadoPrograms, "af", null));
        } catch (Throwable e) { // HACK
          logger.error("unable to retrieve predicted CDS results", e);
          return null;
        }
      }
      // add analysis table to query
      else {
        analysisSelect = ", a.analysis_id, a.program, a.programversion, a.sourcename ";
        analysisFrom = ", analysisfeature af, analysis a ";
        try {
          analysisWhereStr.append(getChadoAdapter().getChadoProgramWhereClause(cdsChadoPrograms, "af", "a"));
        } catch (Throwable e) { // HACK
          logger.error("unable to retrieve predicted CDS results", e);
          return null;
        }
      }
    }
    
    Long mrnaCvId = getFeatureCVTermId("mRNA");

    return 
      "SELECT cds.uniquename AS cds_name, "+
      "trans.uniquename AS transcript_uniquename, "+
      "trans.residues AS transcript_seq, cdsloc.fmin AS fmin, cdsloc.fmax AS fmax,"+
      "cdsloc.is_fmin_partial as is_fmin_partial, cdsloc.is_fmax_partial as is_fmax_partial, "+
      "cdsloc.strand, cds.uniquename AS protein_name, cds.residues AS protein_seq , trans.feature_id as transcript_id "+
      analysisSelect +
      "FROM featureloc cdsloc, feature cds, feature_relationship cds2trans, "+
      "feature trans "+ analysisFrom +
      " WHERE  cds.feature_id = cds2trans.subject_id "+
      " AND cdsloc.feature_id = cds.feature_id "+
      " AND cds2trans.object_id = trans.feature_id "+
      analysisWhereStr.toString()+
      " AND cdsloc.srcfeature_id = "+featLocImp.getContainingFeatureId()+" "+
      " AND cds.type_id = "+protCvId+    
      " AND cds2trans.type_id = "+producedByCvId+" AND trans.type_id = "+mrnaCvId+
      featLocImp.getContainingFeatureWhereClause("cdsloc");
  }

  
  // --> chado-adapter.xml
//   /** cv term for produced by - fb dmel uses "producedby" (tigr produced_by) */
//   private String getProducedByString() {
//     return "producedby";
//   }

//   /** fb: "partof" */ --> chado-adapter.xml
//   public String getPartOfCvTerm() {
//     return "partof";
//   }

  /** In retrieving a seq type, retrieve this much more sequence around the feature.
      for seqType gene pad out 25kb, everything else 0 padding. */
  private int getPaddingForTopLevelSeqType(String seqType) {
    if (seqType.equals("gene")) {
      //if (JdbcChadoAdapter.debug) return 200; // keep small for debug
      return 25000;
    }
    return 0;
  }
  /** just returns chadoName - cant add species as fb doesnt record species of target */
  public String getTargetName(String chadoName,String species,String alignType) {
    return chadoName;
  }
  /** Flybase does feature type "program:sourcename", the rest of the params
      are ignored (used by tigr feat type) */
  public String getFeatureType(String alignType,String program,
                                  String programversion,String targetSp,
                                  String sourcename,String featProp) {
    String t = program;
    if (sourcename != null && sourcename.length() > 0)
      t += ":"+sourcename;
    return t;
  }

  /** Flybases feature cv is SO */
  public String getFeatureCVName() {
    if (super.getFeatureCVName() != null)
      return super.getFeatureCVName();

    // just in case not set in chado-adapter.xml
    return "SO";
  }

//   /** fbs relationship cv is "relationship type" */
//   public String getRelationshipCVName() {
//     return "relationship type"; // flybase-specific 
//   }
//   /** flybase -> "property type" cv */
//   public String getPropertyTypeCVName() {
//     return "property type";
//   }

  /** returns false - flybase does not have redundant feat locs.
      make part of interface? no need to at this point */
  private boolean haveRedundantFeatureLocs() {
    return false;
  }
  
  public CurationSet getCurationSetInRange(ChadoAdapter adapter,
                                           String seqType,
                                           Region region) {
    Connection conn = getConnection();
    if (conn == null) { return null; }

    logger.debug("getCurationSetInRange called");
    // Query for the Chado feature.feature_id of the requested sequence
    long seqFeatId = getChadoAdapter().getFeatureId(conn, seqType, region.getChromosome());
    
    // check that range is within bounds
    int seqLen = getChadoAdapter().getSeqLengthForFeatureId(conn, seqFeatId);
    // if start is less than 1, set it to 1 and warn the user
    if (region.getStart() < 1) {
      region.setStart(1);
      logger.info("Region start coordinate is less than 1 - setting to 1");
    }
    // if end is greater than length of sequence or less than 1, set it to sequence length and warn user
    if (region.getEnd() > seqLen || region.getEnd() < 1) {
      region.setEnd(seqLen);
      logger.info("Region end coordinate is less than 1 or greater than sequence length - setting to sequence length");
    }
      
    FeatureLocImplementation featLocImp = new ChromosomeFeatureLocImp(seqFeatId, region, conn);
    //setTopFeatLoc(featLocImp);
    return getCurationSet(adapter, seqType, region.getChromosome(), featLocImp);
  }
  

  /**
   * @param adapter  ChadoAdapter on whose behalf the database accesses are to be performed.
   * @param seqType  cvterm type_id of the sequence identified by <code>featName</code>
   * @param topFeatName  Chado feature name of the sequence to be 
   displayed/annotated in Apollo. Name correlates with seqType.
   */
  public CurationSet getCurationSet(ChadoAdapter adapter, String seqType, 
                                    String topFeatName) {
    Connection conn = getConnection();
    if (conn == null) {
      return null;
    }

    logger.debug("getCurationSet called");
    // Query for the Chado feature.feature_id of the requested sequence
    long seqFeatId = getChadoAdapter().getFeatureId(conn, seqType, topFeatName);

    // padding not fully implemented - all queries need to pad - and cur set
    // this should come from config (with defaults)
    int padding = getPaddingForTopLevelSeqType(seqType);

    boolean getSeqFromParent = (padding != 0);

    FeatureLocImplementation featLocImp = 
      new FeatureLocImplementation(seqFeatId,haveRedundantFeatureLocs(),conn,padding);
    //setTopFeatLoc(featLocImp);
    return getCurationSet(adapter, seqType, topFeatName, featLocImp);
  }
  
  /** there is a bunch of stuff here that is redundant in different chado instances
      and needs to be centralized either in jdbcChadoAdapter or an abstract super
      class */
  private CurationSet getCurationSet(ChadoAdapter adapter, String seqType,
                                     String topFeatName,
                                     FeatureLocImplementation featLocImp) {
    super.clear();
    Connection conn = getConnection();
    //FeatureLocImplementation featLocImp = getTopFeatLoc();
    setTopFeatLoc(featLocImp);
    CurationSet cset = null;
    //StrandedFeatureSet results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    StrandedFeatureSet results = getResultStrandedFeatSet();
    StrandedFeatureSet annotations = getAnnotStrandedFeatSet();
    // TO DO - allow the data retrieved by this method to be customized somehow, either in a 
    // configuration file or directly in the ChadoAdapterGUI (the latter may require some
    // additional communication between this class and ChadoAdapterGUI)

    //Connection conn = getConnection();

    // turn off seqscan - encourage use of indexes - speeds up gene and cds queries
    // well its more complicated than that - seqscan makes small region (only 1 gene) 
    // gene queries faster (3 sec vs 24 sec) but makes big region gene queries 
    // (scaffolds with 10 genes or so) slower 1:11 vs 26 sec - 
    // gee whiz should we turn this on depending on the query? - 
    // cheesy but turning off for gene queries to make them zippy
    //if (seqType.equals("gene")) setSeqscan(conn,false); - done in PostgresChadoAdapter
    
    cset = new CurationSet();
    //cset.setAssemblyType(seqType);
    String srcFeatureType = featLocImp.getSrcFeatureType();
    cset.setAssemblyType(srcFeatureType == null ? seqType : srcFeatureType);

//    // Query for the Chado feature.feature_id of the requested sequence
//    long seqFeatId = getChadoAdapter().getFeatureId(conn, seqType, topFeatName);
//
//    // padding not fully implemented - all queries need to pad - and cur set
//    int padding = getPaddingForTopLevelSeqType(seqType);
//
//    FeatureLocImplementation featLocImp = 
//      new FeatureLocImplementation(seqFeatId,haveRedundantFeatureLocs(),conn,padding);

    adapter.fireProgressEvent(new ProgressEvent(this, new Double(50.0), "Retrieving " + seqType + " sequence " + topFeatName));
    // Retrieve the corresponding apollo.datamodel.Sequence object
    logger.debug("calling featLocImp.retrieveSequence");

    SequenceI seq = featLocImp.retrieveSequence(getChadoAdapter());
    // gene seq has to be padded out and retrieved from parent as substring 
    // this should be configged or something
    
//     if (seqType.equals("gene") || seqType.equals("chromosome")) { // chromosome w loc
//       // should just take feat loc imp
//       seq = getChadoAdapter().getSeqFromFeatLocImp(featLocImp); 
//     }
//     else {
//       seq = getChadoAdapter().getSequence(conn, featLocImp.getFeatureId(), false, true);
//     }
    //ChadoFeatureLoc featLoc = new ChadoFeatureLoc((int)seqFeatId,conn);
    // need to add 1 to start. chado is interbase - apollo is base oriented.
    //int start =
      //getChadoAdapter().adjustLowForInterbaseToBaseOrientedConversion(featLoc.getFmin());
    cset.setStart(featLocImp.getBaseOrientedMinWithPadding());
    // no need to add 1 to end: chado is end exclusive, apollo is end inclusive.
    //cset.setEnd(featLoc.getFmax());
    cset.setEnd(featLocImp.getMaxWithPadding());
    if (seq!=null)
      cset.setRefSequence(seq); 
    // To set the top level feat name
    //if (featLocImp.getSrcFeatureType().equals("chromosome"))
    if (featLocImp.hasTopLevelName()) // isnt this always true?
      // change setChrom->setTopLevelName
      cset.setChromosome(featLocImp.getTopLevelFeatName()); 
    // TO DO - include sequence coordinates in CurationSet name when support for 
    // subsequence display is added:
    cset.setName(seq.getName());
    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), "Retrieving " + seqType + " sequence " + topFeatName));


    // TO DO - set other CurationSet attributes using info. retrieved from Chado e.g., 
    cset.setOrganism(seq.getOrganism());
    // cset.setChromosome("1");
    
    //boolean skipData = true;
    boolean skipData = false;
    //boolean skipData = JdbcChadoAdapter.debug;

    Double z = new Double(0.0);
    Double d100 = new Double(100.0);

    // -----------------
    // ONE LEVEL RESULTS
    // -----------------
    //ChadoProgram[] oneLevels = getOneLevelResultPrograms();
    String m = "Retrieving 1-level results ";//+concatProgramNames(oneLevels);
    adapter.fireProgressEvent(new ProgressEvent(this, z, m));
    getChadoAdapter().addOneLevelResults();
    logger.debug("addOneLevelResults() returned");

    // ------------------------------------------------
    // Gene models
    // ------------------------------------------------
    getAnnotations(conn, seq, annotations, featLocImp, true,true,true,adapter);

    // ------------------------------------------------
    // Features located directly on the reference sequence
    // ------------------------------------------------    
    
    ChadoProgram[] predictionPrograms = getGenePredictionPrograms();
    
    adapter.fireProgressEvent(new ProgressEvent(this, 
                                                z, 
                                                "Retrieving " + concatProgramNames(predictionPrograms) + " results"));
    if (!skipData) 
      getChadoAdapter().addGenePredictionResults(conn, predictionPrograms, results, featLocImp, null);
    String umsg = "Genscan and piecegenie results retrieved";
    adapter.fireProgressEvent(new ProgressEvent(this, new Double(100.0), umsg));
    logger.debug(umsg);

    // ------------------------------------------------
    // Copy annotated genes into result tier (optional)
    // ------------------------------------------------    
    if (getCopyGeneModelsIntoResultTier()) {
      getChadoAdapter().copyAnnotatedGenesIntoResultTier(annotations, results, "Copy_of_Annotation", null);
    }
 
    boolean getTargetSeqs = true;//false;
    // get target seqs in same query, optimal for fb data as hits go to many diff seqs
    boolean getAlignSeqs = true;
    //boolean getAlignSeqs = false;
    String primaryScoreColumn = "rawscore";
    boolean joinWithFeatureProp = false;
    boolean getTargetSeqDescriptions = true;

    // sim4, blastx, and tblastx
    ChadoProgram[] hitProgs = getSearchHitPrograms();
    m = "Retrieving " + concatProgramNames(hitProgs) + " results";
    ProgressEvent p = new ProgressEvent(this, z, m);
    adapter.fireProgressEvent(p);
    boolean getTgtSqSep = true;
    boolean makeSeqsLazy = true;
    if (!skipData)
      getChadoAdapter().addSearchHits(conn, hitProgs, getTargetSeqs,getTgtSqSep,
                                      makeSeqsLazy,getTargetSeqDescriptions,
                                      joinWithFeatureProp,seq,results,
                                      primaryScoreColumn,getAlignSeqs,featLocImp);
    umsg = "Sim4 and blastx retrieved";
    adapter.fireProgressEvent(new ProgressEvent(this,d100,umsg));
    logger.debug(umsg);


    // ------------------------------------------------
    // Add annotations and results
    // ------------------------------------------------
    cset.setAnnots(annotations);
    cset.setResults(results);

    // ------------------------------------------------
    // Close JDBC connection
    // ------------------------------------------------
    try {
      // i guess it makes sense to close here
      conn.close();
    } catch (SQLException sqle) {
      logger.error(this + ": failed to close JDBC Connection", sqle);
    }

    return cset;
  } // end of getCurationSet
  
  /** Just set start of translation, let apollo calculate stop. This mimics the 
      game adapter. Can the stop be different than the calculated stop and if so 
      how does chado represent that? This will do for now i think. 
      I believe the end of the cds is the protein is one less than the translation
      stop*/
  public void setTranslationStartAndStop(Transcript trans, ChadoCds cds) {
    boolean calculateEnd = true;
    trans.setTranslationStart(cds.getStart(),calculateEnd);
  }

  public void setTranslationStartAndStop(FeatureSet trans, ChadoCds cds) {
    boolean calculateEnd = false;
    // JC: explicitly setting translation end to maintain backwards compatibility
    trans.setTranslationStart(cds.getStart(),calculateEnd);
    trans.setTranslationEnd(cds.getEnd());
  }
  
  private String concatProgramNames(String[] names) {
    String msg = "";
    if (names == null)
      return msg;
    int size = names.length;
    for (int i = 0; i < size; i++) {
      msg += names[i];
      if (i < size - 2)
        msg += ", ";
      else if (i < size - 1)
        msg += " and ";
    }
    return msg;
  }
  
//PredictedCdsRetrieval   
  private String concatProgramNames(ChadoProgram[] progs) {
	  String retStr = "";
	  if (progs == null)
		  return retStr;
	  for (int i=0; i<progs.length; i++){
		  retStr += progs[i].getProgram();
		  if (i<progs.length - 2)
			 retStr += ", ";
		  else if (i<progs.length -1)
			  retStr += " and ";
	  }
	  return retStr; 
  }
  
}
