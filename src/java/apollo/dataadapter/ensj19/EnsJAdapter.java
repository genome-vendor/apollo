package apollo.dataadapter.ensj19;

import java.util.*;
import java.io.*;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.datamodel.seq.*;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.Region;
import apollo.dataadapter.*;
//import apollo.util.SequenceUtil;

import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;
import org.apache.log4j.*;

import org.ensembl19.*;
import org.ensembl19.driver.*;
import org.ensembl19.datamodel.Query;
import org.ensembl19.datamodel.InvalidLocationException;
import org.ensembl19.datamodel.Accessioned;
import org.ensembl19.datamodel.Location;
import org.ensembl19.datamodel.LinearLocation;
import org.ensembl19.datamodel.AssemblyLocation;
import org.ensembl19.datamodel.CloneFragmentLocation;
import org.ensembl19.datamodel.CloneFragment;
import org.ensembl19.datamodel.DnaDnaAlignment;
import org.ensembl19.datamodel.DnaProteinAlignment;
import org.ensembl19.datamodel.PredictionTranscript;
import org.ensembl19.datamodel.PredictionExon;
import org.ensembl19.datamodel.SimplePeptideFeature;
import org.ensembl19.datamodel.Feature;
import org.ensembl19.datamodel.RepeatFeature;
import org.ensembl19.datamodel.RepeatConsensus;
import org.ensembl19.datamodel.AssemblyElement;
//uncomment when SupportingFeatures come into ensembl_9_30
//import org.ensembl19.datamodel.SupportingFeature;
import org.ensembl19.util.*;


/**
 * Adaptor providing access to the ensembl database via the ensj-core
 * library.
 *
 * <p>Originally based on EnsCGIAdaptor.
 */
public abstract class EnsJAdapter extends AbstractApolloAdapter {
  public long tstart;
  public long tend;
  //  private DataAdapterUI ui;
  private static final Logger logger = Logger.getLogger( EnsJAdapter.class.getName() );

  public final static String STABLE_ID_PREFIX = "STABLE_ID ";

  final static int ASSEMBLY_LOCATION_MODE = 0;
  final static int CLONE_FRAGMENT_MODE = 1;
  final static int STABLE_ID_MODE = 2;

  private boolean initialisedDriver = false;

  private Location location = null;

  private String region;
  private int mode;
  private SequenceI genomeSeq = null;

  private Driver driver = null;
  private GeneAdaptor geneAdaptor = null;

  private CloneFragmentAdaptor cloneFragmentAdaptor = null;
  private LocationConverter locationConverter = null;

  //  private String seqDriverConfFiles;

  private DnaProteinAlignmentAdaptor dnaProteinAlignmentAdaptor= null;
  private DnaDnaAlignmentAdaptor dnaDnaAlignmentAdaptor= null;

  private SimplePeptideAdaptor simplePeptideAdaptor= null;

  private SimpleFeatureAdaptor simpleFeatureAdaptor= null;

// uncomment when supporting features come into ensembl_9_30
//  private SupportingFeatureAdaptor supportingFeatureAdaptor= null;

  private RepeatFeatureAdaptor repeatFeatureAdaptor= null;
  private PredictionTranscriptAdaptor predictionTranscriptAdaptor= null;

  private VariationAdaptor variationAdaptor= null;

  private String cachedRegion = null;
  private List cachedGenes = null;

  protected Properties stateInformation;

  private Map translationCache = new HashMap();

  // load times used for logging purposes.
  private long geneLoadTime = 0;
  private long dnaProteinLoadTime = 0;
  private long dnaDnaLoadTime = 0;
  //  private long supportingFeatureLoadTime = 0;
  private long simplePeptideLoadTime = 0;
  private long featureLoadTime = 0;
  private long repeatLoadTime = 0;
  private long predictionTranscriptsLoadTime = 0;
  private long variationLoadTime = 0;
  private long sequenceLoadTime = 0;

  private String loggingFile;
  
  IOOperation [] 
    supportedOperations = 
      {
        ApolloDataAdapterI.OP_READ_DATA,
        ApolloDataAdapterI.OP_READ_SEQUENCE,
        ApolloDataAdapterI.OP_APPEND_DATA
      };

  public void init() {
    tstart = System.currentTimeMillis();
    tend   = System.currentTimeMillis();
  }
  
  public void printTime(String message ) {
    tend = System.currentTimeMillis();
    logger.info("--- Time " + (tend-tstart+1)/1000.0  + " " + message);
    tstart = tend;
  }

  public String getName() {
    return "EnsemblJava";
  }

  public String getType() {
    return "Direct ensembl database access";
  }
  
  public DataInputType getInputType() {
    if (getMode() == ASSEMBLY_LOCATION_MODE)
      return DataInputType.BASEPAIR_RANGE;
    else
      return DataInputType.CONTIG;
  }

  // what?
  public String getInput() {
    return region; // is this right??
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public EnsJAdapter() {}

  /**
   * Set the region to be retrieved by subsequent calls to getCurrationSet().
   *
   * @param region region to be retrieved. Region is encoded as parameters
   * separated by spaces "CHR START END".  
  */
  public void setRegion(String region) throws apollo.dataadapter.ApolloAdapterException {
    this.region = region;

    if(region != null){
      if (region.substring(0,4).equals("Chr ")) {
        setMode(ASSEMBLY_LOCATION_MODE);
      } 
      else if (region.startsWith( STABLE_ID_PREFIX ) ) {
        setMode(STABLE_ID_MODE);
      } 
      else {
        setMode(CLONE_FRAGMENT_MODE);
      }
    }
  }

  public Properties getStateInformation() {
    return stateInformation;
  }

  public void setStateInformation(Properties props) {
    String region;
    
    // Disable cache
    cachedRegion=null;

    stateInformation = props;

    setLoggingFile(props.getProperty(StateInformation.LOGGING_FILE));
    
    setDriverConf(props);

    try {
      region = props.getProperty(StateInformation.REGION);
      if(region != null && region.trim().length() > 0){
        setRegion(region);
      }else{
        setRegion(null);
      }
    } catch (apollo.dataadapter.ApolloAdapterException exception) {
      throw new NonFatalDataAdapterException( exception.getMessage() );
    }

  }

  protected CurationSet getCurationSetWithoutClearingData() throws apollo.dataadapter.ApolloAdapterException{
    
    validateDatabaseProperties(getStateInformation());
    
    //
    //Now you can talk to the db, you can initialise the driver.
    try {
      initialiseDriver();
    } catch(ConfigurationException exception) {
      throw new apollo.dataadapter.ApolloAdapterException(
        "Didn't specify enough information to configure database access: "
          + exception.getMessage()
      );
    }
    
    validateRegionSpecified(getStateInformation());
    
    CurationSet curationSet = new CurationSet();
    
    try {

      if (getMode()==ASSEMBLY_LOCATION_MODE || getMode()==STABLE_ID_MODE) {

        AssemblyLocation al = (AssemblyLocation)getLocation();
        curationSet.setChromosome( al.getChromosome() );
        System.out.println ("Set chromosome to " + al.getChromosome());
        curationSet.setLow(al.getStart());
        curationSet.setHigh(al.getEnd());
        curationSet.setStrand( 0 );        
      } else if ( getMode()==CLONE_FRAGMENT_MODE ) {

        CloneFragmentLocation cfl = (CloneFragmentLocation)getLocation();
        curationSet.setChromosome(region);
        curationSet.setLow( cfl.getStart() );
        curationSet.setHigh( cfl.getEnd() );

      } else {
        throw new apollo.dataadapter.ApolloAdapterException("Unkown mode.");
      }//end if
      genomeSeq = getSequence(new DbXref(region,region,region));
      curationSet.setRefSequence(genomeSeq);
      // Don't set the organism until the reference sequence is set
      // because it really is in the seq model that the org is held
      curationSet.setOrganism ( getOrganism() );

    } catch (org.ensembl19.driver.AdaptorException e) {
      throw new apollo.dataadapter.ApolloAdapterException(
        "Load failed. Are you sure " + region+ "is a real sequence?",
        e
      );
    } catch (ConfigurationException e) {
      throw new apollo.dataadapter.ApolloAdapterException(
        "Load failed. Are you sure " + region+ "is a real sequence?",
        e
      );
    }

    System.out.println ("Now loading ANALYSES");
    curationSet.setResults((StrandedFeatureSetI)getAnalysisRegion(curationSet));
    System.out.println ("Now loading ANNOTATIONS");
    curationSet.setAnnots(getAnnotatedRegion(curationSet));
    curationSet.setName(region);

    /*Put the name of the assembly onto the curation set: needed by otter,
      if we write out annotations */
    String assemblyType = null;
    try{
      assemblyType = getDriver().resolveMapName(getLocation().getMap());
    }catch(org.ensembl19.driver.ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage(), exception);
    }catch(org.ensembl19.driver.AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage(), exception);
    }
    curationSet.setAssemblyType(assemblyType);
    System.out.println ("finished with chromosome " +
                        curationSet.getChromosome());
    return curationSet;
  }


  public CurationSet getCurationSet() throws apollo.dataadapter.ApolloAdapterException {
    super.clearOldData();

    CurationSet curationSet = getCurationSetWithoutClearingData();

    //super.notifyLoadingDone();
    
    return curationSet;
  }
  
  /**
     An ugly hack to tease the organism name from the database name
     Ignore it if this doesn't parse out */
  protected String getOrganism () {
    String database 
      = getStateInformation().getProperty(StateInformation.DATABASE);
    String organism = null;
    try {
      int index = database.indexOf('_');
      index = database.indexOf('_', index + 1);
      if (index < database.length()) {
        String first = database.substring(0,1);
        String last = database.substring(1, index);
        organism = first.toUpperCase() + last;
        System.out.println ("Organism is " + organism);
      }
    } catch (Exception e) {
      System.out.println ("Could not parse organism from " + database);
    }
    return organism;
  }


  /**
   * @return ensj location corresponding to region, or null if no match exists.
   */
  protected LinearLocation getLocation() 
  throws 
    org.ensembl19.driver.AdaptorException, 
    org.ensembl19.driver.ConfigurationException
  {

    if ( getMode()==ASSEMBLY_LOCATION_MODE ) {

      //GenomicRange loc = SequenceUtil.parseChrStartEndString(region);
      Region loc = new Region(region);
      logger.debug("loc"+loc);
      if (loc != null) {

        AssemblyLocation al = new AssemblyLocation();
        if ( al==null ) return null;

        al.setChromosome(loc.getChromosome());
        al.setStart(loc.getStart());
        al.setEnd(loc.getEnd());
        al.setStrand(0);
        location = al;

      }
    } else if ( getMode()==CLONE_FRAGMENT_MODE ) {

      if ( cloneFragmentAdaptor==null )
        initialiseDriver();
      // Region is a clone fragment 'accession' string
      CloneFragment cf = cloneFragmentAdaptor.fetch(region);
      CloneFragmentLocation cfl = new CloneFragmentLocation();

      if(cf == null) return null;

      cfl.setCloneFragmentInternalID(cf.getInternalID());
      cfl.setStart( 1 );
      cfl.setEnd( cf.getLength() );
      cfl.setStrand( 0 );
      location = cfl;

    }
    else if ( getMode()==STABLE_ID_MODE ) {

      // fetch thing by stable id, then get it's location, then reset the
      // region and mode!.
      String stableID = region.substring( STABLE_ID_PREFIX.length(), region.length());
      logger.info("Fetching stable id:" + stableID);

      AssemblyLocation al = stableID2Location( stableID );
      if ( al==null ) return null;
      al.setChromosome( al.getChromosome());
      al.setStart( al.getStart());
      al.setEnd( al.getEnd());
      location = al;

      // Pretend we were called with a chromosome region!
      region = "Chr " + al.getChromosome() + " " + al.getStart() + " " + al.getEnd();
      setMode(ASSEMBLY_LOCATION_MODE);

    }

    return (LinearLocation)location;
  }


  /**
   * Converts a stable id into the assembly location it relates to. Works
   * with stable ids for genes, transripts, translations and exons. Works by
   * querying each table in sequence - we stop querying the first time we actually
   * hit a row with the input stable id.
   * @return location corresponding to annotation with stable, or throw adapter exception if
   * region was not found.
   */
  private AssemblyLocation stableID2Location( String stableID ) throws
  AdaptorException, ConfigurationException{

    AssemblyLocation loc = null;

    //
    //Uncondititional driver initialisation - the database properties
    //might have changed when this method is called (as a follow-on from
    //a setRegion call).
    initialiseDriver();

    org.ensembl19.datamodel.Gene gene
      = ((GeneAdaptor)driver.getAdaptor("gene")).fetch( stableID );

    if ( gene!=null ) {
      loc = (AssemblyLocation)gene.getLocation();
      logger.info("stable id = "+stableID+" loc = " + loc);
      return loc;
    }

    org.ensembl19.datamodel.Transcript transcript
      = ((TranscriptAdaptor)driver.getAdaptor("transcript")).fetch( stableID );

    if ( transcript!=null ) {
      loc = (AssemblyLocation)transcript.getGene().getLocation();
      logger.info("stable id = "+stableID+" loc = " + loc);
      return loc;
    }

    org.ensembl19.datamodel.Translation translation
      = ((TranslationAdaptor)driver.getAdaptor("translation")).fetch( stableID );

    if ( translation!=null ) {
      loc = (AssemblyLocation)translation.getTranscript().getGene().getLocation();
      logger.info("stable id = "+stableID+" loc = " + loc);
      return loc;
    }

    org.ensembl19.datamodel.Exon exon
      = ((ExonAdaptor)driver.getAdaptor("exon")).fetch( stableID );

    if ( exon!=null ) {
      loc = (AssemblyLocation)exon.getGene().getLocation();
      logger.info("stable id = "+stableID+" loc = " + loc);
      return loc;
    }

    return null;
  }




  /**
   * Caches last set of genes retrieved.
   */
  private List getGenes() throws apollo.dataadapter.ApolloAdapterException {

    SimpleTimer timer = new SimpleTimer().start();

    List genes = null;

    if ( cachedRegion != null && cachedRegion.equals(region) ) {

      genes = cachedGenes;

    } else {
      fireProgressEvent(new ProgressEvent(this,new Double(10.0),
                                          "Getting genes..."));

      try {

        if ( geneAdaptor==null )
          initialiseDriver();
        genes = geneAdaptor.fetch(createQuery());
        logger.debug("nGenes=" + genes.size());

        // Ensure 'type' is not null
        for(int i=0; i<genes.size(); ++i) {
          org.ensembl19.datamodel.Gene gene 
            = (org.ensembl19.datamodel.Gene)genes.get(i);
          if ( gene.getType()==null ) gene.setType("UNKOWN");
          if ( gene.getAccessionID()==null ) gene.setAccessionID("UNKOWN");
        }

        cachedGenes = genes;
        cachedRegion = region;

        cacheTranslations( genes );

      } catch (ConfigurationException e) {
        throw new apollo.dataadapter.ApolloAdapterException(
          "Failed to retrieve data from ensj driver:"
          + e.getMessage()
        );
      } catch (AdaptorException e) {
        throw new apollo.dataadapter.ApolloAdapterException(
          "Failed to retrieve data from ensj driver:"
          + e.getMessage()
        );
      }
    }
    logger.info( loadMessage(" genes", genes,
                             (geneLoadTime=timer.stop().getDuration()) ) );

    return genes;
  }



  /**
   * We need to use translations to figure out the genomic locations
   * corresponding to SimplePeptideFeatures. In principle we should be able
   * to do feature.getTranlation().getCodingLocations() but in practice the
   * MySQLSimplePeptideFeatureAdaptor does not set the location on the
   * feature (too slow until caching introduced into the translation
   * adaptor). The work around for this is to cache the translations in this
   * plugin.
   *
   */
  private void cacheTranslations( List genes ) {

    translationCache.clear();

    for(int g=0; g<genes.size(); ++g) {

      org.ensembl19.datamodel.Gene gene = (org.ensembl19.datamodel.Gene)( genes.get(g) );
      List transcripts = gene.getTranscripts();

      for (int t=0; t<transcripts.size(); ++t) {

        org.ensembl19.datamodel.Transcript transcript
          = (org.ensembl19.datamodel.Transcript)transcripts.get(t);
        org.ensembl19.datamodel.Translation translation
          = transcript.getTranslation();
        if (translation!=null) {
          translationCache.put( new Long(translation.getInternalID()),
                                translation );
        }
      }
    }

  }

  /**
    * @return object graph containing genes, transcripts and exons from the currently
    * specified region as apollo annotation objects. 
    * Reads otter source (server or file) for annotations. Converts those
    * annotations into Apollo objects. ONLY if the adapter has been specified
    * an input server/file name. THAT will only happen if EditingEnabled has
    * been marked as "true" on the style file for the adapter.
  **/

  protected StrandedFeatureSetI getAnnotatedRegion(CurationSet curationSet)
    throws apollo.dataadapter.ApolloAdapterException {
    
    StrandedFeatureSetI root = new StrandedFeatureSet( new AnnotatedFeature(),
                               new AnnotatedFeature() );
    if ( getStateAsBoolean(StateInformation.INCLUDE_GENE) ) {
      List genes = getGenes();
      System.out.println ("got " + genes.size() + " genes ");
      Iterator geneIter = genes.iterator();
      while ( geneIter.hasNext() ) {
        
        org.ensembl19.datamodel.Gene gene
          = (org.ensembl19.datamodel.Gene)geneIter.next();

        AnnotatedFeatureI apolloGene = createGene(gene);
        root.addFeature( apolloGene );
      }
    }
    return root;
  }



  /**
   * Adds protein similarity features corresponding to the current region to
   * root.  */
  private void addDnaProteinAlignments( Vector v )
  throws org.ensembl19.driver.AdaptorException, org.ensembl19.driver.ConfigurationException {

    if ( dnaProteinAlignmentAdaptor==null ) {
      logger.warn("Unable to retrieve DNA protein alignments, adaptor unvailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

    fireProgressEvent(new ProgressEvent(this,new Double(30.0),
          "Getting protein features..."));
    List features = dnaProteinAlignmentAdaptor.fetch(getLocation());
    final int nFeatures = features.size();

    for (int i=0; i<nFeatures; ++i) {

      DnaProteinAlignment psf = (DnaProteinAlignment)features.get(i);

      SeqFeatureI proteinHit = new SeqFeature();
      proteinHit.setLow( psf.getHitLocation().getStart());
      proteinHit.setHigh(psf.getHitLocation().getEnd());
      proteinHit.setName(psf.getHitAccesion());
      proteinHit.setId(psf.getHitAccesion());
      proteinHit.setScore(psf.getScore());

      SeqFeatureI genomeHit = new SeqFeature();
      LinearLocation loc = (LinearLocation)psf.getLocation();
      genomeHit.setLow( loc.getStart() );
      genomeHit.setHigh( loc.getEnd() );
      genomeHit.setStrand( loc.getStrand() );
      genomeHit.setName(psf.getDisplayName());
      genomeHit.setId(psf.getDisplayName());
      genomeHit.setFeatureType(psf.getAnalysis().getLogicalName());
      genomeHit.setScore(psf.getScore());


      FeaturePair fp = new FeaturePair( genomeHit, proteinHit );
      v.add( fp );
    }

    logger.info( loadMessage(" dna protein alignments", v, (dnaProteinLoadTime = timer.stop().getDuration()) ) );

  }

  private void addDnaDnaAlignments( Vector v )
  throws org.ensembl19.driver.AdaptorException, org.ensembl19.driver.ConfigurationException {

    if ( dnaDnaAlignmentAdaptor==null ) {
      logger.warn("Unable to retrieve DNA protein alignments, adaptor unvailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

    fireProgressEvent(new ProgressEvent(this,new Double(40.0),
          "Getting dna features..."));
    List features = dnaDnaAlignmentAdaptor.fetch(getLocation());
    final int nFeatures = features.size();


    for (int i=0; i<nFeatures; ++i) {

      DnaDnaAlignment psf = (DnaDnaAlignment)features.get(i);

      SeqFeatureI proteinHit = new SeqFeature();
      proteinHit.setLow( psf.getHitLocation().getStart() );
      proteinHit.setHigh( psf.getHitLocation().getEnd() );
      proteinHit.setName( psf.getHitAccesion() );
      proteinHit.setId( psf.getHitAccesion() );
      proteinHit.setScore( psf.getScore() );

      SeqFeatureI genomeHit = new SeqFeature();
      LinearLocation loc = (LinearLocation)psf.getLocation();
      genomeHit.setLow( loc.getStart() );
      genomeHit.setHigh( loc.getEnd() );
      genomeHit.setStrand( loc.getStrand() );
      genomeHit.setName( psf.getHitAccesion() );
      genomeHit.setId( psf.getHitAccesion() );
      genomeHit.setFeatureType( psf.getAnalysis().getLogicalName() );
      genomeHit.setScore( psf.getScore() );


      FeaturePair fp = new FeaturePair( genomeHit, proteinHit );
      v.add( fp );

    }

    logger.info( loadMessage("dna dna alignments", v,
                             (dnaDnaLoadTime = timer.stop().getDuration()) ) );
  }

    private void addSimplePeptides( Vector v )
      throws org.ensembl19.driver.AdaptorException,
             org.ensembl19.driver.ConfigurationException,
             apollo.dataadapter.ApolloAdapterException{

      if ( simplePeptideAdaptor==null ) {
        logger.warn("Unable to retrieve Simple Peptides, adaptor unvailable");
        return;
      }

      // A bit hacky but we need to force loading of genes because we need to
      // cache translations before we can create
      // simplepeptidefeatures. extractPeptideLocation(...) uses these cached
      // values.
      getGenes();

      SimpleTimer timer = new SimpleTimer().start();

      //List features = simplePeptideAdaptor.fetch(getLocation());
      List features = new ArrayList();
      Iterator iter = translationCache.values().iterator();

      while ( iter.hasNext() ) {
        org.ensembl19.datamodel.Translation t = (org.ensembl19.datamodel.Translation) iter.next();
        features.addAll( simplePeptideAdaptor.fetch( t ) );
      }

      if ( features.size()==0 ) return;

      logger.info("Loaded simple peptides = " + features.size());

      // recyled objects to save time
      //      List subLocations = new ArrayList();

      for (int i=0; i<features.size(); ++i) {

        SimplePeptideFeature feature = (SimplePeptideFeature)features.get(i);
        int start = feature.getPeptideStart();
        int end = feature.getPeptideEnd();
        String displayName = feature.getDisplayName();
        double score = feature.getScore();

        // set feature.translation so feature can use translation to
        // calculate it's location.
        Long translationKey = new Long( feature.getTranslationInternalID() );
        org.ensembl19.datamodel.Translation translation
          = (org.ensembl19.datamodel.Translation)translationCache.get( translationKey );
        if ( translation==null ) {
          logger.warn("Skipping SimplePeptideFeature ( "
                      + feature.getInternalID()
                      +" ) because related translation ("
                      + translationKey
                      +") not cached. "
                      );
          continue;
        }
        feature.setTranslation( translation);

        // SimplePeptideFeatures can correspond to several genomic locations because
        // they relate to parts of one or more exons. We represent each of
        // these 'sections' as a separate FeaturePair.

        Location loc = feature.getLocation();

        for(; loc!=null; loc = loc.next() ) {

          // TODO create once per SPF or do we need one per pair?
          SeqFeatureI peptideHit = new SeqFeature();
          peptideHit.setLow( start );
          peptideHit.setHigh( end );
          peptideHit.setName( displayName );

          SeqFeatureI genomeHit = new SeqFeature();
          genomeHit.setLow( loc.getStart() );
          genomeHit.setHigh( loc.getEnd() );
          genomeHit.setStrand( loc.getStrand() );
          genomeHit.setName( feature.getDisplayName() );
          genomeHit.setId( feature.getDisplayName() );
          genomeHit.setFeatureType( feature.getAnalysis().getLogicalName() );
          genomeHit.setScore( score );
          FeaturePair fp = new FeaturePair( genomeHit, peptideHit );

          v.add( fp );
        }
      }

      logger.info( loadMessage("simple peptides", v,
                               ( simplePeptideLoadTime = timer.stop().getDuration())) );

    }




//   /**
//    * Retrieves 'relevant' parts of the locations available from the
//    * translation.
//    * @return Location if one can be constructed, otherwise null.
//    */
//   private Location extractPeptideLocation(SimplePeptideFeature feature,
//                                           int aminoAcidStart,
//                                           int aminoAcidEnd) {

//     Long translationKey = new Long( feature.getTranslationInternalID() );
//     org.ensembl19.datamodel.Translation translation
//       = (org.ensembl19.datamodel.Translation)translationCache.get( translationKey );
//     if ( translation==null ) {
//       logger.warn("Skipping SimplePeptideFeature ( "
//                   + feature.getInternalID()
//                   +" ) because related translation ("
//                   + translationKey
//                   +") not cached. "
//                   );
//       return null;
//     }





//   }

  private void addFeatures( Vector v )
    throws org.ensembl19.driver.AdaptorException, org.ensembl19.driver.ConfigurationException {

    if ( simpleFeatureAdaptor ==null ) {
      logger.warn("Unable to retrieve features (simple), adaptor unvailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

    List features = simpleFeatureAdaptor.fetch(getLocation());

    final int nFeature = features.size();

    for (int i=0; i<nFeature; ++i) {

      Feature f = (Feature)features.get(i);

      int strand = 1;

      SeqFeatureI genomeHit = new SeqFeature();
      LinearLocation loc = (LinearLocation)f.getLocation();

      if (loc.getStrand() == -1) {
         strand = 1;
      }

      genomeHit.setLow( loc.getStart() );
      genomeHit.setHigh( loc.getEnd() );
      genomeHit.setStrand(strand);
      genomeHit.setName( f.getDisplayName() );
      genomeHit.setId( f.getDisplayName() );
      if (f.getAnalysis() != null) {
      genomeHit.setFeatureType( f.getAnalysis().getLogicalName() );
      } else {
        System.out.println("No analysis for feature");
      }
      v.add( genomeHit );
    }

    logger.info( loadMessage("features (simple)", v,
                             ( featureLoadTime= timer.stop().getDuration() ) ) );

  }

  private void addRepeatFeatures( Vector v )
    throws org.ensembl19.driver.AdaptorException, org.ensembl19.driver.ConfigurationException {

    if ( repeatFeatureAdaptor==null ) {
      logger.warn("Unable to retrieve repeats, adaptor unvailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

     fireProgressEvent(new ProgressEvent(this,new Double(50.0),
                                          "Getting repeats..."));
    List repeats = repeatFeatureAdaptor.fetch(getLocation());
    final int nRepeats = repeats.size();

    for (int i=0; i<nRepeats; ++i) {

      RepeatFeature rf = (RepeatFeature)repeats.get(i);

      SeqFeatureI repeatHit = new SeqFeature();
      LinearLocation hitLoc = (LinearLocation)rf.getHitLocation();
      repeatHit.setLow( hitLoc.getStart() );
      repeatHit.setHigh( hitLoc.getEnd() );
      repeatHit.setName( rf.getHitDisplayName() );

      SeqFeatureI genomeHit = new SeqFeature();
      LinearLocation loc = (LinearLocation)rf.getLocation();
      genomeHit.setLow( loc.getStart() );
      genomeHit.setHigh( loc.getEnd() );
      genomeHit.setStrand( loc.getStrand() );

      RepeatConsensus rc = rf.getRepeatConsensus();

      if (rc != null) {
    genomeHit.setName(rc.getName());
    genomeHit.setId(rc.getName());

         repeatHit.setName(rc.getName());
         repeatHit.setId(rc.getName());
      } else {
    genomeHit.setName(rf.getDisplayName());
    genomeHit.setId(rf.getDisplayName());

        repeatHit.setName( rf.getHitDisplayName() );
        repeatHit.setId( rf.getHitDisplayName() );
      }

      genomeHit.setFeatureType( rf.getAnalysis().getLogicalName() );

      repeatHit.setScore(rf.getScore());
      genomeHit.setScore(rf.getScore());

      FeaturePair fp = new FeaturePair( genomeHit, repeatHit );
      v.add( fp );
    }


    logger.info( loadMessage("repeat", v,
                             ( repeatLoadTime = timer.stop().getDuration()) ) );

  }


  private void addPredictionTranscripts(StrandedFeatureSet root)
    throws org.ensembl19.driver.AdaptorException,
           org.ensembl19.driver.ConfigurationException,
           apollo.dataadapter.ApolloAdapterException {

    if ( predictionTranscriptAdaptor==null ) {
      logger.warn("Unable to retrieve prediction transcripts, adaptor unavailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

    fireProgressEvent(new ProgressEvent(this,new Double(80.0),
          "Getting prediction transcripts..."));
    List predictions = predictionTranscriptAdaptor.fetch(getLocation());

    final int nPredictions = predictions.size();
    Hashtable transTypeHash = new Hashtable();

    for (int i=0; i<nPredictions; ++i) {
      
      PredictionTranscript pt = (PredictionTranscript)predictions.get(i);
      
      StrandedFeatureSetI subRoot;
      String predType = pt.getAnalysis().getLogicalName();

      if (!transTypeHash.containsKey(predType)) {
        subRoot = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
        subRoot.getForwardSet().setFeatureType(predType);
        subRoot.getReverseSet().setFeatureType(predType);
        transTypeHash.put(predType,(Object)subRoot);
      } else {
        subRoot = (StrandedFeatureSetI)(transTypeHash.get(predType));
      }

      FeatureSetI transFeature = new FeatureSet();
      transFeature.setFeatureType(predType);
      transFeature.setName(predType + ":" + pt.getInternalID());
      transFeature.setId(""+ pt.getInternalID());

      Iterator pes = pt.getExons().iterator();

      while (pes.hasNext()) {

        PredictionExon pe = (PredictionExon)pes.next();
        SeqFeatureI predExon = new SeqFeature();
        LinearLocation loc = (LinearLocation)pe.getLocation();
        predExon.setLow( loc.getStart() );
        predExon.setHigh( loc.getEnd() );
        predExon.setStrand( loc.getStrand() );
        predExon.setName( transFeature.getName() );
        predExon.setId( transFeature.getName() );
        predExon.setFeatureType( predType );

        if (pe.getPvalue() > 0) {
           predExon.setScore(pe.getPvalue());
        } else {
           predExon.setScore(pe.getScore());
        }

        transFeature.setStrand(loc.getStrand());
        transFeature.addFeature( predExon );
      }
      transFeature.setTranslationStart(transFeature.getStart());
      transFeature.setTranslationEnd(transFeature.getEnd());
      transFeature.sort(transFeature.getStrand());

      subRoot.addFeature(transFeature);
    }

    Enumeration e = transTypeHash.keys();

    while (e.hasMoreElements()) {

      StrandedFeatureSetI sfs = (StrandedFeatureSetI)transTypeHash.get(e.nextElement());
      root.addFeature((FeatureSetI)sfs.getForwardSet());
      root.addFeature((FeatureSetI)sfs.getReverseSet());

    }

    logger.info( loadMessage("prediction transcripts (conversion to Apollo object model)",
                             Collections.EMPTY_LIST,
                             timer.stop().getDuration()) );

  }

  private void addVariations( Vector v )
    throws org.ensembl19.driver.AdaptorException, org.ensembl19.driver.ConfigurationException {

    if ( variationAdaptor==null ) {
      logger.warn("Unable to retrieve variations (simple), adaptor unvailable");
      return;
    }

    SimpleTimer timer = new SimpleTimer().start();

    fireProgressEvent(new ProgressEvent(this,new Double(60.0),
          "Getting SNPs..."));
    List variations = variationAdaptor.fetch(getLocation());
    final int nVariation = variations.size();

    for (int i=0; i<nVariation; ++i) {

      org.ensembl19.datamodel.Variation variation = (org.ensembl19.datamodel.Variation)variations.get(i);

      // skip variations which are filtered out on the ensembl
      // website. Heikki's request.
      // if ( variation.getMapWeights()>2 ) continue;

      SeqFeatureI seqFeature = new SeqFeature();
      LinearLocation loc = (LinearLocation)variation.getLocation();
      seqFeature.setLow( loc.getStart() );
      seqFeature.setHigh( loc.getEnd() );
      seqFeature.setStrand( loc.getStrand() );
      final String nameID = Long.toString( variation.getInternalID() );
      seqFeature.setName( nameID );
      seqFeature.setId( nameID );
      seqFeature.setFeatureType( "Variation" );

      v.add( seqFeature );
    }
    logger.info( loadMessage("variations", v,
                             ( variationLoadTime= timer.stop().getDuration() ) ) );


  }


  /**
   * Chooses a display name for the marker from amongst it's synonyms.
   */
  /*
  private String chooseMarkerName( org.ensembl19.datamodel.Marker marker ) {

    String name = null;
    String backup = null;

    List synonyms = marker.getSynonyms();
    for( int i=0; i<synonyms.size() && name==null; ++i) {
      String synonym = (String)synonyms.get(i);
      if ( synonym==null || synonym.length()<2 ) continue; // just in case
                                                           // ...
      if ( backup==null ) backup = synonym;

      // look for synonym beginning D[\d] or AFM (case insensitive).
      final char first = synonym.charAt(0);
      final char second = synonym.charAt(1);
      final char third = synonym.charAt(2);
      final boolean dDigit
        = (first=='D' || first=='d') && Character.isDigit(second);
      final boolean afm
        = (first=='A' || first=='a')
        && (second=='F' || second=='f')
        && (third=='M' || third=='m');

      if ( dDigit || afm )
        name = synonym;
    }

    // choose first synonym if no prefered one found
    if ( name==null ) {
      if ( backup!=null ) name = backup;
      else name = Long.toString( marker.getInternalID() );
    }

    return name;
    }
   */

  /**
   * Convert flat vector of features into a tree where all "related" items
   * are grouped by some magic rules defined in ensj.tiers.
   */
  private void buildFeatureSet( StrandedFeatureSet root, Vector flatFeatures ) {
    FeatureSetBuilder fsb = new FeatureSetBuilder();
    fsb.makeSetFeatures (root, flatFeatures, Config.getPropertyScheme());
  }

  /**
   * @return object graph containing transcripts and exons from the currently
   * specified region.  */
  private FeatureSetI getAnalysisRegion(CurationSet curationSet)
    throws apollo.dataadapter.ApolloAdapterException {

    StrandedFeatureSet root = new StrandedFeatureSet( new FeatureSet(),
                                                      new FeatureSet() );

    try {
      Vector flat = new Vector();

      Style style = Config.getStyle();
      if ( getStateAsBoolean(StateInformation.INCLUDE_GENE) &&
           style.getShowAnnotations() == false) {
        getGenes();
      }

      addContigs(root);

      int loc_size = (Math.abs(getLocation().getEnd() - 
                               getLocation().getStart() + 1));

      // Not a particularly nice way of limiting the feature set get size
      boolean skimpy = loc_size > style.getFeatureLoadSize();
      if (!skimpy) {
        if (getStateAsBoolean(StateInformation.INCLUDE_DNA_PROTEIN_ALIGNMENT) )
          addDnaProteinAlignments(flat);
        if (getStateAsBoolean(StateInformation.INCLUDE_DNA_DNA_ALIGNMENT) )
        addDnaDnaAlignments(flat);

        /* At time of writing, simple peptide adapter didn't support a 
           fetch by clone */
        if (getStateAsBoolean(StateInformation.INCLUDE_SIMPLE_PEPTIDE_FEATURE)){
          if (getMode()==CLONE_FRAGMENT_MODE){
            throw new apollo.dataadapter.ApolloAdapterException("Simple Peptide Features are not supported for Clone Fragment-fetches");
          } else {
            addSimplePeptides(flat);
          }
        }//end if
        
        if ( getStateAsBoolean(StateInformation.INCLUDE_FEATURE) )
          addFeatures(flat);
        if ( getStateAsBoolean(StateInformation.INCLUDE_REPEAT_FEATURE) )
          addRepeatFeatures(flat);
        if ( getStateAsBoolean(StateInformation.INCLUDE_VARIATION) )
          addVariations( flat );
      }
      SimpleTimer timer = new SimpleTimer().start();
      if (Config.getPFetchServer() != null) {
        for (int j=0; j< flat.size(); j++) {
          SeqFeatureI sf = (SeqFeatureI)flat.elementAt(j);
          setSequence(sf,curationSet);
        }
      }
      buildFeatureSet(root,flat);
      logger.info("Time it took to build feature set = "
                  + timer.stop().getDuration() + "ms");
      
      if ( getStateAsBoolean(StateInformation.INCLUDE_GENE) &&
           style.getShowAnnotations() == false)
        addTranscripts(root);
      
      if (!skimpy) {
        if (getStateAsBoolean(StateInformation.INCLUDE_PREDICTION_TRANSCRIPT))
          addPredictionTranscripts(root);
      }

      fireProgressEvent(new ProgressEvent(this,new Double(100.0),
        "Done"));

    } catch (AdaptorException e) {
      throw new apollo.dataadapter.ApolloAdapterException("problem loading data: "+e.getMessage(), e);
    } catch (ConfigurationException e) {
      throw new apollo.dataadapter.ApolloAdapterException("problem loading data: "+e.getMessage(), e);
    }

    return root;

  }

  private void addContigs( StrandedFeatureSet root ) throws apollo.dataadapter.ApolloAdapterException{
    AssemblyElement [] elements;
    AssemblyElement element;
    CloneFragment cloneFragment;
    String cloneAccession;
    String contigName;
    SeqFeatureI genomicFeature;
    SeqFeatureI contigFeature;
    FeatureSet subset;

    try {
      if(getMode() == ASSEMBLY_LOCATION_MODE){
        
        elements = 
          ((org.ensembl19.driver.plugin.standard.PartialAssembly)
            ((org.ensembl19.driver.plugin.standard.MySQLLocationConverter)
              getLocationConverter()
            ).getPartialAssembly((AssemblyLocation)location)
          ).getElements();
        
      } else {
        return;
      }

      fireProgressEvent(new ProgressEvent(this,new Double(20.0),"Getting contigs..."));
      
      for(int i=0; i<elements.length; i++) {
        element = elements[i];
        cloneFragment = cloneFragmentAdaptor.fetch( element.getCloneFragmentInternalID() );
        cloneAccession = cloneFragment.getClone().getAccessionID();
        contigName     = cloneFragment.getName();

        genomicFeature = new SeqFeature();
        genomicFeature.setLow( element.getChromosomeStart() );
        genomicFeature.setHigh( element.getChromosomeEnd() );
        genomicFeature.setStrand( element.getCloneFragmentOri() );
        genomicFeature.setName( contigName );
        genomicFeature.setId( contigName );
        genomicFeature.setFeatureType("sequence");

        contigFeature = new SeqFeature();
        contigFeature.setLow( element.getCloneFragmentStart() );
        contigFeature.setHigh( element.getCloneFragmentEnd() );
        contigFeature.setStrand( element.getCloneFragmentOri() );
        contigFeature.setName( contigName);
        contigFeature.setId( contigName );
        contigFeature.setFeatureType("sequence");

        subset = new FeatureSet();
        subset.setStrand( element.getCloneFragmentOri() );
        subset.setFeatureType("sequence");
        subset.addFeature(new FeaturePair(genomicFeature,contigFeature));
        genomicFeature.setRefFeature(subset);
        contigFeature.setRefFeature(subset);
        
        if(element.getCloneFragmentOri() == 1){
          root.getForwardSet().addFeature(subset);
        }else if(element.getCloneFragmentOri() == -1){
          root.getReverseSet().addFeature(subset);
        }else{
          throw new apollo.dataadapter.ApolloAdapterException("Found a contig - id: "+element.getCloneFragmentInternalID()+"without strand");
        }
      }

    } catch (AdaptorException exception) {
      throw new apollo.dataadapter.ApolloAdapterException("Problem adding contigs: "+exception.getMessage(), exception);
    }//end try
  }





  private void setSequence(SeqFeatureI sf, CurationSet curationSet) {
    if (sf instanceof FeaturePair) {
      FeaturePair pair = (FeaturePair)sf;
      String      name = pair.getHname();
      SequenceI   seq  = curationSet.getSequence(name);
      if (seq == null) {
        seq = new PFetchSequence(name,Config.getController());
        //if (seq.getLength() == 0)
        //  seq = new Sequence(name,"");
        curationSet.addSequence(seq);

      }
      pair.getHitFeature().setRefSequence(seq);
    }
  }



  public static void main(String [] args) throws Exception {
    //Config.readConfig("/mnt/Users/jrichter/cvs/apollo/data/apollo.cfg");
    DataAdapterRegistry
    registry
    = new DataAdapterRegistry
      ();
    registry.installDataAdapter("apollo.dataadapter.ensj.EnsJAdapter");
    registry.installDataAdapter("apollo.dataadapter.SerialDiskAdapter");
    registry.installDataAdapter("apollo.dataadapter.gamexml.GAMEAdapter");
    DataAdapterChooser chooser = new DataAdapterChooser(
                                   registry
                                   ,
                                   ApolloDataAdapterI.OP_READ_DATA,
                                   "Load data",
                                   null,
                                   false);
    chooser.setPropertiesFile(new File("/home/craig/dev/apollo/data/apollo.history"));
    chooser.show();

  }


  //    public SequenceI getSequence(apollo.datamodel.DbXref dbXRef ,int start,int
  //                                 end)  throws apollo.dataadapter.DataAdapterException {{
  //      throw new NotImplementedException();
  //    }


  //    public SequenceI getSequence(apollo.datamodel.DbXref dbXRef ,int[] a,int[]
  //                                 b)  throws apollo.dataadapter.DataAdapterException {{
  //      throw new NotImplementedException();
  //    }


  public SequenceI getSequence(String id) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector    getSequences(DbXref[] dbxref)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public Vector    getSequences(DbXref[] dbxref, int[] start, int[] end)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public void commitChanges(CurationSet curationSet)
  throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }
  public String getRawAnalysisResults(String id) throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }


  /**
   * Get the value of loggingFile.
   * @return value of loggingFile.
   */
  public String getLoggingFile() {
    return loggingFile;
  }

  /**
   * Set the value of loggingFile.
   * @param v  Value to assign to loggingFile.
   */
  public void setLoggingFile(String  v) {
    this.loggingFile = v;
  }


  Properties driverConf;

  /**
   * Get the value of driverConf.
   * @return value of driverConf.
   */
  public Properties getDriverConf() {
    return driverConf;
  }

  protected Driver getDriver(){
    return driver;
  }

  protected int getMode(){
    return mode;
  }

  /**
   * Set the value of driverConf.
   * @param v  Value to assign to driverConf.
   */
  public void setDriverConf(Properties  v) {
    this.driverConf = v;
  }


    private void initialiseDriver() throws ConfigurationException {

    try {

      LoggingManager.configure(loggingFile);
      driver = DriverManager.load(driverConf);

      geneAdaptor = (GeneAdaptor)driver.getAdaptor("gene");
      cloneFragmentAdaptor = (CloneFragmentAdaptor)driver.getAdaptor("clone_fragment");

      dnaProteinAlignmentAdaptor
      = (DnaProteinAlignmentAdaptor)driver.getAdaptor("dna_protein_alignment");

      dnaDnaAlignmentAdaptor
      = (DnaDnaAlignmentAdaptor)driver.getAdaptor("dna_dna_alignment");

      simpleFeatureAdaptor = (SimpleFeatureAdaptor)driver.getAdaptor("simple_feature");

// uncomment when supporting features come into ensembl_9_30
//      supportingFeatureAdaptor = (SupportingFeatureAdaptor)driver.getAdaptor("supporting_feature");

      simplePeptideAdaptor = (SimplePeptideAdaptor)driver.getAdaptor("simple_peptide_feature");

      repeatFeatureAdaptor = (RepeatFeatureAdaptor)driver.getAdaptor("RepeatMask");

      predictionTranscriptAdaptor = (PredictionTranscriptAdaptor)driver.getAdaptor("prediction_transcript");

      variationAdaptor = (VariationAdaptor)driver.getAdaptor("variation");

      setLocationConverter((LocationConverter)driver.getAdaptor("location_converter"));


      // Hacky way to force assemblyElements cache to load in
      // MySQLLocationConverter
      SimpleTimer timer = new SimpleTimer().start();
      simpleFeatureAdaptor.fetch(AssemblyLocation.valueOf("1:1-1"));
      logger.info( loadMessage("assemblyElements cache",
                               Collections.EMPTY_LIST,
                               timer.stop().getDuration()) );

      initialisedDriver = true;

    } catch (java.text.ParseException exception) {
      throw new ConfigurationException( exception.getMessage() );
    } catch (AdaptorException exception) {
      throw new ConfigurationException( exception.getMessage() );
    }
  }



  private AnnotatedFeatureI createGene(org.ensembl19.datamodel.Gene ensjGene) {

    AnnotatedFeatureI gene = new AnnotatedFeature();
    String source = ensjGene.getType();

    String id = Long.toString(ensjGene.getInternalID());
    gene.setId( id );
    gene.setName( getName(ensjGene, id) );
    gene.setDescription( ensjGene.getDescription() );
    Location loc = (Location)ensjGene.getLocation();
    gene.setStrand( loc.getStrand() );
    //gene.setRefSequence(genomeSeq);

    Iterator transcripts = ensjGene.getTranscripts().iterator();
    while ( transcripts.hasNext() ) {
      Transcript transcript = 
        createTranscript((org.ensembl19.datamodel.Transcript)transcripts.next());
      gene.addFeature(transcript);
      transcript.setOwner( source );
    }
    // TODO setGeneXref();
    List synonyms = gene.getSynonyms();
    for( int i=0; i<synonyms.size(); ++i) {
      String synonym = (String)synonyms.get(i);
      gene.addSynonym(synonym);
    }
    // TODO gene.addSynonym();

    return gene;
  }

  private Transcript createTranscript(org.ensembl19.datamodel.Transcript
                                      ensjTranscript) {

    Transcript transcript = new Transcript();

    String id = Long.toString(ensjTranscript.getInternalID());
    transcript.setId( id );
    transcript.setName( getName(ensjTranscript, id) );
    //transcript.setRefSequence(genomeSeq);
    Location loc = ensjTranscript.getLocation();
    transcript.setStrand( loc.getStrand() );

    Iterator exons = ensjTranscript.getExons().iterator();
    while ( exons.hasNext() ) {
      Exon exon = createExon(( org.ensembl19.datamodel.Exon)exons.next());
      transcript.addExon(exon);
      logger.debug("exon low = " + exon.getLow() +
                   " exon high = " + exon.getHigh());
    }

    setTranslationStartEndFromEnsjTranscript(transcript,ensjTranscript);

    // TODO transcript.addSynonym();

    return transcript;
  }

  // Note: modifies set
  private void setTranslationStartEndFromEnsjTranscript(FeatureSetI set,
                                                        org.ensembl19.datamodel.Transcript ensjTranscript) {
    // Extract relevant sub locations and crop if necessary.
    if (ensjTranscript.getTranslation() != null) {
      List locs = null;
      try {
        locs = ensjTranscript.getTranslation().getCodingLocations();
      } catch (InvalidLocationException e) {
        locs = Collections.EMPTY_LIST;
      }

      final int nLocs = locs.size();

      if (nLocs == 0) {
        String desc = Long.toString(ensjTranscript.getInternalID());
        String t = ensjTranscript.getAccessionID();
        if ( t!=null ) desc = desc + " (" + t + ")";
        logger.warn("Failed to translate transcript: " + desc);
        return;
      }

      LinearLocation firstLoc = (LinearLocation)locs.get(0);
      LinearLocation lastLoc = (LinearLocation)locs.get(nLocs-1);

      if (set.getStrand() == 1) {
        logger.debug("Forward strand transcript: coding start = " + firstLoc.getStart() + " coding end = " + lastLoc.getEnd());
        logger.debug("                         : set low = " + set.getLow() + " set high = " + set.getHigh());
        set.setTranslationStart(firstLoc.getStart());
        set.setTranslationEnd(lastLoc.getEnd());
      } else {
        logger.debug("Reverse strand transcript: coding start = " + firstLoc.getEnd() + " coding end = " + lastLoc.getStart());
        logger.debug("                         : set low = " + set.getLow() + " set high = " + set.getHigh());
        set.setTranslationStart(firstLoc.getEnd());
        set.setTranslationEnd(lastLoc.getStart());
      }
    }
    return;
  }



  private Exon createExon(org.ensembl19.datamodel.Exon ensjExon) {

    Exon exon = new Exon();

    String id = Long.toString(ensjExon.getInternalID());
    exon.setId( id );
    exon.setName( getName(ensjExon, id) );
    //exon.setRefSequence(genomeSeq);
    LinearLocation loc = (LinearLocation)ensjExon.getLocation();
    exon.setStrand( loc.getStrand() );
    exon.setLow( loc.getStart() );
    exon.setHigh( loc.getEnd() );



    // TODO exon.addSynonym();
    // TODO add Evidence
    // uncomment when supporting features come into ensembl_9_30
    /*
    if ( getStateAsBoolean("include.SupportingFeature") ) {
      Iterator supIter = ensjExon.getSupportingFeatures().iterator();
      while ( supIter.hasNext() ) {

        org.ensembl19.datamodel.SupportingFeature sup
          = (org.ensembl19.datamodel.SupportingFeature)supIter.next();

        exon.addEvidence(""+sup.getInternalID(),sup.getDisplayName(),EvidenceConstants.SIMILARITY);
      }
    }
     */

    return exon;
  }


  private void addTranscripts(StrandedFeatureSet root)
    throws apollo.dataadapter.ApolloAdapterException {

    List genes = null;
    Hashtable geneTypeHash = new Hashtable();

    SimpleTimer timer = new SimpleTimer().start();

    try {
      if ( geneAdaptor==null )
        initialiseDriver();
      genes = getGenes();
    } catch (ConfigurationException exception) {
      throw new apollo.dataadapter.ApolloAdapterException(
        "Failed to retrieve data from ensj driver:" + exception.getMessage(),
        exception
      );
    }

    Iterator geneIter = genes.iterator();
    while ( geneIter.hasNext() ) {

      org.ensembl19.datamodel.Gene gene
        = (org.ensembl19.datamodel.Gene)geneIter.next();

      StrandedFeatureSetI subRoot;
      logger.debug("gene type = " + gene.getType() );
      if (!geneTypeHash.containsKey(gene.getType())) {
        System.out.println("Adding gene type for " + gene.getType());
        subRoot = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
        subRoot.getForwardSet().setFeatureType(gene.getType());
        subRoot.getReverseSet().setFeatureType(gene.getType());
        geneTypeHash.put(gene.getType(),(Object)subRoot);
      } else {
        System.out.println("Using gene type hash for " + gene.getType());
        subRoot = (StrandedFeatureSetI)(geneTypeHash.get(gene.getType()));
      }
      FeatureSetI geneFeature = new FeatureSet();
      geneFeature.setFeatureType(gene.getType());
      geneFeature.setName( getName(gene) );
      geneFeature.setId(getName(gene));

      Iterator transcriptIter = gene.getTranscripts().iterator();
      while ( transcriptIter.hasNext() ) {

        org.ensembl19.datamodel.Transcript transcript
          = (org.ensembl19.datamodel.Transcript)transcriptIter.next();

        FeatureSetI transcriptFeature = new FeatureSet();
        transcriptFeature.setFeatureType(gene.getType());
        transcriptFeature.setName( getName(transcript) );
        transcriptFeature.setId( getName(transcript) );
        //transcriptFeature.setRefSequence(genomeSeq);
        System.out.println ("Added " + transcriptFeature.getName() + " to "+
                            geneFeature.getName() + " a " + 
                            geneFeature.getFeatureType());
        Iterator exonIter = transcript.getExons().iterator();
        while ( exonIter.hasNext() ) {

          org.ensembl19.datamodel.Exon exon
            = (org.ensembl19.datamodel.Exon)exonIter.next();

          // TODO replace this with sophisticated "exon" wrapper
          LinearLocation l = (LinearLocation)exon.getLocation();
          SeqFeatureI exonFeature = new SeqFeature(l.getStart(),
                                                   l.getEnd(),
                                                   gene.getType());

          exonFeature.setStrand( l.getStrand() );
          String id = Long.toString(exon.getInternalID());
          exonFeature.setName( getName(exon, id) );
          exonFeature.setFeatureType( gene.getType() );
          exonFeature.setId( id );
          exonFeature.setScore(100.0); // stop it shrinking to small
          //exonFeature.setRefSequence(genomeSeq);

          transcriptFeature.setStrand( l.getStrand() );
          geneFeature.setStrand( l.getStrand() );
          transcriptFeature.addFeature( exonFeature, true );
        }
        setTranslationStartEndFromEnsjTranscript(transcriptFeature,transcript);
        geneFeature.addFeature(transcriptFeature, true);
        //geneFeature.setRefSequence(genomeSeq);

      }
      subRoot.addFeature(geneFeature);
    }

    Enumeration e = geneTypeHash.keys();
    while (e.hasMoreElements()) {
      StrandedFeatureSetI sfs = (StrandedFeatureSetI)geneTypeHash.get(e.nextElement());
      root.addFeature((FeatureSetI)sfs.getForwardSet());
      root.addFeature((FeatureSetI)sfs.getReverseSet());
    }
    logger.info( loadMessage("genes (conversion to Apollo object model)", 
                             genes,
                             timer.stop().getDuration()) );

  }



  /**
   * Create query based on current region and other pieces of information
   * specified in state.
   */
  private Query createQuery() throws AdaptorException, ConfigurationException {
    Query q = new Query();
    q.setLocation(getLocation());
    q.setIncludeChildren(true);
    return q;
  }

  private String getName(Accessioned item, String defaultName) {
    String name = item.getAccessionID();
    if ( name==null )
      name = defaultName;

    return name;
  }

  private String getName(Accessioned item) {
    return getName(item, Long.toString(item.getInternalID()));
  }


  private String loadMessage(String type,
                             Collection data,
                             long time) {
    long total = dnaProteinLoadTime
      + simplePeptideLoadTime
      + featureLoadTime
      + repeatLoadTime
      + predictionTranscriptsLoadTime
      + geneLoadTime
      + sequenceLoadTime;
    return type+" features: num = " + data.size()
      + ", load time = " + time +"ms"
      + " (total = "+total+")";
  }



  private boolean getStateAsBoolean(String type) {
    logger.debug("stateInformation = " +
                 PropertiesUtil.toString(stateInformation));

    return PropertiesUtil.booleanValue( stateInformation,
                                        type,
                                        false);
  }

  protected String getRegion(){
    return region;
  }

  protected SequenceI getReferenceSequence(){
    return genomeSeq;
  }//end getReferenceSequence

  /**
   * This is a raw jdbc fetch of chromosome name that completely breaks all ensj-encapsulation,
   * in the name of speed.
  **/
  List getChromosomes() throws org.bdgp.io.DataAdapterException, ConfigurationException{
    List returnList = new ArrayList();
    //    List chromosomeList = null;
    
    String jdbcDriver = getStateInformation().getProperty(StateInformation.JDBC_DRIVER);
    String host = getStateInformation().getProperty(StateInformation.HOST);
    String port = getStateInformation().getProperty(StateInformation.PORT);
    String database = getStateInformation().getProperty(StateInformation.DATABASE);
    String url = "jdbc:mysql://" + host + ":" + port + "/"+database;
    String user = getStateInformation().getProperty(StateInformation.USER);
    String password = getStateInformation().getProperty(StateInformation.PASSWORD);

    if(
      jdbcDriver == null ||
      jdbcDriver.trim().length() <= 0 ||
      host == null ||
      host.trim().length() <= 0 ||
      port == null ||
      port.trim().length() <= 0 ||
      user == null ||
      user.trim().length() <= 0
    ){
      return new ArrayList();
    }

    if(password == null){
      password = "";
    }

    try{
      Class.forName(jdbcDriver).newInstance();
    }catch(IllegalAccessException exception){
      throw new apollo.dataadapter.ApolloAdapterException(
        "Cannot access the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }catch(InstantiationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(
        "Cannot create the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }catch(ClassNotFoundException exception){
      throw new apollo.dataadapter.ApolloAdapterException(
        "Cannot find the driver class "+jdbcDriver+"\n"+exception.getMessage(),
        exception
      );
    }//end try

    try{
      java.sql.Connection connection = java.sql.DriverManager.getConnection(url,user,password);
      java.sql.Statement statement = connection.createStatement();

      java.sql.ResultSet names = statement.executeQuery("select name from chromosome");
      String chromosomeName;

      while(names.next()){
        chromosomeName = names.getString(1);
        returnList.add(chromosomeName);
      }//end while
    }catch(java.sql.SQLException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage(), exception);
    }

    Collections.sort(returnList);
    return returnList;
  }//end getChromsomes  
  
  private LocationConverter getLocationConverter(){
    return locationConverter;
  }
  
  private void setLocationConverter(LocationConverter converter){
    locationConverter = converter;
  }
  
  private void setMode(int mode){
    this.mode = mode;
  }
  
  public void clearStateInformation() {
    stateInformation = new StateInformation();
  }

  /**
   * Validates that the core information is present
  **/
  private void validateDatabaseProperties(
    Properties properties
  ) throws apollo.dataadapter.ApolloAdapterException{

    String host = properties.getProperty(StateInformation.HOST);
    String port = properties.getProperty(StateInformation.PORT);
    String database = properties.getProperty(StateInformation.DATABASE);
    
    if(host == null || host.trim().length() <= 0){
      throw new apollo.dataadapter.ApolloAdapterException("database host name must be provided");
    }else if(
      port == null ||
      port.trim().length() <= 0
    ){
      throw new apollo.dataadapter.ApolloAdapterException("database port  must be provided");
    }else if(
      database == null ||
      database.trim().length() <= 0
    ){
      throw new apollo.dataadapter.ApolloAdapterException("ensembl database name must be provided");
    }//end if
  }
  

  protected void validateRegionSpecified(Properties properties)
  throws apollo.dataadapter.ApolloAdapterException
  {

    String region = getRegion();
    Region loc = null;
    CloneFragment cf = null;
    CloneFragmentLocation cfl = null;
    AssemblyLocation al = null;

    if(region == null){
      throw new apollo.dataadapter.ApolloAdapterException(
        "Must specify a region to load"
      );
    }
      
    if ( getMode()==ASSEMBLY_LOCATION_MODE ) {

      try{
        // Region has taken on this function
        loc = new Region(region);//SequenceUtil.parseChrStartEndString(region);
      }catch(RuntimeException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Specified chromosome/start/end could not be parsed as such"
        );
      }
      
    } 
    else if ( getMode()==CLONE_FRAGMENT_MODE ) {

      if ( cloneFragmentAdaptor==null ){
        throw new IllegalStateException("Fatal problem - uninitialised clone fragment adapter!");
      }
      
      try{
        cf = cloneFragmentAdaptor.fetch(region);
      }catch(AdaptorException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Could not fetch clone fragment with specified id"
        );
      }
      
      cfl = new CloneFragmentLocation();
      
      if(cf == null){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Clone Fragment with specified ID does not exist"
        );
      }
    }
    else if ( getMode()==STABLE_ID_MODE ) {
      String stableID = region.substring( STABLE_ID_PREFIX.length(), region.length());
      logger.info("Fetching stable id:" + stableID);
  
      try{
        al = stableID2Location( stableID );
      }catch(ConfigurationException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Driver Configuration problem fetching object with stable ID "+stableID
        );
      }catch(AdaptorException exception){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Could not fetch object with stable ID "+stableID 
        );
      }
      
      if ( al==null ){
        throw new apollo.dataadapter.ApolloAdapterException(
          "Could not find anything with the input stable id"
        );
      }
    }else{
      throw new apollo.dataadapter.ApolloAdapterException(
        "Must specify whether location is Chromosome, Clone or Stable Id"
      );
    }
  }   
}
