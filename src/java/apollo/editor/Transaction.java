package apollo.editor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.SeqFeatureI;

import apollo.config.Config;

import org.apache.log4j.*;

/**
 *  Class for keeping track of transactions (changes to annotations or
 *  annotation subparts (transactions, exons)).
 *  Built and talked to by AnnotationChangeEvents and also by GAMEAdapter
 *  as it reads in old transactions.
 this class is in dire need of some spring cleaning!
 */

public class Transaction implements Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Transaction.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected Date date;
  protected String author;

  // Type of object that was changed:
  // AnnotationChangeEvent.EXON, AnnotationChangeEvent.COMMENT, etc.
  private TransactionClass transactionClass; // ?? phase out -> figure out

  // Type of transaction--new_gene, changed_id, etc.
  private TransactionOperation transactionOperation;// phase out -> subclasses

  /** Enumeration/value class that captures the subpart of the feature that is
      effected by the transaction. This is optional as some transactions effect
      the whole feature (e.g. delete transcript) */
  private TransactionSubpart transactionSubpart;

  /** The touched subpart value. For example, A comment is deleted, updated
  // or added, this Comment object is subpartValue. It can also be a String
  // (e.g. for synonym). This is the post/new value. Rename newValue? */
  private Object newSubpartValue;

  // Previous value updated from
  private Object oldSubpartValue;
  
  // Instead of deciding in advance which properties (e.g. ID, name) we want
  // to save, make it open-ended.  We'll save the old (before transaction) and
  // new properties in separate hashes.
  protected Hashtable oldProperties = new Hashtable(1, 1.0F);
  protected Hashtable newProperties = new Hashtable(1, 1.0F);

  /** If this is a child of a compound transactions, parentTransaction is a ref to
      that parent, null if no parent. */
  private CompoundTransaction parentTransaction;

  public static int OLD = 1;
  public static int NEW = 2;

  /** do we need more than one feature? */
  private SeqFeatureI feature;
  
  /** 
   * A clone of <code>feature</code>, this allows us to know the original state
   * of the SeqFeature as it was affected by the transaction, even if it has
   * subsequently been modified.  Will not be set unless 
   * Config.getSaveClonedFeaturesInTransactions == true.
   */
  private SeqFeatureI featureClone;

  private SeqFeatureI parentFeature;
  private SeqFeatureI parentFeatureClone;
  
  // The source that generate this Transaction object (eg AnnotEditor,FED,...)
  private transient Object source;

  private int subpartRank;

  // 0-based rank of the affected feature with respect to its parent (at the time of the transaction)
  private int rank;

  private String oldId=null;
  private String newId;

  // i think this constructor is pase
  public Transaction(FeatureChangeEvent ace) {
    this();
    setOperation(ace.getOperationAsString());
    setObjectClass(ace.getObjectClassAsString());
    SeqFeatureI sf = ace.getChangedFeature();
    if (sf != null) { setSeqFeature(sf); }
  }

  public Transaction() {
    date = new Date();
    author = UserName.getUserName();
  }

  /** for now does nothing - subclasses implement */
  void undo() {
    logger.error("Transaction subclass needs to implement undo "+this);
  }

  public void setDate(String date) {
    try {
      this.date = new SimpleDateFormat().parse(date);
    } catch (Exception ex) {
      this.date = new Date();
    }
  }
  
  public void setDate(Date date) {
    this.date = date;
  }

  public Date getDate() {
    return date;
  }

  public void setAuthor(String name) {
    author = name;
  }

  public String getAuthor() {
    return author;
  }

  public boolean isUpdate() { return false; }

  public boolean isAdd() { return false; }
  public boolean isAddPeptide() { return false; }

  public boolean isDelete() {
    return false;
    //return getOperation() == TransactionOperation.DELETE;
  }

  /** default false. CompoundTransactions overrides with true. */
  public boolean isCompound() { return false; }

  public boolean isSplit() { return false; }

  public boolean isMerge() { return false; }

  public boolean isUpdateParent() { return false; }
  
  /** If split compound transaction, this returns the new split off feature.
      getSeqFeature() returns the feature split off from. */
  public AnnotatedFeatureI getNewSplitFeature() { return null; }

  public SeqFeatureI getDeletedFeature() {
    if (!isDelete())
      return null; // shouldnt happen
    return feature; // ??? //getSecondFeature();
  }

  public void setSubpart(TransactionSubpart transactionSubpart) {
    this.transactionSubpart = transactionSubpart;
  }

  private void setSubpart(String subpartString) {
    transactionSubpart = TransactionSubpart.stringToSubpart(subpartString);
  }
  
  public TransactionSubpart getSubpart() {
    return this.transactionSubpart;
  }

  /** A transaction does not necasarily have a subpart - could be for whole annot*/
  boolean hasSubpart() { return getSubpart() != null; }
  
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // value classes wont be read in as final statics saved. use string to get statics
    //setObjectClass(transactionClass.toString());
    //setOperation(transactionOperation.toString());
    // guanming mightve scrapped this for Object subpartValue?
    if (transactionSubpart != null)
      setSubpart(transactionSubpart.toString());
  }


  /** different source than trans. should trans even have a source? does it need it?*/
  public AnnotationChangeEvent generateAnnotationChangeEvent(Object source) {
    //return new AnnotationChangeEvent(source,this);
    setSource(source); // why not
    AnnotationChangeEvent ace = generateAnnotationChangeEvent();
    ace.setSource(source);
    return ace;
  }

  public AnnotationChangeEvent generateUndoChangeEvent(Object source)
  {
    setSource(source); // why not
    AnnotationChangeEvent ace = generateUndoChangeEvent();
    ace.setSource(source);
    return ace;    
  }
  
  /** sets ace source to trans source. subclasses override this */
  AnnotationChangeEvent generateAnnotationChangeEvent() {
    logger.warn("subclass not overriding Transaction.genACE()" + this);
    return new AnnotationChangeEvent(this);
  }

  /** sets ace source to trans source. subclasses override this */
  AnnotationChangeEvent generateUndoChangeEvent() {
    logger.warn("subclass not overriding Transaction.genUndoCE()" + this);
    AnnotationChangeEvent ace = new AnnotationChangeEvent(this);
    ace.setUndo(true);
    return ace;
  }
  
  /**
   * Set the touched SeqFeatureI object.
   * @param feature
   */
  public void setSeqFeature(SeqFeatureI feature) {
    this.feature = feature;
    if (feature == null) {
      logger.error("Transaction set with null feature.", new Throwable());
    }
    if (Config.getSaveClonedFeaturesInTransactions()) {
      featureClone = feature.cloneFeature();
    }
  }

  /**
   * Get the touched SeqFeatureI object.
   * @return
   */
  public SeqFeatureI getSeqFeature() {
      return this.feature;
  }
  
  /**
   * Return a clone of the touched SeqFeatureI object; it should reflect
   * the state of the SeqFeatureI when the transaction actually ocurred.
   * Will return null unless 
   * Config.getSaveClonedFeaturesInTransactions() was set to true when
   * the transaction was created.
   */
  public SeqFeatureI getSeqFeatureClone() {
    return this.featureClone;
  }

  public void setParentFeature(SeqFeatureI parent) {
    this.parentFeature = parent;
    logger.debug("setParentFeature called with feature=" + feature + " parent=" + parent);
    if (feature != null) {
      if (Config.getSaveClonedFeaturesInTransactions()) {
        this.parentFeatureClone = parent.cloneFeature();
      }

      // set rank of child wrt parent
      // TODO - this assumes that the features are sorted, and the documentation in Transcript and FeatureSet
      // isn't too clear on when this is guaranteed to be the case.  Also there's at least one comment that
      // suggests there may be problems with Transcript.sortTransSpliced.
      int rank = parent.getFeatureIndex(this.feature);
      setRank(rank);
    }
  }

  public SeqFeatureI getParentFeature() {
    return parentFeature;
  }

  public SeqFeatureI getParentFeatureClone() {
    return parentFeatureClone;
  }

  /** convenience for transactions on annotated feats (which is the mojority of them)*/
  AnnotatedFeatureI getAnnotatedFeature() {
    return getSeqFeature().getAnnotatedFeature();
  }
   
  public void setSource(Object src) { this.source = src; }
  Object getSource() { return source; }

  /**
   * Set the previouse value for this UpdateTransaction. 
   * @param preValue old value
   */
  public void setOldSubpartValue(Object preValue) {
    this.oldSubpartValue = preValue;
  }

  /** Updates and deletes have pre values, adds do not. */
  boolean hasOldSubpartValue() { return getOldSubpartValue() != null; }

  /**
   * Get the previous value. This value is the one before update carried out.
   Or the value that was deleted.
   * @return
   */
  public Object getOldSubpartValue() {
    return this.oldSubpartValue;
  }
  private boolean isComment() { 
    return transactionSubpart == TransactionSubpart.COMMENT; 
  }

  /** Convenience method for getting old comment */
  public Comment getOldComment() {
    if (!isComment())
      return null; // shouldnt happen - exception? err msg?
    // so ive made both update and delete use prevalue. Previously update was using
    // preValue and delete subpartValue - doesnt it make more sense to be consistent?
    return (Comment)getOldSubpartValue();
  }
  
  public String getOldString() {
    if (!(getOldSubpartValue() instanceof String))
      return null; // shouldnt happen
    return (String)getOldSubpartValue();
  }

  /** feature id needs to be explictly set if id changes (type change) */
  public void setOldId(String id) {
    this.oldId = id;
  }

  public String getOldId() {
    return oldId;
  }

  public void setNewId(String newId) {
    this.newId = newId;
  }

  public String getNewId() {
    // if null return seq feat id?
    return newId;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }

  public int getRank() {
    return this.rank;
  }

  public void setSubpartRank(int rank) {
    this.subpartRank = rank;
  }
  
  public int getSubpartRank() {
    return this.subpartRank;
  }
  
  /**
   * Set the subpart value for this Transaction object. A subpart value
   * is an object that is touched by this Transaction object. For example,
   * a Comment object is updated, added or deleted. This Comment object should
   * be the subpartValue.
   So for adds the added subpart is the subpart value(prevalue is null). 
   For updates the new value is the subpart value, the old is the prevalue. 
   For deletes previously deleted value was subpart value and prevalue was null.
   This seemed inconsistent to me and i changed it to prevaule having deleted value
   and subpart value being null. What this really means is subpartValue is really
   the post or new value. If this change is ok then this should be renamed 
   getNewSubpartValue for clarity.
   * @param obj
   */
  public void setNewSubpartValue(Object obj) {
    this.newSubpartValue = obj;
    if (obj == null) {
      logger.error("Transaction subpart value set to null", new Throwable());
    }
  }
  
  /**
   * @see setSubpartValue(Object).
   * @return
   */
  public Object getNewSubpartValue() {
    return this.newSubpartValue;
  }

  public boolean subpartIsString() {
    return getNewSubpartValue() instanceof String;
  }

  public String getSubpartString() {
    if (!subpartIsString())
      logger.error("subpart is not string");
    return (String)getNewSubpartValue();
  }

  /** overridded by CompositeTransaction. Number of child transactions. */
  public int size() { return 0; }

  /** returns true for comp trans if actually has kid trans */
  public boolean hasKids() { return size() > 0; }


  /** Default return null. Returns ith child transaction if composite. */
  public Transaction getTransaction(int i) { return null; }

  void setParentTransaction(CompoundTransaction parent) {
    this.parentTransaction = parent;
  }

  /** If this is a child of a compound transactions, return compound transaction parent
      return null if have no parent. */
  public CompoundTransaction getParentTransaction() {
    return parentTransaction;
  }

  public boolean hasParentTransaction() {
    return getParentTransaction() != null;
  }

  /** Return all descendant leaves of this transaction. If not compound just return
      self as only item in list. CompoundTrans overrides and gets all leaves. */
  public List getLeafTransactions() {
    ArrayList leaves = new ArrayList(1);
    leaves.add(this);
    return leaves;
  }

  public String toString() {
    String s =  "\nOperation: "+getOperationString()+"\nClass: "+getClassString();
    if (hasSubpart()) s += "\nSubpart: "+getSubpart();
    return s;
  }

  /** Short (one-line) summary of the object; concise alternative to toString() */
  public String oneLineSummary() {
    // TODO: oldId, newId, featureClone, old and new Properties
    StringBuffer sb = new StringBuffer();
    sb.append("Transaction[");
    sb.append("date=" + date);
    sb.append(",author=" + author);
    sb.append(",feature=" + feature.getClass());
    sb.append("]");
    return sb.toString();
  }

  // override!
  protected String getOperationString() { return null; }
  public String getClassString() { return feature.getFeatureType(); } //??
  

  // PHASE OUT/DELETE BELOW....

  // i think this is pase
  public void setOperation(TransactionOperation operation) {
    this.transactionOperation = operation;
  }

  // i think this is pase'
  public void setOperation(String operation) {
    //this.operation = FeatureChangeEvent.toNum(operation);
    this.transactionOperation = TransactionOperation.stringToOperation(operation);
    // if operation == null -> error...
  }
  //public int getOperation() {
  // i think this is trumped by the subclasses
  public TransactionOperation getOperation() {
    return transactionOperation;
  }


//   //public void setObjectClass(int objectClass) {
//   public void setObjectClass(TransactionClass objectClass) {
//     this.transactionClass = objectClass;
//   }
  // pase' - phase out - this is now figured out automatically (annot,trans,exon...)
  public void setObjectClass(String objectClass) {
    //this.objectClass = AnnotationChangeEvent.toNum(objectClass);
    this.transactionClass = TransactionClass.stringToClass(objectClass);
  }
  public TransactionClass getObjectClass() {
    return transactionClass;
  }

  String getObjectClassAsString() {
    if (isCompound())
      return "COMPOUND";
    if (getSeqFeature() == null)
      return null;
    return getSeqFeature().getClass().toString();
  }


  // DELETE....
  // i think the stuff below is pase and should be deleted - investigate

  /** Add a new value to the Vector for this property name.  Store in oldProperties
      or newProperties hash, as specified. */
  public void addProperty(String name, String value, int oldOrNew) {
    Hashtable properties;
    if (oldOrNew == OLD)
      properties = oldProperties;
    else
      properties = newProperties;

    if (value != null && !value.equals("")) {
      Vector values;
      if (properties.get(name) == null) {
        values = new Vector();
        properties.put(name, values);
      } else {
        values = (Vector) properties.get(name);
      }
      values.addElement(value);
      logger.debug("Transaction.addProperty: added " + name + "=" + value + " to " + properties + " for " + oneLineSummary());
    }
  }

  /** Return a single property for name.
      If there's more than one value, return the LAST one, not the first.  */
  public String getProperty(String name, int oldOrNew) {
    Hashtable properties;
    if (oldOrNew == OLD)
      properties = oldProperties;
    else
      properties = newProperties;

    if ((properties == null) || (properties.get(name) == null)) {
      return "";
    } else {
      Vector values = (Vector) properties.get(name);
      return (String) values.lastElement();
    }
  }

  // New 12/2002 CTW - supports multiple properties with same name.
  private Vector getPropertyMulti(String name, int oldOrNew) {
    Hashtable properties;
    if (oldOrNew == OLD)
      properties = oldProperties;
    else
      properties = newProperties;

    if (properties == null)
      return null;
    else
      return (Vector) properties.get(name);
  }


    /** Returns a String of XML representing this transaction.
        indent is a string of spaces. -> GAME adapter? 
        this is old transactions -- delete */
  public String toString(String indent) {
    StringBuffer buf = new StringBuffer();
    buf.append(indent + "<transaction>\n");
    buf.append(apollo.dataadapter.gamexml.GAMESave.writeDate(indent+indent, getDate(), ""));
    buf.append(indent + indent + "<author>" + getAuthor() + "</author>\n");
    buf.append(indent + indent + "<object_class>" + getObjectClass() + "</object_class>\n");
    buf.append(indent + indent + "<operation>" + getOperation() + "</operation>\n");
    // Write the information relevant to this particular type of transaction
    // (Perhaps the different types of transaction should be subclasses, each
    // of which has its own toString, but for now it's easier to just do it
    // this way.)
    String transactionInfo = transactionInfo(indent);
    if (transactionInfo.equals("")) // there was nothing to say about this transaction
      return "";
    buf.append(transactionInfo);
    buf.append(indent + "</transaction>\n");
    return buf.toString();
  }

  private String transactionInfo(String indent) {
    Vector list = new Vector();
    Enumeration e = oldProperties.keys();
    while (e.hasMoreElements()) {
      String prop = (String) e.nextElement();
      if (!list.contains(prop))
        list.addElement(prop);
    }
    e = newProperties.keys();
    while (e.hasMoreElements()) {
      String prop = (String) e.nextElement();
      if (!list.contains(prop))
        list.addElement(prop);
    }
    return infoBlock(indent, list);
  }
  /** Generate <before> and <after> info blocks for a transaction.
   This prints out xml string for Transaction that game uses. I think this
   belongs in the game adapter itself not here. */
  private String infoBlock(String indent, Vector keys) {
    StringBuffer buf = new StringBuffer();
    boolean haveContent = false;

    // <before> block
    buf.append(startTag("before", indent+indent));
    for (int i = 0; i < keys.size(); i++) {
      String key = (String) keys.elementAt(i);
      Vector valuesBefore = getPropertyMulti(key, OLD);
      for (int j = 0; valuesBefore != null && j < valuesBefore.size(); j++) {
        String valueBefore = (String) valuesBefore.elementAt(j);
        if (valueBefore != null && !(valueBefore.equals(""))) {
          buf.append(indent + indent + indent + "<" + key + ">" + valueBefore + "</" + key + ">\n");
          haveContent = true;
        }
      }
    }
    buf.append(endTag("before", indent+indent));

    // <after> block
    buf.append(startTag("after", indent+indent));
    for (int i = 0; i < keys.size(); i++) {
      String key = (String) keys.elementAt(i);
      Vector valuesAfter = getPropertyMulti(key, NEW);
      for (int j = 0; valuesAfter != null && j < valuesAfter.size(); j++) {
        String valueAfter = (String) valuesAfter.elementAt(j);
        if (valueAfter != null && !(valueAfter.equals(""))) {
          buf.append(indent + indent + indent + "<" + key + ">" + valueAfter + "</" + key + ">\n");
          haveContent = true;
        }
      }
    }
    buf.append(endTag("after", indent+indent));

    if (!haveContent)  // If there was nothing interesting to report, send back empty string
      return "";
    else
      return buf.toString();
  }

  private String startTag(String tag, String indent) {
    return(indent+"<" + tag + ">\n");
  }
  private String endTag(String tag, String indent) {
    return(indent+"</" + tag + ">\n");
  }

  // -----------------------------------------------------------------------
  // Class methods
  // -----------------------------------------------------------------------

  /**
   * Traverses a transaction and updates the ids/names of any cloned Transcript objects
   * it finds.  This is a workaround for the problem that the PureJDBCTransactionWriter
   * can only manipulate objects with valid ids, but the AnnotationEditor frequently 
   * creates transactions in which the Transcripts (and other objects) have no valid id
   * or name.  In addition, the claimed order of ID and NAME udpate transactions does not
   * always match the actual order, making it necessary in some cases to retroactively
   * update the ids used in lookups to the current id of the feature in question.
   *
   * @param updateAllIds If false then only cloned feature ids/names that are 
   * either null or "no_name" will be updated.  If true then all cloned seq feature ids
   * and names will be updated (but not parent feature ids/names.)
   * @param updateAllParentIds Same as <code>updateAllIds</code>, but applies only to
   * cloned parent features.
   */
  public void updateClonedTranscriptIdsAndNames(Transaction t, boolean updateAllIds, boolean updateAllParentIds) {

    // only relevant if cloned features are saved; otherwise any id/name
    // updates propagate automatically
    if (!Config.getSaveClonedFeaturesInTransactions()) {
      return;
    }

    if (t instanceof CompoundTransaction) {
      CompoundTransaction ct = (CompoundTransaction)t;
      // recursively process child transactions
      int nct = ct.size();
      for (int i = 0;i < nct;++i) {
	Transaction child = ct.getTransaction(i);
	updateClonedTranscriptIdsAndNames(child, updateAllIds, updateAllParentIds);
      }
    } 
    else {
      // seq feature
      SeqFeatureI sf = t.getSeqFeature();
      SeqFeatureI sfClone = t.getSeqFeatureClone();
      // id
      String sfId = sf.getId();
      String sfCloneId = sfClone.getId();

      if ((sfId != null) && (!sfId.equals("no_name")) && 
	  (updateAllIds || (sfCloneId == null) || (sfCloneId.equals("no_name")))) 
	{
	  logger.debug("updating cloned seq feature id from " + sfCloneId + " to " + sfId);
	  sfClone.setId(sfId);
	}
      // name
      String sfName = sf.getName();
      String sfCloneName = sfClone.getName();
      if ((sfName != null) && (!sfName.equals("no_name")) && 
	  (updateAllIds || (sfCloneName == null) || (sfCloneName.equals("no_name")))) 
	{
	  sfClone.setName(sfName);
	}

      // parent feature
      SeqFeatureI pf = t.getParentFeature();
      SeqFeatureI pfClone = t.getParentFeatureClone();
      // id
      String pfId = pf.getId();
      String pfCloneId = pfClone.getId();
      if ((pfId != null) && (!pfId.equals("no_name")) && 
	  (updateAllParentIds || (pfCloneId == null) || (pfCloneId.equals("no_name")))) 
	{
	  logger.debug("updating cloned parent seq feature id from " + pfCloneId + " to " + pfId);
	  pfClone.setId(pfId);
	}
      // name
      String pfName = pf.getName();
      String pfCloneName = pfClone.getName();
      if ((pfName != null) && (!pfName.equals("no_name")) && 
	  (updateAllParentIds || (pfCloneName == null) || (pfCloneName.equals("no_name")))) 
	{
	  pfClone.setName(pfName);
	}
    }
  }
}
