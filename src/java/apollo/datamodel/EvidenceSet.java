package apollo.datamodel;

import java.util.*;

import org.apache.log4j.*;

public class EvidenceSet implements java.io.Serializable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(EvidenceSet.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  Hashtable evidence_hash = new Hashtable();
  Vector evidence = new Vector();

  public EvidenceSet() {}

  public void addEvidence(Evidence ev) {
    if (this.evidence_hash.get(ev.getFeatureId()) == null) {
      this.evidence.addElement(ev);
      this.evidence_hash.put (ev.getFeatureId(), ev);
    }
  }

  public int deleteEvidence(String id) {
    Vector matches = getEvidence(id);
    for (int i=0; i<matches.size(); i++) {
      Evidence ev = (Evidence)matches.elementAt(i);
      deleteEvidence(ev);
    }
    return matches.size();
  }

  public void deleteEvidence(Evidence ev) {
    evidence_hash.remove (ev.getFeatureId());
    evidence.removeElement(ev);
  }

  public Vector getEvidence(String id) {
    Vector matches = new Vector();
    for (int i=0; i<evidence.size(); i++) {
      Evidence ev = getEvidence(i);
      if (ev.getFeatureId().equals(id)) {
        matches.addElement(ev);
      }
    }
    return matches;
  }

  public Vector getEvidence() {
    return evidence;
  }

  public Evidence getEvidence(int index) {
    if (index < 0 || index >= evidence.size()) {
      logger.error("Invalid index in getEvidenceAt");
      return null;
    }
    return (Evidence)evidence.elementAt(index);
  }
}
