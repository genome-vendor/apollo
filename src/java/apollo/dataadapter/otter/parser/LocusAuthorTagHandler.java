package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusAuthorTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene 
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    gene.addProperty(TagHandler.AUTHOR, getCharacters());

    super.handleEndElement(theContentHandler,
                           namespaceURI,
                           localName,
                           qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:author";
  }
  
  public String getLeafName() {
    return "author";
  }
}//end TagHandler
