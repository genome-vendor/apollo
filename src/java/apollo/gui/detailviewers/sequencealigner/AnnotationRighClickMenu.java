package apollo.gui.detailviewers.sequencealigner;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.bdgp.util.Range;

import apollo.config.Config;
import apollo.datamodel.ExonI;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.SequenceI;
import apollo.editor.AnnotationEditor;
import apollo.gui.BaseScrollable;
import apollo.gui.detailviewers.sequencealigner.actions.CreateExonAction;
import apollo.gui.detailviewers.sequencealigner.actions.CreateIntronAction;
import apollo.gui.detailviewers.sequencealigner.actions.DeleteExonAction;
import apollo.gui.detailviewers.sequencealigner.actions.DisplaySequenceAction;
import apollo.gui.detailviewers.sequencealigner.actions.FrameShiftAction;
import apollo.gui.detailviewers.sequencealigner.actions.MergeExonAction;
import apollo.gui.detailviewers.sequencealigner.actions.SequenceEditAction;
import apollo.gui.detailviewers.sequencealigner.actions.SetEndAction;
import apollo.gui.detailviewers.sequencealigner.actions.SetTranslationStartAction;
import apollo.gui.synteny.GuiCurationState;

/**
 * This is the popup menu that gets displayed when you right click on an 
 * annotation.
 */
public class AnnotationRighClickMenu extends JPopupMenu {

  private static final int MIN_FEATURE_SIZE = 1;

  /** This should be the Annotation window */
  private MultiTierPanel panel;

  /** The curation state */
  private GuiCurationState curationState;

  // TODO fix how this is done! 
  // It would be nice to remove this dependency if possible
  /** This should be reference to the main MultiSequenceAlignerPanel */
  private BaseScrollable scrollableObject;

  // The Actions
  private CreateIntronAction createIntronAction;
  private CreateExonAction createExonAction;
  private DeleteExonAction deleteExonAction;
  private FrameShiftAction plus1FrameShiftAction;
  private FrameShiftAction minus1FrameShiftAction;
  private SetTranslationStartAction setTranslationStartAction;
  private MergeExonAction merge5PrimeAction;
  private MergeExonAction merge3PrimeAction;
  private SetEndAction set5PrimeAction;
  private SetEndAction set3PrimeAction;
  private SequenceEditAction insertionAction;
  private SequenceEditAction deletionAction;
  private SequenceEditAction substitutionAction;
  private SequenceEditAction removeAdjustmentAction;
  private DisplaySequenceAction displaySequenceAction;
  private AbstractAction displayFindAction;
  private AbstractAction displayGoToAction;

  // The Lables
  private JLabel locationLabel;
  private JLabel relativeLocationLabel;

  // The menu items
  protected JMenuItem creaateIntronMenuItem;
  protected JMenuItem createExonMenuItem;
  protected JMenuItem merge5PrimeMenuItem;
  protected JMenuItem merge3PrimeMenuItem;
  protected JMenuItem set5PrimeMenuItem;
  protected JMenuItem set3PrimeMenuItem;
  protected JMenuItem deleteExonMenuItem;
  protected JMenuItem setTranslationStartMenuItem;
  protected JMenuItem plus1FrameShiftMenuItem;
  protected JMenuItem minus1FrameShiftMenuItem;
  protected JMenuItem insertionMenuItem;
  protected JMenuItem deletionMenuItem;
  protected JMenuItem substitutionMenuItem;
  protected JMenuItem removeAdjustmentMenuItem;
  protected JMenu sequencingErrorSubMenu;
  protected JMenuItem findSequenceMenuItem;
  protected JMenuItem displaySequenceMenuItem;
  protected JMenuItem goToMenuItem;

  /**
   * Constructor
   * 
   * @param curationState
   * @param panel should be the annotation panel
   */
  public AnnotationRighClickMenu(GuiCurationState curationState,
      MultiTierPanel panel) {
    super();
    this.scrollableObject = null;
    this.curationState = curationState;
    this.panel = panel;
    init();
  }

  /**
   * Gets the scrollable object for this menu
   * 
   * @return
   */
  public BaseScrollable getScrollableObject() {
    return this.scrollableObject;
  }

  /**
   * Sets the scrollable object for this menu
   * 
   * @param scrollableObject this is the object that will be affected by
   * the "Find Sequence" and "Go To" actions
   */
  public void setScrollableObject(BaseScrollable scrollableObject) {
    this.scrollableObject = scrollableObject;
  }

  /**
   * Updates menu to reflect the options available at the given position and
   * tier.
   * 
   * @param pos - tier position
   * @param tier
   */
  public void formatRightClickMenu(int pos, int dnapos, int tier) {
    disableAllActions();

    // Get the relevant features
    SeqFeatureI feature = panel.getTier(tier)
        .featureAt(pos, TierI.Level.BOTTOM);
    if (feature == null) {
      feature = panel.getTier(tier).featureAt(pos, TierI.Level.TOP);
    }

    SeqFeatureI nextFeature = panel.getTier(tier).getNextFeature(pos,
        TierI.Level.BOTTOM);
    SeqFeatureI prevFeature = panel.getTier(tier).getPrevFeature(pos,
        TierI.Level.BOTTOM);

    //int dnapos = panel.tierPositionToBasePair(pos);

    // Setup the actions
    createIntronAction.setFeature(feature);
    createIntronAction.setSelectedBasePair(dnapos);

    createExonAction.setFeature(feature);
    createExonAction.setSelectedBasePair(dnapos);

    deleteExonAction.setFeature(feature);

    plus1FrameShiftAction.setFeature(feature);
    plus1FrameShiftAction.setSelectedBasePair(dnapos);

    minus1FrameShiftAction.setFeature(feature);
    minus1FrameShiftAction.setSelectedBasePair(dnapos);

    setTranslationStartAction.setFeature(feature);
    setTranslationStartAction.setSelectedBasePair(dnapos);

    merge5PrimeAction.setFeature(feature);
    merge5PrimeAction.setMergeFeature(prevFeature);

    merge3PrimeAction.setFeature(feature);
    merge3PrimeAction.setMergeFeature(nextFeature);

    set5PrimeAction.setFeature(feature);
    set5PrimeAction.setSelectedBasePair(dnapos);

    set3PrimeAction.setFeature(feature);
    set3PrimeAction.setSelectedBasePair(dnapos);

    insertionAction.setFeature(feature);
    insertionAction.setSelectedBasePair(dnapos);
    insertionAction.setParent(panel);
    insertionAction.setType(SequenceI.INSERTION);

    deletionAction.setFeature(feature);
    deletionAction.setSelectedBasePair(dnapos);
    deletionAction.setParent(panel);
    deletionAction.setType(SequenceI.DELETION);

    substitutionAction.setFeature(feature);
    substitutionAction.setSelectedBasePair(dnapos);
    substitutionAction.setParent(panel);
    substitutionAction.setType(SequenceI.SUBSTITUTION);

    removeAdjustmentAction.setFeature(feature);
    removeAdjustmentAction.setSelectedBasePair(dnapos);
    removeAdjustmentAction.setParent(panel);
    removeAdjustmentAction.setType(SequenceI.CLEAR_EDIT);

    displaySequenceAction.setFeature(feature);

    // Set up the Labels
    locationLabel.setText("   Location = " + dnapos);
    if (feature != null) {
      if (feature instanceof ExonI) {
        FeatureSetI fs = (FeatureSetI) feature.getRefFeature(); // transcript
        int feat_index = fs.getFeatureIndex(feature);
        int mRNA_loc = (int) fs.getFeaturePosition(dnapos);
        int exon_loc = (int) feature.getFeaturePosition(dnapos);
        relativeLocationLabel.setText("   Base " + exon_loc + " of exon "
            + (feat_index + 1) + " mRNA base " + mRNA_loc);
      } else {
        int trans_loc = (int) feature.getStart()
            + (dnapos * feature.getStrand());
        relativeLocationLabel
            .setText("   Base " + trans_loc + " of transcript");
      }
    } else {
      relativeLocationLabel.setText("   No feature at position");
      relativeLocationLabel.setEnabled(false);
    }

    // Enable all actions which are possible at this position

    if (this.scrollableObject != null) {
      this.displayGoToAction.setEnabled(true);
      this.displayFindAction.setEnabled(true);
    }

    if (feature != null) {
      int start_pos = panel.basePairToTierPosition(feature.getStart());
      int end_pos = panel.basePairToTierPosition(feature.getEnd());

      displaySequenceAction.setEnabled(true);

      if (feature.hasKids()) {
        // cursor is between exons (on an intron)
        Range intronRange = panel.getTier(tier)
            .rangeAt(pos, TierI.Level.BOTTOM);

        createExonAction.setEnabled(dnapos != intronRange.getLow()
            && dnapos != intronRange.getHigh());

      } else {
        // feature has no children -> courser is on an exon or 1 level annot
        boolean isExon = feature.isExon();
        Range exonRange = panel.getTier(tier).rangeAt(pos, TierI.Level.BOTTOM);
        FeatureSetI fs = (FeatureSetI) feature.getRefFeature();

        createIntronAction.setEnabled(isExon && dnapos != exonRange.getLow()
            && dnapos != exonRange.getHigh());

        deleteExonAction.setEnabled(isExon);

        engagePlus1FrameShiftMenuItem(isExon, fs, dnapos);
        engageMinus1FrameShiftMenuItem(isExon, fs, dnapos);

        setTranslationStartAction.setEnabled(isExon);

        this.merge5PrimeMenuItem.setEnabled(prevFeature != null
            && prevFeature.getRefFeature() == feature.getRefFeature());
        this.merge3PrimeMenuItem.setEnabled(nextFeature != null
            && nextFeature.getRefFeature() == feature.getRefFeature());

        set5PrimeAction.setEnabled(pos != start_pos
            && notTooSmall(pos, end_pos));
        set3PrimeAction.setEnabled(pos != end_pos
            && notTooSmall(start_pos, pos));

        // actually deleting is buggy for both exons & 1 level annots
        // the delete is going through but ede is not letting go of deleted feat
        // or not refreshing properly or something
        if (Config.getStyle().seqErrorEditingIsEnabled() 
            && panel.getType() == SequenceType.DNA)
          sequencingErrorMenuItem(fs, dnapos);
      }

    }

  }

  /**
   * Initializes the menu
   */
  private void init() {

    /* Create Actions */
    createActions();
    disableAllActions();

    /* Create Menu Items */
    createMenuItems();

    /* Create Menu */
    add(locationLabel);
    add(relativeLocationLabel);
    addSeparator();
    add(displaySequenceMenuItem);
    add(findSequenceMenuItem);
    add(goToMenuItem);
    addSeparator();
    add(creaateIntronMenuItem);
    add(createExonMenuItem);
    add(deleteExonMenuItem);
    add(merge5PrimeMenuItem);
    add(merge3PrimeMenuItem);
    add(set5PrimeMenuItem);
    add(set3PrimeMenuItem);
    add(setTranslationStartMenuItem);
    if (panel.getType() == SequenceType.DNA) {
      addSeparator();
      if (Config.getStyle().translationalFrameShiftEditingIsEnabled()) {
        add(plus1FrameShiftMenuItem);
        add(minus1FrameShiftMenuItem);
      }
      if (Config.getStyle().seqErrorEditingIsEnabled())
        add(sequencingErrorSubMenu);
    }
  }

  /**
   * Creates all of the actions
   */
  private void createActions() {
    createIntronAction = new CreateIntronAction("Create intron", curationState,
        getEditor());

    createExonAction = new CreateExonAction("Create exon", curationState,
        getEditor());
    deleteExonAction = new DeleteExonAction("Delete exon", curationState,
        getEditor());

    plus1FrameShiftAction = new FrameShiftAction(
        "Set +1 translation frame shift here", curationState, getEditor(),
        FrameShiftAction.FrameShiftType.PLUS1);
    minus1FrameShiftAction = new FrameShiftAction(
        "Set -1 translation frame shift here", curationState, getEditor(),
        FrameShiftAction.FrameShiftType.MINUS1);

    setTranslationStartAction = new SetTranslationStartAction(
        "Set start of translation", curationState, getEditor());

    merge5PrimeAction = new MergeExonAction("Merge with 5' exon",
        curationState, getEditor());
    merge3PrimeAction = new MergeExonAction("Merge with 3' exon",
        curationState, getEditor());

    set5PrimeAction = new SetEndAction("Set as 5' end", curationState,
        getEditor(), getEditor().START);
    set3PrimeAction = new SetEndAction("Set as 3' end", curationState,
        getEditor(), getEditor().END);

    insertionAction = new SequenceEditAction("Insert", curationState,
        getEditor(), SequenceI.INSERTION);
    deletionAction = new SequenceEditAction("Delete", curationState,
        getEditor(), SequenceI.DELETION);
    substitutionAction = new SequenceEditAction("Substitute", curationState,
        getEditor(), SequenceI.SUBSTITUTION);
    removeAdjustmentAction = new SequenceEditAction("Remove Adjustment",
        curationState, getEditor(), SequenceI.CLEAR_EDIT);

    displaySequenceAction = new DisplaySequenceAction("Display Sequence",
        curationState);

    displayFindAction = new AbstractAction("Find Sequence") {
      public void actionPerformed(ActionEvent arg0) {
        FindDialog fd = new FindDialog(getScrollableObject(), panel, panel
            .getTier(0).getReference(), panel.getStrand() == Strand.REVERSE);
        fd.show();
      }
    };

    displayGoToAction = new AbstractAction("Go To") {
      public void actionPerformed(ActionEvent arg0) {
        GoToDialog gt = new GoToDialog(getScrollableObject());
        gt.show();
      }
    };
  }

  /**
   * Creates all of the menu items
   */
  private void createMenuItems() {
    locationLabel = new JLabel("Location");
    relativeLocationLabel = new JLabel("Relative location");

    displaySequenceMenuItem = new JMenuItem(displaySequenceAction);

    creaateIntronMenuItem = new JMenuItem(createIntronAction);

    createExonMenuItem = new JMenuItem(createExonAction);
    deleteExonMenuItem = new JMenuItem(deleteExonAction);

    plus1FrameShiftMenuItem = new JMenuItem(plus1FrameShiftAction);
    minus1FrameShiftMenuItem = new JMenuItem(minus1FrameShiftAction);

    setTranslationStartMenuItem = new JMenuItem(setTranslationStartAction);

    merge5PrimeMenuItem = new JMenuItem(merge5PrimeAction);
    merge3PrimeMenuItem = new JMenuItem(merge3PrimeAction);

    set5PrimeMenuItem = new JMenuItem(set5PrimeAction);
    set3PrimeMenuItem = new JMenuItem(set3PrimeAction);

    insertionMenuItem = new JMenuItem(insertionAction);
    deletionMenuItem = new JMenuItem(deletionAction);
    substitutionMenuItem = new JMenuItem(substitutionAction);

    removeAdjustmentMenuItem = new JMenuItem(removeAdjustmentAction);

    sequencingErrorSubMenu = new JMenu("Adjust for sequencing error here");
    sequencingErrorSubMenu.add(insertionMenuItem);
    sequencingErrorSubMenu.add(deletionMenuItem);
    sequencingErrorSubMenu.add(substitutionMenuItem);

    this.findSequenceMenuItem = new JMenuItem(displayFindAction);
    this.goToMenuItem = new JMenuItem(displayGoToAction);
  }

  /**
   * Sets all of the actions to be disabled
   */
  private void disableAllActions() {
    createIntronAction.setEnabled(false);
    createExonAction.setEnabled(false);
    deleteExonAction.setEnabled(false);
    plus1FrameShiftAction.setEnabled(false);
    minus1FrameShiftAction.setEnabled(false);
    setTranslationStartAction.setEnabled(false);
    merge5PrimeAction.setEnabled(false);
    merge3PrimeAction.setEnabled(false);
    set5PrimeAction.setEnabled(false);
    set3PrimeAction.setEnabled(false);
    insertionAction.setEnabled(false);
    deletionAction.setEnabled(false);
    substitutionAction.setEnabled(false);
    removeAdjustmentAction.setEnabled(false);
    displaySequenceAction.setEnabled(false);
    displayGoToAction.setEnabled(false);
    displayFindAction.setEnabled(false);
  }

  /**
   * Format the menu item for plus1 frame shifts
   * @param isExon
   * @param fs
   * @param dnapos
   */
  private void engagePlus1FrameShiftMenuItem(boolean isExon, FeatureSetI fs,
      int dnapos) {
    plus1FrameShiftAction.setSelectedBasePair(dnapos);
    plus1FrameShiftMenuItem.setText("Set +1 translational frame shift here");

    if (isExon && fs.withinCDS(dnapos) && !isSequencingErrorPosition(dnapos)
        && fs.minus1FrameShiftPosition() != dnapos) {
      plus1FrameShiftAction.setEnabled(true);

      if (dnapos == fs.plus1FrameShiftPosition()) {
        plus1FrameShiftAction.setSelectedBasePair(0); // setting to 0 removes shift
        plus1FrameShiftMenuItem
            .setText("Remove +1 translational frame shift here");
      }
    }
  }

  /**
   * Format the menu item for minus1 frame shifts
   * @param isExon
   * @param fs
   * @param dnapos
   */
  private void engageMinus1FrameShiftMenuItem(boolean isExon, FeatureSetI fs,
      int dnapos) {
    minus1FrameShiftAction.setSelectedBasePair(dnapos);
    minus1FrameShiftMenuItem.setText("Set -1 translational frame shift here");

    if (isExon && fs.withinCDS(dnapos) && !isSequencingErrorPosition(dnapos)
        && fs.plus1FrameShiftPosition() != dnapos) {
      minus1FrameShiftAction.setEnabled(true);

      if (dnapos == fs.minus1FrameShiftPosition()) {
        minus1FrameShiftAction.setSelectedBasePair(0);
        minus1FrameShiftMenuItem
            .setText("Remove -1 translational frame shift here");
      }
    }
  }

  /**
   * Format the menu item for sequence adjustments
   * @param fs
   * @param dnapos
   */
  private void sequencingErrorMenuItem(FeatureSetI fs, int dnapos) {
    if (fs.plus1FrameShiftPosition() != dnapos
        && fs.minus1FrameShiftPosition() != dnapos) {
      SequenceEdit seq_edit = fs.getSequencingErrorAtPosition(dnapos);
      if (seq_edit != null) {
        this.remove(sequencingErrorSubMenu);
        removeAdjustmentMenuItem.setText("Remove " + seq_edit.getEditType()
            + " here");
        this.add(removeAdjustmentMenuItem);
        removeAdjustmentAction.setEnabled(true);
      } else {
        this.remove(removeAdjustmentMenuItem);
        this.add(sequencingErrorSubMenu);
        insertionAction.setEnabled(true);
        deletionAction.setEnabled(true);
        substitutionAction.setEnabled(true);

      }
    } else {
      this.remove(sequencingErrorSubMenu);
      this.remove(removeAdjustmentMenuItem);
    }
  }

  /**
   * Determines if a feature would be considered too small based on the
   * given start and end position.
   * 
   * @param start_pos
   * @param end_pos
   * @return (end_pos - start_pos + 1) >= MIN_FEATURE_SIZE
   */
  private boolean notTooSmall(int start_pos, int end_pos) {
    return (end_pos - start_pos + 1) >= MIN_FEATURE_SIZE;
  }

  /**
   * Gets the annotation editor
   * 
   * @return
   */
  private AnnotationEditor getEditor() {
    return curationState
        .getAnnotationEditor(panel.getStrand() == Strand.FORWARD);
  }

  /**
   * Determines whether or not the position has a sequencing error
   * @param dnapos
   * @return
   */
  private boolean isSequencingErrorPosition(int dnapos) {
    return curationState.getCurationSet().getRefSequence()
        .isSequencingErrorPosition(dnapos);
  }

}
