package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class EvidenceNameTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    Evidence evidence = (Evidence)theContentHandler.getStackObject();
    evidence.setSetId(getCharacters());
    evidence.setFeatureId(getCharacters());

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:evidence:name";
  }//end getFullName
  
  public String getLeafName() {
    return "name";
  }//end getLeafName
}//end TagHandler
