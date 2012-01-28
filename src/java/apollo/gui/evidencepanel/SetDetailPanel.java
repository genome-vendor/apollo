package apollo.gui.evidencepanel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.lang.Math;

import apollo.datamodel.*;
import apollo.gui.ApolloFrame;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.gui.Controller;
import apollo.gui.DetailInfo;
import apollo.gui.SelectionManager;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.*;
import apollo.util.*;
import apollo.util.ClipboardUtil;

import org.bdgp.util.*;


import org.apache.log4j.*;


/**
 * A panel to show a table of the individual features in a single feature set.
 *
 * EvidencePanel (which also extends TablePanel) has a SetDetailPanel as its
 * right side panel 
 * (appears on the bottom right or bottom left depending on orientation 
 * of main apollo window)
 */
public class SetDetailPanel extends TablePanel {
  protected final static Logger logger = LogManager.getLogger(SetDetailPanel.class);

  JLabel         labelBox;
  JScrollPane	 descPane;
  JTextArea      descBox;
  JPanel         headerPanel;

  SeqFeatureI fset;

  public SetDetailPanel(Controller c,SelectionManager sm) {
    super (c,sm);
    init();
  }

  public SetDetailPanel(FeatureSetI fset, Controller c,SelectionManager sm) {
    this(c,sm);
    setFeatureSet (fset);
  }

  protected void init() {
    setLayout(new BorderLayout());

    labelBox = new JLabel("");
    labelBox.setFont (new Font ("Helvetica", Font.BOLD, 12));
    labelBox.setHorizontalAlignment (SwingConstants.CENTER);
    labelBox.setOpaque (true);
    labelBox.setForeground (Color.black);

    descBox = new JTextArea("");
    descBox.setEditable(false);
    descBox.setLineWrap(true);
    descBox.setWrapStyleWord(true);
    descBox.registerKeyboardAction(new CopyAction(),
                                   "Copy",
                                   KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                          KeyEvent.CTRL_MASK),
                                   descBox.WHEN_FOCUSED);
    descPane = new JScrollPane(descBox);

    headerPanel = new JPanel();
    headerPanel.setLayout (new BorderLayout());
    headerPanel.add(labelBox,BorderLayout.NORTH);
    headerPanel.add(descPane,BorderLayout.CENTER);

    add(headerPanel, BorderLayout.NORTH);
    add(pane, BorderLayout.CENTER);

    // Set up anonymous class feature selection listener
    // to select feature currently selected elsewhere in apollo
    getController().addListener(new FeatureSelectionListener() {
        public boolean handleFeatureSelectionEvent(FeatureSelectionEvent e) {
          // If the source of the selection came from ourself dont handle it
          if (isSelectionSource(e))
            return false; // false?
          FeatureList f = e.getFeatures();
          // Only select if its just one item, dont select all exons if all
          // exons are selected
          if (f.size() != 1)
            return false;
          selectFeature(f.getFeature(0));
          return true; // does this mean we've handled it?
        }
      }
    );
  }

  public void setFeatureSet (SeqFeatureI fset) {
    this.fset = fset;
    populateTable();
  }

  protected void reset() {
    logger.debug("SDP: reset");
    descBox.setText("");
    labelBox.setText("");
    model.setType("");
    model.setData(new Vector(0));
    model.setPropertyList(new Vector(0));
    fset = null; // dangling ref to old data
  }

  protected void saveSortKey(String type, String key, boolean isDescending) {
    FeatureProperty fp =
      Config.getPropertyScheme().getFeatureProperty(type);
    fp.setSortProperty(key);
    fp.setReverseSort(isDescending);
  }

  protected void populateTable() {
    //System.out.println("Top of populate table with fset = " + fset);
    if (fset == null) {
      reset();
      repaint();
      return;
    }
    Vector feature_list;
    if (fset.hasKids())
      feature_list = SeqFeatureUtil.getSortedKids(fset);
    // gotta use something--if it has no kids, just use fset itself
    else {
      feature_list = new Vector();
      feature_list.add(fset);
    }

    if (feature_list.size() == 0)
      return;

    SeqFeatureI firstFeature = (SeqFeatureI) feature_list.elementAt(0);
    logger.debug("SDP: firstFeature = " + firstFeature);
    FeatureProperty fp =
      Config.getPropertyScheme().getFeatureProperty(firstFeature.getTopLevelType());
    Vector propertyVals = fp.getColumns();
    String sortKey = fp.getSortProperty();
    boolean reverseSort = fp.getReverseSort();

    String oldType = model.getType();
    String oldSortKey = model.getSortKey();
    boolean oldReverseSort = model.getReverseSort();

    model.setType(firstFeature.getFeatureType());
    model.setPropertyList(propertyVals);
    model.setData(feature_list, sortKey, reverseSort);

    if (oldType == null || oldType.equals(firstFeature.getFeatureType())) {
      model.setSortKey(oldSortKey);
      model.setReverseSort(oldReverseSort);
    }

    setupHeader(fset, firstFeature);

    table.repaint();
  }

  public void setToFeature (SeqFeatureI feature) {}

  /** sets text for descBox and labelBox. if FeaturePair label comes from type + 
      hit seq display id or type + hit name, fp desc is hit seq desc or prog+db.
      If not FeatPair, featureDisplayString(feat) is used which returns 
      biotype+featname - and if annot feat tacks on gene description.
      Also tacks on length to label. */
  protected void setupHeader(SeqFeatureI fset, SeqFeatureI sf) {
    String desc = "";
    String name = "";
    int length = 0;

    // FeaturePair
    if (sf.hasHitFeature()) {
      SequenceI seq = fset.getHitSequence();
      SeqFeatureI hit = sf.getHitFeature();
      if (seq == null) {
        seq = hit.getRefSequence();
      }
      if (seq != null) {
        name = seq.getName() != null ? seq.getName() : "";
        if (!(name.equals("")))
          name = DetailInfo.getPropertyType(fset) + ":  " + name;
        desc = seq.getDescription() != null ? seq.getDescription() : "";
        length = seq.getLength() > 0 ? seq.getLength() : 0;
      } else {
        // Just use the display type with its range for the name
        name = Config.getDisplayPrefs().getBioTypeForDisplay(fset);
      }
    } 
    // Not a FeaturePair    
    else { 
      //      if (fset.getRefFeature() != null && fset.getRefFeature().hasFeatureType()) {
      // One-level features' ref features don't have a type, but we still want
      // to use the name in the description box. 
      // one level annots should not use their ref feat
      if (fset.isAnnotTop()) { // this is true for 1 level annots 
        name = Config.getDisplayPrefs().getDisplayName(fset);
      }
      else if (fset.getRefFeature() != null) { // isnt this always true?
        name = Config.getDisplayPrefs().getDisplayName(fset.getRefFeature());
      } else { // typeless ref feature just use self for name, not reached?
        name = Config.getDisplayPrefs().getDisplayName(fset);
      }
      if (fset.getName() != null && !(fset.getName().equals(RangeI.NO_NAME))
          && !(fset.getName().equals(""))) {
        desc = fset.getName();
      }
      if (fset.hasAnnotatedFeature()) {
        AnnotatedFeatureI gene = fset.getAnnotatedFeature();
        while (gene.getRefFeature().hasAnnotatedFeature()) {
          gene = gene.getRefFeature().getAnnotatedFeature();
        }
        desc += " " + gene.getDescription();
      }
    }
    descBox.setText(desc);
    if (length == 0) {
      labelBox.setText (name);
    } else {
      labelBox.setText (name + " (length=" + length + ")");
    }
    labelBox.setBackground (getFeatureColor());
  }

  private Color getFeatureColor () {
    FeatureProperty prop;
    if (fset instanceof DrawableSeqFeature) {
      prop = ((DrawableSeqFeature)fset).getFeatureProperty();
      return prop.getColour();
    } 
    // If it's an annotation, color by owner (if appropriate)
    else if (fset instanceof AnnotatedFeatureI) {
      AnnotatedFeatureI annot = (AnnotatedFeatureI) fset;
      return Config.getAnnotationColor(annot);
    }
    else {
      prop = Config.getPropertyScheme().getFeatureProperty(fset.getTopLevelType());
      return prop.getColour();
    }
    
  }

  private class CopyAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      ClipboardUtil.copyTextToClipboard(descBox.getSelectedText());
    }
  }
}
