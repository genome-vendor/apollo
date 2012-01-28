package apollo.dataadapter.otter.parser;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;
import apollo.datamodel.*;

/**
 * General do-nothing tag for the root of the document
**/
public class OtterTagHandler extends TagHandler{
  public String getFullName(){
    return "otter";
  }//end getTag

  public String getLeafName(){
    return "otter";
  }//end getTag
}//end TagHandler
