package apollo.editor;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.DbXref;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Synonym;

import org.apache.log4j.*;

  /** Enumeration/value class that captures the subpart of the feature that is
      effected by the transaction. This is an optional part of a Transaction as
      some transactions effect the whole feature (e.g. delete transcript). Do I
      go too far with this enumeration stuff? I think its nice to have a strongly
      typed enumeration over a loose string but it is more work to add more subparts*/
public class TransactionSubpart implements Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TransactionSubpart.class);

  private static Map stringToSubpart = new HashMap();

  public static TransactionSubpart PARENT = new TransactionSubpart("PARENT",
                                                                   SeqFeatureI.class);

  public static TransactionSubpart ID = new IdSubpart();

  public static TransactionSubpart NAME = new NameSubpart();

  public static TransactionSubpart TYPE = new TypeSubpart();

  public static TransactionSubpart COMMENT = 
  new TransactionSubpart("COMMENT",Comment.class);

  public static TransactionSubpart SYNONYM = new SynonymSubpart();

  public static TransactionSubpart LIMITS =
  new TransactionSubpart("LIMITS",RangeI.class);

  //Peptide limits are not represented by an actual range object in the datamodel
  //so is it misleading to have it represent by a range class in the subpart? fuzzy area
  // it probably should be a range in the datamodel (if not its own feature!)
  // Peptide should have its own feature!
  public static TransactionSubpart PEPTIDE_LIMITS =
  new TransactionSubpart("PEPTIDE_LIMITS",RangeI.class);
  
  /** Yuck! we need a Peptide feature! this subpart is just a hack around a lack
      of peptide feature. This is SequenceI's displayId */
  public static TransactionSubpart PEPTIDE_NAME = new PeptideNameSubpart();
  /** This maps to SequenceI accessionNo */
  public static TransactionSubpart PEPTIDE_ID = new PeptideIdSubpart();
  
  public static TransactionSubpart CDNA_NAME = new CdnaNameSubpart();

  public static TransactionSubpart REPLACE_STOP = 
  new TransactionSubpart("REPLACE_STOP",Boolean.class);

  public static TransactionSubpart PLUS_1_FRAMESHIFT =
  new TransactionSubpart("PLUS_1_FRAMESHIFT",Integer.class);

  public static TransactionSubpart MINUS_1_FRAMESHIFT =
  new TransactionSubpart("MINUS_1_FRAMESHIFT",Integer.class);

  // SEQUENCING_ERROR is not a subpart - but a bonafide feat - take this out! 
  //public static TransactionSubpart SEQUENCING_ERROR =
  //new TransactionSubpart("SEQUENCING_ERROR",Integer.class); // integer??

  public static TransactionSubpart NON_CONSENSUS_SPLICE_OKAY =
  new TransactionSubpart("NON_CONSENSUS_SPLICE_OKAY",Boolean.class);

  public static TransactionSubpart IS_DICISTRONIC = 
  new TransactionSubpart("dicistronic",Boolean.class);

  public static TransactionSubpart IS_PROBLEMATIC =
  new TransactionSubpart("IS_PROBLEMATIC",Boolean.class);
  
  public static TransactionSubpart FINISHED = 
  new TransactionSubpart("FINISHED",Boolean.class);

  public static TransactionSubpart DBXREF = new TransactionSubpart("DBXREF",
                                                                   DbXref.class);

  public static TransactionSubpart DESCRIPTION = new TransactionSubpart("DESCRIPTION",
                                                                        String.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // private Map subpartToString = new HashMap(); ??
  private String subpartString;
  private Class type;

//   // phase this constructor out - require type!
//   private TransactionSubpart(String subpartString) {
//     this.subpartString = subpartString;
//     stringToSubpart.put(subpartString,this);
//   }

  private TransactionSubpart(String subpartString,Class type) {
    this.subpartString = subpartString;
    stringToSubpart.put(subpartString,this);
    this.type = type;
  }
  
  public String toString() {
    return this.subpartString;
  }

  public static TransactionSubpart stringToSubpart(String subpartString) {
    return (TransactionSubpart)stringToSubpart.get(subpartString);
  }

  public boolean isBoolean() {
    if (type == null)
      return false;
    return type.equals(Boolean.class);
  }

  public boolean isString() {
    if (type == null)
      return false;
    return type.equals(String.class);
  }

  public boolean isType() {
    return this == TYPE;
  }

  public boolean isId() { return false; } // subclass overrides

  public boolean isInt() {
    if (type == null)
      return false;
    return type.equals(Integer.class);
  }

  public boolean isName() {
    return this == NAME;
  }

  public boolean isComment() {
    return this == COMMENT;
  }

  public boolean isSynonym() {
    return this == SYNONYM;
  }

  public boolean isLimits() {
    return type.equals(RangeI.class);
  }
  
  public boolean isFinished() {
    return this == FINISHED;
  }

  public boolean isProblematic() {
    return this == IS_PROBLEMATIC;
  }  

  public boolean isDbXref() {
    return this == DBXREF;
  }  

  public boolean isDescription() {
    return this == DESCRIPTION;
  }  

  public void setValue(SeqFeatureI feat, Object value) {
    // eventually make abstract
    logger.debug("setValue called with no subclass for subpart "+this);
  }

  public void setValue(SeqFeatureI feat, Object value, boolean isAdd) {
    logger.debug("setValue called with no subclass for subpart "+this);
    // .. subclass override - really this is abstract
  }

  private void checkValue(Object value) {
    // its ok for value to be null, esp undo (but may be prog error)
    if (value == null)
      logger.debug("Value for subpart "+this+" is null");
    if (!valueIsCorrectType(value)) {
      String m = "Wrong type for subpart "+this+" got "+value.getClass()+
        " expected "+type;
      throw new RuntimeException(m);
    }
  }

  private boolean valueIsCorrectType(Object value) {
    // if value is null no way to test type. no way to know if intentional or bug.
    if (value == null)
      return true;
    return value.getClass().equals(type);
  }

  /** a little experimenting... just trying this out  - 
      will being static be problematic? */
  private static class NameSubpart extends TransactionSubpart {

    private NameSubpart() {
      super("NAME",String.class);
    }

    public boolean isName() { return true; }
    
    public void setValue(SeqFeatureI feat, Object name) {
      super.checkValue(name); // checks name is String
      feat.setName((String)name);
    }
  }

  private static class TypeSubpart extends TransactionSubpart {
    private TypeSubpart() {
      super("TYPE",String.class);
    }
    
    public boolean isType() { return true; }

    public void setValue(SeqFeatureI feat, Object type) {
      super.checkValue(type); // String check
      //String oldType = feat.getType();
      feat.setFeatureType((String)type);
      feat.setTopLevelType((String)type); // ?? only true for top level!
      // if (feat.isProteinCodingGene()) set trans start and stop?? do that here??

      // ids and names??? ... since theres only the type transaction now we hafta put it
      // here - eventually there maybe transactions for the ids and names as well in 
      // a compound transaction
    }

  }

  private static class IdSubpart extends TransactionSubpart {
    private IdSubpart() { super("ID",String.class); }

    public boolean isId() { return true; }
   
    public void setValue(SeqFeatureI feat, Object id) {
      super.checkValue(id); // string check
      feat.setId((String)id);
    }
  }

  /** If peptide becomes a feature this class wont be necasary */
  private static class PeptideNameSubpart extends TransactionSubpart {

    private PeptideNameSubpart() { super("PEPTIDE_NAME",String.class); }

    public void setValue(SeqFeatureI feat, Object name) {
      super.checkValue(name); // string check
      feat.getPeptideSequence().setName((String)name); // ????
      // if pep feat -> feat.setName((String)name); - just use NameSubpart
    }
  }

  /** PEPTIDE ID SUBPART inner class */
  private static class PeptideIdSubpart extends TransactionSubpart {
    private PeptideIdSubpart() { super("PEPTIDE_ID",String.class); }
    public void setValue(SeqFeatureI feat, Object name) {
      super.checkValue(name); // string check
      feat.getPeptideSequence().setAccessionNo((String)name);
      // if pep feat -> feat.setId((String)name); - just use IdSubpart
    }
  }



  private static class CdnaNameSubpart extends TransactionSubpart {
    
    private CdnaNameSubpart() { super("CDNA_NAME",String.class); }

    public void setValue(SeqFeatureI feat, Object name) {
      super.checkValue(name);
      if (!feat.hasAnnotatedFeature())
        return; // shouldnt happen - error?
      feat.getAnnotatedFeature().get_cDNASequence().setAccessionNo((String)name);
    }
  }

  private static class SynonymSubpart extends TransactionSubpart {

    private SynonymSubpart() { super("SYNONYM",Synonym.class); }

    public void setValue(SeqFeatureI feat,Object syn,boolean isAdd) {
      if (feat == null || !feat.hasAnnotatedFeature() || syn == null) {
        logger.debug("Failed to set syn val feat "+feat+" syn "+syn);
        return; // err msg? exception?
      }
      super.checkValue(syn); // string check
      AnnotatedFeatureI annot = feat.getAnnotatedFeature();
      if (isAdd)
        annot.addSynonym((Synonym)syn);
      else // delete
        annot.deleteSynonym((Synonym)syn);
    }
  }

}
