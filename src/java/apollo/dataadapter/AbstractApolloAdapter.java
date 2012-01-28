package apollo.dataadapter;

import java.util.*;

import org.apache.log4j.*;

import org.bdgp.io.AbstractDataAdapter;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.io.IOOperation;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.CurationSet;
import apollo.datamodel.CompositeDataHolder;
import apollo.datamodel.DbXref;
import apollo.datamodel.GenomicRange;
import apollo.datamodel.SequenceI;
import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.Style;
import apollo.dataadapter.NotImplementedException;
import apollo.dataadapter.StateInformation;
//import apollo.editor.AnnotationChangeLog; 

/** All the dataadapters now subclass AbstractApolloAdapter, which new data adapters
    should do as well. In AbstractApolloAdapter.getCurationSet()
    apollo.config.Config.newDataAdapter(this) is called. This sets up the new style before
    the data loads. So all the subclasses have to call super.getCurationSet() at the
    start of their getCurationSet() method. A data adapter has to subclass
    AbstactApolloAdapter and call super.getCurationSet(). (AAA.getCS() also clears
    out all the old data that is still lingering).  I know this is a bit of an awkward
    requirement and is easy to miss on coding a new data adapter. I am certainly open
    to other suggestions on how to get the notification out before the data loads.
    The problem is that org.bdgp.swing.widget.DataAdapterChooser is a bit of a black box
    and the first point of control one gets in apollo is in data adapters with
    DataAdapterGUI.doOperation which then calls DataAdapter.getCurationSet. One
    idea I had was to have DataAdapterChooser send out some sort of notification
    of whether its going to do a load or cancel. I've ran this by John Richter
    who is more or less in charge of org.bdgp utilities but I havent heard back yet.
    - MG
*/
public abstract class AbstractApolloAdapter extends AbstractDataAdapter
  implements ApolloDataAdapterI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AbstractApolloAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** DataInputType and input String go together, im almost tempted
   * to make a DataInput object that couples them to make this explicit
   */
  //private DataInputType inputType;
  private DataInput dataInput;
  //private String input;
  //private HashSet dataListeners;
  private String database;
  protected String region;
  protected CurationSet curation_set;
  protected Style style;
  /** synteny explicitly sets the style */
  private boolean styleExplicitlySet=false;
  //private Controller controller;
  private DataLoadListener regionChangeListener;
  private CurationState curationState;
  /** for multi-curation curations are numbered - 0 for single curation */
  private int curationNumber = 0;
  /** To change the name string (which is used in the Data Adapter Chooser list),
      AbstractApolloAdapters should call setName in their constructors, rather
      than overriding getName. */
  private String name = "Abstract Apollo Adapter";

  /** From org.bdgp.io.DataAdapter interface. no-op default implementation. A 
      data adapter should override this if it needs to do some initialization. */
  public void init() {}

  /** Name label for use in Data Adapter Chooser.  Don't override this method--call
      setName from your constructor. */
  public String getName() {
    return name;
  }
  public void setName(String n) {
    name = n;
  }

  /** From org.bdgp.io.DataAdapter. Default implementation returns "". Override this
      to return a string describing the data adapter. The reason for the default 
      implementation is that as far as I can tell getType is not used by apollo nor
      org.bdgp, so seems silly to require it. Am i missing something? */
  public String getType() { return ""; }

  public void setCurationNumber(int curationNumber) {
    this.curationNumber = curationNumber;
  }

  // this is presently not used - delete?
  public void setCurationState(CurationState curationState) {
    this.curationState = curationState;
  }
  
  public CurationState getCurationState() { 
    if (curationState == null) {
      curationState = new CurationState(curationNumber);
      curationState.setDataAdapter(this);
    }
    return curationState; 
  }
  
  // JC: changed protection to public because getCurationState() is public!
  public ApolloNameAdapterI getNameAdapter(AnnotatedFeatureI annot) {
    return getCurationState().getNameAdapter(annot);
  }

  public void setDataInput(DataInput dataInput) {
    this.dataInput = dataInput;
  }
  public DataInput getDataInput() { 
    return dataInput;
  }

  /** Returns null if not file */
  protected String getFilename() {
    if (getDataInput() == null)
      return null;
    return getDataInput().getFilename(); // returns null if not file
  }

  /** DataInputType describes the type of input (gene,cytology,scaffold...)
   does this belong in the abstract class?*/
  public void setInputType(DataInputType type) {
    if (dataInput == null)
      setDataInput(new DataInput(type));
    else 
      getDataInput().setType(type);
    //inputType = type;
  }


  /** Returns the type of input data (gene,file,band...)
   * @see apollo.dataadapter.DataInputType
   * Should this go into org.bdgp.io.DataAdapter?
   */
  public DataInputType getInputType() {
    if (getDataInput() == null)
      return null; // raise exception?
    return getDataInput().getType();
    //return inputType;
  }

  /** Returns the input String passed to the DataAdapter, the input is
   * of course associated with the input type
   * Should this go into org.bdgp.io.DataAdapter? */
  public String getInput() {
    if (getDataInput() == null)
      return null; // raise exception?
    return getDataInput().getInputString();
  }


  /** Input string that corresponds with the input type
   * (eg gene name for gene input type)
   */
  public void setInput(String input) {
    logger.debug("AAA input "+input+" dataInput "+dataInput);
    if (dataInput == null)
      setDataInput(new DataInput(input));
    else
      getDataInput().setInputString(input);
    //this.input = input;
    logger.debug("AAA done with set input "+getDataInput().getInputString()+" dataInput "+dataInput);
    
  }

  /** This has to be called in the subclass.getCurationSet
      (super.clearOldData())
      to do the new data notification(clears old features) -
      also it notifies Config of style change.
      This needs to be called after it has been verified that 
      the user input is ok.
      e.g. that the filename is a valid filename. 
      Otherwise old data will be cleared
      out but the new has failed.
      Split into 2 methods? fireDataLoadEvent and setStyle? or rename newDataLoading?
       */
  protected void clearOldData() 
    throws ApolloAdapterException {
    notifyNewData(); // send out DataLoadEvent
    //apollo.config.Config.newDataAdapter(this); // to pick up new style
    // have to send actual style as there might be a synteny style now associated with
    // this adapter
    this.style = null; // unfix speciesToStyle - will this mess up synteny?
    Style style = getStyle(); // workaround for a bug in getStyle(); do NOT call getStyle() more than once until this is fixed
    style.setLocalNavigationManagerEnabled(null);
    Config.setStyle(style);
    logger.info("set style to " + style + "; style file source(s): " + style.getAllStyleFilesString());
    String tiers = style.getTiersFile();
    getCurationState().emptyNameAdapter();
    // A missing tiers file is pretty serious--better throw an exception.
    if (tiers == null) {
      String message = "\nFatal error: can't find Types (tiers) file!" + 
        "\nPlease fix the Types parameter in " +
        style.getFileName() +
        "\n(and if that looks ok, check that a style file is not trying to include itself)." +
        ".\nYou will then need to restart Apollo.";
      if (style.getFileName().indexOf("game.style") >= 0)
        message += "\n\nYou seem to be using game.style as your primary style file.\n" +
          "Note that game.style is an abstract style that does not include a Types\n" + 
          "file setting.  You should be using fly.style or rice.style, each of which\n" +
          "inherits from game.style";
      //      Config.errorDialog(message);
      throw new ApolloAdapterException(message);
    }
    else
      logger.info("using tiers file " + tiers);

    if (Config.internalMode())
      logger.info("running in internal mode.\n");
  }

  /** Send out DataLoadEvent for beginning of data retrieval */
  private void notifyNewData() {
    // if we dont have a listener yet this is our 1st load and we dont need to fire
    // is that lame?
    if (regionChangeListener == null) 
      return;
    DataLoadEvent e = 
      new DataLoadEvent(this,DataLoadEvent.DATA_RETRIEVE_BEGIN);
    regionChangeListener.handleDataLoadEvent(e);
  }

  // this is redundant with LoadUtil DataLoadEvent
//   protected void notifyLoadingDone() {
//     if (dataListeners==null)
//       return;
//     Iterator it = dataListeners.iterator();
//     while (it.hasNext())
//       ((DataListener)it.next()).dataLoadingDone();
//   }
  
  /**
   * Send any necessary signals to the server to release annotation locks or undo edits
   * --after the user has been prompted that these will be lost. By default this method
   * has an empty implementation: it is overridden, for instance, in the ensj-adapter
  **/
  public boolean rollbackAnnotations(CompositeDataHolder cdh) {
    if (!isComposite())
      return rollbackAnnotations(cdh.getCurationSet(0));
    else // composite/SyntenyAdapter doesnt deal with this yet
      return true;
  }
  /** Overridden by AnnotationEnsJAdapter and EfeatchSequenceAdapter */
  protected boolean rollbackAnnotations(CurationSet curationSet){
    return true;
  }

//   /** Retrieve singleton AnnotationChangeLog */ 
//   protected AnnotationChangeLog getAnnotationChangeLog() {
//     //return AnnotationChangeLog.getAnnotationChangeLog();
//     // shouldnt this do a getCurationState()?
//     if (curationState == null) {
//       logger.error("Cur state is null in AbstractApolloAdapter");
//       return null;
//     }
//     return curationState.getAnnotationChangeLog();
//   }

  /* This isn't really abstract I suppose, but why proliferate copies
     of unimplemented methods of the interface into every single 
     adapter?  SUZ is willing to learn why */
  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public SequenceI getSequence(DbXref dbxref, int start, int end)
    throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref, int[] start, int[] end)
    throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public String getRawAnalysisResults(String id) 
    throws apollo.dataadapter.ApolloAdapterException {
    throw new NotImplementedException();
  }

  /** This region doesnt seem to be used anywhere? */
  public void setRegion(String region) throws ApolloAdapterException {
    this.region = region;
  }

  /** This region is used for overlay onto this existing curation */
  public void setCuration(CurationSet curation) throws ApolloAdapterException {
    this.curation_set = curation;
    opToUI = new HashMap(4); // clear out cache of UIs on cur set change
  }

  public void commitChanges(CurationSet curation) 
    throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  /** SyntenyAdapter and SerialDiskAdapter override this */
  public void commitChanges(CompositeDataHolder cdh) throws ApolloAdapterException {
    if (!isComposite())
      commitChanges(cdh.getCurationSet(0));
  }

  public void commitChanges(Object values) throws ApolloAdapterException {
    if (values instanceof CurationSet)
      commitChanges((CurationSet)values);
    else if (values instanceof CompositeDataHolder)
      commitChanges((CompositeDataHolder)values);
  }

  /** GAMEAdapter and ChadoXMLAdapter can specify whether to save annots and
      whether to save results */
  public void commitChanges(Object values, boolean saveAnnots, boolean saveResults) throws ApolloAdapterException {
    if (values instanceof CurationSet)
      commitChanges((CurationSet)values, saveAnnots, saveResults);
    else if (values instanceof CompositeDataHolder)
      commitChanges((CompositeDataHolder)values, saveAnnots, saveResults);
  }

  public void commitChanges(CurationSet curation, boolean saveAnnots, boolean saveResults) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public void commitChanges(CompositeDataHolder cdh, boolean saveAnnots, boolean saveResults) throws ApolloAdapterException {
    if (!isComposite())
      commitChanges(cdh.getCurationSet(0), saveAnnots, saveResults);
  }

  public CurationSet getCurationSet() throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Properties getStateInformation() {
    throw new NotImplementedException();
  }

  public void setStateInformation(Properties p) {
    throw new NotImplementedException();
  }
  
  // im confused wether to scrap DataInputType or get it to jibe with state info 
  // it is a nice controlled vocab, where state info is loosey goosey
  // im hoping to find a way for DataInputType to complement state info
  // for now this method checks DataInputType and then state info
  String getInputTypeString() {
    if (getInputType() != null)
      return getInputType().toString();
    return getStateInformation().getProperty(StateInformation.INPUT_TYPE); 
  }

  /** sugar for getting input id from state info */
  String getInputId() { 
    return getStateInformation().getProperty(StateInformation.INPUT_ID); 
  }
  int getStart() {
    String startKey = StateInformation.SEGMENT_START;
    return Integer.parseInt(getStateInformation().getProperty(startKey));
  }
  

  /** GenomicRange is a convenient object to carry chromosome, start and end. 
      May at some point replace this with a more generic Location object, for non
      chromosome locations, this will do for now. This sets state info properties
      INPUT_TYPE = "Location", INPUT_ID = chromName, START & END */
  public void setLocation(GenomicRange chromosomeLocation) {
    Properties props = getStateInformation();
    // danger here: "chromosome" has to match sequence type in chado-adapter.xml
    // which is from SOFA. not sure how to ensure these are in synch.
    props.setProperty(StateInformation.INPUT_TYPE,"chromosome");
    props.setProperty(StateInformation.INPUT_ID,chromosomeLocation.getChromosome());
    props.setProperty(StateInformation.SEGMENT_START,chromosomeLocation.getStart()+"");
    props.setProperty(StateInformation.SEGMENT_STOP,chromosomeLocation.getEnd()+"");
  }

  /**
   * This SHOULD reset all the contents of the stateInformation being
   * held by an adapter: with setStateInformation, this is not guaranteed.
  **/
  public void clearStateInformation(){
    throw new NotImplementedException("clearStateInformation must be implemented by the adapter class!");
  }

  public Boolean addToCurationSet()
    throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  /** I think this should be taken out to force a data adapter to implement it. If 
      a data adapter doesnt have a ui it can return null for this. */
//   public DataAdapterUI getUI(IOOperation op) {
//     return null;
//   }

  protected boolean operationIsSupported(IOOperation op) {
    for (int i=0; i<getSupportedOperations().length; i++) {
      if (op.equals(getSupportedOperations()[i])) 
        return true;
    }
    return false;
  }

  /** Whether write operation is supported by data adapter */
  public boolean canWriteData() {
    return operationIsSupported(ApolloDataAdapterI.OP_WRITE_DATA);
  }

  private HashMap opToUI = new HashMap(4);
  protected void cacheUI(IOOperation op, DataAdapterUI ui) {
    // probably not necasary but doesnt hurt to check supported ops
    if (!operationIsSupported(op))
      return; // exception? error msg?
    opToUI.put(op,ui);
  }
  protected DataAdapterUI getCachedUI(IOOperation op) {
    // check supported op?
    return (DataAdapterUI)opToUI.get(op);
  }

  /** Whether data adapter contains link data used for synteny, returns false by default
      override if otherwise */
  public boolean hasLinkData() { return false; }
  
  /** By default return false - data adapter is not composite. SyntenyAdpapter 
      overrides this to return true. */
  public boolean isComposite(){
    return false;
  }

  /** Throws not implemented exception as by default data adapter dont have 
   species adapter (isComposite()=false). SyntenyAdapter overrides this, and 
   returns its species adapters */
  public ApolloDataAdapterI getChildAdapter(String species) {
    throw new NotImplementedException(
      "getSpeciesAdapter() should not be getting called on AbstractApolloAdapters"
    );
  }
  /** Throws not implemented exception as by default data adapter dont have 
   species adapter (isComposite()=false). SyntenyAdapter overrides this, and 
   returns its species adapters */
  public ApolloDataAdapterI getChildAdapter(int i) {
    throw new NotImplementedException(
      "getSpeciesAdapter() should not be getting called on AbstractApolloAdapters"
    );
  }

  /** Returns 0 by default. SyntenyAdapter overrides. */
  public int getNumberOfChildAdapters() { return 0; }

  public Map getAdapters(){
    throw new NotImplementedException(
      "getAdapters() should not be getting called on AbstractApolloAdapters"
    );
  }
  
  /** Synteny gives a species to each adapter to help keep track of them. 
   This also helps retrieve AnnotationChangeLog for species 
   This is for single species adapters(all except SyntenyAdapter) */
  private String species = Config.getDefaultSingleSpeciesName();
  // is string good enough here or should data adapters get involved with some
  // offshoot of guis CurationState? that has controller,speciesName, curset, annEd?
  public void setSpecies(String species) { this.species = species; }
  public String getSpecies() { return species; }

  /** By default return style associated with the adapter in apollo.cfg.
      This can be overridden by setStyle, which is used by SyntenyAdapter
      to overrides its childrens style with its own. Once we have the ability
      to switch between single and multi species, the style will need to 
      be synched with it (not there yet). 
      Also a style can have other styles it loads depending on the db that is 
      being loaded (yes this is a little quirky and perhaps needs a revisit).
  */
  public Style getStyle() {
    // if null hasnt been set by setStyle, return config default
    if (style == null) 
      style = getDefaultStyle();
    // if style has been explicitly set (synteny) dont change it
    if (styleExplicitlySet)
      return style;
    // Check if default style has database styles, and if so load that one
    // getDefaultStyle() has to be queried NOT style 
    if (getDefaultStyle().databaseHasStyle(getDatabase())) {
      // JC: There's a bug here, insofar as a second call to getStyle() can change the style back
      // from the database-specific style to the default style (assuming only a single level of
      // indirection in the style files.)  At the root of the problem is the fact that getStyle() has 
      // a side effect; adding this 'if' clause makes the existing problem evident by making the side 
      // effect non-idempotent.
      
      // so the bug here is that getDefaultStyle() has to be queried NOT style as
      // style may be the style one got from the db from a previous call
      //style = style.getStyleForDb(getDatabase());
      style = getDefaultStyle().getStyleForDb(getDatabase());
    } 
    else { // no db style - use default style
      style = getDefaultStyle();
    }
    // cant set style - style may be set already by synt - confusing
    //Config.setStyle(style); 
    return style;
  }

  /** Return style to use if no db (file) or db is not listed. This is the style
      listed in apollo.cfg with the data adapter */
  protected Style getDefaultStyle() {
    return Config.getStyle(getClass().getName());
  }

  /** Presently only game adapter uses this */
  public void setDatabase(String database) {
    this.database = database; 
  }
  public String getDatabase() { return database; }

  /** Bring up the link as a species in synteny. Presently this can only be done in
      synteny mode - eventually would be nice to do from regular apollo 
      From ApolloDataAdapterI - default throws exception - SyntenyAdapter overrides */
  public void loadNewSpeciesFromLink(apollo.datamodel.SeqFeatureI link, CompositeDataHolder c) 
    throws org.bdgp.io.DataAdapterException {
    throw new NotImplementedException();
  }

  /** Request to "pad" the input padLeft basepairs to the left(5' forward strand) -
   default noop - presently only used by game adapter in synteny context */
  public void setPadLeft(int padLeft) {}
  /** Request to "pad" the input padRight basepairs to the right(3' forward strand) */
  public void setPadRight(int padRight) {}

  /** By default a data adapter uses the style listed with it in the config file.
      In synteny mode the childrens default style needs to be overridden by the 
      synteny adapters style. This method is for the synteny override of its
      child adapters style. also using for SpeciesToStyle override(game). */
  public void setStyle(Style style) { 
    styleExplicitlySet = true;
    this.style = style; 
  }

  
  /** Controller should be in apollo.controller package not gui 
      All we need to do is send out DataLoadEvents so maybe this should just
      be setDataLoadListener(DataLoadListener) */
//   public void setController(Controller c) {
//     controller = c;
//   }
  /** We currently only need one DataLoadListener(curSet controller)
      if we need more than one change this to addDataLoadListener */
  public void setDataLoadListener(DataLoadListener l) {
    regionChangeListener = l;
  }
}


