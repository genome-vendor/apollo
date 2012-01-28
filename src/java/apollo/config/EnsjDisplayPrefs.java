package apollo.config;

import apollo.datamodel.*;
import apollo.util.*;
import java.util.*;

public class EnsjDisplayPrefs extends DefaultDisplayPrefs {

  protected String getURLPrefix(FeatureProperty prop,
                                SeqFeatureI f, String id) {
    String url = prop.getURLString();
    SequenceI seq = f.getRefSequence();
    if (seq != null && seq.getOrganism() != null) {
      try {
        int index = url.indexOf('*');
        if (index > 0) {
          url = (url.substring(0, index) + 
                 seq.getOrganism() +
                 url.substring(index +1));
        }
      } catch (Exception e) {
        System.out.println ("Could not parse * from url " + url);
      }
    }
    return url;
  }

  protected String getIdForURL (SeqFeatureI f) {
    /* If only an HSP is selected and it is not an
     * alignment then use the ID of the parent */
    String id;
    if (f instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair)f;
      SeqFeatureI sbjct = (SeqFeatureI) fp.getHitFeature();
      id = sbjct.getName();
    } else if (!(f instanceof FeatureSet)) {
      id = getDisplayName(f.getRefFeature());
    } else {
      id = getDisplayName(f);
    }
    return id;
  }

   

}
