/* Copyright (c) 2000 Berkeley Drosophila Genome Center, UC Berkeley. */
/* Here's an example of BLASTN output, v 2.2.11, from the NCBI blast server as of June 2005.
   I think we should add examples of other supported formats as well.

BLASTN 2.2.11 [Jun-05-2005]

Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaeffer, 
Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman 
(1997), "Gapped BLAST and PSI-BLAST: a new generation of 
protein database search programs",  Nucleic Acids Res. 25:3389-3402.

RID: 1121286816-10479-186018140443.BLASTQ4

Query=  Acyp
          (363 letters)

Database: NCBI Chromosome Sequences 
           4271 sequences; 17,691,404,557 total letters

                                                                   Score     E
Sequences producing significant alignments:                        (Bits)  Value

gi|56407906|ref|NT_033779.3|  Drosophila melanogaster chromosome    720    0.0  
gi|56411841|ref|NT_033777.2|  Drosophila melanogaster chromosome   34.2    2.1  
gi|56411836|ref|NC_004354.2|  Drosophila melanogaster chromosome   32.2    8.2  
gi|56411837|ref|NT_037436.2|  Drosophila melanogaster chromosome   32.2    8.2  
gi|56411835|ref|NC_004353.2|  Drosophila melanogaster chromosome   32.2    8.2  

ALIGNMENTS

>gi|56407906|ref|NT_033779.3| Drosophila melanogaster chromosome 2L, complete sequence
          Length=22407834

 Features in this part of subject sequence:
   CG16870-PA

 Score =  720 bits (363),  Expect = 0.0
 Identities = 363/363 (100%), Gaps = 0/363 (0%)
 Strand=Plus/Plus

Query  1         ATGGCGACCCATAATGTCCATTCCTGCGAATTCGAGGTATTTGGCCGCGTGCAGGGCGTC  60
                 ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
Sbjct  13912201  ATGGCGACCCATAATGTCCATTCCTGCGAATTCGAGGTATTTGGCCGCGTGCAGGGCGTC  13912260
...
Query  361       TAG  363
                 |||
Sbjct  13912561  TAG  13912563


 Features in this part of subject sequence:
   CG12317-PA, isoform A
   CG12317-PB, isoform B

 Score = 34.2 bits (17),  Expect = 2.1
 Identities = 17/17 (100%), Gaps = 0/17 (0%)
 Strand=Plus/Plus

Query  334       AGTAGTTCCAGCCACCA  350
                 |||||||||||||||||
Sbjct  12053694  AGTAGTTCCAGCCACCA  12053710

...
>gi|56411841|ref|NT_033777.2| Drosophila melanogaster chromosome 3R, complete sequence
          Length=27905053

 Features flanking this part of subject sequence:
   5680 bp at 5' side: CG7847-PB, isoform B
   14038 bp at 3' side: CG14316-PA
...
  Database: NCBI Chromosome Sequences
    Posted date:  Jun 3, 2005  8:21 AM
  Number of letters in database: 118377120
  Number of sequences in database:  7
...
 */
package apollo.dataadapter.analysis;

import java.io.*;
import java.util.*;
import java.lang.Exception;

import apollo.datamodel.*;
import apollo.analysis.filter.AnalysisInput;

import org.apache.log4j.*;

public class BlastParser extends AlignmentParser {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BlastParser.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  boolean ncbi_version;
  boolean queryIsGenomic = true;
  // I added the ability to parse output from IU's FlyBlast server--but they're
  // about to change the format so it'll probably break again.
  // Currently not really used, since they're also going to stop providing
  // BLAST against scaffolds.
  boolean isFlyblast = false;
  // If we have a chromosome region loaded, and we're overlaying NCBI blast hits
  // against the NCBI chromosome sequences, then only save hits against the current
  // chromosome (of course, some of them may be outside of the current region).
  String currentChrom = null;

  Hashtable hit_hash = new Hashtable ();

  public String load (CurationSet curation,
      boolean new_curation,
      InputStream data_stream,
      AnalysisInput input) {
    queryIsGenomic = input.queryIsGenomic();
    initLoad(curation, new_curation, data_stream, input);

    if (parsed) {
      /* WU NEW VERSION
	 BLASTN 2.0MP-WashU [09-Nov-2000] [linux-i686 19:13:41 11-Nov-2000]

	 Copyright (C) 1996-2000 Washington University, Saint Louis, Missouri USA.
	 All Rights Reserved.

	 Reference:  Gish, W. (1996-2000) http://blast.wustl.edu
       */

      /* NCBI OLD VERSION (TREAT AS WU)
	 BLASTN 1.4.9MP [26-March-1996] [Build 17:04:13 Jun 16 1998]

	 Reference:  Altschul, Stephen F., Warren Gish, Webb Miller, Eugene W. Myers,
	 and David J. Lipman (1990).  Basic local alignment search tool.  J. Mol. Biol.
	 215:403-10.
       */

      /* NCBI NEW VERSION
	 BLASTN 2.2.1 [Apr-13-2001]

	 Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaeffer, 
	 Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman (1997), 
	 "Gapped BLAST and PSI-BLAST: a new generation of protein database search
	 programs",  Nucleic Acids Res. 25:3389-3402.
       */
      try {
        // The logic here is that if it says "wash", then it's NOT ncbi,
        // and if it says "build", then it's treated as NOT ncbi.
        // So it's ncbi if it doesn't say wash *and* it doesn't say build.
        ncbi_version = (version.toLowerCase().indexOf ("wash") < 0 &&
            version.toLowerCase().indexOf ("build") < 0);

        /*
	  Not only are there formating differences between NCBI and WU,
	  but there are also some pretty basic differences on what gets
	  reversed and what doesn't. They differ in how strandedness
	  is handled. For BLASTN (all that I've looked at so far) NCBI
	  chooses to reverse the subject strand for hits on the minus
	  strand and WU reverses the query strand. 1
         */
        if (ncbi_version) {
          parseNCBI (curation, forward_analysis, reverse_analysis);
        }
        else {
          parseWU (curation, forward_analysis, reverse_analysis);
        }
        data.close();
      }
      catch (Exception e) {
        logger.error (e.getMessage(), e);
        parsed = false;
      }
    }
    return (parsed ? getAnalysisType() : null);
  }

  public String getProgram () {
    return program;
  }

  public String getVersion () {
    return version;
  }

  public boolean recognizedInput() {
    int tries = 0;
    //let's try to find the line that contains "*BLAST*" 
    try {
      readLine();
      while (line != null && !parsed && tries < 10) {
        if (line.startsWith("<blast_form.map>"))
          isFlyblast = true;

        int index = line.indexOf (' ');
        parsed = (index >= 0 && (line.indexOf ("BLAST") >= 0));
        if (!parsed) {
          tries++;
          readLine();
        }
        else {  // found the BLAST line
          program = line.substring (line.indexOf("BLAST"), index).toLowerCase();
          // Should we figure out from this whether it's flyblast (version will
          // have a * at the end)?
          version = line.substring (++index);
          logger.debug("program = " + program + ", version = " + version);
          // Mark where we were in the data stream so we can rewind after getting the query,
          // because in flyblastn output, database comes BEFORE query
          data.mark(9999999);
          SequenceI focal_seq = getQuerySequence (curation);
          // this will need to change once code is added to make both
          // alternatives possible
          if (curation.getRefSequence() == null) {
            logger.error("Genomic ref sequence was empty--using query sequence " + focal_seq.getName() + " as genomic ref seq.");
            curation.setRefSequence(focal_seq);
          }
          // In flyblastn output, database comes BEFORE query
          data.reset();
          database = grabDatabase ();
          logger.debug("database = " + database);
        }
      }
    }
    catch (Exception e) {
      logger.error("Exception while trying to parse blast output: " + e, e);
    }

    if (!parsed) {
      logger.warn("Unable to parse purported BLAST input--maybe it's a new BLAST format I don't recognize? " +
          "tries = " + tries + ", last line read: " + line);
    }

    return parsed;
  }

  private void parseWU (CurationSet curation,
      FeatureSetI forward_analysis,
      FeatureSetI reverse_analysis) 
  throws Exception {

    logger.info("Parsing WU-blast or old NCBI blast (version 1) output.");
    readLine();
    //    boolean none = false;
    boolean found_hit = false;
    FeatureSetI query_hit_minus = null;
    FeatureSetI query_hit_plus = null;
    FeaturePairI span = null;

    //    while (line != null && ! none) {
    while (line != null) {
      line = line.trim();

      if (line.indexOf ("*** NONE **") != -1) {
        //	none = true;
        logger.info("No hits!");
        return;
      }
      else if (line.length() > 0 && line.charAt(0) == '>') {
        recordHit (query_hit_plus, query_hit_minus, 
            span, forward_analysis, reverse_analysis);
        grabHeader (hit_hash);

        span = null;
        found_hit = false;

        grabLength (hit_hash);

        if (program.equals ("BLASTP")) {
          SequenceI aligned_seq 
          = initSequence (curation,
              (String) hit_hash.get("header"),
              Integer.parseInt((String) hit_hash.get("length")));
          query_hit_plus = initAlignment (aligned_seq, 1);
          query_hit_minus = initAlignment (aligned_seq, -1);
          found_hit = true;
        }
      }
      else if (line.startsWith ("Plus Strand")
          || line.startsWith ("Minus Strand")) {
        recordHit (query_hit_plus, query_hit_minus, 
            span, forward_analysis, reverse_analysis);
        SequenceI aligned_seq 
        = initSequence (curation,
            (String) hit_hash.get("header"),
            Integer.parseInt((String) hit_hash.get("length")));
        query_hit_plus = initAlignment (aligned_seq, 1);
        query_hit_minus = initAlignment (aligned_seq, -1);
        span = null;
        found_hit = true;
      }
      else if (found_hit && line.trim().startsWith ("Score ="))	{
        /* record previous line up of the two sequences */
        if (span != null) {
          addSpan (span, query_hit_plus, query_hit_minus);
        }

        span = initAlignPair();

        grabScores (span);

        readLine();

        grabMatches(span);
        grabFrame (span);
      }

      else if (span != null 
          && found_hit
          && line.startsWith ("Query")) {
        grabQueryMatch (span, false);
      }

      else if (span != null 
          && found_hit
          && line.startsWith ("Sbjct")) {
        grabSubjMatch (span, false);
      }
      //      else if (line.startsWith ("Parameters:")) {
      // }
      readLine();

    }
    //    if (!none)
    // Record last hit
    recordHit (query_hit_plus, query_hit_minus, 
        span, forward_analysis, reverse_analysis);

    logger.info("Done parsing WU-BLAST results.  Got " + forward_analysis.size() + " plus-strand hits and " + reverse_analysis.size() + " minus-strand hits.");
  }

  private void parseNCBI (CurationSet curation,
      FeatureSetI forward_analysis,
      FeatureSetI reverse_analysis)
  throws Exception {
    boolean none = false;
    boolean found_hit = false;
    boolean flip = false;
    //    Hashtable hit_hash = new Hashtable ();
    FeatureSetI query_hit_minus = null;
    FeatureSetI query_hit_plus = null;
    FeaturePairI span = null;

    // If we have a fly region loaded, and we're overlaying NCBI blast hits
    // against the NCBI chromosome sequences, then only save hits against the current
    // chromosome (of course, some of them may be outside of the current region).
    currentChrom = curation.getChromosome();
    if (currentChrom == null) {
      logger.warn("current region has no chromosome!");
      currentChrom = "";
    }
    else {
      if (currentChrom.indexOf(":") > 0) {
        if (currentChrom.indexOf("chromosome:") > 0)
          currentChrom = currentChrom.substring(currentChrom.indexOf("chromosome:")+"chromosome:".length());
      }
      logger.info("Currently loaded genomic region is on chromosome " + currentChrom);
    }

    readLine();
    while (line != null && ! none) {
      line = line.trim();

      if (line.indexOf ("*** NONE **") != -1) {
        none = true;
      }

      else if (line.length() > 0 && line.charAt(0) == '>') {
        recordHit (query_hit_plus, query_hit_minus, 
            span, forward_analysis, reverse_analysis);
        // Store the contents of the > line as "header" in hit_hash
        grabHeader (hit_hash);

        span = null;
        found_hit = false;

        grabLength (hit_hash);
        int length = 0;
        try {
          length = Integer.parseInt((String) hit_hash.get("length"));
        } catch (Exception e) {
          logger.error("Error while parsing NCBI blast output: length for hit " + hit_hash.get("header") + " is not an int: " + hit_hash.get("length"));
        }
        String header = (String) hit_hash.get("header");
        // (The commented-out code below was from when we were trying to filter
        // hits by scaffold, before IU changed FlyBlast so it no longer lets you
        // blast against scaffolds.)
        // If hit was to current scaffold, the sequence for it won't get saved because
        // the curation already has a sequence with that name, so change its name by
        // adding ".scaffold" after the scaffold ID.
//      if (!queryIsGenomic && header.startsWith(curation.getName())) {
//      System.out.println("id for hit is same as genomic (" + curation.getName() + "): " + header);
//      header = header.substring(0, header.indexOf(" ")) + ".scaffold" + header.substring(header.indexOf(" "));
//      }

        //        System.out.println("Header for query seq = " + header); // DEL
        SequenceI aligned_seq = initSequence (curation, header, length);
        query_hit_plus = initAlignment (aligned_seq, 1);
        query_hit_minus = initAlignment (aligned_seq, -1);
        found_hit = true;
      }
      else if (found_hit
          && line.trim().startsWith ("Score =")) {
        /* record previous line up of the two sequences */
        if (span != null) {
          addSpan (span, query_hit_plus, query_hit_minus);
        }

        span = initAlignPair();
        grabScores (span);
        readLine();

        grabMatches(span);
        readLine();
        String subj_strand = grabFrame (span);
        flip = subj_strand.equals ("Minus");
      }
      else if (span != null 
          && found_hit
          && line.startsWith ("Query")) {
        if (queryIsGenomic)
          grabQueryMatch (span, flip);
        else
          grabSubjMatch (span, flip);
      }
      else if (span != null 
          && found_hit
          && line.startsWith ("Sbjct")) {
        if (queryIsGenomic)
          grabSubjMatch (span, flip);
        else 
          grabQueryMatch (span, flip);
      }
      readLine();
    }
    // Record last hit
    if (!none) {
      recordHit (query_hit_plus, query_hit_minus, 
          span, forward_analysis, reverse_analysis);
    }

    logger.info("Done parsing NCBI BLAST results.  Got " + forward_analysis.size() + " plus-strand hits and " + reverse_analysis.size() + " minus-strand hits."); // DEL
    System.gc ();
  }

//private void swapQueryAndSubject(FeaturePairI fs) {
//if (fs == null)
//return;

//SeqFeatureI temp = fs.getQueryFeature();
//System.out.println("Swapping query " + temp.getName() + " and subject " + fs.getHitFeature().getName()); // DEL
//fs.setQueryFeature(fs.getHitFeature());
//fs.setHitFeature(temp);
//}

  private SequenceI getQuerySequence (CurationSet curation)
  throws Exception {
    StringBuffer query_name = new StringBuffer();

    query_name.append (findKeyValue ("Query="));
    int length = 0;

    readLine();
    while (line != null && length == 0) {
      line = line.trim();
      String value = null;
      if (line.contains("Length=")) {
        value = line.substring(line.lastIndexOf('=') + 1);
      }
      else if (line.contains(" letters")) {
        value = line.substring(0, line.indexOf(" letters"));
        value = value.replace (',', ' ');
        value = value.replace ('(', ' ');
      }
      else {
        query_name.append (" " + line);
        readLine();
      }
      if (value != null) {
        value = value.trim();
        StringTokenizer tokens = new StringTokenizer(value);
        while (tokens.hasMoreElements()) {
          String numb = tokens.nextToken();
          length = (length * 1000) + Integer.parseInt(numb);
        }
      }
    }
    SequenceI focal_seq = null;

    if (queryIsGenomic) {
      focal_seq = curation.getRefSequence();
      if (focal_seq == null) {
        focal_seq = initSequence(curation, query_name.toString(), length);
        focal_seq.setLength(length);  // Need?
      }
      logger.debug ("Query is genomic seq: " + focal_seq.getName() +
          "--length is " + length);
    }
    else {  // Genomic is SUBJECT--this other sequence is the query
      focal_seq = initSequence(curation, query_name.toString(), length);
      if (focal_seq == null) {
        logger.warn("Hey, after initSequence, focal_seq is still null!");
        //        focal_seq = new Sequence("Acyp", "ATGGCGACCCATAATGTCCATTCCTGCGAATTCGAGGTATTTGGCCGCGTGCAGGGCGTCAACTTCCGGCGACACGCCCTGCGAAAGGCCAAGACACTGGGTCTTCGCGGCTGGTGCATGAACTCCAGCAGGGGCACCGTGAAGGGTTACATCGAAGGTCGTCCGGCCGAGATGGATGTGATGAAGGAGTGGCTCAGGACGACGGGCAGTCCGCTGTCCAGCATCGAGAAGGTGGAGTTCAGTTCTCAGCGTGAGCGCGATCGCTATGGCTATGCCAACTTTCATATCAAGCCCGATCCGCACGAGAATCGCCCAGTTCATGAAGGATTGGGCAGTAGTTCCAGCCACCATGATAGCAATTAG");
        //        curation.addSequence(focal_seq);
      }
      hit_hash.put("query_header", query_name.toString());
      String query_id = query_name.toString();
      if (query_id.indexOf(" ") > 0) {
        query_id = query_id.substring(0, query_id.indexOf(" "));
        if (query_id.length() > 12 && query_id.indexOf("|") > 2)
          query_id = query_id.substring(0, query_id.indexOf("|"));
      }
      hit_hash.put("query_id", query_id.toString());
//    System.out.println("hit_hash(query_header) = " + hit_hash.get("query_header") + ", query_id = " + query_id); // DEL
      logger.debug ("Genomic is not query.  Query is " + focal_seq.getName() +
          "--length is " + focal_seq.getLength());
    }
    return focal_seq;
  }

  private String findKeyValue (String goal) 
  throws Exception {
    String keyword = "";
    String value = "";
    boolean value_found = false;

    readLine();
    while (line != null && !value_found) {
      logger.trace ("Looking for " + goal + ", line = " + line);
      if (line.startsWith(goal)) {
        value = line.substring (goal.length());
        value = value.trim();
        value_found = true;
      }
      // for IU flyblast output--e.g. *Database:* dmel-scaffold
      else if (line.startsWith("*" + goal)) {
        line = line.substring(1);
        value = line.substring(goal.length());
        logger.trace ("line = " + line + ", value = " + value);
        if (value.startsWith("*"))
          value = value.substring(1);
        value = value.trim();
        value_found = true;
      }
      else {
        readLine();
      }
    }
    return value;
  }

  /** Database: NCBI Chromosome Sequences 
      4271 sequences; 17,691,404,557 total letters */
  private String grabDatabase () 
  throws Exception {
    String db = findKeyValue ("Database:");

    int index = db.lastIndexOf ('/') + 1;
    if (index > 0 && index < db.length())
      db = db.substring (index);

    return db;
  }

  private void grabLength (Hashtable hit_hash) 
  throws Exception {
    String value;
    boolean value_found = false;
    StringTokenizer tokens;
    int MAX_DESCRIP_LENGTH = 1000;

    readLine();
    while ( ! value_found && line != null) {
      line = line.trim();
      if (line.startsWith("Length")) {
        int equalpos = line.indexOf("=");
        value = line.substring(equalpos+1);
        value.trim();
        // Apparently the length can sometimes include commas--translate it to a regular integer
        value = value.replace (',', ' ');
        tokens = new StringTokenizer(value);
        int length = 0;
        while (tokens.hasMoreElements()) {
          String numb = tokens.nextToken();
          length = (length * 1000) + Integer.parseInt(numb);
        }
        value = String.valueOf (length);
        hit_hash.put ("length", value);
        value_found = true;
      }
      else {
        value = (String) hit_hash.get ("header");
        if (value == null)
          value = line;
        else {
          // Don't let description line get too long.
          // --NH 2/11 (was 02 the year??)
          if (value.length() < MAX_DESCRIP_LENGTH)
            value = value.concat (" " + line);
        }
        hit_hash.put ("header", value);
        logger.trace("grabLength: hit_hash.put(header, " + value + ")"); // DEL
        readLine();
      }
    }
  }

  /**  Score =  720 bits (363),  Expect = 0.0 */
  private void grabScores (FeaturePairI span)
  throws Exception {
    line.trim();
    String score;
    String expect;
    String prob;
    String bits;

    try {
      // if (ncbi_version) new
      if (line.indexOf("bits (") >= 0) {
        // NCBI
        // Score = 36.3 bits (207), Expect = 0.016
        bits = line.substring (line.indexOf("Score = ") +
            "Score = ".length(),
            line.indexOf ("bits ("));
        bits.trim();
        score = line.substring (line.indexOf ("bits (") + "bits (".length(),
            line.indexOf(')'));
        score.trim();
        expect = line.substring (line.indexOf ("Expect"),
            line.length());
        int lindex = expect.indexOf(" =");
        expect = expect.substring(lindex+3, expect.length());
        if (expect.indexOf(" ") > 0)
          expect = expect.substring(0, expect.indexOf(" "));
        expect = getDoubleValue(expect);
      }
      else {
        // WU
        // Score = 5219 (789.1 bits), Expect = 7.3e-228, P = 7.3e-228
        int first_separator = line.indexOf(',');
        String score_line = line.substring (0, first_separator);
        first_separator++;
        int next_separator = line.indexOf(',', first_separator);
        String expect_line = line.substring (first_separator, next_separator);
        next_separator++;
        int last_separator = line.indexOf(',', next_separator);
        String prob_line = (last_separator < 0 ?
            line.substring(next_separator) :
              line.substring(next_separator, last_separator));
        score = score_line.substring ("Score =".length(),
            score_line.indexOf('('));
        score = score.trim();

        bits = line.substring (line.indexOf (" (") + " (".length(),
            line.indexOf(" bits)"));
        expect = expect_line.substring (expect_line.indexOf ("Expect = ") +
            "Expect = ".length());
        expect = expect.trim();

        if (prob_line.indexOf("P =") >= 0) {
          prob = prob_line.substring (prob_line.indexOf (" = ") +
              " = ".length());
          prob = prob.trim();
          span.addScore ("probability", (Double.valueOf(prob)).doubleValue());
        }
      }
      span.setScore ((Double.valueOf(score)).doubleValue());
      span.addScore ("expect", (Double.valueOf(expect)).doubleValue());
      span.addScore ("bits", (Double.valueOf(bits)).doubleValue());
    } 
    catch (RuntimeException ex) {
      throw new Exception("BlastParser: error parsing score from " + line + ": " +
          ex.getMessage());//,ex);
    }
  }

  private String getDoubleValue (String str) {
    String d_str = str.trim();
    try {
      double value = (Double.valueOf(str)).doubleValue();
    }
    catch (Exception e) {
      logger.error ("BlastParser: unable to convert " + str +
          " to double--using 1.0" + str + 
          " instead", e);
      d_str = "1.0" + str;
    }
    return d_str;
  }

  private void grabMatches (FeaturePairI span) 
  throws Exception {
    int index;
    String match_string;

    try {
      if (line != null) {
        line = line.trim();
        index = line.indexOf ("/");
        match_string = (line.substring ("Identities = ".length(),
            index));
        int matches = (program.toLowerCase().endsWith("blastx") ?
            Integer.parseInt(match_string) * 3 :
              Integer.parseInt(match_string));
        span.addScore ("matches", matches);
      }
    } 
    catch (RuntimeException ex) {
      logger.error(ex.getMessage(), ex);
      throw new Exception("BlastParser: error parsing identities from "
          + line + " " +
          ex.getMessage());//,ex); // doesnt compile
    }
  }

  private String grabFrame (FeaturePairI span)
  throws Exception {
    /* 
       so many formats so little time.
       BLASTN
       Identities = 685/1199 (57%), Positives = 685/1199 (57%), Strand = Plus / Plus

       BLASTX
       Identities = 118/196 (60%), Positives = 152/196 (77%), Frame = -2

       TBLASTX
       Identities = 87/197 (44%), Positives = 104/197 (52%), Frame = +1 / -3

       BLASTP
       Identities = 87/197 (44%), Positives = 104/197 (52%)

       NCBI BLASTN (some older format)
       Strand = Plus / Plus

       NCBI BLASTN v2.2.11
       Strand=Plus/Plus
     */

    int index;
    String keyword = "";
    String subj = "Plus";

    try {
      if (line != null && !(line.equals(""))) {
        line = line.trim();
        if ((index = line.lastIndexOf ("Frame = ")) != -1) {
          keyword = line.substring (index);
          index = keyword.indexOf (" / ");
          if (index != -1) {
            subj = keyword.substring (index + " / ".length());
          }
          else {
            subj = keyword.substring ("Frame = ".length());
          }
          span.getHitFeature().addProperty ("frame", subj);
        }
        else if ((index = line.lastIndexOf("Strand")) >= 0) {
          index = line.indexOf ("/");
          subj = line.substring (index + "/".length());
          subj.trim();
        }
        else if ( ! program.equals ("BLASTP") ) {
          throw new Exception("Error parsing strand from " + line);
        }
      }
    }
    catch( Exception ex ) {
      logger.error(ex.getMessage(), ex);
      throw new Exception("BlastParser: error parsing strand/frame in "
          + line + " " +
          ex.getMessage());//, ex); doesnt compile
    }
    return subj;
  }

  private void addSpan (FeaturePairI span, 
      FeatureSetI query_hit_plus, 
      FeatureSetI query_hit_minus) {
    FeatureSetI hit;
    logger.trace("Adding span " + span);  // DEL

    if (span != null && query_hit_plus != null && query_hit_minus != null) {
      hit = (span.getStrand() == 1 ? query_hit_plus : query_hit_minus);
      addAlignPair (span, hit);
      rollCigar (span);

      double matches = span.getScore ("matches"); 
      if (matches > 0) {
        double identity = (matches * 100.0) / (double) span.length();
        span.addScore ("identity", identity);
      }
    }
  }

  private void grabQueryMatch (FeaturePairI span, boolean flip)
  throws Exception {
    StringTokenizer tokens;
    String queryOrSubject = "Query";
    if (!queryIsGenomic)
      queryOrSubject = "Sbjct";

    if (line.indexOf(queryOrSubject+":") >= 0)
      tokens = new StringTokenizer(line.substring(6));
    else if (line.indexOf("*"+queryOrSubject+"=") >= 0)  // flyblast
      tokens = new StringTokenizer(line.substring("*Query=* ".length()));
    // Newer versions of NCBI blast don't have a : after Query
    else if (line.indexOf(queryOrSubject) >= 0)
      tokens = new StringTokenizer(line.substring(5));
    else {
      logger.error("couldn't find " + queryOrSubject + " string in supposed query line " + line);
      return;
    }
    String value;
    int start = -1;
    int end = -1;

    value = tokens.nextToken();

    if (!flip)
      start = Integer.parseInt(value);
    else
      end = Integer.parseInt(value);

    coord_seq = (coord_seq + tokens.nextToken());

    if (!tokens.hasMoreElements()) {
      throw new Exception("Error parsing sbjct from "
          + line + " for span " + span.getName());
    }

    value = tokens.nextToken();
    if (!flip)
      end = Integer.parseInt(value);
    else
      start = Integer.parseInt(value);

    // !! If we're layering, it looks like we need to subtract one from start
    // (not sure about end)!  But if we're loading fresh, we don't want to do that!
    // Maybe it's the genomic offset that needs to be adjusted?

    int genomic_offset = input.getOffset();
    SeqFeatureI query_span = span.getQueryFeature();
    if (query_span.getStrand() == 0) {
      int strand = (start < end ? 1 : (start > end ? -1 : 0));
      query_span.setStrand (strand);
      if (!flip) {
        logger.trace("Setting query start to " + start + " + " + genomic_offset); // DEL
        query_span.setStart(start + genomic_offset);
      }
      else
        query_span.setEnd(end + genomic_offset);
    }
    if (!flip)
      query_span.setEnd(end + genomic_offset);
    else
      query_span.setStart(start + genomic_offset);
  }

  private void grabSubjMatch (FeaturePairI span, boolean flip)
  throws Exception {
    StringTokenizer tokens;
    String  queryOrSubject = "Sbjct";
    if (!queryIsGenomic)
      queryOrSubject = "Query";

    if (line.indexOf(queryOrSubject + ":") >= 0)
      tokens = new StringTokenizer(line.substring (6));
    else if (line.indexOf(queryOrSubject) >= 0)
      tokens = new StringTokenizer(line.substring (5));
    else {
      logger.error("couldn't find " + queryOrSubject + " string in supposed subject line " + line);
      return;
    }

    String value;
    int start = -1;
    int end = -1;

    String posStr = tokens.nextToken();

    int pos;

    try {
      pos = Integer.parseInt (posStr);
      value = tokens.nextToken();
    }

    catch (NumberFormatException e) {
      int index = 0;
      while (index < posStr.length() &&
          Character.isDigit(posStr.charAt (index))) {
        index++;
      }
      pos = Integer.parseInt (posStr.substring (0, index));
      value = posStr.substring (index);
    }

    if (!flip)
      start = pos;
    else
      end = pos;

    align_seq = (align_seq + value);

    // Added for debugging.  --NH
    if (!(tokens.hasMoreElements())) {
      throw new Exception("Error parsing sbjct from "
          + line + " for span " + span.getName());
    }

    posStr = tokens.nextToken();
    if (!flip)
      end = Integer.parseInt(posStr);
    else
      start = Integer.parseInt(posStr);

    SeqFeatureI align_span = span.getHitFeature();
    if (align_span.getStrand() == 0) {
      int strand = (start < end ? 1 : (start > end ? -1 : 0));
      align_span.setStrand (strand);
      if (!flip)
        align_span.setStart(start);
      else
        align_span.setEnd(end);
    }
    if (!flip)
      align_span.setEnd(end);
    else
      align_span.setStart(start);
  }

  protected void setHitScore (FeatureSetI hit, FeaturePairI span) {
    double hit_expect = hit.getScore("expect");
    double span_expect = span.getScore ("expect");
    if (hit.size() == 1 || span_expect < hit_expect) {
      hit.addScore ("expect", hit_expect);
      hit.addScore ("probability", span.getScore("probability"));
    }
    if (hit.size() == 1 || span.getScore() > hit.getScore()) {
      hit.setScore (span.getScore());
    }
  }

  private void recordHit (FeatureSetI query_hit_plus,
      FeatureSetI query_hit_minus,
      FeaturePairI span,
      FeatureSetI forward_analysis, 
      FeatureSetI reverse_analysis) {
    if (query_hit_plus != null && query_hit_minus != null) {
      /* record last hit */
      if (span != null) {
        addSpan (span, query_hit_plus, query_hit_minus);
      }

      if (query_hit_plus.size() > 0) {
        // See if this hit is on currently loaded chromosome
        // Sample header:
        // >gi|56407906|ref|NT_033779.3| Drosophila melanogaster chromosome 2L, complete sequence
        if (!queryIsGenomic && (currentChrom != null)) {
          String header = (String) hit_hash.get("header");
          if (header.indexOf("chromosome " + currentChrom) < 0) {
            //              System.out.println("Saving + strand hit to current chrom " + currentChrom + ": " + header);  // DEL
            //          else {
            logger.debug("Skipping hit that does not match current chrom " + currentChrom + ": " + header);
            return;
          }

          // Now that we've determined that this hit corresponds to the current scaffold,
          // change query header to match query seq, not genomic scaffold
          // If this is not the first hit on this chrom, we might have already
          // added the query name to the hit name.
          if (query_hit_plus.getHitSequence().getName().indexOf((String)hit_hash.get("query_id")) < 0) {
            query_hit_plus.getHitSequence().setName((String)hit_hash.get("query_id") + " hit to " + query_hit_plus.getName());
            //            System.out.println("Now hit name is " + query_hit_plus.getHitSequence().getName()); // DEL
          }
          //        query_hit_plus.getHitFeature().setHitId((String)hit_hash.get("query_id"));
        }
        logger.debug("recordHit: adding plus strand hit " + query_hit_plus);
        forward_analysis.addFeature (query_hit_plus, true);
      }

      if (query_hit_minus.size() > 0) {
        if (!queryIsGenomic && (currentChrom != null)) {
          String header = (String) hit_hash.get("header");
          if (header.indexOf("chromosome " + currentChrom) < 0)
//          System.out.println("Saving - strand hit to current chrom " + currentChrom + ": " + header);  // DEL
//          else {
//          System.out.println("Skipping minus strand hit that does not match current chrom " + currentChrom + ": " + query_hit_minus.getName()); // DEL
            return;

          // Now that we've determined that this hit corresponds to the current chrom
          // change query header to match query seq, not genomic chunk
          // If this is not the first hit on this chrom, we might have already
          // added the query name to the hit name.
          if (query_hit_minus.getHitSequence().getName().indexOf((String)hit_hash.get("query_id")) < 0) {
            query_hit_minus.getHitSequence().setName((String)hit_hash.get("query_id") + " hit to " + query_hit_minus.getName());
            //        query_hit_minus.getHitFeature().setId((String)hit_hash.get("query_id"));
          }
//        System.out.println("recordHit: adding minus strand hit " + query_hit_minus);  // DEL
        }
        reverse_analysis.addFeature (query_hit_minus, true);
      }
//    System.gc ();  // Why are we doing it here?  Too slow!  Do once at the end!
    }
  }

  private void grabHeader (Hashtable hit_hash) {
    //    System.out.println("grabHeader: hit_hash.put(header, " + line + ")"); // DEL
    hit_hash.put ("header", line.substring(1));
  }	

}
