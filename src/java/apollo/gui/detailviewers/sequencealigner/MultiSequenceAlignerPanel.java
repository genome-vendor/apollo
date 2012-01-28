package apollo.gui.detailviewers.sequencealigner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import apollo.config.Config;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.gui.BaseScrollable;
import apollo.gui.ControlledObjectI;
import apollo.gui.detailviewers.sequencealigner.AAPanel.AAMultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.DNAPanel.DNAMultiSequenceAlignerPanel;
import apollo.gui.detailviewers.sequencealigner.actions.AddFilterAction;
import apollo.gui.detailviewers.sequencealigner.comparators.ComparatorFactory;
import apollo.gui.detailviewers.sequencealigner.comparators.MultiComparator;
import apollo.gui.detailviewers.sequencealigner.filters.Filter;
import apollo.gui.detailviewers.sequencealigner.filters.FilterFactory;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilter;
import apollo.gui.detailviewers.sequencealigner.filters.MultiFilterAnd;
import apollo.gui.detailviewers.sequencealigner.menus.AddFilterPanel;
import apollo.gui.detailviewers.sequencealigner.overview.Overview;
import apollo.gui.detailviewers.sequencealigner.renderers.AnnotationRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.DNAResultRenderer;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.event.FeatureSelectionListener;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;

public abstract class MultiSequenceAlignerPanel extends JPanel implements
    FeatureSelectionListener, ControlledObjectI, AnnotationChangeListener,
    PropertyChangeListener, BaseScrollable {
  
  protected static int colorIndex = 0;
  
  //TODO: make public methods to this and remove these...
  protected static final Color [] colorList = {
    new Color(0,0,0),
    new Color(255,255,0),
    new Color(0,255,255),
    new Color(255,0,255),
    new Color(255,0,0),
    new Color(0,255,0),
    new Color(0,0,255)
    };
  
  /** The inner array is an array of 2 colors, for alternating exon colors.
  The "outer" array represents transcript coloring. Transcripts can have
  different exon coloring pairs(see SelectableDNARenderer's exonColorIndex
  and transcriptColorIndex). 
  This now takes in 2 colors from Config edeFeatureColor. It no longer 
  is setting up 4 colors for alternating transcripts as well as exons.
  Easy enough to reinstate if desired. */
  public Color [][] getColorArray() {
    Color firstExonColor = Config.getExonDetailEditorFeatureColor1();
    Color secondExonColor = Config.getExonDetailEditorFeatureColor2();
    Color [][] colors = {{firstExonColor,secondExonColor}};
    //Color [][] colors = {{Color.blue, new Color(0,0,180)},
    //{new Color(100,150,255), new Color(100,0,200)}};
    return colors;
  }
  
  /** The curation state*/
  private GuiCurationState curationState;
  
  /** The currently selected annotation*/
  private AnnotatedFeatureI selectedAnnotation;
  
  /** The color from colorList used to identify this instance */
  private Color indicatorColor;
  
  /** The state of external selection acceptance */
  private boolean noExternalSelection;
  
  public MultiSequenceAlignerPanel(GuiCurationState curationState) {
    this.curationState = curationState;
    this.selectedAnnotation = null;
    this.indicatorColor = null;
    this.noExternalSelection = false;
  }
  
  public Color getIndicatorColor() {
    return this.indicatorColor;
  }
  
  public void setIndicatorColor(Color c) {
    this.indicatorColor = c;
    updateEditRegion();
  }
  
  public GuiCurationState getCurationState() {
    return this.curationState;
  }

  public void setCurationState(GuiCurationState cs) {
    this.curationState = cs;
  }
  
  /**
   * Method for easy access to MenuBar
   * @return
   */
  public JMenuBar getMenuBar() {
    JMenuBar menuBar = this.getRootPane().getJMenuBar();
    
    if (menuBar == null) {
      menuBar = new JMenuBar();
      this.getRootPane().setJMenuBar(menuBar);
    }
    return menuBar;
  }
  
  public MultiSequenceAlignerPanel getThis() {
    return this;
  }
  
  /**
   * Creates a drop down menu for this component
   * 
   * @return a menu
   */
  public JMenu makeMenu() {
    
    JMenuItem menuItem;
    JMenu menu = new JMenu(getType() + " Menu");
    //menu.setMnemonic(KeyEvent.VK_A);
    menu.getAccessibleContext().setAccessibleDescription(
            "The menu for the " + getType() + 
            " view of the Multi Sequence Aligner");
    //menuBar.add(menu);
    
    menuItem = new JMenuItem(new AbstractAction("Undo") {
      public void actionPerformed(ActionEvent e) {
        CurationManager.getActiveCurationState().getTransactionManager().undo(this);
      }
      
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
        ActionEvent.CTRL_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
        "Undoes the last edit");
    menu.add(menuItem);
    
    // Separator
    menu.addSeparator();
    
    // Flip Annotations
    menuItem = new JMenuItem(new AbstractAction("Switch Annotation Strand") {
      public void actionPerformed(ActionEvent arg0) {
        switchAnnotations();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_A, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Updates the annotation view to display data from the opposite strand");
    menu.add(menuItem);
    
    // Flip Results
    menuItem = new JMenuItem(new AbstractAction("Switch Alignment Strand") {
      public void actionPerformed(ActionEvent arg0) {
        switchResults();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_R, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Updates the reults view to display data from the opposite strand");
    menu.add(menuItem);
    
    /*
    // Flip Orientation - d
    menuItem = new JMenuItem(new AbstractAction("Flip Orientation") {
      public void actionPerformed(ActionEvent arg0) {
        flipOrientation();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_O, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Reverses the display orientation");
    menu.add(menuItem);
    */
    // Separator
    menu.addSeparator();

    // Next Annotation
    menuItem = new JMenuItem(new AbstractAction("Next Annotation") {
      public void actionPerformed(ActionEvent arg0) {
        nextAnnotation();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_EQUALS, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Scrolls window to the next Annotation");
    menu.add(menuItem);
    
    // Previous Annotation
    menuItem = new JMenuItem(new AbstractAction("Previous Annotation") {
      public void actionPerformed(ActionEvent arg0) {
        prevAnnotation();
      }
    });
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_MINUS, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Scrolls window to the previous Annotation");
    menu.add(menuItem);
    
    menu.addSeparator();
    
    // Edit Sorting
    menuItem = new JMenuItem(new AbstractAction("Edit Alignment Sorting") {
      public void actionPerformed(ActionEvent arg0) {
        // TODO create custom dialog so user can choose to sort by a few things
        Object[] possibilities = {  
            ComparatorFactory.TYPE.TYPE, 
            ComparatorFactory.TYPE.LENGTH,
            ComparatorFactory.TYPE.NAME, 
            ComparatorFactory.TYPE.SCORE,
            ComparatorFactory.TYPE.FRAME,
            };
        
        ComparatorFactory.TYPE type = 
          (ComparatorFactory.TYPE)JOptionPane.showInputDialog(
              getTopLevelAncestor(),
              "What would you like to sort by?",
              "Sort Alignment Dialog",
              JOptionPane.PLAIN_MESSAGE,
              null,
              possibilities,
              possibilities[0]);
        
        if (type != null) {
          
          Comparator<SeqFeatureI> c = 
            ComparatorFactory.makeComparatorSeqFeature(type);
        
          getResultComparator().add(c);
          reformatResultPanel();
          getResultPanel().repaint();
          
        }
      }
    });
   // menuItem.setAccelerator(KeyStroke.getKeyStroke(
   //     KeyEvent.VK_MINUS, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "Change the way Alignment are sorted");
    menu.add(menuItem);
    
    
    // Edit Filtering
    JMenu filterMenu = new JMenu("Add Alignment Filtering");
   // menuItem.setAccelerator(KeyStroke.getKeyStroke(
   //     KeyEvent.VK_MINUS, ActionEvent.ALT_MASK));
    filterMenu.getAccessibleContext().setAccessibleDescription(
            "Change the way Alignment are filtered");
    menu.add(filterMenu);
    
    menuItem = 
      new JMenuItem(new AddFilterAction(this, AddFilterPanel.TYPES.FRAME));
    filterMenu.add(menuItem);
    
    /*
    menuItem = 
      new JMenuItem(new AddFilterAction(this, AddFilterPanel.TYPES.STRAND));
    filterMenu.add(menuItem);
    */
    menuItem = 
      new JMenuItem(new AddFilterAction(this, AddFilterPanel.TYPES.TYPE));
    filterMenu.add(menuItem);
    
    menuItem = new JMenuItem(new AbstractAction("Remove Filters") {
      public void actionPerformed(ActionEvent arg0) {
          getResultFilter().clear();
          reformatResultPanel();
          getResultPanel().repaint();
          getOverview().updateState();
          getOverview().repaint();
        }
    });
    filterMenu.add(menuItem);
    
    
    return menu;
  }

  /**
   * Flipping the orientation will not work, more work is needed to implement...
   */
  public void flipOrientation() {

    // I think the header watches the other components
    getAnnotationPanel().switchOrientation();
    getResultPanel().switchOrientation();
    getReferencePanel().switchOrientation();

    int max = getScrollBar().getMaximum();
    int val = getScrollBar().getValue();
    getScrollBar().setValue(max-val); 
    
    getOverview().updateState();
    
    updateTitle();
    updateEditRegion();
    
    repaint();
  }
  
  
  /**
   * Creates a Sequence object which can be used as a reference for the
   * given strand/frame pair
   * 
   * @param s the strand
   * @param rf the frame
   * @return a reference sequence
   */
  public abstract SequenceI getReferenceSequence(Strand s, ReadingFrame rf);
  
  /**
   * Gets the main horizontal scrollbar for the window
   * @return
   */
  public abstract JScrollBar getScrollBar();
  
  /**
   * Gets the overview object for the panel
   * @return
   */
  public abstract Overview getOverview();
  
  /**
   * Switches the strand for the result panel.
   * <br>
   * side effects: repaint result panel, repaint reference panel
   */
  public void switchResults() {
    Strand rStrand = getResultPanel().getStrand();

    // Update the result panel
    getResultPanel().switchStrand();
    getResultPanel().switchOrientation();
    getResultPanel().setReferenceSequence(
        getReferenceSequence(rStrand.getOpposite(), getFrame()));
    reformatResultPanel();

    // Update reference panel
    reformatReferencePanel(
        getAnnotationPanel().getOrientation());
    reformatReferenceHeader();

    // Repaint
    getResultPanel().repaint();
    getReferencePanel().repaint();
  }
  
  /**
   * Sets the selection the the feature given
   * @param gai the new selection
   * <br>
   * side effects: update the overview, and update the title
   */
  public void setSelection(AnnotatedFeatureI gai) {
    if (gai != null && getTransOrOneLevelAnn(gai) != null) {
      this.selectedAnnotation = getTransOrOneLevelAnn(gai); //gai;
    }
    
    if (getSelection().isTranscript()) {
      Transcript transcript = (Transcript) getTransOrOneLevelAnn(gai);
      // do we really want to do this every time an annotation is selected?
      transcript.setTranslationEndFromStart();
      String translation = transcript.translate();
      SequenceI sequence = transcript.getPeptideSequence();
      getOverview().setTranscript(transcript);
    }
    this.getOverview().repaint();
    this.updateTitle();
  }
  
  /**
   * Gets the currently sellected annotation
   * @return
   */
  public AnnotatedFeatureI getSelection() {
    return this.selectedAnnotation;
  }
  
  /**
   * Sets the title for the window
   */
  public void updateTitle() {
    // todo - translationViewer take in 1 level annot
    if (getSelection() != null && 
        getTransOrOneLevelAnn(getSelection()).isTranscript()) {
      Transcript transcript = 
        (Transcript) getTransOrOneLevelAnn(getSelection());
      JFrame frame = (JFrame) this.getTopLevelAncestor();
      String title = "";
      if (getAnnotationPanel().getStrand() == Strand.FORWARD) {
        title += " + ";
      } else {
        title += " - ";
      }
      
      title += transcript.getName();
      
      if (getResultPanel().getStrand() == Strand.FORWARD) {
        title += " + ";
      } else {
        title += " - ";
      }
      
      frame.setTitle(title);
    }
  }
  
  
  /**
   * Gets the Annotation Panel
   */
  public abstract MultiTierPanel getAnnotationPanel();
  
  /**
   * Gets the JScrollPane which holds the Annotation Panel
   */
  public abstract JScrollPane getAnnotationPane();
  
  /**
   * Gets the header for the annotation panel
   */
  public abstract MultiTierPanelHeaderTable getAnnotationHeader();
  
  /**
   * Gets the Result Panel
   */
  public abstract MultiTierPanel getResultPanel();
  
  /**
   * Gets the header for the result panel
   */
  public abstract MultiTierPanelHeaderTable getResultHeader();
  
  /**
   * Gets the reference panel (the one in between the annotation and result)
   */
  public abstract MultiTierPanel getReferencePanel();
  
  /**
   * Gets the address bar (shows the base numbers you are looking at)
   * @return
   */
  public abstract AbstractScrollablePanel getAddressComponent();
  
  
  /**
   * Gets the type
   */
  public abstract String getType();

  /**
   * Gets the strand of the annotations currently being viewed
   */
  public abstract Strand getStrand();
  
  /**
   * Sets the currently selected strand (no side effects)
   */
  public abstract void setStrand(Strand s);
  
  /**
   * Gets the frame that is currently being viewed
   */
  public abstract ReadingFrame getFrame();
  
  /**
   * Gets the Menu object
   */
  public abstract JMenu getMenu();
  
  /**
   * Creates all of the components and lays everything out
   */
  public abstract void init();
  
  /**
   * cleanup extra components when closing down
   */
  public void cleanUp() {
    getCurationState().getSZAP().removeHighlightRegion(this);
    getCurationState().getSZAP().repaint();
    getController().removeListener(this);
  }
  
  /**
   * Gets the list of annotations.
   * This list is used to determine if an annotation is already added to the
   * panel. TODO redesign use of annotations, probably a better way to do this.
   */
  public abstract List<SeqFeatureI> getAnnotations();
  
  /**
   * Sets the annotations (no side effects)
   */
  public abstract void setAnnotations(List<SeqFeatureI> annotations);
  
  /**
   * Gets the list of results.
   * This list is used to determine if a result is already added to the
   * panel. TODO redesign use of results, probably a better way to do this.
   */
  public abstract List<SeqFeatureI> getResults();
  
  /**
   * Sets the results (no side effects)
   */
  public abstract void setResults(List<SeqFeatureI> results);
  
  /**
   * removes all the annotations from the annotation list and the annotation panel
   */
  public abstract void clearAnnotations();
  
  /**
   * removes all of the results from the results list and the result panel
   */
  public abstract void clearResults();
  
  
  /**
   * clears and repopulates the result panel based on the current state
   */
  public void reformatResultPanel() {
    clearResults();
    
    MultiFilter<SeqFeatureI> filters = new MultiFilterAnd<SeqFeatureI>();
    filters.addAll(getResultFilter());
    filters.add(
        FilterFactory.makeFilter(SequenceType.valueOf(getType())));
    filters.add(FilterFactory.makeFilter(getResultPanel().getStrand()));
    
    
    // Get the FeatureSet for this strand (results)
    FeatureSetI resultSet = getCurationState().getCurationSet()
       .getResults().getFeatSetForStrand(getResultPanel().getStrand().toInt());
    
    // Filter
    Set<SeqFeatureI> results = getResults(resultSet, filters);
    
    // Sort
    List<SeqFeatureI> sortedResults = 
      (List<SeqFeatureI>) sort(results, getResultComparator());
    
    setResults(sortedResults);
    
    // can we get rid of the this.results?
    for (SeqFeatureI feature : getResults()) {
      addFeatureToPanel(feature, getResultPanel());
    }
    getResultPanel().reformat(true);
    
    // Update the result panel header
    if (getResultHeader() != null) {
      getResultHeader().reformat();
      int position = getResultPanel().getPositionForPixel(
          new Point(getScrollBar().getValue(), 0));
      getResultHeader().update(position);
    }
    
    updateTitle();
  }

  
  /**
   * clears and repopulates the annotation panel based on current state
   */
  public void reformatAnnotationPanel() {
    clearAnnotations();
    
    MultiFilter<SeqFeatureI> filters = new MultiFilterAnd();
    filters.addAll(getAnnotationFilter());
    
    // Get the FeatureSet for this strand (annotations)
    FeatureSetI annotSet = getCurationState().getCurationSet()
       .getAnnots().getFeatSetForStrand(
           getAnnotationPanel().getStrand().toInt());
    
    // Get Annotations
    Set<SeqFeatureI> annots = getAnnotations(annotSet);
    
    // Filter
    annots = filter(annots, filters);
    
    // Sort
    List<SeqFeatureI> sortedAnnotations = 
      (List<SeqFeatureI>) sort(annots, getAnnotationComparator());
    
    setAnnotations(sortedAnnotations);
    
    // can we get rid of the this.annotations?
    for (SeqFeatureI feature : getAnnotations()) {
      addFeatureToPanel(feature, getAnnotationPanel());
    }

    getAnnotationPanel().reformat(true);
    
    // Update the annotation panel header
    if (getAnnotationHeader() != null) {
      getAnnotationHeader().reformat();
      int position = getAnnotationPanel().getPositionForPixel(
          new Point(getScrollBar().getValue(), 0));
      getAnnotationHeader().update(position);
    }
    
    updateTitle();
  }
  
  /**
   * updates and paints the reference panel based on current state
   */
  public void reformatReferencePanel(Orientation orientation) {
    // maybe this should go somewhere else...
    getReferencePanel().reformat(false);
    ((TierPanelAddressHeader) getAddressComponent()).reformat();
    ((TierPanelAddressHeader) getAddressComponent()).repaint();
    updateTitle();
  }
  
  /**
   * updates the header for the reference panel
   */
  public abstract void reformatReferenceHeader();

  /**
   * Changes the view to look at the annotations on the opposite strand
   */
  public void switchAnnotations() {
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
    updateTitle();
  }
  
  /** I don't think this is used in any way... should probably remove */
  public abstract boolean annotationFilter(SeqFeatureI f);
  
  /** I don't think this is used in any way... should probably remove */
  public abstract boolean resultFilter(SeqFeatureI f);
  
  /**
   * Adds a collection of annotations to  the list of annotations 
   * (but not the annotation panel)
   * 
   * @param features a list of annotations
   * @return the number of annotations added
   */
  public int addAnnotations(java.util.Collection<SeqFeatureI> features) {
    int count = 0;

    for (SeqFeatureI feature : features) {
      if (feature.isAnnot() && !getAnnotations().contains(feature)) {
        getAnnotations().add(feature);
        count++;
      }
    }
    return count;
  }
  
  /**
   * Adds a collection of results to the list of results
   * (but not to the result panel)
   * @param features a list of results
   * @return the number of results added
   */
  public int addResults(java.util.Collection<SeqFeatureI> features) {
    int count = 0;

    for (SeqFeatureI feature : features) {
      if (!feature.isAnnot() && !getResults().contains(feature)) {
        getResults().add(feature);
        count++;
      }
    }
    return count;
  }
  
  /**
   * Gets the comparator used to order results
   */
  public abstract MultiComparator getResultComparator();

  /**
   * Gets the comparator used to order annotations
   */
  public abstract MultiComparator getAnnotationComparator();
  
  /**
   * Gets the filter used to decide which results to display
   */
  public abstract MultiFilter<SeqFeatureI> getResultFilter();

  /**
   * Gets the filter used to decide which annotations to display
   */
  public abstract MultiFilter<SeqFeatureI> getAnnotationFilter();
  
  
  /**
   * Sorts the result list using the given comparator
   * I dont think this is used anymore...
   * @param comparator
   */
  public void sortResults(Comparator comparator) {
    Object[] oldSet = getResults().toArray();
    Arrays.sort(oldSet, comparator);
    setResults(new ArrayList<SeqFeatureI>(oldSet.length));
    
    for (Object result : oldSet) {
      getResults().add((SeqFeatureI) result);
    }
  }
  
  /**
   * Sorts the annotation list using the given comparator
   * I dont' think this is used anymore...
   * @param comparator
   */
  public void sortAnnotations(Comparator comparator) {
    Object[] oldSet = getAnnotations().toArray();
    Arrays.sort(oldSet, comparator);
    setAnnotations(new ArrayList<SeqFeatureI>(oldSet.length));
    
    for (Object result : oldSet) {
      getAnnotations().add((SeqFeatureI) result);
    }
  }
  
  /**
   * Sorts a collection of features using the given comparator
   * @param collection
   * @param comparator
   * @return the sorted set
   */
  public List<SeqFeatureI> sort(java.util.Collection<SeqFeatureI> collection, 
      Comparator comparator) {
    List<SeqFeatureI> result = new ArrayList<SeqFeatureI>(collection.size());
    Object[] objs = collection.toArray();
    Arrays.sort(objs, comparator);
    
    for (Object obj : objs) {
      result.add((SeqFeatureI)obj);
    }
    return result;
  }
  
  
  /*  Start AnnotationChangeListenter interface */
  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    if (this.isVisible() && evt.getSource() != this) {
      if (evt.isCompound()) {
        for (int i = 0; i < evt.getNumberOfChildren(); ++i) {
          handleAnnotationChangeEvent(evt.getChildChangeEvent(i));
        }
        reformatAnnotationPanel();
        return true;
      }
      
      // can probably be done better, this will reformat the panel a lot
      reformatAnnotationPanel();
      
      if (evt.isEndOfEditSession()) {
        
        getOverview().updateState();
        setSelection(getSelection());
        updateEditRegion();
        
        repaint();
        updateTitle();
      }
      
      if (evt.isAdd()) {
        AnnotatedFeatureI annotToDisplay = null;
        AnnotatedFeatureI gene = evt.getChangedAnnot();
        int trans_count = gene.size();
        for (int i = 0; i < trans_count; ++i) {
          Transcript t = (Transcript) gene.getFeatureAt (i);
          if (evt.isUndo()) {
            //System.out.printf("Attaching %s(%d)\n", t, t.hashCode());
            //addFeatureToPanel(t, getAnnotationPanel());
            if (getSelection() != null && t.getName().equals(getSelection().getName())) {
              annotToDisplay = t;
            }
          }
        }
        
        if (annotToDisplay == null) {
          annotToDisplay = (AnnotatedFeatureI)evt.getChangedAnnot().getFeatureAt(0);
        } else {
          setSelection(annotToDisplay);
        }
        
        getOverview().updateState();
      }
      
      if (evt.isDelete()) {
      }
    }
    
    repaint();
    return true;
  }
  /* End AnnotationChangeListener interface */
  
  
  
  //**************************************************
  //
  //    STATIC METHODS
  //
  //**************************************************
  
  /**
   * Factory method to create the correct display panel based on the type
   */
  public static MultiSequenceAlignerPanel makeAligner(String type,
      GuiCurationState curationState, int strand, ReadingFrame frame) {
    
    if (SequenceI.DNA.equals(type)) {
      return DNAMultiSequenceAlignerPanel.makeAligner(curationState, strand);
    } else if (SequenceI.AA.equals(type)) {
      return AAMultiSequenceAlignerPanel.makeAligner(curationState, strand, frame);
    }
    
    throw new IllegalArgumentException("No Such Type: " + type);
  }
  
  
  /**
   * Gets all the annotations in a particular feature set
   * 
   * @param featureSet
   * @return
   */
  public static Set<SeqFeatureI> getAnnotations(SeqFeatureI featureSet) {
    HashSet<SeqFeatureI> result = new HashSet<SeqFeatureI>();

    if (featureSet.isTranscript()) {

      if (featureSet.isContainedByRefSeq())
        // only add if its fully in range
        result.add(featureSet);

    } else if (featureSet.isAnnotTop() && !featureSet.hasKids()) {
      // only add if its fully in range
      if (featureSet.isContainedByRefSeq())
        result.add(featureSet);

    } else {
      for (int i = 0, size = featureSet.getFeatures().size(); i < size; i++) {
        SeqFeatureI feature = featureSet.getFeatureAt(i);
        if (feature.canHaveChildren()) {
          result.addAll(getAnnotations(feature));
        }
      }
    }

    return result;
  }

  /**
   * Gets all the results from a particular feature set
   * Don't think this is used...
   * @param type the type of results wanted, ether AA or DNA
   * @param featureSet
   * @return
   */
  public static Set<SeqFeatureI> getResults(String type, SeqFeatureI featureSet) {
    HashSet<SeqFeatureI> result = new HashSet<SeqFeatureI>();

    if (featureSet instanceof FeaturePairI) {
      FeaturePairI feature = (FeaturePairI) featureSet;
      SequenceI sequence = feature.getFeatureSequence();
      String seqType = sequence.getResidueType();
      String alignment = feature.getExplicitAlignment();

      if (alignment != null && 
          (type.equals(seqType) || 
           type.equals(SequenceI.AA) == isAminoAcid(alignment)) ) {
        result.add(featureSet);
      }

    } else if (featureSet.getFeatures() != null) {
      for (int i = 0, size = featureSet.getFeatures().size(); i < size; i++) {
        SeqFeatureI feature = featureSet.getFeatureAt(i);
        result.addAll(getResults(type, feature));
      }
    }

    return result;
  }
  
  /**
   * Gets all the results from a particular feature set that pass the filter
   * 
   * @param featureSet
   * @param f the filter used to decide which results to get
   * @return
   */
  public static Set<SeqFeatureI> getResults(SeqFeatureI featureSet, Filter f) {
    HashSet<SeqFeatureI> result = new HashSet<SeqFeatureI>();

    if (featureSet instanceof FeaturePairI && f.keep(featureSet)) {
      result.add(featureSet);
    } else if (featureSet.getFeatures() != null) {
      for (int i = 0, size = featureSet.getFeatures().size(); i < size; i++) {
        SeqFeatureI feature = featureSet.getFeatureAt(i);
        result.addAll(getResults(feature, f));
      }
    }

    return result;
  }
  
  /**
   * Passes a collection of features through a filter
   * 
   * @param collection
   * @param f
   * @return the features in the given collection that pass the filter
   */
  public static Set<SeqFeatureI> filter(
      Collection<SeqFeatureI> collection, Filter f) {
    HashSet<SeqFeatureI> result = new HashSet<SeqFeatureI>();

    for (SeqFeatureI feature : collection) {
      if (f.keep(feature)) {
        result.add(feature);
      }
    }

    return result;
  }
  
  
  
  
  /** 
   * puts up vertical lines in szap(main window) 
   * indicating what region that is currently displayed
   **/
  public void addEditRegion() {
    boolean isReverse = false;
    if (getAnnotationPanel().getStrand() == Strand.FORWARD) {
      isReverse = 
        getAnnotationPanel().getOrientation() == Orientation.THREE_TO_FIVE;
    } else {
      isReverse = 
        getAnnotationPanel().getOrientation() == Orientation.FIVE_TO_THREE;
    }
    
    colorIndex = (colorIndex + 1) % colorList.length;
    setIndicatorColor(colorList[colorIndex]);
    //colorSwatch.setBackground(indicatorColor);
    getCurationState().getSZAP()
      .addHighlightRegion(this, getIndicatorColor(), isReverse);
  }
  
  /**
   * Something to do with the box that shows what part is being displayed in
   * the main apollo window
   */
  protected void updateEditRegion() {
    boolean isReverse = false;
    if (getAnnotationPanel().getStrand() == Strand.FORWARD) {
      isReverse = 
        getAnnotationPanel().getOrientation() == Orientation.THREE_TO_FIVE;
    } else {
      isReverse = 
        getAnnotationPanel().getOrientation() == Orientation.FIVE_TO_THREE;
    }
    
    this.getCurationState().getSZAP()
      .updateHighlightRegion(this, this.getIndicatorColor(), isReverse);
  }
  
  /**
   * Scrolls the window to the start/end of the next annotation to the right.
   */
  public void nextAnnotation() {
    int basepair = getVisibleBase();
    if (getType().equals("AA")) {
        basepair += 3 * getAnnotationPanel().getStrand().toInt();
    }
    int position = getAnnotationPanel().tierPositionToPixelPosition(
        getAnnotationPanel().basePairToTierPosition(basepair));
    SeqFeatureI feature = getAnnotationPanel().getNextFeature(position);
    
    if (feature != null) {
      
      if (getAnnotationPanel().getOrientation() == Orientation.FIVE_TO_THREE){
        scrollToBase(feature.getStart());
      } else {
        scrollToBase(feature.getEnd());
      }
      
      AnnotatedFeatureI selection = null;
      if (feature instanceof AnnotatedFeatureI) {
        selection = getTransOrOneLevelAnn((AnnotatedFeatureI)feature);
      }
      
      if (selection != null && selection.isTranscript() &&
          ((Transcript)selection).haveWholeSequence()) {
        setSelection((AnnotatedFeatureI)feature);
        getCurationState().getSelectionManager().select(feature, this);
      } else if (selection != null && selection != getSelection()) {
        nextAnnotation();
      }
    }
    
  }
  
  /**
   * Scrolls the window to the start/end of the next annotation to the left
   */
  public void prevAnnotation() {
    int basepair = getVisibleBase();
    int offset = getType().equals("AA") ? 3 : 1;
    basepair += (getVisibleBaseCount()-offset)*getStrand().toInt();

    int position = getAnnotationPanel().tierPositionToPixelPosition(
        getAnnotationPanel().basePairToTierPosition(basepair));
        
    SeqFeatureI feature = getAnnotationPanel().getPrevFeature(position);
    
    if (feature != null) {
      
      // get highest pixel position
      position = getAnnotationPanel().getHigh(feature);
    
      position = getType().equals("AA") ? 
          position - (getVisibleBaseCount()/3):
          position - getVisibleBaseCount();
      position = getAnnotationPanel().pixelPositionToTierPosition(position);
      int bp = getAnnotationPanel().tierPositionToBasePair(position);
      scrollToBase(bp);
      
      AnnotatedFeatureI selection = null;
      if (feature instanceof AnnotatedFeatureI) {
        selection = getTransOrOneLevelAnn((AnnotatedFeatureI)feature);
      }
      
      if (selection != null && selection.isTranscript() &&
          ((Transcript)selection).haveWholeSequence()) {
      
        setSelection((AnnotatedFeatureI)feature);
        getCurationState().getSelectionManager().select(feature, this);
      } else if (selection != null && selection != getSelection()) {
        prevAnnotation();
      }
    }
  }
  
  /**
   * true if no external selection is allowed
   * false otherwise
   * @return
   */
  public boolean noExternalSelection() {
    return noExternalSelection;
  }
  
  public void setNoExternalSelection(boolean noExternalselection) {
    this.noExternalSelection = noExternalselection;
  }
  
  
  
  //***********************************************
  //
  //  Private methods
  //
  //***********************************************
  
  
  
  
  //TODO:find a way to figure out if the alignment or feature is an aminoAcid...
  private static boolean isAminoAcid(String a) {
    return a.contains("E") || a.contains("V") || a.contains("P") || 
    a.contains("M") || a.contains("R") || a.contains("Q") || a.contains("L")
    || a.contains("Y") || a.contains("S") || a.contains("H");
  }
  
  /** 
   * Does all the handle selection checks
   */
  public boolean canHandleSelection(FeatureSelectionEvent evt,Object self) {
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
    if (is_reverse != (this.getStrand() == Strand.REVERSE))
      return false;
    if ( ! (sf instanceof AnnotatedFeatureI))
      return false;//repaint transVw?
    else
      return true;
  }
  
  /** 
   * rue if the selection comes from outside world. 
   * false if from this OR editorPanel
   */
  private boolean isExternalSelection(FeatureSelectionEvent e) {
    return e.getSource() != this;
  }
  
  /** AssemblyFeature is the only GAI at the moment that has no relation to
  // a transcript. Dont think they can end up in ede (??).
  // Would be nice to have an interface that was solely for the exon
  // trans gene heirarchy? Should we actively make sure AssemblyFeatures dont
  // get into ede? (filter in AnnotationMenu?)
  returns null if no transcript can be found. */
 public AnnotatedFeatureI getTransOrOneLevelAnn(AnnotatedFeatureI af) {
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
 
 
 
 protected AbstractTierPanel addFeatureToPanel(SeqFeatureI feature,
     MultiTierPanel panel) {
   
   AbstractTierPanel tierPanel = null;
   if (!panel.canAddFeature(feature)) {
     tierPanel = panel.addFeature(feature);
   } else {
     panel.addFeature(feature);
   }
   return tierPanel;
 }
  

}
