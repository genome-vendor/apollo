package apollo.editor;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.Comment;
import apollo.datamodel.DbXref;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.Transcript;

import apollo.config.ApolloNameAdapterI; // temp for id undo

/** Does all the retrieving of subpart stuff - im contemplating subclasses - it would
    be a lot of subclasses - or there could be a separate subclass for each type 
    this is getting slowly phased out for TransactionSubpart subclasses */

public class TransactionUtil {

  public static boolean getBoolean(AnnotatedFeatureI annot,
                                        TransactionSubpart subpart) {
    if (!subpart.isBoolean()) // programmer error
      throw new RuntimeException();

    if (subpart == TransactionSubpart.IS_PROBLEMATIC) 
      return annot.isProblematic();

    if (subpart == TransactionSubpart.REPLACE_STOP)
      // Generic--has readthrough stop codon.  May want to return the specific readthrough stop
      // residue (available via readThroughStopResidue()).
      return annot.hasReadThroughStop();

    if (subpart == TransactionSubpart.IS_DICISTRONIC)
      return Boolean.valueOf(annot.getProperty("dicistronic")).booleanValue();

    if (subpart == TransactionSubpart.FINISHED)
      return annot.getProperty("status").equals("all done");

    if (subpart == TransactionSubpart.NON_CONSENSUS_SPLICE_OKAY)
      return annot.nonConsensusSplicingOkay();

    return false; // exception?? err msg?
  }
  
  public static void flipBoolean(AnnotatedFeatureI annot, 
                                     TransactionSubpart subpart) {

    boolean oldVal = getBoolean(annot,subpart);

    if (subpart == TransactionSubpart.IS_DICISTRONIC) {
      annot.replaceProperty("dicistronic",!oldVal+""); // cheap way to get bool string
    }

    else if (subpart == TransactionSubpart.IS_PROBLEMATIC) {
      annot.setIsProblematic(!oldVal);
    }

    else if (subpart == TransactionSubpart.FINISHED) {
      String newVal = oldVal ? "not done" : "all done";
      annot.replaceProperty("status",newVal);
    }

    else if (subpart == TransactionSubpart.REPLACE_STOP) {
      annot.setReadThroughStop(!oldVal);
    }

    else if (subpart == TransactionSubpart.NON_CONSENSUS_SPLICE_OKAY) {
      // hafta be protein coding, more than one exon, and have non consensus
      // acceptor or donor
      if (haveNonConsensusSpliceSite(annot))
        annot.nonConsensusSplicingOkay(!getBoolean(annot,subpart));
    }
    
  }

  /**  Hafta be protein coding, more than one exon, and have non consensus
      acceptor or donor. Return false if transcript does not have non 
      consensus splice site - should it print an error msg? */
  private static boolean haveNonConsensusSpliceSite(AnnotatedFeatureI annot) {
    if (!annot.getFeatureType().equals(Transcript.TRANSCRIPT_TYPE)) return false;
    if (!annot.isProteinCodingGene()) return false;
    if (!(annot.size() > 1)) return false; // more than one exon
    return annot.getNonConsensusAcceptorNum() >= 0 || annot.getNonConsensusDonorNum() >= 0;
  }
 
  public static void undoUpdateComment(Transaction trans, AnnotatedFeatureI ann) {
    
    undoAddComment(trans);
    undoDeleteComment(trans);
  }

  static void undoAddComment(Transaction trans) {
    // delete comment
    int rank = trans.getSubpartRank();
    trans.getAnnotatedFeature().deleteComment(rank);
  }

  static void undoDeleteComment(Transaction trans) {
    Comment oldComment = trans.getOldComment();
    int index = trans.getSubpartRank();
    trans.getAnnotatedFeature().addComment(index,oldComment);
  }
  
  static void undoAddDbxref(Transaction t)
  {
    DbXref dbxref = (DbXref)t.getNewSubpartValue();
    t.getAnnotatedFeature().getIdentifier().deleteDbXref(dbxref);
  }
  
  static void undoDeleteDbxref(Transaction t)
  {
    DbXref dbxref = (DbXref)t.getOldSubpartValue();
    t.getAnnotatedFeature().addDbXref(dbxref);
  }

//   static void undoAddSynonym(Transaction trans) {
//     String syn = trans.getSubpartString();
//     trans.getAnnotatedFeature().deleteSynonym(syn);
//   }

//   static void undoDeleteSynonym(Transaction trans) {
//     String oldSyn = trans.getOldString();
//     int index = trans.getSubpartRank();
//     trans.getAnnotatedFeature().addSynonym(index,oldSyn);
//   }

  /** This belongs in Transaction but gene edit panel still does some of this
      internally - this is called by UpdateTransaction for type changes
      that cause id changes, eventually capture with compound trans */
  public static void setId(AnnotatedFeatureI ann, String newId,
                             ApolloNameAdapterI nameAdapter) {
    // eventually should return a ChangeList?
    nameAdapter.setAnnotId(ann,newId);
  }
}
  // used to do type - now does nothing - commenting out
//   public static void undoStringUpdate(UpdateTransaction trans, AnnotatedFeatureI ann) {
//     if (!trans.getSubpart().isString())
//       return;
//     String oldString = (String)trans.getPreValue();

// //     if (trans.getSubpart() == TransactionSubpart.TYPE) {
// //       ann.setType(oldString);
// //       ann.setBioType(oldString);
// //       // translation start and stop for gene to non-gene & viceversa
// //     }
//   }


    //if (trans.isUpdate()) {
//       Comment oldComment = trans.getOldComment();
//       int index = trans.getSubpartRank();
//       ann.deleteComment(index);
//       ann.addComment(index,oldComment);
      //}
    // else if (trans.isAdd())
    //else if (trans.isDelete())
      
