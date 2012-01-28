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
 * the result of a 'features' call to a das data source (with a specified 
 * dsn and segment).</p>
 *
 * <p> I am DAS 1.0 compliant.</p>
 *
 * <p> During the parse, I create a list, stored on myself, of the returned
 * features. This list is retrieved by calling <code>getFeaturess</code> after the parse
 * is complete </p>
 *
 * <p> If any of the features retrieved during my run contain subparts or are 
 * reference features (have children), then I store them away. 
 * When I am done with my parse, I 
 * re-invoke the SimpleDASServer with another <code>getFeatures</code> call,
 * passing in all the segments that have children.
 * which creates another instance of me(!) and starts a another parse of 
 * the child-features. This is how I assemble all features for a given parent.</p>
 *
 * @see apollo.dataadapter.das.simple.SimpleDASServer#getEntryPoints
 * @author Vivek Iyer
**/
public class
      DASFeatureContentHandler
      extends
  DefaultHandler {

  private String mode;
  private Stack modeStack = new Stack();
  private List featureArray = new ArrayList();

  //
  //tag labels
  private String DASGFF="DASGFF";
  private String SEGMENT="SEGMENT";
  private String FEATURE="FEATURE";
  private String TYPE="TYPE";
  private String METHOD="METHOD";
  private String START="START";
  private String END="END";
  private String SCORE="SCORE";
  private String ORIENTATION="ORIENTATION";
  private String PHASE="PHASE";
  private String NOTE="NOTE";
  private String GROUP="GROUP";
  private String TARGET="TARGET";
  private String GROUPTARGET="GROUPTARGET";

  //
  //attribute labels
  private String LABEL="label";
  private String ID="id";

  private String SEGMENT_ID="id";
  private String SEGMENT_START="start";
  private String SEGMENT_STOP="stop";

  private String METHOD_ID="id";
  private String TYPE_ID="id";
  private String TYPE_CATEGORY="category";
  private String TYPE_REFERENCE="reference";
  private String TYPE_SUBPARTS="subparts";

  private String TARGET_ID="id";
  private String TARGET_START="start";
  private String TARGET_STOP="stop";

  private String GROUP_ID="id";
  private String GROUP_TARGET_START="start";
  private String GROUP_TARGET_STOP="stop";

  private DASFeature currentFeature;
  private DASSegment currentSegment;

  public DASServerI server;
  public HashMap parentFeatures;
  public HashMap segments;
  public DASDsn dsn;

  public HashMap segmentsToBeRequested = new HashMap();
  public HashMap parentFeaturesOfSegmentsToBeRequested = new HashMap();

  private static boolean print = true;
  private long globalStart;
  private long globalEnd;

  public DASFeatureContentHandler() {}//end DASHandler


  public DASFeatureContentHandler(
    DASServerI theServer,
    DASDsn theDsn,
    HashMap theSegments,
    HashMap theParentFeatures,
    long theGlobalStart,
    long theGlobalEnd
  ) {
    server = theServer;
    dsn = theDsn;
    segments = theSegments;
    parentFeatures = theParentFeatures;
    globalStart = theGlobalStart;
    globalEnd = theGlobalEnd;
  }//end DASHandler

  private long getGlobalStart() {
    return globalStart;
  }//end getGlobalStart

  private long getGlobalEnd() {
    return globalEnd;
  }//end getGlobalEnd

  private DASServerI getServer() {
    return server;
  }//end getServer

  private HashMap getSegments() {
    return segments;
  }//end getSegments

  private DASDsn getDSN() {
    return dsn;
  }//end getDSN

  private void setSegments(HashMap theSegments) {
    segments = theSegments;
  }//end setSegments

  private void setDSN(DASDsn theDSN) {
    dsn = theDSN;
  }//end setDSN

  private HashMap getSegmentsToBeRequested() {
    return segmentsToBeRequested;
  }//end getSegmentsToBeRequested

  private HashMap getParentFeaturesOfSegmentsToBeRequested() {
    return parentFeaturesOfSegmentsToBeRequested;
  }//end getParentFeaturesOfSegmentsToBeRequested

  private HashMap getParentFeatures() {
    return parentFeatures;
  }//end getParentFeatures

  private DASFeature getCurrentFeature() {
    return currentFeature;
  }//end getCurrentFeature

  private void setCurrentFeature(DASFeature theFeature) {
    currentFeature = theFeature;
  }//end setCurrentFeature

  private DASSegment getCurrentSegment() {
    return currentSegment;
  }//end getCurrentSegment

  private void setCurrentSegment(DASSegment theSegment) {
    currentSegment = theSegment;
  }//end setCurrentSegment

  private void addFeature(DASFeature theFeature) {
    getFeatures().add(theFeature);
  }//end addFeature

  public List getFeatures() {
    return featureArray;
  }//end getFeatures

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

    if(DASGFF.equals(localName)) {
      setMode(DASGFF);
    } else if(SEGMENT.equals(localName)) {
      setMode(SEGMENT);
      setCurrentSegment(
        new SimpleDASSegment(
          attributes.getValue(SEGMENT_ID),
          attributes.getValue(SEGMENT_ID),
          attributes.getValue(SEGMENT_START),
          attributes.getValue(SEGMENT_STOP),
          "",
          "",
          ""
        )
      );
    } else if(FEATURE.equals(localName)) {
      setMode(FEATURE);
      setCurrentFeature(
        new SimpleDASFeature(attributes.getValue(ID))
      );
      getCurrentFeature().setId(attributes.getValue(ID));
      getCurrentFeature().setLabel(attributes.getValue(LABEL));
    } else if(TYPE.equals(localName)) {
      setMode(TYPE);
      getCurrentFeature().setTypeId(attributes.getValue(TYPE_ID));
      getCurrentFeature().setTypeCategory(attributes.getValue(TYPE_CATEGORY));
      getCurrentFeature().setTypeReference(attributes.getValue(TYPE_REFERENCE));
      getCurrentFeature().setTypeSubparts(attributes.getValue(TYPE_SUBPARTS));
    } else if(METHOD.equals(localName)) {
      setMode(METHOD);
      getCurrentFeature().setMethodId(attributes.getValue(METHOD_ID));
    } else if(START.equals(localName)) {
      setMode(START);
    } else if(END.equals(localName)) {
      setMode(END);
    } else if(ORIENTATION.equals(localName)) {
      setMode(ORIENTATION);
    } else if(PHASE.equals(localName)) {
      setMode(PHASE);
    } else if(NOTE.equals(localName)) {
      setMode(NOTE);
    } else if(GROUP.equals(localName)) {
      setMode(GROUP);
      getCurrentFeature().setGroupId(attributes.getValue(GROUP_ID));
    } else if(TARGET.equals(localName)) {
      if(getMode().equals(FEATURE)) {
        setMode(TARGET);
        getCurrentFeature().setTargetId(attributes.getValue(TARGET_ID));
        getCurrentFeature().setTargetStart(attributes.getValue(TARGET_START));
        getCurrentFeature().setTargetStop(attributes.getValue(TARGET_STOP));
      } else if(getMode().equals(GROUP)) {
        setMode(GROUPTARGET);
        getCurrentFeature().setGroupTargetStart(attributes.getValue(GROUP_TARGET_START));
        getCurrentFeature().setGroupTargetStop(attributes.getValue(GROUP_TARGET_STOP));
      } else {
        throw new apollo.dataadapter.NonFatalDataAdapterException("TARGET element found as a child to neither FEATURE nor GROUP!");
      }//end if
    }//end if
  }//end startElement

  /**
   * Convert this feature's coordinates and orientation
   * _globally_ - that is, with respect to the 
   * root feature of the set (a chromosome, say). The global start,
   * end and orientation of the set. The parent feature passed into
   * this MUST have start/end and orientation expressed GLOBALLY!
  **/
  private void mapCurrentFeatureCoordinatesToParentFeature(
    DASFeature theCurrentFeature,
    DASFeature theParentFeature,
    DASSegment theParentSegment //this is another view of the parent feature.
  ) {
    //
    //global start-end of parent
    int parentStart = Integer.valueOf(theParentFeature.getStart()).intValue();
    int parentEnd = Integer.valueOf(theParentFeature.getEnd()).intValue();

    //
    //The input Segment that this content handler was set up with
    //tell us the start and end of the window of the parent segment that
    //actually contain the subcomponents of interest -- this is important,
    //because the GLOBAL start/end of the parent segment (above) are actually
    //the start and end of this input window, NOT the start/end of the whole
    //parent.
    int windowStart = Integer.valueOf(theParentSegment.getStart()).intValue();

    int localStart = Integer.valueOf(theCurrentFeature.getStart()).intValue();
    int localEnd = Integer.valueOf(theCurrentFeature.getEnd()).intValue();

    if(theParentFeature != null) {
      if(theParentFeature.getOrientation().equals("+")) {
        theCurrentFeature
        .setStart(
          String.valueOf(parentStart+localStart-windowStart)
        );

        theCurrentFeature
        .setEnd(
          String.valueOf(parentStart+localEnd-windowStart)
        );

        if(theCurrentFeature.getOrientation().equals("+")) {
          theCurrentFeature.setOrientation("+");
        } else if(theCurrentFeature.getOrientation().equals("-")) {
          theCurrentFeature.setOrientation("-");
        } else if(theCurrentFeature.getOrientation().equals("0")) {
          theCurrentFeature.setOrientation("0");
        } else {
          throw new apollo.dataadapter.NonFatalDataAdapterException(
            "Feature with undefined orientation: "+currentFeature
          );
        }//end if

      }
      else if(theParentFeature.getOrientation().equals("-")) {
        theCurrentFeature
        .setStart(String.valueOf(parentEnd-localEnd+windowStart));

        theCurrentFeature
        .setEnd(String.valueOf(parentEnd-localStart+windowStart));

        if(theCurrentFeature.getOrientation().equals("+")) {
          theCurrentFeature.setOrientation("-");
        } else if(theCurrentFeature.getOrientation().equals("-")) {
          theCurrentFeature.setOrientation("+");
        } else if(theCurrentFeature.getOrientation().equals("0")) {
          theCurrentFeature.setOrientation("0");
        } else {
          throw new apollo.dataadapter.NonFatalDataAdapterException(
            "Child Feature with undefined orientation: "+currentFeature
          );
        }//end if

      }
      else {
        throw new apollo.dataadapter.NonFatalDataAdapterException(
          "Parent Feature with undefined orientation: "+theParentFeature
        );
      }//end if
    }//end if
  }//end mapCurrentFeatureCoordinatesToParentFeature


  public void endElement(
    String namespaceURI,
    String localName,
    String qualifiedName
  ) throws SAXException {

    boolean hasSubcomponents;
    boolean liesWhollyOutsideRange;
    boolean liesPartlyOutsideRange;

    if(DASGFF.equals(localName)) {
      //
      //We've come to the end of all features returned for the input
      //segments. If any of these features had children themselves
      //we will recursively call getFeatures() to find the child features.
      if(getSegmentsToBeRequested().size() > 0) {

        //
        //The recursive descent. Note that there are two bits of information
        //going down -
        //
        //1. The parent feature packaged as a DASSegment: this contains the
        //id of the parent feature, and start and end coords of the piece of
        //the parent actually _used_. You MUST only make a das-call with this
        //start and end in the URL.
        //
        //2. The parent feature itself, which has the start/end of its _used_ portion,
        // already mapped into global coords.
        //That is - this is the same location as that going down in the segment-argument,
        //but mapped to global coordinates.
        //
        getFeatures().addAll(
          ((SimpleDASServer)getServer())
          .getFeatures(
            getDSN(),
            getSegmentsToBeRequested(),
            getParentFeaturesOfSegmentsToBeRequested(),
            getGlobalStart(),
            getGlobalEnd()
          )
        );
      }//end if

    } else if(SEGMENT.equals(localName)) {
      //
      //The change in segment in the returned code is not all that
      //significant.
      setCurrentSegment(null);
      closeMode();

    } else if(FEATURE.equals(localName)) {

      DASFeature theCurrentFeature = getCurrentFeature();

      //
      //Parent feature is assumed to have global coordinates
      //and orientation. We alter current feature to have global
      //coordinates and orientation.
      if(getParentFeatures().get(getCurrentSegment().getId()) != null) {
        mapCurrentFeatureCoordinatesToParentFeature(
          theCurrentFeature,
          (DASFeature)getParentFeatures().get(getCurrentSegment().getId()),
          (DASSegment)getSegments().get(getCurrentSegment().getId())
        );
      }//end if

      liesWhollyOutsideRange =
        Integer.valueOf(theCurrentFeature.getEnd()).intValue() < getGlobalStart() ||
        Integer.valueOf(theCurrentFeature.getStart()).intValue() > getGlobalEnd();

      liesPartlyOutsideRange =
        liesWhollyOutsideRange ||
        (
          Integer.valueOf(theCurrentFeature.getStart()).intValue() < getGlobalStart() &&
          Integer.valueOf(theCurrentFeature.getEnd()).intValue() >= getGlobalStart()
        )
        ||
        (
          Integer.valueOf(theCurrentFeature.getEnd()).intValue() > getGlobalEnd() &&
          Integer.valueOf(theCurrentFeature.getStart()).intValue() <= getGlobalEnd()
        );

      hasSubcomponents =
        (
          theCurrentFeature.getTypeReference() != null
          &&
          theCurrentFeature.getTypeReference().equals("yes")
        )
        ||
        (
          theCurrentFeature.getTypeSubparts() != null
          &&
          theCurrentFeature.getTypeSubparts().equals("yes")
        );

      //
      //If the feature has child features, then add this feature
      //to a list of segments/features of interest - we will make a recursive
      //call to the server to get the features belonging to these segments
      //when we are done with all the features at this level.
      if(hasSubcomponents) {
        if(
          !liesWhollyOutsideRange
          &&
          theCurrentFeature.getTargetId()!=null
          &&
          theCurrentFeature.getTargetId().trim().length() > 0
        ) {
          getSegmentsToBeRequested().put(
            theCurrentFeature.getTargetId(),
            new SimpleDASSegment(
              theCurrentFeature.getTargetId(),
              theCurrentFeature.getTargetId(),
              theCurrentFeature.getTargetStart(),
              theCurrentFeature.getTargetStop(),
              "",
              "",
              ""
            )
          );

          getParentFeaturesOfSegmentsToBeRequested().put(
            theCurrentFeature.getTargetId(),
            theCurrentFeature
          );

          addFeature(theCurrentFeature);
        }//end if
      }else {
        if(!liesPartlyOutsideRange) {
          addFeature(theCurrentFeature);
        }//end if
      }//end if

      setCurrentFeature(null);
      closeMode();
    } else if(METHOD.equals(localName)) {
      closeMode();
    } else if(START.equals(localName)) {
      closeMode();
    } else if(END.equals(localName)) {
      closeMode();
    } else if(SCORE.equals(localName)) {
      closeMode();
    } else if(ORIENTATION.equals(localName)) {
      closeMode();
    } else if(PHASE.equals(localName)) {
      closeMode();
    } else if(NOTE.equals(localName)) {
      closeMode();
    } else if(TARGET.equals(localName)) {
      closeMode();
    } else if(GROUP.equals(localName)) {
      closeMode();
    } else if(GROUPTARGET.equals(localName)) {
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
    DASFeature feature = getCurrentFeature();
    if(mode != null && feature != null) {
      if(mode.equals(TYPE)) {
        feature.setTypeLabel(characters);
      } else if(mode.equals(METHOD)) {
        feature.setMethodLabel(characters);
      } else if(mode.equals(START)) {
        feature.setStart(characters);
      } else if(mode.equals(END)) {
        feature.setEnd(characters);
      } else if(mode.equals(SCORE)) {
        feature.setScore(characters);
      } else if(mode.equals(ORIENTATION)) {
        feature.setOrientation(characters);
      } else if(mode.equals(PHASE)) {
        feature.setPhase(characters);
      } else if(mode.equals(NOTE)) {
        feature.setNote(characters);
      }//end if
    }//end if
  }//end characters

  /*
      public static void main(String[] args){
          try {
              XMLReader parser = XMLReaderFactory.createXMLReader();
              DASFeatureContentHandler handler = new DASFeatureContentHandler();
              parser.setContentHandler(handler);
              parser.parse(args[0]);
           }catch (Exception e) {
              e.printStackTrace();
          }//end try
      }
  */
}//end DASHandler
