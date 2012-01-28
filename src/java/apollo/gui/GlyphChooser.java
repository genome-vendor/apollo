package apollo.gui;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import apollo.gui.*;
import apollo.config.*;
import apollo.editor.ResultChangeEvent;
import apollo.editor.FeatureChangeEvent;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.datamodel.FeatureSetI;
import apollo.util.GuiUtil;
import apollo.gui.drawable.DrawableUtil;
import apollo.gui.drawable.Drawable;

import org.apache.log4j.*;


public class GlyphChooser extends JPanel implements ActionListener {
  protected final static Logger logger = LogManager.getLogger(GlyphChooser.class);

  ArrayListTransferHandler arrayListHandler;
  FeatureProperty featureProperty;
  JComboBox glyphChoiceList;
  JButton   applyButton;

  DefaultListModel includedProps, excludedProps;

  public GlyphChooser(FeatureProperty fp) {

    featureProperty = fp;

    // SMJS Hard coded - I'm not interested in making this complicated. If someone wants to that's fine.
    String[] glyphs = {
             "DrawableResultFeatureSet",
             "DrawableGeneFeatureSet",
             "DrawablePhaseHighlightGeneFeatureSet",
             "DrawablePhaseHighlightNoHatGeneFeatureSet",
    };
    glyphChoiceList = new JComboBox(glyphs);
    glyphChoiceList.setEditable(true);
    glyphChoiceList.addActionListener(this);
    glyphChoiceList.getEditor().setItem(featureProperty.getStyle());

    applyButton = new JButton("Apply");
    applyButton.addActionListener(this);

    setLayout(new GridBagLayout());
    JLabel info = new JLabel("Choose glyph to display " + featureProperty.getDisplayType() + " with");
    add(info, GuiUtil.makeConstraintAt(0,0,1));
    add(glyphChoiceList, GuiUtil.makeConstraintAt(0,1,1));
    add(applyButton, GuiUtil.makeConstraintAt(0,2,1));
    setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == applyButton) {
      String glyphStr = (String)glyphChoiceList.getSelectedItem();
      
      // Check that the class actually exists
      Drawable glyph = DrawableUtil.createGlyph(glyphStr);
      if (glyph == null) {
        logger.warn("Unable to find drawable Java class for " + glyphStr + " - not setting.");
        return;
      }

        
      logger.info("Setting glyph for " + featureProperty.getDisplayType() + " to " + glyphStr);
      featureProperty.setStyle(glyphStr);

      for (int i=0;i<CurationManager.getCurationManager().numberOfCurations();i++) {
        GuiCurationState curationState = CurationManager.getCurationManager().getCurationState(i);
        StrandedZoomableApolloPanel szap = curationState.getSZAP();
  
        // SMJS Copied from LoadUtil
        //      No suitable ResultChangeEvent for this - want one that changes all drawables of a certain feature type
        //      Maybe should add another ResultChangeEvent type for this, but I wasn't brave enough
        // Force a redraw by calling szap.setFeatureSet.  Maybe this is overkill, but it WORKS,
        // which the ResultChangeEvent throwing did not.
        szap.setFeatureSet(szap.getCurationSet());
        // Also redraw annotations
        szap.setAnnotations(szap.getCurationSet());
      }
    }
  }
}
