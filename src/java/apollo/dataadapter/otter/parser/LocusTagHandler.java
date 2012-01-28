package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * I will create a new Locus when my tag is found. When I am closed, I will
 * add it to the current GenericAnnotationSet.
**/
public class LocusTagHandler extends TagHandler{
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

    AnnotatedFeatureI gene = new AnnotatedFeature();

    //
    //pushes annotation on the stack so subsequence tags can
    //modify it.
    theContentHandler.pushStackObject(gene);
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
    AnnotatedFeatureI gene = (AnnotatedFeatureI)theContentHandler.popStackObject();
    ((AnnotatedFeatureI)theContentHandler.getCurrentObject()).addFeature(gene);
    gene.setRefFeature((AnnotatedFeatureI)theContentHandler.getCurrentObject());
    // gene.setHolder(true);
    if (gene.getTopLevelType().equals("partial")) {
      gene.setFeatureType("partial");
      for (int i=0;i<gene.size();i++) {
        gene.getFeatureAt(i).setFeatureType("partial");
      }
    } else {
      gene.setFeatureType("otter");
    }
  }//end handleTag

  public String getFullName(){
    return "otter:sequence_set:locus";
  }//end getFullName

  public String getLeafName() {
    return "locus";
  }//end getLeafName

}//end TagHandler
