/*
 * Created on Sep 24, 2004
 *
 */
package apollo.dataadapter.gamexml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import apollo.dataadapter.CurationState;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.ExonI;
import apollo.datamodel.Range;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Transcript;
import apollo.editor.AddTransaction;
import apollo.editor.CompoundTransaction;
import apollo.editor.DeleteTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionSubpart;
import apollo.editor.UpdateTransaction;

/**
 * This class is used to save and load Transaction objects into XML file.
 * @author wgm
 */
public class TransactionXMLAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TransactionXMLAdapter.class);

  // For default extension name
  public static final String EXT_NAME = ".tnxml";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String fileName;
  private DateFormat dateFormat;
  private Writer writer;

  /**
   * Default constructor.
   */
  public TransactionXMLAdapter() {
    dateFormat = DateFormat.getDateTimeInstance();
  }
  
  /** If writing to a game file GAMESave passes in the writer for the file */
  public void setWriter(Writer writer) {
    this.writer = writer;
  }

  public void setFileName(String annotFileName) {
    fileName = parseFileName(annotFileName);
  }

  private Writer getWriter() throws IOException {
    if (writer == null) {
      if (fileName == null) // shouldnt happen
        throw new IOException("Transaction xml adapter has no file/writer set");
      FileWriter fileWriter = new FileWriter(fileName);
      writer = new BufferedWriter(fileWriter);
    }
    return writer;
  }
  
  private String parseFileName(String annotFileName) {
    String tranFileName;
    int index1 = annotFileName.indexOf(File.separator);
    int index2 = -1;
    // this is wrong, this will make file.save.game into file instead of file.save
//     if (index1 > -1)
//       index2 = annotFileName.indexOf(".", index1);
//     else
//       index2 = annotFileName.indexOf(".");
    index2 = annotFileName.lastIndexOf(".");
    if (index2 == -1) {
      // No extension name for annotFileName
      tranFileName = annotFileName + TransactionXMLAdapter.EXT_NAME;
    }
    else {
      tranFileName = annotFileName.substring(0, index2) + TransactionXMLAdapter.EXT_NAME;
    }
    return tranFileName;
  }
  
  /** Loads (new) transactions from a transaction file (transaction file name is derived from
     XML filename supplied to this method). 
     Needs to be static because it's called from static methods.  Is this bad?
     sets curations transactions */
  public static void/*List*/ loadTransactions(String fileName, CurationSet curation, CurationState cs) throws IOException {
    TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
    tnAdapter.setFileName(fileName);
    tnAdapter.load(curation, cs); // no return - sets trans
  }

  public void save(List transactions) throws IOException {
    save(transactions,true); // default write separate file
  }
  /**
   * Save a list of Transaction objects into a file.
   * @param transactions a list of Transactions to be saved
   * @param writingSeparateFile if true writes header & closes Writer when done
   * @throws IOException
   */
  public void save(List transactions, boolean writingSeparateFile) throws IOException {
    // Check if filename is set
//     if (fileName == null)
//       throw new IllegalStateException("TransactionXMLAdapter.save(): No file name is set for transactions.");
    logger.info("Saving " + transactions.size() + " transaction objects");
    StringBuffer buffer = new StringBuffer();
    if (writingSeparateFile)
      buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    String indent = "  ";
    if (!writingSeparateFile)
      buffer.append(indent);
    buffer.append("<apolloTransactions>\n"); // game -> apollo
    saveTransactions(transactions,buffer,indent);
//     Transaction ts = null;
//     for (Iterator it = transactions.iterator(); it.hasNext();) {
//       ts = (Transaction) it.next();
//       if (ts instanceof DeleteTransaction) {
//         saveDeleteTransaction((DeleteTransaction)ts, buffer, indent);
//       }
//       else if (ts instanceof UpdateTransaction) {
//         saveUpdateTransaction((UpdateTransaction)ts, buffer, indent);
//       }
//       else if (ts instanceof AddTransaction) {
//         saveAddTransaction((AddTransaction)ts, buffer, indent);
//       }
//       else if (ts.isCompound()) { // not implemented yet!
//         saveCompoundTransaction((CompoundTransaction)ts, buffer, indent);
//       }
//     }
    if (!writingSeparateFile)
      buffer.append(indent);
    buffer.append("</apolloTransactions>\n"); // game -> apollo
    saveToFile(buffer);
    if (writingSeparateFile)
      getWriter().close();
    logger.info("Saved annotation change transactions to " + fileName);
  }

  private void saveTransactions(List transactions,StringBuffer buffer,String indent) {
    Transaction ts = null;
    for (Iterator it = transactions.iterator(); it.hasNext();) {
      ts = (Transaction) it.next();
      if (ts instanceof DeleteTransaction) {
        saveDeleteTransaction((DeleteTransaction)ts, buffer, indent);
      }
      else if (ts instanceof UpdateTransaction) {
        saveUpdateTransaction((UpdateTransaction)ts, buffer, indent);
      }
      else if (ts instanceof AddTransaction) {
        saveAddTransaction((AddTransaction)ts, buffer, indent);
      }
      else if (ts.isCompound()) { // not implemented yet!
        saveCompoundTransaction((CompoundTransaction)ts, buffer, indent);
      }
    }
    
  }

  /** Flattening out compound transactions. that seems ok for now, and perhaps always?
      the only reason for compounds i believe is for undo, and dont need to undo stuff
      loaded from file - or at least thats an ok restriction. if we want to undo 
      loaded transactions, then we need to preserve compound trans. Actually
      TransactionManager.coalesce() flattens out compound transactions at the moment
      so transXMLAdapter will never actually see compound transactions. */
  private void saveCompoundTransaction(CompoundTransaction tn,
                                        StringBuffer buffer,
                                        String indent) {
    saveTransactions(tn.getTransactions(),buffer,indent);
  }
  
  private void saveExon(ExonI exon, 
                         SeqFeatureI parent, 
                         String indent, 
                         StringBuffer buffer,
                         boolean needEnding) {
    buffer.append(indent);
    buffer.append("<exon start=\"");
    buffer.append(exon.getStart());
    buffer.append("\" end=\"");
    buffer.append(exon.getEnd());
    if (parent != null) {
      buffer.append("\" parentName=\"");
      buffer.append(parent.getName());
      // Save id
      buffer.append("\" id=\"");
      buffer.append(exon.getId());
    }
    buffer.append("\">\n");
    if (needEnding)
      endElement("exon", indent, buffer);
  }
  
  private SeqFeatureI loadExon(Element exonElm, CurationSet curation) {
    String startStr = exonElm.getAttribute("start");
    int start = Integer.parseInt(startStr);
    String endStr = exonElm.getAttribute("end");
    int end = Integer.parseInt(endStr);
    String tranName = exonElm.getAttribute("parentName");
    SeqFeatureI tran = getFeatureByName(curation, tranName);
    if (tran == null)
      return null;
    List features = tran.getFeatures();
    if (features == null || features.size() == 0)
      return null;
    SeqFeatureI feature = null;
    for (Iterator it = features.iterator(); it.hasNext();) {
      feature = (SeqFeatureI) it.next();
      if (feature.getStart() == start &&
          feature.getEnd() == end)
        return feature;
    }
    return null;
  }
  
  private void saveDeleteTransaction(DeleteTransaction tn,
                                      StringBuffer buffer,
                                      String indent) {
    SeqFeatureI feature = tn.getSeqFeature();
    SeqFeatureI parent = tn.getParentFeature();
    startTnElement(tn, "delete", indent, buffer);
    String indent1 = indent + indent;
    if (feature instanceof ExonI)
      saveExon((ExonI) feature, parent, indent1, buffer, true);
    else {
      buffer.append(indent1);
      String type = feature.getFeatureType();
      buffer.append("<");
      buffer.append(type);
      buffer.append(" name=\"");
      buffer.append(feature.getName());
      if (feature instanceof AnnotatedFeature) { // Top-level feature
        // Need id also for genes
        buffer.append("\" id=\"");
        buffer.append(feature.getId());
        
        //Alternate transcripts need theire parent. 
        //If the feature is a transcript, the it's parent (ie gene) childrens are the alternates transcripts
        if (feature instanceof Transcript ){
          //No Exception if the parent is null. On transcript deletion, this can happend after several game save
          //The parent is the curationSet's gene. So the transcript to delete has already been removed, so >=1
          if (parent != null && parent.getNumberOfChildren()>=1){
            buffer.append("\" parentName=\"");
            buffer.append(parent.getName());
          }
        }
      }
      else { // parent should not be null
        if (parent == null)
          throw new IllegalStateException("TransactionXMLAdapter.saveDeleteTransaction(): " +
                                           "in transaction file " + fileName + ",\nno parent is defined for " + feature.getName());
        buffer.append("\" parentName=\"");
        buffer.append(parent.getName());
      }
      buffer.append("\">\n");
      if (tn.getSubpart() != null) {
        // for delete trans preValue holds the deleted subpart, subpartValue is null
        saveSubpart(tn, tn.getOldSubpartValue(),//tn.getSubpartValue(),
                    buffer, indent1 + indent, indent);
      }
      endElement(type, indent1, buffer);
    }
    endElement("delete", indent, buffer);
  }
  
  private void startTnElement(Transaction tn, String op, String indent, StringBuffer buffer) {
    buffer.append(indent);
    buffer.append("<");
    buffer.append(op);
    buffer.append(" author=\"");
    buffer.append(tn.getAuthor());
    buffer.append("\" ts=\"");
    buffer.append(dateFormat.format(tn.getDate()));
    buffer.append("\">\n");
  }
  
  private void startElement(String elmName, String indent, StringBuffer buffer) {
    buffer.append(indent);
    buffer.append("<");
    buffer.append(elmName);
    buffer.append(">\n");
  }
  
  private void endElement(String elmName, String indent, StringBuffer buffer) {
    buffer.append(indent);
    buffer.append("</");
    buffer.append(elmName);
    buffer.append(">\n");
  }
  
  private void saveUpdateTransaction(UpdateTransaction tn,
                                      StringBuffer buffer,
                                      String indent) {
    // Update can work only for subpart
    TransactionSubpart subpart = tn.getSubpart();
    SeqFeatureI feature = tn.getSeqFeature();
    Object preValue = tn.getOldSubpartValue();

    if (preValue == null) {
      String m="Programmer error: prevalue is null for UpdateTransaction on "+
        feature.getName()+ " subpart "+subpart+". Transaction ignored.";
      logger.debug(m);
      return;
    }

    startTnElement(tn, "update", indent, buffer);
    String indent1 = indent + indent;
    String type = feature.getFeatureType();
    if (feature instanceof ExonI) {
      saveExon((ExonI) feature, feature.getParent(), indent1, buffer, false);
    }
    else {
      buffer.append(indent1);
      buffer.append("<");
      buffer.append(type);
      buffer.append(" name=\"");
      buffer.append(feature.getName());
      buffer.append("\">\n");
    }
    String indent2 = indent1 + indent;
    String subpartName = subpart.toString();
    if (subpart == TransactionSubpart.LIMITS ||
        subpart == TransactionSubpart.PEPTIDE_LIMITS) {
      buffer.append(indent2);
      buffer.append("<");
      buffer.append(subpartName);
      buffer.append(" start=\"");
      Range oldRange = (Range) preValue;
      buffer.append(oldRange.getStart());
      buffer.append("\" end=\"");
      buffer.append(oldRange.getEnd());
      buffer.append("\" />\n");
    }
    else if (subpart == TransactionSubpart.PARENT) {
      buffer.append(indent2);
      buffer.append("<");
      buffer.append(subpartName);
      buffer.append(" name=\"");
      SeqFeatureI parentFeature = (SeqFeatureI) preValue;
      if (parentFeature != null)
        buffer.append(parentFeature.getName());
      else 
        buffer.append("null");
      buffer.append("\" />\n");
    }
    else { // Other types from FeatureEditorDialog or ExonDetailDialog
      startElement(subpartName, indent2, buffer);
      // To save rank
      // Rank should be used for comments and synonyms 
      // but not others (no multiple values).
      if (subpart == TransactionSubpart.COMMENT ||
          subpart == TransactionSubpart.SYNONYM) {
        buffer.append(indent2 + indent);
        buffer.append("<rank>");
        buffer.append(tn.getSubpartRank());
        buffer.append("</rank>\n");
      }
      buffer.append(indent2 + indent);
      buffer.append("<value>");
      buffer.append(preValue.toString());
      buffer.append("</value>\n");
      endElement(subpartName, indent2, buffer);
    }
    endElement(type, indent1, buffer);
    endElement("update", indent, buffer);     
  }
  
  private void saveAddTransaction(AddTransaction tn,
                                   StringBuffer buffer,
                                   String indent) {
    SeqFeatureI feature = tn.getSeqFeature();
    startTnElement(tn, "add", indent, buffer);
    String indent1 = indent + indent;
    if (feature instanceof ExonI) {
      // Need to save exon specially. Exon's name is generated on the fly.
      // It might be differnt before and after loading.
      saveExon((ExonI)feature, feature.getParent(), indent1, buffer, true);
    }
    else {
      buffer.append(indent1);
      String type = feature.getFeatureType();
      buffer.append("<");
      buffer.append(type);
      buffer.append(" name=\"");
      buffer.append(feature.getName());
      buffer.append("\">\n");
      if (tn.getSubpart() != null) {
        saveSubpart(tn, tn.getNewSubpartValue(), buffer, indent1 + indent, indent);
      }
      endElement(type, indent1, buffer);
    }
    endElement("add", indent, buffer);  
  }
  
  private void saveSubpart(Transaction tn, 
                           Object subpartValue,
                           StringBuffer buffer, 
                           String indent2, // For the initial indent 
                           String indent) { // For indent step to be added
    String subpartName = tn.getSubpart().toString();
    startElement(subpartName, indent2, buffer);
    if (tn.getSubpart() == TransactionSubpart.COMMENT) {
      // Have to save all information for comments
      saveComment((Comment)subpartValue, indent2 + indent, buffer);
    }
    else if (tn.getSubpart() == TransactionSubpart.DBXREF) {
      // Have to save all informatin for comments
      saveDbxref((DbXref)subpartValue, indent2 + indent, buffer);
    }
    else {
      buffer.append(indent2 + indent);
      buffer.append("<value>");
      //buffer.append(subpartValue);
      buffer.append(subpartValue.toString());
      buffer.append("</value>\n");
    }
    endElement(subpartName, indent2, buffer);
  }
  
  private void saveDbxref(DbXref dbxref,
                          String indent,
                          StringBuffer buffer) {
    // name
    buffer.append(indent);
    buffer.append("<xref_db>");
    buffer.append(dbxref.getDbName());
    buffer.append("</xref_db>\n");
    // id
    buffer.append(indent);
    buffer.append("<db_xref_id>");
    buffer.append(dbxref.getIdValue());
    buffer.append("</db_xref_id>\n");
  }
  
  private DbXref loadDbxref(Element dbxrefElm,
                            AnnotatedFeatureI feature) {
    String id = null;
    String dbName = null;
    NodeList list = dbxrefElm.getChildNodes();
    int size = list.getLength();
    for (int i = 0; i < size; i++) {
      Node tmp = list.item(i);
      if (tmp.getNodeName().equals("xref_db"))
        dbName = tmp.getFirstChild().getNodeValue();
      else if (tmp.getNodeName().equals("db_xref_id"))
        id = tmp.getFirstChild().getNodeValue();
    }
    if (feature.getDbXrefs() != null) {
      for (Iterator it = feature.getDbXrefs().iterator(); it.hasNext();) {
        DbXref tmp = (DbXref) it.next();
        if (tmp.getIdValue().equals(id) &&
            tmp.getDbName().equals(dbName))
          return tmp;
      }
    }
    return null;
  }

  private void saveComment(Comment comment, 
                           String indent,
                           StringBuffer buffer) {
    if (comment == null) {
      logger.warn("Apollo Transaction has null comment.");
      return; // or empty text person & ts??
    }

    // Text
    buffer.append(indent);
    buffer.append("<text>");
    if (comment.getText() != null && !comment.getText().equals(""))
      buffer.append(comment.getText());
    else
      logger.warn("Saving transaction with faulty comment. It has no text.");
    buffer.append("</text>\n");
    // Person
    buffer.append(indent);
    buffer.append("<person>");
    buffer.append(comment.getPerson());
    buffer.append("</person>\n");
    // timestamp
    buffer.append(indent);
    buffer.append("<ts>");
    buffer.append(comment.getTimeStamp());
    buffer.append("</ts>\n");
  }
  
  private Comment loadComment(Element commentElm,
                              boolean needCreate,
                              AnnotatedFeatureI feature) throws SubpartException {
    String text = null;
    String person = null;
    String ts =  null;
    long tsLong = 0;
    NodeList list = commentElm.getChildNodes();
    int size = list.getLength();
    
    if (size == 0)
      throw new SubpartException("Comment xml has no body");

    for (int i = 0; i < size; i++) {
      Node node = list.item(i);
      String nodeName = node.getNodeName();
      try {
        if (nodeName.equals("text")) {
          text =  getFirstChildString(node);
          //if (node.getFirstChild() == null) {
          //System.out.println("Ignoring transaction comment with no text.");
          //throw new SubpartException("Comment text is empty");    //}
          //text = node.getFirstChild().getNodeValue();
        }
        else if (nodeName.equals("person")) {
          person = getFirstChildString(node);
        }
        else if (nodeName.equals("ts")) {
          ts = getFirstChildString(node);
          tsLong = Long.parseLong(ts);
        }
      }
      catch (NoChildException nce) {
        throw new SubpartException("Comment "+nodeName+" is empty");
      }
    }
    if (text == null) // person & ts too?
      throw new SubpartException("Failed to get text for comment");

    Comment comment = null;
    if (needCreate) {
      comment = new Comment(null, text, person, tsLong);
    }
    else {
      // Need to find the one from comments list
      List comments = feature.getComments();
      if (comments != null) {
        for (Iterator it = comments.iterator(); it.hasNext();) {
          Comment tmp = (Comment) it.next();
          // Because of a bug in GAME save/load, ts cannot be used
          // for comparison
          if (//tmp.getTimeStamp() == tsLong && 
              tmp.getPerson().equals(person) &&
              tmp.getText().equals(text)) {
            comment = tmp;
            break;
          }
        }
      }
      if (comment == null)
        throw new SubpartException("Transaction Comment not found in data, person: "+
                                   person+" text: "+text);
    }
    return comment;
  }

  private class NoChildException extends Exception {}

  private String getFirstChildString(Node node) throws NoChildException {
    Node childNode = node.getFirstChild();
    if (childNode == null)
      throw new NoChildException();
    return childNode.getNodeValue();
  }
  
  
  private void saveToFile(StringBuffer buffer) throws IOException {
//    FileWriter fileWriter = new FileWriter(fileName);
//    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    Writer writer = getWriter();
    writer.write(buffer.toString());
    writer.flush();
    //writer.close();
    //fileWriter.close(); doesnt writer.close take care of this?
  }
  
  /**
   * Load Transaction objects from a specified file.
   * @param curation Apollo data model the loading Transactions depenedent on
   * @return
   * @throws IOException
   */
  private void /*List*/ load(CurationSet curation, CurationState cs) throws IOException {
    // Check if fileName is specified
    if (fileName == null)
      throw new IllegalStateException("TransactionXMLAdapter.load(): no transaction file name defined");
    //List list = new ArrayList();
    File file = new File(fileName);
    if (!file.exists()) // No transaction file, probably nothing is modified
      return; // list;
    logger.info("Loading transactions from " + fileName + "...");
    Document doc = null;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();
      doc = builder.parse(file);
    }
    catch(Exception e) {
      logger.error("TransactionXMLAdapter.load(): error in transaction file " + fileName, e);
    }
    // In case nothing is loaded
    if (doc == null)
      return; // list;

    Element root = doc.getDocumentElement();

    //return 
    getTransFromTopElement(root,curation,cs); // sets cur's transactions
  }
  
  /** transElement is the top level "apollo/gameTransactions" xml element that contains
      all of the transactions */
  void getTransFromTopElement(Element root, CurationSet curation, CurationState cs) {
    //List transactionList = new ArrayList();
    // rename TransactionList?
    TransactionManager transactionList = new TransactionManager();
    transactionList.setCurationState(cs);
    if (curation.hasTransactionManager())
      transactionList = curation.getTransactionManager();
    NodeList tranElms = root.getChildNodes();
    int size = tranElms.getLength();
    Node tmp = null;
    Element elm = null;
    Transaction tn = null;
    for (int i = 0; i < size; i++) {
      tmp = tranElms.item(i);
      if (tmp.getNodeType() != Node.ELEMENT_NODE)
        continue;
      elm = (Element) tmp;
      String name = elm.getNodeName();

      try {
        if (name.equals("delete")) {
          tn = loadDeleteTransaction(elm, curation); // throws SubptEx
        }
        else if (name.equals("add")) {
          tn = loadAddTransaction(elm, curation); // throws SubptEx
        }
        else if (name.equals("update")) {
          tn = loadUpdateTransaction(elm, curation);
        }
        else if (name.equals("compound")) {
          // compounds get flattened out on save so no compounds to read in
          // not sure if it will stay this way or not, only need for undo
        }
      }
      // thrown by loadAddTrans & loadDelTrans if problems with subpart
      catch (SubpartException se) {
        logger.error("Dropping "+name+" Transaction due to faulty subpart, "+
                     "(element #"+i+") ", se);
      }
      // IllegalArgumentException causes everything to tank. perhaps this should change 
      // to noting the problems and attempting to move on?
      catch (IllegalArgumentException e) {
        logger.warn("There are problems with transaction file "+fileName+
                    "  Transactions will not be loaded, but don't worry--Apollo should run fine. " +
                    "It doesn't need the transactions; those are mostly useful for updating your database. ", e);
        //transactionList.clear(); // somethings amiss - empty out trans list
        return; // transactionList;
      }

      if (tn != null)
        transactionList.addTransaction(tn);
    }
    //return transactionList;
    curation.setTransactionManager(transactionList);

  }

  private Transaction loadUpdateTransaction(Element elm,
                                             CurationSet curation) {
    UpdateTransaction tn = new UpdateTransaction();
    startLoadTransaction(tn, elm);
    Element featureElm = getFirstElm(elm);
    if (featureElm == null)
      throw new IllegalArgumentException("TransactionXMLAdapter.loadUpdateTransaction(): " +
                                         " in transaction file " + fileName + 
                                         ",\n found invalid transaction element.");
    String typeName = featureElm.getNodeName();
    SeqFeatureI updatedFeature = null;
    if (typeName.equals("exon")) {
      updatedFeature = loadExon(featureElm, curation);
    }
    else {
      // Get the feature name
      String name = featureElm.getAttribute("name");
      if (name != null && name.length() > 0) {
        updatedFeature = getFeatureByName(curation, name);
      }
    }
    if (updatedFeature == null) {
      throw new IllegalArgumentException("TransactionXMLAdapter.loadUpdateTransaction(): " +
                                         " in transaction file " + fileName + 
                                          ",\n cannot find the updated SeqFeaureI.");
    }
    tn.setSeqFeature(updatedFeature);
    Element subpartElm = getFirstElm(featureElm);
    if (subpartElm == null)
      throw new IllegalArgumentException("TransactionXMLAdapter.loadUpdateTransaction(): " +
                                         " in transaction file " + fileName + 
                                          ",\n no subpart specified for a UpdateTransaction.");
    String subpartName = subpartElm.getNodeName();
    TransactionSubpart tsSubpart = TransactionSubpart.stringToSubpart(subpartName);
    tn.setSubpart(tsSubpart);
    if (tsSubpart == TransactionSubpart.LIMITS ||
        tsSubpart == TransactionSubpart.PEPTIDE_LIMITS) {
      Range oldRange = new Range();
      String start = subpartElm.getAttribute("start");
      oldRange.setStart(Integer.parseInt(start));
      String end = subpartElm.getAttribute("end");
      oldRange.setEnd(Integer.parseInt(end));
      tn.setOldSubpartValue(oldRange);
    }
    else if (tsSubpart == TransactionSubpart.PARENT) {
      // Exon moved from one parent to another
      if (updatedFeature instanceof ExonI) {
        String name = subpartElm.getAttribute("name");
        SeqFeatureI oldParent = getFeatureByName(curation, name);
        if (oldParent == null) {
          // This is a special case: exon's parent might be deleted. However, for
          // transaction purpose, only name is needed. So an empty SeqFeatureI can 
          // be used here
          //oldParent = new Transcript();
          //oldParent.setId(name);
          throw new IllegalArgumentException("TransactionXMLAdapter.loadUpdateTransaction(): " +
                                             " in transaction file " + fileName + 
                                             ",\n cannot find old parent for a UpdateTransaction.");
        }
        tn.setOldSubpartValue(oldParent);
      }
    }
    else { // Other types
      // <subpartName>
      //   <!-- for synonym and comment -->
      //   <rank>rank</rank>
      //   <value>oldValue</value>
      // </subpartName>
      NodeList list = subpartElm.getChildNodes();
      int size = list.getLength();
      for (int i = 0; i < size; i++) {
        Node tmp = list.item(i);
        if (tmp.getNodeName().equals("rank")) {
          String text = tmp.getFirstChild().getNodeValue();
          tn.setSubpartRank(Integer.parseInt(text));
        }
        else if (tmp.getNodeName().equals("value")) {
          Element valueElm = (Element) tmp;
          // Get the text value
          String text = valueElm.getFirstChild().getNodeValue();
          if (text.equals("true") || text.equals("false"))
            tn.setOldSubpartValue(Boolean.valueOf(text));
          else
            tn.setOldSubpartValue(text);
        }
      }
    }
    return tn;
  }
  
  /** Creates AddTransaction from elm. Throws SubpartException if it has a subpart,
      and its faulty. Throws IllegalArgumentExceptions if doesnt have any child elements
      or if fails to find associated feature */
  private Transaction loadAddTransaction(Element elm, CurationSet curation) 
    throws SubpartException, IllegalArgumentException {

    AddTransaction tn = new AddTransaction();
    startLoadTransaction(tn, elm);
    Element featureElm = getFirstElm(elm);
    if (featureElm == null)
      throw new IllegalArgumentException("TransactionXMLAdapter.loadAddTransaction(): " +
                                         " in transaction file " + fileName + " " +
                                          ",\n not a valid transaction element.");
    String typeName = featureElm.getNodeName();

    // EXON
    if (typeName.equals("exon")) {
      SeqFeatureI exon = loadExon(featureElm, curation);
      if (exon == null)
        throw new IllegalArgumentException("TransactionXMLAdapter.loadAddTransaction(): " +
                                           " in transaction file " + fileName + 
                                           ",\n an exon cannot be found: " + featureElm.getAttribute("start"));
      tn.setSeqFeature(exon);
    }

    // non-EXON
    else {
      // Get the feature name
      String name = featureElm.getAttribute("name");
      if (name != null && name.length() > 0) {
        SeqFeatureI addedFeat = getFeatureByName(curation, name);
        if (addedFeat == null)
          throw new IllegalArgumentException("TransactionXMLAdapter.loadAddTransaction(): " +
                                             "\n in transaction file " + fileName + ", " +
                                             "cannot find feature " + name + " for AddTrasaction.");
        tn.setSeqFeature(addedFeat);
      }
      else {
        throw new IllegalArgumentException("TransactionXMLAdapter.loadAddTransaction(): " +
                                           " in transaction file " + fileName + ",\n " +
                                           "no name definied for an AddTransaction.");
      }

      // Does nothing if no subpart found. Throws SubpartException if subpart is faulty
      loadSubpart(tn, featureElm);
    }
    return tn;
  }
  
  private class SubpartException extends Exception {
    private SubpartException(String msg) { super(msg); }
  }

  private void loadSubpart(Transaction tn, Element featureElm) throws SubpartException {

    boolean needNewComment = (tn instanceof DeleteTransaction) ? true : false;

    // Check if there is a subpart specified
    Element subpartElm = getFirstElm(featureElm);
    if (subpartElm == null)
      return; // there is no subpart - return 


    String subpartName = subpartElm.getNodeName();
    TransactionSubpart subpart = TransactionSubpart.stringToSubpart(subpartName);
    tn.setSubpart(subpart);
    Object subpartValue = null;
    if (subpart == TransactionSubpart.COMMENT) {
      subpartValue = loadComment(subpartElm, 
                                 needNewComment, // Don't need a new comment
                                 (AnnotatedFeatureI)tn.getSeqFeature());
    }
    else if (subpart == TransactionSubpart.DBXREF) {
      subpartValue = loadDbxref(subpartElm,
                                (AnnotatedFeatureI)tn.getSeqFeature());
    }
    else {
      Element valueElm = getFirstElm(subpartElm);
      subpartValue = valueElm.getFirstChild().getNodeValue();
    }

    if (subpartValue == null) {
      logger.debug("null subpart value for trans.", new Throwable());
      throw new SubpartException("Failed to get subpart value");
    }

    //TODO: Only two cases: Add and Delete related to subpart
    // Probably this is wrong: Have to take a look at update
    // yea what about updating comments??? (update syn -> del,add)
    // update comment comes through - so it must be done elsewhere
    if (tn.isAdd())
      tn.setNewSubpartValue(subpartValue);
    else if (tn.isDelete())
      //((DeleteTransaction)tn).setNewSubpartValue(subpartValue);
      tn.setOldSubpartValue(subpartValue);

  }

  private void startLoadTransaction(Transaction tn, Element tnElm) {
    // Get Author and ts
    String author = tnElm.getAttribute("author");
    if (author != null && author.length() > 0)
      tn.setAuthor(author);
    String ts = tnElm.getAttribute("ts");
    if (ts != null && ts.length() > 0) {
      try {
        Date date = dateFormat.parse(ts);
        tn.setDate(date);
      }
      catch(ParseException e) {
        logger.error("TransactionXMLAdatper.startLoadTransaction(): " +
                     " in transaction file " + fileName + ", parsing error", e);
      }
    }
  }
  
  private Element getFirstElm(Element parentElm) {
    NodeList children = parentElm.getChildNodes();
    int size = children.getLength();
    Element childElm = null;
    for (int i = 0; i < size; i++) {
      Node tmp = children.item(i);
      if (tmp.getNodeType() == Node.ELEMENT_NODE) {
        childElm = (Element) tmp;
        break;
      }
    }
    return childElm;
  }
  
  private SeqFeatureI createFeature(Element featureElm) {
    // Get the name for the deleted feature
    String type = featureElm.getNodeName();
    String name = featureElm.getAttribute("name");
    String id = featureElm.getAttribute("id");
    // Have to initialize a new SeqFeaureI, since it has already been deleted
    SeqFeatureI feature = null;
    String typeName = featureElm.getNodeName();
    if (typeName.equals("exon")) {
      feature = new Exon();
      /*String start = featureElm.getAttribute("start");
      feature.setStart(Integer.parseInt(start));
      String end = featureElm.getAttribute("end");
      feature.setEnd(Integer.parseInt(end));*/
      
      //let's take the strand into account before setting start and stop (mandatory 
      //to correctly set high and low through setStart and setEnd)
      int start = Integer.parseInt(featureElm.getAttribute("start"));
      int end =  Integer.parseInt(featureElm.getAttribute("end"));
      if (start>end)
        feature.setStrand(-1);
      else
        feature.setStrand(1);
      feature.setStart(start);
      feature.setEnd(end);
    }
    else if (typeName.equals("transcript"))
      feature = new Transcript();
    else {
      feature = new AnnotatedFeature();
      feature.setFeatureType(type);
    }
    if (name != null && name.length() > 0)
      feature.setName(name);
    // For gene, id is also needed, that should be used as 
    // uniquename in the chado db.
    if (id != null && id.length() > 0)
      feature.setId(id);
    return feature;
  }
  
  private SeqFeatureI getFeatureByName(CurationSet curation, String name) {
    // Need to get parent SeqFeatureI from curation
    List features = curation.getAnnots().findFeaturesByName(name);
    // Get the first one
    if (features == null || features.size() == 0)
      return null;
    SeqFeatureI feature = (SeqFeatureI) features.get(0);
    return feature;
  }
  
  /** Extracts delete transaction from element. Throws subpart exception if there
      is a subpart and its faulty. */
  private Transaction loadDeleteTransaction(Element elm,CurationSet curation) 
    throws SubpartException {

    DeleteTransaction tn = new DeleteTransaction();
    startLoadTransaction(tn, elm);
    Element featureElm = getFirstElm(elm);
    if (featureElm == null)
      throw new IllegalArgumentException("TransactionXMLAdapter.loadDeleteTransaction(): " +
                                         " in transaction file " + fileName + ",\n " +
                                          "invalid delete transaction element.");

    // Have to initialize a new SeqFeaureI, since it has already been deleted
    SeqFeatureI deletedFeature = createFeature(featureElm);
    tn.setSeqFeature(deletedFeature);
    // Get the parentName
    String parentName = featureElm.getAttribute("parentName");
    if (parentName != null && parentName.length() > 0) {
      SeqFeatureI parentFeature = getFeatureByName(curation, parentName);
      //Handle error when deleting a transcript
      //See saveDeleteTransaction for the reason of this test (alternate transcripts)
      if (parentFeature == null && deletedFeature instanceof Transcript)
        throw new IllegalArgumentException("TransactionXMLAdapter.loadDeleteTransaction: " +
            " in transaction file " + fileName + ",\n " +
            "The gene of transcript" + deletedFeature.getName() + "can not be found, potential bug in game transaction save.");

      if (parentFeature == null)
        throw new IllegalArgumentException("TransactionXMLAdapter.loadDeleteTransaction: " +
                                           " in transaction file " + fileName + ",\n " +
                                           "parent feature cannot be found for deleted feature" + deletedFeature.getName());
      tn.setParentFeature(parentFeature);
      // Need the parent info for deleted feature for coalescing and other
      // purposes (e.g. naming)
      deletedFeature.setRefFeature(parentFeature);
    }
    // Need to check if there is any subpart appear. Does nothing if no subpart.
    // throws subpart exception if subpart is faulty
    loadSubpart(tn, featureElm); 
    return tn;
  }
  
}
