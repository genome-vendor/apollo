package apollo.datamodel;

import java.util.HashMap;
import java.util.Vector;
import java.util.Date;
import java.util.Hashtable;

public interface SequenceI extends java.io.Serializable {

  public int       getLength();
  public void      setLength(int length);

  public String    getResidues  ();
  public String    getResidues  (int start, int end);
  public void      setResidues (String residues);
  /** clears out residues (set to empty string) */
  public void clearResidues();
  /** This will return false if there are no residues (null || "", cleared). */
  public boolean hasResidues();

  /* One of the reasons for the vague term residues
     was so that we could support sequences of alternate types, e.g.
     peptides. To do this we need methods to set/get the residue type
  */
  public static final String AA = "AA";
  public static final String DNA = "DNA";
  public static final String RNA = "RNA";

  /** These are SO terms (under general family of sequence_variation) 
   changed insertion to nucleotide_insertion as its more specific and i
   believe thats what we are doing here, same with deletion
   eventually should come from SO (should this be in tiers file?)
  */
  public static final String INSERTION = "nucleotide_insertion";
  public static final String DELETION = "nucleotide_deletion";
  public static final String SUBSTITUTION = "substitution";
  /** I question if CLEAR_EDIT shouold be a separate type - its just a deletion
      of the above types - refactor? */
  public static final String CLEAR_EDIT = "NOP";

  public String  getResidueType ();
  public void    setResidueType (String res_type);
  /** Calculates residue type  by comparing seq length to feat length.
      If there is more than 2 base pairs per residue than its assigned AA. */
  public boolean hasResidueType();
  /** Return true if getResidueType() == AA */
  public boolean isAA();

  /** I can see this being a handy method but its actually not used at all - delete?*/
  public SequenceI getSubSequence (int start, int end);

  public String    getName  ();
  public void      setName  (String id);
  public boolean   hasName();

  public String    getAccessionNo();
  public void      setAccessionNo(String id);

  public void      addDbXref(String db, String id, int isCurrent);
  public void      addDbXref(String db, String id);
  public void      addDbXref(DbXref xref);
  public Vector    getDbXrefs();

  public String    getChecksum();
  public void      setChecksum(String checksum);

  public char      getBaseAt(int loc);

  public String    getDescription();
  public void      setDescription(String desc);

  public String    getReverseComplement();

  public boolean   isSequenceAvailable(long position);
  public int       getFrame(long position, boolean forward);

  public boolean   usesGenomicCoords();
  public void      setRange (RangeI loc);
  public RangeI    getRange ();

  /** Cleanup dangling references when SequenceI not needed anymore. Is this funny to
      have in SequenceI? (needed for AbstractLazySequence) */
//  public void cleanup();

  public boolean isLazy();

  public HashMap getGenomicErrors();
  public boolean isSequencingErrorPosition(int base_position);
  public SequenceEdit getSequencingErrorAtPosition(int base_position);
  public boolean addSequencingErrorPosition(String operation, 
                                            int pos, 
                                            String residue);
  public boolean addSequenceEdit(SequenceEdit seq_edit);
  public boolean removeSequenceEdit(SequenceEdit seqEdit);

  /* It is much more logical to attach the organism to the sequence
     because it is not an interval on a sequence that is associated
     (its origins) with an organism, but the sequence itself is the
     sequence of a particular organism */
  public String getOrganism();
  public void setOrganism(String organism);

  public void setDate(Date update_date);

  /* Sequences (such as peptides) need to be able to have properties.
     The methods for dealing with properties (addProperty etc.) are duplicated in several
     other classes--should we break them out as a separate class or something? */
//   public void addProperty(String key, String value);
//   public void removeProperty(String key);
//   public void replaceProperty(String key, String value);
//   public String getProperty(String key);
//   public Vector getPropertyMulti(String key);
//   public Hashtable getProperties();
  // This is not used by classes that use SequenceI, so hopefully we can
  // get away without it here (because if it's here, then all the classes
  // that implement SequenceI need to have a getPropertiesMulti method).
  //  public Hashtable getPropertiesMulti();

  /** 10/2005: Sequences now can have synonyms.  Synonym methods copied from Identifer.
      Could go in a separate class, but not sure it's worth the trouble. */
//  public Vector getSynonyms();
//  public Vector getSynonyms(boolean excludeInternalSynonyms);
//  public void addSynonym(Synonym syn);
  // Trying to stick to the minimum set of needed synonym methods.  Add more here
  // if needed.
}

