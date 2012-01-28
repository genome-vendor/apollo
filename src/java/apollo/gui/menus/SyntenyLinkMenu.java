package apollo.gui.menus;

import apollo.config.Config;
import apollo.config.TierProperty;
import apollo.gui.*;
import apollo.gui.genomemap.*;
import apollo.gui.synteny.SyntenyLinkPanel;
import apollo.datamodel.*;
import apollo.gui.event.*;
import apollo.dataadapter.*;
import apollo.seq.io.*;
import apollo.util.FeatureList;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/** This is the right popup menu for synteny links.
    There could be a gui.synteny.menu package to separate from apollo menus. */

public class SyntenyLinkMenu extends JPopupMenu implements ActionListener {

  SyntenyLinkPanel synLinkPanel;

  JMenuItem scale;
  JCheckBoxMenuItem autoStraighten;
  private JMenuItem showJustDnaLinks;
  private JMenuItem showJustProteinLinks;
  private JMenuItem showBothLinkTypes;
  private JRadioButtonMenuItem showAllStrands;
  private JRadioButtonMenuItem plusPlusStrands;
  private JRadioButtonMenuItem minusMinusStrands;
  private JRadioButtonMenuItem plusMinusStrands;
  private JRadioButtonMenuItem minusPlusStrands;
  private JRadioButtonMenuItem clearStrandsInvisibleButton;
  private JMenuItem homeIn;

  //private FeatureList selectedLinks;
  private LinkSet selectedLinks;
  //private Point     pos; // not used anymore
  
  // take pos out - not used
  public SyntenyLinkMenu(SyntenyLinkPanel synLinkPanel, LinkSet selectedLinks) {
    super("Synteny Link");
    this.synLinkPanel = synLinkPanel;
    this.selectedLinks = selectedLinks;
    addPopupMenuListener(new LinkMenuListener());
    addPropertyChangeListener(new PopupPropertyListener());
    menuInit();
  }

  public void menuInit() {
    scale = new JMenuItem("Equalise ranges");
    autoStraighten = new JCheckBoxMenuItem("Auto straighten");
    showJustDnaLinks = new JMenuItem("Show just DNA links");
    showJustProteinLinks = new JMenuItem("Show just protein links");
    showBothLinkTypes = new JMenuItem("Show both protein and DNA links");
    showAllStrands = new JRadioButtonMenuItem("Show all strands");
    plusPlusStrands = new JRadioButtonMenuItem("Show +/+ strands");
    minusMinusStrands = new JRadioButtonMenuItem("Show -/- strands");
    plusMinusStrands = new JRadioButtonMenuItem("Show +/- strands");
    minusPlusStrands = new JRadioButtonMenuItem("Show -/+ strands");
    clearStrandsInvisibleButton = new JRadioButtonMenuItem();
    homeIn = new JMenuItem("Home in on selected link");

    add(scale);
    add(autoStraighten);
    //add(showBothLinkTypes);
    //add(showJustDnaLinks);
    //add(showJustProteinLinks);
    add(showAllStrands);
    add(plusPlusStrands);
    add(minusMinusStrands);
    add(plusMinusStrands);
    add(minusPlusStrands);
    add(homeIn);
    homeIn.setEnabled(selectedLinks.size()==1);

    ButtonGroup strandVisGroup = new ButtonGroup();
    strandVisGroup.add(showAllStrands);
    strandVisGroup.add(plusPlusStrands);
    strandVisGroup.add(minusMinusStrands);
    strandVisGroup.add(plusMinusStrands);
    strandVisGroup.add(minusPlusStrands);
    // Not visible - selected to deselect rest of buttons
    strandVisGroup.add(clearStrandsInvisibleButton);

    autoStraighten.setState(synLinkPanel.getAutoStraighten());

    scale    .addActionListener(this);
    autoStraighten.addActionListener(this);

    showJustDnaLinks.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.displayJustDnaLinks();
        } } );
    showJustProteinLinks.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.displayJustProteinLinks();
        } } );
    showBothLinkTypes.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.displayBothLinks();
        } } );
    plusPlusStrands.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.setSingleStrandVisibility(true,true);
        } } );
    minusMinusStrands.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.setSingleStrandVisibility(false,false);
        } } );
    plusMinusStrands.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.setSingleStrandVisibility(true,false);
        } } );
    minusPlusStrands.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.setSingleStrandVisibility(false,true);
        } } );
    showAllStrands.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.showAllStrands(); } } );
    homeIn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          synLinkPanel.homeInOnSelectedLink(); } } );

    // add menu items for all the link types visibility
    addLinkVisibilityMenuItems();
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == scale) {
      synLinkPanel.syncScales();
      //} else if (e.getSource() == exonLevel){createExonSyntenyPanel();
    } else if (e.getSource() == autoStraighten) {
      synLinkPanel.setAutoStraighten(autoStraighten.getState());
    }
  }
  
  /** Query PropertyScheme for linked FeatureProperties and add menu items for
      their visibility */
  private void addLinkVisibilityMenuItems() {
    int linkedTierPropsSize = Config.getPropertyScheme().getLinkedTierProps().size();
    for (int i=0; i<linkedTierPropsSize; i++) {
      final TierProperty tierProp = Config.getPropertyScheme().getLinkedTierProp(i);
      String s = "Show "+tierProp.getLabel();
      final JRadioButtonMenuItem linkVis = new JRadioButtonMenuItem(s);
      linkVis.setSelected(tierProp.isVisible());
      linkVis.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            tierProp.setVisible(linkVis.isSelected()); } } );
      add(linkVis);
    }
  }


  /** LinkMenuListener inner class. Synch up check boxes on bringing up menu. */
  private class LinkMenuListener implements PopupMenuListener {
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) { 
      boolean topFor = synLinkPanel.isTopForwardVisible();
      boolean topRev = synLinkPanel.isTopReverseVisible();
      boolean botFor = synLinkPanel.isBottomForwardVisible();
      boolean botRev = synLinkPanel.isBottomReverseVisible();
      if (topFor && topRev && botFor && botRev) {
        showAllStrands.setSelected(true);
        return;
      }
      clearStrandsInvisibleButton.setSelected(true);// turn off all
      // no buttons valid for this state - 2 strands in one, 1 strand other
      if ((topFor && topRev) || (botFor && botRev)) return; 
      
      plusPlusStrands.setSelected(topFor && botFor);
      plusMinusStrands.setSelected(topFor && botRev);
      minusPlusStrands.setSelected(topRev && botFor);
      minusMinusStrands.setSelected(topRev && botRev);
    }
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
    public void popupMenuCanceled(PopupMenuEvent e) {}
  }
}
