package apollo.gui.menus;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import apollo.config.Config;
import apollo.config.DisplayPrefsI;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.CurationSet;
import apollo.datamodel.Exon;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.AnnotationEditor;
import apollo.editor.UserName;

import apollo.gui.ApolloFrame;
import apollo.gui.PreferenceWindow;
import apollo.gui.Selection;
import apollo.gui.annotinfo.FeatureEditorDialog;
import apollo.gui.detailviewers.exonviewer.BaseFineEditor;
import apollo.gui.detailviewers.sequencealigner.MultiSequenceAlignerFrame;
import apollo.gui.detailviewers.seqexport.SeqExport;
import apollo.gui.drawable.DrawableAnnotationConstants;

import apollo.analysis.AnalysisGUI;
import apollo.config.Config;
import apollo.gui.*;

import apollo.gui.genomemap.AnnotationView;
import apollo.gui.genomemap.ApolloPanelI;
import apollo.gui.genomemap.FeatureTierManager;
import apollo.gui.synteny.GuiCurationState;
import apollo.util.FeatureList;
import apollo.util.HTMLUtil;

/** Note that this is actually the popup menu you get when you right-click after
 *  selecting an annotation.  The top-level menu that lists all the annotations
 *  in alphabetical order is called (misleadingly) TranscriptMenu. */

public class AnnotationMenu extends JPopupMenu
  implements ActionListener,
  DrawableAnnotationConstants {

  // -----------------------------------------------------------------------
  // Static/class variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AnnotationMenu.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  ApolloPanelI    ap;
  JMenuItem       delete;
  JMenuItem       annotInfo;
  JMenuItem       baseedit;
  JMenuItem       sequenceAligner;
  JMenuItem       sequence;
  JMenuItem       mergeExons;
  JMenuItem       moveExonsToTranscript;
  JMenuItem       mergeTran;
  JMenuItem       splitExon;
  JMenuItem       splitTran;
  JMenuItem       fivePrime;
  JMenuItem       threePrime;
  JMenuItem       orf;
  JMenuItem       setEnds;
  //  JMenuItem       createExon;
  TypesMenu       typesMenu;
  JMenuItem       duplicateTranscript;
  JMenuItem       showGeneReport;
  JMenuItem       assignName;
  JMenuItem       takeOwnership;
  JMenuItem       toggleCompleted;
  PeptideMenu     peptideStatusMenu;
  JMenuItem       collapseTier;
  JMenuItem       expandTier;
  //  JMenuItem       hideTier;
  JMenuItem       changeColor;
  JMenuItem   typeSettings;
  JMenuItem       cancel;
  
  JMenuItem preferences;
  
  JMenuItem analysis;

  AnnotationView  annotView;
  //ApolloPanelI    apolloPanel;
  private GuiCurationState curationState;
  Selection       selection;
  //FeatureList          transcripts = new FeatureList();
  Vector transcripts = new Vector();
  //FeatureList          annots = new FeatureList();
  Vector annots = new Vector();
  /** Selected Annotations in annotView */
  private Vector selectedAnnots;
  /** The feature set that holds all the top level annots(eg genes),
      the top model for the annot view */
  private FeatureSetI holder;

  Point           pos;
  AnnotationEditor editor;

  boolean haveResidues = true;  // False if we don't have actual sequence residues for the current transcript
  boolean haveWholeSeq = true;  // False if we don't have the sequence range for the whole current transcript

  private int strand;

  /** Selection is the selection for the whole ApolloPanel
      (not just the AnnotView)*/
  public AnnotationMenu(GuiCurationState curationState,
                        ApolloPanelI apolloPanel,
                        AnnotationView annotView,
                        Selection selection,
                        Point pos) {
    super("Annotation");
    this.ap = apolloPanel;
    this.curationState = curationState;
    this.annotView      = annotView;
    this.selection = selection;
    this.pos       = pos;
    this.strand = annotView.getStrand();

    editor = annotView.getAnnotationEditor();

    editor.setSelections(annotView,
                         selection,
                         annotView.findFeatures(pos).toVector(),
                         annotView.getTransform().toUser(pos).x,
                         annotView.getStrand());
    menuInit();
  }

  public void menuInit() {
    // this only get transcripts for the strand the user clicked on
    // I dont believe theres any cross strand editing - but does that mean
    // it should ignore the fact that there are things selected on the other
    // strand
    // populates transcripts & annots Vectors
    findTranscriptsAndAnnots(selection);
    //annots       = findAnnots      (transcripts);

    holder = annotView.getTopModel();
    selectedAnnots
      = selection.getSelectionDescendedFromModel(holder).getSelectedVector();

    //    System.out.println("Found " + transcripts.size() + " Transcripts");
    //    System.out.println("Found " + genes.size()       + " genes");

    if (Config.isEditingEnabled())
      annotInfo             = new JMenuItem("Annotation info editor...");
    else
      annotInfo             = new JMenuItem("Annotation info...");
    baseedit         = new JMenuItem("Exon detail editor...");
    sequenceAligner  = new JMenuItem("Sequence Aligner...");
    sequence        = new JMenuItem("Sequence...");
    delete           = new JMenuItem("Delete selection");
    mergeExons            = new JMenuItem("Merge exons");
    moveExonsToTranscript = new JMenuItem("Move exon(s) to transcript");
    mergeTran        = new JMenuItem("Merge transcripts");
    splitExon            = new JMenuItem("Split exon");
    splitTran        = new JMenuItem("Split transcript");
    takeOwnership    = new JMenuItem(getOwner(getFirstAnnot(selection)));
    toggleCompleted  = new JMenuItem(getStatus(transcripts));
    peptideStatusMenu = new PeptideMenu(curationState);
    fivePrime        = new JMenuItem("Set as 5' end");
    threePrime       = new JMenuItem("Set as 3' end");
    setEnds          = new JMenuItem("Set both ends");
    orf              = new JMenuItem("Calculate longest ORF");
    duplicateTranscript = new JMenuItem("Duplicate transcript");
    showGeneReport   = new JMenuItem("Get info about this feature via Web");
    //    createExon       = new JMenuItem("Create exon");
    typesMenu        = new TypesMenu(editor);
    assignName       = new JMenuItem("Assign name from evidence");  // not currently used

    changeColor     = new JMenuItem("Change color of this feature type");
    typeSettings    = new JMenuItem("Settings for this feature type");

    collapseTier    = new JMenuItem("Collapse tier");
    expandTier      = new JMenuItem("Expand tier");
    // Not adding "hide tier" because then I'd also have to add the "Show tier"
    // menu item, and there are already so many things in this menu.
    //    hideTier        = new JMenuItem("Hide tier");

    //    if (selection.size() == 1) {
    //      delete    = new JMenuItem("Delete selection");
    //    }

    cancel = new JMenuItem("Close menu");

    preferences = new JMenuItem("Preferences");
    
    analysis = new JMenuItem("Analyze region");
    
    add(sequence);
    haveResidues = false;
    //if (transcripts.size() > 0) {
    if (selectedAnnots.size() > 0) {
      // Use LAST selected transcript
      SeqFeatureI lastFeat
        = (SeqFeatureI) selectedAnnots.elementAt(selectedAnnots.size() - 1);
      // My way SequenceI seq = lastTranscript.getRefSequence();
      // Make sure we have sequence
      // Note that this will force the sequence to load!  Not good?
      // My way haveResidues = (seq != null && (seq instanceof LazySequenceI || seq.getResidues() != null));
      // why just last transcript? the last trans may be whole
      // but the rest not. especially for multi select
      haveWholeSeq = lastFeat.isContainedByRefSeq();
      SequenceI seq = lastFeat.getRefSequence();
      if (seq instanceof GAMESequence) // --> if !seq.isLazy()
        haveResidues = ((GAMESequence)seq).hasSequence();
      else
        haveResidues = haveWholeSeq;
    }
    // If we don't haveWholeSequence disable seq menu - 12.12.02 MG
    sequence.setEnabled(haveResidues && haveWholeSeq);

    add(showGeneReport);
    // This erroneously enables even if we selected something on OTHER strand, which we can't actually delete
    //    showGeneReport.setEnabled(selection.size() > 0);
    //showGeneReport.setEnabled(transcripts.size() > 0);
    showGeneReport.setEnabled(selectedAnnots.size() > 0);

    add(annotInfo);  // Enable in browser mode, too
    annotInfo.setEnabled(selectedAnnots.size() > 0);
    //annotInfo.setEnabled(transcripts.size() > 0);

    // This is sort of funny, but Thomas Yan wrote a data adapter that put non
    // AnnotatedFeatures in AnnotationView, which caused null pointer. Should
    // probably enforce that only GenericAnnotations go into AnnotationView,
    // but for now check that there is a GenricAnntation in selection
    boolean haveAnnot = getFirstAnnot(selection) != null;

    if (Config.isEditingEnabled()) {
      add(baseedit);
      add(sequenceAligner);
      baseedit.setEnabled (haveResidues && haveWholeSeq && haveAnnot);
      sequenceAligner.setEnabled (haveResidues && haveWholeSeq && haveAnnot);
    }

    addSeparator();

    // Jalview doesn't work on annots yet (but you can see the genomic translation
    // in jalview if you look at results)
//     FeatureSetI resultTop = annotView.getResultView().getTopModel();
//     AlignMenuItems jalviewMenus =
//       new AlignMenuItems(holder, resultTop,selection,curationState.getController());
//     add(jalviewMenus.getAlignSelectionMenuItem());
//     add(jalviewMenus.getAlignRegionMenuItem());
//     addSeparator();

    if (Config.isEditingEnabled()) {
      //      assignName.setEnabled(editor.assignAnnotationNameAllowed());
      add(delete);
      //      delete.setEnabled(selection.size() > 0);  // enabled even if we selected something on OTHER strand, which we can't actually delete
      // If we have an exon, then we have a transcript
      // Tried enabling for size > 0 but that led to problems.
      // Sima has requested multi delte to be reinstated so i did. Nomi what is the
      // problems you speak of - im not seeing any problems - MG
      //delete.setEnabled(transcripts.size() == 1);
      //delete.setEnabled(transcripts.size() > 0);
      if(haveAnnots())
        delete.setEnabled(editor.deleteSelectionAllowed());
      else
        delete.setEnabled(haveAnnots());
	
      add(mergeTran);
      mergeTran.setEnabled(editor.mergeTranscriptsAllowed());
      add(splitTran);
      splitTran.setEnabled(editor.splitTranscriptAllowed());
      add(duplicateTranscript);
      duplicateTranscript.setEnabled(transcripts.size() == 1);

      addSeparator();

      add(mergeExons);
      // mergeExons is for merging exons (in one transcript)
      // editor.mergeExonsAllowed()?
      //mergeExons.setEnabled(transcripts.size() == 1 && annotViewSelection.size() > 1);
      mergeExons.setEnabled(editor.mergeExonsAllowed());

      add(moveExonsToTranscript);
      // for moveExons, want one transcript plus exons from another transcript
      moveExonsToTranscript.setEnabled(editor.moveExonsToTranscriptAllowed());

      add(splitExon);
      splitExon.setEnabled(editor.splitExonAllowed());
      //      add(createExon);
      //      createExon.setEnabled(editor.createAnnotationAllowed());
      add(typesMenu);
      typesMenu.setEnabled(editor.createAnnotationAllowed());

      addSeparator();
      add(orf);
      add(fivePrime);
      add(threePrime);
      add(setEnds);
      orf.setEnabled (transcripts.size() > 0);
      boolean trim_ends = editor.setExonTerminusAllowed();
      fivePrime. setEnabled(trim_ends);
      threePrime.setEnabled(trim_ends);
      setEnds.setEnabled(trim_ends);

      addSeparator();
    }

    if (Config.isEditingEnabled()) {
      if (curationState.getStyle().showEvalOfPeptide() &&  annots.size() == 1) {
        AnnotatedFeatureI g = (AnnotatedFeatureI) annots.elementAt (0);
        if (g.isProteinCodingGene()) {
          peptideStatusMenu.setGene(g);
          add(peptideStatusMenu);
        }
      }

      if (curationState.getStyle().showOwnershipAnnotMenuItem()) {
        add(takeOwnership);
        // Note that checking transcripts.size() == 1 only checks the transcript count
        // on the current strand.  There might be transcripts selected on the other
        // strand, too.  But that's probably ok, since the current strand is whichever
        // your mouse is over, and if you've only selected one transcript there, then
        // there's no ambiguity.
        takeOwnership.setEnabled (selectedAnnots.size() == 1);
      }
      if (curationState.getStyle().showTranscriptFinishedAnnotMenuItem()) {
        add(toggleCompleted);
        toggleCompleted.setEnabled(transcripts.size() > 0);
      }
    }

    changeColor.setEnabled(haveAnnots());//transcripts.size() > 0
    typeSettings.setEnabled(haveAnnots());//transcripts.size() > 0
    collapseTier.setEnabled(haveAnnots());//transcripts.size() > 0
    expandTier.setEnabled(haveAnnots());//transcripts.size() > 0
    //    hideTier.setEnabled(transcripts.size() > 0);

    preferences.setEnabled(haveAnnots());
    analysis.setEnabled(haveAnnots());
    
    addSeparator();
    add(analysis);
    
    addSeparator();
    add(preferences);
    add(changeColor);
    add(typeSettings);

    add(collapseTier);
    add(expandTier);
    //    add(hideTier);
    addSeparator();
    add(cancel);

    assignName.addActionListener(this);
    delete    .addActionListener(this);
    annotInfo      .addActionListener(this);
    baseedit  .addActionListener(this);
    sequenceAligner .addActionListener(this);
    // disable on multiple transcripts?
    sequence.addActionListener(new ActionListener() {
                                  public void actionPerformed(ActionEvent e) {
                                    // getSelData gives vector of SeqFeatureIs
                                    new SeqExport(selection,curationState.getController());
                                  }
                                }
                               );
    splitExon     .addActionListener(this);
    orf       .addActionListener(this);
    fivePrime .addActionListener(this);
    threePrime.addActionListener(this);
    setEnds   .addActionListener(this);
    mergeExons     .addActionListener(this);
    mergeTran .addActionListener(this);
    splitTran .addActionListener(this);
    moveExonsToTranscript.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.moveExonsToTranscript();
        } });
    //    createExon    .addActionListener(this);
    duplicateTranscript.addActionListener(this);
    showGeneReport.addActionListener(this);
    takeOwnership.addActionListener(this);
    toggleCompleted.addActionListener(this);
    cancel.addActionListener(this);

    delete   .setMnemonic('D');
    annotInfo     .setMnemonic('E');
    baseedit .setMnemonic('B');
    sequenceAligner.setMnemonic('A');
    sequence.setMnemonic('T');
    mergeTran.setMnemonic('M');
    splitExon    .setMnemonic('S');

    collapseTier.addActionListener(this);
    expandTier  .addActionListener(this);
    //    hideTier    .addActionListener(this);
    changeColor.addActionListener(this);
    typeSettings.addActionListener(this);
    collapseTier.setMnemonic('O');
    expandTier  .setMnemonic('P');
    preferences.addActionListener(this);
    //    hideTier    .setMnemonic('H');
    // Shortcuts don't work for popup menus, as far as Steve and Nomi know.
    analysis.addActionListener(this);
  }

  private ApolloFrame getApolloFrame() {
    return ApolloFrame.getApolloFrame();
    //returnSwingUtilities.getAncestorOfClass(ApolloFrame.class,apolloPanel);
  }

  public void actionPerformed(ActionEvent e) {
    FeatureTierManager tm = (FeatureTierManager) annotView.getTierManager();

    // If user is trying to edit a feature that doesn't have all of its
    // sequence, change may not get saved in the db, so pop up a warning.
    // Should we just disable in this case?
    if (!haveWholeSeq && (e.getSource() == delete ||
                          e.getSource() == annotInfo ||
                          e.getSource() == baseedit ||
                          e.getSource() == sequenceAligner ||
                          e.getSource() == mergeExons ||
                          e.getSource() == mergeTran ||
                          e.getSource() == splitExon ||
                          e.getSource() == splitTran ||
                          e.getSource() == moveExonsToTranscript ||
                          e.getSource() == takeOwnership ||
                          e.getSource() == toggleCompleted ||
                          e.getSource() == fivePrime ||
                          e.getSource() == threePrime ||
                          e.getSource() == setEnds ||
                          e.getSource() == duplicateTranscript ||
                          e.getSource() == assignName)
        /*|| (e.getSource() == createExon && featureIsWhole...*/ ) {
      // need check for create exon that involves either mouse pos or result feat...
      String m = "WARNING: this transcript extends beyond the current sequence region,\nso any changes you make to it may not get saved in the database.\nEdit it at your own risk.\n";
      if (Config.internalMode())
        JOptionPane.showMessageDialog(null,m);
      else
        logger.warn(m);
    }

    // If transcript is marked "warn on edit", warn user if they select any of the menu options
    // that might change the peptide, and give them a chance to back out.
    if (warnOnEdit() && (e.getSource() == delete ||
                         e.getSource() == baseedit ||
                         e.getSource() == sequenceAligner ||
                         e.getSource() == mergeExons ||
                         e.getSource() == mergeTran ||
                         e.getSource() == splitExon ||
                         e.getSource() == splitTran ||
                         e.getSource() == moveExonsToTranscript ||
                         e.getSource() == fivePrime ||
                         e.getSource() == threePrime ||
                         e.getSource() == setEnds ||
                         e.getSource() == duplicateTranscript)) {
      if (!apollo.main.LoadUtil.areYouSure("Please note: the selected annotation is protected.\nAre you sure you want to edit it?"))
        return;
      else
        logger.warn("User was warned against editing frozen annotation, but did so anyway.");
    }

    if (e.getSource() == delete) {
      editor.deleteSelectedFeatures();
    }

    // AnnotInfo
    else if (e.getSource() == annotInfo) {
      // Use LAST selected transcript
      SeqFeatureI sf
        = (SeqFeatureI) selectedAnnots.elementAt(selectedAnnots.size() - 1);
      FeatureEditorDialog.showTextEditor(sf,curationState);
    }

    else if (e.getSource() == baseedit) {
      //Transcript t = (Transcript) transcripts.elementAt(0);
      // Send off first annotation in sel set, whether its trans,gene,or exon
      // this will allow ede to come up on exon selected, rather than going to
      // the beginning of its transcript (per bev request)
      // Should this be getting the last annot?
      AnnotatedFeatureI gai = getFirstAnnot(selection);
      BaseFineEditor.showBaseEditor(gai,curationState,annotView.getGeneHolder());

      
    } else if (e.getSource() == sequenceAligner) {

      MultiSequenceAlignerFrame
        .makeAligner(curationState, strand, selection, SequenceI.DNA);

    }  else if (e.getSource() == splitExon) {
      editor.splitExon();

    } else if (e.getSource() == mergeExons) {
      editor.mergeExons();

    } else if (e.getSource() == mergeTran) {
      editor.mergeTranscripts();

    } else if (e.getSource() == splitTran) {
      editor.splitTranscript();

//     } else if (e.getSource() == moveExonsToTranscript) {
//       editor.moveExonsToTranscript(); // in anonymous class

    } else if (e.getSource() == assignName) {
      editor.assignAnnotationName();

    } else if (e.getSource() == orf) {
      //      System.out.println("Editor " + editor);
      editor.setLongestORF();
    } else if (e.getSource() == fivePrime) {
      editor.setAs5Prime();
    } else if (e.getSource() == threePrime) {
      editor.setAs3Prime();
    } else if (e.getSource() == setEnds) {
      editor.setAsBothEnds();
      //    } else if (e.getSource() == createExon) {
      //      editor.createAnnotation();
      // Replaced by TypesMenu
    } else if (e.getSource() == duplicateTranscript) {
      // Use LAST selected transcript
      editor.duplicateTranscript((Transcript)transcripts.elementAt(transcripts.size()-1));
    } else if (e.getSource() == toggleCompleted) {
      toggleCompleted.setText (setStatus (transcripts));
    } else if (e.getSource() == takeOwnership) {
      takeOwnership.setText (setOwner(getFirstAnnot(selection)));
    } else if (e.getSource() == showGeneReport) {
      DisplayPrefsI displayPrefs = Config.getDisplayPrefs();
      if (selectedAnnots.size() > 0) {
        // Use the LAST transcript selected, not the first
        SeqFeatureI sf
          = (SeqFeatureI) selectedAnnots.elementAt(selectedAnnots.size() - 1);
        AnnotatedFeatureI topAnnot = getGeneLevelAnnot(sf);
        String url = displayPrefs.generateURL (topAnnot);
        if (url != null) {
          logger.debug("loading web page " + url);
          HTMLUtil.loadIntoBrowser(url);
        }
        else
          JOptionPane.showMessageDialog(null,"Couldn't make URL for annotation " + topAnnot.getId());
      }
    } else if (e.getSource() == collapseTier) {
      tm.collapseTier(annotView, ap);
    } else if (e.getSource() == expandTier) {
      tm.expandTier(annotView, ap);
    } else if (e.getSource() == changeColor) {
      tm.changeTypeColor(annotView, selection);
    } else if (e.getSource() == typeSettings) {
      tm.editTypeSettings(annotView, selection);
    }
    else if (e.getSource() == preferences) {
      PreferenceWindow.getInstance(selection).setVisible(true);
    }
    else if (e.getSource() == analysis) {
      FeatureList fl = selection.getSelectedData();
      new AnalysisGUI(fl.getFeature(0).getLow(), fl.getFeature(fl.size() - 1).getHigh(), fl.getFeature(0).getStrand(), fl);
    }


    // Else they hit the cancel button, but in that case we don't need to do anything.

    annotView.setInvalidity(true);
    annotView.getComponent().repaint(annotView.getBounds().x,
                                     annotView.getBounds().y,
                                     annotView.getBounds().width,
                                     annotView.getBounds().height);
  }

  /** Return ancestor of sf that is a Gene or analogous to gene */
  private AnnotatedFeatureI getGeneLevelAnnot(SeqFeatureI sf) {
    while (sf != null && !holder.getFeatures().contains(sf))
      sf = sf.getParent();
    if (sf!=null && sf.hasAnnotatedFeature())
      return sf.getAnnotatedFeature();
    else
      return null;
  }

  // Not used
//   private String repeatChar(char c, int n) {
//     StringBuffer out = new StringBuffer();
//     for(int i=0; i < n; i++)
//       out.append(c);
//     return out.toString();
//   }

  /**
   * Goes through the selection looking for Genes and Exons. If it finds exons it
   * adds its refFeature which is a Transcript to the vector, if Gene then adds the
   * genes transcripts, if Transcript add the Transcript.
   * Looking for genes and transcripts added - used to assume just exons in selection.
   Bug - it gets transcripts from all strands - i think it should only get transcripts
   from the strand of the annot view that the menu is for - this makes it so
   you can pull up an ede on the forward strand with transcript on the reverse strand
   - fix this!
   */
  private void findTranscriptsAndAnnots(Selection selection) {
    //Vector transcripts = new Vector();
    for (int i=0; i < selection.size() ;i++) {
      SeqFeatureI feat = selection.getSelectedData(i);
      // skip feats on other strand
      if (feat.getStrand() == strand) {
        if (feat.isExon()) {
          //ExonI exon = (ExonI)feat;
          addTranscript(feat.getRefFeature());
          //if (!(transcripts.contains(t)))  transcripts.addElement(t);
        } else if (feat.isTranscript()) {
          addTranscript(feat);
        } else if (feat.hasAnnotatedFeature()) {
          //Vector trans = feat.getFeatures();
          for (int j=0; j<feat.getNumberOfChildren(); j++) {
            addTranscript(feat.getFeatureAt(j));
          }
          addAnnot(feat); // this will get 1 level annots (have no trans)
        }
      }
    }
    //return transcripts;
  }

  private void addTranscript(SeqFeatureI trans) {
    if (!transcripts.contains(trans))
      transcripts.add(trans);
    addAnnot(trans.getRefFeature());
  }

  private void addAnnot(SeqFeatureI annot) {
    if (!annots.contains(annot))
      annots.add(annot);
  }

  private boolean haveAnnots() { return !annots.isEmpty(); }

  /** Returns first AnnotatedFeatureI in Selection on strand clicked on,
      null if there is none.
      Exon, Transcript, and Gene are all AnnotatedFeatureI's */
  private AnnotatedFeatureI getFirstAnnot(Selection selection) {
    for (int i=0; i < selection.size() ;i++) {
      SeqFeatureI feat = selection.getSelectedData(i);
      if (feat instanceof AnnotatedFeatureI && feat.getStrand() == strand) {
        // If this is an exon, get the parent transcript
        if (feat instanceof Exon)
          feat = feat.getRefFeature();
        return (AnnotatedFeatureI)feat;
      }
    }
    return null;
  }

  public Vector findAnnots(Vector tran) {
    Vector genes = new Vector();
    for (int i=0; i < tran.size(); i++ ) {
      Transcript t = (Transcript)tran.elementAt(i);

      if (!(genes.contains(t.getRefFeature()))) {
        genes.addElement(t.getRefFeature());
      }
    }
    return genes;
  }

  private String setStatus (Vector transcripts) {
    // Use LAST selected transcript
    Transcript t = (Transcript) transcripts.elementAt(transcripts.size()-1);
    editor.setTranscriptStatus (t);
    return getStatus (transcripts);
  }

  private String getStatus (Vector transcripts) {
    String text;

    if (transcripts.size() == 1) {
      // Use LAST selected transcript
      Transcript t = (Transcript) transcripts.elementAt(transcripts.size()-1);
      boolean status = (t.getProperty("status") != null &&
                        t.getProperty("status").equals("all done"));
      if (status) {
        text = "Set " + t.getName() + " unfinished";
      } else {
        text = "Set " + t.getName() + " completed";
      }
    } else {
      text = "Transcript status";
    }
    return text;
  }

  /** Passed a single AnnotatedFeatureI (a transcript or one-level annot) */
  private String setOwner (AnnotatedFeatureI annot) {
    if (annot != null) {
      String owner = annot.getOwner();//Config.getProjectName(t.getOwner());
      // The methods in AnnotationEditor for own/disown require a vector
      Vector annotVector = new Vector();
      annotVector.add(annot);
      if ((owner == null) ||
          ( ! owner.equals (UserName.getUserName()) ) )
        editor.takeOwnership(annotVector);
      else
        editor.disown(annotVector);
    }
    return getOwner (annot);
  }

  /** Passed a single AnnotatedFeatureI (a transcript or one-level annot).
   *  If annot is null, the string returned is "Take ownership". */
  private String getOwner (AnnotatedFeatureI annot) {
    String text;

    if (annot != null) {
      String owner = annot.getOwner();//Config.getProjectName(t.getOwner());
      String ownersFullName = Config.getFullNameForUser(owner);
      String user = UserName.getUserName();
      String usersFullName = Config.getUsersFullName();
      if (owner == null)
        text = "Set owner to " + usersFullName;
      else if (owner.equals(user))
        text = "Disown by " + ownersFullName;
      else
        text = ("Set owner to " + usersFullName+
                " (taking from " + ownersFullName + ")");
    } else {
      text = "Take ownership"; // should be disabled
    }
    return text;
  }

  /** Some new properties in ChadoXML (e.g. mutant_in_strain) "freeze"
      the peptide, so warn the user if they choose any menu options that
      would edit it. */
  private boolean warnOnEdit() {
    if (transcripts == null || selectedAnnots == null || selectedAnnots.size() == 0)
      return false; // nothing selected

//     // This seems to happen with trans-spliced transcripts--not sure why,
//     // but they're frozen anyway.
//     if (transcripts.size() == 0)
//       return true;
    if (transcripts.size() == 0)
      return editor.warnOnEdit(selectedAnnots);

    return editor.warnOnEdit(transcripts);
  }

  /** Clean up dangling references (mem leaks) */
  public void clear() {
    // peptide menu has to remove itself as listener to
    // the controller, otherwise results in mem leak to whole curation set
    if (peptideStatusMenu != null) peptideStatusMenu.clear();
    peptideStatusMenu = null;
    // Setting editor to null actually cleans up from a java mem leak bug
    // in 1.3.1_02-b02 (not sure what version its fixed in) where popup
    // menu doesnt cleanup after itself(if one of the menu items is selected.
    // editor leads to the curation set so setting it to null cuts this
    // mem leak from a big fat curation set.
    // -- 1.4 seems to have the same leak! gee whiz sun get it together!
    editor = null;
    transcripts = null;
    annots = null;
    selectedAnnots = null;
    holder = null; // dongling ref to old cur set
    removeAll(); // remove all menu items
  }

  //  // Moving the mouse across a separator in the menu results in a mouse exited event!
  //  public void mouseExited(MouseEvent evt) {
  //    System.out.println("AM: mouse exited event"); // DEL
  //  }
}
