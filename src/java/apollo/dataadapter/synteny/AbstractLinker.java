package apollo.dataadapter.synteny;

import apollo.config.Config;
import apollo.config.LinkType;
import apollo.config.FeatureProperty;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.ApolloAdapterException;
import apollo.dataadapter.DataInputType;
import apollo.datamodel.CurationSet;
import apollo.datamodel.Link;
import apollo.datamodel.LinkSet;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SpeciesComparison;
import apollo.util.FeatureList;
import apollo.util.SeqFeatureUtil;

import org.apache.log4j.*;

abstract class AbstractLinker implements LinkerI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AbstractLinker.class);

  private FeatureProperty featureProperty;

  /** This is basically the factory method for the linker. Cant make singleton as
      might have more than one feat prop per type - could cache on feat prop */
  static LinkerI getLinker(FeatureProperty linkedProp) { 
    LinkType type = linkedProp.getLinkType();

    logger.debug("getLinker() link type "+type);

    if (type.isPeptide()) {
      return new PeptideLinker(linkedProp); 
    }
    else if (type.isTranscript()) {
      return new TranscriptLinker(linkedProp);
    }
    else if (type.isSelf()) {
      return new SelfLinker(linkedProp);
    }
    else if (type.isNoLink()) { // shouldnt get to this point
      logger.error("Programmer error: link type is NO_LINK in "
                   +"GameSynteny.getLinker");
      throw new RuntimeException(); // ??
    }
    logger.warn("link type "+type+" didnt find linker for it???");
    return null; // shouldnt happen
  }

  protected FeatureProperty getFeatureProperty() {
    return featureProperty;
  }
  protected void setFeatureProperty(FeatureProperty fp) {
    featureProperty = fp;
  }

  protected String getSpecies1() { 
    return getFeatureProperty().getLinkSpecies1();
  }

  public LinkSet createLinksFromSpeciesComp(SpeciesComparison twoSpecies) {
    FeatureList feats1 = getLinkFeatures(twoSpecies.getCurationSet1());
    FeatureList feats2 = getLinkFeatures(twoSpecies.getCurationSet2());
    if (feats1.isEmpty() || feats2.isEmpty()) 
      return new LinkSet(); // null?
    return createLinksFromFeatLists(feats1,feats2);
  }

  /** Default behavior is to extract all feats of feature property in cur set */
  protected FeatureList getLinkFeatures(CurationSet curSet) {
    logger.debug("is parent link type "+getFeatureProperty().getLinkType().isParentLevel());
    if (getFeatureProperty().getLinkType().isParentLevel())
      return SeqFeatureUtil.getFeatPropLeafParentFeats(curSet,getFeatureProperty());
    // returns all leaf features for feat prop tier
    return SeqFeatureUtil.getFeatPropLeafFeatures(curSet,getFeatureProperty());
  }

  /** For every feat in feats1 go through every feat in feats2 and if there is a 
      location match make a link */
  private LinkSet createLinksFromFeatLists(FeatureList feats1,FeatureList feats2) {
    LinkSet links = new LinkSet(false); // false - dont link by name - link by feat

    for (int i = 0; i<feats1.size(); i++) {
      SeqFeatureI feat1 = feats1.getFeature(i);

      for (int j=0; j<feats2.size(); j++) {
        SeqFeatureI feat2 = feats2.getFeature(j);

        if (featsAreLinked(feat1,feat2)) {
          Link link = createLink(feat1,feat2);
          if (link != null)
            links.addLink(link);
          else
            logger.error("Failed to make link for "+feat1.getName()+", "+feat2.getName());
        }
      } // end of feats2 for loop
    } // end of feats1 for loop
    return links;
  }

  /** Returns true if 2 feats are linked to each other */
  protected abstract boolean featsAreLinked(SeqFeatureI feat1, SeqFeatureI feat2);

  /** Create link from 2 linked feats - default just use the 2 feats in link.
      PeptideLinker overrides to get subregions of hit feat. */
  public Link createLink(SeqFeatureI feat1, SeqFeatureI feat2) {
    // by default these should be in order of spec comp
    return new Link(feat1,feat2,hasSpeciesFeature(),hasPercentIdentity());
  }

  public CurationSet getCurationSetForLink(ApolloDataAdapterI adap,SeqFeatureI link)
    throws ApolloAdapterException {
    adap.setDatabase(getTargetDbOfLink(link));
    adap.setInputType(getInputType());
    adap.setInput(getInputString(link));
    logger.debug("AbsLink adap input "+adap.getInput()+" input string set to data adapter is "+getInputString(link));
    doPadding(adap,link);
    return adap.getCurationSet(); // throws DA exception
  }
  protected abstract DataInputType getInputType();
  protected abstract String getInputString(SeqFeatureI linkFeat);
  /** Add any padding - default noop */
  protected void doPadding(ApolloDataAdapterI adap,SeqFeatureI link) {}
  /** Return the db that the link links to, the opposite organism that its from  */
  private String getTargetDbOfLink(SeqFeatureI feat) {
    if (featFromLinkSpecies1(feat)) { 
      return Config.getSyntenyDatabaseForSpecies(featureProperty.getLinkSpecies2());
    }
    else {
      return Config.getSyntenyDatabaseForSpecies(featureProperty.getLinkSpecies1());
    }
  }
  boolean featFromLinkSpecies1(SeqFeatureI feat) {
    String spec = featureProperty.getLinkSpecies1();
    return feat.getRefSequence().getOrganism().equals(spec);
  }
  public boolean featListFromLinkSpecies1(FeatureList featList) {
    if (featList.isEmpty()) return false;
    return featFromLinkSpecies1(featList.getFeature(0));
  }

  /** well this is interesting - if have hit feature return hit, if not return feat*/
  protected SeqFeatureI getHitFeat(SeqFeatureI feat) {
    if (feat.hasHitFeature()) {
      feat = feat.getHitFeature();
    }
    return feat;
  }

  protected boolean hasSpeciesFeature() { return true; }
  /** At the moment all game links have no percent identity - change when not so */
  protected boolean hasPercentIdentity() { return false; }

  /** Returns how far feat is from left side of ref seq - for padding for retrieval */
  protected int padLeft(SeqFeatureI feat) {
    return feat.getLow() - feat.getRefSequence().getRange().getLow();
  }
  /** Returns how far feat is from right side of ref seq - for padding for retrieval */
  protected int padRight(SeqFeatureI feat) {
    return feat.getRefSequence().getRange().getHigh() - feat.getHigh();
  }


} 
