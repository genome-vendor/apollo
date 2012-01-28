package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.SeqFeatureI;
import apollo.config.ApolloNameAdapterI; // temp

import org.apache.log4j.*;

/**
 * Transaction class for updating.
 *
 * @author wgm
 */
public class UpdateTransaction extends Transaction {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(UpdateTransaction.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // Previous value updated from
  private Object previousValue;
  private transient ApolloNameAdapterI nameAdapter; // temp

  public UpdateTransaction() {
  }
  
  /**
   * An overloaded constructor
   * @param feature the updated SeqFeatureI object
   * @param part the updated part (It is more like a name thing, not the actual
   * value) - phase this constructor out for the other one?
   */
  public UpdateTransaction(SeqFeatureI feature, TransactionSubpart part) {
    setSeqFeature(feature);
    setSubpart(part);
  }
  
  public UpdateTransaction(SeqFeatureI feat, TransactionSubpart ts, Object oldValue,
                           Object newValue) {
    this(feat,ts);
    setOldSubpartValue(oldValue);
    setNewSubpartValue(newValue);
  }

  public UpdateTransaction(Object source, SeqFeatureI feat, TransactionSubpart ts,
                           Object oldValue, Object newValue) {
    this(feat,ts);
    setSource(source);
    setOldSubpartValue(oldValue);
    setNewSubpartValue(newValue);
  }
   
  /**
   * Get the updated SeqFeatureI
   * @return
   */
  public SeqFeatureI getUpdatedSeqFeature() {
    return getSeqFeature();
  }

  public SeqFeatureI getUpdatedSeqFeatureClone() {
    return getSeqFeatureClone();
  }

  public boolean isUpdate() { return true; }

  /** fiddling about... trying this out */
  void undo() {
    setValue(getOldSubpartValue(),getOldId()); // id is only needed for type hack
  }

  // ??
  public void editModel() {
    // automatically set preValue? setPreValue(getSubpart().getValue())???
    setValue(getNewSubpartValue(),getNewId()); // id is just for type hack
  }

  /** updateId is just for type hack - fix eventually */
  private void setValue(Object value,String updatedId) { 
    getSubpart().setValue(getSeqFeature(),value);
    // this hack is for type changes - not explicit id changes
    // eventually type changes will have all their transactions in them
    if (!getSubpart().isId() && idHasChanged()) {
      // this should go to name adapter! name adapter should determine if id 
      // change causes name change - complicated by temp ids
      if (idEqualsName()) { 
        setNameFromId(updatedId);
      }
      executeIdChange(updatedId);
    }
//     if (idHasChanged())
//       executeIdChange(getNewId()); 
  }

  /** Returns true if transaction has caused the id to change.
      type changes can do this */
  private boolean idHasChanged() {
    // id change detected by comparing explicitly set id with feat id
    if (getOldId() == null || getNewId() == null)
      return false;
    return !getOldId().equals(getNewId());
  }

  private boolean idEqualsName() {
    return getSeqFeature().getId().equals(getSeqFeature().getName());
  }


  // temporary - for type changes that cause id changes -> TransactionUtil?
  private void executeIdChange(String id) {
    TransactionUtil.setId(getAnnotatedFeature(), id, nameAdapter);
  }

  /** This is to undo id changes (caused by type change at the moment). all the 
      id changes dont get transmitted so need name adapter to do all of the id changes
      needed. at some point this should be ammended where all the id & name changes
      do get sent out as transactions. this will do for now. */
  public void setNameAdapter(ApolloNameAdapterI nameAdapter) {
    this.nameAdapter = nameAdapter;
  }

  /** this is temporary - type change transactions dont come with name transactions
      they cause, for now have to derive, eventually put out the name transactions */
  private void setNameFromId(String id) {
    AnnotatedFeatureI topAnnot = getAnnotatedFeature();
    // set top annot name with new id
    topAnnot.setName(id);
    // set transcript names from id
    if (topAnnot.isAnnotTop()) { // should always be true
      for (int i=0; i < topAnnot.size(); i++) {
        AnnotatedFeatureI trans = topAnnot.getAnnotChild(i);
        nameAdapter.setTranscriptNameFromAnnot(trans,topAnnot);
      }
    }
  }

  AnnotationChangeEvent generateAnnotationChangeEvent() {
    if (getSource() == null) {
      setSource(this); // not good
      logger.warn("UpdateTransaction does not have a source. setting to self", new Throwable());
    }
    return new AnnotationUpdateEvent(this);
  }

  protected String getOperationString() { return "UPDATE"; }

  /** 
   * Short (one-line) summary of the object; concise alternative to toString() 
   * that displays every instance variable set in one of the constructors,
   * plus date and author.
   */
  public String oneLineSummary() {
    // not displaying: source
    StringBuffer sb = new StringBuffer();
    sb.append("Upd[");
    sb.append("feat=" + getSeqFeature().getId());
    sb.append(",date=" + date);
    sb.append(",author=" + author);
    sb.append(",subpart=" + getSubpart());
    sb.append(",oldval=" + getOldSubpartValue());
    sb.append(",newval=" + getNewSubpartValue());
    sb.append("]");
    return sb.toString();
  }
}
