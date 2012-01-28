package apollo.datamodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import apollo.util.FeatureList;
import apollo.config.Config;

/** Holds multiple curation sets for synteny.
    Should this be called SyntenyDataHolder or 
    MultiSpeciesDataHolder? For composite curation sets just wraps it - 
    eventually replace CurationSets compositeness with this.  
    CompDataHolder holds a list of SpeciesComparisons 
    (that holds 2 cur sets and a link set)
    rename species stuff to curation stuff
    CompositeDataHolder is too vague. rename MultiCurationHolder, but it also holds 
    links
*/
public class CompositeDataHolder implements ApolloDataI {

  //private ArrayList curationSets = new ArrayList(3); // ??
  /** For now optionally wrapping a composite curation set */
  private CurationSet compositeCurationSet;
  private boolean hasLinks = false;

  /** List of SpeciesComparison objects */
  private List speciesComparisons = new ArrayList(1);
  private String name;

  public CompositeDataHolder() {
  }

  public CompositeDataHolder(SpeciesComparison sc) { 
    addSpeciesComparison(sc);
  }

  /** Puts composite curation sets into species comparisons */
  public CompositeDataHolder(CurationSet curSet) {
    if (curSet.isMultiSpecies()) 
      wrapCompositeCurationSet(curSet);
    else { // truly single species
      SpeciesComparison comparison = new SpeciesComparison();
      comparison.setCurationSet(0,Config.getDefaultSingleSpeciesName(),curSet);
      addSpeciesComparison(comparison);
    }
  }

  private void wrapCompositeCurationSet(CurationSet compositeCur) {
    this.compositeCurationSet = compositeCur;
    hasLinks = true;
    // make species list and species comparison list

    // SMJS These must come out in the correct, which they won't necessarily from
    //      the HashSet, so I added an ordered Vector of the child set names to CurationSet
    //
    //Iterator it = compositeCurationSet.getChildCurationSets().keySet().iterator();

    Iterator it = compositeCurationSet.getChildSetOrderedNames().iterator();

    String prevSpecies = null;
    while (it.hasNext()) {
      String species = (String)it.next();
      if (isComparaCurationSet(species))
	continue;
      //speciesList.add(species);
      if (prevSpecies != null) {
        CurationSet cs1 = getCurSetFromCompCurSet(prevSpecies);
        CurationSet cs2 = getCurSetFromCompCurSet(species);
        LinkSet ls = getCompCurLinkSet(prevSpecies,species);
        SpeciesComparison sc = new SpeciesComparison(prevSpecies,cs1,ls,species,cs2);
        addSpeciesComparison(sc);
      }
      prevSpecies = species;
    }
    name = compositeCur.getName();
  }

  // Would be nice to get rid of eventually and go all sequential if possible
  public CurationSet getCurationSet(String setName) {
    if (isCompCurSetWrapper())
      return compositeCurationSet.getCurationSet(setName);
    for (int i=0; i<speciesComparisons.size(); i++) {
      CurationSet cs = getSpeciesComparison(i).getCurationSet(setName);
      if (cs!=null) 
	return cs;
    }
    // this shouldnt happen
    throw new RuntimeException("No cur set for species "+setName);
  }

  private boolean isCompCurSetWrapper() {
    return compositeCurationSet != null;
  }

  /** Return ith species curation set as listed in species array list - this 
      excludes curation sets for links (if thats how links are represented) 
      rename this - needs to lose species association. getChildCurationSet? 
      getCurationSet? getSubCurationSet? */
  public CurationSet getCurationSet(int i) {
    if (compositeCurationSet!=null)
      return getCurSetFromCompCurSet(getSpecies(i));
    else {
      if (i==0)
      	return getSpeciesComparison(0).getCurationSet1();
      else
        return getSpeciesComparison(i-1).getCurationSet2();
    }
  }

  /** Number of curation sets for species - not links. This is equal to 
      the number of species comparisons or that number plus one depending if
      the last comparison has 2 species or one(not filled in yet). */
  public int numberOfSpecies() { 
    //return speciesList.size(); 
    int num = speciesComparisons.size();
    // If the last comp has both of its species need to add one
    if (getLastSpeciesComp().hasBothSpecies())
      ++num;
    return  num;
  }
  
  /**
   * I keep forgetting numberOfSpecies has no 'get' in front of it,
   * so I'm inserting this standard accessor for my own sanity.
  **/
  public int getNumberOfSpecies(){
    return numberOfSpecies();
  }

  public String getSpecies(int i) { 
    // getting the last species is different
    if (i == speciesComparisons.size()) 
      return getSpeciesComparison(i-1).getSpecies2();
    else
      return getSpeciesComparison(i).getSpecies1();
  }

  // should this be hasLinks(i)? do we need this method?
  public boolean hasNonEmptyLinkSet() {
    return getSpeciesComparison(0).hasNonEmptyLinkSet();
  }

  /** Return Link set linking 2 species - this should go away*/
  private LinkSet getCompCurLinkSet(String firstSetName, 
				    String secondSetName) {
    LinkSet linkSet;// = getLinkSetFromCache(firstSetName,secondSetName);
    //if (linkSet != null) return linkSet;
    CurationSet cs = getCurSetFromCompCurSet(firstSetName+"-"+secondSetName);
    if (cs == null) 
      cs = getCurSetFromCompCurSet(secondSetName+"-"+firstSetName);
    /* Even if style file listed the link as second-first it is important
       to record it in the link set as first and second as that is query
       and hit and how the links are setup */
    linkSet = new LinkSet(cs,firstSetName,secondSetName);
    //linkSet.setCurationsFromCompositeCuration(compositeCurationSet);
    return linkSet;
  }
  // phase out
  public CurationSet getCurSetFromCompCurSet(String setName) {
    return compositeCurationSet.getCurationSet(setName);
  }
  public int getNumberOfLinkSets() {
    return speciesCompSize();
  }
  public LinkSet getLinkSet(int i) {
    return getSpeciesComparison(i).getLinkSet();
  }
  public void addSpeciesComparison(SpeciesComparison sc) { 
    speciesComparisons.add(sc); 
  }
  public int speciesCompSize() {
    return speciesComparisons.size();
  }
  public SpeciesComparison getSpeciesComparison(int i) {
    return (SpeciesComparison)speciesComparisons.get(i);
  }
  private SpeciesComparison getLastSpeciesComp() {
    return getSpeciesComparison(speciesComparisons.size()-1);
  }

  /** Should be phased out */
  public CurationSet getCompositeCurationSet() {
    return compositeCurationSet;
  }

  // for composite cur sets - awkward - phase out
  private boolean isComparaCurationSet(String name) { 
    HashMap logicalNamesAndSpecies =
      apollo.config.Config.getStyle("apollo.dataadapter.synteny.SyntenyAdapter")
      .getSyntenySpeciesNames();
    String speciesName = (String)logicalNamesAndSpecies.get("Name."+name);
    return speciesName != null && speciesName.equals("Compara");
  }

  /** ApolloDataI - we are not a curation set so returning false. This allows
      one to query an ApolloDataI to find out if its a curation set or a composite
      data holder. */
  public boolean isCurationSet() {
    return false;
  }
  
  /** ApolloDataI method. if isCurationSet returns true than a real curation
      set would be returned here. Since this is not a curation set we return
      false. This method should be used after testing isCurationSet()==true,
      so in other words this method should never be called on a composite data
      holder, only on a curation set. */
  public CurationSet getCurationSet() {
    return null;
  }
  public boolean isCompositeDataHolder() {
    return true;
  }
  public CompositeDataHolder getCompositeDataHolder() {
    return this;
  }
  /** Clean up dangling refs */
//  public void cleanup() {
//    if (compositeCurationSet!=null)
//      compositeCurationSet.cleanup();
//    for (int i=0; i<numberOfSpecies(); i++)
//      getCurationSet(i).cleanup();
    // LinkSets?
//  }
  
  /** 
   * If we have more than one species, it answers 'true', otherwise not.
  **/
  public boolean isMultiSpecies() { 
    return getNumberOfSpecies() > 1; 
  }

  public String getName() { 
    if (name == null)
      name = getCurationSet(0).getName();
    return name;
  }
}
