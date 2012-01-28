package apollo.gui.detailviewers.sequencealigner;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputAdapter;

import apollo.datamodel.FeaturePair;
import apollo.datamodel.Range;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.editor.AnnotationEditor;
import apollo.gui.BaseScrollable;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;
import apollo.gui.event.MouseButtonEvent;
import apollo.gui.synteny.GuiCurationState;

public class ResultMouseListener extends MouseInputAdapter {

  /** the curation state */
  private GuiCurationState curationState;
  /** a MultiSequenceAlignerPanel */
  private MultiSequenceAlignerPanel multiSequenceAlignerPanel;
  /** the annotation panel */
  private MultiTierPanel panel;
  
  private String insertion;
  
  
  
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
  

  /**
   * Constructor
   * 
   * @param panel
   * @param curationState
   */
  public ResultMouseListener(MultiTierPanel panel,
      GuiCurationState curationState) {
    this.panel = panel;
    this.curationState = curationState;
    
    multiSequenceAlignerPanel = null;
    insertion = "";
  }
  
  /**
   * Gets the annotation editor.
   * 
   * @return a result editor
   */
  public AnnotationEditor getAnnotationEditor() {
    return curationState.getAnnotationEditor(
        panel.getStrand().equals(Strand.FORWARD));
  }
  
  /**
   * Gets the current (pixel) position 
   * @return
   */
  public int getPos() {
    return pixelPos;
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
   * 
   */
  public int getBasepair() {
    return panel.tierPositionToBasePair(tierPos);
  }

  /**
   * Gets the scrollable object that will be affected
   * 
   * @return
   */
  public MultiSequenceAlignerPanel getMultiSequenceAlignerPanel() {
    return this.multiSequenceAlignerPanel;
  }
  
  /**
   * Selects the feature at a given pos/tier.
   * 
   * Called on internal selection.
   * Both fires off selection event and shows feature selected.
   */
  void selectResult(int pos, int tier) {
    SeqFeatureI sf = panel.getTier(tier).featureAt(pos, TierI.Level.BOTTOM);
    if (sf == null) {
      sf = panel.getTier(tier).getNextFeature(pos, TierI.Level.BOTTOM);
    }
    //showAnnotSelected(sf, tier); // TODO add red box around selected
    // selection manager select fires event
   // if (sf!=null) curationState.getSelectionManager().select(sf, this);
  }
  
  public void setMultiSequenceAlignerPanel(MultiSequenceAlignerPanel multiSequenceAlignerPanel) {
    this.multiSequenceAlignerPanel = multiSequenceAlignerPanel;
  }
  
  
  
  /**
   * Pops up insertions when moused over, removes when gone
   */ 
  public void mouseMoved(MouseEvent e) {

    Point p = new Point(e.getX(), e.getY());

    int mouseMoveTier = calculateTierNumber(p);
    int mouseMovePos = panel.pixelPositionToTierPosition(calculatePosition(p));
    
    int basePair = panel.tierPositionToBasePair(mouseMovePos);
    
    TierI tier = panel.getTier(mouseMoveTier);
    SeqFeatureI feature = tier.featureAt(mouseMovePos, TierI.Level.BOTTOM);
    
    if (feature != null && feature instanceof FeaturePair){
       FeaturePair fp = (FeaturePair) feature;
       if (tier.getType() == SequenceType.AA) {
         ReadingFrame exonFrame = ReadingFrame.valueOf(feature.getFrame());
         ReadingFrame tierFrame = tier.getReadingFrame();
         int offset = 0;
         
         // is this right?
         if (tierFrame != exonFrame) {
           if (tierFrame == ReadingFrame.THREE) {
             offset = 2;
           } else if (tierFrame == ReadingFrame.TWO) {
             offset = 1;
           } else if (tierFrame == ReadingFrame.ONE) {
             offset = exonFrame == ReadingFrame.TWO ? 1 : 2; 
           }
         }
         
         basePair += (offset*feature.getStrand());
       }
       
       String n = fp.getName();
       
       if (fp.getName().equals("AJ309488")) {
         int i = 1;
       }
       
       int hitIndex = fp.getHitIndex(basePair,
                                     tier.getReference().getResidueType());

       hitIndex += fp.insertionsBefore(hitIndex, 
           fp.getQueryFeature().getExplicitAlignment());
       //hitIndex += fp.insertionsInGroupFrom(hitIndex, 
       //    fp.getQueryFeature().getExplicitAlignment());
       
       if (hitIndex-1 > 0 && '-' == 
           fp.getQueryFeature().getExplicitAlignment().charAt(hitIndex-1)) {
         
         panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         Range r = fp.getInsertionRange(hitIndex-1, 
             fp.getQueryFeature().getExplicitAlignment());
         insertion = 
           fp.getHitFeature().getExplicitAlignment().substring(r.getLow(), r.getHigh());
         
       } else if (hitIndex+1 < fp.getQueryFeature().getExplicitAlignment().length() 
           && '-' == fp.getQueryFeature().getExplicitAlignment().charAt(hitIndex+1)) {
         panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         Range r = fp.getInsertionRange(hitIndex+1, 
             fp.getQueryFeature().getExplicitAlignment());
         insertion = 
           fp.getHitFeature().getExplicitAlignment().substring(r.getLow(), r.getHigh());

       } else {
         panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
         insertion = "";
       }
       
       if (panel.getOrientation() == Orientation.FIVE_TO_THREE) {
         StringBuffer buff = new StringBuffer(insertion);
         buff.reverse();
         insertion = buff.toString();
       }
       
    }
    
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
      //panel.scrollToPosition(pixelPos);
      //this.scrollableObject.scrollToBase(getBasepair());
    }
    
    if (MouseButtonEvent.isLeftMouseClick(e)) {
      // do we want to do anything here?
      //selectResult(tierPos, tier);
      //panel.repaint();
    }
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
    this.pixelPos = calculatePosition(p);
    this.tierPos = panel.pixelPositionToTierPosition(pixelPos);
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
  private int calculatePosition(Point p) {
    return panel.getPositionForPixel(p);
  }
  
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
   * Creates the right click menu for the given location
   * 
   * @param xLoc x pixel
   * @param yLoc y pixel
   */
  private void displayRightClickMenu(int xLoc, int yLoc) {

    ResultRightClickMenu rightClickMenu = 
      new ResultRightClickMenu(curationState, panel);
    
    rightClickMenu.setMultiSequenceAlignerPanel(this.getMultiSequenceAlignerPanel());
    rightClickMenu.setInsertion(insertion);
    rightClickMenu.formatRightClickMenu(tierPos, tier);

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
  
}
