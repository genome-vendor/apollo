package apollo.gui.detailviewers.sequencealigner;

import java.util.Collection;
import java.util.List;
import org.bdgp.util.Range;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.editor.AnnotationChangeListener;

/**
 * A Tier is a container class that holds a set of non overlapping
 * SeqFeatureI's.
 * 
 * When referencing a position the 0th position corresponds to the first base in
 * the reference sequence.
 * 
 * The level of a tier is related to the different perspectives at which you can
 * look at a tier. The LEVEL_TOP is a view of the tier at a transcript level
 * while the LEVEL_BOTTOM corresponds to the exon level view. At each level a
 * position is associated with a range, if that range corresponds to a feature
 * then a position is also associated to that feature.
 * 
 * The strand of a tier identifies how to translate from a tier position to a
 * chromosome relative position.
 * 
 * When adding features to a strand they should all be of the same strand type,
 * and phase. Additionally all added features should be within the bounds of the
 * reference sequence.
 * 
 * Are type and phase needed?
 */
public interface TierI extends Range, AnnotationChangeListener {

  // gene?
  public enum Level {
    TOP, BOTTOM
  };

  // Levels can be transcript or exon (are there more general names?)
  // assume 2 level only maybe TOP/BOTTOM

  public SequenceI getReference();
  
  public void setReference(SequenceI s);

  public char charAt(int pos);

  public Range rangeAt(int position, Level level);

  public boolean addFeature(SeqFeatureI feature);

  public Collection<SeqFeatureI> addFeatures(Collection<SeqFeatureI> features);

  public void removeFeature(SeqFeatureI feature);

  public int numFeatures(Level level);

  public boolean featureExsitsAt(int position, Level level);

  public SeqFeatureI featureAt(int position, Level level);

  public SeqFeatureI featureIn(Range range, boolean exact, Level level);

  public Collection<SeqFeatureI> featuresIn(Range range, Level level);

  public int getBasePair(int position);

  public int getPosition(int basePair);

  public Strand getStrand();
  
  public void setStrand(Strand s);
  
  public ReadingFrame getReadingFrame();
  
  public void setReadingFrame(ReadingFrame rf);
  
  public void clear();

  public Collection<SeqFeatureI> getFeatures(Level level);

  public boolean willOverlap(SeqFeatureI feature);
  
  public int getBoundaryType(int position, Level level);
  
  public SeqFeatureI getNextFeature(int position, Level level);
  
  public SeqFeatureI getPrevFeature(int position, Level level);
  
  public SequenceType getType();
  
  

  // public int baseTypeAt(int position);

  /**
   * SeqAlignPanel.NO_BOUNDARY SeqAlignPanel.LEFT_BOUNDARY
   * SeqAlignPanel.RIGHT_BOUNDARY
   * 
   * what about left and right?
   */
  // public int boundaryTypeAt(int pos, Level level);
  // throw
  /*
   * public void setVisible(boolean visible); public boolean isVisible();
   * 
   * public void setRenderer(BaseRenderer rend); public BaseRenderer
   * getRenderer();
   * 
   */

  // i think the only reason people use this is to get the feature for
  // a particular range. in that case they should use featuresIn
  /*
   * public int indexOf(Range range, int level); public int indexOf(Range range,
   * boolean exact, int level); public Range getRange(int index, int level); //i
   * dont know about this... public SeqFeatureI featureAtIndex(int index, int
   * level); // TODO: dont really add
   */

  /** looks for FeatureSet (only returns featureSets) */
  /*
   * public FeatureSetI getFeatureSetAtPosition(int pos); int basePairToPos(int
   * bp);
   */
}
