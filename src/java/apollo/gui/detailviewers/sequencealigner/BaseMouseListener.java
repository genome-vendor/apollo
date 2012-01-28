package apollo.gui.detailviewers.sequencealigner;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;

import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.datamodel.TranslationI;
import apollo.editor.AnnotationEditor;
import apollo.util.ClipboardUtil;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRenderer;
import apollo.gui.event.MouseButtonEvent;

/** class to handle all things mousey in BaseEditorPanel - used to be inner class
    but i felt it was getting too big for its britches - the interface between this
    and base editor panel could probably be improved but not a biggie as this
    class is just a servant to base editor panel. */
class BaseMouseListener extends MouseInputAdapter {

  private BaseEditorPanel baseEditorPanel;
  int dragStartPos = -1;
  int startPos = -1;
  int preDragStartBasePair=-1;
  int preDragEndBasePair=-1;
  int dragType = -1;
  int limit_3prime = -1;
  int limit_5prime = -1;
  SeqFeatureI dragFeature;
  boolean dragging = false;

  int tier = -1;
  int startDragTier = -1;
  int pos = -1;

  BaseMouseListener(BaseEditorPanel baseEditorPanel) {
    this.baseEditorPanel = baseEditorPanel;
  }

  public int getTier() {
    return tier;
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int x, int y) {
    /* after much to-ing and fro-ing it has
       become apparent that pos(itions) within
       a row's seqwrapper are 0-based (Not
       1-based as the sequence is
    */
    this.tier = baseEditorPanel.getTierForPixelPosition(y); // method of SeqAlignPanel
    this.pos = calculatePosition(x,y);
  }

  /** Cacluates logical position of x,y pixel values, but does not
      set the pos variable */
  private int calculatePosition(int x, int y) {
    int row = baseEditorPanel.getRowForPixelPosition(y);
    int col = baseEditorPanel.getColForPixelPosition(x);
    // getColumnCount from super class SeqAlignPanel
    return (baseEditorPanel.getColumnCount() * row) + col;
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
    int mouseMoveTier = baseEditorPanel.getTierForPixelPosition(e.getY()); //SeqAlignPanel
    int mouseMovePos = calculatePosition(e.getX(), e.getY());

    // make the cursor a hand if it is at the end of a feature
    int type = baseEditorPanel.getBoundaryType(mouseMovePos, mouseMoveTier);
    if (type == baseEditorPanel.NO_BOUNDARY)
       baseEditorPanel.setCursor(Cursor.getDefaultCursor());
    else
       baseEditorPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  /** Right mouse: highlight base.
      Left mouse: figure dragType, dragFeature, 
      dragStartPos, startDragTier */
  public void mousePressed(MouseEvent e) {
    setPos(e.getX(), e.getY());

    // show base at right mouse click highlighted (on release - popup menu)
    if (MouseButtonEvent.isRightMouseClickNoShift(e)) {
      BaseRenderer rend = baseEditorPanel.getRendererAt(tier);
      if (rend instanceof SelectableDNARenderer) {
        ((SelectableDNARenderer) rend).setTargetPos(pos, tier);
        baseEditorPanel.repaint();
        return;
      }
    }

    if (!MouseButtonEvent.isLeftMouseClick(e))
      return;

    if (dragStartPos == -1) {

      if (e.isControlDown())
        dragType = baseEditorPanel.SEQ_SELECT_DRAG;
      else
        dragType = baseEditorPanel.getBoundaryType(pos, tier);

      dragFeature = baseEditorPanel.getFeatureAtPosition(pos, tier);

      dragStartPos = pos;
      startPos = pos;
      startDragTier = tier;
      if (dragFeature == null && ((dragType != baseEditorPanel.SEQ_SELECT_DRAG) ||
                                  startDragTier > baseEditorPanel.getTierCount() ||
                                  startDragTier < 3)) {
        resetDragState();
        return;
      }

      if (dragType == baseEditorPanel.START_BOUNDARY) {
        /* the 5 prime edge of the feature (exon) can
           be moved within limits. These are no farther
           in the 3prime direction than the end of the
           feature and no farther in the 5prime direction
           than the beginning (5prime) of the preceding
           intron */
        limit_3prime = (int) dragFeature.getEnd();
        int lowBound = baseEditorPanel.basePairToPos(dragFeature.getStart());
        // subtract one to move into the preceding intron
        double [] lowRange = baseEditorPanel.getRangeAtPosition(tier,
                                                lowBound - 1);
        if (lowRange[0] < 0)
          limit_5prime = baseEditorPanel.posToBasePair(0);
        else
          limit_5prime = baseEditorPanel.posToBasePair((int) lowRange[0]);
        baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                    Cursor.W_RESIZE_CURSOR));
      } else if (dragType == baseEditorPanel.END_BOUNDARY) {
        limit_5prime = (int) dragFeature.getStart();
        int highBound = baseEditorPanel.basePairToPos((int) dragFeature.getEnd());
        double [] highRange = baseEditorPanel.getRangeAtPosition(tier, highBound + 1);
        SequenceI seq = baseEditorPanel.getSequenceForTier(tier);
        if (highRange[1] > seq.getLength())
          limit_3prime = baseEditorPanel.posToBasePair(seq.getLength());
        else
          limit_3prime = baseEditorPanel.posToBasePair((int) highRange[1]);
        baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                    Cursor.E_RESIZE_CURSOR));
      } else if (dragType == baseEditorPanel.SEQ_SELECT_DRAG) {
        baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                    Cursor.TEXT_CURSOR));
        baseEditorPanel.setSelectBeginPos(pos);
        baseEditorPanel.setSelectCurrentPos(pos);
      }
    }
  }
  /** Left Mouse dragging drags end of feature if drag type right or 
      left boundary. dragType is figured in mousePressed */
  public void mouseDragged(MouseEvent e) {
    if (!MouseButtonEvent.isLeftMouseClick(e))
      return;
    // dragFeature set in mousePressed
    // not needed
    if (dragging == false && dragFeature != null) {
      //changer = baseEditorPanel.getAnnotationEditor().beginRangeChange(dragFeature);// ??
      preDragStartBasePair = dragFeature.getStart();
      preDragEndBasePair = dragFeature.getEnd();
    }
    dragging = true;
    // dragType determined in MousePressed
    if (dragType == baseEditorPanel.START_BOUNDARY)
      baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                  Cursor.W_RESIZE_CURSOR));
    else if (dragType == baseEditorPanel.END_BOUNDARY)
      baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                  Cursor.E_RESIZE_CURSOR));
    else if (dragType == baseEditorPanel.SEQ_SELECT_DRAG) {
      baseEditorPanel.setCursor(Cursor.getPredefinedCursor(
                  Cursor.TEXT_CURSOR));
    }
    setPos(e.getX(), e.getY());
    startPos = pos;
    // going right
    // substract the one because during dragging
    // the person operating the mouse drags it
    // one position beyond where they want the
    // drop to occur.
    // pos--;

    if (dragType == baseEditorPanel.START_BOUNDARY) {
      // Is this position okay to use as the new
      // low end or will it make the feature too short?
      if (baseEditorPanel.notTooSmall(pos,baseEditorPanel.basePairToPos((int)dragFeature.getEnd()))) 
        // this actually changes the model - do we really need to do that
        // with every mouse drag - cant we just change it at the end of the 
        // drag? No - the EDE relies on the feature being changed - bummer
        baseEditorPanel.changeStart(dragFeature, pos - dragStartPos,limit_5prime, limit_3prime);
    } else if (dragType == baseEditorPanel.END_BOUNDARY) {
      // Is this position okay to use as the new
      // high end or will it make the feature too short?
      if (baseEditorPanel.notTooSmall(baseEditorPanel.basePairToPos((int)dragFeature.getStart()),pos))
        // This changes the actual exon model! EDE relies on this
        baseEditorPanel.changeEnd(dragFeature, pos - dragStartPos,limit_5prime,limit_3prime);
    } else if (dragType == baseEditorPanel.SEQ_SELECT_DRAG) {
      int redrawLowPos = baseEditorPanel.selectLowPos();
      int redrawHighPos = baseEditorPanel.selectHighPos();
      if (pos < redrawLowPos)
        redrawLowPos = pos;
      if (pos > redrawHighPos)
        redrawHighPos = pos;
      //selectCurrentPos = pos;
      baseEditorPanel.setSelectCurrentPos(pos);
      baseEditorPanel.repaint(redrawLowPos, redrawHighPos);
      return;
    }
    // repaint with exon seq feature ends changed
    baseEditorPanel.repaint(pos, dragStartPos);
    dragStartPos = pos;
  }

  protected void resetDragState() {
    baseEditorPanel.setCursor(Cursor.getDefaultCursor());
    dragStartPos = -1;
    dragType = -1;
    // These are min and max in genomic coord. space
    limit_5prime = -1;
    limit_3prime = -1;
    baseEditorPanel.setSelectBeginPos(-1);
    //selectCurrentPos = -1;
    baseEditorPanel.setSelectCurrentPos(-1);
    dragFeature = null;
    dragging = false;
    preDragStartBasePair = -1;
    preDragEndBasePair = -1;
  }

  /** If right mouse deselect base 
      If end of feature drag notify AnnotationEditor (endRangeChange) and 
      recalc translation end from start
  */
  public void mouseReleased(MouseEvent e) {
    
    // RIGHT MOUSE CLICK
    if (MouseButtonEvent.isRightMouseClick(e)) {
      BaseRenderer rend = baseEditorPanel.getRendererAt(tier);
      if (rend instanceof SelectableDNARenderer) {
        ((SelectableDNARenderer) rend).setTargetPos(-1,-1);
        baseEditorPanel.repaint();
      }
    }

    // SEQ SELECT DRAG
    if (dragType == baseEditorPanel.SEQ_SELECT_DRAG) {
      SequenceI seq = baseEditorPanel.getSequenceForTier(startDragTier);
      int lowBP = baseEditorPanel.posToResidue(baseEditorPanel.selectLowPos());
      int highBP = baseEditorPanel.posToResidue(baseEditorPanel.selectHighPos());
      String sequence = seq.getResidues(lowBP, highBP);

      String header = " Arbitrary selection ("+seq.getName()+": "+
        lowBP + "," + highBP + ")";

      //controllingWindow.copySeqToClipboard(new Sequence (header, sequence));
      ClipboardUtil.copySeqToClipboard(new Sequence(header,sequence));

      resetDragState();
      baseEditorPanel.repaint();
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
        AnnotationEditor ae = baseEditorPanel.getAnnotationEditor();
        ae.setAnnotTerminus(dragFeature.getAnnotatedFeature(),oldStart,oldEnd,
                            dragFeature.getStart(),dragFeature.getEnd());

        // RECALC CDS (if exon/transcript - not for 1 level annots)
        if (dragFeature.isExon() &&
            dragFeature.getRefFeature() != null &&
            (dragFeature.getRefFeature().isTranscript())) {
          //ExonI exon = (ExonI)dragFeature;
          //Transcript t = (Transcript)exon.getRefFeature();
          SeqFeatureI transcript = dragFeature.getRefFeature();
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
        
        baseEditorPanel.repaint();
      }
    }

    resetDragState();
    baseEditorPanel.getCurationState().getSZAP().repaint();
  }

  /** Middle mouse scrolls to where clicked, left mouse selects
      Right mouse brings up popup menu */
  public void mouseClicked(MouseEvent e) {
    setPos (e.getX(), e.getY());

    if (MouseButtonEvent.isRightMouseClickNoShift(e)) {
      if (tier > 2)
        baseEditorPanel.displayRightClickMenu(e.getX(), e.getY());
    }
    if (MouseButtonEvent.isMiddleMouseClickNoShift(e)) {
      baseEditorPanel.scrollToPosition(pos);
    }
    // This is for Windows, which doesn't support middle clicks
    else if (MouseButtonEvent.isRightMouseClickWithShift(e)) {
      baseEditorPanel.scrollToPosition(pos);
    }
    if (MouseButtonEvent.isLeftMouseClick(e)) {
      baseEditorPanel.selectAnnot(pos, tier);
      baseEditorPanel.repaint();
    }
  }
}
