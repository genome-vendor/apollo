package apollo.dataadapter.synteny;

import apollo.config.FeatureProperty;
import apollo.datamodel.SeqFeatureI;

class TranscriptLinker extends PeptideLinker {

  TranscriptLinker(FeatureProperty featProp) {
    super(featProp);
  }

  protected ResultAnnotPairI getResultAnnotPair(SeqFeatureI f1, SeqFeatureI f2) {
    return new ResultTranscriptPair(f1,f2,this);
  }
  

}
