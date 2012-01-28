package apollo.gui.detailviewers.sequencealigner.DNAPanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.editor.AnnotationChangeEvent;
import apollo.gui.Controller;
import apollo.gui.detailviewers.sequencealigner.AbstractScrollablePanel;
import apollo.gui.detailviewers.sequencealigner.AbstractTierPanel;
import apollo.gui.detailviewers.sequencealigner.AnnotationEditorMouseListener;
import apollo.gui.detailviewers.sequencealigner.FeaturePlaceFinder;
import apollo.gui.detailviewers.sequencealigner.HorizontalScrollBarAdjustmentListener;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanel;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanelHeaderTable;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.PanelFactory;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.ReferenceFactory;
import apollo.gui.detailviewers.sequencealigner.ReferenceFactoryI;
import apollo.gui.detailviewers.sequencealigner.ResultMouseListener;
import apollo.gui.detailviewers.sequencealigner.SeqFeatureTableModel;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.TierFactory;
import apollo.gui.detailviewers.sequencealigner.TierPanelAddressHeader;
import apollo.gui.detailviewers.sequencealigner.VerticalScrollBarAdjustmentListener;
import apollo.gui.detailviewers.sequencealigner.ViewportChangeListener;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorSeqFeatureDisplayType;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorSeqFeatureLength;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorSeqFeatureName;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorSeqFeatureScore;
import apollo.gui.detailviewers.sequencealigner.comparators.MultiComparator;
import apollo.gui.detailviewers.sequencealigner.comparators.MultiComparatorArrayList;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilter;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilterAnd;
import apollo.gui.detailviewers.sequencealigner.listeners.AnnotationHeaderMouseListener;
import apollo.gui.detailviewers.sequencealigner.listeners.ResultHeaderMouseListener;
import apollo.gui.detailviewers.sequencealigner.overview.Overview;
import apollo.gui.detailviewers.sequencealigner.renderers.AnnotationRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.BaseRendererI;
import apollo.gui.detailviewers.sequencealigner.renderers.ResultRendererFactory;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.synteny.GuiCurationState;

/**
 * The main class for the DNA view.
 *
 */
public class DNAMultiSequenceAlignerPanel extends  MultiSequenceAlignerPanel {
  
  // The main components
  MultiTierPanel aminoAcidPanel;
  MultiTierPanel annotationPanel;
  AbstractScrollablePanel addressComponent;
  MultiTierPanel referencePanel;
  MultiTierPanel resultPanel;
  
  // The scroll panes that hold those components
  JScrollPane aminoAcidPane;
  JScrollPane annotationPane;
  JScrollPane addressPane;
  JScrollPane referencePane;
  JScrollPane resultPane;

  // buttons to move to next/prev annotation
  Component resultPaneHeading;
  JButton nextAnnotation;
  JButton prevAnnotation;
  
  // checkbox for external selection
  JCheckBox externalSelection;
  
  // The headers for each of the main components
  Component aminoAcidHeader;
  MultiTierPanelHeaderTable annotationHeader;
  Component addressHeader;
  Component referenceHeader;
  Component resultHeaderHeading;
  MultiTierPanelHeaderTable resultHeader;
  
  // The scroll panes that hold the headers
  JViewport annotationHeaderView;
  JViewport resultHeaderView;
  
  // The overview
  Overview overview;
  
  // The main horizontal scroll bar
  JScrollBar horizontalScrollBar;

  // The separator between the headers and the scrollable base components
  JSplitPane horizontalSplitPane;
  JPanel leftPane;
  JSplitPane rightPane;
  
  JPanel topPanel;
  JPanel bottomPanel;
  
  // The menu
  JMenu menu;
  
  AnnotationEditorMouseListener annotationMouseListener;
  ResultMouseListener resultMouseListener;
  
  private List<SeqFeatureI> annotations;
  private List<SeqFeatureI> results;
  
  private MultiComparator<SeqFeatureI> annotationComparator;
  private MultiComparator<SeqFeatureI> resultComparator;
  
  // These hold extra filters (not type or strand)
  private MultiFilter<SeqFeatureI> annotationFilter;
  private MultiFilter<SeqFeatureI> resultFilter;
  
  private Strand strand;
  private ReferenceFactoryI referenceFactory;
  
  private boolean easyRead;
  private ResultRendererFactory resultRendererFactory;

  /**
   * Factory method for creating DNA Panels
   * 
   * @param curationState
   * @param strand
   * @return
   */
  public static DNAMultiSequenceAlignerPanel makeAligner(
      GuiCurationState curationState, int strand) {

    DNAMultiSequenceAlignerPanel msa = new DNAMultiSequenceAlignerPanel(
        curationState, Strand.valueOf(strand));
    
    return msa;
  }


  /**
   * Constructor
   * 
   * @param curationState
   * @param strand
   */
  public DNAMultiSequenceAlignerPanel(
      GuiCurationState curationState, Strand strand) {
    super(curationState);
    this.strand = strand;
    
    this.easyRead = false;
    this.resultRendererFactory = new ResultRendererFactory(easyRead);
    
    this.annotationComparator = new MultiComparatorArrayList<SeqFeatureI>();
    this.resultComparator = new MultiComparatorArrayList<SeqFeatureI>();
    
    this.annotationComparator.add(new ComparatorSeqFeatureLength());
    this.annotationComparator.add(new ComparatorSeqFeatureName());
    
    this.resultComparator.add(new ComparatorSeqFeatureName());
    this.resultComparator.add(new ComparatorSeqFeatureDisplayType());
    this.resultComparator.add(new ComparatorSeqFeatureScore());
    
    
    this.annotationFilter = new MultiFilterAnd<SeqFeatureI>();
    this.resultFilter = new MultiFilterAnd<SeqFeatureI>();
    
    this.referenceFactory = 
      new ReferenceFactory(getCurationState().getCurationSet());
    
    this.annotations = new ArrayList<SeqFeatureI>();
    this.results = new ArrayList<SeqFeatureI>();
    
    this.aminoAcidPanel = 
      new AminoAcidPanel(referenceFactory, Orientation.FIVE_TO_THREE, strand);
    
    this.annotationPanel = new HomogeneousPanel(
        new PanelFactory(Orientation.FIVE_TO_THREE, 
                         new TierFactory(referenceFactory, 
                                         strand, ReadingFrame.NONE)
                                         ) {
          @Override
          public BaseRendererI getRenderer() {
            return new AnnotationRenderer(getCurationState());
          }
        });
    
    this.resultPanel = new HomogeneousPanel(
        new PanelFactory(Orientation.FIVE_TO_THREE, 
            new TierFactory(referenceFactory, 
                            strand, ReadingFrame.NONE)) {
          @Override
          public BaseRendererI getRenderer() {
            return resultRendererFactory.makeRenderer();
          }
        }, 
        FeaturePlaceFinder.Type.SEMI_COMPACT);
    
    // need to make annotation and result panels before the reference panel
    this.referencePanel = 
      new ReferencePanel(referenceFactory, annotationPanel, resultPanel);
    
    this.addressComponent = new TierPanelAddressHeader(annotationPanel);

    this.overview = new Overview(this);
    
    this.reformat();
    this.addEditRegion();
  }
  
  
  public MultiTierPanel getAnnotationPanel() {
    return this.annotationPanel;
  }
  
  public MultiTierPanel getResultPanel() {
    return this.resultPanel;
  }

  public String getType() {
    return SequenceI.DNA;
  }
  
  public Strand getStrand() {
    return this.strand;
  }
  
  public  ReadingFrame getFrame() {
    return ReadingFrame.NONE;
  }
  
  public JMenu getMenu() {
    return menu;
  }
  
  public List<SeqFeatureI> getAnnotations() {
    return annotations;
  }
  
  public void setAnnotations(List<SeqFeatureI> annotations) {
    this.annotations = annotations;
  }
  
  public List<SeqFeatureI> getResults() {
    return results;
  }
  
  public void setResults(List<SeqFeatureI> results) {
    this.results = results;
  }
  
  public void init() {
    this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
    this.setBackground(Color.gray);
    this.setOpaque(true);
    
    this.getController().addListener(this); //Listen for changes to annotations
    
    // Make a ScrollPane for each scrollable object
    aminoAcidPane = new JScrollPane(aminoAcidPanel,
        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    annotationPane = new JScrollPane(annotationPanel,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    addressPane = new JScrollPane(addressComponent,
        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    referencePane = new JScrollPane(referencePanel,
        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    resultPane = new JScrollPane(resultPanel,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    // Set the border to null
    aminoAcidPane.setBorder(null);
    annotationPane.setBorder(null);
    addressPane.setBorder(null);
    referencePane.setBorder(null);
    resultPane.setBorder(null);
    
    // Set the background of the annotation and result viewports in case
    // they get too big.
    annotationPane.getViewport().setBackground(this.getBackground());
    resultPane.getViewport().setBackground(this.getBackground());
    
    annotationPane.getViewport().setOpaque(true);
    resultPane.getViewport().setOpaque(true);
    
    // Make the header for each ScrollPane
    List<SeqFeatureTableModel.ColumnTypes> annotationCols = new ArrayList();
    annotationCols.add(SeqFeatureTableModel.ColumnTypes.NAME);

    List<SeqFeatureTableModel.ColumnTypes> resultCols = new ArrayList();
    resultCols.add(SeqFeatureTableModel.ColumnTypes.NAME);
    resultCols.add(SeqFeatureTableModel.ColumnTypes.SCORE);
    resultCols.add(SeqFeatureTableModel.ColumnTypes.TYPE);
    
    aminoAcidHeader = makeAminoAcidHeader();
    annotationHeader = new MultiTierPanelHeaderTable(annotationPane.getViewport(), annotationCols);
    addressHeader = new Box.Filler(new Dimension(0,0), new Dimension(0,0), new Dimension(0,0));
    referenceHeader = makeReferenceHeader(); reformatReferenceHeader();
    resultHeader = new MultiTierPanelHeaderTable(resultPane.getViewport(), resultCols);
    
    
    annotationHeader.addMouseListener(new AnnotationHeaderMouseListener(this));
    resultHeaderHeading = resultHeader.getTableHeader();
    resultHeaderHeading.addMouseListener(new ResultHeaderMouseListener(this));
    
    resultPaneHeading =  new Box.Filler(new Dimension(0,0), new Dimension(0,0), new Dimension(0,0));
    nextAnnotation = new JButton(new AbstractAction("Next Annotation") {
      public void actionPerformed(ActionEvent e) {
        nextAnnotation();
      }
    });
    prevAnnotation = new JButton(new AbstractAction("Prev Annotation") {
      public void actionPerformed(ActionEvent e) {
        prevAnnotation();
      }
    });
    
    externalSelection = new JCheckBox(new AbstractAction("External Selection") {
      public void actionPerformed(ActionEvent e) {
        setNoExternalSelection(!externalSelection.isSelected());
      }
      
    });
    externalSelection.setSelected(true);
    
    
    // Have the headers listen to viewports where needed
    annotationPane.getViewport()
      .addChangeListener(new ViewportChangeListener(annotationHeader));
    resultPane.getViewport()
      .addChangeListener(new ViewportChangeListener(resultHeader));
    
    // Have the SZAP listen to the annotation viewport so it knows when to repaint
    annotationPane.getViewport()
      .addChangeListener(new RepaintOnChangeListener(getCurationState().getSZAP()));
    
    // add a pulldown menu
    initMenuBar();
    

    // The viewports for the headers that need them
    annotationHeaderView = new JViewport();
    resultHeaderView = new JViewport();
    
    annotationHeaderView.setView(annotationHeader);
    resultHeaderView.setView(resultHeader);
    
    annotationPane.getVerticalScrollBar().addAdjustmentListener(
        new VerticalScrollBarAdjustmentListener(annotationHeaderView));
    resultPane.getVerticalScrollBar().addAdjustmentListener(
      new VerticalScrollBarAdjustmentListener(resultHeaderView));
    
    // Create the ScrollBar at the bottom of the screen
    horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);

    // Add listeners AdjustmentListeners to the ScrollBar
    horizontalScrollBar.addAdjustmentListener(
      new HorizontalScrollBarAdjustmentListener(aminoAcidPane.getViewport()));
    horizontalScrollBar.addAdjustmentListener(
      new HorizontalScrollBarAdjustmentListener(annotationPane.getViewport()));
    horizontalScrollBar.addAdjustmentListener(
      new HorizontalScrollBarAdjustmentListener(addressPane.getViewport()));
    horizontalScrollBar.addAdjustmentListener(
      new HorizontalScrollBarAdjustmentListener(referencePane.getViewport()));
    horizontalScrollBar.addAdjustmentListener(
      new HorizontalScrollBarAdjustmentListener(resultPane.getViewport()));
    
    horizontalScrollBar.addAdjustmentListener(
        new AdjustmentListener() {
          public void adjustmentValueChanged(AdjustmentEvent e) {
              overview.repaint();
          }
        }
    );

    // make the left panel
    leftPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
    leftPane.setBackground(this.getBackground());
    leftPane.setBorder(null);
    leftPane.setOpaque(true);
    
    
    // make the right panel
    topPanel = new JPanel (new FlowLayout(FlowLayout.LEFT, 0, 5));
    topPanel.setBackground(this.getBackground());
    topPanel.setBorder(null);
    topPanel.setOpaque(true);
   
    
    bottomPanel = new JPanel (new FlowLayout(FlowLayout.LEFT, 0, 5));
    bottomPanel.setBackground(this.getBackground());
    bottomPanel.setBorder(null);
    bottomPanel.setOpaque(true);
    
    
    rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                               false, topPanel, bottomPanel);
    
    rightPane.setBackground(this.getBackground());
    rightPane.setBorder(null);
    rightPane.setOpaque(true);
    
    horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     false, leftPane, rightPane);
    
    horizontalSplitPane.setBackground(this.getBackground());
    horizontalSplitPane.setBorder(null);
    horizontalSplitPane.setOpaque(true);
    
    horizontalSplitPane.addPropertyChangeListener(this);
    rightPane.addPropertyChangeListener(this);
    
    
    leftPane.add(aminoAcidHeader);
    leftPane.add(annotationHeaderView);
    leftPane.add(addressHeader);
    leftPane.add(referenceHeader);
    leftPane.add(Box.createRigidArea(new Dimension(10,rightPane.getDividerSize())));
    leftPane.add(resultHeaderHeading);
    leftPane.add(resultHeaderView);
    
    
    topPanel.add(aminoAcidPane);
    topPanel.add(annotationPane);
    topPanel.add(addressPane);
    topPanel.add(referencePane);
    
    bottomPanel.add(prevAnnotation);
    bottomPanel.add(nextAnnotation);
    bottomPanel.add(externalSelection);
    bottomPanel.add(resultPaneHeading);
    bottomPanel.add(resultPane);
    
    this.add(horizontalSplitPane);
    this.add(this.overview);
    this.add(horizontalScrollBar);
    
    this.addListeners();
  }
  
  private void addListeners() {
    annotationMouseListener = new AnnotationEditorMouseListener(this.annotationPanel, getCurationState(), this);
    annotationMouseListener.setScrollableObject(this);
    
    annotationPanel.addMouseListener(annotationMouseListener);
    annotationPanel.addMouseMotionListener(annotationMouseListener);
    
    
    resultMouseListener = new ResultMouseListener(this.resultPanel, getCurationState());
    resultMouseListener.setMultiSequenceAlignerPanel(this);
    
    resultPanel.addMouseListener(resultMouseListener);
    resultPanel.addMouseMotionListener(resultMouseListener);
  }
  
  public void validate() {
    // The preferred size of this panel
    int width = this.getPreferredSize().width;
    int height = this.getPreferredSize().height;
    
    // the amount of space between panels (assumes uniform distance)
    int gap = ((FlowLayout) topPanel.getLayout()).getVgap();
    
    // height of each fixed size component
    int scrollBarH  = 15;
    int aminoAcidH  = aminoAcidPanel.getPreferredSize().height;
    int addressH    = addressComponent.getPreferredSize().height;
    int referenceH  = referencePanel.getPreferredSize().height;
    int resultHeaderHeadingH = resultHeaderHeading.getPreferredSize().height;
    int overviewH = overview.getPreferredSize().height;
    
    // Total height of the horizontally split pane
    int totalH = height - (scrollBarH + overviewH + 2*gap);
    
    // Calculate the width of each part based on the location of the dividers
    int leftW = this.horizontalSplitPane.getDividerLocation();
    int rightW = width - (leftW + this.horizontalSplitPane.getDividerSize());
    
    int topH = this.rightPane.getDividerLocation();
    int bottomH = totalH - (topH + this.rightPane.getDividerSize());
    
    // Calculate the best size for the annotations and results
    int annotationH = 
      Math.max(annotationPanel.getBaseHeight()*3, // three tier
               topH - (aminoAcidH + addressH + referenceH + 5*gap)); // how much room is left
    
    int resultH = 
      Math.max(resultPanel.getBaseHeight(), 
               bottomH - (resultHeaderHeadingH + 2*gap));
    
    
    // Set the preferred size for all the components
    aminoAcidPane.setPreferredSize(new Dimension(rightW, aminoAcidH));
    annotationPane.setPreferredSize(new Dimension(rightW, annotationH));
    addressPane.setPreferredSize(new Dimension(rightW, addressH));
    referencePane.setPreferredSize(new Dimension(rightW, referenceH));
    resultPane.setPreferredSize(new Dimension(rightW, resultH));
    
    this.horizontalSplitPane.setSize(new Dimension(width, totalH));
    
    // TODO: Make min/max size work so that the split pane wont get too big
    this.topPanel.setMinimumSize(new Dimension(0, aminoAcidH + annotationPanel.getBaseHeight()*3 + addressH + referenceH + 5*gap));
    this.topPanel.setMaximumSize(new Dimension(annotationPanel.getPreferredSize().width, aminoAcidH + annotationPanel.getPreferredSize().height + addressH + referenceH + 5*gap));
    this.bottomPanel.setMinimumSize(new Dimension(0, resultPanel.getBaseHeight() + 1*gap));
    this.bottomPanel.setMaximumSize(new Dimension(resultPanel.getPreferredSize().width, resultPanel.getPreferredSize().height + 1*gap));
    
    
    this.leftPane.setPreferredSize(new Dimension(leftW, totalH));
    this.rightPane.setPreferredSize(new Dimension(rightW, totalH));
    this.topPanel.setPreferredSize(new Dimension(rightW, topH));
    this.bottomPanel.setPreferredSize(new Dimension(rightW, bottomH));
    
    aminoAcidHeader.setPreferredSize(new Dimension(leftW, aminoAcidH));
    annotationHeaderView.setPreferredSize(new Dimension(leftW, annotationH));
    ((Box.Filler)addressHeader).changeShape(new Dimension(0, 0), new Dimension(leftW, addressH), new Dimension(leftW, addressH));
    referenceHeader.setPreferredSize(new Dimension(leftW, referenceH));
    resultHeaderView.setPreferredSize(new Dimension(leftW, resultH));
    
    int nextW = nextAnnotation.getPreferredSize().width;
    int prevW = prevAnnotation.getPreferredSize().width;
    int extrW = externalSelection.getPreferredSize().width;
    nextAnnotation.setPreferredSize(new Dimension(nextW, resultHeaderHeadingH));
    prevAnnotation.setPreferredSize(new Dimension(prevW, resultHeaderHeadingH));
    externalSelection.setPreferredSize(new Dimension(extrW, resultHeaderHeadingH));
    resultHeaderHeading.setPreferredSize(new Dimension(leftW, resultHeaderHeadingH));
    ((Box.Filler)resultPaneHeading).
    changeShape(new Dimension(0, 0),
                new Dimension(rightW-(nextW+prevW+extrW), resultHeaderHeadingH),
                new Dimension(rightW-(nextW+prevW+extrW), resultHeaderHeadingH));
    
    
    overview.setPreferredSize(new Dimension(width, overviewH));
    overview.setSize(new Dimension(width, overviewH));
    
    // The max value should be the top left corner of the viewport (related to the viewport width)
    // -15 for the little box in the right corner that allows you to adjust the size of the frame
    horizontalScrollBar.setPreferredSize(new Dimension(width, scrollBarH));
    horizontalScrollBar.setMinimum(0);
    horizontalScrollBar.setMaximum(referencePanel.getPreferredSize().width - rightW + referencePanel.getBaseWidth()); // off by one base somewhere...
    horizontalScrollBar.setBlockIncrement(rightW*3/4);
    horizontalScrollBar.setUnitIncrement(referencePanel.getBaseWidth());

    super.validate();
  }
  

  public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
    if (!canHandleSelection(evt,this))
      return false;
    // now we do something, canHanSel filters for GenAnnIs
    AnnotatedFeatureI gai = (AnnotatedFeatureI)evt.getFeatures().getFeature(0);
    setSelection(gai);
    //displayAnnot(getTransOrOneLevelAnn(gai));
    return true;
  }
  
  
  /** Display AnnotatedFeatureI feature.
   *  Exon, Transcript, and Gene are all GenericAnnotationI.
   *  No selection event is fired. (selectAnnot fires and displays)
  */
  private void displayAnnot() {
    if (getSelection() == null) {
      //transcriptComboBox.removeAllItems();
      //transcriptComboBox.addItem("<no feature selected>");
      //lengthLabel.setText("Translation length: <no feature selected>");
      //upstream_button.setLabel("");
      //downstream_button.setLabel("");
      //translationViewer.setTranscript(null,editorPanel.getSelectedTier()); // ??
      return;
    }
     
    //else {
    //setupTranscriptComboBox(selectedAnnotation);

    SeqFeatureI topAnnot = getSelection();
    if (topAnnot.isTranscript())
      topAnnot = getSelection().getRefFeature();
    if (topAnnot.isProteinCodingGene()) {
      String translation = getSelection().translate();
      if (translation == null) {
        //lengthLabel.setText("Translation length: <no start selected>");
      } else {
        //lengthLabel.setText("Translation length: " +
        //                    currentAnnot.translate().length());
      }
    } else {
      //lengthLabel.setText(topAnnot.getFeatureType() + " annotation");
    }
    FeatureSetI holder = (FeatureSetI) topAnnot.getRefFeature();
    //neighbor_up = null;
    //neighbor_down = null;
    if (holder != null) {

      int index = holder.getFeatureIndex(topAnnot);
      // get next neighbor up that has whole sequence
      /*
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
      } */
    }
    /*
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
    */
    //}
    // todo - translationViewer take in 1 level annot
    if (getSelection().isTranscript())
      overview.setTranscript((Transcript)getSelection());//,
                            // annotationMouseListener.getTier());
                              //editorPanel.getSelectedTier());
  }
  
  /* Start ControlledObjectI interface */
  public Controller getController() {
    return getCurationState().getController();
  }
  

  public Object getControllerWindow() {
    return SwingUtilities.getWindowAncestor(this);
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  public void setController(Controller controller) {
    // TODO Auto-generated method stub
  }
  /* End ControlledObjectI interface */

  

  public void reformat() {
    reformatResultPanel();
    reformatAnnotationPanel();
    reformatReferencePanel(annotationPanel.getOrientation());
    reformatAminoAcidPanel();
  }

  private void reformatAminoAcidPanel() {
    getAminoAcidPanel().reformat(false);
  }

  /* TODO: maybe put the refernce header into the the ReferencePanel object */
  public void reformatReferenceHeader() {
    JTable table = (JTable) referenceHeader;
    TableModel model = table.getModel();
    
    if (referencePanel.getTier(0).getStrand().equals(Strand.FORWARD)) {
      model.setValueAt("Strand +", 0, 0);
    } else {
      model.setValueAt("Strand -", 0, 0);
    }
    
    if (resultPanel.getTier(0).getStrand().equals(Strand.FORWARD)) {
      model.setValueAt("Strand +", 1, 0);
    } else {
      model.setValueAt("Strand -", 1, 0);
    }
    
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getPropertyName().equals("dividerLocation")) {
      this.validate();

      // invalidate the left pane so that it will be redrawn.
      // should this just be put in this.validate?
      this.leftPane.invalidate();
      this.leftPane.validate();
    }
  }
  
  
  private Component makeAminoAcidHeader() {
    String[] aminoAcidColumns = { "Frame" }; // Frame?
    Object[][] aminoAcidData = { { "Frame - 1" }, { "Frame - 2" }, { "Frame - 3" } };
    JTable header = new JTable(aminoAcidData, aminoAcidColumns);
    header.setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    header.setForeground(Color.black);
    header.setBackground(Color.white);
    header.setOpaque(true);
    header.setBorder(null);
    header.setRowHeight(aminoAcidPanel.getBaseHeight());
    header.setRowMargin(0);
    
    return header;
    
  }
  
  private Component makeReferenceHeader() {
    
    String[] referenceColumns = { "Strand" }; // Frame?
    Object[][] referenceData = new Object[2][1];
    
    JTable referenceHeader = new JTable(referenceData, referenceColumns);
    referenceHeader.setFont(apollo.config.Config.getExonDetailEditorSequenceFont());
    referenceHeader.setForeground(Color.black);
    referenceHeader.setBackground(Color.white);
    referenceHeader.setOpaque(true);
    referenceHeader.setBorder(null);
    referenceHeader.setRowHeight(referencePanel.getBaseHeight());
    referenceHeader.setRowMargin(0);
    
    return referenceHeader;
  }
  
  
  /*
   * *********************************************************
   * Implementation of methods for  BaseScrollable
   * 
   * @see apollo.gui.BaseScrollable#getVisibleBase()
   ***********************************************************
   */

  /**
   * Gets the base pair position for the all the bases visible on the
   * left side of the viewable window
   */
  public int getVisibleBase() {
    JScrollBar b = this.horizontalScrollBar;
    int pixelPosition = annotationPanel.getPositionForPixel(
        new Point(horizontalScrollBar.getValue(), 0));
    int position = annotationPanel.pixelPositionToTierPosition(pixelPosition);
    return annotationPanel.tierPositionToBasePair(position);
  }

  /**
   * Gets the number of bases that are visable on a single row
   */
  public int getVisibleBaseCount() {
    Dimension size = this.annotationPane.getSize();
    return (int) Math.floor((1.0*size.width)/annotationPanel.getBaseWidth()); 
  }

  /**
   * Scrolls the left edge of the view of this panel to a specific base pair
   */
  public void scrollToBase(int basePair) {
    //TierPanelI model = annotationPanel.getModelTierPanel();
    //int tierPosition = model.getTier().getPosition(basePair);
    int tierPosition = annotationPanel.basePairToTierPosition(basePair);
    int pixelPosition = annotationPanel.tierPositionToPixelPosition(tierPosition);
    int pixel = annotationPanel.getPixelForPosition(pixelPosition).x;
    
    if (pixel > horizontalScrollBar.getMaximum()) {
      pixel = horizontalScrollBar.getMaximum();
    }
    
    if (pixel < horizontalScrollBar.getMinimum()) {
      pixel = horizontalScrollBar.getMinimum();
    }
    
    this.horizontalScrollBar.setValue(pixel);
  }


  
  
  /** 
   * updates the horizontal bars in the apollo window 
   */
  private class RepaintOnChangeListener implements ChangeListener {
    private Component c;
    
    public RepaintOnChangeListener(Component c) {
      this.c = c;
    }
    
    public void stateChanged(ChangeEvent e) {
      c.repaint();
    }
  }
  
  
  public void switchAnnotations() {
    // Update the AminoAcidPanel
    getAminoAcidPanel().setStrand(getStrand().getOpposite());
    reformatAminoAcidPanel();
    
    // switch the strand
    setStrand(getStrand().getOpposite());
    
    // Update the Annotation panel
    getAnnotationPanel().switchStrand();
    getAnnotationPanel().setReferenceSequence(
        getReferenceSequence(getStrand(), getFrame()));
    
    reformatAnnotationPanel();
    
    getResultPanel().switchOrientation();
    
    //** Update the reference panel **/
    reformatReferencePanel(
        getAnnotationPanel().getOrientation());
    reformatReferenceHeader();
    
    int max = getScrollBar().getMaximum();
    int val = getScrollBar().getValue();
    getScrollBar().setValue(max-val);
    
    getOverview().updateState();
    
    setSelection(getSelection());
    
    updateEditRegion();
    
    repaint();
    
  }

  
  
  public void flipOrientation() {

    getAminoAcidPanel().switchOrientation();
    super.flipOrientation();
   
  }
  
  private void initMenuBar() {
    
    JMenuBar menuBar = getMenuBar();
    menu = makeMenu();
    menuBar.add(menu);
    
    // Separator
    menu.addSeparator();
    
    JMenuItem menuItem;
    
    
    // Result Coloring option
    menuItem = new JMenuItem(new AbstractAction("Switch Result Coloring") {
      public void actionPerformed(ActionEvent arg0) {
        resultRendererFactory.setEasyRead(
            !resultRendererFactory.getEasyRead());
        BaseRendererI r = resultRendererFactory.makeRenderer();
        resultPanel.setRenderer(r);
        repaint();
      }
    });
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Changes the way results are colored");
    menu.add(menuItem);
  }


  @Override
  public Overview getOverview() {
    return this.overview;
  }


  @Override
  public JScrollBar getScrollBar() {
    return this.horizontalScrollBar;
  }


  @Override
  public MultiTierPanel getReferencePanel() {
    return this.referencePanel;
  }
  
  public MultiTierPanel getAminoAcidPanel() {
    return this.aminoAcidPanel;
  }

  @Override
  public void setStrand(Strand s) {
    this.strand = s;
  }


  @Override
  public MultiTierPanelHeaderTable getAnnotationHeader() {
    return this.annotationHeader;
  }
  
  public JScrollPane getAnnotationPane() {
    return this.annotationPane;
  }


  @Override
  public MultiTierPanelHeaderTable getResultHeader() {
    return this.resultHeader;
  }


  @Override
  public SequenceI getReferenceSequence(Strand s, ReadingFrame rf) {
    return this.referenceFactory.getReference(s, rf);
  }


  @Override
  public boolean annotationFilter(SeqFeatureI f) {
    return true;
  }


  @Override
  public boolean resultFilter(SeqFeatureI f) {
    return true;
  }


  @Override
  public MultiComparator getAnnotationComparator() {
    return this.annotationComparator;
  }


  @Override
  public MultiComparator getResultComparator() {
    return this.resultComparator;
  }


  @Override
  public void clearResults() {
    this.results.clear();
    this.resultPanel.clear();
    
  }


  @Override
  public MultiFilter getAnnotationFilter() {
    return this.annotationFilter;
  }


  @Override
  public MultiFilter getResultFilter() {
    return this.resultFilter;
  }


  @Override
  public void clearAnnotations() {
    this.annotations.clear();
    this.annotationPanel.clear();
  }


  @Override
  public AbstractScrollablePanel getAddressComponent() {
    return this.addressComponent;
  }



}
