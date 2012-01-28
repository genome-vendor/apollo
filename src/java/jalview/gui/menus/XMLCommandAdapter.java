package jalview.gui.menus;

import java.awt.event.*;
import java.util.*;

import java.net.*;
import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.bdgp.xml.*;

public class XMLCommandAdapter extends CommandAdapter implements DocumentHandler {

  JalCommandVector commands = new JalCommandVector();

  boolean USE_FLUSH = true;

  org.xml.sax.Parser xml_parser = null;

  XMLElement current_element = null;
  XMLElement top_element = null;

  int element_count = 0;

  // need a Stack to keep track of current object's parent chain
  Stack element_chain;

  String default_parser_name = "com.microstar.xml.SAXDriver";

  String filename;

  public XMLCommandAdapter(String fileName) {
    setFilename(fileName);
  }

  public void add(String comStr, String args) {
    commands.addElement(new JalCommand(comStr,args));
  }

  public void add(String comStr, Hashtable args) {
    commands.addElement(new JalCommand(comStr,args));
  }

  public JalCommand get(int i) {
    return null; //(JalCommand)commands.elementAt(i); // MG
  }

  public int size() {
    return commands.size();
  }

// XML Stuff
  public void init() {}

  public String getName() {
    return "Command Adapter";
  }

  public String getType() {
    return "Command XML File";
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public Properties getStateInformation() {
    Properties props = new Properties();
    props.put("filename", filename);
    return props;
  }

  public void setStateInformation(Properties props) {
    filename = props.getProperty("filename");
    System.err.println("************ filename="+filename);
  }

  /*
    Writing XML
  */

  public void writeLog(CommandLog log) {
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(filename);
    } catch ( Exception ex ) {
      System.err.println("caught exception opening " + filename);
      System.out.println(ex.getMessage());
      ex.printStackTrace();
    }
    if (fos != null) {
      writeXML(fos,log);
      System.out.println("Saved XML to " + filename);
    }
    try {
      fos.close();
    } catch ( Exception ex ) {
      System.err.println("caught exception closing " + filename);
      System.out.println(ex.getMessage());
      ex.printStackTrace();
    }
  }

  public void writeXML(OutputStream os,CommandLog log) {
    // might want to add buffering via BufferedOutputStream in here
    DataOutputStream dos;

    if (os instanceof DataOutputStream) {
      dos = (DataOutputStream)os;
    } else {
      dos = new DataOutputStream(os);
    }

    try {
     dos.writeBytes(writeCommandBegin());
//MG     dos.writeBytes(writeCommands(log));
     dos.writeBytes(writeCommandEnd());
    } catch ( Exception ex ) {
      System.err.println("caught exception committing XML");
      System.out.println(ex.getMessage());
      ex.printStackTrace();
    }
  }

  private String writeCommandBegin() {
    StringBuffer buf = new StringBuffer();
    buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
    buf.append("\n");
    buf.append("<!-- DOCTYPE jalviewcom SYSTEM \"JALVIEW.dtd\" -->\n");
    buf.append("\n");
    buf.append("<jalviewcom>\n");
    buf.append("  <!-- commands from jalview -->\n");
    buf.append("  <!-- Saved on " +
               (new Date()).toString() + " -->\n");
    return buf.toString();
  }

  private String writeCommands() {
    return ("");
  }

  private String writeCommandEnd() {
    return ("</jalviewcom>\n");
  }

  public JalCommandVector getCommands() {
    try {
      System.out.println("OPENING LOG FILE " + filename);
      InputStream xml_stream = new FileInputStream (filename);
      BufferedInputStream bis = new BufferedInputStream(xml_stream);
      readXML(bis);
      getCommandsFromXML();
      getFormats();
      getColourSchemes();
      /*
      	    getSequences (curation);
      	    curation.setResults (getAnalyses (curation));
      	    curation.setFeatureTypes(apollo.config.Config.getFeatureTypes());
      	    curation.setFeatureGroupTypes(apollo.config.Config.getFeatureGroupTypes());
      	    curation.setAnnots (getAnnotations (curation));
      */
    } catch ( Exception ex2 ) {
      System.err.println("caught exception in readXML (" + filename +
                         "): ");
      System.out.println(ex2.getMessage());
      ex2.printStackTrace();
    }
return null; //MG
  }

  private void readXML(InputStream istream) {
    element_chain = new Stack();

    try {
      if (xml_parser == null) {
        xml_parser =
          (org.xml.sax.Parser)Class.forName(default_parser_name).newInstance();
      }
    } catch (Exception e) {
      System.err.println("Fatal Error in xml_parser new: " +
                         e.getMessage() + e);
    }

    try {
      xml_parser.setDocumentHandler(this);
    } catch (Exception e) {
      System.err.println("Fatal Error in xml_parser.setDocumentHandler: "
                         + e.getMessage() + e + " xml_parser = " + xml_parser );
    }
    try {
      xml_parser.parse(new InputSource(istream));
    } catch (Exception e) {
      System.err.println("Fatal Error near element # " +
                         element_count + " : " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void setParser(org.xml.sax.Parser xml_parser) {
    this.xml_parser = xml_parser;
  }

  public void startElement (String name, AttributeList atts) {
    try {
      XMLElement parent_element = current_element;

      current_element = new XMLElement(name);

      if (parent_element == null) {
        /* this is the root element */
        top_element = current_element;
      } else {
        element_chain.push(parent_element);
        current_element.setParent(parent_element);
      }

      element_count++;

//MG      current_element.setAttributes(atts, element_count);
    } catch (Exception e) {
      System.err.println("Unable to start element " + name +
                         e.getMessage());
    }
  }

  public void endElement (String name) {
    if ( ! element_chain.empty() ) {
      current_element = (XMLElement) element_chain.pop();
    }
  }

  public void characters (char ch[], int start, int length) {
    String char_data;

    if ((current_element.getType().equalsIgnoreCase ("residues"))) {
      char_data = XML_util.filterWhiteSpace(ch, start, length);
      char_data = char_data.toUpperCase();
    } else {
      char_data = XML_util.trimWhiteSpace(ch, start, length);
    }

    if (char_data != null && ! char_data.equals ("")) {
      current_element.setCharData (char_data);
    }
  }

  //  public void ignorable (char ch[], int start, int length)  {
  public void ignorableWhitespace(char ch[], int start, int length)  {}

  public void startDocument()  {}

  public void endDocument()  {}

  public void doctype(String name, String publicID, String systemID) {}

  public void processingInstruction (String name, String remainder)  {}

  public void setDocumentLocator (Locator locator)  {
    // no opping for now
  }


  /*
    Utility routines.
  */
  public void getCommandsFromXML() {
    Vector elements = top_element.getChildren();

    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("command")) {
        addCommand(element);
      }
    }
    return;
  }

  public void addCommand(XMLElement commandElement) {
    if (commandElement.getAttribute("args") != null) {
      add(commandElement.getAttribute("name"),
              commandElement.getAttribute("args"));
    } else {
      Hashtable dummy = new Hashtable();
      //dummy.put("state","true");
      add(commandElement.getAttribute("name"),dummy);
    }
  }

  public void getFormats() {
    Vector elements = top_element.getChildren();

    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("format")) {
        addCommand(element);
      }
    }
    return;
  }
  public void getColourSchemes() {
    Vector elements = top_element.getChildren();

    for (int i = 0; i < elements.size(); i++) {
      XMLElement element = (XMLElement) elements.elementAt(i);
      if (element.getType().equals ("colourscheme")) {
        addCommand(element);
      }
    }
    return;
  }
}
