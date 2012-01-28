package apollo.gui.genomemap;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import apollo.config.Config;
import apollo.gui.ControlledObjectI;
import apollo.gui.Selection;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;
import apollo.gui.Controller;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.menus.*;
import apollo.gui.event.*;
import apollo.gui.event.MouseButtonEvent;
import apollo.gui.synteny.GuiCurationState;
import apollo.datamodel.*;
import apollo.seq.io.*;
import apollo.util.*;

import apollo.dataadapter.*;
import gov.sandia.postscript.*;

//used for dynamic loading of DataAdapters
import java.lang.reflect.*;

import org.apache.log4j.*;

/**
 * This class is a container to hold and manage the various Views
 * which are needed by Apollo. It holds all the views from both strands.
 */
public class ApolloPanel extends JPanel implements 
  ApolloPanelI,
  BaseFocusListener,
  ControlledObjectI,
  FeatureSelectionListener,
  KeyListener,
  MouseInputListener,
  MouseWheelListener,
  DataLoadListener,
  RubberbandListener,
    //TypesChangedListener,
  ViewListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ApolloPanel.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected Image               backBuffer;
  protected int                 backBufferWidth  = 0;
  protected int                 backBufferHeight = 0;

  private   Vector              views; // ViewI vector
  protected Vector              linearViews; // ViewI viector
  protected Vector              pickViews; // pickViewI vector
  private   ScaleView           scaleView;  // SZAP will set this so we can access it here

  protected ApolloLayoutManager layout_manager;
  protected Controller          controller;
  protected ViewI               focus;
  protected String              name;

  protected boolean             syncLimits   = false;

  protected RubberbandRectangle rubberband;

  protected boolean             inDrag       = false;
  protected MouseEvent          pressEvent   = null;

  protected boolean             inTierDrag   = false;
  protected TierViewI           dragTierView = null;
  protected StatusBar           statusBar;
  protected Rectangle           layoutBounds;
  protected boolean             invalid      = true;
  protected boolean             allowsMovement = true;
  protected boolean             showEdgeMatches = true;
  //private boolean             realRemove = true;

  private MouseManager mouseManager;
  private SelectionManager selectionManager;
  private GuiCurationState curationState;
  //private boolean loadInProgress=false; // not used anymore - but should it be?
  
  
  ApolloPanel(String name,GuiCurationState curationState) {
    // Basic setup
    setName(name);

    this.curationState = curationState;
    setSelectionManager(curationState.getSelectionManager()); 

    // vectors to hold views and view subsets
    views       = new Vector();
    linearViews = new Vector();
    pickViews   = new Vector();

    // Add mouse and keyboard listeners (there is no addMouseInputListener())
    addMouseMotionListener(this);
    addMouseListener      (this);
    addMouseWheelListener (this);
    addKeyListener        (this);

    // Add rubberband
    rubberband  = new RubberbandRectangle(this);
    rubberband.setActive(true);
    rubberband.addListener(this);

    //new MousePositionReporter(this);

    // Setup panel graphics properties
    setOpaque(false);
  }

  /** This gets called by gui machinery when panel is removed from parent, 
      overrides Component.removeNotify() - this was setup to clear() ApolloPanel
      on remove, but now that szaps and apollo panels are not be recreated with
      every load we actually want to never clear() - i think this means we dont 
      need clear anymore either */
//   public void removeNotify() {
//     // logger.debug("Remove notify for " + getName());
//     if (realRemove)
//       clear();

//     super.removeNotify();
//     realRemove = true;
//   }

//   public void setRealRemove(boolean state) {
//     realRemove = state;
//   }

//   /** clears out and removes listeners and views and sets them to null */
//   void clear() {

//     if (rubberband != null) {
//       rubberband.removeListener(this);
//       removeMouseListener((MouseListener)rubberband);
//       removeMouseMotionListener((MouseMotionListener)rubberband);
//     }

//     removeMouseMotionListener(this);
//     removeMouseListener(this);
//     removeKeyListener(this);

//     clearViews(false); // false - remove views from view vectors

//     rubberband = null;
//     controller = null;
//     layout_manager = null;
//     if (views != null) {
//       views.removeAllElements();
//     }
//     linearViews.removeAllElements();
//     pickViews.removeAllElements();
//     views = null;
//     focus = null;
//     pressEvent = null;
//   }

  /**
     If just features then just takes features out of views,
     otherwise gets rid of views - if its true that we dont need clear() anymore 
     than the boolean can be taken out
  */
  private void clearViews(boolean justFeatures) {
    if (views == null) {
      return;
    }
    ArrayList featViews = getFeatureViews();
    for (int i=0; i<featViews.size(); i++) {
      FeatureView fv = (FeatureView) featViews.get(i);
      if (justFeatures)	fv.clearFeatures();
      else fv.clear(); // wipes everything out
    }
    
    if (!justFeatures) 
      for (int i=0; i<views.size(); i++) 
	remove((ViewI)views.elementAt(i));
  }
  
  /** Helper function to get feature views that goes through ContainerViewI's in
      in views vector */
  private ArrayList getFeatureViews() {
    if (views == null) {
      return null;
    }
    ArrayList featViews = new ArrayList(4);
    for (int i=0; i<views.size(); i++) {
      ViewI view = (ViewI)views.elementAt(i);
      if (view instanceof FeatureView) featViews.add(views.elementAt(i));
      if (view instanceof ContainerViewI) {
	Vector fViews = 
	  ((ContainerViewI)view).getViewsOfClass(apollo.gui.genomemap.FeatureView.class);
	featViews.addAll(fViews);
      }
    }
    return featViews;
  }

  /** Empties out the views but keeps the empty views (clearViews(true)) */
  void clearFeatures() {
    clearViews(true);
  }

  static final Object ALOCK = new ApolloTreeLock();
  static class ApolloTreeLock {}
  public synchronized final Object getApolloLock() {
    return ALOCK;
  }

  /**
   * Add a View to the views. This no longer does an invalidate/validate trigerring
   * a layout. This is now the responsibility of the caller to do. This is more efficient
   * for the case when you are adding many things, you only need to do the validate/layout
   * after adding everything, not for each add. Also the intermediate layouts would
   * screw up scroll bar values on revcomp.
   */
  public void add(ViewI view,Object constraints) {
    views.addElement(view);
    if (view instanceof ViewI && !(view instanceof DragViewI)) {
      linearViews.addElement(view);
    }
    if (view instanceof PickViewI && !(view instanceof DragViewI)) {
      pickViews.addElement(view);
    }
    if (layout_manager != null) {
      layout_manager.addLayoutView(view,constraints);
    }
    view.addViewListener(this);
    // This validate causes a layout to occur before all the views have been added
    // which can alter the scrollbar values on a revcomp since a layout will occur
    // with not all the views so the view will be given a lot more real estate
    // szap now calls validate after all the views have been added - its more efficient
    // and it doenst create funny inbetween states that screw up the scrollbars
  }

  public void add(ViewI view) {
    add(view,null);
  }

  // Called by SZAP so we can access scaleView from here when we need to.
  public void setScaleView(ScaleView scale) {
    scaleView = scale;
  }

  /**
   * Set the Apollo specific layout manager for this Panel
   */
  public void setLayout(ApolloLayoutManager manager) {
    super.setLayout(manager);
    this.layout_manager = manager;
    invalidate();
    validate();
  }

  /**
   * Remove a View from the views
   *
   *
   */
  public void remove(ViewI view) {
    boolean found = false;
    if (views.contains(view)) {
      views.removeElement(view);
      found = true;
    }
    if (linearViews.contains(view)) {
      linearViews.removeElement(view);
    }
    if (pickViews.contains(view)) {
      pickViews.removeElement(view);
    }
    if (layout_manager != null) {
      layout_manager.removeLayoutView(view);
    }
    if (found) {
      invalidate();
      validate();
    }
  }

  // Need this here because we need to do this from places where we only have
  // access to ApolloPanel, not StrandedZoomableApolloPanel
  public void putVerticalScrollbarsAtStart() {
    for (int i=0;i<linearViews.size();i++) {
      ViewI lv = ((ViewI)linearViews.elementAt(i));
      if (lv instanceof ResultView)
        ((ResultView)lv).putScrollAtStart();
      else if (lv instanceof AnnotationView)
        ((AnnotationView)lv).putScrollAtStart();
    }
  }

  /**
   * Sets flag indicating whether to synchronise the limits on all
   * the LinearViews. This is necessary for the OverviewPanel, but
   * not desirable for the FineEditor panel.
   */
  public void setSyncLimits(boolean state) {
    this.syncLimits = state;
    if (this.syncLimits) {
      syncViewLimits(-1, -1);  // no range provided
    }
  }

  public void setSyncLimits(boolean state, int reglow, int reghigh) {
    this.syncLimits = state;
    if (this.syncLimits) {
      syncViewLimits(reglow, reghigh);
    }
  }

  /**
   * Gets flag indicating whether to synchronise the limits on all
   * the LinearViews.
   */
  public boolean isSynced() {
    return this.syncLimits;
  }

  /**
   * Actually do the view limit synchronisation
   * This actually adds a 1/100 of seq length as padding to each end,
   * and updates all the views with these limits. This means that
   * limits != sequence start and end.
   */
  protected void syncViewLimits(int reglow, int reghigh) {
    int [] globalLimits = new int [2];

    if (reglow < 0) {
      for (int i=0;i<linearViews.size();i++) {
        ViewI lv = ((ViewI)linearViews.elementAt(i));
        if (lv.areLimitsSet()) {
          if (i == 0 || lv.getLimits()[0] < globalLimits[0]) {
            globalLimits[0] = lv.getLimits()[0];
          }
          if (i == 0 || lv.getLimits()[1] > globalLimits[1]) {
            globalLimits[1] = lv.getLimits()[1];
          }
        }
      }
    }
    // Original approach resulted in huge limits because some XML files have features
    // that extend way beyond the range of the sequence itself.
    // Why not just use the start/end of the sequence as limits?  Users will still
    // be able to see features that extend beyond it if they zoom out.


    // Because quite often there is no sequence (gff files etc.) so the
    // limits end up screwed. One way round this might be to make a dummy
    // sequence which pretends to have the same limits as the features

    // Note this didn't work because it assumed all ranges start at 0 which they don't

    else {
      // Leave some space on both sides so features don't slam against the left and right edges
      int hspace = (reghigh-reglow)/100;
      globalLimits[0] = reglow - hspace;
      globalLimits[1] = reghigh + hspace;
    }

    for (int i=0;i<linearViews.size();i++) {
      ViewI lv = (ViewI) linearViews.elementAt(i);
      lv.setLimits(globalLimits);
      lv.setZoomFactor(1.0);
    }
    setLinearCentre((int)(globalLimits[1]+globalLimits[0])/2);
  }

  /**
   * Sets the linear centre for the focused view, or
   * all LinearViews depending on syncLimits flag
   */
  public void setLinearCentre(int Position) {
    if (allowsMovement) {
      if (isSynced()) {
        for (int i=0;i<linearViews.size();i++) {
          ViewI lv = ((ViewI)linearViews.elementAt(i));
          lv.setCentre(Position);
        }
      } else {
	focus.setCentre(Position);
      }
    }
    repaint();
  }

  public void setMovementAllowed(boolean state) {
    allowsMovement = state;
  }

  public int getLinearCentre() {
    if (isSynced()) {
      return ((ViewI)linearViews.elementAt(0)).getCentre();
    } else {
      return focus.getCentre();
    }
  }

  public int getLinearMinimum() {
    if (isSynced()) {
      return ((ViewI)linearViews.elementAt(0)).getMinimum();
    } else {
      return focus.getMinimum();
    }
  }

  public int getLinearMaximum() {
    if (isSynced())
      return ((ViewI)linearViews.elementAt(0)).getMaximum();
    else 
      return focus.getMaximum();
  }

  public void setVisible(boolean state) {
    super.setVisible(state);
    backBuffer = createImage(1,1);
  }
  /** This returns the width of the panel in base pairs NOT pixels. 
      Is the linear part of the method name suppose to connote this?
      If so it is not obvious. It should probably be renamed 
      getBasePairWidth or getCoordinateWidth if we dont want to
      assume base pairs.
      Its returning start and end base pair that is currently visible.
      Loops through linear views and uses first visible one.
  */
  public int getVisibleBasepairWidth() {
    int lv_width = -1;

    if (isSynced()) {
      // 0th view is site view which doesnt have width until visible - ???
      ViewI lv = null;
      for (int i = 0; i < linearViews.size() && lv == null; i++) {
        lv = (((ViewI)linearViews.elementAt(i)).isVisible() ?
	      (ViewI) linearViews.elementAt (i) : null);
      }
      if (lv != null) {
        int [] lims = lv.getVisibleRange();
        lv_width = (lims[1]-lims[0]);
      }
    } else {
      int [] lims = focus.getVisibleRange();
      lv_width = (lims[1]-lims[0]);
    }
    return lv_width;
  }

  /**
   * Set zoom factor on all views
   */
  public void setZoomFactor(double xFactor, double yFactor) {
    if (isSynced()) {
      for (int i=0;i<linearViews.size();i++) {
        ViewI lv = ((ViewI)linearViews.elementAt(i));
        if (xFactor > 0.0)
          lv.setZoomFactor(xFactor);
      }
    }
    repaint();
  }
  /**
   * Set X zoom factor on all views
   */
  public void setZoomFactorX(double xFactor) {
    setZoomFactor(xFactor,-1.0);
  }
  /**
   * Set Y zoom factor on all views
   */
  public void setZoomFactorY(double yFactor) {
    setZoomFactor(-1.0,yFactor);
  }

  /**
   * Sets the event controller for the panel. This is for controlling
   * the dispatch of Apollo specific (Feature) events. All the components
   * dealling with apollo data should register with a single controller,
   * which will notify them when changes, selections, moves... occur.
   */
  public void setController(Controller controller) {
    logger.debug("setController called for " + getName());
    this.controller = controller;
    controller.addListener(this);
    for (int i=0; i<views.size(); i++) {
      if (views.elementAt(i) instanceof ControlledObjectI) {
        ControlledObjectI cti = (ControlledObjectI) views.elementAt(i);
        cti.setController(controller);
      }
    }
  }

  /** Controller for selection */
  private void setSelectionManager(SelectionManager sm) {
    selectionManager = sm;
    //selectionManager.setApolloPanel(this); // for now - eventually shouldnt need this
    mouseManager = new MouseManager(this,getCurationState());
  }

  private GuiCurationState getCurationState() { return curationState; }

  public Selection getSelection() {
    return selectionManager.getSelection();
  }

  /**
   * Gets the apollo event controller for the panel.
   */
  public Controller getController() {
    return this.controller;
  }

  public Object getControllerWindow() {
    return SwingMissingUtil.getWindowAncestor(this);
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  public void print(File file, String orientation, String scale) {
    try {
      PrintWriter fw = new PrintWriter(new FileWriter(file));
      Graphics psg = new PSGr2(fw);

      double scaleVal = 1.0;
      try {
        scaleVal = new Double(scale).doubleValue();
      } catch (Exception e) {
        logger.error("Invalid scale factor", e);
        return;
      }

      if (orientation.equals("landscape")) {
        fw.println("-30 30 translate");
        fw.println("90 rotate");
        //fw.println("30 -642 translate");
        fw.println("" + ((int)(30.0*scaleVal)) + " " + ((int)(-822.0*scaleVal)) + " translate");
        fw.println("" + scale + " " + scale + " scale");
      } else {
        fw.println("-30 30 translate");
        int yOffset = (int)(762.0 - 792.0*scaleVal );
        fw.println("30 " + yOffset + " translate");
        fw.println("" + scale + " " + scale + " scale");
      }

      for (int i=0; i<views.size(); i++) {
        ((ViewI)views.elementAt(i)).setInvalidity(true);
      }
      psg.setClip(new Rectangle(0,0,getSize().width,getSize().height));
      paintComponent(psg);
      psg.dispose();
      fw.close();
    } catch (Exception e) {
      logger.error("Failed printing to file " + file, e);
    }
  }

  /**
   * Overidden paintComponent to draw any views requiring a repaint
   */
  public void paintComponent(Graphics g) {
    // The load in progress paint supression needs to be better
    // The views should paint - but just there backrounds - no features
    // even better the views should dump their old features at load time
    //if (loadInProgress) return;//this isnt set anywhere,was it ever,should it be?
    Rectangle clipRect  = null;
    Vector damagedViews = null;
    //    long timestamp;

    // set the graphics to the back graphics

    // get the damaged rectangle
    Rectangle damage = g.getClipBounds();

    // find the overlapping views
    damagedViews = getViews(damage);


    Graphics fg;
    // SMJS If we're printing just send the PSGr2 graphics around,
    //      don't wrap it in a FastClippingGraphics
    if (Config.useFastClipGraphics() && !(g instanceof PSGr2)) {
      fg = new FastClippingGraphics(g);
    } else {
      fg = g;
    }
    fg.setFont(Config.getDefaultFont());
    for (int i=0;i<damagedViews.size();i++) {
      // redraw those views
      ViewI v = ((ViewI)damagedViews.elementAt(i));
      if (!v.isInvalid()) {
        v.setGraphics(fg);
        clipRect = v.getBounds().intersection(damage);
        fg.setClip(clipRect);
        v.paintView();
      }
    }
    fg.setClip(damage);

    // If not drawn then check if view has been set to invalid. If so
    // it needs update for some other reason eg. rescaling, relayout,
    // resizing.
    for (int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v.isInvalid() && !(v instanceof DragViewI) && v.isVisible()) {
        v.setGraphics(fg);
        fg.setClip(v.getBounds());
        v.paintView();
        damage = damage.union(v.getBounds());
        v.setInvalidity(false);
      }
    }

    // After all normal views drawn draw any dragging views.
    for (int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v instanceof DragViewI) {
        Rectangle draggyBounds = new Rectangle(v.getBounds());
        Rectangle panelBounds  = getBounds();

        if (draggyBounds.x < 0) draggyBounds.x = 0;
        if (draggyBounds.y < 0) draggyBounds.y = 0;
        if (draggyBounds.x > panelBounds.width) draggyBounds.x = panelBounds.width;
        if (draggyBounds.y > panelBounds.height) draggyBounds.y = panelBounds.height;
        if (draggyBounds.x+draggyBounds.width  > panelBounds.width)  draggyBounds.width  = panelBounds.width-draggyBounds.x;
        if (draggyBounds.y+draggyBounds.height > panelBounds.height) draggyBounds.height = panelBounds.height-draggyBounds.y;

        fg.setClip(draggyBounds);
        v.setGraphics(fg);
        v.paintView();
        damage = damage.union(v.getBounds());
        v.setInvalidity(false);
      }
    }
    fg.setClip(damage);
  }

  /**
   * Overidden doLayout to handle View layout
   */
  int count = 0;
  public void doLayout() {
    count++;
    logger.debug("doLayout called, count = " + count);

    if (count < 3) {
      // This code is here to stop relayout when just the status bar is updated.
      boolean isInvalid = false;
      for (int i=0; i<views.size() && !isInvalid; i++) {
        ViewI v = (ViewI)views.elementAt(i);
        if (v.isInvalid() && v.isVisible()) {
          isInvalid = true;
          logger.debug("View " + v.getName() + " is invalid");
        }

      }

      isInvalid = isInvalid || invalid;

      // Layout is not done if the bounds haven't changed unless one or more visible
      // views are invalid or the panel has been set invalid.
      if (layout_manager != null 
	  && (!getBounds().equals(layoutBounds) || isInvalid)) {
        layout_manager.layoutViews(this);
        layout_manager.layoutContainer(this);
        layoutBounds = new Rectangle(getBounds());
      }
      logger.debug("end layout");
      count--;
    } else {
      logger.warn("recursive doLayout() call.");
      count = 0;
    }
  }

  /**
   * Set the Graphics to draw to.
   *
   * @param g The new graphics to draw to.
   */
  public void         setViewGraphics(Graphics g) {}


  /** Causes views to layout if this.doLayout is called */
  public void setInvalidity(boolean state) {
    invalid = state;
  }

  /**
   * Get the Graphics to draw to.
   *
   * @param g The new graphics to draw to.
   */
  public Graphics     getViewGraphics() {
    return null;
  }

  public Image   getBackBuffer() {
    return backBuffer;
  }

  /**
   * Returns the name of the panel.
   */
  public String      getName() {
    return this.name;
  }

  /**
   * Sets the name of the panel - is this used at all?
   *
   * @param name The name of the panel.
   */
  public void        setName(String name) {
    this.name = new String(name);
  }

  /**
   * Get the View at a specified location in the Panel
   */
  protected ViewI      getViewAt(Point p) {
    ViewI retview = null;
    for (int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (!(v instanceof DragViewI)) {
        if (v.getBounds().contains(p) && v.isVisible()) {
          if (v instanceof ContainerViewI) {
            v = ((ContainerViewI)v).getContainedViewAt(p);
          }
          if (retview!=null) {
            logger.warn("Overlapping non drag views (" + v.getName() + " and " + retview.getName() + ") in getViewAt()");
          }
          retview = v;
        }
      }
    }
    return retview;
  }

  /**
   * Sets the view which has 'focus'
   */
  protected void       setFocusView(ViewI focus) {
    this.focus = focus;
  }

  // Used by ScaleView
  public ViewI getFocus() {
    return focus;
  }

  // Handle the events
  // Clearing the selection when a TypesChangedEvent occurs is very conservative.
  // It stops selections in types which have become invisible, but could be
  // annoying to the user - we'll see.
//   public boolean handleTypesChangedEvent(TypesChangedEvent evt) {
//     // Clearing selections here inadvertently causes a bug -
//     // when TiersMenu is brought up for the 1st time the ShowMenu
//     // calls up the hidden tiers for their colors, PropertyScheme create
//     // new FeatureProperty and notifies TypesObserver Observer
//     // TypesObserver calls Controller.handleTypesChangedEvent which
//     // calls this method and the selection gets deselected and cant be
//     // used for things in the menu like output to fasta - this only
//     // happens on 1st selection to get the hidden tier stuff
//     return true;
//   }

  public boolean handleDataLoadEvent(DataLoadEvent evt) {
    clearSelection();
    updateSelection();
    return true;
  }

  private void updateSelection() {
    fireFeatureSelectionEvent(getSelection());
    repaint();
  }

  private void    fireFeatureSelectionEvent  (Selection sel) {
    FeatureSelectionEvent evt = new FeatureSelectionEvent(this,sel);
    controller.handleFeatureSelectionEvent(evt);
  }

  public boolean handleViewEvent(ViewEvent evt) {
    if (evt.getType() == ViewEvent.LIMITS_CHANGED) {
      if (isSynced()) {
        syncViewLimits(-1,-1);
      }
    }
    return true;
  }

  public boolean handleBaseFocusEvent (BaseFocusEvent evt) {
    // redundant -  szap.handleBFE->setCentreBase
//    setLinearCentre(evt.getFocus());
    return true;
  }

  /**
   * FeatureSelectionListener - should this scroll to the selection?
   * YES
   * This sets Selection selection to the features in the selection event
   * that are found in the views. When feature gets added to Selection, 
   * Selection tells it its selected. Repaint is called and features will
   * paint themselves selected if they are so.
   */
  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
    boolean alreadyProcessed = false;
    /* if source is from a FeatureView then it's from ourself essentially and
       has already been dealt with (by attaching drawables as 
       selectionListeners)
       This should get set to true if we've single-clicked an intron 
       (double-clicking an intron shouldn't do anything; instead, 
       it's ending up throwing an exception) */
    if (evt.getSource() == this)
      alreadyProcessed = true;
    if (!alreadyProcessed) {
      for (int i=0; i<views.size(); i++) {
        ViewI v = (ViewI)views.elementAt(i);
        if (v instanceof SelectViewI && v.isVisible()) {
          SelectViewI sv = (SelectViewI)v;
          sv.select(evt.getSelection());
	  // The object that is the ContainerViewI should also be a SelectViewI
        } else if (v instanceof ContainerViewI) {
          Vector svs = ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.SelectViewI.class);
          for (int j=0; j<svs.size(); j++) {
            SelectViewI sv = (SelectViewI)svs.elementAt(j);
            sv.select(evt.getSelection());
          }
        }
      }
    }
    processSelection(evt.getSelection());
    getStrandedZoomableApolloPanel().scrollToSelection();
    //highlightEdges(evt.getSelection(),true); 
    //repaint();

    return true;
  }

  /** Notice this is the same iteration as in handleFeatureSelectionEvent
   * sv.verticalScrollToSelection used to be part of handleFeatureSelectionEvent
   * but the vertical scroll cant happen until zoom has happened, as zoom changes
   * the view, and zoom has to wait for the whole selection to be over.
   * It would be nice if we didnt have to iterate through the views to
   * dig up the selection. If the feature itself was capable of adding itself
   * to view.
   */
  void verticalScrollToSelection(/*FeatureSelectionEvent evt*/) {
    for (int i=0; i<views.size(); i++) {

      ViewI v = (ViewI)views.elementAt(i);

      if (v instanceof SelectViewI && v.isVisible()) {
        SelectViewI sv = (SelectViewI)v;
        sv.verticalScrollToSelection();
        //Selection newPick = sv.findSelected(evt);
        //if (newPick.size() > 0)sv.verticalScrollToSelection(newPick);
      }
      else if (v instanceof ContainerViewI) {
	Vector svs = 
	  ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.SelectViewI.class);
	for (int j=0; j<svs.size(); j++) {
	  SelectViewI sv = (SelectViewI)svs.elementAt(j);
	  sv.verticalScrollToSelection();
	}
      }
    }
  }

  // These events are fired through the controller and onto other components

  private void    fireBaseFocusEvent(int        focus) {
    BaseFocusEvent evt = new BaseFocusEvent(this,focus,null);

    controller.handleBaseFocusEvent(evt);
  }

  /**
   * I believe this highlights edges in other views that line up with
   * the edges in the selection
   */
  public void highlightEdges(Selection sel, boolean state) {
    if (showEdgeMatches) {
      clearEdges();
      for (int i=0; i<views.size() ; i++) {
        ViewI v = (ViewI)views.elementAt(i);
        if (v instanceof FeatureView) {
          ((FeatureView)v).setMatchingEdges(sel,state);
        } else if (v instanceof ContainerViewI) {
          Vector fvs = ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.FeatureView.class);
          for (int j=0; j<fvs.size(); j++) {
            ((FeatureView)fvs.elementAt(j)).setMatchingEdges(sel,state);
          }
        }
      }
    }
  }

  public void setEdgeMatching(boolean state) {
    showEdgeMatches = state;
    if (state == true) {
      highlightEdges(getSelection(),true);
    } else {
      clearEdges();
    }
    repaint();
  }

  public boolean isShowingEdgeMatches() {
    return showEdgeMatches;
  }

  public void clearEdges() {

    for (int i=0; i<views.size() ; i++) {
      ViewI v = (ViewI)views.elementAt(i);
      if (v instanceof FeatureView) {
        ((FeatureView)v).clearEdges();
      } else if (v instanceof ContainerViewI) {
        Vector fvs = ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.FeatureView.class);
        for (int j=0; j<fvs.size(); j++) {
          ((FeatureView)fvs.elementAt(j)).clearEdges();
        }
      }
    }
  }

  public void mouseWheelMoved(MouseWheelEvent evt) {
    if (focus != null && focus instanceof TierView) {
      TierView tv = (TierView)focus;	     
      tv.moveScrollbarByWheelAmount(evt.getWheelRotation());
    }   
  }   

  /**
   * MouseInputListener routines
   */
  public void mouseEntered(MouseEvent evt) {
    requestFocusInWindow();
  }

  public void mouseExited(MouseEvent evt) {}

  public void mouseClicked(MouseEvent evt) {
    // Middle button: center on selection
    if (MouseButtonEvent.isMiddleMouseClickNoShift(evt)) {
      doMiddleClick(evt);
    }

    // For left button, check that the View under mouse is selectable
    // and do selection
    else if (MouseButtonEvent.isLeftMouseClick(evt)) {
      doLeftClick(evt);
      /*
        The result of a mouse-click is that we need to run a features 
        selection on ourselves. Since we are the event source, this
        amounts to making sure aligning features are highlighted,
        and repainting ourselves. */
      processSelection(getSelection()); // should this be in MouseManager?
    }

    // Right click is handled in mousePressed, for some reason.
  }

  /** If a selection has occurred highlighting of edges and a repaint have to 
      take place. This is for either internal or external selection to use.
      Anything else that has to happen with a selection should be put here. 
      This is called by internal selection (mouse click and rubberbanding), 
      and external selection (handleFeatureSelectionEvent)
  */
  private void processSelection(Selection selection) {
    highlightEdges(selection,true);// light up lined up edges in other views
    repaint();
  }

  private void doLeftClick(MouseEvent evt) {
    mouseManager.doLeftClick(evt);
  }

  /** If focus is selectable its an instance of PickViewI */
  boolean focusIsSelectable() {
    return focus instanceof PickViewI;
  }

  /** If focus is a pick focus, returns the PickViewI */
  public PickViewI getPickViewFocus() {
    if (!focusIsSelectable())
      return null;
    return
      (PickViewI)focus;
  }

  protected void doMiddleClick(MouseEvent evt) {
    Point base;
    ViewI v;
    if ((v = findVisibleLinearView()) != null) {
      base = v.getTransform().toUser(evt.getX(),evt.getY());
    } else {
      logger.warn("No visible linearViews");
      base = ((ViewI)linearViews.elementAt(0)).getTransform().toUser(evt.getX(),evt.getY());
    }
    setLinearCentre(base.x);
    fireBaseFocusEvent(base.x);
  }

  private ViewI findVisibleLinearView() {
    ViewI v = null;
    for (int i=0;i<linearViews.size() && v == null; i++) {
      ViewI tmp = (ViewI)linearViews.elementAt(i);
      if (tmp.isVisible()) {
        v = tmp;
        if (v instanceof ContainerViewI) {
          Vector lvs = ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.ViewI.class);
          if (lvs.size() != 0) {
            v = (ViewI)lvs.elementAt(0);
          }
        }
      }
    }
    return v;
  }

  /**
   * When the mouse button comes down (but is not released) it
   * may be either the right mouse button or the left. The
   * middle mouse button is for clicks only. If it is the
   * right button then popup a menu. If it is the left button
   * then select and start dragging.
   * @param evt
   */
  public void mousePressed(MouseEvent evt) {
    // Clear rubberband in ScaleView
    scaleView.clear();

    if (MouseButtonEvent.isRightMouseClickNoShift(evt)) {
      ViewI view  = getViewAt(new Point(evt.getX(),evt.getY()));
      if (view instanceof PopupViewI) {
        if (getSelection().size() == 0)
          doLeftClick(evt);
        ((PopupViewI)view).showPopupMenu(evt);
      }
    }
    else if (MouseButtonEvent.isLeftMouseClick(evt)) {
      // Save location of press in case a drag begins without a selection being
      // made. In this case we want to make the selection where the mouse
      // started the drag which is where the press occurred.
      if (getSelection().size() == 0) {
	// I dont think we need this select - if no dragging mouseClicked
	// will call doLeftClick, if dragging mouseDragged calls doViewDrag
	// which call mouseClicked which calls doLeftClick.
	// Otherwise this causes 2 doLeftClicks for one left click 
	// in otherwords its a redundant call - MG
        //doLeftClick(evt);
      }
      pressEvent = evt;
    }
  }

  public void mouseReleased(MouseEvent evt) {
    if (MouseButtonEvent.isLeftMouseClick(evt)) {
      if (inDrag) {
        endViewDrag(evt);
      }
    }
    // If it was shift-right mouse, end tier drag
    else if (MouseButtonEvent.isRightMouseClickWithShift(evt)) {
      if (inTierDrag) {
        endTierDrag(evt);
      }
    }
    // NOTE: middle done by rubberband.
  }

  private MouseEvent correctForShift(MouseEvent evt, DragViewI dv) {
    if ((evt.getModifiers() & Event.SHIFT_MASK) != 0) {
      return evt;
    }

    Point dropPosition = new Point(evt.getPoint());

    dropPosition.x = dv.getOriginPosition().x;

    return new MouseEvent((Component)evt.getSource(),0,evt.getWhen(),
                          evt.getModifiers(),
                          dropPosition.x, dropPosition.y,
                          evt.getClickCount(),
                          evt.isPopupTrigger());
  }

  public void endViewDrag(MouseEvent evt) {
    // For left check that the mouse is over a drag receipient
    // and drop on view.
    if (inDrag) {
      DragViewI  dv   = getDragView();

      MouseEvent evt2 = correctForShift(evt,dv);

      Point dropPosition = new Point(evt2.getPoint());

      setFocusView(getViewAt(dropPosition));

      boolean dropSuccessful = false;

      if (focus instanceof DropTargetViewI) {
        dropSuccessful = ((DropTargetViewI)focus).interpretDrop(dv,evt2);
      } else {
        logger.error("Can't drop in " + focus);
      }

      if (dv instanceof DragView) {
        ((DragView)dv).clear();
      }
      remove((ViewI)dv);
      if (dropSuccessful)
        clearSelection();
      inDrag = false;

      if (statusBar != null) {
        statusBar.setActionPane("");
      }
      repaint();
    }
  }

  public void endTierDrag(MouseEvent evt) {
    if (inTierDrag) {
      dragTierView.endTierDrag(evt);
      inTierDrag = false;
      repaint();
    }
  }


  /**
   * There are 2 types of dragging - tierDrag and viewDrag.
   * Tier drag (shift-right mouse) moves the tier to a new place.
   * "ViewDrag" (left mouse or shift left mouse) lets you drag results into
   * the annotation area to create new genes or exons.
   * Shift-dragging adds an exon to an existing gene (?).
   */
  public void mouseDragged(MouseEvent evt) {
    // Shift-right is tier drag

    if (MouseButtonEvent.isRightMouseClickWithShift(evt)) {
      logger.debug("mouseDragged: tier drag event");
      doTierDrag(evt);
    }
    // For left check that the create new view (source view does this) and
    // adds it to this panel as a DragView. Then redraw.
    else if (MouseButtonEvent.isLeftMouseClick(evt)) {
      doViewDrag(evt);
    }
  }

  protected void doTierDrag(MouseEvent evt) {
    if (!inTierDrag) {
      // set focus
      mouseMoved(evt);
      if (focus instanceof TierViewI) {
        TierViewI tv = (TierViewI)focus;
        if (tv.allowsTierDrags()) {
          if (tv.beginTierDrag(evt)) {
            dragTierView = tv;
            inTierDrag = true;
          }
        }
      }
    }
    if (inTierDrag) {
      // Drag the tier
      dragTierView.updateTierDrag(evt);

      repaint();
    }
  }

  protected void doViewDrag(MouseEvent evt) {
    if (!inDrag) {
      if (focus instanceof TierViewI) {
        TierViewI drag_view = (TierViewI) focus;
        Selection selections = drag_view.getViewSelection(getSelection());
        if (selections.size() != 0) {
          DragViewI dvi = drag_view.createDragView(evt, selections);

          if (dvi != null) {
            add((ViewI)dvi,ApolloLayoutManager.NONE);
            inDrag = true;
          } else {
            clearSelection();
          }
        } else {
          // If nothing selected try to select (we should get another
          // drag after this)
          if (pressEvent != null) {
            mouseClicked(pressEvent); // this will do a select
            pressEvent = null;
            mouseDragged(evt);
          }
        }
      }
    }
    if (inDrag) {
      DragViewI dv = getDragView();
      // Drag the view.
      MouseEvent evt2 = correctForShift(evt,dv);
      Point newLocation = new Point(evt2.getPoint());

      dv.setLocation(newLocation);
      mouseMoved(evt2);

      if (focus instanceof DropTargetViewI && statusBar != null) {
        StringBuffer action = new StringBuffer();
	// This method will set up the string buffer
	((DropTargetViewI)focus).interpretDrop(dv,evt2,false,action);
        if (dv instanceof DragView) {
          DragView dfv = (DragView) dv;
          dfv.setInHotspot(!action.toString().equals("No action"));
          dfv.setHotspotType(action.toString().equals("Add evidence") ?
                             DragView.EVIDENCE_HOTSPOT :
                             DragView.OTHER_HOTSPOT);
        }
        statusBar.setActionPane(action.toString());
      }
      repaint();
    }
    // Update status
  }

  /** Gets features under mouse and puts name and position in status bar */
  public void mouseMoved(MouseEvent evt) {

    setFocusView(getViewAt(new Point(evt.getX(),evt.getY())));

    FeatureList newPick = null;

    if (focus instanceof FeatureView) {
      if (focus instanceof SiteView) {
        SiteView siteFocus = (SiteView)focus;
        newPick = siteFocus.findFeatures(evt.getPoint());
      } else {
        FeatureView featFocus = (FeatureView)focus;
        newPick   = featFocus.findFeatures(evt.getPoint());
      }
    }

    if (statusBar != null) {
      if (newPick != null && newPick.size() != 0) {
        SeqFeatureI sf = newPick.getFeature(0);
        // was sf.getName()
	// all idiosyncrasies of naming and label are held
	// in nameadapter (NOT seq feature util which must
	// not have any gui dependencies)
        StringBuffer barText 
	  = new StringBuffer(Config.getDisplayPrefs().getPublicName(sf));
        if (newPick.size() > 1) {
          barText.append(" + others");
        }
        statusBar.setFeaturePane(barText.toString());
      } else {
        statusBar.setFeaturePane("");
      }
    }
    if (statusBar != null && focus != null) {
      int pos = focus.getTransform().toUser(evt.getPoint()).x;
      statusBar.setPositionPane(new String((new Integer(pos+1)).toString()));
    }
  }

  public void changeTierHeights(int change) {
    for (int i=0; i<views.size(); i++) {

      ViewI v = (ViewI)views.elementAt(i);

      if (v instanceof TierViewI) {
        TierViewI tv = (TierViewI)v;
        if (change > 0) {
          tv.incrementTierHeight();
        } else {
          tv.decrementTierHeight();
        }
        tv.setInvalidity(true);
      } else if (v instanceof ContainerViewI) {
        Vector tvs = ((ContainerViewI)v).getViewsOfClass(apollo.gui.genomemap.TierViewI.class);
        for (int j=0; j<tvs.size(); j++) {
          TierViewI tv = (TierViewI)tvs.elementAt(j);
         if (change > 0) {
           tv.incrementTierHeight();
         } else {
           tv.decrementTierHeight();
         }
         tv.setInvalidity(true);
        }
      }
    }
    layout_manager.layoutViews(this);
    repaint();
  }

  /**
   * Keyboard event listeners
   */
  public void keyPressed(KeyEvent evt) {
    char keyChar = evt.getKeyChar();

    if (keyChar == '-') {
      logger.debug("Reducing yspace");
      changeTierHeights(-1);
    } else if (keyChar == '+') {
      logger.debug("Increasing yspace");
      changeTierHeights(1);
    } else if (keyChar == 'r') {
      logger.info("reverse complementing");
      controller.handleReverseComplementEvent(new ReverseComplementEvent(this));    
    } else if (keyChar == 'o') {
      logger.info("changing y orientation");
      controller.handleOrientationEvent(new OrientationEvent(this));
    }

    if (focus instanceof KeyViewI) {
      ((KeyViewI)focus).keyPressed(evt);
    }
  }

  public void keyReleased(KeyEvent evt) {}

  public void keyTyped(KeyEvent evt) {}

  /**
   * Rubberband related methods
   */
  public RubberbandRectangle getRubberband() {
    return this.rubberband;
  }

  public boolean handleRubberbandEvent(RubberbandEvent evt) {
    Rubberband rb = (Rubberband) evt.getSource();

    Selection newSelection = new Selection();
    // Should we return if not this component? what other component could it be?
    if (rb.getComponent() == this) {
      // Get the pickable views in the rectangle
      Vector viewVector = getPickViews(evt.getBounds());

      // Do a pick in each view
      for (int i=0;i<viewVector.size();i++) {
        PickViewI pv = ((PickViewI)viewVector.elementAt(i));
        Selection viewSelection = pv.findFeaturesForSelection(evt.getBounds());
        newSelection.add (viewSelection);
      }
    }
    // If no shift key exclusive selection (deselect others)
    boolean exclusiveSelection = false;
    if ((rb.getModifiers() & Event.SHIFT_MASK) == 0) {
      exclusiveSelection = true;
    }
    // fires FeatureSelectionEvent, edges and repaint done on receiving event
    selectionManager.select(newSelection,exclusiveSelection,this);
    processSelection(getSelection());
    return true;
  }

  /**
   * get the views Vector
   */
  public Vector getViews() {
    return this.views;
  }

  /**
   * find pick views which are at least partly within a specified rectangle
   */
  public Vector getPickViews(Rectangle bounds) {
    return getViews(bounds,pickViews);
  }

  /**
   * find non drag views which are at least partly within a specified rectangle
   */
  public Vector getViews(Rectangle bounds) {
    return getViews(bounds,views);
  }

  public Vector getViews(Rectangle bounds,Vector viewVector) {
    Vector retviews = new Vector();

    for (int i = 0; i < viewVector.size(); i++) {
      ViewI v = ((ViewI)viewVector.elementAt(i));
      if (!(v instanceof DragViewI)) {
        if (v.getBounds().intersects(bounds) && v.isVisible()) {
          retviews.addElement(v);
        }
      }
    }
    return retviews;
  }

  public Vector getDragViews() {
    Vector retviews = new Vector();
    for (int i=0;i<views.size();i++) {
      ViewI v = ((ViewI)views.elementAt(i));
      if (v instanceof DragViewI) {
        retviews.addElement(v);
      }
    }
    return retviews;
  }

  public DragViewI getDragView() {
    Vector dvs = getDragViews();

    if (dvs.size() > 1) {
      logger.warn("Multiple drag views!!!");
    }
    DragViewI dv = ((DragViewI)dvs.elementAt(0));

    return dv;
  }

  public Dimension getPreferredSize() {
    return super.getPreferredSize();
  }

  public void clearSelection() {
    getSelection().clear();
    clearEdges();
  }

  /** Returns a 2 element int array, 0th element is start, 1st element is end.
   * Units are in basepairs. Returns lowest low and highest high of selection
   * Changed from long[] to int[] to be consistent with everything.
   * ints go up to 2 billion which should be plenty for biology - isnt it?
   */
  int[] getSelectionLimits() {
    int [] limits = new int[2];

    limits[0] =  1000000000;
    limits[1] = -1000000000;

    //Vector features = getSelection().getSelectedData();
    FeatureList features = getSelection().getSelectedData();
    for (int i=0; i<features.size(); i++) {
      SeqFeatureI sf = features.getFeature(i);
      if (sf.getLow() < limits[0]) {
        limits[0] = sf.getLow();
      }
      if (sf.getHigh() > limits[1]) {
        limits[1] = sf.getHigh();
      }
    }
    return limits;
  }

  public void setStatusBar(StatusBar sb) {
    statusBar = sb;
  }

  public StatusBar getStatusBar() {
    return statusBar;
  }

  public void finalize() {
    logger.debug("Finalized ApolloPanel " + getName());
  }

  // not used
//   void setLoadInProgress(boolean inProgress) {
//     loadInProgress = inProgress;
//   }
  
  public StrandedZoomableApolloPanel getStrandedZoomableApolloPanel(){
    StrandedZoomableApolloPanel panel = 
      (StrandedZoomableApolloPanel)
      javax.swing.SwingUtilities.getAncestorOfClass(StrandedZoomableApolloPanel.class, this);
    return panel;    
  }
}
