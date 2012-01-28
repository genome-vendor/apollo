/*
 * Created on Sep 17, 2004
 *
 */
package apollo.dataadapter.chadoxml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import apollo.config.Config;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.TransactionTransformer;
import apollo.dataadapter.chado.ChadoTransaction;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.util.IOUtil;

/**
 * This class is used to write Transaction objects to Chado transaction XML.
 * @author wgm
 */
public class ChadoTransactionXMLWriter extends TransactionOutputAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoTransactionXMLWriter.class);

  private static final String TN_EXT_NAME = ".ctn";
  
  /**
   * Default constructor
   *
   */
  public ChadoTransactionXMLWriter() {
  }
  
  /**
   * Override the super class method to check the type of the target
   * @param target the file target. It must be a file name. This is the file name for game xml.
   * So it should be converted to chado transaction xml file extension.
   */
  public void setTarget(Object target) {
    if (target instanceof String) {
      super.setTarget(parseAnnotFileName(target.toString()));
    }
    else
      throw new IllegalArgumentException("ChadoTransactionXMLWriter.setTarget(): target should be a file name.");
  }
  
  private String parseAnnotFileName(String annotFileName) {
    String tranFileName;
    int index1 = annotFileName.indexOf(File.separator);
    int index2 = -1;
//     if (index1 > -1)
//       index2 = annotFileName.indexOf(".", index1);
//     else
//       index2 = annotFileName.indexOf(".");
    index2 = annotFileName.lastIndexOf(".");
    if (index2 == -1) {
      // No extension name for annotFileName
      tranFileName = annotFileName + TN_EXT_NAME;
    }
    else {
      tranFileName = annotFileName.substring(0, index2) + TN_EXT_NAME;
    }
    return tranFileName;
  }
  
  protected void commitTransformedTransactions(List chadoTransactions) throws Exception {
    if (chadoTransactions == null)
      chadoTransactions = new ArrayList(); // To make an empty file
    logger.info("Saving " + chadoTransactions.size() + " transaction objects");
    StringBuffer buffer = new StringBuffer();
    String name = "conf" + File.separator + Config.getChadoTemplateName();
    String tmpFileName = IOUtil.findFile(name);
    if (tmpFileName == null)
      throw new IllegalStateException("ChadoTransactionXML.commitTransactions(): " +
                                       "Cannot find xml template for chado transaction."); 
    ChadoTransactionXMLTemplate template = new ChadoTransactionXMLTemplate(tmpFileName);
    template.setStartIndent("    ");
    template.setIndent("    ");
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    buffer.append(template.getRootStartTag());
    if (chadoTransactions.size() > 0) {
      buffer.append(template.getPreambleString());
      // Need to search up for srcfeature_id
      ChadoTransaction ts = ((ChadoTransactionTransformer)transformer).createSrcFeatureIDTransaction(mapID, mapType);
      buffer.append(template.generateElement(ts));
      for (Iterator it = chadoTransactions.iterator(); it.hasNext();) {
        ts = (ChadoTransaction) it.next();
        buffer.append(template.generateElement(ts));
      }
    }
    buffer.append(template.getRootEndTag());
    saveToFile(buffer, (String)target);
    logger.info("Saved Chado transactions to " + target);
  }
  
  private void saveToFile(StringBuffer buffer, String fileName) throws IOException {
    FileWriter fileWriter = new FileWriter(fileName);
    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    bufferedWriter.write(buffer.toString());
    bufferedWriter.close();
    fileWriter.close();
  }
  
  /**
   * Need to add some information from config file.
   * 
   */
  public void setTransformer(TransactionTransformer transformer) {
    super.setTransformer(transformer);
    if (transformer instanceof ChadoTransactionTransformer) {
      String configFileName = Config.getChadoJdbcAdapterConfigFile();
      if (configFileName == null) {
        logger.error("can't find chado cfg file -> can't transform transactions "
                     +"to chado");
        return;
      }
//       String configFileName = apollo.util.IOUtil.findFile("conf"
//           + File.separator + "chado-adapter.xml");
//       if (configFileName == null) {
//         configFileName = apollo.util.IOUtil.findFile("chado-adapter.xml");
//       }
//       if (configFileName == null) {
//         logger.error(this.getClass()
//             + ": warning - unable to find chado-adapter.cfg");
//       }
      else {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
          DocumentBuilder builder = dbf.newDocumentBuilder();
          Document doc = null;
          doc = builder.parse(configFileName);
          Element root = doc.getDocumentElement();
          NodeList list = root.getChildNodes();
          int size = list.getLength();
          for (int i = 0; i < size; i++) {
            Node node = list.item(i);
            if (node.getNodeName().equals("chadoInstance")) {

              // this is funny - it gets the "default" instance from chado-adapter.xml
              // file this is for 1-level & 3-level annots (i think that might be the sole
              // reason). but this will only work for 1 species/database/instance
              // alternate solutions: 1) instead of default put species in the instance
              // 2) put levels in style - then ya hafta fish up style for species - but
              // thats already done anyways (at least for reading game)
              // 3) put levels in tiers - might be misleading since ap datamodel still does
              // three levels for everything - but thats where the types are
              // 4) query levels from SO file (or SO in db)
              String isDefault = ((Element)node).getAttribute("default");
              if (isDefault != null && isDefault.equals("true")) {
                initFeaturesForLevels(node, (ChadoTransactionTransformer)transformer);
                break;
              }
            }
          }
        }
        catch (Exception e) {
          logger.error("ChadoTransactionWriter.setTransformer(): " + e, e);
        }
      }
    }
  }
  
  private void initFeaturesForLevels(Node node, ChadoTransactionTransformer transformer) {
    NodeList children = node.getChildNodes();
    int size = children.getLength();
    for (int i = 0; i < size; i++) {
      Node tmp = children.item(i);
      String nodeName = tmp.getNodeName();
      if (nodeName.equals("oneLevelAnnotTypes")) {
        transformer.setOneLevelAnnotTypes(extractTypes(tmp));
      }
      else if (nodeName.equals("threeLevelAnnotTypes")) {
        transformer.setThreeLevelAnnotTypes(extractTypes(tmp));
      }
      else if (nodeName.equals("polypeptideType")) {
        transformer.setPolypeptideType(tmp.getFirstChild().getNodeValue());
      }
    }
  }
  
  private List extractTypes(Node typeNode) {
    NodeList children = typeNode.getChildNodes();
    int size = children.getLength();
    List rtn = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      Node tmp = children.item(i);
      if (tmp.getNodeName().equals("type")) {
        rtn.add(tmp.getFirstChild().getNodeValue());
      }
    }
    return rtn;
  }
  
}
