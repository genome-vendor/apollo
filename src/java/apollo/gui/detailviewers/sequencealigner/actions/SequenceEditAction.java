package apollo.gui.detailviewers.sequencealigner.actions;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.bdgp.util.DNAUtils;

import apollo.config.Config;
import apollo.datamodel.CurationSet;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.editor.AnnotationEditor;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.FeatureList;

/**
 * An Action class for creating a single base sequence edit.
 * 
 * The types of edits allowed are:
 *  SequenceI.INSERTION
 *  SequenceI.DELETION
 *  SequenceI.SUBSTITUTION
 *  SequenceI.CLEAR_EDIT
 *  
 */
public class SequenceEditAction extends AbstractAction {

  /** Static Variables **/
  private static Object[] opts = { "OK", "Cancel" };
  private static Object[] bases = { "A", "T", "G", "C", "N", "Cancel" };
  private static Object[] bases_noA = { "T", "G", "C", "N", "Cancel" };
  private static Object[] bases_noT = { "A", "G", "C", "N", "Cancel" };
  private static Object[] bases_noG = { "A", "T", "C", "N", "Cancel" };
  private static Object[] bases_noC = { "A", "T", "G", "N", "Cancel" };
  private static Map<String, Object[]> exclusionMap = new HashMap<String, Object[]>();

  static {
    exclusionMap.put("A", bases_noA);
    exclusionMap.put("T", bases_noT);
    exclusionMap.put("G", bases_noG);
    exclusionMap.put("C", bases_noC);
  }

  /** Instance Variables **/
  private GuiCurationState curationState;
  private AnnotationEditor annotationEditor;
  private SeqFeatureI feature;
  private int basepair;
  private String type;
  private JComponent parent;

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   */
  public SequenceEditAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor) {
    super(name);
    this.curationState = curationState;
    this.annotationEditor = annotationEditor;
    this.feature = null;
    this.basepair = -1;
    this.type = null;
    this.parent = null;
    this.setEnabled(false);
  }

  /**
   * Constructor
   * @param name the name of this action
   * @param curationState the curation state
   * @param annotationEditor the annotation editor
   * @param type the type of sequence edit this action will perform
   */
  public SequenceEditAction(String name, GuiCurationState curationState,
      AnnotationEditor annotationEditor, String type) {
    this(name, curationState, annotationEditor);
    setType(type);
  }

  /**
   * Performs the sequence edit
   */
  public void actionPerformed(ActionEvent e) {

    // TODO check that basepair is within exon region
    if (basepair < 0 || feature == null) {
      throw new IllegalStateException("basepair:" + basepair + " feature:"
          + feature);
    }
    if (type.equals(SequenceI.INSERTION)) {
      handleInsertion();
    } else if (type.equals(SequenceI.DELETION)) {
      handleDeletion();
    } else if (type.equals(SequenceI.SUBSTITUTION)) {
      handleSubstitution();
    } else if (type.equals(SequenceI.CLEAR_EDIT)) {
      handleClearEdit();
    } else {
      throw new IllegalStateException("Unknown edit type:" + type);
    }

    curationState.getSZAP().repaint();
    // TODO repaint all msa's
  }

  /**
   * Sets the feature to be edited
   * @param feature
   */
  public void setFeature(SeqFeatureI feature) {
    this.feature = feature;
  }

  /**
   * Sets the base pair to be edited
   * @param basepair
   * 
   * NOTE: does this have to be within an exon region of a feature? 
   * I don't think so
   */
  public void setSelectedBasePair(int basepair) {
    this.basepair = basepair;
  }

  /**
   * Sets the type of edit which will happen
   * Should be one of:
   *  SequenceI.INSERTION
   *  SequenceI.DELETION
   *  SequenceI.SUBSTITUTION
   *  SequenceI.CLEAR_EDIT
   * 
   * @param type
   */
  public void setType(String type) {
    if (type != null
        && !(type.equals(SequenceI.INSERTION)
            || type.equals(SequenceI.DELETION)
            || type.equals(SequenceI.SUBSTITUTION) || type
            .equals(SequenceI.CLEAR_EDIT))) {
      throw new IllegalArgumentException("Invalid type:" + type);
    }
    this.type = type;
  }

  /**
   * Sets the parent component for the dialog box that pops up
   * @param parent
   */
  public void setParent(JComponent parent) {
    this.parent = parent;
  }

  //********************************************************
  //
  //  Private Methods
  //
  //*********************************************************

  /**
   * Handler for insertions
   */
  private void handleInsertion() {
    String base = getBaseFromUser("Enter base to be inserted at " + basepair,
        bases);
    if (base != null) {
      dealWithSequenceEdit(feature, basepair, SequenceI.INSERTION, base);
    }
  }

  /**
   * Handler for deletions
   */
  private void handleDeletion() {
    dealWithSequenceEdit(feature, basepair, SequenceI.DELETION, null);
  }

  /**
   * Handler for substitutions
   */
  private void handleSubstitution() {
    char old_base = feature.getRefSequence().getBaseAt(basepair);

    if (feature.getStrand() == Strand.REVERSE.toInt()) {
      old_base = DNAUtils.complement(old_base);
    }

    Object[] options = exclusionMap.containsKey(String.valueOf(old_base)
        .toUpperCase()) ? exclusionMap.get(String.valueOf(old_base)
        .toUpperCase()) : bases;

    String base = getBaseFromUser("Enter base to substitute for " + old_base
        + " at " + basepair, options);

    if (base != null) {
      dealWithSequenceEdit(feature, basepair, SequenceI.SUBSTITUTION, base);
    }
  }

  /**
   * Handler for edit removals
   */
  private void handleClearEdit() {
    dealWithSequenceEdit(feature, basepair, SequenceI.CLEAR_EDIT, null);
  }

  /**
   * Prompts the user to select a base for a set of bases
   * 
   * @param msg the message that the user will be prompted with
   * @param opts the set of bases the user can select from
   * @return a single character string with the selected base
   * 
   * If "Cancel" is included in opts, null will be returned upon selection
   */
  private String getBaseFromUser(String msg, Object[] opts) {
    JOptionPane pane = new JOptionPane(msg, JOptionPane.QUESTION_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION, null, opts, opts[0]);

    JDialog dialog = pane.createDialog(parent, "Please select a base");
    dialog.setBackground(Config.getAnnotationBackground());
    dialog.show();

    String result = (String) pane.getValue();
    if (result.equals("Cancel")) {
      result = null;
    }

    /* is there a reason for this amount of checking? */
    /*if (result != null) {
      for (int i = 0; i < bases.length && base == null; i++)
        base = (bases[i].equals(result) ? (String) result : null);
      if (base.equals("Cancel"))
        base = null;
    }*/
    return result;
  }

  /**
   * Does the actual sequence editing
   * 
   * Deletions of seq edit come in here with operation CLEAR_EDIT 
   */
  private void dealWithSequenceEdit(SeqFeatureI feature, int basepair,
      String operation, String base) {

    /* get the list of transcripts that overlap this position */
    CurationSet curation = curationState.getCurationSet();
    SequenceI seq = curation.getRefSequence();

    /* populates annots vector with leaf features at the current basepair */
    FeatureList annots = curation.getAnnots().getLeafFeatsOver(basepair);

    // TODO non-exons are being returned, this shouldn't be happening
    // clean them out from our exon list
    FeatureList exons = new FeatureList();
    for (int i = 0; i < annots.size(); ++i) {
      SeqFeatureI feat = annots.getFeature(i);
      if (feat.isExon()) {
        exons.add(feat);
      }
    }

    boolean approved = true;

    /* get approval to modify each of the exons which were not selected.
     * changes to all of them must be approved in order for the action to
     * take place.*/
    for (int i = 0, size = exons.size(); i < size && approved; i++) {
      SeqFeatureI exon = exons.getFeature(i);
      if (exon != feature) {
        approved = getOkayFromUser(operation, exon);
      }
    }
    if (approved) {
      annotationEditor.setSequencingErrorPositions(seq, operation, basepair,
          base, exons);
    }
  }

  /**
   * Creates a dialog box that prompts user for aproval to perform an operation
   * on a feature.
   * 
   * @param operation the operation to be performed
   * @param sf the feature which will be affected
   * @return true if the user oks the operation false otherwise
   */
  private boolean getOkayFromUser(String operation, SeqFeatureI sf) {
    String msg = "This "
        + ((operation.equals(SequenceI.CLEAR_EDIT) ? "" : operation + " ")
            + "will affect the transcript of " + sf.getRefFeature().getName() 
            + ". Are you sure you want to do this?");

    JOptionPane pane = new JOptionPane(msg, JOptionPane.QUESTION_MESSAGE,
        JOptionPane.OK_CANCEL_OPTION, null, opts, opts[1]);

    JDialog dialog = pane.createDialog(parent, "Please confirm");
    dialog.show();

    Object result = pane.getValue();
    return result != null && opts[0].equals(result);
  }
}
