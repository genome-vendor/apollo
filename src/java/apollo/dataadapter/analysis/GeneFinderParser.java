/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.analysis.filter.AnalysisInput;

public abstract class GeneFinderParser extends AbstractParser {
  String comment;
  String seq_name;
  String source;
  String type;
  int low;
  int high;
  double score;
  int strand;
  String phase;
  String gene_id;
  String trans_id;

  public GeneFinderParser () {
  }

  protected abstract boolean grabGenes (CurationSet curation,
					FeatureSetI forward_analysis,
					FeatureSetI reverse_analysis);
  
  protected abstract boolean isCommentary ();
  
  protected void addGene (FeatureSetI gene, 
			  FeatureSetI forward_analysis,
			  FeatureSetI reverse_analysis) {
    gene.setScore (gene.getScore() / gene.size());
    if (gene.getStrand() == 1)
      forward_analysis.addFeature (gene);
    else
      reverse_analysis.addFeature (gene);
  }

  protected String parseProgram() {
    String version = null;
    if (comment.startsWith("##gff")) {
      program = comment.substring (2, "##gff".length());
      version = comment.substring ("##gff-".length());
    }
    else
      program = (source.equals("src") ? "fgenesh" : source);
    return version;
  }

  public String getProgram () {
    return program;
  }

}

