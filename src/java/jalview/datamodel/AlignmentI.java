package jalview.datamodel;

import jalview.gui.*;
import java.util.*;

/** Data structure to hold and manipulate a multiple sequence alignment
 */
public interface AlignmentI {

  public int         getHeight() ;
  public int         getWidth() ;
  public int         getMaxIdLength() ;

  public Vector      getSequences();
  public AlignSequenceI   getSequenceAt(int i);

  public void        addSequence(AlignSequenceI seq) ;
  public void        setSequenceAt(int i,AlignSequenceI seq);

  public void        deleteSequence(AlignSequenceI s) ;
  public void        deleteSequence(int i) ;

  public AlignSequenceI[] getColumns(int start, int end) ;
  public AlignSequenceI[] getColumns(int seq1, int seq2, int start, int end) ;

  public void        deleteColumns(int start, int end) ;
  public void        deleteColumns(int seq1, int seq2, int start, int end) ;

  public void        insertColumns(AlignSequenceI[] seqs, int pos) ;

  public AlignSequenceI   findName(String name) ;
  public int         findIndex(AlignSequenceI s) ;

  // Modifying
  public void        trimLeft(int i) ;
  public void        trimRight(int i) ;

  public void        removeGaps() ;
  public Vector      removeRedundancy(float threshold, Vector sel) ;


  // Grouping methods
  public SequenceGroup findGroup(int i) ;
  public SequenceGroup findGroup(AlignSequenceI s) ;
  public void          addToGroup(SequenceGroup g, AlignSequenceI s) ;
  public void          removeFromGroup(SequenceGroup g,AlignSequenceI s) ;
  public void          addGroup(SequenceGroup sg) ;
  public SequenceGroup addGroup() ;
  public void          deleteGroup(SequenceGroup g) ;
  public Vector        getGroups();
  public void          setGroups(Vector groups);

  public Hashtable[]   removeArrayElement(Hashtable[] cons, int i) ;
  public void          removeIntArrayColumn(int[][] cons2, int start) ;

  // Sorting
  public void          sortGroups() ;
  public void          sortByPID(AlignSequenceI s) ;
  public void          sortByID() ;


  // Conservation
  public void          percentIdentity(Vector sel) ;
  public void          percentIdentity2(int start, int end, Vector sel) ;
  public void          percentIdentity2() ;
  public void          percentIdentity2(int start, int end) ;
  public void          percentIdentity(int start, int end, Vector sel) ;
  public void          percentIdentity() ;
    //  public void          findQuality() ;
    //public void          findQuality(int start, int end) ;

  public void          setGapCharacter(String gc);
  public String        getGapCharacter();

    public Vector        getAAFrequency();
}



