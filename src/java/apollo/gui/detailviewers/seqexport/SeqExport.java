package apollo.gui.detailviewers.seqexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import apollo.util.FeatureIterator;
import apollo.util.FeatureList;
import apollo.util.NumericKeyFilter;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Transcript;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.TranslationI;

import apollo.seq.io.FastaFile;
import apollo.config.Config;
import apollo.gui.ControlledObjectI;
import apollo.gui.Controller;
import apollo.gui.Selection;
import apollo.gui.event.FeatureSelectionEvent;
import apollo.gui.event.FeatureSelectionListener;
import apollo.editor.FeatureChangeEvent;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.dataadapter.DataLoadListener;
import apollo.dataadapter.DataLoadEvent;

/**
 * Displays sequence that can be translated and/or written to a file.
 * Implements ControlledObjectI and EventListener so it can be added to Controller,
 * the only purpose being to show up in Windows menu, not actually listening for
 * any events from Controller.
 *
 * 3/11/2004:  Code for adding text box to allow user to adjust range for
 * corresponding genomic sequence was contributed by Sylvain Foret
 * <foret@rsbs.anu.edu.au> */

public class SeqExport extends JFrame
  implements DataLoadListener {
  private static int panelWidth = 440;
  private final static Dimension windowSize = new Dimension(panelWidth,580);
  // Needs to have a fixed width font
  // This font looks bad in JDK1.4, though.  Maybe make it bigger?
  private static final Font seqFont = new Font("Courier", 0, 12);

  //private Vector features;
  private FeatureList features;
  private JScrollPane seqScrollPane;
  /** text area displaying sequence */
  private JTextArea seqTextArea;
  //private ButtonGroup radioButtonGroup;
  private JRadioButton currentlySelectedButton;
  private JRadioButton resultSequenceButton;
  private JRadioButton cdsButton;
  private JRadioButton aminoAcidsButton;
  private JRadioButton cdnaButton;
  private JRadioButton genomicButton;
  //  private int genomicPlusMinus = 500;
  private JRadioButton genomicPlusMinusButton;
  private JTextField genomicPlusMinusField;
  private static final int defaultGenomicPlusMinus = 500;
  private int genomicPlusMinus = defaultGenomicPlusMinus;

  protected static Color bgColor = new Color (255,255,204);
  private JCheckBox followSelectionCheckBox;
  private Selection selection;
  private Controller controller;
  private int offset = 0;

  public SeqExport(Selection selection,Controller controller) {
    this(selection, controller, 0);
  }

  /** SeqExport post-processes the selection with
      selection.getConsolidatedFeatures */
  public SeqExport(Selection selection, Controller controller, int offset) {
    this.selection = selection;
    this.controller = controller;
    // Offset is for horizontally offsetting new sequence windows so they're
    // not all smack on top of each other.
    this.offset = offset;
    // If all exons in transcript present, it replaces exons w/ trans
    // so user sees transcript seq not all the exons seq
    FeatureList consolidatedFeatures = selection.getConsolidatedFeatures();
    init(consolidatedFeatures,controller);
  }

  public SeqExport(/*RangeI*/SeqFeatureI feat, Controller controller) {
    //Vector v = new Vector(1);
    FeatureList f = new FeatureList(1);
    f.addFeature(feat); //  Only a single feature - no need to consolidate
    init(f,controller);
  }

  private void init(FeatureList features, Controller controller) {
    initGui();
    setLocation(offset, 0);
    processFeatures(features);
    // inner class that deals with selection and window control
    new SeqExportFeatureListener(controller);
    setVisible(true);
  }

  /** go through all_features feat list and add features to feature list,
   if gene is found, add its transcripts not the gene.
   if a feature has result sequence enable result button */
  private void processFeatures(FeatureList all_features) {
    resultSequenceButton.setEnabled(false); // disable before enabling below
    cdsButton.setEnabled(false);
    aminoAcidsButton.setEnabled(false);
    cdnaButton.setEnabled(false);

    this.features = new FeatureList();

    for (int i=0; i < all_features.size(); i++) {
      SeqFeatureI feat = all_features.getFeature(i);

      if (feat.isAnnotTop() && feat.hasKids()) {
        Vector transcripts = feat.getFeatures();
        features.addAll(transcripts);
      }

//       //SeqFeatureI topAnnot = null;
//       // TRANSCRIPT
//       if (feat.isTranscript()) {
//         //topAnnot = feat.getRefFeature();
//         features.addFeature(feat);
//       }
//       // EXON
//       else if (feat.isExon()) {
//         features.addFeature(feat);
//         //topAnnot = feat.getParent().getParent();
//       }
//       // TOP LEVEL ANNOT
//       else if (feat.isAnnot()) {
//         // 3 LEVEL ANNOT - has kids,  > 1 level (e.g. gene)
//         if (feat.hasKids()) {  //canHaveChildren()
//           Vector transcripts = feat.getFeatures();
//           features.addAll(transcripts);
//         }
//         // 1 LEVEL ANNOT - NO KIDS
//         else {
//           features.addFeature(feat);
//         }
//         topAnnot = feat;
//       }

      else {
        /* whatever it is, add it and deal with it (as long as it
           has some residues. This is to keep 0-residue start/stop
           codons from showing up) */
        if (feat.getResidues().length() > 0)
          features.addFeature(feat);
      }

      // cds and aa buttons
      //  && feat.hasAnnotatedFeature()? isProt should cover and also its
      // possible for results to have prots/translation start&stop
      if (!cdsButton.isEnabled() && feat.isProteinCodingGene()) {
        // Don't enable unless it's really a gene (not, e.g., a pseudogene)
        //if (topAnnot.isProteinCodingGene()) {
        cdsButton.setEnabled(true);
        aminoAcidsButton.setEnabled(true);
          // Always select the amino acid button when we switch
          // to an annotation
          // 7/2004: why??
          //	  currentlySelectedButton = aminoAcidsButton;
        //}
      }
      if (!cdsButton.isEnabled()) {
        TranslationI translation = null;
        if (feat.hasKids() && feat.hasTranslation()) {
        //FeatureSetI fset = (FeatureSetI)feat;
          translation = feat.getTranslation();
        }
        else if (feat.getParent() != null && feat.getParent().hasTranslation()) {
          translation = feat.getParent().getTranslation();
        }
        //if (fset.getTranslationStart() != 0 && fset.getTranslationEnd() != 0) {
        if (translation != null && translation.hasTranslationStart() && translation.hasTranslationEnd()) {
          cdsButton.setEnabled(true);
          aminoAcidsButton.setEnabled(true);
        }
      }
//       else if (!cdsButton.isEnabled() && feat.getParent() != null) {
//         //FeatureSetI fset = ((SeqFeatureI) feat).getParent();
//         if (fset.getTranslationStart() != 0 && fset.getTranslationEnd() != 0) {
//           cdsButton.setEnabled(true);
//           aminoAcidsButton.setEnabled(true); }  }

      if (!cdnaButton.isEnabled() && !isOneLevelAnnot(feat)) {
        cdnaButton.setEnabled(true);
      }

      // Result sequence button
      if (!resultSequenceButton.isEnabled() && hasResultSequence(feat))
        resultSequenceButton.setEnabled(true);
    }

    currentlySelectedButton.setSelected(true);

    // If a button that's been disabled is selected,
    // select result sequence button instead
    if (isSelectedAndDisabled(cdsButton)
        || isSelectedAndDisabled(aminoAcidsButton)) {
      currentlySelectedButton = resultSequenceButton;
      currentlySelectedButton.setSelected(true);
    }
    if (isSelectedAndDisabled(resultSequenceButton)) {
      currentlySelectedButton = genomicButton;
    }
    displaySequences();
  }

  private boolean isOneLevelAnnot(SeqFeatureI feat) {
    return feat.isAnnot() && feat.isAnnotTop() && !feat.hasKids();
  }

  private boolean isSelectedAndDisabled(JRadioButton but) {
    return but.isSelected() && ! but.isEnabled();
  }

  /** String that appears in window menus list */
  public String getTitle() {
    if (features.size() == 1) {
      SeqFeatureI feature = features.getFeature(0);
      return Config.getDisplayPrefs().getHeader(feature);
    } else
      return features.size()+" sequences";
  }

  /** set up gui */
  private void initGui() {
    getContentPane().setBackground(bgColor);
    seqTextArea = new JTextArea();
    seqTextArea.setFont(seqFont);
    seqScrollPane = new JScrollPane(seqTextArea);
    seqScrollPane.setMinimumSize(new Dimension(panelWidth, 300));
    seqScrollPane.setPreferredSize(new Dimension(panelWidth, 300));
    // fiddling to try to get scrollbar to top - left in for revisit
    //seqScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    //seqScrollPane.setVerticalScrollBar(new MyScrollBar());
    //seqScrollPane.getViewport().setViewPosition(new java.awt.Point(0,0));
    //MyViewport vp = new MyViewport();
    //vp.setView(seqTextArea);
    //seqScrollPane.setViewport(vp);

    aminoAcidsButton = new JRadioButton("Peptide sequence");
    aminoAcidsButton.setBackground(bgColor);
    aminoAcidsButton.addActionListener(new ActionListener() {
                                         public void actionPerformed(ActionEvent e) {
                                           displayAASequence();
                                           currentlySelectedButton = aminoAcidsButton;
                                         }
                                       }
                                      );
    aminoAcidsButton.setSelected(true); // default
    currentlySelectedButton = aminoAcidsButton;

    cdnaButton = new JRadioButton("cDNA sequence");
    cdnaButton.setBackground(bgColor);
    cdnaButton.addActionListener(new ActionListener() {
                             public void actionPerformed(ActionEvent e) {
                               displayCDNASequence();
                               currentlySelectedButton = cdnaButton;
                             }
                           }
                          );

    cdsButton = new JRadioButton("CDS sequence");
    cdsButton.setBackground(bgColor);
    cdsButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          displayCodingSequence();
          currentlySelectedButton = cdsButton;
        }
      }
                                );
    genomicButton = new JRadioButton("Corresponding genomic sequence");
    genomicButton.setBackground(bgColor);
    genomicButton.addActionListener(new ActionListener() {
                                      public void actionPerformed(ActionEvent e) {
                                        displayGenomicSequence();
                                        currentlySelectedButton = genomicButton;
                                      }
                                    }
                                   );

    genomicPlusMinusButton = new JRadioButton("Corresponding genomic sequence +/-");
    genomicPlusMinusButton.setBackground(bgColor);
    GenomicPlusMinusListener plusMinusListener = new GenomicPlusMinusListener();
    genomicPlusMinusButton.addActionListener(plusMinusListener);

    resultSequenceButton = new JRadioButton("Result sequence");
    resultSequenceButton.setBackground(bgColor);
    resultSequenceButton.addActionListener(new ActionListener() {
                                             public void actionPerformed(ActionEvent e) {
                                               displayResultSequence();
                                               currentlySelectedButton = resultSequenceButton;
                                             }
                                           }
                                          );

    //ButtonGroup
    ButtonGroup radioButtonGroup = new ButtonGroup();
    radioButtonGroup.add(aminoAcidsButton);
    radioButtonGroup.add(cdnaButton);
    radioButtonGroup.add(cdsButton);
    radioButtonGroup.add(genomicButton);
    radioButtonGroup.add(genomicPlusMinusButton);
    radioButtonGroup.add(resultSequenceButton);

    // Adds the possibility to choose the genomic range to export
    genomicPlusMinusField = new JTextField(Integer.toString(genomicPlusMinus), 12);
    // trial & error ; had to do this, otherwise looks uggly when resizing
    Dimension fieldDim = new Dimension(90, 20);
    genomicPlusMinusField.setMinimumSize(fieldDim);
    genomicPlusMinusField.setMaximumSize(fieldDim);
    genomicPlusMinusField.addKeyListener(NumericKeyFilter.getFilter());
    genomicPlusMinusField.addActionListener(plusMinusListener);

    JLabel bases = new JLabel("bases");

    Box genomicPlusMinusBox = new Box(BoxLayout.X_AXIS);
    genomicPlusMinusBox.add(genomicPlusMinusButton);
    genomicPlusMinusBox.add(genomicPlusMinusField);
    genomicPlusMinusBox.add(bases);

    Box seqButtonsBox = new Box(BoxLayout.Y_AXIS);
    seqButtonsBox.add(aminoAcidsButton);
    aminoAcidsButton.setAlignmentX(0.0f); // left aligns the radio buttons
    seqButtonsBox.add(cdnaButton);
    cdnaButton.setAlignmentX(0.0f);
    seqButtonsBox.add(cdsButton);
    cdsButton.setAlignmentX(0.0f);
    genomicButton.setAlignmentX(0.0f);
    seqButtonsBox.add(genomicButton);
    genomicPlusMinusBox.setAlignmentX(0.0f);
    seqButtonsBox.add(genomicPlusMinusBox);
    seqButtonsBox.add(resultSequenceButton);
    resultSequenceButton.setAlignmentX(0.0f);

    Box seqButtonsHorizontalBox = new Box(BoxLayout.X_AXIS);
    seqButtonsHorizontalBox.add(Box.createHorizontalGlue());
    seqButtonsHorizontalBox.add(seqButtonsBox);
    // force it further left
    seqButtonsHorizontalBox.add(Box.createHorizontalStrut(150));

    JButton save = new JButton("Save as...");
    save.setBackground(Color.white);
    save.addActionListener(new SaveListener());

    JButton close = new JButton("Close");
    close.setBackground(Color.white);
    close.addActionListener(new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                dispose();
                              }
                            }
                           );

    JButton newSeqWindow = new JButton("New sequence window...");
    newSeqWindow.setBackground(Color.white);
    newSeqWindow.addActionListener(new ActionListener() {
                              public void actionPerformed(ActionEvent e) {
                                new SeqExport(selection, controller, offset+20);
                              }
                            }
                           );

    Box saveCloseBox = new Box(BoxLayout.X_AXIS);
    saveCloseBox.add(Box.createHorizontalGlue());
    saveCloseBox.add(save);
    saveCloseBox.add(Box.createHorizontalStrut(10));
    saveCloseBox.add(close);
    saveCloseBox.add(Box.createHorizontalStrut(10));
    saveCloseBox.add(newSeqWindow);
    saveCloseBox.add(Box.createHorizontalGlue());

    followSelectionCheckBox = new JCheckBox("Follow external selection",false);
    followSelectionCheckBox.setBackground(bgColor);

    Box followHorizontalBox = new Box(BoxLayout.X_AXIS);
    followHorizontalBox.add(Box.createHorizontalGlue());
    followHorizontalBox.add(followSelectionCheckBox);
    // force it further left
    followHorizontalBox.add(Box.createHorizontalStrut(206)); // trial & error

    Container content = getContentPane();
    content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
    content.add(seqScrollPane);
    content.add(Box.createVerticalStrut(10));
    //    content.add(seqButtonsBox);
    content.add(seqButtonsHorizontalBox);
    content.add(Box.createVerticalStrut(10));
    content.add(saveCloseBox);
    content.add(Box.createVerticalStrut(15));
    //    content.add(followSelectionCheckBox);
    content.add(followHorizontalBox);
    content.add(Box.createVerticalStrut(10));

    setSize(windowSize);
  }

  private void displaySequences() {
    currentlySelectedButton.doClick();
    setTitle(getTitle());
  }

  private void displayGenomicSequence() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat = features.getFeature(i);
      fastas += featureFastaString(feat.getResidues(),feat,"genomic");
    }
    seqTextArea.setText(fastas);
    // Scroll text pane to top (by default, comes up scrolled to bottom)
    seqTextArea.setCaretPosition(0);
  }

  private void displayGenomicPlusMinus() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat = features.getFeature(i);
      SequenceI refSeq = feat.getRefSequence();
      if (feat == null || refSeq == null)
        continue;
      int minus = genomicPlusMinus*feat.getStrand(); // hard wired for now
      int plus = genomicPlusMinus*feat.getStrand();
      // should this be a method in Range?
      // rev strand?
      String seq = refSeq.getResidues(feat.getStart()-minus,feat.getEnd()+plus);
      fastas += featureFastaString(seq,feat,"genomic +/-"+genomicPlusMinus + " bases");
    }
    seqTextArea.setText(fastas);
    seqTextArea.setCaretPosition(0);
  }

  private void displayAASequence() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat = features.getFeature(i);
      String peptideSeq;
      if (feat.isTranscript()) {
        // Don't force Transcripts to retranslate.
        // getPeptideSequence will translate if necessary (e.g. if
        // peptide sequence is null)
        peptideSeq = ((Transcript)feat).getPeptideSequence().getResidues();
      }
      else {
        peptideSeq = feat.translate();
      }
      fastas += featureFastaString(peptideSeq,feat,"amino acid");
    }
    seqTextArea.setText(fastas);
    seqTextArea.setCaretPosition(0);
  }

  private void displayCDNASequence() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat = features.getFeature(i);
      fastas += featureFastaString(feat.get_cDNA(),feat,"cDNA");
    }
    seqTextArea.setText(fastas);
    seqTextArea.setCaretPosition(0);
  }

  private void displayCodingSequence() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat =  features.getFeature(i);
      fastas += featureFastaString(feat.getCodingDNA(),feat,"coding");
    }
    seqTextArea.setText(fastas);
    seqTextArea.setCaretPosition(0);
  }

  private void displayResultSequence() {
    String fastas = "";
    int feat_count = features.size();
    for (int i = 0; i < feat_count; i++) {
      SeqFeatureI feat =  features.getFeature(i);
      String seq = getResultSequence(feat);
      if (seq != null)
        fastas += featureFastaString(seq,feat,"result");
    }
    seqTextArea.setText(fastas);
    seqTextArea.setCaretPosition(0);
  }

  /** Returns true if has non null result seq. Recurses on features sets. */
  private boolean hasResultSequence(SeqFeatureI feat) {
    if (feat instanceof FeaturePairI) {
      FeaturePairI fpair = (FeaturePairI)feat;
      String seq = fpair.getHitFeature().getFeatureSequence().getResidues();
      if (seq != null && !(seq.equals("")))
        return true;
    } else if (feat.canHaveChildren()) {
      Vector kids = feat.getFeatures();
      for (int i=0; i<kids.size(); i++) {
        // if any of the descendants has result sequence return true
        if (hasResultSequence((SeqFeatureI)kids.elementAt(i)))
          return true;
      }
    }
    return false;
  }

  /** Returns null if no sequence. Only returns sequence if feat or descendants
   of feat are FeaturePairIs with non null sequences. */
  private String getResultSequence(SeqFeatureI feat) {
    String seq = "";
    if (feat instanceof FeaturePairI) {
      FeaturePairI fpair = (FeaturePairI)feat;
      return fpair.getHitFeature().getFeatureSequence().getResidues();
    } else if (feat.canHaveChildren()) {
      Vector kids = feat.getFeatures();
      for (int i=0; i<kids.size(); i++) {
        // if any of the descendants has result sequence return true
        String s = getResultSequence((SeqFeatureI)kids.elementAt(i));
        if (s != null)
          seq += s;
      }
    }
    if (seq.equals(""))
      return null;
    return seq;
  }

  private String featureFastaString(String seq, RangeI feat, String type) {
    String header = (">"+ Config.getDisplayPrefs().getHeader(feat) +
                     " (" + type +" sequence): "
                     + seq.length() + " residues");
    // We'd like to add the chrom name and range to the header line,
    // but this isn't right (we'll need to consider what display mode
    // we're in--result sequence, genomic, etc.).
    // Need to work on this for next release.
//     if (feat.getRefSequence() != null) {
//       header = header + " (";
//       SequenceI refseq = feat.getRefSequence();
//       if (refseq.getRange() != null)
//         //         header = header + refseq.getRange().getChromosome() + ":";
//         header = header + refseq.getOrganism() + ":";
//     }
//
//     header = header + feat.getStart() + "-" + feat.getEnd() + ")";
    header = header + "\n";
    return FastaFile.format(header,seq,50);
  }

  /** Listens for changed in the genomic plus minus range
   * used if the radio button is clicked and when the an action
   * is performed on the text field */
  private class GenomicPlusMinusListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String txt = genomicPlusMinusField.getText();
      try {
        genomicPlusMinus = Integer.parseInt(txt);
      } catch (NumberFormatException nfe)  { // Mostly in case of overflow
        genomicPlusMinus = defaultGenomicPlusMinus;
        genomicPlusMinusField.setText(Integer.toString(defaultGenomicPlusMinus));
        String msg = "Could not convert " + txt + "\nto an integer, using " + defaultGenomicPlusMinus + " instead";
        JOptionPane.showMessageDialog(SeqExport.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
      }
      if ( currentlySelectedButton != genomicPlusMinusButton ) {
        currentlySelectedButton = genomicPlusMinusButton;
        genomicPlusMinusButton.setSelected(true);
      }
      displayGenomicPlusMinus();
    }
  }

  /** Listens to save button, brings up save file chooser, and writes file */
  private class SaveListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      JFileChooser chooser = new JFileChooser();
      int returnVal = chooser.showSaveDialog(null);
      if(returnVal == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        if (file==null) { // file might be null
          // is this message overkill - should it just exit?
          JOptionPane.showMessageDialog(null,"No file selected");
          return;
        }
        try {
          BufferedWriter writer = new BufferedWriter(new FileWriter(file));
          writer.write(seqTextArea.getText());
          writer.close();
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(null,"Couldn't write file to "+file);
        }
      }
    }
  }

  /** Deals with selection listening and being a controlled object to get in
   * controllers window list
   * implements ControlledObjectI so it can be added to Controller,
   * so it show up in Windows menu
   */
  private class SeqExportFeatureListener implements
    FeatureSelectionListener,
    AnnotationChangeListener,
    ControlledObjectI {

    private Controller controller;

    private SeqExportFeatureListener(Controller controller) {
      setController(controller);
      controller.addListener(this); // adds this inner class as listener
    }
    /**
     * Sets the Controller for the object (ControlledObjectI)
     */
    public void setController(Controller controller) {
      this.controller = controller;
      // this is to get the window to show up in the windows menu
    }
    /**
     * Gets the Controller for the object(ControlledObjectI)
     */
    public Controller getController() {
      return controller;
    }
    public Object     getControllerWindow() {
      return SeqExport.this;
    }
    /** Whether controller should remove as listener on window closing */
    public boolean    needsAutoRemoval() {
      return true;
    }

    /** FeatureSelectionListener - handle selection. Only deals with selection
    if this is the most recent instance */
    public  boolean handleFeatureSelectionEvent(FeatureSelectionEvent e) {
      // if followSelectionCheckBox not checked dont handle
      if  (!followSelectionCheckBox.isSelected() && !e.forceSelection())
        return false;

      // if all exons of trans present, deletes exons adds trans
      FeatureList consolidatedFeatures = e.getSelection().getConsolidatedFeatures();
      processFeatures(consolidatedFeatures);
      return true;
    }

    public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
      if (evt.isEndOfEditSession()) {
        /* Perhaps it would be better to check the vector and only
           process if the changed features is something that is visible,
           but this is much simpler.
           What really is wrong here is that deletes and additions to
           the feature is not really dealt with and that should be
           fixed */
        processFeatures(features);
      } else if (evt.isDelete()) {
        SeqFeatureI gone = evt.getChangedFeature();
        boolean handled = false;
        for (int i = features.size() - 1; i >= 0 && !handled; i--) {
          handled = features.remove(gone);
        }
      }
      return true;
    }
  }

  /** Region changing means a new data set is being loaded. Get rid of this
      window.
      (Not working yet--doesn't seem to get called when region changes.  Probably
      need to add window listener when new SeqWindow is created.)
      Do we want this? I think sima likes that the seq window lingers. */
  public boolean handleDataLoadEvent(DataLoadEvent e) {
    //    System.out.println("SeqWindow: handling region change event"); // DEL
    this.hide();
    this.dispose();
    controller.removeListener(this);
    // Need to set anything to null?
    return true;
  }
}
