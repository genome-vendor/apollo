package apollo.datamodel;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.log4j.*;

/** One thought Im having is that annotation stuff would be a HASA instead
    of a ISA for SeqFeatureI. So this object would not extend FeatureSet/SeqFeature,
    it would just contain these methods. A SeqFeatureI would optionally have one 
    of these objects. just a thought.*/

public class AnnotatedFeature extends FeatureSet implements AnnotatedFeatureI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AnnotatedFeature.class);

  private EvidenceSet evidenceSet = new EvidenceSet();
  private EvidenceFinder finder;
  private Vector comment_list;
  // rename - also String id in SeqFeature
  private boolean isProblematic = false;
  protected boolean annotationRoot = true;
  protected String owner = null;

  public AnnotatedFeature () {
    super();
    init();
  }

  public AnnotatedFeature(SeqFeatureI sf) {
    super(sf);
    init ();
  }

  public AnnotatedFeature(FeatureSetI fs, String class_name) {
    super(fs, class_name);
    init ();
  }

  private void init () {
    // should only set type if we dont have one, MG 9.12.05
    if (getFeatureType() == null || getFeatureType().equals(RangeI.NO_TYPE)) 
      setFeatureType("gene");
    evidenceSet = new EvidenceSet();
  }

  public AnnotatedFeatureI cloneAnnot() {
    return (AnnotatedFeatureI)cloneFeature();
  }

  /**
   * General implementation of Visitor pattern. (see apollo.util.Visitor).
  **/
  public void accept(apollo.util.Visitor visitor){
    visitor.visit(this);
  }//end accept

  // Needed for the JTree. Please do not remove.
  public String toString() {
    return new String (getName());
  }

  public Vector     getEvidence() {
    return evidenceSet.getEvidence();
  }

  public void       addEvidence(String evidenceId,String setId,int type) {
    evidenceSet.addEvidence(new Evidence(evidenceId,setId,type));
  }

  public void       addEvidence(String evidenceId) {
    evidenceSet.addEvidence(new Evidence(evidenceId));
  }

  public void       addEvidence(String evidenceId, int type) {
    evidenceSet.addEvidence(new Evidence(evidenceId,type));
  }

  public void       addEvidence(Evidence evidence) {
    evidenceSet.addEvidence(evidence);
  }

  public int       deleteEvidence(String evidenceId) {
    return evidenceSet.deleteEvidence(evidenceId);
  }

  public void setEvidenceFinder(EvidenceFinder ef) {
    this.finder = ef;
    int feat_count = size();
    for (int i=0; i < feat_count; i++) {
      SeqFeatureI sf = getFeatureAt(i);
      if (sf instanceof AnnotatedFeatureI) {
        ((AnnotatedFeatureI)sf).setEvidenceFinder(ef);
      }
    }
  }

  public EvidenceFinder getEvidenceFinder() {
    return finder;
  }

  /* Couldn't allow allow empty strings as comments
     because of the hash table, so changed it to a vector */
  public Vector     getComments() {
    if (comment_list == null)
      comment_list = new Vector(1);
    return comment_list;
  }

  /** Returns index of comment, -1 if not found */
  public int getCommentIndex(Comment comm) {
    return getComments().indexOf(comm);
  }

  public void       addComment(Comment comm) {
    addComment(getComments().size(),comm);
  }

  public void addComment(int index,Comment comm) {
    if (comm != null && comm.getText() != null) {
      getComments().add(index,comm);
    } else {
      logger.error("AnnFeat.addComment comment or comment text is null");
    }
  }

  public void       deleteComment(Comment comm) {
    if (comm != null && comm.getText() != null) {
      if (comment_list == null)
        comment_list = new Vector(1);
      comment_list.remove (comm);
    }
  }

  public void deleteComment(int i) {
    if (i >= comment_list.size()) {
      logger.info("Deleting comment out of range "+i);
      return;
    }
    comment_list.remove(i);
  }
      

  public void clearComments () {
    // Clearing out the vector isn't good enough.  If we've cloned this object,
    // we need to set its comment_list to null so the clone will be forced to
    // make a new comment_list rather than dangerously sharing its parent's.
    comment_list = null;
  }

  public Object clone() {
    AnnotatedFeatureI clone = ((AnnotatedFeatureI)super.clone());
    if (clone != null) {
      clone.clearComments();
      for (int i=0; i < getComments().size(); i++) {
        Comment comm = (Comment) ((Comment)getComments().elementAt(i)).clone();
        clone.addComment(comm);
      }
      clone.setIdentifier((Identifier) getIdentifier().clone());
    }
    return clone;
  }


  public boolean isProblematic() {
    return isProblematic;
  }
  public boolean isFinished() {
    String status = getProperty("status");
    if(status != null)
    	return status.equals("all done");
    return false;
  }

  public void setIsProblematic(boolean isProblematic) {
    this.isProblematic = isProblematic;
  }

  /** addSynonym used to refuse (quite reasonably) to add synonyms that are the
      same as the name of the feature, but Chado XML is rather fond of those
      reflexive synonyms, so go ahead and add them, what the heck. */
  public void addSynonym(String syn) {
    //    if (!syn.equals (getName()))
    getIdentifier().addSynonym(syn);
  }

  public void addSynonym(int index, String syn) {
    //    if (!syn.equals (getName()))
    getIdentifier().addSynonym(index, syn);
  }

  public void addSynonym(Synonym syn) {
    getIdentifier().addSynonym(syn);
  }

  public Vector getSynonyms() {
    return getIdentifier().getSynonyms();
  }

  public Vector getSynonyms(boolean excludeInternalSynonyms) {
    return getIdentifier().getSynonyms(excludeInternalSynonyms);
  }

  public void deleteSynonym (String syn) {
    getIdentifier().deleteSynonym (syn);
  }

  public void deleteSynonym (Synonym syn) {
    getIdentifier().deleteSynonym(syn);
  }

  public void clearSynonyms () {
    getIdentifier().clearSynonyms ();
  }

  public int getSynonymSize() { return getSynonyms().size(); }

  public Synonym getSynonym(int i) { 
    return (Synonym)getSynonyms().get(i);
  }

  public boolean hasSynonym(String name) {
    return getIdentifier().hasSynonym(name);
  }

  /** This is the isProteinCodingGene method that really matters--the ones in other classes
      aren't really used. */
  public boolean isProteinCodingGene() {
    return (getFeatureType().equalsIgnoreCase("gene") ||
            getFeatureType().equalsIgnoreCase("pseudogene"));
  }

  public void       setDescription(String desc) {
    getIdentifier().setDescription(desc);
  }

  public String     getDescription() {
    return getIdentifier().getDescription();
  }

  /* returns true if this is the root of an entire annotation
     feature hierarchy. There were 2 ways to do this and I'm
     not sure which is better. One is simply with a flag that is
     set when the class is created. Two is to look at the parent
     and see if it is an annotation (if not, then the object is the
     root. The second seemed too risky since I don't know where
     for certain, all the new instances of AnnotatedFeature (aka genes)
     are created, but it might be the safer way. The first way
     relies upon the flag being set appropriately by any new instances
     (hmmm, maybe should switch to second method??)

     Returns true for AnnotatedFeature(genes and the like),
     false for Exon and Transcript.
     In the future there may be AnnotatedFeature that are parts of other
     AnnotatedFeatures (and not Transcripts or Exons) so need to watch this.
     Rename this isAnnotationTop? isAnnotationRoot? isAnnotation seems like it
     could return true for exon and transcript - too vague. Theres already an
     isAnnot method that returns true for genes, Transcripts and Exons so this
     is muddled - also this should probably go in SeqFeatureI. - MG
  */
  public boolean isAnnotTop() {
    return annotationRoot;
  }

  public boolean hasAnnotatedFeature() { return true; }

  public AnnotatedFeatureI getAnnotatedFeature() { return this; }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getOwner() {
    return owner;
  }

  /** Returns transcript exon number with non consensus acceptor. Return -1 
      by default - Transcript overrides. */
  public int getNonConsensusAcceptorNum() { return -1; }
  /** Returns transcript exon number with non consensus donor. Returns -1 by
      default - Transcript overrides. */
  public int getNonConsensusDonorNum() { return -1; }
  /** Return true if non consensus splicing has been deemed ok. This is set
      by curator with method that takes boolean. Overridden by Transcript. */
  public boolean nonConsensusSplicingOkay() { return false; }

  /** Set whether its ok to have non-consensus splice site - no-op overridden
      by Transcript */
  public void nonConsensusSplicingOkay(boolean okay) {}
//   /** Returns true if annotated feature has/is a transcript. Rename isTranscript? 
//       returns false by default */
//   public boolean hasTranscript() { return false; }
//   /** If hasTranscript is true returns its transcript */
//   public Transcript getTranscript() { return null; } // exception?

  /** by default return null - overridden by Transcript subclass */
  public SequenceI get_cDNASequence() { return null; }

//  /** by default return null - overridden by Transcript subclass */
//  public String derivePeptideName(String cdnaName) { return null; }

  /** Return ith AnnotatedFeatureI child */
  public AnnotatedFeatureI getAnnotChild(int i) {
    return (AnnotatedFeatureI)getFeatureAt(i);
  }

  /** Methods for getting/setting dbxrefs have been moved to SeqFeatureI */
}
