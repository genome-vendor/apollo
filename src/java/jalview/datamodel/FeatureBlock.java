package jalview.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Iterator;

import org.bdgp.util.DNAUtils;

import apollo.util.FeatureList;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;

/** Block tracks all the data associated with a block of overlapping features 
    for the CondensedAlignment and Mapper */
class FeatureBlock {
  private final static int pad    = 10;

  private String gappedGenomic; // formerly consensus
  private FeatureList features = new FeatureList();
  // put these in Range object?
  private int genomicLow; 
  private int genomicHigh; 
  private int jalviewLow;
  private int jalviewHigh;
  private boolean isForwardStrand;
  private SequenceI genomicSequence;

  /** BlockAlignmentList is an inner class that contains a list of BlockAlignments
    (another inner class). It also maps parents feats to their BlockAlignments.
    BlockAlignments represent an alignment in this block (of one or more kid feats 
    with the same parent feat)  */
  //private HashMap parentToAlignment; 
  private BlockAlignmentList blockAlignmentList;

  FeatureBlock(SeqFeatureI feat) { 
    addFeature(feat); 
    isForwardStrand = feat.getStrand() == 1;
    genomicSequence = feat.getRefSequence();
  }

  void addFeature(SeqFeatureI feat) {
    updateHighLow(feat.getLow(),feat.getHigh());
    features.addFeature(feat);
  }
  void addAlignable(SeqFeatureI feat) {
    updateHighLow(feat.getLow(),feat.getHigh());
    features.addAlignable(feat);
  }
  private SeqFeatureI getAlignable(int i) {
    return features.getFeature(i);
  }
    
  private FeatureList getFeatures() { return features; }
  int featSize() { return features.size(); }
  SeqFeatureI getFeature(int i) { return features.getFeature(i); }
  
  private void updateHighLow(int lo, int hi) {
    if (features.size() == 0) {
      genomicLow = lo; // start?
      genomicHigh = hi; // end?
    }
    else {
      if (lo < genomicLow) genomicLow = lo;
      if (hi > genomicHigh) genomicHigh = hi;
    }
  }
    
  boolean overlaps(SeqFeatureI sf) {
    return (sf.getLow() <= genomicHigh && sf.getHigh() >= genomicLow);
  }

  /** pad out for extra genomic for splice sites. This just changes genomic low and high,
   adding of pad seq not done here. */
  void pad() {
    genomicLow -= pad;
    if (genomicLow < 1) genomicLow = 1;
    // should check if greater than max seq....
    genomicHigh += pad;
    // add to the genomic seq if exists
    // gapped genomic has not been created yet at time of padding
//     if (gappedGenomic==null) return;
//     String prefix = genomicSequence.getResidues(genomicLow,genomicLow+pad);
//     String suffix = genomicSequence.getResidues(genomicHigh-pad,genomicHigh);
//     gappedGenomic = prefix + gappedGenomic + suffix;
  }

  void setJalviewLow(int low) {
    jalviewLow = low;
    jalviewHigh = low + getGappedLength(); // + 1?
  }

  int getJalviewLow() { return jalviewLow; }
  int getJalviewHigh() { return jalviewHigh; }

  /** do we still need this? - this is length without gaps */
  int length() { return genomicHigh - genomicLow; }

  int getGenomicLow() { return genomicLow; }
  int getGenomicHigh() { return genomicHigh; }
  int getGenomicStart() {
    return isForwardStrand ? genomicLow : genomicHigh;
  }
  int getGenomicEnd() {
    return isForwardStrand ? genomicHigh : genomicLow;
  }

  boolean containsGenomic(int genomic) {
    return genomic >= genomicLow && genomic <= genomicHigh;
  }

  boolean containsFeature(SeqFeatureI feat) {
    return containsGenomic(feat.getLow()) && containsGenomic(feat.getHigh());
  }

  boolean containsJalviewCoord(int jalview) {
    return jalview >= jalviewLow && jalview <= jalviewHigh;
  }

  /** Merge this block with mergeBlock */
  void merge(FeatureBlock mergeBlock) {
    if (mergeBlock.getGenomicLow() < genomicLow) 
      genomicLow = mergeBlock.getGenomicLow();
    if (mergeBlock.getGenomicHigh() > genomicHigh) 
      genomicHigh = mergeBlock.getGenomicHigh();
    jalviewHigh = jalviewLow + length();
    features.addAll(mergeBlock.getFeatures());
  }

  /** Returns BlockAlignment which has align string for kid(s)
      of parent. Returns null if dont have any kids of parent. */
  BlockAlignment getBlockAlignForParent(SeqFeatureI parent) {
    if (!alignmentDone()) doAlignment();
    //return (String)parentToAlignment.get(parent);
    return blockAlignmentList.getAlignForParent(parent);
  }
  /** returns genomic with gaps. First time its called it does the gapping
      for all the sequences -> rename getGappedGenomic! i guess getQueryAlignment from
      Alignable?*/
  public String getGappedGenomic() {
    if (!alignmentDone()) doAlignment();
    return gappedGenomic;
  }

  private boolean alignmentDone() { return gappedGenomic != null; }
   
  private void doAlignment() {
    int block_size = size();
    int block_start;
    int block_end;
    HashMap featToAlignment = new HashMap(block_size); // feat -> hit alignment
    //parentToAlignment = new HashMap(block_size);
      
    // are these sorted by starts?
    for (int i = 0; i < block_size; i++) {
      SeqFeatureI feature = getAlignable(i);
      // synchronize starts, padToStart returns a string of dots
      String leftPad = padToStart(feature);
      // This alignment is what is save and used
      String hit_alignment = leftPad + feature.getHitFeature().getAlignment();//getHitAlignment());
      // This alignment introduces any new gaps into the gappedGenomic
      // that are need by the above alignment of the feature
      //int start = feature.getStart();
      String ref_alignment = feature.getAlignment();

      // first time around - genomic prefix
      if (gappedGenomic == null) {
        String startPad = getGenomicStartPad();
        gappedGenomic = startPad + ref_alignment;
      }
      else {
        // not first feat - need leftPad that accounts for gaps and block start
        ref_alignment = leftPad + ref_alignment;
        // make all the consensus sequences agree on length
        // get position of first gap in genomic and ref_alignment
        int con_gap_pos = gappedGenomic.indexOf ('-', leftPad.length());
        int ref_gap_pos = ref_alignment.indexOf ('-', leftPad.length());
        boolean at_end = beyondEnd (con_gap_pos, 
                                    ref_gap_pos, 
                                    gappedGenomic.length(),
                                    ref_alignment.length());
        // while we have not hit the end (and there are gaps)
        // insert one gap per iteration
        while ((con_gap_pos >=0 || ref_gap_pos >= 0) && ! at_end) {
          int index = con_gap_pos + 1;
          // gaps not in same position
          if (ref_gap_pos != con_gap_pos) {
            if (con_gap_pos < 0 ||
                (ref_gap_pos >= 0 && ref_gap_pos < con_gap_pos)) {
              // need to insert a gap in existing alignments
              insertGapIntoAlignments (ref_gap_pos, featToAlignment);
              index = ref_gap_pos + 1;
            }
            else if (ref_gap_pos < 0 ||
                     (con_gap_pos >= 0 && con_gap_pos < ref_gap_pos)) {
              // need to insert a gap in the feature being
              // added to the block
              hit_alignment = insertGap (hit_alignment, con_gap_pos);
              ref_alignment = insertGap (ref_alignment, con_gap_pos);
            }
            else {
              System.out.println ("This should not have happened!!");
              at_end = true;
            }
          }
          // both gaps are in the same position
          // is con_gap_pos < 0 possible. means both gapless which would mean at end
          // wouldnt it?
          else if (con_gap_pos < 0) 
            at_end = true;
          
          if (!at_end) {
            ref_gap_pos = ref_alignment.indexOf ('-', index);
            con_gap_pos = gappedGenomic.indexOf ('-', index);
            at_end = beyondEnd (con_gap_pos, 
                                ref_gap_pos, 
                                gappedGenomic.length(),
                                ref_alignment.length());
          }
        }
        /*
        The existing consensus may be shorter than this
        new feature that is being added. If this is true
        then the new part of the alignment must be added 
        to the end of the consensus
        */
        if (ref_alignment.length() > gappedGenomic.length())
          // need to append to end of consensus
          extendConsensus (ref_alignment, featToAlignment);
        /*
        conversely the new feature may be a short one
        and not extend as far as the previous one, in
        which case the new alignment needs to have blanks
        inserted at the end of its reference sequence
        However this cannot be done until all of the
        differences in where gaps are placed is resolved
        (as done above)
        */
        else if (gappedGenomic.length() > hit_alignment.length())
          // need to pad out the new alignment
          hit_alignment = extendAlignment (hit_alignment, gappedGenomic);
      } // end of else
      
      featToAlignment.put (feature, hit_alignment);
    } // end of for loop on blocks
    
    // Now that the gapped genomic sequence is figured need to add 10 basepairs
    // at end of genomic, so user can see splice site stuff - 
    addGenomicEnd(featToAlignment);
    
    // now need to collapse any child feature pairs that are
    // matches to the same parent sequence as they belong on the same line
    // BlockAlignment holds the alignment of siblings, BlockAlignmentList
    // is just a liast of the BlockAlignments
    //HashMap parentToKids = new HashMap(block_size);
    blockAlignmentList = new BlockAlignmentList(block_size);
    Iterator keys = featToAlignment.keySet().iterator();
    while (keys.hasNext()) {
      SeqFeatureI kid = (SeqFeatureI) keys.next();
      SeqFeatureI parent = kid.getParent();
      //FeatureList kidList = (FeatureList) parentToKids.get(parent);
      //BlockAlignment kidsAlignment=(BlockAlignment)parentToKidsAlignment(parent);
      BlockAlignment kidsAlignment = blockAlignmentList.getAlignForParent(parent);
      //if (kidList == null) {
      // if we havent created a BlockAlignment for parent yet, create one and add to
      // block alignment list
      if (kidsAlignment == null) { 
        //kidList = new FeatureList();
        //kidList.setAlignablesOnly(true);
        kidsAlignment = new BlockAlignment(parent,featToAlignment);
        //parentToKids.put(parent, kidList);
        //parentToKidsAlignment.put(parent,kidsAlignment);
        blockAlignmentList.addBlockAlignment(kidsAlignment);
      }
      //kidList.addAlignable(kid);
      kidsAlignment.addFeature(kid);
    }
//     // If any of the parents have more than one kid, the kids get smerged into 
//     // one kidsAlignSeq String
//     //keys = parentToKids.keySet().iterator();
//     //while (keys.hasNext()) {
//     for (int i=0; i<blockAlignmentList.size(); i++) {
//       //SeqFeatureI parent = (SeqFeatureI) keys.next();
//       BlockAlignment blockAlign = blockAlignmentList.getBlockAlign(i);
//       //FeatureList kidList = (FeatureList) parentToKids.get (parent);
//       //FeatureList kidList = blockAlign. ??
//       //kidList.sortByStart(); // this is problematic as alignables
//       //blockAlign.sertByStart();
//       // get first kids alignment
//       SeqFeatureI first_kid = kidList.getAlignable(0);
//       String kidsAlignSeq = (String) featToAlignment.get(first_kid);
//       // smerge all other kids alignments
//       for (int i = 1; i < kidList.size(); i++) {
// 	SeqFeatureI sf = kidList.getAlignable(i);
// 	String seq = (String) featToAlignment.get(sf);
// 	kidsAlignSeq = smerge (kidsAlignSeq, seq);
// 	featToAlignment.put (sf, kidsAlignSeq); // why bother - not used anymore
//       }
//       // Now that kids are merged into kidsAlignSeq, map parent to kidsAlignSeq
//       //parentToAlignment.put (parent, kidsAlignSeq);
//     }

  } // end of doAlignment()



  /** List of all BlockAlignments for this block */
  private class BlockAlignmentList {
    private ArrayList blockAlignList = new ArrayList(1);
    private HashMap parentToBlockAlign = new HashMap(1);
    private BlockAlignmentList(int blockSize) {
      blockAlignList = new ArrayList(blockSize);
      parentToBlockAlign = new HashMap(blockSize);
    }
    private BlockAlignment getAlignForParent(SeqFeatureI parent) {
      return (BlockAlignment)parentToBlockAlign.get(parent);
    }
    private void addBlockAlignment(BlockAlignment blockAlignment) {
      blockAlignList.add(blockAlignment);
      parentToBlockAlign.put(blockAlignment.getParent(),blockAlignment);
    }
    private int size() { return blockAlignList.size(); }  
    private BlockAlignment getBlockAlign(int i) { 
      return (BlockAlignment)blockAlignList.get(i);
    }
  } // end of BlockAlignmentList class



  private int size() { return features.size(); }

  /** Returns a string of "." length of start of block to start of feat */
  private String padToStart(SeqFeatureI feature) {
    StringBuffer pad = new StringBuffer();
    //if (feature.getStart() != getStart()) {
    if (feature.getStart() != getGenomicStart()) {
      // have to figure out how much to add
      // this isn't straight forward because gaps have been
      // introduced and they must be accounted for as well
      //int offset = (Math.abs (feature.getStart() - getStart()));
      int offset = (Math.abs (feature.getStart() - getGenomicStart()));
      int index = 0;
      int i = 0; 
      //int gaps = 0; not really used 
      while (index < offset) {
  // gappedGenomic is null on first call to padToStart - thats ok just do
  // a regular non gapped padding
  if (gappedGenomic == null) 
    index++;
  // This can happen if feat start is beyond current block end(but close enough 
  // to be merged into same block)
  else if (i >= gappedGenomic.length()) 
    index++;
        /** Fixed a funny bug here. So ' '/space is never used as padding like dash is so
            theres no reason to not push up index if theres a space. The bug this creates
            is actually off of another bug. When aligning protein sequence the 
            gappedGenomic actually becomes gappedPeptide - although its unclear if this
            is undisreable - anyways as a peptide the gappedGenomic contains spaces 
            (2 between each amino) that are NOT gaps, while this was treating them 
            as gaps and padding too much. */
  else if (gappedGenomic.charAt(i) != '-' /*&& gappedGenomic.charAt(i) != ' '*/) 
    index++;
  //else gaps++; 
  //pad.append('-');
  pad.append('.');
  i++;
      }
    }
    return pad.toString();
  }

  /** Returns the sequence padding for the beginning of the block.
      Compensates for strand. */
  private String getGenomicStartPad() {
    // The genomicStart coord is already padded at this point
    if (isForwardStrand()) 
      return genomicSequence.getResidues(getGenomicStart(),getGenomicStart()+pad-1);
    // since start-pad+1 < start should come back revcomped
    else 
      return genomicSequence.getResidues(getGenomicStart(),getGenomicStart()-pad+1);
  }

  /** Now that the gapped genomic sequence is figured need to add 10 basepairs
      at end of genomic, so user can see splice site stuff - */
  private void addGenomicEnd(HashMap aligned_seqs) {
    //the genomic end has been padded already
    if (isForwardStrand())
      gappedGenomic += genomicSequence.getResidues(getGenomicEnd()-pad+1,getGenomicEnd());
    else 
      gappedGenomic += genomicSequence.getResidues(getGenomicEnd()+pad-1,getGenomicEnd());
    // all other features need to pad this part out with dots
    String dotPadding = "";
    for (int i=0; i<pad; i++) dotPadding += ".";
    Iterator seqs = aligned_seqs.entrySet().iterator();
    while (seqs.hasNext()) {
      Entry en = (Entry)seqs.next();
      en.setValue(((String)en.getValue()) + dotPadding);
    }
  }

  /** Does boundary checks: 
      a is first gap in gapped genomic
      b is first gap in ref alignment
      if a and b < 0 return true(no gaps).
      if a < b and b > 0 and a>=lengthb return true
      (how can a be less than b but greater than blength? doesnt b
      have to be less than blength??)
   if a >= 0 and b < 0 and a > length b return true
   if b<a and a>0 return true, if b>=0 and b>=lengtha true */
  private boolean beyondEnd (int indexA, int indexB,
           int lengthA, int lengthB) {
    boolean beyond = (indexA < 0 && indexB < 0);
    if (!beyond) 
      beyond = (((indexA < indexB && indexB > 0) ||
     (indexA >= 0 && indexB < 0)) &&
    indexA >= lengthB);
    if (!beyond)
      beyond = (((indexB < indexA && indexA > 0) || 
     (indexB >= 0 && indexA < 0)) &&
    indexB >= lengthA);
    return beyond;
  }

  /** Inserts a gap at gap_pos into the aligned seqs and the gapped genomic */
  private void insertGapIntoAlignments (int gap_pos, HashMap aligned_seqs) {
    gappedGenomic = insertGap (gappedGenomic, gap_pos);
    Iterator keys = aligned_seqs.keySet().iterator();
    while (keys.hasNext()) {
      SeqFeatureI sf = (SeqFeatureI)keys.next();
      String seq = (String) aligned_seqs.get(sf);
      seq = insertGap (seq, gap_pos);
      aligned_seqs.put (sf, seq);
    }
  }

  /** Inserts a "-" at the position in seq */
  private String insertGap (String seq, int position) {
    StringBuffer gapped_seq = new StringBuffer(seq.substring (0, position) +
                 '-');
                 //'%'); // debug
    if (position < seq.length())
      gapped_seq.append(seq.substring (position));
    return gapped_seq.toString();
  }


  /** Extends the gapped genomic and all of the aligned seqs with the suffix 
      of ref_alignment that is greater. The gapped_genomic gets extended by
      the suffix of the ref_alignment. The hit alignments get extended by
      a string of dashes  */
  private void extendConsensus (String ref_alignment, HashMap aligned_seqs) {
    String suffix = ref_alignment.substring (gappedGenomic.length());
    gappedGenomic = gappedGenomic + suffix;
    suffix = getPadding (suffix.length());
    Iterator keys = aligned_seqs.keySet().iterator();
    while (keys.hasNext()) {
      //SeqFeatureI sf = (SeqFeatureI) keys.next();
      SeqFeatureI sf = (SeqFeatureI)keys.next();
      String seq = (String) aligned_seqs.get(sf);
      seq = seq + suffix;
      aligned_seqs.put (sf, seq);
    }
  }

  /** Pads end of alignment with dots til just as long as consensus */
  private String extendAlignment (String alignment, String consensus) {
    int padLength = consensus.length() - alignment.length();
    // used to erroneously extend by consensus genomic rather than dots
    return (alignment + getPadding(padLength));
  }

  /** Returns a string of length dots - changed from dashes? */
  private String getPadding (int length) {
    StringBuffer pad = new StringBuffer(length);
    for (int i = 0; i < length; i++) {
      pad.append ('.'); //'*'); // * -> debug
    }
    return pad.toString();
  }

  /** Get the length of the block including gaps. */
  int getGappedLength() {
    return getGappedGenomic().length();
  }
  
  boolean hasTranslatedGenomic() {
    return genomicSequence != null && genomicSequence.getResidues() != null;
  }

  /** Return string of translated gapped genomic for genome_frame 
      rename getGappedTranslatedGenomic? */
  String getTranslatedGenomic(int genome_frame) {

    // make sure consensus is ready to go
    if (!alignmentDone()) doAlignment();
    // what frame is this block in?
    int block_frame = getFrame();
    int frame_offset = ((genome_frame + 3) - block_frame) % 3;
    int frame_extend = frame_offset + (3 - (length() % 3));
    int start = getGenomicStart() + (frame_offset * getStrand());
    int end = getGenomicEnd() + (frame_extend * getStrand());
    String frame_res = genomicSequence.getResidues (start, end);

    if (frame_res == null) // no genomic to translate -> exit
      return null;
    
    // introduce the gaps from the consensus sequence
    int index = gappedGenomic.indexOf ('-');
    while (index >= 0) {
      // add the frame because this string doesn't start where
      // the consensus did
      int frame_index = index - frame_offset;
      if (frame_index > frame_res.length()) {
        // Too annoying seeing all these error messages, so I've commented out
        // the println for now, but we need to fix this problem for real!
        // --NH, 3/12/2004
        //        System.out.println("getTranslatedGenomic: warning: frame_index " + frame_index + " > frame_res.length " + frame_res.length()); // DEL
        frame_index = -1;
      }
      String gap = "   ";
      String prefix = (frame_index >= 0 ?
                       frame_res.substring (0, frame_index) + gap :
                       frame_offset == 1 ? "  " : " ");
      String suffix = (frame_index > 0 ?
                       (frame_index < frame_res.length() ?
                        frame_res.substring(frame_index) : "") :
                       frame_res);
      
      frame_res = prefix + suffix;
      if (index +  3 < gappedGenomic.length())
        index = gappedGenomic.indexOf ('-', index + 3);
      else
        index = -1;
    }
    frame_res = DNAUtils.translate(frame_res,
           DNAUtils.FRAME_ONE,
           DNAUtils.ONE_LETTER_CODE);
    // Pad the peptides with the 2 extra bases
    frame_res = insertPeptideSpacers (frame_res, frame_offset);
    return frame_res;
  }

  private int getStrand() { return isForwardStrand ? 1 : -1; }
  boolean isForwardStrand() { return isForwardStrand; }

  private int getFrame() { 
    return genomicSequence.getFrame(getGenomicStart(), isForwardStrand);
  }

  private String  insertPeptideSpacers (String pep, int offset) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < offset; i++)
      buf.append (' ');
    for (int i = 0; i < pep.length(); i++)
      buf.append (pep.charAt (i) + "  ");
    buf.setLength (getGappedLength());
    return buf.toString();
  }

  /** Mapper stuff - returns genomic coord for jalviewCoord */
  int jalviewToGenomic(int jalviewCoord) {
    // contains? FeatureBlock should know what its jalview coords are
    int[] jToG = getJalviewToGenomic();
    int arrayIndex = jalviewCoord-jalviewLow;
    // This supresses exception with and prints err msg - need to really 
    // fix this
    if (arrayIndex >= jToG.length) {
      System.err.println("jalview coord "+arrayIndex+" equals or exceeds "+
                         "jalviewToGenomic "+" mapping array, size: "+jToG.length);
      arrayIndex = jToG.length - 1;
    }
    return jToG[arrayIndex];
  }
  /** Array of genomic coords. Where there are gaps the same genomic coordinate
      is repeated - should it be 0 or -1 in gaps? */
  private int[] jalviewToGenomic = null;
  private int[] getJalviewToGenomic() {
    if (jalviewToGenomic!=null) return jalviewToGenomic; // cached

    String gappedGenomic = getGappedGenomic();
    int[] jalviewToGenomic = new int[gappedGenomic.length()];
    jalviewToGenomic[0] = getGenomicStart();
    int lastUngappedGenomic = jalviewToGenomic[0];
    for (int i=1; i < gappedGenomic.length();  i++) {
      String base = gappedGenomic.substring(i,i+1);
      jalviewToGenomic[i] = -1; // gapped
      // if not a gap increment for pos strand, dec for reverse
      if (!base.equals("-")) { // ungapped
  lastUngappedGenomic += getStrand();
  jalviewToGenomic[i] = lastUngappedGenomic;
      }
    }
    return jalviewToGenomic;
  }
//   /** This is not right - but its actually not used anymore - delete?*/
//   int genomicToJalview(int genomic) {
//     if (!contains(genomic)) return -1;
//     //int[] genToJal = getGenomicToJalview();
//     int index = genomic - getStart();
//     if (getStrand()==-1) index = getStart() - genomic;
//     //return genToJal[index];
//     return -1;
//   }
//   private int[] genomicToJalview = null;
//   private int[] getGenomicToJalview() {
//     if (genomicToJalview!=null) return genomicToJalview; // cached

//     String gappedGenomic = getQueryAlignment(); // goes from 5' to 3' i believe
//     int[] genomicToJalview = new int[length()];
//     genomicToJalview[0] = 1;
//     int genomicIndex = 1;
//     for (int i=1; i < length();  i++) {
//       String base = gappedGenomic.substring(i,i+1);
//       if (!base.equals("-"))genomicToJalview[genomicIndex++] = i;
//     }
//     return genomicToJalview;
//   }

} 
 
