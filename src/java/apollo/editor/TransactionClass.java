package apollo.editor;

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

/** 
 * Value class for transaction object classes 
 * Presently used for Transaction. Should eventually be used 
 * by FeatureChangeEvents as well. 
 */
public class TransactionClass implements Serializable {

  private static Map stringToClass = new HashMap(7);

  public static final TransactionClass EXON = new TransactionClass("EXON");
  public static final TransactionClass COMMENT = new TransactionClass("COMMENT");
  public static final TransactionClass EVIDENCE = new TransactionClass("EVIDENCE");
  // I dont think translation belongs here - fix this!
  public static final TransactionClass TRANSLATION = new TransactionClass("TRANSLATION");
  public static final TransactionClass TRANSCRIPT = new TransactionClass("TRANSCRIPT");
  public static final TransactionClass ANNOTATION = new TransactionClass("ANNOTATION");
  /** This probably belongs in a new value class ObjectPart or something like 
      that - for now shoving here - to say type has changed */
  public static final TransactionClass TYPE = new TransactionClass("TYPE");

  private String classString;

  private TransactionClass(String classString) {
    this.classString = classString;
    stringToClass.put(classString,this);
  }
  public String toString() { return classString; }

  public static TransactionClass stringToClass(String classString) {
    return (TransactionClass)stringToClass.get(classString);
  }
}
