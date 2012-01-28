package apollo.gui.genomemap;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.beans.*;

import apollo.datamodel.*;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.config.Style;
import apollo.config.TierProperty;
import apollo.gui.ApolloFrame;
import apollo.gui.synteny.SyntenyLinkPanel;
import apollo.gui.BaseScrollable;
import apollo.gui.ControlledObjectI;
import apollo.gui.ControlledPanel;
import apollo.gui.Controller;
import apollo.gui.GCScoreCalculator;
import apollo.gui.ScoreCalculator;
import apollo.gui.SgrScoreCalculator;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;
import apollo.gui.TierManagerI;
import apollo.gui.Transformer;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.drawable.DrawableFeatureSet;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.drawable.DrawableSetI;
import apollo.gui.drawable.Drawable;
import apollo.gui.event.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.*;

import misc.JIniFile;

import org.apache.log4j.*;

/**
 * The class which controls and renders the main feature display panel, containing an
 * AnnotationView, ResultView and SiteView for each strand.
 *
 * This class contains ApolloPanel which contains all the views, but it also contains
 * the views as well, which seems confusing. Shouldnt either this or ApolloPanel
 * contain all the views but not both?
 *
 * SiteViews should not be created unless there is sequence - change this.
 In addition to ApolloPanel this also has scrollbars, zoom buttons, & nav bar.
 */
public class StrandedZoomableApolloPanel extends ControlledPanel
      implements ActionListener,
      AdjustmentListener,
      ApolloPanelHolderI,
      BaseFocusListener,
      ControlledObjectI,
      DrawableAnnotationConstants,
      NamedFeatureSelectionListener,
      SetActiveCurStateListener,
      OrientationListener,
      DataLoadListener,
      RubberbandListener,
      ScrollListener,
      //TypesChangedListener,
      ReverseComplementListener,
      ZoomListener
{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(StrandedZoomableApolloPanel.class);
  public static String ZOOMIN = "ZOOMIN";
  public static String ZOOMIN2 = "ZOOMIN2";
  public static String ZOOMOUT = "ZOOMOUT";
  public static String ZOOMOUT2 = "ZOOMOUT2";
  public static String RESET = "RESET";
  public static String CUSTOM = "CUSTOM";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  JPanel               panel2          = new JPanel();

  ApolloPanel          apolloPanel;  //= new ApolloPanel("apolloPanel");
  //MovementPanel        movementPanel;        //= new MovementPanel();
  //NavigatorManager     navManager      = new NavigatorManager();  // inner class

  JScrollBar           hScroll         = new JScrollBar();

  /** ShiftAwareButton is an inner class */
  JButton              zoomin          = new ShiftAwareButton("x2");
  JButton              zoomout         = new ShiftAwareButton("x.5");
  JButton              zoomin2         = new ShiftAwareButton("x10");
  JButton              zoomout2        = new ShiftAwareButton("x.1");
  JButton              reset           = new ShiftAwareButton("Reset");

  StatusBar            statusBar       = null;

  boolean              HSCROLLABLE     = true;
  boolean              ZOOMABLE        = true;
  String               ZOOMPOSITION    = BorderLayout.SOUTH;
  // not used was this supposed to refer to having a navigation manager?
  //boolean              NAVIGABLE       = false;

  String               name            = "";

  /** Whether forward strand is visible */
  private boolean              forwardVisible = true;
  boolean              reverseVisible = true;
  /** Whether forward sites are at present zoom level */
  private boolean      forwardSitesVisibleAtZoomLevel = false;
  /** Whether reverse sites are at present zoom level */
  private boolean      reverseSitesVisibleAtZoomLevel = false;
  /** Whether sites are visible at all, zoomed in or not */
  private boolean      sitesVisibleOnZoom = true;
  boolean              reverseComplement = false;

  ResultView           forwardResultView;
  ResultView           reverseResultView;

  public AnnotationView       forwardAnnotView;
  public AnnotationView       reverseAnnotView;

  SplitterView forwardSplitterView;
  SplitterView reverseSplitterView;
  LaidoutViewContainer forwardContainerView;
  LaidoutViewContainer reverseContainerView;
  LaidoutViewContainer graphContainerView;
  SplitterView strandSplitter;
  SplitterView forwardSv;
  LaidoutViewContainer forwardSplitterScaleContainer;

  ScaleView            scaleView;

  // TranslationView      translationView;

  SiteView             forwardSiteView;
  SiteView             reverseSiteView;

  Controller           controller;
  private SelectionManager selectionManager;

  double               xscale = 1.0;
  boolean              settingScroller = false;
  JLabel zoomLabel = new JLabel("  Zoom factor = "+xscale + "   ");

  Vector highlightRegions = new Vector();

  GuideLine            guide;
  java.util.List<GraphView> graphs = new ArrayList<GraphView>();
  CurationSet          curationSet; // use CurState's curSet?
  private boolean haveSequence = true;

  //  private boolean loadInProgress = false;
  //  private StrandVisibilityListener strandVisibilityListener;
  private RevCompListener revCompListener;

  //if an organism is passed into a curation set, this label will
  //be initialised with name and range.
  private JLabel organismLabel;

  // The scroll hack produces even more non intuitive behaviour than not
  // using it in my view (SMJS).
  boolean useScrollHack = false;

  private boolean annotationViewsVisible;
  private boolean resultViewsVisible;
  private boolean scaleViewVisible=true;

  /** This is for synteny where a scroll propigates from one
      szap to the other("locked scrolling"). */
  private boolean scrollingPropagated=false;
  /** Synteny zoom propagate with shift if true, false without */
  private boolean shiftForLockedZooming=true;
  /** Used to track whether a new curation set has brought in a new style,
      which affects layout issues */
  private Style previousStyle=null;
  private boolean styleChanged=true;

  /** curation that szap is for. has all state info for species */
  private GuiCurationState curationState;

  //  private Color activeColor = new Color("0x00df00");  // bright green--too hard to see 
  private Color activeColor = new Color(0,153,0);  // darker green


  /** Makes zoomable scrollable szap */
  public StrandedZoomableApolloPanel(GuiCurationState curationState, String zoomPosition) {
    this.HSCROLLABLE  = true;
    this.ZOOMABLE     = true;
    // this.NAVIGABLE    = false; // not used anywhere
    this.ZOOMPOSITION = zoomPosition;

    this.curationState = curationState;
    apolloPanel = new ApolloPanel("apolloPanel",curationState);
    //movementPanel = new MovementPanel(curationState);

    // SMJS setController was at end
    setController(curationState.getController());
    setSelectionManager(curationState.getSelectionManager());
    jbInit();
    HighlightDragListener hdl = new HighlightDragListener(this);
    apolloPanel.addMouseListener(hdl);
    apolloPanel.addMouseMotionListener(hdl);
  }

  public void setGraphVisibility(boolean state) {
    getGraphView().setVisible(state);
    apolloPanel.setInvalidity(true);
    apolloPanel.doLayout();
    apolloPanel.setInvalidity(false);
  }

  public void setGuideLine(boolean state) {
    if (state == true) {
      guide = new GuideLine(scaleView);

      addHighlightRegion(guide,
                         Config.getFeatureBackground() == Color.black ? Color.white : Color.black,
                         false);
    } else {
      if (guide != null)
        removeHighlightRegion(guide);
    }
    repaint();
  }

  public CurationSet getCurationSet() {
    return curationSet;
  }

  public void setCurationSet(CurationSet set) {
    // Important to do this, otherwise limit changes to views
    // which produce LIMIT_CHANGED view events will replace
    // all the limits we want to keep.
    this.curationSet = set;

    doStyleChangeCheck();
    boolean previousHaveSequence = haveSequence;

    SequenceI seq = set.getRefSequence();
    haveSequence = (seq != null &&
                    (seq.isLazy() || seq.getResidues() != null));


    if (haveSequence != previousHaveSequence) {
      // result is either adding or removing site views
      removeAllViews();
      addAllViews();
    }

    apolloPanel.setSyncLimits(false);

    setViewColours();
    setFeatureSet(set);
    setAnnotations(set);

    // SMJS Needs sorting

    // 2/8/02: It turns out that some datasets (including example.xml)
    // have features that
    // extend well beyond the range of the actual sequence
    // (neighboring gbunits, in this case).
    // So setSyncLimits was setting the range to (-305899, 602486)
    // instead of (0, seqlength).
    // I created a new setSyncLimits method that
    // takes an additional arg--the length of the
    // sequence--and uses that to set the limits.

    // Sometimes there is no sequence so this will not work!
    // Also this didn't work for non 0 based ranges
    // I think the way to solve this is to set the range in the
    // CurationSet which is meant to have a range - it is
    // a SeqFeature now.
    // Currently the example.xml is returning a bad range for
    // this (map_position in the file is set to a single position)
    // To cater for all current cases this is a bit complicated.
    // ApolloPanel.setSyncLimits pads the limits on each end, this means
    // limits != seq start,end
    if (set.getStart() > 0 && set.getEnd() > 0) {
      apolloPanel.setSyncLimits(true, set.getStart(), set.getEnd());
    } else if (set.length() > 0) {
      apolloPanel.setSyncLimits(true, 1, set.length());
    } else {
      apolloPanel.setSyncLimits(true);
    }
    setScrollValues();

    if (haveSequence) {
      // why do forward and reverse sites separately - arent they always going
      // to be the same?
      forwardSitesVisibleAtZoomLevel = canShowSites(forwardVisible);
      forwardSiteView.setVisible(forwardSitesVisibleAtZoomLevel);
      reverseSitesVisibleAtZoomLevel = canShowSites(reverseVisible);
      reverseSiteView.setVisible(reverseSitesVisibleAtZoomLevel);
    }

    // set zoom factor,
    // boolean true means force zoom factor through without testing bounds
    setZoomFactor(1.0,true);

    // i believe the nav manager allow one to go to the next chunk of data over
    // not sure why this has anything to do with what was formerly browser mode
    // wouldnt this also be handy in editor mode??
    // i think that was there as sort of isEnsembl() kind of method as i think
    // only the ensembl dataadapter probably knows how to work with navman
    // but im just guessing
    // It seems the only dataadapter that sets the genomic range (and makes it nonnull)
    // at the moment is ensemble cgi (and i dont know how to get it to give me
    // data)
    // Changed it to a Config specification
    /*
    if (Config.isNavigationManagerEnabled()) {
      //	    logger.info("Setting nav manager enabled");
      navManager.setEnabled(true);
      //movementPanel.setCurationSet(set);
      movementPanel.setVisible(true);
    } else {
      //	    logger.info("Setting nav manager disabled");
      navManager.setEnabled(false);
    }
    */

    // With a new curation theres potentially a new style which might have a change
    // in annotation and result visibility, setVisible queries config for this
    setVisible(true);
    // putVertScr used to be called from within setFeatureSet - has to be
    // called here as setVisible changes layout with views changing visiblity
    // which would displace the vertical scroll from start
    putVerticalScrollbarsAtStart();

    // new curation - new style - possible change of editing capability
    // disables dropping in annot view if editing disabled
    reverseAnnotView.setEditingEnabled(Config.isEditingEnabled());
    forwardAnnotView.setEditingEnabled(Config.isEditingEnabled());

    if(set != null && set.getOrganism() != null){
      String organism = set.getOrganism();
      String chromosome = set.getChromosome();
      String start = set.getStartAsString();
      String end = set.getEndAsString();
      String label = "";

      if(organism != null){
        label += organism;
      }

      if (chromosome != null){
        label += ":"+chromosome;
      }

      if(start != null){
        label += ":"+start;
      }

      if(end != null){
        label += "-"+end;
      }

      getOrganismLabel().setText(label);
    }//end if
  }

  public boolean handleSetActiveCurStateEvent(SetActiveCurStateEvent evt) {
    if (evt.getNewActiveCurState() != null && 
        evt.getNewActiveCurState().getSZAP() == this) {
      getOrganismLabel().setForeground(activeColor);
    } else if (evt.getOldActiveCurState() != null && 
               evt.getOldActiveCurState().getSZAP() == this) {
      getOrganismLabel().setForeground(Color.black);
    }
    return true;
  }

  private void printViewLimits(String where) {
    logger.trace(where);
    logger.trace("View   Min     Max     VisMin   VisMax");
    logger.trace("forwardResultView   " + forwardResultView.getMinimum() +
                 "  " + forwardResultView.getMaximum());
    logger.trace("  " + forwardResultView.getVisibleRange()[0] + "  " + forwardResultView.getVisibleRange()[1]);
    logger.trace("forwardAnnotView   " + forwardAnnotView.getMinimum() + "  " + forwardAnnotView.getMaximum());
    logger.trace("  " + forwardAnnotView.getVisibleRange()[0] + "  " + forwardAnnotView.getVisibleRange()[1]);
    logger.trace("forwardSiteView   " + forwardSiteView.getMinimum() + "  " + forwardSiteView.getMaximum());
    logger.trace("  " + forwardSiteView.getVisibleRange()[0] + "  " + forwardSiteView.getVisibleRange()[1]);
    logger.trace("reverseResultView   " + reverseResultView.getMinimum() + "  " + reverseResultView.getMaximum());
    logger.trace("  " + reverseResultView.getVisibleRange()[0] + "  " + reverseResultView.getVisibleRange()[1]);
    logger.trace("reverseAnnotView   " + reverseAnnotView.getMinimum() + "  " + reverseAnnotView.getMaximum());
    logger.trace("  " + reverseAnnotView.getVisibleRange()[0] + "  " + reverseAnnotView.getVisibleRange()[1]);
    logger.trace("reverseSiteView   " + reverseSiteView.getMinimum() + "  " + reverseSiteView.getMaximum());
    logger.trace("  " + reverseSiteView.getVisibleRange()[0] + "  " + reverseSiteView.getVisibleRange()[1]);
  }

  public FeatureSetI getForwardResults() {
    return curationSet.getResults().getForwardSet();
  }

  public FeatureSetI getReverseResults() {
    return curationSet.getResults().getReverseSet();
  }
  
  public void setFeatureSet(CurationSet cset) {
    // fset is a StrandedFeatureSetI
    StrandedFeatureSetI fset = cset.getResults();
    if (fset == null) {
      logger.info ("SZAP.setFeatureSet: do not have any results for " + cset.getName());
      return;
    }
    FeatureSetI forward_results = fset.getForwardSet();
    FeatureSetI reverse_results = fset.getReverseSet();

    logger.debug("making feature set for " + forward_results.size() + " forward strand analyses");
    forwardResultView.setDrawableSet(makeDrawableSet(forward_results));
    logger.debug("making feature set for " + reverse_results.size() + " reverse strand analyses");
    reverseResultView.setDrawableSet(makeDrawableSet(reverse_results));

    scaleView.setCurationSet(cset);

    if (haveSequence) {
      // this will create all of the start and stop codons as features
      logger.debug("finding start/stop codons");
      forwardSiteView.setStrand(1);
      forwardSiteView.setCurationSet(cset);
      reverseSiteView.setStrand(-1);
      reverseSiteView.setCurationSet(cset);
    }

    // Calling this with 100 or 101 makes no difference--
    // it still shows up as 101
    getGraphView().setScoreCalculator(new GCScoreCalculator(cset,100));
  }
  
  /** setVisible calls setTextAvoidance(true).
   * It's done here because it was waiting for graphics.
   * Setting textAvoidance to true causes a new layout to happen
   * which will probably have more tiers, thus a side effect of setVisible is
   * to get a larger view.
   */
  public void setVisible(boolean state) {
    super.setVisible(state);

    if (apolloPanel != null) {
      apolloPanel.setVisible(state);

      Graphics g = apolloPanel.getGraphics();

      if (g == null)
        return; // no sense in proceeding

      TierManagerI ftm = forwardResultView.getTierManager();

      if (ftm != null) {
        g.setFont(Config.getDefaultFont());

        ftm.setCharHeight(g.getFontMetrics().getAscent() +
                          g.getFontMetrics().getDescent());
      }

      TierManagerI rtm = reverseResultView.getTierManager();
      if (rtm != null) {
        rtm.setCharHeight(g.getFontMetrics().getAscent() +
                          g.getFontMetrics().getDescent());
      }
      g = apolloPanel.getBackBuffer().getGraphics();
      g.setFont(Config.getDefaultFont());
    }
    // Must be done AFTER the Graphics has been created
    setTextAvoidance(true);

    // If style has changed set up szap according to specs in new style file
    // otherwise keep setup the same
    if (styleHasChanged()) setViewVisibilityToStyleSpecifications();
    //apolloPanel.doLayout(); // this seems redundat with doLayout below

    // This is here to update the site visibility now we have something to show
    if (state == true)
      updateSiteVisibility(true);

    apolloPanel.setInvalidity(true);
    apolloPanel.doLayout();
    apolloPanel.setInvalidity(false);
    this.repaint(); // change of view visibility changes szap
    // The aim of this is to stop the pause on first scroll
    System.gc();
  }

  /** This can only be called once per curation set load. Records state of whether
      the style has actually changed in loading new cur set. */
  private void doStyleChangeCheck() {
    // could check if cur set same to enforce once per load
    Style newStyle = Config.getStyle();
    styleChanged = (newStyle!=previousStyle); // should Config track this?
    previousStyle = newStyle; // this is why only called once
  }

  private boolean styleHasChanged() { return styleChanged; }

  /** This sets all the views scrollbars at start position. This is called
   * after setVisible(true) which causes the views to grow (see note above) and
   * throws any previous attempt at setting the scrollbars at start position.
   * Im not sure whether to go through the views that szap has or to tell apollo
   * panel to go through the views, as both have the views - confusing.
   */
  public void putVerticalScrollbarsAtStart() {
    /* I want to just put this in apolloPanel, but apolloPanel actually only
       has ViewIs, LinearViewIs, and PickViewIs, which dont seem appropriate
       for a vertical scroll method, and it seems overkill to make another
       interface for this, so ill use szap's result and annot views
       apolloPanel.putVerticalScrollbarsAtStart();
    */
    forwardAnnotView.putScrollAtStart();
    reverseAnnotView.putScrollAtStart();
    forwardResultView.putScrollAtStart();
    reverseResultView.putScrollAtStart();
    // site views dont initially appear and dont scroll
  }

  public ApolloPanel getApolloPanel() {
    return this.apolloPanel;
  }

  private DrawableSetI makeDrawableSet(FeatureSetI fset) {
    DrawableSetI dfset = new DrawableFeatureSet(fset, false);
    dfset.setVisible(true);
    return dfset;
  }

  private DrawableSetI makeDrawableAnnotatedSet(FeatureSetI fset) {

    DrawableSetI dfset = new DrawableFeatureSet(fset, false);

    dfset.setVisible(true);

    return dfset;
  }

  public FeatureSetI getForwardAnnotations() {
    return curationSet.getAnnots().getForwardSet();
  }

  public FeatureSetI getReverseAnnotations() {
    return curationSet.getAnnots().getReverseSet();
  }

  public void setAnnotations(CurationSet curation) {
    StrandedFeatureSetI annots = curation.getAnnots();

    if (annots == null) {
      logger.info("setAnnotations: no annotations.");
      return;
    }

    FeatureSetI forward_annots = annots.getForwardSet();
    FeatureSetI reverse_annots = annots.getReverseSet();

    DrawableSetI drawForward = makeDrawableAnnotatedSet(forward_annots);
    DrawableSetI drawReverse = makeDrawableAnnotatedSet(reverse_annots);

    forwardAnnotView.setStrand(1);
    forwardAnnotView.setDrawableSet(drawForward);

    reverseAnnotView.setStrand(-1);
    reverseAnnotView.setDrawableSet(drawReverse);

    /* because the annotation view is the one that instantiates
       the annotation editor and the annotation editor needs to
       have access to the curation set (for naming and such)
       the curation set needs to be passed to the view so it
       can hand it to the editor - a typical apollo convolution */
    forwardAnnotView.setCurationSet(curation);
    reverseAnnotView.setCurationSet(curation);

    setEvidenceFinder(drawForward.getFeatureSet(),
                      forwardAnnotView.getEvidenceFinder());
    setEvidenceFinder(drawReverse.getFeatureSet(),
                      reverseAnnotView.getEvidenceFinder());
  }

  // This is very experimental and will need refactoring. I'm seeing how
  // well evidence is working. It's been out of use for a while but it
  // seems to be working sort of OK. The big change is that the 
  // EvidenceFinder is holding model objects so to get the Drawables for
  // them (which are in a different view to the annotations) I made this
  // method. Need to think about how best to do it.
  // As ensj is the only adapter except game xml which loads any evidence
  // and the game xml adapter code indicates its not used, and that the only
  // place this is called is from my DrawablePhaseHighlightGeneFeatureSet,
  // I think its safe to leave this in for now.
  public Drawable findDrawableForFeatureInResultViews(SeqFeatureI sf) {
    Drawable d = null;

    if (sf ==null) {
      return null;
    }
    if (forwardResultView.getDrawableSet() != null) {
      d = forwardResultView.getDrawableSet().findDrawable(sf);
    }
    if (d != null) return d;
    if (reverseResultView.getDrawableSet() != null) {
      d = reverseResultView.getDrawableSet().findDrawable(sf);
    }
    return d;
  }

  private void setEvidenceFinder(FeatureSetI fs, EvidenceFinder finder) {
    if (finder != null) {
      int feat_count = fs.size();
      for (int i = 0; i < feat_count; i++) {
        AnnotatedFeatureI annot = (AnnotatedFeatureI) fs.getFeatureAt(i);
        annot.setEvidenceFinder(finder);
      }
    }
    else {
      logger.info ("setEvidenceFinder: no evidence for FeatureSet " + fs.getName());
    }
  }

  // cur state has this - take out? make private?
  public FeatureSetI getAnnotations() {
    return curationSet.getAnnots();
  }

  public FeatureSetI getResults() {
    return curationSet.getResults();
  }

  public void zoomToSelection() {
    // Default (additional) window size around selection is 0 (not counting a
    // small fudge factor added in by zoomToSelectionWithWindow)
    zoomToSelectionWithWindow(0);
  }

  /** If zoomToSelectionWithWindow is called with no centerBase argument, make
   *  centerBase -1 to let the real method know that. */
  public void zoomToSelectionWithWindow(int window) {
    zoomToSelectionWithWindow(window, -1);
  }

  // window is TOTAL that is to be added to the selected range,
  // so if you want, say, +/-5000, you'd use a window of 10000.
  public void zoomToSelectionWithWindow(int window, int centerBase) {
    if (apolloPanel.getSelection().size() == 0)
      return;

    setSelectedViewVisible(); // if sel strand hidden make vis
    int [] limits = apolloPanel.getSelectionLimits();
    double newWidth = (double)Math.max(limits[1] - limits[0],5) + window;

    // Making the new zoom a bit smaller so the feature wont run into the
    // scrollbar - currently the views width includes the scrollbar area
    // and getting it to think otherwise would mean views that dont have
    // scrollbars(scaleView) would stop where scrollbars stop. I think it
    // would take a bit of work to get this right, so the fudge works for now
    // seems to me the way to do it would be to have all the views share the
    // same x transform which is based on a view with a scrollbar
    double fudgeForScrollbar = 0.95;
    double curWidth = (double)apolloPanel.getVisibleBasepairWidth();
    double oldFactor = getZoomFactor();
    double newFactor = xscale * fudgeForScrollbar * curWidth/newWidth;

    setZoomFactor(newFactor);

    int padForScrollbar = (int)(newWidth/100.0);

    //    int centerBase = (limits[0] + limits[1])/2 + padForScrollbar;
    // If centerBase is -1, that means it was not specified--calculate it now.
    if (centerBase < 0)
      centerBase = (limits[0] + limits[1])/2 + padForScrollbar;

    logger.debug("zoomtoselectionwithWindow: window = " + window + ", center base = " + centerBase + 
                 ", limits[0] = " + limits[0] + ", limits[1] = " + limits[1] + ", newWidth = " + newWidth 
                 + ", curWidth = " + curWidth);

    setCentreBase(centerBase);

    // zooming might cause selected feature to vertically scroll off
    // Why is this not always working??
    apolloPanel.verticalScrollToSelection();

    // SMJS Added this call - I noticed that in synteny view zoom with Ctrl-Z wasn't being propagated to SyntenyLinkPanel
    controller.handleZoomEvent(new ZoomEvent(this,  
                                             !shiftForLockedZooming(), //propagateZoom(button)
                                             CUSTOM,
                                             newFactor/oldFactor));
  }

  /** If selection in a strand thats not visible, make strand visible
      This should also makes sure the view is visible as well.
      It doesnt deal with SiteView - do we need to? assumes non annots are
      results.
  */
  private void setSelectedViewVisible() {
    if (forwardVisible && reverseVisible) return;

    //Vector features = apolloPanel.getSelection().getSelectedData();
    FeatureList features = apolloPanel.getSelection().getSelectedData();
    // if both strands have been made visible we can stop iterating
    for (int i = 0;
         i < features.size() && (!forwardVisible || !reverseVisible);
         i++) {
      SeqFeatureI sf = features.getFeature(i);
      if (sf.getStrand() == 1 && !forwardVisible)
        setForwardVisible(true);
      else if (sf.getStrand() == -1 && !reverseVisible)
        setReverseVisible(true);

      if (sf.hasAnnotatedFeature()) {
        if (!areAnnotationViewsVisible())
          setAnnotationViewsVisible(true);
      }
      else
        if (!areResultViewsVisible())
          setResultViewsVisible(true);
    }
  }

// pase' - delete? or will this come into play with synteny?
  /** For now only one listener, if need more then one listener in future
      make this addStrandVisibilyListener and add to vector */
//   public void setStrandVisibilityListener(StrandVisibilityListener l) {
//     strandVisibilityListener = l;
//   }

//   public StrandVisibilityListener getStrandVisibilityListener(){
//     return strandVisibilityListener;
//   }

//   private void notifyStrandVisibility(boolean isForward) {
//     strandVisibilityListener.strandIsVisible(isForward);
//   }

  public void setRevCompListener(RevCompListener l) {
    revCompListener = l;
  }

  public RevCompListener getRevCompListener(){
    return revCompListener;
  }

  private void notifyRevCompListener() {
    if (revCompListener != null)
      revCompListener.updateRevComp(isReverseComplement());
  }

  public void scrollToSelection() {
      apolloPanel.verticalScrollToSelection();
  }

  private void zoomToWidth(int[] limits) {
    // dispWidth is just the subtraction of the limits as far as i can tell
    zoomToWidth(limits[1] - limits[0], limits);
  }


  // isnt the dispWidth just the subtraction of the 2 ints in limits, unclear to me
  // why this needs to be passed in as well
  public void zoomToWidth(double dispWidth, int [] limits) {
    // curWidth is width in base pairs
    double curWidth = (double)apolloPanel.getVisibleBasepairWidth();
    double zoomFactor = curWidth/dispWidth * xscale;

    setZoomFactor(zoomFactor);
    setCentreBase(limits[0]+(int)dispWidth/2);

    // This should not propagate to other controllers, hence the 'false' 
    controller.handleZoomEvent(new ZoomEvent(this, false,CUSTOM,zoomFactor));
  }


  /**
   * Searches features for name and selects all that are found, does nothing
   * if none are found. Zooms to the new selection. Should scroll to it as well
   * Returns false if no features are found by name
   * This code was taken from FindPanel and modified
   * window is how much padding to allow in the zoom.
   */
  public boolean selectFeaturesByName(String name, int window) {
    return selectFeaturesByName(name, window, false);
  }

  /** window is how much padding to allow in the zoom. */
  public boolean selectFeaturesByName(String name, int window, boolean useRegExp) {
    if (name==null || name.equals(""))
      return false;

    // Special hack:  if name is an URL representing a request for a gene,
    // get the gene name out.
    if (name.startsWith("http")) {
      int genePos = name.indexOf("gene=");
      if (genePos > 0) {
        name = name.substring(genePos+5);
        if (name.indexOf("&") > 0)
          name = name.substring(0, name.indexOf("&"));
      }
    }

    FeatureList feats = null;//the set of found features

    // Check annotations first
    // getAnnotations returns a FeatureSetI(model) of the annots
    //only call findFeaturesByName(name) for nonregexp searches
    //(since I don't want tohave to add findFeaturesByName(String, boolean)
    //to FeatureSetI and possibly break tons of code.
    if(!useRegExp)
      feats = getAnnotations().findFeaturesByName(name);
    if (feats == null || feats.isEmpty()){
      //Call the method that looks for pattern as a prefix,
      //but treat name as plain old string, not RegExp, if useRegExp is false
      feats = getAnnotations().findFeaturesByAllNames(name, useRegExp);
    }
    // Now check results
    // findFeaturesByAllNames checks hit names and regular names
    feats.addAllFeatures(getResults().findFeaturesByAllNames(name,useRegExp));
    // If we don't find name, look for name*
    if (feats.isEmpty())
      //Call the method that looks for pattern as a prefix,
      //but treat name as plain old string, not RegExp, if useRegExp is false
      feats = getResults().findFeaturesByAllNames(name, useRegExp);

    // No features found, return false
    if (feats.isEmpty())
      return false;

    // Shouldnt have to iterate through FeatureSet. Should be able to
    // tell FeatureSet that its selected in selection event.
    // Trying this theory out by commenting out the feature set iterating.
    // Selection seems to happen fine with just sending the FeatureSet itself.
    // Does anyone see any potential problems with this?
    selectionManager.select(feats,true,false,this);

    zoomToSelectionWithWindow(window); // zoom in on new selection
    // zoomToSelection scrolls after zoom as zoom changes view

    return true; // name is found
  }

  /** Called from constructor only. Initializes szap. */
  private void jbInit() /*throws Exception*/ {
    //    Rectangle tmpBounds = new Rectangle(1,1,1,1);

    JPanel pp1   = new JPanel();          // Contains the hscrolling panel
    JPanel pp2   = new JPanel();          // Contains the zooming panel
    JPanel pp3   = new JPanel();          // Contains the organism label and pp2

    JLabel hlabel = new JLabel("Position");
    JLabel zlabel = new JLabel("Zoom  ");

    this.  setLayout(new BorderLayout());
    panel2.setLayout(new GridLayout(2,1));
    apolloPanel.setLayout(new ColumnApolloLayout());
    //apolloPanel.setSelectionManager(selectionManager);

    // Should site view only get created on demand if we have data with sequence?
    reverseSiteView = new SiteView(apolloPanel,"reverse sites",
                                   getController(),
                                   selectionManager);

    reverseResultView = new ResultView(apolloPanel,
                                       "reverse results",
                                       selectionManager);

    reverseAnnotView = new AnnotationView(apolloPanel,
                                          "reverse annotations",
                                          selectionManager,curationState);
    reverseSplitterView = new SplitterView(apolloPanel,
                                           "reverse splitter",
                                           true,
                                           reverseAnnotView,
                                           reverseResultView);

    scaleView = new ScaleView(apolloPanel,"sv");

    forwardResultView = new ResultView(apolloPanel,
                                       "forward results",
                                       selectionManager);
    forwardAnnotView = new AnnotationView(apolloPanel,
                                          "forward annotations",
                                          selectionManager,curationState);
    forwardSplitterView = new SplitterView(apolloPanel,
                                           "forward splitter",
                                           true,
                                           forwardResultView,
                                           forwardAnnotView);


    forwardSiteView = new SiteView(apolloPanel,
                                   "forward sites",
                                   getController(),
                                   selectionManager);

    GraphView g = new GraphView(apolloPanel, "graphView"); 
    g.addVisibilityListener(new GraphVisibilityListener());
    graphs.add(g);
    
    forwardContainerView = new LaidoutViewContainer(getApolloPanel(),
                                                    new ColumnApolloLayout(),
                                                    "forward container view");

    graphContainerView = new LaidoutViewContainer(apolloPanel, new ColumnApolloLayout(), "graphs");
    graphContainerView.add(getGraphView(), ApolloLayoutManager.BOTH);
    
    forwardSplitterScaleContainer = new LaidoutViewContainer(apolloPanel, new ColumnApolloLayout(), "forward splitter-scale container");
    
    forwardSplitterScaleContainer.add(forwardSplitterView, ApolloLayoutManager.BOTH);
    forwardSplitterScaleContainer.add(scaleView, ApolloLayoutManager.HORIZONTAL);
    //forwardContainerView.add(forwardSplitterView, ApolloLayoutManager.BOTH);
    //forwardContainerView.add(scaleView, ApolloLayoutManager.HORIZONTAL);
    
    forwardSv = new SplitterView(apolloPanel, "forward splitter view", true, forwardSplitterScaleContainer, graphContainerView);
    forwardContainerView.add(forwardSv, ApolloLayoutManager.BOTH);
    graphContainerView.setVisible(false);
    
    //forwardContainerView.add(getGraphView(), ApolloLayoutManager.HORIZONTAL);
    /*
    for (GraphView g : getGraphs()) {
    	forwardContainerView.add(g, ApolloLayoutManager.HORIZONTAL);
    }
    */

    /*
    testGraph = new DiscreteGraphView(apolloPanel, "test", Color.LIGHT_GRAY);
    forwardContainerView.add(testGraph, ApolloLayoutManager.HORIZONTAL);
    testGraph.setScoreCalculator(new SgrScoreCalculator(curationSet, 100));
    testGraph.setVisible(true);
    */
    
    reverseContainerView = new LaidoutViewContainer(getApolloPanel(),
                                                    new ColumnApolloLayout(),
                                                    "reverse container view");
    reverseContainerView.add(reverseSplitterView, ApolloLayoutManager.BOTH);

    strandSplitter = new SplitterView(apolloPanel,"strand split",true,
                                      forwardContainerView,
                                      reverseContainerView);

    forwardResultView.setStrand(1);
    reverseResultView.setStrand(-1);

    forwardAnnotView.setYOrientation(Transformer.UP);
    forwardResultView.setYOrientation(Transformer.UP);

    addAllViews();

    forwardAnnotView.registerDragSource(forwardResultView);
    forwardAnnotView.registerDragSource(forwardAnnotView);
    forwardAnnotView.registerDragSource(forwardSiteView);
    forwardAnnotView.setResultView(forwardResultView);
    forwardAnnotView.setSiteView(forwardSiteView);

    reverseAnnotView.registerDragSource(reverseResultView);
    reverseAnnotView.registerDragSource(reverseSiteView);
    reverseAnnotView.registerDragSource(reverseAnnotView);
    reverseAnnotView.setResultView(reverseResultView);
    reverseAnnotView.setSiteView(reverseSiteView);

    forwardResultView.setAnnotationView(forwardAnnotView);
    reverseResultView.setAnnotationView(reverseAnnotView);

    forwardSiteView.setResultView(forwardResultView);
    reverseSiteView.setResultView(reverseResultView);

    if (HSCROLLABLE == true) {
      hScroll.setOrientation(JScrollBar.HORIZONTAL);
      hScroll.addAdjustmentListener(this);

      // A GridBagLayout is used to minimize the Y space the
      // scroller uses.
      pp1.setLayout(new GridBagLayout());

      GridBagConstraints gbc = new GridBagConstraints();

      gbc.fill       = GridBagConstraints.HORIZONTAL;
      gbc.weighty    = 0.0;
      gbc.weightx    = 0.0;
      gbc.gridheight = 1;
      gbc.gridwidth  = 1;

      pp1.add(hlabel, gbc);

      gbc.weightx   = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;

      pp1.add(hScroll, gbc);

    }

    if (ZOOMABLE == true) {
      GridBagConstraints gbc = new GridBagConstraints();

      gbc.fill       = GridBagConstraints.HORIZONTAL;
      gbc.weighty    = 0.0;
      gbc.weightx    = 0.0;
      gbc.gridheight = 1;
      gbc.gridwidth  = 1;

      pp2.setLayout(new GridBagLayout());


      pp2.add(zlabel,gbc);
      pp2.add(zoomin2,gbc);
      pp2.add(zoomin,gbc);
      pp2.add(zoomout,gbc);
      pp2.add(zoomout2,gbc);
      pp2.add(reset,gbc);

      gbc.weightx   = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      pp2.add(zoomLabel,gbc);

      zoomin.  addActionListener(this);
      zoomin2. addActionListener(this);
      zoomout. addActionListener(this);
      zoomout2.addActionListener(this);
      reset   .addActionListener(this);
    }

    //
    //at point of creation I probably don't know what the curation set is.
    pp3.setLayout(new GridBagLayout());
    organismLabel = new JLabel("");

    GridBagConstraints gbc = new GridBagConstraints();

    gbc.weighty = 0.0;
    gbc.weightx = 0.0;
    gbc.gridheight = 1;
    gbc.gridwidth  = 1;
    gbc.fill = GridBagConstraints.NONE;

    pp3.add(pp2,gbc);

    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;

    pp3.add(organismLabel,gbc);

    if(getCurationSet()!= null && getCurationSet().getOrganism() != null){
      String organism = getCurationSet().getOrganism();
      String chromosome = getCurationSet().getChromosome();
      String start = getCurationSet().getStartAsString();
      String end = getCurationSet().getEndAsString();
      organismLabel.setText("  "+organism+":"+chromosome+":"+start+"-"+end);
    }//end if

    panel2.add(pp1);
    panel2.add(pp3);

    this.add(apolloPanel, BorderLayout.CENTER);

    if (HSCROLLABLE || ZOOMABLE) {
      this.add(panel2 , ZOOMPOSITION);
    }

    setViewVisibilityToStyleSpecifications();

    if (ZOOMPOSITION.equals(BorderLayout.SOUTH)) {
      this.add(curationState.getNavigationBar(), BorderLayout.NORTH);
    } else {
      this.add(curationState.getNavigationBar(), BorderLayout.SOUTH);
    }
  }


  public void setControlPanelVisibility(boolean state) {
    logger.debug("setting control panel visibility to " + state);
    panel2.setVisible(state);
  }

  public boolean isControlPanelVisible() {
    return panel2.isVisible();
  }

  /** Set view visibility to how they are specified in the style. This is for
      initialization(jbInit) as well as for resetViews. */
  private void setViewVisibilityToStyleSpecifications() {
    setAnnotationViewsVisible(Config.getShowAnnotations());
    setResultViewsVisible(Config.getShowResults());
    setScaleViewVisible(true); // ?? config?
    // this causes a null ptr because it triggers a doLayout that view arent
    // ready for - dont have a stacker yet in jbinit - still at construct time
    // if (!inInitialization) ??
    //setSiteViewVisible(Config.getStyle().getInitialSitesVisibility());
    // siteViewVisible(config,doLoad=false);
    sitesVisibleOnZoom = Config.getStyle().getInitialSitesVisibility() && haveSequence;
    setScrollingPropagated(Config.getStyle().initialLockedScrolling());
    setShiftForLockedZooming(Config.getStyle().initialShiftForLockedZooming());
  }

  public void setViewColours() {
    reverseResultView.setBackgroundColour(Config.getFeatureBackground());
    forwardResultView.setBackgroundColour(Config.getFeatureBackground());
    forwardSiteView.setBackgroundColour(Config.getFeatureBackground());
    reverseSiteView.setBackgroundColour(Config.getFeatureBackground());

    forwardAnnotView.setBackgroundColour(Config.getAnnotationBackground());
    reverseAnnotView.setBackgroundColour(Config.getAnnotationBackground());

    scaleView.setBackgroundColour(Config.getCoordBackground());
    scaleView.setForegroundColour(Config.getCoordForeground());
    getGraphView().setForegroundColour(Config.getCoordForeground());
    getGraphView().setPlotColour(Config.getCoordForeground());
    getGraphView().setBackgroundColour(Config.getCoordBackground());
  }

  private boolean canShowSites(boolean viewIsVisible) {
    if (!sitesVisibleOnZoom) return false;
    //    if (!haveSequence)
    //      return false;
    int siteShowLim = Config.getSiteShowLimit();
    boolean showSites = (forwardResultView.getTransform().getXCoordsPerPixel() <= siteShowLim && viewIsVisible);

    if (!showSites)
      return false;

    // Try to figure out if we have sequence--didn't want to force it to load before, but now it's ok
    int [] visRange = forwardSiteView.getVisibleRange();
    if (curationSet != null && curationSet.getRefSequence()!=null && curationSet.getRefSequence().getResidues(visRange[0], visRange[1]) != null)
      return true;
    else
      return false;
  }

  private void updateSiteVisibility(boolean force) {
    if (!haveSequence) 
      return;

    boolean update = false;

    boolean show_sites = canShowSites(forwardVisible);
    if (forwardSitesVisibleAtZoomLevel != show_sites || force == true) {
      forwardSitesVisibleAtZoomLevel = show_sites;
      forwardSiteView.setVisible(forwardSitesVisibleAtZoomLevel);
      update = true;
    }

    show_sites = canShowSites(reverseVisible);
    if (reverseSitesVisibleAtZoomLevel != show_sites || force == true) {
      reverseSitesVisibleAtZoomLevel = show_sites;
      reverseSiteView.setVisible(reverseSitesVisibleAtZoomLevel);
      update = true;
    }

    if (update) {
      apolloPanel.setInvalidity(true);
      apolloPanel.doLayout();
      apolloPanel.setInvalidity(false);
    }
  }

  public void setForwardVisible(boolean state) {
    setStrandVisible(forwardAnnotView,forwardResultView,1,state);
  }

  public void setReverseVisible(boolean state) {
    setStrandVisible(reverseAnnotView,reverseResultView,-1,state);
  }

  public AnnotationView getAnnotView(int strand) {
    return (strand == 1 ? forwardAnnotView : reverseAnnotView);
  }

  private void removeAllViews() {
    apolloPanel.remove(forwardSiteView);
    apolloPanel.remove(strandSplitter);
    apolloPanel.remove(reverseSiteView);
  }


  /**
   * This sets up the constraints for all the views. Annotation views was previously
   * constrained horizontally but not vertically, which made it a bit of a space hog
   * when theres a lot of annotation. Its been changed to be constrained vertically
   * (as well as horizontally). Its unclear if this will work properly as it seems
   * AnnotationView is set up to be unconstrained vertically, so it might need more
   * work, or might have to be scrapped...
   * I got it to work but AnnotationView had to be tweeked to check the constraint
   * that it was added with, which seems improper but works for now - should
   * probably revisit this.
   *
   */
  private void addAllViews() {

    // yikes - hack hack
    String resultConstraint = ApolloLayoutManager.BOTH;

    apolloPanel.setInvalidity(true);

    if (reverseComplement) {
      apolloPanel.add(reverseSiteView,ApolloLayoutManager.HORIZONTAL);
    } else {
      apolloPanel.add(forwardSiteView,ApolloLayoutManager.HORIZONTAL);
    }
    apolloPanel.add(strandSplitter,resultConstraint);
    if (reverseComplement) {
      apolloPanel.add(forwardSiteView,ApolloLayoutManager.HORIZONTAL);
    } else {
      apolloPanel.add(reverseSiteView,ApolloLayoutManager.HORIZONTAL);
    }

    apolloPanel.setScaleView(scaleView);

    apolloPanel.invalidate();
    apolloPanel.validate();

    apolloPanel.setInvalidity(false);
  }

  public boolean isForwardStrandVisible() { return forwardVisible; }
  public boolean isReverseStrandVisible() { return reverseVisible; }

  private void setStrandVisible(AnnotationView av, ResultView rv,
                                int strand, boolean state) {
    apolloPanel.setInvalidity(true);

    //if (Config.getShowAnnotations()) { //&& (force || annotationViewsVisible)) {
    //if (annotationViewsVisible) {
      // annotationViewsVisible should reflect state of view menu item
    av.setVisible(state && (annotationViewsVisible));
      //}
    //if (resultViewsVisible) { //Config.getShowResults()) {
      // resultViewsVisible should reflect state of view menu item
    rv.setVisible(state && resultViewsVisible);
    //}

    // clearSelection does a repaint for the apolloPanel
    // why do we need to clear selections on setting a strand visible
    // this is problematic if the reason why we are setting the strand visible
    // is that a feature it contains has been selected (eg find)
    // should it clear selections on just set vis false?
    // apolloPanel.clearSelection();

    // I thought this fixed the revcomp-middle-click-not-centering bug, but apparently not.
    // Do we want it?
    // apolloPanel.clearEdges();

    if (strand == 1) {
      forwardVisible = state;
      forwardSplitterView.setVisible(state);
      forwardContainerView.setVisible(state);
      if (state == false &&
          forwardContainerView.getViewsOfClass(apollo.gui.genomemap.ScaleView.class).size() > 0) {
        forwardContainerView.remove(scaleView);
        //forwardContainerView.remove(graphView);
        //forwardContainerView.remove(translationView);

        //need to remove any other graphs from forward strand
        for (GraphView g : graphs) {
        	forwardContainerView.remove(g);
        }
        
        if (reverseComplement) {
          reverseContainerView.add(scaleView,ApolloLayoutManager.HORIZONTAL);
          //reverseContainerView.add(graphView,ApolloLayoutManager.HORIZONTAL);
          //reverseContainerView.add(translationView,ApolloLayoutManager.HORIZONTAL);
          
          //need to add any other graphs to reverse strand
          for (GraphView g: graphs) {
        	  reverseContainerView.add(g, ApolloLayoutManager.HORIZONTAL);
          }
          
        } else {
          reverseContainerView.addFirst(scaleView,ApolloLayoutManager.HORIZONTAL);
          //reverseContainerView.addFirst(graphView,ApolloLayoutManager.HORIZONTAL);
          //reverseContainerView.addFirst(translationView,ApolloLayoutManager.HORIZONTAL);

          //need to add any other graphs to reverse strand
          for (GraphView g: graphs) {
        	  reverseContainerView.addFirst(g, ApolloLayoutManager.HORIZONTAL);
          }
        }
      }
    } else {
      reverseVisible = state;
      reverseSplitterView.setVisible(state);
      reverseContainerView.setVisible(state);
      if (state == false &&
          reverseContainerView.getViewsOfClass(apollo.gui.genomemap.ScaleView.class).size() > 0) {
        reverseContainerView.remove(scaleView);
        //reverseContainerView.remove(graphView);
        // reverseContainerView.remove(translationView);

        for (GraphView g: graphs) {
      	  reverseContainerView.remove(g);
        }

        if (reverseComplement) {
          forwardContainerView.addFirst(scaleView,ApolloLayoutManager.HORIZONTAL);
          //forwardContainerView.addFirst(graphView,ApolloLayoutManager.HORIZONTAL);
          // forwardContainerView.addFirst(translationView,ApolloLayoutManager.HORIZONTAL);

          for (GraphView g: graphs) {
          	  forwardContainerView.addFirst(g, ApolloLayoutManager.HORIZONTAL);
          }

        
        } else {
          forwardContainerView.add(scaleView,ApolloLayoutManager.HORIZONTAL);
          //forwardContainerView.add(graphView,ApolloLayoutManager.HORIZONTAL);
          // forwardContainerView.add(translationView,ApolloLayoutManager.HORIZONTAL);

          for (GraphView g: graphs) {
          	  forwardContainerView.add(g, ApolloLayoutManager.HORIZONTAL);
          }
        
        }
      }
    }
    // true means force an update even if they already are visible
    updateSiteVisibility(true);
  }

  public void setController(Controller controller) {
    this.controller = controller;
    controller.addListener(this);
    apolloPanel.setController(controller);
    //movementPanel.setController(controller);
  }

  public boolean handleOrientationEvent(OrientationEvent evt) {
    changeYOrientation();
    return false;
  }

  /**
   * If we are handed a feature selected only by name, then -
   * as long as we are not the source of the event, we will try to find the
   * features in our result set which correspond to the named features.
   * Then we create a FeatureSelectionEvent containing the features we found,
   * and re-throw this event back at the controller.
  **/
  public boolean handleNamedFeatureSelectionEvent(NamedFeatureSelectionEvent theEvent) {
    String[] selectionNames = theEvent.getNames();
    int nameCounter;
    FeatureList foundFeatures = new FeatureList();

    for(nameCounter = 0; nameCounter < selectionNames.length; nameCounter++){
      if(!Range.NO_NAME.equals(selectionNames[nameCounter])){
        String nm = selectionNames[nameCounter];
        // SMJS I added a condition around this because it was eating cycles
        //      in SyntenyLinkPanel selections, because findFeaturesByAllNames
        //      uses a regex matcher which is doing all sorts of slow things
        //      under the hood (lower casing the string etc). The only other
        //      place this is used currently is ApolloJalviewEventBridge and
        //      for which I don't know what find is needed so I've left that
        //      doing the find the slow way
        if (theEvent.getSource() instanceof SyntenyLinkPanel) { 
          foundFeatures.addAllFeatures(getResults().findFeaturesByName(nm,true));
          foundFeatures.addAllFeatures(getAnnotations().findFeaturesByName(nm,true));
        } else {
          foundFeatures.addAllFeatures(getResults().findFeaturesByAllNames(nm));
          foundFeatures.addAllFeatures(getAnnotations().findFeaturesByAllNames(nm));
        }
      }
    }//end for

    getSelectionManager().select(
        foundFeatures,
        true, //exclusive
        false,//select parents
        theEvent.getSource()
    );

    return true;
  }//end handleNamedFeatureSelectionEvent

  public void changeYOrientation() {

    reverseAnnotView.setYOrientation(reverseAnnotView.getTransform().getYOrientation() == Transformer.UP ?
                                                 Transformer.DOWN :
                                                 Transformer.UP );
    reverseResultView.setYOrientation(reverseResultView.getTransform().getYOrientation() == Transformer.UP ?
                                                 Transformer.DOWN :
                                                 Transformer.UP );
    forwardAnnotView.setYOrientation(forwardAnnotView.getTransform().getYOrientation() == Transformer.UP ?
                                                 Transformer.DOWN :
                                                 Transformer.UP );
    forwardResultView.setYOrientation(forwardResultView.getTransform().getYOrientation() == Transformer.UP ?
                                                 Transformer.DOWN :
                                                 Transformer.UP );
    setScrollValues();

    repaint();
  }

  public void setSelectionManager(SelectionManager sm) {
    selectionManager = sm;
  }

  //  private   int                baseWindowStart = 7000;
  //  private   int                baseWindowWidth = 1000;

  public void paintComponent(Graphics g) {
    // JC: Enabling this because it may be of use for debugging.  Obviously this 
    // method is time-critical, but Log4J is heavily optimized to make this check 
    // very fast:
    if (logger.isTraceEnabled()) {
      printViewLimits("paintComponent");
    }
    super.paintComponent(g);
  }

  public ScaleView getScaleView() {
    return scaleView;
  }
  public GraphView getGraphView() {
    return graphs.get(0);
  }
  /** true if GraphView is visible */
  public boolean getGraphVisibility() { return getGraphView().isVisible(); }

  public java.util.List<GraphView> getGraphs()
  {
	  return graphs;
  }
  
  public void addGraph(ScoreCalculator s, boolean isDiscrete, Color color)
  {
    GraphView g = isDiscrete ? new DiscreteGraphView(apolloPanel, "graph", color, scaleView.getTransform()) : null;
    //forwardContainerView.add(g, ApolloLayoutManager.HORIZONTAL);
    Vector views = forwardContainerView.getViews();
    
    //SplitterView sv = new SplitterView(apolloPanel, "test", true, (ViewI)(views.get(views.size() - 1)), g);
    //SplitterView sv = new SplitterView(apolloPanel, "test", true, graphs.get(graphs.size() - 1), g);
    //forwardContainerView.add(sv, ApolloLayoutManager.BOTH);
    graphContainerView.add(g, ApolloLayoutManager.BOTH);
    g.addVisibilityListener(new GraphVisibilityListener());
    getGraphs().add(g);
	  g.setScoreCalculator(s);
	  g.setVisible(true);
    
	  //this is a nasty hack -- for some reason when adding this without forcing another redraw of the display, the visible range goes crazy...
	  //can't figure out why (sigh)
	  doScrolling(hScroll.getValue());
  }

  public Rectangle calculateHighlightRect(HighlightWrapper hw) {
    Transformer transformer = scaleView.getTransform();

    // SMJS Removed +1
    // MG: put in - 1 as it was off by one base, even though getVisibleBase
    // was returning the proper base. It seems like transformer.toPixel is off
    // by one base but that seems crazy since everything uses it - very strange
    int startCoord = hw.scroller.getVisibleBase();
    int width = hw.scroller.getVisibleBaseCount();

    // not entirely sure why this is necessary, but it is
    // SMJS Removed
    //if (reverseComplement)
    //    startCoord--;

    int baselineX = transformer.toPixel(0,0).x;
    // have to do -1, transformer gives pixel for end of coord
    //int startXPixel = transformer.toPixelX(startCoord-1);
    // minXPixelAtUserCoord gives the left most value of the coord,
    // toPixelX gives the rightmost which leaves out the base itself
    int startXPixel = transformer.minXPixelAtUserCoord(startCoord);
    int widthPixel = transformer.toPixel(width,0).x-baselineX;

    if (widthPixel < 0) {
      if (!hw.reverse)
        startXPixel += widthPixel;
      widthPixel = -widthPixel;
    } else if (hw.reverse) {
      // RC: want to move start to the right when it is reversed
      startXPixel = transformer.maxXPixelAtUserCoord(startCoord);
      startXPixel -= widthPixel;
    }

    if (widthPixel < 1) {
      widthPixel = 1;
    }
    Rectangle rect = new Rectangle(startXPixel,apolloPanel.getBounds().y,widthPixel,
                                   apolloPanel.getSize().height);
    return rect;
  }

  public void repaintHighlights() {
    RepaintManager currentManager =
      RepaintManager.currentManager(this);
    for(int i=0; i < highlightRegions.size(); i++) {
      HighlightWrapper hw = (HighlightWrapper)
                            highlightRegions.elementAt(i);

      Rectangle rect = calculateHighlightRect(hw);
      Rectangle rect2 = hw.getHighlightRect();
      if (rect == null || rect2 == null || !rect2.equals(rect)) {
        if (rect != null) {
          // logger.debug("repainting rect");
          currentManager.addDirtyRegion(this,
                                        rect.x,
                                        rect.y,
                                        rect.width+1,
                                        rect.height);
        }
        if (rect2 != null) {
          // logger.debug("repainting rect2");
          currentManager.addDirtyRegion(this,
                                        rect2.x,
                                        rect2.y,
                                        rect2.width+1,
                                        rect2.height);
        }
      }
    }
    currentManager.paintDirtyRegions();
  }

  public void paint(Graphics g) {
    // If a load is in progress then the tiers could be in an inconsistent
    // state causing exceptions to be thrown - and we dont need to paint during
    // load anyways
    //if (loadInProgress) return;
    //if (!loadInProgress) super.paint(g);
    super.paint(g);
    /* attempt to draw a selection window */

    for(int i=0; i < highlightRegions.size(); i++) {
      HighlightWrapper hw = (HighlightWrapper)
                            highlightRegions.elementAt(i);

      Rectangle rect = calculateHighlightRect(hw);
      hw.setHighlightRect(rect);
      g.setColor(hw.color);
      g.drawRect(rect.x,rect.y,rect.width,rect.height);
    }
  }

  public void addHighlightRegion(BaseScrollable scroller, Color color, boolean reverse) {
    highlightRegions.addElement(new HighlightWrapper(scroller,
                                color, reverse));
  }

  public void removeHighlightRegion(BaseScrollable scroller) {
    highlightRegions.removeElement(new HighlightWrapper(scroller,null, false));
  }
  
  public void updateHighlightRegion(BaseScrollable scroller, Color color, boolean reverse) {
    int index = highlightRegions.indexOf(new HighlightWrapper(scroller,null, false));
    if (index >= 0) {
      HighlightWrapper hw = (HighlightWrapper)highlightRegions.get(index);
      hw.setColor(color);
      hw.setReverse(reverse);
    }
  }

  public boolean isReverseComplement() {
    return reverseComplement;
  }

  public void setReverseComplement(boolean state) {
    reverseComplement = state;

    int frv_val = 0;
    int rrv_val = 0;

    //forwardSv.invertViews();
    forwardSplitterScaleContainer.invertViews();
    forwardContainerView.invertViews();
    reverseContainerView.invertViews();
    strandSplitter.invertViews();
    graphContainerView.invertViews();

    if (useScrollHack) {
      frv_val = forwardResultView.getInvertedScrollbarValue();
      rrv_val = reverseResultView.getInvertedScrollbarValue();
    }

    // set order of views
    removeAllViews();
    addAllViews();

    // set X directions
    int dir = reverseComplement ? Transformer.RIGHT : Transformer.LEFT;
    // do even if no sequence i think
    reverseSiteView.setXOrientation(dir);
    reverseResultView.setXOrientation(dir);
    reverseAnnotView.setXOrientation(dir);
    forwardSiteView.setXOrientation(dir);
    forwardResultView.setXOrientation(dir);
    forwardAnnotView.setXOrientation(dir);
    scaleView.setXOrientation(dir);
    for (GraphView g : graphs) {
      g.setXOrientation(dir);
    }
    // translationView .setXOrientation(dir);

    // set Y directions
    if (reverseComplement) {
      reverseAnnotView.setYOrientation(Transformer.UP);
      reverseResultView.setYOrientation(Transformer.UP);

      forwardAnnotView.setYOrientation(Transformer.DOWN);
      forwardResultView.setYOrientation(Transformer.DOWN);
      forwardSiteView.setYOrientation(Transformer.DOWN);

    } else {
      reverseAnnotView.setYOrientation(Transformer.DOWN);
      reverseResultView.setYOrientation(Transformer.DOWN);
      reverseSiteView.setYOrientation(Transformer.DOWN);

      forwardAnnotView.setYOrientation(Transformer.UP);
      forwardResultView.setYOrientation(Transformer.UP);
    }
    setScrollValues();  // This sets HORIZONTAL scroll values

    if (useScrollHack) {
      forwardResultView.setScrollbarValue(frv_val);
      reverseResultView.setScrollbarValue(rrv_val);
    }

    Color tmp = Config.getCoordForeground();
    (Config.getStyle()).setCoordForeground(Config.getCoordRevcompColor());
    (Config.getStyle()).setCoordRevcompColor(tmp);

    // Change ApolloFrame title
    Window  win = SwingUtilities.windowForComponent(apolloPanel);
    if (win instanceof ApolloFrame) {
      ApolloFrame frame = (ApolloFrame) win;
      if (frame != null) {
        String revcompPhrase = " (REVERSE COMPLEMENTED)";
        if (reverseComplement)
          frame.setTitle(frame.getTitle() + revcompPhrase);
        else {
          String title = frame.getTitle();
          if (title != null && title.lastIndexOf(revcompPhrase) > 0) {
            title = title.substring(0, title.lastIndexOf(revcompPhrase));
            frame.setTitle(title);
          }
        }
      }
    }
    setViewColours();
    repaint();
    notifyRevCompListener();
  }

  public boolean handleScrollEvent(ScrollEvent event){
    double pixelAmount = event.getValue();
    int baseAmount = (int)(getTransformer().getXCoordsPerPixel() * pixelAmount);
    int finalScrollbarPosition = hScroll.getValue() + baseAmount;

    if(isReverseComplement()){
      pixelAmount = -1 * pixelAmount;
    }

    doScrolling(finalScrollbarPosition);

    return true;
  }

  private void doScrolling(int val){
    setCentreBase(_scrollToPosition(val));
    fireBaseFocusEvent(getCentreBase());
    setScrollValues();
  }

  // Trap scrollbar events
  public void adjustmentValueChanged(AdjustmentEvent e) {
    double pixelChange;
    int val;
    int basesScrolled;

    // settingScroller stops the centre being changed while setting
    // the scroller properties - irritating.
    val = hScroll.getValue();
    if (HSCROLLABLE && e.getSource() == hScroll && !settingScroller){
      //
      //The 'val' doesn't take into account a revcomp. _scrollToPosition fixes
      //that, so I can correctly compute the right pixelChange whether the scale
      //is revcomped or not.
      basesScrolled = _scrollToPosition(val) - getCentreBase();
      if(!isReverseComplement()){
        pixelChange = getTransformer().getXPixelsPerCoord() * basesScrolled;
      }else{
        pixelChange = -1 * getTransformer().getXPixelsPerCoord() * basesScrolled;
      }

      doScrolling(val);

      if(isScrollingPropagated()){
        getController().handleScrollEvent(
          new ScrollEvent(this, pixelChange)
        );
      }
    }
  }

  private int _scrollToPosition(int scrollVal) {
    int retVal = scrollVal;

    if (reverseComplement) {
      retVal = (int)apolloPanel.getLinearMaximum() - scrollVal + (int)apolloPanel.getLinearMinimum();
    }
    return retVal;
  }

  private int _positionToScroll(int posVal) {
    int retVal = posVal;

    if (reverseComplement) {
      retVal = (int)apolloPanel.getLinearMaximum() + (int)apolloPanel.getLinearMinimum() - posVal;
    }
    return retVal;
  }

  public boolean handleBaseFocusEvent (BaseFocusEvent evt) {
    if (evt.isPosition()) // center on position
      setCentreBase(evt.getFocus());
    
    else //range
      zoomToWidth(evt.getRangeLimits());

    //return false; // doesnt much matter but i think this should be true
    return true;
  }
  
  public boolean handleReverseComplementEvent(ReverseComplementEvent evt) {
    setReverseComplement(!reverseComplement);
    return false;
  }
  // This is redundant with SyntenyLinkPanel.handlePropSchemeCngEv which repaints
//   public boolean handlePropSchemeChangedEvent (TypesChangedEvent evt) {
//     repaint();
//     return false;
//   }

  private void fireBaseFocusEvent(int position) {
    //      SeqFeatureI sf = new SeqFeature();

    BaseFocusEvent evt = new BaseFocusEvent(this,position,null);

    controller.handleBaseFocusEvent(evt);
  }

  public int getCentreBase() {
    return  apolloPanel.getLinearCentre();
  }

  public void setCentreBase(int location) {
    logger.trace("setCentreBase() called, setting apolloPanel centre to " + location);

    apolloPanel.setLinearCentre(location);

    logger.trace("setting hscroll value to " + location);

    settingScroller = true;
    hScroll.setValue(_positionToScroll((int)location));
    settingScroller = false;

    logger.trace("hscroll value now " + hScroll.getValue());
  }

  public void setScrollValues() {

    if (HSCROLLABLE == true && hScroll != null) {
      settingScroller = true;
      int visWidth = (int)apolloPanel.getVisibleBasepairWidth();

      hScroll.setMinimum((int)apolloPanel.getLinearMinimum());
      hScroll.setMaximum((int)apolloPanel.getLinearMaximum()+visWidth);
      hScroll.setVisibleAmount(visWidth);
      hScroll.setUnitIncrement(Math.max(visWidth/20,1));
      hScroll.setBlockIncrement(Math.max(visWidth/4,1));

      logger.trace("calling hScroll.setValue for centre base at " + getCentreBase());
      hScroll.setValue(_positionToScroll((int)getCentreBase()));

      if (logger.isTraceEnabled()) {
        printScrollValues();
      }
      settingScroller = false;
    }
  }

  // for debugging
  private void printScrollValues() {
    if (hScroll != null) {
      logger.trace("hscroll values:- ");
      logger.trace(" minimum       = " + hScroll.getMinimum());
      logger.trace(" maximum       = " + hScroll.getMaximum());
      logger.trace(" visibleAmount = " + hScroll.getVisibleAmount());
      logger.trace(" value         = " + hScroll.getValue());
      logger.trace(" unitInc       = " + hScroll.getUnitIncrement());
      logger.trace(" blockInc      = " + hScroll.getBlockIncrement());
    }
  }

  public Controller getController() {
    return this.controller;
  }

  public SelectionManager getSelectionManager() {
    return selectionManager;
  }

  public boolean handleRubberbandEvent(RubberbandEvent evt) {
    return false;
  }
  public boolean handleDataLoadEvent(DataLoadEvent evt) {

    if (!evt.dataRetrievalBeginning())
      return false;

    if (isReverseComplement())
      setReverseComplement(false);

    clearData();

    setScrollValues();
    setZoomFactor(1.0);
    return true;
  }

  /**
   * Action event listener
   */
  public void actionPerformed(ActionEvent evt) {
    double newFactor = getZoomFactor();
    String zoomFactor = null;
    ShiftAwareButton button = null;

    if(evt.getSource() instanceof ShiftAwareButton){
      button = (ShiftAwareButton)evt.getSource();
    }

    if (ZOOMABLE && button == zoomin) {
      newFactor *= 2.0;
      zoomFactor = ZOOMIN;
    } else if (ZOOMABLE && button == zoomin2) {
      newFactor *= 10.0;
      zoomFactor = ZOOMIN2;
    } else if (ZOOMABLE && button == zoomout) {
      newFactor /= 2.0;
      zoomFactor = ZOOMOUT;
    } else if (ZOOMABLE && button == zoomout2) {
      newFactor /= 10.0;
      zoomFactor = ZOOMOUT2;
    } else if (ZOOMABLE && button == reset) {
      newFactor = 1.0;
      zoomFactor = RESET;
    } else {
      logger.error("Unknown source for ActionEvent");
      return;
    }

    doZoom(newFactor, zoomFactor);

    controller.handleZoomEvent(new ZoomEvent(this,propagateZoom(button),zoomFactor));
  }

  /** If style calls for shiftForLockedZooming and the shift key is down then
      propagate the zoom (in synteny other species then zooms)
      Also propagate if style doesn't call for shift locking and the shift key
      is not down. Otherwise dont propagate the zoom. */
  private boolean propagateZoom(ShiftAwareButton button) {
    boolean shift = (button != null) && (button.isMousePressShifted());
    return (shift && shiftForLockedZooming()) || (!shift && !shiftForLockedZooming());
  }

  private boolean shiftForLockedZooming() { return shiftForLockedZooming; }
  public void setShiftForLockedZooming(boolean state) {
    shiftForLockedZooming = state;
  }

  private void doZoom(double newFactor, String zoomCommand){
    setZoomFactor(newFactor);

    if (newFactor == 1.0 && RESET.equals(zoomCommand)) {
      // This may be debatable, but when I reset, the scrollbars often end up in some
      // strange position (so we can no longer see the selected thing), so let's
      // scroll so that we can see the thing we selected.
      // First reset all scrollbars because verticalScrollToSelection only fixes the one that
      // has the selected thing--the others are left in some random place.
      putVerticalScrollbarsAtStart();
      apolloPanel.verticalScrollToSelection();
    }
  }//end event.getZoomFactor()

  public void resetViews() {
    // This will set visibility to how specified in style which I feel resetViews
    // should do. We can take this out if others dont agree. MG
    setViewVisibilityToStyleSpecifications();
    setZoomFactor(1.0);
    if (isReverseComplement()) {
      setReverseComplement(false);
    }
    forwardSplitterView.resetSplitFract();
    reverseSplitterView.resetSplitFract();
    strandSplitter.resetSplitFract();
    setForwardVisible(true);
    setReverseVisible(true);
    apolloPanel.setInvalidity(true);
    apolloPanel.doLayout();
    apolloPanel.setInvalidity(false);
    putVerticalScrollbarsAtStart();

    double curWidth = (double)apolloPanel.getVisibleBasepairWidth();

    setCentreBase(curationSet.getLow()+(int)curWidth/2);
  }


  /** boolean force means force in the new factor and dont test it
   * against the limits. This is necasary when initially bringing up a new
   * curation set, the factor is being set to 1 which we know is ok, and the
   * xscale is from the old set while the width is from the new so if we do
   * test it at this point it falsely fails and thus the force
   */
  private void setZoomFactor(double factor,boolean force) {
    double newDesiredWidth = (double)apolloPanel.getVisibleBasepairWidth()*xscale/factor;
    // If we are forcing the zoom OR the zoom is between 2 and a billion do the zoom
    if ( force || ( (newDesiredWidth > 2.0 ) && (newDesiredWidth < 1000000000.0) ) ) {
      xscale = factor;
      apolloPanel.setZoomFactor(xscale,-1.0);
      // false means don't force an update if they already in that state
      setScrollValues();
    }
    updateSiteVisibility(false);
    int newWidth = apolloPanel.getVisibleBasepairWidth();
    zoomin.setEnabled(newWidth > 4);
    zoomin2.setEnabled(newWidth > 10);
    zoomout.setEnabled(newWidth < 500000000L);
    zoomout2.setEnabled(newWidth < 100000000L);
    zoomLabel.setText("  Zoom factor = "+new Format("%6.4f").form(xscale) + "   ");
  }

  public void setZoomFactor(double factor) {
    setZoomFactor(factor,false);
  }

  private double getZoomFactor() {
    return xscale;
  }

  public void setTextAvoidance(boolean state) {
    forwardAnnotView.setTextAvoidance(state);
    reverseAnnotView.setTextAvoidance(state);
    forwardResultView.setTextAvoidance(state);
    reverseResultView.setTextAvoidance(state);
  }

  public void setStatusBar(StatusBar sb) {
    this.statusBar = sb;
    if (apolloPanel != null) {
      apolloPanel.setStatusBar(sb);
    }
  }

  public void setEdgeMatching(boolean state) {
    apolloPanel.setEdgeMatching(state);
  }

  public Transformer getTransformer() {
    return forwardResultView.getTransform();
  }

  public boolean haveSequence() {
    return haveSequence;
  }

  // not used
//   public void setLoadInProgress(boolean inProgress) {
//     //loadInProgress = inProgress;
//     //apolloPanel.setLoadInProgress(inProgress);
//     if (inProgress)
//       apolloPanel.clearFeatures();
//   }

  public void clearData() {
    apolloPanel.clearFeatures();
    // trying to lose refs to old cur set as soon as new one ready to load
    curationSet = null;
    //movementPanel.setCurationSet(null);
    forwardSiteView.setCurationSet(null);
    reverseSiteView.setCurationSet(null);
    scaleView.setCurationSet(null);
    getGraphView().setScoreCalculator(null); // gets a new one in setFeatureSet
    //navManager.action = null; // ??
    //curationState.getNavigationBar().setEnabled(Config.isNavigationManagerEnabled());
    while (graphs.size() > 1) {
      //removeGraph(graphs.get(graphs.size() - 1));
      graphs.get(graphs.size() - 1).setVisible(false, true);
    }
    setGraphContainerViewVisibility(getGraphView().isVisible());
  }

  /**
   * A label that's stamped with a curation set's organism name and range
   * whenever I'm informed of a new curation set.
  **/
  public JLabel getOrganismLabel(){
    return organismLabel;
  }//end getOrganismLabel

  public void repaint(){
    if(getController()!= null){
      getController().handleChainedRepaintEvent(new ChainedRepaintEvent(this));
    }//end if

    super.repaint();
  }

  public boolean handleZoomEvent(ZoomEvent event){
    double newFactor = getZoomFactor();

    if (ZOOMABLE && ZOOMIN.equals(event.getZoomFactor())) {
      newFactor *= 2.0;
    } else if (ZOOMABLE && ZOOMIN2.equals(event.getZoomFactor())) {
      newFactor *= 10.0;
    } else if (ZOOMABLE && ZOOMOUT.equals(event.getZoomFactor())) {
      newFactor /= 2.0;
    } else if (ZOOMABLE && ZOOMOUT2.equals(event.getZoomFactor())) {
      newFactor /= 10.0;
    } else if (ZOOMABLE && RESET.equals(event.getZoomFactor())) {
      newFactor = 1.0;
    } else if (ZOOMABLE && CUSTOM.equals(event.getZoomFactor())) {
      newFactor *= event.getFactorMultiplier();
    }//end if

    doZoom(newFactor, event.getZoomFactor());

    return true;
  }//end handleZoomEvent

  public Component getComponent() { return this; }

  // Maybe all the view methods could be interfaced? ViewVisibilityI?
  public void setAnnotationViewsVisible(boolean state){
    annotationViewsVisible = state;
    apolloPanel.setInvalidity(true);
    forwardAnnotView.setVisible(state);
    reverseAnnotView.setVisible(state);
  }

  public boolean areAnnotationViewsVisible(){
    return annotationViewsVisible;
  }

  public ResultView getViewForFeature(SeqFeatureI sf) {
    return (sf.getStrand() >= 0 ? forwardResultView : reverseResultView);
  }

  public void setResultViewsVisible(boolean state){
    resultViewsVisible = state;
    apolloPanel.setInvalidity(true);
    forwardResultView.setVisible(state);
    reverseResultView.setVisible(state);
  }

  public boolean areResultViewsVisible(){
    return resultViewsVisible;
  }

  public void setScaleViewVisible(boolean state){
    scaleViewVisible = state;
    //apolloPanel.setInvalidity(true);
    scaleView.setVisible(state);
    //apolloPanel.doLayout();
    //apolloPanel.setInvalidity(false);
  }

  public boolean isScaleViewVisible(){
    return scaleViewVisible;
  }

  /** If state is false hide site view, even if zoomed in. If state true
      show sites only if at proper zoom level. */
  public void setSiteViewVisibleOnZoom(boolean state) {
    sitesVisibleOnZoom = state;
    updateSiteVisibility(true);
  }
  /** This does not return whther the sites are actually visible but whether
      they are capable of being visible. Returns false if not visible at any
      zoom level, return true if visible when zoomed in. In either state
      sites are not visible when zoomed out. */
  public boolean getSiteViewVisibleOnZoom() { return sitesVisibleOnZoom; }

  public void setScrollingPropagated(boolean value){
    scrollingPropagated = value;
  }

  public boolean isScrollingPropagated(){
    return scrollingPropagated;
  }

  public boolean isShowingEdgeMatches(){
    return apolloPanel.isShowingEdgeMatches();
  }

  public void saveLayout(JIniFile iniFile, String section) {
    iniFile.writeInteger(section,"width",getSize().width);
    iniFile.writeInteger(section,"height",getSize().height);
    iniFile.writeInteger(section,"xorigin",getLocation().x);
    iniFile.writeInteger(section,"yorigin",getLocation().y);

    iniFile.writeBoolean(section,"controls_visible",isControlPanelVisible());
    iniFile.writeBoolean(section,"navbar_visible",curationState.getNavigationBar().isVisible());

    iniFile.writeInteger(section,"centre_base",getCentreBase());
    iniFile.writeDouble(section,"zoom_factor",getZoomFactor());
    iniFile.writeInteger(section,"curation_low",curationSet.getLow());
    iniFile.writeInteger(section,"curation_high",curationSet.getHigh());

    iniFile.writeBoolean(section,"reverse_complement",isReverseComplement());

    iniFile.writeDouble(section,"strand_split_fract",strandSplitter.getSplitFract());
    iniFile.writeDouble(section,"forward_split_fract",forwardSplitterView.getSplitFract());
    iniFile.writeDouble(section,"reverse_split_fract",reverseSplitterView.getSplitFract());

    iniFile.writeBoolean(section,"forward_views_visible",isForwardStrandVisible());
    iniFile.writeBoolean(section,"reverse_views_visible",isReverseStrandVisible());
    iniFile.writeBoolean(section,"annotation_views_visible",areAnnotationViewsVisible());
    iniFile.writeBoolean(section,"result_views_visible",areResultViewsVisible());
    iniFile.writeBoolean(section,"scale_visible",isScaleViewVisible());
    iniFile.writeBoolean(section,"sites_visible",getSiteViewVisibleOnZoom());
    iniFile.writeBoolean(section,"graph_visible",getGraphVisibility());

    iniFile.writeBoolean(section,"show_edge_matches",isShowingEdgeMatches());
  }

  public void applyVisibilityFromLayout(JIniFile iniFile, String section) {
    boolean controlVis = iniFile.readBoolean(section,"controls_visible",isControlPanelVisible());
    boolean navVis     = iniFile.readBoolean(section,"navbar_visible",curationState.getNavigationBar().isVisible());
    boolean sitesVis   = iniFile.readBoolean(section,"sites_visible",getSiteViewVisibleOnZoom());

    if (curationState.getNavigationBar().isVisible() != navVis) {
      // Fake up an event to modify the nav bar visibility through its action
      curationState.getNavigationBar().getNavigationAction().actionPerformed(new ActionEvent(this,0,"windowarranger"));
    }

    setControlPanelVisibility(controlVis);
    setSiteViewVisibleOnZoom(sitesVis);

    boolean forwardVis = iniFile.readBoolean(section,"forward_views_visible",isForwardStrandVisible());
    boolean reverseVis = iniFile.readBoolean(section,"reverse_views_visible",isReverseStrandVisible());
    boolean annotVis   = iniFile.readBoolean(section,"annotation_views_visible",areAnnotationViewsVisible());
    boolean resultVis  = iniFile.readBoolean(section,"result_views_visible",areResultViewsVisible());
    boolean scaleVis   = iniFile.readBoolean(section,"scale_visible",isScaleViewVisible());
    boolean graphVis   = iniFile.readBoolean(section,"graph_visible",getGraphVisibility());

    if (forwardVis != isForwardStrandVisible())  setForwardVisible(forwardVis);
    if (reverseVis != isReverseStrandVisible())  setReverseVisible(reverseVis);
    if (annotVis != areAnnotationViewsVisible()) setAnnotationViewsVisible(annotVis);
    if (resultVis != areResultViewsVisible())    setResultViewsVisible(resultVis);
    if (scaleVis != isScaleViewVisible())        setScaleViewVisible(scaleVis);
    if (graphVis != getGraphVisibility())        setGraphVisibility(scaleVis);
  }

  public void applyLayout(JIniFile iniFile, String section, boolean updateBaseLocation) {
    applyVisibilityFromLayout(iniFile, section);

    if (updateBaseLocation) {
      int basePos       = iniFile.readInteger(section,"centre_base",getCentreBase());
      double zoomFactor = iniFile.readDouble(section,"zoom_factor",getZoomFactor());
      int curationLow   = iniFile.readInteger(section,"curation_low",0);
      int curationHigh  = iniFile.readInteger(section,"curation_high",0);

      if (curationLow != curationSet.getLow() || curationHigh != curationSet.getHigh()) {
        logger.warn("Curation set bounds in ini file different to current set. Zoom factor setting from ini file may not do what you expect");
      }

      setCentreBase(basePos);
      setZoomFactor(zoomFactor);
    }

    boolean reverseComplemented   = iniFile.readBoolean(section,"reverse_complement",isReverseComplement());
    if (reverseComplemented != isReverseComplement()) {
      setReverseComplement(reverseComplemented);
    }

    double splitFract;
    splitFract = iniFile.readDouble(section,"strand_split_fract",strandSplitter.getSplitFract());
    strandSplitter.setSplitFract(splitFract);

    splitFract = iniFile.readDouble(section,"forward_split_fract",forwardSplitterView.getSplitFract());
    forwardSplitterView.setSplitFract(splitFract);

    splitFract = iniFile.readDouble(section,"reverse_split_fract",reverseSplitterView.getSplitFract());
    reverseSplitterView.setSplitFract(splitFract);

    boolean showEdges = iniFile.readBoolean(section,"show_edge_matches",isShowingEdgeMatches());
    setEdgeMatching(showEdges);
  }
  
  /**
   * My zoom-buttons are aware of when they are pressed with a shift-key.
   * I need this because sometimes (when viewing multiple species data) the
   * viewer wants to propagate a zoom from one panel to another: the signal
   * is to hold the shift key down.
  **/
  public class ShiftAwareButton extends JButton{
    public boolean mousePressShifted;
    public ShiftAwareButton(String label){
      super(label);
      // Make zoom buttons a bit smaller to save on real estate
      // Args are top, left, bottom, right margins
      setMargin(new Insets(2, 6, 2, 6));
    }

    public void processMouseEvent(MouseEvent event){
      try{
        if(((event.getModifiers() & event.SHIFT_MASK) !=0)){
          mousePressShifted = true;
        }
        super.processMouseEvent(event);
      }finally{
        mousePressShifted = false;
      }//end try
    }//end processMouseEvent

    public boolean isMousePressShifted(){
      return mousePressShifted;
    }
  }//end ShiftAwareButton
  private class HighlightWrapper {
    Color color;
    BaseScrollable scroller;
    boolean reverse;
    Rectangle highlightRect = null;

    public HighlightWrapper(BaseScrollable scroller,
                            Color color, boolean reverse) {
      this.reverse = reverse;
      this.color = color;
      this.scroller = scroller;
    }

    public boolean equals(Object o) {
      if (o instanceof HighlightWrapper) {
        HighlightWrapper hw = (HighlightWrapper) o;
        return hw.scroller.equals(scroller);
      }
      return false;
    }

    public void setHighlightRect(Rectangle highlightRect) {
      this.highlightRect = highlightRect;
    }

    public Rectangle getHighlightRect() {
      return highlightRect;
    }
    
    public boolean getReverse() {
      return reverse;
    }
    
    public void setReverse(boolean r){
      this.reverse = r;
    }
    
    public Color getColor() {
      return color;
    }
    
    public void setColor(Color c) {
      color = c;
    }
  }
  /** Handles dragging of highlights. The drag only happens in scale view.
  eg the vertical bar from bofe  */
  private class HighlightDragListener extends MouseInputAdapter {

    HighlightWrapper dragged;
    boolean dragging = false;
    RepaintManager repainter;
    JComponent parent;
    Transformer t = scaleView.getTransform();

    public HighlightDragListener(JComponent parent) {
      repainter = RepaintManager.currentManager(parent);
    }

    public void mousePressed(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e))
        return;
      ViewI view  = apolloPanel.getViewAt(new Point(e.getX(),e.getY()));
      if (view instanceof ScaleView) {
        for(int i=0; i < highlightRegions.size(); i++) {
          HighlightWrapper hw = (HighlightWrapper) highlightRegions.elementAt(i);

          int x = e.getX();
          Rectangle rect = calculateHighlightRect(hw);

          if (x >= rect.x - 1 &&
              x <= rect.x + rect.width + 1) {
            dragged = hw;
            dragging = true;
          }
        }
      }
    }

    private void updateLoc(int newLoc) {
      int clickCoord = t.toUser(newLoc, 0).x;
      Rectangle rect = calculateHighlightRect(dragged);

      repainter.addDirtyRegion(parent, rect.x, 0,
                               rect.width, apolloPanel.getSize().height);
      repainter.addDirtyRegion(parent, newLoc, 0,
                               rect.width, apolloPanel.getSize().height);

      dragged.scroller.scrollToBase(clickCoord);
      repaint();
      //scroll window here
    }

    public void mouseDragged(MouseEvent e) {
      if (dragging) {
        updateLoc(e.getX());
      }
    }

    public void mouseReleased(MouseEvent e) {
      if (dragging) {
        updateLoc(e.getX());
        dragging = false;
      }
    }
  } // end of HighlightDragListener
  
  private class GraphVisibilityListener implements VisibilityListener
  {
    public void visibilityChanged(VisibilityEvent e)
    {
      java.util.List<DiscreteGraphView> goneGraphs = new LinkedList<DiscreteGraphView>();
      boolean visible = false;
      for (Object o : graphContainerView.getViews()) {
        LinearView lv = (LinearView)o;
        if (lv.isVisible()) {
          visible = true;
        }
        else {
          if (lv instanceof DiscreteGraphView && e.isRemoveElement()) {
            goneGraphs.add((DiscreteGraphView)lv);
          }
        }
      }
      setGraphContainerViewVisibility(visible);
      for (DiscreteGraphView graph : goneGraphs) {
        removeGraph(graph);
      }
    }
  }
  
  private void setGraphContainerViewVisibility(boolean visible)
  {
    if (visible && !graphContainerView.isVisible()) {
      graphContainerView.setVisible(visible);
      forwardSv.setSplitFract(0.8);
    }
    graphContainerView.setVisible(visible);
  }
  
  public void removeAllGraphs()
  {
    graphs.clear();
    graphContainerView.clear();
  }
  
  public void removeGraph(GraphView graph)
  {
    graphs.remove(graph);
    graphContainerView.remove(graph);
  }
  
}
