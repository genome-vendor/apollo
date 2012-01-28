/**
 * 
 */
package apollo.gui.detailviewers.sequencealigner;

import java.util.Collection;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

import org.bdgp.util.Range;
import org.bdgp.util.RangeHolder;
import org.bdgp.util.RangeHash;

import apollo.datamodel.FeatureSetI;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.editor.AnnotationChangeEvent;
import apollo.gui.detailviewers.sequencealigner.TierI.Level;

/**
 * A Tier is a container class that holds a set of non overlapping
 * SeqFeatureI's.
 * 
 * When referencing a position the 0th position corresponds to the first base in
 * the reference sequence.
 * 
 * The level of a tier is related to the different perspectives at which you can
 * look at a tier. The Level.TOP is a view of the tier at a transcript level
 * while the Level.BOTTOM corresponds to the exon level view. At each level a
 * position is associated with a range, if that range corresponds to a feature
 * then a position is also associated to that feature.
 * 
 * The strand of a tier identifies how to translate from a tier position to a
 * chromosome relative position.
 * 
 * When adding features to a tier they should all be of the same strand type,
 * and reading frame. Additionally all added features should be within the bounds of the
 * reference sequence.
 * 
 * Are type and phase needed?
 */
public class Tier implements TierI {

  /** The sequence this tier is based off of */
  private SequenceI reference;

  /** The strand of this tier (FORWARD or REVERSE) */
  private Strand strand;

  /** The type of sequence in this tier */
  private SequenceType type;

  /** The phase of this tier when it's sequence type is AA */
  private ReadingFrame readingFrame;

  /** The different levels this tier can be viewed from. */
  protected Map<Level, RangeHash> views;

  /**
   * Creates a new Tier
   * 
   * @param reference
   *          the sequence on which the tier is based
   * @param strand
   *          the strand of the tier
   * @param phase
   *          the reading frame of the tier
   */
  public Tier(SequenceI reference, Strand strand, ReadingFrame readingFrame) {
    this.reference = reference;
    this.strand = strand;
    this.type = SequenceType.valueOf(reference.getResidueType() == null ? "DNA" : reference.getResidueType());
    this.readingFrame = readingFrame;
    this.views = new HashMap<Level, RangeHash>();
    views.put(Level.TOP, new RangeHash());
    views.put(Level.BOTTOM, new RangeHash());
  }

  /**
   * Adds the feature to this Tier
   * 
   * @param feature
   *          the feature to be added
   * @return true if the feature was successfully added false otherwise
   * @see apollo.gui.detailviewers.exonviewer.TierI#addFeature(apollo.datamodel.SeqFeatureI)
   * 
   * Calls <code>willOverlap</code>
   */
  public boolean addFeature(SeqFeatureI feature) {
    if (willOverlap(feature)) {
      return false;
    }
    if (feature.isAnnot()) {
      addAnnotation(feature);
    } else {
      addResult(feature);
    }
    return true;
  }

  /**
   * Adds a collection of features to this tier
   * 
   * @param features
   *          a collection of features to be added
   * @return the collection of features which were not added
   * @see apollo.gui.detailviewers.exonviewer.TierI#addFeatures()
   * 
   * Calls <code>addFeature</code>
   */
  public Collection<SeqFeatureI> addFeatures(Collection<SeqFeatureI> features) {
    ArrayList<SeqFeatureI> extraFeatures = new ArrayList();
    for (SeqFeatureI feature : features) {
      if (!addFeature(feature)) {
        extraFeatures.add(feature);
      }
    }
    return extraFeatures;
  }

  /**
   * Returns the type of the base at a given position
   * 
   * @param position
   *          the position of the base which should be checked
   * @return a type of base
   * @see SeqAlignPanel
   * 
   * We should probably make an enumeration of the types. Are there cases where
   * we would want to have a base be of multiple types? If so maybe this should
   * be renamed.
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#baseTypeAt(int, int)
   * 
   * Calls <code>featureAt</code>
   */
  /*
   * public int baseTypeAt(int position) { SeqFeatureI feature =
   * featureAt(position, Level.BOTTOM); if (feature == null) { feature =
   * featureAt(position, Level.TOP); }
   * 
   * if (feature == null) { return SeqAlignPanel.NO_TYPE; } else if
   * (feature.canHaveChildren()) { return SeqAlignPanel.INTRON; } else { return
   * SeqAlignPanel.EXON; } }
   */

  /**
   * Returns the type of boundary of a base at a given position and level.
   * 
   * @param position
   *          the position to look at
   * @param level
   *          the level to look at
   * @return a type of boundary
   * @see apollo.gui.detailviewers.exonviewer.TierI#boundaryTypeAt(int, int)
   */
   public int getBoundaryType(int position, Level level) { 
     FeatureWrapper fw = featureWrapperAt(position, level); 
     if (fw == null) 
       return SeqAlignPanel.NO_BOUNDARY; 
     else if (fw.getLow() == position) // does this depend on the the strand? 
      return SeqAlignPanel.START_BOUNDARY; 
     else if(fw.getHigh() == position) 
       return SeqAlignPanel.END_BOUNDARY; 
     else return SeqAlignPanel.NO_BOUNDARY;
   }

  /**
   * Returns the base on the reference sequence for a given position
   * 
   * @param position
   *          the position of the base that is returned
   * @return a base character
   * @see apollo.gui.detailviewers.exonviewer.TierI#charAt(int)
   */
  public char charAt(int position) {
    int basePair = getBasePair(position);

    if (reference.getRange().getLow() <= basePair
        && basePair <= reference.getRange().getHigh()) {
      return reference.getBaseAt(basePair);
    } else {
      return '\0'; // TODO: throw out of range exception
    }
  }

  /**
   * Gets the feature that contains a given position
   * 
   * @param position
   *          the position that should be within the returned feature
   * @param level
   *          the level to view the tier at
   * @return the feature that contains the position or <code>null</code>
   * @see apollo.gui.detailviewers.exonviewer.TierI#featureAt(int, int)
   */
  public SeqFeatureI featureAt(int position, Level level) {
    FeatureWrapper wrapper = featureWrapperAt(position, level);
    SeqFeatureI feature = null;

    if (wrapper != null) {
      feature = wrapper.getFeature();
    }

    return feature;
  }

  /**
   * Determines if there exsists a feature at a given position
   * 
   * @param position
   *          the position to look for a feature at
   * @param level
   *          the level to view the tier at
   * @return true if <code>featureAt(position,level)</code> returns a feature
   *         false if it returns null
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#featureExsitsAt(int, int)
   */
  public boolean featureExsitsAt(int position, Level level) {
    return featureAt(position, level) != null;
  }

  /**
   * Gets the first(?) feature within a given range inclusive(?)
   * 
   * @param range
   *          a position range to look for features within
   * @param exact
   *          not sure...
   * @param level
   *          the level to view the tier at
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#featureIn(org.bdgp.util.Range,
   *      boolean, int)
   */
  public SeqFeatureI featureIn(Range range, boolean exact, Level level) {
    RangeHash view = viewAt(level);

    int index = view.getIntervalIndex(range.getLow(), range.getHigh(), exact);

    FeatureWrapper fw = ((FeatureWrapper) view.getItemAtIndex(index));
    return fw.getFeature();
  }

  /**
   * Gets all of the features within a given range
   * 
   * @param range
   *          a position range to look for the features within
   * @param level
   *          the level to view the tier at
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#featuresIn(org.bdgp.util.Range,
   *      int)
   */
  public Collection<SeqFeatureI> featuresIn(Range range, Level level) {
    RangeHash view = viewAt(level);
    ArrayList<SeqFeatureI> features = new ArrayList();
    Vector<FeatureWrapper> wrappers = view.get(range.getLow(), range.getHigh());
    for (FeatureWrapper wrapper : wrappers) {
      features.add(wrapper.getFeature());
    }
    return new ArrayList<SeqFeatureI>(features);
  }

  /**
   * Returns the sequence which is used as a reference for this tier.
   * 
   * @return the reference sequence for this tier
   * @see apollo.gui.detailviewers.exonviewer.TierI#getReference()
   */
  public SequenceI getReference() {
    return this.reference;
  }
  
  public void setReference(SequenceI s) {
    this.reference = s;
  }

  /**
   * Gets the number of features at a given level.
   * 
   * @param level
   *          the level to view the tier at
   * @return the number of features
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#numFeatures(int)
   */
  public int numFeatures(Level level) {
    return featuresIn(this, level).size();
  }

  /**
   * Returns the range that corresponds to a given position at a level. All of
   * the positions within the returned range will be of the same type at that
   * level.
   * 
   * @param position
   *          a position within the tier
   * @param level
   *          a level to view the tier at
   * @return a range of positions which all have the same type
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#rangeAt(int, int)
   */
  public Range rangeAt(int position, Level level) {
    RangeHash view = viewAt(level);
    double[] interval = view.getInterval(position);
    
    //
    if (interval[0] == Double.NEGATIVE_INFINITY) {
      interval[0] = getLow();
    }
    if (interval[1] == Double.POSITIVE_INFINITY) {
      interval [1] = getHigh();
    }
    return new RangeHolder((int) interval[0], (int) interval[1]);
  }

  /**
   * Removes a given feature from this tier.
   * 
   * @param feature
   *          the feature to be removed
   * 
   * @see apollo.gui.detailviewers.exonviewer.TierI#removeFeature(apollo.datamodel.SeqFeatureI)
   */
  // TODO: rework feature removal
  public void removeFeature(SeqFeatureI feature) {
    Enumeration e = viewAt(Level.TOP).values();
    clear();
    // Can NOT use position because deleted features
    // no longer have valid start and end coordinates
    while (e.hasMoreElements()) {
      SeqFeatureI sf = ((FeatureWrapper) e.nextElement()).getFeature();
      if (sf != feature) {
        addFeature(sf);
      }
    }
  }

  /**
   * Gets a collection of all the top level features in the tier
   * 
   * @return a collection of features
   * 
   */
  // TODO: make getFeatures take a level as a parameter
  public Collection<SeqFeatureI> getFeatures(Level level) {
    return featuresIn(this, level);
  }

  /**
   * Gets the highest position on this tier
   * 
   * @return the highest position
   * 
   * @see org.bdgp.util.Range#getHigh()
   */
  public int getHigh() {
    return Math.max(getPosition(this.reference.getRange().getHigh()),
        getPosition(this.reference.getRange().getLow()));
  }

  /**
   * Gets the lowest position on this tier
   * 
   * @return the lowest position
   * @see org.bdgp.util.Range#getLow()
   */
  public int getLow() {
    return Math.min(getPosition(this.reference.getRange().getHigh()),
        getPosition(this.reference.getRange().getLow()));
  }

  /**
   * Translates a position on the tier into base pair coordinates.
   * 
   * @param position
   *          a position within the tier
   * @return the base pair coordinate for that position
   */
  public int getBasePair(int position) {
    // do i need to update the endpoints of the features in annotation panel?
    if (this.type == SequenceType.AA) {
      int i = getReadingFrame().getIndex();
      position = position*3 + getReadingFrame().getIndex();
    }
    
    int h = reference.getRange().getHigh();
    int l = reference.getRange().getLow();
    
    int b = position + reference.getRange().getLow();
    // reverse strand numbering is backwards
    if (this.strand.equals(Strand.REVERSE))
      return reference.getRange().getHigh() - position;
    else // forward strand
      return position + reference.getRange().getLow();
  }

  /**
   * Translates a base pair coordinate into a position on the tier
   * 
   * @param basePair
   *          a base pair coordinate within the tier
   * @return a position within the tier
   */
  public int getPosition(int basePair) {
    int result = -1;
    if (this.strand.equals(Strand.REVERSE))
      result = reference.getRange().getHigh() - basePair;
    else // forward strand
      result = basePair - reference.getRange().getLow();
    
    if (this.type == SequenceType.AA) {
      int i = getReadingFrame().getIndex();
      //result -= getReadingFrame().getIndex();
      int j = result/3;
      result = result/3;
    }
    
    return result;
  }

  /**
   * Determine weather or not a feature will overlap with another feature
   * already in the tier. Two features overlap if they both contain a common set
   * of base pair locations.
   * 
   * @param feature
   *          the feature to be tested
   * @return true if the feature will overlap false otherwise
   */
  public boolean willOverlap(SeqFeatureI feature) {
    Range range = new FeatureWrapper(feature, this);
    Collection<SeqFeatureI> features = featuresIn(range, Level.TOP);
    return features.size() > 0;
  }

  /**
   * Gets the strand of this tier.
   * 
   * @return the strand of this tier
   */
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
  
  /**
   * gets the next feature in the 3' direction
   */
  public SeqFeatureI getNextFeature(int position, Level level) {
    SeqFeatureI feature = featureAt(position, level);
    
    if (feature != null) {
      position = getPosition(feature.getEnd()) + 1;
    }

    Range r = rangeAt(position, level);
    position = r.getHigh() + 1;

    return featureAt(position, level);

  }
  /** 
   * gets the next feature in the 5' direction
   */
  public SeqFeatureI getPrevFeature(int position, Level level) {
    SeqFeatureI feature = featureAt(position, level);
    
    if (feature != null) {
      position = getPosition(feature.getStart()) - 1;
    }

    Range r = rangeAt(position, level);
    position = r.getLow() - 1;

    return featureAt(position, level);
  }

  /*
   * Private methods
   * 
   */

  private void addAnnotation(SeqFeatureI annotation) {
    if (annotation.hasKids()) { // in.canHaveChildren()) { // 1 level annots can
                                // but dont
      addFeature(annotation, Level.TOP);
      Collection<SeqFeatureI> children = annotation.getFeatures();
      for (SeqFeatureI child : children) {
        addFeature(child, Level.BOTTOM);
      }
    } else {
      addFeature(annotation, Level.TOP);
      addFeature(annotation, Level.BOTTOM);
      /*
       * if (in.isAnnotTop() && !in.hasKids()) XXX is this ok?
       * featureSets.put(fw,fw);
       */
    }

  }

  // maybe results and annotations arn't really that different?
  private void addResult(SeqFeatureI result) {
    addFeature(result, Level.TOP);
    addFeature(result, Level.BOTTOM);
  }

  /**
   * In a feature wrapper every one is oriented 5' to 3' just as it is in the
   * display. In other words the start position of the feature is always the low
   * end.
   * 
   */
  private void addFeature(SeqFeatureI feature, Level level) {
    RangeHash view = viewAt(level);
    FeatureWrapper wrapper = new FeatureWrapper(feature, this);
    view.put(wrapper, wrapper); // maybe i can put the feature as the second
                                // argument?
  }

  private RangeHash viewAt(Level level) {
    RangeHash view = views.get(level);
    if (view == null) {
      throw new IllegalArgumentException("Invalid level:" + level);
    }
    return view;
  }

  private FeatureWrapper featureWrapperAt(int position, Level level) {
    return (FeatureWrapper) viewAt(level).get(position);
  }

  public void clear() {
    this.views.clear();
    views.put(Level.TOP, new RangeHash());
    views.put(Level.BOTTOM, new RangeHash());
  }

  public boolean handleAnnotationChangeEvent(AnnotationChangeEvent evt) {
    // TODO: optimize this will be slow when there are a lot of features
    Enumeration e = viewAt(Level.TOP).values();
    clear();
    // Can NOT use position because deleted features
    // no longer have valid start and end coordinates
    while (e.hasMoreElements()) {
      SeqFeatureI sf = ((FeatureWrapper) e.nextElement()).getFeature();
      addFeature(sf);
    }
    
    return true;
  }

  public SequenceType getType() {
    return this.type;
  }
  
}
