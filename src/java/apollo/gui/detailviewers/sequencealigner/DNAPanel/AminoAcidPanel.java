package apollo.gui.detailviewers.sequencealigner.DNAPanel;

import java.awt.Dimension;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.AbstractTierPanel;
import apollo.gui.detailviewers.sequencealigner.FeaturePlaceFinder;
import apollo.gui.detailviewers.sequencealigner.MultiTierPanel;
import apollo.gui.detailviewers.sequencealigner.Orientation;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.ReferenceFactoryI;
import apollo.gui.detailviewers.sequencealigner.Strand;
import apollo.gui.detailviewers.sequencealigner.Tier;
import apollo.gui.detailviewers.sequencealigner.TierFactoryI;
import apollo.gui.detailviewers.sequencealigner.TierI;
import apollo.gui.detailviewers.sequencealigner.TierPanel;
import apollo.gui.detailviewers.sequencealigner.FeaturePlaceFinder.Type;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidRendererSpaced;

class AminoAcidPanel extends MultiTierPanel {
  
  private ReferenceFactoryI referenceFactory;
  
  public AminoAcidPanel(ReferenceFactoryI referenceFactory, 
      Orientation orientation, Strand strand) {
    super(FeaturePlaceFinder.Type.COMPACT);
    
    this.referenceFactory = referenceFactory;
    /* The DNA Reference for the current strand */
    SequenceI reference = 
      referenceFactory.getReference(strand, ReadingFrame.NONE);

    /* Frame 1 */
    AminoAcidRendererSpaced aar = new AminoAcidRendererSpaced(0);
    TierI tier1 = new Tier(
        referenceFactory.getReference(strand, ReadingFrame.ONE),
        strand,
        ReadingFrame.ONE);
    AbstractTierPanel tierPanel1 = 
      new TierPanel(tier1, orientation, aar);

    /* Frame 2 */
    aar = new AminoAcidRendererSpaced(1);
    TierI tier2 = new Tier(
        referenceFactory.getReference(strand, ReadingFrame.TWO),
        strand,
        ReadingFrame.TWO);
    AbstractTierPanel tierPanel2 = 
      new TierPanel(tier2, orientation, aar);

    /* Frame 3 */
    aar = new AminoAcidRendererSpaced(2);
    TierI tier3 = new Tier(
        referenceFactory.getReference(strand, ReadingFrame.THREE),
        strand,
        ReadingFrame.THREE);
    AbstractTierPanel tierPanel3 = 
      new TierPanel(tier3, orientation, aar);

    this.addPanel(tierPanel1);
    this.addPanel(tierPanel2);
    this.addPanel(tierPanel3);
    this.reformat(false);
  }
  
  public boolean canAddFeature(SeqFeatureI feature) {
    return false;
  }
  
  public void reformat(boolean isRecursive) {
    /* The DNA Reference for the current strand */
    SequenceI reference = 
      referenceFactory.getReference(getStrand(), ReadingFrame.NONE);
    
    AbstractTierPanel panel = getPanel(0);
    panel.getTier().setReference(
        referenceFactory.getReference(getStrand(), ReadingFrame.ONE));
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(reference.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    panel = getPanel(1);
    panel.getTier().setReference(
        referenceFactory.getReference(getStrand(), ReadingFrame.TWO));
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(reference.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    panel = getPanel(2);
    panel.getTier().setReference(
        referenceFactory.getReference(getStrand(), ReadingFrame.THREE));
    panel.reformat(true);
    panel.setPreferredSize(new Dimension(reference.getLength()
        * panel.getBaseWidth(), panel.getBaseHeight()));
    
    super.reformat(isRecursive);
  }

}
