package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.datamodel.*;
import apollo.seq.*;
import apollo.config.Config;
import apollo.editor.AnnotationEditor;
import apollo.editor.ResultChangeEvent;
import apollo.editor.ResultChangeListener;
import apollo.gui.SelectionManager;
import apollo.gui.Selection;
import apollo.gui.event.*;
import apollo.gui.menus.TierPopupMenu;

import org.apache.log4j.*;

/**
 * An extension of FeatureView for drawing analysis results.
 */
public class ResultView extends FeatureView implements
  KeyViewI,
  ResultChangeListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ResultView.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected AnnotationView   annotationView;
  private TierPopupMenu tierPopupMenu;

  public ResultView(JComponent ap,
		    String name,
		    SelectionManager selectionManager) {
    super(ap, name, selectionManager);
  }

  public void setAnnotationView(AnnotationView av) {
    annotationView = av;
  }

  public AnnotationView getAnnotationView() {
    return annotationView;
  }
  
  /** Returns the topmost model object of the result view, the FeatureSet that
      holds all of the tiers, (just dfset.getFeatureSet()) */
  public FeatureSetI getTopModel() {
    if (dfset != null)
      return dfset.getFeatureSet();
    else
      return null;
  }

  protected JPopupMenu createPopupMenu(ApolloPanelI ap, MouseEvent evt) {
    if (tierPopupMenu!=null)
      tierPopupMenu.clear(); // clear mem leaks
    
    tierPopupMenu = new TierPopupMenu(ap, this, ap.getSelection(),
				      new Point(evt.getX(),evt.getY()));
    return tierPopupMenu;
  }

  /** Clear out mem leaks */
  protected void clear(boolean justFeatures) {
    super.clear(justFeatures);
    if (tierPopupMenu!=null)
      tierPopupMenu.clear();
    tierPopupMenu = null;
  }

  /** If return is pressed then take the current selection and make it a new
      annotation. Who knew about this functionality? */
  public void keyPressed(KeyEvent evt) {
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
      AnnotationEditor editor = annotationView.getAnnotationEditor();
      if (editor == null) {
        logger.error("Null editor in keyPressed");
      } else {
        Selection our_selections 
          = getViewSelection(selectionManager.getSelection());
        if (our_selections.size() > 0) {
          editor.setSelections(annotationView, our_selections, new Vector());
          editor.addGeneOrTranscript();
        }
      }
    }
  }

  public boolean handleResultChangeEvent(ResultChangeEvent evt) {
    SeqFeatureI top_sf = evt.getChangeTop();
    // i dont think operation can be null unless endOfEditSession
    if (top_sf != null) {// && evt.getOperationAsString() != null) {
      /* SUZ, I don't think this test is necessary. If it
         was added to speed things up, then we need to
         change it to allow for the case where new overlay
         data is being added, even if the current size is 0
      */
      // if (dfset.size() > 0 &&
      if (top_sf.getStrand() == getStrand() ||
          top_sf.getStrand() == 0) {
        if (!evt.isEndOfEditSession()) {//getType() != FeatureChangeEvent.REDRAW) {
          dfset.repairFeatureSet(evt);
        } 
	else { // REDRAW
          // wait until all of the additons are finished before
          // fixing the layout
          manager.doLayoutTiers();
          setInvalidity(true);
        }
      }
    } else {
      logger.warn("Ignoring INVALID (null pointer for top) feature change in " + getName());
      try {
        throw new Exception ("Where is this from??");
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    return true;
  }
}
