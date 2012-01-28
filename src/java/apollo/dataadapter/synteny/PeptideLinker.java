package apollo.dataadapter.synteny;

import apollo.config.FeatureProperty;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInputType;
import apollo.datamodel.CurationSet;
import apollo.datamodel.Link;
import apollo.datamodel.SeqFeatureI;
import apollo.util.FeatureList;
import apollo.util.SeqFeatureUtil;

class PeptideLinker extends AbstractLinker implements LinkerI {

  /** Cant be singleton - could have multiple featProps with pep link */
  PeptideLinker(FeatureProperty featProp) {
    setFeatureProperty(featProp);
  }
  
  /** if cur set from link species1, than return feat prop feats (super.getLF)
      if species2 than get its transcripts, as we link to transcript/peptides
      in species2 */
  protected FeatureList getLinkFeatures(CurationSet speciesCurSet) {
    // check if org is null - if so throw null pointer w msg
    if (speciesCurSet.getOrganism() == null) {
      String m = "Organism is NULL for "+speciesCurSet.getName()+"\nCannot get "+
        "species links with no organism. Check your data.";
      throw new NullPointerException(m);
    }
    if (speciesCurSet.getOrganism().equals(getSpecies1())) 
      return super.getLinkFeatures(speciesCurSet); // feat prop feats
    else 
      return getTranscripts(speciesCurSet);
  }

  private FeatureList getTranscripts(CurationSet curSet) {
    return SeqFeatureUtil.getTranscripts(curSet);
  }

  /** Figure which feat is from peptide species, than see if its peptide name
      matches the hit name of the other feature */
  protected boolean featsAreLinked(SeqFeatureI feat1, SeqFeatureI feat2) {
    return getResultAnnotPair(feat1,feat2).isLinked();
  }

  protected ResultAnnotPairI getResultAnnotPair(SeqFeatureI f1, SeqFeatureI f2) {
    return new ResultPeptidePair(f1,f2,this);
  }


  /** Figure out which feature is a transcript and which is the result.
      Find the sub region of the exon result hit, and use that as the link feature 
      use the midpoint of  result which is in trascript coords - translate 
      to genomic of transcript cur set - find the exon in transcript that this hits
      then take the minimum of the exon and the hit as the feature (take out 
      blast bleeding over the edges)  */
  public Link createLink(SeqFeatureI feat1, SeqFeatureI feat2) {
    //ResultTranscriptPair resTrans = new ResultTranscriptPair(feat1,feat2);
    return getResultAnnotPair(feat1,feat2).createLink();
  }

  protected String getInputString(SeqFeatureI link) {
    if (link.canHaveChildren() && link.getFeatureAt(0) != null) {
      return link.getFeatureAt(0).getHitFeature().getName();
    }
    return link.getHitFeature().getName();
  }
  protected DataInputType getInputType() {
    return DataInputType.GENE;
  }
  protected void doPadding(ApolloDataAdapterI adap, SeqFeatureI linkFeat) {
    adap.setPadLeft(padLeft(linkFeat));
    adap.setPadRight(padRight(linkFeat));
  }

} 



