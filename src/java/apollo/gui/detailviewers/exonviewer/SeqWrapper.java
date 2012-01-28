package apollo.gui.detailviewers.exonviewer;

import java.util.Enumeration;
import java.util.Vector;

import org.bdgp.util.RangeHash;

import apollo.datamodel.FeatureSetI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;

/** SeqWrapper wraps a SequenceI, i believe offset is not
    chromosomal, but offset just from the curation set, in other words
    a local offset not a global offset
*/
class SeqWrapper {
  protected SequenceI seq;
  protected int phase;
  protected BaseRenderer rend;
  protected String seq_type = SequenceI.DNA;
  protected RangeHash featureSets;
  private RangeHash seqFeatures;
  protected boolean visible = true;
  private SeqAlignPanel seqAlignPanel;

  public SeqWrapper(SequenceI seq, int phase, String seq_type,
                    SeqAlignPanel panel) {
    this(seq,
         phase,
         seq_type,
         new DefaultBaseRenderer(panel.getCharWidth(),panel.getCharHeight()),
         panel);

  }

  private SeqWrapper(SequenceI seq, 
                    int phase, 
                    String seq_type,
                    BaseRenderer rend,
                     SeqAlignPanel panel) {
    this.seq = seq;
    this.phase = phase;
    this.seq_type = seq_type;
    this.rend = rend;
    featureSets = new RangeHash();
    seqFeatures = new RangeHash();
    seqAlignPanel = panel;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isVisible() {
    return visible;
  }

  public BaseRenderer getRenderer() {
    return rend;
  }

  public void setRenderer(BaseRenderer rend) {
    this.rend = rend;
  }

  public SequenceI getSequence() {
    return seq;
  }

  /** @param i i is in pos/row/col coords NOT base pairs,
      @return  true if sequence exists at this point */
  private boolean existsAt(int i) {
    int pos = i;
    if (seq.hasResidueType() && seq.getResidueType().equals(SequenceI.AA))
      pos = i / 3;
    return pos >= 0 && pos < seq.getLength();
  }

  /**
   *
   * @param pos is in pos/row/col coords NOT base pairs,
   *  and it is zero based
   * @return the base or amino acid at that position in
   *  sequence
   */
  public char getCharAt(int pos) {
    if (existsAt(pos)) {
      if (seq.hasResidueType() &&
          seq.getResidueType().equals (SequenceI.AA)) {
        if (phase == (pos % 3)) {
          return seq.getBaseAt(pos / 3);
        }
      } else {
        /* this is a bit of secret knowledge here
           and isn't very nice.
           The peptide sequence above are created
           just for these displays and are not
           sequences of the abstract type.
           But, the genomic sequence may be an abstract
           sequence and if it is then the pos (based
           at 0) must be turned into genomic coords
        */
        if (seq_type.equals(SequenceI.AA)) {
          if ((phase == (pos % 3)) &&
              (pos + 2 <= seq.getLength())) {
            String codon = seq.getResidues (pos, pos + 2);
            return codon.charAt(0);
          }
        } else {
          int index = seqAlignPanel.posToResidue(pos);
          if (index <= seqAlignPanel.getLongestSeq())
            return seq.getBaseAt(index);
          else
            return '\0';
        }
      }
    }
    return '\0';
  }

  public Vector getFeaturesInRange(int startpos,
                                   int endpos) {
    Vector features = seqFeatures.get(startpos,
                                      endpos);
    if (features.size() == 0)
      features = featureSets.get(startpos,
                                 endpos);
    Vector out = new Vector();
    for(int i=0; i < features.size(); i++)
      out.addElement(((FeatureWrapper) features.elementAt(i)).
                     getFeature());

    return out;

  }

  public SeqFeatureI getFeatureAtPosition(int pos) {
    FeatureWrapper out = (FeatureWrapper) seqFeatures.get(pos);
    if (out == null)
      out = (FeatureWrapper) featureSets.get(pos);
    if (out == null)
      return null;
    else
      return out.getFeature();
  }

  public FeatureSetI getFeatureSetAtPosition(int pos) {
    FeatureWrapper out = (FeatureWrapper) featureSets.get(pos);
    if (out != null &&
        out.getFeature().canHaveChildren())
      return (FeatureSetI) out.getFeature();
    else
      return null;
  }

  protected FeatureWrapper getFeatureWrapperAtPosition(int pos) {
    FeatureWrapper out = (FeatureWrapper) seqFeatures.get(pos);

    if (out == null)
      out = (FeatureWrapper) featureSets.get(pos);
    if (out == null)
      return null;
    else
      return out;
  }

  public int getTypeAtPosition(int pos) {
    SeqFeatureI feature = getFeatureAtPosition(pos);
    if (feature == null)
      return SeqAlignPanel.NO_TYPE;
    else if (feature.canHaveChildren())
      return SeqAlignPanel.INTRON;
    else
      return SeqAlignPanel.EXON;
  }

  public double [] getRangeAtPosition(int pos) {
    double [] output = seqFeatures.getInterval(pos);
    return output;
  }

  public int getRangeIndex(int low, int high) {
    return featureSets.getIntervalIndex(low,high);
  }

  public int getExonRangeIndex(int low, int high) {
    return seqFeatures.getIntervalIndex(low, high);
  }

  public int getExonRangeIndex(int low, int high, boolean exact) {
    return seqFeatures.getIntervalIndex(low, high, exact);
  }

  public int getFeatureCount() {
    return seqFeatures.size();
  }

  public SeqFeatureI getFeatureAtIndex(int index) {
    if (index < 0 || index >= seqFeatures.size())
      return null;
    else
      return (SeqFeatureI) ((FeatureWrapper) seqFeatures.
                            getItemAtIndex(index)).getFeature();
  }

  public int getBoundaryType(int pos) {
    FeatureWrapper fw = getFeatureWrapperAtPosition(pos);
    if (fw == null)
      return SeqAlignPanel.NO_BOUNDARY;
    else if (fw.getLow() == pos)
      return SeqAlignPanel.LEFT_BOUNDARY;
    else if (fw.getHigh() == pos)
      return SeqAlignPanel.RIGHT_BOUNDARY;
    else
      return SeqAlignPanel.NO_BOUNDARY;
  }

  public void addFeature(SeqFeatureI in) {
    if (in.hasKids()) { //in.canHaveChildren()) { // 1 level annots can but dont
      FeatureSetI fs = (FeatureSetI) in;
      // In a feature wrapper every one is
      // oriented 5' to 3' just as it is
      // in the display. In other words the
      // start position of the feature is
      // always the low end.
      FeatureWrapper wrap = new FeatureWrapper(fs, this);
      featureSets.put(wrap, wrap);
      for(int i=0; i < fs.getFeatures().size(); i++) {
        SeqFeatureI sf = (SeqFeatureI) fs.getFeatures().elementAt(i);
        FeatureWrapper fwrap = new FeatureWrapper(sf, this);
        seqFeatures.put(fwrap, fwrap);
      }
    } else {
      FeatureWrapper fw = new FeatureWrapper(in,this);
      seqFeatures.put(fw,fw);
      if (seqAlignPanel.isOneLevelAnnot(in))
        featureSets.put(fw,fw);
    }
  }


  public void removeFeature(FeatureSetI fs) {
    Enumeration e = featureSets.values();
    featureSets = new RangeHash();
    seqFeatures = new RangeHash();
    // Can NOT use position because deleted features
    // no longer have valid start and end coordinates
    while(e.hasMoreElements()) {
      SeqFeatureI sf = ((FeatureWrapper) e.nextElement()).getFeature();

      if (sf != fs &&
          /*
          (sf.isClone() && sf.getCloneSource() != fs) &&
          (fs.isClone() && fs.getCloneSource() != sf)) {
          */
          !sf.getId().equals(fs.getId())) {

        /*
        if (sf.getName().equals(fs.getName())) {
          System.out.printf("%s(%d)\t%s(%d)\n", sf.toString(), sf.hashCode(), fs.toString(), fs.hashCode());
          System.out.printf("%s(%d)\t%s(%d)\n", sf.getCloneSource(), sf.getCloneSource() != null ? sf.getCloneSource().hashCode() : 0,
              fs.getCloneSource(),  fs.getCloneSource() != null ? fs.getCloneSource().hashCode() : 0);
          System.exit(1);
        }
        */

        /*
        if (fs.isClone() && fs.getCloneSource() == sf ||
            sf.isClone() && sf.getCloneSource() == fs) {
          continue;
        }
        */
        
        /*
        if (sf.getName().equals(fs.getName())) {
          System.out.printf("data:\t%s(%d)\t%s(%d)\n", sf.toString(), sf.hashCode(), fs.toString(), fs.hashCode());
          System.out.printf("clone:\t%s(%d)\t%s(%d)\n", sf.getCloneSource(), sf.getCloneSource() != null ? sf.getCloneSource().hashCode() : 0,
              fs.getCloneSource(),  fs.getCloneSource() != null ? fs.getCloneSource().hashCode() : 0);
          System.exit(1);
        }
        */
        
        addFeature(sf);
      }
    }
  }

  public Vector getFeatures() {
    Vector out = new Vector();
    Enumeration e = featureSets.values();
    while(e.hasMoreElements())
      out.addElement(e.nextElement());
    return out;
  }

  int basePairToPos(int bp) { return seqAlignPanel.basePairToPos(bp); }

} // end of SeqWrapper
