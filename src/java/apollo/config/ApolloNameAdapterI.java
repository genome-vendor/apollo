package apollo.config;

// should this go in apollo.editor package? i think so

import java.util.Date;
import java.util.Vector;
import apollo.datamodel.*;
//import apollo.editor.AnnotationChangeLog;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.dataadapter.ApolloDataAdapterI;

public interface ApolloNameAdapterI {

  //generates a name for a given feature
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature);

  /** Generates name for a given feature. May or may not use associated 
      vector of exon results used to make the annot */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature, Vector exonResults);

  /** Generate a name for a gene split */
  public String generateAnnotSplitName(SeqFeatureI annot,
                                       StrandedFeatureSetI annotParent,
                                       String curationName);

  //generates an id for a given feature; the returned id may be the same as
  //the feature's current id
  public String generateId(StrandedFeatureSetI annots, 
                           String curation_name,
                           SeqFeatureI feature);

  //request the generation of a *new* id for a feature.  note that the name
  //adapter may ignore the request and return the feature's current id, if
  //that is its policy
  public String generateNewId(StrandedFeatureSetI annots, 
			      String curation_name,
			      SeqFeatureI feature);

  public String generateExonId(StrandedFeatureSetI annots,
                               String curation_name,
                               SeqFeatureI exon, 
                               String geneId);

  //request the generation of a *new* id for an exon.  note that the name
  //adapter may ignore the request and return the exon's current id, if
  //that is its policy
  public String generateNewExonId(StrandedFeatureSetI annots,
				  String curation_name,
				  SeqFeatureI exon, 
				  String geneId);

  /** 
   * This method is used by some adapters/databases to update the exon's id to 
   * reflect its new coordinates any time the exon's location is updated.  */
  public void updateExonId(ExonI exon);

  //ADDED by TAIR
  public String getSuffixDelimiter();
  public boolean checkName(String name,Class featureClass);
  // sets the name of a given feature - not used anymore
  //public void setName(SeqFeatureI feature, String name);

  /** Set name for top level annot. may set synonym and transcript names as well 
      depending on subclass */
  public CompoundTransaction setAnnotName(AnnotatedFeatureI annot,String newName);

  /** Sets transcripts name. may also set peptide & cdna seq accession */
  public CompoundTransaction setTranscriptName(AnnotatedFeatureI trans,String name);

  /** Sets transcript id, may also set peptide id */
  public CompoundTransaction setTranscriptId(SeqFeatureI trans, String id);

  /** Sets the name of a transcript based upon its annot parent.
      May also set exon names. May also set peptide accession */
  public CompoundTransaction setTranscriptNameFromAnnot(AnnotatedFeatureI transcript,
                                         AnnotatedFeatureI gene);

  //find out if name is an id string
  public boolean nameIsId (SeqFeatureI feature);


  public boolean suffixInUse(Vector transcripts, String suffix, int t_index);

  /** Returns true if id jibes with seq feature's ID format. This can be used for both
   * ids and names that mirror the id */
  public boolean checkFormat(SeqFeatureI feat,String id);

  /** Return true if id and name have same format */
  public boolean idAndNameHaveSameFormat(SeqFeatureI feat,String id, String name);

  /** Returns expected pattern (if any) for transcript names */
  public String getTranscriptNamePattern();

  /** Returns true if changing type from oldType to newType will cause a change
      in feature ID, i.e. the ID prefix will change to reflect the new type */
  public boolean typeChangeCausesIdChange(String oldType, String newType);

  public String getNewIdFromTypeChange(String oldId,String oldType,String newType);

  /** A name adapter needs a TransactionManager. Has to make sure new temp id
      isnt in log. */
  public void setTransactionManager(TransactionManager tm);
  //public void setAnnotationChangeLog(AnnotationChangeLog acl);

  /** Set annots id to id. May set annot subparts ids as well.
      Returns a CompoundTransaction of all the transactions that have
      occurred. */
  public CompoundTransaction setAnnotId(AnnotatedFeatureI annot, String id);

  /** Returns true if id/name String is a temp id/name. Default is to look for
      "temp" in id or name. */
  public boolean isTemp(String idOrName);

  /** Generate a peptide name given a transcript name. */
  public String generatePeptideNameFromTranscriptName(String transcriptName);
  public String generatePeptideIdFromTranscriptId(String transcriptId);

  /** 
   * Generate a CDS name given a transcript name.  Used only by the chado adapter.
   */
  public String generateChadoCdsNameFromTranscriptName(String transcriptName);
  public String generateChadoCdsIdFromTranscriptId(String transcriptId);
  
  public void setDataAdapter(ApolloDataAdapterI dataAdapter);
}

  // now just used in fly name adap internally - commenting out
  // sets the name of a derived sequences based upon transcript name
  //public CompoundTransaction changeSeqNames(AnnotatedFeatureI transcript);

//   //generates a name for a given feature - not used anymore
//   public String selectMergeName(StrandedFeatureSetI annots,
//                                 String curation_name,
//                                 AnnotatedFeatureI feature1, 
//                                 AnnotatedFeatureI feature2);
