package apollo.gui.detailviewers.sequencealigner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import apollo.dataadapter.DataLoadEvent;
import apollo.dataadapter.DataLoadListener;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Transcript;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.Selection;
import apollo.gui.SelectionManager;
import apollo.gui.Transformer;
import apollo.gui.event.BaseFocusEvent;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.event.FeatureSelectionListener;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;

public class BaseFineEditor extends JFrame
  implements FeatureSelectionListener,
  ControlledObjectI,
  AnnotationChangeListener {

  private static final Color [] colorList = {
        new Color(0,0,0),
        new Color(255,255,0),
        new Color(0,255,255),
        new Color(255,0,255),
        new Color(255,0,0),
        new Color(0,255,0),
        new Color(0,0,255)};
  private static int colorIndex = 0;

  /** RAY: The static list of all active EDEs */
  private static ArrayList baseEditorInstanceList = new ArrayList(5);

  /** RAY: This is where the annotations get added */
  protected BaseEditorPanel editorPanel = null;
  protected BaseEditorPanel resultPanel = null;
  /** RAY: The box at the bottom of the EDE which shows the selected transcript */
  private TranslationViewer translationViewer;
  /** RAY: This is the box that holds the Scrollable part of the EDE */
  private JViewport viewport;

  protected JLabel lengthLabel;
  /** RAY: The drop down menu with the list of transcripts in it */
  private JComboBox transcriptComboBox;

  StrandedZoomableApolloPanel szap;
  Transformer transformer;
  private FineEditorScrollListener scrollListener;
  Color indicatorColor;
  JPanel colorSwatch;
  SelectionManager selection_manager;

  protected JButton findButton;
  protected JButton clearFindsButton;
  protected JButton goToButton;  
  protected JCheckBox showIntronBox;  // Need?
  private JCheckBox followSelectionCheckBox;
  protected JButton upstream_button;
  protected JButton downstream_button;

  private AnnotatedFeatureI currentAnnot;//Transcript
  protected Transcript neighbor_up;
  protected Transcript neighbor_down;
  private GuiCurationState curationState;

  public static void test(
          GuiCurationState curationState,
          int strand,
          Selection selection,
          String type) {
    
      JFrame frame = new JFrame("TopLevelDemo");

      MultiSequenceAlignerFrame msa 
        = MultiSequenceAlignerFrame.makeAligner(curationState, strand,
                                                selection, type);
      msa.setPreferredSize(new Dimension(1000, 800));
      msa.pack();
      msa.setVisible(true);
      
      
      AnnotatedFeatureI gai = getFirstAnnot(selection);
      
      if (gai != null) {
         msa.getPanel().scrollToBase(gai.getStart());
         msa.getPanel().setSelection(gai);
      }
      
  }
  
  /** Returns first AnnotatedFeatureI in Selection on strand clicked on,
  null if there is none.
  Exon, Transcript, and Gene are all AnnotatedFeatureI's */
  public static AnnotatedFeatureI getFirstAnnot(Selection selection) {
    for (int i=0; i < selection.size() ;i++) {
      SeqFeatureI feat = selection.getSelectedData(i);
      if (feat instanceof AnnotatedFeatureI ) {
        // If this is an exon, get the parent transcript
        // XXX: uncoment if you want to start at the beginning of the transcript
        //if (feat instanceof Exon)
          //feat = feat.getRefFeature();
        return (AnnotatedFeatureI)feat;
      }
    }
    return null;
  }
  
  
  public static void showBaseEditor(AnnotatedFeatureI editMe,GuiCurationState curationState,
                                    SeqFeatureI geneHolder) {
    BaseFineEditor bfe = new BaseFineEditor(editMe,curationState,geneHolder);
    baseEditorInstanceList.add(bfe);
  }

  public static void showBaseEditor(AnnotatedFeatureI editMe,GuiCurationState curationState,
          SeqFeatureI geneHolder, Set<SeqFeatureI> selectedResults) { 
    BaseFineEditor bfe = new BaseFineEditor(editMe,curationState,geneHolder, selectedResults);
    //bfe.addResults(selectedResults);
    ArrayList v = baseEditorInstanceList;
    baseEditorInstanceList.add(bfe);
  }

  private void addResults(Set<SeqFeatureI> results) {
	  editorPanel.addResults(results);
  }
  
  private BaseFineEditor(AnnotatedFeatureI editMe,GuiCurationState curationState,
                         SeqFeatureI geneHolder) {

    super((editMe.isForwardStrand() ? "Forward" : "Reverse") + " Strand Exon Editor");
    this.curationState = curationState;
    curationState.getController().addListener(this);
    curationState.getController().addListener(new BaseEditorDataListener());
    szap = curationState.getSZAP();
    transformer = szap.getScaleView().getTransform();
    int seqStart = curationState.getCurationSet().getLow();
    int seqEnd = curationState.getCurationSet().getHigh();
    
    //FeatureSetI annFeatSet=((DrawableFeatureSet)view.getDrawableSet()).getFeatureSet();
    editorPanel = new BaseEditorPanel(curationState,
                                      this,
                                      !editMe.isForwardStrand(),
                                      seqStart,
                                      seqEnd,
                                      geneHolder);

    initGui(editMe); // cant do til after editorPanel made
    // cant do this til after initGui (editorPanel needs to know size)
    editorPanel.displayAnnot(editMe);
    attachListeners();
    showEditRegion();
    displayAnnot(getTransOrOneLevelAnn(editMe));
    translationViewer.repaint();
    setVisible(true);
    // Might just be linux, but bofe gets iconified on close
    if (getState()==Frame.ICONIFIED)
      setState(Frame.NORMAL);
  }
  
  private BaseFineEditor(AnnotatedFeatureI editMe,GuiCurationState curationState,
          SeqFeatureI geneHolder, Set<SeqFeatureI> selectedResults) {

    super((editMe.isForwardStrand() ? "Forward" : "Reverse") + " Strand Exon Editor");
    this.curationState = curationState;
    curationState.getController().addListener(this);
    curationState.getController().addListener(new BaseEditorDataListener());
    szap = curationState.getSZAP();
    transformer = szap.getScaleView().getTransform();
    int seqStart = curationState.getCurationSet().getLow();
    int seqEnd = curationState.getCurationSet().getHigh();
    CurationSet s = curationState.getCurationSet();
    //FeatureSetI annFeatSet=((DrawableFeatureSet)view.getDrawableSet()).getFeatureSet();
    
    // REFACTOR BaseEditorPanel - (what allows for selection and modification of the annotations?)
    // create BaseViewPanel -> have BaseEditorPanel extend it
    // take the header outside of the editorPanel (is it really inside right now?)   
    // create a new panel using BaseViewPanel
    
    /*
     * The viewable components of the new BaseFineEditor (should this be renamed?):
     * 
     *  1) The Genomic Strand
     *  2) The Annotations (will these always be a copy of the genomic strand?) 
     *         q1) Can annotations also be nucliotides? - A: Yes. Create a separate view for those.
     *         q2) Does it make sense to view both the forward and reverse strand at once?
     *         q3) How do we know what to load - A: highlight the region we want to look at?
     *         q4) Will we expect users to be able to view many annotations at once?
     *         q4) How will the user know which annotation they are looking at? - A: Display the name when hovering over?
     *  3) The Results 
     *         q1) See above questions.
     *  4) Result Score information
     *  5) An overview - Right now this is at the transcript (gene model?) level
     *         q1) What should be contained in this view?
     *         
     * 
     */
    
    
    // I think that right now every base in the ede is a copy of the genomic, will that always be the case?
    // is there ever sequence associated with an annotation.
    editorPanel = new BaseEditorPanel(curationState,
                       this,
                       !editMe.isForwardStrand(),
                       seqStart,
                       seqEnd,
                       geneHolder);

    addResults(selectedResults);
    initGui(editMe); // cant do til after editorPanel made
    // cant do this til after initGui (editorPanel needs to know size)
    editorPanel.displayAnnot(editMe);
    attachListeners();
    showEditRegion();
    displayAnnot(getTransOrOneLevelAnn(editMe));
    translationViewer.repaint();
    setVisible(true);
    // Might just be linux, but bofe gets iconified on close
    if (getState()==Frame.ICONIFIED)
        setState(Frame.NORMAL);
  }

  /** ControlledObjectI interface - but controller now comes from curationState */
  public void setController(Controller controller) {
    //this.controller = controller;
    //controller.addListener (this);
  }

  public Controller getController() {
    return curationState.getController();
  }

  public Object getControllerWindow() {
    return this;
  }

  // Although they are removed we need to remove from hash in controller
  public boolean needsAutoRemoval() {
    return true;
  }

  public void setSelectionManager (SelectionManager sm) {
    this.selection_manager = sm;
  }

  public SelectionManager getSelectionManager () {
    return curationState.getSelectionManager();//this.selection_manager;
  }

  private void removeComponents(Container cont) {
    Component[] components = cont.getComponents();
    Component comp;

    for (int i = 0; i < components.length; i++) {
      comp = components[i];
      if (comp != null) {
        cont.remove(comp);
        if (comp instanceof Container)
          removeComponents((Container) comp);
      }
    }
  }

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    if (this.isVisible() && evt.getSource() != this) {
      if (evt.isCompound()) {
        for (int i = 0; i < evt.getNumberOfChildren(); ++i) {
          handleAnnotationChangeEvent(evt.getChildChangeEvent(i));
        }
        return true;
      }
      
      if (evt.isEndOfEditSession()) {
        // Dont want scrolling on ann change, also sf for redraw is the
        // gene, so it just goes to 1st trans which is problematic.
        displayAnnot(currentAnnot);
        repaint(); // this is the needed repaint after the changes
        return true;
      }
      
      /*
      AnnotatedFeatureI gene = evt.getChangedAnnot();
      for (Object o : szap.getAnnotations().getFeatures()) {
        AnnotatedFeatureI feat = (AnnotatedFeatureI)o;
        if (feat.getId().equals(gene.getId())) {
          //gene = feat;
          break;
        }
      }
      */
      
      if (evt.isAdd()) {
        AnnotatedFeatureI annotToDisplay = null;
        AnnotatedFeatureI gene = evt.getChangedAnnot();
        int trans_count = gene.size();
        for (int i = 0; i < trans_count; ++i) {
          Transcript t = (Transcript) gene.getFeatureAt (i);
          if (evt.isUndo()) {
            //System.out.printf("Attaching %s(%d)\n", t, t.hashCode());
            editorPanel.attachAnnot(t);
            if (currentAnnot != null && t.getName().equals(currentAnnot.getName())) {
              annotToDisplay = t;
            }
          }
        }
        if (annotToDisplay == null) {
          annotToDisplay = (AnnotatedFeatureI)evt.getChangedAnnot().getFeatureAt(0);
        }
        displayAnnot(annotToDisplay);
        return true;
      }
      
      if (evt.isDelete()) {
        AnnotatedFeatureI gene = evt.getChangedAnnot();
        int trans_count = gene.size();
        for (int i = 0; i < trans_count; ++i) {
          if (evt.isUndo()) {
            Transcript t = (Transcript) gene.getFeatureAt (i);
            //System.out.printf("Detaching %s(%d)\n", t, t.hashCode());
            editorPanel.detachTranscript(t);
          }
        }
        return true;        
      }

      /*
      // getAnnotation() can be null after a delete - return if null?
      SeqFeatureI sf = evt.getAnnotTop();
      
      // If the annotation event is not for the editors strand then return
      boolean is_reverse = (sf.getStrand() == -1);
      if (editorPanel.getReverseStrand() != is_reverse)
        return false;

      if (evt.isDelete() && evt.isRootAnnotChange()) {
        AnnotatedFeatureI gene = evt.getChangedAnnot();
        int trans_count = gene.size();
        for (int i = 0; i < trans_count; i++) {
          Transcript t = (Transcript) gene.getFeatureAt (i);
          editorPanel.detachTranscript (t);
          if (t == currentAnnot)
            currentAnnot = null;
        }
      } else if (evt.isDelete() && evt.isTranscriptChange()) {
        Transcript t = (Transcript) evt.getChangedAnnot();
        editorPanel.detachTranscript (t);
        if (t == currentAnnot)
          currentAnnot = null;
      } else if ((evt.isAdd() || evt.isSplit() || evt.isMerge()) &&
                 evt.isRootAnnotChange()) {
        AnnotatedFeatureI gene = evt.getChangedAnnot();
        int trans_count = gene.size();
        for (int i = 0; i < trans_count; i++) {
          Transcript t = (Transcript) gene.getFeatureAt (i);
          int tier = editorPanel.attachAnnot (t);
        }
      } else if (evt.isAdd() && evt.isTranscriptChange()) {
        Transcript t = (Transcript) evt.getChangedAnnot();
        int tier = editorPanel.attachAnnot (t);
      }
      // Exon split or intron made - same thing isnt it?
      else if (evt.isAdd() && evt.isExonChange()) {
        Transcript t = (Transcript) evt.getParentFeature();
        // removing and adding transcript will force editorPanel
        // to pick up new exon
        editorPanel.detachTranscript(t);
        editorPanel.attachAnnot(t);
      }
      else if (evt.isDelete() && evt.isExonChange()) {
        Transcript t = (Transcript) evt.getParentFeature();
        // removing and adding transcript will force editorPanel to remove exon
        editorPanel.detachTranscript(t);
        editorPanel.attachAnnot(t);
        // or editorPanel.removeFeature((Exon)evt.getSecondFeature());
      } 
//       else if (evt.isReplace()) { //&&evt.isTopAnnotChange - always true of replace 
//         AnnotatedFeatureI old_gene = evt.getReplacedFeature();
//         AnnotatedFeatureI new_gene = (AnnotatedFeatureI)evt.getChangedFeature();
//         int trans_count = old_gene.size();
//         /* This does assume that there are no changes in the number
//            of transcripts between the old and the new. Might be safer
//            to separate the 2 loops */
//         for (int i = 0; i < trans_count; i++) {
//           Transcript t;
//           t = (Transcript) old_gene.getFeatureAt (i);
//           editorPanel.detachTranscript (t);
//           t = (Transcript) new_gene.getFeatureAt (i);
//           int tier = editorPanel.attachTranscript (t);
//         }
//         // This is so that we don't select a different transcript
//         AnnotatedFeatureI current_gene = (current_transcript != null ?
//                                           current_transcript.getGene() : null);
//         if (current_gene != null && current_gene == old_gene) {
//           int index = current_gene.getFeatureIndex(current_transcript);
//           current_transcript = (Transcript) new_gene.getFeatureAt(index);
//         }
//       } 
      // this isnt possible - all replaces are for top annots
//       else if (evt.isReplace() && evt.isTranscriptChange()) {
//         Transcript old_trans = (Transcript)evt.getReplacedFeature();
//         Transcript new_trans = (Transcript)evt.getChangedFeature();
//         editorPanel.detachTranscript (old_trans);
//         int tier = editorPanel.attachTranscript (new_trans);
//         // This is so that we don't select a different transcript
//         if (current_transcript != null && current_transcript == old_trans) {
//           current_transcript = new_trans;
//         }
//       }
    }
    
    return true;
  }

  /** Handle the selection event
      (feature was selected in another window--select it here)
      This is also where selections from BaseFineEditor come in which
      are really internal selections.
      Only handle external selection if followSelection is checked */
    public boolean handleFeatureSelectionEvent (FeatureSelectionEvent evt) {
    if (!canHandleSelection(evt,this))
      return false;
    // now we do something, canHanSel filters for GenAnnIs
    AnnotatedFeatureI gai = (AnnotatedFeatureI)evt.getFeatures().getFeature(0);
    displayAnnot(getTransOrOneLevelAnn(gai));
    translationViewer.repaint();
    return true;
  }

  /** Does all the handle selection checks for BaseFineEditor and BaseEditorPanel */
  boolean canHandleSelection(FeatureSelectionEvent evt,Object self) {
    if ((noExternalSelection() && isExternalSelection(evt)) &&
        !evt.forceSelection())
      return false;
    if (evt.getSource() == self)
      return false;
    if (!this.isVisible())
      return false;
    if (evt.getFeatures().size() == 0)
      return false;
    SeqFeatureI sf = evt.getFeatures().getFeature(0);
    // if strand of selection is not our strand return
    boolean is_reverse = (sf.getStrand() == -1);
    if (is_reverse != editorPanel.getReverseStrand())
      return false;
    if ( ! (sf instanceof AnnotatedFeatureI))
      return false;//repaint transVw?
    else
      return true;
  }

  /** True if the selection comes from outside world. false if from this
      OR editorPanel */
  private boolean isExternalSelection(FeatureSelectionEvent e) {
    if (e.getSource() == this || e.getSource() == editorPanel) return false;
    return true;
  }

  private boolean noExternalSelection() {
    return !followSelectionCheckBox.isSelected();
  }

  public Color getIndicatorColor() {
    return indicatorColor;
  }


  /** puts up vertical lines in szap(main window) indicating the region EDE is
   *  displaying - make private? */
  private void showEditRegion() {
    colorIndex = (colorIndex + 1) % colorList.length;
    indicatorColor = colorList[colorIndex];
    colorSwatch.setBackground(indicatorColor);
    szap.addHighlightRegion(editorPanel,
                            indicatorColor,
                            editorPanel.getReverseStrand());
  }

  public void attachListeners() {
    // changeListener = new AnnotationChangeDoodad();
    addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                          szap.removeHighlightRegion(editorPanel);
                          szap.repaint();
                          BaseFineEditor.this.setVisible(false);
                        }
                      }
                     );
    clearFindsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editorPanel.setShowHitZones(false);
          clearFindsButton.setEnabled(false);
        }
      }
                                     );

    findButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editorPanel.showFindDialog();
        }
      }
                                     );
    goToButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editorPanel.showGoToDialog();
        }
      }
                                     );				     

    showIntronBox.addActionListener(new ActionListener() {
                                      public void actionPerformed(ActionEvent e) {
                                        translationViewer.
                                        setDrawIntrons(showIntronBox.isSelected());
                                      }
                                    }
                                   );
    upstream_button.addActionListener(new ActionListener() {
                                        public void actionPerformed(ActionEvent e) {
                                          selectAnnot(neighbor_up);
                                        }
                                      }
                                     );
    downstream_button.addActionListener(new ActionListener() {
                                          public void actionPerformed(ActionEvent e) {
                                            selectAnnot(neighbor_down);
                                          }
                                        }
                                       );
  }

  /** RAY: updates the horizontal bars in the apollo window 
        and the box for the transcript viewer in the EDE */
  private class FineEditorScrollListener implements ChangeListener {
    int oldstart = -1;
    int oldwidth = -1;

    public void stateChanged(ChangeEvent e) {
      szap.repaint();
      translationViewer.repaint();
    }
  }

  /** Dispose all editors currently in existence. Keeps track of all instances
      for this purpose */
  public static void disposeAllEditors() {
    for (Iterator it = baseEditorInstanceList.iterator(); it.hasNext();  )
      ((BaseFineEditor)it.next()).dispose();
    baseEditorInstanceList.clear();
  }

  private class BaseEditorDataListener implements DataLoadListener {
    public boolean handleDataLoadEvent(DataLoadEvent e) {
      if (!e.dataRetrievalBeginning())
        return false;
      disposeAllEditors();
      curationState.getController().removeListener(this);
      return true;
    }
  }

  public void dispose() {
    removeComponents(this);
    editorPanel.cleanup(); // 1.3.1 popup mem leak & remove listener
    szap.removeHighlightRegion(editorPanel);
    editorPanel = null;
    translationViewer = null;
    viewport.removeChangeListener(scrollListener);
    viewport = null;

    lengthLabel = null;
    //featureNameLabel = null;
    transcriptComboBox = null;

    szap = null;
    transformer = null;
    scrollListener = null;
    indicatorColor = null;
    colorSwatch = null;
    // changeListener = null;
    // removeWindowListener(windowListener);
    // windowListener = null;
    getController().removeListener(this);
    //getController() = null;
    //view = null;

    findButton = null;
    clearFindsButton = null;
    goToButton = null;
    upstream_button = null;
    downstream_button = null;

    super.dispose();
  }

  private void initGui(AnnotatedFeatureI annot) {
    translationViewer = new TranslationViewer(editorPanel);
    translationViewer.setBackground(Color.black);
    transcriptComboBox = new JComboBox();
    lengthLabel = new JLabel("Translation length: <no feature selected>");
    lengthLabel.setForeground(Color.black);
    findButton = new JButton("Find sequence...");
    clearFindsButton = new JButton("Clear search hits");
    // Disable until we actually get search results
    clearFindsButton.setEnabled(false);
    
    goToButton = new JButton("GoTo...");
    showIntronBox = new JCheckBox("Show introns in translation viewer",
                                  true);
    showIntronBox.setBackground (Color.white);
    followSelectionCheckBox = new JCheckBox("Follow external selection",false);
    followSelectionCheckBox.setBackground(Color.white);
    upstream_button = new JButton ();
    downstream_button = new JButton ();

    colorSwatch = new JPanel();

    setSize(824,500);
    JScrollPane pane = new JScrollPane(editorPanel);
    pane.setHorizontalScrollBarPolicy(
      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // RAY: SET SCROLL POLICY HERE! WAS HORIZONTAL_SCROLLBAR_NEVER

    //pane.setColumnHeaderView(new BaseFineEditorRowHeader(editorPanel)); // RAY: setRowHeader need to create column header
    pane.setColumnHeaderView(new BaseFineEditorHorizontalColHeader(editorPanel));
    viewport = pane.getViewport();
    colorSwatch.setPreferredSize(new Dimension(10,10));

    getContentPane().setBackground (Color.white);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(colorSwatch, "North");
    getContentPane().add(pane, "Center");

    Box transcriptListBox = new Box(BoxLayout.X_AXIS);
    JLabel tranLabel;
    // 1 LEVEL ANNOT
    if (annot.isAnnotTop())
      tranLabel = new JLabel("Annotation: ");
    else  // 3-level
      tranLabel = new JLabel("Transcript: ");
    tranLabel.setForeground(Color.black);
    transcriptListBox.add(Box.createHorizontalStrut(5));
    transcriptListBox.add(tranLabel);
    transcriptListBox.setBackground(Color.white);
    transcriptComboBox.setMaximumSize(new Dimension(300,30));
    transcriptListBox.add(transcriptComboBox);
    transcriptListBox.add(Box.createHorizontalGlue());
    transcriptListBox.add(Box.createHorizontalStrut(5));
    transcriptListBox.add(lengthLabel);
    transcriptListBox.add(Box.createHorizontalGlue());

    Box checkboxesTop = new Box(BoxLayout.X_AXIS);
    checkboxesTop.setBackground (Color.white);
    checkboxesTop.add(Box.createHorizontalStrut(5));
    checkboxesTop.add(findButton);    
    checkboxesTop.add(Box.createHorizontalStrut(10));
    checkboxesTop.add(clearFindsButton);
    checkboxesTop.add(Box.createHorizontalStrut(15));
    checkboxesTop.add(goToButton);
    checkboxesTop.add(Box.createHorizontalGlue());        
    Box checkboxesBottom = new Box(BoxLayout.X_AXIS);
    checkboxesBottom.add(showIntronBox);
    checkboxesBottom.add(Box.createHorizontalGlue());
    checkboxesBottom.add(Box.createHorizontalStrut(10));
    checkboxesBottom.add(followSelectionCheckBox);
    Box checkboxes = new Box(BoxLayout.Y_AXIS);
    checkboxes.add(checkboxesTop);
    checkboxes.add(checkboxesBottom);

    Box labelPanel = new Box(BoxLayout.Y_AXIS);
    labelPanel.setBackground(Color.white);
    labelPanel.add(transcriptListBox);
    labelPanel.add(Box.createVerticalStrut(5));
    labelPanel.add(checkboxes);

    Box navPanel = new Box(BoxLayout.Y_AXIS);
    navPanel.setBackground (Color.white);
    navPanel.add(upstream_button);
    navPanel.add(Box.createVerticalStrut(10));
    navPanel.add(downstream_button);
    navPanel.add(Box.createVerticalGlue());

    Box textBoxes = new Box(BoxLayout.X_AXIS);
    textBoxes.setBackground (Color.white);
    textBoxes.add(labelPanel);
    textBoxes.add(navPanel);

    Box detailPanel = new Box(BoxLayout.Y_AXIS);
    detailPanel.setBackground(Color.white);
    detailPanel.add(translationViewer);
    detailPanel.add(textBoxes);
    getContentPane().add(detailPanel, "South");

    validateTree();
    scrollListener = new FineEditorScrollListener();
    viewport.addChangeListener(scrollListener);

    transcriptComboBox.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_U &&
            (e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
          CurationManager.getActiveCurationState().getTransactionManager().undo(this);
        }
      }
    });

  }


  /** Both fires off selection event and displays annot */
  private void selectAnnot(AnnotatedFeatureI annot) {
    displayAnnot(getTransOrOneLevelAnn(annot));
    translationViewer.repaint();
    BaseFocusEvent evt = new BaseFocusEvent(this, annot.getStart(), annot);
    getController().handleBaseFocusEvent(evt);
    getSelectionManager().select(annot,this); // sends off selection
  }

  /** Display AnnotatedFeatureI feature.
   *  Exon, Transcript, and Gene are all GenericAnnotationI.
   *  No selection event is fired. (selectAnnot fires and displays)
  */
  private void displayAnnot(AnnotatedFeatureI annot) {
    currentAnnot = annot;
    if (currentAnnot == null) {
      transcriptComboBox.removeAllItems();
      transcriptComboBox.addItem("<no feature selected>");
      lengthLabel.setText("Translation length: <no feature selected>");
      upstream_button.setLabel("");
      downstream_button.setLabel("");
      translationViewer.setTranscript(null,editorPanel.getSelectedTier()); // ??
      return;
    }
     
    //else {
    setupTranscriptComboBox(currentAnnot);

    SeqFeatureI topAnnot = currentAnnot;
    if (topAnnot.isTranscript())
      topAnnot = currentAnnot.getRefFeature();
    if (topAnnot.isProteinCodingGene()) {
      String translation = currentAnnot.translate();
      if (translation == null) {
        lengthLabel.setText("Translation length: <no start selected>");
      } else {
        lengthLabel.setText("Translation length: " +
                            currentAnnot.translate().length());
      }
    } else {
      lengthLabel.setText(topAnnot.getFeatureType() + " annotation");
    }
    FeatureSetI holder = (FeatureSetI) topAnnot.getRefFeature();
    neighbor_up = null;
    neighbor_down = null;
    if (holder != null) {

      int index = holder.getFeatureIndex(topAnnot);
      // get next neighbor up that has whole sequence
      for (int i = index-1; i >= 0 && neighbor_up==null; i--) {
        FeatureSetI gene_sib = (FeatureSetI) holder.getFeatureAt(i);
        if (gene_sib.getFeatureAt(0) instanceof Transcript) {
          Transcript trans =  (Transcript) gene_sib.getFeatureAt(0);
          if (trans.haveWholeSequence())//szap.getCurationSet()))
            neighbor_up = trans;
        }
      }

      // get next neighbor down that has whole sequence
      for (int i = index+1; i < holder.size() && neighbor_down==null; i++) {
        FeatureSetI gene_sib = (FeatureSetI) holder.getFeatureAt(i);
        if (gene_sib.getFeatureAt(0) instanceof Transcript) {
          Transcript trans =  (Transcript) gene_sib.getFeatureAt(0);
          if (trans.haveWholeSequence())//szap.getCurationSet()))
            neighbor_down = trans;
        }
      }
    }
    upstream_button.setLabel (neighbor_up == null ?
                              "" :
                              "Go to next 5' annotation (" +
                              neighbor_up.getParent().getName()+")");
    upstream_button.setVisible (neighbor_up != null);
    downstream_button.setLabel (neighbor_down == null ?
                                "" :
                                "Go to next 3' annotation (" +
                                neighbor_down.getParent().getName()+")");
    downstream_button.setVisible (neighbor_down != null);
    //}
    // todo - translationViewer take in 1 level annot
    if (currentAnnot.isTranscript())
      translationViewer.setTranscript((Transcript)currentAnnot,
                                    editorPanel.getSelectedTier());
  }

  /** Sets up transcriptComboBox (pulldown list) with transcript and
   * its parent gene's other transcripts, with transcript selected */
  private void setupTranscriptComboBox(AnnotatedFeatureI annot) {
    // could also check for gene change before doing a removeAll
    if (transcriptComboBox.getSelectedItem() == annot) return;
    // adding and removing items causes item events to fire so need to remove
    // listener here - is there any other way to supress firing?
    transcriptComboBox.removeItemListener(transItemListener);
    transcriptComboBox.removeAllItems();
    if (annot==null) {
      transcriptComboBox.addItem("<no feature selected>");
      return;
    }

    // 1 LEVEL ANNOT
    if (annot.isAnnotTop()) {
      transcriptComboBox.addItem(annot);
      return;
    }
    
    // TRANSCRIPT
    SeqFeatureI gene = annot.getRefFeature();
    Vector transcripts = gene.getFeatures();
    for (int i=0; i<transcripts.size(); i++)
      transcriptComboBox.addItem(transcripts.elementAt(i));
    transcriptComboBox.setSelectedItem(annot); // transcript
    transcriptComboBox.addItemListener(transItemListener);
  }
  
  private TranscriptComboBoxItemListener transItemListener
    = new TranscriptComboBoxItemListener();
  private class TranscriptComboBoxItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() == ItemEvent.SELECTED
          && e.getItem() instanceof Transcript)
        selectAnnot((Transcript)e.getItem());
    }
  }

  /** AssemblyFeature is the only GAI at the moment that has no relation to
    // a transcript. Dont think they can end up in ede (??).
    // Would be nice to have an interface that was solely for the exon
    // trans gene heirarchy? Should we actively make sure AssemblyFeatures dont
    // get into ede? (filter in AnnotationMenu?)
    returns null if no transcript can be found. */
  private AnnotatedFeatureI getTransOrOneLevelAnn(AnnotatedFeatureI af) {
    if (af != null) {
      if (af.isTranscript())
        return af;
      if (af.isExon())
        return af.getRefFeature().getAnnotatedFeature();
      if (af.isAnnotTop()) {
        if (af.hasKids())
          return af.getFeatureAt(0).getAnnotatedFeature(); // transcript
        else // 1 level annot
          return af;
      }
    }
    return null;
  }

}
