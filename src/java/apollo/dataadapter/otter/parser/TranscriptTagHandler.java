package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * I will create a new Gene when my tag is found. When I am closed, I will
 * add myself to the current GenericAnnotationSet.
**/
public class TranscriptTagHandler extends TagHandler{
  public void handleStartElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName,
    Attributes attributes
  ){
    //
    //should just push the mode - so subsequence tags know we're
    //dealing with a gene.
    super.handleStartElement(
      theContentHandler,
      namespaceURI,
      localName,
      qualifiedName,
      attributes
    );
    
    Transcript transcript = new Transcript();
    
    //
    //pushes annotation on the stack so subsequence tags can 
    //modify it.
    theContentHandler.pushStackObject(transcript);
  }//end handleTag
	
  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    //
    //should pop the mode off the stack.
    super.handleEndElement(
      theContentHandler,
      namespaceURI,
      localName,
      qualifiedName
    );

    //grab the annotation off the stack, and set it into the 
    //parent sequence_set.
    Transcript transcript = (Transcript)theContentHandler.popStackObject();
    AnnotatedFeatureI parentGene 
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    parentGene.addFeature(transcript);
    transcript.setRefFeature(parentGene);
    //transcript.setRefSequence(theContentHandler.getReferenceSequence());
    transcript.setRefSequence(theContentHandler.getCurationSet().getRefSequence());
    if (transcript.getTopLevelType().equals("partial")) {
      transcript.setFeatureType("partial");
    } else {
      transcript.setFeatureType("otter");
    }
    transcript.getGene().setStrand(transcript.getStrand());

    //work out translation coords if there is a translation
    
    String translationStartStr = transcript.getProperty(TagHandler.TRANSLATION_START);
    String translationEndStr = transcript.getProperty(TagHandler.TRANSLATION_END);
    java.util.Hashtable props = transcript.getProperties();
    java.util.Iterator iterator = props.keySet().iterator();
    //
    //For some odd reason a SeqFeature will h<and back an empty string
    //instead of null if you ask for a property it doesn't have.
    if (
      (translationStartStr != null) &&
      (translationStartStr.trim().length() > 0) &&
      (translationEndStr != null) &&
      (translationEndStr.trim().length() > 0)
    ){
      int genomicTranslationStartPos = Integer.parseInt(translationStartStr);
      int genomicTranslationEndPos = Integer.parseInt(translationEndStr);
      transcript.setTranslationStart(genomicTranslationStartPos);
      transcript.setTranslationEnd(genomicTranslationEndPos);
      transcript.removeProperty(TagHandler.TRANSLATION_START);
      transcript.removeProperty(TagHandler.TRANSLATION_END);
    } 

  }//end handleTag

  public String getFullName(){
    return "otter:sequence_set:locus:transcript";
  }//end getFullName
  
  public String getLeafName() {
    return "transcript";    
  }//end getLeafName
  
}//end TagHandler
