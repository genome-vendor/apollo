package apollo.gui.synteny;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.List;

import apollo.external.IgbBridge;

import apollo.config.ApolloNameAdapterI;
import apollo.config.Style;
import apollo.config.PropertyScheme;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.dataadapter.CurationState;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.CurationSet;
//import apollo.editor.AnnotationChangeLog;
import apollo.editor.AnnotationEditor;
import apollo.editor.TransactionManager;
import apollo.gui.Controller;
import apollo.gui.featuretree.FeatureTreeFrame;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;
import apollo.gui.genomemap.NavigationBar;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;

/** Holds stuff needed for each curation-set/apollo-panel/szap/"species". 
 should this subclass CurationState? */
public class GuiCurationState {

  /** curationNumber corresponds with # in speciesList and curation number 
      in compositeDataHolder */
   private int curationNumber; 
//   private String curationName;
//   private CurationSet curationSet;
//   /** Theres an AnnotationEditor for each strand */
//   private AnnotationEditor forwardAnnotEditor;
//   private AnnotationEditor reverseAnnotEditor;
//   private AnnotationChangeLog annotationChangeLog;
//   private ApolloDataAdapterI dataAdapter;
  private Controller controller; // -> CurState?
  private CurationState curState;

  private SelectionManager selectionManager;
  private StrandedZoomableApolloPanel strandedZoomableApolloPanel;
  private String zoomLayoutPosition = BorderLayout.SOUTH;
  private NavigationBar navigationBar;
  private FeatureTreeFrame featureTreeFrame;
  private IgbBridge igbBridge;

  GuiCurationState(int i) {
    curationNumber = i;
    // should we make the CurationState here or will it be preexisting?
    //curState = new CurationState(i);
  }

  private CurationState getCurationState() {
    // i think this will already be setup by data adapter but just in case
    if (curState == null) 
      curState = new CurationState(curationNumber);
    return curState;
  }

  public Controller getController() { 
    if (controller == null) {
      controller = new Controller();
      // controller method?
      controller.addListener(Controller.getMasterController());
    }
    return controller; 
  }

  public SelectionManager getSelectionManager() { 
    if (selectionManager == null) {
      selectionManager = new SelectionManager();
      selectionManager.setController(getController());
    }
    return selectionManager; 
  }

  void setCurationName(String spStr) {
    //curationName = spStr;
    curState.setCurationName(spStr);
  }
  
  public String getCurationName() {
    //return curationName;
    return curState.getCurationName();
  }

  public StrandedZoomableApolloPanel getSZAP() {
    if (strandedZoomableApolloPanel == null) {
      String zoomPos = getZoomLayoutPosition();
      strandedZoomableApolloPanel = new StrandedZoomableApolloPanel(this,zoomPos);
      StatusBar sb = CurationManager.getCurationManager().getStatusBar();
      strandedZoomableApolloPanel.setStatusBar(sb);
    }
    navigationBar.setEnabled(apollo.config.Config.isNavigationManagerEnabled());
    return strandedZoomableApolloPanel;
  }

  /** navigation bar on top of szap - should this be multi cur set? */
  public NavigationBar getNavigationBar() { 
    if (navigationBar == null) 
      navigationBar = new NavigationBar(this);
    return navigationBar; 
  }

  private String getZoomLayoutPosition() {
    return zoomLayoutPosition;
    // OR getCurationManager().getZoomLayoutPosition(curationNumber);
  }

  /** 1st zoombar goes on top, subsequent on bottoms - eventually reduce to one zoom
      bar - dont need all those zoom bars */
  void setZoomLayoutPosition(String layoutPos) {
    this.zoomLayoutPosition = layoutPos;
  }
    
  /** called by CurationManager before CurationState removal (when # of curSets 
      decreases) */
  void clear() {
    if (controller == null)
      return;
    controller.clear(false);
    // null things out? mem leaks?
  }
  
  
  void setDataAdapter(ApolloDataAdapterI da) {
    if (da.getCurationState() != null)
      curState = da.getCurationState();
    else
      getCurationState().setDataAdapter(da);
//     if (da.isComposite()) { // shouldnt happen
//       dataAdapter = da.getChildAdapter(curationNumber);
//     } 
//     else 
//       dataAdapter = da;
    // for now controller in gui
    da.setDataLoadListener(getController());
    da.getStyle().getPropertyScheme().addPropSchemeChangeListener(getController());
    // this was taken out by steve searle - but its needed for prop scheme changes
    // on tiers/dataadapter changes (e.g. gff to game)
    // if this breaks something in synteny (steve?) then an alternate approach needs
    // to be taken - but somehow the event needs to be fired
    da.getStyle().getPropertyScheme().firePropSchemeChangeEvent();
  }
  
  public ApolloDataAdapterI getDataAdapter() {
    //return dataAdapter;
    return curState.getDataAdapter();
  }

  /** does the data adapter have the right style? check this */
  public Style getStyle() { 
    if (getDataAdapter().getStyle() != null)
      return getDataAdapter().getStyle();
    return apollo.config.Config.getStyle(); // just in case
  }

  public PropertyScheme getPropertyScheme() {
    if (getDataAdapter() == null) // hopefully doesnt happen
      return null;
    return getDataAdapter().getStyle().getPropertyScheme();
  }

  public ApolloNameAdapterI getNameAdapter(AnnotatedFeatureI af) {
    //FeatureProperty fp = getPropertyScheme().getFeatureProperty(af.getBioType());
    return curState.getNameAdapter(af);
  }
  /** CurationHolder goes through compDatHolder and sets CurationState cur set */
  void setCurationSet(CurationSet cs) {
    unRegisterTranManager();
    curState.setCurationSet(cs);
    //if (cs.hasAnnotationChangeLog())
    //addAnnotLogListeners();
//     if (curationSet != null)// clean up old curationSet if there is one
//       curationSet.cleanup();
//     // set new curationSet
//     curationSet = cs;
    // could send out DataLoadEvent here after curState gets its cur set?
    // and RCE just goes to its controller
    getController().handleDataLoadEvent(new DataLoadEvent(this,cs));
    getSZAP().setCurationSet(cs);
    // To register TransactionManager to Controller
    registerTranManager();
    setUpIgbBridge();
  }
  
  private void setUpIgbBridge() {
    // remove old igb listener (if there was one)
    getController().removeListener(getIgbBridge());

    if (!getStyle().igbHttpConnectionEnabled())
      return; // igb connection not enabled - nothing to do
    
    getIgbBridge().setGenomicRange(getCurationSet());

    getController().addListener(getIgbBridge());
  }

  private IgbBridge getIgbBridge() {
    if (igbBridge == null)
      igbBridge = new IgbBridge(getCurationSet());
    return igbBridge;
  }

  /** This needs to be refactored! */
  private void unRegisterTranManager() {
    // curState might be changed (e.g. GameXML and Chado, two instances
    // are used) ???? why are 2 instances used - is this still true? i dont think so
    //if (curState.getTransactionManager() != null) {
    //  TransactionManager tm = curState.getTransactionManager();
    //  getController().removeListener(tm);
    //}
    // This is not a good way. The internal data should not be modified here. However,
    // moving this function to Controller will increase the logical coupling between
    // Controller and TransactionManager even though there is no code coupling.
    List listeners = getController().getListeners();
    if (listeners != null) {
      for (Iterator it = listeners.iterator(); it.hasNext();) {
        Object obj = it.next();
        if (obj instanceof TransactionManager) {
          it.remove();
        }
      }
    }
    getController().removeListener(curationDataLoadListener);
  }
  
  // should this be in CurationState - probably
  private void registerTranManager() {
    TransactionManager tm = curState.getTransactionManager();
    if (!getController().hasListener(tm)) {
      getController().addListener(tm); // annotChangeEvents -> trans
      tm.setAnnotationChangeListener(getController()); // for undos
      getController().addListener(curationDataLoadListener);
      //tm.setCurationState(getCurationState()); // for now for id undo
    }
  }

  private CurationDataLoadListener curationDataLoadListener =
  new CurationDataLoadListener();
    

  /** Clear out TransactionManager transactions on data load (this shouldnt clear
      on append loads - does DataLoadEvents happen on appends?) */
  private class CurationDataLoadListener implements DataLoadListener {
    public boolean handleDataLoadEvent(DataLoadEvent e) {
      if (e.dataRetrievalBeginning())
        getTransactionManager().clear();
      return true;
    }
  }

  public TransactionManager getTransactionManager() {
    return curState.getTransactionManager();
  }
  
  public CurationSet getCurationSet() { 
    return curState.getCurationSet();
  }

  public FeatureTreeFrame getFeatureTreeFrame() {
    if (featureTreeFrame == null) {
      featureTreeFrame = new FeatureTreeFrame(getCurationSet().getAnnots(),this);
    }
    return featureTreeFrame;
  }


  public void addAnnotationEditor(AnnotationEditor ae,boolean forwardStrand) {
    curState.addAnnotationEditor(ae,forwardStrand);
//     if (forwardStrand)
//       forwardAnnotEditor = ae;
//     else
//       reverseAnnotEditor = ae;
  }

  public AnnotationEditor getAnnotationEditor(boolean forwardStrand) {
    //return forwardStrand ? forwardAnnotEditor : reverseAnnotEditor;
    return curState.getAnnotationEditor(forwardStrand);
  }

  /** Whether curationSet has ref sequence (formerly in ApolloFrame) */
  public boolean haveSequence() {
    return curState.haveSequence();
//     SequenceI seq = curationSet.getRefSequence();
//     if (seq == null)
//       return false;
//     if (seq.isLazy()) 
//       return true;
//     return seq.getResidues()!=null && !seq.getResidues().equals("");
  }

  /** Returns true if curSet have >0 annotations */
  public boolean haveAnnots() {
    return curState.haveAnnots();
  }

}

//   /** Clear out AnnotationChangeLog transactions on data load 
//       -- this is done in CurationState */
//   private class CurationDataLoadListener implements DataLoadListener {
//     public boolean handleDataLoadEvent(DataLoadEvent e) {
//       if (e.dataRetrievalBeginning())
//         getAnnotationChangeLog().clearTransactions();
//       return true;
//     }
//   }

//   private class CurationDataLoadListener implements DataLoadListener {
//     public boolean handleDataLoadEvent(DataLoadEvent e) {
//       if (!e.dataRetrievalBeginning())
//         return false;
//       curationSet.cleanup();
//       return true;
//     }
//   }
//   void setStatusBar(StatusBar statusBar) {
//     getSZAP().setStatusBar(statusBar);
//   }

//   private AnnotationChangeLog getAnnotationChangeLog() {
//     AnnotationChangeLog acl = curState.getAnnotationChangeLog();
//     //if (annotationChangeLog == null) {
//     //annotationChangeLog = new AnnotationChangeLog();
//     addAnnotLogListeners();
//     return acl;
//  }

//   private void addAnnotLogListeners() {
//     AnnotationChangeLog acl = curState.getAnnotationChangeLog();
//     if (!getController().hasListener(acl)) {
//       getController().addListener(acl); // annotChangeEvents
//       getController().addListener(new CurationDataLoadListener()); //load clear
//     }
//   }
