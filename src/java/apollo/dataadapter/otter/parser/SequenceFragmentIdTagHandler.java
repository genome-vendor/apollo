package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * I add on the name of the current sequence fragment.
**/
public class SequenceFragmentIdTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    AssemblyFeature annotation = (AssemblyFeature)theContentHandler.getStackObject();
    annotation.setId(getCharacters());
    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:sequence_fragment:id";
  }//end getTag
  
  public String getLeafName(){
    return "id";
  }//end getTag
  
}//end TagHandler
