package apollo.dataadapter;

import java.util.EventObject;
import apollo.datamodel.*;

/**
   DataLoadEvent are sent out on new data being loaded. (formerly RegionChangeEvent)
   Should these be for a specific species/CurationSet or for all 
   species/CompositeDataHolder? probably the former
 */

public class DataLoadEvent extends EventObject {

  public final static int DATA_RETRIEVE_BEGIN = 0;
  public final static int DATA_RETRIEVE_DONE = 1;
  // if need be we can add GUI_LOAD_BEGIN and GUI_LOAD_END, for before and after
  // loading of curationSet/compData into the apollo gui. GUI_LOAD_BEGIN is 
  // probably redundant of DATA_RETRIEVE_DONE
  private int type = DATA_RETRIEVE_DONE;

  //private String      region;
  private CurationSet curationSet;
  //private ApolloDataI apolloData;
//   /**
//    * get rid of this one - cur set -> apollo data
  // may want to revive this one - species specific cur set
//    */
//   public DataLoadEvent(Object      source,
//                            CurationSet set,
//                            String      region) {
//     super(source);
//     this.set = set;
//     this.region = region;
//   }

  /** If type is DATA_RETRIEVE_BEGIN then we dont have apolloData nor region yet */
  public DataLoadEvent(Object source, int type) {
    super(source);
    this.type = type;
  }

  public DataLoadEvent(Object source, CurationSet cs) {
    super(source);
    //this.apolloData = apolloData;
    this.curationSet = cs;
    //this.region = region;
    type = DATA_RETRIEVE_DONE;
  }

//   /** This is not used at all - delete? */
//   public String getRegion() {
//     //return region;
//     return curationSet.getName();
//   }
  /** This wasnt actually used anywhere - though i can see why its here */
//   public CurationSet getCurationSet_changetogetapollodata() { // delete!
//     return set;
//   }

  /** Should this just be curation set for a particular species? Currently the 
      only place this is used is in MenuManager, asks if isMultiSpecies to 
      determine whether to put up synteny menu. could have separate flag for that.
  */
  public CurationSet getCurationSet() {
    return curationSet;
  }

  /** New data is about to be fetched and made. 
      This also implies getApolloData()==null as we havent gotten the data yet */
  public boolean dataRetrievalBeginning() {
    return type ==  DATA_RETRIEVE_BEGIN;
  }
  /** New data has been retrieved/created. 
      A new curation set/composite data holder has been
      constructed by the data adapter, but it has not been loaded into the gui yet.
      getApolloData will be non null, unless its for a former species that now doesnt
      have a curation set (species were downsized) */
  public boolean dataRetrievalDone() {
    return type == DATA_RETRIEVE_DONE;
  }
}
