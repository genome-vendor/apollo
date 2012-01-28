package apollo.util;

import java.util.*;

import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;

import org.apache.log4j.*;

/** Just a little helper class. Holds an array list of SeqFeatureIs,
 * add more list methods as needed.
 * This should be replaced by FeatureSet once FeatureSet is refactored,
 * right now FeatureSet is just too heavyweight for such a lightweight
 * activity. Once FS is refactored this should be deleted/incorporated.
 */

public class FeatureList extends ArrayList {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureList.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //private Vector featureVector;
  //private boolean vectorWrapper=false;
  private boolean featurePairsOnly=false;
  private boolean alignablesOnly=false;
  /** if unique check is true, check that feat doesnt already exist on adding
   * true by default*/
  private boolean doUniqueCheck=true;

  public FeatureList() {
    //vectorWrapper = false;
  }

  public FeatureList(int size) {
    super(size);
    //vectorWrapper = false;
  }

  public FeatureList(Vector features) {
    //vectorWrapper = true;
    //featureVector = features; // or could just dump vector into featList?
    addAll(features);
  }

  /** if unique check is true, check that feat doesnt already exist on adding */
  public FeatureList(boolean doUniqueCheck) {
    setDoUniqueCheck(doUniqueCheck);
  }

  /** Create FeatureList from seqFeat. If addLeaves is true make feat list of all 
      leaves(descendants) of seqFeat, if addLeaves is false make a list of just the 
      seqFeat itself */
  public FeatureList(SeqFeatureI seqFeat, boolean addLeaves) {
    if (!addLeaves) {
      addFeature(seqFeat);
    }
    else {
      FeatureList fl = new FeatureList();
      fl.addFeature(seqFeat);
      addAllFeatures(fl.getAllLeaves()); // is there a better way to do this?
    }
  }

  // not used - but i could see this being reinstated with different signature
//   public FeatureList(boolean featurePairsOnly) {
//     this.featurePairsOnly = featurePairsOnly;
//   }

  /** Adds SeqFeatureI if not already in list */
  public void addFeature(SeqFeatureI feat) {
    add(feat);
  }

  public void setDoUniqueCheck(boolean doUniqueCheck) {
    this.doUniqueCheck = doUniqueCheck;
  }

  /** Adds all feats that are new, that it doesnt already have. If we need to do addAll that
      doesnt guarantee uniqueness we can paramaterize */
  public void addAll(FeatureList feats) {
    for (int i=0; i<feats.size(); i++)
      addFeature(feats.getFeature(i)); // checks for contains
  }

  public void removeFeature(SeqFeatureI feat) {
    remove(feat);
    //if (vectorWrapper) featureVector.remove(feat);
  }
  
  private void error(String m) { throw new RuntimeException(m); }

  /** addFeature should be used in lieu of add. This is here as an override to 
      ArrayList.add. cant call addFeature as it calls add. Ive added the constraint that
      it only adds feature if doesnt already have, in other words cant have redundant
      features. I think this is always how FeatureLists are used. If not, this can be
      parameterized. */
  public boolean add(Object o) {
    if (o == null) 
      return false;
    boolean added = false;
    if (o instanceof SeqFeatureI) {
      SeqFeatureI sf = (SeqFeatureI) o;
      if (featurePairsOnly && ! (sf instanceof FeaturePairI)) 
        return false;
      if (alignablesOnly && !sf.hasAlignable()) {
        error("tried to add non alignable to alignable only feat list");
      }
      if (doUniqueCheck && contains(sf)) { // guarantee uniqueness
        return false; // debug msg?
      }
      //if (!vectorWrapper) {
      added = super.add(o);
      //}
      //else { added = featureVector.add(o); }
    }
    else {
      logger.error("FeatureList error, adding non SeqFeatureI "+o);
    }
    return added;
  }
//   public Object get(int index) {
//     if (!vectorWrapper)
//       return super.get(index);
//     else
//       return featureVector.get(index);
//   }
//   public int size() {
//     if (!vectorWrapper)
//       return super.size();
//     else
//       return featureVector.size();
//   }

  public FeatureIterator featureIterator() {
    return new FeatureIterator(this);
  }
  public SeqFeatureI getFeature(int index) {
    if (index >= size() || index < 0)
      return null;
    else
      return (SeqFeatureI) get(index);
  }

  public SeqFeatureI last() {
    if (isEmpty()) return null;
    return getFeature(size()-1);
  }

  public SeqFeatureI first() { return getFeature(0); }

  public void addAllFeatures(FeatureList feats) {
    addAll(feats);
    // vector wrapper...
  }

  /** Returns unique list of parents */
  public FeatureList getParents() {
    FeatureList parents = new FeatureList();
    for (int i=0; i<size(); i++) {
      parents.addFeature(getFeature(i).getRefFeature());
    }
    return parents;
  }

  /** Returns a FeatureList that contains seq feature clones of all features in
      current list. */
  public FeatureList cloneList() {
    FeatureList clonedList = new FeatureList();
    for (int i=0; i<size(); i++)
      clonedList.addFeature(getFeature(i).cloneFeature());
    return clonedList;
  }

  public Vector toVector() {
//     if (vectorWrapper)
//       return featureVector;
    Vector v = new Vector();
    for (int i = 0; i < size(); i++) {
      v.addElement (get(i));
    }
    return v;
  }

  public void addVector(Vector v) {
    for (int i = 0; i < v.size(); i++) {
      add (v.elementAt(i));
    }
  }

  /** Assumes feats on same strand */
  public RangeI getRangeOfWholeList() {
    if (size()==0) return null;
    int low = getFeature(0).getLow();
    int high = getFeature(0).getHigh();
    for (int i=1; i<size(); i++) {
      SeqFeatureI feat = getFeature(i);
      if (feat.getLow() < low) low = feat.getLow();
      if (feat.getHigh() > high) high = feat.getHigh();
    }
    if (getFeature(0).getStrand() == 1) // isForwardStrand
      return new Range(RangeI.NO_NAME,low,high);
    else // rev strand high is start, low is end
      return new Range(RangeI.NO_NAME,high,low);
  }


  /** Returns a list of all the descendants of the current list
      that are leaves.
      Leaves are SeqFeatures with no children (eg Exons, FeaturePairs) */
  public FeatureList getAllLeaves() { 
    return getAllLeaves(false);
  }

  public FeatureList getAllFeaturePairLeaves() {
    return getAllLeaves(true);
  }
  private FeatureList getAllLeaves(boolean justFeaturePairs) { 
    FeatureList leaves = new FeatureList();
    leaves.setFeaturePairsOnly(justFeaturePairs);
    for (FeatureIterator iter = featureIterator(); iter.hasNext(); ) {
      SeqFeatureI sf = iter.nextFeature();
      if (!sf.canHaveChildren()) {
        if (!justFeaturePairs || sf.hasHitFeature())
          leaves.addFeature(sf);
      }
      else {
        FeatureList kids = new FeatureList(((FeatureSetI)sf).getFeatures());
        leaves.addAll(kids.getAllLeaves());
      }
    }
    return leaves;
  }

    

  /** Filters FeatureList for features with hasAlignable==true and
      the Alignable hasAlignmentSequence */
  public FeatureList getFeatsWithAlignments() {
    FeatureList alignFeats = new FeatureList();
    for (FeatureIterator iter = featureIterator(); iter.hasNext(); ) {
      SeqFeatureI feat = iter.nextFeature();
      if (feat.hasAlignable()) {
        alignFeats.addFeature(feat);
      }
    }
    return alignFeats;
  }

  public FeatureList getFeatsWithinRefSeqRegion() {
    FeatureList containedFeats = new FeatureList();
    for (FeatureIterator iter = featureIterator(); iter.hasNext(); ) {
      SeqFeatureI feat = iter.nextFeature();
      if (feat.isContainedByRefSeq())
        containedFeats.addFeature(feat);
    }
    return containedFeats;
  }

  /** Consolidates features. eg if all exons in a transcript are in the
      feature list, the transcript is added and the exons are removed.
      Presently this is not recursive, only does one level of processing
      as that is all that is needed, change if need be.
      This stops at "holders". In other words if we have selected all the
      genes on a strand it will not consolidate to the FeatureSet that is
      the holder of all those genes.
      @see #FeatureSetI.isHolder
  */
  public FeatureList getConsolidatedFeatures() {
    FeatureList consolidatedFeatures = new FeatureList(size());
    HashMap parentToNumberOfKids = new HashMap();
    for (int i=0; i<size(); i++) {
      SeqFeatureI child = getFeature(i);
      //FeatureSetI parent = child.getParent(); // SeqFeatureI?
      SeqFeatureI parent = child.getParent(); // SeqFeatureI?
      // codons have null parents
      // isHolder says "i hold stuff, but am not actually drawn", not a real
      // feature, but more a holder of features, so no need to consolidate these.
      // isHolder was commented out and nothing replaced it. this caused an obscure
      // bug that if there was only one gene it would consolidate to the gene holder
      //if (parent == null) { // || parent.isHolder()) {
      if (isTopLevel(child)) {
        consolidatedFeatures.add(child);
        continue;
      }
      Integer numKids = new Integer(1);
      if (parentToNumberOfKids.containsKey(parent)) {
        numKids = (Integer)parentToNumberOfKids.get(parent);
        // need to be removed?
        numKids = new Integer(numKids.intValue() + 1);
      }
      parentToNumberOfKids.put(parent,numKids);

      // if we have all of the parent's kids, remove kids and add parent
      if (numKids.intValue() == parent.getNumberOfChildren()) {
        Vector kids = parent.getFeatures();
        for (int j=0; j<kids.size(); j++)
          consolidatedFeatures.remove(kids.get(j));
        consolidatedFeatures.add(parent);
      } else {
        // add for now - will get removed if parent added later
        // if already have parent(messy selection) then dont add child
        if (!consolidatedFeatures.contains(parent))
          consolidatedFeatures.add(child);
      }
    }
    return consolidatedFeatures;
  }
  
  private boolean isTopLevel(SeqFeatureI feat) {
    if (feat.getParent() == null) // i dont think this is possible
      return true;
    // this is a cop out. only know top level for annots. turns out for results
    // there isnt a situation that will get us in trouble - hopefully
    if (!feat.hasAnnotatedFeature())
      return true;
    // Returns true if gene or other top level annot (false for exon,transcript)
    return feat.getAnnotatedFeature().isAnnotTop();
  }

  /** this is misnamed. Its sorting by low not start */
  public void sortByStart() {
    int start[] = new int[size()];
    //SeqFeatureI obj[] = new SeqFeatureI[size()];
    Object obj[] = new Object[size()];

    for (int i = 0 ; i < size();i++) {
      //start[i] = getFeature(i).getLow();
      start[i] = getLow(i);
      //obj[i]   = getFeature(i);
      obj[i]   = get(i);
    }

    QuickSort.sort(start,obj);

    for (int i = 0; i < obj.length; i++) {
      set(i,obj[i]);
    }
  }

  private int getLow(int i) {
    // This is where it would be nice if they 
    // both implemented some common interface
    // like RangeI, but RangeI has a bunch of 
    // stuff that doesnt concern Alignables
    /* the above reasoning makes no sense at all. clearly an
       alignable is a range, whether it uses everything is irrelevant */
    if (alignablesOnly) 
      return getAlignable(i).getLow();
    else
      return getFeature(i).getLow();
  }
  
  // Dont do this - if you add a feature to a feature set it will abandon the
  // feature set it was previously a part of changing the datamodel!
//   public apollo.datamodel.FeatureSet makeFeatureSet() {
//     apollo.datamodel.FeatureSet fs = new apollo.datamodel.FeatureSet();
//     for (int i=0; i<size(); i++) fs.addFeature(getFeature(i));
//     return fs;
//   }
  
  /** We could just have a FeaturePairList? */
  public void setFeaturePairsOnly(boolean featPairOnly) {
    this.featurePairsOnly = featPairOnly;
  }
  public void addFeaturePair(FeaturePairI featPair) {
    addFeature(featPair);
  }

  public FeatureList filterForFeaturePairs() {
    FeatureList featPairs = new FeatureList();
    featPairs.setFeaturePairsOnly(true);
    for (int i = 0;i<size(); i++) 
      featPairs.addFeature(getFeature(i)); // wont be added if not feature pair
    return featPairs;
  }
  public FeaturePairI getFeaturePair(int i) {
    if (!featurePairsOnly) return null; // is that harsh?
    return (FeaturePairI)getFeature(i);
  }

  public String[] getUniqueHitNames() {
    if (!featurePairsOnly) return null;
    Set names = new HashSet();
    for (int i=0; i<size(); i++) 
      names.add(getFeaturePair(i).getHitFeature().getName());
    // Should we just return the Set?
    return (String[])names.toArray(new String[0]);
  }

  // This doesn't seem to be called anywhere
  public void setAlignablesOnly(boolean alignablesOnly) {
    this.alignablesOnly = alignablesOnly;
  }
  
  public void addAlignable(SeqFeatureI alignable) {
    if (alignable == null)
      return;
    if (!contains(alignable))
      add(alignable);
  }
  public SeqFeatureI getAlignable(int index) { 
    if (!alignablesOnly) {
      error("Calling FeatList.getAlignable(i) on feat list thats not alignables");
      //return null; // too harsh? exception?
    }
    return (SeqFeatureI)get(index);
  }

  /** not recursive - just gets first feature with name - null if none */
  public SeqFeatureI getFeatWithName(String name) {
    for (int i=0; i<size(); i++) {
      if (getFeature(i).getName().equals(name))
        return getFeature(i);
    }
    return null;
  }
}
