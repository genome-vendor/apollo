package apollo.dataadapter;

/** Interface to get notification when an adapter is about to create new data 
    Should this be merged with DataLoadListener? YES! and go through controller
    perhaps Controller should go in a different package than gui?
    Should there be one method with a DataEvent that distinguishes 
    beginning and ending load?
    7.26.04 This has been merged with DataLoadListener. should remove after testing
 */
public interface DataListener { // DataLoadListener?, Lo
  /** Data source is creating new data - rename dataBeginLoad? dataLoadBegin yes 
   or in litener theme: handleLoadBeginEvent(DataLoadEvent)? */
  public void newData();

  // this event is sent out by AbstractApolloAdapter but on one currently listens to it
  // when does handleDataLoadEvent occur?
  // rename handleLoadDoneEvent(DataLoadEvent)?
  public void dataLoadingDone();
}

