package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class TranscriptTranslationStableIdTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    Transcript transcript = (Transcript)theContentHandler.getStackObject();
    DbXref dbx = new DbXref("OtterTranslationId",getCharacters(),"otter");
    transcript.addDbXref(dbx);

    super.handleEndElement( theContentHandler, namespaceURI, localName, qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:transcript:translation_stable_id";
  }//end getFullName
  
  public String getLeafName() {
    return "translation_stable_id";
  }//end getLeafName
}//end TagHandler
