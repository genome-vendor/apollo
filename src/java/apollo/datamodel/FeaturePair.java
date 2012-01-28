package apollo.datamodel;

import java.util.*;

import org.bdgp.util.DNAUtils;

public class FeaturePair extends SeqFeature implements FeaturePairI {
  /** query feature, do we really need to store query separately, couldnt query
      just be this and just the associated hit would be a separate SeqFeatureI?
      Thats really the way to go. how hard a change would this be? */
  SeqFeatureI query; 
  /** hit feature */
  SeqFeatureI hit;
  /** Its debatable whether hit alignments belong in the hit feature itself,
      but you could argue that the query and hit alignments depend on each other
      and belong together in FeaturePair, rather than separated in hit and query
      feats - ive been going back and forth about this 
  im now thinking its awkward to have to access the hit alignment via the FeaturePair
  where the FeaturePair is essentially the query feat. i think a seq feat should have 
  regular and explicit alignment */
  //private String explicitHitAlignment;
  /** Need to have separate explicit and calculated alignment as at save time 
      want to preserve how alignment was loaded. */
  //private String hitAlignment;
  /** Alignment set explicitly (formerly getProperty("alignment"). */
  //private String explicitQueryAlignment;
  /** Alignment for query - either from explicit, cigar, or just the ref seq */
  //private String queryAlignment;

  //StringBuffer q_alignment;
  //private String queryAlignment;
  //StringBuffer h_alignment;
  //private String hitAlignment;
  // M == match
  // D == delete
  // I == insert
  //private static final String cigarDelims = "MDI";

  /** Genuine Sequence of the hit to query , the aligned sequence
      is still stored as a property of the hit feature */
  // SequenceI alignmentSequence;
  /* Don't use the above, if the sequence is needed then get the
     real thing from the FeatureSet holding this FeaturePair */
  //makes sense to store it as null, why waste the space...
  private String cigar = null; // why "" and not null?

  /** f1 is query feature, f2 is hit feature */
  public FeaturePair(SeqFeatureI f1, SeqFeatureI f2) {
    this.query = f1;
    setHitFeature(f2);
  }
  public void        setQueryFeature(SeqFeatureI feature) {
    this.query = feature;
  }
  public SeqFeatureI getQueryFeature() {
    return query;
  }
  public void        setHitFeature(SeqFeatureI feature) {
    this.hit = feature;
    // needs access to cigar for on-demand parsing with getAlignment()
    hit.setQueryFeature(this);
  }
  public SeqFeatureI getHitFeature() {
    return hit;
  }
  /** from SeqFeatureI */
  public boolean hasHitFeature() { return hit != null; }

  public void        setLow(int low) {
    query.setLow(low);
  }
  public int        getLow() {
    return query.getLow();
  }
  public void        setHigh(int high) {
    query.setHigh(high);
  }
  public int        getHigh() {
    return query.getHigh();
  }
  public void        setStart(int start) {
    query.setStart(start);
  }
  public int        getStart() {
    return query.getStart();
  }
  public void        setEnd(int end) {
    query.setEnd(end);
  }
  public int        getEnd() {
    return query.getEnd();
  }
  public void        setStrand(int strand) {
    query.setStrand(strand);
  }
  public int         getStrand() {
    return query.getStrand();
  }

  public void        setName(String name) {
    query.setName(name);
  }

  public String      getName() {
    if (query.getName().equals(Range.NO_NAME)) {
      SequenceI seq = (SequenceI) hit.getRefSequence();
      if (seq != null && seq.getName() != null) {
        query.setName(seq.getName());
      } else {
        query.setName(hit.getName() != null ? hit.getName() : "");
      }
    }
    return query.getName();
  }

  public void        setId(String id) {
    query.setId(id);
  }
  public String      getId() {
    return query.getId();
  }

  public double      getScore() {
    return query.getScore();
  }

  public double getScore (String name) {
    return query.getScore(name);
  }

  public Hashtable getScores () {
    return query.getScores();
  }

  public void        setScore(double score) {
    query.setScore(score);
  }

  public void addScore(Score s) {
    query.addScore(s);
  }
  public void addScore(double score) {
    query.addScore(score);
  }
  public void addScore(String name, double score) {
    query.addScore(name, score);
  }
  public void addScore(String name, String score) {
    query.addScore(name, score);
  }

  public void addProperty(String name, String value) {
    query.addProperty(name, value);
  }

  public void removeProperty(String key) {
    query.removeProperty(key);
  }

  public String getProperty(String name) {
    return query.getProperty(name);
  }

  public Hashtable getProperties() {
    return query.getProperties();
  }

  public void        setFeatureType(String type) {
    query.setFeatureType(type);
  }
  public String      getTopLevelType() {
    return query.getTopLevelType();
  }
  // setBioType??
  public String      getFeatureType() {
    return query.getFeatureType();
  }
  public void        setProgramName(String name) {
    query.setProgramName(name);
  }
  public String      getProgramName() {
    return query.getProgramName();
  }

  public String      getResidues() {
    return query.getResidues();
  }

  public void        setRefSequence(SequenceI seq) {
    query.setRefSequence(seq);
  }
  public SequenceI   getRefSequence() {
    return query.getRefSequence();
  }
  public String      getRefId() {
    return query.getRefId();
  }

  public SeqFeatureI getRefFeature() {
    return query.getRefFeature();
  }
  public void        setRefFeature(SeqFeatureI feature) {
    query.setRefFeature(feature);
  }

  public int         getFrame() {
    return query.getFrame();
  }

  public String      getHname() {
    return hit.getName();
  }
  public void        setHname(String name) {
    hit.setName(name);
  }
  public int        getHstart() {
    return hit.getStart();
  }
  public void        setHstart(int start) {
    hit.setStart(start);
  }
  public int        getHend() {
    return hit.getEnd();
  }
  public void        setHend(int end) {
    hit.setEnd(end);
  }
  public void        setHlow(int low) {
    hit.setLow(low);
  }
  public int        getHlow() {
    return hit.getLow();
  }
  public void        setHhigh(int high) {
    hit.setHigh(high);
  }
  public int        getHhigh() {
    return hit.getHigh();
  }

  public void        setHstrand(int strand) {
    hit.setStrand(strand);
  }
  public int         getHstrand() {
    return hit.getStrand();
  }

  /** Gets the index into the hit strings explicitAlignment for a genomic position**/
  public int getHitIndex(int genomicPosition) {
    int index = 0;
    if (isForwardStrand()) {
      index = genomicPosition - query.getLow();
    } else {
      index =  query.getHigh() - genomicPosition;
    }
    
    return index;
  }
  
  public int getHitIndex(int genomicPosition, String type) {
    int index = 0;
    
    if (isForwardStrand()) {
      index = genomicPosition - query.getLow();
    } else {
      index =  query.getHigh() - genomicPosition;
    }
    
    if (index > -1 && SequenceI.AA.equals(type)) {
      index = index/3;
    }
    
    return index;
  }
  
  public int insertionsBefore(int hitIndex, String alignment) {
    
    int count = 0;
    String query = 
      alignment.substring(0, Math.min(alignment.length(), hitIndex+1));
    int index = query.indexOf('-', 0);
    while (index != -1) {
      count++;
      query = alignment.substring(0, Math.min(alignment.length(), hitIndex+count+1));
      index = query.indexOf('-', index+1);
    }
    
    return count;
  }
  /*
  public int insertionsInGroupFrom(int hitIndex, String alignment) {
    
    int count = 0;
    while (hitIndex < alignment.length() 
        && alignment.charAt(hitIndex) == '-') {
      count++;
      hitIndex++;
    }
    
    return count;
  }
  */
  public Range getInsertionRange(int hitIndex, String alignment) {
    
    int start = -1;
    int end = -1;
    
    for (int hi = hitIndex; hi >= 0 && alignment.charAt(hi) == '-'; hi--) {
      start = hi;
    }
    
    for(int hi = hitIndex; 
        hi < alignment.length() && alignment.charAt(hi) == '-'; hi++) {
      end = hi;
    }
    
    // end is exclusive to make substr easier to use.
    if (start != -1) {
      end++;
    }
    
    return new Range(start, end); 
  }
  
  
  // never used
//   public void invert() {
//     SeqFeatureI tmp = query;
//     query = hit;
//     //hit = tmp;
//     setHitFeature(tmp);
//   }

  /** Returns hit.getFeatureSequence (which is start to end of hits ref seq), 
  */
  public SequenceI getHitSequence() {
    return hit.getRefSequence();
  }
  
  public void setHitSequence(SequenceI hit_seq) {
    hit.setRefSequence(hit_seq);
  }
  
//   /** Returns true if alignment is peptide sequence, false if nucleotide 
//    This is for the hit alignment(not query). Rename hitAlignIsPeptide?
//    From alignable interface.   */
//   public boolean alignmentIsPeptide() {
//     // Nasty hack to tell peptide from dna. Blergh.
//     // BLAST Hstarts and Hends are actually in peptide coordinates, which seems
//     // erroneous to me. Anyways its just as well to use alignment length.
//     String residueType = null;
//     SequenceI seq = hit.getRefSequence();
//     if (seq != null) {
//       residueType = seq.getResidueType();
//     }
//     if (residueType == null ) {
//       // These add up the length of all the parents feats - why is that needed?
//       int hit_length = ((FeatureSetI) getRefFeature()).getSplicedLength();
//       int seq_length = getHAlignedLength();
//       residueType = SeqFeatureUtil.guessResidueTypeFromFeatureLength(seq_length,
//                                                                      hit_length);
//       if (seq != null)
//         seq.setResidueType (residueType);
//     }
//     return residueType == SequenceI.AA;
//   }

//   /** this is so we can specifically get the length of a alignment
//   // or the portion for transcripts that share a 5' first exon 
//   This actually works on the FeaturePairs FeatureSet parent - should it be
//   a FeatureSet method? */
//   private int getHAlignedLength() {
//     int spliced_length = 0;

//     FeatureSetI parent = (FeatureSetI) getRefFeature();
//     int hit_size = parent.size();
//     for (int i = 0; i < hit_size; i++) {
//       //FeaturePairI span = (FeaturePairI) parent.getFeatureAt(i);
//       SeqFeatureI querySpan = parent.getFeatureAt(i);
      
//       SeqFeatureI hitSpan = querySpan.getHitFeature();
//       // why just explicits?? - what about cigars??
//       //if (querySpan.haveExplicitHitAlignment())
//       if (hitSpan.getUnpaddedAlignment() != null) // haveUnspacedAlignment
//         //spliced_length += querySpan.getExplicitHitAlignment().length();
//         spliced_length += hitSpan.getUnpaddedAlignment().length();
//       else
//         spliced_length += hitSpan.length();
//     }
//     return spliced_length;
//   }

  public static void main(String[] args) {
    SeqFeature sf1 = new SeqFeature(100,200,"pog",1);
    SeqFeature sf2 = new SeqFeature(100,200,"pog",-1);

    sf1.setName("query");
    sf2.setName("hit");

    System.out.println("Features " + sf1);
    System.out.println("Features " + sf2);
    FeaturePair fp = new FeaturePair(sf1,sf2);
    System.out.println("Feature is " + fp);
    System.out.println("Left/right overlaps " + fp.getLeftOverlap(sf1) + " " + fp.getRightOverlap(sf1));
    System.out.println("Overlap " + fp.isExactOverlap(sf1) + " " + fp.isExactOverlap(sf2));

    //fp.invert();
    System.out.println("Feature is " + fp);

    System.out.println("Overlap " + fp.isExactOverlap(sf1) + " " + fp.isExactOverlap(sf2));
  }

  public String getDisplayId() {
    //Sequence seq = (Sequence) hit.getRefSequence();
    SequenceI seq = hit.getRefSequence();
    if (seq != null)
      return seq.getName();
    else
      return getFeatureType() + " result" + getName();
  }

  public void setCigar (String cigar) {
    if (cigar != null) 
      this.cigar = cigar;
  }

  public String getCigar () {    
    if (hasCigar()) {
        return cigar;
    }
    //convert explicit alignment into CIGAR format
    else if (haveExplicitAlignment()) {
        String aln1 = getExplicitAlignment();
        String aln2 = getHitFeature().getExplicitAlignment();
        if (aln1.length() != aln2.length() && aln1.length() * 3 != aln2.length()) {
          logger.warn("Alignment lengths for query and hit do not match for hit " +
              getHitSequence().getName() + "(" + getLow() + ", " + getHigh() + ") - cannot generate cigar string");
          return null;
        }
        int queryUnits = 1;
        int hitUnits = 1;
        if ((double)(getHigh() - getLow()) / aln1.length() >= 2) {
          queryUnits = 3;
        }
        if ((double)(getHitFeature().getHigh() - getHitFeature().getLow()) / aln2.length() >= 2) {
          hitUnits = 3;
        }
        //if both queryUnits and hitUnits equal 3, then we have a translating alignment on both the
        //query and subject (e.g. TBLASTX) so need to scale the alignment by 3 to correspond
        //to translated codons
        int units = (queryUnits == 3 && hitUnits == 3) ? 3 : 1;
        
        StringBuilder tmpCigar = new StringBuilder();
        int match = 0;
        int ins = 0;
        int del = 0;
        for (int i = 0; i < aln1.length(); ++i) {
            if (aln1.charAt(i) != '-' && aln2.charAt(i) != '-') {
                match += units;
                if (ins > 0 || del > 0) {
                    if (tmpCigar.length() > 0) {
                        tmpCigar.append(" ");
                    }
                    if (ins > 0) {
                        tmpCigar.append("I" + ins);
                        ins = 0;
                    }
                    if (del > 0) {
                        tmpCigar.append("D" + del);
                        del = 0;
                    }
                }
            }
            else if (aln1.charAt(i) == '-') {
                ins += units;
                if (match > 0 || del > 0) {
                    if (tmpCigar.length() > 0) {
                        tmpCigar.append(" ");
                    }
                    if (match > 0) {
                        tmpCigar.append("M" + match);
                        match = 0;
                    }
                    if (del > 0) {
                        tmpCigar.append("D" + del);
                        del = 0;
                    }
                }
            }
            else {
                del += units;
                if (match > 0 || ins > 0) {
                    if (tmpCigar.length() > 0) {
                        tmpCigar.append(" ");
                    }
                    if (match > 0) {
                        tmpCigar.append("M" + match);
                        match = 0;
                    }
                    if (ins > 0) {
                        tmpCigar.append("I" + ins);
                        ins = 0;
                    }
                }
            }
        }
        if (match > 0) {
            if (tmpCigar.length() > 0) {
                tmpCigar.append(" ");
            }
            tmpCigar.append("M" + match);
        }
        else if (ins > 0) {
            if (tmpCigar.length() > 0) {
                tmpCigar.append(" ");
            }
            tmpCigar.append("I" + match);
        }
        else if (del > 0) {
            if (tmpCigar.length() > 0) {
                tmpCigar.append(" ");
            }
            tmpCigar.append("D" + match);
        }
        return tmpCigar.toString();
    }
    return null;
  }

  protected boolean hasCigar() {
    return !isEmptyOrNull(cigar);
  }

  /** Explicitly set alignment. no padding */
  public String getExplicitAlignment() { 
    if (query.getExplicitAlignment() == null && hasCigar()) {
      parseCigar();
    }
    return query.getExplicitAlignment();
  }

  public void setExplicitAlignment(String align) {
    query.setExplicitAlignment(align);
  }

//   /**  Returns alignment string for hit, with dashes
//       for gaps. If the alignment is peptide, alignment will have peptide spacing
//       - 2 spaces after every amino 
//       There should be a setHitAlignment for explicit alignments
//       Some game data is faulty and provides only one of the hit or ref alignments,
//       in this case the sole alignment is not used, feat residues is used for both.
//   */
//   public String getAlignment() {
//     if (alignment == null) {
//       // have to have both alignments
//       if (haveRealHitAlignment() && haveRealQueryAlignment()) {
//         // Check if hit alignment given explicitly
//         if (haveExplicitHitAlignment()) {
//           hitAlignment = getExplicitHitAlignmentWithSpacing();
//         }
//         else {
//           // sets hitAlignment (with spacing - buggy though)
//           parseCigar();  
//         }
//       }
//       // if there is no explicit alignment for both nor cigar
//       else {
//         hitAlignment = getQueryAlignment();
//       }
//     }
//     return alignment;
//   }


//   public String getHitAlignment () {
//     if (hitAlignment == null) {
//       // have to have both alignments
//       if (haveRealHitAlignment() && haveRealQueryAlignment()) {
//         // Check if hit alignment given explicitly
//         if (haveExplicitHitAlignment()) {
//           hitAlignment = getExplicitHitAlignmentWithSpacing();
//         }
//         else {
//           // sets hitAlignment (with spacing - buggy though)
//           parseCigar();  
//         }
//       }
//       // if there is no explicit alignment for both nor cigar
//       else {
//         hitAlignment = getQueryAlignment();
//       }
//     }
//     return hitAlignment;
//   }
  
  /** Returns true if have an explicit hit alignment or have cigar,
      return false if not and just using ref alignment */
//  private boolean haveRealHitAlignment() {
//    return hasCigar() || haveExplicitHitAlignment();
//  }
  /** Return true if have explicit alignment for hit */
//  public boolean haveExplicitHitAlignment() {
//    return !isEmptyOrNull(explicitHitAlignment);
//  }
//   public void setExplicitHitAlignment(String hitAlign) {
//     explicitHitAlignment = hitAlign;
//   }
//  public String getExplicitHitAlignment() { 
//    return explicitHitAlignment; 
//  }
//   /** Return explicit hit alignment - with peptide spcing if peptide */
//   private String getExplicitHitAlignmentWithSpacing() {
//     String eha =  explicitHitAlignment;//Property("alignment");
//     if (alignmentIsPeptide()) {
//       eha = addPeptideSpacing(eha);
//     }
//     return eha;
//   }

//   /** This needs to return gapped genomic, even if an explicit query alignment 
//       is given as protein. This first tries to get alignment from property, then from 
//       cigar, should that be reversed? */
//   public String getQueryAlignment () {
//     if (queryAlignment == null) {
//       // have to have both alignments
//       if (haveRealQueryAlignment() && haveRealHitAlignment()) {
//         // Check if query alignment given explicitly
//         if (haveExplicitQueryAlignment()) {
//           queryAlignment = getExplicitQueryAlignmentWithSpacing();
//         }
//         else {
//           parseCigar();  // sets queryAlignment
//         }
//       }
//       // if there is no explicit alignment for both nor cigar
//       else {
//         queryAlignment = super.getQueryAlignment();
//       }
//     }
//     return queryAlignment;
//   }

  /** Returns true if have an explicit query alignment or have cigar,
      return false if not and just using ref alignment */
//  private boolean haveRealQueryAlignment() {
//    return hasCigar() || haveExplicitQueryAlignment();
//  }
  /** Return true if have explicit alignment for query */
//  private boolean haveExplicitQueryAlignment() {
//    return !isEmptyOrNull(explicitQueryAlignment);
//  }
//  public void setExplicitQueryAlignment(String expQueryAlign) {
//    explicitQueryAlignment = expQueryAlign;
//  }
//  public String getExplicitQueryAlignment() {
//    return explicitQueryAlignment;
//  }
  /** Return explicit query alignment - with peptide spcing if peptide */
//  private String getExplicitQueryAlignmentWithSpacing() {
//    if (!haveExplicitQueryAlignment()) return null;
//    String era =  explicitQueryAlignment;//Property("alignment");
//    if (queryAlignmentIsPeptide(era)) {
//      era = addPeptideSpacing(era);
//    }
//    return era;
//  }

//   private boolean queryAlignmentIsPeptide(String queryAlignment) {
//     int al = queryAlignment.length();
//     int ql = getQueryFeature().length();
//     String residueType = SeqFeatureUtil.guessResidueTypeFromFeatureLength(al,ql);
//     return residueType == SequenceI.AA;
//   }

  private boolean isEmptyOrNull(String s) {
    if (s==null) return true;
    return s.equals("");
  }

  /** Populates getHitFeature().getAlignment() and queryAlignment using cigar string and hit and 
      query seqs */
  public void parseCigar() {
    if (!hasCigar()) return;
    if (isEmptyOrNull(getResidues())) return;
    if (isEmptyOrNull(getHitFeature().getResidues())) return;
    if (isEmptyOrNull(getCigar())) return;
    if (!validateCigar()) return;
    if (!checkRange()) return;

    // this will take care of any reverse complementing that
    // is required
    String q_seq = getResidues();
    String h_seq = getHitFeature().getResidues();

    StringBuffer q_alignment = new StringBuffer();
    StringBuffer h_alignment = new StringBuffer();
    StringBuffer tmp = new StringBuffer();
    // units indicates whether to count by one (for DNA bases) or
    // to count by 3 (for AA)
    int q_units = 1;
    int h_units = 1;

    //determine the units (looks at length differences)
    if ((double)q_seq.length() / h_seq.length() >= 2) {
        q_units = 3;
    }
    else if ((double)h_seq.length() / q_seq.length() >= 2) {
        h_units = 3;
    }

    int qidx = 0;
    int hidx = 0;
    for (String token : getCigar().split("\\s+")) {
        int len = Integer.parseInt(token.substring(1));
        tmp.delete(0, tmp.length());
        if (token.charAt(0) == 'M') { //match
          /*
          qidx = appendAlignment(q_alignment, q_seq, len, qidx, q_units,
              h_units, false);
           */
          qidx = appendAlignment(tmp, q_seq, len, qidx, q_units,
              h_units, false);
          if (q_units == 3 && h_units == 1) {
            q_alignment.append(DNAUtils.translate(tmp.toString(), DNAUtils.FRAME_ONE, DNAUtils.ONE_LETTER_CODE));
          }
          else {
            q_alignment.append(tmp.toString());
          }
          hidx = appendAlignment(h_alignment, h_seq, len, hidx, h_units,
              q_units, false);
        }
        else if (token.charAt(0) == 'I') {
          /*
          qidx = appendAlignment(q_alignment, q_seq, len, qidx, q_units,
              h_units, true);
           */
          qidx = appendAlignment(tmp, q_seq, len, qidx, q_units,
              h_units, true);
          if (q_units == 3 && h_units == 1) {
            q_alignment.append(tmp.substring(0, tmp.length() / 3));
          }
          else {
            q_alignment.append(tmp.toString());
          }
          hidx = appendAlignment(h_alignment, h_seq, len, hidx, h_units,
              q_units, false);
        }
        else if (token.charAt(0) == 'D') {
          /*
          qidx = appendAlignment(q_alignment, q_seq, len, qidx, q_units,
              h_units, false);
           */
          qidx = appendAlignment(tmp, q_seq, len, qidx, q_units,
              h_units, false);
          if (q_units == 3 && h_units == 1) {
            q_alignment.append(DNAUtils.translate(tmp.toString(), DNAUtils.FRAME_ONE, DNAUtils.ONE_LETTER_CODE));
          }
          else {
            q_alignment.append(tmp.toString());
          }
          hidx = appendAlignment(h_alignment, h_seq, len, hidx, h_units,
              q_units, true);
        }
    }

    //queryAlignment = q_alignment.toString();
    /*
    if (q_units != h_units) {
      String aln = q_alignment.toString();
      q_alignment.delete(0, aln.length());
      q_alignment.append(DNAUtils.translate(aln, DNAUtils.FRAME_ONE, DNAUtils.ONE_LETTER_CODE, "", "", ""));
    }
    */
    setExplicitAlignment(q_alignment.toString());
    //hitAlignment = h_alignment.toString();
    getHitFeature().setExplicitAlignment(h_alignment.toString());
  }

  /** If units==3 will put in peptide spacing(2 spaces) */
  private int appendAlignment (StringBuffer alignment,
                                  String seq,
                                  int len,
                                  int idx,
                                  int seqUnits,
                                  int otherUnits,
                                  boolean isGap) {
      for (int i = 0; i < len * seqUnits; ++i) {
          alignment.append(isGap ? '-' : seq.charAt(idx++));
          /*
          if (otherUnits == 3) {
              alignment.append("  ");
          }
          */
      }
      return idx;
  }

  /** Appends length dashes to alignment */
  private void padAlignment (StringBuffer alignment,
                             int length) {
    for (int i = 0; i < length; i++) {
      alignment.append('-');
    }
  }

  /** Returns true if has a real alignment or if theres a "trivial" alignment
      from SeqFeature.getTrivialAlignable */
  public boolean hasAlignable() { 
    // Also check for !.equals""?
    if (hasRealAlignment())
      return true;
    else
      return super.hasAlignable();
  }

  /** Whether FeaturePair has a real ref an hit alignment as opposed to the
      trivial alignment of seq feature - where both hit and ref are just the 
      features sequence */
  private boolean hasRealAlignment() {
    return getAlignment()!=null && getHitFeature().getAlignment() != null;
  }

  /** Check for valid cigar string
   * 
   * @return false if cigar length does not match alignment length
   */
  private boolean validateCigar()
  {
    String []cigarTokens = getCigar().split("\\s+");
    int queryCigarLen = 0;
    int hitCigarLen = 0;
    for (String token : cigarTokens) {
      int len = Integer.parseInt(token.substring(1));
      switch (token.charAt(0)) {
      case 'M':
        queryCigarLen += len;
        hitCigarLen += len;
        break;
      case 'D':
        queryCigarLen += len;
        break;
      case 'I':
        hitCigarLen += len;
        break;
      }
    }
    int queryAlnLen = getHigh() - getLow() + 1;
    int hitAlnLen = getHhigh() - getHlow() + 1;
    if ((queryAlnLen == queryCigarLen || queryAlnLen == queryCigarLen * 3) &&
        (hitAlnLen == hitCigarLen || hitAlnLen == hitCigarLen * 3)) {
      return true;
    }
    logger.warn("Query and hit span lengths do not match alignment length for " +
        getHitSequence().getName() + "(" + getLow() + ", " + getHigh() + ") - cannot parse cigar string");
    return false;
  }
  
  private boolean checkRange()
  {
    if (!getQueryFeature().isContainedByRefSeq()) {
      logger.warn("Query feature is not contained within genomic feature for " +
          getLow() + ", " + getHigh());
      return false;
    }
    return true;
  }
}
