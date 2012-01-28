package apollo.gui.detailviewers.sequencealigner;

public class Utils {
  
  private static final int MIN_FEATURE_SIZE = 1;
  
  /**
   * returns the positions of the donor and acceptor
   * splice sites in coordinates relative to the genomic
   * sequence. The index of 0 points to the lower coord
   * whether or not it is the donor
   * RAY: From BaseEditorPanel
   */
  
  // what we want is a method that will find the splice sites within a genomic region
  // signiture RangeI findSplices(bp, strand, low, high, type)
  // type could be GT-AG, AT-AC
  // does strand matter... should reverse strand look for 
  public static int[] findSplices (MultiTierPanel panel, int pos, int strand, 
      boolean add_exon, int low_limit, int high_limit) {
    int start_pos = Math.max (pos - 12, (int) low_limit + 1);
    int end_pos = Math.min (pos + 12, (int) high_limit - 1);
    int dna_start = panel.tierPositionToBasePair(start_pos);
    int dna_end = panel.tierPositionToBasePair(end_pos);
    int donor;
    int acceptor;
    boolean well_ordered;
    String dna = panel.getTier(0).getReference().getResidues(dna_start, dna_end);
    if (add_exon) { // find donor/acceptor for exon
      //RAY is this true on the reverse strand?
      // inserting an exon
      // exon starts after the AG
      acceptor  = dna.indexOf("AG");
      // exon ends before the GT
      donor = dna.indexOf ("GT", (int) (dna.length()*0.5));
      well_ordered = donor != -1 && acceptor != -1 && (acceptor < donor);
    } else { // find donor/acceptor for an intron
      // inserting an intron
      // exon ends before the GT
      donor = dna.indexOf ("GT");
      // exon starts after the AG
      acceptor  = dna.indexOf("AG", (int) (dna.length()*0.5));
      well_ordered = donor != -1 && acceptor != -1 && (donor < acceptor);
    }
    if (!well_ordered) {
      if (add_exon) {
        acceptor = panel.tierPositionToBasePair(pos);
        donor = panel.tierPositionToBasePair(pos);
      } else {
        donor = panel.tierPositionToBasePair(pos - 1);
        acceptor = panel.tierPositionToBasePair(pos + 1);
      }
    } else {
      donor = panel.basePairToTierPosition(donor - 1 + dna_start);
      acceptor = panel.basePairToTierPosition(acceptor + 2 + dna_start);
    }
    int splices[] = new int[2];
    splices[0] = donor;
    splices[1] = acceptor;
    
    return splices;
  }

  public static boolean notTooSmall (int start_pos, int end_pos) {
    return (end_pos - start_pos + 1) >= MIN_FEATURE_SIZE;
  }
  
}
