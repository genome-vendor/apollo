/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.analysis.filter.AnalysisInput;

public class FgeneshParser extends GeneFinderParser {
  private String analysis_date = null;
  private int translation_start = 0;

  public FgeneshParser () {
  }

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad(curation, new_curation, data_stream, input);
    if (parsed) {
      if (analysis_date != null) {
        forward_analysis.addProperty("date", analysis_date);
        reverse_analysis.addProperty("date", analysis_date);
      }
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
    if (line.indexOf("CDS") < 0) {
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
      while (line != null && (!line.equals("")) && !parsed && tries < 10) {
	int index1 = line.indexOf ("FGENESH");
        int index2 = (index1 >= 0 ? line.indexOf (' ', index1) : -1);
	parsed = (index1 >= 0 &&  index2 > 0);
	if (!parsed) {
	  readLine();
	  tries++;
	}
	else {
	  program = line.substring(index1, index2).toLowerCase().trim();
          index1 = index2 + 1;
          if (index1 < line.length()) {
            index2 = line.indexOf(' ', index1);
            if (index2 > 0)
              version = line.substring(index1, index2);
            else
              version = line.substring(index1);
          }
          // grab all the other data of interest
          readLine(); 
          line = line.trim();
          String header = "";
          int length = 50000000;
          while (line != null && (!line.equals(""))) {
            index1 = line.indexOf(':') + 1;
            String val = (index1 > 0 && index1 < line.length() ?
                          line.substring(index1).trim() : null);
            if (line.startsWith("Time")) {
              analysis_date = val;
            }
            else if (line.startsWith("Seq name")) {
              header = val;
            }
            else if (line.startsWith("Length of sequence")) {
              length = Integer.parseInt(val);
            }
            else {
              System.out.println ("Ignoring " + line);
            }
            readLine(); 
            line = line.trim();
          }
	  if (curation.getRefSequence() == null) {
              curation.setRefSequence(initSequence (curation,
						    header,
						    length));
	  }
	}
      }
    } catch (Exception e) {}
    return parsed;
  }

  protected boolean parseLine () {
    low = 0;
    high = 0;
    score = 0;
    strand = 0;
    gene_id = "";

    try {
      if (line != null) {
	line = line.trim();
        int index1 = line.indexOf("CDS");
        String gene_str = line.substring(0, index1);
        StringTokenizer tokens = new StringTokenizer (gene_str);
        gene_id = parseToken (tokens, "gene_id");
        String strand_str = parseToken (tokens, "strand_str");
        parsed &= (strand_str.equals("+") || 
                   strand_str.equals("-") || 
                   strand_str.equals("."));
        strand = (parsed && (strand_str.equals("+") || 
                             strand_str.equals(".")) ?
                  1 : -1);
        
        int index2 = line.indexOf(' ', index1);
        String loc_str = line.substring(index2).trim();
        tokens = new StringTokenizer (loc_str);
        low = parseInteger (tokens, "low");
        high = parseInteger (tokens, "high");
        if (!parsed) {
          parsed = true;
          high = parseInteger (tokens, "high");
        }
        score = parseDouble (tokens, "score_str");
        if (line.indexOf("CDSf") > 0) {
          translation_start = parseInteger (tokens, "TSS");
          if (strand == -1) {
            translation_start = parseInteger (tokens, "TSS");
            if (!parsed) {
              parsed = true;
              translation_start = parseInteger (tokens, "TSS");
            }
          }
          // need to check and see if start/end are arithmetic
          // or biological for fgenesh. just guessing for now
          // that they are arithmetic
          if ((strand == 1 && translation_start == low) ||
              (strand == -1 && translation_start == high)) {
            translation_start = 0;
          }
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
      while (line != null) {
        if (!isCommentary ()) {
          parseLine();
          /*
   1 +    1 CDSf      2377 -      2421    1.13      2377 -      2421     45
   1 +    2 CDSi      2481 -      2696   24.28      2481 -      2696    216
   1 +    3 CDSi      2757 -      2975   21.15      2757 -      2975    219
   1 +    4 CDSi      3038 -      3232   12.50      3038 -      3232    195
   1 +    5 CDSl      3288 -      3578   19.35      3288 -      3578    291
          */
          if (gene != null && !gene.getId().equals(gene_id)) {
            transcript.setScore (transcript.getScore() / transcript.size());
            if (translation_start > 0)
              transcript.setTranslationStart(translation_start, true);
	    gene.addFeature(transcript);
	    addGene (gene, forward_analysis, reverse_analysis);
	    gene = null;
	    transcript = null;
	  }
          /*
	  if (transcript != null && !transcript.getId().equals(trans_id)) {
	    gene.addFeature(transcript);
	    transcript = null;
	  }
          */
          if (gene == null) {
            gene = initSet(gene_id, strand);
          }
          if (transcript == null) {
            // transcript = initSet(trans_id, strand);
            transcript = initSet(gene_id, strand);
            translation_start = 0;
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
	readLine();
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

}

