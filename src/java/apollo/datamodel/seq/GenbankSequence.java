package apollo.datamodel.seq;

import java.util.*;
import java.lang.String;

import apollo.datamodel.Range;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;

/**
   Currently GenbankSequence is not lazy. It won't ever be
   because it simply loads a complete EMBL/Genbank entry
   in its entirety.
*/

public class GenbankSequence extends Sequence {
  protected String header;
  protected Hashtable properties = null;

  public GenbankSequence(String id, String dna) {
    super(id, dna);
  }

  public void setHeader(String header) {
    this.header = header;
  }
  
  public String getHeader() {
    return this.header;
  }

  public void addProperty(String name, String value) {
    if (value != null && !value.equals("")) {
      if (properties == null) {
        properties = new Hashtable(1, 1.0F);
      }
      String current_value;
      if (properties.get(name) != null) {
        // For the properties we're adding verbatim, the value ends with \n
        // and we don't want to add a space.
        if (value.endsWith("\n"))
          current_value = (String) properties.get(name);
        else
          current_value = (String) properties.get(name) + " ";
      } else {
        current_value = "";
      }
      properties.put(name, current_value + value);
    }
  }

  public void removeProperty(String key) {
    if (key != null && !key.equals("")) {
      if (properties == null)
        return;
      if (properties.containsKey(key)) {
        properties.remove(key);
      }
    }
  }

  public String getProperty(String name) {
    if (properties == null)
      return null;
    else 
      return (String) properties.get(name);
  }

  public Hashtable getProperties() {
    if (properties == null)
      properties = new Hashtable(1, 1.0F);
    return properties;
  }

  /* Note: low and high are relative coordinates(coords in the range). 
     genomicRange.getStart() is subtracted to transform them into java
     string zero  based coords. */
  protected String getResiduesImpl(int low, int high) {
    int str_low = low - getRange().getStart();
    /* +1 was for the inclusive end i believe
       inclusive end is now centralized in AbstractSequence to alleviate 
       confusion */
    int str_high = high - getRange().getStart();

    if (str_low == str_high)
      str_high = str_low + 1;
    
    String dna = null;
    if (residues != null && str_high < residues.length())
      dna = residues.substring (str_low, str_high);
    else if (residues != null && str_low < residues.length())
      dna = residues.substring (str_low);
    return dna;
  }

}
