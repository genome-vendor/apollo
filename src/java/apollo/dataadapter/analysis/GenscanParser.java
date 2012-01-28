/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.analysis.filter.AnalysisInput;

public class GenscanParser extends GeneFinderParser {
  private double donor_score;
  private double acceptor_score;
  private double coding_potential;
  private double probability;

  private static String date_key = "Date run: ";
  private static String matrix_key = "Parameter matrix: ";
  private static String seq_key = "Sequence ";
  private static String gene_key = "Gn.Ex";
  private static String program_key = "GENSCAN";
  private static String pep_key = "Predicted peptide ";
  private static String sub_key = "Suboptimal exons";

  private static String[] comments = {
    program_key,
    date_key,
    matrix_key,
    seq_key,
    pep_key,
    sub_key,
    "----- ",
    "Predicted genes",
    gene_key
  };

  public GenscanParser () {
  }

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad(curation, new_curation, data_stream, input);
    if (parsed) {
      try {
	if (version != null) {
	  forward_analysis.addProperty ("version", version);
	  reverse_analysis.addProperty ("version", version);
	}
	parsed = grabGenes (curation, forward_analysis, reverse_analysis);
	data.close();
      }      
      catch(Exception ex) {
	System.out.println(ex.getMessage());
	ex.printStackTrace();
	parsed = false;
      }
    }
    return (parsed ? getAnalysisType() : null);
  }

  protected boolean isCommentary () {
    boolean a_comment = line.equals("");
    for (int i = 0; i < comments.length && !a_comment; i++) {
      a_comment = (line.indexOf(comments[i]) >= 0);
      if (a_comment) {
	comment = line;
      }
    }
    return a_comment;
  }

  public boolean recognizedInput () {
    int i = 0;
    while (!parsed && i < 10) {
      parseLine();
      parsed &= (comment.startsWith(program_key));
      i++;
    }
    if (!parsed)
      program = null;
    else {
      program = program_key;
      version = parseVersion();
    }
    return parsed;
  }

  protected String parseVersion() {
    //don't assume first line start with "GENSCAN"
    String version = comment.substring (program_key.length());
    int index = version.indexOf ('\t');
    if (index >= 0) {
      version = version.substring (0, index);
    }
    return version;
  }

  protected boolean parseLine () {
    comment = "";
    seq_name = "";
    source = "";
    type = "";
    low = 0;
    high = 0;
    score = 0;
    strand = 0;
    phase = "";
    gene_id = "";

    /* 
       Gn.Ex Type S .Begin ...End .Len Fr Ph I/Ac Do/T CodRg P.... Tscr..
       ----- ---- - ------ ------ ---- -- -- ---- ---- ----- ----- ------
       Input lines look like this
       1.01 Sngl +   1799   2611  813  1  0   23   38   987 0.824  82.98
       
       The interpretation of this that I am using is as follows:
       S = strand
       Begin = starting base
       End = last base included in the exon
       Len = total number of bases in this span
       Fr = frame
       Ph = phase (kind of like frame, but for cases where preceding
       exon isn't known)
       I/AC = score for intron acceptor site
       Do/T = score for intron donor site
       CodRg = score for coding probability
       P = total probability that this is an exon
       Tsc = Some total score for this exon
    */

    try {
      readLine();
      parsed = line != null;
      if (parsed) {
	line = line.trim();
	if (!isCommentary()) {
	  StringTokenizer tokens = new StringTokenizer (line);
	  gene_id = parseToken (tokens, "gene_id");
	  if (gene_id.indexOf (".") > 0)
	    gene_id = gene_id.substring(0, gene_id.indexOf("."));

	  type = parseToken (tokens, "type");
	  // Genscan uses the abbreviation "intr" for
	  // internal exons
	  if (parsed && (type.equals("Init") ||
			 type.equals("Intr") ||
			 type.equals("Term") ||
			 type.equals("Sngl")))
	    type = "Exon";
	  
	  String strand_str = parseToken (tokens, "strand");
	  parsed &= (strand_str.equals("+") || 
		     strand_str.equals("-") || 
		     strand_str.equals("."));
	  if (parsed)
	    strand = (strand_str.equals("+") || strand_str.equals(".") ?
		      1 : -1);

	  if (parsed)
	    low = parseInteger (tokens, "low");
	  if (parsed)
	    high = parseInteger (tokens, "high");

	  // ignoring the length
	  String temp = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";

	  if ( ! type.equals("PlyA") && ! type.equals("Prom")) {
	    // ignoring the frame
	    temp = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";

	    phase = parseToken(tokens, "phase");
	    parsed &= (phase.equals("0") || 
		       phase.equals("1") || 
		       phase.equals("2") || 
		       phase.equals("."));

	    acceptor_score = parseDouble(tokens, "acceptor_score");

	    donor_score = parseDouble(tokens, "donor_score");
	    
	    coding_potential = parseDouble(tokens, "coding_potential");
    
	    probability = parseDouble(tokens, "probability") * 100;
	  }
	  temp = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
	  parsed &= !temp.equals("");
	  score = parseScore(temp);
	}
	else {
	  comment = line;
	}
      }
    }
    catch (Exception e) {
      System.out.println ("Error parsing line " + line);
      e.printStackTrace();
      parsed = false;
      if (!parsed)
	System.out.println ("Set parsed to false because of line" + line);
    }
    return parsed;
  }

  protected double parseDouble(StringTokenizer tokens, String msg) {
    try {
      return parseScore(parseToken(tokens, msg));
    } catch (Exception ex) {
      parsed = false;
      return -1;
    }
  }

  protected double parseScore (String value)
    throws ParseException {
    //take 'nan' as 0.0001 of prob so predictions will be kept
    double score = 0.0;
    if (value.toLowerCase().equals ("nan") ) {
      value = "0.0001";
    }
    if (value.equals("."))
      value = "0";
    if (parsed) {
      try {
	score = Double.valueOf(value).doubleValue();
      }
      catch (Exception ex) {
	throw new ParseException("Error when parsing double from "+ value,
				 line_number);
      }
    }
    return score;
  }

  protected boolean grabGenes (CurationSet curation,
			       FeatureSetI forward_analysis,
			       FeatureSetI reverse_analysis) {
    FeatureSetI transcript = null;
    try {
      boolean have_genes = false;
      while (parsed && !have_genes) {
	if (comment.indexOf (date_key) >= 0) {
	  grabDate (forward_analysis, reverse_analysis);
	}
	else if (comment.indexOf(matrix_key) >=0) {
	  grabMatrix (forward_analysis, reverse_analysis);
	}
	else if (comment.indexOf (seq_key) >= 0) {
	  SequenceI focal_seq = grabDNA ();
	  initCuration (curation, focal_seq);
	}
	else {
	  have_genes = (comment.startsWith (gene_key));
	}
	parseLine();
      }
      while (parsed && have_genes) {
	if (!gene_id.equals("")) {
	  if (curation.getRefSequence() == null) {
	    // this is bogus, but use a great big number for the length
	    SequenceI focal_seq = initSequence(curation,seq_name,50000000);
	    initCuration (curation, focal_seq);
	  }
	  if (transcript != null && !transcript.getId().equals(gene_id)) {
	    addGene (transcript, forward_analysis, reverse_analysis);
	    transcript = null;
	  }
	  if (transcript == null) {
	    transcript = initSet(gene_id, strand);
	    transcript.setName("genscan"+gene_id);
	  }
	  SeqFeatureI exon = initFeature();
	  exon.setScore (score);
	  exon.setTopLevelType (type);
	  transcript.setScore (transcript.getScore() + exon.getScore());
	  int start, end;
	  exon.setStrand(strand);
	  try {
	    exon.setStart(low);
	    exon.setEnd(high);
	    transcript.addFeature (exon, true);
	    if (acceptor_score > 0)
	      exon.addScore("acceptor_score", acceptor_score);
	    if (donor_score > 0)
	      exon.addScore("donor_score", donor_score);
	    if (coding_potential > 0)
	      exon.addScore("coding_potential", coding_potential);
	    if (probability > 0)
	      exon.addScore("probability", probability);
	  } catch (NumberFormatException ex) {
	    throw new ParseException("Error parsing low/high as number "
				     +low+"/"+high, line_number);
	  }
	  catch( Exception ex ) {
	    System.out.println(ex.getMessage() + "can't parse");
	    System.out.println ("Set parsed to false because of low/high");
	    parsed = false;
	  }
	}
	else {
	  if (comment.startsWith (pep_key) || comment.startsWith(sub_key))
	    have_genes = false;
	  else if (!comment.equals("")) {
	    // System.out.println (comment + " cannot be parsed correctly");
	    parsed = false;
	  }
	}
	parseLine();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println (e.getMessage());
      parsed = false;
    }
    if (transcript != null) {
      addGene (transcript, forward_analysis, reverse_analysis);
    }
    return parsed;
  }
  
  protected void grabSequence (FeatureSetI forward_analysis, 
			     FeatureSetI reverse_analysis) {
    String value = comment.substring (matrix_key.length()).trim();
    forward_analysis.addProperty ("matrix", value);
    reverse_analysis.addProperty ("matrix", value);
  }

  protected void grabMatrix (FeatureSetI forward_analysis, 
			     FeatureSetI reverse_analysis) {
    String value = comment.substring (matrix_key.length()).trim();
    forward_analysis.addProperty ("matrix", value);
    reverse_analysis.addProperty ("matrix", value);
  }

  protected void grabDate (FeatureSetI forward_analysis, 
			   FeatureSetI reverse_analysis) {
    Date date = null;
    String value = null;
    int index = comment.indexOf (date_key);
    if (index >= 0) {
      value = comment.substring (index + date_key.length());
      index = value.indexOf ("\t");
      if (index >= 0) {
	value = value.substring (0, index);
      }
      // if it is an old version of Genscan then any year
      // past 1999 appears as 3 digits. Thus 2000 = 100
      // this is a hack to fix this
      index = value.lastIndexOf ('-');
      if (value.length() - (index + 1) > 2) {
	value = (value.substring (0, index + 1) +
		 value.substring (value.length() - 2));
      }
      forward_analysis.addProperty ("date", value);
      reverse_analysis.addProperty ("date", value);
    }
  }

  protected SequenceI grabDNA () 
    throws ParseException {
    SequenceI seq = null;
    try {
      String value = comment.substring (seq_key.length()).trim();
      int index = value.indexOf (" : ");
      seq_name = (index >= 0) ? value.substring (0, index) : value;
      seq = new Sequence (seq_name, "");
	
      value = value.substring (index + " : ".length());
      index = value.indexOf (" bp : ");
      String seq_length = ((index >= 0) ? 
			   value.substring (0, index) : value);
      seq.setLength (Integer.parseInt(seq_length));
      
      value = value.substring (index + " bp : ".length());
      System.out.println (seq_name + " is " + seq_length + "bp");
      seq.setDescription (value);
    }
    catch( Exception ex ) {
      System.out.println(ex.getMessage());
      ex.printStackTrace();
      parsed = false;
      throw new ParseException("Error when parsing line " + comment,
			       line_number);
    }
    return seq;
  }

}

