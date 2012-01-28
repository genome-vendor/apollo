package apollo.dataadapter.otter.parser;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

public class LocusTypeTagHandler extends TagHandler{

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(LocusTypeTagHandler.class);

  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){

    AnnotatedFeatureI gene
      = (AnnotatedFeatureI)theContentHandler.getStackObject();
    String characters = getCharacters();
    if (!characters.equals("gene")) {
      logger.warn("Unknown locustype. Bad things will happen");
    }
    logger.info("Setting biotype to " + characters);
    gene.setTopLevelType(characters);

    super.handleEndElement( theContentHandler,
                            namespaceURI, 
                            localName,
                            qualifiedName);
  }

  public String getFullName(){
    return "otter:sequence_set:locus:locus_type";
  }
  
  public String getLeafName() {
    return "locus_type";
  }
}//end TagHandler
