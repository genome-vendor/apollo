package apollo.dataadapter.genbank;

import java.util.*;

import org.apache.log4j.*;

/** A value class for the different types of Data input */
public class PublicDbFeature {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PublicDbFeature.class);

  private static final int key_offset = 5; 

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String type;
  // this is a hash into vectors to allow for cases where
  // there is more than one value for an individual tag
  private HashMap tagValues = new HashMap(3);
  private StringBuffer location;
  private GenbankRead parent;
  private String active_tag = null;
  private Vector locs = null;

  private boolean initialized = false;
  private boolean missing5prime = false;
  private boolean extend3prime = false;
  private boolean extend5prime = false;

  // private constructor - cant be made outside class
  protected PublicDbFeature(GenbankRead parent) {
    this.parent = parent;
  }

  protected String getFeatureType(String current_line) {
    String str = current_line.substring(key_offset);
    int index = str.indexOf(' ');
    if (index > 0) {
      this.location = new StringBuffer();
      this.type = str.substring(0, index);
      this.location.append(str.substring(index).trim());
    }
    return this.type;
  }

  protected String getFeatureType() {
    return this.type;
  }

  protected boolean forSameFeature(String current_line) {
    return (current_line.charAt(key_offset) == ' ');
  }

  protected boolean addToFeature(String current_line) {
    if (forSameFeature(current_line)) {
      String str = current_line.substring(key_offset).trim();
      // if (str.indexOf('/') < 0 && active_tag == null){
      if (str.charAt(0) != '/' && active_tag == null) {
        this.location.append(str);
      }
      else {
        setTagValue(str);
      }
      return true;
    }
    else {
      return false;
    }
  }

  protected Vector getValues(String tag) {
    if (!initialized) {
      initialized = true;
      initSynonyms();
      initLocations();
    }
    Object vals = tagValues.get(tag);
    if ((vals != null) && (vals instanceof Vector))
      return (Vector) vals;
    else
      return null;
  }

  protected HashMap getTagValues() {
    return tagValues;
  }

  protected String getValue(String tag) {
    /* there were 2 options here, either just get the first
       value or get all of them concatenated together. going
       with the latter so nothing is lost */
    StringBuffer val = new StringBuffer();
    Vector all_vals = getValues(tag);
    if (all_vals != null) {
      int val_count = all_vals.size();
      for (int i = 0; i < val_count; i++) {
        val.append((String) all_vals.elementAt(i));
        if (i < val_count-1)
          val.append(" ");
      }
    }
    return val.toString();
  }

  protected HashMap getDbXrefs() {
    return (HashMap) tagValues.get("db_xref");
  }

  protected Vector getSynonyms() {
    return getValues("synonyms");
  }

  protected boolean missing5prime() {
    return this.missing5prime;
  }

  protected int getCodonStart() {
    int offset = 0;
    String offset_str = getValue("codon_start");
    if (offset_str != null && !offset_str.equals("")) {
      try {
        offset = Integer.parseInt(offset_str);
        offset--;
      } catch (Exception e) {
        offset = 0;
      }
    }
    return offset;
  }

  protected void initSynonyms() {
    Vector syns = null;
    String note = getValue("note");
    int index = (note != null ? note.indexOf("synonyms:") : -1);
    if (index >= 0) {
      syns = new Vector(1);
      String prefix = note.substring(0, index);
      String syns_str = note.substring(index + "synonyms:".length()).trim();
      String syn;
      String suffix = "";
      int end = syns_str.indexOf(';');
      if (end > 0) {
        syns_str = syns_str.substring(0, end);
        index = note.indexOf(';', index) + 1;
        if (index < note.length())
          suffix = note.substring(index);
      }
      while (syns_str.length() > 0) {
        end = syns_str.indexOf(',');
        if (end > 0) {
          syn = syns_str.substring(0, end);
          syns_str = (++end < syns_str.length() ? 
                      syns_str.substring(end) : "");
        } else {
          syn = syns_str;
          syns_str = "";
        }
        syns.addElement(syn);
      }
      Vector note_vec = (Vector) tagValues.get("note");
      note_vec.removeElementAt(0);
      note_vec.addElement(prefix + suffix);
    }
    if (syns != null)
      tagValues.put("synonyms", syns);
  }

  private void setTagValue(String content) {
    String tag;
    String value;
    String db_tag = null;
    String db_value = null;
    String synonyms = null;

    if (content.charAt(0) == '/') {
      int index = content.indexOf("=");
      if (index >= 0) {
        tag = content.substring(1, index);
        value = content.substring(index + "=".length());
        value = stripQuotes(value);
        
        index = value.indexOf(':');
        if (index > 0 &&
            !tag.equals("note") && !value.substring(0, index).contains(" ") &&
            !tag.equals("gene") &&
            !tag.equals("method") &&
            !tag.equals("date") &&
            !tag.endsWith("synonym") &&
            !tag.equals("product") &&
            !tag.equals("prot_desc")) {
          if (!tag.equals("db_xref")) {
            // Why do we want to do this?  I see lots of cases where we *don't*
            // want to, but I don't see when we'd ever want to.  --NH
            tag = value.substring(0, index);
            String tmp = value.substring(index + ":".length());
            logger.debug("setTagValue: changing tag " + tag + " from " +
                         value + " to " + tmp);
            value = tmp;
          } else {  // db_xref
            db_tag = value.substring(0, index);
            db_value = value.substring(index + ":".length());
          }
        }
        active_tag = tag;
      } else if (content.charAt(0) == '/') {
        tag = content.substring(1);
        value = "true";
      } else {
        tag = active_tag;
        value = content;
      }
    } else {
      tag = active_tag;
      value = content;
      value = stripQuotes(value);
    }

    if (db_tag == null) {
      if (!value.equalsIgnoreCase("unknown")) {
        Vector current_vec = (Vector) tagValues.get(tag);
        if (current_vec == null) {
          current_vec = new Vector();
          tagValues.put(tag, current_vec);
        }
        else if (content.charAt(0) != '/' || tag.equals("note")) {
          /* This is an extension, not a second occurance, of a tag */
          int i = current_vec.size() - 1;
          String current_value = (String) current_vec.elementAt(i);
          current_vec.removeElementAt(i);
          value = current_value + " " + value;
        }
        if (!value.equals("") && !value.equals(".")) {
          current_vec.addElement(value);
        }
      }
    } else {
        HashMap current_dbs = (HashMap) tagValues.get(tag);
        if (current_dbs == null) {
          current_dbs = new HashMap();
          tagValues.put(tag, current_dbs);
        }
        current_dbs.put(db_tag, db_value);

    }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(type + " at " + location.toString() + "\n");
    Iterator keys = tagValues.keySet().iterator();
    while (keys.hasNext()) {
      String tag = (String) keys.next();
      if (!tag.equals("translation") &&
          !tag.equals("method") &&
          !tag.equals("db_xref"))
        buf.append("\t" + tag + " = " + getValue(tag) + "\n");
    }
    return buf.toString();
  }
  
  private String stripQuotes(String value) {
    if (value.equals(""))
      return "";

    if (value.charAt(0) == '\"')
      value = value.substring(1);

    if (value.length() >= 1 && value.charAt(value.length() - 1) == '\"')
      return value.substring(0, value.length() - 1);
    else
      return value;
  }

  protected Vector getLocation() {
    if (!initialized) {
      initialized = true;
      initSynonyms();
      initLocations();
    }
    return locs;
  }

  protected void initLocations() {
    locs = new Vector();
    parseLocations(this.location.toString(), locs);
    if (extend5prime)
      missing5prime = true;
  }

  /**
     location operators ::= complement, join, order
     locations ::= 
     basenumber^basenumber (insertion between these)
     basenumber..basenumber  (span)
     basenumber.basenumber (single base)
     basenumber (single base)
     adding < and > now too
     entryname:basenumber..basenumber (indirect references)
     *  
     *  
     **/
  private void parseLocations(String location_str, Vector locs) {
    if (location_str != null && !location_str.equals("")) {
      String operation_str = null;
      int index_start = 0;
      int index_end = 0;
      if (location_str.startsWith("complement(")) {
        Vector comp_vect = new Vector();
        index_start = "complement(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, comp_vect);
        int cmp_count = comp_vect.size();
        for (int i = cmp_count - 1; i >= 0; i--) {
          int [] span = (int []) comp_vect.elementAt(i);
          comp_vect.remove(span);
          int tmp = span[0];
          span[0] = span[1];
          span[1] = tmp;
          locs.addElement(span);
        }
        if (extend3prime) {
          extend5prime = true;
          extend3prime = false;
        } else if (extend5prime) {
          extend5prime = false;
          extend3prime = true;
        }
      }
      else if (location_str.startsWith("join(")) {
        index_start = "join(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, locs);
      }
      else if (location_str.startsWith("order(")) {
        index_start = "order(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, locs);
      }
      else if (!Character.isDigit(location_str.charAt(0))) {
        if (location_str.charAt(0) == '<') {
          logger.debug("PublicDbFeature.parseLocations: location_str " + location_str + " starts with <; extend5prime = true");
          extend5prime = true;
          parseLocations(location_str.substring(1), locs);
        }
        else if (location_str.indexOf(':') > 0) {
          // 6/21/04: Sometimes the EMBL file has a source like
          // FT   source          AJ009736:1..7411
          // Why should this make us refuse to parse the file?  Let's just
          // strip off the part before the : (and hope for the best).
          logger.warn("Ignoring indirect location reference in " + location_str);
          parseLocations(location_str.substring(location_str.indexOf(':')+1), locs);
        }
        else
          parseLocations(location_str.substring(1), locs);
      }
      else {
        index_start = 0;
        index_end = indexOfNextNonDigit(location_str, index_start);
        String pos_str = "";
        try {
          pos_str = substringLocation(location_str, 
                                      index_start, index_end);
          int low = Integer.parseInt(pos_str);
          int high;
          boolean no_high = ((index_end < location_str.length() &&
                              location_str.charAt(index_end) == ',') ||
                             index_end >= location_str.length());
          if (no_high) {
            high = low;
          }
          else {
            index_start = ++index_end;
            if (location_str.charAt(index_start) == '>') {
              extend3prime = true;
              index_start++;
            }
            if (Character.isDigit(location_str.charAt(index_start))) {
              // This is a point location
              index_end = indexOfNextNonDigit(location_str, index_start);
              pos_str = substringLocation(location_str, 
                                          index_start, index_end);
              high = Integer.parseInt(pos_str);
              if (high != low) {
                low = low + ((high - low + 1) / 2);
                high = low + 1;
              }
              else
                high++;
            } else {
              index_start++;
              if (location_str.charAt(index_start) == '>') {
                extend3prime = true;
                index_start++;
              }
              index_end = indexOfNextNonDigit(location_str, index_start);
              pos_str = substringLocation(location_str, 
                                          index_start, index_end);
              high = Integer.parseInt(pos_str);
            }
          }
          int [] pos = new int [2];
          pos[0] = low;
          pos[1] = high;
          locs.addElement(pos);
        } catch (Exception e) {
          logger.error("Unable to parse location from " + pos_str, e);
        }
      }
      int index_comma = (index_end < location_str.length() ?
			 location_str.indexOf(",", index_end) : -1);
      int next_locs = (index_comma >= 0 ?
		       index_comma + 1 : 0);
      if (next_locs > 0 && next_locs < location_str.length())
        parseLocations(location_str.substring(next_locs), locs);
    }
  }

  private int indexOfNextNonDigit(String location_str, int index_start) {
    int index_end = index_start;
    while (index_end < location_str.length() &&
           Character.isDigit(location_str.charAt(index_end)))
      index_end++;
    return index_end;
  }

  private String substringLocation(String location_str, 
                                   int index_start, int index_end) {
    try {
      return (index_end < location_str.length() ?
              location_str.substring(index_start, index_end) :
              (index_start < location_str.length() ? 
               location_str.substring(index_start) : location_str));
    } catch (Exception e) {
      logger.error("Could not substring " + location_str +
                   " index_start=" + index_start +
                   " index_end=" + index_end, e);
      //      System.exit(1);
      return null;
    }
  }

  private int indexOfClosingParen(String location_str, int index_start) {
    int index_end = index_start;
    int open_paren_count = 1;
    while (index_end < location_str.length() &&
           open_paren_count != 0) {
      if (location_str.charAt(index_end) == ')')
        open_paren_count--;
      else if (location_str.charAt(index_end) == '(')
        open_paren_count++;
      if (open_paren_count != 0)
        index_end++;
    }
    return index_end;
  }

}
