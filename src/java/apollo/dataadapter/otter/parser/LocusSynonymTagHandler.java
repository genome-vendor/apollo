package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusSynonymTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    gene.addSynonym(getCharacters());

    super.handleEndElement(theContentHandler, 
                           namespaceURI,
                           localName,
                           qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:synonym";
  }
  
  public String getLeafName() {
    return "synonym";
  }
}//end TagHandler
