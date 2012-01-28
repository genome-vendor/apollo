package apollo.dataadapter.ensj;

import java.util.*;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.util.QuickSort;
import apollo.datamodel.*;
import apollo.dataadapter.FeatureSetBuilder;

import org.apache.log4j.*;

// Modified behaviour of standard FeatureSetBuilder. I wanted to be able to store internal ids 
// in the id field of the seq feature, but FeatureSetBuilder was using that for grouping. I
// want to use name for grouping, hence I override buildFeatureHash which does the grouping on
// 'id' which is now using getName instead of getId.
// Also as features are split by ensj into their component ungapped features using a cigar
// string, those ungapped features will all now have the same id (NOT name I mean id) because
// they come from the same feature in the db. That means I can use id for grouping homol
// features in splitHomols so this class overrides that method too.
public class EnsJFeatureSetBuilder extends FeatureSetBuilder {
  protected final static Logger logger = LogManager.getLogger(EnsJFeatureSetBuilder.class);

  protected void buildFeaturesHash(Vector features,
                                   Hashtable plus_fhash,
                                   Hashtable minus_fhash) {
    Hashtable fhash;

    int setSize = features.size();
    for (int i=0; i < setSize; i++) {
      SeqFeatureI sf   = (SeqFeatureI) features.elementAt(i);
      String type = (sf.getFeatureType() == null ?
                     SeqFeatureI.NO_TYPE : sf.getFeatureType());
      String ref_id = (sf.getName() == null || (sf instanceof FeatureSetI && !((FeatureSetI)sf).hasNameBeenSet()) ?
                       "NoRefId" : sf.getName());
      if (ref_id.equals ("NoRefId")) {
        logger.warn("Set ref id to " + ref_id + ". " +
                           "For child feature  " +
                           sf.getName() + ":" +
                           sf.getFeatureType());
      }

      Vector sf_vect;

      fhash = (sf.getStrand() == -1 ? minus_fhash : plus_fhash);
      Hashtable id_hash = (Hashtable) fhash.get(type);
      if (id_hash == null) {
        logger.debug("NEW TYPE IN HASHFEATURES: " + type);
        id_hash = new Hashtable(1);
        fhash.put (type, id_hash);
      }
      sf_vect = (Vector) id_hash.get(ref_id);
      if (sf_vect == null) {
        sf_vect = new Vector(1);
        id_hash.put (ref_id, sf_vect);
      }
      sf_vect.addElement (sf);
    }
  }

  public void makeHomolSets(StrandedFeatureSet fset,
                            Hashtable id_hash,
                            String type,
                            int strand) {

    FeatureSetI fs = new FeatureSet();

    fs.setFeatureType(type);
    fs.setStrand (strand);

    Enumeration e = id_hash.keys();
    while (e.hasMoreElements()) {
      String ref_id = (String) e.nextElement();
      Vector sf_vect = (Vector) id_hash.get(ref_id);

      Vector r_vect = splitHomols (sf_vect, type, ref_id, strand);
      int rVectSize = r_vect.size();
      for (int i = 0; i < rVectSize; i++) {
        FeatureSetI r = (FeatureSet) r_vect.elementAt(i);

        r.setFeatureType(type);
        r.setName(ref_id);
        r.setStrand (strand);
        fs.addFeature (r);
      }
    }
    fset.addFeature (fs, false);
  }

  public Vector splitHomols(Vector sf_vect,
                            String type,
                            String ref_id,
                            int strand) {


    int sfVectSize = sf_vect.size();
    long[]        starts = new long[sfVectSize];
    SeqFeatureI[] f      = new SeqFeatureI[sfVectSize];

    // Sort the feature set by start coord
    for (int i=0; i < sfVectSize; i++) {
      starts[i] = ((SeqFeatureI) sf_vect.elementAt(i)).getStart();
      f[i]      = (SeqFeatureI) sf_vect.elementAt(i);
    }

    QuickSort.sort(starts,f);

    Vector fset = new Vector(sfVectSize);
    for (int i=0; i < sfVectSize; i++) {
      fset.addElement(f[i]);
    }

    // Now partition the feature set into
    // continuous pieces
    Vector newsets = new Vector(1);
    Vector potentialSets = new Vector(1);
    int    gap       = 20;
    int    maxGenGap = 1000000;

    int fsetSize = fset.size();

    // Three parts for finding the most appropriate groups
    //    HitEnd - HitStart and GenomicEnd - GenomicStart compatibility
    //    Same id 
    //    Most similar score (logic is that if same then might be part of same hit)
    for (int i=0; i < fsetSize; i++) {
      if (fset.elementAt(i) instanceof FeaturePair) {
        FeaturePair sf = (FeaturePair) fset.elementAt(i);

        potentialSets.removeAllElements();

        int newSetsSize = newsets.size();

        // Puts homols together based on coordinate gaps between them
        // and also that the hit start coord follows on from
        // the previous hit end

        for (int k=newSetsSize-1; k >= 0; k--) {
          FeatureSetI tmp1 = (FeatureSetI)newsets.elementAt(k);
          FeaturePair tmp2
          = (FeaturePair)tmp1.getFeatures().lastElement();

          if (tmp2.getStrand() == sf.getStrand()) {
            if (tmp2.getStrand() == 1) {
              long Hgap = Math.abs(tmp2.getHend() -
                                   sf.getHstart());
              long Ggap = Math.abs(tmp2.getEnd() -
                                   sf.getStart());
              if (Hgap <= gap &&
                  Ggap <= maxGenGap &&
                  sf.getHstart() > tmp2.getHend()) {
                potentialSets.addElement(tmp1);
              }
            } else {
              long Hgap = Math.abs(tmp2.getHstart() -
                                   sf.getHend());
              long Ggap = Math.abs(tmp2.getStart() -
                                   sf.getEnd());
              if  (Hgap <= gap &&
                   Ggap <= maxGenGap &&
                   tmp2.getHstart() > sf.getHend()) {
                potentialSets.addElement(tmp1);
              }
            }
          }
        }

        FeatureSetI theSet = null;
        int potentialSetsSize = potentialSets.size();

        // Try id
        if (potentialSetsSize != 0) {
          String sfId = sf.getId();
          for (int k=0;k<potentialSetsSize;k++) {
            FeatureSetI testSet = (FeatureSetI)potentialSets.elementAt(k);
            String testId = ((FeaturePair)testSet.getFeatures().lastElement()).getId();
            if (sfId.equals(testId)) {
              theSet = testSet;
// Actually set the id of the set now we have multiple features with the same one
              theSet.setId(testId);
              break;
            }
          }
        }

        // If theSet is still null then there wasn't an id match so try score compatibility
        //
        if (potentialSetsSize != 0 && theSet == null) {
          theSet = (FeatureSetI)potentialSets.elementAt(0);
          double sfScore = sf.getScore();
          double smallestDiff = Math.abs(sfScore-theSet.getScore());
          double testDiff;
          for (int k=0;k<potentialSetsSize;k++) {
            FeatureSetI testSet
            = (FeatureSetI)potentialSets.elementAt(k);
            if (testSet.getScore() == sf.getScore()) {
              theSet = testSet;
              break;
            }
            testDiff = Math.abs(sfScore-testSet.getScore());
            if (testDiff < smallestDiff) {
              theSet = testSet;
              smallestDiff = testDiff;
            }
          }
        }
        if (theSet == null) {
          FeatureSet newfs = new FeatureSet();
          newfs.setFeatureType(type);
          newsets.addElement(newfs);
          theSet = newfs;
        } else {
        }
        theSet.addFeature(sf);
      }
    }
// Could go through newsets and check for ones which contain features with only one id and set id on the set (note
// id has already been set above where id has been used to link)
    return newsets;
  }
}
