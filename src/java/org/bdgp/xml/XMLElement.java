package org.bdgp.xml;

import java.lang.String;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
// import org.w3c.dom.TypeInfo; // new to jdk 1.5 not in 1.4

import org.xml.sax.Attributes;

/** extending IIOMetadataNode is a bit hacky but it allows this class to work in both
 * jdk 1.4 & 1.5. The org.w3c.dom.Element interface is very
 * different in jdk1.4 and 1.5 - such that you cant implement both interfaces (with 
 *stubs) and be able to compile in both 1.4 & 1.5. in particular 1.5 has interfaces
 * in their methods like TypeInfo that are not in 1.4. so in order to avoid having to
 * have an XMLElement for both 1.4 and 1.5 (and thus 2 different apollo downloads - 
 * yuck) we are using javas IOMetadataNode which does exactly that - it has a 1.4
 * and 1.5 implementation that come with the jdks and do implement the different 
 * Element interfaces so we dont have to worry about it (Sohel Merchants inspiration)
 * Oh - the reason XMLElement implements Element is so we can pass off the XMLElement
 * from transactions to the Transaction xml parser that expects an Element. 
 * XMLElement is apollos homegrown DOM. it would be nice to phase this out for 
 * javas org.w3c.dom, but for now we are stuck in both worlds.
 */
/** 1/05/2006: Turns out this is problematic because javax.imageio.metadata.IIOMetadataNode
 *  doesn't exist in jdk1.3, and older versions of Mac OS X (pre-10.2.3) can't run JDK1.4
 *  so they are stuck with 1.3.  So I'm commenting out the "extends" thing just for the
 *  release (1.6.4) so that Apollo will still (theoretically) run on these older Macs.
 *  Then I'm going to put it in.  The next time we do an Apollo release (which will
 *  probably be late 2006), I think we'll just stop supporting JDK1.3. */

public class XMLElement 
  extends javax.imageio.metadata.IIOMetadataNode  // Commented out for r1.6.4
  implements Element { // ?? 
  protected String type;
  protected XMLElement parent = null;
  protected Vector children = new Vector();
  protected String chardata = null;
  protected Hashtable attributes = new Hashtable();
  private NodeList childNodes = new BdgpNodeList();

  public XMLElement (String type)
    {
      this.type = type;
    }

  public String getType ()
    {
      return this.type;
    }

  public void setParent (XMLElement parent)
    {
      if (parent != null)
	{
	  this.parent = parent;
	  parent.addChild (this);
	}
    }

  public XMLElement getParent ()
    {
      return this.parent;
    }

  public void addChild (XMLElement child) {
    children.addElement(child);
  }

  public Vector getChildren ()
    {
      return this.children;
    }

  public void removeChild (XMLElement child)
    {
      this.children.remove(child);
    }

  public void removeChild (int index)
    {
      this.children.remove(index);
    }

  public int numChildren() {
    return children.size();
  }

  public void setCharData (String chardata)
    {
      this.chardata = chardata;
    }

  //public XMLElement getFirstChild() {
  //  return (XMLElement)(this.children.firstElement()); }

  public Node getFirstChild() {
    if (!children.isEmpty())
      return (Node)children.firstElement();
    // w3c considers char data a child (bdgp didnt)
    // this is needed for TransactionXMLAdapter text (eg update value)
    // is it a little hacky to make text an XMLElement?
    XMLElement textChildNode = new XMLElement("#text");
    textChildNode.setCharData(getCharData());
    return textChildNode;
  }

  public XMLElement popChild() {
    XMLElement oldest = (XMLElement)getFirstChild();
    // Will this really free the memory? - i dont think so but im not sure
    removeChild(0);
    return oldest;
  }

  public void appendCharData (String chardata)
    {
      if (this.chardata == null)
        this.chardata = chardata;
      else
        this.chardata = this.chardata + chardata;
    }

  public String getCharData ()
    {
      return this.chardata;
    }

  public void setAttributes (Attributes atts, int count)
    {
      for (int i = 0; i < atts.getLength(); i++)
	{
	  String name = atts.getQName(i);
	  String value = atts.getValue(i);
	  attributes.put (name, value);
	}
      if (attributes.get ("id") == null || attributes.get("id").equals(""))
	{
	  attributes.put ("id", (type + ":" + count));
	}
    }

  public String getAttribute (String name)
    {
      return (String) attributes.get (name);
    }

  //public Hashtable getAttributes() { return attributes; } // not used
  
  public boolean hasAttribute(String name) {
    return getAttribute(name) != null; // ""?
  }

  public String getID ()
    {
      return getAttribute ("id");
    }

  // for org.w3c.dom.Element compliance...
  // from java 1.4
  public String getTagName() { return null; }
  public NodeList getElementsByTagName(String name) { return null; }
  public NodeList getElementsByTagNameNS(String namespaceURI,String localName) {
    return null;
  }
  public void setAttribute(String name,String value) {}
  public void removeAttribute(String name) {}
  public Attr setAttributeNode(Attr newAttr) { return null; }
  public Attr getAttributeNode(String name) { return null; }
  public Attr removeAttributeNode(Attr oldAttr) { return null; }
  public Attr setAttributeNodeNS(Attr newAttr) { return null; }
  public Attr getAttributeNodeNS(String namespaceURI,String localName){return null;}
  public boolean hasAttributeNS(String namespaceURI,String localName) {return false;}
  public void setAttributeNS(String namespaceURI,String qualifiedName,String value){}
  public void removeAttributeNS(String namespaceURI,String localName) {}
  public String getAttributeNS(String namespaceURI,String localName){return null;}

  // org.w3c.dom.Element inherits org.w3c.dom.Node...
  public NodeList getChildNodes() {
    return childNodes;
  }

  /** i believe all XML Elements are Node.ELEMENT_NODE */
  public short getNodeType() {
    return Node.ELEMENT_NODE;
  }

  /** type is the node name */
  public String getNodeName() {
    return getType();
  }
  
  // i think in w3c this is the char data
  public String getNodeValue() { return getCharData(); }

  public Node appendChild(Node newChild) { return null; }
  public Node cloneNode(boolean deep) { return null; }
  public NamedNodeMap getAttributes() { return null; }
  public Node getLastChild() { return null; }
  public String getLocalName() { return null; }
  public String getNamespaceURI() { return null; }
  public Node getNextSibling() { return null; }
  public Document getOwnerDocument() { return null; }
  public Node getParentNode() { return null; }
  public String getPrefix() { return null; }
  public Node getPreviousSibling() { return null; }
  public boolean hasAttributes() { return false; }
  public boolean hasChildNodes() { return false; }
  public Node insertBefore(Node newChild, Node refChild) { return null; }
  public boolean isSupported(String feature, String version) { return false; }
  public void normalize() {}
  public Node removeChild(Node oldChild) { return null; }
  public Node replaceChild(Node newChild,Node oldChild) { return null; }
  public void setNodeValue(String nodeValue) {}
  public void setPrefix(String prefix) {}

  private class BdgpNodeList implements NodeList {
    public int getLength() { return numChildren(); }
    public Node item(int index) { 
      return (Node)children.get(index);
    }
  }


  // from java 1.5 DOM3 Element interface
  // we cant do 1.5 conformance as it has interfaces that are not in 1.4 
  // like TypeInfo - bummer
//    public TypeInfo getSchemaTypeInfo() { return null; }
//   public void setIdAttribute(String name, boolean isId) {}
//    public void setIdAttributeNode(Attr idAttr, boolean isId) {}
//    public void setIdAttributeNS(String namespaceURI,String locNm, boolean isId){}
}
