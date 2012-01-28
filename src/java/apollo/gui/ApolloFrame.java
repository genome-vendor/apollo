/**
 * ApolloFrame is no longer really the main class controlling Apollo--that's now ApolloRunner.
 * But there's still a lot of stuff here.
 *
 * @author    S.M.J.Searle
 * @version   $Header: /cvsroot/gmod/apollo/src/java/apollo/gui/ApolloFrame.java,v 1.199 2007/10/24 16:08:57 searle Exp $
 */

package apollo.gui;

import java.lang.reflect.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;

import apollo.config.Config;
import apollo.main.Apollo;
import apollo.main.Version;
import apollo.datamodel.*;
import apollo.datamodel.seq.LazySequenceI;
import apollo.datamodel.seq.GAMESequence;
import apollo.gui.synteny.CompositeApolloFrame;
import apollo.gui.synteny.GuiCurationState;
import apollo.gui.synteny.CurationManager;
import apollo.gui.synteny.SyntenyPanel;
import apollo.gui.genomemap.StrandedZoomableApolloPanel;
import apollo.gui.genomemap.ApolloPanel;
import apollo.gui.genomemap.AnnotationView;
import apollo.gui.annotinfo.FeatureEditorDialog;
import apollo.gui.detailviewers.exonviewer.BaseFineEditor;
import apollo.gui.menus.*;
import apollo.gui.event.*;
import apollo.gui.evidencepanel.EvidencePanel;
import apollo.gui.evidencepanel.EvidencePanelContainer;
import apollo.gui.evidencepanel.EvidencePanelOrientationManager;
import apollo.dataadapter.*;
import apollo.util.*;
import apollo.gui.UnBorderedSplitPane;

// for test main
import apollo.seq.io.*;

import misc.JIniFile;

import org.apache.log4j.*;

import org.bdgp.io.DataAdapter;
import org.bdgp.io.DataAdapterRegistry;
import org.bdgp.swing.BackgroundImagePanel;

/** CompositeApolloFrame subclass is now the working instantiation of ApolloFrame.
    These should eventually be merged - or done with a has-a */

public abstract class ApolloFrame extends JFrame implements ControlledObjectI,
  LazyLoadListener, DataLoadListener/*,ApolloLoaderI*/ {

  protected final static Logger logger = LogManager.getLogger(ApolloFrame.class);

  static String version = Version.getVersion();

  /**
   * UnBorderedSplitPane to split evp and szap.
   */
  private UnBorderedSplitPane evidenceSzapSplitPane; 

  /** SyntenyPanel holds all the szaps and link panels (rename?) */
  private SyntenyPanel syntenyPanel;


//   /** Constructor with no controller for CompositeApolloFrame for now */
//   protected ApolloFrame() {}

  /** I think eventually CompostieApolloFrame and ApolloFrame should be merged */
  public static CompositeApolloFrame getApolloFrame() {
    return CompositeApolloFrame.getApolloFrame();
  }

  public static JFrame getFrame() {
    return getApolloFrame();
  }

  /**
     constructor for singleton. set controller to master controller.
   */
  protected ApolloFrame() {}


  /** no-op */
  public void setController(Controller c) {}
    //this.controller = c;
    //controller.addListener(this);
    // static singleton access to AnnChLog(ACL) - this needs to be thought out for 
    // multi species case. currently theres many controllers - either all controllers
    // need to be lumped into one, or many ACLs, or ACL deals with many controllers
    // currently synteny not being used for editing
    //AnnotationChangeLog.getAnnotationChangeLog().setController(c);
  // }

  /** ControlledObjectI interface - this is the master controller
      (set by CompositeApolloFrame) */
  public Controller getController() {
    //return controller;
    return Controller.getMasterController();
  }

  public Object getControllerWindow() {
    return SwingMissingUtil.getWindowAncestor(this);
  }

  public boolean needsAutoRemoval() {
    return true;
  }

  /** This method got inadvertently disabled with composite frame merging (AF was 
      listening to a unused Controller). But when it was disabled no visible difference
      in apollo that i could discern. 
      EvidencePanel gets reset via ApolloPanel.
      Is the invalidate/validate really necasary - dont have a grasp on this 
      If it isnt necasary we should delete this method 
      Actually Im now thinking that DataListener and DataLoadListener are
      somewhat redundant and should be merged - though DataListener has both newData
      and dataLoadingDone - done allows for cursor changing.
  */
  public boolean handleDataLoadEvent(DataLoadEvent evt) {
    if (evt.dataRetrievalBeginning())
      // This cursor does not appear when loading 
      // from DataAdapterChooser - not sure why
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    else if (evt.dataRetrievalDone())
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      
    // forces a layout - is this needed?
    invalidate();
    validate();
    // this is redundant with AP.handleRegChEv which clears selection and sends out
    // empty selection event which prompts evidence panel reset()
    //evp.reset(); 
    return true;
  }


  private JDialog ld;
  private Runnable beforeLazyLoad = 
  new Runnable() {
    public void run() {
      Apollo.setLog4JDiagnosticContext();
      // heres javadoc for getAncestorOfClass: Convenience method for searching above
      //comp in the component hierarchy and returns the first object of class c it finds.
      // so i must be missing something here as this is getting ApolloFrame itself
      //JFrame f = (JFrame)SwingUtilities.getAncestorOfClass(ApolloFrame.class,szap.getComponent());
      JFrame af = ApolloFrame.this;
      ld = new JDialog(af,"Loading sequence");
//ld.setLocationRelativeTo(SwingUtilities.getRootPane(szap.getComponent()).getParent());
      ld.setLocationRelativeTo(af);
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(250,20));
      ld.getContentPane().add(p);
      ld.pack();
      ld.show();
      Apollo.clearLog4JDiagnosticContext();
    }
  };
  
  Runnable afterLazyLoad = new Runnable() {
                             public void run() {
                               Apollo.setLog4JDiagnosticContext();
                               ld.hide();
                               ld.dispose();
                               Apollo.clearLog4JDiagnosticContext();
                             }
                           };

  /** woops - this is currently disabled as apollo frame is no longer 
      listening to the controller - to reenable this should either be moved
      or made an inner class that does its own listening to the controller
      but it sounds like maybe this dialog was annoying and we may not want 
      to reenable it. in any case this probably doesnt belong in apollo frame */
  public boolean handleLazyLoadEvent(LazyLoadEvent evt) {
    //System.out.println("Handling lazy load");
    if (evt.getType() == LazyLoadEvent.BEFORE_LOAD) {
      if (!SwingUtilities.isEventDispatchThread()) {
        SwingUtilities.invokeLater(beforeLazyLoad);
      } else {
        beforeLazyLoad.run();
      }
    } else {
      if (!SwingUtilities.isEventDispatchThread()) {
        SwingUtilities.invokeLater(afterLazyLoad);
      } else {
        afterLazyLoad.run();
      }
    }
    return true;
  }


  /** If we decide to go all the way with the ApolloPanelHolderI interface 
      at some point, this will need to be changed to return an ApolloHolderI 
      To move toward the multispecies world this should return an array or
      list of szaps(?) or there could be another method?
      Presently the szap is set by CAFs instance vars when made active - 
      this is accessed by the menus - this is how the menus work with the active
      szap. There should be a method getActiveSzap and some tracking of which
      is the active szap - and the menus would query for ApolloFrame.getActiveSZAP
      Then we wouldnt have to be slipping szaps into apollo frame with 
      introspection for the menus sake.
  */
//   public StrandedZoomableApolloPanel getOverviewPanel() {
//     return (StrandedZoomableApolloPanel)this.szap;
//   }

  /** Override JFrame.setVisible. set szaps visible. */
  public void setVisible(boolean state) {
    super.setVisible(state);
    for (int i=0; i<getCurationManager().numberOfCurations(); i++) 
      getSZAP(i).setVisible(state);
  }


  /** Width of main apollo window */
  private int getPreferredFrameWidth() {
    return(Config.getMainWindowWidth());
  }

  public int getPreferredFrameHeight() { 
    if(getCurationManager().isMultiCuration())
      return 900; 
    else
      return 700; 
  }

  /** Returns split pane between evidence panel(s) and syntenyPanel(szaps), creates 
      one on first call. EvPanOrientMan actually adds in the synteny and ev panel. */
  public JSplitPane getSplitPane() {
    if (evidenceSzapSplitPane == null) {
      evidenceSzapSplitPane = new UnBorderedSplitPane();
    }
    return evidenceSzapSplitPane;
  }

  /** This is only called once, from ApolloRunner when the gui is first coming up,
      after the first dataset is loaded  (CAF.loadData()) */
  public void completeGUIInitialization() {

    MenuManager.createMenuManager(this);

    // previously in CAF.layoutPanels
    getContentPane().setLayout(new java.awt.BorderLayout());
    getContentPane().add(getSplitPane(),java.awt.BorderLayout.CENTER);
    StatusBar sb = getCurationManager().getStatusBar();
    getContentPane().add(sb,java.awt.BorderLayout.SOUTH);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new BasicWindowListener());

    // make sure window is not bigger than screen
    setSize(GuiUtil.fitToScreen(getPreferredFrameWidth(),getPreferredFrameHeight()));
    centerOnScreen(this);

    //getOrientationManager().orientEvidencePanels();
    EvidencePanelOrientationManager.getSingleton().orientEvidencePanels();

    
    setVisible(true);
    // Setting frame visible causes text avoidance to turn on
    // (waits til vis because it needs a graphics i think)
    // Turning on text avoidance causes a new layout with more tiers
    // This causes the vertical scroll bars to no longer be at start
    // so the vertical scroll bars need to be put at start here
    // I was tempted to make it part of the setVis call but i thought
    // it a bad idea to bundle the 2
    putVerticalScrollbarsAtStart();

    TypePanel.getTypePanelInstance().setLocationRelativeToFrame(getSize());

    // See comment in CAF.loadData for why this is here as well as there
    if (Config.getLayoutIniFile() != null) {
      JIniFile iniFile = new JIniFile(Config.getLayoutIniFile());
      applyLayout(iniFile, false);
    }
  } //end completeGUIInitialization


  public void saveLayout(JIniFile iniFile) {
    iniFile.writeString("Version","apollo",Version.getVersion()); 
    iniFile.writeString("Version","ini_file","0.1"); 

    iniFile.writeInteger("ApolloFrame","width",getSize().width); 
    iniFile.writeInteger("ApolloFrame","height",getSize().height); 
    iniFile.writeInteger("ApolloFrame","xorigin",getLocation().x); 
    iniFile.writeInteger("ApolloFrame","yorigin",getLocation().y); 

    iniFile.writeInteger("ApolloFrame","split_position",getSplitPane().getDividerLocation()); 
    String orientation = getSplitPane().getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? "vertical" : "horizontal";
    iniFile.writeString("ApolloFrame","split_orientation",orientation); 

    EvidencePanelContainer evc = EvidencePanelContainer.getSingleton();
    evc.saveLayout(iniFile);
    syntenyPanel.saveLayout(iniFile);
  }

  public void applyLayout(JIniFile iniFile, boolean updateBaseLocation) {
    logger.info("Apply layout from " + iniFile);

    int  numSpecies = iniFile.readInteger("SyntenyPanel","numSpecies",0);

    // A very basic sanity check
    if (numSpecies != syntenyPanel.getPanels().size()) {
      logger.warn("Different numbers of species in layout file to current display - not applying");
      return;
    }

 
    String version = iniFile.readString("Version","ini_file","FILE_VERSION_MISSING"); 

    if (!version.equals("0.1")) {
      logger.warn("Ini file version not compatible (want 0.1 have " + version + ")");
      return;
    }

    String section = "ApolloFrame";
    int width = iniFile.readInteger(section,"width",getSize().width);
    int height = iniFile.readInteger(section,"height",getSize().height);

    int xorigin = iniFile.readInteger(section,"xorigin",getLocation().x);
    int yorigin = iniFile.readInteger(section,"yorigin",getLocation().y);

    setBounds(xorigin,yorigin,width,height);
    validate();

    String curOrientation = getSplitPane().getOrientation() == JSplitPane.HORIZONTAL_SPLIT ? "vertical" : "horizontal";
    String orientation = iniFile.readString("ApolloFrame","split_orientation",curOrientation); 
    EvidencePanelOrientationManager.getSingleton().orientEvidencePanels(orientation);

    int splitPos = iniFile.readInteger("ApolloFrame","split_position",getSplitPane().getDividerLocation()); 
    getSplitPane().setDividerLocation(splitPos);
    getSplitPane().validate();

    EvidencePanelContainer evc = EvidencePanelContainer.getSingleton();
    evc.applyLayout(iniFile,updateBaseLocation);
    syntenyPanel.applyLayout(iniFile,updateBaseLocation);
  }

  /** Puts all the views vertical scroll bars at start position. compleGUIInit helper
   move to CurationManager? */
  private void putVerticalScrollbarsAtStart() {
    for (int i=0; i<getCurationManager().numberOfCurations(); i++) 
      getSZAP(i).putVerticalScrollbarsAtStart();
  }




  /** centers component on screen - put in a util class? */
  public static void centerOnScreen(Component c) {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    c.setLocation((screenSize.width - c.getSize().width) / 2,
                  (screenSize.height - c.getSize().height) / 2);
  }
  
  
  /** For now this is a no-op overridden by CompositeApolloFrame - eventually a 
      regular apollo frame would load a second species and go into multi species 
      This should be migrated to LoatUtil/DataLoader.
  */
  public void loadSyntenyLink(SeqFeatureI link) {}
  

  public SyntenyPanel getSyntenyPanel(){
    return syntenyPanel;
  }
  
  protected void setSyntenyPanel(SyntenyPanel panel){
    syntenyPanel = panel;
  }

  protected CompositeDataHolder getCompositeDataHolder(){
    return getCurationManager().getCompositeDataHolder();
  }

  protected static CurationManager getCurationManager() { 
    return CurationManager.getCurationManager(); 
  }
  protected GuiCurationState getCurationState(int i) { 
    return getCurationManager().getCurationState(i); 
  }
  private StrandedZoomableApolloPanel getSZAP(int i) {
    return getCurationState(i).getSZAP(); 
  }
  
}

//   private boolean isLazySequence(SequenceI seq) {
//     // I'm pretty sure the test below works--GAMESequence is the only kind of sequence
//     // that claims to be LazySequenceI but in fact preloads rather than lazy-loading.
//     // NH, 05/09/2003
//     return ((seq instanceof LazySequenceI) && 
// 	    !(seq instanceof GAMESequence));
//   }


  // not used -- this was to supress drawing during a load - but this method is no
  // longer called anywhere - do we still need this - if so it has to be revamped
  // for multi species - commenting out for now
//   void setLoadInProgress(boolean inProgress) {
//     szap.setLoadInProgress(inProgress);
//   }
//   public static void copyTextToClipboard(String text) {
//     StringSelection contents = new StringSelection(text);
//     Clipboard clipboard = Toolkit.getDefaultToolkit().
//                           getSystemClipboard();
//     clipboard.setContents(contents, contents);
//   }

//   // this is currently not used
// //   public SequenceI getSeqFromClipboard() {
// //     return (SequenceI) clipboard_seqs.elementAt (0);
// //   }

//   public void copySeqToClipboard(Sequence seq) {
//     //clipboard_seqs.removeAllElements(); -- not used
//     //clipboard_seqs.addElement (seq);
//     String fasta = FastaFile.format (seq.getDisplayId(),
//                                      seq.getResidues(),
//                                      50);
//     copyTextToClipboard (fasta);
//   }
