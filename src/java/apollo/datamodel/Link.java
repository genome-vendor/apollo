package apollo.datamodel;

public class Link {

  /** link1Feature goes with top species - gets drawn on top */
  private SeqFeatureI link1Feature;
  /** bottom species */
  private SeqFeatureI link2Feature;
  private SeqFeatureI species1Feature;
  /** Do we need species2 feature - will there ever be a need for 2 separate 
      species feats? */
  private SeqFeatureI species2Feature;
  private boolean hasSpeciesFeatures = false;
  private boolean hasPercentIdentity = true;
  private boolean typeIsFeat1 = true;

//   /** hasSpeciesFeatures defaults to true */
//   public Link(SeqFeatureI link1Feature, SeqFeatureI link2Feature) {
//     this(link1Feature,link2Feature,true);
//   }
  
  public Link(SeqFeatureI link1Feature, SeqFeatureI link2Feature, boolean hasSpec, 
              boolean hasPercentIdentity) {
    this.link1Feature = link1Feature;
    this.link2Feature = link2Feature;
    hasSpeciesFeatures = hasSpec;
    this.hasPercentIdentity = hasPercentIdentity;
  }

  public Link(FeaturePairI wrappedFeatPair) {
    this.link1Feature = wrappedFeatPair.getQueryFeature();
    this.link2Feature = wrappedFeatPair.getHitFeature();
    // at least at the moment feature pair links are not of real species feats
    // if this changes this will need to be passed in
    hasSpeciesFeatures = false; 
  }

  /** set species1 feature if different than link1 feature (often they are the same) */
  public void setSpeciesFeature1(SeqFeatureI species1) {
    species1Feature = species1;
  }
  public void setSpeciesFeature2(SeqFeatureI species2) {
    species2Feature = species2;
  }

  /** If state is true use species feature 1 for type, false use spec feat 2 
      link type is used by gui for color/vis */
  public void setTypeIsFeat1(boolean state) {
    typeIsFeat1 = state;
  }

  /** return null if no type */
  public String getType() { 
    if (typeIsFeat1 && getSpeciesFeature1() != null) {
      return getSpeciesFeature1().getFeatureType();
    }
    if (getSpeciesFeature2() != null)
      return getSpeciesFeature2().getFeatureType(); 
    return null;
  }

  public String getName1() {
    return link1Feature.getName();
  }
  public String getName2() {
    return link2Feature.getName();
  }

  /** Returns true if query and hit have names */
  public boolean hasNames()  {
    if (getName1() == null) return false;
    if (getName1().equals(Range.NO_NAME)) return false;
    if (getName2() == null) return false;
    if (getName2().equals(Range.NO_NAME)) return false;
    return true;
  }


  boolean hasName(String name) {
    // toss no_name?
    if (getName1().equals(name)) {
      return true;
    }
    if (getName2().equals(name)) {
      return true;
    }
    return false;
  }

  public int getLow1() { return link1Feature.getLow(); }
  public int getHigh1() { return link1Feature.getHigh(); }
  public int getLow2() { return link2Feature.getLow(); }
  public int getHigh2() { return link2Feature.getHigh(); }
  public int getStrand1() { return link1Feature.getStrand(); }
  public boolean isForwardStrand1() { return getStrand1() == 1; }
  public int getStrand2() { return link2Feature.getStrand(); }
  public boolean isForwardStrand2() { return getStrand2() == 1; }
  //public double getScore() { return link1Feature.getScore(); }
  public double getPercentIdentity() { 
    if (!hasPercentIdentity()) { // shouldnt happen
      return 0;
    }
    return getSpeciesFeature1().getScore(); // what if score with species2??
  }
  /** Return false if species feat has no score or if its score is no perc id */
  public boolean hasPercentIdentity() {
    return hasPercentIdentity;
  }

  public boolean hasSpeciesFeatures() { return hasSpeciesFeatures; }

  public SeqFeatureI getSpeciesFeature1() {
// SMJS Seemed a little odd to not allow the link1Feature to be returned
//      and breaks ensj synteny
//    if (!hasSpeciesFeatures()) { // this shouldnt be called in this case
//      return null;  // there is no species feat
//    }
    if (species1Feature == null) { // link1Feature and species1Feature are same
      return link1Feature;
    }
    return species1Feature; // species1Feat and link1Feat are different
  }
  public SeqFeatureI getSpeciesFeature2() {
// SMJS Seemed a little odd to not allow the link1Feature to be returned
//      and breaks ensj synteny
//    if (!hasSpeciesFeatures()) { // this shouldnt be called in this case
//      return null;  // there is no species feat
//    }
    if (species2Feature == null) { // link1Feature and species1Feature are same
      return link2Feature;
    }
    return species2Feature; // species1Feat and link1Feat are different
  }

  /** If feat is not part of link - neither species1 nor species2 feature - then
      return null. If part of link return the feature it links to */
  public SeqFeatureI getLinkedSpeciesFeature(SeqFeatureI feat) {
    if (getSpeciesFeature1() == feat) {
      return getSpeciesFeature2();
    }
    if (getSpeciesFeature2() == feat) {
      return getSpeciesFeature1();
    }
    return null; // feat is not part of link
  }
}
