package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * I add on the assembly start of the current sequence fragment.
**/
public class SequenceSetAuthorTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    AnnotatedFeatureI set
      = (AnnotatedFeatureI)theContentHandler.getCurrentObject();
    set.addProperty(TagHandler.AUTHOR, getCharacters());

    super.handleEndElement(theContentHandler,
                           namespaceURI, 
                           localName,
                           qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:author";
  }
  
  public String getLeafName() {
    return "author";
  }
}//end TagHandler
