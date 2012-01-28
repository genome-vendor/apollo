package apollo.gui.evidencepanel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.editor.AnnotationChangeEvent;
import apollo.editor.AnnotationChangeListener;
import apollo.seq.*;
import apollo.util.FeatureList;
import apollo.util.SeqFeatureUtil;
import apollo.gui.Controller;
import apollo.gui.SelectionManager;
import apollo.gui.event.*;
import apollo.gui.DetailInfo;
import apollo.gui.drawable.Drawable;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.UnBorderedSplitPane;
import apollo.config.PropSchemeChangeListener;
import apollo.config.PropSchemeChangeEvent;
import apollo.config.ColumnProperty;

import misc.JIniFile;

import org.apache.log4j.*;


/**
 * A panel containing a table of selected result feature sets.
 *
 * extends TablePanel extends ControlledPanel extends JPanel
 * Puts a JSplitPane in its JPanel. TablePanel's JScrollPane pane
 * is on the left side of JSplitPane, constructs a SetDetailPanel
 * and puts that in the right hand side.
 * (This is currently shown at the bottom of the main apollo frame)
 */
public class EvidencePanel extends TablePanel {
  protected final static Logger logger = LogManager.getLogger(EvidencePanel.class);

  private SetDetailPanel    setDetailPanel;
  UnBorderedSplitPane        split;
  ApolloFeatureSelectionListener featureSelectionListener =
    new ApolloFeatureSelectionListener();

  public static Vector annot_columns = new Vector(3);
  static
  {
    //annot_columns.addElement("Type");
    annot_columns.addElement(new ColumnProperty("BIOTYPE"));
    annot_columns.addElement(new ColumnProperty("NAME"));
    annot_columns.addElement(new ColumnProperty("Range"));
  }

  public static Vector result_columns = new Vector(4);
  static
  {
    result_columns.addElement(new ColumnProperty("Type"));
    //result_columns.addElement("BioType");
    result_columns.addElement(new ColumnProperty("NAME"));
    result_columns.addElement(new ColumnProperty("Range"));
    result_columns.addElement(new ColumnProperty("SCORE"));
  }

  public EvidencePanel (GuiCurationState curationState) {
    super (curationState.getController(),curationState.getSelectionManager());
    logger.debug("Making evidence panel");
    curationState.getController().addListener(featureSelectionListener);
    curationState.getController().addListener(new EvPanPropSchemeCngListener());
    init();
  }

  private void init () {
    setLayout(new BorderLayout());

    setDetailPanel = new SetDetailPanel(getController(),getSelectionManager());
    split = new UnBorderedSplitPane(JSplitPane.VERTICAL_SPLIT, false, pane, setDetailPanel);
    //split.setDividerLocation(500);  // Changing this doesn't seem to do anything
    //split.setDividerLocation(0.6);
    add(split, BorderLayout.CENTER);

    setMinimumSize(new Dimension(150,0));  // Let user collapse it vertically if they so choose
    // You'd think that setting the min size to 0,0 would let them collapse
    // it horizontally when the evidence panel is in vertical mode (right now
    // it's not letting the user adjust it much), but unfortunately that makes
    // it come up collapsed to nothing--the preferred size seems to have no 
    // effect. :-(
    //    setMinimumSize(new Dimension(0,0));  // Let user collapse it entirely if they so choose
    // Changing preferred size seems to have no effect
    setPreferredSize(new Dimension(150,150));

    // SMJS While adding PropertySchemeChange support I noticed this call. I'm not sure this is the 
    // right controller any more (might be curationState.getController()), but I'm not in a position 
    // to test this
    getController().addListener(new EvPanAnnotCngListener());
  }

  /** Clear out the tables */
  public void reset() {
    model.setType("");
    model.setData(new Vector(0));
    model.setPropertyList(new Vector(0));
    setDetailPanel.reset();
  }


  /** Populates evidence panel table as well as set detail panel */
  private void populateBothTablesFromSelection(FeatureList selection) {

    FeatureList featSets = getFeatureSetsFromSelection(selection);
   
    if (featSets.size() == 0) {
      reset(); // this clears out the table
      return;
    }

    Vector columns = ((haveScore (featSets)) ?
                      result_columns : annot_columns);

    String oldType = model.getType();
    String oldSortKey = model.getSortKey();
    //    boolean oldReverseSort = model.getReverseSort();

    SeqFeatureI firstFeature = featSets.first();
    // sequence errors have no ref feature - should they? and theres no reason
    // to display them in eveidence panel
    if (firstFeature.getRefFeature() == null)
      return;

    model.setType(firstFeature.getFeatureType());
    model.setPropertyList(columns);

    String sortKey = "Range";
    boolean sortDescending = (firstFeature.getStrand() < 0);
    if (haveScore(featSets)) {
      sortKey = "SCORE";
      sortDescending = true;
      // Make SCORE column sort descending on left click
      // (ascend on shift left click)
      model.setColumnDefaultSortingDescending("SCORE");
    }
    model.setData(featSets, sortKey, sortDescending);

    if (oldType == null || oldType.equals(firstFeature.getFeatureType())) {
      model.setSortKey(oldSortKey);
    }

    // setup SetDetailPanel with last set
    setDetailPanel.setFeatureSet(featSets.last());

    table.getColumn("Type").setCellRenderer (new TypeCellRenderer());
    table.repaint();

  }

  /** Returns FeatureList of feature sets - filters out codons */
  private FeatureList getFeatureSetsFromSelection(FeatureList selection) {
    FeatureList sets = new FeatureList(true); // true -> unique check
    for (int i=0; i < selection.size() ;i++) {
      SeqFeatureI  sf = selection.getFeature(i);
      if (!displayInEvidencePanel(sf))
        continue; // skip codons (and acceptors and donors (pase?))
      SeqFeatureI set=null;
      if (sf.canHaveChildren()) // hasKids()? actually canHave works for 1 levels
        set = sf;
      else
        set = sf.getRefFeature();
      if (set == null) // is this even possible?
        continue;
      sets.addFeature(set);
    }
    return sets;
  }

  /** codons, donors & acceptors are not displayed in ev panel 
   * are acceptors & donors pase? yes says suzi */
  private boolean displayInEvidencePanel(SeqFeatureI sf) {
    return !sf.isCodon(); // && !sf.isDonorOrAcceptor();
  }

  /** Overrides TablePanel setToFeature, called from TablePanel when feat is
   * selected in table - sets setDetailPanel to feat */
  public void setToFeature(SeqFeatureI feature) {
    setDetailPanel.setFeatureSet (feature);
  }

  public void setOrientation(int newOrientation) {
    split.setOrientation(newOrientation);
    //split.setDividerLocation(0.6); // fractional amount does not work until displayed
    //split.setDividerLocation(100);
  }

  /** should we have public getter and setter for this variable? Is 50% a good initial
      setting? */
  private double initialDividerPercentage = 0.5;
  private boolean firstPaint = true;
  /** Overriding paint to set the initial divider location as a percentage - this
      only works if the split pane has a size/is being displayed, so have to wait
      til paint time to get this in (is there a better way to do this?). It only
      sets the fraction with the first paint, so it will respect the user changing it.
  */
  public void paint(Graphics g) {
    if (firstPaint) { 
      split.setDividerLocation(initialDividerPercentage); 
      firstPaint = false; 
    }
    super.paint(g);
  }

  private boolean haveScore (FeatureList feature_list) {
    boolean scored = false;
    for (int i = 0; i < feature_list.size() && !scored; i++) {
      SeqFeatureI sf = feature_list.getFeature(i);
      //      String type = DetailInfo.getPropertyType(sf);
      // WATCH OUT, this is a nasty hard-coded assumption,
      // quite yucky, but it is past midnight
      //      scored = ! type.equals ("Annotation");
      // 4/03 There are now different types of annotation, but they're all
      // in the annotation tier.  Let's try this.
      String tier_label = DetailInfo.getTier(sf).toString();
      // Are there any other tiers or types that don't have scores?
      // this is a little presumptious isnt it?
      if (!(tier_label.equalsIgnoreCase("Annotation")))
	scored = true;
    }
    return scored;
  }

  /**
   * Inner class for listening for feature selections. Note that we avoid
   * responding to events which are fired during synteny views when homologous
   * features are selected...
  **/
  private class ApolloFeatureSelectionListener implements FeatureSelectionListener {
    public boolean handleFeatureSelectionEvent(FeatureSelectionEvent evt) {
      if(evt.getSource() instanceof TablePanel.RowSelectionListener)
        return false;
      populateBothTablesFromSelection(evt.getFeatures());
      return true;
    }
  } // end of inner class
  
  /** Resynchs with selection on annot change events */
  private class EvPanAnnotCngListener implements AnnotationChangeListener {

    public boolean handleAnnotationChangeEvent(AnnotationChangeEvent a) {
      FeatureList l = getSelectionManager().getSelection().getSelectedData();
      populateBothTablesFromSelection(l);
      return true;
    }
  }

  private class EvPanPropSchemeCngListener implements PropSchemeChangeListener {

    public boolean handlePropSchemeChangeEvent(PropSchemeChangeEvent e) {
      FeatureList l = getSelectionManager().getSelection().getSelectedData();
      populateBothTablesFromSelection(l);
      return true;
    }
  }

  public void saveLayout(JIniFile iniFile, String section) {
    iniFile.writeInteger(section, "split_position", split.getDividerLocation());
  }

  public void applyLayout(JIniFile iniFile, String section, boolean updateBaseLocation) {

    int splitPos = iniFile.readInteger(section, "split_position", split.getDividerLocation());
    split.validate();
    split.setDividerLocation(splitPos);
 
    // Stop the first paint stuff happening if we're deliberately applying a layout
    firstPaint = false;
  }
}
