/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */

package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import java.text.ParseException;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.util.FastaHeader;
import apollo.analysis.filter.AnalysisInput;

import org.apache.log4j.*;

import org.bdgp.util.DNAUtils;

public class Sim4Parser extends AlignmentParser {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Sim4Parser.class);

  private static String [] intron_begin = {"|>",
                                           " >",
                                           "->",
                                           "|<",
                                           " <",
                                           "-<"};

  private static String [] intron_end = {">|",
                                         "> ",
                                         ">-",
                                         "<|",
                                         "< ",
                                         "<-",
                                         ">>>",
                                         "<<<"};

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  boolean complement = false;
  boolean introns_reverse;
  boolean introns_forward;
  int donor_index;
  Stack introns = new Stack();
  int line_number = 0;
  String seq1_line;
  String seq2_line;
  String fasta1_line;
  String fasta2_line;
  AnalysisInput analysis_input;

  public Sim4Parser () {
  }

  //regexp can be 0-length string (null)
  public String load (CurationSet curation,
                      boolean new_curation,
                      InputStream data_stream,
                      AnalysisInput input) {
    this.analysis_input = input;
    initLoad(curation, new_curation, data_stream, input);
    int numAlignsTotal = 0;
    int numAlignsChucked = 0;
    
    if (parsed) {
      // don't assume that the input file is
      // output from the sim4 program
      try {
        while (seq2_line != null) {
          FeatureSetI match = setupMatch (curation);
          
          if (match != null) {
            ++numAlignsTotal;
            introns.clear();
            line = grabSpans (match);
            if (match.size() > 0) {
              if (introns_reverse ||
                  (complement && !introns_forward && !introns_reverse)) {
                if (!complement && introns_reverse)
                  thisIsOdd(match);
                revIt(match);
              } 
              else if (complement && introns_forward)  {
                thisIsOdd(match);
                // flipflop(match);
              }
              if (introns_forward || introns_reverse)
                match.addProperty("sim4_set", "true");
              if (match.getStrand() == 1)
                forward_analysis.addFeature (match, true);
              else
                reverse_analysis.addFeature (match, true);
            }
            else {
              ++numAlignsChucked;
            }
          }
          else {
            readLine();
          }
          findMatch(curation, false);
        }
        data.close();
      }
      catch (Exception ex) {
        parsed = false;
        logger.error(ex.getMessage(), ex);
      }
    }
    if (numAlignsChucked > 0) {
      apollo.util.IOUtil.errorDialog(numAlignsChucked + " alignment" + (numAlignsChucked > 1 ? " sets " : " set ") +
          "chucked out of " + numAlignsTotal + " due to inconsistencies / cutoffs");
    }
    return (parsed ? getAnalysisType() : null);
  }
  
  public String getProgram () {
    return "sim4";
  }

  public boolean recognizedInput () {
    //inside findMatch set query_seq (Sequence object)?!
    boolean found = false;
    try {
      readLine();
      found = findMatch (curation, new_curation);
      if (found) {
        database = grabDatabase();
      }
    }
    catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return (found);
  }

  private boolean findMatch (CurationSet curation,
                             boolean new_curation)
    throws ParseException {
    seq1_line = null;
    seq2_line = null;
    fasta1_line = null;
    fasta2_line = null;
    try {
      while (line != null && seq2_line == null) {
        if (line.startsWith ("seq1 = "))
          seq1_line = line;
        else if (line.startsWith ("seq2 = ") && validSeq2(line))
          seq2_line = line;
        readLine();
      }
      /* The fasta header lines, may or may not be there
         We don't want to ignore them if they are available,
         but we don't want to roll past the data when searching
         for them. Assume that they are in the next lines
         or they are not there after all */
      readLine();
      if (line != null && line.startsWith (">")) {
        fasta1_line = line;
        readLine();
        if (line != null && line.startsWith (">")) {
          fasta2_line = line;
          readLine();
        } 
      }
    }
    catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    if (seq1_line != null && seq2_line != null) {
      if (curation.getRefSequence() == null) {
        SequenceI focal_seq;
        int query_length = parseLength(seq1_line);
        if (fasta1_line != null) {
          focal_seq = initSequence (curation, fasta1_line, query_length);
        }
        else {
          int index = seq1_line.lastIndexOf('/');
          if (index < 0)
            index = "seq1 = ".length();
          focal_seq = initSequence (curation,
                                    seq1_line.substring(index),
                                    query_length);
        }
        initCuration (curation, focal_seq);
      }
      return true;
    }
    else {
      return false;
    }
  }

  protected int parseLength(String line) throws ParseException {
    String length_str = "";
    int length = 0;
    try {
      length_str = line.substring (line.lastIndexOf(", ") +
                                   ", ".length(),
                                   line.lastIndexOf(" bp"));
      length = Integer.parseInt (length_str);
    } 
    catch (RuntimeException ex) {
      throw new ParseException("Error parsing line, "+
                               "looking for \", bp\":"
                               +"  " + line, line_number);
    }
    return length;
  }

  protected String grabDatabase() {
    String db;
    db = analysis_input.getDatabase();
    if (db == null || db.equals("")) {
      int index = ((seq2_line.lastIndexOf ("/") > 0) ?
                   seq2_line.lastIndexOf ("/") + "/".length() :
                   "seq2 = ".length());
      db = seq2_line.substring(index);
      index = db.indexOf(" ");
      if (index > 0)
        db = db.substring (0, index);
      logger.info ("Sim4 parsed out db " + db);
      analysis_input.setDatabase(db);
    }
    return db;
  }

  private FeatureSetI setupMatch (CurationSet curation)
    throws ParseException {
    FeatureSetI match = null;
    String header;
    if (fasta2_line == null) {
      try {
        int index = seq2_line.indexOf(" (>");
        if (index == -1) {
          index = seq2_line.indexOf(" (");
        }
        header = seq2_line.substring(index + " (".length(),
            seq2_line.indexOf("), ", index));
      } catch (Exception e) {
        throw new ParseException("Error parsing header from " + seq2_line,
            line_number);
      }
    } else {
      header = fasta2_line;
    }
    FastaHeader temp = new FastaHeader(header);
    String match_name = temp.getSeqId();
    // make sure this isn't a self-hit
    if (match_name.indexOf(curation.getRefSequence().getName()) < 0) {
      int length = parseLength(seq2_line);
      SequenceI seq = initSequence (curation, header, length);
      if (seq != null) {
        match = initAlignment(seq, 0);
      }
    }
    return match;
  }

  private boolean validSeq2 (String line) {
    int open_paren = line.indexOf ("(");
    int close_paren = line.lastIndexOf ("),");
    return (open_paren >= 0 &&
            close_paren >= 0 &&
            open_paren + "(".length() < close_paren);
  }

  private String grabSpans (FeatureSetI match)
    throws ParseException {
    if (line.length() == 0) {
      line = "";
      readLine();
    }
    boolean value_found = false;
    boolean keep_it = true;
    complement = false;
    donor_index = -1;
    SequenceI aligned_seq = match.getHitSequence();
    FeaturePairI prev_span = null;

    int seq_length = aligned_seq.getLength();
    try {
      while (line != null && !value_found) {
        String untrimmed_line = line;
        line = line.trim();
        if (line.startsWith ("seq")) {
          if (prev_span != null)
            rollCigar(prev_span);
          value_found = true;
        }
        else if (line.startsWith (">")) {
          // grabDescription (aligned_seq);
          logger.error ("line starts with '>': this should no longer happen");
        }
        else if (line.length() > 0 && (line.startsWith ("(complement)"))) {
          complement = true;
        }
        else if (line.indexOf ("%") >= 0 &&
                 line.indexOf ("(") >= 0) {
          int index = line.indexOf ("-");
          keep_it &= index > 0;
          if (keep_it) {
            FeaturePairI span = grabSpan (seq_length);
            if (span != null && 
                // span.getScore() > 0 &&
                !overlapsExisting (match, span)) {
              if (match.size() == 0) {
                match.setStrand (span.getStrand());
              }
              addAlignPair (span, match);
            }
          }
        }

        // this is a bit risky as a test for the initial
        // line of an alignment 4-some, but...
        else if (line.length() > 0 &&
                 untrimmed_line.indexOf ("0 ") >= 0 &&
                 untrimmed_line.charAt (0) == ' ') {
          // see if this is the alignment,
          // which will be a series of line quadruplets
          // any one line may contain fragments of alignments
          // belonging to two different spans
          prev_span = grabAlignment (seq_length, match, prev_span);
          keep_it &= prev_span != null;
        }
        else if (line.length() > 0) {
          //sim4 sometimes gives out some warning, let it go
          if (line.indexOf("The two sequences are not really similar.") >= 0
              || line.indexOf("Please try an exact aligning method.") >= 0) {
            logger.warn(match.getHitSequence().getName() +
                        ": sim4 has a warning \"" + line + "\"");
          } 
          else {
            logger.warn (match.getHitSequence().getName() + 
                         ": Don't know how to parse \"" + line +
                         "\"");
            //      System.exit (1);
          }
        }
        if (!value_found) {
          readLine();
        }
      }
    }
    catch (RuntimeException ex) {
      throw new ParseException("Error parsing " + line,
                               line_number);
    }	
    catch( Exception ex ) {
      logger.error ("Could not parse " + line + 
                    " for " + 
                    aligned_seq.getName(), ex);
      keep_it = false;
    }
    //stack of introns is empty when resultspan is empty!
    //this is a quick fix for empty stack exception
    if (match.size() > 0) {
      setIntrons (match);
    }

    /* don't keep this alignment if it crosses strands */
    keep_it &= ! (introns_forward && introns_reverse);
    if (!keep_it) {
      String name = match.getHitSequence().getName();
      /*
        if (match.size() > 0)
        System.out.println (name + " deleting because it crosses strands");
      */
      while (match.size() > 0) {
        match.deleteFeatureAt(0);
      }
    }
    return (line);
  }

  private FeaturePairI grabSpan (int seq_length)
    throws ParseException {
    String before = line;
    FeaturePairI span = null;

    try {
      int index = line.indexOf ("-");
      String seq_start = line.substring (0, index);
        
      index++;
      line = line.substring (index);
      index = line.indexOf (" ");
      String seq_end = line.substring (0, index);

      index = line.indexOf ("(");
      index++;
      line = line.substring (index);
      index = line.indexOf ("-");
      String match_start = line.substring (0, index);
        
      index++;
      line = line.substring (index);
      index = line.indexOf (")");
      String match_end = line.substring (0, index);
      
      index++;
      line = line.substring (index);
      index = line.indexOf ("%");
      String score = line.substring (0, index);
      score = score.trim();
      
      String current_intron = ((line.indexOf ("<-") >= 0) ? 
                               "reverse" :
                               ((line.indexOf ("->") >= 0) ? 
                                "forward" : ""));
      introns.push (current_intron);

      span = initAlignPair();
      int start;
      int end;
      // since coordinates are 1-based, subtract -1 from offset to get correct
      // offset
      int genomic_offset = input.getOffset() - 1;
      start = Integer.parseInt (seq_start);
      end = Integer.parseInt (seq_end);
      span.setStrand (start <= end ? 1 : -1); 
      span.setStart(start + genomic_offset);
      span.setEnd(end + genomic_offset);
      start = Integer.parseInt (match_start);
      end = Integer.parseInt (match_end);
      span.setHstrand (start <= end ? 1 : -1); 
      span.setHstart(start);
      span.setHend(end);
      span.setScore((Double.valueOf(score)).doubleValue());
      span.addScore ("identity", span.getScore());
    }
    catch (RuntimeException ex)	{
      throw new ParseException("Error parsing " + before,
                               line_number);
    }
    return span;
  }			  

  private void setIntrons (FeatureSetI match) {
    introns_reverse = false;
    introns_forward = false;
    // the last one always leads no where
    introns.pop();

    /* the polyA tails MUST be removed before this discrepancy
       check because they are often aligned as single spans in
       the opposite orientation and that renders this entire
       congruency check for the exons all being connected in
       the same orientation invalid */
    int exon_cnt = match.size();
    if (exon_cnt > 1) {
      // check both ends to be sure
      int index = exon_cnt - 1;
      FeaturePairI span = (FeaturePairI) match.getFeatureAt(index);
      String dna = span.getResidues();
      int intron_length = Math.abs(span.getStart() -
                                   match.getFeatureAt(index - 1).getEnd());
      String name = span.getHitFeature().getRefSequence().getName();
      if (Compress.isPolyATail (dna,
                                intron_length,
                                (name+"-span:"+(index+1)))) {
        debugFeature (match, "Deleting polyA tail from end " + 
                      " intron:exon is " + intron_length + ":" +
                      dna.length() +
                      " dna=" + dna);
        match.deleteFeature(span);
        match.setFlag(true, FeatureSet.POLYA_REMOVED);
        String stuff = (String) introns.pop();
      } else {
        span = (FeaturePairI) match.getFeatureAt(0);
        dna = span.getResidues();
        intron_length = (match.size() > 1 ?
                         Math.abs(match.getFeatureAt(1).getStart() -
                                  span.getEnd()) :
                         0);
        name = span.getHitFeature().getRefSequence().getName();
        if (Compress.isPolyATail (dna,
                                  intron_length,
                                  (name+"-span:1"))) {
          debugFeature (match, "Deleting polyA tail from beginning "  +
                        " intron:exon is " + intron_length + ":" +
                        dna.length() +
                        " dna=" + dna);
          match.deleteFeature(span);
          match.setFlag(true, FeatureSet.POLYA_REMOVED);
          if (!introns.empty()) {
            introns.removeElementAt(0);
          }
        }
      }
    }

    String name = match.getHitSequence().getName();
    while (!introns.empty()) {
      String current_intron = (String) introns.pop();
      introns_forward |= current_intron.equals ("forward");
      introns_reverse |= current_intron.equals ("reverse");
    }
  }

  private int findPositionIndex(String align_line) {
    int index = -1;
    boolean found = false;
    for (int i = 0; i < align_line.length() && !found; i++) {
      char c = align_line.charAt(i);
      if (Character.isDigit(c))
        index = i + 1;
      else 
        found = (index >= 0);
    }
    return index;
  }

  private int parsePosition(String align_line, int space) {
    int pos = 0;
    try {
      if (space >= 0)
        pos = Integer.parseInt (align_line.substring(0, space).trim());
      else
        pos = Integer.parseInt (align_line);
    } catch (Exception e) {
      logger.error ("Unable to parse alignment position from " +
                    align_line + " at " + space, e);
    }
    return pos;
  }
  
  private String trimOffPosition(String align_line, int index) {
    if (index >= 0)
      return (align_line.substring (index));
    else
      return (" ");
  }

  protected FeaturePairI grabAlignment (int seq_length,
                                        FeatureSetI match,
                                        FeaturePairI prev_span)
    throws ParseException {
    String query_line = "";
    String align_line = "";
    String sbjct_line = "";

    if (donor_index > 0) {
      donor_index = 0;
    }

    FeaturePairI curr_span = null;
    try {
      boolean debug = false;
      query_line = data.readLine();
      line_number++;
      align_line = data.readLine();
      line_number++;
      sbjct_line = data.readLine();
      line_number++;

      //get positions for the current lines (2567) in:"   2567 CCGAGTCGTGCG"
      int space = findPositionIndex(query_line);
      logger.debug ("Space is at " + space);
      int query_pos = parsePosition(query_line, space);
      int sbjct_pos = parsePosition(sbjct_line, space);
      space++;
      query_line = trimOffPosition(query_line, space);
      sbjct_line = trimOffPosition(sbjct_line, space);
      /* Can not do a trim on the alignment line because it
         may have a meaningful blank space. Hope this is safe */
      align_line = trimOffPosition(align_line, space);
      align_line = align_line.replace(' ', '-');
      
      curr_span = (FeaturePairI) match.getFeatureContaining (query_pos);
      if (curr_span == null) {
        for (int i = 0; i < match.size() && curr_span == null; i++) {
          FeaturePairI s = (FeaturePairI) match.getFeatureAt(i);
          if (s.getHitFeature().contains (sbjct_pos)) {
            curr_span = s;
          }
        }
      }
      if (curr_span == null) {
        // ARrrrGggggH!!! i hate sim4
        // throw in the towel
        logger.error ("Have no span at " + query_pos +
                      " limits are " +
                      match.getStart() + "-" +
                      match.getEnd() + " " +
                      match.getHitSequence().getName() + " " +
                      match.size() +
                      " spans");

        for (int i = 0; i < match.size(); i++) {
          FeaturePairI s = (FeaturePairI) match.getFeatureAt(i);
          logger.error ("match " + i + ": " + s.getStart() + "-" + s.getEnd());
        }
        // System.exit (-1);
        // be polite about it
        return null;
      }

      /* This is needed if the current span has moved on */
      if (prev_span != null && curr_span != prev_span)
        rollCigar(prev_span);

      
      int query_index = (curr_span != null ? 
                         match.getFeatureIndex (curr_span) : -1);
      
      int [] exon_indices = exonIndex (align_line, debug);
      int start_index = exon_indices [0];
      int end_index = exon_indices [1];
      
      while (start_index >= 0) {
        String query_align_str = getSubAlign(query_line,
                                             start_index,
                                             end_index);
        
        String sbjct_align_str = getSubAlign(sbjct_line,
                                             start_index,
                                             end_index);

        logger.debug(match.getName() + " start_index=" + start_index +
                     " end_index=" + end_index + " in line " +
                     align_line + " parsed out " +
                     query_align_str);
        /*
          383  TGGTCTTTGTCCAGACAGACAAATCAATCTACAAACCAGGGCAGACAG
          ||||||||||||||||||||||||||||||||||||||||||||||||>>
          8505 TGGTCTTTGTCCAGACAGACAAATCAATCTACAAACCAGGGCAGACAGGT
        */
        // make them equal length strings
        if (sbjct_align_str.length() > query_align_str.length()) {
          sbjct_align_str 
            = sbjct_align_str.substring(0, query_align_str.length());
        }
        if (sbjct_align_str.length() < query_align_str.length()) {
          query_align_str 
            = query_align_str.substring(0, sbjct_align_str.length());
        }
        
        //replace space with -
        sbjct_align_str = sbjct_align_str.replace(' ', '-');
        query_align_str = query_align_str.replace(' ', '-');
        // remove ... at front of span or at end of span 
        // and reduce --- for other
        if (sbjct_align_str.startsWith(".")
            || query_align_str.startsWith(".")) {
          while (sbjct_align_str.startsWith(".")
                 || query_align_str.startsWith(".")) {
            sbjct_align_str = sbjct_align_str.substring(1);
            query_align_str = query_align_str.substring(1);
          }
        }
        if (sbjct_align_str.endsWith(".")
            || query_align_str.endsWith(".")) {
          while (sbjct_align_str.endsWith(".")
                 || query_align_str.endsWith(".")) {
            sbjct_align_str 
              = sbjct_align_str.substring(0, sbjct_align_str.length() - 1);
            query_align_str 
              = query_align_str.substring(0, query_align_str.length() - 1);
          }
        }
        //set query span alignment string
        align_seq += sbjct_align_str;
        coord_seq += query_align_str;

        if (end_index >= 0) {
          align_line = align_line.substring (end_index);
          if (end_index < query_line.length())
            query_line = query_line.substring (end_index);
          else
            query_line = "";
          if (end_index < sbjct_line.length())
            sbjct_line = sbjct_line.substring (end_index);
          else
            sbjct_line = "";
          query_line = padForAlign(query_line, 
                                   align_line.length());
          sbjct_line = padForAlign(sbjct_line, 
                                   align_line.length());
          exon_indices = exonIndex (align_line, debug);
          start_index = exon_indices [0];
          end_index = exon_indices [1];
    
          if (start_index >= 0) {
            int span_index = match.getFeatureIndex(curr_span);
            span_index++;
            prev_span = curr_span;
            rollCigar (prev_span);
            curr_span = (FeaturePairI) match.getFeatureAt(span_index);
            if (curr_span == null) {
              logger.error (match.getHitSequence().getName() +
                            " with " + 
                            match.size() + 
                            " spans " +
                            " has alignment\n\t" + 
                            query_line + "\n\t" +
                            align_line + "\n\t" +
                            sbjct_line + "\n" +
                            " BUT spans are null " +
                            " span_index is " + span_index);
              for (int i = 0; i < match.size(); i++) {
                SeqFeatureI s = match.getFeatureAt(i);
                logger.error ("match " + i + ": " + s.getStart() + "-" + s.getEnd());
              }
            }
          }
        }
        else {
          start_index = -1;
        }
      }
    }
    catch (Exception ex) {
      logger.error ("Error parsing alignment of " + 
                    match.getHitSequence().getName() + 
                    "\n\t" + query_line +
                    "\n\t" + align_line +
                    "\n\t" + sbjct_line, ex);
      return null;
    }	
    return curr_span;
  }

  private String getSubAlign(String align_str, int start_index, int end_index){
    if (start_index >= align_str.length())
      return "";
    else
      return ((end_index < 0 || end_index >= align_str.length()) ?
              align_str.substring (start_index) :
              align_str.substring (start_index, end_index));
  }

  private int [] exonIndex (String align_line, boolean debug) {
    int [] exon_indices = {0, align_line.length() - 1};
      
    if (donor_index < 0 &&
        (align_line.startsWith ("<<<") ||
         align_line.startsWith (">>>"))) {
      donor_index = 1;
    }

    if (donor_index >= 0) {
      // find the beginning of the next exon
      int find_it = -1;
      String the_end = "";
      for (int i = 0; i < intron_end.length; i++) {
        int index = (align_line.indexOf (intron_end[i], donor_index));
        if (index >= 0 && (find_it < 0 || index < find_it)) {
          find_it = index;
          the_end = intron_end[i];
        }
      }
      if (find_it >= 0) {
        if (the_end.length() == 2)
          exon_indices[0] = find_it + 1;
        else {
          find_it += the_end.length();
          exon_indices[0] = (find_it >= align_line.length() ?
                             -1 : find_it); //-1;
        }
        donor_index = -1;
      }
      else {
        exon_indices[0] = -1;
      }
    }
    if (donor_index < 0 && exon_indices[0] >= 0) {
      // find the end of the this exon
      int find_it = -1;
      for (int i = 0; i < intron_begin.length; i++) {
        int index = (align_line.indexOf (intron_begin[i], 
                                         exon_indices[0]));
        if (index >= 0 && (find_it < 0 || index < find_it)) {
          /* should be 3 in a row or the end of the line 
             index = (align_line.indexOf (">", index) >= 0 ?
             align_line.indexOf (">", index) :
             align_line.indexOf ("<", index));
             char intron_char = align_line.charAt (index);
             boolean found_intron = true;
             index++;

             for (int j = 0; 
             j < 2 && index < align_line.length() && found_intron;
             j++) {
             if (index < align_line.length()) {
             found_intron = align_line.charAt(index) == intron_char;
             index++;
             }
          */
          find_it = (align_line.indexOf (">", index) >= 0 ?
                     align_line.indexOf (">", index) :
                     align_line.indexOf ("<", index));
        }
      }
      if (find_it >= 0) {
        exon_indices[1] = find_it;
        donor_index = 1;
      }
      else
        exon_indices[1] = -1;
    }
    return exon_indices;
  }
    
  private String padForAlign (String str, int length) {
    if (str.length() < length) {
      int pad = length - str.length();
      StringBuffer buf = new StringBuffer (pad);
      for (int i = 0; i < pad; i++)
        buf = buf.append (' ');
      return str + buf.toString();
    }
    else
      return str;
  }

  private boolean overlapsExisting (FeatureSetI hit, FeaturePairI new_span) {
    /* sim4 sometimes produces overlapping
       spans. in this case lets merge them
       and see what happens */
    boolean overlaps = false;
    for (int i = 0; i < hit.size() && !overlaps; i++) {
      FeaturePairI check_span = (FeaturePairI) (hit.getFeatureAt(i));
      overlaps = check_span.overlaps (new_span);
      if (overlaps) {
        logger.debug (hit.getHitSequence().getName() + 
                      " has overlapping spans.\n\told at " +
                      check_span.getStart() + "-" + 
                      check_span.getEnd() +
                      "\n\tnew at " +
                      new_span.getStart() + "-" +
                      new_span.getEnd());
        extendWith (check_span, new_span);
        extendWith (check_span.getHitFeature(), new_span.getHitFeature());
        hit.adjustEdges ();
      }
    }
    return overlaps;
  }

  private void flipflop (FeatureSetI match) {
    for (int i = 0; i < match.size(); i++) {
      FeaturePairI span = (FeaturePairI) match.getFeatureAt(i);
      SeqFeatureI hit_span = span.getHitFeature();
      revAlignment(span, hit_span);
    }
  }

  private void revIt (FeatureSetI match) {
    match.flipFlop();
    SequenceI aligned_seq = match.getHitSequence();
    int seq_length = aligned_seq.getLength();
    for (int i = 0; i < match.size(); i++) {
      FeaturePairI span = (FeaturePairI) match.getFeatureAt(i);
      SeqFeatureI hit_span = span.getHitFeature();
      int pos1 = seq_length - hit_span.getStart() + 1;
      int pos2 = seq_length - hit_span.getEnd() + 1;
      hit_span.setStrand(pos2 <= pos1 ? 1 : -1);
      hit_span.setStart(pos2);
      hit_span.setEnd(pos1);
      revAlignment(span, hit_span);
    }
  }

  private void revAlignment(FeaturePairI span, SeqFeatureI hit_span) {
    coord_seq = span.getExplicitAlignment();
    align_seq = span.getHitFeature().getExplicitAlignment();
    if (coord_seq != null && !coord_seq.equals("") && 
        align_seq != null && !align_seq.equals("")) {
      coord_seq = DNAUtils.reverseComplement(coord_seq);
      align_seq = DNAUtils.reverseComplement(align_seq);
      rollCigar(span);
    }
  }

  public void extendWith (SeqFeatureI to, SeqFeatureI from) {
    if (to.getStrand() != from.getStrand()) {
      // can't use this if it isn't in the same direction
      logger.error ("Error: spans do not agree on direction.\n " +
                    to.getStart() + "," + to.getEnd() + " - " +
                    from.getStart() + "," + from.getEnd());
      return;
    }

    /*
      if (to.next_span != null && 
      from.getNextSpan() != null &&
      to.next_span != from.getNextSpan()) {
      logger.error ("These two spans cannot be merged they " +
      "link to different spans. Spans " +
      to.getStart() + "-" + to.getEnd() +
      " and " + from.getStart() + "-" + from.getEnd());
      return;
      }
      if (next_span == null)
      setNextSpan (span.getNextSpan());
    */
    if (from.getLow() < to.getLow())
      to.setLow (from.getLow());
    if (from.getHigh() > to.getHigh())
      to.setHigh (from.getHigh());
  }  
  
  private void thisIsOdd(FeatureSetI match) {
    SequenceI aligned_seq = match.getHitSequence();
    String value = ((!complement && introns_reverse) ?
                    "nocomp_rev" : (complement && introns_forward ?
                                    "comp_forward" : "comp_either"));
    if (!match.getProperty("ODD").equals(""))
      match.removeProperty("ODD");
    else
      match.addProperty("ODD", value);
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
      logger.error ("Something seriously wrong with feature " + sf.toString());
    }
    return name;
  }

  private void debugFeature (SeqFeatureI sf, String prefix) {
    String name = debugName (sf);
    // TODO - use Log4J debug levels for this:
    if (input.debugFilter(name)) {
      logger.info (prefix + "\n\t" +
                    name + " " +
                    " strand=" + sf.getStrand() +
                    " start=" + sf.getStart() + 
                    " end =" + sf.getEnd() +
                    "\n\tlength=" + sf.length() +
                    " expect=" + sf.getScore("expect") +
                    " score=" + sf.getScore() +
                    " type=" + sf.getFeatureType());

      if (sf instanceof FeatureSetI) {
        FeatureSetI fs = (FeatureSetI) sf;
        for (int i = 0; i < fs.size(); i++) {
          FeaturePairI fp = (FeaturePairI) fs.getFeatureAt(i);
          logger.info ("\tSpan " + (i+1) + 
                       " genomic range=" + fp.getStart() + 
                       "-" + fp.getEnd() +
                       " EST range=" + fp.getHstart() + 
                       "-" + fp.getHend());
        }
      }
    }
  }

}

