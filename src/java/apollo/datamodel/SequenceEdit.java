package apollo.datamodel;

import java.lang.String;
import java.io.Serializable;

import org.apache.log4j.*;

/** seq edits are really seq features (very tiny ones) arent they
    so subclass seq feature? do we still need seq edit? or seq edit has
    a seq feature? chado models indels as features which makes
    sense so why not apollo? needs to be annotated feat for chado
    transaction transformer sake - to be consistent with other annots */
public class SequenceEdit extends AnnotatedFeature implements Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SequenceEdit.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  SequenceI refSeq;
  String residue;
  //String edit_type;
  //int position; --> seqFeat low

  // this constructor is used by jalview - who knew
  public SequenceEdit(SequenceI refSeq,
                      String edit_type, int position, String residue) {
    this(edit_type, position, residue);
    logger.debug("seq edit from jalview");
    this.refSeq = refSeq;
    refSeq.addSequenceEdit(this); // well thats a side effect
  }

  public SequenceEdit(String edit_type, int position, String residue) {
    // theres actually no good way to express coords for an insertion in
    // base-oriented - will have to deal with this in interbase conversion
    setLow(position);
    setHigh(position);
    //this.edit_type = edit_type;
    setFeatureType(edit_type);
    //this.position = position;
    if (edit_type.equals(SequenceI.SUBSTITUTION) 
        || edit_type.equals(SequenceI.INSERTION)) {
	if (residue != null)
          this.residue = residue;
	else {
          logger.warn("Substitution/Insertion genomic sequence error but no base provided");
          this.residue = "N";
	}
    }
  }

  public SequenceEdit(String res) {
    residue = res; // cant chek null with type - dont have type yet - oh well
  }

  public int getPosition() {
    //return position;
    return getLow(); // getHigh?
  }

  public String getResidue () {
    return residue;
  }

  public SequenceI getRefSequence() {
    return refSeq;
  }

  public void setRefSequence(SequenceI refSeq) {
    if (refSeq == null) {
      logger.error("seq edit getting null ref seq");
      return;
    }
    this.refSeq = refSeq;
    // why not right?
    // el - have to comment it out to allow correct undo operation
    //refSeq.addSequenceEdit(this);
  }

  //public String getFeatureType() {return edit_type;x}

  /** phase out for getFeatureType? yes! same thing - although actually 
   CLEAR_EDIT/NOP is really the deletion of the indel - hmmm */
  public String getEditType() {
    return getFeatureType();
  }

  /** should come from config/SO - used in jdbc adapter */
  public static boolean typeIsSeqError(String type) {
    return type.equals(SequenceI.INSERTION) || type.equals(SequenceI.DELETION)
      || type.equals(SequenceI.SUBSTITUTION);
  }
  public boolean isInsertion() {
  	return getFeatureType().equals(SequenceI.INSERTION);
  } 
  public boolean isDeletion() {
  	return getFeatureType().equals(SequenceI.DELETION);
  }
  public boolean isSubstitution() {
  	return getFeatureType().equals(SequenceI.SUBSTITUTION);
  }  
  public boolean isSequencingError() {
  	return typeIsSeqError(getFeatureType());
  }
  
  String getHashKey() { return getPosition()+""; }
}

