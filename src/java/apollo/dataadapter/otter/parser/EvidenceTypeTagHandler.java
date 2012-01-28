package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class EvidenceTypeTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    Evidence evidence = (Evidence)theContentHandler.getStackObject();
    evidence.setDbType(getCharacters());

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:evidence:type";
  }//end getFullName
  
  public String getLeafName() {
    return "type";
  }//end getLeafName
}//end TagHandler
