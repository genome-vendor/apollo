package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;

public class FilterReadingFrame implements Filter<SeqFeatureI> {
  
  private ReadingFrame readingFrame;

  public FilterReadingFrame(ReadingFrame rf) {
    readingFrame = rf;
  }
  
  public boolean keep(SeqFeatureI f) {
    return f.getFrame() == readingFrame.toInt();
  }

  public String toString() {
    return "Keep " + valueToString();
  }

  public String valueToString() {
    return "ReadingFrame " + readingFrame;
  }
  
}
