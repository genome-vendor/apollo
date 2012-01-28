package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusNameTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    String characters = getCharacters();
    gene.setDescription(characters);
    gene.setId(characters);
    gene.setName(characters);

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:name";
  }
  
  public String getLeafName() {
    return "name";
  }
}//end TagHandler
