package apollo.gui.annotinfo;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.lang.Integer;

import apollo.datamodel.*;
import apollo.editor.Transaction;
import apollo.editor.TransactionSubpart;
import apollo.config.Config;
import apollo.gui.event.*;
import apollo.config.FeatureProperty;
import apollo.util.GuiUtil;
import apollo.util.NumericKeyFilter;
import apollo.config.ApolloNameAdapterI;
import apollo.gui.drawable.DrawableUtil;

import org.apache.log4j.*;

/**
 * A JPanel to display Transcript info. Used by FeatureEditorDialog.
 * Fields disabled if read only.
 * No CommentEditPanel if read only (transcript comments show up in 
 * FeatureEditPanel's panel)
 
 Currently its featureList(in superclass FeatureEditPanel) now expects real
 transcripts. Changed to take real transcripts to be 
 consistent with the gene featureList.
 */

class TranscriptEditPanel extends FeatureEditPanel {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TranscriptEditPanel.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private JLabel           ownerLabel              = new JLabel();
  private JCheckBox        finishedCheckbox        = new JCheckBox();
  private JLabel nonConsensusSplice;
  private JCheckBox nonConsensusSpliceOkay = new JCheckBox();
  private JLabel missing5prime = new JLabel();
  private JLabel missing3prime = new JLabel();
  private JLabel startCodon     = new JLabel();
  private JCheckBox replaceStop = new JCheckBox();
  private JLabel slippagePosition = new JLabel();
  private JLabel stutterPosition = new JLabel();
  private boolean editingName = false; // whether in process of editing name

  TranscriptEditPanel(FeatureEditorDialog featureEditorDialog,
                      boolean isReadOnly) {
    super (featureEditorDialog, isReadOnly);
    //    bgColor = new Color(176, 236, 248);  // turquoise--can't see some of the curator colors on it
    //    bgColor = new Color(255,255,204); // pale yellow
    bgColor = new Color (210,240,255); // light blue
    jbInit();
  }

  // for debug
  private ReadWriteField proteinNameField = new ReadWriteField();

  protected void jbInit() {
    super.jbInit(); // adds featureNameField

    getNameField().setEditable(getStyle().getTranscriptSymbolEditable());

    // ids include "DEBUG" to indicate that Apollo is in debug mode
    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
    if(Config.DEBUG) {
      addField("ID(debug)", featureIDField.getComponent()); // debug - DELETE
      addField("Protein Name(debug)",proteinNameField.getComponent());
      proteinNameField.setEditable(false);
    }

    addSynonymGui();

    addIsProblematicCheckbox();

    if (getStyle().showFinishedCheckbox()) {
      addField ("Finished?", finishedCheckbox); // adds to fieldPanel
      finishedCheckbox.setBackground(bgColor);  // defined in FeatureEditPanel
      finishedCheckbox.setEnabled(!isReadOnly);
    }

    addField("Author", ownerLabel);
    ownerLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    ownerLabel.setForeground(Color.black);
    ownerLabel.setMinimumSize(new Dimension(80, 25));

    if (getStyle().showReplaceStopCheckbox()) {
      //      String replaceStopPhrase = "Replace stop codon with selenocysteine ";
      //      if (Config.getStyle().getAllStyleFilesString().indexOf("chado.style") >= 0)
      String replaceStopPhrase = "Readthrough stop codon";
      addField(replaceStopPhrase, replaceStop);
      replaceStop.setBackground(bgColor);  // defined in FeatureEditPanel
      replaceStop.setEnabled(!isReadOnly);
    }

    nonConsensusSplice = initLabel("Non-consensus splice ");
    addField(nonConsensusSplice, nonConsensusSpliceOkay);
    nonConsensusSpliceOkay.setBackground(bgColor);
    nonConsensusSpliceOkay.setEnabled(!isReadOnly);

    if (Config.getStyle().translationalFrameShiftEditingIsEnabled()) {
      addField("+1 translational frame shift ", 
               slippagePosition);
      slippagePosition.setFont(new Font("Dialog", Font.BOLD, 12));
      slippagePosition.setForeground(Color.black);
      slippagePosition.setMinimumSize(new Dimension(80, 25));
      
      addField("-1 translational frame shift ", 
               stutterPosition);
      stutterPosition.setFont(new Font("Dialog", Font.BOLD, 12));
      stutterPosition.setForeground(Color.black);
      stutterPosition.setMinimumSize(new Dimension(80, 25));
    }

    addField("Missing start codon", missing5prime);
    missing5prime.setFont(new Font("Dialog", Font.BOLD, 12));
    missing5prime.setForeground(Color.black);
    missing5prime.setMinimumSize(new Dimension(80, 25));

    addField("Missing stop codon", missing3prime);
    missing3prime.setFont(new Font("Dialog", Font.BOLD, 12));
    missing3prime.setForeground(Color.black);
    missing3prime.setMinimumSize(new Dimension(80, 25));

    addField("Unconventional start codon", startCodon);
    startCodon.setVisible(true);
    startCodon.setFont(new Font("Dialog", Font.BOLD, 12));
    startCodon.setForeground(Color.black);
    startCodon.setMinimumSize(new Dimension(80, 25));

    // Transcript IDs are not editable
    featureIDField.setEditable(false);
    // pad out bottom of fields panel, for layout - otherwise syns can get huge
    super.addFieldsPanelBottomGlue();
    super.getFeatureBox().add(super.getFieldsPanel()); // where addField stuff goes

    border = new TitledBorder("Transcript");
    border.setTitleColor (Color.black);
    this.setBorder(border);

    addListeners();
  }

  private Transcript getTranscript() {
    return (Transcript)getEditedFeature();
  }

  /** Update gui from model/clone */
  protected void loadSelectedFeature() {
    if (getEditedFeature() == null) {
      clearFields();
    } else {
      isGoodUser();
      Transcript t = getTranscript();
      featureNameField.setValue(t.getName());
      featureNameField.setEditable(goodUser);
      
      // names include "DEBUG" to indicate that Apollo is in debug mode
      // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
      if (Config.DEBUG) {
        featureIDField.setValue(t.getId());
        String n="";
        if (t.getPeptideSequence() != null) {
          n = "Pep acc:  "+t.getPeptideSequence().getAccessionNo() +
            "  Pep name: "+t.getPeptideSequence().getName();
        }
        if (t.get_cDNASequence() != null)
          n += " cDNA acc: "+t.get_cDNASequence().getAccessionNo();
        proteinNameField.setValue(n);
      }

      loadSynonymGui();

      isProblematicCheckbox.setSelected(t.isProblematic());
      isProblematicCheckbox.setEnabled(goodUser);
      
      border.setTitle(t.getFeatureType() + " " + t.getName());
      String owner = t.getOwner();
      FeatureProperty fp
        = Config.getPropertyScheme().getFeatureProperty(t.getTopLevelType());
      Color owner_color = Config.getAnnotationColor (owner, fp);
      ownerLabel.setForeground (owner_color);
      String ownersFullName = Config.getFullNameForUser(owner);
      if (ownersFullName == null)
        ownersFullName = "";
      ownerLabel.setText (ownersFullName);
      border.setTitleColor (owner_color);

      boolean status
        = (t.getProperty("status") != null &&
           t.getProperty("status").equals("all done"));
      finishedCheckbox.setSelected(status);
      finishedCheckbox.setEnabled(goodUser);
      
      int odd_acceptor_exon_index = t.getNonConsensusAcceptorNum();
      int odd_donor_exon_index = t.getNonConsensusDonorNum();
      if (t.size() == 1) {
        nonConsensusSplice.setText("No introns");
        nonConsensusSpliceOkay.setEnabled(false);
        nonConsensusSpliceOkay.setVisible(false);
      }
      else if (odd_acceptor_exon_index == -1 &&
               odd_donor_exon_index == -1) {
        nonConsensusSplice.setText("All splice sites match consensus");
        nonConsensusSpliceOkay.setEnabled(false);
        nonConsensusSpliceOkay.setVisible(false);
      } else {
        String text = "Approve non-consensus";
        if (odd_acceptor_exon_index >= 0)
          text = (text + " acceptor in exon " + (odd_acceptor_exon_index + 1));
        if (odd_donor_exon_index >= 0) {
          if (odd_acceptor_exon_index >= 0)
            text = text + " and";
          text = (text + " donor in exon " + (odd_donor_exon_index + 1));
        }
        nonConsensusSplice.setText(text);
        nonConsensusSpliceOkay.setSelected(t.nonConsensusSplicingOkay());
        nonConsensusSpliceOkay.setEnabled(!isReadOnly);
        nonConsensusSpliceOkay.setVisible(true);
      }

      if (t.isProteinCodingGene()) {
        // The actual value of the readthrough stop residue is now available
        // via the readThroughStopResidue method, but for now, just stick with
        // the yes/no checkbox.
        replaceStop.setSelected(t.hasReadThroughStop());

        int frameshiftPosition;
        frameshiftPosition = t.plus1FrameShiftPosition();
        if (frameshiftPosition > 0) {
          slippagePosition.setText("Yes: " + frameshiftPosition);
        } else {
          slippagePosition.setText("No");
        }

        frameshiftPosition = t.minus1FrameShiftPosition();
        if (frameshiftPosition > 0) {
          stutterPosition.setText("Yes: " + frameshiftPosition);
        } else {
          stutterPosition.setText("No");
        }

        missing5prime.setText(t.isMissing5prime() ? "Yes" : "No");
        //missing3prime.setText(t.isMissing3prime() ? "Yes" : "No");
        setMissing3PrimeGuiFromModel();

        if (t.unConventionalStart()) {
          startCodon.setForeground(DrawableUtil.getStartCodonColor(t));
          startCodon.setText ("Encoding " + t.getStartCodon() +
                              " as Met");
        }
        else {
          startCodon.setForeground(Color.black);
          startCodon.setText("No");
        }
      }
    }
    updateUI();
  }

  /** This is not an editable but is affected by other edits. */
  private void setMissing3PrimeGuiFromModel() {
    boolean miss3 = getEditedFeature().isMissing3prime();
    missing3prime.setText(miss3 ? "Yes" : "No");
  }
  

  private void addListeners() {
    finishedCheckbox.addActionListener(new FinishedCheckboxListener());
    replaceStop.addActionListener(new ReplaceStopCheckboxListener());
    nonConsensusSpliceOkay.addActionListener(new NonConSpliceCheckboxListener());
    getNameField().addFocusListener(new TranscriptNameFocusListener());
  }

  /** Focus driven edits occur in textboxes -> synonyms, names, and ids. If the user
      selects a new annot in annot tree the focus event comes too late AFTER the
      loading of new annot, so this checks for edits before loadAnnotation sets the
      new annotation in place. this just checks syns, gene & trans panels override 
      to check name & ids. */
  protected void checkFocusDrivenEdits() {
    super.checkFocusDrivenEdits(); // synonyms
    // If we were just editing transcript name then do edit, otherwise dont as gui name
    // may be different from model due to gene name change or type change or id change
    if (editingName)
      updateModelNameFromGui();
  }


  /** Change model and fire change event on check box selection */
  private class FinishedCheckboxListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean b = finishedCheckbox.isSelected();
      TransactionSubpart t = TransactionSubpart.FINISHED;
      setBooleanSubpart(b,t);
    }
  }
  private class ReplaceStopCheckboxListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean b = replaceStop.isSelected();
      TransactionSubpart t = TransactionSubpart.REPLACE_STOP;
      setBooleanSubpart(b,t);
      // check if "stop codon missing" state has changed
      setMissing3PrimeGuiFromModel();
    }
  }
  private class NonConSpliceCheckboxListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      boolean guiState = nonConsensusSpliceOkay.isSelected();
      TransactionSubpart ts = TransactionSubpart.NON_CONSENSUS_SPLICE_OKAY;
      setBooleanSubpart(guiState,ts);
    }
  }

  private class TranscriptNameFocusListener implements FocusListener {
    public void focusGained(FocusEvent e) {
      editingName = true;
    }
    public void focusLost(FocusEvent e) {
      updateModelNameFromGui();
      editingName = false;
    }
  }

  /** Checks new gui transcript name is ok, if so commits it to model and
      fires event */
  private void updateModelNameFromGui() {
      if (getGuiName().equals(getModelName()))
        return; // no change -> exit

    if (transcriptNameIsFaulty()) { // if bad returns true, puts up err msg
      setGuiNameFromModel();
      return;
    }

    // transcript name/suffix ok -> change model, fire event
    // this aint right - doesnt fire event anymore - returns transaction
    //commitNameChange();
    Transaction t = getNameAdapter().setTranscriptName(getEditedFeature(),getGuiName());
    t.setSource(getFeatureEditorDialog());
    fireAnnotEvent(t.generateAnnotationChangeEvent(getFeatureEditorDialog()));
    // TODO - introduce a parameter that controls this feature, instead of using global DEBUG (deprecated)
    if (Config.DEBUG) loadSelectedFeature(); // debug trans id
  }

  /** this should all be done with name adapter... */
  private boolean transcriptNameIsFaulty() {

    if (namePrefixNotEqualGeneName())
      return true;

    if (suffixInUse())
      return true;

    if (suffixIsFaulty())
      return true;
      
    return false;
  }

  /** Do we even need this test - in setting gene name transcipt name gets
      set automatically - this should call name adapter - this is mod specific */
  private boolean namePrefixNotEqualGeneName() {
    String prefix = getGuiNamePrefix();
    if (!prefix.equals(getGeneName())) {
      errorPopup("Error: transcript symbol " + getGuiName() +
                " does not match parent annotation's symbol, " + getGeneName());
      return true;
    }
    return false;
  }

  private boolean suffixInUse() {
    Vector trans = getAnnotTranscripts();
    String suffix = getGuiNameSuffix();
    // I am not fond of this method signature - should just take 
    // gene and suffix - i dont see why index is needed since its a suffix change
    if (getNameAdapter().suffixInUse(trans, suffix, getTranscriptIndex())) {
      errorPopup("Error: suffix used twice in transcript symbols: -" + suffix);
      return true;
    }
    return false;
  }

  /** Check that suffix is legal (e.g. -RA) and not something bogus like -R23@5 */
  private boolean suffixIsFaulty() {
    if (!getNameAdapter().checkFormat(getTranscript(),getGuiName())) {
      String err = "Error: illegal transcript suffix format: -" + getGuiNameSuffix();
      String expectedTranscriptNameFormat = getNameAdapter().getTranscriptNamePattern();
      if (expectedTranscriptNameFormat != null)
        err += "\nSuffix name format should be: " + expectedTranscriptNameFormat;
      errorPopup(err);
      return true;
    }
    return false;
  }

  
  private String getGeneName() {
    return getTranscript().getRefFeature().getName();
  }

  private String getGuiNamePrefix() {
    // this needs to come from name adapter .getTranscriptPrefix

    //edited by TAIR (although this should be in here anyway)
    int index = getGuiName().lastIndexOf(getNameAdapter().getSuffixDelimiter());//used to be "-"
    
    if (index > 0)
      return getGuiName().substring(0,index);
    logger.warn(getGuiName() + " is missing a suffix!!!");
    return getGuiName();
  }

  private String getGuiNameSuffix() {
    // this needs to come from name adapter -> getTranscriptSuffix
    return getGuiName().substring(getGuiName().lastIndexOf("-")+1);
  }
  
  private Vector getAnnotTranscripts() {
    return getTranscript().getRefFeature().getFeatures();
  }
  /** Get transcript number with respect to parent/annot */
  private int getTranscriptIndex() {
    Vector v = getAnnotTranscripts();
    for (int i=0; i<v.size(); i++) {
      if (getTranscript() == v.get(i))
        return i;
    }
    return -1; // shouldnt happen
  }
  
}
