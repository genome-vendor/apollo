package apollo.config;

import java.util.Vector;
import java.util.regex.Pattern;

import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.DbXref;
import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Transcript;
import apollo.datamodel.RangeI;
import apollo.editor.AddTransaction;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.UpdateTransaction;
import apollo.editor.TransactionSubpart;
import apollo.util.SeqFeatureUtil;

import org.apache.log4j.*;

import org.bdgp.util.VectorUtil;
import apollo.util.FeatureComparator;

public class FlyNameAdapter extends GmodNameAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(FlyNameAdapter.class);

  //private static int annotNumber = 1;

  //private String transcriptNamePattern = ".*-R[A-Z]+"; // --> flybase!

  /** This is used by ChadoXmlWrite to generate a (new) name for exons in
      FlyBase Harvard's preferred style.  (Other types of things are passed
      up to the parent class's generateName method.) */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature) {
    // In Chado, exons have names.  Each exon is named parent_annot_name:N,
    // where N is a number representing the position of this exon in a sorted
    // set of all the exons of this annotation.  (A given exon may be shared by
    // more than one transcript.)
    // It's inefficient to have to sort all the exons just to name this one,
    // so possibly we should rename all the exons of an annot at once.
    if (feature instanceof ExonI) {
      Transcript transcript = ((ExonI)feature).getTranscript();
      AnnotatedFeatureI annot = (AnnotatedFeatureI)transcript.getRefFeature();
      int exonNum = getExonNumber((ExonI)feature, annot);
      return annot.getName() + ":" + exonNum;
    }
    else
      return super.generateName(annots, curation_name, feature);
  }

  /** Sort all of the exons belonging to annot by starting position; then
      return thisExon's number within the order. */
  private int getExonNumber(ExonI thisExon, AnnotatedFeatureI annot) {
    Vector allExons = new Vector();
    Vector transcripts = annot.getFeatures();
    for (int t=0; t < transcripts.size(); t++) {
      Transcript transcript = (Transcript)transcripts.elementAt(t);
      Vector exons = transcript.getFeatures();
      for (int e=0; e < exons.size(); e++) {
        ExonI exon = (ExonI) exons.elementAt(e);
        //        if (!allExons.contains(r))  // for some reason that doesn't work
        if (!vectorContains(allExons, exon))
          allExons.add(exon);
      }
    }
    //    System.out.println("For " + annot.getName() + ", vector of exons before sorting: "); // DEL
    //    for (int e=0; e < allExons.size(); e++) { // DEL
    //      RangeI r = (RangeI)allExons.elementAt(e); // DEL
    //      System.out.println(e + ": " + r.getLow() + "-" + r.getHigh()); // DEL
    //    } // DEL
    allExons = VectorUtil.sort(allExons, new FeatureComparator());
    //    System.out.println("For " + annot.getName() + ", vector of exons after sorting: "); // DEL
    // Locate thisExon in the sorted list and return its (1-based) number
    for (int e=0; e < allExons.size(); e++) {
      RangeI r = (RangeI)allExons.elementAt(e);
      //      System.out.println(e + ": " + r.getLow() + "-" + r.getHigh()); // DEL
      if (r.sameRange(thisExon))
        return e+1;
    }
    logger.error("programmer error: couldn't find exon " + thisExon + " in vector");
    return 9999;
  }

  /** Exon datamodels have stuff other than what we care about--the range--so just
      check whether we already have an entry in the vector for this range. */
  private boolean vectorContains(Vector features, RangeI range) {
    for (int i = 0; i < features.size(); i++) {
      RangeI f = (RangeI) features.elementAt(i);
      if (f.sameRange(range))
        return true;
    }
    return false;
  }

  /** generates a ID for a given feature. for annots calls generateAnnotTempId
      which produces a temp id */
  public String generateId(StrandedFeatureSetI annots,
                           String curation_name,
                           SeqFeatureI feature) {
    if (feature.isTranscript()) {
      return generateTranscriptId(feature);
    } 
    // In Chado, exons have IDs.
    else if (feature instanceof ExonI) {
      Transcript transcript = ((ExonI)feature).getTranscript();
      AnnotatedFeatureI annot = (AnnotatedFeatureI)transcript.getRefFeature();
      int exonNum = getExonNumber((ExonI)feature, annot);
      return annot.getId() + ":" + exonNum;
    }
    else if (feature.hasAnnotatedFeature()) {
      // take out cur name suffix
      return generateAnnotTempId (annots,curation_name,getIDPrefix(feature),
                                  ":" + curation_name);
    } else
      return "???";
  }

  public String generateAnnotSplitName(SeqFeatureI annot,
                                       StrandedFeatureSetI annotParent,
                                       String curationName) {
    if (isTemp(annot) || isAnnotId(annot)) {
      String suffix = ":" + curationName;
      return generateAnnotTempId(annotParent,curationName,getIDPrefix(annot),suffix);
    }
    else 
      return super.generateAnnotSplitName(annot,annotParent,curationName);
  }

  /** Produces next temp id */
  private String generateAnnotTempId(StrandedFeatureSetI annots,
                                     String curation_name,
                                     String prefix, String suffix) {
    prefix = prefix + ":temp";
    if (annots == null) {
      logger.error("FlyNameAdapter.genAnnTempId: annots is null!?");
      return "";
    }
    int num = nextAnnotNumber(annots, curation_name);
    return prefix+num+suffix;
  }

  /** Generate the next number for use in temporary annotation id 
   in DefaultNameAdapter theres also a nextAnnotNumber but it takes a sfs and a Class -
   this seems funny */
  private int nextAnnotNumber(StrandedFeatureSetI annots,String curation_name) {
    // Don't increment it yet--we'll see if we really need to.
    int num = annotNumber;
    // Check all the annots to make sure this number wasn't already used
    Vector features = SeqFeatureUtil.getFeaturesOfClass(annots,
                                                        AnnotatedFeature.class,
                                                        false);
    for(int i=0; i < features.size(); i++) {
      SeqFeatureI g = (SeqFeatureI) features.elementAt(i);
      String this_id = g.getId();
      num = skipUsedTempNum(curation_name, this_id, num);
    }

    // Check list of Transactions in TransactionManager, and exclude
    // any temp IDs found there. - so i assume this is to skp over deleted
    // temp ids - but does it matter? i guess for coalescing it might
    //AnnotationChangeLog acl = AnnotationChangeLog.getAnnotationChangeLog();

    // this doesnt work anymore - its using old style transactions not new

    for (int i=0; i < getTransactionManager().size(); i++) {
      //if (getTransactionManager().getTransaction(i) != null) { // not needed
      Transaction trans = getTransactionManager().getTransaction(i);
      String id = trans.getProperty("id", Transaction.OLD);
      num = skipUsedTempNum(curation_name, id, num);
      id = trans.getProperty("id", Transaction.NEW);
      num = skipUsedTempNum(curation_name, id, num);
      id = trans.getProperty("annotation_id", Transaction.OLD);
      num = skipUsedTempNum(curation_name, id, num);
        //}
    }  

    annotNumber = num;
    return annotNumber;
  }

  /** helper function to create new temp ids - gets the next temp number.
   * if id is a temp id, parses out number used in temp id and return 
   * that number plus one if greater then num. This assumes temping is done 
   * with ":temp#". If curation_name is non null it uses it to strip it off 
   * the end (fly temp names have :curation_name at the end - this should probably
   * just go in the fly adapter) */
  protected int skipUsedTempNum(String curation_name, String id, int num) {
    int tempIdNum = -1;
    if (id != null && !(id.equals(""))) {
      if (id.indexOf("temp") < 0)
        return num;

      int index = (curation_name != null ? id.indexOf (":" + curation_name) : -1);
      if (index > 0) // cut off curation name if there is one
        id = id.substring (0, index);
    }
    return super.skipUsedTempNum(id,num);
  }


  /** Construct a new index for transcript #t_index, following the
   * pattern -RA, -RB, ..., -RAA, -RAB, ... -RZZ
   * I'm assuming there will not be more than 26*26 transcripts. 
   * addTemp is ignored. fly never adds temp to its transcript suffixes.*/
  protected String generateTranscriptSuffix(int t_index, Vector transcripts,
                                            boolean addTemp) {
    char letter;
    String suffix = "";
    // I'm going to be bold here and assume that t_index might be more than 26,
    // but it won't be more than 26*26.
    if (t_index >= 26) {
      int div26 = t_index/26 - 1;
      letter = (char)('A' + div26);
      suffix = suffix + letter;
    }
    int remainder = t_index % 26;
    letter = (char)('A' + remainder);
    suffix = suffix + letter;
    suffix = "-R" + suffix;
    // make sure it is unique
    if (suffixInUse (transcripts, suffix, t_index)) {
      // If that suffix was used, recursively choose a new one.
      return(generateTranscriptSuffix(t_index+1, transcripts,addTemp));
    }
    return suffix;
  }

  /** Set the cDNA *and* peptide names based on the new transcript name. */
  protected CompoundTransaction setSeqNamesFromTranscript(AnnotatedFeatureI trans) {
    /** peptide name from gmod */
    CompoundTransaction ct = super.setSeqNamesFromTranscript(trans);

    // dont know if this is right - its setting cdna id/acc on trans name change
    if (trans.get_cDNASequence() != null) {
      //trans.get_cDNASequence().setAccessionNo(trans.getName()); -> editModel
      TransactionSubpart ts = TransactionSubpart.CDNA_NAME;
      UpdateTransaction t = new UpdateTransaction(trans,ts);
      t.setOldSubpartValue(trans.get_cDNASequence().getName());
      t.setNewSubpartValue(trans.getName());
      // Sequence accession should be id, not name
      // nomi - shouldnt this be getName(), you just changed this, reverting back - mg
      // t.setNewSubpartValue(trans.getId());
      t.editModel();
      ct.addTransaction(t);
    }
    return ct;
  }


  /** The prefix to use if idFormat is not specified in tiers file for a type */
  protected String getDefaultIDPrefix() {
    return "CG";
  }

  /** Given a format string (e.g. CG\d) returns the prefix (e.g. CG) */
  protected String getPrefix(String idFormat) {
    // Heuristic: prefix is the (first) portion of the idFormat that
    // consists entirely of letters (e.g. CG, FBti)
    int i;
    for (i=0; i < idFormat.length() && Character.isLetter(idFormat.charAt(i)); i++)
      ;
    return(idFormat.substring(0, i));
  }


  private final static String transcriptSuffixRoot = "-R";
  protected String getTranscriptSuffixRoot() {
    return transcriptSuffixRoot;
  }

  private final static String peptideSuffixRoot = "-P";
  /** returns "-P", The peptide suffix minus the ordinal */
  protected String getPeptideSuffixRoot() {
    return peptideSuffixRoot;
  }

  /** Returns false. Fly transcript ordinal is alphabetic, not numeric. */
  protected boolean transcriptOrdinalIsNumeric() {
    return false;
  }
  /** Returns false - fly doesnt temp transcripts */
  protected boolean transcriptCanHaveTempSuffix() {
    return false;
  }


  /** returns CompoundTransaction of all id changes - this is used by GeneEditPanel
      for explicit id changes(fly) and UpdateTransaction/TransactionUtil for id changes
      caused by type changes (fly) which should eventually use compound trans - 
      at the moment this is really just for fly 
      Merge & split dont use this (should they?) */
  public CompoundTransaction setAnnotId(AnnotatedFeatureI annot, String id) {
    CompoundTransaction compoundTrans = super.setAnnotId(annot,id); //DefNA sets annot
    
    // If root set all transcript ids - should this go in fly and/or rice?
    if (annot.isAnnotTop()) {
      CompoundTransaction transcriptTrans = setTranscriptIdsFromAnnot(annot);
      compoundTrans.addTransaction(transcriptTrans); // ignores null
    }

    return compoundTrans;  //return ut;
  }

  /** called by setAnnotId, sets all the transcript ids. used for explicit id changes
      from GEP and id changes from type changes. */
  private CompoundTransaction setTranscriptIdsFromAnnot(AnnotatedFeatureI annotParent) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    for (int i=0; i<annotParent.size(); i++) {
      SeqFeatureI trans = annotParent.getFeatureAt(i);
      String transcriptSuffix = getTranscriptSuffix(trans,annotParent);
      String transId = annotParent.getId() + transcriptSuffix;
      //trans.setId(transId);
      UpdateTransaction ut = setId(trans,transId); // DefaultNameAdapter
      compTrans.addTransaction(ut);
      //setExonIdsFromTranscript(trans); // ret CompoundTrans?? // doesnt do anything
      ut = setPeptideIdFromTranscriptId(trans);
      compTrans.addTransaction(ut);
    }
    return compTrans;
  }

  private UpdateTransaction setPeptideIdFromTranscriptId(SeqFeatureI transcript) {
    if (!transcript.hasPeptideSequence())
      return null;
    
    String pepId = generatePeptideIdFromTranscriptId(transcript.getId());

    TransactionSubpart ts = TransactionSubpart.PEPTIDE_ID;
    String oldId = transcript.getPeptideSequence().getAccessionNo();
    UpdateTransaction ut = new UpdateTransaction(transcript,ts,oldId,pepId);
    ut.editModel();
    return ut;
  }

  /** Flys peptide ids derive from its transcript id (unlike gmod/rice) */
  protected UpdateTransaction setPeptideIdFromTranscript(SeqFeatureI trans) {
    if (!trans.hasPeptideSequence()) {
      logger.error("can't set peptide id; transcript has no peptide (see FlyNameAdapter)", new Throwable());
      return null;
    }
    String newPepId = generatePeptideIdFromTranscriptId(trans.getId());
    String oldPepId = trans.getPeptideSequence().getAccessionNo();
    TransactionSubpart ts = TransactionSubpart.PEPTIDE_ID;
    UpdateTransaction ut = new UpdateTransaction(trans,ts,oldPepId,newPepId);
    ut.editModel();
    return ut;
  }

//   /** Fly and rice do this differently, though it would be nice if fly adopted
//       rice's exon id style. For now fly does nothing, and rice overrides. Fly 
//       will need to get into exon ids at some point. */
//   protected void setExonIdsFromTranscript(SeqFeatureI transcript) {}

}

//  private final static String transcriptNamePattern = ".*-R[A-Z]+";

//   /** This is used as a check if transcript suffix editing is allowed in the annot
//       info editor. fly allows this so its important there. */
//   public String getTranscriptNamePattern() {
//     return transcriptNamePattern;
//   }

//   /** Construct a name for a new top level annotation */
//   protected String generateAnnotName(StrandedFeatureSetI annots,
//                          String curation_name,
//                          SeqFeatureI annot) {
//     String gene_name = annot.getName();
//     if (gene_name.equals (SeqFeatureI.NO_NAME))
//       // If it's not named yet, make an ID and use that as the name
//       return generateAnnotTempId(annots,
//                     curation_name,
//                     getIDPrefix(annot), ":" + curation_name);
//     // This is for genes with names that are ids and actually this catches
//     // temp genes as well as they start with CG & CR. just makes temp ids
//     // i think GmodNameAdapter.genAnnName may cover these cases with isAnnotId() & isTemp
//     if (gene_name.startsWith ("CG")) 
//       return generateAnnotTempId(annots,
//                     curation_name,
//                     "CG", ":" + curation_name);
//     else if (gene_name.startsWith ("CR")) {
//       return generateAnnotTempId(annots,
//                     curation_name,
//                     "CR", ":" + curation_name);
//     }
//     // This is for gene splits with real names (not temp, not id) - it uses old name
//     // but tacks on a number to each split off gene
//     else {
//       return generateAnnotSplitName(annot,annots);
// //       int index = gene_name.lastIndexOf (":");
// //       if (index > 0 && (index + 1) < gene_name.length()) {
// //         // then parse out the count from the name
// //         //String num_str = gene_name.substring(index + 1); // not used
// //         gene_name = gene_name.substring (0, index);
// //       }
// //       // parses out numbers from all other genes with same name
// //       int num = getNameCount (annots, AnnotatedFeature.class, 1, gene_name);
// //       return gene_name+":"+(num);
//     }
//   }

// this isnt used - i cant even find it commented out - but their certainly is
// a lot of logic here
//   // --> FLY!
//   public String selectMergeName (StrandedFeatureSetI annots,
//                                  String curation_name,
//                                  AnnotatedFeatureI feature1, 
//                                  AnnotatedFeatureI feature2) {
//     String chosen_name = feature1.getName();

//     String name1 = feature1.getName();
//     String name2 = feature2.getName();
//     if (name1.equals (SeqFeatureI.NO_NAME))
//       name1 = getIDPrefix(feature1);
//     if (name2.equals (SeqFeatureI.NO_NAME))
//       name2 = getIDPrefix(feature1);

//     // If one of them has "temp" in its name, that is never the right choice--
//     // pick the other one.
//     if (name2.indexOf (":temp") >= 0)
//       return name1;
//     else if (name1.indexOf (":temp") >= 0)
//       return name2;

//     if ((name1.startsWith ("CG") && name2.startsWith ("CG")) ||
//         (name1.startsWith ("CG") && name2.startsWith ("CR")) ||
//         (name1.startsWith ("CR") && name2.startsWith ("CG"))) {
//       chosen_name = getId (annots, curation_name,
//                            AnnotatedFeature.class, annotNumber, "CG", "");
//     }
//     else if (name1.startsWith ("CR") && name2.startsWith ("CR")) {
//       chosen_name = getId (annots, curation_name,
//                            AnnotatedFeature.class, annotNumber, "CR", "");
//     }
//     else if (name1.startsWith ("CG") || name1.startsWith("CR")) {
//       chosen_name = name2;
//     }
//     else if (name2.startsWith ("CG") || name2.startsWith("CR")) {
//       chosen_name = name1;
//     }
//     else {
//       // Cambridge has to decide these based on date
//       // precedence (Can this be inferred from FBgn?)
//       boolean choosen = false;
//       if (feature1.getDbXrefs().size() > 0 &&
//           feature2.getDbXrefs().size() > 0) {
//         DbXref xref1 = (DbXref) feature1.getDbXrefs().elementAt (0);
//         DbXref xref2 = (DbXref) feature2.getDbXrefs().elementAt (0);
//         String id1 = xref1.getIdValue();
//         String id2 = xref2.getIdValue();
//         if (id1 != null && 
//             id2 != null &&
//             id1.startsWith ("FBgn") && 
//             id2.startsWith ("FBgn")) {
//           id1 = id1.substring ("FBgn".length());
//           id2 = id2.substring ("FBgn".length());
//           try {
//             if ((Integer.parseInt (id1)) < (Integer.parseInt(id2)))
//               chosen_name = name1;
//             else
//               chosen_name = name2;
//             choosen = true;
//           } catch (Exception e) {}
//         }
//       }
//       if (!choosen) {
//         chosen_name = getName (annots, curation_name, feature1);
//       }
//     }
//     return chosen_name;
//   }
//         int prefixOffset = id.indexOf(":temp");
//         // substring out the number at the end
//         String tempNumString = id.substring(prefixOffset + ":temp".length());
//         tempIdNum = Integer.parseInt(tempNumString);
// //      }
// //      catch (NumberFormatException e) {}
//     }//         int prefixOffset = id.indexOf(":temp");
//         // substring out the number at the end
//         String tempNumString = id.substring(prefixOffset + ":temp".length());
//         tempIdNum = Integer.parseInt(tempNumString);
// //      }
// //      catch (NumberFormatException e) {}
//     }

//     if (tempIdNum >= num)
//       num = tempIdNum + 1;
    
//     return num;
//   }


//     if (tempIdNum >= num)
//       num = tempIdNum + 1;
    
//     return num;
//   }
