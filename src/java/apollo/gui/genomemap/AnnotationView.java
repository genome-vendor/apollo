package apollo.gui.genomemap;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

import javax.swing.*;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.AnnotationEditor;
import apollo.seq.*;
import apollo.config.Config;
import apollo.gui.Controller;
import apollo.gui.ControlledObjectI;
import apollo.gui.Selection;
import apollo.gui.SelectionManager;
import apollo.gui.drawable.DrawableSetI;
import apollo.gui.event.*;
import apollo.gui.menus.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.FeatureList;

import org.apache.log4j.*;

/**
 * A view to display annotations in
 */
public class AnnotationView extends FeatureView implements
  ControlledObjectI,
  DropTargetViewI,
  AnnotationChangeListener {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AnnotationView.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * The ResultView linked to this AnnotationView for edit purposes.
   */
  protected ResultView       resultView;

  /**
   * Essentially a hash table of all the results in the affiliated
   * ResultView linked to this AnnotationView for edit purposes.
   * Initialized when resultView is set. is this still used?
   */
  private EvidenceFinder   finder;

  /**
   * The SiteView linked to this AnnotationView for edit purposes.
   */
  protected SiteView         siteView;

  /**
   * The AnnotationEditor for performing edits in this view.
   */
  protected AnnotationEditor editor;

  /** Species this annotation view is for, has all the state for that species/szap */
  private GuiCurationState curationState;

  /** Whether editing is enabled, default true */
  private boolean editingEnabled=true;

  public AnnotationView(JComponent component, String name,
                        SelectionManager selectionManager, GuiCurationState curationState) {
    super(component, name, selectionManager);
    this.curationState = curationState;
    // CHECK THIS IS OK!!!
    setDropSpaceSize(20);
    setBackgroundColour(Config.getAnnotationBackground());
  }

  /** Returns the model(not drawable) of top of the model for the view.
      This is the holder of all the Genes.
      This is analogous to ResultViews getTopModel except that it
      returns a AnnotatedFeatureSetI.
      (Should there be a AnnotHolderI with getGenes, getTransposons...?)
  */
  public FeatureSetI getGeneHolder() {
    return dfset.getFeatureSet();
  }

  public FeatureSetI getTopModel() {
    return getGeneHolder();
  }

  public void setDrawableSet(DrawableSetI fset) {
    //dfset.getFeatureSet is the top level model for this view
    // it is a AnnotatedFeatureSetI that holds all the genes in annot view
    // rid dfset passing!
    super.setDrawableSet(fset);
    // We probably only need on AnnotationEditor for both the forward and reverse
    // AnnotationView, presently there is one for each.
    editor = new AnnotationEditor(curationState, getGeneHolder());
    editor.setView (this);
    editor.setParentComponent(getComponent());
  }

  public boolean handleTierManagerEvent(TierManagerEvent evt) {
    setInvalidity(true);
    // previously this presumed that AnnotationView gets to have
    // its preferred size. If AnnotationView is added with
    // ApolloLayoutManager.BOTH then (adding is currently done in
    // StrandedZoomableApolloPanel) AnnotationView will not get
    // its preferred size and this will cause a recursive loop thats
    // only stopped by ApolloPanel.doLayout halting itself after 3
    // recursive loops - which presumes nothing is greater than 3
    // levels deep
    Hashtable vm = ((ApolloLayoutManager)getComponent().getLayout()).viewmap;
    String constraint = (String)vm.get(this);

    // changing this so it checks the constraint put on it -
    // i think this rubs against the layout manager paradigm as i
    // think only the layout manager deals with and knows the layout
    // constraints - but doLayout should only be called here if it is not
    // vertically constrained - in other words i dont think this code
    // belongs here but should somehow be integrated with the layout
    // manager - but im not sure how to go about that yet or how involved
    // that would be to change
    boolean heightUnconstrained = false;
    if (constraint != null) {
      heightUnconstrained=(constraint.equals(ApolloLayoutManager.NONE) ||
                           constraint.equals(ApolloLayoutManager.HORIZONTAL));

    }
    if (heightUnconstrained &&
        getPreferredSize().height != getBounds().height) {
      getComponent().doLayout();
      return true;
    } else {
      return super.handleTierManagerEvent(evt);
    }
  }

  public boolean allowsTierDrags() {
    return false;
  }

  public void annotDrag(DragViewI dragView,
                        Vector annots,
                        boolean doFlag,
                        StringBuffer action) {
    if (dragView.getOriginView() == this) {
      if (annots.size() > 0) {
        if (editor.mergeTranscriptsAllowed()) {
          if (doFlag) {
            editor.mergeTranscripts();
          }
          action.append("Merge transcripts");
        }
      }
    }
  }

  private void featureDrag(DragViewI dragView,
                           Vector annots,
                           boolean doFlag,
                           StringBuffer action) {
    if ((dragView.getOriginView() != this) &&
        !(dragView.getOriginView() instanceof AnnotationView)) {
      if (editor.addGeneOrTranscriptAllowed()) {
        if (doFlag) {
          editor.addGeneOrTranscript();
        }
        action.append("Create new gene or transcript");
      } else if (editor.addExonsAllowed()) {
        if (doFlag) {
         editor.addExons();
        }
        action.append("Add exon(s)");
      } else if (editor.addEvidenceExonsAllowed ()) {
        if (doFlag) {
          editor.addEvidenceExons();
        }
        action.append("Add evidence");
      }
    }
  }


  /**
   * Setting to false disables editing.
   * At the moment this just disables dropping of exons,
   * as other disabling of editing is done by getting rid of the
   * associated menu items. This is presumptious though and really all editing
   * functionality should be disabled, not sure how much of an ordeal that is
   * and how necasary that is at this point.
   */
  public void setEditingEnabled(boolean enable) {
    editingEnabled = enable;
  }
  public boolean isEditingEnabled() {
    return editingEnabled;
  }

  public boolean interpretDrop(DragViewI dragView, MouseEvent evt) {
    StringBuffer action = new StringBuffer();

    return interpretDrop(dragView, evt, true, action);
  }

  public AnnotationEditor getAnnotationEditor() {
    return editor;
  }


  /**
   * Returns true if drop is sucessful (?)
   * If editing is disabled does nothing, returns false.
   * From DropTargetViewI
   */
  public boolean interpretDrop(DragViewI dragView,
                               MouseEvent evt,
                               boolean doFlag,
                               StringBuffer action) {

    if (!editingEnabled)
      return false;

    if (super.interpretDrop(dragView,evt,doFlag,action)) {
      // this is the spot!!
      // don't use the cursor point, rather figure out where the boxes
      // are begin drawn and see if they overlap with the stuff in
      // the annotpanel
      DragView dfv = (DragView)dragView;
      Vector shadowBoxes = dfv.collectShadows();
      Vector annots = findFeatures(shadowBoxes).toVector();

      // This should explicitly call setSel(int,int,Vec,V,V,V)
      // with the 3rd Vec being the resultVect
      editor.setSelections(this,
                           dfv.getSelection(),
                           annots,
                           transformer.toUser(evt.getPoint()).x,
                           getStrand());

      // The interpretation isn't totally based on start and end positions
      // A annot move is a semi-horizontal move within this same view
      // either a vertical (i.e. constrained, the default) move
      // or a diagonal move both must be from a different view.
      // NEW: A drag from a SiteView is treated as a special case
      // even newer?? splice sites (from a resultview) are yet
      // another special case
      if ((dragView.getOriginView() instanceof SiteView) &&
          editor.setTranslationTerminusAllowed()) {
        if (doFlag) {
          editor.setTranslationTerminus();
        }
        action.append("Set start/stop codon");
      }
      else if (!(dragView.getOriginView() instanceof SiteView)) {
        if (dragView.getOriginView() == this) {
          annotDrag(dragView, annots, doFlag, action);
        } else {
          featureDrag(dragView, annots, doFlag, action);
        }
        //       } else if (doFlag) {
        //         System.out.println("What did you mean (over nothing)");
      }
      if (action.length() == 0) {
        action.append("No action");
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {

    if (evt.isCompound()) { // process child events
      for (int i=0; i<evt.getNumberOfChildren(); i++) {
        handleAnnotationChangeEvent(evt.getChildChangeEvent(i));
      }
      return true;
    }

    SeqFeatureI annot = evt.getAnnotTop();
    // i dont think the annot is ever null is it?
    if (!evt.isEndOfEditSession() && annot == null) {
      logger.error("ACE with null annot!!!", new Throwable());
    }

    if (annot != null && annot.getRefFeature() == null)
      return false; // seq errors actually dont have ref feats

//     if (evt.isEndOfEditSession()) {
//       // This is true if the event was a deletion.
//       manager.doLayoutTiers();
//       visibleDrawables = getVisibleDrawables();
//       return true;
//     }

    if (annot != null &&
        //evt.getTypeAsString() != null &&
        ((annot.getStrand() == getStrand()) ||
         annot.getStrand() == 0)) {
      /*
        This makes sure that any new drawables are added
        and any that need to be deleted are handled
      */
      // ! Why is this ignoring the DrawableFeatureSet
      // returned by repairFeatureSet?  Is that important?
      //if (evt.getOperation() != FeatureChangeEvent.REDRAW) {
      //if (!evt.isEndOfEditSession()) {
        dfset.repairFeatureSet(evt);
        // wait until all of the editing is finished before
        // fixing the layout
        //}
//       else {
//         manager.doLayoutTiers();
//         visibleDrawables = getVisibleDrawables();
//       }
    }

    // in new paradigm a "singular" event can both be end of session and a repairable
    // hopefully this will work out ok
    if (evt.isEndOfEditSession()) {
      // This is true if the event was a deletion.
      manager.doLayoutTiers();
      visibleDrawables = getVisibleDrawables();
      return true;
    }

//     else if (annot == null && evt.isEndOfEditSession()) {
//       // This is true if the event was a deletion. NOT TRUE
//       // i dont know about that - it seems to have an annot in AnnotEd?
//       manager.doLayoutTiers();
//       visibleDrawables = getVisibleDrawables();
//     }
    return true;
  }

  public void setResultView(ResultView rv) {
    resultView = rv;
  }

  /** i think evidence finder is pase now that theres edge matching - rip out? */
  public EvidenceFinder getEvidenceFinder() {
    if (finder == null && resultView.getTopModel() != null)
      finder = new EvidenceFinder(resultView.getTopModel());
    return finder;
  }

  public ResultView getResultView() {
    return resultView;
  }

  public void setSiteView(SiteView sv) {
    siteView = sv;
  }

  public SiteView getSiteView() {
    return siteView;
  }

  protected void changeTierHeight(int change) {
    super.changeTierHeight(change);
    manager.doLayoutTiers();
  }

  protected boolean needsTextAvoidUpdate() {
    return manager != null;
  }

  private AnnotationMenu annotMenu;

  protected JPopupMenu createPopupMenu(ApolloPanelI ap, MouseEvent evt) {
    if (annotMenu!=null)
      annotMenu.clear(); // nullify for mem leak
    annotMenu = new AnnotationMenu(curationState, ap, this, ap.getSelection(),
                                   new Point(evt.getX(),evt.getY()));
    return annotMenu;
  }

  protected void clear(boolean justFeatures) {
    super.clear(justFeatures);
    if (annotMenu!=null) annotMenu.clear();
    annotMenu = null;
    editor = null; // makes new editor on new curation with setFeatureSet
    finder = null;
  }

  protected void setCurationSet(CurationSet curation) {
    if (editor != null)
      editor.setCurationSet(curation);
  }

  /**
   * A ControlledObjectI method. This also resets the controller on the editor.
   */
  public void setController(Controller c) {
    super.setController(c);
    if (editor != null) {
      editor.setController(c);
    }
  }
}
