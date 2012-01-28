package apollo.gui.detailviewers.sequencealigner.filters;

import apollo.datamodel.SeqFeatureI;
import apollo.gui.detailviewers.sequencealigner.ReadingFrame;
import apollo.gui.detailviewers.sequencealigner.SequenceType;
import apollo.gui.detailviewers.sequencealigner.Strand;

public class FilterFactory {

  public static Filter<SeqFeatureI> makeFilter(Strand s) {
    return new FilterStrand(s);
  }
  
  public static Filter<SeqFeatureI> makeFilter(ReadingFrame rf){
    return new FilterReadingFrame(rf);
  }
  
  public static Filter<SeqFeatureI> makeFilter(SequenceType st) {
    return new FilterSequenceType(st);
  }
  
  public static Filter<SeqFeatureI> makeFilterDisplayType(String s) {
    return new FilterDisplayType(s);
  }
  
  public static Filter<SeqFeatureI> makeFilterName(String n) {
    return new FilterName(n);
  }
  
  public static Filter makeInverse(Filter f) {
    return new FilterInverse(f);
  }
  
  public static MultiFilter makeMultiFilterAnd() {
    return new MultiFilterAnd();
  }
  
  public static MultiFilter makeMultiFilterOr() {
    return new MultiFilterOr();
  }
}
