package apollo.dataadapter.chado.jdbc;

import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SequenceI;

import org.apache.log4j.*;

/**
 * Class that stores essential information about a Chado 'CDS' feature.
 */
class ChadoCds {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoCds.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String name;
  private String cdnaSeq;
  private String proteinName;
  private String proteinSeq;

  // coordinates in chado space-oriented 0-indexed form:
  private int fmin;
  private int fmax;
  private boolean fmin_partial;
  private boolean fmax_partial;
  private int strand;

  /** base-oriented fmin */
  private int apollo_fmin;

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  ChadoCds(String cdsName, String cdnaSeq, String proteinName, String proteinSeq, int fmin, int fmax, 
           boolean fmin_partial, boolean fmax_partial, int strand) 
  {
    this.name = cdsName;
    this.cdnaSeq = cdnaSeq;
    this.proteinName = proteinName;
    this.proteinSeq = proteinSeq;
    this.fmin = fmin;
    this.fmax = fmax;
    this.fmin_partial = fmin_partial;
    this.fmax_partial = fmax_partial;
    this.strand = strand;
    this.apollo_fmin = JdbcChadoAdapter.adjustLowForInterbaseToBaseOrientedConversion(fmin);
  }

  // -----------------------------------------------------------------------
  // ChadoCds
  // -----------------------------------------------------------------------

  /** Create and return protein sequence object from protein seq and name */
  Sequence getProteinSequence() {
    Sequence proteinSequence = new Sequence(proteinName, proteinSeq);
    proteinSequence.setResidueType(SequenceI.AA);
    proteinSequence.setAccessionNo(proteinName);
    return proteinSequence;
  }

  /** Create and return cDNA sequence object from cDNA seq and name */
  Sequence getcDNASequence() {
    Sequence cDNASequence = new Sequence(name, cdnaSeq);
    cDNASequence.setResidueType(SequenceI.RNA);
    cDNASequence.setAccessionNo(name);
    return cDNASequence;
  }

  // chado coordinates

  int getFmin() {
    return fmin;
  }

  int getFmax() {
    return fmax;
  }

  boolean getFminPartial() {
    return fmin_partial;
  }

  boolean getFmaxPartial() {
    return fmax_partial;
  }

  int getStrand() {
    return strand;
  }

  String getName() {
    return name;
  }

  // apollo coordinates

  private boolean isForward() {
    return strand >= 0;
  }

  int getStart() {
    return isForward() ? apollo_fmin : fmax;
  }
  
  int getEnd() {
    return isForward() ? fmax : apollo_fmin;
  }

  boolean getStartPartial() {
    return isForward() ? fmin_partial : fmax_partial;
  }

  boolean getEndPartial() {
    return isForward() ? fmax_partial : fmin_partial;
  }
  
  /** 
   * Return the basepair where the translation stop codon begins, in Apollo genomic
   * coordinates.  The +/- 2 adjustment is required because in Apollo
   * the end of the CDS feature is the 1st base of the stop codon, whereas in
   * Chado the end of the CDS feature is the 3rd/last base of the stop codon.
   * Note that either fmin_partial or fmax_partial is used to determine whether
   * the CDS is really contained in the chado CDS feature, or runs off the end
   * (i.e., no in-frame stop contained within the transcript.)
   *
   * Note that FlyBase autocomputes the stop position, avoiding this issue.
  */
  // TODO - the simple-minded +/- 2 adjustment will fail if the stop codon spans a splice junction
  int getTranslationEnd() {
    if (isForward()) 
      return getEndPartial() ? getEnd() : getEnd() - 2;
    else
      return getEndPartial() ? getEnd() : getEnd() + 2;
  }

  /** check that cds bounds are within transcript bounds. set to transcript if not
      this shouldnt happen but i guess it does */
  void checkCdsBounds(SeqFeatureI transcript) {

    if (apollo_fmin < transcript.getLow()) {
      logger.warn("setting ChadoCds fmin to transcript fmin=" + transcript.getLow() + " for cds " + name);
    }

    if (fmax > transcript.getHigh()) {
      logger.warn("setting ChadoCds fmax to transcript fmax=" + transcript.getHigh() + " for cds " + name);
    }

    if (apollo_fmin < transcript.getLow()) {
      apollo_fmin = transcript.getLow();
      fmin = JdbcChadoAdapter.adjustLowForBaseOrientedToInterbaseConversion(fmin);
    }

    if (fmax > transcript.getHigh()) {
      fmax = transcript.getHigh();
    } 
  }
}
