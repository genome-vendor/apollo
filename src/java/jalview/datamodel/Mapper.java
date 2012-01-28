package jalview.datamodel;

import java.util.*;

/**
 * Mapped class to store the mapping of genomic regions to a condensed alignment.
 * Also contains conversion functions for converting to and from genomic coords.
 */

public class Mapper {
  private ArrayList blockList;
  private boolean isForwardStrand;
  
  public Mapper(boolean isForwardStrand) {
    this.isForwardStrand = isForwardStrand;
  }

  void addBlocks(ArrayList blockList) {
    this.blockList = blockList;
    if (blockList.size() == 0) return;
    isForwardStrand = ((FeatureBlock)blockList.get(0)).isForwardStrand();
  }

  private FeatureBlock getFeatureBlock(int i) {
    return (FeatureBlock)blockList.get(i);
  }

  public FeatureBlock genomic2block(int genomic) {
    for (int i = 0; i < blockList.size(); i++) {
      FeatureBlock block = (FeatureBlock)blockList.get(i);
      if (block.containsGenomic(genomic)) return block;
    }
    return null;
  }
    
//   public int genomic2condensed(int genomic) {
//     FeatureBlock block = genomicToBlock(genomic);
//     if (block == null) return -1;
//     return block.genomicToJalview(genomic);
//   }
  /** This is not right - fix - i dont think this is being used yet but
      it will be i think */
  public int genomic2condensed(int genomic) {

    for (int i = 0; i < blockList.size(); i++) {
      FeatureBlock block = (FeatureBlock)blockList.get(i);

      if (block.containsGenomic(genomic)) {
	// negative on rev strand
	int offset = Math.abs(genomic - block.getGenomicStart());
	int coord  = block.getJalviewLow() + offset;
	return coord;
      }
    }
    return -1;

  }
//   private FeatureBlock genomicToBlock(int genomic) {
//     for (int i=0; i<featureBlocks.size(); i++) {
//       FeatureBlock block = getFeatureBlock(i);
//       // is this right? is FeatureSet.contains for the genomic?
//       if (block.contains(genomic)) return block;
//     }
//     return null;
//   }

  public int condensed2genomic(int condensed) {
    FeatureBlock block = condensedToBlock(condensed);
    if (block == null) return -1;
    return block.jalviewToGenomic(condensed);
  }
  private FeatureBlock condensedToBlock(int condensed) {
    for (int i=0; i<blockList.size(); i++) {
      FeatureBlock block = getFeatureBlock(i);
      if (block.containsJalviewCoord(condensed)) return block;
    }
    return null;
  }

}

  
