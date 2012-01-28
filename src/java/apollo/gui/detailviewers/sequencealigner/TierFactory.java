package apollo.gui.detailviewers.sequencealigner;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;
import apollo.gui.detailviewers.sequencealigner.renderers.AminoAcidRenderer;

import java.util.Map;

import org.bdgp.util.DNAUtils;

public class TierFactory implements TierFactoryI {

  private ReferenceFactoryI referenceFactory;
  private Strand strand;
  private ReadingFrame readingFrame;

  public TierFactory(ReferenceFactoryI ref, Strand s, ReadingFrame rf) {
    this.referenceFactory = ref;
    this.strand = s;
    this.readingFrame = rf;
  }
  
  public TierI makeTier() {
    SequenceI reference = referenceFactory.getReference(strand, readingFrame);
    return new Tier(reference, strand, readingFrame);
  }
  
  public TierI makeTier(SequenceI reference, Strand s, ReadingFrame rf) {
    return new Tier(reference, s, rf);
  }
  
  public Strand getStrand() {
    return this.strand;
  }
  
  public void setStrand(Strand s) {
    this.strand = s;
  }
  
  public ReadingFrame getReadingFrame() {
    return this.readingFrame;
  }
  
  public void setReadingFrame(ReadingFrame rf) {
    this.readingFrame = rf;
  }

}
