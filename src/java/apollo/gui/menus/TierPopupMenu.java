/** Right-mouse popup menu for *results* (not annotations) 
 *  (This should be renamed ResultPopupMenu) */

package apollo.gui.menus;

import apollo.editor.AnnotationEditor;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.config.DisplayPrefsI;
import apollo.gui.*;
import apollo.gui.genomemap.ApolloPanelI;
import apollo.gui.genomemap.AnnotationView;
import apollo.gui.genomemap.FeatureView;
import apollo.gui.genomemap.FeatureTierManager;
import apollo.gui.genomemap.ResultView;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.synteny.CurationManager;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.detailviewers.blixem.*;
import apollo.gui.detailviewers.PropertyDisplay;
import apollo.util.*;
import apollo.datamodel.*;
import apollo.dataadapter.*;
import apollo.seq.io.FastaFile;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.*;

public class TierPopupMenu extends JPopupMenu implements ActionListener {

  // -----------------------------------------------------------------------
  // Static/class variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TierPopupMenu.class);
  private static String no_tag = "None";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  ResultView resultView;
  ApolloPanelI ap;
  JMenuItem   collapseTier;
  JMenuItem   expandTier;
  JMenuItem   showLabel;
  JMenuItem   hideLabel;
  JMenuItem   hideTier;
  JMenuItem   changeColor;
  JMenuItem   typeSettings;
  AnnotationEditor editor = null;
  Selection   selection;

  JMenuItem dnaBlixem;
  JMenuItem protBlixem;
  JMenuItem getData;
  JMenuItem set5Prime;
  JMenuItem set3Prime;
  JMenuItem setEnds;
  JMenuItem createGeneTrans;
  TypesMenu typesMenu;
  //  JMenuItem createOverlapping;  // No longer used
  JMenuItem addTranscript;
  JMenuItem flipResult;
  JMenuItem sequence;
  JMenuItem props;
  private JMenuItem loadSyntenyLink;
  JMenuItem cancel;
  Hashtable resultTags = new Hashtable();
  private AlignMenuItems jalviewMenus;
  Point pos;

  JMenuItem preferences;
  
  ButtonGroup tag_group;

  private class CopyAction implements ActionListener {
    JTextArea area;

    public CopyAction(JTextArea area) {
      this.area = area;
    }

    public void actionPerformed(ActionEvent e) {
      ClipboardUtil.copyTextToClipboard(area.getSelectedText());
    }
  }

  public TierPopupMenu(ApolloPanelI ap, 
                       ResultView resultView,
                       Selection selection,
                       Point pos) {
    super("Tier operations");
    this.ap   = ap;
    this.resultView = resultView;
    this.pos  = pos;
    this.selection = selection;

    AnnotationView av = resultView.getAnnotationView();
    if (av != null) {
      editor = av.getAnnotationEditor();
      FeatureSetI avTopModel = av.getGeneHolder();
      // not sure what distinction is between "cursor" and annots
      Selection cursorSel = 
	selection.getSelectionDescendedFromModel(avTopModel,true);
      
      editor.setSelections(av,
			   selection,
			   // problematic, leaves out external selected annots
			   cursorSel.getSelectedVector(),
			   av.getTransform().toUser(pos).x,
			   av.getStrand());
    } else {
      logger.warn("getAnnotationView returned null in TierPopupMenu");
    }

    menuInit();
  }

  public TierPopupMenu(ApolloPanelI ap, ResultView resultView, Point pos) {
    this(ap,resultView,null,pos);
  }

  // For now outputting sequence can only deal with 1 feature
  private boolean sequenceAllowed() {
    return true; //selection.size() == 1; - can handle multiselect now
  }

  
  private ApolloFrame getApolloFrame() {
    return ApolloFrame.getApolloFrame();
  }
//     Window  win = SwingUtilities.windowForComponent((JComponent) ap);
//     if (win instanceof ApolloFrame) 
//       return(ApolloFrame)win;
//     // if its not an apollo frame we got problems
//     else
//       throw new RuntimeException("window is not apollo frame");
//}
     
  public void menuInit() {
    dnaBlixem       = new JMenuItem("Blixem on DNA hits");
    protBlixem      = new JMenuItem("Blixem on protein hits");
    changeColor     = new JMenuItem("Change color of this feature type");
    typeSettings    = new JMenuItem("Settings for this feature type");
    collapseTier    = new JMenuItem("Collapse tier");
    expandTier      = new JMenuItem("Expand tier");
    showLabel       = new JMenuItem("Show Label");
    hideLabel       = new JMenuItem("Hide Label");
    hideTier        = new JMenuItem("Hide tier");
    getData         = new JMenuItem("Get info about this feature via Web");
    loadSyntenyLink = new JMenuItem("Bring up link as other species in synteny");
    set5Prime       = new JMenuItem("Set as 5' end");
    set3Prime       = new JMenuItem("Set as 3' end");
    setEnds         = new JMenuItem("Set both ends");
    typesMenu       = new TypesMenu(editor);
    // This menu option is a synonym for dragging a result down--if there's an
    // overlapping gene, it adds this result as a transcript of that gene;
    // otherwise, it creates a new annotation of the type preferred by the
    // selected result.
    createGeneTrans = new JMenuItem("Add as gene transcript");
    // NO LONGER USED--replaced by TypesMenu
    //    createOverlapping  = new JMenuItem("Create new overlapping annotation");
    addTranscript   = new JMenuItem("Add as new transcript to selected gene");
    flipResult      = new JMenuItem("Move to other strand");
    sequence	    = new JMenuItem("Sequence...");
    props	    = new JMenuItem("Print this feature's properties");
    cancel          = new JMenuItem("Close menu");

    preferences = new JMenuItem("Preferences");
    
    add(sequence);
    add(getData);
    add(props);
    props.setEnabled(true);
    addSeparator();

    // check if adapter is a synteny adapter? adapter.isMultiSpecies?
    if (Config.getStyle().addSyntenyResultMenuItem()) {
      add(loadSyntenyLink);
      loadSyntenyLink.setEnabled(enableSyntenyLink());
    }

    if (Config.getBlixemLocation() != null && apollo.util.IOUtil.isUnix()) {
      add(dnaBlixem);
      add(protBlixem);
    }

    FeatureSetI annotTop = resultView.getAnnotationView().getTopModel();
    FeatureSetI resultTop = resultView.getTopModel();
    jalviewMenus = 
      new AlignMenuItems(annotTop,resultTop,selection,ap.getController());
    add(jalviewMenus.getAlignSelectionMenuItem());
    add(jalviewMenus.getAlignRegionMenuItem());
    addSeparator();

    if (apollo.config.Config.isEditingEnabled()) {
      add(set5Prime);
      add(set3Prime);
      add(setEnds);
      addSeparator();
      add(typesMenu);
      add(createGeneTrans);
//      add(createOverlapping);
      add(addTranscript);
      add(flipResult);
      if (selection.size() == 1)
        addTagItems(selection.getSelectedData(0));
      addSeparator();
    }

    add(preferences);
    add(changeColor);
    add(typeSettings);
    add(collapseTier);
    add(expandTier);
    add(showLabel);
    add(hideLabel);
    add(hideTier);

    if (editor == null) {
      set5Prime.setEnabled(false);
      set3Prime.setEnabled(false);
      setEnds.setEnabled(false);
      typesMenu.setEnabled(false);
      createGeneTrans.setEnabled(false);
      //      createOverlapping.setEnabled(false);
      addTranscript.setEnabled(false);
      flipResult.setEnabled(false);
      preferences.setEnabled(false);
      changeColor.setEnabled(false);
      typeSettings.setEnabled(false);
      collapseTier.setEnabled(false);
      expandTier.setEnabled(false);
      showLabel.setEnabled(false);
      hideLabel.setEnabled(false);
      hideTier.setEnabled(false);
    } else {
      set5Prime.setEnabled(editor.setExonTerminusAllowed());
      set3Prime.setEnabled(editor.setExonTerminusAllowed());
      setEnds.setEnabled(editor.setExonTerminusAllowed());
      typesMenu.setEnabled(editor.addGeneOrTranscriptAllowed());
      createGeneTrans.setEnabled(editor.addGeneOrTranscriptAllowed());
      //      createOverlapping.setEnabled(editor.addOverlappingAllowed());
      addTranscript.setEnabled(editor.addTranscriptAllowed());
      flipResult.setEnabled(editor.resultIsSelected());
      preferences.setEnabled(editor.resultIsSelected());
      changeColor.setEnabled(editor.resultIsSelected());
      typeSettings.setEnabled(editor.resultIsSelected());
      collapseTier.setEnabled(editor.resultIsSelected());
      expandTier.setEnabled(editor.resultIsSelected());
      showLabel.setEnabled(editor.resultIsSelected());
      hideLabel.setEnabled(editor.resultIsSelected());
      hideTier.setEnabled(editor.resultIsSelected());
    }

    // Enable menu item that says "Get info about this feature via Web"
    // I'd like to enable this only if ONE result was selected, but
    // for some reason there often seem to be two copies of the same result
    // in the selection, so for now, enabling if one OR MORE results 
    // are selected.
    getData.setEnabled(selection.size() >= 1);

    // Add "Sequence" menu item
    boolean have_seq = false;
    if (selection.size() > 0) {
      SeqFeatureI firstFeat = selection.getFeature(0);
      SequenceI seq = firstFeat.getRefSequence();
      // Make sure we have sequence
      have_seq = (seq != null && (seq.isLazy() || seq.getResidues() != null));
    }
    sequence.setEnabled(have_seq);

    // Add menu item for turning on hidden tier(s)
    add(new ShowMenu(resultView,pos));

    addSeparator();
    add(cancel);

    dnaBlixem.addActionListener(this);
    protBlixem.addActionListener(this);
    set5Prime.addActionListener(this);
    set3Prime.addActionListener(this);
    setEnds.addActionListener(this);
    createGeneTrans.addActionListener(this);
    //    createOverlapping.addActionListener(this);
    addTranscript.addActionListener(this);
    flipResult.addActionListener(this);
    getData.addActionListener(this);
    props.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          new PropertyDisplay(selection);
        } } );
    sequence.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          new SeqExport(selection, ap.getController());
        } } );
    loadSyntenyLink.addActionListener(this);
    cancel.addActionListener(this);

    collapseTier.addActionListener(this);
    expandTier  .addActionListener(this);
    showLabel   .addActionListener(this);
    hideLabel   .addActionListener(this);
    hideTier    .addActionListener(this);
    preferences.addActionListener(this);
    changeColor.addActionListener(this);
    typeSettings.addActionListener(this);
    collapseTier.setMnemonic('O');
    expandTier  .setMnemonic('P');
    hideTier    .setMnemonic('H');

    // Shortcuts don't work for popup menus, as far as Steve and Nomi know.
  }
  
  private Vector getFeatureSets(Vector exons) {
    Hashtable out = new Hashtable();
    Vector v = new Vector();
    if (exons != null) {
      for(int i=0; i < exons.size(); i++) {
        SeqFeatureI feature = (SeqFeatureI) exons.elementAt(i);
        feature = getFeatureSet(feature);
        if (feature != null && feature.getId() != null)
          out.put(feature.getId(), feature);
      }
      Enumeration e = out.elements();
      while (e.hasMoreElements())
        v.addElement(e.nextElement());
    }
    return v;
  }

  private FeatureSetI getFeatureSet(SeqFeatureI feature) {
    if (feature == null)
      return null;
    else if (feature instanceof DrawableSeqFeature)
      return getFeatureSet(((DrawableSeqFeature) feature).getFeature());
    else if (feature.canHaveChildren())
      return (FeatureSetI) feature;
    else
      return getFeatureSet(feature.getRefFeature());
  }

  // could have action listener inner classes for this
  public void actionPerformed(ActionEvent e) {
    FeatureTierManager tm = (FeatureTierManager) resultView.getTierManager();

    if (e.getSource() == dnaBlixem || e.getSource() == protBlixem) {
      StrandedZoomableApolloPanel szap = ap.getStrandedZoomableApolloPanel();

      Vector types = getUniqueListOfAllTypesUsedInSelection();

      int centre = getCentreOfSelection();
      int resType=((e.getSource() == dnaBlixem) ? BlixemRunner.DNA : 
                   BlixemRunner.PROTEIN);

      logger.info("running blixem");
      // Note: types isn't used for anything yet.
      BlixemRunner br = new BlixemRunner(szap.getCurationSet(), 
                                         types, 
                                         centre, 
                                         resType);
      br.run();
    } else if (e.getSource() == set5Prime) {
      editor.setAs5Prime();
    } else if (e.getSource() == getData) {
      String url = makeURLForFeature();
      if (url != null) {
        logger.debug("loading web page " + url);
        HTMLUtil.loadIntoBrowser(url);
      }
     } else if (e.getSource() == set3Prime) {
      editor.setAs3Prime();
    } else if (e.getSource() == setEnds) {
      editor.setAsBothEnds();
    } else if (e.getSource() == createGeneTrans) {
      editor.addGeneOrTranscript();
//     } else if (e.getSource() == createOverlapping) {
//       editor.addGeneOrTranscript(true);
    } else if (e.getSource() == addTranscript) {
      editor.addTranscript();
    } else if (e.getSource() == flipResult) {
      editor.flipResult();
    } else if (resultTags.get (e.getSource()) != null) {
      setTag ((JRadioButtonMenuItem) e.getSource());
    } else if (e.getSource() == loadSyntenyLink) {
      getApolloFrame().loadSyntenyLink(selection.getSelectedData(0));
    } else if (e.getSource() == collapseTier) {
      tm.collapseTier(resultView, ap);
    } else if (e.getSource() == expandTier) {
      tm.expandTier(resultView, ap);
    } else if (e.getSource() == showLabel) {
      tm.showLabelTier(resultView, ap);
    } else if (e.getSource() == hideLabel) {
      tm.hideLabelTier(resultView, ap);
    } else if (e.getSource() == hideTier) {
      tm.hideTier(resultView, ap);
      ap.clearSelection();
    } else if (e.getSource() == changeColor) {
      tm.changeTypeColor(resultView, selection);
    } else if (e.getSource() == typeSettings) {
      tm.editTypeSettings(resultView, selection);
    }
    else if (e.getSource() == preferences) {
      PreferenceWindow.getInstance(selection).setVisible(true);
    }

    // Else they hit the cancel button

    /*
    resultView.setInvalidity(true);
    resultView.getComponent().repaint(resultView.getBounds().x,
                                      resultView.getBounds().y,
                                      resultView.getBounds().width,
                                      resultView.getBounds().height);

     // Why do we need to repaint annotation panel?
     // Good question but we do seem to need to.
     AnnotationView av = resultView.getAnnotationView();
     if (av != null) {
       av.setInvalidity(true);
       av.getComponent().repaint(av.getBounds().x,
                                 av.getBounds().y,
                                 av.getBounds().width,
                                 av.getBounds().height);
    }
    */
    StrandedZoomableApolloPanel szap = CurationManager.getActiveCurationState().getSZAP();  
        szap.setViewColours();
        szap.setAnnotations(szap.getCurationSet());
        szap.setFeatureSet(szap.getCurationSet());
        szap.setAnnotationViewsVisible(Config.getStyle().getShowAnnotations());
        szap.setResultViewsVisible(Config.getStyle().getShowResults());
  }
  
  /** Returns a vector with a unique list of all types being used in 
      selection in resultView. */
  private Vector getUniqueListOfAllTypesUsedInSelection() {
    Vector v = new Vector();
    v.addAll(ap.getSelection().getSelectedVisualTypes()); // strand?
    return v;
  }

  public int getCentreOfSelection() {
    int min = -1;
    int max = -1;
    if (resultView instanceof FeatureView) { 
      FeatureView fv = (FeatureView)resultView;

      //Vector f = fv.getViewSelection(ap.getSelection()).getSelectedData();
      Selection s = fv.getViewSelection(ap.getSelection());

      for (int i=0; i < s.size(); i++) {
        SeqFeatureI sf = s.getFeature(i);

        if (sf.getLow() < min || min == -1) {
          min = sf.getLow();
        }
        if (sf.getHigh() > max || max == -1) {
          max = sf.getHigh();
        }
      }
    }
    return ((max-min) + min);
  }

  // Add ResultTag menu items (defined in style file) to tier menu for
  // results of appropriate type.
  // e.g. ResultTag "Fly EST:Select for full-length sequencing."
  private void addTagItems (SeqFeatureI sf) {
    if (sf instanceof FeaturePair) {  // resultspan--look at parent
      sf = (SeqFeatureI) sf.getRefFeature();
    }
    resultTags.clear();
    Hashtable tags = (Config.getStyle()).getResultTags();
    Enumeration e = tags.keys();
    while (e.hasMoreElements()) {
      String result_type = (String) e.nextElement();
      FeatureProperty fp
        = Config.getPropertyScheme().getFeatureProperty(sf.getFeatureType());
      // Add this result tag to menu if selected feature is the same type as
      // one of the types in the result tags hash.
      if (fp.getDisplayType().equals (result_type)) {
        Vector result_tags = (Vector) tags.get(result_type);
        if ( ! result_tags.contains (no_tag) )
          result_tags.addElement (no_tag);
        String current_tag = sf.getProperty ("tag");
        if (current_tag == null || current_tag.equals ("")) {
          current_tag = no_tag;
        }

        //        addSeparator();
        JMenu tag_menu = new JMenu("Change tag from \"" + current_tag + "\"");
        tag_group = new ButtonGroup();

        for (int i = 0; i < result_tags.size(); i++) {
          String text = (String) result_tags.elementAt (i);
          JRadioButtonMenuItem tag = new JRadioButtonMenuItem(text);
          tag.setEnabled(true);
          tag.addActionListener(this);
          tag_menu.add(tag);
          tag_group.add (tag);
          tag.setSelected (text.equals(current_tag));
          resultTags.put (tag, sf);
        }
        add (tag_menu);
      }
    }
  }

  private void setTag (JRadioButtonMenuItem item) {
    // Why are we getting sf from the hash?  Isn't it just the current selection?
    //    SeqFeatureI sf = (SeqFeatureI) resultTags.get (item);
    // !! Should we iterate through all the selected items rather than just
    // using the first one?
    SeqFeatureI sf = selection.getSelectedData(0);
    SeqFeatureI orig_sf = sf;
    if (orig_sf instanceof FeaturePair) {  // resultspan--look at parent
      sf = (SeqFeatureI) orig_sf.getRefFeature();
    }
    String text = item.getText();
    if (!text.equals (no_tag)) {
      sf.replaceProperty ("tag", text);
      //      System.out.println("Added tag " + text + " to " + sf.getName()); // DEL
      // If data was from Chado, may need to propagate tag to siblings
      // (other result spans of same parent).
      // This is unnecessary but harmless (because we don't change the tag
      // unless it was already set) for GAME data
      // If sf is orig_sf's parent, spans will include orig_sf itself.
      Vector spans = sf.getFeatures();  
      for (int i = 0; i < spans.size (); i++) {
        SeqFeatureI span = (SeqFeatureI) spans.elementAt (i);

        if (span.getProperty("tag") != null && !span.getProperty("tag").equals("")) {
          //          System.out.println("Replacing tag " + span.getProperty("tag") + " on that span with " + text); // DEL
          span.replaceProperty ("tag", text);
        }
        // In Chado, may need to propagate tag change down to query & subject
        SeqFeatureI query = ((FeaturePair)span).getQueryFeature();
        SeqFeatureI subject = ((FeaturePair)span).getHitFeature();
        if (query.getProperty("tag") != null && !query.getProperty("tag").equals(""))
          query.replaceProperty ("tag", text);
        //        if (subject.getProperty("tag") != null && !subject.getProperty("tag").equals("")) {
        // Do it all the time--doesn't affect GAME output; fixes ChadoXML output.
        // System.out.println("Replacing tag " + span.getProperty("tag") + " with " + text + " on subject span " + subject); // DEL
        subject.replaceProperty ("tag", text);
      }

      // Force redrawing to make pink rectangle more visible
      if (resultView instanceof ResultView) {
        ResultView rv = (ResultView)resultView;
        rv.setInvalidity(true);
        rv.getComponent().repaint(rv.getBounds().x,
                                  rv.getBounds().y,
                                  rv.getBounds().width,
                                  rv.getBounds().height*2);
      }
    }
    // text == no_tag--remove tag
    else {
      sf.removeProperty ("tag");
    }
    //    item.setSelected(true);
  }

  private String makeURLForFeature() {
    // Figure out URL for getting more information about this feature.
    // If more than one feature was selected, use the LAST one.
    DisplayPrefsI displayPrefs = Config.getDisplayPrefs();
    return displayPrefs.generateURL (selection.last());
  }

  private boolean isSyntenyLinked(SeqFeatureI sf) {
    return Config.getPropertyScheme().getFeatureProperty(sf.getTopLevelType()).isSyntenyLinked();
  }

  /** return true if selection is single featSet with kids that are synteny linked, 
      or selection is syn linked kids of common parent   */
  private boolean enableSyntenyLink() {

    SeqFeatureI sf = selection.getFeature(0);

    if (selection.size() == 1) {
      return isSyntenyLinked(sf);
    }
    
    // Return true if one or more feats of same parent selected and they
    // all are synteny linked (multiple result "exons" of same result "transcript")
    else if (selection.size() >= 1) {
      SeqFeatureI parent = null;
      for (int i=0; i<selection.size(); i++) {
        sf = selection.getSelectedData(i);
        // if doesnt have hit return false
        if (!isSyntenyLinked(sf)) 
          return false;
        if (parent == null)
          parent = sf.getParent(); // get 1st parent
        // return false if children of different parents
        if (parent != sf.getParent())
          return false;
      }
      // passed parent and syn link test - return true
      return true;
    }
    return false; // selection.size() == 0
  }

  /** Clean up dangling references (mem leaks) */
  public void clear() {
    editor = null;
    if (jalviewMenus != null)
      jalviewMenus.clear();
    //featureSets = null;
  }
}
