package apollo.editor;

import apollo.datamodel.*;

import java.util.*;
import java.io.*;

import org.apache.log4j.*;

import org.bdgp.util.Comparator;
import org.bdgp.util.VectorUtil;

import apollo.util.FeatureList;
import apollo.datamodel.*;
import apollo.gui.Selection;
import apollo.gui.genomemap.ViewI;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.event.*;

/**
 * This is a convienience class for determining and making easily available which sets,
 * transcripts and genes a group of features come from. Its a helper class for
 AnnotationEditor.
 */
class SelectionSet {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SelectionSet.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  // Vector  drawFeatures;
  /** features contains only leaf features(descendants of sets) -> FeatureLists */
  private Vector  leafFeatures;
  private FeatureList leafFeatList; // FeatureList of features
  private Vector  sets;
  private Vector  transcripts;
  private Vector  annots;
  /** Vector of the original selection vector passed in, contains all different types
      of features. */
  private Vector originalFeatures;
  private boolean used = false;

  SelectionSet(Vector df) {
    init(df);
  }

  // not used
//  public SelectionSet(Selection sel) {
//    init(sel.getSelectedVector());
//  }

  private void init(Vector feat_vect) {
    if (feat_vect != null) {
      originalFeatures = feat_vect;
      this.leafFeatures = findLeafFeatures(feat_vect); // adds all descendants
      sets         = findSets (leafFeatures);
      //transcripts=findTranscripts (features);annots=findAnnots (transcripts);
      findTranscriptsAndAnnots(leafFeatures); // sets transcripts & annots
    } else {
      originalFeatures = new Vector(0);
      leafFeatures     = new Vector(0);
      sets         = new Vector(0);
      transcripts  = new Vector(0);
      annots        = new Vector(0);
    }
    setUsed(false);
  }

  // not used
//  public boolean getUsed() {
//    return used;
//  }

  void setUsed(boolean state) {
    used = state;
  }

  Vector getLeafFeatures() {
    return leafFeatures;
  }
  FeatureList getLeafFeatList() {
    if (leafFeatList == null)
      leafFeatList = new FeatureList(leafFeatures);
    return leafFeatList;
  }

  SeqFeatureI getLeafFeat(int index) {
    Object elem = leafFeatures.elementAt(index);
    if (elem instanceof SeqFeatureI) {
      return (SeqFeatureI)elem;
    } else {
      logger.error("Non SeqFeatureI in features");
      return null;
    }
  }

  Vector getSets() {
    return sets;
  }

  // not used
//   public FeatureSetI getSet(int index) {
//     Object elem = sets.elementAt(index);
//     if (elem instanceof SeqFeatureI && ((SeqFeatureI)elem).canHaveChildren()) {
//       return (FeatureSetI)elem;
//     } else {
//       logger.error("Non FeatureSetI in sets");
//       return null;
//     }
//   }

  /** Return all transcript involved in selection, whether fully or partially
      selected. */
  Vector getTranscripts() {
    return transcripts;
  }

  FeatureList getTranscriptList() {
    return new FeatureList(transcripts); // cache? rid vector?
  }

  int getTranscriptsSize() {
    return transcripts.size();
  }

  Transcript getTranscript(int index) {
    Object elem = transcripts.elementAt(index);
    if (elem instanceof Transcript) {
      return (Transcript)elem;
    } else {
      logger.error("Non Transcript in transcripts");
      return null;
    }
  }

  /** Returns the first transcript in the selection that has all
      of its exons selected, if a Transcript is present that certainly
      qualifies. This is not the same as getTranscript(0). The transcripts
      stored in SelectionSet do not have to be fully selected. So
      getTranscript(0) may return a multi-exon transcript that has only one of
      its exons selected. At present we only need the first fully selected trans
      (for AnnEd.moveExonsToTranscript). If in the future we need more than just
      this, this can be generalized.
  */
  public Transcript getFirstFullySelectedTranscript() {
    HashMap transToExonNum = new HashMap(5);
    for (int i=0; i<originalFeatures.size(); i++) {
      SeqFeatureI sf = (SeqFeatureI)originalFeatures.elementAt(i);
      if (sf instanceof Transcript)
        return (Transcript)sf;
      if (sf instanceof ExonI) {
        Transcript trans = (Transcript) sf.getRefFeature();
        int numExons = 1;
        if (transToExonNum.containsKey(trans))
          numExons += ((Integer)transToExonNum.get(trans)).intValue();
        if (trans.getFeatures().size() == numExons)
          return trans;
        transToExonNum.remove(trans);
        // assumes same exon not in originalfeatures twice
        transToExonNum.put(trans,new Integer(numExons));
      }
    }
    return null;
  }

  Vector getGenes() {
    return annots;
  }

  FeatureList getGeneList() {
    return new FeatureList(annots); // cache?
  }

  public AnnotatedFeatureI getGene(int index) {
    Object elem = annots.elementAt(index);
    AnnotatedFeatureI sf = null;
    if (elem instanceof AnnotatedFeatureI) {
      sf = (AnnotatedFeatureI) elem;
      if (!sf.isAnnotTop())
        sf = null;
    }
    if (sf == null) {
      logger.error("Non Gene in genes");
    }
    return sf;
  }

  private void findTranscriptsAndAnnots(Vector features) {
    transcripts = new Vector();
    annots = new Vector();

    for (int i=0; i < features.size() ;i++) {
      Object elem = features.elementAt(i);
      SeqFeatureI sf = null;
      // i dont think this is necasary anymore is it?
      if (elem instanceof DrawableSeqFeature) {
        DrawableSeqFeature dsf = (DrawableSeqFeature)elem;
        sf = dsf.getFeature();
      } else if (elem instanceof SeqFeatureI) {
        sf = (SeqFeatureI) elem;
      } else {
        logger.error("Non DrawableSeqFeature, non SeqFeatureI in findTranscripts");
      }

      if (sf != null) {
        if (sf.isExon()) {
          addTranscript(sf.getRefFeature());
        } 
        else if (sf.isTranscript()) {
          addTranscript(sf);
        }
        else if (sf.isAnnot()) {
          for (int j=0; i<sf.getNumberOfChildren(); j++)
            addTranscript(sf.getFeatureAt(j)); // isTranscript check?
          addAnnot(sf); // in case has no transcripts
        }
      }
    }

    sortRanges(transcripts);
    sortRanges(annots); // not sure if needed
    //return transcripts;
  }

  private RangeCompare rangeComparator = new RangeCompare();
  private class RangeCompare implements Comparator {
    public int compare (Object a, Object b) {
      if (a instanceof RangeI && b instanceof RangeI) {
        RangeI ta = (RangeI) a;
        RangeI tb = (RangeI) b;
        if (ta.getStart() == tb.getStart())
          return org.bdgp.util.ComparisonConstants.EQUAL_TO;
        else if (ta.getStrand() == 1)
          return (ta.getStart() < tb.getStart() ?
                  org.bdgp.util.ComparisonConstants.LESS_THAN :
                  org.bdgp.util.ComparisonConstants.GREATER_THAN);
        else
          return (ta.getStart() > tb.getStart() ?
                  org.bdgp.util.ComparisonConstants.LESS_THAN :
                  org.bdgp.util.ComparisonConstants.GREATER_THAN);
      } else {
        logger.info("Comparing a " + a.getClass().getName() +
                    " to b " + b.getClass().getName());
        return org.bdgp.util.ComparisonConstants.EQUAL_TO;
      }
    }
  }

  private void sortRanges(Vector ranges) {
    VectorUtil.sort(ranges,rangeComparator);
  }


  private void addTranscript(SeqFeatureI trans) {
    if (trans != null && !(transcripts.contains(trans)))
      transcripts.add(trans);
    addAnnot(trans.getRefFeature());
  }
    
  private void addAnnot(SeqFeatureI annot) {
    if (annot != null && !annots.contains(annot))
      annots.add(annot);
  }

  /** Creates vector of all leaf descendants of feats in features */
  private Vector findLeafFeatures(Vector features) {
    Vector sets = new Vector();
    for (int i=0; i < features.size() ;i++) {
      SeqFeatureI sf = (SeqFeatureI) features.elementAt(i);
      addLeafFeats (sf, sets);
    }
    return sets;
  }

  private void addLeafFeats (SeqFeatureI sf, Vector sets) {
    // canHaveChildren is actually not a good test of leafness. An annotatedFeature
    // returns true for canHaveChildren (its a FeatSet) but a 1 level annot wont
    // actually have kids, and is in fact the leaf. hasKids actually speaks better to
    // the leaf issue.
    //if (sf.canHaveChildren() && (!apollo.config.Config.DO_ONE_LEVEL_ANNOTS || 
    if (sf.hasKids()) {
      FeatureSetI fs = (FeatureSetI) sf;
      int fs_size = fs.size();
      for (int i = 0; i < fs_size; i++) {
        addLeafFeats(fs.getFeatureAt(i), sets);
      }
    } else {
      sets.addElement(sf);
    }
  }

  /** Goes through vector of features finding FeatureSets. If features is a drawable
      it gets its model feature. If model feature is a FeatureSet adds it to
      return vector, if not adds parent, if not already added */
  private Vector findSets(Vector features) {
    Vector sets = new Vector();
    for (int i=0; i < features.size() ;i++) {
      Object elem = features.elementAt(i);
      SeqFeatureI sf = null;
      if (elem instanceof DrawableSeqFeature) {
        DrawableSeqFeature dsf = (DrawableSeqFeature)elem;
        sf = (SeqFeatureI)dsf.getFeature();
      } else if (elem instanceof SeqFeatureI) {
        sf = (SeqFeatureI)elem;
      } else {
        logger.error("DrawableSeqFeature, non SeqFeatureI in findSets");
      }
      if (sf != null ) {
        FeatureSetI set = null;
        if (sf.canHaveChildren()) {
          set = (FeatureSetI) sf;
        } else {
          set = (FeatureSetI)sf.getRefFeature();
        }
        if (set != null && !sets.contains(set)) {
          sets.addElement(set);
        }
      }
    }
    return sets;
  }

  private Vector findAnnots(Vector tran) {
    Vector annots = new Vector();
    for (int i=0; i < tran.size(); i++ ) {
      Transcript t = (Transcript)tran.elementAt(i);

      if (!(annots.contains(t.getRefFeature()))) {
        annots.addElement(t.getRefFeature());
      }
    }
    return annots;
  }

  public void dump(String desc) {
    logger.info("Dumping " + desc);

    /*
      logger.info(" Drawable features vector:");
      for (int i=0;i<drawFeatures.size(); i++) {
        logger.info("  " + getDrawFeature(i));
      }
    */
    logger.info(" Features vector:");
    for (int i=0;i<leafFeatures.size(); i++) {
      logger.info("  " + (SeqFeatureI)leafFeatures.elementAt(i));
    }
    logger.info(" Sets vector:");
    for (int i=0;i<sets.size(); i++) {
      logger.info("  " + (FeatureSetI)sets.elementAt(i));
    }
    logger.info(" Transcripts vector:");
    for (int i=0;i<transcripts.size(); i++) {
      logger.info("  " + (Transcript)transcripts.elementAt(i));
    }
    logger.info(" Gene vector:");
    for (int i=0;i<annots.size(); i++) {
      logger.info("  " + (AnnotatedFeatureI)annots.elementAt(i));
    }
    logger.info(" Used flag = " + used);
  }

}


//     /*
//       SZL
//       Its important that the transcript vector is kept in sorted
//       order. From most 5' to most 3'. Otherwise how transcripts
//       are merged (preferred name, start of translation) is
//       dependent on click order, not biology
//       do the annots need to be sorted too?
//     */
//     org.bdgp.util.Comparator transcriptCompare = new org.bdgp.util.Comparator() {
//           public int compare (Object a, Object b) {
//             if (a instanceof Transcript && b instanceof Transcript) {
//               Transcript ta = (Transcript) a;
//               Transcript tb = (Transcript) b;
//               if (ta.getStart() == tb.getStart())
//                 return org.bdgp.util.ComparisonConstants.EQUAL_TO;
//               else if (ta.getStrand() == 1)
//                 return (ta.getStart() < tb.getStart() ?
//                         org.bdgp.util.ComparisonConstants.LESS_THAN :
//                         org.bdgp.util.ComparisonConstants.GREATER_THAN);
//               else
//                 return (ta.getStart() > tb.getStart() ?
//                         org.bdgp.util.ComparisonConstants.LESS_THAN :
//                         org.bdgp.util.ComparisonConstants.GREATER_THAN);
//             } else {
//               logger.info ("Comparing a " + a.getClass().getName() +
//                                   " to b " + b.getClass().getName());
//               return org.bdgp.util.ComparisonConstants.EQUAL_TO;
//             }
//           }
//         };
//     org.bdgp.util.VectorUtil.sort (transcripts, transcriptCompare);
