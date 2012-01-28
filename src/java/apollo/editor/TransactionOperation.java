package apollo.editor;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/** Value class for transaction operations - not sure how serializing will work
 i think when it deserializes it will have the string but not the final static
 which could mess up things(like undo). */
public class TransactionOperation implements Serializable {

  private static Map stringToOperation = new HashMap(11);

  public static final TransactionOperation ADD  = new TransactionOperation("ADD");
  public static final TransactionOperation DELETE = new TransactionOperation("DELETE");
  // subpart
  public static final TransactionOperation NAME = new TransactionOperation("NAME");
  // --> subpart
  public static final TransactionOperation ID = new TransactionOperation("ID");
  // split??
  public static final TransactionOperation SPLIT = new TransactionOperation("SPLIT");
  public static final TransactionOperation MERGE = new TransactionOperation("MERGE");
  /* Replace old feat with new feat - equivalent to DELETE then ADD. I think most
      replaces are really updates. Replace is an annot change event but not a 
      transaction! */
  //public static final TransactionOperation REPLACE = new TransactionOperation("REPLACE");
  /** Update part of feature */
  public static final TransactionOperation UPDATE = new TransactionOperation("UPDATE");
  // move this to subpart
  public static final TransactionOperation LIMITS = new TransactionOperation("LIMITS");
  // public static final TransOp TRANSLATION_CHANGE = new TransOp("TRANSLATIONCNG");
//public static TransactionOperation REDRAW=new TransactionOperation("REDRAW");
  //public static final TransactionOperation MODEL_CHANGED = 
  //new TransactionOperation("MODEL_CHANGED"); // ?????
//public static final TransactionOperation STATUS=new TransactionOperation("STATUS");
//public static final TransactionOperation SYNC = new TransactionOperation("SYNC");


  private String operationString;
  
  private TransactionOperation(String opString) {
    this.operationString = opString;
    stringToOperation.put(operationString,this);
  }

  public String toString() { return operationString; }

  //public int to_int() { return intConstant; }

  public static TransactionOperation stringToOperation(String opString) {
    return (TransactionOperation)stringToOperation.get(opString);
  }

}
