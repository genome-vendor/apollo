package apollo.gui.detailviewers.sequencealigner;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;

import org.bdgp.util.Range;

import apollo.config.Config;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.datamodel.TranslationI;
import apollo.editor.AnnotationEditor;
import apollo.util.ClipboardUtil;
import apollo.gui.BaseScrollable;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;
import apollo.gui.detailviewers.sequencealigner.renderers.AnnotationRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRendererI;
import apollo.gui.event.MouseButtonEvent;
import apollo.gui.synteny.GuiCurationState;

/**
 * Class to handle mouse interactions for an annotation panel
 * 
 * NOTE: It can be hard to keep track of what coordinate system you are working
 * in. This should probably be cleaned up...
 * 
 */
public class AnnotationEditorMouseListener extends MouseInputAdapter {
  
  public static int MIN_FEATURE_SIZE = 1;
  
  /** the curation state */
  private GuiCurationState curationState;
  /** the scrollable object, probably a MultiSequenceAlignerPanel */
  private BaseScrollable scrollableObject;
  /** the annotation panel */
  private MultiTierPanel panel;
  
  private MultiSequenceAlignerPanel mainPanel;
  
  
  private SeqFeatureI dragFeature;
  
  // Don't really remember what coordinates all this stuff is in...
  private boolean dragging = false;
  private Level dragLevel = Level.BOTTOM;
  private int dragStartPos = -1;
  private int dragType = -1;
  
  /** base pair */
  private int limit_3prime = -1;
  /** base pair */
  private int limit_5prime = -1;
  
  
  private int pixelPos = -1;
  private int preDragEndBasePair=-1;
  private int preDragStartBasePair=-1;
  
  private int selectBeginPos = -1;

  private int selectCurrentPos = -1;
  private int startDragTier = -1;
  private int startPos = -1;
  private int tier = -1;

  private int tierPos = -1;
  private int bp = -1;
  
  /**
   * Constructor
   * 
   * @param panel
   * @param curationState
   */
  public AnnotationEditorMouseListener(MultiTierPanel panel,
      GuiCurationState curationState, MultiSequenceAlignerPanel mainPanel) {
    this.panel = panel;
    this.curationState = curationState;
    this.mainPanel = mainPanel;
    
    scrollableObject = null;
    dragFeature = null;
    dragging = false;
    dragLevel = Level.BOTTOM;
    dragStartPos = -1;
    dragType = -1;
    
    limit_3prime = -1;
    limit_5prime = -1;
    
    pixelPos = -1;
    preDragEndBasePair=-1;
    preDragStartBasePair=-1;
    
    selectBeginPos = -1;
    selectCurrentPos = -1;
    startDragTier = -1;
    startPos = -1;
    
    tier = -1;
    tierPos = -1;
    
  }

  /**
   * Calculates the (pixel) position of a given point.
   * 
   * @param p the pixel coordinates of a point on the annotation panel
   * @return the (pixel) position that the point is on
   * 
   * A pixel position represents the offset (starting with 0?) of the
   * base which contains this pixel. The 0th pixel position is the one
   * that is farthest to the left side of the screen.
   */
  /*
  private int calculatePosition(Point p) {
    return panel.getPanel(tier).getPositionForPixel(p);
  }
*/

  /**
   * Calculates the tier number for a given point
   * 
   * @param p the pixel coordinates of a point on the annotation panel
   * @return the tier that the point is on
   */
  private int calculateTierNumber(Point p) {
    int nonAdjustedTier = panel.tierForPixel(p);
    int invisibleTiers = 0;
    
    for (int i = 0; i <= nonAdjustedTier + invisibleTiers && i < panel.numTiers(); i++) {
      AbstractTierPanel tierPanel = panel.getPanel(i);
      if (!tierPanel.isVisible()) {
        invisibleTiers++;
      }
    }
    
    int tierNumber = nonAdjustedTier + invisibleTiers;
    if (tierNumber >= panel.numTiers()) {
      // The mouse is not over an actual tier
      tierNumber = 0;
    }
    
    return tierNumber;
  }

  /**
   * Updates the end of a feature if the new position is within
   * the given limits
   * 
   * @param feature the feature to be updated
   * @param new_end the new end
   * @param limit_5prime the 5prime limit
   * @param limit_3prime the 3prime limit
   * 
   * I think coordinates are in basepair coordinates
   */
  private void changeEnd(SeqFeatureI feature, int new_end,
      int limit_5prime, int limit_3prime) {

    boolean okay = (panel.getStrand().equals(Strand.REVERSE)  ?
        (new_end > limit_3prime && new_end < limit_5prime) :
        (new_end < limit_3prime && new_end > limit_5prime));
    
    if (okay) {
      feature.setEnd(new_end);
      FeatureSetI parent = (FeatureSetI) feature.getRefFeature();
    }
  }

  /**
   * Updates the start of a feature if the new position is within
   * the given limits
   * 
   * @param feature the feature to be updated
   * @param new_start the new start
   * @param limit_5prime the 5prime limit
   * @param limit_3prime the 3prime limit
   * 
   * I think coordinates are in basepair coordinates
   */
  private void changeStart(SeqFeatureI feature, int new_start,
      int limit_5prime, int limit_3prime) {
    
    boolean okay = (panel.getStrand().equals(Strand.REVERSE) ?
        (new_start > limit_3prime && new_start < limit_5prime) :
        (new_start < limit_3prime && new_start > limit_5prime));
    
    if (okay) {
      feature.setStart(new_start);
      FeatureSetI parent = (FeatureSetI) feature.getRefFeature();
    } 
  }

  /**
   * Creates the right click menu for the given location
   * 
   * @param xLoc x pixel
   * @param yLoc y pixel
   */
  private void displayRightClickMenu(int xLoc, int yLoc) {

    AnnotationRighClickMenu rightClickMenu = 
      new AnnotationRighClickMenu(curationState, panel);
    
    rightClickMenu.setScrollableObject(this.getScrollableObject());
    rightClickMenu.formatRightClickMenu(tierPos, bp, tier);

    // Keep the menu from falling off the edge of the screen
    if (xLoc >= panel.getSize().width/2)  // We're on the right side of the screen
      // Move the popup to the left of the cursor
      xLoc = xLoc - rightClickMenu.getPreferredSize().width;
    else
      // Move the popup to the right of the cursor, plus a small nudge
      xLoc = xLoc + 10;

    if (yLoc + rightClickMenu.getPreferredSize().height > panel.getSize().height)
      yLoc = yLoc - ((yLoc + rightClickMenu.getPreferredSize().height) 
                  - panel.getSize().height);
    
    rightClickMenu.show(panel,xLoc,yLoc);
  }
  
  /**
   * Gets the annotation editor.
   * 
   * @return an annotation editor
   */
  public AnnotationEditor getAnnotationEditor() {
    return curationState.getAnnotationEditor(panel.getStrand().equals(Strand.FORWARD));
  }

  /**
   * Gets the current (pixel) position 
   * @return
   */
  /*
  public int getPos() {
    return pixelPos;
  }
  */
  
  /**
   * 
   */
  /*
  public int getBasepair() {
    return panel.tierPositionToBasePair(tierPos);
  }
  */

  /**
   * Gets the scrollable object that will be affected
   * 
   * @return
   */
  public BaseScrollable getScrollableObject() {
    return this.scrollableObject;
  }
  
  /**
   * Gets the current tier
   * 
   * @return
   */
  public int getTier() {
    return tier;
  }

  /** 
   * Handles the mouse click events
   * 
   *  Middle mouse scrolls to where clicked
   *  Left mouse selects a feature
   *  Right mouse brings up popup menu
   */
  public void mouseClicked(MouseEvent e) {
    // update the current position
    setPos(e.getX(), e.getY());

    if (MouseButtonEvent.isRightMouseClickNoShift(e)) {
        displayRightClickMenu(e.getX(), e.getY());
    }
    
    if (MouseButtonEvent.isMiddleMouseClickNoShift(e) ||
        MouseButtonEvent.isRightMouseClickWithShift(e)) {
      this.scrollableObject.scrollToBase(bp);
      //panel.scrollToPosition(pixelPos);
      //this.scrollableObject.scrollToBase(getBasepair());
    }
    
    if (MouseButtonEvent.isLeftMouseClick(e)) {
      selectAnnot(tierPos, tier);
      panel.repaint();
    }
  }

  /**
   * Handles the mouse drag events
   * 
   * Left mouse dragging drags end of feature
   * if drag type right or left boundary.
   * DragType is figured in mousePressed
   * 
   * TODO clean up!
   */
  public void mouseDragged(MouseEvent e) {
    if (!MouseButtonEvent.isLeftMouseClick(e))
      return;
    
    setPos(e.getX(), e.getY());
    
    // scrolls window when dragging at boundaries
    scroll(e.getX(), e.getY());
    
    // dragFeature set in mousePressed
    if (dragging == false && dragFeature != null) {
      //not needed?
      //changer = baseEditorPanel.getAnnotationEditor().beginRangeChange(dragFeature); // ??
      preDragStartBasePair = dragFeature.getStart();
      preDragEndBasePair = dragFeature.getEnd();
    }
    
    dragging = true;
    // dragType determined in MousePressed
    if (dragType == BaseEditorPanel.SEQ_SELECT_DRAG) {
      // Set the cursor
      // TODO: move to own method?
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
      
      int redrawLowPos = selectLowPos();
      int redrawHighPos = selectHighPos();
      if (pixelPos < redrawLowPos)
        redrawLowPos = pixelPos;
      if (pixelPos > redrawHighPos)
        redrawHighPos = pixelPos;

      setSelectCurrentPos(pixelPos);
      BaseRendererI rend = panel.getPanel(tier).getRenderer();
      if (rend instanceof RegionSelectableI) {
        for (int i = 0; i < panel.getPanels().size(); i++) {
          TierPanelI p = panel.getPanel(i);
          BaseRendererI r = p.getRenderer();
          int po = getTierPosition(e.getX(), e.getY(), i);
          ((RegionSelectableI) r).setRegionEnd(po);
        }
      }
      
      panel.repaint();
      return;
      
    } else if (dragFeature != null) {
      // dragType is a boundary change
      

      //TODO handle drag when not holding any other buttons
      int dfPixelStartPos = panel.getPanel(tier).tierPositionToPixelPosition(
          panel.getTier(tier).getPosition(dragFeature.getStart()));
      int dfPixelEndPos = panel.getPanel(tier).tierPositionToPixelPosition(
          panel.getTier(tier).getPosition(dragFeature.getEnd()));
      
      //int pixelPosBP = getBasePair(e.getX(), e.getY(), tier);
      
      int pixelPosBP = panel.tierPositionToBasePair(
          panel.pixelPositionToTierPosition(pixelPos));

        if (panel.getTier(0).getType() == SequenceType.AA) {
          if (dragType == SeqAlignPanel.START_BOUNDARY) {
            int start = dragFeature.getStart();
            int nextBpInFrame = panel.tierPositionToBasePair(
              panel.pixelPositionToTierPosition(dfPixelStartPos+1));
            int offset = Math.abs(nextBpInFrame - start);
            pixelPosBP -= (offset*dragFeature.getStrand());
          } else if (dragType == SeqAlignPanel.END_BOUNDARY) {
            int end = dragFeature.getEnd();
            int prevBpInFrame = panel.tierPositionToBasePair(
              panel.pixelPositionToTierPosition(dfPixelEndPos-1));
            int offset = Math.abs(end - prevBpInFrame);
            pixelPosBP += (offset*dragFeature.getStrand());
          }
        }

      
      if (dragType == SeqAlignPanel.START_BOUNDARY) {
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        if (notTooSmall(pixelPos, dfPixelEndPos)) 
          changeStart(dragFeature, pixelPosBP,limit_5prime, limit_3prime);
        
      } else if (dragType == SeqAlignPanel.END_BOUNDARY) {
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        if (notTooSmall(dfPixelStartPos, pixelPos))
          changeEnd(dragFeature, pixelPosBP,limit_5prime,limit_3prime);
      }
    }

    // repaint with exon seq feature ends changed
    //baseEditorPanel.repaint(pos, dragStartPos);
    panel.repaint();
    dragStartPos = pixelPos;
  }

  /** makes cursor hand if at the end	of an exon using mouses position, 
      does not change pos or tier variable
  */
  public void mouseMoved(MouseEvent e) {
    // setPos sets the pos variable to the mouse position, cant do this
    // because on right click menu want to preserve the pos clicked on 
    // and setPos here changes pos if user moves mouse after right click
    //setPos (e.getX(), e.getY());

    // moveMouseTier and moveMousePos can differ from this.tier and pos
    Point p = new Point(e.getX(), e.getY());
    //int mouseMoveTier = baseEditorPanel.getTierForPixelPosition(e.getY()); //SeqAlignPanel
    //int mouseMovePos = calculatePosition(e.getX(), e.getY());

    int mouseMoveTier = calculateTierNumber(p);
    int mouseMovePos = getTierPosition(e.getX(), e.getY(), mouseMoveTier);
    //int mouseMovePos = panel.pixelPositionToTierPosition(calculatePosition(p));
    
    // make the cursor a hand if it is at the end of a feature
    int type = panel.getTier(mouseMoveTier).getBoundaryType(mouseMovePos, TierI.Level.BOTTOM);
    if (type == SeqAlignPanel.NO_BOUNDARY)
      type = panel.getTier(mouseMoveTier).getBoundaryType(mouseMovePos, TierI.Level.TOP);
      
    if (!e.isShiftDown() && type == SeqAlignPanel.NO_BOUNDARY)
      panel.setCursor(Cursor.getDefaultCursor());
    else if (e.isShiftDown())
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    else
      panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }
  
  /** Right mouse: highlight base.
      Left mouse: figure dragType, dragFeature, 
      dragStartPos, startDragTier */
  public void mousePressed(MouseEvent e) {
    setPos(e.getX(), e.getY());
    BaseRendererI rend = panel.getPanel(tier).getRenderer();

    if (rend instanceof RegionSelectableI) {
      for (TierPanelI p : panel.getPanels()) {
        BaseRendererI r = p.getRenderer();
        ((RegionSelectableI) r).setRegionStart(-1);
        ((RegionSelectableI) r).setRegionEnd(-1);
      }
      panel.repaint();
    }
    
    // show base at right mouse click highlighted (on release - popup menu)
    if (MouseButtonEvent.isRightMouseClickNoShift(e)) {
      
      if (rend instanceof RegionSelectableI) {
        //int target = panel.getModelTierPanel().pixelPositionToTierPosition(pixelPos);
        //for (TierPanelI p : panel.getPanels()) {
        for (int i = 0; i < panel.getPanels().size(); i++) {
          TierPanelI p = panel.getPanel(i);
          BaseRendererI r = p.getRenderer();
          int tierPosition = getTierPosition(e.getX(),e.getY(),i);
          ((RegionSelectableI) r).setRegionStart(tierPosition);
          ((RegionSelectableI) r).setRegionEnd(tierPosition);
        }
        panel.repaint();
        return;
      }
    }

    if (!MouseButtonEvent.isLeftMouseClick(e))
      return;

    // drag has not started
    if (dragStartPos == -1) {
      if (e.isShiftDown()) {
        dragType = BaseEditorPanel.SEQ_SELECT_DRAG; // copying sequence
      } else {
      dragType = panel.getTier(tier).getBoundaryType(tierPos, TierI.Level.BOTTOM);
      dragFeature = panel.getTier(tier).featureAt(tierPos, Tier.Level.BOTTOM);
      dragLevel = Level.BOTTOM;
      }
        
      if (dragType == SeqAlignPanel.NO_BOUNDARY) {
        dragType = panel.getTier(tier).getBoundaryType(tierPos, TierI.Level.TOP);
        dragFeature = panel.getTier(tier).featureAt(tierPos, TierI.Level.TOP);
        dragLevel = Level.TOP;
      }
      
      //if (dragType == SeqAlignPanel.NO_BOUNDARY) {
      //  dragType = BaseEditorPanel.SEQ_SELECT_DRAG;
      //}
      
      dragStartPos = pixelPos;
      startPos = pixelPos;
      startDragTier = tier;
      
      if (dragFeature == null && ((dragType != BaseEditorPanel.SEQ_SELECT_DRAG) ||
          startDragTier >= panel.numTiers())) {
        resetDragState();
        return;
      }

      // What is this doing?
      if (dragType == SeqAlignPanel.START_BOUNDARY) {
        /* the 5 prime edge of the feature (exon) can
           be moved within limits. These are no farther
           in the 3prime direction than the end of the
           feature and no farther in the 5prime direction
           than the end of the preceding exon */
        limit_3prime = dragFeature.getEnd();
        int dfStartPos = panel.basePairToTierPosition(dragFeature.getStart());
        SeqFeatureI precedingFeature = 
          panel.getTier(tier).getPrevFeature(dfStartPos, dragLevel);

        if (precedingFeature == null) {
          limit_5prime = panel.tierPositionToBasePair(0);
        } else {
          limit_5prime = precedingFeature.getEnd() 
            + (1 * precedingFeature.getStrand()); // keep the intron
        }
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        
      } else if (dragType == SeqAlignPanel.END_BOUNDARY) {
        
        limit_5prime = dragFeature.getStart();
        int dfEndPos = panel.basePairToTierPosition(dragFeature.getEnd());
        SeqFeatureI nextFeature = 
          panel.getTier(tier).getNextFeature(dfEndPos, dragLevel);
        
        if (nextFeature == null) {
          SequenceI seq = panel.getTier(tier).getReference();
          limit_3prime = panel.tierPositionToBasePair(seq.getLength()-1);
        } else {
          limit_3prime = nextFeature.getStart()
            - (1 * nextFeature.getStrand()); // keep the intron;
        }
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
      } else if (dragType == BaseEditorPanel.SEQ_SELECT_DRAG) {
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        selectBeginPos = pixelPos;
        selectCurrentPos = pixelPos;
        
        if (rend instanceof RegionSelectableI) {
          for (int i = 0; i < panel.getPanels().size(); i++) {
            TierPanelI p = panel.getPanels().get(i);
            BaseRendererI r = p.getRenderer();
            int pos = getTierPosition(e.getX(),e.getY(),i);
            ((RegionSelectableI) r).setRegionStart(pos);
            ((RegionSelectableI) r).setRegionEnd(pos);
          }
          panel.repaint();
        }
      }
    }
  }
  
  /** If right mouse deselect base 
      If end of feature drag notify AnnotationEditor (endRangeChange) and 
      recalc translation end from start
  */
  public void mouseReleased(MouseEvent e) {
    
    // RIGHT MOUSE CLICK
    if (MouseButtonEvent.isRightMouseClick(e)) {
      BaseRendererI rend = panel.getPanel(tier).getRenderer();
      
      if (rend instanceof RegionSelectableI) {
        for (TierPanelI p : panel.getPanels()) {
          BaseRendererI r = p.getRenderer();
          ((RegionSelectableI) r).setRegionStart(-1);
          ((RegionSelectableI) r).setRegionEnd(-1);
        }
        panel.repaint();
        return;
      }
    }

    // SEQ SELECT DRAG
    if (dragType == BaseEditorPanel.SEQ_SELECT_DRAG) {
      SequenceI seq = panel.getTier(startDragTier).getReference();
      
      int lowBP = panel.tierPositionToBasePair(
          panel.pixelPositionToTierPosition(selectLowPos()));
      int highBP = panel.tierPositionToBasePair(
          panel.pixelPositionToTierPosition(selectHighPos()));
      
      if (lowBP > highBP) {
        int temp = lowBP;
        lowBP = highBP;
        highBP = temp;
      }
      
      String sequence = seq.getResidues(lowBP, highBP);
      
      int start = lowBP;
      int end = highBP;
      if (seq.getRange().getStrand() == -1) {
        start = highBP;
        end = lowBP;
      }

      String header = " Arbitrary selection ("+seq.getName()+": "+
        start + "," + end + ") ";

      //controllingWindow.copySeqToClipboard(new Sequence (header, sequence));
      ClipboardUtil.copySeqToClipboard(new Sequence(header,sequence));

      resetDragState();
      panel.repaint();
      return;
    } 

    // NOT SEQ SELECT DRAG
    else {
      
      // NOT DRAGGING - RETURN
      if (!MouseButtonEvent.isLeftMouseClick(e) || !dragging ||
          dragFeature == null) {
        resetDragState();
        return;
      }
      
      // DRAGGING ANNOT ENDS
      if (dragging && dragFeature != null &&
          dragFeature.isAnnot()) {

        // NOTIFY ANNOTATION EDITOR (generates transaction)
        int oldStart = 
          (preDragStartBasePair==-1) ? dragFeature.getStart() : preDragStartBasePair;
        int oldEnd = 
          (preDragEndBasePair==-1) ? dragFeature.getEnd() : preDragEndBasePair;
        AnnotationEditor ae = getAnnotationEditor();
        ae.setAnnotTerminus(dragFeature.getAnnotatedFeature(),oldStart,oldEnd,
                            dragFeature.getStart(),dragFeature.getEnd());
        recalcCDS(dragFeature);
        panel.repaint();
      }
    }

    resetDragState();
    curationState.getSZAP().repaint();
  }
  

  public static void recalcCDS(SeqFeatureI feature) {
    // RECALC CDS (if exon/transcript - not for 1 level annots)
    if (feature.isExon() &&
        feature.getRefFeature() != null &&
        (feature.getRefFeature().isTranscript())) {
      //ExonI exon = (ExonI)dragFeature;
      //Transcript t = (Transcript)exon.getRefFeature();
      SeqFeatureI transcript = feature.getRefFeature();
      TranslationI cds = transcript.getTranslation();
      int transStart = transcript.getStart();
      int cdsStart = cds.getTranslationStart();
      boolean isForward = transcript.isForwardStrand();
      if (cds.isMissing5prime() &&
          ((isForward && transStart < cdsStart)
           ||
           (!isForward && transStart > cdsStart)) ) {
        cds.calcTranslationStartForLongestPeptide(); // missing start
      } else {
        cds.setTranslationEndFromStart(); // got start - set end
      }
    }
  }
  
  protected void resetDragState() {
    panel.setCursor(Cursor.getDefaultCursor());
    dragStartPos = -1;
    dragType = -1;
    // These are min and max in genomic coord. space
    limit_5prime = -1;
    limit_3prime = -1;
    
    selectBeginPos = -1;
    selectCurrentPos = -1;
    
    dragFeature = null;
    dragging = false;
    preDragStartBasePair = -1;
    preDragEndBasePair = -1;
  }
  
  /**
   * Selects the feature at a given pos/tier.
   * 
   * Called on internal selection.
   * Both fires off selection event and shows feature selected.
   */
  void selectAnnot(int pos, int tier) {
    //SeqFeatureI sf = panel.getTier(tier).featureAt(pos, TierI.Level.TOP);
    SeqFeatureI sf = panel.getTier(tier).featureAt(pos, TierI.Level.BOTTOM);
    if (sf == null) {
      sf = panel.getTier(tier).getNextFeature(pos, TierI.Level.BOTTOM);
    }
    //showAnnotSelected(sf, tier); // TODO add red box around selected
    // selection manager select fires event
    if (sf!=null) curationState.getSelectionManager().select(sf, this);
}

  public int selectHighPos() {
    return (selectBeginPos > selectCurrentPos ?
        selectBeginPos : selectCurrentPos);
  }

  public int selectLowPos() {
    return (selectBeginPos < selectCurrentPos ?
        selectBeginPos : selectCurrentPos);
  }
  
  /**
   * Updates the necessary position state
   * 
   * @param x
   * @param y
   */
  private void setPos(int x, int y) {
    Point p = new Point(x, y);
    this.tier = calculateTierNumber(p);
    this.pixelPos = getPixelPosition(x, y, tier);
    this.tierPos = getTierPosition(x, y, tier);
    this.bp = getBasePair(x, y, tier);
  }

  private int getPixelPosition(int x, int y, int t) {
    Point p = new Point(x,y);
    return panel.getPanel(t).getPositionForPixel(p);
    
  }
  
  private int getTierPosition(int x, int y, int t) {
    Point p = new Point(x,y);
    int pixelPosition = panel.getPanel(t).getPositionForPixel(p);
    int tierPosition = panel.getPanel(t).pixelPositionToTierPosition(pixelPosition);
    int bp = panel.getTier(t).getBasePair(tierPosition);
    
    if (panel.getTier(t).getType() == SequenceType.AA) {
      int offset = AnnotationRenderer.getOffset(tierPosition, panel.getTier(t));
      bp += offset*panel.getTier(t).getStrand().toInt();
      tierPosition = panel.getTier(t).getPosition(bp);
    }
    
    return tierPosition;
  }
  
  private int getBasePair(int x, int y, int t) {
    Point p = new Point(x,y);
    int pixelPosition = panel.getPanel(t).getPositionForPixel(p);
    int tierPosition = panel.getPanel(t).pixelPositionToTierPosition(pixelPosition);
    int bp = panel.getTier(t).getBasePair(tierPosition);
    
    if (panel.getTier(t).getType() == SequenceType.AA) {
      int offset = AnnotationRenderer.getOffset(tierPosition, panel.getTier(t));
      bp += offset*panel.getTier(t).getStrand().toInt();
      tierPosition = panel.getTier(t).getPosition(bp);
    }
    
    return bp;
  }
  
  
  public void setScrollableObject(BaseScrollable scrollableObject) {
    this.scrollableObject = scrollableObject;
  }
  
  void setSelectBeginPos(int sbp) {
    this.selectBeginPos = sbp;
  }
  
  void setSelectCurrentPos(int scp) {
    this.selectCurrentPos = scp;
  }
  
  /** 
   * returns true if the distance between the two end points (inclusive) is 
   * greater than or equal to MIN_FEATURE_SIZE
   */
  public static boolean notTooSmall (int start_pos, int end_pos) {
    return Math.abs(end_pos - start_pos) + 1 >= MIN_FEATURE_SIZE;
  }
  
  
  public void scroll(int x, int y) {
    int bp = mainPanel.getVisibleBase();
    Rectangle rect = mainPanel.getAnnotationPane().getViewport().getViewRect();
    int startPixPos = panel.getPanel(tier).tierPositionToPixelPosition(
        panel.basePairToTierPosition(bp));
    int endPixPos = startPixPos + rect.width/panel.getBaseWidth();
    
    if (getPixelPosition(x,y,tier) >= endPixPos-1) {
      scrollRight();
    } else if (getPixelPosition(x,y,tier) <= startPixPos+1){
      scrollLeft();
    }
  }
  
  
  public void scrollLeft() {
    int bp = mainPanel.getVisibleBase();
    int startPixPos = panel.getPanel(tier).tierPositionToPixelPosition(
        panel.basePairToTierPosition(bp));
    
    int base = panel.tierPositionToBasePair(
        panel.pixelPositionToTierPosition(startPixPos-1));
    mainPanel.scrollToBase(base);
  }
  
  public void scrollRight() {
    int bp = mainPanel.getVisibleBase();
    int startPixPos = panel.getPanel(tier).tierPositionToPixelPosition(
        panel.basePairToTierPosition(bp));
    
    int base = panel.tierPositionToBasePair(
        panel.pixelPositionToTierPosition(startPixPos+1));
    mainPanel.scrollToBase(base);
  }
  
  
}
