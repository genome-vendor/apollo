package apollo.dataadapter.das.simple;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import java.io.*;
import java.util.*;

import apollo.dataadapter.das.*;

/**
 * <p>I am the ContentHandler used when <code>SimpleDASServer</code> parses
 * the result of a 'dsn' call to a das data source.</p>
 *
 * <p> I am DAS 1.0 compliant.</p>
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * DSN's. This list is retrieved by calling <code>getDsns</code> after the parse
 * is complete </p>
 *
 * @see apollo.dataadapter.das.simple.SimpleDASServer#getDSNs
 * @author Vivek Iyer
**/
public class
      DASDsnContentHandler
      extends
  DefaultHandler {

  private String mode;
  private Stack modeStack = new Stack();
  private List dsns = new ArrayList();

  //
  //tag labels
  private String DASDSN="DASDSN";
  private String DSN="DSN";
  private String SOURCE="SOURCE";
  private String MAPMASTER="MAPMASTER";
  private String DESCRIPTION="DESCRIPTION";

  //
  //attribute labels
  private String SOURCE_ID="id";
  private String SOURCE_VERSION="version";

  private DASDsn currentDsn;

  public DASDsnContentHandler() {}//end DASHandler


  private DASDsn getCurrentDsn() {
    return currentDsn;
  }

  private void setCurrentDsn(DASDsn theDsn) {
    currentDsn = theDsn;
  }

  private void addDsn(DASDsn theDsn) {
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

  public void startElement(
    String namespaceURI,
    String localName,
    String qualifiedName,
    Attributes attributes
  ) throws SAXException {

    if(DASDSN.equals(localName)) {
      setMode(DASDSN);
    } else if(DSN.equals(localName)) {
      setMode(DSN);
      setCurrentDsn(new SimpleDASDsn("","","","",""));
    } else if(SOURCE.equals(localName)) {
      setMode(SOURCE);
      getCurrentDsn().setSourceId(attributes.getValue(SOURCE_ID));
      getCurrentDsn().setSourceVersion(attributes.getValue(SOURCE_VERSION));
    } else if(MAPMASTER.equals(localName)) {
      setMode(MAPMASTER);
    } else if(DESCRIPTION.equals(localName)) {
      setMode(DESCRIPTION);
    }//end if
  }//end startElement


  public void endElement(
    String namespaceURI,
    String localName,
    String qualifiedName
  ) throws SAXException {
    if(DASDSN.equals(localName)) {
      closeMode();
    } else if(DSN.equals(localName)) {
      addDsn(getCurrentDsn());
      setCurrentDsn(null);
      closeMode();
    } else if(SOURCE.equals(localName)) {
      closeMode();
    } else if(MAPMASTER.equals(localName)) {
      closeMode();
    } else if(DESCRIPTION.equals(localName)) {
      closeMode();
    }//end if
  }//end endElement


  public void characters(
    char[] text,
    int start,
    int length
  )throws SAXException {
    String characters =
      (new StringBuffer())
      .append(
        text,
        start,
        length
      ).toString();
    String mode = getMode();
    DASDsn dsn = getCurrentDsn();
    if(mode != null && dsn != null) {
      if(mode.equals(SOURCE)) {
        dsn.setSource(characters);
      } else if(mode.equals(MAPMASTER)) {
        dsn.setMapMaster(characters);
      } else if(mode.equals(DESCRIPTION)) {
        dsn.setDescription(characters);
      }//end if
    }//end if
  }//end characters

}//end DASHandler
