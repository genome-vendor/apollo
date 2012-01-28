package apollo.config;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

import java.text.NumberFormat;

import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.DbXref;
import apollo.datamodel.ExonI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.datamodel.Transcript;
import apollo.datamodel.SequenceI;
import apollo.editor.AddTransaction;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.UpdateTransaction;
import apollo.editor.TransactionSubpart;
import apollo.util.SeqFeatureUtil;

import org.apache.log4j.*;

/** This has a lot of stuff that needs to go in FlyNameAdapter subclass.
    work in progress */

public class GmodNameAdapter extends DefaultNameAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(GmodNameAdapter.class);

  protected static int annotNumber = 1;

  /** generates a name for a given feature - this only works for
      genes, transcripts and exons */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature) {
    if (feature.isTranscript()) {
      return generateTranscriptName(feature);
    }
    else if (feature.isExon()) {
      Transcript transcript = ((ExonI)feature).getTranscript();
      if (transcript.isProteinCodingGene())
        return (transcript.getName() +
                " exon " + (transcript.getFeatureIndex(feature) + 1));
      else
        return (transcript.getName());
    }
    else if (feature.hasAnnotatedFeature()) {
      //return generateAnnotTempId(annots, getIDPrefix(feature));
      return generateId(annots,curation_name,feature);
    } else
      return "???";
  }


  /** Produces next temp id - suffix not used (used in fly subclass) make 
   separate method without suffix and curation_name? */
  private String generateAnnotTempId(StrandedFeatureSetI annots,String prefix) {
    if (annots == null) {
      logger.warn("GmodNameAdapter.genAnnTempId: annots is null!?");
      return "";
    }
    prefix = prefix + getTempSuffix();
    int num = nextAnnotNumber(annots);
    return prefix + num;
  }

  /** generates both temp names and split names. a split annot can have 3 kinds of
      names: temp, id, & real-user-added name. temps, and ids just get temp ids. if its
      a real-user-added name that means its being split and it needs split numbers.
      old split numbers are looked for and added to if there. a split number is a :#
      at the end of a real user name. looks at other annots with same name(prefix) and 
      if they have split numbers uses next highest. curationName not used here
      (for fly subclass) */
  public String generateAnnotSplitName(SeqFeatureI annot,
                                       StrandedFeatureSetI annotParent,
                                       String curationName) {
    // TEMP, ID -> temp id
    if (isTemp(annot) || isAnnotId(annot))
      return generateAnnotTempId(annotParent, getIDPrefix(annot));
    
    // REAL NAME (from user) -> add number suffix
    String nameWithoutNumber = annot.getName();
    // strip old number off (:# suffix) if it has one
    int index = nameWithoutNumber.lastIndexOf(":");
    if (index > 0 && (index + 1) < nameWithoutNumber.length()) {
      nameWithoutNumber = nameWithoutNumber.substring (0, index);
    }
    int nextSplitNumber = 1;
    for(int i=0; i < annotParent.size(); i++) {
      SeqFeatureI annotSibling = annotParent.getFeatureAt(i);
      String this_name = annotSibling.getName();
      if (this_name.startsWith(nameWithoutNumber)) {
        try {
          int splitNumber = getSplitNumber(this_name);
          if (splitNumber >= nextSplitNumber)
            nextSplitNumber = splitNumber +1;
        } // if no # throws exception. no-op - just move on to next annot
        catch (NumberFormatException e) {}
      }
    }
    return nameWithoutNumber+":"+nextSplitNumber;
  }

  /** Parses out :# at the end of splitName (eg Hph:3). If no :# or # is not a number
      throws NumberFormatException */
  private int getSplitNumber(String splitName) throws NumberFormatException {
    int splitNumber = 0;
    int index = splitName.indexOf (":");
    if (index == -1) // no split number - throw exception
      throw new NumberFormatException("No split number present");
    //if (index > 0) { 
    // HAS COLON (with # presumably - if not throws exception)
    String num_str = splitName.substring (index + 1);
    int namenum = Integer.parseInt(num_str); //  throws number fmt ex
    return namenum;
      //}
  }


  /** Construct a name for a new transcript */
  private String generateTranscriptName(SeqFeatureI transcript) {
    return generateTranscriptNameOrId(transcript,transcript.getRefFeature().getName());
  }
  protected String generateTranscriptId(SeqFeatureI trans) {
    return generateTranscriptNameOrId(trans,trans.getRefFeature().getId());
  }
//     SeqFeatureI gene = transcript.getRefFeature();

//     Vector transcripts = gene.getFeatures();
//     int t_index = transcripts.indexOf(transcript);
//     if (t_index < 0)
//       t_index = transcripts.size();

//     // Constructing the transcript name from the gene name led to inconsistent
//     // names.  For example, duplicating transcript fzy:CG4274-RA created a new
//     // transcript called (CG4274):fzy-RB.  Using the gene ID instead seems to
//     // yield the desired transcript name (fzy:CG4274-RB).  But if this is bad
//     // for some reason, let me know.  --NH 06/02/03
//     //      new_name = gene.getName() + suffix;
//     // 12/15/2003 Restored creation of transcript name from gene name (not ID)
//     // if gene is temp, dont make transcript temp as well 
//     // one temp will do it for trigger
//     boolean addTemp = !isTemp(gene.getName());
//     String suffix = generateTranscriptSuffix(t_index, transcripts, addTemp);
//     String new_name = gene.getName() + suffix;

//     return new_name;
//   }

  /** Returns true if this annot type needs a suffix (e.g. -RA).
      Currently this decision is made by whether it is a first-tier annotation.
      Test should probably be whether this is a single-level annot (which can
      now be specified in tiers file). */
  public boolean needsSuffix(String type) {
    return Config.getPropertyScheme().getTierProperty(type).isFirstTier();
  }

  /** generates suffix and appends it to annotNameOrId 
      could also do this with a boolean instead of the String(?) */
  private String generateTranscriptNameOrId(SeqFeatureI transcript,
                                            String annotNameOrId) {
    SeqFeatureI annot = transcript.getRefFeature();

    Vector transcripts = annot.getFeatures();
    int t_index = transcripts.indexOf(transcript);
    if (t_index < 0)
      t_index = transcripts.size();

    // Constructing the transcript name from the annot name led to inconsistent
    // names.  For example, duplicating transcript fzy:CG4274-RA created a new
    // transcript called (CG4274):fzy-RB.  Using the annot ID instead seems to
    // yield the desired transcript name (fzy:CG4274-RB).  But if this is bad
    // for some reason, let me know.  --NH 06/02/03
    //      new_name = annot.getName() + suffix;
    // 12/15/2003 Restored creation of transcript name from annot name (not ID)
    // if annot is temp, dont make transcript temp as well 
    // one temp will do it for trigger

//     // If "transcript" is not for a protein-coding gene, it shouldn't have
//     // any suffix.
//     if (!annot.isProteinCodingGene())
    // 9/2005: The test for whether to add a suffix is whether this is a first-tier annot
    // (but perhaps should be whether this is a single-level annot)
    if (!needsSuffix(annot.getFeatureType()))
      return annotNameOrId;

    boolean addTemp = !isTemp(annotNameOrId);
    String suffix = generateTranscriptSuffix(t_index, transcripts, addTemp);
    String newNameOrId = annotNameOrId + suffix;

    return newNameOrId;
  }
  
  /** Return -transcript#:temp, where # is transcript number. If suffix already being
      used, uses the first unused #. :temp is added to trip chado trigger to create
      new transcript name - which prevents naming collisions. */
  protected String generateTranscriptSuffix(int t_index, Vector transcripts,
                                            boolean addTemp) {
	//el - should always increase the index by one, otherwise will have conflicts
	//with index 1 (0->1, 1->1, 2->2)
    //if (t_index == 0) 
    ++t_index; // 0-based -> 1-based - more namelike
    String suffix = getTranscriptSuffixRoot()+t_index;
    // make sure suffix isnt in use without temp (prev existing transcripts)
    //t_index has been incremented, decrement it in the calls to suffixInUse to avoid a suffix self check 
    if (suffixInUse(transcripts,suffix,t_index-1)) {
      ++t_index;
      suffix = generateTranscriptSuffix(t_index,transcripts,false); // no temp
    }
    if (addTemp)
      suffix += ":temp";
    if (suffixInUse (transcripts, suffix, t_index-1)) {
      // If that suffix was used, recursively choose a new one.
      return(generateTranscriptSuffix(t_index+1, transcripts, addTemp));
    }
    return suffix;
  }

  /** generates a ID for a given feature. for annots calls generateAnnotTempId
      which produces a temp id */
  public String generateId(StrandedFeatureSetI annots,
                           String curation_name,
                           SeqFeatureI feature) {
    if (feature.isTranscript()) {
      // Have to use id for the chado db
      // The following statements are copied from generateTranscriptName(Transcript) method.
      // They should be merged with that method and refactor to a new method
      // called makeTransIdentity(Transcript tran, String prefix)
      return generateTranscriptId(feature);
//       SeqFeatureI gene = feature.getRefFeature();
//       Vector transcripts = gene.getFeatures();
//       int t_index = transcripts.indexOf (feature);
//       if (t_index < 0)
//         t_index = transcripts.size();
//       boolean addTemp = !isTemp(gene.getId()); // add temp if gene is not temp
//       return gene.getId() + generateTranscriptSuffix(t_index,transcripts,addTemp);
    } 
    else if (feature.hasAnnotatedFeature()) {
      // take out cur name suffix
      return generateAnnotTempId (annots,getIDPrefix(feature));
    } else
      return "???";
  }

//   protected String generateTransciptId(SeqFeatureI transcript) {
//     SeqFeatureI annot = transcript.getRefFeature();
//     Vector transcripts = annot.getFeatures();
//     int t_index = transcripts.indexOf (transcript);
//     if (t_index < 0)
//       t_index = transcripts.size();
//     boolean addTemp = !isTemp(annot.getId()); // add temp if annot is not temp
//     return annot.getId() + generateTranscriptSuffix(t_index,transcripts,addTemp);
//   }

  /** Generate the next number for use in temporary annotation id 
   in DefaultNameAdapter theres also a nextAnnotNumber but it takes a sfs and a Class -
   this seems funny */
  private int nextAnnotNumber(StrandedFeatureSetI annots) {
    // Don't increment it yet--we'll see if we really need to.
    int num = annotNumber;
    // Check all the annots to make sure this number wasn't already used
//Vector features=SeqFeatureUtil.getFeaturesOfClass(annots,AnnotatedFeature.class,false);
    for(int i=0; i < annots.size(); i++) {
      SeqFeatureI g = annots.getFeatureAt(i);
      String this_id = g.getId();
      num = skipUsedTempNum(this_id, num);
    }
    
    // need to check against SequenceEdits as well (otherwise will lead to possible id
    // collisions)
    SequenceI seq = annots.getRefSequence();
    if (seq != null) {
      HashMap errors = seq.getGenomicErrors();
      if (errors != null) {
        for (Object o : errors.values()) {
          SeqFeatureI error = (SeqFeatureI)o;
          num = skipUsedTempNum(error.getId(), num);
        }
      }
    }

    // Check list of Transactions in AnnotationChangeLog, and exclude
    // any temp IDs found there. - so i assume this is to skp over deleted
    // temp ids - but does it matter? i guess for coalescing it might
    //AnnotationChangeLog acl = AnnotationChangeLog.getAnnotationChangeLog();

    // this doesnt work anymore - its using old style transactions not new

    for (int i=0; i < getTransactionManager().size(); i++) {
      //if (getTransactionManager().getTransaction(i) != null) { // not needed
      Transaction trans = getTransactionManager().getTransaction(i);
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


  // Checks whether feature's name is an ID appropriate for this kind of feature
  // this could use a better comment - logic here is confusing
  // where is this used - this is fly specific it seems
  public boolean nameIsId (SeqFeatureI feature) {
    String prefix;
    boolean is_ID = false;
    if (feature instanceof ExonI) {
      prefix = "EX";
      is_ID = (getCG (feature.getName(), prefix) != null ||
               feature.getName().startsWith (prefix+":temp") ||
               (feature.getName().startsWith (prefix) &&
                feature.getName().indexOf ("tmp") > 0));
      prefix = "CG";
    } else if (feature instanceof Transcript) {
      prefix = "CT";
      is_ID = (getCG (feature.getName(), prefix) != null ||
               feature.getName().startsWith (prefix+":temp") ||
               (feature.getName().startsWith (prefix) &&
                feature.getName().indexOf ("tmp") > 0));
      prefix = "CG";
    } else if (feature instanceof AnnotatedFeature) {
      prefix = "CG";
      is_ID = (getCG (feature.getName(), prefix) != null ||
               feature.getName().startsWith (prefix+":temp") ||
               (feature.getName().startsWith (prefix) &&
                feature.getName().indexOf ("tmp") > 0));
      prefix = "CR";
    } else
      prefix = "???";

    is_ID |= (getCG (feature.getName(), prefix) != null ||
              feature.getName().startsWith (prefix+":temp") ||
              (feature.getName().startsWith (prefix) &&
               feature.getName().indexOf ("tmp") > 0));

    return is_ID;
  }

  protected boolean isAnnotId(SeqFeatureI annot) {
    // check if name and id are same?
    return annot.getName().startsWith(getIDPrefix(annot));
  }

  private boolean isNoName(String name) {
    return name.equals(SeqFeatureI.NO_NAME);
  }

  /** This is for top level annots (eg gene). not underling annots (transcript)
      sets gene name, adds old gene name as synonym, sets transcript names. */
  public CompoundTransaction setAnnotName(AnnotatedFeatureI annot, String newName) {
    CompoundTransaction ct = new CompoundTransaction(this);
    String oldName = annot.getName(); // get old name before edit
    ct.addTransaction(setName(annot,newName));
    ct.addTransaction(addSynonym(annot,oldName));
    ct.addTransaction(setAllTranscriptNamesFromAnnot(annot));
    return ct;
  }

  private CompoundTransaction setAllTranscriptNamesFromAnnot(AnnotatedFeatureI annot) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    for(int i=0; i < annot.size(); i++) {
      AnnotatedFeatureI trans = annot.getFeatureAt(i).getAnnotatedFeature();
      // Set transcript name based on parent's name
      CompoundTransaction t = setTranscriptNameFromAnnot(trans, annot);
      compTrans.addTransaction(t);
    }
    return compTrans;
  }

  /* 12/08/2003:
     Transcript names (symbols) should inherit from parent annotation symbol,
     not parent annotation ID.
     Transcript ID (passed as argument) is used only for its *suffix*--
     prefix is ignored. */
  public CompoundTransaction setTranscriptNameFromAnnot(AnnotatedFeatureI trans,
                                         AnnotatedFeatureI ann) {
    // get existing suffix from transcript (not generating new one)
    String transcriptSuffix = getTranscriptSuffix(trans,ann);
    String transcript_name = ann.getName() + transcriptSuffix;
    return setTranscriptName(trans,transcript_name);
  }
  
  public CompoundTransaction setTranscriptName(AnnotatedFeatureI trans,String name) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    //if(trans.getName()==null||(trans.getName()!=null&&!trans.getName().equals(name))){
      //trans.setName(transcript_name);
    if (trans.getName() != null && trans.getName().equals(name))
      return null;

    String oldName = trans.getName();

    UpdateTransaction ut = super.setName(trans,name); // from DefaultNameAdap
    compTrans.addTransaction(ut);
//    //Change exon names, too(needed for Chado) actually not? no ex names, just ids?
//      setExonNamesFromTranscript(trans);
    // what if not protein coding - should we wipe out old prot names?
    if (trans.getRefFeature().isProteinCodingGene()) {
      // When transcript name is changed, change cDNA and peptide names also
      // rice/chado jdbc doesnt need to do this as the updating of seq names is
      // automatic via trigger (same with transcript & exons)
      // need these transactions for undo (rice trigger supressed for now)
      CompoundTransaction seqTrans = setSeqNamesFromTranscript(trans);
      compTrans.addTransaction(seqTrans);
    }

    // add transcript synonym (if not null,no_name, or temp) from DefNA
    AddTransaction at = super.addSynonym(trans,oldName);
    compTrans.addTransaction(at);

    return compTrans;
  }

  public CompoundTransaction setTranscriptId(SeqFeatureI trans,String id) {
    CompoundTransaction compTrans = super.setTranscriptId(trans,id);
    
    if (trans.getRefFeature().isProteinCodingGene()) {
      compTrans.addTransaction(setPeptideIdFromTranscript(trans));
    }
    // synonym??
    // AddTransaction at = super.addSynonym(trans,oldId);
    // compTrans.addTransaction(at);
    compTrans.setSource(this);
    return compTrans;
  }

  /** chado generates peptide ids not apollo. apollo either keeps the existing
      accession, and if null just creates a temp pep id (just uses name) */
  protected UpdateTransaction setPeptideIdFromTranscript(SeqFeatureI transcript) {
    /*// if already set leave it be...
    if (transcript.getPeptideSequence() != null 
        && transcript.getPeptideSequence().getAccessionNo() != null)
      return null;*/
    
    // if already set leave it be...
    //No, if the transcript has a tmp name and not the peptide, we need to modify this (case spotted on gene split)
    if (transcript.getPeptideSequence() != null 
        && transcript.getPeptideSequence().getAccessionNo() != null
        && !isTemp(transcript.getId()))
      return null;

    // just need anything with temp in it!
    String tempPepId = generatePeptideNameFromTranscriptName(transcript.getId());
    if (tempPepId == null)
      tempPepId = transcript.getName() + getPeptideSuffixRoot() + getTempSuffix();
    // its probably already temp - but just in case
    if (!isTemp(tempPepId))
      tempPepId += getTempSuffix();

    String oldPepId = null; // null required above
    TransactionSubpart ts = TransactionSubpart.PEPTIDE_ID;
    UpdateTransaction ut = new UpdateTransaction(transcript,ts,oldPepId,tempPepId);
    ut.editModel();
    return ut;
  }

  // this is currently not actually used - was only called by setTranscriptIdsFromAnnot
  // which GmodNameAdapter doesnt use (explicit id cng & id cng via type cng)
//   /** Exon id with range: Gene:start-end, it would be nice if fly 
//       would adopt this naming as well. Called from setTranscriptIdFromAnnot */
//   protected void setExonIdsFromTranscript(SeqFeatureI transcript) {
//     for (int i=0; i<transcript.getNumberOfChildren(); i++) {
//       SeqFeatureI exon = transcript.getFeatureAt(i);
//       String annotTopId = transcript.getRefFeature().getId();
//       // eventually should create event as well... and return it
//       exon.setId(annotTopId + ":" + exon.getStart() + "-" + exon.getEnd());
//     }
//   }

  protected String getTranscriptSuffix(SeqFeatureI trans,AnnotatedFeatureI annot) {
    // so in both rice & flybase the suffixes are on names and not on ids
    //String transcript_id = (trans.getId() == null ? trans.getName() : trans.getId());
    String transcriptName = trans.getName();
    int index = (transcriptName != null ? transcriptName.lastIndexOf("-") : -1);
    if (index > 0)
      return transcriptName.substring(index);
    else if (annot != null) {
      boolean addTemp = !isTemp(annot.getName());
      return generateTranscriptSuffix(annot.getFeatureIndex(trans),
                                      annot.getFeatures(),addTemp);
    }
    else { // shouldnt happen
      logger.error("FlyNameAdap.getTransSuffix trans "+trans+" has no suffix");
      return "";
    }
  }

  /** Rice/gmod does peptide names (but not cdna names). apollo never displays seq
      names and rice triggers automatically change prot names on transcript name change
      This is actually needed to 
      give peptides a unique id for putting together chado transactions, otherwise it
      defaults to transcript name and things get messed up. 
      This is called by setTranscriptNameFromAnnot(). used to be called
      by FED as well - no longer.*/
  protected CompoundTransaction setSeqNamesFromTranscript(AnnotatedFeatureI trans) {
    CompoundTransaction ct = new CompoundTransaction(this);
    if (trans.hasPeptideSequence()) {
      String pepName = generatePeptideNameFromTranscriptName(trans.getName());
      //String pepId = generatePeptideIdFromTranscriptId(trans.getId());
      //trans.getPeptideSequence().setAccessionNo(pepName); -> editModel
      // Peptides need their own feature! This should take a peptide feat 
      // and NAME subpart!
      TransactionSubpart ts = TransactionSubpart.PEPTIDE_NAME;
      UpdateTransaction t = new UpdateTransaction(trans,ts);
      //t.setOldSubpartValue(trans.getPeptideSequence().getAccessionNo());
      t.setOldSubpartValue(trans.getPeptideSequence().getName());
      //      t.setNewSubpartValue(pepName);
      // Sequence accession should be id, not name
      t.setNewSubpartValue(pepName);
      t.editModel(); // setAccessionNo (should it do diplayId? probably)
      ct.addTransaction(t);
    }
    return ct;
  }

  /** Generates peptide name from existing transcript name. 
      Replace transcript suffix with peptide suffix */
  public String generatePeptideNameFromTranscriptName(String transcriptName) {
    String pepName = transcriptName; // default? -- shouldnt happen
    int index = transcriptName.indexOf(getTranscriptSuffixRoot());
    if (index == -1) // shouldnt happen
      return null;

    // transfer transcript ordinal to peptide. might include :temp as well
    String ordinal =
      transcriptName.substring(index + getTranscriptSuffixRoot().length());
    pepName = transcriptName.substring(0,index) + getPeptideSuffixRoot() + ordinal;
    return pepName;
  }

  /** Generates peptide id from existing transcript id. 
      Replace transcript suffix with peptide suffix */
  public String generatePeptideIdFromTranscriptId(String transcriptId) {
    // this will do it for trans id - should rename method?
    return generatePeptideNameFromTranscriptName(transcriptId);
  }

  private final static String transcriptSuffixRoot = "-transcript";
  /** This needs some explaining. This is the root of the suffix. Its the part of the
      transcript suffix that doesnt change. so for rice/gmod its "-transcript", 
      for fly its "-R" */
  protected String getTranscriptSuffixRoot() {
    return transcriptSuffixRoot;
  }
 

  private final static String peptideSuffixRoot = "-protein";
  /** The root of the suffix is the non changing part of the suffix - the suffix\
      minus the ordinal. for gmod/rice its "-protein" */
  protected String getPeptideSuffixRoot() {
    return peptideSuffixRoot;
  }

  /** This is used as a check if transcript suffix editing is allowed in the annot
      info editor. gmod/rice disallows this by default so this is actually not
      relevant yet for rice. (fly allows this so its important there). 
      move to fly? */
  public String getTranscriptNamePattern() {
    String pat = ".*"+getTranscriptSuffixRoot()+getTranscriptOrdinalPattern();
    if (transcriptCanHaveTempSuffix())
      pat += "("+getTempSuffix()+")*"; // is that right? * on parentheses?
    return pat;
  }

  protected String getTranscriptOrdinalPattern() {
    if (transcriptOrdinalIsNumeric())
      return "\\d+";
    else
      return "[A-Z]+";
  }

  /** Return true if transcripts do numbers in suffix, false if do letter [A-Z]
      in suffix as ordinal. Gmod/rice is numeric(true), fly is alpha(false) */
  protected boolean transcriptOrdinalIsNumeric() {
    return true;
  }

  /** Returns true. gmod temps transcript suffixes (by default). the :temp indicates
      that the db needs to create a new transcript name */
  protected boolean transcriptCanHaveTempSuffix() {
    return true;
  }

  private String getTempSuffix() { return ":temp"; }

  /** Returns true if changing type from oldType to newType will cause a change
      in feature ID, i.e. the ID prefix will change to reflect the new type. 
      This code should probably be moved to default name adapter */
  public boolean typeChangeCausesIdChange(String oldType, String newType) {
    return !getIDPrefix(oldType).equals(getIDPrefix(newType));
  }

  /** convert oldId to a new id in new type format */
  public String getNewIdFromTypeChange(String oldId,String oldType,String newType) {
    String oldPrefix = getIDPrefix(oldType);
    String oldSuffix = oldId.substring(oldPrefix.length()); // strip off prefix
    String newPrefix = getIDPrefix(newType);
    return newPrefix + oldSuffix;
  }

  /** Special version for fly--knows proper format for transcript names */
  protected boolean match(SeqFeatureI sf, String matchString) {
    if (sf.getFeatureType().equals("transcript"))
      return Pattern.matches(getTranscriptNamePattern(), matchString);
    // Special case: if ID has "temp" or "tmp" in it, just check 
    // the *prefix* for legality
    else if (sf.getId().indexOf("temp") >= 0 ||
             sf.getId().indexOf("tmp") >= 0) {
      FeatureProperty fp 
        = Config.getPropertyScheme().getFeatureProperty(sf.getTopLevelType());
      if (fp == null || (fp != null && fp.getIdFormat() == null))
        // Return FALSE if no pattern specified for type, because
        // if we say "true" to match, that has more implications.
        // 2/12/04 That seems to result in the wrong behavior--condemning an ID
        // for having the "wrong" format when in fact no format pattern
        // is defined.
        return true; 
      else
        return Pattern.matches(getPrefix(fp.getIdFormat()), 
                               getPrefix(matchString));
    }

    else
      return super.match(sf, matchString);
  }

  /** funny method - if name starts with prefix it returns everything up to the 
   * first colon - if there is a colon right after the prefix (CG:) it returns null -
   * this is true of fly temp ids (CG:temp1:...) this is used by name IsId which is fly
   * specific
   */
  private String getCG (String name, String prefix) {
    String cg_name = null;
    String tmp_name = name;
    if (tmp_name.startsWith (prefix)) {
      int index = tmp_name.indexOf (":");
      if (index > 0)
        tmp_name = tmp_name.substring (0, index);
      if (!tmp_name.equals (prefix)) {
        //        String num_str = tmp_name.substring(prefix.length());
        cg_name = tmp_name;
      }
    }
    return cg_name;
  }

  /** A simpler signature for this might be (AnnotatedFeatureI gene, String suffix)
      I dont think t_index is necasary as the suffix in question should be
      new - even different from the transcript it came from if the trans has
      a name yet - but maybe im missing something */
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


  /** Returns the prefix of a SeqFeature's ID format (e.g. "CG" for genes) */
  protected String getIDPrefix(SeqFeatureI sf) {
    return getIDPrefix(sf.getFeatureType());
  }

  private String getIDPrefix(String type) {
    FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(type);
    if (fp == null || (fp != null && fp.getIdFormat() == null))
      return getDefaultIDPrefix(); // ??  What should be the default?

    String idFormat = fp.getIdFormat();
    return getPrefix(idFormat);
  }

  /** The prefix to use if idFormat is not specified in tiers file for a type */
  protected String getDefaultIDPrefix() {
    // Print message saying to set in tiers file??
    return "GMOD";
  }

  /** Given a format string (e.g. RICE\d) returns the prefix (e.g. RICE)
      This allows for colons ater prefix, fly doesnt. */
  protected String getPrefix(String idFormat) {
    Pattern p = Pattern.compile("[a-zA-Z_]+:*");
    Matcher m = p.matcher(idFormat);
    boolean patternFound = m.find();
    if (!patternFound)
      return idFormat; // ?
    return idFormat.substring(0,m.end());
  }

  public String getNewChadoDbUniquename(String featType, long pk)
  {
    int minimumIntegerDigits = 8;
    String delim = ":";
    NumberFormat nf = NumberFormat.getNumberInstance();
    nf.setGroupingUsed(false);
    nf.setMinimumIntegerDigits(minimumIntegerDigits);
    return getIDPrefix(featType) + delim + nf.format(pk);    
  }
  
  public String getNewChadoDbUniquename(SeqFeatureI feat, long pk)
  {
    return getNewChadoDbUniquename(feat.getFeatureType(), pk);
  }

}


  /** Construct a temp name for a new top level annotation - temp names are identical
      to temp ids
 */
//   protected String generateAnnotName(StrandedFeatureSetI annots,
//                          String curation_name,
//                          SeqFeatureI annot) {
//     return generateAnnotTempId(annots, getIDPrefix(annot));
//   }
//     String annotName = annot.getName();
//       // If it's not named yet, its a temp id or if its an ID, make a temp ID 
//     if (isTemp(annot) || isAnnotId(annot) || isNoName(annotName)) {
//       return generateAnnotTempId(annots, getIDPrefix(annot));
//     }

//     // This is for gene splits with real names - it uses old name but tacks on
//     // a number to each split off gene - not working for rice temp splits
//     // this is getting called on splits - but i think something is amiss - i thought
//     // splits caused 2 temped genes (no using of old name) or is that merges?
//     // separate split method for clarity?
//     else {
//       return generateAnnotSplitName(annot,annots);
//     }
//   }
// --> refactored into generateSplitName
//   /** parses out numbers from all other genes with same prefix, and returns the highest
//       number plus one.  Called by generateAnnotName. This is used for genes with real
//       names (not ids) that have been split */
//   protected int getNameCount(StrandedFeatureSetI annots, Class featureClass,
//                            int start, String prefix) {
//     Vector features = SeqFeatureUtil.getFeaturesOfClass(annots,featureClass,false);
//     int num = start;
//     for(int i=0; i < features.size(); i++) {
//       SeqFeatureI g = (SeqFeatureI) features.elementAt(i);
//       String this_name = g.getName();
//       if (this_name.startsWith(prefix)) {
//         try {
//           // check for any temp in the name
//           int index = this_name.indexOf (":temp");
//           String num_str;
//           // then parse out the count from the name

//           // TEMP
//           if (index > 0)
//             num_str = this_name.substring (index + ":temp".length());

//           // NOT TEMP (REAL)
//           else {
//             index = this_name.indexOf (":");
//             if (index > 0) // HAS COLON (with # presumably)
//               num_str = this_name.substring (index + 1);
//           // this is looking for the case where the gene name is a number - 
//           // dont think this can happen
//             else // NO COLON - just use prefix - if not num, exception caught, start def
//               num_str = this_name.substring(prefix.length());
//           }
//           int namenum = Integer.parseInt(num_str);
//           if (namenum >= num) {
//             num = namenum + 1;
//           }
//         } catch (NumberFormatException e) {}
//       }
//     }
//     return num;
//   }
