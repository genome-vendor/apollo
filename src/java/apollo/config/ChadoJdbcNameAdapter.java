package apollo.config;

import org.apache.log4j.*;

import apollo.datamodel.*;

import apollo.editor.AddTransaction;
import apollo.editor.CompoundTransaction;
import apollo.editor.Transaction;
import apollo.editor.TransactionManager;
import apollo.editor.TransactionSubpart;
import apollo.editor.UpdateTransaction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.Runtime;
import java.util.Vector;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import apollo.dataadapter.chado.ChadoAdapter;
import apollo.dataadapter.chado.ChadoDatabase;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import apollo.dataadapter.ApolloDataAdapterI;

public class ChadoJdbcNameAdapter  implements ApolloNameAdapterI {

  // -----------------------------------------------------------------------
  // Class variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ChadoJdbcNameAdapter.class); 
   
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private TransactionManager transactionManager;
  //private String prefix = "TOTO";
  private Connection connection = null;
  private long uniquenameNumber = 0;
  
  private Hashtable geneNumbers = new Hashtable();
  private ChadoAdapter dataAdapter=null;

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------
  
  public ChadoJdbcNameAdapter() { }
  
  // -----------------------------------------------------------------------
  // ApolloNameAdapterI
  // -----------------------------------------------------------------------
  public void setDataAdapter(ApolloDataAdapterI dataAdapter) {
     if(dataAdapter  instanceof ChadoAdapter)
      this.dataAdapter=(ChadoAdapter) dataAdapter;
  }

  //generates a name for a given feature
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature) {

     logger.debug("generateName "+annots+" curation_name = "+curation_name+ " feature="+feature+" type ="+feature.getFeatureType());
    if (feature.isTranscript()) {
    	return generateTranscriptName(feature, curation_name);
    }
    else if (feature.hasAnnotatedFeature()) {
      return generateId(annots,curation_name,feature);
    } else
      return "???";
  }
  
  private FeatureProperty getFeatureProperty(String type) {
      FeatureProperty fp = Config.getPropertyScheme().getFeatureProperty(type);
      if (fp != null)
        return fp;
      return null;
  }
  
    /** Returns the prefix of a SeqFeature's ID format (e.g. "CG" for genes) */
  protected String getIDPrefix(SeqFeatureI sf) {
    return getIDPrefix(sf.getFeatureType());
  }

  private String getIDPrefix(String type) {  
    FeatureProperty fp = getFeatureProperty(type);
    if (fp == null || (fp != null && fp.getIdPrefix() == null))
      return getDefaultIDPrefix(); // ??  What should be the default?

    return fp.getIdPrefix();
  }

  /** The prefix to use if idFormat is not specified in tiers file for a type */
  protected String getDefaultIDPrefix() {
    // Print message saying to set in tiers file??
    return "TEMP";
  }
  
  
  
  private String getChromosomeFormat(SeqFeatureI sf) {
    return getChromosomeFormat(sf.getFeatureType());
  } 
  
  private String getChromosomeFormat(String type) {
    FeatureProperty fp = getFeatureProperty(type);
    if (fp == null || (fp != null && fp.getChromosomeFormat() == null)) {
      logger.error("No Chromosome prefix for the type "+type);    
      throw new UnsupportedOperationException();
    }
    return fp.getChromosomeFormat(); 
  }
    
  
  private String generateTranscriptName(SeqFeatureI transcript, String curationName) {
    Long geneNumber = getNumber(transcript.getRefFeature().getName(), getIDPrefix(transcript));
    String chromosomePrefix = getChromosomePrefix(curationName,getChromosomeFormat(transcript)); 
    return getIDPrefix(transcript)+typeLetter("transcript")+chromosomePrefix+geneNumber+"-"+getUserName();
  }

  /** Generates name for a given feature. May or may not use associated 
      vector of exon results used to make the annot */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature, Vector exonResults) {
    logger.debug("generateName(StrandedFeatureSetI "+annots+", String "+curation_name+", SeqFeatureI "+feature+", Vector "+exonResults+")");
    return generateName(annots, curation_name, feature); // ignore exonResults
  }


  /** Generate a name for a gene split */
  public String generateAnnotSplitName(SeqFeatureI annot, StrandedFeatureSetI annotParent, String curationName) 
  {
    logger.debug("generateAnnotSplitName called on annot=" + annot + ", parent=" + annotParent + " curation=" + curationName);
    return generateName(annotParent,curationName,annot);
  }


  //generates an id for a given feature
  public String generateId(StrandedFeatureSetI annots, 
                           String curationName,
                           SeqFeatureI feature) {
    //Probably the gene or the transcript
    logger.debug("generateId "+annots+" curation_name = "+curationName+ " feature="+feature+" type ="+feature.getFeatureType());
    if (feature instanceof Transcript) { 
          	
    	return generateTranscriptName(feature, curationName);
    }
    else if(feature instanceof SequenceEdit) {
        String numChromosome =	parseNumberChromosomeFromCurationName(curationName,getChromosomeFormat(feature));
	return createSequenceEditId((SequenceEdit) feature, numChromosome);
    }
    else if(feature instanceof AnnotatedFeature) {
    	String numChromosome = parseNumberChromosomeFromCurationName(curationName,getChromosomeFormat(feature));
	String chromosomePrefix = getChromosomePrefix(curationName,getChromosomeFormat(feature));
        return createNewGeneId(numChromosome, feature.getFeatureType(), curationName, chromosomePrefix)+"-"+getUserName();
    }
    return null;
  }

  public String generateNewId(StrandedFeatureSetI annots, 
			      String curation_name,
			      SeqFeatureI feature) {
    return generateId(annots, curation_name, feature);
  }
  
  private String getUserName() {
    return apollo.editor.UserName.getUserName();
  }
  
  private boolean sameChromosome(String numChromosome) {
  	Pattern validSameChromosome = Pattern.compile("^"+numChromosome);
  	Matcher m = validSameChromosome.matcher(""+uniquenameNumber);
	if (m.find()) 
		return true;
	return false;
  }
  
  private String createNewGeneId(String numChromosome, String type, String curationName, String chromosomePrefix) {
  
       String idPrefix = getIDPrefix(type);
       String typeLetter = typeLetter(type);
       
       if(uniquenameNumber == 0 || !sameChromosome(numChromosome)) {
	    try {
	    	Connection c = getConnection();
		
		if(c == null) {
			logger.info("No connection available");
			uniquenameNumber = Long.parseLong(numChromosome+"000001001");
		} else {
		        //String chromosomePrefix = getChromosomePrefix(type);
			String sql = "SELECT f.uniquename FROM feature f, featureloc fl "+
				"WHERE fl.srcfeature_id =(select feature_id from feature where uniquename='"+curationName+"') "+
				" AND f.feature_id = fl.feature_id "+
				" AND f.uniquename like '"+idPrefix+typeLetter+"%'  "+
				" order by uniquename desc";
			logger.debug(sql);		
			ResultSet rs = executeLoggedSelectQuery("generateNewChadoFeatureUniquename", c, sql);
			if (rs.next()) { 
				String uniquename = rs.getString("uniquename");
				uniquenameNumber = getNumber(uniquename, idPrefix).longValue() + 1000;
			} else {
				uniquenameNumber = Long.parseLong(numChromosome+"000001001");
			}
		}
		
	   
	    } 
	    catch (SQLException sqle) {
	      logger.error("SQLException running "+sqle);
	    }
    } else {
    	uniquenameNumber+=1000;
    } 
    return idPrefix+typeLetter+chromosomePrefix+uniquenameNumber;
  }
  
  
  private String typeLetter(String type) {
    if(type.equals("gene"))
    	return "G";    
    else if(type.equals("transcript"))
    	return "T";
    else if(type.equals("exon"))
    	return "E";	
    else if(type.equals("polypeptide"))
    	return "P";	
    else if(type.equals("CDS"))
    	return "C";	
    else if(type.equals("pseudogene"))
    	return "PS";    
    else {
    	logger.error("The type "+type+" is not recognized");
    	return null;
    }
  
  }

  private String createSequenceEditId(SequenceEdit feature, String numChromosome) {
      if (feature.getEditType().equals(SequenceI.DELETION)) {
        return "DELETION"+numChromosome+"_"+feature.getPosition();
      }
      else if(feature.getEditType().equals(SequenceI.INSERTION)) {
        return "INSERTION"+numChromosome+"_"+feature.getPosition()+feature.getResidue();
      }
      else if(feature.getEditType().equals(SequenceI.SUBSTITUTION)) {
        return "SUBSTITUTION"+numChromosome+"_"+feature.getPosition()+feature.getResidue();
      }
      
      return "SEQUENCINGERROR"+numChromosome+"_"+feature.getPosition();	
  }

  public String generateExonId(StrandedFeatureSetI annots,
                               String curation_name,
                               SeqFeatureI exon, 
                               String geneId) {

	if(exon != null) {
	     
	     String idPrefix = getIDPrefix(exon);
	     if(exon.getId() != null ) {
	        Pattern validPrefixPattern = Pattern.compile("^"+idPrefix);
	        Matcher mexon = validPrefixPattern.matcher(exon.getId());
	        if (mexon.find()) 
		     return exon.getId();
	     }
	     
	     Long exonIndice = getLastExonIndice(""+ getNumber(geneId, idPrefix) ,  idPrefix+typeLetter("exon"));
	     String chromosomePrefix = getChromosomePrefix(curation_name,getChromosomeFormat(exon));
	     String exonId =  idPrefix+typeLetter("exon")+chromosomePrefix+exonIndice+"-"+getUserName() ; 
	     return exonId;
	     
	} else {
		logger.error("Exon id must be not null");   		
	}
	return null;	   
  }
  public String generateNewExonId(StrandedFeatureSetI annots,
				  String curation_name,
				  SeqFeatureI exon, 
				  String geneId) {
    return generateExonId(annots, curation_name, exon, geneId);
  }
  private Long getNumber(String uniqueName, String prefix) {
        String suffixUniqueName = uniqueName.substring(prefix.length()); // strip off prefix
  	return new Long(suffixUniqueName.replaceAll("[^\\d]+",""));
  }
  
  private Long getLastExonIndice(String geneNum, String exonPrefix) {
  	Long nextExonIndice = null; 
  	if(geneNumbers.get(geneNum) != null) {
		nextExonIndice = new Long((((Long) geneNumbers.get(geneNum)).longValue() + 1));
	} else {	
	     try {
	    	Connection c = getConnection();
		if(c == null) {
			nextExonIndice = new Long( geneNum );
		} else {
		String exonGeneNumber = geneNum.replaceFirst("001$","");
		String sql = "SELECT uniquename FROM feature "+
				"WHERE uniquename like '"+exonPrefix+exonGeneNumber+"%' order by uniquename desc";
		
		logger.debug(sql);		
		ResultSet rs = executeLoggedSelectQuery("getLastExonIndice", c, sql);
		if (rs.next()) { 
			String uniquename = rs.getString("uniquename");
			nextExonIndice = new Long((getNumber(uniquename, exonPrefix ).longValue() + 1));
	    		
		} else {
			nextExonIndice = new Long( geneNum );
		}
		
		}
		
							   
	    } 
	    catch (SQLException sqle) {
	      logger.error("SQLException running ");// + sql, sqle);
	    }
  		
  	}
	geneNumbers.put(geneNum, nextExonIndice);
	return nextExonIndice;
  }
  


  /** 
   * This method is used by some adapters/databases to update the exon's id to 
   * reflect its new coordinates any time the exon's location is updated.  */
  public void updateExonId(ExonI exon) {
    Transcript transcript = (Transcript) exon.getParent();
    AnnotatedFeatureI gene = transcript.getGene();
    String newId = generateExonId(null, null, exon, gene.getId());
    exon.setId(newId);
  }


  //ADDED by TAIR
  public String getSuffixDelimiter() {
    throw new UnsupportedOperationException();
  }

  public boolean checkName(String name,Class featureClass) {
    return false;
  }

  // sets the name of a given feature - not used anymore
  //public void setName(SeqFeatureI feature, String name);

  /** Set name for top level annot. may set synonym and transcript names as well 
      depending on subclass */
  public CompoundTransaction setAnnotName(AnnotatedFeatureI annot,String newName) {
    CompoundTransaction ct = new CompoundTransaction(this);
    String oldName = annot.getName(); // get old name before edit
    ct.addTransaction(setName(annot,newName));
    ct.addTransaction(addSynonym(annot,oldName));
    ct.addTransaction(setAllTranscriptNamesFromAnnot(annot)); // not in default
    return ct;
  }


  /** Sets transcripts name. may also set peptide & cdna seq accession */
  public CompoundTransaction setTranscriptName(AnnotatedFeatureI trans,String name) {
    // TODO - copied this verbatim from DefaultNameAdapter
    String oldName = trans.getName(); // before edit
    CompoundTransaction ct = new CompoundTransaction(this);
    UpdateTransaction ut = setName(trans,name);
    ct.addTransaction(ut);
    return ct;
  }


  /** Sets transcript id, may also set peptide id */
  public CompoundTransaction setTranscriptId(SeqFeatureI trans, String id) {
    // TODO - copied this verbatim from DefaultNameAdapter
    CompoundTransaction ct = new CompoundTransaction(this);
    ct.addTransaction(setId(trans,id));
    return ct;
  }


  /** Sets the name of a transcript based upon its annot parent.
      May also set exon names. May also set peptide accession */
  public CompoundTransaction setTranscriptNameFromAnnot(AnnotatedFeatureI current,
                                         AnnotatedFeatureI parent) {
    String new_current_name = parent.getName() +"-"+ current.getFeatureType();
    return setTranscriptName(current,new_current_name);
  }


  //find out if name is an id string
  public boolean nameIsId (SeqFeatureI feature) {
 //   String featId = feature.getId();
//    String featName = feature.getName();
//    logger.info("nameIsId called for feature with id=" + featId + " name=" + featName);
    //return isValidFeatureUniquename(featName, feature);
    return true;
  }



  public boolean suffixInUse(Vector transcripts, String suffix, int t_index) {
    throw new UnsupportedOperationException();
  }


  /** Returns true if id jibes with seq feature's ID format. This can be used for both
   * ids and names that mirror the id */
  public boolean checkFormat(SeqFeatureI feat,String id) {
    throw new UnsupportedOperationException();
  }


  /** Return true if id and name have same format */
  public boolean idAndNameHaveSameFormat(SeqFeatureI feat,String id, String name) {
    return false;
  }


  /** Returns expected pattern (if any) for transcript names */
  public String getTranscriptNamePattern() {
    throw new UnsupportedOperationException();
  }


  /** Returns true if changing type from oldType to newType will cause a change
      in feature ID, i.e. the ID prefix will change to reflect the new type */
  public boolean typeChangeCausesIdChange(String oldType, String newType) {
    return true;
  }


  public String getNewIdFromTypeChange(String oldId,String oldType,String newType) {
    String oldPrefix = getIDPrefix(oldType)+typeLetter(oldType);
    String oldSuffix = oldId.substring(oldPrefix.length()); // strip off prefix
    String newPrefix = getIDPrefix(newType);
    return newPrefix +typeLetter(newType)+ oldSuffix;
  }


  /** A name adapter needs a TransactionManager. Has to make sure new temp id
      isnt in log. */
  public void setTransactionManager(TransactionManager tm) {
    this.transactionManager = tm;
  }

  //public void setAnnotationChangeLog(AnnotationChangeLog acl);

  /** Set annots id to id. May set annot subparts ids as well.
      Returns a CompoundTransaction of all the transactions that have
      occurred. */
  public CompoundTransaction setAnnotId(AnnotatedFeatureI annot, String id) {
    //annot.setId(id);
    UpdateTransaction ut = setId(annot,id); // modifies model
    CompoundTransaction compoundTrans = new CompoundTransaction(this);
    compoundTrans.addTransaction(ut);
    return compoundTrans;  
  }


  /** Returns true if id/name String is a temp id/name. Default is to look for
      "temp" in id or name. */
  public boolean isTemp(String idOrName) {
    // this name adapter does not generate temporary ids:
    return false;
  }


  /** Generate a peptide name given a transcript name. */
  public String generatePeptideNameFromTranscriptName(String transcriptName) {
    String prefix = getIDPrefix("transcript");
    String peptideName= transcriptName.replaceFirst(prefix+typeLetter("transcript"), prefix+typeLetter("polypeptide"));
    return peptideName;
  }

  public String generatePeptideIdFromTranscriptId(String transcriptId) {
     throw new UnsupportedOperationException();
  }

  public String generateChadoCdsNameFromTranscriptName(String transcriptName) {
    String prefix = getIDPrefix("transcript");
    String CDSName= transcriptName.replaceFirst(prefix+typeLetter("transcript"), prefix+typeLetter("CDS"));
    return CDSName;
  }

  public String generateChadoCdsIdFromTranscriptId(String transcriptId) {
    return generateChadoCdsNameFromTranscriptName(transcriptId);
  }
  //-----------------------
  // protected methods
  //-----------------------
  private Connection getConnection()  throws SQLException {
    if (dataAdapter != null && (connection == null || connection.isClosed())) {
      
      ChadoDatabase chadodatabase = dataAdapter.getActiveDatabase();
      logger.debug("ParameciumNameAdaptor: connecting with '" + chadodatabase.getJdbcUrl() + 
      			"' username='" + chadodatabase.getLogin() + "' password='****'");
    
      connection = DriverManager.getConnection(
      					chadodatabase.getJdbcUrl(),
					 chadodatabase.getLogin(),
					  chadodatabase.getPassword());
    }
    return connection; 
  }
  private ResultSet executeLoggedSelectQuery(String method, Connection c, String sql) {
   ResultSet rs = null;
   // Since all queries are executed via this method the Log4J LocationInfo won't
   // be of any use.  Therefore we'll manually add the calling method name to the
   // nested diagnostic context:

    try {
      if (c == null) { c = getConnection(); }
   logger.debug(sql);
   
      Statement s = c.createStatement();
      rs = s.executeQuery(sql);
    } 
    catch (SQLException sqle) {
      logger.error("SQLException running " + sql, sqle);
    }
    return rs;
  }
  
    /** returns an add transaction for adding synonym to annot. return null if
      syn is null,no_name, or temp - should probably also check if synonym exists
      already  */
  private AddTransaction addSynonym(AnnotatedFeatureI annFeat, String synString) {
    if (synString == null || synString.equals(SeqFeatureI.NO_NAME))
      return null;
    if (annFeat.hasSynonym(synString))
      return null;
    TransactionSubpart ts = TransactionSubpart.SYNONYM;
    Synonym synonym = new Synonym(synString);
    AddTransaction at = new AddTransaction(annFeat,ts,synonym);
    at.editModel();
    return at;
  }
  
  private CompoundTransaction setAllTranscriptNamesFromAnnot(AnnotatedFeatureI annot) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    for(int i=0; i < annot.size(); i++) {
      AnnotatedFeatureI trans = annot.getFeatureAt(i).getAnnotatedFeature();
      // Set transcript name based on parent's name
      CompoundTransaction t = setTranscriptNameFromAnnot(trans, annot);
      compTrans.addTransaction(t);
      compTrans.addTransaction(setAllExonNamesFromAnnot(trans,annot.getName()));
    }
    return compTrans;
  }
  private CompoundTransaction setAllExonNamesFromAnnot(AnnotatedFeatureI annot,String newName) {
    CompoundTransaction compTrans = new CompoundTransaction(this);
    for(int i=0; i < annot.size(); i++) {
      FeatureSetI exon = annot.getFeatureAt(i).getAnnotatedFeature();
      // Set transcript name based on parent's name
      CompoundTransaction t = setExonNameFromAnnot(exon, newName);
      compTrans.addTransaction(t);
    }
    return compTrans;  
  }
  
  private CompoundTransaction setExonNameFromAnnot(FeatureSetI exon,String newName) {
    //String new_exon_name = newName +":"+ exon.getStart()+".."+exon.getEnd();
    String new_exon_name = generateExonId(null,null,exon,newName);
    //same method for rename an exon or a transcript
    return setTranscriptName((AnnotatedFeatureI) exon,new_exon_name);
  }
  
  private String parseNumberChromosomeFromCurationName(String curationName,String chromosomeFormat) {
     Pattern numberChromosome = Pattern.compile(chromosomeFormat);
     Matcher m = numberChromosome.matcher(curationName);
     if (m.find()) 
        return m.group(1);
     else 
        logger.error("curation name "+curationName+" does not match with the chromosome format "+chromosomeFormat);
     return null;
  }
  
  private String getChromosomePrefix(String curationName, String chromosomeFormat) {
    String chromosomeNumber = parseNumberChromosomeFromCurationName(curationName,chromosomeFormat);
    Pattern numberZero = Pattern.compile("^(0+)");
    Matcher m = numberZero.matcher(chromosomeNumber);
    if (m.find()) 
        return m.group(1);
    return "";
  } 
  
  // HACK - copied this from DefaultNameAdapter
  protected UpdateTransaction setName(AnnotatedFeatureI annFeat, String newName) {
    TransactionSubpart ts = TransactionSubpart.NAME;
    UpdateTransaction ut = new UpdateTransaction(annFeat,ts,annFeat.getName(),newName);
    ut.editModel();
    return ut;
  }
  protected UpdateTransaction setId(SeqFeatureI annot, String newId) {
    // TODO - copied this verbatim from DefaultNameAdapter
    TransactionSubpart ts = TransactionSubpart.ID;
    String oldId = annot.getId();
    UpdateTransaction ut = new UpdateTransaction(annot,ts,oldId,newId);
    ut.setOldId(oldId);
    ut.setNewId(newId);
    ut.editModel(); // makes change to annot
    return ut;
  }
}
