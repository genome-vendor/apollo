package apollo.gui.detailviewers.blixem;

import java.io.*;
import java.util.*;

import apollo.datamodel.*;

import apollo.main.Apollo;
import apollo.seq.io.FastaFile;
import apollo.config.Config;

import org.apache.log4j.*;

public class BlixemRunner {
  
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(BlixemRunner.class);

  public final static int DNA = 1;
  public final static int PROTEIN = 2;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private int genomicPlusMinus = 10000;
  private CurationSet curationSet;
  private Vector typeStrings;
  private int centre;
  private int resType;
  private boolean is_tblastx = false;

  public BlixemRunner(CurationSet set, Vector types, int centre, 
                      int resType) {
    this.curationSet = set;
    this.typeStrings = types;
    this.centre = centre;
    this.resType = resType;
  }

  public void setGenomicPlusMinus(int gpm) {
    genomicPlusMinus = gpm;
  }

  public void run() {
    Apollo.setLog4JDiagnosticContext();
    try {

      // Write temporary files (genomic sequence, features and genes)
      File exblxFile = File.createTempFile("blx",".exblx");
      File fastaFile = File.createTempFile("blx",".fa");
  
      // Delete temp file when apollo exits.
      exblxFile.deleteOnExit();
      fastaFile.deleteOnExit();
    
      FileOutputStream exblxFos = new FileOutputStream(exblxFile);
      FileOutputStream fastaFos = new FileOutputStream(fastaFile);
  
      DataOutputStream exblxDos = new DataOutputStream(exblxFos);
      DataOutputStream fastaDos = new DataOutputStream(fastaFos);

      SequenceI seq = curationSet.getRefSequence();
      int min = (centre - genomicPlusMinus);
      int max = (centre + genomicPlusMinus);
 
      if (seq.usesGenomicCoords()) {
        RangeI range = seq.getRange();
        if (min < range.getLow()) {
          min = range.getLow();
        }
        if (max > range.getHigh()) {
          max = range.getHigh();
        }
      } else {
        if (min < 1) {
          min = 1;
        }
        if (max > seq.getLength()) {
          max = seq.getLength();
        }
      }

      exblxDos.writeBytes("# exblx\n");
      if (resType == DNA) {
	for (int i = 0; i < typeStrings.size(); i++) {
	  String type = (String)typeStrings.elementAt(i);
	  if (type.indexOf("tblastx") >= 0)
	    is_tblastx = true;
	}
	// Unfortunately, if ANY of the result types are tblastx, we'd better use tblastx
	// rather than blastn as the type.  But will that screw up blastn results?
	if (is_tblastx)
	  exblxDos.writeBytes("# tblastx\n");
	else
	  exblxDos.writeBytes("# blastN\n");
      } else {
        exblxDos.writeBytes("# blastx\n");
      }
      StringBuffer exblxOut = new StringBuffer();
      StringBuffer seqblOut = new StringBuffer();

      StringBuffer [] strs = new StringBuffer[2];
      strs[0] = exblxOut;
      strs[1] = seqblOut;

      generateSeqblExblxData(curationSet.getAnnots(),min,max,strs);
      if (strs[1].length() > 0) {
        logger.info("Shouldn't get seqbl data in annotations\n");
      }
      exblxDos.writeBytes(strs[0].toString());

      strs[0].setLength(0);
      strs[1].setLength(0);
      logger.debug("Calling generateSeqblExblxData on " + curationSet.getResults().size() + " total results, range " + min + "-" + max + ", typeStrings = " + typeStrings + ", is_tblastx = " + is_tblastx + ", resType = " + resType + ", strs = " + strs);
      generateSeqblExblxData(curationSet.getResults(),min,max,strs);

      exblxDos.writeBytes(strs[0].toString());
      logger.debug("Wrote " + strs[0].length() + " bytes to " + exblxFile);
      exblxDos.close();

      File seqblFile = null;
      if (strs[1].length() > 0) {
        seqblFile = File.createTempFile("blx",".seqbl");
        seqblFile.deleteOnExit();
        FileOutputStream seqblFos = new FileOutputStream(seqblFile);
        DataOutputStream seqblDos = new DataOutputStream(seqblFos);
        seqblDos.writeBytes("# seqbl\n");
        if (resType == DNA) {
	  if (is_tblastx)
	    seqblDos.writeBytes("# tblastx\n");
	  else
	    seqblDos.writeBytes("# blastN\n");
        } else {
          seqblDos.writeBytes("# blastx\n");
        }
        seqblDos.writeBytes(strs[1].toString());
	logger.debug("Wrote " + strs[1].length() + " bytes to " + seqblFile);
        seqblDos.close();
      }
      //      else if (Config.getGeneDefinition() == Config.FLY_GENEDEF) {
      //	logger.error("No sequences to show in Blixem!");
      //	return;
      //      }

      fastaDos.writeBytes(FastaFile.format(">\n", seq.getResidues(min,max),60));
      fastaDos.close();
      logger.debug("BlixemRunner: writing residues.  min = " + min + " max = " + max);
      String comStr = Config.getBlixemLocation() + " -O " + min + " " + 
	// " -S" + centre + " " +
	((seqblFile == null) ? "" : "-x " + seqblFile.getAbsolutePath() + " ") +
	fastaFile.getAbsolutePath() + " " + 
	exblxFile.getAbsolutePath();
      logger.info("Blixem command string: " + comStr);
      Runtime.getRuntime().exec(comStr);
    } catch (Exception e) {
      logger.error("Failed to run blixem", e);
    }
    Apollo.clearLog4JDiagnosticContext();
}

//# exblx
//# blastN
//-2 (+1) 1938    2344    0       0 dJ591C20.GENSCAN.2i
//99 (+1) 1081    1457    1       377 Em:BG150213.1
  private void generateSeqblExblxData(FeatureSetI set, int min, int max, StringBuffer [] strs) {
    StringBuffer exblxOut = strs[0];
    StringBuffer seqblOut = strs[1];

    for (int i=0; i< set.size(); i++) {
      SeqFeatureI sf = set.getFeatureAt(i);
        
      if (sf.canHaveChildren()) {
        FeatureSetI fs = (FeatureSetI)sf;
        generateSeqblExblxData(fs,min,max,strs);
      } else if (sf instanceof FeaturePair) {

        FeaturePair fp = (FeaturePair)sf;

        // Hacky way to guess hit type (should probably actually set this
        // or put in tiers file)
        boolean isCorrectResType = false;
        SeqFeatureI query = fp.getQueryFeature();
        SeqFeatureI hit   = fp.getHitFeature();

        double qToh = (double)query.length()/(double)hit.length();

        if (qToh > 2.5 && PROTEIN == resType) {
          isCorrectResType = true;
        } else if (qToh < 1.2 && DNA == resType) {
          isCorrectResType = true;
        }

        if (isCorrectResType &&
            typeStrings.contains(sf.getFeatureType()) &&
            (!(sf.getHigh() > max) && !(sf.getLow() < min))) {
          // First number should be score in range 0 to 100
          String frameStr;
          if (resType == PROTEIN) {
            int frame;
            if (fp.getStrand() == 1) {
              frame = (fp.getLow()-min+1)%3;
            } else {
              frame = (max-fp.getHigh()+1)%3;
            }
            if (frame == 0) frame = 3;
            frameStr = (fp.getStrand() == 1 ? ("+" + frame) : "-" + frame);
          } else {
            frameStr = (fp.getStrand() == 1 ? "+1" : "-1");
          }
          StringBuffer out;
          SequenceI hitSeq = fp.getHitFeature().getRefSequence();
	  // for game hits sometimes lack a ref seq, but have alignments
	  // this is a part of game that needs reworking
	  String gameAlignSeqString = null;
	  if (hitSeq == null ||  hitSeq.getLength() == 0) 
	    gameAlignSeqString = fp.getHitFeature().getProperty("subject_alignment");
          if ((hitSeq != null && hitSeq.getLength() > 1) || gameAlignSeqString!=null) {
            logger.debug("For feature " + fp + ", hitSeq = " + hitSeq + "--writing to " + seqblOut);
            out = seqblOut;
          } else {
            logger.info("Hit " + hit);
            logger.info("Hitref " + hit.getRefSequence());
            if (hit.getRefSequence() != null) {
	    hitSeq = curationSet.getSequence(hit.getRefSequence().getName());
            logger.debug("For feature " + fp + ", hitSeq = null--trying to get sequence for " + hit.getRefSequence().getName());
            }
	    if (hitSeq != null) {
 	      out = seqblOut;
 	    }
 	    else {
              logger.debug("For feature " + fp + ", couldn't get hitSeq--writing to exblxOut");
	      out = exblxOut;
	    }
          }

	  // Format of seqbl lines:
	  // 100 (+1)	7255	7713	131	283 Q9CWX3 VLKAIFQEVHVQSLLQVDRHTVFSIITNFMRSREEELKGLGADFTFGFIQVMDGEKDPRNLLLAFRIVHDLISKDYSLGPFVEELFEVTSCYFPIDFTPPPNDPYGIQREDLILSLRAVLASTPRFAEFLLPLLIEKVDSEILSAKLDSLQTL
	  // score (frame) querystart queryend subjectstart subjectend id residues

	  int hitstart = fp.getHstart();
	  int hitend = fp.getHend();
	  // For tblastx results, hitstart is allowed to be > hitend, but Blixem gets
	  // all pissy if the coordinates are not one-based on the subsequence.
	  if (is_tblastx && (hitstart > hitend)) {
	      hitstart = hitstart - hitend + 1;
	      hitend = 1;

	      //	      logger.debug("For tblastx hit, adjusting hit start and end for " + fp + ": fp.getHstart = " + fp.getHstart() + ", fp.getHend = " + fp.getHend() + ", hitstart now = " + hitstart + ", hitend now = " + hitend); // DEL
	  }
	  
	  // Don't write out this line until we figure out if we need to write sequence with it,
	  // and, if so, whether we *have* sequence.
	  String line = 100 + " " +
	    "(" + frameStr + ")\t" +
	    (fp.getStart()-min+1) + "\t" +
	    (fp.getEnd()-min+1) + "\t" +
	    hitstart + "\t" +
	    hitend + " " +
	    fp.getHname();
          if (out == seqblOut) {
	    logger.debug("For " + fp + ", fp.getHstart = " + fp.getHstart() + ", fp.getHend = " + fp.getHend());
	    String subseq = hitSeq.getResidues(fp.getHstart(),fp.getHend());
	    if (gameAlignSeqString!=null) subseq = gameAlignSeqString;

	    if (subseq == null || subseq.length() <= 1) {
	      logger.debug("Couldn't get residues for hit seq " + hitSeq.getName() + "--trying to get from feature sequence");
	      subseq = hit.getFeatureSequence().getResidues();
	    }

	    if (subseq != null && subseq.length() > 1) {
	      logger.debug("Writing sequence for " + fp.getHname() + ": " + fp.getHstart() +", " + fp.getHend() + ": " + (hitSeq.getResidues(fp.getHstart(),fp.getHend())).length() + " residues");
	      out.append(line + " " + subseq + "\n");
	    }
	    //	    else // DEL
	    //	      logger.debug("Couldn't get get residues for hit seq " + hitSeq); // DEL
	  }
	  else
	    out.append(line + "\n");
	}
      } else if (sf instanceof AnnotatedFeatureI) {
        if (!(sf.getHigh() > max) && !(sf.getLow() < min)) {
          exblxOut.append(-2 + " " + 
                     "(" + sf.getStrand() + ")\t" +
                     (sf.getStart()-min+1) + "\t" +
                     (sf.getEnd()-min+1) + "\t0\t0");
  
          if (sf.getRefFeature() != null) {
            exblxOut.append(" " + sf.getRefFeature().getId() + "\n");
          } else {
            exblxOut.append(" " + sf.getId() + "\n");
          }
        }
      }
    }
    return;
  }
}
