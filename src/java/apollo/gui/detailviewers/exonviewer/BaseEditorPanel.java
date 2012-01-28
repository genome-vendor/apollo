package apollo.gui.detailviewers.exonviewer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeCellEditor.EditorContainer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import apollo.datamodel.*;
import apollo.seq.*;
import apollo.seq.io.FastaFile;
import apollo.util.*;
import apollo.gui.SequenceSelector;
import apollo.gui.event.*;
import apollo.editor.AnnotationEditor;
import apollo.editor.UserName;
//import apollo.editor.ChangeList;
import apollo.gui.ApolloFrame;
import apollo.config.Config;
import apollo.gui.SelectionManager;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.genomemap.AnnotationView;
import apollo.gui.genomemap.ViewI;
import apollo.gui.genomemap.ContainerViewI;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.synteny.GuiCurationState;

import org.apache.log4j.*;

import org.bdgp.util.DNAUtils;

public class BaseEditorPanel extends SeqAlignPanel {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BaseEditorPanel.class);
  public static final int SEQ_SELECT_DRAG = 2100;
  private static final int MIN_FEATURE_SIZE = 1;
  private static Object [] bases = {"A", "T", "G", "C", "N", "Cancel"};
  private static Object [] bases_noA = {"T", "G", "C", "N", "Cancel"};
  private static Object [] bases_noT = {"A", "G", "C", "N", "Cancel"};
  private static Object [] bases_noG = {"A", "T", "C", "N", "Cancel"};
  private static Object [] bases_noC = {"A", "T", "G", "N", "Cancel"};

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected int padAmt = 15;
  public SeqFeatureI selectedFeature;
  protected int selectedTier;
  //protected ApolloPanel controllingPanel;
  //protected ApolloFrame controllingWindow;
  protected AnnotationEditor annotationEditor;
  //protected AnnotationView annotationView;
  protected BaseFineEditor baseFineEditor;

  protected JLabel locationLabel;
  protected JLabel relativeLocationLabel;

  protected JPopupMenu rightClickMenu;
  protected JMenuItem makeIntronMenuItem;
  protected JMenuItem createExonMenuItem;
  protected JMenuItem merge5PrimeMenuItem;
  protected JMenuItem merge3PrimeMenuItem;
  protected JMenuItem set5PrimeMenuItem;
  protected JMenuItem set3PrimeMenuItem;
  protected JMenuItem deleteExonMenuItem;
  protected JMenuItem startTranslationMenuItem;
  protected JMenuItem translationSlipMenuItem;
  protected JMenuItem translationStutterMenuItem;
  protected JMenuItem insertionMenuItem;
  protected JMenuItem deletionMenuItem;
  protected JMenuItem substitutionMenuItem;
  protected JMenuItem noAdjustmentMenuItem;
  protected JMenu sequencingErrorSubMenu;
  protected JMenuItem findSequenceMenuItem;
  protected JMenuItem sequenceMenuItem;

  protected Color seqSelectColor = new Color(255,153,0);

  // Box that goes over exon diagram at the bottom to show which region
  // you're seeing in the detailed view
  protected Color selectionBoxColor = Color.black;

  private SequenceI refSeq;
  protected SequenceI [] frame = {null, null, null};

  BaseMouseListener mouseListener; // inner class
  RightClickActionListener rightClickListener;

  protected int selectBeginPos = -1;
  protected int selectCurrentPos = -1;

  protected int [] splices = new int[2];

  protected Vector hitZones = new Vector();
  protected Vector translatedHitZones;

  protected boolean showHitZones = false;

  private GuiCurationState curationState;

  /** inner class that listens for selection events */
  private BaseEditorSelectionListener baseEditorSelectionListener;

  /** Need lowestBase for offset of sequence */
  public BaseEditorPanel(GuiCurationState curationState,
      final BaseFineEditor baseFineEditor,
      boolean reverseStrand,
      int lowestBase,
      int highestBase,
      SeqFeatureI annotFeatureSet) {
    super(1, (int)lowestBase, (int)highestBase);
    this.curationState = curationState;
    setFont(Config.getExonDetailEditorSequenceFont());
    translatedHitZones = new Vector(3);
    translatedHitZones.addElement(null);
    translatedHitZones.addElement(null);
    translatedHitZones.addElement(null);

    setReverseStrand(reverseStrand);
    //this.controllingPanel = controllingPanel;
    //this.controllingWindow=SwingUtilities.getAncestorOfClass(ApolloFrame.class,controllingPanel);
    this.annotationEditor = curationState.getAnnotationEditor(!reverseStrand);
    this.baseFineEditor = baseFineEditor;
    setStripeWidth(10);
    // Default row header does the numbering -
    // not sure how it gets hooked up
    this.defaultRowHeader = new BaseFineEditorRowHeader(this);
    createRightClickMenu();
    attachListeners();
    // selections both from outside world and BaseFineEditor
    baseEditorSelectionListener = new BaseEditorSelectionListener();
    baseFineEditor.getController().addListener(baseEditorSelectionListener);
    // retrieves and adds the transcripts
    addTranscriptsFromFeatSet(annotFeatureSet);
  }

  AnnotationEditor getAnnotationEditor() {
    return curationState.getAnnotationEditor(!getReverseStrand());
  }

  public void setShowHitZones(boolean showHitZones) {
    this.showHitZones = showHitZones;
    repaint();
  }

  public boolean getShowHitZones() {
    return showHitZones;
  }

  /** Digs out Transcripts from annotFeatureSet and adds them */
  private void addTranscriptsFromFeatSet(SeqFeatureI annotFeatureSet) {
    //Vector transList = new Vector();
    Vector annots = getTranscriptsAnd1LevAnnots(annotFeatureSet);
    attachTranscripts(annots); // adds trans and amino seqs   
  }

  /** Recurses featureSet looking for Transcripts. Adds Transcripts to
      Vector output if they are fully in range */
  private Vector getTranscriptsAnd1LevAnnots(SeqFeatureI annotFeatureSet) {
    Vector output = new Vector();
    if (annotFeatureSet.isTranscript()) {
      //Transcript trans = (Transcript)annotFeatureSet;
      // only add if in cur set range
      if (annotFeatureSet.isContainedByRefSeq())
        output.add(annotFeatureSet);
    }
    else if (isOneLevelAnnot(annotFeatureSet)) {
      output.add(annotFeatureSet);
    }
    else {
      for(int i=0; i < annotFeatureSet.getFeatures().size(); i++) {
        SeqFeatureI feature = annotFeatureSet.getFeatureAt(i);
        if (feature.canHaveChildren())
          output.addAll(getTranscriptsAnd1LevAnnots(feature)); // recurse
      }
    }
    return output;
  }


  public void setHitZones(Vector hitZones) {
    this.hitZones = hitZones;
  }

  public void setTranslatedHitZones(Vector hitZones, int frame) {
    translatedHitZones.setElementAt(hitZones, frame);
  }

  public Vector getTranslatedHitZones(int frame) {
    if (frame >= translatedHitZones.size())
      return null;
    return (Vector) translatedHitZones.elementAt(frame);
  }


  void cleanup() {
    baseFineEditor.getController().removeListener(baseEditorSelectionListener);
    memLeakBugWorkAround();
  }

  /** Fix for java 1.3.1 mem leak bug on pop up menu, nullify refs leading to 
      curation set */
  private void memLeakBugWorkAround() { 
    annotationEditor = null; 
    sequences = null;
    selectedFeature = null;
    refSeq = null;
  }

  public int selectLowPos() {
    return (selectBeginPos < selectCurrentPos ?
        selectBeginPos : selectCurrentPos);
  }

  public int selectHighPos() {
    return (selectBeginPos > selectCurrentPos ?
        selectBeginPos : selectCurrentPos);
  }

  void setSelectCurrentPos(int scp) {
    this.selectCurrentPos = scp;
  }

  //int getSelectBeginPos() { return selectBeginPos; }
  void setSelectBeginPos(int sbp) {
    this.selectBeginPos = sbp;
  }

  public int getRowMargin() {
    return 20;
  }

  /** The inner array is an array of 2 colors, for alternating exon colors.
      The "outer" array represents transcript coloring. Transcripts can have
      different exon coloring pairs(see SelectableDNARenderer's exonColorIndex
      and transcriptColorIndex). 
      This now takes in 2 colors from Config edeFeatureColor. It no longer 
      is setting up 4 colors for alternating transcripts as well as exons.
      Easy enough to reinstate if desired.
   */
  public Color [][] getColorArray() {
    Color firstExonColor = Config.getExonDetailEditorFeatureColor1();
    Color secondExonColor = Config.getExonDetailEditorFeatureColor2();
    Color [][] colors = {{firstExonColor,secondExonColor}};
    //Color [][] colors = {{Color.blue, new Color(0,0,180)},
    //{new Color(100,150,255), new Color(100,0,200)}};
    return colors;
  }

  public SeqFeatureI getSelectedFeature() {
    return selectedFeature;
  }

  public int getSelectedTier() {
    return selectedTier;
  }

  /** Adds feature to an existing SeqWrapper if it doesnt overlap with it,
   * otherwise creates a new SeqWrapper for it,
   * @return The element number in sequences,
   * which is synonomous with tier number
   */
  public int addFeature(SeqFeatureI feature) {
    return addAlignFeature (refSeq, feature);
  }


  /** Called on internal selection. Both fires off selection event and shows feature 
      selected. */
  void selectAnnot(int pos, int tier) {
    SeqFeatureI sf = getFeatureSetAtPosition(pos, tier);
    showAnnotSelected(sf, tier); // red box
    // selection manager select fires event
    if (sf!=null) baseFineEditor.getSelectionManager().select(sf, this);
  }

  /** This just sets up state so feature will be drawn as selected with red box
      around it */
  private void showAnnotSelected(SeqFeatureI sf, int tier) {
    if (sf == null) 
      return;
    selectedTier = tier;
    selectedFeature = sf;
  }

  /** Display GenericAnnotationI sf.
      This is for external selection. No selection event is fired.
      If SeqFeatureI is a gene, its first transcript is selected, as selection
      is done at the transcript(or exon) level. It makes no sense to select 
      a gene in the BaseEditorPanel.
   */
  void displayAnnot(AnnotatedFeatureI sf) { 
    if (!(sf.isTranscript()) && !(sf.isExon()) && sf.hasKids())
      sf = sf.getFeatureAt(0).getAnnotatedFeature();
    int tier = getTierForFeature(sf);
    if (tier < 0) {
      // this is a new feature
      logger.error("Can't scroll to " + sf.getName() + " because it can't be found", new Throwable());
    } else {
      // red box around annot - false no selection firing
      showAnnotSelected (sf, tier); 
      scrollToFeature (sf);
      // repaint to show new selection, scrollToFeat doesnt repaint 
      // if no scroll
      repaint(); 
    }
  }

  private Vector findPositions(String seq, String search_str) {
    Vector out = new Vector();
    // The sequence is already in upper case
    // The user might not have typed in an upper case
    // string however.
    search_str = search_str.toUpperCase();
    int index = seq.indexOf(search_str, 0);
    while(index >= 0) {
      int [] indices = new int[2];
      indices[0] = index;
      indices[1] = index + search_str.length();
      out.addElement(indices);
      index = seq.indexOf(search_str, index+1);
    }
    return out;
  }

  /**
   * returns the positions of the donor and acceptor
   * splice sites in coordinates relative to the genomic
   * sequence. The index of 0 points to the lower coord
   * whether or not it is the donor
   */

  private void findSplices (int pos, int strand, boolean add_exon,
      int low_limit, int high_limit) {
    int start_pos = Math.max (pos - 12, (int) low_limit + 1);
    int end_pos = Math.min (pos + 12, (int) high_limit - 1);
    int dna_start = posToResidue (start_pos);
    int dna_end = posToResidue (end_pos);
    int donor;
    int acceptor;
    boolean well_ordered;
    String dna = refSeq.getResidues(dna_start, dna_end);
    if (add_exon) {
      // inserting an exon
      // exon starts after the AG
      acceptor  = dna.indexOf("AG");
      // exon ends before the GT
      donor = dna.indexOf ("GT", (int) (dna.length()*0.5));
      well_ordered = donor != -1 && acceptor != -1 && (acceptor < donor);
    } else {
      // inserting an intron
      // exon ends before the GT
      donor = dna.indexOf ("GT");
      // exon starts after the AG
      acceptor  = dna.indexOf("AG", (int) (dna.length()*0.5));
      well_ordered = donor != -1 && acceptor != -1 && (donor < acceptor);
    }
    if (!well_ordered) {
      if (add_exon) {
        acceptor = posToBasePair (pos);
        donor = posToBasePair (pos);
      } else {
        donor = posToBasePair (pos - 1);
        acceptor = posToBasePair (pos + 1);
      }
    } else {
      donor = residueToBasePair(donor - 1 + dna_start);
      acceptor = residueToBasePair(acceptor + 2 + dna_start);
    }
    splices[0] = donor;
    splices[1] = acceptor;
  }
  private class GoToDialog extends JDialog {
    JLabel posLabel;
    JTextField posField;
    JButton goButton;

    public GoToDialog() {
      posLabel = new JLabel("Position");
      posField = new JTextField();
      goButton = new JButton("GoTo");
      init();
    }
    private void init() {
      Box posBox = new Box(BoxLayout.X_AXIS);
      posBox.add(Box.createHorizontalStrut(5));
      posBox.add(posLabel);
      posBox.add(Box.createHorizontalStrut(10));
      posBox.add(posField);
      posBox.add(Box.createHorizontalStrut(15));
      posBox.add(goButton);      
      getContentPane().add(posBox);
      setTitle("Exon Detail Editor GoTo");
      setSize(250,50);

      goButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          goTo();
        }
      });
    }    

    private void goTo() {
      String pos = posField.getText();
      if (pos.length() < 1)
        return;
      try {
        int position = Integer.parseInt(pos);
        scrollToBase(position);
        Vector zones = new Vector();
        int [] match_positions = new int[2];
        if (reverseStrand) {
          match_positions[0] = basePairToPos(position);
          match_positions[1] = basePairToPos((position-1));
        } else {
          match_positions[0] = basePairToPos(position);
          match_positions[1] = basePairToPos((position+1));
        }
        zones.add(match_positions);
        setHitZones(zones);
        setShowHitZones(true);
        baseFineEditor.clearFindsButton.setEnabled(true);
        dispose();
      } catch (Exception e) {
        logger.error("Not an integer: " + pos);
      }
    }
  }
  private class FindDialog extends JDialog {
    JLabel seqLabel;
    JTextField seqField;
    JRadioButton isAminoAcidButton;
    JRadioButton isGenomicButton;
    JTable posTable;
    JButton searchButton;
    Vector colNames;

    public FindDialog() {
      seqLabel = new JLabel("Sequence");
      seqField = new JTextField();
      isAminoAcidButton = new JRadioButton("is amino acid sequence");
      isGenomicButton = new JRadioButton("is genomic sequence");
      colNames = new Vector();
      colNames.addElement("Position");
      colNames.addElement("Query_frame"); // rename from phase to Query_frame
      posTable = new JTable(new Vector(), colNames);
      searchButton = new JButton("Search");
      init();
      installListeners();
    }

    public void init() {
      posTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      ButtonGroup radioButtons = new ButtonGroup();
      radioButtons.add(isAminoAcidButton);
      radioButtons.add(isGenomicButton);

      searchButton.setAlignmentX((float) .5);
      Box seqBox = new Box(BoxLayout.X_AXIS);
      seqBox.add(Box.createHorizontalStrut(10));
      seqBox.add(seqLabel);
      seqBox.add(Box.createHorizontalStrut(5));
      seqBox.add(seqField);
      seqBox.add(Box.createHorizontalStrut(10));

      Box buttonBoxes = new Box(BoxLayout.X_AXIS);
      buttonBoxes.add(isAminoAcidButton);
      buttonBoxes.add(Box.createHorizontalStrut(5));
      buttonBoxes.add(isGenomicButton);

      JScrollPane pane = new
      JScrollPane(posTable,
          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      getContentPane().setLayout(new BoxLayout(getContentPane(),
          BoxLayout.Y_AXIS));
      getContentPane().add(Box.createVerticalStrut(10));
      getContentPane().add(seqBox);
      getContentPane().add(buttonBoxes);
      getContentPane().add(pane);
      getContentPane().add(searchButton);
      setTitle("Exon Detail Editor sequence search");
      setSize(400,300);
    }

    class FindTableModel extends javax.swing.table.AbstractTableModel {
      Vector coords;
      Vector phases;

      public FindTableModel() {
        coords = new Vector();
        phases = new Vector();
      }

      public void add(int [] coord, int phase) {
        coords.addElement(coord);
        phases.addElement(new Integer(phase));
      }

      public int getColumnCount() {
        return 2;
      }

      public String getColumnName (int col) {
        return ((String) colNames.elementAt(col));
      }

      public int getRowCount() {
        return coords.size();
      }

      public Object getValueAt(int row, int col) {
        if (col == 0) {
          int [] coord = (int []) coords.elementAt(row);
          return coord[0]+" - "+coord[1];
        } else {
          Integer myInt = (Integer) phases.elementAt(row);
          if (myInt.intValue() == -1)
            return "N/A";
          else
            return myInt.toString();
        }
      }

      public int getBaseForRow(int row) {
        if (row >= 0 && row < coords.size()) {
          int [] coord = (int []) coords.elementAt(row);
          return coord[0];
        } else {
          return 1;
        }
      }
    }

    public void doSearch() {
      String seqText = seqField.getText();
      if (seqText.length() < 1)
        return;

      SequenceSelector seqSelector;
      SeqFeatureI seqFeature;
      FindTableModel model = new FindTableModel();

      if (isAminoAcidButton.isSelected()) {
        for(int j=0; j < 3; j++) {
          SequenceI aa_seq = frame[j];
          seqFeature = new SeqFeature(1,aa_seq.getLength(),"sequence");
          seqFeature.setRefSequence(aa_seq);
          seqSelector = new SequenceSelector(seqFeature,seqText,false,true);
          //          String aa = aa_seq.getResidues();  // not used
          // These are essentially in the window
          // coord system. That is, the positions
          // are from 0 to the length of the sequence.
          Vector matches = seqSelector.getZones();
          setTranslatedHitZones(matches, j);
          for(int i=0; i < matches.size(); i++) {
            int [] zone = (int []) matches.elementAt(i);
            zone[0]--;
            // change the aa positions into nucleic acid
            // window coords
            int start = (zone[0] * 3) + j;
            int end = (zone[1] * 3) + j;
            int [] found_pos = new int[2];
            if (reverseStrand) {
              found_pos[1] = posToBasePair(start);
              found_pos[0] = posToBasePair(end);
            } else {
              found_pos[0] = posToBasePair(start);
              found_pos[1] = posToBasePair(end);
            }
            // j is 0,1,2. frame needs to be 1,2,3 -> add one
            int queryFrame = j + 1;
            model.add(found_pos, queryFrame);
          }
        }
      } else {
        seqFeature = new SeqFeature(1,refSeq.getLength(),"sequence");
        Sequence backbone = 
          new Sequence(String.valueOf(System.currentTimeMillis()),
              refSeq.getResidues());
        seqFeature.setRefSequence(backbone);
        seqSelector = 
          new SequenceSelector(seqFeature,seqText,false,true);
        Vector matches = seqSelector.getZones();
        setHitZones(matches);
        for(int i=0; i < matches.size(); i++) {
          int [] zone = (int []) matches.elementAt(i);
          zone[0]--;
          // change the aa positions into nucleic acid
          // window coords
          int [] found_pos = new int[2];
          if (reverseStrand) {
            found_pos[1] = posToBasePair(zone[0]);
            found_pos[0] = posToBasePair(zone[1]);
          } else {
            found_pos[0] = posToBasePair(zone[0]);
            found_pos[1] = posToBasePair(zone[1]);
          }
          model.add(found_pos, -1);
        }
      }
      posTable.setModel(model);
      // BaseFineEditor baseFineEditor = (BaseFineEditor) SwingUtilities.
      // getAncestorOfClass(JFrame.class, BaseEditorPanel.this);
      baseFineEditor.clearFindsButton.setEnabled(true);
      setShowHitZones(true);
    }

    public void installListeners() {
      searchButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doSearch();
        }
      }
      );
      ListSelectionModel select_model = posTable.getSelectionModel();
      select_model.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      select_model.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            int row = posTable.getSelectedRow();
            if (row >= 0) {
              FindTableModel ftm = (FindTableModel) posTable.
              getModel();
              scrollToBase(ftm.getBaseForRow(row));
            }
          }
        }
      }
      );
    }
  }

  private class RightClickActionListener implements ActionListener {
    int pos;
    int tier;

    public int getPos() {
      return pos;
    }

    // make sure to call setEndExonId &
    // setStartExonId after deleting exons in merge
    // and delete
    public void actionPerformed(ActionEvent e) {
      pos =  mouseListener.getPos();
      tier = mouseListener.getTier();
      selectAnnot(pos, tier);
      SeqFeatureI feature = getFeatureAtPosition(pos, tier);
      int basepair = posToBasePair (pos);

      // MAKE INTRON
      // This needs fixing! tries to create intron that extends to splice sites but
      // its only making 1 bp introns - this used to work. FIX!
      if (e.getSource() == makeIntronMenuItem) {
        Vector exons = new Vector();
        exons.addElement(feature);
        int strand = feature.getStrand();
        int start = basePairToPos((int) feature.getStart());
        int end = basePairToPos((int) feature.getEnd());
        boolean make_a_break = true;
        if (pos != start && pos != end) {
          findSplices (pos,
              strand,
              false,
              start,
              end);
          int donor_pos = basePairToPos(splices[0]);
          int acceptor_pos = basePairToPos(splices[1]);
          if (notTooSmall(start, donor_pos) &&
              notTooSmall(acceptor_pos, end)) {
            make_a_break = false;
            if (splices[0] < splices[1])
              annotationEditor.setSelections(splices[0],
                  splices[1],
                  exons,
                  exons,
                  null,
                  null);
            else
              annotationEditor.setSelections(splices[1],
                  splices[0],
                  exons,
                  exons,
                  null,
                  null);
          }
        }
        if (make_a_break)
          annotationEditor.setSelections(basepair,
              -1,
              exons,
              exons,
              null,
              null);
        annotationEditor.splitExon();
      } 

      // DELETE EXON
      else if (e.getSource() == deleteExonMenuItem) {
        Vector exons = new Vector();
        exons.addElement(feature);
        annotationEditor.setSelections(exons,
            exons,
            null,
            null);
        //annotationEditor.deleteExon();
        annotationEditor.deleteSelectedFeatures();
      } 

      // CREATE EXON
      else if (e.getSource() == createExonMenuItem) {
        SeqFeature temp = new SeqFeature();
        if (reverseStrand)
          temp.setStrand(-1);
        else
          temp.setStrand(1);
        // the start is the acceptor splice
        temp.setStart(splices[1]);

        // the end is the donor splice
        temp.setEnd(splices[0]);

        Vector transcripts = new Vector();
        // This was wrong - it ended up always using the 1st transcript
        // no matter what transcript was actually selected
        transcripts.addElement(feature);

        Vector exons = new Vector();
        exons.addElement(temp);
        annotationEditor.setSelections(transcripts,
            transcripts,
            exons,
            null);
        annotationEditor.addExons();
      } else if (e.getSource() == merge5PrimeMenuItem) {
        int index = getExonRangeIndex(tier,
            basePairToPos((int) feature.getStart()),
            basePairToPos((int) feature.getEnd()),
            true);
        SeqFeatureI nextExon = getFeatureAtIndex(tier, index-1);

        Vector exons = new Vector();
        exons.addElement(feature);
        exons.addElement(nextExon);
        annotationEditor.setSelections(exons,
            exons,
            null,
            null);
        annotationEditor.mergeExons();

      } else if (e.getSource() == merge3PrimeMenuItem) {
        int index = getExonRangeIndex(tier,
            basePairToPos((int) feature.getStart()),
            basePairToPos((int) feature.getEnd()),
            true);
        SeqFeatureI nextExon = getFeatureAtIndex(tier, index+1);

        Vector exons = new Vector();
        exons.addElement(feature);
        exons.addElement(nextExon);
        annotationEditor.setSelections(exons,
            exons,
            null,
            null);
        annotationEditor.mergeExons();
      } else if (e.getSource() == set5PrimeMenuItem) {
        SeqFeatureI temp = new SeqFeature();
        if (reverseStrand)
          temp.setStrand(-1);
        else
          temp.setStrand(1);
        temp.setStart(basepair);
        temp.setEnd(basepair);
        Vector temps = new Vector();
        temps.addElement(temp);
        Vector exons = new Vector();
        exons.addElement(feature);
        annotationEditor.setSelections(exons, exons, temps, null);
        annotationEditor.setExonTerminus(annotationEditor.START);
      } else if (e.getSource() == set3PrimeMenuItem) {
        SeqFeatureI temp = new SeqFeature();
        if (reverseStrand)
          temp.setStrand(-1);
        else
          temp.setStrand(1);
        temp.setStart(basepair);
        temp.setEnd(basepair);
        Vector temps = new Vector();
        temps.addElement(temp);
        Vector exons = new Vector();
        exons.addElement(feature);
        annotationEditor.setSelections(exons, exons, temps, null);
        annotationEditor.setExonTerminus(annotationEditor.END);
      } else if (e.getSource() == startTranslationMenuItem) {
        annotationEditor.setSelections(null, null, null, null);
        annotationEditor.setTranslationStart((ExonI)feature, basepair);
      } else if (e.getSource() == translationSlipMenuItem) {
        annotationEditor.setSelections(null, null, null, null);
        if (translationSlipMenuItem.getText().startsWith("Remove"))
          annotationEditor.setFrameShiftPosition((ExonI)feature, 0, true);
        else
          annotationEditor.setFrameShiftPosition((ExonI)feature, 
              basepair,
              true);
      } else if (e.getSource() == translationStutterMenuItem) {
        annotationEditor.setSelections(null, null, null, null);
        if (translationStutterMenuItem.getText().startsWith("Remove"))
          annotationEditor.setFrameShiftPosition((ExonI)feature, 0, false);
        else
          annotationEditor.setFrameShiftPosition((ExonI)feature, 
              basepair,
              false);
      } else if (e.getSource() == insertionMenuItem) {
        String base = getBaseFromUser("Enter base to be inserted at " +
            basepair,
            bases);
        if (base != null) {
          dealWithSequenceEdit(feature, basepair, SequenceI.INSERTION, base);
        }
      } else if (e.getSource() == deletionMenuItem) {
        dealWithSequenceEdit(feature, basepair, SequenceI.DELETION, null);
      } else if (e.getSource() == substitutionMenuItem) {
        char old_base = feature.getRefSequence().getBaseAt(basepair);
        // manual rev-comp of this single base
        if (feature.getStrand() < 0) {
          if (old_base == 'A')
            old_base = 'T';
          else if (old_base == 'T')
            old_base = 'A';
          else if (old_base == 'G')
            old_base = 'C';
          else if (old_base == 'C')
            old_base = 'G';
        }
        String base;
        if (old_base == 'A')
          base = getBaseFromUser("Enter base to substitute for " + 
              old_base + " at " + basepair,
              bases_noA);
        else if (old_base == 'T')
          base = getBaseFromUser("Enter base to substitute for " + 
              old_base + " at " + basepair,
              bases_noT);
        else if (old_base == 'G')
          base = getBaseFromUser("Enter base to substitute for " + 
              old_base + " at " + basepair,
              bases_noG);
        else if (old_base == 'C')
          base = getBaseFromUser("Enter base to substitute for " + 
              old_base + " at " + basepair,
              bases_noC);
        else
          base = getBaseFromUser("Enter base to substitute for " + 
              old_base + " at " + basepair,
              bases);
        if (base != null) {
          dealWithSequenceEdit(feature,basepair,SequenceI.SUBSTITUTION,base);
        }
      } else if (e.getSource() == noAdjustmentMenuItem) {
        dealWithSequenceEdit(feature,basepair,SequenceI.CLEAR_EDIT,null);
      } else if (e.getSource() == findSequenceMenuItem) {
        showFindDialog();
      } 
      repaint();
      //controllingPanel.repaint();
      curationState.getSZAP().repaint();
    } // end of actionPerformed()
  } // end of RightClickActionListener inner class -> make outer class


  protected void showFindDialog() {
    FindDialog fd = new FindDialog();
    fd.show();
  }

  protected void showGoToDialog() {
    GoToDialog gtd = new GoToDialog();
    gtd.show();
  }

  protected boolean notTooSmall (int start_pos, int end_pos) {
    return (end_pos - start_pos + 1) >= MIN_FEATURE_SIZE;
  }

  protected void changeStart(SeqFeatureI feature, int base_offset,
      int limit_5prime, int limit_3prime) {
    if (base_offset == 0)
      return;
    int new_start
    = feature.getStart() + (base_offset * feature.getStrand());
    boolean okay = (reverseStrand ?
        (new_start > limit_3prime && new_start < limit_5prime) :
          (new_start < limit_3prime && new_start > limit_5prime));
    if (okay) {
      feature.setStart(new_start);
      FeatureSetI parent = (FeatureSetI) feature.getRefFeature();
      if (parent != null) {
        parent.adjustEdges();
      }
    }
  }

  protected void changeEnd(SeqFeatureI feature, int base_offset,
      int limit_5prime, int limit_3prime) {
    if (base_offset == 0)
      return;
    int new_end
    = feature.getEnd() + (base_offset * feature.getStrand());
    boolean okay = (reverseStrand ?
        (new_end > limit_3prime && new_end < limit_5prime) :
          (new_end < limit_3prime && new_end > limit_5prime));
    if (okay) {
      feature.setEnd(new_end);
      FeatureSetI parent = (FeatureSetI) feature.getRefFeature();
      if (parent != null) {
        parent.adjustEdges();
      }
    }
  }

  protected void createRightClickMenu() {
    rightClickMenu = new JPopupMenu();

    rightClickListener = new RightClickActionListener();
    locationLabel = new JLabel("Location");
    relativeLocationLabel = new JLabel("Relative location");

    makeIntronMenuItem = new JMenuItem("Make intron");
    createExonMenuItem = new JMenuItem("Create exon");
    deleteExonMenuItem = new JMenuItem("Delete exon");
    merge5PrimeMenuItem = new JMenuItem("Merge with 5' exon");
    merge3PrimeMenuItem = new JMenuItem("Merge with 3' exon");
    set5PrimeMenuItem = new JMenuItem("Set as 5' end");
    set3PrimeMenuItem = new JMenuItem("Set as 3' end");

    startTranslationMenuItem = new JMenuItem("Set start of translation");
    findSequenceMenuItem = new JMenuItem("Find sequence...");

    sequenceMenuItem = new JMenuItem("Sequence...");

    translationSlipMenuItem
    = new JMenuItem("Set +1 translational frame shift here");

    translationStutterMenuItem
    = new JMenuItem("Set -1 translational frame shift here");

    sequencingErrorSubMenu =
      new JMenu("Adjust for sequencing error here");
    insertionMenuItem = new JMenuItem("Insertion...");
    deletionMenuItem = new JMenuItem("Deletion");
    substitutionMenuItem = new JMenuItem("Substitution...");
    noAdjustmentMenuItem = new JMenuItem("Remove sequencing adjustment here");
    sequencingErrorSubMenu.add(insertionMenuItem);
    sequencingErrorSubMenu.add(deletionMenuItem);
    sequencingErrorSubMenu.add(substitutionMenuItem);

    makeIntronMenuItem.addActionListener(rightClickListener);
    createExonMenuItem.addActionListener(rightClickListener);
    merge5PrimeMenuItem.addActionListener(rightClickListener);
    merge3PrimeMenuItem.addActionListener(rightClickListener);
    set5PrimeMenuItem.addActionListener(rightClickListener);
    set3PrimeMenuItem.addActionListener(rightClickListener);
    deleteExonMenuItem.addActionListener(rightClickListener);
    startTranslationMenuItem.addActionListener(rightClickListener);
    translationSlipMenuItem.addActionListener(rightClickListener);
    translationStutterMenuItem.addActionListener(rightClickListener);
    insertionMenuItem.addActionListener(rightClickListener);
    deletionMenuItem.addActionListener(rightClickListener);
    substitutionMenuItem.addActionListener(rightClickListener);
    noAdjustmentMenuItem.addActionListener(rightClickListener);
    findSequenceMenuItem.addActionListener(rightClickListener);
    sequenceMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new SeqExport(getSelectedFeature(), baseFineEditor.getController());
      }
    } );

    rightClickMenu.add(locationLabel);
    rightClickMenu.add(relativeLocationLabel);
    rightClickMenu.addSeparator();
    rightClickMenu.add(sequenceMenuItem);
    rightClickMenu.add(findSequenceMenuItem);
    rightClickMenu.addSeparator();
    rightClickMenu.add(makeIntronMenuItem);
    rightClickMenu.add(createExonMenuItem);
    rightClickMenu.add(deleteExonMenuItem);
    rightClickMenu.add(merge5PrimeMenuItem);
    rightClickMenu.add(merge3PrimeMenuItem);
    rightClickMenu.add(set5PrimeMenuItem);
    rightClickMenu.add(set3PrimeMenuItem);
    rightClickMenu.add(startTranslationMenuItem);
    rightClickMenu.addSeparator();
    if (Config.getStyle().translationalFrameShiftEditingIsEnabled()) {
      rightClickMenu.add(translationSlipMenuItem);
      rightClickMenu.add(translationStutterMenuItem);
    }
    if (Config.getStyle().seqErrorEditingIsEnabled())
      rightClickMenu.add(sequencingErrorSubMenu);
  }
  
  public boolean testUser(String owner) {
    if(Config.getCheckOwnership() && owner != null && !owner.equals(UserName.getUserName()))
      return false;
    return true;
  }
  
  public void formatRightClickMenu() {
    int pos = mouseListener.getPos();
    int tier = mouseListener.getTier();
    SeqFeatureI feature = getFeatureAtPosition (pos, tier); // exon
    int dnapos = posToBasePair (pos);
    boolean goodUser = true;
    
    if (feature != null) {
      if (feature instanceof ExonI) {
        FeatureSetI fs = (FeatureSetI) feature.getRefFeature(); // transcript
        int feat_index = fs.getFeatureIndex(feature);
        int mRNA_loc = (int) fs.getFeaturePosition (dnapos);
        int exon_loc = (int) feature.getFeaturePosition (dnapos);
        relativeLocationLabel.setText("   Base " + exon_loc +
            " of exon " + (feat_index + 1) +
            " mRNA base " + mRNA_loc);
	goodUser = testUser(((AnnotatedFeatureI) fs).getOwner());
      } else {
        int trans_loc =
          (int) feature.getStart() + (dnapos * feature.getStrand());
        relativeLocationLabel.setText("   Base " + trans_loc +
        " of transcript");
        goodUser = testUser(((AnnotatedFeatureI) feature.getRefFeature()).getOwner());
      }
    } else {
      relativeLocationLabel.setText("   No feature at position");
      relativeLocationLabel.setEnabled(false);
    }

    locationLabel.setText("   Location = "+dnapos);

    sequenceMenuItem.setEnabled(selectedFeature != null);

    if (feature != null && goodUser) {
      int start = basePairToPos((int) feature.getStart());
      int end = basePairToPos((int) feature.getEnd());
      if (feature.hasKids()) {//canHaveChildren()) { // 1 level annots
        // This means that the cursor is between exons and sitting in an intron
        startTranslationMenuItem.setEnabled(false);
        translationSlipMenuItem.setEnabled(false);
        translationStutterMenuItem.setEnabled(false);
        deleteExonMenuItem.setEnabled(false);
        set5PrimeMenuItem.setEnabled(false);
        set3PrimeMenuItem.setEnabled(false);
        makeIntronMenuItem.setEnabled(false);
        merge3PrimeMenuItem.setEnabled(false);
        merge5PrimeMenuItem.setEnabled(false);

        double [] intronRange = getRangeAtPosition(tier, pos);
        // splices are always the donor basepair first
        if (pos != intronRange[0] && pos != intronRange[1]) {
          findSplices (pos,
              feature.getStrand(),
              true,
              (int) intronRange[0],
              (int) intronRange[1]);
          int donor_pos = basePairToPos(splices[0]);
          int acceptor_pos = basePairToPos(splices[1]);
          // intron range is in window coordinates which move
          // from high to low if on the reverse strand
          createExonMenuItem.setEnabled(donor_pos > intronRange[0] &&
              acceptor_pos < intronRange[1]);
        } else
          createExonMenuItem.setEnabled(false);
      } 

      // Feature doesnt have children -> The cursor is smack-dab on top of an exon
      // or a 1 level annot
      else {
        createExonMenuItem.setEnabled(false);
        boolean isExon = feature.isExon();
        startTranslationMenuItem.setEnabled(isExon);
        FeatureSetI fs = (FeatureSetI) feature.getRefFeature();
        engageTranslationSlipMenuItem(isExon, fs, dnapos);
        engageTranslationStutterMenuItem(isExon,fs, dnapos);
        if (Config.getStyle().seqErrorEditingIsEnabled())
          sequencingErrorMenuItem(fs, dnapos);
        // actually deleting is buggy for both exons & 1 level annots
        // the delete is going through but ede is not letting go of deleted feat
        // or not refreshing properly or something
        deleteExonMenuItem.setEnabled(isExon);
        set5PrimeMenuItem.setEnabled(pos != start && notTooSmall(pos, end));
        set3PrimeMenuItem.setEnabled(pos != end && notTooSmall(start, pos));
        makeIntronMenuItem.setEnabled(isExon && notTooSmall(pos + 1, end) &&
            notTooSmall(start, pos - 1));
        int index = getExonRangeIndex(tier, start, end, true);

        SeqFeatureI nextExon = getFeatureAtIndex(tier, index-1);
        merge5PrimeMenuItem.setEnabled(nextExon != null &&
            nextExon.getRefFeature() ==
              feature.getRefFeature());
        nextExon = getFeatureAtIndex(tier, index+1);
        merge3PrimeMenuItem.setEnabled(nextExon != null &&
            nextExon.getRefFeature() ==
              feature.getRefFeature());
      }
    } else {
      makeIntronMenuItem.setEnabled(false);
      startTranslationMenuItem.setEnabled(false);
      translationSlipMenuItem.setEnabled(false);
      translationStutterMenuItem.setEnabled(false);
      deleteExonMenuItem.setEnabled(false);
      set5PrimeMenuItem.setEnabled(false);
      set3PrimeMenuItem.setEnabled(false);
      merge5PrimeMenuItem.setEnabled(false);
      merge3PrimeMenuItem.setEnabled(false);
      createExonMenuItem.setEnabled(false);
      sequencingErrorSubMenu.setEnabled(false);
    }
  }

  public void displayRightClickMenu(int xLoc, int yLoc) {
    formatRightClickMenu();
    // Keep the menu from falling off the edge of the screen
    if (xLoc >= this.getSize().width/2)  // We're on the right side of the screen
      // Move the popup to the left of the cursor
      xLoc = xLoc - rightClickMenu.getPreferredSize().width;
    else
      // Move the popup to the right of the cursor, plus a small nudge
      xLoc = xLoc + 10;

    if (yLoc + rightClickMenu.getPreferredSize().height > this.getSize().height)
      yLoc = yLoc - ((yLoc + rightClickMenu.getPreferredSize().height) - this.getSize().height);
    rightClickMenu.show(this,xLoc,yLoc);
  }

  private void attachListeners() {
    mouseListener = new BaseMouseListener(this);
    addMouseListener(mouseListener);
    addMouseMotionListener(mouseListener);
  }

  public void detachTranscript (Transcript trans) {
    int trans_tier = getTierForFeature(trans);
    if (trans_tier == -1) {
      logger.error("EDE failed to find transcript to delete. This will mess up your"
          +" display. close & reopen EDE and report bug to apollo list.", new Throwable());
      return;
    }
    removeFeature (trans, trans_tier);
  }

  /** can be transcript or 1 level annot */
  public int attachAnnot(AnnotatedFeatureI annot) {
    int current_tier_count = getTierCount();
    int trans_tier = addFeature(annot);
    if (trans_tier >= current_tier_count) {
      SelectableDNARenderer renderer =
        new SelectableDNARenderer(this,
            getCharWidth(),
            getCharHeight());
      renderer.setFont(getFont());
      attachRendererToSequence(trans_tier, renderer);
    }
    return trans_tier;
  }

  /** Expects a vector of Transcripts or 1 level annots. 
      This also creates the 3 amino acid sequences. 
      This method is essential for BaseEditorPanel initialization.
   */
  public void attachTranscripts(Vector transcripts) {
    if (transcripts.size() < 1)
      return;

    SeqFeatureI transcript = (SeqFeatureI) transcripts.elementAt(0);
    refSeq = transcript.getRefSequence();
    // ensurese seq has res type
    if (refSeq.getResidueType() == null) {
      refSeq.setResidueType (SequenceI.DNA);
    }
    if (reverseStrand) {
      refSeq = new Sequence(refSeq.getName(),
          refSeq.getReverseComplement());
      refSeq.setResidueType(SequenceI.DNA);
    }

    for(int i=0; i < transcripts.size(); i++) {
      SeqFeatureI t = (SeqFeatureI) transcripts.elementAt(i);
      attachAnnot (t.getAnnotatedFeature());
    }

    int char_width = getCharWidth();
    int char_height = getCharHeight();

    String dna = refSeq.getResidues();
    String aa;

    AminoAcidRenderer aar = new AminoAcidRenderer(char_width,
        char_height,
        0,
        this);
    aar.setFontSizes(getFont().getSize());
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_ONE,
        DNAUtils.ONE_LETTER_CODE);
    frame[0] = new Sequence (refSeq.getName(), aa);
    frame[0].setResidueType (SequenceI.AA);
    addSequence (frame[0], 0, SequenceI.AA, 0);
    attachRendererToSequence(0,aar);

    aar = new AminoAcidRenderer(char_width,
        char_height,
        1,
        this);
    aar.setFontSizes(getFont().getSize());
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_TWO,
        DNAUtils.ONE_LETTER_CODE);
    frame[1] = new Sequence (refSeq.getName(), aa);
    frame[1].setResidueType (SequenceI.AA);
    addSequence (frame[1], 1, SequenceI.AA, 1);
    attachRendererToSequence(1,aar);

    aar = new AminoAcidRenderer(char_width,
        char_height,
        2,
        this);
    aar.setFontSizes(getFont().getSize());
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_THREE,
        DNAUtils.ONE_LETTER_CODE);
    frame[2] = new Sequence (refSeq.getName(), aa);
    frame[2].setResidueType (SequenceI.AA);
    addSequence (frame[2], 2, SequenceI.AA, 2);
    attachRendererToSequence(2,aar);

    reformat();
  }

  public Color getSeqSelectColor() {
    return seqSelectColor;
  }

  public Color getSelectionBoxColor() {
    return selectionBoxColor;
  }

  protected boolean isSequencingErrorPosition(int dnapos) {
    return curationState.getCurationSet().getRefSequence().isSequencingErrorPosition(dnapos);
  }

  private void engageTranslationSlipMenuItem(boolean isExon, FeatureSetI fs, int dnapos) {
    boolean enable = false;
    if (isExon && fs.withinCDS(dnapos)) {
      if (!isSequencingErrorPosition(dnapos) &&
          fs.minus1FrameShiftPosition() != dnapos) {
        enable = true;
        String text = (dnapos == fs.plus1FrameShiftPosition() ?
            "Remove +1 translational frame shift here" :
        "Set +1 translational frame shift here");
        translationSlipMenuItem.setText(text);
      }
    }
    translationSlipMenuItem.setEnabled(enable);
  }

  private void engageTranslationStutterMenuItem(boolean isExon, FeatureSetI fs,
      int dnapos) {
    boolean enable = false;
    if (isExon && fs.withinCDS(dnapos)) {
      if (!isSequencingErrorPosition(dnapos) &&
          fs.plus1FrameShiftPosition() != dnapos) {
        enable = true;
        String text = (dnapos == fs.minus1FrameShiftPosition() ?
            "Remove -1 translational frame shift here" :
        "Set -1 translational frame shift here");
        translationStutterMenuItem.setText(text);
      }
    }
    translationStutterMenuItem.setEnabled(enable);
  }

  private void sequencingErrorMenuItem(FeatureSetI fs, int dnapos) {
    if (fs.plus1FrameShiftPosition() != dnapos &&
        fs.minus1FrameShiftPosition() != dnapos) {
      SequenceEdit seq_edit = fs.getSequencingErrorAtPosition(dnapos);
      if (seq_edit != null) {
        rightClickMenu.remove(sequencingErrorSubMenu);
        noAdjustmentMenuItem.setText("Remove " + seq_edit.getEditType() +
        " here");
        rightClickMenu.add(noAdjustmentMenuItem);
      }
      else {
        rightClickMenu.remove(noAdjustmentMenuItem);
        rightClickMenu.add(sequencingErrorSubMenu);
      }
    } else {
      rightClickMenu.remove(sequencingErrorSubMenu);
      rightClickMenu.remove(noAdjustmentMenuItem);
    }
  }

  private String getBaseFromUser(String msg, Object [] bases) {
    JOptionPane pane = new JOptionPane(msg,
        JOptionPane.QUESTION_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION,
        null,
        bases,
        bases[0]);
    JDialog dialog = pane.createDialog(baseFineEditor, "Please select a base");
    dialog.setBackground(Config.getAnnotationBackground());
    dialog.show();
    Object result = pane.getValue();
    String base = null;
    if (result != null) {
      for (int i = 0; i < bases.length && base == null; i++)
        base = (bases[i].equals(result) ? (String) result : null);
      if (base.equals("Cancel"))
        base = null;
    }
    return base;
  }

  /** This has to change - this method was changing the model - this should
      be done by AnnotationEditor to be consistent, and needed there for
      fully described AnnotChangeEvent 
      Deletions of seq edit come in here with operation CLEAR_EDIT */
  private void dealWithSequenceEdit(SeqFeatureI feature, 
      int basepair,
      String operation,
      String base) {
    /* get the list of transcripts that overlap this position */
    CurationSet curation = getCurationSet();
    SequenceI seq = curation.getRefSequence();
    //Vector annots = new Vector();
    // populates annots vector with leaf
    FeatureList annots = curation.getAnnots().getLeafFeatsOver(basepair);
    // for some odd reason non-exons are being returned, which shouldn't be...
    // clean them out from our exon list
    FeatureList exons = new FeatureList();
    for (int i = 0; i < annots.size(); ++i) {
      SeqFeatureI feat = annots.getFeature(i);
      if (feat.isExon()) {
        exons.add(feat);
      }
    }
    int exon_count = exons.size() - 1;
    boolean approved = true;
    for (int i = exon_count; i >= 0 && approved; i--) {
      /* check the exon with currently selected to see if they are 
         the same, if not then issue a warning and double check
         with the user that they want to make this change */
      SeqFeatureI exon = exons.getFeature(i);
      if (exon != feature) {
        /* find out now */
        approved = getOkayFromUser(operation, exon);
      }
    }
    if (approved) {
      // this needs to happen in AnnotationEditor! changes to the model
      /// should happen there not here.
      // i think operation, basepair, and base should be bundled in an object 
      // this will be handy for undo
//    boolean added = seq.addSequencingErrorPosition(operation,
//    basepair,
//    base);
//    if (added) {
//    exon_count = exons.size();
//    for (int i = 0; i < exon_count; i++) {
//    SeqFeatureI exon = (SeqFeatureI) exons.elementAt(i);
//    annotationEditor.setSelections(null, null, null, null);
//    annotationEditor.setSequencingErrorPosition((ExonI)exon);
//    }
//    }
      annotationEditor.setSequencingErrorPositions(seq,operation,basepair,base,exons);
    }
  }

  private static Object [] opts = {"OK", "Cancel"};

  private boolean getOkayFromUser(String operation, SeqFeatureI sf) {
    String msg = ((operation.equals(SequenceI.CLEAR_EDIT) ?
        "This " : "This " + operation + " ") +
        "will affect the transcript of " +
        sf.getRefFeature().getName() +
    ". Are you sure you want to do this?");
    JOptionPane pane = new JOptionPane(msg,
        JOptionPane.QUESTION_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION,
        null,
        opts,
        opts[1]);
    JDialog dialog = pane.createDialog(baseFineEditor, "Please confirm");
    dialog.show();
    Object result = pane.getValue();
    boolean okay = (result != null && opts[0].equals(result) ? true : false);
    return okay;
  }

  private CurationSet getCurationSet() { return curationState.getCurationSet(); }
  GuiCurationState getCurationState() { return curationState; }

  private class BaseEditorSelectionListener implements FeatureSelectionListener {
    public boolean handleFeatureSelectionEvent(FeatureSelectionEvent e) {
      if (!baseFineEditor.canHandleSelection(e,BaseEditorPanel.this))
        return false;
      // canHandleSel ensures itsa AnnotatedFeatureI
      AnnotatedFeatureI g = (AnnotatedFeatureI)e.getFeatures().getFeature(0);
      displayAnnot(g); 
      return true;
    }
  }

}
