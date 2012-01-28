package apollo.config;

import java.util.*;

import apollo.datamodel.*;

/**
 * A class detect overlapping transcripts for Drosophila
 * 
 */

public class ORF_Overlap implements OverlapI {

  public boolean areOverlapping(SeqFeatureI sa,SeqFeatureI sb) {
    boolean overlap = false;
    if (sb.canHaveChildren()) {
      FeatureSetI fb = (FeatureSetI)sb;
      for (int i=0; i<fb.size() && !overlap; i++) {
        overlap = areOverlapping (sa,fb.getFeatureAt(i));
      }
    }
    else if (sa.canHaveChildren()) {
      FeatureSetI fa = (FeatureSetI)sa;
      for (int i=0; i<fa.size() && !overlap; i++) {
        overlap = areOverlapping (fa.getFeatureAt(i),sb);
      }
    }
    else if (sa.overlaps(sb)) {
      /* the intervals of the sequences overlap, but
      this does not count unless the ORF overlaps
      UTRs are allowed to overlap and still be
      considered separate genes
      */
      SeqFeatureI sf1 = getCodingPortion (sa);
      SeqFeatureI sf2 = getCodingPortion (sb);
      if (sf1 != null && sf2 != null) {
        overlap = sf1.overlaps (sf2);
      }
    }
    return overlap;
  }

  private SeqFeatureI getCodingPortion (SeqFeatureI sf) {
    SeqFeatureI orf = null;
    boolean is_orf = false;

    // refFeature is null if this is a brand new seqFeature that has not been
    // added in yet - as is the case with AnnEd.createAnnotation 
    // crucial to test for null - MG
    AnnotatedFeatureI fs = (sf instanceof AnnotatedFeatureI ? 
                            (AnnotatedFeatureI) sf.getRefFeature() : null);
      
      // first find out if it is a gene because
      // overlap of genes only concerns the coding sequence
    if (fs != null && fs.isProteinCodingGene()) {
      int tss = fs.getTranslationStart();
      // this returns either the last base of the transcript or else
      // the last base position of the ORF (prior to stop codon)
      int tes = fs.getPositionFrom (fs.getTranslationEnd(), -1);
      is_orf |= (sf.getStrand() == 1 && 
		 tss > 0 &&
		 tss <= sf.getEnd() && 
		 tes >= sf.getStart());
      is_orf |= (sf.getStrand() == -1 && 
		 tss > 0 &&
		 tss >= sf.getEnd() && 
		 tes <= sf.getStart());
      if (is_orf) {
	orf = new SeqFeature (sf);
	if (tss != orf.getStart() && orf.contains (tss))
	  orf.setStart (tss);
	if (tes != orf.getEnd() && orf.contains (tes))
	  orf.setEnd (tes);
      }
    }
    // overlap of anything else is simply itself
    else {
      orf = sf;
    }
    return orf;
  }

}
