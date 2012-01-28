package apollo.datamodel;

import apollo.util.FeatureList;

public class SpeciesComparison {
  private String species1; // for now we need this
  private CurationSet curationSet1;
  private String species2;
  private CurationSet curationSet2;
  private LinkSet linkSet;

  public SpeciesComparison() {}
  
  public SpeciesComparison(String species1, CurationSet curationSet1) {
    this.curationSet1 = curationSet1;
    this.species1 = species1;
  }

  public SpeciesComparison(String s1, CurationSet c1, String s2, CurationSet c2) {
    this(s1,c1);
    species2 = s2;
    curationSet2 = c2;
  }
  public SpeciesComparison(String s1, CurationSet c1, LinkSet l, String s2, 
                           CurationSet c2) {
    this(s1,c1,s2,c2);
    linkSet = l;
  }

  public void setCurationSet1(String name, 
			      CurationSet cs) { 
    species1 = name;
    curationSet1 = cs; 
  }
  public CurationSet getCurationSet1() {
    return curationSet1;
  }

  public void setCurationSet2(String name, CurationSet cs) { 
    species2 = name;
    curationSet2 = cs;
  }

  public CurationSet getCurationSet2() { 
    return curationSet2;
  }

  /** index 0 -> cur set 1, index 1 -> cur set 2 */
  public void setCurationSet(int index, 
			     String name, 
			     CurationSet cs) {
    if (index < 0 || index > 1) return; // exception?
    if (index == 0) setCurationSet1(name,cs);
    else if (index == 1) setCurationSet2(name,cs);
  }

  /** Returns null if dont have species */
  public CurationSet getCurationSet(String species) {
    if (species1.equals(species))
      return curationSet1;
    if (species2.equals(species))
      return curationSet2;
    else
      return null;
  }
  
  public void setLinks(LinkSet links) {
    linkSet = links;
    linkSet.setSpecies1(species1);
    linkSet.setSpecies2(species2);
  }

  public void setLinks(FeatureList links) {
    this.linkSet = new LinkSet(links,species1,species2);
  }

  LinkSet getLinkSet() { return linkSet; }

  public String getSpecies1() { return species1; }
  public String getSpecies2() { return species2; }
  public boolean hasBothSpecies() {
    return curationSet1!=null && curationSet2!=null;
  }
  public boolean hasSecondSpecies() { return curationSet2!=null; }
  /** Returns true if there is a link set, even if its empty */
  public boolean hasLinkSet() {
    return linkSet!=null;
  }
  /** Returns true if there is a non-empty link set */
  boolean hasNonEmptyLinkSet() {
    return hasLinkSet() && linkSet.hasLinks();
  }

  public boolean featFromCurSet1(SeqFeatureI feat) {
    return curationSet1.getOrganism().equals(feat.getRefSequence().getOrganism());
  }
}
