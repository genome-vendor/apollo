package jalview.datamodel;

import apollo.datamodel.SeqFeatureI;
import java.util.HashMap;
import apollo.util.FeatureList;

/** An alignment in a block, an alignment in a block is made of all
    the kids with the same parent smerged alignment seq. */
class BlockAlignment {
  /** All kid feats with common parent represented in this object */
  private SeqFeatureI parent;
  /** Has alignments for kid feats, would like to make new obj for this stuff*/
  private HashMap featToAlignment; 
  private FeatureList kidFeats;
  private String alignSeq;
  private int genomicStart;
  private int genomicEnd;

  BlockAlignment(SeqFeatureI parent,HashMap featToAlignment) {
    this.parent = parent;
    this.featToAlignment = featToAlignment;
    kidFeats = new FeatureList(3);
    kidFeats.setAlignablesOnly(true);
  }

  SeqFeatureI getParent() { return parent; }

  void addFeature(SeqFeatureI kid) {
    kidFeats.addFeature(kid);
  }
    
  int getGenomicStart() {
    return kidFeats.getFeature(0).getStart(); // will this work for rev strand?
  }
  int getGenomicEnd() {
    return kidFeats.getFeature(kidFeats.size()-1).getEnd(); // rev?
  }

  String getAlignment() {
    if (alignSeq == null) {
      alignSeq = makeAlignSeq();
    }
    return alignSeq;
  }
  /** Go through kidFeats and smerge aligns into one alignment */
  private String makeAlignSeq() {
    kidFeats.sortByStart();
    SeqFeatureI first_kid = kidFeats.getAlignable(0);
    String kidsAlignSeq = (String) featToAlignment.get(first_kid);
    // smerge all other kids alignments
    for (int i = 1; i < kidFeats.size(); i++) {
      SeqFeatureI kid = kidFeats.getAlignable(i);
      String seq = (String) featToAlignment.get(kid);
      kidsAlignSeq = smerge (kidsAlignSeq, seq);
      // why bother,not used anymore,unless redundant kids
      //featToAlignment.put (kid, kidsAlignSeq); 
    }
    return kidsAlignSeq;
  }

  /** If into has a dash(gap) or dot(padding) where from does not, 
      into replaces it with froms char */
  private String smerge (String into_seq, String from_seq) {
    char [] into = into_seq.toCharArray();
    char [] from = from_seq.toCharArray();
    int length = from_seq.length();

    for (int i = 0; i < length; i++) {
      if ((into[i] == '-' && from[i] != '-') 
          || (into[i] == '.' && from[i] != '.' ))
        into[i] = from[i];
    }
    return (new String (into));
  }

} 
