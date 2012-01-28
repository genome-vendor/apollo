package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class ExonStrandTagHandler extends TagHandler{
  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    Exon exon = (Exon)theContentHandler.getStackObject();
    int strand = Integer.valueOf(getCharacters()).intValue();
    exon.setStrand(strand);

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:exon:strand";
  }//end getFullName
  
  public String getLeafName() {
    return "strand";
  }//end getLeafName
}//end TagHandler
