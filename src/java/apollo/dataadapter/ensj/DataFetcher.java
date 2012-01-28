package apollo.dataadapter.ensj;

import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.datamodel.seq.*;

import org.apache.log4j.*;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.*;
import java.sql.*;

import org.ensembl.*;
import org.ensembl.driver.*;
import org.ensembl.datamodel.*;
import org.ensembl.util.*;

/**
 * I am just a separate helper class to stop the EnsJAdapter from becoming too huge.
 * I keep all the code that drives ensj to fetch stuff.
**/
public class DataFetcher {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DataFetcher.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private EnsJAdapter _adapter;
  
  public DataFetcher(EnsJAdapter adapter){
    _adapter = adapter;
  }
  
  private EnsJAdapter getAdapter(){
    return _adapter;
  }
  
  private String makeGeneTypeString(org.ensembl.datamodel.Gene ensJGene, String prefix, int schema) {
    String type;

    if (schema == 30) {
      type = ensJGene.getType();
    } else {
      String prefStr = "";
      if (prefix.length() > 0)
        prefStr = prefix + "_";
       
        
      type = prefStr + 
             (ensJGene.getType() != null ? ensJGene.getType() : "") + "_" + 
             (ensJGene.getConfidence() != null ? ensJGene.getConfidence() : "") + "_" + 
             (ensJGene.getSource() != null ? ensJGene.getSource() : "");
    }
    return type;
  }
  
  /**
   * Retrieves ensJ genes/transcripts/exons, converts them into seq features and adds them
   * to the curation set. Passes out the list of ensj-genes retreived: this so that the simple-feature
   * adapter can, if it wants, use the translations to retrieve protein annotations.
  **/
  public List addGenes(StrandedFeatureSet results, Location location, java.util.List includedTypes, boolean aggressiveNaming, String typePrefix) 
  throws AdaptorException 
  {
    GeneAdaptor adaptor = getAdapter().getDriver().getGeneAdaptor();
    logger.info("Doing initial gene fetch");
    Iterator genes = adaptor.fetch(location,true,aggressiveNaming).iterator();
    logger.info("Done initial gene fetch");

    Iterator transcripts;
    Iterator exons;
    String unPrefixedType;
    String prefixedType;
    List returnListOfEnsJGenes = new ArrayList();
    
    org.ensembl.datamodel.Gene ensJGene;
    org.ensembl.datamodel.Transcript ensJTranscript;
    org.ensembl.datamodel.Exon ensJExon;
    
    FeatureSet geneFeature;
    FeatureSet transcriptFeature;
    SeqFeature exonFeature;
    
    Location exonLocation;
    int returnedNumber = 0;
    boolean checkType = (includedTypes.size() > 0);
    List codingLocations = null;
    Location firstLocation = null;
    Location lastLocation = null;

    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);
    
    while(genes.hasNext()){
      ensJGene = (org.ensembl.datamodel.Gene)genes.next();

      unPrefixedType = makeGeneTypeString(ensJGene,"",schema);
      prefixedType = makeGeneTypeString(ensJGene,typePrefix,schema);

      if(checkType){
        if(!includedTypes.contains(unPrefixedType)){
          continue;
        }
      }
      
      returnListOfEnsJGenes.add(ensJGene);
      
      returnedNumber++;
      
      geneFeature = new FeatureSet();

      geneFeature.setFeatureType(prefixedType);
      //if(ensJGene.getDisplayName() != null){
      //  geneFeature.setName(ensJGene.getDisplayName());
      //} else 
      if(ensJGene.getAccessionID() != null){
        geneFeature.setName(ensJGene.getAccessionID());
      }else{
        geneFeature.setName(String.valueOf(ensJGene.getInternalID()));
      }
      geneFeature.setId(String.valueOf(ensJGene.getInternalID()));
      
      transcripts = ensJGene.getTranscripts().iterator();
      
      while(transcripts.hasNext()){
        ensJTranscript = (org.ensembl.datamodel.Transcript)transcripts.next();
        
        transcriptFeature = new FeatureSet();
        transcriptFeature.setFeatureType(prefixedType);
        //if(ensJTranscript.getDisplayName() != null){
        //  transcriptFeature.setName(ensJTranscript.getDisplayName());
        //} else 
        if(ensJTranscript.getAccessionID() != null){
          transcriptFeature.setName(ensJTranscript.getAccessionID());
        } else if (aggressiveNaming)  {
          // Protect the getSupportingFeatures call with a try catch block to
          // allow code to be used with pre 31 dbs (with correct ensj jar) which
          // don't have a transcript_supporting_feature table
          try {
            Iterator supports = ensJTranscript.getSupportingFeatures().iterator();
            if (supports.hasNext()) {
              org.ensembl.datamodel.FeaturePair sf = (org.ensembl.datamodel.FeaturePair)supports.next();
              String tscriptName = sf.getHitAccession() + " (" + ensJTranscript.getInternalID() + ")";
              transcriptFeature.setName(tscriptName);
            } else {
              transcriptFeature.setName(String.valueOf(ensJTranscript.getInternalID()));
            }
          } catch (Exception e) {
            transcriptFeature.setName(String.valueOf(ensJTranscript.getInternalID()));
          }
        } else {
          transcriptFeature.setName(String.valueOf(ensJTranscript.getInternalID()));
        }
        transcriptFeature.setId(String.valueOf(ensJTranscript.getInternalID()));
        
        exons = ensJTranscript.getExons().iterator();
        while(exons.hasNext()){

          ensJExon = (org.ensembl.datamodel.Exon)exons.next();
          exonLocation = ensJExon.getLocation();

          exonFeature = new SeqFeature();
          exonFeature.setLow(exonLocation.getStart());
          exonFeature.setHigh(exonLocation.getEnd());
          exonFeature.setStrand(exonLocation.getStrand());
          //exonFeature.setId(transcriptFeature.getName());
          // Note: Setting to exon stable id makes status bar show exon id rather than transcript id
          // which is not ideal. Just use transcript id for name (can get exon id if load as annotation)
          //exonFeature.setName(ensJExon.getAccessionID() != null ? ensJExon.getAccessionID() : transcriptFeature.getName());
          exonFeature.setName(transcriptFeature.getName());
          exonFeature.setId(String.valueOf(ensJExon.getInternalID()));
          exonFeature.setFeatureType(prefixedType);
          
          geneFeature.setStrand(exonLocation.getStrand());
          transcriptFeature.setStrand(exonLocation.getStrand());

          transcriptFeature.addFeature(exonFeature);
        }
        
        if (ensJTranscript.getTranslation() != null) {
          codingLocations = ensJTranscript.getTranslation().getCodingLocations();
          if (codingLocations.size() > 0) {
            firstLocation = (Location)codingLocations.get(0);
            lastLocation = (Location)codingLocations.get(codingLocations.size()-1);

            if (transcriptFeature.getStrand() == 1) {
              transcriptFeature.setTranslationStart(firstLocation.getStart());
              transcriptFeature.setTranslationEnd(lastLocation.getEnd());
            } else {
              transcriptFeature.setTranslationStart(firstLocation.getEnd());
              transcriptFeature.setTranslationEnd(lastLocation.getStart());
            }
          }
        }
        
        geneFeature.addFeature(transcriptFeature);
      }
      
      results.addFeature(geneFeature,false);
    }
    
    return returnListOfEnsJGenes;
  }
  
  /**
   * Add all repeat features into the input set.
  **/
  public int addRepeatFeatures(CurationSet curationSet, List flatFeatures, Location location) throws AdaptorException{
    
    RepeatFeatureAdaptor adaptor = getAdapter().getDriver().getRepeatFeatureAdaptor();
    List features = adaptor.fetch(location);
    RepeatFeature feature;
    Iterator iterator = features.iterator();
    SeqFeatureI repeatHit;
    SeqFeatureI genomeHit;
    Location genomeLocation;
    Location repeatLocation;
    RepeatConsensus repeatConsensus;
    apollo.datamodel.FeaturePair featurePair;
    int returnedNumber = 0;
    
    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);

    while(iterator.hasNext()){
      feature = (RepeatFeature)iterator.next();

      if (schema > 30 && feature.getLocation().containsGapNodes()) {
        continue;
      }

      returnedNumber++;

      genomeHit = new SeqFeature();
      genomeLocation = feature.getLocation();
      if(genomeLocation.getStart() <= genomeLocation.getEnd()){
        genomeHit.setLow( genomeLocation.getStart() );
        genomeHit.setHigh( genomeLocation.getEnd() );
      }else{
        genomeHit.setLow(genomeLocation.getEnd());
        genomeHit.setHigh(genomeLocation.getStart());
      }
      genomeHit.setStrand( genomeLocation.getStrand() );

      repeatHit = new SeqFeature();
      repeatHit.setName( feature.getHitDisplayName() );
      repeatLocation = feature.getHitLocation();
      if(repeatLocation.getStart() <= repeatLocation.getEnd()){
        repeatHit.setLow( repeatLocation.getStart() );
        repeatHit.setHigh( repeatLocation.getEnd() );
      }else{
        repeatHit.setLow( repeatLocation.getEnd() );
        repeatHit.setHigh( repeatLocation.getStart() );
      }
        

      repeatConsensus = feature.getRepeatConsensus();

      if (repeatConsensus != null) {
        genomeHit.setName(repeatConsensus.getName());
        genomeHit.setId(repeatConsensus.getName());

        repeatHit.setName(repeatConsensus.getName());
        repeatHit.setId(repeatConsensus.getName());
      } else {
        genomeHit.setName(feature.getDisplayName());
        genomeHit.setId(feature.getDisplayName());
                                                                                                                                             
        repeatHit.setName( feature.getHitDisplayName() );
        repeatHit.setId( feature.getHitDisplayName() );
      }
                                                                                                                                             
      genomeHit.setFeatureType( feature.getAnalysis().getLogicalName() );
      genomeHit.setScore(feature.getScore());
      repeatHit.setScore(feature.getScore());
      featurePair = new apollo.datamodel.FeaturePair( genomeHit, repeatHit );
      
      getAdapter().setSequence(featurePair, curationSet);
      flatFeatures.add(featurePair);
    }
    
    return returnedNumber;
  }

  public int addProteinAlignFeatures(CurationSet curationSet, List flatFeatures, Location location, List includedTypes) 
  throws AdaptorException 
  {

    String[] logicalNames = new String[includedTypes.size()];
    DnaProteinAlignmentAdaptor adaptor = getAdapter().getDriver().getDnaProteinAlignmentAdaptor();
    Iterator featuresIter = null;
    List ungappedFeatures = null;
    Iterator ungappedFeaturesIter = null;
    DnaProteinAlignment feature;
    DnaProteinAlignment ungappedFeature;
    DnaProteinAlignment gappedFeature;
    SeqFeatureI proteinHit;
    SeqFeatureI genomeHit;
    Location proteinLocation;
    Location genomeLocation;
    apollo.datamodel.FeaturePair pair;
    int returnedNumber = 0;

    logicalNames = (String[])includedTypes.toArray(logicalNames);
    
    if(includedTypes.size() > 0){
      featuresIter = adaptor.fetch(location, logicalNames).iterator();
    }else{
      featuresIter = adaptor.fetch(location).iterator();
    }
    
    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);

    while(featuresIter.hasNext()){

      gappedFeature = (DnaProteinAlignment)featuresIter.next();

      if (schema > 30 && gappedFeature.getLocation().containsGapNodes()) {
        continue;
      }

      try {
        ungappedFeatures = gappedFeature.getUngappedAlignmentFeatures();
      } catch (Exception e) {
        logger.warn("Feature " + gappedFeature.getInternalID() + " ignored. Probably means its broken in db");
        continue;
      }  
      ungappedFeaturesIter = ungappedFeatures.iterator();

      FeatureSet newSet = null;
      while (ungappedFeaturesIter.hasNext()) {
        feature = (DnaProteinAlignment)ungappedFeaturesIter.next();

        returnedNumber++;
  
        proteinHit = new SeqFeature();
        proteinHit.setName(feature.getHitAccession());
        //proteinHit.setId(feature.getHitAccession());
        proteinHit.setId("" + feature.getInternalID());
        proteinHit.setId(String.valueOf(feature.getInternalID()));
        proteinHit.setScore(feature.getScore());
        proteinLocation = feature.getHitLocation();
        proteinHit.setLow( proteinLocation.getStart());
        proteinHit.setHigh(proteinLocation.getEnd());
  
        genomeHit = new SeqFeature();
        genomeHit.setName(feature.getHitAccession());
        //genomeHit.setId(feature.getHitAccession());
        genomeHit.setId(String.valueOf(feature.getInternalID()));
        genomeHit.setFeatureType(feature.getAnalysis().getLogicalName());
        genomeHit.setScore(feature.getScore());
        genomeLocation = feature.getLocation();
        genomeHit.setLow( genomeLocation.getStart() );
        genomeHit.setHigh( genomeLocation.getEnd() );
        genomeHit.setStrand( genomeLocation.getStrand() );
  
        pair = new apollo.datamodel.FeaturePair( genomeHit, proteinHit );
        getAdapter().setSequence(pair, curationSet);
        flatFeatures.add(pair);
      }
    }
    return returnedNumber;
  }
  

  public int addDnaAlignFeatures(CurationSet curationSet, List flatFeatures, Location location, List includedTypes) 
  throws AdaptorException 
  {

    String[] logicalNames = new String[includedTypes.size()];
    DnaDnaAlignmentAdaptor adaptor = getAdapter().getDriver().getDnaDnaAlignmentAdaptor();
    Iterator features;
    List ungappedFeatures = null;
    Iterator ungappedFeaturesIter = null;
    
    DnaDnaAlignment feature;
    DnaDnaAlignment gappedFeature;
    DnaDnaAlignment ungappedFeature;
    SeqFeatureI targetDnaHit;
    SeqFeatureI genomeHit;
    Location proteinLocation;
    Location genomeLocation;
    apollo.datamodel.FeaturePair pair;
    int returnedNumber = 0;

    logicalNames = (String[])includedTypes.toArray(logicalNames);
    
    if(includedTypes.size() > 0){
      features = adaptor.fetch(location, logicalNames).iterator();
    }else{
      features = adaptor.fetch(location).iterator();
    }

    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);
    
    while(features.hasNext()){
      gappedFeature = (DnaDnaAlignment)features.next();

      if (schema > 30 && gappedFeature.getLocation().containsGapNodes()) {
        continue;
      }
      
      try {
        ungappedFeatures = gappedFeature.getUngappedAlignmentFeatures();
      } catch (Exception e) {
        logger.warn("Feature " + gappedFeature.getInternalID() + " ignored. Probably means its broken in db");
        continue;
      }  

      ungappedFeaturesIter = ungappedFeatures.iterator();

      while (ungappedFeaturesIter.hasNext()) {
        feature = (DnaDnaAlignment)ungappedFeaturesIter.next();
        returnedNumber++;
  
        targetDnaHit = new SeqFeature();
        targetDnaHit.setName(feature.getHitAccession());
        //targetDnaHit.setId(feature.getHitAccession());
        targetDnaHit.setId(String.valueOf(feature.getInternalID()));
        targetDnaHit.setScore(feature.getScore());
        proteinLocation = feature.getHitLocation();
        targetDnaHit.setLow( proteinLocation.getStart());
        targetDnaHit.setHigh(proteinLocation.getEnd());
  
        genomeHit = new SeqFeature();
        genomeHit.setName(feature.getHitAccession());
        //genomeHit.setId(feature.getHitAccession());
        genomeHit.setId(String.valueOf(feature.getInternalID()));
        genomeHit.setFeatureType(feature.getAnalysis().getLogicalName());
        genomeHit.setScore(feature.getScore());
        genomeLocation = feature.getLocation();
        genomeHit.setLow( genomeLocation.getStart() );
        genomeHit.setHigh( genomeLocation.getEnd() );
        genomeHit.setStrand( genomeLocation.getStrand() );
  
        pair = new apollo.datamodel.FeaturePair( genomeHit, targetDnaHit );
        getAdapter().setSequence(pair, curationSet);
        flatFeatures.add(pair);
      }
    }
    
    return returnedNumber;
  }  
  
  public int addDitagFeatures(CurationSet curationSet, List flatFeatures, Location location, List includedTypes) 
  throws AdaptorException 
  {

    String[] logicalNames = new String[includedTypes.size()];
    DitagFeatureAdaptor adaptor = getAdapter().getDriver().getDitagFeatureAdaptor();
    Iterator features;
    
    DitagFeature feature;
    SeqFeatureI targetTagHit;
    SeqFeatureI genomeHit;
    Location tagLocation;
    Location genomeLocation;
    apollo.datamodel.FeaturePair pair;
    int returnedNumber = 0;

    logicalNames = (String[])includedTypes.toArray(logicalNames);
    
    if(includedTypes.size() > 0){
      features = adaptor.fetch(location, logicalNames, true).iterator();
    }else{
      features = adaptor.fetch(location, true).iterator();
    }

    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);
    
    while(features.hasNext()){
      feature = (DitagFeature)features.next();

      if (schema > 30 && feature.getLocation().containsGapNodes()) {
        continue;
      }
      
      returnedNumber++;

      String name = new String(feature.getDitag().getInternalID() + " (" + feature.getDisplayName() + ")");
      if (!feature.getDitagSide().equals("F")) {
        name = new String(name + "_pair" + feature.getDitagPairID());
      }

      targetTagHit = new SeqFeature();
      targetTagHit.setName(name);
      targetTagHit.setId(name);
      targetTagHit.setScore(feature.getDitag().getTagCount());
      tagLocation = feature.getHitLocation();
      targetTagHit.setLow( tagLocation.getStart());
      targetTagHit.setHigh(tagLocation.getEnd());

      genomeHit = new SeqFeature();
      genomeHit.setName(name);
      genomeHit.setId(name);
      genomeHit.setFeatureType(feature.getAnalysis().getLogicalName());
      genomeHit.setScore(feature.getDitag().getTagCount());
      genomeLocation = feature.getLocation();
      genomeHit.setLow( genomeLocation.getStart() );
      genomeHit.setHigh( genomeLocation.getEnd() );
      genomeHit.setStrand( genomeLocation.getStrand() );
 
      //System.out.println("start = " + genomeLocation.getStart() + " end " + genomeLocation.getEnd() + " strand " +  genomeLocation.getStrand());

      pair = new apollo.datamodel.FeaturePair( genomeHit, targetTagHit );
      getAdapter().setSequence(pair, curationSet);
      flatFeatures.add(pair);
    }
    
    return returnedNumber;
  }  

  public int addPredictionTranscripts(StrandedFeatureSet results, Location location, List includedTypes) 
  throws AdaptorException 
  {
    String[] logicalNames = new String[includedTypes.size()];
    PredictionTranscriptAdaptor adaptor = getAdapter().getDriver().getPredictionTranscriptAdaptor();
    Iterator features = null;
    PredictionTranscript transcript;
    PredictionExon exon;
    FeatureSet transcriptFeature;
    SeqFeature exonFeature;
    String type;
    Location exonLocation;
    int returnedNumber = 0;
    
    logicalNames = (String[])includedTypes.toArray(logicalNames);
    
    if(includedTypes.size() > 0){
      features = adaptor.fetch(location, logicalNames).iterator();
    }else{
      features = adaptor.fetch(location).iterator();
    }

    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);
    
    while(features.hasNext()){
      
      transcript = (PredictionTranscript)features.next();

      if (schema > 30 && transcript.getLocation().containsGapNodes()) {
        continue;
      }
      
      returnedNumber++;
      transcriptFeature = new FeatureSet();
      type = transcript.getAnalysis().getLogicalName();
      transcriptFeature.setFeatureType(type);
      transcriptFeature.setName(type + ":" + transcript.getInternalID());
      transcriptFeature.setId(String.valueOf(transcript.getInternalID()));
      
      Iterator predictionExons = transcript.getExons().iterator();
      
      while(predictionExons.hasNext()){
        
        exon = (PredictionExon)predictionExons.next();
        exonLocation = exon.getLocation();
        
        exonFeature = new SeqFeature();
        exonFeature.setLow(exonLocation.getStart());
        exonFeature.setHigh(exonLocation.getEnd());
        exonFeature.setStrand(exonLocation.getStrand());
        exonFeature.setName(transcriptFeature.getName());
        exonFeature.setId(transcriptFeature.getName());
        exonFeature.setFeatureType(type);
                                                                                                                                             
        if (exon.getPvalue() > 0) {
         exonFeature.setScore(exon.getPvalue());
        } else {
         exonFeature.setScore(exon.getScore());
        }
                                                                                                                                             
        transcriptFeature.setStrand(exonLocation.getStrand());
        transcriptFeature.addFeature(exonFeature);
      }
      results.addFeature(transcriptFeature,false);
    }
    
    return returnedNumber;
  }
  
  public int addSimpleFeatures(CurationSet curationSet, List flatFeatures, Location location, List includedTypes) 
  throws AdaptorException 
  {
    String[] logicalNames = new String[includedTypes.size()];
    SimpleFeatureAdaptor adaptor = getAdapter().getDriver().getSimpleFeatureAdaptor();
    Iterator features = null;
    SimpleFeature feature;
    Location genomeLocation;
    SeqFeature genomeFeature;
    int returnedNumber = 0;

    logicalNames = (String[])includedTypes.toArray(logicalNames);
    
    if(includedTypes.size() > 0){
      features = adaptor.fetch(location, logicalNames).iterator();
    }else{
      features = adaptor.fetch(location).iterator();
    }

    // Only need this to check whether can call containsGapNodes
    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);

    while(features.hasNext()){

      feature = (SimpleFeature)features.next();

      if (schema > 30 && feature.getLocation().containsGapNodes()) {
        continue;
      }

      returnedNumber++;

      genomeFeature = new SeqFeature();
      genomeFeature.setName(feature.getDisplayName());
      genomeFeature.setId(feature.getDisplayName());
      genomeFeature.setFeatureType(feature.getAnalysis().getLogicalName());
      genomeFeature.setScore(feature.getScore());
      genomeLocation = feature.getLocation();
      genomeFeature.setLow( genomeLocation.getStart() );
      genomeFeature.setHigh( genomeLocation.getEnd() );
      genomeFeature.setStrand( genomeLocation.getStrand() );

      flatFeatures.add(genomeFeature);
    }
    
    return returnedNumber;
  }

  public int addContigFeatures(CurationSet curationSet, List flatFeatures, Location location)
  throws AdaptorException 
  {
    CoordinateSystemAdaptor csa = getAdapter().getDriver().getCoordinateSystemAdaptor();
    CoordinateSystem contigCS;
    LocationConverter  locationConverter = getAdapter().getDriver().getLocationConverter();
    Location loc;
    Location contigLocation;
    SeqFeature contigFeature;
    int returnedNumber = 0;

    Location completeLoc = locationConverter.fetchComplete(location);
    if (completeLoc == null) return 0;
    if (completeLoc.getStrand()==0)
      completeLoc.setStrand(1);
 
    contigCS = csa.fetch("contig",null);

    contigLocation = locationConverter.convert(completeLoc,contigCS);

    while(contigLocation != null){

      Location node = contigLocation.copyNode();

      if (!node.isGap()) {
        returnedNumber++;
  
        Location chromContigLocation = locationConverter.convert(node,location.getCoordinateSystem());
  
        SeqFeature genomeFeature = new SeqFeature();
        genomeFeature.setName(node.getSeqRegionName());
        genomeFeature.setId(node.getSeqRegionName());
        genomeFeature.setFeatureType("contig");
  
  
        genomeFeature.setLow( chromContigLocation.getStart() );
        genomeFeature.setHigh( chromContigLocation.getEnd() );
        // Setting to node strand means it gets drawn on reverse strand when its backwards
        genomeFeature.setStrand( node.getStrand() );

        contigFeature = new SeqFeature();
        contigFeature.setName(node.getSeqRegionName());
        contigFeature.setId(node.getSeqRegionName());
        contigFeature.setFeatureType("contig");
  
  
        contigFeature.setLow( node.getStart() );
        contigFeature.setHigh( node.getEnd() );
        contigFeature.setStrand( chromContigLocation.getStrand() );

        apollo.datamodel.FeaturePair featurePair = new apollo.datamodel.FeaturePair( genomeFeature, contigFeature );

        getAdapter().setSequence(featurePair, curationSet);
        flatFeatures.add(featurePair);
      }

      contigLocation = contigLocation.next();
    }
    
    return returnedNumber;
  }
   
  
  public int addProteinAnnotations(CurationSet curationSet, List flatFeatures, List retrievedGenes) 
  throws AdaptorException 
  {
    ProteinFeatureAdaptor adaptor = getAdapter().getDriver().getProteinFeatureAdaptor();
    List ensJProteinFeatures;
    ProteinFeature feature;
    Location genomeLocation;
    SeqFeature genomeFeature;
    int returnedNumber = 0;
    List retrievedTranscripts = null;
    List translations = new ArrayList();
    Iterator translationIterator;
    Translation translation;
    int start = 0;
    int end = 0;
    String displayName;
    double score;
    Iterator annotationIterator;
    Location location;
    SeqFeatureI peptideHit;
    SeqFeatureI genomeHit;
    
    for(int i=0; i<retrievedGenes.size(); i++){
      retrievedTranscripts = ((org.ensembl.datamodel.Gene)retrievedGenes.get(i)).getTranscripts();
      for(int j=0; j<retrievedTranscripts.size(); j++){
        if(((org.ensembl.datamodel.Transcript)retrievedTranscripts.get(j)).getTranslation() != null){
          translations.add(((org.ensembl.datamodel.Transcript)retrievedTranscripts.get(j)).getTranslation());
        }
      }
    }

    translationIterator = translations.iterator();
    
    while ( translationIterator.hasNext() ) {
      translation = (org.ensembl.datamodel.Translation) translationIterator.next();
      
      annotationIterator = adaptor.fetch(translation).iterator();
      while(annotationIterator.hasNext()){
        feature = (ProteinFeature)annotationIterator.next();
        
        start = feature.getPeptideStart();
        end = feature.getPeptideEnd();
        displayName = feature.getDisplayName();
        score = feature.getScore();
        
        //why do a setTranslation?
        feature.setTranslation(translation);
        
        location = feature.getLocation();
        peptideHit = new SeqFeature();
        peptideHit.setLow( start );
        peptideHit.setHigh( end );
        peptideHit.setName( displayName );

        genomeHit = new SeqFeature();
        genomeHit.setLow( location.getStart() );
        genomeHit.setHigh( location.getEnd() );
        genomeHit.setStrand( location.getStrand() );
        genomeHit.setName( feature.getDisplayName() );
        genomeHit.setId( feature.getDisplayName() );
        genomeHit.setFeatureType( feature.getAnalysis().getLogicalName() );
        genomeHit.setScore( score );

        flatFeatures.add( new apollo.datamodel.FeaturePair( genomeHit, peptideHit ) );
          
      }
    }
      
    return returnedNumber;
  }

  protected StrandedFeatureSetI getAnnotations(CurationSet curationSet, List genes, boolean addTranscriptSupport)
    throws apollo.dataadapter.ApolloAdapterException, AdaptorException {

    Connection conn = getAdapter().getDriver().getConnection();
    int schema = EnsJConnectionUtil.getEnsemblSchema(conn);
    JDBCUtil.close(conn);

    StrandedFeatureSetI root = new StrandedFeatureSet( new AnnotatedFeature(),
                               new AnnotatedFeature() );
    if (genes != null) {
      Iterator geneIter = genes.iterator();
      while ( geneIter.hasNext() ) {
        org.ensembl.datamodel.Gene gene = (org.ensembl.datamodel.Gene)geneIter.next();
        AnnotatedFeatureI apolloGene = createGene(gene,schema,addTranscriptSupport);
        root.addFeature( apolloGene );
      }
    }
    return root;
  }

  private AnnotatedFeatureI createGene(org.ensembl.datamodel.Gene ensjGene, int schema, boolean addTranscriptSupport) {


    // Try not setting type prefix here - user probably won't select annotation to be created for all dbs, so may well prefer that original
    // type (with no prefix) is retained for the annotations
    String type = makeGeneTypeString(ensjGene, "", schema);

    AnnotatedFeatureI gene = new AnnotatedFeature();
    String source = ensjGene.getSource();

    String id = Long.toString(ensjGene.getInternalID());
    gene.setId( id );
    gene.setName( getName(ensjGene, id) );
    gene.setDescription( ensjGene.getDescription() );
    Location loc = (Location)ensjGene.getLocation();
    gene.setStrand( loc.getStrand() );
    gene.setFeatureType( type );

    Iterator transcripts = ensjGene.getTranscripts().iterator();
    while ( transcripts.hasNext() ) {
      apollo.datamodel.Transcript transcript =
        createTranscript((org.ensembl.datamodel.Transcript)transcripts.next(), type, addTranscriptSupport);
      gene.addFeature(transcript);
      transcript.setOwner( source );
    }
    // TODO setGeneXref();
    List synonyms = gene.getSynonyms();
    for( int i=0; i<synonyms.size(); ++i) {
      String synonym = (String)synonyms.get(i);
      gene.addSynonym(new apollo.datamodel.Synonym(synonym));
    }

    return gene;
  }

  private apollo.datamodel.Transcript createTranscript(org.ensembl.datamodel.Transcript ensjTranscript, String type, boolean addTranscriptSupport) {

    apollo.datamodel.Transcript transcript = new apollo.datamodel.Transcript();

    String id = Long.toString(ensjTranscript.getInternalID());
    transcript.setId( id );
    transcript.setName( getName(ensjTranscript, id) );
    Location loc = ensjTranscript.getLocation();
    transcript.setStrand( loc.getStrand() );
    transcript.setFeatureType( type );

    Iterator exons = ensjTranscript.getExons().iterator();
    while ( exons.hasNext() ) {
      apollo.datamodel.Exon exon = createExon(( org.ensembl.datamodel.Exon)exons.next());
      exon.setFeatureType(type);
      transcript.addExon(exon);
    }

    if (addTranscriptSupport) {
      Iterator supIter = ensjTranscript.getSupportingFeatures().iterator();
      while ( supIter.hasNext() ) {

        org.ensembl.datamodel.FeaturePair sup
          = (org.ensembl.datamodel.FeaturePair)supIter.next();

        transcript.addEvidence(""+sup.getInternalID(),sup.getHitAccession(),EvidenceConstants.SIMILARITY);
      }
    }

    setTranslationStartEndFromEnsjTranscript(transcript,ensjTranscript);

    return transcript;
  }

  // Note: modifies set
  private void setTranslationStartEndFromEnsjTranscript(FeatureSetI set,
                                                        org.ensembl.datamodel.Transcript ensjTranscript) {
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
        logger.error("Failed to translate transcript: " + desc);
        return;
      }

      Location firstLoc = (Location)locs.get(0);
      Location lastLoc = (Location)locs.get(nLocs-1);

      if (set.getStrand() == 1) {
        set.setTranslationStart(firstLoc.getStart());
        set.setTranslationEnd(lastLoc.getEnd());
      } else {
        set.setTranslationStart(firstLoc.getEnd());
        set.setTranslationEnd(lastLoc.getStart());
      }
    }
    return;
  }

  private apollo.datamodel.Exon createExon(org.ensembl.datamodel.Exon ensjExon) {

    apollo.datamodel.Exon exon = new apollo.datamodel.Exon();

    String id = Long.toString(ensjExon.getInternalID());
    exon.setId( id );
    exon.setName( getName(ensjExon, id) );
    Location loc = (Location)ensjExon.getLocation();
    exon.setStrand( loc.getStrand() );
    exon.setLow( loc.getStart() );
    exon.setHigh( loc.getEnd() );

    // TODO add Evidence
    // uncomment when supporting features are better supported!
    /*
    if ( getStateAsBoolean("include.SupportingFeature") ) {
      Iterator supIter = ensjExon.getSupportingFeatures().iterator();
      while ( supIter.hasNext() ) {

        org.ensembl.datamodel.FeaturePair sup
          = (org.ensembl.datamodel.FeaturePair)supIter.next();

        exon.addEvidence(""+sup.getInternalID(),sup.getHitAccession(),EvidenceConstants.SIMILARITY);
      }
    }
    */

    return exon;
  }

  private String getName(Accessioned item, String defaultName) {
    String name = item.getAccessionID();
    if ( name==null )
      name = defaultName;

    return name;
  }
}
