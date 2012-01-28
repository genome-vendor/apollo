package apollo.dataadapter.synteny;

import apollo.config.FeatureProperty;
import apollo.dataadapter.DataInputType;
import apollo.datamodel.Link;
import apollo.datamodel.SeqFeatureI;

import org.apache.log4j.*;
  
/** TierLinker? TierLocationLinker? FeatLinker? FeatLocation? Result? */
class SelfLinker extends AbstractLinker implements LinkerI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SelfLinker.class);

  SelfLinker(FeatureProperty featProp) {
    setFeatureProperty(featProp);
  }

  private boolean isLinkById() {
    return getFeatureProperty().getLinkType().isLinkById();
  }
  
  /** Return true if hit location of feat1 is query location of feat2, 
      shouldnt matter which is feat1 and feat2 */
  protected boolean featsAreLinked(SeqFeatureI feat1, SeqFeatureI feat2) {
    // Link By ID - for now getSynLinkInfo just returns string (future list)
    logger.debug("SelfLinker testing for a link feat1 "+feat1+" id "+feat1.getId()+" feat2 "+feat2+" syn info f2 "+feat2.getSyntenyLinkInfo());
    if (isLinkById())
      return feat1.getId().equals(feat2.getSyntenyLinkInfo());
    // Link By Range (game)
    feat1 = getHitFeat(feat1);
    return feat1.getStart() == feat2.getStart() && feat1.getEnd() == feat2.getEnd();
  }
  protected String getInputString(SeqFeatureI link) {
    // ID
    logger.debug("SelfLink getInputString isLink "+isLinkById()+" link info "+link.getSyntenyLinkInfo()+" link "+link);
    if (isLinkById()) {
      // for now eventually would like to do this with generic HitFeature
      // that can be part of any seq feat - not just feat pair leaves
      return link.getSyntenyLinkInfo();
    }
    // RANGE
    String in = null;
    SeqFeatureI hit = link.getHitFeature();
    // have to do padding here - no pad in location url
    int start = hit.getLow() - padLeft(link);
    int end = hit.getHigh() + padRight(link);
    //if (linkByLocation()) { -- dont need for now
    in = hit.getName()+":"+start+":"+end;
    return in;
  }
  protected DataInputType getInputType() {
    // hmmmmm - are ids genes?? not sure about this
    if (isLinkById()) {
      // for now.... need to correlate with so type lest chado adapater frown
      return DataInputType.getDataTypeForSoType("mRNA");
      
      //return DataInputType.GENE;
      //return DataInputType.MRNA;
    }
    else
      return DataInputType.BASEPAIR_RANGE;
  }
}
