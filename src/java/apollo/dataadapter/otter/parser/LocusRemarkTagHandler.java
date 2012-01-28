package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusRemarkTagHandler extends TagHandler{

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    Comment comment = 
      new Comment(
        gene.getId(),
        getCharacters(),
        "no author",
        0 //should be a long representing a timestamp.
      );

    gene.addComment(comment);

    super.handleEndElement(theContentHandler,
                           namespaceURI,
                           localName,
                           qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:remark";
  }//end getFullName
  
  public String getLeafName() {
    return "remark";
  }//end getLeafName
}//end TagHandler
