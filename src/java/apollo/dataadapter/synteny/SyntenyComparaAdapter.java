package apollo.dataadapter.synteny;

import java.util.*;
import java.io.*;
import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;
import apollo.dataadapter.*;
import apollo.dataadapter.ensj.EnsJAdapter;

import org.apache.log4j.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;

import org.ensembl.*;
import org.ensembl.datamodel.*;
import org.ensembl.compara.datamodel.*;
import org.ensembl.driver.*;
import org.ensembl.compara.driver.*;
import org.ensembl.util.*;

import apollo.dataadapter.otter.parser.*;

public class SyntenyComparaAdapter extends AbstractApolloAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SyntenyComparaAdapter.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private SyntenyComparaAdapterGUI theAdapterGUI;
  private String querySpecies;
  private String hitSpecies;
  private int start;
  private int end;
  private String chromosome;
  private String coordsystem;
  private int hitStart;
  private int hitEnd;
  private String hitChromosome;
  private String hitCoordSystem;
  private String dnaAligns;
  private String [] dnaAlignTypes;
  private String highConservationDnaAligns;
  private String proteinAligns;
  private String queryStableId;
  private String hitStableId;
  private String stableId;
  public static int BUFFER_SIZE = 200000;
  protected Properties stateInformation;
  private String loggingFile;
  protected Properties driverConf;

  IOOperation []
    supportedOperations =
      {
        ApolloDataAdapterI.OP_READ_DATA
      };
  
  /**
   * BEWARE: this method creates a brand-new instance of this adapter's UI and hands it back!
  **/
  public DataAdapterUI getUI(IOOperation op) {
    return new SyntenyComparaAdapterGUI(op);
  }

  // These three methods used to be inherited from ensj19.EnsJAdapter, but as I'm trying
  // to remove dependencies on that (for its eventual removal) I've put them here
  public void setLoggingFile(String  v) {
    this.loggingFile = v;
  }

  public Properties getStateInformation() {
    return stateInformation;
  }
 
  public void setDriverConf(Properties  v) {
    this.driverConf = v;
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  private apollo.datamodel.FeaturePair convertEnsJFeaturePairToApolloFeaturePair(
    org.ensembl.datamodel.FeaturePair ensJFeaturePair,
    String type,
    boolean invertFeatures
  ){
    SeqFeature queryFeature = new SeqFeature();
    SeqFeature hitFeature = new SeqFeature();
    apollo.datamodel.FeaturePair apolloFeaturePair;

    queryFeature.setId(ensJFeaturePair.getDisplayName());
    queryFeature.setStart(ensJFeaturePair.getLocation().getStart());
    queryFeature.setEnd(ensJFeaturePair.getLocation().getEnd());
    if(ensJFeaturePair.getDescription() != null){
      queryFeature.setName(ensJFeaturePair.getDescription());
    }else{
      queryFeature.setName(apollo.datamodel.Range.NO_NAME);
    }

    // SMJS If you don't set strand the feature gets added to both strands in the StrandedFeatureSet
    //      However working out what manipulation needs doing to the strand is something I'd prefer
    //      not to have to do
    logger.debug("Set strand to " + ensJFeaturePair.getLocation().getStrand() + " for feat of type " + type);
    queryFeature.setStrand(ensJFeaturePair.getLocation().getStrand());

    hitFeature.setId(ensJFeaturePair.getDisplayName());
    hitFeature.setStart(ensJFeaturePair.getHitLocation().getStart());
    hitFeature.setEnd(ensJFeaturePair.getHitLocation().getEnd());

    if(ensJFeaturePair.getHitDescription() != null){
      hitFeature.setName(ensJFeaturePair.getHitDescription());
    }else{
      hitFeature.setName(apollo.datamodel.Range.NO_NAME);
    }

    logger.debug("Set hit strand to " + ensJFeaturePair.getHitLocation().getStrand() + " for feat of type " + type);
    hitFeature.setStrand(ensJFeaturePair.getHitLocation().getStrand());

    if(!invertFeatures){
      apolloFeaturePair = new apollo.datamodel.FeaturePair(queryFeature,hitFeature);
    }else{
      apolloFeaturePair = new apollo.datamodel.FeaturePair(hitFeature, queryFeature);
    }
    
    apolloFeaturePair.setFeatureType(type);
    
    if(type.equals("dna-dna-align")){
      
      apolloFeaturePair.setScore(((DnaDnaAlignFeature)ensJFeaturePair).getPercentageIdentity());
      
      if(((DnaDnaAlignFeature)ensJFeaturePair).getMethodLinkType().equals("BLASTZ_NET_TIGHT")){
        apolloFeaturePair.setFeatureType("dna-dna-align-high-conservation");
      }
    }
    
    apolloFeaturePair.setCigar(ensJFeaturePair.getCigarString());
    
    return apolloFeaturePair;
  }//end convertEnsJFeaturePairToApolloFeaturePair
  
  public CurationSet getCurationSet() throws apollo.dataadapter.ApolloAdapterException{
    CurationSet curationSet = new CurationSet();
    StrandedFeatureSet featureSet = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);
    
      if(getProteinAligns().equals(String.valueOf(true))){

        //Need both ranges specified (or inferred from the above dna-align read)
        //to pull up protein aligns.

        if (getChromosome() != null && 
            getChromosome().trim().length()>0 &&
            getHitChromosome() != null && 
            getHitChromosome().trim().length()>0){
          logger.debug("Both loc find with " + getChromosome() + " " + getHitChromosome());

          findProteinAlignsForLocationAndSpecies(
            getQuerySpecies(),
            new AssemblyLocation( getChromosome(), getStart(), getEnd(), 0),
            getHitSpecies(),
            new AssemblyLocation( getHitChromosome(), getHitStart(), getHitEnd(), 0),
            featureSet);

        }else if(getChromosome() != null && 
                 getChromosome().trim().length()>0){
          logger.debug("QUERY loc find with " + getChromosome());

          findProteinAlignsForLocationAndSpecies(
            getQuerySpecies(),
            new AssemblyLocation( getChromosome(), getStart(), getEnd(), 0),
            getHitSpecies(),
            featureSet,
            curationSet,
            false); //query and hit-species are the "right way around"
          
        }else if(getHitChromosome() != null && 
                 getHitChromosome().trim().length()>0){
          
          logger.debug("HIT loc find with " + getHitChromosome());

          findProteinAlignsForLocationAndSpecies(
            getHitSpecies(),
            new AssemblyLocation( getHitChromosome(), getHitStart(), getHitEnd(), 0),
            getQuerySpecies(),
            featureSet,
            curationSet,
            true); //query and hit-species are NOT the "right way around" - I'm passing them in inverted.
          
        } else if(getQueryStableId() != null) {
          
          logger.debug("QUERY stable id find with " + getQueryStableId());

          findProteinAlignsForStableIdAndSpecies(
            getQuerySpecies(),
            new String[]{getQueryStableId()},
            getHitSpecies(),
            featureSet,
            false); //query and hit Are NOT inverted for this call
          
        } else if(getHitStableId() != null) {
          logger.debug("HIT stable id find with " + getHitStableId());

          findProteinAlignsForStableIdAndSpecies(
            getHitSpecies(),
            new String[]{getHitStableId()},
            getQuerySpecies(),
            featureSet,
            true); //query and hit ARE inverted for this call
            
        }
      }
    
      if(getDnaAligns().equals(String.valueOf(true)) ||
         getHighConservationDnaAligns().equals(String.valueOf(true))) {
        
        if(getChromosome()!= null && getHitChromosome() != null){
          findDnaAlignsForLocationAndSpecies(
            getQuerySpecies(),
            new AssemblyLocation( getChromosome(), getStart(), getEnd(), 0),
            getHitSpecies(),
            new AssemblyLocation( getHitChromosome(), getHitStart(), getHitEnd(), 0),
            featureSet,
            curationSet
          );
        } else if (getChromosome() != null) {
          
          findDnaAlignsForLocationAndSpecies(
            getQuerySpecies(),
            new AssemblyLocation( getChromosome(), getStart(), getEnd(), 0),
            getHitSpecies(),
            featureSet,
            curationSet,
            false); //query and hit Are NOT inverted for this call

        } else if(getHitChromosome()!= null) {
          
          findDnaAlignsForLocationAndSpecies(
            getHitSpecies(),
            new AssemblyLocation( getHitChromosome(), getHitStart(), getHitEnd(), 0),
            getQuerySpecies(),
            featureSet,
            curationSet,
            true); //query and hit ARE inverted for this call

        }else{
          throw new apollo.dataadapter.ApolloAdapterException(
            "You have opted to read dna-aligns, but I cannot infer any range information"
          );
        }
      }
      
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
    
    logger.debug("Size of featureSet at end of SCA getCurationSet = " + featureSet.size());
    curationSet.setResults(featureSet);
    
    return curationSet;
  }//end getCurationSet

  private void findDnaAlignsForLocationAndSpecies(
    String querySpecies,
    AssemblyLocation queryLocation,
    String hitSpecies,
    FeatureSetI featureSet,
    CurationSet curationSet,
    boolean queryAndHitAreInverted
  )throws apollo.dataadapter.ApolloAdapterException{
    
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);
      
      //Fetch dna-dna aligns as ensj FeaturePairs.

      List list = new ArrayList();
      
      if ( getDnaAligns().equals(String.valueOf(true)) &&
          !getHighConservationDnaAligns().equals(String.valueOf(true))){
        for (int i=0;i<getDnaAlignTypes().length;i++) {
          list.addAll(dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies, getDnaAlignTypes()[i]));
        }

      } else if( getHighConservationDnaAligns().equals(String.valueOf(true)) &&
                !getDnaAligns().equals(String.valueOf(true))){
        list = dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies, "BLASTZ_NET_TIGHT");

      } else {
        list = dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies);
      }

      //If we ran a one-sided fetch and it yielded no limit information, then 
      //bug out now.

      if(list.size() <= 0){
        throw new apollo.dataadapter.ApolloAdapterException(
          "No dna-dna alignments were found for the selected range - "+
          " I cannot infer the corresponding range on the hit species");
      }
      
      //set the chr/start/end limits on the curation set.

      findLimitsOfAligns(list, curationSet);

      logger.debug("Got " + list.size() + " ensj dna aligns");

      ensJFeaturePairs = list.iterator();

      int nConverted = 0;

      //Convert dna-dna aligns into Apollo FeaturePairs. DONT invert query<->hit feature

      while(ensJFeaturePairs.hasNext()){
        ensJFeaturePair = (org.ensembl.datamodel.FeaturePair)ensJFeaturePairs.next();
        logger.debug("curationSet.getChromosome = " + curationSet.getChromosome() +
                     " ensJ chromosome = " + ((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName());

        //only convert the ones on the right chromosome.
        if( curationSet.getChromosome().equals( ((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName())){
          apolloFeaturePair = 
            convertEnsJFeaturePairToApolloFeaturePair( ensJFeaturePair, "dna-dna-align", queryAndHitAreInverted);

          featureSet.addFeature(apolloFeaturePair,false);
          nConverted++;
        }
      }

      logger.debug("Converted " + nConverted + " of these to apollo features");
      
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
  }//end findDnaAlignsForLocationAndSpecies

  private void findDnaAlignsForLocationAndSpecies(
    String querySpecies,
    AssemblyLocation queryLocation,
    String hitSpecies,
    AssemblyLocation hitLocation,
    FeatureSetI featureSet,
    CurationSet curationSet
  ) throws apollo.dataadapter.ApolloAdapterException{
    
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);

      //Fetch dna-dna aligns as ensj FeaturePairs.
      List list = new ArrayList();
      
      if( getDnaAligns().equals(String.valueOf(true)) &&
         !getHighConservationDnaAligns().equals(String.valueOf(true))){
        for (int i=0;i<getDnaAlignTypes().length;i++) {
          list.addAll(dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies, getDnaAlignTypes()[i]));
        }
        
      } else if( getHighConservationDnaAligns().equals(String.valueOf(true)) &&
                !getDnaAligns().equals(String.valueOf(true))){
        list = dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies, "BLASTZ_NET_TIGHT");
        
      }else{
        list = dnaDnaAlignFeatureAdaptor.fetch( querySpecies, queryLocation, hitSpecies);
      }

      logger.debug("Got " + list.size() + " ensj dna aligns");

      ensJFeaturePairs = list.iterator();

      int nConverted = 0;

      // Convert dna-dna aligns into Apollo FeaturePairs. DONT invert query<->hit feature

      while(ensJFeaturePairs.hasNext()){
        ensJFeaturePair = (org.ensembl.datamodel.FeaturePair)ensJFeaturePairs.next();
        logger.debug("curationSet.getChromosome = " + curationSet.getChromosome() +
                     " ensJ chromosome = " + ((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName());

        if(((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName().equals(hitLocation.getSeqRegionName())){

          apolloFeaturePair = convertEnsJFeaturePairToApolloFeaturePair( ensJFeaturePair, "dna-dna-align", false);

          featureSet.addFeature(apolloFeaturePair,false);
          nConverted++;
        }
      }
      logger.debug("Converted " + nConverted + 
                   " of these to apollo features in curation set version of findDnaAlignsForLocationAndSpecies");
      
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
  }//end findDnaAlignsForLocationAndSpecies

  private void findProteinAlignsForLocationAndSpecies(
    String querySpecies,
    AssemblyLocation location,
    String hitSpecies,
    AssemblyLocation hitLocation,
    FeatureSetI featureSet
  )throws apollo.dataadapter.ApolloAdapterException{
     
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    int counter = 0;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);

      List list = memberAdaptor.fetch( querySpecies, location, hitSpecies, hitLocation);

      ensJFeaturePairs = list.iterator();
      
      //Convert protein aligns into Apollo FeaturePairs.

      while (ensJFeaturePairs.hasNext()) {
        ensJFeaturePair = (org.ensembl.datamodel.FeaturePair)ensJFeaturePairs.next();
        apolloFeaturePair = convertEnsJFeaturePairToApolloFeaturePair( ensJFeaturePair, "protein-protein-align", false);

        featureSet.addFeature(apolloFeaturePair,false);
      }
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
  }
   
  private void findProteinAlignsForLocationAndSpecies(
    String querySpecies,
    AssemblyLocation location,
    String hitSpecies,
    FeatureSetI featureSet,
    CurationSet curationSet,
    boolean areQueryAndHitInverted
  )throws apollo.dataadapter.ApolloAdapterException{
     
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    int counter = 0;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());
      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);

      List list = memberAdaptor.fetch( querySpecies, location, hitSpecies);

      //If we ran a one-sided fetch and it yielded no limit information, then 
      //bug out now.

      if(list.size() <= 0){
        throw new apollo.dataadapter.ApolloAdapterException(
          "No protein alignments were found for the selected range - "+
          " I cannot infer the corresponding range on the hit species"
        );
      }
      
      //This so the single-species fetch that follows this compara-fetch
      //will know where to look. Sets information on the curation set.

      findLimitsOfAligns(list, curationSet);

      //We came into this method because we had "partial" information: only one species' range.
      //We have established the other range with the findLimitsOfAligns call. We should set
      //this range on ourselves, so that the dna-dna align calls which follow use the same ranges.

      if(!areQueryAndHitInverted){
        setHitChromosome(curationSet.getChromosome());
        setHitStart(curationSet.getStart());
        setHitEnd(curationSet.getEnd());
      }else{
        setChromosome(curationSet.getChromosome());
        setStart(curationSet.getStart());
        setEnd(curationSet.getEnd());
      }
      
      ensJFeaturePairs = list.iterator();
      
      counter = 0;
      
      //Convert protein aligns into Apollo FeaturePairs.

      while(ensJFeaturePairs.hasNext()){
        ensJFeaturePair = (org.ensembl.datamodel.FeaturePair)ensJFeaturePairs.next();
        String chr = ((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName();
        String chr2 = curationSet.getChromosome();
        if (((Location)ensJFeaturePair.getHitLocation()).getSeqRegionName().equals(curationSet.getChromosome())) {
          apolloFeaturePair = 
            convertEnsJFeaturePairToApolloFeaturePair( ensJFeaturePair, "protein-protein-align", areQueryAndHitInverted);

          featureSet.addFeature(apolloFeaturePair,false);
          counter++;
        }
      }
      
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
  }
   
  private void findProteinAlignsForStableIdAndSpecies(
    String querySpecies,
    String[] stableIds,
    String hitSpecies,
    FeatureSetI featureSet,
    boolean areQueryAndHitInverted
  )throws apollo.dataadapter.ApolloAdapterException{
    org.ensembl.datamodel.FeaturePair ensJFeaturePair;
    Iterator ensJFeaturePairs;
    apollo.datamodel.FeaturePair apolloFeaturePair;
    int counter = 0;
    
    try{
      ComparaDriver comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      MemberAdaptor memberAdaptor = (MemberAdaptor)comparaDriver.getAdaptor(MemberAdaptor.TYPE);

      logger.debug("Doing stable id fetch");

      ensJFeaturePairs = memberAdaptor.fetch( querySpecies, stableIds, hitSpecies).iterator();

      //Convert protein aligns into Apollo FeaturePairs.

      while(ensJFeaturePairs.hasNext()){

        ensJFeaturePair = (org.ensembl.datamodel.FeaturePair)ensJFeaturePairs.next();
        apolloFeaturePair = 
           convertEnsJFeaturePairToApolloFeaturePair( ensJFeaturePair, "protein-protein-align", areQueryAndHitInverted);

        featureSet.addFeature(apolloFeaturePair,false);

        if(counter<=0){
          //ALWAYS use the hit-description - because the species input to this method
          //have been flipped around beforehand, if necessary.

          setStableId(ensJFeaturePair.getHitDescription());
          
          //If we have been passed in a stableid, then we can use it to set the range
          //of the read (with a 200K buffer either side): 
          //that means we can grab dna-dna aligns later using this range.

          if (!areQueryAndHitInverted){
            
            setChromosome(((Location)ensJFeaturePair.getLocation()).getSeqRegionName());
            
            if (ensJFeaturePair.getLocation().getStart() < ensJFeaturePair.getLocation().getEnd()){
              setStart( Math.max(ensJFeaturePair.getLocation().getStart() - BUFFER_SIZE, 1));
              setEnd(ensJFeaturePair.getLocation().getEnd()+BUFFER_SIZE);
            }else{
              setEnd( Math.max(ensJFeaturePair.getLocation().getStart() - BUFFER_SIZE, 1));
              setStart(ensJFeaturePair.getLocation().getEnd()+BUFFER_SIZE);
            }

            setHitChromosome(null);
            setHitStart(0);
            setHitEnd(0);

          } else {
            setHitChromosome(((Location)ensJFeaturePair.getLocation()).getSeqRegionName());
            
            if (ensJFeaturePair.getLocation().getStart() < ensJFeaturePair.getLocation().getEnd()){
              setHitStart( Math.max(ensJFeaturePair.getLocation().getStart() - BUFFER_SIZE, 1));
              setHitEnd(ensJFeaturePair.getLocation().getEnd()+BUFFER_SIZE);
            } else {
              setHitEnd( Math.max(ensJFeaturePair.getLocation().getStart() - BUFFER_SIZE, 1));
              setHitStart(ensJFeaturePair.getLocation().getEnd()+BUFFER_SIZE);
            }
            
            setChromosome(null);
            setStart(0);
            setEnd(0);

          }
        }
        counter++;
      }

    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }
  }
  
  /**
   * Sift through the aligns returned, establish a start/end,
   * and in the process make sure that all the aligns lie on a single 
   * chromosome. 
   * If we can't do that, pop up a dialogue and ask the user which
   * of the locations they want.
   * --boolean says to use query or hit-locations  
  **/
  private void findLimitsOfAligns( List aligns, CurationSet curationSet) throws apollo.dataadapter.ApolloAdapterException{

    HashMap locationHash = new HashMap(); //will store the locations (if more than one)
    Object[] hashElement; //will store {AssemblyLocation, Integer}
    AssemblyLocation currentAlignLimit = null;
    AssemblyLocation newAlignLimit = null;
    AssemblyLocation chosenAlignLimit = null;
    Integer currentAlignCount = null;
    
    int start = -1;
    int end = 0;
    String chromosome = null;
    org.ensembl.datamodel.FeaturePair align = null;
    Location location = null;

    for(int i=0; i<aligns.size(); i++){
      align = (org.ensembl.datamodel.FeaturePair) aligns.get(i);
      location = (Location)align.getHitLocation();
      chromosome = location.getSeqRegionName();

      if (!locationHash.containsKey(chromosome)){
        start = -1;
        end = 0;
        currentAlignCount = Integer.valueOf("0");
      } else {
        hashElement = (Object[])locationHash.get(chromosome);
        currentAlignLimit = (AssemblyLocation)hashElement[0];
        currentAlignCount = (Integer)hashElement[1];
        start = currentAlignLimit.getStart();
        end = currentAlignLimit.getEnd();
      }
        
      if(location.getStart() < start || start < 0){
        start = location.getStart();
      }

      if(location.getEnd() > end){
        end = location.getEnd();
      }
        
      newAlignLimit = new AssemblyLocation(chromosome, start, end, 0);
      locationHash.put(chromosome, new Object[]{newAlignLimit, new Integer(currentAlignCount.intValue()+1)});
    }

    if(locationHash.size() > 1){
      //Pop up a dialogue to fetch the chosen limit from the user.
      chosenAlignLimit = getUserChoice(locationHash);

    }else if (locationHash.size() == 1){
      chosenAlignLimit = ((AssemblyLocation) ((Object[])locationHash.values().iterator().next())[0]);
    }else{
      throw new IllegalStateException("there are no align limits in a place where I'm expecting at least one!");
    }
    
    start = chosenAlignLimit.getStart();
    end = chosenAlignLimit.getEnd();
    
    if(start < end){
      start = Math.max(start - BUFFER_SIZE, 1);
      end = end+BUFFER_SIZE;
    }else{
      start = start + BUFFER_SIZE;
      end = Math.max(end - BUFFER_SIZE, 1);
    }
    
    curationSet.setChromosome(chosenAlignLimit.getChromosome());
    curationSet.setStart(start);
    curationSet.setEnd(end);
  }//findLimitsOfAligns
  
  public AssemblyLocation getUserChoice(HashMap locationHash){
    Object[] userAlignChoice = AlignBlockChooser.getUserAlignChoice(locationHash);
    return (AssemblyLocation)userAlignChoice[0];
  }
  
  public void setStateInformation(Properties props) {
    String logicalSpecies;
    String actualSpecies;

    stateInformation = props;
    int stableIdIndex = -1;
    String stableId;
    
    setLoggingFile(props.getProperty("loggingFile"));
    
    setDriverConf(props);

    setStableId(null);
    setQueryStableId(null);
    setHitStableId(null);
    setChromosome(null);
    setCoordSystemString(null);
    setStart(-1);
    setEnd(-1);
    setHitChromosome(null);
    setHitCoordSystemString(null);
    setHitStart(-1);
    setHitEnd(-1);
    
    if(props.getProperty("chromosome") != null && props.getProperty("chromosome").trim().length()>0){
      setChromosome(props.getProperty("chromosome"));
    }
    if(props.getProperty("coordsystem") != null && props.getProperty("coordsystem").trim().length()>0){
      setCoordSystemString(props.getProperty("coordsystem"));
    }
    if(props.getProperty("start") != null && props.getProperty("start").trim().length()>0){
      setStart(Integer.valueOf(props.getProperty("start")).intValue());
    }
    if(props.getProperty("end") != null && props.getProperty("end").trim().length()>0){
      setEnd(Integer.valueOf(props.getProperty("end")).intValue());
    }
    
    //set the hit ranges, species names.

    logicalSpecies = props.getProperty("querySpecies");
    
    if(logicalSpecies == null || logicalSpecies.trim().length() <= 0){
      throw new NonFatalDataAdapterException(" ''querySpecies'' must be provided in the compara-adaptor's state information");
    }
    
    actualSpecies = convertLogicalNameToSpeciesName(logicalSpecies);
    
    setQuerySpecies(actualSpecies);
    
    logicalSpecies = props.getProperty("hitSpecies");
    
    if(logicalSpecies == null || logicalSpecies.trim().length() <= 0){
      throw new NonFatalDataAdapterException(" ''hitSpecies'' must be provided in the compara-adaptor's state information");
    }
    
    actualSpecies = convertLogicalNameToSpeciesName(logicalSpecies);
    setHitSpecies(actualSpecies);
    
    if(props.getProperty("hitChr") != null && props.getProperty("hitChr").trim().length()>0){
      setHitStart(Integer.valueOf(props.getProperty("hitStart")).intValue());
      setHitEnd(Integer.valueOf(props.getProperty("hitEnd")).intValue());
      setHitChromosome(props.getProperty("hitChr"));
      setHitCoordSystemString(props.getProperty("hitCoordSys"));
    }
    
    stableId = props.getProperty("stableId");
    
    if(stableId != null && 
       stableId.trim().length() >= EnsJAdapter.STABLE_ID_PREFIX.length()){

    //  stableId = stableId.substring(EnsJAdapter.STABLE_ID_PREFIX.length());
      if(!stableId.trim().equals("null")){
        setQueryStableId(stableId);
      }
    }
    
    
    stableId = props.getProperty("hitStableId");
    
    if(stableId != null && 
       stableId.trim().length() >= EnsJAdapter.STABLE_ID_PREFIX.length()){

     // stableId = stableId.substring(EnsJAdapter.STABLE_ID_PREFIX.length());
      if(!stableId.trim().equals("null")){
        setHitStableId(stableId);
      }
    }
    
    setDnaAligns(props.getProperty("dnaAligns"));
    setHighConservationDnaAligns(props.getProperty("highConservationDnaAligns"));
    setProteinAligns(props.getProperty("proteinAligns"));
    if (props.getProperty("dnaAlignTypes") != null) {
      setDnaAlignTypes(props.getProperty("dnaAlignTypes").split(","));
    } else {
      String [] types = new String[1];

      types[0] = "BLASTZ_NET";
      setDnaAlignTypes(types);
    }
  }//end setStateInformation

  public String getQuerySpecies(){
    return querySpecies;
  }
  
  public String getHitSpecies(){
    return hitSpecies;
  }
  
  public int getHitStart(){
    return hitStart;
  }
  
  public int getHitEnd(){
    return hitEnd;
  }

  public String getHitChromosome(){
    return hitChromosome;
  }

  public String getHitCoordSystemString(){
    return hitCoordSystem;
  }

  public void setQuerySpecies(String newValue){
    querySpecies = newValue;
  }
  
  public void setHitSpecies(String newValue){
    hitSpecies = newValue;
  }
  
  public void setHitStart(int newValue){
    hitStart = newValue;
  }
  
  public void setHitEnd(int newValue){
    hitEnd = newValue;
  }

  public void setHitChromosome(String newValue){
    hitChromosome = newValue;
  }

  public void setHitCoordSystemString(String newValue){
    hitCoordSystem = newValue;
  }
  
  public void setStart(int newValue){
    start = newValue;
  }
  
  public void setEnd(int newValue){
    end = newValue;
  }

  public void setChromosome(String newValue){
    chromosome = newValue;
  }
  
  public void setCoordSystemString(String newValue){
    coordsystem = newValue;
  }
  
  public int getStart(){
    return start;
  }
  
  public int getEnd(){
    return end;
  }

  public String getChromosome(){
    return chromosome;
  }

  public String getCoordSystemString(){
    return coordsystem;
  }
  
  /**
   * Use the synteny style to convert from logical to actual species names.
  **/
  public String convertLogicalNameToSpeciesName(String logicalName){
    HashMap speciesNames = 
      Config.getStyle("apollo.dataadapter.synteny.SyntenyAdapter").getSyntenySpeciesNames();
    
    Iterator logicalNames = speciesNames.keySet().iterator();
    int index;
    String longName;
    String shortName = null;
    
    while(logicalNames.hasNext()){
      longName = (String)logicalNames.next();
      
      //Convert Name.Human to Human

      index = longName.indexOf(".");
      shortName = longName.substring(index+1);
      
      if(shortName.equals(logicalName)){
        return (String)speciesNames.get(longName);
      }
    }
    
    if(true){
      throw new IllegalStateException("No logical species name matches the name input:"+shortName);
    }
      
    return null;
  }//end convertLogicalNameToSpeciesName
  
  public List fetchAllAlignFeatures(
   String speciesName,
   String chromosome,
   int start,
   int end,
   HashMap sequenceHash,
   String type
  ) throws apollo.dataadapter.ApolloAdapterException{
    ComparaDriver comparaDriver = null;
    DnaDnaAlignFeatureAdaptor dnaDnaAlignFeatureAdaptor = null;
    Iterator featurePairs = null;
    org.ensembl.datamodel.DnaDnaAlignFeature pair;
    apollo.datamodel.SeqFeatureI feature = null;
    apollo.datamodel.SeqFeatureI hitFeature = null;
    apollo.datamodel.FeaturePairI apolloPair = null;
    List fetchList = new ArrayList();
    List returnList = new ArrayList();
    FeatureSet set = null;
    HashMap setMap = new HashMap();
    
    try{
      comparaDriver = org.ensembl.compara.driver.ComparaDriverFactory.createComparaDriver(getStateInformation());

      dnaDnaAlignFeatureAdaptor = 
        (DnaDnaAlignFeatureAdaptor)comparaDriver.getAdaptor(DnaDnaAlignFeatureAdaptor.TYPE);

      //Fetch dna-dna aligns as ensj FeaturePairs.

      String chr = new String(chromosome.substring(chromosome.indexOf(":")+1));
      logger.debug("chr = " + chr);
      fetchList = dnaDnaAlignFeatureAdaptor.fetch(convertLogicalNameToSpeciesName(speciesName), 
                                                  new AssemblyLocation(chr, start, end, 0), 
                                                  null, 
                                                  type);
      
      featurePairs = fetchList.iterator();
      
      while(featurePairs.hasNext()){
        feature = new apollo.datamodel.SeqFeature();
        hitFeature = new apollo.datamodel.SeqFeature();
        
        pair = (org.ensembl.datamodel.DnaDnaAlignFeature)featurePairs.next();
        
        feature.setFeatureType(pair.getHitSpecies()+"-compara-"+type);
        //feature.setName( pair.getHitSpecies()+" ("+ ((Location)pair.getHitLocation()).getSeqRegionName()+")");
        feature.setName( pair.getHitSpecies()+ " ("+ pair.getDisplayName() +")");
        feature.setStart(pair.getLocation().getStart());
        feature.setEnd(pair.getLocation().getEnd());
        
        hitFeature.setFeatureType(pair.getHitSpecies()+"-compara-"+type);
        //hitFeature.setName( pair.getHitSpecies()+ " ("+((Location)pair.getHitLocation()).getSeqRegionName()+")");
        hitFeature.setName( pair.getHitSpecies()+ " ("+ pair.getDisplayName() +")");
        hitFeature.setStart(pair.getHitLocation().getStart());
        hitFeature.setEnd(pair.getHitLocation().getEnd());
        hitFeature.setRefSequence((SequenceI)sequenceHash.get(pair.getHitSpecies()));
        apolloPair = new apollo.datamodel.FeaturePair(feature, hitFeature);
        if (setMap.containsKey(feature.getName())) {
          set = (apollo.datamodel.FeatureSet)setMap.get(feature.getName());
        } else {
          set = new apollo.datamodel.FeatureSet();
          setMap.put(feature.getName(),set);
          returnList.add(set);
        }
        set.addFeature(apolloPair);
        set.setFeatureType(pair.getHitSpecies()+"-compara-"+type);
      }
      
    }catch(ConfigurationException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }catch(AdaptorException exception){
      throw new apollo.dataadapter.ApolloAdapterException(exception.getMessage());
    }

    return returnList;
  }
        
  private void setDnaAligns(String newValue){
    dnaAligns = newValue;
  }

  private void setDnaAlignTypes(String [] newValue){
    dnaAlignTypes = newValue;
  }
  
  private void setHighConservationDnaAligns(String newValue){
    highConservationDnaAligns = newValue;
  }
  
  private void setProteinAligns(String newValue){
    proteinAligns = newValue;
  }

  private String getDnaAligns(){
    return dnaAligns;
  }

  private String [] getDnaAlignTypes(){
    return dnaAlignTypes;
  }
  
  private String getHighConservationDnaAligns(){
    return highConservationDnaAligns;
  }
  
  private String getProteinAligns(){
    return proteinAligns;
  }
  
  public String getQueryStableId(){
    return queryStableId;
  }
  
  private void setQueryStableId(String id){
    queryStableId = id;
  }
  
  public String getHitStableId(){
    return hitStableId;
  }
  
  private void setHitStableId(String id){
    hitStableId = id;
  }
  
  private void setStableId(String id){
    stableId = id;
  }
  
  public String getStableId(){
    return stableId;
  }
}
