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
 * the result of a 'dna' call to a das data source (passing in
 * dsn and segment).</p>
 *
 * <p> I am DAS 1.0 compliant.</p>
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * sequences. This list is retrieved by calling <code>getSequences</code> after the parse
 * is complete </p>
 *
 * @see apollo.dataadapter.das.simple.SimpleDASServer#getEntrySequences
 * @author Vivek Iyer
**/
public class DASSequenceContentHandler extends DefaultHandler {

  private String mode;
  private Stack modeStack = new Stack();
  private List sequenceArray = new ArrayList();

  //
  //tag labels
  private String DASDNA="DASDNA";
  private String SEQUENCE="SEQUENCE";
  private String DNA="DNA";

  //
  //attribute labels
  private String LABEL="label";
  private String SEQUENCE_ID="id";
  private String SEQUENCE_START="start";
  private String SEQUENCE_STOP="stop";
  private String SEQUENCE_VERSION="version";
  private String DNA_LENGTH="length";

  private DASSequence currentSequence;

  public DASServerI server;
  public DASSegment[] segments;
  public DASDsn dsn;

  private static boolean print = true;

  public DASSequenceContentHandler() {}//end DASHandler


  public DASSequenceContentHandler(
    DASServerI theServer,
    DASDsn theDsn, DASSegment[] theSegments
  ) {
    server = theServer;
    dsn = theDsn;
    segments = theSegments;
  }//end DASHandler

  private DASServerI getServer() {
    return server;
  }//end getServer

  private DASSegment[] getSegments() {
    return segments;
  }//end getSegments

  private DASDsn getDSN() {
    return dsn;
  }//end getDSN

  private void setSegments(DASSegment[] theSegments) {
    segments = theSegments;
  }//end setSegments

  private void setDSN(DASDsn theDSN) {
    dsn = theDSN;
  }//end setDSN

  private DASSequence getCurrentSequence() {
    return currentSequence;
  }//end getCurrentSequence

  private void setCurrentSequence(DASSequence theSequence) {
    currentSequence = theSequence;
  }//end setCurrentSequence

  private void addSequence(DASSequence theSequence) {
    getSequences().add(theSequence);
  }//end addSequence

  public List getSequences() {
    return sequenceArray;
  }//end addSequence

  private Stack getModeStack() {
    return modeStack;
  }//end getModeStack

  private void setMode(String theMode) {
    getModeStack().push(theMode);
  }//end setMode

  private String getMode() {
    if(!getModeStack().isEmpty()) {
      return (String)getModeStack().peek();
    } else {
      return null;
    }//end if
  }//end getMode


  private void closeMode() {
    if(!getModeStack().isEmpty()) {
      getModeStack().pop();
    }//end if
  }//end closeMode


  public void startElement(
    String namespaceURI,
    String localName,
    String qualifiedName,
    Attributes attributes
  ) throws SAXException {
    if(DASDNA.equals(localName)) {
      setMode(DASDNA);
    } else if(SEQUENCE.equals(localName)) {
      setMode(SEQUENCE);
      setCurrentSequence(
        new SimpleDASSequence(
          attributes.getValue(SEQUENCE_ID),
          attributes.getValue(SEQUENCE_START),
          attributes.getValue(SEQUENCE_STOP),
          attributes.getValue(SEQUENCE_VERSION),
          null,
          null
        )
      );
    } else if(DNA.equals(localName)) {
      setMode(DNA);
      getCurrentSequence().setDNALength(attributes.getValue(DNA_LENGTH));
    }//end if
  }//end startElement


  public void endElement(
    String namespaceURI,
    String localName,
    String qualifiedName
  ) throws SAXException {
    if(DNA.equals(localName)) {
      closeMode();
    } else if(SEQUENCE.equals(localName)) {
      DASSequence theCurrentSequence = getCurrentSequence();
      addSequence(theCurrentSequence);
      setCurrentSequence(null);
      closeMode();
    } else if(DASDNA.equals(localName)) {
      closeMode();
    }//end if
  }//end endElement


  public void characters(
    char[] text,
    int start,
    int length
  )throws SAXException {
    String characters;
    String charactersWithNoWhiteSpace = null;
    DASSequence sequence;
    String mode = getMode();
    StringTokenizer tokenizer;

    sequence = getCurrentSequence();

    if(mode != null && sequence != null) {
      if(mode.equals(DNA)) {
        characters =
          (new StringBuffer())
          .append(
            text,
            start,
            length
          ).toString();


        tokenizer = new StringTokenizer(characters);

        while(tokenizer.hasMoreTokens()) {
          if(charactersWithNoWhiteSpace != null) {
            charactersWithNoWhiteSpace =
              charactersWithNoWhiteSpace+tokenizer.nextToken();
          } else {
            charactersWithNoWhiteSpace = tokenizer.nextToken();
          }//end if
        }//end while


        if(sequence.getDNA() != null) {
          sequence.setDNA(sequence.getDNA()+charactersWithNoWhiteSpace);
        } else {
          sequence.setDNA(charactersWithNoWhiteSpace);
        }//end if
      }//end if
    }//end if
  }//end characters

  /*
      public static void main(String[] args){
          try {
              XMLReader parser = XMLReaderFactory.createXMLReader();
              DASSequenceContentHandler handler = new DASSequenceContentHandler ();
              parser.setContentHandler(handler);
              parser.parse(args[0]);
          }catch (Exception e) {
              e.printStackTrace();
          }//end try
      }
   */
}//end DASHandler
