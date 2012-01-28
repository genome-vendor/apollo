package apollo.editor;

import java.awt.Component;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import apollo.util.FeatureList;
import apollo.config.ApolloNameAdapterI;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.OverlapI;
import apollo.config.PropertyScheme;
import apollo.config.SimpleOverlap;
import apollo.datamodel.*;
import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.DetailInfo;
import apollo.gui.Selection;
import apollo.gui.SelectionManager;
import apollo.gui.drawable.DrawableAnnotationConstants;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.genomemap.AnnotationView;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.genomemap.TierViewI;
import apollo.gui.synteny.GuiCurationState;

import org.apache.log4j.*;

/**
 * A class for performing edits on the annotations.
 * <BR>
 * How to use the editor: <BR>
 *   Using the AnnotationEditor is a two step process: <BR>
 *      1. call setSelections with the current selections. <BR>
 *      2. call the method for the edit you want. <BR>
 * <BR>
 * How it works:
 *   setSelections creates a group of SelectionSets. These contain the
 *   data selected by the Selection and the data selected under the
 *   cursor. Some processing is done on the basic selected features to
 *   allow easy access to the true (non Drawable) features, the Transcripts,
 *   and the Genes in the selection. The data selected by the Selection is
 *   also divided into subsets containing the data from the AnnotationView
 *   and the data from the ResultView.
 * <BR> <BR>
 *   The AnnotationEditor marks the selections as used at the end of
 *   each of the edit methods. If you try to use the same selections
 *   again a warning is given.
 * <BR> <BR>
 *   The edit methods are all overloaded, with a no arguments version
 *   which uses the default selections. This feeds the correct data
 *   to the method with arguments which actually does the edit. This
 *   method could be called directly because it does not use the
 *   SelectionSets for anything (except the endEdit).
 *   Some of the methods do use the dfset to get the top annotations.
 * <BR> <BR>
 *   The edit methods do the edits by modifying the real features and
 *   firing AnnotationChangeEvents to the Controller listeners
 *   implementing the AnnotationChangeListener interface.
 * <BR> <BR>
 * Important notes about editting: <BR>
 *   1. If you edit an ExonI this may affect multiple Transcripts. <BR>
 *   2. Because of 1 setting limits means regenerating all transcript
 *      feature set limits in ALL cases (even internal ExonIs in one
 *      transcript could be terminal ExonIs in
 *      another). <BR>
 *   3. Watch the order you do the edit and the fire especially for deletes.
 *      <BR>
 *   4. Genes are automatically deleted when they run out of Transcripts. <BR>
 *   5. If you write new edits think if you need to do a consolidateGenes,
 *      splitGenes or updateGenes. <BR>
 *   6. adjustEdges calls often help. There is no automatic propogation of
 *      limit changes in the Annotation tree. <BR>

 * Currently There is one AnnotationEditor for each strand/AnnotView. Im wondering if
 * this could be ammended so theres one AnnotationEditor for both strands.

 * AnnotationEditor has been moved from gui to editor. Its still rather tangled with the
 * gui, which should be untangled eventually.

 * This class is too big and unwieldy - its 4000 lines! needs to be broken up somehow
 */

public class AnnotationEditor implements ControlledObjectI,
  DrawableAnnotationConstants {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------
  
  private static final Logger      logger = LogManager.getLogger(AnnotationEditor.class);

  /** I think these constants should be used everywhere,
      rather than the strings,
      but for now they're just here in AnnotationEditor. */
  static String ALL_DONE =    "all done";
  static String NOT_DONE =    "not done";
  
  static boolean DO_ONE_LEVEL_ANNOTS = false;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  /**
   * The controller to which AnnotationChangeEvents are sent.
   */
  Controller                  controller = null;

  /** Who to fire AnnotationChangeEvents to */
  private AnnotationChangeListener annotCngListener;

  /**
   The top of the annotations AnnotatedFeatureI. This is model NOT drawable.
   This is AnnotViews dfset.getFeature. It is the top model object for
   the AnnotView being edited. It is the holder of genes of a strand, of
   all genes in an AnnotView.
   */
  FeatureSetI annotationCollection = null;

  /**
     The entire curation set, not just the portion on one strand
     (as above in the annotationCollection */
  CurationSet curation = null;

  /**
   * The coalescer for a particular edit.
   dont need this as object state - not used that way
   */
  //AnnotationChangeCoalescer      coalescer  = null;

  /**
   * The description of the current edit. This isnt actually used - remove?
   */
  //String                      editDescription = null;

  /**
   * One of the four SelectionSets. This one is made up of the features
   * under the cursor (for the default edits at least).
   */
  private SelectionSet                cursorSet;

  /**
   * One of the four SelectionSets. This one is made up of the features from
   * the selection in the current view (for the default edits at least).
   */
  private SelectionSet                annotSet;

  /**
   * One of the four SelectionSets. This one is made up of the features
   * from the selection in the result view (for the default edits at least).
   */
  SelectionSet                evidenceSet;

  /**
   * One of the four SelectionSets. This one is made up of the features from
   * the selection in the start-stop codon view (for the default edits at
   least).
   */
  SelectionSet                siteSet;

  /**
   * The base position at which the current edit is occuring.
   */
  int                        basePosition = -1;

  /**
   * endBasePosition specifies the end of the feature to be added????
   */
  int                        endBasePosition = -1;

  /**
   * This strand is used if no information is available from features.
   */
  int                         defaultStrand;

  /**
   * Indicates whether the defaultStrand has been set.
   */
  boolean                     strandSet = false;

  /**
   * The parentComponent is needed for displaying messages in a dialog box.
   */
  Component                   parentComponent;

  /**
   * We need access to annotationView later on when we
   * select the newly-created thing
   */
  AnnotationView              annotationView;

  /** should just be CurationState - eventually */
  private GuiCurationState curationState;

  private FeatureList oldTranscripts;
  private FeatureList newTranscripts;
  private FeatureList oldAnnots;

  /**
   * The AnnotationEditor constructor. This sets the controller to use for
   *
   * @param c The controller to use for propogating AnnotationChangeEvents
   * for edits
   * @param dfset The TOP of the DrawableAnnotatedFeatureSet tree to edit.
   @param annotationCollection top level model of annot view being edited
   */
  public AnnotationEditor(GuiCurationState curationState, FeatureSetI annotationCollection) {
    this.curationState = curationState;
    this.annotationCollection = annotationCollection;
    setController(curationState.getController());
    annotCngListener = curationState.getController();
    // does annotationCollection have strand? it should if it doesnt
    curationState.addAnnotationEditor(this,annotationCollection.isForwardStrand());
  }

  /**
   * ControlledObjectI method for setting the controller for the editor.
   */
  public void setController(Controller c) {
    controller = c;
  }

  /**
     The curation set needed when naming and giving IDs to the
     annotations */
  public void setCurationSet(CurationSet curation) {
    this.curation = curation;
  }

  /**
   * ControlledObjectI method for getting the controller for the editor.
   */
  public Controller getController() {
    return controller;
  }

  /**
   * ControlledObjectI method. The editor has no window so this method
   * returns null.
   */
  public Object getControllerWindow() {
    return null;
  }
  /** ControlledObjectI. getContollerWindow is null so this is irrelevant */
  public boolean needsAutoRemoval() {
    return true;
  }

  /**
   * Sets the component to enable error and warning messages to be
   * show in dialog boxes. Should the popping up of warning messages happen
   * in some gui class if thats possible?
   */
  public void setParentComponent(Component comp) {
    parentComponent = comp;
//     ApolloFrame frame = (ApolloFrame)
//                         SwingMissingUtil.getWindowAncestor(parentComponent);
  }

  /**
   * Gets the parent component which is set to enable error and
   * warning messages to be shown in dialog boxes.
   */
//   public Component getParentComponent() {
//     return parentComponent;
//   }

  /**
   * Reinitialises the four SelectionSets and basePosition, endBasePosition and
   * strandSet.
   */
  protected void resetData() {
    annotSet = cursorSet = evidenceSet = siteSet = null;
    basePosition = endBasePosition = -1;
    strandSet = false;
  }

  /**
   * Uses the Selection and AnnotationView passed to generate the EvidenceSet
   * (the features from the Selection in the ResultView for the
   * AnnotationView),
   * the annotSet (the features from the Selection in the AnnotationView) and
   * the annotSet (all the features in the Selection). The cursorSet comes
   * from the underCursor Vector.
   * This is used by ResultView.keyPressed creating annot on "enter" key press.
   *
   * @param v The AnnotationView used for dividing the Selection.
   * @param selection The Selection to be divided.
   * @param underCursor A vector of features under the cursor location.
   */
  public void setSelections(AnnotationView v,
                            Selection selection,
                            Vector underCursor) {
    // Remember this for later--if our selection changes, we'll need it
    this.annotationView = v;

    /** getSelectedData(ViewI) is problematic, only gets selections with
        view as source. If a selection came from a different source this
        wont get it, even if it is actually selected in that view. Yikes! */
    setSelections(underCursor,
                  v.getViewSelection(selection).getSelectedVector(),
                  v.getResultView().getViewSelection(selection).getSelectedVector(),
                  v.getSiteView().getViewSelection(selection).getSelectedVector());
  }

  /**
   * Uses the Selection and AnnotationView passed to generate the EvidenceSet
   * (the features from the Selection in ResultView for the AnnotationView),
   * the annotSet (the features from the Selection in the AnnotationView) and
   * the annotSet (all the features in the Selection). The cursorSet comes
   * from the underCursor Vector. Also sets the basePosition and
   * defaultStrand (used in some edits).
   *
   * @param v The AnnotationView used for dividing the Selection.
   * @param selection The Selection to be divided.
   * @param underCursor A vector of features under the cursor location.
   * @param basePosition This sets this.basePosition (used in position specific
   *                     edits such as {@link #splitFeature()}.
   * @param strand This sets defaultStrand (used in {@link #createAnnotation()}).
   (This is the call AnnotationMenu and TierPopupMenu use)
   */
  public void setSelections(AnnotationView annotView,
                            Selection selection,
                            Vector underCursor,
                            int basePosition,
                            int strand) {
    // Remember this for later--if our selection changes, we'll need it
    this.annotationView = annotView;

    // replaces selection.getSelectedData(AV) which doesnt take into account
    // selections from sources other than annot view
    boolean checkForRedundantDescendants = true;
    Selection annotSel  =
      selection.getSelectionDescendedFromModel(annotationCollection,
                                               checkForRedundantDescendants);
    Vector annotVector = annotSel.getSelectedVector();
    FeatureSetI topModel = annotView.getResultView().getTopModel();
    Selection resultSel = selection.getSelectionDescendedFromModel(topModel);
    Vector resultVector = resultSel.getSelectedVector();

    setSelections(underCursor, // -> cursorSet
                  annotVector,
                  // faulty - only gets if selected in res view - no ext sel
                  resultVector, // evidenceSet
                  // this is ok for now since sites cant be externally selected
                  annotView.getSiteView().getViewSelection(selection).getSelectedVector());
    strandSet = true;
    this.basePosition = basePosition;
    // This was omitted even though its in the javadoc above
    this.defaultStrand = strand;
  }

  public void setView(AnnotationView annotView) {
    // Remember this for later--if our selection changes, we'll need it
    this.annotationView = annotView;
  }

  /**
   * sets the four SelectionSets which are used in the edit methods, from the
   * four input Vectors. This setSelections should be called from all the
   * others.
   * <BR>
   * NOTE: It calls {@link #resetData()}.
   * This constructor is used by EDE to make an intron via splitExon that extends to
   * splice sites. This currently doesnt work properly (intron always 1 bp)
   * see BaseEditorPanel.RichtClickActionListener
   *
   Change these to FeatureList
   * @param basePosition start base position of edit (start of intron)
   * @param endBasePosition end base position of edit (end of intron)
   * @param inSel Vector of features used for the annotSet.
   * @param underCursor Vector of features used for the cursorSet.
   * @param inView Vector of features used for the viewSet.
   * @param inEvidence Vector of features used for the evidenceSet.
   *
   */
  public void setSelections(int basePosition,
                            int endBasePosition,
                            Vector underCursor,
                            Vector annotVect,
                            Vector resultVect,
                            Vector siteVect) {
    setSelections(underCursor,
                  annotVect,
                  resultVect,
                  siteVect);
    this.basePosition = basePosition;
    this.endBasePosition = endBasePosition;
  }

  /**
   * sets the four SelectionSets which are used in the edit methods, from the
   * four input Vectors. This setSelections should be called from all the
   * others.
   * <BR>
   * NOTE: It calls {@link #resetData()}.
   *
   * @param inSel Vector of features used for the annotSet.
   * @param underCursor Vector of features used for the cursorSet.
   * @param inView Vector of features used for the viewSet.
   * @param inEvidence Vector of features used for the evidenceSet.
   *
   */
  public void setSelections(Vector underCursor,
                            Vector annotVect,
                            Vector resultVect,
                            Vector siteVect) {
    resetData();

    cursorSet    = new SelectionSet(underCursor);
    annotSet     = new SelectionSet(annotVect);
    evidenceSet  = new SelectionSet(resultVect);
    siteSet      = new SelectionSet(siteVect);

    synchOldAnnots();
  }

  
  /** Old and new transcripts and annots are for calculating annot, transcript, and translation
   *  range changes after edits are done */
  void synchOldAnnots() {
    synchTranscripts(getSelectionSetTranscripts());
    FeatureList newAnnots = getNewTopAnnots();
    if (newAnnots == null) return;
    oldAnnots = newAnnots.cloneList();
  }

  private void synchTranscript(SeqFeatureI transcript) {
    FeatureList transList = new FeatureList(transcript,false);
    synchTranscripts(transList);
  }

  private void synchTranscripts(FeatureList transcripts) {
    if (transcripts == null) return;
    newTranscripts = transcripts;
    oldTranscripts = transcripts.cloneList();
  }

  FeatureList getSelectionSetTranscripts() {
    if (annotSet == null) return null; // test case ignores annot set
    FeatureList transcripts = annotSet.getTranscriptList();
    transcripts.addAll(cursorSet.getTranscriptList());
    return transcripts;
  }

  /** get top level annots like genes -- for now just get from selection set.
   *  presently no other source for top annots */
  FeatureList getNewTopAnnots() {
    if (annotSet == null)
      return null; // happens with test case, skips sel set - should it?
    FeatureList genes = annotSet.getGeneList();
    genes.addAll(cursorSet.getGeneList());
    return genes;
  }

  FeatureList getOldTranscripts() {
    return oldTranscripts;
  }

  FeatureList getNewTranscripts() {
    return newTranscripts;
  }

  /** get old top level annots like genes */
  FeatureList getOldTopAnnots() {
    return oldAnnots;
  }

  /**
   * Determine if the current selections are compatible
   * with performing a merge transcripts. This IS used by
   * mergeTranscripts.
   */
  public boolean mergeTranscriptsAllowed() {
    if (annotSet.getTranscripts().size() != 2) {
      return false;
    }
    // Check that transcripts are from annotations of matching types
    Transcript tran1 = (Transcript)annotSet.getTranscripts().elementAt(0);
    Transcript tran2 = (Transcript)annotSet.getTranscripts().elementAt(1);
    if (!(tran1.getGene().getTopLevelType().equals(tran2.getGene().getTopLevelType()))) {
//       System.out.println("Can't merge annotations of different types: " +
//                          tran1.getName() + " belongs to a " +
//                          tran1.getGene().getBioType() +
//                          " and " + tran2.getName() + " belongs to a " +
//                          tran2.getGene().getBioType());
      return false;
    }
    if(!goodOwner(tran1.getOwner()) || !goodOwner(tran2.getOwner()))
      return false;
      
    return true;
  }
  
  private boolean goodOwner(String owner) {
    if(Config.getCheckOwnership() && owner != null && !owner.equals(UserName.getUserName()))
      return false;
    return true;
  }

  /**
   * The default mergeTranscripts method. This merges a single transcript
   * selected from the cursorSet (under the cursor) and a single transcript
   * from in the annotSet (which usually comes from the current Selection).
   */
  public void mergeTranscripts() {
    if (mergeTranscriptsAllowed()) {
      Vector combinedTranscripts = new Vector();
      combinedTranscripts.addElement(annotSet.getTranscripts().elementAt(0));
      combinedTranscripts.addElement(annotSet.getTranscripts().elementAt(1));
      
      mergeTranscripts(combinedTranscripts);

    } else {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "To merge transcripts, you must select two transcripts on the same strand.");
    }
  }

  /**
   * merge a Vector of two Transcripts.
   */
  private void mergeTranscripts(Vector transcripts) {
    Transcript tran1 = (Transcript)transcripts.elementAt(0);
    Transcript tran2 = (Transcript)transcripts.elementAt(1);

    // if we merge transcripts then we have to check that any other
    // overlapping transcripts are also pulled into the same gene
    // NOTE: This is now done in consolidateGenes.
    logger.info("****** MERGING TRANSCRIPTS ******");

    /* moveExonsToTrans is the same functionality of merging trans so reuse it
       have to clone feature vector as the exons gets deleted from transcript
       as the the vector is iterated - but shouldnt there be 2 separate events?
       MERGE TRANSCRIPT, and MERGE EXONS? 
       also wont this cloning throw things like drawables? */
    //When saving into chado and if the user is merging a temp transcript with an existing transcript, 
    //get sure that the exons of the temp trans get moved to the existing and not the contrary
    if (isTemp(tran1) && ! isTemp(tran2)){
      tran2=(Transcript)transcripts.elementAt(0);
      tran1=(Transcript)transcripts.elementAt(1);
    }

    moveExonsToTranscript(tran1,
                          (Vector)tran2.getFeatures().clone(),
                          "merge transcripts");
  }

  /**
   * A debugging method to print out the four current selection sets.
   */
  public void printSets() {
    cursorSet     .dump("cursorSet:");
    annotSet    .dump("annotSet:");
    evidenceSet.dump("evidenceSet:");
    siteSet.dump("siteSet:");
  }

  /**
   * begin edit should be called at the start of all edits performed in the
   * AnnotationEditor. This creates a new Coalescer for a new series of edits.
   */
  private ChangeList beginEdit(String description) {
    //editDescription = new String(description); -- not used who cares
    //coalescer = new AnnotationChangeCoalescer(getController());
    //return coalescer;
    return createCoalescer();
  }

  private ChangeList createCoalescer() {
    return createChangeList();
  }

  private ChangeList createChangeList() {
    return new ChangeList(this,getController());
  }

  /**
   * Display a message. Error and warning messages are output both to
   * the terminal and in a dialog box if the parent component has been
   * set. Other messages are only output to the terminal.
   */
  protected void showMessage(int type, String message) {
    switch (type) {
    case JOptionPane.ERROR_MESSAGE:
      logger.error(message);
      showMessageWindow(type,message,"Editor error");
      break;
    case JOptionPane.WARNING_MESSAGE:
      logger.warn(message);
      showMessageWindow(type,message,"Editor warning");
      break;
    default:
      logger.error(message);
    }
  }

  /**
   * Display the message dialog window with the specified message string and
   * title.
   * <BR>
   * NOTE: This can only be done if parentComponent is set.
   */
  protected void showMessageWindow(int type, String message, String title) {
    if (parentComponent != null) {
      JOptionPane.showMessageDialog(parentComponent,message,title,type);
    }
  }

  /**
   * areOverlapping determines if two SeqFeatureIs overlap using the
   * currently defined gene definition (from Config.getGeneDefinition().
   * Obviously this method can be called with FeatureSets such as
   * Genes or Transcripts - it should handle such cases.
   Changed logic here. returns false if 2 feats are of different annot types
   as 2 different annot types will never "overlap"
   */
  protected boolean areOverlapping(SeqFeatureI sa, SeqFeatureI sb) {
    // this is a funny test - the biotype for a result may end up being the biotype of
    // the annot - but its not there yet, its still the result biotype and thus would
    // seem unequal here - need to get annot type of result somehow???
    // yup - fixed this with getAnnotTypeForFeature()
    if (!getAnnotTypeForFeature(sa).equals(getAnnotTypeForFeature(sb)))
      return false;

//     OverlapI checker = (sa.getTopLevelType().equals(sb.getTopLevelType()) ?
//                         Config.getOverlapper(sa) :
//                         SimpleOverlap.getSimpleOverlap());
    OverlapI checker = Config.getOverlapper(sa);
    return checker.areOverlapping (sa, sb);
  }

  /**
   * This method is called to identify Genes which because of the
   * edit have become merged. It generates the necessary AnnotationChanges
   * to merge the genes.
   */
  private CompoundTransaction consolidateGenes(AnnotatedFeatureI dest) {
    // Default is to ask before merging
    return consolidateGenes(dest, true);
  }

  /** Returns null if no merge is found. Returns compound transaction if merge is
   *  found. */
  private CompoundTransaction consolidateGenes(AnnotatedFeatureI destGene,
                                               boolean askBeforeMerging) {
    if (!(destGene.isAnnotTop())) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "consolidateGenes called with non-annotation " + destGene.getName());
      return null;
    }

    // If destGene isn't first-tier, don't bother asking about other existing
    // overlapping genes in the return--just return.
    if (!isFirstTierAnnot(destGene))
      return null;

    CompoundTransaction mergeTrans = null;

    Vector overlapped_genes = new Vector();
    overlapped_genes.addElement (destGene);
    // findMerges returns true if any genes overlap with destGene, and puts those overlapping
    // genes in overlapped_genes vector (may want to refactor for clarity)
    if (findMerges (destGene, overlapped_genes)) {
      mergeTrans = new CompoundTransaction(CompoundTransaction.MERGE,this);

      for (int i = 0; i < overlapped_genes.size(); i++) {
        AnnotatedFeatureI g
          = (AnnotatedFeatureI) overlapped_genes.elementAt(i);
        if (g != destGene) { // info msg to stdout
          logger.info (g.getName() + " at " +
                       g.getStart() + "-" + g.getEnd() +
                       " is a " + g.getTopLevelType() + " and overlaps " +
                       destGene.getTopLevelType() + " " +
                       destGene.getName() + " at " +
                       destGene.getStart() + "-" +
                       destGene.getEnd());

          if (askBeforeMerging && !(destGene.getName().equals(g.getName()))) {
            boolean answer = askMergeDialog(destGene.getName() + " and " + g.getName());
            if (!answer)
              continue;
          }

          int size = g.size();
          for (int j=0; j < size; j++) {
            // Note that as features are deleted the feature at 0 will change
            Transcript t = (Transcript)g.getFeatureAt(0);
            CompoundTransaction ct = purgeTranscript(g, t);
            mergeTrans.addTransaction(ct);
            AddTransaction at = addTranscript(destGene, t); // changer
            mergeTrans.addTransaction(at);
          }
          // this gene has been deleted, salvage its name(s)
          // actually this asks user for which name - should do this before above and
          // if user wants the overlapping gene (g) as name then then g should imbibe
          // dest gene rather than vice versa - should do this in mvExToTrans as well
          // also this is rather crude if more than one overlapping user will be asked
          // naming issue more than once (how do you get more than one? not easy/common)
          CompoundTransaction ct = mergeGeneIdentifiers(destGene,g);
          mergeTrans.addTransaction(ct);
        }
      }
    }
    if (mergeTrans == null || !mergeTrans.hasKids())
      return null;
    return mergeTrans;
  }

  /** Returns true if user said to merge */
  private boolean askMergeDialog(String geneNames) {
    // There are overlapping genes in this region--ask user what to do
    Object[] options = { "Merge",
                         "Don't merge" };

    String message = "There are overlapping genes in this region: " + geneNames + ".\nDo you want to merge these overlapping genes into a single gene?";
    logger.info(message);
    JOptionPane pane =
      new JOptionPane(message,
                      JOptionPane.DEFAULT_OPTION,
                      JOptionPane.OK_CANCEL_OPTION,
                      null, // icon
                      options, // all choices
                      options[0]); // initial value
    JDialog dialog = pane.createDialog(null,
                                       "Please Confirm");
    dialog.show();

    Object result = pane.getValue();
    if (result != null && ((String)result).equals("Merge")) {
      logger.info("Merging genes...");
      return true;
    }
    else
      return false;
  }

  /**
   * Sets the start of translation to the specified position and sets the
   * end of translation based on the start. called by BaseEditorPanel start translation
   * menu item. does not use SelectionSets (calls setSelection with all nulls)
   */
  public void setTranslationStart(ExonI exon, int pos) {

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    SeqFeatureI topLevelFeat = getTopLevelFeat(exon);
    if (!topLevelFeats.containsKey(topLevelFeat.hashCode())) {
      generateFeatChangeData(topLevelFeats, topLevelFeat);
    }
    
    Transcript trans = (Transcript)exon.getRefFeature();
    /* Invalidate peptide sequence because it will need to be recalculated
       if anyone asks for it later.  (Don't bother recaculating right now--
       might be a waste of time.)
       In case the pos is outside the transcript, check to make sure
       setting occured. Use flag to indicate that end should be set
       as well.
    */
    // I am puzzled by this if.  I tried changing the translation start, and
    // since trans.setTranslationStart often returned false, this "if" didn't
    // get executed--and yet the translation start did move--don't we want to
    // record that transaction??  --NH
    // nomi - whats an example of this? - MG
    //int oldTranslationStart = trans.getTranslationStart();
    RangeI oldRange = trans.getTranslationRange();
    if (trans.setTranslationStart(pos, true)) {
      //AnnotationChangeCoalescer changer = beginEdit("set translation start");
      trans.setMissing5prime(false);
      trans.setPeptideValidity(false);
      trans.setOwner(getUserName());
      trans.replaceProperty ("date", (new Date()).toString());

      Transaction t = updatePeptideRange(trans,oldRange);
      //fireTransaction(t); // this might cause ChangeList to recreate same trans
      //getController().handleAnnotationChangeEvent(t.generateAnnotationChangeEvent());
      generateEditTransaction(topLevelFeats);
    }
  }

  /** actually its possible that a protein doesnt yet exist (in the database)
      to update. in that case this needs to be an insert. hasName is being
      used to check if the peptide is pre-existing or new - i think this will
      do the trick (if new its NO_NAME) - if it doesnt we can get more elaborate
      Transcript.getPepSeq creates seq with no name if there isnt one
      this needs to happen for all peptide updates (name & id) */
  Transaction updatePeptideRange(SeqFeatureI trans,RangeI oldRange) {
    Transaction transaction=null;
    
    // PEPTIDE EXISTS - UPDATE
    if (trans.hasPeptideSequence() && trans.getPeptideSequence().hasName()
        && trans.getPeptideSequence().getAccessionNo() != null) {
      //AnnotationUpdateEvent aue = 
      //new AnnotationUpdateEvent(this,trans,TransactionSubpart.PEPTIDE_LIMITS);
      //aue.setOldRange(oldRange); // undo
      TransactionSubpart ts = TransactionSubpart.PEPTIDE_LIMITS;
      transaction = new UpdateTransaction(this,trans,ts,oldRange,trans);
      
      // AnnotationChangeCoalescer does auto figuring and firing of translation range
      // changes - but this would require selection set being set which is bypassed
      //fireSingleChange(aue);
    }

    // PEPTIDE IS NEW - INSERT
    // the alternative is to have this figured out in the data adapter itself
    // just let the update go through, and the dataadapter could turn it into
    // an insert
    else {
      trans.getPeptideSequence().setName("temp");
      trans.getPeptideSequence().setAccessionNo("temp");
      boolean isAddPeptide = true;
      transaction = new AddTransaction(this,trans,isAddPeptide);
    }
    return transaction;
  }

  /**
   * Sets the start of translation to the specified position and sets the
   * end of translation based on the start.
   */
  public void setFrameShiftPosition(ExonI exon, int pos, boolean plus1) {
    Transcript trans = (Transcript)exon.getRefFeature();
    //setOldTranscriptAndGene(trans); // for ACC to figure translation change
    /* Invalidate peptide sequence because it will need to be recalculated
       if anyone asks for it later.  (Don't bother recaculating right now--
       might be a waste of time.)
       In case the pos is outside the transcript, check to make sure
       setting occured. Use flag to indicate that end should be set
       as well.
    */
    
    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    SeqFeatureI topLevelFeat = getTopLevelFeat(trans);
    generateFeatChangeData(topLevelFeats, topLevelFeat);
    
    boolean okay = false;
    int oldPos; // undo
    TransactionSubpart subpart;
    synchTranscript(trans); // for figuring translation changes in ACC
    if (plus1) {
      oldPos = trans.plus1FrameShiftPosition();
      okay = trans.setPlus1FrameShiftPosition(pos); // alters model, translation
      subpart = TransactionSubpart.PLUS_1_FRAMESHIFT;
    }
    else {
      oldPos = trans.minus1FrameShiftPosition();
      okay = trans.setMinus1FrameShiftPosition(pos);
      subpart = TransactionSubpart.MINUS_1_FRAMESHIFT;
    }

    if (okay) {
      //AnnotationChangeCoalescer changer = beginEdit("set frame shift");
      trans.setPeptideValidity(false);
      trans.setOwner(getUserName());
      trans.replaceProperty ("date", (new Date()).toString());
      //endEdit(changer, true, trans.getGene(), FeatureChangeEvent.REDRAW);
      AnnotationUpdateEvent aue = new AnnotationUpdateEvent(this,trans,subpart);
      aue.setOldInt(oldPos);
      // Because we're not calling handleAnnotationChangeEvent, the
      // transaction doesn't get recorded by AnnotationChangeLog unless
      // we explicitly ask it to.
      //AnnotationChangeLog.getAnnotationChangeLog().addTransaction(ace.getTransaction());
      //fireSingleChange(aue);
      
      generateEditTransaction(topLevelFeats);
    }
  }

  /** This will handle firing AnnotationChangeEvent if only one in the series.
      Since theres only one no need to use AnnotationChangeCoalescer.
      Fires the event and then fires a MODEL_CHANGED event signifying edits series is
      over. */
  private void fireSingleChange(AnnotationChangeEvent ace) {
    ace.setSingularEventState(true); // says this is the only event coming (->redraw)
    ChangeList acc = createCoalescer();
    acc.addChange(ace);
    acc.executeChanges(); // checks for gene,transcript,and translation changes
    //annotCngListener.handleAnnotationChangeEvent(ace);
    //fireEditSessionDoneEvent(); // ??
  }

  private void fireCompoundTransaction(CompoundTransaction ct) {
    if (ct == null)
      return;
    // if (!ct.hasKids()) return; // ????
    ct.setSource(this);
    fireSingleChange(ct.generateAnnotationChangeEvent());
  }

  private void fireTransaction(Transaction t) {
    if (t == null) return;
    t.setSource(this); // just in case
    fireSingleChange(t.generateAnnotationChangeEvent());
  }

//   /** This is for single edits bypassing the coalescer - no need to coalesce 
//       single edits */
//   private void fireEditSessionDoneEvent() {
//     //AnnotationChangeEvent done = AnnotationChangeEvent.editSessionDoneEvent;
//     //AnnotationChangeEvent done = new AnnotationChangeEvent(this);
//     AnnotSessionDoneEvent done = new AnnotSessionDoneEvent(this);
//     annotCngListener.handleAnnotationChangeEvent(done);
//   }


  /**
   * Sets the start of translation to the specified position and sets the
   * end of translation based on the start. Moved modifying model functionality from 
   BaseEditorPanel to here - where it belongs.
   Do for a feature list of leaf annots - either exons or 1 level annots.
   For deleting the seq error operation is SequenceI.CLEAR_EDIT
   annots is a list of AnnotatedFeatures that contain position, if transcript then
   will recalc translation
   residue is the residue changed to (null for deletions)
   */
  public void setSequencingErrorPositions(SequenceI seq, String operation, int pos,
                                          String residue, FeatureList annots) {


    Transaction transaction;
    SequenceEdit seqEdit;
    boolean editSuccessful = false;

    // DELETE
    if (operation == SequenceI.CLEAR_EDIT) {
      // need seqEdit with id/uniquename for delete back to db
      if (!seq.isSequencingErrorPosition(pos)) {
        logger.error("no seq err to remove at pos "+pos);
        return;
      }
      seqEdit = seq.getSequencingErrorAtPosition(pos);
      //seqEdits have no parent feature - make up a dummy parent for cur set?
      // there should be a cur set feat anyways...
      SeqFeatureI dummyParent = new SeqFeature(); // will this fly?
      transaction = new DeleteTransaction(seqEdit, this);
      // transaction.editModel(); ???
      editSuccessful = seq.removeSequenceEdit(seqEdit);
    }
    // INSERT
    else {
     seqEdit = new SequenceEdit(operation,pos,residue);
     seqEdit.setRefSequence(seq);
     ApolloNameAdapterI na = getNameAdapter(seqEdit); // should get default NA
      seqEdit.setId(na.generateId(curation.getAnnots(),curation.getName(),seqEdit));
      // a seq edit is a seq feat??? thats how chado does it...
      // its really a one level annotation under the covers
      if(annots.first().isExon())
      	seqEdit.addRefFeature("exon",annots.first());
      AddTransaction at = new AddTransaction(this,seqEdit); // ?????
      at.setResidues(seqEdit.getResidue());
      transaction = at;
      // at.editModel ???
      // this is the editing of the model, todo: move to trans.editModel
      // this currently does delete if operation is CLEAR_EDIT...
      editSuccessful = seq.addSequenceEdit(seqEdit);
     }
    
    // If the sequencing error failed to take than nothing to do
    if (!editSuccessful) { // err msg?
      logger.error("Seq error edit failed at "+pos);
      return;
    }
  
    // redo translations of affected transcripts
    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();    
    int annot_count = annots.size();
    for (int i = 0; i < annot_count; i++) {
      SeqFeatureI annot = annots.getFeature(i);
      if (!annot.isAnnot()) { // shouldnt happen
        logger.error("non-annot in getSeqErrPosition");
        continue;
      }
      SeqFeatureI topLevelFeat = getTopLevelFeat(annot);
      generateFeatChangeData(topLevelFeats, topLevelFeat);
      //setSelections(null, null, null, null); // is this needed?
      redoTranscriptTranslation(annot.getAnnotatedFeature());
    }
    //fireTransaction(transaction); // i think this needs to happed after translation corrections?
    
    CompoundTransaction ct = new CompoundTransaction(this);
    ct.addTransaction(transaction);
    generateEditTransaction(topLevelFeats, ct);
  }

  /** Set sequence error position for an exon. shouldnt the seq error transaction be
   * on the sequence and not a feature? i know that capability is not presently there
   * but we may want that for this case, its not the feat that gets the error 
   changed this, for seq error just need to go through transcripts and reset translation
*/
  private void redoTranscriptTranslation(AnnotatedFeatureI annot) {
    if (annot.isExon() && annot.getRefFeature().isTranscript()) {
      SeqFeatureI trans = annot.getRefFeature();
      /* Invalidate peptide sequence because it will need to be recalculated
      if anyone asks for it later.  (Don't bother recaculating right now--
      might be a waste of time.)
      In case the pos is outside the transcript, check to make sure
      setting occured. Use flag to indicate that end should be set
      as well. */
      // But shouldnt start be checked as well???
      synchTranscript(trans); // records old translation to catch changes in translation
      TranslationI translation = trans.getTranslation();
      logger.info("old stop "+translation.getTranslationEnd());
      translation.setTranslationEndFromStart();
      logger.info("new stop "+translation.getTranslationEnd());
      translation.setPeptideValidity(false); // Transcript method
      trans.getAnnotatedFeature().setOwner(getUserName());
      trans.replaceProperty ("date", (new Date()).toString());
    }
  }


  /**
   * Sets the start of translation to the specified position and sets the
   * end of translation based on the start. what specified position??? just sets
   * start at first start codon.
   I think this can reset stop as well - side effect of start - and should send out
   event for stop as well - or should it just be a range event! translation range??
   */
  public void setLongestORF() {
    /* need to call getTranscripts, because simply calling getFeatures
       may not necessarily return a vector solely of transcripts
       and this in turn will screw up the casting */
    Vector trans_vect = annotSet.getTranscripts();
    int trans_count = trans_vect.size();
    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    for (int i = 0; i < trans_count; i++) {
      //AnnotationChangeCoalescer changer = beginEdit("set longest ORF");
      //FeatureSetI trans;
      Transcript trans;
      ChangeList changer =  beginEdit("Set longest ORF");
      try {
        trans = (Transcript) trans_vect.elementAt (i);
        SeqFeatureI topLevelFeat = getTopLevelFeat(trans);
        generateFeatChangeData(topLevelFeats, topLevelFeat);
        ((Transcript) trans).setPeptideValidity(false);
        ((Transcript) trans).setOwner(getUserName());
      } catch (Exception e) { // not possible w getTranscripts above
        FeatureSetI fs = (FeatureSetI) trans_vect.elementAt(i);
        logger.warn (fs.getName() + " is not a Transcript " +
                     " it is a " + fs.getClass().getName(), e);
        continue;
      }
      /* Invalidate peptide sequence because it will need to be
         recalculated if anyone asks for it later.  (Don't bother
         recaculating right now--might be a waste of time.) */
//      int oldStart = trans.getTranslationStart();
      // this might change stop as well - should test for and send out event
      // should translation event be a range
      trans.calcTranslationStartForLongestPeptide();
//       int newStart = trans.getTranslationStart();
//       if (oldStart == newStart) // if nothing changed dont fire event
//         continue;
      trans.replaceProperty ("date", (new Date()).toString());
      endEdit(changer, true, trans.getRefFeature(), FeatureChangeEvent.REDRAW);
//       AnnotationUpdateEvent aue = 
//         new AnnotationUpdateEvent(this,trans,TransactionSubpart.TRANSLATION_START);
//       aue.setOldInt(oldStart);
//       fireSingleChange(aue);
      //endEdit(changer, true, trans, FeatureChangeEvent.REDRAW);
    }
    // will automatically detect all translation changes
    createCoalescer().executeChanges();
    //create transactions
    generateEditTransaction(topLevelFeats);
  }
  
  public boolean setTranslationTerminusAllowed() {
    if (cursorSet.getLeafFeatures().size() != 1 ||
        siteSet.getLeafFeatures().size() != 1) {
      return false;
    }
    return true;
  }

  /**
   * The default setTranslationTerminus method. This expects the selected
   * feature to be an DrawableTerminalCodon and the cursor to be over a
   * transcript.
   (gui note - caused by dragging codons onto transcript)
   */
  public void setTranslationTerminus() {
    if (cursorSet.getLeafFeatures().size() != 1 ||
        siteSet.getLeafFeatures().size() != 1) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Must select exactly one terminus and one exon");
      return;
    }
    if (!(cursorSet.getLeafFeat(0) instanceof ExonI) ||
        (siteSet.getLeafFeat(0).getFeatureType().indexOf("codon") < 0)) {

      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Wrong types in setTranslationTerminus");
      return;
    }

    setTranslationTerminus((ExonI)cursorSet.getLeafFeat(0),
                           siteSet.getLeafFeat(0));

  }

  /**
   * Sets either the start or end of translation depending on the type
   * of the passed DrawableTerminalCodon. <BR>
   * NOTE: Setting the stop doesn't update the start, but setting the
   *       start does update the stop.
   */
  private void setTranslationTerminus(ExonI exon,
                                     SeqFeatureI codon) {

    if (!exon.contains(codon)) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Exon doesn't contain terminal codon range.");
      return;
    }
    if (!(exon.getRefFeature() instanceof Transcript)) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Exon parent not Transcript");
      return;
    }

    Transcript trans = (Transcript)exon.getRefFeature();

    //    AnnotationUpdateEvent updateEvt=null;
    if (codon.getFeatureType().startsWith("startcodon")) {
//      int oldSite = trans.getTranslationStart();
//       if (oldSite == codon.getStart())
//         return; // new and old same
      // this will probably effect stop as well - need event for that
      trans.setTranslationStart(codon.getStart(), true);
      trans.setMissing5prime(false);
//       TransactionSubpart subpart = TransactionSubpart.TRANSLATION_START;
//       updateEvt = new AnnotationUpdateEvent(this,trans,subpart);
//       updateEvt.setOldInt(oldSite);
    } 
    else if (codon.getFeatureType().startsWith("stopcodon")) {
      // NOTE: trans.getStrand moves to the correct stop position,
      // because the translation range doesn't include the stop codon.
      // 8/2005: What was this - trans-getStrand() for?  It was making the
      // stop codon come down at the wrong place.
      //      int stopPos = codon.getStart() - trans.getStrand();
      int stopPos = codon.getStart();  
      if (isRangeOK(trans,trans.getTranslationStart(),stopPos)) {
//         int oldSite = trans.getTranslationEnd();
//         if (oldSite == stopPos)
//           return; // not a real update
        trans.setTranslationEnd (stopPos);
        trans.setMissing3prime(false);
//         TransactionSubpart subpart = TransactionSubpart.TRANSLATION_STOP;
//         updateEvt = new AnnotationUpdateEvent(this,trans,subpart);
//         updateEvt.setOldInt(oldSite);
      } else {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "Can't set stop before the start");
        return;
      }
    }
    /* Invalidate peptide sequence because it will need to be recalculated
       if anyone asks for it later.  (Don't bother recaculating right now--
       might be a waste of time.) */
    trans.setPeptideValidity(false);

    trans.setOwner(getUserName());
    trans.replaceProperty ("date", (new Date()).toString());

    //fireSingleChange(updateEvt);
    // will automatically detect all translation changes
    createCoalescer().executeChanges();
  }

  /**
   * The default splice site setting function. This expects a single
   * DrawableSpliceSite to have been selected and the cursor to be
   * over an exon.
   This is currently unused.
   */
  public void setSpliceSite() {
    if (cursorSet.getLeafFeatures().size() != 1 ||
        evidenceSet.getLeafFeatures().size() != 1) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Must select exactly one splice site and one exon");
      return;
    }
    if (!(cursorSet.getLeafFeat(0) instanceof ExonI) ||
        (evidenceSet.getLeafFeat(0).getFeatureType().indexOf("splice") < 0)) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Wrong types in setSpliceSite");
      return;
    }

    setSpliceSite((ExonI)cursorSet.getLeafFeat(0),
                  (SeqFeatureI)evidenceSet.getLeafFeat(0));
  }

  /**
   * Sets either a donor or acceptor splice site depending on the type of the
   * DrawableSpliceSite passed.
   * <BR>
   * TODO: Check new limits are right
   * (getEnd for site might not be site boundary).
   This is currently unused so i just commented out pase annotation change event
   Once splice site editor is up and working will revisit with new ACE.
   */
  private void setSpliceSite(ExonI exon, SeqFeatureI site) {
    Transcript             trans;
    if (exon.getRefFeature() instanceof Transcript) {
      trans = (Transcript)exon.getRefFeature();
    } else {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Exon with non Transcript parent");
      return;
    }

    int exIndex = trans.getFeatureIndex(exon);

    String site_type = site.getFeatureType();

    if (!site_type.equals ("donor") &&
        !site_type.equals ("acceptor"))
      site_type = site.getRefFeature().getProperty ("splice");

    if (site_type.equals("donor")) {
      if (exIndex == trans.size() - 1) {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "Can't set donor site on last exon");
        return;
      }
      if (isExonOverlap(trans.getGene(),
                        exon,
                        exon.getStart(),
                        site.getEnd())) {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "Can't set donor site because it would cause overlap");
        return;
      }
      if (!isRangeOK(exon,
                     exon.getStart(),
                     site.getEnd())) {
        return;
      }
      exon.setEnd(site.getEnd());

    } else if (site_type.equals("acceptor")) {
      if (exIndex == 0) {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "Can't set acceptor site on first exon");
        return;
      }
      if (isExonOverlap(trans.getGene(),
                        exon,
                        site.getStart(),
                        exon.getEnd())) {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "Can't set acceptor site because it would cause overlap");
        return;
      }
      if (!isRangeOK(exon,
                     site.getStart(),
                     exon.getEnd())) {
        return;
      }
      exon.setStart(site.getEnd());
    } else {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Can't set splice site of type " + site.getFeatureType());
      return;
    }

    setGeneEnds(trans);

    ChangeList changer = beginEdit("set splice site");

    // this AnnotationChangeEvent is not right - commenting out - need to put
    // in new one once this method becomes relevant again
//     AnnotationChangeEvent ace
//     = new AnnotationChangeEvent(this,
//                                 trans.getRefFeature(),
//                                 FeatureChangeEvent.LIMITS,
//                                 AnnotationChangeEvent.EXON,
//                                 trans,
//                                 exon);
//     ace.getTransaction().addProperty("transcript_name",
//                                      trans.getName(),
//                                      Transaction.OLD);
//     ace.getTransaction().addProperty("annotation_id",
//                                      trans.getGene().getId(),
//                                      Transaction.OLD);
//     ace.getTransaction().addProperty("transcript_name",
//                                      trans.getName(),
//                                      Transaction.NEW);
//     ace.getTransaction().addProperty("annotation_id",
//                                      trans.getGene().getId(),
//                                      Transaction.NEW);
//     changer.addChange(ace);
    endEdit(changer, true, trans.getGene(), FeatureChangeEvent.REDRAW);
  }

  /**
   * Checks whether a specified range is OK. OK in this context is
   * that start > end if strand is + OR
   *      start < end if strand is -.
   */
  protected boolean isRangeOK(SeqFeatureI sf, int start, int end) {
    if ((sf.getStrand() ==  1 && start > end) ||
        (sf.getStrand() == -1 && end > start)) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Range is not okay - ignoring");
      return false;
    }
    if (sf.getStrand() == 0) {
      showMessage(JOptionPane.WARNING_MESSAGE,
                  "0 stranded feature");
    }
    return true;
  }

  /**
   * Check all Transcripts in a Gene to see if a change to an Exon boundary
   * would cause an overlap in any of them.
   */
  private boolean isExonOverlap(AnnotatedFeatureI gene,
                                  AnnotatedFeatureI exon,
                                  int start,
                                  int end) {
    int trans_count = gene.size();
    for (int i=0; i < trans_count; i++) {
      if (isExonOverlapInTrans(gene.getFeatureAt(i),exon,start,end)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check whether the new boundaries for an Exon overlap
   * the other exons in the transcript.
   * THIS SHOULD USE THE SEQFEATURE METHODS!!!
   */
  private boolean isExonOverlapInTrans(SeqFeatureI trans,
                                  AnnotatedFeatureI exon,
                                  int start,
                                  int end) {

    int exIndex = trans.getFeatureIndex(exon);

    if (exIndex != -1) {
      
      // FORWARD STRAND
      if (trans.getStrand() == 1) {
        if (exIndex != trans.size()-1 &&
            trans.getFeatureAt(exIndex+1).getStart() <= end) {
          return true;
        }
        if (exIndex != 0 &&
            trans.getFeatureAt(exIndex-1).getEnd() >= start) {
          return true;
        }
      } 

      // REVERSE STRAND
      else if (trans.getStrand() == -1) {
        if (exIndex != trans.size()-1 &&
            trans.getFeatureAt(exIndex+1).getStart() >= end) {
          return true;
        }
        if (exIndex != 0 &&
            trans.getFeatureAt(exIndex-1).getEnd() <= start) {
          return true;
        }
      }
      
      // NO STRAND
      else {
        showMessage(JOptionPane.ERROR_MESSAGE,"Unstranded transcript");
        return false;
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * Determine if there is anything at all selected to move to the
   * other strand
   * <BR>
   */
  public boolean resultIsSelected () {
    // not sure if this is the correct test, but it'll do for now
    return (evidenceSet.getLeafFeatures().size() > 0);
  }

  /** Should this be a util function? boolean for a popup?*/
  private void errorMsgWithStackTrace(String errMsg,boolean popup) {
    errMsg="Programmer error: "+errMsg;
    if (popup)
      JOptionPane.showMessageDialog(null,errMsg);
    logger.error(errMsg, new Throwable());
  }

  /**
   * Move a result to other strand */
  public void flipResult() {
    if (!resultIsSelected())
      return;  // Nothing to do

    /* getSets gives the evidence features as FeatureSets
       (parents of non FSets) */
    Vector features = evidenceSet.getSets();

    //ApolloFrame apollo_frame=SwingMissingUtil.getWindowAncestor(getParentComponent());
    StrandedZoomableApolloPanel szap = getCurationState().getSZAP();
    TierViewI resultView = null;

    for (int i = 0; i < features.size(); i++) {
      FeatureSetI feature = (FeatureSetI) features.elementAt(i);
      FeatureSetI parent = (FeatureSetI) feature.getRefFeature();
      parent.deleteFeature (feature); // removes data model
      ResultChangeEvent rm_evt
        = new ResultChangeEvent(this,
                                parent,
                                FeatureChangeEvent.DELETE,
                                ResultChangeEvent.RESULTSET,
                                parent,
                                feature);
      getController().handleResultChangeEvent(rm_evt); // removes from view

      resultView = szap.getViewForFeature(feature);

      feature.flipFlop(); // changes strand

      SeqFeatureI newParent = parent.getAnalogousOppositeStrandFeature();
      // if parent does not have an analogous opposite strand feature, there
      // is no way we can do the flip. This is a programmer error. The
      // data adapter needs to set this (see GAMEAdapter.addAnalysis)
      // no longer true - just create new op strand fs
      if (!parent.hasAnalogousOppositeStrandFeature()) { // newParent==null
        newParent = createOppositeStrandAnalysisFeature(parent); // workinprogress
//         String m = "You have hit a known apollo bug. You can not presently move\n"
//           +"features to a strand that doesnt have any features of that type, in\n"
//           +"other words features of this type exist on one strand but not the other.\n"
//           +"Currently we deem this bug low priority and a rare case. Please email\n"
//           +"apollo@fruitfly.org to have this bug fixed.\n";
//         errorMsgWithStackTrace(m,true);
//         return;
      }

      newParent.addFeature (feature, true);

      ResultChangeEvent add_evt
        = new ResultChangeEvent(this,
                                newParent,
                                FeatureChangeEvent.ADD,
                                ResultChangeEvent.RESULTSET,
                                newParent,
                                feature);
      getController().handleResultChangeEvent(add_evt);
    }

    // Redraw both strands [this code added by NH, adapted from LoadUtil]
    FeatureSetI top_forward = szap.getForwardResults();
    FeatureSetI top_reverse = szap.getReverseResults();
    ResultChangeEvent redraw_evt;
    redraw_evt = new ResultChangeEvent(this,
                                       top_forward,
                                       FeatureChangeEvent.REDRAW,
                                       ResultChangeEvent.RESULTSET,
                                       null,
                                       null);
    getController().handleResultChangeEvent(redraw_evt);
    redraw_evt = new ResultChangeEvent(this,
                                       top_reverse,
                                       FeatureChangeEvent.REDRAW,
                                       ResultChangeEvent.RESULTSET,
                                       null,
                                       null);
    getController().handleResultChangeEvent(redraw_evt);

    if (resultView != null)
      selectFeatures(features, resultView);
  }

  /** Creates analysis feature for opposite strand of analysis feat
   For now creates name of analysis in GAME style "program:db-strand" - should probably
  name adapter this at some point - but probably not really critical
  This doesnt work yet but its getting close - need to revisit. Need to tell
  view/drawables that it needs to create a whole new tier - i dont think
  theres presently a way to do that.
  */
  private SeqFeatureI createOppositeStrandAnalysisFeature(SeqFeatureI analysis) {
    SeqFeatureI oppositeAnalysis = analysis.cloneFeature();
    // the coning will grab all the kids which we dont want - drop em
    oppositeAnalysis.clearKids();

    int strand = analysis.getStrand() * -1;
    oppositeAnalysis.setStrand(strand);
    // Yikes - this is data/name adapter specific isnt it?
    // cant retrieve a name adapter for a non-annot - shouldnt we be able to?
    //ApolloNameAdapterI na = Config.getNameAdapter(analysis);
    // for now just gonna do name like game does it (GAMEAdapter.initAnalysis) -
    //should revisit this - on the other hand the name of the analysis is
    //probably not that important
    boolean isForward = oppositeAnalysis.isForwardStrand();
    String n = analysis.getProgramName()+":"+analysis.getDatabase();
    n += isForward ? "-plus" : "-minus";
    oppositeAnalysis.setName(n);
    // add to data model - ref ref feat is result stranded feature set
    // this doesnt work - view/drawables doesnt know the results stranded feat set
    // this is the StrandedFeatureSet - 
    StrandedFeatureSetI resultsBothStrands = analysis.getStrandedFeatSetAncestor();
    // parent is the result FeatureSet for that strand
    SeqFeatureI strandParent = resultsBothStrands.getFeatSetForStrand(strand);
    strandParent.addFeature(oppositeAnalysis);
    // set analogous feats
    analysis.setAnalogousOppositeStrandFeature(oppositeAnalysis);
    oppositeAnalysis.setAnalogousOppositeStrandFeature(analysis);
    // this causes exception to fly as view doesnt know results model - FIX
    ResultChangeEvent add_evt
      = new ResultChangeEvent(this,
                              strandParent,
                              FeatureChangeEvent.ADD,
                              ResultChangeEvent.RESULTSET,
                              strandParent,
                              oppositeAnalysis);
    getController().handleResultChangeEvent(add_evt);
    return oppositeAnalysis;
  }


  /**
   * Determine if the current selection sets are compatible with the
   * the default adding evidence to exons method.
   * <BR>
   * NOTE: addEvidenceExons() does not actually call this.
   */
  public boolean addEvidenceExonsAllowed() {
    return goodEvidence (cursorSet.getTranscripts(), evidenceSet.getLeafFeatures());
  }

  /** @returns true if have only 1 transcript in trans vector and at least one
      feature and one of the features overlaps with the transcript */
  protected boolean goodEvidence (Vector transcripts, Vector features) {
    boolean allowed = false;
    if (transcripts.size() == 1 && features.size() > 0) {
      Transcript trans = (Transcript) transcripts.elementAt(0);
      for (int i = 0; i < features.size() && !allowed; i++) {
        SeqFeatureI possible_evidence
        = (SeqFeatureI) features.elementAt(i);
        if (possible_evidence.getId() != null) {
          for (int j = 0; j < trans.size() && !allowed; j++) {
            ExonI exon = trans.getExonAt(j);
            allowed = possible_evidence.overlaps(exon);
          }
        }
      }
    }
    return allowed;
  }

  /*
    If any one of the seqfeatures doesn't overlap with any of the
    existing exons, then it is okay to add it as a new exon
  */
  protected boolean validNewExons (Vector transcripts, Vector features) {
    boolean allowed = false;
    if (transcripts.size() == 1 && features.size() > 0) {
      Transcript trans = (Transcript) transcripts.elementAt(0);
      for (int i = 0; i < features.size() && !allowed; i++) {
        SeqFeatureI possible_evidence
        = (SeqFeatureI) features.elementAt(i);

        if (possible_evidence.getId() != null) {
          allowed = true;
          for (int j = 0;
               j < trans.getExons().size() && allowed;
               j++) {
            ExonI exon = trans.getExonAt(j);
            allowed &= ! possible_evidence.overlaps(exon);
          }
        }
      }
    }
    return allowed;
  }

  /**
   * The default method to add evidence to exons. The cursorSet is used to determine
   * the transcript to add evidence to. The evidenceSet is used to determine
   * the features to use as evidence.
   */
  public void addEvidenceExons() {
    switch (cursorSet.getTranscripts().size()) {
    case 0:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected NO transcript to add exon evidence to");
      break;
    case 1:
      addEvidenceExons(cursorSet.getTranscript(0),
                       evidenceSet.getLeafFeatures(),
                       EvidenceConstants.SIMILARITY);
      break;
    default:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected more than 1 transcript to add exon evidence to");
      break;
    }
  }

  /**
   * Add evidence to exons in a transcript.
   * @param trans    The transcript to add evidence to the exons of.
   * @param features A Vector of the features to add as evidence.
   * This just begins and ends the edit. The work is done by
   * {@link #addEvidenceExons(Transcript,Vector,AnnotationChangeCoalescer,int)}.
   */
  private void addEvidenceExons(Transcript trans,
                               Vector features,
                               int evidenceType) {

    ChangeList changer = beginEdit("add evidence exons");

    addEvidenceExons(trans,features,changer,evidenceType);

    endEdit(changer, true, trans.getGene(), FeatureChangeEvent.REDRAW);
  }

  /**
   * This is the version of addEvidenceExons which actually does the work.
   * It takes a Vector of SeqFeatures and tries to find overlapping exons in the
   * specified transcript. If an overlap is found then the SeqFeature is added
   * as evidence. If not an error is given.  If a single feature is added to
   * multiple exons a warning is given.
   * <BR>
   * NOTE: This method is called by several of the other edits to add evidence to
   *       exons.
   is this still used? - or has it been made irrelevant by showing matching edges.
   called by the other addEvidenceExons,addExons,addTranscript,addGene,setExonTerminus
   evidence is added to the datamodel but i dont think any of the dataadapters use it
   GAME used to but no longer

   I have suppressed the firing of the evidence events. This is to simplify matters
   for annot change events/transactions. 1st of all noone actually 
   listens for these events (theyre just avoided), secondly evidence is presently
   irrelevant having been replaced by edge matching no data adapter uses them.
   Should the whole evidence apparatus be ripped out?
   */
  private void addEvidenceExons(Transcript trans,
                                  Vector features,
                                  ChangeList changer,
                                  int evidenceType) {
    SeqFeatureI newEvidence = null;
    int         nadded = 0;
    // Really we should be prohibiting creation/modification of annots from collapsed tiers,
    // not just morning about it.
    //    boolean warnedAboutCollapsedTier = false;

    for (int i = 0; i < features.size(); i++) {
      newEvidence = (SeqFeatureI) features.elementAt(i);

      if (newEvidence.getId() != null) {
        boolean added = false;
        for (int j = 0; j < trans.size(); j++) {
          ExonI exon = trans.getExonAt(j);
          if (newEvidence.overlaps(exon)) {
            // Evidence is not used anymore anyway.  If we need to show any
            // message about the fact that the user has (perhaps unknowingly)
            // dragged in multiple exons, it shouldn't use the word "evidence".
//             if (added) {
//               showMessage(JOptionPane.WARNING_MESSAGE,
//                           "Evidence " + newEvidence.getId()  +
//                           " added to multiple exons");
//             }
            exon.addEvidence(newEvidence.getId(), evidenceType);
            added = true;
            nadded++;
          }
        }
        if (!added) {
          showMessage(JOptionPane.ERROR_MESSAGE,
                      "Tried to add non overlapping evidence");
        }
      }
    }

    // suuppressed event firing - see javadoc above
//     if (nadded > 0) {
//       AnnotationChangeEvent ace
//       = new AnnotationChangeEvent(this,
//                                   trans.getRefFeature(),
//                                   FeatureChangeEvent.ADD,
//                                   AnnotationChangeEvent.EVIDENCE,
//                                   trans,
//                                   newEvidence);
//       changer.addChange(ace);
//     }

    return;
  }

  /**
   * Checks whether the current SelectionSets are compatible with removing
   * evidence from exons.
   */
  public boolean removeEvidenceExonsAllowed() {
    return goodEvidence (annotSet.getTranscripts(), evidenceSet.getLeafFeatures());
  }

  /**
   * The default removeEvidenceExons which uses the viewSet to determine the
   * transcript to remove evidence from the exons. The evidenceSet is used to
   * determine the features to remove as evidence.
   */
  public void removeEvidenceExons() {
    switch (annotSet.getTranscripts().size()) {
    case 0:
      showMessage(JOptionPane.ERROR_MESSAGE,"Selected NO transcript to remove exon evidence from");
      break;
    case 1:
      removeEvidenceExons(annotSet.getTranscript(0),
                          evidenceSet.getLeafFeatures());
      break;
    default:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected more than 1 transcript to remove exon evidence from");
      break;
    }
  }

  /**
   * Remove evidence to exons in a transcript.
   * This just begins and ends the edit. The work is done by
   * {@link #removeEvidenceExons(Transcript,Vector,AnnotationChangeCoalescer)}.
   *
   * @param trans    The transcript to remove evidence from the exons of.
   * @param features A Vector of the features to remove as evidence.
   */
  public void removeEvidenceExons(Transcript trans, Vector features) {

    ChangeList changer = beginEdit("remove evidence exons");

    removeEvidenceExons(trans,features,changer);

    endEdit(changer, true, trans.getGene(), FeatureChangeEvent.REDRAW);
  }

  /**
   * This actually does the removal of Evidence from exons in a transcript.
   * It is protected because it is not a complete edit (there is no beginEdit
   * or endEdit call in this method.
   * @param trans The transcript to remove exons from.
   * @param features The Vector of features to remove as evidence from the exons
   *                 of trans.
   * @param changer The coalescer for this edit.
   I think evidence and thus this method is irrelevant now w/ edge matching.
   not firing annot change event - see comment in addEvidenceExons
   */
  protected void removeEvidenceExons(Transcript trans, Vector features,
                                     ChangeList changer) {
    SeqFeatureI feature  = null;
    int         nremoved = 0;

    for (int i=0;i<features.size();i++) {
      feature = (SeqFeatureI)features.elementAt(i);
      boolean     removed = false;
      if (feature.getId() != null) {
        for (int j=0; j<trans.size(); j++) {
          ExonI exon = trans.getExonAt(j);
          int nDel = exon.deleteEvidence(feature.getId());
          if (nDel > 0) {
            if (removed) {
              showMessage(JOptionPane.WARNING_MESSAGE,
                          "Evidence " + feature.getId() +
                          " removed from multiple exons");
            }
            removed = true;
            nremoved+=nDel;
          }
        }
        if (!removed) {
          showMessage(JOptionPane.ERROR_MESSAGE,
                      "Tried to delete feature which is not evidence");
        }
      } else {
        logger.warn("tried to remove evidence with null ID (this may be OK)");
      }
    }
    
    // not firing annot change event - see comment in addEvidenceExons
//     if (nremoved > 0) {
//       AnnotationChangeEvent ace
//       = new AnnotationChangeEvent(this,
//                                   trans.getRefFeature(),
//                                   FeatureChangeEvent.DELETE,
//                                   AnnotationChangeEvent.EVIDENCE,
//                                   trans,
//                                   feature);
//       changer.addChange(ace);
//     }

    return;
  }

  public boolean addExonsAllowed() {
    if (!validNewExons (cursorSet.getTranscripts(),
                        evidenceSet.getLeafFeatures()))
      return false;
    if (warnOnEdit(cursorSet.getTranscripts())) {
      JOptionPane.showMessageDialog(null,"Please note: the selected annotation is protected,\nso you shouldn't add exons to it.");
      // Let them do it anyway
      // return false;
    }
    return true;
  }

  /** Some new properties in ChadoXML (e.g. mutant_in_strain) don't allow you
      to edit the peptide, so we may need to warn user. */
  public boolean warnOnEdit(Vector transcripts) {
    //    Transcript transcript = (Transcript) transcripts.elementAt(transcripts.size() - 1);
    FeatureSetI feat = (FeatureSetI) transcripts.elementAt(transcripts.size() - 1);
    SeqFeatureI parent = feat.getRefFeature();

    // warnOnEdit is on a per-type basis
    if (Config.getPropertyScheme().getFeatureProperty(feat.getTopLevelType()).warnOnEdit())
      return true;

    if (checkFeatureAndParentForProperty("warn_about_edit", feat, parent))
      return true;
    if (checkFeatureAndParentForProperty("unstranded", feat, parent))
      return true;
    if (feat.hasReadThroughStop())
      return true;
//     if (checkFeatureAndParentForProperty("stop_codon_readthrough", transcript, parent))
//       return true;
//     if (checkFeatureAndParentForProperty("stop_codon_redefinition_as_selenocysteine", transcript, parent))
//       return true;

    // These next two can probably be deleted when Harvard starts adding the
    // warn_about_edit property to the relevant transcripts
    if (checkFeatureAndParentForProperty("mutant_in_strain", feat, parent))
      return true;
    if (checkFeatureAndParentForProperty("transpliced_transcript", feat, parent))
      return true;
    return false;
  }

  private boolean checkFeatureAndParentForProperty(String property, FeatureSetI feat, SeqFeatureI parent) {
    if ((!(feat.getProperty(property).equals(""))
         && !(feat.getProperty(property).equals("0"))
         && !(feat.getProperty(property).equals("false")))
        || (parent != null 
            && !(parent.getProperty(property).equals(""))
            && !(parent.getProperty(property).equals("0"))
            && !(parent.getProperty(property).equals("false"))))
      return true;
    else
      return false;
  }

  /**
   * The default addExons which adds exons to a transcript. The features to add
   * as exons should be in the annotSet, with the transcript to add to in the
   * cursorSet.
   */
  public void addExons() {
    switch (cursorSet.getTranscripts().size()) {
    case 0:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected NO transcript to add exons to");
      printSets();
      break;
    case 1:
      addExons(cursorSet.getTranscript(0),evidenceSet.getLeafFeatures());
      break;
    default:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected more than 1 transcript to add exons to");
      break;
    }
  }

  /**
   * The version of addExons which actually does the work. It adds the exons
   * sets the EvidenceFinder, consolidates the genes if necessary and
   * adds the features used to create the exons as evidence for them.
   * NOTE: The Evidence is currently added as SIMILARITY evidence.
   * @param trans The transcript to add exons to.
   * @param features A Vector of features to define the limits for
   * the new exons.
   */
  public void addExons(Transcript trans, Vector features) {
    SeqFeatureI            resFeature  = null;
    ExonI                  ex          = null;
    Vector added = new Vector();

    checkIfBeyondRange();

    logger.info ("****** ADDING EXONS ******");
    Vector exons_to_add = new Vector();

    // Really we should be prohibiting creation/modification of annots from collapsed tiers,
    // not just warning about it.
    boolean warnedAboutCollapsedTier = false;
    for (int i = 0; i < features.size(); i++) {
      resFeature = (SeqFeatureI)features.elementAt(i);
      if (!warnedAboutCollapsedTier && !(resFeature.getFeatureType().equals("tmptype")) && !Config.getPropertyScheme().getTierProperty(resFeature.getFeatureType()).isExpanded()) {
        String warning = "Warning: adding possibly multiple features from collapsed tier (" + resFeature.getFeatureType() + ")";
        showMessage(JOptionPane.WARNING_MESSAGE, warning);
        warnedAboutCollapsedTier = true;
      }
      boolean allowed = true;
      for (int j = 0; j < trans.size() && allowed; j++) {
        ExonI exon = trans.getExonAt (j);
        allowed &= ! (exon.overlaps(resFeature));
      }
      if (allowed) {
        added.addElement (resFeature);
        boolean merged = false;
        for (int j = 0; j < exons_to_add.size() && !merged; j++) {
          ExonI sf = (ExonI) exons_to_add.elementAt (j);
          merged = (sf.overlaps (resFeature));
          if (merged) {
            sf.merge (resFeature);
          }
        }
        if (!merged) {
          if (trans.getStrand() != resFeature.getStrand()) {
            logger.warn("Catering for incorrect strand in result");
            resFeature = new SeqFeature(resFeature.getLow(),
                                        resFeature.getHigh(),
                                        resFeature.getFeatureType(),
                                        trans.getStrand());
          }
          Exon newExon = new Exon (resFeature);
          exons_to_add.addElement (newExon);
        }
      }
    }
    ChangeList changer = beginEdit("add exons");

    if (exons_to_add.size() == 0) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Tried to add overlapping exon(s)");
    }
    else {
      for (int i = 0; i < exons_to_add.size(); i++) {
        ex = (ExonI) exons_to_add.elementAt (i);
        // this does an adjustEdges. need to check if transcript edges have changed
        // if so fire range event
        trans.addExon(ex);
        // Set id for exon
        setExonID(ex, trans.getGene().getId(), false);
        ex.setEvidenceFinder(trans.getEvidenceFinder());
        AnnotationAddEvent aae = new AnnotationAddEvent(this,ex);
        changer.addChange(aae);
      }
      changer.executeChanges();

      // i think this will need to be a separate event or transaction adapter
      // needs to deal with this
      trans.setOwner(getUserName());
      trans.replaceProperty ("date", (new Date()).toString());
      CompoundTransaction ct = consolidateGenes(trans.getGene());
      if (ct != null)
        changer.addChange(ct.generateAnnotationChangeEvent());
    }
    //  I thought we weren't doing anything with evidence anymore.
    //addEvidenceExons(trans,features,changer,EvidenceConstants.SIMILARITY);

    // Invalidate peptide sequence because it will need to be
    // recalculated if anyone asks for it later.  (Don't bother
    // recaculating right now--might be a waste of time.)
    trans.setPeptideValidity(false);
    trans.setTranslationEndFromStart();

    endEdit(changer,true, trans.getGene(), FeatureChangeEvent.REDRAW);

    return;
  }

  /**
   * Looks through all first-tier annotations (genes, etc.) in the annotationCollection
   * looking for overlapping (according to the current gene definition) annots.
   *
   * @param features Vector of features to look for overlaps to.
   * @returns A Vector of overlapping genes from the annotationCollection.
   */
  protected Vector findOverlappingAnnots(Vector features) {
    // The features of the geneHolder are the genes, are there other top
    // level annotations besides genes?
    Vector annots = annotationCollection.getFeatures();
    Vector  overlapGenes = new Vector();

    for (int i=0; i<annots.size(); i++) {
      AnnotatedFeatureI gi = (AnnotatedFeatureI)annots.elementAt(i);
      if (isFirstTierAnnot(gi)) {
        for (int j=0; j<features.size(); j++) {
          SeqFeatureI sf = (SeqFeatureI)features.elementAt(j);
          if (areOverlapping(gi,sf)) {
            if (gi.isAnnotTop()) {
              if (!overlapGenes.contains(gi)) {
                logger.trace("findOverlappingAnnots: adding " + gi.getName() + " (type = " + gi.getFeatureType() + ")");
                overlapGenes.addElement(gi);
              }
            }
          }
        }
      }
    }
    return overlapGenes;
  }

  /** Returns true if this is a first-tier annotation */
  public boolean isFirstTierAnnot(FeatureSetI annot) {
    return Config.getPropertyScheme().getTierProperty(annot.getFeatureType()).isFirstTier();
  }

  /**
   * Determine whether it's OK to do an add gene or transcript. It's OK if some
   * result features are selected.
   */
  public boolean addGeneOrTranscriptAllowed() {
    return (evidenceSet.getLeafFeatures().size() > 0 
	    // cursorSet is non-empty if some annotations are selected.
            // I'm not sure why that's so bad.
	    && cursorSet.getLeafFeatures().size() == 0);
  }

  /**
   * The default addGeneOrTranscript. This method creates a new Gene or
   * Transcript from the selSet (by default the current Selection).
   */
  public void addGeneOrTranscript() {
    // Default is NOT to create new overlapping gene
    addGeneOrTranscript(false);
  }

  public void addGeneOrTranscript(boolean overlapping) {
    if (!addGeneOrTranscriptAllowed()) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Add gene or transcript not allowed when no features selected");
      return;
    }
    checkIfBeyondRange();
    if (overlapping) {
      addOverlappingGene(evidenceSet.getLeafFeatures());
    }
    else {
      addAnnotOrTranscript(evidenceSet.getLeafFeatures());
    }
  }

  public void addNewAnnot(String type) {
    // Check that at least one result was selected--if not,
    // create a de novo annotation
    if (evidenceSet.getLeafFeatures().size() == 0) {
      createAnnotation(type);
      return;
    }
    // cursorSet > 0 when annotations are also selected
    if (cursorSet.getLeafFeatures().size() > 0) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "To create a new annotation, select only results, not existing annotations.\nIf you want to add a transcript to an existing annotation, use the menu option 'Add as new transcript to selected gene'.");
      return;
    }

    checkIfBeyondRange();

    addAnnotation(evidenceSet.getLeafFeatures(), false, type);
  }

  /**
   * This method determines whether there are any overlapping genes and calls
   * either {@link #addGene(Vector)} if there are not or
   * {@link #addTranscript(AnnotatedFeatureI,Vector)} if there are. A warning dialog appears
   * if a gene merge results from the addition of this transcript.
   * @param features The features to create exons in the new transcript from (these are
   * the evidence/result features used to make the gene).
   */
  private void addAnnotOrTranscript(Vector features) {
    Vector overlapAnnots = findOverlappingAnnots(features);

    //switch (overlapGenes.size()) {

    // NO OVERLAPS
    if (overlapAnnots.size() == 0) {
      // true means do ask whether to merge overlapping genes - true?? 
      // we already determined no overlap! - this just ends up calling
      // areOverlapping (in findMerges) again - no need
      //addGene(features, true);
      addAnnot(features,false);
    }
        
    // 1 OVERLAP  
    else if (overlapAnnots.size() == 1) {
      // If the overlapped annotation is not protein coding, don't add result
      // as a transcript of it--make it a new overlapping annot.
      // Also add new overlapping annot (not transcript) if the type of annot the
      // result(s) want to make is not the same as the annot that overlaps it.
      // this is now covered in areOverlapping (or at least should be)
      // the protein coding part should be covered with NoOverlap
//       if (((AnnotatedFeatureI)overlapGenes.elementAt(0)).isProteinCodingGene() &&
//           getAnnotTypeForMultipleResults(features).equals(((AnnotatedFeatureI)overlapGenes.elementAt(0)).getFeatureType()))
      addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(0),features);
//      else addOverlappingGene(features);
    }

    // 2 OR MORE OVERLAPS.... 
    else { // overlapGenes.size >= 2
      twoOrMoreOverlappingAnnots(features,overlapAnnots);
    } 
  }

  /** Deals with case in addGeneOrTranscript where 2 or more annots overlapped */
  private void twoOrMoreOverlappingAnnots(Vector features,Vector overlapAnnots) {
      String geneNames = "";
      for (int i = 0; i < overlapAnnots.size(); i++) {
        AnnotatedFeatureI g = (AnnotatedFeatureI) overlapAnnots.elementAt(i);
        geneNames = geneNames + g.getName();
        if (i == overlapAnnots.size()-2)
          geneNames = geneNames + " and ";
        else if (i < overlapAnnots.size()-2)
          geneNames = geneNames + ", ";
      }
      
      // Features overlap more than one gene--ask user what to do
      // false means DON'T include non-protein-coding annots
      // is there a reason findOverlappingGenes is called a 2nd time?
      //overlapGenes = findOverlappingGenes(features);
      Vector options = new Vector();
      options.add("New annotation");
      options.add("Merge annotations");
      for (int i = 0; i < overlapAnnots.size(); i++) {
        options.add("Add to " +
                    DetailInfo.getName((SeqFeatureI)overlapAnnots.elementAt(i)));
      }
      options.add("Cancel");
      Object[] optionsArray = options.toArray();

      // By default, the option buttons are all as big as the longest name--but that takes
      // up too much space when we have long (temp) gene names

      JOptionPane pane =
        new JOptionPane("The selected features overlap more than one annotation: " + geneNames + 
                        ".\n Please choose an option:\n" +
                        "- Add selected features as new separate but overlapping annotation\n" +
                        "- Merge " + geneNames + " using selected features\n" +
                        (overlapAnnots.size() > 0 ?
                         "- Add as new transcript of " + overlapAnnots.elementAt(0) + "\n" : "") +
                        (overlapAnnots.size() > 1 ?
                         "- Add as new transcript of " + overlapAnnots.elementAt(1) + "\n" : "") +
                        "- Cancel",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.OK_CANCEL_OPTION,
                        null, // icon
                        optionsArray, // all choices
                        optionsArray[1]); // initial value
      JDialog dialog = pane.createDialog(null,
                                         "Please Confirm");
      dialog.show();

      Object result = pane.getValue();
      if ((result == null) || ((String)result).equals("Cancel"))
        return;
      else if (((String)result).equals(optionsArray[0]))
        addNewAnnot(null);  // create new overlapping annot (type unspecified)
      else if (((String)result).equals(optionsArray[1])) { // Merge
        // Add Transcript to first of the two overlapping genes and allow genes to merge
        // without asking
        addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(0),
                      features,
                      true, false);
      }
      else if (options.size() > 2 && ((String)result).equals(optionsArray[2]))
        // Add transcript to first overlapping gene, don't merge
        addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(0),
                      features,
                      false, false);
      else if (options.size() > 3 && ((String)result).equals(optionsArray[3]))
        // Add transcript to second overlapping gene, don't merge
        addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(1),
                      features,
                      false, false);
      else if (options.size() > 4 && ((String)result).equals(optionsArray[4]))
        // Add transcript to third overlapping gene, don't merge
        addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(2),
                      features,
                      false, false);
      else if (options.size() > 5 && ((String)result).equals(optionsArray[5]))
        // Add transcript to fourth overlapping gene, don't merge
        addTranscript((AnnotatedFeatureI)overlapAnnots.elementAt(3),
                      features,
                      false, false);
  }

  /* Determine whether it's ok to add an overlapping gene.
   * This only makes sense if the selected features overlap at least one gene. */
  public boolean addOverlappingAllowed() {
    Vector features = evidenceSet.getLeafFeatures();
    if (features.size() == 0)
      return false;
    Vector overlapGenes = findOverlappingAnnots(features);
    if (overlapGenes.size() >= 1)
      return true;
    else
      return false;
  }

  /**
   * This method creates a new gene from the features, whether or not they overlap
   * an existing gene.
   * @param features The features to create exons in the new transcript from.
   */
  public void addOverlappingGene(Vector features) {
    addAnnot(features, false);  // false means don't merge overlapping genes
  }

  /** generate trans name from name adapter and sets it. add synonym of old
      name if not temp. set trans id as well (that could be controversial for
      genes with different name and id - should check if same format) 
      i think just about all of this should be relegated to the name adapter,
      which does synonyms now - also it does pep names (for fly) which this lacks
      and this should be centralized anyways!
      This may only be called for temp genes, which would mean we dont need to go 
      name adapter i think - look into this.
  */
  private CompoundTransaction setTransNameAndId(Transcript trans) {
    //ChangeList changes = createChangeList();
    CompoundTransaction compoundTransaction = new CompoundTransaction(this);

    // Also set id here. Probably it should be called inline where setTransName()
    // is called. However, name and id should always set at the same time. So, this
    // seems a good place to do this -- Guanming 
    // this isnt quite right - id and name can be different - revisit! - MG
    // this should also only happen if != as above
    // its backwards really - setTransId should call setTransName if same
    // -- actually i think this is ok - setTransId calls NA.genId which sets trans
    // id according to annot id NOT trans name

    // !! 2/2005: Chado XML has exon names (GAME XML typically doesn't) and they
    // need to be updated if the transcript name changes.  Rather than trying
    // to infer new exon names (which include ordering numbers), just assign
    // a new temp name.  (We probably also need to do this whenever we add or
    // delete an exon, because its number will no longer be correct!)
    // Go through each exon; if it has a name assigned, then give it a new temp
    // name.  (If it doesn't have a name, then don't worry about it.)

    CompoundTransaction ct = setTransID(trans);
    compoundTransaction.addTransaction(ct);

    // calls name adapter which may set old name as syn as well
    CompoundTransaction nameTransaction = setTransName(trans);
    compoundTransaction.addTransaction(nameTransaction);
    compoundTransaction.setSource(this);
    return compoundTransaction;
  }
  
  /** Calls name adapter.setTransName which may set old name as synonym as well */
  private CompoundTransaction setTransName(Transcript trans, AnnotatedFeatureI gene) {
    ApolloNameAdapterI nameAdapter = curationState.getNameAdapter(gene);
    String newName = nameAdapter.generateName(curation.getAnnots(),curation.getName(),trans);
    // name adapter may set old name as synonym as well
    CompoundTransaction ct = nameAdapter.setTranscriptName(trans,newName);
    return ct;
  }

  private CompoundTransaction setTransName(Transcript trans) {
    return setTransName(trans, trans.getGene());
  }

  /** returns null if old id is null - its a new trans no need for id update */
  private CompoundTransaction setTransID(Transcript trans, AnnotatedFeatureI gene) {
    ApolloNameAdapterI nameAdapter = curationState.getNameAdapter(gene);
    //    this aint right!!! we need to set the peptide id/acc somewhere - preferably NA
    String tID = (nameAdapter.generateId(curation.getAnnots(), 
                                         curation.getName(), 
                                         trans));
    // name adapter may set peptide id as well
    //    System.out.println("setTransID: old id = " + trans.getId() + ", new = " + tID);  // DEL
    CompoundTransaction compoundTransaction = nameAdapter.setTranscriptId(trans,tID);
    //UpdateTransaction ut = setID(trans,tID);
    // name adapter?
    setExonIdsFromTranscript(trans); // no need for transactions at this point...
    return compoundTransaction;
  }

  private CompoundTransaction setTransID(Transcript trans) {
    return setTransID(trans, trans.getGene());
  }

  /** sets annot id, returns update transaction. returns null if old id is null
   *  as then its a new annot so dont need transaction as it will get coalesced out
   *  anyways, perhaps this is presumptious. actually since it will get coalesced
   *  it doesnt hurt to include - actually we will need it for undo wont we? actually
   *  probably not - undoing new annot will just delete it - no need to undo its id
   *  before deleting it
   *  shouldnt this recursively set annot, trans, exon id and name? 
   *  helper function to mergeGeneIdentifiers */
  private UpdateTransaction setID(AnnotatedFeatureI annot,String newId) {
    TransactionSubpart ts = TransactionSubpart.ID;
    UpdateTransaction ut = new UpdateTransaction(annot,ts,annot.getId(),newId);
    ut.editModel();
    ut.setSource(this);
    return ut;
  }
  
  /** return CompoundTransaction? i think we will if chado naming trigger gets nixed 
   *  -> name adapter? */
  private void setExonIdsFromTranscript(Transcript trans) {
    for (int i=0; i<trans.size(); i++) {
      setExonID(trans.getExonAt(i),trans.getRefFeature().getId(),false);
    }
  }

  private void setExonID(ExonI exon, String geneID, boolean newId) {
    if (newId) {
      exon.setId(getNameAdapter(exon).generateNewExonId(curation.getAnnots(),curation.getName(),exon,geneID));
    } else {
      exon.setId(getNameAdapter(exon).generateExonId(curation.getAnnots(),curation.getName(),exon,geneID));
    }
  }

  /** Whether allowed to add transcript, just calls addEvidenceExonsAllowed
      as conditions are same */
  public boolean addTranscriptAllowed() {
    return addEvidenceExonsAllowed();
  }

  /**
   * If called with no args, addTranscript checks which gene was selected
   * and adds the transcript to that gene. */
  public void addTranscript() {
    if (annotSet.getTranscripts().size() != 1) {
      showMessage(JOptionPane.WARNING_MESSAGE,
                  "Can't add transcript to " +
                  annotSet.getTranscripts().size() +
                  "genes.  Please select exactly one gene transcript.");
      return;
    }
    Transcript trans = (Transcript)(annotSet.getTranscripts().elementAt(0));
    AnnotatedFeatureI gene = trans.getGene();
    // Don't ask, don't merge are the 2 flags
    addTranscript(gene, evidenceSet.getLeafFeatures(), false, false);
  }

  private void addTranscript(AnnotatedFeatureI gene, Vector features) {
    // Default is to ask about merge overlapping genes
    addTranscript(gene, features, true, true);
  }

  private void addTranscript(AnnotatedFeatureI gene,
			     Vector features,
			     boolean mergeOverlapping,
			     boolean askAboutMerge) {
    // null means desired type not specified--will be figured out
    addTranscript(gene, features, null, mergeOverlapping, askAboutMerge);
  }
  /**
   * The addTranscript method is a complete edit which actually does the work
   * of adding a transcript to an existing gene. It makes a new transcript,
   * adds it to the gene, adds the features as evidence to the new exons in the
   * new transcript, and performs any necessary gene merges which result.
   * @param gene The gene to add the transcript to.
   * @param features The Vector of SeqFeatureIs to create the new exons from.
   */

  private void addTranscript(AnnotatedFeatureI gene,
			     Vector features,
			     String type,
			     boolean mergeOverlapping,
			     boolean askAboutMerge) {
    logger.info ("****** ADDING TRANSCRIPT ******");

    // Really we should be prohibiting creation/modification of annots from collapsed tiers,
    // not just warning about it.
    boolean warnedAboutCollapsedTier = false;
    for (int i = 0; i < features.size(); i++) {
      SeqFeatureI resFeature = (SeqFeatureI)features.elementAt(i);
      if (!warnedAboutCollapsedTier && !(resFeature.getFeatureType().equals("tmptype")) && !Config.getPropertyScheme().getTierProperty(resFeature.getFeatureType()).isExpanded()) {
        String warning = "Warning: adding possibly multiple features from collapsed tier (" + resFeature.getFeatureType() + ")";
        showMessage(JOptionPane.WARNING_MESSAGE, warning);
        warnedAboutCollapsedTier = true;
      }
    }

    Transcript trans = buildTranscript (gene, features, type);

    if (gene.isProteinCodingGene())
      trans.calcTranslationStartForLongestPeptide();
    // since transcript name is derived from its parent gene the name should not be set
    // until it is assigned to the gene, also for pep name has to be after start set
    setTransNameAndId(trans);
    trans.setOwner(getUserName());
    trans.replaceProperty ("date", (new Date()).toString());

    if (annotationView != null && annotationView.getEvidenceFinder() != null) {
      gene.setEvidenceFinder(annotationView.getEvidenceFinder());
    } else {
      logger.error("Didn't set EF for new annotations");
    }

    ChangeList changer = beginEdit("add transcript");

    AnnotationAddEvent aae = new AnnotationAddEvent(this,trans);
    changer.addChange(aae);
    changer.executeChanges();

    addEvidenceExons(trans, features, changer, EvidenceConstants.SIMILARITY); //rm?
    if (mergeOverlapping) {
      // true means ask whether to merge--sometimes it ends up asking twice
      // but right now I can't get it to ask exactly once every time, and twice
      // is better than not at all.
      CompoundTransaction merge = consolidateGenes(gene, true);
      if (merge != null)
        changer.addChange(merge.generateAnnotationChangeEvent());
    }
    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW);
    // Select newly-created transcript trans, annotationView);
    // selectionManager now does select

  }

  /**  Warn user about the impending split and give them a chance to
       cancel it */
  protected boolean userReallyWantsToSplit(AnnotatedFeatureI orig_gene) {
    String prevName = orig_gene.getName();
    String prevId = orig_gene.getId();
    Object[] options = { "Split",
                         "Cancel" };

    if (!prevName.equals(prevId))
      prevName += " (" + prevId + ")?";
    String msg = "Are you sure you want gene " + prevName + " to be split?";

    JOptionPane pane =
      new JOptionPane(msg,
                      JOptionPane.DEFAULT_OPTION,
                      JOptionPane.OK_CANCEL_OPTION,
                      null, // icon
                      options, // all choices
                      options[0]); // initial value
    JDialog dialog = pane.createDialog(null,
                                       "Please Confirm");
    dialog.show();

    Object result = pane.getValue();
    if (result != null && ((String)result).equals("Split"))
      return true;
    else
      return false;
  }

  /** Creates new Transcript. Makes Exons from ranges for seq features in features
      vector and adds them to Transcript. Adds Transcript to gene.
   */
  private Transcript buildTranscript(AnnotatedFeatureI gene,
                                     Vector features, String type) {
    Transcript t = new Transcript();
    SequenceI seq = null;
    String site = "";
    /* this is the default */
    String annot_type = null;
    boolean inherit_type = true;
    for (int i = 0; i < features.size(); i++) {
      Object elem = features.elementAt(i);
      if (elem instanceof SeqFeatureI) {
        SeqFeatureI sf = (SeqFeatureI)elem;
        ExonI exon = new Exon(sf);
        if (seq == null) {
          seq = sf.getRefSequence();
        }
        if (seq == null && annotationCollection != null)
          seq = annotationCollection.getRefSequence();
        if (seq == null) {
          logger.warn (sf.getName() + " (" +
                       sf.getId() + ") " +
                       "has no reference seq");
        }
        exon.setRefSequence(seq);
        t.setStrand (exon.getStrand());

        // add the exon even if its overlapping. mergeExons expects that.
        t.addFeature(exon, true);

        // check if exon overlaps any existing exons. this can happen if user
        // creating trans with overlapping results (disallow?)
        FeatureList overlappingExons = getOverlappingExons(t,exon);

        // merge any overlapping exons
        if (!overlappingExons.isEmpty()) {
          // dont need compound transaction, its new so dont care that we are purging out
          // overlapping exons - dont need to record this
          mergeExons(t,overlappingExons, false);
        }

        setExonID(exon, gene.getId(), true);
        if (site.equals(""))
          site = sf.getProperty("insertion_site");
        if (site.equals("") && sf.getRefFeature() != null)
          site = sf.getRefFeature().getProperty("insertion_site");
        /* in order to inherit a default annotation type
           all of the selected features must
           be the sort used to create the same annotation type 
           newGeneSetup no longer needs this, but i think addTranscript does
           i think ultimately 2 annot types should be disallowed */
	String new_type = (type == null ? getAnnotTypeForResult(sf) : type);
        inherit_type &= !(new_type == null ||
                          (new_type != null && annot_type != null &&
                           !new_type.equals(annot_type)));
        annot_type = new_type;
      } else {
        logger.error("Feature vector contained non SeqFeatureI element");
      }
    }
    t.setRefSequence(seq);
    t.addProperty("insertion_site", site);

    //... do translate and if has seq, do peptide naming with name adapter???
    // actually i think it should happen in setTransName which is called outside this
    // this method...

    /* inherit the type as well  */
    if (inherit_type) {
      //      System.out.println ("Inheriting annot type " + annot_type);
      gene.setFeatureType(annot_type);
    }
    gene.setStrand(t.getStrand());
    gene.addFeature(t);
    return t;
  }

  /** if sf is annot return its biotype, if its a result return getAnnotTypeForResult
   */
  private String getAnnotTypeForFeature(SeqFeatureI sf) {
    if (sf.isAnnot())
      return sf.getTopLevelType();
    return getAnnotTypeForResult(sf);
  }

  private String getAnnotTypeForResult(RangeI sf) {
    PropertyScheme scheme = Config.getPropertyScheme();
    FeatureProperty fp = scheme.getFeatureProperty(sf.getFeatureType());
    // should this return "gene" the default type if no fp
    String type = (fp != null ? fp.getAnnotType() 
                   : FeatureProperty.getDefaultAnnotType());
    return type;
  }

  /** Not currently used */
//   private String getAnnotTypeForMultipleResults(Vector features) {
//     String annot_type = "";
//     for (int i = 0; i < features.size(); i++) {
//       Object elem = features.elementAt(i);
//       if (elem instanceof SeqFeatureI) {
//         SeqFeatureI sf = (SeqFeatureI)elem;
//         String typeForRes = getAnnotTypeForResult(sf);
//         if (annot_type == null || annot_type.equals(""))
//           annot_type = typeForRes;
//         else if (!(typeForRes == null) && !typeForRes.equals(annot_type)) {
//           //          System.out.println("getAnnotTypeForMultipleResults: annot types include " + annot_type + " and " + typeForRes);  // DEL
//           return "multiple types";
//         }
//       }
//     }
//     if (annot_type == null)
//       annot_type = "gene";  // default type
//     return annot_type;
//   }
  
  /** Returns list of exons in transcript that exon overlaps, includes exon itself. */
  private FeatureList getOverlappingExons(SeqFeatureI transcript, SeqFeatureI exon) {
    FeatureList overlappingExons = new FeatureList();
    for (int i=0; i < transcript.getNumberOfChildren(); i++) {
      SeqFeatureI transExon = transcript.getFeatureAt(i);
      // dont add new exon at this point (only later if there is overlap)
      if (transExon.overlaps(exon) && transExon != exon) {
        overlappingExons.addFeature(transcript.getFeatureAt(i));
      }
    }
    // add exon if there is overlap
    if (overlappingExons.size() > 0 && !overlappingExons.contains(exon)) {
      overlappingExons.add(0,exon); // 1st element?
    }
    return overlappingExons;
  }

  /**
   * A method to setup a newly created annot. This method sorts out the
   * RefFeature, sets the holder flag, generates a name for the gene and for
   * each of its transcripts, sets the evidence finder and adds the gene as a
   * child of the parent.
   * @param parent The parent for the new gene.
   * @param gene   The new gene.
   */
  private AnnotatedFeatureI newGeneSetup(Vector features, String type) {
    AnnotatedFeatureI annot = new AnnotatedFeature();

    if (type != null) {
      annot.setFeatureType(type);
    }
    else {
      // try to get type from (result) features
      setTypeFromFeatures(annot,features);
    }

    annot.setRefSequence(annotationCollection.getRefSequence());
    // Move from after buildTranscript(annot, feature) since annot id
    // is needed to setup exon's ids that is build in transcript.
    // buildTranscript was setting gene type, but type is needed for setting
    // gene name with name adapter so doing above with setTypeFromFeats
    setAnnotNameEtc(annot,features);
    
    if (!isOneLevelAnnot(annot)) {
      buildTranscript(annot, features, type);
    }
    else {
      if (features.size() == 0)
        return annot; // shouldnt happen - errmsg? ex?
      RangeI result = (RangeI)features.get(0); // 1-level has 1 result
      buildOneLevelAnnotFromResult(annot, result);
    }

    //setAnnotNameEtc(annot);
    return annot;
  }

  private boolean isOneLevelAnnot(RangeI annot) {

    // only doing this in debug mode for now
    if (!DO_ONE_LEVEL_ANNOTS) return false;

    PropertyScheme ps = Config.getPropertyScheme();
    FeatureProperty fp = ps.getFeatureProperty(annot.getFeatureType());
    return fp.getNumberOfLevels() == 1;
  }

  private void buildOneLevelAnnotFromResult(RangeI annot, RangeI result) {
    annot.setStrand(result.getStrand());
    annot.setStart(result.getStart());
    annot.setEnd(result.getEnd());
    // ??? strand?
  }


  private void setTypeFromFeatures(AnnotatedFeatureI annot, Vector resultFeats) {
    if (resultFeats.size() == 0)
      return; // exception? err msg?

    // eventually should check for multiple annot types in results...
    //boolean hasMultiTypes = resultFeatsHaveMultipleType(resultFeats);
    // if(hasMultiTypes) throw new EditorException("Multiple annot types in results");

    RangeI firstResult = (RangeI)resultFeats.get(0);
    String annotType = getAnnotTypeForResult(firstResult);
    // if annotType == null throw exception??
    // if no type, ie result not associate with an annot type.
    // set to default "gene" type
    if (annotType == null || annotType == SeqFeatureI.NO_TYPE)
      annot.setFeatureType("gene");
    else
      annot.setFeatureType(annotType);
  }

  /** Names annot using name adapter, gene is expected to have it types set to get
   *  the proper name adapter for it, and have the name adapter work properly */
  private void setAnnotNameEtc(AnnotatedFeatureI annot, Vector resultExons) {
    /* these must be added in sorted order so that the EDE upstream
       and downstream code works */
    annotationCollection.addFeature(annot, true);

    ApolloNameAdapterI nameAdapter = curationState.getNameAdapter(annot);
    String geneName = nameAdapter.generateName(curation.getAnnots(), curation.getName(),
                                               annot, resultExons);
    annot.setName (geneName);
    annot.setId (geneName);
    annot.setEvidenceFinder(annotationView.getEvidenceFinder());
  }

  /**
   * Method that sets the id and name of a new gene created by a "split" event.
   * Also updates the id and name of the original gene and copies over any relevant 
   * gene-associated annotation information from the original gene to the new one.
   *
   * @param parent Parent feature of the original gene.
   * @param orig_gene The original gene.
   * @param new_gene The new ("split") gene.
   * @return A CompoundTransaction with all the update events.
   */
  private CompoundTransaction setSplitGeneIds(FeatureSetI parent,
					      AnnotatedFeatureI orig_gene,
					      AnnotatedFeatureI new_gene) {

    CompoundTransaction compoundTransaction = new CompoundTransaction(this);

    // current/original name for orig_gene
    String origGeneOldName = orig_gene.getName();
    // new name for orig_gene (which should have a name/id already)
    String origGeneNewName = "";
    // new name for new_gene (which should not have a name/id already)
    String newGeneNewName = "";

    ApolloNameAdapterI nameAdapter = curationState.getNameAdapter(new_gene);
    StrandedFeatureSetI annots = curation.getAnnots();
    String curation_name = curation.getName();

    // ----------------------------------
    // new_gene name and id
    // ----------------------------------

    // IMPORTANT: to avoid naming inconcistencies with gmodNameAdapter the new_gene's name and
    // ID must be set before processing origGeneNewName

    // set new name for new_gene
    newGeneNewName = nameAdapter.generateAnnotSplitName(orig_gene,annots,curation_name);
    new_gene.setName(newGeneNewName);
    
    // set new id for new_gene
    if (nameAdapter.nameIsId (new_gene)) {
      // name and id are the same
      new_gene.setId (new_gene.getName());
    } else {
      // name and id aren't necessarily the same
      new_gene.setId (nameAdapter.generateId(annots,curation_name,new_gene));
    }
    
    

    // ----------------------------------
    // handle name swapping
    // ----------------------------------

    // whether the gene names were swapped by splitGeneSwapNameDialog
    boolean swapped = false;

    // queryForNamesOnSplit() == false
    if(!Config.getStyle().queryForNamesOnSplit())
    {
      origGeneNewName =
        nameAdapter.generateAnnotSplitName(orig_gene,annots,curation_name);
      logger.debug("not swapping name, origGeneNewName = " + origGeneNewName);
    }
    
    // queryForNamesOnSplit() == true (currently used only by TAIR?)
    else
    {
      swapped = splitGeneSwapNameDialog(orig_gene,new_gene);
      origGeneNewName = orig_gene.getName();
      // The new gene's ID has been set before. Modify it if needed.
      if (nameAdapter.nameIsId (new_gene))
        new_gene.setId (new_gene.getName());
      logger.debug("swapping name, origGeneNewName=" + origGeneNewName);
    }

    // ----------------------------------
    // orig_gene name and id
    // ----------------------------------
    // once the new_gene name and id have been set we can do the orig_gene:

    // orig_gene NAME

    // this seems wrong - this seems like it will assign the old gene a new temp id
    // thats not what we want is it? - actually that is what we want - who knew
    // fire name event
    UpdateTransaction origGeneNameTrans = null;
    if(swapped || !Config.getStyle().queryForNamesOnSplit())
    {
      // transaction needs to be created before the change is made 
      origGeneNameTrans = new UpdateTransaction(orig_gene,TransactionSubpart.NAME,orig_gene.getName(),origGeneNewName);
    }
    orig_gene.setName(origGeneNewName);

    // orig_gene ID
    String origGeneOldId = orig_gene.getId();
    String origGeneNewId = null;

    if (nameAdapter.nameIsId (orig_gene)) {
      origGeneNewId = orig_gene.getName();
    } else {
      origGeneNewId = nameAdapter.generateId(annots, curation_name, orig_gene);
    }
    // transaction needs to be created before the change is made 
    UpdateTransaction origGeneIdTrans = new UpdateTransaction(orig_gene,TransactionSubpart.ID,origGeneOldId,origGeneNewId);
    orig_gene.setId(origGeneNewId);

    logger.info ("When " + origGeneOldName +
                 " is split into two genes, one gene (orig_gene) will have name=" +
                 orig_gene.getName() + ", id=" + orig_gene.getId());

    // orig_gene transactions - change id before name, but make sure name update
    // transaction is using the new id (otherwise it won't find the renamed transcript!)

    // change id first then name
    compoundTransaction.addTransaction(origGeneIdTrans);
    if(origGeneNameTrans != null) {
      // since this isn't the order in which it was really done we need to make
      // a correction to prevent id-based lookups from failing:
      if (Config.getSaveClonedFeaturesInTransactions()) {
        SeqFeatureI sf = origGeneNameTrans.getSeqFeature();
        SeqFeatureI sfClone = origGeneNameTrans.getSeqFeatureClone();
        sfClone.setId(sf.getId());
      }
      compoundTransaction.addTransaction(origGeneNameTrans);
    }

    // NOTE: don't need to create update name/id transactions for new_gene because 
    // the new_gene's name and id will be set when the AddTransaction for that gene
    // is processed

    logger.info (" and the other gene (new_gene) will have name=" +
                 new_gene.getName() +
                 ", id=" + new_gene.getId());

    new_gene.setEvidenceFinder(annotationView.getEvidenceFinder()); // pase - delete?

    // copy annotation data
    // make sure to do this first (?)
    compoundTransaction.addTransaction(copyAnnotData(orig_gene,new_gene));

    // Synonyms need an owner.  If this is a newly-added synonym, the
    // owner is the person who has renamed this gene.
    String username = getUserName();
    compoundTransaction.addTransaction(addSynonym(new_gene, origGeneOldName, username));
    compoundTransaction.addTransaction(addSynonym(orig_gene, origGeneOldName, username));

    // Harvard wants the old name added as a synonym, but not the old uniquename (id)
    //    compoundTransaction.addTransaction(addSynonym(orig_gene,prevId, username));
    //    compoundTransaction.addTransaction(addSynonym(new_gene,prevId, username));

    return compoundTransaction;
  }

  /**
   * Used in split gene events to allow the user to determine which gene (the original
   * gene or the new gene) gets to keep the original gene's name.  This option was 
   * added by TAIR, according to the comments in Style.java (and is controlled by
   * the queryForNamesOnSplit() option)
   *
   * @param left_gene The original/left gene.
   * @param right_gene The new/right gene.
   * @return Whether the gene names were swapped.
   */
  private boolean splitGeneSwapNameDialog(AnnotatedFeatureI left_gene,
					  AnnotatedFeatureI right_gene)
  {
    String name1 = left_gene.getName();
    String name2 = right_gene.getName();
    Object[] options = { "5' GENE","3' GENE" };

    // TODO - put this message directly in the dialog, in case there's no terminal window output?
    String message = "Please choose the gene to get the original name: left or right";
    logger.info(message);
    JOptionPane pane =
      new JOptionPane(message,
                      JOptionPane.DEFAULT_OPTION,
                      JOptionPane.OK_CANCEL_OPTION,
                      null, // icon
                      options, // all choices
                      options[0]); // initial value

    JDialog dialog = pane.createDialog(null, "Please choose one");
    dialog.show();

    Object result = pane.getValue();
    boolean returnValue = false;

    // Too late to back out of the split now--just pick one for them
    if(result == "3' GENE") {
      left_gene.setName(name2);
      right_gene.setName(name1);
      returnValue = true;
    }
    return returnValue;
  }

  /** Return compound transaction of update name transactions for transcripts 
   *  also update id & possibly add synonyms */
  private CompoundTransaction setTransNameFromGene (AnnotatedFeatureI gene, boolean forceSet) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    int trans_count = gene.size();
    String gene_name = gene.getName();
    for (int i = 0; i < trans_count; i++) {
      Transcript trans = (Transcript) gene.getFeatureAt (i);
      if (!(trans.getName().startsWith (gene_name))
          || forceSet)
        compTrans.addTransaction(setTransNameAndId(trans));
    }
    return compTrans;
  }

  /**
   * Duplicates a transcript.
   */
  public void duplicateTranscript(Transcript transcript) {
    checkIfBeyondRange();

    logger.info ("****** DUPLICATING TRANSCRIPT ******");

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    SeqFeatureI topLevelFeat = getTopLevelFeat(transcript);
    generateFeatChangeData(topLevelFeats, topLevelFeat);
    
    AnnotatedFeatureI gene = (AnnotatedFeatureI) transcript.getRefFeature();
    Transcript newTrans = new Transcript(transcript);
    gene.addFeature(newTrans);
    String geneId = gene.getId();
    for (Iterator it = newTrans.getFeatures().iterator(); it.hasNext();) {
      SeqFeatureI tmp = (SeqFeatureI) it.next();
      if (tmp instanceof ExonI)
        setExonID((ExonI)tmp, geneId, true);
    }
    if (gene.isProteinCodingGene()) {
      newTrans.setTranslationStart(transcript.getTranslationStart(), true);
    }
    // this may set peptide id/acc as well (name adapter dependent) in which case
    // it has to happen after translation start (which produces pep)
    setTransNameAndId (newTrans);
    // IDs are needed for SeqFeatureI objects for Transaction handling
    //setTransID(newTrans); setTransNameAndId does this already
    newTrans.setOwner(getUserName());
    newTrans.replaceProperty ("date", (new Date()).toString());
    ChangeList changer =  beginEdit("Duplicate transcript");
    /*
    AnnotationAddEvent aae = new AnnotationAddEvent(this,newTrans);
    changer.addChange(aae);
    changer.executeChanges();
    */

    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW);
    // Select newly-created transcript newTrans, annotationView); -> selMan
    
    generateEditTransaction(topLevelFeats);
  }

  /**
   * This method is a complete edit for adding a new gene. The gene is created
   * by making exons from the SeqFeatureIs in the features Vector and adding
   * the new gene to the AnnotationSet (the parent of all the Genes). The
   * features used to create the new gene are added as Evidence to the exons of
   * the new gene. A
   * {@link #consolidateGenes(AnnotatedFeatureI,AnnotationChangeCoalescer)} is
   * done to do any necessary gene merges.
   * <BR>
   * NOTE: The Evidence is currently added as SIMILARITY evidence.
   * @param features The Vector of SeqFeatureIs to base the exons in the new
   *                 gene on.
   */
  public void addAnnot(Vector features, boolean mergeOverlapping) {
    // null means type for new annot is not defined--addAnnotation will
    // figure it out.
    addAnnotation(features, mergeOverlapping, null);
  }

  public void addAnnotation(Vector features, boolean mergeOverlapping, String type) {
    if (annotationCollection == null) {
      return;
    }

    // Really we should be prohibiting creation/modification of annots from collapsed tiers,
    // not just warning about it.
    boolean warnedAboutCollapsedTier = false;
    for (int i = 0; i < features.size(); i++) {
      SeqFeatureI resFeature = (SeqFeatureI)features.elementAt(i);
      if (!warnedAboutCollapsedTier && !(resFeature.getFeatureType().equals("tmptype")) && !Config.getPropertyScheme().getTierProperty(resFeature.getFeatureType()).isExpanded()) {
        String warning = "Warning: adding possibly multiple features from collapsed tier (" + resFeature.getFeatureType() + ")";
        showMessage(JOptionPane.WARNING_MESSAGE, warning);
        warnedAboutCollapsedTier = true;
      }
    }

    // calls buildTranscript which makes the transcripts (if not 1 level), but doesnt
    // set cds - probably should
    AnnotatedFeatureI annot = newGeneSetup(features, type);

    // Check whether new annot goes out of range of current region; if
    // so, refuse to create it.  (Should really disable "Create new
    // annotation" AnnotationMenu option if click is out of range,
    // rather than letting user invoke that and then complaining and
    // refusing to create the new out-of-range annot.)
    if (!(annot.isContainedByRefSeq())) {
      String m = "ERROR: your new annotation is not entirely contained within the current sequence region--cannot create.";
      logger.error(m);
      JOptionPane.showMessageDialog(null,m);
      return;
    }

    logger.info ("****** Adding new" + (type == null ? "" : " " + type) + " annotation ******");

    if (!isOneLevelAnnot(annot)) {
      Transcript trans = (Transcript) annot.getFeatureAt (0);
      //setTransNameAndId(trans); -> moved down
      if (annot.isProteinCodingGene())
        trans.calcTranslationStartForLongestPeptide();
      // also sets peptide name, needs to be after trans start to get peptide
      setTransNameAndId(trans);
      setOwnerAndDate(trans);
    } 
    else // one-level annot
      setOwnerAndDate(annot);

    ChangeList changer = beginEdit("add annot");
    AnnotationAddEvent aae = new AnnotationAddEvent(this,annot);
    changer.addChange(aae);
    changer.executeChanges();

    // evidence exons are pase (mmade so by highlighting matched ends)
//     addEvidenceExons(trans, features,changer,
//                      EvidenceConstants.SIMILARITY);

    if (mergeOverlapping) {
      CompoundTransaction merge = consolidateGenes(annot);
      if (merge != null)
        changer.addChange(merge.generateAnnotationChangeEvent());
    }

    endEdit(changer, true, annot, FeatureChangeEvent.REDRAW);
  }


  /**
   * Determine if the current selections are compatible
   * with moving exons to another transcript.
   * Want a whole transcript plus one or more exons from another transcript.
   */
  public boolean moveExonsToTranscriptAllowed() {
    if ((annotSet.getTranscripts().size() == 2) &&
        annotSet.getFirstFullySelectedTranscript() != null)
      return true;
    return false;
  }

  /**
   The default moveExonsToTranscript. This version expects two transcripts
   to be selected in the viewSet. The receiving transcript has to have all
   of its exons selected. If the donor transcript has all of its exons
   selected. The operation is synonomous with mergeTranscripts. The exons
   from the donor transcript will be moved to and merged with receiving
   transcript. Issues: start and stop not being recalculated - do here?
   New exon in transcript not selected - do here?
   Used to be called mergeTranscriptWithExons.
   */
  public void moveExonsToTranscript() {
    Transcript trans = annotSet.getFirstFullySelectedTranscript();
    if (trans == null) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "The transcript accepting exons must be fully selected.");
      return;
    }
    /* The if below is shown if we've selected transcripts that are on
       opposite strands. */
    if (annotSet.getTranscripts().size() < 2) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Can only add exons from other transcripts.\n" +
                  "You must select a transcript plus some exons on " +
                  "the SAME strand.");
      return;
    }
    moveExonsToTranscript(trans, annotSet.getLeafFeatures());
  }

  /**
   * The moveExonsToTranscript which actually does the edit.
   * to be selected in the viewSet. Any of the exons in the feature
   * which are not from trans are added to trans (and removed from
   * their current transcript).
   * @param tgtTrans The transcript to merge with(target).
   * @param features A Vector of ExonIs to merge with trans
   * (any from trans will be ignored).
   */
  private void moveExonsToTranscript(Transcript trans, Vector features) {
    moveExonsToTranscript(trans,features,"move exons to transcript");
  }

  /** This is used by both moveExonToTrans and mergeTranscripts */
  private void moveExonsToTranscript(Transcript tgtTrans,
                                     Vector features,
                                     String log) {
    /* ! For some reason, it's not warning if you try to merge an
       out-of-range exon with an in-range transcript. */
    checkIfBeyondRange();

    // If the features (exons) that are going to be moved to the other transcript
    // are in fact ALL of the exons left in the parent gene, then first ask if
    // user is prepared to merge the genes.

    // First count how many exons are being moved to trans--count only those
    // that are not already in trans.
    int exon_count = 0;
    Transcript sourceTrans = null;
    for (int i = 0; i < features.size(); i++) {
      if (features.elementAt(i) instanceof ExonI) {
        ExonI exon = (ExonI)features.elementAt(i);
        if (exon.getRefFeature() != tgtTrans) {
          ++exon_count;
          if (sourceTrans == null)
            sourceTrans = (Transcript) exon.getRefFeature();
        }
      }
    }
    AnnotatedFeatureI sourceGene = (AnnotatedFeatureI) sourceTrans.getRefFeature();
    
    /* If all exons of sourceTrans were selected, and there's
       only one transcript, then moving the exons would make the genes
       want to merge. */
    if (exon_count == sourceTrans.getFeatures().size()
        && ((sourceGene.getFeatures().size() == 1) ||
            (tgtTrans.getGene().getFeatures().size() == 1))) {
      if (!askMergeDialog(tgtTrans.getGene().getName() + " and " +
                          sourceGene.getName() +
                          "\nIf you move the selected exons, " +
                          "these genes will merge"))
        return; // if the user doesn want to merge -> return
      // if they do want to merge should we bring up merge name dialog to figure
      // what is to and whats from? (instead of in mergeGeneIdent?)
    }

    logger.info ("****** MOVING EXONS TO TRANSCRIPT ******");
    CompoundTransaction compTrans = new CompoundTransaction(this);

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    generateFeatChangeData(topLevelFeats, getTopLevelFeat(tgtTrans));

    for (int i = 0; i < features.size(); i++) {
      if (features.elementAt(i) instanceof ExonI) {
        ExonI exon = (ExonI)features.elementAt(i);
        AnnotatedFeatureI tgtGene = tgtTrans.getGene();

        if (exon.getRefFeature() != tgtTrans) {
          SeqFeatureI topLevelFeat = getTopLevelFeat(exon);
          if (!topLevelFeats.containsKey(topLevelFeat.hashCode())) {
            generateFeatChangeData(topLevelFeats, topLevelFeat);
          }
          Transcript srcTrans = (Transcript) exon.getRefFeature();
          AnnotatedFeatureI srcGene = srcTrans.getGene();

          // update exon parent - dont delete
          // moves exon to new transcript, returns comp trans of move
          CompoundTransaction ct = moveSingleExonToTranscript(exon,tgtTrans);
          compTrans.addTransaction(ct);
          //tgtTrans.addExon(exon);

          // TRANSCRIPT MERGE
          //else -- this else was wrong - copyTranscriptData needs to happen with
          // gene merges & trans merges. mergeGeneIds DOES NOT copy trans data
          // make syn of old trans, copy syns,dbxrefs,comms,desc
          // so this needs to happen whether or not genes are merging!
          if (srcTrans.size() == 0) {
            copyTransProtData(srcTrans, tgtTrans); // transactions???
          }

          // GENE MERGE - there are 2 ways gene merges have to be checked for
          // if we just blew away a gene by taking away all of its exons(here). 
          // 2nd is consolidateGenes below that wont catch this case but
          // will catch genes that still exist but have been bridged.
          if (srcGene.size() == 0) {
            CompoundTransaction mergeIdTrans = mergeGeneIdentifiers(tgtGene,srcGene);
            // move merge type from mergeIdTrans up to compTrans where it belongs
            // eventually mergeIdTrans will be typeless...
            mergeIdTrans.setCompoundType(CompoundTransaction.NO_TYPE);
            compTrans.setCompoundType(CompoundTransaction.MERGE);
            compTrans.addTransaction(mergeIdTrans);
            // Update transcript parent
          }

       } // end of if exon ref != trans

      } // end of if instance of ExonI
    } // end of for loop on vector of features

    // shouldnt this stuff be done before event?
    // shouldt this also be done on src trans
    tgtTrans.setTranslationEndFromStart();
    /* Invalidate peptide sequence because it will need to be recalculated
       if anyone asks for it later.  (Don't bother recaculating right now--
       might be a waste of time.) */
    tgtTrans.setPeptideValidity(false);

    setOwnerAndDate(tgtTrans);

    // Ask before merging genes in this region. This will catch all merges not
    // caught above (and wont catch ones above - need both). This catches merges of
    // genes that got bridged but havent had all their exons taken.
    CompoundTransaction merge = consolidateGenes(tgtTrans.getGene(), true);
    if (merge != null) {
      // move merge type to compTrans where it belongs from merge trans
      compTrans.setCompoundType(CompoundTransaction.MERGE);
      merge.setCompoundType(CompoundTransaction.NO_TYPE);
      compTrans.addTransaction(merge);
    }
    //fireCompoundTransaction(compTrans);
    generateEditTransaction(topLevelFeats);
  }

  /** Removes exon from current transcript and adds to tgtTrans. Adds update exon
      parent annot change event to coalsecer. Purges transcript if no exons left
      This is used by moveExonsToTranscript and splitTranscript 
      If exons overlap merges them 
      work in progress - compound transaction replacing coalescer
      compound transaction not actually used yet - getting there
  */
  private CompoundTransaction moveSingleExonToTranscript(ExonI exon,
                                                         Transcript tgtTrans) {
    CompoundTransaction compoundTransaction = new CompoundTransaction(this);
    Transcript srcTrans = exon.getTranscript();
    srcTrans.deleteExon(exon);

    // mergeExons(tgtTrans,overlappingExons); -- have to do 2nd
    // have to do merge before addExon as addExon does its own merge(w/ no event)
    //tgtTrans.addExon(exon);  -- cant do this
    // so before we do the merge we need to add the exon to the trans, otherwise
    // mergeExons might falsely think that the transcript is empty and purge it
    // but we cant call addExon as that does the merging without firing events
    // so we call addFeature (no merging)
    // add exon even if its an overlapper thats gonna get merged below
    // if this transcript moves to a new gene then both the trans & exon need to
    // get new names/ids - is this handled somewhere?
    // have to do overlap search before we add exon to new trans (avoid self overlap)
    FeatureList overlappingExons = getOverlappingExons(tgtTrans,exon);
    tgtTrans.addFeature(exon,!tgtTrans.isTransSpliced());

    // if overlap - get exons
    //     Vector overlappingExons = new Vector();
    //     for (int i=0; i < tgtTrans.size(); i++) {
    //       if (tgtTrans.getFeatureAt(i).overlaps(exon)) {
    //         overlappingExons.addElement(tgtTrans.getFeatureAt(i));
    //       }
    //     }
    // this is wrong - have to check for overlap before addFeat - otherwise a move
    // of just one exon will find an overlap with itself - which is silly
    //FeatureList overlappingExons = getOverlappingExons(tgtTrans,exon);

    // hafta update the parent before the merging as the merging refers to 
    // to the transcripts its moved to
    UpdateParentTransaction upt = new UpdateParentTransaction(this,exon,srcTrans,tgtTrans);
    //     TransactionSubpart ts = TransactionSubpart.PARENT;
    //     AnnotationUpdateEvent aue = new AnnotationUpdateEvent(this,exon,ts);
    //     aue.setOldParent(srcTrans);
    //     coalescer.addChange(aue);
    //     UpdateTransaction ut = new UpdateTransaction(exon,ts,srcTrans,tgtTrans);
    compoundTransaction.addTransaction(upt);

    if (overlappingExons.size() > 0) {
      // add exon to overlappers. first exon gets updated range, rest get purged
      //overlappingExons.add(0,exon); // no longer needed - included in overlapExons
      // adds limits and delete exon events to coalescer(doesnt fire)
      CompoundTransaction ct = mergeExons(tgtTrans,overlappingExons, false); 
      compoundTransaction.addTransaction(ct);
      //coalescer.addTransaction(ct); // temporary
    }

    //     coalescer.addChange(aue); // moved this to above

    if (srcTrans.size() == 0) { // purges gene if last trans
      CompoundTransaction ct = purgeTranscript(srcTrans.getGene(),srcTrans);
      compoundTransaction.addTransaction(ct);
      // move identifiers! mergeTranscripts? 
      // done in calling method but could be done here
    }
    else {
      setOwnerAndDate(srcTrans);
    }
    compoundTransaction.setSource(this);
    return compoundTransaction;
  }

  /** set owner and date on annot feat (transcript or one-level annot) */
  private void setOwnerAndDate(AnnotatedFeatureI annFeat) {
    annFeat.setOwner(getUserName());
    annFeat.replaceProperty("date",new Date().toString());
  }

  /** If from trans doesnt have a temp name add it as synonym to to trans.
   *  Copy synonyms,xrefs and comments from from to to(copyAnnotData()) and
   *  adds from_trans name as synonym in to_trans(if not a temp name)  
   *  producing comp transactions, but its not being used */
  private CompoundTransaction copyTransProtData (Transcript from_trans,
                                                 Transcript to_trans) {
    logger.debug("copyTransProtData copying data from " + from_trans + " to " + to_trans);
    CompoundTransaction compTrans = new CompoundTransaction(this);
    // this should be done in name adapter - this is fly specific
    AddTransaction at = addSynonym(to_trans, from_trans.getName(), from_trans.getOwner());
    compTrans.addTransaction(at);
    CompoundTransaction ct = copyAnnotData(from_trans, to_trans);
    compTrans.addTransaction(ct);
    if (from_trans.hasProteinFeat() && to_trans.hasProteinFeat()) {
      ct = copyAnnotData(from_trans.getProteinFeat(),to_trans.getProteinFeat());
      compTrans.addTransaction(ct);
    }

    // protein xrefs - done with copyAnnotData above
    //ct = copyDbXrefs(from_trans.getProteinFeat(),to_trans.getProteinFeat());
    //compTrans.addTransaction(ct);

    return compTrans;
  }

  // not currently used
//   private boolean nameInUseElseWhere(AnnotatedFeatureI gene) {
//     StrandedFeatureSetI annots = curation.getAnnots();
//     Vector check_list = (gene.getStrand() == 1 ?
//                          annots.getForwardSet().getFeatures() :
//                          annots.getReverseSet().getFeatures());
//     boolean in_use = false;
//     for (int i = 0; i < check_list.size() && !in_use; i++) {
//       SeqFeatureI sf = (SeqFeatureI) check_list.elementAt(i);
//       in_use =(sf != gene && sf.getName().equals(gene.getName()));
//     }
//     return in_use;
//   }

  /** Ask user to choose gene name of to_gene, from_gene, or new temp name from 
   *  name adapter. Returns compound transaction of all trans */
  private CompoundTransaction mergeGeneIdentifiers (AnnotatedFeatureI to_gene,
                                                    AnnotatedFeatureI from_gene) {
    
    CompoundTransaction compTrans = new CompoundTransaction(this);

    ApolloNameAdapterI nameAdapter = curationState.getNameAdapter(to_gene);
    String userChosenGeneName = null;
    
    String toName = to_gene.getName();
    String fromName = from_gene.getName();
    String newGeneratedName = nameAdapter.generateNewId(curation.getAnnots(),
							curation.getName(),to_gene);

    // if both to and from genes are temps (new) no need to ask user for which one
    // since both temps its inconsequential.
    
    //Rule #2: If merging a temp name and an existing name, then don't prompt the user 
    
    if (isTemp(to_gene) && isTemp(from_gene)){
      userChosenGeneName = toName;
    } else if (!isTemp(to_gene) && isTemp(from_gene)){
      userChosenGeneName = toName;
    } else if (isTemp(to_gene) && !isTemp(from_gene)){
      /*that's not good.
      In this case, this method callers should have sorted transcript in order 
      to put the existing gene as the to_gene, not the contrary*/
      //A bit brutal (should we warn the user with a swing dialog that his transaction is likely to be broken?)
      logger.error("Error in AnnotationEditor.mergeGeneIdentifier, trying to add an existing gene's exons to a temp gene");
      return null;
    }
    else
      userChosenGeneName = askMergeName(toName,fromName,newGeneratedName);

    logger.info ("User chose name " + userChosenGeneName +
                 " when merging " + toName +
                 " with " + fromName);

    // The name chosen was that of the TO_GENE, which has already
    // had all the FROM_GENE's transcripts moved to it.
    // We therefore don't need to change the name of the TO_GENE, but
    // we need to change its transcript and peptide names.
    if (userChosenGeneName.equals(toName)) {
      String fromId = from_gene.getId();
      addIdAndNameSynonyms(to_gene,fromId,fromName);
      //       setTransNameFromGene(to_gene);  // this happens later anyway
      // didn't help
      //       UpdateTransaction idUpdateTrans = setID(to_gene, to_gene.getId());
      //      compTrans.addTransaction(idUpdateTrans);
      // didn't help
      //      UpdateTransaction nameTrans = makeNameTrans(to_gene,userChosenGeneName); 
      //      to_gene.setName(userChosenGeneName);
    }

    // GENE NAME NOT TO_GENE NAME, set its name, etc...
    // in the case that the 2nd name is picked its renaming the 1st rather than using
    // the 2nd feature - this is problematic for object models that are mapped to the
    // features like tair - this should be ammended
    // this is non-trivial as the caller (eg mergeExToTrans) has already moved all
    // of from_genes exons to to_gene - the sequence of events is funny as we ask about
    // gene naming after we've already moved the exons over - not sure what best
    // way to ammend this is - tairs workaround with moving object id over isnt so bad
    // in light of this - any suggestions from tair? maybe we could figure ahead of
    // time if genes are merging and ask user for name choice and set up to & from 
    // appropriately in moveExToTrans?
    // User has either chosen the "From" gene's name or a new temp name.
    else if (!userChosenGeneName.equals(toName)) {
      String prev_name = to_gene.getName();
      String prev_id = to_gene.getId();
      // do before name change (?)
      UpdateTransaction nameTrans = makeNameTrans(to_gene,userChosenGeneName); 
      // does this need to happen here??
      to_gene.setName(userChosenGeneName);

      UpdateTransaction idUpdateTrans = null;
      CompoundTransaction synTrans = null;
    
      // CHOSEN GENE NAME IS FROM_GENE NAME (not a new temporary name)
      if (userChosenGeneName.equals(fromName)) {
        // Add previous id as syn - only if its not a temporary name
        synTrans = addIdAndNameSynonyms(to_gene,prev_id,prev_name);

        String newId = from_gene.getId();
        idUpdateTrans = setID(to_gene,newId);
      } // end of if user gene name is from_gene name 


      // GENE NAME IS NEW TEMP (&& temp name is an ID (eg CG..., not a user name))
      // shouldnt this just be a test for isTemp NOT isID?
      // well this is questionable - askMergeName 3rd option is from NA.generateId
      // will that always be a temp id - depends on name adapter doesnt it?
      else if (userChosenGeneName.equals(newGeneratedName)) {
        //(nameAdapter.nameIsId(to_gene)) {
        // this means that both names were symbols, so
        // we have reverted to using an arbitrary CG (arbitrary??)
        synTrans = addIdAndNameSynonyms(to_gene,prev_id,prev_name);
        
        String fromId = from_gene.getId();
        //String fromName = from_gene.getName();
        synTrans.addTransaction(addIdAndNameSynonyms(to_gene,fromId,fromName));
        idUpdateTrans = setID(to_gene,userChosenGeneName);
      }

      // this case means THINGS ARE CRAZY, really shouldn't get here
      // actually its not just crazy its a bug
      else {
        String id = nameAdapter.generateId(curation.getAnnots(),
                                           curation.getName(),
                                           to_gene);
        idUpdateTrans = setID(to_gene,id);
        logger.error("name is neither generated nor from gene in mergeGeneIdentifiers");
      }

      // order events properly - id first, name second, syns 3rd
      // (the problem with this is that it doesn't reflect the order in which the operations
      //  actually occurred - see cleanup below)
      compTrans.addTransaction(idUpdateTrans);
      // ensure that name transaction is using the *new* id (otherwise any id-based lookup will fail)
      if (Config.getSaveClonedFeaturesInTransactions()) {
	SeqFeatureI sf = nameTrans.getSeqFeature();
	SeqFeatureI sfClone = nameTrans.getSeqFeatureClone();
	sfClone.setId(sf.getId());
      }
      compTrans.addTransaction(nameTrans);

      // THIS IS WRONG - THIS NEEDS TO HAPPEN IN ALL CASES NOT JUST WHEN
      // 1ST NAME IS NOT CHOSEN!
      //setTransNameFromGene (to_gene); // ignore events created - dont need for now
      compTrans.addTransaction(synTrans);
    } // end of if user chosen gene name != to_gene name
    
    // wait - what about trans id?? - actually trans id gets set to
    // this is not quite right - id and name changes should happen separately
    // in case id and name are different
    // supress events from trans name change?
    // true means force ID to get set as well--if to_gene was the chosen name,
    // the transcript *name* will be ok, but the ID will need to be reset.

    // JC: previously this transaction was ignored.  I'd argue that if a qualifying
    // event occurs then a transaction should be generated.  If a particular data
    // adapter wants to ignore certain transactions, then that's a policy the adapter
    // can implement, not something we should be enforcing in the AnnotationEditor.
    CompoundTransaction tnt = setTransNameFromGene(to_gene, true);
    compTrans.addTransaction(tnt);

    compTrans.addTransaction(copyAnnotData(from_gene, to_gene));
    return compTrans;
  }

  /** Add old id and name as syns to annot if they are not new. helper function for
      mergeGeneIdentifiers */
  private CompoundTransaction addIdAndNameSynonyms(AnnotatedFeatureI annot,
						   String oldId, String oldName) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    if (!isTemp(oldId,annot)) {
      // Harvard wants the old name added as a synonym, but not the old uniquename (id)
      //      AddTransaction at = addSynonym(annot,oldId, getUserName()); // makes tran, adds syn
      // compTrans.addTransaction(at);
      if (!oldId.equals(oldName)) 
        compTrans.addTransaction(addSynonym(annot,oldName, getUserName())); // makes trans, adds syn
    }
    return compTrans;
  }

  /** just tests for presence of "temp" in string - this should be a name adapter
   *  method - different adapters may do temping differently */
  private boolean isTemp(String idOrName,AnnotatedFeatureI feat) {
    return getNameAdapter(feat).isTemp(idOrName);
    //return idOrName.indexOf("temp") >= 0;
  }

  private boolean isTemp(AnnotatedFeatureI feat) {
    return isTemp(feat.getName(),feat);
  }

  private ApolloNameAdapterI getNameAdapter(AnnotatedFeatureI feat) {
    return curationState.getNameAdapter(feat);
  }

  /** tests for default name - has "feature_set" in string. this is the case if
      the feature is brand new and hasnt even be assigned a temp id yet - at least
      thats my understanding 
      - i think this was for old buggy data and can now be taken out */
  //   private boolean isBrandNew(String idOrName) {
  //     return idOrName.indexOf("feature_set") >= 0;
  //   }
   
  /** Done before name change happens so annot still has old name */
  private UpdateTransaction makeNameTrans(AnnotatedFeatureI annot,String newName) {
    if (annot.getName() == null || annot.getName() == RangeI.NO_NAME)
      return null;
    TransactionSubpart ts = TransactionSubpart.NAME;
    //AnnotationUpdateEvent aue = new AnnotationUpdateEvent(this,annot,ts);
    UpdateTransaction ut = new UpdateTransaction(annot,ts,annot.getName(),newName);
    //aue.setOldString(annot.getName());
    return ut;
  }

  /** Synonyms are now not just strings--if we get a string, make a new synonym for it. 
   *  Add synonym owner, too. */
  private AddTransaction addSynonym(AnnotatedFeatureI annot, String syn, String owner) {
    return addSynonym(annot, new Synonym(syn, owner));
  }

  /** Adds synonym to annot and makes and returns add syn event */
  private AddTransaction addSynonym(AnnotatedFeatureI annot, Synonym syn) {
    // if annot already has syn dont add
    if (annot.hasSynonym(syn.getName()))
      return null;

    // check for temp syns?
    if (isTemp(syn.getName(),annot))
      return null;
    
    //annot.addSynonym(syn);
    TransactionSubpart ts = TransactionSubpart.SYNONYM;
    //AnnotationAddEvent aae = new AnnotationAddEvent(this,annot,ts);
    AddTransaction at = new AddTransaction(annot,ts,syn);
    at.editModel(); // adds synonym
    return at; //aae;
  }

  private String askMergeName(String geneName1, String geneName2, String generatedName) {
    if (geneName1.equals(geneName2))
      return geneName1;

    Object[] options = { geneName1,geneName2,generatedName };
    //nameAdapter.generateId(curation.getAnnots(),curation.getName(),to_gene) };

    String message = "Please choose a name for the gene resulting from the merge\nof " + geneName1 + " and " + geneName2 + ":";
    //    System.out.println(message);
    JOptionPane pane =
      new JOptionPane(message,
                      JOptionPane.DEFAULT_OPTION,
                      JOptionPane.OK_CANCEL_OPTION,
                      null, // icon
                      options, // all choices
                      options[0]); // initial value
    JDialog dialog = pane.createDialog(null,
                                       "Please choose one");
    dialog.show();

    Object result = pane.getValue();
    // Too late to back out of the merge now--just pick one for them
    if (result == null)
      return geneName1;

    return (String)result;
  }

  /** Copies over synonyms, dbxrefs, and comments - this needs to fire events?
   *  or be implicit in a merge event? 
   *  This also sets to's description to froms description - if to has a null 
   *  description. This is used for both genes and transcripts */
  private CompoundTransaction copyAnnotData(AnnotatedFeatureI from_fs,
                                            AnnotatedFeatureI to_fs) {
    //ChangeList changes = createChangeList();
    CompoundTransaction compoundTrans = new CompoundTransaction(this);

    Vector syns = from_fs.getSynonyms();
    for ( int i = 0; i < syns.size(); i++) {
      Synonym syn = (Synonym) syns.elementAt (i);
      compoundTrans.addTransaction(addSynonym(to_fs,syn));
    }
    compoundTrans.addTransaction(copyDbXrefs(from_fs,to_fs));

    Vector comments = from_fs.getComments();
    for ( int i = 0; i < comments.size(); i++) {
      Comment comm = (Comment) comments.elementAt (i);
      compoundTrans.addTransaction(addComment(to_fs,comm));
    }
    // do annotations ever have descriptions?
    compoundTrans.addTransaction(setDescription(to_fs,from_fs.getDescription()));

    return compoundTrans;
  }

  private AddTransaction addComment(AnnotatedFeatureI ann, Comment cm) {
    ann.addComment(cm); // not in subpart yet
    TransactionSubpart ts = TransactionSubpart.COMMENT;
    int subpartRank = ann.getComments().size()-1;
    AddTransaction at = new AddTransaction(ann,ts,cm,subpartRank);
    return at;//aae;
  }

  private CompoundTransaction copyDbXrefs(SeqFeatureI from, SeqFeatureI to) {
    CompoundTransaction compoundTrans = new CompoundTransaction(this);
    for (int i = 0; i < from.getDbXrefs().size(); i++) {
      compoundTrans.addTransaction(addDbXref(to,from.getDbXref(i),true));
    }
    return compoundTrans;
  }

  /**
   * @param ann                Add the DbXref to this annotation.
   * @param dbxref             The DbXref to add to <code>ann</code>
   * @param convertToSecondary Whether to convert any primary dbxref to a secondary dbxref.
   * @return An AddTransaction with getNewSubpartValue.isDbXref() == true
   */
  private AddTransaction addDbXref(SeqFeatureI ann, DbXref dbxref, boolean convertToSecondary) {
    // For Chado, is_current must be set to 0 in the merged dbxrefs
    dbxref.setCurrent(0);
    if (convertToSecondary && dbxref.isPrimary()) {
      dbxref.setIsPrimary(false);
      dbxref.setIsSecondary(true);
    }
    ann.addDbXref(dbxref); // not done by subpart yet
    TransactionSubpart ts = TransactionSubpart.DBXREF;
    int subpartRank = ann.getDbXrefs().size()-1;
    AddTransaction at = new AddTransaction(ann,ts,dbxref,subpartRank);
    return at;
  }

  /** Returns null if already have description or desc is null - do annots ever 
      have descriptions? ive never seen it */
  private UpdateTransaction setDescription(AnnotatedFeatureI ann, String desc) {
    if (ann.getDescription() != null || desc == null)
      return null;
    String oldDesc = ann.getDescription();
    ann.setDescription(desc);
    //return new AnnotationUpdateEvent(this,ann,TransactionSubpart.DESCRIPTION);
    return new UpdateTransaction(ann,TransactionSubpart.DESCRIPTION,oldDesc,desc);
  }

  /**
   * This method tidies up after the edit. It warns for multiple use of the
   * the same set of SelectionSets, executes the changes through the
   * {@link AnnotationChangeCoalescer#executeChanges()} method, and sets the
   * current SelectionSets to used.
   */
  private void endEdit(ChangeList changer,
                       boolean done,
                       SeqFeatureI annot,
                       int change_type,
                       boolean updateSelection) {
    endEdit(changer, done, annot, change_type);
    if (updateSelection) {
      /* any delete events fired above will have been handled by the
         selection manager which will have updated the current
         selection to remove what has been deleted, but nothing
         else - SelectionManager cant fire selection event on annot change event
         as other listeners may not have gotten the annot change event yet - yikes! */
      FeatureSelectionEvent fse
        = new FeatureSelectionEvent(annotationView,
                                    getSelectionManager().getSelection(),
                                    true);
      getController().handleFeatureSelectionEvent(fse);
    }
  }

  private void printTree(SeqFeatureI annot, int depth)
  {
    for (int i = 0; i < depth; ++i) {
      System.out.print("\t");
    }
    System.out.println(annot.getName() + "\t" + annot);
    for (Object o : annot.getFeatures()) {
      printTree((SeqFeatureI)o, depth + 1);
    }
  }
  
  /**
   * This method tidies up after the edit. It warns for multiple use of the
   * the same set of SelectionSets, executes the changes through the
   * {@link AnnotationChangeCoalescer#executeChanges()} method, and sets the
   * current SelectionSets to used.
   select vector v, called by moveExonsToTranscript to select moved exons in new
   transcript. this is bad - this is gui stuff (stuff to select) in model/editor land
   * 12/05 Not currently used
   */
//   private void endEdit(ChangeList changer,
//                        boolean done,
//                        SeqFeatureI annot,
//                        int change_type,
//                        Vector v, TierViewI view) {
//     endEdit(changer, done, annot, change_type);
//     selectFeatures (v, view);
//   }

  private void endEdit(ChangeList changer) {
    AnnotSessionDoneEvent e = new AnnotSessionDoneEvent(this);
    changer.addChange(e);
    changer.executeChanges();
  }

  /** Presently every call to endEdit is with done=true, so it doesnt seem necasary
      as a parameter. if done is true calls _setUsed */
  private void endEdit(ChangeList changer,
                       boolean done,
                       SeqFeatureI annot,
                       int change_type) {
    //     AnnotationChangeEvent ace
    //       = new AnnotationChangeEvent(this,
    //                                   annot,
    //                                   change_type,
    //                                   AnnotationChangeEvent.ANNOTATION,
    //                                   annot,
    //                                   null);
    //     AnnotationChangeEvent ace = new AnnotSessionDoneEvent(this);
    //     changer.addChange(ace);
    //     changer.executeChanges();
    endEdit(changer);
    //coalescer       = null;
    //editDescription = null;

    if (done && annotSet != null && evidenceSet != null)
      _setUsed(done);

  }

  /** True if can merge exons */
  public boolean mergeExonsAllowed() {
    if (annotSet.getTranscripts().size() != 1) return false;
    if (annotSet.getLeafFeatures().size() <= 1) return false;
    
    for(int i=0; i<annotSet.getTranscripts().size(); i++)
      if(!goodOwner(annotSet.getTranscript(i).getOwner()))
        return false;
    // should there be a gene check?
    return true;
  }

  /**
   * The default mergeExons method. This method expects there to be exactly
   * one transcript in the annotSet, with multiple exons from this selected
   * which are in the viewSet.
   */
  public void mergeExons() {
    switch (annotSet.getTranscripts().size()) {
    case 0:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected NO transcript to merge features in");
      break;
    case 1:
      CompoundTransaction ct = 
        mergeExons(annotSet.getTranscript(0),annotSet.getLeafFeatList(), true);
      //fireCompoundTransaction(ct);
      break;
    default:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected more than 1 transcript to merge features in");
      break;
    }
  }


  //  private void mergeExons(Transcript tran, FeatureList mergingExons) {
  //ChangeList changeList = beginEdit("merge features");
  //    CompoundTransaction ct = mergeExons(tran,mergingExons,changeList);
  //    ct.setSource(this);
  //endEdit(changeList, true, tran.getGene(), FeatureChangeEvent.REDRAW,
  // important to refresh selection with changed exon
  //        null, annotationView); // take out at some point
  //  }

  /**
   * Merges features within a transcript. This is the version which actually
   * does the work.
   * @param tran The transcript to merge exons in.
   * @param features The Vector of features to be merged (should be exons from
   *                 tran).
   * I believe this merges exons in Vector features, as well as any unspecified
   * exons in between the specified exons in features
   * This updates the range of the first exon and deletes the rest of the exons
   * I think merge exons only happens on exons in same transcript so its impossible
   * to purge a transcript here. Should we take out purge trans check?
   * The 1st exon in features gets its range changed, the rest are purged.
   * Adds changes to coalescer - does not fire them/execute.
   * CompoundTrans should replace coalescer - ease out coalescer
  */
  private CompoundTransaction mergeExons(Transcript tran, FeatureList exons,
      boolean executeTransaction) { //, 
    //                          ChangeList coalescer) {
    checkIfBeyondRange();

    CompoundTransaction compoundTransaction = new CompoundTransaction(this);

    logger.info ("****** MERGING EXONS IN TRANSCRIPT ******");
    int                   min        =  1000000000;
    int                   max        = -1000000000;
    ExonI                  sourceExon = null;
    Vector deleted_trans = new Vector();
    Vector deleted_genes = new Vector();

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    
    for (int i=0; i < exons.size(); i++) {
      SeqFeatureI sf = exons.getFeature(i);

      if (sf instanceof ExonI) {
        SeqFeatureI topLevelFeat = getTopLevelFeat(sf);
        if (topLevelFeat != null) {
          if (!topLevelFeats.containsKey(topLevelFeat.hashCode())) {
            generateFeatChangeData(topLevelFeats, topLevelFeat);
          }
        }
        
        ExonI exon = (ExonI)sf;

        if (i == 0) {
          sourceExon = exon;
        } else {
          CompoundTransaction ct = purgeExon(exon);
          compoundTransaction.addTransaction(ct);
          // is it possible for from_tran top be different than tran?
          // perhaps in theory but i dont think this method is currently used that way
          // dont think this is necasary as merge exon cant del trans or gene
          Transcript from_tran = (Transcript) exon.getRefFeature();
          AnnotatedFeatureI from_gene
            = (AnnotatedFeatureI) from_tran.getRefFeature();
          if (from_tran.getExons().size() == 0) {
            deleted_trans.addElement (from_tran);
          }
          if (from_gene != null && from_gene.size() == 0) {
            deleted_genes.addElement (from_gene);
          }
        }
        if (exon.getLow()  < min) {
          min = exon.getLow();
        }
        if (exon.getHigh() > max) {
          max = exon.getHigh();
        }
      }
    }

    // only continue if we have a source exon (is it possible to not?)
    // sourceExon is the one exon thats not deleted but range updated
    if (sourceExon != null) {
      // only make limits tnx if limits on exon have actually changed
      if (sourceExon.getLow() != min || sourceExon.getHigh() != max) {
        TransactionSubpart ts = TransactionSubpart.LIMITS;
        RangeI oldRange = sourceExon.getRangeClone();
        sourceExon.setLow(min);
        sourceExon.setHigh(max);
        SeqFeatureI gene = getTopLevelFeat(sourceExon);
        if (gene != null) {
          setExonID(sourceExon, gene.getId(), false);
        }
        // sourceExon is the new range/value
        UpdateTransaction ut = new UpdateTransaction(sourceExon,ts,oldRange,sourceExon);
        compoundTransaction.addTransaction(ut);
      }

      // Look for exons which have been surrounded and remove them.
      for (int i=0; i<tran.size(); i++) {
        ExonI exon = tran.getExonAt(i);
        if (exon.overlaps(sourceExon) && sourceExon != exon) {
          // Message doesnt seem necasary
          CompoundTransaction ct = purgeExon(exon);
          compoundTransaction.addTransaction(ct);
          Transcript from_tran = (Transcript) exon.getRefFeature();
          AnnotatedFeatureI from_gene
            = (AnnotatedFeatureI) from_tran.getRefFeature();
          if (from_tran.getExons().size() == 0) {
            deleted_trans.addElement (from_tran);
          }
          //this actually isnt possible. merge exons are always within same trans
          // comment out?
          if (from_gene.size() == 0) {
            deleted_genes.addElement (from_gene);
          }
          i--;
        }
      }
      // I dont think this can actually happen - exons are from same transcripts
      // therefore at least one exon should be left. 
      // check if anything makes this happen - if so need transactions!
      for (int i = 0; i < deleted_trans.size(); i++) {
        copyTransProtData ((Transcript)deleted_trans.elementAt(i),tran);
      }
      // this wont happen - exons from same gene/trans
      for (int i = 0; i < deleted_genes.size(); i++) {
        mergeGeneIdentifiers (tran.getGene(),
                              (AnnotatedFeatureI) deleted_genes.elementAt(i));
      }

      setGeneEnds(tran);

    }
    
    if (executeTransaction) {
      generateEditTransaction(topLevelFeats);
    }
    
    // checks for range changes - shouldnt be any but doesnt hurt i guess
    // caller fires
    //fireCompoundTransaction(compoundTransaction);
    if (!compoundTransaction.hasKids())
      return null; // nothing happened
    return compoundTransaction;
  }

  /**
   * Determines if two exons are adjacent to one another in the transcript.
   * @param trans        The transcript to check in
   * @param exonFeatures Two exons from trans in a Vector
   */
  protected boolean checkAdjacent(Transcript trans,
                                  Vector     exonFeatures) {

    if (exonFeatures.size() != 2) {
      logger.error("checkAdjacent called with wrong number of features");
      return false;
    }

    int ind0 = trans.getFeatureIndex((SeqFeatureI)exonFeatures.elementAt(0));
    int ind1 = trans.getFeatureIndex((SeqFeatureI)exonFeatures.elementAt(1));

    if (ind0 == ind1+1 || ind1 == ind0+1) {
      return true;
    }
    return false;
  }

  /**
   * Determine if the current SelectionSets are compatible with splitting a
   * transcript. Not actually used by splitTranscript().
   */
  public boolean splitTranscriptAllowed() {
    if (annotSet.getTranscripts().size() == 1 &&
        annotSet.getLeafFeatures().size() == 2) {
      if(!goodOwner(annotSet.getTranscript(0).getOwner()))
        return false;
      return checkAdjacent(annotSet.getTranscript(0),
                           annotSet.getLeafFeatures());
    }
    return false;
  }

  /**
   * The default splitTranscript method. This method expects exactly two exons
   * from a single transcript to be in the viewSet, and for them to be adjacent.
   */
  public void splitTranscript() {
    if (annotSet.getTranscripts().size() != 1) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Can only split a single transcript");
      return;
    }
    if (annotSet.getLeafFeatures().size() != 2 ||
        !checkAdjacent(annotSet.getTranscript(0),annotSet.getLeafFeatures())) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Must select exactly two adjacent exons");
      return;
    }
    splitTranscript(annotSet.getTranscript(0),
                    (ExonI)annotSet.getLeafFeat(0),
                    (ExonI)annotSet.getLeafFeat(1));
  }

  /**
   * Splits a transcript into two.
   * @param trans The transcript to split.
   * @param exon1 One of the exons to split between.
   * @param exon2 The another exon to split between.
   */
  private void splitTranscript(Transcript trans,
                               ExonI exon1,
                               ExonI exon2) {

    AnnotatedFeatureI gene = trans.getGene();

    // Ask before splitting genes in this region
    if (!userReallyWantsToSplit(gene))
      return;

    checkIfBeyondRange();

    logger.info ("****** SPLITTING TRANSCRIPT ******");
    //AnnotationChangeCoalescer changer  = beginEdit("split transcript");

    // SplitTransaction splitTransaction = new SplitTransaction();
    //CompoundTransaction compoundTransaction = new CompoundTransaction();
    //compoundTransaction.setCompoundType(CompoundTransaction.SPLIT);

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    if (!topLevelFeats.containsKey(gene.hashCode())) {
      generateFeatChangeData(topLevelFeats, gene);
    }

    
    int ind1 = trans.getFeatureIndex(exon1);
    int ind2 = trans.getFeatureIndex(exon2);

    if (ind1 == -1 || ind2 == -1) {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "split transcript with non transcript exons impossible");
      return;
    }

    int splitind = ind1;
    if (ind1 > ind2) {
      splitind = ind2;
    }

    setOwnerAndDate(trans);

    Transcript newTrans = new Transcript();
    newTrans.setStrand (trans.getStrand());

    // fire add transcript event???
    // JC: Yes, there's a problem here, which is that we're creating a new Transcript 
    // without using an AddTransaction.  Later on in the split gene/transcript process 
    // we may fire an UpdateParentTransaction for this Transcript, which may confuse 
    // any data adapter trying to track changes by looking solely at the transactions.  
    // The current workaround (for the PureJDBCTransactionWriter) is to ignore the first 
    // half (the delete) of any UpdateParentTransaction that references a nonexistent 
    // relationship.

    // The moving of exon events have to happen after creating of trans/gene
    // otherwise they'd have nothing to move to
    int origSize = trans.size();
    ChangeList exonChanges = createCoalescer();
    //CompoundTransaction exonCompTrans = new CompoundTransaction(this);
    for (int i = splitind+1; i < origSize; i++) {
      ExonI  exon = trans.getExonAt(splitind+1);
      // this aint right - needs to move exons to new transcript - moveExons ->
      // update exon parent
      //purgeExon(exon,changer);
      //newTrans.addExon(exon);
      // this adds update parent events to changer
      CompoundTransaction ct = moveSingleExonToTranscript(exon,newTrans);
      //exonCompTrans.addTransaction(ct);
      exonChanges.addChange(ct.generateAnnotationChangeEvent());
    }

    //    AnnotationChangeEvent ace;
    //AnnotatedFeatureI newTransGene = gene;
    gene.adjustEdges(); 

    gene.addFeature(newTrans); // needed for merge/split analysis

    // copy synonyms,xrefs, and comments from old trans to new - also trans name
    //  becomes synonym in newTrans
    copyTransProtData(trans,newTrans); // transaction/events???
    setOwnerAndDate(newTrans);
    //     setTransNameFromGene (newTransGene); -> below

    // i imagine this has to be done before merge/split analysis as CDS affects it
    trans.setTranslationEndFromStart();
    if (gene.isProteinCodingGene())
      newTrans.calcTranslationStartForLongestPeptide();

    // The adding of the transcript/gene event has to happen BEFORE the move exon
    // events added above or else the db will be trying to move exons to a non-existent
    // transcript. The exon moves have to happen before figuring overlap so its 
    // circular problem, thus the inserting of the transcript event before the exon
    // events.
    //changer.insertChange(0,ace);
    //changer.executeChanges(); // no need to execute changes here - do below

    ChangeList changeList = createCoalescer();

    // look for merges
    //ChangeList mergeChanges = createCoalescer();
    //consolidateGenes(newTransGene, mergeChanges, true);
    // are there really any merges on splits? would this be due to orf changes?
    CompoundTransaction merge = consolidateGenes(gene, true);
    if (merge != null)
      changeList.addChange(merge.generateAnnotationChangeEvent());

    // This may create new gene - this will cause the split off transcript added above
    // to be deleted (as its in the new gene) - should this be figured out? and how?
    // this method goes further than the split analysis above - it would be nice
    // to consolidate the 2 somehow but i think this method depends on the new
    // transcript being added - even though it may then get removed
    // This is problematic for new transcript above which is adding itself to old
    // gene - with this it will now be with new gene - but then new gene event has
    // to come along before new trans or new gene wont be recgonized - in fact 
    // dont need new trans anymore with this (and only really need delete part of
    // exon - not really add part - comes with new gene - unless doing auditing)
    //splitAllGenes(changer, false);
    // false - dont query user
    //AnnotationChangeCoalescer splitChangeList = splitAllGenes(false,false);
    //newTrans.setId("temp-id"); // not truly set til below - used for removing event
    //SeqFeatureI clonedTranscript = newTrans.cloneFeature();
    ChangeList splitChangeList = splitAllGenes(false, topLevelFeats);
    if (splitChangeList.hasChanges()) {
      // new trans add event never added so no need for del event
      // but actually what does need to be removed is the update transcript parent
      // transaction of the transcript that was created above as its just an artifact
      // of this process not a real transaction - todo todo...
      //splitChangeList.removeDeleteEvent(clonedTranscript);
      // this needs to be reinstated - forget why it didnt work??
      //splitChangeList.removeUpdateParentEvent(clonedTranscript); not working
      //     and use this list as main list to add to - yes
      // otherwise make new transcript event - put in list - add move exon list to it
      changeList.addChangeList(splitChangeList);
    }
    else { // no splits - add transcript to current gene
      // need to check out how merge effects this...
      AnnotationAddEvent aae = new AnnotationAddEvent(this,newTrans);
      changeList.addChange(aae);
    }

    // JC: workaround to correct exon UpdateParentTransactions.  Only affects
    // PureJDBCTransactionWriter.
    logger.debug("updating cloned transcript ids and names on exonChanges");
    boolean updateAllIds = false;
    // update cloned parent ids even if they're not null
    boolean updateAllParentIds = true; 
    exonChanges.updateClonedTranscriptIdsAndNames(updateAllIds, updateAllParentIds);

    // after splits - splitGene also calls this - seems redundant
    // false means don't force ID to change as well if name doesn't need to be changed--is this right?

    // JC: this seems redundant to me too; splitGene() should handle this
    //    CompoundTransaction ct = setTransNameFromGene(newTrans.getGene(), false);
    //    ct.setSource(this);
    //    changeList.addChange(ct.generateAnnotationChangeEvent()); 

    // now that gene and transcript are resolved put in exon changes
    changeList.addChangeList(exonChanges);
    //endEdit(changeList);
    
    generateEditTransaction(topLevelFeats);
    
  } // end of splitTranscript()
  
  /**
   * Determine whether or not the SelectionSets are compatible with creating
   * an unsupported (no evidence) annotation.
   */
  public boolean createAnnotationAllowed() {
    // dont need an evidence set
    return (annotSet.getLeafFeatures().size() == 0
            && strandSet
            && basePosition != 1);
  }

  public void createAnnotation() {
    createAnnotation(null);  // type not specified
  }

  /**
   * The default createAnnotation method for creating an unsupported annotation.
   * Nothing should be selected, or under the cursor (annotSet and cursorSet should
   * be empty). The basePosition should be set, as should the default strand.
   * @see #setSelections(AnnotationView, Selection, Vector, int, int)
   */
  public void createAnnotation(String type) {
    if (annotSet.getLeafFeatures().size() > 0) {
      showMessage(JOptionPane.ERROR_MESSAGE,"There are features selected.");
      return;
    }
    if (basePosition == -1) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Base position isn't set.");
      return;
    }
    if (!strandSet) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Strand isn't set.");
      return;
    }
    createAnnotation(basePosition, defaultStrand, type);
  }

  /**
   * This method puts up a dialog asking for the size of the new exon, and
   * then calls {@link #createAnnotation(int,int,int)}.
   */
  public void createAnnotation(int basePosition, int strand, String type) {
    int size = 100;
    if (parentComponent != null) {
      String sizeStr = JOptionPane.showInputDialog(parentComponent,
                                                   "Enter desired length of annotation (in bases)",
                                                   "Specify annotation length",  // window title
                                                   JOptionPane.INFORMATION_MESSAGE);
      try {
        size = Integer.parseInt(sizeStr);
      } catch (NumberFormatException e) {
        // returns null if user hit "Cancel"
        if (sizeStr != null)
          showMessage(JOptionPane.ERROR_MESSAGE,"Non-numeric length " + sizeStr + "-- try again");
        return;
      }
    }

    // It's not clear to me why we need to add one to basePosition--if anything, I'd
    // expect to have to subtract one.  But the exon was starting one base too early.
    basePosition++;
    if (strand == -1)
        createAnnotation(basePosition-size+1, basePosition, strand, type);
    else
        createAnnotation(basePosition, basePosition+size-1, strand, type);
  }

  /**
   * This method uses {@link #addAnnotation(Vector,false,type)} 
   to create an unsupported transcript or gene, or one level?
   */
  public void createAnnotation(int low, int high, int strand, String type) {
    Vector features = new Vector();
    if (type == null)
      type = "tmptype";
    SeqFeatureI sf = new SeqFeature(low,high,type,strand);

    features.addElement(sf);
    addAnnotation(features, false, type);
  }

  /** annotSet can only have one exon */
  public boolean splitExonAllowed() {
    if (annotSet.getLeafFeatures().size() > 1) return false;
    if (annotSet.getLeafFeatures().size() == 0) return false;
    if (!(annotSet.getLeafFeat(0) instanceof ExonI)) return false;
    if (basePosition == -1) return false;
    if(!goodOwner(annotSet.getTranscript(0).getOwner())) return false;
    return true;
  }

  /**
   * The default splitExon method. This splits a single feature which should
   * be in the cursorSet (ie. by default under the cursor NOT selected), at the
   * basePosition (which should have been set using the appropriate
   * setSelection method). If endBasePosition is set it is used as the start
   * of one of the new exons, otherwise the exon is split at
   * basePosition + and - 1.
   */
  public void splitExon() {
    if (annotSet.getLeafFeatures().size() > 1) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Can only split one feature at a time.");
      return;
    } else if (annotSet.getLeafFeatures().size() == 0) {
      showMessage(JOptionPane.ERROR_MESSAGE,"No feature under cursor.");
      return;
    }
    if (!(annotSet.getLeafFeat(0) instanceof ExonI)) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Can only split an exon.");
      return;
    }
    if (basePosition == -1) {
      showMessage(JOptionPane.ERROR_MESSAGE,"Base position isn't set.");
      return;
    }
    if (endBasePosition == -1)
      splitExon((ExonI)annotSet.getLeafFeat(0),basePosition);
    else
      splitExon((ExonI)annotSet.getLeafFeat(0),
                basePosition,
                endBasePosition);
  }

  /**
   * The splitExon method which actually does the edit. It splits the exon
   * creating two exons, one which starts at exon.getLow() and ends at newHigh
   * and one which starts at newLow and ends at exon.getHigh(). It checks that
   * the newHigh and newLow are valid.
   * @param exon The exon to split.
   * @param newHigh Split exon limit (exon.getLow() - newHigh)
   * @param newLow  Split exon limit (newLow - exon.getHigh())
   */
  public void splitExon(ExonI exon, int newHigh, int newLow) {
    checkIfBeyondRange();

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    SeqFeatureI topLevelFeat = getTopLevelFeat(exon);
    generateFeatChangeData(topLevelFeats, topLevelFeat);
    
    logger.info ("****** SPLITTING FEATURE ******");
    Transcript tran       = (Transcript)exon.getRefFeature();
    //    int       exonsize   = exon.getHigh() - exon.getLow() + 1;
    int       high       = exon.getHigh();

    if (newHigh >= exon.getLow() && newLow <= exon.getHigh()) {
      //exon.setHigh(newHigh);
      int start = exon.isForwardStrand() ? exon.getStart() : newHigh;
      int end = exon.isForwardStrand() ? newHigh : exon.getEnd();
      setExonTerminus(exon,start,end, topLevelFeats); // sets model, fires event

      ExonI       newExon = new Exon();
      newExon.setStrand(exon.getStrand());
      newExon.setLow(newLow);
      newExon.setHigh(high);

      tran.addExon(newExon); // wont change ends of transcript
      
      setGeneEnds(tran); // probably not necasary
      setExonID(newExon, tran.getGene().getId(), true);

      ChangeList changer = beginEdit("split feature");
      ////AnnotationChangeEvent aae = new AnnotationAddEvent(this,newExon);
      ////changer.addChange(aae);
      ////changer.executeChanges();
      
      /* Should we select both exons or deselect both? */
      // nothing being done with these - selManager handles select
      //Vector v = new Vector(2); v.addElement(exon); v.addElement(newExon);
      
      endEdit(changer, true, tran.getGene(), FeatureChangeEvent.REDRAW);

      generateEditTransaction(topLevelFeats);
    } else {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Trying to split exon "+
                  "outside allowable exon boundaries");
    }
  }

  /**
   * Splits a Feature at basePosition + and - 1 with
   * {@link #splitExon(ExonI,int,int)}.
   */
  public void splitExon(ExonI exon, int basePosition) {
    splitExon(exon, basePosition - 1, basePosition + 1);
  }

   /** True if can delete selection */
  public boolean deleteSelectionAllowed() {
    if (annotSet.getLeafFeatures().size() == 0) return false;
    //logger.info(annotSet.getLeafFeatures());
    for(int i=0; i<annotSet.getTranscripts().size(); i++) 
      if(!goodOwner(annotSet.getTranscript(i).getOwner()))
        return false;    
    return true;
  } 
  
  /**
   * The default deleteSelectedFeatures which deletes any features in the
   * viewSet.
   */
  public void deleteSelectedFeatures() {
    deleteSelectedFeatures(annotSet.getLeafFeatures(),null);
  }

  /**
   * Deletes the Vector of features.
   * @param features The Vector of leaf features to delete, exons for 3 level annots
   * just annotatedFeat for 1 level annot.
   * @see #purgeExon(ExonI) and #purgeAnnot(AnnotatedFeatureI)
   */
  public void deleteSelectedFeatures(Vector features,String description) {
    checkIfBeyondRange();

    // confirm delete
    if (Config.getConfirmDeleteSelection()) {
      Object[] options = { "Delete", "Cancel" };
      JOptionPane pane =
        new JOptionPane("Are you sure you want to delete the current selection(s)?",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.OK_CANCEL_OPTION,
                        null, // icon
                        options, // all choices
                      options[1]); // initial value
      JDialog dialog = pane.createDialog(null,
                                         "Please Confirm");
      dialog.show();
      Object result = pane.getValue();
      if (result != null && ((String)result).equals("Cancel")) 
        return;
    }

    /*
    Set<SeqFeatureI> topLevelFeats = new TreeSet<SeqFeatureI>(new java.util.Comparator<SeqFeatureI>() {
      public int compare(SeqFeatureI sf1, SeqFeatureI sf2) {
        if (sf1.hashCode() < sf2.hashCode()) {
          return -1;
        }
        else if (sf1.hashCode() > sf2.hashCode()) {
          return 1;
        }
        return 0;
      }
      public boolean equals(Object o) {
        return this.equals(o);
      }
    });
    */

    /*
    for (int i = 0; i < features.size(); ++i) {
      SeqFeatureI feat = (SeqFeatureI)features.elementAt(i);
      while (!feat.isAnnotTop()) {
        feat = feat.getParent();
      }
      if (!topLevelFeats.containsKey(feat.hashCode())) {
        SeqFeatureI clone = feat.cloneFeature();
        System.out.printf("%s(%d)\t", feat.toString(), feat.hashCode());
        feat.setName("foo");
        System.out.printf("%s(%d)\t", feat.toString(), feat.hashCode());
        clone.setName("clone");
        System.out.printf("%s(%d)\n", clone.toString(), clone.hashCode());
        topLevelFeats.put(feat.hashCode(), clone);
      }
    }

    for (Map.Entry<Integer, SeqFeatureI> entry : topLevelFeats.entrySet()) {
      System.out.println(entry.getValue());
    }
    */
    
    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    ChangeList changer =
      beginEdit(description != null ? description : "delete selected features");
    AnnotatedFeatureI gene = null;
    Vector touched_trans = new Vector (1);
    for (int i=0; i < features.size(); i++ ) {
      SeqFeatureI sf = (SeqFeatureI)features.elementAt(i);
      SeqFeatureI topLevelFeat = getTopLevelFeat(sf);
      generateFeatChangeData(topLevelFeats, topLevelFeat);
      if (sf.isExon()) {
        Transcript trans = (Transcript) sf.getRefFeature();
        setOwnerAndDate(trans);
        if (!touched_trans.contains (trans))
          touched_trans.addElement (trans);
        gene = trans.getGene();
        if (changer == null) {
          logger.info ("****** DELETING FEATURES ******");
          /*
          if (description==null)
            description = "delete selected features";
          changer = beginEdit(description);
          */
        }
        //CompoundTransaction ct = purgeExon((ExonI)sf);
        purgeExon((ExonI)sf);
        //fireCompoundTransaction(ct);
      }
      //Config.DO_ONE_LEVEL_ANNOTS && - took out flag
      else if (sf.isAnnot() && sf.getAnnotatedFeature().isAnnotTop()) {
        //DeleteTransaction dt = purgeAnnot(sf.getAnnotatedFeature());
        purgeAnnot(sf.getAnnotatedFeature());
        //fireTransaction(dt);
        // cant return - need to process whole list of deletes
        //return; // nothing below (trans & splits) is needed
      }
      else {
        logger.warn("Deleted feat was neither an exon nor a 1 level"+
                    " annot - not deleted");
      }
    }
    if (changer != null) {
      for (int i=0; i < touched_trans.size(); i ++ ) {
        Transcript trans = (Transcript) touched_trans.elementAt(i);
        // if weve done a delete trans gene may be null and endEdit (below)
        // would be called with a null gene without this test
        if (trans.getGene() != null)
          gene = trans.getGene();
        if (trans.size() > 0 && gene.isProteinCodingGene()) {
          // Invalidate peptide sequence because it will need to be
          // recalculated if anyone
          // asks for it later.  (Don't bother recaculating right now--
          // might be a waste of time.)
          trans.setPeptideValidity(false);
          if (trans.getFeatureContaining(trans.getTranslationStart()) != null)
            trans.setTranslationEndFromStart();
          else
            trans.calcTranslationStartForLongestPeptide();
        }
      }

      splitAllGenes(changer, true, topLevelFeats);
      
    }

    generateEditTransaction(topLevelFeats);
    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW, true);
  }

  /**
   * Set the selSet and cursorSet to the used state. Its useful to know if the
   * same selections have been used for multiple edits, which will usually
   * indicate an error.
   */
  private void _setUsed(boolean state) {
    annotSet.setUsed(true);
    evidenceSet.setUsed(true);
  }

  /**
   * Determine if the default purgeExon is compatible with the current
   * SelectionSets.
   */
  public boolean deleteExonAllowed() {
    if (annotSet.getLeafFeatures().size() == 1 &&
        annotSet.getLeafFeat(0).isExon()) {
      return true;
    }
    return false;
  }

  /**
   * The default edit for deleting an exon from a transcript. This deletes the
   * first feature in the cursorSet (by default the features under the cursor). This
   * must be an ExonI.
   * Just calls deleteSelectedFeature with vector of exon. deleteSelectedFeature
   * handles finding trans start and end (which deleteExon previously wasnt)
   */
  public void deleteExon() {
    checkIfBeyondRange();
    logger.info ("****** DELETING EXON ******");
    if (deleteExonAllowed()) {
      Vector feats = new Vector(1);
      feats.addElement(cursorSet.getLeafFeat(0));
      deleteSelectedFeatures(feats,"delete exon");
    } else {
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Cursor selection incompatible with delete exon.");
    }
  }

  private AddTransaction addTranscript (AnnotatedFeatureI gene,
                                        Transcript trans) {
    gene.addFeature(trans);
    AddTransaction at = new AddTransaction(this,trans);
    //AnnotationAddEvent aae = new AnnotationAddEvent(this,trans);
    //changer.addChange(aae);
    //changer.executeChanges(); // take out? yes! investigate
    return at;
  }

  /** Deletes gene from model, returns DeleteTransaction. 
      called only by purgeTranscript */
  private DeleteTransaction purgeAnnot (AnnotatedFeatureI delete_annot) {
    /* Genes are no longer automatically deleted inside of deleteFeature
       if the number of transcripts goes to zero, so this needs to
       happen here now */
    
    FeatureSetI annotParent = delete_annot.getParent();
    if (annotParent != null)
      annotParent.deleteFeature(delete_annot);

    DeleteTransaction dt = new DeleteTransaction(delete_annot,annotParent);
    return dt;
  }

  /** Deletes transcript - change this. Deletes gene if last transcript (purgeGene). 
      used to fire delete event, now just adds it to changer. If addDeleteEvent is
      false delete event is not added (just modifies model) 
      This is doing double duty, doing both changer and compound trans. need to phase out
      changer... */
  private CompoundTransaction purgeTranscript (AnnotatedFeatureI gene,
                                               Transcript trans) {
    checkIfBeyondRange();
    CompoundTransaction compoundTransaction = new CompoundTransaction(this);
    gene.deleteFeature(trans);
    // have to clone transcript as it may get reused (split,merge)
    // used to not have to clone when we fired events instantaneoulsy, but now that
    // transactions/events are not fired right away (gathered in compound/changeList)
    // by the time this gets fired the deleted trans is part of a new gene (merge/split)
    // which can mess up the recipient (drawables i think has a hard time synching)
    AnnotatedFeatureI transClone = trans.cloneAnnot();
    //AnnotationDeleteEvent ade = new AnnotationDeleteEvent(this,transClone,gene);
    //changer.addChange(ade);
    /*DeleteTransaction del = new DeleteTransaction(transClone,gene);
    compoundTransaction.addTransaction(del);

    if (gene.size() == 0) {
      del = purgeAnnot (gene);
      compoundTransaction.addTransaction(del);
    }*/
    //For transactions, either delete the gene, or delete the transcript, but not both 
    if (gene.size() == 0) {
      DeleteTransaction del = purgeAnnot (gene);
      compoundTransaction.addTransaction(del);
    }else{
      DeleteTransaction del = new DeleteTransaction(transClone,gene);
      compoundTransaction.addTransaction(del);
    }
    return compoundTransaction;
  }

  /**
   * Delete an exon from a transcript. If this is the last exon in a transcript
   * the transcript is removed(purgeTranscript), and in that case if that transcript
   * is the last transcript in the gene, the gene is also deleted.
   * <BR>
   * NOTE: This method is called from several of the other edits.
   * <BR>
   * When you do this you could split a gene into two. Currently
   * this method leaves it to the caller to tidy up the genes.
   Took out call to executeChanges - caller must now do that - which it already was
   */
  private CompoundTransaction purgeExon(ExonI exon) {//, ChangeList changer) {
    CompoundTransaction compoundTransaction = new CompoundTransaction(this);
    Transcript  tran    = (Transcript) exon.getRefFeature();
    AnnotatedFeatureI gene    = (AnnotatedFeatureI) tran.getRefFeature();

    // delete exon before execute changes so views can pick up model changes
    // on handleAnnotationChange. Will this mess things up? MG 1.10.03
    tran.deleteExon(exon);

    //     if (changer != null) { // phase out...
    //       AnnotationDeleteEvent ade = new AnnotationDeleteEvent(this,exon,tran);
    //       changer.addChange(ade);
    //     }
    DeleteTransaction del = new DeleteTransaction(exon,tran);
    compoundTransaction.addTransaction(del);

    if (tran.size() == 0) {
      CompoundTransaction ct = purgeTranscript (gene, tran);
      compoundTransaction.addTransaction(ct);
    } else {
      setOwnerAndDate(tran);
    }
    return compoundTransaction;
  }

  /**
   * Sets the 5' end of an exon. A single exon in the AnnotationView should have
   * been selected (in the viewSet) and a single feature from the result view
   * (in the siteSet). The exon 5' is set to the result 5' position.
   */
  public void setAs5Prime() {
    setExonTerminus(START);
  }

  /**
   * Sets the 3' end of an exon. A single exon in the AnnotationView should have
   * been selected (in the viewSet) and a single feature from the result view
   * (in the siteSet). The exon 3' is set to the result 3' position.
   */
  public void setAs3Prime() {
    setExonTerminus(END);
  }

  /**
   * Sets both ends of an exon. A single exon in the AnnotationView should have
   * been selected (in the viewSet) and a single feature from the result view
   * (in the siteSet). The exon limits are set to the result limits.
   */
  public void setAsBothEnds() {
    setExonTerminus(BOTHENDS);
  }

  /**
   * Checks whether the current SelectionSets are consistent with the default
   * setExonTerminus method. This method is NOT actually used in setExonTerminus,
   * setAs3Prime or setAs5Prime.
   */
  public boolean setExonTerminusAllowed() {
    if (annotSet.getLeafFeatures().size() != 1 ||
        evidenceSet.getLeafFeatures().size() != 1) {
      return false;
    }
    return true;
  }


  /** int end is from DrawableAnnotationConstants - need to move to this package */
  public void setExonTerminus(int end) {
    switch (annotSet.getLeafFeatures().size()) {
    case 0:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected NO features to set end with");
      break;
    case 1:
      SeqFeatureI firstAnnot = annotSet.getLeafFeat(0);
      SeqFeatureI evFeat = evidenceSet.getLeafFeat(0);

      if (!setExonTerminusAllowed()) {
        logger.warn("setExonTerminusAllowed not allowed");
      } else
        checkIfBeyondRange(); // checks that evidence set is not out of range

      if (firstAnnot.isExon()) {
        setAnnotTerminusFromFeature(firstAnnot,evFeat,end);
      }
      else if (DO_ONE_LEVEL_ANNOTS && isOneLevelAnnot(firstAnnot)) {
        setAnnotTerminusFromFeature(firstAnnot,evFeat,end);
      }
      else {
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "MUST select exactly one exon to set end for");
      }
      break;
    default:
      showMessage(JOptionPane.ERROR_MESSAGE,
                  "Selected more than 1 feature to use for setting end");
      break;
    }
  }

  /**
   * Sets a terminus/i of an exon.
   * A single exon in the AnnotationView should have
   * been selected (in the viewSet) and a single feature from the result view
   * (in the siteSet).
   * The exon terminus/i is/are set to the result terminus position.
   * <BR>
   * NOTE: When you do this you could split a gene into two
   * OR combine two genes.
   *
   * @param exon The exon to change the terminus on.
   * @param sf   The feature to set the end from.
   * @param endType  Either DrawableAnnotationConstants.START or
   *                    DrawableAnnotationConstants.END or
   *                    DrawableAnnotationConstants.BOTHENDS
   * @param name How to describe the end (eg 5Prime).
   *
   */
  private void setAnnotTerminusFromFeature(SeqFeatureI annot,
                                          SeqFeatureI sf,
                                          int endType) {
    int oldStart = annot.getStart();
    int oldEnd = annot.getEnd();
    int newStart = ((endType == START || endType == BOTHENDS) ?
                    sf.getStart() : oldStart);
    int newEnd = ((endType == END || endType == BOTHENDS) ?
                  sf.getEnd() : oldEnd);

    setAnnotTerminus(annot.getAnnotatedFeature(),oldStart,oldEnd,newStart,newEnd);

  }

  /** Set exon terminus to newStart and newEnd. exon has not been updated
      yet so can get old values from exon. */
  private void setExonTerminus(ExonI exon, int newStart, int newEnd) {
    setAnnotTerminus(exon,exon.getStart(),exon.getEnd(),newStart,newEnd);
  }

  private void setExonTerminus(ExonI exon, int newStart, int newEnd, Map<Integer, FeatChangeData> topLevelAnnots) {
    setAnnotTerminus(exon,exon.getStart(),exon.getEnd(),newStart,newEnd, topLevelAnnots);
  }
  
  public SeqFeatureI setAnnotTerminus(AnnotatedFeatureI annFeat, int oldStart, int oldEnd, 
      int newStart, int newEnd) {
    annFeat.setStart(oldStart);
    annFeat.setEnd(oldEnd);
    /*
    System.out.println(annFeat + "\t" + annFeat.hashCode() + "\t" + oldStart + "\t" + oldEnd + "\t" + newStart + "\t" + newEnd);
    System.out.println("=========");
    */

    Map<Integer, FeatChangeData> topLevelFeats = new HashMap<Integer, FeatChangeData>();
    SeqFeatureI topLevelFeat = getTopLevelFeat(annFeat);
    SeqFeatureI clone = generateFeatChangeData(topLevelFeats, topLevelFeat, true);

    /*
    System.out.println(topLevelFeat + "\t" + topLevelFeat.hashCode() + "\toriginal");
    for (Object o : topLevelFeat.getFeatures()) {
      System.out.println("\t" + o + "\t" + o.hashCode());
      for (Object o2 : ((SeqFeatureI)o).getFeatures()) {
        System.out.println("\t\t" + o2 + "\t" + o2.hashCode());
      }
    }
    System.out.println(clone + "\t" + clone.hashCode() + "\tnew");
    for (Object o : clone.getFeatures()) {
      System.out.println("\t" + o + "\t" + o.hashCode());
      for (Object o2 : ((SeqFeatureI)o).getFeatures()) {
        System.out.println("\t\t" + o2 + "\t" + o2.hashCode());
      }
    }
    System.out.println("=========");
    */

    //System.out.println(getNameAdapter(annFeat).generateExonId(curation.getAnnots(), curation.getName(), annFeat, annFeat.getParent().getParent().getId()));
    
    setAnnotTerminus(annFeat, oldStart, oldEnd, newStart, newEnd, topLevelFeats);

    //if it's an exon, might need to change it's name/id
    if (annFeat.isExon()) {
      /*
      annFeat.setId(getNameAdapter(annFeat).generateExonId(curation.getAnnots(),
          curation.getName(), annFeat, annFeat.getParent().getParent().getId()));
          */
      setExonID((ExonI)annFeat, annFeat.getParent().getParent().getId(), true);
      //annFeat.setName(annFeat.getId());
    }
    generateEditTransaction(topLevelFeats);
    /*
    for (FeatChangeData i : topLevelFeats.values()) {
      System.out.println(i.oldFeat + "\t" + i.oldFeat.hashCode() + "\told");
      for (Object o : i.oldFeat.getFeatures()) {
        System.out.println("\t" + o + "\t" + o.hashCode());
        for (Object o2 : ((SeqFeatureI)o).getFeatures()) {
          System.out.println("\t\t" + o2 + "\t" + o2.hashCode());
        }
      }        
      for (SeqFeatureI j : i.newFeats) {
        System.out.println(j + "\t" + j.hashCode() + "\tnew");
        for (Object o : j.getFeatures()) {
          System.out.println("\t" + o + "\t" + o.hashCode());
          for (Object o2 : ((SeqFeatureI)o).getFeatures()) {
            System.out.println("\t\t" + o2 + "\t" + o2.hashCode());
          }
        }        
      }
    }
    */
    
    return clone;

  }
  
  /** Set exon ends to newStart and newEnd (if they are different) 
      Update transcript and gene ends.
      Check for merge and splits  
      Whats funny here is that exon should contain the oldStart and oldEnd.
      But the EDE actually changes the exon ends as it is dragged so what we 
      get (post-drag) is an exon with the new values not old. This method accomodates
      both exons with new range and with old range. */
  public void setAnnotTerminus(AnnotatedFeatureI annFeat, int oldStart, int oldEnd, 
                              int newStart, int newEnd, Map<Integer, FeatChangeData> topLevelFeats) {

    if (!annFeat.getRefFeature().isTranscript() && !DO_ONE_LEVEL_ANNOTS) {
      logger.error("Exon belongs to a " + annFeat.getRefFeature().getClass().getName());
      return;
    }

    Transcript trans = null;
    AnnotatedFeatureI topAnnot = null;

    // EXON OVERLAP CHECK
    if (annFeat.isExon()) {
      trans = (Transcript)annFeat.getRefFeature();
      topAnnot = trans.getGene();

      if (isExonOverlap(topAnnot, annFeat, newStart, newEnd)) {
        String desc = getExonChangeDescription(annFeat,newStart,newEnd);
        showMessage(JOptionPane.ERROR_MESSAGE,
                    "New " + desc + " boundary would overlap existing exon");
        return;
      }
    }
    else if (annFeat.isAnnotTop()) { // one level annots
      topAnnot = annFeat;
    }
    

    if (!isRangeOK(annFeat,newStart,newEnd)) {
      return;
    }

    if (newStart == oldStart && newEnd == oldEnd)
      return; // nothings actually changed

    // Change the model - cant clone exon as exon may have new range
    RangeI oldRange = new Range("",oldStart,oldEnd);
    if (newStart != annFeat.getStart())
      annFeat.setStart(newStart);
    if (newEnd != annFeat.getEnd())
      annFeat.setEnd(newEnd);

    // Set ends for all transcripts in the gene and the gene itself.
    // changes model, doesnt fire events
    if (annFeat.isExon())
      setGeneEnds(trans);

    TransactionSubpart ts = TransactionSubpart.LIMITS;
    AnnotationUpdateEvent aue = new AnnotationUpdateEvent(this,annFeat,ts);
    // This is crucial, not just for undo, but for exon identity!
    aue.setOldRange(oldRange);

    ChangeList changer  = beginEdit("set exon terminus");
    ////changer.addChange(aue);
    
    // True means ask before merging genes in this region
    CompoundTransaction merge = consolidateGenes(topAnnot,true);
    if (merge != null)
      ////changer.addChange(merge.generateAnnotationChangeEvent());

    // Ask before splitting (renaming) genes in this region
    splitAllGenes(changer, true, topLevelFeats);

    endEdit(changer);
  }

  /** Return string descriping limit change to exon based on input, used for err msg */
  private String getExonChangeDescription(AnnotatedFeatureI exon, int newStart,
                                          int newEnd) {
    if (newStart != exon.getStart() && newEnd != exon.getEnd())
      return "Both termini";
    else if (newStart != exon.getStart())
      return "5Prime";
    else if (newEnd != exon.getEnd())
      return "3Prime";
    return "(neither end changed)";
  }

  /** Should there be an ownership event - probably */
  public void takeOwnership(Vector features) {
    ChangeList changer = beginEdit("take ownership");
    SeqFeatureI gene = null;
    for (int i=0; i < features.size(); i ++ ) {
      AnnotatedFeatureI sf = (AnnotatedFeatureI) features.elementAt(i);
      setOwnerAndDate(sf);

      if (sf instanceof Transcript)  // it might instead be a one-level annot
        gene = ((Transcript)sf).getGene();

      // somehow register edit with changer
    }
    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW);
  }

  public void disown(Vector features) {
    ChangeList changer = beginEdit("disowning");
    SeqFeatureI gene = null;
    for (int i=0; i < features.size(); i ++ ) {
      AnnotatedFeatureI sf = (AnnotatedFeatureI) features.elementAt(i);
      sf.setOwner(null);
      if (sf instanceof Transcript)   // it might instead be a one-level annot
        gene = ((Transcript)sf).getGene();
      // somehow register edit with changer
    }
    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW);
    // ! Do we need a Transaction for the change in ownership? YES!
  }
  /** This needs to send out a real annotation change event - only sends out
      a redraw at the moment */
  public void setTranscriptStatus(Transcript t) {
    ChangeList changer = beginEdit("set status");
    boolean status = (t.getProperty("status") != null &&
                      t.getProperty("status").equals(ALL_DONE));
    if (!status) {
      t.replaceProperty("status", ALL_DONE);
    } else {
      t.replaceProperty("status", NOT_DONE);
    }

    SeqFeatureI gene = t.getRefFeature();
    setOwnerAndDate(t);
    endEdit(changer, true, gene, FeatureChangeEvent.REDRAW);
  }

  public boolean assignAnnotationNameAllowed() {
    return evidenceSet.getLeafFeatures().size() == 1 &&
      annotSet.getLeafFeatures().size() == 1;
  }

  public void assignAnnotationName() {
    SeqFeatureI evidence = evidenceSet.getLeafFeat(0);

    SeqFeatureI annot = annotSet.getLeafFeat(0);
    if (annot instanceof ExonI)
      annot = annot.getRefFeature();

    String newName = evidence.getName();
    if (evidence instanceof FeaturePairI) {
      SeqFeatureI hit = ((FeaturePairI) evidence).getHitFeature();
      SequenceI seq = (SequenceI) hit.getRefSequence();
      if (seq != null && seq.getName() != null)
        newName = seq.getName();
      else if (hit.getName() != null)
        newName = hit.getName();
    }
    annot.setName(newName);
  }


  /**
   * Redetermines the limits for a gene, and all its contained transcripts.
   redo translation start and stop if needed, invalidate peptide, set owner,date
   */
  private void setGeneEnds(Transcript trans) {
    AnnotatedFeatureI gene = trans.getGene();
    if (gene == null) // this is possible if transcript is new (buildTrans)
      return;
    for (int i=0; i<gene.size(); i++) {
      ((Transcript)gene.getFeatureAt(i)).adjustEdges();
    }
    gene.adjustEdges();
    if (gene.isProteinCodingGene()) {
      if (trans.getFeatureContaining(trans.getTranslationStart()) != null)
        // don't mess with the existing start, but do update
        // where the stop will now occur
        trans.setTranslationEndFromStart();
      else
        /* the location of the translation start is no longer contained in this
           transcript, so it is okay to (even needed) reset it */
        trans.calcTranslationStartForLongestPeptide();
      /* Invalidate peptide sequence because it will need to be
         recalculated if anyone asks for it later.  (Don't bother
         recaculating right now--might be a waste of time.) */
      trans.setPeptideValidity(false);
    }
    // this transcript has been modified, so take ownership
    setOwnerAndDate(trans);
  }

  /**
   * findSplits returns true if gene needs splitting.  
   * The method for finding gene splits based on the FLY_GENEDEF gene
   * definition.
   *
   * Note: This is also used for the HUMAN_GENEDEF now, although
   * the overlap methods differ
   * The transcripts making up each new gene are returned in Vectors in groups.
   * For each transcript, find all other transcripts it connects to,
   * either directly or indirectly. To do this for each transcript,
   * check against
   * all the others and record those which it hits directly in a vector.
   * Then for each transcript go to all it hits directly and get any new ones
   * from there until either all transcripts are found or all hits have been
   * examined.

   If gene is dicistronic doesnt split it, prints message to stdout.
   *
   * @param gene   The gene to check for splits in
   * @param groups In as an empty vector, filled with overlapping groups of
   *               transcripts if gene is split.
   */
  private boolean findSplits(AnnotatedFeatureI gene, Vector groups) {
    boolean split = false;

    if (gene.size() == 0) {  // Why does this happen?
      logger.debug("findSplits: gene " + gene.getName() + " has no transcripts!");
      return false;
    }

    Hashtable linkSets = new Hashtable();

    if (gene.getProperty("dicistronic") != null &&
        gene.getProperty("dicistronic").equals("true")) {
      logger.info("Not splitting dicistronic gene " + gene.getName() + ".  If you want to split it, pull it up in the Annotation Info Editor and uncheck the 'Is Dicistronic?' checkbox.");
      return false;
    }

    // find overlapping set of transcripts for each transcript
    for (int i=0; i<gene.size(); i++) {
      SeqFeatureI trans1 = gene.getFeatureAt(i);

      /* this is a vector of all the transcripts that overlap this one. The vector of
         overlapping transcripts is saved in linksSets */
      Vector links = new Vector();
      linkSets.put(trans1,links);

      for (int j=i+1; j<gene.size(); j++) {
        SeqFeatureI trans2 = gene.getFeatureAt(j);
        if (areOverlapping(trans1, trans2)) {
          links.addElement(trans2);
        }
      }
    }

    // Merge overlapping transcript vectors created above
    Vector alreadyAdded = new Vector();
    for (int i=0; i<gene.size(); i++) {

      SeqFeatureI trans1 = gene.getFeatureAt(i);

      if (!alreadyAdded.contains(trans1)) {

        Vector allLinks = new Vector();
        allLinks.addElement(trans1);

        _addLinks(trans1, linkSets, allLinks, gene.size());

        for (int j=0; j<allLinks.size(); j++)
          alreadyAdded.addElement(allLinks.elementAt(j));

        if (allLinks.size() != gene.size()) {
          // Gene needs splitting
          groups.addElement(allLinks);
          split  = true;
        }
      }
    }
    return split;
  }

  /**
   * Used by flySplitGenes to add indirectly linked transcripts recursively.
   */
  private void _addLinks(SeqFeatureI key,
                         Hashtable linkSets,
                         Vector allLinks,
                         int totLink) {
    Vector links = (Vector)linkSets.get(key);

    for (int i=0;i<links.size() && allLinks.size()!=totLink; i++) {
      SeqFeatureI sf = (SeqFeatureI)links.elementAt(i);
      if (!allLinks.contains(sf)) {
        allLinks.addElement(sf);
        _addLinks(sf,linkSets, allLinks,totLink);
      }
    }
  }

  /**
   * findMerges a group listing other genes to merge.
   * The genes making up
   * each gene to subsume are returned in Vectors in groups.
   * Gets transitive closure of overlaps. That is annots that overlap this annot and annots
   that overlap that, etcetera. The way it avoids endless recursive loop is to pass in
   annots/groups already found to overlap.
   * @param gene   The gene to check for merges with
   * @param groups In as an empty vector, filled with overlapping groups of
   *               transcripts if gene is split. 
                   So in a sense its both passed in and returned
   Returns true if found at least one group.
   Refactor for clarity. Call getOverlappingAnnots() and have it return list
   of overlapping annots. Caller can test list for emptiness.
   
   9/8/2005: Only look for overlaps between first-tier annotations--ignore
   second-tier annotations.
   */
  private boolean findMerges(AnnotatedFeatureI gene, Vector groups) {
    Vector annots = annotationCollection.getFeatures(); // all annots on strand
    for (int i=0; i<annots.size(); i++) {
      AnnotatedFeatureI gi = (AnnotatedFeatureI)annots.elementAt(i);
      if (gi != gene &&
          isFirstTierAnnot(gi) &&
          areOverlapping(gi,gene) &&
          !groups.contains (gi)) {
        groups.addElement (gi);
        findMerges (gi, groups);
      }
    }
    return (groups.size() > 1);
  }

  /** Return AnnotationChangeCoalescer which contains a list of all the events
      the split has caused - if any. 
      checkFirst is whether to query user about splitting 
      if addDeleteTranscriptEvents is false then delete transcript event will not be
      added to coalescer. This is necasary if theres no transaction adding these 
      transcripts, as is the case in splitTranscript where the transcript is new */
  private ChangeList splitAllGenes(boolean checkFirst, Map<Integer, FeatChangeData> topLevelFeats) {
    ChangeList changeList = createCoalescer();
    splitAllGenes(changeList,checkFirst, topLevelFeats);
    return changeList;
  }
  
  private ChangeList splitAllGenes(boolean checkFirst) {
    return splitAllGenes(checkFirst, null);
  }

  /**
   * Goes through ALL the genes in the annotationCollection
   (set in the constructor) checking
   * and fixing any that have become split.
   Calls splitGene on any gene found to be split by findSplits
   * @param changer  The AnnotationChangeCoalescer for the current edit (usually
   *                 returned from beginEdit).
   * @param dontSplit  Vector of Genes that the user does not want to split.
   */
  private void splitAllGenes(ChangeList changer,boolean checkFirst) {
    splitAllGenes(changer, checkFirst, null);
  }

  private void splitAllGenes(ChangeList changer,boolean checkFirst, Map<Integer, FeatChangeData> topLevelFeats) {
    // Ask before splitting (renaming) genes in this region
    Vector annots = annotationCollection.getFeatures();
    int split_cnt = 0;
    Map<Integer, Integer> splits = new HashMap<Integer, Integer>();
    for (int i=0; i <  annots.size(); i++) {
      AnnotatedFeatureI gene = (AnnotatedFeatureI)annots.elementAt(i);
      Vector groups = new Vector();
      if (findSplits(gene, groups)) {
        if (!checkFirst ||
            (checkFirst && userReallyWantsToSplit(gene))) {
          logger.warn("Gene " + gene.getName() + " will be split.");
          //CompoundTransaction splitTrans = splitGene(gene,groups);
          //changer.addChange(splitTrans.generateAnnotationChangeEvent(this));
          splitGene(gene, groups, topLevelFeats, splits);
          split_cnt++;
        }
      }
    }
    // Don't select any of the newly split genes
    if (split_cnt > 0)
      deselect();
  }

  private CompoundTransaction splitGene(AnnotatedFeatureI gene,Vector groups) {
    return splitGene(gene, groups, null, null);
  }
  
  /**
   * Actually do the edits necessary to create multiple genes from a
   * single gene that has become split.
   * Which group should retain the original gene id etc
   * (largest, leftest, ???) ? I voted leftest. <BR>
   * NOTE: This method should be changed to the rules defined in a message sent
   *       by Sima to the apollo list.
   * And hopefully it now has been (SUZ)
   Creates new gene & returns CompoundTransaction for split.
   Removes transcripts from gene and adds them to new gene
   groups is a vector of vectors (yuck). The first vector is the set of transcripts
   left for the gene. the remaining vectors are sets of transcripts for new genes.
   This can produce more than one new gene.
   called by splitAllGenes
   */  
  private CompoundTransaction splitGene(AnnotatedFeatureI gene,Vector groups,
      Map<Integer, FeatChangeData> topLevelAnnots, Map<Integer, Integer> splits) {
    FeatureSetI geneParent = annotationCollection;

    CompoundTransaction splitTransaction = new CompoundTransaction(this);
    splitTransaction.setCompoundType(CompoundTransaction.SPLIT);
    splitTransaction.setSeqFeature(gene);

    // Note first group is left in existing gene
    for (int i=1; i<groups.size(); i++) {
      Vector linked = (Vector)groups.elementAt(i);
      logger.debug("splitGene handling group " + i + "/" + groups.size());

      AnnotatedFeatureI newGene = new AnnotatedFeature();

      geneParent.addFeature(newGene, true);
      
      splits.put(newGene.hashCode(), gene.hashCode());
      //System.out.printf("split:\t%s(%d)\t%s(%d)\n", newGene, newGene.hashCode(), gene, gene.hashCode());
 
      Integer hashCode = gene.hashCode();
      FeatChangeData data = topLevelAnnots.get(hashCode);
      //if a product from a gene split needs to be split, need to be able to find link
      //to the original gene before any splits
      while (data == null) {
        hashCode = splits.get(hashCode);
        data = topLevelAnnots.get(hashCode);
      }
      //topLevelAnnots.get(gene.hashCode()).newFeats.add(newGene);
      data.newFeats.add(newGene);

      // JC: this doesn't make sense to me.  If groups.size() > 2 then this code will
      // run each time through the loop and you'll end up with the CompoundTransaction's
      // split feature set to the *last* gene created.  If that was truly the desired
      // effect of this code then why is it inside the loop?  Discuss.
      // I also don't see any calls to getNewSplitFeature, so I think the whole thing
      // can be excised.
      splitTransaction.setNewSplitFeature(newGene);

      /* this absolutely must be set so that when the the drawables are created this 
	 silly thing can be found again.  Once again one must remember that apollo does 
	 not (weep) treat the sequence as a single molecule/dataobject but as two 
	 separate things */
      newGene.setStrand (gene.getStrand());

      /* inherit the same annotation type as well */
      newGene.setFeatureType (gene.getFeatureType());

      /* give split gene new name, id, syn, copy dbxref,syns,comments,desc */
      CompoundTransaction geneIdAndAnnotTrans = setSplitGeneIds(geneParent, gene, newGene);

      Hashtable movedTrans = new Hashtable(linked.size());
      for (int j=0; j<linked.size(); j++) {
        Transcript trans = (Transcript)linked.elementAt(j);
        movedTrans.put(trans, Boolean.TRUE);
      }

      // rename the transcripts that will *not* be moved to newGene
      CompoundTransaction renameOrigTranscripts = new CompoundTransaction(this);
      String geneName = gene.getName();
      int geneTransCount = gene.size();
      for (int j=0; j<geneTransCount; j++) {
        Transcript trans = (Transcript) gene.getFeatureAt(j);
        // this transcript will *not* be moved to newGene:
        if (!movedTrans.containsKey(trans)) {
          logger.debug("SETTING ID AND NAME FOR ORIGINAL TRANSCRIPT " + j + " of " + gene);
          //	  CompoundTransaction tIdTrans = setTransID(trans,gene);
          //	  CompoundTransaction tNameTrans = setTransName(trans,gene);

          splitTransaction.addTransaction(setTransID(trans,gene));
          splitTransaction.addTransaction(setTransName(trans,gene));
        }
      }

      // el - need to assign new transcript id/name after it has been moved to newGene
      //      otherwise it won't pick up newGene's name
      // Assign names and ids to transcripts that *will* be moved to newGene.
      // This has to happen before the UpdateParentTransactions are generated so that the 
      // names and ids aren't null.
      /*
      String newGeneName = newGene.getName();
      for (int j=0; j<linked.size(); j++) {
        Transcript trans = (Transcript)linked.elementAt(j);
        if (!(trans.getName().startsWith(newGeneName))) {
          // ignoring transactions; setting of ids and names will be taken care of by 
          // AddTransaction for newGene
          setTransID(trans, newGene);
          setTransName(trans, newGene);
        }
      } 
      */     
      
      // Create UpdateParentTransactions
      CompoundTransaction parentTransactions = new CompoundTransaction(this);
      for (int j=0; j<linked.size(); j++) {
        Transcript trans = (Transcript)linked.elementAt(j);

        // no longer fires del event
        // cant clone here - wont dont model properly - only clone for event in purge
        //Transcript oldTrans = trans.cloneTranscript(); -- cloned in purge
        //AnnotatedFeatureI oldAnnot = gene.cloneAnnot(); -- no need to clone
        //purgeTranscript(gene, trans, changer);
        // actually what we need here is an update transcript parent event as 
        // otherwise its impossible to get drawables and transaction correct and
        // it is in fact what is happening
        //ace = moveTranscriptToAnnot(trans,newGene);
        // parent transaction not dealt with in ChadoTransTx, but coalesced anyways
        // in slit hack so not seen yet
        // So it turns out this transaction is solely an artifact of the funny split
        // process where the transcript is split before the gene is and the new
        // transcript is added to the old gene, and then if the gene is seen to be
        // split the old transcript is moved to the new gene. Though actually im not
        // sure about multi trans cases, where existing trans get moved over as well
        // need to think about this some more, for now supressing as it messes up
        // DrawableFeatureSet (puts out unfindable message) and chado adapter doesnt
        // need (and no undo yet)
        // ok i was somewhat wrong this is needed for the multi trans case for the
        // existing trans. its just the newly split off trans thats not needed at all
        // it would be good to supress that somehow as some self-coalescence thing
        // but is there anyway to know which one that is? i think we'd actually
        // have to pass it along (or splitTranscript could extract it)
        UpdateParentTransaction ut = moveTranscriptToAnnot(trans,newGene);
        parentTransactions.addTransaction(ut);
      }

      // Assign names and ids to transcripts that *will* be moved to newGene.
      // This has to happen before the UpdateParentTransactions are generated so that the 
      // names and ids aren't null.
      String newGeneName = newGene.getName();
      for (int j=0; j<linked.size(); j++) {
        Transcript trans = (Transcript)linked.elementAt(j);
        if (!(trans.getName().startsWith(newGeneName))) {
          // ignoring transactions; setting of ids and names will be taken care of by 
          // AddTransaction for newGene
          setTransID(trans, newGene);
          setTransName(trans, newGene);
        }
      } 

      // el - need to add newGene to feature set BEFORE generating new id/name
      //      otherwise will have id/name conflict
      /* these must be added in sorted order so that the EDE upstream
         and downstream code works */
      //geneParent.addFeature(newGene, true);

      // Add event for adding new gene - needs to come first, but not before the *new* genes
      // and transcripts have been assigned ids.
      AddTransaction at = new AddTransaction(newGene);
      splitTransaction.addTransaction(at);
      splitTransaction.addTransaction(geneIdAndAnnotTrans);

      // these events are not being processed properly in transaction manager
      // and we dont actually need them becasue split is currently just a delete
      // and 2 adds - and its crunch time - so im scrapping these events
      // false means don't force ID to change as well if name doesn't need to be changed--is this right?
      //      setTransNameFromGene(gene, false);  // returns CompoundTrans - at some point will need
      ///      setTransNameFromGene(newGene, false); // returns CompoundTrans - ignoring for now

      // add update trans parent events
      // see note above about supressing these
      splitTransaction.addTransaction(parentTransactions);
    }
    splitTransaction.setSource(this);
    return splitTransaction;
  }

  /** This is solely used by splitGene. In hindsight I realize it doesnt need to 
   *   produce a transaction as theres noone who needs it (db,gui, or undo). The
   *   reason is this is merely an artifact of the way splits are done. We split a 
   *   transcript and add the new transcript to the old gene, then we check if the 
   *   gene is split and if so move the transcript to the new gene. But the transcript
   *   didnt need to be added to the old gene in the first place - its an artifact
   *   of a funny process. didnt realize this at the time. take out
   *   trans? wait but it is needed for transcripts that werent split
   *   that now migrate to the new split off gene isnt it? yes */
  private UpdateParentTransaction moveTranscriptToAnnot(Transcript trans,
                                                        AnnotatedFeatureI newParent) {
    AnnotatedFeatureI oldAnnot = trans.getGene(); // rename getAnnot
    oldAnnot.deleteFeature(trans);
    newParent.addFeature(trans);
    if (newParent.isProteinCodingGene())
      trans.calcTranslationStartForLongestPeptide();
    setOwnerAndDate(trans);

    UpdateParentTransaction upt = new UpdateParentTransaction(this,trans,oldAnnot,newParent);
    // the id of this transcript may get changed by its new
    // gene(depending on name adapter). Old id is needed
    // for database lookup/update. id is not changed yet.
    upt.setOldId(trans.getId());

    // if oldAnnot has no transcripts purge it - can this happen? dont think so
    return upt;
  }

  /** Returns a copy of original gene that has only ID  and name set,
   *  this is all that is needed for reporting in the change log and this
   *  is all that these copies should ever, ever be used for.
   *  12/2005 Not currently used */
//   private AnnotatedFeatureI shallowCopy(AnnotatedFeatureI orig) {
//     AnnotatedFeatureI copy = new AnnotatedFeature();
//     copy.setId(orig.getId());
//     copy.setName(orig.getName());
//     return copy;
//   }

  /** Check that all features the user is trying to create a transcript with 
   *  (evidenceSet) (or do some other edit to) are contained within the bounds 
   *  of the current region. If not, warn user. */
  private void checkIfBeyondRange() {
    if (evidenceSet == null) return;
    Vector features = evidenceSet.getLeafFeatures();
    for (int i = 0; i < features.size(); i++) {
      Object elem = features.elementAt(i);
      if (elem instanceof SeqFeatureI) {
        SeqFeatureI sf = (SeqFeatureI)elem;
        if (!(sf.isContainedByRefSeq())) {
          String m = "WARNING: the feature(s) you have selected are not entirely contained within the current sequence region,\nso this edit may not get saved in the database.\n";
          logger.warn(m);
          if (Config.internalMode())
            JOptionPane.showMessageDialog(null,m);
          return;
        }
      }
    }
  }

  /** 12/2005 Not currently used */
//   private void selectFeature(SeqFeatureI feat, TierViewI view) {
//     getSelectionManager().select(feat, view); // exclusive select
//     // built scrolling into apollopanel selection - SUZ
//     // scrolls *vertically* to selection
//   }

  private void selectFeatures(Vector features, TierViewI view) {
    getSelectionManager().select(features, view);
  }

  /* When a feature is deleted, we need to deselect it.  Otherwise,
   * it's still thought to be the current selection, and bad things can
   * happen (clicking on an empty blue area will throw an exception). */
  private void deselect() {
    getSelectionManager().clearSelections();
  }
  private SelectionManager getSelectionManager() {
    return getCurationState().getSelectionManager();
  }

  private GuiCurationState getCurationState() { return curationState; }

  private String getUserName() {
    return UserName.getUserName();
  }  
  
  public void generateEditTransaction(Map<Integer, FeatChangeData> topLevelFeats, CompoundTransaction ct)
  {
    //System.out.println("topLevelFeats.size():\t" + topLevelFeats.size());
    for (Map.Entry<Integer, FeatChangeData> entry : topLevelFeats.entrySet()) {
      FeatChangeData data = entry.getValue();
      FeatureSetI parent = data.oldFeat.getParent();
      ct.addTransaction(new DeleteTransaction(data.oldFeat, parent));
      for (SeqFeatureI newFeat : data.newFeats) {
        if (newFeat.size() > 0) {
          ct.addTransaction(new AddTransaction(this, newFeat));          
        }
      }
    }
    fireCompoundTransaction(ct);
    fireSingleChange(new AnnotSessionDoneEvent(this));
  }
  
  /** Creates add/delete transactions for the top level feature edits.
   * A modification to a top level feature's descendent will cause the
   * original and the modified top level feature to be swapped (makes
   * for more cleaner and more elegant transaction support)
   * 
   * @param topLevelFeats - features that have been modified (or a descendant has been modified)
   */
  public void generateEditTransaction(Map<Integer, FeatChangeData> topLevelFeats)
  {
    CompoundTransaction ct = new CompoundTransaction(this);
    generateEditTransaction(topLevelFeats, ct);
  }

  /** Return the top level feature for a given SeqFeatureI object
   * 
   * @param feat - feature to return the top level feature
   * @return top level feature
   */
  public SeqFeatureI getTopLevelFeat(SeqFeatureI feat)
  {
    while (feat != null && !feat.isAnnotTop()) {
      feat = feat.getParent();
    }
    return feat;
  }

  public void generateFeatChangeData(Map<Integer, FeatChangeData> topLevelFeats, SeqFeatureI topLevelFeat)
  {
    generateFeatChangeData(topLevelFeats, topLevelFeat, false);
  }
  
  /** Clone a top level feature and inserts the data into a FeatChangeData object, which keeps track
   *  of the unmodified feature and newly added features
   *  
   * @param topLevelFeats - features that have been modified (or a descendant has been modified)
   * @param topLevelFeat - top level feature to be cloned and added to topLevelFeats
   */
  public SeqFeatureI generateFeatChangeData(Map<Integer, FeatChangeData> topLevelFeats, SeqFeatureI topLevelFeat,
      boolean deepCopy)
  {
    if (topLevelFeats.containsKey(topLevelFeat.hashCode())) {
      return null;
    }
    SeqFeatureI clone = topLevelFeat.cloneFeature(); //deepCopy ? topLevelFeat.deepCopy() : topLevelFeat.cloneFeature();
    FeatChangeData data = new FeatChangeData();
    data.oldFeat = clone;
    data.newFeats.add(topLevelFeat);
    topLevelFeats.put(topLevelFeat.hashCode(), data);
    return clone;
  }
  
  public static void setDoOneLevelAnnots(boolean doOne) {
    DO_ONE_LEVEL_ANNOTS = doOne;
  }

  /** Helper class for keeping track of changed top level annots
   * 
   */
  public class FeatChangeData
  {
    public SeqFeatureI oldFeat;
    public List<SeqFeatureI> newFeats = new LinkedList<SeqFeatureI>();
  }

}

   // should this be an add?? i think so
    //TransactionSubpart sp = TransactionSubpart.SEQUENCING_ERROR;
    //AnnotationUpdateEvent aue = new AnnotationUpdateEvent(this,annot,sp);
    // annot -> genomic seq?? cur set?
    //AnnotationAddEvent aae = AnnotationAddEvent(this,annot,sp); 
    // seq edit being created in 2 places AbsSeq & here - centralize!
    //AddTransaction at = new AddTransaction(this,annot,sp,seqEdit);
    // a seq edit is a seq feat??? thats how chado does it...
    // its really a one level annotation under the covers
//    AddTransaction at = new AddTransaction(this,seqEdit); // ?????
//   at.setResidues(seqEdit.getResidue());
    // at.editModel ???
    // for update have to know where the actual position is - can be many sequence
    // errors at many positions - this is insufficient - need in,del,sub
    //aae.setNewSequencingErrorPosition(pos); 
    // a.setOldSeqEdit(oldSeqEdit); // undo
   // for undo need to get seq error position pre edit
    // transaction for seq edit (instead of feat trans?)
    // transaction.editModel should really do this
    //boolean added = seq.addSequencingErrorPosition(operation,pos,residue);
     // for now get name adapter from exon/annot - really should configure 
    // name adapter for indels in tiers file - but then will show up in annot
    // info types list - need to have option of supressing from annot info
    //AnnotatedFeatureI firstAnnot = annots.getFeature(0).getAnnotatedFeature();
    //ApolloNameAdapterI na = getNameAdapter(firstAnnot);
