package apollo.datamodel;

/** DataLoader returns an ApolloDataI which can be queried whether its holding a 
    CurationSet or CompositeDataHolder, which can then be retrieved. This allows 
    both to be loaded by the DataLoader. 
    If you have an ApolloDataI in your hand (comes back from data load) 
    isCurationSet and isCompositeData are used to figure out what the thing is. 
    if isCurationSet returns true then we have a curationset and we do a 
    getCurationSet to get it. So for composite data holder returns false on 
    isCurationSet and so getCurationSet should not be called. The alternative would 
    be to pass on Object and then do instanceof CurationSet and instanceof 
    CompositeDataHolder. ApolloDataI makes it clear what are the 2 things one expects
    this data to be, and gives you a means of querying for and retreiving those 2 
    things. Its for clarity sake*/
public interface ApolloDataI {
  // Maybe this should be hasCurationSet - "is" is an implementation detail
  public boolean isCurationSet();
  /** if isCurationSet returns true than a real curation set would be returned here.
      This method should be used after testing isCurationSet()==true */ 
  public CurationSet getCurationSet();
  // is -> has?
  public boolean isCompositeDataHolder();
  public CompositeDataHolder getCompositeDataHolder();
  ///** Clean up dangling refs */ public void cleanup(); no longer needed
  /** Temporary method until all synteny stuff uses CompositeDataHolder */
  public boolean isMultiSpecies();

  /** unclear if name is really needed. region change event takes it but never used*/
  public String getName();
}
