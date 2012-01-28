/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;

import apollo.datamodel.*;
import apollo.analysis.filter.AnalysisInput;
import apollo.util.CigarUtil;

import org.apache.log4j.*;

public abstract class AlignmentParser extends AbstractParser {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AlignmentParser.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected String coord_seq;
  protected String align_seq;

  public String load (CurationSet curation,
          boolean new_curation,
          InputStream data_stream,
          AnalysisInput input) {
    logger.error (this.getClass().getName() + 
                  " is Abstract and should never be used");
    return null;
  }

  protected void addAlignPair (FeaturePairI span, FeatureSetI hit) {
    hit.addFeature (span, true);
    span.setName (hit.getName() + " pair" + hit.size());
    span.setRefSequence(hit.getRefSequence());
    span.setHitSequence(hit.getHitSequence());
    setHitScore (hit, span);
  }

  protected void rollCigar (FeaturePairI span) {
    // save these the oldfashioned way first
    span.setExplicitAlignment(coord_seq);//addProperty ("alignment", coord_seq);
    span.getHitFeature().setExplicitAlignment(align_seq);
    int untranslate = (program.toLowerCase().endsWith("blastx") ? 3 : 1);
    span.setCigar(CigarUtil.roll(coord_seq, align_seq, untranslate));
    coord_seq = "";
    align_seq = "";
  }

  protected FeatureSetI initAlignment (SequenceI aligned_seq, int strand) {
    String value;

    FeatureSetI hit = new FeatureSet ();
    hit.setStrand (strand);
    hit.setRefSequence (curation.getRefSequence());
    hit.setProgramName (program);
    hit.setDatabase (database);
    hit.setFeatureType (getAnalysisType());
    if (program.equalsIgnoreCase ("blastx") ||
  program.equalsIgnoreCase ("blastp")) {
      aligned_seq.setResidueType(SequenceI.AA);
    }
    else {
      aligned_seq.setResidueType(SequenceI.DNA);
    }
    hit.setHitSequence (aligned_seq);
    hit.setName (aligned_seq.getName());
    return hit;
  }

  protected FeaturePairI initAlignPair () {
    SeqFeatureI query = new SeqFeature();
    SeqFeatureI sbjct = new SeqFeature();
    FeaturePairI span = new FeaturePair (query, sbjct);
    span.setFeatureType (getAnalysisType());
    span.setRefSequence (curation.getRefSequence());
    coord_seq = "";
    align_seq = "";
    return span;
  }

}

