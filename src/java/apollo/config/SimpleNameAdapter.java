package apollo.config;

import java.util.Vector;

import org.apache.log4j.*;

import apollo.datamodel.RangeI;
import apollo.datamodel.SeqFeatureI;
import apollo.datamodel.StrandedFeatureSetI;
import apollo.util.FeatureList;

/** This adapter just uses the name of the result to name the annot. In other words the
    annots are pre named. So its really not much of a name adapter. This is simpler than 
    DefaultNameAdapter, and perhaps this should be Default(?). Not sure if 
    SimpleNameAdapter is a good name for this - PreNamedNameAdapter?
    UseResultNameAdapter? ResultNameAdapter? ...? OneLevelNameAdapter? 
    i think this is prtty much for 1 level annots*/

public class SimpleNameAdapter extends DefaultNameAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(SimpleNameAdapter.class);

  /** Annot suffix could come from config, or even be grounds for a new name adapter.
      For now just hardwiring in - needed to differentiate annot name from result
      for commiting to chado db (violates unique constraint) */
  private static final boolean addAnnotSuffix = true;

  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature, Vector resultFeats) {
    if (resultFeats == null || resultFeats.size() == 0) {
      logger.debug("no result for SimpleNameAdapter to name annot with");
      return super.generateName(annots,curation_name,feature);
    }
      
    RangeI firstResult = (RangeI)resultFeats.get(0);
    // check for no name? null?
    String name = firstResult.getName();
    if (addAnnotSuffix)
      name += "-annot" + getSuffixNumber(name,annots);
    return name;
  }

  /** Return 1 + highest suffix number in use */
  private int getSuffixNumber(String resultName,StrandedFeatureSetI annots) {
    String nameRegexp = resultName+"-annot\\d+";
    // true -> regexp, true -> kidNamesOverParent
    FeatureList anns = annots.findFeaturesByAllNames(nameRegexp,true,true);
    if (anns.isEmpty())
      return 1;
    int index = resultName.length() + "-annot".length();
    int largestSuffixNumber = 0;
    for (int i=0; i<anns.size(); i++) {
      SeqFeatureI ann = anns.getFeature(i);
      String numString = ann.getName().substring(index);
      try {
        int sfxNum = Integer.valueOf(numString).intValue();
        if (sfxNum > largestSuffixNumber)
          largestSuffixNumber = sfxNum;
      } catch (NumberFormatException e) {}
    }
    return largestSuffixNumber + 1;
  }
  

  /** this is a workaround until we actually have 1-level annots. for annots that
      should be 1 level but still have transcripts, name the transcript with the
      gene name. the transcript name is what shows up in the main window, without
      this you would see a temp name */
  public String generateName(StrandedFeatureSetI annots,
                             String curation_name,
                             SeqFeatureI feature) {

    if (isTranscriptOfOneLevelAnnot(feature))
      return feature.getRefFeature().getName();
    
    return super.generateName(annots,curation_name,feature);
  }

  /** In other words is this a false transcript */
  private boolean isTranscriptOfOneLevelAnnot(SeqFeatureI feat) {
    if (!feat.isTranscript())
      return false;
    String annotType = feat.getRefFeature().getFeatureType();
    FeatureProperty fp =Config.getPropertyScheme().getFeatureProperty(annotType);
    return fp.isOneLevel();
  }

}
