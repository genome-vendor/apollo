package apollo.gui.annotinfo;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Date;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

import junit.framework.TestCase;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.editor.AnnotationReplaceEvent;
import apollo.editor.AnnotationUpdateEvent;
import apollo.editor.FeatureChangeEvent;
import apollo.editor.Transaction;
import apollo.editor.TransactionSubpart;
import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.gui.ApolloFrame;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.Style;
import apollo.gui.Controller;
import apollo.gui.ControlledObjectI;
import apollo.gui.EditWindowI;
import apollo.gui.SelectionManager;
import apollo.gui.FeatureNavigationI;
import apollo.gui.featuretree.FeatureTreePanel;
import apollo.gui.event.*;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.GuiUtil;
import apollo.util.SwingMissingUtil;
import apollo.util.SeqFeatureUtil;
import apollo.config.ApolloNameAdapterI;

import org.apache.log4j.*;

/**
 * FeatureEditorDialog
 *
 * Editor for comments, gene and transcript names, etc
 *
 * Editing is disabled in browser mode.
 * In browser mode just used to view the comments.

 FeatureNavigationI interface is for internal FeatureTreePanel to call featureSelected
 on FeatureEditorDialog.
 */
public class FeatureEditorDialog extends JFrame implements
  ControlledObjectI,
  AnnotationChangeListener,
  FeatureSelectionListener,
  DataLoadListener,
  FeatureNavigationI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FeatureEditorDialog.class);

  /** Whether the mode is read-only - false if it allows editing */
  private static boolean isReadOnly = !Config.isEditingEnabled();
  private final static String warn_message
    = (" extends beyond the current sequence region,\n" +
       "so any changes you make to it may not get saved in the database.\n" +
       "Are you sure you want to save your changes?");
  protected static Color bgColor;

  /**
   * Keep track of the FeatureEditorDialog windows so we can kill them all when requested
   */
  private static ArrayList feds = new ArrayList(5);
  
  private final static int textEdHeight = 665; // 600
  private final static Dimension textEdDimForEditing
    = new Dimension(1100,textEdHeight+60);
  /** Text editor in readonly has less stuff so it's smaller */
  private final static Dimension textEdDimReadOnly
    = new Dimension(550,textEdHeight);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  Controller controller;
  private SelectionManager selectionManager;
  private boolean selectIsFromTree = false;

  private Box buttonPanel;
  //  private JButton saveButton;
  private JButton cancelButton;
  private JButton undoButton; // experimental
  private JCheckBox followSelectionCheckBox;

  private GeneEditPanel genePanel;            // gene editor
  private FeatureTreePanel treePanel;
  // every annotation in this curation
  private StrandedFeatureSetI annots;
  private SeqFeatureI orig_feature = null;
  private AnnotatedFeatureI orig_annot;

  /** Has state related to species/szap that fed has sprung from */
  private GuiCurationState curationState;

  /** true if amidst undo */
  private boolean isUndoing = false;

  public static void showTextEditor(SeqFeatureI sf, GuiCurationState s) {
    FeatureEditorDialog fed = new FeatureEditorDialog(sf,s);
    feds.add(fed);
  }


  /** CONSTRUCTOR */
  private FeatureEditorDialog(SeqFeatureI sf, GuiCurationState s) {
    this.curationState = s;
    setController(curationState.getController());
    setSelectionManager(curationState.getSelectionManager());
    isReadOnly = !Config.isEditingEnabled();
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          e.getWindow().setVisible(false); } } );

    jbInit();
    attachListeners();
    setFeature(sf);
    show();
    setVisible(true);
    // Might just be linux, but FED gets iconified on close
    if (getState()==Frame.ICONIFIED)
      setState(Frame.NORMAL);
  }

  GuiCurationState getCurationState() { return curationState; }

  /** populates the annots vector with the current set of top level annots
      (e.g. gene,transposon,...) */
  private void updateAnnotList() {
    annots = curationState.getCurationSet().getAnnots();
    treePanel.setFeatureSet(annots);
  }

  /** Called from constructor */
  private void setFeature (SeqFeatureI feature) {
    setFeature(feature, true);
  }

  /** feature needs to be a Gene or a descendant of a Gene.
      If it gets a descendant of a Gene, it just finds the Gene ancestor */
  private void setFeature(SeqFeatureI feature, boolean clear) {
    orig_feature = feature;
    // populates the annots vector with the current set of genes
    // annots vector is used for the gene list
    if (clear)
      updateAnnotList();

    // This seems to trigger if a new annot was created after AIE was open--feature
    // is null, even though the new annot DOES appear in the tree.  It would be
    // better to select nothing than to select (confusingly) the first annot.
    // When would we want to select the first annot?
    if (feature == null &&
        annots != null &&
        annots.size() > 0) {
      feature = annots.getFeatureAt (0);
    }
    if (feature != null) {
      // feature is something like CG5848-RB exon 6
//       if (orig_annot != null) {
//         /* make sure we have the most recent data from any
//            existing edits to a different feature */
//         genePanel.updateFeature();
//       }
      orig_annot = getAnnot(feature);
      if (orig_annot != null) {
        setTitle(orig_annot.getName() + " Annotation Information");
        genePanel.loadAnnotation(orig_annot);
        if (!selectIsFromTree) {
          if (getTranscript() == null)
            treePanel.findObject(orig_annot);
          else
            treePanel.findObject(getTranscript());
        }
        // To help in debugging
        if (getTranscript() != null)
          logger.info("Annotation Info Editor: looking at " + getTranscript().getName() + " (type " + orig_annot.getFeatureType() + ")");
      }
    }
  }

  /**
      Listen to selections from the tree view
      and respond to FeatureSelection events.
      This is the method from the interface FeatureNavigationI that allows
      FeatureTreePanel to set the selection of FeatureEditorDialog.
      This is solely used by FeatureTreePanel
  */
  public void featureSelected(SeqFeatureI sf) {
    selectIsFromTree = true;
    if (selectIfChanged(sf))
      fireFeatureSelectionEvent(sf); // only fires if follow selection checked
    selectIsFromTree = false;
  }

  private boolean selectIfChanged(SeqFeatureI sf) {
    // this only examines first element - if first element isnt an annot
    // should probably check other parts of selection
    // returns null if sf not a gene or gene descendant
    AnnotatedFeatureI newAnnot = getAnnot(sf);
    boolean update = (newAnnot != null);
    if (update) {
      update = (newAnnot != orig_annot);
      if (!update) {
        // check for a change to the transcript
        AnnotatedFeatureI newTrans = getTranscript(orig_annot, sf);
        AnnotatedFeatureI currentTrans = getTranscript();
        update = newTrans != currentTrans;
      }
    }
    if (update) {
      setFeature(sf, false);
    }
    return update;
  }

  private void fireFeatureSelectionEvent(SeqFeatureI sf) {
    // Send a focus event to update the position in the Overview panel
    // should a selection do base focus by default?
    if (allowExternalSelection()) {
      BaseFocusEvent evt = new BaseFocusEvent(this, sf.getStart(), sf);
      controller.handleBaseFocusEvent(evt);
      selectionManager.select(sf,this);
    }
  }


  protected Transcript getTranscript() {
    return getTranscript(orig_annot, orig_feature);
  }

  // SeqFeature sf should be an exon (could it be anything else??)
  private Transcript getTranscript(AnnotatedFeatureI gene,
                                   SeqFeatureI sf) {
    Vector transcripts = gene.getFeatures();
    AnnotatedFeatureI annot = getTopAnnot(transcripts, sf);
    if (annot != null && (annot instanceof Transcript))
      return ((Transcript) annot);
    else
      // assume that there is at least one transcript?
      return (Transcript) gene.getFeatureAt(0);
  }

  /** A more generic parentGene, return AnnotatedFeatureI?
      "topAnnot" is analogous to gene for non genes
      Returns null if top non holder is not an annot.
   */
  private AnnotatedFeatureI getTopAnnot(Vector list, SeqFeatureI sf) {
    // this doesnt work Gene is actually a holder
    while (sf != null && !list.contains(sf)) {
      sf = sf.getParent();
    }
    // only AnnotatedFeatureI's are true for isAnnot()
    if (sf != null && sf.hasAnnotatedFeature())
      return sf.getAnnotatedFeature();
    else {
      return null;
    }
  }


  /**
   * builds GUI
   */
  private void jbInit() {
    genePanel = new GeneEditPanel(this, isReadOnly);
    bgColor = genePanel.getBackgroundColor();

    if (!isReadOnly) {
      // No longer used--changes are "committed" instantly
      //      saveButton = new JButton("Commit");
      //      saveButton.setBackground (Color.white);
      //cancelButton = new JButton("Cancel");
      undoButton = new JButton("Undo");
      undoButton.setBackground(Color.white);
    }
    cancelButton = new JButton("Close");

    cancelButton.setBackground (Color.white);
    followSelectionCheckBox = new JCheckBox("Follow selection",false);
    followSelectionCheckBox.setBackground(bgColor);

    buttonPanel = new Box(BoxLayout.X_AXIS);
    buttonPanel.setBackground (bgColor);
    buttonPanel.add(Box.createHorizontalStrut(5));
    buttonPanel.add(followSelectionCheckBox);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(Box.createHorizontalStrut(10));
    if (!isReadOnly) {
      //buttonPanel.add(saveButton);
      buttonPanel.add(undoButton);
      // Move cancel button to the right edge
      buttonPanel.add(Box.createHorizontalStrut(300));
    }
    buttonPanel.add(cancelButton);

    Box navBox = new Box(BoxLayout.X_AXIS);
    // "this" is FeatureNavigationI - see featureSelected(SeqFeat)
    treePanel = new FeatureTreePanel(this,getCurationState());
    navBox.add(Box.createHorizontalStrut(12));
    navBox.add(treePanel);
    navBox.add(Box.createHorizontalStrut(8));
    navBox.add(genePanel);
    navBox.add(Box.createHorizontalStrut(12));

    Dimension treeDim = new Dimension (200, textEdHeight - 100);
    treePanel.setPreferredSize(treeDim);
    treePanel.setMinimumSize(treeDim);

    this.getContentPane().setLayout(new BoxLayout(this.getContentPane(),
                                    BoxLayout.Y_AXIS));
    this.getContentPane().setBackground(bgColor);
    this.getContentPane().setForeground(Color.black);
    this.getContentPane().add(Box.createVerticalStrut(12));
    this.getContentPane().add(navBox);
    this.getContentPane().add(Box.createVerticalStrut(12));
    this.getContentPane().add(buttonPanel);

    // Set the size of the Annot Info editor
    Dimension sz = (Config.isEditingEnabled() ?
                    textEdDimForEditing : textEdDimReadOnly);
    setSize(GuiUtil.fitToScreen(sz)); // util?
  }


  /** disposes commentator, then calls super.dispose (Window)
   */
  public void dispose() {
    // apolloFrame.repaint(); is this needed??
    // destroy the comment frame ??
    genePanel.disposeCommentator();
    super.dispose();
  }

  public void windowClosed(WindowEvent e) {
  }


  /** Dispose all FeatureEditorDialogs currently in existence.
      Keeps track of all instances for this purpose */
  public static void disposeAllEditors() {
    for (Iterator it = feds.iterator(); it.hasNext();) {
      FeatureEditorDialog fed = (FeatureEditorDialog) it.next();
      fed.dispose();
      fed = null;
    }
    feds.clear();
  }


  void refreshCommentField() {
    genePanel.refreshCommentField();
  }

  // No longer used--this now happens elsewhere
//   /** Name change may be for annotation and/or transcript(s) */
//   private void setupNameChange (AnnotatedFeatureI oldAnnot,
//                                 AnnotatedFeatureI newAnnot,
//                                 ApolloNameAdapterI name_adapter) {

//     String new_name = newAnnot.getName();
//     String old_name = oldAnnot.getName();
//     if (!old_name.equals (new_name)) {
//       // only add synonym if its not a temporary name
//       if (old_name.indexOf ("temp") < 0) {
//         newAnnot.addSynonym (old_name);
//       }

//       // Transcript names are changed as a result of change to parent annotation's name
//       Vector transcripts = newAnnot.getFeatures();//Transcripts();
//       int trans_count = transcripts.size();
//       String curation_name = curationState.getCurationSet().getName();
//       for (int i = 0; i < trans_count; i++) {
//         Transcript t = (Transcript) transcripts.elementAt(i);
//         String trans_name = name_adapter.generateName(annots,
//                                                       curation_name,
//                                                       t);
//         String prev_name = t.getName();
//         t.setName(trans_name);
//         if (prev_name.indexOf ("temp") < 0) {
//           t.addSynonym (prev_name);
//         }
//       }
//     }
//     // check for changes to the names of the transcripts
//     int annotSize = newAnnot.size();
//     for (int i = 0; i < annotSize; i++) {
//       Transcript oldt = (Transcript) oldAnnot.getFeatureAt(i);
//       Transcript newt = (Transcript) newAnnot.getFeatureAt(i);

//       String prev_tname = oldt.getName();
//       String new_tname = newt.getName();
//       if (!prev_tname.equals (new_tname)) {
//         if (prev_tname.indexOf ("temp") < 0) {
//           newt.addSynonym (prev_tname);
//         }
//         // Tell transcript to update its cDNA and peptide names based on the
//         // new transcript name.
//         // this is taken care of on trans name change - 
//         // FlyNameAdapter.setTranscriptNameFromAnnot calls changeSeqNames 
//         // not needed for rice - trigger does it
//         //name_adapter.changeSeqNames(newt);

//       }
//     }
//   }

  /**
   * attaches listeners to active GUI components
   */
  void attachListeners() {

    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { dispose(); }  } ); // close

    if (!isReadOnly) {
      undoButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { undo(); } } );
    }
  }

  /** undo directly to tran manager - this should replace ACE undo */
  void undo() {
    if (curationState.getTransactionManager().numberOfTransactions() == 0) {
      // This is the easy part--the hard part is (re)enabling the Undo button
      // whenever something has happened that could be undone!
      //      undoButton.setEnabled(false);
      logger.warn("Nothing to undo!");
    }
    isUndoing = true;
    curationState.getTransactionManager().undo(this);
    isUndoing = false;
  }

  /** Returns true if amidst an undo. this needs to be known in FEP.loadAnnot as if
      amidst undo it shouldnt check focus driven edits (will revert undo) */
  boolean isUndoing() { return isUndoing; }

  protected AnnotatedFeatureI getAnnot(SeqFeatureI sf) {
    return getTopAnnot(annots.getFeatures(), sf);
  }

  private JLabel initLabel (String text) {
    JLabel label = new JLabel(text);
    label.setFont(new Font("Dialog", Font.PLAIN, 12));
    label.setBackground(Config.getAnnotationBackground());
    label.setForeground(Color.black);
    return label;
  }

  // methods for controller interface
  public void setController(Controller c) {
    controller = c;
    controller.addListener(this);
  }

  public Controller getController() {
    return controller;
  }

  /** Set selection manager, controller for selection. Set up
   * listener to transcript panel feature list selection.
   * Selecting a a gene in genePanel causes a transcript to get selected
   * which then causes this inner listener to be called.
   * oldAnnot has to get set to new gene here i believe
   */
  public void setSelectionManager(SelectionManager sm) {
    selectionManager = sm;
  }

  public void addNotify() {
    super.addNotify();
  }

  public Object getControllerWindow() {
    //return SwingMissingUtil.getWindowAncestor((Component)this);
    // I think FeatureEditorDialog is its own conrolling window
    return this;
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  /** Calls setFeature with new annotation. This triggers a recloning of the
      gene (via GeneEditPanel.loadSelectedFeature), which wipes out any
      uncommitted edits to the old clone. This is erroneous. The old clone
      needs to be preserved or copied. */
  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    if (evt.getSource() == this) 
      return false;
    
    else if (evt.isCompound() || evt.isEndOfEditSession()) {
      setFeature(evt.getAnnotTop(), true);
      return true;
    }

    return false;
  }

  // Handle the selection event
  // (feature was selected in another window--select it here)
  public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
    if (allowExternalSelection()) {
      if (isVisible() &&
          evt.getSource() != this &&
          evt.getFeatures().size() > 0) {
        selectIfChanged(evt.getFeatures().getFeature(0));
        return true;
      }
    }
    return false; // why false
  }

  public boolean handleDataLoadEvent(DataLoadEvent e) {
    disposeAllEditors();
    return true;
  }

  private boolean allowExternalSelection() {
    return followSelectionCheckBox.isSelected();
  }

  protected void fireAnnotEvent(AnnotationChangeEvent ace) {
    getController().handleAnnotationChangeEvent(ace);
  }

  protected ApolloNameAdapterI getNameAdapter(AnnotatedFeatureI annot) {
    return curationState.getNameAdapter(annot);
  }

  Style getStyle() { return curationState.getStyle(); }



  public static void testNameUndo(TestCase testCase) {
    GuiCurationState cs =  
      apollo.gui.synteny.CurationManager.getCurationManager().getActiveCurState();
    testCase.assertNotNull(cs.getCurationSet());
    SeqFeatureI annot = cs.getCurationSet().getAnnots().getFeatureAt(0);
    FeatureEditorDialog fed = new FeatureEditorDialog(annot,cs); // cur state
    fed.genePanel.testNameUndo(testCase);
  }
}
