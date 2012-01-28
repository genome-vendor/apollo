package apollo.dataadapter;

//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

//import apollo.util.SequenceUtil;

import apollo.dataadapter.DataInputType;

import org.apache.log4j.*;

/** This lumps together DataInputType and getInput String in ApolloDataAdapterI.
    This deals with location strings, which lump together chrom, start and end
    At the moment its assumed location strings are of the ilk "chrom#:start-end"
    The other apollo location format of "Chr chrom# start end" needs to be dealt
    with at some point as well.
*/

public class DataInput {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DataInput.class);

  private DataInputType inputType;
  private Region region;
  private String inputString;
  private String sequenceFilename; // for separate seq files


  public DataInput(DataInputType inputType, String inputString) {
    this.inputType = inputType;
    this.inputString = inputString;
    if (isRegion())
      parseRegion(this.inputString);
  }

  public DataInput(String soType, String inputString) {
    if (soType == null) return; // print? exception? prog error

    inputType = DataInputType.getDataTypeForSoType(soType);
    if (inputType.isLocation())
      parseRegion(inputString);
    else
      this.inputString = inputString;
  }

  
  public DataInput(String chrom, String start, String end) {
    region = new Region(chrom,start,end); // throws runtime exceptions
    inputType = DataInputType.BASEPAIR_RANGE;
  }

  /** Constructor for a location with Strings for chrom, start, & end & so type */
  public DataInput(String chrom, String start, String end, String seqType) {
    this(chrom,start,end);
    inputType.setSoType(seqType);
  }

  public DataInput(DataInputType inputType) {
    this.inputType = inputType;
  }

  public DataInput(String inputString) {
    this.inputString = inputString;
  }

  /** if file starts with http: then its really an url so correct the type
   this should only be used for types that are either file or url - none others
   file adapters in other words */
  public void setTypeToFileOrUrl() {
    if (inputString == null) return;
    inputString = inputString.trim();
    if (inputString.startsWith("http:")) {
      inputType = DataInputType.URL;
    }
    else {
      inputType = DataInputType.FILE;
    }
  }

  public DataInputType getType() { return inputType; }
  public String getSoType() { 
    if (getType() == null) {
      logger.error("error: no type in DataInput for getSoType(), returning null");
      return null;
    }
    return getType().getSoType(); 
  }
  public void setSoType(String soType) {
    getType().setSoType(soType);
  }
  public String getInputString() {
    if (haveRegion())
      return region.getColonDashString();
    return inputString; 
  }

  /** So for all input types except BASEPAIR_RANGE/regions the "seqId" is the input
      string itself. For a Region the chromosome is the seq id. This may be too 
      database/chado specific and if so i could take it out of here. */
  public String getSeqId() {
    logger.debug("DataInput.getSeqId() isRegion "+isRegion()+" in string "+getInputString()+" this "+this);
    if (isRegion())
      return getRegion().getChromosome();
    return getInputString();
  }

  public void setType(DataInputType type) {
    this.inputType = type;
    if (isRegion() && haveInputString())
      parseRegion(getInputString());
  }

  public void setInputString(String inputString) {
    this.inputString = inputString;
    if (isRegion())
      parseRegion(inputString);
  }

  private boolean haveInputString() {
    return getInputString() != null;
  }

  public boolean isFile() { return getType() == DataInputType.FILE; }

  public String getFilename() {
    if (!isFile())
      return null;
    return getInputString();
  }

  /** Returns true if DataInputType is BASEPAIR_RANGE */
  public boolean isRegion() {
    return getType() == DataInputType.BASEPAIR_RANGE;
  }

  // this is admittedly an awkward method that points to a need for refactoring
  // SO types DataInputTypes are getting muddled
  public void makeDataTypeRegion() {
    if (isRegion()) return; // already is
    // alright its kinda wierd that an enum has state - its not an enum anymore
    // but for now...
    String soType = inputType.getSoType();
    DataInputType t = DataInputType.BASEPAIR_RANGE;
    t.setSoType(soType);
    setType(t);
  }



  public Region getRegion() { return region; }

  private void parseRegion(String regionString) {
    region = new Region(regionString);
  }


  private boolean haveRegion() { return region != null; }


  // isValid()?
  public boolean hasInput() {
    if (getType() == null)
      return false;
    return haveInputString() || haveRegion();
  }

  // GFF adapter has separate file for sequence
  public void setSequenceFilename(String sequenceFilename) {
    this.sequenceFilename = sequenceFilename;
  }

  public boolean hasSequenceFilename() {
    return getSequenceFilename() != null;
  }

  public String getSequenceFilename() {
    return sequenceFilename;
  }

  public String toString() {
    return "DataInput type: "+getType()+" input: "+getInputString();
  }
}

