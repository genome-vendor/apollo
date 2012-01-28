package apollo.gui;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.ResultChangeEvent;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import apollo.gui.event.*;

import org.apache.log4j.*;

/**
 * Converts events from apollo to jalview and vice versa
 */
public class ApolloJalviewEventBridge implements 
      apollo.gui.event.BaseFocusListener,
      apollo.editor.AnnotationChangeListener,
      apollo.gui.event.FeatureSelectionListener,
      apollo.gui.event.NamedFeatureSelectionListener,
      apollo.dataadapter.DataLoadListener,
      apollo.editor.ResultChangeListener,
      jalview.gui.event.ColumnSelectionListener,
      jalview.gui.event.AlignViewportListener,
      jalview.gui.event.SequenceSelectionListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ApolloJalviewEventBridge.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  apollo.gui.Controller  apolloController;
  jalview.gui.Controller jalviewController;
  jalview.gui.AlignFrame alignFrame;
  
  public ApolloJalviewEventBridge(apollo.gui.Controller apolloController,
                                  jalview.gui.Controller jalviewController,
                                  jalview.gui.AlignFrame alignFrame) {
    this.jalviewController = jalviewController;
    this.apolloController  = apolloController;
    this.alignFrame = alignFrame;
    apolloController.addListener(this);
    jalviewController.addListener(this);
  }

  public jalview.gui.Controller getJalviewController() {
    return jalviewController;
  }

  public JFrame getAlignFrame() {
    return alignFrame;
  }


  public boolean handleAnnotationChangeEvent (AnnotationChangeEvent evt) {
    logger.debug("Handle annotation change event in AJEB");
    return true;
  }

  public boolean handleNamedFeatureSelectionEvent(NamedFeatureSelectionEvent evt) {
    logger.debug("Handle named feature selection event in AJEB");
    return true;
  }

  public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
    logger.debug("Handle feature selection event in AJEB");
    return true;
  }

  public boolean handleDataLoadEvent (DataLoadEvent evt) {
    logger.debug("Handle data load event in AJEB");
    alignFrame.dispose();
    apolloController.removeListener(this);
    jalviewController.removeListener(this);
    return true;
  }


  public boolean handleResultChangeEvent (ResultChangeEvent evt) {
    logger.debug("Handle results change event in AJEB");
    return true;
  }

  // not used yet - commenting out for now
//   public boolean handlePropSchemeChangedEvent (PropSchemeChangeEvent evt) {
//     logger.debug("Handle prop scheme change event in AJEB");
//     return true;
//   }

  public boolean handleBaseFocusEvent (BaseFocusEvent evt) {
    logger.debug("Handle base focus event in AJEB");
    alignFrame.getAlignViewport().setStartRes(evt.getFocus());
    //jalviewController.handleAlignViewportEvent(new jalview.gui.event.AlignViewportEvent(this,
    //alignFrame.getAlignViewport(),
    //jalview.gui.event.AlignViewportEvent.LIMITS));
    return true;
  }

  public boolean handleSequenceSelectionEvent (jalview.gui.event.SequenceSelectionEvent evt) {
    logger.debug("Handle sequence selection event in AJEB");

    jalview.gui.Selection sel = evt.getSelection();
    String names[] = new String[sel.size()];

    for (int i=0; i<sel.size(); i++) {
      names[i] = ((jalview.datamodel.AlignSequenceI)sel.sequenceAt(i)).getName();
      logger.debug(names[i]);
    }
    if (names.length != 0) {
      apolloController.handleNamedFeatureSelectionEvent(
               new apollo.gui.event.NamedFeatureSelectionEvent(this,names));
    }
    
    return true;
  }

  public boolean handleColumnSelectionEvent (jalview.gui.event.ColumnSelectionEvent evt) {
    logger.debug("Handle column selection event in AJEB");
    jalview.gui.ColumnSelection colSel = evt.getColumnSelection();
    int jalviewCol = -1;

    for (int i=0; i<colSel.size(); i++) {
      // why go thru multiple columns if only using one?
      jalviewCol = (int)colSel.columnAt(i); 
    }
    int genomic = alignFrame.getMapper().condensed2genomic(jalviewCol);
    if (genomic != -1) {
      BaseFocusEvent b = new BaseFocusEvent(this,genomic,null);
      apolloController.handleBaseFocusEvent(b);
    }
    
    return true;
  }

  // PositionFocusEvent needs to be committed
//   public boolean handlePositionFocusEvent (jalview.gui.event.PositionFocusEvent evt) {
//     System.out.println("Handle position focus event in AJEB");
//     return true;
//   }

  public boolean handleAlignViewportEvent (jalview.gui.event.AlignViewportEvent evt) {
      //System.out.println("Handle align viewport event in AJEB type = " +  evt.getType());
    if (evt.getType() == jalview.gui.event.AlignViewportEvent.HSCROLL) {
	// MC - comment these out for now until I've debugged the sequence display stuff
	//apolloController.handleBaseFocusEvent(new apollo.gui.event.BaseFocusEvent(this,alignFrame.getAlignViewport().getStartRes(),null));
    }
    return true;
  }

}
