package apollo.util;

import java.util.*;

import apollo.datamodel.AnnotatedFeatureI;
import apollo.datamodel.CurationSet;
import apollo.datamodel.FeatureSetI;
import apollo.datamodel.FeaturePairI;
import apollo.datamodel.Score;
import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.SequenceI;
import apollo.datamodel.StrandedFeatureSet;

import apollo.config.FeatureProperty; // gui util?

import org.apache.log4j.*;

import org.bdgp.util.*;

public class SeqFeatureUtil {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SeqFeatureUtil.class);

  public static AnnotatedFeatureI getAnnotRoot(AnnotatedFeatureI annot) {
    if (annot.isAnnotTop())
      return annot;
    if (annot.getRefFeature() == null) // shouldnt happen
      return null;
    if (annot.getRefFeature().getAnnotatedFeature() == null) // shouldnt happen
      return null;
    return getAnnotRoot(annot.getRefFeature().getAnnotatedFeature());
  }

  /** Returns true if container contains query (or is query) */
  public static boolean containsFeature(SeqFeatureI container,SeqFeatureI query) {
    if (container == query)
      return true;
    boolean found = false;
    int setSize = container.getNumberOfChildren();
    for (int i = 0; i < setSize && !found; i++) {
      SeqFeatureI check = container.getFeatureAt(i);
      found = containsFeature(check,query);
      if (found)
        return true;
      //found = (sf == check);
      //if (!found && (check.canHaveChildren())) {
      //found = (check).findFeature(sf);
      //}
    }
    return found;

  }

  public static boolean isAminoAcidAlignment(String type) {
    return type.toUpperCase().indexOf("BLASTX") != -1;
  }

  public static boolean isAminoAcidAlignment(SeqFeatureI sf) {
    return isAminoAcidAlignment(sf.getFeatureType());
  }

  /** Calculates residue type  by comparing seq length to feat length. If there
      is more than 2 base pairs per residue than its assigned AA.
      Otherwise sets it to DNA. It would be
      nice if the residue type came from the data itself, but barring that
      we do this little trick.
      Michele writes: Nasty hack to tell peptide from dna. Blergh.
  */
  public static String guessResidueTypeFromFeatureLength(int seqLength,
                                                  int featureLength) {
    float basepairsPerResidue = (Math.abs((float)featureLength/
                                          (float)seqLength));
    return (basepairsPerResidue > 2.0 ? SequenceI.AA : SequenceI.DNA);
  }

  public static Vector getFeaturesOfClass(SeqFeatureI feature,
                                          Class type,
                                          boolean traverseMatches) {
    Vector out = new Vector();
    if (feature == null) {
      if (logger.isDebugEnabled()){
        logger.debug("null feature for SeqFeatureUtil.getFeaturesOfClass", new Throwable());
      }
      return out;
    }
    populateFeaturesOfClass(out, feature, type, traverseMatches);
    return out;
  }

  protected static void populateFeaturesOfClass(Vector out,
      SeqFeatureI feature,
      Class type,
      boolean traverseMatches) {
    if (feature == null) {// shouldnt happen
      if (logger.isDebugEnabled()) {
        logger.debug("null feature for SeqFeatureUtil.populateFeaturesOfClass", new Throwable());
      }
      return;
    }
    if (type.isAssignableFrom(feature.getClass())) {
      out.addElement(feature);
      if (!traverseMatches)
        return;
    }
    if (feature.canHaveChildren()) {
      FeatureSetI features = (FeatureSetI) feature;
      for(int i=0; i < features.getFeatures().size(); i++) {
        SeqFeatureI child = (SeqFeatureI)
                            features.getFeatures().elementAt(i);
        populateFeaturesOfClass(out,child,type,traverseMatches);
      }
    }
  }

  public static Vector getSortedKids(SeqFeatureI fs) {
    if (fs.getName().equals ("SeqFeatureI.NO_NAME")) {
      logger.error("Unexpected input in getSortedKids: " +
                   fs.getClass().getName() +
                   " of type " + fs.getFeatureType() +
                   " with " + fs.size() + " features?");
    }
    int setSize = fs.size();

    Vector sorted_list = new Vector(setSize);
    for (int i=0; i < setSize; i++) {
      sorted_list.addElement(fs.getFeatureAt(i));
    }

    // sort by Low if 0 strand or by Start if stranded set
    sort (sorted_list, fs.getStrand(), fs.getStrand() == 0 ? true : false);

    return sorted_list;
  }

  public static void sort(Vector features, int sortStrand) {
    // Sort by Start
    sort(features, sortStrand, false);
  }

  /* Changed to use the simplest, most basic feature so that
     it will work for all - RangeI */
  // byLow flag allows sorting by Low instead of Start to get true genomic ordering 
  // when there are features on both strands in the features vector.
  public static void sort(Vector features, int sortStrand, boolean byLow) {
    int setSize = features.size();

    if (setSize == 0) {
      return;
    }

    int[] coord = new int[setSize];
    RangeI[] obj  = new RangeI[setSize];

    for (int i=0; i < setSize; i++) {
      RangeI sf = (RangeI) features.elementAt(i);
      if (byLow) {
        coord[i] = sf.getLow();
      } else {
        coord[i] = sf.getStart();
      }
      obj  [i] = sf;
    }

    QuickSort.sort(coord,obj);

    // Used to be sortStrand == 1 but it seemed wrong to sort 0 stranded features backwards
    if (sortStrand != -1) {
      for (int i=0; i < setSize;i++) {
        features.setElementAt(obj[i], i);
      }
    }
    else {
      for (int i=0; i < setSize;i++) {
        features.setElementAt(obj[i], (setSize - i - 1));
      }
    }
  }

  public static boolean equals(CurationSet a, CurationSet b) {
    //debug("Results:");
    /*
    Vector a_features = getFeatureVector(a);
    Vector b_features = getFeatureVector(b);
    if (a_features.size() != b_features.size())
    throw new FeatureInequalityException("Different number of features from each source");
    for(int j=0; j < a_features.size(); j++) {
    FeatureSetI fs_a = (FeatureSetI) a_features.elementAt(j);
    FeatureSetI fs_b = (FeatureSetI) b_features.elementAt(j);
    if (!equals(fs_a, fs_b))
     throw new FeatureInequalityException("Features don't match "+
    "(this exception will never actually be thrown)");
  }
    */
    if (!equals(a.getResults(), b.getResults()))
      return false;
    else
      return true;
  }

  public static boolean equals(SeqFeatureI a, SeqFeatureI b) {
    if (a instanceof FeatureSetI && b instanceof FeatureSetI)
      return equalsFeatures(a, b) &&
             equals((FeatureSetI) a, (FeatureSetI) b);
    if (a instanceof FeaturePairI && b instanceof FeaturePairI)
      return equalsFeatures(a, b) &&
             equals((FeaturePairI) a, (FeaturePairI) b);
    else
      return equalsFeatures(a, b);
  }

  public static boolean equals(FeaturePairI a, FeaturePairI b) {
    if (!equals(a.getQueryFeature(), b.getQueryFeature()))
      throw new FeatureInequalityException("Query features don't match",
                                           a, b);
    if (!equals(a.getHitFeature(), b.getHitFeature()))
      throw new FeatureInequalityException("Hit features don't match",
                                           a, b);

    return true;
  }

  public static boolean equals(FeatureSetI a, FeatureSetI b) {
    if (a.getFeatures().size() != b.getFeatures().size())
      throw new FeatureInequalityException("Different feature counts",
                                           a, b);
    Vector a_features = VectorUtil.sort(a.getFeatures(),
                                        new FeatureComparator());
    Vector b_features = VectorUtil.sort(b.getFeatures(),
                                        new FeatureComparator());
    for(int i=0; i < a_features.size(); i++) {
      SeqFeatureI sf_a = (SeqFeatureI) a_features.elementAt(i);
      SeqFeatureI sf_b = (SeqFeatureI) b_features.elementAt(i);
      if (!equals(sf_a, sf_b))
        throw new FeatureInequalityException("Features don't match",
                                             a, b);
    }
    return true;
  }

  public static boolean equalsFeatures(SeqFeatureI a, SeqFeatureI b) {
    if (!strcmp(a.getId(), b.getId())) {
      throw new FeatureInequalityException("Mismatched ids", a, b);
    } else if (!strcmp(a.getName(), b.getName())) {
      throw new FeatureInequalityException("Mismatched names", a, b);
    } else if (a.getStart() != b.getStart()) {
      throw new FeatureInequalityException("Mismatched starts", a, b);
    } else if (a.getEnd() != b.getEnd()) {
      throw new FeatureInequalityException("Mismatched ends", a, b);
    } else if (!strcmp(a.getResidues(), b.getResidues())) {
      throw new FeatureInequalityException("Mismatched sequences", a, b);
    } else if (!equals(a.getRefSequence(), b.getRefSequence())) {
      throw new FeatureInequalityException("Mismatched sequences", a, b);
    } else if (!strcmp(a.getRefId(), b.getRefId())) {
      throw new FeatureInequalityException("Mismatched ref ids", a, b);
    } else if (a.getStrand() != b.getStrand()) {
      throw new FeatureInequalityException("Mismatched strands", a, b);
    } else if (!a.getFeatureType().equals(b.getFeatureType())) {
      throw new FeatureInequalityException("Mismatched types", a, b);
    } else if (!strcmp(a.getProgramName(), b.getProgramName())) {
      throw new FeatureInequalityException("Mismatched program names",
                                           a,
                                           b);
    } else if (!scoreCompare(a.getScores(), b.getScores())) {
      throw new FeatureInequalityException("Mismatched scores", a, b);
    } else if (a.getPhase() != b.getPhase()) {
      throw new FeatureInequalityException("Mismatched phases", a, b);
    } else
      return true;
  }

  public static boolean strcmp(String a, String b) {
    if (a == null || b == null) {
      return a == b;
    } else
      return a.equals(b);
  }

  public static boolean equals(SequenceI a, SequenceI b) {
    if (a == null || b == null) {
      return a == b;
    }
    if (!strcmp(a.getAccessionNo(), b.getAccessionNo()))
      return false;
    if (a.getLength() != b.getLength())
      return false;
    if (!strcmp(a.getResidues(), b.getResidues()))
      return false;
    if (!strcmp((a).getDescription(), (b).getDescription()))
      return false;
    return true;
  }

  public static boolean scoreCompare(Hashtable scoresa, Hashtable scoresb) {
    if (scoresa.size() != scoresb.size())
      return false;
    Enumeration keys = scoresa.keys();
    while(keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      Score avalue = (Score) scoresa.get(key);
      Score bvalue = (Score) scoresb.get(key);
      if ((avalue == null || bvalue == null) &&
          avalue != bvalue)
        return false;
      if (avalue.getValue() != bvalue.getValue())
        return false;
    }
    return true;
  }

  public static Vector getFeatureVector(CurationSet set) {
    Vector features = getFeatureVector(set.getResults());
    Vector annotationVector = getFeatureVector(set.getAnnots());

    for(int i=0; i < annotationVector.size(); i++) {
      features.addElement(annotationVector.elementAt(i));
    }

    features = VectorUtil.sort(features, new FeatureComparator());
    return features;
  }

  public static Vector getFeatureVector(FeatureSetI set) {
    Vector out = new Vector();
    for(int i=0; i < set.getFeatures().size(); i++) {
      FeatureSetI fs = (FeatureSetI) set.getFeatures().elementAt(i);
      for(int j=0; j < fs.getFeatures().size(); j++) {
        FeatureSetI fs_in = (FeatureSetI) fs.getFeatures().
                            elementAt(j);
        out.addElement(fs_in);
      }
    }
    return out;
  }

  // Is static right??
  public static int binarySearch(Vector v, Comparator c, Object o, boolean findSpace) {
    if (v.size() == 0) {
      return -1;
    }
    int first = 0;
    int last = v.size()-1;
    int middle = -1;
    while(first <= last) {
      middle = (first+last) / 2;

      Object middleElt = v.elementAt(middle);
      int compValue = c.compare(middleElt,o);

      if (findSpace) {
        boolean leftSmaller = true;
        boolean rightLarger = (compValue == 1);

        if (middle > 0)
          leftSmaller = (c.compare(o,v.elementAt(middle-1))
                         != -1);
        if (leftSmaller && rightLarger)
          return middle;
      }

      if (compValue == 0)
        if (findSpace)
          return middle+1;
        else
          return middle;
      else if (compValue == -1)
        first = middle + 1;
      else
        last = middle - 1;
    }
    if (findSpace)
      return v.size();
    else
      return -1;
  }

  public static Vector sortFeaturesAlphabetically(Vector features) {
      // do insertion sort
      Vector sortedVector = new Vector();
      //      FeatureComparator comp = new FeatureComparator();
      FeatureAlphabeticalComparator comp = new apollo.util.FeatureAlphabeticalComparator();
      for(int i=0; i < features.size(); i++) {
        SeqFeatureI feature = (SeqFeatureI) features.elementAt(i);
        int index = apollo.util.SeqFeatureUtil.binarySearch(sortedVector,
                                 comp,
                                 feature,
                                 true);
        if (index < 0)
          index = 0;
        if (index >= sortedVector.size())
          sortedVector.addElement(feature);
        else
          sortedVector.insertElementAt(feature, index);
      }
      return sortedVector;
  }

  /** Get all leaf features for the FeatureProperty. FeatureProperty is a
      visual/gui type. is gui stuff off limits in util?
      should there be a util for gui stuff?
      boolean for leaf features?
  */
  public static FeatureList getFeatPropLeafFeatures(CurationSet cur,
                                                    FeatureProperty prop) {
    FeatureList tierFeats = new FeatureList();
    // Theres a separate feature set for each analysis type.
    // a tier has one or more analysis types
    for (int i=0; i<prop.getAnalysisTypesSize(); i++) {
      String analysisType = prop.getAnalysisType(i);
      logger.debug("SFU getting leaves for analysis type "+analysisType+" prop "+prop);
      FeatureList anFeats = cur.getAnalysisFeatureList(analysisType);
      tierFeats.addAllFeatures(anFeats); // gets leaves below
    }
    return tierFeats.getAllLeaves(); // get the leaves of the sfs
  }

  /** return parent features of leaves (often these are transcript or transcript 
      like feats) - the thin line that connects leaf feats/exons */
  public static FeatureList getFeatPropLeafParentFeats(CurationSet cur,
                                                       FeatureProperty prop) {
    FeatureList leaves = getFeatPropLeafFeatures(cur,prop);
    FeatureList parents = new FeatureList(); // by default checks for unique
    for (int i=0; i<leaves.size(); i++)
      parents.add(leaves.getFeature(i).getRefFeature());
    return parents;
  }

  /** Returns a list of all transcripts in cur set - both strands */
  public static FeatureList getTranscripts(CurationSet curSet) {
    return getTranscripts(curSet.getAnnots());
  }
  private static FeatureList getTranscripts(SeqFeatureI feat) {
    FeatureList featList = new FeatureList();
    if (feat.getFeatureType().equals("transcript")) {
      featList.addFeature(feat);
    }
    else {
      for (int i=0; i<feat.getNumberOfChildren(); i++) {
        featList.addAll(getTranscripts(feat.getFeatureAt(i)));
      }
    }
    return featList;
  }
}

class FeatureInequalityException extends RuntimeException {
  SeqFeatureI a;
  SeqFeatureI b;


  public FeatureInequalityException(String message) {
    super(message);
  }

  public FeatureInequalityException(String message,
                                    SeqFeatureI a,
                                    SeqFeatureI b) {
    super(message);
    this.a = a;
    this.b = b;
  }
}
