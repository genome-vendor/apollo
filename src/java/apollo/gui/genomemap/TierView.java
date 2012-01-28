package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.datamodel.*;
import apollo.gui.Controller;
import apollo.gui.ControlledObjectI;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import apollo.gui.SelectionManager;
import apollo.gui.Transformer;
import apollo.gui.TierManager;
import apollo.gui.TierManagerI;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableUtil;
import apollo.gui.event.*;
import apollo.util.*;
import java.util.*;

/**
 * This class is the base class for FeatureView and SiteView. It is
 * abstract because it doesn't implement getTierData() and would be
 * non functional even if it did because it doesn't create the manager
 * object anywhere. The reason it has been separated from FeatureView is
 * to enable the use of the Scrolling and Tier functionality in the
 * SequenceView, which although different to the FeatureView is basically
 * a TierView where each tier is a piece of the sequence, so we can use
 * the Tier and Scroll functionality and just change the meaning of tiers
 * by creating a new TierManager and changing all the draw methods.
 *
 * Presently scrolling is done by mapping the scroll value to a tier number.
 * Tiers run in opposite directions on forward and reverse strands.  The tier
 * number has to be inverted on the forward strand to be consistent with how
 * the reverse strand is scrolled. Also this means that ScrollableTierView
 * has to know about strand, which is a little funny because its subclass SequenceView
 * is unstranded. This makes the code a bit confusing. I think
 * a better way would be to not scroll by tier, but just by y values. A view would set
 * itself according to the y values put out by the scroll bar. Im guessing that
 * one of the reasons to do scrolling by tier was to have a unit of scrolling be a tier,
 * but this can still be achieved with scrolling by y value, it would just round it to
 * the nearest tier. This would be a bit of a work I think so Im putting it off for now.
 * I'm also curious what others think. - MG
 */
public abstract class TierView extends ManagedView implements 
  AdjustmentListener,
  ControlledObjectI,
  DropTargetViewI,
  PickViewI,
  TierManagerListener,
  TierViewI {

  protected JScrollBar   vScroll;
  protected boolean      vscrollable = true;
  protected boolean      rightSide   = true;

  protected Controller   controller;
  protected Vector       dragSources = new Vector();

  /** Height of row in coordinate space (TierManager.Y_PIXELS_PER_FEATURE) */
  private int rowCoordHeight=0;

  protected SelectionManager selectionManager;

  public TierView(JComponent ap,
                  String name,
                  SelectionManager selectionManager) {
    super(ap, name, false);
    this.selectionManager = selectionManager;
    addScrollBar ();
  } 
		     
  public void setSelectionManager(SelectionManager selectionManager) {
    this.selectionManager = selectionManager;
  }

  public void setTierManager(FeatureTierManager ftm) {
    // Initialise the data in the TierManager
    if (this.manager != null) {
      Controller c;
      if ((c = this.manager.getController()) != null) {
        c.removeListener((EventListener)this.manager);
      }
    }
    ftm.addTierManagerListener(this);
    super.setTierManager((TierManagerI)ftm);
  }

  protected void updateManagerHeight() {
    if (manager != null) {
      super.updateManagerHeight();
      /* Set the vscroller
         This is the one of the ways that the tierview differs
         from the dragview. Sites, results, and annotation views
         all support scrolling. Not repainting until this is set */
      setScrollValues();
    }
  }

  public void moveScrollbarByWheelAmount(int nClick) {
    setScrollbarValue(getScrollbarValue() + nClick*rowCoordHeight);
  }

  /** YOrientation gets flipped with revcomping */
  public void setYOrientation(int direction) {
    /* set this before altering the direction */
    boolean flipped = (getTransform().getYOrientation() != direction &&
                       (direction == Transformer.UP || 
                        direction == Transformer.DOWN));
    super.setYOrientation(direction);
    // scrollbar needs syncing on flip,
    // have to check for null manager, initial orient setting no manager yet
    if (vscrollable && flipped && manager != null) {
      flipScrollBar();
    }
  }

  protected void addScrollBar() {
    vScroll = new JScrollBar();
    vScroll.setOrientation       (JScrollBar.VERTICAL);
    vScroll.addAdjustmentListener(this);
    vScroll.setVisible           (true);

    getComponent().add(vScroll,ApolloLayoutManager.NONE);
  }

  // Overidden LinearView methods
  public void setVisible(boolean state) {
    super.setVisible(state);
    setScrollVisibility(state);
  }

  /** Just repaints scrollbars */
  public void paintView() {
    super.paintView();
    if (vscrollable) {
      Rectangle vb = new Rectangle(getBounds());
      if (rightSide)
        vb.x         = vb.x + vb.width - vScroll.getPreferredSize().width;
      else
        vb.x         = 0;

      vb.width     = vScroll.getPreferredSize().width;

      vScroll.setBounds(vb);
      vScroll.invalidate();
      vScroll.validate();
    }
  }

  /** View is flipping (revcomping/changing y orientation),
      preserve scrollbar and view setting by inverting scrollbar value 
      This isn't quite right - needs tweaking - off by page
  */
  private void flipScrollBar() {
    int newVal
      = vScroll.getMaximum() - vScroll.getValue() - vScroll.getVisibleAmount();
    vScroll.setValue(newVal);
  }

  public void setScrollSide(int side) {
    if (side == ViewI.LEFTSIDE) {
      rightSide = false;
    } else {
      rightSide = true;
    }
  }

  public void incrementTierHeight() {
    changeTierHeight(1);
  }

  public void decrementTierHeight() {
    changeTierHeight(-1);
  }

  protected void changeTierHeight(int change) {
    if (change == 1) {
      manager.incrementTierHeight();
    } else {
      manager.decrementTierHeight();
    }
    updateManagerHeight();
    if (isVisible())
      getComponent().repaint();
  }

  public void setLowestVisibleTier(long tier) {
    // Set the lowest visible tier in the manager
    manager.setLowestVisible((int)tier);

    // Get the position in transformer coords for the base
    // Set the lowest visible in transformer
    transformer.setYVisibleMinimum(manager.getMinimumVisibleTransformCoord());

    transformer.setYRange(getYRange());

    setInvalidity(true);
    if (isVisible())
      getComponent().repaint();
  }

  public int getLowestVisibleTier() {
    return manager.getLowestVisible();
  }

  public boolean allowsTierDrags() {
    return true;
  }

  public boolean beginTierDrag(MouseEvent evt) {
    System.out.println("Need to override beginTierDrag!!!");
    return true;
  }

  public void updateTierDrag(MouseEvent evt) {
    System.out.println("Need to override updateTierDrag!!!");
  }

  public void endTierDrag(MouseEvent evt) {
    System.out.println("Need to override endTierDrag!!!");
  }

  // TierManagerListener method
  public boolean handleTierManagerEvent(TierManagerEvent evt) {
    setScrollValues();
    setInvalidity(true);
    if (isVisible())
      getComponent().repaint();
    return true;
  }

  // DropTargetViewI methods (needed for tier rearrangements)
  public boolean interpretDrop(DragViewI dragView, MouseEvent evt) {
    return interpretDrop(dragView,evt,true,new StringBuffer());
  }

  public boolean interpretDrop(DragViewI dragView,
                               MouseEvent evt,
                               boolean doFlag,
                               StringBuffer action) {
    //Check if source is valid
    if (!isValidDragSource(dragView.getOriginView())) {
      action.append("No action");
      return false;
    }
    return true;
    // Check if this is a tier drag
    // Update tier order
  }

  public void registerDragSource(TierViewI view) {
    if (!dragSources.contains(view)) {
      dragSources.addElement(view);
    }
  }

  public boolean isValidDragSource(TierViewI view) {
    if (dragSources.contains(view)) {
      return true;
    }
    return false;
  }

  // AdjustmentListener methods - called when scrollbar adjusted
  public void adjustmentValueChanged(AdjustmentEvent evt) {
    if (vscrollable && evt.getSource() == vScroll) {
      syncScrollbars();
    }
  }

  private void syncScrollbars() {
    if (vScroll == null) {
      try {
        throw new Exception("Failed syncScrollbar");
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
    if (scrollHack) {
      setLowestVisibleTier((long) vScroll.getMaximum() -
                           vScroll.getValue()-manager.getNumVisible());
    } else
      setLowestVisibleTier(scrollValueToTierNumber(vScroll.getValue()));
  }

  /**
   * This method makes scrolling on forward and reverse strands scroll the same way.
   * On the forward strand the tier number from the scroll value has to be flipped
   * around (subtracted from total number of tiers, and the extent has to be subtracted
   * as well)
   */
  private int scrollValueToTierNumber(int scrollValue) {

    // Down Orientation
    if (isDownOrientation())
      return (int)manager.toTier(scrollValue);

    // isUpOrientation()
    int extent = (int)manager.getVisibleUserCoord();
    // dont know why but sometimes this is < 0
    if (extent < 0)
      extent = 0;
    int scrollPlusExtent = scrollValue + extent;
    int tierToFlip = (int)manager.toTier(scrollPlusExtent);
    int forwardStrandTier = manager.getNumTiers() - tierToFlip;
    if (forwardStrandTier < 0)
      forwardStrandTier = 0;
    return forwardStrandTier;
  }
			
  /** Down Orientation happens with reverse strand normal
      and forward strand in reverse comp (below axis)
  */
  protected boolean isDownOrientation() {
    return getTransform().getYOrientation() == Transformer.DOWN;
  }
  /** Up orientation happens with forward strand normal
      and reverse strand in rev comp, its above the axis */
  protected boolean isUpOrientation() {
    return getTransform().getYOrientation() == Transformer.UP;
  }

  /**
   * This is to set a view in its vertical "start" position, which is opposite
   * for the 2 strands. This would be true even if we were not tier based.
   */
  protected void putScrollAtStart() {
    // Set forward strand to show 1st tier which is the maximum scroll value
    if (isUpOrientation())
      setScrollbarValue(getMaxScrollbarValue());
    // ReverseStrand's 1st tier is the minimum scroll value
    if (isDownOrientation())
      setScrollbarValue(getMinScrollbarValue());
  }

  protected void setScrollbarValue(int val) {
    vScroll.setValue(val);
  }

  protected int getMaxScrollbarValue() {
    return vScroll.getMaximum();
  }

  protected int getVisibleScrollbarValue() {
    return vScroll.getVisibleAmount();
  }

  protected int getMinScrollbarValue() {
    return vScroll.getMinimum();
  }

  protected int getScrollbarValue() {
    return vScroll.getValue();
  }

  protected int getInvertedScrollbarValue() {
    return getMaxScrollbarValue() -
      (getScrollbarValue() + vScroll.getVisibleAmount());
  }

  private boolean scrollHack = false;

  public void setScrollHack(boolean value) {
    scrollHack = value;
  }

  // Class specific methods
  public void setVScrollable(boolean state) {
    vscrollable = state;
  }

  public void fireViewEvent(int type) {
    ViewEvent evt = new ViewEvent(getComponent(),this,type);
    super.fireViewEvent(evt);
  }
			

  public void setScrollValues() {
    if (vscrollable == true && vScroll != null) {
      int oldval = vScroll.getValue();
      int oldvisible = vScroll.getVisibleAmount();
      
      vScroll.setMinimum(0);
      
      int maxUserCoord 
        = (int)manager.getMaxUserCoord() + (int)manager.getMaxTierUserHeight();
      vScroll.setMaximum(maxUserCoord);
      
      int visCoord     = (int)manager.getVisibleUserCoord();
      if (visCoord > maxUserCoord)
        visCoord = maxUserCoord;

      if (rowCoordHeight==0) {
        rowCoordHeight = (int)manager.getMaxTierUserHeight();
        vScroll.setUnitIncrement(rowCoordHeight);
      }
      /*set paging to size of visible screen minus one row 
        so one row retained on page */
      vScroll.setBlockIncrement(visCoord-rowCoordHeight);

      if (scrollHack && oldvisible != vScroll.getVisibleAmount()) {
        int bumpme = vScroll.getVisibleAmount() - oldvisible;
        vScroll.setValue(vScroll.getValue() - bumpme);
      }

      // I found that subtracting visCoord  messes revcomp up but is needed
      // or otherwise the scrollbars still scroll when theres nothing to scroll
      if (oldval > maxUserCoord - visCoord) {
        // this happens if tiers dont fill screen
        if (maxUserCoord-visCoord <= vScroll.getMinimum())
          vScroll.setValue(vScroll.getMinimum());
        else
          vScroll.setValue(maxUserCoord - visCoord);
      }

      // setVisibleAmount has to come after setValue as it can get rejected
      // depending on the value -
      vScroll.setVisibleAmount(visCoord);

      //Scrollbars are now permanent again whether there is anything to scroll or not.
      //When they were not showing up and their space was reclaimed the logic was
      //erroneous and views with and without scrollbars would have different
      //basepairs/pixel and features would not line up. Until this is addressed
      //scrollbars should just be kept in.
    }
  }

  public void printScrollValues() {
    System.out.println("vScroll params:-");
    System.out.println("  minimum " + vScroll.getMinimum());
    System.out.println("  maximum " + vScroll.getMaximum());
    System.out.println("  value   " + vScroll.getValue());
    System.out.println("  visam   " + vScroll.getVisibleAmount() + 
                       " nvistier " + manager.getNumVisible());
  }

  public void setScrollVisibility(boolean state) {
    if (vscrollable && vScroll!=null) {
      vScroll.setVisible(state);
    }
  }
			
  public Controller getController() {
    return controller;
  }

  public Object getControllerWindow() {
    return SwingMissingUtil.getWindowAncestor(getComponent());
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  public void setController(Controller c) {
    controller = c;
    controller.addListener(this);
    if (getTierManager() != null) {
      getTierManager().setController(getController());
    }
  }

  /**
    returns a FeatureList of SeqFeatureI instances that
    fall beneath the point passed in
    */
  public FeatureList findFeatures(Point pnt) {
    return findFeatures(getSelectionRectangle(pnt));
  }

  /**
    returns a FeatureList of SeqFeatureI instances that
    overlap any of the areas in the Vector of rectangles
    that is passed in
    */
  public FeatureList findFeatures(Vector rects) {
    FeatureList    features = new FeatureList();
    for (int i = 0; i < rects.size(); i++) {
      Rectangle rect = (Rectangle) rects.elementAt (i);
      features.addVector ((findFeatures(rect)).toVector());
    }
    return features;
  }

  /**
     returns a FeatureList of SeqFeatureI instances (model not drawables)
     overlap any of the areas in the Vector of rectangles that is passed in
  */
  public FeatureList findFeatures(Rectangle rect) {
    FeatureList    features = new FeatureList();
    Vector these_features = findDrawables(rect);
    while (these_features.size() > 0) {
      Drawable se = (Drawable) these_features.elementAt(0);
      these_features.removeElement (se);
      features.addFeature (se.getFeature());
    }
    return features;
  }


  /** Find geared to selection. The SelectionItems in the Selection 
      are model(SeqFeatureI) but they have the associated drawable 
      attached as a listener, so when the SelectionItem is selected it 
      will tell its drawable listener to select. This achieves 2 goals:
      1) Keeps the Selection model based so someones own set of drawables 
      is not being passed around
      2) With the drawables attached we dont have to refind them when 
      receiving the feature
      selection event that came from us, which is rather inefficient and slow
      part of PickViewI interface. Can not do actual selection here.
      If part of previous selection it will get deselected after it gets
      selected.
  */
  public Selection findFeaturesForSelection(Point p, boolean selectParents) {
    return findFeaturesForSelection(getSelectionRectangle(p),selectParents);
  }

  public Selection findFeaturesForSelection(Rectangle rect) {
    // false - dont select parents
    return findFeaturesForSelection(rect, false);
  }

  protected abstract Selection findFeaturesForSelection(Rectangle rect, 
                                                        boolean selectParents);

  /**
   * CANT do selection here as this is called for mouse over as well!
   */
  public Vector findDrawables(Point pnt) {
    return findDrawables(getSelectionRectangle(pnt));
  }

  /** Finds all the sites in rect and creates SiteCodon for them.
   * Returns vector of SiteCodons
   * The SiteCodons are not drawn.
   * Sites are notified when a SiteCodon is selected.
   */
  public Vector findDrawables(Rectangle rect) {
    return findDrawables (rect, false);
  }

  protected abstract Vector findDrawables(Rectangle rect,
                                          boolean select_filter);

  public void clear() {
    getComponent().remove(vScroll);
    vScroll.removeAdjustmentListener(this);
    vScroll = null;
    super.clear();
  }
  	
  private int drag_low;
  private int drag_high;

  /** Returns a FeatureSet of Drawables/view, not a model FeatureSet.
      This is what drag view expects which is what this is used for.
      This could perhaps go in Selection? */
  protected Vector drawablesForDrag(Selection selection) {
    // Make a set of drawables for the selected features
    Vector bundle = new Vector();
    for (int i=0; i < selection.size(); i++) {
      SeqFeatureI selFeat = selection.getSelectedData(i);
      if (i == 0 || selFeat.getLow() < drag_low)
        drag_low = selFeat.getLow();
      if (i == 0 || selFeat.getHigh() > drag_high)
        drag_high = selFeat.getHigh();
      Drawable dsf = DrawableUtil.createDrawable(selFeat);
      dsf.setFeature(selFeat);
      dsf.setVisible(true);
      dsf.setDrawn(true);
      bundle.add(dsf);
    }
    return bundle;
  }

  /** given the entirety of what is currently selected, remove anything
      that doesn't belong to this view and return the remaining selections.
      This used to be handled in the Selection class, but it didn't quite
      work, because the 'source'=='where it was originally selected'
      which, may or may not be, the same as this view.
  */
  public abstract Selection getViewSelection(Selection selection);

  public DragViewI createDragView(MouseEvent evt,
                                  Selection view_selection) {
    // Check if anything is selected to drag
    if (view_selection.size() != 0) {
      // A drawableFeatureSet whose FeatureSet contains Drawables not model
      // What would happen if selToFS returned a FeatureList instead?
      // I dont think so it uses a lot of FeatureSet functionality
      Vector drawable_vect = drawablesForDrag(view_selection);
      Rectangle boxBounds = DrawableUtil.getBoxBounds(drawable_vect, 
						      transformer,
                                                      manager);

      if (evt.getX() >= boxBounds.x &&
	  evt.getX() < boxBounds.x + boxBounds.width + 1) {
	Rectangle bounds = new Rectangle(evt.getX(),
					 evt.getY(),
					 boxBounds.width+1,
					 20000);

	DragView dv =  new DragView(getComponent(), "draggy", view_selection);
	dv.setBounds(bounds);
	dv.setOrigin(this, evt.getPoint());
	dv.setDrawables(drawable_vect);

	DrawableTierManager dm = new DrawableTierManager();
        dm.setTransformer(transformer);
	dm.setAggregateSizeChange(getTierManager().getAggregateSizeChange());
        /* This is very, very sneaky. When the view has its manager
           set then the View gives the drawables to the manager which then
           sets up its tiers accordingly */
	dv.setTierManager(dm);

	int drag_right = (transformer.getXOrientation() == Transformer.LEFT ?
			  drag_high : drag_low);
        Point pixel_right = getTransform().toPixel(drag_right + 1, 0);

        /* Limits are in application coordinates. That is, nucleic acid
           base count. But the upper limit needs to be rounded to 
           pixels? */
        int upper_limit = (drag_high +
                           ((boxBounds.x+boxBounds.width+1-pixel_right.x) *
                            (int)getTransform().getXCoordsPerPixel()));
        dv.setLimits((new int [] {drag_low, upper_limit}));

        dv.setYOrientation(getTransform().getYOrientation());
        dv.setXOrientation(getTransform().getXOrientation());
        dv.getTransform().setXVisibleMinimum(drag_low);
        dv.setRelativePosition(new Point(evt.getX()-boxBounds.x,0));
        return dv;
      } else {
	return null;
      }
    } else {
      return null;
    }
  }
}

