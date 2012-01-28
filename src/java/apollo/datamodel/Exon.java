package apollo.datamodel;

import java.util.Vector;
import org.bdgp.util.DNAUtils;

public class Exon extends AnnotatedFeature implements ExonI {

  // Should validate feature here.
  public Exon(SeqFeatureI sf) {
    super(sf);
    setup();
  }

  public Exon () {
    super ();
    setup();
  }

  public boolean isExon() { return true; }

  private void setup() {
    setFeatureType ("exon");
    annotationRoot = false;
  }

  public boolean canHaveChildren() {
    return false;
  }

  /** Overrides SeqFeature.getFrame.
      @return 1, 2, or 3 for frames, -1 if not in coding.
      The frame is relative to the ref sequence, not the feature start.
      In contrast getPhase returns 0,1,2 in relation to beginning of feature.
      @see #getPhase
      should this return -1 if outside of region?
  */
  public int getFrame() {
    if (!containsCoding())
      return -1;
    Transcript t = (Transcript) getRefFeature();
    // This should be covered by containsCoding, but just in case
    if (!t.hasTranslationStart())
      return -1;
    int frame;
    // 1st coding exon
    if (isFirstCodingExon()) {
      // is refseq the cur set seq?
      int start = t.getTranslationStart();
      frame = getRefSequence().getFrame(start,isForwardStrand());
    } else { // coding exon, not first
      int firstCodon = getStart() + getPhase() * getStrand();
      frame = getRefSequence().getFrame(firstCodon,isForwardStrand());
    }
    // AbsSeq.getFrame returns 0,1,2. Add 1 for 1,2,3.
    return frame + 1;
 }
  
  private boolean isFirstCodingExon() {
    Transcript t = (Transcript) getRefFeature();
    if (!t.hasTranslationStart())
      return false;
    else
      return contains(t.getTranslationStart());
  }

  /** Returns true if any part of exon is outside of ref sequence range */
  private boolean outsideOfRegion() {
    if (getRefSequence() == null) {
      // This shouldn't really happen--I think we're hitting it because
      // of a bug in the new chado xml adapter. --NH
      System.out.println("Exon.outsideOfRegion: ref sequence is null for exon " + getId());
      return false;
    }
    return !getRefSequence().getRange().contains(this);
  }

  public String toString() {
    // replaced phase with frame
    String frame = (getFrame() == -1) ? "non coding" : "frame "+ getFrame();
    // If outside of ref seq range dont put up frame
    if (outsideOfRegion()) 
      frame = "outside of region";
    return getFeatureType() + " " + getRefFeature() + " ("+frame+"): " 
      + getStart() + "-" + getEnd();
  }


  private static final String acceptor_site = "AG";
  private static final String donor_site = "GT";

  public String getAcceptor() {
    return getRefSequence().getResidues(getStart()-(2*getStrand()),
                                        getStart()-(getStrand()));
  }

  public String getDonor() {
    return getRefSequence().getResidues(getEnd()+getStrand(),
                                        getEnd()+(2*getStrand()));
  }

  public boolean isNonConsensusAcceptor() {
    boolean odd = false;
    if (isProteinCodingGene() &&
        isSequenceAvailable(getStart())) {
      if (getTranscript().getFeatureIndex(this) > 0) {
        String acc = getAcceptor();
        odd = (acc != null) && !acc.equals(acceptor_site);
      }
    }
    return odd;
  }

  public boolean isNonConsensusDonor() {
    boolean odd = false;
    if (isProteinCodingGene() &&
        isSequenceAvailable(getEnd())) {
      if (getTranscript().getFeatureIndex(this) < (getTranscript().size()-1)) {
        String don = getDonor();
        odd = !don.equals(donor_site);
      }
    }
    return odd;
  }

  /** Convenience function that uses getCodingProperties() 
      and returns true if its codingProperty has coding */
  public boolean containsCoding() {
    int cp = getCodingProperties();
    return (cp == CodingPropertiesI.MIXED_BOTH || 
            cp == CodingPropertiesI.MIXED_5PRIME || 
            cp == CodingPropertiesI.MIXED_3PRIME || 
            cp == CodingPropertiesI.CODING);
  }


  /** Exon has the same biotype as its gene */
  public String getTopLevelType() { 
    if (getRefFeature() != null &&
	getRefFeature().getRefFeature() != null)
      return getRefFeature().getRefFeature().getTopLevelType();
    /* this happens if the exon is used in a drag */
    else {
      if (biotype == null || biotype.equals (""))
	return getFeatureType();
      else
	return biotype;
    }
  }

  /* to avoid the embarassment of public casting */
  public Transcript getTranscript() {
    return (Transcript) getRefFeature();
  }

  public boolean isProteinCodingGene() {
    return getTranscript().isProteinCodingGene();
  }

  // 2/2005: Don't construct a particular kind of name here; just let the
  // super method return the assigned name (if any).
  // Exon name assignment should happen as data is read in.
//   public String getName() {
//     Transcript transcript = getTranscript();
//     if (isProteinCodingGene())
//       return (transcript.getName() +
//               " exon " + (transcript.getFeatureIndex(this) + 1));
//     else
//       return (transcript.getName());
//   }
}
