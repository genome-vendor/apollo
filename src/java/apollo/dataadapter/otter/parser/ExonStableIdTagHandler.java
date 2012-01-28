package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class ExonStableIdTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    Exon exon = (Exon)theContentHandler.getStackObject();
    String characters = getCharacters();
    exon.addProperty("OtterStableId",characters);
    exon.setId(characters);
    exon.setName(characters);

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:exon:stable_id";
  }//end getFullName
  
  public String getLeafName() {
    return "stable_id";
  }//end getLeafName
}//end TagHandler
