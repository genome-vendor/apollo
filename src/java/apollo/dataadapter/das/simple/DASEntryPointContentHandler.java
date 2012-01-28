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
 * the result of a 'entry_points' call to a das data source and dsn.</p>
 *
 * <p> I am DAS 1.0 compliant.</p>
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * entry points. This list is retrieved by calling <code>getSegments</code> after the parse
 * is complete </p>
 *
 * @see apollo.dataadapter.das.simple.SimpleDASServer#getEntryPoints
 * @author Vivek Iyer
**/
public class
      DASEntryPointContentHandler
      extends
  DefaultHandler {

  private String mode;
  private Stack modeStack = new Stack();
  private List segments = new ArrayList();

  //
  //tag labels
  private String ENTRY_POINTS="ENTRY_POINTS";
  private String SEGMENT="SEGMENT";

  //
  //attribute labels
  private String SEGMENT_ID="id";
  private String SEGMENT_SIZE="size";
  private String SEGMENT_START="start";
  private String SEGMENT_STOP="stop";
  private String SEGMENT_ORIENTATION="orientation";
  private String SEGMENT_SUBPARTS="subparts";

  private DASSegment currentSegment;

  public DASEntryPointContentHandler() {}//end DASHandler


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
    if(ENTRY_POINTS.equals(localName)) {
      setMode(ENTRY_POINTS);
    } else if(SEGMENT.equals(localName)) {
      setMode(SEGMENT);
      setCurrentSegment(
        new SimpleDASSegment(
          attributes.getValue(""),
          attributes.getValue(SEGMENT_ID),
          attributes.getValue(SEGMENT_START),
          attributes.getValue(SEGMENT_STOP),
          attributes.getValue(SEGMENT_ORIENTATION),
          attributes.getValue(SEGMENT_SUBPARTS),
          attributes.getValue(SEGMENT_SIZE)
        )
      );
    }//end if
  }//end startElement


  public void endElement(
    String namespaceURI,
    String localName,
    String qualifiedName
  ) throws SAXException {
    if(ENTRY_POINTS.equals(localName)) {
      closeMode();
      Iterator iterator = getSegments().iterator();
    } else if(SEGMENT.equals(localName)) {
      addSegment(getCurrentSegment());
      setCurrentSegment(null);
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
    DASSegment segment = getCurrentSegment();
    if(mode != null && segment != null) {
      if(mode.equals(SEGMENT)) {
        segment.setSegment(characters);
      }//end if
    }//end if
  }//end characters
}//end DASHandler
