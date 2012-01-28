package apollo.gui.detailviewers.sequencealigner.AAPanel;

import java.awt.Color;
import java.awt.Dimension;

import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.AbstractTierPanel;
import apollo.gui.detailviewers.sequencealigner.FeaturePlaceFinder;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanel;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.ReferenceFactoryI;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.Tier;
import apollo.gui.detailviewers.sequencealigner.TierI;
import apollo.gui.detailviewers.sequencealigner.TierPanel;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidBaseRenderer;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidRenderer;

public class ReferencePanel extends MultiTierPanel {
  
  private ReferenceFactoryI referenceFactory;
  private MultiTierPanel annotationPanel;
  private Color dnaColor;

  public ReferencePanel(ReferenceFactoryI referenceFactory, 
      MultiTierPanel annotationPanel) {
      //Orientation orientation, Strand strand, ReadingFrame frame) {
    super(FeaturePlaceFinder.Type.COMPACT);
    this.annotationPanel = annotationPanel;
    
    this.dnaColor = Color.DARK_GRAY;
    
    Orientation orientation = annotationPanel.getOrientation();
    Strand strand = annotationPanel.getStrand();
    ReadingFrame frame = annotationPanel.getReadingFrame();
    
    this.referenceFactory = referenceFactory;

    // The AA Reference for the current strand
    SequenceI referenceAA = 
      referenceFactory.getReference(strand, frame);
    
    
    // Reference for the current frame 
    AminoAcidRenderer aar = new AminoAcidRenderer(frame);
    TierI tier = new Tier(referenceAA, strand, frame);
    AbstractTierPanel panel = new TierPanel(tier, orientation, aar);
    addPanel(panel);
    
    // The following three tiers show want bases make up the reference AA
    SequenceI referenceDNA = referenceFactory.getReference(strand);
    AminoAcidBaseRenderer aabr = 
      new AminoAcidBaseRenderer(frame.getIndex() + 0, orientation);
    aabr.setBackground(dnaColor);
    tier = new Tier(referenceDNA, strand, ReadingFrame.NONE);
    panel = new TierPanel(tier, orientation, aabr);
    addPanel(panel);
    
    aabr = new AminoAcidBaseRenderer(frame.getIndex() + 1, orientation);
    aabr.setBackground(dnaColor);
    tier = new Tier(referenceDNA, strand, ReadingFrame.NONE);
    panel = new TierPanel(tier, orientation, aabr);
    addPanel(panel);
    
    aabr = new AminoAcidBaseRenderer(frame.getIndex() + 2, orientation);
    aabr.setBackground(dnaColor);
    tier = new Tier(referenceDNA, strand, ReadingFrame.NONE);
    panel = new TierPanel(tier, orientation, aabr);
    addPanel(panel);
    
    //this.setReadingFrame(frame);
    this.reformat(false);
  }
  
  public void reformat(boolean isRecursive) {
    
    Orientation orientation = annotationPanel.getOrientation();
    Strand strand = annotationPanel.getStrand();
    ReadingFrame frame = annotationPanel.getReadingFrame();
    
    // The AA Reference for the current strand 
    SequenceI referenceAA = 
      referenceFactory.getReference(strand, frame);
    
    // Reference for the current frame
    AbstractTierPanel panel = getPanel(0);
    panel.getTier().setStrand(strand);
    panel.getTier().setReference(referenceAA);
    panel.setOrientation(orientation);
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(referenceAA.getLength() 
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    // The following three tiers show want bases make up the reference AA
    SequenceI referenceDNA = referenceFactory.getReference(getStrand());
    AminoAcidBaseRenderer aabr = 
      new AminoAcidBaseRenderer(frame.getIndex() + 0, orientation);
    aabr.setBackground(dnaColor);
    panel = getPanel(1);
    panel.setRenderer(aabr);
    panel.getTier().setStrand(strand);
    panel.getTier().setReference(referenceDNA);
    panel.setOrientation(orientation);
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(referenceAA.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    panel = getPanel(2);
    aabr = new AminoAcidBaseRenderer(frame.getIndex() + 1, orientation);
    aabr.setBackground(dnaColor);
    panel.setRenderer(aabr);
    panel.getTier().setStrand(strand);
    panel.getTier().setReference(referenceDNA);
    panel.reformat(true);
    panel.setOrientation(orientation);
    panel.setPreferredSize(new Dimension(referenceAA.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    panel = getPanel(3);
    aabr = new AminoAcidBaseRenderer(frame.getIndex() + 2, orientation);
    aabr.setBackground(dnaColor);
    panel.setRenderer(aabr);
    panel.getTier().setStrand(strand);
    panel.getTier().setReference(referenceDNA);
    panel.setOrientation(orientation);
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(referenceAA.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    super.reformat(isRecursive);
  }

}
