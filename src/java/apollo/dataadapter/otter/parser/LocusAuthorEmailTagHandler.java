package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusAuthorEmailTagHandler extends TagHandler{
  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene 
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    gene.addProperty(TagHandler.AUTHOR_EMAIL, getCharacters());

    super.handleEndElement( theContentHandler, 
                            namespaceURI, 
                            localName, 
                            qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:author_email";
  }
  
  public String getLeafName() {
    return "author_email";
  }
}//end LocusAuthorEmailTagHandler
