package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;


/**
 * General superclass of the command-objects
 * which are used by the OtterContentHandler.
**/
public abstract class TagHandler {
  
  public static String LEFT = "<";
  public static String RIGHT = ">";
  public static String SLASH = "/";
  public static String OTTER = "otter";
  public static String SEQUENCE_SET = "sequence_set";
  public static String INDENT = "  ";
  public static String RETURN = "\n";
  
  public static String AUTHOR ="author";
  public static String AUTHOR_EMAIL ="author_email";
  public static String ASSEMBLY_TYPE ="assembly_type";
  
  public static String SEQUENCE_FRAGMENT ="sequence_fragment";
  public static String ID = "id";
  public static String KEYWORD ="keyword";
  public static String REMARK ="remark";
  public static String ACCESSION ="accession";
  public static String VERSION ="version";
  //public static String ACCESSION ="accession";
  public static String CHROMOSOME ="chromosome";
  public static String ASSEMBLY_START ="assembly_start";
  public static String ASSEMBLY_END ="assembly_end";
  public static String FRAGMENT_ORI ="fragment_ori";
  public static String FRAGMENT_OFFSET ="fragment_offset";
  
  public static String LOCUS = "locus";
  public static String NAME ="name";
  public static String LOCUS_TYPE = "locus_type";
  public static String STABLE_ID = "stable_id";
  public static String SYNONYM = "synonym";
  public static String KNOWN = "known";
  
  public static String TRANSCRIPT = "transcript";
  public static String CDS_START_NOT_FOUND = "cds_start_not_found";
  public static String CDS_END_NOT_FOUND = "cds_end_not_found";
  public static String MRNA_START_NOT_FOUND = "mRNA_start_not_found";
  public static String MRNA_END_NOT_FOUND = "mRNA_end_not_found";
  public static String TRANSLATION_START = "translation_start";
  public static String TRANSLATION_END = "translation_end";
  public static String TRANSLATION_STABLE_ID = "translation_stable_id";
  public static String TRANSCRIPT_CLASS = "transcript_class";
  
  public static String EXON = "exon";
  public static String START = "start";
  public static String END = "end";
  public static String FRAME = "frame";
  public static String STRAND = "strand";
  
  public static String EVIDENCE = "evidence";
  public static String TYPE = "type";
  
  private StringBuffer characterBuffer;
  
  public void handleStartElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName,
    Attributes attributes
  ){
    theContentHandler.setMode(this);
    setCharacterBuffer(new StringBuffer());
  }//handleStartElement
	
  public void handleEndElement(
    OtterContentHandler theContentHandler,
    String namespaceURI,
    String localName,
    String qualifiedName
  ){
    theContentHandler.closeMode();
  }

  public void handleCharacters(
    OtterContentHandler theContentHandler,
    char[] text,
    int start,
    int length
  ){
    String newText = (new StringBuffer()).append(text,start,length).toString();
    appendCharacterBuffer(newText);
  }//end handleTag

  public abstract String getFullName();
  
  public abstract String getLeafName();
  
  protected String getCharacters(){
    return getCharacterBuffer().toString();
  }
  
  protected StringBuffer getCharacterBuffer(){
    return characterBuffer;
  }
  
  protected void appendCharacterBuffer(String value){
    getCharacterBuffer().append(value);
  }
  
  protected void setCharacterBuffer(StringBuffer newValue){
    characterBuffer = newValue;
  }
}//end TagHandler
