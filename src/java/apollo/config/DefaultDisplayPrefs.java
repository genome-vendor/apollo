package apollo.config;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import javax.swing.JOptionPane;

import apollo.datamodel.*;
import apollo.util.*;
import apollo.gui.drawable.DrawableSeqFeature;
import apollo.gui.URLQueryGenerator;

import apollo.config.Config;

import org.apache.log4j.*;

public class DefaultDisplayPrefs implements DisplayPrefsI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DefaultDisplayPrefs.class);

  protected String getIdForURL (SeqFeatureI f) {
    return getDisplayName(f);
  }

  protected String getURLPrefix(FeatureProperty prop,
                                SeqFeatureI f, String id) {
    return prop.getURLString();
  }

  public String generateURL (SeqFeatureI f) {
    // Figure out URL for getting more information about this feature.
    // If more than one feature was selected, use the LAST one.
    String id = getIdForURL(f);
    FeatureProperty prop
      = Config.getPropertyScheme().getFeatureProperty(f.getFeatureType());

    String urlPrefix = getURLPrefix(prop, f, id);

    if (urlPrefix==null) {
      String m = "Sorry, no URL registered for type " + f.getFeatureType() +
        " in types file";
      JOptionPane.showMessageDialog(null,m);
      return null;
    }
    return URLQueryGenerator.getURL(urlPrefix, id);
  }

  public String getDisplayName (RangeI sf) {
    if (sf instanceof SeqFeatureI)
      return getDisplayName((SeqFeatureI)sf);
    return sf.getName();
  }

  private String getDisplayName(SeqFeatureI sf) {
    if (sf.hasHitFeature()) {
      SeqFeatureI hit = sf.getHitFeature();
      SequenceI seq = hit.getRefSequence();
      if (seq != null)
        return seq.getName();
      return hit.getName();
    }
    return sf.getName();
  }

  public String getHeader(RangeI sf) {
    return getDisplayName(sf);
  }

  public String getTitle (CurationSet curation) {
    String title;
    if (curation.getChildCurationSets().size() <= 0){
      if (curation.getRefSequence() == null) {
	title = (curation.getName() != null ?
		 curation.getName() :
		 "Unknown sequence");
      }
      else
	title = curation.getRefSequence().getName();
    }
    else {
      title = "composite";
    }
    return title;
  }

  public String getBioTypeForDisplay(SeqFeatureI sf) {
    return sf.getTopLevelType();
  }

  public String getPublicName(SeqFeatureI origSF) {
    String      name = null;
    SeqFeatureI sf;

    if (origSF instanceof DrawableSeqFeature) {
      sf = ((DrawableSeqFeature)origSF).getFeature();
    } else {
      sf = origSF;
    }

    if (sf instanceof ExonI) {
      ExonI exon = (ExonI)sf;
      if (exon.getRefFeature() != null) {
        name = exon.getRefFeature().getName();
      } else {
        name = exon.getName();
        logger.warn("No transcript for exon " + name);
      }
    } else if (sf instanceof FeaturePair) {
      FeaturePair fp = (FeaturePair)sf;
      name = fp.getHitFeature().getName();
      if (name == null || name.equals("")) {
        name = fp.getName();
      }
    } else if (sf instanceof Transcript) {
      Transcript trans = (Transcript)sf;
      name = trans.getName();
    } else if (sf instanceof FeatureSetI) {
      FeatureSetI fs = (FeatureSetI)sf;
      SequenceI seq = fs.getHitSequence();
      if (seq != null) {
        name = seq.getName();
      } else {
        SeqFeatureI sf2 = fs.getFeatureAt(0);
        if (sf2 instanceof FeaturePair) {
          SeqFeatureI hit = ((FeaturePair)sf2).getHitFeature();
          seq = hit.getRefSequence();
          if (seq != null) {
            name = seq.getName();
          } else {
            name = fs.getName();
          }
        }
        else if (sf2 != null) {
          name = sf2.getName();
        }
        else {
          name = sf.getName();
        }
      }
    } else {
      name = sf.getName();
    }
    if (name == null) {
      logger.warn("null group name " +
                  " class=" + sf.getClass().getName() +
                  " name=" + sf.getName() +
                  " type=" + sf.getFeatureType());
      name = "SeqFeatureI.NO_NAME";
    }
    return name;
  }  

}
