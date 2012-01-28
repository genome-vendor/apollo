package apollo.gui;

import apollo.datamodel.*;
import apollo.seq.*;

import java.util.*;

import org.apache.oro.text.regex.*;


/**
 * Searches for and maintains a list of matches of a search sequence in a SequenceI.
 * The class handles reverse complement by reverse complementing the search sequence
 * so that the whole SequenceI object sequence does not have to be reverse complemented -
 * that could use a lot of memory. Coordinate conversions are done within the class
 * to generate correct coordinates.
 * The matches to the search SequenceI are generated when the object is created. They
 * can be accessed one at a time using the getNextAsSeqFeature() and getNextAsLong().
 * areMoreMatches() indicates if there are more matches to get.
 * moveToSeqPosition() sets the current SEQUENCE position to the value specified, with
 * the match position moved to the first match at a position equal to or after the
 * specified sequence position.
 */
public class SequenceSelector {
  RangeI sf;
  boolean   reverseComplement = false;
  boolean  useRegExp;
  int      start;
  int      end;
  String    queryString;
  // This keeps all of the positions Vector of int [2] entries
  Vector    zones = new Vector();
  int       curInd;
  int       blockSize = 100000;
  int       contextLen = 35;
  
  public SequenceSelector(RangeI sf, String queryStr) {
    this(sf, queryStr, false, false);
  }
  
  public SequenceSelector(RangeI sf, String queryStr,
                          boolean reverseComplement) {
    this(sf, queryStr, reverseComplement, false);
  }  
  
  public SequenceSelector(RangeI sf, String queryStr,
                          boolean reverseComplement, boolean useRegExp) {
    this.queryString = queryStr;
    this.sf = sf;
    this.start = sf.getStart();//sf.getLow();
    this.end = sf.getEnd();//sf.getHigh();
    this.useRegExp   = useRegExp;
    if (start > end) {
      int tmp = start;
      start = end;
      end = tmp;
    }
    if(sf.getRefSequence().getLength() < (3 * blockSize))
      blockSize = sf.getRefSequence().getLength()/3;
    this.reverseComplement = reverseComplement;
    findMatches();
  }
  
  
  /**
   * Finds positions in the sequence which match the query sequence.
   */
  public int findMatches() {
    //    int seqPosition = -1;
    //    int length = queryString.length();
    
    String correctedQueryString;
    
    if(!useRegExp)
      correctedQueryString = cleanUpNonRegExpString(queryString);
    else
      correctedQueryString = queryString;
    
    if (reverseComplement) {
      correctedQueryString = revCompRegExp(correctedQueryString);
    }
    
    //support for RegExp matching
    Perl5Compiler p5Compiler = new Perl5Compiler();
    Perl5Pattern pattern = null, defaultPattern = null;
    Perl5Matcher matcher = new Perl5Matcher();
    try{
      defaultPattern = 
	(Perl5Pattern)p5Compiler.compile("invalid perl5 expression");
      pattern =
	(Perl5Pattern)p5Compiler.compile(correctedQueryString,
                                         p5Compiler.CASE_INSENSITIVE_MASK);
    }
    catch(MalformedPatternException ex){
      pattern = defaultPattern;
    }
    
    PatternMatcherInput pmInput = null;
    int chunkLen = blockSize + 2*correctedQueryString.length();
    SequenceI seq = sf.getRefSequence();
    for (int i=(int)start; i<=(int)end; i+=blockSize) {
      //      int limit = (i+chunkLen < end ? i+chunkLen : end);
      String seqString = seq.getResidues(i,i+chunkLen);
      pmInput = new PatternMatcherInput(seqString);
      while(matcher.contains(pmInput, pattern)) {
	int matchPos = pmInput.getMatchBeginOffset()+i;
	int matchEnd = pmInput.getMatchEndOffset()+i-1;
	if (reverseComplement && matchPos < 1) {
          matchPos = 1;
        }
        int [] match_positions = new int[2];
        match_positions[0] = matchPos;
        match_positions[1] = matchEnd;
        zones.add(match_positions);
      }
    }
    return zones.size();
  }
  
  /**
   * Returns the number of matches to the query string found in the sequence
   */
  public int getMatchCount() {
    return zones.size();
  }
  
  /**
     Returns a vector of SeqFeatures for the find display
  */
  public Vector getMatches() {
    int match_cnt = zones.size();
    Vector matches = new Vector();
    for (int i = 0; i < match_cnt; i++) {
      int [] match_positions = (int []) zones.elementAt(i);
      int strand = (reverseComplement ? -1 : 1);
      /* This is a hack--to highlight the sequence selection,
         we defined a type "Sequence selection" 
         in the tiers file. This is a hack, and should be removed 
         from the tiers when we come up with a better way of doing 
         sequence selection.
      */
      SeqFeatureI match = new SeqFeature(match_positions[0],
                                         match_positions[1],
                                         "Sequence selection",
                                         strand);
      // The next line lets the matching sequence show up in the table
      match.setRefSequence(sf.getRefSequence());
      matches.add(match);
    }
    return matches;
  }
  
  /**
   * Returns a simple vector of match positions to BaseEditorPanel.
   * We pull apart the "(\d+)-(\d+)" pattern here and just return the match
   * position vector so that BaseEditorPanel doesn't need to know about 
   * ORO and pattern matching and all that good stuff
   */
  public Vector getZones() {
    return zones;
  }

  String revCompRegExp(String inString){
    //first, replace actg appropriately
    char [] charArray = inString.toCharArray();
    for(int i=0;i<charArray.length;i++){
      if(charArray[i]=='a' || charArray[i]=='A')
	charArray[i]='T';
      else if(charArray[i]=='c' || charArray[i]=='C')
	charArray[i]='G';
      else if(charArray[i]=='g' || charArray[i]=='G')
	charArray[i]='C';
      else if(charArray[i]=='t' || charArray[i]=='T')
	charArray[i]='A';
    }
    //now, produce reversed regexp string
    return reversedRegExp(new String(charArray));
  }
  
  String reversedRegExp(String inString){
    //break into regular expression "characters" and reverse the order
    boolean inside_bracket = false;
    int num_cons_backslashes = 0;
    String revString = new String(), starter = "", ender = "";
    char [] charString = inString.toCharArray();
    StringBuffer saveChar = new StringBuffer();
    for(int i=0;i<charString.length;i++){
      if(i == 0){
	    if(charString[i] == '^'){
	      starter = "^";
	      continue;
	    }
      }else if (i == charString.length-1){
	if(charString[i] == '$'){
	  ender = "$";
	  continue;
	}
      }	    
      //deal with anchors
      inside_bracket = inside_bracket || 
	(charString[i] == '[' && (num_cons_backslashes % 2 == 0));
      if(inside_bracket && (num_cons_backslashes % 2 == 0) &&
	 (charString[i]==']'))
	inside_bracket = false;
      
      if(charString[i] == '\\')
	num_cons_backslashes++;
      else
	num_cons_backslashes = 0;
      
      //saveChar.append(String.valueOf(charString[i]));
      if(!inside_bracket && (num_cons_backslashes % 2 == 0)){
	//look for end character
	if(charString[i] == '*' || charString[i] == '+' || 
	   charString[i] == '?' || charString[i] == 'A' ||
	   charString[i] == 'T' || charString[i] == 'G' ||
	   charString[i] == 'C' ||  charString[i] == 'N' ){
	  //revString = saveChar.toString()+charString[i]+revString;
	  revString = charString[i]+saveChar.toString()+revString;
	  saveChar = new StringBuffer();
	}else
	  saveChar.append(String.valueOf(charString[i]));
      }else
	saveChar.append(String.valueOf(charString[i]));
    }
    revString = starter + saveChar.toString()+revString + ender;
    return revString;
  }

   /** clean up a string that was NOT intended as a RegExp search:
       keyValidator should have caught most of these, but in case not,
       consume everything but actg
   */
  String cleanUpNonRegExpString(String in) {
    StringBuffer cleaned = new StringBuffer();
    char [] chars = in.toCharArray();
    for(int index = 0; index < chars.length; index++){
      switch (chars[index]) {
      case '*':
	cleaned.append(".*");
	break;
      case 'a':
      case 'A':
      case 'c':
      case 'C':
      case 'g':
      case 'G':
      case 't':
      case 'T':
      case 'N':
      case 'n':
	cleaned.append(chars[index]);
      default:
	break;
      }
    }
    return cleaned.toString();
  }
  
}


