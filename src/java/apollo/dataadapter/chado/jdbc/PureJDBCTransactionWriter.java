package apollo.dataadapter.chado.jdbc;

import java.sql.Connection;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import apollo.config.Config;
import apollo.config.ApolloNameAdapterI;
import apollo.config.TigrAnnotNameAdapter;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.DbXref;
import apollo.datamodel.Exon;
import apollo.datamodel.Protein;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.Transcript;
import apollo.datamodel.SequenceEdit;
import apollo.datamodel.Comment;
import apollo.datamodel.Synonym;

import apollo.dataadapter.chado.ChadoAdapter;

import apollo.editor.Transaction;
import apollo.editor.AddTransaction;
import apollo.editor.DeleteTransaction;
import apollo.editor.UpdateTransaction;
import apollo.editor.CompoundTransaction;
import apollo.editor.UpdateParentTransaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionSubpart;
import apollo.editor.UserName;

import org.apache.log4j.*;
  
/**
 * An alternative to JDBCTransactionWriter that:
 *
 * <ul>
 *  <li>Does not require any triggers to be defined in the database</li>
 *  <li>Keeps all of the SQL and database access code localized to JdbcChadoAdapter and its subclasses</li>
 *  <li>Supports two update modes: in-place update and copy-on-write</li>
 * </ul>
 *
 * @author Jonathan Crabtree
 * @version $Revision: 1.37 $ $Date: 2009/06/25 17:26:50 $ $Author: gk_fan $
 */
public class PureJDBCTransactionWriter {

  // -----------------------------------------------------------------------
  // NOTES:
  //
  // all Transactions have:
  //  date
  //  author
  //  transactionClass
  //  transactionOperation - determined from subclass
  //  newSubpartValue
  //  oldSubpartValue
  //  newProperties
  //  oldProperties
  //  parentTransaction
  //  feature
  //  source
  //  subpartRank
  //  oldId
  //  newId
  //
  // Current assumptions:
  //  each feature is localized to at most one sequence
  //   (i.e., no redundant featurelocs)
  //
  // If a feature/row is the target of an in-place delete then the delete
  //  must be preceded by deleting/nulling all the referencing rows
  //  (need to be careful of delete + add to change in this case)
  //
  // How many tables have the is_obsolete column needed for copy-on-write deletes?
  //  -only feature?
  // 
  // TODO:
  //  add option to record when a new gene or transcript was created from 
  //   an existing feature (e.g., a gene prediction)?
  //  
  // Things to revisit (maybe):
  //  is_analysis is set to 0 for all central dogma features in chado that
  //  are created from Apollo
  // -----------------------------------------------------------------------
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(PureJDBCTransactionWriter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------
  
  protected ChadoAdapter adapter;
  protected JdbcChadoAdapter jdbcAdapter;
  protected boolean copyOnWrite = false;
  protected boolean noCommit = false;

  // whether to log all Apollo transactions that the class is told to commit
  protected boolean logApolloTransactions = true;
  // whether to log all SQL commands issued as part of a commit or attempted commit
  protected boolean logSql = true;
  
  // If you want to use the CDS feature
  protected boolean useCDS = true;
  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------

  /**
   * @param adapter      ChadoAdapter to use for writing changes
   * @param jdbcAdapter  JdbcChadoAdapter to use for writing changes
   * @param copyOnWrite  Whether to keep a copy of the old object(s) when writing changes.
   * @param noCommit     Whether to run in 'no commit' mode.
   */
  public PureJDBCTransactionWriter(ChadoAdapter adapter, JdbcChadoAdapter jdbcAdapter, boolean copyOnWrite, boolean noCommit, boolean useCDS) {
    this.adapter = adapter;
    this.jdbcAdapter = jdbcAdapter;
    this.copyOnWrite = copyOnWrite;
    this.noCommit = noCommit;
    this.useCDS = useCDS;
    logger.debug("PureJDBCTransactionWriter copyOnWrite=" + copyOnWrite + ", noCommit=" + noCommit);
  }

  // -----------------------------------------------------------------------
  // PureJDBCTransactionWriter - public methods
  // -----------------------------------------------------------------------

  /**
   * Write a set of transactions to the database using <code>adapter</code>
   *
   * @param tm  TransactionManager that contains the uncommitted transactions to write.
   * @return Whether the commit succeeded.
   *
   */
  public boolean commitTransactions(TransactionManager tm) {
    boolean commitSucceeded = true;
    List tlist = tm.getTransactions();
    int nt = tlist.size();
    Iterator i = tlist.iterator();
    int tnum = 1;

    // log all transactions before attempting to commit them
    logger.debug("commitTransactions: " + nt + " transaction(s) to commit");
    logTransactionList(tlist);

    try {
      // BEGIN TRANSACTION
      boolean retval = this.jdbcAdapter.beginTransaction();
      if (!retval) { fatalError("unable to begin transaction for write/update"); }

      // report/log initial maximum chado ids of each table affected by the writer
      if (logger.isTraceEnabled()) {
        logger.trace("feature NEXT ID=" +  this.jdbcAdapter.getNextPrimaryKeyId("feature"));
        logger.trace("featureloc NEXT ID=" +  this.jdbcAdapter.getNextPrimaryKeyId("featureloc"));
        logger.trace("feature_relationship NEXT ID=" +  this.jdbcAdapter.getNextPrimaryKeyId("feature_relationship"));
      }
      
      while (i.hasNext()) {
        Transaction t = (Transaction)(i.next());
        boolean rv = commitTransaction(Integer.toString(tnum++), nt, t, false);
        if (!rv) {
          logger.error("commitTransaction: failed for transaction " + t.oneLineSummary());
          commitSucceeded = false;
          break;
        }
      }

      // COMMIT OR ROLLBACK
      if (this.noCommit || (!commitSucceeded)) {
        // rollback

	// workaround to have this message come from the ChadoAdapter:
	if (this.noCommit) {
	  Logger l2 = LogManager.getLogger(ChadoAdapter.class);
	  l2.warn("ChadoAdapter: running in 'no commit' mode - rolling back changes");
	} else {
	  logger.warn("error trying to commit changes; rolling back transaction");
	}
        this.jdbcAdapter.rollbackTransaction();
      } else {
        // commit
        this.jdbcAdapter.commitTransaction();
      }
    } catch (Throwable t) {
      logger.error("commitTransactions encountered uncaught exception", t);
      this.jdbcAdapter.rollbackTransaction();
      commitSucceeded = false;
    }

    if (!commitSucceeded) {
      // handles rollback
      fatalError("commitTransactions: commit was not successful, rolling back");
    }

    return commitSucceeded;
  }

  // -----------------------------------------------------------------------
  // PureJDBCTransactionWriter - protected/internal methods
  // -----------------------------------------------------------------------

  protected void logTransactionList(List tlist) {
    int nt = tlist.size();
    Iterator i = tlist.iterator();
    int tnum = 1;

    while (i.hasNext()) {
      Transaction t = (Transaction)(i.next());
      logger.debug(tnum + ": " + t.oneLineSummary());
      tnum++;
    }
  }

  /**
   * @param checkExons whether to check database for exons in addExon
   */
  protected boolean commitTransaction(String transId, int numTransactions, Transaction t, boolean checkExons) {
    boolean commitSucceeded = true;
    // TODO - is this the right approach or should we be using a custom Log4J formatter for apollo transactions?
    logger.debug("commitTransaction: " + transId + "/" +  numTransactions + ":  " + t.oneLineSummary());

    // UpdateParentTransaction - must check this before CompoundTransaction since it's a superclass
    if (t instanceof UpdateParentTransaction) {
      // could handle this as a CompoundTransaction with NO_TYPE
      commitSucceeded = commitUpdateParentTransaction(transId, (UpdateParentTransaction)t);
    }

    // CompoundTransaction
    else if (t instanceof CompoundTransaction) {
      CompoundTransaction ct = (CompoundTransaction)t;

      // whether addExon should check whether an exon has already been inserted
      boolean checkExonsBeforeInsert = false;

      // has compound type
      if (ct.hasCompoundType()) {
	if (ct.isSplit()) {
	  // because the SPLIT transaction will contain an addGene for a new gene that uses old exons
	  checkExonsBeforeInsert = true;
	} else if (ct.isMerge()) {
	  // not using this info. right now
	}
	else {
	  fatalError("CompoundTransaction with compound type is not a split or a merge: " + ct);
	  return false;
	}
      }

      // process child transactions normally
      int nct = ct.size();
      for (int i = 0;i < nct;++i) {
	boolean retval = commitTransaction(transId + "." + (i+1), numTransactions, ct.getTransaction(i), checkExonsBeforeInsert);
	if (!retval) {
	  logger.error("failure committing compound transaction with " + nct + " child transactions");
	  return false;
	}
      }
    }

    // AddTransaction
    else if (t instanceof AddTransaction) {
      commitSucceeded = commitAddTransaction(transId, (AddTransaction)t, checkExons);
    }

    // DeleteTransaction
    else if (t instanceof DeleteTransaction) {
      commitSucceeded = commitDeleteTransaction(transId, (DeleteTransaction)t);
    }

    // UpdateTransaction
    else if (t instanceof UpdateTransaction) {
      commitSucceeded = commitUpdateTransaction(transId, (UpdateTransaction)t);
    }

    // Unknown subclass of Transaction
    else {
      fatalError("unrecognized apollo.editor.Transaction subclass " + t.getClass());
      commitSucceeded = false;
    }

    return commitSucceeded;
  }

  // ----------------------------------
  // CHADO FEATURE_ID
  // ----------------------------------

  // TODO - This method should be moved elsewhere, probably either into the name adapter or 
  // the chado instance.  It computes the mapping from Apollo SeqFeatureI to chado feature_id
  // and could therefore depend on the specific instance.  Currently the SeqFeatureI id, name, etc., 
  // are set by JdbcChadoAdapter (e.g. for exons and transcripts) so it might make sense to have
  // a single piece of code that sets the SeqFeatureI attributes based on the database row and,
  // conversely, that extracts the chado feature_id given a SeqFeatureI.
  //
  // A related issue is whether the chado feature_id should be stored directly in the SeqFeatureI
  // or whether a lookup should be done (using uniquename, for example.)  Do we want to engineer
  // the lookup to fail if another Apollo instance has edited the gene/exon/transcript?  Not
  // worrying about this yet.
  //

  /**
   * @return The chado feature_id or -1 if it could not be determined.
   */
  protected long getChadoFeatureId(SeqFeatureI feature) {
    // for both exons and transcripts the Apollo id = the chado uniquename
    String uniquename = feature.getId();
    long featureId = this.jdbcAdapter.getFeatureId(uniquename);
    if (featureId == -1) {
      fatalError("unable to determine chado feature_id for Apollo SeqFeature with uniquename = " + uniquename);
    }
    return featureId;
  }

  // ----------------------------------
  // UPDATE PARENT
  // ----------------------------------

  protected boolean commitUpdateParentTransaction(String transId, UpdateParentTransaction t) {
    List tl = t.getTransactions();
    int nt = tl.size();

    if (nt != 2) {
      logger.error("unexpected number of child transactions (" + nt + ") in UpdateParentTransaction");
      return false;
    }

    Transaction t1 = (Transaction)(tl.get(0));
    Transaction t2 = (Transaction)(tl.get(1));

    if (!(t1 instanceof DeleteTransaction)) {
      logger.error("transaction 0 of UpdateParentTransaction is not a DeleteTranaction");
      return false;
    }
    if (!(t2 instanceof AddTransaction)) {
      logger.error("transaction 1 of UpdateParentTransaction is not an AddTranaction");
      return false;
    }

    DeleteTransaction dt = (DeleteTransaction)t1;
    AddTransaction at = (AddTransaction)t2;
    SeqFeatureI f1 = dt.getDeletedFeatureClone();
    SeqFeatureI oldParent = dt.getParentFeatureClone();
    SeqFeatureI newParent = at.getParentFeatureClone();
    int newRank = at.getRank();

    logger.debug("committing UpdateParentTransaction with feature=" + f1 + " old parent=" + oldParent + " new parent=" + newParent + " new rank=" + newRank);
    logger.debug("committing UpdateParentTransaction with original feature=" + dt.getDeletedFeature() + " old parent=" + dt.getParentFeature() + " new parent=" + at.getParentFeature() + " new rank=" + newRank);

    String author = t.getAuthor();
    Date date = t.getDate();
    boolean updateSucceeded = true;

    // case 1: Exon moving between Transcripts
    if (f1.isExon() && oldParent.isTranscript() && newParent.isTranscript()) {
      long exonId = getChadoIdForFeature(f1);
      long transcriptId1 = getChadoIdForFeature(oldParent);
      long transcriptId2 = getChadoIdForFeature(newParent);
      
      // query for old exon rank
      Long oldRank = this.jdbcAdapter.getExonRank(new Long(exonId), new Long(transcriptId1));
      
      // delete old feature_relationship (exon - transcript)
      String exonTransRelTerm = this.jdbcAdapter.getChadoInstance().getExonTransRelationTerm();
      Long exonTransRelId = getChadoRelationshipCvTermId(exonTransRelTerm);
      if (!this.jdbcAdapter.deleteFeatureRelationshipRow(new Long(exonId), new Long(transcriptId1), exonTransRelId)) {
        logger.error("commitUpdateParentTransaction failed to delete old exon-transcript feature_relationship");
        return false;
      }

      // decrement old exon ranks
      if (!this.jdbcAdapter.decrementTranscriptExonRanks(new Long(transcriptId1), oldRank.longValue())) {
        logger.error("commitUpdateParentTransaction failed to decrement exon ranks for old transcript");
        return false;
      }

      // Check whether the target row already exists - this is another workaround for the 
      // somewhat inconsistent transactions produced by the AnnotationEditor.  In some
      // cases (e.g. gene splits) we can receive UpdateParentTransactions for exons that
      // have been moved between transcripts.  We find the database in a state where the
      // old exon-transcript feature_relationship has to be deleted but the new one has
      // already been created (as part of an earlier monolithic "add gene" transaction.)

      // TODO - an alternative here would be to track the feature_relationship rows that 
      // we've inserted during the current transaction and refrain from inserting any duplicates

      // does the new feature_relationship exist already?
      Long newFeatRelId = this.jdbcAdapter.getFeatureRelationshipId(new Long(exonId), new Long(transcriptId2), exonTransRelId);
      // no
      if (newFeatRelId == null) {
	// increment new exon ranks
	if (!this.jdbcAdapter.incrementTranscriptExonRanks(new Long(transcriptId2), (long)newRank)) {
	  logger.error("commitUpdateParentTransaction failed to increment exon ranks for new transcript");
	  return false;
	}
	
	// insert new feature_relationship (exon - transcript)
	Long featRelId = this.jdbcAdapter.insertFeatureRelationshipRow(new Long(exonId), new Long(transcriptId2), exonTransRelId, null, new Long(newRank));
	if (featRelId == null) {
	  logger.error("commitUpdateParentTransaction failed to insert new exon-transcript feature_relationship");
	  return false;
	}
      } 
      // yes
      else {
	logger.warn("skipping UpdateParentTransaction for exon " + exonId + " and new transcript = " + transcriptId2 + " - already done");
      }
    }

    // case 2: Transcript moving between Genes
    else if (f1.isTranscript() && oldParent.isProteinCodingGene() && newParent.isProteinCodingGene()) {
      long transcriptId = getChadoIdForFeature(f1);
      long geneId1 = getChadoIdForFeature(oldParent);
      long geneId2 = getChadoIdForFeature(newParent);
      logger.debug("transcript-gene UpdateParentTransaction transcriptId=" + transcriptId + " geneId1=" + geneId1 + " geneId2=" + geneId2);
      
      // query for old transcript rank
      Long oldRank = this.jdbcAdapter.getTranscriptRank(new Long(transcriptId), new Long(geneId1));
      
      // delete old feature_relationship (transcript - gene)
      String transGeneRelTerm = this.jdbcAdapter.getChadoInstance().getTransGeneRelationTerm();
      Long transGeneRelId = getChadoRelationshipCvTermId(transGeneRelTerm);

      // This is a workaround for a known problem with the AnnotationEditor, namely that in the process
      // of splitting a gene/transcript it will create a Transcript without firing an add event.  That
      // transcript gets added to the original (unsplit) gene and is then moved to one of the newly-created
      // genes (by an UpdateParentTransaction.)  Since a transaction was never generated for the original
      // add event we may not have committed a row for the (strictly temporary) relationship between
      // transcriptId and geneId1.  Furthermore, since the new feature_relationship is created when the
      // new gene is added (via an AddTransaction), we can completely ignore the UpdateParentTransaction
      // if we find that:
      //
      // 1. The original feature_relationship doesn't exist.
      // 2. The new feature_relationship already exists.
      //
      if (!this.jdbcAdapter.deleteFeatureRelationshipRow(new Long(transcriptId), new Long(geneId1), transGeneRelId)) {
	// failed to delete original feature_relationship; check whether it's even there
	Long oldFeatRelId = this.jdbcAdapter.getFeatureRelationshipId(new Long(transcriptId), new Long(geneId1), transGeneRelId);

	// original feature_relationship not found
	if (oldFeatRelId == null) { 
	  // does the new feature_relationship exist already?
	  Long newFeatRelId = this.jdbcAdapter.getFeatureRelationshipId(new Long(transcriptId), new Long(geneId2), transGeneRelId);
	  // no
	  if (newFeatRelId == null) {
	    // this isn't necessarily an error, but haven't encountered any cases yet where the delete has
	    // been processed already and the add hasn't:
	    logger.error("UpdateParentTransaction - old feature_relationship not found, but new feature_relationship doesn't exist either");
	    return false;
	  } 
	  // no need to do UpdateParentTransaction
	  else {
	    logger.warn("skipping UpdateParentTransaction for transcript " + transcriptId + " and new gene = " + geneId2 + " - already done");
	    return true;
	  }
	} 
	// old relationship exists, but couldn't be deleted - this is still an error
	else {
	  logger.error("failed to delete feature_relationship between transcript " + transcriptId + " and " + geneId1);
	  return false;
	}
      }
      // feature_relationship delete succeeded; decrement any rank values left over
      else {
	// decrement old transcript ranks
	if (!this.jdbcAdapter.decrementGeneTranscriptRanks(new Long(transcriptId), oldRank.longValue())) {
	  logger.error("commitUpdateParentTransaction failed to decrement transcript ranks for old transcript");
	  return false;
	}
      }

      // increment new transcript ranks
      if (!this.jdbcAdapter.incrementGeneTranscriptRanks(new Long(transcriptId), (long)newRank)) {
        logger.error("commitUpdateParentTransaction failed to increment transcript ranks for new transcript");
        return false;
      }

      // insert new feature_relationship (transcript - gene)
      Long featRelId = this.jdbcAdapter.insertFeatureRelationshipRow(new Long(transcriptId), new Long(geneId2), transGeneRelId, null, new Long(newRank));
      if (featRelId == null) {
        logger.error("commitUpdateParentTransaction failed to insert new transcript-gene feature_relationship");
        return false;
      }
    }

    // anything else to handle?  moving transcripts between genes?
    else {
      logger.error("don't know how to handle UpdateParentTransaction with feature=" + f1 + " old parent=" + oldParent + " new parent=" + newParent);
      updateSucceeded = false;
    }

    return updateSucceeded;
  }

  // ----------------------------------
  // ADD
  // ----------------------------------

  /**
   * @param checkExons whether to check database for exons in addExon
   */
  protected boolean commitAddTransaction(String transId, AddTransaction t, boolean checkExons) {
    // we have to use the cloned feature to ensure that we're seeing the feature
    // as it was when the transaction was created:
    SeqFeatureI feature = t.getAddedFeatureClone();
    TransactionSubpart subpart = t.getSubpart();
    String author = t.getAuthor();
    Date date = t.getDate();
    boolean addSucceeded = true;

    if (logger.isTraceEnabled()) {
      SeqFeatureI f2 = t.getAddedFeature();
      logger.trace("commitAddTransaction clone id=" + feature.getId() + " original id=" + f2.getId());
    }

    // ---------------------------------------
    // peptide
    // ---------------------------------------
    if (t.isAddPeptide()) {

      // this actually gets treated like a CDS/peptide update, because the adapter will 
      // never (currently) write a transcript without a corresponding CDS and polypeptide
      // feature (even if they have NULL residues)
      if (!(feature instanceof Transcript)) {
        logger.error("encountered AddTransaction with isAddPeptide()=true, but feature is not a Transcript");
        return false;
      }
      long featureId = getChadoIdForFeature(feature);
      return updateCdsFeatureLocation(featureId, (Transcript)feature, null);
    }

    // ---------------------------------------
    // exon
    // ---------------------------------------
    if (feature instanceof Exon) {
      // have to check this case first because exons can have both isExon()==true and isProteinCodingGene == true
      Exon exon = (Exon)feature;
      int exonRank = t.getRank();

      // must have a transcript in the database already 
      SeqFeatureI parent = t.getParentFeatureClone();
      if ((parent == null) || (!(parent instanceof Transcript))) {
        logger.error("AddTransaction for exon " + exon + " has parentFeatureClone=" + parent);
        return false;
      }

      Transcript transcript = (Transcript)parent;
      Long transcriptId = new Long(getChadoIdForFeature(transcript));
      Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
      ChadoFeatureLoc transcriptLoc = new ChadoFeatureLoc(transcript, null, c);

      if (exonRank < 0) {
        logger.error("Transcript returned feature index = " + exonRank + " for exon " + exon);
        return false;
      }
      ChadoFeature srcFeature = transcriptLoc.getSourceFeature();
      if (addExon(feature, author, date, transcriptId, exonRank, srcFeature, checkExons) == null) {
        addSucceeded = false;
      }
    }

    // ---------------------------------------
    // transcript
    // ---------------------------------------
    else if (feature instanceof Transcript) {
      
      if(subpart.isComment()) {  
        return addComment(feature,t);
      }
      
      logger.debug("adding feature " + feature + " with isTranscript() = true, isProteinCodingGene()=" + feature.isProteinCodingGene());      
      Transcript  transcript = (Transcript)feature;
      
      // get gene and gene location
      SeqFeatureI parent = t.getParentFeatureClone();
      if (!parent.isProteinCodingGene()) {
        logger.error("parent of transcript " + feature + " has isProteinCodingGene() = false");
        return false;
      }

      Long geneId = new Long(getChadoIdForFeature(parent));
      if (geneId.longValue() == -1) {
        logger.error("could not determine chado id for " + parent);
        return false;
      }

      ChadoFeatureLoc geneLoc = new ChadoFeatureLoc(geneId.longValue(), this.jdbcAdapter.getConnectionUsedForLastTransaction());
      ChadoFeature srcFeature = geneLoc.getSourceFeature();

      // get number of transcripts in gene; add this one at the end
      List tl = this.jdbcAdapter.getGeneTranscriptIds(geneId);
      long transcriptRank = tl.size();

      logger.debug("adding transcript " + transcript + " to gene " + parent + " with rank = " + transcriptRank);
      Long transcriptId = addTranscript(transcript, author, date, geneId, transcriptRank, srcFeature, checkExons);
      if (transcriptId == null) {
        addSucceeded = false; 
      }
    }
    // ---------------------------------------
    // Sequencing error
    // ---------------------------------------
    else if (feature instanceof SequenceEdit) {
      SequenceEdit  seqEdit = (SequenceEdit)feature; 
      if(addSequenceEdit(seqEdit, author, date) == null)
        addSucceeded = false;	
    }
    // this case must appear last because transcripts and exons can have isProteinCodingGene() == true:

    // ---------------------------------------
    // gene
    // ---------------------------------------
    else if (feature.isProteinCodingGene()) {

      // adding a gene
      if (subpart == null) {
        if (addGene(feature, author, date, null, checkExons) == null) {
          addSucceeded = false;
        }
      } 
      // adding something TO a gene
      else {
        // synonym
        if (subpart.isSynonym()) {
          addSucceeded = addSynonym(feature,t);
        } else if(subpart.isComment()) {  
          addSucceeded = addComment(feature,t);
        } else if(subpart.isDbXref()) {
          addSucceeded = addDbXref(feature, t);  
        } else {
          logger.error("not yet implemented - gene AddTransaction with subpart = " + subpart);
          addSucceeded = false;
        }
      }
    } 
    else {
      throw new UnsupportedOperationException("commitAddTransaction() can't handle add request for " + feature);
    }
    return addSucceeded;
  }

  /**
   * Add a top level annotation to the chado database.
   *
   * @param annot Apollo feature that represents the top level annotation
   * @param author Author string from the Apollo transaction.
   * @param date Date from the Apollo transaction.
   * @param srcFeature Chado feature on which the gene is located.  May be null.
   * @return The chado feature.feature_id of the gene feature.
   */
  protected Long addTopLevelAnnot(SeqFeatureI annot, String author, Date date, ChadoFeature srcFeature) {
    logger.debug("addTopLevelAnnot called with annotation=" + annot + " srcFeature=" + srcFeature);
    ChadoFeatureLoc annotLoc = new ChadoFeatureLoc(annot, srcFeature, this.jdbcAdapter.getConnectionUsedForLastTransaction());
    if (srcFeature == null) { srcFeature = annotLoc.getSourceFeature(); }
    long organismId = srcFeature.getOrganismId();
    long refseqFeatureId = srcFeature.getFeatureId();
    logger.debug("addannot() called on annot with id=" + annot.getId() + " name=" + annot.getName());

    // ---------------------------------------
    // annot feature
    // ---------------------------------------
    String uniquename = annot.getId();
    String name = uniquename;
    if(annot.getName() != null)
      name = annot.getName();
      
    Long dbxrefId = getChadoPrimaryDbxrefId(annot);
    Integer isAnalysis = new Integer(getChadoIsAnalysis(annot));
    String residues = null;
    Long seqlen = null;
    Long typeId = this.jdbcAdapter.getFeatureCVTermId(annot.getFeatureType());
    Long annotId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, name, uniquename, residues, seqlen, typeId, isAnalysis, date);
    if (annotId == null) return null;

    // ---------------------------------------
    // annot featureloc
    // ---------------------------------------
    int gfmin = annot.getLow();
    int gfmax = annot.getHigh();
    int gstrand = annot.getStrand();

    // if the annot has children, featureloc is the smallest extent that includes all of the children
    if (annot.getFeatures().size() > 0) {
      Vector children = annot.getFeatures();
      for (int i = 0; i < children.size(); ++i) {
        SeqFeatureI child = (SeqFeatureI)children.elementAt(i);
        ChadoFeatureLoc childloc = new ChadoFeatureLoc(child, srcFeature, null);
        int tfmin = childloc.getFmin().intValue();
        int tfmax = childloc.getFmax().intValue();
        int tstrand = childloc.getStrand().intValue();

        // initialize fmin, fmax, and strand
        if (i == 0) {
          gfmin = tfmin;
          gfmax = tfmax;
          gstrand = tstrand;
        } else {
          if (tfmin < gfmin) { gfmin = tfmin; }
          if (tfmax > gfmax) { gfmax = tfmax; }
          if (tstrand != gstrand) {
            fatalError("addannot() child feature #" + i + " of annot " + uniquename + " has strand = " + tstrand);
          }
        }
      }
    }

    // annot should always have featureloc.rank = 0
    int rank = 0;
    // phase and residueInfo not applicable to annots
    Integer phase = null;
    String residueInfo = null;
    int locgroup = getChadoLocgroup(annot, srcFeature);
    boolean gfmin_partial = false;
    boolean gfmax_partial = false;
    Long flId = this.jdbcAdapter.insertFeaturelocRow(annotId, new Long(srcFeature.getFeatureId()), new Integer(gfmin), new Integer(gfmax),
        gfmin_partial, gfmax_partial, new Integer(gstrand), phase, residueInfo, locgroup, rank);


    // ---------------------------------------
    // annot featureprop
    // ---------------------------------------    
    String ownerTerm = this.jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
    Long ownerTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
    if (ownerTypeId == null) {
      logger.warn("database has no 'owner' cvterm; cannot record owner of annot with id = " + annotId);
    } else {
      Long annotOwnerFpId = this.jdbcAdapter.insertFeaturepropRow(annotId, ownerTypeId, author, 0);
      if (annotOwnerFpId == null) return null;
    }

    String dateTerm = this.jdbcAdapter.getChadoInstance().getFeatureCreateDatePropertyTerm();
    Long dateTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(dateTerm);
    if (dateTypeId == null) {
      logger.warn("database has no 'date' cvterm; cannot store date featureprop for annot with id = " + annotId);
    } else {
      Long annotDateFpId = this.jdbcAdapter.insertFeaturepropRow(annotId, dateTypeId, date.toString(), 0);
      if (annotDateFpId == null) return null;
    }

    // isProblematic annotation
    if(Config.getStyle().showIsProbCheckbox()) {
      Long problemTypeId = this.jdbcAdapter.getPropertyTypeCVTermId("problem");
      if(problemTypeId == null) {
        logger.warn("database has no 'problem' cvterm; cannot store problem featureprop for annot with id = " + annotId);
      } else {
        Long transProbFpId = this.jdbcAdapter.insertFeaturepropRow(annotId, problemTypeId, "false", 0);
        if (transProbFpId == null) return null;
      }
    }

    // TODO: what about the following tables - 
    //  feature_cvterm?
    //   note - this is used for some transcripts in eha3
    //  feature_dbxref?
    //  feature_synonym?

    return annotId;
  }
  
  /**
   * Add a gene to the chado database.
   *
   * @param gene Apollo feature that represents the gene.
   * @param author Author string from the Apollo transaction.
   * @param date Date from the Apollo transaction.
   * @param srcFeature Chado feature on which the gene is located.  May be null.
   * @return The chado feature.feature_id of the gene feature.
   * @param checkExons whether to check database for exons in addExon
   */
  protected Long addGene(SeqFeatureI gene, String author, Date date, ChadoFeature srcFeature, boolean checkExons) {
    logger.debug("addGene called with gene=" + gene + " srcFeature=" + srcFeature);

    Long geneId = addTopLevelAnnot(gene, author, date, srcFeature);    
    //String name = gene.getId();
    String uniquename = gene.getId();

    // ---------------------------------------
    // transcript feature(s)
    // ---------------------------------------
    // note that the following is OK because clone() clones a feature's children (but not the parent):
    Vector transcripts = gene.getFeatures();
    int nt = transcripts.size();
    if (nt == 0) { fatalError("addGene() called on gene " + uniquename + " with no transcripts"); }

    for (int t = 0;t < nt;++t) {
      SeqFeatureI trans = (SeqFeatureI)transcripts.elementAt(t);
      logger.debug("adding transcript " + t + " for gene with id=" + trans.getId() + " name=" + trans.getName());

      if (trans.isTranscript()) {
        // Note that we're using the order of the transcripts in Apollo to set the transcript's 
        // chado rank (in feature_relationship).   Note also the coercion to Transcript; currently 
        // only instances of this class have isTranscript() == true.  In general it's not clear in 
        // Apollo when one is supposed to use instanceof versus isExon(), isTranscript(), etc.  
        Long transId = addTranscript((Transcript)trans, author, date, geneId, t, srcFeature, checkExons);
        if (transId == null) return null;
      } else {
        fatalError("addGene() child feature #" + t + " of gene " + uniquename + " has isTranscript() == false");
      }
    } // end of transcript loop
    
    // synonyms
    addSynonyms((AnnotatedFeatureI)gene);
    
    // comments
    addComments((AnnotatedFeatureI)gene);
    
    // dbxrefs
    addDbxrefs((AnnotatedFeatureI)gene);
    
    return geneId;
  }

  /**
   * Add a transcript to the chado database.
   *
   * @param transcript Apollo feature that represents the transcript/mRNA.
   * @param author Author string from the Apollo transaction.
   * @param date Date from the Apollo transaction.
   * @param geneId Chado feature.feature_id of the gene to which the transcript belongs.
   * @param transcriptRank Rank of the transcript with respect to the gene.
   * @param srcFeature Chado feature on which the transcript is located.  May be null.
   * @param checkExons whether to check database for exons in addExon
   * @return The chado feature.feature_id of the transcript feature.
   */
  protected Long addTranscript(Transcript transcript, String author, Date date, Long geneId, long transcriptRank, 
			       ChadoFeature srcFeature, boolean checkExons) 
  {
    ChadoFeatureLoc transLoc = new ChadoFeatureLoc(transcript, srcFeature, this.jdbcAdapter.getConnectionUsedForLastTransaction());
    if (srcFeature == null) { srcFeature = transLoc.getSourceFeature(); }
    long organismId = srcFeature.getOrganismId();
    long refseqFeatureId = srcFeature.getFeatureId();
    Long dbxrefId = getChadoPrimaryDbxrefId(transcript);
    Integer isAnalysis = new Integer(getChadoIsAnalysis(transcript));

    // ---------------------------------------
    // transcript feature
    // ---------------------------------------
    String uniquename = transcript.getId();
    String name = uniquename;
    if(transcript.getName() != null)
      name = transcript.getName();
    
    if (uniquename == null) {
      System.out.println("null uniquename");
    }
    
    Long typeId = this.jdbcAdapter.getChadoInstance().getTranscriptCvTermId();
    // transcripts aren't assigned residues or seqlen (TODO - config option? this may be TIGR-specific)
    String residues = null;
    Long seqlen = null;
    Long transId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, name, uniquename, residues, seqlen, typeId, isAnalysis, date);
    if (transId == null) return null;

    // ---------------------------------------
    // transcript member_of gene
    // ---------------------------------------
    String transGeneRelTerm = this.jdbcAdapter.getChadoInstance().getTransGeneRelationTerm();
    Long transGeneRelId = getChadoRelationshipCvTermId(transGeneRelTerm);
    String value = null;
    Long rank = new Long(transcriptRank);
    Long gtRelId = this.jdbcAdapter.insertFeatureRelationshipRow(transId, geneId, transGeneRelId, value, rank);
    if (gtRelId == null) return null;
    
    // ---------------------------------------
    // transcript featureprops
    // ---------------------------------------   
    String ownerTerm = this.jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
    Long ownerTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
    if (ownerTypeId == null) {
      logger.warn("database has no 'owner' cvterm; cannot record owner of transcript with id = " + transId);
    } else {
      Long transOwnerFpId = this.jdbcAdapter.insertFeaturepropRow(transId, ownerTypeId, author, 0);
      if (transOwnerFpId == null) return null;
    }

    String dateTerm = this.jdbcAdapter.getChadoInstance().getFeatureCreateDatePropertyTerm();
    Long dateTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(dateTerm);
    if (dateTypeId == null) {
      logger.warn("database has no 'date' cvterm; cannot store date featureprop for transcript with id = " + transId);
    } else {
      Long transDateFpId = this.jdbcAdapter.insertFeaturepropRow(transId, dateTypeId, date.toString(), 0);
      if (transDateFpId == null) return null;
    }
    
    // isProblematic annotation
    if(Config.getStyle().showIsProbCheckbox()) {
      Long problemTypeId = this.jdbcAdapter.getPropertyTypeCVTermId("problem");
      if(problemTypeId == null) {
	logger.warn("database has no 'problem' cvterm; cannot store problem featureprop for transcript with id = " + transId);
      } else {
	Long transProbFpId = this.jdbcAdapter.insertFeaturepropRow(transId, problemTypeId,""+transcript.isProblematic(), 0);
	if (transProbFpId == null) return null;
      }
    }
    // status : is finished annotation ?
    if(Config.getStyle().showFinishedCheckbox()) {
      Long statusTypeId = this.jdbcAdapter.getPropertyTypeCVTermId("status");
      if(statusTypeId == null) {
	logger.warn("database has no 'status' cvterm; cannot store status featureprop for transcript with id = " + transId);
      } else {
	Long transStatusFpId = this.jdbcAdapter.insertFeaturepropRow(transId, statusTypeId, transcript.getProperty("status"), 0);
	if (transStatusFpId == null) return null;
      }
    }    
    
    // synonyms
    addSynonyms(transcript);

    // comments
    addComments(transcript);

    // dbxrefs
    addDbxrefs(transcript);
    
    // ---------------------------------------
    // exon feature(s)
    // ---------------------------------------
    Vector exons = transcript.getFeatures();
    int ne = exons.size();
    if (ne == 0) { fatalError("addTranscript() called on transcript " + uniquename + " with no exons"); }

    for (int e = 0;e < ne;++e) {
      SeqFeatureI exon = (SeqFeatureI)exons.elementAt(e);
      if (exon.isExon()) {
        Long exonId = addExon(exon, author, date, transId, e, srcFeature, checkExons);
        if (exonId == null) return null;
      } else {
        fatalError("addTranscript() child feature #" + e + " of transcript " + uniquename + " has isExon() == false");
      }
    }

    // ---------------------------------------
    // transcript featureloc
    // ---------------------------------------
    int tfmin = transLoc.getFmin().intValue();
    int tfmax = transLoc.getFmax().intValue();
    boolean tfmin_partial = false;
    boolean tfmax_partial = false;
    Integer tstrand = transLoc.getStrand();
    // phase and residueInfo not applicable to transcripts
    Integer phase = null;
    String residueInfo = null;
    int locgroup = getChadoLocgroup(transcript, srcFeature);
    Long flId = this.jdbcAdapter.insertFeaturelocRow(transId, new Long(srcFeature.getFeatureId()), new Integer(tfmin), new Integer(tfmax), tfmin_partial, tfmax_partial, 
                                                     tstrand, phase, residueInfo, locgroup, transcriptRank);
    if (flId == null) { return null; }


    ApolloNameAdapterI nameAdapter = this.adapter.getNameAdapter((AnnotatedFeatureI)transcript);
    
    if (transcript.isProteinCodingGene()) {

      Long cdsId = null;
      ChadoCds cc = getCdsCoordinatesFromTranscript(transcript, tfmin, tfmax);
      if(useCDS) {
        // ---------------------------------------
        // add CDS
        // ---------------------------------------

        // TODO - We're currently assuming a 1-1-1 relationship between transcript, CDS, and polypeptide,
        //        hence the addTranscript method is responsible for adding the CDS and polypeptide features.

        // TODO - handle issue of non-protein-coding genes.  
        // config option? ( TIGR policy is to create CDS and polypeptide but make the residues/seqlen null/0.)

        String cdsName = getChadoCdsName(nameAdapter, name); //nameAdapter.generateChadoCdsNameFromTranscriptName(name);
        String cdsUniquename = cdsName;
        String cdsResidues = transcript.getCodingDNA();
        Long cdsSeqlen = new Long(cdsResidues.length());
        Long cdsTypeId = this.jdbcAdapter.getFeatureCVTermId("CDS");

        // use same dbxrefId and isAnalysis as for the transcript
        cdsId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, cdsName, cdsUniquename, cdsResidues, cdsSeqlen, cdsTypeId, isAnalysis, date);
        if (cdsId == null) return null;

        // ---------------------------------------
        // CDS-transcript relationship
        // ---------------------------------------
        String cdsTransRelTerm = this.jdbcAdapter.getChadoInstance().getCdsTransRelationTerm();
        Long cdsTransRelId = getChadoRelationshipCvTermId(cdsTransRelTerm);
        value = null;
        // set rank to 0 - assuming only 1 CDS per transcript
        rank = new Long(0);
        Long ctRelId = this.jdbcAdapter.insertFeatureRelationshipRow(cdsId, transId, cdsTransRelId, value, rank);
        if (ctRelId == null) return null;

        // ---------------------------------------
        // CDS featureloc
        // ---------------------------------------
        phase = null;
        residueInfo = null;
        locgroup = getChadoLocgroup(transcript, srcFeature);
        Long cdsFlId = this.jdbcAdapter.insertFeaturelocRow(cdsId, new Long(srcFeature.getFeatureId()), new Integer(cc.getFmin()), new Integer(cc.getFmax()), 
            cc.getFminPartial(), cc.getFmaxPartial(), tstrand, phase, residueInfo, locgroup, transcriptRank);
        if (cdsFlId == null) return null;

        // ---------------------------------------
        // CDS featureprop
        // ---------------------------------------    
        if (ownerTypeId != null) {
          Long cdsOwnerFpId = this.jdbcAdapter.insertFeaturepropRow(cdsId, ownerTypeId, author, 0);
          if (cdsOwnerFpId == null) return null;  
        }
        if (dateTypeId != null) {
          Long cdsDateFpId = this.jdbcAdapter.insertFeaturepropRow(cdsId, dateTypeId, date.toString(), 0);
          if (cdsDateFpId == null) return null;  
        }
      }  

      // ---------------------------------------
      // add polypeptide
      // ---------------------------------------
      Protein pfeat = transcript.getProteinFeat();
      if (pfeat == null) {
        // TODO - even if Apollo doesn't create a Protein we need to do so in chado (config option? TIGR usage convention)
        fatalError("transcript " + name + " has no Protein feature");
        return null;
      }
      String protName = getChadoPeptideName(nameAdapter, name, pfeat); //nameAdapter.generatePeptideNameFromTranscriptName(name);
      String protUniquename = protName;
      String protResidues = getChadoResidues(pfeat);
      Long protSeqlen = new Long(protResidues.length());
      // TODO - this (and other cvterm.names) depend on the chado version; they shouldn't be hard-coded
      Long protTypeId = this.jdbcAdapter.getFeatureCVTermId("polypeptide");
      // use same dbxrefId and isAnalysis as for the transcript
      Long protId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, protName, protUniquename, protResidues, protSeqlen, protTypeId, isAnalysis, date);
      if (protId == null) return null;

      // ---------------------------------------
      // polypeptide properties
      // ---------------------------------------
      if (ownerTypeId != null) {
        Long protOwnerFpId = this.jdbcAdapter.insertFeaturepropRow(protId, ownerTypeId, author, 0);
        if (protOwnerFpId == null) return null;
      }
      if (dateTypeId != null) {
        Long protDateFpId = this.jdbcAdapter.insertFeaturepropRow(protId, dateTypeId, date.toString(), 0);
        if (protDateFpId == null) return null;
      }


      if(useCDS) {
        // ---------------------------------------
        // polypeptide-CDS relationship
        // ---------------------------------------
        String pepCdsRelTerm = this.jdbcAdapter.getChadoInstance().getPolypeptideCdsRelationTerm();
        Long pepCdsRelId = getChadoRelationshipCvTermId(pepCdsRelTerm);
        value = null;
        rank = new Long(0);
        Long pcRelId = this.jdbcAdapter.insertFeatureRelationshipRow(protId, cdsId, pepCdsRelId, value, rank);
        if (pcRelId == null) return null;
      }

      // ---------------------------------------
      // polypeptide-transcript relationship
      // ---------------------------------------

      // This is an optional, redundant relationship used by TIGR.  To prevent the adapter
      // from creating these relationships, do not create an entry for <polypeptideTransRelationTerm>
      // in chado-adapter.xml

      String pepTransRelTerm = this.jdbcAdapter.getChadoInstance().getPolypeptideTransRelationTerm();
      if (pepTransRelTerm == null) {
        return transId;
      }
      Long pepTransRelId = getChadoRelationshipCvTermId(pepTransRelTerm);
      value = null;
      rank = new Long(0);
      Long ptRelId = this.jdbcAdapter.insertFeatureRelationshipRow(protId, transId, pepTransRelId, value, rank);
      if (ptRelId == null) return null;

      // ---------------------------------------
      // polypeptide featureloc
      // ---------------------------------------
      // polypeptide featureloc is the same as that of the CDS (this redundant featureloc is TIGR-specific)
      phase = null;
      residueInfo = null;
      locgroup = getChadoLocgroup(transcript, srcFeature);
      Long protFlId = this.jdbcAdapter.insertFeaturelocRow(protId, new Long(srcFeature.getFeatureId()), new Integer(cc.getFmin()), new Integer(cc.getFmax()), 
          cc.getFminPartial(), cc.getFmaxPartial(), tstrand, phase, residueInfo, locgroup, transcriptRank);
      if (protFlId == null) return null;
    }
    
    return transId;
  }

  /**
   * Add an exon to a transcript, inserting the exon into the database if it does not
   * already exist.
   *
   * @param exon Apollo feature that represents the exon.
   * @param author Author string from the Apollo transaction.
   * @param date Date from the Apollo transaction.
   * @param transId Chado feature.feature_id of the transcript to which the exon belongs.
   * @param exonNum Index of the exon in the transcript (counting from 0, as per chado feature_relationship.rank)
   * @param srcFeature Chado feature on which the exon is located.  May be null.
   * @param checkExons whether to check database for exons in addExon
   * @return The chado feature.feature_id of the exon feature.
   */
  protected Long addExon(SeqFeatureI exon, String author, Date date, Long transId, int exonNum, ChadoFeature srcFeature, boolean checkExons) {
    ChadoFeatureLoc exonLoc = new ChadoFeatureLoc(exon, srcFeature, this.jdbcAdapter.getConnectionUsedForLastTransaction());
    long organismId = srcFeature.getOrganismId();
    long refseqFeatureId = srcFeature.getFeatureId();
    Long dbxrefId = getChadoPrimaryDbxrefId(exon);
    Integer isAnalysis = new Integer(getChadoIsAnalysis(exon));
    boolean exonAlreadyInDb = false;

    Long exonId = null;

    // check whether the exon is already in the database
    if (checkExons) {
      long eid = getChadoIdForFeature(exon);
      if (eid != -1) { 
        exonAlreadyInDb = true; 
        exonId = new Long(eid);
      }
    }

    // ---------------------------------------
    // update feature_relationship ranks
    // ---------------------------------------
    // increment feature_relationship rank for all exons with rank >= exonNum, to make room for the new exon
    if (!this.jdbcAdapter.incrementTranscriptExonRanks(transId, exonNum)) {
      logger.error("failed to increment feature_relationship.rank for transcript with feature_id=" + transId + " new rank=" + exonNum);
      return null;
    }

    // ---------------------------------------
    // exon feature
    // ---------------------------------------

    if (exonAlreadyInDb) {
      // TODO - is it possible that the exon has changed somehow and we need to do an update?
      // TODO - update exon featureprops?
    } else {
      String name = exon.getId();
      String uniquename = exon.getId();
      Long typeId = this.jdbcAdapter.getFeatureCVTermId("exon");
      // exons aren't assigned residues or seqlen (TODO - config option? this may be TIGR-specific)
      String residues = null;
      Long seqlen = null;
      exonId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, name, uniquename, residues, seqlen, typeId, isAnalysis, date);
      if (exonId == null) return null;

      // ---------------------------------------
      // exon featureprops
      // ---------------------------------------
      String ownerTerm = this.jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
      Long ownerTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
      if (ownerTypeId != null) {
	Long exonOwnerFpId = this.jdbcAdapter.insertFeaturepropRow(exonId, ownerTypeId, author, 0);
	if (exonOwnerFpId == null) return null;
      }
      String dateTerm = this.jdbcAdapter.getChadoInstance().getFeatureCreateDatePropertyTerm();
      Long dateTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(dateTerm);
      if (dateTypeId != null) {
	Long exonDateFpId = this.jdbcAdapter.insertFeaturepropRow(exonId, dateTypeId, date.toString(), 0);
	if (exonDateFpId == null) return null;
      }
    }
    
    // ---------------------------------------
    // exon part_of transcript
    // ---------------------------------------
    String exonTransRelTerm = this.jdbcAdapter.getChadoInstance().getExonTransRelationTerm();
    Long exonTransRelId = getChadoRelationshipCvTermId(exonTransRelTerm);
    String value = null;
    // rank indicates order of exon in transcript
    Long relRank = new Long(exonNum);
    Long etRelId = this.jdbcAdapter.insertFeatureRelationshipRow(exonId, transId, exonTransRelId, value, relRank);
    if (etRelId == null) return null;
    
    // ---------------------------------------
    // exon part_of gene
    // ---------------------------------------

    // TODO - config option? we don't seem to be using these in the most recent TIGR databases

    // Note that rank is not being set here, since there's no obvious way to impose a consistent order on all
    // the exons of a gene, given the possibility of alternate and trans-splicing.
    //    Long egRelId = this.jdbcAdapter.insertFeatureRelationshipRow(exonId, geneId, partOfId, null, null);
    //    if (egRelId == null) return null;
    
    // ---------------------------------------
    // exon featureloc
    // ---------------------------------------
    if (exonAlreadyInDb) { 
      // TODO - is it possible that the exon location has changed somehow (with no transaction) and we need to do an update?
    } else {
      Integer efmin = exonLoc.getFmin();
      Integer efmax = exonLoc.getFmax();
      boolean efmin_partial = false;
      boolean efmax_partial = false;
      Integer estrand = exonLoc.getStrand();
      // residueInfo not applicable to exons
      String residueInfo = null;
      // phase is not set because it could depend on the splice form
      Integer phase = null;
      int locgroup = getChadoLocgroup(exon, srcFeature);
      // exons have only one location, so featureloc.rank does not apply:
      int rank = 0;
      Long flId = this.jdbcAdapter.insertFeaturelocRow(exonId, new Long(srcFeature.getFeatureId()), efmin, efmax, efmin_partial, efmax_partial,
						       estrand, phase, residueInfo, locgroup, rank);
      if (flId == null) return null;
    }

    // dbxrefs
    addDbxrefs((AnnotatedFeatureI)exon);

    return exonId;
  }
  
  protected boolean addSynonym (SeqFeatureI feature, AddTransaction t) {
    long featId = getChadoFeatureId(feature);
	
    String name=t.getNewSubpartValue().toString();
    return addSynonym(featId, name);

    /*
    if(name == null) {
      logger.error("Can't insert a synonym with a name null");
      return false;
    }
	
    Long synonymTypeId = this.jdbcAdapter.getNullCVTermId("synonym");
    Long synId = null;
    if (synonymTypeId == null) {
      logger.warn("database has no 'synonym' cvterm; cannot record synonym with name = " + name);
    } else {
      synId = this.jdbcAdapter.insertSynonymRowIfNeeded(name,synonymTypeId,name);		
    }
    if(synId == null) return false; 
		
    // feature_synonym table
    Integer pubId = new Integer(1); //Need to be changed
    boolean is_current = true;
    boolean is_internal = false;
    Long featsynId = this.jdbcAdapter.insertFeatureSynonymRow(synId,new Long(featId),new Integer(1),is_current,is_internal);
    if(featsynId == null)
      return false; 
		   
    return true;
    */
  }

  /**
     Comment strings look like this: 
     Only one EST supports this alternative transcript::DATE:Mon Feb 26 18:08:49 CET 2007::TS:1172509728919
     
     With the author in the pub table
  */
  protected boolean addComment (SeqFeatureI feature, AddTransaction t) {
    long featId = getChadoFeatureId(feature);
	
    AnnotatedFeatureI annot = feature.getAnnotatedFeature();
    int index = t.getSubpartRank();
    Comment comment= (Comment) annot.getComments().get(index);

    /*
    if(comment == null) {
      logger.error("Can't insert a comment with a value null");
      return false;
    }

    String fullComment = formatChadoCommentValue(comment);
	
    String commentTerm = this.jdbcAdapter.getChadoInstance().getCommentPropertyTerm();
    Long commentTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(commentTerm);
    if (commentTypeId == null) {
      logger.warn("database has no "+commentTerm+" cvterm");
      return false;
    }
	
    String ownerTerm = this.jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
    Long ownerTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
    if (ownerTypeId == null) {
      logger.warn("database has no '"+ownerTerm+"' cvterm;");
      return false;
    }
    boolean checkNextRank = true;
    long rank = 0;
    Long commentFpId = this.jdbcAdapter.insertFeaturepropRow(new Long(featId), commentTypeId, fullComment, rank,checkNextRank);
    if (commentFpId == null) return false;
	      
    Long pubId = this.jdbcAdapter.insertPubRowIfNeeded(comment.getPerson(), ownerTypeId.longValue());
    if (pubId == null) return false;
	
    Long commentFppubId = this.jdbcAdapter.insertFeaturepropPubRow(commentFpId,pubId);
    if (commentFppubId == null) return false;
	
    return true;
    */
    return addComment(featId, comment);
  }
  
  /** Add all dbxrefs from an AnnotatedFeatureI object
   * 
   * @param annot - annotation to store dbxrefs from
   */
  protected void addDbxrefs(AnnotatedFeatureI annot)
  {
    long annotId = getChadoFeatureId(annot);

    //add dbxrefs
    for (Object o : annot.getDbXrefs()) {
      addDbXref(annotId, (DbXref)o);
    }
  }

  protected boolean addDbXref (SeqFeatureI feature, AddTransaction t) {
    Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
    long featureId = getChadoFeatureId(feature);

    Object o = t.getNewSubpartValue();
    if (o == null) {
      logger.error("can't add null dbxref");
      return false;
    }
    if (!(o instanceof DbXref)) {
      logger.error("new subpart value in add dbxref transaction is not a DbXref");
      return false;
    }
    DbXref dbx = (DbXref)o;

    return addDbXref(featureId, dbx);
  }

  protected boolean addDbXref(long featureId, DbXref dbx) {
    Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();

    // first insert db and dbxref (if needed)
    Long dbId = this.jdbcAdapter.insertDbRowIfNeeded(dbx.getDbName());
    if (dbId == null) return false;
    Long dbXrefId = this.jdbcAdapter.insertDbXrefRowIfNeeded(dbId.longValue(),dbx.getIdValue(),dbx.getVersion(),dbx.getDescription());
    if (dbXrefId == null) return false;

    // then link it in to feature or feature_dbxref or both
    // primary dbxref - update the feature table (feature.dbxref_id)
    if (dbx.isPrimary()) {
      ChadoFeature dbFeat = new ChadoFeature(new Long(featureId), c, true);

      // leave these values unchanged; only modifying the dbxref_id
      String name = dbFeat.getName();
      String uniquename = dbFeat.getUniquename();
      String residues = dbFeat.getResidues();
      long organismId = dbFeat.getOrganismId();
      long typeId = dbFeat.getTypeId();
      boolean isAnalysis = dbFeat.getIsAnalysis();
      boolean isObsolete = dbFeat.getIsObsolete();

      if (!this.jdbcAdapter.updateFeatureRow(featureId, dbFeat, dbXrefId, organismId, name, uniquename, 
          residues, typeId, isAnalysis, isObsolete)) {
        return false;
      }
    }

    // secondary dbxref - insert/update the feature_dbxref table
    if (dbx.isSecondary()) {
      // check whether the row is already present
      Long fdbx_id = this.jdbcAdapter.getFeatureDbXrefId(featureId, dbXrefId.longValue());
      boolean isCurrent = (dbx.getCurrent() == 0) ? false : true;

      // the row is present; check that it doesn't need updating
      if (fdbx_id != null) {
        ChadoFeatureDbXref dbFeatDbXref = new ChadoFeatureDbXref(fdbx_id.longValue(), c);
        if (!this.jdbcAdapter.updateFeatureDbXrefRow(dbFeatDbXref, featureId, dbXrefId.longValue(), isCurrent)) {
          return false;
        }
      }

      // the row is not present - insert it
      else {
        fdbx_id = this.jdbcAdapter.insertFeatureDbXrefRow(featureId, dbXrefId.longValue(), isCurrent);
        if (fdbx_id == null) return false;
      }
    }
    return true;
  }
  
  protected boolean deleteDbXref(long annotId, DbXref dbXref)
  {
    Long dbId = jdbcAdapter.getDbId(dbXref.getDbName());
    if (dbId == null) {
      logger.error("No database stored in Chado for dbxref " + dbXref.getDbName() + ":" + dbXref.getIdValue());
      return false;
    }
    Long dbXrefId = jdbcAdapter.getDbXrefId(dbId, dbXref.getIdValue(), dbXref.getVersion());
    if (dbXrefId == null) {
      logger.error("No dbXref stored in Chado for dbxref " + dbXref.getDbName() + ":" + dbXref.getIdValue());
      return false;
    }
    
    return jdbcAdapter.deleteFeatureDbXrefRow(annotId, dbXrefId);
  }
  
  private String formatChadoCommentValue(Comment comment) {
    String fullText = comment.getText();
    long timeStamp = comment.getTimeStamp();
    String person = comment.getPerson();
    Date date = new Date(timeStamp);
    return fullText+"::DATE:"+date+"::TS:"+timeStamp;  
  }
  
  protected Long addSequenceEdit(SequenceEdit seqEdit, String author, Date date) {
	
    logger.debug("addSequenceEdit "+seqEdit+" "+seqEdit.getRefFeature());    
    
    ChadoFeature  srcFeature = new ChadoFeature(seqEdit.getRefFeature("exon"), this.jdbcAdapter.getConnectionUsedForLastTransaction());
    long organismId = srcFeature.getOrganismId();
    long refseqFeatureId = srcFeature.getFeatureId();
    
    // ---------------------------------------
    // sequence edit feature
    // ---------------------------------------   
    
    String name = seqEdit.getId();
    String uniquename = name;
    Long dbxrefId = getChadoPrimaryDbxrefId(seqEdit);
    Integer isAnalysis = new Integer(getChadoIsAnalysis(seqEdit));
    
    
    String residues = seqEdit.getResidue();
    Long seqlen = null;
    if(residues != null) 
      seqlen = new Long(""+residues.length());
    Long typeId = this.jdbcAdapter.getFeatureCVTermId(seqEdit.getFeatureType());

    Long seqEditId = this.jdbcAdapter.insertFeatureRow(dbxrefId, organismId, name, uniquename, residues, seqlen, typeId, isAnalysis, date);
    if (seqEditId == null) return null;

    // ---------------------------------------
    // sequence edit featureloc
    // ---------------------------------------       
    long rank = 0;
    int fmin = seqEdit.getStart()-1;
    int fmax = seqEdit.getEnd(); // normally the same thing than fmin
    Integer phase = null;
    Integer strand = null;
    String residueInfo = null;
    int locgroup = getChadoLocgroup(seqEdit, srcFeature);
    
    boolean fmin_partial = false;
    boolean fmax_partial = false;
    Long flId = this.jdbcAdapter.insertFeaturelocRow(seqEditId, new Long(refseqFeatureId), new Integer(fmin), new Integer(fmax),
                                                     fmin_partial, fmax_partial, strand, phase, residueInfo, locgroup, rank);

    if (flId == null) return null;
    return seqEditId;
  }
  
  /**
   * Compute the chado featureloc.locgroup for an Apollo feature.
   *
   * @param feat Apollo feature.
   * @param srcFeature Chado feature on which <code>feat</code> is located (determines locgroup).
   * @returns the chado featureloc.locgroup assigned to the featureloc between <code>feat</code> and <code>srcFeature</code>
   */
  protected int getChadoLocgroup(SeqFeatureI feat, ChadoFeature srcFeature) {
    // TODO - this assumes that multiple/redundant featurelocs are not used (config option?)
    return 0;
  }

  protected int getChadoLocgroup(long feature_id, ChadoFeature srcFeature) {
    // TODO - this assumes that multiple/redundant featurelocs are not used (config option?)
    return 0;
  }

  /**
   * Determine what to put in the feature.dbxref_id column for an Apollo feature.
   *
   * @param feat Apollo feature.
   * @returns a value to insert into feature.dbxref_id for <code>feat</code>, or null if none.
   */
  protected Long getChadoPrimaryDbxrefId(SeqFeatureI feat) {
    // TODO - figure out whether we should be assigning anything useful to this
    return null;
  }

  /**
   * Determine what value to place in the chado feature.is_analysis column for 
   * an Apollo feature.
   *
   * @param feat Apollo feature.
   * @returns a value to insert into feature.is_analysis for <code>feat</code> (either 0 or 1)
   */
  protected int getChadoIsAnalysis(SeqFeatureI feat) {
    // all features created in Apollo are assumed to be manually-curated/created, so is_analysis = 0:
    return 0;
  }

  /**
   * Determine what value to place in the chado feature.residues column for 
   * an Apollo feature.
   *
   * @param feat Apollo feature
   * @returns A valid chado feature.residues value for <code>feat</code>
   */
  protected String getChadoResidues(SeqFeatureI feat) {
    SequenceI fs = null;
    if (feat instanceof Protein) {
      // Protein uses a different method name for some reason
      fs = ((Protein)feat).getPeptideSequence();  
    } else {
      fs = feat.getFeatureSequence();  
    }
    if (fs == null) {
      fatalError("getFeatureSequence() returned null for " + feat.getId());
    }
    return fs.getResidues();
  }

  protected Long getChadoRelationshipCvTermId(String cvName) {
    Long cvtermId = this.jdbcAdapter.getRelationshipCVTermId(cvName); 
    if (cvtermId == null) {
      fatalError("getChadoRelationshipCvTermId could not find cvterm_id for '" + cvName + "'");
    }
    return cvtermId;
  }
  
  // ----------------------------------
  // DELETE
  // ----------------------------------

  protected boolean commitDeleteTransaction(String transId, DeleteTransaction dt) {
    SeqFeatureI feature = dt.getDeletedFeatureClone();
    SeqFeatureI parent = dt.getParentFeatureClone();
    int oldRank = dt.getSubpartRank();
    TransactionSubpart subpart = dt.getSubpart();
    Object oldVal = dt.getOldSubpartValue();
    // NOTE - can't access Transaction.getSource();
    boolean deleteSucceeded = false;
    if(subpart != null ) {
      if(subpart.isSynonym()) {
        long featureId = getChadoFeatureId(feature);
        /*
        Long synonymTypeId = this.jdbcAdapter.getNullCVTermId("synonym");
        if (synonymTypeId == null) {
          logger.error("database has no 'synonym' cvterm;");
        } else {
          Long synonymId = this.jdbcAdapter.getSynonymId(((Synonym) oldVal).getName(),synonymTypeId);
          return this.jdbcAdapter.deleteFeatureSynonymRow(new Long(featureId),synonymId,new Integer(1));
        }
        return false;
        */
        return deleteSynonym(featureId, ((Synonym)oldVal).getName());

      } else if(subpart.isComment()) {
        long featureId = getChadoFeatureId(feature);
        /*
        String value = formatChadoCommentValue((Comment) oldVal);
        String commentTerm = this.jdbcAdapter.getChadoInstance().getCommentPropertyTerm();
        Long typeId = this.jdbcAdapter.getPropertyTypeCVTermId(commentTerm);
        if (typeId == null) {
          logger.warn("database has no "+commentTerm+" cvterm");
          return false;
        }
        Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
        ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(new Long(featureId),typeId,value, c);
        if(dbFeatProp != null) { 
          return this.jdbcAdapter.deleteFeaturepropRow(new Long(dbFeatProp.getFeaturepropId()));
        }	  
        return false;
        */
        return deleteComment(featureId, (Comment)oldVal);

      } else {
        logger.error("not yet implemented - DeleteTransaction with subpart = " + subpart);
        return false;
      }
      // exon
    } else if (feature.isExon()) {
      if ((parent == null) || (!(parent instanceof Transcript))) {
	logger.error("DeleteTransaction for exon " + feature + " has parentFeatureClone=" + parent);
	return false;
      }
      Transcript transcript = (Transcript)parent;
      long transcriptId = getChadoIdForFeature(transcript);
      long exonId = getChadoFeatureId(feature);

      deleteSucceeded = deleteExon(exonId, transcriptId);
    } 
    // transcript
    else if (feature.isTranscript()) {
      long featureId = getChadoFeatureId(feature);
      deleteSucceeded = deleteTranscript(featureId);
    }
    // protein-coding gene
    else if (feature.isProteinCodingGene()) {
      long featureId = getChadoFeatureId(feature);
      deleteSucceeded = deleteGene(featureId);
    }
    else if (feature.isSequencingError()) {
      long featureId = getChadoFeatureId(feature);
      deleteSucceeded = this.jdbcAdapter.deleteRow("feature", featureId);
    }
    // anything else to delete?  polypeptide and CDS are currently in 1-1 association with transcript (by assumption)
    else {
      fatalError("DeleteTransaction for unsupported feature: " + feature + " with FeatureType=" + feature.getFeatureType());
    }

    return deleteSucceeded;
  }

  /** Delete an exon from the database (defaults to not checking if it is a shared
   *  exon
   * 
   * @param exonId - feature.feature_id of the exon
   * @param transcriptId - feature.feature_id of the parent transcript
   * @return whether the exon was successfully deleted
   */
  protected boolean deleteExon(long exonId, long transcriptId)
  {
    return deleteExon(exonId, transcriptId, false);
  }
  
  /** Delete an exon from the database
   * 
   * @param exonId - feature.feature_id of the exon
   * @param transcriptId - feature.feature_id of the parent transcript
   * @param checkExon - check to see that this exon is not shared before deletion
   * @return whether the exon was successfully deleted
   */
  protected boolean deleteExon(long exonId, long transcriptId, boolean checkExon) {
    // query database for exon's current rank; can't get it from the transaction because the
    // exon may already have been removed from the transcript by the time the transaction is created
    Long exonRank = this.jdbcAdapter.getExonRank(new Long(exonId), new Long(transcriptId));
    if (exonRank == null) {
      logger.error("unable to determine rank for exon with feature_id=" + exonId + " in transcript with feature_id=" + transcriptId);
      return false;
    }

    logger.debug("deleting exon with feature_id = " + exonId + ", rank = " + exonRank);

    // delete exon (and all referencing rows, including featureloc and feature_relationship)
    if (!checkExon || !jdbcAdapter.isSharedExon(exonId)) {
      if (!this.jdbcAdapter.deleteRow("feature", exonId)) {
        logger.error("failed to delete exon with feature_id = " + exonId);
        return false;
      }
    }
    else {
      String exonTransRelTerm = jdbcAdapter.getChadoInstance().getExonTransRelationTerm();
      Long exonTransRelId = getChadoRelationshipCvTermId(exonTransRelTerm);
      if (!jdbcAdapter.deleteFeatureRelationshipRow(exonId, transcriptId, exonTransRelId)) {
        return false;
      }
    }
    
    // adjust feature_relationship rank of any exon following this one
    if (!this.jdbcAdapter.decrementTranscriptExonRanks(new Long(transcriptId), exonRank.longValue())) {
      logger.error("failed to decrement feature_relationship.rank for transcript with feature_id=" + transcriptId + " new rank=" + exonRank);
      return false;
    }

    return true;
  }

  protected boolean deleteTranscript(long featureId)
  {
    //return deleteTranscript(featureId, false);
    return deleteTranscript(featureId, true);
  }
  
  protected boolean deleteTranscript(long featureId, boolean checkExons) {

    // TODO - get list of exons that have *only* this transcript as their parent and delete them
    // (is this necessary or can we rely on Apollo to generate the delete exon events for us?)
    // el - new transaction model will not generate delete transactions for exons, so need to
    // delete them explicitly

    // 1. find and delete linked CDS feature(s)
    
    List clist = this.jdbcAdapter.getTranscriptCdsIds(new Long(featureId));
    logger.debug("retrieved " + clist.size() + " CDS features linked to transcript with id " + featureId);
    Iterator ci = clist.iterator();
    while (ci.hasNext()) {
      Long cdsId = (Long)(ci.next());
      // deleteCDS takes care of polypeptide feature
      if (!deleteCDS(cdsId.longValue())) { return false; }
    }
    
    // 2. find and delete linked polypeptide features
    List plist = this.jdbcAdapter.getTranscriptPolypeptideIds(new Long(featureId));
    logger.debug("retrieved " + plist.size() + " polypeptide features linked to CDS with id " + featureId);
    Iterator pi = plist.iterator();
    while (pi.hasNext()) {
      Long pepId = (Long)(pi.next());
      if (!deletePolypeptide(pepId.longValue())) { return false; }
    }  
    
    // 3. find and delete linked exon feature(s)
    List elist = this.jdbcAdapter.getTranscriptExonIds(new Long(featureId));
    logger.debug("retrieved " + elist.size() + " exon features linked to transcript with id " + featureId);
    Iterator ei = elist.iterator();
    while (ei.hasNext()) {
      Long exonId = (Long)(ei.next());
      //if (!deletePolypeptide(exonId.longValue())) { return false; }
      if (!deleteExon(exonId, featureId, checkExons)) {
        return false;
      }
    }  
    
    
    // 4. delete transcript (and all referencing rows)
    if (!this.jdbcAdapter.deleteRow("feature", featureId)) {
      return false;
    }

    return true;
  }

  protected boolean deleteCDS(long featureId) {
    // 1. find and delete linked polypeptide features
    List plist = this.jdbcAdapter.getCdsPolypeptideIds(new Long(featureId));
    logger.debug("retrieved " + plist.size() + " polypeptide features linked to CDS with id " + featureId);
    Iterator pi = plist.iterator();
    while (pi.hasNext()) {
      Long pepId = (Long)(pi.next());
      if (!deletePolypeptide(pepId.longValue())) { return false; }
    }

    // 2. delete CDS (and all referencing rows)
    return this.jdbcAdapter.deleteRow("feature", featureId);
  }

  protected boolean deletePolypeptide(long featureId) {
    // delete polypeptide (and all referencing rows)
    return this.jdbcAdapter.deleteRow("feature", featureId);
  }

  protected boolean deleteGene(long featureId) {
    int rowcount = 0;

    // 1. get list of transcripts in the gene
    List tlist = this.jdbcAdapter.getGeneTranscriptIds(new Long(featureId));
    Iterator ti = tlist.iterator();

    // 2. delete each transcript - these shouldn't be shared with any other gene
    while (ti.hasNext()) {
      Long transId = (Long)(ti.next());
      if (!deleteTranscript(transId.longValue())) { return false; }
    }

    // 3. delete the gene itself (and all referencing rows)
    return this.jdbcAdapter.deleteRow("feature", featureId);
  }  
  
  // ----------------------------------
  // UPDATE
  // ----------------------------------

  protected boolean commitUpdateTransaction(String transId, UpdateTransaction ut) {
    SeqFeatureI feature = ut.getUpdatedSeqFeatureClone();
    TransactionSubpart subpart = ut.getSubpart();
    Object oldVal = ut.getOldSubpartValue();
    Object newVal = ut.getNewSubpartValue();
    boolean updateSucceeded = false;
      
    // PEPTIDE_LIMITS (CDS and polypeptide)
    if (subpart == TransactionSubpart.PEPTIDE_LIMITS) {
      RangeI oldRange = null;
      if (oldVal instanceof RangeI) {
	oldRange = (RangeI)oldVal;
      } else {
	logger.error("oldVal for PEPTIDE_LIMITS update is not an instance of RangeI");
	return false;
      }
      if (!(newVal instanceof Transcript)) {
	logger.error("newVal for PEPTIDE_LIMITS update is not a Transcript");
	return false;
      }
        
      // update CDS and polypeptide, using CDS of feature specified in newVal
      Transcript trans = (Transcript)newVal;
      long featureId = getChadoFeatureId(trans);
      updateSucceeded = updateCdsFeatureLocation(featureId, trans, oldRange);
    }

    // LIMITS (exon, transcript, or gene)
    else if (subpart.isLimits()) {
      SeqFeatureI of = ut.getUpdatedSeqFeature();
      RangeI oldRange = null;
      if (oldVal instanceof RangeI) {
	oldRange = (RangeI)oldVal;
      } else {
	logger.error("oldVal for LIMITS update is not an instance of RangeI");
	return false;
      }
      if (newVal != null && logger.isDebugEnabled()) {
	// check for mismatch between feature and newVal coords
	// There is an issue if the boundaries have been changed before an update
	// transaction like a 'Merge'. We write this only when the DEBUG mode is enabled

	RangeI nr = (RangeI)newVal;
	if ((feature.getStart() != nr.getStart()) ||
	    (feature.getEnd() != nr.getEnd()) ||
	    (feature.getStrand() != nr.getStrand())) 
	  {
	    logger.trace("newVal " + newVal + " for LIMITS update does not match feature " + feature);
	    //return false;
	  }
      }

      // update featureloc of the specified feature
      if (feature.isExon() || feature.isTranscript() || feature.isProteinCodingGene()) {
	long featureId = getChadoFeatureId(feature);
	ChadoFeatureLoc newLoc = new ChadoFeatureLoc(feature, null, this.jdbcAdapter.getConnectionUsedForLastTransaction());
	updateSucceeded = updateFeatureLocation(featureId, oldRange, newLoc);
      }
    } 

    // NAME and ID (e.g., for merge and split events)
    else if ((subpart.isName() || subpart.isId())) {
      long featureId = getChadoFeatureId(feature);
      if (!(oldVal instanceof String) || !(newVal instanceof String)) {
	logger.error("oldVal and/or newVal is not a String in NAME or ID UpdateTransaction");
	return false;
      }

      String oldName = (String)oldVal;
      String newName = (String)newVal;

      // don't think the old value should ever be null:
      String cfi = feature.getId();
      String cfn = feature.getName();

      // check invariant: cloned feature should have same id/name as oldVal
      if (subpart.isId()) {
	if (!oldName.equals(cfi)) {
	  logger.error("oldVal (" + oldName + " != cloned feature id (" + cfi + ") in ID UpdateTransaction");
	  return false;
	}
      } else {
	if (!oldName.equals(cfn)) {
	  logger.error("oldVal (" + oldName + " != cloned feature name (" + cfn + ") in NAME UpdateTransaction");
	  return false;
	}
      }

      // do the update
      return updateFeatureName(featureId,newName,subpart.isName());
    }
    else if (subpart.isFinished() || subpart.isProblematic() || subpart.isComment()) {      
      AnnotatedFeatureI annot = feature.getAnnotatedFeature();
      long featureId = getChadoFeatureId(annot);
      String newValue = null;
      Long typeId = null;
      Integer rank = new Integer(0);
      ChadoFeatureProp dbFeatProp = null;
      Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
	
      if (subpart.isFinished()) {
	newValue = annot.getProperty("status");
	typeId = this.jdbcAdapter.getPropertyTypeCVTermId("status");
	dbFeatProp = new ChadoFeatureProp(new Long(featureId),typeId,rank, c);
      } else if(subpart.isProblematic()) {
	newValue = (new Boolean(annot.isProblematic())).toString();
	typeId = this.jdbcAdapter.getPropertyTypeCVTermId("problem");
	dbFeatProp = new ChadoFeatureProp(new Long(featureId),typeId,rank, c);
      } else if(subpart.isComment()) {
	
	String oldText = formatChadoCommentValue(ut.getOldComment());
		
	int index =  ut.getSubpartRank();
	Comment comment = (Comment) annot.getComments().get(index);
	newValue = formatChadoCommentValue(comment);
	String commentTerm = this.jdbcAdapter.getChadoInstance().getCommentPropertyTerm();
	typeId = this.jdbcAdapter.getPropertyTypeCVTermId(commentTerm);
	if (typeId == null) {
	  logger.warn("database has no "+commentTerm+" cvterm");
	  return false;
	}
		
	dbFeatProp = new ChadoFeatureProp(new Long(featureId),typeId,oldText, c);
	rank = new Integer(dbFeatProp.getRank());
		
      }
      if(newValue == null || typeId == null) {
	logger.info("newValue or typeId can not be null");
	return false;
      }
	
      return this.jdbcAdapter.updateFeaturepropRow(dbFeatProp.getFeaturepropId(), dbFeatProp, featureId,typeId.longValue(), newValue,rank.intValue());
    }
    else if(subpart.isType()) {
	
      long featureId = getChadoFeatureId(feature);
      // do the update
      Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
      ChadoFeature dbFeat = new ChadoFeature(new Long(featureId), c, true);
		
      String newType = (String)newVal;
      long typeId = this.jdbcAdapter.getFeatureCVTermId(newType).longValue();
	
      // leave these values unchanged
      String residues = dbFeat.getResidues();
      Long dbXrefId = dbFeat.getDbXrefId();
      long organismId = dbFeat.getOrganismId();
      String name = ut.getNewId() != null ? ut.getNewId() : dbFeat.getName();
      String uniquename =  ut.getNewId() != null ? ut.getNewId() : dbFeat.getUniquename();
      boolean isAnalysis = dbFeat.getIsAnalysis();
      boolean isObsolete = dbFeat.getIsObsolete();

      return this.jdbcAdapter.updateFeatureRow(featureId, dbFeat, dbXrefId, organismId, name, uniquename, 
					       residues, typeId, isAnalysis, isObsolete);

    }
    else {
      throw new UnsupportedOperationException("unsupported update transaction with subpart=" + subpart);
    }

    return updateSucceeded;
  }

  /**
   * Update the location of a transcript's CDS and polypeptide.
   *
   * @param featureId Chado feature id that corresponds to <code>transcript</code>
   * @param transcript Apollo transcript whose peptide has changed
   * @param oldLocn The range of the old CDS feature location.  MAY BE NULL.
   */
  protected boolean updateCdsFeatureLocation(long featureId, Transcript transcript, RangeI oldLocn) {
    int start = transcript.getStart();
    int end = transcript.getEnd();
    int strand = transcript.getStrand();
    int tfmin, tfmax;

    if (strand == -1) {
      tfmin = end - 1;
      tfmax = start;
    } else {
      tfmin = start - 1;
      tfmax = end;
    }

    // compute new CDS coordinates
    ChadoCds cc = getCdsCoordinatesFromTranscript(transcript, tfmin, tfmax);

    // get existing CDS feature from database
    Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
    if(useCDS) {
        List clist = this.jdbcAdapter.getTranscriptCdsIds(new Long(featureId));
        int nc = clist.size();

        if (nc == 0) {
          logger.error("transcript with feature_id=" + featureId + " has no linked CDS to update");
          return false;
        } else if (nc > 1) {
          // TODO - support Transcripts with multiple CDS features (not sure that Apollo allows this)
          logger.error("transcript with feature_id=" + featureId + " has multiple linked CDSs");
          return false;
        }

        // updating the CDS entails changing both the featureloc and the feature (due to the sequence change)
        Long cdsId = (Long)(clist.get(0));
        ChadoFeatureLoc newCdsLoc = new ChadoFeatureLoc(cc.getFmin(), cc.getFmax(), cc.getFminPartial(), cc.getFmaxPartial(), cc.getStrand());
        if (!updateFeatureLocation(cdsId.longValue(), oldLocn, newCdsLoc)) {
          logger.error("failed to update CDS location for feature_id=" + cdsId);
          return false;
        }

        // update CDS sequence/residues

        ChadoFeature dbCdsFeat = new ChadoFeature(cdsId, c, true);

        // updated residues, length and md5checksum
        String cdsResidues = transcript.getCodingDNA();

        // other values are left unchanged
        Long cdsDbXrefId = dbCdsFeat.getDbXrefId();
        long cdsOrganismId = dbCdsFeat.getOrganismId();
        String cdsName = dbCdsFeat.getName();
        String cdsUniquename = dbCdsFeat.getUniquename();
        long cdsTypeId = dbCdsFeat.getTypeId();
        boolean cdsIsAnalysis = dbCdsFeat.getIsAnalysis();
        boolean cdsIsObsolete = dbCdsFeat.getIsObsolete();

        if (!this.jdbcAdapter.updateFeatureRow(cdsId.longValue(), dbCdsFeat, cdsDbXrefId, cdsOrganismId, cdsName, cdsUniquename, 
                                           cdsResidues, cdsTypeId, cdsIsAnalysis, cdsIsObsolete)) 
          {
            return false;
          }
    }
    
    // find linked polypeptide feature
    List plist = this.jdbcAdapter.getTranscriptPolypeptideIds(new Long(featureId));
    int np = plist.size();

    if (np == 0) {
      logger.error("Transcript with feature_id=" + featureId + " has no linked polypeptide to update");
      return false;
    } else if (np > 1) {
      // TODO - support CDSs with multiple polypeptide features (not sure Apollo even allows this)
      // normally not possible ?
      logger.error("Transcript with feature_id=" + featureId + " has multiple linked polypeptides");
      return false;
    }

    // update polypeptide location
    Long pepId = (Long)(plist.get(0));
    ChadoFeatureLoc newPepLoc = new ChadoFeatureLoc(cc.getFmin(), cc.getFmax(), cc.getFminPartial(), cc.getFmaxPartial(), cc.getStrand());
    if (!updateFeatureLocation(pepId.longValue(), oldLocn, newPepLoc)) {
      logger.error("failed to update polypeptide location for feature_id=" + pepId);
      return false;
    }

    // update polypeptide sequence/residues
    ChadoFeature dbPepFeat = new ChadoFeature(pepId, c, true);
    Protein pfeat = transcript.getProteinFeat();
    if (pfeat == null) {
      fatalError("transcript " + transcript.getName() + " has no Protein feature");
      return false;
    }

    // updated residues, length and md5checksum
    String pepResidues = getChadoResidues(pfeat);

    // other values are left unchanged
    Long pepDbXrefId = dbPepFeat.getDbXrefId();
    long pepOrganismId = dbPepFeat.getOrganismId();
    String pepName = dbPepFeat.getName();
    String pepUniquename = dbPepFeat.getUniquename();
    long pepTypeId = dbPepFeat.getTypeId();
    boolean pepIsAnalysis = dbPepFeat.getIsAnalysis();
    boolean pepIsObsolete = dbPepFeat.getIsObsolete();

    if (!this.jdbcAdapter.updateFeatureRow(pepId.longValue(), dbPepFeat, pepDbXrefId, pepOrganismId, pepName, pepUniquename, 
                                           pepResidues, pepTypeId, pepIsAnalysis, pepIsObsolete)) 
      {
        return false;
      }

    return true;
  }

  /**
   * Update the location of a gene-related feature, e.g. exon, transcript, or gene
   */
  protected boolean updateFeatureLocation(long featureId, RangeI oldLocn, ChadoFeatureLoc newLoc) {
    Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
    // get current featureloc from the database
    ChadoFeatureLoc dbFeatLoc = new ChadoFeatureLoc(featureId, c);
    // source feature doesn't change
    ChadoFeature srcFeature = dbFeatLoc.getSourceFeature();
    Long srcFeatureId = new Long(srcFeature.getFeatureId());
    
    // TODO - additional checks that we _might_ want to do:
    //   -is featLoc == oldLocn; if not, has the database been changed under us?
    //   -is featLoc == database contents; if so, then no update required?
    //   (latter handled by JdbcChadoAdapter?)
    
    // new featureloc values
    Integer fmin = newLoc.getFmin();
    Integer fmax = newLoc.getFmax();
    Integer strand = newLoc.getStrand();

    // the feature types we're dealing with shouldn't have partial locations
    boolean fmin_partial = newLoc.getFminPartial();
    boolean fmax_partial = newLoc.getFmaxPartial();

    // residueInfo not applicable to exons, transcripts or genes
    String residueInfo = null;
    // phase is not set--even for exons--because it could depend on the splice form
    Integer phase = null;
    int locgroup = getChadoLocgroup(featureId, srcFeature);
    // these features have only one location, so featureloc.rank does not apply:
    int rank = 0;
    
    long featurelocId = dbFeatLoc.getFeaturelocId();
    if (featurelocId <= 0) {
      logger.error("updateFeatureLocation retrieved featureloc_id=" + featurelocId + " for feature_id=" + featureId);
      return false;
    }

    return this.jdbcAdapter.updateFeaturelocRow(featurelocId, dbFeatLoc, featureId, srcFeatureId, fmin, fmax, 
                                                fmin_partial, fmax_partial, strand, phase, residueInfo, locgroup, rank);
  }

  /**
   * Update the name 
   */
  protected boolean updateFeatureName(long featureId, String newName, boolean isName) {
  
      Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
      ChadoFeature dbFeat = new ChadoFeature(new Long(featureId), c, true);
      
      String uniquename = dbFeat.getUniquename();
      String name = dbFeat.getName();
      if(isName) {
         name = newName;
      } else {
         uniquename = newName;
      }
      // leave these values unchanged
      
      String residues = dbFeat.getResidues();
      Long dbXrefId = dbFeat.getDbXrefId();
      long organismId = dbFeat.getOrganismId();
      long typeId = dbFeat.getTypeId();
      boolean isAnalysis = dbFeat.getIsAnalysis();
      boolean isObsolete = dbFeat.getIsObsolete();

      return this.jdbcAdapter.updateFeatureRow(featureId, dbFeat, dbXrefId, organismId, newName, uniquename, 
					       residues, typeId, isAnalysis, isObsolete);
  }


  /**
   * Update the type 
   */
  protected boolean updateFeatureType(long featureId, String newType) {
  
      Connection c = this.jdbcAdapter.getConnectionUsedForLastTransaction();
      ChadoFeature dbFeat = new ChadoFeature(new Long(featureId), c, true);
      
      long typeId = this.jdbcAdapter.getFeatureCVTermId(newType).longValue();
	
      // leave these values unchanged
      String uniquename = dbFeat.getUniquename();
      String name = dbFeat.getName();
      String residues = dbFeat.getResidues();
      Long dbXrefId = dbFeat.getDbXrefId();
      long organismId = dbFeat.getOrganismId();
      boolean isAnalysis = dbFeat.getIsAnalysis();
      boolean isObsolete = dbFeat.getIsObsolete();

      return this.jdbcAdapter.updateFeatureRow(featureId, dbFeat, dbXrefId, organismId, name, uniquename, 
					       residues, typeId, isAnalysis, isObsolete);
  }

  // -----------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------

  // Placing error reporting code into methods to make it easier to change the behavior:

  protected void fatalError(String error) {
    // what is fatal for the transaction writer may not be fatal for the application as a whole:
    logger.error(error);
    if (this.jdbcAdapter.hasOpenTransaction()) {
      logger.info("rolling back to last commit");
      this.jdbcAdapter.rollbackTransaction();
      logger.info("rollback complete");
    }
  }

  /**
   * Retrieve the chado primary key value for a given feature, or -1 if the feature does
   * not exist in the database.
   *
   * TODO - move this into the name adapter; could be different for different sites/dbs.  see earlier comments
   */
  protected long getChadoIdForFeature(SeqFeatureI f) {
    String uniquename = f.getId();
    long featureId = jdbcAdapter.getFeatureId(uniquename);

    /*
    if (featureId == -1) {
      fatalError("failed to retrieve chado feature_id for " + f);
    }
    */
    
    return featureId;
  }

  /**
   * Figure out chado CDS coordinates and sequence based on the Apollo transcript.
   *
   * In Apollo the CDS feature coordinates are genomic coordinates corresponding to the 1st 
   * base of the start codon and the 1st base of the stop codon, respectively
   *
   */
  protected ChadoCds getCdsCoordinatesFromTranscript(Transcript transcript, int tfmin, int tfmax) {
    int ts = transcript.getTranslationStart();
    int te = transcript.getTranslationEnd();
    int tstrand = transcript.getStrand();

    logger.debug("getCdsCoordinatesFromTranscript ts=" + ts + " te=" + te);

    // first convert to chado coords
    int cfmin, cfmax;
    boolean cfmin_partial = false, cfmax_partial = false;

    if (tstrand == -1) {
      cfmin = te - 1;
      cfmax = ts;
    } else {
      cfmin = ts - 1;
      cfmax = te;
    }

    logger.debug("getCdsCoordinatesFromTranscript cfmin=" + cfmin + " cfmax=" + cfmax);

    // check that CDS coords are within the transcript
    // TODO - also check that CDS coords fall within exons
    boolean cfminValid = (cfmin >= tfmin) && (cfmin <= tfmax);
    boolean cfmaxValid = (cfmax >= tfmin) && (cfmax <= tfmax);

    if (!cfminValid) {
      logger.warn("CDS fmin=" + cfmin + " out of range for " + transcript.getName() + " [" + tfmin + "-" + tfmax + "]");
      cfmin = tfmin;
    }
    if (!cfmaxValid) {
      logger.warn("CDS fmax=" + cfmax + " out of range for " + transcript.getName() + " [" + tfmin + "-" + tfmax + "]");
      cfmax = tfmax;
    }

    // adjust CDS *end* coordinate because in Apollo the end coordinate = 1st base of stop codon
    // and in chado the end coordinate = 3rd base of stop codon

    // TODO - handle case where stop codon crosses a splice boundary, in which case a simple +/-2 adjustment is incorrect
    if (tstrand == -1) {
      if (cfminValid) 
        cfmin = cfmin - 2; 
      else 
        cfmin_partial = true;
    } else {
      if (cfmaxValid) 
        cfmax = cfmax + 2;
      else 
        cfmax_partial = true;
    }

    // check again
    cfminValid = (cfmin >= tfmin) && (cfmin <= tfmax);
    cfmaxValid = (cfmax >= tfmin) && (cfmax <= tfmax);
    if (!cfminValid) logger.warn("after adjustment CDS fmin=" + cfmin + " out of range for " + transcript.getName() + " [" + tfmin + "-" + tfmax + "]");
    if (!cfmaxValid) logger.warn("after adjustment CDS fmax=" + cfmax + " out of range for " + transcript.getName() + " [" + tfmin + "-" + tfmax + "]");

    // using dummy values; these may not have been determined yet
    String cdsName = "";
    String cdnaSeq = "";
    String proteinName = "";
    String proteinSeq = "";

    logger.debug("getCdsCoordinatesFromTranscript creating ChadoCds with cfmin=" + cfmin + " cfmax=" + cfmax);

    return new ChadoCds(cdsName, cdnaSeq, proteinName, proteinSeq, cfmin, cfmax, cfmin_partial, cfmax_partial, tstrand);
  }
  
  /** Add all synonyms from an AnnotatedFeatureI object
   * 
   * @param annot - annotation to store synonyms from
   */
  protected void addSynonyms(AnnotatedFeatureI annot)
  {
    long annotId = getChadoFeatureId(annot);

    //add synonyms
    for (Object o : annot.getSynonyms()) {
      addSynonym(annotId, ((Synonym)o).getName());
    }
  }

  /** Add a synonym to the database
   * 
   * @param annotId - Chado ID for owner annotation
   * @param name - Synonym
   * @return true if successful
   */
  protected boolean addSynonym(long annotId, String name)
  {
    System.out.println("Adding synonym: " + name);
    if (name == null) {
      logger.error("Can't insert a synonym with a name null");
      return false;
    }

    String synonymTerm = jdbcAdapter.getChadoInstance().getSynonymPropertyTerm();
    Long synonymTypeId = jdbcAdapter.getPropertyTypeCVTermId(synonymTerm);
    if (synonymTypeId == null) {
      logger.warn("database has no "+ synonymTerm +" cvterm");
      return false;
    }
    Long synId = jdbcAdapter.insertSynonymRowIfNeeded(name, synonymTypeId, name);
    if (synId == null) {
      return false;
    }
    int pubId = 1;
    boolean isCurrent = true;
    boolean isInternal = false;
    Long annotSynId = jdbcAdapter.insertFeatureSynonymRow(synId,
        annotId, pubId, isCurrent, isInternal);
    if (annotSynId == null) {
      return false;
    }
    return true;
  }

  /** Delete a synonym from the database
   * 
   * @param annotId - Chado ID for owner annotation
   * @param name - Synonym
   * @return true if successful
   */
  protected boolean deleteSynonym(long annotId, String name)
  {
    System.out.println("Deleting synonym: " + name);
    String synonymTerm = jdbcAdapter.getChadoInstance().getSynonymPropertyTerm();
    Long synonymTypeId = jdbcAdapter.getPropertyTypeCVTermId(synonymTerm);
    if (synonymTypeId == null) {
      logger.warn("database has no "+ synonymTerm +" cvterm");
      return false;
    }
    Long synonymId = this.jdbcAdapter.getSynonymId(name ,synonymTypeId);
    if (this.jdbcAdapter.deleteFeatureSynonymRow(new Long(annotId), synonymId, 1)) {
      this.jdbcAdapter.deleteSynonymRowIfNoLongerNeeded(synonymId);
      return true;
    }
    return false;
  }

  /** Add all comments from an AnnotatedFeatureI object
   * 
   * @param annot - annotation to store comments from
   */
  protected void addComments(AnnotatedFeatureI annot)
  {
    long annotId = getChadoFeatureId(annot);

    //add comments
    for (Object o : annot.getComments()) {
      addComment(annotId, (Comment)o);
    }
  }
  
  /** Add a comment to the database
   * 
   * @param annotId - Chado ID for owner annotation
   * @param comment - Comment object
   * @return true if successful
   */
  protected boolean addComment(long annotId, Comment comment)
  {
    System.out.println("Adding comment: " + comment.getText());
    if(comment == null) {
      logger.error("Can't insert a comment with a value null");
      return false;
    }

    String fullComment = formatChadoCommentValue(comment);
  
    String commentTerm = jdbcAdapter.getChadoInstance().getCommentPropertyTerm();
    Long commentTypeId = jdbcAdapter.getPropertyTypeCVTermId(commentTerm);
    if (commentTypeId == null) {
      logger.warn("database has no "+commentTerm+" cvterm");
      return false;
    }
  
    String ownerTerm = jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
    Long ownerTypeId = jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
    if (ownerTypeId == null) {
      logger.warn("database has no '"+ownerTerm+"' cvterm;");
      return false;
    }
    boolean checkNextRank = true;
    long rank = 0;
    Long commentFpId = jdbcAdapter.insertFeaturepropRow(annotId, commentTypeId, fullComment, rank,checkNextRank);
    if (commentFpId == null) {
      return false;
    }
        
    Long pubId = this.jdbcAdapter.insertPubRowIfNeeded(comment.getPerson(), ownerTypeId.longValue());
    if (pubId == null) {
      return false;
    }
  
    Long commentFppubId = this.jdbcAdapter.insertFeaturepropPubRow(commentFpId,pubId);
    if (commentFppubId == null) {
      return false;
    }

    return true;
  }
  
  protected boolean deleteComment(long annotId, Comment comment)
  {
    System.out.println("Deleting comment: " + comment.getText());
    String value = formatChadoCommentValue(comment);
    String commentTerm = jdbcAdapter.getChadoInstance().getCommentPropertyTerm();
    Long typeId = jdbcAdapter.getPropertyTypeCVTermId(commentTerm);
    if (typeId == null) {
      logger.warn("database has no " + commentTerm + " cvterm");
      return false;
    }
    Connection c = jdbcAdapter.getConnectionUsedForLastTransaction();
    ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(annotId, typeId, value, c);
    if(dbFeatProp != null) { 
      return this.jdbcAdapter.deleteFeaturepropRow(new Long(dbFeatProp.getFeaturepropId()));
    }         
    return false;
  }
  
  protected boolean updateStatus(long annotId, String statusTerm, String newStatus, String oldStatus)
  {
    Long typeId = jdbcAdapter.getPropertyTypeCVTermId(statusTerm);
    if (typeId == null) {
      logger.warn("database has no " + statusTerm + " cvterm");
      return false;
    }
    Connection c = jdbcAdapter.getConnectionUsedForLastTransaction();
    ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(annotId, typeId, oldStatus, c);
    int rank = dbFeatProp.getRank();
    if(dbFeatProp != null) { 
      logger.info("updating property '" + statusTerm + "' to '" + newStatus + "' from '" + oldStatus+"'");
      return this.jdbcAdapter.updateFeaturepropRow(dbFeatProp.getFeaturepropId(),dbFeatProp,annotId, typeId, newStatus, rank);
    }         
    return false;
  }  
  
  protected boolean updateOwner(long annotId,String newStatus, String oldStatus)
  {
    String ownerTerm =
      this.jdbcAdapter.getChadoInstance().getFeatureOwnerPropertyTerm();
    Long ownerTypeId = this.jdbcAdapter.getPropertyTypeCVTermId(ownerTerm);
    if (ownerTerm == null) {
      logger.warn("database has no owner term");
      return false;
    }
    Connection c = jdbcAdapter.getConnectionUsedForLastTransaction();
    if (newStatus == null) {
      ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(annotId, ownerTypeId,
          oldStatus, c);
      if(dbFeatProp != null) { 
        return this.jdbcAdapter.deleteFeaturepropRow(dbFeatProp.getFeaturepropId());
      }
      else {
        return(true);
      }
    }

    if (oldStatus == null) {        
      /* Check if we do not have a Feature prop with  null as author */
      ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(annotId, ownerTypeId,0, c);
      if (dbFeatProp.getFeaturepropId() != 0) { 
        /* Updating the null value to newStatus */
        int rank = dbFeatProp.getRank();
        return this.jdbcAdapter.updateFeaturepropRow(dbFeatProp.getFeaturepropId(),dbFeatProp,annotId,
            ownerTypeId, newStatus, rank);
      }
      else {
        /* Otherwise we are creating a new entry */
        Long ownerId = this.jdbcAdapter.insertFeaturepropRow(annotId, ownerTypeId,
            newStatus, 0);
        if (ownerId == null) {
          return false;
        }
        else {
          return true;
        }
      }
    }

    if (!newStatus.equals(oldStatus)) {
      ChadoFeatureProp dbFeatProp = new ChadoFeatureProp(annotId, ownerTypeId,
          oldStatus, c);
      if(dbFeatProp != null) { 
        int rank = dbFeatProp.getRank();
        return this.jdbcAdapter.updateFeaturepropRow(dbFeatProp.getFeaturepropId(),dbFeatProp,annotId,
            ownerTypeId, newStatus, rank);
      }
      else {
        Long ownerId = this.jdbcAdapter.insertFeaturepropRow(annotId, ownerTypeId, newStatus, 0);
        if (ownerId == null) {
          return false;
        }
        else {
          return true;
        }
      }
    }
    return false;
  }
  
  protected String getChadoCdsName(ApolloNameAdapterI nameAdapter, String name)
  {
    return nameAdapter.generateChadoCdsNameFromTranscriptName(name);
  }
  
  protected String getChadoPeptideName(ApolloNameAdapterI nameAdapter, String name, SeqFeatureI pfeat)
  {
    String pname = nameAdapter.generatePeptideNameFromTranscriptName(name);
    pfeat.setId(pname);
    pfeat.setName(pname);
    return pname;
  }
}
