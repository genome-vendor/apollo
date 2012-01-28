/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.AnalysisInput;
import apollo.util.FastaHeader;

public class RpMaskerParser extends AbstractParser {

  public RpMaskerParser () {
  }

  /*
    score    = Smith-Waterman score of the match, usually complexity adjusted
    The SW scores are not always directly comparable. Sometimes
    the complexity adjustment has been turned off, and a variety of
    scoring-matrices are used dependent on repeat age and GC level.
  */
  double score;
  // % substitutions in matching region compared to the consensusAA
  String perc_sub = "";

  // % of bases opposite a gap in the query sequence (deleted bp)
  String perc_del = "";

  // % of bases opposite a gap in the repeat consensus (inserted bp)
  String perc_ins = "";

  // name of query sequence
  String seq_name = "";

  // starting position of match in query sequence
  int query_1;

  // ending position of match in query sequence
  int query_2;

  // no. bases in query sequence past the ending position of match
  int query_past;

  // match is with the Complement of the repeat consensus sequence
  String complement  = "";

  // name of the matching interspersed repeat
  String repeatname  = "";

  // the sequence accession of the repeat
  String acc         = "";

  // starting position of match in repeat consensus sequence
  int subj_1;

  // ending position of match in repeat consensus sequence
  int subj_2;

  // no. of bases in (complement of) the repeat consensus sequence
  // prior to beginning of the match (0 means that the match 
  // extended all the way to the end of the repeat consensus 
  // sequence)
  int subj_prior;

  // uniqueness for individual insertions
  String subj_unique = "";

  // this is all that there is if there are no repeats found
  SequenceI focal_seq = null;

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad (curation, new_curation, data_stream, input);

    if (parsed) {
      input.runFilter(false);

      if (focal_seq != null && curation.getRefSequence() == null) {
        curation.addSequence (focal_seq);
        initCuration (curation, focal_seq);
      } else {    
        try {
          while (line != null) {
            parseLine(line);
            FeaturePairI repeat = grabRepeat (curation);
            if (repeat != null) {
              if (repeat.isForwardStrand())
                forward_analysis.addFeature(repeat);
              else
                reverse_analysis.addFeature (repeat);
            }
            readLine();
          }
          data.close();
        }
        catch( Exception ex ) {
          parsed = false;
          System.out.println (ex.getMessage());
          ex.printStackTrace();
        }
      }
    }
    return (parsed ? getAnalysisType() : null);
  }

  public String getProgram() {
    return "RepeatMasker";
  }

  public boolean recognizedInput () {
    /* Rpmasker has absolutely nothing in the way of a header,
       (yuck), no version, no preamble, nothing to tell
       you what sort of input file it is. Sooo, just try
       parsing the first line and see if it works or not.
       And (because genscan has the same output format) check
       that it is not a genscan file
    */
    try {
      readLine();
      //try skip some warning msg line -- Shu
      int i = 0;
      while (line != null && i < 10 && !parsed &&
             line.toLowerCase().indexOf ("genscan") < 0) {
        parsed = true;
        // check for no results, which is also valid
        if (line.startsWith("There were no repetitive sequences detected in")){
          program = "repeatmasker";
          int index = line.lastIndexOf('/');
          String seq_name = (index >= 0 ? line.substring(index + 1) : line);
          index = seq_name.lastIndexOf('.');
          if (index > 0)
            seq_name = seq_name.substring(0, index);
          FastaHeader fasta = new FastaHeader(seq_name);
          String seq_id = fasta.getSeqId();
          focal_seq = fasta.generateSequence();
        } else {
          parseLine(line);
          if (!parsed) {
            readLine();
            i++;
          }
          else
            program = "repeatmasker";
        }
      }
    } catch (Exception e) {
      parsed = false;
    }
    return parsed;
  }

  private FeaturePairI grabRepeat (CurationSet curation) {
    FeaturePairI repeat = null;
    if (subj_unique.equals ("")) {
      if (curation.getRefSequence() == null) {
        System.out.println ("Would like genomic sequence fasta file");
	SequenceI target_seq = initSequence(curation, seq_name, 300000);
	initCuration (curation, target_seq);
      }
      repeat = new FeaturePair (new SeqFeature(), new SeqFeature());
      int strand = (complement.equals("+") ? 1 : -1);
      repeat.setStrand (strand);
      repeat.setRefSequence (curation.getRefSequence());
      repeat.setProgramName (program);
      repeat.setDatabase (database);
      repeat.setFeatureType (getAnalysisType());
      repeat.setId (repeatname);
      repeat.setScore (score);

      repeat.setLow (query_1);
      repeat.setHigh (query_2);
	
      int length;
      repeat.setHstrand(1);
      if (complement.equals("+")) {
	repeat.setHlow(subj_1);
	repeat.setHhigh(subj_2);
	length = repeat.length() + subj_prior;
      }
      else {
	// this seems a bit wacky, but i think it is correct now
	repeat.setHlow(subj_prior);
	repeat.setHhigh(subj_2);
	length = repeat.length() + subj_1;
      }
      SequenceI aligned_seq = curation.getSequence(repeatname);
      if (aligned_seq == null)
	aligned_seq = curation.getSequence(acc);
      if (aligned_seq == null) {
	aligned_seq = new Sequence (repeatname, "");
	aligned_seq.setLength (length);
	aligned_seq.setAccessionNo (acc);
	if (acc.startsWith ("FB") || acc.startsWith ("fb")) {
	  aligned_seq.addDbXref ("FB", acc);
	}
	else {
	  aligned_seq.addDbXref ("gb", acc);
	}
	curation.addSequence(aligned_seq);
      }
      repeat.setHitSequence (aligned_seq);
	
      repeat.addProperty ("substitutions", perc_sub);
      repeat.addProperty ("deletions", perc_del);
      repeat.addProperty ("insertions", perc_ins);
    }
    return repeat;
  }

  private boolean parseLine (String line) {
    try {
      line = line.trim();
      StringTokenizer tokens = new StringTokenizer (line);
      
      score = parseDouble(tokens, "score_str");

      perc_sub = parseToken(tokens, "percent_sub");
      
      perc_del = parseToken(tokens, "percent_del");
      
      perc_ins = parseToken(tokens, "percent_ins");
      
      seq_name = parseToken (tokens, "seq_name");
      
      query_1 = parseLocation(tokens);
      query_2 = parseLocation(tokens);
      query_past = parseLocation(tokens);
      
      complement = parseToken(tokens, "complement");
      
      repeatname = parseToken(tokens, "repeat_name");
      
      acc = parseToken(tokens, "accession");
      
      subj_1 = parseLocation(tokens);
      subj_2 = parseLocation(tokens);
      subj_prior = parseLocation(tokens);
      
      // this one is optional
      subj_unique = ((tokens.hasMoreTokens()) ? 
		     tokens.nextToken() : "");
    }
    catch (Exception ex) {
      System.out.println ("Caught exception " + ex);
      parsed = false;
    }
    return parsed;
  }

  private int parseLocation (StringTokenizer tokens) {
    String loc_str = parseToken(tokens, "location");
    int loc = -1;
    if (parsed) {
      int index;
      if ((index = loc_str.indexOf ("(")) >= 0)
        loc_str = loc_str.substring (index + 1);
      if ((index = loc_str.indexOf (")")) > 0)
        loc_str = loc_str.substring (0, index);
      try {
        loc = Integer.parseInt(loc_str);
      }
      catch (NumberFormatException ex) {
        parsed = false;
      }
    }
    return loc;
  }
}

