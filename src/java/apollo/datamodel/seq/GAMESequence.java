package apollo.datamodel.seq;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.datamodel.Range;
import apollo.datamodel.SequenceI;
import apollo.gui.Controller;

/** Currently GAMESequence is not lazy. I think it extends AbstractLazySequence
    because it intends to be lazy at some point. GAMESequence is the main genomic 
    sequence for game data(but not the result sequences) 
    I think the main thing it does is deal with mapping genomic range coords 
    (genomic start and end)  to sequence coords (1 to length). This is done in 
    getResidueFromSourceImpl. I know Sequence does not do this, and as far as I
    can tell, this is the only sequence object that does this. Should Sequence
    do this? or should this be given a more general name? 

so Sequence now does do this (actually has for quite some time) - so there is no
longer a reason for GAMESequence to exist and should probably be vanquished - the 
only possible reason would be if game adapter holds off on sucking in sequences
and then reparses game file on demand - which actually seems silly - a better
scenario would be to have a game file with no sequence and seq is retrieved on
demand from chado db using ChadoLazySeq - or through url? but game comes in pretty
fast so the only reason would be to save on memory

    flybase chado needs to  use this so it should its more general than GAME.
    The main thing is GAMESequence has a non null Range. The non-null Range is not
    enforced, but if Range is not set null pointers will fly. Should it be more 
    flexible and be able to deal with null ranges (null range being one to one mapping)
    Usual place Range 
    gets set is in CurationSet.setRefSequence, where CurationSet sets the sequences
    range to itself. Currently this object is only used for the game adapter, and 
    chado flybase adapters curation sets. Regular Sequence objects are used for 
    FeaturePairs hit seqs. Maybe this should be renamed CurationSetSequence?
*/
public class GAMESequence extends AbstractLazySequence {
  private String range_residues;

  public GAMESequence(String id, Controller c, String dna) {
    super(id,c);
    // need to do setResiudes as it sets the length as well as setting range_residues
    //range_residues = dna;
    setResidues(dna);
  }

  protected CacheSequenceLoader createCacher() {
    return new CacheSeqIdLoader(this);
  }

  public String getResiduesFromSource(int low, int high) {

    String seq = getResiduesFromSourceImpl(low,high);

    return seq;
  }

  // this isnt actually used anywhere
  public SequenceI getSubSequence(int start, int end) {
    /* Changing behavior as it contradicts Sequence's implementation.
     * This implementation expects relative coordinate, while the Sequence expects absolute coordinates (makes more sense).
     * (el)
    Range subLoc = new Range(getRange().getName(),
                             getRange().getStart()+start-1,
                             getRange().getStart()+end-1);
    GAMESequence temp = new GAMESequence(getName(), llco.getController(),
                                         range_residues.substring(start, end));
     */
    Range subLoc = new Range(getRange().getName(), start, end);
    GAMESequence temp = new GAMESequence(getName(), llco.getController(),
        range_residues.substring(start - getRange().getStart(), end - getRange().getStart()));
    temp.setRange (subLoc);
    temp.setLength(subLoc.length());

    return temp;
  }

  /** Whether the end needs adding to, to make it inclusive.
      EnsCGISequence does not need an inclusive end and overrides 
      this to return false. */
  protected boolean needInclusiveEnd() { return true; }
  
  /* Note: low and high are relative coordinates(coords in the range). 
     genomicRange.getStart() is subtracted to transform them into java
     string zero  based coords. */
  protected String getResiduesFromSourceImpl(int low, int high) {
    if (range_residues == null)
      return null;

    int str_low = low - getRange().getStart();
    /* +1 was for the inclusive end i believe
       inclusive end is now centralized in AbstractSequence to alleviate 
       confusion */
    int str_high = high - getRange().getStart();

    // For debugging
    //    if (str_low < 0 || str_high < 0) {
    //	System.out.println("GAMESequence.getResiduesFromSourceImpl: for seq " + getDisplayId() +", low = " + low + ", high = " + high + ", str_low = " + str_low + ", str_high = " + str_high); // DEL
    //    }

    if (str_low == str_high)
      str_high = str_low + 1;

    if (str_high < range_residues.length())
      return range_residues.substring (str_low, str_high);
    else if (str_low < range_residues.length())
      return range_residues.substring (str_low);
    else
      return null;
  }

  public boolean hasSequence() {
    /* Note that this forces whole sequence to load, which we don't want
       if we're doing lazy loading.  When we get lazy loading working with
       GAMESequences, need to turn this off. */
    String residues = getResidues();
    if (residues != null && residues.length() > 1)
      return true;
    else
      return false;
  }

  public void setResidues (String seqString) {
    if (seqString != null && ! seqString.equals("")) {
      this.range_residues = seqString;
      setLength(range_residues.length());
    }
  }
  
  /** Change this to return true when GAMESequence actually becomes lazy */
  public boolean isLazy() { return false; }
}
