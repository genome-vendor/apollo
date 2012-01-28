package apollo.gui.menus;

// This is so sloppy!  Should import the appropriate classes, not everything. */
import apollo.config.Config;
import apollo.config.PropertyScheme;
import apollo.config.FeatureProperty;
import apollo.gui.ApolloFrame;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.Selection;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.util.*;

import java.awt.event.*;
import java.awt.Color;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;

/** This should actually be called AnnotationMenu (but that's already used
    as the name of the annotation popup menu).
    Top-level menu that lists all annotations in alphabetical order. */

public class TranscriptMenu extends JMenu {

  ApolloFrame frame;

  public TranscriptMenu(ApolloFrame frame) {
    super("Annotation");
    this.frame = frame;
    addMenuListener(new TranscriptMenuListener());
    // clear out menu items on region change (mem leak)
    frame.getController().addListener(new DataLoadListener() {
	public boolean handleDataLoadEvent(DataLoadEvent e) {
	  removeAll();
	  return true;
	} } );
  }

  private class TranscriptMenuListener implements MenuListener {
    public void menuCanceled(MenuEvent e) {}

    public void menuDeselected(MenuEvent e) {}

    public void menuSelected(MenuEvent e) {
      menuInit();
    }
  }

  private class FocusMenuListener implements ActionListener {
    SeqFeatureI feature;

    public FocusMenuListener(SeqFeatureI feature) {
      this.feature = feature;
    }

    public void actionPerformed(ActionEvent e) {
      // select and zoom all cur sets (if they have it)
      // If feature has only one child, select just that child
      // (shouldn't really be necessary, but otherwise if you selected a
      // second-tier annot, it wouldn't scroll vertically to it).
      if (feature.getFeatures().size() == 1) {
        feature = (SeqFeatureI)feature.getFeatures().firstElement();
      }
      CurationManager cm = CurationManager.getCurationManager();
      for (int i=0; i< cm.numberOfCurations(); i++) {
        GuiCurationState cs = cm.getCurationState(i);
        //cs.getSelectionManager().select(feature,this);
        //Selection sel      = cs.getSelectionManager().getSelection();

        FeatureList fl = new FeatureList();
        fl.addFeature(feature);

        Selection sel = new Selection(fl,this);
        
        Selection subSel   = sel.getSelectionDescendedFromModel(cs.getCurationSet().getAnnots());
        subSel.add(sel.getSelectionDescendedFromModel(cs.getCurationSet().getResults()));
        if (subSel.size() > 0) {
          cs.getSelectionManager().select(subSel,true,this);
          cs.getSZAP().zoomToSelectionWithWindow(Config.getGeneWindow());
        }
         
      }
    }
  }

  /** For now just getting annots from active cur set */
  public void menuInit() {
    removeAll();
    CurationSet cs = 
      CurationManager.getCurationManager().getActiveCurState().getCurationSet();
    Vector features 
      = SeqFeatureUtil.getFeaturesOfClass(cs.getAnnots(),AnnotatedFeature.class,false);
    int maxMenuLength = 25;  // somewhat arbitrary

    if (features.size() > 0) {
      PropertyScheme scheme = Config.getPropertyScheme();
      features = SeqFeatureUtil.sortFeaturesAlphabetically(features);

      JMenu currentMenu = this;
      JMenu submenu;
      for(int i=0; i < features.size(); i++) {
        SeqFeatureI feature = (SeqFeatureI) features.elementAt(i);
        String name = feature.getName();

        JMenuItem menuItem = new JMenuItem(name + " " +
                                           (feature.getStrand() != -1 ?
                                            "(+)" : "(-)"));
        if (feature.getFeatureType().equalsIgnoreCase ("gene")) {
          String pep_status = feature.getProperty ("sp_status");
          menuItem.setFont (Config.getPeptideStatus(pep_status).getFont());
	  //	  System.out.println("For " + feature + " using font " + Config.getPeptideStatus(pep_status).getFont() + " for pep_status " + pep_status); // DEL 
        } else {
          menuItem.setFont (Config.getDefaultFeatureLabelFont());
        }
        if (feature instanceof AnnotatedFeatureI) {
          AnnotatedFeatureI annot = (AnnotatedFeatureI)feature;
          menuItem.setBackground(Config.getAnnotationColor(annot));
        }
        else {
          menuItem.setBackground(scheme.getFeatureProperty(feature.getFeatureType()).getColour());
        }

        menuItem.addActionListener(new FocusMenuListener(feature));
	if (i > 0 && i % maxMenuLength == 0) {
	  submenu = new JMenu("More...");
	  currentMenu.add(submenu);
	  currentMenu = submenu;
	}
        currentMenu.add(menuItem);
      }
    } else {
      JMenuItem menuItem = new JMenuItem("<no annotations>");
      menuItem.setEnabled(false);
      add(menuItem);
    }
  }
}

//   private class FeatureComparator implements Comparator {
// (moved to util/SeqFeatureUtil.java)
