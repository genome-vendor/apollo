package apollo.util;

import apollo.datamodel.SeqFeature;
import apollo.datamodel.AssemblyFeature;
import apollo.datamodel.StrandedFeatureSet;
import apollo.datamodel.FeatureSet;
import apollo.datamodel.AnnotatedFeature;
import apollo.datamodel.Transcript;
import apollo.datamodel.Exon;
import apollo.datamodel.Evidence;
import apollo.datamodel.CurationSet;

/**
 * I am a utility for visiting each element in the datamodel.
 * See the accept method on each element in the datamodel for
 * how I am used. Planned implementation: rendering XML from
 * the datamodel.
**/
public interface Visitor{
  public void visit(SeqFeature feature);
  public void visit(AssemblyFeature feature);
  public void visit(StrandedFeatureSet feature);
  public void visit(FeatureSet feature);
  public void visit(AnnotatedFeature feature);
  public void visit(Transcript feature);
  public void visit(Exon feature);
  public void visit(Evidence evidence);
  public void visit(CurationSet set);
  
}


