package jalview.datamodel;

import jalview.gui.*;
import jalview.gui.schemes.*;
import jalview.analysis.*;
import jalview.util.*;
import java.util.*;

/** Data structure to hold and manipulate a multiple sequence alignment
 */
public class Alignment implements AlignmentI {

  protected Vector      sequences;
  protected int[][]     scores;
  protected Vector      groups = new Vector();
  public    Hashtable[] cons;
  public    int[][]     cons2;
  protected String      gapCharacter = "-";
  protected Vector      quality;
  protected Vector      aaFrequency;
  protected boolean    aaFrequencyValid = false;

  /** Make an alignment from an array of Sequences.
  * 
  * @param sequences 
  */
  public Alignment(AlignSequenceI[] seqs) {
    this();

    for (int i=0; i < seqs.length; i++) {
      sequences.addElement(seqs[i]);
    }

    groups.addElement(new SequenceGroup());

    int i = 0;

    while (i < seqs.length) {
      addToGroup((SequenceGroup)groups.elementAt(0),seqs[i]);
      i++;
    }
  }

  public Alignment() {
    sequences = new Vector();
  }

  public Vector      getSequences() {
    return sequences;
  }

  public AlignSequenceI getSequenceAt(int i) {
    if (i < sequences.size()) {
      return (AlignSequenceI)sequences.elementAt(i);
    }

    return null;
  }

  /** Adds a sequence to the alignment.  Recalculates maxLength and size.
   * Should put the new sequence in a sequence group!!!
   * 
   * @param snew 
   */
  public void addSequence(AlignSequenceI snew) {
    sequences.addElement(snew);

    ((SequenceGroup)groups.lastElement()).addSequence(snew);
  }

  public void addSequence(AlignSequenceI[] seq) {
    for (int i=0; i < seq.length; i++) {
      addSequence(seq[i]);
    }
  }

  /** Adds a sequence to the alignment.  Recalculates maxLength and size.
   * Should put the new sequence in a sequence group!!!
   * 
   * @param snew 
   */
  public void setSequenceAt(int i, AlignSequenceI snew) {
    AlignSequenceI oldseq = getSequenceAt(i);
    deleteSequence(oldseq);

    sequences.setElementAt(snew,i);

    ((SequenceGroup)groups.lastElement()).addSequence(snew);
  }

  public Vector getGroups() {
    return groups;
  }

  public void setGroups(Vector groups) {
    this.groups = groups;
  }

  /** Sorts the sequences by sequence group size - largest to smallest.
   * Uses QuickSort.
   */
  public void sortGroups() {
    float[]  arr = new float [groups.size()];
    Object[] s   = new Object[groups.size()];

    for (int i=0; i < groups.size(); i++) {
      arr[i] = ((SequenceGroup)groups.elementAt(i)).sequences.size();
      s[i]   = groups.elementAt(i);
    }

    QuickSort.sort(arr,s);

    Vector newg = new Vector(groups.size());

    for (int i=groups.size()-1; i >= 0; i--) {
      newg.addElement(s[i]);
    }

    groups = newg;
  }

  /** Takes out columns consisting entirely of gaps (-,.," ")
   */
  public void removeGaps() {
    for (int i=0; i < getWidth();i++) {
      boolean flag = true;

      for (int j=0; j < getHeight(); j++) {
        if (getSequenceAt(j).getLength() > i) {
	    /* MC Should move this to a method somewhere */
	  if (getSequenceAt(j).residueIsSpacer(i)) {
            flag = false;
          }
        }
      }
      if (flag == true) {
        System.out.println("Deleting column " + i);
        deleteColumns(i+1,i+1);
      }
    }
  }

  /** Returns an array of Sequences containing columns
   * start to end (inclusive) only.
   * 
   * @param start start column to fetch
   * @param end end column to fetch
   * @return Array of Sequences, ready to put into a new Alignment
   */
  public AlignSequenceI[] getColumns(int start, int end) {
    return getColumns(0,getHeight()-1,start,end);
  }

  /** Removes a range of columns (start to end inclusive).
   * 
   * @param start Start column in the alignment
   * @param end End column in the alignment
   */
  public void deleteColumns(int start, int end) {
    if (quality != null) {
      if (end > quality.size()) {
        end = quality.size();
      }
      for (int i=start; i <= end; i++) {
        quality.removeElementAt(start);
      }
    } else {
      if (Config.DEBUG) System.out.println("NOTE: Quality not in alignment");
    }
    deleteColumns(0,getHeight()-1,start,end);
  }

  /**
   * @param i 
   * @param cons 
   */
  public Hashtable[] removeArrayElement(Hashtable[] cons, int i) {
    if (cons != null && cons.length > i) {
      Hashtable[] newhash = new Hashtable[cons.length-1];
      for (int j = 0; j < i; j++) {
        newhash[j] = cons[j];
      }

      for (int j=i+1;j < newhash.length; j++) {
        newhash[j] = cons[j+1];
      }
      return newhash;
    }
    return null;
  }

  /**    */
  public void removeIntArrayColumn(int[][] cons2, int start) {
    int length    = getWidth();
    int cons2leng = cons2.length;

    for (int k=start; k < (cons2leng-1); k++) {
      cons2[k] = cons2[k+1];
    }

  }

  /**    */
  public void deleteColumns(int seq1, int seq2, int start, int end) {

    for (int i=0; i <= (end-start); i++) {
      if (cons != null) {
        cons = removeArrayElement(cons,start);
        if (cons2 != null) {
          removeIntArrayColumn(cons2,start);
        } else {
          System.out.println("NOTE: cons2 not in alignment");
        }
      } else {
        if (Config.DEBUG) System.out.println("NOTE: cons not in alignment");
      }

      for (int j=seq1; j <= seq2; j++) {
        getSequenceAt(j).deleteCharAt(start);
      }
    }

    aaFrequencyValid = false;
  }

  /**    */
  public void insertColumns(AlignSequenceI[] seqs, int pos) {
    if (seqs.length == getHeight()) {
      for (int i=0; i < getHeight();i++) {
        String tmp = new String(getSequenceAt(i).getResidues());
        getSequenceAt(i).setResidues(tmp.substring(0,pos) + 
				     seqs[i].getResidues() + 
				     tmp.substring(pos));
      }

    }
    aaFrequencyValid = false;
  }

  /**    */
  public AlignSequenceI[] getColumns(int seq1, int seq2, int start, int end) {
    AlignSequenceI[] seqs = new AlignSequence[(seq2-seq1)+1];
    for (int i=seq1; i<= seq2; i++ ) {
      seqs[i] = new AlignSequence(getSequenceAt(i).getName(),
                             getSequenceAt(i).getResidues(start,end),
                             getSequenceAt(i).findPosition(start),
                             getSequenceAt(i).findPosition(end));
    }
    return seqs;
  }
  /**    */
  public void trimLeft(int i) {
    for (int j = 0;j< getHeight();j++) {

      AlignSequenceI s        = getSequenceAt(j);
      int       newstart = s.findPosition(i);

      s.setStart(newstart);
      s.setResidues(s.getResidues().substring(i));
      s.setNums(s.getResidues());
    }
    if (cons != null) {
      int length = cons.length;
      for (int k=0; k < (length-i); k++) {
        cons[k] = cons[i+k];
      }
    }
    if (cons2 != null) {
      int length = getWidth();
      for (int k=0; k < (length-i); k++) {
        cons2[k] = cons2[i+k];
      }
    }

    aaFrequencyValid = false;

  }
  /**    */
  public void  trimRight(int i) {
    for (int j = 0;j< getHeight();j++) {
      AlignSequenceI s      = getSequenceAt(j);
      int       newend = s.findPosition(i);

      s.setEnd(newend);
      s.setResidues(s.getResidues().substring(0,i+1));
    }
    if (cons != null) {
      int         length = cons.length;
      Hashtable[] tmp    = new Hashtable[i+1];

      for (int k=0; k <= i; k++) {
        tmp[k] = cons[i];
      }
    }

    aaFrequencyValid = false;
  }

  /**    */
  public void deleteSequence(AlignSequenceI s) {
    for (int i=0; i < getHeight(); i++) {
      if (getSequenceAt(i) == s) {
        deleteSequence(i);
      }
    }
  }
  /**    */
  public void  deleteSequence(int i) {
    sequences.removeElementAt(i);
    aaFrequencyValid = false;
  }

  /**    */
  public Vector removeRedundancy(float threshold, Vector sel) {
    Vector del = new Vector();

    for (int i=1; i < sel.size(); i++) {
      for (int j = 0; j < i; j++) {
        // Only do the comparison if either have not been deleted
        if (!del.contains((AlignSequenceI)sel.elementAt(i)) ||
            !del.contains((AlignSequenceI)sel.elementAt(j))) {

          float pid = Comparison.compare((AlignSequenceI)sel.elementAt(j),
                                         (AlignSequenceI)sel.elementAt(i));

          if (pid >= threshold) {
            // Delete the shortest one
            if (((AlignSequenceI)sel.elementAt(j)).getLength() >
                ((AlignSequenceI)sel.elementAt(i)).getLength()) {
              del.addElement(sel.elementAt(i));
              System.out.println("Deleting sequence " + ((AlignSequenceI)sel.elementAt(i)).getName());
            } else {
              del.addElement(sel.elementAt(i));
              System.out.println("Deleting sequence " + ((AlignSequenceI)sel.elementAt(i)).getName());
            }
          }
        }
      }
    }

    // Now delete the sequences
    for (int i=0; i < del.size(); i++) {
      System.out.println("Deleting sequence " + ((AlignSequenceI)del.elementAt(i)).getName());
      deleteSequence((AlignSequenceI)del.elementAt(i));
    }

    return del;
  }

  /**    */

  /**    */
  public void sortByPID(AlignSequenceI s) {

    float     scores[] = new float[getHeight()];
    AlignSequenceI seqs[]   = new AlignSequenceI[getHeight()];

    for (int i = 0; i < getHeight(); i++) {
      scores[i] = Comparison.compare(getSequenceAt(i),s);
      seqs[i]   = getSequenceAt(i);
    }

    QuickSort.sort(scores,0,scores.length-1,seqs);

    int len = 0;
    if (getHeight()%2 == 0) {
      len = getHeight()/2;
    } else {
      len = (getHeight()+1)/2;
    }

    for (int i = 0; i < len; i++) {
      AlignSequenceI tmp = seqs[i];
      sequences.setElementAt(seqs[getHeight()-i-1],i);
      sequences.setElementAt(tmp,getHeight()-i-1);
    }
  }

  /**    */
  public void sortByID() {
    String    ids[]   = new String[getHeight()];
    AlignSequenceI seqs[]  = new AlignSequenceI[getHeight()];

    for (int i = 0; i < getHeight(); i++) {
      ids[i]  = getSequenceAt(i).getName();
      seqs[i] = getSequenceAt(i);
    }

    QuickSort.sort(ids,seqs);

    int len = 0;

    if (getHeight()%2 == 0) {
      len = getHeight()/2;
    } else {
      len = (getHeight()+1)/2;
      System.out.println("Sort len is odd = " + len);
    }
    for (int i = 0; i < len; i++) {
      System.out.println("Swapping " + seqs[i].getName() + " and " + seqs[getHeight()-i-1].getName());
      AlignSequenceI tmp = seqs[i];
      sequences.setElementAt(seqs[getHeight()-i-1],i);
      sequences.setElementAt(tmp,getHeight()-i-1);
    }
  }

  /**    */
  public SequenceGroup findGroup(int i) {
    return findGroup(getSequenceAt(i));
  }

  /**    */
  public SequenceGroup findGroup(AlignSequenceI s) {
    for (int i = 0; i < this.groups.size();i++) {
      SequenceGroup sg = (SequenceGroup)groups.elementAt(i);
      if (sg.sequences.contains(s)) {
        return sg;
      } else {
	for (int j = 0; j < sg.sequences.size(); j++) {
	  DrawableSequence seq = (DrawableSequence)sg.sequences.elementAt(j);

	  if (seq.getSequenceObj() == s) {
	    return sg;
	  }
	}
      }
    }
    return null;

  }
  /**    */
  public void addToGroup(SequenceGroup g, AlignSequenceI s) {
    if (!(g.sequences.contains(s))) {
      g.sequences.addElement(s);
    }
  }
  /**    */
  public void removeFromGroup(SequenceGroup g,AlignSequenceI s) {
    if (g != null && g.sequences != null) {
      if (g.sequences.contains(s)) {
        g.sequences.removeElement(s);
        if (g.sequences.size() == 0) {
          groups.removeElement(g);
        }
      }
    }
  }

  /**    */
  public void addGroup(SequenceGroup sg) {
    groups.addElement(sg);

    for (int i = 0; i < sg.getSize(); i++) {

      AlignSequenceI seq = (AlignSequenceI)sg.getSequenceAt(i);
      sequences.add(seq);
    }
  }

  /**    */
  public SequenceGroup addGroup() {
    SequenceGroup sg = new SequenceGroup();
    groups.addElement(sg);
    return sg;
  }

  /**    */
  public void deleteGroup(SequenceGroup g) {
    if (groups.contains(g)) {
      groups.removeElement(g);
    }
  }

  /**    */
  public AlignSequenceI findName(String name) {
    int i = 0;
    while (i < sequences.size()) {
      AlignSequenceI s = getSequenceAt(i);
      if (s.getName().equals(name)) {
        return s;
      }
      i++;
    }
    return null;
  }

  /**    */
  public int findIndex(AlignSequenceI s) {
    int i=0;
    while (i < sequences.size()) {
      if (s == getSequenceAt(i)) {
        return i;
      }
      i++;
    }
    return -1;
  }
  /**    */
  public int getHeight() {
    return sequences.size();
  }


  /**    */
  public int getWidth() {
    int maxLength = -1;
    for (int i = 0; i < sequences.size(); i++) {
      if (getSequenceAt(i).getLength() > maxLength) {
        maxLength = getSequenceAt(i).getLength();
      }
    }

    for (int i = 0; i < sequences.size(); i++) {
      for (int j = getSequenceAt(i).getLength(); j < maxLength; j++) {
        getSequenceAt(i).insertCharAt(j,gapCharacter.charAt(0));
      }
    }

    return maxLength;
  }

  /**    */
  public int getMaxIdLength() {
    int max = 0;
    int i   = 0;

    while (i < sequences.size()) {
      AlignSequenceI seq = getSequenceAt(i);
      String    tmp = seq.getName() + "/" + seq.getStart() + "-" + seq.getEnd();

      if (tmp.length() > max) {
        max = tmp.length();
      }

      i++;
    }
    return max;
  }

  /**    */
  public void  percentIdentity(Vector sel) {
    int max = getWidth();

    cons  = new Hashtable[max];
    cons2 = new int[max][24];

  }


  /**    */
  public void percentIdentity2(int start, int end, Vector sel) {}
  /**    */
  public void percentIdentity2() {
    percentIdentity2(0,getWidth()-1);
  }
  /**    */
  public void percentIdentity2(int start, int end) {}
  /**    */
  public void percentIdentity(int start, int end, Vector sel) {}
  /**    */
  public void  percentIdentity() {}
  /**   
  public void findQuality() {
    findQuality(0,getWidth()-1);
  }
  */
  /**    */
  public void findQuality(int start, int end) {}


  public void setGapCharacter(String gc) {
    gapCharacter = gc;
  }

  public String getGapCharacter() {
    return gapCharacter;
  }

  public Vector getAAFrequency() {

    if (aaFrequency == null || aaFrequencyValid == false) {

      Vector seqs = new Vector();

      seqs.addElement(sequences.elementAt(0));

      aaFrequency = AAFrequency.calculate(seqs,1,getWidth());

      //AAFrequency.print(aaFrequency);

      aaFrequencyValid = true;
    }
    return aaFrequency;
  }
}








