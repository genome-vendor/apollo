package apollo.dataadapter;

import java.util.*;

/** A value class for the different types of Data input */
public class DataInputType {

  private String typeString;
  private static HashMap stringToType = new HashMap(8);
  private String soType;

  /** This is an attempt to correlate SO terms with DataInputType.
      The 2 should work hand in hand, until all adapters are using SO, then
      we can scrap DataInputType for SO. Only handles chromosome at the moment. 
      This should be derived from SO, or at least configured, hardwiring for now */
  public static DataInputType getDataTypeForSoType(String soType) {

    if (soType == null)
      return null; // shouldnt happen

    DataInputType dataInputType = null;

    try {
      dataInputType = stringToType(soType); // works for gene
      dataInputType.setSoType(soType);
      return dataInputType;
    }
    catch (UnknownTypeException e) {
      // so strings are unknowns - no big deal
    }

    // unknown type...

    if (soType.equals("chromosome")) { // hardwire chromosome_arm?
      dataInputType = BASEPAIR_RANGE;
    }
    else if (soType.equals("golden_path_region")) {
      dataInputType = SCAFFOLD;
    }
    // will need to add BAC when rice gets BAC

    // if the above fails just construct one with so type - this is all chado
    // adapter needs
    dataInputType = new DataInputType(soType);

    //if (dataInputType != null)
    dataInputType.setSoType(soType);
    return dataInputType;
  }

  void setSoType(String soType) {
    this.soType = soType;
  }
  
  public String getSoType() { return soType; }

  // private constructor - cant be made outside class
  private DataInputType(String typeString) {
    this.typeString = typeString;
    stringToType.put(typeString,this);
  }
  
  // should there be a constructor with typeString and soString? probably not
  // if we wanna configure so types

  public static DataInputType GENE = new DataInputType("gene");
  public static DataInputType MRNA = new DataInputType("mRNA");
  public static DataInputType CYTOLOGY = new DataInputType("cytology");
  public static DataInputType SCAFFOLD = new DataInputType("scaffold");
  public static DataInputType FILE = new DataInputType("file");
  public static DataInputType URL = new DataInputType("url");
  public static DataInputType SEQUENCE = new DataInputType("sequence");
  // Should this be "chromosome" in conformance with SO? will their be ranges that
  // are not on chromosomes? probably - need to think about this
  public static DataInputType BASEPAIR_RANGE = new DataInputType("base_pair_range");
  public static DataInputType CONTIG = new DataInputType("contig");
  public static DataInputType DIR = new DataInputType("directory");

  public String toString() {
    return typeString;
  }
  public static DataInputType stringToType(String s) throws UnknownTypeException {
    DataInputType d =  (DataInputType)stringToType.get(s);
    if (d == null)
      throw new UnknownTypeException(s);
    return d;
  }

  public static class UnknownTypeException extends Exception {
    private UnknownTypeException(String typeString) {
      super("Type "+typeString+" is not a known input type");
    }
  }


  public boolean isLocation() { return this == BASEPAIR_RANGE; }

  public static String[] getTypeStrings() {
    Set stringSet = stringToType.keySet();
    String[] s = new String[] {};
    return (String[])stringSet.toArray(s);
  }
}
