/*
  Copyright (c) 1997
  University of California Berkeley
*/

package apollo.analysis.filter;

import java.io.*;
import java.util.*;

import apollo.datamodel.*;
import apollo.dataadapter.exception.BopException;
import apollo.util.SeqFeatureUtil;
import apollo.util.CigarUtil;

import org.apache.log4j.*;

import org.bdgp.util.DNAUtils;

public class AnalysisFilter implements AnalysisFilterI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(AnalysisFilter.class);

  protected static int CHIMERA_GAP = 50000;

  protected static String default_3prime = "3prime";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  protected AnalysisInput filter_input;

  protected String suffix_3prime = null;

  FeatureSetI forward_analysis = null;
  FeatureSetI reverse_analysis = null;

  public void cleanUp (CurationSet curation,
                       String analysis_type,
                       AnalysisInput filter_input) {
    this.filter_input = filter_input;

    if (!filter_input.runFilter())
      return;

    SequenceI seq = curation.getRefSequence();
    String seq_name = (seq != null) ? seq.getName() : null;

    String type = filter_input.getAnalysisType();
    forward_analysis = getAnalysis (curation, analysis_type, 1, type);
    reverse_analysis = getAnalysis (curation, analysis_type, -1, type);

    debugAnalysis(forward_analysis);
    debugAnalysis(reverse_analysis);

    // create separate hits for each frame of the subject
    if (filter_input.splitFrames()) {
      logger.info ("Splitting hits into separate frames");
      splitHitsByFrame (forward_analysis);
      splitHitsByFrame (reverse_analysis);
    }

    // may delete spans from the hit,
    // but not the hit itself, unless no spans remain
    // THIS MUST PRECEDE SPLITTING TANDEM DUPLICATIONS!!
    // AND MUST FOLLOW SPLITTING BY FRAME
    if (filter_input.useCoincidence()) {
      int percent_overlap = filter_input.getCoincidence();
      logger.info ("Removing HSPs that overlap by " + percent_overlap + "%");
      cleanUpSpanCoincidents (forward_analysis, percent_overlap);
      cleanUpSpanCoincidents (reverse_analysis, percent_overlap);
    }

    if (filter_input.useAutonomousHSPs()) {
      logger.info ("Making each HSP an individual hit");
      promoteHSPsToHits(forward_analysis);
      promoteHSPsToHits(reverse_analysis);
    }
      
    // create separate hits for tandem duplications
    if (filter_input.splitTandems()) {
      logger.info ("Splitting tandem hits");
      splitTandemHits (forward_analysis, filter_input);
      splitTandemHits (reverse_analysis, filter_input);
    }
      
    // remove hits based on low score, low identity, or
    // high expectation of random hit like this
    if (forward_analysis != null)
      processHits (forward_analysis, filter_input);
    if (reverse_analysis != null)
      processHits (reverse_analysis, filter_input);
      
    if (filter_input.trimPolyA()) {
      logger.info ("Removing polyA tails");
      if (forward_analysis != null)
        removeTails(forward_analysis);
      if (reverse_analysis != null)
        removeTails(reverse_analysis);
    }
      
    if (filter_input.revComp3Prime()) {
      suffix_3prime = filter_input.get3PrimeSuffix();
      logger.info ("Reverse complementing 3' EST reads (suffix=" + 
                          suffix_3prime + ")");
      Vector completed = new Vector();
      if (forward_analysis != null)
        reverseESTs(forward_analysis, completed);
      if (reverse_analysis != null)
        reverseESTs(reverse_analysis, completed);
    }
      
    if (filter_input.joinESTends()) {
      logger.info ("Join EST reads from same cDNA clones");
      joinESTs(forward_analysis, reverse_analysis);
    }
      
    if (filter_input.removeLowContent()) {
      int wordsize = filter_input.getWordSize();
      int max_ratio = filter_input.getMaxRatio();
      logger.info ("Removing hits with compression < " + max_ratio +
                          "%" + " with wordsize=" + wordsize);
      removeLowContent(forward_analysis, wordsize, max_ratio);
      removeLowContent(reverse_analysis, wordsize, max_ratio);
    }
      
    if (filter_input.removeTwilights()) {
      logger.info ("Removing twilight spans");
      cleanupTwilights(forward_analysis, filter_input);
      cleanupTwilights(reverse_analysis, filter_input);
    }
      
    // These next 2 are (right now) just for Genscan
    // remove predictions of polyadenylation sites
    if (!filter_input.keepPolyA()) {
      logger.info ("Removing polyA predictions");
      cleanupByType(forward_analysis, "PlyA");
      cleanupByType(reverse_analysis, "PlyA");
    }
      
    // remove predictions of polyadenylation sites
    if (!filter_input.keepPolyA()) {
      logger.info ("Removing promoter predictions");
      cleanupByType(forward_analysis, "Prom");
      cleanupByType(reverse_analysis, "Prom");
    }
      
    // run this check before total length is checked
    if (filter_input.limitAlignGap()) {
      int max_missing = filter_input.getMaxAlignGap();
      logger.info ("Removing hits with internal gaps > " + 
                          max_missing + "% of total length");
      removeGappedAlign (forward_analysis, max_missing);
      removeGappedAlign (reverse_analysis, max_missing);
    }
      
    if (filter_input.useLength()) {
      int min_length = filter_input.getMinLength();
      boolean is_percentage = filter_input.usePercentage();
      logger.info ("Removing hits with length < " + min_length +
                          (is_percentage ? "%" : ""));
      removeShorties(forward_analysis, min_length, is_percentage);
      removeShorties(reverse_analysis, min_length, is_percentage);
    }
      
    if (filter_input.limitMaxExons()) {
      int max_exons = filter_input.getMaxExons();
      logger.info ("Removing hits with > " + max_exons +
                          " HSPs");
      removeFragmented (forward_analysis, max_exons);
      removeFragmented (reverse_analysis, max_exons);
    }

    Vector regions = Coverage.sortRegions (forward_analysis,
                                           reverse_analysis);
      
    if (filter_input.removeShadows()) {
      logger.info ("Removing shadows");
      cleanUpShadows (regions, forward_analysis, reverse_analysis);
    }
      
    if (filter_input.limitCoverage()) {
      Coverage.cleanUp (regions, forward_analysis, reverse_analysis,
                        filter_input.getMaxCover(), this);
    }

    //    if (filter_input.queryNotGenomic()) {
    //      removeNonScaffoldHits(curation, forward_analysis);
    //      removeNonScaffoldHits(curation, reverse_analysis);
    //    }
  }

  private void processHits (FeatureSetI analysis,
                            AnalysisInput filter_input) {
    if (analysis == null) {
      logger.warn("AnalysisFilter.processHits: analysis is null");
      return;
    }

    boolean use_expect = filter_input.useExpect();
    double max_expect = filter_input.getMaxExpect();
    boolean use_score = filter_input.useScore();
    int min_score = filter_input.getMinScore();
    boolean use_identity = filter_input.useIdentity();
    int min_identity = filter_input.getMinIdentity();

    if (use_expect)
      logger.info ("Removing hits with expect > " + max_expect);
    if (use_score)
      logger.info ("Removing hits with score < " + min_score);
    if (use_identity)
      logger.info ("Removing hits with identity < " + min_identity);

    int hit_index = 0;
    SeqFeatureI sf = analysis.getFeatureAt (hit_index);
    while (hit_index < analysis.size ()) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (hit_index);
      FeatureSetI next_hit 
        = ((hit_index + 1 < analysis.size()) ?
           (FeatureSetI) analysis.getFeatureAt (hit_index + 1) : null);
      if (use_expect && hit.getScore("expect") > max_expect) {
        /** A maximum expectation value may also be applied
         * as a simple threshhold value. Spans with an expect
         * greater than this value are discarded. <p>
         * This value is set in the preferences file as
         * -max_expect 1.0e-4
         */
        analysis.deleteFeature(hit);
        debugFeature (hit, "Removed because expectation too high ");
      }
      else if (use_score && hit.getScore() < min_score) {
        analysis.deleteFeature(hit);
        debugFeature (hit, "Removed because score " + hit.getScore() +
                      " < " + min_score);
      }
      else if (use_identity && lowIdentity(hit, min_identity)) {
        analysis.deleteFeature(hit);
      }
      if (next_hit != null)
        hit_index = analysis.getFeatureIndex(next_hit);
      else
        hit_index = analysis.size();
    }
    return;
  }

  /** Experimental--not currently being used. */
//   private void removeNonScaffoldHits(CurationSet curation, FeatureSetI analysis) {
//     String scaffold = curation.getName();
//     if (scaffold == null || !scaffold.startsWith("AE")) {
//       logger.error("removeNonScaffoldHits: current region " + scaffold + " is not a recognizable scaffold");
//       return;
//     }

//     logger.info("Removing not-scaffold hits..."); // DEL

//     int hit_index = 0;
//     SeqFeatureI sf = analysis.getFeatureAt (hit_index);
//     while (hit_index < analysis.size ()) {
//       FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (hit_index);
//       FeatureSetI next_hit 
//         = ((hit_index + 1 < analysis.size()) ?
//            (FeatureSetI) analysis.getFeatureAt (hit_index + 1) : null);
//       if (hit.getName().startsWith(scaffold)) {
//         analysis.deleteFeature(hit);
//         debugFeature (hit, "Removed because hit is not on current scaffold ");
//       }
//     }
//   }

  protected void cleanupByType (FeatureSetI fs,
                                String type) {
    if (fs == null)
      return;
    for (int i = fs.size() - 1; i >= 0; i--) {
      SeqFeatureI sf = fs.getFeatureAt (i);
      if (sf.getTopLevelType().equals (type)) {
        fs.deleteFeature (sf);
        debugFeature (sf, "Removed because type was " + type);
      }
      else if (sf instanceof FeatureSetI)
        cleanupByType ((FeatureSetI) sf, type);
    }
  }

  protected void removeShorties (FeatureSetI analysis,
                                 int min_length,
                                 boolean is_percentage) {
    if (analysis == null)
      return;
    for (int i = analysis.size() - 1; i >= 0; i--) {
      FeatureSetI hit = (FeatureSetI)analysis.getFeatureAt (i);
      if (tooShort(hit, min_length, is_percentage)) {
        analysis.deleteFeature(hit);
      }
    }
  }

  private void debugAnalysis (FeatureSetI analysis) {
    if (analysis == null)
      return;
    for (int i = analysis.size() - 1; i >= 0; i--) {
      FeatureSetI hit = (FeatureSetI)analysis.getFeatureAt (i);
      debugFeature(hit, "Feature is now ");
    }
  }

  protected boolean tooShort (FeatureSetI hit,
                              int min_length,
                              boolean is_percentage) {
    if (hit == null)
      return false;
    int span_cnt = hit.size();
    double hit_length = 0;
    for (int i = span_cnt - 1; i >= 0; i--) {
      FeaturePairI span = (FeaturePairI) hit.getFeatureAt(i);
      hit_length += span.getHitFeature().length();
    }
    if (is_percentage) {
      SequenceI hit_seq = hit.getHitSequence();
      hit_length = (hit_length * 100) / hit_seq.getLength();
    }
    if (hit_length < min_length)
      debugFeature (hit, "This hit's length " + hit_length + " < " +
                    min_length);
    return hit_length < min_length;
  }

  protected void removeGappedAlign (FeatureSetI analysis,
                                    int max_missing) {
    if (analysis == null)
      return;

    for (int i = analysis.size() - 1; i >= 0; i--) {
      FeatureSetI hit = (FeatureSetI)analysis.getFeatureAt (i);
      int match_length = hit.getHitSequence().getLength();
      int max_gap = (int) (max_missing * match_length) / 100;
      gappedAlignment(hit, max_gap, 0, 1);
      gappedAlignment(hit, max_gap, hit.size() - 1, hit.size() - 2);
      if (hit.size() == 0) {
        analysis.deleteFeature(hit);
        debugFeature (hit, "This hit has no spans left, where did they go?");
      }
    }
  }
  
  private void gappedAlignment (FeatureSetI hit, int max_gap,
                                int index1, int index2) {
    if (hit == null)
      return;

    if (hit.size() > 1) {
      // only checking the ends for now
      FeaturePairI span1 = (FeaturePairI) hit.getFeatureAt(index1);
      FeaturePairI span2 = (FeaturePairI) hit.getFeatureAt(index2);
      int gap_length = gapBetween (span1.getHitFeature(), 
                                   span2.getHitFeature());
      if (gap_length >= max_gap) {
        String msg = (hit.getHitSequence().getName() +
                      "\n\tDistance between span1 " + 
                      span1.getHitFeature().getLow() + "-" +
                      span1.getHitFeature().getHigh() + 
                      " and span2 " +
                      span2.getHitFeature().getLow() + "-" +
                      span2.getHitFeature().getHigh() + "\n\tis " + 
                      gap_length + " bases, exceeding max " + max_gap);
        debugFeature (hit, msg);
        hit.deleteFeature (span1);
      }
    }
  }

  protected void removeFragmented (FeatureSetI analysis,
                                   int max_exons) {
    if (analysis == null)
      return;

    for (int i = analysis.size() - 1; i >= 0; i--) {
      FeatureSetI hit = (FeatureSetI)analysis.getFeatureAt (i);
      if (hit.size() > max_exons) {
        analysis.deleteFeature(hit);
        debugFeature (hit, "This hit had " + hit.size() + 
                      " HSPs, which is > max of " + max_exons);
      }
    }
  }

  protected void removeLowContent (FeatureSetI analysis,
                                   int wordsize,
                                   int max_ratio) {
    if (analysis == null)
      return;

    for (int i = 0; i < analysis.size();) {
      FeatureSetI hit = (FeatureSetI)analysis.getFeatureAt (i);
      if (lacksContent (hit, wordsize, max_ratio)) {
        analysis.deleteFeature(hit);
        debugFeature (hit, "Deleted low content hit at ");
      }
      else
        i++;
    }
  }

  /**
   * If this is a nucleic acid sequence (actually this would
   * work for a amino acid sequence as well) BOP uses Huffman
   * encoding to look for low information content. If the string
   * compresses too easily (too easily being a threshhold set
   * in the preferences file). The word size is also set in the
   * preferences file. A setting of -Analysis_repeat2 15 indicates
   * a word size of two (dinucleotide repeat) and compression cutoff of
   * less than 15 bits of information
   */
  private boolean lacksContent (FeatureSetI hit,
                                int wordsize,
                                int max_ratio) {

    boolean no_info = true;
    boolean debug 
      = filter_input.debugFilter(hit.getHitSequence().getName());
    for (int i = 0; no_info && i < hit.size (); i++) {
      SeqFeatureI span = hit.getFeatureAt(i);
      // this shouldn't ever happen once its debugged
      // this happens if sim4 and blast spans get confused and
      // have been put into the same hit
      //if (span.getProperty("alignment").length() == 0) {
      if (span.getExplicitAlignment().length() == 0) {
        logger.error("no sequence in span " + (i+1));
        no_info = false;
      }
      else {
        /* even if an individual span may lack content the hit
           is not removed. It is all or nothing either the entire
           hit is removed or it is kept with all spans, including
           those with low information content
        */
        //String alignment = span.getProperty("alignment");
        //String alignment = span.getExplicitQueryAlignment(); // query seems wrong
        String alignment = span.getExplicitAlignment(); 
        double shrinkage = Compress.compress (alignment, wordsize, debug);
        no_info &= shrinkage <= max_ratio;
      }
    }
    return no_info;
  }

  private void splitHitsByFrame (FeatureSetI analysis) {
    if (analysis.getFeatures().size() == 0) {
      return;
    }
    FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (0);
    FeaturePairI span = (FeaturePairI) hit.getFeatureAt (0);
    SeqFeatureI matching_span = span.getHitFeature();
    String frame = matching_span.getProperty("frame");
    Hashtable frames = new Hashtable(6);
    if (frame != null && !frame.equals ("")) {
      int hit_cnt = analysis.size();
      // accumulated all of the new hits in this vector
      Vector new_frames = new Vector();
      FeatureSetI current_hit;
      for (int i = 0; i < hit_cnt; i++) {
        frames.clear();
        hit = (FeatureSetI) analysis.getFeatureAt (i);
        span = (FeaturePairI) hit.getFeatureAt (0);
        matching_span = span.getHitFeature();
        frame = matching_span.getProperty("frame");
        frames.put (frame, hit);
        for (int j = 0; j < hit.size();) {
          span = (FeaturePairI) hit.getFeatureAt (j);
          matching_span = span.getHitFeature();
          frame = matching_span.getProperty("frame");
          current_hit = (FeatureSetI) frames.get (frame);
          if (current_hit == null) {
            // this is not the same frame as any previous
            hit.deleteFeature (span);
            current_hit = makeNewHit (hit, span);
            frames.put (frame, current_hit);
          }
          else if (current_hit != hit) {
            hit.deleteFeature (span);
            addToHit (span, current_hit);
          }
          else
            j++;
        }
        Enumeration e = frames.elements();
        while (e.hasMoreElements()) {
          current_hit = (FeatureSetI) e.nextElement();
          if (current_hit != hit) {
            new_frames.addElement (current_hit);
          }
        }
      }
      for (int i = 0; i < new_frames.size(); i++) {
        current_hit = (FeatureSetI) new_frames.elementAt(i);
        analysis.addFeature (current_hit, true);
        debugFeature (current_hit, "Added new frame ");
      }
    }
  }

  private void removeTails (FeatureSetI analysis) {
    int polyA_length = 5;
    int hit_index = 0;
    while (hit_index < analysis.size ()) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (hit_index);
      /** 
       * This is very similar to the low complexity check
       * BOP uses Huffman encoding to look 
       * for low information content. If the string
       * compresses too easily (too easily being a threshhold set
       * in the preferences file). The word size is always one.
       * This only applies to the last span and there must be > 1 span.
       */
      FeaturePairI span;
      if (hit.size() > 1 && !hit.isFlagSet(FeatureSet.POLYA_REMOVED)) {
        // check both ends to be sure
        int index = hit.size() - 1;
        span = (FeaturePairI) hit.getFeatureAt(index);
        String name = span.getHitFeature().getRefSequence().getName();
        String dna = span.getResidues();
        int intron_length = Math.abs(span.getStart() -
                                     hit.getFeatureAt(index - 1).getEnd());
        if (Compress.isPolyATail (dna,
                                  intron_length,
                                  (name+"-span:"+(index+1)), 
                                  filter_input.debugFilter(name))) {
          hit.deleteFeature(span);
          debugFeature (span, "deleted polyA tail from end");
        } else {
          span = (FeaturePairI) hit.getFeatureAt(0);
          dna = span.getResidues();
          intron_length = (hit.size() > 1 ?
                           Math.abs(hit.getFeatureAt(1).getStart() -
                                    span.getEnd()) :
                           0);
          name = span.getHitFeature().getRefSequence().getName();
          if (Compress.isPolyATail (dna,
                                    intron_length,
                                    (name+"-span:1"),
                                    filter_input.debugFilter(name))) {
            hit.deleteFeature(span);
            debugFeature (span, "deleted polyA tail from beginning");
          }
        }
      }
      hit_index++;
    }
  }

  protected void splitTandemHits (FeatureSetI analysis, 
                                  AnalysisInput filter_input) {
    int hit_index = 0;
    boolean do_the_splits = true;
    while (hit_index < analysis.size ()) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (hit_index);
      do_the_splits = splitTandemHit(analysis, hit, filter_input);
      hit_index = (do_the_splits ? 0 : hit_index + 1);
    }
  }

  private boolean splitTandemHit (FeatureSetI analysis, FeatureSetI hit,
                                  AnalysisInput filter_input) {
    
    /* separate all hits that are not in serial order */
    /* make sure all the hits are in sequential order on subject strand
       hits have already been ordered according to genomic strand
       when they were inserted. */
    FeatureSetI current_hit = null;
    FeaturePairI prev_span = (FeaturePairI) hit.getFeatureAt(0);
    int span_index = 1;
    Vector added_hits = new Vector();

    while (span_index < hit.size()) {
      // The spans are known to be ordered sequentially along
      // the genome (i hope so any way), and if they aren't
      // sequential in the same way across the subject sequence
      // then it will be split into separate hits.
      FeaturePairI next_span = (FeaturePairI) hit.getFeatureAt(span_index);
      if (!isSubjSequential (prev_span, next_span)) {
  hit.deleteFeature (next_span);
  current_hit = makeNewHit (hit, next_span);
  added_hits.addElement (current_hit);
      }
      else {
  if (current_hit != null) {
    hit.deleteFeature (next_span);
    addToHit(next_span, current_hit);
  }
  else
    span_index++;
      }
      prev_span = next_span;
    }

    boolean changed = added_hits.size() > 0;
    if (changed) {
      double max_score = getHitScore(hit);
      FeatureSetI top_hit = hit;
      for (int i = 0; i < added_hits.size(); i++) {
  current_hit = (FeatureSetI) added_hits.elementAt(i);
        debugFeature (current_hit, "Split proposed new hit #" + (i+1));
  if (max_score < current_hit.getScore()) {
    max_score = current_hit.getScore();
          top_hit = current_hit;
          debugFeature (top_hit, "This is the current top score");
        }
      }
      double min_score = Math.min(max_score * 0.25, 100);
      String msg = ("Min score = " + min_score);
      if (hit.getScore() < min_score) {
        debugFeature(hit, "Deleted initial hit after split, because score " +
                     hit.getScore() + " too low " + msg);
        debugFeature(top_hit, "Now starting over after split, with score " +
                     top_hit.getScore());
        added_hits.removeElement(top_hit);
        analysis.deleteFeature (hit);
  analysis.addFeature(top_hit, true);
        hit = top_hit;
      }
      boolean start_over = false;
      for (int i = added_hits.size() - 1; i >= 0; i--) {
  current_hit = (FeatureSetI) added_hits.elementAt(i);
  if (current_hit.getScore() < min_score) {
          added_hits.removeElement(current_hit);
          start_over = true;
  }
      }
      debugFeature (hit,
                    " found " + added_hits.size() +
                    " starting over = " + start_over);
      cleanUpTwilight(hit, filter_input);
      for (int i = 0; i < added_hits.size(); i++) {
        current_hit = (FeatureSetI) added_hits.elementAt(i);
        if (start_over) {
          cleanUpTwilight(current_hit, filter_input);
          if (current_hit == hit) {
            debugFeature(hit, "Why is this in added hits list?", true);
          }
          else {
            while (current_hit.size() > 0) {
              FeaturePairI span = (FeaturePairI) current_hit.getFeatureAt(0);
              current_hit.deleteFeature(span);
              hit.addFeature(span, true);
            }
          }
        } else {
          debugFeature(current_hit, "Added after split");
          analysis.addFeature(current_hit, true);
        }
      }
      if (start_over)
        debugFeature(hit, "Remerged splits after culling low scores");
    }
    return changed;
  }

  private void promoteHSPsToHits (FeatureSetI analysis) {
    Vector new_hits = new Vector();
    int hit_cnt = analysis.size();
    for (int i = 0; i < hit_cnt; i++) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (i);
      // any HSP beyond one needs to get its own hit
      while (hit.size() > 1) {
  FeaturePairI span = (FeaturePairI) hit.getFeatureAt(1);
  hit.deleteFeature (span);
  FeatureSetI new_hit = makeNewHit (hit, span);
  new_hits.addElement (new_hit);
      }
    }
    for (int i = 0; i < new_hits.size(); i++) {
      FeatureSetI new_hit = (FeatureSetI) new_hits.elementAt (i);
      // may delete spans from the hit,
      // but not the hit itself
      analysis.addFeature(new_hit, true);
    }
  }

  private void removeWeakerSpan (FeatureSetI hit,
         SeqFeatureI span1, SeqFeatureI span2) {
    SeqFeatureI weaker;

    /* Given a choice between two spans choose the one that appears
       to be a better alignment */

    if (SecondIsWeaker (span1, span2)) {
      weaker = span2;
    }
    else {
      if (SecondIsWeaker (span2, span1)) {
  weaker = span1;
      }
      else {
  weaker = span2;
      }
    }
    hit.deleteFeature(weaker);
    SeqFeatureI stronger = (weaker == span1 ? span2 : span1);
    debugFeature (weaker, "Removing overlapping span ");
    debugFeature (stronger, "Kept this span");
  }

  public String debugName (SeqFeatureI sf) {
    String name = null;
    if (sf instanceof FeaturePairI)
      name = ((FeaturePairI) sf).getHitSequence().getName();
    else if (sf instanceof FeatureSetI)
      name = ((FeatureSetI) sf).getHitSequence().getName();
    else
      name = sf.getRefSequence().getName();
    if (name == null) {
      logger.warn ("Something seriously wrong with feature " +
        sf.toString());
    }
    return name;
  }

  public void debugFeature (SeqFeatureI sf, SeqFeatureI sf2, 
          String prefix, boolean force) {
    String name = debugName (sf);
    if (filter_input.debugFilter(name) || force) {
      debugFeature (sf, prefix, force);
      debugFeature (sf2, prefix, true);
    }
  }
    
  public void debugFeature (SeqFeatureI sf, String prefix) {
    debugFeature (sf, prefix, false);
  }

  public void debugFeature (SeqFeatureI sf, String prefix, boolean force) {
    String name = debugName (sf);
    // TODO - this should be handled by the Log4J configuration
    if (filter_input.debugFilter(name) || force) {
      logger.info (name + ":  " + prefix + "\n\t" +
                   " strand=" + sf.getStrand() +
                   " start=" + sf.getStart() + 
                   " end=" + sf.getEnd() +
                   "\n\tlength=" + sf.length() +
                   " expect=" + sf.getScore("expect") +
                   " bits=" + sf.getScore("bits") +
                   " score=" + sf.getScore() +
                   " type=" + sf.getFeatureType());
      if (sf instanceof FeatureSetI) {
        FeatureSetI fs = (FeatureSetI) sf;
        for (int i = 0; i < fs.size(); i++) {
          FeaturePairI fp = (FeaturePairI) fs.getFeatureAt(i);
          logger.info ("\tSpan " + (i+1) + 
                       " genomic range=" + fp.getStart() + 
                       "-" + fp.getEnd() +
                       "\tmatch range=" + fp.getHstart() + 
                       "-" + fp.getHend());
        }
      }
    }
  }

  private boolean isSubjSequential (FeaturePairI prev_span,
            FeaturePairI next_span) {
    boolean is_sequential = true;
    SeqFeatureI prev_hit_span = prev_span.getHitFeature();
    SeqFeatureI next_hit_span = next_span.getHitFeature();
    /* The spans are returned in the order that they are
       found on the sequence from the database
       i.e. the "query" sequence.
       Depending upon whether we are examining the plus
       strand of the query sequence or the minus strand
       we would expect the start positions to be ascending
       or descending respectively
       NOTA BENE:
       The subject strand also has plus and minus alignments
       to the query, but all subject plus matches are treated as
       a single 'hit' and all subject minus matches as an independent
       hit, so they are not dealt with here (see "shadows" for
       how these query-subject sequence matches are filtered)
    */
    /* the first test is simply whether the start of the next
       span comes after the start of the previous span */
    is_sequential = (prev_hit_span.getStrand() == 1 ?
         next_hit_span.getStart() > prev_hit_span.getStart() :
         next_hit_span.getStart() < prev_hit_span.getStart());

    if (is_sequential && prev_hit_span.overlaps(next_hit_span)) {
      int spacing = (prev_hit_span.getStrand() == 1 ?
         next_hit_span.getStart() - prev_hit_span.getEnd() :
         prev_hit_span.getEnd() - next_hit_span.getStart());
      if (spacing < 0) {
  /* The spans are not sequential, but first check to make
     sure that the amount of overlap is not trivial and
     can be ignored
  */
  int overlap = -spacing;
  int genomic_overlap = getOverlap (prev_span, next_span);
  if (genomic_overlap > 0 &&
      prev_hit_span.getRefSequence().getResidueType() == SequenceI.AA)
    genomic_overlap = genomic_overlap / 3;
  if (genomic_overlap > 0) {
    double ratio = overlap / genomic_overlap;
    is_sequential = (ratio > 0.8 && ratio <= 6.0);
    if (!is_sequential) {
      String msg = ("overlap = " + overlap +
                    " genomic_overlap = " + genomic_overlap +
                    " ratio = " + ratio + " ");
      debugFeature (prev_hit_span, msg);
    }
  }
  else {
    int hit_extent = (Math.max (prev_hit_span.getHigh(), 
                                next_hit_span.getHigh()) -
                      Math.min (prev_hit_span.getLow(),
                                next_hit_span.getLow()));
    double percent_overlap =  (Math.abs(spacing) * 100) / hit_extent;
    is_sequential = (percent_overlap <= 10.0);
    if (!is_sequential)
      debugFeature(prev_hit_span,
                   "Spacing between this and next span is " +
                   spacing + " genomic overlap is " + genomic_overlap +
                   " hit extent " + hit_extent +
                   " overlap " + percent_overlap + "%");
  }
      }
    }
    return is_sequential;
  }

  private FeatureSetI makeNewHit (FeatureSetI old_hit, FeaturePairI span) {
    FeatureSetI new_hit = new FeatureSet ();
    new_hit.setStrand (old_hit.getStrand());
    new_hit.setName (old_hit.getName());
    new_hit.setRefSequence (old_hit.getRefSequence());
    new_hit.setFeatureType (old_hit.getFeatureType());
    new_hit.setHitSequence (old_hit.getHitSequence());
    new_hit.setProgramName(old_hit.getProgramName());
    new_hit.setDatabase (old_hit.getDatabase());
    addToHit (span, new_hit);
    return new_hit;
  }

  private double getHitScore(FeatureSetI hit) {
    double max_score = -1;
    double hit_expect = 1;
    double hit_prob = 1;
    for (int i = 0; i < hit.size (); i++) {
      SeqFeatureI hsp = (SeqFeatureI) hit.getFeatureAt(i);
      double hsp_expect = hsp.getScore ("expect");
      if (hsp_expect < hit_expect) {
        hit_expect = hsp_expect;
        hit_prob = hsp.getScore("probability");
      }
      if (hsp.getScore() > max_score)
        max_score = hsp.getScore();
    }
    hit.setScore (max_score);
    hit.addScore ("expect", hit_expect);
    hit.addScore ("probability", hit_prob);
    return max_score;
  }

  private void addToHit (SeqFeatureI span, FeatureSetI hit) {
    hit.addFeature (span, true);
    double hit_expect = hit.getScore ("expect");
    int score;
    for (int i = 0; i < hit.size (); i++) {
      SeqFeatureI hsp = (SeqFeatureI) hit.getFeatureAt(i);
      if (hit_expect < 0 || hsp.getScore("expect") < hit_expect) {
  hit.setScore (hsp.getScore());
  hit.addScore ("expect", hsp.getScore("expect"));
  hit.addScore ("probability", hsp.getScore("probability"));
      }
    }
  }

  private boolean SecondIsWeaker (SeqFeatureI span1, SeqFeatureI span2) {
    boolean span2weaker = false;
    double identity1, identity2;
    double expect1 = span1.getScore("expect");
    double expect2 = span2.getScore("expect");
    double score1 = span1.getScore();
    double score2 = span2.getScore();

    // first measure is the expectation value
    if (expect1 < expect2) {
      span2weaker = true;
    }
    else {
      // if the expectation is the same perhaps the score is different
      if (expect1 == expect2
    && score1 > score2) {
  span2weaker = true;
      }
      else {
  identity1 = identity(span1);
  identity2 = identity(span2);
  if (score1 == score2
      && expect1 == expect2
      && identity1 > identity2) {
    span2weaker = true;
  }
  else {
    if (score1 == score2
        && expect1 == expect2
        && identity1 == identity2
        && span1.length() > span2.length()) {
      span2weaker = true;
    }
  }
      }
    }
    return span2weaker;
  }

  public double identity (SeqFeatureI span) {
    return span.getScore("identity");
  }

  protected boolean lowIdentity (FeatureSetI hit, int min_identity) {
    int span_cnt = hit.size();
    for (int i = span_cnt - 1; i >= 0; i--) {
      FeaturePairI span = (FeaturePairI) hit.getFeatureAt(i);
      if (identity(span) < min_identity) {
  hit.deleteFeature(span);
  debugFeature (span, "Removed because " + identity(span) +
          "% < " + min_identity + "% identity");
      }
    }
    return hit.size() <= 0;
  }

  private void cleanUpSpanCoincidents (FeatureSetI analysis, int coincidence) {
    /* handle spans that overlap to a significant degree */
    int hit_cnt = analysis.size();
    for (int i = 0; i < hit_cnt; i++) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (i);
      SeqFeatureI span1, span2;
      boolean cleanup = true;
      debugFeature (hit, "Checking for overlapping spans ");
      while (cleanup) {
        cleanup = false;
        int span_index = 0;
        while ((span_index + 1) < hit.size() && !cleanup) {
          span1 = (FeaturePairI) hit.getFeatureAt (span_index);
          span2 = (FeaturePairI) hit.getFeatureAt (span_index + 1);
          cleanup = spansOverlap (span1, span2, coincidence);
          if (cleanup) {
            removeWeakerSpan (hit, span1, span2);
          }
          else {
            span_index++;
          }
        }
      }
    }
  }

  private void cleanupTwilights (FeatureSetI analysis,
         AnalysisInput filter_input) {
    for (int i = 0; i < analysis.size(); i++) {
      FeatureSetI hit = (FeatureSetI) analysis.getFeatureAt (i);
      // may delete spans from the hit,
      // but not the hit itself
      cleanUpTwilight (hit, filter_input);
    }
  }

  private void cleanUpTwilight (FeatureSetI hit, AnalysisInput filter_input) {
    // may delete spans from the hit,
    // but not the hit itself
    TwilightFilter twilight = new TwilightFilter();
    if (hit.getRefSequence().getResidueType() == SequenceI.DNA)
      twilight.setSeqNucleic (true);
    else
      twilight.setSeqNucleic (false);

    if (hit.getHitSequence().getResidueType() == SequenceI.DNA)
      twilight.setRefNucleic (true);
    else
      twilight.setRefNucleic (false);

    twilight.setCertainlyGood(filter_input.getTwilightUpper());
    twilight.setCertainlyBad(filter_input.getTwilightLower());

    boolean testing 
      = filter_input.debugFilter(hit.getHitSequence().getName());
    try {
      /* handle spans that are in the twilight zone. Not high enough
         to be definite keepers and now low enough to simply throw out.
      */
      boolean consider_it = true;
      while (hit.size() > 1 && consider_it) {
        FeaturePairI span1 = (FeaturePairI) hit.getFeatureAt (0);
        FeaturePairI span2 = (FeaturePairI) hit.getFeatureAt (1);

        FeaturePairI remove_span 
          = twilight.cleanUpTwilightZone (span1,
                                          span2,
                                          hit.size() == 2,
                                          testing);
        /* 
           If remove_span is null this means BOTH of the spans have
           bit scores that make them good enough to keep 
           
           If the span that is selected for removal is the terminal
           span (on the end), then go ahead and remove it

           Or if the selected span is the interior span, but the
           terminal span not good either then go ahead and remove
           the terminal span
        */
        if (remove_span != null &&
            (remove_span == span1 ||
             // both spans are crap
             (remove_span == span2 &&
              span1.getScore("bits") <= twilight.getCertainlyBad()))) {
          if (hit.size() == 2) {
            debugFeature(remove_span, "Removing weaker twilight span");
            hit.deleteFeature (remove_span);
          }
          else {
            debugFeature(span1, "Removing starting twilight span");
            hit.deleteFeature (span1);
          }
        } else {
          consider_it = false;
        }
      }
      consider_it = true;
      while (hit.size() > 1 && consider_it) {
        int last_span = hit.size() - 1;
        FeaturePairI span1 = (FeaturePairI) hit.getFeatureAt (last_span);
        FeaturePairI span2 = (FeaturePairI) hit.getFeatureAt (last_span - 1);

        FeaturePairI remove_span 
          = twilight.cleanUpTwilightZone (span1,
                                          span2,
                                          hit.size() == 2,
                                          testing);
        if (remove_span != null)
          debugFeature(remove_span, "May remove twilight span from end, is span1 " +
                       (remove_span == span1));
        if (remove_span != null &&
            (remove_span == span1 ||
             // both spans are crap
             (remove_span == span2 &&
              span1.getScore("bits") <= twilight.getCertainlyBad()))) {
          if (hit.size() == 2) {
            debugFeature(remove_span, "Removing weaker twilight span");
            hit.deleteFeature (remove_span);
          }
          else {
            debugFeature(span1, "Removing terminal twilight span");
            hit.deleteFeature (span1);
          }
        } else {
          consider_it = false;
        }
      }
    } catch (Exception e) {
      logger.error ("Error removing twilights " + e.getMessage(), e);
    }
  }

  private boolean spansOverlap (SeqFeatureI span1, SeqFeatureI span2,
        int coincidence) {
    // this checks for total enclosure of one span by the other
    // if this is true then the weaker one is always deleted
    boolean overlaps = false;
    double common = 0;
    int overlap = getOverlap (span1, span2);
    if (overlap > 0) {
      int extent_start = Math.min (span1.getLow(), span2.getLow());
      int extent_end = Math.max(span1.getHigh(), span2.getHigh());
      common = (overlap * 100) / (extent_end - extent_start + 1);
      overlaps = common >= coincidence;
      String msg = ("overlap = " + overlap +
        " common = " + common + "%" +
        " length = " + (extent_end - extent_start + 1) + " ");
      debugFeature (span1, msg);
    }
    return overlaps;
  }

  private int getOverlap (SeqFeatureI span1, SeqFeatureI span2) {
    // this returns the length of the overlap in NA or AA
    int overlap = 0;

    if (span1.contains (span2) || span2.contains (span1)) {
      // return the longer of two span lengths if one
      // contains the other
      overlap = (span1.length() > span2.length() ?
     span1.length() : span2.length());
    }
    else if (span1.overlaps(span2)) {
      // now test for partial overlaps
      int connect_start = (span1.getLow() < span2.getLow() ?
         span2.getLow() : span1.getLow());
      int connect_end = (span1.getHigh() > span2.getHigh() ?
       span2.getHigh() : span1.getHigh());
      overlap = connect_end - connect_start;
    }
    return overlap;
  }

  private void cleanUpShadows (Vector regions,
             FeatureSetI forward_analysis,
             FeatureSetI reverse_analysis) {
    for (int i = 0; i < regions.size (); i++) {
      Vector region = (Vector) regions.elementAt (i);
      for (int j = 0; j < region.size (); j++) {
  FeatureSetI hit = (FeatureSetI) region.elementAt (j);
  SequenceI align_seq = hit.getHitSequence();
  int k = j + 1;
  String hit_name = align_seq.getName();
  FeatureSetI close_hit;

  while (k < region.size()) {
    close_hit = (FeatureSetI) region.elementAt (k);
    SequenceI close_seq = close_hit.getHitSequence();
    /* same sequence, but opposite strand */
    if (hit_name.equals (close_seq.getName())
        && (hit.getStrand() != close_hit.getStrand())) {
      // assuming that the region list is presorted
      // with the stronger hit first
      region.removeElementAt (k);
      if (close_hit.getStrand() == 1)
        forward_analysis.deleteFeature (close_hit);
      else
        reverse_analysis.deleteFeature (close_hit);
      debugFeature (hit, "Removed because it is a shadow ");
    }
    else {
      k++;
    }
  }
      }
    }
  }

  private FeatureSetI getAnalysis (CurationSet curation,
           String analysis_type,
           int strand,
           String type) {

    FeatureSetI the_one = null;
    FeatureSetI analyses = (strand == 1 ?
          curation.getResults().getForwardSet() :
          curation.getResults().getReverseSet());
    
    for (int i = 0; i < analyses.size() && the_one == null; i++) {
      FeatureSetI analysis  = (FeatureSetI) analyses.getFeatureAt(i);
      String sf_type = analysis.getFeatureType();
      logger.debug("Looking for " + analysis_type + " checking " +
                   (sf_type == null ? "null" : sf_type));
      the_one = (sf_type != null && sf_type.equals (analysis_type) ?
     analysis : null);
    }
    return the_one;
  }

  private void joinESTs (FeatureSetI forward_analysis, 
                         FeatureSetI reverse_analysis) {
    int i = 0;
    Vector completed = new Vector();
    while (i < forward_analysis.size())	{
      FeatureSetI hit = (FeatureSetI) forward_analysis.getFeatureAt (i);
      if (!completed.contains (hit)) {
        i = joinEST (hit, forward_analysis, reverse_analysis, completed);
      }
      else {
        debugFeature(hit, 
                     "joinESTs: have already dealt with " + hit.getName() +
                     " on forward strand");
        i++;
      }
    }
    i = 0;
    while (i < reverse_analysis.size())	{
      FeatureSetI hit = (FeatureSetI) reverse_analysis.getFeatureAt (i);
      if (!completed.contains (hit)) {
        i = joinEST (hit, forward_analysis, reverse_analysis, completed);
      }
      else {
        debugFeature(hit,
                     "joinESTs: have already dealt with " + hit.getName() +
                     " on reverse strand");
        i++;
      }
    }
  }

  private boolean isFullLength (String name) {
    return ((name.indexOf ("complete") > 0) ||
            (name.indexOf ("contig") > 0 && name.indexOf ("prime") < 0));
  }

  private boolean is5prime (String name) {
    return (name.indexOf ("5prime") > 0);
  }

  private boolean is3prime (String name) {
    if (suffix_3prime != null)
      return (name.indexOf (suffix_3prime) > 0);
    else
      return (name.indexOf (default_3prime) > 0);
  }

  private FeatureSetI get5primeMostHit(FeatureSetI hit1, FeatureSetI hit2) {
    return (((hit1.getStrand() != -1 && hit2.getStart() < hit1.getStart()) ||
             (hit1.getStrand() == -1 && hit2.getStart() > hit1.getStart())) ?
            hit2 : hit1);
  }

  private FeatureSetI get3primeMostHit(FeatureSetI hit1, FeatureSetI hit2) {
    return (((hit1.isForwardStrand() && hit2.getEnd() > hit1.getEnd()) ||
             (!hit1.isForwardStrand() && hit2.getEnd() < hit1.getEnd())) ?
            hit2 : hit1);
  }

  private boolean estIsInternal (FeatureSetI leading_hit, 
                                 FeatureSetI trailing_hit,
                                 FeatureSetI est_hit) {
    return (estFollows (leading_hit, est_hit) &&
            estPrecedes3 (trailing_hit, est_hit));
  }

  private boolean estFollows (SeqFeatureI leading, SeqFeatureI check) {
    boolean follows =  ((leading.isForwardStrand() && 
                         check.getStart() >= leading.getStart()) ||
                        (!leading.isForwardStrand() &&
                         check.getStart() <= leading.getStart()));
    if (!follows) {
      debugFeature (check, "estFollows: this (?) should come after " +
                    ((FeatureSetI) leading).getHitSequence().getName());
    }
    return follows;
  }

  private boolean estPrecedes3 (FeatureSetI trailing_est, FeatureSetI est) {
    boolean trails = (trailing_est != null ?
                      (get3primeMostHit(trailing_est, est) == trailing_est) :
                      true);
    if (!trails)
      debugFeature (est, "estPrecedes3: this (?) should come before " +
                    trailing_est.getHitSequence().getName());
    return trails;
  }

  private int joinEST (FeatureSetI hit, 
                       FeatureSetI forward_analysis,
                       FeatureSetI reverse_analysis,
                       Vector completed) {
    FeatureSetI analysis;
    int hit_index = forward_analysis.getFeatureIndex (hit);
    if (hit_index < 0) {
      hit_index = reverse_analysis.getFeatureIndex (hit);
      analysis = reverse_analysis;
    }
    else
      analysis = forward_analysis;

    String name = hit.getHitSequence().getName();
    String clone = getPrefix(name);
    Vector est_list = new Vector();
    
    est_list.addElement (hit);
    completed.addElement (hit);
    
    debugFeature (hit, "joinEST: joining ends now ");

    // first simply collect all the other ESTs that are from
    // the same cDNA clone. do this by checking their names
    // assumption is that ESTs from the same cDNA share a prefix
    collectESTs (clone, forward_analysis, est_list, completed);
    collectESTs (clone, reverse_analysis, est_list, completed);

    FeatureSetI leading_est = getLeader(est_list, false);
    FeatureSetI trailing_est = getTrailer(est_list, leading_est, false);

    /* If there are any additional reads from this same
       clone now is the time to combine them into a single hit
    */
    if (est_list.size() > 1) {
      Vector est_sets = new Vector();
      est_sets.addElement (est_list);
      /* Usually there is just one set, but occasionally
         (in the case of chimeric clones) there is more than
         one set to be joined that are from the same clone 
         This can only be tested if there is both a 5' and 3'
         read to bracket the clone.
         Do this test, before making any changes to the strand
      */
      if (leading_est != null && trailing_est != null) {
        boolean chimeric = checkForChimericClones (est_list, 
                                                   est_sets,
                                                   leading_est,
                                                   trailing_est);
        if (!chimeric) {
          /*
            Now that the potential for a chimeric has been eliminated
            it is safe to move the 3' to the same strand as the 5'
            This must be done before the inversion check which relies
            on the two ESTs being on the same strand
          */
          if (leading_est != trailing_est &&
              leading_est.getStrand() != trailing_est.getStrand()) {
            pickStrand (leading_est, trailing_est);
          }
          // check for inverted clones, likewise only possible if
          // both ends of the clone (5' and 3') are present
          boolean inverted = checkForInversions (est_list,
                                                 leading_est,
                                                 trailing_est);
          if (!inverted)
            forceToStrand (est_list, leading_est.getStrand());
          else
            forceToStrand (est_list, trailing_est.getStrand());
        }
      }
      else {
        checkForMissingEnds (est_list, leading_est, trailing_est);
      }
    
      checkForMisidentity (est_sets);

      for (int i = 0; i < est_sets.size(); i++) {
        est_list = (Vector) est_sets.elementAt (i);
        // now that this has been finally chosen, take it off
        // the collected list and hang onto it as the EST that
        // any others that are merged will be hung off of
        SeqFeatureUtil.sort (est_list,
                             ((SeqFeatureI) est_list.elementAt(0)).getStrand());
        leading_est = getLeader (est_list, true);
        trailing_est = getTrailer (est_list, leading_est, true);
        String lead_name = leading_est.getHitSequence().getName();
        est_list.remove (leading_est);
        if (est_list.size() > 0) {
          // set up this hash table with the name of the original
          // sequence before changing the name
          Hashtable span_seqs = new Hashtable();
          String name_qualifier = (est_sets.size() > 1 ? ":set"+(i+1) : "");
          for (int j = 0; j < leading_est.size(); j++) {
            FeaturePairI span 
              = (FeaturePairI) leading_est.getFeatureAt(j);
            Vector seqs = new Vector();
            seqs.addElement (span.getHitSequence().getName() + " " + 
                             span.getHitSequence().getLength() + " bp");
            span_seqs.put (span, seqs);
          }
      
          // There is going to be a merger so we need to
          // rename the sequence to simply be that of the
          // clone
          String seq_id = getPrefix(lead_name);
          debugFeature (trailing_est, "Synthesizing EST hit to " + seq_id);

          SequenceI clone_seq = new Sequence(seq_id, "");
          clone_seq.setAccessionNo (seq_id);
          StringBuffer description = new StringBuffer();
          int internal_count = createDescription (leading_est,
                                                  description,
                                                  true,
                                                  0);

          leading_est.setHitSequence(clone_seq);
          for (int j = 0; j < est_list.size(); j++) {
            FeatureSetI est = (FeatureSetI) est_list.elementAt(j);
            internal_count = createDescription (est,
                                                description,
                                                false,
                                                internal_count);
      
            // merge these into one hit
            if (est.getScore() > leading_est.getScore()) {
              leading_est.setScore (est.getScore());
            }

            if (forward_analysis.getFeatureIndex(est) >= 0)
              forward_analysis.deleteFeature (est);
            else
              reverse_analysis.deleteFeature (est);

            debugFeature (est, "Adding EST to cDNA group ");
            while (est.size() > 0) {
              FeaturePairI span = (FeaturePairI) est.getFeatureAt(0);
              est.deleteFeature (span);
              debugFeature (span, "Adding EST span to cDNA group ");
              insertSpan (leading_est, span, span_seqs);
            }
          }
          if (internal_count > 0) {
            if (description.length() > 0)
              description.append (", ");
            description.append (internal_count + " internal reads");
          }
          clone_seq.setDescription (description.toString());
          extendAlignmentSeq (leading_est);
          setSpanSequences (leading_est, span_seqs, name_qualifier);
        }
      }
    } else {
      debugFeature (hit, "No other ESTS for " + name);
    }
    if (analysis.getFeatureIndex (hit) < 0)
      return hit_index;
    else
      return hit_index + 1;
  }

  private boolean checkForChimericClones (Vector est_list, 
                                          Vector est_sets,
                                          FeatureSetI leading_est,
                                          FeatureSetI trailing_est) {
    // if both the 3' and the 5' ESTs are available do a
    // sanity double-check to make sure that the clone is
    // not inverted. first make sure they are both on the
    // same strand. then make sure that when that is the
    // case the 5' end precedes the 3'end
    if (leading_est != null && trailing_est != null) {
      /* if the gap between the 2 reads is too large then we
         just don't believe it. It becomes more likely that the
         clone represents 2 separate mRNA transcripts that are
         together because of a cloning artifact and the gap
         is NOT due to an intron */
      if (gapBetween(leading_est, trailing_est) >= CHIMERA_GAP)	{
        debugFeature (leading_est, trailing_est, "Have a gap of " +
                      gapBetween(leading_est, trailing_est) + "bp ",
                      true);
        Vector new_list = new Vector();
  est_list.removeElement (trailing_est);
  addQualifier ("chimeric", trailing_est);
  new_list.addElement (trailing_est);
  for (int i = 0; i < est_list.size(); i++) {
    FeatureSetI est = (FeatureSetI) est_list.elementAt (i);
    addQualifier ("chimeric", est);
    // sort all of the reads between so that they go to the closest end
    if (gapBetween (est, leading_est) > gapBetween (est, trailing_est)) {
      est_list.removeElement (est);
      new_list.addElement(est);
    }
  }
  est_sets.addElement (new_list);
      }
    }
    return (est_sets.size() > 1);
  }

  private boolean checkForInversions (Vector est_list,
              FeatureSetI leading_est,
              FeatureSetI trailing_est) {
    boolean inverted = false;
    if (leading_est != null && trailing_est != null) {
      if (!estPrecedes3 (trailing_est, leading_est)) {
  addQualifier ("inverted", leading_est);
  addQualifier ("inverted", trailing_est);
  for (int i = 0; i < est_list.size(); i++) {
    FeatureSetI est = (FeatureSetI) est_list.elementAt (i);
    addQualifier ("inverted", est);
  }
  inverted = true;
  debugFeature (leading_est, "is inverted");
      }
    }
    return inverted;
  }

  private void checkForMissingEnds (Vector est_list,
              FeatureSetI leading_est,
              FeatureSetI trailing_est) {
    // Sound an alarm if 5' and 3' ESTs are missing
    FeatureSetI first_hit;
    String hit_name;
    if (leading_est == null && trailing_est == null) {
      cloneIsUnknown (est_list);
      first_hit = (FeatureSetI) est_list.elementAt (0);
      hit_name = first_hit.getHitSequence().getName();
    }
    else {
      first_hit = (leading_est != null ? leading_est : trailing_est);
      hit_name = first_hit.getHitSequence().getName();
      /* undo any previous rev-comping that has occured and get back
         to the ground state */
      if (first_hit.getProperty("revcomp").equals("true"))
        reverseEST (first_hit, true);
    }
    int strand = pickStrand(est_list);
    debugFeature (first_hit,
                  "Forcing to est " + hit_name +
                  " on strand " + strand);
    forceToStrand (est_list, strand);
  }

  private void checkForMisidentity (Vector est_sets) {
    for (int i = est_sets.size() - 1; i >= 0; i--) {
      Vector est_list = (Vector) est_sets.elementAt (i);
      FeatureSetI leading_est = getLeader (est_list, true);
      FeatureSetI trailing_est = getTrailer (est_list, leading_est, true);

      String lead_name = leading_est.getHitSequence().getName();

      // make sure that all of the internal hits are indeed internal
      // and that there haven't been any tracking errors
      int attempts = 0;
      boolean done = false;
      while (attempts < 2 && !done) {
        attempts++;
        done = true;
        for (int j = est_list.size() - 1 ; j >= 0 && done; j--) {
          FeatureSetI est = (FeatureSetI) est_list.elementAt(j);
          String est_name = est.getHitSequence().getName();
          boolean rev_comped = est.getProperty("revcomp").equals("true");
          // It is possible that the 5' end may be superceded
          // by a transposon read. As long as they overlap this
          // is acceptable
          if (!estIsInternal (leading_est, trailing_est, est)) {
            debugFeature (est, est_name + " is not internal to " + lead_name);
            // assume everything is okay
            boolean extended = true;
            if (!estFollows (leading_est, est)) {
              debugFeature (est, 
                            est_name + " does not follow 5' of " + lead_name);
              extended &= (estOverlaps (leading_est, est, 0));
              if (!extended)
                debugFeature (est, 
                              est_name + " does not overlap " + lead_name);
            }
            if (!estPrecedes3 (trailing_est, est))
              extended &= (estOverlaps (trailing_est, est, 
                                        trailing_est.size() - 1));
            if (!extended) {
              if (est_name.indexOf("prime") > 0 &&
                  !isFullLength(lead_name)) {
                if (rev_comped)
                  reverseEST (est, true);
                int strand = pickStrand(est_list);
                forceToStrand (est_list, strand);
                done = false;
              }
              else {
                addUnknownClone (est_sets, 
                                 est_list,
                                 est,
                                 leading_est);
              }
            }
          }
        }
      }
      if (attempts >= 2) {
        try {
          throw new BopException ("Not all EST reads from " + 
                                  lead_name + 
                                  " at " +
                                  leading_est.getStart() + "-" +
                                  leading_est.getEnd() +
                                  " are within 5' and 3' reads ");
        }
        catch (Exception e) {
          logger.error(e.getMessage(), e);
          logger.warn("removing " + lead_name);
          est_sets.removeElement (est_list);
        }
      }
    }
  }

  private boolean isQualified(String qualifier, FeatureSetI est) {
    return (est.getProperty(qualifier).equals("true"));
  }

  private void addQualifier (String qualifier, FeatureSetI est) {
    if (isQualified(qualifier, est))
      est.removeProperty(qualifier);
    else
      est.addProperty(qualifier, "true");
  }

  private void cloneIsUnknown (Vector est_list) {
    if (est_list.size() > 1) {
      for (int i = 0; i < est_list.size(); i++) {
        FeatureSetI est = (FeatureSetI) est_list.elementAt (i);
        addQualifier ("unknown", est);
      }
    }
  }

  private int gapBetween (SeqFeatureI est1, SeqFeatureI est2) {
    if (est1.overlaps (est2))
      return 0;
    else if (est1.getLow() < est2.getLow())
      return est2.getLow() - est1.getHigh();
    else
      return est1.getLow() - est2.getHigh();
  }

  private boolean estOverlaps (FeatureSetI hit1, FeatureSetI hit2, int terminus) {
    // if any span overlaps any other span
    boolean overlaps = false;
    FeaturePairI span1 = (FeaturePairI) hit1.getFeatureAt (terminus);
    for (int i = 0; i < hit2.size() && !overlaps; i++) {
      FeaturePairI span2 = (FeaturePairI) hit2.getFeatureAt (i);
      overlaps = (span1.overlaps (span2));
    }
    return overlaps;
  }

  private void insertSpan (FeatureSetI hit,
                           FeaturePairI new_span, 
                           Hashtable span_seqs) {
    FeaturePairI hit_span;
    int i = 0;
    boolean inserted = false;
    
    while (i < hit.size() && ! inserted) {
      hit_span = (FeaturePairI) hit.getFeatureAt(i);
      if (hit_span.overlaps (new_span)) {
        mergeSpans (hit_span, new_span);
        hit.adjustEdges (hit_span);
        // append this seq name to the list for this span
        Vector seqs = (Vector) span_seqs.get (hit_span);
        seqs.addElement (new_span.getHitSequence().getName() + " " + 
                         new_span.getHitSequence().getLength() + " bp");
        debugFeature(hit_span, "Merged spans " +
                     hit_span.getHitSequence().getName() +
                     " and " +
                     new_span.getHitSequence().getName());
        inserted = true;
      }
      else
        i++;
    }
    // make sure every span has a vector of names associated with it
    if (!inserted) {
      Vector seqs = new Vector();
      seqs.addElement (new_span.getHitSequence().getName() + " " + 
                       new_span.getHitSequence().getLength() + " bp");
      span_seqs.put (new_span, seqs);
      // append the new span
      if (!estFollows (hit, new_span)) {
        logger.warn ("How did insert of " +
                     new_span.getHitSequence().getName() + " at " + 
                     new_span.getStart() + " get before " + 
                     hit.getHitSequence().getName() + " at " +
                     hit.getEnd());
      }
      hit.addFeature (new_span, true);
    }
  }

  private void setSpanSequences (FeatureSetI leading_hit, 
                                 Hashtable span_seqs, 
                                 String name_qualifier) {
    // any two spans that have a name in common should share the
    // same sequence object for the alignment
    FeaturePairI span;
    String seq_id = leading_hit.getHitSequence().getName();
    Vector prev_seqs = null;
    SequenceI  prev_seq = null;
    int contig = 0;
    for (int i = 0; i < leading_hit.size(); i++) {
      span = (FeaturePairI) leading_hit.getFeatureAt(i);
      Vector seqs = (Vector) span_seqs.get (span);
      span.addProperty ("READS", appendToDescription ("", seqs));
      SequenceI seq = null;
      if (prev_seqs != null) {
        for (int j = 0; j < prev_seqs.size(); j++) {
          String prev_name = (String) prev_seqs.elementAt(j);
          for (int k = seqs.size() - 1; k >= 0; k--) {
            String this_name = (String) seqs.elementAt(k);
            if (prev_name.equals (this_name)) {
              seq = prev_seq;
              seqs.removeElementAt (k);
            }
          }
        }
      }

      String description = "";
      if (seq == null) {
        contig++;
        String span_name = seq_id + name_qualifier + ":contig" + contig;
        seq = new Sequence(seq_id, "");
        seq.setAccessionNo (span_name);
        prev_seqs = seqs;
        debugFeature (span, "Set span " + (i+1) + " to " + span_name);
      }
      else {
        description = seq.getDescription();
        for (int j = 0; j < seqs.size(); j++) {
          String this_name = (String) seqs.elementAt(j);
          prev_seqs.addElement (this_name);
        }
      }
      
      description = appendToDescription (description, seqs);
      seq.setDescription (description);
      span.setHitSequence(seq);
      prev_seq = seq;
    }
  }
    
  private String appendToDescription (String prefix, Vector seqs) {  
    String description = prefix;
    for (int j = 0; j < seqs.size(); j++) {
      String this_name = (String) seqs.elementAt(j);
      String suffix;
      int suffix_index = this_name.indexOf (".");
      if (suffix_index >= 0)
        suffix = this_name.substring(suffix_index + 1);
      else
        suffix = this_name;
      if (description.length() > 0) {
        description = (description + ", ");
      }
      description = (description + suffix);
    }
    return description;
  }

  // Reverse complement everything so that it matches the
  // 5prime sequence and then added to the ordered set of
  // ESTs. Use this rather than vector so that they are 
  // sorted automatically
  private void forceToStrand (Vector est_list, int strand) {
    for (int i = 0; i < est_list.size(); i++) {
      FeatureSetI est = (FeatureSetI) est_list.elementAt(i);
      if (est.getStrand() != strand) {
        reverseEST (est, true);
      }	    
    }
  }

  private int pickStrand(Vector est_list) {
    int strand = 0;
    for (int i = 0; i < est_list.size() && strand == 0; i++) {
      FeatureSetI est = (FeatureSetI) est_list.elementAt (i);
      String sim4_set = est.getProperty("sim4_set");
      if (sim4_set != null && sim4_set.equals("true"))
        strand = est.getStrand();
    }
    if (strand == 0) {
      FeatureSetI est = (FeatureSetI) est_list.elementAt (0);
      strand = est.getStrand();
    }
    return strand;
  }

  private void pickStrand (FeatureSetI est1, FeatureSetI est2) {
    if (est1.getStrand() != est2.getStrand()) {
      String sim4_set;
      sim4_set = est1.getProperty("sim4_set");
      boolean est1_set = (sim4_set != null && sim4_set.equals("true"));
      sim4_set = est2.getProperty("sim4_set");
      boolean est2_set = (sim4_set != null && sim4_set.equals("true"));
      if (est2_set && !est1_set)
        reverseEST(est1, true);
      else
        reverseEST(est2, true);
    }
  }

  private void reverseESTs (FeatureSetI hits, Vector completed) {
    /* go through these from the end of the list to the beginning
       because they may be moved(deleted) from this list of hits
       and if that happens the index into the list will be off
       by one if you increment 
    */
    for (int i = hits.size() - 1; i >= 0; i--) {
      FeatureSetI hit = (FeatureSetI) hits.getFeatureAt (i);
      String name = hit.getHitSequence().getName();
      if (is3prime(name) && !completed.contains(hit)) {
        debugFeature(hit, "Checking for reversal");
        reverseEST (hit, false);
        completed.addElement(hit);
      }	    
    }
  }

  private void reverseEST (FeatureSetI hit, boolean force) {
    SequenceI seq = hit.getHitSequence();
    String seq_name = seq.getName();
    String sim4_set = hit.getProperty("sim4_set");
    if (sim4_set != null && sim4_set.equals("true") && !force) {
      debugFeature (hit, "Tried reversing: " + seq_name + ", but it is set");
      return;
    }
    debugFeature (hit, "Reversing: " + seq_name);
    hit.flipFlop ();

    SequenceI aligned_seq = hit.getHitSequence();
    int seq_length = aligned_seq.getLength();
    for (int i = 0; i < hit.size(); i++) {
      FeaturePairI span = (FeaturePairI) hit.getFeatureAt(i);
      SeqFeatureI hit_span = span.getHitFeature();
      int pos1 = seq_length - hit_span.getStart() + 1;
      int pos2 = seq_length - hit_span.getEnd() + 1;
      hit_span.setStrand(pos2 <= pos1 ? 1 : -1);
      hit_span.setStart(pos2);
      hit_span.setEnd(pos1);
      String coord_seq = span.getExplicitAlignment();
      String align_seq = hit_span.getExplicitAlignment();
      if (coord_seq != null && !coord_seq.equals("") && 
          align_seq != null && !align_seq.equals("")) {
        coord_seq = DNAUtils.reverseComplement(coord_seq);
        align_seq = DNAUtils.reverseComplement(align_seq);
        //span.addProperty ("alignment", coord_seq);
        span.setExplicitAlignment(coord_seq);
        //span.setExplicitHitAlignment(align_seq);
        hit_span.setExplicitAlignment(align_seq);
        span.setCigar(CigarUtil.roll(coord_seq, align_seq, 1));
      }
    }
    if (hit.getProperty("revcomp").equals("true"))
      hit.removeProperty("revcomp");
    else
      hit.addProperty("revcomp", "true");
    String rev = DNAUtils.reverseComplement(seq.getResidues());
    seq.setResidues(rev);
    FeatureSetI analysis = (FeatureSetI) hit.getRefFeature();
    /* this check is probably not needed, they should be
       different by now */
    if (analysis.getStrand() != hit.getStrand()) {
      if (analysis == forward_analysis) {
        forward_analysis.deleteFeature(hit);
        reverse_analysis.addFeature(hit, true);
        debugFeature (hit, "Moved: " + seq_name + " to minus strand");
      } else if (analysis == reverse_analysis) {
        reverse_analysis.deleteFeature(hit);
        forward_analysis.addFeature(hit, true);
        debugFeature (hit, "Moved: " + seq_name + " to plus strand");
      } else
        logger.warn ("What is this analysis " + 
                     analysis.getName() + " a " +
                     analysis.getClass().getName() + " for " +
                     hit.getHitSequence().getName());
    } else {
      logger.warn ("Why are strands the same for " + 
                   analysis.getName() + " a " +
                   analysis.getClass().getName() + " and " +
                   hit.getHitSequence().getName());
    }
  }

  private void extendAlignmentSeq (FeatureSetI hit) {
    FeaturePairI span = (FeaturePairI) hit.getFeatureAt(0);
    if (span == null) {
      logger.warn ("extendAlignmentSeq: hit has no span " + 
                   hit.getHitSequence().getName());
      //      System.exit(1);
    }

    int offset = span.getHstart() - 1;

    for (int i = 0; i < hit.size(); i++) {
      span = (FeaturePairI) hit.getFeatureAt(i);
      int length = span.length();
      span.setHstart(offset + 1);
      span.setHend(offset + length);
      offset += length;
      debugFeature(span, " from offset " + offset +
                   " set span to " + span.getHstart() + "-" + span.getHend());
    }
    debugFeature(hit, " final total length is " + offset);
    hit.getHitSequence().setLength(offset);
  }

  private String getPrefix (String name) {
    String clone;
    
    if (name.indexOf(".") < 0)
      clone = name;
    else
      clone = name.substring(0, name.indexOf("."));
    
    return clone;
  }

  private void mergeSpans (FeaturePairI into, FeaturePairI from) {
    if (into.getStrand() != from.getStrand()) {
      // can't use this if it isn't in the same direction
      logger.error ("spans do not agree on direction.\n " +
                    into.getStart() + "," + into.getEnd() + " - " +
                    from.getStart() + "," + from.getEnd());
      return;
    }
    if (from.getHigh() > into.getHigh())
      into.setHigh (from.getHigh());
    if (from.getLow() < into.getLow())
      into.setLow (from.getLow());
  }

  private void addUnknownClone (Vector est_sets,
                                Vector est_list,
                                FeatureSetI est_hit,
                                FeatureSetI leading_hit) {
    logger.warn (est_hit.getHitSequence().getName() +
                 " at " +
                 est_hit.getStart() + "-" +
                 est_hit.getEnd() + 
                 " is somewhere different than the other ESTs");
    
    addQualifier ("unknown", est_hit);
    
    est_list.remove (est_hit);
    boolean placed = false;
    for (int i = 0; i < est_sets.size() && !placed; i++) {
      Vector check_hits = (Vector) est_sets.elementAt (i);
      if (check_hits != est_list &&
          check_hits.size() > 0) {
        FeatureSetI check_est = (FeatureSetI) check_hits.elementAt(0);
        if (gapBetween (check_est, est_hit) < CHIMERA_GAP) {
          placed = true;
          check_hits.addElement (est_hit);
        }
      }
    }
    if (!placed) {
      Vector new_list = new Vector();
      new_list.addElement (est_hit);
      est_sets.addElement (new_list);
    }
  }

  private int createDescription (FeatureSetI est, 
                                 StringBuffer description,
                                 boolean add_bp,
                                 int internal_count) {
    String est_name = est.getHitSequence().getName();
    if (add_bp)	{
      if (description.length() > 0)
        description.append (", ");
      description.append (est_name + " " +
                          est.getHitSequence().getLength() +
                          "bp");
    }
    else {
      internal_count++;
    }
    return internal_count;
  }

  private void collectESTs (String clone,
                            FeatureSetI analysis,
                            Vector est_list,
                            Vector completed) {
    for (int i = 0; i < analysis.size(); i++) {
      FeatureSetI check_hit = (FeatureSetI) analysis.getFeatureAt(i);
      String check_name = check_hit.getHitSequence().getName();
      String check_clone = getPrefix(check_name);
      if (clone.equals (check_clone) && !completed.contains (check_hit)) {
        // The read will be joined with the other read
        // don't sort these now, because they may be on opposite
        // strands
        est_list.addElement (check_hit);
        completed.addElement (check_hit);
      }
    }
  }

  /* don't use force unless the est_list have already all been
     put onto the same strand */
  private FeatureSetI getLeader (Vector est_list, boolean force) {
    FeatureSetI leading_est = null;
    FeatureSetI best_begin = (est_list.size() > 0 ?
                              (FeatureSetI) est_list.elementAt (0) : null);
    for (int i = 0; i < est_list.size(); i++) {
      FeatureSetI est = (FeatureSetI) est_list.elementAt(i);
      String est_name = est.getHitSequence().getName();
      best_begin = get5primeMostHit(best_begin, est);
      // check the perimeters
      if (isFullLength(est_name) || is5prime(est_name) ||
          (is3prime(est_name) && isQualified("inverted", est))) {
        if (leading_est != null) {
          leading_est = get5primeMostHit(leading_est, est);
        }
        else {
          leading_est = est;
        }
      }
    }
    if (force && leading_est == null) {
      leading_est = best_begin;
      debugFeature(leading_est, "Leading 5' EST is forced to be " + 
                   best_begin.getHitSequence().getName());
    } 
    return leading_est;
  }

  private FeatureSetI getTrailer (Vector est_list, 
                                  FeatureSetI leading_est,
                                  boolean force) {
    FeatureSetI trailing_est = null;
    FeatureSetI best_end = (est_list.size() > 0 ?
                            (FeatureSetI) est_list.elementAt (0) : null);
    for (int i = 0; i < est_list.size(); i++) {
      FeatureSetI est = (FeatureSetI) est_list.elementAt(i);
      best_end = get3primeMostHit(best_end, est);
      String est_name = est.getHitSequence().getName();
      if (isFullLength(est_name) || is3prime(est_name) ||
          (is5prime(est_name) && isQualified("inverted", est))) {
        if (trailing_est != null) {
          /* have both a 3 prime and a full length, so take
             the one that extends most 3 prime. Assuming here
             that they are on the same strand, otherwise this
             test is meaningless */
          trailing_est = get3primeMostHit(trailing_est, est);
        }
        else {
          trailing_est = est;
        }
      }
    }
    if (force) {
      boolean changed = false;
      if (trailing_est == null) {
        trailing_est = best_end;
        debugFeature(trailing_est, "Trailing 3' EST is forced to be " + 
                     best_end.getHitSequence().getName());
        changed = true;
      }
      if (trailing_est == leading_est &&
          !isFullLength(trailing_est.getHitSequence().getName())) {
        int index = est_list.size() - 1 - est_list.indexOf (leading_est);
        trailing_est = (FeatureSetI) est_list.elementAt (index);
        debugFeature(trailing_est,
                     "Trailing 3' EST is forced to end opposite " + 
                     leading_est.getHitSequence().getName());
        changed = true;
      }
      if (!changed)
        debugFeature (trailing_est, "Trailing 3' EST is " + 
                      trailing_est.getHitSequence().getName());
    }
    return trailing_est;
  }

}
