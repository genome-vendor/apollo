package apollo.dataadapter;

import java.util.*;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import apollo.util.*;
import apollo.datamodel.*;
import org.bdgp.util.Memer;

public class FeatureSetBuilder {
  // this is expecting a vector of features that have no
  // relationship to one another, in the sense of FeatureSets
  // and RefFeature. The objective is to build such an
  // organization from this homogeneous collection
  public void makeSetFeatures(StrandedFeatureSet fset,
                              Vector features,
                              PropertyScheme pscheme) {
    Hashtable plus_fhash = new Hashtable (12);
    Hashtable minus_fhash = new Hashtable(12);

    buildFeaturesHash(features, plus_fhash, minus_fhash);
    buildFeatures (fset, plus_fhash, 1, pscheme);
    buildFeatures (fset, minus_fhash, -1, pscheme);
    plus_fhash = null;
    minus_fhash = null;
    System.gc();
  }

  protected void buildFeatures (StrandedFeatureSet fset,
                                Hashtable fhash,
                                int strand,
                                PropertyScheme pscheme) {
    Enumeration  e           = fhash.keys();
    while (e.hasMoreElements()) {
      String type = (String) e.nextElement();
      Hashtable id_hash = (Hashtable) fhash.get(type);
      FeatureProperty fp = (pscheme != null ?
                            pscheme.getFeatureProperty(type) : null);
      if (fp != null &&
          fp.getGroupFlag() == FeatureProperty.GRP_GENE) {
        makeGeneSets(fset, id_hash, type, strand);
      } else if (fp != null &&
                 fp.getGroupFlag() == FeatureProperty.GRP_HOMOLOGY) {
        makeHomolSets(fset, id_hash, type, strand);
      } else {
        // to be consistent with the CORBAAdapter
        // each SINGLE feature is placed in a FeatureSet
        makeSingSets(fset, id_hash, type, strand);
      }
    }
    // DisplayTool.showFeatureSet(fset);
  }

  protected void buildFeaturesHash(Vector features,
                                   Hashtable plus_fhash,
                                   Hashtable minus_fhash) {
    Hashtable fhash;

    int setSize = features.size();
    for (int i=0; i < setSize; i++) {
      SeqFeatureI sf   = (SeqFeatureI) features.elementAt(i);
      String type = (sf.getFeatureType() == null ?
                     SeqFeatureI.NO_TYPE : sf.getFeatureType());
      String ref_id = (sf.getId() == null ?
                       "NoRefId" : sf.getId());
      if (ref_id.equals ("NoRefId")) {
        System.out.println("Set ref id to " + ref_id + ". " +
                           "For child feature  " +
                           sf.getName() + ":" +
                           sf.getFeatureType());
      }

      Vector sf_vect;

      fhash = (sf.getStrand() == -1 ? minus_fhash : plus_fhash);
      Hashtable id_hash = (Hashtable) fhash.get(type);
      if (id_hash == null) {
        // System.out.println("NEW TYPE IN HASHFEATURES: " + type);
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

  public void makeGeneSets(StrandedFeatureSet fset,
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
      FeatureSetI t = new FeatureSet();
      t.setFeatureType(type);
      t.setId(ref_id);
      t.setStrand (strand);
      int sfVectSize = sf_vect.size();
      for (int i = 0; i < sfVectSize; i++) {
        SeqFeatureI sf = (SeqFeatureI) sf_vect.elementAt (i);
        t.addFeature(sf);
      }
      fs.addFeature (t);
    }
    fset.addFeature (fs);
  }

  public void makeSingSets(StrandedFeatureSet fset,
                           Hashtable id_hash,
                           String type,
                           int strand) {
    FeatureSetI fs = new FeatureSet();

    fs.setFeatureType(type);
    // fs.setHolder (true);
    fs.setStrand (strand);

    Enumeration e = id_hash.keys();
    while (e.hasMoreElements()) {
      String ref_id = (String) e.nextElement();
      Vector sf_vect = (Vector) id_hash.get(ref_id);
      int sfVectSize = sf_vect.size();
      for (int i = 0; i < sfVectSize; i++) {
        FeatureSet s = new FeatureSet();
        s.setFeatureType(type);
        s.setId(ref_id);
        s.setStrand (strand);
        SeqFeatureI sf = (SeqFeatureI) sf_vect.elementAt (i);
        s.addFeature(sf);
        fs.addFeature (s);
      }
    }
    fset.addFeature (fs,false);
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
        r.setId(ref_id);
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

        if (potentialSetsSize != 0) {
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
    return newsets;
  }


  public static void main(String[] args) {
    SeqFeature sf1 = new SeqFeature(100,200,"genscan",1);
    SeqFeature sf2 = new SeqFeature(300,400,"genscan",1);
    SeqFeature sf3 = new SeqFeature(500,600,"genscan",1);
    SeqFeature sf4 = new SeqFeature(700,800,"genscan",1);

    sf1.setName("gene1");
    sf2.setName("gene1");
    sf3.setName("gene2");
    sf4.setName("gene2");


    SeqFeature sf5 = new SeqFeature(100,200,"BLASTX",1);
    SeqFeature sf6 = new SeqFeature(101,201,"BLASTX",-1);
    SeqFeature sf7 = new SeqFeature(300,400,"BLASTX",1);
    SeqFeature sf8 = new SeqFeature(202,302,"BLASTX",-1);

    sf5.setName("query");
    sf7.setName("query");

    sf6.setName("hit");
    sf8.setName("hit");

    FeaturePair fp1 = new FeaturePair(sf5,sf6);
    FeaturePair fp2 = new FeaturePair(sf7,sf8);

    Vector features = new Vector();
    PropertyScheme ftypes = null;
    try {
      ftypes = new PropertyScheme();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    features.addElement(sf1);
    features.addElement(sf2);
    features.addElement(sf3);
    features.addElement(sf4);

    features.addElement(fp1);
    features.addElement(fp2);

    StrandedFeatureSet fset = new StrandedFeatureSet(new FeatureSet(), new FeatureSet());

    FeatureSetBuilder fse    = new FeatureSetBuilder();
    fse.makeSetFeatures(fset, features, ftypes);

    for (int i=0; i < fset.size(); i++) {
      System.out.println("Printing set feature " + i);
      System.out.println((SeqFeatureI)fset.getFeatureAt(i));
    }
  }

}
