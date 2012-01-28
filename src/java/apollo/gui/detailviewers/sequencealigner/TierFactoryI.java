/**
 * 
 */
package apollo.gui.detailviewers.sequencealigner;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;

/**
 * 
 * 
 */
public interface TierFactoryI {
  
  public abstract TierI makeTier();
  
  public abstract TierI makeTier(SequenceI reference, Strand s, ReadingFrame rf);
  /*
  
  public abstract TierI makeTier(Strand s, ReadingFrame rf);

  public abstract TierI makeTier(SeqFeatureI f, SequenceType t);

  public abstract TierI makeTier(SequenceI reference, Strand s);

  public abstract TierI makeTier(SequenceI reference, SeqFeatureI f);

  public abstract TierI makeTier(SequenceI reference, Strand s, ReadingFrame f);
  
  */

}
