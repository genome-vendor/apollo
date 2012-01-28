/*
 * Created on Oct 8, 2004
 *
 */
package apollo.dataadapter.chado;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import apollo.dataadapter.TransactionTransformer;
import apollo.dataadapter.TransactionTransformException;
import apollo.dataadapter.chado.jdbc.ChadoInstance; // move to chado pkg
import apollo.datamodel.AbstractSequence;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.DbXref;
import apollo.datamodel.ExonI;
import apollo.datamodel.Range;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Synonym;
import apollo.datamodel.Transcript;
import apollo.editor.AddTransaction;
import apollo.editor.DeleteTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionSubpart;
import apollo.editor.UpdateTransaction;
import apollo.editor.UserName;
import apollo.dataadapter.chado.ChadoTransaction.Operation;

import org.apache.log4j.*;

/**
 * This class is used to translate Apollo Transaction objects to Chado Transaction 
 * objects.
 * @author wgm
 */
public class ChadoTransactionTransformer implements TransactionTransformer {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoTransactionTransformer.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // To list all feature lookup transaction. Only one is needed
  // key is feature's id, value is ChadoTransaction. id should be used instead of
  // feature references in the case of exon range change. The new range is shared
  // by another exon.
  private Map featureLookupTns;
  private Map nameToPubLookupTns;
  private String pub_id;
  private SimpleDateFormat dateFormat;
  // These two lists for controlling the feature levels
  // A list of one level features
  private List oneLevelAnnotTypes;
  // A list of three level features
  private List threeLevelAnnotTypes;
  private String polypeptideType = "polypeptide";
  private String transProtRelationTerm = "derives_from";
  //private ChadoInstance chadoInstance;
   
  public ChadoTransactionTransformer() {
    featureLookupTns = new HashMap();
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    oneLevelAnnotTypes = new ArrayList();
    threeLevelAnnotTypes = new ArrayList();
  }

  
  /** I think the incoming List of transactions are apollo editor Transactions.
   * These are transformed and the returned List is a list of Chado Transactions.
   This does not deal with CompoundTransactions. TransactionManager.coalesce 
   flattens out compounds so we shouldnt see them here. If that changes then
   they need to be dealt with here.
   */
  public List transform(TransactionManager transManager)
    throws TransactionTransformException {

    // this will work on flattened list (will flatten if need to)
    transManager.replaceIdUpdatesWithDelAndAdd();
    transManager.coalesce();

    List transactions = transManager.getTransactions();
    if (logger.isDebugEnabled()) {
      logger.debug("transform() called with " + transactions.size() + " flattened apollo transactions");
      int nt = transactions.size();
      for (int i = 0;i < nt;++i) {
	logger.debug("transaction " + i + ": " + transactions.get(i).toString());
      }
    }
    if (transactions == null || transactions.size() == 0)
      return new ArrayList(); // return an empty list

    List rtn = new ArrayList(transactions.size());
    Transaction tn = null;
    // Add a pub_id for the user - this is only needed for comments & syns
    // so this should be done on demand (especially since the pub issue is
    // problematic in fly/crm with "curator" & "curated genome annotation"
    //ChadoTransaction chadoTn = createUserTransaction();
    //if (chadoTn != null)
      //rtn.add(chadoTn);
    for (Iterator it = transactions.iterator(); it.hasNext();) { 
      tn = (Transaction) it.next();
      if (tn.isDelete()) {
        transformDeleteTransaction((DeleteTransaction)tn, rtn);
      }
      else if (tn.isAdd()) {
        transformAddTransaction((AddTransaction)tn, rtn);
      }
      else if (tn.isUpdate())
        transformUpdateTransaction((UpdateTransaction)tn, rtn);
    }
    if (logger.isDebugEnabled()) {
      int nt = rtn.size();
      logger.debug("transform() returning " + nt + " chado transactions:");
      for (int i = 0;i < nt;++i) {
	logger.debug("transaction " + i + ": " + rtn.get(i).toString());
      }
    }
    return rtn;
  }
  
  private void transformUpdateTransaction(UpdateTransaction updateTn,
                                          List transformedTns)
    throws TransactionTransformException{
    // UpdateTransaction should have a subpart
    if (updateTn.getSubpart() == null)
      throw new IllegalArgumentException(
          "ChadoTransactionTransformed.transformUpdateTransaction(): "
              + "UpdateTransaction should have suppart defined!");
    SeqFeatureI feature = updateTn.getSeqFeature();
    TransactionSubpart subpart = updateTn.getSubpart();

//     if (subpart == TransactionSubpart.SEQUENCING_ERROR) {
//       transformSeqError(updateTn); } --> ADD/DEl not UPDATE

    if (feature instanceof ExonI) { // For updating exon
      transformExonUpdateTransactions(feature, subpart, updateTn.getOldSubpartValue(),
                                      transformedTns);
    }
    else {
      Object preValue = updateTn.getOldSubpartValue();
      //TODO: How to update synonyms and comments?
      // For simple property, because there is no way to query if a pre-value
      // has already be saved, try to do a delete first, then insert new value
      if (subpart == TransactionSubpart.IS_DICISTRONIC) {
        boolean oldValue = ((Boolean) preValue).booleanValue();
        updateBooleanProperty("dicistronic", oldValue, feature, transformedTns);
      }
      else if (subpart == TransactionSubpart.IS_PROBLEMATIC) {
        boolean oldValue = ((Boolean) preValue).booleanValue();
        updateBooleanProperty("problem", oldValue, feature, transformedTns);
      }
      //How to save finished info? It seems not necessary?
      else if (subpart == TransactionSubpart.FINISHED) {
        boolean oldValue = ((Boolean) preValue).booleanValue();
        updateBooleanProperty("finished", oldValue, feature, transformedTns);
      }
//       else if (subpart == TransactionSubpart.SYNONYM) {
//         updateSynonym(preValue.toString(), feature, transformedTns);
//       }
      else if (subpart == TransactionSubpart.COMMENT) {
        updateComment(updateTn, transformedTns);
      }
      else if (subpart == TransactionSubpart.PEPTIDE_LIMITS) {
        // would be nice if this feature was a protein feature, but its
        // transcript with a protein seq (no prot feats in apollo! not yet)
        updateTranslationRange(feature, transformedTns);
      }
      else if (subpart == TransactionSubpart.PEPTIDE_NAME) {
        updatePeptideName(preValue.toString(),feature, transformedTns);
      }
      else if (subpart == TransactionSubpart.LIMITS) {
        updateFeatureRange(feature, transformedTns);
      }
      else if (subpart.isType()) {
        // need id before id change to do delete for type change
        updateFeatureType(preValue.toString(), feature, updateTn.getOldId(),
                          transformedTns);
      }
      else if (subpart == TransactionSubpart.NAME) {
        updateFeatureName(preValue.toString(), feature,  transformedTns);
      }
      else if (subpart == TransactionSubpart.ID) {
        updateFeatureID(preValue.toString(), feature, transformedTns);
      }
    }
  }
  
  /** I believe this isnt actually being used at the moment as updates to feature
      ids are changed into del&add in TransactionManager - which should probably 
      change at some point */
  private void updateFeatureID(String oldId, SeqFeatureI feature, List transformedTns) {
    ChadoUpdateTransaction updateTn = new ChadoUpdateTransaction();
    updateTn.setTableName("feature");
    updateTn.addProperty("uniquename", oldId);
    updateTn.addProperty("organism_id", "organism");
    updateTn.addProperty("type_id", getChadoType(feature));
    updateTn.addUpdateProperty("uniquename", feature.getId());
    transformedTns.add(updateTn);
  }

  
  private void updateFeatureName(String oldName, SeqFeatureI feature,
                                 List transformedTns) {
    String type = getChadoType(feature);
    ChadoUpdateTransaction updateTn =
      makeNameTransaction(feature.getId(),feature,type,oldName,feature.getName());
    transformedTns.add(updateTn);
  }

  /** as usual exons are different - exons are a pain! */
  private void updateExonFeatName(String oldName, SeqFeatureI exon, List trans) {
    String type = getChadoType(exon);
    String newName = getChadoExonName(exon);
    ChadoUpdateTransaction ut = makeNameTransaction(null,exon,type,oldName,newName);
    ut.removeProperty("uniquename"); // uniquename not used for exon
    ut.setID(newName); // i think this is needed and should be name for exons
    ut.setIsExon(true);
    trans.add(ut);
  }
    
  //ChadoUpdateTransaction makeNameTransaction(null,exon,
  

  private ChadoUpdateTransaction makeNameTransaction(String uniqueName,
                                                     SeqFeatureI feature,
                                                     String type, String oldName,
                                                     String newName) {
    ChadoUpdateTransaction updateTn = new ChadoUpdateTransaction();
    updateTn.setTableName("feature");
    updateTn.addProperty("uniquename", uniqueName);
    updateTn.addProperty("organism_id", "organism");
    updateTn.addProperty("type_id", type);
    // can we skip old name for prots? cos we dont have it
    if (oldName != null)
      updateTn.addProperty("name", oldName); 
    updateTn.addUpdateProperty("name", newName);
    return updateTn;
  }

  
  /** For now type updates cause a DELETE and ADD. Its actually really complicated 
      otherwise updating between types that have different amount of levels. */
  private void updateFeatureType(String oldType, SeqFeatureI feature, String featureId,
                                 List transformedTns)
    throws TransactionTransformException {

    // DELETE the old type
    ChadoTransaction transaction = generateTnFromOperation(ChadoTransaction.DELETE);
    transaction.setTableName("feature");
    // Value "organism" should be defined in the transaction xml template.
    // It is not a natural way to do this.
    transaction.addProperty("organism_id", "organism");
    // type change can cause id change
    transaction.addProperty("uniquename", featureId);//feature.getId());
    String type = null;
    if (oneLevelAnnotTypes.contains(oldType))
      transaction.addProperty("type_id", oldType);
    else // All three level features use "gene" as the root always
      transaction.addProperty("type_id", "gene");
    transformedTns.add(transaction);

    // INSERT/ADD the new type
    insertRootFeature(feature, transformedTns);

  }
  
  private void updateFeatureRange(SeqFeatureI feature, List transformedTns) {
    // Lookup feature_id
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(tn);
    }
    // Update fmin and fmax
    ChadoUpdateTransaction updateTn = new ChadoUpdateTransaction();
    updateTn.setTableName("featureloc");
    updateTn.addProperty("feature_id", feature.getId());
    updateTn.addProperty("strand", feature.getStrand() + "");
    int[] coord = convertToChadoCoord(feature);
    updateTn.addUpdateProperty("fmin", coord[0] + "");
    updateTn.addUpdateProperty("fmax", coord[1] + "");
    transformedTns.add(updateTn);      
  }
  
  private void updateTranslationRange(SeqFeatureI feature,
                                       List transformedTns) {
    AbstractSequence sequence = (AbstractSequence) feature.getPeptideSequence();
    if (sequence == null)
      return ; // Nothing to update
    String id = sequence.getAccessionNo();
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(id);
    if (tn == null) {
      tn = generateTnFromOperation(ChadoTransaction.UPDATE);
      tn.setTableName("feature");
      tn.setID(id);
      tn.addProperty("uniquename", id);
      tn.addProperty("organism_id", "organism");
      tn.addProperty("type_id", getPolypeptideType());
      ((ChadoUpdateTransaction)tn).addUpdateProperty("residues", sequence.getResidues());
      ((ChadoUpdateTransaction)tn).addUpdateProperty("seqlen", sequence.getLength() + "");
      transformedTns.add(tn);
      featureLookupTns.put(id, tn);
    }
    // Update featureloc.fmin, featureloc.fmax for peptide
    ChadoUpdateTransaction updateTn = new ChadoUpdateTransaction();
    updateTn.setTableName("featureloc");
    updateTn.addProperty("feature_id", id);
    updateTn.addProperty("strand", feature.getStrand() + "");
    AnnotatedFeature transcript = (AnnotatedFeature) feature;
    int[] coord = convertToChadoCoordForProtein(transcript);
    updateTn.addUpdateProperty("fmin", coord[0] + "");
    updateTn.addUpdateProperty("fmax", coord[1] + "");
    transformedTns.add(updateTn);  
  }

  /** for now chado peptides are carrying around uniquenames(RICE#) 
      not names(genename-peptide#), this may change, not sure 
      Change : now peptides will have theire name(genename-peptide#) + theire uniquename (RICE#) */
  private void updatePeptideName(String oldUniqueName, SeqFeatureI feature,
                                 List transformedTns) {
    
    //TODO transitional state,peptide become SeqFeature instead of Sequence
    //Get the peptide's feature name or the sequence if the SeqFeature is null
    String pepNewName=feature.getPeptideSequence().getAccessionNo();
    if (feature.getProteinFeat() != null){
      pepNewName=feature.getProteinFeat().getName();
    }
    ChadoUpdateTransaction cut =
      makeNameTransaction(oldUniqueName,feature,getPolypeptideType(),null,pepNewName);
    transformedTns.add(cut);
  }

  public void setChadoInstance(ChadoInstance instance) {
    //this.chadoInstance = instance;
    setPolypeptideType(instance.getPolypeptideType());
    setOneLevelAnnotTypes(instance.getOneLevelAnnotTypes());
    setThreeLevelAnnotTypes(instance.getThreeLevelAnnotTypes());
    transProtRelationTerm = instance.getTransProtRelationTerm();
  }

  public String getPolypeptideType() {
    return polypeptideType;
  }
  public void setPolypeptideType(String type) {
    polypeptideType = type;
  }
  
  // i think this doesnt happen
//   private void updateSynonym(String preValue,
//                              SeqFeatureI feature,
//                              List transformedTns) {
//     // make sure we have user pub trans
//     //addUserPubTransaction(chadoTransactions);
//  }
  
  private void updateComment(UpdateTransaction updateTn,
                             List transformedTns) {
    AnnotatedFeature feature = (AnnotatedFeature) updateTn.getSeqFeature();
    // Make sure there is a id defined for feature
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(tn);
    }
    int index = updateTn.getSubpartRank();
    Comment comment = (Comment) feature.getComments().get(index);
    String newValue = createChadoCommentValue(comment.getText(), comment);
    //String oldText = updateTn.getPreValue().toString();
    String oldText = updateTn.getOldComment().getText();
    String oldValue = createChadoCommentValue(oldText, comment);
    tn = new ChadoUpdateTransaction();
    tn.setTableName("featureprop");
    tn.addProperty("feature_id", feature.getId());
    tn.addProperty("type_id", "comment");
    tn.addProperty("value", oldValue);
    ((ChadoUpdateTransaction)tn).addUpdateProperty("value", newValue);
    transformedTns.add(tn);
  }
  
  private void updateBooleanProperty(String propName,
                                     boolean preValue,
                                     SeqFeatureI feature,
                                     List transformedTns) {
    // Need feature_id
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(tn);
    }
    // Delete the property first. This is a test run. If the prevalue
    // is not there, nothing should happen
    tn = generatePropertyTransaction(feature.getId(),
                                     propName,
                                     preValue + "",
                                     0,
                                     ChadoTransaction.DELETE);
    transformedTns.add(tn);
    // Insert new value
    tn = generatePropertyTransaction(feature.getId(),
                                     propName,
                                     !preValue + "",
                                     0,
                                     ChadoTransaction.INSERT);
    transformedTns.add(tn);
  }
  
  private void transformExonUpdateTransactions(SeqFeatureI feature, 
                                                TransactionSubpart subpart,
                                                Object preValue,
                                                List transformedTns) 
    throws TransactionTransformException {
    // Update Exon Range
    if (subpart == TransactionSubpart.LIMITS) {
      updateExonRange(feature, (Range)preValue, transformedTns);
    }
    else if (subpart == TransactionSubpart.PARENT) { // Move from one parent to antother
      updateExonParent(feature, (SeqFeatureI)preValue, transformedTns);
    }
  }
  
  /**
   * This method can be only applied to a intra-gene moving.
   * @param feature
   * @param oldParent
   * @param transformedTns
   */
  private void updateExonParent(SeqFeatureI feature, SeqFeatureI oldParent, List transformedTns) {
    SeqFeatureI newParent = feature.getRefFeature();
    // Need to lookup the exon first
    ChadoTransaction lookupTn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (lookupTn == null) {
      lookupTn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(lookupTn);
    }
    // Need new parent id
    lookupTn = (ChadoTransaction) featureLookupTns.get(newParent.getId());
    if (lookupTn == null) {
      lookupTn = generateFeatureTransaction(newParent, ChadoTransaction.LOOKUP);
      transformedTns.add(lookupTn);
    }
    lookupTn = (ChadoTransaction) featureLookupTns.get(oldParent.getId());
    if (lookupTn == null) {
      lookupTn = generateFeatureTransaction(oldParent, ChadoTransaction.LOOKUP);
      transformedTns.add(lookupTn);
    }
    // Update feature relationship for this exon
    ChadoUpdateTransaction updateTn = new ChadoUpdateTransaction();
    updateTn.setTableName("feature_relationship");
    updateTn.addProperty("subject_id", feature.getId());
    updateTn.addProperty("object_id", oldParent.getId());
    updateTn.addProperty("type_id", "partof");
    updateTn.addUpdateProperty("object_id", newParent.getId());
    transformedTns.add(updateTn);
  }

  private void updateExonRange(SeqFeatureI exon,Range oldRange,List transformedTns)
    throws TransactionTransformException {
    SeqFeatureI transcript =  exon.getRefFeature();
    // should this be id or name - i think it should be name??
    //Yes, it should be name (CP 01/12/2005)
    //String geneName = transcript.getRefFeature().getId();
    String geneName = transcript.getRefFeature().getName();
    String oldChadoName = getChadoExonName(geneName,oldRange);
    //geneName+":"+oldRange.getStart()+"-"+oldRange.getEnd();
    String newName = getChadoExonName(exon);//exon.getId(); // ???
    boolean isOldShared = isExonShared(transcript, oldRange.getStart(), oldRange.getEnd());
    boolean isNewShared = isExonShared(transcript, exon.getStart(), exon.getEnd());
    // Case I: Exon is not shared, new range is not shared with another exon
    // Need the old name for updating
    if (!isOldShared && !isNewShared) {
      // Update exon.uniquename, exon.seqlen
//       ChadoUpdateTransaction chadoTn = new ChadoUpdateTransaction();
//       chadoTn.setTableName("feature");
//       chadoTn.setID(newName);
//       //chadoTn.addProperty("uniquename", oldChadoName);
//       // scott changed this from uniquename to name
//       chadoTn.addProperty("name", oldChadoName);
//       chadoTn.addProperty("organism_id", "organism");
//       chadoTn.addProperty("type_id", "exon");
//       chadoTn.addUpdateProperty("name", newName); // was uniquename
//       chadoTn.setIsExon(true);
//      transformedTns.add(chadoTn);
      // makes update trans & adds it
      updateExonFeatName(oldChadoName,exon,transformedTns);

      // Update featureloc.fmin, featureloc.fmax
      ChadoUpdateTransaction chadoTn = new ChadoUpdateTransaction();
      chadoTn.setTableName("featureloc");
      chadoTn.addProperty("feature_id", newName);
      chadoTn.addProperty("strand", exon.getStrand() + "");
      int[] coord = convertToChadoCoord(exon);
      chadoTn.addUpdateProperty("fmin", coord[0] + "");
      chadoTn.addUpdateProperty("fmax", coord[1] + "");
      transformedTns.add(chadoTn);
    }
    // Case II: Old range is not shared, new range is shared
    else if (!isOldShared && isNewShared) {
      // Old exon should be deleted in feature table
      ChadoTransaction deleteExon = generateTnFromOperation(ChadoTransaction.DELETE);
      deleteExon.setTableName("feature");
      deleteExon.addProperty("name", oldChadoName); // was uniquename
      deleteExon.addProperty("type_id", "exon");
      deleteExon.addProperty("organism_id", "organism");
      transformedTns.add(deleteExon);
      // Find another exon shared by new range
      ChadoTransaction lookupExon = (ChadoTransaction) featureLookupTns.get(newName);
      if (lookupExon == null) {
        lookupExon = generateExonFeatureTransaction(exon,newName,ChadoTransaction.LOOKUP);
        transformedTns.add(lookupExon);
      }
      // need transcript id
      ChadoTransaction lookupTran = (ChadoTransaction) featureLookupTns.get(transcript);
      if (lookupTran == null) {
        lookupTran = generateFeatureTransaction(transcript, ChadoTransaction.LOOKUP);
        transformedTns.add(lookupTran);
      }
      // Add an entry to new shared exon for transcript
      ChadoTransaction addFeatRel = generateExonFeatRelTrans(exon,newName,transcript,
                                                              ChadoTransaction.INSERT);
      transformedTns.add(addFeatRel);
    }
    // Case III: Old range is shared, new range is not shared
    else if (isOldShared && !isNewShared) {
      // Lookup transcript
      ChadoTransaction lookupTran = (ChadoTransaction) featureLookupTns.get(transcript.getId());
      if (lookupTran == null) {
        lookupTran = generateFeatureTransaction(transcript, ChadoTransaction.LOOKUP);
        transformedTns.add(lookupTran);
      }
      // Lookup old exon
//ChadoTransaction lookupExon=(ChadoTransaction)featureLookupTns.get(oldChadoName);
      //if (lookupExon == null) {
      if (!haveLookupTransaction(oldChadoName)) {
        //exon.setId(oldChadoName); // id -> name
        //exon.setName(oldChadoName); // temp set, sets it back below - funny
        //lookupExon = generateFeatureTransaction(exon, ChadoTransaction.LOOKUP);
        ChadoTransaction lookupExon =
          generateExonFeatureTransaction(exon,oldChadoName,ChadoTransaction.LOOKUP);
        //exon.setName(newName);
        transformedTns.add(lookupExon);
      }
      // Delete a feature_relatinship for the old
      ChadoTransaction delFeatRel = generateTnFromOperation(ChadoTransaction.DELETE);
      delFeatRel.setTableName("feature_relationship");
      delFeatRel.addProperty("subject_id", oldChadoName);
      delFeatRel.addProperty("object_id", transcript.getId());
      delFeatRel.addProperty("type_id", "partof");
      transformedTns.add(delFeatRel);

      // Add a new exon
      insertExon(exon, transformedTns); // throws FeatLoc/Transform exception
    }
    // Case IV: Old range is shared, new range is shared too.
    else {
      // Lookup the old exon
      ChadoTransaction lookupExon = (ChadoTransaction) featureLookupTns.get(oldChadoName);
      if (lookupExon == null) {
        // Cheat the method
        exon.setId(oldChadoName);
        lookupExon = generateFeatureTransaction(exon, ChadoTransaction.LOOKUP);
        transformedTns.add(lookupExon);
        exon.setId(newName); // Use the new name
      }
      // Lookup the new exon
      lookupExon = (ChadoTransaction) featureLookupTns.get(newName);
      if (lookupExon == null) {
        lookupExon = generateFeatureTransaction(exon, ChadoTransaction.LOOKUP);
        transformedTns.add(lookupExon);
      }
      // Lookup the transcript
      ChadoTransaction lookupTran = (ChadoTransaction) featureLookupTns.get(transcript);
      if (lookupTran == null) {
        lookupTran = generateFeatureTransaction(transcript, ChadoTransaction.LOOKUP);
        transformedTns.add(lookupTran);
      }
      // Update the feature_relationship table
      ChadoUpdateTransaction update = new ChadoUpdateTransaction();
      update.setTableName("feature_relationship");
      update.addProperty("object_id", transcript.getId());
      update.addProperty("subject_id", oldChadoName);
      update.addProperty("type_id", "partof");
      update.addUpdateProperty("subject_id", newName);
      transformedTns.add(update);
    }
  }
  
  private void transformAddSubpartTransaction(AddTransaction tn,
                                              List transformedTns) {
    SeqFeatureI feature = tn.getSeqFeature();
    ChadoTransaction chadoTn = null;
    TransactionSubpart subpart = tn.getSubpart();
    Object newValue = tn.getNewSubpartValue();
    if (subpart == TransactionSubpart.SYNONYM) {
      addSynonymTransactions(feature.getAnnotatedFeature(),
                             0,
                             newValue.toString(),
                             transformedTns);
    }
    else if (subpart == TransactionSubpart.COMMENT) {
      addCommentsTransaction(feature.getAnnotatedFeature(),
                             tn.getSubpartRank(),
                             (Comment)newValue,
                             transformedTns);
    }
  }

  private void transformAddTransaction(AddTransaction tn,
                                        List transformedTns)
    throws TransactionTransformException {
    if (tn.getSubpart() != null) {
      transformAddSubpartTransaction(tn, transformedTns);
      return;
    }

    SeqFeatureI feature = tn.getSeqFeature();

    // for seq errors we need a feature that represents the curation set!
    // at the moment it currently holds the exon
    // --> insertRootFeature??
//     if (tn.isAddSeqError()) {
//       addSequenceingError(tn);
//     }

    if (tn.isAddPeptide()) {
      // add peptide carries the transcript not the peptide seq
      addProteinTransactions(feature.getAnnotatedFeature(), transformedTns);
    }

    //ChadoTransaction chadoTn = null;
    //try {
    else if (feature.isExon()) { // Add an exon
      insertExon(feature, transformedTns);
    }
    else if (feature.isTranscript()) {
      insertTranscript(feature, transformedTns, true);
    }
    else { // Add a gene, one level annot, seq error
      // seqErrors insertion & substition include residues
      String residues = tn.getResidues(); // returns null if have none
      insertRootFeature(feature, residues, transformedTns);
    }
    //}
//     catch (RangeException re) {
//       System.out.println("Error: not inserting feature due to "+re.getMessage());
//       if (debug)
//         re.printStackTrace();
//     }
  }
  
  private void insertRootFeature(SeqFeatureI f, List chadoTrans)
    throws TransactionTransformException {
    insertRootFeature(f,null,chadoTrans); // null residues
  }

  /** Does add of root/top annot (gene,transposon,seqerror...)
   adds feat & feat loc insert ChadoTransactions to transformedTns
  Residues is for insertions & substitutions residue - no others use it */
  private void insertRootFeature(SeqFeatureI feature, String residues,
                                 List transformedTns) 
    throws TransactionTransformException {
    String type = feature.getFeatureType();
    if (type.equals("miscellaneous curator's observation"))
      type = "remark";
    // seq errors should be configured as one level type
    if (oneLevelAnnotTypes != null && oneLevelAnnotTypes.contains(type))
      insertOneLevelFeature(feature, residues, transformedTns);
    else
      insertGene(feature, transformedTns);    
  }
  
  private void insertOneLevelFeature(SeqFeatureI f, List chadoTrans) 
    throws TransactionTransformException {
    insertOneLevelFeature(f,null,chadoTrans); // null residues
  }

  /** feature is a one level annot, residues is for seq of insertion & substition.
      chado add feature & feat loc transaction added to transforemd tns */
  private void insertOneLevelFeature(SeqFeatureI feature, String residues,
                                     List transformedTns) 
    throws TransactionTransformException {
    // One level features will be added on AnnotatiedFeature level
    ChadoTransaction chadoTn;
    AnnotatedFeatureI annotFeat = feature.getAnnotatedFeature();
    // im confused - doesnt a feat_rel need to be inserted?
    // Add gene to feature table
    chadoTn = generateFeatureTransaction(annotFeat, residues, ChadoTransaction.INSERT);
    transformedTns.add(chadoTn);
    // Add to featureloc table, throws FeatLoc TransformException
    chadoTn = generateFeatureLocTransaction(annotFeat, 
                                           ChadoTransaction.INSERT);
    transformedTns.add(chadoTn);
    addCommentsTransaction(annotFeat, transformedTns);
    addSynTransForNewAnnot(annotFeat, transformedTns);
    addDbxrefTransactions(annotFeat.getDbXrefs(), transformedTns, feature.getId());
    addPropsTransactions(annotFeat, transformedTns);    
  }
  
  // rename - may not be gene?
  private void insertGene(SeqFeatureI feature, List transformedTns) 
    throws TransactionTransformException {
    insertOneLevelFeature(feature, transformedTns);
    ChadoTransaction chadoTn;
    AnnotatedFeature annotFeat = (AnnotatedFeature) feature;
    // Add transcripts contained by annotFeat
    List transcripts = annotFeat.getFeatures();
    if (transcripts.size() == 0)
      logger.debug("gene has 0 transcripts to insert, very suspicious");
    for (Iterator it = transcripts.iterator(); it.hasNext();) {
      SeqFeatureI feat = (SeqFeatureI) it.next();
      if (feat.isTranscript())
        // Exons will be handled later
        insertTranscript(feat, transformedTns, false);
    }
    // add exons
    List exons = getExonsFromAnnotFeat(annotFeat);
    for (Iterator it = exons.iterator(); it.hasNext();) {
      SeqFeatureI exon = (SeqFeatureI) it.next();
      // Add to feature table
      //chadoTn = generateFeatureTransaction(exon, ChadoTransaction.INSERT);
      // need to use generated chado name as exon ids can get out of synch with
      // merging & duplicating (annot editor doesnt & shouldnt maintatin them)
      String chadoExonName = getNewChadoExonName(exon);
      chadoTn = generateExonFeatureTransaction(exon,chadoExonName,ChadoTransaction.INSERT);
      transformedTns.add(chadoTn);
      // Add to featureloc table - throws FeatLoc Transform Exception
      //chadoTn = generateFeatureLocTransaction(exon, ChadoTransaction.INSERT);
      chadoTn = genExonFeatLocTransaction(exon,chadoExonName,ChadoTransaction.INSERT);
      transformedTns.add(chadoTn);
    }
    // Add exons to transcripts
    for (Iterator it = transcripts.iterator(); it.hasNext();) {
      SeqFeatureI transcript = (SeqFeatureI) it.next();
      if (transcript.isTranscript()) {
        for (Iterator it1 = transcript.getFeatures().iterator(); it1.hasNext();) {
          SeqFeatureI exon = (SeqFeatureI) it1.next();
          if (exon.isExon()) {
            //chadoTn = generateFeatureRelTransaction(exon, feat, ChadoTransaction.INSERT);
            String chadoExonName = getNewChadoExonName(exon);
            chadoTn = generateExonFeatRelTrans(exon,chadoExonName,transcript,
                                               ChadoTransaction.INSERT);
            transformedTns.add(chadoTn);
          }
        }
      }
    }
  }

  /**
   * A helper method to feature a list of exons in that no two exons share the same range.
   * @param feature
   * @return
   */
  private List getExonsFromAnnotFeat(AnnotatedFeature gene) {
    Map exons = new HashMap();
    SeqFeatureI feat = null;
    for (Iterator it = gene.getFeatures().iterator(); it.hasNext();) {
      feat = (SeqFeatureI) it.next();
      if (feat instanceof Transcript) {
        for (Iterator it1 = feat.getFeatures().iterator(); it1.hasNext();) {
          SeqFeatureI feat1 = (SeqFeatureI) it1.next();
          if (feat1 instanceof ExonI) {
            String key = feat1.getStart() + "-" + feat1.getEnd();
            exons.put(key, feat1);
          }
        }
      }
    }
    return new ArrayList(exons.values());
  }
  
  private void insertTranscript(SeqFeatureI feature,List transformedTns,
                                boolean needExon) throws TransactionTransformException{
    ChadoTransaction chadoTn;
    Transcript transcript = (Transcript) feature;
    // Gene id is needed
    SeqFeatureI gene = transcript.getGene();
    chadoTn = (ChadoTransaction) featureLookupTns.get(gene.getId());
    if (chadoTn == null) {
      chadoTn = generateFeatureTransaction(gene, ChadoTransaction.LOOKUP);
      transformedTns.add(chadoTn);
    }
    // Add transcript to feature table
    chadoTn = generateFeatureTransaction(feature, ChadoTransaction.INSERT);
    transformedTns.add(chadoTn);
    // Add transcript to featureloc table -- throws FeatLocException if bad range
    try {
      chadoTn = generateFeatureLocTransaction(feature, ChadoTransaction.INSERT);
    } catch (FeatLocException fle) {
      logger.error("faulty featureLoc/Range. Disregarding rest of transcript insert", fle);
    }

    transformedTns.add(chadoTn);
    // Add transcript to feature_relationship table
    chadoTn = generateFeatureRelTransaction(feature, ChadoTransaction.INSERT);
    transformedTns.add(chadoTn);
    // comments
    addCommentsTransaction(transcript, transformedTns);
    // Add synonyms
    addSynTransForNewAnnot(transcript, transformedTns);
    // Add dbxref of transcript
    addDbxrefTransactions(transcript.getDbXrefs(), transformedTns, feature.getId());
    // Add properties of transcript
    addPropsTransactions(transcript, transformedTns);
    if (needExon) {
      // Add exons contained by transcript
      List exons = transcript.getFeatures();
      for (Iterator it = exons.iterator(); it.hasNext();) {
        SeqFeatureI feat = (SeqFeatureI) it.next();
        if (feat.isExon())
          insertExon(feat, transformedTns);
      }
    }
    // Add protein product
    addProteinTransactions(transcript, transformedTns);
  }

  private void addProteinTransactions(AnnotatedFeatureI transcript,
                                      List chadoTransactions) {
    if (!transcript.isProteinCodingGene()) 
      return; // No Protein generated. Do nothing.
    // It is a little strange: A protein is not a SeqFeatureI.
    AbstractSequence sequence = (AbstractSequence) transcript.getPeptideSequence();
    if (sequence == null)
      return;
    // Insert peptide to feature table
    ChadoTransaction tn = generateTnFromOperation(ChadoTransaction.INSERT);
    tn.setTableName("feature");
    tn.setID(sequence.getAccessionNo());
    tn.addProperty("uniquename", sequence.getAccessionNo());
    tn.addProperty("residues", sequence.getResidues());
    tn.addProperty("organism_id", "organism");
    if (sequence.getChecksum() != null)
      tn.addProperty("md5checksum", sequence.getChecksum());
    tn.addProperty("seqlen", sequence.getLength() + "");
    tn.addProperty("type_id", getPolypeptideType());
    chadoTransactions.add(tn);
    // Have to register so that it will not be searched again in case
    // its id changed because of triggers in the database
    featureLookupTns.put(sequence.getAccessionNo(), tn);

    // this requires that transcript apollo id to be mapped to chado
    // feature id by previous lookup or insert - not so if just inserting
    // peptide and nothing else
    // Make sure feature_id is available
    //tn = (ChadoTransaction) featureLookupTns.get(transcript.getId());
    //if (tn == null) {
    // if we already have lookup trans then dont need to do trans twice
    if (!haveLookupTransaction(transcript)) {
      tn = generateFeatureTransaction(transcript, ChadoTransaction.LOOKUP);
      chadoTransactions.add(tn);
    }
    // Insert to feature_relation table
    tn = generateTnFromOperation(ChadoTransaction.INSERT);
    tn.setTableName("feature_relationship");
    tn.addProperty("object_id", transcript.getId());
    tn.addProperty("subject_id", sequence.getAccessionNo());
    tn.addProperty("type_id", transProtRelationTerm);
    chadoTransactions.add(tn);

    // Insert into featureloc table
    tn = generateTnFromOperation(ChadoTransaction.INSERT);
    tn.setTableName("featureloc");
    tn.addProperty("feature_id", sequence.getAccessionNo());
    // genomicSequence needed to be defined somewhere
    tn.addProperty("srcfeature_id", ChadoTransaction.MAP_POSITION_ID);
    tn.addProperty("strand", transcript.getStrand() + "");
    int[] coord = convertToChadoCoordForProtein(transcript);
    tn.addProperty("fmin", coord[0] + "");
    tn.addProperty("fmax", coord[1] + "");
    //tn.addProperty("phase", feature.getPhase() + "");
    chadoTransactions.add(tn);
  }
  
  /** make add synonym transaction for every synonym in feature - feature is new so
   * all its synonyms are as well */
  private void addSynTransForNewAnnot(AnnotatedFeatureI newAnnot, List chadoTransactions) {
    List synonyms = newAnnot.getSynonyms();
    if (synonyms == null || synonyms.size() == 0)
      return;
    Synonym synonym = null;
    ChadoTransaction tn = null;
    int index = 0;
    for (Iterator it = synonyms.iterator(); it.hasNext();) {
      synonym = (Synonym) it.next();
      addSynonymTransactions(newAnnot, index, synonym.getName(), chadoTransactions);
      index ++;
    }
  }
  
  /** Creates synonym ChadoTransactions and adds them to chadoTransactions list
   * Does FORCE on Synonym table, Lookup for feature_id (if not already present),
   * and INSERT into feature_synonym - this is for adding synonyms (not delete) */
  private void addSynonymTransactions(AnnotatedFeatureI feature,
                                      int index,
                                      String synonym,
                                      List chadoTransactions) {

    // make sure we have user pub trans
    addUserPubTransaction(chadoTransactions);

    ChadoTransaction tn = null;
    // Add to synonym table
    tn = generateTnFromOperation(ChadoTransaction.FORCE); // More safe with force
    String id = feature.getId() + ".synonym." + index;
    tn.setID(id);
    tn.setTableName("synonym");
    tn.addProperty("name", synonym);
    // dont think sgml is needed and could cause problem if not roundtripped
    // sgml is nonnull - has to be inserted, since apollo has no sgml syn editor
    // just use syn name. But sgml is not part of unique key for lookup - this is
    // encoded in ChadoTransaction actually so we are ok.
    tn.addProperty("synonym_sgml", synonym);
    // The definition for synonym should be handled by xml template
    tn.addProperty("type_id", "synonym");
    chadoTransactions.add(tn);
    // Add to feature_synonym table
    // Make sure feature_id is available
    tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      chadoTransactions.add(tn);
    }
    tn = generateTnFromOperation(ChadoTransaction.INSERT);
    tn.setTableName("feature_synonym");
    tn.addProperty("feature_id", feature.getId());
    tn.addProperty("synonym_id",id);
    tn.addProperty("pub_id", pub_id);
    tn.addProperty("is_internal", "0"); // Use default false
    tn.addProperty("is_current", "1"); // Use default true
    chadoTransactions.add(tn);    
  }
                                      
  
  private void insertExon(SeqFeatureI exon, List transformedTns) throws FeatLocException {
    ChadoTransaction chadoTn;
    SeqFeatureI transcript = exon.getRefFeature();
    // Have to lookup transcript first
    chadoTn = (ChadoTransaction) featureLookupTns.get(transcript.getId());
    if (chadoTn == null) {
      chadoTn = generateFeatureTransaction(transcript, ChadoTransaction.LOOKUP);
      transformedTns.add(chadoTn);
    }
    // add a shared (shared with an exon in the database) or brand new exon
    // can use the same transaction xml
    // Use force for exon in the feature table
    // why force?? dont we know this is an insert??
    // so guanming thinks he made it a force as a safeguard, in case the db
    // changed and the exon was created and thus out of synch with apollo
    // this shouldnt happen (hopefully) so im changing to INSERT 
    // Force is problematic because the lookup cant have the temp uniquename
    // but it is needed for the insert (shared exons are so wierd)
    // if we do need to do FORCE we will need to tell ChadoTrans to not use
    // uniquename in lookup or something like that
    // actually there is a compelling reason to do force - theres wierdo scenarios
    // where apollo will have the false impression that theres an exon in the db
    // - eg if you add an exon and dup trans it will see the exon as shared and existing
    // where actually both exons are new - so in order to avoid the headache of figuring
    // whether exons are from db or not (i think the logic could get complicated with
    // funny scenarios) i think it easiest to do a force and just lookup in db
    // need to resolve uniquename issue on lookup
    //chadoTn = generateFeatureTransaction(feature, ChadoTransaction.FORCE);
    // id needs to have temp in it on insert to fire chado trigger
    
    
    //if (!isExonShared(transcript, feature.getStart(), feature.getEnd())){
    // -temp can be name adapter specific - should use nameAdapter.isTemp()
    if (exon.getId().indexOf("temp") == -1)
      exon.setId(exon.getId() + "-temp");
    
    //chadoTn = generateFeatureTransaction(exon, ChadoTransaction.INSERT);
    //chadoTn = generateFeatureTransaction(exon, ChadoTransaction.FORCE);
    ChadoTransaction.Operation FORCE = ChadoTransaction.FORCE;
    String chadoExName = getChadoExonName(exon);
    chadoTn = generateExonFeatureTransaction(exon,chadoExName,FORCE);
    transformedTns.add(chadoTn);
    // Use another force for exon in the featureloc table
    //chadoTn = generateFeatureLocTransaction(exon, ChadoTransaction.FORCE);
    // throws TransactionTransformException
    chadoTn = genExonFeatLocTransaction(exon,chadoExName,FORCE);
    transformedTns.add(chadoTn);
    // Use insert for exon in the feature_relationship table
    // Have to use force here since the previous operations are force. Otherwise,
    // an error will throws: try to insert duplicate record.
    //chadoTn = generateFeatureRelTransaction(exon, ChadoTransaction.FORCE);
    chadoTn = generateExonFeatRelTrans(exon,chadoExName,transcript,FORCE);
    transformedTns.add(chadoTn);
//     }else{ -- dont need with FORCE feature
//       //Alternate transcript case
//       //Adding an isExonShared test for the alternate transcript case
//       //TODO : Warning, the current test could lead to bugs in certain cases. For instance, adding an alternate
//       //Transcript and then deleting the original transcript rise a problem.
//       //Indeed, in that case, the exon to add is not shared in the scop of apollo model (ie when transforming inner apollo transaction to chado transaction)
//       //But it is still shared in the scop of chado. This would lead to an SQL error except if we ensure that the original
//       //transcript gets deleted before insertibng the exons of the alternate transcript
//       //although coalesce could already take care of this problem.
      
//       //Just updating feat relationship
//       String sharedExonChadoName = getChadoExonName(feature);
//       //Find another exon shared by new range
//       ChadoTransaction lookupExon = (ChadoTransaction) featureLookupTns.get(sharedExonChadoName);
//       if (lookupExon == null) {
//         lookupExon = generateExonTransaction(feature,sharedExonChadoName,ChadoTransaction.LOOKUP);
//         transformedTns.add(lookupExon);
//       }
//       ChadoTransaction addFeatRel = generateExonFeatRelTrans(feature,sharedExonChadoName,transcript,
//           ChadoTransaction.INSERT);
//       transformedTns.add(addFeatRel);
//     }

  }

  private void addDbxrefTransactions(List dbxrefs,
                                                 List chadoTransactions,
                                                 String featureID) {
    if (dbxrefs == null || dbxrefs.size() == 0)
      return;
    DbXref xref = null;
    ChadoTransaction tn = null;
    int index = 0;
    for (Iterator it = dbxrefs.iterator(); it.hasNext();) {
      xref = (DbXref) it.next();
      tn = generateTnFromOperation(ChadoTransaction.FORCE);
      // Register under dbxref table
      String id = featureID + ".dbxref." + index;
      tn.setID(id);
      tn.setTableName("dbxref");
      String dbName = xref.getDbName();
      if (!dbName.startsWith("DB")) // Make sure id is not conflicting with others
                                    // e.g. null is used in both pub and cv tables.
        dbName = "DB:" + dbName;
      tn.addProperty("db_id", dbName);
      tn.addProperty("accession", xref.getIdValue());
      chadoTransactions.add(tn);
      // Insert to feature_dbxref table
      tn = generateTnFromOperation(ChadoTransaction.INSERT);
      tn.setTableName("feature_dbxref");
      tn.addProperty("feature_id", featureID);
      tn.addProperty("dbxref_id", id);
      // Note: idType saves to nowhere!
      chadoTransactions.add(tn);
      index ++;
    }
  }
  
  private void addPropsTransactions(AnnotatedFeatureI transcript, List chadoTransactions) {
    String id = transcript.getId();
    ChadoTransaction tn = null;
    // Owner
    String owner = transcript.getOwner();
    if (owner != null && owner.length() > 0) {
      tn = generatePropertyTransaction(id, "owner", owner, 0, ChadoTransaction.INSERT);
      chadoTransactions.add(tn);
    }
    // IsProblem
    if (transcript.isProblematic()) {
      tn = generatePropertyTransaction(id, "problem", "true", 0, ChadoTransaction.INSERT);
      chadoTransactions.add(tn);
    }
    // description
    if (transcript.getDescription() != null &&
        transcript.getDescription().length() > 0) {
      tn = generatePropertyTransaction(id, 
                                       "description", 
                                       transcript.getDescription(), 
                                       0,
                                       ChadoTransaction.INSERT);
      chadoTransactions.add(tn);
    }
    Map properties = transcript.getProperties();
    if (properties != null && properties.size() > 0) {
      for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
        String name = (String) it.next();
        // Some properies are not saved in a vector.
        // So have to check it 
        Object obj = properties.get(name);
        if (obj instanceof List) {
          List values = (List) properties.get(name); // properties are saved
          // in a vector
          if (values == null || values.size() == 0)
            continue;
          for (int i = 0; i < values.size(); i++) {
            String value = (String) values.get(0);
            tn = generatePropertyTransaction(id, name, value, i, ChadoTransaction.INSERT);
            chadoTransactions.add(tn);
          }
        } 
        else if (obj.toString().length() > 0) {
          // It should be save to assume obj is a String.
          tn = generatePropertyTransaction(id, name, obj.toString(), 0, ChadoTransaction.INSERT);
          chadoTransactions.add(tn);
        }
      }
    }
  }
  
  private void addCommentsTransaction(AnnotatedFeatureI feature, List chadoTransactions) {
    if (feature.getComments() == null || feature.getComments().size() == 0)
      return;
    String id = feature.getId();
    List comments = feature.getComments();
    Comment comment = null;
    String value = null;
    ChadoTransaction tn = null;
    int index = 0;
    for (Iterator it = comments.iterator(); it.hasNext();) {
      comment = (Comment) it.next();
      addCommentsTransaction(feature, index, comment, chadoTransactions);
      index ++;
    }
  }
  
  private void addCommentsTransaction(AnnotatedFeatureI feature, 
                                      int index, 
                                      Comment comment, 
                                      List chadoTransactions) {

    // make sure we have user pub trans - creates pub_id used below
    addUserPubTransaction(chadoTransactions);

    if (comment == null) {
      String m = "Can't transform comment transaction to chado trans as comment is null";
      logger.error(m);
      return;
    }


    // Make sure there is a id defined for feature
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      chadoTransactions.add(tn);
    }
    String value = createChadoCommentValue(comment.getText(), comment);
    // There is no place for comment id in the chado db
    tn = generatePropertyTransaction(feature.getId(), 
                                     "comment", 
                                     value, 
                                     index, 
                                     ChadoTransaction.INSERT);
    String featPropID = feature.getId() + ".comment." + index;
    tn.setID(featPropID);
    chadoTransactions.add(tn);
    if (pub_id != null && pub_id.length() > 0) {
      // Need to add user to featureprop_pub table
      tn = generateTnFromOperation(ChadoTransaction.INSERT);
      tn.setTableName("featureprop_pub");
      tn.addProperty("featureprop_id", featPropID);
      tn.addProperty("pub_id", pub_id); // pub_id should be generated in ChadoXMLWriter
      chadoTransactions.add(tn);
    }
  }

  /**
   * @param rank -1 for not specifying rank in ChadoTransaction.
   */
  private ChadoTransaction generatePropertyTransaction(String id, 
                                                        String type, 
                                                        String value, 
                                                        int rank,
                                                        ChadoTransaction.Operation op) {
    ChadoTransaction tn = generateTnFromOperation(op);
    tn.setTableName("featureprop");
    tn.addProperty("feature_id", id);
    tn.addProperty("type_id", type);
    tn.addProperty("value", value);
    if (rank > -1) // Don't do anything if rank is -1.
      tn.addProperty("rank", rank + "");
    return tn;
  }
  
  private void transformDeleteSubpartTransaction(DeleteTransaction tn,
                                                 List transformedTns) {  
    SeqFeatureI feature = tn.getSeqFeature();
    ChadoTransaction chadoTn = null;
    TransactionSubpart subpart = tn.getSubpart();
    //Object deletedValue = tn.getSubpartValue();
    Object deletedValue = tn.getOldSubpartValue();
    if (deletedValue == null) {
      String m = "\nChado Transaction: Dropping delete transasction - null subpart "+
        "value for "+subpart+" for feature "+feature.getName()+"\n";
      logger.error(m, new Throwable());

      // Illegal state exception seems like a funny choice - following whats been
      // done elsewhere for now. it is handy thats its a runtime exception.
      // An exception will kill the whole wrting of transactions - i think for now
      // ill just drop the transaction, and print the error message.
      //throw new IllegalStateException(m);
      return;
    }
    if (subpart == TransactionSubpart.SYNONYM) {
      Synonym syn = (Synonym)deletedValue;
      deleteSynonymTransaction(feature,syn,transformedTns);
    }
    else if (subpart == TransactionSubpart.COMMENT) {
      deleteCommentTransaction((AnnotatedFeatureI)feature,
                             (Comment)deletedValue,
                             transformedTns);
    }
  }
  
  private void deleteSynonymTransaction(SeqFeatureI feature,
                                        Synonym synonym,
                                        List transformedTns) {
    // make sure we have user pub trans - creates pub_id used below
    String synOwner = synonym.hasOwner() ? synonym.getOwner() : UserName.getUserName();
    addPubLookup(synOwner,transformedTns); // checks if already added

   ChadoTransaction tn = null;
    // Look up synonym_id
    tn = generateTnFromOperation(ChadoTransaction.LOOKUP); // More safe with force
    String synonymId = feature.getId() + ".synonym";
    tn.setID(synonymId);
    tn.setTableName("synonym");
    tn.addProperty("name", synonym.getName());
    // dont think we need sgml and could cause problem
    //tn.addProperty("synonym_sgml", synonym);
    // The definition for synonym should be handled by xml template
    tn.addProperty("type_id", "synonym");
    transformedTns.add(tn);
    // Delete link between synonym_id and feature_id in feature_synonym table
    tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(tn);
    }
    tn = generateTnFromOperation(ChadoTransaction.DELETE);
    tn.setTableName("feature_synonym");
    tn.addProperty("feature_id", feature.getId());
    tn.addProperty("synonym_id",synonymId);
    //tn.addProperty("pub_id", pub_id); // pub_id comes from addUsrPubTrans above
    tn.addProperty("pub_id",synOwner);
    transformedTns.add(tn);    
    // However, since synonym is shared. There is no way to check is a synonym
    // is still used by xort. Have to use a trigger, but no trigger right now!
    // orphans will run rampant!
  }
  
  private void deleteCommentTransaction(AnnotatedFeatureI feature,
                                        Comment comment,
                                        List transformedTns) {
    // make sure we have user pub trans - creates pub_id used below
    // actually deleting comment doest seem to use pub id - not sure why??
    //addUserPubTransaction(chadoTransactions);

    // Make sure there is a id defined for feature
    ChadoTransaction tn = (ChadoTransaction) featureLookupTns.get(feature.getId());
    if (tn == null) {
      tn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
      transformedTns.add(tn);
    }
    String value = createChadoCommentValue(comment.getText(), comment);
    // There is no place for comment id in the chado db
    tn = generatePropertyTransaction(feature.getId(), 
                                     "comment", 
                                     value,
                                     -1, // A mark for not specifying rank
                                     ChadoTransaction.DELETE);
    transformedTns.add(tn);
    // No need to delete record in featureprop_pub table. It will be taken
    // care of by foreign key constraint.
  }
  
  private String createChadoCommentValue(String text, Comment comment) {
    Date date = new Date(comment.getTimeStamp());
    String dateStr = dateFormat.format(date);
    return text + "::DATE:" + dateStr + "::TS:" + comment.getTimeStamp();
  }
  
  private void transformDeleteTransaction(DeleteTransaction tn,
                                           List transformedTns) {
    if (tn.getSubpart() != null) {
      transformDeleteSubpartTransaction(tn, transformedTns);
      return;
    }
    ChadoTransaction chadoTn = null;
    SeqFeatureI feature = tn.getSeqFeature();
    
    // DELETE EXON
    if (feature.isExon()) {
      String transcriptName = tn.getParentFeature().getName();

      // Lookup the parent transcript
      //chadoTn = (ChadoTransaction) featureLookupTns.get(tn.getParentFeature().getId());
      if (!haveLookupTransaction(tn.getParentFeature())) {
        chadoTn = generateFeatureTransaction(tn.getParentFeature(),
                                             ChadoTransaction.LOOKUP);
        transformedTns.add(chadoTn);
      }
      
      // Lookup the exon
      //chadoTn = (ChadoTransaction) featureLookupTns.get(feature.getId());
      //String chadoExonName = getChadoExonName(feature);
      //if (chadoTn == null) {
      String chadoExonName = getChadoExonName(feature);
      if (!haveLookupTransaction(chadoExonName)) {
        // this may be wrong - i think this should be calling generateExonTransaction
        // which does exon name w range instead of uniquename - but im not sure...
        //chadoTn = generateFeatureTransaction(feature, ChadoTransaction.LOOKUP);
        // range hasnt changed so old range is the new range
        chadoTn = generateExonFeatureTransaction(feature,chadoExonName,ChadoTransaction.LOOKUP);
        transformedTns.add(chadoTn);
        featureLookupTns.put(chadoExonName,chadoTn);
      }

      // Delete the entry for the relationship between exon and its transcrip
      // in the feature_realtionship (chado trigger should take care of deleting
      // exon feat & featloc)
      chadoTn = generateFeatureRelTransaction(feature,
                                              chadoExonName,
                                              tn.getParentFeature(), 
                                              ChadoTransaction.DELETE);
      transformedTns.add(chadoTn);
    }
    
    // DELETE TRANSCRIPT
    else if (feature instanceof Transcript) {
      // To delete a feature in the feature table,
      // no need to look it up first 
      // Delete the transcript in the feature table
      chadoTn = generateFeatureTransaction(feature, ChadoTransaction.DELETE);
      transformedTns.add(chadoTn);
      // The recordes in other tables referring to the deleted transcript should
      // be removed automatically by foreign key constraints
    }
    
    // DELETE GENE/TOP ANNOT 
    else {
      // It is similar to delete an transcript
      chadoTn = generateFeatureTransaction(feature, ChadoTransaction.DELETE);
      transformedTns.add(chadoTn);
    }
  }
  
  /** Returns true if there is a lookup for the feature */
  private boolean haveLookupTransaction(SeqFeatureI feat) {
    if (feat.isExon())
      return haveLookupTransaction(getChadoExonName(feat));
    return haveLookupTransaction(feat.getId());
  }

  /** lookupKey is feature id except for exons where its old name */
  private boolean haveLookupTransaction(String lookupKey) {
    return featureLookupTns.get(lookupKey) != null;
  }

  private ChadoTransaction generateFeatureTransaction(SeqFeatureI f, 
                                                      ChadoTransaction.Operation op) {
    return generateFeatureTransaction(f,null,op); // null residues
  }

  private ChadoTransaction generateFeatureTransaction(SeqFeatureI feature,String residues,
                                                      ChadoTransaction.Operation op) {
    ChadoTransaction transaction = generateTnFromOperation(op);
    transaction.setTableName("feature");
    transaction.setID(feature.getId());
    // Value "organism" should be defined in the transaction xml template.
    // It is not a natural way to do this.
    transaction.addProperty("organism_id", "organism");
    String rootType = getRootType(feature);
    boolean isRoot = feature.isAnnotTop();
    
    // INSERT for non-genes
    if (op == ChadoTransaction.INSERT && !rootType.equals("gene")) {
      // Add type information to temp name. Required by trigger in db. A rather hacky way!
      // i think this is a pase hack
      String id = insertTypeToTempName(feature.getId(), rootType, isRoot);
      String name = insertTypeToTempName(feature.getName(), rootType, isRoot);
      transaction.addProperty("uniquename", id);
      if (!feature.isExon())
        transaction.addProperty("name", name);
    }
    
    // EXON INSERT/FORCE
    // need to lookup exon with its range, which is in its name (not uniquename)
    // shouldnt this be true of all operations on exon?
    // this is wrong - have to have uniquename - i think name is generated by trigger
//     else if (op.isForce() && feature.isExon()) {
//       // This is rice/gmod specific (fly is different). exon names are gene name
//       // with range in interbase (not base-oriented)
//       transaction.addProperty("name",getChadoExonName(feature));
//     }
    
    else {
      transaction.addProperty("uniquename", feature.getId());
      // Don't need name for exon. Name is only needed for insert since it is not in the uniquekey.
      if (!(feature.isExon()) && op != ChadoTransaction.DELETE)
        transaction.addProperty("name", feature.getName());
    }
    
    transaction.addProperty("type_id", getChadoType(feature));

    if (residues != null)
      transaction.addProperty("residues", residues);

    // Register all featurelookup transactions so that only one is needed
    if (op == ChadoTransaction.LOOKUP || 
        op == ChadoTransaction.INSERT ||
        op == ChadoTransaction.FORCE) {
      featureLookupTns.put(feature.getId(), transaction);
    }
    return transaction;    
  }

  /** currently only used for lookup - might be used for more later 
      returns null if, actually used for insert & force now as well */
  private ChadoTransaction generateExonFeatureTransaction(SeqFeatureI exon, String name,
                                                   Operation op) {
    // do the generic genFeat - and then take out unique & add name
    ChadoTransaction transaction = generateFeatureTransaction(exon,op);
    //if (op.isLookup() && name != null) {
      // see if its in cache already - if its in cache doesnt that mean its already
      // been looked up and redundant to put it in again??
      // take out uniquename
    //transaction.removeProperty("uniquename");
      //String geneName = exon.getRefFeature().getRefFeature().getName();
      //String name = getChadoExonName(geneName,oldRange);
      // check transaction cache with name
      //if (haveLookupTransaction(name))
      //return null; // this exon has already been looked up - return null
    //transaction.addProperty("name",name);
    //transaction.setID(name);
      // add to lookup transaction cache - done by caller
      //featureLookupTns.put(name,transaction);
    //}
    //return transaction;
    // what this is doing is taking uniquename out of lookups - alternatively we can
    // make the unique key "name" (in ChadoTransaction) and then it will only lookup
    // on name and will do insert with uniquename (for FORCE) which has -temp which
    // triggers id & name creation
    //if (!op.isInsert())
    //  transaction.removeProperty("uniquename");
    // this is a little funny but this signals ChadoTransaction to use exon unique
    // key which has "name" instead of "uniquename"
    transaction.setIsExon(true);
    transaction.addProperty("name",name);
    // i can see why this is done as name is really the identifier but is it necasary?
    // it would be easier to leave it as the id if that would work as other transactions
    // just use id - then wouldnt have to do special methods for exons
    transaction.setID(name);
    return transaction;
  }

  private ChadoTransaction genExonFeatLocTransaction(SeqFeatureI exon, String name,
                                                          Operation op)
    throws FeatLocException {
    // throws TransactionTransformException
    ChadoTransaction transaction = generateFeatureLocTransaction(exon,op);
    transaction.addProperty("feature_id",name);
    return transaction;
  }

  private ChadoTransaction generateExonFeatRelTrans(SeqFeatureI exon, String name,
                                                    SeqFeatureI transcript,
                                                    Operation op) {
    ChadoTransaction transaction = generateFeatureRelTransaction(exon,transcript,op);
    transaction.addProperty("subject_id",name);
    return transaction;
  }

  /** differ from apollo exon names in 2 ways, interbase range not base oriented
      range. also goes from low to high, not start to end */
  private String getChadoExonName(SeqFeatureI exon) {
    String geneName = exon.getRefFeature().getRefFeature().getName();
    return getChadoExonName(geneName,exon);
  }
  private String getChadoExonName(String geneName,RangeI range) {
    RangeI interbaseRange = convertBaseOrientedToInterbase(range);
    return geneName+":"+interbaseRange.getLow()+"-"+interbaseRange.getHigh();
  }

  private String getNewChadoExonName(SeqFeatureI exon) {
    return getChadoExonName(exon) + "-temp"; // -temp at end?
  }

  /** Base oriented is not inclusive and is zero based, so subtract one from low */
  private RangeI convertBaseOrientedToInterbase(RangeI baseOriented) {
    return new Range(baseOriented.getLow() - 1, baseOriented.getHigh());
  }

  private String getRootType(SeqFeatureI feature) {
    if (feature instanceof ExonI) {
      return ((ExonI)feature).getTranscript().getGene().getFeatureType();
    }
    else if (feature instanceof Transcript)
      return ((Transcript)feature).getGene().getFeatureType();
    else
      return feature.getFeatureType();
  }
  
  /**
   * A hack. It should not do something like this. Try to make trigger in the chado db happy.
   * This method can only be used for features other than gene.
   I think this is pase now - just does mucky stuff with CG & CR
   * @param name
   * @param type
   * @return
   */
  private String insertTypeToTempName(String name, String type, boolean isRoot) {
    int index = name.indexOf("temp");
    if (index < 0)
      return name; // It is not a temp name
    if (name.startsWith("CG"))
      name = "CR" + name.substring(2);
    // Type is needed only for one level features
    if (oneLevelAnnotTypes.contains(type)) {
      if (name.startsWith("CG:") || name.startsWith("CR:")) {
        return name.substring(0, 3) + type + ":" + name.substring(3);
      }
    }
    return name; 
  }
  
  private class FeatLocException extends TransactionTransformException {
    private FeatLocException(String m) { super (m); }
  }

  private ChadoTransaction generateFeatureLocTransaction(SeqFeatureI feature, 
                                                         ChadoTransaction.Operation op)
    throws FeatLocException {
    if (feature.rangeIsUnassigned()) {
      String m = "ERROR: Transaction transform to chado transactions has failed.\n"+
        "Feature "+feature+" has no range for feat loc transaction. "+
        "Could be that its been deleted. ";
      throw new FeatLocException(m);
    }
    ChadoTransaction transaction = generateTnFromOperation(op);
    transaction.setTableName("featureloc");
    transaction.addProperty("feature_id", feature.getId());
    // genomicSequence needed to be defined somewhere
    transaction.addProperty("srcfeature_id", ChadoTransaction.MAP_POSITION_ID);
    transaction.addProperty("strand", feature.getStrand() + "");
    int[] coord = convertToChadoCoord(feature);
    transaction.addProperty("fmin", coord[0] + "");
    transaction.addProperty("fmax", coord[1] + "");
    //transaction.addProperty("phase", feature.getPhase() + "");
    return transaction;
  }
  
  private int[] convertToChadoCoord(SeqFeatureI feature) {
    int[] coord = new int[2];
    // feature insertions are wierd beasts - now good way to represent in 
    // base-oriented so correcting for as much here by not subtracting
    // as represented in base-oriented as 7 to 7 which is how they should be in 
    // interbase - not sure how else to finesse this
    if (feature.getFeatureType().equals(apollo.datamodel.SequenceI.INSERTION))
      coord[0] = feature.getLow();
    else
      coord[0] = feature.getLow() - 1;
    coord[1] = feature.getHigh();
    return coord;
  }
  
  /**
   * Have to use interbase for chado. So minus -1 for the start.
   * @return a two element int array. The first is for fmin, and 
   * second for fmax.
   */    
  private int[] convertToChadoCoordForProtein(AnnotatedFeatureI transcript) {
    int[] coord = new int[2];
    int strand = transcript.getStrand();
    int start = transcript.getTranslationStart();
    int end = transcript.getTranslationEnd();
    if (strand >= 0) {
      coord[0] = start - 1;
      if (coord[0] == -1)
        coord[0] = transcript.getStart() - 1;
      coord[1] = end;
      if (coord[1] == 0)
        coord[1] = coord[0] + transcript.getPeptideSequence().getLength() * 3;
    }
    else {
      coord[1] = start;
      if (coord[1] == -1)
        coord[1] = transcript.getStart();
      coord[0] = end - 1;
      if (coord[0] == -1)
        coord[0] = coord[1] - transcript.getPeptideSequence().getLength() * 3;
      if (coord[0] < 0) // Something wrong in peptide sequence, use transcript information
        coord[0] = transcript.getEnd() - 1;
    }
    return coord;        
  }
  
  private ChadoTransaction generateFeatureRelTransaction(SeqFeatureI feature,
                                                          ChadoTransaction.Operation op) {
    // Make sure parent is not null
    if (feature.getParent() == null)
      throw new IllegalArgumentException("ChadoTransactionTransformer.generateFeatureRelTransaction(): " +
                                          "no object feature for feature_relation!");
    return generateFeatureRelTransaction(feature, feature.getParent(), op);
  }
  
  private ChadoTransaction generateFeatureRelTransaction(SeqFeatureI feature,
                                                         SeqFeatureI parent,
                                                         ChadoTransaction.Operation op){
    return generateFeatureRelTransaction(feature,feature.getId(),parent,op);
  }

  /** subjectId allows for exon subjects to pass in exon name rather than using
      exon id. */
  private ChadoTransaction generateFeatureRelTransaction(SeqFeatureI feat,
                                                         String subjectId,
                                                         SeqFeatureI parent,
                                                         ChadoTransaction.Operation op){
    ChadoTransaction transaction = generateTnFromOperation(op);
    transaction.setTableName("feature_relationship");
    transaction.addProperty("object_id", parent.getId());
    transaction.addProperty("subject_id", subjectId);
    // partof should be defined in the transaction xml template.
    transaction.addProperty("type_id", "partof");
    return transaction;  
  }
  
  private String getChadoType(SeqFeatureI feature) {
    if (feature instanceof ExonI)
      return feature.getFeatureType();
    else if (feature instanceof Transcript) {
      // Check parent
      AnnotatedFeature parent = (AnnotatedFeature) feature.getParent();
      if (parent.getFeatureType().equals("gene"))
        return "mRNA";
      else
        return parent.getFeatureType();
    }
    else if (feature instanceof AnnotatedFeature) {
      String type = feature.getFeatureType();
      logger.debug("getChadoType "+type+oneLevelAnnotTypes.contains(type));
      if (oneLevelAnnotTypes != null &&
          oneLevelAnnotTypes.contains(type))
        return type;
      else
        return "gene";
    }
    return "Unknown Type";
  }
  
  /**
   * A helper method to create a ChadoTransaction based on the specified operation.
   * @param op
   * @return
   */
  private ChadoTransaction generateTnFromOperation(ChadoTransaction.Operation op) {
    ChadoTransaction transaction = null;
    if (op == ChadoTransaction.UPDATE)
      transaction = new ChadoUpdateTransaction();
    else {
      transaction = new ChadoTransaction();
      transaction.setOperation(op);
    }
    return transaction;
  }
  
  /**
   * A helper method to check if an exon is shared with another exon in
   * another transcript in the same gene. The range specified by start, end
   * can be the old values.
   * @param tst the transcrip containing the compared exon
   * @param start the start position of the exon
   * @param end the end position of the exon
   * @return true for sharing range with other.
   */
  private boolean isExonShared(SeqFeatureI tst, int start, int end) {
    // Need to get the top-level gene
    SeqFeatureI gene = tst.getRefFeature();
    List transcripts = gene.getFeatures();
    Transcript tmp = null;
    for (Iterator it = transcripts.iterator(); it.hasNext();) {
      tmp = (Transcript) it.next();
      if (tmp == tst)
        continue;
      SeqFeatureI tmp1 = tmp.getFeatureContaining(start);
      if (tmp1 == null)
        continue;
      // tmp1 should be another exon and its range should be tested
      if (tmp1.isExon()) {
        //ExonI exon1 = (ExonI) tmp1;
        if (tmp1.getStart() == start &&
            tmp1.getEnd() == end)
          return true;
      }
    }
    return false;
  }
  
  private boolean userPubTransactionAdded = false;
  private void addUserPubTransaction(List transformedTns) {
    // only need this lookup trans once, if already done just return
    if (userPubTransactionAdded)
      return;

    ChadoTransaction ct = createUserPubTransaction();
    transformedTns.add(ct);
    userPubTransactionAdded = true;
  }

  /** this has side effect of setting pub_id String to user name 
   * phase this out for addPubLookup... */
  private ChadoTransaction createUserPubTransaction() {
    String user = UserName.getUserName();
    if (havePubLookup(user))
      return null;
    if (user == null || user.trim().length() == 0)
      return null;
    ChadoTransaction ts = createPubLookup(user);
    // Keep the pub_id
    pub_id = user;
    return ts;
  }

  private void addPubLookup(String uniquename,List transformedTns) {
    if (havePubLookup(uniquename))
      return; // already got it
    ChadoTransaction ct = createPubLookup(uniquename);
    transformedTns.add(ct);
  }

  private ChadoTransaction createPubLookup(String uniquename) {
    ChadoTransaction ts = new ChadoTransaction();
    ts.setOperation(ChadoTransaction.FORCE);
    ts.setTableName("pub");
    ts.setID(uniquename);
    ts.addProperty("uniquename", uniquename);
    // "curator" type can be problematic - in flybase sima is type curated gen ann...
    // another reason this is problematic is there can be syns with pubs with
    // name "none" and type "null pub" - those are the self synonyms though so maybe
    // they should not even be displayed as synonyms - no reason to delete self syns
    // but uniquename is key - no need for type
    // but wait this is a force - and type_id is non-null! - need unique key to 
    // differentiate lookup & insert (hardwire in ChadoTransaction)
    ts.addProperty("type_id", "curator");
    if (nameToPubLookupTns == null)
      nameToPubLookupTns = new HashMap();
    nameToPubLookupTns.put(uniquename,ts);
    return ts;
  }
  
  private boolean havePubLookup(String uniquename) {
    return nameToPubLookupTns != null && nameToPubLookupTns.containsKey(uniquename);
  } 

  public ChadoTransaction createSrcFeatureIDTransaction(String mapID, String mapType) {
    ChadoTransaction ts = new ChadoTransaction();
    ts.setOperation(ChadoTransaction.LOOKUP);
    ts.setTableName("feature");
    ts.setID(ChadoTransaction.MAP_POSITION_ID);
    ts.addProperty("uniquename", mapID);
    ts.addProperty("organism_id", "organism");
    ts.addProperty("type_id", mapType);
    return ts;
  }

  public void setOneLevelAnnotTypes(List features) {
    this.oneLevelAnnotTypes = features;
  }
  
  public void setThreeLevelAnnotTypes(List features) {
    this.threeLevelAnnotTypes = features;
  }

}
