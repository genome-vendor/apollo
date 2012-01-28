package apollo.dataadapter.otter.parser;

import apollo.datamodel.*;
import apollo.editor.UserName;
import java.io.*;
import java.util.*;

/**
 * I visit elements of the Apollo datamodel, converting each them into XML with my
 * visit() methods. I am typically invoked with a call like: 
 * <code>
 * GenericAnnotationSet set = ...
 * OtterXMLRenderingVisitor visitor = new OtterXMLRenderingVisitor();
 * set.visit(visitor); //will create XML for all features in the set
 * String xml = visitor.getReturnBuffer().toString();
 * </code>
**/
public class OtterXMLRenderingVisitor
implements apollo.util.Visitor
{
  private int indent = 0;
  private StringBuffer returnBuffer = new StringBuffer();
  private String indentString = "";

  private static String [] genePropNames = { 
                                            TagHandler.AUTHOR, 
                                            TagHandler.AUTHOR_EMAIL
                                           }; 

  private static String [] transcriptPropNames = { 
                                                  TagHandler.CDS_START_NOT_FOUND,
                                                  TagHandler.CDS_END_NOT_FOUND, 
                                                  TagHandler.MRNA_START_NOT_FOUND,
                                                  TagHandler.MRNA_END_NOT_FOUND
                                                 }; 
  
  private int getIndent(){
    return indent;
  }//end getIndent
  
  private void incrementIndent(){
    indentString += TagHandler.INDENT;
    indent++;
  }//end incrementIndent
  
  private void decrementIndent(){
    indent--;
    if(indent >=1){
      indentString = indentString.substring(0, indentString.length()-2);
    }//end if
  }//end decrementIndent
  
  private String getIndentString(){
    return indentString;
  }//end getIndentString

  public StringBuffer getReturnBuffer(){
    return returnBuffer;
  }//end getReturnBuffer

  private void setReturnBuffer(StringBuffer newValue){
    returnBuffer = newValue;
  }//end setReturnBuffer
  
  private StringBuffer append(String append){
    return getReturnBuffer().append(append);
  }//end appendToReturnBuffer
  
  public void visit(SeqFeature feature){
    throw new apollo.dataadapter.NotImplementedException("This is not implemented");
  }

  private StringBuffer open(String tag){
    return
        append(TagHandler.LEFT)
        .append(tag)
        .append(TagHandler.RIGHT);
  }
  
  private StringBuffer close(String tag){
    return
        append(TagHandler.LEFT)
        .append(TagHandler.SLASH)
        .append(tag)
        .append(TagHandler.RIGHT);
  }
  
  private StringBuffer retrn(){
    return append(TagHandler.RETURN);
  }
  
  private void wrap(String tag, String value){
    append(getIndentString());
    open(tag).append(value);
    close(tag);
    retrn();
  }
  
  private void wrapPropertiesForFeature(SeqFeature feature, String [] propNames){
    Iterator keys;
    String key;
    String value;
    
    for (int i=0;i<propNames.length;i++) {
      key = propNames[i];
      if (feature.getProperties().containsKey(key)) {
        value = feature.getProperty(key);
        wrap(key, value);
      } else {
        wrap(key, "");
      }
    }
  }//end wrapPropertiesForFeature
  
  public void visit(AssemblyFeature feature){
     System.out.println("Visiting assembly_feature"); 
    append(getIndentString());
    open(TagHandler.SEQUENCE_FRAGMENT);
    retrn();

    incrementIndent();
    
    // wrapPropertiesForFeature(feature);
    
    //<assembly_start>1922</assembly_start>
    wrap(TagHandler.ID,feature.getId());
    wrap(TagHandler.CHROMOSOME,feature.getChromosome());
    wrap(TagHandler.ASSEMBLY_START,String.valueOf(feature.getLow()));
    //<assembly_end>1922</assemblyend>
    wrap(TagHandler.ASSEMBLY_END,String.valueOf(feature.getHigh()));
    wrap(TagHandler.FRAGMENT_ORI,String.valueOf(feature.getStrand()));
    wrap(TagHandler.FRAGMENT_OFFSET,String.valueOf(feature.getFragmentOffset()));
    if (feature.getAccession() != null) {
      wrap(TagHandler.ACCESSION,feature.getAccession());
      wrap(TagHandler.VERSION,String.valueOf(feature.getVersion()));
    }
    if (feature.getKeywords() != null) {
      Iterator keywordIterator = feature.getKeywords().iterator();
      while(keywordIterator.hasNext()){
        wrap(TagHandler.KEYWORD,(String)keywordIterator.next());
      }//end while
    }

    if (feature.getRemarks() != null) {
      Iterator remarkIterator = feature.getRemarks().iterator();
      while(remarkIterator.hasNext()){
        wrap(TagHandler.REMARK, (String)remarkIterator.next());
      }//end while
    }
    
    wrapAuthorDetails(feature);

    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.SEQUENCE_FRAGMENT);
    retrn();
    
  }  

  /**
   * We may encounter "lesser" (non-stranded) feature sets along the way, which we 
   * will ignore - ie we'll just run visit on their child features, but not create
   * any extra xml structure for the sets themselves. At time of writing I am doing
   * this to accomodate the fact that the annotations contain genes, and extra feature
   * sets containing the assembly features.
  **/
  public void visit(FeatureSet feature){
    Iterator featureIterator = feature.getFeatures().iterator();
    while(featureIterator.hasNext()){
      ((SeqFeature)featureIterator.next()).accept(this);
      append(TagHandler.RETURN);
    }//end while
  }

  /**
   * The stranded feature set should be the root of the tree, so we open/close
   * the root <otter> tags
  **/
  public void visit(StrandedFeatureSet feature){
//    open(TagHandler.OTTER);
//    retrn();
//    
//    incrementIndent();
//    
//    append(getIndentString());
//    open(TagHandler.SEQUENCE_SET);
//    retrn();

    Iterator featureIterator = feature.getFeatures().iterator();
    incrementIndent();
    
    // wrapPropertiesForFeature(feature);
    
    while(featureIterator.hasNext()){
      //
      //this processes all contents of the set - assemblyfeatures,
      //genes and their child transcripts and exons.
      ((SeqFeature)featureIterator.next()).accept(this);
      append(TagHandler.RETURN);
    }//end while
    decrementIndent();
    
//    append(getIndentString());
//    close(TagHandler.SEQUENCE_SET);
//    retrn();
//    
//    decrementIndent();
//    
//    close(TagHandler.OTTER);
  }//end visit 

  public void visit(CurationSet feature){
    open(TagHandler.OTTER);
    retrn();
    
    incrementIndent();
    
    append(getIndentString());
    open(TagHandler.SEQUENCE_SET);
    retrn();

    if (feature.getAssemblyType() != null) {
      wrap(TagHandler.ASSEMBLY_TYPE, feature.getAssemblyType());
    }

    Iterator featureIterator = feature.getAnnots().getFeatures().iterator();
    incrementIndent();
    
    // wrapPropertiesForFeature(feature);
    
    while(featureIterator.hasNext()){
      //
      //this processes all contents of the set - assemblyfeatures,
      //genes and their child transcripts and exons.
      ((SeqFeature)featureIterator.next()).accept(this);
      append(TagHandler.RETURN);
    }//end while
    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.SEQUENCE_SET);
    retrn();
    
    decrementIndent();
    
    close(TagHandler.OTTER);
  }//end visit 
  
  private void wrapAuthorDetails(SeqFeature feature) {

    if (feature.getProperties().containsKey(TagHandler.AUTHOR)) {
      wrap(TagHandler.AUTHOR,feature.getProperty(TagHandler.AUTHOR));
    } else {
      wrap(TagHandler.AUTHOR,UserName.getUserName());
    }

    if (feature.getProperties().containsKey(TagHandler.AUTHOR_EMAIL)) {
      wrap(TagHandler.AUTHOR_EMAIL,feature.getProperty(TagHandler.AUTHOR_EMAIL));
    } else {
      wrap(TagHandler.AUTHOR_EMAIL,UserName.getUserName() + "@sanger.ac.uk");
    }
  }

  public void visit(AnnotatedFeature feature){
    append(getIndentString());
    open(TagHandler.LOCUS);
    retrn();

    incrementIndent();
    
    wrap(TagHandler.LOCUS_TYPE, feature.getTopLevelType());

    // Temporary location for name
    Iterator dbxrefs = feature.getDbXrefs().iterator();
    boolean foundId = false;
    while(dbxrefs.hasNext()){
      DbXref dbx = (DbXref)dbxrefs.next();
      if (dbx.getIdType().equals("OtterId")) {
        wrap(TagHandler.STABLE_ID, dbx.getIdValue());
        foundId = true;
      }
    }
    if (!foundId) {
      wrap(TagHandler.STABLE_ID, "");
    }
    wrap(TagHandler.NAME, feature.getName());

    Iterator synonyms = feature.getSynonyms().iterator();
    while(synonyms.hasNext()){
      wrap(TagHandler.SYNONYM, ((String)synonyms.next()));
    }
    
    Iterator comments = feature.getComments().iterator();
    while(comments.hasNext()){
      wrap(TagHandler.REMARK, ((Comment)comments.next()).getText());
    }//end while

    wrapAuthorDetails(feature);

    if (feature.getProperties().containsKey(TagHandler.KNOWN)) {
      wrap(TagHandler.KNOWN,feature.getProperty(TagHandler.KNOWN));
    } 
    
    Iterator featureIterator = feature.getFeatures().iterator();

    while(featureIterator.hasNext()){
      //
      //this processes all contents of the set - assemblyfeatures,
      //genes and their child transcripts and exons.
      ((SeqFeature)featureIterator.next()).accept(this);
      append(TagHandler.RETURN);
    }//end while

    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.LOCUS);
    retrn();
  }
  
  public void visit(Transcript feature){
    Iterator featureIterator = feature.getFeatures().iterator();
    Iterator evidenceIterator = feature.getEvidence().iterator();
    Evidence evidence;
    
    append(getIndentString());
    open(TagHandler.TRANSCRIPT);
    retrn();
    
    incrementIndent();

    Iterator dbxrefs = feature.getDbXrefs().iterator();
    boolean foundId = false;
    while(dbxrefs.hasNext()){
      DbXref dbx = (DbXref)dbxrefs.next();
      if (dbx.getIdType().equals("OtterId")) {
        wrap(TagHandler.STABLE_ID, dbx.getIdValue());
        foundId = true;
      }
    }
    if (!foundId) {
      wrap(TagHandler.STABLE_ID, "");
    }
    
    wrap(TagHandler.NAME, feature.getName());
    
//    Iterator synonyms = feature.getSynonyms().iterator();
//    if (synonyms.hasNext()) {
//      while(synonyms.hasNext()){
//        wrap(TagHandler.NAME, ((String)synonyms.next()));
//      }
//    } else {
//      wrap(TagHandler.NAME, "");
//    }

    wrapAuthorDetails(feature);

    Iterator comments = feature.getComments().iterator();
    while(comments.hasNext()){
      wrap(TagHandler.REMARK, ((Comment)comments.next()).getText());
    }//end while
    

    wrap(TagHandler.TRANSCRIPT_CLASS, feature.getTopLevelType());

    wrapPropertiesForFeature(feature,transcriptPropNames);
    
    //
    //print out evidence
    while(evidenceIterator.hasNext()){
      evidence = (Evidence)evidenceIterator.next();
      evidence.accept(this);
    }
 
    if (feature.getTranslationStart() != 0) {
      if (feature.getTranslationEnd() == 0) {
        feature.setTranslationEndNoPhase(feature.getEnd());
      }
      wrap(TagHandler.TRANSLATION_START,String.valueOf(feature.getTranslationStart()));
      wrap(TagHandler.TRANSLATION_END,String.valueOf(feature.getTranslationEnd()));
      boolean foundTranslationId = false;
      dbxrefs = feature.getDbXrefs().iterator();
      while(dbxrefs.hasNext()){
        DbXref dbx = (DbXref)dbxrefs.next();
        if (dbx.getIdType().equals("OtterTranslationId")) {
          wrap(TagHandler.TRANSLATION_STABLE_ID, dbx.getIdValue());
          foundTranslationId = true;
        }
      }
      if (!foundTranslationId) {
        wrap(TagHandler.TRANSLATION_STABLE_ID, "");
      }
    }
    //
    //print out all exons.
    while(featureIterator.hasNext()){
      //
      //this processes all contents of the set - assemblyfeatures,
      //genes and their child transcripts and exons.
      ((SeqFeature)featureIterator.next()).accept(this);
      append(TagHandler.RETURN);
    }//end while
    
    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.TRANSCRIPT);
    retrn();
  }
  
  public void visit(Exon feature){
    append(getIndentString());
    open(TagHandler.EXON);
    retrn();

    incrementIndent();
    
    wrap(TagHandler.STABLE_ID, feature.getProperty("OtterStableId"));
    wrap(TagHandler.START, String.valueOf(feature.getLow()));
    wrap(TagHandler.END, String.valueOf(feature.getHigh()));
    wrap(TagHandler.STRAND, String.valueOf(feature.getStrand()));
    int exonType = feature.getCodingProperties();

    if (exonType == CodingPropertiesI.MIXED_5PRIME || exonType == CodingPropertiesI.MIXED_BOTH) {
      wrap(TagHandler.FRAME, "0");
    } else if (exonType == CodingPropertiesI.CODING || exonType == CodingPropertiesI.MIXED_3PRIME) {
      wrap(TagHandler.FRAME, String.valueOf(feature.getPhase()));
    } else {
       //System.out.println("No frame for exon " + feature.getId());
    } 

    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.EXON);
    retrn();
  }

  public void visit(Evidence evidence){
    append(getIndentString());
    open(TagHandler.EVIDENCE);
    retrn();
    
    incrementIndent();
    
    wrap(TagHandler.NAME, evidence.getSetId());
    wrap(TagHandler.TYPE, evidence.getDbType());
    
    decrementIndent();
    
    append(getIndentString());
    close(TagHandler.EVIDENCE);
    retrn();
  }
  
}//end OtterXMLRenderingVistor


