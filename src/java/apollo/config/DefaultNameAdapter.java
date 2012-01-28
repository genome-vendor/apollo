package apollo.config;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;

import apollo.datamodel.*;
import apollo.editor.AddTransaction;
//import apollo.editor.AnnotationChangeLog;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionSubpart;
import apollo.editor.UpdateTransaction;
import apollo.util.*;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.config.Config;
import apollo.config.FeatureProperty; 
import apollo.dataadapter.ApolloDataAdapterI;
/** If no name adapter is specified, and no or human gene def, then DefaultNameAdapter 
    is used. Also superclass for FlyNameAdapter. */
public class DefaultNameAdapter implements ApolloNameAdapterI {

  private static int annotNumber = 1;
  private TransactionManager transactionManager;

  protected DefaultNameAdapter() {}

  public void setTransactionManager(TransactionManager tm) {
    this.transactionManager = tm;
  }
  
  protected TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature, Vector exonResults) {
    // by default ignore the results the annot is associated with
    return generateName(annots,curation_name,feature);
  }

  //added by TAIR
  public String getSuffixDelimiter()
  {
    return "-";
  }

  public boolean checkName(String name,Class featureClass)
  {
    return false;
  }

  //generates a name for a given feature
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature) {
    if (feature instanceof AnnotatedFeature) {
      return getName(annots, curation_name,
                     AnnotatedFeature.class, 
                     nextAnnotNumber(annots, AnnotatedFeature.class),
                     "GN", "");
    } else if (feature instanceof Transcript) {
      return getName (annots, curation_name,
                      Transcript.class, 
                      nextAnnotNumber(annots, Transcript.class),
                      "XS", "");
    } else if (feature instanceof ExonI) {
      return getName(annots, curation_name,
                     ExonI.class, 
                     nextAnnotNumber(annots, ExonI.class),
                     "EX", "");
    } else
      return "???";
  }

  public String generateAnnotSplitName(SeqFeatureI annot,
                                       StrandedFeatureSetI annotParent,
                                       String curationName) {
    return generateName(annotParent,curationName,annot);
  }

  //generates a ID for a given feature
  public String generateId(StrandedFeatureSetI annots,
                           String curation_name, SeqFeatureI feature) {
    if (feature.isTranscript()) {
      return getId(annots, curation_name,
                   Transcript.class, 
                   nextAnnotNumber(annots, Transcript.class),
                   "XS", "");
    } else if (feature.isExon()) {
      return getId(annots, curation_name,
                   ExonI.class,
                   nextAnnotNumber(annots, ExonI.class),
                   "EX", "");
    } else if (feature.hasAnnotatedFeature()) {
      return getId (annots, curation_name,
                    AnnotatedFeature.class,
                    nextAnnotNumber(annots, AnnotatedFeature.class),
                    "GN", "");
    } else
      return "???";
  }

  public String generateNewId(StrandedFeatureSetI annots, String curation_name, SeqFeatureI feature) {
    // by default the request for a *new* id is ignored; adapters are free to pay attention
    // to this nuance if they wish
    return generateId(annots, curation_name, feature);
  }

  public String generateExonId(StrandedFeatureSetI annots,
                               String curation_name,
                               SeqFeatureI exon, 
                               String geneId) {
    return geneId + ":" + exon.getStart() + "-" + exon.getEnd();
  }

  public String generateNewExonId(StrandedFeatureSetI annots,
				  String curation_name,
				  SeqFeatureI exon, 
				  String geneId) {
    // by default the request for a *new* id is ignored; adapters are free to pay attention
    // to this nuance if they wish
    return generateExonId(annots, curation_name, exon, geneId);
  }

  public void updateExonId(ExonI exon) {
    Transcript transcript = (Transcript) exon.getParent();
    AnnotatedFeatureI gene = transcript.getGene();
    String newId = generateExonId(null, null, exon, gene.getId());
    exon.setId(newId);
  }

  /** Makes name update transaction, edits model w it, & returns it */
  protected UpdateTransaction setName(AnnotatedFeatureI annFeat, String newName) {
    TransactionSubpart ts = TransactionSubpart.NAME;
    UpdateTransaction ut = new UpdateTransaction(annFeat,ts,annFeat.getName(),newName);
    ut.editModel();
    return ut;
  }

  public CompoundTransaction setAnnotName(AnnotatedFeatureI annot, String newName) {
    CompoundTransaction ct = new CompoundTransaction(this);
    String oldName = annot.getName(); // get old name before edit
    ct.addTransaction(setName(annot,newName));
    ct.addTransaction(addSynonym(annot,oldName));
    //ct.addTransaction(setAllTranscriptNamesFromAnnot(annot)); // not in default
    return ct;
  }


  /** sets transcript name to name and adds synonym of old trans name, returns compound
      transaction with update name and add syn transactions */
  public CompoundTransaction setTranscriptName(AnnotatedFeatureI trans,String name) {
    String oldName = trans.getName(); // before edit
    CompoundTransaction ct = new CompoundTransaction(this);
    UpdateTransaction ut = setName(trans,name);
    ct.addTransaction(ut);
    // by default add transcript synonym as well (??)
    AddTransaction at = addSynonym(trans,oldName);
    ct.addTransaction(at);
    return ct;
  }

  public CompoundTransaction setTranscriptId(SeqFeatureI trans, String id) {
    CompoundTransaction ct = new CompoundTransaction(this);
    ct.addTransaction(setId(trans,id));
    return ct;
  }

  public CompoundTransaction setTranscriptNameFromAnnot(AnnotatedFeatureI trans,
                                         AnnotatedFeatureI gene) {
    // setting to gene name seems a funny default for mutli-trans??
    return setTranscriptName(trans,gene.getName());
  }

  /** returns an add transaction for adding synonym to annot. return null if
      syn is null,no_name, or temp - should probably also check if synonym exists
      already  */
  protected AddTransaction addSynonym(AnnotatedFeatureI annFeat, String synString) {
    if (synString == null || synString.equals(SeqFeatureI.NO_NAME) || isTemp(synString))
      return null;
    if (annFeat.hasSynonym(synString))
      return null;
    TransactionSubpart ts = TransactionSubpart.SYNONYM;
    Synonym synonym = new Synonym(synString);
    AddTransaction at = new AddTransaction(annFeat,ts,synonym);
    at.editModel();
    return at;
  }

  /** Returns true if id/name String is a temp id/name. Default is to look for
      "temp" in id or name. */
  public boolean isTemp(String idOrName) {
    return idOrName.indexOf(":temp") >= 0; // colon?
  }

  protected boolean isTemp(SeqFeatureI feat) {
    return isTemp(feat.getName());
  }


  // Returns true if feature name starts with same prefix as feature ID
  // (The name of this method seems slightly misleading, since it doesn't
  // actually check whether the name is the same as the ID.)
  public boolean nameIsId (SeqFeatureI feature) {
    String prefix;
    if (feature instanceof AnnotatedFeature) {
      prefix = "GN";
    } else if (feature instanceof Transcript) {
      prefix = "XS";
    } else if (feature instanceof ExonI) {
      prefix = "EX";
    } else
      prefix = "???";
    return (feature.getName().startsWith (prefix));
  }

  private String getId(StrandedFeatureSetI annots,
                       String curation_name,
                       Class featureClass,
                       int start,
                       String prefix, String suffix) {
    prefix = prefix + ":temp";
    Vector features = SeqFeatureUtil.getFeaturesOfClass(annots,
                                                        featureClass,
                                                        false);
    int num = start;
    for(int i=0; i < features.size(); i++) {
      SeqFeatureI g = (SeqFeatureI) features.elementAt(i);
      String this_id = g.getId();
      if (this_id != null && this_id.startsWith(prefix)) {
        try {
          int index = this_id.indexOf (":" + curation_name);
          if (index > 0)
            this_id = this_id.substring (0, index);
          String id = this_id.substring(prefix.length());
          int idnum = Integer.parseInt(id);
          if (idnum >= num) {
            num = idnum + 1;
          }
        } catch (NumberFormatException e) {}
      }
    }
    return prefix+(num)+suffix;
  }

  private String getName(StrandedFeatureSetI annots,
                         String curation_name,
                         Class featureClass,
                         int start,
                         String prefix, String suffix) {
    prefix = prefix + ":temp";
    Vector features = SeqFeatureUtil.getFeaturesOfClass(annots,
                                                        featureClass,
                                                        false);
    int num = start;
    for(int i=0; i < features.size(); i++) {
      SeqFeatureI g = (SeqFeatureI) features.elementAt(i);
      String this_name = g.getName();
      if (this_name.startsWith(prefix)) {
        try {
          int index = this_name.indexOf (":" + curation_name);
          if (index > 0)
            this_name = this_name.substring (0, index);
          String num_str = this_name.substring(prefix.length());
          int namenum = Integer.parseInt(num_str);
          if (namenum >= num) {
            num = namenum + 1;
          }
        } catch (NumberFormatException e) {}
      }
    }
    return prefix+(num)+suffix;
  }

  // Copied from FlyNameAdapter
  public boolean suffixInUse (Vector transcripts, String suffix, int t_index) {
    boolean used = false;

    for (int i = 0; i < transcripts.size() && !used; i++) {
      // don't check the one we're looking to update
      if (i != t_index) {
        Transcript t = (Transcript) transcripts.elementAt (i);
        used = t.getName().endsWith (suffix);
      }
    }
    return used;
  }

  /** Returns true if seq feature's ID format is ok.  
      The format checked is the regex from tiers/feature property.
      If no regex provided returns true. 
      idOrName could be the features ID or it could be its name to check if
      the name is mirroring the IDs format */
  public boolean checkFormat(SeqFeatureI feat,String idOrName) {
    //if (feat.getId() == null) return true; else
    // Returns true if id fits pattern specified for this type of SeqFeature
    return match(feat,idOrName);
  }

  /** Returns true if name and id of feat are in the same format.
      The format checked is the regex from tiers/feature property.
      If no regex provided returns true. 
      Rename idAndNameHaveIdFormat? HaveConfiggedFormat?
      The first checks whether the ID format is legal
      the second checks whether the name also matches the ID format
      If there is no format specified then there needs to be
      a third check to see there is a format at all. If none
      is specified then this will return false always 
      Currently the only place this is called is in FeatureEditorDialog 
  */
  public boolean idAndNameHaveSameFormat(SeqFeatureI feat,String id, String name) {
    FeatureProperty fp 
      = Config.getPropertyScheme().getFeatureProperty(feat.getFeatureType());
    if (fp == null || (fp != null && fp.getIdFormat() == null))
      // Return FALSE if no pattern specified for type, because
      // if we say "true" to match, that has more implications.
      return false;
    else
      return checkFormat(feat,id) && match(feat,name);
  }

  /** Returns true if matchString matches pattern specified for this 
      type of SeqFeature. should temp ids be able to pass? currently they cant
      i think they should?? this came up with a bug in gene edit panel */
  protected boolean match(SeqFeatureI sf, String matchString) {
    FeatureProperty fp 
      = Config.getPropertyScheme().getFeatureProperty(sf.getFeatureType());
    if (fp == null || (fp != null && fp.getIdFormat() == null)) {
      // Return FALSE if no pattern specified for type, because
      // if we say "true" to match, that has more implications.
      // 2/12/04 That seems to result in the wrong behavior--condemning an ID
      // for having the "wrong" format when in fact no format pattern is 
      // defined.
      return true; 
    }
    return Pattern.matches(fp.getIdFormat(),matchString);
  }

  public String getTranscriptNamePattern() {
    // No transcript name pattern defined for default name adapter
    return null;
  }

  /** Returns true if changing type from oldType to newType will cause a change
      in feature ID, i.e. the ID prefix will change to reflect the new type.
      By default returns false. */
  public boolean typeChangeCausesIdChange(String oldType, String newType) {
    return false;
  }
  public String getNewIdFromTypeChange(String oldId,String oldType,String newType) {
    return oldId;
  }

  /** Generate the next number for use in temporary annotation id */
  protected int nextAnnotNumber(StrandedFeatureSetI annots, Class featClass) {
    // Don't increment it yet--we'll see if we really need to.
    int num = annotNumber;
    // Check all the annots to make sure this number wasn't already used
    Vector features = SeqFeatureUtil.getFeaturesOfClass(annots,
                                                        featClass,
                                                        false);
    for(int i=0; i < features.size(); i++) {
      SeqFeatureI g = (SeqFeatureI) features.elementAt(i);
      String this_id = g.getId();
      num = skipUsedTempNum(this_id, num);
    }

    // Check list of Transactions in AnnotationChangeLog, and exclude
    // any temp IDs found there.
    //AnnotationChangeLog acl = AnnotationChangeLog.getAnnotationChangeLog();
    for (int i=0; i < transactionManager.size(); i++) {
      //if (transactionManager.getTransaction(i) != null) { not needed
      Transaction trans = transactionManager.getTransaction(i);
      String id = trans.getProperty("id", Transaction.OLD);
      num = skipUsedTempNum(id, num);
      id = trans.getProperty("id", Transaction.NEW);
      num = skipUsedTempNum(id, num);
      id = trans.getProperty("annotation_id", Transaction.OLD);
      num = skipUsedTempNum(id, num);
      //}
    }  
    annotNumber = num;
    return annotNumber;
  }

  /** helper function to create new temp ids - gets the next temp number.
      if id is a temp id, parses out number used in temp id and return 
      that number plus one if greater then num. This assumes temping is done 
      with ":temp#". If curation_name is non null it uses it to strip it off 
      the end (fly temp names have :curation_name at the end - this should probably
      just go in the fly adapter) */
  protected int skipUsedTempNum(String id, int num) {
    int tempIdNum = -1;
    if (id != null && !(id.equals(""))) {
      if (id.indexOf("temp") < 0) // should use !isTemp()
        return num;
      try {
//         int index = (curation_name != null ? 
//                      id.indexOf (":" + curation_name) : -1);
//         if (index > 0) // cut off curation name if there is one
//           id = id.substring (0, index);
        int prefixOffset = id.indexOf(":temp"); // getTempPrefix()?
        // substring out the number at the end
        String tempNumString = id.substring(prefixOffset + ":temp".length());
        tempIdNum = Integer.parseInt(tempNumString);
      }
      catch (NumberFormatException e) {}
    }

    if (tempIdNum >= num)
      num = tempIdNum + 1;
    
    return num;
  }
  
  /** currently only used by fly which overrides this - 
      returns CompoundTransaction of all id changes - this is used by GeneEditPanel
      for explicit id changes(fly) and UpdateTransaction/TransactionUtil for id changes
      caused by type changes (fly) which should eventually use compound trans 
      Merge & split dont use this (should they?) */
  public CompoundTransaction setAnnotId(AnnotatedFeatureI annot, String id) {
    //annot.setId(id);
    UpdateTransaction ut = setId(annot,id); // modifies model
    CompoundTransaction compoundTrans = new CompoundTransaction(this);
    compoundTrans.addTransaction(ut);
    
//     // If root set all transcript ids - should this go in fly and/or rice?
//     if (annot.isAnnotationRoot()) {
//       CompoundTransaction transcriptTrans = setTranscriptIdsFromAnnot(annot);
//       compoundTrans.addTransaction(transcriptTrans); // ignores null
//     }

    return compoundTrans;  //return ut;
  }

  protected UpdateTransaction setId(SeqFeatureI annot, String newId) {
    TransactionSubpart ts = TransactionSubpart.ID;
    String oldId = annot.getId();
    UpdateTransaction ut = new UpdateTransaction(annot,ts,oldId,newId);
    ut.setOldId(oldId);
    ut.setNewId(newId);
    ut.editModel(); // makes change to annot
    return ut;
  }

//   /** For now does nothing. fly/rice subclass implements. Should fly rice setting
//       of trans id be the default? */
//   protected CompoundTransaction setTranscriptIdsFromAnnot(AnnotatedFeatureI annotParent) {
//     return null;
//   }

  public String generatePeptideNameFromTranscriptName(String transcriptName) {
    return transcriptName;
  }
  public String generatePeptideIdFromTranscriptId(String transcriptId) {
    return transcriptId;
  }

  public String generateChadoCdsNameFromTranscriptName(String transcriptName) {
    return transcriptName + "-CDS";
  }
  public String generateChadoCdsIdFromTranscriptId(String transcriptId) {
    return transcriptId + "-CDS";
  }  
  
  public void setDataAdapter(ApolloDataAdapterI dataAdapter) {}
  
}
  // This is no longer used externally - its an internal FlyNameAdapter thing
  // taking it out. 
//   /** I dont know if i buy this as the default - that cdna and pep seq get set to
//       transcript name. I would say a better default would be a no-op. If a dataadapter
//       has explicitly set the cdna and pep names (to something different than trans)
//       they wouldnt want them wiped out on transcript name update. By the way 
//       DefaultNameAdapter.changeSeqNames is not currently used (just flynameadapter) */
//   public CompoundTransaction changeSeqNames(AnnotatedFeatureI trans) {
//     if (trans.get_cDNASequence() != null)
//       trans.get_cDNASequence().setAccessionNo(trans.getName());
//     if (trans.getPeptideSequence() != null) {
//       trans.getPeptideSequence().setAccessionNo(trans.getName());
//     }
//  }

//   public String selectMergeName (StrandedFeatureSetI annots,
//                                  String curation_name,
//                                  AnnotatedFeatureI feature1, 
//                                  AnnotatedFeatureI feature2) {
//     String chosen_name = feature1.getName();
//     return chosen_name;
//   }

//   /** used for creating temp ids/names to see whats already used - this needs to be
//       switched over to the new TransactionManager - ACL is pase'! */
//   private AnnotationChangeLog annotationChangeLog;
  
  //private static DefaultNameAdapter defNameAdapSingleton = null;

//   /** Get DefaultNameAdapter singleton. Only need one DefaultNameAdapter and making it
//     a singleton allows "==" to work as a test of whether name adapter is the default.
//   */
// im forgetting the reason i took this out as a singleton - is it because the 
// subclasses are not singletons? is it funny to have a singleton where the subclasses
// are not? and is it possible to have the subclasses be singletons - perhaps a factory
//   public static DefaultNameAdapter getDefaultNameAdapter() {
//     if (defNameAdapSingleton == null) 
//       defNameAdapSingleton = new DefaultNameAdapter();
//     return defNameAdapSingleton;
//   }
//   public void setAnnotationChangeLog(AnnotationChangeLog acl) {
//     this.annotationChangeLog = acl;
//   }

//   protected AnnotationChangeLog getAnnotChangeLog() {
//     return annotationChangeLog;
//   }
