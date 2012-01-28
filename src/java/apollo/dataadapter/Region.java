package apollo.dataadapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import apollo.util.SequenceUtil;

/** Has chromosome string and start and end ints. 
    Can parse chrom string 2L:1000-5000 
    I wanted to name this Location but theres a Location class in ensj.jar so compile
    errors went flying. it seems Region is safe. The only bummer is theres a
    basically deprecated method in ApolloDataAdapterI setRegion(String) that ensembl
    still uses, that has nothing to do with this class. If this is confusing then
    maybe this should be renamed ApolloLocation or ApolloLoc 
    should this be merged with datamodel.GenomicRange? */
public class Region {
  private String chrom; // may need to be more general than chrom
  private int start,end;

  public Region(String regionString) {
    parseRegionString(regionString); // throws runtime exception
  }

  // throws runtime exception on non-int start & end - make real ex?
  public Region(String chrom, String startString, String endString) {
    start = stringToPositiveInt(startString); // throws runtime exception
    end = stringToPositiveInt(endString);
    checkStartLessThanEnd();
    this.chrom = chrom;
  }

  public Region(String chrom, int start, int end) {
    this.start = start;
    this.end = end;
    this.chrom = chrom;
    checkStartLessThanEnd(); // runtime ex
  }    

  public String getChromosome() { return chrom; }
  public int getStart() { return start; }
  public int getEnd() { return end; }


  public String toString() { return getColonDashString(); }

  public String getColonDashString() {
    return getChromosome()+":"+getStart()+"-"+getEnd();
  }

  public void setStart(int start) {
    this.start = start;
  }

  public void setEnd(int end) {
    this.end = end;
  }
 
  /** Throws runtime exception if start > end */
  private void checkStartLessThanEnd() {
    if (start > end) {
      throw new RuntimeException(
        "End value should be greater than start value.");
    }
  }
      
  private void parseRegionString(String regionString) {
    nullCheck(regionString); // throws runtime exception
    
    boolean parsed = parseColonString(regionString);
    
    if (!parsed)
      parsed = parseChrSpacesString(regionString);

    if (!parsed)
      throw new RuntimeException("failed to parse loc string "+regionString);

    checkStartLessThanEnd();
  }


  /** Parse chrom string of chr:start-end ilk. ie 1:1000-5000 
      OR 2 colons ie 1:1000:5000 */
  private boolean parseColonString(String colonString) {
      
    Pattern p = Pattern.compile("([^:]+):(\\d+)[-:](\\d+)");
    Matcher m = p.matcher(colonString);
    boolean match = m.matches();
    if (!match)
      return false;
    chrom = m.group(1);
    String startString = m.group(2);
    String endString = m.group(3);
    start = stringToInt(startString); // runtime exception
    end = stringToInt(endString);

    return true;
  }

  /** parses "Chr 1 1000 5000", navigation bar puts this out */
  private boolean parseChrSpacesString(String spacesString) {
    Pattern p = Pattern.compile("Chr (\\S+) (\\d+) (\\d+)");
    Matcher m = p.matcher(spacesString);
    boolean match = m.matches();
    if (!match)
      return false;
    chrom = m.group(1);
    String startString = m.group(2);
    String endString = m.group(3);
    start = stringToInt(startString); // runtime exception
    end = stringToInt(endString);

    return true;
  }

  // util functions
  public static void nullCheck(String chromString) {
    if (chromString == null)
      throw new RuntimeException("Chromosome string is null");
    if (chromString.equals(""))
      throw new RuntimeException("Chromosome string is empty");
  }

  private static int stringToPositiveInt(String intString) {
    int intVal = stringToInt(intString); 
    if (intVal < 0) {
      String m = "Positive integer value should be input for start or end field.";
      throw new RuntimeException(m);
    }
    return intVal;
  }

  public static int stringToInt(String intString) {
    int intValue;

    String errorMsg = intString+" is not valid in this field--expecting positive integer value.\n";

    intString = takeOutCommas(intString);

    if (intString.length() == 0)
      throw new RuntimeException(errorMsg);

    try {
      intValue = Integer.parseInt(intString);
    }
    catch (java.lang.NumberFormatException e) {
      throw new RuntimeException(errorMsg + e.getMessage());
    }
    return intValue;
  }

  private static String takeOutCommas(String intString) {
    intString = intString.trim();
    int index = intString.indexOf(',');
    if (index < 0)
      return intString;
    else {
      return intString.replaceAll(",", "");
    }
  }

}

