package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * Responses are returned from the server when the user does an update.
 * contain everything with a marker for an author. They are added to the
 * returned objects on the controller as a string.
**/
public class ResponseTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    theContentHandler.addReturnedObject(getCharacters());
    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }
  
  public String getFullName(){
    return "otter:response";
  }//end getTag

  public String getLeafName(){
    return "response";
  }//end getTag

}//end TagHandler
