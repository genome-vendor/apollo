package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class ExonFrameTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    Exon exon = (Exon)theContentHandler.getStackObject();
    String characters = getCharacters();
    if(characters != null && characters.trim().length() >0){

      // Convert frame to phase
      // exon.setPhase((3-Integer.valueOf(characters).intValue())%3);

      exon.setPhase(Integer.valueOf(characters).intValue());
    }else{
      exon.setPhase(0);
    }

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:exon:frame";
  }//end getFullName
  
  public String getLeafName() {
    return "frame";
  }//end getLeafName

}//end TagHandler
