package apollo.datamodel;

import java.util.*;
import apollo.util.FeatureList;
import org.apache.log4j.*;

/**
  It may well be that any FeatureSet for which the
  strand is not explicitly set to either 1 or -1
  (in other words the strand is 0) should be treated
  as a mixed set of features that may be on either
  strand
*/

public class StrandedFeatureSet extends FeatureSet
  implements StrandedFeatureSetI {
  protected FeatureSetI forward;
  protected FeatureSetI reverse;
  protected final static Logger logger = LogManager.getLogger(StrandedFeatureSet.class);

  public StrandedFeatureSet(FeatureSetI forward_set, FeatureSetI reverse_set) {
    this.forward = forward_set;
    this.reverse = reverse_set;
    forward.setStrand (1);
    reverse.setStrand (-1);
    /* make sure these sets know their origins */
    forward.setRefFeature(this);
    reverse.setRefFeature(this);
    setStrand (0);
  }

  public StrandedFeatureSet() {
    this(new FeatureSet(),new FeatureSet());
  }

  // StrandedFeatureSetI methods
  public FeatureSetI getForwardSet() {
    return forward;
  }

  public FeatureSetI getReverseSet() {
    return reverse;
  }

  // FeatureSetI methods
  public int size() {
    int tmp = (forward == null ? 0 : forward.size());
    tmp += (reverse == null ? 0 : reverse.size());

    return tmp;
  }

  /** Features must be added in sort order,
      if this turns out to be too slow, then we
      will need to figure out another way to keep
      them all sorted (5prime to 3prime that is) */
  // SMJS Why must they be added in sorted order???
  public void addFeature(SeqFeatureI feature) {
    if (feature.getStrand() == -1) {
      reverse.addFeature(feature, true);
    } else if (feature.getStrand() == 1) {
      forward.addFeature(feature, true);
    } else {
      forward.addFeature(feature);
      reverse.addFeature(feature);
    }
    adjustEdges (feature);
  }

  // SMJS Added a new addFeature method which allows SeqFeatureIs to
  //      be added without sorting, so saving lots of cycles
  //      This isn't the default behaviour, unlike in FeatureSet,
  //      because I'm not sure whether there are places which will
  //      need the sorted insert to be the default behaviour from
  //      addFeature for this class
  public void addFeature(SeqFeatureI feature, boolean sortFlag) {
    if (feature.getStrand() == -1) {
      reverse.addFeature(feature, sortFlag);
    } else if (feature.getStrand() == 1) {
      forward.addFeature(feature, sortFlag);
    } else {
      forward.addFeature(feature, sortFlag);
      reverse.addFeature(feature, sortFlag);
    }
    adjustEdges (feature);
  }

  public void deleteFeature(SeqFeatureI feature) {
    forward.deleteFeature(feature);
    reverse.deleteFeature(feature);
  }

  public SeqFeatureI deleteFeatureAt(int i) {
    SeqFeatureI sf;

    if (i < forward.size()) {
      sf = forward.deleteFeatureAt(i);
      return sf;
    } else {
      int size = forward.size();
      int num  = i - size;

      if (num < reverse.size()) {
        sf = reverse.deleteFeatureAt(num);
        return sf;
      }
    }
    return null;
  }

  /** This actually returns not forward feature set and reverse feature set,
      but the children of forward and reverse. In other words it retrieves
      the grandchildren of the StrandedFeatureSet */
  public SeqFeatureI getFeatureAt(int i) {
    SeqFeatureI sf = null;
    if (i < forward.size()) {
      sf = forward.getFeatureAt(i);
    } else {
      int size = forward.size();
      int num  = i - size;

      if (num < reverse.size()) {
        sf = reverse.getFeatureAt(num);
      }
    }
    return sf;
  }

  /** This actually returns not forward feature set and reverse feature set,
      but the children of forward and reverse. In other words it retrieves
      the grandchildren of the StrandedFeatureSet */
  public Vector getFeatures() {
    Vector tmp = new Vector();
    for (int i=0; i < forward.size();i++) {
      tmp.addElement(forward.getFeatureAt(i));
    }
    for (int i=0; i < reverse.size();i++) {
      tmp.addElement(reverse.getFeatureAt(i));
    }
    return tmp;
  }

  public FeatureList getLeafFeatsOver(int pos) {
    FeatureList leaves = forward.getLeafFeatsOver(pos);
    leaves.addAll(reverse.getLeafFeatsOver(pos));
    return leaves;
  }

  //overridden SeqFeatureI methods
  public int getStart() {
    int tmp1 = forward.getStart();
    int tmp2 = reverse.getStart();
    // this is wrong, must not be used
    if (tmp1 < tmp2) {
      return tmp1;
    } else {
      return tmp2;
    }

  }

  public int getEnd() {
    int end = 1;
    boolean set = false;

    if (forward != null && forward.hasDescendents()) {
      end = forward.getEnd();
      set = true;
    }

    if (reverse != null &&
        reverse.hasDescendents() &&
        reverse.getEnd() > end) {
      end = reverse.getEnd();
      set = true;
    }
    if (!set) {
      logger.info("Warning: No features to getEnd() for");
    }
    return end;

  }

  public void setRefSequence(SequenceI seq) {
    this.refSeq = seq;
    // The ref sequence for the feature set will be the ref sequence
    // for all its sub features
    forward.setRefSequence (seq);
    reverse.setRefSequence (seq);
  }
  
  /** Returns seq feat grandchildren (tier level) of type & strand.
      returns null if not present */
  public SeqFeatureI getSeqFeat(int strand, String type) {
    FeatureSetI strandFS = getFeatSetForStrand(strand);
    for (int i=0; i<strandFS.size(); i++) {
      SeqFeatureI grandChild = strandFS.getFeatureAt(i);
      if (grandChild.getFeatureType().equals(type))
        return grandChild;
    }
    return null; // not found
  }

  public FeatureSetI getFeatSetForStrand(int strand) {
    if (strand == 1)
      return getForwardSet();
    return getReverseSet();
  }

  public StrandedFeatureSetI getStrandedFeatSetAncestor() {
    return this;
  }

  /**
   * General implementation of Visitor pattern. (see apollo.util.Visitor).
  **/
  public void accept(apollo.util.Visitor visitor){
    visitor.visit(this);
  }//end accept
  
}


