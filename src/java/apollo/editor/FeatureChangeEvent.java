package apollo.editor;

import java.util.EventObject;
import java.util.Vector;
import apollo.datamodel.*;

import org.apache.log4j.*;

/**
 * A controller managed event class which signals when a change is made to
 * a set of features and what type of change occurred. Objects interested
 * in listening for these event should implement the FeatureChangeListener
 * interface and register with the controller.
 */

public abstract class FeatureChangeEvent extends EventObject {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------
  protected final static Logger logger = LogManager.getLogger(FeatureChangeEvent.class);

  /** Replace these with TransactionOperation! */
  public static final int            ADD          = 1;
  public static final int            DELETE       = 2;
  //public static final int            LIMITS       = 3;
  // When is STATUS used?? - its not - its never fired - its checked for in
  // FeatureEditorDialog and thats it
  //public static final int            STATUS       = 4;
  // this forces a redraw after features have been added
  // or deleted
  public static final int            REDRAW       = 5;
  // This brings the drawables into accord with the
  // current curation, adding and removing drawables
  // as necessary
  public static final int            SYNC         = 6;
  /* This changes the class of the drawables if the
     annotation type has changed */
//  public static final int            NAME         = 7;
//  public static final int            ID           = 8;

  // AnnotationChangeEvent uses 10-15

  public static final int            SPLIT        = 20;
  public static final int            MERGE        = 21;
  /** Replace means rip out the old, put in new feat */
  public static final int            REPLACE      = 22;
  /** Update means only one part of the feature has changed as hopefully
      described by ObjectClass or SubObjectClass or something like that */
//  public static final int            UPDATE       = 23;
  // public static final int     TRANSLATION_CHANGE = 24;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private     int            operation;
  private     SeqFeatureI    changeTop;
  // make this behave like the other getFeatures
  //protected     Vector features          = new Vector(2);
  //private SeqFeatureI feature1;
  private SeqFeatureI changedFeature;
  //private SeqFeatureI feature2;
  /** parent only explictly set for deletes */
  protected SeqFeatureI parentFeature;
  /** Until we get rid of REPLACE - artifact of annot info doing undo kluge */
  private AnnotatedFeatureI replacedFeature;

  //private boolean editSessionDone = false;

  private Transaction transaction;

  private TransactionSubpart transactionSubpart;

  /** If true, event is not part of a series of events, its stand alone,
      and isEndOfEditSession returns true - dont need separate end of session event 
      should this be called endOfSession - and be used by real events at the end of
      a session? that might be murky territory - i think compound events will solve
      a lot of these issues - which will be needed for undo */
  private boolean isSingularEvent;

  /**
   * This constructor needs to be vanquished i think. Going away from
   generic feature1 and feature 2.
   */
  public FeatureChangeEvent(Object      source,
                            SeqFeatureI changeTop,
                            int         operation,
                            SeqFeatureI feature1,
                            SeqFeatureI feature2) {
    super(source);
    this.changeTop     = changeTop;
    this.operation    = operation;
    // supposed to be the parent
    //this.features.addElement(feature1);
    //this.feature1 = feature1;
    // for now - this is usually true but not always
    this.parentFeature = feature1;
    // supposed to be the child
    //this.features.addElement(feature2);
    // for now - not sure if this is always so
    //this.feature2 = feature2;
    this.changedFeature = feature2;
    //editSessionDone = false;
  }

  /** No subpart */
  protected FeatureChangeEvent(Object source, SeqFeatureI changedFeature) {
    super(source);
    //feature1 = feature; // ???
    this.changedFeature = changedFeature;
    // set parent? set top? or queried on demand
  }
  
  /** With subpart */
  protected FeatureChangeEvent(Object source, SeqFeatureI changedFeature,
                               TransactionSubpart subpart) {
    this(source,changedFeature);
    this.transactionSubpart = subpart;
  }

//   FeatureChangeEvent(Object source,TransactionOperation op,SeqFeatureI feature) {
//     super(source);
//     //this.transOp = op;
//     // operation int?
//   }

  /** For edit session done event. */
  protected FeatureChangeEvent(Object source) {
    super(source);
    //setEndOfEditSession(true);
  }

  /** If end of edit session or REPLACE then not Transaction. same with REDRAW and
      SYNC but I think these should be replaced by end of edit session. */
  boolean isTransactionOperation() {
    if (isEndOfEditSession() ||
        operation == REDRAW ||
        operation == SYNC ) //|| isReplace())
      return false;
    else
      return true;
  }

//   protected void setEndOfEditSession(boolean end) {
//     editSessionDone = end;
//   }

  /** once compound transactions are fully in place i think this can be phased out
      essentially this can be seen as a hack around a lack of compound transactions.*/
  public boolean isEndOfEditSession() {
    // temporary til we get rid of REDRAW
    if (getOperation() == REDRAW || getOperation() == SYNC) // SYNC??
      return true;
    // If event is not part of compound event then the event is the end of the session
    // will this work?
    return isSingularEvent;
  }

  /** If true then event is a loner not compound - and isEndOfEditSession will be
      true. This compound stuff will be refactored once we have to do it for real
      for undo - for now this will do */
  void setSingularEventState(boolean isSingularEvent) {
    this.isSingularEvent = isSingularEvent;
  }

  public Object getSource() {
    return source;
  }

  public SeqFeatureI getChangeTop() {
    return changeTop;
  }

  protected void setChangeTop(SeqFeatureI changeTop) {
    this.changeTop = changeTop;
  }

  public int getOperation() {//getType() {
    return operation;
  }
//   /** For switch statements */
//   public int getOperationAsInt() {
//     // operation.getInt(); // TransactionOperation in future
//     return operation;
//   }


  /** This takes place of ObjectClass ANNOTATION */
  public boolean isRootAnnotChange() {
    if (getChangedFeature() == null) // compound
      return false;
    if (getChangedFeature().getAnnotatedFeature() == null)
      return false;
    return getChangedFeature().getAnnotatedFeature() == getChangeTop();
  }

  /** Replaces Object Class TRANSCRIPT */
  public boolean isTranscriptChange() {
    SeqFeatureI cf = getChangedFeature();
    if ((cf == null) || (cf.getFeatureType() == null))
      return false;
    // is it bad to rely on type string?
    return cf.getFeatureType().equals("transcript");
  }

  /** Replaces Object Class EXON */
  public boolean isExonChange() {
    SeqFeatureI cf = getChangedFeature();
    if ((cf == null) || (cf.getFeatureType() == null))
      return false;
    return cf.getFeatureType().equals("exon");
  }

  // i think this might be able to be abstract - look into
  protected String getOperationAsString() {
    return asString(operation);
  }

  public boolean isAdd() {
    return getOperation() == ADD;
  }

  /** Convenience. If operation is ADD then feature2 is the added feature 
   is getChangedFeature good enough? get rid of? */
  public SeqFeatureI getAddedFeature() {
    if (!isAdd()) return null;
    return getChangedFeature();
  }

  /** This is the feature that has been added,deleted,updated,... */
  public SeqFeatureI getChangedFeature() {
    return changedFeature;
  }

  /** Set the parent of the edited feature. I think this will only be needed for
      deletes, where parent and child have been severed (how tragic) */
  public void setParentFeature(SeqFeatureI parent) {
    parentFeature = parent;
  }

  public SeqFeatureI getParentFeature() {
    if (parentFeature != null)
      return parentFeature;
    // this should just be getFeature()
    else return getChangedFeature().getRefFeature(); // shouldnt be null
  }

  public boolean isDelete() {
    return getOperation() == DELETE;
  }

  public boolean isMerge() {
    return getOperation() == MERGE;
  }
  public boolean isSplit() {
    return getOperation() == SPLIT;
  }

  /** Move is a special case of UPDATE - updating parent subpart 
      AnnotationUpdateEvent overrides this */
  public boolean isMove() { return false; }

//   public SeqFeatureI getDeletedFeature() {
//     //return getTransaction().getDeletedFeature();
//   }

//   public boolean isReplace() {
//     return getOperation() == REPLACE;
//   }

  //public AnnotationReplaceEvent getReplaceEvent ??
  // OR
  /** Returns feature replaced for replace event. getChangedFeature
      returns the feature that has taken its place. Replace is solely
      for annot info editors limited undo. Once we have real undo
      Replace will go by the wayside. */
  public AnnotatedFeatureI getReplacedFeature() {
    return replacedFeature;
  }

  protected void setReplacedFeature(AnnotatedFeatureI replacedFeature) {
    this.replacedFeature = replacedFeature;
  }

  public boolean isSync() {
    return getOperation() == SYNC;
  }

  /** Default false - overridden by AnnotationUpdateEvent */
  public boolean isUpdate() {
    return false;
  }

  /** Update event has a bunch of stuff - need to be able to acces this from 
      superclass - separate UpdateDetail class with update stuff? EventDetail?
      for now just get the update event */
  public UpdateDetailsI getUpdateDetails() {
    return null; // AnnotationUpdateEvent ovverrides this
  }

  public boolean hasSubpart() {
    return getSubpart() != null;
  }

  /** Returns null if no subpart */
  public TransactionSubpart getSubpart() {
    return transactionSubpart;
  }

  void setSubpart(TransactionSubpart subpart) {
    transactionSubpart = subpart;
  }

  /** whether event is a compound event - ie contains child transactions/events.
      default is false. */
  public boolean isCompound() { return false; }

//   public boolean limitsChanged() {
//     // update has to have subparts
//     return isUpdate() && (getSubpart() == TransactionSubpart.START
//                           || getSubpart() == TransactionSubpart.END);
//   }

  /** what about result object classes? */
  String getObjectClassAsString() {
    if (isCompound())
      return "COMPOUND";
    if (isRootAnnotChange())
      return "ANNOTATION";
    if (isTranscriptChange())
      return "TRANSCRIPT";
    if (isExonChange())
      return "EXON";
    return "UNKNOWN OBJECT CLASS";
  }

  public String toString () {
    return (getOperationAsString() + "ing " +
            getObjectClassAsString() +
            " on strand " + 
            (getChangeTop() == null ? "top?" : Integer.toString(getChangeTop().getStrand())) +
            ", top feature " +
            (getChangeTop() == null ? "top?" : getChangeTop().getName()) +
            ", parent " +
            (getParentFeature() == null ? "parent?" : getParentFeature().getName()) +
            ", changed feature " +
            (getChangedFeature() == null ?
             "changedFeature?" : getChangedFeature().getName()));
  }
  static String asString(int operation) {
    // NOTE: No breaks
    switch (operation) {
    case ADD:
      return "ADD";
    case DELETE:
      return new String("DELETE");
    case REDRAW:
      return new String("REDRAW");
//     case STATUS:
//       return new String("STATUS");
//     case LIMITS:
//       return new String("LIMITS");
    case SYNC:
      return new String("SYNC");
//    case NAME:
//      return new String("NAME");
//    case ID:
//      return new String("ID");
    case SPLIT:
      return new String("SPLIT");
    case MERGE:
      return new String("MERGE");
    case REPLACE:
      return new String("REPLACE");
//    case UPDATE:
//      return TransactionOperation.UPDATE.toString();
    default:
      return new String("UNKNOWN OPERATION (" + operation + ")");
    }
  }

//   public static int toNum(String operation) {
//     if (operation.equalsIgnoreCase("ADD"))
//       return ADD;
//     if (operation.equalsIgnoreCase("DELETE"))
//       return DELETE;
//     if (operation.equalsIgnoreCase("REDRAW"))
//       return REDRAW;
//     if (operation.equalsIgnoreCase("STATUS"))
//       return STATUS;
// //     if (operation.equalsIgnoreCase("LIMITS"))
// //       return LIMITS;
//     if (operation.equalsIgnoreCase("SYNC"))
//       return SYNC;
//     if (operation.equalsIgnoreCase("NAME"))
//       return NAME;
//     if (operation.equalsIgnoreCase("ID"))
//       return ID;
//     if (operation.equalsIgnoreCase("SPLIT"))
//       return SPLIT;
//     if (operation.equalsIgnoreCase("MERGE"))
//       return MERGE;
//     if (operation.equalsIgnoreCase("REPLACE"))
//       return REPLACE;
//     else {
//       //      logger.error("FeatureChangeEvent.toNum: unknown operation " + operation); // DEL
//       return -1;  // unknown
//     }
//   }
}
