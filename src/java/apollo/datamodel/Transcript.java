package apollo.datamodel;

import java.util.Vector;
import apollo.util.QuickSort;
import org.apache.log4j.*;
import org.bdgp.util.DNAUtils;

public class Transcript extends AnnotatedFeature {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Transcript.class);

  public final static String TRANSCRIPT_TYPE = "transcript";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected SequenceI cDNA = null;
  protected SequenceI peptide = null;
  //protected SequenceI prior_peptide = null;
  private   boolean peptideValidity = true;
  private boolean allow_nonconsensus_splicing = false;
  private Protein proteinFeat; // ??
  //private AnnotatedFeatureI proteinFeat; // ??

  public Transcript() {
    super();
    setup();
  }

  // The features in the set should be
  // checked so that they are in coordinate
  // order and don't overlap and
  // have distinct names - maybe we should
  // rename

  public Transcript(FeatureSetI fs) {
    // the children that are created will be of class Exon
    super(fs, "apollo.datamodel.Exon");
    setup();
  }

  private void setup () {
    setFeatureType(TRANSCRIPT_TYPE); 
    annotationRoot = false;
  }

  /** If exon overlaps with existing exons it merges them, otherwise it
      adds the exon to the transcript */
  private void addExonWithOverlapCheck(ExonI exon, boolean sort) {
    // We need to check for overlaps with > 1 exon
    Vector overlaps = new Vector();
    int setSize = features.size();
    for (int i=0; i < setSize; i++) {
      if (getFeatureAt(i).overlaps(exon)) {
        overlaps.addElement(getExonAt(i));
      }
    }
    for (int i=0; i < overlaps.size(); i++) {
      exon.merge((ExonI)overlaps.elementAt(i));
      // delete merged in exons
      deleteExon((ExonI)overlaps.elementAt(i));
    }
    addFeature(exon,sort);
  }

  /** If exon overlaps with other exons then it merges them, otherwise
      adds exon to transcript */
  public void addExon(ExonI exon) {
    // im a little surprised this wasnt here before - perhaps happens with caller
    // but certailny doesnt hurt to have it here
    setPeptideValidity(false);
    // Check the same strand if we already have exons
    if (features.size() != 0 && !trans_spliced) {
      if (getExonAt(0).getStrand() != exon.getStrand()) {
        // Don't throw an error, because there are biological cases: mod(mdg4) where
        // trans-splicing occurs
        String msg = ("ERROR: strand inconsistent in addExon() to " +
                      getName() + ".\n" + "Exon " + exon.getStart() + "-" +
                      exon.getEnd() + " has strand " + exon.getStrand() + 
                      ", whereas the first exon in that gene has strand " + getExonAt(0).getStrand() +
                      " Setting trans_spliced to true");
        logger.warn (msg);
        trans_spliced = true;
      }
    }
    // Check we don't overlap existing exons - merge if it does -otherwise add
    // We have to clone the exon as it could be in another
    // transcript (?)
    addExonWithOverlapCheck(exon,!trans_spliced);
  }

  public void sortExons() {
    sort(getStrand());
  }

  public void addFeature (SeqFeatureI feature) {
    addExon ((ExonI) feature);
  }

  public void deleteExon(ExonI exon) {
    deleteFeature(exon);
  }

  public Vector getExons() {
    return features;
  }

  public ExonI getExonAt(int i) {
    return (ExonI) getFeatureAt (i);
  }

  /** Calls FeatureSet.get_cDNA which appends exon sequence
      (with getSplicedTranscript) where the actual sequence is retrieved 
      from the ref sequence. In the case of overhanging transcripts this
      wont work because there is only ref sequence for part of the transcript.
      So in this case it does not retrieve ref sequence but just returns the
      cdna sequence originally assigend. This is ok because overhanging 
      transcripts are uneditable in apollo, so theres no need to retrieve
      from the ref seq. */
  public String get_cDNA () {
    if (!haveWholeSequence() && cDNA !=null) {
      // cDNA sometimes does not have residues
      if (cDNA.getResidues() == null)
        return super.get_cDNA(); 
      return cDNA.getResidues();
    }

    // This gets sequence from the entry sequence
    String na = super.get_cDNA(); 

    if (na != null && !na.equals ("")) {
      if (cDNA == null) {
        cDNA = new Sequence (getName(), na);
        cDNA.setResidueType (SequenceI.RNA);  // ?
        // nulling pep can no longer be the way. this wipes out any peptide names that
        // have been set (disName & acc). need to just gut sequence & preserve names.
        // the point is to trigger retranslating peptide seq from new cdna.
        //peptide = null;
        // clear out peptides residues, make way for new translation
        if (peptide != null) {
          peptide.clearResidues();
          // this is crucial! otherwise peptide wont get retranslated & stay ""
          setPeptideValidity(false);
        }
      } else {
        //   ctw 17oct02 - Don't set transcript cDNA residues to na 
        //                 just because they differ from na.
        // This updating needs to happen for normal non-overhanging 
        // transcripts. mg 12.12.02
        if (cDNA.getResidues() == null || !cDNA.getResidues().equals (na)) {
          cDNA.setResidues (na);
          setPeptideValidity(false);
        }
      }
    }
    return (cDNA != null ? cDNA.getResidues() : na);
  }

  /** Returns true if transcript is hanging off the bounds of the entry
      Does this belong in FeatureSet? Or even SeqFeature?
      Delete this - replaced by isContainedByRefSeq
  */
  public boolean haveWholeSequence() {
    return isContainedByRefSeq();
  }

  public SequenceI get_cDNASequence () {
    if (cDNA == null)
      get_cDNA();
    return cDNA;
  }

  public void set_cDNASequence (SequenceI cDNA) {
    this.cDNA = cDNA;
  }

  /** Creates SequenceI peptide if doesnt exist already - if translation is null
   * or "" peptide will not be created - should it anyways? */
  public String translate() {
    if (!isProteinCodingGene() || !hasRefSequence()) {// cant translate with no ref seq
      setPeptideValidity(false);
      return ""; // shouldnt translate non coding, just in case not checked upstream
    }

    String aa = super.translate ();
    // create peptide even if aa is "" - we may want to add ids & names before we have
    // exons
    if (peptide == null) {
      peptide = new Sequence(null,aa);
      peptide.setResidueType(SequenceI.AA);
    }
    else if (aa != null && !aa.equals ("")) {
      
//       // NO PEPTIDE - create one
//       if (peptide == null) {
//         // Should peptide be given null name or transcript name by default?
//         // the id should be null - that helps indicate its new
//         peptide = new Sequence (null, aa);
//         peptide.setResidueType (SequenceI.AA);
//         // Set the peptide's accession # here, 
//         // so it will get saved properly if we save the data
//         // name adapter does this, data adapter needs to call name adapter
//         // if they dont explicitly name the peptide -- this is now pase
//         //peptide.setAccessionNo(derivePeptideName(getName()));
//       }

      // PEPTIDE EXISTS - replace its sequence
      
//      else {
        /* ! This may need to be changed.  It's translating the cDNA and then making that
           the new peptide sequence, if the current peptide sequence doesn't match.  We
           may decide that we need to preserve the current peptide, unless the cDNA
           itself has changed. the basic question is: is it possible for the pep seq to
           differ from the translated genomic? this is currently not possible if so. */
        //prior_peptide = peptide; // prior_peptide not actually used anywhwere
        // what causes * to come from FS.translate()?
        // replace old seq w new. old seq could be cleared out ("").
        if (aa.indexOf ("*") < 0 &&
            (!peptide.hasResidues() || !aa.equals (peptide.getResidues())) ) {
          peptide.setResidues (aa);
        }
//      }
    }
    //return (peptide != null ? peptide.getResidues() : aa);
    setPeptideValidity(true);
    return peptide.getResidues();
  }

  /** creates peptide sequence if dont have one && prot coding gene - also if pep
   * is invalid retranslates */
  public SequenceI getPeptideSequence() {

    // 8/17/2005: I added the isProteinCodingGene() test because I don't see why
    // we should try to translate non-coding annots--it just leads to problems.
    if (peptide == null && isProteinCodingGene()) {
      peptide = new Sequence(null,"");
      peptide.setResidueType(SequenceI.AA);
      translate(); // sets peptide if can translate
    }
    else if (!getPeptideValidity()) {
      translate();
    }
    // setPeptideValidity(true); -> translate()
    return peptide;
  }

  public void setPeptideSequence (SequenceI pep) {
    this.peptide = pep;
  }

  /** I think the idea here is if validity is false its saying the peptide is no
   * longer valid and a new translation needs to be calculated - this is now
   part of TranslationI - move this method to FeatureSet? (cdna? peptide?)  */
  public void setPeptideValidity(boolean validity) {
    peptideValidity = validity;
    if (peptideValidity == false) {
      // Checksums will need to be recalculated
      if (cDNA != null)
        cDNA.setChecksum("");
      if (peptide != null)
        peptide.setChecksum("");
    }
  }
  
  public boolean getPeptideValidity() {
    return peptideValidity;
  }

  public String toString() {
    return getName();
  }

  public void sortTransSpliced() {
    int setSize = size();

    if (setSize == 0) {
      return;
    }
    int[] coord = new int[setSize];
    SeqFeatureI[] obj  = new SeqFeatureI[setSize];

    for (int i=0; i < setSize; i++) {
      SeqFeatureI sf = getFeatureAt(i);
      coord[i] = sf.getStart();
      obj  [i] = sf;
    }

    QuickSort.sort(coord, obj);

    // need the span to know the correct strand where 
    // transcription begins
    SeqFeatureI start_span = getFeatureContaining(getTranslationStart());
    int strand = start_span.getStrand();
    int count = 0;
    for (int i = 0; i < setSize; i++) {
      int index = strand == 1 ? i : (setSize - i - 1);
      SeqFeatureI exon = obj[index];
      if (exon.getStrand() == strand) {
        features.setElementAt(exon, i);
	count++;
      }
    }
    strand *= -1;
    for (int i=0; i < setSize;i++) {
      int index = strand == 1 ? i : (setSize - i - 1);
      SeqFeatureI exon = obj[index];
      if (exon.getStrand() == strand) {
        features.setElementAt(exon, i+count);
      }
    }
  }


  /** If we have a translation start(nonzero) we have a peptide seq 
      do i have this right? translate may still return null if we have start
      but no transcript sequence(if theres no sequence for the entry). 
      This calls getPeptideSequence() which will actually create a peptide
      sequence if it can & there isnt one yet 
      this aint right - can still have peptide with no trans start - just missing
      5 prime thats all */
  public boolean hasPeptideSequence() {
    //return hasTranslationStart() && getPeptideSequence() != null;
    return getPeptideSequence() != null;
  }

//   /** Returns true if Transcript can generate a peptide sequence, has to have both
//       a translation start & dna seq to translate. hasPeptideSeq() tests the same
//       thing so it just calls that, even though the methods have slightly different
//       shades of meaning. */
//   public boolean canGeneratePeptideSequence() {
//     return hasPeptideSequence();
//   }

//   /** creates and returns a peptide Sequence. getPeptideSeq actually does the same
//       so it just calls that, this method is more for clarity. name is set to name
//       accessionNo set to accession. */
//   public SequenceI generatePeptideSequence(String accession, String name) {
//     if (!canGeneratePeptideSequence())
//       return null;
//     getPeptideSequence().setName(name);
//     getPeptideSequence().setAccessionNo(accession);
//     return getPeptideSequence();
//   }

  /** Transcript has the same biotype as its gene */
  public String getTopLevelType() { 
    /* gene can be null if transcript has been deleted but is still 
       being erroneously held onto (like in SetDetailPanel) */
    if (getRefFeature() == null) {
      if (biotype == null || biotype.equals (""))
	return getFeatureType();
      else
	return biotype;
    }
    else
      return getRefFeature().getTopLevelType(); 
  }

  public AnnotatedFeatureI getGene() {
    return (AnnotatedFeatureI) getRefFeature();
  }

  public boolean isProteinCodingGene() {
    if (getGene() != null) 
      return getGene().isProteinCodingGene();
    else
      return true;
  }

  /** This returns the exon index of the non-consensus acceptor (if
      any), or -1 if there aren't any non-consensus acceptors.  I
      think it should be called getNonConsensusAcceptorIndex instead--
      getNonConsensusAcceptorNum makes it sounds like it's returning
      the total number (0 or more) of non-consensus acceptors. */
  public int getNonConsensusAcceptorNum() {
    int odd = -1;
    int exon_cnt = size();
    if (isProteinCodingGene() && exon_cnt > 1) {
      for (int i = 0; i < exon_cnt && odd == -1; i++) {
        if (((ExonI) getFeatureAt(i)).isNonConsensusAcceptor())
          odd = i;
      }
    }
    return odd;
  }

  public int getNonConsensusDonorNum() {
    int odd = -1;
    int exon_cnt = size();
    if (isProteinCodingGene() && exon_cnt > 1) {
      for (int i = 0; i < exon_cnt && odd == -1; i++) {
        if (((ExonI) getFeatureAt(i)).isNonConsensusDonor())
          odd = i;
      }
    }
    return odd;
  }

  public boolean nonConsensusSplicingOkay() {
    return allow_nonconsensus_splicing;
  }

  public void nonConsensusSplicingOkay(boolean okay) {
    this.allow_nonconsensus_splicing = okay;
  }

  /** Returns true becuase we have a transcript - its ourself. Rename isTranscript? 
      does transcript always have translation start and stop? */
  //public boolean hasTranslationStartAndStop() { return true; }
  public boolean isTranscript() { return true; }
//   /** Returns self */
//   public Transcript getTranscript() { return this; }

  /** first cut - eventually grow into full fledged protein feat
      that has a pep seq - which will replace Transcripts peptide Seq
      probably will require a Protein class. returns null if !isProteinCodingGene()*/
  public Protein getProteinFeat() {
    if (!isProteinCodingGene())
      return null;
    if (proteinFeat != null)
      return proteinFeat;
    proteinFeat = new Protein(this);
    return proteinFeat;
  }

  public boolean hasProteinFeat() { return getProteinFeat() != null; }

  void setProteinFeat(Protein prot) {
    this.proteinFeat = prot;
  }
  
}
