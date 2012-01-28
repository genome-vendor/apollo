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
 * For parsing the results of a "segment" request, e.g.
 * <?xml version="1.0" encoding="UTF-8"?>
 * <SEGMENTS xmlns="http://www.biodas.org/ns/das/genome/2.00">
 *  <SEGMENT id="segment/chrI" name="chrI" length="230209" />
 *  <SEGMENT id="segment/chrII" name="chrII" length="813179" />
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * entry points. This list is retrieved by calling <code>getSegments</code> after the parse
 * is complete </p>
 *
 * @see apollo.dataadapter.das.simple.SimpleDASServer#getEntryPoints
 * @author Vivek Iyer and Nomi Harris
 *
 * 3/20/2006: New name for "id" in version 300 is "uri"
 * New name for "name" is "title"
 *  <SEGMENT uri="segment/chrI" title="chrI"
 *               length="230209" reference=""/>
**/
public class SegmentParser
  extends DefaultHandler {
  private String mode;
  private Stack modeStack = new Stack();
  private List segments = new ArrayList();

  //tag labels
  private String SEGMENTS="SEGMENTS";
  private String SEGMENT="SEGMENT";

  //attribute labels
  private String SEGMENT_ID="id";
  private String SEGMENT_NAME="name";
  private String SEGMENT_LENGTH="length";

  private DASSegment currentSegment;

  public SegmentParser() {}


  private DASSegment getCurrentSegment() {
    return currentSegment;
  }

  private void setCurrentSegment(DASSegment theSegment) {
    currentSegment = theSegment;
  }

  private void addSegment(DASSegment theSegment) {
    getSegments().add(theSegment);
  }

  /**
   * I return a list of <code>DASSegment</code>s. Each segment
   * corresponds to a single entry point in the set of entry points
   * returned by the das data source. I should be interrogated after
   * the parse is complete.
   **/
  public List getSegments() {
    return segments;
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
    setMode(localName);
    if (localName.equals(SEGMENT)) {
      //      System.out.println("SegmentParser.startElement: name=" + attributes.getValue(SEGMENT_NAME)); // DEL
      // NAME is generally the ID without the /segment
      addSegment(
                 new SimpleDASSegment(// attributes.getValue(SEGMENT_NAME),
                                      attributes.getValue("title"),
                                      // attributes.getValue(SEGMENT_ID),
                                      attributes.getValue("uri"),
                                      "",
                                      "",
                                      "",
                                      "",
                                      attributes.getValue(SEGMENT_LENGTH)));
    }//end if
  }//end startElement


  public void endElement(
    String namespaceURI,
    String localName,
    String qualifiedName
  ) throws SAXException {
//     if(ENTRY_POINTS.equals(localName)) {
//       closeMode();
//       Iterator iterator = getSegments().iterator();
//     } else if(SEGMENT.equals(localName)) {
//       addSegment(getCurrentSegment());
//       setCurrentSegment(null);
//       closeMode();
//     }//end if
  }//end endElement


  public void characters(
    char[] text,
    int start,
    int length
  )throws SAXException {
//     String characters =
//       (new StringBuffer())
//       .append(
//         text,
//         start,
//         length
//       ).toString();

//     String mode = getMode();
//     DASSegment segment = getCurrentSegment();
//     if(mode != null && segment != null) {
//       if(mode.equals(SEGMENT)) {
//         segment.setSegment(characters);
//       }//end if
//     }//end if
  }//end characters
}//end DASHandler
