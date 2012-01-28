package apollo.dataadapter.synteny;

import apollo.datamodel.SeqFeatureI;

class ResultTranscriptPair extends ResultPeptidePair implements ResultAnnotPairI {

  ResultTranscriptPair(SeqFeatureI feat1, SeqFeatureI feat2, AbstractLinker linker) {
    super(feat1,feat2,linker);
  }

  public boolean isLinked() {
    return getResult().getHitFeature().getName().equals(getTranscript().getName());
  }

  //public Link createLink() { }

  /** hitPosition is in transcript coords */
  protected int getGenomicPosition(int hitPosition) {
    return getTranscript().getGenomicPosition(hitPosition);
  }
}
