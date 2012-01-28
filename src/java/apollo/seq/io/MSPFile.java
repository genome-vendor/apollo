package apollo.seq.io;

import apollo.datamodel.*;

public class MSPFile {

  public static String print(FeatureSetI set) {
    StringBuffer out = new StringBuffer();

    for (int i=0; i< set.size(); i++) {
      SeqFeatureI sf = set.getFeatureAt(i);
      if (sf.canHaveChildren()) {
        FeatureSetI fs = (FeatureSetI)sf;
        out.append(print(fs));
      } else if (sf instanceof FeaturePair) {

        FeaturePair fp = (FeaturePair)sf;
        out.append(fp.getScore() + " (" + fp.getFrame() + ")  " + fp.getHstart() + " " + fp.getHend() + " " +
                   fp.getStart() + " " + fp.getEnd() + " " + fp.getName() + " " +
                   fp.getResidues() + "\n");
      } else {
        System.out.println("Ignoring SeqFeature " + sf + " in MSPFile print\n");
      }
    }
    return out.toString();
  }
}
