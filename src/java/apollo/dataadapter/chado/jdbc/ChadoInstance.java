package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import apollo.dataadapter.Region;
import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.DataInput;
import apollo.dataadapter.chado.SeqType;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.Transcript;

/** 
 * An interface whose goal is to capture all the observed differences 
 * between disparate sites' (e.g., FlyBase, TIGR) chado database usage.
 *
 * @version $Revision: 1.49 $ $Date: 2008/10/03 16:50:26 $ $Author: gk_fan $
 */
public interface ChadoInstance extends Cloneable {

  // -----------------------------------------------------------------------
  // Instance ID
  // -----------------------------------------------------------------------

  // JC: I think this is only used in an error message right now?
  public void setId(String id);

  // -----------------------------------------------------------------------
  // JdbcChadoAdapter
  // -----------------------------------------------------------------------

  // JC: the presence of these get/set methods suggests that the ChadoInstance
  // hasn't been cleanly separated from the JdbcChadoAdapter.  It makes sense
  // that the adapter would need to invoke methods of the instance, but is it
  // necessary to have method calls in the other direction?  (Certainly it is
  // so long as ChadoInstance implements the big "getCurationSet" methods,
  // but perhaps the bulk of these methods could be factored back out into the
  // adapter class/config. file?)

  public void setChadoAdapter(JdbcChadoAdapter jdbcAdapter);
  public JdbcChadoAdapter getChadoAdapter();

  // -----------------------------------------------------------------------
  // Cached StrandedFeatureSets
  // -----------------------------------------------------------------------

  public StrandedFeatureSet getResultStrandedFeatSet();
  public StrandedFeatureSet getAnnotStrandedFeatSet();

  // JC: how about getTopLevelFeatureLocImplementation to better match getTopLevelFeatType?
  public FeatureLocImplementation getTopFeatLoc();

  // -----------------------------------------------------------------------
  // CurationSet retrieval
  // -----------------------------------------------------------------------

  /**
   * Retrieve all the curation data (gene models, gene predictions, search results, etc.)
   * for a single chado feature/sequence.
   *
   * @param adapter  ChadoAdapter on whose behalf the database accesses are to be performed.
   * @param seqType  cvterm.name that corresponds to the sequence identified by <code>seqId</code>
   * @param seqId    Chado feature_id of the sequence to be displayed/annotated in Apollo.
   * @return a CurationSet containing all the features on <code>seqId</code>
   */
  public CurationSet getCurationSet(ChadoAdapter adapter,
                                    String seqType,
                                    String seqId);
  
  /**
   * Retrieve all the curation data (gene models, gene predictions, search results, etc.)
   * for a given region on a single chado feature/sequence.
   *
   * @param adapter  ChadoAdapter on whose behalf the database accesses are to be performed.
   * @param seqType  cvterm.name that corresponds to the sequence identified by <code>location</code>
   * @param location The region for which to retrieve data.
   * @param conn     JDBC connection to the database
   * @return a CurationSet containing all the features in the region given by <code>location</code>
   */
  public CurationSet getCurationSetInRange(ChadoAdapter adapter,
                                           String seqType,
                                           Region location);

  // -----------------------------------------------------------------------
  // Setting gene and transcript names
  // -----------------------------------------------------------------------

  /** 
   * Return the column in the chado feature table where the gene name can be found.
   * The adapter will call setName() on each new gene using the value in this field.
   *
   * @return The name of a field/column in the chado "feature" table.
   */
  public String getGeneNameField();

  /**
   * Return the column in the chado feature table where the transcript name can be found.
   * The adapter will call setName() on each new transcript using the value in this field.
   *
   * @return The name of a field/column in the chado "feature" table.
   */
  public String getTranscriptNameField();

  // -----------------------------------------------------------------------
  // CDS feature retrieval
  // -----------------------------------------------------------------------

  /**
   * Return an SQL query that will retrieve all CDS features within a specified
   * range.  The way that CDS and protein/polypeptide features are stored in 
   * different chado instances is sufficiently different that the entire SQL query
   * must be returned.  The SQL result set must contain (at least) the following 
   * fields:
   *
   * <ul>
   *  <li>fmin</li>
   *  <li>fmax</li>
   *  <li>strand</li>
   *  <li>cds_name</li>
   *  <li>transcript_uniquename</li>
   *  <li>transcript_seq</li>
   *  <li>protein_name</li>
   *  <li>protein_seq</li>
   * </ul>
   *
   * @param featLocImp  The region whose CDS features the SQL query should retrieve.
   * @return A string containing a valid SQL query.
   */ 
  public String getCdsSql(FeatureLocImplementation featLocImp)
    throws RelationshipCVException;

  /**
   * A variant of <code>getCdsSql</code> that returns an SQL query that only fetches
   * a CDS feature if it was predicted by one of the programs in <code>chadoPrgs</code>
   * for which <code>isRetrieveCDS()</code> is true.
   *
   * @param featLocImp  The region whose CDS features the SQL query should retrieve.
   * @param chadoPrgs   Fetch only CDS features predicted by one of these programs.
   * @return A string containing a valid SQL query.
   * @throws RelationshipCVException
   * @see #getCdsSql(FeatureLocImplementation)
   */
  public String getPredictedCdsSql(FeatureLocImplementation featLocImp, 
				   ChadoProgram[] chadoPrgs)
    throws RelationshipCVException;

  /** 
   * Using ChadoCds set start and stop of transcript. Flybase just sets start,
   * lets apollo calculate stop. TIGR sets start and stop explicitly. 
   *
   * @param trans  Transcript whose start and stop should be set.
   * @param cds    CDS used to determine start and stop
   */
  public void setTranslationStartAndStop(Transcript trans, ChadoCds cds);

  /** 
   * Using ChadoCds set the start and stop of translation for a FeatureSet
   * (i.e. predicted transcript).
   *
   * @param trans  Transcript whose start and stop should be set.
   * @param cds    CDS used to determine start and stop
   */
  public void setTranslationStartAndStop(FeatureSet trans, ChadoCds cds);

  // -----------------------------------------------------------------------
  // Search hit feature names and types
  // -----------------------------------------------------------------------

  // JC: consider adding "SearchHit" to these method names?

  /**
   * Return the name to use for a target sequence in a search hit.
   * Used in JdbcChadoAdapter.addSearchHits
   *
   * @param chadoName  chado feature.uniquename of the feature/sequence
   * @param species    organism name generated by JdbcChadoAdapter.getOrganismFullName
   * @param alignType  cv.name for the leaf alignment/search hit object
   * @return The string that will be used to name the target Sequence in the search hit.
   */
  public String getTargetName(String chadoName,
			      String species,
			      String alignType);

  /** 
   * Return the Apollo type for a search hit feature.
   *
   * @param alignType       cv.name for the leaf alignment/search hit object
   * @param program         value from analysis.program for the search hit
   * @param programversion  value from analysis.programversion for the searc
   * @param targetSp        organism name generated by JdbcChadoAdapter.getOrganismFullName
   * @param featProp        optional featureprop value associated with the search hit
   * @return A string containing the type of the specified search hit feature.
   */
  public String getFeatureType(String alignType,
			       String program,
			       String programversion,
			       String targetSp,
			       String sourcename,
			       String featProp);

  // -----------------------------------------------------------------------
  // Names of CVs (all from config. file)
  // -----------------------------------------------------------------------

  // TODO: add "Name" to XML tags so they match the method names?

  /**
   * @return Name of the chado CV from which to draw feature terms.
   */
  public String getFeatureCVName();
  public void setFeatureCVName(String cvname);

  /**
   * @return Name of the chado CV from which to draw relationship terms.
   */
  public String getRelationshipCVName();
  public void setRelationshipCVName(String relCV);

  /**
   * @return Name of the chado CV from which to draw property type terms.
   */
  public String getPropertyTypeCVName();
  public void setPropertyTypeCVName(String cv);

  // -----------------------------------------------------------------------
  // Names of cvterms (all from config. file)
  // -----------------------------------------------------------------------

  // TODO - make it clear which cvterms come from which CVs
  
  /** return string for "part_of" relationship (tigr part_of, fb partof) */
  public String getPartOfCvTerm();
  public void setPartOfCvTerm(String term);
  
  public String getTranscriptTerm();
  public Long getTranscriptCvTermId();
  // JC: method/property names are not consistent; why not use getTransProtRelationCvTerm?

  public String getTransProtRelationTerm();
  public void setTransProtRelationTerm(String term);

  public String getTransGeneRelationTerm();
  public void setTransGeneRelationTerm(String term);

  public String getExonTransRelationTerm();
  public void setExonTransRelationTerm(String term);

  public String getCdsTransRelationTerm();
  public void setCdsTransRelationTerm(String term);

  public String getPolypeptideCdsRelationTerm();
  public void setPolypeptideCdsRelationTerm(String term);

  public String getPolypeptideTransRelationTerm();
  public void setPolypeptideTransRelationTerm(String term);

  // JC: method/property names are not consistent; why not use getPolypeptideCvTerm?

  /** Old SO/SOFA uses "protein", new uses "polypeptide" (default) */
  public String getPolypeptideType();
  public void setPolypeptideType(String polypeptideType);
  
  public String getSyntenyRelationshipType();
  public void setSyntenyRelationshipType(String syntenyRelationshipType);    
  
  public String getFeatureOwnerPropertyTerm();
  public void setFeatureOwnerPropertyTerm(String term);

  public String getFeatureCreateDatePropertyTerm();
  public void setFeatureCreateDatePropertyTerm(String term);
  
  public void setCommentPropertyTerm(String commentTerm);
  public String getCommentPropertyTerm();

  public void setSynonymPropertyTerm(String commentTerm);
  public String getSynonymPropertyTerm();
  
  // -----------------------------------------------------------------------
  // Sequence description name and cvterm
  // -----------------------------------------------------------------------
  
  public String getSeqDescriptionCVName();
  public void setSeqDescriptionCVName(String cvName);
  
  public String getSeqDescriptionTerm();
  public void setSeqDescriptionTerm(String term);

  // -----------------------------------------------------------------------
  // One and three-level annotation types
  // -----------------------------------------------------------------------
  
  /**
   * Get a list of annotation types that are saved in the Chado database as 
   * one-level features.  A one-level annotation is a non-analysis feature
   * that is featureloc'ed directly to a sequence of interest, with no 
   * additional structure.
   *
   * @return a list of chado cvterm.name
   */
  public List getOneLevelAnnotTypes();

  /**
   * Set a list of annotation types that are saved in the Chado database as 
   * one-level features.
   *
   * @param features a list of chado cvterm.name
   */
   // TODO: rename this -> setOneLevelAnnotFeatures for distinction with results
  public void setOneLevelAnnotTypes(List features);
  
  /**
   * Get a list of features that are stored in the chado db as three-level features
   * (gene-namedFeature-exon).
   * @return a list of chado cvterm.name (?)
   */
  public List getThreeLevelAnnotTypes();
  
  /**
   * Set a list of features that are saved in the chado db as three-level features.
   * (gene-namedFeature-exon).
   *
   * @param features a list of feature names.
   */
  public void setThreeLevelAnnotTypes(List features);

  // -----------------------------------------------------------------------
  // Gene prediction programs
  // -----------------------------------------------------------------------

  /**
   * @return an array of program objects. These objects are created during the xml configuration parsing.
   */
  public ChadoProgram[] getGenePredictionPrograms();
  public void setGenePredictionPrograms(ChadoProgram[] predictionPrograms);
  public String getTranscriptName(FeatureSet tfs, String program);

  // -----------------------------------------------------------------------
  // Search hits
  // -----------------------------------------------------------------------

  // JC: It might be nice to change the name of this property (and any others
  // that are implemented the same way) to indicate that it's a LIKE query, 
  // not an equijoin.

  /** Set array of strings for all search hit programs. This comes from
      xml configuration. A program string can have % wildcards in them as they
      are queried with "like" not "=". */
  public ChadoProgram[] getSearchHitPrograms();
  public void setSearchHitPrograms(ChadoProgram[] searchHitPrograms);

  /** returns true if search hits have feature locs. Hits are the feature sets
      that hold the leaf/hsps. The leaves always have feat-locs, but the featSet/hits
      dont always have feat locs, which makes it impractical to retrieve out of
      range leaves/hsps. Currently rice has hit feat locs, fly & tigr dont */
  public boolean searchHitsHaveFeatLocs();
  public void setSearchHitsHaveFeatLocs(boolean haveFeatLocs);

  /**
   * Return the Apollo analysis type for a chado analysis.
   *
   * @param analysisId chado analysis.analysis_id
   * @param program chado analysis.program
   * @param programversion chado analysis.programversion
   * @param sourcename chado analysis.sourcename
   *
   * @return The Apollo type to be used for all features from this analysis.
   */
  public String getAnalysisType(long analysisId, String program, String programversion, String sourcename);

  // -----------------------------------------------------------------------
  // Result programs
  // -----------------------------------------------------------------------

  /** for simple results that are only 1 level deep */
  public ChadoProgram[] getOneLevelResultPrograms();
  public void setOneLevelResultPrograms(ChadoProgram[] oneLevelResultPrograms);

  // -----------------------------------------------------------------------
  // SequenceTypes
  // -----------------------------------------------------------------------

  // TODO: change SeqType -> SequenceType for consistency with XML/other methods

  // JC: Why no getSeqTypeList()?  This is the only straightforward property of the bunch:
  public void setSeqTypeList(List seqTypes);

  /** Return number of seq types for instance */
  public int getSeqTypesSize();
  /** Return ith SeqType */
  public SeqType getSeqType(int i);
  /** SeqType for name */
  public SeqType getSeqType(String name);
  /** Returns seq type that has location */
  public SeqType getLocationSeqType();
  /** Does type require start & end data */
  public boolean typeHasStartAndEnd(String typeName);

  /** Top level feature type is needed for saving back to chado. Also for querying
      locations from the command line (where type isn't given). This is configured
      in chado-adapter cfg file */
  public String getTopLevelFeatType();

  /** If dataInput is a location, change its SO type to be <code>getLocationSeqType()</code> */
  public void checkForLocation(DataInput dataInput);

  // -----------------------------------------------------------------------
  // Cache preferences
  // -----------------------------------------------------------------------

  public void setCacheAnalysisTable(boolean cache);
  public boolean cacheAnalysisTable();

  // -----------------------------------------------------------------------
  // Miscellaneous properties
  // -----------------------------------------------------------------------

  // JC: why no getRetrieveAnnotations?
  public void setRetrieveAnnotations(boolean retrieveAnnots);

  public void setCopyGeneModelsIntoResultTier(boolean copyModels);
  public boolean getCopyGeneModelsIntoResultTier();

  public boolean getUseSynonyms();
  public void setUseSynonyms(boolean useSynonyms);
  
  // -----------------------------------------------------------------------
  // Writeback adapter
  // -----------------------------------------------------------------------

  /** This is the beginning of trying to get chado-adapter.xml and
      transactionXMLTemplate non-redundant, so you dont have to configure the same
      things twice. A bunch of the preamble macros can be figured now from
      chado-adpter.xml config and dont need to be in the template */
  public List getChadoTransMacros();

  public void setWritebackTemplateFile(String templateFile);
  public String getWritebackTemplateFile();

  // PureJDBCWriteAdapter parameters
  public void setPureJDBCWriteMode(boolean writeMode);
  public boolean getPureJDBCWriteMode();

  public void setPureJDBCCopyOnWrite(boolean copyOnWrite);
  public boolean getPureJDBCCopyOnWrite();

  public void setPureJDBCNoCommit(boolean noCommit);
  public boolean getPureJDBCNoCommit();

  public void setPureJDBCUseCDS(boolean useCDS);
  public boolean getPureJDBCUseCDS(); 
  
  public void setLogDirectory(String path);
  public String getLogDirectory();

  public void setQueryFeatureIdWithUniquename(boolean newVal);
  public boolean getQueryFeatureIdWithUniquename();

  public void setQueryFeatureIdWithName(boolean newVal);
  public boolean getQueryFeatureIdWithName();

  // -----------------------------------------------------------------------
  // Cloneable
  // -----------------------------------------------------------------------

  public ChadoInstance cloneInstance();

}
