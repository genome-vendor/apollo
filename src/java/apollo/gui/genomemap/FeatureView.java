package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.datamodel.*;
import apollo.config.Config;
import apollo.gui.ControlledObjectI;
import apollo.gui.Selection;
import apollo.gui.SelectionItem;
import apollo.gui.SelectionManager;
import apollo.gui.Transformer;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSetI;
import apollo.gui.drawable.DrawableFeatureSet;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.*;
import apollo.gui.menus.*;
import apollo.util.FeatureList;

/**
 * FeatureView is a View for displaying DrawableSeqFeatures and
 * DrawableFeatureSets (and maybe other Drawables),
 * in linear tiers. It uses a FeatureTierManager to control the
 * layout, and a Transformer for pixel to Base coord transforms.
 * It can have a vertical scrollbar for scrolling tiers.
 * It can do drag and drop.
 */
public abstract class FeatureView extends TierView implements 
  PopupViewI,
  SelectViewI {

  protected Dimension          pickSize;
  protected DrawableSetI dfset = null;
  private   int []            edges = null;
  private   int                lastTierNum;
  private   int                startTierNum;
  private   boolean            inTierDrag = false;
  private   Color              tierColour;
  protected Vector drawables = new Vector();
  // needed for verticalScrollToSelection
  private Vector selectedDrawables;

  public FeatureView(JComponent ap,
		     String name,
		     SelectionManager selectionManager) {
    super(ap, name, selectionManager);
    setTransparent(false);
    // Why is this 2?  Changing it doesn't seem to make any difference.
    setLeadSpaceSize(2);
    addScrollBar();
  }

  public void paintView() {
    /* paints drawables (from ManagedView) and the scroll bars
       (from TierView) */
    super.paintView();

    /* This needs to happen AFTER super.paintView or else we don't see the
       tier drag rectangle. */
    if (inTierDrag) {
      drawDragRectangle(graphics);
    }
  }

  protected void drawDragRectangle(Graphics g) {
    int low  = manager.toUser(lastTierNum);
    int high = manager.toUser(lastTierNum-1*transformer.getYOrientation());

    Point lowpos = transformer.toPixel(transformer.getXVisibleRange()[0],low);
    Point highpos = transformer.toPixel(transformer.getXVisibleRange()[1],high);
    Rectangle decBounds;
    if (transformer.getYOrientation() == Transformer.DOWN) {
      decBounds = new Rectangle(lowpos.x,lowpos.y,
				highpos.x-lowpos.x,highpos.y-lowpos.y);
    } else {
      decBounds = new Rectangle(lowpos.x,highpos.y,
				highpos.x-lowpos.x,lowpos.y-highpos.y);
    }
    if (decBounds.height < 0)
      decBounds.height = -decBounds.height;
    if (decBounds.width < 0)
      decBounds.width = -decBounds.width;
    g.setColor(tierColour);
    g.fillRect(decBounds.x,decBounds.y,decBounds.width,decBounds.height);
  }

  public boolean beginTierDrag(MouseEvent evt) {
    Point dragStartPoint = new Point(evt.getPoint());
    Point userCoord      = getTransform().toUser(dragStartPoint);

    int tierNum  = (int)manager.toTier(userCoord.y);
    lastTierNum  = tierNum;
    startTierNum = tierNum;

    tierColour = ((FeatureTier)manager.getTiers().elementAt(tierNum)).getColour();
    inTierDrag = true;

    //System.out.println("Begin tier drag for tier " + tierNum);

    return true;
  }

  public void updateTierDrag(MouseEvent evt) {
    Point userCoord = getTransform().toUser(evt.getPoint());
    int tierNum     = (int)manager.toTier(userCoord.y);

    //System.out.println("Update tier drag at tier " + tierNum);

    if (tierNum != lastTierNum) {
      int lowestVis = getLowestVisibleTier();
      int change    = tierNum-lastTierNum;
      //System.out.println("Doing new tier num checks. Change =  " + change + " lowest visible " + lowestVis);
      if (tierNum < lowestVis) {
        if (vscrollable && vScroll != null && lowestVis > 0) {
          vScroll.setValue((int)manager.toUser(lowestVis+change));
        }
        tierNum += change;
      } else if (tierNum > lowestVis + manager.getNumVisible()-1) {
        if (vscrollable && vScroll != null &&
            lowestVis + manager.getNumVisible() < manager.getNumTiers()) {
          vScroll.setValue((int)manager.toUser(lowestVis+change));
        }
        tierNum += change;
      }
    }
    lastTierNum = tierNum;
  }

  public void endTierDrag(MouseEvent evt) {
    String fromType
      = ((FeatureTierManagerI) manager).getTierLabel(startTierNum);
    String toType
      = ((FeatureTierManagerI) manager).getTierLabel(lastTierNum);

    if (! fromType.equals(toType)) {
      Config.getPropertyScheme().moveTier(fromType,toType);
    }
    inTierDrag = false;
  }

  /** From PickViewI - */
  protected Selection findFeaturesForSelection(Rectangle r,
                                               boolean selectParents) {
    Selection selection = new Selection();
    // if the view is not visible then it cant be selected via 
    // rectangles that come from the mouse dragging or clicking
    if (!isVisible()) 
      return selection;
    Vector selected_drawables = findDrawables(r);
    //hafta clone as gets emptied above
    selectedDrawables = (Vector) selected_drawables.clone();
    for (int i=0; i < selected_drawables.size(); i++) {
      Drawable dsf = (Drawable)selected_drawables.elementAt(i);
      // the ref feature of a DrawableSeqFeature is 
      // actually a DrawableSeqFeature (not actual model)
      // if we are at the top already dont/ select parents - 1 level annots
      // should not have their parents selected
      // this only covers annots - need a similar notion for results if 
      // results start varying levels (like 1 lev) for now ok isResTop? isTop?
      if (dsf.getFeature().isAnnotTop())
        selectParents = false;
      
      if (selectParents) {
        // this seems funny - shouldnt it be dsf.getFeat.getRefFeat?
        if (dsf.getRefDrawable() == null) {
          System.out.println (dsf.getName() + " has no ref drawable " +
                              " draw class is " + dsf.getClass().getName() +
                              " type is " + dsf.getFeature().getFeatureType());
        }
        else
          dsf = dsf.getRefDrawable();
        if (dsf.getFeature() == null) {
          System.out.println (dsf.getName() +
                              " ref drawable has no feature " +
                              " draw class is " + dsf.getClass().getName() +
                              " type is " + dsf.getFeature().getFeatureType());
        }
      }
      SelectionItem si = new SelectionItem(this,dsf.getFeature());
      // to get deselects and selects from SelectionItem
      si.addSelectionListener(dsf);
      // selection only adds it if model not already in selection
      selection.add(si);//,true);
      // cant do here cos might get deselected later if already selected
    }
    return selection;
  }

  /**
     returns a Vector of Drawable instances that
     overlap the area of the rectangle passed in
     * The selected_only is ignored: first check children 
     * (if its a feature set), if no children hit check self 
     * this follows the old logic and you dont have to make
     * 2 calls one with setFlag=false and if that didnt hit 
     * then with setFlag=false This is the only way i have
     * seen this used so it seems ok to change.
     * Follow up question is it even necasary to check ones 
     * self if none of your children have hit? Is it possible
     * for a feature set to get hit with none of the kids getting hit?
     * Yes it is possible - the intron hits the feature set but not its kids
     * Also if all of your kids get hit should you actually figure this
     * out and return the containing feature set?

     * The above leads to some funny behaviour is the tier is
     * collapsed. What ends up getting selected is individual
     * features for those that features that lie directly beneath
     * the click and the entire feature-set for those with the
     * gap directly beneath the click. Decided to fix this by using
     * the loop twice.
     */
  protected Vector findDrawables(Rectangle rect, boolean selected_only) {
    Vector matches = new Vector();
    if (visibleDrawables == null)
      return matches; // load->clear() -> null

    if (getBounds().intersects(rect)) {
      for (int i=0; i < visibleDrawables.size(); i++) {
        Vector curVis = (Vector) visibleDrawables.elementAt(i);
        // If feature set first check kids - should this be recursive?
        for (int j = 0; j < curVis.size(); j++) {
          Drawable se = (Drawable) curVis.elementAt(j);
          if (se instanceof DrawableSetI) {
            DrawableSetI fset = (DrawableSetI)se;
            for (int k=0; k < fset.size(); k++) {
              Drawable se2 = fset.getDrawableAt(k);
	      if (se2.intersects (rect,
				  transformer,
				  getTierManager())) {
		matches.addElement(se2);
	      }
            }
          }
        }
      }
      if (matches.size() == 0) {
        // Now that nothing was found looking for kids try
        // the entire set
        for (int i=0; i < visibleDrawables.size(); i++) {
          Vector curVis = (Vector) visibleDrawables.elementAt(i);
          for (int j = 0; j < curVis.size(); j++) {
            Drawable se = (Drawable) curVis.elementAt(j);
            if (se.intersects (rect,
                               transformer,
                               getTierManager())) {
              // Changed this to add feature set not 1st element of fs if fs
              matches.addElement(se);
            }
          }
        }
      }
    }
    return matches;
  }

  /** Returns the DrawableFeatureSet that is the top(holder) of all the
      drawables in the view */
  public DrawableSetI getDrawableSet() {
    return dfset;
  }

  /** Returns the topmost model object of the result view, the FeatureSet that
      holds all of the tiers, (just dfset.getFeatureSet()) */
  public FeatureSetI getTopModel() {
    return dfset.getFeatureSet();
  }

  /** Expects a DrawableFeatureSet (which is a FeatureSetI) */
  public void setDrawableSet(DrawableSetI dfset) {
    this.dfset = dfset;
    drawables.clear();
    if (this.dfset != null) {
      // should we create a new manager each time as style may change?
      drawables.addElement(dfset);
      if (manager == null) {
        FeatureTierManager ftm = new FeatureTierManager(getController());
        ftm.setTransformer(transformer);
        setTierManager(ftm);
        setVisible (true);
      }
      manager.setTierData(drawables);
      setScrollValues();
      // useless to do this here as views may be 
      // added/lost thru style change after
      // setDrawableSet which alters the scrollbar
      fireViewEvent(ViewEvent.LIMITS_CHANGED);
    } else {
      System.out.println("Null fset in FeatureView");
    }
  }

  /** Set edge highlighting on for features that have edges that match
   *  selections edges. Recursive goes through descendants of features(via getEdges())
   */
  void setMatchingEdges(Selection selection,boolean state) {
    if (selection.size() == 0)
      return;

    // recursively gets all edges of all descendants
    int[] tmpEdges = getEdges(selection);
    if (tmpEdges.length > 0) {
      Arrays.sort(tmpEdges);
      int [] tmpEdges2 = new int[tmpEdges.length];//2*selSize];
      int nEdge = 0;
      tmpEdges2[nEdge++] = tmpEdges[0];
      for (int i=1;i<tmpEdges.length/*2*selSize*/;i++) {
        if (tmpEdges[i] != tmpEdges[i-1])
          tmpEdges2[nEdge++] = tmpEdges[i];
      }
      edges = new int[nEdge];
      System.arraycopy(tmpEdges2,0,edges,0,nEdge);
      setMatchingEdges(edges,state);
    }
  }

  private int[] getEdges(Selection selection) {
    int[] allEdges= new int[0];
    for (int i=0;i<selection.size();i++) {
      SeqFeatureI feat = selection.getSelectedData(i);
      int[] selFeatEdges = getEdges(feat);
      /* make a temporary copy of the previous array */
      int previousSize = allEdges.length;
      int tmpEdges[] = new int[previousSize];
      System.arraycopy(allEdges,0,tmpEdges,0,previousSize);

      /* make a new array that is big enough for both the
         existing elements and the new elements */
      int newArraySize = previousSize + selFeatEdges.length;
      allEdges = new int[newArraySize];
      System.arraycopy(tmpEdges,0,allEdges,0,tmpEdges.length);
      System.arraycopy(selFeatEdges,0,allEdges,previousSize,selFeatEdges.length);
    }
    return allEdges;
  }

  /** Recursively gets all edges, presently does not check for redundant
   * edges, could be improved to do so */
  private int[] getEdges(SeqFeatureI feat) {
    if (feat.hasKids()) { //canHaveChildren()) {
      FeatureSetI fset = (FeatureSetI)feat;
      int[] allKidsEdges = new int[fset.getNumberOfDescendents()*2];
      int allKidsEdgesIndex=0;
      for (int i=0; i<fset.size(); i++) {
        int[] kidEdges = getEdges(fset.getFeatureAt(i));
        System.arraycopy(kidEdges,0,allKidsEdges,allKidsEdgesIndex,kidEdges.length);
        allKidsEdgesIndex += kidEdges.length;
      }
      // dont need to add selfs edges as it should be covered by children
      return allKidsEdges;
    } else { // not a FeatureSetI (SeqFeatureI leaf)
      int[] edges = new int[] {feat.getStart(),feat.getEnd()};
      return edges;
    }
  }

  public void setMatchingEdges(int [] edges, boolean state) {
    dfset.setEdgeHighlights(edges,state,transformer);
  }

  public void setXOrientation(int direction) {
    super.setXOrientation(direction);
    if (needsTextAvoidUpdate()) {
      manager.doLayoutTiers();
    }
    if (edges != null) {
      setMatchingEdges(edges,true);
    }
  }

  public void clearEdges() {
    if (edges != null) {
      if (dfset != null)
	dfset.setEdgeHighlights(edges,false,transformer);
      edges = null;
    }
  }

  public void clearHighlights() {
    if (dfset != null)
      dfset.setHighlighted(false);
  }

  /**
   * Find the drawables in this view associated with the model in selection and
   * select them.
   * This method is used to deal with external selections
   * (used by ApolloPanel.handleFeatureSelectionEvent)
   */
  public void select(Selection selection) {
    selectedDrawables = new Vector();
    for (int i=0; i<selection.size(); i++) {
      SelectionItem selItem = selection.getSelectionItem(i);
      SeqFeatureI modelFeature = selItem.getData();
      Drawable drawable = dfset.findDrawable(modelFeature);
      if (drawable != null) {
        drawable.setSelected(true);
        selItem.addSelectionListener(drawable);
        selectedDrawables.addElement(drawable);
      }
    }
    // since its an external select might be scrolled off
    verticalScrollToSelection();
    // should also check if strand is hidden or feature is hidden and unhide...
  }

  /** SelectViewI? */
  public void verticalScrollToSelection() {
    verticalScrollToSelection(selectedDrawables); // in ScrollableTierView
  }

  /**
   * Scroll to the Selection - move this to FeatureView?
   */
  public void verticalScrollToSelection(Vector selectedDrawables) {
    if (selectedDrawables==null || selectedDrawables.size()==0)
      return;
    
    // The selected items might have tiers numbers that are no longer valid
    // from a previous tier setup
    // there is no way to tell as far as i can see whether its valid or not so 
    // we have to just remap all tiers - 
    // it would be nice to do this in a more clever fashion but im not
    // sure if its possible
    // I attempted to get the drawables assigned with tier numbers in 
    // FlexibleFeatureTierManager but it did not work - not sure why.
    ((FeatureTierManagerI) manager).synchDrawablesWithTiers();

    // y is a tier value - scrollbar values are in tiers
    int lowestY = 99999999;
    boolean allVisible = true;
    int selsize= selectedDrawables.size();
    for (int i=0;i<selsize;i++) {
      Drawable dsf = (Drawable)selectedDrawables.elementAt(i);
      if (!isVerticallyVisible(dsf))
        allVisible = false;
      // getTierIndex checks if tier number assigned and if not gets one
      int y = getTierYCentre(dsf);
      if (y < lowestY)
        lowestY = y;
    }
    // if the whole selection is visible already then dont scroll
    if (allVisible)
      return;
    // this has to be reverted for upper strand
    if (isUpOrientation())
      lowestY = getMaxScrollbarValue() - lowestY - vScroll.getVisibleAmount();
    setScrollbarValue((int)lowestY); // does this trigger adjustvalchange?
  }

  protected int getTierYCentre(Drawable dsf) {
    return manager.getTier(dsf.getTierIndex(manager)).getDrawCentre();
  }

  /** Check if DrawableseqFeature vertically within viewing part of
   * view, ie not scrolled out */
  private boolean isVerticallyVisible(Drawable dsf) {
    int y = getTierYCentre(dsf);
    if (isUpOrientation())
      y = getMaxScrollbarValue() - y;
    return y > getScrollbarValue()
      && y < getScrollbarValue() + getVisibleScrollbarValue();
  }

  public void setCentre(int Position) {
    super.setCentre(Position);
    setInvalidity(true);
  }

  public void setBounds(Rectangle bounds) {
    super.setBounds(bounds);

    if (needsTextAvoidUpdate()) {
      manager.doLayoutTiers();
    }
    setInvalidity(true);
  }

  public void setZoomFactor(double factor) {
    super.setZoomFactor(factor);
    
    if (needsTextAvoidUpdate()) {
      manager.doLayoutTiers();
    }
    setInvalidity(true);
  }

  /**
   * This only sets the height to a sensible value. It expects the width to be
   * managed by an ApolloLayoutManager.
   */
  public Rectangle getPreferredSize() {
    int height = getDropSpaceSize() + getLeadSpaceSize();
    if (manager != null) {
      height += manager.getTotalHeight();
    }
    return new Rectangle(0,0,1,height);
  }

  public void finalize() {
    //    System.out.println("Finalized feature view  " + getName());
  }

  /** sets internal vars to null: fset, dfset, visibleDrawables, graphics
      should it set them to empty vectors instead of null? T*/
  public void clear() {
    clear(false);
  }

  /** just clears out features from view */
  public void clearFeatures() {
    clear(true);
  }

  protected void clear(boolean justFeatures) {
    dfset = null;
    drawables.clear();
    visibleDrawables = null;
    selectedDrawables = null;
    graphics = null;
    // have to clear features out of manager or
    // visibleDrawables will refill with old features with
    // getVisibleFeatures - can we do this without casting?
    if (manager!=null)
      ((FeatureTierManagerI) manager).clearFeatures();
    if (!justFeatures) {
      super.clear();
    }
  }

  public void setTextAvoidance(boolean state) {
    if (manager != null) {
      if (state) {
        ((FeatureTierManagerI) manager).setTextAvoidance(getTransform(), 
                                                         getGraphics());
      } else {
        ((FeatureTierManagerI) manager).unsetTextAvoidance();
      }
    }
  }

  double lastBPP = 100000000;

  protected boolean needsTextAvoidUpdate() {
    boolean flag = false;
    //    double xCoordsPerPixel = transformer.getXCoordsPerPixel();
    if (manager != null) {
      if (((FeatureTierManagerI) manager).isAvoidingTextOverlaps() && 
          ((FeatureTierManagerI) manager).areAnyTiersLabeled()) {
        int textAvoidLim = Config.getTextAvoidLimit();
        if (transformer.getXCoordsPerPixel() < textAvoidLim) {
          flag = true;
        }
        if (lastBPP < textAvoidLim &&
            transformer.getXCoordsPerPixel() > textAvoidLim) {
          flag = true;
        }
      }
      lastBPP = transformer.getXCoordsPerPixel();
    }
    return flag;
  }

  /** This is overridden by ResultView and AnnotView - make abstract? */
  protected abstract JPopupMenu createPopupMenu(ApolloPanelI ap,
                                                MouseEvent evt);

  public void showPopupMenu(MouseEvent evt) {
    ApolloPanelI ap = (ApolloPanelI)getComponent();

    JPopupMenu popup = createPopupMenu(ap, evt);
    JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class,
                                                              getComponent());
    Point p = SwingUtilities.convertPoint((Component)evt.getSource(),
                                          evt.getX(),
                                          evt.getY(),
                                          frame);
    int xLoc = (int) p.x;
    int yLoc = (int) p.y;
    if (xLoc + popup.getPreferredSize().width > frame.getSize().width)
      xLoc = xLoc - ((xLoc + popup.getPreferredSize().width) -
                     frame.getSize().width);
    if (Config.verticallyMovePopups()) {
      if (yLoc + popup.getPreferredSize().height > frame.getSize().height)
        yLoc = yLoc - ((yLoc + popup.getPreferredSize().height) -
                       frame.getSize().height);
    }
    popup.show(frame,xLoc,yLoc);
  }

  /** given the entirety of what is currently selected, remove anything
      that doesn't belong to this view and return the remaining selections.
      This used to be handled in the Selection class, but it didn't quite
      work, because the 'source'=='where it was originally selected'
      which, may or may not be, the same as this view.
  */
  public Selection getViewSelection(Selection selection) {
    return ((FeatureTierManagerI) manager).getViewSelection(selection);
  }

  /** double-check that the feature sets have been initialized */
  protected void putScrollAtStart() {
    if (dfset != null)
      super.putScrollAtStart();
  }

}
