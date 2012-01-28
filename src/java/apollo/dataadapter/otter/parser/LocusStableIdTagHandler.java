package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusStableIdTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene 
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    DbXref dbx = new DbXref("OtterId",getCharacters(),"otter");
    gene.addDbXref(dbx);

    super.handleEndElement(theContentHandler,
                           namespaceURI,
                           localName, 
                           qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:stable_id";
  }
  
  public String getLeafName() {
    return "stable_id";
  }
}//end TagHandler
