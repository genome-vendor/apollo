package apollo.gui;

import apollo.datamodel.*;

/** changed long positions to int positions to be consistent with rest of apollo
    using ints for positions, hope this is ok */
public class GCScoreCalculator extends WindowScoreCalculator {
  CurationSet curation;

  public GCScoreCalculator(CurationSet curation, int winSize) {
    super(winSize);
    if (curation != null) {
      this.curation = curation;
    }
  }

  public int [] getXRange() {
    if (curation != null) {
      return new int [] {curation.getLow(), curation.getHigh()};
    } else {
      return new int [] {1,1};
    }
  }

  public double [] getScoresForPositions(int [] positions) {

    int posLen = positions.length;
    int [] regionExtents = new int[2];
    SequenceI sequence = curation.getRefSequence();

    if (positions == null || posLen == 0 || sequence == null) {
      return new double[0];
    }

    getWindowExtents(positions[0],regionExtents,(int)curation.getLow(),(int)curation.getHigh());
    int tmp = regionExtents[0];
    getWindowExtents(positions[posLen-1],regionExtents,(int)curation.getLow(),(int)curation.getHigh());
    regionExtents[0] = tmp;

    int bottomOverhang = positions[0]-regionExtents[0];
    //    int topOverhang = regionExtents[1]-positions[posLen-1];

    char [] regSeq;
//    if (xOrientation == Transformer.LEFT) {
//      regSeq = sequence.getResidues((int)regionExtents[0],
//                                    (int)regionExtents[1]).toCharArray();
//    } else {
//      regSeq = sequence.getResidues((int)regionExtents[1],
//                                    (int)regionExtents[0]).toCharArray();
//    }
    // Want sequence from low to high in user coordinates no matter what the xOrientation, because
    // positions are in user coordinates. Strand of sequence is unimportant for GC calculation.
    regSeq = sequence.getResidues((int)regionExtents[0],
                                  (int)regionExtents[1]).toCharArray();

    double [] scores = new double[posLen];

    for (int i=0; i<posLen; i++) {
      getRelativeWindowExtents(regSeq,positions[i]-positions[0]+bottomOverhang,winExtents);
      scores[i] = getScore(regSeq,(int)winExtents[0],(int)winExtents[1]);
    }
    return scores;
  }

  private double getScore(char [] seqChars, int low, int high) {
    double nGC = 0;

    for (int i=low; i<=high; i++) {
      if (seqChars[i] == 'G' || seqChars[i] == 'C')
        nGC++;
    }
    return nGC/(double)(high-low+1) * 100.0;
  }

  private void getRelativeWindowExtents(char [] seqStr, int pos,
                                        int [] extents) {
    // looking upstream and downstream from this position
    int low  = pos-halfWinSize;
    int high = pos+halfWinSize;

    extents[0] = low < 0 ? 0 : low;
    int seqLen = seqStr.length - 1;
    extents[1] = high > seqLen ? seqLen : high;
  }
}
