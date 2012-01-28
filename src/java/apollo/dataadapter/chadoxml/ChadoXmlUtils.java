package apollo.dataadapter.chadoxml;

import java.util.Vector;
import org.bdgp.xml.XMLElement;

public class ChadoXmlUtils {

  public ChadoXmlUtils() {}

  public static String printXMLElement(XMLElement e) {
    if (e == null) {
      return "(empty XML element)";
    }

    StringBuffer buf = new StringBuffer();
    Vector children = e.getChildren();
    int numChildren = (children == null) ? 0 : children.size();
    buf.append(e.getID() + " (type " + e.getType() + ") has " + numChildren +  " children");
    // e.g. <_appdata  name="title">CG9397</_appdata>
    if (e.getType().equals("_appdata"))
      buf.append("\n" + printAttribute(e, "name") + ": " + printValue(e, "name"));

    return buf.toString();
  }

  public static String printAttribute(XMLElement e, String attribute) {
    if (e == null) {
      return "(empty XML element)";
    }

    return "  " + attribute + "=" + e.getAttribute(attribute);
  }

  public static String printValue(XMLElement e, String attribute) {
    String value = e.getCharData();
    String name = e.getAttribute(attribute);
    if (name.equalsIgnoreCase("residues")) {
      if (value.length() > 25)
        value = value.substring(0, 25) + "..." + value.substring(value.length()-5) + " (" + value.length() + " bases)";
    }
    return value;
  }
    
}
