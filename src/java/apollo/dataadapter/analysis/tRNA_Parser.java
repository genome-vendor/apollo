/*
  Copyright (c) 1997 Berkeley Drosophila Genome Center, UC Berkeley. 
*/

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.AnalysisInput;

public class tRNA_Parser extends AbstractParser 
{
  public tRNA_Parser () {
  }

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad (curation, new_curation, data_stream, input);
    /* 
       Output follows this format
       
       Sequence                tRNA Bounds     tRNA    Anti    Intron Bounds   Cove
       Name            tRNA #  Begin   End     Type    Codon   Begin   End     Score
       --------        ------  -----   ---     ----    -----   -----   -----   -----
       DS01514_1       1       18947   18876   Gln     CTG     0       0       68.47
       
    */
    if (parsed) {
      // don't assume that the input file is
      // output from the tRNAscanSE program
      /* this should be the data */
      input.runFilter(false);
      try {
	while ((line = data.readLine()) != null) {
	  line = line.trim();
	  SeqFeatureI tRNA = grab_tRNA (curation,
					line,
					new_curation);
	  if (tRNA != null) {
	    System.out.println ("Adding tRNA " + tRNA.getName());
	    if (tRNA.isForwardStrand()) 
	      forward_analysis.addFeature (tRNA);
	    else
	      reverse_analysis.addFeature (tRNA);
	  }
	}
	data.close();
      }
      catch(Exception ex) {
	parsed = false;
	System.out.println(ex.getMessage());
	ex.printStackTrace();
      }
    }
    return (parsed ? getAnalysisType() : null);
  }
  
  public String getProgram () {
    return "tRNAscan-SE";
  }

  public boolean recognizedInput () {
    try {
      readLine();
      parsed = (line != null && line.startsWith ("Sequence"));
      /* next two lines are more of the header */
      if (parsed) {
	readLine();
	parsed &= (line != null && line.startsWith ("Name"));
	if (parsed) {
	  readLine();
	  parsed &= (line != null && line.startsWith ("------"));
	}
      }
    }
    catch (Exception e) {
      parsed = false;
    }
    return parsed;
  }

  private SeqFeatureI grab_tRNA (CurationSet curation,
				 String line,
				 boolean new_curation) 
    throws ParseException {
    String value = "";
    StringTokenizer tokens;
    int i;
    boolean valid = true;
    SeqFeatureI tRNA = null;
    int begin = 0;
    int end = 0;
    String aa = "";
    String codon = "";
    double score = 0;
    int id;

    tokens = new StringTokenizer(line);
    try {
      if (valid &= tokens.hasMoreElements()) {
	String seq_name = tokens.nextToken(); // this is the sequence name
	SequenceI focal_seq = curation.getRefSequence();
	if (focal_seq == null) {
	  System.out.println ("Would like genomic sequence fasta file");
	  focal_seq = initSequence(curation, seq_name, 300000);
	  initCuration (curation, focal_seq);
	}
      }
      if (valid &= tokens.hasMoreElements()) {
	try {
	  // this is the beginning of the interval
	  id = Integer.parseInt (tokens.nextToken());
	}
	catch (Exception e) {
	  valid = false;
	}
      }
      
      if (valid &= tokens.hasMoreElements()) {
	try {
	  // this is the beginning of the interval
	  begin = Integer.parseInt (tokens.nextToken());
	}
	catch (Exception e) {
	  valid = false;
	}
      }
      
      if (valid &= tokens.hasMoreElements()) {
	// this is the end of the interval
	try {
	  end = Integer.parseInt (tokens.nextToken());
	}
	catch (Exception e) {
	  valid = false;
	}
      }
      
      if (valid &= tokens.hasMoreElements())
	aa = tokens.nextToken();
      
      if (valid &= tokens.hasMoreElements())
	// this is the anti-codon
	codon = tokens.nextToken();
      
      if (valid &= tokens.hasMoreElements())
	tokens.nextToken(); // intron begins
      
      if (valid &= tokens.hasMoreElements())
	tokens.nextToken(); // bounds end
      
      if (valid &= tokens.hasMoreElements()) {
	try {
	  // this is the score TA-DA!
	  score = (Double.valueOf(tokens.nextToken())).doubleValue();
	}
	catch (Exception e) {
	  valid = false;
	}
      }
      if (valid) {
	tRNA = initFeature();
	int strand = (begin > end ? 1 : -1);
	tRNA.setStrand (strand);
	tRNA.setStart (begin);
	tRNA.setEnd (end);
	tRNA.setName (aa);
	tRNA.setProgramName (program);
	tRNA.addProperty("aminoacid", aa);
	tRNA.addProperty ("anticodon", codon);
	tRNA.setRefSequence(curation.getRefSequence());
	tRNA.setScore (score);
      }
    }
    catch (Exception ex) {//possible set non-number to number
      throw new ParseException(ex.getMessage() + 
			       " occurred in tRNAscan parsing of line ", 
			       line_number);
    }
    /* do this last so that start and end can be used by
       the result */
    if (valid)
      return (tRNA);
    else
      return null;
  }

}

