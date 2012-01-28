package apollo.dataadapter.chado.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.*;

import org.bdgp.util.ProgressEvent;

import apollo.config.Config;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.FeatureSet;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.chado.ChadoTransaction;
import apollo.dataadapter.chado.SeqType;

abstract class AbstractChadoInstance implements ChadoInstance {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AbstractChadoInstance.class);
  private final static Double Z = new Double(0.0);
  private final static Double D100 = new Double(100.0);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private JdbcChadoAdapter jdbcChadoAdapter;
  private String logDirectory = null;
  private ChadoProgram[] genePredictionPrograms;
  private ChadoProgram[] searchHitPrograms;
  private ChadoProgram[] oneLevelResultPrograms;
  private List oneLevelAnnotTypes;
  private List threeLevelAnnotTypes;

  // JDBCTransactionWriter
  private String writebackTemplateFile;
  private List chadoTransactionMacros; // TODO: add 'writeback' to make name consistent?

  // PureJDBCTransactionWriter
  private boolean pureJDBCWriteMode = false;
  private boolean pureJDBCCopyOnWrite = false;
  private boolean pureJDBCNoCommit = false;
  private boolean pureJDBCUseCDS = true;  

  // cv names
  private String featureCVName = null;
  private String relationshipCVName = null;
  private String propertyTypeCVName = null;
  private String seqDescriptionCVName = null;

  // cvterm names (defaults appear below
  private String polypeptideType = "polypeptide";
  private String seqDescriptionTerm = "description";
  private String partOfCvTerm = "part_of";
  private String transProtRelationCvTerm = "derives_from";
  private String transGeneRelationCvTerm = "derives_from";
  private String exonTransRelationCvTerm = "part_of";
  private String cdsTransRelationCvTerm = "derives_from";
  private String polypeptideCdsRelationCvTerm = "derives_from";
  private String polypeptideTransRelationCvTerm = null;
  private String transcriptCvTerm = "transcript";
  private String syntenyRelationshipType = "paralogous_to";
  private String featureOwnerPropertyTerm = "owner";
  private String featureCreateDatePropertyTerm = "date";
  private String commentPropertyTerm = "comment";
  private String synonymPropertyTerm = "synonym";

  // boolean flags
  private boolean searchHitsHaveFeatLocs = false;
  private boolean cacheAnalysisTable = true;
  private boolean retrieveAnnotations = true;
  private boolean copyGeneModelsIntoResultTier = false;
  private boolean queryFeatureIdWithUniquename = true;
  private boolean queryFeatureIdWithName = true;

  private String topLevelFeatType;
  private String id;
  
  /** A list of seq types allowing the user to choose from */
  private List seqTypes;
  private StrandedFeatureSet results;
  private StrandedFeatureSet annots;
  private FeatureLocImplementation topFeatLoc;
  
  private boolean useSynonyms;

  protected AbstractChadoInstance() {}

  protected AbstractChadoInstance(JdbcChadoAdapter jdbcChadoAdapter) {
    this.jdbcChadoAdapter = jdbcChadoAdapter;
  }

  public ChadoInstance cloneInstance() {
    try {
      // do we need to deep clone anything - probably not
      return (ChadoInstance)this.clone();
    } catch (CloneNotSupportedException e) {
      return null; // shouldnt happen
    }
  }

  public void setId(String id) { this.id = id; }

  // rename this getJdbcChadoAdapter - confusing!
  public void setChadoAdapter(JdbcChadoAdapter jdbcAdapter) {
    this.jdbcChadoAdapter = jdbcAdapter;
  }
  
  public JdbcChadoAdapter getChadoAdapter() {
    return this.jdbcChadoAdapter;
  }

  protected Connection getConnection() {
    return getChadoAdapter().getConnection();
  }

  public void setWritebackTemplateFile(String templateFile) {
    writebackTemplateFile = templateFile;
  }

  public String getWritebackTemplateFile() { return writebackTemplateFile; }

  public void setPureJDBCWriteMode(boolean writeMode) {
    pureJDBCWriteMode = writeMode;
  }

  public boolean getPureJDBCWriteMode() {
    return pureJDBCWriteMode;
  }

  public void setPureJDBCCopyOnWrite(boolean copyOnWrite) {
    pureJDBCCopyOnWrite = copyOnWrite;
  }

  public boolean getPureJDBCCopyOnWrite() {
    return pureJDBCCopyOnWrite;
  }

  public void setPureJDBCNoCommit(boolean noCommit) {
    pureJDBCNoCommit = noCommit;
  }

  public boolean getPureJDBCNoCommit() {
    return pureJDBCNoCommit;
  }
  
  public void setPureJDBCUseCDS(boolean useCDS) {
    this.pureJDBCUseCDS = useCDS;
  }

  public boolean getPureJDBCUseCDS() {
    return pureJDBCUseCDS;
  } 

  public void setLogDirectory(String path) {
    this.logDirectory = path;
  }

  public String getLogDirectory() {
    return this.logDirectory;
  }

  public void setGenePredictionPrograms(ChadoProgram[] genePredictionPrograms) {
    this.genePredictionPrograms = genePredictionPrograms;
  }

  public boolean getUseSynonyms()
  {
    return useSynonyms;
  }
  
  public void setUseSynonyms(boolean useSynonyms)
  {
    this.useSynonyms = useSynonyms;
  }
  
  /**
   * Return array  for all the gene prediction programs. This comes from xml configuration
   * @return an array of ChadoProgram
   */
  public ChadoProgram[] getGenePredictionPrograms() {
    return genePredictionPrograms;
  }
  
  public String getTranscriptName(FeatureSet tfs, String program) {
  	return  program+":"+tfs.getStart()+"-"+tfs.getEnd();
  }
  
  public void setSearchHitPrograms(ChadoProgram[] searchHitPrograms) {
    this.searchHitPrograms = searchHitPrograms;
  }

  /** Return array of strings for all the hit programs. This comes from
      xml configuration */
  public ChadoProgram[] getSearchHitPrograms() {
    return searchHitPrograms;
  }

  public void setOneLevelResultPrograms(ChadoProgram[] oneLevelResults) {
    oneLevelResultPrograms = oneLevelResults;
  }
  
  public ChadoProgram[] getOneLevelResultPrograms() {
    return oneLevelResultPrograms;
  }

  /**
   * Get a list of annotation types that are saved in Chado db as one level features
   * from a list of types.
   * @return
   */
  public List getOneLevelAnnotTypes() {
    if (oneLevelAnnotTypes == null)
      oneLevelAnnotTypes = new ArrayList(0);
    return oneLevelAnnotTypes;
  }
  
  public void setOneLevelAnnotTypes(List features) {
    oneLevelAnnotTypes = new ArrayList(features);
  }
  
  /**
   * Get a list of features that are stored in the chado db as three level features
   * (gene-namedFeaure-exon).
   * @return
   */
  public List getThreeLevelAnnotTypes() {
    if (threeLevelAnnotTypes == null)
      threeLevelAnnotTypes = new ArrayList(0);
    return threeLevelAnnotTypes;
  }
  
  public void setThreeLevelAnnotTypes(List features) {
    threeLevelAnnotTypes = new ArrayList(features);
  }

  /** returns true if search hits have feature locs. Hits are the feature sets
      that hold the leaf/hsps. The leaves always have feat-locs, but the featSet/hits
      dont always have feat locs, which makes it impractical to retrieve out of
      range leaves/hsps. Currently rice has hit feat locs, fly & tigr dont  */
  public boolean searchHitsHaveFeatLocs() {
    return searchHitsHaveFeatLocs;
  }
  
  public void setSearchHitsHaveFeatLocs(boolean haveFeatLocs) {
    this.searchHitsHaveFeatLocs = haveFeatLocs;
  }

  protected Long getRelationshipCVTermId(String name) {
    return getChadoAdapter().getRelationshipCVTermId(name);
  }
  
  public void setRelationshipCVName(String relCV) {
    relationshipCVName = relCV;
  }

  public String getRelationshipCVName() {
    return relationshipCVName;
  }
  
  /** return string for part of relationship (tigr part_of, fb partof) */
  public String getPartOfCvTerm() { return partOfCvTerm; }
  public void setPartOfCvTerm(String term) {
    partOfCvTerm = term;
  }

  public String getTransProtRelationTerm() { return transProtRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setTransProtRelationTerm(String term) { transProtRelationCvTerm = term; }

  public String getTransGeneRelationTerm() { return transGeneRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setTransGeneRelationTerm(String term) { transGeneRelationCvTerm = term; }

  public String getExonTransRelationTerm() { return exonTransRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setExonTransRelationTerm(String term) { exonTransRelationCvTerm = term; }

  public String getCdsTransRelationTerm() { return cdsTransRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setCdsTransRelationTerm(String term) { cdsTransRelationCvTerm = term; }

  public String getPolypeptideCdsRelationTerm() { return polypeptideCdsRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setPolypeptideCdsRelationTerm(String term) { polypeptideCdsRelationCvTerm = term; }

  public String getPolypeptideTransRelationTerm() { return polypeptideTransRelationCvTerm; }
  /** Need to set (from chado-adapter.xml) if diff from default */
  public void setPolypeptideTransRelationTerm(String term) { polypeptideTransRelationCvTerm = term; }

  class ProducedByException extends RelationshipCVException {
    ProducedByException(String m) { super(m); }
  }

  protected Long getProducedByCVTermId() throws ProducedByException {
    Long id = getRelationshipCVTermId(getTransProtRelationTerm());
    if (id == null) {
      String m = "produced by cv term '"+getTransProtRelationTerm()+"' not found in "+
        "relationship cv "+getRelationshipCVName();
      ProducedByException e = new ProducedByException(m);
      logger.error(m, e);
      throw e;
    }
    return id;
  }

  public void setFeatureCVName(String featCV) {
    featureCVName = featCV;
  }

  public String getFeatureCVName() { return featureCVName; }

  /** Old SO/SOFA uses "protein", new uses "polypeptide" (default) */
  public void setPolypeptideType(String polypeptideType) {
    this.polypeptideType = polypeptideType;
  }
  public String getPolypeptideType() {
    return polypeptideType;
  }
  
  
  public void setSyntenyRelationshipType(String syntenyRelationshipType) {
    this.syntenyRelationshipType = syntenyRelationshipType;
  }
  public String getSyntenyRelationshipType() {
    return syntenyRelationshipType;
  }  
  
  public void setFeatureOwnerPropertyTerm(String ownerTerm) {
    this.featureOwnerPropertyTerm = ownerTerm; 
  }
  public String getFeatureOwnerPropertyTerm() {
    return this.featureOwnerPropertyTerm;
  }

  public void setFeatureCreateDatePropertyTerm(String dateTerm) {
    this.featureCreateDatePropertyTerm = dateTerm; 
  }
  public String getFeatureCreateDatePropertyTerm() {
    return this.featureCreateDatePropertyTerm;
  }

  public void setCommentPropertyTerm(String commentTerm) {
    this.commentPropertyTerm = commentTerm; 
  }
  public String getCommentPropertyTerm() {
    return this.commentPropertyTerm;
  }  

  public String getSynonymPropertyTerm()
  {
    return synonymPropertyTerm;
  }
  public void setSynonymPropertyTerm(String synonymPropertyTerm)
  {
    this.synonymPropertyTerm = synonymPropertyTerm;
  }
  
  public String getSeqDescriptionCVName() {
    // use propertyTypeCVName by default for reverse compatability
    return (this.seqDescriptionCVName == null) ? propertyTypeCVName : seqDescriptionCVName;
  }
  public void setSeqDescriptionCVName(String cvName) {
    this.seqDescriptionCVName = cvName;
  }
  
  public String getSeqDescriptionTerm() {
    return this.seqDescriptionTerm;
  }
  public void setSeqDescriptionTerm(String term) {
    this.seqDescriptionTerm = term;
  }

  protected Long getPolypeptideCVTermId() {
    return getFeatureCVTermId(getPolypeptideType());
  }
  
  public Long getTranscriptCvTermId() { 
  	return getFeatureCVTermId(transcriptCvTerm); 
  }
  public String getTranscriptTerm() {
  	return transcriptCvTerm;
  }

  /** calls JdbcChadoAdapter -> cvterms should have its own class */
  protected Long getFeatureCVTermId(String name) {
    return getChadoAdapter().getFeatureCVTermId(name);
  }
  
  public void setPropertyTypeCVName(String cv) {
    propertyTypeCVName = cv;
  }
  public String getPropertyTypeCVName() { return propertyTypeCVName; }


  /** 
   * Returns type string as program:sourcename; override if this is 
   * not what your database requires. 
   */
  public String getAnalysisType(long analysisId, String program, String programversion, String sourcename) {
    if (sourcename != null && sourcename.length() > 0) {
      return program + ":" + sourcename;
    }
    return program;
  }

  public void setCacheAnalysisTable(boolean cache) {
    this.cacheAnalysisTable = cache;
  }

  public boolean cacheAnalysisTable() {
    return this.cacheAnalysisTable;
  }

  public void setRetrieveAnnotations(boolean retrieveAnnotations) {
    this.retrieveAnnotations = retrieveAnnotations;
  }

  public void setCopyGeneModelsIntoResultTier(boolean copyModels) {
    this.copyGeneModelsIntoResultTier = copyModels;
  }

  public boolean getCopyGeneModelsIntoResultTier() {
    return this.copyGeneModelsIntoResultTier;
  }

  public void setQueryFeatureIdWithUniquename(boolean newVal) {
    this.queryFeatureIdWithUniquename = newVal;
  }

  public boolean getQueryFeatureIdWithUniquename() {
    return this.queryFeatureIdWithUniquename;
  }

  public void setQueryFeatureIdWithName(boolean newVal) {
    this.queryFeatureIdWithName = newVal;
  }

  public boolean getQueryFeatureIdWithName() {
    return this.queryFeatureIdWithName;
  }

  protected void getAnnotations(Connection conn,SequenceI refSeq, StrandedFeatureSet sfs,
                                FeatureLocImplementation featLocImp, boolean getFeatProps,
                                boolean getSynonyms, boolean getDbXRefs,
                                ChadoAdapter adapter) {
    if (!retrieveAnnotations)
      return;
    getChadoAdapter().addOneLevelAnnotations(conn, refSeq, sfs, featLocImp, getFeatProps, getSynonyms, getDbXRefs);
    adapter.fireProgressEvent(new ProgressEvent(this,D100, "One Level Annotations retrieved"));

    adapter.fireProgressEvent(new ProgressEvent(this, Z, "Retrieving gene models"));
    getChadoAdapter().addAnnotationModels(conn, refSeq, sfs, featLocImp, getFeatProps,
                                    getSynonyms,getDbXRefs,getThreeLevelAnnotTypes());
    // Continue to fetch gene models: one level AnnotatedFeatures
    adapter.fireProgressEvent(new ProgressEvent(this, new Double(75.0), "Gene models retrieved"));

  }

  /** This is the beginning of trying to get chado-adapter.xml and
      transactionXMLTemplate non-redundant, so you dont have to configure the same
      things twice. A bunch of the preamble macros can be figured now from
      chado-adpter.xml config and dont need to be in the template */
  public List getChadoTransMacros() {
    if (chadoTransactionMacros != null)
      return chadoTransactionMacros;

    chadoTransactionMacros = new ArrayList(4);

    // Feature CV
    ChadoTransaction chadoTrans = makeCvTrans(getFeatureCVName());
    chadoTransactionMacros.add(chadoTrans);

    // Relationship CV
    chadoTrans = makeCvTrans(getRelationshipCVName());
    chadoTransactionMacros.add(chadoTrans);

    // Lookup protein/polypep
    chadoTrans = makeCvTermTrans(getPolypeptideType(),getFeatureCVName());
    chadoTransactionMacros.add(chadoTrans);

    chadoTrans = makeCvTermTrans(getTransProtRelationTerm(),getRelationshipCVName());
    chadoTransactionMacros.add(chadoTrans);

    // more to come...

    return chadoTransactionMacros;
  }

  private ChadoTransaction makeCvTrans(String cv) {
    ChadoTransaction chadoTrans = new ChadoTransaction();
    chadoTrans.setOperation(ChadoTransaction.LOOKUP);
    chadoTrans.setTableName("cv");
    chadoTrans.setID(cv);
    chadoTrans.addProperty("name",cv);
    return chadoTrans;
  }

  private ChadoTransaction makeCvTermTrans(String id, String cv) {
    ChadoTransaction chadoTrans = new ChadoTransaction();
    chadoTrans.setOperation(ChadoTransaction.LOOKUP);
    chadoTrans.setTableName("cvterm");
    chadoTrans.setID(id);
    chadoTrans.addProperty("cv_id",cv);
    chadoTrans.addProperty("name",id); // id & name are same
    return chadoTrans;
  }

  public void setSeqTypeList(List seqTypes) {
    this.seqTypes = seqTypes;
  }

  public int getSeqTypesSize() { 
    if (seqTypes == null)
      return 0;
    return seqTypes.size();
  }
  public SeqType getSeqType(int i) { return (SeqType)seqTypes.get(i); }

  /** Assumes theres only 1 location seq type - returns first it finds */
  public SeqType getLocationSeqType() {
    for (int i=0; i<getSeqTypesSize(); i++) {
      SeqType seqType = getSeqType(i);
       if (seqType.hasStartAndEnd())
         return seqType;
    }
    return null; // shouldnt happen
  }

  /** Top level feature type is needed for saving back to chado. Also for querying
      locations from the command line (where type isnt given). This is configured
      in chado-adapter cfg file under <seqTypes> <type> with <isTopLevel>true. 
      If top level is not configged tries to gleen from seq types - if theres any
      seqTypes with start&end, uses that, if no locs then just looks for 'chromosome'
      in type name, and if all that fails just sets to "chromosome". */
  public String getTopLevelFeatType() {
    if (topLevelFeatType != null)
      return topLevelFeatType;

    // check first if explicitly set with <isTopLevel> in chado config
    for (int i=0; i<getSeqTypesSize(); i++) {
      SeqType seqType = getSeqType(i);
      if (seqType.isTopLevelFeatType()) {
        topLevelFeatType = seqType.getName();
        return topLevelFeatType;
      }
    }

    // not explicitly set, usually the seqType with start&end is top level, and
    // usually there is only one of these (if theres more than one print msg)
    for (int i=0; i<getSeqTypesSize(); i++) {
      SeqType seqType = getSeqType(i);

       if (seqType.hasStartAndEnd()) {

         // not set - 1st location - hopefully there is only 1 location
         if (topLevelFeatType == null) { 
           topLevelFeatType = seqType.getName();
         }

         // there are 2 locations! yikes
         else {

           String m = "2 locations specified in seq types for instance "+id;

           if (hasChromosomeInString(seqType.getName()))
             topLevelFeatType = seqType.getName();

           if (hasChromosomeInString(topLevelFeatType)) {
             m += "using "+topLevelFeatType+" for top level feat because it has "
               +"'chromosome' in it";
           }

           else {// chromosome not in either type
             m += "Arbitrarily setting top level feat to "+topLevelFeatType+
               ". Set top level feat type explicitly with <isTopLevel>";
           }

           logger.info(m);
         }

      }

    }
    if (topLevelFeatType != null) // got it with location
      return topLevelFeatType;

    // isTopLevel not explicitly set nor is there a location - look for chrom
    for (int i=0; i<getSeqTypesSize(); i++) {
      SeqType seqType = getSeqType(i);
      if (hasChromosomeInString(seqType.getName())) {
        topLevelFeatType = seqType.getName();
        
        logger.info("setting top level feat type to "+topLevelFeatType+
                    " because it had 'chromosome' in it. Set explicitly "+
                    "with <isTopLevel>true in <sequenceTypes> <type>");
        return topLevelFeatType;
      }
    }

    // everything failed - just set to chromosome and hope it works 
    // lets hope it doesnt get to this point - it shouldnt
    topLevelFeatType = "chromosome";
    logger.info("unable to discern top level feat type from config. Just setting "
                       +"it to 'chromosome' and hoping to get lucky. Top level "
                       +"seq type needs to be configured.");
    return topLevelFeatType;
  }

  private boolean hasChromosomeInString(String type) {
    return type.indexOf("chromosome") != -1;
  }

  /** If dataInputs type is configured as a location, then change dataInput to be
      a location - is there a better way to do this? */
  public void checkForLocation(DataInput dataInput) {
    if (dataInput.getSoType() == null && dataInput.isRegion())
      dataInput.setSoType(getLocationSeqType().getName());
    if (isLocation(dataInput.getSoType()) && !dataInput.isRegion()) {
      // it is a location - make it so, this causes loc in string to be parsed
      // which is the desired effect
      dataInput.makeDataTypeRegion(); // a little silly
    }
  }
  private boolean isLocation(String type) {
    SeqType st = getSeqType(type);
    if (st == null)
      return false;
    return st.hasStartAndEnd();
  }
  public SeqType getSeqType(String name) {
    for (int i=0; i<getSeqTypesSize(); i++) {
      if (getSeqType(i).getName().equals(name))
        return getSeqType(i);
    }
    return null;
  }
  public boolean typeHasStartAndEnd(String typeName) {
    SeqType st = getSeqType(typeName);
    if (st == null) return false;
    return st.hasStartAndEnd();
  }

  public StrandedFeatureSet getResultStrandedFeatSet() {
    if (results == null)
      results = new StrandedFeatureSet();
    return results;
  }

  public StrandedFeatureSet getAnnotStrandedFeatSet() {
    if (annots == null)
      annots = new StrandedFeatureSet();
    return annots;
  }

  protected void clear() {
    annots = null;
    results = null;
  }

  void setTopFeatLoc(FeatureLocImplementation topFeatLoc) {
    this.topFeatLoc = topFeatLoc;
  }


  public FeatureLocImplementation getTopFeatLoc() {
    return topFeatLoc;
  }

}
