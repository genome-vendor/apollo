package apollo.dataadapter;

import java.util.*;

/**
 * Central class for state-information keys
**/
public class StateInformation extends Properties {

  public Set allowedSet = new HashSet();

  //
  //This should be universal.
  public static final String REGION = "region";
  
  //
  //set up for ensj.
  public static final String LOGGING_FILE = "loggingFile";

  // key to indicate type of input
  public static final String INPUT_TYPE = "Input Type";
  // For input ids of many types - type indicated by INPUT_TYPE
  public static final String INPUT_ID = "Input";
  public static final String INPUT_STRING = "InputString";
  
  public static final String DATA_ADAPTER = "adapter";

  //keys for buttons saying which data types to include.
  public static final String INCLUDE_GENE = "include.Gene";
  public static final String INCLUDE_GENE_TYPES = "INCLUDE_GENE_TYPES";
  public static final String INCLUDE_DNA_PROTEIN_ALIGNMENT = "include.DnaProteinAlignment";
  public static final String INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES = "INCLUDE_DNA_PROTEIN_ALIGNMENT_TYPES";
  public static final String INCLUDE_DNA_DNA_ALIGNMENT = "include.DnaDnaAlignment";
  public static final String INCLUDE_DNA_DNA_ALIGNMENT_TYPES = "INCLUDE_DNA_DNA_ALIGNMENT_TYPES";
  public static final String INCLUDE_FEATURE = "include.Feature";
  public static final String INCLUDE_FEATURE_TYPES = "INCLUDE_FEATURE";
  public static final String INCLUDE_DITAG_FEATURE = "include.DitagFeature";
  public static final String INCLUDE_DITAG_TYPES = "INCLUDE_DITAG_FEATURE_TYPES";
  public static final String INCLUDE_SIMPLE_PEPTIDE_FEATURE = "include.SimplePeptideFeature";
  public static final String INCLUDE_SIMPLE_PEPTIDE_FEATURE_TYPES = "INCLUDE_SIMPLE_PEPTIDE_FEATURE_TYPES";
  public static final String INCLUDE_REPEAT_FEATURE = "include.RepeatFeature";
  public static final String INCLUDE_REPEAT_FEATURE_TYPES = "INCLUDE_REPEAT_FEATURE_TYPES";
  public static final String INCLUDE_CONTIG_FEATURE = "include.ContigFeature";
  public static final String INCLUDE_CONTIG_FEATURE_TYPES = "INCLUDE_CONTIG_FEATURE_TYPES";
  public static final String INCLUDE_PREDICTION_TRANSCRIPT = "include.PredictionTranscript";
  public static final String INCLUDE_PREDICTION_TRANSCRIPT_TYPES = "INCLUDE_PREDICTION_TRANSCRIPT_TYPES";
  public static final String INCLUDE_VARIATION = "include.Variation";

  //keys for fine control of data loading
  public static final String RESET_GENE_START_AND_STOP = "resetStartAndStop.Gene";
  public static final String AGGRESSIVE_GENE_NAMING  = "aggressiveNaming.Gene";
  public static final String ADD_RESULT_GENES_AS_ANNOTATIONS  = "addResultAsAnnotation.Gene";
  public static final String ADD_TRANSCRIPT_SUPPORT = "addSupport.Transcript";
  public static final String TYPE_PREFIX_STRING = "typePrefix.Gene";
  
  //keys for saying which database to read from.
  public static final String JDBC_DRIVER = "jdbc_driver";
  public static final String HOST = "host";
  public static final String PORT = "port";
  public static final String USER = "user";
  public static final String PASSWORD = "password";
  public static final String ENSEMBL_DRIVER = "ensembl_driver";
  public static final String SIGNATURE = "signature";
  public static final String DATABASE = "database";
  public static final String NAME = "name";
  public static final String CONNECTION_STRING = "connection_string";
  public static final String SCHEMA_VERSION = "schema_version";

  //keys for which annotation server/file to read/write from/to.
  public static final String INPUT_FILE_NAME = "AnnotationInputFile";
  public static final String OUTPUT_FILE_NAME  = "AnnotationOutputFile";
  public static final String INPUT_SERVER_NAME  = "AnnotationInputServer";
  public static final String SERVER_PORT  = "AnnotationServerPort";
  public static final String OUTPUT_SERVER_NAME  = "AnnotationOutputServer";
  public static final String INPUT_DATA_SET  = "AnnotationInputDataSet";
  public static final String OUTPUT_DATA_SET  = "AnnotationOutputDataSet";
  public static final String AUTHOR  = "AnnotationAuthor";
  public static final String AUTHOR_EMAIL  = "AnnotationAuthorEmail";
  public static final String LOCK = "AnnotationLock";
  
  //DAS. keys pointing at server url, the dataset (eg species) you want
  //and the segment (eg contig or chromosome id) and start/end.
  public static final String SERVER_URL = "DAS_server_url";
  public static final String DSN_SOURCE_ID = "DSN_sourceId";
  public static final String DSN_SOURCE_VERSION = "DSN_sourceVersion";
  public static final String DSN_SOURCE = "DSN_source";
  public static final String DSN_MAP_MASTER = "DSN_mapMaster";
  public static final String DSN_DESCRIPTION = "DSN_description";
  
  public static final String SEGMENT_SEGMENT = "Segment_segment";
  public static final String SEGMENT_ID = "Segment_id";
  public static final String SEGMENT_START = "Segment_start";
  public static final String SEGMENT_STOP = "Segment_stop";
  public static final String SEGMENT_ORIENTATION= "Segment_orientation";
  public static final String SEGMENT_SUBPARTS = "Segment_subparts";
  public static final String SEGMENT_LENGTH = "Segment_length";
  
  public static final String HTTP_PROXY_SET = "http.proxySet";
  public static final String HTTP_PROXY_HOST = "http.proxyHost";
  public static final String HTTP_PROXY_PORT = "http.proxyPort";
  
  //
  //File based adapters
  public static final String DATA_FILE_NAME = "dataFileName";
  public static final String SEQUENCE_FILE_NAME = "sequenceFileName";
  
  public StateInformation(){
    allowedSet.add(REGION);
    
    allowedSet.add(INPUT_TYPE);
    allowedSet.add(INPUT_ID);
    allowedSet.add(INPUT_STRING);
    allowedSet.add(DATA_ADAPTER);
    
    allowedSet.add(LOGGING_FILE);
    allowedSet.add(INCLUDE_GENE);
    allowedSet.add(INCLUDE_DNA_PROTEIN_ALIGNMENT);
    allowedSet.add(INCLUDE_DNA_DNA_ALIGNMENT);
    allowedSet.add(INCLUDE_FEATURE);
    allowedSet.add(INCLUDE_SIMPLE_PEPTIDE_FEATURE);
    allowedSet.add(INCLUDE_REPEAT_FEATURE);
    allowedSet.add(INCLUDE_CONTIG_FEATURE);
    allowedSet.add(INCLUDE_DITAG_FEATURE);
    allowedSet.add(INCLUDE_PREDICTION_TRANSCRIPT);
    allowedSet.add(INCLUDE_VARIATION);
    
    allowedSet.add(RESET_GENE_START_AND_STOP);
    allowedSet.add(AGGRESSIVE_GENE_NAMING);
    allowedSet.add(ADD_RESULT_GENES_AS_ANNOTATIONS);
    allowedSet.add(ADD_TRANSCRIPT_SUPPORT);
    allowedSet.add(TYPE_PREFIX_STRING);

    allowedSet.add(JDBC_DRIVER);
    allowedSet.add(HOST);
    allowedSet.add(PORT);
    allowedSet.add(USER);
    allowedSet.add(PASSWORD);
    
    allowedSet.add(ENSEMBL_DRIVER);
    allowedSet.add(SIGNATURE);
    allowedSet.add(DATABASE);
    allowedSet.add(NAME);
    allowedSet.add(CONNECTION_STRING);
    allowedSet.add(SCHEMA_VERSION);
    
    allowedSet.add(INPUT_FILE_NAME);
    allowedSet.add(OUTPUT_FILE_NAME);
    allowedSet.add(INPUT_SERVER_NAME);
    allowedSet.add(SERVER_PORT);
    allowedSet.add(OUTPUT_SERVER_NAME);
    allowedSet.add(INPUT_DATA_SET);
    allowedSet.add(OUTPUT_DATA_SET);
    allowedSet.add(AUTHOR);
    allowedSet.add(AUTHOR_EMAIL);
    allowedSet.add(LOCK);
    
    allowedSet.add(SERVER_URL);
    allowedSet.add(DSN_SOURCE_ID);
    allowedSet.add(DSN_SOURCE_VERSION);
    allowedSet.add(DSN_SOURCE);
    allowedSet.add(DSN_MAP_MASTER);
    allowedSet.add(DSN_DESCRIPTION);
    allowedSet.add(SEGMENT_SEGMENT);
    allowedSet.add(SEGMENT_ID);
    allowedSet.add(SEGMENT_START);
    allowedSet.add(SEGMENT_STOP);
    allowedSet.add(SEGMENT_ORIENTATION);
    allowedSet.add(SEGMENT_SUBPARTS);
    allowedSet.add(SEGMENT_LENGTH);
    
    allowedSet.add(HTTP_PROXY_SET);
    allowedSet.add(HTTP_PROXY_HOST);
    allowedSet.add(HTTP_PROXY_PORT);
    
    allowedSet.add(DATA_FILE_NAME);
    allowedSet.add(SEQUENCE_FILE_NAME);
  }
  
  public Object setProperty(String key, String property){
    if(!allowedSet.contains(key)){
      throw new IllegalStateException("Attempt to add an unrecognised property: "+key+" to StateInformation");
    }
    
    return super.setProperty(key, property);
  }
}
