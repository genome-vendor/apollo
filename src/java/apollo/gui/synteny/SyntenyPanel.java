package apollo.gui.synteny;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import apollo.*;
import apollo.dataadapter.*;
import apollo.datamodel.*;
import apollo.seq.io.*;
import apollo.config.Config;
import apollo.gui.Controller;
import apollo.gui.SelectionManager;
import apollo.gui.StatusBar;
import apollo.gui.genomemap.*;
import apollo.gui.event.*;
import apollo.gui.menus.*;
import apollo.util.*;
import apollo.gui.UnBorderedSplitPane;

import java.io.*;
import gov.sandia.postscript.*;
import misc.JIniFile;

import org.jdesktop.swingx.MultiSplitLayout;
import org.jdesktop.swingx.MultiSplitLayout.*;
import org.jdesktop.swingx.MultiSplitPane;

import org.apache.log4j.*;



/** JPanel that holds szaps and link panels(if multi species). 
    Just gets created once, and theres only one of them - Singleton? 
    szaps and link panels get reset on every load - change that?*/
public class SyntenyPanel extends JPanel implements ReverseComplementListener {

  protected final static Logger logger = LogManager.getLogger(SyntenyPanel.class);

  private Vector speciesPanels;
  private Vector linkPanels;
  private Vector dividers;
  private StatusBar sb;
  private CompositeApolloFrame compositeApolloFrame;

  int curNumCuration = 0; // SMJS added this to store the number of curations in  the current set, so we can
                          //      check whether a newly loaded set has the same number of curations. This is
                          //      used to decide whether relayout of the panels is necessary

  public SyntenyPanel(Vector speciesPanels, Vector linkPanels, CompositeApolloFrame c) {
    this.speciesPanels = speciesPanels;
    this.linkPanels = linkPanels;
    this.compositeApolloFrame = c;
    for (int i=0; i<linkPanels.size(); i++) 
      ((SyntenyLinkPanel)linkPanels.elementAt(i)).setSyntenyPanel(this);
    setPreferredSize(new java.awt.Dimension(900, 350));
    _layoutPanels();
  }

  void setPanels(Vector szaps, Vector linkPanels) {
    this.speciesPanels = szaps;
    this.linkPanels = linkPanels;
    for (int i=0; i<linkPanels.size(); i++) {
      ((SyntenyLinkPanel)linkPanels.elementAt(i)).setSyntenyPanel(this);
    }
    _layoutPanels(); // does a removeAll
  }

  public void print(File file, String orientation, String scale) {
    try {
      PrintWriter fw = new PrintWriter(new FileWriter(file));
      Graphics psg = new PSGr2(fw);

      double scaleVal = 1.0;
      try {
        scaleVal = new Double(scale).doubleValue();
      } catch (Exception e) {
        logger.error("Invalid scale factor");
        return;
      }

      int yOffset;
      if (orientation.equals("landscape")) {
        fw.println("-30 30 translate");
        fw.println("90 rotate");
        //fw.println("30 -642 translate");
        yOffset = ((int)(-822.0*scaleVal));
        fw.println("" + ((int)(30.0*scaleVal)) + " " + ((int)(-822.0*scaleVal)) + " translate");
        fw.println("" + scale + " " + scale + " scale");
      } else {
        fw.println("-30 30 translate");
        yOffset = (int)(762.0 - 792.0*scaleVal );
        fw.println("30 " + yOffset + " translate");
        fw.println("" + scale + " " + scale + " scale");
      }

      invalidate();
      psg.setClip(new Rectangle(0,0,getSize().width,getSize().height));

      for (int i=0; i < speciesPanels.size(); i++) {
        ApolloPanel ap = ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i)).getApolloPanel();
        ap.paintComponent(psg);
        yOffset = (int) ((double)(ap.getSize().height));
        fw.println(0 + " " + (-yOffset) + " translate");
        if (i!=speciesPanels.size()-1) {
          SyntenyLinkPanel slp = ((SyntenyLinkPanel)linkPanels.elementAt(i));
          slp.paintComponent(psg);
          yOffset = (int) ((double)(slp.getSize().height));
          fw.println(0 + " " + (-yOffset)  + " translate");
        }
      }
      psg.dispose();
      fw.close();
    } catch (Exception e) {
      logger.error("Failed printing to file " + file + " Exception: " + e);
      e.printStackTrace();
    }
  }

  public void saveLayout(JIniFile iniFile) {
    iniFile.writeInteger("SyntenyPanel","width",getSize().width);
    iniFile.writeInteger("SyntenyPanel","height",getSize().height);
    iniFile.writeInteger("SyntenyPanel","numSpecies",speciesPanels.size());

    for (int i=0; i<speciesPanels.size(); i++) {
      String section = "StrandedZoomableApolloPanel" + i;
      StrandedZoomableApolloPanel szap = ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i));
      
      szap.saveLayout(iniFile, section);
    }

    // Currently do this here because I don't need much from the link panels
    for (int i=0; i < linkPanels.size(); i++) {
      String section = "SyntenyLinkPanel" + i;
      SyntenyLinkPanel slp = ((SyntenyLinkPanel)linkPanels.elementAt(i));

      iniFile.writeInteger(section,"width",slp.getSize().width);
      iniFile.writeInteger(section,"height",slp.getSize().height);
      iniFile.writeInteger(section,"xorigin",slp.getLocation().x);
      iniFile.writeInteger(section,"yorigin",slp.getLocation().y);
    }
  }

  public void applyLayout(JIniFile iniFile, boolean updateBaseLocation) {

    int  numApolloPanels = iniFile.readInteger("SyntenyPanel","numSpecies",0);

    // Go through species panels setting various visibility flags
    // Do this before any resizing so it doesn't disrupt the final layout
    for (int i=0; i<speciesPanels.size(); i++) {
      String section = "StrandedZoomableApolloPanel" + i;
      StrandedZoomableApolloPanel szap = ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i));

      szap.applyVisibilityFromLayout(iniFile, section);
    }

    if (numApolloPanels > 1) {
      int divInd = dividers.size()-1;
      int divLoc;
      MultiSplitPane msp = (MultiSplitPane)getSzap(0).getParent();
      Node modelRoot = (Node)msp.getMultiSplitLayout().getModel();
 
      int  panelHeight = iniFile.readInteger("SyntenyPanel","height",getSize().height);
      int  panelWidth = iniFile.readInteger("SyntenyPanel","width",getSize().width);
      modelRoot.setBounds(new Rectangle(0,0,panelWidth,panelHeight));

      for (int i=speciesPanels.size()-1; i>0; i--) {
        String section = "StrandedZoomableApolloPanel" + i;
        int yorigin = iniFile.readInteger(section,"yorigin",0);
  
        divLoc = yorigin - 3;
        Divider div = (Divider)dividers.get(divInd--);
        div.setBounds(new Rectangle(0,divLoc,0,3));
  
        section = "SyntenyLinkPanel" + (i-1);
        yorigin = iniFile.readInteger(section,"yorigin",0);
  
        divLoc = yorigin - 3;
        div = (Divider)dividers.get(divInd--);
        div.setBounds(new Rectangle(0,divLoc,0,3));
      }
      // MultiSplitLayout.printModel(modelRoot);
    }
    invalidate();
    validate();

    // Now that we've arranged all the panels nicely go through species panels setting all layout values
    for (int i=0; i<speciesPanels.size(); i++) {
      String section = "StrandedZoomableApolloPanel" + i;
      StrandedZoomableApolloPanel szap = ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i));

      szap.applyLayout(iniFile, section, updateBaseLocation);
    }
  }

  public Vector getPanels() {
    return speciesPanels;
  }

  public Vector getLinkPanels() {
    return linkPanels;
  }

  public Dimension getPreferredSize() {
    return new Dimension(550,550);
  }

  /** Min size 0,0 - allow the user to shrink the syn panel down to 0 */
  public Dimension getMinimumSize() {
    return new Dimension(0,0);
  }

  public void addPanel(StrandedZoomableApolloPanel ap) {
    if (!speciesPanels.contains(ap)) {
      speciesPanels.addElement(ap);
    }
  }

  public void removePanel(StrandedZoomableApolloPanel ap) {
    if (speciesPanels.contains(ap)) {
      speciesPanels.removeElement(ap);
      _layoutPanels();
    }
  }

  public void addLinkPanel(SyntenyLinkPanel linkPanel) {
    if (!linkPanels.contains(linkPanel)) {
      linkPanels.addElement(linkPanel);
    }
  }

  public boolean handleReverseComplementEvent(ReverseComplementEvent evt) {
    repaint();
    return false;
  }

  public void setStatusBar(StatusBar sb) {
    if (speciesPanels.size() > 0) {
      for (int i=0; i < speciesPanels.size(); i++) {
        ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i)).setStatusBar(sb);
      }
    }
    if (linkPanels.size() > 0) {
      for (int i=0; i < linkPanels.size(); i++) {
        SyntenyLinkPanel lp = (SyntenyLinkPanel)linkPanels.elementAt(i);
        lp.setStatusBar(sb);
      }
    }
    this.sb = sb;
  }


  public void zoomToSelection() {
    if (speciesPanels.size() > 0) {
      for (int i=0; i < speciesPanels.size(); i++) {
        ((StrandedZoomableApolloPanel)speciesPanels.elementAt(i)).zoomToSelection();
      }
    }
  }

  public StatusBar getStatusBar() {
    return sb;
  }

  public void _layoutPanels() {
    int bigPanelSize = Config.getStyle().getSingleSpeciesPanelSize(); //defaults to 300
    int smallPanelSize = Config.getStyle().getLinkPanelSize(); //defaults to 50
    int currentDividerLocation = 0; 

    // Don't relayout if the number of curations has not changed
    if (CurationManager.getCurationManager().numberOfCurations() == curNumCuration) {
      return;
    }

    curNumCuration = CurationManager.getCurationManager().numberOfCurations();

    // change this to divvy up panels as a percentage of whole - > 3 species
    // are getting squeezed out
    if(bigPanelSize <= 0){
      bigPanelSize = 300;
    }

    if(smallPanelSize <=0){
      smallPanelSize = 50;
    }

    int totalHeight = bigPanelSize*speciesPanels.size() + smallPanelSize*linkPanels.size();
    if (getBounds().height > 0 && isVisible()) {
      if (totalHeight > getBounds().height) {
        double multiplier = (double) getBounds().height / (double)totalHeight;
        bigPanelSize = (int) (multiplier*(double)bigPanelSize);
        smallPanelSize = (int) (multiplier*(double)smallPanelSize);
      }
    } else if (totalHeight > 700) {
      double multiplier = (double) 700 / (double)totalHeight;
      bigPanelSize = (int) (multiplier*(double)bigPanelSize);
      smallPanelSize = (int) (multiplier*(double)smallPanelSize);
    }

    logger.debug("species panel size = " + bigPanelSize + " link panel size " + smallPanelSize);

    if (speciesPanels.size() > 0) {
      removeAll();
      dividers = null;
      double weight = 1.0D / (double) (speciesPanels.size() + linkPanels.size());

      Split modelRoot = new Split();
      modelRoot.setBounds(new Rectangle(0, 0, 0, isVisible() && getBounds().height > 0? getBounds().height : 700));
      modelRoot.setRowLayout(false);

      ArrayList children = new ArrayList();
      dividers = new Vector();
      for (int i=0; i < speciesPanels.size(); i++) {
        Leaf leaf = new Leaf("szap" + i);
        leaf.setWeight(weight);
        children.add(leaf);
        currentDividerLocation += bigPanelSize;

        if (linkPanels.size() > i) {
          Divider div = new Divider();
          div.setBounds(new Rectangle(0,currentDividerLocation,0,3));
          dividers.add(div);
          children.add(div);
          currentDividerLocation += 3;

          leaf = new Leaf("slp" + i);
          leaf.setWeight(weight);
          children.add(leaf);
          currentDividerLocation += smallPanelSize;

          div = new Divider();
          div.setBounds(new Rectangle(0,currentDividerLocation,0,3));
          children.add(div);
          dividers.add(div);
          currentDividerLocation += 3;
        }
      }

      MultiSplitPane multiSplitPane = new MultiSplitPane();
      multiSplitPane.getMultiSplitLayout().setFloatingDividers(false);
      multiSplitPane.getMultiSplitLayout().setDividerSize(3);
      // multiSplitPane.setContinuousLayout(false);
      // multiSplitPane.setDividerPainter(new TriangleDividerPainter());

      if (speciesPanels.size() > 1) {
        modelRoot.setChildren(children);
        multiSplitPane.getMultiSplitLayout().setModel(modelRoot);
      } else {
        multiSplitPane.getMultiSplitLayout().setModel((Node)children.get(0));
      }

        
      for (int i=0; i < speciesPanels.size(); i++) {
        multiSplitPane.add(getSzap(i), "szap" + i);

        if (linkPanels.size() > i) {
          multiSplitPane.add(getLinkPanel(i), "slp" + i);
          getLinkPanel(i).setPreferredSize(new Dimension(0,10));
        }
      }

      setLayout(new java.awt.BorderLayout());
      add(multiSplitPane, java.awt.BorderLayout.CENTER);
      // For debugging 
      // MultiSplitLayout.printModel(modelRoot);
    }
  }

  public void setVisible(boolean state) {
    super.setVisible(state);
  }
  
  public boolean areOpaqueLinksUsed(){
    return ((SyntenyLinkPanel)getLinkPanels().get(0)).getUseOpaqueLinks();
  }
  
  public boolean areLinksShadedByPercId(){
    return ((SyntenyLinkPanel)getLinkPanels().get(0)).getShadeByPercId();
  }
  
  public void setUseOpaqueLinks(boolean value){
    Vector panels = getLinkPanels();
    for(int i=0; i<panels.size(); i++){
      ((SyntenyLinkPanel)panels.get(i)).setUseOpaqueLinks(value);
      ((SyntenyLinkPanel)panels.get(i)).repaint();
    }
  }
  
  public void setShadeByPercId(boolean value){
    Vector panels = getLinkPanels();
    for(int i=0; i<panels.size(); i++){
      ((SyntenyLinkPanel)panels.get(i)).setShadeByPercId(value);
      ((SyntenyLinkPanel)panels.get(i)).repaint();
    }
  }

  public void setLockScrolling(boolean value){
    for(int i=0; i<getPanels().size(); i++) 
      getSzap(i).setScrollingPropagated(value);
  }

  private int numberOfPanels() { return getPanels().size(); }

  private StrandedZoomableApolloPanel getSzap(int i) {
    return (StrandedZoomableApolloPanel)getPanels().get(i);
  }

  private SyntenyLinkPanel getLinkPanel(int i) {
    return (SyntenyLinkPanel)getLinkPanels().get(i);
  }

  void setShiftLockZoom(boolean state) {
    for (int i=0; i<numberOfPanels(); i++) 
      getSzap(i).setShiftForLockedZooming(state);
  }
}
