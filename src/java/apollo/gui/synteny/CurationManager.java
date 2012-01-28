package apollo.gui.synteny;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;

import apollo.config.Config;
import apollo.config.Style;
import apollo.datamodel.CompositeDataHolder;
import apollo.datamodel.CurationSet;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.DataInputType;
import apollo.dataadapter.SerialDiskAdapter;
import apollo.external.ApolloControlServer;
import apollo.gui.AutoSaveThread;
import apollo.gui.CheckMemoryThread;
import apollo.gui.Controller;
import apollo.gui.StatusBar;
import apollo.gui.evidencepanel.EvidencePanel;
import apollo.gui.evidencepanel.EvidencePanelContainer;
import apollo.gui.event.SetActiveCurStateEvent;

import org.apache.log4j.*;

/** Holds CurationState objects. Reflects current CompositeDataHolder. 
    Should CurationManager also get in the link business and hold SyntenyLinkPanels
    as well? probably.
    Perhaps this should be broken into 2 (or 3) objects. The tracking of data 
    adapters is handy for SyntenyAdapter - should maybe have the data adapter 
    stuff in a dataadapter object - but might be hard to separate - maybe do with
    an interface?
*/

public class CurationManager { 

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(CurationManager.class);

  private static CurationManager curationManagerSingleton;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /** list of all CurState instances */
  private List curStateList = new ArrayList(4);
  
  /** CurState that is currently the "active" curState */
//SMJS  private GuiCurationState activeCurState = getCurationState(0);
  private GuiCurationState activeCurState;

  private CompositeDataHolder compositeDataHolder;
  private StatusBar statusBar;

  /** This is SyntenyAdapter for multi-species, and "single-species adapter"
      (eg GAMEAdapter) for single curationState */
  private ApolloDataAdapterI dataAdapter;

  /** Singleton - there should only be one curationState holder */
  public static CurationManager getCurationManager() {
    if (curationManagerSingleton==null)
      curationManagerSingleton = new CurationManager();
    return curationManagerSingleton;
  }
  
  /** Private constructor called by singleton get method. inits autosave */
  private CurationManager() {
    initAutoSave();
    statusBar = new StatusBar();
    initHttpServer(); // listens for http requests (from igb) if configged
  }

  /** Synch CurationManager up with new composite data holder. Called by 
   CompositeApolloFrame.loadData (on every load) */
  void setCompositeDataHolder(CompositeDataHolder cdh) {
    if (compositeDataHolder == cdh) {
      logger.debug("setCompositeDataHolder(): already synched");
      return; // already synched
    }
    compositeDataHolder = cdh;
    getEvidencePanelContainer().clear(); // wipes out ev panel, change

    // reset curationState from cdh
    for (int i = 0; i<compositeDataHolder.numberOfSpecies(); i++) {
      GuiCurationState curationState = getCurationState(i);
      //curationState.setCompositeDataHolder(compositeDataHolder);
      curationState.setCurationName(compositeDataHolder.getSpecies(i));
      curationState.setCurationSet(compositeDataHolder.getCurationSet(i));
      // could send out DataLoadEvent here after curState gets its cur set?
      // Should CurationState hold evidence panel?
      EvidencePanel currentEvidencePanel = new EvidencePanel(getCurationState(i));
      getEvidencePanelContainer().addEvidencePanel(currentEvidencePanel);
    }   

    if (activeCurState == null) {
      setActiveCurState(getCurationState(0));
    }
    
    // set up igb bridge if configured
    //setupIgbBridge(); // --> gui cur state
    

    // remove excess curationState (if we downsized our curationState)
    for (int i = numberOfCurations()-1; i>=compositeDataHolder.numberOfSpecies(); i--)
      removeCurationState(i);
  }

  public CompositeDataHolder getCompositeDataHolder() {
    return compositeDataHolder;
  }

  private EvidencePanelContainer getEvidencePanelContainer() {
    return EvidencePanelContainer.getSingleton();
  }

  /** data adapter may be composite or may not - if not then we only
      have one curationState (at least we better). This is called by 
      CompositeApolloFrame.loadData() */
  void setDataAdapter(ApolloDataAdapterI dataAdapter) {
    this.dataAdapter = dataAdapter;
    if (dataAdapter.isComposite()) {
      for (int i = 0; i< dataAdapter.getNumberOfChildAdapters(); i++) {
        getCurationState(i).setDataAdapter(dataAdapter.getChildAdapter(i));
      }
    }
    else { // single curationState (hopefully)
      getCurationState(0).setDataAdapter(dataAdapter);
      if (numberOfCurations() > 1) { // in theory this shouldnt happen
        String m="Programmer error. CurationManager.setDataAdapter received non-composite"
          +" data adapter but has more than one curationState";
        logger.error(m);
      }
    }
  }

  /** data adapter could be composite(SyntenyAdapter) or single(the rest) depending on
      if we are multi or single curationState. It is the data adapter that produced the
      composite data holder(or the curation set that the composite data holder wraps)
      and can save the comp data holder (or its 1st cur set) */
  public ApolloDataAdapterI getDataAdapter() { return dataAdapter; }

  public int numberOfCurations() { 
    return curStateList.size();
  }

  /** Whether we have more than one curationState or not */
  public boolean isMultiCuration() {
    return numberOfCurations() > 1;
  }
  public boolean isSingleCuration() {
    return numberOfCurations() == 1; // 0?
  }

  public GuiCurationState getCurationState(int i) {
    GuiCurationState curState=null;
    if (i < curStateList.size())
      curState = (GuiCurationState)curStateList.get(i);
    else if (i >= curStateList.size()) {
      curState = new GuiCurationState(i);
      // or should curstate query curManager for this?
      curState.setZoomLayoutPosition(getZoomLayoutPosition(i));
      //curState.setStatusBar(statusBar);
      curStateList.add(i,curState); // i should be size of list in theory
    }
    return curState;
  }

  private String getZoomLayoutPosition(int i) {
    if (i == 0 && dataAdapter.getNumberOfChildAdapters() > 1 ) 
       // SMJS Used to be && isMultiCuration() but as getCurationState(0) was being created at instatiation time it didn't know it
       // was going to be part of a multicuration, so I moved its creation a bit later in the process
      return BorderLayout.NORTH;
    return BorderLayout.SOUTH; // all others south
  }

  // convenience
  public String getCurationName(int i) {
    return getCurationState(i).getCurationName();
  }

  public StatusBar getStatusBar() { return statusBar; }

  private void removeCurationState(int i) {
    getCurationState(i).clear();
    curStateList.remove(i);
  }

  /** Convenience for getCurationState(i).getController() */
  public Controller getCurationController(int i) {
    return getCurationState(i).getController();
  }

  /** Convenience for adding listener to all curationState */
  void addListenerToAllCurations(EventListener l) {
    for (int i=0; i<numberOfCurations(); i++)
      getCurationController(i).addListener(l);
  }

  public void setActiveCurState(GuiCurationState curationState) {
    GuiCurationState oldActiveCurState = activeCurState;

    SetActiveCurStateEvent evt = new SetActiveCurStateEvent(this,curationState,oldActiveCurState);

    if (activeCurState != null) {
      activeCurState.getController().handleSetActiveCurStateEvent(evt);
    }   

    activeCurState = curationState;

    if (curationState != null) {
      curationState.getController().handleSetActiveCurStateEvent(evt);
    }
  }
  
  public void setActiveCurState(String curSetName) {
    GuiCurationState cs = getCurStateForCurName(curSetName);
    if (cs != null)
      setActiveCurState(cs);
  }

  public static Style getActiveStyle() {
    return getActiveCurationState().getStyle();
  }

  public static GuiCurationState getActiveCurationState() {
    return getCurationManager().getActiveCurState();
  }

  public GuiCurationState getActiveCurState() { return activeCurState; }

  public boolean isActiveCurName(String curName) {
    if (curName == null)
      return false;
    return curName.equals(getActiveCurState().getCurationName());
  }

  /** Returns null if no cur state with curationName (shouldnt happen) */
  public GuiCurationState getCurStateForCurName(String curationName) {
    // dont think we need a hash for this
    for (int i=0; i<numberOfCurations(); i++) {
      if (getCurationState(i).getCurationName().equals(curationName))
        return getCurationState(i);
    }
    logger.error("Programmer error: no cur set with name "+curationName);
    return null; 
  }
  
  /**
   * If the input from dataadapter is a specific feature within the curation 
   * then select it. Presently only set up for DataInputType.GENE. If GENE
   * then search for features with gene name and select them. This can be
   * done for any input that is a subset of the whole curation set obvioulsy.
   * Was a part of loadGUI(), had to break out as FileMenu._doLoad clears 
   * out selection after calling loadGUI().
   */
  public void selectInputFeatures() {
    for (int i=0; i<getCurationManager().numberOfCurations(); i++) {
      ApolloDataAdapterI dataAdapter = getCurationState(i).getDataAdapter();
      if (dataAdapter == null)
        return;
      if (dataAdapter.getInputType() == DataInputType.GENE ||
          // Special case--input was an URL with "gene" in it, e.g.
          // http://www.fruitfly.org/cgi-bin/annot/get_xml_url.pl?database=%DATABASE%&gene=cact
          (dataAdapter.getInputType() == DataInputType.URL &&
           // If input was via the DAS2 adapter gui, getInput() will be null
           dataAdapter.getInput() != null &&
           dataAdapter.getInput().indexOf("gene") > 0)) {
        String in = dataAdapter.getInput();
        if (in != null) {
          // gene window is how much padding to allow when zooming into feature
          getCurationState(i).getSZAP().selectFeaturesByName(in,Config.getGeneWindow());
        }
      }
    }
  }
  
  private void initAutoSave() {
    if (!Config.isEditingEnabled())
      return;
    try {
      int saveInterval = Config.getAutosaveInterval();
      if (saveInterval > 0) {
        AutoSaveThread ast = 
          new AutoSaveThread(Config.getAutosaveFile(),saveInterval);
        
        ast.setDataAdapter(new SerialDiskAdapter(Config.getAutosaveFile()));
        
        ast.start();
      } else { // save interval == 0
        logger.warn("Autosave disabled. If Apollo crashes and you "
                    +"have not saved, YOUR WORK WILL BE LOST.");
      }
      
      // Also start thread to check free memory
      CheckMemoryThread cmt = new CheckMemoryThread(Config.getMemoryAllocation());
      cmt.start();
    } catch (RuntimeException e) {
      JOptionPane.showMessageDialog(null,e,"Warning",
                                    JOptionPane.WARNING_MESSAGE);
      logger.error(e.getMessage(), e);
      //System.exit(2); // i dont think we should exit should we?
    }//end try/catch
  }

  private void initHttpServer() {
    if (Config.httpServerIsEnabled())
      new ApolloControlServer(); // starts up ACServlet that listens for http requests
  }

}
