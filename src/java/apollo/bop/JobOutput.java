package apollo.bop;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.sql.*;

import apollo.datamodel.*;
import apollo.dataadapter.exception.BopException;

import org.apache.log4j.*;

public class JobOutput {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(JobOutput.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String name;
  String raw_output;
  java.util.Date run_date;
  String program;
  String dataset;
  String analysis_type;
  SequenceI query_seq;

  public JobOutput() {
  }

  public String getRawOutput () {
    return raw_output;
  }
  
  public void setRawOutput (String raw) {
    this.raw_output = raw;
  }

  public java.util.Date getRunDate () {
    return run_date;
  }
  
  public void setRunDate (long processed_time) {
    if (processed_time > 0)
      run_date = new java.util.Date(processed_time * 1000);
    else
      run_date = new java.util.Date(); //default to now
  }

  public void setQuerySequence(SequenceI seq) {
    this.query_seq = seq;
  }

  public SequenceI getQuerySequence() {
    return query_seq;
  }

  public String getAnalysisType () {
    return analysis_type.toLowerCase();
  }

  public String getProgram() {
    return program;
  }

  public String getDatabase() {
    return dataset;
  }

  public void setAnalysisType(String program, String dataset) 
    throws BopException {
    this.program = program;
    this.dataset = dataset;
    String type_key;
    analysis_type = null;
    if (dataset != null && !dataset.equalsIgnoreCase("dummy"))
      type_key = program + ":" + dataset;
    else
      type_key = program;

    String prefs_file = apollo.util.IOUtil.findFile ("bop.prefs");
    int tok;
    StreamTokenizer tokens;
    InputStream prefs_stream;
      
    if ( prefs_file != null && ! prefs_file.equals ("") ) {
      if ( ! (new File (prefs_file)).canRead () ) {
	throw new BopException("Can't read options from " + prefs_file);
      }
      
      try {
	prefs_stream = new FileInputStream (prefs_file);
	Reader r = new BufferedReader(new InputStreamReader(prefs_stream));
	tokens = new StreamTokenizer(r);
	tokens.eolIsSignificant(false);
	tokens.quoteChar('"');
      }
      catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
	return;
      }
          
      try {
	while ((tok = tokens.nextToken()) != tokens.TT_EOF && 
	       analysis_type == null) {
	  String pref_type = tokens.sval;
	  if (pref_type.equals("filter")) {
	    //ignore
	    tokens.nextToken();
	    tokens.nextToken();
	  }
	  else if (pref_type.equals("output")) {
	    //ignore
	    tokens.nextToken();
	    tokens.nextToken();
	  } 
	  else if (pref_type.equals("analysis")) {
	    tokens.nextToken();
	    String parsed_type = tokens.sval;
	    tokens.nextToken();
	    String parsed_type_key = (tokens.sval).toLowerCase();
	    if (parsed_type_key.equals(type_key))
	      analysis_type = parsed_type;
	  }
	  else {
	    //ignore
	  }
	}
      } catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
      }
      try {
	prefs_stream.close();
      } catch( Exception ex ) {
	logger.error(ex.getMessage(), ex);
      }    
    } else {
      logger.error ("Can not parse as analysis type can not be determined "+
                    "without pref file");
    }
    if (analysis_type == null)
      analysis_type = type_key;
  }      
}
