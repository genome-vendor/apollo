package apollo.dataadapter.chado.jdbc;

import apollo.config.ApolloNameAdapterI;
import apollo.config.GmodNameAdapter;

import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.Region;

import apollo.dataadapter.chado.jdbc.ChadoFeature;

import apollo.datamodel.CurationSet;
import apollo.datamodel.DbXref;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Sequence;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Transcript;
import apollo.datamodel.Synonym;
import apollo.datamodel.Comment;
import apollo.datamodel.SequenceEdit;

import java.util.TreeSet;
import java.util.Vector;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.log4j.*;

/** This class handles writing back to a Chado instance.  It primarily uses
 *  the PureJDBCTransactionWriter implementation, but instead of writing
 *  based on transactions (which have been revamped to support undo)
 *  it writes based on a diff system.
 */
public class JdbcChadoWriter extends PureJDBCTransactionWriter {

  protected final static Logger logger = LogManager.getLogger(JdbcChadoWriter.class);

  private ChadoInstance chadoInstance;
  private SimpleDateFormat dateFormat;
  private CurationSet cs;

  private String gffSource;

  /** Constructor
   *
   *  @param adapter - ChadoAdapter to use for writing changes
   *  @param jdbcAdapter - JdbcChadoAdapter to use for writing changes
   *  @param copyOnWrite - Whether to keep a copy of the old object(s) when writing changes
   *  @param noCommit - Whether to run in 'no commit' mode
   *  @param useCDS - Whether to generate CDS features
   *  @param chadoInstance - ChadoInstance representing this Chado database`a
   */
  public JdbcChadoWriter(ChadoAdapter adapter, JdbcChadoAdapter jdbcAdapter, boolean copyOnWrite,
      boolean noCommit, boolean useCDS, ChadoInstance chadoInstance)
  {
    super(adapter, jdbcAdapter, copyOnWrite, noCommit, useCDS);
    this.chadoInstance = chadoInstance;
    dateFormat = new SimpleDateFormat("EEE MMM d k:m:s zzzzzzzzz yyyy");
  }

  /** Write the data to the database
   *
   *  @param cs - CurationSet containing annotations to write
   */
  public void write(CurationSet cs)
  {
    this.cs = cs;
    boolean getFeatProps = true;
    boolean getSynonyms = true;
    boolean getDbXRefs = true;    
    SequenceI refSeq = cs.getRefSequence();
    SequenceI origSeq = new Sequence(refSeq.getName(), "");
    String seqType = cs.getAssemblyType();
    if(seqType == null)
      seqType = "chromosome";

    StrandedFeatureSetI currentAnnots = cs.getAnnots();
    StrandedFeatureSet originalAnnots = new StrandedFeatureSet();
    Region region = null;
    try {
      region = new Region(refSeq.getName());
    }
    catch (RuntimeException e) {
      region = new Region(String.format("%s:%d-%d", refSeq.getName(),
          cs.getLow(), cs.getHigh()));
    }
    long seqFeatId = jdbcAdapter.getFeatureId(jdbcAdapter.getConnection(),
        seqType, region.getChromosome());
    FeatureLocImplementation featLoc = new ChromosomeFeatureLocImp(seqFeatId, region);
    CurationSet origCs = new CurationSet(null, originalAnnots, region.toString());
    origCs.setRefSequence(origSeq);
    jdbcAdapter.addOneLevelAnnotations(jdbcAdapter.getConnection(), origSeq,
        originalAnnots, featLoc, getFeatProps, getSynonyms, getDbXRefs);
    jdbcAdapter.addAnnotationModels(jdbcAdapter.getConnection(), origSeq,
        originalAnnots, featLoc, getFeatProps, getSynonyms, getDbXRefs,
        chadoInstance.getThreeLevelAnnotTypes());
    beginTransaction();    
    processFeatures(currentAnnots.getForwardSet().getFeatures(),
        originalAnnots.getForwardSet().getFeatures(), refSeq);
    processFeatures(currentAnnots.getReverseSet().getFeatures(),
        originalAnnots.getReverseSet().getFeatures(), refSeq);
    processSequenceEdits(refSeq, origSeq);
    commitTransaction();
    this.cs = null;
  }
  
  public String getGFFSource()
  {
    return gffSource;
  }
  
  public void setGFFSource(String gffSource)
  {
    this.gffSource = gffSource;
  }

  /** Process features to be written.  It diffs the annotations for determining whether
   *   to add, deleted, or update a given annotation.
   *
   *  @param annots1 - Vector containing current annotations
   *  @param annots2 - Vector containing annotations stored in database
   *  @param refSeq - SequenceI object representing genomic sequence
   */
  private void processFeatures(Vector annots1, Vector annots2, SequenceI refSeq)
  {

    if (annots1.size() == 0 && annots2.size() == 0) {
      return;
    }

    Map<String, AnnotatedFeatureI> nodes1 = new HashMap<String, AnnotatedFeatureI>();
    Map<String, AnnotatedFeatureI> nodes2 = new HashMap<String, AnnotatedFeatureI>();

    for (Object o : annots1) {
      AnnotatedFeatureI annot = (AnnotatedFeatureI)o;
      if (refSeq.getRange().contains(annot)) {
        nodes1.put(annot.getId(), annot);
      }
    }
    for (Object o : annots2) {
      AnnotatedFeatureI annot = (AnnotatedFeatureI)o;
      if (refSeq.getRange().contains(annot)) {
        nodes2.put(annot.getId(), annot);
      }
    }

    for(Object o : nodes2.values()) {
      AnnotatedFeatureI annot = (AnnotatedFeatureI)o;
      //delete annot
      if (!nodes1.containsKey(annot.getId())) {
        deleteAnnot(annot);
      }
    }

    for(Object o : nodes1.values()) {
      AnnotatedFeatureI annot1 = (AnnotatedFeatureI)o;
      AnnotatedFeatureI annot2 = (AnnotatedFeatureI)nodes2.get(annot1.getId());

      // add dbxref if GFF_source is defined from the command line
      DbXref gffSourceXref = null;
      if (annot1.isAnnotTop() && getGFFSource() != null) {
        gffSourceXref = new DbXref("GFF_source", getGFFSource(), "GFF_source");
        gffSourceXref.setVersion("");
        gffSourceXref.setCurrent(1);
        addDbXrefToSelfAndDescendants(annot1, gffSourceXref);
        //annot1.addDbXref(gffSourceXref);
      }
      
      //add annot
      if (annot2 == null) {
        addAnnot(annot1);
      }
      else {
        //update annot
        if (!isSameRange(annot1, annot2)) {
          updateAnnotLocation(annot1, annot2);
        }
        if(!annot1.getFeatureType().equals(annot2.getFeatureType()))
          updateAnnotType(annot1,annot2);
	
        if(!annot1.getName().equals(annot2.getName())) {
          updateAnnotName(annot1, annot2);
        }
        updateCds(annot1, annot2);
        /*
        else {
          System.out.println(annot1 + " is identical");
        }
        */
        updateSynonyms(annot1, annot2);
        updateComments(annot1, annot2);
        updateStatus(annot1, annot2);
        updateOwner(annot1, annot2);
        updateDbXrefs(annot1, annot2);
        processFeatures(annot1.getFeatures(), annot2.getFeatures(), refSeq);
      }
    }

  }
  
  /** Add dbxref to feature and all of its descendants
   * 
   * @param feat - ancestor feature
   * @param dbxref - dbxref to add to feature and its descendants
   */
  private void addDbXrefToSelfAndDescendants(SeqFeatureI feat, DbXref dbxref)
  {
    feat.addDbXref(dbxref);
    for (Object o : feat.getFeatures()) {
      addDbXrefToSelfAndDescendants((SeqFeatureI)o, dbxref);
    }
  }

  /** Checks to see whether two SeqFeatureI objects are identical.  Checks to make sure
   *  that they are the same range.
   *
   *  @param feat1 - First SeqFeatureI object being compared.
   *  @param feat2 - Second SeqFeatureI object being compared.
   *  @return true if both SeqFeatureI objects are identical
   */
  private boolean isSameRange(SeqFeatureI feat1, SeqFeatureI feat2)
  {
    if (!feat1.sameRange(feat2)) {
      return false;
    }
    if (feat1 instanceof AnnotatedFeatureI &&
        feat2 instanceof AnnotatedFeatureI) {
      AnnotatedFeatureI af1 = (AnnotatedFeatureI)feat1;
      AnnotatedFeatureI af2 = (AnnotatedFeatureI)feat2;
      if (af1.getTranslationStart() != af2.getTranslationStart() ||
          af1.getTranslationEnd() != af2.getTranslationEnd()) {
        return false;
      }
    }
    return true;
  }

  /** Add an annotation to the database.
   *
   *  @param annot - AnnotatedFeatureI object to be added
   */
  private void addAnnot(AnnotatedFeatureI annot)
  {
    //System.out.println("addAnnot: " + annot);
    SeqFeatureI parent = annot.getParent();
    AnnotatedFeatureI topLevelAnnot = annot;
    while (!topLevelAnnot.isAnnotTop()) {
      topLevelAnnot = (AnnotatedFeatureI)topLevelAnnot.getParent();
    }
    String owner = getOwner(topLevelAnnot);
    Date date = getDate(topLevelAnnot);
    if (annot.isAnnotTop()) {
      //3-tiered top level annotation
      if (annot.getFeatures().size() > 0) {
        addGene(annot, owner, date, null, true);
      }
      //1-tiered top level annotation
      else {
        addTopLevelAnnot(annot, owner, date, null);
      }
    }
    else {
      Long parentId = getChadoIdForFeature(parent);
      ChadoFeatureLoc parentLoc = new ChadoFeatureLoc(parentId,
          jdbcAdapter.getConnectionUsedForLastTransaction());
      if (annot.isTranscript()) {
        addTranscript((Transcript)annot, owner, date,
            parentId, getRank(annot), parentLoc.getSourceFeature(), true);
      }
      else if (annot.isExon()) {
        addExon(annot, owner, date, parentId,
            getRank(annot), parentLoc.getSourceFeature(), true);
      }
    }
  }

  /** Delete an annotation from the database.
   *
   *  @param annot - AnnotatedFeatureI object to be deleted
   */
  private void deleteAnnot(AnnotatedFeatureI annot)
  {
    //System.out.println("deleteAnnot: " + annot.getId());
    long annotId = getChadoFeatureId(annot);
    if (annot.isAnnotTop()) {
      deleteGene(annotId);
    }
    else if (annot.isTranscript()) {
      deleteTranscript(annotId, true);
    }
    else if (annot.isExon()) {
      long transcriptId = getChadoFeatureId(annot.getParent());
      deleteExon(annotId, transcriptId, true);
    }
  }

  /** Update an annotation location from the database.
   *
   *  @param annot1 - AnnotatedFeatureI for the new annotation (to be updated to)
   *  @param annot2 - AnnotatedFeatureI for the old annotation (to be updated from)
   */
  private void updateAnnotLocation(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    //System.out.println("updateAnnot: " + annot1);
    long id = getChadoFeatureId(annot1);
    ChadoFeatureLoc newLoc = new ChadoFeatureLoc(annot1, null,
        jdbcAdapter.getConnectionUsedForLastTransaction());
    if (annot1.getLow() != annot2.getLow() ||
        annot1.getHigh() != annot2.getHigh()) {
      if (!updateFeatureLocation(id, annot2, newLoc)) {
        logger.error("Error updating " + annot1);
      }
    }
  }

  /** Update an AnnotatedFeature's CDS from the database (if instance of Transcript).
  *
  *  @param annot1 - AnnotatedFeatureI for the new transcript (to be updated to)
  *  @param annot2 - AnnotatedFeatureI for the old transcript (to be updated from)
  */
 private void updateCds(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
 {
   if (!(annot1 instanceof Transcript) || !(annot2 instanceof Transcript)) {
     return;
   }
   Transcript t1 = (Transcript)annot1;
   Transcript t2 = (Transcript)annot2;
   if (t1.getTranslationStart() != t2.getTranslationStart()) {
     long id = getChadoFeatureId(t1);
     if (!updateCdsFeatureLocation(id, t1, t2.getTranslationRange())) {
       logger.error("Error updating " + t1);
     }
   }
 }
  
  /** Update an annotation name from the database.
   *
   *  @param annot1 - AnnotatedFeatureI for the new annotation (to be updated to)
   *  @param annot2 - AnnotatedFeatureI for the old annotaiton (to be updated from)
   */
  private void updateAnnotName(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    //System.out.println("updateAnnot: " + annot1);
    long id = getChadoFeatureId(annot1);
    if(!updateFeatureName(id,annot1.getName(),true)) {
      logger.error("Error updating " + annot1);
    }
  } 
  
  /** Update an annotation type from the database.
   *
   *  @param annot1 - AnnotatedFeatureI for the new annotation (to be updated to)
   *  @param annot2 - AnnotatedFeatureI for the old annotaiton (to be updated from)
   */
  private void updateAnnotType(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    //System.out.println("updateAnnot: " + annot1);
    long id = getChadoFeatureId(annot1);
    if(!updateFeatureType(id,annot1.getFeatureType())) {
      logger.error("Error updating type " + annot1);
    }
  } 

  /** Add a sequence edit to the database (insertion/deletion/substitution)
   *
   *  @param edit - SequenceEdit object representing the edit
   */
  private void addSequenceEdit(SequenceEdit edit)
  {
    //System.out.println("Adding sequence edit: " + edit);
    String owner = getOwner(edit);
    Date date = getDate(edit);
    addSequenceEdit(edit, owner, date);
  }

  /** Delete a sequence edit to the database (insertion/deletion/substitution)
   *
   *  @param edit - SequenceEdit object representing the edit
   */
  private void deleteSequenceEdit(SequenceEdit edit)
  {
    //System.out.println("Deleting sequence edit: " + edit);
    long editId = getChadoFeatureId(edit);
    jdbcAdapter.deleteRow("feature", editId);
  }

  /** Begin a database transaction (not an Apollo transaction).
   */
  private void beginTransaction()
  {
    if (!jdbcAdapter.beginTransaction()) {
      logger.error("Error starting database transaction");
      return;
    }
  }

  /** Commit a database transaction.
   */
  private void commitTransaction()
  {
    if (!jdbcAdapter.commitTransaction()) {
      logger.error("Error commiting transaction");
    }
  }

  /** Rollback a database transaction.
   */
  private void rollbackTransaction()
  {
    if (!jdbcAdapter.rollbackTransaction()) {
      logger.error("Error rolling back transcation");
    }
  }

  /** Get the owner for an annotation (the user who has editted an annotation).
   *
   *  @param annot - AnnotatedFeatureI object to retrieve the owner from.
   *  @return The owner of this AnnotatedFeatureI object.
   */
  private String getOwner(AnnotatedFeatureI annot)
  {
    String owner = annot.getOwner();
    if (owner != null && owner.length() > 0) {
      return owner;
    }
    for (Object o : annot.getFeatures()) {
      owner = getOwner((AnnotatedFeatureI)o);
      if (owner != null && owner.length() > 0) {
        return owner;
      }
    }    
    return owner;
  }

  /** Get the owner for a sequence edit.  This is needed because the owner is not stored
   *  directly in the SequenceEdit object, so needs to retrieve the parent exon for the
   *  edit and get the owner from it.
   *
   *  @param edit - SequenceEdit object to retrieve the owner from.
   *  @return The owner of this SequenceEdit object.
   */
  private String getOwner(SequenceEdit edit)
  {
    return getOwner((AnnotatedFeatureI)edit.getRefFeature("exon").getParent());
  }

  /** Get the date for an annotation edit.  If no date is found, return the current
   *  date.
   *
   *  @param annot - AnnotatedFeatureI object to retrieve the date from.
   *  @return The date for this annotation edit.
   */
  private Date getDate(AnnotatedFeatureI annot)
  {
    String dateProp = annot.getProperty("date");
    Date date = null;
    if (dateProp != null && dateProp.length() > 0) {
      try {
        return dateFormat.parse(dateProp);
      }
      catch (ParseException e) {
      }
    }
    for (Object o : annot.getFeatures()) {
      date = getDate((AnnotatedFeatureI)o);
      if (date != null) {
        return date;
      }
    }
    //if no date is found, just return the current date
    return date != null ? date : new Date();
  }

  /** Get the date for a sequence edit.  This is needed because the owner is not stored
   *  directly in the SequenceEdit object, so needs to retrieve the parent exon for the
   *  edit and get the owner from it.  If no date is found, return the current
   *  date.
   *
   *  @param edit - SequenceEdit object to retrieve the date from.
   *  @return The date for this sequence edit.
   */
  private Date getDate(SequenceEdit edit)
  {
    return getDate((AnnotatedFeatureI)edit.getRefFeature("exon").getParent());
  }

  /** Get the rank of a feature in the parent feature.
   *
   *  @param feat - SeqFeatureI object to get the rank.
   *  @return The rank of the SeqFeatureI object.  -1 if this feature has no parent.
   */
  private int getRank(SeqFeatureI feat)
  {
    SeqFeatureI parent = feat.getParent();
    if (parent == null) {
      logger.warn("No parent for feature " + feat);
      return -1;
    }
    int rank = 0;
    for (Object o : parent.getFeatures()) {
      if (o == feat) {
        return rank;
      }
      ++rank;
    }
    logger.warn("Feature " + feat + " was not found in parent " + parent);
    return -1;
  }

  /** Update the synonyms for a AnnotatedFeatureI object.
   *
   *  @param annot1 - AnnotatedFeatureI object being edited.
   *  @param annot2 - AnnotatedFeatureI object stored in database.
   */
  private void updateSynonyms(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    Set<String> syns1 = new HashSet<String>();
    Set<String> syns2 = new HashSet<String>();
    for (Object o : annot1.getSynonyms()) {
      Synonym syn = (Synonym)o;
      syns1.add(syn.getName());
    }
    for (Object o : annot2.getSynonyms()) {
      Synonym syn = (Synonym)o;
      syns2.add(syn.getName());
    }
    long annotId = getChadoFeatureId(annot1);

    //add synonyms
    for (String syn : syns1) {
      if (!syns2.contains(syn)) {
        addSynonym(annotId, syn);
      }
    }

    //delete synonyms
    for (String syn : syns2) {
      if (!syns1.contains(syn)) {
        deleteSynonym(annotId, syn);
      }
    }
  }

  /** Update the comments for a AnnotatedFeatureI object.
   *
   *  @param annot1 - AnnotatedFeatureI object being edited.
   *  @param annot2 - AnnotatedFeatureI object stored in database.
   */
  private void updateComments(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    Map<String, Comment> comms1 = new HashMap<String, Comment>();
    Map<String, Comment> comms2 = new HashMap<String, Comment>();
    for (Object o : annot1.getComments()) {
      Comment comment = (Comment)o;
      comms1.put(comment.getText(), comment);
    }
    for (Object o : annot2.getComments()) {
      Comment comment = (Comment)o;
      comms2.put(comment.getText(), comment);
    }

    long annotId = getChadoFeatureId(annot1);

    //add comments
    for (Object o : annot1.getComments()) {
      Comment comment = (Comment)o;
      if (!comms2.containsKey(comment.getText())) {
        addComment(annotId, comment);
      }
    }
    //delete comments
    for (Object o : annot2.getComments()) {
      Comment comment = (Comment) o;
      if (!comms1.containsKey(comment.getText())) {
        deleteComment(annotId, comment);
      }
    }
  }


  /** Update the status for a AnnotatedFeatureI object.
   *
   *  @param annot1 - AnnotatedFeatureI object being edited.
   *  @param annot2 - AnnotatedFeatureI object stored in database.
   */
  private void updateStatus(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    boolean isFinished1 = annot1.isFinished();
    boolean isFinished2 = annot2.isFinished();

    long annotId = getChadoFeatureId(annot1);

    if(isFinished1 != isFinished2) {
      updateStatus(annotId,"status",annot1.getProperty("status"),annot2.getProperty("status"));     
    }

    boolean isProblematic1 = annot1.isProblematic();
    boolean isProblematic2 = annot2.isProblematic();
    if(isProblematic1 != isProblematic2) {
      updateStatus(annotId,"problem",""+isProblematic1,""+isProblematic2);
    }
  }

  /** Update the owner for a AnnotatedFeatureI object.
   *
   *  @param annot1 - AnnotatedFeatureI object being edited.
   *  @param annot2 - AnnotatedFeatureI object stored in database.
   */
  private void updateOwner(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
  {
    String owner1 = annot1.getOwner();
    String owner2 = annot2.getOwner();

    long annotId = getChadoFeatureId(annot1);

    if ((owner1!=null) || (owner2 != null)) {
      updateOwner(annotId,owner1,owner2);     
    }
  }
  
  /** Update the dbXrefs for a AnnotatedFeatureI object.
  *
  *  @param annot1 - AnnotatedFeatureI object being edited.
  *  @param annot2 - AnnotatedFeatureI object stored in database.
  */
 private void updateDbXrefs(AnnotatedFeatureI annot1, AnnotatedFeatureI annot2)
 {
   Comparator<DbXref> comp = new Comparator<DbXref>() {
     public int compare(DbXref dbxref1, DbXref dbxref2)
     {
       if (dbxref1.getDbName().equals(dbxref2.getDbName())) {
         return dbxref1.getIdValue().compareTo(dbxref2.getIdValue());
       }
       else {
         return dbxref1.getDbName().compareTo(dbxref2.getDbName());
       }
     }
     public boolean equals(DbXref dbxref1, DbXref dbxref2)
     {
       return compare(dbxref1, dbxref2) == 0;
     }
   };
   
   Set<DbXref> dbxrefs1 = new TreeSet<DbXref>(comp);
   Set<DbXref> dbxrefs2 = new TreeSet<DbXref>(comp);
   for (Object o : annot1.getDbXrefs()) {
     dbxrefs1.add((DbXref)o);
   }
   for (Object o : annot2.getDbXrefs()) {
     dbxrefs2.add((DbXref)o);
   }
   if (dbxrefs1.size() == 0 && dbxrefs2.size() == 0) {
     return;
   }
   long annotId = getChadoFeatureId(annot1);

   //add dbxrefs
   for (DbXref dbxref : dbxrefs1) {
     if (!dbxrefs2.contains(dbxref)) {
       addDbXref(annotId, dbxref);
     }
   }

   //delete dbxrefs
   for (DbXref dbxref : dbxrefs2) {
     if (!dbxrefs1.contains(dbxref)) {
       deleteDbXref(annotId, dbxref);
     }
   }
 }

  /** Process the sequence edits associated with a SequenceI object.
   *
   *  @param refSeq - SequenceI object for the genomic sequence being edited.
   *  @param origSeq - SequenceI object for the genomic sequence stored in the database.
   */
  private void processSequenceEdits(SequenceI refSeq, SequenceI origSeq)
  {
    Map<String, SequenceEdit> edits1 = new HashMap<String, SequenceEdit>();
    Map<String, SequenceEdit> edits2 = new HashMap<String, SequenceEdit>();
    if (refSeq.getGenomicErrors() != null) {
      for (Object o : refSeq.getGenomicErrors().entrySet()) {
        Map.Entry e = (Map.Entry)o;
        edits1.put((String)e.getKey(), (SequenceEdit)e.getValue());
      }
    }
    if (origSeq.getGenomicErrors() != null) {
      for (Object o : origSeq.getGenomicErrors().entrySet()) {
        Map.Entry e = (Map.Entry)o;
        edits2.put((String)e.getKey(), (SequenceEdit)e.getValue());
      }
    }

    //add sequence edit
    for (Map.Entry<String, SequenceEdit> i : edits1.entrySet()) {
      if (!edits2.containsKey(i.getKey())) {
        addSequenceEdit(i.getValue());
      }
    }
    //delete sequence edit
    for (Map.Entry<String, SequenceEdit> i : edits2.entrySet()) {
      if (!edits1.containsKey(i.getKey())) {
        deleteSequenceEdit(i.getValue());
      }
    }
  }

  /** Add a top level annotation to the database.
   *
   *  @param annot - SeqFeatureI object for the top level annotation.
   *  @param author - Owner of this annotation.
   *  @param date - Date of edit.
   *  @param srcFeature - ChadoFeature object for the source of this feature.
   *  @return Primary key for the newly added feature.
   */
  protected Long addTopLevelAnnot(SeqFeatureI annot, String author, Date date, ChadoFeature srcFeature)
  {
    setChadoUniquename(annot);
    return super.addTopLevelAnnot(annot, author, date, srcFeature);
  }

  /** Add a transcript to the database.
   *
   *  @param transcript - Transcript object to be added to the database.
   *  @param author - Owner of this transcript.
   *  @param date - Date of edit.
   *  @param geneId - Primary key of the parent gene.
   *  @param transcriptRank - Rank of transcript in parent gene.
   *  @param srcFeature - ChadoFeature object for the source of the transcript.
   *  @param checkExons - Whether to check for shared exons (the convention for Chado is to have shared exons).
   *  @return Primary key of the newly added transcript.
   */
  protected Long addTranscript(Transcript transcript, String author, Date date, Long geneId, long transcriptRank, 
      ChadoFeature srcFeature, boolean checkExons) 
  {
    setChadoUniquename(transcript);
    return super.addTranscript(transcript, author, date, geneId, transcriptRank, srcFeature, checkExons);
  }

  /** Add an exon to the database.
   *
   *  @param exon - SeqFeatureI object for the exon to be added to the database.
   *  @param author - Owner of this exon.
   *  @param date - Date of edit.
   *  @param transId - Primary key of the parent transcript.
   *  @param exonNum - Rank of exon in parent transcript.
   *  @param srcFeature - ChadoFeature object for the source of the exon.
   *  @param checkExons - Whether to check for shared exons (the convention for Chado is to have shared exons).
   *  @return Primary key of the newly added exon.
   */
  protected Long addExon(SeqFeatureI exon, String author, Date date, Long transId, int exonNum, ChadoFeature srcFeature, boolean checkExons)
  {
    boolean newExon = true;
    if (checkExons) {
      if (jdbcAdapter.getFeatureId(exon.getId()) != -1) {
        newExon = false;
      }
      else {
        ChadoFeature cf = jdbcAdapter.getChadoFeatureByLocation(exon, srcFeature);
        if (cf != null) {
          newExon = false;
          exon.setId(cf.getUniquename());
          exon.setName(cf.getName());
        }
      }
    }
    if (newExon) {
      setChadoUniquename(exon);
    }
    return super.addExon(exon, author, date, transId, exonNum, srcFeature, checkExons);
  }

  /** Add a sequence edit to the database.
   *
   *  @param seqEdit - SequenceEdit object to be added.
   *  @param author - Owner of sequence edit.
   *  @param date - Date of edit.
   *  @return Primary key of newly added sequence edit.
   */
  protected Long addSequenceEdit(SequenceEdit seqEdit, String author, Date date)
  {
    setChadoUniquename(seqEdit);
    return super.addSequenceEdit(seqEdit, author, date);
  }

  /** Get the Chado name for a peptide.
   *
   *  @param nameAdapter - ApolloNameAdapterI object that defines what the name should be.
   *  @param name - Parent transcript name.
   *  @param pfeat - SeqFeatureI object for the peptide feature.
   *  @return Generated peptide name.
   */
  protected String getChadoPeptideName(ApolloNameAdapterI nameAdapter, String name, SeqFeatureI pfeat)
  {
    if (nameAdapter instanceof GmodNameAdapter) {
      setChadoUniquename(pfeat);
      return pfeat.getId();
    }
    return super.getChadoPeptideName(nameAdapter, name, pfeat);
  }

  /** The the Chado name for a CDS.
   *
   *  @param nameAdapter - ApolloNameAdapterI object that defines what the name should be.
   *  @param name - Parent transcript name
   *  @return Generated CDS name.
   */
  protected String getChadoCdsName(ApolloNameAdapterI nameAdapter, String name)
  {
    if (nameAdapter instanceof GmodNameAdapter) {
      return ((GmodNameAdapter)nameAdapter).getNewChadoDbUniquename("CDS",
          jdbcAdapter.getNextPrimaryKeyId("feature"));
    }
    return nameAdapter.generateChadoCdsNameFromTranscriptName(name);
  }

  /** Sets the Chado uniquename for a feature.  Currently only supports GmodNameAdapter
   *  instances.
   *
   *  @param feat - SeqFeatureI object to have the uniquename set.
   */
  private void setChadoUniquename(SeqFeatureI feat)
  {
    if (!feat.isAnnot()) {
      return;
    }
    AnnotatedFeatureI annot = (AnnotatedFeatureI)feat;
    ApolloNameAdapterI nameAdapter = this.adapter.getNameAdapter(annot);
    String id = annot.getId();
    if (nameAdapter instanceof GmodNameAdapter) {
      GmodNameAdapter gmodNameAdapter = (GmodNameAdapter)nameAdapter;
      //if (gmodNameAdapter.isTemp(id)) {
      id = gmodNameAdapter.getNewChadoDbUniquename(annot, jdbcAdapter.getNextPrimaryKeyId("feature"));
      annot.setId(id);
      annot.setName(id);
      //}
    }
  }
}
