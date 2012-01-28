package apollo.datamodel;

import java.util.*;

import apollo.util.FeatureList;
//import apollo.editor.AnnotationChangeLog;
import apollo.editor.TransactionManager;

import org.apache.log4j.*;

/**
 * <p>I am the central holder of renderable data - results, annotations etc. 
 * I am a composite - I can contain instances of myself. In this case, these
 * "child" curation sets can be retrieved by name. This is the way
 * that the species-specific (and compara-) curation sets are passed around for the
 * Synteny case. </p>
 *
 * <p>In the case that I have no children, my get... methods 
 * will act on the (only) Results, Annotations etc that I have. If I _do_
 * have children, then the get... methods will blow up when invoked. </p>

 My feeling is that CurationSet should not be composite, 
 but just be for one species.
 There should be a new object for the multi-species scene: 
   MultiSpeciesDataHolder or something like that. It would hold CurationSets
   and LinkSets. This would mean changing DataLoader to be able to return 
   more than just CurationSets.
**/
public class CurationSet extends GenomicRange implements java.io.Serializable, 
  ApolloDataI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------
  
  private static final Logger      logger = LogManager.getLogger(CurationSet.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //the children
  private HashMap childCurationSets = new HashMap();

  private Vector childSetOrderedNames = new Vector();
  
  // The result features
  private StrandedFeatureSetI results;

  // the annotations
  private StrandedFeatureSetI annots;

  protected Vector sequence_vect = new Vector ();
  protected Hashtable sequence_hash = new Hashtable();

  private String assemblyType = null;

  // should this just be a List of Transactions?? thats all an annot change log
  // is anyways
  //private AnnotationChangeLog annotationChangeLog;
  // To catch Transactions
  private TransactionManager tranManager;

  // Remember where input came from
  private String inputFile = null;

  public CurationSet() {}

  public CurationSet(StrandedFeatureSetI results,
                     StrandedFeatureSetI annots,
                     String              region) {
    setName(region);
    this.results = results;
    this.annots = annots;
  }

  public HashMap getChildCurationSets(){
    return childCurationSets;
  }//end getChildCurationSets

  public void setChildSetOrderedNames(Vector setNames) {
    childSetOrderedNames = setNames;
  }
  public Vector getChildSetOrderedNames() {
    return childSetOrderedNames;
  }

  
  public void addCurationSet(Object key, CurationSet curationSet){
    getChildCurationSets().put(key, curationSet);
  }//end addCurationSet
  
  public CurationSet getCurationSet(Object key){
    return (CurationSet) getChildCurationSets().get(key);
  }//end getCurationSet
  
  private void throwExceptionIfTrueComposite(){
    if(getChildCurationSets().size()>0){
      throw new IllegalStateException("Attempt to access a true composite curation set as if it were a leaf");
    }//end if
  }//end throwExceptionIfTrueComposite
  
  /* 
    This was assuming that the range (i.e. the start & end)
    were always set prior to setting the reference sequence,
    but this is not always the case. It is a bit of a chicken
    and egg problem. If you do have the sequence length (from
    an analysis program or a fasta file) you don't want that
    to be destroyed by simply calling setRefSequence on a 
    curation that has not yet had its range set
  */
  public void setRefSequence (SequenceI seq) {
    throwExceptionIfTrueComposite();
    
    if (seq == null) {
      logger.warn("setRefSequence: ref seq is null!");
      return;
    }
    super.setRefSequence (seq);
    // no longer diddle silently with the seq length
    // but minds its own business
    seq.setRange (this);
  }

  /** A CurationSet has a dangling reference that needs to be cleaned up when
      a new curation set is loaded to replace this one, otherwise it will 
      persist as a memory leak. The dangling reference is that if its ref seq
      is an AbstractLazySequence, ALS's LazyLoadControlledObject is a listener
      to the Controller. ALS.LLCO has to get a signal to remove itself as a
      listener when a new curation set is loaded and this is pase, 
      thus this method. */
//  public void cleanup() {
    //throwExceptionIfTrueComposite();
//    if (isMultiSpecies()) {
//      Iterator sets = getChildCurationSets().values().iterator();
//      while(sets.hasNext()) ((CurationSet)sets.next()).cleanup();
//    }
    //no longer needed
//    else if (getRefSequence()!=null) { // Single species
//      getRefSequence().cleanup();
//    }
//  }

  public StrandedFeatureSetI getAnnots() {
    throwExceptionIfTrueComposite();
    
    return annots;
  }

  public StrandedFeatureSetI getResults() {
    throwExceptionIfTrueComposite();
    
    return results;
  }

  public void setResults(StrandedFeatureSetI results) {
    throwExceptionIfTrueComposite();
    if (results == null) {
      logger.info("CurationSet.setResults: results was null--creating new results set.");
      results = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());
    }
    this.results = results;
    if (this.getRefSequence() != null && results.getRefSequence() == null)
      results.setRefSequence(this.getRefSequence());
  }

  public String getAssemblyType() {
    throwExceptionIfTrueComposite();
    
    return assemblyType;
  }

  public void setAssemblyType(String type) {
    throwExceptionIfTrueComposite();
    
    this.assemblyType = type;
  }

  public String getInputFilename() {
    return inputFile;
  }

  public void setInputFilename(String file) {
    this.inputFile = file;
  }

  public void setAnnots(StrandedFeatureSetI annots) {
    throwExceptionIfTrueComposite();    
    this.annots = annots;
    if (this.getRefSequence() != null && annots.getRefSequence() == null)
      annots.setRefSequence(this.getRefSequence());
  }

  public SequenceI addSequence (SequenceI seq) {
    throwExceptionIfTrueComposite();
    
    if (!sequence_vect.contains (seq) &&
	sequence_hash.get(seq.getName()) == null ) {
      sequence_vect.addElement (seq);
      sequence_hash.put (seq.getName(), seq);
    }
    return ( (SequenceI) sequence_hash.get (seq.getName()) );
  }

  public Vector getSequences () {
    throwExceptionIfTrueComposite();    
    
    return sequence_vect;
  }

  public SequenceI getSequence (String id) {
    throwExceptionIfTrueComposite();

    if (sequence_hash == null || id == null)
      return null;  // Should we complain?

    return (SequenceI) sequence_hash.get (id);
  }

  public void removeSequence (SequenceI seq) {
    throwExceptionIfTrueComposite();
    sequence_vect.removeElement (seq);
    sequence_hash.remove (seq.getName());
  }

  /**
   * <p> In the single-curation set case, this toString method is good for 
   * GAME fly data, but    * for Ensembl data, the range already has all
   * the info, so the title ends up something like
   * "2 from Chr 2 200000 4000000:20000-4000000" </p>
   *
   * <p> For a composite case, 
   * the toString is the concatentation of all toString's
   * on the children nodes.</p>
  **/
  public String toString () {
    StringBuffer returnBuffer = new StringBuffer();
    Set keySet;
    Iterator childNames;
    String displayId;
    
    if (getChildCurationSets().size() > 0) {
      keySet = getChildCurationSets().keySet();
      childNames = keySet.iterator();
      while(childNames.hasNext()) {
        returnBuffer.append(getCurationSet(childNames.next().toString()));
      }
    }
    else {
      displayId = getName();

      if (getStart() > 0) {
        if ( getRefSequence() != null &&
	     !getRefSequence().getName().equals (getName())) {
          String range = (getRefSequence().getName() + ":" +
			  getStart() + "-" + getEnd());
          
          if (!(range.equals(displayId))) {
            displayId = displayId + " from " + range;
          }
        }
      }
      returnBuffer.append(displayId);
    }
    return returnBuffer.toString();
  }

  /**
   * General implementation of Visitor pattern. (see apollo.util.Visitor).
  **/
  public void accept(apollo.util.Visitor visitor){
    visitor.visit(this);
  }//end accept

  /** Overrides Range.contains. The only difference is that CurationSet
      does not consider strand in its contains method, where Range does.
      CurationSets are usually forward stranded (since there start is less than 
      their start - see Range constructor), but they hold reverse as well as 
      forwards stranded features. Without this override all of the 
      reverse stranded features fail the contains test with their CurationSet.
  */
  public boolean contains(RangeI sf) {
    if (overlaps(sf)             &&
        getLeftOverlap(sf)  <= 0 && 
        getRightOverlap(sf) <= 0 ) { 
      return true;
    } else {
      return false;
    }
  }
    
  /** Overrides Range.overlaps. Took out strand comparison. 
      @see contains
  */
  public boolean     overlaps(RangeI sf) {
    return (getLow()    <= sf.getHigh() &&
            getHigh()   >= sf.getLow());
  }
  
  /** ApolloDataI */
  public boolean isCurationSet() { return true; }
  public CurationSet getCurationSet() { return this; }
  public boolean isCompositeDataHolder() { return false; }
  public CompositeDataHolder getCompositeDataHolder() {
    if (isMultiSpecies())
      return new CompositeDataHolder(this);
    else
      return null;
  }
  /** Temporary method until all synteny stuff uses CompositeDataHolder
      rename isComposite? */
  public boolean isMultiSpecies() {
    return getChildCurationSets().size() > 0;
  }

  /** Returns a FeatureList for both strands of an analysis, 
      the first item in list is forward strand, 2nd is rev strand.
      analysisName is what is called resulttype in the tiers file - 
      its the name for the analysis in the data itself - 
      not the name of the tier that it maps to in the gui (via the tiers file)
  */
  public FeatureList getAnalysisFeatureList(String analysisName) {
    SeqFeatureI forw = getAnalysisFeatureSet(getResults().getForwardSet(), 
                                             analysisName);
    if (forw == null) {
      forw = getAnalysisFeatureSet(getAnnots().getForwardSet(),
                                   analysisName);
    }
    SeqFeatureI rev = getAnalysisFeatureSet(getResults().getReverseSet(),
                                            analysisName);
    if (rev == null) {
      rev = getAnalysisFeatureSet(getAnnots().getReverseSet(),analysisName);
    }
    // cant use StrandedFeatureSet -
    // it sets its childrens ref feature - basically
    // alters the datamodel - yikes - FeatureList with 2 FeatureSets?
    // return new StrandedFeatureSet(forFeatSet,rev);
    FeatureList fl = new FeatureList();
    fl.addFeature(forw);
    fl.addFeature(rev);
    return fl;
  }

  /** recursive helper function for getAnalysisStrandedFeatureSet */
  private SeqFeatureI getAnalysisFeatureSet(SeqFeatureI seqFeat, 
                                            String analysisName) {
    for (int i=0; i < seqFeat.getNumberOfChildren(); i++) {
      SeqFeatureI child = seqFeat.getFeatureAt(i);
      if (child.getTopLevelType() == RangeI.NO_TYPE) { // if no type keep recursing
        SeqFeatureI fs = getAnalysisFeatureSet(child,analysisName);
        if (fs != null) { 
          return fs;
        }
      } 
      else if (child.getTopLevelType().equals(analysisName)) {
        logger.debug("found feat with type "+analysisName+" child "+
                     child+" chil top lev type "+child.getTopLevelType());
        return child;
      } 
    }
    return null; // none found
  }

//   /** May change this to just a list of Transactions - not sure. Actually all
//       AnnotationChangeLog really is is a list of Transactions */
//   public void setAnnotationChangeLog(AnnotationChangeLog acl) {
//     this.annotationChangeLog = acl;
//   }
//   public AnnotationChangeLog getAnnotationChangeLog() {
//     return annotationChangeLog;
//   }
  public boolean hasTransactions() {
    //return annotationChangeLog != null && annotationChangeLog.hasTransactions();
    return tranManager != null && tranManager.hasTransactions();
  }

  public boolean hasTransactionManager() {
    return tranManager != null;
  }

  /**
   * @return Returns the tranManager.
   */
  public TransactionManager getTransactionManager() {
    return tranManager;
  }
  /**
   * @param tranManager The tranManager to set.
   */
  public void setTransactionManager(TransactionManager tranManager) {
    this.tranManager = tranManager;
  }
  
  /**
   * A rather strange implemenation. This method is based on 
   * GAMESave.writeGenomePosition(CurationSet).
   */
  public boolean isChromosomeArmUsed() {
    SequenceI genomic_seq = getRefSequence();
    return (genomic_seq != null && 
            genomic_seq.getName() != null &&
            !getName().equals(genomic_seq.getName()));
  }
}
