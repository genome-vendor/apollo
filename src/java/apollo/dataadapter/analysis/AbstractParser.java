/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.util.FastaHeader;
import apollo.analysis.filter.AnalysisInput;

import org.apache.log4j.*;

public abstract class AbstractParser implements AnalysisParserI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AbstractParser.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String program = "";
  String version = "";
  String database = "";
  CurationSet curation;
  boolean new_curation;
  AnalysisInput input;
  /* all of the parsers go about this the same way */
  int line_number = 0;
  BufferedReader data;
  String line = null;
  boolean parsed;
  FeatureSetI forward_analysis;
  FeatureSetI reverse_analysis;

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    logger.error (this.getClass().getName() + 
                  ".load is Abstract and should never be used");
    return null;
  }

  public abstract boolean recognizedInput ();
  public abstract String getProgram ();

  protected void setHitScore(FeatureSetI hit, SeqFeatureI span) {
    if (hit.size() == 1 || span.getScore() > hit.getScore()) {
      hit.setScore (span.getScore());
    }
  }

  protected SequenceI initSequence(CurationSet curation,
				   String header, 
				   int length) {
    FastaHeader fasta = new FastaHeader(header);
    String seq_id = fasta.getSeqId();
    SequenceI seq = curation.getSequence (seq_id);
    if (seq == null) {
      if (seq_id.indexOf(';') >= 0) {
	seq_id = seq_id.replace(';', ' ');
	seq_id = seq_id.trim();
	seq = curation.getSequence (seq_id);
      }
      if (seq == null) {
	seq = fasta.generateSequence();
	curation.addSequence (seq);
      }
    }
    seq.setLength (length);
    return seq;
  }

  protected FeatureSetI initSet (String id, int strand) {
    String value;

    FeatureSetI hit = new FeatureSet ();
    hit.setStrand (strand);
    hit.setRefSequence (curation.getRefSequence());
    hit.setProgramName (program);
    hit.setDatabase (database);
    hit.setFeatureType (getAnalysisType());
    hit.setId (id);
    return hit;
  }

  protected String getAnalysisType () {
    if (database.equals(""))
      return program;// + ":dummy";
    else
      return program + ":" + database;
  }

  protected FeatureSetI initAnalysis (StrandedFeatureSetI analyses,
				      int strand,
				      String type) {
    String analysis_type = getAnalysisType();
    String analysis_name = (analysis_type +
			    (strand == 1 ? "-plus" :
			     (strand == -1 ? "-minus" : "")));

    FeatureSetI analysis = null;
    FeatureSetI check_analyses = (strand == 1 ?
				  analyses.getForwardSet() :
				  analyses.getReverseSet());
    // PropertyScheme scheme = Config.getPropertyScheme();
    for (int i = 0; i < check_analyses.size() && analysis == null; i++) {
      FeatureSetI sf = (FeatureSetI) analyses.getFeatureAt(i);
      String sf_type = sf.getFeatureType();
      analysis = ((sf_type.equals(analysis_type))
                  ? sf : null);
    }
    if (analysis == null) {
      analysis = new FeatureSet();
      analysis.setStrand (strand);
      analysis.setFeatureType (analysis_type);
      analyses.addFeature(analysis);
    }
    else
      logger.debug ("Found existing analysis of type " + analysis_type);
    analysis.setProgramName(program);
    analysis.setDatabase(database);
    analysis.setName (analysis_name);
    if (type != null)
      analysis.setTopLevelType (type);
    if (version != null)
      analysis.addProperty ("version", version);
    return analysis;
  }

  protected SeqFeatureI initFeature () {
    SeqFeatureI sf = new SeqFeature();
    sf.setFeatureType (getAnalysisType());
    sf.setRefSequence (curation.getRefSequence());
    return sf;
  }

  protected void initCuration (CurationSet curation, SequenceI focal_seq) {
    boolean update = false;
    if (focal_seq != null && curation.getRefSequence() == null) {
      curation.setRefSequence(focal_seq);
      update = true;
    }
    if ((focal_seq == null) ||
	(focal_seq != null && focal_seq != curation.getRefSequence())) {
      focal_seq = curation.getRefSequence();
      update = true;
    }
    if (update && focal_seq != null) {
      //      System.out.println ("initCuration: parsing " + program + " analysis of sequence " + 
      //                          focal_seq.getName() + " (" +
      //                          focal_seq.getLength() + "bp)");  // DEL
      curation.setName (focal_seq.getName());
      curation.setStrand (1);
      curation.setStart(1);
      curation.setEnd(focal_seq.getLength());
      logger.debug ("initCuration: sequence range for " + focal_seq.getName() +" is " + curation.getStart() + " to " +
                    curation.getEnd());
    }
  }

  protected void initLoad (CurationSet curation,
			   boolean new_curation,
			   InputStream data_stream,
			   AnalysisInput input) {
    this.input = input;
    this.curation = curation;
    this.new_curation = new_curation;
    this.database = input.getDatabase();

    data = null;
    parsed = false;
    program = null;

    try	{
      data = new BufferedReader(new InputStreamReader(data_stream));
    }
    catch (Exception e) {
      logger.error ("Unable to open data stream", e);
    }
    if (data != null)
      parsed = recognizedInput();

    if (parsed) {
      program = getProgram();

      if (new_curation)
        initCuration (curation, null);

      logger.info ("Preparing to parse " + program + " results...");
      StrandedFeatureSetI all_analyses = curation.getResults();
      if (all_analyses == null) {
	all_analyses = new StrandedFeatureSet(new FeatureSet(), 
					      new FeatureSet());
	all_analyses.setName ("Analyses");
	curation.setResults (all_analyses);
      }

      String type = input.getType();
      forward_analysis = initAnalysis (all_analyses, 1, type);
      reverse_analysis = initAnalysis (all_analyses, -1, type);
      
      // Set pointers to analysis "twin" on opposite strand, this is needed for
      // results that need to be moved to the opposite strand
      forward_analysis.setAnalogousOppositeStrandFeature(reverse_analysis);
      reverse_analysis.setAnalogousOppositeStrandFeature(forward_analysis);
    }
    else
      this.program = null;
  }

  protected void readLine() throws ParseException {
    try {
      line = data.readLine();
      line_number++;
    }
    catch( Exception ex ) {
      line = null;
      parsed = false;
      throw new ParseException (ex.getMessage(), line_number);
    }
  }

  protected String parseToken(StringTokenizer tokens, String msg) {
    String value = "";
    if (parsed) {
      value = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
      parsed &= !value.equals("");
    }
    return value;
  }

  protected int parseInteger(StringTokenizer tokens, String msg) {
    String value = parseToken(tokens, msg);
    int number = 0;
    if (parsed) {
      try {
        number = Integer.parseInt(value);
      }
      catch (NumberFormatException ex) {
        parsed = false;
      }
    }
    return number;
  }

  protected double parseDouble(StringTokenizer tokens, String msg) {
    String value = parseToken(tokens, msg);
    double number = 0;
    if (parsed) {
      try {
        if (value.equals("."))
          value = "0";
        number = Double.valueOf(value).doubleValue();
      }
      catch (NumberFormatException ex) {
        parsed = false;
      }
    }
    return number;
  }

}

