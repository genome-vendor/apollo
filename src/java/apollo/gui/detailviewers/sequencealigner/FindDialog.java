package apollo.gui.detailviewers.sequencealigner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bdgp.util.DNAUtils;

import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.gui.BaseScrollable;
import apollo.gui.SequenceSelector;

public class FindDialog extends JDialog {
  JLabel seqLabel;
  JTextField seqField;
  JRadioButton isAminoAcidButton;
  JRadioButton isGenomicButton;
  JTable posTable;
  JButton searchButton;
  Vector colNames;

  
  /** What type of sequence is being displayed */
  public boolean reverseStrand;
  
  private SequenceI refSeq;
  protected SequenceI [] frame = {null, null, null};
  protected Vector translatedHitZones;
  protected Vector hitZones = new Vector();
  protected boolean showHitZones = false;
  
  MultiTierPanel transformer;
  
  /** Object that will be scrolled */
  BaseScrollable scrollableObject;
  
  
  public FindDialog(BaseScrollable scrollableObject, MultiTierPanel transformer,
        SequenceI refSeq, boolean reverseStrand) {
    seqLabel = new JLabel("Sequence");
    seqField = new JTextField();
    isAminoAcidButton = new JRadioButton("is amino acid sequence");
    isGenomicButton = new JRadioButton("is genomic sequence");
    colNames = new Vector();
    colNames.addElement("Position");
    colNames.addElement("Query_frame"); // rename from phase to Query_frame
    posTable = new JTable(new Vector(), colNames);
    searchButton = new JButton("Search");
    translatedHitZones = new Vector(3);
    translatedHitZones.addElement(null);
    translatedHitZones.addElement(null);
    translatedHitZones.addElement(null);
    this.scrollableObject = scrollableObject;
    this.transformer = transformer;
    this.refSeq = refSeq;
    this.reverseStrand = reverseStrand;
    
    String dna = refSeq.getResidues();
    String aa;
    
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_ONE,
        DNAUtils.ONE_LETTER_CODE);
    frame[0] = new Sequence (refSeq.getName(), aa);
    frame[0].setResidueType (SequenceI.AA);
    
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_TWO,
        DNAUtils.ONE_LETTER_CODE);
    frame[1] = new Sequence (refSeq.getName(), aa);
    
    aa = DNAUtils.translate (dna,
        DNAUtils.FRAME_THREE,
        DNAUtils.ONE_LETTER_CODE);
    frame[2] = new Sequence (refSeq.getName(), aa);
    frame[2].setResidueType (SequenceI.AA);
    
    
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
        if (reverseStrand)
          return coord[1];
        else
          return coord[0];
      } else {
        return 1;
      }
    }
  }
  
  public void setTranslatedHitZones(Vector hitZones, int frame) {
    translatedHitZones.setElementAt(hitZones, frame);
  }
  
  public void setHitZones(Vector hitZones) {
    this.hitZones = hitZones;
  }

  public void setShowHitZones(boolean showHitZones) {
    this.showHitZones = showHitZones;
    repaint();
  }
  
  public boolean getShowHitZones() {
    return showHitZones;
  }
  
  public void doSearch() {
    String seqText = seqField.getText();
    if (seqText != null) 
      seqText = seqText.trim();
    
    if (seqText.length() < 1)
      return;

    SequenceSelector seqSelector;
    SeqFeatureI seqFeature;
    FindTableModel model = new FindTableModel();

    if (isAminoAcidButton.isSelected()) {
      for(int j=0; j < 3; j++) {
        SequenceI aa_seq = frame[j]; // need to setup frame...
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
            found_pos[1] = transformer.tierPositionToBasePair(start);//posToBasePair(start);
            found_pos[0] = transformer.tierPositionToBasePair(end); //posToBasePair(end);
          } else {
            found_pos[0] = transformer.tierPositionToBasePair(start);//posToBasePair(start);
            found_pos[1] = transformer.tierPositionToBasePair(end);//posToBasePair(end);
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
          found_pos[1] = transformer.tierPositionToBasePair(zone[0]);//posToBasePair(zone[0]);
          found_pos[0] = transformer.tierPositionToBasePair(zone[1]);//posToBasePair(zone[1]);
        } else {
          found_pos[0] = transformer.tierPositionToBasePair(zone[0]);//posToBasePair(zone[0]);
          found_pos[1] = transformer.tierPositionToBasePair(zone[1]);//posToBasePair(zone[1]);
        }
        model.add(found_pos, -1);
      }
    }
    posTable.setModel(model);
    // BaseFineEditor baseFineEditor = (BaseFineEditor) SwingUtilities.
    // getAncestorOfClass(JFrame.class, BaseEditorPanel.this);
    //baseFineEditor.clearFindsButton.setEnabled(true); // enable button that stops find color changes...
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
            FindTableModel ftm = (FindTableModel) posTable.getModel();
            scrollableObject.scrollToBase(ftm.getBaseForRow(row));
          }
        }
      }
    }
    );
  }
} // END: FindDialog
