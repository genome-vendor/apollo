package jalview.datamodel;

import java.util.*;
import java.io.*;

import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.util.*;
import apollo.config.Config;

import jalview.gui.DrawableSequence;
import apollo.seq.ResidueProperties;
import org.bdgp.util.DNAUtils;

public class CondensedAlignment {

  /** List of SeqFeatures */
  FeatureList feats;
  private Mapper    mapper;

  /** ArrayList of Block objects */
  private ArrayList blockList;

  private boolean isForwardStrand;

  public ArrayList dnaseqs;
  public ArrayList pepseqs;

  private final static int mingap = 20; // this is 2 * padding
  private final static int intron = 20; // this is actual lenght of intron

  AlignSequence[] seqarr;

  private apollo.datamodel.SequenceI genomicSeq;

  public CondensedAlignment(FeatureList feats) {
    this.feats = feats;
    
    // all features are expected to have the same strand
    if (feats.size() == 0) return;
    isForwardStrand = feats.getFeature(0).getStrand() == 1;
    genomicSeq = feats.getFeature(0).getRefSequence();

    makeBlocks();
    
  }

  public Mapper getMapper() {
    return mapper;
  }
 
  private void makeBlocks() {

    mapper = new Mapper(isForwardStrand);
    feats = feats.getAllLeaves(); // get list of all leaf features
    clusterFeats();       // Find non overlapping regions from pairs
    padAndMergeBlocks();       // Pad the blocks and insert introns
    mapper.addBlocks(blockList);
  }


  /**  Find non overlapping regions from features.
       Creates Blocks and adds them to blockList. Each block contains 
       features that all overlap. */
  private void clusterFeats() {
    blockList = new ArrayList();

    strandedSort(feats);

    FeatureBlock currentBlock=null;
    for (int i = 0; i < feats.size(); i++) {
      SeqFeatureI feat = feats.getFeature(i);

      if (i == 0) {
	FeatureBlock firstBlock = new FeatureBlock(feat);
	blockList.add(firstBlock);
	currentBlock = firstBlock;
      } else {
	if (currentBlock.overlaps(feat)) currentBlock.addFeature(feat);
	else {
	  currentBlock = new FeatureBlock(feat);
	  blockList.add(currentBlock);
	}
      }
    }
  }


  /** 
      Pad the blocks,insert introns,set jalview coordinates. 
      This also merges blocks if the padding is bigger than the gap size
  */
  private void padAndMergeBlocks() {
    FeatureBlock previousBlock=null;
    // The block list after blocks merge. Cant delete amidst iterating.
    ArrayList mergedBlockList = new ArrayList();

    // loop through blockList to pad and merge blocks. At this point a block
    // contains a FeatureList of overlapping SeqFeatures
    for (int i = 0; i < blockList.size(); i++) { 
      FeatureBlock block = (FeatureBlock)blockList.get(i);
      // pad out blocks
      block.pad();

      // First block
      if (i == 0) {  
	previousBlock = block;
	mergedBlockList.add(block);
      }
      // everything but first block
      else if (i > 0) { 
	// needs to go in createSequences i think - dont have gaps yet
	int condstart = previousBlock.getJalviewHigh() + intron;
	int condend   = condstart + block.length();

	// If padding reaches to next block, merge them
	int blockGap = block.getGenomicLow() - previousBlock.getGenomicHigh();
	if (Math.abs(blockGap) <= mingap) {
	  // Merge this block with the previous one
	  previousBlock.merge(block);
	} 
	else { 
	  previousBlock = block;
	  mergedBlockList.add(block);
	}

      } // end of else if (i > 0) (not first block)
    } // end of blockList for loop

    // Update blockList with the new merged block list
    blockList = mergedBlockList;
  }
  
  /** Return a featureList of parents. Each parent represent one line of sequence
      whose children are contained in the different blocks of the sequence. 
      The horizontal axis is the blocks.
      This creates the vertical axis, one entry per sequence
      in the alignment. Helper function for createSequence */
  private FeatureList createParentList() {
    FeatureList parentList = new FeatureList();

    for (int i = 0; i < blockList.size(); i++) {
      FeatureBlock block = (FeatureBlock)blockList.get(i);
      for (int j = 0; j < block.featSize(); j++) {
	SeqFeatureI feat = block.getFeature(j);
	SeqFeatureI parent = feat.getParent();
	// check if Alignable?
	if (!parentList.contains (parent)) parentList.add (parent);
      }
    }
    return parentList;
  }

  /** Go thru blocks and create Sequences. 
      Each line of jalview is a Sequence, so
      a Sequence can contain more than one apollo SeqFeature.
      A Sequence is akin to a FeatureSet */
  public AlignSequenceI[] createSequences() {
    
    ArrayList seqs    = new ArrayList();
    dnaseqs = new ArrayList();
    pepseqs = new ArrayList();

    // First of all we need to get a unique list of parents
    FeatureList parentList = createParentList();

    // suzi addition - this will order align seqs by their starts, meaning the 
    // topmost sequence(after genomic) will start first)
    //parentList.sortByStart(); --> stranded sort
    

    // Loop over each "sequence" and construct the condensed sequence
    // by padding each block to the block ends and adding intron sequence
    // A "sequence" is a FeatureList of SeqFeatures (all with the same parent)
    for (int i = 0; i < parentList.size(); i++) {
      
      //FeatureSetI parent = (FeatureSetI)en.nextElement();
      SeqFeatureI parent = parentList.getFeature(i);
      //FeatureList siblingFeats  = (FeatureList)seqhash.get(parent);
      String    alignType = "DNA";

      // ?? we need sibling feats do we not?? for strandedness? - or does this 
      // happen in the block now?
      //strandedSort(siblingFeats); 
      
      // current tracks where we are in the making of the sequence in 
      // condensed/column/jalview coordinates (not genomic)
      //int current = 1; // not used anymore
      StringBuffer hit_alignment = new StringBuffer();
      
      int seqGenomicStart = -1;
      int seqGenomicEnd=-1;

      // To construct the sequence we need to loop over each block and
      // Look to see whether this sequence is in here.  If not we
      // just add in spaces. This builds up the string seq which is then used
      // to make a Sequence.
      // The blocks go from 5' to 3' (i think)
      for (int l = 0; l < blockList.size(); l++) {

	FeatureBlock block = (FeatureBlock)blockList.get(l);
	if (l == 0) 
	  block.setJalviewLow(1);
	else {
	  FeatureBlock prevBlock = (FeatureBlock)blockList.get(l-1);
	  block.setJalviewLow(prevBlock.getJalviewHigh() + mingap);
	}

	// suzi addition/improvement - the block takes care of this:
	//String align_res = block.getAlignmentForParent(parent);
        BlockAlignment blockAlignment = block.getBlockAlignForParent(parent);
        String align_res=null;// = blockAlignment.getAlignment();
	//if (align_res == null) { // this block not part of "Seq"
        if (blockAlignment != null) {
          align_res = blockAlignment.getAlignment();
          if (seqGenomicStart == -1) {
            seqGenomicStart = blockAlignment.getGenomicStart(); 
          }
          seqGenomicEnd = blockAlignment.getGenomicEnd();
        }
        else {
	  align_res = emptyBlock(block.getGappedLength());
        } 

	hit_alignment.append (align_res);

	// add in intron if not the last block
	if (l != blockList.size()-1)
	  hit_alignment.append(getIntronString());


      } // end of block loop


      // Now finally create the sequence object
      // for rev strand this needs to change
      String name = Config.getDisplayPrefs().getDisplayName(parent);
      AlignSequence seqobj = new AlignSequence(name,
                                               hit_alignment.toString(),
                                               seqGenomicStart,
                                               seqGenomicEnd);
      seqs.add(seqobj);

      if (alignType.equals("DNA")) {
	dnaseqs.add(seqobj);
      } else {
	pepseqs.add(seqobj);
      }

    } // end of sequence loop - while (en.hasMoreElements())


    // Now generate the genomic sequence plus 3 frame translation
    // subroutine -> makeGenomicAndTranslations()
    
    StringBuffer genstr = new StringBuffer();
    StringBuffer tmp0   = new StringBuffer();
    StringBuffer tmp1   = new StringBuffer();
    StringBuffer tmp2   = new StringBuffer();
    int genomicEnd=0;

    for (int j = 0; j < blockList.size(); j++) {

      FeatureBlock block = (FeatureBlock)blockList.get(j);
      // For reverse strand, block end < start, getRes revcomps automatically
      //String tmp = genomicSeq.getResidues(block.getGenomicStart(),block.getGenomicEnd());
      String align_res = block.getGappedGenomic();

      genstr.append(align_res);

      // Now let's do the 3 frame translation
      //String localFrame0 = jalview.gui.schemes.ResidueProperties.translate(align_res,0);

      // There should be a separate method for adding translated genomics!
      // dont add translated genomic if theres no genomic to translate...
      if (block.hasTranslatedGenomic()) {
        String localFrame1 = block.getTranslatedGenomic(0);
        String localFrame2 = block.getTranslatedGenomic(1);
        String localFrame3 = block.getTranslatedGenomic(2);
        
        // take out the tmps?
        tmp0.append(localFrame1);
        tmp1.append(localFrame2);
        tmp2.append(localFrame3);

        // Need to pad to end of genomic - is this intron or genomic???
        // i think this is now pase and should be deleted (?)
        for (int i = tmp0.length() ; i < genstr.length(); i++) {
          //tmp0.append("-");
          tmp0.append(":"); // changing to colon for now
        }
        for (int i = tmp1.length() ; i < genstr.length(); i++) {
          //tmp1.append("-");
          tmp1.append(":"); // changing to colon for now
        }
        for (int i = tmp2.length() ; i < genstr.length(); i++) {
          //tmp2.append("-");
          tmp2.append(":"); // changing to colon for now
        }

      }

      // record last base pair that isnt intron for end of Sequence
      genomicEnd = mapper.condensed2genomic(genstr.length());
      // And finally the intron
      // Do we want N's in the intron or dashes? or something else.
      // N seems confusing because its also used in sequences unless
      // that was the point and im missing it - changing to "-" for now
      // changing to "_"
      if (j != blockList.size()-1) { // do not put an intron after the last block
        genstr.append(getIntronString());
        tmp0.append(getIntronString());
        tmp1.append(getIntronString());
        tmp2.append(getIntronString());
      }

    } // end of blockList for loop
     

    // Create the array of sequences 
    seqarr = new AlignSequence[seqs.size()+4];


    // To get the colour schemes right a new sequence needs to be made containing all 
    // 3 translations of the genomic and then to compare to all peptide sequences but 
    // not displayed.  A subclass is needed to contain this information.
    // Isnt it better to use the genomic in the label?
    int start = mapper.condensed2genomic(1); // genomic begins at column 1
    seqarr[0] = new AlignSequence("GENOMIC",genstr.toString(),start,genomicEnd);

    // Make frames consistent with the rest of apollo which is frame 0 for the beginning 
    // of the big sequence
    int genomicFrame =  genomicSeq.getFrame(start,isForwardStrand);
    String genomicFrame0=null, genomicFrame1=null, genomicFrame2=null;
    if (true || genomicFrame==0) {
      genomicFrame0 = tmp0.toString();
      genomicFrame1 = tmp1.toString();
      genomicFrame2 = tmp2.toString();
    } else if (genomicFrame==1)  {
      genomicFrame0 = tmp2.toString();
      genomicFrame1 = tmp0.toString();
      genomicFrame2 = tmp1.toString();
    } else if (genomicFrame==2) {
      genomicFrame0 = tmp1.toString();
      genomicFrame1 = tmp2.toString();
      genomicFrame2 = tmp0.toString();
    }
      
    // add translated genomic seqs
    // is it funny to have peptides in genomic coords?
    seqarr[1] = new AlignSequence("FRAME1",genomicFrame0,start,genomicEnd);
    seqarr[2] = new AlignSequence("FRAME2",genomicFrame1,start,genomicEnd);
    seqarr[3] = new AlignSequence("FRAME3",genomicFrame2,start,genomicEnd);

    for (int i = 4; i < (4+seqs.size()); i++) {
      seqarr[i] = (AlignSequence)seqs.get(i-4);     
    }

    return seqarr;
  } // end of createSequences

  private String intronString = "";
  private String getIntronString() {
    if (intronString.equals(""))
      for (int k = 0; k < intron; k++) intronString += "_";
    return intronString;
  }

  public SequenceGroup getDnaSequences() {
    SequenceGroup dnag = new SequenceGroup();
    dnag.addSequence(new DrawableSequence(seqarr[0]));

    for (int i = 0; i< dnaseqs.size(); i++) {
      DrawableSequence seq = new DrawableSequence((AlignSequence)dnaseqs.get(i));
      dnag.addSequence(seq);
    }

    return dnag;
  }

  public SequenceGroup getPeptideSequences() {
    SequenceGroup pepg = new SequenceGroup();

    pepg.addSequence(new DrawableSequence(seqarr[1]));
    pepg.addSequence(new DrawableSequence(seqarr[2]));
    pepg.addSequence(new DrawableSequence(seqarr[3]));

    for (int i = 0; i < pepseqs.size(); i++) {
      DrawableSequence seq = new DrawableSequence((AlignSequence)pepseqs.get(i));
      pepg.addSequence(seq);
    }

    return pepg;
  }

  /** descending order(5' to 3') for rev strand, ascending for forward strand */
  private void strandedSort(FeatureList arr) {
      int start[] = new int[arr.size()];
      SeqFeatureI obj[]   = new SeqFeatureI[arr.size()];

      for (int i = 0 ; i < arr.size();i++) {
	start[i] = arr.getFeature(i).getStart(); // not getLow
	obj[i]   = arr.getFeature(i);
      }

      QuickSort.sort(start,obj);

      for (int i = 0; i < obj.length; i++)  {
	if (isForwardStrand) 
	  arr.set(i,obj[i]);
	else  // sort backwards for reverse strand
	  arr.set(obj.length-1-i,obj[i]);
      }
  }

  /** Returns a string of '-' of length length */
  private String emptyBlock(int length) {
    StringBuffer buf = new StringBuffer();
    // should this be dots?
    //padSequence (buf, length, '-');
    padSequence (buf, length, '.');
    return buf.toString();
  }
  private void padSequence (StringBuffer seq, int length, char pad) {
    for (int i = 0; i < length; i++)  seq.append(pad);
  }

  public static void main(String[] args) {

    try {
      GFFFile gff = new GFFFile(args[0],"File");
      
      
      FeatureList f = new FeatureList();
      
      for (int i = 0; i < gff.seqs.size(); i++) {
	f.add(gff.seqs.elementAt(i));
      }
      
      CondensedAlignment ca = new CondensedAlignment(f);

      AlignSequenceI[] seqs = ca.createSequences();

      
    } catch (IOException e) {
      System.out.println("Exception e " + e);
    }

  }
}

