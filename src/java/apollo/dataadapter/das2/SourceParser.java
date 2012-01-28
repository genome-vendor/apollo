package apollo.dataadapter.das2;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
//import java.io.*;
import java.util.*;

import apollo.dataadapter.das.*;
import apollo.dataadapter.das.simple.*;

/**
 * <p>I am the ContentHandler used when <code>DAS2Request</code> parses
 * the result of a 'sequence' call to a DAS/2 data source.</p>
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * sources. This list is retrieved by calling <code>getVersionedSources</code> after the parse
 * is complete </p>
 *
 * @see apollo.dataadapter.das2.VersionedSources
 * @author Vivek Iyer and Nomi Harris
 *
 * Example of what this parses (2/6/2006):
 * <?xml version="1.0" standalone="no"?>
 * <?xml-stylesheet type="text/xsl" href="/xsl/das.xsl"?>
 * <!DOCTYPE DAS2DSN SYSTEM "http://www.biodas.org/dtd/das2dsn.dtd">
 * <!-- this doesn't work and screws up the xsl     xmlns="http://www.biodas.org/ns/das/genome/2.00" -->
 * <SOURCES
 *       xmlns:xlink="http://www.w3.org/1999/xlink"
 *       xmlns="http://www.biodas.org/ns/das/genome/2.00"
 *       xml:base="http://das.biopackages.net/das/genome/">
 * 
 *   <SOURCE id="yeast" title="yeast genome" writeable="no" doc_href="" taxid="Yeast">
 *     <VERSION id="yeast/S228C" title="Sce" created="" modified="">
 * 
 *       <COORDINATES taxid="" source="" authority="">
 *         <VERSION name=""/>
 *       </COORDINATES>
 * 
 *       <CAPABILITY type="features" query_id="yeast/S228C/feature">
 *         <!-- list non-das2xml templates here -->
 *       </CAPABILITY>
 *       <CAPABILITY type="segments" query_id="yeast/S228C/segment"/>
 *       <CAPABILITY type="types"    query_id="yeast/S228C/type"/>
 *       <CAPABILITY type="locks"    query_id="yeast/S228C/lock"/>
 * 
 *       <PROP key="" value=""/>
 * [etc]
**/
public class SourceParser
  extends DefaultHandler {
  private String baseURL;
  private String mode;
  private Stack modeStack = new Stack();
  private List dsns = new ArrayList();
    private String segment_uri;

  //tag labels
  private String SOURCE="SOURCE";
  private String SOURCES="SOURCES";
  private String VERSION="VERSION";
  private String CAPABILITY="CAPABILITY";

  //attribute labels
  private String SOURCE_ID="id";
  //  private String SOURCE_VERSION="version";
  private String BASE_URL="xml:base";

  private SimpleDASDsn currentDsn;

  public SourceParser() {}

  private SimpleDASDsn getCurrentDsn() {
    return currentDsn;
  }

  private void setCurrentDsn(SimpleDASDsn theDsn) {
    currentDsn = theDsn;
  }

  private void addDsn(SimpleDASDsn theDsn) {
    getDsns().add(theDsn);
  }

  public List getDsns() {
    return dsns;
  }

  private Stack getModeStack() {
    return modeStack;
  }

  private void setMode(String theMode) {
    getModeStack().push(theMode);
  }

  private String getMode() {
    if(!getModeStack().isEmpty()) {
      return (String)getModeStack().peek();
    } else {
      return null;
    }
  }

  private void closeMode() {
    if(!getModeStack().isEmpty()) {
      getModeStack().pop();
    }
  }

  public void setURL(String url) {
      //    System.out.println("SourceParser: set URL to " + url);  // DEL
    this.baseURL = url;
  }
  public String getURL() {
    return baseURL;
  }

  public void startElement(
    String namespaceURI,  // not used
    String tag,
    String qualifiedName,  // not used
    Attributes attributes
  ) throws SAXException {
    setMode(tag);
    if (tag.equals(SOURCES)) {
      String url = attributes.getValue(BASE_URL);
      if (url != null)
        setURL(url);
    }
    else if (tag.equals(SOURCE)) {
      //      System.out.println("startElement: got SOURCE"); // DEL
    } else if(tag.equals(VERSION)) {
      //      getCurrentDsn().setSourceId(attributes.getValue(SOURCE_ID));
      String version = attributes.getValue(SOURCE_ID);
      if (version != null) {
	  //	  System.out.println("SourceParser.startElement: got version id = " + version); // DEL

	  // Save the previous DSN before starting a new one
	if (getCurrentDsn() != null) {
	    System.out.println("SourceParser.startElement: adding current dsn " + getCurrentDsn()); // DEL
	    addDsn(getCurrentDsn());
	    setCurrentDsn(null);
	}

	// Use the URL (e.g. http://das.biopackages.net/das/genome) as the "mapMaster" of this DSN
        SimpleDASDsn source = new SimpleDASDsn(baseURL + version, version, "", getURL(), "");
	//        addDsn(source);  // not yet
	setCurrentDsn(source);
      }
      // CAPABILITY used to be CATEGORY
    } else if (tag.equals("CAPABILITY") || tag.equals("CATEGORY")) {
	//      <CAPABILITY type="segments" query_id="human/17/segment"/>
	if (getCurrentDsn() == null)
	    System.out.println("Got new cability element " + attributes.getValue("type") + ", " + attributes.getValue("query_id") + " but current DSN is null");
	else
	    getCurrentDsn().addCapability(attributes.getValue("type"), attributes.getValue("query_id"));
    }//end if (ignore everything else for now)
  }//end startElement


  public void endElement(
    String namespaceURI,
    String tag,
    String qualifiedName
  ) throws SAXException {
    closeMode();
    if (tag.equals(SOURCE)) {
	// Save the last DSN
	if (getCurrentDsn() != null) {
	    //	    System.out.println("SourceParser.endElement: adding current dsn " + getCurrentDsn()); // DEL
	    addDsn(getCurrentDsn());
	    setCurrentDsn(null);
	}
    }//end if
  }//end endElement


  public void characters(
     char[] text,
     int start,
     int length) 
     throws SAXException {
//     String characters =  (new StringBuffer()).
//       append(text,
//              start,
//              length).toString();
//     String mode = getMode();
//     DASDsn dsn = getCurrentDsn();
    //    if (text.length > 0)
    //      System.out.println("characters: mode = " + mode + ", chars = " + characters); // DEL

//     if(mode != null && dsn != null) {
//       if(mode.equals(SOURCE)) {
//         dsn.setSource(characters);
//       } else if(mode.equals(MAPMASTER)) {
//         dsn.setMapMaster(characters);
//       } else if(mode.equals(DESCRIPTION)) {
//         dsn.setDescription(characters);
//       }//end if
//     }//end if
  }//end characters

}//end DASHandler
