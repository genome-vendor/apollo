package apollo.datamodel;

import java.util.Vector;

import java.util.ArrayList;

import apollo.util.FeatureList;

/** Set of links for synteny view */

public class LinkSet {
  
  /**  A List of Link objects */
  private ArrayList links = new ArrayList();
  private String species1, species2;
  /** Temp */
  private CurationSet wrappedCurationLinkSet;
  // dont think we need these anymore?
  //private CurationSet curationSet1, curationSet2;
  /** if false links contain features from the species themselves - otherwise the 
      connection to species features is via feature names
      if i recall ensj/gff just gives feat names for links and i think what
      this means is that for link select it actually finds the selected 
      features via the name - for embedded/game/jdbc i added in the actual
  features to the links so ya dont have to keep searching for them on select
  or at least thats what i recall - MG */
  private boolean linkedByName = true;

  /** non cur set wrapper */
  public LinkSet() {
    //??? shouldnt false be default - dont link by name!
    this(true); // linkedByName true by default
  }

  /** false is to not link by name (ensembl & gff link by name) */
  public LinkSet(boolean linkedByName) {
    this.linkedByName = linkedByName;
  }

  public LinkSet(FeatureList links,String species1, String species2) {
    setSpecies1(species1);
    setSpecies2(species2);
    addLinks(links);
  }

  /** Temporary CurationSet wrapper */
  public LinkSet(CurationSet curLinkSet, String species1, String species2) {
    setSpecies1(species1);
    setSpecies2(species2);
    wrappedCurationLinkSet = curLinkSet;
    getFeatsFromWrappedCurationLinkSet();
  }

  public void addLinks(LinkSet linkSet) {
    this.links.addAll(linkSet.links);
  }

  private void addLinks(FeatureList links) {
    links.setFeaturePairsOnly(true); // better be true
    for (int i=0; i<links.size(); i++) {
      links.add(new Link(links.getFeaturePair(i)));
    }
  }

  /** Take FeaturePairs out of curation set and put in FeatureList */
  private void getFeatsFromWrappedCurationLinkSet() {
    Vector forwardSet = 
      wrappedCurationLinkSet.getResults().getForwardSet().getFeatures();
    for(int j=0; j<forwardSet.size(); j++)
      addLink((FeaturePairI)forwardSet.get(j));
      
    Vector reverseSet =  
      wrappedCurationLinkSet.getResults().getReverseSet().getFeatures();
    for(int j=0; j<reverseSet.size(); j++)
      addLink((FeaturePairI)reverseSet.get(j));
  }
  
  public boolean linkedByName() { return linkedByName; }
  public boolean linkedByFeature() { return !linkedByName; }
  
  public String getName() { return species1+"-"+species2; }

  //void addFeature(SeqFeatureI feat) {
  public void addLink(FeaturePairI link) {
    //feats.addFeature(link);
    links.add(new Link(link));
  }

  public void addLink(Link link) {
    if (link ==  null)
      return;
    links.add(link);
  }

  public Link getLink(int i) { return (Link)links.get(i); }

  public int size() { return links.size(); }

  boolean hasLinks() { return size() > 0; }

  /** The idea i think is that species1 is above species 2 in synteny */
  public void setSpecies1(String species1) { this.species1 = species1; }
  public String getSpecies1() { return species1; }
  public void setSpecies2(String species2) { this.species2 = species2; }
  public String getSpecies2() { return species2; }

  /** Return LinkSet? yes */
  //public FeatureList findFeaturesByAllNames(String name) {
  public LinkSet findLinksWithName(String name) {
    LinkSet ls = new LinkSet();
    for (int i=0; i<size(); i++) {
      Link link = getLink(i);
      if (link.hasName(name)) {
        ls.addLink(link);
      }
    }
    return ls;
  }

//   void setCurationsFromCompositeCuration(CurationSet comp) {
//     curationSet1 = comp.getCurationSet(species1);
//     curationSet2 = comp.getCurationSet(species2);
//   }
//   public CurationSet getCurationSet1() { return curationSet1; }
//   public CurationSet getCurationSet2() { return curationSet2; }

  public boolean contains(Link link) { return links.contains(link); }
  public void clear() { links.clear(); }
}


