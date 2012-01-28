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

import apollo.dataadapter.Region;

public class ParameciumNameAdapter  implements ApolloNameAdapterI {

  // -----------------------------------------------------------------------
  // Class variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(TigrAnnotNameAdapter.class); 
   
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private TransactionManager transactionManager;
  private String prefix = "PTET";
  private Connection connection = null;
  private long uniquenameNumber = 0;
  
  protected Pattern validCurationNamePattern = Pattern.compile("^scaffold_(\\d+)$");
  protected Pattern validNamePattern = Pattern.compile("^PTET\\w\\w?(\\d+)$");  
  protected Pattern validBaseNamePattern = Pattern.compile("^PTET\\w\\w?(\\d+)\\d{3}$");
  protected Pattern validPrefixPattern = Pattern.compile("PTET");
  private Hashtable geneNumbers = new Hashtable();
  private ChadoAdapter dataAdapter=null;

  // -----------------------------------------------------------------------
  // Constructor
  // -----------------------------------------------------------------------
  
  public ParameciumNameAdapter() {}
  
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
    	return generateTranscriptName(feature);
    }
    else if (feature.hasAnnotatedFeature()) {
      return generateId(annots,curation_name,feature);
    } else
      return "???";
  }
  
  private String generateTranscriptName(SeqFeatureI transcript) {
    String gene_name = transcript.getRefFeature().getName();
    String transcript_name= gene_name.replaceFirst(prefix+"G", prefix+"T");
    //for pseudogene
    transcript_name= transcript_name.replaceFirst(prefix+"PS", prefix+"T");
    return transcript_name;
  }

  /** Generates name for a given feature. May or may not use associated 
      vector of exon results used to make the annot */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature, Vector exonResults) {
    logger.trace("generateName(StrandedFeatureSetI "+annots+", String "+curation_name+", SeqFeatureI "+feature+", Vector "+exonResults+")");
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
                           String curation_name,
                           SeqFeatureI feature) {
    //Probably the gene or the transcript
    logger.debug("generateId "+annots+" curation_name = "+curation_name+ " feature="+feature+" type ="+feature.getFeatureType());
    if (feature instanceof Transcript) {    	
    	return generateTranscriptName(feature);
    }
    else if(feature instanceof SequenceEdit) {
        String numScaffold = parseNumberScaffoldFromCurationName(curation_name);
	return createSequenceEditId((SequenceEdit) feature, numScaffold);
    }
    else if(feature instanceof AnnotatedFeature) {
    	String numScaffold = parseNumberScaffoldFromCurationName(curation_name);
        return createNewGeneId(numScaffold,feature.getFeatureType())+"-"+getUserName();
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
  
  private boolean sameScaffold(String numScaffold) {
  	Pattern validSameScaffold = Pattern.compile("^"+numScaffold);
  	Matcher m = validSameScaffold.matcher(""+uniquenameNumber);
	if (m.find()) 
		return true;
	return false;
  }
  
  private String createNewGeneId(String numScaffold, String type) {
       if(uniquenameNumber == 0 || !sameScaffold(numScaffold)) {
	    try {
	    	Connection c = getConnection();
		
		if(c == null) {
			logger.info("No connection available");
			uniquenameNumber = Long.parseLong(numScaffold+"00001001");
		} else {
			String sql = "SELECT f.uniquename FROM feature f, featureloc fl "+
				"WHERE fl.srcfeature_id =(select feature_id from feature where uniquename='scaffold_"+numScaffold+"') "+
				" AND f.feature_id = fl.feature_id "+
				" AND (f.uniquename like '"+prefix+"G%' OR f.uniquename like '"+prefix+"PS%') "+
				" order by f.feature_id desc,f.uniquename desc";
			logger.debug(sql);		
			ResultSet rs = executeLoggedSelectQuery("generateNewChadoFeatureUniquename", c, sql);
			if (rs.next()) { 
				String uniquename = rs.getString("uniquename");
				uniquenameNumber = getNumber(uniquename).longValue() + 1000;
			} else {
				uniquenameNumber = Long.parseLong(numScaffold+"00001001");
			}
		}
		
	   
	    } 
	    catch (SQLException sqle) {
	      logger.error("SQLException running "+sqle);
	    }
    } else {
    	uniquenameNumber+=1000;
    } 
    
    if(type.equals("gene"))
    	return prefix+"G"+uniquenameNumber;
    else if(type.equals("pseudogene"))
    	return prefix+"PS"+uniquenameNumber;
    else {
    	logger.error("The type "+type+" is not recognized");
    	return null;
    }
  }

  private String createSequenceEditId(SequenceEdit feature, String numScaffold) {
      if (feature.getEditType().equals(SequenceI.DELETION)) {
        return "DELETION"+numScaffold+"_"+feature.getPosition();
      }
      else if(feature.getEditType().equals(SequenceI.INSERTION)) {
        return "INSERTION"+numScaffold+"_"+feature.getPosition()+feature.getResidue();
      }
      else if(feature.getEditType().equals(SequenceI.SUBSTITUTION)) {
        return "SUBSTITUTION"+numScaffold+"_"+feature.getPosition()+feature.getResidue();
      }
      
      return "SEQUENCINGERROR"+numScaffold+"_"+feature.getPosition();	
  }

  public String generateExonId(StrandedFeatureSetI annots,
                               String curation_name,
                               SeqFeatureI exon, 
                               String geneId) {

	if(exon != null) {
	     if(exon.getId() != null ) {
	        Matcher mexon = validPrefixPattern.matcher(exon.getId());
	        if (mexon.find()) 
		     return exon.getId();
	     }
	     
	     Long geneNumber = getLastExonIndice(""+ getNumber(geneId));
	     String exonId = prefix+"E"+geneNumber+"-"+getUserName();    
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
  private Long getNumber(String uniqueName) {
  	return new Long(uniqueName.replaceAll("[^\\d]+",""));
  }
  
  private Long getLastExonIndice(String geneNum) {
  	Long nextExonIndice = null; 
  	if(geneNumbers.get(geneNum) != null) {
		nextExonIndice = new Long((((Long) geneNumbers.get(geneNum)).longValue() + 1));
	} else {	
	     try {
	    	Connection c = getConnection();
		if(c == null) {
			nextExonIndice = new Long( geneNum );
		} else {
		String exonPrefix = geneNum.replaceFirst("001$","");
		String sql = "SELECT uniquename FROM feature "+
				"WHERE uniquename like '"+prefix+"E"+exonPrefix+"%' order by uniquename desc";
		
		logger.debug(sql);		
		ResultSet rs = executeLoggedSelectQuery("getLastExonIndice", c, sql);
		if (rs.next()) { 
			String uniquename = rs.getString("uniquename");
			nextExonIndice = new Long((getNumber(uniquename).longValue() + 1));
	    		
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
    if(oldType.equals("gene") && newType.equals("pseudogene")) { //gene -> pseudogene
    	return oldId.replaceFirst(prefix+"G", prefix+"PS");
	
    } else if(oldType.equals("pseudogene") && newType.equals("gene")) { //
    	return oldId.replaceFirst(prefix+"PS", prefix+"G");
	
    } else {
        logger.error("Can not convert "+oldId+" from "+oldType+" type "+newType);
    	throw new UnsupportedOperationException();
    }
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
    String prot_name= transcriptName.replaceFirst(prefix+"T", prefix+"P");
    return prot_name;
  }

  public String generatePeptideIdFromTranscriptId(String transcriptId) {
     throw new UnsupportedOperationException();
  }

  public String generateChadoCdsNameFromTranscriptName(String transcriptName) {
    String cds_name= transcriptName.replaceFirst(prefix+"T", prefix+"C");
    return cds_name;
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
  
  private String parseNumberScaffoldFromCurationName(String curationName) {
    //return curationName.replaceAll("scaffold_","");
    String chromosome = curationName;
    return chromosome.replaceAll("scaffold_", "");
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
