package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class SequenceFragmentAssemblyEndTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    AssemblyFeature annotation = (AssemblyFeature)theContentHandler.getStackObject();
    annotation.setEnd(Integer.valueOf(getCharacters()).intValue());

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:sequence_fragment:assembly_end";
  }//end getTag

  public String getLeafName(){
    return "assembly_end";
  }//end getTag
  
}//end TagHandler
