package apollo.datamodel;

import java.util.Vector;
import java.io.IOException;
import java.lang.String;

import org.bdgp.util.DNAUtils;

/** Update this to take to use Range - used to ignore Range and use residueOffset,
 which was redundat with using Range. I believe this makes GAMESequence pase as the
 main function of GAMESequence was to be Range sensitive. If no Range then 0 offset */

public class Sequence extends AbstractSequence implements SequenceI {
  private char [] dna_chars;
  // make these protected so subclasses in other packages can get them
  protected String residues; // -> AbstractSequence?
  // Suzi took out setResidueOffset as its redudant with setRange - this var needs to 
  //   be replaced by getRange().getStart() (0 if no range) 
//private int    residueOffset  = 0;

  // residues can be peptide or dna
  public Sequence(String id, String residues) {
    super(id);
    setResidues(residues);
  }

  public int       getLength() {
    if (residues != null && ! residues.equals(""))
      return residues.length();
    else
      return length;
  }

  public char getBaseAt(int loc) {
    //return residues.charAt(loc - residueOffset);
    int i = residues.length();
    String res = this.residues;
    return residues.charAt(translateRangeToStringCoords(loc));
  }

  // According to John Richter, writeObject and readObject are custom
  // methods for serializing Sequence objects for strings >64K, but these
  // shouldn't be needed anymore with JDK 1.3 or higher.
  // However, since it works as is, I'm inclined to leave it alone.
  private void writeObject(java.io.ObjectOutputStream out)
  throws IOException {
    String dna_temp = this.residues;
    this.residues = null;
    if (dna_temp != null) {
      dna_chars = new char[dna_temp.length()];
      dna_temp.getChars(0, dna_temp.length(), dna_chars, 0);
    }

    out.defaultWriteObject();

    dna_chars = null;
    this.residues = dna_temp;
  }

  private void readObject(java.io.ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (dna_chars != null)
      setResidues (new String(dna_chars));

    dna_chars = null;
  }

  /** this method actually isnt used at all - take out of SequenceI?
   calls AbstractSequence.getResidues which calls getResiduesImpl */
  
  public SequenceI    getSubSequence  (int start, int end) {
    return new Sequence(getName(), getResidues(start,end));
  }

  public void      setResidues  (String seqString) {
    if (seqString != null) { // && ! seqString.equals("")) { "" for cleared res
      /* much of the code relies upon the DNA residues being in
         upper-case. Easiest to catch it here for most all of 
         the cases and ensure that it is Upper case */
      this.residues = seqString.toUpperCase();
      length = residues.length();
    }
  }

  public void clearResidues() {
    residues = "";
    length = 0;
  }

  public String getResidues () {
    return residues;
  }

  
  /** this does a plus one to the end which i assume is to be inclucsive,
      the only problem i think is AbstractSequence.getResudes calls pegLimits
      which sutracts one from start to convert from one based to zero based,
      but one is not subtracted from the end which i think is also an act of 
      inclusivity. By inclusivity i mean that java substring includes the 
      start but not end, the apollo standard is to include both.
      This seems to accomodated for twice and is thus adding an extra base.
      Am I right about this? */
  protected String getResiduesImpl(int rangeStart, int rangeEnd) {
    if (residues != null && residues.length() > 0 && rangeStart >= 0 && rangeEnd >= 0) {
      int substringStart = translateRangeToStringCoords(rangeStart);
      int substringEnd = translateRangeToStringCoords(rangeEnd);//end - rangeStart;
      if (substringStart > substringEnd) {
        int temp = substringStart;
        substringStart = substringEnd;
        substringEnd = temp;
      }
      // prevents StringIndexOutOfBoundsException
      if (substringEnd > residues.length())
        substringEnd = residues.length();
      return residues.substring(substringStart, substringEnd);
    } else {
      return "";
    }
  }

  /** rangeCoord is a chromosomal/genomic (or what not) coord. Its in the 
      coordinates of the entry. String/seq coords start at 0. */
  private int translateRangeToStringCoords(int rangeCoord) {
    if (!hasRange()) 
      return rangeCoord; // this doesn't seem like a good thing to do?
    int start = getRange().getStart();
    int coord = Math.abs(rangeCoord - getRange().getStart());
    int c = coord;
    if (this.residue_type == this.AA) {
      coord = coord / 3;
    }
    
    if (coord >= residues.length()) {
      coord = residues.length()-1;
    } else if (coord < 0) {
      coord = 0;
    }
    
    return coord; // RAY: start will be higher for reverse sequences
  }

  protected String getResiduesImpl(int start) {
    if (residues != null)
      //return residues.substring(start - residueOffset);
      return residues.substring(translateRangeToStringCoords(start));
    else
      return "";
  }

  /** Override if need to clean up dangling references, 
      like removing as listener. (AbstractLazySequence uses this */
//  public void cleanup() {}

}
