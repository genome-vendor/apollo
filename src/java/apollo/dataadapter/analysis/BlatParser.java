package apollo.dataadapter.analysis;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

import apollo.datamodel.*;
import apollo.analysis.filter.Compress;
import apollo.analysis.filter.AnalysisInput;

import org.apache.log4j.*;

public class BlatParser extends AlignmentParser {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BlatParser.class);
  
  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  //blat variables to parse
  String match, misMatch, repMatch, numN, queryGapCount;
  String queryGapBases, targetGapCount, targetGapBases, strand;
  String queryName, querySize, queryStart, queryEnd, targetName, targetSize;
  String targetStart, targetEnd, blockCount, blockSizes, queryStarts,targetStarts;
  Vector splitBlockSizes, splitQueryStarts, splitTargetStarts;

  //seq feature objects 
  Vector all_features;

  public String load (CurationSet curation,
		      boolean new_curation,
		      InputStream data_stream,
		      AnalysisInput input) {
    initLoad(curation, new_curation, data_stream, input);
  
    if (parsed) {
      // if this is indeed a blat file then eat-it-up
      logger.info ("Parsing blat analysis " + 
                   " type is " + input.getType() +
                   " analysis type is " + getAnalysisType());
      try {
	while (line != null){
	  FeatureSetI hit = createFP();
	  if (hit.getStrand() == 1)
	    forward_analysis.addFeature (hit, true);
	  else
	    reverse_analysis.addFeature (hit, true);
	  
	  logger.info ("Hit size is " + hit.size());
	  
	  readLine();
	  if (line != null) {
	    line = line.trim();
	    grabGenes();
	  }
	}
	data.close();
      }
      catch (Exception e) {
        logger.error(e.getMessage(), e);
        parsed = false;
      }
    }
    else
      logger.error("Failed to parse blat data");

    return (parsed ? getAnalysisType() : null);
  }

  public String getProgram() {
    return "blat";
  }

  public boolean recognizedInput() {
    /* BLAT has absolutely nothing in the way of a header,
       (yuck), no version, no preamble, nothing to tell
       you what sort of input file it is. Sooo, just try
       parsing the first line and see if it works or not
    */
      /* 5/7/05 That no longer seems to be the case--the blat example I have starts
	 with the line "psLayout version 3" and then a blank line. */
    try {
      readLine();
      if (line == null)
	  return false;

      if (line.startsWith("psLayout")) {
	  readLine();
	  if (line == null)
	      return false;
	  if (line.length() < 2)
	      readLine();
	  if (line == null)
	      return false;
	  // Skip header line:
	  // match	mis- 	rep. 	N's	Q gap	Q gap	T gap	T gap	strand	Q        	Q   	Q    	Q  	T        	T   	T    	T  	block	blockSizes 	qStarts	 tStarts
	  if (line.startsWith("match"))
	      readLine();
	  if (line == null)
	      return false;
	  line = line.trim();
	  // Skip next header line:
	  //      match	match	   	count	bases	count	bases	      	name     	size	start	end	name     	size	start	end	count
	  if (line.startsWith("match"))
	      readLine();
	  if (line == null)
	      return false;
	  // Skip --------------- line
	  if (line.startsWith("---"))
	      readLine();
	  if (line == null)
	      return false;
      }
      // Why are we checking parsed here?  We didn't try to parse anything yet, did we??
//      if (line != null && parsed) {
      if (line != null) {
	line = line.trim();
	grabGenes();
      }
      else
        logger.debug("Failed to parse first line of blat data: " + line);
    }
    catch (Exception e) {}
    return parsed;
  }

  public FeatureSetI createFP () {
    //generate exon pairs of query and target exons
    int blocks = splitBlockSizes.size();
    logger.debug(strand);
    FeatureSetI hit = new FeatureSet();
    SequenceI query_seq = new Sequence (queryName, "");
    query_seq.setLength(Integer.parseInt(querySize));
    SequenceI target_seq = new Sequence (targetName, "");
    target_seq.setLength(Integer.parseInt(targetSize));
    hit.setRefSequence(query_seq);
    hit.setHitSequence(target_seq);
    int query_strand = (strand.equals("+") ? 1 : -1);
    hit.setStrand(query_strand);
    hit.setScore (100);
    hit.setName(queryName);
    for (int index = 0; index < blocks; index++) {
      //------------------------------------------------------------//
      //------------------Set query range---------------------------//
	    
      int qLow, qHigh;
	    
      //use strand information here
      //because this is blat, the output is similar to the
      //sim4 output.  
      //when query is forward, everything is as expected.  
	    
      if (query_strand == 1) {
	qLow = 
	  Integer.parseInt((String)splitQueryStarts.elementAt(index));
	//find end point by adding blockSize to qLow 
	qHigh = 
	  qLow  + Integer.parseInt((String)splitBlockSizes.elementAt(index));
      }
      //when query is reverse, you have to adjust the coordinates
      //essentially, the minus strand blockSizes and qStarts are what
      //you would get if you revcomped the query.  But, the qStart and
      //qEnd are non-reveresed
      else {
	qHigh = 
	  Integer.parseInt(querySize) -
	  Integer.parseInt((String)splitQueryStarts.elementAt(index));
		
	qLow  =  
	  Integer.parseInt(querySize) -
	  Integer.parseInt((String)splitQueryStarts.elementAt(index)) -
	  Integer.parseInt((String)splitBlockSizes.elementAt(index));
      }
		    
      //add one to qLow to make this base rather than
      //interbase coordinates
      qLow ++;
	    
      //make seqFeature 
      SeqFeatureI query = new SeqFeature();
      query.setStrand(query_strand);
      query.setLow(qLow);
      //query.setStart(qLow);
      query.setHigh(qHigh);
      //query.setEnd(qHigh);
      logger.info("STRAND" + strand + "  LOWER" + qLow +  "--");
      logger.info("HIGHER" + qHigh);
      logger.info("SETTING Start as" + qLow + "  End" + qHigh +  "--");
      query.setRefSequence(query_seq);
      query.setName(queryName);
      //------------------------------------------------------------//
      //------------------Set target range---------------------------//
      int tLow = 
	Integer.parseInt((String)splitTargetStarts.elementAt(index));
      int tHigh = 
	tLow + Integer.parseInt((String)splitBlockSizes.elementAt(index));
      tLow ++;

      //make seqFeature 
      SeqFeatureI target = new SeqFeature();
      target.setStrand(1);
      //target.setStart(tLow);
      target.setLow(tLow);
      //target.setEnd(tHigh);
      target.setHigh(tHigh);
      logger.info("TARGET" + strand + "  LOWER" + tLow +  "--");
      logger.info("HIGHER" + tHigh);
      logger.info("SETTING Start as" + tLow + "  End" + tHigh +  "--");
      target.setRefSequence(target_seq);


      //-------------------------------------------------------------
      //Put seqFeatures into FeaturePair object
      FeaturePairI fp = new FeaturePair(query, target);
      fp.setScore (100);
      hit.addFeature(fp);
      logger.info("Added feature " + fp.getName());
      logger.info(tLow + "-" + tHigh + " (" +qLow + "-" + qHigh + ")");
      logger.info("made seqfeature object for each pair");
    }
    return hit;
  }

  public void errorCheck () {
    logger.info(blockSizes + "-->" + splitBlockSizes.lastElement());
    logger.info(queryStarts + "-->" + splitQueryStarts.lastElement());
    logger.info(targetStarts + "--> " + splitTargetStarts.lastElement());
    logger.info("THIS MANY LINES PARSED: " + line_number);
  }

  private void grabGenes () {
    StringTokenizer tokens = new StringTokenizer (line);
    int count = tokens.countTokens();
    boolean okay = true;
    
    match = ((tokens.hasMoreTokens()) ? tokens.nextToken() : "");
    okay &= !match.equals("");
    
    misMatch = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !misMatch.equals("");
    
    repMatch = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !repMatch.equals("");
    
    numN = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !numN.equals("");
    
    queryGapCount = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryGapCount.equals("");
    
    queryGapBases = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryGapBases.equals("");
    
    targetGapCount = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetGapCount.equals("");
    
    targetGapBases = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetGapBases.equals("");
    
    strand = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !strand.equals("");
    
    queryName = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryName.equals("");
    
    querySize = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !querySize.equals("");
    
    queryStart = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryStart.equals("");
    
    queryEnd = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryEnd.equals("");
    
    targetName = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetName.equals("");
    
    targetSize = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetSize.equals("");
    
    targetStart = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetStart.equals("");
    
    targetEnd = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetEnd.equals("");
    
    blockCount = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !blockCount.equals("");
    
    blockSizes = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !blockSizes.equals("");	
    
    queryStarts = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !queryStarts.equals("");
    
    targetStarts = (tokens.hasMoreTokens()) ? tokens.nextToken() : "";
    okay &= !targetStarts.equals("");
    
    if (!okay) {
      parsed = false;
      logger.error ("Could not parse " + line);
    }
    else {
      //---------------------------------------------------------------
      //split up the last three comma delimited fields
      
      //tokenize line using comma delimiter
      StringTokenizer tokensBS = new StringTokenizer (blockSizes, ",");
      
      //make vector to hold data
      splitBlockSizes = new Vector();
      
      //Fill up vector with each element in
      //the token list
      while (tokensBS.hasMoreTokens()) 
	splitBlockSizes.add(tokensBS.nextToken());
      //---------------------------------------------------------------
      //Do the same with the QS field
      StringTokenizer tokensQS = new StringTokenizer (queryStarts, ",");
      splitQueryStarts = new Vector();
      while (tokensQS.hasMoreTokens()) 
	splitQueryStarts.add(tokensQS.nextToken());
      //---------------------------------------------------------------
      //Do the same with the SS field
      StringTokenizer tokensSS = new StringTokenizer (targetStarts, ",");
      splitTargetStarts = new Vector();
      while (tokensSS.hasMoreTokens()) 
	splitTargetStarts.add(tokensSS.nextToken());
      //---------------------------------------------------------------
    }
  } 
}
