package apollo.gui.detailviewers.sequencealigner.DNAPanel;

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
import apollo.gui.detailviewers.sequencealigner.FeaturePlaceFinder.Type;
import apollo.gui.detailviewers.sequencealigner.renderers.TestBaseRenderer;

public class ReferencePanel extends MultiTierPanel {
  
  ReferenceFactoryI referenceFactory;
  MultiTierPanel annotationPanel;
  MultiTierPanel resultPanel;

  public ReferencePanel(ReferenceFactoryI referenceFactory, 
      MultiTierPanel annotationPanel, MultiTierPanel resultPanel) {
    super(FeaturePlaceFinder.Type.COMPACT);
    this.referenceFactory = referenceFactory;
    this.annotationPanel = annotationPanel;
    this.resultPanel = resultPanel;
    
    // set the reference for the annotations
    SequenceI sequence = 
      referenceFactory.getReference(annotationPanel.getStrand());
    TierI tier = 
      new Tier(sequence, annotationPanel.getStrand(), ReadingFrame.NONE);
    AbstractTierPanel panel = 
      new TierPanel(tier, annotationPanel.getOrientation(), 
                    new TestBaseRenderer());
    this.addPanel(panel);

    // set the reference for the results
    sequence = referenceFactory.getReference(resultPanel.getStrand());
    tier = new Tier(sequence, resultPanel.getStrand(), ReadingFrame.NONE);
    panel = 
      new TierPanel(tier, resultPanel.getOrientation(), new TestBaseRenderer());
    this.addPanel(panel);
    
    /*
    if (annotationPanel.getStrand() == resultPanel.getStrand()) {
      panel.setOrientation(orientation);
    } else {
      panel.setOrientation(orientation.getOpposite());
    }
    */
    this.reformat(true);
  }
  
  public void reformat(boolean isRecursive) {
    
    getTier(0).setReference(
        referenceFactory.getReference(annotationPanel.getStrand()));
    getTier(0).setStrand(annotationPanel.getStrand());
    getPanel(0).setOrientation(annotationPanel.getOrientation());
    
    getTier(1).setReference(
        referenceFactory.getReference(resultPanel.getStrand()));
    getTier(1).setStrand(resultPanel.getStrand());
    getPanel(1).setOrientation(resultPanel.getOrientation());
    
    super.reformat(isRecursive);
  }


}
