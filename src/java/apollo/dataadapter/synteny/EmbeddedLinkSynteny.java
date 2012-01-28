package apollo.dataadapter.synteny;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bdgp.io.DataAdapterException;
import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import apollo.util.FeatureIterator;
import apollo.util.FeatureList;
import apollo.util.SeqFeatureUtil;
import apollo.datamodel.CompositeDataHolder;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.Link;
import apollo.datamodel.LinkSet;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SpeciesComparison;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInputType;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;

import org.apache.log4j.*;

/** I renamed this from GameSynteny to EmbeddedLinkSynteny as in fact this 
    generalizes beyond game! jdbc now uses this as well. the difference from
    ensj & gff is that the synteny link data is embedded in the curation sets
    themselves (& extracted from them) rather than going to an external source
    for the link data (ie gff link file or ensj compara database)
   This is a helper class to SyntenyAdapter. I didnt call it GameSyntenyAdapter
    because its not a full on data adapter its just a helper to the SytnenyAdapter.
    I just wanted to encapsulate all the synteny game stuff, separate it from
    all the ensj synteny stuff, as the synteny adapter was getting rather muddled.
    Theres also actually no hardcoded reference to a game adapter. I tried to make
    this generic, and put all the particulars of links into FeatureProperty, so the
    idea is that its not necasarily just for game. I called it GameSynteny because
    at the moment only game is using this stuff, and that might be true indefinitely,
    and if i called it FeaturePropertySynteny it would be unclear how its used, where
    GameSynteny makes it really obvious this is where the game synteny stuff happens.
    If other adapters end up using this stuff we should rename it. 
    There are 2 entry points: 
    loadSpeciesAndLinks,
    loadNewSpeciesFromLink
*/
class EmbeddedLinkSynteny {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(EmbeddedLinkSynteny.class);
  
  private SyntenyAdapter syntenyAdapter;
  private SpeciesComparison speciesComparison;

  EmbeddedLinkSynteny(SyntenyAdapter syntenyAdapter) {
    this.syntenyAdapter = syntenyAdapter;
  }

  CompositeDataHolder loadSpeciesAndLinks(int numberOfSpecies) 
    throws DataAdapterException{ 
    if (numberOfSpecies == 1) {
      SpeciesComparison sc = loadOneCuration();
      return new CompositeDataHolder(sc);
    }
    CompositeDataHolder cdh = new CompositeDataHolder();
    SpeciesComparison previousComp = null;
    for (int i=0; i<numberOfSpecies-1; i++) {
      SpeciesComparison newComp = makeSpeciesComparison(i,previousComp);
      cdh.addSpeciesComparison(newComp);
      previousComp = newComp;
    }
    return cdh;
  }

  /** Load first curation set, return SpeciesComparison with 1st cur set - 
      second curation set will be loaded later(eg loadNewSpeciesFromLink) */
  private SpeciesComparison loadOneCuration() throws DataAdapterException {
    CurationSet cs = loadCurationSet(0);
    return new SpeciesComparison(getSpeciesName(0),cs);
  }

  /** Load up 2 species at once and add there links (link set) */
  private SpeciesComparison makeSpeciesComparison(int firstSpeciesIndex,
                                             SpeciesComparison previousComparison) 
    throws DataAdapterException {

    SpeciesComparison sc = new SpeciesComparison();
    CurationSet cs1;
    if (previousComparison == null) // load cur set if no prev comp
      cs1 = loadCurationSet(firstSpeciesIndex);
    else // get 1st cur set from prev comp cur set 2
      cs1 = previousComparison.getCurationSet2();
    
    CurationSet cs2 = loadCurationSet(firstSpeciesIndex+1);
    String firstSpec = getSpeciesName(firstSpeciesIndex);
    String secondSpec = getSpeciesName(firstSpeciesIndex+1);
    SpeciesComparison comp =  new SpeciesComparison(firstSpec,cs1,secondSpec,cs2);
    // this is where the LinkSet is made
    getLinksFromCurationSets(comp);
    return comp;
  }

  private String getSpeciesName(int speciesIndex) {
    return syntenyAdapter.getChildAdapter(speciesIndex).getSpecies();
  }

  /** From ApolloDataAdapterI. Bring up the link as a species in synteny.  
      Should this return true or false on success?
      A "link" is either a feature with a hit or the parent of features
      with hits.
      This is hardwired to 2 species case - need to make flexible.
   */
  public void loadNewSpeciesFromLink(SeqFeatureI feat,SpeciesComparison specComp) 
    throws DataAdapterException {
    // this is not true for feature sets - should i refine the checking?
    //if (!link.hasHitFeature()) return; // return false? exception?
    setSpeciesComparison(specComp);
    loadCurationSetForFeat(feat);
    getLinksFromCurationSets(specComp);
    // return compData? // return speciesComp // return true? - success?
  }

  private void setSpeciesComparison(SpeciesComparison sc) { speciesComparison = sc; }

  private SpeciesComparison getSpeciesComparison() { 
    if (speciesComparison==null) speciesComparison = new SpeciesComparison();
    return speciesComparison; 
  }

  /** Loads CurationSet from syntenyAdapter's adapter at adapterIndex
      this is currently hardwired for just 2 species (only works with 1 spec comp)
      maybe shouldnt return specComp? */
//   private SpeciesComparison loadCurationSetDELETE(int adapterIndex) throws DataAdapterException {
//     ApolloDataAdapterI adap = syntenyAdapter.getChildAdapter(adapterIndex);
//     IOOperation op = ApolloDataAdapterI.OP_READ_DATA; // we are reading
//     Object values = null;  // I think we dont use values - ok to be null?
//     CurationSet cs =  (CurationSet)adap.getUI(op).doOperation(values);
//     getSpeciesComparison().setCurationSet(adapterIndex,adap.getSpecies(),cs);
//     return getSpeciesComparison();
//   }

  private CurationSet loadCurationSet(int adapterIndex) throws DataAdapterException {
    ApolloDataAdapterI adap = syntenyAdapter.getChildAdapter(adapterIndex);
    IOOperation op = ApolloDataAdapterI.OP_READ_DATA; // we are reading
    Object values = null;  // I think we dont use values - ok to be null?
    return (CurationSet)adap.getUI(op).doOperation(values);
    //getSpeciesComparison().setCurationSet(adapterIndex,adap.getSyntenyTag(),cs);
    //return getSpeciesComparison();
  }

  /** Hardwired to 2 species case. */
  private void loadCurationSetForFeat(SeqFeatureI linkFeat) 
    throws DataAdapterException {
    ApolloDataAdapterI adap = syntenyAdapter.getChildAdapter(1);
    if (!featFromCurSet1(linkFeat)) { // from species 2 - loading for species 1
      adap = syntenyAdapter.getChildAdapter(0); // species 1 -> adap 0
    }
    LinkerI linker = getLinker(linkFeat);
    CurationSet cs = linker.getCurationSetForLink(adap,linkFeat);
    if (featFromCurSet1(linkFeat)) {
      speciesComparison.setCurationSet2(adap.getSpecies(),cs);
    } 
    else {
      speciesComparison.setCurationSet1(adap.getSpecies(),cs);
    }
  }

  /** Dig through both curation sets features with linked feature props and create links
      if they link. how to see what is linked is configured in tiers file (in theory) */
  private void getLinksFromCurationSets(SpeciesComparison speciesComp) {
    // false means dont link by name, which is old ensj/gff way of linking
    LinkSet linkSet = new LinkSet(false);
    try {
      for (int i=0; i<getPropertyScheme().getLinkedFeatPropsSize(); i++) {
        FeatureProperty linkedProp = getPropertyScheme().getLinkedFeatProp(i);
        LinkerI linker = getLinker(linkedProp);
        linkSet.addLinks(linker.createLinksFromSpeciesComp(speciesComp));
      }
    }
    catch (NullPointerException e) { // no organism -> null ptr w msg
      logger.error("Species link retrieval failed due to a null pointer "
                   +"exception. Organism may be null", e);
    }
    speciesComp.setLinks(linkSet);
  }

  private FeatureProperty getFeatProp(SeqFeatureI feat) {
    return getPropertyScheme().getFeatureProperty(feat.getTopLevelType());
  }
  private PropertyScheme getPropertyScheme() {
    return Config.getPropertyScheme();
  }
  
  /** "Species1" is the 1st cur set, the "top" species - this needs to be 
      paramaterized on species index */
  private boolean featFromCurSet1(SeqFeatureI feat) {
    String species1 = getSpeciesComparison().getCurationSet1().getOrganism();
    return feat.getRefSequence().getOrganism().equals(species1);
  }


  private LinkerI getLinker(SeqFeatureI featLink) {
    return getLinker(getFeatProp(featLink));
  }

  /** This is basically the factory method for the linker. */
  private LinkerI getLinker(FeatureProperty linkedProp) {
    return AbstractLinker.getLinker(linkedProp);
  }

}

