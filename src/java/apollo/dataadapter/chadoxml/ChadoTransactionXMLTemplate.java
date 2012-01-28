/*
 * Created on Sep 28, 2004
 *
 */
package apollo.dataadapter.chadoxml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import apollo.dataadapter.chado.ChadoTransaction;
import apollo.dataadapter.chado.ChadoUpdateTransaction;

/**
 * This class is used to process xml template for transactions.
 * @author wgm
 */
public class ChadoTransactionXMLTemplate {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoTransactionXMLTemplate.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  private Document doc;
  // Default parameter values
  private Map defaultValues = new HashMap();
  // To control export format
  private String startIndent = "";
  private String indent = "    ";
  
  public ChadoTransactionXMLTemplate(String fileName) {
    loadTemplates(fileName);
  }
  
  public ChadoTransactionXMLTemplate(String fileName, String startIndent, String indent) {
    this(fileName);
    this.startIndent = startIndent;
    this.indent = indent;
  }
  
  public void setStartIndent(String indent) {
    this.startIndent = indent;
  }
  
  public void setIndent(String indent) {
    this.indent = indent;
  }
  
  private void loadTemplates(String fileName) {
    try {
      DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = df.newDocumentBuilder();
      doc = builder.parse(fileName);
    }
    catch(Exception e) {
      logger.error("XMLTemplate.loadTemplates(): " + e, e);
    }
  }
  
  public String generateElement(ChadoTransaction tn) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(startIndent);
    buffer.append("<");
    buffer.append(tn.getTableName());
    buffer.append(" op=\"");
    buffer.append(tn.getOperation());
    buffer.append("\"");
    if (tn.getID() != null) {
      buffer.append(" id=\"");
      buffer.append(tn.getID());
      buffer.append("\"");
    }
    buffer.append(">\n");
    outputProperties(buffer, startIndent + indent, tn.getProperties());
    if (tn instanceof ChadoUpdateTransaction) {
      outputUpdateProperties(buffer, 
                             startIndent + indent, 
                             ((ChadoUpdateTransaction)tn).getUpdateProperies());
    }
    closeElementTag(buffer, indent, tn);
    return buffer.toString();
  }
  
  private void outputProperties(StringBuffer buffer, String indent, Map properties) {
    String key;
    String value;
    for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
      key = (String) it.next();
      value = (String) properties.get(key);
      buffer.append(indent);
      buffer.append("<");
      buffer.append(key);
      buffer.append(">");
      buffer.append(value);
      buffer.append("</");
      buffer.append(key);
      buffer.append(">\n");
    }
  }
  
  private void outputUpdateProperties(StringBuffer buffer, String indent, Map updateProps) {
    if (updateProps == null || updateProps.size() == 0)
      return;
    String key, value;
    for (Iterator it = updateProps.keySet().iterator(); it.hasNext();) {
      key = (String) it.next();
      value = (String) updateProps.get(key);
      buffer.append(indent);
      buffer.append("<");
      buffer.append(key);
      buffer.append(" op=\"update\">");
      buffer.append(value);
      buffer.append("</");
      buffer.append(key);
      buffer.append(">\n");
    }
  }
  
  private void closeElementTag(StringBuffer buffer, String indent, ChadoTransaction ts) {
    buffer.append(startIndent);
    buffer.append("</");
    buffer.append(ts.getTableName());
    buffer.append(">\n");
  }
  
  public Element getElement(String elmName) {
    // Find the root element
    Element root = doc.getDocumentElement();
    NodeList children = root.getChildNodes();
    int size = children.getLength();
    for (int i = 0; i < size; i++) {
      Node node = children.item(i);
      if (node.getNodeName().equals(elmName))
        return (Element)node;
    }
    return null;
  }
  
  public String getRootStartTag() {
    Element rootElm = getTxRootElement();
    StringBuffer buffer = new StringBuffer();
    buffer.append("<");
    buffer.append(rootElm.getNodeName());
    NamedNodeMap atts = rootElm.getAttributes();
    if (atts != null) {
      int size = atts.getLength();
      for (int i = 0; i < size; i++) {
        Node node = atts.item(i);
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
          Attr att = (Attr) node;
          buffer.append(" ");
          buffer.append(att.getName());
          buffer.append("=\"");
          buffer.append(att.getName());
          buffer.append("\"");
        }
      }
    }
    buffer.append(">\n");
    return buffer.toString();
  }
  
  public String getRootEndTag() {
    Element txRootElm = getTxRootElement();
    return "</" + txRootElm.getNodeName() + ">\n";
  }
  
  private Element getTxRootElement() {
    Element rootElm = getElement("root");
    NodeList children = rootElm.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        return (Element)node;
      }
    }
    return null;
  }
  
  public String getPreambleString() {
    Element preamleElm = getElement("preamble");
    if (preamleElm != null) {
      StringBuffer buffer = new StringBuffer();
      // No need for <preamble> element
      NodeList children = preamleElm.getChildNodes();
      int size = children.getLength();
      for (int i = 0; i < size; i++) {
        Node node = children.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE)
          convertElementToString((Element)node, buffer, startIndent);
      }
      return buffer.toString();
    }
    return null;
  }
  
  private void convertElementToString(Element element, StringBuffer buffer, String indent1) {
    buffer.append(indent1);
    buffer.append("<");
    buffer.append(element.getNodeName());
    // Convert attributes
    NamedNodeMap attributes = element.getAttributes();
    if (attributes != null) {
      int size = attributes.getLength();
      Node node = null;
      for (int i = 0; i < size; i++) {
        node = attributes.item(i);
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
          Attr att = (Attr) node;
          buffer.append(" ");
          buffer.append(att.getName());
          buffer.append("=\"");
          buffer.append(att.getValue());
          buffer.append("\"");
        }
      }
    }
    buffer.append(">");
    // Convert elements
    NodeList children = element.getChildNodes();
    int size = children.getLength();
    // A special case for text node
    if (size == 1 && children.item(0).getNodeType() == Node.TEXT_NODE) {
      Node textNode = children.item(0);
      buffer.append(textNode.getNodeValue());
    }
    else {
      // Close the start tag
      buffer.append("\n");
      for (int i = 0; i < size; i++) {
        Node node = children.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          convertElementToString((Element) node, buffer, indent1 + indent);
        } 
      }
      buffer.append(indent1);
    }
    buffer.append("</");
    buffer.append(element.getNodeName());
    buffer.append(">\n");
  }
  
}
