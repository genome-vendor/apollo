package apollo.gui.synteny;
import java.lang.reflect.*;
import java.util.*;
import apollo.config.Config;
import apollo.datamodel.*;
import apollo.dataadapter.ApolloDataAdapterI;
import apollo.gui.*;
import apollo.dataadapter.DataLoadEvent;
import apollo.gui.evidencepanel.EvidencePanel;
import apollo.gui.evidencepanel.EvidencePanelOrientationManager;
import apollo.gui.genomemap.*;
import apollo.gui.menus.*;
import apollo.main.LoadUtil;
import apollo.main.DataLoader;
import javax.swing.*;
import java.awt.*;

import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

import misc.JIniFile;

/**
 * <p>The synteny panels need to display many stranded-zoomable-apollo-panels at once,
 * (unlike the standard apollo frame). However apollo's menus are very aware of
 * apollo frames...this subclass is an attempt to circumvent these issues without
 * having to rewrite lots of the apollo menu-handling code to support synteny
 * viewing.</p>
 *
 * <p>The idea is: to hold many copies of the instance variables that an Apollo frame
 * usually holds (one for each browsed species). 
 * When the user their switches between different species, each copy of 
 * these instance variables is, in turn, switched over to the set specific to that species.
 * Menu items (and anything else which explicitly needs to see the frame) should be 
 * oblivious to the change.</p>
 *
 * <p> Of course, we override the drawing/initialisation code to set up multiple 
 * stranded-zoomable-apollo-panels, controllers etc. </p>
 * <P> Warning this code is scary
 * <p> 1. It is obscure </p>
 * <p> 2. It relies upon the hash keys to be exact string matches to the 
 * names of variables in ApolloFrame makes this extremely vulnerable
 * to bugs cropping up because of name changes to the variables or
 * other changes in initialization. It completely depends upon the
 * exposure of the object internals.
 * It strikes me that this is really not an object that extends
 * the basic ApolloFrame, but really one that manages ApolloFrames.
 * <p> NEEDS TO BE REFACTORED! </p>
 * 
 * Should CompositeApolloFrame be merged with ApolloFrame now that CAF is used
 for single and multi species?
 
**/
public class CompositeApolloFrame extends ApolloFrame {
  
  // hmmmmmmmmmmm - composite adapter seems to only be used for load synteny link
  // and doesnt seem to be getting set anywhere - what the hell is going on???
  // so i think this is pase - a refactoring of sorts is needed ???
  private ApolloDataAdapterI compositeAdapter; // -> curMan
  
  //convenience map to allow me to retrieve link panels by name
  private HashMap linkPanels = new HashMap();

  private Color normalLabelColor = null;
  private Color selectedLabelColor = new Color(0,153,0);  // dark green

  private static CompositeApolloFrame compositeApolloFrameSingleton;

  public static CompositeApolloFrame getApolloFrame() {
    if (compositeApolloFrameSingleton == null) { 
      Application app = new DefaultApplication();
      compositeApolloFrameSingleton = new CompositeApolloFrame();
      app.addApplicationListener(new MacApplicationSupport());
    }
    return compositeApolloFrameSingleton;
  }

  /** should singleton method be in CAF? */
  private CompositeApolloFrame() {}

  /** loadData gets called with every data load(see LoadUtil). This is where the
      data gets loaded (previously called init()) */
  public void loadData(ApolloDataAdapterI dataAdapter, CompositeDataHolder cdh) {
    // before we wipe out old cdh need to clean it of mem leaks
    //this.compositeDataHolder = cdh;
    //setCompositeAdapter(dataAdapter);

    getCurationManager().setDataAdapter(dataAdapter);
    getCurationManager().setCompositeDataHolder(cdh);
    // sets up synteny panel - is it necasary to recreate this with
    // every load? - minimally should only do if curSet # changes
    initialiseApolloPanels(cdh);
    //loadDataIntoPanels(compositeDataHolder);
    setTitle();
    // select feats requested in load (if gene)
    getCurationManager().selectInputFeatures(); 
    //createMenuManager(); // no longer needed
    // validate causes a repaint that doesnt happen otherwise - wacky
    // recreating menu manager used to serve the role (unknowingly) of making a 
    // repaint happen. repaint and invalidate dont do the trick.

    validate();
  }

  /** get title for frame -> CurationManager? */
  private void setTitle() {
    
    //These two instance variables treated carefully to try and stop memory leaks...
    // This is for mem leak cleanup - this is the wrong place for cleanup 
    // its too late - this is suppose to clean up old name adapter but
    // we already have the new one (causes null pointer) this is taken
    // care of in AF.ClearOutDataListener.newData()
    //this.compositeDataHolder = compositeDataHolder;

    // Get the title 
    String title = ""; 
    if(getCurationManager().isMultiCuration()){
      title = "Synteny: "; //will be composed of compara-curation-set titles.
    }else{  // Not synteny view--normal view
      title = 
        getCompositeDataHolder().getCurationSet(0).getName();
    }
    for (int i=0; i<getCompositeDataHolder().numberOfSpecies(); i++) {
      //setName = compositeDataHolder.getSpecies(i);
//       currentPanel = (StrandedZoomableApolloPanel)
//         ((HashMap) getSpeciesSpecificApolloFrameInstanceVariables().get(setName)
//         ).get("szap");
      CurationSet currentSet = getCompositeDataHolder().getCurationSet(i);
//       currentPanel.setCurationSet(currentSet);
      if (currentSet.getOrganism() != null && !(currentSet.getOrganism().equals(""))) {
        if (i > 0)
          title += " /";
        title += " " + currentSet.getOrganism();
      }
//       ((HashMap)
//         getSpeciesSpecificApolloFrameInstanceVariables().get(setName)
//       ).put("curationSet", currentSet);
    }//end for
    setTitle(title);
  } // end setTitle

  /** called with every load - puts all into existing syntenyPanel. shoould only do 
      this if # of curSets change. creates new syntenyLinkPanels (should be cached in 
      CurManager) and sets its link data. setting of link data should be done in
      a different method so only gui stuff here for curSet # change */
  private void initialiseApolloPanels(
    //ApolloDataAdapterI compositeAdapter,
    CompositeDataHolder compositeDataHolder
  ){
    StrandedZoomableApolloPanel firstPanel;
    StrandedZoomableApolloPanel secondPanel;
    SyntenyLinkPanel currentLinkPanel;
    //ApolloDataAdapterI currentAdapter;

    Vector orderedSingleSpeciesPanels = new Vector();
    Vector orderedLinkPanels = new Vector();
    for (int i=0; i<compositeDataHolder.speciesCompSize(); i++) {

      SpeciesComparison specComp = compositeDataHolder.getSpeciesComparison(i);
      // I dont think its presumptious to assume it at least has species1
      //firstSetName = specComp.getSpecies1();
      //firstPanel = (SZAP)getSingleSpeciesPanels().get(firstSetName);
      firstPanel = getCurationState(i).getSZAP();

      // Only add 1st species the first time, otherwise its added already as the 2nd
      // species of the previous SpecComp (dont add it twice)
      if (orderedSingleSpeciesPanels.isEmpty())
        orderedSingleSpeciesPanels.addElement(firstPanel);

      // single cur set wont have 2nd species/curSet
      if (specComp.hasSecondSpecies()) {
        //secondSetName = specComp.getSpecies2();
        //secondPanel=(SZAP)getSingleSpeciesPanels().get(secondSetName);
        secondPanel = getCurationState(i+1).getSZAP();
        orderedSingleSpeciesPanels.addElement(secondPanel);

        // I think it makes sense to nest this inside 2nd spec "if"
        // this is returns true even for link sets with no links
        // should this use hasNonEmptyLinkSet? not sure
        if (specComp.hasLinkSet()) {
          LinkSet currentLinkSet = compositeDataHolder.getLinkSet(i);
          if (getLinkPanels().containsKey(currentLinkSet.getName())) {
            currentLinkPanel = (SyntenyLinkPanel)getLinkPanels().get(currentLinkSet.getName());
          } else {
            currentLinkPanel = new SyntenyLinkPanel(firstPanel,secondPanel);
            getLinkPanels().put(currentLinkSet.getName(), currentLinkPanel);
            currentLinkPanel.setMinimumSize(new Dimension(0,0));
          }
          currentLinkPanel.setLinks(currentLinkSet);
            
          firstPanel.getController().addListener(currentLinkPanel);
          secondPanel.getController().addListener(currentLinkPanel);

          currentLinkPanel.setStatusBar(getCurationManager().getStatusBar());
          orderedLinkPanels.addElement(currentLinkPanel);

          Config.getStyle().getPropertyScheme().addPropSchemeChangeListener(firstPanel.getController());
          Config.getStyle().getPropertyScheme().addPropSchemeChangeListener(secondPanel.getController());
          
        }
      } // end of if specComp.hasSecondSpecies

    } // end of speciesComparison for loop

    if (getSyntenyPanel() == null) { 
      setSyntenyPanel(
        new SyntenyPanel(orderedSingleSpeciesPanels,orderedLinkPanels,this));
      //getSyntenyPanel().setPreferredSize(new java.awt.Dimension(900, 350));
    }
    else { // do we really need to reset on every load?
      getSyntenyPanel().setPanels(orderedSingleSpeciesPanels,orderedLinkPanels);
    }
    
    // i think this needs to happen every time as listeners get cleared out
    // this raises the questioin of whether all the listeners need be cleared
    getCurationManager().addListenerToAllCurations(getSyntenyPanel());
  } //end initializeApolloPanels

  private HashMap getLinkPanels() {
    return linkPanels;
  }

  
  private Color getNormalLabelColor(){
    return normalLabelColor;
  }//end getNormalLabelColor
  
  private void setNormalLabelColor(Color color){
    normalLabelColor = color;
  }//end setNormalLabelColor

  private Color getSelectedLabelColor(){
    return selectedLabelColor;
  }
  private void setSelectedLabelColor(Color color){
    selectedLabelColor = color;
  }//end setNormalLabelColor

  /**
   * Propagate the setVisible call to all stranded-zoomable panels: their
   * setVisible call is quite involved...
  **/
  public void setVisible(boolean state) {
    super.setVisible(state);

    // Super call seems to also do this so remove from here
    //Vector panels = getSyntenyPanel().getPanels();
    //for(int i=0; i<panels.size(); i++){
    //  ((StrandedZoomableApolloPanel)panels.get(i)).setVisible(state);
    //}
  }
  
  /**
   * We have to clear data from each one of the child curation sets owned by the 
   * composite.
  **/
//   public void clearData () {
//     super.clearData();
//     compositeDataHolder.cleanup();
//   }  

   // should SyntenyPanel be a singleton? SyntenyPanel methods
  public void setUseOpaqueLinks(boolean value){
    getSyntenyPanel().setUseOpaqueLinks(value);
  }
  
  public void setShadeByPercId(boolean value){
    getSyntenyPanel().setShadeByPercId(value);
  }

   /** Load new species and links for the link passed in 
       This needs to be moved to LoadUtil/DataLoader */
  public void loadSyntenyLink(SeqFeatureI link) { // boolean isTop?
    // this is wrong - we dont have a compositeAdapter anymore
    // for now.... workaround
    compositeAdapter = CurationManager.getCurationManager().getDataAdapter();
    try { compositeAdapter.loadNewSpeciesFromLink(link,getCompositeDataHolder()); }
    catch (org.bdgp.io.DataAdapterException e) { 
      // temp - fill this in later
      System.out.println("data adapter exception - do something...");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
// now done in CurationState.setCurationSet()
//    // hardwire to 1 - change when not hardwired
//     String region = getCompositeDataHolder().getCurationSet(1).getName();
//     DataLoadEvent e = new DataLoadEvent(this,getCompositeDataHolder(),region);
//     // this has to happen before loadData - as it clears out all the controllers
//     // listeners - is there something that needs it after - we need 2 events!
//     Controller.getMasterController().handleDataLoadEvent(e);
    loadData(compositeAdapter,getCompositeDataHolder());
//     String region = compositeDataHolder.getCurationSet(1).getName();
//     DataLoadEvent e = new DataLoadEvent(this,compositeDataHolder,region);
//     // this has to happen before loadData - as it clears out all the controllers
//     // listeners - is there something that needs it after - we need 2 events!
//     Controller.getMasterController().handleDataLoadEvent(e);
  }

  public void setLockScrolling(boolean value){
    getSyntenyPanel().setLockScrolling(value);
  }

  public void setShiftZoomLock(boolean state) { 
    getSyntenyPanel().setShiftLockZoom(state);
  }
  
  public static class MacApplicationSupport implements ApplicationListener
  {
    public void handleAbout(ApplicationEvent e)
    {
        new HelpMenu(null).showAboutBox();
        e.setHandled(true);
    }
    
    public void handleQuit(ApplicationEvent e)
    {
        if (LoadUtil.confirmSaved(FileMenu.quit_options, new DataLoader())) {
            System.exit(0);
        }
    }
    
    public void handleOpenApplication(ApplicationEvent e)
    {
    }
    
    public void handleOpenFile(ApplicationEvent e)
    {
    }
    
    public void handlePreferences(ApplicationEvent e)
    {
    }
    
    public void handlePrintFile(ApplicationEvent e)
    {
    }
    
    public void handleReopenApplication(ApplicationEvent e)
    {
    }
  }
  
}//end CompositeApolloFrame 


