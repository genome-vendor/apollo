/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.analysis.filter.AnalysisInput;

public class GenieParser extends GeneFinderParser {

  public GenieParser () {
  }

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad(curation, new_curation, data_stream, input);
    if (parsed) {
      try {
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
    if (line.startsWith("#")) {
      comment = line;
      return true;
    }
    else {
      comment = "";
      return false;
    }
  }

  public boolean recognizedInput() {
    //try skip some warning msg line -- Shu
    int tries = 0;
    try {
      readLine();
      parsed = false;
      while (line != null && !parsed && tries < 10) {
        line = line.trim();
        // ##gff-version 1
        parsed = isCommentary();
        if (parsed) {
          version = parseProgram();
          parsed &= version != null && program != null;
        }
        if (!parsed) {
	  readLine();
	  tries++;
	}
      }
    } catch (Exception e) {}
    return parsed;
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

    try {
      if (line != null) {
	line = line.trim();
	if (!isCommentary()) {
	  StringTokenizer tokens = new StringTokenizer (line);
	  seq_name = parseToken (tokens, "seq_name");
	  source = parseToken (tokens, "source");
	  type = parseToken (tokens, "type");
	  if (parsed)
	    low = parseInteger (tokens, "low");
	  if (parsed)
	    high = parseInteger (tokens, "high");
	  if (parsed) {
            score = parseDouble(tokens, "score_str");
	  }
	  if (parsed) {
	    String strand_str = parseToken (tokens, "strand_str");
	    parsed &= (strand_str.equals("+") || 
		       strand_str.equals("-") || 
		       strand_str.equals("."));
	    strand = (strand_str.equals("+") || 
		      strand_str.equals(".") ?
		      1 : -1);
	    phase = parseToken (tokens, "phase");
	    parsed &= (phase.equals("0") || 
		       phase.equals("1") || 
		       phase.equals("2") || 
		       phase.equals("."));
	    if (!parsed) {
	      phase = "";
	    }
	    else {
	      gene_id = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
	      if (gene_id == null)
		gene_id = "";
	      if (gene_id.startsWith("gene") && tokens.hasMoreTokens())
		gene_id = tokens.nextToken();
	    }
	  }
	}
	// debugLine (line);
	else {
	  comment = line;
	}
      }
    }
    catch (Exception e) {
      System.out.println ("Error parsing line " + line);
      e.printStackTrace();
      parsed = false;
    }
    return parsed;
  }

  protected boolean grabGenes (CurationSet curation,
			       FeatureSetI forward_analysis,
			       FeatureSetI reverse_analysis) {
    FeatureSetI gene = null;
    FeatureSetI transcript = null;
    try {
      while (line != null && parsed) {
	if (comment.startsWith ("##date")) {
	  grabDate (forward_analysis, reverse_analysis);
	}
	else if (comment.startsWith ("##DNA")) {
	  SequenceI focal_seq = grabDNA ();
	  initCuration (curation, focal_seq);
	}
	else if (comment.equals("")) {
	  if (curation.getRefSequence() == null) {
	    // this is bogus, but use a great big number for the length
	    SequenceI focal_seq = initSequence(curation,seq_name,50000000);
	    initCuration (curation, focal_seq);
	  }
	  if (gene != null && !gene.getId().equals(gene_id)) {
	    gene.addFeature(transcript);
	    addGene (gene, forward_analysis, reverse_analysis);
	    gene = null;
	    transcript = null;
	  }
	  if (type.equals ("exon")) {
	    if (gene == null) {
	      gene = initSet(gene_id, strand);
	    }
	    if (transcript == null) {
	      transcript = initSet(trans_id, strand);
	    }
	    SeqFeatureI exon = initFeature();
	    exon.setScore (score);
	    transcript.setScore (transcript.getScore() + exon.getScore());
	    int start, end;
	    exon.setStrand(strand);
	    exon.setLow(low);
	    exon.setHigh(high);
	    transcript.addFeature (exon);
	  }
	}
	else {
	  if (!comment.startsWith ("#") && !type.equals("sequence")) {
	    //	    System.out.println ("Fgenesh " + 
	    //				comment + " cannot be parsed correctly");
	    parsed = false;
	  }
	}
	readLine();
	if (line != null)
	  parseLine();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println (e.getMessage());
      parsed = false;
    }
    if (transcript != null)
      gene.addFeature(transcript);
    if (gene != null)
      addGene (transcript, forward_analysis, reverse_analysis);
    return parsed;
  }

  protected void grabDate (FeatureSetI forward_analysis, 
			   FeatureSetI reverse_analysis) {
    String value = comment.substring ("##date".length()).trim();
    if (value != null && !value.equals("")) {
      forward_analysis.addProperty ("date", value);
      reverse_analysis.addProperty ("date", value);
    }
  }

  protected SequenceI grabDNA () 
    throws ParseException {
    SequenceI seq = null;
    
    if (comment.startsWith("##DNA ")) {
      String value = comment.substring ("##DNA".length()).trim();
      if (value != null && !value.equals("")) {
	seq = new Sequence (value, "");
	StringBuffer dna = new StringBuffer();
	line = "";
	try {
	  readLine();
	  while (line != null && !(line.startsWith("##end-DNA"))) {
	    dna.append (line.substring (2));
	    readLine();
	  }
	  seq.setResidues (dna.toString());
	} 
	catch( Exception ex ) {
	  System.out.println(ex.getMessage());
	  ex.printStackTrace();
	  parsed = false;
	  throw new ParseException("Error when parsing line "+line,
				   line_number);
	}
      }
    }
    if (seq == null) {
      System.out.println ("Could not parse out ##DNA");
    }
    return seq;
  }

  private void debugLine (String line) {
    System.out.println ("Parsed (" + parsed + ") " +
			"from line " + line_number + ": " + line +
			"\n\tcomment=" + comment +
			"\n\tseq_name=" + seq_name +
			"\n\tsource=" + source +
			"\n\ttype=" + type +
			"\n\tlow=" + low +
			"\n\thigh=" + high +
			"\n\tscore=" + score +
			"\n\tstrand=" + strand +
			"\n\tphase=" + phase +
			"\n\tgene_id=" + gene_id);
  }

}

